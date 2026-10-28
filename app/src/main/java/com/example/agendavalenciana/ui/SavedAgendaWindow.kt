package com.example.agendavalenciana.ui

import java.time.LocalDate

/**
 * General i les llistes pròpies mostren TOTS els esdeveniments guardats disponibles
 * en ordre cronològic, independentment dels filtres del feed i sense límit de dies.
 */
internal fun <T> savedGeneralAgenda(
    values: Collection<T>,
    date: (T) -> LocalDate,
    time: (T) -> String
): List<T> = values.sortedWith(
    compareBy<T> { date(it) }.thenBy(time)
)

/** Dates de la jornada cultural: fins a les 05:00 del dia següent continuen vigents.
 * El `today` és proporcionat per AgendaState.agendaToday() (Europe/Madrid).
 * En dates incompletes, mantindre visible és més prudent que ocultar erròniament.
 */
internal fun savedDateNotPast(raw: String, today: LocalDate): Boolean =
    runCatching { LocalDate.parse(raw.trim().take(10)) }.getOrNull()
        ?.let { !it.isBefore(today) } ?: true

/** Per a festivals, usar l'última jornada real; no descartar-los pel primer dia. */
internal fun savedEventNotPast(
    startDate: String,
    endDate: String,
    scheduleDates: Collection<String>,
    today: LocalDate
): Boolean {
    val lastDay = sequenceOf(startDate, endDate)
        .plus(scheduleDates.asSequence())
        .mapNotNull { runCatching { LocalDate.parse(it.trim().take(10)) }.getOrNull() }
        .maxOrNull()
    return lastDay?.let { !it.isBefore(today) } ?: true
}
