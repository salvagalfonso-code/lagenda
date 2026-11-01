package com.example.agendavalenciana.ios

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch

/** Pas 45: compra nativa Stripe per clients registrats; PHP confirma el pagament.
 * Dades d'enviament únicament en RAM fins a enviar-les al servidor. */
@Composable
internal fun IosArtistStoreCheckout(api: IosAgendaApi, product: IosArtistProduct,
    onClose: () -> Unit, onLogin: () -> Unit, onPaid: () -> Unit) {
    val scope = rememberCoroutineScope()
    var quantity by remember(product.id) { mutableIntStateOf(1) }
    var recipient by remember(product.id) { mutableStateOf("") }
    var street by remember(product.id) { mutableStateOf("") }
    var postcode by remember(product.id) { mutableStateOf("") }
    var city by remember(product.id) { mutableStateOf("") }
    var country by remember(product.id) { mutableStateOf("Espanya") }
    var busy by remember(product.id) { mutableStateOf(false) }
    var paymentUI by remember(product.id) { mutableStateOf(false) }
    var error by remember(product.id) { mutableStateOf("") }
    var info by remember(product.id) { mutableStateOf("") }
    var pending by remember(product.id) { mutableStateOf(api.pendingArtistStoreOrder()) }
    var unresolved by remember(product.id) { mutableStateOf(api.artistStoreCreationUncertain()) }

    fun verify(orderId: Long) {
        if (busy || paymentUI) return
        busy = true; error = ""; info = ""
        scope.launch {
            try {
                val result = api.artistStoreStatus(orderId)
                if (result.status.equals("paid", true)) {
                    val known = api.pendingArtistStoreOrder()
                    check(known != null && IosArtistStorePolicy.paidConfirmed(known, result)) {
                        "La resposta no confirma exactament la compra iniciada."
                    }
                    api.clearArtistStoreOrder(orderId)
                    pending = null; info = "Compra confirmada pel servidor · ${result.quantity} × ${result.product}."
                    onPaid()
                } else {
                    info = "Estat al servidor: ${result.status}. Encara no es considera pagada."
                    if (result.status.lowercase() in setOf("cancelled", "canceled", "payment_failed", "failed")) {
                        api.clearArtistStoreOrder(orderId); pending = null
                    }
                }
            } catch (ex: Exception) { error = ex.message ?: "No s'ha pogut verificar el pagament." }
            finally { busy = false }
        }
    }
    fun present(payment: IosStorePayment) {
        if (paymentUI || busy) return
        val bridge = IosNativeBridge.current ?: run { error = "Stripe no està disponible en iPhone."; return }
        paymentUI = true; error = ""; info = ""
        try {
            bridge.presentTicketPayment(payment.clientSecret, payment.publicKey, payment.merchant,
                object : IosTicketPaymentCallback {
                    override fun onComplete(result: String, error: String) {
                        paymentUI = false
                        if (api.activePersonalId() != payment.ownerId) return
                        when (result) {
                            "COMPLETED" -> verify(payment.orderId)
                            "CANCELLED" -> info = "Pagament interromput. La comanda pot continuar pendent; comprova-la o reprén-la."
                            else -> info = error.ifBlank { "Stripe no ha completat el pagament." } +
                                " Consulta la comanda abans de repetir el cobrament."
                        }
                    }
                })
        } catch (ex: Exception) { paymentUI = false; error = ex.message ?: "Error obrint Stripe." }
    }
    Dialog(onDismissRequest = { if (!busy && !paymentUI) onClose() }) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxWidth().heightIn(max = 680.dp).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Text(product.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(enabled = !busy && !paymentUI, onClick = onClose) { Text("Tancar") }
                }
                Text("Preu: ${IosArtistPagePolicy.money(product.priceCents)} · Enviament: ${IosArtistPagePolicy.money(product.shippingCents)}")
                if (!api.hasRegisteredSessionToken()) {
                    Text("Cal iniciar sessió per a comprar. La botiga es manté al servidor.")
                    Button(onClick = onLogin) { Text("Iniciar sessió o crear compte") }
                } else {
                    val remaining = product.stock ?: 20
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(enabled = quantity > 1 && !busy && pending == null, onClick = { quantity-- }) { Text("−") }
                        Text("$quantity unitats", modifier = Modifier.padding(top = 12.dp))
                        OutlinedButton(enabled = quantity < minOf(20, remaining) && !busy && pending == null,
                            onClick = { quantity++ }) { Text("+") }
                    }
                    Text("Subtotal orientatiu: ${IosArtistPagePolicy.money(product.priceCents * quantity + product.shippingCents)}. El total definitiu, inclosa la gestió, el confirma Stripe des del servidor.", style = MaterialTheme.typography.bodySmall)
                    if (pending == null) {
                        OutlinedTextField(recipient, { recipient = it }, label = { Text("Nom i cognoms") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(street, { street = it }, label = { Text("Adreça d'enviament") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(postcode, { postcode = it }, label = { Text("Codi postal") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(city, { city = it }, label = { Text("Població") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(country, { country = it }, label = { Text("País") }, modifier = Modifier.fillMaxWidth())
                    }
                    if (busy || paymentUI) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    if (info.isNotBlank()) Text(info, style = MaterialTheme.typography.bodySmall)
                    if (pending != null) {
                        Text("Comanda #${pending!!.orderId} · Import del servidor: ${IosArtistPagePolicy.money(pending!!.amountCents)}")
                        if (pending!!.productId != product.id) {
                            Text("Esta comanda és d'un altre producte. Consulta-la abans de començar-ne una altra.",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            OutlinedButton(enabled = !busy && !paymentUI, onClick = { verify(pending!!.orderId) }) { Text("Comprovar") }
                            Button(enabled = !busy && !paymentUI && pending!!.productId == product.id,
                                onClick = { present(pending!!) }) { Text("Reprendre Stripe") }
                        }
                    } else if (unresolved) {
                        Text("No sabem si el servidor ha creat una comanda. Consulta el teu compte abans de fer una compra nova.",
                            color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        TextButton(enabled = !busy, onClick = {
                            api.acknowledgeUncertainArtistOrder(); unresolved = false
                            info = "Reintentar podria crear una comanda duplicada."
                        }) { Text("He comprovat el servidor; permetre nou intent") }
                    } else {
                        Button(enabled = !busy && !paymentUI &&
                            IosArtistStorePolicy.canBuy(product, quantity) &&
                            IosArtistStorePolicy.validAddress(IosStoreAddress(recipient, street, postcode, city, country)),
                            onClick = {
                                busy = true; error = ""; info = ""
                                scope.launch {
                                    try {
                                        val created = api.artistStoreCreatePayment(product, quantity,
                                            IosStoreAddress(recipient, street, postcode, city, country))
                                        pending = created
                                        busy = false
                                        present(created)
                                    } catch (ex: Exception) {
                                        error = ex.message ?: "No s'ha confirmat la creació de la comanda."
                                        unresolved = api.artistStoreCreationUncertain()
                                        busy = false
                                    }
                                }
                            }) { Text("Continuar a Stripe") }
                    }
                }
            }
        }
    }
}

/** Només artistes retornats com a gestionats per professional-profiles/mine.php. */
@Composable
internal fun IosArtistStoreManager(api: IosAgendaApi, ownerId: Int, onNotice: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var artists by remember(ownerId) { mutableStateOf(emptyList<String>()) }
    var artist by remember(ownerId) { mutableStateOf("") }
    var products by remember(ownerId, artist) { mutableStateOf(emptyList<IosArtistProduct>()) }
    var orders by remember(ownerId, artist) { mutableStateOf(emptyList<IosStoreOrder>()) }
    var section by remember(ownerId) { mutableStateOf("products") }
    var editing by remember(ownerId) { mutableStateOf<IosArtistProduct?>(null) }
    var adding by remember(ownerId) { mutableStateOf(false) }
    var name by remember(ownerId) { mutableStateOf("") }
    var type by remember(ownerId) { mutableStateOf("MERCH") }
    var description by remember(ownerId) { mutableStateOf("") }
    var imageUrl by remember(ownerId) { mutableStateOf("") }
    var price by remember(ownerId) { mutableStateOf("0") }
    var shipping by remember(ownerId) { mutableStateOf("0") }
    var stock by remember(ownerId) { mutableStateOf("") }
    var busy by remember(ownerId) { mutableStateOf(false) }
    var error by remember(ownerId) { mutableStateOf("") }
    var uncertain by remember(ownerId) { mutableStateOf(false) }
    var revision by remember(ownerId) { mutableIntStateOf(0) }
    fun refresh() { revision++ }
    fun edit(p: IosArtistProduct?) {
        editing = p; adding = true; name = p?.name.orEmpty(); type = p?.type ?: "MERCH"
        description = p?.description.orEmpty(); imageUrl = p?.imageUrl.orEmpty()
        price = IosProfessionalSellerPolicy.euros(p?.priceCents ?: 0L).removeSuffix(" €")
        shipping = IosProfessionalSellerPolicy.euros(p?.shippingCents ?: 0L).removeSuffix(" €")
        stock = p?.stock?.toString().orEmpty(); error = ""
    }
    fun operation(block: suspend () -> Unit) {
        if (busy || uncertain) return
        busy = true; error = ""
        scope.launch {
            try {
                block()
                check(api.activePersonalId() == ownerId) { "La sessió ha canviat." }
                refresh()
            } catch (ex: Exception) {
                uncertain = true
                error = (ex.message ?: "No s'ha confirmat l'operació.") +
                    " Consulta el servidor abans de tornar-la a fer."
            } finally { busy = false }
        }
    }
    LaunchedEffect(ownerId) {
        try {
            artists = api.managedArtistStoreNames()
            if (api.activePersonalId() == ownerId) artist = artists.firstOrNull().orEmpty()
        } catch (ex: Exception) { error = ex.message ?: "No es poden carregar els perfils gestionats." }
    }
    LaunchedEffect(ownerId, artist, section, revision) {
        if (artist.isBlank()) return@LaunchedEffect
        try {
            if (section == "orders") orders = api.artistStoreOrders(artist)
            else products = api.artistStoreCatalog(artist)
        } catch (ex: Exception) { error = ex.message ?: "No es poden consultar les dades de la botiga." }
    }
    Text("Botiga d'artistes · productes i comandes", fontWeight = FontWeight.Bold)
    if (artists.isEmpty()) {
        Text("No consten artistes gestionats en el servidor. Cal que el perfil professional tinga la reclamació aprovada.", style = MaterialTheme.typography.bodySmall)
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        return
    }
    artists.forEach { candidate ->
        FilterChip(selected = artist == candidate, onClick = {
            artist = candidate; adding = false; section = "products"; error = ""
        }, label = { Text(candidate) })
    }
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        OutlinedButton(enabled = !busy, onClick = { section = "products" }) { Text("Productes") }
        OutlinedButton(enabled = !busy, onClick = { section = "orders" }) { Text("Comandes") }
        TextButton(enabled = !busy, onClick = { uncertain = false; refresh() }) { Text("Actualitzar") }
    }
    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    if (section == "products") {
        if (!adding) {
            Button(enabled = !busy && !uncertain, onClick = { edit(null) }) { Text("+ Producte") }
            if (products.isEmpty()) Text("Encara no hi ha productes publicats.")
            products.forEach { p ->
                Text("${p.name} · ${IosArtistPagePolicy.money(p.priceCents)} · Estoc: ${p.stock?.toString() ?: "Il·limitat"}")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(enabled = !busy && !uncertain, onClick = { edit(p) }) { Text("Editar") }
                    OutlinedButton(enabled = !busy && !uncertain, onClick = {
                        operation {
                            products = api.artistStoreDelete(artist, p.id)
                            onNotice("Producte eliminat i verificat al servidor.")
                        }
                    }) { Text("Eliminar") }
                }
                HorizontalDivider()
            }
        } else {
            Text(if (editing == null) "Nou producte" else "Modificar producte #${editing!!.id}")
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                FilterChip(selected = type == "MERCH", onClick = { type = "MERCH" }, label = { Text("Merch") })
                FilterChip(selected = type == "DISC", onClick = { type = "DISC" }, label = { Text("Disc") })
            }
            OutlinedTextField(name, { name = it }, label = { Text("Nom") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(description, { description = it }, label = { Text("Descripció") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(imageUrl, { imageUrl = it }, label = { Text("URL imatge HTTPS") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(price, { price = it }, label = { Text("Preu (€)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(shipping, { shipping = it }, label = { Text("Enviament (€)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(stock, { stock = it.filter(Char::isDigit) }, label = { Text("Estoc, buit = il·limitat") }, modifier = Modifier.fillMaxWidth())
            val draft = IosStoreDraft(editing?.id ?: 0L, artist, name, type, description, imageUrl,
                IosProfessionalSellerPolicy.cents(price) ?: -1L, IosProfessionalSellerPolicy.cents(shipping) ?: -1L,
                if (stock.isBlank()) null else stock.toIntOrNull() ?: -1)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(enabled = !busy && !uncertain && IosArtistStorePolicy.validDraft(draft), onClick = {
                    operation {
                        products = api.artistStoreSave(draft)
                        adding = false; editing = null
                        onNotice("Producte confirmat al servidor.")
                    }
                }) { Text("Guardar") }
                TextButton(enabled = !busy, onClick = { adding = false; editing = null; uncertain = false }) { Text("Cancel·lar") }
            }
        }
    } else {
        if (orders.isEmpty()) Text("Encara no hi ha comandes al servidor.")
        orders.forEach { order ->
            Text("#${order.id} · ${order.quantity} × ${order.product} · ${IosArtistPagePolicy.money(order.totalCents)}", fontWeight = FontWeight.SemiBold)
            Text("${order.status} · ${order.createdAt}", style = MaterialTheme.typography.bodySmall)
            Text("${order.recipient} · ${order.address} · ${order.postcode} ${order.city} · ${order.country}", style = MaterialTheme.typography.bodySmall)
            if (order.status.equals("paid",true)) Button(enabled = !busy && !uncertain, onClick = {
                operation { orders = api.artistStoreShip(artist, order.id); onNotice("Enviament confirmat pel servidor.") }
            }) { Text("Marcar com enviat") }
            HorizontalDivider()
        }
    }
}
