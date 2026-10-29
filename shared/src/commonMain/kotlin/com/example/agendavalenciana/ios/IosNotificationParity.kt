package com.example.agendavalenciana.ios

import com.example.agendavalenciana.model.IosBatecTime

/** On-device preferences for the in-app inbox; scoped to the account and never mistaken for server email settings. */
data class IosNotificationPreferences(
    val followedArtist: Boolean = true,
    val upcoming: Boolean = true,
    val tickets: Boolean = true,
    val community: Boolean = true,
    val suggestions: Boolean = true,
    val systemAlerts: Boolean = true,
    val quietHours: Boolean = false,
    val quietFrom: String = "23:00",
    val quietUntil: String = "08:00"
) {
    fun encode(): String = listOf(followedArtist,upcoming,tickets,community,suggestions,
        systemAlerts,quietHours,quietFrom,quietUntil).joinToString("|")
    companion object {
        fun decode(value: String): IosNotificationPreferences {
            val fields = value.split('|')
            if (fields.size != 9) return IosNotificationPreferences()
            val times = fields.drop(7)
            if (times.any { !it.matches(Regex("(?:[01]\\d|2[0-3]):[0-5]\\d")) }) return IosNotificationPreferences()
            return IosNotificationPreferences(fields[0] == "true",fields[1] == "true",fields[2] == "true",
                fields[3] == "true",fields[4] == "true",fields[5] == "true",fields[6] == "true",
                fields[7],fields[8])
        }
    }
}

/** Wire representation matches the existing Android mail/preferences-api.php contract. */
data class IosEmailPreferences(
    val frequency: String = "NONE",
    val recommendations: Boolean = false,
    val followedArtists: Boolean = false,
    val savedReminders: Boolean = false,
    val personalizedAds: Boolean = false
)

/** Stable local/remote references. A locally derived notification is never sent to the PHP read endpoint. */
data class IosNotificationEntry(
    val key: String, val section: String, val title: String, val body: String,
    val eventId: Int?, val timeKey: String,
    val remoteId: Int? = null, val artistName: String = ""
)

/** Implements Android's saved-next-seven-days and bell-artist logic from real event metadata. */
object IosNotificationParity {
    private const val WEEK = 7L * 86_400_000L
    private fun norm(s: String) = IosAdvancedSearch.norm(s)
    private fun future(event: AgendaEventSummary, today: String): Boolean =
        event.endDate.ifBlank { event.date } >= today
    private fun matchesArtist(event: AgendaEventSummary, name: String): Boolean =
        (event.artists + event.schedule.map { it.artist }).any { norm(it) == norm(name) }

    fun build(
        remote: List<IosNotification>, events: List<AgendaEventSummary>, saved: Set<Int>,
        bells: List<IosArtistBell>, today: String, nowMillis: Long,
        preferences: IosNotificationPreferences
    ): List<IosNotificationEntry> {
        val entries = mutableListOf<IosNotificationEntry>()
        remote.filter { notice ->
            when (IosNotificationGrouping.fromDeepLinkType(notice.linkType)) {
                IosNotificationGrouping.COMMUNITY -> preferences.community
                IosNotificationGrouping.NEW -> preferences.followedArtist
                IosNotificationGrouping.UPCOMING -> preferences.upcoming
                IosNotificationGrouping.TICKETS -> preferences.tickets
                IosNotificationGrouping.SUGGESTED -> preferences.suggestions
                else -> true
            }
        }.forEach { notice ->
            val section = IosNotificationGrouping.fromDeepLinkType(notice.linkType)
            entries += IosNotificationEntry(
                key = "server:${notice.id}", section = section, title = notice.title,
                body = notice.body, eventId = notice.linkValue.toIntOrNull(),
                timeKey = notice.createdAt, remoteId = notice.id,
                artistName = if (notice.linkType.uppercase() in setOf("ARTIST", "NEW_ARTIST_EVENT")) notice.linkValue else ""
            )
        }
        if (preferences.upcoming && IosFilterDates.valid(today)) {
            val weekEnd = IosFilterDates.addDays(today, 7)
            events.asSequence().filter { it.id in saved && future(it,today) && it.date in today..weekEnd }
                .distinctBy { it.id }.forEach { ev ->
                    entries += IosNotificationEntry("saved:${ev.id}:${ev.date}",
                        IosNotificationGrouping.UPCOMING, "Pròxim: ${ev.title}",
                        listOf(ev.date,ev.time,ev.town).filter(String::isNotBlank).joinToString(" · "),
                        ev.id, ev.date)
                }
        }
        if (preferences.followedArtist && bells.isNotEmpty()) {
            val byArtist = bells.filter { it.enabled }.distinctBy { norm(it.artistName) }
            byArtist.forEach { bell ->
                val gap = when (bell.frequency.uppercase()) {
                    "DAILY" -> 86_400_000L; "WEEKLY" -> WEEK; else -> 0L
                }
                val visibility = if (gap == 0L) WEEK else gap
                var releaseAt: Long? = null
                val scheduled = events.asSequence().filter { future(it,today) && matchesArtist(it,bell.artistName) }
                    .mapNotNull { ev ->
                        val created = IosBatecTime.epochMillis(ev.createdAt) ?: return@mapNotNull null
                        if (created <= 0L || created > nowMillis) return@mapNotNull null
                        ev to created
                    }.sortedWith(compareBy<Pair<AgendaEventSummary, Long>> { it.second }.thenBy { it.first.id })
                    .map { (ev,created) ->
                        val release = releaseAt?.let { maxOf(created,it+gap) } ?: created
                        releaseAt = release
                        ev to release
                    }.filter { (_,release) -> release <= nowMillis && (nowMillis-release) < visibility }.toList()
                scheduled.maxByOrNull { it.second }?.let { (ev,release) ->
                    entries += IosNotificationEntry("artist:${norm(bell.artistName)}:${ev.id}:$release",
                        IosNotificationGrouping.NEW, "Nou esdeveniment de ${bell.artistName}",
                        "${ev.title} · ${ev.date} · ${ev.town}",ev.id, ev.createdAt,
                        artistName=bell.artistName)
                }
            }
        }
        return entries.distinctBy { it.key }.sortedWith(compareByDescending<IosNotificationEntry> { it.timeKey }.thenBy { it.key })
    }

    /** Only explicit taste AND locality matches are presented; no speculative activity/popularity scores. */
    fun suggestions(events: List<AgendaEventSummary>, saved: Set<Int>,
        followedArtists: Set<String>, categories: Set<String>, towns: Set<String>,
        today: String, enabled: Boolean): List<AgendaEventSummary> {
        if (!enabled || categories.isEmpty() || towns.isEmpty() || !IosFilterDates.valid(today)) return emptyList()
        val end = IosFilterDates.addDays(today,7)
        val normalizedCats = categories.map(::norm).toSet()
        val normalizedTowns = towns.map(::norm).toSet()
        val normalizedFollows = followedArtists.map(::norm).toSet()
        return events.asSequence().filter { e ->
            e.date in today..end && e.id !in saved && norm(e.town) in normalizedTowns &&
                norm(e.category) in normalizedCats &&
                (e.artists + e.schedule.map { it.artist }).none { norm(it) in normalizedFollows }
        }.distinctBy { it.id }.sortedWith(compareBy<AgendaEventSummary> { it.date }.thenBy { it.time })
            .take(5).toList()
    }
}
