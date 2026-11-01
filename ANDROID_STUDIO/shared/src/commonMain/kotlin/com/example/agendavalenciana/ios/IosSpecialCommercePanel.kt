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

/** Pas 46: gestor de donacions de grup, amb configuració llegida i comprovada al PHP. */
@Composable
internal fun IosArtistDonationManager(api: IosAgendaApi, ownerId: Int, onNotice: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var artists by remember(ownerId) { mutableStateOf(emptyList<String>()) }
    var selected by remember(ownerId) { mutableStateOf("") }
    var cfg by remember(ownerId,selected) { mutableStateOf<IosDonationConfig?>(null) }
    var active by remember(ownerId,selected) { mutableStateOf(false) }
    var title by remember(ownerId,selected) { mutableStateOf("") }
    var description by remember(ownerId,selected) { mutableStateOf("") }
    var image by remember(ownerId,selected) { mutableStateOf("") }
    var amounts by remember(ownerId,selected) { mutableStateOf("") }
    var busy by remember(ownerId) { mutableStateOf(false) }
    var uncertain by remember(ownerId) { mutableStateOf(false) }
    var message by remember(ownerId) { mutableStateOf("") }
    var revision by remember(ownerId) { mutableIntStateOf(0) }
    LaunchedEffect(ownerId) {
        runCatching { api.managedArtistStoreNames() }.onSuccess {
            if (api.activePersonalId() == ownerId) { artists = it; selected = it.firstOrNull().orEmpty() }
        }.onFailure { message = it.message.orEmpty() }
    }
    LaunchedEffect(ownerId, selected, revision) {
        if (selected.isNotBlank()) runCatching { api.artistDonationConfig(selected) }.onSuccess { current ->
            if (api.activePersonalId() == ownerId) {
                cfg = current; active = current.active; title = current.title
                description = current.description; image = current.imageUrl
                amounts = current.suggested.joinToString(",") { (it / 100.0).toString() }
            }
        }.onFailure { message = it.message.orEmpty() }
    }
    Text("Donacions dels artistes gestionats", fontWeight = FontWeight.Bold)
    if (artists.isEmpty()) Text("No hi ha grups gestionats confirmats pel servidor.")
    artists.forEach { artist -> FilterChip(selected == artist, onClick = { selected = artist; uncertain = false; message = "" }, label = { Text(artist) }) }
    if (cfg != null) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Activar donacions")
            Switch(checked = active, onCheckedChange = { active = it }, enabled = !busy)
        }
        OutlinedTextField(title, { title = it }, label = { Text("Títol") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(description, { description = it }, label = { Text("Descripció") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(image, { image = it }, label = { Text("Imatge URL") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(amounts, { amounts = it }, label = { Text("Imports suggerits (€), separats per comes") }, modifier = Modifier.fillMaxWidth())
        val cents = amounts.split(",").mapNotNull { IosProfessionalSellerPolicy.cents(it.trim()) }
        val original = cfg!!
        val draft = IosDonationConfig(active, title.trim(), description.trim(), image.trim(), cents,
            original.minCents, original.maxCents)
        Button(enabled = !busy && !uncertain && IosSpecialCommercePolicy.validConfig(draft), onClick = {
            busy = true; message = ""
            scope.launch {
                try {
                    cfg = api.saveDonationConfig(selected, draft)
                    check(api.activePersonalId() == ownerId)
                    onNotice("Donacions actualitzades i verificades al servidor.")
                } catch (ex: Exception) {
                    uncertain = true
                    message = (ex.message ?: "Error") + " Consulta l'estat abans de repetir."
                } finally { busy = false }
            }
        }) { Text("Guardar i verificar") }
    }
    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.error)
    TextButton(onClick = { uncertain = false; revision++ }, enabled = !busy) { Text("Consultar el servidor") }
}

/** Donacions públiques; la sessió registrada s'exigeix només per al cobrament, igual que Android. */
@Composable
internal fun IosArtistDonationCheckout(api: IosAgendaApi, artist: String, onLogin: () -> Unit, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var cfg by remember(artist) { mutableStateOf<IosDonationConfig?>(null) }
    var loading by remember(artist) { mutableStateOf(true) }
    var error by remember(artist) { mutableStateOf("") }
    var busy by remember(artist) { mutableStateOf(false) }
    var payUI by remember(artist) { mutableStateOf(false) }
    var selected by remember(artist) { mutableLongStateOf(0L) }
    var custom by remember(artist) { mutableStateOf("") }
    var name by remember(artist) { mutableStateOf("") }
    var donorMessage by remember(artist) { mutableStateOf("") }
    var anonymous by remember(artist) { mutableStateOf(true) }
    var pending by remember(artist) { mutableStateOf(api.pendingDonation()) }
    var uncertain by remember(artist) { mutableStateOf(api.donationUncertain()) }
    var info by remember(artist) { mutableStateOf("") }
    LaunchedEffect(artist) {
        runCatching { api.artistDonationConfig(artist) }.onSuccess {
            cfg = it; selected = it.suggested.firstOrNull() ?: it.minCents
        }.onFailure { error = it.message.orEmpty() }
        loading = false
    }
    fun verify(p: IosDonationPayment) {
        if (busy || payUI) return
        busy = true; error = ""; info = ""
        scope.launch {
            try {
                val receipt = api.donationStatus(p.donationId)
                if (IosSpecialCommercePolicy.confirmedDonation(p, receipt)) {
                    info = "Donació confirmada pel servidor."
                    api.clearDonation(p.donationId); pending = null
                } else {
                    info = "Estat: ${receipt.status}. No s'ha confirmat cap cobrament."
                    if (receipt.status.lowercase() in setOf("failed", "canceled", "cancelled")) {
                        api.clearDonation(p.donationId); pending = null
                    }
                }
            } catch (ex: Exception) { error = ex.message.orEmpty() }
            finally { busy = false }
        }
    }
    fun present(p: IosDonationPayment) {
        val bridge = IosNativeBridge.current ?: run { error = "Stripe no està disponible."; return }
        if (busy || payUI) return
        payUI = true
        bridge.presentTicketPayment(p.secret, p.key, p.merchant, object : IosTicketPaymentCallback {
            override fun onComplete(result: String, error: String) {
                payUI = false
                if (api.activePersonalId() != p.ownerId) return
                when (result) {
                    "COMPLETED" -> verify(p)
                    "CANCELLED" -> info = "Donació pendent; es pot comprovar o reprendre."
                    else -> info = "Stripe no ha completat la donació: $error"
                }
            }
        })
    }
    Dialog(onDismissRequest = { if (!busy && !payUI) onDismiss() }) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxWidth().heightIn(max = 680.dp).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Donacions · $artist", fontWeight = FontWeight.Bold)
                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                val c = cfg
                if (c != null && !c.active) Text("Este artista no accepta donacions actualment.")
                if (c != null && c.active) {
                    Text(c.title); if (c.description.isNotBlank()) Text(c.description)
                    if (!api.hasRegisteredSessionToken()) {
                        Text("Inicia sessió per a fer una donació.")
                        Button(onClick = onLogin) { Text("Iniciar sessió") }
                    } else {
                        c.suggested.forEach { amount -> FilterChip(selected = selected == amount && custom.isBlank(), onClick = {
                            selected = amount; custom = ""
                        }, label = { Text(IosArtistPagePolicy.money(amount)) }) }
                        OutlinedTextField(custom, { custom = it }, label = { Text("Altres imports (€)") })
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Anònima"); Switch(anonymous, { anonymous = it })
                        }
                        if (!anonymous) OutlinedTextField(name, { name = it }, label = { Text("Nom (opcional)") })
                        OutlinedTextField(donorMessage, { donorMessage = it }, label = { Text("Missatge (opcional)") })
                        val amount = if (custom.isBlank()) selected else IosProfessionalSellerPolicy.cents(custom) ?: -1L
                        if (pending != null) {
                            Text("Donació #${pending!!.donationId} pendent de verificació.")
                            OutlinedButton(enabled = !busy && !payUI, onClick = { verify(pending!!) }) { Text("Comprovar") }
                            Button(enabled = !busy && !payUI && pending!!.artist == artist,
                                onClick = { present(pending!!) }) { Text("Reprendre Stripe") }
                        } else if (uncertain) {
                            Text("No sabem si el servidor ha creat una donació. Consulta el compte abans de repetir.")
                            TextButton(onClick = { api.acknowledgeDonationUncertain(); uncertain = false }) {
                                Text("He comprovat el servidor; permetre un nou intent")
                            }
                        } else Button(enabled = !busy && !payUI && IosSpecialCommercePolicy.validDonationAmount(c, amount),
                            onClick = {
                                busy = true; error = ""
                                scope.launch {
                                    try {
                                        val p = api.createDonation(artist, amount, name, donorMessage, anonymous)
                                        pending = p; busy = false; present(p)
                                    } catch (ex: Exception) {
                                        error = ex.message.orEmpty(); uncertain = api.donationUncertain(); busy = false
                                    }
                                }
                            }) { Text("Donar ${IosArtistPagePolicy.money(amount)}") }
                    }
                }
                if (busy || payUI) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
                if (info.isNotBlank()) Text(info)
                TextButton(enabled = !busy && !payUI, onClick = onDismiss) { Text("Tancar") }
            }
        }
    }
}

