package com.example.agendavalenciana.ios

/** Android FestivalPopup/Concert.isFestival parity for the public iOS catalog.
 * A theatre run or an exhibition with many sessions must not become a festival.
 */
object IosEventPopupPolicy {
    fun isFestival(event: AgendaEventSummary): Boolean {
        val structure = event.eventStructure.trim().uppercase()
        if (structure in setOf("FESTIVAL", "PROGRAMME", "CYCLE")) return true
        if (structure == "STANDARD") return false
        if (IosAdvancedSearch.norm(event.category) in setOf("exposicions", "exposiciones", "exhibitions", "exhibition")) return false
        // Android AUTO: >=3 logical days AND different actual programmes, never just a date range.
        val scheduleDays = event.schedule.map { it.date.take(10) }.filter(String::isNotBlank).distinct()
        val numDays = if (scheduleDays.isNotEmpty()) scheduleDays.size else {
            val start = event.date.take(10)
            val end = event.endDate.take(10)
            if (IosFilterDates.valid(start) && IosFilterDates.valid(end) && end >= start)
                (IosFilterDates.epochDays(end) - IosFilterDates.epochDays(start) + 1).toInt()
            else 1
        }
        if (numDays < 3) return false
        val performers = (event.artists + event.schedule.map { it.artist })
            .map(::artistKey).filter(String::isNotBlank).distinct()
        val distinctProgrammes = event.schedule.groupBy { it.date.take(10) }.values
            .map { slots -> slots.map { artistKey(it.artist.ifBlank { it.title }) }.filter(String::isNotBlank).distinct().sorted() }
            .filter { it.isNotEmpty() }.distinct().size
        // A repeated single show remains a standard event, however many sessions it has.
        if (distinctProgrammes == 1 && performers.size <= 1) return false
        return performers.size > 1 || distinctProgrammes > 1 ||
            (performers.isEmpty() && Regex("\\b(festival|fira|feria|fest|cicle|ciclo|trobada|jornades?|mostra|certamen|aplec)\\b", RegexOption.IGNORE_CASE)
                .containsMatchIn(event.title))
    }

    fun artistKey(raw: String): String = IosAdvancedSearch.norm(raw)
        .replace(Regex("[^a-z0-9]+"), "")

    fun isArtistInFestival(event: AgendaEventSummary, name: String): Boolean {
        val key = artistKey(name)
        return key.isNotEmpty() && (event.artists.any { artistKey(it) == key } ||
            event.schedule.any { artistKey(it.artist.ifBlank { it.title }) == key })
    }

    /** Only an artist-specific search (or an explicit artist popup action) can set focus. */
    fun requestedArtist(event: AgendaEventSummary, raw: String, kind: IosSearchType?): String {
        if (!event.isFestival || raw.isBlank()) return ""
        if (kind != null && kind != IosSearchType.ARTIST) return ""
        return (event.artists + event.schedule.map { it.artist.ifBlank { it.title } })
            .firstOrNull { artistKey(it) == artistKey(raw) }.orEmpty()
    }

    fun matchingSlot(schedule: List<AgendaScheduleEntry>, artist: String, time: String = "", fromDate: String = ""): AgendaScheduleEntry? {
        val key = artistKey(artist)
        if (key.isEmpty()) return null
        val matches = schedule.filter { artistKey(it.artist.ifBlank { it.title }) == key }
            .sortedWith(compareBy<AgendaScheduleEntry> { it.date }.thenBy { it.time })
        val upcoming = if (IosFilterDates.valid(fromDate))
            matches.filter { it.date.take(10) >= fromDate }.ifEmpty { matches } else matches
        return upcoming.firstOrNull { time.isNotBlank() && it.time == time } ?: upcoming.firstOrNull()
    }

    /** Days come from real published slots, never all intermediate calendar dates. */
    fun publishedDays(schedule: List<AgendaScheduleEntry>, today: String): List<String> =
        schedule.map { it.date.take(10) }.filter(IosFilterDates::valid).distinct().sorted()
            .filter { !IosFilterDates.valid(today) || it >= today }
}
