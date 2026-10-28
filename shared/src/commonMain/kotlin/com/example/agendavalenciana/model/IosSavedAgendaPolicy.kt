package com.example.agendavalenciana.model

/** The saved festivals store a length-prefixed key rather than a serialized screenshot.
 * Decode only keys created by IosFestivalSavedSlotPolicy.key; malformed data is ignored.
 * The source festival must still exist in the public catalog before it can be displayed. */
data class IosSavedFestivalSlot(
    val eventId: Int, val day: String, val time: String,
    val artist: String, val title: String, val originalKey: String
)

object IosSavedAgendaPolicy {
    fun decodeSlotKey(raw: String): IosSavedFestivalSlot? {
        var cursor = 0
        val parts = ArrayList<String>(5)
        repeat(5) {
            val colon = raw.indexOf(':', cursor)
            if (colon < cursor || colon - cursor > 8) return null
            val length = raw.substring(cursor, colon).toIntOrNull() ?: return null
            if (length < 0 || length > 1000) return null
            val end = colon + 1 + length
            if (end > raw.length) return null
            parts.add(raw.substring(colon + 1, end))
            cursor = end
            if (it < 4) {
                if (cursor >= raw.length || raw[cursor] != '|') return null
                cursor++
            }
        }
        if (cursor != raw.length) return null
        val id = parts[0].toIntOrNull()?.takeIf { it > 0 } ?: return null
        val day = parts[1].take(10)
        if (!day.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return null
        return IosSavedFestivalSlot(id, day, parts[2], parts[3], parts[4], raw)
    }

    fun validEventIds(selected: Set<Int>, allListEventIds: Set<Int>): Set<Int> =
        selected + allListEventIds

    /** A festival is valid through its last date, not just the opening date. */
    fun eventStillUpcoming(startDate: String, endDate: String, today: String): Boolean {
        val last = endDate.ifBlank { startDate }.take(10)
        return last.isNotBlank() && last >= today
    }

    fun activeSlots(rawKeys: Set<String>, festivalId: Int, today: String): List<IosSavedFestivalSlot> =
        rawKeys.mapNotNull(::decodeSlotKey).filter { it.eventId == festivalId && it.day >= today }
            .distinctBy { it.originalKey }.sortedWith(compareBy<IosSavedFestivalSlot> { it.day }
                .thenBy { it.time }.thenBy { it.artist })
}
