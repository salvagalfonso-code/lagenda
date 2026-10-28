package com.example.agendavalenciana.ios

/** iOS feed pagination + compact-card presentation. No Compose/state dependencies.
 * Cards are never split or reordered by the current progressive page size. */
object IosUnfilteredFeedPolicy {
    const val initialCount = 5
    const val secondCount = 20 // first 5 + next 15
    const val laterStep = 10

    fun nextCount(visible: Int, total: Int): Int = when {
        total <= 0 -> 0
        visible < initialCount -> initialCount.coerceAtMost(total)
        visible < secondCount -> secondCount.coerceAtMost(total)
        else -> (visible + laterStep).coerceAtMost(total)
    }

    /** Date as on Android detail screens: DD/MM/YYYY. Input remains ISO. */
    fun fullDate(raw: String): String = if (raw.length >= 10 &&
        raw[4] == '-' && raw[7] == '-') "${raw.substring(8,10)}/${raw.substring(5,7)}/${raw.substring(0,4)}"
        else raw

    fun cardDate(iso: String): String {
        if (iso.length < 10) return iso
        val month = iso.substring(5, 7).toIntOrNull() ?: return iso
        val day = iso.substring(8, 10).toIntOrNull() ?: return iso
        val names = listOf("gen", "feb", "mar", "abr", "mai", "jun", "jul", "ago", "set", "oct", "nov", "des")
        return if (month in 1..12 && day in 1..31) "${day.toString().padStart(2, '0')} ${names[month - 1]}" else iso
    }

    /** When the festival began yesterday, the compact card advertises the next scheduled day,
     * not the already elapsed opening date. Never alter the underlying event identity. */
    fun relevantDate(start: String, end: String, today: String, scheduledDates: List<String>): String {
        if (start.isBlank()) return start
        if (start >= today) return start
        val next = scheduledDates.asSequence().map { it.take(10) }
            .filter { it.length == 10 && it >= today && (end.isBlank() || it <= end) }
            .minOrNull()
        if (next != null) return next
        return if (end.isNotBlank() && end >= today) today else start
    }

    fun ongoingWithoutSchedule(start: String, end: String, today: String, dates: List<String>): Boolean =
        start.isNotBlank() && start < today && end >= today && dates.none { it.take(10) >= today }

    /** Only the relevant festival day appears in the secondary card line. */
    fun festivalSummary(date: String, entries: List<Pair<String, String>>, fallback: String): String {
        val names = entries.asSequence().filter { it.first.take(10) == date }
            .map { it.second.trim() }.filter(String::isNotBlank).distinctBy { it.lowercase() }.toList()
        return if (names.isEmpty()) fallback.trim() else {
            val first = names.take(2).joinToString(" · ")
            if (names.size > 2) "$first · +${names.size - 2}" else first
        }
    }

    fun ticketLabel(available: Boolean, rawPrice: String): String {
        val price = rawPrice.trim().removeSuffix("€").trim()
        return when {
            price.toDoubleOrNull() == 0.0 || rawPrice.trim().equals("gratuït", true) -> "Gratuït"
            price.toDoubleOrNull()?.let { it > 0 } == true -> "$price €"
            available -> "Entrades"
            else -> "Sense venda"
        }
    }
}
