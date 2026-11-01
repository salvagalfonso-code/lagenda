package com.example.agendavalenciana.ios

/** Shared pure search rules: a suggested entity is not an unrestricted text match. */
enum class IosSearchType { ARTIST, TOWN, COMARCA, ORGANIZER, VENUE, STYLE, CATEGORY, EVENT }

data class IosSearchSuggestion(val type: IosSearchType, val value: String, val subtitle: String = "",
                               val aliases: List<String> = emptyList(), val comarca: String = "")

data class IosSearchFilters(
    val town: String = "", val comarca: String = "", val radiusKm: Int = 0,
    val category: String = "", val style: String = "", val fromDate: String = "", val toDate: String = "",
    val categoryCodes: Set<String> = emptySet(), val month: String = "", val maxPrice: Int? = null,
    val freeOnly: Boolean = false, val preferredTowns: Set<String> = emptySet()
) {
    val isActive: Boolean get() = listOf(town, comarca, category, style, fromDate, toDate, month)
        .any { it.isNotBlank() } || categoryCodes.isNotEmpty() || radiusKm > 0 || maxPrice != null || freeOnly
    fun valid(): Boolean = radiusKm in 0..2000 &&
        (fromDate.isBlank() || IosSearchPresetContract.isIsoDate(fromDate)) &&
        (toDate.isBlank() || IosSearchPresetContract.isIsoDate(toDate)) &&
        (month.isBlank() || IosSearchPresetContract.isIsoMonth(month)) &&
        (fromDate.isBlank() || toDate.isBlank() || fromDate <= toDate) &&
        (maxPrice == null || maxPrice in 0..1000000) &&
        categoryCodes.size <= 40 && categoryCodes.all { IosCategoryTaxonomy.mainKey(it) != null }
}

object IosAdvancedSearch {
    /** Accent-insensitive matching without JVM-only Normalizer (also works under Kotlin/Native). */
    fun norm(value: String): String = value.trim().lowercase()
        .replace("à","a").replace("á","a").replace("â","a").replace("ä","a")
        .replace("è","e").replace("é","e").replace("ê","e").replace("ë","e")
        .replace("í","i").replace("ï","i").replace("ì","i")
        .replace("ó","o").replace("ò","o").replace("ö","o")
        .replace("ú","u").replace("ù","u").replace("ü","u")
        .replace("ç","c").replace("ñ","n")
        .replace(Regex("\\s+"), " ")
    private fun same(a: String,b: String) = norm(a) == norm(b)
    private fun contains(a: String,b: String) = norm(a).contains(norm(b))

    /** Build the pool once per catalog revision, not once per typed letter. */
    fun pool(events: List<AgendaEventSummary>, geoTowns: List<String>, comarques: List<String>,
             detailedTowns: List<IosGeoPlaces.TownEntry> = emptyList()): List<IosSearchSuggestion> {
        val result = ArrayList<IosSearchSuggestion>()
        fun add(values: Sequence<String>, type: IosSearchType, subtitle: String) {
            values.map(String::trim).filter(String::isNotBlank).distinctBy(::norm).forEach {
                result.add(IosSearchSuggestion(type, it, subtitle))
            }
        }
        add(events.asSequence().flatMap { (it.artists + it.schedule.map { row -> IosFestivalArtistSearch.artistLabel(row) }).asSequence() }, IosSearchType.ARTIST, "Grup / artista")
        // Android retains multiple municipalities sharing a name, separated by comarca.
        val structuredNames = detailedTowns.map { norm(it.name) }.toSet()
        detailedTowns.forEach { town ->
            val subtitle = listOf(town.official.takeIf { it.isNotBlank() && norm(it) != norm(town.name) },
                town.comarca.takeIf(String::isNotBlank), town.territory.takeIf(String::isNotBlank))
                .filterNotNull().distinct().joinToString(" · ")
            result += IosSearchSuggestion(IosSearchType.TOWN, town.name, subtitle,
                (town.aliases + town.official).filter(String::isNotBlank).distinctBy(::norm), town.comarca)
        }
        add(geoTowns.asSequence().filter { norm(it) !in structuredNames }, IosSearchType.TOWN, "Població")
        add((comarques + events.mapNotNull { it.comarca.takeIf(String::isNotBlank) }).asSequence(), IosSearchType.COMARCA, "Comarca")
        add(events.asSequence().flatMap { it.organizers.asSequence() }, IosSearchType.ORGANIZER, "Organitzador")
        add(events.asSequence().map { it.venue }, IosSearchType.VENUE, "Sala / espai")
        add(events.asSequence().flatMap { it.styles.asSequence().flatMap { s -> s.split('|',',',';').asSequence() } }, IosSearchType.STYLE, "Estil")
        add(events.asSequence().map { it.category }, IosSearchType.CATEGORY, "Categoria")
        add(events.asSequence().map { it.title }, IosSearchType.EVENT, "Esdeveniment")
        return result.distinctBy { Triple(it.type, norm(it.value),
            if (it.type == IosSearchType.TOWN) norm(it.comarca) else "") }
    }

