package com.example.agendavalenciana.model

import java.text.Normalizer
import java.util.Locale

/** Les exclusions permanents no depenen dels filtres de data/territori del feed. */
object UserVisibilityRules {
    private fun key(value: String): String = Normalizer.normalize(
        value.trim().lowercase(Locale.ROOT), Normalizer.Form.NFKD
    ).replace(Regex("\\p{M}+"), "")
        .replace(Regex("[’‘`´]"), "'")
        .replace(Regex("[‐‑‒–—−]"), "-")
        .replace(Regex("\\s+"), " ")

    fun artists(concert: Concert): List<String> {
        // Si un festival té cartell de diverses bandes, no afegir-hi com a artista
        // el títol de l'esdeveniment que l'API pot haver usat com a fallback.
        val explicit = concert.lineup.map(String::trim).filter(String::isNotBlank)
        val candidates = if (explicit.isEmpty()) listOf(concert.artist) else explicit
        val meaningful = candidates.map(String::trim).filter(String::isNotBlank)
            .distinctBy(::key)
            .filterNot { key(it) == key(concert.title) }
        return if (meaningful.isNotEmpty()) meaningful
        else if (concert.category == CulturalCategory.MUSIC && concert.artist.isNotBlank())
            listOf(concert.artist.trim())
        else emptyList()
    }

    fun artistIsBlocked(artist: String, blockedArtists: Set<String>): Boolean =
        blockedArtists.any { key(it) == key(artist) }

    /**
     * Un artista bloquejat oculta només esdeveniments on tots els artistes
     * realment vinculats també estiguen bloquejats. Un cartell mixt continua visible.
     */
    fun isVisible(
        concert: Concert,
        hiddenEventIds: Set<Int>,
        blockedCategories: Set<CulturalCategory>,
        blockedArtists: Set<String>
    ): Boolean {
        if (concert.id in hiddenEventIds || concert.category in blockedCategories) return false
        val artists = artists(concert)
        return artists.isEmpty() || artists.any { !artistIsBlocked(it, blockedArtists) }
    }
}
