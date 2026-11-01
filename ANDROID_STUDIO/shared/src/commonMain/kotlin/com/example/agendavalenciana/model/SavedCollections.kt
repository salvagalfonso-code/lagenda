package com.example.agendavalenciana.model

/** Llistes personals independents del guardat general. */
data class PersonalSavedList(
    val id: Int,
    val name: String,
    val eventIds: Set<Int>,
    val owner: Boolean = true,
    val inviteUrl: String = "",
    val shareCode: String = ""
)

/** Grup d’amics persistent, no vinculat a un únic esdeveniment. */
data class FriendsCircle(
    val id: Int,
    val name: String,
    val eventIds: Set<Int>,
    val membersCount: Int,
    val unreadCount: Int,
    val inviteUrl: String,
    val creator: Boolean
)

data class FriendsCircleMessage(
    val id: Int,
    val groupId: Int,
    val author: String,
    val text: String,
    val eventId: Int?,
    val createdAt: String
)

/** Novetat nova al grup d’amics per a la campaneta de Comunitat. */
data class FriendsCircleNotice(
    val id: Int,
    val groupId: Int,
    val groupName: String,
    val eventId: Int?,
    val eventTitle: String,
    val createdAt: String,
    val kind: String = "EVENT_ADDED",
    val message: String = "",
    val author: String = ""
)
