package com.example.agendavalenciana.ios

import com.example.agendavalenciana.model.IosFestivalSavedSlotPolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IosFestivalPopupPolicyTest {
    private val a = AgendaScheduleEntry("2026-11-05", "19:00", "Actuació A", "A", "Escenari 1",
        groupName = "Vesprada", sortOrder = 2)
    private val b = AgendaScheduleEntry("2026-11-05", "18:00", "Actuació B", "B", "Escenari 2",
        groupName = "Vesprada", sortOrder = 1)
    private val marker = AgendaScheduleEntry("2026-11-05", "17:00", "Vesprada", "", "Escenari 1",
        groupName = "Vesprada", itemType = "activity", sortOrder = 0)
    private val old = AgendaScheduleEntry("2026-11-01", "20:00", "Antic", "", "Escenari 1")

    @Test fun daysAreRealAndFutureOnly() {
        assertEquals(listOf("2026-11-05"), IosFestivalPopupPolicy.upcomingDays(listOf(old,a,b), "2026-11-05"))
        assertEquals(emptyList(), IosFestivalPopupPolicy.upcomingDays(listOf(old), "2026-11-05"))
        assertEquals("2026-11-05", IosFestivalPopupPolicy.initialDay(listOf("2026-11-05"), b, "2026-11-01"))
    }
    @Test fun programmeRespectsOrderAndGroupMarkers() {
        assertEquals(listOf("B", "A"), IosFestivalPopupPolicy.performances(listOf(old,a,marker,b),
            "2026-11-05",false, emptySet(), 17).map { it.artist })
        assertTrue(IosFestivalPopupPolicy.isGroupMarker(marker))
    }
    @Test fun personalFilterSupportsOldAndNewKeysAndStages() {
        val oldKey = IosFestivalSavedSlotPolicy.key(17,a.date,a.time,a.artist,a.title)
        val currentKey = IosFestivalSavedSlotPolicy.key(17,b.date,b.time,b.artist,b.title,b.location)
        assertEquals(2, IosFestivalPopupPolicy.performances(listOf(a,b),"2026-11-05",true,
            setOf(oldKey,currentKey),17).size)
        assertFalse(IosFestivalPopupPolicy.saved(17,a,setOf(currentKey)))
    }
    @Test fun partialRemoteSchedulePreservesRealCatalogueSlots() {
        val remote = a.copy(details = "Publicada al servidor")
        val merged = IosFestivalPopupPolicy.mergePublished(listOf(remote), listOf(a,b))
        assertEquals(2, merged.size)
        assertEquals("Publicada al servidor", merged[0].details)
        assertEquals("B", merged[1].artist)
    }
    @Test fun stagePlacesUsePublishedVenueAndStage() {
        assertEquals(listOf("Recinte", "Escenari 1", "Escenari 2"),
            IosFestivalPopupPolicy.places("Recinte","Vila-real",listOf(a,b)).map { it.first })
        assertTrue(IosFestivalPopupPolicy.places("", "", emptyList()).isEmpty())
    }
    @Test fun ticketsIncludeFestivalPassAndDay() {
        val all = IosTicketOption(1,"Abonament",20.0,null,4,"", "")
        val one = IosTicketOption(2,"Dia 5",10.0,null,4,"2026-11-05", "19:00")
        val two = IosTicketOption(3,"Dia 6",10.0,null,4,"2026-11-06", "19:00")
        assertEquals(listOf(1,2),IosFestivalPopupPolicy.ticketsForDay(listOf(all,one,two),"2026-11-05").map { it.id })
        assertEquals(3,IosFestivalPopupPolicy.ticketsForDay(listOf(all,one,two),"").size)
    }
}
