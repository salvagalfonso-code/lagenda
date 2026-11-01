package com.example.agendavalenciana.ios

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IosUnfilteredAgendaTest {
    private fun event(id: Int, date: String, end: String = "", kind: String = "", category: String = "Música",
                      dates: List<String> = emptyList()) = AgendaEventSummary(
        id = id, title = "Festival de prova", date = date, time = "20:00", town = "Sagunt",
        venue = "", category = category, description = "", posterUrl = "", endDate = end,
        eventStructure = kind,
        schedule = dates.map { AgendaScheduleEntry(it, "21:00", "", "Grup", "") }
    )

    @Test fun futureConsecutiveFestivalHasOneChangingCard() {
        val e = event(1,"2026-10-28","2026-10-31","FESTIVAL",dates = listOf("2026-10-28","2026-10-29","2026-10-30"))
        assertEquals(listOf("2026-10-28"), IosUnfilteredAgenda.select(listOf(e), "2026-10-27").map { it.displayDate })
        assertEquals(listOf("2026-10-29"), IosUnfilteredAgenda.select(listOf(e), "2026-10-29").map { it.displayDate })
        assertTrue(IosUnfilteredAgenda.select(listOf(e), "2026-10-31").isEmpty())
        assertTrue(IosUnfilteredAgenda.isFestivalBeforeStart(e, "2026-10-27"))
        assertFalse(IosUnfilteredAgenda.isFestivalBeforeStart(e, "2026-10-29"))
    }

    @Test fun cycleRendersOnlyItsExplicitUpcomingSessions() {
        val e = event(2,"2026-10-28","2026-11-30","CYCLE",dates = listOf("2026-10-28","2026-11-07","2026-11-15"))
        val result = IosUnfilteredAgenda.select(listOf(e), "2026-10-29")
        assertEquals(listOf("2026-11-07", "2026-11-15"), result.map { it.displayDate })
        assertEquals(setOf(2), result.map { it.id }.toSet())
        assertEquals("2026-10-28", result.first().date) // canonical event date is untouched
    }

    @Test fun noPhantomFestivalDatesAndNoPastEvents() {
        assertTrue(IosUnfilteredAgenda.select(listOf(event(3, "2026-10-01", "2026-12-31", "FESTIVAL")), "2026-10-29").isEmpty())
        assertTrue(IosUnfilteredAgenda.select(listOf(event(4, "2026-10-01", "2026-10-20")), "2026-10-29").isEmpty())
    }

    @Test fun exhibitionsAreOneCardNotDailyFestival() {
        val long = event(5, "2026-10-01", "2026-10-20", category = "Exposicions")
        assertEquals("2026-10-08", IosUnfilteredAgenda.select(listOf(long), "2026-10-03").single().displayDate)
        val short = event(6, "2026-10-01", "2026-10-07", category = "Exposicions")
        assertEquals("2026-10-05", IosUnfilteredAgenda.select(listOf(short), "2026-10-05").single().displayDate)
    }

    @Test fun preservesServerDayIdentityAndSortsChronologically() {
        val scheduled = event(10, "2026-10-20").copy(feedOccurrenceDate = "2026-11-05", eventStructure = "STANDARD")
        val result = IosUnfilteredAgenda.select(listOf(event(11,"2026-11-06",kind="STANDARD"), scheduled, scheduled),"2026-11-01")
        assertEquals(listOf(10,11),result.map { it.id })
        assertEquals("10_2026-11-05", result.first().key)
    }

    @Test fun dedupeDoesNotHidePaidActivity() {
        val normal = event(30,"2026-11-03",kind="STANDARD")
        val duplicate = normal.copy(id = 31)
        assertEquals(1, IosUnfilteredAgenda.select(listOf(normal,duplicate),"2026-11-01").size)
        assertEquals(2, IosUnfilteredAgenda.select(listOf(normal,duplicate),"2026-11-01",setOf(31)).size)
        assertEquals(2, IosUnfilteredAgenda.select(listOf(normal,duplicate.copy(ticketAvailable = true)),"2026-11-01").size)
    }

    @Test fun supportsLeapDaysAndRejectsInvalidCalendarDates() {
        assertTrue(IosUnfilteredAgenda.validDay("2028-02-29"))
        assertFalse(IosUnfilteredAgenda.validDay("2027-02-29"))
        assertFalse(IosUnfilteredAgenda.validDay("2026-13-01"))
    }
}
