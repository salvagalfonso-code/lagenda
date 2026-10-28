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
    private val storyKeyPattern = Regex("(?:manual|event):\\d+")
    private fun normal(value: String) = value.trim().lowercase()

    /** Six different followed artists are required; no fake stories or unfollowed filler. */
    fun select(
        candidates: List<BatecCandidate>,
        followedArtists: Set<String>,
        seen: Set<String>,
        minimumArtists: Int = 6
    ): List<IosBatecArtistGroup> {
        val followed = followedArtists.map(::normal).filter(String::isNotBlank).toSet()
        if (followed.isEmpty()) return emptyList()
        val grouped = linkedMapOf<String, MutableList<BatecCandidate>>()
        val uniqueStoryKeys = hashSetOf<String>()
        candidates.forEach { candidate ->
            val artistKey = normal(candidate.artist)
            if (candidate.storyKey.matches(storyKeyPattern) &&
                artistKey in followed && uniqueStoryKeys.add(candidate.storyKey)) {
                grouped.getOrPut(artistKey) { mutableListOf() }.add(candidate)
            }
        }
        if (grouped.size < minimumArtists) return emptyList()
        return grouped.map { (artistKey, items) ->
            val keys = items.map { it.storyKey }
            IosBatecArtistGroup(
                key = artistKey,
                artist = items.first().artist,
                storyKeys = keys,
                openingStoryKey = keys.firstOrNull { it !in seen } ?: keys.first(),
                allSeen = keys.all { it in seen }
            )
        }
    }
}
