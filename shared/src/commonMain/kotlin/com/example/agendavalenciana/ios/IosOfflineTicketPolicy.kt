package com.example.agendavalenciana.ios

/** Only backend-issued QR codes may be projected offline. No offline redemption. */
object IosOfflineTicketPolicy {
    fun requireValid(items: List<IosTicket>) {
        require(items.size <= 10_000) { "Massa entrades per a la còpia offline." }
        require(items.all { ticket ->
            ticket.id.isNotBlank() && ticket.id.length <= 80 &&
            ticket.eventId.toIntOrNull()?.let { it > 0 } == true &&
            ticket.code.isNotBlank() && ticket.code.length <= 256 &&
            ticket.code.none { it.isISOControl() } &&
            ticket.status.uppercase() in setOf("ACTIVE", "USED", "CANCELLED") &&
            ticket.eventTitle.length <= 500 && ticket.ticketOptionName.length <= 400 &&
            ticket.sessionDate.length <= 64 && ticket.sessionTime.length <= 64 &&
            ticket.sector.length <= 400 && ticket.offer.length <= 400
        }) { "El servidor ha retornat entrades incompletes o invàlides." }
        require(items.map { it.id }.distinct().size == items.size &&
            items.map { it.code }.distinct().size == items.size) {
            "La resposta té entrades duplicades."
        }
    }
    fun displayStatus(status: String): String = when (status.uppercase()) {
        "ACTIVE" -> "Activa"
        "USED" -> "Utilitzada"
        "CANCELLED" -> "Anul·lada"
        else -> "Estat desconegut"
    }
    fun details(ticket: IosTicket): String = listOf(ticket.ticketOptionName, ticket.sessionDate,
        ticket.sessionTime, ticket.sector, ticket.offer).filter { it.isNotBlank() }.joinToString(" · ")
}
