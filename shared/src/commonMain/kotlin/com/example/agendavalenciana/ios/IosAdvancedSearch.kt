package com.example.agendavalenciana.ios

/** Shared pure search rules: a suggested entity is not an unrestricted text match. */
enum class IosSearchType { ARTIST, TOWN, COMARCA, ORGANIZER, VENUE, STYLE, CATEGORY, EVENT }

data class IosSearchSuggestion(val type: IosSearchType, val value: String, val subtitle: String = "")

/** Criteria shared by the iOS search popup and server presets (Android-compatible keys). */
data class IosSearchFilters(
    val town: String = "", val comarca: String = "", val radiusKm: Int = 0,
    val category: String = "", val style: String = "", val fromDate: String = "", val toDate: String = "",
    val categories: Set<String> = emptySet(), val month: String = "", val maxPrice: Int? = null,
    val freeOnly: Boolean = false,
    /** Derived from the user's preferred places, never written into server presets. */
    val referenceTowns: Set<String> = emptySet()
) {
    val isActive: Boolean get() = listOf(town, comarca, category, style, fromDate, toDate, month)
        .any { it.isNotBlank() } || categories.isNotEmpty() || radiusKm > 0 || maxPrice != null || freeOnly

    fun valid(): Boolean = radiusKm in setOf(0, 5, 10, 15, 25, 30, 50, 100) &&
        (radiusKm == 0 || town.isNotBlank() || referenceTowns.isNotEmpty()) &&
        (fromDate.isBlank() || IosFilterDates.valid(fromDate)) &&
        (toDate.isBlank() || IosFilterDates.valid(toDate)) &&
        (fromDate.isBlank() || toDate.isBlank() || fromDate <= toDate) &&
        (month.isBlank() || IosFilterDates.validMonth(month)) &&
        (maxPrice == null || maxPrice >= 0) && !(freeOnly && maxPrice != null)
}

