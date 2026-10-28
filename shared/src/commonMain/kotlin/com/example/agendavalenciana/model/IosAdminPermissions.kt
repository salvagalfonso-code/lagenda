package com.example.agendavalenciana.model

/** Secondary UI check; PHP session guards remain authoritative. */
object IosAdminPermissions {
    val roles = listOf("PUBLIC", "USER", "GROUP", "ORGANIZER", "VENUE", "PROMOTER", "LABEL", "AGENCY", "SUPER_ADMIN")
    fun canOpen(role: String) = role == "SUPER_ADMIN"
    fun validRole(role: String) = role in roles
    fun canEdit(actorId: Int, actorRole: String, targetId: Int): Boolean =
        canOpen(actorRole) && actorId > 0 && targetId > 0 && actorId != targetId
    fun validTarget(allUsers: Boolean, followArtist: String, savedEvent: Int): Boolean =
        allUsers || followArtist.trim().isNotEmpty() || savedEvent > 0
    fun canConfirmRemoval(eventId: Int, explanation: String): Boolean =
        eventId > 0 && explanation.trim().length >= 12
}
