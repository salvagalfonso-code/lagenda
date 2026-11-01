package com.example.agendavalenciana.ios

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IosFollowCompatibilityTest {
    @Test fun typedKeysPreventAccidentalUnfollow() {
        val artist = IosFollow("ARTIST", "la sal", "La Sal")
        val organization = IosFollow("ORGANIZER", "sal_org", "La Sal")
        val result = IosFollowMutation.updated(listOf(artist, organization), "ARTIST", "La Sal", "La Sal", false)
        assertEquals(listOf(organization), result)
        assertFalse(IosFollowMutation.contains(result, "ARTIST", "La Sal"))
        assertTrue(IosFollowMutation.contains(result, "ORGANIZER", "sal_org"))
    }
    @Test fun phpCanonicalLowercaseMatchesAndroidKey() {
        val row = IosFollow("ARTIST", "maria", "Maria")
        assertTrue(IosFollowMutation.contains(listOf(row), "ARTIST", " MARIA "))
        val category = IosFollow("CATEGORY", "music__jazz_blues_soul", "Jazz, blues i soul")
        assertTrue(IosFollowMutation.contains(listOf(category), "CATEGORY", "MUSIC__JAZZ_BLUES_SOUL"))
        assertEquals("MUSIC__JAZZ_BLUES_SOUL", IosCategoryTaxonomy.subcategoryCode(category.key))
    }
    @Test fun publicLabelsCannotReplaceDistinctUsernames() {
        val a = IosFollow("ORGANIZER", "company_a", "Companyia")
        val b = IosFollow("ORGANIZER", "company_b", "Companyia")
        val result = IosFollowMutation.updated(listOf(a, b), "ORGANIZER", "company_b", "Companyia", false)
        assertEquals(listOf(a), result)
    }
    @Test fun offlineDesiredStateIsIdempotentOnReplay() {
        val a = IosFollowMutation.updated(emptyList(), "ARTIST", "Example", "Example", true)
        assertEquals(a, IosFollowMutation.updated(a,"ARTIST","example","Example",true))
        assertEquals(emptyList(), IosFollowMutation.updated(a,"ARTIST","example","Example",false))
    }
}
