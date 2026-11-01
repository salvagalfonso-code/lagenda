package com.example.agendavalenciana.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agendavalenciana.state.AgendaState

@Composable
internal fun TicketSellerAccess(
    state: AgendaState,
    onBack: () -> Unit,
    onVerified: (Int) -> Unit
) {
    val existing = state.currentAccount
    val loggedAsPublisher = state.isAuthorizedPublisher()
    var username by remember(existing?.username) { mutableStateOf(existing?.username.orEmpty()) }
    var password by remember(existing?.username) { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var target by remember { mutableStateOf<Rect?>(null) }
    var buttonRect by remember { mutableStateOf<Rect?>(null) }
    var helpIndex by remember { mutableStateOf(0) }
    var showHelp by remember { mutableStateOf(true) }
    val steps = listOf(
        TicketHelp("Accés segur per vendre entrades", "Abans de gestionar preus, aforaments o publicacions, cal identificar un compte professional. La venda no es modifica en este pas."),
        TicketHelp("Usuari i contrasenya", "Introduïx les credencials del compte autoritzat. La contrasenya només s'envia al servidor per verificar-la i no es guarda en el tutorial."),
        TicketHelp("Verificar accés", "En tocar este botó comprovarem els permisos i, si són correctes, podràs seleccionar el festival.")
    )
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)) {
            TextButton(onClick = onBack) { Text("← Tornar") }
            Text("Vendre entrades", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Pas 1 · Identificació", fontWeight = FontWeight.Bold)
            Text("Accés protegit per a organitzadors i equips autoritzats.")
            Column(Modifier.onGloballyPositioned { target = it.boundsInRoot() },
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(username, { username = it; message = "" }, label = { Text("Usuari o correu") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !loggedAsPublisher)
                OutlinedTextField(password, { password = it; message = "" },
                    label = { Text("Contrasenya") }, visualTransformation = PasswordVisualTransformation(),
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                if (loggedAsPublisher) Text("Comprovarem el compte amb què ja has iniciat sessió: ${existing?.username.orEmpty()}.", fontSize = 12.sp)
            }
            if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.error)
            Button(onClick = {
                busy = true
                if (loggedAsPublisher) {
                    state.verifyTicketManagerPassword(password) { success, description ->
                        password = ""; busy = false; message = description
                        if (success) {
                            state.currentAccount?.let { onVerified(it.serverUserId ?: it.id) }
                        }
                    }
                } else {
                    state.login(username, password) { success ->
                        password = ""; busy = false
                        if (success && state.isAuthorizedPublisher()) {
                            state.currentAccount?.let { onVerified(it.serverUserId ?: it.id) }
                        } else message = if (success) "Este compte no pot administrar entrades." else state.loginError ?: "Credencials incorrectes."
                    }
                }
            }, enabled = !busy && username.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth().onGloballyPositioned { buttonRect = it.boundsInRoot() }) {
                Text(if (busy) "Verificant…" else "Verificar i continuar")
            }
            Text("Si tens un compte de Google sense contrasenya, crea'n una amb la recuperació de contrasenya abans de gestionar les entrades.",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { helpIndex = 0; showHelp = true }) { Text("? Repetir explicació") }
        }
        if (showHelp) TicketCoachMark(
            targetInRoot = if (helpIndex == 2) buttonRect else target,
            help = steps[helpIndex], index = helpIndex, total = steps.size,
            onNext = { if (helpIndex == steps.lastIndex) showHelp = false else helpIndex++ },
            onSkip = { showHelp = false }
        )
    }
}
