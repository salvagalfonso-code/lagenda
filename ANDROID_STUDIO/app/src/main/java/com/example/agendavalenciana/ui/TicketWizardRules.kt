package com.example.agendavalenciana.ui

/** El servidor actual només publica opcions, preus, places per lot i finestres de venda. */
internal fun unsupportedTicketFeatures(
    hasExtras: Boolean,
    hasDayCaps: Boolean,
    hasSelloutRule: Boolean
): List<String> = buildList {
    if (hasExtras) add("productes extres")
    if (hasDayCaps) add("aforaments per jornada")
    if (hasSelloutRule) add("activacions per esgotament")
}