/** Date calculations without platform-specific java.time or iOS Foundation. */
object IosFilterDates {
    fun leap(year: Int): Boolean = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
    fun daysInMonth(year: Int, month: Int): Int = when (month) {
        2 -> if (leap(year)) 29 else 28
        4, 6, 9, 11 -> 30
        in 1..12 -> 31
        else -> 0
    }
    fun validMonth(raw: String): Boolean = raw.matches(Regex("\\d{4}-\\d{2}")) &&
        ((raw.substring(5,7).toIntOrNull() ?: -1) in 1..12)
    fun valid(raw: String): Boolean {
        if (!raw.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return false
        val year = raw.substring(0,4).toIntOrNull() ?: return false
        val month = raw.substring(5,7).toIntOrNull() ?: return false
        val day = raw.substring(8,10).toIntOrNull() ?: return false
        return day in 1..daysInMonth(year,month)
    }
    private fun beforeYear(y: Int): Long { val x = (y - 1).toLong(); return 365*x + x/4 - x/100 + x/400 }
    private fun dayOfYear(y: Int, month: Int, day: Int): Int =
        (1 until month).sumOf { daysInMonth(y,it) } + day - 1
    fun epochDays(iso: String): Long {
        require(valid(iso))
        val year = iso.substring(0,4).toInt(); val month = iso.substring(5,7).toInt()
        return beforeYear(year) + dayOfYear(year,month,iso.substring(8,10).toInt()) - beforeYear(1970)
    }
    fun fromEpochDays(days: Long): String {
        var remaining = days + beforeYear(1970)
        var year = 1970
        if (remaining < beforeYear(year)) {
            while (remaining < beforeYear(year)) year--
        } else {
            while (remaining >= beforeYear(year+1)) year++
        }
        remaining -= beforeYear(year)
        var month = 1
        while (remaining >= daysInMonth(year, month)) { remaining -= daysInMonth(year, month); month++ }
        return "$year-${month.toString().padStart(2,'0')}-${(remaining.toInt()+1).toString().padStart(2,'0')}"
    }
    fun addDays(iso: String, days: Int): String = fromEpochDays(epochDays(iso) + days)
    fun weekendSaturday(today: String): String {
        val monZero = ((epochDays(today) + 3) % 7 + 7) % 7
        return addDays(today, ((5 - monZero + 7) % 7).toInt())
    }
    fun monthRange(month: String): Pair<String,String> {
        require(validMonth(month))
        val year = month.substring(0,4).toInt(); val m = month.substring(5,7).toInt()
        return "$month-01" to "$month-${daysInMonth(year,m).toString().padStart(2,'0')}"
    }
    fun monthOptions(today: String, count: Int = 12): List<Pair<String,String>> {
        val year = today.substring(0,4).toInt(); val month = today.substring(5,7).toInt()
        val names = listOf("Gener","Febrer","Març","Abril","Maig","Juny","Juliol","Agost","Setembre","Octubre","Novembre","Desembre")
        return (0 until count).map { index ->
            val absolute = year * 12 + (month - 1) + index
            val y = absolute / 12; val m = absolute % 12 + 1
            "${names[m-1]} $y" to "$y-${m.toString().padStart(2,'0')}"
        }
    }
    fun toUtcMillis(date: String): Long = epochDays(date) * 86_400_000L
    fun fromUtcMillis(value: Long): String {
        val days = if (value >= 0) value / 86_400_000L else (value - 86_399_999L) / 86_400_000L
        return fromEpochDays(days)
    }
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
            val entity = selected?.takeIf {
                if (it.type == IosSearchType.ARTIST)
                    IosEventPopupPolicy.artistKey(it.value) == IosEventPopupPolicy.artistKey(term)
                else same(it.value,term)
            }
            val found = if (entity == null) {
                // EventCatalog.matches handles festival schedule artists too.
                (listOf(event.title,event.subtitle,event.town,event.venue,event.category) +
                    event.artists + event.organizers + event.styles +
                    event.schedule.flatMap { listOf(it.artist,it.title) }).any { contains(it,term) }
            } else when (entity.type) {
                IosSearchType.ARTIST -> (event.artists + event.schedule.map { it.artist.ifBlank { it.title } })
                    .any { IosEventPopupPolicy.artistKey(it) == IosEventPopupPolicy.artistKey(term) }
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
        if (filters.radiusKm > 0 && filters.town.isBlank()) {
            val destination = IosGeoPlaces.place(event.town) ?: return false
            // Same nearest-preferred-place semantics as the Android distance choices.
            if (filters.referenceTowns.none { place -> IosGeoPlaces.place(place)?.let {
                IosGeoPlaces.kilometres(it, destination) <= filters.radiusKm
            } == true }) return false
        }
        if (filters.comarca.isNotBlank() && !same(IosGeoPlaces.place(event.town)?.comarca.orEmpty(),filters.comarca)) return false
        if (filters.category.isNotBlank() && !same(event.category,filters.category)) return false
        if (filters.categories.isNotEmpty() && filters.categories.none { same(it,event.category) }) return false
        if (filters.freeOnly || filters.maxPrice != null) {
            val raw = event.price.trim().lowercase()
            val number = Regex("\\d+(?:[.,]\\d{1,2})?").find(raw)?.value?.replace(',', '.')?.toDoubleOrNull()
            val free = raw in setOf("gratuït", "gratuit", "gratis", "gratuïta", "free") || number == 0.0
            if (filters.freeOnly && !free) return false
            if (filters.maxPrice != null && (number == null || number > filters.maxPrice)) return false
        }
        if (filters.style.isNotBlank() && event.styles.none { it.split('|',',',';').any { s -> same(s,filters.style) } }) return false
        val start = event.date.take(10)
        val end = event.endDate.ifBlank { event.date }.take(10)
        if ((filters.fromDate.isNotBlank() || filters.toDate.isNotBlank() || filters.month.isNotBlank()) &&
            start.isBlank() && end.isBlank()) return false
        // Festivals overlapping the selected dates remain in results.
        if (filters.fromDate.isNotBlank() && end.isNotBlank() && end < filters.fromDate) return false
        if (filters.toDate.isNotBlank() && start.isNotBlank() && start > filters.toDate) return false
        if (filters.month.isNotBlank()) {
            val (first, last) = IosFilterDates.monthRange(filters.month)
            if (end.isNotBlank() && end < first) return false
            if (start.isNotBlank() && start > last) return false
        }
        return true
    }
}
