package com.example.agendavalenciana.ui

import com.example.agendavalenciana.model.AgendaDateFormat
import com.example.agendavalenciana.data.EventReminderScheduler

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.agendavalenciana.model.Concert
import com.example.agendavalenciana.model.FriendsCircle
import com.example.agendavalenciana.state.AgendaState
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * Selector de guardats de L’Agenda: una sola llista contínua, compacta i de selecció múltiple.
 * La distribució és purament visual; no altera el guardat ni el vincle amb el servidor.
 */
@Composable
internal fun SavedCollectionPickerDialog(state: AgendaState, eventId: Int, onDismiss: () -> Unit) {
    var newListName by remember(eventId) { mutableStateOf("") }
    var showNewList by remember(eventId) { mutableStateOf(false) }
    var result by remember(eventId) { mutableStateOf("") }
    var submitting by remember(eventId) { mutableStateOf(false) }
    LaunchedEffect(eventId, state.currentAccount?.serverUserId) { state.refreshSavedCollections() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE9E9E9))
        ) {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 570.dp).verticalScroll(rememberScrollState())
                    .padding(horizontal = 17.dp, vertical = 14.dp)
            ) {
                // Tipografia, contrast i accent de marca coherents amb Les meues agendes.
                Text("L’AGENDA  ·  GUARDATS", fontSize = 10.sp, letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold, color = Color(0xFF986100))
                Spacer(Modifier.height(3.dp))
                Text("Guardar esdeveniment", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    color = Color(0xFF181818))
                Spacer(Modifier.height(2.dp))
                Text("Selecciona les llistes que vulgues.", fontSize = 12.sp,
                    color = Color(0xFF777777))
                Spacer(Modifier.height(11.dp))

                // Guardats i llistes personals comparteixen el mateix bloc, sense separadors.
                CollectionOption("General · Guardats", eventId in state.savedConcerts) {
                    state.toggleSaved(eventId)
                }
                if (state.currentAccount == null) {
                    Spacer(Modifier.height(7.dp))
                    Text("Inicia sessió per crear llistes personals.", fontSize = 12.sp,
                        color = Color(0xFF777777))
                } else {
                    state.personalSavedLists.filter { it.owner }.forEach { list ->
                        CollectionOption(list.name, eventId in list.eventIds) {
                            if (!submitting) {
                                submitting = true
                                state.setPersonalListEvent(list.id, eventId, eventId !in list.eventIds) { _, msg ->
                                    result = msg
                                    submitting = false
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (!showNewList) {
                        TextButton(
                            onClick = { showNewList = true },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text("+ Nova llista", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF181818))
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            OutlinedTextField(
                                value = newListName,
                                onValueChange = { newListName = it },
                                placeholder = { Text("Nom de la llista", fontSize = 13.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(11.dp),
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                shape = RoundedCornerShape(11.dp),
                                color = if (!submitting && newListName.isNotBlank()) Color(0xFF1B1B1B)
                                        else Color(0xFFE7E7E7),
                                modifier = Modifier.clickable(enabled = !submitting && newListName.isNotBlank()) {
                                    submitting = true
                                    state.createPersonalSavedList(newListName.trim(), eventId) { ok, msg ->
                                        if (ok) {
                                            newListName = ""
                                            showNewList = false
                                        }
                                        result = msg
                                        submitting = false
                                    }
                                }
                            ) {
                                Text("Crear", modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                                    color = if (!submitting && newListName.isNotBlank()) Color.White
                                            else Color(0xFF777777),
                                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                if (result.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(result, fontSize = 12.sp, color = Color(0xFF606060))
                }
                state.collectionsError?.let {
                    Spacer(Modifier.height(5.dp))
                    Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Surface(
                        shape = RoundedCornerShape(11.dp),
                        color = Color(0xFF1B1B1B),
                        modifier = Modifier.clickable(onClick = onDismiss)
                    ) {
                        Text("Fet", modifier = Modifier.padding(horizontal = 23.dp, vertical = 10.dp),
                            color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/** Fila compacta, accessible i sense targetes o marges independents per a cada llista. */
@Composable
private fun CollectionOption(name: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 39.dp)
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onClick() }),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val checkShape = RoundedCornerShape(6.dp)
        Box(
            modifier = Modifier.size(21.dp)
                .background(if (selected) Color(0xFF1B1B1B) else Color.White, checkShape)
                .border(BorderStroke(1.dp, if (selected) Color(0xFF1B1B1B) else Color(0xFFD2D2D2)), checkShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) Text("✓", fontSize = 14.sp, color = Color.White,
                fontWeight = FontWeight.Bold)
        }
        Text(name, modifier = Modifier.weight(1f), color = Color(0xFF242424),
            fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Una actuació és una cita guardada a una hora concreta, no un altre esdeveniment. */
private data class SavedFestivalPerformance(val festival: Concert, val slot: SpecialScheduleItem)

private fun resolvedFestivalPerformances(state: AgendaState): List<SavedFestivalPerformance> =
    state.allSelectedFestivalSlots().flatMap { (id, selected) ->
        val festival = state.concertById(id) ?: return@flatMap emptyList()
        val remote = state.specialEventRemote(id)?.let { remoteSpecialConfig(it, festival) }
        val config = mergeFestivalConfig(festival, remote, specialEventConfigFor(festival))
        config.schedule
            .filter { it.date.isNotBlank() && scheduleSelectionKey(it) in selected }
            .distinctBy(::scheduleSelectionKey)
            .map { SavedFestivalPerformance(festival, it) }
    }.sortedWith(compareBy<SavedFestivalPerformance> { it.slot.date.take(10) }
        .thenBy { it.slot.time }.thenBy { it.festival.title })

private sealed interface SavedTimelineItem {
    val date: String
    val time: String
    data class Event(val concert: Concert) : SavedTimelineItem {
        override val date get() = concert.feedDate.take(10)
        override val time get() = concert.time
    }
    data class Performance(val saved: SavedFestivalPerformance) : SavedTimelineItem {
        override val date get() = saved.slot.date.take(10)
        override val time get() = saved.slot.time
    }
}

@Composable
private fun SavedFestivalPerformanceCard(
    state: AgendaState,
    saved: SavedFestivalPerformance,
    onOpenConcert: (Concert) -> Unit,
    onOpenArtist: (String) -> Unit
) {
    val festival = saved.festival
    val slot = saved.slot
    val artist = festivalScheduledArtist(festival, slot)
    val followed = artist != null && state.followedArtists.any { festivalArtistKey(it) == festivalArtistKey(artist) }
    val photo = artist?.let { state.publicPhotoForArtist(it) }
    val image = rememberImageBitmapFromUris(listOf(photo, slot.imageUri, festival.posterUri), maxDimension = 200)
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp),
        border = BorderStroke(1.dp, Color(0xFFF1AE29)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("Actuació guardada · ${festival.title}", fontSize = 11.sp, color = Color(0xFF986100), fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                if (image != null) Image(image, contentDescription = slot.title,
                    modifier = Modifier.size(54.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                Column(Modifier.weight(1f).clickable { onOpenConcert(festival.copy(selectedDate = slot.date.take(10))) }) {
                    Text(slot.title, fontWeight = FontWeight.Bold)
                    Text(listOf(AgendaDateFormat.date(slot.date), slot.time, slot.location).filter(String::isNotBlank).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = { state.toggleFestivalArtist(festival.id, scheduleSelectionKey(slot)) }) {
                    Text("✓ Guardat", fontSize = 11.sp)
                }
            }
            if (artist != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onOpenArtist(artist) }) { Text(artist, fontSize = 12.sp) }
                    TextButton(onClick = { state.toggleFollowArtist(artist) }) {
                        Text(if (followed) "✓ Seguint" else "+ Seguir", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/** Guardats és una agenda personal, independent dels filtres i del cercador del feed. */
@Composable
internal fun SavedCollectionsPanel(
    state: AgendaState,
    onOpenConcert: (Concert) -> Unit,
    onOpenArtist: (String) -> Unit
) {
    var selectedKey by remember { mutableStateOf("general") }
    var menuExpanded by remember { mutableStateOf(false) }
    var listActionDialog by remember { mutableStateOf(false) }
    var createDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var joinDialog by remember { mutableStateOf(false) }
    var joinCode by remember { mutableStateOf("") }
    var joinCodeError by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var shareData by remember { mutableStateOf<Pair<String, String>?>(null) }
    var shareName by remember { mutableStateOf("") }
    var renameId by remember { mutableStateOf<Int?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deleteListId by remember { mutableStateOf<Int?>(null) }
    var leaveListId by remember { mutableStateOf<Int?>(null) }
    var savedSubTab by remember { mutableStateOf(0) }
    var pendingReminderDeactivation by remember { mutableStateOf<Int?>(null) }
    var reminderListRevision by remember { mutableStateOf(0) }
    val context = LocalContext.current
    // Permet trobar guardats antics fora de la finestra carregada pel feed.
    LaunchedEffect(state.currentAccount?.serverUserId) {
        // Els guardats locals s'han de pintar abans de qualsevol petició remota.
        // Una única actualització diferida de llistes; la càrrega general ja pertany al feed.
        kotlinx.coroutines.delay(350)
        state.refreshSavedCollections()
        // Només carregar el catàleg complet quan falta alguna fitxa guardada.
        if (state.allCollectionSavedIds().any { state.concertById(it) == null }) {
            state.ensureFullAgendaLoaded("saved_missing_ids")
        }
    }
    // Guardats manté els IDs originals però no mostra esdeveniments caducats.
    // El dia cultural canvia a les 05:00, com en la resta de L’Agenda.
    val savedToday = state.agendaToday()
    val savedIds = state.allCollectionSavedIds()
    val allSaved = remember(savedIds, state.concerts, savedToday) {
        savedIds.mapNotNull(state::concertById).distinctBy { it.id }
            .filter { savedEventNotPast(it.date, it.endDate, it.scheduleDates, savedToday) }
    }
    // General és una agenda completa: ni finestreta de set dies ni topall de cinc.
    // No hereta cap cerca, filtre ni configuració del feed.
    val general = remember(allSaved) { savedGeneralAgenda(allSaved, ::dateForSaved) { it.time } }
    val festivalSelections = state.allSelectedFestivalSlots()
    val allFestivalPerformances = remember(festivalSelections, state.concerts, savedToday) {
        resolvedFestivalPerformances(state).filter { savedDateNotPast(it.slot.date, savedToday) }
    }
    val selectedList = state.personalSavedLists.firstOrNull { selectedKey == "list_${it.id}" }
    val actualKey = if (selectedKey == "general" || selectedList != null) selectedKey else "general"
    val shownEvents = if (actualKey == "general") general else {
        savedGeneralAgenda(
            (selectedList?.eventIds ?: emptySet()).mapNotNull(state::concertById).distinctBy { it.id }
                .filter { savedEventNotPast(it.date, it.endDate, it.scheduleDates, savedToday) },
            ::dateForSaved
        ) { it.time }
    }
    val selectedName = selectedList?.name ?: "General"
    val festivalPerformances = if (actualKey == "general") allFestivalPerformances else emptyList()
    val timeline = remember(shownEvents, festivalPerformances) {
        (shownEvents.map { SavedTimelineItem.Event(it) } +
            festivalPerformances.map { SavedTimelineItem.Performance(it) })
            .sortedWith(compareBy<SavedTimelineItem> { it.date }.thenBy { it.time })
    }
    val reminderIds = state.alertConcerts
    // La llista programada es torna a consultar quan canvia la campaneta o es cancela un avís.
    val scheduledReminders = remember(savedSubTab, reminderListRevision, reminderIds) {
        if (savedSubTab == 1) EventReminderScheduler.activeReminders(context) else emptyList()
    }
    val reminderEventIds = remember(scheduledReminders, reminderIds) {
        (scheduledReminders.map { it.eventId } + reminderIds).distinct()
    }
    // Un ID no resolt pot significar que falta part del catàleg local: recuperar-lo en segon pla.
    // NO esborrem els avisos sense verificar la fitxa, per a evitar pèrdues de configuració.
    LaunchedEffect(savedSubTab, reminderEventIds, state.currentAccount?.serverUserId) {
        if (savedSubTab == 1 && reminderEventIds.any { state.concertById(it) == null }) {
            state.ensureFullAgendaLoaded("reminder_missing_ids")
        }
    }

    Column(Modifier.fillMaxWidth().background(Color.White).padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Les meues agendes", modifier = Modifier.padding(horizontal = 15.dp),
            fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        Row(Modifier.fillMaxWidth().padding(horizontal = 15.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.material3.FilterChip(selected = savedSubTab == 0,
                onClick = { savedSubTab = 0 }, label = { Text("Guardats") })
            androidx.compose.material3.FilterChip(selected = savedSubTab == 1,
                onClick = { savedSubTab = 1 }, label = { Text("Recordatoris") })
        }
        if (savedSubTab == 1) {
            // No mostrar IDs sense fitxa, ni actes caducats. La persistència del recordatori
            // queda intacta mentre el catàleg es completa, per no perdre avisos legítims.
            val visibleReminders = reminderEventIds.mapNotNull(state::concertById)
                .distinctBy { it.id }
                .filter { savedEventNotPast(it.date, it.endDate, it.scheduleDates, savedToday) }
                .sortedWith(compareBy<Concert> { it.feedDate }.thenBy { it.time }.thenBy { it.id })
            if (visibleReminders.isEmpty()) {
                Text("No tens recordatoris actius per a esdeveniments disponibles.",
                    modifier = Modifier.padding(18.dp))
            } else visibleReminders.forEach { concert ->
                CompactConcertItem(
                    state = state,
                    concert = concert,
                    onOpenArtist = onOpenArtist,
                    onOpenConcert = { onOpenConcert(concert) },
                    onReminderClick = { pendingReminderDeactivation = concert.id }
                )
            }
        } else {
        Row(Modifier.fillMaxWidth().padding(horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) {
                Surface(shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE5E5E5)), color = Color.White,
                    modifier = Modifier.fillMaxWidth().clickable { menuExpanded = true }) {
                    Text("$selectedName  ▾", modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold, color = Color(0xFF181818))
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    containerColor = Color.White,
                    tonalElevation = 0.dp
                ) {
                    DropdownMenuItem(text = { Text("General · tots els guardats", color = Color(0xFF181818)) }, onClick = {
                        selectedKey = "general"; menuExpanded = false
                    })
                    state.personalSavedLists.forEach { list ->
                        DropdownMenuItem(text = { Text("${list.name}${if (list.owner) "" else " · compartida"}", color = Color(0xFF181818)) }, onClick = {
                            selectedKey = "list_${list.id}"; menuExpanded = false
                        })
                    }
                }
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (state.currentAccount != null) Color(0xFF1B1B1B) else Color(0xFFE7E7E7),
                modifier = Modifier.clickable(enabled = state.currentAccount != null) { listActionDialog = true }
            ) {
                Text("+ Nova llista", modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    maxLines = 1, color = if (state.currentAccount != null) Color.White else Color.Gray,
                    fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
        }
        if (selectedList != null) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 15.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (selectedList.owner) {
                    OutlinedButton(onClick = {
                        shareName = selectedList.name
                        // Una invitació ja generada és reutilitzable sense cap POST.
                        if (selectedList.inviteUrl.isNotBlank() && selectedList.shareCode.isNotBlank()) {
                            shareData = selectedList.inviteUrl to selectedList.shareCode
                            status = ""
                        } else {
                            state.sharePersonalSavedList(selectedList.id) { ok, msg, url, code ->
                                status = msg
                                if (ok && url.isNotBlank() && code.isNotBlank()) shareData = url to code
                            }
                        }
                    }) { Text("Compartir") }
                    OutlinedButton(onClick = { renameId = selectedList.id; renameText = selectedList.name }) { Text("Editar") }
                    TextButton(onClick = { deleteListId = selectedList.id }) { Text("Eliminar") }
                } else {
                    Text("Compartida amb tu · només lectura", style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = { leaveListId = selectedList.id }) { Text("Deixar") }
                }
            }
        }
        if (timeline.isEmpty()) {
            Text(if (actualKey == "general") "Encara no tens cap esdeveniment o actuació guardats."
                else "Esta llista encara no té esdeveniments.",
                modifier = Modifier.padding(18.dp), style = MaterialTheme.typography.bodyMedium)
        } else {
            timeline.forEach { entry ->
                if (entry is SavedTimelineItem.Performance) {
                    SavedFestivalPerformanceCard(state, entry.saved, onOpenConcert, onOpenArtist)
                } else {
                val concert = (entry as SavedTimelineItem.Event).concert
                // Sense xips ni etiquetes de llista. General és una única agenda seguida.
                CompactConcertItem(state, concert, onOpenArtist, { onOpenConcert(concert) })
                }
            }
        }
        if (state.currentAccount == null) Text("Inicia sessió per crear i compartir llistes.", modifier = Modifier.padding(horizontal = 15.dp))
        if (status.isNotBlank()) Text(status, modifier = Modifier.padding(horizontal = 15.dp), style = MaterialTheme.typography.bodySmall)
        state.collectionsError?.let { Text(it, modifier = Modifier.padding(horizontal = 15.dp), color = MaterialTheme.colorScheme.error) }
        }
    }
    val reminderIdToDisable = pendingReminderDeactivation
    if (reminderIdToDisable != null) {
        AlertDialog(
            onDismissRequest = { pendingReminderDeactivation = null },
            title = { Text("Desactivar recordatori?") },
            text = { Text("Vols desactivar este recordatori? No rebràs més avisos d’este esdeveniment fins que el tornes a activar.") },
            confirmButton = {
                TextButton(onClick = {
                    // Cancel·lar tant el WorkManager Android com la campaneta guardada al compte.
                    EventReminderScheduler.cancel(context, reminderIdToDisable)
                    if (reminderIdToDisable in state.alertConcerts) {
                        state.toggleConcertAlert(reminderIdToDisable)
                    }
                    state.reminderConfigurationChanged()
                    reminderListRevision++
                    pendingReminderDeactivation = null
                }) { Text("Desactivar") }
            },
            dismissButton = {
                TextButton(onClick = { pendingReminderDeactivation = null }) { Text("Mantindre") }
            }
        )
    }
    if (listActionDialog) Dialog(onDismissRequest = { listActionDialog = false }) {
        Surface(shape = RoundedCornerShape(18.dp), color = Color.White) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Nova llista", fontWeight = FontWeight.Bold, fontSize = 19.sp, color = Color(0xFF171717))
                Text("Què vols fer?", color = Color(0xFF666666), fontSize = 13.sp)
                SavedAgendaChoice("Crear llista", "Organitza els teus esdeveniments") {
                    listActionDialog = false; createDialog = true
                }
                SavedAgendaChoice("Afegir compartida", "Introduïx el codi curt d'una llista") {
                    listActionDialog = false; joinDialog = true
                }
                TextButton(onClick = { listActionDialog = false }, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel·lar", color = Color(0xFF444444))
                }
            }
        }
    }
    if (createDialog) Dialog(onDismissRequest = { createDialog = false }) {
        Surface(shape = RoundedCornerShape(18.dp), color = Color.White) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Crear llista", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF181818))
                OutlinedTextField(newName, { newName = it }, label = { Text("Nom de la llista") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp))
                SavedAgendaPrimaryButton("Crear llista", newName.isNotBlank()) {
                    val name = newName.trim()
                    state.createPersonalSavedList(name) { _, msg -> status = msg }
                    newName = ""; createDialog = false
                }
                TextButton(onClick = { createDialog = false }, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel·lar", color = Color(0xFF444444))
                }
            }
        }
    }
    if (joinDialog) Dialog(onDismissRequest = { joinDialog = false }) {
        Surface(shape = RoundedCornerShape(18.dp), color = Color.White) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Afegir compartida", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF181818))
                Text("Escriu només el codi curt que t'han compartit.", color = Color(0xFF666666), fontSize = 13.sp)
                OutlinedTextField(joinCode, { joinCode = it.uppercase().take(16); joinCodeError = "" },
                    label = { Text("Codi curt") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp))
                if (joinCodeError.isNotBlank()) Text(joinCodeError, color = Color(0xFFAC382C), fontSize = 12.sp)
                SavedAgendaPrimaryButton("Afegir llista", joinCode.isNotBlank()) {
                    val code = joinCode.trim().uppercase()
                    if (!code.matches(Regex("(?:[A-F0-9]{10}|[A-F0-9]{16})"))) {
                        joinCodeError = "Escriu un codi vàlid de 10 caràcters (o de 16 si és antic)."
                    } else {
                        state.joinSharedSavedList(code) { _, msg -> status = msg }
                        joinCode = ""; joinDialog = false
                    }
                }
                TextButton(onClick = { joinDialog = false }, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel·lar", color = Color(0xFF444444))
                }
            }
        }
    }
    shareData?.let { (url, code) ->
        AlertDialog(onDismissRequest = { shareData = null },
            title = { Text("Compartir · $shareName") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Els teus amics podran seguir la llista i vore les noves incorporacions.")
                Text(url, style = MaterialTheme.typography.bodySmall)
                Text("Codi: $code", fontWeight = FontWeight.Bold)
            } },
            confirmButton = { TextButton(onClick = {
                val message = "T'he compartit la llista $shareName de l'Agenda:\n$url\nCodi: $code"
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"; putExtra(Intent.EXTRA_TEXT, message)
                }
                runCatching { context.startActivity(Intent.createChooser(intent, "Compartir per WhatsApp o altres")) }
                    .onFailure { status = "No hi ha cap aplicació per compartir. Pots copiar l’enllaç." }
            }) { Text("WhatsApp / Compartir") } },
            dismissButton = { Row {
                TextButton(onClick = {
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager)
                        .setPrimaryClip(ClipData.newPlainText("Enllaç de llista", url))
                    status = "Enllaç copiat."
                }) { Text("Copiar enllaç") }
                TextButton(onClick = {
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager)
                        .setPrimaryClip(ClipData.newPlainText("Codi llista", code))
                    status = "Codi copiat."
                }) { Text("Codi") }
                TextButton(onClick = { shareData = null }) { Text("Tancar") }
            } })
    }
    renameId?.let { id -> AlertDialog(onDismissRequest = { renameId = null },
        title = { Text("Canviar nom de la llista") },
        text = { OutlinedTextField(renameText, { renameText = it }, singleLine = true) },
        confirmButton = { TextButton(onClick = {
            state.renamePersonalSavedList(id, renameText) { _, msg -> status = msg }; renameId = null
        }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = { renameId = null }) { Text("Cancel·lar") } }) }
    deleteListId?.let { id -> AlertDialog(onDismissRequest = { deleteListId = null },
        title = { Text("Eliminar esta llista?") },
        text = { Text("Només s'eliminarà la llista. Els esdeveniments continuaran a l'Agenda.") },
        confirmButton = { TextButton(onClick = {
            state.deletePersonalSavedList(id) { _, msg -> status = msg }
            selectedKey = "general"; deleteListId = null
        }) { Text("Eliminar") } },
        dismissButton = { TextButton(onClick = { deleteListId = null }) { Text("Cancel·lar") } }) }
    leaveListId?.let { id -> AlertDialog(onDismissRequest = { leaveListId = null },
        title = { Text("Deixar de seguir la llista?") },
        text = { Text("No eliminaràs la llista de l'amic, només la llevaràs dels teus Guardats.") },
        confirmButton = { TextButton(onClick = {
            state.leaveSharedSavedList(id) { _, msg -> status = msg }
            selectedKey = "general"; leaveListId = null
        }) { Text("Deixar") } },
        dismissButton = { TextButton(onClick = { leaveListId = null }) { Text("Cancel·lar") } }) }
}


@Composable
private fun SavedAgendaPrimaryButton(title: String, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp),
        color = if (enabled) Color(0xFF1B1B1B) else Color(0xFFE7E7E7),
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick)) {
        Text(title, modifier = Modifier.padding(14.dp),
            color = if (enabled) Color.White else Color.Gray, fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun SavedAgendaChoice(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color(0xFFE8E8E8)),
        color = Color(0xFFFAFAFA), modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(horizontal = 15.dp, vertical = 13.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = Color(0xFF1B1B1B))
            Text(subtitle, fontSize = 12.sp, color = Color(0xFF777777))
        }
    }
}

private fun savedListColor(index: Int): Color = listOf(
    Color(0xFF067C74), Color(0xFF7353A6), Color(0xFFB45A19),
    Color(0xFF3367B1), Color(0xFFA33565), Color(0xFF607C20), Color(0xFFB35047)
)[index % 7]

private fun dateForSaved(concert: Concert): LocalDate =
    runCatching { LocalDate.parse(concert.feedDate.take(10)) }.getOrDefault(LocalDate.MIN)

internal fun extractSavedListInviteToken(raw: String): String? {
    val trimmed = raw.trim()
    val token = runCatching { Uri.parse(trimmed) }.getOrNull()?.let { uri ->
        if (uri.scheme == "agendavalenciana" && uri.host == "lists") uri.pathSegments.firstOrNull()
        else if (uri.scheme == "https" && uri.host == "agendaval.com" && uri.path == "/app/lists.php")
            uri.getQueryParameter("invite")
        else null
    } ?: trimmed
    return token.takeIf { it.matches(Regex("(?:[a-f0-9]{48}|[A-Fa-f0-9]{10}|[A-Fa-f0-9]{16})")) }?.let {
        if (it.length == 10 || it.length == 16) it.uppercase() else it
    }
}

/** Els grups d'amics són persistents, independents de xats sobre concerts. */
@Composable
internal fun FriendsCirclesCommunityPanel(state: AgendaState, onOpenConcert: (Concert) -> Unit) {
    var newName by remember { mutableStateOf("") }
    var invite by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var openGroup by remember { mutableStateOf<Int?>(null) }
    val ctx = LocalContext.current
    LaunchedEffect(state.currentAccount?.serverUserId) {
        if (state.currentAccount == null) return@LaunchedEffect
        while (true) {
            state.refreshSavedCollections()
            delay(45_000L)
        }
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text("Grups d’amics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Creeu una agenda conjunta, proposeu esdeveniments i comenteu-los en el grup.",
            style = MaterialTheme.typography.bodySmall)
        if (state.currentAccount == null) {
            Text("Inicia sessió per crear un grup o entrar-hi amb un enllaç.")
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                OutlinedTextField(newName, { newName = it }, label = { Text("Nom del grup") },
                    modifier = Modifier.weight(1f), singleLine = true)
                Button(enabled = newName.isNotBlank(), onClick = {
                    state.createFriendsCircle(newName) { ok, msg ->
                        if (ok) newName = ""
                        status = msg
                    }
                }) { Text("Crear") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                OutlinedTextField(invite, { invite = it }, label = { Text("Enllaç d’invitació") },
                    modifier = Modifier.weight(1f), singleLine = true)
                Button(enabled = invite.isNotBlank(), onClick = {
                    val token = extractFriendsInviteToken(invite)
                    if (token == null) status = "Enllaç d’invitació no vàlid."
                    else state.joinFriendsCircle(token) { ok, msg ->
                        if (ok) invite = ""
                        status = msg
                    }
                }) { Text("Entrar") }
            }
        }
        state.friendsCircles.forEach { group ->
            Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(group.name, fontWeight = FontWeight.Bold)
                    Text("${group.membersCount} membres · ${group.eventIds.size} esdeveniments" +
                        if (group.unreadCount > 0) " · ${group.unreadCount} novetats" else "",
                        style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        OutlinedButton(onClick = { openGroup = group.id }) { Text("Conversa i agenda") }
                        TextButton(onClick = {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "Uneix-te al grup ${group.name} de l’Agenda: ${group.inviteUrl}")
                            }
                            ctx.startActivity(Intent.createChooser(send, "Convidar amics"))
                        }) { Text("Convidar") }
                    }
                }
            }
        }
        if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodySmall)
        state.collectionsError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
    openGroup?.let { id -> state.friendsCircles.firstOrNull { it.id == id }?.let { group ->
        FriendsCircleDialog(state, group, { openGroup = null }, onOpenConcert)
    } }
}

internal fun extractFriendsInviteToken(raw: String): String? {
    val token = raw.trim().let { text ->
        runCatching { Uri.parse(text) }.getOrNull()?.let { parsed ->
            if (parsed.scheme == "agendavalenciana" && parsed.host == "friends") parsed.pathSegments.firstOrNull()
            else if ((parsed.scheme == "https" || parsed.scheme == "http") && (parsed.host == "agendaval.com" || parsed.host == "quimerafest.com")
                && parsed.path == "/app/friends.php") parsed.getQueryParameter("invite")
            else null
        } ?: text
    }
    return token.takeIf { it.matches(Regex("[a-f0-9]{48}")) }
}

/** Targeta compacta per a les propostes de l'agenda d'un grup d'amics. */
@Composable
private fun SavedCollectionEventRow(
    concert: Concert,
    onOpenConcert: (Concert) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onOpenConcert(concert) },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                concert.title.ifBlank { concert.artist },
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                listOf(concert.feedDate, concert.time, concert.town)
                    .filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun FriendsCircleDialog(
    state: AgendaState,
    group: FriendsCircle,
    onDismiss: () -> Unit,
    onOpenConcert: (Concert) -> Unit
) {
    var typed by remember(group.id) { mutableStateOf("") }
    var status by remember(group.id) { mutableStateOf("") }
    var confirmLeave by remember(group.id) { mutableStateOf(false) }
    var confirmDelete by remember(group.id) { mutableStateOf(false) }
    LaunchedEffect(group.id) {
        while (true) {
            state.refreshFriendsCircleMessages(group.id)
            state.markFriendsCircleRead(group.id)
            delay(20_000L)
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().heightIn(max = 650.dp).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(group.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Agenda compartida · ${group.membersCount} membres", style = MaterialTheme.typography.bodySmall)
                val today = state.agendaToday()
                val events = group.eventIds.mapNotNull(state::concertById)
                    .filter { savedEventNotPast(it.date, it.endDate, it.scheduleDates, today) }
                    .sortedBy(::dateForSaved)
                if (events.isEmpty()) Text("Encara no heu afegit cap esdeveniment.", style = MaterialTheme.typography.bodySmall)
                events.take(6).forEach { SavedCollectionEventRow(it, onOpenConcert = { selected ->
                    onDismiss(); onOpenConcert(selected)
                }) }
                Divider()
                Text("Conversa i avisos", fontWeight = FontWeight.SemiBold)
                val messages = state.friendMessages[group.id].orEmpty()
                LazyColumn(Modifier.weight(1f, fill = false).fillMaxWidth().heightIn(min = 75.dp)) {
                    items(messages, key = { it.id }) { message ->
                        Column(Modifier.padding(vertical = 5.dp)) {
                            Text(message.author + " · " + AgendaDateFormat.dateTime(message.createdAt), style = MaterialTheme.typography.labelSmall)
                            Text(message.text, style = MaterialTheme.typography.bodyMedium)
                            message.eventId?.let { eventId -> state.concertById(eventId)?.let { concert ->
                                TextButton(onClick = { onDismiss(); onOpenConcert(concert) }) { Text("Veure esdeveniment") }
                            } }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    OutlinedTextField(typed, { typed = it }, modifier = Modifier.weight(1f),
                        label = { Text("Missatge") }, maxLines = 3)
                    Button(enabled = typed.isNotBlank(), onClick = {
                        state.sendFriendsCircleMessage(group.id, typed) { ok, msg ->
                            if (ok) { typed = ""; state.refreshFriendsCircleMessages(group.id) }
                            status = msg
                        }
                    }) { Text("Enviar") }
                }
                if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodySmall)
                Row {
                    if (group.creator) {
                        TextButton(onClick = { state.rotateFriendsCircleInvite(group.id) { _, msg -> status = msg } }) {
                            Text("Revocar enllaç")
                        }
                        TextButton(onClick = { confirmDelete = true }) { Text("Eliminar grup") }
                    } else TextButton(onClick = { confirmLeave = true }) { Text("Eixir del grup") }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Tancar") }
                }
            }
        }
    }
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false },
        title = { Text("Eliminar el grup?") },
        text = { Text("S'eliminaran l'agenda i els missatges compartits per a tots els membres.") },
        confirmButton = { TextButton(onClick = {
            state.deleteFriendsCircle(group.id) { ok, msg -> if (ok) onDismiss() else status = msg }
            confirmDelete = false
        }) { Text("Eliminar") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel·lar") } })
    if (confirmLeave) AlertDialog(onDismissRequest = { confirmLeave = false }, title = { Text("Abandonar el grup?") },
        text = { Text("Deixaràs de vore l'agenda i els missatges compartits.") },
        confirmButton = { TextButton(onClick = {
            state.leaveFriendsCircle(group.id) { ok, msg -> if (ok) onDismiss() else status = msg }
            confirmLeave = false
        }) { Text("Eixir") } },
        dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Cancel·lar") } })
}
