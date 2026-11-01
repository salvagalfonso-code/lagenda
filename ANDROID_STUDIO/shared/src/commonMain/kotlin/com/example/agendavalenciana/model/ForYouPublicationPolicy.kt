package com.example.agendavalenciana.model

/** Evita que el resultat provisional d'un refresc sobreescriga el resultat definitiu. */
object ForYouPublicationPolicy {
    fun <T> choose(prepared: List<T>, computed: List<T>, computedIsCurrent: Boolean): List<T> =
        if (computedIsCurrent) computed else prepared
}
