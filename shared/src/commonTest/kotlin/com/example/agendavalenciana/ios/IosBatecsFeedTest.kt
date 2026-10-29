package com.example.agendavalenciana.ios

import com.example.agendavalenciana.model.BatecCandidate
import com.example.agendavalenciana.model.IosBatecArtistGroups
import com.example.agendavalenciana.model.IosBatecTime
import com.example.agendavalenciana.model.IosBatecTimelinePolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IosBatecsFeedTest {
    private val followed = (1..7).map { IosFollow("ARTIST", "artist $it", "Artist $it") }
    private val now = IosBatecTime.epochMillis("2026-10-29 09:00:00")!!
    private fun catalogEvent(id: Int = 1, date: String = "2026-12-01") = AgendaEventSummary(
        id = id, title = "Concert", date = date, time = "20:00", town = "València",
        venue = "Sala", category = "Música", description = "", posterUrl = "",
        artists = listOf("Artist 1"), createdAt = "2026-10-29 08:00:00"
    )
    private fun assemble(
        follows: List<IosFollow> = followed,
        events: List<AgendaEventSummary> = emptyList(),
        server: List<IosBatec> = emptyList(),
        hidden: Set<String> = emptySet()
    ) = IosBatecsFeed.assemble(server, follows, events, emptyMap(), now, "2026-10-29", hidden)
    private fun groups(items: List<IosBatec>, seen: Set<String> = emptySet()) =
        IosBatecArtistGroups.select(items.map {
            BatecCandidate(it.storyKey, it.artist, it.publishedAtMillis,
                it.sourceType == "FOLLOWED_PLACEHOLDER")
        }, followed.map { it.label }.toSet(), seen, now)

    @Test fun exactlySixCirclesOnlyWithSevenFollowedArtists() {
        assertTrue(assemble(follows = followed.take(6)).isEmpty())
        val items = assemble()
        assertEquals(7, items.size)
        assertEquals(6, groups(items).size)
        assertTrue(groups(items).all { it.allSeen })
        assertTrue(assemble(hidden = setOf("Artist 7")).isEmpty())
    }
    @Test fun futureEventCreatesUnseenBatecButOldAndExpiredDoNot() {
        val fresh = assemble(events = listOf(catalogEvent()))
        assertEquals("event:1:artist 1", fresh.first().storyKey)
        assertEquals("artist 1", groups(fresh).first().key)
        assertTrue(!groups(fresh).first().allSeen)
        assertTrue(groups(fresh, setOf("event:1:artist 1")).first().allSeen)
        assertTrue(assemble(events = listOf(catalogEvent(date = "2026-10-28"))).all { it.eventId == 0 })
        assertTrue(IosBatecTimelinePolicy.isServerSeenKey("event:1:artist 1"))
        assertTrue(!IosBatecTimelinePolicy.isServerSeenKey("followed:artist 1"))
    }
    @Test fun manualPublicationSuppressesDuplicateAutomaticPublication() {
        val server = IosBatec("4",1,"Artist 1","Concert","Prova","","manual:4",now - 10_000L)
        assertEquals(1, assemble(events = listOf(catalogEvent()), server = listOf(server))
            .count { it.eventId == 1 })
    }
    @Test fun timestampsValidatedAndNormalizedLikeAndroid() {
        assertEquals(0L, IosBatecTime.epochMillis("1970-01-01 00:00:00"))
        assertEquals(null, IosBatecTime.epochMillis("2026-02-29 10:00:00"))
        assertEquals("l'home brut", IosBatecTimelinePolicy.normal("L’Home Brut"))
    }
}
