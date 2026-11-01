package com.example.agendavalenciana.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agendavalenciana.data.ServerApiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private data class SellerTicketType(
    val id: Long, val name: String, val priceCents: Long, val capacity: Int?,
    val sold: Int, val reserved: Int, val maxOrder: Int,
    val date: String, val time: String, val sector: String, val offer: String,
    val starts: String, val ends: String, val active: Boolean
)

private fun JSONObject.sellerTicketType(): SellerTicketType = SellerTicketType(
    optLong("id"), optString("name"), optLong("price_cents"),
    if (isNull("capacity")) null else optInt("capacity"),
    optInt("sold"), optInt("reserved"), optInt("max_per_order", 6),
    optString("session_date").takeUnless { it == "null" }.orEmpty(),
    optString("session_time").takeUnless { it == "null" }.orEmpty(),
    optString("sector").takeUnless { it == "null" }.orEmpty(),
    optString("offer").takeUnless { it == "null" }.orEmpty(),
    optString("sale_starts_at").takeUnless { it == "null" }.orEmpty(),
    optString("sale_ends_at").takeUnless { it == "null" }.orEmpty(),
    optInt("active") == 1 || optBoolean("active")
)

@Composable
internal fun SellerTicketTypesEditor(
    eventId: Int,
    eventTitle: String,
    contentPadding: PaddingValues,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var types by remember(eventId) { mutableStateOf(emptyList<SellerTicketType>()) }
    var selling by remember(eventId) { mutableStateOf(false) }
    var revision by remember(eventId) { mutableStateOf(0) }
    var loading by remember(eventId) { mutableStateOf(false) }
    var saving by remember(eventId) { mutableStateOf(false) }
    var error by remember(eventId) { mutableStateOf<String?>(null) }
    var edit by remember(eventId) { mutableStateOf<SellerTicketType?>(null) }
    var showForm by remember(eventId) { mutableStateOf(false) }

    LaunchedEffect(eventId, revision) {
        loading = true
        val response = withContext(Dispatchers.IO) {
            ServerApiRepository.getAuthenticatedJson(context,"professional-seller/options.php?event_id=$eventId")
        }
        response.onSuccess { obj ->
            selling = obj.optInt("ticket_enabled") == 1
            val array = obj.optJSONArray("options")
            types = buildList {
                if (array != null) for (i in 0 until array.length()) {
                    array.optJSONObject(i)?.let { add(it.sellerTicketType()) }
                }
            }
            error = null
        }.onFailure { error = it.message ?: "No s'han pogut carregar les modalitats." }
        loading = false
    }

    Column(Modifier.fillMaxSize().padding(contentPadding).verticalScroll(rememberScrollState())
        .padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TextButton(onClick = onBack) { Text("← Tornar als esdeveniments") }
        Text(eventTitle, fontWeight = FontWeight.Black, fontSize = 21.sp)
        Text("Un preu per cada combinació de dates, contingent i zona", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Pots combinar els tres criteris: per exemple, Pista · 100 primeres entrades · venda anticipada · 12 €.",
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (loading) Text("Carregant dades de venda…")
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text(if (selling) "Venda activa" else "Venda desactivada", fontWeight = FontWeight.Bold)
        OutlinedButton(enabled = !saving && !loading, onClick = {
            saving = true
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    ServerApiRepository.postAuthenticatedJson(context,"professional-seller/options.php",
                        JSONObject().put("event_id",eventId).put("action","set_enabled").put("enabled",!selling))
                }
                result.onSuccess { revision++ }.onFailure { error = it.message }
                saving = false
            }
        }, modifier = Modifier.fillMaxWidth()) { Text(if (selling) "Desactivar venda" else "Activar venda") }
        Text("Cada modalitat té el seu propi preu i contingent. Per activar la venda cal tindre dades de facturació verificades, condicions vigents acceptades i almenys una modalitat activa.", fontSize = 12.sp)
        types.forEach { option ->
            Surface(Modifier.fillMaxWidth(), border = BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(option.name, fontWeight = FontWeight.Bold)
                    Text("${sellerFormatMoney(option.priceCents)} · ${if (option.active) "Activa" else "Inactiva"}")
                    val remaining = option.capacity?.let { (it - option.sold - option.reserved).coerceAtLeast(0) }
                    Text("Contingent: ${option.capacity?.toString() ?: "Sense límit per modalitat"} · " +
                        "Venudes: ${option.sold} · Reservades: ${option.reserved}" +
                        (remaining?.let { " · Disponibles: $it" } ?: ""), fontSize = 12.sp)
                    if (option.sector.isNotBlank()) Text("Zona: ${option.sector}", fontSize = 12.sp)
                    if (option.starts.isNotBlank() || option.ends.isNotBlank())
                        Text("Període de venda: ${option.starts.ifBlank { "Sense inici" }} → ${option.ends.ifBlank { "Sense fi" }}", fontSize = 12.sp)
                    if (option.date.isNotBlank()) Text("Sessió: ${option.date} ${option.time}", fontSize = 12.sp)
                    if (option.offer.isNotBlank()) Text("Etiqueta: ${option.offer}", fontSize = 12.sp)
                    TextButton(onClick = { edit = option; showForm = true }) { Text("Editar modalitat") }
                }
            }
        }
        if (types.isEmpty() && !loading) Text("Encara no has creat cap modalitat. Afig una fila per cada combinació de preu, zona, dates i contingent.")
        if (types.isEmpty() && !loading) {
            OutlinedButton(enabled = !saving, onClick = {
                saving = true
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        ServerApiRepository.postAuthenticatedJson(context, "events/ticket-drafts.php",
                            JSONObject().put("action", "import").put("event_id", eventId))
                    }
                    result.onSuccess { revision++ }.onFailure {
                        error = it.message ?: "No s'han pogut importar les modalitats."
                    }
                    saving = false
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Recuperar les entrades definides en crear l'esdeveniment")
            }
            Text("Les modalitats preparades s'incorporen sense activar la venda. Completa abans les dades fiscals i les condicions de venda.", fontSize = 12.sp)
        }
        if (!showForm) Button(onClick = { edit = null; showForm = true }, modifier = Modifier.fillMaxWidth()) {
            Text("+ Afegir tipus d'entrada")
        }
        if (showForm) SellerTicketTypeForm(edit, saving, onCancel = { showForm = false }, onSave = { option ->
            saving = true
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    ServerApiRepository.postAuthenticatedJson(context,"professional-seller/options.php",option.put("event_id",eventId).put("action","upsert"))
                }
                result.onSuccess { showForm = false; revision++ }.onFailure { error = it.message }
                saving = false
            }
        })
        Spacer(Modifier.height(24.dp))
    }
}

