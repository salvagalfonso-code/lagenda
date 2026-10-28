package com.example.agendavalenciana.model

import com.example.agendavalenciana.ios.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IosStyleMetadataParityTest {
    private fun e(id: Int, artist: String, style: String, town: String = "València") =
        IosPersonalEvent(id, "${id}_2026-12-10", "2026-12-10", "20:00",
            listOf(artist), emptyList(), emptyList(), "Sala", "Música", listOf(style), town, "")

    @Test fun androidStyleAliasesAreShared() {
        assertEquals(IosStyleAffinityCatalog.canonical("alternative rock"),
            IosStyleAffinityCatalog.canonical("rock alternatiu"))
        assertEquals(0, IosStyleAffinityCatalog.rank("alternative rock", "rock alternatiu"))
    }
    @Test fun decodeOfficialEntityMetadata() {
        val raw = """{"entities":[{"type":"ARTIST","name":"La Maria","style_raw":"jazz|folk"},
            {"type":"VENUE","name":"Sala","style_raw":"metal"}]}"""
        val rows = IosEntityAffinityCatalog.decodeResponse(Json.parseToJsonElement(raw).jsonObject)
        assertEquals(setOf("jazz","folk"), rows["la maria"])
        assertFalse(rows.containsKey("sala"))
    }
    @Test fun followedArtistMetadataOutranksUnrelatedStyle() {
        val items = listOf(e(1,"La Maria","folk","Barcelona"),
            e(2,"Altre","jazz"),e(3,"Altre","folk"))
        val settings = IosPersonalSettings(towns = setOf("Sagunt"), subcategories = setOf("jazz"))
        val selected = IosPersonalFeedPolicy.select(items,
            listOf(IosFollow("ARTIST","la maria","La Maria")),settings,emptySet(),
            artistMetadata = mapOf("la maria" to setOf("jazz")))
        assertTrue("1_2026-12-10" in selected)
        assertTrue("2_2026-12-10" in selected)
        assertFalse("3_2026-12-10" in selected)
    }
    @Test fun commercialCardNeverDisappearsWithoutLikes() {
        val selected = IosPersonalFeedPolicy.select(listOf(e(4,"Desconegut","metal","Barcelona")),
            emptyList(),IosPersonalSettings(),setOf(4))
        assertEquals(listOf("4_2026-12-10"),selected)
    }
}
