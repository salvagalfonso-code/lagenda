package com.example.agendavalenciana.ui

import com.example.agendavalenciana.model.AgendaDateFormat

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.agendavalenciana.BuildConfig
import com.example.agendavalenciana.model.AttendanceStatus
import com.example.agendavalenciana.model.CommunityBoardPolicy
import com.example.agendavalenciana.model.CommunityAccessMode
import com.example.agendavalenciana.model.CommunityGroup
import com.example.agendavalenciana.model.CommunityGroupType
import com.example.agendavalenciana.model.Concert
import com.example.agendavalenciana.state.AgendaState
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlin.math.roundToInt
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConcertDetailSheet(
    state: AgendaState,
    concert: Concert,
    onDismiss: () -> Unit,
    onOpenSpecialEvent: ((Concert) -> Unit)? = null,
    onRequireLogin: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val attendance = state.attendanceFor(concert.id)
    val groups = state.groupsForConcert(concert.id)
    val saved = state.isSavedAnywhere(concert.id)
    val purchased = concert.id in state.ticketPurchases
    LaunchedEffect(concert.id, state.currentAccount?.serverUserId) {
        state.refreshSpecialEvent(concert.id)
        if (state.currentAccount != null) state.refreshCommunityGroups(concert.id)
    }
    LaunchedEffect(concert.id) {
        while (true) {
            state.refreshSocialCounts(listOf(concert.id))
            delay(20_000)
        }
    }
    val specialConfig = state.specialEventRemote(concert.id)
        ?.takeIf { it.enabled }
        ?.let { remoteSpecialConfig(it, concert) }
        ?: specialEventConfigFor(concert)
    val shareArtwork = rememberImageBitmapFromUris(
        concert.imageCandidates(state.profileForArtist(concert.artist)?.photoUri),
        maxDimension = 720,
        eventId = concert.id,
        knownResolvedUri = concert.resolvedCardImageUri
    )

    var showCreateGroup by remember { mutableStateOf(false) }
    var createGroupType by remember(concert.id) { mutableStateOf(CommunityGroupType.CAR) }
    var selectedGroup by remember { mutableStateOf<CommunityGroup?>(null) }
    var showTicketDialog by remember { mutableStateOf(false) }
    var showCreateBatec by remember { mutableStateOf(false) }

    LaunchedEffect(concert.id, state.currentAccount?.serverUserId) {
        if (state.currentAccount != null) {
            state.refreshBatecEligibility(concert.id)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 38.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(horizontal = 18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(concert.title, fontSize = 25.sp, fontWeight = FontWeight.Black)
                            Text(
                                "${formatConcertDate(concert.feedDate)} · ${concert.time}",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f)
                            )
                        }
                        EventReportMenuButton(state = state, concert = concert, compact = false)
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Outlined.Close, contentDescription = "Tancar")
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.LocationOn, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("${concert.venue} · ${concert.town} · ${concert.comarca}")
                    }

                    Spacer(Modifier.height(14.dp))
                    ConcertHeroImage(
                        artist = concert.title,
                        photoUris = concert.imageCandidates(state.profileForArtist(concert.artist)?.photoUri)
                    )

                    if (concert.sponsored) {
                        Spacer(Modifier.height(10.dp))
                        Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                            Text(
                                concert.sponsorLabel ?: "Concert patrocinat",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (specialConfig != null) {
                        Spacer(Modifier.height(16.dp))
                        PersonalEventModePanel(
                            state = state,
                            concert = concert,
                            specialConfig = specialConfig,
                            onShareVisual = {
                                state.recordShare(concert.id)
                                EventShareCard.share(context, concert, shareArtwork?.asAndroidBitmap())
                            },
                            onOpenSpecialEvent = onOpenSpecialEvent?.let { openSpecial ->
                                {
                                    onDismiss()
                                    openSpecial(concert)
                                }
                            }
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("Info", fontWeight = FontWeight.Black, fontSize = 19.sp)
                    Spacer(Modifier.height(8.dp))
                    UniversalEventInfo(concert)

                    Spacer(Modifier.height(18.dp))
                    Text("Què vols fer?", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = saved,
                                onClick = { state.openSavePicker(concert.id) },
                                label = { Text("${if (saved) "Guardat" else "Guardar"} · ${state.savedCountForConcert(concert.id)}") },
                                leadingIcon = { Icon(Icons.Outlined.BookmarkBorder, null, modifier = Modifier.size(18.dp)) }
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (state.currentAccount == null) {
                                    onRequireLogin?.invoke()
                                } else if (concert.ticketUrl.isNotBlank()) {
                                    state.recordTicketClick(concert.id)
                                    openTicketUrl(context, concert.ticketUrl)
                                } else {
                                    showTicketDialog = true
                                }
                            },
                            enabled = concert.ticketAvailable,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.ConfirmationNumber, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                when {
                                    concert.ticketUrl.isNotBlank() && concert.isFree -> "Reservar entrades"
                                    concert.ticketUrl.isNotBlank() && concert.ticketPrice > 0 -> "Comprar · ${concert.ticketPrice} €"
                                    concert.ticketUrl.isNotBlank() -> "Comprar entrades"
                                    purchased && concert.isFree -> "Afegir reserva"
                                    purchased && concert.ticketPrice > 0 -> "Comprar més · ${ticketChargeLabel(concert.ticketPrice)}"
                                    concert.isFree -> "Reserva gratuïta"
                                    concert.ticketPrice > 0 -> "Entrades · ${ticketChargeLabel(concert.ticketPrice)}"
                                    else -> "Entrades"
                                }
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                state.recordShare(concert.id)
                                EventShareCard.share(context, concert, shareArtwork?.asAndroidBitmap())
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.Share, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Compartir")
                        }
                    }

                    Spacer(Modifier.height(22.dp))
                    Divider()
                    Spacer(Modifier.height(18.dp))

                    val eventBatecs = state.batecsForConcert(concert.id)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("⚡ Batecs", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                            Text(
                                state.batecCreationMessage(concert),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .62f)
                            )
                        }
                        if (BuildConfig.IS_PRO_APP && state.canCreateBatecForConcert(concert)) {
                            Button(onClick = { showCreateBatec = true }) {
                                Text("Crear batec")
                            }
                        }
                    }

                    if (eventBatecs.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        eventBatecs.take(5).forEach { batec ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            batec.artist,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (batec.organizer) {
                                            Surface(
                                                shape = RoundedCornerShape(999.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Text(
                                                    "Organització",
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    Text(batec.title, fontWeight = FontWeight.Black)
                                    if (batec.body.isNotBlank()) {
                                        Text(
                                            batec.body,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Encara no hi ha batecs d'este concert.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(22.dp))
                    Divider()
                    Spacer(Modifier.height(18.dp))

                    Text("Comunitat", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(8.dp))
                    CommunityBoardSections(
                        state = state,
                        concertId = concert.id,
                        groups = groups,
                        onCreate = { selected ->
                            createGroupType = selected
                            showCreateGroup = true
                        },
                        onOpen = { selectedGroup = it }
                    )
                }
            }
        }
    }

    if (showCreateBatec) {
        CreateBatecDialog(
            state = state,
            concert = concert,
            onDismiss = { showCreateBatec = false },
            onPublished = { showCreateBatec = false }
        )
    }

    if (showCreateGroup) {
        CreateCommunityGroupDialog(
            state = state,
            concert = concert,
            onDismiss = { showCreateGroup = false },
            onCreated = { showCreateGroup = false; state.refreshCommunityGroups(concert.id) },
            initialType = createGroupType
        )
    }

    if (showTicketDialog) {
        TicketPurchaseDialog(
            state = state,
            concert = concert,
            onDismiss = { showTicketDialog = false },
            onPurchased = { showTicketDialog = false }
        )
    }

    selectedGroup?.let { group ->
        CommunityChatDialog(
            state = state,
            groupId = group.id,
            onDismiss = { selectedGroup = null }
        )
    }
}

@Composable
fun CreateBatecDialog(
    state: AgendaState,
    concert: Concert,
    onDismiss: () -> Unit,
    onPublished: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = {
            if (!state.isPublishingBatec) onDismiss()
        },
        title = { Text("Crear batec") },
        text = {
            Column {
                Text(
                    concert.title,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    state.batecCreationMessage(concert),
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; error = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Títol") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it; error = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Què està passant?") },
                    minLines = 3
                )
                error?.let {
                    Text(
                        it,
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank() && !state.isPublishingBatec,
                onClick = {
                    state.publishEventBatec(concert.id, title, body) { ok, message ->
                        if (ok) onPublished() else error = message
                    }
                }
            ) {
                Text(if (state.isPublishingBatec) "Publicant…" else "Publicar")
            }
        },
        dismissButton = {
            TextButton(
                enabled = !state.isPublishingBatec,
                onClick = onDismiss
            ) { Text("Cancel·lar") }
        }
    )
}

@Composable
internal fun CommunityGroupCard(
    state: AgendaState,
    group: CommunityGroup,
    onOpen: () -> Unit
) {
    val member = state.isMemberOfCommunityGroup(group.id)
    val creator = state.isCreatorOfCommunityGroup(group.id)
    var feedback by remember(group.id, group.myStatus) { mutableStateOf<String?>(null) }
    val full = group.status == "FULL"
    val closed = group.status == "CLOSED"
    val detailLine = feedback ?: buildString {
        append(group.type.label)
        append(if (group.chatEnabled) " · Amb xat" else " · Sense xat")
        if (group.town.isNotBlank()) append(" · ${group.town}")
        if (group.meetingAt.isNotBlank()) append(" · ${formatCommunityMeeting(group.meetingAt)}")
        if (group.capacity != null) append(" · ${group.capacity} places anunciades")
        else if (group.chatEnabled) append(" · ${group.members.size} persones")
        when {
            full -> append(" · Complet")
            closed -> append(" · Tancat")
            else -> append(if (group.accessMode == CommunityAccessMode.OPEN) " · Obert" else " · Amb aprovació")
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(group.type.emoji, fontSize = 19.sp)
            Spacer(Modifier.width(7.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    group.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    detailLine,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .60f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(6.dp))
            when {
                !group.chatEnabled -> OutlinedButton(
                    onClick = onOpen,
                    modifier = Modifier.height(32.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 9.dp, vertical = 0.dp)
                ) { Text(if (creator) "Consultes" else "Contactar", fontSize = 10.sp) }
                member -> Button(
                    onClick = onOpen,
                    modifier = Modifier.height(32.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 9.dp, vertical = 0.dp)
                ) {
                    Text(
                        if (creator && group.pendingCount > 0) "Obrir · ${group.pendingCount}" else "Obrir",
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
                group.myStatus == "PENDING" -> OutlinedButton(
                    onClick = {},
                    modifier = Modifier.height(32.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) { Text("Pendent", fontSize = 10.sp, maxLines = 1) }
                full -> OutlinedButton(
                    enabled = false,
                    onClick = {},
                    modifier = Modifier.height(32.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) { Text("Complet", fontSize = 10.sp, maxLines = 1) }
                closed -> OutlinedButton(
                    enabled = false,
                    onClick = {},
                    modifier = Modifier.height(32.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) { Text("Tancat", fontSize = 10.sp, maxLines = 1) }
                else -> OutlinedButton(
                    onClick = {
                        state.joinCommunityGroup(group.id) { ok, msg ->
                            feedback = msg
                            if (ok && group.accessMode == CommunityAccessMode.OPEN) onOpen()
                        }
                    },
                    modifier = Modifier.height(32.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        if (group.accessMode == CommunityAccessMode.OPEN) "Entrar" else "Sol·licitar",
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** Cinc seccions permanents en tots els esdeveniments, amb contingut o accés a crear. */
@Composable
internal fun CommunityBoardSections(
    state: AgendaState,
    concertId: Int,
    groups: List<CommunityGroup>,
    onCreate: (CommunityGroupType) -> Unit,
    onOpen: (CommunityGroup) -> Unit
) {
    val expanded = remember(concertId) { mutableStateMapOf<CommunityGroupType, Boolean>() }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        CommunityBoardPolicy.defaultTypes.forEach { section ->
            val sectionGroups = CommunityBoardPolicy.boardsInSection(groups, section)
            // Seccions amb anuncis obertes d'entrada; les buides demanen tocar per crear.
            val open = expanded[section] ?: sectionGroups.isNotEmpty()
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { expanded[section] = !open },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(section.emoji, fontSize = 19.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(section.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(
                        if (sectionGroups.isEmpty()) "Crear" else "${sectionGroups.size} ${if (sectionGroups.size == 1) "tauló" else "taulons"}",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (open) "▴" else "▾", fontSize = 14.sp)
                }
            }
            if (open) {
                if (sectionGroups.isEmpty()) {
                    Text(
                        "Encara no hi ha taulons d'esta secció per a este esdeveniment.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                } else {
                    sectionGroups.forEach { board ->
                        CommunityGroupCard(state = state, group = board, onOpen = { onOpen(board) })
                    }
                }
                OutlinedButton(
                    onClick = { onCreate(section) },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(if (sectionGroups.isEmpty()) "Crear el primer tauló" else "Crear un altre tauló", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
internal fun CreateCommunityGroupDialog(
    state: AgendaState,
    concert: Concert,
    onDismiss: () -> Unit,
    onCreated: () -> Unit,
    initialType: CommunityGroupType = CommunityGroupType.CAR
) {
    val types = CommunityBoardPolicy.defaultTypes
    var type by remember(concert.id, initialType) { mutableStateOf(initialType) }
    var accessMode by remember(concert.id) { mutableStateOf(CommunityAccessMode.APPROVAL) }
    var chatEnabled by remember(concert.id) { mutableStateOf(false) }
    var town by remember(concert.id) { mutableStateOf(state.userPreferences.preferredLocations.firstOrNull() ?: "") }
    var meetingTime by remember(concert.id) { mutableStateOf("") }
    var subject by remember(concert.id) { mutableStateOf("") }
    var description by remember(concert.id) { mutableStateOf("") }
    var capacity by remember(concert.id) { mutableStateOf("") }
    var creating by remember(concert.id) { mutableStateOf(false) }
    var error by remember(concert.id) { mutableStateOf<String?>(null) }
    val seats = capacity.toIntOrNull()?.takeIf { it > 0 }
    val title = CommunityBoardPolicy.title(type, town, seats, subject)
    val requiresLocation = type != CommunityGroupType.OTHER
    val allowsPlaces = type in listOf(CommunityGroupType.CAR, CommunityGroupType.TRANSPORT, CommunityGroupType.HOSTING)
    val locationLabel = when (type) {
        CommunityGroupType.CAR, CommunityGroupType.TRANSPORT -> "D'on ix el transport? (poble o comarca)"
        CommunityGroupType.HOSTING, CommunityGroupType.NEED_HOSTING -> "On està o busques l'allotjament?"
        CommunityGroupType.MEETUP -> "On serà la quedada?"
        else -> "Ubicació (opcional)"
    }

    Dialog(onDismissRequest = { if (!creating) onDismiss() }) {
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            LazyColumn(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Crear tauló", fontSize = 21.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                        IconButton(enabled = !creating, onClick = onDismiss) { Icon(Icons.Outlined.Close, null) }
                    }
                    Text(concert.title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Tipus de tauló", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 7.dp))
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(types) { option ->
                            FilterChip(selected = CommunityBoardPolicy.section(type) == option, onClick = { type = option; error = null }, label = { Text("${option.emoji} ${option.label}") })
                        }
                    }
                }
                if (CommunityBoardPolicy.section(type) == CommunityGroupType.HOSTING) item {
                    Text("Què vols fer amb l'allotjament?", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        FilterChip(selected = type == CommunityGroupType.HOSTING,
                            onClick = { type = CommunityGroupType.HOSTING; error = null },
                            label = { Text("Compartisc") })
                        FilterChip(selected = type == CommunityGroupType.NEED_HOSTING,
                            onClick = { type = CommunityGroupType.NEED_HOSTING; error = null },
                            label = { Text("Busque") })
                    }
                }
                item {
                    if (requiresLocation) {
                        OutlinedTextField(value = town, onValueChange = { town = it; error = null }, modifier = Modifier.fillMaxWidth(), label = { Text(locationLabel) })
                    }
                    if (type == CommunityGroupType.OTHER) {
                        OutlinedTextField(value = subject, onValueChange = { subject = it; error = null }, modifier = Modifier.fillMaxWidth(), label = { Text("De què tracta el tauló?") })
                    }
                    if (allowsPlaces) {
                        OutlinedTextField(value = capacity, onValueChange = { capacity = it.filter(Char::isDigit).take(3) }, modifier = Modifier.fillMaxWidth(), label = { Text("Places disponibles") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                    OutlinedTextField(value = meetingTime, onValueChange = { meetingTime = it.take(5) }, modifier = Modifier.fillMaxWidth(), label = { Text("Hora d'eixida o trobada (opcional, HH:mm)") }, placeholder = { Text("13:30") })
                    OutlinedTextField(value = description, onValueChange = { description = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Informació addicional") }, minLines = 2)
                }
                item {
                    Text("Títol automàtic", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                        Text(title, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                    }
                    Text("El títol es crea a partir de les dades que has introduït.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item {
                    Text("Com es comunicarà la gent?", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !chatEnabled, onClick = { chatEnabled = false }, label = { Text("Tauló sense xat") })
                        FilterChip(selected = chatEnabled, onClick = { chatEnabled = true }, label = { Text("Tauló amb xat") })
                    }
                    Text(if (chatEnabled) "Hi haurà un xat públic entre les persones admeses i una via privada amb el creador."
                        else "La informació serà visible; les consultes al creador seran privades, no un xat públic.",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (chatEnabled) item {
                    Text("Accés al xat", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        FilterChip(selected = accessMode == CommunityAccessMode.OPEN, onClick = { accessMode = CommunityAccessMode.OPEN }, label = { Text("Obert") })
                        FilterChip(selected = accessMode == CommunityAccessMode.APPROVAL, onClick = { accessMode = CommunityAccessMode.APPROVAL }, label = { Text("Amb aprovació") })
                    }
                }
                item {
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                    Button(
                        onClick = {
                            val time = meetingTime.trim()
                            val validTime = time.isBlank() || time.matches(Regex("^([01]\\d|2[0-3]):[0-5]\\d$"))
                            when {
                                requiresLocation && town.isBlank() -> error = "Indica una població o comarca."
                                type == CommunityGroupType.OTHER && subject.isBlank() -> error = "Explica de què tracta el tauló."
                                !validTime -> error = "Escriu l'hora com 13:30."
                                else -> {
                                    creating = true
                                    val meetingAt = time.takeIf { it.isNotBlank() }?.let { "${concert.date} $it:00" }
                                    state.createCommunityGroup(concert.id, type, accessMode, town.trim(), meetingAt, title,
                                        description, seats, chatEnabled = chatEnabled) { ok, msg ->
                                        creating = false
                                        if (ok) onCreated() else error = msg
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(), enabled = !creating
                    ) { Text(if (creating) "Creant…" else "Crear tauló") }
                }
            }
        }
    }
}

@Composable
internal fun CommunityChatDialog(
    state: AgendaState,
    groupId: Int,
    onDismiss: () -> Unit
) {
    val group = state.communityGroups.firstOrNull { it.id == groupId } ?: return
    if (!group.chatEnabled) {
        CommunityBoardInquiryDialog(state = state, group = group, onDismiss = onDismiss)
        return
    }
    val member = state.isMemberOfCommunityGroup(group.id)
    val creator = state.isCreatorOfCommunityGroup(group.id)
    var showInquiries by remember(groupId) { mutableStateOf(false) }
    val requests = state.requestsForCommunityGroup(group.id)
    val messages = state.messagesForCommunityGroup(group.id)
    var text by remember(groupId) { mutableStateOf("") }
    var feedback by remember(groupId) { mutableStateOf<String?>(null) }
    var showEditGroup by remember(groupId) { mutableStateOf(false) }

    LaunchedEffect(groupId, member, creator) {
        if (member) state.refreshCommunityMessages(groupId)
        if (creator) state.refreshCommunityRequests(groupId)
        while (member) {
            delay(5000)
            state.refreshCommunityMessages(groupId)
            if (creator) state.refreshCommunityRequests(groupId)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(modifier = Modifier.fillMaxWidth().height(680.dp), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(group.type.emoji, fontSize = 24.sp)
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(group.title, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            listOfNotNull(group.town.takeIf(String::isNotBlank), group.meetingAt.takeIf(String::isNotBlank)?.let(::formatCommunityMeeting)).joinToString(" · "),
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { showInquiries = true }) { Text(if (creator) "Consultes" else "Contactar") }
                    if (creator) TextButton(onClick = { showEditGroup = true }) { Text("Editar") }
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, null) }
                }
                Divider()

                if (creator && requests.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .35f)).padding(12.dp)) {
                        Text("Sol·licituds pendents (${requests.size})", fontWeight = FontWeight.Bold)
                        requests.forEach { request ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                                CommunityAvatar(request.displayName, request.photoUri, 30.dp)
                                Spacer(Modifier.width(8.dp))
                                Text(request.displayName, modifier = Modifier.weight(1f), fontSize = 13.sp)
                                TextButton(onClick = { state.respondCommunityRequest(groupId, request.userId, false) { _, msg -> feedback = msg } }) { Text("Rebutjar") }
                                Button(onClick = { state.respondCommunityRequest(groupId, request.userId, true) { _, msg -> feedback = msg } }) { Text("Acceptar") }
                            }
                        }
                    }
                }

                when {
                    group.myStatus == "PENDING" -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("La teua sol·licitud està pendent d'aprovació.", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                    !member -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                                Text(
                                    if (group.accessMode == CommunityAccessMode.OPEN)
                                        "Pots entrar directament. El xat apareixerà al teu menú Comunitat quan envies el primer missatge."
                                    else "Sol·licita entrar; el creador ha d’aprovar-te abans d’accedir al xat.",
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(Modifier.height(12.dp))
                                Button(onClick = { state.joinCommunityGroup(group.id) { _, msg -> feedback = msg } }) {
                                    Text(if (group.accessMode == CommunityAccessMode.OPEN) "Entrar al xat" else "Sol·licitar entrar al xat")
                                }
                            }
                        }
                    }
                    else -> {
                        LazyColumn(modifier = Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
                            item {
                                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Informació del grup", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        if (group.description.isNotBlank()) Text(group.description, fontSize = 12.sp)
                                        Text("Creat per ${group.creator} · ${if (group.accessMode == CommunityAccessMode.OPEN) "Obert" else "Amb aprovació"}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            if (messages.isEmpty()) item { Text("Encara no hi ha missatges. Comença la conversa.") }
                            else items(messages, key = { it.id }) { message ->
                                CommunityMessageCard(state, message.author, message.text, message.timestamp, false, message.photoUri)
                            }
                        }

                        feedback?.let { Text(it, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)) }
                        if (!creator) {
                            TextButton(onClick = { state.leaveCommunityGroup(group.id) { ok, msg -> feedback = msg; if (ok) onDismiss() } }, modifier = Modifier.padding(horizontal = 8.dp)) { Text("Abandonar grup") }
                        } else {
                            TextButton(onClick = { state.setCommunityGroupClosed(group.id, group.status != "CLOSED") { _, msg -> feedback = msg } }, modifier = Modifier.padding(horizontal = 8.dp)) {
                                Text(if (group.status == "CLOSED") "Reobrir grup" else "Tancar inscripcions")
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp).imePadding(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.weight(1f), placeholder = { Text("Missatge al grup…") }, singleLine = true)
                            Button(onClick = { val outgoing=text; state.sendCommunityChatMessage(group.id, outgoing) { ok, msg -> if (ok) text="" else feedback=msg } }, enabled = text.isNotBlank()) { Text("Enviar") }
                        }
                    }
                }
            }
        }
    }
    if (showInquiries) {
        CommunityBoardInquiryDialog(state = state, group = group, onDismiss = { showInquiries = false })
    }
    if (showEditGroup && creator) {
        EditCommunityGroupDialog(
            state = state,
            groupId = groupId,
            onDismiss = { showEditGroup = false },
            onSaved = {
                showEditGroup = false
                feedback = "Xat actualitzat."
            }
        )
    }
}

@Composable
private fun CommunityBoardInquiryDialog(
    state: AgendaState,
    group: CommunityGroup,
    onDismiss: () -> Unit
) {
    val creator = state.isCreatorOfCommunityGroup(group.id)
    var selectedPeer by remember(group.id) { mutableStateOf<Int?>(null) }
    var showEditBoard by remember(group.id) { mutableStateOf(false) }
    var draft by remember(group.id) { mutableStateOf("") }
    var feedback by remember(group.id) { mutableStateOf<String?>(null) }
    var sending by remember(group.id) { mutableStateOf(false) }
    val inquiries = state.communityBoardInquiries.filter { it.groupId == group.id }
    val peers = inquiries.distinctBy { it.peerUserId }.sortedBy { it.peerName }
    val currentPeer = if (creator) selectedPeer ?: peers.firstOrNull()?.peerUserId else group.creatorUserId
    val thread = inquiries.filter { !creator || it.peerUserId == currentPeer }

    LaunchedEffect(group.id) {
        state.refreshCommunityBoardInquiries(group.id)
        while (true) {
            delay(15000)
            state.refreshCommunityBoardInquiries(group.id)
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(modifier = Modifier.fillMaxWidth().height(610.dp), shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (creator) "Consultes del tauló" else "Contactar amb el creador", fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text(group.title, fontSize = 11.sp, maxLines = 2)
                    }
                    if (creator) TextButton(onClick = { showEditBoard = true }) { Text("Editar") }
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, null) }
                }
                if (group.description.isNotBlank()) {
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Text(group.description, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                    }
                }
                Text("Conversa privada: les altres persones del tauló no poden llegir-la.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (creator && peers.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                        items(peers, key = { it.peerUserId }) { peer ->
                            FilterChip(selected = currentPeer == peer.peerUserId, onClick = { selectedPeer = peer.peerUserId }, label = { Text(peer.peerName) })
                        }
                    }
                }
                LazyColumn(modifier = Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 10.dp)) {
                    if (thread.isEmpty()) item { Text(if (creator) "Encara no hi ha consultes." else "Escriu al creador del tauló.", fontSize = 12.sp) }
                    items(thread, key = { it.id }) { message ->
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(11.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(message.author + if (message.own) " · Tu" else "", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(message.body, fontSize = 13.sp)
                            }
                        }
                    }
                }
                state.communityInquiriesError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
                feedback?.let { Text(it, fontSize = 11.sp) }
                if (!creator || currentPeer != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        OutlinedTextField(value = draft, onValueChange = { draft = it }, modifier = Modifier.weight(1f), placeholder = { Text("Missatge privat…") }, maxLines = 3)
                        Button(enabled = !sending && draft.isNotBlank(), onClick = {
                            sending = true
                            state.sendCommunityBoardInquiry(group.id, if (creator) currentPeer else null, draft) { ok, msg ->
                                sending = false
                                feedback = msg
                                if (ok) draft = ""
                            }
                        }) { Text("Enviar") }
                    }
                }
            }
        }
    }
    if (showEditBoard) {
        EditCommunityGroupDialog(state = state, groupId = group.id,
            onDismiss = { showEditBoard = false },
            onSaved = { showEditBoard = false })
    }
}

@Composable
private fun EditCommunityGroupDialog(
    state: AgendaState,
    groupId: Int,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val group = state.communityGroups.firstOrNull { it.id == groupId } ?: return
    var type by remember(groupId, group.type) { mutableStateOf(group.type) }
    var accessMode by remember(groupId, group.accessMode) { mutableStateOf(group.accessMode) }
    var chatEnabled by remember(groupId, group.chatEnabled) { mutableStateOf(group.chatEnabled) }
    var title by remember(groupId, group.title) { mutableStateOf(group.title) }
    var town by remember(groupId, group.town) { mutableStateOf(group.town) }
    var meetingAt by remember(groupId, group.meetingAt) { mutableStateOf(group.meetingAt) }
    var description by remember(groupId, group.description) { mutableStateOf(group.description) }
    var capacity by remember(groupId, group.capacity) { mutableStateOf(group.capacity?.toString().orEmpty()) }
    var saving by remember(groupId) { mutableStateOf(false) }
    var error by remember(groupId) { mutableStateOf<String?>(null) }
    val editableTypes = CommunityBoardPolicy.defaultTypes

    Dialog(onDismissRequest = { if (!saving) onDismiss() }) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(680.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Editar xat", fontSize = 21.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                        IconButton(enabled = !saving, onClick = onDismiss) { Icon(Icons.Outlined.Close, null) }
                    }
                }
                item {
                    Text("Tipus", fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.padding(top = 5.dp)) {
                        items(editableTypes) { option ->
                            FilterChip(
                                selected = CommunityBoardPolicy.section(type) == option,
                                onClick = { type = option },
                                label = { Text("${option.emoji} ${option.label}") }
                            )
                        }
                    }
                }
                if (CommunityBoardPolicy.section(type) == CommunityGroupType.HOSTING) item {
                    Text("Allotjament", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        FilterChip(selected = type == CommunityGroupType.HOSTING,
                            onClick = { type = CommunityGroupType.HOSTING }, label = { Text("Compartisc") })
                        FilterChip(selected = type == CommunityGroupType.NEED_HOSTING,
                            onClick = { type = CommunityGroupType.NEED_HOSTING }, label = { Text("Busque") })
                    }
                }
                item {
                    Text("Accés", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.padding(top = 5.dp)) {
                        FilterChip(
                            selected = accessMode == CommunityAccessMode.OPEN,
                            onClick = { accessMode = CommunityAccessMode.OPEN },
                            label = { Text("Obert") }
                        )
                        FilterChip(
                            selected = accessMode == CommunityAccessMode.APPROVAL,
                            onClick = { accessMode = CommunityAccessMode.APPROVAL },
                            label = { Text("Amb aprovació") }
                        )
                    }
                }
                item {
                    Text("Tipus de tauló", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !chatEnabled, onClick = { chatEnabled = false }, label = { Text("Sense xat") })
                        FilterChip(selected = chatEnabled, onClick = { chatEnabled = true }, label = { Text("Amb xat") })
                    }
                }
                item { OutlinedTextField(title, { title = it; error = null }, Modifier.fillMaxWidth(), label = { Text("Nom del tauló") }) }
                item { OutlinedTextField(town, { town = it }, Modifier.fillMaxWidth(), label = { Text("Origen o punt de trobada") }) }
                item {
                    OutlinedTextField(
                        AgendaDateFormat.editableDateTime(meetingAt),
                        { meetingAt = AgendaDateFormat.inputDateTimeIso(it) },
                        Modifier.fillMaxWidth(),
                        label = { Text("Data/hora de trobada") }
                    )
                    Text(
                        "Ex.: 04/10/2026 13:30. Deixa-ho buit si no cal.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item { OutlinedTextField(description, { description = it }, Modifier.fillMaxWidth(), label = { Text("Informació") }, minLines = 2) }
                item {
                    OutlinedTextField(
                        capacity,
                        { capacity = it.filter(Char::isDigit) },
                        Modifier.fillMaxWidth(),
                        label = { Text("Places màximes (opcional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                item {
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                    Button(
                        onClick = {
                            saving = true
                            val meeting = meetingAt.trim().ifBlank { null }
                            state.updateCommunityGroup(
                                groupId = groupId,
                                type = type,
                                accessMode = accessMode,
                                town = town,
                                meetingAt = meeting,
                                title = title,
                                description = description,
                                capacity = capacity.toIntOrNull(),
                                chatEnabled = chatEnabled
                            ) { ok, msg ->
                                saving = false
                                if (ok) onSaved() else error = msg
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = title.isNotBlank() && !saving
                    ) { Text(if (saving) "Guardant…" else "Guardar canvis") }
                }
            }
        }
    }
}

@Composable
private fun CommunityMessageCard(
    state: AgendaState,
    author: String,
    text: String,
    timestamp: String,
    official: Boolean,
    photoUri: String? = null
) {
    val identity = state.communityIdentity(author)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (official || identity.official) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
            CommunityAvatar(identity.displayName, photoUri ?: identity.photoUri)
            Spacer(Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(identity.displayName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    if (official || identity.official) {
                        Spacer(Modifier.width(6.dp))
                        Text("OFICIAL", fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(AgendaDateFormat.dateTime(timestamp), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .48f))
                }
                if (identity.tastes.isNotEmpty()) {
                    Text(
                        identity.tastes.joinToString(" · "),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(text, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

private fun formatCommunityMeeting(raw: String): String = AgendaDateFormat.dateTime(raw)

@Composable
private fun CommunityAvatar(name: String, photoUri: String?, size: androidx.compose.ui.unit.Dp = 38.dp) {
    val bitmap = rememberImageBitmapFromUri(photoUri, maxDimension = 192)
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = name,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            val initials = name.split(" ").filter { it.isNotBlank() }.take(2)
                .joinToString("") { it.first().uppercaseChar().toString() }
            Text(initials, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Black, fontSize = (size.value * .31f).sp)
        }
    }
}

@Composable
private fun ConcertHeroImage(artist: String, photoUris: List<String?>) {
    val image = rememberImageBitmapFromUris(photoUris, maxDimension = 720)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = "Foto de $artist",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .58f))))
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp)
        ) {
            Text(artist, color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
        }
    }
}

@Composable
internal fun TicketPurchaseDialog(
    state: AgendaState,
    concert: Concert,
    onDismiss: () -> Unit,
    onPurchased: () -> Unit
) {
    val context = LocalContext.current
    var quantity by remember(concert.id) { mutableStateOf(1) }
    var selectedOptionId by remember(concert.id) { mutableStateOf<Long?>(null) }
    var ticketError by remember(concert.id) { mutableStateOf<String?>(null) }
    var purchaseSuccessMessage by remember(concert.id) { mutableStateOf<String?>(null) }
    var pendingOrderId by remember(concert.id) { mutableStateOf<Long?>(null) }
    var paymentSheetOpen by remember(concert.id) { mutableStateOf(false) }

    LaunchedEffect(concert.id) {
        state.refreshTicketOptions(concert.id)
    }
    val ticketOptions = state.ticketOptionsFor(concert).filter { it.active }
    LaunchedEffect(ticketOptions) {
        if (ticketOptions.isNotEmpty() && ticketOptions.none { it.id == selectedOptionId }) {
            selectedOptionId = ticketOptions.first().id
            quantity = 1
        }
    }
    val selectedOption = ticketOptions.firstOrNull { it.id == selectedOptionId }
    val unitPrice = selectedOption?.price ?: concert.ticketPrice
    val effectiveIsFree = unitPrice <= 0
    val maxPerOrder = (selectedOption?.maxPerOrder ?: 6).coerceAtLeast(1)
    val remaining = selectedOption?.remaining
    val quantityLimit = minOf(maxPerOrder, remaining ?: maxPerOrder).coerceAtLeast(1)
    val soldOut = selectedOption?.isSoldOut == true

    val paymentSheet = PaymentSheet.Builder { result ->
        paymentSheetOpen = false
        when (result) {
            is PaymentSheetResult.Completed -> {
                val orderId = pendingOrderId
                if (orderId == null) {
                    ticketError = "Stripe ha finalitzat, però falta l'identificador de la comanda."
                } else {
                    ticketError = "Pagament fet. Verificant amb el servidor…"
                    state.finalizeStripePayment(concert.id, orderId) { ok, message ->
                        if (ok) {
                            ticketError = null
                            purchaseSuccessMessage = message
                        } else {
                            ticketError = message
                        }
                    }
                }
            }
            is PaymentSheetResult.Canceled -> {
                ticketError = null
            }
            is PaymentSheetResult.Failed -> {
                ticketError = result.error.localizedMessage ?: "Stripe no ha pogut completar el pagament."
            }
        }
    }.build()

    val successMessage = purchaseSuccessMessage
    AlertDialog(
        onDismissRequest = {
            if (successMessage != null) {
                onPurchased()
            } else if (!state.isPurchasingTicket && !paymentSheetOpen) {
                onDismiss()
            }
        },
        title = {
            Text(
                when {
                    successMessage != null && effectiveIsFree -> "Reserva confirmada"
                    successMessage != null -> "Entrades adquirides"
                    effectiveIsFree -> "Reserva d'entrades"
                    else -> "Compra d'entrades"
                }
            )
        },
        text = {
            if (successMessage != null) {
                Column {
                    Text("✓", fontSize = 42.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(8.dp))
                    Text(successMessage, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (quantity == 1) "Ja tens l'entrada disponible en l'apartat Entrades."
                        else "Ja tens les entrades disponibles en l'apartat Entrades.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column {
                    Text("${concert.artist} · ${concert.town}", fontWeight = FontWeight.Bold)
                    Text("${formatConcertDate(concert.feedDate)} · ${concert.time}", fontSize = 13.sp)
                    if (ticketOptions.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        Text("Tria entrada", fontWeight = FontWeight.Black, fontSize = 13.sp)
                        Spacer(Modifier.height(6.dp))
                        ticketOptions.forEach { option ->
                            val details = listOfNotNull(
                                option.sessionDate.takeIf { it.isNotBlank() },
                                option.sessionTime.takeIf { it.isNotBlank() },
                                option.sector.takeIf { it.isNotBlank() },
                                option.offer.takeIf { it.isNotBlank() }
                            ).joinToString(" · ")
                            FilterChip(
                                selected = selectedOptionId == option.id,
                                enabled = !option.isSoldOut && !state.isPurchasingTicket && !paymentSheetOpen,
                                onClick = {
                                    selectedOptionId = option.id
                                    quantity = 1
                                    ticketError = null
                                },
                                label = {
                                    Column {
                                        Text(
                                            buildString {
                                                append(option.name)
                                                append(" · ")
                                                append(if (option.price <= 0) "Gratuït" else ticketChargeLabel(option.price))
                                                if (option.isSoldOut) append(" · Esgotada")
                                            },
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (details.isNotBlank()) {
                                            Text(details, fontSize = 11.sp)
                                        }
                                        option.remaining?.let { places ->
                                            if (!option.isSoldOut) Text("$places places disponibles", fontSize = 11.sp)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    state.ticketCapacityFor(concert)?.let { capacity ->
                        Spacer(Modifier.height(8.dp))
                        Text("Aforament total: $capacity", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            enabled = !state.isPurchasingTicket && !paymentSheetOpen,
                            onClick = { if (quantity > 1) quantity-- }
                        ) { Text("−") }
                        Text("$quantity", modifier = Modifier.padding(horizontal = 18.dp), fontSize = 20.sp, fontWeight = FontWeight.Black)
                        OutlinedButton(
                            enabled = !state.isPurchasingTicket && !paymentSheetOpen,
                            onClick = { if (quantity < quantityLimit) quantity++ }
                        ) { Text("+") }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (effectiveIsFree) "Reserva gratuïta · $quantity entrada/es"
                        else "Total amb gestió: ${formatEuroCents(ticketChargeUnitCents(unitPrice) * quantity)}",
                        fontWeight = FontWeight.Bold
                    )
                    if (!effectiveIsFree) {
                        Text(
                            "Preu base: ${unitPrice} € per entrada · comissió aplicada segons la tarifa de gestió.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (effectiveIsFree) {
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = .72f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "Si reserves, intenta assistir o anul·lar amb temps. Les absències reiterades sense cancel·lació podran limitar temporalment futures reserves gratuïtes.",
                                modifier = Modifier.padding(10.dp),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    } else {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Pagament segur amb Stripe. L'entrada només es crea quan el servidor confirma el cobrament.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    ticketError?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = if (it.startsWith("Pagament fet")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            if (successMessage != null) {
                Button(onClick = onPurchased) {
                    Text("D'acord")
                }
            } else {
                Button(
                    enabled = !state.isPurchasingTicket && !paymentSheetOpen && !soldOut,
                    onClick = {
                        ticketError = null
                        if (effectiveIsFree) {
                            state.purchaseTicket(concert.id, quantity, selectedOptionId) { ok, message ->
                                if (ok) {
                                    ticketError = null
                                    purchaseSuccessMessage = message
                                } else {
                                    ticketError = message
                                }
                            }
                        } else {
                            state.createStripePayment(concert.id, quantity, selectedOptionId) { start, error ->
                                if (start == null) {
                                    ticketError = error ?: "No s'ha pogut iniciar Stripe."
                                } else {
                                    runCatching {
                                        PaymentConfiguration.init(context, start.publishableKey)
                                        pendingOrderId = start.orderId
                                        paymentSheetOpen = true
                                        paymentSheet.presentWithPaymentIntent(
                                            start.clientSecret,
                                            PaymentSheet.Configuration.Builder(start.merchantName).build()
                                        )
                                    }.onFailure { ex ->
                                        paymentSheetOpen = false
                                        ticketError = ex.localizedMessage ?: "No s'ha pogut obrir Stripe."
                                    }
                                }
                            }
                        }
                    }
                ) {
                    Text(
                        when {
                            state.isPurchasingTicket -> "Processant…"
                            soldOut -> "Esgotada"
                            effectiveIsFree -> "Reservar"
                            else -> "Pagar amb Stripe"
                        }
                    )
                }
            }
        },
        dismissButton = {
            if (successMessage == null) {
                TextButton(
                    enabled = !state.isPurchasingTicket && !paymentSheetOpen,
                    onClick = onDismiss
                ) { Text("Cancel·lar") }
            }
        }
    )
}


private fun ticketChargeUnitCents(baseEuros: Int): Int {
    if (baseEuros <= 0) return 0
    return ((baseEuros * 100 + 25 + 121) * 1.015).roundToInt()
}

private fun formatEuroCents(cents: Int): String = String.format(
    Locale("ca", "ES"),
    "%.2f €",
    cents / 100.0
)

private fun ticketChargeLabel(baseEuros: Int): String = formatEuroCents(ticketChargeUnitCents(baseEuros))

internal fun openTicketUrl(context: Context, rawUrl: String) {
    val url = rawUrl.trim().let {
        if (it.startsWith("http://") || it.startsWith("https://")) it else "https://$it"
    }
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
    }
}


private fun formatConcertDate(raw: String): String = AgendaDateFormat.date(raw)
