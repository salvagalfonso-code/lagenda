package com.example.agendavalenciana.ios

/** Pure tab-selection rules matching Android. Never confuse cultural-category navigation
 * with the advanced-search options sheet, and never mix personal recommendations into Seguits. */
object IosFeedModePolicy {
    private fun artistKey(raw: String): String = IosAdvancedSearch.norm(raw)
        .replace(Regex("[^a-z0-9 ]"), " ").replace(Regex("\\s+"), " ").trim()

    fun followed(events: List<AgendaEventSummary>, follows: List<IosFollow>, today: String): List<AgendaEventSummary> {
        val names = follows.asSequence().filter { it.type.equals("ARTIST", true) }
            .flatMap { sequenceOf(it.key, it.label) }.map(::artistKey)
            .filter(String::isNotBlank).toSet()
        if (names.isEmpty()) return emptyList()
        return events.filter { ev ->
            isUpcoming(ev, today) &&
                (ev.artists + ev.schedule.map { it.artist }).any { artistKey(it) in names }
        }.distinctBy { it.key }.sortedWith(chronological(today))
    }

    fun category(events: List<AgendaEventSummary>, code: String?, today: String): List<AgendaEventSummary> {
        val wanted = code?.takeIf(String::isNotBlank) ?: return emptyList()
        return events.filter { ev ->
            isUpcoming(ev, today) && IosPersonalizationTaxonomy.categoryForEvent(ev.category) == wanted
        }.distinctBy { it.key }.sortedWith(chronological(today))
    }

    fun categoryLabel(code: String?): String =
        (IosPersonalizationTaxonomy.categories + IosPersonalizationTaxonomy.children)
            .firstOrNull { it.code == code }?.label ?: "Categoria"

    private fun isUpcoming(e: AgendaEventSummary, today: String): Boolean =
        IosUnfilteredFeedPolicy.relevantDate(e.date, e.endDate, today,
            if (e.isFestival) e.schedule.map { it.date } else emptyList()) >= today

    private fun chronological(today: String): Comparator<AgendaEventSummary> =
        compareBy<AgendaEventSummary> {
            IosUnfilteredFeedPolicy.relevantDate(it.date, it.endDate, today,
                if (it.isFestival) it.schedule.map { slot -> slot.date } else emptyList())
        }.thenBy { it.time }.thenBy { it.id }
}
