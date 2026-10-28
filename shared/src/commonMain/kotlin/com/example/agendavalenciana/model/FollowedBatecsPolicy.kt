package com.example.agendavalenciana.model

/** Shared, deterministic selection of Batecs. Never shows an unfollowed artist. */
data class BatecCandidate(val storyKey: String, val artist: String)

object FollowedBatecsPolicy {
    private fun normalized(value: String): String = value.trim().lowercase()

    /** Retains the server's order and unique story IDs; hides the strip below six items. */
    fun firstSix(candidates: List<BatecCandidate>, followedArtists: Set<String>): List<String> {
        val followed = followedArtists.map(::normalized).filter(String::isNotBlank).toSet()
        if (followed.isEmpty()) return emptyList()
        val matches = candidates.asSequence()
            .filter { it.storyKey.isNotBlank() && normalized(it.artist) in followed }
            .distinctBy(BatecCandidate::storyKey)
            .take(6)
            .map(BatecCandidate::storyKey).toList()
        return if (matches.size == 6) matches else emptyList()
    }
}
