package com.example.agendavalenciana.ios

import com.example.agendavalenciana.model.IosAccountCachePolicy

/** Only explicit event openings; searches of places alone never train suggestions.
 * Stored per account, at most 120 records, expires after 15 days.
 */
object IosRecentInterest {
    private const val PERIOD = 15L * 24L * 60L * 60L * 1000L
    fun recordOpen(accountId: Int?, event: AgendaEventSummary, now: Long = currentEpochMillis()) {
        val key = IosAccountCachePolicy.key("recent_styles", accountId)
        val old = platformReadPublicCache(key).split(';').mapNotNull { line ->
            val pos = line.indexOf(':')
            if (pos < 1) null else line.substring(0,pos).toLongOrNull()?.let { stamp ->
                if (stamp in (now-PERIOD)..now) line else null
            }
        }
        val codes = (event.styles + event.category).flatMap { it.split('|',',',';') }
            .map(IosGeoPlaces::key).filter { it.isNotBlank() && !it.contains(':') && !it.contains(';') }.distinct()
        val additions = codes.map { "$now:$it" }
        platformWritePublicCache(key,(old + additions).takeLast(120).joinToString(";"))
    }
    /** Committed searches only, never each keystroke. Place-only searches do not train tastes. */
    fun recordSearch(accountId: Int?, term: String, events: List<AgendaEventSummary>, now: Long = currentEpochMillis()) {
        val q = IosGeoPlaces.key(term)
        if (q.length < 3) return
        val matched = events.asSequence().filter { event ->
            IosGeoPlaces.key(event.title).contains(q) ||
                (event.artists + event.schedule.map { it.artist }).any { IosGeoPlaces.key(it).contains(q) }
        }.take(6).toList()
        if (matched.isEmpty()) return
        val old = platformReadPublicCache(IosAccountCachePolicy.key("recent_styles", accountId))
            .split(';').mapNotNull { line ->
                val pos = line.indexOf(':')
                if (pos < 1) null else line.substring(0,pos).toLongOrNull()?.let { stamp ->
                    if (stamp in (now-PERIOD)..now) line else null
                }
            }
        val additions = matched.flatMap { it.styles + it.category }.flatMap { it.split('|', ',', ';') }
            .map(IosStyleAffinityCatalog::canonical).filter(String::isNotBlank)
            .distinct().map { "$now:$it" }
        platformWritePublicCache(IosAccountCachePolicy.key("recent_styles", accountId),
            (old + additions).takeLast(120).joinToString(";"))
    }

    /** Repeated interests have more weight, with a hard bound and 15 day expiry. */
    fun weighted(accountId: Int?, now: Long = currentEpochMillis()): Map<String, Int> {
        val counts = linkedMapOf<String, Int>()
        platformReadPublicCache(IosAccountCachePolicy.key("recent_styles", accountId))
            .split(';').forEach { entry ->
                val pos = entry.indexOf(':')
                if (pos < 1) return@forEach
                val stamp = entry.substring(0,pos).toLongOrNull() ?: return@forEach
                if (stamp !in (now-PERIOD)..now) return@forEach
                val code = IosStyleAffinityCatalog.canonical(entry.substring(pos+1))
                if (code.isNotBlank()) counts[code] = (counts[code] ?: 0) + 1
            }
        return counts
    }

    fun current(accountId: Int?, now: Long = currentEpochMillis()): Set<String> {
        val raw = platformReadPublicCache(IosAccountCachePolicy.key("recent_styles", accountId))
        return raw.split(';').mapNotNull { entry ->
            val pos = entry.indexOf(':')
            if (pos < 1) return@mapNotNull null
            val stamp = entry.substring(0,pos).toLongOrNull() ?: return@mapNotNull null
            if (stamp !in (now-PERIOD)..now) null else entry.substring(pos+1).takeIf(String::isNotBlank)
        }.toSet()
    }
}
