package com.example.agendavalenciana.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IosSavedListInvitationTest {
    @Test fun acceptsCodesAndOfficialInvitationLinks() {
        assertEquals("ABCDEF1234", IosSavedListInvitation.parse(" ABCDEF1234 "))
        val token = "a".repeat(48)
        assertEquals(token, IosSavedListInvitation.parse("https://agendaval.com/app/lists.php?invite=$token"))
        assertEquals(token, IosSavedListInvitation.parse("https://agendaval.com/app/lists.php?invite=$token&utm_source=ios"))
    }
    @Test fun rejectsArbitraryUrlsAndInvalidCodes() {
        assertNull(IosSavedListInvitation.parse("https://example.com/?invite=${"a".repeat(48)}"))
        assertNull(IosSavedListInvitation.parse("123"))
        assertNull(IosSavedListInvitation.parse("DROP TABLE"))
    }
}
