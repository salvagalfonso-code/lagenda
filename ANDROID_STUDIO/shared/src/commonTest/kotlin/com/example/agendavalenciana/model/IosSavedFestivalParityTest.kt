package com.example.agendavalenciana.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IosSavedFestivalParityTest {
    @Test fun stageIsPartOfIdentityAndOlderDataRemainsReadable() {
        val old = IosFestivalSavedSlotPolicy.key(12, "2027-02-01", "21:00", "X", "Concert")
        val stageA = IosFestivalSavedSlotPolicy.key(12, "2027-02-01", "21:00", "X", "Concert", "A")
        val stageB = IosFestivalSavedSlotPolicy.key(12, "2027-02-01", "21:00", "X", "Concert", "B")
        assertNotEquals(stageA, stageB)
        assertEquals("", IosSavedAgendaPolicy.decodeSlotKey(old)?.location)
        assertEquals("a", IosSavedAgendaPolicy.decodeSlotKey(stageA)?.location)
        assertEquals("2027-02-01|21:00|A|X", IosFestivalSavedSlotPolicy.androidKey(
            "2027-02-01", "21:00", "A", "X", "Concert"))
    }

    @Test fun savedDatesMatchAndroidCulturalVisibilityPolicy() {
        assertTrue(IosSavedAgendaPolicy.eventStillUpcoming("2027-02-01", "2027-02-02",
            "2027-02-05", listOf("2027-02-06")))
        assertFalse(IosSavedAgendaPolicy.eventStillUpcoming("2027-02-01", "2027-02-02",
            "2027-02-05", listOf("2027-02-04")))
        assertTrue(IosSavedAgendaPolicy.eventStillUpcoming("", "", "2027-02-05"))
        assertFalse(IosSavedAgendaPolicy.validDate("2027-02-30"))
        assertNull(IosSavedAgendaPolicy.decodeSlotKey("badkey"))
    }

    @Test fun savedPerformanceResolvesOnlyToRealProgramme() {
        val first = IosFestivalSavedSlotPolicy.key(55, "2027-07-01", "22:00", "Grup", "Concert", "Plaça")
        val second = IosFestivalSavedSlotPolicy.key(55, "2027-07-01", "22:00", "Grup", "Concert", "Parc")
        val plan = listOf(
            IosFestivalSession("2027-07-01", "22:00", "Grup", "Concert", "Plaça"),
            IosFestivalSession("2027-07-01", "22:00", "Grup", "Concert", "Parc")
        )
        assertEquals(2, IosSavedAgendaPolicy.matchSlots(setOf(first, second), 55, plan, "2027-07-01").size)
        assertEquals(1, IosSavedAgendaPolicy.matchSlots(setOf(first), 55, plan, "2027-07-01").size)
        assertTrue(IosSavedAgendaPolicy.matchSlots(setOf(first), 55, emptyList(), "2027-07-01").isEmpty())
        assertTrue(IosSavedAgendaPolicy.matchSlots(setOf(first), 55, plan, "2027-07-02").isEmpty())
        assertEquals(setOf(second), IosSavedAgendaPolicy.withoutSession(setOf(first,second),
            IosSavedAgendaPolicy.decodeSlotKey(first)!!))
    }
}
