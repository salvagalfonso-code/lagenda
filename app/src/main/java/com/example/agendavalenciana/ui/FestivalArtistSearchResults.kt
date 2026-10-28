package com.example.agendavalenciana.ui

import com.example.agendavalenciana.model.Concert

/** Una fila de feed = una actuació real, no una jornada inventada del festival. */
internal data class ArtistConcertSearchRow(
    val concert: Concert,
    val artist: String? = null,
    val slot: SpecialScheduleItem? = null
) {
    val key: String get() = buildString {
        // v286: una mateixa fitxa pot tindre diverses jornades/sessions en el feed.
        // L'ID de l'esdeveniment no identifica per si sol cada card d'una LazyColumn;
        // usar la identitat de la instància evita col·lisions després de sincronitzar.
        append(concert.feedInstanceKey).append('|')
        append(festivalArtistKey(artist.orEmpty())).append('|')
        append(slot?.date.orEmpty()).append('|').append(slot?.time.orEmpty()).append('|')
        append(slot?.location.orEmpty()).append('|').append(slot?.title.orEmpty())
    }
    val chronologicalDate: String get() = slot?.date?.take(10)?.takeIf(String::isNotBlank) ?: concert.feedDate
    val chronologicalTime: String get() = slot?.time.orEmpty()
}

/** Prefereix la programació publicada al servidor; mai associa l'hora genèrica del festival al grup. */
internal fun festivalArtistSlots(
    artist: String,
    remoteRows: List<SpecialScheduleItem>,
    localRows: List<SpecialScheduleItem>
): List<SpecialScheduleItem?> {
    fun matches(source: List<SpecialScheduleItem>): List<SpecialScheduleItem> = source.filter { item ->
        val realName = item.artistName?.takeIf(String::isNotBlank) ?: item.title
        festivalArtistKey(realName) == festivalArtistKey(artist) &&
            (!item.itemType.equals("activity", ignoreCase = true) || !item.artistName.isNullOrBlank())
    }
    // Si el servidor té un horari incomplet, aprofitem el registre local que sí identifica el grup.
    val found = matches(remoteRows).ifEmpty { matches(localRows) }
        .distinctBy { row ->
            listOf(row.date.take(10), row.time.trim(), festivalArtistKey(row.location), festivalArtistKey(row.title))
        }
        .sortedWith(compareBy<SpecialScheduleItem> { it.date }.thenBy { it.time }.thenBy { it.sortOrder })
    // La inclusió al cartell sí és informació útil, però no permet atribuir-li un dia ni una hora.
    return if (found.isEmpty()) listOf(null) else found
}

/** Fora de festivals, les sessions continuen tenint la seua targeta normal. */
internal fun buildArtistConcertSearchRows(
    concerts: List<Concert>,
    artist: String,
    scheduleFor: (Concert) -> Pair<List<SpecialScheduleItem>, List<SpecialScheduleItem>>
): List<ArtistConcertSearchRow> = concerts
    .filter { concert ->
        (concert.lineup + concert.artist).any { festivalArtistKey(it) == festivalArtistKey(artist) }
    }
    .distinctBy { concert -> if (concert.isFestivalExperience) "festival_${concert.id}" else concert.feedInstanceKey }
    .flatMap { concert ->
        if (!concert.isFestivalExperience) listOf(ArtistConcertSearchRow(concert))
        else {
            val (remote, local) = scheduleFor(concert)
            // v331: un festival és UNA targeta normal, independent del nombre
            // d'actuacions del grup. El primer horari verificat servix per
            // obrir directament la jornada correcta dins del popup.
            listOf(ArtistConcertSearchRow(concert, artist, festivalArtistSlots(artist, remote, local).firstOrNull()))
        }
    }
    .distinctBy(ArtistConcertSearchRow::key)
    .sortedWith(compareBy<ArtistConcertSearchRow> { it.chronologicalDate }
        .thenBy { it.chronologicalTime }.thenBy { it.concert.id })
