package com.example.agendavalenciana.ui

import com.example.agendavalenciana.model.AgendaDateFormat

import com.example.agendavalenciana.data.FestivalFeedNextDay

import android.content.Intent
import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.net.Uri

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.agendavalenciana.model.Concert
import com.example.agendavalenciana.model.CulturalCategory
import com.example.agendavalenciana.BuildConfig
import com.example.agendavalenciana.data.FestivalDayVisibility
import com.example.agendavalenciana.state.AgendaState
import com.example.agendavalenciana.data.EventReminderScheduler
import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FestivalBrandOrange = Color(0xFFFEAE06)
private val FestivalSoftOrange = Color(0xFFFFF7E6)
private val FestivalHairline = Color(0xFFE7E7E7)

private enum class FestivalPopupTab(val label: String) {
    ARTISTS("Artistes"),
    INFO("Info"),
    TICKETS("Entrades"), // Accessible des del botó d'Entrades superior, no com a pestanya.
    LOCATION("Ubicació"),
    COMMUNITY("Comunitat")
}

/**
 * Fitxa emergent comuna: un únic disseny amb dos comportaments.
 * Els festivals conserven l'horari i el selector de jornades reals.
 * Els esdeveniments habituals tenen horari general, sense navegar per dies.
 * El diàleg se superposa al feed i mai canvia la seua posició.
 */
