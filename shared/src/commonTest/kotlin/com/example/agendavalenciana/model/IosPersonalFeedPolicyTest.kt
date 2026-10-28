package com.example.agendavalenciana.model

import com.example.agendavalenciana.ios.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IosPersonalFeedPolicyTest {
    private fun event(id: Int, town: String, artist: String = "", style: String = "", lang: String = "") =
        IosPersonalEvent(id,"${id}_2026-12-10","2026-12-10","20:00",
            if (artist.isBlank()) emptyList() else listOf(artist),emptyList(),emptyList(),
            "Centre","Música",if (style.isBlank()) emptyList() else listOf(style),town,lang)

    @Test fun followedArtistIsGlobalAndRelatedStyleUsesChosenRadius() {
        val events=listOf(event(1,"Barcelona","La Fúmiga","ska"),
            event(2,"València","Altre","ska"),event(3,"Barcelona","Altre","ska"))
        val selected=IosPersonalFeedPolicy.select(events,listOf(IosFollow("ARTIST","la fumiga","La Fúmiga")),
            IosPersonalSettings(towns=setOf("Sagunt"),townRadii=mapOf("Sagunt" to 50)),emptySet())
        assertEquals(setOf("1_2026-12-10","2_2026-12-10"),selected.toSet())
    }
    @Test fun categoriesAndGeographyUseHundredKmAndComarques() {
        val events=listOf(event(1,"València",style="jazz"),event(2,"Barcelona",style="jazz"),event(3,"Xàtiva",style="jazz"))
        val selected=IosPersonalFeedPolicy.select(events,emptyList(),
            IosPersonalSettings(subcategories=setOf("jazz"),towns=setOf("Sagunt")),emptySet())
        assertTrue("1_2026-12-10" in selected)
        assertFalse("2_2026-12-10" in selected)
        assertTrue(IosGeoPlaces.within("Xàtiva",emptySet(),setOf("Costera"),emptyMap()))
    }
    @Test fun paidEventsAreGuaranteedEvenWithoutPersonalSignals() {
        assertEquals(listOf("7_2026-12-10"),IosPersonalFeedPolicy.select(
            listOf(event(7,"Barcelona")),emptyList(),IosPersonalSettings(),setOf(7)))
    }
    @Test fun localeDoesNotInventUnresolvedMunicipalities() {
        assertFalse(IosGeoPlaces.within("poble desconegut",setOf("Sagunt"),emptySet(),emptyMap()))
    }
    @Test fun explicitValencianPreferenceReordersOnlyWithinDate() {
        val events=listOf(event(1,"València",style="ska",lang="anglés"),
            event(2,"València",style="ska",lang="valencià"))
        assertEquals(listOf("2_2026-12-10","1_2026-12-10"),IosPersonalFeedPolicy.select(events,emptyList(),
            IosPersonalSettings(subcategories=setOf("ska"),valencianFirst=true),emptySet()))
    }
    @Test fun radiiOnlyAllowSupportedValues() {
        assertEquals(mapOf("Sagunt" to 30),IosPersonalFeedPolicy.decodeRadii(setOf("Sagunt@10")))
    }
}
