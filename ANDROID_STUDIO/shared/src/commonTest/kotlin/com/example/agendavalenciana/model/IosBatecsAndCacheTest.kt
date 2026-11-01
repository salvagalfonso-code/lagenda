package com.example.agendavalenciana.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IosBatecsAndCacheTest {
    @Test fun stripsUnfollowedBatecsAndRequiresSix() {
        val source = (1..7).map { BatecCandidate("manual:$it", "La Maria") } + BatecCandidate("manual:8", "Altri")
        assertEquals((1..6).map { "manual:$it" }, FollowedBatecsPolicy.firstSix(source, setOf("la maria")))
        assertTrue(FollowedBatecsPolicy.firstSix(source.take(5), setOf("La Maria")).isEmpty())
        assertTrue(FollowedBatecsPolicy.firstSix(source, setOf("Altri")).isEmpty())
    }
    @Test fun preferencesAreAccountScoped() {
        assertTrue(IosAccountCachePolicy.key("pref_categories", 12) != IosAccountCachePolicy.key("pref_categories", 13))
        assertTrue(IosAccountCachePolicy.reminderKey(12, 99) != IosAccountCachePolicy.reminderKey(13, 99))
        assertTrue(IosAccountCachePolicy.decode("~", "Música", true).isEmpty())
        assertTrue(IosAccountCachePolicy.decode("", "Música", false).isEmpty())
    }
}
