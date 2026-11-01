package com.example.agendavalenciana.ios

import kotlinx.serialization.json.*

/** Same MariaDB visibility endpoint and entity keys as frozen Android v336. */
internal data class IosVisibilitySnapshot(
    val events: Set<Int> = emptySet(),
    val artists: Set<String> = emptySet(),
    val categories: Set<String> = emptySet()
) {
    companion object {
        fun decode(response: JsonObject): IosVisibilitySnapshot {
            require(response["ok"]?.jsonPrimitive?.booleanOrNull == true) { "Visibilitat no confirmada" }
            val root = response["exclusions"] as? JsonObject ?: error("Falten exclusions")
            fun values(field: String): Set<String> = (root[field] as? JsonArray
                ?: error("Falta el camp $field")).map { it.jsonPrimitive.content }.toSet()
            return IosVisibilitySnapshot(values("events").mapNotNull { it.toIntOrNull()?.takeIf { n -> n > 0 } }.toSet(),
                values("artists"), values("categories"))
        }
    }
}
internal data class IosVisibilityMutation(val kind: String, val target: String, val action: String) {
    init {
        require(kind in setOf("EVENT", "ARTIST", "CATEGORY"))
        require(action == "add" || action == "remove")
        require(target.isNotBlank() && target.length <= 190)
        if (kind == "EVENT") require(target.toIntOrNull()?.let { it > 0 } == true)
    }
    fun payload(): JsonObject = buildJsonObject {
        put("action", action); put("kind", kind); put("target", target)
    }
}
internal object IosVisibilityAffinityContract {
    fun mutations(previous: IosVisibilitySnapshot, desired: IosVisibilitySnapshot): List<IosVisibilityMutation> {
        val mutations = mutableListOf<IosVisibilityMutation>()
        fun update(kind: String, before: Set<String>, after: Set<String>) {
            (before - after).sorted().forEach { mutations += IosVisibilityMutation(kind, it, "remove") }
            (after - before).sorted().forEach { mutations += IosVisibilityMutation(kind, it, "add") }
        }
        update("EVENT", previous.events.map(Int::toString).toSet(), desired.events.map(Int::toString).toSet())
        update("ARTIST", previous.artists, desired.artists)
        update("CATEGORY", previous.categories, desired.categories)
        return mutations
    }
    fun apply(base: IosVisibilitySnapshot, mutation: IosVisibilityMutation): IosVisibilitySnapshot {
        val adding = mutation.action == "add"
        fun updated(values: Set<String>): Set<String> = if (adding) values + mutation.target else values - mutation.target
        return when (mutation.kind) {
            "EVENT" -> base.copy(events = updated(base.events.map(Int::toString).toSet()).map(String::toInt).toSet())
            "ARTIST" -> base.copy(artists = updated(base.artists))
            else -> base.copy(categories = updated(base.categories))
        }
    }
    fun confirm(snapshot: IosVisibilitySnapshot, mutation: IosVisibilityMutation): Boolean {
        val present = when (mutation.kind) {
            "EVENT" -> mutation.target.toInt() in snapshot.events
            "ARTIST" -> mutation.target in snapshot.artists
            else -> mutation.target in snapshot.categories
        }
        return present == (mutation.action == "add")
    }
}

/** Affinities are returned by user-state/mine.php, never created from an iOS-only preference. */
data class IosServerAffinity(val type: String, val key: String, val label: String,
    val effectiveScore: Double, val permanent: Boolean) {
    companion object {
        fun decode(root: JsonObject): IosServerAffinity? {
            val type = root.text("entity_type").uppercase()
            val key = root.text("entity_key")
            if (type.isBlank() || key.isBlank()) return null
            return IosServerAffinity(type,key,root.text("entity_label"),
                root.text("effective_score").toDoubleOrNull()?.coerceIn(0.0,100.0) ?: 0.0,
                root.bool("permanent"))
        }
    }
}
internal object IosAffinitySignals {
    fun search(term: String, dedupe: String): JsonObject {
        val value = term.trim().take(190)
        require(value.length >= 3 && dedupe.isNotBlank())
        return buildJsonObject {
            put("action_type","SEARCH_FREE"); put("entity_type","RAW")
            put("entity_key",value); put("entity_label",value); put("dedupe_key",dedupe)
        }
    }
    fun open(event: AgendaEventSummary, dedupe: String): JsonObject {
        require(event.id > 0 && dedupe.isNotBlank())
        return buildJsonObject {
            put("action_type","OPEN_EVENT"); put("entity_type","EVENT")
            put("entity_key",event.id.toString()); put("entity_label",event.title.take(190))
            put("event_id",event.id); put("town",event.town); put("category",event.category)
            put("language",event.language); put("primary_artist",event.artists.firstOrNull().orEmpty())
            put("venue",event.venue); put("styles_csv",event.styles.joinToString(",").take(500))
            put("dedupe_key",dedupe)
        }
    }
    fun styleWeights(affinities: List<IosServerAffinity>): Map<String, Int> = affinities
        .filter { it.type == "STYLE" && it.effectiveScore > 0.0 }
        .groupBy { IosStyleAffinityCatalog.canonical(it.key) }
        .filterKeys(String::isNotBlank)
        .mapValues { (_, rows) -> (rows.maxOfOrNull { it.effectiveScore } ?: 0.0).toInt().coerceAtLeast(1) }
}
