package com.example.agendavalenciana.ios

fun main() {
    val p = IosProfessionalSellerPolicy
    var n = 0
    fun verify(condition: Boolean) { check(condition) { "fallada ${n+1}" }; n++ }
    listOf("0" to 0L, "10" to 1000L, "10,2" to 1020L, "10.23" to 1023L,
        "0003,50" to 350L, "999999999" to 99999999900L).forEach { (str, expected) ->
        verify(p.cents(str) == expected)
    }
    listOf("", "-1", "12,345", "0,", ".5", "1.2.3", "1e6", "  ", "1,2a", "1000000000").forEach {
        verify(p.cents(it) == null)
    }
    verify(p.euros(1234) == "12,34 €")
    verify(p.euros(0) == "0,00 €")
    verify(!p.validPeriod("none")); verify(p.validPeriod("7d"))
    val before = IosProTicketOption(1L, "General", 1250L, 100, 10, 5, 6, "2027-01-02", "21:00", "Pista", "", "", "", true)
    val draft = IosProTicketDraft(1, "General", 1250, 100, 6, "2027-01-02", "21:00", "Pista", "", "", "", true)
    verify(p.validOption(draft, before))
    verify(!p.validOption(draft.copy(capacity=14), before))
    verify(p.validOption(draft.copy(capacity=15), before))
    verify(!p.validOption(draft.copy(maxPerOrder=21), before))
    verify(!p.validOption(draft.copy(cents=-1), before))
    verify(!p.validOption(draft.copy(date="2027/01/02"), before))
    verify(!p.validOption(draft.copy(time="25:00"), before))
    verify(!p.validOption(draft.copy(starts="2027-01-03 20:00:00", ends="2027-01-02 20:00:00"), before))
    verify(p.validOption(draft.copy(starts="2027-01-01 20:00:00", ends="2027-01-03 20:00:00"), before))
    verify(!p.validOption(draft.copy(starts="2027-01-01 20:00:00", ends=""), before))
    verify(!p.validOption(draft.copy(id=2), before))
    verify(p.matches(draft, before))
    verify(!p.matches(draft.copy(name="VIP"), before))
    val catalog = IosProTicketCatalog(11,false,listOf(before))
    verify(p.verifiedUpsert(catalog, catalog, draft))
    verify(!p.verifiedUpsert(catalog, IosProTicketCatalog(12,false,listOf(before)), draft))
    val newDraft = draft.copy(id=0,name="VIP",cents=3000)
    val after = IosProTicketCatalog(11,false,listOf(before, before.copy(id=2,name="VIP",cents=3000)))
    verify(p.verifiedUpsert(catalog, after, newDraft))
    verify(!p.verifiedUpsert(catalog, catalog, newDraft))
    verify(!p.verifiedUpsert(catalog, after, newDraft,3))
    verify(p.verifiedUpsert(catalog, after, newDraft,2))
    val company = IosProCompany(5,"Afers", "B12345678", "C Major 1", "46001", "València", "València", "COMPANY")
    val event = IosProEvent(11,"Concert", "2027-01-02", "València", "published", false, 5, 4, 2, 1000)
    val sale = IosProSale(1,11,"Concert","2027-01-02",2,500,"General")
    val dash = IosProDashboard(listOf(company), listOf(event), listOf(sale), emptyList(),4,2,1000,false)
    verify(p.validSummary(dash))
    verify(p.canAssign(dash,11,5));verify(!p.canAssign(dash,11,6));verify(!p.canAssign(dash,12,5))
    verify(p.confirmedAssignment(dash,11,5)); verify(!p.confirmedAssignment(dash,11,6))
    verify(p.canRegisterInvoice(dash,5,"FA-1","2027-01-02",1000,true))
    verify(!p.canRegisterInvoice(dash,5,"FA-1","2027-01-02",1000,false))
    verify(!p.canRegisterInvoice(dash,10,"FA-1","2027-01-02",1000,true))
    verify(!p.canRegisterInvoice(dash,5,"", "2027-01-02",1000,true))
    verify(!p.canRegisterInvoice(dash,5,"FA-1", "2027/01/02",1000,true))
    val invoice = IosProInvoice("FA-1","Afers","2027-01-02",1000,"issued")
    verify(p.confirmedInvoice(dash,dash.copy(invoices=listOf(invoice)),"FA-1"))
    verify(!p.confirmedInvoice(dash,dash,"FA-1"))
    verify(!p.canRegisterInvoice(dash.copy(invoices=listOf(invoice)),5,"FA-1","2027-01-02",1000,true))
    verify(!p.validSummary(dash.copy(grossCents=-1)))
    verify(!p.validSummary(dash.copy(sales=listOf(sale.copy(quantity=0)))))
    verify(!p.validSummary(dash.copy(companies=listOf(company.copy(id=0)))))
    verify(!p.validSummary(dash.copy(events=listOf(event.copy(sold=-1)))))
    verify(p.reportCsv(dash).contains("1;\"Concert\";\"2027-01-02\";\"General\";2;5,00"))
    verify(!p.reportCsv(dash).contains("Stripe"))
    println("PAS43_POLICY_ASSERTIONS=$n PASS")
}
