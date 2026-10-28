package com.example.agendavalenciana.state

import java.time.LocalDate

/** Regles pures del carrusel: només seguits, un cercle per artista i vistos en gris. */
internal object BatecVisibilityPolicy {
    fun visibleFollowedArtists(
        followed: Collection<String>,
        blocked: Collection<String>,
        normalize: (String) -> String
    ): List<String> {
        val excluded = blocked.map(normalize).toSet()
        return followed.map(String::trim)
            .filter(String::isNotBlank)
            .distinctBy(normalize)
            .filter { normalize(it) !in excluded }
            .sortedBy(normalize)
    }

    fun participatingFollowedArtists(
        artist: String,
        lineup: Collection<String>,
        followed: Collection<String>,
        normalize: (String) -> String
    ): List<String> {
        val followedKeys = followed.map(normalize).toSet()
        return (sequenceOf(artist) + lineup.asSequence())
            .filter { normalize(it) in followedKeys }
            .distinctBy(normalize)
            .toList()
    }

    fun isGrey(sourceType: String, storyKey: String, seenKeys: Set<String>): Boolean =
        sourceType == "FOLLOWED_PLACEHOLDER" || storyKey in seenKeys

    // Calendari civil valencià: ahir mai és una data futura, tampoc abans de les 05.00.
    // Una data il·legible no és prova d'un concert futur i no es pot mostrar com a tal.
    fun isCurrentOrFutureConcert(startDate: String, endDate: String, today: LocalDate): Boolean {
        val lastDate = endDate.takeIf(String::isNotBlank) ?: startDate
        val parsed = runCatching { LocalDate.parse(lastDate.trim().take(10)) }.getOrNull()
        return parsed != null && !parsed.isBefore(today)
    }

    fun isRecentPublication(publishedAt: Long, nowMillis: Long, lifetimeMillis: Long): Boolean =
        publishedAt > 0L && lifetimeMillis > 0L && nowMillis >= publishedAt &&
            nowMillis - publishedAt < lifetimeMillis

    data class ArtistCandidate<T>(
        val artist: String,
        val items: List<T>,
        val newestUnseenAt: Long,
        val newestAnyAt: Long
    )

    /** Sis cercles exactes, només a partir de 7 artistes seguits vàlids.
     * Les novetats sense vore passen per davant de les ja vistes/grises.
     */
    fun <T> selectSixArtists(
        candidates: List<ArtistCandidate<T>>,
        eligibleFollowedCount: Int
    ): List<ArtistCandidate<T>> {
        if (eligibleFollowedCount <= 6) return emptyList()
        return candidates.sortedWith(
            compareByDescending<ArtistCandidate<T>> { it.newestUnseenAt > 0L }
                .thenByDescending { it.newestUnseenAt }
                .thenByDescending { it.newestAnyAt }
                .thenBy { it.artist.lowercase() }
        ).take(6)
    }

}
