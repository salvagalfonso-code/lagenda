package com.example.agendavalenciana.ios

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch

private val communityKinds = listOf(
    "CAR" to "Cotxe compartit", "TRANSPORT" to "Transport organitzat",
    "MEETUP" to "Quedades", "HOSTING" to "Allotjament compartit", "OTHER" to "Altres"
)
private fun kindName(id: String) = communityKinds.firstOrNull { it.first == id }?.second ?: "Altres"

@Composable
internal fun IosCommunityHub(groups: List<IosCommunityGroup>, api: IosAgendaApi,
                              isAdmin: Boolean, authenticated: Boolean, reload: (Boolean) -> Unit, onOpen: (IosCommunityGroup) -> Unit) {
    var mine by remember { mutableStateOf(false) }
    var selectedKind by remember { mutableStateOf("") }
    var blockedOpen by remember { mutableStateOf(false) }
    var adminOpen by remember { mutableStateOf(false) }
    val filtered = groups.filter { group ->
        (selectedKind.isEmpty() || group.type == selectedKind) &&
            (!mine || group.membership == "ACCEPTED" || group.creatorUserId == api.account?.id)
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Comunitat", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = { reload(mine) }) { Text("Actualitzar") }
        }
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            FilterChip(selected = !mine, onClick = { mine = false; reload(false) }, label = { Text("Tots") })
            Spacer(Modifier.width(8.dp))
            if (authenticated) FilterChip(selected = mine, onClick = { mine = true; reload(true) }, label = { Text("Els meus") })
            Spacer(Modifier.weight(1f))
            if (authenticated) TextButton(onClick = { blockedOpen = true }) { Text("Bloquejats") }
            if (isAdmin) TextButton(onClick = { adminOpen = true }) { Text("Moderar") }
        }
        androidx.compose.foundation.lazy.LazyRow(contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item { FilterChip(selected = selectedKind.isEmpty(), onClick = {selectedKind = ""}, label = {Text("Tots")}) }
            items(communityKinds) { item ->
                FilterChip(selected = selectedKind == item.first, onClick = { selectedKind=item.first },
                    label = {Text(item.second, fontSize = 11.sp)})
            }
        }
        if (filtered.isEmpty()) Text("No hi ha taulons en esta selecció.",Modifier.padding(18.dp))
        else LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = {it.id}) { g ->
                ElevatedCard(Modifier.fillMaxWidth().clickable { onOpen(g) }) {
                    Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(g.title.ifBlank { "Tauló #${g.id}" }, fontWeight=FontWeight.Bold)
                        Text("${kindName(g.type)} · ${if(g.access=="OPEN") "Obert" else "Amb aprovació"} · ${g.status}", fontSize=11.sp, color=Color.Gray)
                        if (g.description.isNotEmpty()) Text(g.description, maxLines=2, fontSize=13.sp)
                        if (g.origin.isNotEmpty()) Text("Origen: ${g.origin}", fontSize=11.sp)
                        if (g.meeting.isNotEmpty()) Text("Trobada: ${g.meeting}", fontSize=11.sp)
                        Text("Participants: ${g.participants}",fontSize=11.sp)
                    }
                }
            }
        }
    }
    if (authenticated && blockedOpen) IosBlockedPeopleDialog(api) { blockedOpen = false; reload(mine) }
    if (adminOpen) IosAdminReportsDialog(api) { adminOpen = false; reload(mine) }
}

/** Public preview has no access to membership, messages, creator identity or private details. */
@Composable
internal fun IosGuestCommunityPreview(group: IosCommunityGroup, onDismiss: () -> Unit, onLogin: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(group.title) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Tauló cultural · ${kindName(group.type)}", fontSize = 13.sp)
            Text(if (group.access == "OPEN") "Grup obert" else "Requerix aprovació", fontSize = 13.sp)
            Text("Per consultar missatges, participar o escriure en el tauló, inicia sessió.", fontSize = 12.sp)
        } },
        confirmButton = { TextButton(onClick = onLogin) { Text("Iniciar sessió per participar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Tancar") } })
}

