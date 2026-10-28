package com.example.agendavalenciana.ios

/** Shared pure search rules: a suggested entity is not an unrestricted text match. */
enum class IosSearchType { ARTIST, TOWN, COMARCA, ORGANIZER, VENUE, STYLE, CATEGORY, EVENT }

data class IosSearchSuggestion(val type: IosSearchType, val value: String, val subtitle: String = "")

data class IosSearchFilters(
    val town: String = "", val comarca: String = "", val radiusKm: Int = 0,
    val category: String = "", val style: String = "", val fromDate: String = "", val toDate: String = ""
) {
    val isActive: Boolean get() = listOf(town, comarca, category, style, fromDate, toDate).any { it.isNotBlank() }
    fun valid(): Boolean = radiusKm in setOf(0,5,15,30,50,100) &&
        (radiusKm == 0 || town.isNotBlank()) &&
        (fromDate.isBlank() || fromDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) &&
        (toDate.isBlank() || toDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) &&
        (fromDate.isBlank() || toDate.isBlank() || fromDate <= toDate)
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
    fun pool(events: List<AgendaEventSummary>, geoTowns: List<String>, comarques: List<String>): List<IosSearchSuggestion> {
        val result = ArrayList<IosSearchSuggestion>()
        fun add(values: Sequence<String>, type: IosSearchType, subtitle: String) {
            values.map(String::trim).filter(String::isNotBlank).distinctBy(::norm).forEach {
                result.add(IosSearchSuggestion(type, it, subtitle))
            }
        }
        add(events.asSequence().flatMap { (it.artists + it.schedule.map(AgendaScheduleEntry::artist)).asSequence() }, IosSearchType.ARTIST, "Grup / artista")
        add(geoTowns.asSequence(), IosSearchType.TOWN, "Població")
        add(comarques.asSequence(), IosSearchType.COMARCA, "Comarca")
        add(events.asSequence().flatMap { it.organizers.asSequence() }, IosSearchType.ORGANIZER, "Organitzador")
        add(events.asSequence().map { it.venue }, IosSearchType.VENUE, "Sala / espai")
        add(events.asSequence().flatMap { it.styles.asSequence().flatMap { s -> s.split('|',',',';').asSequence() } }, IosSearchType.STYLE, "Estil")
        add(events.asSequence().map { it.category }, IosSearchType.CATEGORY, "Categoria")
        add(events.asSequence().map { it.title }, IosSearchType.EVENT, "Esdeveniment")
        return result.distinctBy { it.type to norm(it.value) }
    }

    /** Bounded top-N ranking; never sort every town on each keyboard stroke. */
    fun suggest(pool: List<IosSearchSuggestion>, raw: String, max: Int = 10): List<IosSearchSuggestion> {
        val q = norm(raw)
        if (q.length < 2 || max <= 0) return emptyList()
        val types = IosSearchType.entries
        val best = ArrayList<Pair<Int,IosSearchSuggestion>>()
        for (s in pool) {
            val text = norm(s.value)
            val score = when {
                text == q -> 0
                text.startsWith(q) -> 10
                text.contains(q) -> 20
                else -> continue
            } + types.indexOf(s.type)
            val index = best.indexOfFirst { it.first > score || (it.first == score && norm(it.second.value) > text) }
                .let { if (it < 0) best.size else it }
            if (index < max) {
                best.add(index,score to s)
                if (best.size > max) best.removeAt(best.lastIndex)
            }
        }
        return best.map { it.second }
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
                IosSearchType.ARTIST -> (event.artists + event.schedule.map { it.artist }).any { same(it,term) }
                IosSearchType.TOWN -> same(event.town,term) ||
                    (IosGeoPlaces.place(event.town) != null && IosGeoPlaces.place(event.town) == IosGeoPlaces.place(term))
                IosSearchType.COMARCA -> same(IosGeoPlaces.place(event.town)?.comarca.orEmpty(),term)
                IosSearchType.ORGANIZER -> event.organizers.any { same(it,term) }
                IosSearchType.VENUE -> same(event.venue,term)
                IosSearchType.STYLE -> event.styles.any { style -> style.split('|',',',';').any { same(it,term) } }
                IosSearchType.CATEGORY -> same(event.category,term)
                IosSearchType.EVENT -> same(event.title,term)
            }
            if (!found) return false
        }
        if (filters.town.isNotBlank()) {
            val origin = IosGeoPlaces.place(filters.town)
            val venue = IosGeoPlaces.place(event.town)
            if (filters.radiusKm == 0) {
                if (!same(filters.town,event.town) && (origin == null || origin != venue)) return false
            } else if (origin == null || venue == null || IosGeoPlaces.kilometres(origin,venue) > filters.radiusKm) return false
        }
        if (filters.comarca.isNotBlank() && !same(IosGeoPlaces.place(event.town)?.comarca.orEmpty(),filters.comarca)) return false
        if (filters.category.isNotBlank() && !same(event.category,filters.category)) return false
        if (filters.style.isNotBlank() && event.styles.none { it.split('|',',',';').any { s -> same(s,filters.style) } }) return false
        val start = event.date.take(10)
        val end = event.endDate.ifBlank { event.date }.take(10)
        // Festivals overlapping the selected dates remain in results.
        if (filters.fromDate.isNotBlank() && end.isNotBlank() && end < filters.fromDate) return false
        if (filters.toDate.isNotBlank() && start.isNotBlank() && start > filters.toDate) return false
        return true
    }
}
