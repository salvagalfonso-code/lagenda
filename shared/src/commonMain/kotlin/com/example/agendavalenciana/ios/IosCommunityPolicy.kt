package com.example.agendavalenciana.ios

/** UI safeguards; server authentication and authorization are always authoritative. */
object IosCommunityPolicy {
    fun displayMessage(authorUserId: Int, blockedUsers: Set<Int>): Boolean =
        authorUserId <= 0 || authorUserId !in blockedUsers

    fun canRequestAccess(status: String, membership: String): Boolean =
        status == "OPEN" && membership !in setOf("ACCEPTED", "PENDING")

    fun canSend(status: String, membership: String): Boolean =
        status == "OPEN" && membership == "ACCEPTED"

    fun isCreator(creatorId: Int, currentId: Int): Boolean =
        creatorId > 0 && creatorId == currentId

    fun canModerateMember(creatorId: Int, actorId: Int, targetId: Int): Boolean =
        isCreator(creatorId, actorId) && targetId > 0 && targetId != actorId

    fun canSeeAdminQueue(role: String): Boolean = role.uppercase() == "SUPER_ADMIN"
}
