package com.example.agendavalenciana.ios

/** Categories backed only by explicit, unambiguous notification deep-link types.
 * The current server sends no category field; don't pretend an ordinary EVENT alert is a reminder.
 */
object IosNotificationGrouping {
    const val GENERAL = "General"
    const val UPCOMING = "Pròxim"
    const val NEW = "Nous"
    const val TICKETS = "Entrades"
    const val SUGGESTED = "Suggerències"
    const val COMMUNITY = "Comunitat"

    fun fromDeepLinkType(value: String): String = when (value.trim().uppercase()) {
        "TICKET", "TICKETS", "TICKET_ID", "ORDER", "TICKET_ORDER" -> TICKETS
        "COMMUNITY", "COMMUNITY_GROUP", "COMMUNITY_MESSAGE", "BOARD" -> COMMUNITY
        "UPCOMING", "UPCOMING_EVENT", "EVENT_REMINDER", "REMINDER" -> UPCOMING
        "NEW", "NEW_EVENT", "NEW_ARTIST_EVENT", "FOLLOWED_ARTIST_NEW" -> NEW
        "SUGGESTION", "SUGGESTED_EVENT", "RECOMMENDATION" -> SUGGESTED
        else -> GENERAL
    }
}
