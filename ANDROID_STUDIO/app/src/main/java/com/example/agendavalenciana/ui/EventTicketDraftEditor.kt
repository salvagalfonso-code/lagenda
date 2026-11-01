package com.example.agendavalenciana.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** El borrador es publica amb l'activitat, però no activa la venda. */
internal data class EventTicketDraft(
    val name: String, val priceCents: Int, val capacity: Int?, val sector: String,
    val saleStartsAt: String, val saleEndsAt: String, val maxPerOrder: Int = 6
) {
    fun json(): JSONObject = JSONObject().put("name", name).put("price_cents", priceCents)
        .put("capacity", capacity ?: JSONObject.NULL).put("sector", sector)
        .put("sale_starts_at", saleStartsAt).put("sale_ends_at", saleEndsAt)
        .put("max_per_order", maxPerOrder)
}
internal fun List<EventTicketDraft>.ticketDraftJson(): JSONArray = JSONArray().also { array -> forEach { array.put(it.json()) } }
internal fun JSONArray.ticketDraftItems(): List<EventTicketDraft> = buildList {
    for (i in 0 until length()) {
        val obj = optJSONObject(i) ?: continue
        add(EventTicketDraft(obj.optString("name"), obj.optInt("price_cents"),
            if (obj.isNull("capacity")) null else obj.optInt("capacity"),
            obj.optString("sector"), obj.optString("sale_starts_at"),
            obj.optString("sale_ends_at"), obj.optInt("max_per_order",6)))
    }
}

@Composable
internal fun EventTicketDraftEditor(
    drafts: List<EventTicketDraft>,
    onChange: (List<EventTicketDraft>) -> Unit,
    modifier: Modifier = Modifier
) {
    var editIndex by remember { mutableStateOf<Int?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text("Tipus d'entrades i preus", fontWeight = FontWeight.Bold)
        Text("Afig diferents entrades per dates, quantitat i zones, combinant els tres criteris. La venda no s'activarà fins que tingues compte i dades de facturació.", style = MaterialTheme.typography.bodySmall)
        drafts.forEachIndexed { index, d ->
            Surface(border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant),shape=MaterialTheme.shapes.medium) {
                Column(Modifier.fillMaxWidth().padding(12.dp),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                    Text(d.name, fontWeight=FontWeight.SemiBold)
                    Text("${BigDecimal(d.priceCents).movePointLeft(2).toPlainString()} €" +
                        (d.sector.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""))
                    Text("Contingent: ${d.capacity?.toString() ?: "sense límit"} · Màxim per compra: ${d.maxPerOrder}",style=MaterialTheme.typography.bodySmall)
                    if (d.saleStartsAt.isNotBlank()) Text("Venda: ${d.saleStartsAt} → ${d.saleEndsAt}",style=MaterialTheme.typography.bodySmall)
                    Row {
                        TextButton(onClick={editIndex=index;showEditor=true}) { Text("Editar") }
                        TextButton(onClick={onChange(drafts.filterIndexed { i,_ -> i != index })}) { Text("Eliminar") }
                    }
                }
            }
        }
        OutlinedButton(onClick={editIndex=null;showEditor=true},modifier=Modifier.fillMaxWidth()) {
            Text("+ Afegir tipus d'entrada")
        }
    }
    if(showEditor) {
        val initial=editIndex?.let { drafts.getOrNull(it) }
        key(editIndex,showEditor) {
            TicketDraftDialog(initial=initial,onDismiss={showEditor=false},onSave={draft ->
                onChange(if(editIndex == null) drafts + draft else drafts.mapIndexed { i, d -> if(i==editIndex) draft else d })
                showEditor=false
            })
        }
    }
}