@Composable
internal fun IosCommunityChatDialog(initial: IosCommunityGroup, api: IosAgendaApi, currentUserId: Int,
                                     onDismiss: () -> Unit, onChanged: () -> Unit) {
    val scope = rememberCoroutineScope()
    var group by remember(initial.id) { mutableStateOf(initial) }
    val creator = IosCommunityPolicy.isCreator(group.creatorUserId,currentUserId)
    var membership by remember(initial.id) { mutableStateOf(initial.membership) }
    var messages by remember(initial.id) { mutableStateOf(emptyList<IosMessage>()) }
    var blocked by remember(initial.id) { mutableStateOf(emptySet<Int>()) }
    var notice by remember(initial.id) { mutableStateOf("") }
    var draft by remember(initial.id) { mutableStateOf("") }
    var working by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var requestsOpen by remember { mutableStateOf(false) }
    var managing by remember { mutableStateOf(false) }
    var reportMessage by remember { mutableStateOf<IosMessage?>(null) }
    var reportGroup by remember { mutableStateOf(false) }
    var blockPerson by remember { mutableStateOf<IosMessage?>(null) }
    var deleteMessage by remember { mutableStateOf<IosMessage?>(null) }
    var banPerson by remember { mutableStateOf<IosMessage?>(null) }
    var closeConfirm by remember { mutableStateOf(false) }
    var leaveConfirm by remember { mutableStateOf(false) }
    fun launchAction(action: suspend () -> Unit) {
        if (working) return
        working = true
        scope.launch {
            try { action(); notice = "Canvis guardats." }
            catch (e: Exception) { notice = e.message ?: "No s'ha pogut completar l'operació." }
            finally { working = false }
        }
    }
    LaunchedEffect(initial.id) {
        runCatching { api.blockedCommunityUsers() }.onSuccess { blocked=it.map { p -> p.id }.toSet() }
        if (membership == "ACCEPTED") runCatching { api.messages(group.id) }
            .onSuccess { messages = it }.onFailure { notice = it.message ?: "No s'han pogut consultar els missatges." }
    }
    Dialog(onDismissRequest=onDismiss) {
        Surface(shape=RoundedCornerShape(20.dp),color=Color.White,
            modifier=Modifier.fillMaxWidth().fillMaxHeight(.93f)) {
            Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text(group.title,fontWeight=FontWeight.Bold,fontSize=17.sp,modifier=Modifier.weight(1f))
                    TextButton(onClick=onDismiss) { Text("✕") }
                }
                Text("${kindName(group.type)} · ${group.status} · ${group.participants} participants",fontSize=11.sp,color=Color.Gray)
                if(group.description.isNotBlank()) Text(group.description,fontSize=12.sp)
                if(group.origin.isNotBlank()) Text("Origen: ${group.origin}",fontSize=11.sp)
                if(group.meeting.isNotBlank()) Text("Trobada: ${group.meeting}",fontSize=11.sp)
                Row(Modifier.horizontalScroll(rememberScrollState()),verticalAlignment=Alignment.CenterVertically) {
                    TextButton(onClick={launchAction { messages = api.messages(group.id) }},enabled=membership=="ACCEPTED" && !working) {Text("Actualitzar")}
                    TextButton(onClick={reportGroup=true}) {Text("Denunciar tauló")}
                    if(creator) {
                        TextButton(onClick={editing=true}){Text("Editar")}
                        TextButton(onClick={requestsOpen=true}){Text("Sol·licituds")}
                        TextButton(onClick={managing=true}){Text("Participants")}
                        TextButton(onClick={closeConfirm=true}){Text(if(group.status=="CLOSED") "Reobrir" else "Tancar")}
                    } else if(membership=="ACCEPTED") TextButton(onClick={leaveConfirm=true}){Text("Abandonar")}
                }
                if(membership!="ACCEPTED") {
                    Button(onClick={launchAction {
                        membership=api.joinGroup(group.id)
                        if(membership=="ACCEPTED") messages=api.messages(group.id)
                        else notice="Sol·licitud pendent d'aprovació."
                        onChanged()
                    }},enabled=!working && IosCommunityPolicy.canRequestAccess(group.status,membership)) {
                        Text(if(membership=="PENDING") "Accés pendent" else "Sol·licitar accés")
                    }
                }
                if (notice.isNotBlank()) Text(notice,color=Color.DarkGray,fontSize=11.sp)
                LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                    items(messages,key={it.id}) { m ->
                        if(IosCommunityPolicy.displayMessage(m.userId,blocked)) Column(Modifier.fillMaxWidth().background(Color(0xFFF7F6F9),RoundedCornerShape(10.dp)).padding(9.dp)) {
                            Text(m.author,fontWeight=FontWeight.Bold,fontSize=12.sp)
                            Text(m.body,fontSize=13.sp)
                            Row(Modifier.horizontalScroll(rememberScrollState())) {
                                TextButton(onClick={reportMessage=m}) {Text("Denunciar",fontSize=11.sp)}
                                if(m.userId>0 && m.userId!=currentUserId) {
                                    TextButton(onClick={blockPerson=m}){Text("Bloquejar",fontSize=11.sp)}
                                    if(IosCommunityPolicy.canModerateMember(group.creatorUserId,currentUserId,m.userId)) TextButton(onClick={banPerson=m}){Text("Expulsar",fontSize=11.sp)}
                                }
                                if(creator) TextButton(onClick={deleteMessage=m}){Text("Retirar",fontSize=11.sp)}
                            }
                        }
                    }
                }
                if(IosCommunityPolicy.canSend(group.status,membership)) Row(verticalAlignment=Alignment.CenterVertically) {
                    OutlinedTextField(value=draft,onValueChange={draft=it.take(2000)},modifier=Modifier.weight(1f),
                        label={Text("Missatge")},maxLines=3)
                    TextButton(enabled=draft.isNotBlank() && !working,onClick={launchAction {
                        api.sendMessage(group.id,draft);draft="";messages=api.messages(group.id)
                    }}){Text("Enviar")}
                }
            }
        }
    }
    if(editing) IosCommunityEditDialog(group,api,onDismiss={editing=false},onSaved={
        group=it;editing=false;onChanged()
    },onError={notice=it})
    if(requestsOpen) IosCommunityRequestsDialog(group.id,api,onDismiss={requestsOpen=false},onChanged=onChanged)
    if(managing) IosCommunityManageDialog(group.id,api,onDismiss={managing=false},onChanged={onChanged();launchAction {messages=api.messages(group.id)}})
    if(reportMessage!=null || reportGroup) IosCommunityReportDialog(
        title=if(reportMessage!=null) "Denunciar missatge" else "Denunciar tauló",
        onDismiss={reportMessage=null;reportGroup=false},onSend={reason -> launchAction {
            val m=reportMessage
            if(m!=null) api.reportCommunityMessage(group.id,m.id.toIntOrNull()?:0,reason)
            else api.reportCommunityGroup(group.id,reason)
            reportMessage=null;reportGroup=false
        }})
    blockPerson?.let { m -> IosCommunityConfirmDialog("Bloquejar ${m.author}?",
        "Deixaràs de veure els seus missatges. Podràs desfer-ho des de Comunitat > Bloquejats.",
        "Bloquejar",onDismiss={blockPerson=null},onConfirm={launchAction {
            api.setCommunityBlocked(m.userId,true); blocked=blocked+m.userId;blockPerson=null;messages=api.messages(group.id)
        }}) }
    deleteMessage?.let { m -> IosCommunityConfirmDialog("Retirar el missatge?",m.body.take(100),"Retirar",
        onDismiss={deleteMessage=null},onConfirm={launchAction {
            api.moderateCommunityGroup(group.id,"DELETE_MESSAGE",messageId=m.id.toIntOrNull()?:0)
            deleteMessage=null;messages=api.messages(group.id)
        }}) }
    banPerson?.let { m -> IosCommunityConfirmDialog("Expulsar ${m.author}?",
        "No podrà tornar a entrar fins que el creador retire el bloqueig del tauló.","Expulsar",
        onDismiss={banPerson=null},onConfirm={launchAction {
            api.moderateCommunityGroup(group.id,"BAN_MEMBER",userId=m.userId)
            banPerson=null;messages=api.messages(group.id);onChanged()
        }}) }
    if(closeConfirm) IosCommunityConfirmDialog(
        if(group.status=="CLOSED") "Reobrir tauló?" else "Tancar tauló?",
        "Els canvis seran visibles als participants.",if(group.status=="CLOSED") "Reobrir" else "Tancar",
        onDismiss={closeConfirm=false},onConfirm={launchAction {
            val newStatus=if(group.status=="CLOSED") "OPEN" else "CLOSED"
            api.setCommunityStatus(group.id,newStatus);group=group.copy(status=newStatus)
            closeConfirm=false;onChanged()
        }})
    if(leaveConfirm) IosCommunityConfirmDialog("Abandonar tauló?","No rebràs més missatges del grup.","Abandonar",
        onDismiss={leaveConfirm=false},onConfirm={launchAction {
            api.leaveGroup(group.id);leaveConfirm=false;onChanged();onDismiss()
        }})
}

