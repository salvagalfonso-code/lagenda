package com.example.agendavalenciana.ios

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Pas 1: tests de contracte de connexió, sense enviar peticions de xarxa. */
class IosServerConnectionContractTest {
    @Test fun sameApiBaseAsAndroidV336() {
        assertEquals("https://quimerafest.com/app/api/", IosServerConnection.API_BASE)
        assertEquals("https://quimerafest.com/app/api/auth/me.php", IosServerConnection.endpoint("auth/me.php"))
        assertEquals("https://quimerafest.com/app/api/events/index.php?limit=5",
            IosServerConnection.endpoint("events/index.php?limit=5"))
    }

    @Test fun cannotRequestAnUntrustedHostOrRelativeTraversal() {
        for (path in listOf("", "/auth/me.php", "https://another.example", "../config.php", "auth/me.php#secret")) {
            assertFailsWith<IllegalArgumentException> { IosServerConnection.endpoint(path) }
        }
    }
}
