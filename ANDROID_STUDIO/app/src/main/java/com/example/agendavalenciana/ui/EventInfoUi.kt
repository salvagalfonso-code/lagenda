package com.example.agendavalenciana.ui

import com.example.agendavalenciana.model.AgendaDateFormat

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.agendavalenciana.model.Concert
import com.example.agendavalenciana.model.CategoryTaxonomy

@Composable
internal fun UniversalEventInfo(
    concert: Concert,
    festivalStyle: Boolean = false,
    showSchedule: Boolean = true,
    showLocation: Boolean = true,
    showExtra: Boolean = true,
    showMapButton: Boolean = true
) {
    val context = LocalContext.current
    val info = concert.eventInfo
    val starts = if (info.dailyStart.isNotEmpty()) info.dailyStart else listOfNotNull(
        concert.date.takeIf(String::isNotBlank)?.let { com.example.agendavalenciana.model.EventDailyStart(it.take(10), concert.time.take(5), "Inici") }
    )
    val mapsUrl = info.mapsUrl.ifBlank {
        val query = listOf(concert.venue, concert.town).map(String::trim).filter(String::isNotBlank).joinToString(", ")
        if (query.isBlank()) "" else "https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}"
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (concert.subcategoryCodes.isNotEmpty() || concert.isInfantil) {
            InfoCard("Classificació", festivalStyle) {
                val names = concert.subcategoryCodes.mapNotNull {
                    CategoryTaxonomy.findByCode(it)?.label
                }.distinct()
                val tags = buildList {
                    add(concert.category.label)
                    addAll(names)
                    if (CategoryTaxonomy.isInfantil(concert)) add("Infantil")
                }
                Text(tags.joinToString(" · "), fontSize = 13.sp)
            }
        }
        if (showSchedule) {
            InfoCard("Horari", festivalStyle) {
                if (starts.isEmpty()) Text("Horari pendent de confirmar", fontSize = 13.sp)
                else starts.forEach { row ->
                    val date = row.date.takeIf(String::isNotBlank)?.let(::formatEventInfoDate).orEmpty()
                    val time = row.time.takeIf(String::isNotBlank)?.take(5).orEmpty()
                    val label = row.label.ifBlank { "Inici" }
                    Text(listOf(date, if (time.isNotBlank()) "$label · $time" else label).filter(String::isNotBlank).joinToString(" · "), fontSize = 13.sp)
                }
            }
        }
        if (showLocation) InfoCard("Ubicació i accés", festivalStyle) {
            val address = info.address.ifBlank { listOf(concert.venue, concert.town).filter(String::isNotBlank).joinToString(" · ") }
            if (address.isNotBlank()) Text(address, fontSize = 13.sp)
            if (info.access.isNotBlank()) { Spacer(Modifier.height(4.dp)); Text(info.access, fontSize = 13.sp) }
            if (showMapButton && mapsUrl.isNotBlank()) { Spacer(Modifier.height(8.dp)); Button(onClick = { openTicketUrl(context, mapsUrl) }) { Text("Obrir en Google Maps") } }
        }
        if (showExtra) InfoCard("Transport públic", festivalStyle) {
            if (info.transport.isEmpty()) Text("Informació pendent de verificar", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else info.transport.forEach { row ->
                val prefix = when (row.type.trim().lowercase()) {
                    "metro" -> "Metro"
                    "bus" -> "Bus"
                    "train", "tren", "rodalies", "cercanias" -> "Tren"
                    "tram" -> "TRAM"
                    else -> row.type.ifBlank { "Transport" }
                }
                Text("$prefix · ${row.text}", fontSize = 13.sp)
            }
        }
        if (showExtra) InfoCard("Accessibilitat PMR", festivalStyle) {
            val status = info.pmrStatus.uppercase()
            val prefix = when (status) { "YES", "SI", "SÍ" -> "Accessible"; "PARTIAL" -> "Accessibilitat parcial"; "NO" -> "No confirmada com accessible"; else -> "Informació pendent de verificar" }
            Text(if (info.pmrText.isBlank()) prefix else "$prefix · ${info.pmrText}", fontSize = 13.sp)
        }
        if (showExtra) InfoCard("Avisos", festivalStyle) {
            if (info.notices.isEmpty()) Text("Sense avisos publicats", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else info.notices.forEach { Text("• $it", fontSize = 13.sp) }
        }
    }
}

@Composable
private fun InfoCard(title: String, festivalStyle: Boolean, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (festivalStyle) 14.dp else 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (festivalStyle) Color.White else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .48f)
        ),
        border = if (festivalStyle) BorderStroke(1.dp, Color(0xFFE7E7E7)) else null
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, fontWeight = FontWeight.Black, fontSize = 14.sp)
            Spacer(Modifier.height(5.dp))
            content()
        }
    }
}

private fun formatEventInfoDate(raw: String): String = AgendaDateFormat.date(raw)
