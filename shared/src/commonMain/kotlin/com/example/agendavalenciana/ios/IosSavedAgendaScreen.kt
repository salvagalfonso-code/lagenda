package com.example.agendavalenciana.ios

import com.example.agendavalenciana.model.IosAccountCachePolicy
import com.example.agendavalenciana.model.IosSavedAgendaPolicy
import com.example.agendavalenciana.model.IosSavedFestivalSlot
import com.example.agendavalenciana.model.IosFestivalSavedSlotPolicy
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

/** Android's two-tab "Les meues agendes" layout. Lists are selected within Guardats,
 * not displayed as a third main tab. All private changes use the authenticated API. */
@Composable
internal fun IosSavedAgendaScreen(
    accountId: Int,
    authenticated: Boolean,
    onLoginRequired: () -> Unit,
    user: IosUserState,
    lists: List<IosSavedList>,
    available: List<AgendaEventSummary>,
    today: String,
    api: IosAgendaApi,
    work: (suspend () -> Unit) -> Unit,
    onListsChanged: (List<IosSavedList>) -> Unit,
    onStatus: (String) -> Unit,
    onOpenEvent: (AgendaEventSummary, String) -> Unit,
    onToggleSaved: (AgendaEventSummary) -> Unit,
    onDisableReminder: (AgendaEventSummary) -> Unit,
    reminderTime: (AgendaEventSummary) -> String,
    festivalRevision: Int
) {
    var remindersTab by remember(accountId) { mutableStateOf(false) }
    var listId by remember(accountId) { mutableStateOf<Int?>(null) }
    var listMenu by remember { mutableStateOf(false) }
    var newListDialog by remember { mutableStateOf(false) }
    var createDialog by remember { mutableStateOf(false) }
    var joinDialog by remember { mutableStateOf(false) }
    var renameDialog by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var shareData by remember { mutableStateOf<IosSavedListShare?>(null) }
    var newName by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }
    var renameName by remember { mutableStateOf("") }
    var addSearch by remember { mutableStateOf("") }
    var localSlotRevision by remember(accountId) { mutableIntStateOf(0) }
    var slotToRemove by remember { mutableStateOf<Pair<AgendaEventSummary, IosSavedFestivalSlot>?>(null) }
    val selectedList = lists.firstOrNull { it.id == listId }
    val selectedIds = if (selectedList == null) {
        IosSavedAgendaPolicy.validEventIds(user.saved, lists.flatMap { it.eventIds }.toSet())
    } else selectedList.eventIds
    val eventsById = remember(available) { available.groupBy { it.id }.mapValues { it.value.first() } }
    val displayed = remember(available, selectedIds, today) {
        selectedIds.mapNotNull(eventsById::get).filter {
            IosSavedAgendaPolicy.eventStillUpcoming(it.date, it.endDate, today)
        }.distinctBy { it.id }
    }
    // Saved festival performances are local on this device and scoped to this account.
    // Only the General view aggregates them; a shared list contains events, not private slots.
    val slots = remember(accountId, available, today, festivalRevision, localSlotRevision) {
        available.filter { it.isFestival }.distinctBy { it.id }.flatMap { festival ->
            val key = IosAccountCachePolicy.key("festival_schedule_${festival.id}", accountId.takeIf { authenticated })
            val saved = IosFestivalSavedSlotPolicy.decode(platformReadPublicCache(key))
            IosSavedAgendaPolicy.activeSlots(saved, festival.id, today).map { festival to it }
        }
    }
    val rows = remember(displayed, slots, selectedList, today) {
        val eventRows: List<IosSavedAgendaRow> = displayed.map { IosSavedAgendaRow.Event(it) }
        val performanceRows: List<IosSavedAgendaRow> = if (selectedList == null) slots.map {
            IosSavedAgendaRow.Performance(it.first, it.second)
        } else emptyList()
        (eventRows + performanceRows).sortedWith(compareBy<IosSavedAgendaRow> { it.day }
            .thenBy { it.time }.thenBy { it.sortId })
    }
    fun reloadLists(message: String) {
        if (!authenticated) { onLoginRequired(); return }
        work { onListsChanged(api.savedLists()); onStatus(message) }
    }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Text("Les meues agendes", fontSize = 22.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 15.dp, top = 17.dp, bottom = 10.dp))
        Row(Modifier.padding(start = 15.dp, end = 15.dp, top = 5.dp, bottom = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(false to "Guardats", true to "Recordatoris").forEach { (isReminder, title) ->
                Box(Modifier.clip(RoundedCornerShape(9.dp))
                    .background(if (remindersTab == isReminder) AgendaLavender else Color.White)
                    .then(if (remindersTab != isReminder) Modifier.border(1.dp,Color(0xFFCCCCCC),RoundedCornerShape(9.dp)) else Modifier)
                    .clickable { remindersTab = isReminder }
                    .padding(horizontal = 15.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                    Text(title, fontWeight = if(remindersTab == isReminder) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp, color = AgendaBlack)
                }
            }
        }
        if (!remindersTab) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    Surface(shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color(0xFFE5E5E5)),
                        color = Color.White, modifier = Modifier.fillMaxWidth().clickable { listMenu = true }) {
                        Text("${selectedList?.name ?: "General"}  ▾", modifier = Modifier.padding(horizontal = 14.dp, vertical = 17.dp),
                            maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                    }
                    DropdownMenu(expanded = listMenu, onDismissRequest = { listMenu = false }) {
                        DropdownMenuItem(text = { Text("General · tots els guardats") }, onClick = {
                            listId = null; listMenu = false
                        })
                        lists.forEach { list ->
                            DropdownMenuItem(text = { Text(list.name + if (list.owner) "" else " · compartida") },
                                onClick = { listId = list.id; listMenu = false })
                        }
                    }
                }
                Box(Modifier.height(53.dp).clip(RoundedCornerShape(12.dp))
                    .background(AgendaBlack).clickable {
                        if (authenticated) newListDialog = true else onLoginRequired()
                    }
                    .padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                    Text("+ Nova llista", maxLines = 1, color = Color.White,
                        fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }
            if (selectedList != null) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 15.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (selectedList.owner && authenticated) {
                        TextButton(onClick = {
                            if (selectedList.shareCode.isNotBlank() && selectedList.inviteUrl.isNotBlank()) {
                                shareData = IosSavedListShare(selectedList.inviteUrl, selectedList.shareCode)
                            } else work { shareData = api.shareSavedList(selectedList.id) }
                        }) { Text("Compartir", fontSize = 12.sp) }
                        TextButton(onClick = { renameName = selectedList.name; renameDialog = true }) {
                            Text("Editar", fontSize = 12.sp)
                        }
                        TextButton(onClick = { confirmDelete = true }) { Text("Eliminar", fontSize = 12.sp) }
                    } else {
                        Text("Compartida amb tu · només lectura", modifier = Modifier.weight(1f),
                            fontSize = 11.sp, color = Color.Gray)
                        TextButton(onClick = { confirmDelete = true }) { Text("Deixar", fontSize = 12.sp) }
                    }
                }
                if (selectedList.owner) {
                    OutlinedTextField(value = addSearch, onValueChange = { addSearch = it.take(80) },
                        label = { Text("Afegir activitat a la llista") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp))
                    if (addSearch.trim().length >= 2) {
                        val matches = available.asSequence().filter { it.id !in selectedList.eventIds && it.matches(addSearch) }
                            .distinctBy { it.id }.take(5).toList()
                        matches.forEach { match ->
                            TextButton(onClick = { work {
                                api.changeSavedListEvent(selectedList.id, match.id, true)
                                onListsChanged(api.savedLists()); addSearch = ""; onStatus("Activitat afegida")
                            } }, modifier = Modifier.padding(horizontal = 12.dp)) {
                                Text("+ ${match.title}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
        if (remindersTab) {
            val reminders = remember(available, user.alerts, today) {
                user.alerts.mapNotNull(eventsById::get)
                    .filter { IosSavedAgendaPolicy.eventStillUpcoming(it.date, it.endDate, today) }
                    .distinctBy { it.id }.sortedWith(compareBy<AgendaEventSummary> { it.date }.thenBy { it.time }.thenBy { it.id })
            }
            if (reminders.isEmpty()) Text("No tens recordatoris actius per a esdeveniments disponibles.",
                modifier = Modifier.padding(18.dp), fontSize = 14.sp)
            else LazyColumn(contentPadding = PaddingValues(horizontal = 15.dp, vertical = 7.dp)) {
                items(reminders, key = { "reminder_${it.id}" }) { ev ->
                    IosSavedCompactCard(ev, note = reminderTime(ev).replace('T', ' ').takeIf(String::isNotBlank).orEmpty(),
                        onOpen = { onOpenEvent(ev, "") }, trailing = {
                            TextButton(onClick = { onDisableReminder(ev) }) {
                                Text("🔔", fontSize = 22.sp, color = Color(0xFF1F1F1F))
                            }
                        })
                }
            }
        } else if (rows.isEmpty()) {
            Text(if (selectedList == null) "Encara no tens cap esdeveniment o actuació guardats."
                else "Esta llista encara no té esdeveniments.", modifier = Modifier.padding(18.dp))
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 15.dp, vertical = 6.dp)) {
                items(rows, key = { it.rowId }) { row ->
                    when (row) {
                        is IosSavedAgendaRow.Event -> IosSavedCompactCard(row.event,
                            onOpen = { onOpenEvent(row.event, "") }, trailing = {
                                if (selectedList?.owner == true) {
                                    TextButton(onClick = { work {
                                        api.changeSavedListEvent(selectedList.id, row.event.id, false)
                                        onListsChanged(api.savedLists()); onStatus("Activitat eliminada de la llista")
                                    } }) { Text("Llevar", fontSize = 12.sp) }
                                } else if (selectedList == null) {
                                    TextButton(onClick = { onToggleSaved(row.event) }) {
                                        Text(if (row.event.id in user.saved) "▣" else "▢", fontSize = 22.sp)
                                    }
                                }
                            })
                        is IosSavedAgendaRow.Performance -> {
                            Column(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
                                Text("Actuació guardada · ${row.festival.title}", fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold, color = Color(0xFF986100))
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                    IosSavedPoster(row.festival.posterUrl, Modifier.size(60.dp))
                                    Column(Modifier.weight(1f).clickable { onOpenEvent(row.festival, row.slot.artist) }) {
                                        Text(row.slot.title.ifBlank { row.slot.artist }, fontWeight = FontWeight.Bold,
                                            maxLines = 2)
                                        Text("${row.slot.day} · ${row.slot.time}", fontSize = 12.sp)
                                    }
                                    TextButton(onClick = { slotToRemove = row.festival to row.slot }) {
                                        Text("✓", color = Color(0xFF986100))
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }

    if (newListDialog) AlertDialog(onDismissRequest = { newListDialog = false }, title = { Text("Nova llista") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TextButton(onClick = { newListDialog = false; createDialog = true }) { Text("Crear llista") }
            TextButton(onClick = { newListDialog = false; joinDialog = true }) { Text("Afegir compartida") }
        } }, confirmButton = { TextButton(onClick = { newListDialog = false }) { Text("Cancel·lar") } })
    if (createDialog) AlertDialog(onDismissRequest = { createDialog = false }, title = { Text("Crear llista") },
        text = { OutlinedTextField(newName, { newName = it.take(100) }, singleLine = true,
            label = { Text("Nom de la llista") }) },
        confirmButton = { TextButton(enabled = newName.trim().isNotBlank(), onClick = {
            val name = newName.trim(); createDialog = false
            work { api.createSavedList(name); onListsChanged(api.savedLists()); newName = ""; onStatus("Llista creada") }
        }) { Text("Crear") } }, dismissButton = { TextButton(onClick = { createDialog = false }) { Text("Cancel·lar") } })
    if (joinDialog) AlertDialog(onDismissRequest = { joinDialog = false }, title = { Text("Afegir compartida") },
        text = { OutlinedTextField(inviteCode, { inviteCode = it.take(200) }, singleLine = true,
            label = { Text("Codi curt o enllaç") }) },
        confirmButton = { TextButton(enabled = inviteCode.isNotBlank(), onClick = {
            val invite = inviteCode.trim(); joinDialog = false
            work { api.joinSavedList(invite); inviteCode = ""; onListsChanged(api.savedLists()); onStatus("Llista incorporada") }
        }) { Text("Afegir") } }, dismissButton = { TextButton(onClick = { joinDialog = false }) { Text("Cancel·lar") } })
    if (renameDialog && selectedList != null) AlertDialog(onDismissRequest = { renameDialog = false },
        title = { Text("Canviar nom de la llista") },
        text = { OutlinedTextField(renameName, { renameName = it.take(100) }, singleLine = true) },
        confirmButton = { TextButton(enabled = renameName.trim().isNotBlank(), onClick = {
            val id = selectedList.id; val name = renameName.trim(); renameDialog = false
            work { api.renameSavedList(id, name); onListsChanged(api.savedLists()); onStatus("Nom modificat") }
        }) { Text("Guardar") } }, dismissButton = { TextButton(onClick = { renameDialog = false }) { Text("Cancel·lar") } })
    if (confirmDelete && selectedList != null) AlertDialog(onDismissRequest = { confirmDelete = false },
        title = { Text(if (selectedList.owner) "Eliminar esta llista?" else "Deixar de seguir la llista?") },
        text = { Text(if (selectedList.owner) "Els esdeveniments continuaran guardats a l'Agenda."
            else "No eliminaràs la llista original, només la llevaràs dels teus Guardats.") },
        confirmButton = { TextButton(onClick = {
            val id = selectedList.id; val isOwner = selectedList.owner; confirmDelete = false
            work {
                if (isOwner) api.deleteSavedList(id) else api.leaveSavedList(id)
                onListsChanged(api.savedLists()); listId = null; onStatus("Llista actualitzada")
            }
        }) { Text(if (selectedList.owner) "Eliminar" else "Deixar") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel·lar") } })
    shareData?.let { shared ->
        AlertDialog(onDismissRequest = { shareData = null }, title = { Text("Compartir llista") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("Codi: ${shared.shareCode}", fontWeight = FontWeight.Bold)
                androidx.compose.foundation.text.selection.SelectionContainer { Text(shared.inviteUrl, fontSize = 12.sp) }
            } }, confirmButton = {
                TextButton(onClick = {
                    IosNativeBridge.current?.shareText("Llista de L'Agenda: ${shared.inviteUrl}\nCodi: ${shared.shareCode}")
                }) { Text("Compartir") }
            }, dismissButton = { TextButton(onClick = { shareData = null }) { Text("Tancar") } })
    }
    slotToRemove?.let { (event, slot) ->
        AlertDialog(onDismissRequest = { slotToRemove = null }, title = { Text("Llevar actuació guardada?") },
            text = { Text(slot.title.ifBlank { slot.artist }) },
            confirmButton = { TextButton(onClick = {
                val cacheKey = IosAccountCachePolicy.key("festival_schedule_${event.id}", accountId.takeIf { authenticated })
                val current = IosFestivalSavedSlotPolicy.decode(platformReadPublicCache(cacheKey))
                platformWritePublicCache(cacheKey, IosFestivalSavedSlotPolicy.encode(current - slot.originalKey))
                slotToRemove = null
                localSlotRevision++
                onStatus("Actuació llevada de Guardats")
            }) { Text("Llevar") } },
            dismissButton = { TextButton(onClick = { slotToRemove = null }) { Text("Mantindre") } })
    }
}

private sealed interface IosSavedAgendaRow {
    val day: String
    val time: String
    val sortId: Int
    val rowId: String
    data class Event(val event: AgendaEventSummary): IosSavedAgendaRow {
        override val day get() = event.date
        override val time get() = event.time
        override val sortId get() = event.id
        override val rowId get() = "event_${event.id}"
    }
    data class Performance(val festival: AgendaEventSummary, val slot: IosSavedFestivalSlot): IosSavedAgendaRow {
        override val day get() = slot.day
        override val time get() = slot.time
        override val sortId get() = festival.id
        override val rowId get() = "slot_${slot.originalKey}"
    }
}

@Composable
private fun IosSavedPoster(url: String, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFFF0EDF4)), contentAlignment = Alignment.Center) {
        if (url.isNotBlank()) AsyncImage(model = url, contentDescription = "Cartell", modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop)
        else Text("♪", color = Color.DarkGray)
    }
}

@Composable
private fun IosSavedCompactCard(ev: AgendaEventSummary, note: String = "", onOpen: () -> Unit,
                                trailing: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onOpen)
        .padding(horizontal = 7.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)) {
        // Fixed 4:5 thumbnail and subtle category border, matching the Android cards.
        Box(Modifier.width(70.dp).height(88.dp)
            .border(1.dp, Color(0xFF9A986A), RoundedCornerShape(9.dp)).padding(2.dp)) {
            IosSavedPoster(ev.posterUrl, Modifier.fillMaxSize())
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(ev.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AgendaBlack,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (note.isNotBlank()) Text("Avís: $note", fontSize = 11.sp, color = Color.DarkGray,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("📍 " + listOf(ev.town, ev.venue).filter(String::isNotBlank).joinToString(" · "),
                fontSize = 12.sp, color = Color(0xFF4B4B4B), maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Text("◷ ${ev.time.ifBlank { "—" }}   ·   🎟 " +
                IosUnfilteredFeedPolicy.ticketLabel(ev.ticketAvailable, ev.price),
                fontSize = 12.sp, maxLines = 1, color = Color.DarkGray,
                overflow = TextOverflow.Ellipsis)
        }
        Column(Modifier.width(49.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(ev.category, maxLines = 1, overflow = TextOverflow.Ellipsis,
                fontSize = 9.sp, color = Color(0xFF927F85))
            Text(IosUnfilteredFeedPolicy.cardDate(ev.date), fontWeight = FontWeight.Bold,
                fontSize = 11.sp, maxLines = 1)
            trailing()
        }
    }
    HorizontalDivider(color = Color(0xFFEEEEEE))
}
