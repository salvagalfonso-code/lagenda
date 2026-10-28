package com.example.agendavalenciana.data

/** Les jornades anteriors al dia actual no es mostren al popup públic del festival. */
object FestivalDayVisibility {
    private val isoDate = Regex("\\d{4}-\\d{2}-\\d{2}")

    fun upcomingDays(dates: List<String>, today: String): List<String> =
        dates.asSequence().map { it.take(10) }
            .filter { it.matches(isoDate) && it >= today }
            .distinct().sorted().toList()

    fun isPastDay(date: String, today: String): Boolean =
        date.take(10).matches(isoDate) && date.take(10) < today
}