@Composable
internal fun IosSpecialServicesDialog(api: IosAgendaApi, eventId: Int, onLogin: () -> Unit, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var event by remember(eventId) { mutableStateOf<IosSpecialEvent?>(null) }
    var error by remember(eventId) { mutableStateOf("") }
    var revision by remember(eventId) { mutableIntStateOf(0) }
    var selected by remember(eventId) { mutableStateOf<IosSpecialService?>(null) }
    var quantities by remember(eventId) { mutableStateOf<Map<Long, Int>>(emptyMap()) }
    var pending by remember(eventId) { mutableStateOf(api.pendingSpecialPayment()) }
    var uncertain by remember(eventId) { mutableStateOf(api.specialPaymentUncertain()) }
    var busy by remember(eventId) { mutableStateOf(false) }
    var payUI by remember(eventId) { mutableStateOf(false) }
    var confirmedQr by remember(eventId) { mutableStateOf("") }
    LaunchedEffect(eventId, revision) {
        runCatching { api.specialEvent(eventId) }.onSuccess { event = it }
            .onFailure { error = it.message.orEmpty() }
    }
    fun verify(p: IosSpecialPayment) {
        if (busy || payUI) return
        busy = true; error = ""
        scope.launch {
            try {
                val order = api.specialOrderStatus(p.orderId)
                if (IosSpecialCommercePolicy.confirmedService(p, order)) {
                    confirmedQr = order.qrPayload
                    api.clearSpecialPayment(p.orderId); pending = null
                } else {
                    error = "Comanda ${order.status}. No consta com a pagada ni validada."
                    if (order.status.lowercase() in setOf("failed", "canceled", "cancelled")) {
                        api.clearSpecialPayment(p.orderId); pending = null
                    }
                }
            } catch (ex: Exception) { error = ex.message.orEmpty() }
            finally { busy = false }
        }
    }
    fun present(p: IosSpecialPayment) {
        val native = IosNativeBridge.current ?: run { error = "Stripe no està disponible."; return }
        if (payUI || busy) return
        payUI = true
        native.presentTicketPayment(p.secret, p.key, p.merchant, object : IosTicketPaymentCallback {
            override fun onComplete(result: String, detail: String) {
                payUI = false
                if (api.activePersonalId() != p.ownerId) return
                if (result == "COMPLETED") verify(p)
                else error = if (result == "CANCELLED") "Pagament interromput; comprova la comanda abans de repetir."
                    else detail.ifBlank { "Stripe no ha completat el pagament." }
            }
        })
    }
    Dialog(onDismissRequest = { if (!busy && !payUI) onDismiss() }) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxWidth().heightIn(max = 690.dp).verticalScroll(rememberScrollState()).padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Serveis especials", fontWeight = FontWeight.Bold)
                if (event == null) Text("Consultant el servidor…")
                if (event != null && (event?.enabled == false || event?.services?.none { it.active } == true))
                    Text("No hi ha serveis disponibles per a este esdeveniment.")
                event?.takeIf { it.enabled }?.services?.filter { it.active }?.forEach { service ->
                    Text(service.name, fontWeight = FontWeight.SemiBold)
                    if (service.description.isNotBlank()) Text(service.description)
                    service.products.filter { it.active }.forEach { product ->
                        Text("${product.name} · ${IosArtistPagePolicy.money(product.priceCents)}")
                        if (product.reserveOnly) Text("Reserva amb el sistema de cobrament del servidor")
                        if (product.stock == 0) Text("Esgotat")
                    }
                    OutlinedButton(onClick = { selected = service; quantities = emptyMap() }) {
                        Text("Preparar comanda")
                    }
                    HorizontalDivider()
                }
                selected?.let { service ->
                    Text("Comanda · ${service.name}", fontWeight = FontWeight.Bold)
                    service.products.filter { it.active }.forEach { product ->
                        val count = quantities[product.id] ?: 0
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text(product.name, modifier = Modifier.weight(1f))
                            OutlinedButton(enabled = !busy && pending == null && count > 0, onClick = {
                                quantities = quantities + (product.id to count - 1)
                            }) { Text("−") }
                            Text("$count")
                            OutlinedButton(enabled = !busy && pending == null && count < minOf(99,product.stock ?: 99), onClick = {
                                quantities = quantities + (product.id to count + 1)
                            }) { Text("+") }
                        }
                    }
                    val chosen = service.products.mapNotNull { item ->
                        val count = quantities[item.id] ?: 0
                        if (count > 0) item to count else null
                    }
                    Text("Subtotal orientatiu: ${IosArtistPagePolicy.money(chosen.sumOf { it.first.priceCents * it.second })}. La gestió final la confirma Stripe.")
                    if (!api.hasRegisteredSessionToken()) Button(onClick = onLogin) { Text("Iniciar sessió per a comprar") }
                    else if (pending == null && !uncertain) Button(
                        enabled = !busy && !payUI && chosen.isNotEmpty() &&
                            chosen.all { (product, count) -> IosSpecialCommercePolicy.canBuy(service, product, count) },
                        onClick = {
                            busy = true; error = ""
                            scope.launch {
                                try {
                                    val payment = api.createSpecialPayment(eventId, service, chosen)
                                    pending = payment; busy = false; present(payment)
                                } catch (ex: Exception) {
                                    error = ex.message.orEmpty(); uncertain = api.specialPaymentUncertain(); busy = false
                                }
                            }
                        }) { Text("Continuar a Stripe") }
                }
                if (pending != null) {
                    Text("Comanda #${pending!!.orderId} encara pendent.")
                    OutlinedButton(enabled = !busy && !payUI, onClick = { verify(pending!!) }) { Text("Comprovar") }
                    Button(enabled = !busy && !payUI, onClick = { present(pending!!) }) { Text("Reprendre Stripe") }
                } else if (uncertain) {
                    Text("Possible comanda creada, però no confirmada. Comprova el servidor abans de repetir.")
                    TextButton(onClick = { api.acknowledgeSpecialUncertain(); uncertain = false }) {
                        Text("He comprovat la comanda; permetre un nou intent")
                    }
                }
                if (confirmedQr.isNotBlank()) {
                    Text("Comanda pagada i QR confirmat pel servidor.")
                    Button(onClick = { IosNativeBridge.current?.showSpecialServiceQr(confirmedQr, "Servei especial") }) {
                        Text("Mostrar QR")
                    }
                }
                if (busy || payUI) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
                TextButton(enabled = !busy && !payUI, onClick = onDismiss) { Text("Tancar") }
            }
        }
    }
}

