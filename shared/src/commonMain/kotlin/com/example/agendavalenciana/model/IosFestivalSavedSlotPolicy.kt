package com.example.agendavalenciana.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** Local iOS event-session bookmarks, scoped to the signed-in user by the caller.
 * They are NOT claimed to be synchronized with the server. */
object IosFestivalSavedSlotPolicy {
    fun key(eventId: Int, day: String, time: String, artist: String, title: String): String =
        listOf(eventId.toString(), day.trim(), time.trim(), artist.trim().lowercase(), title.trim().lowercase())
            .joinToString(separator = "|") { "${it.length}:$it" }

    fun encode(keys: Set<String>): String = JsonArray(keys.sorted().map { JsonPrimitive(it) }).toString()

    fun decode(raw: String): Set<String> = runCatching {
        (Json.parseToJsonElement(raw) as? JsonArray ?: JsonArray(emptyList())).mapNotNull {
            (it as? JsonPrimitive)?.contentOrNull
        }.filter(String::isNotEmpty).toSet()
    }.getOrDefault(emptySet())

}
