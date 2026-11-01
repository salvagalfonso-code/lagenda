package com.example.agendavalenciana.ios

import com.example.agendavalenciana.model.PersonalizationOptions

/** Pas 8: el contracte d'Android v336 i preferences/locations.php emmagatzema
 * els noms exactes del catàleg i un radi per poble; no exposa IDs numèrics.
 * No geocodifiquem, no llegim GPS ni creem llocs nous al mòbil.
 */
internal data class IosLocationSelection(
    val towns: Map<String, Int>,
    val comarques: Set<String>
)

internal object IosLocationRules {
    private fun key(name: String): String = name.trim().lowercase()

    /** Conserva els noms ja utilitzats per Android/servidor: només retalla espais,
     * valida el contracte i ordena. No elimina noms remots desconeguts.
     */
    fun prepare(towns: Set<String>, comarques: Set<String>, radii: Map<String, Int>): IosLocationSelection {
        require(towns.size <= 100 && comarques.size <= 100) { "Màxim 100 pobles i 100 comarques" }
        fun validName(name: String): String {
            val value = name.trim()
            require(value.isNotEmpty() && value.length <= 120) { "Nom de lloc invàlid" }
            return value
        }
        val names = towns.map(::validName)
        val regions = comarques.map(::validName)
        require(names.map(::key).distinct().size == names.size) { "Pobles duplicats" }
        require(regions.map(::key).distinct().size == regions.size) { "Comarques duplicades" }
        val normalizedRadii = radii.entries.associate { key(it.key) to it.value }
        val validatedTowns = names.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it })
            .associateWith { name ->
                // Android defaults missing/invalid radius values to 30 km.
                PersonalizationOptions.validRadius(normalizedRadii[key(name)])
            }
        return IosLocationSelection(validatedTowns,
            regions.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it }).toCollection(linkedSetOf()))
    }

    /** Les respostes HTTP genèriques no són prou: comparar la fotografia real
     * que torna l'API abans de treure la mutació de la cua xifrada.
     */
    fun matches(expected: IosLocationSelection, received: IosLocationSelection): Boolean =
        expected.towns.mapKeys { key(it.key) } == received.towns.mapKeys { key(it.key) } &&
            expected.comarques.map(::key).toSet() == received.comarques.map(::key).toSet()
}
