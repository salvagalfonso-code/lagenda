package com.example.agendavalenciana.ios

import kotlinx.serialization.json.*

/** Artist classification from the existing entities/images.php contract.
 * A failed request never destroys the last verified entity metadata snapshot.
 */
object IosEntityAffinityCatalog {
    private const val TTL = 30L * 60L * 1000L
    private const val KEY = "ios_artist_subcategories_v1"
    private var restored = false
    private var snapshot: Map<String, Set<String>> = emptyMap()
    private var updated = 0L

    fun decodeResponse(root: JsonObject): Map<String, Set<String>> {
        val result = linkedMapOf<String, Set<String>>()
        root.items("entities").forEach { entity ->
            if (!entity.text("type", "entity_type").equals("ARTIST", true)) return@forEach
            val artist = IosGeoPlaces.key(entity.text("name", "display_name"))
            if (artist.isBlank()) return@forEach
            val raw = entity.text("style_raw", "style").replace(" / ", "|")
                .split('|',',',';') + entity.names("subcategories") +
                    entity.text("subcategories_codis").split('|',',',';')
            val codes = raw.map(IosStyleAffinityCatalog::canonical).filter(String::isNotBlank).toSet()
            if (codes.isNotEmpty()) result[artist] = codes
        }
        return result
    }
    private fun decodeSaved(raw: String): Map<String, Set<String>> = runCatching {
        Json.parseToJsonElement(raw).jsonObject.mapValues { (_, v) ->
            v.jsonArray.map { it.jsonPrimitive.content }.toSet()
        }
    }.getOrDefault(emptyMap())
    fun cached(): Map<String, Set<String>> {
        if (!restored) {
            restored = true
            snapshot = decodeSaved(platformReadPublicCache(KEY))
            updated = platformReadPublicCache("${KEY}_at").toLongOrNull() ?: 0L
        }
        return snapshot
    }
    suspend fun refresh(api: IosAgendaApi, force: Boolean = false): Map<String, Set<String>> {
        val old = cached()
        val age = currentEpochMillis() - updated
        if (!force && age in 0 until TTL && old.isNotEmpty()) return old
        return runCatching {
            val response = api.entityStyles()
            IosPersonalizationImages.ingest(response)
            val incoming = decodeResponse(response)
            if (incoming.isEmpty()) return@runCatching old
            snapshot = incoming
            updated = currentEpochMillis()
            val compact = buildJsonObject {
                incoming.forEach { (name, codes) ->
                    put(name, JsonArray(codes.sorted().map { JsonPrimitive(it) }))
                }
            }
            platformWritePublicCache(KEY, compact.toString())
            platformWritePublicCache("${KEY}_at", updated.toString())
            snapshot
        }.getOrDefault(old)
    }
}
