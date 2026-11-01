package com.example.agendavalenciana.ui

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agendavalenciana.data.ServerApiRepository
import com.example.agendavalenciana.state.AgendaState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

private data class SellerCompany(
    val id: Int, val displayName: String, val taxId: String, val address: String,
    val postalCode: String, val town: String, val province: String, val kind: String
)
private data class SellerEvent(
    val id: Int, val title: String, val date: String, val town: String,
    val status: String, val enabled: Boolean, val assignedCompanyId: Int?, val sold: Int, val orders: Int,
    val grossCents: Long
)
private data class SellerSale(
    val id: Long, val eventId: Int, val eventTitle: String, val date: String,
    val quantity: Int, val totalCents: Long, val optionName: String
)
private data class SellerInvoice(
    val number: String, val company: String, val issuedAt: String,
    val amountCents: Long, val status: String
)
private data class SellerData(
    val companies: List<SellerCompany>, val events: List<SellerEvent>,
    val sales: List<SellerSale>, val invoices: List<SellerInvoice>,
    val sold: Int, val orders: Int, val grossCents: Long, val eventsTruncated: Boolean
)

private fun jsonObjects(arr: JSONArray?): List<JSONObject> = buildList {
    if (arr != null) for (index in 0 until arr.length()) arr.optJSONObject(index)?.let(::add)
}
private fun JSONObject.sellerCompany(): SellerCompany = SellerCompany(
    optInt("id"), optString("display_name"), optString("tax_id"),
    optString("address"), optString("postal_code"), optString("town"),
    optString("province"), optString("kind")
)
private fun JSONObject.sellerEvent(): SellerEvent = SellerEvent(
    optInt("id"), optString("title"), optString("event_date"), optString("town"),
    optString("status"), optInt("ticket_enabled", 0) == 1,
    optInt("company_id", 0).takeIf { it > 0 }, optInt("sold"), optInt("orders"),
    optLong("gross_cents")
)
private fun JSONObject.sellerSale(): SellerSale = SellerSale(
    optLong("id"), optInt("event_id"), optString("event_title"),
    optString("paid_at"), optInt("quantity"), optLong("total_cents"),
    optString("option_name", "Entrada general")
)
private fun JSONObject.sellerInvoice(): SellerInvoice = SellerInvoice(
    optString("invoice_number"), optString("company"), optString("issued_at"),
    optLong("total_cents"), optString("status")
)
private fun JSONObject.sellerData(): SellerData = SellerData(
    jsonObjects(optJSONArray("companies")).map { it.sellerCompany() },
    jsonObjects(optJSONArray("events")).map { it.sellerEvent() },
    jsonObjects(optJSONArray("sales")).map { it.sellerSale() },
    jsonObjects(optJSONArray("invoices")).map { it.sellerInvoice() },
    optInt("tickets_sold"), optInt("orders_count"), optLong("gross_cents"),
    optBoolean("events_truncated", false)
)
private fun euros(cents: Long): String = NumberFormat.getCurrencyInstance(Locale("ca", "ES")).format(cents / 100.0)

/**
 * V287: les xifres s'obtenen EXCLUSIVAMENT de comandes cobrades al servidor.
 * Sense API desplegada no apareixen imports inventats ni es guarden empreses en local.
 */
