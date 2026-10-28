package com.example.agendavalenciana.ui

import com.example.agendavalenciana.model.AgendaDateFormat

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.agendavalenciana.model.Concert
import com.example.agendavalenciana.model.PurchasedTicket
import com.example.agendavalenciana.model.TicketStatus
import com.example.agendavalenciana.data.TicketQrCache
import com.example.agendavalenciana.state.AgendaState

@Composable
fun PurchasedTicketsPanel(
    state: AgendaState,
    focusedConcertId: Int? = null,
    onOpenConcert: (Concert) -> Unit
) {
    val tickets = state.ticketsForCurrentUser()
    if (tickets.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(30.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Encara no tens entrades comprades.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val grouped = tickets.groupBy { it.concertId }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        grouped.entries
            .sortedBy { (concertId, _) -> state.concerts.firstOrNull { it.id == concertId }?.date.orEmpty() }
            .forEach { (concertId, eventTickets) ->
                val concert = state.concerts.firstOrNull { it.id == concertId }
                if (concert != null) {
                    TicketWalletCard(
                        concert = concert,
                        tickets = eventTickets,
                        initiallyExpanded = focusedConcertId == concertId,
                        onOpenConcert = { onOpenConcert(concert) }
                    )
                }
            }
    }
}

@Composable
private fun TicketWalletCard(
    concert: Concert,
    tickets: List<PurchasedTicket>,
    initiallyExpanded: Boolean = false,
    onOpenConcert: () -> Unit
) {
    var expanded by remember(concert.id) { mutableStateOf(initiallyExpanded) }
    LaunchedEffect(initiallyExpanded) {
        if (initiallyExpanded) expanded = true
    }
    val active = tickets.count { it.status == TicketStatus.ACTIVE }
    val used = tickets.count { it.status == TicketStatus.USED }
    val cancelled = tickets.count { it.status == TicketStatus.CANCELLED }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .16f)),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.ConfirmationNumber, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(concert.title, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text(
                        "${AgendaDateFormat.date(concert.date)} · ${concert.time} · ${concert.town}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(if (expanded) "Ocultar QR" else "Veure QR", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(Modifier.height(7.dp))
            Text(
                buildString {
                    append("${tickets.size} entrada")
                    if (tickets.size != 1) append("es")
                    if (active > 0) append(" · $active activa${if (active == 1) "" else "s"}")
                    if (used > 0) append(" · $used utilitzada${if (used == 1) "" else "s"}")
                    if (cancelled > 0) append(" · $cancelled anul·lada${if (cancelled == 1) "" else "s"}")
                },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (expanded) {
                Spacer(Modifier.height(12.dp))
                tickets.forEachIndexed { index, ticket ->
                    TicketQrBlock(concert = concert, ticket = ticket, number = index + 1)
                    if (index != tickets.lastIndex) Spacer(Modifier.height(12.dp))
                }
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = onOpenConcert, modifier = Modifier.align(Alignment.End)) {
                    Text("Veure esdeveniment")
                }
            }
        }
    }
}

@Composable
private fun TicketQrBlock(concert: Concert, ticket: PurchasedTicket, number: Int) {
    val context = LocalContext.current
    val qr = remember(ticket.code) { TicketQrCache.loadOrCreate(context, ticket.code).asImageBitmap() }
    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            TicketPdf.writeToUri(context, uri, concert, ticket)
                .onSuccess { Toast.makeText(context, "PDF guardat.", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(context, it.message ?: "No s'ha pogut guardar el PDF.", Toast.LENGTH_LONG).show() }
        }
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f)
    ) {
        Column(
            modifier = Modifier.padding(13.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Entrada $number", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                AssistChip(onClick = {}, label = { Text(ticket.status.label) })
            }
            Image(
                bitmap = qr,
                contentDescription = "QR entrada ${ticket.code}",
                modifier = Modifier.size(210.dp)
            )
            Text(TicketPdf.displayCode(ticket.code), fontWeight = FontWeight.Black, fontSize = 14.sp, letterSpacing = .7.sp)
            val optionDetails = listOfNotNull(
                ticket.ticketOptionName.takeIf { it.isNotBlank() },
                ticket.sessionDate.takeIf { it.isNotBlank() },
                ticket.sessionTime.takeIf { it.isNotBlank() },
                ticket.sector.takeIf { it.isNotBlank() },
                ticket.offer.takeIf { it.isNotBlank() }
            ).joinToString(" · ")
            if (optionDetails.isNotBlank()) {
                Spacer(Modifier.height(5.dp))
                Text(optionDetails, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            if (ticket.status == TicketStatus.USED && ticket.usedAtMillis != null) {
                Text(
                    "Validada ${formatTicketTimestamp(ticket.usedAtMillis)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(9.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { pdfLauncher.launch(TicketPdf.suggestedFileName(concert, number)) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("PDF")
                }
                OutlinedButton(
                    onClick = {
                        TicketPdf.share(context, concert, ticket)
                            .onFailure { Toast.makeText(context, it.message ?: "No s'ha pogut compartir.", Toast.LENGTH_LONG).show() }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Compartir")
                }
            }
        }
    }
}

private fun formatTicketTimestamp(timestamp: Long): String = AgendaDateFormat.timestamp(timestamp)
