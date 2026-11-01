package com.example.agendavalenciana.model

/** Activa el feed personalitzat només amb seguiments o gustos seleccionats explícitament.
 * L'historial de visites, cerques, compres i els guardats no són un consentiment
 * per a convertir automàticament un feed sense filtres en Per a tu.
 */
object DefaultFeedPolicy {
    fun hasExplicitSignals(
        followedArtists: Set<String>,
        followedOrganizers: Set<String>,
        followedVenues: Set<String>,
        followedCategories: Set<String>,
        preferredLocations: Set<String>,
        preferredComarques: Set<String>
    ): Boolean = followedArtists.isNotEmpty() || followedOrganizers.isNotEmpty() ||
        followedVenues.isNotEmpty() || followedCategories.isNotEmpty() ||
        preferredLocations.isNotEmpty() || preferredComarques.isNotEmpty()
}
