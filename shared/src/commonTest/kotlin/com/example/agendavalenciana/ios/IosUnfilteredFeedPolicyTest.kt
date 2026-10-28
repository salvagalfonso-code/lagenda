package com.example.agendavalenciana.ios

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IosUnfilteredFeedPolicyTest {
    @Test fun progressiveBatches() {
        val p = IosUnfilteredFeedPolicy
        assertEquals(5,p.nextCount(0,47))
        assertEquals(20,p.nextCount(5,47))
        assertEquals(30,p.nextCount(20,47))
        assertEquals(40,p.nextCount(30,47))
        assertEquals(47,p.nextCount(40,47))
        assertEquals(3,p.nextCount(0,3))
        assertEquals(0,p.nextCount(0,0))
        assertEquals(5,p.nextCount(5,5))
    }
    @Test fun feedDatesAndOngoingFestival() {
        val p = IosUnfilteredFeedPolicy
        assertEquals("23 oct",p.cardDate("2026-10-23"))
        assertEquals("01 gen",p.cardDate("2027-01-01"))
        assertEquals("2026-42-02",p.cardDate("2026-42-02"))
        assertEquals("2026-10-29",p.relevantDate("2026-10-27","2026-10-31","2026-10-28",listOf("2026-10-27","2026-10-29")))
        assertEquals("2026-10-28",p.relevantDate("2026-10-27","2026-10-31","2026-10-28",emptyList()))
        assertFalse(p.ongoingWithoutSchedule("2026-10-27","2026-10-31","2026-10-28",listOf("2026-10-29")))
        assertTrue(p.ongoingWithoutSchedule("2026-10-27","2026-10-31","2026-10-28",emptyList()))
    }
    @Test fun festivalLineOnlyRelevantDay() {
        val p = IosUnfilteredFeedPolicy
        assertEquals("A · B · +1",p.festivalSummary("2026-10-29",listOf("2026-10-28" to "Old", "2026-10-29" to "A", "2026-10-29" to "B", "2026-10-29" to "C", "2026-10-29" to "a"),"ALT"))
        assertEquals("Cartell disponible",p.festivalSummary("2026-10-30",emptyList(),"Cartell disponible"))
    }
    @Test fun ticketStates() {
        val p = IosUnfilteredFeedPolicy
        assertEquals("Sense venda",p.ticketLabel(false,""))
        assertEquals("Entrades",p.ticketLabel(true,""))
        assertEquals("Gratuït",p.ticketLabel(true,"0"))
        assertEquals("12 €",p.ticketLabel(true,"12 €"))
    }
}
