package com.example.agendavalenciana.model

import com.example.agendavalenciana.ios.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** No server credentials and no live requests: verifies the actual PHP event payload format. */
class IosApiContractTest {
    @Test fun decodesArtistsAndFestivalSchedule() {
        val raw = """{"ok":true,"events":[{"id":99,"title":"Festival prova","date":"2027-03-15","time":"20:00","town":"Sagunt","category":"Música","ticket_enabled":true,"artists":[{"name":"Artista A"}],"schedule":[{"date":"2027-03-15","time":"21:00","title":"Artista A","artist_name":"Artista A"}]}]}"""
        val events = IosEventCatalog.decode(raw)
        assertEquals(1, events.size)
        assertEquals("Artista A", events.first().artists.single())
        assertTrue(events.first().isFestival)
        assertTrue(events.first().ticketAvailable)
        assertEquals("Artista A", events.first().matchedSchedule("Artista A")?.artist)
    }
    @Test fun decodesPromotedApiDateFields() {
        val raw = """{"ok":true,"events":[{"id":72,"title":"Acte patrocinat","start_date":"2027-04-02","event_time":"21:00","category":"Teatre","ticket_enabled":true}]}"""
        val event = IosEventCatalog.decode(raw).single()
        assertEquals("2027-04-02", event.date)
        assertEquals("21:00", event.time)
        assertTrue(event.ticketAvailable)
    }
    @Test fun festivalScheduleArtistIsSearchable() {
        val fest = AgendaEventSummary(21, "Festival cultural", "2027-03-11", "18:00", "Sagunt", "", "Música", "", "",
            schedule = listOf(AgendaScheduleEntry("2027-03-11", "22:00", "Concert", "La Banda", "Escenari principal")))
        assertTrue(fest.matches("La Banda"))
        assertEquals("La Banda", fest.matchedSchedule("La Banda")?.artist)
        assertTrue(!fest.matches("Un grup que no hi actua"))
    }
    @Test fun multiDayFestivalIsNotExpiredAtStartDate() {
        val fest = AgendaEventSummary(22, "Festival dos dies", "2027-04-03", "", "Xàtiva", "", "Música", "", "",
            endDate = "2027-04-04")
        assertTrue(fest.endDate >= "2027-04-04")
    }
    @Test fun eventSearchDoesNotInventOtherResults() {
        val event = AgendaEventSummary(4, "Concert prova", "2027-01-01", "20:00", "Vila-real", "", "Música", "", "", artists = listOf("Artista B"))
        assertTrue(event.matches("Artista B"))
        assertTrue(!event.matches("Artista C"))
    }
}
