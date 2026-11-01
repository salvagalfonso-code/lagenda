package com.example.agendavalenciana.ios

/** UI safeguards; server authentication and authorization are always authoritative. */
object IosCommunityPolicy {
    fun displayMessage(authorUserId: Int, blockedUsers: Set<Int>): Boolean =
        authorUserId <= 0 || authorUserId !in blockedUsers

    fun canRequestAccess(status: String, membership: String): Boolean =
        status == "OPEN" && membership !in setOf("ACCEPTED", "PENDING")

    fun canonicalMembership(value: String): String = value.uppercase().takeIf {
        it in setOf("ACCEPTED", "PENDING", "REJECTED", "LEFT", "CONTACT", "NONE")
    } ?: "NONE"

    fun verifiedJoin(posted: String, groupId: Int, returnedId: Int, serverStatus: String,
                     expectedStatus: String): Boolean = groupId > 0 && returnedId == groupId &&
        canonicalMembership(posted) == canonicalMembership(expectedStatus) &&
        canonicalMembership(serverStatus) == canonicalMembership(expectedStatus) &&
        expectedStatus in setOf("ACCEPTED", "PENDING")

    fun canOpenChat(membership: String, chatEnabled: Boolean): Boolean =
        membership == "ACCEPTED" && chatEnabled

    fun canStartPrivateInquiry(isCreator: Boolean, groupStatus: String, peerUserId: Int): Boolean =
        if (isCreator) peerUserId > 0 else groupStatus !in setOf("CLOSED", "CANCELLED", "ARCHIVED")

    /** A post to this endpoint is not idempotent: never replay it in the offline queue. */
    fun validMessage(text: String): Boolean = text.isNotBlank() && text.trim().length <= 2000

    fun verifiedSentMessage(groupId: Int, senderId: Int, text: String, resultId: Int,
                            resultGroup: Int, resultSender: Int, resultBody: String): Boolean =
        groupId > 0 && senderId > 0 && resultId > 0 && resultGroup == groupId &&
            resultSender == senderId && text.trim() == resultBody


    fun canSend(status: String, membership: String): Boolean =
        status in setOf("OPEN", "FULL") && membership == "ACCEPTED"

    fun isCreator(creatorId: Int, currentId: Int): Boolean =
        creatorId > 0 && creatorId == currentId

    fun canModerateMember(creatorId: Int, actorId: Int, targetId: Int): Boolean =
        isCreator(creatorId, actorId) && targetId > 0 && targetId != actorId

    fun canSeeAdminQueue(role: String): Boolean = role.uppercase() == "SUPER_ADMIN"
}
