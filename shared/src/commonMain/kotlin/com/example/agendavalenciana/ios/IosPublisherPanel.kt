package com.example.agendavalenciana.ios

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch

/** Pas 36: Android's + publisher sheet with the same permission gates.
 * No local professional role elevation, no fake payment success and no duplicate non-idempotent POST.
 */
@Composable
internal fun IosPublisherPanel(api: IosAgendaApi, expectedOwnerId: Int, registered: Boolean,
    onDismiss: () -> Unit, onAuth: () -> Unit, onPublished: (Int) -> Unit,
    onAdmin: (IosPublishAction) -> Unit, onNotice: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    var snapshot by remember(expectedOwnerId) { mutableStateOf<IosPublisherSnapshot?>(null) }
    var own by remember(expectedOwnerId) { mutableStateOf(emptyList<IosPublishedEvent>()) }
    var page by remember(expectedOwnerId) { mutableStateOf("menu") }
    var busy by remember(expectedOwnerId) { mutableStateOf(false) }
    var error by remember(expectedOwnerId) { mutableStateOf("") }
    var current by remember(expectedOwnerId) { mutableStateOf<IosPublishedEvent?>(null) }
    var seller by remember(expectedOwnerId) { mutableStateOf<IosSellerStatus?>(null) }
    var sellerUncertain by remember(expectedOwnerId) { mutableStateOf(false) }
    var sellerSale by remember(expectedOwnerId) { mutableStateOf<IosSellerSaleState?>(null) }
    var selectedSaleEvent by remember(expectedOwnerId) { mutableIntStateOf(0) }
    var saleUncertain by remember(expectedOwnerId) { mutableStateOf(false) }
    var publicationReceipt by remember(expectedOwnerId) { mutableStateOf<IosPublicationReceipt?>(null) }
    var submissionUncertain by remember(expectedOwnerId) { mutableStateOf(false) }
    var submittedDraft by remember(expectedOwnerId) { mutableStateOf<IosPublishDraft?>(null) }

    fun load() {
        if (busy) return
        busy = true; error = ""
        scope.launch {
            try {
                val result = api.publisherSnapshot()
                check(result.userId == expectedOwnerId) { "El compte ha canviat" }
                val loaded = api.publisherOwnEvents()
                check(api.activePersonalId() == expectedOwnerId) { "El compte ha canviat" }
                snapshot = result
                own = loaded
            } catch (ex: Exception) { error = ex.message ?: "Error llegint permisos del servidor" }
            finally { busy = false }
        }
    }
    LaunchedEffect(expectedOwnerId) { load() }
    Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().heightIn(max = 690.dp).verticalScroll(rememberScrollState())
                .padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (page == "menu") "Publicar" else "Panell de publicació", fontWeight = FontWeight.Bold,
                        fontSize = 20.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = { if (page == "menu") onDismiss() else { page = "menu"; load() } },
                        enabled = !busy) { Text(if (page == "menu") "Tancar" else "Arrere") }
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                val access = snapshot
                if (access == null) {
                    Text("Comprovant el rol i les publicacions pròpies al servidor…", fontSize = 12.sp)
                    TextButton(onClick = ::load, enabled = !busy) { Text("Reintentar") }
                } else if (page == "menu") {
                    Text(if (IosPublisherRoles.isProfessional(access)) "Panell professional" else "Publicar activitats",
                        fontWeight = FontWeight.SemiBold)
                    Text(if (access.suspended) "Publicació suspesa per moderació. Les publicacions existents es conserven."
                        else if (IosPublisherRoles.canPublishDirectly(access)) "Publicació directa autoritzada pel servidor."
                        else "Totes les persones poden publicar activitats; el servidor aplica la revisió corresponent.",
                        fontSize = 12.sp)
                    if (!registered) Text("Convidat amb ID $expectedOwnerId: publicar és possible sense registrar-se. Per vendre entrades cal crear o iniciar sessió.", fontSize = 12.sp)
                    val labels = mapOf(
                        IosPublishAction.NEW_EVENT to "Nou esdeveniment",
                        IosPublishAction.EDIT_EVENTS to "Editar esdeveniments",
                        IosPublishAction.CREATE_AD to "Crear anunci",
                        IosPublishAction.TICKETS to "Entrades, ingressos i facturació",
                        IosPublishAction.ACTIVATE_SALES to "Activar venda d'entrades",
                        IosPublishAction.MERCH to "Vendre discos / merch",
                        IosPublishAction.DONATIONS to "Donacions",
                        IosPublishAction.SPECIAL_SERVICES to "Serveis especials",
                        IosPublishAction.CLAIM_PROFESSIONAL to if (IosPublisherRoles.isProfessional(access)) "Reclamar un altre perfil" else "Soc organitzador o artista",
                        IosPublishAction.REVIEW_PUBLICATIONS to "Revisar publicacions",
                        IosPublishAction.REVIEW_PROFESSIONAL_CLAIMS to "Sol·licituds professionals")
                    IosPublisherRoles.visibleActions(registered, access).forEach { action ->
                        OutlinedButton(modifier = Modifier.fillMaxWidth(), enabled = !busy, onClick = {
                            if (!IosPublisherRoles.allows(action, registered, access)) {
                                error = "Accés no confirmat pel servidor."
                            } else when(action) {
                                IosPublishAction.NEW_EVENT -> {
                                    if (access.suspended) error = "La publicació està suspesa per moderació."
                                    else { current = null; submissionUncertain = false; publicationReceipt = null; page = "event" }
                                }
                                IosPublishAction.EDIT_EVENTS -> page = "own"
                                IosPublishAction.CLAIM_PROFESSIONAL -> if (registered) page = "claim" else onAuth()
                                IosPublishAction.TICKETS -> { page = "dashboard" }
                                IosPublishAction.ACTIVATE_SALES -> {
                                    page = "seller"
                                    sellerUncertain = false
                                    busy = true
                                    scope.launch {
                                        try { seller = api.sellerOnboardingStatus() }
                                        catch (ex: Exception) { error = ex.message ?: "No hi ha accés a facturació" }
                                        finally { busy = false }
                                    }
                                }
                                IosPublishAction.REVIEW_PUBLICATIONS, IosPublishAction.REVIEW_PROFESSIONAL_CLAIMS -> { onDismiss(); onAdmin(action) }
                                IosPublishAction.CREATE_AD -> page = "advertising"
                                IosPublishAction.MERCH -> page = "commerce"
                                IosPublishAction.DONATIONS -> page = "donations"
                                IosPublishAction.SPECIAL_SERVICES -> page = "services"
                            }
                        }) { Text(labels[action] ?: action.name) }
                    }
                    TextButton(onClick = ::load, enabled = !busy) { Text("Actualitzar permisos i autoria") }
                } else if (page == "own") {
                    Text("Activitats publicades per este usuari, segons MariaDB", fontSize = 12.sp)
                    own.filter { it.status.equals("published", true) }.forEach { item ->
                        Text("${item.title} · ${item.date}", fontWeight = FontWeight.Medium)
                        if (item.posterUrl.isNotBlank()) {
                            AsyncImage(model = item.posterUrl, contentDescription = "Cartell de ${item.title}",
                                modifier = Modifier.size(96.dp), contentScale = ContentScale.Crop)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            OutlinedButton(modifier = Modifier.weight(1f), enabled = !busy, onClick = {
                                current = item; page = "event"
                            }) { Text("Editar") }
                            OutlinedButton(modifier = Modifier.weight(1f), enabled = !busy, onClick = {
                                val native = IosNativeBridge.current
                                if (native == null) { error = "Falta el selector de fotos d'iPhone." }
                                else native.pickProfilePhoto(object : IosPickedPhotoCallback {
                                    override fun onComplete(base64Jpeg: String, error: String) {
                                        if (error.isNotBlank() || base64Jpeg.isBlank()) {
                                            if (error != "Selecció cancel·lada") onNotice(error.ifBlank { "No s'ha pogut llegir el cartell." })
                                        } else {
                                            busy = true
                                            scope.launch {
                                                try {
                                                    val url = api.publisherUploadPoster(item.eventId, base64Jpeg)
                                                    if (api.activePersonalId() == expectedOwnerId) {
                                                        onNotice("Cartell confirmat al servidor: $url")
                                                        own = api.publisherOwnEvents()
                                                    }
                                                } catch (ex: Exception) {
                                                    if (api.activePersonalId() == expectedOwnerId)
                                                        onNotice(ex.message ?: "El servidor no ha confirmat el cartell.")
                                                } finally { busy = false }
                                            }
                                        }
                                    }
                                })
                            }) { Text("Cartell") }
                        }
                        if (registered) OutlinedButton(modifier = Modifier.fillMaxWidth(), enabled = !busy, onClick = {
                            current = item; page = "schedule"
                        }) { Text("Programació (si el servidor dona permís)") }
                        HorizontalDivider()
                    }
                    if (own.none { it.status.equals("published",true) }) Text("No tens activitats publicades.")
                } else if (page == "event") {
                    if (submissionUncertain) {
                        Text("No podem saber si el servidor ha rebut la petició. Per evitar duplicats, la publicació està bloquejada fins que consultes les teues activitats.",
                            color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        OutlinedButton(enabled = !busy, onClick = {
                            busy = true
                            scope.launch {
                                try {
                                    val recent = api.publisherOwnEvents()
                                    check(api.activePersonalId() == expectedOwnerId) { "La sessió ha canviat" }
                                    own = recent
                                    val draft = submittedDraft
                                    val matches = if (draft == null) emptyList() else recent.filter {
                                        it.title.trim().equals(draft.title.trim(), true) &&
                                            it.date == draft.date && it.town.trim().equals(draft.town.trim(), true)
                                    }
                                    error = if (matches.isNotEmpty())
                                        "Hi ha una publicació semblant al servidor (ID: ${matches.joinToString { it.eventId.toString() }}). Consulta-la abans de publicar de nou."
                                    else "No s'ha trobat una coincidència en esta consulta; un enviament recent encara podria estar processant-se."
                                } catch (ex: Exception) { error = ex.message ?: "No s'ha pogut consultar el servidor." }
                                finally { busy = false }
                            }
                        }) { Text("Comprovar les meues publicacions") }
                        TextButton(enabled = !busy, onClick = {
                            submissionUncertain = false
                            error = "Tornar a publicar pot crear una activitat duplicada si el primer enviament es va completar."
                        }) { Text("Reintentar manualment (risc de duplicat)") }
                    }
                    IosPublisherEventEditor(initial = current, enabled = !busy && !submissionUncertain, onSave = { draft ->
                        val eventToEdit = current?.eventId
                        busy = true; error = ""; submittedDraft = draft
                        scope.launch {
                            try {
                                check(api.activePersonalId() == expectedOwnerId) { "Sessió canviada" }
                                if (eventToEdit == null) {
                                    val receipt = api.publisherSubmit(draft)
                                    if (api.activePersonalId() == expectedOwnerId) {
                                        publicationReceipt = receipt
                                        onNotice(receipt.message)
                                        if (receipt.published) onPublished(receipt.eventId)
                                        page = "submitted"
                                    }
                                } else {
                                    api.publisherEdit(eventToEdit, draft)
                                    if (api.activePersonalId() == expectedOwnerId) {
                                        onNotice("Edició confirmada pel servidor")
                                        page = "menu"
                                    }
                                }
                                // Refresh server permissions on next open; never auto-repeat a POST.
                                // Keep the access snapshot to render the confirmation page.
                                // Only clear it if we return straight to the menu after an edit.
                                if (page == "menu") snapshot = null
                            } catch (ex: Exception) {
                                error = ex.message ?: "El servidor no ha confirmat la publicació."
                                // A lost response may mean that the POST succeeded. Never repeat automatically.
                                if (eventToEdit == null) submissionUncertain = true
                            } finally {
                                busy = false
                                if (page == "menu" && snapshot == null) load()
                            }
                        }
                    })
                } else if (page == "schedule") {
                    current?.let { item ->
                        IosPublisherSchedulePanel(api, expectedOwnerId, item.eventId, enabled = !busy)
                    }
                } else if (page == "submitted") {
                    val receipt = publicationReceipt
                    if (receipt != null) {
                        Text("Comprovant de l'esdeveniment #${receipt.eventId}", fontWeight = FontWeight.Bold)
                        Text(receipt.message)
                        if (!receipt.reviewConfirmed) Text("No es pot verificar des de l'app que la fitxa s'haja inserit a REVISIO_PUBLICACIONS_APP.", fontSize = 12.sp)
                        if (!registered) {
                            Text("La publicació ja s'ha enviat. Pots crear un compte per a gestionar-la i activar la venda d'entrades quan completes la facturació.", fontSize = 12.sp)
                            Button(modifier = Modifier.fillMaxWidth(), onClick = onAuth) { Text("Crear compte o iniciar sessió") }
                            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onDismiss) { Text("Ara no") }
                        } else Button(modifier = Modifier.fillMaxWidth(), onClick = onDismiss) { Text("Tancar") }
                    }
                } else if (page == "claim") {
                    IosProfessionalClaimForm(enabled = !busy, onSend = { entity, type, relation, contact, message ->
                        busy = true; error = ""
                        scope.launch {
                            try {
                                val receipt = api.requestProfessionalClaim(entity, type, relation, contact, message)
                                if (api.activePersonalId() == expectedOwnerId) { onNotice(receipt); page = "menu" }
                            } catch (ex: Exception) { error = ex.message ?: "Sol·licitud no confirmada" }
                            finally { busy = false }
                        }
                    })
                } else if (page == "dashboard") {
                    IosProfessionalDashboardPanel(api, expectedOwnerId, enabled = !busy, onFiscalSetup = {
                        page = "seller"
                        seller = null
                        busy = true
                        scope.launch {
                            try { seller = api.sellerOnboardingStatus() }
                            catch (ex: Exception) { error = ex.message ?: "No es pot consultar l'alta fiscal" }
                            finally { busy = false }
                        }
                    })
                } else if (page == "seller") {
                    val state = seller
                    Text("Les dades fiscals i la vigència dels consentiments es consulten al servidor.", fontSize = 12.sp)
                    if (!registered) {
                        Text("Per activar la venda, crea un compte o inicia sessió.")
                        Button(onClick = onAuth) { Text("Crear compte o iniciar sessió") }
                    } else if (state == null) {
                        Text("No s'ha pogut comprovar l'alta fiscal.")
                        TextButton(enabled = !busy, onClick = {
                            busy = true
                            scope.launch {
                                try { seller = api.sellerOnboardingStatus(); sellerUncertain = false }
                                catch (ex: Exception) { error = ex.message ?: "No es pot consultar la facturació" }
                                finally { busy = false }
                            }
                        }) { Text("Consultar estat al servidor") }
                    } else {
                        Text(if (state.hasBilling) "Hi ha titulars fiscals registrats." else "Encara no hi ha un titular fiscal vinculat.", fontSize = 12.sp)
                        if (!IosSellerLegalPolicy.policiesUsable(state))
                            Text("Les polítiques legals no estan disponibles en HTTPS: no es pot completar l'alta.",
                                color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        Text("Cal triar una activitat pròpia publicada i un titular fiscal. Esta alta no activa per si sola els cobraments ni ven entrades.", fontSize = 12.sp)
                        if (state.hasBilling && state.companies.any { it.noticesCurrent }) {
                            Text("Venda d'entrades (requereix almenys una modalitat activa)", fontWeight = FontWeight.Medium)
                            own.filter { it.status.equals("published", true) }.forEach { ownEvent ->
                                OutlinedButton(enabled = !busy, onClick = {
                                    selectedSaleEvent = ownEvent.eventId
                                    sellerSale = null
                                    saleUncertain = false
                                    page = "sale"; busy = true
                                    scope.launch {
                                        try { sellerSale = api.sellerSaleOptions(ownEvent.eventId) }
                                        catch (ex: Exception) { error = ex.message ?: "No es pot consultar la venda" }
                                        finally { busy = false }
                                    }
                                }, modifier = Modifier.fillMaxWidth()) { Text("Gestionar venda: ${ownEvent.title}") }
                            }
                        }
                        if (sellerUncertain) {
                            Text("Resposta incerta després d'enviar. Consulta l'estat abans de tornar a intentar-ho, per evitar duplicats.",
                                color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            TextButton(enabled = !busy, onClick = {
                                busy = true
                                scope.launch {
                                    try { seller = api.sellerOnboardingStatus(); sellerUncertain = false }
                                    catch (ex: Exception) { error = ex.message ?: "No es pot comprovar l'alta" }
                                    finally { busy = false }
                                }
                            }) { Text("Consultar titulars, no repetir l'alta") }
                        } else if (IosSellerLegalPolicy.policiesUsable(state)) {
                            val publishedOwn = own.filter { it.status.equals("published", true) }
                            if (publishedOwn.isEmpty()) Text("Publica primer una activitat pròpia i espera que el servidor confirme la publicació.")
                            else IosSellerActivationForm(publishedOwn, state,
                                enabled = !busy, onSave = { draft ->
                                    busy = true; error = ""
                                    scope.launch {
                                        try {
                                            seller = api.sellerActivate(draft)
                                            if (api.activePersonalId() == expectedOwnerId) {
                                                onNotice("Titular fiscal i condicions confirmats. Cal configurar les entrades i el TPV per vendre.")
                                                sellerUncertain = false
                                                page = "seller"
                                            }
                                        } catch (ex: Exception) {
                                            sellerUncertain = true
                                            error = ex.message ?: "No s'ha confirmat l'alta. Consulta el servidor abans de reintentar."
                                        } finally {
                                            busy = false
                                            if (page == "menu" && snapshot == null) load()
                                        }
                                    }
                                })
                        }
                    }
                } else if (page == "sale") {
                    Text("Activació efectiva de la venda al servidor", fontWeight = FontWeight.SemiBold)
                    Text("Els tipus d'entrada es configuren en el panell professional. Este control només activa o desactiva les opcions ja creades.", fontSize = 12.sp)
                    val currentSale = sellerSale
                    if (currentSale == null) {
                        Text(if (saleUncertain) "El resultat de l'últim canvi és incert. Consulta l'estat al servidor abans de tornar a tocar la venda."
                            else "Carregant venda de l'activitat...", fontSize = 12.sp)
                        TextButton(enabled = !busy, onClick = {
                            busy = true
                            scope.launch {
                                try { sellerSale = api.sellerSaleOptions(selectedSaleEvent); saleUncertain = false }
                                catch (ex: Exception) { error = ex.message ?: "No s'ha recuperat l'estat de venda" }
                                finally { busy = false }
                            }
                        }) { Text("Consultar l'estat al servidor") }
                    } else {
                        Text(if (currentSale.enabled) "Venda activada pel servidor" else "Venda desactivada",
                            fontWeight = FontWeight.Bold)
                        Text("Modalitats actives: ${currentSale.activeOptions} de ${currentSale.totalOptions}", fontSize = 12.sp)
                        val canEnableSale = seller?.let {
                            IosSellerSalePolicy.canEnable(currentSale, it,
                                own.filter { ev -> ev.status.equals("published", true) }.map { ev -> ev.eventId }.toSet())
                        } == true
                        if (!currentSale.enabled && currentSale.activeOptions == 0)
                            Text("Primer crea o importa modalitats i preus des del panell de gestió d'entrades.", fontSize = 12.sp)
                        Button(enabled = !busy && (currentSale.enabled || canEnableSale), modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                busy = true; error = ""
                                scope.launch {
                                    try {
                                        sellerSale = api.sellerSetSaleEnabled(selectedSaleEvent, !currentSale.enabled)
                                        saleUncertain = false
                                        onNotice(if (sellerSale?.enabled == true) "Venda activada i verificada amb el servidor."
                                            else "Venda desactivada i verificada amb el servidor.")
                                    } catch (ex: Exception) {
                                        sellerSale = null
                                        saleUncertain = true
                                        error = ex.message ?: "No es pot confirmar el canvi; consulta el servidor."
                                    } finally { busy = false }
                                }
                            }) { Text(if (currentSale.enabled) "Desactivar venda" else "Activar venda") }
                    }
                } else if (page == "advertising") {
                    IosAdvertisingPanel(api, expectedOwnerId, registered, own, onAuth, onNotice)
                } else if (page == "donations") {
                    IosArtistDonationManager(api, expectedOwnerId, onNotice)
                } else if (page == "services") {
                    if (!registered) Button(onClick = onAuth) { Text("Iniciar sessió") }
                    else {
                        IosSpecialServicesManager(api, expectedOwnerId, onNotice)
                        HorizontalDivider()
                        IosSpecialServicesRedeem(api)
                    }
                } else if (page == "commerce") {
                    if (!registered) {
                        Text("Cal iniciar sessió per a gestionar els productes d'un artista.")
                        Button(onClick = onAuth) { Text("Iniciar sessió") }
                    } else {
                        IosArtistStoreManager(api, expectedOwnerId, onNotice)
                        
                    }
                }
            }
        }
    }
}

