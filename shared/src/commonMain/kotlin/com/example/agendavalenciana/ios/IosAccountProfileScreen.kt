package com.example.agendavalenciana.ios

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.agendavalenciana.model.IosProfileParityPolicy

private val profilePanel = Color(0xFFF7F7F7)
private val profileCaption = Color(0xFF777777)

/** Android parity: opening the person icon without an account is not a bare login prompt. */
@Composable
internal fun IosGuestProfileScreen(
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    onSystemLanguageSettings: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TextButton(onClick = onBack) { Text("← Tornar") }
        Text("Usuari", fontWeight = FontWeight.Black, fontSize = 28.sp)
        Surface(shape = RoundedCornerShape(22.dp), color = Color(0xFFF0F0F0), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Estàs utilitzant l’Agenda com a convidat", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Pots buscar, guardar, seguir i personalitzar Per a tu sense iniciar sessió. " +
                    "Les preferències es conserven en este dispositiu; la sincronització entre dispositius necessita un compte.",
                    fontSize = 14.sp, color = Color.DarkGray)
            }
        }
        IosAppLanguageSetting(onOpen = onSystemLanguageSettings)
        Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) { Text("Iniciar sessió") }
        OutlinedButton(onClick = onRegister, modifier = Modifier.fillMaxWidth()) { Text("Crear compte") }
        Text("Inicia sessió per comprar entrades o recuperar les dades en un altre dispositiu. " +
            "Les preferències locals del convidat no s'esborraran en obrir el formulari.",
            fontSize = 12.sp, color = profileCaption)
    }
}

@Composable
private fun IosAppLanguageSetting(onOpen: () -> Unit) {
    Text("Configuració", fontWeight = FontWeight.Black, fontSize = 18.sp)
    Surface(shape = RoundedCornerShape(18.dp), color = profilePanel, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Idioma de l’app", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            OutlinedButton(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
                Text("Configuració d’idioma de l’iPhone")
            }
            Text("L'idioma del sistema es gestiona en Configuració. La traducció completa " +
                "de la interfície iOS està pendent.", fontSize = 11.sp, color = profileCaption)
        }
    }
}

/** The same account profile layout and editable fields as Android.
 * Never treat local role selection as authorization; only the API can confirm a role change.
 */
