package com.example.agendavalenciana.model

/**
 * La mida del catàleg NO determina si la selecció és completa:
 * llocs, radis i disciplines poden donar menys de 100 candidats orgànics.
 * La signatura de preferències i l'estat final es validen al repositori.
 */
object ForYouSnapshotReusePolicy {
    fun isComplete(
        ids: List<Int>,
        currentEventFingerprints: Map<Int, Int>,
        savedEventFingerprints: Map<Int, Int>
    ): Boolean =
        ids.size == ids.distinct().size &&
        ids.all { it in currentEventFingerprints } &&
        currentEventFingerprints == savedEventFingerprints

    /**
     * v324: no cal recalcular afinitats quan els únics canvis del catàleg
     * són activitats d'artistes seguits. Conservem la resta dels IDs triats,
     * eliminem fitxes passades/retirades i incorporem els nous seguits.
     * Qualsevol canvi en un candidat no seguit força el càlcul normal.
     */
    fun patchFollowedChanges(
        selectedIds: List<Int>,
        currentEventFingerprints: Map<Int, Int>,
        savedEventFingerprints: Map<Int, Int>,
        previousFollowedIds: Set<Int>,
        currentFollowedIds: Set<Int>
    ): List<Int>? {
        // Les snapshots anteriors a v324 no guarden seguits: recalcular una vegada.
        if (previousFollowedIds.isEmpty() && currentFollowedIds.isEmpty()) return null
        val changedIds = (currentEventFingerprints.keys + savedEventFingerprints.keys)
            .filterTo(HashSet()) { currentEventFingerprints[it] != savedEventFingerprints[it] }
        if (changedIds.isEmpty()) return null
        if (!changedIds.all { it in previousFollowedIds || it in currentFollowedIds }) return null
        val remaining = selectedIds.filter {
            it !in previousFollowedIds && it in currentEventFingerprints
        }
        return (remaining + currentFollowedIds.filter { it in currentEventFingerprints })
            .distinct()
    }
}