@Composable
internal fun FestivalPopupDialog(
    state: AgendaState,
    concert: Concert,
    onDismiss: () -> Unit,
    onRequireLogin: () -> Unit = {},
    focusedArtist: String? = null,
    focusedTime: String? = null,
    focusedCommunityGroupId: Int? = null,
    onOpenSpecialEvent: ((Concert) -> Unit)? = null,
    onOpenArtist: ((String) -> Unit)? = null
) {
    val isFestival = concert.isFestivalExperience
    val context = LocalContext.current
    var pendingExternalTicketUrl by remember(concert.id) { mutableStateOf<String?>(null) }
    var showExternalMapsConfirmation by remember(concert.id) { mutableStateOf(false) }
    var selectedMapDestination by remember(concert.id) { mutableStateOf<String?>(null) }
    var showTicketDialog by remember(concert.id) { mutableStateOf(false) }
    var showCreateBatec by remember(concert.id) { mutableStateOf(false) }
    var showPosterDialog by remember(concert.id) { mutableStateOf(false) }
    var showReminderDialog by remember(concert.id) { mutableStateOf(false) }
    var reminderPermissionMessage by remember(concert.id) { mutableStateOf<String?>(null) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) EventReminderScheduler.setAndroidEnabled(context, false)
        reminderPermissionMessage = if (granted) null else
            "Recordatori guardat. Per rebre avisos fora de l’app, activa les notificacions d’Android en els ajustos."
    }
    LaunchedEffect(concert.id, state.currentAccount?.serverUserId) {
        if (state.currentAccount != null) state.refreshBatecEligibility(concert.id)
    }
    LaunchedEffect(concert.id) {
        while (true) {
            state.refreshSocialCounts(listOf(concert.id))
            delay(20_000)
        }
    }
    var scheduleLoading by remember(concert.id) { mutableStateOf(true) }
    var scheduleLoadError by remember(concert.id) { mutableStateOf<String?>(null) }
    LaunchedEffect(concert.id) {
        scheduleLoading = true
        scheduleLoadError = null
        state.refreshSpecialEvent(concert.id) { ok, error ->
            scheduleLoading = false
            scheduleLoadError = if (ok) null else error
        }
        delay(12_000)
        if (scheduleLoading) {
            scheduleLoading = false
            scheduleLoadError = "TIMEOUT"
        }
    }

    val localConfig = specialEventConfigFor(concert)
    val remoteConfig = state.specialEventRemote(concert.id)?.let { remoteSpecialConfig(it, concert) }
    val config = remember(remoteConfig, localConfig, concert) {
        mergeFestivalConfig(concert, remoteConfig, localConfig)
    }

    // Les pestanyes de dies només naixen de jornades reals del schedule.
    // Mai expandim event_date..end_date: una obra, un cicle o un festival dispers
    // no té activitat necessàriament tots els dies del rang.
    // Revisem la data mentre el popup està obert, també si travessa mitjanit.
    // L'Agenda considera que la jornada cultural acaba a les 05:00 del dia següent.
    var visibleToday by remember(concert.id) { mutableStateOf(state.agendaToday().toString()) }
    LaunchedEffect(concert.id) {
        while (true) {
            visibleToday = state.agendaToday().toString()
            delay(60_000)
        }
    }
    val allDates = remember(config.schedule, concert) {
        // Combinar els horaris remots i les dates auxiliars: cap font parcial
        // no ha d'ocultar la jornada que l'usuari acaba de tocar al feed.
        val scheduleDays = config.schedule.map { it.date.take(10) }.filter(String::isNotBlank)
        (scheduleDays + FestivalFeedNextDay.scheduledDays(concert).map { it.toString() })
            .distinct().sorted()
    }
    val dates = remember(allDates, visibleToday) { FestivalDayVisibility.upcomingDays(allDates, visibleToday) }
    val festivalFinished = allDates.isNotEmpty() && allDates.all { FestivalDayVisibility.isPastDay(it, visibleToday) }

    val preferredDate = sequenceOf(concert.selectedDate, concert.feedOccurrenceDate, concert.feedDate)
        .mapNotNull { it?.takeIf(String::isNotBlank)?.take(10) }
        .firstOrNull { it in dates }
        ?: dates.firstOrNull().orEmpty()

    var selectedDate by remember(concert.id, concert.feedDate, dates) { mutableStateOf(preferredDate) }
    var selectedTab by remember(concert.id, concert.feedDate, focusedCommunityGroupId) {
        mutableStateOf(if (focusedCommunityGroupId != null) FestivalPopupTab.COMMUNITY else FestivalPopupTab.ARTISTS)
    }
    LaunchedEffect(focusedArtist, focusedTime, concert.selectedDate, config.schedule) {
        if (isFestival && !focusedArtist.isNullOrBlank()) {
            val matches = config.schedule.filter { item ->
                festivalArtistKey(item.artistName?.takeIf(String::isNotBlank) ?: item.title) == festivalArtistKey(focusedArtist)
            }
            val requestedDate = concert.selectedDate?.take(10).orEmpty()
            val target = matches.firstOrNull { item ->
                requestedDate.isNotBlank() && item.date.take(10) == requestedDate &&
                    (focusedTime.isNullOrBlank() || item.time == focusedTime)
            } ?: matches.firstOrNull { item -> focusedTime?.takeIf(String::isNotBlank) == item.time }
                ?: matches.firstOrNull()
            val matchDate = target?.date?.take(10)
            if (!matchDate.isNullOrBlank() && matchDate in dates) selectedDate = matchDate
            else if (requestedDate.isNotBlank() && requestedDate in dates) selectedDate = requestedDate
            selectedTab = FestivalPopupTab.ARTISTS
        }
    }
    var showCreateGroup by remember(concert.id) { mutableStateOf(false) }
    var createType by remember(concert.id) { mutableStateOf(com.example.agendavalenciana.model.CommunityGroupType.CAR) }
    var selectedCommunityGroupId by remember(concert.id, focusedCommunityGroupId) {
        mutableStateOf(focusedCommunityGroupId)
    }

    LaunchedEffect(selectedTab, concert.id, state.currentAccount?.serverUserId) {
        if (selectedTab == FestivalPopupTab.COMMUNITY && state.currentAccount != null) {
            state.refreshCommunityGroups(concert.id)
        }
    }

    val placeQuery = concert.eventInfo.address.trim().ifBlank {
        listOf(concert.venue, concert.town).map(String::trim).filter(String::isNotBlank).joinToString(", ")
    }
    val rawMapUrl = concert.eventInfo.mapsUrl.trim()
    val hasMapDestination = placeQuery.isNotBlank() ||
        rawMapUrl.startsWith("https://", ignoreCase = true) || rawMapUrl.startsWith("http://", ignoreCase = true)
    // La pestanya Ubicació demana permís per obrir indicacions externes.
    // Els festivals amb diversos escenaris permeten triar primer la ubicació concreta.
    val selectTab: (FestivalPopupTab) -> Unit = { tab ->
        selectedTab = tab
        if (tab == FestivalPopupTab.LOCATION) {
            val places = if (isFestival) festivalLocations(concert, config.schedule) else emptyList()
            val onePlace = if (isFestival) places.singleOrNull()?.second else placeQuery
            if (!onePlace.isNullOrBlank()) {
                selectedMapDestination = onePlace
                showExternalMapsConfirmation = true
            } else if (!isFestival && hasMapDestination) {
                selectedMapDestination = rawMapUrl
                showExternalMapsConfirmation = true
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // Límit en dp: en tauletes i mòbils grans no ocupem tota la pantalla.
            // En pantalles menudes el contingut continua desplaçable dins de cada pestanya.
            val desiredHeight = when {
                isFestival -> 720.dp
                config.schedule.size > 2 || concert.description.length > 250 -> 640.dp
                else -> 555.dp
            }
            val popupHeight = minOf(desiredHeight, maxHeight * .92f)
            Surface(
                modifier = Modifier.fillMaxWidth(.96f).height(popupHeight),
                shape = RoundedCornerShape(22.dp),
                color = Color.White,
                tonalElevation = 0.dp,
                shadowElevation = 10.dp,
                border = BorderStroke(1.dp, FestivalHairline)
            ) {
                Column(Modifier.fillMaxSize()) {
                    FestivalHeader(
                        state = state,
                        config = config,
                        concert = concert,
                        onDismiss = onDismiss,
                        isFestival = isFestival,
                        onOpenPoster = { showPosterDialog = true },
                        onOpenReminder = {
                            // La campaneta sempre guarda l'esdeveniment en General,
                            // encara que es tanque el diàleg sense programar cap avís.
                            if (concert.id !in state.savedConcerts) state.toggleSaved(concert.id)
                            showReminderDialog = true
                        }
                    )
                    FestivalQuickActions(
                        state = state,
                        concert = concert,
                        isFestival = isFestival,
                        onSave = { state.openSavePicker(concert.id) },
                        onTickets = { selectTab(FestivalPopupTab.TICKETS) }
                    )
                    FestivalTabMenu(selected = selectedTab, isFestival = isFestival, onSelect = selectTab)
                if (isFestival && selectedTab == FestivalPopupTab.ARTISTS && dates.size > 1) {
                    FestivalDaySelector(
                        dates = dates,
                        labels = config.dayLabels,
                        selectedDate = selectedDate,
                        onSelect = { date -> selectedDate = date }
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (selectedTab) {
                        FestivalPopupTab.ARTISTS -> if (isFestival) {
                            FestivalScheduleContent(state, concert, config, selectedDate, scheduleLoading, scheduleLoadError, focusedArtist, focusedTime, onOpenArtist, visibleToday, festivalFinished)
                        } else {
                            FestivalArtistsContent(state = state, concert = concert, config = config, onOpenArtist = onOpenArtist)
                        }
                        FestivalPopupTab.INFO -> FestivalInfoContent(
                            state = state, concert = concert, config = config, isFestival = isFestival
                        )
                        FestivalPopupTab.TICKETS -> FestivalTicketsContent(
                            state = state,
                            concert = concert,
                            config = config,
                            onPurchaseInternal = {
                                if (state.currentAccount == null) onRequireLogin() else showTicketDialog = true
                            },
                            onOpenExternalUrl = { url -> pendingExternalTicketUrl = url },
                            minVisibleDate = if (isFestival) visibleToday else ""
                        )
                        FestivalPopupTab.LOCATION -> if (isFestival) {
                            FestivalLocationsContent(concert = concert, schedule = config.schedule, onOpenLocation = { destination ->
                                selectedMapDestination = destination
                                showExternalMapsConfirmation = true
                            })
                        } else {
                            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Ubicació", fontWeight = FontWeight.Black, fontSize = 18.sp)
                                if (hasMapDestination) {
                                    UniversalEventInfo(
                                        concert = concert,
                                        festivalStyle = true,
                                        showSchedule = false,
                                        showLocation = true,
                                        showExtra = false,
                                        showMapButton = false
                                    )
                                    Button(onClick = {
                                        selectedMapDestination = placeQuery.ifBlank { rawMapUrl }
                                        showExternalMapsConfirmation = true
                                    }, modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Outlined.LocationOn, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(7.dp))
                                        Text("Com arribar")
                                    }
                                } else {
                                    Text("Ubicació pendent de confirmar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        FestivalPopupTab.COMMUNITY -> FestivalCommunityContent(
                            state = state,
                            concert = concert,
                            isFestival = isFestival,
                            onCreate = { selected ->
                                if (state.currentAccount == null) onRequireLogin() else {
                                    createType = selected
                                    showCreateGroup = true
                                }
                            },
                            onOpen = { selectedCommunityGroupId = it }
                        )
                    }
                }
                }
            }
        }
    }

    pendingExternalTicketUrl?.let { url ->
        AlertDialog(
            onDismissRequest = { pendingExternalTicketUrl = null },
            title = { Text("Obrir taquilla externa?") },
            text = { Text("La compra o reserva es farà fora de l’Agenda, a la pàgina de la taquilla de l’esdeveniment.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingExternalTicketUrl = null
                    state.recordTicketClick(concert.id)
                    openTicketUrl(context, url)
                }) { Text("Obrir taquilla") }
            },
            dismissButton = { TextButton(onClick = { pendingExternalTicketUrl = null }) { Text("Cancel·lar") } }
        )
    }
    if (showExternalMapsConfirmation) {
        AlertDialog(
            onDismissRequest = { showExternalMapsConfirmation = false },
            title = { Text("Obrir les indicacions?") },
            text = { Text("Vols obrir la ruta en el navegador o en una aplicació de mapes del mòbil?") },
            confirmButton = {
                TextButton(onClick = {
                    showExternalMapsConfirmation = false
                    val target = selectedMapDestination.orEmpty().ifBlank { placeQuery }
                    if (target.isNotBlank()) {
                        val destinationUrl = if (target.startsWith("https://", true) || target.startsWith("http://", true)) {
                            target
                        } else {
                            "https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(target)}"
                        }
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(destinationUrl))
                        runCatching { context.startActivity(Intent.createChooser(intent, "Obrir indicacions amb")) }
                            .onFailure { openTicketUrl(context, destinationUrl) }
                    }
                    selectedMapDestination = null
                }) { Text("Obrir indicacions") }
            },
            dismissButton = { TextButton(onClick = {
                showExternalMapsConfirmation = false
                selectedMapDestination = null
            }) { Text("Cancel·lar") } }
        )
    }

    if (showReminderDialog) {
        EventReminderDialog(
            state = state,
            concert = concert,
            onDismiss = { showReminderDialog = false },
            onSaved = { epochMillis, customTime ->
                if (concert.id !in state.savedConcerts) state.toggleSaved(concert.id)
                if (concert.id !in state.alertConcerts) state.toggleConcertAlert(concert.id)
                EventReminderScheduler.save(context, concert.id, concert.title, concert.town, epochMillis, customTime)
                state.reminderConfigurationChanged()
                showReminderDialog = false
                Toast.makeText(context, "Configuració del recordatori guardada.", Toast.LENGTH_SHORT).show()
                if (EventReminderScheduler.isAndroidEnabled(context) && Build.VERSION.SDK_INT >= 33 &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                else reminderPermissionMessage = null
            },
            onRemove = {
                if (concert.id in state.alertConcerts) state.toggleConcertAlert(concert.id)
                EventReminderScheduler.cancel(context, concert.id)
                state.reminderConfigurationChanged()
                showReminderDialog = false
                reminderPermissionMessage = null
                Toast.makeText(context, "Recordatori desactivat.", Toast.LENGTH_SHORT).show()
            }
        )
    }
    reminderPermissionMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { reminderPermissionMessage = null },
            title = { Text("Notificacions Android") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { reminderPermissionMessage = null }) { Text("D’acord") } }
        )
    }

    if (showPosterDialog) {
        val posterUri = (listOf(config.heroImageUri) + concert.imageCandidates() + listOf(concert.resolvedCardImageUri))
            .firstOrNull { !it.isNullOrBlank() }
        if (posterUri != null) {
            EventPosterViewerDialog(
                posterUri = posterUri,
                title = concert.title,
                onDismiss = { showPosterDialog = false }
            )
        }
    }

    if (showTicketDialog) {
        TicketPurchaseDialog(
            state = state,
            concert = concert,
            onDismiss = { showTicketDialog = false },
            onPurchased = { showTicketDialog = false }
        )
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
            initialType = createType,
            onCreated = {
                showCreateGroup = false
                state.refreshCommunityGroups(concert.id)
            }
        )
    }

    selectedCommunityGroupId?.let { groupId ->
        CommunityChatDialog(
            state = state,
            groupId = groupId,
            onDismiss = { selectedCommunityGroupId = null }
        )
    }
}