@Composable
private fun IosCommunityConfirmDialog(title:String,body:String,action:String,onDismiss:()->Unit,onConfirm:()->Unit) {
    AlertDialog(onDismissRequest=onDismiss,title={Text(title)},text={Text(body)},
        confirmButton={TextButton(onClick=onConfirm){Text(action)}},
        dismissButton={TextButton(onClick=onDismiss){Text("Cancel·lar")}})
}

@Composable
private fun IosCommunityReportDialog(title:String,onDismiss:()->Unit,onSend:(String)->Unit) {
    var reason by remember {mutableStateOf("")}
    AlertDialog(onDismissRequest=onDismiss,title={Text(title)},
        text={Column { Text("Selecciona o explica el motiu.",fontSize=12.sp)
            OutlinedTextField(reason,{reason=it.take(500)},label={Text("Motiu")},minLines=2)
        }}, confirmButton={TextButton(enabled=reason.isNotBlank(),onClick={onSend(reason)}){Text("Enviar denúncia")}},
        dismissButton={TextButton(onClick=onDismiss){Text("Cancel·lar")}})
}

@Composable
private fun IosCommunityEditDialog(g:IosCommunityGroup,api:IosAgendaApi,onDismiss:()->Unit,
                                   onSaved:(IosCommunityGroup)->Unit,onError:(String)->Unit) {
    val scope=rememberCoroutineScope()
    var title by remember(g.id){mutableStateOf(g.title)}
    var description by remember(g.id){mutableStateOf(g.description)}
    var origin by remember(g.id){mutableStateOf(g.origin)}
    var meeting by remember(g.id){mutableStateOf(g.meeting)}
    var capacity by remember(g.id){mutableStateOf(if(g.capacity>0)g.capacity.toString() else "")}
    var open by remember(g.id){mutableStateOf(g.access=="OPEN")}
    var kind by remember(g.id){mutableStateOf(g.type)}
    var working by remember {mutableStateOf(false)}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Editar tauló")},
        text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(title,{title=it.take(160)},label={Text("Nom")})
            OutlinedTextField(description,{description=it.take(1000)},label={Text("Descripció")})
            OutlinedTextField(origin,{origin=it.take(190)},label={Text("Origen o lloc")})
            OutlinedTextField(meeting,{meeting=it.take(19)},label={Text("Trobada AAAA-MM-DD HH:MM")})
            OutlinedTextField(capacity,{capacity=it.filter(Char::isDigit).take(3)},label={Text("Places disponibles")})
            Row(verticalAlignment=Alignment.CenterVertically){Checkbox(open,{open=it});Text("Obert sense aprovació")}
            communityKinds.forEach { (key,label)-> Row(Modifier.fillMaxWidth().clickable {kind=key},verticalAlignment=Alignment.CenterVertically) {
                RadioButton(selected=kind==key,onClick={kind=key});Text(label,fontSize=12.sp)
            } }
        }},confirmButton={TextButton(enabled=title.isNotBlank() && !working,onClick={
            working=true;scope.launch {try {onSaved(api.updateCommunityGroup(g,title,description,origin,meeting,
                capacity.toIntOrNull()?:0,kind,if(open)"OPEN" else "APPROVAL"))}
            catch(e:Exception){onError(e.message?:"No s'ha pogut editar")}
            finally{working=false} }
        }) {Text("Guardar")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel·lar")}})
}

@Composable
private fun IosCommunityRequestsDialog(groupId:Int,api:IosAgendaApi,onDismiss:()->Unit,onChanged:()->Unit) {
    val scope=rememberCoroutineScope()
    var members by remember(groupId){mutableStateOf(emptyList<IosMemberRequest>())}
    var error by remember {mutableStateOf("")}
    LaunchedEffect(groupId){runCatching {api.pendingRequests(groupId)}.onSuccess {members=it}.onFailure {error=it.message.orEmpty()}}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Sol·licituds d'accés")},
        text={Column {if(error.isNotBlank())Text(error)
            if(members.isEmpty())Text("No hi ha peticions pendents.")
            members.forEach {m-> Row(verticalAlignment=Alignment.CenterVertically){
                Text(m.displayName,modifier=Modifier.weight(1f))
                TextButton(onClick={scope.launch {try {api.respondToMember(groupId,m.userId,true);members=members.filterNot{it.userId==m.userId};onChanged()}catch(e:Exception){error=e.message.orEmpty()}}}){Text("Sí")}
                TextButton(onClick={scope.launch {try {api.respondToMember(groupId,m.userId,false);members=members.filterNot{it.userId==m.userId};onChanged()}catch(e:Exception){error=e.message.orEmpty()}}}){Text("No")}
            }}
        }},confirmButton={TextButton(onClick=onDismiss){Text("Tancar")}})
}

