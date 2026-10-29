package com.example.agendavalenciana.ios

import com.example.agendavalenciana.model.IosBatecTimelinePolicy
import com.example.agendavalenciana.model.IosBatecTime

/** Mirrors Android's AgendaState.activeStories(): server publications + real future
 * events for followed artists, with grey followed-artist entries as a last fallback.
 * Never invents a published Batec or represents an unverified creation as 'new'. */
internal object IosBatecsFeed {
    fun assemble(
        server: List<IosBatec>,
        follows: List<IosFollow>,
        catalog: List<AgendaEventSummary>,
        recentEventCreations: Map<Int, Long>,
        nowMillis: Long,
        todayIso: String,
        excludedArtists: Set<String> = emptySet()
    ): List<IosBatec> {
        val hidden = excludedArtists.map(IosBatecTimelinePolicy::normal).toSet()
        val followed = follows.asSequence()
            .filter { it.type.equals("ARTIST", ignoreCase = true) }
            .map { it.label.ifBlank { it.key }.trim() }
            .filter(String::isNotBlank)
            .distinctBy(IosBatecTimelinePolicy::normal)
            .filter { IosBatecTimelinePolicy.normal(it) !in hidden }
            .toList()
        // Android shows exactly six circles only after following at least seven artists.
        if (followed.size <= 6) return emptyList()
        val lookup = followed.associateBy(IosBatecTimelinePolicy::normal)
        val events = catalog.filter { item ->
            val lastDate = item.endDate.ifBlank { item.date }.take(10)
            lastDate.length == 10 && lastDate >= todayIso
        }.distinctBy { it.id }
        val eventsById = events.associateBy { it.id }
        val eventsByArtist = linkedMapOf<String, MutableList<AgendaEventSummary>>()
        events.forEach { event ->
            (event.artists + event.schedule.map { it.artist }).asSequence()
                .map(IosBatecTimelinePolicy::normal)
                .filter { it in lookup }
                .distinct()
                .forEach { key -> eventsByArtist.getOrPut(key) { mutableListOf() }.add(event) }
        }
        val itemsByArtist = linkedMapOf<String, MutableList<IosBatec>>()
        fun add(key: String, item: IosBatec) {
            itemsByArtist.getOrPut(key) { mutableListOf() }.add(item)
        }
        val manualEvents = mutableSetOf<Pair<Int, String>>()
        server.forEach { publication ->
            val event = eventsById[publication.eventId]
            // A publication linked to an expired event must not reappear on the feed.
            if (publication.eventId > 0 && event == null) return@forEach
            val direct = IosBatecTimelinePolicy.normal(publication.artist)
            val artist = when {
                direct in lookup -> direct
                event != null -> (event.artists + event.schedule.map { it.artist })
                    .map(IosBatecTimelinePolicy::normal).firstOrNull { it in lookup }
                else -> null
            } ?: return@forEach
            val key = publication.storyKey.ifBlank { "manual:${publication.id}" }
            if (!IosBatecTimelinePolicy.isServerSeenKey(key)) return@forEach
            add(artist, publication.copy(artist = lookup.getValue(artist), storyKey = key))
            if (publication.eventId > 0) manualEvents.add(publication.eventId to artist)
        }
        followed.forEachIndexed { index, artistName ->
            val key = IosBatecTimelinePolicy.normal(artistName)
            val upcoming = eventsByArtist[key].orEmpty()
            upcoming.forEach { event ->
                val created = recentEventCreations[event.id]
                    ?: IosBatecTime.epochMillis(event.createdAt) ?: 0L
                if (!IosBatecTimelinePolicy.recent(created, nowMillis) ||
                    (event.id to key) in manualEvents) return@forEach
                add(key, IosBatec(
                    id = "event:${event.id}:$key", eventId = event.id, artist = artistName,
                    eventTitle = event.title, text = "${event.town} · ${event.date} ${event.time}".trim(),
                    imageUrl = event.posterUrl, storyKey = "event:${event.id}:$key",
                    publishedAtMillis = created, sourceType = "AUTO_EVENT"
                ))
            }
            if (itemsByArtist[key].isNullOrEmpty()) {
                val latest = upcoming.maxWithOrNull(compareBy<AgendaEventSummary> {
                    recentEventCreations[it.id] ?: IosBatecTime.epochMillis(it.createdAt) ?: 0L
                }.thenBy { it.id })
                if (latest != null) {
                    add(key, IosBatec(
                        id = "event:${latest.id}:$key", eventId = latest.id, artist = artistName,
                        eventTitle = latest.title,
                        text = listOf(latest.town, latest.date, latest.time).filter(String::isNotBlank).joinToString(" · "),
                        imageUrl = latest.posterUrl, storyKey = "event:${latest.id}:$key",
                        // Falls back to a grey event when its real creation time is unknown/old.
                        publishedAtMillis = recentEventCreations[latest.id]
                            ?: IosBatecTime.epochMillis(latest.createdAt) ?: 0L,
                        sourceType = "AUTO_EVENT"
                    ))
                } else {
                    add(key, IosBatec(
                        id = "followed:$key:$index", eventId = 0, artist = artistName,
                        eventTitle = "Sense novetats", text = "Encara no hi ha novetats d'este artista.",
                        imageUrl = "", storyKey = "followed:$key", sourceType = "FOLLOWED_PLACEHOLDER"
                    ))
                }
            }
        }
        return followed.flatMap { artist ->
            itemsByArtist[IosBatecTimelinePolicy.normal(artist)].orEmpty()
                .distinctBy(IosBatec::storyKey).sortedByDescending { it.publishedAtMillis }.take(20)
        }
    }
}
