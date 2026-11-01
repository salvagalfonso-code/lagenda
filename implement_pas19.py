from pathlib import Path
root=Path('/mnt/data/LAGENDA_IOS_TREBALL_ACUMULATIU/ANDROID_STUDIO')
model=root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/model'
ios=root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios'
(model/'IosBatecArtistGroups.kt').write_text('''package com.example.agendavalenciana.model

/** Same v336 home rule: >6 eligible followed artists, exactly six circles. */
data class IosBatecArtistGroup(
    val key: String,
    val artist: String,
    val storyKeys: List<String>,
    val openingStoryKey: String,
    val allSeen: Boolean
)

object IosBatecArtistGroups {
    private val storyKeyPattern = Regex("(?:manual:\\\\d+|event:\\\\d+:.+|followed:.+)")
    private fun normal(value: String) = value.trim().lowercase()

    /** Stories come from the server and the Android-compatible projection; no unfollowed filler.
     * Placeholders are always grey and may only represent a real followed artist.
     */
    fun select(
        candidates: List<BatecCandidate>,
        followedArtists: Set<String>,
        seen: Set<String>,
        minimumArtists: Int = 6
    ): List<IosBatecArtistGroup> {
        val followed = followedArtists.map(::normal).filter(String::isNotBlank).toSet()
        if (followed.size <= 6) return emptyList()
        val grouped = linkedMapOf<String, MutableList<BatecCandidate>>()
        val keys = hashSetOf<String>()
        candidates.forEach { item ->
            val artistKey = normal(item.artist)
            if (item.storyKey.matches(storyKeyPattern) && artistKey in followed && keys.add(item.storyKey)) {
                grouped.getOrPut(artistKey) { mutableListOf() }.add(item)
            }
        }
        if (grouped.size < minimumArtists) return emptyList()
        return grouped.map { (key, items) ->
            val content = items.sortedWith(compareByDescending<BatecCandidate> {
                it.sourceType != "FOLLOWED_PLACEHOLDER" && it.sourceType != "EVENT_FALLBACK" && it.storyKey !in seen
            }.thenByDescending { it.publishedAt }.thenBy { it.storyKey })
            val newestUnseen = content.filter { it.sourceType != "FOLLOWED_PLACEHOLDER" &&
                it.sourceType != "EVENT_FALLBACK" && it.storyKey !in seen }.maxOfOrNull { it.publishedAt }.orEmpty()
            val newestAny = content.maxOfOrNull { it.publishedAt }.orEmpty()
            Triple(key, content, newestUnseen to newestAny)
        }.sortedWith(compareByDescending<Triple<String, List<BatecCandidate>, Pair<String, String>>> { it.third.first.isNotEmpty() }
            .thenByDescending { it.third.first }.thenByDescending { it.third.second }.thenBy { it.first })
            .take(6).map { (key, content, _) ->
                val ordered = content.map { it.storyKey }
                val unseen = content.firstOrNull { it.sourceType != "FOLLOWED_PLACEHOLDER" &&
                    it.sourceType != "EVENT_FALLBACK" && it.storyKey !in seen }
                IosBatecArtistGroup(
                    key, content.first().artist, ordered,
                    unseen?.storyKey ?: ordered.first(),
                    content.all { it.sourceType == "FOLLOWED_PLACEHOLDER" ||
                        it.sourceType == "EVENT_FALLBACK" || it.storyKey in seen }
                )
            }
    }
}
'''.replace('\\\\\\d','\\d'))
# Existing model carries the extra metadata; backwards-compatible for existing callers/tests.
f=model/'FollowedBatecsPolicy.kt'; s=f.read_text();s=s.replace('data class BatecCandidate(val storyKey: String, val artist: String)', '''data class BatecCandidate(
    val storyKey: String, val artist: String,
    val sourceType: String = "MANUAL", val publishedAt: String = ""
)''');f.write_text(s)
# Projection: no derived artist from author, and server data is the source for recency.
(ios/'IosBatecFeedContract.kt').write_text('''package com.example.agendavalenciana.ios

/** Android v336 Batecs home selection: seven followed artists unlock exactly six circles.
 * Server batecs/index.php provides recent manual publications; recent-events.php provides
 * real event publication timestamps. Empty followed artists can show a grey placeholder.
 * This is a RAM-only projection; there is no per-device Batecs authority.
 */
object IosBatecFeedContract {
    fun norm(raw: String) = raw.trim().lowercase().replace(Regex("\\\\s+"), " ")

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
            val key = item.storyKey.takeIf { it.matches(Regex("manual:\\\\d+")) } ?: continue
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
'''.replace('\\\\s','\\s').replace('\\\\\\d','\\d'))
# Add proper params to API function and decode source-only fields.
f=ios/'IosAgendaApi.kt';s=f.read_text()
a=s.index('    /** Followed artists are sorted first by the server.')
b=s.index('    /** Seen state is account-specific',a)
s=s[:a]+'''    /** Same data endpoints as Android; DO NOT use manual post author as the artist.
     * Android generates additional items from recent-events.php and followed artists.
     * Avoid scanning at all when fewer than seven artists are followed.
     */
    suspend fun batecs(
        followedArtists: Set<String>, catalog: List<AgendaEventSummary>, blockedArtists: Set<String> = emptySet()
    ): List<IosBatec> {
        val eligible = followedArtists.map(IosBatecFeedContract::norm).filter(String::isNotBlank)
            .toSet() - blockedArtists.map(IosBatecFeedContract::norm).toSet()
        if (eligible.size <= 6) return emptyList()
        val owner = activePersonalId() ?: return emptyList()
        val manual = linkedMapOf<String, IosBatec>()
        var offset = 0
        repeat(6) { // PHP may return 300 records; scan all, not just 50 from the first artist.
            val page = request("batecs/index.php?offset=$offset&limit=50", authenticated = true)
            if (activePersonalId() != owner) return emptyList()
            val items = page.items("batecs")
            items.forEach { item ->
                val id = item.text("id").toIntOrNull()?.takeIf { it > 0 } ?: return@forEach
                val eventId = item.text("event_id").toIntOrNull() ?: return@forEach
                val key = item.text("story_key").ifBlank { "manual:$id" }
                manual.putIfAbsent(key, IosBatec(
                    id.toString(), eventId, item.text("event_artist"),
                    item.text("event_title", "title"), item.text("body", "caption", "text"),
                    validMediaUrl(item.text("image_url", "media_url")), key, "MANUAL",
                    item.text("published_at"), item.text("event_date")
                ))
            }
            val nextOffset = page.text("next_offset").toIntOrNull()?.takeIf { it > offset }
                ?: offset + items.size
            if (!page.bool("has_more") || items.isEmpty()) {
                offset = -1
                return@repeat
            }
            offset = nextOffset
        }
        val recent = runCatching {
            request("batecs/recent-events.php", authenticated = true).items("events")
                .mapNotNull { obj ->
                    obj.text("event_id").toIntOrNull()?.let { it to obj.text("created_at") }
                }.toMap()
        }.getOrDefault(emptyMap())
        if (activePersonalId() != owner) return emptyList()
        return IosBatecFeedContract.project(followedArtists, blockedArtists, catalog,
            manual.values.toList(), recent, currentLocalDateIso())
    }
''' + s[b:]
# fix loop short circuit
s=s.replace('''        repeat(6) { // PHP may return 300 records; scan all, not just 50 from the first artist.
            val page''','''        for (pageIndex in 0 until 6) { // PHP returns at most 300; do not stop at one artist.
            val page''')
