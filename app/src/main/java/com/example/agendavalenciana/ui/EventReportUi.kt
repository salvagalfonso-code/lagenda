package com.example.agendavalenciana.ui

import com.example.agendavalenciana.model.AgendaDateFormat
import com.example.agendavalenciana.model.CategoryTaxonomy

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agendavalenciana.model.Concert
import com.example.agendavalenciana.model.CulturalCategory
import com.example.agendavalenciana.state.AgendaState
import android.widget.Toast

enum class EventReportReason(val code: String, val label: String) {
    ANIMAL_ABUSE("ANIMAL_ABUSE", "Maltractament animal"),
    HUMAN_RIGHTS("HUMAN_RIGHTS", "Ataca els drets humans"),
    HATE_DISCRIMINATION("HATE_DISCRIMINATION", "Odi o discriminació"),
    INCITEMENT_VIOLENCE("INCITEMENT_VIOLENCE", "Incitació a la violència"),
    CANCELLED("CANCELLED", "S’ha cancel·lat"),
    INCORRECT_INFO("INCORRECT_INFO", "Informació incorrecta"),
    DUPLICATE("DUPLICATE", "Esdeveniment duplicat"),
    NOT_CULTURAL("NOT_CULTURAL", "No és un esdeveniment cultural"),
    POSSIBLE_SCAM("POSSIBLE_SCAM", "Possible estafa"),
    INAPPROPRIATE_CONTENT("INAPPROPRIATE_CONTENT", "Contingut inadequat"),
    OTHER("OTHER", "Altres")
}

private enum class HideIntent { EVENT, ARTIST, CATEGORY }

@Composable
fun EventReportMenuButton(
    state: AgendaState,
    concert: Concert,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    compact: Boolean = true
) {
    var menuExpanded by remember(concert.id) { mutableStateOf(false) }
    var showReportDialog by remember(concert.id) { mutableStateOf(false) }
    var chooseArtist by remember(concert.id) { mutableStateOf(false) }
    var pendingHide by remember(concert.id) { mutableStateOf<Pair<HideIntent, String>?>(null) }
    val availableArtists = state.hideableArtists(concert)

    Box(modifier = modifier) {
        IconButton(
            onClick = { menuExpanded = true },
            modifier = Modifier.size(if (compact) 30.dp else 40.dp)
        ) {
            Icon(
                Icons.Outlined.MoreVert,
                contentDescription = "Més opcions",
                modifier = Modifier.size(if (compact) 18.dp else 22.dp),
                tint = tint
            )
        }
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            containerColor = Color.White
        ) {
            DropdownMenuItem(
                leadingIcon = { Icon(Icons.Outlined.ReportProblem, contentDescription = null) },
                text = { Text("Informar d’un problema") },
                onClick = { menuExpanded = false; showReportDialog = true }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("No mostrar esdeveniment") },
                onClick = {
                    menuExpanded = false
                    pendingHide = HideIntent.EVENT to concert.title
                }
            )
            DropdownMenuItem(
                enabled = availableArtists.isNotEmpty(),
                text = { Text("No mostrar artista") },
                onClick = {
                    menuExpanded = false
                    if (availableArtists.size == 1) pendingHide = HideIntent.ARTIST to availableArtists.single()
                    else chooseArtist = true
                }
            )
            DropdownMenuItem(
                text = { Text("No mostrar categoria") },
                onClick = {
                    menuExpanded = false
                    pendingHide = HideIntent.CATEGORY to concert.category.name
                }
            )
        }
    }

    if (showReportDialog) {
        EventReportDialog(
            state = state,
            concert = concert,
            onDismiss = { showReportDialog = false }
        )
    }

    if (chooseArtist) {
        AlertDialog(
            onDismissRequest = { chooseArtist = false },
            title = { Text("Quin artista vols deixar de veure?") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    availableArtists.forEach { artist ->
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                chooseArtist = false
                                pendingHide = HideIntent.ARTIST to artist
                            }
                        ) { Text(artist) }
                    }
                    Text(
                        "Després hauràs de confirmar l'exclusió.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { chooseArtist = false }) { Text("Cancel·lar") } }
        )
    }

    pendingHide?.let { (intent, target) ->
        val category = if (intent == HideIntent.CATEGORY) {
            CulturalCategory.values().firstOrNull { it.name == target }
        } else null
        val label = category?.label ?: target
        val explanation = when (intent) {
            HideIntent.EVENT -> "Este esdeveniment deixarà d'aparéixer al teu feed i a les cerques."
            HideIntent.ARTIST -> "No tornaran a mostrar-se esdeveniments només d’${target}. " +
                "Si en un esdeveniment actuen altres artistes que no has exclòs, continuarà apareixent."
            HideIntent.CATEGORY -> "No tornaran a mostrar-se esdeveniments de la categoria «${label}». " +
                "L'exclusió és permanent fins que la desfaces."
        }
        AlertDialog(
            onDismissRequest = { pendingHide = null },
            title = { Text("Confirmes que no vols veure «${label}»?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(explanation)
                    Text(
                        "Podràs modificar esta decisió en Personalització, dins de les opcions i del perfil.",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    when (intent) {
                        HideIntent.EVENT -> state.hideConcert(concert.id)
                        HideIntent.ARTIST -> state.blockArtist(target)
                        HideIntent.CATEGORY -> category?.let(state::blockCategory)
                    }
                    pendingHide = null
                }) { Text("Sí, no mostrar") }
            },
            dismissButton = { TextButton(onClick = { pendingHide = null }) { Text("Cancel·lar") } }
        )
    }
}

