package com.example.agendavalenciana.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** Per-performance identity kept in the server state (sync/state.php), never in UserDefaults.
 * The sixth length-prefixed field is the location/stage; five fields remain readable
 * for saved v349 entries. Android's UI key instead uses date|time|location|artist. */
object IosFestivalSavedSlotPolicy {
    fun key(eventId: Int, day: String, time: String, artist: String, title: String,
            location: String = ""): String =
        (listOf(eventId.toString(), day.trim().take(10), time.trim().take(5),
            artist.trim().lowercase(), title.trim().lowercase()) +
            location.trim().takeIf(String::isNotBlank).let { if (it != null) listOf(it.lowercase()) else emptyList() })
            .joinToString(separator = "|") { "${it.length}:$it" }

    /** Byte-compatible selection used by Android's FestivalPopup.scheduleSelectionKey. */
    fun androidKey(day: String, time: String, location: String, artist: String, title: String): String =
        listOf(day.take(10), time, location, artist.ifBlank { title }).joinToString("|") { it.trim() }

    fun encode(keys: Set<String>): String = JsonArray(keys.sorted().map { JsonPrimitive(it) }).toString()

    fun decode(raw: String): Set<String> = runCatching {
        (Json.parseToJsonElement(raw) as? JsonArray ?: JsonArray(emptyList())).mapNotNull {
            (it as? JsonPrimitive)?.contentOrNull
        }.filter(String::isNotEmpty).toSet()
    }.getOrDefault(emptySet())

}
