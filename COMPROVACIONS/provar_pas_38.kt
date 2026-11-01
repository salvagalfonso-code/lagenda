package com.example.agendavalenciana.ios

// Isolated assertions against production IosPaidTicketPolicy and IosTicketCatalogPolicy.
data class IosTicket(val id: String, val eventId: String, val eventTitle: String, val status: String, val code: String)

fun main() {
    var assertions = 0
    fun ok(value: Boolean) { check(value); assertions++ }
    fun rejected(block: () -> Unit) {
        val result = runCatching(block)
        ok(result.isFailure)
    }
    val option = IosTicketOption(3,"Pista",15.0,11,8,"2027-03-12","19:00", "Pista")
    IosPaidTicketPolicy.requirePurchasable(7, option,6); assertions++
    rejected { IosPaidTicketPolicy.requirePurchasable(7, option,7) }
    rejected { IosPaidTicketPolicy.requirePurchasable(7, option.copy(price=0.0),1) }
    rejected { IosPaidTicketPolicy.requirePurchasable(7, option.copy(active=false),1) }
    rejected { IosPaidTicketPolicy.requirePurchasable(7,option.copy(remaining=0),1) }
    val start = IosTicketPaymentStart(55,"pi_123_secret_abc","pk_test_abc",1500,"eur",
        "l'Agenda","test",10,7,3,1)
    IosPaidTicketPolicy.requireValidStart(start); assertions++
    rejected { IosPaidTicketPolicy.requireValidStart(start.copy(orderId=0)) }
    rejected { IosPaidTicketPolicy.requireValidStart(start.copy(clientSecret="bad")) }
    rejected { IosPaidTicketPolicy.requireValidStart(start.copy(publishableKey="sk_test_abc")) }
    rejected { IosPaidTicketPolicy.requireValidStart(start.copy(mode="live")) }
    rejected { IosPaidTicketPolicy.requireValidStart(start.copy(amountCents=0)) }
    rejected { IosPaidTicketPolicy.requireValidStart(start.copy(currency="usd")) }
    rejected { IosPaidTicketPolicy.requireValidStart(start.copy(quantity=9)) }
    val ticket = IosTicket("1","7","Esdeveniment","ACTIVE","ABC123")
    val confirmation = IosTicketPaymentConfirmation(55,"paid",listOf(ticket))
    IosPaidTicketPolicy.requireConsistentResult(start,confirmation); assertions++
    rejected { IosPaidTicketPolicy.requireConsistentResult(start,confirmation.copy(orderId=56)) }
    rejected { IosPaidTicketPolicy.requireConsistentResult(start,confirmation.copy(tickets=emptyList())) }
    rejected { IosPaidTicketPolicy.requireConsistentResult(start,confirmation.copy(tickets=listOf(ticket.copy(eventId="8")))) }
    rejected { IosPaidTicketPolicy.requireConsistentResult(start,confirmation.copy(tickets=listOf(ticket.copy(code="")))) }
    rejected { IosPaidTicketPolicy.requireConsistentResult(start,confirmation.copy(status="payment_pending")) }
    IosPaidTicketPolicy.requireConsistentResult(start,IosTicketPaymentConfirmation(55,"payment_pending",emptyList())); assertions++
    ok(IosPaidTicketPolicy.isPaid("PAID"))
    ok(IosPaidTicketPolicy.isTerminalFailure("refunded"))
    ok(IosPaidTicketPolicy.isTerminalFailure("payment_failed"))
    ok(!IosPaidTicketPolicy.isPaid("payment_pending"))
    println("PAS 38: $assertions comprovacions PASS")
}
