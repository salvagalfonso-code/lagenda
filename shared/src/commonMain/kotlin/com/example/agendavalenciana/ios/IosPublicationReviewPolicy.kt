package com.example.agendavalenciana.ios

/** Pas 40: the response from community-submit.php is authoritative.
 * Android v336 publishes first and sends an automatic post-publication report for review.
 * We cannot claim a Sheets moderation record exists without an explicit server receipt.
 */
internal data class IosPublicationReceipt(
    val eventId: Int,
    val status: String,
    val reviewConfirmed: Boolean
) {
    val published: Boolean get() = status == "published"
    val message: String get() = when {
        published && reviewConfirmed -> "Esdeveniment publicat. El servidor ha confirmat l'enviament a revisió."
        published -> "Esdeveniment publicat. La revisió posterior es gestiona al servidor; no s'ha confirmat la cua de revisió."
        status == "pending" || status == "pending_review" ->
            "Esdeveniment rebut, pendent de revisió segons el servidor. Encara no és públic."
        status == "draft" -> "Esdeveniment desat com a esborrany al servidor; no és públic."
        else -> "Esdeveniment rebut pel servidor. No s'ha confirmat si ja és públic o està pendent de revisió."
    }
}

internal object IosPublicationReviewPolicy {
    fun fromServer(eventId: Int, status: String?, reviewStatus: String?, reviewQueued: Boolean?): IosPublicationReceipt {
        require(eventId > 0) { "El servidor no ha tornat un identificador d'esdeveniment." }
        val state = status?.trim()?.lowercase().orEmpty()
        require(state !in setOf("rejected", "error", "failed", "cancelled")) {
            "El servidor no ha acceptat la publicació."
        }
        // Do not infer review-sheet insertion from an ID or 'published' alone.
        val confirmed = reviewQueued ?: (reviewStatus?.trim()?.lowercase() in
            setOf("queued", "pending", "pending_review", "enqueued", "submitted"))
        return IosPublicationReceipt(eventId, state, confirmed)
    }
}
