package com.example.agendavalenciana.ios

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Pas 23: suggeriments territorials; no altera l'estat de cap usuari. */
class IosTerritorySuggestionsTest {
    @Test fun municipisRealsComarcaIAccents() {
        val entries = IosGeoPlaces.municipalityEntries()
        assertTrue(entries.size > 1000)
        val pool = IosAdvancedSearch.pool(emptyList(), IosGeoPlaces.allTowns(), IosGeoPlaces.allComarques(), entries)
        assertTrue(IosAdvancedSearch.suggest(pool, "valencia").any {
            it.type == IosSearchType.TOWN && it.value == "València"
        })
        val sagunt = IosAdvancedSearch.suggest(pool, "Sagunt").first { it.type == IosSearchType.TOWN }
        assertTrue(sagunt.subtitle.contains("Camp de Morvedre"))
        assertTrue(IosAdvancedSearch.suggest(pool, "morvedre").any { it.type == IosSearchType.COMARCA })
        assertTrue(IosAdvancedSearch.suggest(pool, "ademuz").any { it.type == IosSearchType.TOWN && it.value == "Ademús" })
        assertTrue(IosAdvancedSearch.suggest(pool, "fakevalue").isEmpty())
    }

    @Test fun homonimsIPreferenciaNomsOficials() {
        val entries = listOf(
            IosGeoPlaces.TownEntry("Alfons", "Alfonso", "Baix Camp", "CAT", listOf("Alfons vell")),
            IosGeoPlaces.TownEntry("Alfons", "Alfonso", "Camp de Morvedre", "PV", emptyList())
        )
        val pool = IosAdvancedSearch.pool(emptyList(), emptyList(), emptyList(), entries)
        assertEquals(2, IosAdvancedSearch.suggest(pool, "alfons").size)
        assertEquals("Baix Camp", IosAdvancedSearch.suggest(pool, "Alfons vell").single().comarca)
        assertTrue(IosAdvancedSearch.suggest(pool, "alfonso").all { it.type == IosSearchType.TOWN })
    }

    @Test fun laComarcaDelResultatSeleccionatEsRespecta() {
        val selected = IosSearchSuggestion(IosSearchType.TOWN, "Sagunt", "Camp de Morvedre", comarca="Camp de Morvedre")
        val event = AgendaEventSummary(1,"Mostra", "2026-11-08", "20:00", "Sagunt", "Sala", "Teatre", "", "", comarca="Camp de Morvedre")
        assertTrue(IosAdvancedSearch.matches(event,"Sagunt",selected,IosSearchFilters()))
        assertFalse(IosAdvancedSearch.matches(event.copy(comarca="Baix Camp"),"Sagunt",selected,IosSearchFilters()))
        assertTrue(IosAdvancedSearch.matches(event,"",null,IosSearchFilters(comarca="Camp de Morvedre")))
        assertFalse(IosAdvancedSearch.matches(event,"",null,IosSearchFilters(comarca="Baix Camp")))
    }

    @Test fun suggerimentsLimitatsNoCanvienEstat() {
        val pool = IosAdvancedSearch.pool(emptyList(), listOf("Sagunt", "Sallent", "Sant Celoni"), listOf("Camp de Morvedre"))
        assertEquals(emptyList(), IosAdvancedSearch.suggest(pool,""))
        assertEquals(2, IosAdvancedSearch.suggest(pool,"s",2).size)
        assertTrue(IosAdvancedSearch.suggest(pool,"s",1).size <= 1)
    }
}