/** Alta i edició de serveis / productes en esdeveniments explícitament autoritzats per PHP. */
@Composable
internal fun IosSpecialServicesManager(api: IosAgendaApi, ownerId: Int, onNotice: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var events by remember(ownerId) { mutableStateOf(emptyList<IosSpecialEventAccess>()) }
    var eventId by remember(ownerId) { mutableIntStateOf(0) }
    var event by remember(ownerId, eventId) { mutableStateOf<IosSpecialEvent?>(null) }
    var service by remember(ownerId) { mutableStateOf<IosSpecialService?>(null) }
    var product by remember(ownerId) { mutableStateOf<IosServiceProduct?>(null) }
    var name by remember(ownerId) { mutableStateOf("") }
    var description by remember(ownerId) { mutableStateOf("") }
    var imageUrl by remember(ownerId) { mutableStateOf("") }
    var overlay by remember(ownerId) { mutableStateOf("#FFE082") }
    var price by remember(ownerId) { mutableStateOf("0") }
    var stock by remember(ownerId) { mutableStateOf("") }
    var reserveOnly by remember(ownerId) { mutableStateOf(false) }
    var busy by remember(ownerId) { mutableStateOf(false) }
    var uncertain by remember(ownerId) { mutableStateOf(false) }
    var error by remember(ownerId) { mutableStateOf("") }
    var revision by remember(ownerId) { mutableIntStateOf(0) }
    fun refresh() { revision++ }
    fun mutate(block: suspend () -> IosSpecialEvent) {
        if (busy || uncertain) return
        busy = true; error = ""
        scope.launch {
            try { event = block(); check(api.activePersonalId() == ownerId); onNotice("Canvis verificats al servidor.") }
            catch (ex: Exception) { uncertain = true; error = (ex.message ?: "No confirmat") + " Consulta abans de repetir." }
            finally { busy = false }
        }
    }
    LaunchedEffect(ownerId) {
        runCatching { api.managedSpecialEvents() }.onSuccess {
            if (api.activePersonalId() == ownerId) { events = it; eventId = it.firstOrNull()?.id ?: 0 }
        }.onFailure { error = it.message.orEmpty() }
    }
    LaunchedEffect(ownerId,eventId,revision) {
        if (eventId > 0) runCatching { api.specialEvent(eventId,manage=true) }
            .onSuccess { event = it }.onFailure { error = it.message.orEmpty() }
    }
    Text("Serveis especials · gestió", fontWeight = FontWeight.Bold)
    if (events.isEmpty()) Text("No consten esdeveniments especials autoritzats al servidor.")
    events.forEach { candidate -> FilterChip(selected = candidate.id == eventId, onClick = {
        eventId = candidate.id; service = null; product = null; uncertain = false
    }, label = { Text(candidate.title.ifBlank { "#${candidate.id}" }) }) }
    if (event != null && event?.canEditServices != true) Text("Permís d'edició de serveis no concedit pel servidor.")
    if (event?.canEditServices == true) {
        OutlinedButton(onClick = { service = IosSpecialService(0,eventId,"","","","#FFE082",true, emptyList()); name=""; description=""; imageUrl=""; overlay="#FFE082"; product=null }) { Text("+ Servei") }
        event?.services?.forEach { s ->
            Text("${s.name} · ${s.products.size} productes", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedButton(enabled=!busy,onClick = { service=s; product=null; name=s.name;description=s.description;imageUrl=s.imageUrl;overlay=s.color }) { Text("Editar") }
                OutlinedButton(enabled=!busy && !uncertain,onClick={mutate { api.deleteSpecialService(eventId,s.id) }}) { Text("Eliminar") }
                OutlinedButton(enabled=!busy,onClick = { service=s;product=IosServiceProduct(0,s.id,"","","",0,false,null,true);name="";description="";imageUrl="";price="0";stock="";reserveOnly=false }) { Text("+ Producte") }
            }
            s.products.filter { it.active }.forEach { item ->
                Text("${item.name} · ${IosArtistPagePolicy.money(item.priceCents)}")
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    OutlinedButton(onClick = { service=s;product=item;name=item.name;description=item.description;imageUrl=item.imageUrl;price=(item.priceCents/100.0).toString();stock=item.stock?.toString().orEmpty();reserveOnly=item.reserveOnly }) { Text("Editar") }
                    OutlinedButton(enabled=!busy && !uncertain,onClick={ mutate { api.deleteSpecialProduct(eventId,s.id,item.id) } }) { Text("Eliminar") }
                }
            }
        }
        if (service != null) {
            HorizontalDivider()
            Text(if (product != null) "Producte del servei ${service!!.name}" else "Configuració del servei")
            OutlinedTextField(name, { name=it }, label={Text("Nom")},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(description,{description=it}, label={Text("Descripció")},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(imageUrl,{imageUrl=it}, label={Text("URL imatge HTTPS")},modifier=Modifier.fillMaxWidth())
            if (product == null) OutlinedTextField(overlay,{overlay=it}, label={Text("Color #RRGGBB")},modifier=Modifier.fillMaxWidth())
            if (product != null) {
                OutlinedTextField(price,{price=it},label={Text("Preu €")},modifier=Modifier.fillMaxWidth())
                OutlinedTextField(stock,{stock=it.filter(Char::isDigit)},label={Text("Estoc (buit = sense límit)")},modifier=Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) {
                    Text("Només reserva");Switch(reserveOnly,{reserveOnly=it})
                }
            }
            val cents = IosProfessionalSellerPolicy.cents(price) ?: -1L
            Button(enabled=!busy && !uncertain && IosSpecialCommercePolicy.validServiceName(name) &&
                (product==null || cents>=0) && (imageUrl.isBlank() || IosArtistPagePolicy.imageUrl(imageUrl)==imageUrl) &&
                (product!=null || overlay.matches(Regex("#[0-9A-Fa-f]{6}"))), onClick={
                val svc = service!!; val pr = product
                mutate {
                    if (pr == null) api.saveSpecialService(eventId,svc.id,name,description,imageUrl,overlay)
                    else api.saveSpecialProduct(eventId, pr.copy(name=name,description=description,imageUrl=imageUrl,priceCents=cents,
                        stock=stock.toIntOrNull(),reserveOnly=reserveOnly))
                }
                service=null;product=null
            }) { Text("Guardar i verificar") }
            TextButton(onClick = {service=null;product=null}) { Text("Cancel·lar") }
        }
    }
    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    if (error.isNotBlank()) Text(error,color=MaterialTheme.colorScheme.error)
    TextButton(enabled=!busy,onClick = {uncertain=false;refresh()}) { Text("Consultar el servidor") }
}

/** Bescanvi servidor de serveis, independent de l'escàner d'entrades. */
@Composable
internal fun IosSpecialServicesRedeem(api: IosAgendaApi) {
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var inspection by remember { mutableStateOf<IosSpecialInspection?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    Text("Validar serveis especials",fontWeight=FontWeight.Bold)
    OutlinedTextField(code,{code=it;inspection=null},label={Text("Codi QR del servei")},modifier=Modifier.fillMaxWidth())
    OutlinedButton(onClick = {
        IosNativeBridge.current?.scanSpecialServiceQr(object : IosSpecialServiceQrCallback {
            override fun onScanned(scanned: String, error: String) {
                if (scanned.isNotBlank()) { code=scanned; inspection=null; message="Codi llegit. Consulta el servidor abans de bescanviar." }
                else message=error.ifBlank { "No s'ha llegit cap QR." }
            }
        }) ?: run { message="La càmera no està disponible en este dispositiu." }
    }) { Text("Escanejar QR amb la càmera") }
    OutlinedButton(enabled=!busy && code.isNotBlank(),onClick={
        busy=true;message=""
        scope.launch {
            runCatching { api.inspectSpecialOrder(code) }.onSuccess { inspection=it }.onFailure { message=it.message.orEmpty() }
            busy=false
        }
    }) { Text("Consultar") }
    inspection?.let { found ->
        Text("Comanda #${found.orderId} · ${found.serviceName} · ${found.status}")
        if (IosSpecialCommercePolicy.redeemAllowed(found)) Button(enabled=!busy,onClick={
            busy=true;message=""
            scope.launch {
                runCatching { api.redeemSpecialOrder(code,found) }.onSuccess {
                    inspection=null;message="Bescanvi confirmat pel servidor."
                }.onFailure { message=it.message.orEmpty() }
                busy=false
            }
        }) { Text("Bescanviar al servidor") }
    }
    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    if (message.isNotBlank()) Text(message)
}
