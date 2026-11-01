package com.example.agendavalenciana.model

import com.example.agendavalenciana.ios.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IosCommercialVisibilityTest {
    private fun event(id: Int, internal: Boolean = false, url: String = "") =
        AgendaEventSummary(id = id, title = "Esdeveniment $id", date = "2026-11-01",
            ticketAvailable = internal, ticketUrl = url)

    @Test fun paidPlacementsAndOnlyInternalTicketSalesHaveAutomaticProtection() {
        assertTrue(IosCommercialVisibility.protected(event(7), setOf(7)))
        assertTrue(IosCommercialVisibility.protected(event(8, internal = true), emptySet()))
        assertFalse(IosCommercialVisibility.protected(event(9, internal = true,
            url = "https://taquilla-externa.example/"), emptySet()))
        assertFalse(IosCommercialVisibility.protected(event(10), emptySet()))
        assertEquals(listOf(7, 8), IosCommercialVisibility.guaranteed(
            listOf(event(7), event(8, true), event(9,true,"https://externa.example")), setOf(7)).map { it.id })
    }

    @Test fun creativeVisualMapIsAppliedOnlyToMatchingActiveAd() {
        val raw = Json.parseToJsonElement("""{ "visuals": [
          { "ad_id": 8, "card_focus_x": 1.5, "card_focus_y": -1, "image_url": "https://example.org/creative.jpg" },
          { "ad_id": 0, "card_focus_x": 0.4 }
        ] }""") as JsonObject
        val visuals = IosCommercialVisuals.decode(raw)
        val ads = listOf(IosAd("8",55,"HEADER","https://example.org/fallback.jpg"),
            IosAd("9",56,"FEED_NORMAL","https://example.org/normal.jpg"))
        val result = IosCommercialVisuals.apply(ads, visuals)
        assertEquals(1f, result[0].focusX)
        assertEquals(0f, result[0].focusY)
        assertEquals("https://example.org/creative.jpg", result[0].imageUrl)
        assertEquals(ads[1], result[1])
    }

    @Test fun visualMapDoesNotActivateCampaignsNotInServerInventory() {
        val visuals = mapOf(7 to IosAdVisual(0.9f,0.4f,"https://example.org/x.jpg"))
        assertTrue(IosCommercialVisuals.apply(emptyList(), visuals).isEmpty())
    }
}