    /** Bounded top-N ranking; never sort every town on each keyboard stroke. */
    fun suggest(pool: List<IosSearchSuggestion>, raw: String, max: Int = 12): List<IosSearchSuggestion> {
        val q = norm(raw)
        if (q.isBlank() || max <= 0) return emptyList()
        data class Match(val rank: Int, val typeRank: Int, val text: String, val suggestion: IosSearchSuggestion)
        val byType = listOf(IosSearchType.ARTIST, IosSearchType.TOWN, IosSearchType.COMARCA,
            IosSearchType.ORGANIZER, IosSearchType.VENUE, IosSearchType.STYLE, IosSearchType.CATEGORY, IosSearchType.EVENT)
        val comparator = compareBy<Match> { it.rank }.thenBy { it.typeRank }.thenBy { it.text }
            .thenBy { norm(it.suggestion.subtitle) }
        val best = ArrayList<Match>(max)
        for (candidate in pool) {
            // The ranking follows Android: exact > prefix > containing, then type/name.
            // Official names and historical aliases must also match the canonical town.
            val rank = (listOf(candidate.value) + candidate.aliases).mapNotNull { value ->
                val normalized = norm(value)
                when { normalized == q -> 0; normalized.startsWith(q) -> 1
                    normalized.contains(q) -> 2; else -> null }
            }.minOrNull() ?: continue
            val entry = Match(rank, byType.indexOf(candidate.type), norm(candidate.value), candidate)
            val index = best.binarySearch(entry, comparator).let { if (it < 0) -it - 1 else it }
            if (index < max) {
                best.add(index, entry)
                if (best.size > max) best.removeAt(best.lastIndex)
            }
        }
        return best.map { it.suggestion }
    }

    fun matches(event: AgendaEventSummary, query: String, selected: IosSearchSuggestion?, filters: IosSearchFilters): Boolean {
        if (!filters.valid()) return false
        val term = query.trim()
        if (term.isNotBlank()) {
            val entity = selected?.takeIf { same(it.value,term) }
            val found = if (entity == null) {
                // EventCatalog.matches handles festival schedule artists too.
                (listOf(event.title,event.subtitle,event.town,event.venue,event.category) +
                    event.artists + event.organizers + event.styles +
                    event.schedule.flatMap { listOf(it.artist,it.title) }).any { contains(it,term) }
            } else when (entity.type) {
                IosSearchType.ARTIST -> IosFestivalArtistSearch.isInLineup(event, term) ||
                    event.artists.any { same(it,term) }
                IosSearchType.TOWN -> {
                    val namesMatch = same(event.town, term) ||
                        (IosGeoPlaces.place(event.town) != null && IosGeoPlaces.place(event.town) == IosGeoPlaces.place(term))
                    val comarca = event.comarca.ifBlank { IosGeoPlaces.place(event.town)?.comarca.orEmpty() }
                    namesMatch && (entity.comarca.isBlank() ||
                        (if (event.comarca.isBlank() && IosGeoPlaces.ambiguousTown(event.town)) false
                         else same(comarca, entity.comarca)))
                }
                IosSearchType.COMARCA -> same(event.comarca.ifBlank {
                    IosGeoPlaces.place(event.town)?.comarca.orEmpty()
                }, term)
                IosSearchType.ORGANIZER -> event.organizers.any { same(it,term) }
                IosSearchType.VENUE -> same(event.venue,term)
                IosSearchType.STYLE -> event.styles.any { style -> style.split('|',',',';').any { same(it,term) } }
                IosSearchType.CATEGORY -> same(event.category,term)
                IosSearchType.EVENT -> same(event.title,term)
            }
            if (!found) return false
        }
        if (filters.radiusKm > 0 && filters.town.isBlank()) {
            // Android's saved maxDistanceKm can reference personal preferred locations
            // without a specific town. Never silently ignore that constraint in iOS.
            val venue = IosGeoPlaces.place(event.town) ?: return false
            val origins = filters.preferredTowns.mapNotNull(IosGeoPlaces::place)
            if (origins.isEmpty() || origins.none { IosGeoPlaces.kilometres(it, venue) <= filters.radiusKm }) return false
        }
        if (filters.town.isNotBlank()) {
            val origin = IosGeoPlaces.place(filters.town)
            val venue = IosGeoPlaces.place(event.town)
            if (filters.radiusKm == 0) {
                if (!same(filters.town,event.town) && (origin == null || origin != venue)) return false
            } else if (origin == null || venue == null || IosGeoPlaces.kilometres(origin,venue) > filters.radiusKm) return false
        }
        if (filters.comarca.isNotBlank() && !same(event.comarca.ifBlank {
            IosGeoPlaces.place(event.town)?.comarca.orEmpty()
        },filters.comarca)) return false
        if (filters.categoryCodes.isNotEmpty()) {
            val eventCode = IosCategoryTaxonomy.mainKey(event.category)
            if (eventCode !in filters.categoryCodes) return false
        } else if (filters.category.isNotBlank() && !same(event.category,filters.category)) return false
        if (filters.month.isNotBlank()) {
            val startMonth = event.date.take(7)
            val endMonth = event.endDate.ifBlank { event.date }.take(7)
            if (startMonth.isBlank() || endMonth.isBlank() || startMonth > filters.month || endMonth < filters.month) return false
        }
        if (filters.maxPrice != null || filters.freeOnly) {
            val price = IosSearchPresetContract.priceEuros(event.price) ?: return false
            if (filters.maxPrice != null && price > filters.maxPrice.toDouble()) return false
            if (filters.freeOnly && price != 0.0) return false
        }
        if (filters.style.isNotBlank() && event.styles.none { it.split('|',',',';').any { s -> same(s,filters.style) } }) return false
        val start = event.date.take(10)
        val end = event.endDate.ifBlank { event.date }.take(10)
        // Festivals overlapping the selected dates remain in results.
        if (filters.fromDate.isNotBlank() && end.isNotBlank() && end < filters.fromDate) return false
        if (filters.toDate.isNotBlank() && start.isNotBlank() && start > filters.toDate) return false
        return true
    }
}
