package com.example.agendavalenciana.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.agendavalenciana.data.ServerApiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Es basa en l'API real del panell superadmin. El backend NO disposa encara de push FCM. */
@Composable
internal fun AdminNotificationsComposer(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var androidOnly by remember { mutableStateOf(true) }
    var previewCount by remember { mutableStateOf<Int?>(null) }
    var sending by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf("") }
    fun preview() {
        sending = true
        result = ""
        scope.launch {
            val res = withContext(Dispatchers.IO) {
                ServerApiRepository.adminNotificationPreview(context, androidOnly)
            }
            sending = false
            res.onSuccess { previewCount = it }
                .onFailure { result = it.message ?: "No s'han pogut consultar els destinataris." }
        }
    }
    AlertDialog(
        onDismissRequest = { if (!sending) onDismiss() },
        title = { Text("Notificació · Superadmin") },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("L'enviament disponible actualment és a la bústia interna de L’Agenda. " +
                    "El push Android fora de l'app requerix integrar FCM al servidor i registrar els dispositius.")
                OutlinedTextField(title, onValueChange = { title = it.take(160); previewCount = null },
                    label = { Text("Títol") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(body, onValueChange = { body = it.take(1200); previewCount = null },
                    label = { Text("Missatge") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = androidOnly, onCheckedChange = { androidOnly = it; previewCount = null })
                    Text("Només comptes amb sessions Android")
                }
                Text("La selecció per Android es basa en la sessió del compte, no en un token de push.")
                Button(onClick = ::preview, enabled = !sending && title.isNotBlank() && body.isNotBlank()) {
                    Text(if (sending) "Consultant…" else "Previsualitzar destinataris")
                }
                previewCount?.let { count ->
                    Text("Destinataris: $count. Confirma l'enviament intern només després de revisar el contingut.")
                }
                if (result.isNotBlank()) Text(result)
            }
        },
        confirmButton = {
            Button(
                enabled = previewCount != null && previewCount!! > 0 && !sending,
                onClick = {
                    sending = true
                    val expected = previewCount ?: return@Button
                    scope.launch {
                        val response = withContext(Dispatchers.IO) {
                            ServerApiRepository.adminNotificationSend(context, title, body, androidOnly, expected)
                        }
                        sending = false
                        response.onSuccess { root ->
                            result = "Campanya creada per a ${root.optInt("recipient_count", 0)} comptes. " +
                                "Avís intern guardat; push Android no disponible."
                            previewCount = null
                        }.onFailure { err -> result = err.message ?: "No s'ha pogut enviar."; previewCount = null }
                    }
                }
            ) { Text("Enviar a la bústia interna") }
        },
        dismissButton = { TextButton(enabled = !sending, onClick = onDismiss) { Text("Tancar") } }
    )
}
