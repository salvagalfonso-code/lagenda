package com.example.agendavalenciana.ios

/** Pas 44. Dades de campanya: l'estat de facturació i de publicació és SEMPRE del PHP. */
internal data class IosCampaignTarget(
    val styles: Set<String> = emptySet(), val terms: Set<String> = emptySet(),
    val towns: Set<String> = emptySet(), val comarques: Set<String> = emptySet(),
    val followers: Boolean = false, val highIntent: Boolean = false, val all: Boolean = false
)
internal data class IosCampaignDraft(
    val eventId: Int, val placement: String = "FEED_NORMAL", val durationDays: Int = 1,
    val target: IosCampaignTarget = IosCampaignTarget()
)
internal data class IosCampaign(
    val id: Int, val eventId: Int, val placement: String, val status: String,
    val startsAt: String, val endsAt: String, val amountCents: Long,
    val imageUrl: String = "", val focusX: Float = .5f, val focusY: Float = .5f,
    val target: IosCampaignTarget = IosCampaignTarget()
)
internal data class IosCampaignPayment(
    val adId: Int, val secret: String, val key: String, val cents: Long,
    val currency: String, val merchant: String, val mode: String
)
internal data class IosCampaignStatus(val adId: Int, val status: String, val ad: IosCampaign?)

internal object IosAdvertisingPolicy {
    val placements = setOf("HEADER", "FEED_PREMIUM", "FEED_NORMAL", "POPUP")
    val durations = setOf(1, 3, 7)
    private val finalFailure = setOf("payment_failed", "cancelled", "canceled")
    fun valid(d: IosCampaignDraft): Boolean = d.eventId > 0 && d.placement in placements &&
        d.durationDays in durations && sequenceOf(d.target.styles, d.target.terms, d.target.towns, d.target.comarques)
            .all { entries -> entries.size <= 30 && entries.all { it.isNotBlank() && it.length <= 90 } }
    fun validPayment(start: IosCampaignPayment): Boolean = start.adId > 0 &&
        start.secret.startsWith("pi_") && "_secret_" in start.secret &&
        (start.key.startsWith("pk_test_") || start.key.startsWith("pk_live_")) &&
        start.cents > 0 && start.currency.equals("eur", true) &&
        start.merchant.isNotBlank() && start.mode.lowercase() in setOf("test", "live") &&
        (if (start.mode.equals("live", true)) start.key.startsWith("pk_live_") else start.key.startsWith("pk_test_"))
    fun activeConfirmed(requestedId: Int, status: IosCampaignStatus, fresh: List<IosCampaign>): Boolean =
        requestedId > 0 && status.adId == requestedId && status.status.equals("active", true) &&
        status.ad?.let { it.id == requestedId && it.status.equals("active", true) } == true &&
        fresh.count { it.id == requestedId && it.status.equals("active", true) } == 1
    fun terminalFailure(status: String): Boolean = status.lowercase() in finalFailure
    fun sameTarget(a: IosCampaignTarget, b: IosCampaignTarget): Boolean = a == b
    fun updatedTarget(original: IosCampaign, expected: IosCampaignTarget, after: List<IosCampaign>): Boolean =
        after.count { it.id == original.id && it.eventId == original.eventId &&
            it.placement == original.placement && sameTarget(expected, it.target) } == 1
    fun updatedVisual(adId: Int, x: Float, y: Float, removed: Boolean, after: List<IosCampaign>): Boolean =
        x in 0f..1f && y in 0f..1f && after.count { it.id == adId &&
            kotlin.math.abs(it.focusX-x) <= .011f && kotlin.math.abs(it.focusY-y) <= .011f &&
            (!removed || it.imageUrl.isBlank()) } == 1
    fun parseCsv(text: String): Set<String> = text.split(',', ';', '\n').map(String::trim)
        .filter(String::isNotBlank).toSet()
}
