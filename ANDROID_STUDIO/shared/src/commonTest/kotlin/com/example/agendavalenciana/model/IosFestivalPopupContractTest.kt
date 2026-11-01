package com.example.agendavalenciana.model

import com.example.agendavalenciana.ios.IosFestivalScheduleDecoder
import com.example.agendavalenciana.ios.AgendaEventSummary
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IosFestivalPopupContractTest {
    @Test fun aSingleDayFestivalCanBeFlaggedWithoutInventingExtraDates() {
        val festival = AgendaEventSummary(6, "Mostra", "2027-05-02", "18:00", "", "", "Música", "", "", eventStructure = "FESTIVAL")
        assertTrue(festival.isFestival)
        val ordinary = festival.copy(eventStructure = "AUTO")
        assertFalse(ordinary.isFestival)
    }

    @Test fun publicSpecialScheduleUsesAndroidFieldsAndFiltersDisabledRows() {
        val json = """{
          "ok": true, "schedule": [
            {"date":"2027-06-12","start_time":"19:30:00","artist_name":"La Banda","title":"Actuació","stage":"Escenari A","active":1},
            {"schedule_date":"2027-06-13","start_at":"2027-06-13 18:00:00","group_name":"Teatre","title":"Obra","location":"Sala","active":1},
            {"date":"2027-06-13","start_time":"20:00","artist_name":"No actua","active":0}
          ]
        }""".trimIndent()
        val rows = IosFestivalScheduleDecoder.decode(Json.parseToJsonElement(json) as JsonObject)
        assertEquals(2, rows.size)
        assertEquals("19:30", rows.first().time)
        assertEquals("La Banda", rows.first().artist)
        assertEquals("Escenari A", rows.first().location)
        assertEquals("Teatre", rows[1].artist)
    }
}