@Composable
private fun TicketDraftDialog(initial: EventTicketDraft?,onDismiss:()->Unit,onSave:(EventTicketDraft)->Unit) {
    var name by remember {mutableStateOf(initial?.name.orEmpty())}
    var price by remember {mutableStateOf(initial?.priceCents?.let{BigDecimal(it).movePointLeft(2).toPlainString()}.orEmpty())}
    var useQuantity by remember {mutableStateOf(initial?.capacity != null)}
    var quantity by remember {mutableStateOf(initial?.capacity?.toString().orEmpty())}
    var useZone by remember {mutableStateOf(!initial?.sector.isNullOrBlank())}
    var zone by remember {mutableStateOf(initial?.sector.orEmpty())}
    var useDates by remember {mutableStateOf(!initial?.saleStartsAt.isNullOrBlank())}
    var starts by remember {mutableStateOf(initial?.saleStartsAt.orEmpty())}
    var ends by remember {mutableStateOf(initial?.saleEndsAt.orEmpty())}
    var maxOrder by remember {mutableStateOf((initial?.maxPerOrder?:6).toString())}
    val cents=runCatching { BigDecimal(price.replace(',','.')).multiply(BigDecimal(100))
        .setScale(0,RoundingMode.UNNECESSARY).intValueExact() }.getOrNull()
    val pattern= remember { DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm") }
    val datesValid=!useDates || runCatching {
        val start=LocalDateTime.parse(starts,pattern)
        val end=LocalDateTime.parse(ends,pattern)
        start.isBefore(end)
    }.getOrDefault(false)
    val stock=quantity.toIntOrNull()
    val max=maxOrder.toIntOrNull()
    val valid = name.isNotBlank() && name.length<=190 && cents!=null && cents in 0..99999999 &&
        (!useQuantity || (stock!=null && stock in 1..100000000)) &&
        (!useZone || zone.isNotBlank()) && datesValid && max!=null && max in 1..20
    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text(if(initial==null) "Afegir entrada" else "Editar entrada")},
        text={ Column(Modifier.fillMaxWidth().heightIn(max=510.dp).verticalScroll(rememberScrollState()),
            verticalArrangement=Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(name,{name=it.take(190)},label={Text("Nom de l'entrada")},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(price,{price=it.take(12)},label={Text("Preu (€)")},modifier=Modifier.fillMaxWidth())
            Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(useDates,{useDates=it});Text("Preu per dates")
            }
            if(useDates) {
                OutlinedTextField(starts,{starts=it.take(16)},label={Text("Inici · AAAA-MM-DD HH:MM")},modifier=Modifier.fillMaxWidth())
                OutlinedTextField(ends,{ends=it.take(16)},label={Text("Final · AAAA-MM-DD HH:MM")},modifier=Modifier.fillMaxWidth())
                if(!datesValid) Text("Indica un període amb inici anterior al final.",color=MaterialTheme.colorScheme.error)
            }
            Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(useQuantity,{useQuantity=it});Text("Limitar per quantitat")
            }
            if(useQuantity) OutlinedTextField(quantity,{quantity=it.filter(Char::isDigit).take(9)},
                label={Text("Entrades disponibles en este tram")},modifier=Modifier.fillMaxWidth())
            Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(useZone,{useZone=it});Text("Preu per zona")
            }
            if(useZone) OutlinedTextField(zone,{zone=it.take(160)},label={Text("Zona · Pista, grada, VIP…")},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(maxOrder,{maxOrder=it.filter(Char::isDigit).take(2)},label={Text("Màxim per compra")},modifier=Modifier.fillMaxWidth())
            Text("Cada modalitat té el seu preu i contingent independent.",style=MaterialTheme.typography.bodySmall)
        } },
        confirmButton={TextButton(enabled=valid,onClick={
            onSave(EventTicketDraft(name.trim(),cents!!,if(useQuantity)stock else null,
                if(useZone)zone.trim() else "",
                if(useDates)starts+":00" else "",if(useDates)ends+":00" else "",max!!))
        }) {Text("Guardar entrada")}},
        dismissButton={TextButton(onClick=onDismiss){Text("Cancel·lar")}}
    )
}