private fun festivalFallbackSchedule(concert: Concert): List<SpecialScheduleItem> = buildList {
    if (concert.scheduleStatus.equals("MISSING", ignoreCase = true)) return@buildList
    val perDay = concert.scheduleDaySummaries.toSortedMap()
    if (perDay.isNotEmpty()) {
        perDay.forEach { (date, summary) ->
            val artists = summary.split('·').map(String::trim).filter(String::isNotBlank)
                .distinctBy { it.lowercase() }
            artists.forEachIndexed { index, artist ->
                add(
                    SpecialScheduleItem(
                        time = if (index == 0) concert.scheduleDayStartTimes[date].orEmpty() else "",
                        title = artist,
                        location = concert.venue,
                        details = if (index == 0 && concert.scheduleDayStartTimes[date].isNullOrBlank()) "Horari pendent" else "",
                        artistName = artist,
                        date = date,
                        sortOrder = (index + 1) * 10
                    )
                )
            }
        }
        return@buildList
    }

    // Festival d'un sol dia: si encara no hi ha schedule detallat, almenys mostrem el cartell.
    // En multijornada no repartim artistes inventant dies: esperem el schedule del servidor.
    if (concert.scheduleDates.distinct().size <= 1) {
        val date = concert.feedDate.takeIf(String::isNotBlank) ?: concert.date.take(10)
        concert.lineup.map(String::trim).filter(String::isNotBlank)
            .distinctBy { it.lowercase() }
            .forEachIndexed { index, artist ->
                add(
                    SpecialScheduleItem(
                        time = if (index == 0) concert.time else "",
                        title = artist,
                        location = concert.venue,
                        details = if (index == 0 && concert.time.isBlank()) "Horari pendent" else "",
                        artistName = artist,
                        date = date,
                        sortOrder = (index + 1) * 10
                    )
                )
            }
    }
}

internal fun mergeFestivalConfig(
    concert: Concert,
    remote: SpecialEventConfig?,
    local: SpecialEventConfig?
): SpecialEventConfig {
    val fallbackSchedule = if (concert.isFestivalExperience) festivalFallbackSchedule(concert) else emptyList()
    if (remote != null && local != null) {
        return remote.copy(
            displayTitle = local.displayTitle.ifBlank { remote.displayTitle },
            heroImageUri = remote.heroImageUri ?: local.heroImageUri,
            subtitle = local.subtitle.ifBlank { remote.subtitle },
            dateLabel = local.dateLabel.ifBlank { remote.dateLabel },
            venueLabel = remote.venueLabel.ifBlank { local.venueLabel },
            styles = local.styles.ifEmpty { remote.styles },
            infoGeneral = local.infoGeneral.ifEmpty { remote.infoGeneral },
            schedule = remote.schedule.ifEmpty { local.schedule.ifEmpty { fallbackSchedule } },
            dayLabels = local.dayLabels,
            festivalTickets = (remote.festivalTickets + local.festivalTickets)
                .distinctBy { listOf(it.name.trim().lowercase(), it.date.take(10), it.url.trim(), it.priceLabel.trim()).joinToString("|") }
        )
    }
    if (remote != null) return remote.copy(schedule = remote.schedule.ifEmpty { fallbackSchedule })
    if (local != null) return local.copy(schedule = local.schedule.ifEmpty { fallbackSchedule })

    val dateLabel = when {
        concert.endDate.isNotBlank() && concert.endDate.take(10) != concert.date.take(10) ->
            "${formatFestivalDate(concert.date)} – ${formatFestivalDate(concert.endDate)}"
        else -> formatFestivalDate(concert.date)
    }
    val info = buildList {
        concert.description.trim().takeIf(String::isNotBlank)?.let {
            add((if (concert.isFestivalExperience) "Festival" else "Descripció") to it)
        }
        if (concert.isFestivalExperience) {
            add("Ubicació" to listOf(concert.venue, concert.town, concert.comarca).filter(String::isNotBlank).joinToString(" · "))
            if (concert.lineup.isNotEmpty()) add("Cartell" to concert.lineup.distinct().joinToString(" · "))
        } else {
            val organizer = concert.organizers.filter(String::isNotBlank).distinct().joinToString(" · ")
            if (organizer.isNotBlank()) add("Organització" to organizer)
        }
    }
    return SpecialEventConfig(
        displayTitle = concert.title,
        heroImageUri = concert.posterUri,
        accessTitle = concert.title,
        subtitle = if (concert.isFestivalExperience) "Informació general, entrades, horaris per dies i comunitat."
            else "Informació general, horari, entrades i comunitat.",
        dateLabel = dateLabel,
        venueLabel = listOf(concert.venue, concert.town).filter(String::isNotBlank).joinToString(" · "),
        styles = concert.styles,
        infoGeneral = info,
        schedule = fallbackSchedule,
        services = emptyList(),
        channels = emptyList()
    )
}

