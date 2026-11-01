package com.example.agendavalenciana.ios

import com.example.agendavalenciana.model.DiscoveryLevel
import kotlinx.serialization.json.*

/** Step 12: only server-confirmed IDs can represent an Android prepared snapshot.
 * The independent server-v1 calculation is a separate, explicitly labelled fallback.
 * No locally generated signature is ever posted into Android's for-you/index.php.
 */
internal data class IosForYouServerIds(val ids: List<Int>, val source: String) {
    companion object {
        fun androidCache(response: JsonObject): IosForYouServerIds? {
            require(response["ok"]?.jsonPrimitive?.booleanOrNull == true) { "Caché Android invàlida" }
            if (response["hit"]?.jsonPrimitive?.booleanOrNull != true ||
                response["fresh"]?.jsonPrimitive?.booleanOrNull != true) return null
            return IosForYouServerIds(readIds(response), "android-v329")
        }
        fun serverCalculation(response: JsonObject): IosForYouServerIds {
            require(response["ok"]?.jsonPrimitive?.booleanOrNull == true &&
                response["calculated_on_server"]?.jsonPrimitive?.booleanOrNull == true &&
                response["source"]?.jsonPrimitive?.contentOrNull == "server-v1") {
                "El servidor no confirma el càlcul de Per a tu"
            }
            return IosForYouServerIds(readIds(response), "server-v1")
        }
        private fun readIds(response: JsonObject): List<Int> {
            val arr = response["ids"] as? JsonArray ?: error("No hi ha IDs de Per a tu")
            require(arr.size <= 5000) { "Massa esdeveniments" }
            val values = arr.map { entry ->
                val id = (entry as? JsonPrimitive)?.intOrNull ?: error("ID no vàlid")
                require(id > 0) { "ID no vàlid" }
                id
            }
            require(values.distinct().size == values.size) { "IDs duplicats" }
            return values
        }
    }
}

/** Preserves stable server order; never synthesizes events not in the public catalog. */
internal object IosForYouServerOrder {
    fun select(ids: List<Int>, catalog: List<AgendaEventSummary>): List<AgendaEventSummary> {
        val byId = catalog.groupBy { it.id }
        return ids.flatMap { byId[it].orEmpty() }
            .distinctBy { it.key }
    }
    /** Strictly use the frozen Android v336 signature if it is in central server state.
     * Android has a different hard signature from the PHP server-v1 SHA256 signature.
     */
    fun androidSignature(state: IosHydratedServerState?, username: String?): Int? {
        val account = username?.trim()?.lowercase()?.takeIf(String::isNotEmpty) ?: "guest"
        val key = "for_you_v268_unified_snapshot_${account}_hard_sig_v329"
        val value = state?.central?.long(key) ?: return null
        return value.takeIf { it in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() }?.toInt()
    }
    fun payload(categories: Set<String>, subcategories: Set<String>, towns: Set<String>,
        comarques: Set<String>, radii: Map<String, Int>, suggestions: Boolean,
        discovery: DiscoveryLevel, valencianFirst: Boolean): JsonObject = buildJsonObject {
        put("preferences", buildJsonObject {
            put("categories", JsonArray(categories.sorted().map(::JsonPrimitive)))
            put("subcategories", JsonArray(subcategories.sorted().map(::JsonPrimitive)))
            put("towns", JsonArray(towns.sorted().map(::JsonPrimitive)))
            put("comarques", JsonArray(comarques.sorted().map(::JsonPrimitive)))
            put("radii", buildJsonObject { radii.entries.sortedBy { it.key }.forEach { (town, radius) ->
                if (town.isNotBlank() && radius in setOf(5, 15, 30, 50, 100)) put(town, radius)
            } })
            put("suggestions", suggestions)
            put("discovery", discovery.name)
            put("valencian_first", valencianFirst)
        })
        // Server uses the authenticated user's own follows, not a client mirror.
        put("follows", JsonArray(emptyList()))
    }
}
