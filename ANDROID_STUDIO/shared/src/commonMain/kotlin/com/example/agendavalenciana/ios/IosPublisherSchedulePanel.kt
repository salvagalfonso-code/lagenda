package com.example.agendavalenciana.ios

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Pas 41: independent management from generic community-update.
 * Generic events are not assumed to have a special-events schedule editor.
 */
@Composable
internal fun IosPublisherSchedulePanel(api: IosAgendaApi, ownerId: Int, eventId: Int, enabled: Boolean) {
    val scope = rememberCoroutineScope()
    var data by remember(eventId, ownerId) { mutableStateOf<IosSpecialScheduleAccess?>(null) }
    var editing by remember(eventId, ownerId) { mutableStateOf<IosSpecialScheduleSlot?>(null) }
    var creating by remember(eventId, ownerId) { mutableStateOf(false) }
    var busy by remember(eventId, ownerId) { mutableStateOf(false) }
    var deleteTarget by remember(eventId, ownerId) { mutableStateOf<IosSpecialScheduleSlot?>(null) }
    var message by remember(eventId, ownerId) { mutableStateOf("") }
    fun refresh() {
        if (busy) return
        busy = true; message = ""
        scope.launch {
            try {
                val fresh = api.publisherSchedule(eventId)
                if (api.activePersonalId() == ownerId) data = fresh
            } catch (ex: Exception) {
                if (api.activePersonalId() == ownerId) message = ex.message ?: "Programació no disponible en este esdeveniment."
            } finally { busy = false }
        }
    }
    LaunchedEffect(eventId, ownerId) { refresh() }
    Text("Programació de l'esdeveniment #$eventId", fontWeight = FontWeight.Bold)
    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.error)
    deleteTarget?.let { target ->
        AlertDialog(onDismissRequest = { if (!busy) deleteTarget = null },
            title = { Text("Eliminar actuació?") },
            text = { Text("${target.title} · ${target.date} ${target.startTime}. Esta acció modifica la programació del servidor.") },
            confirmButton = { TextButton(enabled = !busy, onClick = {
                deleteTarget = null; busy = true; message = ""
                scope.launch {
                    try {
                        val result = api.publisherDeleteSchedule(eventId, target.id)
                        if (api.activePersonalId() == ownerId) { data = result; message = "Actuació eliminada al servidor." }
                    } catch(ex: Exception) { message = ex.message ?: "Eliminació no confirmada." }
                    finally { busy = false }
                }
            }) { Text("Eliminar definitivament") } },
            dismissButton = { TextButton(enabled = !busy, onClick = { deleteTarget = null }) { Text("Cancel·lar") } })
    }
    val access = data
    if (access == null) {
        Text("Només els esdeveniments especials configurats al servidor disposen d'editor de programació.")
        OutlinedButton(enabled = enabled && !busy, onClick = ::refresh) { Text("Tornar a consultar") }
    } else if (!access.canEdit) {
        Text("El servidor no ha autoritzat l'edició dels horaris. Pots consultar-los però no canviar-los.")
        access.slots.forEach { Text("${it.date} ${it.startTime} · ${it.title}") }
    } else {
        if (creating || editing != null) {
            IosPublisherSlotForm(editing ?: IosSpecialScheduleSlot(0,eventId,"","",""),
                enabled = enabled && !busy,
                onCancel = { creating = false; editing = null }, onSave = { candidate ->
                    busy = true; message = ""
                    scope.launch {
                        try {
                            val result = api.publisherSaveSchedule(candidate)
                            if (api.activePersonalId() == ownerId) {
                                data = result; creating = false; editing = null
                                message = "Programació confirmada pel servidor."
                            }
                        } catch (ex: Exception) {
                            message = ex.message ?: "No s'ha pogut confirmar. No repetisques sense consultar."
                        } finally { busy = false }
                    }
                })
        } else {
            access.slots.sortedWith(compareBy<IosSpecialScheduleSlot> { it.date }.thenBy { it.startTime }.thenBy { it.sortOrder }).forEach { slot ->
                Text("${slot.date} ${slot.startTime} · ${slot.title}", fontWeight = FontWeight.Medium)
                Text(listOf(slot.stage, slot.artist).filter { it.isNotBlank() }.joinToString(" · "))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(enabled = enabled && !busy, onClick = { editing = slot }) { Text("Editar") }
                    TextButton(enabled = enabled && !busy, onClick = { deleteTarget = slot }) { Text("Eliminar") }
                }
                HorizontalDivider()
            }
            Button(enabled = enabled && !busy, onClick = { creating = true; editing = null }) { Text("Afegir actuació / sessió") }
            TextButton(enabled = enabled && !busy, onClick = ::refresh) { Text("Actualitzar des del servidor") }
        }
    }
}

@Composable
private fun IosPublisherSlotForm(initial: IosSpecialScheduleSlot, enabled: Boolean,
    onCancel: () -> Unit, onSave: (IosSpecialScheduleSlot) -> Unit) {
    var title by remember(initial.id) { mutableStateOf(initial.title) }
    var date by remember(initial.id) { mutableStateOf(initial.date) }
    var time by remember(initial.id) { mutableStateOf(initial.startTime) }
    var endTime by remember(initial.id) { mutableStateOf(initial.endTime) }
    var stage by remember(initial.id) { mutableStateOf(initial.stage) }
    var artist by remember(initial.id) { mutableStateOf(initial.artist) }
    var details by remember(initial.id) { mutableStateOf(initial.details) }
    var imageUrl by remember(initial.id) { mutableStateOf(initial.imageUrl) }
    var sort by remember(initial.id) { mutableStateOf(initial.sortOrder.toString()) }
    var error by remember(initial.id) { mutableStateOf("") }
    Text(if (initial.id > 0) "Editar actuació" else "Afegir actuació", fontWeight = FontWeight.Medium)
    OutlinedTextField(title, { title = it.take(220) }, label = { Text("Nom de l'actuació") }, modifier = Modifier.fillMaxWidth())
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(date, { date = it.take(10) }, label = { Text("Data AAAA-MM-DD") }, modifier = Modifier.weight(1f))
        OutlinedTextField(time, { time = it.take(5) }, label = { Text("Hora HH:MM") }, modifier = Modifier.weight(1f))
    }
    OutlinedTextField(endTime, { endTime = it.take(5) }, label = { Text("Hora final opcional") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(stage, { stage = it.take(220) }, label = { Text("Escenari / espai") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(artist, { artist = it.take(220) }, label = { Text("Artista / participant") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(details, { details = it.take(5000) }, label = { Text("Detalls") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(imageUrl, { imageUrl = it.take(2000) }, label = { Text("Imatge https (opcional)") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(sort, { sort = it.take(7) }, label = { Text("Ordre") }, modifier = Modifier.fillMaxWidth())
    if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(enabled = enabled, onClick = onCancel) { Text("Cancel·lar") }
        Button(enabled = enabled, onClick = {
            try {
                val candidate = initial.copy(title = title.trim(), date = date, startTime = time, endTime = endTime,
                    stage = stage.trim(), artist = artist.trim(), details = details.trim(), imageUrl = imageUrl.trim(),
                    sortOrder = sort.toIntOrNull() ?: throw IllegalArgumentException("Ordre no vàlid"))
                candidate.validate(); error = ""; onSave(candidate)
            } catch (ex: Exception) { error = ex.message ?: "Dades incorrectes" }
        }) { Text("Guardar") }
    }
}
