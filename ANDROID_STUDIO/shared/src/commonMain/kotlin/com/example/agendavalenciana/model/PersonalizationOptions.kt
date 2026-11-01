package com.example.agendavalenciana.model

/** Opcions estables de Personalització; les claus persistides no depenen de traduccions. */
enum class DiscoveryLevel(val label: String, val description: String) {
    MY_TASTES("Els meus gustos", "Quasi tot relacionat amb els meus interessos"),
    BALANCED("Equilibrat", "Combina els meus interessos amb descobertes"),
    EXPLORE("Descobrir més", "Inclou més propostes diferents dels meus gustos")
}

object PersonalizationOptions {
    val locationRadiiKm: List<Int> = listOf(5, 15, 30, 50, 100)
    const val defaultRadiusKm: Int = 30

    fun validRadius(value: Int?): Int =
        value?.takeIf { it in locationRadiiKm } ?: defaultRadiusKm

}
