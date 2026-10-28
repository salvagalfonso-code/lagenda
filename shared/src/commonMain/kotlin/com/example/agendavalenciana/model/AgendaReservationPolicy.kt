package com.example.agendavalenciana.model

/**
 * Reserva pròpia de L'Agenda: classificació de la disponibilitat d'entrades.
 * La venda o reserva NO dóna cap prioritat al feed «Per a tu».
 * Els anuncis contractats es mostren exclusivament en els seus espais propis.
 */
object AgendaReservationPolicy {
    /** Una reserva gratuïta interna no apareix a «Per a tu» (v282). */
    fun isFreeInternalAgendaReservation(
        ticketEnabled: Boolean, ticketPrice: Int, ticketUrl: String
    ): Boolean = ticketEnabled && ticketPrice == 0 && ticketUrl.isBlank()
}
