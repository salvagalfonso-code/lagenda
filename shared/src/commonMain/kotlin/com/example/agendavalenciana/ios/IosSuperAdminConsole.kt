package com.example.agendavalenciana.ios

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agendavalenciana.model.IosAdminPermissions
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Admin iOS - only installed under the SUPER_ADMIN profile branch.
 * Every privileged endpoint also checks the authenticated server-side role.
 * This UI contains no credentials or API bypasses.
 */
@Composable
internal fun IosSuperAdminConsole(api: IosAgendaApi, actorId: Int, onMessage: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var section by remember(actorId) { mutableStateOf("Resum") }
    var working by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf("") }
    var dashboard by remember { mutableStateOf<JsonObject?>(null) }
    var users by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var selectedUser by remember { mutableStateOf<JsonObject?>(null) }
    var userDetail by remember { mutableStateOf<JsonObject?>(null) }
    var pending by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var reports by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var period by remember { mutableIntStateOf(30) }

    fun execute(task: suspend () -> Unit) {
        if (working) return
        working = true
        failure = ""
        scope.launch {
            try { task() }
            catch (e: Exception) { failure = e.message ?: "No s'ha pogut completar l'operació." }
            finally { working = false }
        }
    }
    fun reload() = execute {
        when (section) {
            "Resum" -> dashboard = api.adminDashboard(period)
            "Usuaris" -> users = api.adminUsers()
            "Publicacions" -> { pending = api.adminCommunityPending(); reports = api.adminEventReports() }
        }
    }
    LaunchedEffect(section, period, actorId) {
        if (actorId <= 0) return@LaunchedEffect
        try {
            when (section) {
                "Resum" -> dashboard = api.adminDashboard(period)
                "Usuaris" -> users = api.adminUsers()
                "Publicacions" -> { pending = api.adminCommunityPending(); reports = api.adminEventReports() }
            }
        } catch (e: Exception) { failure = e.message ?: "No s'ha pogut carregar l'administració." }
    }

    HorizontalDivider()
    Text("Superadministració", fontWeight = FontWeight.Bold, fontSize = 20.sp)
    Text("Només el servidor pot autoritzar les operacions. La informació administrativa no es guarda en la caché local.", fontSize = 11.sp)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("Resum", "Usuaris", "Publicacions", "Campanyes").forEach { name ->
            FilterChip(selected = section == name, onClick = { section = name; selectedUser = null; userDetail = null }, label = { Text(name) })
        }
    }
    if (working) LinearProgressIndicator(Modifier.fillMaxWidth())
    if (failure.isNotEmpty()) Text(failure, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
    if (section != "Campanyes") TextButton(onClick = ::reload, enabled = !working) { Text("↻ Actualitzar") }

    when (section) {
        "Resum" -> {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(7, 30, 90).forEach { days -> FilterChip(period == days, onClick = { period = days }, label = { Text("$days dies") }) }
            }
            val overview = dashboard?.get("overview") as? JsonObject
            if (overview == null) Text("Carregant les dades administratives…")
            else {
                listOf(
                    "Usuaris actius" to overview.text("active_users"),
                    "Nous registres" to overview.text("users_new"),
                    "Esdeveniments futurs" to overview.text("events_future"),
                    "Publicacions pendents" to overview.text("events_pending"),
                    "Denúncies pendents" to overview.text("reports_pending"),
                    "Comandes del període" to overview.text("orders_period"),
                    "Entrades del període" to overview.text("tickets_period"),
                    "Vendes del període" to overview.text("sales_amount"),
                    "Persones connectades ara" to overview.text("connected_now")
                ).forEach { (title, value) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(title, fontSize = 13.sp)
                        Text(value.ifBlank { "—" }, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
                Text("Campanyes internes recents", fontWeight = FontWeight.Bold)
                dashboard?.items("notification_history")?.take(6)?.forEach { campaign ->
                    Text("${campaign.text("title")} · ${campaign.text("recipient_count")} destinataris · ${campaign.text("created_at")}", fontSize = 12.sp)
                }
                Text("Les xifres mostren només els recursos que el servidor pot consultar.", fontSize = 11.sp)
            }
        }
        "Usuaris" -> {
            var query by remember { mutableStateOf("") }
            var limit by remember { mutableIntStateOf(20) }
            OutlinedTextField(query, { query = it.take(80); limit = 20 }, label = { Text("Buscar usuari o correu") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            val matches = users.filter { u -> query.isBlank() || u.text("username").contains(query, true) || u.text("email").contains(query, true) }
            Text("${matches.size} coincidències", fontSize = 12.sp)
            matches.take(limit).forEach { u ->
                val id = u.text("id").toIntOrNull() ?: 0
                if (id > 0) {
                    OutlinedButton(onClick = {
                        selectedUser = u
                        execute { userDetail = api.adminUserDetail(id) }
                    }, modifier = Modifier.fillMaxWidth(), enabled = !working) {
                        Column(Modifier.fillMaxWidth()) {
                            Text(u.text("username"), fontWeight = FontWeight.Medium)
                            Text("${u.text("role")} · ${if (u.bool("active")) "Actiu" else "Desactivat"} · ${u.text("email")}", fontSize = 11.sp)
                        }
                    }
                }
            }
            if (limit < matches.size) TextButton(onClick = { limit += 20 }) { Text("Mostrar 20 més") }
            selectedUser?.let { selected ->
                val selectedId = selected.text("id").toIntOrNull() ?: 0
                val isSelf = selectedId == actorId
                HorizontalDivider()
                Text("Fitxa · ${selected.text("username")}", fontWeight = FontWeight.Bold)
                userDetail?.let { detail ->
                    val person = detail["user"] as? JsonObject
                    Text("${person?.text("username") ?: ""} · ${person?.text("role") ?: ""}", fontSize = 12.sp)
                    Text("Seguits: ${detail.items("follows").size} · Sessions: ${detail.items("sessions").size} · Comandes: ${detail.items("orders").size}", fontSize = 12.sp)
                    detail.items("activity").take(5).forEach { activity ->
                        Text("${activity.text("created_at")} · ${activity.text("action_type")}", fontSize = 11.sp)
                    }
                }
                if (isSelf) Text("No pots canviar el teu propi accés des d'ací.", fontSize = 12.sp)
                else if (selectedId > 0) AdminUserAccessEditor(
                    selected, working, onApply = { role, active -> execute {
                        api.adminSetUserAccess(selectedId, role, active)
                        users = api.adminUsers()
                        selectedUser = users.firstOrNull { it.text("id").toIntOrNull() == selectedId }
                        userDetail = api.adminUserDetail(selectedId)
                        onMessage("Accés del compte actualitzat")
                    } }
                )
                if (!isSelf && selectedId > 0) {
                    var deleteConfirmation by remember(selectedId) { mutableStateOf("") }
                    Text("Baixa administrativa i anonimització", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("És irreversible. El servidor preserva els registres fiscals i d'auditoria que cal conservar.", fontSize = 11.sp)
                    OutlinedTextField(deleteConfirmation, { deleteConfirmation = it.take(120) },
                        label = { Text("Escriu ELIMINAR ${selected.text("username")}") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedButton(enabled = !working && deleteConfirmation == "ELIMINAR ${selected.text("username")}",
                        onClick = {
                            deleteConfirmation = ""
                            execute {
                                api.adminDeleteUser(selectedId, selected.text("username"))
                                selectedUser = null; userDetail = null
                                users = api.adminUsers()
                                onMessage("Compte anonimitzat pel servidor")
                            }
                        }) { Text("Confirmar baixa administrativa") }
                }
            }
        }
        "Publicacions" -> {
            var eventQuery by remember { mutableStateOf("") }
            var eventPage by remember { mutableIntStateOf(1) }
            var foundEvents by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
            var eventPages by remember { mutableIntStateOf(1) }
            var moderateId by remember { mutableStateOf("") }
            Text("Consultar esdeveniments", fontWeight = FontWeight.Bold)
            OutlinedTextField(eventQuery, { eventQuery = it.take(180); eventPage = 1 },
                label = { Text("Títol, poble, artista o sala") }, modifier = Modifier.fillMaxWidth())
            OutlinedButton(enabled = !working, onClick = { execute {
                val response = api.adminFindEvents(eventQuery, eventPage)
                foundEvents = response.items("events")
                eventPages = response.text("pages").toIntOrNull() ?: 1
            } }) { Text("Buscar esdeveniments") }
            foundEvents.take(25).forEach { ev ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("#${ev.text("id")} · ${ev.text("title")} · ${ev.text("status")}",
                        fontSize = 12.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = { moderateId = ev.text("id") }) { Text("Revisar") }
                }
            }
            if (foundEvents.isNotEmpty() && eventPages > 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(enabled = !working && eventPage > 1, onClick = { execute {
                        val next = eventPage - 1; val response = api.adminFindEvents(eventQuery, next)
                        eventPage = next; foundEvents = response.items("events")
                    } }) { Text("Anterior") }
                    Text("$eventPage / $eventPages", fontSize = 12.sp)
                    TextButton(enabled = !working && eventPage < eventPages, onClick = { execute {
                        val next = eventPage + 1; val response = api.adminFindEvents(eventQuery, next)
                        eventPage = next; foundEvents = response.items("events")
                    } }) { Text("Següent") }
                }
            }
            Text("Publicacions enviades per la Comunitat", fontWeight = FontWeight.Bold)
            if (pending.isEmpty()) Text("No hi ha sol·licituds carregades.", fontSize = 12.sp)
            pending.take(25).forEach { row ->
                val eventId = row.text("event_id").toIntOrNull() ?: 0
                if (eventId > 0) AdminPendingSubmission(row, working) { decision, notes -> execute {
                    api.adminReviewCommunity(eventId, decision, notes)
                    pending = api.adminCommunityPending()
                    onMessage("Revisió de la publicació registrada")
                } }
            }
            HorizontalDivider()
            Text("Denúncies d'esdeveniments · consulta", fontWeight = FontWeight.Bold)
            Text("La resolució de les denúncies està gestionada pel circuit de revisió del servidor. Retirar una publicació no marca automàticament la denúncia com a resolta.", fontSize = 11.sp)
            reports.filter { it.text("status").uppercase() in listOf("PENDING", "REOPENED") }.take(20).forEach { report ->
                Text("#${report.text("report_id")} · ${report.text("event_title")} · ${report.text("reason")}", fontSize = 12.sp)
            }
            AdminEventModeration(working, suggestedEventId = moderateId) { eventId, action, note -> execute {
                api.adminModerateEvent(eventId, action, note)
                reports = api.adminEventReports()
                onMessage("Acció de moderació executada al servidor")
            } }
        }
        "Campanyes" -> IosAdminCampaignComposer(api, working, ::execute, onMessage)
    }
}

@Composable
private fun AdminUserAccessEditor(user: JsonObject, busy: Boolean, onApply: (String, Boolean) -> Unit) {
    val id = user.text("id")
    var role by remember(id, user.text("role")) { mutableStateOf(user.text("role")) }
    var active by remember(id, user.bool("active")) { mutableStateOf(user.bool("active")) }
    var confirmation by remember(id) { mutableStateOf("") }
    Text("Canviar rol i activació (no modifica el perfil)", fontWeight = FontWeight.Medium, fontSize = 12.sp)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        IosAdminPermissions.roles.forEach { type -> FilterChip(role == type, onClick = { role = type; confirmation = "" }, label = { Text(type, fontSize = 10.sp) }) }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(active, { active = it; confirmation = "" })
        Text("Compte actiu")
    }
    if (role == "SUPER_ADMIN") Text("Atenció: este rol permet administrar tots els comptes.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
    OutlinedTextField(confirmation, { confirmation = it.take(20) }, label = { Text("Escriu CANVIAR per confirmar") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    Button(enabled = !busy && confirmation == "CANVIAR" && IosAdminPermissions.validRole(role) && (role != user.text("role") || active != user.bool("active")), onClick = {
        onApply(role, active); confirmation = ""
    }) { Text("Aplicar canvi d'accés") }
    Text("Esta operació requerix instal·lar admin/user-access-ios.php al servidor.", fontSize = 11.sp)
}

@Composable
private fun AdminPendingSubmission(row: JsonObject, busy: Boolean, onDecide: (String, String) -> Unit) {
    val id = row.text("event_id")
    var note by remember(id) { mutableStateOf("") }
    var confirm by remember(id) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("#${id} · ${row.text("title")}", fontWeight = FontWeight.Medium, fontSize = 13.sp)
        Text("${row.text("town")} · ${row.text("username")} · ${row.text("publication_mode")}", fontSize = 11.sp)
        OutlinedTextField(note, { note = it.take(2000); confirm = false }, label = { Text("Motiu de la decisió") }, minLines = 2, modifier = Modifier.fillMaxWidth())
        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(confirm, { confirm = it }); Text("He revisat esta publicació", fontSize = 12.sp) }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(enabled = !busy && confirm, onClick = { onDecide("APPROVE", note); confirm = false }) { Text("Aprovar") }
            OutlinedButton(enabled = !busy && confirm && note.trim().length >= 12, onClick = { onDecide("REJECT", note); confirm = false }) { Text("Rebutjar") }
        }
        HorizontalDivider()
    }
}

@Composable
private fun AdminEventModeration(busy: Boolean, suggestedEventId: String, onAct: (Int, String, String) -> Unit) {
    var id by remember(suggestedEventId) { mutableStateOf(suggestedEventId) }
    var reason by remember { mutableStateOf("") }
    var action by remember { mutableStateOf("REMOVE_CONFIRMED") }
    var confirmation by remember { mutableStateOf("") }
    Text("Moderació directa d'un esdeveniment", fontWeight = FontWeight.Bold)
    OutlinedTextField(id, { id = it.filter(Char::isDigit).take(12); confirmation = "" }, label = { Text("ID de l'esdeveniment verificat") }, singleLine = true)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("REMOVE_CONFIRMED" to "Retirar", "RESTORE" to "Restaurar", "APPROVE" to "Aprovar").forEach { (code, label) ->
            FilterChip(action == code, onClick = { action = code; confirmation = "" }, label = { Text(label) })
        }
    }
    OutlinedTextField(reason, { reason = it.take(2000); confirmation = "" }, label = { Text("Justificació") }, minLines = 2, modifier = Modifier.fillMaxWidth())
    Text("Retirar una activitat pot generar advertències i suspensions del creador. No elimina les entrades venudes.", fontSize = 11.sp)
    OutlinedTextField(confirmation, { confirmation = it.take(20) }, label = { Text("Escriu MODERAR") }, singleLine = true)
    Button(enabled = !busy && confirmation == "MODERAR" && (id.toIntOrNull() ?: 0) > 0 && (action != "REMOVE_CONFIRMED" || IosAdminPermissions.canConfirmRemoval(id.toIntOrNull() ?: 0, reason)), onClick = {
        onAct(id.toInt(), action, reason); confirmation = ""
    }) { Text("Executar moderació") }
}

@Composable
private fun IosAdminCampaignComposer(api: IosAgendaApi, busy: Boolean,
                                     execute: (suspend () -> Unit) -> Unit, onMessage: (String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf("") }
    var activeDays by remember { mutableIntStateOf(0) }
    var allUsers by remember { mutableStateOf(true) }
    var followedArtist by remember { mutableStateOf("") }
    var savedEvent by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var confirmed by remember { mutableStateOf(false) }
    fun filter(): JsonObject = buildJsonObject {
        put("all_users", allUsers); put("role", role); put("platform", platform); put("active_days", activeDays)
        if (!allUsers) {
            if (followedArtist.trim().isNotEmpty()) {
                put("follow_type", "ARTIST"); put("follow_key", followedArtist.trim().take(190))
            }
            put("saved_event_id", savedEvent.toIntOrNull()?.coerceAtLeast(0) ?: 0)
        }
    }
    val signature = listOf(title, body, role, platform, activeDays.toString(), allUsers.toString(), followedArtist, savedEvent).joinToString("|")
    val canTarget = IosAdminPermissions.validTarget(allUsers, followedArtist, savedEvent.toIntOrNull() ?: 0)
    Text("Campanya a la bústia interna", fontWeight = FontWeight.Bold)
    Text("L'enviament és INTERN. Un recompte de destinataris no implica una notificació push APNs o FCM.", fontSize = 11.sp)
    OutlinedTextField(title, { title = it.take(160) }, label = { Text("Títol") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    OutlinedTextField(body, { body = it.take(1200) }, label = { Text("Missatge") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(allUsers, { allUsers = it }); Text("Tots els usuaris actius") }
    if (!allUsers) {
        OutlinedTextField(followedArtist, { followedArtist = it.take(190) }, label = { Text("Artista seguit") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(savedEvent, { savedEvent = it.filter(Char::isDigit).take(12) }, label = { Text("ID esdeveniment guardat") })
        Text("Si indiques tots dos criteris, el servidor exigix que es complisquen conjuntament.", fontSize = 11.sp)
    }
    Text("Plataforma", fontWeight = FontWeight.Medium, fontSize = 12.sp)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        listOf("" to "Totes", "ANDROID" to "Android", "IOS" to "iPhone", "WEB" to "Web").forEach { (code, label) ->
            FilterChip(platform == code, onClick = { platform = code }, label = { Text(label) })
        }
    }
    Text("Rol destinatari", fontWeight = FontWeight.Medium, fontSize = 12.sp)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        (listOf("") + IosAdminPermissions.roles).forEach { value ->
            FilterChip(role == value, onClick = { role = value }, label = { Text(value.ifBlank { "Tots" }, fontSize = 10.sp) })
        }
    }
    Text("Activitat recent", fontSize = 12.sp)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        listOf(0, 1, 7, 30, 90).forEach { n -> FilterChip(activeDays == n, onClick = { activeDays = n }, label = { Text(if (n == 0) "Qualsevol" else "$n dies") }) }
    }
    OutlinedButton(enabled = !busy && title.isNotBlank() && body.isNotBlank() && canTarget, onClick = {
        val frozenSignature = signature
        val frozenFilter = filter()
        execute {
            val result = api.adminPreviewTargets(frozenFilter)
            preview = frozenSignature to (result.text("count").toIntOrNull() ?: 0)
        }
    }) { Text("Previsualitzar destinataris") }
    if (preview?.first == signature) {
        Text("Destinataris: ${preview?.second ?: 0}", fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(confirmed, { confirmed = it }); Text("Confirme l'enviament a estos comptes") }
        Button(enabled = !busy && confirmed && (preview?.second ?: 0) > 0, onClick = {
            val frozenFilter = filter()
            val count = preview?.second ?: 0
            val frozenTitle = title
            val frozenBody = body
            preview = null; confirmed = false
            execute {
                val response = api.adminSendTargets(frozenTitle, frozenBody, count, frozenFilter)
                onMessage("Campanya interna enviada a ${response.text("recipient_count")} comptes")
            }
        }) { Text("Enviar campanya interna") }
    } else if (preview != null) {
        Text("Els filtres o el missatge han canviat. Torna a previsualitzar.", fontSize = 11.sp)
    }
}
