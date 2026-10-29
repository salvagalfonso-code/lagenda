package com.example.agendavalenciana.ios

import kotlinx.serialization.json.*

/** Fotografia CANÒNICA per entitat, amb el mateix origen entities/images.php que Android.
 * No fem passar el cartell d'un esdeveniment per una foto d'artista o categoria.
 */
object IosPersonalizationImages {
    private const val KEY = "ios_personalization_entity_images_v1"
    private var loaded = false
    private var snapshot: Map<String, String> = emptyMap()
    fun id(type: String, name: String) = "${type.uppercase()}:${IosGeoPlaces.key(name)}"
    fun cached(): Map<String,String> {
        if (!loaded) {
            loaded = true
            snapshot = runCatching {
                Json.parseToJsonElement(platformReadPublicCache(KEY)).jsonObject.mapValues { it.value.jsonPrimitive.content }
            }.getOrDefault(emptyMap())
        }
        return snapshot
    }
    fun ingest(root: JsonObject): Map<String,String> {
        val out = mutableMapOf<String,String>()
        root.items("entities").forEach { e ->
            val type = e.text("type","entity_type").uppercase()
            val name = e.text("name","display_name")
            if (type !in setOf("ARTIST", "CATEGORY", "TOWN", "VENUE", "ORGANIZER", "FESTIVAL") || name.isBlank()) return@forEach
            val url = validMediaUrl(e.text("image_url"))
            if (url.isNotBlank()) out[id(type,name)] = url
        }
        if (out.isNotEmpty()) {
            snapshot = out; loaded = true
            platformWritePublicCache(KEY, buildJsonObject {
                out.forEach { (key,url) -> put(key, url) }
            }.toString())
        }
        return cached()
    }
    fun photo(images: Map<String,String>, type: String, name: String): String =
        images[id(type,name)].orEmpty()
}
