package com.example.agendavalenciana.ios

import kotlinx.serialization.json.*

/** Exact PHP Batecs contract shared with Android v336: registered author or admin writes;
 * signed-in guests may only read / mark seen, never create, update or delete.
 */
data class IosManualBatec(
    val id: Int, val eventId: Int, val title: String, val body: String,
    val author: String, val creatorId: Int, val canEdit: Boolean, val publishedAt: String
)
data class IosBatecEligibility(val allowed: Boolean, val organizer: Boolean,
                               val reason: String, val opensAt: String, val closesAt: String)

internal object IosBatecOperations {
    fun canonicalKey(raw: String): String {
        val key = com.example.agendavalenciana.model.IosBatecArtistGroups.canonicalSeenKey(raw)
        require(Regex("(manual|event):[0-9]+").matches(key)) { "Identificador de Batec invàlid" }
        return key
    }
    fun seenKeys(root: JsonObject): Set<String> {
        check(root["ok"]?.jsonPrimitive?.booleanOrNull == true)
        val keys = root["seen_keys"] as? JsonArray ?: error("Falten vistos")
        return keys.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            .filter { Regex("(manual|event):[0-9]+").matches(it) }.toSet()
    }
    fun verifySeenReceipt(root: JsonObject, key: String) {
        check(root["ok"]?.jsonPrimitive?.booleanOrNull == true &&
            root["seen"]?.jsonPrimitive?.booleanOrNull == true &&
            root["story_key"]?.jsonPrimitive?.contentOrNull == key) { "Vist no confirmat" }
    }
    fun validate(title: String, body: String): Pair<String, String> {
        val t = title.trim(); val b = body.trim()
        require(t.isNotBlank() && t.length <= 120) { "El títol ha de tindre entre 1 i 120 caràcters" }
        require(b.length <= 1000) { "El text no pot superar 1.000 caràcters" }
        return t to b
    }
    fun eligibility(root: JsonObject, eventId: Int): IosBatecEligibility {
        check(root["ok"]?.jsonPrimitive?.booleanOrNull == true &&
            root["event_id"]?.jsonPrimitive?.intOrNull == eventId)
        return IosBatecEligibility(
            root["allowed"]?.jsonPrimitive?.booleanOrNull == true,
            root["organizer"]?.jsonPrimitive?.booleanOrNull == true,
            root["reason"]?.jsonPrimitive?.contentOrNull.orEmpty().takeIf { it != "null" }.orEmpty(),
            root["opens_at"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            root["closes_at"]?.jsonPrimitive?.contentOrNull.orEmpty())
    }
    fun decode(obj: JsonObject, eventId: Int): IosManualBatec {
        val id = obj["id"]?.jsonPrimitive?.intOrNull ?: error("Batec sense ID")
        val event = obj["event_id"]?.jsonPrimitive?.intOrNull ?: error("Batec sense esdeveniment")
        require(id > 0 && event == eventId) { "Batec d'un altre esdeveniment" }
        return IosManualBatec(id, event,
            obj["title"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            obj["body"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            obj["author_name"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            obj["creator_user_id"]?.jsonPrimitive?.intOrNull ?: 0,
            obj["can_edit"]?.jsonPrimitive?.booleanOrNull == true,
            obj["published_at"]?.jsonPrimitive?.contentOrNull.orEmpty())
    }
    fun decodeList(root: JsonObject, eventId: Int): List<IosManualBatec> {
        check(root["ok"]?.jsonPrimitive?.booleanOrNull == true)
        val arr = root["batecs"] as? JsonArray ?: error("Falta llista de batecs")
        return arr.map { decode(it as? JsonObject ?: error("Batec invàlid"), eventId) }
    }
    fun verifyMutation(root: JsonObject, eventId: Int, title: String,
                       body: String, expectedId: Int? = null): IosManualBatec {
        check(root["ok"]?.jsonPrimitive?.booleanOrNull == true)
        val item = decode(root["batec"] as? JsonObject ?: error("Falta el Batec"), eventId)
        check(item.title == title && item.body == body) { "El servidor no confirma el mateix contingut" }
        if (expectedId != null) check(item.id == expectedId) { "S'ha editat un altre Batec" }
        // Admins can edit another author's post. The server's can_edit is authoritative.
        check(item.canEdit) { "El servidor no confirma el permís d'edició" }
        return item
    }
    fun verifyDelete(root: JsonObject, id: Int) {
        check(root["ok"]?.jsonPrimitive?.booleanOrNull == true &&
            root["id"]?.jsonPrimitive?.intOrNull == id) { "Eliminació no confirmada" }
    }
}
