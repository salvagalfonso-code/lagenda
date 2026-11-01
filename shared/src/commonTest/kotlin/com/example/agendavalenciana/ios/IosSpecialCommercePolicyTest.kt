package com.example.agendavalenciana.ios

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IosSpecialCommercePolicyTest {
    private val c = IosDonationConfig(true, "Suport", "", "", listOf(500,1000,2000),100,100000)
    private val product = IosServiceProduct(6,2,"Sessió", "", "", 300,true,5,true)
    private val service = IosSpecialService(2,7,"Skate", "", "", "#FFE082",true,listOf(product))
    @Test fun limits() {
        assertTrue(IosSpecialCommercePolicy.validConfig(c))
        assertFalse(IosSpecialCommercePolicy.validConfig(c.copy(minCents=5000)))
        assertTrue(IosSpecialCommercePolicy.validDonationAmount(c,100))
        assertFalse(IosSpecialCommercePolicy.validDonationAmount(c.copy(active=false),500))
        assertFalse(IosSpecialCommercePolicy.validDonationAmount(c,100001))
    }
    @Test fun donationsNeedPhpPayment() {
        val p = IosDonationPayment(5,16,"Grup",1000,"pi_abc_secret_xyz","pk_test_abc","l'Agenda","test")
        assertTrue(IosSpecialCommercePolicy.validDonationPayment(p))
        assertTrue(IosSpecialCommercePolicy.confirmedDonation(p,IosDonationReceipt(16,"paid",1000)))
        assertFalse(IosSpecialCommercePolicy.confirmedDonation(p,IosDonationReceipt(16,"pending",1000)))
        assertFalse(IosSpecialCommercePolicy.confirmedDonation(p,IosDonationReceipt(18,"paid",1000)))
        assertFalse(IosSpecialCommercePolicy.confirmedDonation(p,IosDonationReceipt(16,"paid",500)))
        assertFalse(IosSpecialCommercePolicy.validPayment("fake","pk_test_ok","test"))
        assertFalse(IosSpecialCommercePolicy.validPayment("pi_ok_secret_a","pk_test_ok","live"))
    }
    @Test fun reserveOnlyCanBePaidLikeAndroid() {
        assertTrue(IosSpecialCommercePolicy.canBuy(service,product,1))
        assertTrue(IosSpecialCommercePolicy.canBuy(service,product,5))
        assertFalse(IosSpecialCommercePolicy.canBuy(service,product,6))
        assertFalse(IosSpecialCommercePolicy.canBuy(service,product,0))
        assertFalse(IosSpecialCommercePolicy.canBuy(service.copy(active=false),product,1))
        assertFalse(IosSpecialCommercePolicy.canBuy(service,product.copy(active=false),1))
        assertFalse(IosSpecialCommercePolicy.canBuy(service,product.copy(serviceId=3),1))
    }
    @Test fun servicePaymentMustBeConfirmedInMariaDb() {
        val p = IosSpecialPayment(5,14,7,2,"pi_abc_secret_123","pk_test_123",600,"l'Agenda","test")
        assertTrue(IosSpecialCommercePolicy.validSpecialPayment(p))
        assertTrue(IosSpecialCommercePolicy.confirmedService(p,IosSpecialOrder(14,"paid","Skate",600,"qr_valid")))
        assertFalse(IosSpecialCommercePolicy.confirmedService(p,IosSpecialOrder(14,"paid","Skate",600,"")))
        assertFalse(IosSpecialCommercePolicy.confirmedService(p,IosSpecialOrder(14,"pending","Skate",600,"qr")))
        assertFalse(IosSpecialCommercePolicy.confirmedService(p,IosSpecialOrder(14,"paid","Skate",900,"qr")))
        assertFalse(IosSpecialCommercePolicy.confirmedService(p,IosSpecialOrder(15,"paid","Skate",600,"qr")))
    }
    @Test fun managementAndRedemption() {
        val before = listOf(service)
        assertTrue(IosSpecialCommercePolicy.serviceSaved(7,before, listOf(service.copy(name="Skate 2")),2,"Skate 2","","","#FFE082"))
        assertFalse(IosSpecialCommercePolicy.serviceSaved(7,before, before,2,"Skate 2","","","#FFE082"))
        assertTrue(IosSpecialCommercePolicy.productSaved(listOf(product),listOf(product.copy(priceCents=500)),product.copy(priceCents=500)))
        assertFalse(IosSpecialCommercePolicy.productSaved(listOf(product),listOf(product),product.copy(priceCents=500)))
        assertTrue(IosSpecialCommercePolicy.redeemAllowed(IosSpecialInspection(4,7,"paid","Skate",300)))
        assertFalse(IosSpecialCommercePolicy.redeemAllowed(IosSpecialInspection(4,7,"redeemed","Skate",300)))
        assertFalse(IosSpecialCommercePolicy.redeemAllowed(IosSpecialInspection(0,7,"paid","Skate",300)))
    }
}
