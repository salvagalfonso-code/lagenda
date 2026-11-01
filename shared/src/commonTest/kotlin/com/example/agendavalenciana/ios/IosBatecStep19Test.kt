package com.example.agendavalenciana.ios

import com.example.agendavalenciana.model.BatecCandidate
import com.example.agendavalenciana.model.IosBatecArtistGroups
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IosBatecStep19Test {
    private val seven = (1..7).map { "Artista $it" }.toSet()
    private val calendar = (1..7).map { idx ->
        AgendaEventSummary(idx, "Concert $idx", "2026-11-08", "21:00", "València", "Sala", "Música", "", "",
            artists = listOf("Artista $idx"))
    }
    @Test fun requiresSevenFollowedButDisplaysExactlySix() {
        val six = seven.take(6).toSet()
        assertTrue(IosBatecFeedContract.project(six, emptySet(), calendar, emptyList(), emptyMap(), "2026-10-31").isEmpty())
        val projected = IosBatecFeedContract.project(seven, emptySet(), calendar, emptyList(), emptyMap(), "2026-10-31")
        val circles = IosBatecArtistGroups.select(projected.map { BatecCandidate(it.storyKey, it.artist, it.sourceType, it.publishedAt) }, seven, emptySet())
        assertEquals(6, circles.size)
        assertTrue(circles.all { it.allSeen })
    }
    @Test fun onlyFollowedUnblockedAndRealFutureDates() {
        val projected = IosBatecFeedContract.project(seven, setOf("Artista 7"), calendar,
            emptyList(), mapOf(1 to "2026-10-31 18:30:00"), "2026-10-31")
        assertTrue(projected.isEmpty())
        val future = IosBatecFeedContract.project(seven, emptySet(), calendar,
            emptyList(), mapOf(1 to "2026-10-31 18:30:00"), "2026-10-31")
        assertTrue(future.any { it.storyKey == "event:1:artista 1" })
    }
    @Test fun eventSeenUsesActualPhpKeyAndGreyRing() {
        assertEquals("event:42", IosBatecArtistGroups.canonicalSeenKey("event:42:la maria"))
        val candidates = (1..7).map { i -> BatecCandidate("event:$i:artista $i", "Artista $i", "AUTO_EVENT", "2026-10-31 18:40:00") }
        val circles = IosBatecArtistGroups.select(candidates, seven, setOf("event:1"))
        assertEquals(6, circles.size)
        assertTrue(circles.first().artist != "Artista 1")
    }
}
