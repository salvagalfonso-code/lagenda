package com.example.agendavalenciana.ios

/** Pas 36: role is server-confirmed; publication trust is NOT conferred by a role. */
internal data class IosPublisherSnapshot(
    val userId: Int, val role: String, val managedArtists: Set<String> = emptySet(),
    val professionalVerified: Boolean = false, val approvedCount: Int = 0,
    val canAutoPublish: Boolean = false, val suspended: Boolean = false,
    val ownedPublishedIds: Set<Int> = emptySet(), val serverConfirmed: Boolean = false
)

internal enum class IosPublishAction { NEW_EVENT, EDIT_EVENTS, CREATE_AD, TICKETS, MERCH,
    DONATIONS, SPECIAL_SERVICES, ACTIVATE_SALES, CLAIM_PROFESSIONAL, REVIEW_PUBLICATIONS, REVIEW_PROFESSIONAL_CLAIMS }

internal object IosPublisherRoles {
    private val professionalRoles = setOf("PROFESSIONAL", "GROUP", "AGENCY", "LABEL", "VENUE", "PROMOTER")
    fun normalized(role: String?): String = role?.trim()?.uppercase().orEmpty().ifBlank { "GUEST" }
    fun isSuperAdmin(role: String?) = normalized(role) == "SUPER_ADMIN"
    fun isProfessional(snapshot: IosPublisherSnapshot): Boolean = snapshot.serverConfirmed &&
        (isSuperAdmin(snapshot.role) || snapshot.professionalVerified || normalized(snapshot.role) in professionalRoles)
    fun canPublishDirectly(snapshot: IosPublisherSnapshot): Boolean = snapshot.serverConfirmed &&
        !snapshot.suspended && (isSuperAdmin(snapshot.role) || snapshot.canAutoPublish)
    fun visibleActions(registered: Boolean, snapshot: IosPublisherSnapshot): List<IosPublishAction> = buildList {
        add(IosPublishAction.NEW_EVENT) // Android: even server-backed guests may publish.
        if (snapshot.ownedPublishedIds.isNotEmpty()) {
            add(IosPublishAction.EDIT_EVENTS)
            add(IosPublishAction.CREATE_AD)
        }
        if (registered && snapshot.ownedPublishedIds.isNotEmpty() && !isProfessional(snapshot)) {
            // Android seller onboarding after publishing, before the account becomes PROFESSIONAL.
            add(IosPublishAction.ACTIVATE_SALES)
        }
        if (registered && isProfessional(snapshot)) {
            add(IosPublishAction.TICKETS)
            add(IosPublishAction.MERCH)
            add(IosPublishAction.DONATIONS)
            add(IosPublishAction.SPECIAL_SERVICES)
        }
        if (registered && isSuperAdmin(snapshot.role) && snapshot.serverConfirmed) {
            add(IosPublishAction.REVIEW_PUBLICATIONS)
            add(IosPublishAction.REVIEW_PROFESSIONAL_CLAIMS)
        } else add(IosPublishAction.CLAIM_PROFESSIONAL) // Guest opens login before contacting the API.
    }
    fun allows(action: IosPublishAction, registered: Boolean, snapshot: IosPublisherSnapshot): Boolean =
        snapshot.serverConfirmed && action in visibleActions(registered, snapshot)
}

internal data class IosPublishedEvent(
    val eventId: Int, val title: String, val status: String, val date: String,
    val town: String, val category: String, val description: String = "", val subtitle: String = "",
    val time: String = "", val endDate: String = "", val endTime: String = "",
    val comarca: String = "", val venue: String = "", val artists: List<String> = emptyList(),
    val organizers: List<String> = emptyList(), val styles: List<String> = emptyList(),
    val price: Int = 0, val ticketEnabled: Boolean = false, val ticketUrl: String = "",
    val posterUrl: String = ""
)

internal data class IosPublishDraft(
    val title: String, val date: String, val time: String, val town: String,
    val category: String = "OTHER", val artists: List<String> = emptyList(),
    val organizers: List<String> = emptyList(), val subtitle: String = "", val endDate: String = "",
    val endTime: String = "", val comarca: String = "", val venue: String = "",
    val styles: List<String> = emptyList(), val price: Int = 0,
    val ticketChannel: String = "NONE", val ticketUrl: String = "", val description: String = ""
) {
    fun validate() {
        require(title.trim().isNotBlank() && title.length <= 220 && town.trim().isNotBlank()) { "Cal indicar títol i poble." }
        require(date.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) && time.matches(Regex("\\d{2}:\\d{2}"))) { "Indica data AAAA-MM-DD i hora HH:MM." }
        require(endDate.isEmpty() || endDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) && endDate >= date) { "Data final invàlida." }
        require(endTime.isEmpty() || endTime.matches(Regex("\\d{2}:\\d{2}"))) { "Hora final invàlida." }
        require(category in categories) { "Categoria cultural incorrecta." }
        require(category != "MUSIC" || artists.any { it.isNotBlank() }) { "Cal indicar artistes en la categoria Música." }
        require(price >= 0 && ticketChannel in setOf("NONE", "EXTERNAL", "AGENDA")) { "Configuració d'entrades invàlida." }
        require(ticketChannel != "EXTERNAL" || ticketUrl.trim().startsWith("https://")) { "Indica un enllaç https d'entrades." }
    }
    companion object { val categories = listOf("MUSIC", "THEATRE", "CINEMA", "TALKS", "DANCE", "EXHIBITIONS", "LITERATURE", "WORKSHOPS", "CHILDREN", "POPULAR_CULTURE", "HERITAGE", "OTHER") }
}

/** Android's seller-onboarding/index.php requires a published event AND legal notices. */
internal data class IosSellerActivation(
    val eventId: Int, val existingCompanyId: Int = 0, val kind: String = "PERSON",
    val displayName: String = "", val taxId: String = "", val address: String = "",
    val postalCode: String = "", val town: String = "", val province: String = "",
    val acceptedPrivacy: Boolean = false, val acceptedSales: Boolean = false,
    val privacyViewed: Boolean = false, val salesViewed: Boolean = false
) {
    fun validate() {
        require(eventId > 0 && acceptedPrivacy && acceptedSales) { "Cal un esdeveniment propi i acceptar les dos condicions." }
        require(existingCompanyId >= 0)
        if (existingCompanyId == 0) {
            require(kind in setOf("PERSON", "COMPANY") && displayName.isNotBlank() && address.isNotBlank() && town.isNotBlank()) {
                "Indica el titular fiscal i l'adreça completa."
            }
            require(taxId.uppercase().replace(" ","").matches(Regex("[A-Z0-9-]{8,15}"))) { "DNI/NIE/CIF invàlid." }
            require(postalCode.matches(Regex("\\d{5}"))) { "Codi postal de cinc xifres." }
        }
    }
}
