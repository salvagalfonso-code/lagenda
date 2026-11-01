package com.example.agendavalenciana.ui

import com.example.agendavalenciana.model.AgendaDateFormat

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Checkbox
import androidx.compose.ui.text.style.TextAlign

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.agendavalenciana.model.Concert
import com.example.agendavalenciana.model.TicketOption
import com.example.agendavalenciana.state.AgendaState
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/**
 * Editor professional per festivals.
 * Tipus, preus i aforament global => API tickets/configure.php existent.
 * Extres, aforaments diaris i regles encadenades => ESBORRANY LOCAL, no publicació.
 * El checkout no els suporta encara: prohibida la seua activació implícita.
 */
private data class FestivalTypeDraft(
    val key: String = UUID.randomUUID().toString(),
    val id: Long = 0,
    val name: String = "",
    val day: String = "",
    val hour: String = "",
    val sector: String = "",
    val price: String = "0",
    val capacity: String = "",
    val maxOrder: String = "6",
    val starts: String = "",
    val ends: String = "",
    val active: Boolean = false,
    val activation: String = "manual",
    val afterType: String = "",
    val sold: Int = 0,
    val reserved: Int = 0
)

private data class FestivalExtraDraft(
    val key: String = UUID.randomUUID().toString(),
    val name: String = "",
    val variants: String = "",
    val price: String = "0",
    val stock: String = "",
    val maxUnits: String = "1",
    val eligibleType: String = "Totes",
    val active: Boolean = true
)

private fun TicketOption.asFestivalDraft(rule: JSONObject? = null): FestivalTypeDraft = FestivalTypeDraft(
    id = id, name = name, day = sessionDate, hour = sessionTime, sector = sector,
    price = price.toString(), capacity = capacity?.toString().orEmpty(),
    maxOrder = maxPerOrder.toString(), starts = saleStartsAt, ends = saleEndsAt,
    active = active, sold = sold, reserved = reserved,
    activation = rule?.optString("activation", "manual") ?: "manual",
    afterType = rule?.optString("after_type", "") ?: ""
)

private fun FestivalTypeDraft.identity(): String = "${name.trim().lowercase()}|${day.trim()}|${sector.trim().lowercase()}"
private fun euroCents(raw: String): Long? = runCatching {
    val value = BigDecimal(raw.trim().replace(',', '.')).setScale(2, RoundingMode.UNNECESSARY)
    if (value.signum() < 0) null else value.movePointRight(2).longValueExact()
}.getOrNull()
private fun validDate(raw: String): Boolean = raw.isBlank() || AgendaDateFormat.toIso(raw) != null
private fun validDateTime(raw: String): Boolean = raw.isBlank() || runCatching {
    LocalDateTime.parse(AgendaDateFormat.inputDateTimeIso(raw).trim().replace(' ', 'T'))
}.isSuccess

private fun readDraft(ctx: Context, userId: Int, eventId: Int): JSONObject {
    val raw = ctx.getSharedPreferences("festival_ticket_editor_drafts_v1", Context.MODE_PRIVATE)
        .getString("${userId}_${eventId}", "{}") ?: "{}"
    return runCatching { JSONObject(raw) }.getOrDefault(JSONObject())
}

private fun writeDraft(ctx: Context, userId: Int, eventId: Int, data: JSONObject): Boolean =
    ctx.getSharedPreferences("festival_ticket_editor_drafts_v1", Context.MODE_PRIVATE)
        .edit().putString("${userId}_${eventId}", data.toString()).commit()

private fun JSONObject.extraDraft(): FestivalExtraDraft = FestivalExtraDraft(
    key = optString("key").ifBlank { UUID.randomUUID().toString() },
    name = optString("name"), variants = optString("variants"),
    price = optString("price", "0"), stock = optString("stock"),
    maxUnits = optString("max_units", "1"), eligibleType = optString("eligible_type", "Totes"),
    active = optBoolean("active", true)
)

private fun FestivalExtraDraft.json(): JSONObject = JSONObject()
    .put("key", key).put("name", name.trim()).put("variants", variants.trim())
    .put("price", price).put("price_cents", euroCents(price)).put("stock", stock).put("max_units", maxUnits)
    .put("eligible_type", eligibleType).put("active", active)

