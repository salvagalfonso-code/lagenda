package com.example.agendavalenciana.state

import java.text.Normalizer
import java.util.Locale

/** Un perfil només es prioritza si la cerca identifica un artista sense ambigüitat. */
internal object ArtistSearchPriority {
    private fun key(v: String) = Normalizer.normalize(
        v.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD
    ).replace(Regex("\\p{M}+"), "").replace(Regex("\\s+"), " ")

    fun resolve(query: String, artists: Collection<String>): String? {
        val needle = key(query)
        if (needle.length < 2) return null
        val unique = artists.map(String::trim).filter(String::isNotBlank).distinctBy(::key)
        unique.firstOrNull { key(it) == needle }?.let { return it }
        if (needle.length < 3) return null
        return unique.filter { key(it).contains(needle) }.singleOrNull()
    }

    fun isFollowing(artist: String, followed: Collection<String>): Boolean =
        followed.any { key(it) == key(artist) }
}