@Composable
private fun IosCommunityManageDialog(groupId:Int,api:IosAgendaApi,onDismiss:()->Unit,onChanged:()->Unit) {
    val scope=rememberCoroutineScope()
    var data by remember(groupId){mutableStateOf(IosCommunityManagement(emptyList(),emptyList()))}
    var error by remember {mutableStateOf("")}
    var target by remember {mutableStateOf<IosCommunityUser?>(null)}
    var pendingAction by remember {mutableStateOf("")}
    fun reload(){scope.launch {runCatching{api.communityManageMembers(groupId)}.onSuccess{data=it}.onFailure{error=it.message.orEmpty()}}}
    LaunchedEffect(groupId){reload()}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Gestionar participants")},
        text={Column(Modifier.verticalScroll(rememberScrollState())) {
            if(error.isNotBlank()) Text(error,fontSize=11.sp)
            data.members.filter { it.role!="CREATOR" && it.status=="ACCEPTED" }.forEach {m ->
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text(m.name,Modifier.weight(1f));TextButton(onClick={target=IosCommunityUser(m.id,m.name);pendingAction="BAN_MEMBER"}){Text("Expulsar")}
                }
            }
            Text("Persones expulsades",fontWeight=FontWeight.Bold)
            data.bans.forEach {u->Row(verticalAlignment=Alignment.CenterVertically){
                Text(u.name,Modifier.weight(1f));TextButton(onClick={target=u;pendingAction="UNBAN_MEMBER"}){Text("Readmetre")}
            }}
        }},confirmButton={TextButton(onClick=onDismiss){Text("Tancar")}})
    target?.let {u-> IosCommunityConfirmDialog("${if(pendingAction=="BAN_MEMBER")"Expulsar" else "Readmetre"} ${u.name}?",
        "Esta acció modifica l'accés al tauló.","Confirmar",onDismiss={target=null},onConfirm={scope.launch {
            try{api.moderateCommunityGroup(groupId,pendingAction,userId=u.id);target=null;reload();onChanged()}
            catch(e:Exception){error=e.message.orEmpty()}
        }}) }
}

