package com.example.agendavalenciana.ios

/** Pas 38: Stripe is just a payment UI. Tickets and paid status come ONLY from PHP/MariaDB. */
data class IosTicketPaymentStart(
    val orderId: Long, val clientSecret: String, val publishableKey: String,
    val amountCents: Long, val currency: String, val merchantName: String,
    val mode: String, val userId: Int, val eventId: Int, val optionId: Int, val quantity: Int
)

data class IosTicketPaymentConfirmation(
    val orderId: Long, val status: String, val tickets: List<IosTicket>
)

object IosPaidTicketPolicy {
    fun requirePurchasable(eventId: Int, option: IosTicketOption?, quantity: Int) {
        require(eventId > 0 && option != null && option.price > 0 &&
            IosTicketCatalogPolicy.selectable(option) &&
            quantity in 1..IosTicketCatalogPolicy.quantityLimit(option)) {
            "Entrada de pagament no disponible; refresca les modalitats."
        }
    }
    fun requireValidStart(start: IosTicketPaymentStart) {
        require(start.orderId > 0 && start.userId > 0 && start.eventId > 0 && start.optionId > 0 &&
            start.quantity in 1..6 && start.amountCents > 0 && start.currency.equals("eur",true) &&
            start.merchantName.isNotBlank() && start.merchantName.length <= 120 &&
            start.clientSecret.startsWith("pi_") && "_secret_" in start.clientSecret &&
            (start.publishableKey.startsWith("pk_test_") || start.publishableKey.startsWith("pk_live_")) &&
            (start.mode.isBlank() || start.mode.lowercase() in setOf("test","live"))) {
            "El servidor no ha retornat una sessió de pagament vàlida."
        }
        if (start.mode.equals("test",true)) require(start.publishableKey.startsWith("pk_test_")) {
            "Configuració de pagament incoherent: claus de prova i producció barrejades."
        }
        if (start.mode.equals("live",true)) require(start.publishableKey.startsWith("pk_live_")) {
            "Configuració de pagament incoherent: claus de prova i producció barrejades."
        }
    }
    fun isPaid(status: String) = status.equals("paid", true)
    fun isTerminalFailure(status: String) = status.lowercase() in setOf("payment_failed","cancelled","refunded")
    /** Do not trust success=true, PaymentSheet.completed, or a nonempty ticket list alone. */
    fun requireConsistentResult(start: IosTicketPaymentStart, answer: IosTicketPaymentConfirmation) {
        require(answer.orderId == start.orderId && answer.status.isNotBlank()) {
            "La resposta del servidor no correspon a la comanda."
        }
        if (isPaid(answer.status)) {
            require(answer.tickets.size == start.quantity && answer.tickets.all { t ->
                t.id.isNotBlank() && t.code.isNotBlank() && t.eventId == start.eventId.toString()
            } && answer.tickets.map { it.id }.toSet().size == start.quantity &&
                answer.tickets.map { it.code }.toSet().size == start.quantity) {
                "El servidor ha marcat el pagament però encara no ha lliurat totes les entrades. Revisa Entrades."
            }
        } else require(answer.tickets.isEmpty()) {
            "Les entrades no es poden considerar comprades sense una comanda pagada."
        }
    }
}