@Composable
internal fun TicketManagementPublisherScreen(
    state: AgendaState,
    contentPadding: PaddingValues,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accountId = state.currentAccount?.let { it.serverUserId ?: it.id }
    var inTicketEditor by remember { mutableStateOf(false) }
    var selectedTicketEvent by remember { mutableStateOf<SellerEvent?>(null) }
    var tab by remember { mutableStateOf("Resum") }
    var eventFilter by remember { mutableStateOf("Totes") }
    var companyFilter by remember { mutableStateOf(0) }
    var period by remember { mutableStateOf("all") }
    var revision by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var data by remember { mutableStateOf<SellerData?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(accountId) {
        inTicketEditor = false
        selectedTicketEvent = null
        data = null
    }
    // v301: la sessió d'AgendaState ja identifica l'usuari. No repetir el login
    // ni reclamar contrasenya a un compte autenticat (també funciona amb Google).
    // Les operacions econòmiques continuen validades pel token i els permisos de l'API.
    if (accountId == null) {
        Box(Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
            Text("Inicia sessió a L’Agenda per gestionar les entrades.")
        }
        return
    }
    // L'API és qui decidix si el compte és professional autoritzat, també si
    // l'accés prové d'un perfil professional reclamat i el rol local no s'ha actualitzat.
    // Mai fem una altra pantalla de login ni confiem en un permís només local.
    selectedTicketEvent?.let { event ->
        SellerTicketTypesEditor(event.id, event.title, contentPadding,
            onBack = { selectedTicketEvent = null; revision++ })
        return
    }
    if (inTicketEditor) {
        TicketLegacyPublisherScreen(
            state = state, contentPadding = contentPadding,
            onBack = { inTicketEditor = false; revision++ }
        )
        return
    }

    LaunchedEffect(accountId, companyFilter, period, revision) {
        loading = true
        error = null
        val result = withContext(Dispatchers.IO) {
            ServerApiRepository.getAuthenticatedJson(
                context, "professional-seller/dashboard.php?company_id=$companyFilter&period=$period"
            ).mapCatching { it.sellerData() }
        }
        result.onSuccess { data = it }.onFailure {
            data = null
            error = it.message ?: "No es poden carregar les dades professionals."
        }
        loading = false
    }

    Column(
        Modifier.fillMaxSize().padding(contentPadding).verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        TextButton(onClick = onBack) { Text("← Tornar") }
        Text("Panell professional", fontSize = 25.sp, fontWeight = FontWeight.Black)
        Text("Vendes d'entrades, ingressos, esdeveniments i facturació", color = MaterialTheme.colorScheme.onSurfaceVariant)
        val choices = data?.companies.orEmpty()
        if (data != null && choices.isEmpty()) {
            Text("Primer pas: registra les dades fiscals de l'empresa o autònom que vendrà les entrades.",fontWeight = FontWeight.SemiBold)
            Button(onClick = { tab = "Empreses" },modifier=Modifier.fillMaxWidth()) { Text("+ Registrar empresa") }
        }
        if (choices.isNotEmpty()) {
            Text("Empresa de facturació", fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                FilterChip(companyFilter == 0, onClick = { companyFilter = 0 }, label = { Text("Totes") })
                choices.forEach { company ->
                    FilterChip(companyFilter == company.id, onClick = { companyFilter = company.id }, label = { Text(company.displayName) })
                }
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("Resum", "Vendes", "Esdeveniments", "Factures", "Empreses").forEach { name ->
                FilterChip(tab == name, onClick = { tab = name }, label = { Text(name) })
            }
        }
        if (loading) Text("Actualitzant dades del servidor…")
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Text("Per a activar este panell cal desplegar l'API i la migració SQL incloses en el ZIP v287. No es mostren dades fictícies.", fontSize = 12.sp)
            OutlinedButton(onClick = { revision++ }) { Text("Tornar a provar") }
        }
        notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        if (data != null && error == null) {
            val current = data!!
            when (tab) {
                "Resum" -> {
                    Text("Període", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf("all" to "Tot", "30d" to "30 dies", "7d" to "7 dies").forEach { (key, label) ->
                            FilterChip(period == key, onClick = { period = key }, label = { Text(label) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        SellerMetric("Ingressos bruts", euros(current.grossCents), Modifier.weight(1f))
                        SellerMetric("Entrades venudes", current.sold.toString(), Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        SellerMetric("Comandes cobrades", current.orders.toString(), Modifier.weight(1f))
                        SellerMetric("Esdeveniments", (if (current.eventsTruncated) "500+" else current.events.size.toString()), Modifier.weight(1f))
                    }
                    Text("Ingressos bruts de comandes pagades; no són beneficis ni imports liquidats. No inclouen comissions, devolucions ni impostos desglossats.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Esdeveniments amb més ingressos", fontWeight = FontWeight.Bold)
                    current.events.sortedByDescending { it.grossCents }.take(5).forEach { event ->
                        SellerEventLine(event, onConfigure = {
                            if (event.assignedCompanyId == null) {
                                tab = "Esdeveniments"
                                notice = "Assigna una empresa fiscal a este esdeveniment abans de configurar les entrades."
                            } else selectedTicketEvent = event
                        })
                    }
                    if (current.events.isEmpty()) Text("Encara no hi ha esdeveniments vinculats al compte.")
                    OutlinedButton(onClick = { revision++ }, modifier = Modifier.fillMaxWidth()) { Text("Actualitzar dades") }
                }
                "Vendes" -> {
                    Text("Últimes comandes cobrades", fontWeight = FontWeight.Bold)
                    if (current.sales.isEmpty()) Text("No hi ha vendes pagades en este període.")
                    current.sales.forEach { sale ->
                        SellerPanelCard {
                            Text(sale.eventTitle, fontWeight = FontWeight.SemiBold)
                            Text("#${sale.id} · ${sale.date} · ${sale.optionName}", fontSize = 12.sp)
                            Text("${sale.quantity} entrada/es · ${euros(sale.totalCents)}", fontWeight = FontWeight.Bold)
                        }
                    }
                    if (current.sales.size >= 100) Text("Només es mostren les 100 últimes comandes. El total del resum inclou totes les del període.", fontSize = 12.sp)
                    OutlinedButton(onClick = {
                        val report = buildString {
                            append("ID;Esdeveniment;Data de cobrament;Tipus;Quantitat;Import EUR\n")
                            current.sales.forEach { s ->
                                fun cell(v: String) = "\"" + v.replace("\"", "\"\"") + "\""
                                append("${s.id};${cell(s.eventTitle)};${cell(s.date)};${cell(s.optionName)};${s.quantity};${s.totalCents / 100}.${(s.totalCents % 100).toString().padStart(2, '0')}\n")
                            }
                        }
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Vendes l'Agenda")
                            putExtra(Intent.EXTRA_TEXT, report)
                        }
                        context.startActivity(Intent.createChooser(send, "Exportar vendes (CSV)"))
                    }, enabled = current.sales.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                        Text("Compartir informe CSV de les comandes mostrades")
                    }
                }
                "Esdeveniments" -> {
                    Text("Esdeveniments en venda d'entrades", fontWeight = FontWeight.Bold)
                    if (current.companies.isEmpty()) {
                        Text("Abans d'activar la venda, crea una empresa amb les dades fiscals i confirma que has llegit els documents.\n", fontSize = 13.sp)
                        Button(onClick = { tab = "Empreses" }) { Text("Crear empresa") }
                    }
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf("Totes", "Actives", "Finalitzades", "Cancel·lades", "Liquidades").forEach { filter ->
                            FilterChip(eventFilter == filter, onClick = { eventFilter = filter }, label = { Text(filter) })
                        }
                    }
                    if (eventFilter == "Liquidades") {
                        Text("Les liquidacions bancàries i les seues comissions encara no estan integrades amb el proveïdor de pagaments. No es pot certificar cap venda com a liquidada.",fontSize = 12.sp)
                    }
                    val today = java.time.LocalDate.now().toString()
                    val filteredEvents = current.events.filter { e -> when(eventFilter) {
                        "Actives" -> e.enabled && e.status != "cancelled" && e.date >= today
                        "Finalitzades" -> e.date < today && e.status != "cancelled"
                        "Cancel·lades" -> e.status == "cancelled" || e.status == "canceled"
                        "Liquidades" -> false
                        else -> true
                    } }
                    filteredEvents.forEach { event ->
                        SellerPanelCard {
                            SellerEventLine(event, onConfigure = {
                            if (event.assignedCompanyId == null) {
                                tab = "Esdeveniments"
                                notice = "Assigna una empresa fiscal a este esdeveniment abans de configurar les entrades."
                            } else selectedTicketEvent = event
                        })
                            val existing = current.companies.firstOrNull { it.id == event.assignedCompanyId }
                            Text("Factura amb: ${existing?.displayName ?: "Sense empresa assignada"}", fontSize = 12.sp)
                            var pendingCompany by remember(event.id, event.assignedCompanyId) { mutableStateOf(event.assignedCompanyId ?: 0) }
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                current.companies.forEach { c ->
                                    FilterChip(pendingCompany == c.id, onClick = { pendingCompany = c.id }, label = { Text(c.displayName) })
                                }
                            }
                            if (current.companies.isNotEmpty()) Button(
                                enabled = pendingCompany > 0 && pendingCompany != event.assignedCompanyId,
                                onClick = {
                                    scope.launch {
                                        val result = withContext(Dispatchers.IO) {
                                            ServerApiRepository.postAuthenticatedJson(context, "professional-seller/assign-company.php", JSONObject()
                                                .put("event_id", event.id).put("company_id", pendingCompany))
                                        }
                                        result.onSuccess { notice = "Empresa assignada a ${event.title}."; revision++ }
                                            .onFailure { notice = it.message ?: "No s'ha pogut assignar l'empresa." }
                                    }
                                }
                            ) { Text("Guardar empresa") }
                        }
                    }
                    if (filteredEvents.isEmpty() && eventFilter != "Liquidades") Text("No hi ha esdeveniments amb este filtre.")
                    Text("Tria «Gestionar entrades i preus» en cada esdeveniment per crear entrada general, reduïda, VIP, sectors i sessions.", fontSize = 12.sp)
                }
                "Factures" -> {
                    Text("Documents de facturació", fontWeight = FontWeight.Bold)
                    Text("Les comandes pagades no equivalen a factures emeses. Només consten ací documents emesos pel sistema de facturació i vinculats a la teua empresa.", fontSize = 13.sp)
                    if (current.invoices.isEmpty()) Text("Encara no hi ha factures emeses registrades. La generació fiscal i els PDF requerixen completar la integració de facturació.")
                    current.invoices.forEach { invoice -> SellerPanelCard {
                        Text(invoice.number, fontWeight = FontWeight.Bold)
                        Text("${invoice.company} · ${invoice.issuedAt}")
                        Text("${euros(invoice.amountCents)} · ${invoice.status}")
                    } }
                    SellerInvoiceForm(current.companies, onSave = { payload ->
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                ServerApiRepository.postAuthenticatedJson(context,"professional-seller/invoices.php",payload)
                            }
                            result.onSuccess { notice = "Referència de factura externa registrada."; revision++ }
                                .onFailure { notice = it.message ?: "No s'ha pogut registrar la factura." }
                        }
                    })
                }
                "Empreses" -> {
                    Text("Dades fiscals de les empreses", fontWeight = FontWeight.Bold)
                    Text("Pots afegir tantes empreses com necessites. Una empresa no es pot assignar a un esdeveniment sense permisos de gestió.", fontSize = 13.sp)
                    current.companies.forEach { company -> SellerPanelCard {
                        Text(company.displayName, fontWeight = FontWeight.Bold)
                        Text("${company.taxId} · ${company.address}")
                        Text("${company.postalCode} ${company.town} · ${company.province}")
                    } }
                    SellerCompanyForm(onSave = { json ->
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                ServerApiRepository.postAuthenticatedJson(context, "professional-seller/companies.php", json)
                            }
                            result.onSuccess { notice = "Empresa de facturació guardada."; revision++ }
                                .onFailure { notice = it.message ?: "Error en guardar l'empresa." }
                        }
                    })
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun SellerMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = androidx.compose.foundation.shape.RoundedCornerShape(13.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(13.dp)) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SellerPanelCard(content: @Composable () -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(13.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }
    }
}

@Composable
private fun SellerEventLine(event: SellerEvent, onConfigure: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(event.title, fontWeight = FontWeight.Bold)
        Text("${event.date} · ${event.town} · ${if (event.enabled) "Venda activada" else "Venda desactivada"}", fontSize = 12.sp)
        Text("${event.sold} entrades · ${euros(event.grossCents)} bruts", fontWeight = FontWeight.SemiBold)
        TextButton(onClick = onConfigure) { Text("Gestionar entrades i preus") }
    }
}

@Composable
private fun SellerCompanyForm(onSave: (JSONObject) -> Unit) {
    var kind by remember { mutableStateOf("COMPANY") }
    var name by remember { mutableStateOf("") }
    var taxId by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var postCode by remember { mutableStateOf("") }
    var town by remember { mutableStateOf("") }
    var province by remember { mutableStateOf("") }
    var paymentRead by remember { mutableStateOf(false) }
    var privacyRead by remember { mutableStateOf(false) }
    var paymentAccepted by remember { mutableStateOf(false) }
    var privacyAccepted by remember { mutableStateOf(false) }
    var document by remember { mutableStateOf("") }

    if (document.isNotBlank()) AlertDialog(
        onDismissRequest = { document = "" },
        title = { Text(if (document == "payment") "Condicions de pagament" else "Tractament de dades") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            Text(if (document == "payment")
                "INFORMACIÓ PROVISIONAL (v287) · La venda es cobra mitjançant el proveïdor de pagaments integrat. Els imports del resum són bruts, no les liquidacions. Les despeses, devolucions, retencions, obligacions fiscals i calendaris de pagament s'han de definir al contracte professional definitiu. Este text no substituïx eixe contracte."
            else "INFORMACIÓ PROVISIONAL (v287) · El nom fiscal, DNI/NIF/CIF i adreça es guarden al servidor per a la gestió de l'emissor de les entrades i documents comptables. Només els usuaris autenticats amb permisos sobre esta empresa en poden consultar les dades. Cal publicar la política de privacitat definitiva amb responsable, base jurídica, terminis, drets i destinataris abans d'operar en producció.")
        } },
        confirmButton = { TextButton(onClick = {
            if (document == "payment") paymentRead = true else privacyRead = true
            document = ""
        }) { Text("He llegit el document") } }
    )
    SellerPanelCard {
        Text("Afegir empresa o autònom", fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            FilterChip(kind == "COMPANY", { kind = "COMPANY" }, label = { Text("Empresa") })
            FilterChip(kind == "PERSON", { kind = "PERSON" }, label = { Text("Persona / autònom") })
        }
        OutlinedTextField(name, { name = it }, label = { Text(if (kind == "COMPANY") "Nom de l'empresa" else "Nom i cognoms") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(taxId, { taxId = it.uppercase().filter { c -> c.isLetterOrDigit() || c == '-' } }, label = { Text("DNI / NIE / CIF") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(address, { address = it }, label = { Text("Adreça fiscal") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(postCode, { postCode = it.filter(Char::isDigit).take(5) }, label = { Text("Codi postal") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(town, { town = it }, label = { Text("Poble / municipi") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(province, { province = it }, label = { Text("Província (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        TextButton(onClick = { document = "payment" }) { Text("Llegir les condicions de pagament") }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(paymentAccepted, { paymentAccepted = it }, enabled = paymentRead)
            Text("He llegit i accepte les condicions mostrades", fontSize = 12.sp)
        }
        TextButton(onClick = { document = "privacy" }) { Text("Llegir la informació de tractament de dades") }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(privacyAccepted, { privacyAccepted = it }, enabled = privacyRead)
            Text("He llegit la informació de privacitat", fontSize = 12.sp)
        }
        Text("Els documents v287 són provisionals. El registre fiscal no activa per si sol els cobraments ni les liquidacions.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val valid = name.isNotBlank() && taxId.matches(Regex("[A-Z0-9-]{8,15}")) &&
            address.isNotBlank() && postCode.length == 5 && town.isNotBlank() && paymentAccepted && privacyAccepted
        Button(
            enabled = valid,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                onSave(JSONObject().put("kind", kind).put("display_name", name.trim())
                    .put("tax_id", taxId.trim()).put("address", address.trim())
                    .put("postal_code", postCode).put("town", town.trim())
                    .put("province", province.trim()).put("accept_payment", paymentAccepted)
                    .put("accept_privacy_notice", privacyAccepted))
            }
        ) { Text("Guardar empresa") }
    }
}


/** Registre comptable extern: no emet, signa ni numera factures legalment. */
@Composable
private fun SellerInvoiceForm(companies: List<SellerCompany>, onSave: (JSONObject) -> Unit) {
    var selectedCompany by remember { mutableStateOf(0) }
    var number by remember { mutableStateOf("") }
    var issued by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var alreadyIssued by remember { mutableStateOf(false) }
    val amountValue = remember(amount) { runCatching {
        java.math.BigDecimal(amount.trim().replace(',', '.'))
            .setScale(2,java.math.RoundingMode.UNNECESSARY)
    }.getOrNull()?.takeIf { it.signum() >= 0 && it <= java.math.BigDecimal("9999999999.99") } }
    val validDate = remember(issued) { runCatching { java.time.LocalDate.parse(issued) }.isSuccess }
    SellerPanelCard {
        Text("Registrar factura ja emesa externament", fontWeight = FontWeight.Bold)
        Text("Este registre no genera factures fiscals ni PDF. Introduïx únicament documents emesos per un sistema de facturació legal.",fontSize = 12.sp)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            companies.forEach { company ->
                FilterChip(selectedCompany == company.id,
                    onClick = { selectedCompany = company.id }, label = { Text(company.displayName) })
            }
        }
        OutlinedTextField(number,{number=it.take(80)},label={Text("Número de factura emesa")},modifier=Modifier.fillMaxWidth())
        OutlinedTextField(issued,{issued=it.take(10)},label={Text("Data d'emissió (AAAA-MM-DD)")},modifier=Modifier.fillMaxWidth())
        OutlinedTextField(amount,{amount=it},label={Text("Import total (€)")},modifier=Modifier.fillMaxWidth())
        OutlinedTextField(description,{description=it.take(500)},label={Text("Descripció (opcional)")},modifier=Modifier.fillMaxWidth())
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(alreadyIssued,{alreadyIssued=it})
            Text("Confirme que esta factura ja està emesa fora de L'Agenda",fontSize = 12.sp)
        }
        Button(modifier=Modifier.fillMaxWidth(),
            enabled=companies.any { it.id==selectedCompany } && number.isNotBlank() && validDate && amountValue != null && alreadyIssued,
            onClick = { onSave(JSONObject().put("company_id",selectedCompany)
                .put("invoice_number",number.trim()).put("issued_at",issued)
                .put("total_amount",amountValue.toString()).put("description",description.trim())) }
        ) { Text("Registrar referència") }
    }
}
