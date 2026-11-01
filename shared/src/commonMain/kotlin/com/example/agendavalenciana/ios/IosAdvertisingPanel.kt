package com.example.agendavalenciana.ios

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch

/** Pas 44: creador/gestor d'anuncis. Una campanya només està activa si PHP la confirma.
 * Les dades de pagament, creativitats i segmentació mai es guarden al dispositiu. */
@Composable
internal fun IosAdvertisingPanel(api: IosAgendaApi, ownerId: Int, registered: Boolean,
    owned: List<IosPublishedEvent>, onLogin: () -> Unit, onNotice: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var campaigns by remember(ownerId) { mutableStateOf(emptyList<IosCampaign>()) }
    var managed by remember(ownerId) { mutableStateOf(emptyList<IosProEvent>()) }
    var selectedEvent by remember(ownerId) { mutableIntStateOf(0) }
    var placement by remember(ownerId) { mutableStateOf("FEED_NORMAL") }
    var duration by remember(ownerId) { mutableIntStateOf(1) }
    var styles by remember(ownerId) { mutableStateOf("") }
    var terms by remember(ownerId) { mutableStateOf("") }
    var towns by remember(ownerId) { mutableStateOf("") }
    var comarques by remember(ownerId) { mutableStateOf("") }
    var followers by remember(ownerId) { mutableStateOf(false) }
    var highIntent by remember(ownerId) { mutableStateOf(false) }
    var matchAll by remember(ownerId) { mutableStateOf(false) }
    var busy by remember(ownerId) { mutableStateOf(false) }
    var uncertain by remember(ownerId) { mutableStateOf(false) }
    var loading by remember(ownerId) { mutableStateOf(false) }
    var error by remember(ownerId) { mutableStateOf("") }
    var info by remember(ownerId) { mutableStateOf("") }
    var revision by remember(ownerId) { mutableIntStateOf(0) }
    var editing by remember(ownerId) { mutableStateOf<IosCampaign?>(null) }
    var pending by remember(ownerId) { mutableStateOf(api.pendingCampaignPayment()) }
    var focusX by remember(ownerId) { mutableStateOf("0.5") }
    var focusY by remember(ownerId) { mutableStateOf("0.5") }
    var visualImage by remember(ownerId) { mutableStateOf("") }
    var payUI by remember(ownerId) { mutableStateOf(false) }

    val target = IosCampaignTarget(IosAdvertisingPolicy.parseCsv(styles), IosAdvertisingPolicy.parseCsv(terms),
        IosAdvertisingPolicy.parseCsv(towns), IosAdvertisingPolicy.parseCsv(comarques),
        followers, highIntent, matchAll)

    fun applyAd(ad: IosCampaign) {
        editing = ad; styles = ad.target.styles.joinToString(", "); terms = ad.target.terms.joinToString(", ")
        towns = ad.target.towns.joinToString(", "); comarques = ad.target.comarques.joinToString(", ")
        followers = ad.target.followers; highIntent = ad.target.highIntent; matchAll = ad.target.all
        focusX = ad.focusX.toString(); focusY = ad.focusY.toString(); visualImage = ad.imageUrl
        error = ""; info = ""
    }
    fun reload() { revision++ }
    fun verify(adId: Int) {
        if (busy) return
        busy = true; error = ""; info = ""
        scope.launch {
            try {
                val status = api.campaignStatus(adId)
                check(api.activePersonalId() == ownerId) { "La sessió ha canviat." }
                val confirmed = api.confirmedActiveCampaign(adId)
                if (confirmed != null) {
                    info = "Campanya activa confirmada per MariaDB."
                    pending = null
                    api.clearPendingCampaignPayment(adId)
                    onNotice("Campanya publicitària activada pel servidor.")
                } else if (IosAdvertisingPolicy.terminalFailure(status.status)) {
                    error = "El servidor indica ${status.status}. No es crearà un altre pagament automàticament."
                    pending = null; api.clearPendingCampaignPayment(adId)
                } else {
                    info = "Estat actual: ${status.status}. Encara no està activa; torna a consultar el servidor."
                }
                reload()
            } catch (ex: Exception) {
                uncertain = true
                error = ex.message ?: "No es pot verificar el pagament. Consulta abans de tornar a contractar."
            } finally { busy = false }
        }
    }
    fun openStripe(payment: IosCampaignPayment) {
        if (payUI || busy) return
        val native = IosNativeBridge.current
        if (native == null) { error = "Stripe natiu no està disponible en este dispositiu."; return }
        payUI = true; error = ""
        try {
            native.presentTicketPayment(payment.secret, payment.key, payment.merchant,
                object : IosTicketPaymentCallback {
                    override fun onComplete(result: String, error: String) {
                        payUI = false
                        if (api.activePersonalId() != ownerId) return
                        when(result) {
                            "COMPLETED" -> verify(payment.adId)
                            "CANCELLED" -> info = "Pagament interromput. Pots consultar l'estat o reprendre el mateix intent, sense crear-ne un altre."
                            else -> {
                                info = error.ifBlank { "El pagament no s'ha completat." } +
                                    " Consulta el servidor abans de tornar a provar."
                            }
                        }
                    }
                })
        } catch (ex: Exception) {
            payUI = false
            error = ex.message ?: "No es pot obrir Stripe."
        }
    }
    fun operate(action: suspend () -> Unit) {
        if (busy || payUI || uncertain) return
        busy = true; error = ""; info = ""
        scope.launch {
            try {
                action()
                check(api.activePersonalId() == ownerId) { "La sessió ha canviat." }
                reload()
            } catch (ex: Exception) {
                uncertain = true
                error = (ex.message ?: "El servidor no ha confirmat el canvi.") +
                    " Consulta l'estat abans de repetir una operació."
            } finally { busy = false }
        }
    }
    LaunchedEffect(ownerId, registered, revision) {
        if (!registered) return@LaunchedEffect
        loading = true
        try {
            campaigns = api.myCampaigns()
            managed = try { api.professionalDashboard().events.filter { it.status.equals("published", true) } }
                catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
                catch (_: Exception) { emptyList() }
            check(api.activePersonalId() == ownerId) { "La sessió ha canviat." }
            uncertain = false
        } catch (ex: Exception) {
            error = ex.message ?: "No s'han pogut recuperar els anuncis."
        } finally { loading = false }
    }

    Text("Campanyes publicitàries", fontWeight = FontWeight.Bold)
    Text("Els anuncis pagats actius són globals i independents dels filtres personals.",
        style = MaterialTheme.typography.bodySmall)
    if (!registered) {
        Text("Cal iniciar sessió per contractar una campanya, encara que pots publicar activitats com a convidat.")
        Button(onClick = onLogin) { Text("Iniciar sessió o crear compte") }
        return
    }
    if (busy || loading || payUI) LinearProgressIndicator(Modifier.fillMaxWidth())
    if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    if (info.isNotBlank()) Text(info, style = MaterialTheme.typography.bodySmall)
    if (uncertain) {
        Text("L'última operació no s'ha pogut verificar. Abans de repetir-la, consulta la campanya al servidor.",
            style = MaterialTheme.typography.bodySmall)
    }
    OutlinedButton(enabled = !busy && !payUI && !loading, onClick = {
        uncertain = false; error = ""; reload()
    }) { Text("Consultar campanyes al servidor") }
    if (pending != null) {
        val current = pending!!
        HorizontalDivider()
        Text("Pagament pendent · anunci #${current.adId}", fontWeight = FontWeight.Bold)
        Text("Import retornat pel servidor: ${IosProfessionalSellerPolicy.euros(current.cents)} · ${current.currency.uppercase()}",
            style = MaterialTheme.typography.bodySmall)
        OutlinedButton(enabled = !busy && !payUI, onClick = { verify(current.adId) }) { Text("Comprovar pagament") }
        Button(enabled = !busy && !payUI && !uncertain, onClick = { openStripe(current) }) { Text("Reprendre el mateix pagament") }
        Text("No es crearà cap altra comanda mentre este pagament siga pendent.", style = MaterialTheme.typography.bodySmall)
    }
    HorizontalDivider()
    Text("Les meues campanyes", fontWeight = FontWeight.Bold)
    if (campaigns.isEmpty() && !loading) Text("No consten campanyes al servidor.", style = MaterialTheme.typography.bodySmall)
    val serverPending = campaigns.any { it.status.lowercase() in setOf("payment_pending", "pending", "requires_payment") }
    if (serverPending) Text("Hi ha una campanya pendent al servidor. Comprova el seu estat abans de crear una altra comanda. Si no es conserva el PaymentIntent en memòria, no es pot reprendre el cobrament en esta sessió.",
        style = MaterialTheme.typography.bodySmall)
    campaigns.forEach { ad ->
        Text("#${ad.id} · ${ad.placement} · ${ad.status}", fontWeight = FontWeight.Medium)
        Text("Esdeveniment ${ad.eventId} · ${ad.startsAt} – ${ad.endsAt}", style = MaterialTheme.typography.bodySmall)
        Text("Import: ${IosProfessionalSellerPolicy.euros(ad.amountCents)}", style = MaterialTheme.typography.bodySmall)
        if (ad.imageUrl.isNotBlank()) AsyncImage(model = ad.imageUrl, contentDescription = "Creativitat de l'anunci",
            modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            OutlinedButton(enabled = !busy && !payUI, onClick = { applyAd(ad) }) { Text("Editar campanya") }
            OutlinedButton(enabled = !busy && !payUI, onClick = { verify(ad.id) }) { Text("Comprovar estat") }
        }
        HorizontalDivider()
    }
    if (editing != null) {
        val ad = editing!!
        Text("Editar campanya #${ad.id}", fontWeight = FontWeight.Bold)
        OutlinedButton(onClick = { editing = null; error = "" }, enabled = !busy) { Text("Tancar edició") }
    } else {
        Text("Nova campanya", fontWeight = FontWeight.Bold)
        if (owned.none { it.status.equals("published", true) } && managed.isEmpty()) {
            Text("No hi ha publicacions pròpies aprovades. Si gestiones un perfil professional reclamat, el servidor també pot autoritzar-ne activitats.", style = MaterialTheme.typography.bodySmall)
        }
        owned.filter { it.status.equals("published", true) }.forEach { event ->
            FilterChip(selected = selectedEvent == event.eventId, enabled = !busy && pending == null,
                onClick = { selectedEvent = event.eventId }, label = { Text("${event.title} · ${event.date}") })
        }
        managed.filter { managedEvent -> owned.none { it.eventId == managedEvent.id } }.forEach { event ->
            FilterChip(selected = selectedEvent == event.id, enabled = !busy && pending == null,
                onClick = { selectedEvent = event.id }, label = { Text("${event.title} · perfil professional") })
        }
        Text("Espai publicitari")
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            IosAdvertisingPolicy.placements.forEach { p -> FilterChip(selected = placement == p,
                enabled = !busy && pending == null, onClick = { placement = p }, label = { Text(p) }) }
        }
        Text("Duració")
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            IosAdvertisingPolicy.durations.sorted().forEach { d -> FilterChip(selected = duration == d,
                enabled = !busy && pending == null, onClick = { duration = d }, label = { Text("$d ${if (d == 1) "dia" else "dies"}") }) }
        }
    }
    val canEdit = registered && !busy && !payUI && !loading && !uncertain && pending == null
    OutlinedTextField(styles, { styles = it }, enabled = canEdit, label = { Text("Estils (separats per comes)") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(terms, { terms = it }, enabled = canEdit, label = { Text("Paraules de cerca") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(towns, { towns = it }, enabled = canEdit, label = { Text("Pobles") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(comarques, { comarques = it }, enabled = canEdit, label = { Text("Comarques") }, modifier = Modifier.fillMaxWidth())
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Seguidors")
        Switch(followers, { followers = it }, enabled = canEdit)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Interés alt")
        Switch(highIntent, { highIntent = it }, enabled = canEdit)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Combinar tots els criteris (AND)")
        Switch(matchAll, { matchAll = it }, enabled = canEdit)
    }
    Text("La segmentació és configuració de campanya. Les ubicacions de publicitat pagada vigent no desapareixen del feed per preferències d'usuari.",
        style = MaterialTheme.typography.bodySmall)

    val chosen = editing
    if (chosen == null) {
        val draft = IosCampaignDraft(selectedEvent, placement, duration, target)
        Button(modifier = Modifier.fillMaxWidth(), enabled = canEdit && !serverPending && selectedEvent > 0 && IosAdvertisingPolicy.valid(draft), onClick = {
            operate {
                val start = api.createCampaignPayment(draft)
                pending = start
                info = "Comanda d'anunci #${start.adId} · ${IosProfessionalSellerPolicy.euros(start.cents)}. Confirma el preu abans de pagar."
                // Native checkout must be user-driven; it is opened from a separate button.
            }
        }) { Text("Preparar pagament al servidor") }
        Text("L'import definitiu el calcula el servidor, no el formulari.", style = MaterialTheme.typography.bodySmall)
    } else {
        Button(modifier = Modifier.fillMaxWidth(), enabled = canEdit &&
            IosAdvertisingPolicy.valid(IosCampaignDraft(chosen.eventId, chosen.placement, 1, target)), onClick = {
            operate { val updated = api.updateCampaignTarget(chosen, target); applyAd(updated); info = "Segmentació verificada." }
        }) { Text("Guardar segmentació") }
        OutlinedTextField(focusX, { focusX = it }, enabled = canEdit, label = { Text("Enquadrament X (0–1)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(focusY, { focusY = it }, enabled = canEdit, label = { Text("Enquadrament Y (0–1)") }, modifier = Modifier.fillMaxWidth())
        if (visualImage.isNotBlank()) AsyncImage(model = visualImage, contentDescription = "Imatge actual de la campanya",
            modifier = Modifier.fillMaxWidth().heightIn(max = 190.dp))
        val x = focusX.replace(',', '.').toFloatOrNull()
        val y = focusY.replace(',', '.').toFloatOrNull()
        val safe = canEdit && x != null && y != null && x in 0f..1f && y in 0f..1f
        OutlinedButton(enabled = safe, onClick = {
            val native = IosNativeBridge.current
            if (native == null) { error = "No hi ha selector de fotos d'iPhone." }
            else native.pickProfilePhoto(object : IosPickedPhotoCallback {
                override fun onComplete(base64Jpeg: String, error: String) {
                    if (error.isNotBlank() || base64Jpeg.isBlank()) {
                        if (error != "Selecció cancel·lada") onNotice(error.ifBlank { "No es pot llegir la imatge." })
                    } else operate {
                        val updated = api.updateCampaignVisual(chosen, x!!, y!!, base64Jpeg)
                        applyAd(updated); info = "Creativitat confirmada pel servidor."
                    }
                }
            })
        }) { Text("Pujar nova creativitat") }
        Button(enabled = safe, onClick = {
            operate { val updated = api.updateCampaignVisual(chosen, x!!, y!!); applyAd(updated); info = "Enquadrament confirmat." }
        }) { Text("Guardar enquadrament") }
        OutlinedButton(enabled = safe && chosen.imageUrl.isNotBlank(), onClick = {
            operate { val updated = api.updateCampaignVisual(chosen, x!!, y!!, removeImage = true); applyAd(updated); info = "Imatge eliminada i verificada." }
        }) { Text("Eliminar creativitat") }
    }
}
