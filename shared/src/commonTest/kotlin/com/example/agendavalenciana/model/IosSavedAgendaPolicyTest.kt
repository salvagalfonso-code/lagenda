package com.example.agendavalenciana.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IosSavedAgendaPolicyTest {
    @Test fun decodedSlotIncludesArtistAndDate() {
        val raw = IosFestivalSavedSlotPolicy.key(104, "2026-11-02", "20:15", "La Maria", "Concert")
        val decoded = IosSavedAgendaPolicy.decodeSlotKey(raw)!!
        assertEquals(104, decoded.eventId)
        assertEquals("2026-11-02", decoded.day)
        assertEquals("la maria", decoded.artist)
        assertEquals(raw, decoded.originalKey)
    }
    @Test fun invalidDataIsIgnored() {
        assertNull(IosSavedAgendaPolicy.decodeSlotKey("festival-42"))
        assertNull(IosSavedAgendaPolicy.decodeSlotKey("1:1|1:2|1:3|1:4|99:a"))
    }
    @Test fun festivalUsesItsEndDate() {
        assertTrue(IosSavedAgendaPolicy.eventStillUpcoming("2026-10-25", "2026-11-01", "2026-10-28"))
        assertFalse(IosSavedAgendaPolicy.eventStillUpcoming("2026-10-25", "2026-10-27", "2026-10-28"))
    }
    @Test fun generalAggregatesListsAndSaved() {
        assertEquals(setOf(1,2,3), IosSavedAgendaPolicy.validEventIds(setOf(1,2), setOf(3)))
    }
    @Test fun expiredSlotsAreSkipped() {
        val old = IosFestivalSavedSlotPolicy.key(4, "2026-01-02", "20:00", "Grup", "Actuació")
        val fresh = IosFestivalSavedSlotPolicy.key(4, "2026-11-02", "20:00", "Grup", "Actuació")
        assertEquals(listOf(fresh), IosSavedAgendaPolicy.activeSlots(setOf(old,fresh), 4, "2026-10-28").map { it.originalKey })
    }
}
