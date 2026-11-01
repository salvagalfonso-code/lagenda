package com.example.agendavalenciana.ios

/**
 * Step 13: chronological, non-personalized agenda equivalent to Android v336's
 * sharedUnfilteredBase -> expandMultiDayOccurrences -> orderVisibleAgendaDays.
 * No account preferences or scoring enter this selection. Visibility is applied
 * by the caller after day projection; the original event ID never changes.
 *
 * Dates are ISO YYYY-MM-DD in the same local calendar day as currentLocalDateIso().
 * Scheduled dates are explicit data, NOT generated from the start/end interval.
 */
object IosUnfilteredAgenda {
    private val programmeWords = Regex("\\b(festival|fira|feria|fest|cicle|ciclo|jornades?|mostra|certamen|aplec)\\b", RegexOption.IGNORE_CASE)
    private val festivalWords = Regex("\\b(festival|fest|fira|feria|aplec)\\b", RegexOption.IGNORE_CASE)

    fun validDay(date: String): Boolean {
        if (date.length != 10 || date[4] != '-' || date[7] != '-') return false
        val y = date.substring(0, 4).toIntOrNull() ?: return false
        val m = date.substring(5, 7).toIntOrNull() ?: return false
        val d = date.substring(8, 10).toIntOrNull() ?: return false
        if (y !in 1..9999 || m !in 1..12) return false
        val days = when (m) {
            2 -> if (y % 400 == 0 || (y % 4 == 0 && y % 100 != 0)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
        return d in 1..days
    }

    private fun daysFromCivil(date: String): Long? {
        if (!validDay(date)) return null
        var y = date.substring(0,4).toLong()
        val m = date.substring(5,7).toInt()
        val d = date.substring(8,10).toLong()
        if (m <= 2) y--
        val era = y / 400L
        val yoe = y - era * 400L
        val mp = m + if (m > 2) -3 else 9
        val doy = (153L * mp + 2L) / 5L + d - 1L
        return era * 146097L + yoe * 365L + yoe / 4L - yoe / 100L + doy - 719468L
    }

    private fun civilFromDays(epoch: Long): String {
        val z = epoch + 719468L
        val era = z / 146097L
        val doe = z - era * 146097L
        val yoe = (doe - doe / 1460L + doe / 36524L - doe / 146096L) / 365L
        var y = yoe + era * 400L
        val doy = doe - (365L * yoe + yoe / 4L - yoe / 100L)
        val mp = (5L * doy + 2L) / 153L
        val d = doy - (153L * mp + 2L) / 5L + 1L
        val m = mp + if (mp < 10L) 3L else -9L
        if (m <= 2L) y++
        return "${y.toString().padStart(4,'0')}-${m.toString().padStart(2,'0')}-${d.toString().padStart(2,'0')}"
    }

    fun scheduledDays(event: AgendaEventSummary): List<String> =
        (event.schedule.map { it.date.take(10) } + event.feedOccurrenceDate.takeIf { it.isNotBlank() }.orEmpty())
            .filter(::validDay).distinct().sorted()

    private fun isProgramme(event: AgendaEventSummary): Boolean {
        if (event.category.contains("expos", true)) return false
        val structure = event.eventStructure.uppercase()
        if (structure in setOf("FESTIVAL", "CYCLE", "PROGRAMME")) return true
        if (structure == "STANDARD") return false
        return programmeWords.containsMatchIn(event.title) &&
            (event.endDate > event.date || scheduledDays(event).size > 1)
    }

    fun isConsecutiveFestival(event: AgendaEventSummary, dates: List<String> = scheduledDays(event)): Boolean {
        if (!isProgramme(event) || event.eventStructure in setOf("CYCLE", "PROGRAMME")) return false
        if (event.eventStructure != "FESTIVAL" && !festivalWords.containsMatchIn(event.title)) return false
        return dates.size >= 2 && dates.zipWithNext().all { (a, b) ->
            val aDay = daysFromCivil(a) ?: return@all false
            daysFromCivil(b) == aDay + 1L
        }
    }

    fun isFestivalBeforeStart(event: AgendaEventSummary, today: String): Boolean {
        val days = scheduledDays(event)
        return isConsecutiveFestival(event, days) && days.firstOrNull()?.let { today < it } == true
    }

    private fun onDay(event: AgendaEventSummary, day: String, preStart: Boolean = false): AgendaEventSummary {
        if (preStart) return event.copy(feedOccurrenceDate = day)
        val slots = event.schedule.filter { it.date.take(10) == day }
        val time = slots.map { it.time }.filter(String::isNotBlank).minOrNull()
        return event.copy(feedOccurrenceDate = day,
            time = time ?: if (day == event.date) event.time else "")
    }

    /** In Android, exhibitions lasting >5 days show one card (weekly from day 8).
     * They are not treated as festivals nor expanded into fictitious performances. */
    private fun longExhibition(event: AgendaEventSummary, today: String): AgendaEventSummary? {
        val start = daysFromCivil(event.date) ?: return null
        val end = daysFromCivil(event.endDate) ?: return null
        val now = daysFromCivil(today) ?: return null
        val span = end - start + 1L
        if (span <= 5L) return null
        val target = when {
            now <= start -> start
            span <= 7L -> now
            else -> start + ((now - start + 6L) / 7L) * 7L
        }
        return if (target <= end) onDay(event, civilFromDays(target)) else null
    }

    fun hasFutureDate(event: AgendaEventSummary, today: String): Boolean {
        if (!validDay(today)) return true
        return listOf(event.date, event.endDate, event.feedOccurrenceDate)
            .plus(event.schedule.map { it.date.take(10) })
            .any { validDay(it) && it >= today }
    }

    private fun occurrences(event: AgendaEventSummary, today: String): List<AgendaEventSummary> {
        val start = event.date
        val existing = event.feedOccurrenceDate
        if (event.category.contains("expos", true)) {
            if (daysFromCivil(event.endDate)?.minus(daysFromCivil(start) ?: return emptyList())?.let { it > 4 } == true) {
                return listOfNotNull(longExhibition(event, today))
            }
        }
        val days = scheduledDays(event)
        if (isProgramme(event)) {
            if (days.isEmpty()) return if (validDay(start) && start >= today) listOf(event) else emptyList()
            val future = days.filter { it >= today }
            if (future.isEmpty()) return emptyList()
            // Consecutive festival: one evolving card, full lineup prior to opening.
            if (isConsecutiveFestival(event, days)) {
                return listOf(onDay(event, future.first(), preStart = today < days.first()))
            }
            return future.map { onDay(event, it) }
        }
        // One server occurrence is already materialized; don't expand it a second time.
        if (validDay(existing)) return if (existing >= today) listOf(event) else emptyList()
        if (days.isNotEmpty()) return days.filter { it >= today }.map { onDay(event, it) }
        // A date range without a real programme does not create imaginary days.
        return if (validDay(start) && start >= today) listOf(event) else emptyList()
    }

    /** Conservative Android-compatible dedupe: only exact canonical titles, place,
     * day and compatible performers. Fuzzy cross-title matching is deliberately not
     * performed without Android's JVM-only AgendaDedupe engine. */
    private fun normalized(value: String): String = value.trim().lowercase()
        .replace("à", "a").replace("á", "a").replace("è", "e").replace("é", "e")
        .replace("í", "i").replace("ï", "i").replace("ò", "o").replace("ó", "o")
        .replace("ú", "u").replace("ü", "u").replace("ç", "c").replace("ñ", "n")
        .replace(Regex("[^a-z0-9]+"), " ").trim()

    private fun areSameEvent(a: AgendaEventSummary, b: AgendaEventSummary): Boolean {
        if (a.id == b.id || a.date != b.date || a.feedOccurrenceDate != b.feedOccurrenceDate ||
            normalized(a.town) != normalized(b.town) || normalized(a.town).isBlank()) return false
        if ((a.isFestival || a.endDate > a.date) != (b.isFestival || b.endDate > b.date)) return false
        if (normalized(a.title).isBlank() || normalized(a.title) != normalized(b.title)) return false
        val va = normalized(a.venue)
        val vb = normalized(b.venue)
        if (va.isNotBlank() && vb.isNotBlank() && va != vb) return false
        if (a.time.isNotBlank() && b.time.isNotBlank() && a.time != b.time) return false
        if (a.isFestival || a.endDate > a.date) return va == vb || va.isBlank() || vb.isBlank()
        val artistsA = a.artists.map(::normalized).filter(String::isNotBlank).toSet()
        val artistsB = b.artists.map(::normalized).filter(String::isNotBlank).toSet()
        if (artistsA.isNotEmpty() && artistsB.isNotEmpty()) {
            val union = artistsA.union(artistsB).size
            return union > 0 && artistsA.intersect(artistsB).size.toDouble() / union >= .50
        }
        return (va.isNotBlank() && va == vb) || (artistsA.isEmpty() && artistsB.isEmpty())
    }

    private fun mergeDuplicated(a: AgendaEventSummary, b: AgendaEventSummary): AgendaEventSummary {
        val canonical = if (a.id <= b.id) a else b
        val extra = if (canonical.id == a.id) b else a
        return canonical.copy(
            artists = (canonical.artists + extra.artists).distinctBy(::normalized),
            organizers = (canonical.organizers + extra.organizers).distinctBy(::normalized),
            schedule = (canonical.schedule + extra.schedule).distinctBy { listOf(it.date, it.time, it.artist, it.title) },
            posterUrl = canonical.posterUrl.ifBlank { extra.posterUrl },
            subtitle = canonical.subtitle.ifBlank { extra.subtitle },
            description = canonical.description.ifBlank { extra.description },
            endDate = maxOf(canonical.endDate, extra.endDate),
            ticketAvailable = canonical.ticketAvailable || extra.ticketAvailable
        )
    }

    private fun dedupeCatalogue(catalog: List<AgendaEventSummary>, protectedIds: Set<Int>): List<AgendaEventSummary> {
        val result = mutableListOf<AgendaEventSummary>()
        val indices = HashMap<String, MutableList<Int>>()
        for (event in catalog) {
            val baseKey = "${event.date}|${normalized(event.town)}"
            val candidates = indices.getOrPut(baseKey) { mutableListOf() }
            val match = if (IosCommercialVisibility.protected(event, protectedIds)) null else
                candidates.firstOrNull { index ->
                    val previous = result[index]
                    !IosCommercialVisibility.protected(previous, protectedIds) && areSameEvent(previous, event)
                }
            if (match != null) result[match] = mergeDuplicated(result[match], event)
            else { candidates.add(result.size); result.add(event) }
        }
        return result
    }

    fun select(catalog: List<AgendaEventSummary>, today: String, protectedIds: Set<Int> = emptySet()): List<AgendaEventSummary> {
        if (!validDay(today)) return emptyList()
        return dedupeCatalogue(catalog, protectedIds).asSequence()
            .filter { hasFutureDate(it, today) }
            .flatMap { occurrences(it, today).asSequence() }
            .filter { validDay(it.displayDate) && it.displayDate >= today }
            .distinctBy { it.key }
            .sortedWith(compareBy<AgendaEventSummary> { it.displayDate }
                .thenBy { it.time }.thenBy { it.id })
            .toList()
    }
}