/** Resum breu i accions accessibles sense baixar per la fitxa. */
@Composable
private fun FestivalQuickActions(
    state: AgendaState,
    concert: Concert,
    isFestival: Boolean,
    onSave: () -> Unit,
    onTickets: () -> Unit
) {
    val date = concert.feedOccurrenceDate?.take(10)?.takeIf(String::isNotBlank)
        ?: concert.selectedDate?.take(10)?.takeIf(String::isNotBlank)
        ?: concert.date.take(10)
    val time = concert.time.take(5).takeIf(String::isNotBlank)
    val dateLabel = listOfNotNull(
        date.takeIf(String::isNotBlank)?.let(::formatFestivalDate),
        time?.let { "$it h" }
    ).joinToString(" · ").ifBlank { "Horari per confirmar" }
    val priceLabel = when {
        concert.ticketPrice == 0 -> "Gratuït"
        concert.ticketPrice > 0 -> "${concert.ticketPrice} €"
        concert.price == 0 -> "Gratuït"
        concert.price > 0 -> "${concert.price} €"
        else -> "Preu no indicat"
    }
    val saved = concert.id in state.allCollectionSavedIds()
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.CalendarToday, contentDescription = null,
                modifier = Modifier.size(16.dp), tint = Color(0xFF555555))
            Text(if (isFestival && concert.endDate.isNotBlank() && concert.feedOccurrenceDate.isNullOrBlank())
                    "Consulta les jornades" else dateLabel,
                modifier = Modifier.weight(1f), fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1,
                overflow = TextOverflow.Ellipsis, color = Color(0xFF252525))
            Text(priceLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                color = Color(0xFF805800))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onSave, modifier = Modifier.weight(1f).height(44.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (saved) FestivalBrandOrange else Color(0xFF222222))) {
                Icon(if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = null, modifier = Modifier.size(17.dp), tint = Color(0xFF191919))
                Spacer(Modifier.width(6.dp))
                Text(if (saved) "Guardat" else "Guardar", fontSize = 13.sp,
                    fontWeight = FontWeight.Bold, color = Color(0xFF191919))
            }
            Button(onClick = onTickets, modifier = Modifier.weight(1f).height(44.dp),
                contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D1D1D), contentColor = Color.White)) {
                Icon(Icons.Outlined.ConfirmationNumber, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(5.dp))
                Text("Entrades", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Mostrar només dades addicionals reals: res de targetes buides de transport o PMR. */
@Composable
private fun FestivalUsefulDetails(concert: Concert) {
    val info = concert.eventInfo
    Card(shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, FestivalHairline)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("Informació pràctica", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            if (info.access.isNotBlank()) Text("Accés · ${info.access}", fontSize = 13.sp)
            info.transport.filter { it.text.isNotBlank() }.forEach { transport ->
                Text("${transport.type.ifBlank { "Transport" }} · ${transport.text}", fontSize = 13.sp)
            }
            if (info.pmrStatus.uppercase() in setOf("YES", "SI", "SÍ", "PARTIAL", "NO") || info.pmrText.isNotBlank()) {
                val accessibility = when (info.pmrStatus.uppercase()) {
                    "YES", "SI", "SÍ" -> "Accessible"
                    "PARTIAL" -> "Accessibilitat parcial"
                    "NO" -> "No confirmat com accessible"
                    else -> "Accessibilitat"
                }
                Text(listOf(accessibility, info.pmrText).filter(String::isNotBlank).joinToString(" · "), fontSize = 13.sp)
            }
            info.notices.filter(String::isNotBlank).forEach { notice ->
                Text("Avís · $notice", fontSize = 13.sp)
            }
        }
    }
}

/** La descripció no desplaça d'entrada els horaris i la resta de dades importants. */
@Composable
private fun FestivalDescriptionPreview(eventId: Int, description: String) {
    var expanded by remember(eventId) { mutableStateOf(false) }
    Card(shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = FestivalSoftOrange.copy(alpha = .32f)),
        border = BorderStroke(1.dp, FestivalHairline)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Sobre l'esdeveniment", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(description, fontSize = 13.sp, lineHeight = 18.sp,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = if (expanded) TextOverflow.Clip else TextOverflow.Ellipsis,
                color = Color(0xFF303030))
            if (description.length > 155) {
                TextButton(onClick = { expanded = !expanded },
                    contentPadding = PaddingValues(0.dp), modifier = Modifier.heightIn(min = 36.dp)) {
                    Text(if (expanded) "Llegir menys" else "Llegir més", fontSize = 13.sp, color = Color(0xFF805800))
                }
            }
        }
    }
}

/**
 * Un recordatori concret per esdeveniment. No utilitza intervals d'hores predefinits:
 * només dies abans de l'esdeveniment o dia/hora triats per la persona.
 * Els avisos es programen localment, sense exigir registre.
 */
@Composable
private fun EventReminderDialog(
    state: AgendaState,
    concert: Concert,
    onDismiss: () -> Unit,
    onSaved: (Long, Boolean) -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val zone = remember { ZoneId.of("Europe/Madrid") }
    val eventDate = remember(concert.date) {
        runCatching { LocalDate.parse(concert.date.take(10)) }.getOrNull()
    }
    val eventTime = remember(concert.time) {
        runCatching { LocalTime.parse(concert.time.trim().take(5)) }.getOrNull()
    }
    val previouslyScheduled = remember(concert.id) { EventReminderScheduler.get(context, concert.id) }
    var selected by remember(concert.id) {
        mutableStateOf(if (previouslyScheduled != null) "custom" else "1")
    }
    var customDate by remember(concert.id) {
        mutableStateOf(
            previouslyScheduled?.atZone(zone)?.toLocalDate()
                ?: eventDate?.minusDays(1) ?: LocalDate.now(zone)
        )
    }
    var customTime by remember(concert.id) {
        mutableStateOf(previouslyScheduled?.atZone(zone)?.toLocalTime() ?: LocalTime.of(10, 0))
    }
    var errorMessage by remember(concert.id) { mutableStateOf<String?>(null) }
    val isActive = concert.id in state.alertConcerts || previouslyScheduled != null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quan vols que t’avisem?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("L’esdeveniment es guardarà en la llista General.", fontSize = 13.sp)
                listOf("7" to "7 dies abans", "3" to "3 dies abans", "1" to "1 dia abans", "custom" to "Personalitzar dia i hora").forEach { (key, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { selected = key; errorMessage = null },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected == key, onClick = { selected = key; errorMessage = null })
                        Text(label, fontSize = 14.sp)
                    }
                }
                if (selected == "custom") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            val date = customDate
                            DatePickerDialog(context, { _, y, m, d ->
                                customDate = LocalDate.of(y, m + 1, d)
                                errorMessage = null
                            }, date.year, date.monthValue - 1, date.dayOfMonth).show()
                        }, modifier = Modifier.weight(1f)) { Text(customDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), fontSize = 12.sp) }
                        OutlinedButton(onClick = {
                            TimePickerDialog(context, { _, h, m ->
                                customTime = LocalTime.of(h, m)
                                errorMessage = null
                            }, customTime.hour, customTime.minute, true).show()
                        }, modifier = Modifier.weight(1f)) { Text(customTime.format(DateTimeFormatter.ofPattern("HH:mm")), fontSize = 12.sp) }
                    }
                } else {
                    Text("Avís a les 10.00 h del dia seleccionat (hora peninsular).", fontSize = 12.sp, color = Color.Gray)
                }
                errorMessage?.let { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val eventEnd = eventDate?.atTime(eventTime ?: LocalTime.of(23, 59))
                val selectedMoment = if (selected == "custom") LocalDateTime.of(customDate, customTime)
                    else eventDate?.minusDays(selected.toLongOrNull() ?: 1L)?.atTime(10, 0)
                val targetMillis = selectedMoment?.atZone(zone)?.toInstant()?.toEpochMilli()
                errorMessage = when {
                    targetMillis == null -> "No s’ha pogut determinar la data de l’esdeveniment."
                    targetMillis <= System.currentTimeMillis() -> "Tria un dia i una hora futurs."
                    eventEnd != null && selectedMoment != null && !selectedMoment.isBefore(eventEnd) -> "L’avís ha de ser abans de l’esdeveniment."
                    else -> null
                }
                if (errorMessage == null && targetMillis != null) onSaved(targetMillis, selected == "custom")
            }) { Text(if (isActive) "Guardar avís" else "Activar recordatori") }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isActive) TextButton(onClick = onRemove) { Text("Eliminar avís") }
                TextButton(onClick = onDismiss) { Text("Cancel·lar") }
            }
        }
    )
}

