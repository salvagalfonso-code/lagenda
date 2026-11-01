package com.example.agendavalenciana.ios

import com.example.agendavalenciana.model.DiscoveryLevel
import com.example.agendavalenciana.model.PersonalizationOptions
import com.example.agendavalenciana.model.ForYouHundredPolicy

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
        // Pas 7: Android saves both categories AND subcategories as CATEGORY entity_key.
        // The keys (not entity_label and not free-form "styles") decide editorial matches.
        val followsFromServer = IosCategoryTaxonomy.fromServer(follows)
        val selectedTaxonomy = IosCategoryTaxonomy.normalized(
            settings.categories + followsFromServer.categories,
            settings.subcategories + followsFromServer.subcategories)
        val preferredCodes = (settings.categories + settings.subcategories)
            .filterNot(IosCategoryTaxonomy::isKnown)
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
        // Offline fallback uses the SAME four-route/quota policy as Android v336.
        // It is a provisional selection: the server remains authoritative when reachable.
        val candidates = mutableListOf<ForYouHundredPolicy.Candidate>()
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
            val hasTaxonomyPrefs = selectedTaxonomy.categories.isNotEmpty() || selectedTaxonomy.subcategories.isNotEmpty()
            val hasCulturalPrefs = hasTaxonomyPrefs || preferredCodes.isNotEmpty()
            val taxonomyMatch = IosCategoryTaxonomy.matchesEvent(e.category, e.styles, selectedTaxonomy)
            val personalization = (hasCulturalPrefs || hasLocationPrefs) &&
                (!hasCulturalPrefs || taxonomyMatch || codes.any { it in preferredCodes }) &&
                (!hasLocationPrefs || geo(e, true))
            val suggestionScore = if (settings.suggestionsEnabled && hasLocationPrefs &&
                geo(e, false)) codes.sumOf { normalizedWeights[it] ?: if (it in normalizedRecent) 1 else 0 }
                else 0
            candidates += ForYouHundredPolicy.Candidate(
                id = e.id, followed = followed, interest = fromFollowedStyle,
                proximity = personalization, discovery = suggestionScore > 0,
                recent = suggestionScore > 0, affinityScore = suggestionScore)
        }
        val selectedIds = ForYouHundredPolicy.selectDetailed(candidates,
            settings.discoveryLevel, includeReasons = false).ids.toSet() + guaranteed
        return events.filter { it.id in selectedIds }.distinctBy { it.key }.sortedWith(
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
