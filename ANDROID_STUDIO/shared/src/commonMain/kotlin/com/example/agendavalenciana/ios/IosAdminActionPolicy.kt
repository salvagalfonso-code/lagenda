package com.example.agendavalenciana.ios

/** Client-side safeguards. The API must still enforce SUPER_ADMIN and ownership. */
internal object IosAdminActionPolicy {
    fun validProfessionalClaim(id: Int, accept: Boolean, notes: String): Boolean =
        id > 0 && (accept || notes.trim().length >= 12)

    fun validReview(eventId: Int, decision: String, notes: String): Boolean =
        eventId > 0 && decision in setOf("APPROVE", "REJECT") &&
            (decision == "APPROVE" || notes.trim().length >= 12)

    fun validModeration(eventId: Int, action: String, reason: String): Boolean =
        eventId > 0 && action in setOf("APPROVE", "REMOVE_CONFIRMED", "RESTORE") &&
            (action != "REMOVE_CONFIRMED" || reason.trim().length >= 12)

    fun validCampaign(title: String, body: String, count: Int): Boolean =
        title.trim().length in 1..160 && body.trim().length in 1..1200 && count > 0
}
