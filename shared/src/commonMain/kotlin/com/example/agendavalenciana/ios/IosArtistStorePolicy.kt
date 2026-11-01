package com.example.agendavalenciana.ios

/** Pas 45. Els catàlegs, l'estoc, el preu final, els pagaments i les comandes
 * provenen del servidor PHP, mai d'un estat persistent de l'iPhone. */
internal data class IosStoreDraft(
    val id: Long, val artist: String, val name: String, val type: String,
    val description: String, val imageUrl: String, val priceCents: Long,
    val shippingCents: Long, val stock: Int?
)
internal data class IosStoreAddress(
    val recipient: String, val street: String, val postcode: String,
    val city: String, val country: String
)
internal data class IosStorePayment(
    val orderId: Long, val productId: Long, val ownerId: Int,
    val clientSecret: String, val publicKey: String,
    val amountCents: Long, val currency: String, val merchant: String, val mode: String
)
internal data class IosStorePaidOrder(
    val id: Long, val status: String, val product: String, val quantity: Int,
    val productCents: Long, val shippingCents: Long, val totalCents: Long
)
internal data class IosStoreOrder(
    val id: Long, val product: String, val quantity: Int, val totalCents: Long,
    val status: String, val recipient: String, val address: String,
    val postcode: String, val city: String, val country: String, val createdAt: String
)
internal object IosArtistStorePolicy {
    fun validArtist(name: String) = name.trim().length in 1..190
    fun validDraft(d: IosStoreDraft): Boolean = d.id >= 0 && validArtist(d.artist) &&
        d.name.trim().length in 1..180 && d.type in setOf("DISC", "MERCH") &&
        d.description.length <= 4000 && d.priceCents in 0..99_999_999 &&
        d.shippingCents in 0..99_999_999 && (d.stock == null || d.stock >= 0) &&
        (d.imageUrl.isBlank() || IosArtistPagePolicy.imageUrl(d.imageUrl) == d.imageUrl)
    fun sameDraft(d: IosStoreDraft, p: IosArtistProduct) =
        IosArtistPagePolicy.sameArtist(d.artist, p.artist) && d.name.trim() == p.name &&
        d.type == p.type && d.description.trim() == p.description &&
        d.imageUrl.trim() == p.imageUrl && d.priceCents == p.priceCents &&
        d.shippingCents == p.shippingCents && d.stock == p.stock && p.active
    fun saved(d: IosStoreDraft, before: List<IosArtistProduct>, after: List<IosArtistProduct>,
              acknowledgedId: Long): Boolean {
        if (!validDraft(d)) return false
        if (d.id > 0) return before.any { it.id == d.id } &&
            after.count { it.id == d.id && sameDraft(d,it) } == 1
        val ids = before.map { it.id }.toSet()
        return after.count { it.id > 0 && it.id !in ids &&
            (acknowledgedId <= 0 || it.id == acknowledgedId) && sameDraft(d,it) } == 1
    }
    fun deleted(id: Long, before: List<IosArtistProduct>, after: List<IosArtistProduct>): Boolean =
        id > 0 && before.any { it.id == id } && after.none { it.id == id && it.active }
    fun canBuy(product: IosArtistProduct, quantity: Int): Boolean = product.id > 0 &&
        product.active && quantity in 1..20 && quantity <= (product.stock ?: 20) &&
        product.priceCents >= 0 && product.shippingCents >= 0
    fun validAddress(a: IosStoreAddress): Boolean =
        a.recipient.trim().length in 2..190 && a.street.trim().length in 4..250 &&
        a.postcode.trim().length in 2..20 && a.city.trim().length in 2..120 &&
        a.country.trim().length in 2..120
    fun validPayment(p: IosStorePayment): Boolean = p.orderId > 0 && p.productId > 0 && p.ownerId > 0 &&
        p.clientSecret.startsWith("pi_") && "_secret_" in p.clientSecret &&
        p.publicKey.startsWith(if (p.mode == "live") "pk_live_" else "pk_test_") &&
        p.mode in setOf("test","live") && p.currency.equals("eur", true) &&
        p.amountCents > 0 && p.merchant.isNotBlank()
    fun paidConfirmed(p: IosStorePayment, order: IosStorePaidOrder): Boolean =
        validPayment(p) && p.orderId == order.id && order.status.equals("paid", true) &&
        order.quantity in 1..20 && order.product.isNotBlank() &&
        order.productCents >= 0 && order.shippingCents >= 0 && order.totalCents >= 0
    fun confirmedShipped(id: Long, before: List<IosStoreOrder>, after: List<IosStoreOrder>): Boolean =
        before.count { it.id == id && it.status.equals("paid", true) } == 1 &&
        after.count { it.id == id && it.status.equals("shipped", true) } == 1
}
