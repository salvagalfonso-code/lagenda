package com.example.agendavalenciana.ios

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IosTicketCatalogPolicyTest {
    private val vip = IosTicketOption(11,"Pista anticipada",12.0,null,20,"2026-12-04","20:30",
        sector="Pista",offer="100 primeres",capacity=100,sold=76,reserved=19)
    @Test fun stockAndQuantityAreServerBased() {
        assertEquals(5, vip.available)
        assertEquals(5, IosTicketCatalogPolicy.quantityLimit(vip))
        assertEquals(6, IosTicketCatalogPolicy.quantityLimit(vip.copy(capacity=50,sold=0,reserved=0)))
        assertEquals(0, IosTicketCatalogPolicy.quantityLimit(vip.copy(capacity=95)))
        assertFalse(IosTicketCatalogPolicy.selectable(vip.copy(active=false)))
    }
    @Test fun dayZoneAndOfferAreCombinable() {
        val gen = vip.copy(id=12, name="Abonament", sessionDate="", sector="", offer="")
        val other = vip.copy(id=13, sector="Grada", sessionDate="2026-12-05")
        val available = IosTicketCatalogPolicy.visible(listOf(vip,gen,other),"2026-11-01")
        assertEquals(listOf("2026-12-04","2026-12-05"), IosTicketCatalogPolicy.days(available))
        assertEquals(listOf("Grada", "Pista"), IosTicketCatalogPolicy.sectors(available))
        assertEquals(setOf(11,12), IosTicketCatalogPolicy.filter(available,"2026-12-04","Pista").map { it.id }.toSet())
        assertTrue(IosTicketCatalogPolicy.details(vip).contains("100 primeres"))
    }
    @Test fun invalidOrInactiveOptionsAreNotAvailable() {
        assertFalse(IosTicketCatalogPolicy.valid(vip.copy(price=Double.NaN)))
        assertFalse(IosTicketCatalogPolicy.valid(vip.copy(id=0)))
        assertFalse(IosTicketCatalogPolicy.valid(vip.copy(maxPerOrder=21)))
        assertTrue(IosTicketCatalogPolicy.visible(listOf(vip.copy(active=false)),"2026-11-01").isEmpty())
        assertEquals(0,IosTicketCatalogPolicy.quantityLimit(null))
    }
}
