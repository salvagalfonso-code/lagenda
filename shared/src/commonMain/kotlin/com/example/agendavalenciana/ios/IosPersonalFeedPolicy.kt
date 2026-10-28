package com.example.agendavalenciana.ios

import com.example.agendavalenciana.model.DiscoveryLevel
import com.example.agendavalenciana.model.PersonalizationOptions

/** Input independent of Compose/HTTP for reproducible Android and iOS rules tests. */
data class IosPersonalEvent(val id: Int, val key: String, val date: String, val time: String,
    val artists: List<String>, val scheduleArtists: List<String>, val organizers: List<String>,
    val venue: String, val category: String, val styles: List<String>, val town: String,
    val language: String, val ticketSale: Boolean = false)

data class IosPersonalSettings(val categories: Set<String> = emptySet(),
    val subcategories: Set<String> = emptySet(), val towns: Set<String> = emptySet(),
    val comarques: Set<String> = emptySet(), val townRadii: Map<String,Int> = emptyMap(),
    val suggestionsEnabled: Boolean = false, val discoveryLevel: DiscoveryLevel = DiscoveryLevel.BALANCED,
    val valencianFirst: Boolean = true)

object IosPersonalFeedPolicy {
    private fun norm(s: String) = IosGeoPlaces.key(s)
    private fun styles(raw: List<String>): Set<String> = raw.flatMap { it.split('|',',',';') }
        .map(IosStyleAffinityCatalog::canonical).filter(String::isNotBlank).toSet()
    private fun valencia(language: String): Boolean {
        val lang = norm(language)
        return lang.contains("valencia") || lang.contains("catala") || lang == "ca"
    }
    fun select(events: List<IosPersonalEvent>, follows: List<IosFollow>, settings: IosPersonalSettings,
        guaranteed: Set<Int>, recentStyles: Set<String> = emptySet(),
        artistMetadata: Map<String, Set<String>> = emptyMap(),
        recentStyleWeights: Map<String, Int> = emptyMap()): List<String> {
        val followedArtists = follows.filter { it.type.equals("ARTIST",true) }.flatMap { listOf(it.key,it.label) }
            .map(::norm).filter(String::isNotBlank).toSet()
        val followedOrganizers = follows.filter { it.type.equals("ORGANIZER",true) }.flatMap { listOf(it.key,it.label) }
            .map(::norm).filter(String::isNotBlank).toSet()
        val followedVenues = follows.filter { it.type.equals("VENUE",true) }.flatMap { listOf(it.key,it.label) }
            .map(::norm).filter(String::isNotBlank).toSet()
        val followedCategories = follows.filter { it.type.equals("CATEGORY",true) }.flatMap { listOf(it.key,it.label) }
            .map(::norm).filter(String::isNotBlank).toSet()
        val preferredCodes = (settings.categories + settings.subcategories + followedCategories)
            .map(IosStyleAffinityCatalog::canonical).filter(String::isNotBlank).toSet()
        val hasLocationPrefs = settings.towns.isNotEmpty() || settings.comarques.isNotEmpty()
        val normalizedRecent = recentStyles.map(IosStyleAffinityCatalog::canonical).toSet()
        // Prefer authoritative entity metadata; fall back to actual event styles only
        // for followed artists missing from that catalog (Android's same fallback).
        val classified = followedArtists.flatMap { artistMetadata[it].orEmpty() }
            .map(IosStyleAffinityCatalog::canonical).filter(String::isNotBlank).toSet()
        val withoutMetadata = followedArtists.filter { artistMetadata[it].isNullOrEmpty() }.toSet()
        val fallback = if (withoutMetadata.isEmpty()) emptySet() else events.asSequence().filter { e ->
            (e.artists + e.scheduleArtists).any { norm(it) in withoutMetadata }
        }.flatMap { styles(it.styles).asSequence() }.toSet()
        val followedStyles = classified + fallback
        val normalizedWeights = mutableMapOf<String, Int>()
        recentStyleWeights.forEach { (rawCode, count) ->
            val code = IosStyleAffinityCatalog.canonical(rawCode)
            if (code.isNotBlank()) normalizedWeights[code] = (normalizedWeights[code] ?: 0) + count.coerceAtLeast(0)
        }
        val geoFixed = HashMap<String,Boolean>()
        val geoSelected = HashMap<String,Boolean>()
        fun geo(e: IosPersonalEvent, fixed: Boolean): Boolean {
            val cache = if (fixed) geoFixed else geoSelected
            return cache.getOrPut(e.town) {
                IosGeoPlaces.within(e.town, settings.towns, settings.comarques, settings.townRadii,
                    PersonalizationOptions.defaultRadiusKm, if (fixed) 100 else null)
            }
        }
        val core = LinkedHashSet<String>()
        val suggestions = mutableListOf<Pair<IosPersonalEvent,Int>>()
        val seen = HashSet<String>()
        for (e in events) {
            if (!seen.add(e.key)) continue
            val followed = (e.artists + e.scheduleArtists).any { norm(it) in followedArtists } ||
                e.organizers.any { norm(it) in followedOrganizers } || norm(e.venue) in followedVenues
            val codes = styles(e.styles) + IosStyleAffinityCatalog.canonical(e.category)
            val fromFollowedStyle = followedStyles.isNotEmpty() && codes.any { it in followedStyles } &&
                hasLocationPrefs && geo(e, false)
            // Category without a place: cultural interest; place without category: local activities.
            // Both: require BOTH cultural affinity and the 100 km preference area.
            val personalization = (preferredCodes.isNotEmpty() || hasLocationPrefs) &&
                (preferredCodes.isEmpty() || codes.any { it in preferredCodes }) &&
                (!hasLocationPrefs || geo(e, true))
            if (followed || fromFollowedStyle || personalization || e.id in guaranteed || e.ticketSale) {
                core.add(e.key)
            } else if (settings.suggestionsEnabled && hasLocationPrefs && geo(e, false)) {
                val weight = codes.sumOf { normalizedWeights[it] ?: if (it in normalizedRecent) 1 else 0 }
                if (weight > 0) suggestions += e to weight
            }
        }
        if (settings.suggestionsEnabled && suggestions.isNotEmpty()) {
            val quota = when(settings.discoveryLevel) {
                DiscoveryLevel.MY_TASTES -> if (core.isEmpty()) 1 else (core.size/6).coerceIn(1,8)
                DiscoveryLevel.BALANCED -> if (core.isEmpty()) 3 else (core.size/3).coerceIn(2,20)
                DiscoveryLevel.EXPLORE -> if (core.isEmpty()) 8 else ((core.size*2)/3).coerceIn(4,40)
            }
            suggestions.sortedWith(compareByDescending<Pair<IosPersonalEvent,Int>> { it.second }
                .thenBy { it.first.id }).take(quota).forEach { core += it.first.key }
        }
        return events.filter { it.key in core }.distinctBy { it.key }.sortedWith(
            compareBy<IosPersonalEvent> { it.date.ifBlank { "9999" } }
                .thenByDescending { settings.valencianFirst && valencia(it.language) }
                .thenBy { it.time }.thenBy { it.id }
        ).map { it.key }
    }

    /** Stable per-place radii; only one of the five supported values is allowed. */
    fun decodeRadii(raw: Set<String>): Map<String,Int> = raw.mapNotNull { item ->
        val split = item.lastIndexOf('@')
        if (split < 1) return@mapNotNull null
        val r = item.substring(split+1).toIntOrNull() ?: return@mapNotNull null
        item.substring(0,split).takeIf(String::isNotBlank)?.let { it to PersonalizationOptions.validRadius(r) }
    }.toMap()
    fun encodeRadii(values: Map<String,Int>): Set<String> = values.map { (town,radius) ->
        "${town}@${PersonalizationOptions.validRadius(radius)}"
    }.toSet()
}
