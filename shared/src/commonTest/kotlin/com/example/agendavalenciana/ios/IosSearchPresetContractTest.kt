package com.example.agendavalenciana.ios

import kotlin.test.*
import kotlinx.serialization.json.*

class IosSearchPresetContractTest {
    private val full = IosSearchFilters(
        town = "Sagunt", comarca = "Camp de Morvedre", radiusKm = 15,
        categoryCodes = setOf("MUSIC", "THEATRE"),
        fromDate = "2026-11-01", toDate = "2026-11-30", month = "2026-11",
        maxPrice = 20, freeOnly = false
    )

    @Test fun fullAndroidPayloadRoundTrip() {
        val json = IosSearchPresetContract.toServer(full)
        assertEquals(setOf("MUSIC", "THEATRE"),
            (json["categories"] as JsonArray).map { (it as JsonPrimitive).content }.toSet())
        assertEquals("2026-11-01", (json["dateFrom"] as JsonPrimitive).content)
        assertEquals(15, (json["maxDistanceKm"] as JsonPrimitive).int)
        assertTrue(IosSearchPresetContract.same(full, IosSearchPresetContract.fromServer(json)))
        assertFalse("style" in json)
        assertFalse("radiusKm" in json)
    }

    @Test fun nameOnlyAndStyleAreNeverSilentlySaved() {
        assertFailsWith<IllegalArgumentException> { IosSearchPresetContract.toServer(IosSearchFilters()) }
        assertFailsWith<IllegalArgumentException> {
            IosSearchPresetContract.toServer(full.copy(style="Jazz"))
        }
    }

    @Test fun trueDatesNotJustDatePattern() {
        assertTrue(IosSearchPresetContract.isIsoDate("2028-02-29"))
        assertFalse(IosSearchPresetContract.isIsoDate("2026-02-29"))
        assertFalse(IosSearchPresetContract.isIsoDate("2026-13-01"))
        assertTrue(IosSearchPresetContract.isIsoMonth("2026-12"))
        assertFalse(IosSearchPresetContract.isIsoMonth("2026-00"))
    }

    @Test fun priceFilterIsStrictAndDoesNotFloorCents() {
        assertEquals(10.95, IosSearchPresetContract.priceEuros("10,95 €"))
        assertEquals(0.0, IosSearchPresetContract.priceEuros("Gratuït"))
        assertNull(IosSearchPresetContract.priceEuros(""))
        val costly = AgendaEventSummary(1, "Teatre", "2026-11-15", "20:00", "Sagunt", "Sala", "Teatre", "", "", price="10,95 €")
        assertFalse(IosAdvancedSearch.matches(costly, "", null, IosSearchFilters(maxPrice=10)))
        assertTrue(IosAdvancedSearch.matches(costly, "", null, IosSearchFilters(maxPrice=15)))
    }

    @Test fun multipleCategoriesAndMonthIntersect() {
        val theatre = AgendaEventSummary(2, "Teatre", "2026-11-15", "20:00", "Sagunt", "Sala", "Teatre", "", "")
        val cinema = theatre.copy(category="Cinema")
        val wanted = IosSearchFilters(categoryCodes=setOf("THEATRE","MUSIC"), month="2026-11")
        assertTrue(IosAdvancedSearch.matches(theatre,"",null,wanted))
        assertFalse(IosAdvancedSearch.matches(cinema,"",null,wanted))
        assertFalse(IosAdvancedSearch.matches(theatre,"",null,wanted.copy(month="2026-12")))
    }

    @Test fun savedAndroidDistanceCanUsePreferredTowns() {
        val fromAndroid = IosSearchPresetContract.fromServer(buildJsonObject {
            put("categories", JsonArray(emptyList()))
            put("maxDistanceKm", 15); put("freeOnly", false)
        })
        assertTrue(fromAndroid.valid())
        assertEquals(15,fromAndroid.radiusKm)
        val town = AgendaEventSummary(3,"Concert","2026-11-15","20:00","Sagunt","Plaça","Música","", "")
        assertFalse(IosAdvancedSearch.matches(town,"",null,fromAndroid))
        assertTrue(IosAdvancedSearch.matches(town,"",null,fromAndroid.copy(preferredTowns=setOf("Sagunt"))))
    }
}
