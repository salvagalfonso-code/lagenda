package com.example.agendavalenciana.ios

/** Pas 41: compare the full editable public projection, not just title/town.
 * Missing server fields or a pending review are not proof that an edit succeeded.
 */
internal object IosPublicationEditPolicy {
    private fun unique(list: List<String>) = list.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    fun confirmed(beforeId: Int, draft: IosPublishDraft, actual: IosPublishedEvent?): Boolean {
        if (actual == null || actual.eventId != beforeId || !actual.status.equals("published", true)) return false
        return actual.title.trim() == draft.title.trim() && actual.subtitle.trim() == draft.subtitle.trim() &&
            actual.date == draft.date && actual.time.take(5) == draft.time &&
            actual.endDate == draft.endDate && actual.endTime.take(5) == draft.endTime &&
            actual.town.trim() == draft.town.trim() && actual.comarca.trim() == draft.comarca.trim() &&
            actual.venue.trim() == draft.venue.trim() && actual.category.equals(draft.category, true) &&
            actual.description.trim() == draft.description.trim() &&
            unique(actual.artists) == unique(draft.artists) &&
            unique(actual.organizers) == unique(draft.organizers) &&
            unique(actual.styles) == unique(draft.styles) &&
            actual.price == draft.price && actual.ticketEnabled == (draft.ticketChannel != "NONE") &&
            (draft.ticketChannel != "EXTERNAL" || actual.ticketUrl.trim() == draft.ticketUrl.trim())
    }
    fun posterConfirmed(expected: String, actual: String): Boolean =
        expected.startsWith("https://") && actual.startsWith("https://") &&
            expected.substringBefore('?') == actual.substringBefore('?')
}

internal data class IosSpecialScheduleSlot(
    val id: Long, val eventId: Int, val title: String, val date: String, val startTime: String,
    val endTime: String = "", val stage: String = "", val artist: String = "",
    val details: String = "", val imageUrl: String = "", val sortOrder: Int = 0,
    val artistId: Long = 0L
) {
    fun validate() {
        require(eventId > 0 && id >= 0 && artistId >= 0 && title.isNotBlank() && title.length <= 220)
        require(date.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) && startTime.matches(Regex("\\d{2}:\\d{2}")))
        require(endTime.isBlank() || endTime.matches(Regex("\\d{2}:\\d{2}")))
        require(endTime.isBlank() || endTime >= startTime) { "Hora de finalització anterior a l'inici" }
        require(imageUrl.isBlank() || imageUrl.startsWith("https://"))
    }
}

internal data class IosSpecialScheduleAccess(val eventId: Int, val canEdit: Boolean,
    val slots: List<IosSpecialScheduleSlot>)

internal object IosSpecialSchedulePolicy {
    fun confirmed(expected: IosSpecialScheduleSlot, found: List<IosSpecialScheduleSlot>, beforeIds: Set<Long>): Boolean {
        val candidates = if (expected.id > 0) found.filter { it.id == expected.id }
            else found.filter { it.id > 0 && it.id !in beforeIds }
        return candidates.any { actual -> actual.eventId == expected.eventId &&
            actual.title.trim() == expected.title.trim() && actual.date == expected.date &&
            actual.startTime.take(5) == expected.startTime && actual.endTime.take(5) == expected.endTime &&
            actual.stage.trim() == expected.stage.trim() && actual.artist.trim() == expected.artist.trim() &&
            actual.details.trim() == expected.details.trim() }
    }
}
