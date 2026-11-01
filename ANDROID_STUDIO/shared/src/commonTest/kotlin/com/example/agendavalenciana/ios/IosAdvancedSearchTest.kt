package com.example.agendavalenciana.ios

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class IosAdvancedSearchTest {
    private val festival = AgendaEventSummary(1, "Festival Terra", "2026-11-01", "20:00",
        "Sagunt", "Plaça", "Música", "", "", endDate = "2026-11-03",
        schedule = listOf(AgendaScheduleEntry("2026-11-02", "22:00", "Concert", "La Maria", "Escenari")))
    private val concert = AgendaEventSummary(2, "Prova", "2026-11-02", "20:00", "València", "Sala ABC",
        "Música", "", "", artists = listOf("Grup Àngel"), organizers = listOf("Afers Culturals"), styles = listOf("Folk;Pop"))
    private val other = AgendaEventSummary(3, "Teatre", "2026-12-01", "20:00", "Barcelona", "Teatre", "Teatre", "", "")

    @Test fun suggestionsHaveRealTypesAndAccents() {
        val pool = IosAdvancedSearch.pool(listOf(festival, concert, other),
            listOf("Sagunt", "València", "Barcelona"), listOf("Camp de Morvedre"))
        assertTrue(IosAdvancedSearch.suggest(pool, "la mar").any { it.type == IosSearchType.ARTIST })
        assertTrue(IosAdvancedSearch.suggest(pool, "valencia").any { it.type == IosSearchType.TOWN && it.value == "València" })
        assertTrue(IosAdvancedSearch.suggest(pool, "camp de").any { it.type == IosSearchType.COMARCA })
        assertTrue(IosAdvancedSearch.suggest(pool, "afers").any { it.type == IosSearchType.ORGANIZER })
    }
    @Test fun festivalsMatchScheduleArtistNotUnrelatedEvents() {
        val artist = IosSearchSuggestion(IosSearchType.ARTIST,"La Maria")
        assertTrue(IosAdvancedSearch.matches(festival,"La Maria", artist, IosSearchFilters()))
        assertFalse(IosAdvancedSearch.matches(other,"La Maria", artist, IosSearchFilters()))
    }
    @Test fun advancedFiltersIntersectInsteadOfBroaden() {
        assertTrue(IosAdvancedSearch.matches(concert,"",null,IosSearchFilters(category="Música",style="Folk")))
        assertFalse(IosAdvancedSearch.matches(other,"",null,IosSearchFilters(category="Música")))
        assertTrue(IosAdvancedSearch.matches(festival,"",null,IosSearchFilters(fromDate="2026-11-02",toDate="2026-11-02")))
        assertFalse(IosAdvancedSearch.matches(festival,"",null,IosSearchFilters(fromDate="2026-11-04")))
        assertTrue(IosAdvancedSearch.matches(festival,"",null,IosSearchFilters(comarca="Camp de Morvedre")))
        assertFalse(IosAdvancedSearch.matches(festival,"",null,IosSearchFilters(town="Barcelona",radiusKm=5)))
    }
    @Test fun invalidFiltersNeverExpandSearch() {
        assertFalse(IosSearchFilters(radiusKm=30).valid())
        assertFalse(IosSearchFilters(fromDate="2026-11-04",toDate="2026-11-01").valid())
    }
}
