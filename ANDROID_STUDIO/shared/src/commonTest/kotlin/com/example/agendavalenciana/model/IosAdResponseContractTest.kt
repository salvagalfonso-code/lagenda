package com.example.agendavalenciana.model

import com.example.agendavalenciana.ios.decodePromoted
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IosAdResponseContractTest {
    @Test fun incorporatesAllCommercialPriorityListsAndDedicatedPlacements() {
        val root = Json.parseToJsonElement("""{
            "priority_event_ids":[10],"platform_ticket_event_ids":[11],"paid_ticket_event_ids":[12],
            "ads":[
                {"id":1,"event_id":13,"placement":"HEADER","image_url":"images/ads/a.jpg","card_focus_x":0.3},
                {"id":2,"event_id":14,"placement":"FEED_PREMIUM"},
                {"id":3,"event_id":15,"placement":"FEED_NORMAL"},
                {"id":4,"event_id":16,"placement":"POPUP"}
            ],"priority_events":[],"events":[]
        }""") as JsonObject
        val result = decodePromoted(root)
        assertTrue(result.guaranteedEventIds.containsAll(setOf(10,11,12,13,14,15,16)))
        assertEquals(listOf("HEADER","FEED_PREMIUM","FEED_NORMAL","POPUP"), result.ads.map { it.placement })
        assertEquals(0.3f, result.ads.first().focusX)
    }
}