private fun FestivalTypeDraft.serverOption(): TicketOption = TicketOption(
    id = id, name = name.trim(), sessionDate = day.trim(), sessionTime = hour.trim(),
    sector = sector.trim(), price = price.toIntOrNull() ?: 0,
    capacity = capacity.toIntOrNull(), maxPerOrder = maxOrder.toIntOrNull() ?: 6,
    saleStartsAt = starts.trim(), saleEndsAt = ends.trim(),
    // Una regla dependent d'un altre lot MAI ha de deixar l'entrada activa sense servidor.
    active = active && activation != "esgotament", sold = sold, reserved = reserved
)

private fun validateFestivalDraft(
    types: List<FestivalTypeDraft>, extras: List<FestivalExtraDraft>, dayCaps: Map<String, String>, overall: String
): String? {
    if (overall.isNotBlank() && (overall.toIntOrNull() ?: -1) <= 0) return "L'aforament general ha de ser positiu."
    if (types.isEmpty()) return "Crea almenys una modalitat d'entrada."
    if (types.any { it.name.isBlank() }) return "Totes les modalitats han de tindre nom."
    if (types.map { it.identity() }.distinct().size != types.size) return "Hi ha modalitats repetides amb el mateix nom, dia i sector."
    if (types.any { !validDate(it.day) || !validDateTime(it.starts) || !validDateTime(it.ends) }) return "Revisa les dates: DD/MM/YYYY i DD/MM/YYYY HH:MM."
    if (types.any { it.price.toIntOrNull() == null || it.price.toInt() < 0 || (it.maxOrder.toIntOrNull() ?: 0) !in 1..6 }) return "Preu incorrecte o màxim per compra fora d'1 a 6."
    if (types.any { it.capacity.isNotBlank() && (it.capacity.toIntOrNull() ?: -1) < it.sold + it.reserved }) return "L'aforament d'una modalitat no pot ser inferior a les entrades venudes i reservades."
    if (types.any { it.starts.isNotBlank() && it.ends.isNotBlank() && it.starts >= it.ends }) return "L'inici de venda ha de ser anterior al final."
    if (types.any { it.activation == "data" && it.starts.isBlank() }) return "Una activació per data necessita data i hora d'inici de venda."
    if (types.any { it.activation == "esgotament" && (it.afterType.isBlank() || it.afterType == it.identity() || types.none { t -> t.identity() == it.afterType }) }) return "Indica un lot d'origen vàlid per a cada activació per esgotament."
    val lookup = types.associateBy { it.identity() }
    types.forEach { target ->
        val visited = mutableSetOf<String>()
        var current = target
        while (current.activation == "esgotament") {
            if (!visited.add(current.identity())) return "Hi ha un bucle entre les regles d'activació."
            current = lookup[current.afterType] ?: break
        }
    }
    if (dayCaps.any { (day, cap) -> !validDate(day) || (cap.isNotBlank() && (cap.toIntOrNull() ?: 0) < 1) }) return "Revisa els aforaments per jornada."
    if (extras.any { it.name.isBlank() || euroCents(it.price) == null || (it.maxUnits.toIntOrNull() ?: 0) !in 1..99 || (it.stock.isNotBlank() && (it.stock.toIntOrNull() ?: -1) < 0) }) return "Revisa el nom, preu, estoc i límits dels productes."
    if (extras.any { it.eligibleType != "Totes" && types.none { t -> t.identity() == it.eligibleType } }) return "Un extra apunta a una modalitat d'entrada que ja no existeix."
    return null
}


private fun FestivalTypeDraft.toDraftJson(): JSONObject = JSONObject()
    .put("id", id).put("name", name).put("day", day).put("hour", hour)
    .put("sector", sector).put("price", price).put("capacity", capacity)
    .put("max_order", maxOrder).put("starts", starts).put("ends", ends)
    .put("active", active).put("activation", activation).put("after_type", afterType)
    .put("sold", sold).put("reserved", reserved)

private fun JSONObject.toFestivalType(): FestivalTypeDraft = FestivalTypeDraft(
    id = optLong("id"), name = optString("name"), day = optString("day"),
    hour = optString("hour"), sector = optString("sector"),
    price = optString("price", "0"), capacity = optString("capacity"),
    maxOrder = optString("max_order", "6"), starts = optString("starts"),
    ends = optString("ends"), active = optBoolean("active"),
    activation = optString("activation", "manual"), afterType = optString("after_type"),
    sold = optInt("sold"), reserved = optInt("reserved")
)