private fun sellerFormatMoney(cents: Long): String =
    "%s,%02d €".format(cents / 100, (cents % 100).toInt())

@Composable
private fun SellerTicketTypeForm(
    existing: SellerTicketType?,
    saving: Boolean,
    onCancel: () -> Unit,
    onSave: (JSONObject) -> Unit
) {
    var name by remember(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var price by remember(existing?.id) { mutableStateOf(existing?.priceCents?.let {
        "${it / 100}.${(it % 100).toString().padStart(2, '0')}"
    }.orEmpty()) }
    var capacity by remember(existing?.id) { mutableStateOf(existing?.capacity?.toString().orEmpty()) }
    var maxOrder by remember(existing?.id) { mutableStateOf((existing?.maxOrder ?: 6).toString()) }
    var date by remember(existing?.id) { mutableStateOf(existing?.date.orEmpty()) }
    var time by remember(existing?.id) { mutableStateOf(existing?.time.orEmpty()) }
    var sector by remember(existing?.id) { mutableStateOf(existing?.sector.orEmpty()) }
    var offer by remember(existing?.id) { mutableStateOf(existing?.offer.orEmpty()) }
    var active by remember(existing?.id) { mutableStateOf(existing?.active ?: true) }
    var starts by remember(existing?.id) { mutableStateOf(existing?.starts.orEmpty()) }
    var ends by remember(existing?.id) { mutableStateOf(existing?.ends.orEmpty()) }
    // Els criteris són independents i combinables. Inferim els actius en editar.
    var byDates by remember(existing?.id) { mutableStateOf(!existing?.starts.isNullOrBlank() || !existing?.ends.isNullOrBlank()) }
    var byQuantity by remember(existing?.id) { mutableStateOf(existing?.capacity != null) }
    var byZone by remember(existing?.id) { mutableStateOf(!existing?.sector.isNullOrBlank()) }
    val cents = remember(price) { runCatching {
        BigDecimal(price.replace(',', '.')).setScale(2,RoundingMode.UNNECESSARY)
            .movePointRight(2).longValueExact()
    }.getOrNull()?.takeIf { it in 0L..99_999_999L } }
    val max = maxOrder.toIntOrNull()
    val stock = capacity.toIntOrNull()
    val dateFmt = remember { DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss") }
    fun validStamp(value: String): Boolean = runCatching {
        LocalDateTime.parse(value, dateFmt); true
    }.getOrDefault(false)
    val validRange = !byDates || (validStamp(starts) && validStamp(ends) &&
        runCatching { LocalDateTime.parse(starts,dateFmt).isBefore(LocalDateTime.parse(ends,dateFmt)) }.getOrDefault(false))
    val validSession = date.isBlank() || runCatching { LocalDate.parse(date); true }.getOrDefault(false)
    val reservedMin = (existing?.sold ?: 0) + (existing?.reserved ?: 0)
    val valid = name.isNotBlank() && cents != null &&
        (!byQuantity || (stock != null && stock > 0 && stock >= reservedMin)) &&
        (!byZone || sector.isNotBlank()) && validRange && validSession &&
        max != null && max in 1..20

    Surface(Modifier.fillMaxWidth(), border = BorderStroke(1.dp,MaterialTheme.colorScheme.outline),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(if (existing == null) "Nova modalitat d'entrada" else "Editar ${existing.name}", fontWeight = FontWeight.Bold)
            Text("Activa tants criteris com necessites. Cada modalitat definix una combinació amb el seu preu.",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(name, { name=it }, label={ Text("Nom: General, Pista anticipada, VIP…") }, modifier=Modifier.fillMaxWidth())
            OutlinedTextField(price, { price=it }, label={ Text("Preu d'esta modalitat (€)") }, modifier=Modifier.fillMaxWidth(),singleLine=true)
            Text("Aplicar preu segons:",fontWeight=FontWeight.SemiBold)
            Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(byDates, { byDates=it }); Text("Dates")
                Checkbox(byQuantity, { byQuantity=it }); Text("Quantitat")
            }
            Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(byZone, { byZone=it }); Text("Zones")
            }
            if (byDates) {
                Text("Període durant el qual s'oferix este preu", fontWeight=FontWeight.SemiBold)
                OutlinedTextField(starts,{starts=it.take(19)},label={Text("Des de (AAAA-MM-DD HH:MM:SS)")},modifier=Modifier.fillMaxWidth())
                OutlinedTextField(ends,{ends=it.take(19)},label={Text("Fins a (AAAA-MM-DD HH:MM:SS)")},modifier=Modifier.fillMaxWidth())
                if (!validRange) Text("Indica dos dates i hores vàlides, amb fi posterior a l'inici.",color=MaterialTheme.colorScheme.error,fontSize=12.sp)
            }
            if (byQuantity) {
                Text("Contingent d'esta modalitat",fontWeight=FontWeight.SemiBold)
                OutlinedTextField(capacity,{capacity=it.filter(Char::isDigit).take(9)},
                    label={Text("Nombre màxim d'entrades a este preu")},modifier=Modifier.fillMaxWidth(),singleLine=true)
                Text("Quan s'esgota el contingent, este preu deixa d'estar disponible. No és un mínim d'entrades per comanda.",fontSize=12.sp)
                if (stock != null && stock < reservedMin) Text("Ja hi ha $reservedMin entrades venudes o reservades; no pots abaixar el contingent per davall.",color=MaterialTheme.colorScheme.error,fontSize=12.sp)
            }
            if (byZone) {
                Text("Zona de l'esdeveniment",fontWeight=FontWeight.SemiBold)
                OutlinedTextField(sector,{sector=it.take(160)},label={Text("Pista, graderia, VIP…")},modifier=Modifier.fillMaxWidth())
                Text("El contingent s'aplica a esta modalitat, no automàticament a totes les modalitats de la mateixa zona.",fontSize=12.sp)
            }
            OutlinedTextField(maxOrder,{maxOrder=it.filter(Char::isDigit).take(2)},
                label={Text("Màxim per compra (1–20)")},modifier=Modifier.fillMaxWidth(),singleLine=true)
            OutlinedTextField(date,{date=it.take(10)},label={Text("Data de l'actuació o sessió (AAAA-MM-DD; opcional)")},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(time,{time=it.take(8)},label={Text("Hora de la sessió (HH:MM; opcional)")},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(offer,{offer=it.take(190)},label={Text("Etiqueta comercial (opcional)")},modifier=Modifier.fillMaxWidth())
            Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(active,{active=it}); Text("Modalitat activa")
            }
            Button(enabled=valid && !saving,
                onClick={
                    val obj=JSONObject().put("id",existing?.id ?: 0).put("name",name.trim())
                        .put("price_cents",cents).put("capacity",if(byQuantity) stock else JSONObject.NULL)
                        .put("max_per_order",max).put("session_date",if(date.isBlank()) JSONObject.NULL else date)
                        .put("session_time",if(time.isBlank()) JSONObject.NULL else time)
                        .put("sector",if(byZone) sector.trim() else "")
                        .put("offer",offer.trim()).put("sale_starts_at",if(byDates) starts else "")
                        .put("sale_ends_at",if(byDates) ends else "").put("active",active)
                    onSave(obj)
                }, modifier=Modifier.fillMaxWidth()) { Text(if (saving) "Guardant…" else "Guardar modalitat") }
            TextButton(onClick=onCancel,modifier=Modifier.fillMaxWidth()) { Text("Cancel·lar") }
        }
    }
}
