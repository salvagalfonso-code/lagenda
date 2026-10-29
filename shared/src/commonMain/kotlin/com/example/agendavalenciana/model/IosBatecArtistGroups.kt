package com.example.agendavalenciana.model

/** One visual story circle per followed artist, not per server publication. */
data class IosBatecArtistGroup(
    val key: String,
    val artist: String,
    val storyKeys: List<String>,
    val openingStoryKey: String,
    val allSeen: Boolean
)

object IosBatecArtistGroups {
    private fun normal(value: String) = IosBatecTimelinePolicy.normal(value)

    /** Android parity: seven followed artists enable six circles; grey placeholders are explicit. */
    fun select(
        candidates: List<BatecCandidate>,
        followedArtists: Set<String>,
        seen: Set<String>,
        nowMillis: Long = 0L,
        minimumFollowedArtists: Int = 7
    ): List<IosBatecArtistGroup> {
        val followed = followedArtists.map(::normal).filter(String::isNotBlank).toSet()
        if (followed.size < minimumFollowedArtists) return emptyList()
        val grouped = linkedMapOf<String, MutableList<BatecCandidate>>()
        val uniqueStoryKeys = hashSetOf<String>()
        candidates.forEach { candidate ->
            val artistKey = normal(candidate.artist)
            if (IosBatecTimelinePolicy.isBatecKey(candidate.storyKey) &&
                artistKey in followed && uniqueStoryKeys.add(candidate.storyKey)) {
                grouped.getOrPut(artistKey) { mutableListOf() }.add(candidate)
            }
        }
        if (grouped.size < 6) return emptyList()
        val ranked = grouped.map { (artistKey, items) ->
            val keys = items.map { it.storyKey }
            val unseen = items.filter { candidate ->
                !candidate.isPlaceholder && candidate.storyKey !in seen &&
                    IosBatecTimelinePolicy.recent(candidate.publishedAtMillis, nowMillis)
            }
            val mostRecent = items.maxOfOrNull { it.publishedAtMillis } ?: 0L
            val newestUnseen = unseen.maxOfOrNull { it.publishedAtMillis } ?: 0L
            val result = IosBatecArtistGroup(
                key = artistKey,
                artist = items.first().artist,
                storyKeys = keys,
                openingStoryKey = unseen.firstOrNull()?.storyKey ?: keys.first(),
                allSeen = unseen.isEmpty()
            )
            Triple(result, newestUnseen, mostRecent)
        }
        return ranked.sortedWith(compareByDescending<Triple<IosBatecArtistGroup, Long, Long>> { it.second > 0L }
            .thenByDescending { it.second }.thenByDescending { it.third }.thenBy { it.first.key })
            .take(6).map { it.first }
    }
}
