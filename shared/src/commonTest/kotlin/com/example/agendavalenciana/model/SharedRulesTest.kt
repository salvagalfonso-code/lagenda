package com.example.agendavalenciana.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SharedRulesTest {
    @Test fun validRadius() {
        assertEquals(30, PersonalizationOptions.validRadius(42))
        assertEquals(50, PersonalizationOptions.validRadius(50))
    }
    @Test fun feedDoesNotForceFreeReservation() {
        assertTrue(AgendaReservationPolicy.isFreeInternalAgendaReservation(true, 0, ""))
    }
    @Test fun snapshotPatch() {
        val ids = ForYouSnapshotReusePolicy.patchFollowedChanges(
            listOf(1, 2), mapOf(1 to 2, 2 to 1, 3 to 1), mapOf(1 to 1, 2 to 1),
            setOf(1), setOf(1, 3))
        assertTrue(ids != null && 3 in ids)
    }
}
