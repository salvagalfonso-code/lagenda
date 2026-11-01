package com.example.agendavalenciana.ios

import com.example.agendavalenciana.model.DiscoveryLevel
import com.example.agendavalenciana.model.PersonalizationOptions
import kotlinx.serialization.json.*

/** Pas 4. Read-only RAM snapshot of the same sync/state.php store used by Android v336.
 * Keep ALL unknown keys: later migration steps can project them without another schema.
 * No server snapshot, personal preference, or bearer is persisted in UserDefaults.
 */
internal data class IosCentralStateSnapshot(
    val userId: Int,
    val revision: Long,
    val values: JsonObject
) {
    init { require(userId > 0 && revision >= 0L) }

    /** Android ServerManagedPreferences encodes every SharedPreferences value as {t,v}. */
    private fun encoded(key: String): JsonObject? = values[key] as? JsonObject
    fun string(key: String): String? = encoded(key)?.takeIf { it["t"]?.jsonPrimitive?.contentOrNull == "string" }
        ?.get("v")?.jsonPrimitive?.contentOrNull
    fun bool(key: String): Boolean? = encoded(key)?.takeIf { it["t"]?.jsonPrimitive?.contentOrNull == "bool" }
        ?.get("v")?.jsonPrimitive?.booleanOrNull
    fun long(key: String): Long? = encoded(key)?.takeIf { it["t"]?.jsonPrimitive?.contentOrNull in setOf("long", "int") }
        ?.get("v")?.jsonPrimitive?.longOrNull
    fun strings(key: String): Set<String>? {
        val obj = encoded(key)?.takeIf { it["t"]?.jsonPrimitive?.contentOrNull == "set" } ?: return null
        val arr = obj["v"] as? JsonArray ?: return null
        return arr.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.toSet()
    }
    companion object {
        fun decode(root: JsonObject, expectedUserId: Int): IosCentralStateSnapshot {
            require(root["ok"]?.jsonPrimitive?.booleanOrNull == true) { "El servidor no ha confirmat l'estat." }
            val id = root["user_id"]?.jsonPrimitive?.intOrNull ?: 0
            require(expectedUserId > 0 && id == expectedUserId) {
                "L'estat rebut no pertany a l'usuari actiu."
            }
            val revision = root["revision"]?.jsonPrimitive?.longOrNull
                ?: error("Falta la revisió de l'estat.")
            val values = root["values"] as? JsonObject ?: error("Falten els valors de l'estat.")
            return IosCentralStateSnapshot(id, revision, values)
        }
    }
}

internal data class IosServerLocations(
    val exists: Boolean, val revision: Long, val towns: Map<String, Int>, val comarques: Set<String>
) {
    companion object {
        fun decode(root: JsonObject): IosServerLocations {
            val locations = root["locations"] as? JsonObject ?: error("Falten llocs")
            val towns = linkedMapOf<String, Int>()
            (locations["towns"] as? JsonArray).orEmpty().forEach { item ->
                val row = item as? JsonObject ?: return@forEach
                val name = row["name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                // Same fallback as Android ServerApiRepository.parseServerLocations().
                if (name.isNotBlank()) towns[name] = PersonalizationOptions.validRadius(
                    row["radius_km"]?.jsonPrimitive?.intOrNull)
            }
            val comarques = (locations["comarques"] as? JsonArray).orEmpty()
                .mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank) }.toSet()
            return IosServerLocations(root["exists"]?.jsonPrimitive?.booleanOrNull == true,
                root["revision"]?.jsonPrimitive?.longOrNull ?: 0L, towns, comarques)
        }
    }
}

internal data class IosServerFeedPreferences(
    val exists: Boolean, val suggestions: Boolean, val discovery: DiscoveryLevel, val valencianFirst: Boolean
) {
    companion object {
        fun decode(root: JsonObject): IosServerFeedPreferences {
            val preferences = root["preferences"] as? JsonObject ?: error("Falten preferències del feed")
            return IosServerFeedPreferences(
                root["exists"]?.jsonPrimitive?.booleanOrNull == true,
                preferences["suggestions"]?.jsonPrimitive?.booleanOrNull ?: true,
                runCatching { DiscoveryLevel.valueOf(preferences["discovery"]?.jsonPrimitive?.contentOrNull ?: "BALANCED") }
                    .getOrDefault(DiscoveryLevel.BALANCED),
                preferences["valencian_first"]?.jsonPrimitive?.booleanOrNull ?: true
            )
        }
    }
}

/** Complete read of the general state + dedicated locations/feed domains.
 * These supplementary APIs can be absent on older deployments: the central snapshot
 * is still authoritative for keys it contains; absent domains are NEVER saved as empty.
 */
internal data class IosHydratedServerState(
    val central: IosCentralStateSnapshot,
    val locations: IosServerLocations?,
    val feed: IosServerFeedPreferences?,
    val userDomain: JsonObject? = null, // follows, saved events, alerts, affinities and notification flags
    val visibility: IosVisibilitySnapshot? = null
) {
    val userId: Int get() = central.userId
    private fun scoped(key: String, username: String?): String =
        "${key}_${username?.trim()?.lowercase()?.takeIf(String::isNotBlank) ?: "guest"}"

    fun towns(username: String?): Set<String>? =
        locations?.takeIf { it.exists }?.towns?.keys ?: central.strings(scoped("preferred_locations",username))
    fun comarques(username: String?): Set<String>? =
        locations?.takeIf { it.exists }?.comarques ?: central.strings(scoped("preferred_comarques",username))
    fun radii(username: String?): Map<String, Int>? {
        locations?.takeIf { it.exists }?.let { return it.towns }
        val raw = central.string(scoped("preferred_location_radii",username)) ?: return null
        return runCatching { Json.parseToJsonElement(raw) as? JsonObject }.getOrNull()?.mapNotNull { (town, value) ->
            (value as? JsonPrimitive)?.intOrNull?.let { town to PersonalizationOptions.validRadius(it) }
        }?.toMap()
    }
    /** L'idioma de la UI és una clau central Android, NO valencian_first. */
    fun uiLanguage(): IosUiLanguage? =
        IosUiLanguage.fromServer(central.string(IosFeedPreferencesContract.centralUiLanguageKey))

    fun suggestions(): Boolean? = feed?.takeIf { it.exists }?.suggestions ?: central.bool("show_suggestions")
    fun valencianFirst(): Boolean? = feed?.takeIf { it.exists }?.valencianFirst ?: central.bool("valencian_first")
    fun discovery(): DiscoveryLevel? = feed?.takeIf { it.exists }?.discovery ?: central.string("discovery_level")?.let { name ->
        runCatching { DiscoveryLevel.valueOf(name) }.getOrNull()
    }
}

/** Per-user persistent UI selections that do not have a dedicated server table. */
internal fun IosHydratedServerState.personalSubcategories(): Set<String>? = central.strings("ios_pref_subcategories_v1")
internal fun IosHydratedServerState.personalHiddenEvents(): Set<Int>? = visibility?.events
internal fun IosHydratedServerState.personalHiddenArtists(): Set<String>? = visibility?.artists
internal fun IosHydratedServerState.personalHiddenCategories(): Set<String>? = visibility?.categories
internal fun IosHydratedServerState.personalFestivalSlots(eventId: Int): Set<String>? =
    central.strings("ios_festival_schedule_${eventId}")
