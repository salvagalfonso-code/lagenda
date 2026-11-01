package com.example.agendavalenciana.ios

/** Android v336 Batecs home selection: seven followed artists unlock exactly six circles.
 * Server batecs/index.php provides recent manual publications; recent-events.php provides
 * real event publication timestamps. Empty followed artists can show a grey placeholder.
 * This is a RAM-only projection; there is no per-device Batecs authority.
 */
object IosBatecFeedContract {
    fun norm(raw: String) = com.example.agendavalenciana.model.IosBatecNames.canonical(raw)

    fun artistForEvent(event: AgendaEventSummary, followed: Set<String>): String? =
        (event.artists + event.schedule.map { it.artist }).firstOrNull { norm(it) in followed && it.isNotBlank() }

    fun acceptsDate(event: AgendaEventSummary, today: String): Boolean {
        val ending = event.endDate.ifBlank { event.date }.take(10)
        return IosUnfilteredAgenda.validDay(ending) && ending >= today
    }

    /** Never reassign a manual post to its author when author is not the artist.
     * Only event programme names or a direct server artist name can prove identity.
     */
    fun project(
        followedNames: Set<String>, blockedNames: Set<String>,
        events: List<AgendaEventSummary>, manual: List<IosBatec>,
        recentEvents: Map<Int, String>, today: String
    ): List<IosBatec> {
        val blocked = blockedNames.map(::norm).toSet()
        val eligible = followedNames.filter { it.isNotBlank() && norm(it) !in blocked }
            .distinctBy(::norm).sortedBy(::norm)
        if (eligible.size <= 6) return emptyList()
        val byKey = eligible.associateBy(::norm)
        val eligibleKeys = byKey.keys
        val eventMap = events.associateBy { it.id }
        val result = linkedMapOf<String, IosBatec>()
        val directManualPairs = hashSetOf<Pair<Int, String>>()
        for (item in manual) {
            val event = eventMap[item.eventId] ?: continue
            if (!acceptsDate(event, today)) continue
            val artist = if (norm(item.artist) in eligibleKeys) byKey[norm(item.artist)]
                else artistForEvent(event, eligibleKeys)?.let { byKey[norm(it)] }
            if (artist == null) continue
            val key = item.storyKey.takeIf { it.matches(Regex("manual:\\d+")) } ?: continue
            result.putIfAbsent(key, item.copy(artist = artist, sourceType = "MANUAL", eventDate = event.date))
            directManualPairs += item.eventId to norm(artist)
        }
        // Only events expressly returned by the existing 48-hour server API become fresh auto Batecs.
        for (event in events) {
            val published = recentEvents[event.id]?.takeIf(String::isNotBlank) ?: continue
            if (!acceptsDate(event, today)) continue
            val names = (event.artists + event.schedule.map { it.artist }).mapNotNull { byKey[norm(it)] }
                .distinctBy(::norm)
            for (artist in names) {
                if ((event.id to norm(artist)) in directManualPairs) continue
                val key = "event:${event.id}:${norm(artist)}"
                result.putIfAbsent(key, IosBatec(
                    id = "auto_${event.id}", eventId = event.id, artist = artist,
                    eventTitle = event.title, text = listOf(event.town, event.date, event.time)
                        .filter(String::isNotBlank).joinToString(" · "), imageUrl = event.posterUrl,
                    storyKey = key, sourceType = "AUTO_EVENT", publishedAt = published, eventDate = event.date
                ))
            }
        }
        // Android also gives a last known upcoming event, otherwise a grey empty circle.
        for (artist in eligible) {
            if (result.values.any { norm(it.artist) == norm(artist) }) continue
            val last = events.filter { event -> acceptsDate(event, today) &&
                (event.artists + event.schedule.map { it.artist }).any { norm(it) == norm(artist) } }
                .maxWithOrNull(compareBy<AgendaEventSummary> { it.updatedAt }.thenBy { it.id })
            if (last != null) {
                val key = "event:${last.id}:${norm(artist)}"
                result.putIfAbsent(key, IosBatec("fallback_${last.id}", last.id, artist, last.title,
                    "${last.town} · ${last.date} · ${last.time}", last.posterUrl,
                    key, "EVENT_FALLBACK", last.updatedAt, last.date))
            } else {
                val key = "followed:${norm(artist)}"
                result.putIfAbsent(key, IosBatec("placeholder", 0, artist, "", "Sense novetats", "",
                    key, "FOLLOWED_PLACEHOLDER"))
            }
        }
        return result.values.toList()
    }
}
