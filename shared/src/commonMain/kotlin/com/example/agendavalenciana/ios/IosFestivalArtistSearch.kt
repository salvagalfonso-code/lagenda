package com.example.agendavalenciana.ios

/** Android v336 FestivalArtistSelection / FestivalArtistSearchResults compatibility.
 * A festival is one search result; only its *published programme* can provide
 * an artist's day, time and stage. Never infer these from the festival header.
 */
object IosFestivalArtistSearch {
    /** Kotlin/Native-friendly equivalent of Android's festivalArtistKey. */
    fun key(name: String): String = IosAdvancedSearch.norm(name)
        .replace(Regex("[^a-z0-9\\p{L}\\p{N}]+"), " ")
        .trim().replace(Regex("\\s+"), " ")

    fun resolveArtist(query: String, suggestion: IosSearchSuggestion?, pool: List<IosSearchSuggestion>): String? {
        val wanted = key(query)
        if (wanted.isEmpty()) return null
        if (suggestion?.type == IosSearchType.ARTIST && key(suggestion.value) == wanted)
            return suggestion.value
        // Honour an explicitly selected non-artist entity, even if it shares a label.
        if (suggestion != null && key(suggestion.value) == wanted) return null
        // Mirror Android ArtistSearchPriority.resolve: exact match first, then
        // only a UNIQUE partial match (minimum three letters).
        val names = pool.filter { it.type == IosSearchType.ARTIST }
            .map { it.value }.distinctBy(::key)
        names.firstOrNull { key(it) == wanted }?.let { return it }
        if (wanted.length < 3) return null
        return names.filter { key(it).contains(wanted) }.singleOrNull()
    }

    // The abbreviated catalog lacks the PHP item_type; don't turn a generic
    // "Concert"/"Activitat" schedule row into a fictitious artist suggestion.
    fun artistLabel(slot: AgendaScheduleEntry): String = slot.artist.trim().ifBlank {
        val title = slot.title.trim()
        if (key(title) in setOf("concert", "actuacio", "activitat", "espectacle",
                "taller", "musica", "acte", "programacio")) "" else title
    }
    private fun name(slot: AgendaScheduleEntry): String = artistLabel(slot)
    fun matchesArtist(slot: AgendaScheduleEntry, artist: String): Boolean =
        artist.isNotBlank() && key(name(slot)) == key(artist)

    fun isInLineup(event: AgendaEventSummary, artist: String): Boolean =
        artist.isNotBlank() && (event.artists.any { key(it) == key(artist) } ||
            event.schedule.any { matchesArtist(it, artist) })

    fun slots(event: AgendaEventSummary, artist: String,
              published: List<AgendaScheduleEntry>? = null): List<AgendaScheduleEntry> {
        if (artist.isBlank() || !event.isFestival) return emptyList()
        // Prefer PHP special-events/public.php. Fall back only if the remote
        // response is empty or its explicit programme omits the group.
        val remote = published.orEmpty().filter { matchesArtist(it, artist) }
        val local = event.schedule.filter { matchesArtist(it, artist) }
        return (if (remote.isNotEmpty()) remote else local)
            .filter { it.date.length >= 10 && it.date.take(10).matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }
            .distinctBy { listOf(it.date.take(10), it.time.take(5), key(it.location), key(name(it)), key(it.title)) }
            .sortedWith(compareBy<AgendaScheduleEntry> { it.date }.thenBy { it.time }.thenBy { it.location })
    }

    fun firstVerified(event: AgendaEventSummary, artist: String,
                      published: List<AgendaScheduleEntry>? = null): AgendaScheduleEntry? =
        slots(event, artist, published).firstOrNull()

    /** Preserve the event ID; no fake artist-specific event or duplicate day cards. */
    fun searchRows(events: List<AgendaEventSummary>, artist: String,
                   published: Map<Int, List<AgendaScheduleEntry>>): List<AgendaEventSummary> {
        val seenFestivals = mutableSetOf<Int>()
        return events.asSequence().filter { !it.isFestival || seenFestivals.add(it.id) }
            .map { event ->
                val slot = firstVerified(event, artist, published[event.id])
                if (event.isFestival && slot != null) event.copy(feedOccurrenceDate = slot.date.take(10))
                else if (event.isFestival) event.copy(feedOccurrenceDate = "")
                else event
            }.sortedWith(compareBy<AgendaEventSummary> { it.displayDate.ifBlank { "9999" } }
                .thenBy { event -> firstVerified(event, artist, published[event.id])?.time ?: event.time }
                .thenBy { it.id }).toList()
    }
}
