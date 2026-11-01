package com.example.agendavalenciana.ios

/** Personal interests live only in RAM and in MariaDB (ios_recent_styles_v1).
 * Never persist them to UserDefaults; the UI posts changes through sync/state.php.
 */
object IosRecentInterest {
    private const val PERIOD = 15L * 24L * 60L * 60L * 1000L
    private val recentByUser = mutableMapOf<Int, List<String>>()
    private fun valid(list: List<String>, now: Long): List<String> = list.mapNotNull { line ->
        val p = line.indexOf(':')
        val stamp = if (p > 0) line.substring(0,p).toLongOrNull() else null
        if (stamp != null && stamp in (now - PERIOD)..now) line else null
    }.takeLast(120)
    fun hydrate(accountId: Int, raw: String, now: Long = currentEpochMillis()) {
        if (accountId > 0) recentByUser[accountId] = valid(raw.split(';'), now)
    }
    fun export(accountId: Int): String = accountId?.let { recentByUser[it] }.orEmpty().joinToString(";")
    fun recordOpen(accountId: Int?, event: AgendaEventSummary, now: Long = currentEpochMillis()) {
        val id = accountId?.takeIf { it > 0 } ?: return
        val codes = (event.styles + event.category).flatMap { it.split('|',',',';') }
            .map(IosGeoPlaces::key).filter { it.isNotBlank() && !it.contains(':') && !it.contains(';') }.distinct()
        recentByUser[id] = valid(recentByUser[id].orEmpty(), now)
            .plus(codes.map { "$now:$it" }).takeLast(120)
    }
    fun recordSearch(accountId: Int?, term: String, events: List<AgendaEventSummary>, now: Long = currentEpochMillis()) {
        val id = accountId?.takeIf { it > 0 } ?: return
        val q = IosGeoPlaces.key(term)
        if (q.length < 3) return
        val matched = events.asSequence().filter { event ->
            IosGeoPlaces.key(event.title).contains(q) ||
                (event.artists + event.schedule.map { it.artist }).any { IosGeoPlaces.key(it).contains(q) }
        }.take(6).toList()
        if (matched.isEmpty()) return
        val additions = matched.flatMap { it.styles + it.category }.flatMap { it.split('|', ',', ';') }
            .map(IosStyleAffinityCatalog::canonical).filter(String::isNotBlank)
            .distinct().map { "$now:$it" }
        recentByUser[id] = (valid(recentByUser[id].orEmpty(), now) + additions).takeLast(120)
    }
    fun current(accountId: Int?, now: Long = currentEpochMillis()): Set<String> =
        valid(accountId?.let { recentByUser[it] }.orEmpty(), now).mapNotNull { it.substringAfter(':', "").takeIf(String::isNotBlank) }.toSet()
    fun weighted(accountId: Int?, now: Long = currentEpochMillis()): Map<String, Int> {
        val counts = linkedMapOf<String, Int>()
        valid(accountId?.let { recentByUser[it] }.orEmpty(), now).forEach { entry ->
            val code = IosStyleAffinityCatalog.canonical(entry.substringAfter(':', ""))
            if (code.isNotBlank()) counts[code] = (counts[code] ?: 0) + 1
        }
        return counts
    }
}