private val festivalWizardNames = listOf("Accés i venda", "Aforaments", "Modalitats", "Activacions", "Extres", "Revisió")
private val festivalWizardHelp = listOf(
    listOf(
        TicketHelp("Comença pel festival", "Estàs configurant les entrades d'este festival. Indica si la venda serà interna amb QR o des d'una taquilla externa."),
        TicketHelp("Com avança l'assistent", "Prem Següent quan hages acabat. Pots tornar arrere sense perdre les dades: es guarden com a esborrany local.")
    ),
    listOf(
        TicketHelp("Aforament general", "Indica el màxim de persones que poden entrar al recinte. No confongues este límit amb el nombre d'entrades d'un lot."),
        TicketHelp("Aforament per jornada", "Si hi ha diversos dies, cada dia pot tindre un límit propi. El servidor encara no aplica estos límits compartits: quedaran en esborrany.")
    ),
    listOf(
        TicketHelp("Cada modalitat és una entrada", "Crea abonaments, entrades per dia, VIP o lots. Posa nom, dia, preu, unitats i màxim per compra."),
        TicketHelp("Entrades per dia", "Deixa el dia buit per a un abonament de festival. Escriu DD/MM/YYYY quan la modalitat siga per a una jornada concreta.")
    ),
    listOf(
        TicketHelp("Quan comença la venda?", "Pots activar un lot manualment o programar dates d'inici i de final. Les hores són les del festival."),
        TicketHelp("Activació per esgotament", "Pots preparar que un lot comence quan un altre s'esgote. Fins que el servidor ho gestione, esta regla no es publica.")
    ),
    listOf(
        TicketHelp("Afegix productes opcionals", "Samarretes, gots, acampada i altres extres poden tindre preu, estoc, variants i entrades compatibles."),
        TicketHelp("Estoc i cobrament", "Els extres només són un esborrany: encara falta reservar l'estoc i cobrar-los junt amb l'entrada al servidor.")
    ),
    listOf(
        TicketHelp("Revisa abans de publicar", "Comprova entrades, preus, disponibilitat, aforaments i extres. La venda no es modifica fins que confirmes l'últim botó."),
        TicketHelp("Publicació segura", "Les modalitats compatibles poden publicar-se amb confirmació. Si hi ha extres o regles encara no suportades, el sistema impedix publicar eixa configuració incompleta.")
    )
)

