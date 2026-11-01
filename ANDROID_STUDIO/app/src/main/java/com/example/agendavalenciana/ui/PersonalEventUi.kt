package com.example.agendavalenciana.ui

import com.example.agendavalenciana.model.AgendaDateFormat

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agendavalenciana.data.TicketQrCache
import com.example.agendavalenciana.model.Concert
import com.example.agendavalenciana.state.AgendaState

/**
 * Panell especial d'un esdeveniment. Només apareix quan el panell ha estat
 * activat expressament per a eixe esdeveniment. Guardar, marcar Aniré o tindre
 * una entrada no l'activen automàticament.
 */
@Composable
internal fun PersonalEventModePanel(
    state: AgendaState,
    concert: Concert,
    specialConfig: SpecialEventConfig?,
    onShareVisual: () -> Unit,
    onOpenSpecialEvent: (() -> Unit)? = null
) {
    if (specialConfig == null) return

    val context = LocalContext.current
    val tickets = state.ticketsForCurrentUser(concert.id)
    val selectedArtists = state.selectedFestivalArtists(concert.id)
    val isFestival = concert.isFestival

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .58f))
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Panell de l'esdeveniment", fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Text(
                        specialConfig.displayTitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                AssistChip(
                    onClick = {},
                    label = { Text(if (isFestival) "Mode festival" else "Mode concert") }
                )
            }

            Spacer(Modifier.height(9.dp))
            Text(
                if (isFestival) {
                    "Organitza el teu festival: entrada, grups seleccionats i horari personal en un mateix lloc."
                } else {
                    "Accés ràpid a l'entrada, hora, lloc i informació útil del concert."
                },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(10.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(13.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = .78f)
            ) {
                Column(Modifier.padding(11.dp)) {
                    Text(
                        listOf(AgendaDateFormat.date(concert.date), concert.time).filter(String::isNotBlank).joinToString(" · "),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        listOf(concert.venue, concert.town).filter(String::isNotBlank).joinToString(" · "),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (tickets.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                val first = tickets.first()
                val qr = remember(first.code) { TicketQrCache.loadOrCreate(context, first.code).asImageBitmap() }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(qr, "QR de l'entrada", Modifier.size(96.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.ConfirmationNumber, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    if (tickets.size == 1) "Entrada disponible offline" else "${tickets.size} entrades disponibles offline",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                            }
                            Text(
                                "El QR queda guardat al dispositiu i no necessita cobertura per mostrar-se.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (isFestival) {
                Spacer(Modifier.height(15.dp))
                Text("El meu cartell", fontWeight = FontWeight.Black, fontSize = 15.sp)
                Text(
                    "Marca els grups que vols veure. La selecció es guarda en este dispositiu.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(7.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(concert.lineup.distinctBy { it.trim().lowercase() }) { artist ->
                        val selected = festivalArtistIsSelected(state, concert.id, artist)
                        FilterChip(
                            selected = selected,
                            onClick = { toggleFestivalArtistByName(state, concert.id, artist) },
                            label = { Text(artist, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            leadingIcon = if (selected) {
                                { Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text("Horari personal", fontWeight = FontWeight.Black, fontSize = 15.sp)
                val schedule = specialConfig?.schedule.orEmpty()
                val personalSchedule = schedule.filter { item ->
                    val key = item.artistName?.takeIf(String::isNotBlank) ?: item.title
                    selectedArtists.any { selection ->
                        selection == scheduleSelectionKey(item) ||
                            ('|' !in selection && festivalArtistKey(selection) == festivalArtistKey(key))
                    }
                }
                when {
                    selectedArtists.isEmpty() -> Text(
                        "Selecciona almenys un grup per construir el teu horari.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    personalSchedule.isNotEmpty() -> {
                        personalSchedule.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(item.time, modifier = Modifier.width(56.dp), fontWeight = FontWeight.Black, fontSize = 13.sp)
                                Column(Modifier.weight(1f)) {
                                    Text(item.artistName ?: item.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    if (item.location.isNotBlank()) {
                                        Text(item.location, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                    else -> Text(
                        festivalSelectedArtistNames(state, concert.id).joinToString(" · ") + "\nHoraris encara no publicats.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(13.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onShareVisual, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Share, null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Compartir card")
                }
                if (isFestival && specialConfig != null && onOpenSpecialEvent != null) {
                    Button(onClick = onOpenSpecialEvent, modifier = Modifier.weight(1f)) {
                        Text("Obrir mode festival")
                    }
                }
            }
        }
    }
}
