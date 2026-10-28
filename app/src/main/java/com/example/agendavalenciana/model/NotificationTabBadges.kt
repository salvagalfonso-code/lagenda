package com.example.agendavalenciana.model

/** Nombre d'avisos sense llegir en les pestanyes REALMENT accessibles. */
data class NotificationTabBadges(
    val general: Int,
    val upcoming: Int,
    val newEvents: Int,
    val community: Int,
    val tickets: Int,
    val suggestions: Int
)

object NotificationTabBadgeRules {
    fun count(
        notifications: List<AgendaNotification>,
        suggestions: List<Concert>,
        readIds: Set<String>,
        dismissedIds: Set<String>,
        upcomingEventIds: Set<Int>,
        newEventIds: Set<Int>,
        ticketEventIds: Set<Int>
    ): NotificationTabBadges {
        val unseen = NotificationGeneralRules.unseen(notifications, suggestions, readIds, dismissedIds)
        val unreadAlerts = notifications.filterNot { it.id in readIds || it.id in dismissedIds }
        fun countIn(type: NotificationType, availableIds: Set<Int>): Int = unreadAlerts
            .filter { it.type == type && it.concertId in availableIds }
            .distinctBy { it.concertId }
            .size
        return NotificationTabBadges(
            general = unseen.size,
            upcoming = countIn(NotificationType.REMINDER, upcomingEventIds),
            newEvents = unreadAlerts
                .filter { it.type == NotificationType.NEW_CONCERT && it.concertId in newEventIds }
                .distinctBy { it.concertId }
                .size,
            community = unreadAlerts
                .filter { it.type == NotificationType.COMMUNITY }
                .distinctBy { it.id }
                .size,
            tickets = countIn(NotificationType.TICKETS, ticketEventIds),
            suggestions = suggestions.count { NotificationGeneralRules.suggestionId(it.id) !in readIds }
        )
    }
}