@Composable
fun FestivalTicketManagementPanel(state: AgendaState, concert: Concert, onBack: () -> Unit) {
    val context = LocalContext.current
    val ownerId = state.currentAccount?.serverUserId ?: state.currentAccount?.id ?: 0
    val initial = remember(concert.id, ownerId) { readDraft(context, ownerId, concert.id) }
    val hasLocalTypes = initial.optJSONArray("types") != null
    var types by remember(concert.id, ownerId) {
        mutableStateOf(if (hasLocalTypes) buildList {
            val arr = initial.getJSONArray("types")
            for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { add(it.toFestivalType()) }
        } else state.ticketOptionsFor(concert).map { option ->
            option.asFestivalDraft(initial.optJSONObject("rules")?.optJSONObject(
                "${option.name.trim().lowercase()}|${option.sessionDate}|${option.sector.trim().lowercase()}"))
        })
    }
    var extras by remember(concert.id, ownerId) { mutableStateOf(buildList {
        val arr = initial.optJSONArray("extras") ?: JSONArray()
        for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { add(it.extraDraft()) }
    }) }
    var dayCaps by remember(concert.id, ownerId) { mutableStateOf(buildMap<String, String> {
        val obj = initial.optJSONObject("day_caps") ?: JSONObject()
        obj.keys().forEach { day -> put(day, obj.optString(day)) }
    }) }
    var overall by remember(concert.id, ownerId) { mutableStateOf(initial.optString("overall", state.ticketCapacityFor(concert)?.toString().orEmpty())) }
    var enabled by remember(concert.id, ownerId) { mutableStateOf(initial.optBoolean("enabled", concert.ticketAvailable)) }
    var url by remember(concert.id, ownerId) { mutableStateOf(initial.optString("url", concert.ticketUrl)) }
    var saving by remember(concert.id) { mutableStateOf(false) }
    var message by remember(concert.id) { mutableStateOf("") }
    var dayInput by remember(concert.id) { mutableStateOf("") }
    var stage by remember(concert.id) { mutableStateOf(0) }
    var guideEnabled by remember { mutableStateOf(true) }
    var showTip by remember { mutableStateOf(true) }
    var tipIndex by remember { mutableStateOf(0) }
    var sectionRect by remember { mutableStateOf<Rect?>(null) }
    var navigationRect by remember { mutableStateOf<Rect?>(null) }
    var confirmPublish by remember { mutableStateOf(false) }
    var published by remember { mutableStateOf(false) }
    var dirty by remember { mutableStateOf(false) }

    LaunchedEffect(concert.id) { state.refreshTicketOptions(concert.id) }
    val freshOptions = state.ticketOptionsFor(concert)
    LaunchedEffect(freshOptions) {
        if (!dirty && !hasLocalTypes && freshOptions.isNotEmpty()) {
            types = freshOptions.map { option ->
                option.asFestivalDraft(initial.optJSONObject("rules")?.optJSONObject(
                    "${option.name.trim().lowercase()}|${option.sessionDate}|${option.sector.trim().lowercase()}"))
            }
        }
    }
    LaunchedEffect(stage) {
        tipIndex = 0
        showTip = guideEnabled && stage in 0..5
        sectionRect = null
        navigationRect = null
    }
    fun editType(key: String, update: (FestivalTypeDraft) -> FestivalTypeDraft) {
        types = types.map { if (it.key == key) update(it) else it }
        dirty = true
    }
    fun editExtra(key: String, update: (FestivalExtraDraft) -> FestivalExtraDraft) {
        extras = extras.map { if (it.key == key) update(it) else it }
        dirty = true
    }
    fun saveLocal(): Boolean {
        val caps = JSONObject()
        dayCaps.forEach { (day, cap) -> if (cap.isNotBlank()) caps.put(day, cap) }
        val rules = JSONObject()
        types.forEach { rules.put(it.identity(), JSONObject().put("activation", it.activation).put("after_type", it.afterType)) }
        val arr = JSONArray()
        types.forEach { arr.put(it.toDraftJson()) }
        val products = JSONArray()
        extras.forEach { products.put(it.json()) }
        return writeDraft(context, ownerId, concert.id, JSONObject()
            .put("schema_version", 2).put("types", arr).put("rules", rules)
            .put("day_caps", caps).put("extras", products)
            .put("overall", overall).put("url", url).put("enabled", enabled))
    }
    val days = (concert.scheduleDates + types.map { it.day }.filter(String::isNotBlank) + dayCaps.keys)
        .filter(String::isNotBlank).distinct().sorted()
    val serverUnsupported = unsupportedTicketFeatures(
        hasExtras = extras.isNotEmpty(),
        hasDayCaps = dayCaps.any { it.value.isNotBlank() },
        hasSelloutRule = types.any { it.activation == "esgotament" }
    )
    val canPublish = serverUnsupported.isEmpty()
    val issue = validateFestivalDraft(types, extras, dayCaps, overall)

    if (confirmPublish) AlertDialog(
        onDismissRequest = { confirmPublish = false },
        title = { Text("Confirmar la publicació") },
        text = { Text("Vols ${if (enabled) "activar" else "guardar sense activar"} la venda d'entrades de ${concert.title}? Esta acció actualitza el servidor i és visible als compradors si la venda està activa.") },
        dismissButton = { TextButton(onClick = { confirmPublish = false }) { Text("Tornar a revisar") } },
        confirmButton = { Button(onClick = {
            confirmPublish = false
            if (!saveLocal()) {
                message = "No s'ha pogut guardar l'esborrany. No s'ha publicat res."
                return@Button
            }
            saving = true
            state.updateTicketSales(
                concertId = concert.id, ticketUrl = url.trim(),
                ticketPrice = types.minOf { it.price.toInt() }, ticketAvailable = enabled,
                capacity = overall.toIntOrNull(),
                ticketOptions = types.map { it.serverOption() }, configureAdvanced = true
            ) { ok, msg ->
                saving = false
                message = msg
                if (ok) {
                    // En reobrir, recuperem els IDs definitius del servidor i no
                    // tornem a crear una modalitat amb id=0 en publicar de nou.
                    val saved = readDraft(context, ownerId, concert.id)
                    saved.remove("types")
                    writeDraft(context, ownerId, concert.id, saved)
                    dirty = false
                    published = true
                    stage = 6
                }
            }
        }) { Text("Confirmar") } }
    )

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = {
                if (!saveLocal()) message = "No s'ha pogut conservar l'esborrany."
                onBack()
            }) { Text("← Tornar als esdeveniments") }
            Text(concert.title, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            if (published) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(if (enabled) "Venda configurada al servidor" else "Configuració guardada al servidor", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Les modalitats compatibles s'han guardat. Les entrades es mostraran segons les dates i l'estat de venda configurats.")
                        Text(message)
                        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Finalitzar") }
                    }
                }
            } else {
                Text("PAS ${stage + 1} DE 6 · ${festivalWizardNames[stage]}", fontWeight = FontWeight.Bold, fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(6) { n ->
                        Box(Modifier.weight(1f).height(5.dp).background(
                            if (n <= stage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            androidx.compose.foundation.shape.RoundedCornerShape(3.dp)))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { guideEnabled = true; tipIndex = 0; showTip = true }) { Text("? Repetir explicació") }
                }
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (stage) {
                        0 -> {
                            Text("Com vols gestionar la venda?", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            Text("Les entrades es poden vendre amb el sistema intern o dirigir a una taquilla externa.")
                            OutlinedTextField(value = url, onValueChange = { url = it; dirty = true },
                                label = { Text("URL de taquilla externa (opcional)") },
                                supportingText = { Text("Deixa-la buida per a venda interna i QR") },
                                modifier = Modifier.fillMaxWidth().onGloballyPositioned { sectionRect = it.boundsInRoot() })
                            Card { Text("La publicació es farà només al final, després de revisar i confirmar. Pots eixir i recuperar l'esborrany.", Modifier.padding(14.dp)) }
                        }
                        1 -> {
                            Text("Aforament del festival", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(overall, { overall = it.filter(Char::isDigit); dirty = true },
                                label = { Text("Aforament general") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                supportingText = { Text("Buit = sense límit general configurat") },
                                modifier = Modifier.fillMaxWidth().onGloballyPositioned { sectionRect = it.boundsInRoot() })
                            Text("Aforaments per jornada", fontWeight = FontWeight.Bold)
                            Text("Estos límits compartits es guarden en esborrany fins que el servidor els puga aplicar.", fontSize = 12.sp)
                            days.forEach { day ->
                                OutlinedTextField(dayCaps[day].orEmpty(), { value ->
                                    dayCaps = dayCaps + (day to value.filter(Char::isDigit)); dirty = true
                                }, label = { Text("Jornada ${AgendaDateFormat.date(day)}") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth())
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(AgendaDateFormat.date(dayInput), { dayInput = AgendaDateFormat.inputDateIso(it) }, label = { Text("Nou dia (DD/MM/YYYY)") }, modifier = Modifier.weight(1f))
                                OutlinedButton(onClick = { if (dayInput.isNotBlank() && validDate(dayInput)) {
                                    dayCaps = dayCaps + (AgendaDateFormat.inputDateIso(dayInput) to ""); dayInput = ""; dirty = true
                                } }) { Text("Afegir") }
                            }
                        }
                        2 -> {
                            Text("Tipus d'entrades", fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.onGloballyPositioned { sectionRect = it.boundsInRoot() })
                            Text("Pots crear tantes modalitats com necessites: abonament, dia concret, VIP o lot promocional.", fontSize = 12.sp)
                            types.forEachIndexed { index, t ->
                                Card(Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                        Text("Modalitat ${index + 1}", fontWeight = FontWeight.Bold)
                                        OutlinedTextField(t.name, { editType(t.key) { d -> d.copy(name = it) } },
                                            label = { Text("Nom") }, modifier = Modifier.fillMaxWidth())
                                        OutlinedTextField(AgendaDateFormat.date(t.day), { text -> editType(t.key) { d -> d.copy(day = AgendaDateFormat.inputDateIso(text)) } },
                                            label = { Text("Dia (buit si és abonament)") }, modifier = Modifier.fillMaxWidth())
                                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                            OutlinedTextField(t.price, { v -> editType(t.key) { it.copy(price = v.filter(Char::isDigit)) } },
                                                label = { Text("Preu €") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                                            OutlinedTextField(t.capacity, { v -> editType(t.key) { it.copy(capacity = v.filter(Char::isDigit)) } },
                                                label = { Text("Unitats") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                            OutlinedTextField(t.hour, { v -> editType(t.key) { it.copy(hour = v) } },
                                                label = { Text("Hora d'accés") }, modifier = Modifier.weight(1f))
                                            OutlinedTextField(t.maxOrder, { v -> editType(t.key) { it.copy(maxOrder = v.filter(Char::isDigit)) } },
                                                label = { Text("Màx./compra") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                                        }
                                        OutlinedTextField(t.sector, { v -> editType(t.key) { it.copy(sector = v) } }, label = { Text("Sector (opcional)") }, modifier = Modifier.fillMaxWidth())
                                        Text("Venudes ${t.sold} · Reservades ${t.reserved}", fontSize = 12.sp)
                                        if (t.id == 0L) TextButton(onClick = { types = types.filterNot { it.key == t.key }; dirty = true }) { Text("Eliminar modalitat") }
                                    }
                                }
                            }
                            Button(onClick = { types = types + FestivalTypeDraft(); dirty = true }, modifier = Modifier.fillMaxWidth()) { Text("+ Crear tipus d'entrada") }
                        }
                        3 -> {
                            Text("Activacions i períodes de venda", fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.onGloballyPositioned { sectionRect = it.boundsInRoot() })
                            types.forEach { t ->
                                Card(Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(t.name.ifBlank { "Modalitat sense nom" }, fontWeight = FontWeight.Bold)
                                        OutlinedTextField(AgendaDateFormat.editableDateTime(t.starts), { text -> editType(t.key) { d -> d.copy(starts = AgendaDateFormat.inputDateTimeIso(text)) } },
                                            label = { Text("Inici DD/MM/YYYY HH:MM") }, modifier = Modifier.fillMaxWidth())
                                        OutlinedTextField(AgendaDateFormat.editableDateTime(t.ends), { text -> editType(t.key) { d -> d.copy(ends = AgendaDateFormat.inputDateTimeIso(text)) } },
                                            label = { Text("Final DD/MM/YYYY HH:MM") }, modifier = Modifier.fillMaxWidth())
                                        Text("Com s'activa?", fontWeight = FontWeight.SemiBold)
                                        listOf("manual" to "Manual", "data" to "En una data", "esgotament" to "Quan s'esgote un altre lot").forEach { (mode, label) ->
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                androidx.compose.material3.RadioButton(selected = t.activation == mode,
                                                    onClick = { editType(t.key) { it.copy(activation = mode, active = if (mode == "data") true else it.active) } })
                                                Text(label)
                                            }
                                        }
                                        if (t.activation == "esgotament") {
                                            Text("Regla només en esborrany; el servidor no l'activarà encara.", fontSize = 12.sp)
                                            types.filter { it.key != t.key }.forEach { source ->
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    androidx.compose.material3.RadioButton(selected = t.afterType == source.identity(),
                                                        onClick = { editType(t.key) { it.copy(afterType = source.identity()) } })
                                                    Text(source.name.ifBlank { "(sense nom)" })
                                                }
                                            }
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Lot habilitat", Modifier.weight(1f))
                                            Switch(checked = t.active && t.activation != "esgotament",
                                                onCheckedChange = { value -> editType(t.key) { it.copy(active = value) } },
                                                enabled = t.activation != "esgotament")
                                        }
                                    }
                                }
                            }
                        }
                        4 -> {
                            Text("Productes extres", fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.onGloballyPositioned { sectionRect = it.boundsInRoot() })
                            Text("Estos productes no es publicaran ni es cobraran fins que el servidor gestione estocs i pagament conjunt.", fontSize = 12.sp)
                            extras.forEachIndexed { idx, extra ->
                                Card(Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                        Text("Producte ${idx + 1}", fontWeight = FontWeight.Bold)
                                        OutlinedTextField(extra.name, { editExtra(extra.key) { e -> e.copy(name = it) } },
                                            label = { Text("Nom del producte") }, modifier = Modifier.fillMaxWidth())
                                        OutlinedTextField(extra.variants, { editExtra(extra.key) { e -> e.copy(variants = it) } },
                                            label = { Text("Talles o variants (separades per comes)") }, modifier = Modifier.fillMaxWidth())
                                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                            OutlinedTextField(extra.price, { v -> editExtra(extra.key) { it.copy(price = v.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }) } },
                                                label = { Text("Preu €") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                                            OutlinedTextField(extra.stock, { v -> editExtra(extra.key) { it.copy(stock = v.filter(Char::isDigit)) } },
                                                label = { Text("Estoc") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                        }
                                        OutlinedTextField(extra.maxUnits, { v -> editExtra(extra.key) { it.copy(maxUnits = v.filter(Char::isDigit)) } },
                                            label = { Text("Màxim per compra") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                                        Text("Entrades compatibles", fontWeight = FontWeight.SemiBold)
                                        (listOf("Totes" to "Totes") + types.map { it.identity() to it.name.ifBlank { "(sense nom)" } }).forEach { (key, label) ->
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                androidx.compose.material3.RadioButton(selected = extra.eligibleType == key,
                                                    onClick = { editExtra(extra.key) { it.copy(eligibleType = key) } })
                                                Text(label)
                                            }
                                        }
                                        TextButton(onClick = { extras = extras.filterNot { it.key == extra.key }; dirty = true }) { Text("Llevar producte") }
                                    }
                                }
                            }
                            Button(onClick = { extras = extras + FestivalExtraDraft(); dirty = true }, modifier = Modifier.fillMaxWidth()) { Text("+ Afegir producte") }
                            OutlinedButton(onClick = { stage = 5 }, modifier = Modifier.fillMaxWidth()) { Text("No tinc extres · Continuar") }
                        }
                        5 -> {
                            Text("Revisió final", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.onGloballyPositioned { sectionRect = it.boundsInRoot() })
                            Text("Festival: ${concert.title}")
                            Text("Tipus d'entrades: ${types.size}")
                            Text("Aforament general: ${overall.ifBlank { "no configurat" }}")
                            Text("Extres: ${extras.size} · Jornades amb límit: ${dayCaps.count { it.value.isNotBlank() }}")
                            types.forEach { Text("• ${it.name} · ${it.day.takeIf(String::isNotBlank)?.let(AgendaDateFormat::date) ?: "abonament"} · ${it.price} € · ${it.activation}") }
                            Divider()
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("Venda pública activada", Modifier.weight(1f))
                                Switch(checked = enabled, onCheckedChange = { enabled = it; dirty = true })
                            }
                            Text("Si ho desactives, les modalitats es guarden al servidor però la venda no s'obri.", fontSize = 12.sp)
                            if (serverUnsupported.isNotEmpty()) {
                                Card { Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text("Publicació bloquejada per seguretat", fontWeight = FontWeight.Bold)
                                    Text("Falta integrar al servidor: ${serverUnsupported.joinToString(", ")}. Pots guardar l'esborrany i continuar editant-lo, però no es publicarà una venda parcial.", fontSize = 12.sp)
                                } }
                            }
                            issue?.let { Text("Revisa: $it", color = MaterialTheme.colorScheme.error) }
                            Button(onClick = {
                                message = if (saveLocal()) "Esborrany guardat en este dispositiu; no s'ha publicat." else "No s'ha pogut guardar l'esborrany."
                            }, modifier = Modifier.fillMaxWidth()) { Text("Guardar esborrany") }
                            Button(onClick = { if (saveLocal()) { message = ""; confirmPublish = true } else message = "Error guardant l'esborrany." },
                                enabled = !saving && canPublish && issue == null,
                                modifier = Modifier.fillMaxWidth()) {
                                Text(if (saving) "Publicant…" else if (enabled) "Revisat · Publicar venda" else "Guardar al servidor sense activar")
                            }
                            Text("Només el servidor confirma la venda i controla les entrades venudes i els QR.", fontSize = 11.sp)
                        }
                    }
                    if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
                Row(Modifier.fillMaxWidth().onGloballyPositioned { navigationRect = it.boundsInRoot() },
                    horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    OutlinedButton(onClick = { if (saveLocal() && stage > 0) stage -= 1 }, enabled = stage > 0,
                        modifier = Modifier.weight(1f)) { Text("← Anterior") }
                    Button(onClick = {
                        if (!saveLocal()) message = "No s'ha pogut guardar l'esborrany."
                        else if (stage < 5) { message = ""; stage += 1 }
                    }, enabled = stage < 5, modifier = Modifier.weight(1f)) { Text("Següent →") }
                }
            }
        }
        if (showTip && stage in 0..5 && !published) {
            val help = festivalWizardHelp[stage]
            TicketCoachMark(
                targetInRoot = if (tipIndex == 0) sectionRect else navigationRect,
                help = help[tipIndex], index = tipIndex, total = help.size,
                onNext = { if (tipIndex < help.lastIndex) tipIndex++ else showTip = false },
                onSkip = { showTip = false; guideEnabled = false }
            )
        }
    }
}
