package com.example.agendavalenciana.model

/** The saved festivals store a length-prefixed key rather than a serialized screenshot.
 * Decode only keys created by IosFestivalSavedSlotPolicy.key; malformed data is ignored.
 * The source festival must still exist in the public catalog before it can be displayed. */
data class IosSavedFestivalSlot(
    val eventId: Int, val day: String, val time: String,
    val artist: String, val title: String, val originalKey: String,
    val location: String = ""
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
        // v349 format had five parts. v2 appends stage/location without invalidating v1.
        val location = if (cursor == raw.length) "" else {
            if (raw[cursor] != '|') return null
            cursor++
            val colon = raw.indexOf(':', cursor)
            if (colon < cursor || colon - cursor > 8) return null
            val length = raw.substring(cursor, colon).toIntOrNull() ?: return null
            if (length < 0 || length > 1000 || colon + 1 + length != raw.length) return null
            raw.substring(colon + 1)
        }
        val id = parts[0].toIntOrNull()?.takeIf { it > 0 } ?: return null
        val day = parts[1]
        if (!validDate(day)) return null
        return IosSavedFestivalSlot(id, day, parts[2], parts[3], parts[4], raw, location)
    }

    fun validEventIds(selected: Set<Int>, allListEventIds: Set<Int>): Set<Int> =
        selected + allListEventIds

    /** Android SavedAgendaWindow: use the last *real* day (schedule included), and
     * keep incomplete/invalid dates visible rather than hide them accidentally. */
    fun validDate(day: String): Boolean {
        if (day.length != 10 || day[4] != '-' || day[7] != '-') return false
        val year = day.substring(0, 4).toIntOrNull() ?: return false
        val month = day.substring(5, 7).toIntOrNull() ?: return false
        val number = day.substring(8, 10).toIntOrNull() ?: return false
        if (year !in 1..9999 || month !in 1..12) return false
        val max = when (month) {
            2 -> if (year % 400 == 0 || (year % 4 == 0 && year % 100 != 0)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
        return number in 1..max
    }

    fun eventStillUpcoming(startDate: String, endDate: String, today: String,
                           scheduleDates: Collection<String> = emptyList()): Boolean {
        val latest = (listOf(startDate, endDate) + scheduleDates)
            .map { it.trim().take(10) }.filter(::validDate).maxOrNull()
        return latest == null || latest >= today
    }

    /** A saved concert must match a real session, as on Android. Missing sessions do
     * not become invented timeline rows, and two venues at the same hour stay distinct.
     * Old 5-field bookmarks are resolved against the actual schedule. */
    fun matchSlots(rawKeys: Set<String>, eventId: Int,
                   schedule: List<IosFestivalSession>, today: String): List<IosSavedFestivalSlot> {
        val selected = rawKeys.mapNotNull(::decodeSlotKey).filter { it.eventId == eventId && it.day >= today }
        return schedule.asSequence().filter { validDate(it.day) && it.day >= today }
            .mapNotNull { session ->
                val saved = selected.firstOrNull { key ->
                    key.day == session.day && key.time == session.time &&
                    (key.location.isEmpty() || key.location.equals(session.location, true)) &&
                    key.artist.equals(session.artist, true) && key.title.equals(session.title, true)
                } ?: return@mapNotNull null
                IosSavedFestivalSlot(eventId, session.day, session.time, session.artist,
                    session.title, saved.originalKey, session.location)
            }.distinctBy { listOf(it.day, it.time, it.location, it.artist, it.title) }
            .sortedWith(compareBy<IosSavedFestivalSlot> { it.day }.thenBy { it.time }
                .thenBy { it.location }.thenBy { it.artist }).toList()
    }

    /** Removes all legacy and venue-aware keys identifying one real performance. */
    fun withoutSession(rawKeys: Set<String>, selected: IosSavedFestivalSlot): Set<String> =
        rawKeys.filterNot { raw ->
            val old = decodeSlotKey(raw) ?: return@filterNot false
            old.eventId == selected.eventId && old.day == selected.day &&
                old.time == selected.time && old.artist.equals(selected.artist, true) &&
                old.title.equals(selected.title, true) &&
                (old.location.isBlank() || old.location.equals(selected.location, true))
        }.toSet()

    fun activeSlots(rawKeys: Set<String>, festivalId: Int, today: String): List<IosSavedFestivalSlot> =
        rawKeys.mapNotNull(::decodeSlotKey).filter { it.eventId == festivalId && it.day >= today }
            .distinctBy { it.originalKey }.sortedWith(compareBy<IosSavedFestivalSlot> { it.day }
                .thenBy { it.time }.thenBy { it.artist })
}

/** Minimal schedule shape decoupled from API decoding for deterministic JVM tests. */
data class IosFestivalSession(val day: String, val time: String, val artist: String,
                              val title: String, val location: String)
