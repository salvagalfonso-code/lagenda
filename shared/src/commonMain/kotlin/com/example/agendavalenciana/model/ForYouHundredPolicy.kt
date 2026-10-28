package com.example.agendavalenciana.model

/**
 * v288 · Seguits, mateix ESTIL dins del radi elegit, subcategories marcades
 * dins de 100 km, i suggerències graduables de cerques i cards dels últims 15
 * dies dins del radi elegit. Cap publicitat ni reserva imposa una targeta.
 */
object ForYouHundredPolicy {
    // Compatibilitat amb codi antic: NO és un mínim exigible.
    const val MINIMUM_EVENTS = 100

    data class Candidate(
        val id: Int,
        val eligibleForRecommendations: Boolean = true,
        val followed: Boolean = false,
        /** No mostrar targetes de reserva gratuïta interna en «Per a tu». */
        val freeInternalReservation: Boolean = false,
        val interest: Boolean = false,
        val proximity: Boolean = false,
        val proximityScore: Int = 0,
        val discovery: Boolean = false,
        val recent: Boolean = false,
        val affinityScore: Int = 0,
        val publishedAtMillis: Long = 0L,
        /** Explicacions dels senyals calculats al mateix moment que la selecció. */
        val diagnosticSignals: List<String> = emptyList()
    )

    data class Selection(val ids: List<Int>, val reasons: Map<Int, String>)

    /**
     * Quatre vies. La quarta és graduable i només treballa amb cerques/obertures
     * recents dins del radi configurat. No hi ha garantia comercial ni mínim de 100.
     */
    fun selectDetailed(
        candidates: List<Candidate>,
        level: DiscoveryLevel = DiscoveryLevel.BALANCED,
        // En producció no es mostren els diagnòstics. No formar cadenes per a
        // centenars de candidats si ningú no les consumirà.
        includeReasons: Boolean = true
    ): Selection {
        val unique = candidates.distinctBy { it.id }
            .filterNot { it.freeInternalReservation }
        val reasons = linkedMapOf<Int, String>()
        val selectedIds = HashSet<Int>(unique.size)
        val core = unique.filter { item ->
            item.followed || (item.eligibleForRecommendations && (item.interest || item.proximity))
        }
        core.forEach { item ->
            selectedIds += item.id
            if (includeReasons) {
                val routes = buildList {
                    if (item.followed) add("Seguits: artistes, organitzadors o sales")
                    if (item.interest) add("ESTIL d'un Seguit i distància elegida")
                    if (item.proximity) add("Preferències culturals o territorials")
                }
                reasons[item.id] = (routes + item.diagnosticSignals).distinct().joinToString(" · ")
            }
        }
        val suggestionCandidates = unique.filter { item ->
            item.id !in selectedIds && item.eligibleForRecommendations && item.recent && item.discovery
        }
        // El grau governa la quantitat de descobertes, no força un nombre total.
        // Sense actes base, permet un nombre menut de propostes si hi ha senyals reals.
        val coreCount = core.size
        val quota = when (level) {
            DiscoveryLevel.MY_TASTES -> if (coreCount == 0) 1 else (coreCount / 6).coerceIn(1, 8)
            DiscoveryLevel.BALANCED -> if (coreCount == 0) 3 else (coreCount / 3).coerceIn(2, 20)
            DiscoveryLevel.EXPLORE -> if (coreCount == 0) 8 else ((coreCount * 2) / 3).coerceIn(4, 40)
        }
        suggestionCandidates.sortedWith(compareByDescending<Candidate> { it.affinityScore }
            .thenBy { it.id }).take(quota).forEach { item ->
            selectedIds += item.id
            if (includeReasons) {
                reasons[item.id] = (listOf("Suggerència: cerques o grups de cards obertes durant 15 dies") +
                    item.diagnosticSignals).distinct().joinToString(" · ")
            }
        }
        return Selection(unique.map { it.id }.filter(selectedIds::contains), reasons)
    }

    /** API antiga mantinguda perquè altres clients no es trenquen. */
    @Suppress("UNUSED_PARAMETER")
    fun select(
        candidates: List<Candidate>,
        minimum: Int = MINIMUM_EVENTS,
        level: DiscoveryLevel = DiscoveryLevel.BALANCED
    ): List<Int> = selectDetailed(candidates, level).ids
}