@Composable
private fun IosPublisherEventEditor(initial: IosPublishedEvent?, enabled: Boolean, onSave: (IosPublishDraft) -> Unit) {
    var title by remember(initial?.eventId) { mutableStateOf(initial?.title.orEmpty()) }
    var subtitle by remember(initial?.eventId) { mutableStateOf(initial?.subtitle.orEmpty()) }
    var date by remember(initial?.eventId) { mutableStateOf(initial?.date.orEmpty()) }
    var time by remember(initial?.eventId) { mutableStateOf(initial?.time.orEmpty()) }
    var endDate by remember(initial?.eventId) { mutableStateOf(initial?.endDate.orEmpty()) }
    var endTime by remember(initial?.eventId) { mutableStateOf(initial?.endTime.orEmpty()) }
    var town by remember(initial?.eventId) { mutableStateOf(initial?.town.orEmpty()) }
    var comarca by remember(initial?.eventId) { mutableStateOf(initial?.comarca.orEmpty()) }
    var venue by remember(initial?.eventId) { mutableStateOf(initial?.venue.orEmpty()) }
    var description by remember(initial?.eventId) { mutableStateOf(initial?.description.orEmpty()) }
    var artists by remember(initial?.eventId) { mutableStateOf(initial?.artists.orEmpty()) }
    var artistDraft by remember(initial?.eventId) { mutableStateOf("") }
    var organizers by remember(initial?.eventId) { mutableStateOf(initial?.organizers?.joinToString(", ").orEmpty()) }
    var styles by remember(initial?.eventId) { mutableStateOf(initial?.styles?.joinToString(", ").orEmpty()) }
    var category by remember(initial?.eventId) { mutableStateOf(initial?.category?.takeIf { it in IosPublishDraft.categories } ?: "OTHER") }
    var price by remember(initial?.eventId) { mutableStateOf(initial?.price?.toString() ?: "0") }
    var ticketChannel by remember(initial?.eventId) { mutableStateOf(if (initial?.ticketEnabled == true) if (initial.ticketUrl.isNotBlank()) "EXTERNAL" else "AGENDA" else "NONE") }
    var ticketUrl by remember(initial?.eventId) { mutableStateOf(initial?.ticketUrl.orEmpty()) }
    var localError by remember(initial?.eventId) { mutableStateOf("") }
    Text(if (initial == null) "Publicar un esdeveniment cultural" else "Editar l'esdeveniment ${initial.eventId}", fontWeight = FontWeight.Bold)
    OutlinedTextField(title, { title = it.take(220) }, label = { Text("Títol") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(subtitle, { subtitle = it.take(255) }, label = { Text("Subtítol") }, modifier = Modifier.fillMaxWidth())
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        OutlinedTextField(date, { date = it.take(10) }, label = { Text("Data AAAA-MM-DD") }, modifier = Modifier.weight(1f))
        OutlinedTextField(time, { time = it.take(5) }, label = { Text("Hora HH:MM") }, modifier = Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        OutlinedTextField(endDate, { endDate = it.take(10) }, label = { Text("Data final (opcional)") }, modifier = Modifier.weight(1f))
        OutlinedTextField(endTime, { endTime = it.take(5) }, label = { Text("Hora final") }, modifier = Modifier.weight(1f))
    }
    OutlinedTextField(town, { town = it.take(160) }, label = { Text("Poble") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(comarca, { comarca = it.take(160) }, label = { Text("Comarca") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(venue, { venue = it.take(220) }, label = { Text("Sala o ubicació") }, modifier = Modifier.fillMaxWidth())
    Text("Disciplina cultural", fontSize = 12.sp)
    Column {
        IosPublishDraft.categories.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { key -> FilterChip(selected = category == key, onClick = { category = key },
                    label = { Text(key.lowercase().replace('_',' '), fontSize = 9.sp) }) }
            }
        }
    }
    Text("Artistes i participants de la programació", fontWeight = FontWeight.Medium)
    artists.forEach { artist ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(artist, modifier = Modifier.weight(1f))
            TextButton(enabled = enabled, onClick = { artists = artists.filterNot { it == artist } }) { Text("Llevar") }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        OutlinedTextField(artistDraft, { artistDraft = it.take(160) }, label = { Text("Afegir artista") }, modifier = Modifier.weight(1f))
        OutlinedButton(enabled = enabled && artistDraft.isNotBlank(), onClick = {
            val name = artistDraft.trim()
            if (name.isNotBlank() && artists.none { it.equals(name, ignoreCase = true) }) artists = artists + name
            artistDraft = ""
        }) { Text("Afegir") }
    }
    OutlinedTextField(organizers, { organizers = it }, label = { Text("Organitzadors") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(styles, { styles = it }, label = { Text("Estils") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(description, { description = it.take(20000) }, label = { Text("Descripció") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
    OutlinedTextField(price, { price = it.take(8) }, label = { Text("Preu (€)") }, modifier = Modifier.fillMaxWidth())
    Text("Entrades", fontWeight = FontWeight.Medium)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("NONE" to "Sense venda", "EXTERNAL" to "Externa", "AGENDA" to "L'Agenda").forEach { (key, label) ->
            FilterChip(selected = ticketChannel == key, onClick = { ticketChannel = key }, label = { Text(label) })
        }
    }
    if (ticketChannel == "EXTERNAL") OutlinedTextField(ticketUrl, { ticketUrl = it }, label = { Text("URL https") }, modifier = Modifier.fillMaxWidth())
    if (ticketChannel == "AGENDA") Text("Es publica primer; per vendre cal compte registrat i facturació validada.", fontSize = 12.sp)
    if (localError.isNotBlank()) Text(localError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
    Button(enabled = enabled, modifier = Modifier.fillMaxWidth(), onClick = {
        try {
            val draft = IosPublishDraft(title,date,time,town,category,
                (artists + artistDraft.split(',').map(String::trim).filter(String::isNotBlank)).distinct(),
                organizers.split(',').map(String::trim).filter(String::isNotEmpty),
                subtitle, endDate, endTime, comarca,venue,
                styles.split(',').map(String::trim).filter(String::isNotEmpty), price.toIntOrNull() ?: -1,
                ticketChannel,ticketUrl,description)
            draft.validate(); localError = ""; onSave(draft)
        } catch (ex: Exception) { localError = ex.message ?: "Formulari incorrecte" }
    }) { Text(if (initial == null) "Publicar" else "Guardar canvis") }
}

@Composable
private fun IosProfessionalClaimForm(enabled: Boolean, onSend: (String,String,String,String,String) -> Unit) {
    var entity by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("GROUP") }
    var relation by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    Text("Reclamar un perfil professional", fontWeight = FontWeight.Bold)
    OutlinedTextField(entity, { entity = it }, label = { Text("Nom del grup o organització") }, modifier = Modifier.fillMaxWidth())
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        FilterChip(type == "GROUP", onClick = { type = "GROUP" }, label = { Text("Artista / grup") })
        FilterChip(type == "AGENCY", onClick = { type = "AGENCY" }, label = { Text("Organització") })
    }
    OutlinedTextField(relation, { relation = it }, label = { Text("Quina relació tens amb l'entitat?") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(contact, { contact = it }, label = { Text("Correu o telèfon") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(message, { message = it }, label = { Text("Informació addicional") }, modifier = Modifier.fillMaxWidth())
    Button(enabled = enabled && entity.isNotBlank() && relation.isNotBlank() && contact.isNotBlank(),
        onClick = { onSend(entity, type, relation, contact, message) }) { Text("Enviar sol·licitud") }
    Text("La sol·licitud no concedix permisos automàtics. Administració ha de verificar el perfil.", fontSize = 11.sp)
}


@Composable
private fun IosSellerActivationForm(events: List<IosPublishedEvent>, state: IosSellerStatus,
    enabled: Boolean, onSave: (IosSellerActivation) -> Unit) {
    val uriHandler = LocalUriHandler.current
    var selectedEvent by remember(events) { mutableIntStateOf(events.firstOrNull()?.eventId ?: 0) }
    var selectedCompany by remember(state) { mutableIntStateOf(state.companies.firstOrNull()?.id ?: 0) }
    var kind by remember { mutableStateOf("PERSON") }
    var displayName by remember { mutableStateOf("") }
    var taxId by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("") }
    var town by remember { mutableStateOf("") }
    var province by remember { mutableStateOf("") }
    var privacyViewed by remember(state.privacyUrl) { mutableStateOf(false) }
    var salesViewed by remember(state.salesTermsUrl) { mutableStateOf(false) }
    var privacy by remember(state.privacyUrl) { mutableStateOf(false) }
    var terms by remember(state.salesTermsUrl) { mutableStateOf(false) }
    var failure by remember { mutableStateOf("") }
    val existing = IosSellerLegalPolicy.selectedCompany(state, selectedCompany)
    val previouslyAccepted = existing?.noticesCurrent == true

    Text("Activitat publicada que vendrà entrades", fontWeight = FontWeight.Medium)
    events.forEach { event -> Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = event.eventId == selectedEvent, onClick = { selectedEvent = event.eventId })
        Text("${event.title} · ${event.date}", maxLines = 2)
    } }
    if (state.companies.isNotEmpty()) {
        Text("Titular fiscal", fontWeight = FontWeight.Medium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selectedCompany == 0, onClick = { selectedCompany = 0 })
            Text("Afegir titular fiscal nou")
        }
        state.companies.forEach { company ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selectedCompany == company.id, onClick = { selectedCompany = company.id })
                Column {
                    Text(company.displayName.ifBlank { "Titular #${company.id}" })
                    Text(if (company.noticesCurrent) "Condicions vigents acceptades" else "Acceptacions pendents o caducades", fontSize = 11.sp)
                }
            }
        }
    }
    if (selectedCompany == 0) {
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            FilterChip(kind == "PERSON", onClick = { kind = "PERSON" }, label = { Text("Persona / autònom") })
            FilterChip(kind == "COMPANY", onClick = { kind = "COMPANY" }, label = { Text("Empresa") })
        }
        OutlinedTextField(displayName, { displayName = it }, label = { Text("Nom complet o empresa") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(taxId, { taxId = it.uppercase().take(24) }, label = { Text("DNI/NIE/CIF") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(address, { address = it }, label = { Text("Adreça fiscal") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(postalCode, { postalCode = it.filter(Char::isDigit).take(5) }, label = { Text("Codi postal") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(town, { town = it }, label = { Text("Població") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(province, { province = it }, label = { Text("Província") }, modifier = Modifier.fillMaxWidth())
    }
    if (previouslyAccepted) {
        Text("El servidor confirma que este titular ja té acceptades les condicions actuals.", fontSize = 12.sp)
    } else {
        Text("Llig els dos documents abans d'acceptar-los.", fontSize = 12.sp)
        TextButton(onClick = {
            try { uriHandler.openUri(state.privacyUrl); privacyViewed = true }
            catch (ex: Exception) { failure = "No s'ha pogut obrir la política de privacitat." }
        }) { Text("Llegir la política de privacitat") }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = privacy, onCheckedChange = { privacy = it }, enabled = privacyViewed)
            Text("He llegit la política de privacitat", fontSize = 12.sp)
        }
        TextButton(onClick = {
            try { uriHandler.openUri(state.salesTermsUrl); salesViewed = true }
            catch (ex: Exception) { failure = "No s'han pogut obrir les condicions de venda." }
        }) { Text("Llegir les condicions de venda") }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = terms, onCheckedChange = { terms = it }, enabled = salesViewed)
            Text("Accepte les condicions de venda d'entrades", fontSize = 12.sp)
        }
    }
    if (failure.isNotBlank()) Text(failure, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
    val draft = IosSellerActivation(selectedEvent, selectedCompany, kind, displayName, taxId,
        address, postalCode, town, province, previouslyAccepted || privacy,
        previouslyAccepted || terms, privacyViewed, salesViewed)
    val allowed = IosSellerLegalPolicy.canSubmit(draft, state, true, events.map { it.eventId }.toSet())
    Button(enabled = enabled && allowed, modifier = Modifier.fillMaxWidth(), onClick = {
        try { draft.validate(); failure = ""; onSave(draft) }
        catch (ex: Exception) { failure = ex.message ?: "Falten dades fiscals" }
    }) { Text("Confirmar titular fiscal i continuar") }
    Text("Les entrades i els pagaments no s'activen fins que es configure la venda en el servidor.", fontSize = 11.sp)
}
