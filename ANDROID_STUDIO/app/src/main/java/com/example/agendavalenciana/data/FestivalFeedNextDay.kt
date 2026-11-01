package com.example.agendavalenciana.data

import com.example.agendavalenciana.model.Concert
import com.example.agendavalenciana.model.CulturalCategory
import java.time.LocalDate

/**
 * Una fitxa de festival/cicle conserva sempre date/endDate originals.
 * El feed i el primer fotograma projecten només les jornades reals futures.
 * Si no coneixem cap jornada futura, no inventem dies ni recuperem els passats.
 */
object FestivalFeedNextDay {
    private val programmeHeading = Regex(
        "\\b(festival|fira|feria|fest|cicle|ciclo|jornades?|mostra|certamen|aplec)\\b",
        RegexOption.IGNORE_CASE
    )
    private fun parseDate(value: String?): LocalDate? = value?.trim()
        ?.takeIf { it.length >= 10 }
        ?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }

    /**
     * Un cicle amb dos jornades pot no assolir la regla del popup de festival.
     * Per al feed, el calendari també mana en estos formats de programació.
     */
    fun isFeedProgramme(concert: Concert): Boolean {
        if (concert.category == CulturalCategory.EXHIBITIONS) return false
        if (concert.isFestivalExperience) return true
        val structure = concert.eventStructure.trim().uppercase()
        if (structure in setOf("FESTIVAL", "PROGRAMME", "CYCLE")) return true
        if (structure == "STANDARD") return false
        val start = parseDate(concert.date)
        val end = parseDate(concert.endDate)
        val multipleDays = start != null && end != null && end.isAfter(start)
        return programmeHeading.containsMatchIn(concert.title) &&
            (multipleDays || concert.scheduleDates.distinct().size > 1)
    }

    /** Les dades parcials de les caches es complementen; no triem només una font. */
    fun scheduledDays(concert: Concert): List<LocalDate> = buildList {
        addAll(concert.scheduleDates.mapNotNull(::parseDate))
        addAll(concert.scheduleDayStartTimes.keys.mapNotNull(::parseDate))
        addAll(concert.scheduleDaySummaries.keys.mapNotNull(::parseDate))
        addAll(concert.scheduleDayTicketPrices.keys.mapNotNull(::parseDate))
        addAll(concert.eventInfo.dailyStart.mapNotNull { parseDate(it.date) })
        addAll(concert.ticketOptions.mapNotNull { parseDate(it.sessionDate) })
    }.distinct().sorted()

    /**
     * Festivals de jornades realment consecutives: una sola card que evoluciona.
     * Cicles amb pauses: una card per cadascuna de les jornades futures.
     * No deduïm dies del rang inici-fi: tots han de constar al calendari.
     */
    fun isConsecutiveFestival(concert: Concert): Boolean {
        if (!isFeedProgramme(concert)) return false
        val kind = concert.eventStructure.trim().uppercase()
        if (kind == "CYCLE" || kind == "PROGRAMME") return false
        val festivalName = Regex("\\b(festival|fest|fira|feria|aplec)\\b", RegexOption.IGNORE_CASE)
            .containsMatchIn(concert.title)
        if (kind != "FESTIVAL" && !festivalName) return false
        val dates = scheduledDays(concert)
        return dates.size >= 2 && dates.zipWithNext().all { (a, b) -> b == a.plusDays(1) }
    }

    /** La targeta inicial publicita el cartell complet; no amaguem els grups abans d'arrancar. */
    fun isFestivalBeforeStart(concert: Concert, today: LocalDate): Boolean =
        isConsecutiveFestival(concert) && scheduledDays(concert).firstOrNull()?.isAfter(today) == true

    /** Resum visual, no altera el cartell ni el programa íntegre del popup. */
    fun festivalPreview(concert: Concert, today: LocalDate, maxGroups: Int = 3): String {
        if (!isConsecutiveFestival(concert)) return ""
        val allDays = scheduledDays(concert)
        val preStart = allDays.firstOrNull()?.isAfter(today) == true
        val groups = if (preStart) concert.lineup.map(String::trim).filter(String::isNotBlank)
        else concert.scheduleDaySummaries[concert.feedDate.take(10)].orEmpty()
            .split('·').map(String::trim).filter(String::isNotBlank)
        val distinct = groups.distinctBy(String::lowercase)
        if (preStart) {
            return when {
                distinct.isNotEmpty() -> distinct.joinToString(" · ")
                else -> "Festival · ${allDays.size} jornades"
            }
        }
        if (distinct.isEmpty()) return "Programació de la jornada"
        val shown = distinct.take(maxGroups.coerceAtLeast(1)).joinToString(" · ")
        val extra = distinct.size - maxGroups.coerceAtLeast(1)
        return if (extra > 0) "$shown · + $extra grups" else shown
    }

    private fun projectScheduledDay(concert: Concert, day: LocalDate): Concert {
        val value = day.toString()
        val summary = concert.scheduleDaySummaries[value].orEmpty().trim()
        val artists = summary.split('·').map(String::trim).filter(String::isNotBlank)
            .distinctBy(String::lowercase)
        val firstTime = concert.scheduleDayStartTimes[value].orEmpty().trim()
            .ifBlank { concert.eventInfo.dailyStart.firstOrNull { it.date.take(10) == value }?.time.orEmpty() }
        val allDates = scheduledDays(concert).map(LocalDate::toString)
        return concert.copy(
            feedOccurrenceDate = value,
            selectedDate = value,
            feedInstanceUid = "event_${concert.id}_$value",
            lineup = if (artists.isNotEmpty()) artists else if (allDates.size > 1) emptyList() else concert.lineup,
            subtitle = summary.ifBlank { if (allDates.size > 1) "" else concert.subtitle },
            time = firstTime.ifBlank { if (value == concert.date.take(10)) concert.time else "" },
            ticketPrice = concert.scheduleDayTicketPrices[value] ?: concert.ticketPrice,
            scheduleDates = allDates,
            // Preservem la naturalesa de festival en la instància del feed: una
            // sola jornada pot tindre menys artistes que el programa complet.
            eventStructure = when {
                concert.eventStructure.trim().uppercase() != "AUTO" -> concert.eventStructure
                Regex("\\b(cicle|ciclo|jornades?|trobades?|circuit)\\b", RegexOption.IGNORE_CASE)
                    .containsMatchIn(concert.title) -> "CYCLE"
                else -> "FESTIVAL"
            },
            isMultidayFromServer = concert.isMultidayFromServer || allDates.size > 1
        )
    }

    /**
     * Quan no sabem quina és la jornada següent, NO assignem la data actual
     * d'un festival que va començar fa dies. És preferible que no aparega al
     * feed fins que arriben dates verificades de programació.
     */
    private fun unknownNextDay(concert: Concert, day: LocalDate): Concert? {
        val start = parseDate(concert.date) ?: return null
        if (start.isBefore(day)) return null
        return concert.copy(feedOccurrenceDate = null, selectedDate = null, feedInstanceUid = null)
    }

    /** Una sola jornada pendent, incloent l'arrancada des de la memòria cau. */
    fun next(concert: Concert, today: LocalDate): Concert? {
        if (!isFeedProgramme(concert)) return concert
        val upcoming = scheduledDays(concert).firstOrNull { !it.isBefore(today) }
        return if (upcoming != null) {
            if (isFestivalBeforeStart(concert, today)) concert.copy(
                feedOccurrenceDate = upcoming.toString(),
                selectedDate = upcoming.toString(),
                feedInstanceUid = "event_${concert.id}_festival_${upcoming}",
                scheduleDates = scheduledDays(concert).map(LocalDate::toString),
                eventStructure = if (concert.eventStructure.trim().uppercase() == "AUTO") "FESTIVAL" else concert.eventStructure
            ) else projectScheduledDay(concert, upcoming)
        } else unknownNextDay(concert, today)
    }

    /** En el feed complet només es generen les jornades amb data comprovada. */
    fun upcoming(concert: Concert, today: LocalDate): List<Concert> {
        if (!isFeedProgramme(concert)) return listOf(concert)
        val dates = scheduledDays(concert)
        return if (dates.isEmpty()) listOfNotNull(unknownNextDay(concert, today))
        else if (isConsecutiveFestival(concert)) listOfNotNull(next(concert, today))
        else dates.filterNot { it.isBefore(today) }.map { projectScheduledDay(concert, it) }
    }

    /**
     * Barrera final de seguretat per a snapshots, cachés residents i errors de
     * recàlcul: cap card del feed conserva una data d'una jornada passada.
     */
    fun cleanVisible(items: List<Concert>, today: LocalDate): List<Concert> {
        val todayText = today.toString()
        return items.asSequence().mapNotNull { concert ->
            val outdated = concert.feedDate.take(10) < todayText
            val fakeOngoing = concert.feedInstanceUid?.contains("_ongoing_") == true
            when {
                fakeOngoing || (outdated && isFeedProgramme(concert)) -> next(concert, today)
                outdated -> null
                else -> concert
            }
        }.filter { it.feedDate.take(10) >= todayText }
            .distinctBy { it.feedInstanceKey }
            .toList()
    }

    /** Només per a compatibilitat amb targetes antigues persistides. */
    @Suppress("UNUSED_PARAMETER")
    fun isOngoingWithoutSchedule(concert: Concert, today: LocalDate): Boolean =
        isFeedProgramme(concert) && concert.feedInstanceUid?.contains("_ongoing_") == true
}
