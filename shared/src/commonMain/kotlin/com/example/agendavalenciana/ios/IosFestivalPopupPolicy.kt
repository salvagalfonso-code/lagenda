package com.example.agendavalenciana.ios

import com.example.agendavalenciana.model.IosFestivalSavedSlotPolicy

/** Step 26: festival-only projection over published special-events/public.php rows.
 * Never expand date ranges into invented performances. Android v336 is read-only.
 * Dates passed to upcomingDays must be the 05:00 Europe/Madrid cultural date. */
object IosFestivalPopupPolicy {
    private val isoDate = Regex("\\d{4}-\\d{2}-\\d{2}")

    fun upcomingDays(rows: List<AgendaScheduleEntry>, culturalToday: String): List<String> =
        rows.map { it.date.take(10) }.filter { isoDate.matches(it) }
            .filter { culturalToday.isBlank() || it >= culturalToday }
            .distinct().sorted()

    fun initialDay(days: List<String>, focused: AgendaScheduleEntry?, preferred: String): String =
        sequenceOf(focused?.date?.take(10), preferred.take(10), days.firstOrNull())
            .filterNotNull().firstOrNull { it in days }.orEmpty()

    /** Prefer published API metadata, but retain catalogue sessions absent from a partial response.
     * Both inputs contain actual programme rows, not dates inferred from the festival span. */
    fun mergePublished(published: List<AgendaScheduleEntry>, catalogue: List<AgendaScheduleEntry>): List<AgendaScheduleEntry> {
        if (published.isEmpty()) return catalogue
        fun identity(row: AgendaScheduleEntry): List<String> = listOf(
            row.date.take(10), row.time.take(5), row.location.trim().lowercase(),
            row.artist.ifBlank { row.title }.trim().lowercase())
        return (published + catalogue).distinctBy(::identity)
    }

    fun isGroupMarker(row: AgendaScheduleEntry): Boolean =
        row.groupName.isNotBlank() && row.itemType.equals("activity", true) &&
            row.title.trim().equals(row.groupName.trim(), true)

    fun saved(eventId: Int, row: AgendaScheduleEntry, keys: Set<String>): Boolean {
        val modern = IosFestivalSavedSlotPolicy.key(eventId, row.date, row.time, row.artist, row.title, row.location)
        val legacy = IosFestivalSavedSlotPolicy.key(eventId, row.date, row.time, row.artist, row.title)
        return modern in keys || legacy in keys
    }

    fun performances(rows: List<AgendaScheduleEntry>, day: String, onlyMine: Boolean,
                     keys: Set<String>, eventId: Int): List<AgendaScheduleEntry> =
        rows.asSequence().filter { day.isNotBlank() && it.date.take(10) == day }
            .filterNot(::isGroupMarker)
            .distinctBy { IosFestivalSavedSlotPolicy.key(eventId, it.date, it.time, it.artist, it.title, it.location) }
            .filter { !onlyMine || saved(eventId, it, keys) }
            .sortedWith(compareBy<AgendaScheduleEntry> { it.sortOrder }
                .thenBy { it.time.ifBlank { "99:99" } }.thenBy { it.location }.thenBy { it.title })
            .toList()

    /** Festival stages and auxiliary programmed places, never geocode invented coordinates. */
    fun places(venue: String, town: String, rows: List<AgendaScheduleEntry>): List<Pair<String, String>> {
        val result = linkedMapOf<String, Pair<String, String>>()
        fun append(label: String, query: String) {
            val k = label.trim().lowercase()
            if (k.isNotBlank() && query.isNotBlank()) result.getOrPut(k) { label.trim() to query.trim() }
        }
        val base = listOf(venue, town).filter(String::isNotBlank).distinct().joinToString(", ")
        append(venue.ifBlank { town }, base)
        rows.forEach { r ->
            if (r.location.isNotBlank() && !r.location.equals(venue, true))
                append(r.location, listOf(r.location, venue, town).filter(String::isNotBlank)
                    .distinct().joinToString(", "))
        }
        return result.values.toList()
    }

    fun ticketsForDay(options: List<IosTicketOption>, day: String): List<IosTicketOption> =
        options.filter { day.isBlank() || it.sessionDate.isBlank() || it.sessionDate.take(10) == day }
}
