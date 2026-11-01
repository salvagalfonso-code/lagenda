package com.example.agendavalenciana.model

/** Bústia unificada: només elements encara nous, amb els IDs originals de cada secció. */
object NotificationGeneralRules {
    fun suggestionId(eventId: Int): String = "suggestion_$eventId"

    fun unseen(
        notifications: List<AgendaNotification>,
        suggestions: List<Concert>,
        readIds: Set<String>,
        dismissedIds: Set<String> = emptySet()
    ): List<AgendaNotification> {
        val alertItems = notifications.asSequence()
            .filterNot { it.id in readIds || it.id in dismissedIds }
        val suggestionItems = suggestions.asSequence()
            .filterNot { suggestionId(it.id) in readIds }
            .map { concert ->
                AgendaNotification(
                    id = suggestionId(concert.id),
                    type = NotificationType.SUGGESTION,
                    title = concert.title,
                    body = "${AgendaDateFormat.date(concert.date)} · ${concert.town}",
                    concertId = concert.id,
                    sortKey = concert.createdAtMillis.coerceAtLeast(0L)
                )
            }
        // Distinct per ID de notificació, no per concert: un mateix esdeveniment
        // pot originar avisos legítims distints a Comunitat, Entrades i Pròxim.
        return (alertItems + suggestionItems)
            .distinctBy { it.id }
            .sortedWith(compareByDescending<AgendaNotification> { it.sortKey }.thenBy { it.id })
            .toList()
    }
}