@Composable
internal fun IosAccountProfileScreen(
    account: IosAccount,
    profile: IosMyProfile?,
    settings: IosProfileSettings?,
    saving: Boolean,
    onBack: () -> Unit,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onSave: (username: String, email: String, name: String, bio: String, role: String, phone: String) -> Unit,
    onSendEmailVerification: () -> Unit,
    onRefreshEmailVerification: () -> Unit,
    onOpenPersonalization: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenLanguageSettings: () -> Unit,
    onDeleteAccount: () -> Unit,
    onLogout: () -> Unit,
    api: IosAgendaApi,
    onMessage: (String) -> Unit
) {
    val isAdmin = IosProfileParityPolicy.superAdmin(account.role)
    var name by remember(account.id, profile?.displayName) {
        mutableStateOf(profile?.displayName?.ifBlank { account.username } ?: account.username)
    }
    var username by remember(account.id, account.username) { mutableStateOf(account.username) }
    var email by remember(account.id, account.email) { mutableStateOf(account.email) }
    var bio by remember(account.id, profile?.bio) { mutableStateOf(profile?.bio.orEmpty()) }
    var professional by remember(account.id, settings?.role) {
        mutableStateOf(IosProfileParityPolicy.professional(settings?.role ?: account.role))
    }
    var phone by remember(account.id, settings?.phone) { mutableStateOf(settings?.phone.orEmpty()) }
    var showAdmin by remember(account.id) { mutableStateOf(false) }
    var confirmLogout by remember(account.id) { mutableStateOf(false) }
    val canSave = !saving && IosProfileParityPolicy.validName(name) &&
        IosProfileParityPolicy.validUsername(username) && IosProfileParityPolicy.validEmail(email) &&
        (isAdmin || !professional || settings == null || IosProfileParityPolicy.validProfessionalPhone(phone))

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("← Tornar") }
        Text("Perfil", fontWeight = FontWeight.Black, fontSize = 28.sp)
        Text("Pots modificar les dades directament ací i guardar-les en acabar.", fontSize = 12.sp, color = profileCaption)
        Surface(shape = RoundedCornerShape(22.dp), color = Color(0xFFF0F0F0), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
                    Surface(shape = CircleShape, color = Color(0xFFE8DFF2), modifier = Modifier.fillMaxSize()) {
                        Box(contentAlignment = Alignment.Center) {
                            if (!profile?.photoUrl.isNullOrBlank()) {
                                AsyncImage(model = profile?.photoUrl, contentDescription = "Fotografia del perfil",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                            } else {
                                Text((name.ifBlank { username }).take(2).uppercase(), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                TextButton(onClick = onPickPhoto, enabled = !saving) { Text("+ Canviar fotografia") }
                if (!profile?.photoUrl.isNullOrBlank()) {
                    TextButton(onClick = onRemovePhoto, enabled = !saving) { Text("Llevar foto") }
                }
                Text(name.ifBlank { username }, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text("@$username", color = profileCaption)
            }
        }
        OutlinedTextField(name, { name = it.take(190) }, modifier = Modifier.fillMaxWidth(),
            label = { Text("Nom i cognoms") }, singleLine = true)
        OutlinedTextField(username, { username = it.trimStart().take(80) }, modifier = Modifier.fillMaxWidth(),
            label = { Text("Nom d'usuari") }, singleLine = true)
        OutlinedTextField(email, { email = it.trim().take(190) }, modifier = Modifier.fillMaxWidth(),
            label = { Text("Correu electrònic") }, singleLine = true)
        OutlinedTextField(bio, { bio = it.take(3000) }, modifier = Modifier.fillMaxWidth(),
            label = { Text("Biografia (opcional)") }, minLines = 2, maxLines = 4)
        Text("Rol", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        if (isAdmin) {
            FilterChip(selected = true, onClick = {}, enabled = false, label = { Text("Superadmin") })
            OutlinedButton(onClick = { showAdmin = !showAdmin }, modifier = Modifier.fillMaxWidth()) {
                Text("Crear notificació · Superadmin / Administració")
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !professional, onClick = { professional = false }, enabled = settings != null, label = { Text("Públic") })
                FilterChip(selected = professional, onClick = { professional = true }, enabled = settings != null, label = { Text("Professional") })
            }
            if (professional) OutlinedTextField(phone, { phone = it.take(24) }, modifier = Modifier.fillMaxWidth(),
                label = { Text("Telèfon professional (obligatori)") }, singleLine = true)
            if (settings == null) Text("No s'han carregat encara els permisos professionals. No es canviarà el rol fins a confirmar-los amb el servidor.",
                fontSize = 11.sp, color = profileCaption)
        }
        Button(onClick = {
            onSave(username.trim(), email.trim(), name.trim(), bio, if (professional) "PROFESSIONAL" else "PUBLIC", phone.trim())
        }, enabled = canSave, modifier = Modifier.fillMaxWidth()) {
            Text(if (saving) "Guardant…" else "Guardar")
        }
        IosAppLanguageSetting(onOpen = onOpenLanguageSettings)
        if (account.email.isBlank()) {
            Text("Afig un correu electrònic per poder-lo verificar.", fontSize = 12.sp)
        } else if (settings == null) {
            Text("Verificació del correu pendent de consultar al servidor.", fontSize = 12.sp)
            TextButton(onClick = onRefreshEmailVerification) { Text("Comprovar estat del correu") }
        } else if (!settings.emailVerified) {
            Text("Correu sense confirmar", color = Color(0xFFB43232), fontSize = 12.sp)
            OutlinedButton(enabled = !saving, onClick = onSendEmailVerification) { Text("Enviar correu de confirmació") }
            TextButton(onClick = onRefreshEmailVerification) { Text("Ja l'he confirmat · Comprovar") }
        } else {
            Text("Correu confirmat", color = Color(0xFF2F6B45), fontSize = 12.sp)
        }
        HorizontalDivider()
        Text("Altres opcions", fontSize = 18.sp, fontWeight = FontWeight.Black)
        OutlinedButton(onClick = onOpenPersonalization, modifier = Modifier.fillMaxWidth()) { Text("Personalització") }
        OutlinedButton(onClick = onOpenNotificationSettings, modifier = Modifier.fillMaxWidth()) { Text("Configurar notificacions d'iPhone") }
        OutlinedButton(onClick = onDeleteAccount, modifier = Modifier.fillMaxWidth()) { Text("Eliminar el meu compte") }
        OutlinedButton(onClick = { confirmLogout = true }, modifier = Modifier.fillMaxWidth()) { Text("Tancar sessió") }
        if (isAdmin && showAdmin) IosSuperAdminConsole(api, account.id, onMessage)
    }
    if (confirmLogout) AlertDialog(
        onDismissRequest = { confirmLogout = false },
        title = { Text("Tancar sessió?") },
        text = { Text("La sessió es tancarà en este iPhone.") },
        confirmButton = { TextButton(onClick = { confirmLogout = false; onLogout() }) { Text("Tancar sessió") } },
        dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Cancel·lar") } }
    )
}