@Composable
private fun FestivalHeader(
    state: AgendaState,
    config: SpecialEventConfig,
    concert: Concert,
    onDismiss: () -> Unit,
    isFestival: Boolean,
    onOpenPoster: () -> Unit,
    onOpenReminder: () -> Unit
) {
    val context = LocalContext.current
    val hero = rememberImageBitmapFromUris(
        listOf(config.heroImageUri) + concert.imageCandidates(),
        maxDimension = 900,
        eventId = concert.id,
        knownResolvedUri = concert.resolvedCardImageUri
    )
    // El text de la ubicació pot ocupar tantes línies com necessite.
    // La imatge i el degradat s'ajusten a l'altura real de la capçalera,
    // evitant que es talle el final de l'adreça en mòbils estrets.
    Box(Modifier.fillMaxWidth().heightIn(min = 170.dp)) {
        if (hero != null) {
            Image(
                hero,
                contentDescription = "Ampliar cartell de ${config.displayTitle}",
                modifier = Modifier.matchParentSize().clickable(onClick = onOpenPoster),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                Modifier.matchParentSize().background(
                    Brush.linearGradient(
                        listOf(
                            FestivalBrandOrange.copy(alpha = 0.68f),
                            Color(0xFF262626)
                        )
                    )
                )
            )
        }
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.08f),
                        Color.Black.copy(alpha = 0.48f),
                        Color.Black.copy(alpha = 0.93f)
                    )
                )
            )
        )
        if ((config.heroImageUri?.isNotBlank() == true ||
                    concert.imageCandidates().any { it.isNotBlank() } ||
                    !concert.resolvedCardImageUri.isNullOrBlank())) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onOpenPoster),
                shape = RoundedCornerShape(999.dp),
                color = Color.Black.copy(alpha = .42f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = .30f))
            ) {
                Row(modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("▣", fontSize = 14.sp, color = Color.White)
                    Text("Cartell", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.White)
                }
            }
        }
        Row(
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            IconButton(
                onClick = onOpenReminder,
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White.copy(alpha = .94f), RoundedCornerShape(999.dp))
            ) {
                Icon(
                    if (concert.id in state.alertConcerts) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                    contentDescription = if (concert.id in state.alertConcerts) "Desactivar avisos" else "Activar avisos",
                    tint = if (concert.id in state.alertConcerts) FestivalBrandOrange else Color(0xFF222222)
                )
            }
            IconButton(
                onClick = {
                    state.recordShare(concert.id)
                    EventShareCard.share(context, concert, hero?.asAndroidBitmap())
                },
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White.copy(alpha = .94f), RoundedCornerShape(999.dp))
            ) {
                Icon(Icons.Outlined.Share, contentDescription = "Compartir", tint = Color(0xFF222222))
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White.copy(alpha = .94f), RoundedCornerShape(999.dp))
            ) {
                Icon(Icons.Outlined.Close, contentDescription = "Tancar", tint = Color(0xFF222222))
            }
        }
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                // Reservar espai als botons superiors fins i tot amb adreces llargues.
                .padding(start = 15.dp, end = 15.dp, top = 62.dp, bottom = 12.dp)
        ) {
            Text(if (isFestival) "FESTIVAL" else "ESDEVENIMENT", color = Color.White.copy(alpha = .83f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(config.displayTitle, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(AgendaDateFormat.dateRangeLabel(config.dateLabel), color = FestivalBrandOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            if (config.venueLabel.isNotBlank()) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = .96f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = config.venueLabel,
                        modifier = Modifier.weight(1f),
                        color = Color.White.copy(alpha = .96f),
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Medium,
                        softWrap = true // Cap límit de línies ni punts suspensius.
                    )
                }
            }
        }
    }
}

@Composable
private fun FestivalTabMenu(selected: FestivalPopupTab, isFestival: Boolean, onSelect: (FestivalPopupTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Artistes és la pestanya principal; les entrades mantenen el botó propi superior.
        val tabs = listOf(FestivalPopupTab.ARTISTS, FestivalPopupTab.INFO, FestivalPopupTab.LOCATION, FestivalPopupTab.COMMUNITY)
        tabs.forEach { tab ->
            FestivalTabButton(
                tab = tab,
                active = tab == selected,
                isFestival = isFestival,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(tab) }
            )
        }
    }
}

