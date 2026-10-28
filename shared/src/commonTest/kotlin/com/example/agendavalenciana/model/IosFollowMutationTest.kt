package com.example.agendavalenciana.model

import com.example.agendavalenciana.ios.IosFollow
import com.example.agendavalenciana.ios.IosFollowMutation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IosFollowMutationTest {
    @Test fun followDifferentTypesWithSameLabelKeepsBoth() {
        val artist = IosFollow("ARTIST", "La Sala", "La Sala")
        val items = IosFollowMutation.updated(listOf(artist), "VENUE", "La Sala", "La Sala", true)
        assertEquals(2, items.size)
        assertTrue(IosFollowMutation.contains(items, "VENUE", "La Sala"))
        assertTrue(IosFollowMutation.contains(items, "ARTIST", "La Sala"))
    }
    @Test fun unfollowOnlyRemovesMatchingType() {
        val items = listOf(IosFollow("ARTIST", "Alpha", "Alpha"), IosFollow("CATEGORY", "Alpha", "Alpha"))
        assertEquals(listOf(items.last()), IosFollowMutation.updated(items, "ARTIST", "Alpha", "Alpha", false))
    }
    @Test fun followIsIdempotent() {
        val once = IosFollowMutation.updated(emptyList(), "ORGANIZER", "Cooperativa", "Cooperativa", true)
        assertEquals(once, IosFollowMutation.updated(once, "ORGANIZER", "Cooperativa", "Cooperativa", true))
    }
    @Test fun blankAndUnsupportedNamesCannotMutate() {
        val items = emptyList<IosFollow>()
        assertFalse(IosFollowMutation.contains(items, "VENUE", ""))
        var threw = false
        try { IosFollowMutation.updated(items, "UNKNOWN", "x", "x", true) } catch (_: IllegalArgumentException) { threw = true }
        assertTrue(threw)
    }
}