/**
 * Personalització → Bloquejats: mateixa jerarquia visual que les llistes
 * d'artistes, amb fotografia i acció individual de restauració.
 */
@Composable
fun VisibilityExclusionsSection(state: AgendaState) {
    // No mostrem fitxes passades ni identificadors sense fitxa al catàleg actual.
    // L'exclusió es conserva a les preferències per no reintroduir actes antics.
    val blockedEvents = state.activeHiddenConcerts()
    val blockedArtists = state.blockedArtists.sorted()
    val blockedCategories = state.blockedCategories.sortedBy { it.label }
    val blockedSubcategories = state.blockedSubcategories.sorted()

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        if (blockedEvents.isEmpty() && blockedArtists.isEmpty() &&
            blockedCategories.isEmpty() && blockedSubcategories.isEmpty()) {
            Text(
                "No tens cap exclusió activa.",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (blockedEvents.isNotEmpty()) {
            Text("Esdeveniments", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            blockedEvents.forEach { event ->
                VisibilityPreferenceRow(
                    label = event.title,
                    subtitle = "${AgendaDateFormat.date(event.date)} · ${event.town}",
                    photoUris = state.cardImageCandidates(event),
                    onRestore = { state.unhideConcert(event.id) }
                )
            }
        }
        if (blockedArtists.isNotEmpty()) {
            Text("Artistes i grups", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            blockedArtists.forEach { artist ->
                VisibilityPreferenceRow(
                    label = artist,
                    photoUris = listOf(state.publicPhotoForArtist(artist)),
                    onRestore = { state.unblockArtist(artist) }
                )
            }
        }
        if (blockedCategories.isNotEmpty()) {
            Text("Categories", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            blockedCategories.forEach { category ->
                VisibilityPreferenceRow(
                    label = category.label,
                    photoUris = listOf(state.publicPhotoForCategory(category)),
                    onRestore = { state.unblockCategory(category) }
                )
            }
        }
        if (blockedSubcategories.isNotEmpty()) {
            Text("Subcategories", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            blockedSubcategories.forEach { code ->
                CategoryTaxonomy.findByCode(code)?.let { sub ->
                    VisibilityPreferenceRow(
                        label = sub.label,
                        subtitle = sub.category.label,
                        photoUris = listOf(state.publicPhotoForCategory(sub.category)),
                        onRestore = { state.unblockSubcategory(sub) }
                    )
                }
            }
        }
    }
}

@Composable
private fun VisibilityPreferenceRow(
    label: String,
    photoUris: List<String?>,
    subtitle: String? = null,
    onRestore: () -> Unit
) {
    val thumbnail = rememberImageBitmapFromUris(photoUris, maxDimension = 128)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(9.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .28f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(7.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail, contentDescription = label,
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                    )
                } else {
                    Text(label.take(1).uppercase(), fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(label, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                if (!subtitle.isNullOrBlank()) {
                    Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            TextButton(onClick = onRestore, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 5.dp)) {
                Text("Tornar a mostrar", fontSize = 11.sp, maxLines = 1)
            }
        }
    }
}

@Composable
fun EventReportDialog(
    state: AgendaState,
    concert: Concert,
    onDismiss: () -> Unit
) {
    var selected by remember(concert.id) { mutableStateOf<EventReportReason?>(null) }
    var details by remember(concert.id) { mutableStateOf("") }
    var sending by remember(concert.id) { mutableStateOf(false) }
    var error by remember(concert.id) { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = { if (!sending) onDismiss() },
        containerColor = Color.White,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    "Informar d’un problema",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF161616)
                )
                Text(
                    "Selecciona el motiu i, si cal, afegeix detalls.",
                    fontSize = 12.sp,
                    color = Color(0xFF6C6C6C)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF7F7F7),
                    border = BorderStroke(1.dp, Color(0xFFE8E8E8)),
                    tonalElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            concert.title,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color(0xFF181818)
                        )
                        val meta = listOf(AgendaDateFormat.date(concert.date), concert.town, concert.venue)
                            .map(String::trim)
                            .filter(String::isNotBlank)
                            .joinToString(" · ")
                        if (meta.isNotBlank()) {
                            Text(meta, fontSize = 12.sp, color = Color(0xFF707070))
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    "Què passa?",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color(0xFF222222)
                )
                Spacer(Modifier.height(6.dp))

                EventReportReason.entries.forEachIndexed { index, reason ->
                    val active = selected == reason
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !sending) {
                                selected = reason
                                error = null
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = .08f) else Color.White,
                        border = BorderStroke(
                            1.dp,
                            if (active) MaterialTheme.colorScheme.primary.copy(alpha = .55f) else Color(0xFFE7E7E7)
                        ),
                        tonalElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            if (active) {
                                Icon(
                                    Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Spacer(Modifier.size(18.dp))
                            }
                            Text(
                                reason.label,
                                fontSize = 13.sp,
                                color = Color(0xFF242424),
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                    if (index != EventReportReason.entries.lastIndex) Spacer(Modifier.height(5.dp))
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFECECEC))
                Spacer(Modifier.height(14.dp))
                Text(
                    "Detalls opcionals",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color(0xFF222222)
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = details,
                    onValueChange = { if (it.length <= 1500) details = it },
                    enabled = !sending,
                    placeholder = { Text("Explica’ns què passa") },
                    supportingText = { Text("${details.length}/1500") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(12.dp)
                )
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        dismissButton = {
            TextButton(enabled = !sending, onClick = onDismiss) { Text("Cancel·lar") }
        },
        confirmButton = {
            Button(
                enabled = !sending,
                onClick = {
                    val reason = selected
                    if (reason == null) {
                        error = "Selecciona primer el motiu del report."
                        Toast.makeText(context, "Selecciona un motiu", Toast.LENGTH_SHORT).show()
                    } else {
                        sending = true
                        error = null
                        state.submitEventReport(concert, reason.code, details) { ok, message ->
                            sending = false
                            if (ok) {
                                Toast.makeText(context, "Report enviat", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            } else {
                                val visibleMessage = message?.takeIf { it.isNotBlank() }
                                    ?: "No s’ha pogut enviar. Torna-ho a provar."
                                error = visibleMessage
                                Toast.makeText(context, visibleMessage, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            ) {
                Text(if (sending) "Enviant report…" else "Enviar report")
            }
        }
    )
}
