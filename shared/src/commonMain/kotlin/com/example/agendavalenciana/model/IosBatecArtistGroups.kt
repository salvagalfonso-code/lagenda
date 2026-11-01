package com.example.agendavalenciana.model

/** Same v336 home rule: >6 eligible followed artists, exactly six circles. */
data class IosBatecArtistGroup(
    val key: String,
    val artist: String,
    val storyKeys: List<String>,
    val openingStoryKey: String,
    val allSeen: Boolean
)

object IosBatecArtistGroups {
    private val storyKeyPattern = Regex("(?:manual:\\d+|event:\\d+:.+|followed:.+)")
    private fun normal(value: String) = IosBatecNames.canonical(value)
    /** PHP seen.php only accepts event:ID (not Android's event:ID:artist). */
    fun canonicalSeenKey(key: String): String =
        if (key.startsWith("event:")) key.substringBefore(':', "event") + ":" +
            key.substringAfter(':').substringBefore(':') else key
    private fun isSeen(candidate: BatecCandidate, seen: Set<String>) =
        candidate.storyKey in seen || canonicalSeenKey(candidate.storyKey) in seen

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
                it.sourceType != "FOLLOWED_PLACEHOLDER" && it.sourceType != "EVENT_FALLBACK" && !isSeen(it, seen)
            }.thenByDescending { it.publishedAt }.thenBy { it.storyKey }).take(20)
            val newestUnseen = content.filter { it.sourceType != "FOLLOWED_PLACEHOLDER" &&
                it.sourceType != "EVENT_FALLBACK" && !isSeen(it, seen) }.maxOfOrNull { it.publishedAt }.orEmpty()
            val newestAny = content.maxOfOrNull { it.publishedAt }.orEmpty()
            Triple(key, content, newestUnseen to newestAny)
        }.sortedWith(compareByDescending<Triple<String, List<BatecCandidate>, Pair<String, String>>> { it.third.first.isNotEmpty() }
            .thenByDescending { it.third.first }.thenByDescending { it.third.second }.thenBy { it.first })
            .take(6).map { (key, content, _) ->
                val ordered = content.map { it.storyKey }
                val unseen = content.firstOrNull { it.sourceType != "FOLLOWED_PLACEHOLDER" &&
                    it.sourceType != "EVENT_FALLBACK" && !isSeen(it, seen) }
                IosBatecArtistGroup(
                    key, content.first().artist, ordered,
                    unseen?.storyKey ?: ordered.first(),
                    content.all { it.sourceType == "FOLLOWED_PLACEHOLDER" ||
                        it.sourceType == "EVENT_FALLBACK" || isSeen(it, seen) }
                )
            }
    }
}