@Composable
private fun IosBlockedPeopleDialog(api:IosAgendaApi,onDismiss:()->Unit) {
    val scope=rememberCoroutineScope()
    var users by remember{mutableStateOf(emptyList<IosCommunityUser>())}
    var error by remember{mutableStateOf("")}
    var chosen by remember{mutableStateOf<IosCommunityUser?>(null)}
    LaunchedEffect(Unit){runCatching{api.blockedCommunityUsers()}.onSuccess{users=it}.onFailure{error=it.message.orEmpty()}}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Persones bloquejades")},
        text={Column {if(error.isNotBlank()) Text(error,fontSize=12.sp)
            if(users.isEmpty()) Text("No tens persones bloquejades.")
            users.forEach{u->Row(verticalAlignment=Alignment.CenterVertically){
                Text(u.name,Modifier.weight(1f));TextButton(onClick={chosen=u}){Text("Desbloquejar")}
            }}
        }},confirmButton={TextButton(onClick=onDismiss){Text("Tancar")}})
    chosen?.let {u->IosCommunityConfirmDialog("Desbloquejar ${u.name}?","Podràs tornar a veure els seus missatges.","Desbloquejar",
        onDismiss={chosen=null},onConfirm={scope.launch{try{api.setCommunityBlocked(u.id,false);users=users.filterNot{it.id==u.id};chosen=null}
        catch(e:Exception){error=e.message.orEmpty()}}})}
}