@Composable
private fun FestivalTabButton(
    tab: FestivalPopupTab,
    active: Boolean,
    isFestival: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(13.dp))
            .clickable(onClick = onClick),
        color = if (active) FestivalSoftOrange else Color.White,
        shape = RoundedCornerShape(13.dp),
        border = BorderStroke(
            1.dp,
            if (active) FestivalBrandOrange.copy(alpha = .72f) else FestivalHairline
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 3.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (tab) {
                FestivalPopupTab.ARTISTS -> Icon(Icons.Outlined.Person, null, modifier = Modifier.size(18.dp), tint = FestivalBrandOrange)
                FestivalPopupTab.INFO -> Icon(Icons.Outlined.Info, null, modifier = Modifier.size(18.dp), tint = FestivalBrandOrange)
                FestivalPopupTab.TICKETS -> Icon(Icons.Outlined.ConfirmationNumber, null, modifier = Modifier.size(18.dp), tint = FestivalBrandOrange)
                FestivalPopupTab.LOCATION -> Icon(Icons.Outlined.LocationOn, null, modifier = Modifier.size(18.dp), tint = FestivalBrandOrange)
                FestivalPopupTab.COMMUNITY -> Icon(Icons.Outlined.Groups, null, modifier = Modifier.size(18.dp), tint = FestivalBrandOrange)
            }
            Spacer(Modifier.height(2.dp))
            Text(
                tab.label,
                fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Les jornades reals són botons consecutius en una única fila.
 * Sense desplegable; si hi ha moltes jornades, la fila es desplaça lateralment.
 */
@Composable
private fun FestivalDaySelector(
    dates: List<String>,
    labels: Map<String, String>,
    selectedDate: String,
    onSelect: (String) -> Unit
) {
    val current = selectedDate.takeIf { it in dates } ?: dates.firstOrNull().orEmpty()
    val dayListState = rememberLazyListState()

    // Quan s'obri des d'una actuació d'una jornada avançada,
    // la jornada seleccionada també ha d'aparéixer a la vista.
    LaunchedEffect(current, dates) {
        val index = dates.indexOf(current)
        if (index >= 0 && dayListState.layoutInfo.visibleItemsInfo.none { it.index == index }) {
            dayListState.scrollToItem(index)
        }
    }

    LazyRow(
        state = dayListState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(dates, key = { it }) { date ->
            val active = date == current
            val dayName = labels[date].orEmpty().trim()
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(11.dp))
                    .clickable { onSelect(date) },
                shape = RoundedCornerShape(11.dp),
                color = if (active) FestivalSoftOrange else Color.White,
                border = BorderStroke(
                    1.dp,
                    if (active) FestivalBrandOrange else FestivalHairline
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = if (dayName.isBlank()) 10.dp else 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        formatFestivalDay(date),
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (active) Color(0xFF8A5B00) else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    if (dayName.isNotBlank()) {
                        Text(
                            dayName,
                            fontSize = 10.sp,
                            color = if (active) Color(0xFF8A5B00) else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 150.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Fitxa de perfil dels artistes per als esdeveniments NO festivals.
 * Mateix esquema que al cercador: foto pública, nom i Seguir / Seguint.
 * Una activitat no es dibuixa mai amb este component.
 */
@Composable
private fun OrdinaryEventArtistCard(
    state: AgendaState,
    artist: String,
    onOpenArtist: ((String) -> Unit)?,
    fallbackImageUri: String? = null
) {
    val photo = rememberImageBitmapFromUris(
        listOf(state.publicPhotoForArtist(artist), fallbackImageUri),
        maxDimension = 200
    )
    val followed = state.followedArtists.any { festivalArtistKey(it) == festivalArtistKey(artist) }
    val openProfile = Modifier.then(
        if (onOpenArtist != null) Modifier.clickable { onOpenArtist(artist) } else Modifier
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(13.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(12.dp))
                    .background(FestivalSoftOrange).then(openProfile),
                contentAlignment = Alignment.Center
            ) {
                if (photo != null) {
                    Image(
                        photo, contentDescription = "Foto de $artist",
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                    )
                } else {
                    Text(artist.take(1).uppercase(), fontSize = 20.sp,
                        fontWeight = FontWeight.Black, color = Color(0xFF8A5B00))
                }
            }
            Text(
                artist, modifier = Modifier.weight(1f).then(openProfile),
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            TextButton(onClick = { state.toggleFollowArtist(artist) }) {
                Text(if (followed) "✓ Seguint" else "+ Seguir", fontSize = 12.sp)
            }
        }
    }
}

/** Informació editorial i pràctica, sense repetir artistes ni data/hora de la capçalera. */
@Composable
private fun FestivalInfoContent(
    state: AgendaState,
    concert: Concert,
    config: SpecialEventConfig,
    isFestival: Boolean
) {
    val extraRows = remember(config.infoGeneral, concert.description) {
        config.infoGeneral.filter { (title, value) ->
            val key = title.trim().lowercase()
            val isArtistOrSchedule = listOf(
                "artista", "cartell", "lineup", "programació", "programacion", "horari", "hora", "data", "dates", "actuacions"
            ).any { key.contains(it) }
            val duplicateDescription = (key == "descripció" || key == "festival" || key == "descripción") &&
                concert.description.isNotBlank() && value.trim() == concert.description.trim()
            !isArtistOrSchedule && !duplicateDescription && value.isNotBlank()
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (concert.description.isNotBlank()) {
            item { FestivalDescriptionPreview(concert.id, concert.description) }
        }
        if (isFestival) {
            item {
                UniversalEventInfo(
                    concert = concert, festivalStyle = true,
                    showSchedule = false, showLocation = true,
                    showExtra = true, showMapButton = false
                )
            }
        } else {
            if (concert.subcategoryCodes.isNotEmpty() || concert.isInfantil) {
                item {
                    UniversalEventInfo(
                        concert = concert, showSchedule = false,
                        showLocation = false, showExtra = false,
                        showMapButton = false
                    )
                }
            }
            if (concert.eventInfo.let { info ->
                    info.access.isNotBlank() || info.transport.any { it.text.isNotBlank() } ||
                        info.pmrText.isNotBlank() || info.pmrStatus.uppercase() in setOf("YES", "SI", "SÍ", "PARTIAL", "NO") ||
                        info.notices.any(String::isNotBlank)
                }) {
                item { FestivalUsefulDetails(concert) }
            }
        }
        if (isFestival && config.styles.isNotEmpty()) {
            item { Text(config.styles.joinToString(" · "), fontSize = 12.sp, color = FestivalBrandOrange, fontWeight = FontWeight.Bold) }
        }
        items(extraRows) { (title, value) ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, FestivalHairline)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(value, fontSize = 13.sp, lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .80f))
                }
            }
        }
        item { EventReportMenuButton(state = state, concert = concert, compact = false) }
    }
}

/** Artistes, fotografies i seguiment. En els festivals es conserva la programació per jornades. */
@Composable
private fun FestivalArtistsContent(
    state: AgendaState,
    concert: Concert,
    config: SpecialEventConfig,
    onOpenArtist: ((String) -> Unit)?
) {
    val linkedArtists = remember(concert, config.schedule) {
        ordinaryEventArtists(
            title = concert.title,
            primaryArtist = concert.artist,
            lineup = concert.lineup,
            organizers = concert.organizers,
            isStageEvent = concert.category in setOf(
                CulturalCategory.MUSIC, CulturalCategory.THEATRE,
                CulturalCategory.DANCE, CulturalCategory.POPULAR_CULTURE
            ),
            scheduledArtists = config.schedule.mapNotNull { it.artistName }
        )
    }
    val scheduledItems = remember(config.schedule) {
        config.schedule.sortedWith(compareBy<SpecialScheduleItem>({ it.date }, { it.time }, { it.sortOrder }))
    }
    val otherArtists = linkedArtists.filter { artist ->
        scheduledItems.none { session ->
            festivalArtistKey(session.artistName.orEmpty()) == festivalArtistKey(artist) ||
                festivalArtistKey(session.title) == festivalArtistKey(artist)
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Text("Artistes", fontSize = 18.sp, fontWeight = FontWeight.Black) }
        if (scheduledItems.isNotEmpty()) {
            items(scheduledItems) { session ->
                val artistName = linkedArtists.firstOrNull { artist ->
                    festivalArtistKey(artist) == festivalArtistKey(session.artistName.orEmpty()) ||
                        festivalArtistKey(artist) == festivalArtistKey(session.title)
                }
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, FestivalHairline)
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val whenText = listOfNotNull(
                            session.date.takeIf(String::isNotBlank)?.let(::formatFestivalDate),
                            session.time.takeIf(String::isNotBlank)?.let { "$it h" },
                            session.endTime?.takeIf(String::isNotBlank)?.let { "fins a $it h" }
                        ).joinToString(" · ")
                        if (whenText.isNotBlank()) Text(whenText, fontSize = 12.sp, color = FestivalBrandOrange, fontWeight = FontWeight.SemiBold)
                        if (artistName != null) {
                            if (festivalArtistKey(artistName) != festivalArtistKey(session.title)) {
                                Text(session.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                            OrdinaryEventArtistCard(
                                state = state, artist = artistName, onOpenArtist = onOpenArtist,
                                fallbackImageUri = session.imageUri
                            )
                        } else {
                            Text(session.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        if (session.details.isNotBlank()) Text(session.details, fontSize = 12.sp)
                    }
                }
            }
        }
        val artistsToList = if (scheduledItems.isEmpty()) linkedArtists else otherArtists
        if (artistsToList.isNotEmpty()) {
            items(artistsToList) { artist ->
                OrdinaryEventArtistCard(
                    state = state, artist = artist, onOpenArtist = onOpenArtist,
                    fallbackImageUri = concert.artistImageUri.takeIf {
                        festivalArtistKey(artist) == festivalArtistKey(concert.artist)
                    }
                )
            }
        }
        if (scheduledItems.isEmpty() && artistsToList.isEmpty()) {
            item { Text("No hi ha artistes vinculats a esta activitat.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) }
        }
    }
}

@Composable
private fun FestivalTicketsContent(
    state: AgendaState,
    concert: Concert,
    config: SpecialEventConfig,
    onPurchaseInternal: () -> Unit,
    onOpenExternalUrl: (String) -> Unit,
    minVisibleDate: String = ""
) {
    val configured = config.festivalTickets.filter { ticket ->
        minVisibleDate.isBlank() || ticket.date.isBlank() || !FestivalDayVisibility.isPastDay(ticket.date, minVisibleDate)
    }.sortedWith(compareBy<FestivalTicketInfo> { it.date.isNotBlank() }.thenBy { it.date }.thenBy { it.name })
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Text("Entrades", fontWeight = FontWeight.Black, fontSize = 20.sp) }
        if (configured.isNotEmpty()) {
            items(configured) { ticket ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, FestivalHairline)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f)) {
                                Text(ticket.name, fontWeight = FontWeight.Black)
                                Text(if (ticket.date.isBlank()) "Abonament / entrada general" else formatFestivalDate(ticket.date),
                                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(ticket.priceLabel, fontWeight = FontWeight.Black, color = FestivalBrandOrange)
                        }
                        ticket.details.takeIf(String::isNotBlank)?.let { Spacer(Modifier.height(6.dp)); Text(it, fontSize = 12.sp, lineHeight = 17.sp) }
                        ticket.url.takeIf(String::isNotBlank)?.let { url ->
                            Spacer(Modifier.height(8.dp))
                            Surface(
                                modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable {
                                    onOpenExternalUrl(url)
                                },
                                color = FestivalSoftOrange,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, FestivalBrandOrange.copy(alpha = .65f))
                            ) {
                                Text("Comprar", modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
        val upcomingOptions = concert.ticketOptions.filter { option ->
            option.active && (minVisibleDate.isBlank() || option.sessionDate.isBlank() ||
                !FestivalDayVisibility.isPastDay(option.sessionDate, minVisibleDate))
        }
        if (upcomingOptions.isNotEmpty()) {
            items(upcomingOptions.sortedWith(compareBy<com.example.agendavalenciana.model.TicketOption>({ it.sessionDate.isNotBlank() }, { it.sessionDate }, { it.sessionTime }))) { option ->
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, FestivalHairline)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(option.name, fontWeight = FontWeight.Bold)
                            val detail = listOf(option.sessionDate.takeIf(String::isNotBlank) ?: "Entrada general", option.sessionTime, option.sector, option.offer).filter(String::isNotBlank).joinToString(" · ")
                            if (detail.isNotBlank()) Text(detail, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(if (option.price == 0) "Gratuït" else "${option.price} €", fontWeight = FontWeight.Black, color = FestivalBrandOrange)
                    }
                }
            }
        }
        if (configured.isEmpty() && upcomingOptions.isEmpty() && minVisibleDate.isBlank()) {
            item {
                val label = when {
                    concert.ticketPrice == 0 -> "Gratuït"
                    concert.ticketPrice > 0 -> "Des de ${concert.ticketPrice} €"
                    else -> "Consulta disponibilitat"
                }
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, FestivalHairline)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Entrada", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(label, fontWeight = FontWeight.Black, color = FestivalBrandOrange)
                    }
                }
            }
        }
        if (concert.ticketUrl.isNotBlank() && configured.none { it.url == concert.ticketUrl }) {
            item {
                Button(onClick = { onOpenExternalUrl(concert.ticketUrl) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Taquilla general / abonaments")
                }
            }
        }
        if (concert.ticketAvailable && concert.ticketUrl.isBlank()) {
            item {
                Button(
                    onClick = onPurchaseInternal,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (concert.isFree) "Reservar entrada gratuïta" else "Comprar entrades") }
            }
        }
    }
}

@Composable
private fun StandardEventScheduleContent(concert: Concert, config: SpecialEventConfig) {
    // Esdeveniments normals: mostrar hores i sessions publicades sense selector de dies.
    // Mai inventar activitats o dies entre date i endDate.
    val rows = config.schedule.sortedWith(compareBy<SpecialScheduleItem> { it.date }.thenBy { it.time }.thenBy { it.sortOrder })
    val dailyStarts = concert.eventInfo.dailyStart
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Text("Horari", fontWeight = FontWeight.Black, fontSize = 20.sp) }
        if (rows.isNotEmpty()) {
            items(rows) { row ->
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, FestivalHairline)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        val whenLabel = listOfNotNull(
                            row.date.takeIf { it.isNotBlank() }?.let(::formatFestivalDate),
                            row.time.takeIf { it.isNotBlank() },
                            row.endTime?.takeIf { it.isNotBlank() }?.let { "– $it" }
                        ).joinToString(" · ")
                        if (whenLabel.isNotBlank()) Text(whenLabel, fontWeight = FontWeight.Bold, color = FestivalBrandOrange)
                        Text(row.title, fontWeight = FontWeight.Black)
                        if (row.location.isNotBlank()) Text(row.location, fontSize = 12.sp)
                        if (row.details.isNotBlank()) Text(row.details, fontSize = 12.sp)
                    }
                }
            }
        } else if (dailyStarts.isNotEmpty()) {
            items(dailyStarts) { row ->
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, FestivalHairline)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        if (row.date.isNotBlank()) Text(formatFestivalDate(row.date), fontWeight = FontWeight.Bold)
                        Text(listOf(row.label.ifBlank { "Inici" }, row.time.take(5).ifBlank { "Horari pendent" }).joinToString(" · "), color = FestivalBrandOrange)
                    }
                }
            }
        } else {
            item {
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, FestivalHairline)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(formatFestivalDate(concert.feedDate), fontWeight = FontWeight.Bold)
                        Text(concert.time.takeIf(String::isNotBlank) ?: "Horari pendent de confirmar", color = FestivalBrandOrange)
                        concert.endTime.takeIf(String::isNotBlank)?.let { Text("Finalització · $it", fontSize = 12.sp) }
                    }
                }
            }
        }
    }
}

