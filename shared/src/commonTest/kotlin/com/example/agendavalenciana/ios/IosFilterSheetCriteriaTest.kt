package com.example.agendavalenciana.ios

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals

class IosFilterSheetCriteriaTest {
    private val concert = AgendaEventSummary(2001, "Concert", "2026-10-30", "21:00", "Sagunt", "Auditori", "Música", "", "",
        endDate = "2026-11-02", price = "12,50 €")
    private val play = AgendaEventSummary(2002, "Teatre", "2026-11-01", "19:00", "Sagunt", "Teatre", "Teatre", "", "",
        price = "Gratuït")

    @Test fun calendarShortcutsRespectLeapDaysAndWeekend() {
        assertEquals("2026-10-31", IosFilterDates.weekendSaturday("2026-10-29"))
        assertEquals("2026-11-01", IosFilterDates.addDays("2026-10-31", 1))
        assertEquals("2024-02-29", IosFilterDates.addDays("2024-02-28", 1))
        assertEquals("2026-02-01" to "2026-02-28", IosFilterDates.monthRange("2026-02"))
        assertFalse(IosFilterDates.valid("2026-02-29"))
        assertEquals("2026-10-29", IosFilterDates.fromUtcMillis(IosFilterDates.toUtcMillis("2026-10-29")))
    }
    @Test fun monthSpanningFestivalRemainsDiscoverable() {
        assertTrue(IosAdvancedSearch.matches(concert, "", null, IosSearchFilters(month = "2026-11")))
        assertFalse(IosAdvancedSearch.matches(concert, "", null, IosSearchFilters(month = "2026-12")))
    }
    @Test fun multiCategoryAndPriceSelectionsActuallyFilter() {
        assertTrue(IosAdvancedSearch.matches(concert, "", null, IosSearchFilters(categories = setOf("Teatre", "Música"))))
        assertFalse(IosAdvancedSearch.matches(concert, "", null, IosSearchFilters(categories = setOf("Teatre"))))
        assertTrue(IosAdvancedSearch.matches(play, "", null, IosSearchFilters(freeOnly = true)))
        assertFalse(IosAdvancedSearch.matches(concert, "", null, IosSearchFilters(freeOnly = true)))
        assertTrue(IosAdvancedSearch.matches(concert, "", null, IosSearchFilters(maxPrice = 15)))
        assertFalse(IosAdvancedSearch.matches(concert, "", null, IosSearchFilters(maxPrice = 10)))
    }
    @Test fun radiusNeedsRealOriginAndInvalidRangesNeverBroadenResults() {
        assertFalse(IosSearchFilters(radiusKm = 10).valid())
        assertTrue(IosSearchFilters(town = "Sagunt", radiusKm = 10).valid())
        assertTrue(IosSearchFilters(radiusKm = 25, referenceTowns = setOf("Sagunt")).valid())
        assertTrue(IosAdvancedSearch.matches(concert, "", null,
            IosSearchFilters(radiusKm = 25, referenceTowns = setOf("Sagunt"))))
        assertFalse(IosSearchFilters(fromDate = "2026-11-05", toDate = "2026-11-01").valid())
    }
}
