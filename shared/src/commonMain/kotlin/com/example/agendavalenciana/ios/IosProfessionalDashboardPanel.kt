package com.example.agendavalenciana.ios

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Pas 43. Compose multiplataforma; totes les dades provenen de professional-seller/dashboard.php.
 * No guardar facturació, vendes o empreses en UserDefaults, cache ni Keychain.
 */
@Composable
internal fun IosProfessionalDashboardPanel(api: IosAgendaApi, expectedOwnerId: Int,
    enabled: Boolean, onFiscalSetup: () -> Unit) {
    val scope = rememberCoroutineScope()
    var data by remember(expectedOwnerId) { mutableStateOf<IosProDashboard?>(null) }
    var loading by remember(expectedOwnerId) { mutableStateOf(false) }
    var busy by remember(expectedOwnerId) { mutableStateOf(false) }
    var uncertain by remember(expectedOwnerId) { mutableStateOf(false) }
    var error by remember(expectedOwnerId) { mutableStateOf("") }
    var notice by remember(expectedOwnerId) { mutableStateOf("") }
    var period by remember(expectedOwnerId) { mutableStateOf("all") }
    var companyFilter by remember(expectedOwnerId) { mutableIntStateOf(0) }
    var revision by remember(expectedOwnerId) { mutableIntStateOf(0) }
    var tab by remember(expectedOwnerId) { mutableStateOf("Resum") }
    var eventFilter by remember(expectedOwnerId) { mutableStateOf("Totes") }
    var editEvent by remember(expectedOwnerId) { mutableStateOf<IosProEvent?>(null) }
    var catalog by remember(expectedOwnerId) { mutableStateOf<IosProTicketCatalog?>(null) }
    var catalogLoading by remember(expectedOwnerId) { mutableStateOf(false) }
    var editOption by remember(expectedOwnerId) { mutableStateOf<IosProTicketOption?>(null) }
    var showOptionForm by remember(expectedOwnerId) { mutableStateOf(false) }

    fun loadDashboard() { revision++ }
    LaunchedEffect(expectedOwnerId, period, companyFilter, revision) {
        loading = true; error = ""; data = null
        try {
            val result = api.professionalDashboard(companyFilter, period)
            check(api.activePersonalId() == expectedOwnerId) { "La sessió ha canviat." }
            data = result; uncertain = false
        } catch (e: Exception) {
            error = e.message ?: "No es pot consultar el panell professional."
        } finally { loading = false }
    }

    fun manageEvent(event: IosProEvent) {
        editEvent = event; catalog = null; showOptionForm = false; catalogLoading = true; error = ""
        scope.launch {
            try { catalog = api.professionalTicketOptions(event.id) }
            catch (e: Exception) { error = e.message ?: "No es poden consultar les modalitats." }
            finally { catalogLoading = false }
        }
    }
    fun applyMutation(success: String, action: suspend () -> Unit) {
        if (busy || uncertain) return
        busy = true; notice = ""; error = ""
        scope.launch {
            try {
                action()
                check(api.activePersonalId() == expectedOwnerId) { "La sessió ha canviat." }
                notice = success
                // Dashboard may have changed while saving; refresh from server.
                loadDashboard()
            } catch (e: Exception) {
                uncertain = true
                error = (e.message ?: "No s'ha confirmat l'operació.") +
                    " Revisa les dades al servidor abans de repetir l'operació."
            } finally { busy = false }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text("Panell professional", fontWeight = FontWeight.Bold)
        Text("Comandes cobrades, modalitats, esdeveniments i facturació del servidor.", style = MaterialTheme.typography.bodySmall)
        if (busy || loading || catalogLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        if (notice.isNotBlank()) Text(notice, style = MaterialTheme.typography.bodySmall)
        if (uncertain) {
            Text("Resultat incert: no es repetirà cap enviament automàtic.", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(enabled = !busy && !loading, onClick = {
                catalog = null; editEvent = null; showOptionForm = false; loadDashboard()
            }) { Text("Consultar l'estat al servidor") }
        }
        val current = data
        if (editEvent != null) {
            val event = editEvent!!
            TextButton(enabled = !busy, onClick = { editEvent = null; catalog = null; loadDashboard() }) { Text("← Esdeveniments") }
            Text(event.title, fontWeight = FontWeight.Bold)
            if (catalog == null) {
                if (!catalogLoading) OutlinedButton(enabled = !busy, onClick = { manageEvent(event) }) { Text("Reconsultar modalitats") }
            } else {
                val cat = catalog!!
                Text("Venda: " + (if (cat.enabled) "Activa" else "Desactivada"))
                Button(enabled = enabled && !busy && !uncertain, onClick = {
                    val goal = !cat.enabled
                    applyMutation("Estat de venda confirmat pel servidor.") {
                        catalog = api.professionalSetSaleEnabled(event.id, goal)
                    }
                }) { Text(if (cat.enabled) "Desactivar venda" else "Activar venda") }
                Text("Els preus i contingents es configuren per modalitat. Una venda activa requereix titular fiscal i condicions legals vigents.",
                    style = MaterialTheme.typography.bodySmall)
                cat.options.forEach { option ->
                    HorizontalDivider()
                    Text("${option.name} · ${IosProfessionalSellerPolicy.euros(option.cents)}", fontWeight = FontWeight.SemiBold)
                    Text("${if (option.active) "Activa" else "Inactiva"} · Venudes ${option.sold} · Reservades ${option.reserved} · Contingent ${option.capacity?.toString() ?: "Sense límit"}",
                        style = MaterialTheme.typography.bodySmall)
                    Text(listOf(option.date, option.time, option.sector, option.offer).filter(String::isNotBlank).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(enabled = enabled && !busy && !uncertain,
                        onClick = { editOption = option; showOptionForm = true }) { Text("Editar modalitat") }
                }
                if (cat.options.isEmpty()) {
                    Text("No hi ha modalitats creades.", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(enabled = enabled && !busy && !uncertain, onClick = {
                        applyMutation("Modalitats importades i comprovades.") {
                            catalog = api.professionalImportTicketDrafts(event.id)
                        }
                    }) { Text("Importar les modalitats de l'esdeveniment") }
                }
                if (!showOptionForm) Button(enabled = enabled && !busy && !uncertain, onClick = {
                    editOption = null; showOptionForm = true
                }) { Text("+ Nova modalitat") }
                if (showOptionForm) IosProfessionalTicketOptionForm(editOption, enabled && !busy && !uncertain,
                    onCancel = { showOptionForm = false }, onSave = { draft ->
                        applyMutation("Modalitat comprovada al servidor.") {
                            catalog = api.professionalSaveTicketOption(event.id, draft)
                            showOptionForm = false
                        }
                    })
            }
        } else {
            if (current != null) {
                if (current.companies.isEmpty()) {
                    Text("Falten les dades fiscals per vendre entrades.", style = MaterialTheme.typography.bodySmall)
                    Button(onClick = onFiscalSetup, enabled = enabled && !busy) { Text("Registrar titular fiscal") }
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    FilterChip(companyFilter == 0, onClick = { companyFilter = 0 }, label = { Text("Totes les empreses") })
                    current.companies.forEach { company -> FilterChip(companyFilter == company.id,
                        onClick = { companyFilter = company.id }, label = { Text(company.name) }) }
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf("Resum", "Vendes", "Esdeveniments", "Factures", "Empreses").forEach { name ->
                        FilterChip(tab == name, onClick = { tab = name }, label = { Text(name) })
                    }
                }
                when (tab) {
                    "Resum" -> {
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            listOf("all" to "Tot", "30d" to "30 dies", "7d" to "7 dies").forEach { (key, label) ->
                                FilterChip(period == key, onClick = { period = key }, label = { Text(label) })
                            }
                        }
                        Text("Ingressos bruts: ${IosProfessionalSellerPolicy.euros(current.grossCents)}", fontWeight = FontWeight.Bold)
                        Text("${current.sold} entrades · ${current.orders} comandes cobrades · ${current.events.size}${if (current.eventsTruncated) "+" else ""} esdeveniments")
                        Text("Els imports bruts no són beneficis ni liquidacions i no desglossen impostos, devolucions ni comissions.",
                            style = MaterialTheme.typography.bodySmall)
                        current.events.sortedByDescending { it.grossCents }.take(5).forEach { event ->
                            HorizontalDivider(); ProEventLine(event)
                            TextButton(onClick = { manageEvent(event) }, enabled = enabled && !busy) { Text("Gestionar entrades") }
                        }
                    }
                    "Vendes" -> {
                        Text("Últimes comandes cobrades", fontWeight = FontWeight.SemiBold)
                        current.sales.forEach { sale ->
                            HorizontalDivider()
                            Text(sale.title, fontWeight = FontWeight.SemiBold)
                            Text("#${sale.id} · ${sale.paidAt} · ${sale.optionName}", style = MaterialTheme.typography.bodySmall)
                            Text("${sale.quantity} entrades · ${IosProfessionalSellerPolicy.euros(sale.cents)}")
                        }
                        if (current.sales.isEmpty()) Text("No consten vendes cobrades en este període.")
                        if (current.sales.size >= 100) Text("Només es mostren les últimes 100 comandes. El resum pot incloure'n més.", style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(enabled = current.sales.isNotEmpty() && IosNativeBridge.current != null, onClick = {
                            IosNativeBridge.current?.shareText(IosProfessionalSellerPolicy.reportCsv(current))
                        }) { Text("Compartir vendes CSV (text)") }
                    }
                    "Esdeveniments" -> {
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Totes", "Actives", "Finalitzades", "Cancel·lades", "Liquidades").forEach { f ->
                                FilterChip(eventFilter == f, onClick = { eventFilter = f }, label = { Text(f) })
                            }
                        }
                        if (eventFilter == "Liquidades") Text("Les liquidacions bancàries no estan acreditades per esta API.", style = MaterialTheme.typography.bodySmall)
                        val filtered = current.events.filter { ev -> when (eventFilter) {
                            "Actives" -> ev.enabled && ev.date >= currentLocalDateIso() && ev.status.lowercase() !in setOf("cancelled", "canceled")
                            "Cancel·lades" -> ev.status.lowercase() in setOf("cancelled", "canceled")
                            "Finalitzades" -> ev.date < currentLocalDateIso() && ev.status.lowercase() !in setOf("cancelled", "canceled")
                            "Liquidades" -> false
                            else -> true
                        } }
                        filtered.forEach { ev ->
                            HorizontalDivider(); ProEventLine(ev)
                            val assigned = current.companies.firstOrNull { it.id == ev.companyId }
                            Text("Titular: ${assigned?.name ?: "Sense empresa assignada"}", style = MaterialTheme.typography.bodySmall)
                            var companyToAssign by remember(ev.id, ev.companyId) { mutableIntStateOf(ev.companyId ?: 0) }
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                current.companies.forEach { company -> FilterChip(companyToAssign == company.id,
                                    onClick = { companyToAssign = company.id }, label = { Text(company.name) }) }
                            }
                            Button(enabled = enabled && !busy && !uncertain && companyToAssign > 0 && companyToAssign != ev.companyId,
                                onClick = {
                                    applyMutation("Assignació fiscal comprovada.") {
                                        api.professionalAssignCompany(ev.id, companyToAssign)
                                    }
                                }) { Text("Guardar titular de l'activitat") }
                            OutlinedButton(enabled = enabled && !busy && ev.companyId != null,
                                onClick = { manageEvent(ev) }) { Text("Gestionar entrades i preus") }
                        }
                        if (filtered.isEmpty() && eventFilter != "Liquidades") Text("No hi ha esdeveniments amb este filtre.")
                    }
                    "Factures" -> {
                        Text("Només referències de factures ja emeses externament; no són factures generades per L’Agenda.",
                            style = MaterialTheme.typography.bodySmall)
                        current.invoices.forEach { invoice ->
                            HorizontalDivider()
                            Text(invoice.number, fontWeight = FontWeight.SemiBold)
                            Text("${invoice.company} · ${invoice.issuedAt} · ${IosProfessionalSellerPolicy.euros(invoice.cents)} · ${invoice.status}",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        if (current.invoices.isEmpty()) Text("No consten factures registrades.")
                        IosProfessionalInvoiceForm(current.companies, enabled && !busy && !uncertain) { company, number, date, cents, description ->
                            applyMutation("Referència fiscal comprovada al servidor.") {
                                api.professionalRegisterExternalInvoice(company, number, date, cents, description, true)
                            }
                        }
                    }
                    "Empreses" -> {
                        current.companies.forEach { c ->
                            HorizontalDivider()
                            Text(c.name, fontWeight = FontWeight.SemiBold)
                            Text("${c.taxId} · ${c.address} · ${c.postcode} ${c.town} · ${c.province}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text("El registre de titulars nous i l'acceptació legal es fan a Alta professional, i no activen la venda automàticament.",
                            style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = onFiscalSetup, enabled = enabled && !busy) { Text("Gestionar alta fiscal") }
                    }
                }
                OutlinedButton(onClick = ::loadDashboard, enabled = enabled && !busy && !loading) { Text("Actualitzar des del servidor") }
            } else if (!loading) OutlinedButton(onClick = ::loadDashboard, enabled = enabled && !busy) { Text("Tornar a consultar el panell") }
        }
    }
}

@Composable
private fun ProEventLine(event: IosProEvent) {
    Text(event.title, fontWeight = FontWeight.SemiBold)
    Text("${event.date} · ${event.town} · ${if (event.enabled) "Venda activada" else "Venda desactivada"}",
        style = MaterialTheme.typography.bodySmall)
    Text("${event.sold} entrades · ${event.orders} comandes · ${IosProfessionalSellerPolicy.euros(event.grossCents)} bruts",
        style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun IosProfessionalTicketOptionForm(existing: IosProTicketOption?, enabled: Boolean,
    onCancel: () -> Unit, onSave: (IosProTicketDraft) -> Unit) {
    var name by remember(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var amount by remember(existing?.id) { mutableStateOf(existing?.cents?.let { "${it / 100},${(it % 100).toString().padStart(2, '0')}" }.orEmpty()) }
    var capacity by remember(existing?.id) { mutableStateOf(existing?.capacity?.toString().orEmpty()) }
    var max by remember(existing?.id) { mutableStateOf((existing?.maxPerOrder ?: 6).toString()) }
    var date by remember(existing?.id) { mutableStateOf(existing?.date.orEmpty()) }
    var time by remember(existing?.id) { mutableStateOf(existing?.time.orEmpty()) }
    var sector by remember(existing?.id) { mutableStateOf(existing?.sector.orEmpty()) }
    var offer by remember(existing?.id) { mutableStateOf(existing?.offer.orEmpty()) }
    var starts by remember(existing?.id) { mutableStateOf(existing?.starts.orEmpty()) }
    var ends by remember(existing?.id) { mutableStateOf(existing?.ends.orEmpty()) }
    var limited by remember(existing?.id) { mutableStateOf(existing?.capacity != null) }
    var byDate by remember(existing?.id) { mutableStateOf(!existing?.starts.isNullOrEmpty() || !existing?.ends.isNullOrEmpty()) }
    var active by remember(existing?.id) { mutableStateOf(existing?.active ?: true) }
    val cents = IosProfessionalSellerPolicy.cents(amount)
    val draft = cents?.takeIf { (!limited || capacity.toIntOrNull() != null) &&
        (!byDate || (starts.isNotBlank() && ends.isNotBlank())) }?.let { IosProTicketDraft(existing?.id ?: 0L, name, it,
        if (limited) capacity.toIntOrNull() else null, max.toIntOrNull() ?: 0,
        date, time, sector, offer, if (byDate) starts else "", if (byDate) ends else "", active) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        HorizontalDivider()
        Text(if (existing == null) "Nova modalitat" else "Editar modalitat", fontWeight = FontWeight.Bold)
        OutlinedTextField(name, { name = it.take(180) }, label = { Text("Nom de l'entrada") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(amount, { amount = it.take(15) }, label = { Text("Preu (€)") }, modifier = Modifier.fillMaxWidth())
        Row { Checkbox(limited, onCheckedChange = { limited = it }); Text("Contingent limitat") }
        if (limited) OutlinedTextField(capacity, { capacity = it.filter(Char::isDigit).take(9) },
            label = { Text("Nombre d'entrades a este preu") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(max, { max = it.filter(Char::isDigit).take(2) }, label = { Text("Màxim per compra (1–20)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(date, { date = it.take(10) }, label = { Text("Data de sessió AAAA-MM-DD (opcional)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(time, { time = it.take(5) }, label = { Text("Hora HH:MM (opcional)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(sector, { sector = it.take(160) }, label = { Text("Zona o sector (opcional)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(offer, { offer = it.take(190) }, label = { Text("Oferta / etiqueta") }, modifier = Modifier.fillMaxWidth())
        Row { Checkbox(byDate, onCheckedChange = { byDate = it }); Text("Límit temporal de venda") }
        if (byDate) {
            OutlinedTextField(starts, { starts = it.take(19) }, label = { Text("Inici AAAA-MM-DD HH:MM:SS") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(ends, { ends = it.take(19) }, label = { Text("Fi AAAA-MM-DD HH:MM:SS") }, modifier = Modifier.fillMaxWidth())
        }
        Row { Checkbox(active, onCheckedChange = { active = it }); Text("Modalitat activa") }
        Text("El contingent no pot ser inferior a les places venudes i reservades.", style = MaterialTheme.typography.bodySmall)
        Button(enabled = enabled && draft != null && IosProfessionalSellerPolicy.validOption(draft, existing),
            onClick = { draft?.let(onSave) }) { Text("Guardar i verificar") }
        TextButton(enabled = enabled, onClick = onCancel) { Text("Cancel·lar") }
    }
}

@Composable
private fun IosProfessionalInvoiceForm(companies: List<IosProCompany>, enabled: Boolean,
    onSave: (Int, String, String, Long, String) -> Unit) {
    var companyId by remember { mutableIntStateOf(0) }
    var number by remember { mutableStateOf("") }
    var issued by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var external by remember { mutableStateOf(false) }
    val cents = IosProfessionalSellerPolicy.cents(amount)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        HorizontalDivider()
        Text("Registrar factura emesa externament", fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            companies.forEach { company -> FilterChip(companyId == company.id, onClick = { companyId = company.id }, label = { Text(company.name) }) }
        }
        OutlinedTextField(number, { number = it.take(80) }, label = { Text("Número de factura") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(issued, { issued = it.take(10) }, label = { Text("Data d'emissió AAAA-MM-DD") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(amount, { amount = it.take(20) }, label = { Text("Import €") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(description, { description = it.take(500) }, label = { Text("Descripció") }, modifier = Modifier.fillMaxWidth())
        Row { Checkbox(external, onCheckedChange = { external = it }); Text("Confirme que ja s'ha emés fora de L’Agenda") }
        Button(enabled = enabled && companyId > 0 && external && number.isNotBlank() &&
            issued.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) && cents != null,
            onClick = { cents?.let { onSave(companyId, number.trim(), issued, it, description) } }) {
            Text("Registrar referència externa")
        }
    }
}
