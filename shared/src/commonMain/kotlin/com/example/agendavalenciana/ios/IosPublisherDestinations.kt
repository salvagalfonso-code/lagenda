package com.example.agendavalenciana.ios

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/** Temporary truthful destination until its publisher workflow is migrated. Never imply a submission was made. */
@Composable
internal fun IosPublisherPendingDestination(action: IosPublishAction, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(action.title) },
        text = { Text("El menú ja reproduïx les opcions i permisos d’Android. La pantalla de gestió «${action.title}» encara necessita la seua implementació funcional en iOS; no s’ha enviat ni modificat cap dada.") },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tancar") } })
}

/** The Android professional access request, using the same server endpoint and explicit human review. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun IosProfessionalRequestSheet(
    api: IosAgendaApi,
    artists: List<String>,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var kind by remember { mutableStateOf("ARTIST") }
    var entityName by remember { mutableStateOf("") }
    var relation by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var comments by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf("") }
    var success by remember { mutableStateOf(false) }
    var dropOpen by remember { mutableStateOf(false) }
    val matches = remember(entityName, artists) {
        if (entityName.length < 2) emptyList()
        else artists.filter { it.contains(entityName,ignoreCase = true) }.take(8)
    }
    ModalBottomSheet(onDismissRequest = { if (!busy) onDismiss() }, containerColor = Color.White) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Soc organitzador o artista", fontSize = 23.sp, fontWeight = FontWeight.Black)
            Text("La verificació és manual: et contactarem nosaltres.", color = Color.Gray, fontSize = 13.sp)
            Text("Quin tipus de perfil representes?", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = kind == "ARTIST", onClick = { kind = "ARTIST"; entityName = "" }, label = { Text("Artista / grup") })
                FilterChip(selected = kind == "ORGANIZER", onClick = { kind = "ORGANIZER"; entityName = "" }, label = { Text("Organitzador") })
            }
            if (kind == "ARTIST") {
                Text("Busca i selecciona un artista o grup existent a l’Agenda.", fontSize = 12.sp)
                OutlinedTextField(entityName, { entityName = it.take(190); success = false },
                    label = { Text("Artista o grup *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                matches.forEach { name ->
                    TextButton(onClick = { entityName = name }) { Text("Seleccionar: $name") }
                }
            } else {
                OutlinedTextField(entityName, { entityName = it.take(190); success = false },
                    label = { Text("Nom de l’organització *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
            OutlinedTextField(relation, { relation = it.take(255) }, label = { Text("Quina relació tens? *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(contact, { contact = it.take(200) }, label = { Text("Correu o telèfon de contacte *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(comments, { comments = it.take(2000) }, label = { Text("Informació addicional") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            Text("El perfil no s’assigna automàticament. L’administració revisa cada sol·licitud.", fontSize = 12.sp, color = Color.Gray)
            if (result.isNotBlank()) Text(result, fontSize = 12.sp,
                color = if (success) Color(0xFF248047) else MaterialTheme.colorScheme.error)
            Button(onClick = {
                busy = true; result = ""
                scope.launch {
                    try {
                        result = api.requestProfessionalClaim(entityName, kind, relation, contact, comments)
                        success = true
                    } catch (e: Exception) { result = e.message ?: "No s’ha pogut enviar la sol·licitud."; success = false }
                    finally { busy = false }
                }
            }, enabled = !busy && !success && entityName.isNotBlank() && relation.isNotBlank() && contact.isNotBlank() &&
                (kind != "ARTIST" || artists.any { it.equals(entityName.trim(),true) }),
                modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Enviant…" else if (kind == "ARTIST") "Reclamar grup" else "Enviar sol·licitud") }
            Spacer(Modifier.height(5.dp))
        }
    }
}