@Composable
private fun IosAdminReportsDialog(api:IosAgendaApi,onDismiss:()->Unit) {
    val scope=rememberCoroutineScope()
    var reports by remember{mutableStateOf(emptyList<IosCommunityReport>())}
    var error by remember{mutableStateOf("")}
    var chosen by remember{mutableStateOf<IosCommunityReport?>(null)}
    var action by remember{mutableStateOf("")}
    fun reload(){scope.launch{runCatching{api.communityReports()}.onSuccess{reports=it}.onFailure{error=it.message.orEmpty()}}}
    LaunchedEffect(Unit){reload()}
    Dialog(onDismissRequest=onDismiss){Surface(shape=RoundedCornerShape(20.dp),color=Color.White,
        modifier=Modifier.fillMaxWidth().fillMaxHeight(.9f)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {Text("Moderació de Comunitat",fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                TextButton(onClick=onDismiss){Text("Tancar")}}
            if(error.isNotBlank()) Text(error,color=Color.Red,fontSize=11.sp)
            TextButton(onClick={reload()}){Text("Actualitzar denúncies")}
            LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                items(reports,key={it.id}){r-> ElevatedCard(Modifier.fillMaxWidth()){
                    Column(Modifier.padding(10.dp)) {
                        Text("#${r.id} · ${r.groupTitle}",fontWeight=FontWeight.Bold)
                        Text("Motiu: ${r.reason}",fontSize=12.sp)
                        if(r.messageText.isNotBlank()) Text("Missatge: ${r.messageText}",fontSize=11.sp)
                        if(r.reportedName.isNotBlank())Text("Persona: ${r.reportedName}",fontSize=11.sp)
                        Row(Modifier.horizontalScroll(rememberScrollState())) {
                            val actions=listOf("DISMISS" to "Descartar","REVIEW" to "Revisada",
                                "DELETE_MESSAGE" to "Retirar missatge","CLOSE_GROUP" to "Tancar tauló","BAN_MEMBER" to "Expulsar")
                            actions.filter {(_,label)-> when(label) {
                                "Retirar missatge" -> r.messageId>0
                                "Expulsar" -> r.reportedUserId>0 && r.groupId>0
                                else -> true
                            } }.forEach {(k,label)->TextButton(onClick={chosen=r;action=k}){Text(label,fontSize=11.sp)}}
                        }
                    }
                }}
            }
        }
    }}
    chosen?.let{r->IosCommunityConfirmDialog("Resoldre denúncia #${r.id}?","Acció: $action. Es registrarà qui l'ha efectuada.","Confirmar",
        onDismiss={chosen=null},onConfirm={scope.launch{
            try{api.resolveCommunityReport(r.id,action);chosen=null;reload()}
            catch(e:Exception){error=e.message.orEmpty()}
        }})}
}
