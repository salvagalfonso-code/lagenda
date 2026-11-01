package com.example.agendavalenciana.ios

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IosEventDetailContractTest {
    @Test fun ordinaryEventsHaveExactlyFourTabs() {
        assertEquals(listOf("Info", "Entrades", "Ubicació", "Comunitat"),
            IosEventDetailContract.tabs(false))
    }
    @Test fun festivalsKeepScheduleAndExposeTicketTab() {
        assertEquals(listOf("Horaris", "Info", "Entrades", "Ubicació", "Comunitat"),
            IosEventDetailContract.tabs(true))
    }
    @Test fun ticketsRejectUnencryptedUrl() {
        assertTrue(IosEventDetailContract.isSafeExternalTicketUrl("https://example.org/checkout"))
        assertFalse(IosEventDetailContract.isSafeExternalTicketUrl("http://example.org/checkout"))
        assertFalse(IosEventDetailContract.isSafeExternalTicketUrl("javascript:alert(1)"))
    }
    @Test fun mapsDoNotInventCoordinates() {
        assertEquals("Sala", IosEventDetailContract.placeQuery("Sala", "Sala"))
        assertEquals("Sala, Poble", IosEventDetailContract.placeQuery(" Sala ", "Poble"))
        assertEquals("", IosEventDetailContract.placeQuery("", ""))
    }
    @Test fun communityTypesMatchBackendCodes() {
        assertEquals("Cotxe compartit", IosEventDetailContract.communityTypeLabel("CAR"))
        assertEquals("Transport organitzat", IosEventDetailContract.communityTypeLabel("TRANSPORT"))
        assertEquals("Quedades", IosEventDetailContract.communityTypeLabel("MEETUP"))
        assertEquals("Allotjament compartit", IosEventDetailContract.communityTypeLabel("HOSTING"))
        assertEquals("Altres", IosEventDetailContract.communityTypeLabel("OTHER"))
    }
}
