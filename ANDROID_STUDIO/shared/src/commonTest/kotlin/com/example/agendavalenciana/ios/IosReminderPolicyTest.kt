package com.example.agendavalenciana.ios

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IosReminderPolicyTest {
    @Test fun datesAndFuture() {
        assertTrue(IosReminderPolicy.validLocalDateTime("2026-11-01T19:45"))
        assertFalse(IosReminderPolicy.validLocalDateTime("2026-11-01T25:61"))
        assertFalse(IosReminderPolicy.validLocalDateTime("2026/11/01 19:45"))
        IosReminderPolicy.requireFuture(8, true, 5000, 4000)
        assertFailsWith<IllegalArgumentException> { IosReminderPolicy.requireFuture(8, true, 4000, 4000) }
        assertFailsWith<IllegalArgumentException> { IosReminderPolicy.requireFuture(0, false, 0, 4000) }
        IosReminderPolicy.requireFuture(8, false, 0, 4000)
    }
    @Test fun idsAndUnconfirmedState() {
        assertEquals(setOf("2", "8"), IosReminderPolicy.updatedIds(setOf("2"), 8, true))
        assertEquals(setOf("2"), IosReminderPolicy.updatedIds(setOf("2", "8"), 8, false))
        assertNull(IosReminderPolicy.updatedIds(null, 8, true))
        assertEquals(setOf("8"), IosReminderPolicy.updatedIds(setOf("-1", "hola"), 8, true))
    }
    @Test fun pendingOverridesCanonicalAlerts() {
        assertEquals(setOf(5, 10), IosReminderPolicy.projectedAlerts(setOf(2, 5),
            listOf(2 to false, 10 to true)))
        assertEquals(setOf(2), IosReminderPolicy.projectedAlerts(emptySet(),
            listOf(2 to true, 2 to false, 2 to true)))
    }
}
