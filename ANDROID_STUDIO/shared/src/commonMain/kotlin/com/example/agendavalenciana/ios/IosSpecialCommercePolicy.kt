package com.example.agendavalenciana.ios

/** Pas 46: el cobrament i els permisos són sempre del servidor. Cap dada persistent al telèfon. */
internal data class IosDonationConfig(val active: Boolean, val title: String, val description: String,
    val imageUrl: String, val suggested: List<Long>, val minCents: Long, val maxCents: Long)
internal data class IosDonationPayment(val ownerId: Int, val donationId: Long, val artist: String,
    val amountCents: Long, val secret: String, val key: String, val merchant: String, val mode: String)
internal data class IosDonationReceipt(val id: Long, val status: String, val amountCents: Long)
internal data class IosServiceProduct(val id: Long, val serviceId: Long, val name: String,
    val description: String, val imageUrl: String, val priceCents: Long,
    val reserveOnly: Boolean, val stock: Int?, val active: Boolean)
internal data class IosSpecialService(val id: Long, val eventId: Int, val name: String,
    val description: String, val imageUrl: String, val color: String,
    val active: Boolean, val products: List<IosServiceProduct>)
internal data class IosSpecialEvent(val id: Int, val title: String, val enabled: Boolean,
    val canEditServices: Boolean, val services: List<IosSpecialService>)
internal data class IosSpecialEventAccess(val id: Int, val title: String, val enabled: Boolean)
internal data class IosSpecialPayment(val ownerId: Int, val orderId: Long, val eventId: Int,
    val serviceId: Long, val secret: String, val key: String, val amountCents: Long,
    val merchant: String, val mode: String)
internal data class IosSpecialOrder(val id: Long, val status: String, val serviceName: String,
    val totalCents: Long, val qrPayload: String)
internal data class IosSpecialInspection(val orderId: Long, val eventId: Int, val status: String,
    val serviceName: String, val totalCents: Long)

internal object IosSpecialCommercePolicy {
    fun validImage(image: String) = image.isBlank() ||
        (image.length <= 2048 && !image.any(Char::isWhitespace) &&
        (image.startsWith("https://") || image.startsWith("/") &&
            !image.startsWith("//") && !image.contains("..")))
    fun validConfig(c: IosDonationConfig): Boolean = validImage(c.imageUrl) && c.title.length <= 180 &&
        c.description.length <= 4000 && c.minCents in 100..100000 &&
        c.maxCents in c.minCents..100000 && c.suggested.size in 1..8 &&
        c.suggested.all { it in c.minCents..c.maxCents }
    fun validDonationAmount(c: IosDonationConfig, cents: Long): Boolean =
        c.active && cents in c.minCents..c.maxCents
    fun validPayment(secret: String, key: String, mode: String) =
        secret.startsWith("pi_") && "_secret_" in secret &&
        mode in setOf("test", "live") && key.startsWith(if (mode == "live") "pk_live_" else "pk_test_")
    fun validDonationPayment(p: IosDonationPayment) = p.ownerId > 0 && p.donationId > 0 &&
        p.artist.isNotBlank() && p.amountCents > 0 && validPayment(p.secret, p.key, p.mode)
    fun confirmedDonation(p: IosDonationPayment, r: IosDonationReceipt) =
        validDonationPayment(p) && p.donationId == r.id && r.status.equals("paid", true) &&
        r.amountCents == p.amountCents
    fun canBuy(s: IosSpecialService, p: IosServiceProduct, n: Int) =
        s.active && s.id > 0 && p.active && p.id > 0 && p.serviceId == s.id &&
        p.priceCents >= 0 && n in 1..99 && (p.stock == null || n <= p.stock)
    fun validSpecialPayment(p: IosSpecialPayment) = p.ownerId > 0 && p.orderId > 0 &&
        p.eventId > 0 && p.serviceId > 0 && p.amountCents > 0 &&
        validPayment(p.secret, p.key, p.mode)
    fun confirmedService(p: IosSpecialPayment, order: IosSpecialOrder) =
        validSpecialPayment(p) && p.orderId == order.id && order.status.equals("paid", true) &&
        order.totalCents == p.amountCents && order.qrPayload.isNotBlank()
    fun validServiceName(name: String) = name.trim().length in 1..180
    fun validProduct(p: IosServiceProduct) = p.id >= 0 && p.serviceId > 0 &&
        validServiceName(p.name) && validImage(p.imageUrl) && p.description.length <= 4000 &&
        p.priceCents in 0..99_999_999 && (p.stock == null || p.stock >= 0)
    fun serviceSaved(eventId: Int, old: List<IosSpecialService>, fresh: List<IosSpecialService>,
        id: Long, name: String, desc: String, image: String, color: String): Boolean =
        eventId > 0 && validServiceName(name) && validImage(image) &&
        (if (id > 0) old.any { it.id == id } &&
            fresh.count { it.id == id && it.eventId == eventId && it.active &&
                it.name == name && it.description == desc && it.imageUrl == image && it.color == color } == 1
         else fresh.count { it.id > 0 && it.eventId == eventId && it.active &&
            it.name == name && it.description == desc && it.imageUrl == image && it.color == color &&
            old.none { o -> o.id == it.id } } == 1)
    fun productSaved(old: List<IosServiceProduct>, fresh: List<IosServiceProduct>, p: IosServiceProduct): Boolean =
        validProduct(p) && (if (p.id > 0) old.any { it.id == p.id } &&
            fresh.count { it.id == p.id && it.active && it.serviceId == p.serviceId &&
                it.name == p.name && it.description == p.description && it.imageUrl == p.imageUrl &&
                it.priceCents == p.priceCents && it.reserveOnly == p.reserveOnly && it.stock == p.stock } == 1
         else fresh.count { it.id > 0 && it.active && it.serviceId == p.serviceId &&
            it.name == p.name && it.description == p.description && it.imageUrl == p.imageUrl &&
            it.priceCents == p.priceCents && it.reserveOnly == p.reserveOnly && it.stock == p.stock &&
            old.none { prev -> prev.id == it.id } } == 1)
    fun redeemAllowed(i: IosSpecialInspection): Boolean = i.orderId > 0 && i.eventId > 0 &&
        i.status.equals("paid", true)
}
