package com.example.agendavalenciana.ios

/** Isolated compile of the REAL iOS offline-ticket policy with its IosTicket model stub. */
fun main() {
    var assertions = 0
    fun yes(value: Boolean) { check(value); assertions++ }
    fun rejected(vararg input: IosTicket) {
        yes(runCatching { IosOfflineTicketPolicy.requireValid(input.toList()) }.isFailure)
    }
    val a = IosTicket("51", "8", "Festival", "ACTIVE", "QR-CODE-VALID",
        "General", "2027-03-13", "19:30", "Pista", "Jove")
    val b = IosTicket("52", "8", "Festival", "USED", "QR-CODE-USED", validatedAt = "2027-03-13 19:40")
    val c = IosTicket("53", "10", "Teatre", "CANCELLED", "QR-CODE-CANCELLED")
    IosOfflineTicketPolicy.requireValid(listOf(a,b,c)); assertions++
    IosOfflineTicketPolicy.requireValid(emptyList()); assertions++ // authoritative zero tickets allowed
    yes(IosOfflineTicketPolicy.displayStatus("ACTIVE") == "Activa")
    yes(IosOfflineTicketPolicy.displayStatus("USED") == "Utilitzada")
    yes(IosOfflineTicketPolicy.displayStatus("CANCELLED") == "Anul·lada")
    yes(IosOfflineTicketPolicy.displayStatus("future") == "Estat desconegut")
    yes(IosOfflineTicketPolicy.details(a) == "General · 2027-03-13 · 19:30 · Pista · Jove")
    yes(IosOfflineTicketPolicy.details(b).isEmpty())
    rejected(a.copy(id=""))
    rejected(a.copy(id=" "))
    rejected(a.copy(eventId="0"))
    rejected(a.copy(eventId="x"))
    rejected(a.copy(code=""))
    rejected(a.copy(code="\r"))
    rejected(a.copy(code="ab\ncd"))
    rejected(a.copy(code="A".repeat(257)))
    rejected(a.copy(status="PAYMENT_PENDING"))
    rejected(a.copy(status="FAKE"))
    rejected(a.copy(eventTitle="x".repeat(501)))
    rejected(a.copy(sector="a".repeat(401)))
    rejected(a.copy(sessionDate="x".repeat(65)))
    rejected(a, a)
    rejected(a, a.copy(id="99")) // duplicate exact QR code
    rejected(a, a.copy(code="different")) // duplicate ticket id
    rejected(a, a.copy(id="99",code="different",eventId=""))
    yes(a.status == "ACTIVE" && b.status == "USED")
    println("PAS39_KOTLIN_POLICY_PASS: $assertions assertions")
}
