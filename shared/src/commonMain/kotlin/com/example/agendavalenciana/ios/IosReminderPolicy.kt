package com.example.agendavalenciana.ios

/** Pas 28. Un sol planificador de recordatoris: MariaDB via sync/state.php.
 * Les notificacions locals heretades es poden cancel·lar, però mai crear de nou.
 * La selecció de dies i hores es fa en hora local i després es converteix a epoch UTC.
 */
internal object IosReminderPolicy {
    fun validLocalDateTime(value: String): Boolean =
        Regex("\\d{4}-\\d{2}-\\d{2}T(?:[01]\\d|2[0-3]):[0-5]\\d").matches(value)

    fun requireFuture(eventId: Int, enabled: Boolean, epoch: Long, now: Long) {
        require(eventId > 0) { "Esdeveniment invàlid" }
        if (enabled) require(epoch > now) { "L'hora del recordatori ha de ser futura" }
    }

    /** Never erase another reminder because the local snapshot is missing. */
    fun updatedIds(current: Set<String>?, eventId: Int, enabled: Boolean): Set<String>? {
        if (current == null) return null
        val next = current.filterTo(mutableSetOf()) { it.toIntOrNull()?.let { n -> n > 0 } == true }
        if (enabled) next.add(eventId.toString()) else next.remove(eventId.toString())
        return next
    }

    fun projectedAlerts(server: Set<Int>, actions: List<Pair<Int, Boolean>>): Set<Int> {
        val result = server.toMutableSet()
        actions.forEach { (id, enabled) ->
            require(id > 0)
            if (enabled) result.add(id) else result.remove(id)
        }
        return result
    }
}