@Composable
private fun FestivalScheduleContent(
    state: AgendaState,
    concert: Concert,
    config: SpecialEventConfig,
    selectedDate: String,
    scheduleLoading: Boolean,
    scheduleLoadError: String?,
    focusedArtist: String?,
    focusedTime: String?,
    onOpenArtist: ((String) -> Unit)?,
    minVisibleDate: String,
    festivalFinished: Boolean
) {
    var onlyMine by remember(concert.id, selectedDate) { mutableStateOf(false) }
    val selectedSlots = state.selectedFestivalArtists(concert.id)
    val allRows = config.schedule
        .filter { !FestivalDayVisibility.isPastDay(it.date, minVisibleDate) }
        .filter { selectedDate.isBlank() || it.date.take(10) == selectedDate }
        .sortedWith(compareBy<SpecialScheduleItem> { it.sortOrder }.thenBy { it.time })
    val rows = if (onlyMine) {
        allRows.filter { row -> scheduleSelectionKey(row) in selectedSlots }
    } else allRows

    // Obrir un resultat de cerca porta la llista a la fila exacta de l'artista.
    // Comptem també els encapçalaments de grup i els blocs informatius de LazyColumn.
    val focusedRowIndex = if (focusedArtist.isNullOrBlank()) null else run {
        var index = 1 // Únicament la capçalera d'Horaris; sense avís redundant de cerca.
        var previousGroup = ""
        var target: Int? = null
        for (item in rows) {
            val group = item.groupName.trim()
            val isGroupMarker = group.isNotBlank() &&
                item.itemType.equals("activity", ignoreCase = true) &&
                item.title.trim().equals(group, ignoreCase = true)
            if (group.isNotBlank() && !group.equals(previousGroup, ignoreCase = true)) index++
            if (!isGroupMarker) {
                val artistName = item.artistName?.takeIf(String::isNotBlank) ?: item.title
                if (festivalArtistKey(artistName) == festivalArtistKey(focusedArtist) &&
                    (focusedTime.isNullOrBlank() || item.time == focusedTime)) {
                    target = index
                    break
                }
                index++
            }
            previousGroup = group
        }
        target
    }
    val scheduleListState = rememberLazyListState()
    LaunchedEffect(selectedDate, focusedArtist, focusedTime, focusedRowIndex) {
        if (focusedRowIndex != null) scheduleListState.scrollToItem(focusedRowIndex)
    }
    LazyColumn(
        state = scheduleListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (selectedDate.isBlank()) "Programació" else config.dayLabels[selectedDate]?.let { "${formatFestivalDayLong(selectedDate)} · $it" } ?: formatFestivalDayLong(selectedDate),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, FestivalHairline)
                ) {
                    Row(Modifier.padding(2.dp)) {
                        FestivalScheduleFilterButton("Tots", active = !onlyMine) { onlyMine = false }
                        FestivalScheduleFilterButton("Els meus", active = onlyMine) { onlyMine = true }
                    }
                }
            }
        }
        if (rows.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, FestivalHairline)) {
                    Text(
                        if (festivalFinished) "Totes les jornades d'este festival ja han passat."
                        else if (onlyMine) "Encara no has marcat cap actuació d'esta jornada."
                        else if (scheduleLoading) "Carregant programació…"
                        else if (!scheduleLoadError.isNullOrBlank()) "No s'ha pogut carregar la programació. Torna-ho a intentar."
                        else "Programació pendent de publicar per a esta jornada.",
                        modifier = Modifier.padding(14.dp),
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            var previousGroup = ""
            rows.forEach { scheduleItem ->
                val group = scheduleItem.groupName.trim()
                val isGroupMarker = group.isNotBlank() &&
                    scheduleItem.itemType.equals("activity", ignoreCase = true) &&
                    scheduleItem.title.trim().equals(group, ignoreCase = true)

                if (group.isNotBlank() && !group.equals(previousGroup, ignoreCase = true)) {
                    item(key = "group_${selectedDate}_${group}_${scheduleItem.sortOrder}") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 1.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            scheduleItem.time.takeIf { isGroupMarker && it.isNotBlank() }?.let {
                                Text(
                                    it,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = FestivalBrandOrange,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                            Text(group, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        }
                    }
                }

                if (!isGroupMarker) {
                    item(key = "${scheduleItem.date}_${scheduleItem.sortOrder}_${scheduleItem.location}_${scheduleItem.title}") {
                        val selectionKey = scheduleSelectionKey(scheduleItem)
                        val recognizedArtist = festivalScheduledArtist(concert, scheduleItem)
                        val artistName = recognizedArtist ?: scheduleItem.title
                        val selected = selectionKey in selectedSlots
                        val followed = state.followedArtists.any { festivalArtistKey(it) == festivalArtistKey(artistName) }
                        val highlighted = !focusedArtist.isNullOrBlank() && festivalArtistKey(artistName) == festivalArtistKey(focusedArtist) &&
                            (focusedTime.isNullOrBlank() || scheduleItem.time == focusedTime)
                        val artistPhoto = recognizedArtist?.let { state.publicPhotoForArtist(it) }
                        val image = rememberImageBitmapFromUris(listOf(artistPhoto, scheduleItem.imageUri, concert.posterUri), maxDimension = 240)
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(if (highlighted) 2.dp else 1.dp, if (highlighted || selected) FestivalBrandOrange.copy(alpha = .8f) else FestivalHairline)
                        ) {
                            Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (image != null) {
                                    Image(image, contentDescription = scheduleItem.title, modifier = Modifier.size(58.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                                    Spacer(Modifier.width(10.dp))
                                }
                                Column(Modifier.weight(1f)) {
                                    if (scheduleItem.time.isNotBlank() || !scheduleItem.endTime.isNullOrBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            scheduleItem.time.takeIf(String::isNotBlank)?.let {
                                                Text(it, fontWeight = FontWeight.Black, fontSize = 16.sp, color = FestivalBrandOrange)
                                            }
                                            scheduleItem.endTime?.takeIf(String::isNotBlank)?.let { Text(" – $it", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                        }
                                    }
                                    Text(scheduleItem.title, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                        modifier = if (recognizedArtist != null && onOpenArtist != null) {
                                            Modifier.clickable { onOpenArtist(artistName) }
                                        } else Modifier)
                                    recognizedArtist?.takeIf { !it.equals(scheduleItem.title, true) }?.let { Text(it, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                                    scheduleItem.location.takeIf(String::isNotBlank)?.let { Text(it, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                    scheduleItem.details.takeIf(String::isNotBlank)?.let { Text(it, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .72f), maxLines = 3, overflow = TextOverflow.Ellipsis) }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    if (recognizedArtist != null) {
                                        TextButton(onClick = { state.toggleFollowArtist(artistName) }) {
                                            Text(if (followed) "✓ Seguint" else "+ Seguir", fontSize = 11.sp)
                                        }
                                    }
                                    TextButton(onClick = { toggleFestivalScheduleItem(state, concert.id, scheduleItem) }) {
                                        Text(if (selected) "✓ Guardat" else "+ Guardar", fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
                previousGroup = group
            }
        }
    }
}

/** Els espais del festival inclouen la seu i tots els escenaris/emplaçaments publicats. */
internal fun festivalLocations(concert: Concert, schedule: List<SpecialScheduleItem>): List<Pair<String, String>> {
    val venue = concert.venue.trim()
    val town = concert.town.trim()
    val address = concert.eventInfo.address.trim()
    val result = linkedMapOf<String, Pair<String, String>>()
    fun add(label: String, query: String) {
        val clean = label.trim()
        val where = query.trim()
        if (clean.isNotBlank() && where.isNotBlank()) {
            result.putIfAbsent(clean.lowercase(), clean to where)
        }
    }
    add(venue.ifBlank { address.ifBlank { town } },
        concert.eventInfo.mapsUrl.trim().takeIf {
            it.startsWith("https://", true) || it.startsWith("http://", true)
        } ?: listOf(address.ifBlank { venue }, town).filter(String::isNotBlank).distinct().joinToString(", "))
    schedule.forEach { row ->
        val location = row.location.trim()
        if (location.isNotBlank() && !location.equals(venue, true)) {
            add(location, listOf(location, venue, town).filter(String::isNotBlank).distinct().joinToString(", "))
        }
    }
    return result.values.toList()
}

@Composable
private fun FestivalLocationsContent(
    concert: Concert,
    schedule: List<SpecialScheduleItem>,
    onOpenLocation: (String) -> Unit
) {
    val locations = remember(concert, schedule) { festivalLocations(concert, schedule) }
    LazyColumn(modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Ubicacions", fontWeight = FontWeight.Black, fontSize = 20.sp) }
        if (locations.isEmpty()) item { Text("Ubicacions pendents de confirmar") }
        items(locations) { (name, search) ->
            Card(shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, FestivalHairline)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(name, fontWeight = FontWeight.Bold)
                        if (search != name && !search.startsWith("http"))
                            Text(search, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { onOpenLocation(search) }) { Text("Com arribar") }
                }
            }
        }
    }
}

@Composable
private fun FestivalScheduleFilterButton(label: String, active: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(999.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (active) FestivalSoftOrange else Color.Transparent
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) Color(0xFF8A5B00) else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Només és perfil seguible si la sessió identifica un artista o coincideix amb el cartell. */
internal fun festivalScheduledArtist(concert: Concert, item: SpecialScheduleItem): String? =
    item.artistName?.trim()?.takeIf(String::isNotBlank)
        ?: concert.lineup.firstOrNull {
            festivalArtistKey(it) != festivalArtistKey(concert.title) &&
                festivalArtistKey(it) == festivalArtistKey(item.title)
        }

internal fun scheduleSelectionKey(item: SpecialScheduleItem): String = listOf(
    item.date.take(10),
    item.time,
    item.location,
    item.artistName.orEmpty().ifBlank { item.title }
).joinToString("|") { it.trim() }

private fun toggleFestivalScheduleItem(state: AgendaState, concertId: Int, item: SpecialScheduleItem) {
    state.toggleFestivalArtist(concertId, scheduleSelectionKey(item))
}

@Composable
private fun FestivalCommunityContent(
    state: AgendaState,
    concert: Concert,
    isFestival: Boolean,
    onCreate: (com.example.agendavalenciana.model.CommunityGroupType) -> Unit,
    onOpen: (Int) -> Unit
) {
    val groups = state.groupsForConcert(concert.id)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (isFestival) "Comunitat del festival" else "Comunitat de l'esdeveniment", fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Text("Cotxe · Transport · Quedades · Allotjament · Altres", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("Taulons", fontSize = 12.sp)
            }
        }
        item {
            CommunityBoardSections(
                state = state,
                concertId = concert.id,
                groups = groups,
                onCreate = onCreate,
                onOpen = { group -> onOpen(group.id) }
            )
        }
    }
}

private fun formatFestivalDay(raw: String): String = AgendaDateFormat.date(raw)

private fun formatFestivalDayLong(raw: String): String = AgendaDateFormat.date(raw)

private fun formatFestivalDate(raw: String): String = AgendaDateFormat.date(raw)
