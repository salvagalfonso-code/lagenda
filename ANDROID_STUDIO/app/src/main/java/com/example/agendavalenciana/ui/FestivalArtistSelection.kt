package com.example.agendavalenciana.ui

import com.example.agendavalenciana.model.Concert
import com.example.agendavalenciana.state.AgendaState
import java.text.Normalizer
import java.util.Locale

/** Conserva les seleccions antigues per franja i les noves per nom d'artista. */
internal fun festivalArtistKey(text: String): String = Normalizer.normalize(
    text.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD
).replace(Regex("\\p{M}+"), "").replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()

internal fun festivalSelectionArtist(entry: String): String = entry.substringAfterLast('|').trim()

internal fun festivalSelectedArtistNames(state: AgendaState, concertId: Int): List<String> =
    state.selectedFestivalArtists(concertId).map(::festivalSelectionArtist)
        .filter(String::isNotBlank).distinctBy(::festivalArtistKey).sortedWith(String.CASE_INSENSITIVE_ORDER)

internal fun festivalArtistIsSelected(state: AgendaState, concertId: Int, artist: String): Boolean =
    state.selectedFestivalArtists(concertId).any { festivalArtistKey(festivalSelectionArtist(it)) == festivalArtistKey(artist) }

internal fun toggleFestivalArtistByName(state: AgendaState, concertId: Int, artist: String) {
    val matches = state.selectedFestivalArtists(concertId)
        .filter { '|' !in it && festivalArtistKey(it) == festivalArtistKey(artist) }
    // Els botons antics per nom no han d'eliminar actuacions individuals guardades per hora.
    if (matches.isEmpty()) state.toggleFestivalArtist(concertId, artist)
    else matches.forEach { state.toggleFestivalArtist(concertId, it) }
}

internal fun matchedArtistInFestival(concert: Concert, searchedArtist: String?): String? {
    if (searchedArtist.isNullOrBlank()) return null
    return (concert.lineup + concert.artist).firstOrNull {
        festivalArtistKey(it) == festivalArtistKey(searchedArtist)
    }
}