s=s.replace('''            if (!page.bool("has_more") || items.isEmpty()) {
                offset = -1
                return@repeat
            }
            offset = nextOffset
        }
        val recent''','''            if (!page.bool("has_more") || items.isEmpty()) break
            offset = nextOffset
        }
        val recent''')
s=s.replace('data class IosBatec(val id: String, val eventId: Int, val artist: String, val eventTitle: String, val text: String, val imageUrl: String, val storyKey: String = "")', '''data class IosBatec(val id: String, val eventId: Int, val artist: String, val eventTitle: String,
    val text: String, val imageUrl: String, val storyKey: String = "",
    val sourceType: String = "MANUAL", val publishedAt: String = "", val eventDate: String = "")''')
f.write_text(s)
# UI: one consistent fetch triggered by following + catalog + session, not duplicated under two methods
f=ios/'AgendaIosScreen.kt';s=f.read_text()
s=s.replace('''                batecs = runCatching { api.batecs(user.follows.filter { it.type.equals("ARTIST", true) }
                    .map { it.label }.toSet()) }.getOrDefault(batecs)
''','')
s=s.replace('''                // A session may be restored after the public catalog was loaded.
                // Re-fetch the followed Batecs without clearing the existing strip on failure.
                val newBatecs = runCatching { api.batecs(newUser.follows
                    .filter { it.type.equals("ARTIST", true) }.map { it.label }.toSet()) }.getOrNull()
''','')
s=s.replace('''                        newBatecs?.let { batecs = it }
''','')
# following changes and account changes invalidate async Batecs; no need a second private cache
needle='''    val followedGroups = remember(batecs, user.follows, seenBatecs) {
'''
insert='''    // Pas 19: single server-backed Batecs refresh, including newly followed artists.
    // A stale response may never cross a guest/registered account boundary.
    LaunchedEffect(events, user.follows, excludedArtists, account?.id, guestReadyUserId) {
        val owner = account?.id ?: guestReadyUserId
        val follows = user.follows.filter { it.type.equals("ARTIST", true) }
            .map { it.label.ifBlank { it.key } }.filter(String::isNotBlank).toSet()
        val eligible = follows.map(IosBatecFeedContract::norm).toSet() -
            excludedArtists.map(IosBatecFeedContract::norm).toSet()
        if (owner == null || eligible.size <= 6) { batecs = emptyList(); return@LaunchedEffect }
        val next = runCatching { api.batecs(follows, events, excludedArtists) }.getOrNull()
        if (next != null && (account?.id ?: guestReadyUserId) == owner) batecs = next
    }
    val followedGroups = remember(batecs, user.follows, seenBatecs, excludedArtists) {
'''
assert needle in s
s=s.replace(needle,insert,1)
s=s.replace('''            batecs.map { BatecCandidate(it.storyKey, it.artist) },
            user.follows.filter { it.type.equals("ARTIST", true) }.map { it.label }.toSet(),
''','''            batecs.map { BatecCandidate(it.storyKey, it.artist, it.sourceType, it.publishedAt) },
            user.follows.filter { it.type.equals("ARTIST", true) }
                .map { it.label.ifBlank { it.key } }.filter { label ->
                    excludedArtists.none { IosBatecFeedContract.norm(it) == IosBatecFeedContract.norm(label) }
                }.toSet(),
''')
# for placeholder/fallback no mark seen; existing mark method restricts anyway.
s=s.replace('''                    val pending = group.storyKeys.filter { it !in seenBatecs }
''','''                    val pending = group.storyKeys.filter { key -> key !in seenBatecs &&
                        batecs.any { it.storyKey == key && it.sourceType in setOf("MANUAL", "AUTO_EVENT") } }
''')
# remove stale Batecs across account switches
s=s.replace('''        excludedEventIds = emptySet(); excludedArtists = emptySet(); excludedCategories = emptySet()
        serverState = null''','''        excludedEventIds = emptySet(); excludedArtists = emptySet(); excludedCategories = emptySet()
        batecs = emptyList()
        serverState = null''')
f.write_text(s)
print('Edited files',*(str(x) for x in (model/'IosBatecArtistGroups.kt',model/'FollowedBatecsPolicy.kt',ios/'IosBatecFeedContract.kt',ios/'IosAgendaApi.kt',ios/'AgendaIosScreen.kt')),sep='\n')
