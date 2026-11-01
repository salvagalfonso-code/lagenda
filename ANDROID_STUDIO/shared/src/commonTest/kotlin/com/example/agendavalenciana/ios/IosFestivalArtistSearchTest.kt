package com.example.agendavalenciana.ios

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class IosFestivalArtistSearchTest {
    private val festival = AgendaEventSummary(20, "Festival", "2026-11-01", "17:00",
        "Sagunt", "Parc", "Música", "", "", artists = listOf("La Maria"), endDate = "2026-11-04")
    private val set = listOf(
        AgendaScheduleEntry("2026-11-03", "22:00", "Concert", "La Maria", "Escenari A"),
        AgendaScheduleEntry("2026-11-02", "21:00", "Concert", "Là María", "Escenari B"),
        AgendaScheduleEntry("2026-11-03", "23:00", "Concert", "Maria d'Alt", "Escenari A"))

    @Test fun typedArtistBehavesAsAndroidPriorityOnlyIfUnique() {
        val pool = listOf(
            IosSearchSuggestion(IosSearchType.ARTIST, "La Maria"),
            IosSearchSuggestion(IosSearchType.ARTIST, "Maria d'Alt"))
        assertEquals("La Maria", IosFestivalArtistSearch.resolveArtist("la mar", null, pool))
        assertNull(IosFestivalArtistSearch.resolveArtist("maria", null, pool))
        assertNull(IosFestivalArtistSearch.resolveArtist("la", null, pool))
        assertNull(IosFestivalArtistSearch.resolveArtist("La Maria",
            IosSearchSuggestion(IosSearchType.TOWN, "La Maria"), pool))
    }

    @Test fun programmeGivesActualDateTimeAndStage() {
        val matches = IosFestivalArtistSearch.slots(festival, "La Maria", set)
        assertEquals(2, matches.size)
        assertEquals("2026-11-02", matches.first().date)
        assertEquals("21:00", matches.first().time)
        assertEquals("Escenari B", matches.first().location)
        assertFalse(IosFestivalArtistSearch.matchesArtist(set.last(), "La Maria"))
    }

    @Test fun noScheduleMeansNoInventedArtistPerformance() {
        assertEquals("", IosFestivalArtistSearch.artistLabel(AgendaScheduleEntry(
            "2026-11-03", "20:00", "Concert", "", "")))
        assertNull(IosFestivalArtistSearch.firstVerified(festival, "La Maria"))
        assertTrue(IosFestivalArtistSearch.slots(festival, "La Maria",
            listOf(AgendaScheduleEntry("", "20:00", "Concert", "La Maria", ""))).isEmpty())
    }

    @Test fun oneFestivalCardKeepsTheOriginalIdAndUsesTheArtistDay() {
        val rows = IosFestivalArtistSearch.searchRows(listOf(
            festival.copy(feedOccurrenceDate = "2026-11-02"), festival,
            AgendaEventSummary(30, "Concert", "2026-11-06", "20:00", "Sagunt",
                "Sala", "Música", "", "", artists = listOf("La Maria"))),
            "La Maria", mapOf(20 to set))
        assertEquals(2, rows.size)
        assertEquals(20, rows.first().id)
        assertEquals("2026-11-02", rows.first().displayDate)
        assertEquals("2026-11-01", rows.first().date)
        val unconfirmed = IosFestivalArtistSearch.searchRows(listOf(
            festival.copy(feedOccurrenceDate = "2026-11-02")), "La Maria", emptyMap())
        assertEquals("2026-11-01", unconfirmed.single().displayDate)
    }
}
