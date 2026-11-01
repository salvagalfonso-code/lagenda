package com.example.agendavalenciana.ios

/** Pas 42: la facturació i els consentiments tenen com a font de veritat el servidor.
 * Un POST que retorna un ID no prova ni la vigència dels consentiments ni la vinculació
 * amb el titular seleccionat; cal verificar el GET del compte i el mateix company_id.
 */
internal data class IosSellerCompany(
    val id: Int, val displayName: String, val noticesCurrent: Boolean
)

internal data class IosSellerStatus(
    val hasBilling: Boolean, val policiesReady: Boolean,
    val privacyUrl: String, val salesTermsUrl: String,
    val companies: List<IosSellerCompany>
)

internal object IosSellerLegalPolicy {
    fun validLegalUrl(url: String): Boolean {
        val clean = url.trim()
        if (clean != url || clean.length !in 12..2048 || !clean.startsWith("https://", true)) return false
        if (clean.any { it.isWhitespace() || it.code < 32 }) return false
        val domain = clean.substring(8).substringBefore('/').substringBefore('?').substringBefore('#')
        return domain.length in 4..253 && domain.contains('.') && !domain.contains('@') &&
            domain.all { it.isLetterOrDigit() || it in ".-:" } &&
            !domain.startsWith('.') && !domain.endsWith('.')
    }

    fun policiesUsable(status: IosSellerStatus) = status.policiesReady &&
        validLegalUrl(status.privacyUrl) && validLegalUrl(status.salesTermsUrl)

    fun selectedCompany(status: IosSellerStatus, selectedId: Int): IosSellerCompany? =
        status.companies.singleOrNull { selectedId > 0 && it.id == selectedId }

    fun canSubmit(draft: IosSellerActivation, status: IosSellerStatus,
                  isRegistered: Boolean, ownPublishedIds: Set<Int>): Boolean {
        if (!isRegistered || draft.eventId !in ownPublishedIds || !policiesUsable(status)) return false
        if (runCatching { draft.validate() }.isFailure) return false
        if (draft.existingCompanyId > 0 && selectedCompany(status, draft.existingCompanyId) == null) return false
        val alreadyAccepted = status.hasBilling &&
            selectedCompany(status, draft.existingCompanyId)?.noticesCurrent == true
        return alreadyAccepted || (draft.privacyViewed && draft.salesViewed &&
            draft.acceptedPrivacy && draft.acceptedSales)
    }

    fun postAcknowledged(eventId: Int, companyId: Int, ready: Boolean, enabled: Boolean,
                         requestedEventId: Int, requestedCompanyId: Int): Boolean =
        eventId == requestedEventId && companyId > 0 && ready && !enabled &&
            (requestedCompanyId == 0 || requestedCompanyId == companyId)

    fun getConfirmed(status: IosSellerStatus, companyId: Int): Boolean =
        status.hasBilling && selectedCompany(status, companyId)?.noticesCurrent == true
}

/** Venda efectiva de la modalitat. El GET professional-seller/options.php és l'autoritat. */
internal data class IosSellerSaleState(
    val eventId: Int, val enabled: Boolean, val activeOptions: Int, val totalOptions: Int
)

internal object IosSellerSalePolicy {
    fun canEnable(sale: IosSellerSaleState, fiscal: IosSellerStatus, ownPublished: Set<Int>): Boolean =
        sale.eventId > 0 && sale.eventId in ownPublished &&
        IosSellerLegalPolicy.policiesUsable(fiscal) && fiscal.hasBilling &&
        fiscal.companies.any { it.noticesCurrent } && sale.activeOptions > 0

    fun verified(targetEventId: Int, targetEnabled: Boolean, actual: IosSellerSaleState): Boolean =
        targetEventId > 0 && actual.eventId == targetEventId && actual.enabled == targetEnabled
}
