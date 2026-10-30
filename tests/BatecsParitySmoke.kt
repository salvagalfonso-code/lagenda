package com.example.agendavalenciana.ios

import com.example.agendavalenciana.model.*

// Local smoke stubs for isolated JVM policy checks. Not distributed in the app's sourceSets.
data class IosFollow(val type: String, val key: String, val label: String)
data class AgendaScheduleEntry(val artist: String)
data class AgendaEventSummary(
    val id: Int, val title: String, val date: String, val endDate: String = "",
    val time: String = "", val town: String = "", val posterUrl: String = "",
    val artists: List<String> = emptyList(), val schedule: List<AgendaScheduleEntry> = emptyList(),
    val createdAt: String = ""
)
data class IosBatec(val id: String, val eventId: Int, val artist: String, val eventTitle: String,
    val text: String, val imageUrl: String, val storyKey: String = "",
    val publishedAtMillis: Long = 0L, val sourceType: String = "MANUAL")

fun main() {
    var checks = 0
    fun verify(condition: Boolean, title: String) { check(condition) { "FAILED: $title" }; checks++ }
    val now = IosBatecTime.epochMillis("2026-10-29 09:00:00")!!
    val follows7 = (1..7).map { IosFollow("ARTIST", "artist $it", "Artist $it") }
    fun compose(
        server: List<IosBatec> = emptyList(),
        follows: List<IosFollow> = follows7,
        events: List<AgendaEventSummary> = emptyList(),
        creations: Map<Int, Long> = emptyMap(),
        hidden: Set<String> = emptySet()
    ) = IosBatecsFeed.assemble(server,follows,events,creations,now,"2026-10-29",hidden)
    fun groups(items: List<IosBatec>, follows: List<IosFollow> = follows7, seen: Set<String> = emptySet()) =
        IosBatecArtistGroups.select(items.map { BatecCandidate(it.storyKey,it.artist,it.publishedAtMillis,
            it.sourceType == "FOLLOWED_PLACEHOLDER") }, follows.map { it.label }.toSet(),seen,now)
    verify(compose(follows=follows7.take(6)).isEmpty(), "Android gate: requires more than six follows")
    val placeholders = compose()
    verify(placeholders.size == 7 && placeholders.all { it.sourceType == "FOLLOWED_PLACEHOLDER" }, "Seven followed artists produce grey placeholders")
    verify(groups(placeholders).size == 6, "Exactly six circles")
    verify(groups(placeholders).all { it.allSeen }, "No false unread placeholder")
    verify(compose(hidden=setOf("Artist 7")).isEmpty(), "Excluded artists count toward fewer than seven")
    val event=AgendaEventSummary(123,"Nou directe","2026-12-15", artists=listOf("Artist 1"), createdAt="2026-10-29 08:00:00")
    val fromEvents = compose(events=listOf(event))
    verify(fromEvents.any { it.storyKey == "event:123:artist 1" && it.sourceType == "AUTO_EVENT" }, "Real new event creates a Batec")
    verify(groups(fromEvents).first().key == "artist 1" && !groups(fromEvents).first().allSeen, "New event ranked first in purple")
    verify(groups(fromEvents, seen=setOf("event:123:artist 1")).first().allSeen, "New event grey after server confirmed seen")
    verify(IosBatecTimelinePolicy.isServerSeenKey("event:123:artist 1"), "Seen keys include normalized artist name with spaces")
    verify(!IosBatecTimelinePolicy.isServerSeenKey("followed:artist 1"), "No fake seen request for placeholders")
    verify(compose(events=listOf(event.copy(date="2026-10-28"))).all { it.eventId == 0 }, "Expired events excluded")
    val oldEvent = event.copy(createdAt="2026-10-20 08:00:00")
    val oldItems = compose(events=listOf(oldEvent))
    verify(oldItems.any { it.eventId==123 } && groups(oldItems).all { it.allSeen }, "Older events retained but grey")
    val manual = IosBatec("22",123,"Artist 1","directe","Publicat","", "manual:22",now-2000)
    verify(compose(listOf(manual),events=listOf(event)).count { it.eventId == 123 }==1, "Manual Batec suppresses duplicate auto for same event and artist")
    val lineup = event.copy(id=124,artists=listOf("Other"),schedule=listOf(AgendaScheduleEntry("Artist 2")))
    verify(compose(events=listOf(lineup)).any { it.artist == "Artist 2" && it.eventId==124 }, "Festival lineup included")
    verify(IosBatecTime.epochMillis("1970-01-01 00:00:00") == 0L, "UTC epoch parser")
    verify(IosBatecTime.epochMillis("2026-02-29 10:00:00") == null, "Invalid leap dates excluded")
    verify(IosBatecTimelinePolicy.normal("L’Home Brut") == "l'home brut", "Apostrophe normalization")
    println("OK: $checks Batecs Android/iOS parity smoke tests")
}
