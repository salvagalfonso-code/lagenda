package com.example.agendavalenciana.ios

import kotlinx.serialization.json.*

/** Android v336 events/save.php and events/saved.php contracts.
 * Use the stable event ID, never a feed occurrence, translated label or local cache key.
 */
internal object IosSavedEventContract {
    fun payload(event: AgendaEventSummary, saved: Boolean, userTown: String = "", userComarca: String = ""): JsonObject {
        require(event.id > 0) { "Identificador d'esdeveniment invàlid" }
        return buildJsonObject {
            put("event_id", event.id)
            put("entity_label", event.title)
            put("saved", saved)
            // Match the contextual affinity metadata sent by Android v336; only the
            // event ID and saved bit are used as the primary save/delete identity.
            put("user_town", userTown.trim())
            put("user_comarca", userComarca.trim())
            put("town", event.town)
            put("comarca", "") // comarca absent from this public catalog projection
            put("category", event.category)
            put("language", event.language)
            put("primary_style", event.styles.firstOrNull().orEmpty())
            put("primary_artist", event.artists.firstOrNull().orEmpty())
            put("organizer", event.organizers.firstOrNull().orEmpty())
            put("venue", event.venue)
            put("styles_csv", event.styles.joinToString(" | "))
        }
    }

    fun expected(payload: JsonObject): Pair<Int, Boolean> {
        val eventId = payload["event_id"]?.jsonPrimitive?.intOrNull ?: error("Falta event_id")
        require(eventId > 0) { "ID invàlid" }
        val desired = payload["saved"]?.jsonPrimitive?.booleanOrNull ?: error("Falta estat saved")
        return eventId to desired
    }

    fun verifyPost(response: JsonObject, payload: JsonObject) {
        val (_, desired) = expected(payload)
        require(response["ok"]?.jsonPrimitive?.booleanOrNull == true) { "API de guardats sense confirmació" }
        require(response["saved"]?.jsonPrimitive?.booleanOrNull == desired) {
            "L'API no confirma el guardat o eliminació demanat"
        }
        require((response["saved_count"]?.jsonPrimitive?.intOrNull ?: -1) >= 0) {
            "Falta el recompte de guardats del servidor"
        }
    }

    /** events/saved.php is scoped by bearer to the same user in MariaDB. */
    fun verifyCanonicalList(response: JsonObject, payload: JsonObject) {
        val (eventId, desired) = expected(payload)
        require(response["ok"]?.jsonPrimitive?.booleanOrNull == true) { "Llista de guardats invàlida" }
        val ids = response["event_ids"] as? JsonArray ?: error("Falta event_ids")
        val validIds = ids.map { (it as? JsonPrimitive)?.intOrNull ?: error("ID de guardat invàlid") }
        require(validIds.all { it > 0 }) { "IDs invàlids en servidor" }
        check((eventId in validIds) == desired) { "Guardat encara no confirmat en MariaDB" }
    }

    /** An older GET cannot undo any acknowledged-to-disk but unconfirmed write. */
    fun project(serverSaved: Set<Int>, pendingPayloads: List<JsonObject>): Set<Int> {
        val result = serverSaved.toMutableSet()
        pendingPayloads.forEach { body ->
            val (eventId, wantSaved) = expected(body)
            if (wantSaved) result.add(eventId) else result.remove(eventId)
        }
        return result
    }
}
