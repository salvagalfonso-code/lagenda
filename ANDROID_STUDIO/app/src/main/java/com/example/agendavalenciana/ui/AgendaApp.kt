package com.example.agendavalenciana.ui

import com.example.agendavalenciana.model.BellFollowedFrequency
import com.example.agendavalenciana.model.AgendaDateFormat
import com.example.agendavalenciana.model.CategoryTaxonomy

import com.example.agendavalenciana.data.FestivalFeedNextDay
import com.example.agendavalenciana.data.EventReminderScheduler
import android.widget.Toast
import androidx.compose.ui.graphics.vector.ImageVector

import android.app.DatePickerDialog
import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.AlarmOn
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.agendavalenciana.BuildConfig
import com.example.agendavalenciana.auth.GoogleAuthManager
import com.example.agendavalenciana.R
import com.example.agendavalenciana.data.DemoData
import com.example.agendavalenciana.data.ServerApiRepository
import com.example.agendavalenciana.model.AccountRole
import com.example.agendavalenciana.model.AppAccount
import com.example.agendavalenciana.model.AdPlacement
import com.example.agendavalenciana.model.AttendanceStatus
import com.example.agendavalenciana.model.AlertPreferences
import com.example.agendavalenciana.model.AgendaNotification
import com.example.agendavalenciana.model.NotificationType
import com.example.agendavalenciana.model.NotificationGeneralRules
import com.example.agendavalenciana.model.NotificationTabBadgeRules
import com.example.agendavalenciana.model.Concert
import com.example.agendavalenciana.model.ConcertViewMode
import com.example.agendavalenciana.model.CulturalCategory
import com.example.agendavalenciana.model.DiscoveryLevel
import com.example.agendavalenciana.model.PersonalizationOptions
import com.example.agendavalenciana.model.EmailDigestFrequency
import com.example.agendavalenciana.model.Filters
import com.example.agendavalenciana.model.FeedMode
import com.example.agendavalenciana.model.FeedSearchSuggestion
import com.example.agendavalenciana.model.FeedSearchSuggestionType
import com.example.agendavalenciana.model.FollowSuggestion
import com.example.agendavalenciana.model.FollowSuggestionType
import com.example.agendavalenciana.model.EventSubmissionStatus
import com.example.agendavalenciana.model.EventAdvertisement
import com.example.agendavalenciana.model.GlobalAdPlacementPolicy
import com.example.agendavalenciana.model.GroupPost
import com.example.agendavalenciana.model.Story
import com.example.agendavalenciana.model.UserPreferences
import com.example.agendavalenciana.state.AgendaState
import com.example.agendavalenciana.state.ArtistSearchPriority
import java.text.Normalizer
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.collect
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult

private enum class Sheet {
    User, Alerts, Discover, Filters, PublishActions
}

private enum class HomeSection {
    VISUALITZACIO, GUARDATS, ENTRADES, COMUNITAT
}

private fun filtersAnalyticsPayload(filters: Filters): Pair<String, JSONObject> {
    val active = mutableListOf<String>()
    val meta = JSONObject()
    if (filters.categories.isNotEmpty()) {
        active += "DISCIPLINA"
        meta.put("categories", JSONArray(filters.categories.map { it.label }))
    }
    if (filters.dateFrom != null || filters.dateTo != null) {
        active += "DATES"
        meta.put("date_from", filters.dateFrom ?: JSONObject.NULL)
        meta.put("date_to", filters.dateTo ?: JSONObject.NULL)
    }
    filters.month?.let { active += "MES"; meta.put("month", it) }
    filters.comarca?.let { active += "COMARCA"; meta.put("comarca", it) }
    filters.town?.let { active += "POBLE"; meta.put("town", it) }
    filters.maxDistanceKm?.let { active += "DISTANCIA"; meta.put("max_distance_km", it) }
    filters.maxPrice?.let { active += "PREU"; meta.put("max_price", it) }
    if (filters.freeOnly) { active += "GRATUIT"; meta.put("free_only", true) }
    meta.put("active_count", active.size)
    meta.put("filters", JSONArray(active))
    return (if (active.isEmpty()) "SENSE_FILTRES" else active.sorted().joinToString(" + ")) to meta
}

private const val FEED_PAGE_SIZE = 100
private const val FEED_SCROLL_BATCH = 10
private val AgendaBrandOrange = Color(0xFFFEAE06)

@Composable
private fun LanguageSettingsCard(modifier: Modifier = Modifier) {
    val language = LocalUiLanguage.current
    val setLanguage = LocalSetUiLanguage.current
    var expanded by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .35f)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Idioma de l’app", fontSize = 17.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(language.publicLabel, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Text("▾")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    UiLanguage.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.publicLabel) },
                            onClick = {
                                expanded = false
                                setLanguage(option)
                            },
                            trailingIcon = {
                                if (option == language) Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AgendaApp(
    deepLinkUri: String? = null,
    feedSessionEpoch: Int = 0,
    onDeepLinkConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val storedLanguage = remember { UiLanguageStore.read(context.applicationContext) }
    var uiLanguage by remember { mutableStateOf(storedLanguage ?: UiLanguage.VALENCIAN) }

    val setLanguage: (UiLanguage) -> Unit = remember(context) {
        { selected ->
            UiLanguageStore.save(context.applicationContext, selected)
            uiLanguage = selected
        }
    }

    // Primera obertura: valencià per defecte, sense pantalla ni pregunta d'idioma.
    LaunchedEffect(Unit) {
        if (storedLanguage == null) UiLanguageStore.save(context.applicationContext, UiLanguage.VALENCIAN)
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalUiLanguage provides uiLanguage,
        LocalSetUiLanguage provides setLanguage
    ) {
        AgendaAppContent(deepLinkUri = deepLinkUri, feedSessionEpoch = feedSessionEpoch, onDeepLinkConsumed = onDeepLinkConsumed)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AgendaAppContent(
    deepLinkUri: String? = null,
    feedSessionEpoch: Int = 0,
    onDeepLinkConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    // El formulari s'ofereix únicament en instal·lacions noves. En actualitzar
    // des d'una versió existent NO s'interromp una configuració ja feta.
    val firstRunOffer = remember(context) {
        val prefs = context.getSharedPreferences("agenda_valenciana", Context.MODE_PRIVATE)
        !prefs.getBoolean("v294_first_run_done", false) &&
            (prefs.getBoolean("v294_first_run_offered", false) || prefs.all.isEmpty())
    }
    var firstRunVisible by rememberSaveable { mutableStateOf(firstRunOffer) }
    LaunchedEffect(firstRunVisible) {
        if (firstRunVisible) context.getSharedPreferences("agenda_valenciana", Context.MODE_PRIVATE)
            .edit().putBoolean("v294_first_run_offered", true).apply()
    }
    val closeFirstRun: () -> Unit = {
        context.getSharedPreferences("agenda_valenciana", Context.MODE_PRIVATE)
            .edit().putBoolean("v294_first_run_done", true).apply()
        firstRunVisible = false
    }
    val state = remember { AgendaState(context.applicationContext) }
    // v285: els actes sincronitzats mentre l'app roman oberta també entren en el
    // catàleg sense esperar a tancar-la. Es fa sync incremental, mai per tecla.
    LaunchedEffect(state) {
        while (true) {
            delay(180_000L)
            if (!state.isAgendaRefreshing) state.refreshRemoteAgenda()
        }
    }
    // Quan l'app torna després d'una hora inactiva, comença una lectura nova.
    // Mai es reinicia només per haver compartit un esdeveniment o canviat d'app.
    LaunchedEffect(feedSessionEpoch) {
        if (feedSessionEpoch > 0) state.refreshForReadingSession("$feedSessionEpoch:0")
    }

    var sheet by remember { mutableStateOf<Sheet?>(null) }
    var pageStack by remember { mutableStateOf<List<String>>(emptyList()) }
    val page = pageStack.lastOrNull()
    val pushPage: (String) -> Unit = { target ->
        if (pageStack.lastOrNull() != target) pageStack = pageStack + target
    }
    val popPage: () -> Unit = {
        if (pageStack.isNotEmpty()) pageStack = pageStack.dropLast(1)
    }
    val clearPages: () -> Unit = { pageStack = emptyList() }
    var story by remember { mutableStateOf<Story?>(null) }
    var storyViewerStories by remember { mutableStateOf<List<Story>>(emptyList()) }
    // Tancar un Batec ha de tornar al primer element, encara que el Batec vist
    // passe al final de l'ordre. El comptador NO reinicia la sessió de Per a tu.
    var batecsReturnEpoch by remember { mutableStateOf(0) }
    val closeBatecsViewer: () -> Unit = {
        story = null
        storyViewerStories = emptyList()
        batecsReturnEpoch += 1
    }
    var selectedConcert by remember { mutableStateOf<Concert?>(null) }
    var focusedFestivalArtist by remember { mutableStateOf<String?>(null) }
    var focusedFestivalTime by remember { mutableStateOf<String?>(null) }
    var focusedNotificationCommunityGroupId by remember { mutableStateOf<Int?>(null) }
    var selectedArtist by remember { mutableStateOf<String?>(null) }
    var homeSection by remember { mutableStateOf(HomeSection.VISUALITZACIO) }
    var inviteAfterLogin by remember { mutableStateOf<String?>(null) }
    // v317: l'esdeveniment s'ha publicat/guardat ABANS d'oferir l'alta
    // del titular per a la venda interna. Els botons obrin formularis diferents.
    var showInternalSaleAccountDialog by remember { mutableStateOf(false) }
    // Només s'activa en el clic de Publicar/Guardar, mai en obrir l'app.
    var resumeSellerOnboardingAfterAuth by remember { mutableStateOf(false) }
    var saleBillingStatus by remember { mutableStateOf<org.json.JSONObject?>(null) }
    var saleBillingBusy by remember { mutableStateOf(false) }
    LaunchedEffect(showInternalSaleAccountDialog, state.currentAccount?.serverUserId) {
        saleBillingStatus = null
        saleBillingBusy = showInternalSaleAccountDialog && state.currentAccount != null
        if (saleBillingBusy) {
            saleBillingStatus = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                ServerApiRepository.getAuthenticatedJson(context.applicationContext, "seller-onboarding/index.php").getOrNull()
            }
            saleBillingBusy = false
        }
    }
    var specialEventConcert by remember { mutableStateOf<Concert?>(null) }
    var specialEventParentConcert by remember { mutableStateOf<Concert?>(null) }
    var focusedTicketConcertId by remember { mutableStateOf<Int?>(null) }
    var focusedFeedConcertId by remember { mutableStateOf<Int?>(null) }
    val globalKeyboardController = LocalSoftwareKeyboardController.current
    val globalFocusManager = LocalFocusManager.current
    val isKeyboardVisible = WindowInsets.isImeVisible

    // El canvi de jornada de l’Agenda és a les 05:00 (Europe/Madrid).
    // Si l’app queda oberta durant la nit, el feed es refresca exactament en creuar el tall.
    LaunchedEffect(state) {
        while (true) {
            delay(state.millisUntilNextAgendaDayBoundary() + 250L)
            state.refreshAgendaDayBoundary()
        }
    }

    // 0.20.8.10cr: qualsevol autenticació correcta acaba sempre a l'arrel d'Inici.
    // No depén de des d'on s'ha obert Login (perfil, detall d'esdeveniment o registre).
    val goHomeAfterAuthentication: () -> Unit = {
        val resumeSale = resumeSellerOnboardingAfterAuth
        resumeSellerOnboardingAfterAuth = false
        sheet = null
        story = null
        storyViewerStories = emptyList()
        selectedConcert = null
        focusedNotificationCommunityGroupId = null
        selectedArtist = null
        specialEventConcert = null
        specialEventParentConcert = null
        focusedTicketConcertId = null
        focusedFeedConcertId = null
        homeSection = HomeSection.VISUALITZACIO
        state.selectDefaultFeedModeForCurrentUser()
        if (state.search.isNotBlank()) state.updateSearch("")
        // La pila de pantalles es buida: autenticació correcta sempre torna a Inici.
        clearPages()
        if (resumeSale) pushPage("seller_billing")
    }

    // v318: una publicacio pendent de vincular MAI bloqueja l'entrada a l'app.
    // Conservar el comprovant local i vincular NOMES quan l'usuari s'autentique
    // voluntariament (des del popup posterior a Publicar/Guardar o des de Perfil).
    LaunchedEffect(state.currentAccount?.serverUserId) {
        if (state.currentAccount != null &&
            ServerApiRepository.pendingGuestClaimCount(context.applicationContext) > 0
        ) {
            state.retryPendingGuestClaims()
        }
    }

    if (showInternalSaleAccountDialog) {
        val guest = state.currentAccount == null
        val existingBilling = saleBillingStatus?.optBoolean("has_billing", false) == true
        AlertDialog(
            onDismissRequest = { showInternalSaleAccountDialog = false },
            title = {
                Text(if (guest) "Per a iniciar el procés de venda:" else
                    if (existingBilling) "Dades de facturació disponibles" else "Dades necessàries per a vendre")
            },
            text = {
                Text(when {
                    guest -> "L’esdeveniment ja és públic. Inicia sessió o crea un compte; després completaràs les dades de facturació i acceptaràs les condicions de venda."
                    saleBillingBusy -> "Comprovant les dades de facturació del teu compte…"
                    existingBilling -> "Ja tens dades de facturació guardades. Continua per seleccionar el titular i configurar les entrades."
                    else -> "L’esdeveniment ja és públic. Per a vendre entrades has d’afegir les dades de facturació i acceptar les condicions de venda, a més de llegir la política de privacitat."
                })
            },
            confirmButton = {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (guest) {
                        Button(
                            onClick = {
                                showInternalSaleAccountDialog = false
                                resumeSellerOnboardingAfterAuth = true
                                pushPage("login")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AgendaBrandOrange)
                        ) { Text("Iniciar sessió", color = Color.Black, fontWeight = FontWeight.Bold) }
                        OutlinedButton(
                            onClick = {
                                showInternalSaleAccountDialog = false
                                resumeSellerOnboardingAfterAuth = true
                                pushPage("intro_register")
                            }, modifier = Modifier.fillMaxWidth()
                        ) { Text("Crear compte", fontWeight = FontWeight.Bold) }
                    } else {
                        Button(
                            enabled = !saleBillingBusy,
                            onClick = {
                                showInternalSaleAccountDialog = false
                                pushPage("seller_billing")
                            }, modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AgendaBrandOrange)
                        ) { Text(if (existingBilling) "Continuar amb la venda" else "Introduir dades de facturació", color = Color.Black) }
                    }
                    TextButton(
                        onClick = { showInternalSaleAccountDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Ara no") }
                }
            }
        )
    }

    state.guestClaimNotice?.let { notice ->
        val failed = notice.contains("pendent(s)") || notice.contains("No s'han pogut")
        AlertDialog(
            onDismissRequest = { state.clearGuestClaimNotice() },
            title = { Text(if (failed) "Falta vincular la publicació" else "Publicació conservada") },
            text = { Text(notice) },
            confirmButton = {
                TextButton(onClick = {
                    state.clearGuestClaimNotice()
                    if (failed) state.retryPendingGuestClaims()
                }) { Text(if (failed) "Reintentar" else "Entés") }
            },
            dismissButton = if (failed) ({ TextButton(onClick = { state.clearGuestClaimNotice() }) { Text("Més tard") } }) else null
        )
    }

    // 0.20.8.10fp: telemetria operativa del Panell. No entra en affinity_events,
    // per tant no modifica els gustos ni el càlcul de "Per a tu".
    LaunchedEffect(state.currentAccount?.serverUserId) {
        if (state.currentAccount?.serverUserId != null) {
            ServerApiRepository.trackUsage(context.applicationContext, "SESSION_START", "APP_OPEN")
            while (true) {
                ServerApiRepository.trackUsage(context.applicationContext, "SESSION_HEARTBEAT", "ACTIVE")
                delay(120_000)
            }
        }
    }

    LaunchedEffect(homeSection, page, state.currentAccount?.serverUserId) {
        if (state.currentAccount?.serverUserId != null) {
            val view = page?.takeIf { it.isNotBlank() } ?: "HOME_${homeSection.name}"
            ServerApiRepository.trackUsage(context.applicationContext, "VIEW_OPEN", view)
        }
    }

    LaunchedEffect(state.feedMode, state.selectedFeedCategory, state.currentAccount?.serverUserId) {
        if (state.currentAccount?.serverUserId != null) {
            val meta = JSONObject()
            state.selectedFeedCategory?.let { meta.put("category", it.label) }
            ServerApiRepository.trackUsage(context.applicationContext, "FEED_MODE", state.feedMode.name, meta)
        }
    }

    LaunchedEffect(state.filters, state.currentAccount?.serverUserId) {
        if (state.currentAccount?.serverUserId != null) {
            val (label, meta) = filtersAnalyticsPayload(state.filters)
            ServerApiRepository.trackUsage(context.applicationContext, "FEED_FILTERS", label, meta)
        }
    }

    var friendsInviteToAccept by remember { mutableStateOf<String?>(null) }
    var listInviteToAccept by remember { mutableStateOf<String?>(null) }
    var listInviteName by remember { mutableStateOf<String?>(null) }
    var listInviteError by remember { mutableStateOf<String?>(null) }
    var listInviteAfterLogin by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(listInviteToAccept) {
        val token = listInviteToAccept ?: return@LaunchedEffect
        listInviteName = null
        listInviteError = null
        val result = withContext(Dispatchers.IO) { ServerApiRepository.publicSharedSavedListName(token) }
        result.onSuccess { listInviteName = it }.onFailure {
            listInviteError = it.message ?: "Esta invitació no està disponible."
        }
    }
    LaunchedEffect(state.currentAccount?.serverUserId, listInviteAfterLogin) {
        if (state.currentAccount != null && listInviteAfterLogin != null) {
            val token = listInviteAfterLogin!!
            listInviteAfterLogin = null
            state.joinSharedSavedList(token) { ok, _ ->
                if (ok) { homeSection = HomeSection.GUARDATS; clearPages() }
            }
        }
    }
    LaunchedEffect(state.currentAccount?.serverUserId, inviteAfterLogin) {
        if (state.currentAccount != null && inviteAfterLogin != null) {
            val token = inviteAfterLogin!!
            inviteAfterLogin = null
            state.joinFriendsCircle(token) { ok, _ -> if (ok) {
                homeSection = HomeSection.COMUNITAT
                clearPages()
            } }
        }
    }
    LaunchedEffect(deepLinkUri) {
        val raw = deepLinkUri?.trim().orEmpty()
        val listInvite = extractSavedListInviteToken(raw)
        val invite = extractFriendsInviteToken(raw)
        if (listInvite != null && raw.contains("://")) {
            listInviteToAccept = listInvite
            onDeepLinkConsumed()
        } else if (invite != null && raw.contains("://")) {
            friendsInviteToAccept = invite
            onDeepLinkConsumed()
        }
    }

    LaunchedEffect(deepLinkUri, state.concerts, state.currentAccount?.username) {
        val raw = deepLinkUri?.trim().orEmpty()
        if (raw.isNotBlank() && extractFriendsInviteToken(raw) == null && extractSavedListInviteToken(raw) == null) {
            val uri = runCatching { android.net.Uri.parse(raw) }.getOrNull()
            val eventId = if (uri?.scheme.equals("agendavalenciana", ignoreCase = true) &&
                uri?.host.equals("event", ignoreCase = true)) {
                uri?.pathSegments?.firstOrNull()?.toIntOrNull()
            } else null
            if (eventId == null) {
                onDeepLinkConsumed()
            } else {
                val linkedConcert = state.concerts.firstOrNull { it.id == eventId }
                if (linkedConcert != null) {
                    homeSection = HomeSection.VISUALITZACIO
                    state.selectFeedMode(FeedMode.FOR_YOU)
                    selectedConcert = linkedConcert
                    onDeepLinkConsumed()
                } else {
                    state.ensureFullAgendaLoaded("deep_link")
                }
            }
        }
    }

    LaunchedEffect(state.currentAccount?.serverUserId) {
        // No competeix amb la primera pintura d'Inici. Entrades locals/QR ja venen de cache.
        delay(15_000)
        state.refreshSpecialAccessibleEvents()
    }

    // Les novetats dels grups d'amics alimenten la campaneta sense refrescar el feed.
    LaunchedEffect(state.currentAccount?.serverUserId) {
        if (state.currentAccount == null) state.refreshSavedCollections()
        else {
            delay(10_000L)
            while (true) {
                state.refreshSavedCollections()
                delay(60_000L)
            }
        }
    }

    // Missatges dels xats de Cotxe/Transport/Quedades/Altres en segon pla.
    LaunchedEffect(state.currentAccount?.serverUserId) {
        if (state.currentAccount == null) {
            state.refreshEventCommunityNotifications() // buida dades d'un compte anterior
        } else {
            delay(25_000L)
            while (true) {
                state.refreshEventCommunityNotifications()
                delay(120_000L)
            }
        }
    }

    // 10cw: la resolució d'un report no depén del feed ni de la campaneta.
    // També funciona com a convidat gràcies al guest_id estable del dispositiu.
    LaunchedEffect(state.currentAccount?.serverUserId) {
        // La campaneta ja fa una primera consulta durant l'arrancada. Este bucle és
        // només el reforç periòdic perquè una resolució nova aparega sense tocar el feed.
        delay(60_000)
        while (true) {
            state.refreshReportResults()
            delay(60_000)
        }
    }

    // 10cx: si una denúncia s'ha descartat, el mateix popup permet explicar-se i
    // demanar una segona revisió sense crear un report duplicat.
    val pendingReportPopup = state.reportResultPopupState.firstOrNull {
        state.reportResultNotificationId(it.reportId, it.reviewRound) !in state.readNotificationIds
    }
    if (pendingReportPopup != null) {
        val dismissed = pendingReportPopup.reviewStatus.equals("DISMISSED", ignoreCase = true)
        var reportFollowupText by remember(pendingReportPopup.reportId, pendingReportPopup.reviewRound) { mutableStateOf("") }
        var reportFollowupSending by remember(pendingReportPopup.reportId, pendingReportPopup.reviewRound) { mutableStateOf(false) }
        var reportFollowupError by remember(pendingReportPopup.reportId, pendingReportPopup.reviewRound) { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { },
            title = { Text(pendingReportPopup.title.ifBlank { if (dismissed) "Hem revisat el teu avís" else "Resultat del teu avís" }) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(pendingReportPopup.message)
                    if (dismissed) {
                        Text(
                            "Creus que ens falta informació? Explica’ns què hauríem de comprovar i ho revisarem de nou.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedTextField(
                            value = reportFollowupText,
                            onValueChange = {
                                reportFollowupText = it.take(2000)
                                reportFollowupError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Explica’ns-ho") },
                            minLines = 3,
                            maxLines = 6,
                            enabled = !reportFollowupSending
                        )
                        reportFollowupError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                if (dismissed) {
                    TextButton(
                        enabled = !reportFollowupSending && reportFollowupText.trim().length >= 5,
                        onClick = {
                            reportFollowupSending = true
                            reportFollowupError = null
                            state.submitReportFollowup(pendingReportPopup, reportFollowupText) { ok, error ->
                                reportFollowupSending = false
                                if (!ok) reportFollowupError = error ?: "No s’ha pogut enviar."
                            }
                        }
                    ) { Text(if (reportFollowupSending) "Enviant…" else "Revisar de nou") }
                } else {
                    TextButton(onClick = {
                        state.markNotificationRead(
                            state.reportResultNotificationId(pendingReportPopup.reportId, pendingReportPopup.reviewRound)
                        )
                    }) { Text("Entés") }
                }
            },
            dismissButton = if (dismissed) {
                {
                    TextButton(onClick = {
                        state.markNotificationRead(
                            state.reportResultNotificationId(pendingReportPopup.reportId, pendingReportPopup.reviewRound)
                        )
                    }) { Text("Entés") }
                }
            } else null
        )
    }

    val hasInternalBackTarget =
        selectedConcert != null || story != null || sheet != null || pageStack.isNotEmpty() ||
            selectedArtist != null || specialEventConcert != null ||
            homeSection != HomeSection.VISUALITZACIO || state.search.isNotBlank()

    BackHandler(enabled = hasInternalBackTarget) {
        when {
            selectedConcert != null -> {
                selectedConcert = null
                focusedNotificationCommunityGroupId = null
            }
            story != null -> closeBatecsViewer()
            sheet != null -> sheet = null
            // El convidat pot eixir del formulari i del login sense forçar el compte.
            page == "login" -> popPage()
            selectedArtist != null -> selectedArtist = null
            specialEventConcert != null -> {
                specialEventConcert = null
                specialEventParentConcert?.let { selectedConcert = it }
                specialEventParentConcert = null
            }
            pageStack.isNotEmpty() -> popPage()
            homeSection != HomeSection.VISUALITZACIO -> homeSection = HomeSection.VISUALITZACIO
            state.search.isNotBlank() -> state.updateSearch("")
        }
    }

    // 0.20.8.10fm: si el teclat està obert, Arrere sempre tanca primer l'IME.
    // Este BackHandler es declara després del de navegació perquè tinga prioritat.
    BackHandler(enabled = isKeyboardVisible) {
        globalFocusManager.clearFocus(force = true)
        globalKeyboardController?.hide()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (selectedArtist == null && page == null) {
                InstagramTopBar(
                    notificationCount = state.unreadNotificationCountState,
                    onUser = { pushPage("profile") },
                    onPublish = { sheet = Sheet.PublishActions },
                    onDiscover = { pushPage("discover") },
                    onAlerts = { sheet = Sheet.Alerts }
                )
            }
        },
        bottomBar = {
            if (selectedArtist == null && page == null) {
                CompactBottomMenu(
                    state = state,
                    selectedSection = homeSection,
                    onSelectSection = { section ->
                        focusedFeedConcertId = null
                        // Canviar de pestanya no regenera Inici. El snapshot i la finestra
                        // visible es reutilitzen tal com estan.
                        if (homeSection != section) homeSection = section
                        if (section == HomeSection.VISUALITZACIO) {
                            // Inici és l'arrel: tanca capes/panells, però no toca l'algoritme
                            // ni programa recàlculs si ja estàvem en Per a tu sense cerca.
                            selectedConcert = null
                            specialEventConcert = null
                            story = null
                            sheet = null
                            // La pestanya elegida a mà continua activa en tornar a Inici.
                            if (state.feedMode == FeedMode.FOR_YOU) {
                                state.selectDefaultFeedModeForCurrentUser()
                            }
                            if (state.search.isNotBlank()) state.updateSearch("")
                        }
                    }
                )
            }
        },
    ) { scaffoldPadding ->
        when {
            // Login és una capa global i pot obrir-se des d’un perfil o un detall.
            // En tornar, la pila conserva la pantalla d’origen.
            page == "intro_register" -> RegistrationScreen(
                state = state,
                onBack = popPage,
                onAuthenticated = goHomeAfterAuthentication,
                accountOnly = true,
                sellerMode = resumeSellerOnboardingAfterAuth
            )
            (page == "login" || page == "profile_login") -> LoginScreen(
                state = state,
                onAuthenticated = goHomeAfterAuthentication,
                onContinueAsGuest = popPage,
                registrationAccountOnly = page == "profile_login"
            )
            page == "seller_billing" -> SellerBillingOnboardingScreen(
                state = state,
                contentPadding = scaffoldPadding,
                onBack = popPage,
                onActivated = {
                    state.applySellerProfessionalRole()
                    popPage()
                    pushPage("tickets_manage")
                }
            )
            page == "new_event" -> NewEventPublisherScreen(
                state = state,
                contentPadding = scaffoldPadding,
                onBack = popPage,
                onCreateAccount = { pushPage("login") },
                onInternalSaleAccountRequired = { showInternalSaleAccountDialog = true }
            )
            page == "edit_events" -> ManagedEventsPublisherScreen(
                state = state,
                contentPadding = scaffoldPadding,
                onBack = popPage,
                onCreateAccount = { pushPage("login") },
                onInternalSaleAccountRequired = { showInternalSaleAccountDialog = true }
            )
            page == "tickets_manage" -> TicketManagementPublisherScreen(
                state = state,
                contentPadding = scaffoldPadding,
                onBack = popPage
            )
            page == "create_ad" -> AdvertisementPublisherScreen(
                state = state,
                contentPadding = scaffoldPadding,
                onBack = popPage,
                onLogin = { pushPage("login") },
                onPublished = {
                    sheet = null
                    story = null
                    storyViewerStories = emptyList()
                    selectedConcert = null
                    selectedArtist = null
                    specialEventConcert = null
                    specialEventParentConcert = null
                    focusedTicketConcertId = null
                    focusedFeedConcertId = null
                    homeSection = HomeSection.VISUALITZACIO
                    state.selectDefaultFeedModeForCurrentUser()
                    if (state.search.isNotBlank()) state.updateSearch("")
                    clearPages()
                }
            )
            page == "artist_commerce" -> ArtistCommerceManagementScreen(
                state = state,
                contentPadding = scaffoldPadding,
                onBack = popPage
            )
            page == "professional_request" -> ProfessionalAccessRequestScreen(
                state = state,
                contentPadding = scaffoldPadding,
                onBack = popPage
            )
            page == "review_publications" -> CommunityPublisherReviewScreen(
                state = state,
                contentPadding = scaffoldPadding,
                onBack = popPage
            )
            page == "review_professional_claims" -> ProfessionalClaimReviewScreen(
                state = state,
                contentPadding = scaffoldPadding,
                onBack = popPage
            )
            selectedArtist != null -> GroupProfileScreen(
                state = state,
                artist = selectedArtist!!,
                contentPadding = scaffoldPadding,
                onBack = { selectedArtist = null },
                onOpenConcert = { event ->
                    focusedFestivalArtist = selectedArtist?.takeIf { event.isFestivalExperience }
                    selectedConcert = event
                }
            )
            page == "profile" -> {
                if (state.currentAccount == null) {
                    GuestAccountScreen(
                        state = state,
                        contentPadding = scaffoldPadding,
                        onBack = popPage,
                        onLogin = { pushPage("profile_login") },
                        onCreateAccount = { pushPage("intro_register") }
                    )
                } else {
                    AccountProfileScreen(
                        state = state,
                        contentPadding = scaffoldPadding,
                        onBack = popPage,
                        onLogout = { clearPages(); state.logout() }
                    )
                }
            }
            page == "discover" -> DiscoverScreen(
                state = state,
                contentPadding = scaffoldPadding,
                onBack = popPage,
                // Mantindre "discover" davall del perfil permet tornar exactament on estàvem.
                onOpenArtist = { selectedArtist = it }
            )
            specialEventConcert != null -> SpecialEventSpaceScreen(
                state = state,
                concert = specialEventConcert!!,
                contentPadding = scaffoldPadding,
                onBack = {
                    specialEventConcert = null
                    specialEventParentConcert?.let { selectedConcert = it }
                    specialEventParentConcert = null
                },
                onOpenArtist = { artist -> selectedArtist = artist }
            )
            else -> HomeFeed(
                state = state,
                feedSessionEpoch = feedSessionEpoch,
                batecsReturnEpoch = batecsReturnEpoch,
                homeSection = homeSection,
                contentPadding = scaffoldPadding,
                onFilter = { sheet = Sheet.Filters },
                onOpenStory = { opened, snapshot ->
                    // El carrusel ja ha calculat els Batecs en segon pla. Conservem eixe
                    // snapshot perquè obrir-ne un no torne a reconstruir activeStories()
                    // en el fil principal i no congele la interfície.
                    storyViewerStories = snapshot
                    story = opened
                },
                onOpenConcert = { concert ->
                    focusedFeedConcertId = null
                    focusedFestivalArtist = null
                    focusedFestivalTime = null
                    selectedConcert = concert
                },
                onOpenFestivalArtist = { concert, artist, time ->
                    focusedFestivalArtist = artist
                    focusedFestivalTime = time
                    selectedConcert = concert
                },
                onOpenAdvertisement = { concert ->
                    // La publicitat obri la mateixa fitxa emergent que qualsevol
                    // targeta, sense reordenar ni desplaçar el feed.
                    focusedTicketConcertId = null
                    focusedFeedConcertId = null
                    story = null
                    sheet = null
                    selectedArtist = null
                    specialEventConcert = null
                    selectedConcert = concert
                },
                onOpenArtist = { selectedArtist = it },
                onOpenSpecialEvent = {
                    specialEventParentConcert = null
                    specialEventConcert = it
                },
                focusedTicketConcertId = focusedTicketConcertId,
                focusedFeedConcertId = focusedFeedConcertId,
                onSearchApplied = {
                    focusedFeedConcertId = null
                    homeSection = HomeSection.VISUALITZACIO
                    selectedArtist = null
                    // v293: en netejar la cerca/filtres, Inici recupera «Per a tu».
                }
            )
        }
    }

    if (firstRunVisible && state.currentAccount == null && deepLinkUri == null) {
        FirstRunOnboardingDialog(
            state = state,
            onSkip = closeFirstRun,
            onLogin = { closeFirstRun(); pushPage("login") },
            onCreateAccount = { closeFirstRun(); pushPage("intro_register") },
            onContinueGuest = closeFirstRun
        )
    }

    when (sheet) {
        Sheet.User -> UserSheet(
            state = state,
            onLogout = {
                sheet = null
                state.logout()
            },
            onDismiss = { sheet = null }
        )
        Sheet.Alerts -> AlertsSheet(
            state = state,
            onOpenEvent = { eventId ->
                // V232: no tanquem la campaneta ni canviem de pantalla. El popup de
                // l'esdeveniment apareix per damunt i en tancar-lo tornem a la mateixa pestanya.
                selectedConcert = state.concertById(eventId)
                focusedFestivalArtist = null
                focusedFestivalTime = null
                focusedNotificationCommunityGroupId = null
            },
            onOpenTickets = { eventId ->
                // També els avisos d'entrades obrin la fitxa sense perdre Notificacions.
                selectedConcert = eventId?.let(state::concertById)
                focusedFestivalArtist = null
                focusedFestivalTime = null
                focusedNotificationCommunityGroupId = null
            },
            onOpenCommunityEvent = { eventId, groupId ->
                selectedConcert = state.concertById(eventId)
                focusedFestivalArtist = null
                focusedFestivalTime = null
                focusedNotificationCommunityGroupId = groupId
            },
            onOpenArtist = { artist ->
                sheet = null
                selectedArtist = artist
            },
            onDismiss = { sheet = null }
        )
        Sheet.Discover -> DiscoverSheet(
            state = state,
            onOpenArtist = { artist -> sheet = null; selectedArtist = artist },
            onDismiss = { sheet = null }
        )
        Sheet.Filters -> FilterSheet(state = state, onDismiss = { sheet = null })
        Sheet.PublishActions -> PublishActionsSheet(
            state = state,
            onNewEvent = {
                sheet = null
                pushPage("new_event")
            },
            onEditEvents = { sheet = null; pushPage("edit_events") },
            onTickets = { sheet = null; pushPage("tickets_manage") },
            onArtistCommerce = { sheet = null; pushPage("artist_commerce") },
            onCreateAd = { sheet = null; pushPage("create_ad") },
            onReviewPublications = { sheet = null; pushPage("review_publications") },
            onReviewProfessionalClaims = { sheet = null; pushPage("review_professional_claims") },
            onProfessionalRequest = {
                sheet = null
                if (state.currentAccount == null) pushPage("login") else pushPage("professional_request")
            },
            onDismiss = { sheet = null }
        )
        null -> Unit
    }

    story?.let { openedStory ->
        // No recalcular Batecs al clicar: utilitza exactament el snapshot que ja estava
        // pintat al carrusel. Si per algun motiu no existeix, obrim almenys el Batec tocat.
        val activeBatecs = storyViewerStories.ifEmpty { listOf(openedStory) }
            .filterNot { state.isArtistBlocked(it.artist) }
        StoryViewer(
            stories = activeBatecs,
            concerts = state.concerts,
            initialStoryKey = openedStory.storyKey.ifBlank { "manual:${openedStory.id}" },
            photoForArtist = { artist -> state.publicPhotoForArtist(artist) },
            isFollowed = { artist -> artist in state.followedArtists },
            onToggleFollow = { artist -> state.toggleFollowArtist(artist) },
            onSeenGroup = { viewed ->
                state.markBatecsSeen(viewed)
            },
            onOpenEvent = { eventId ->
                // Els Batecs obrin directament el popup i no desplacen el feed.
                closeBatecsViewer()
                specialEventConcert = null
                homeSection = HomeSection.VISUALITZACIO
                selectedConcert = state.concertById(eventId)
                focusedFeedConcertId = if (selectedConcert == null) eventId else null
            },
            onDismiss = closeBatecsViewer
        )
    }

    LaunchedEffect(selectedConcert?.id) {
        selectedConcert?.let(state::recordConcertView)
    }

    friendsInviteToAccept?.let { token ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { friendsInviteToAccept = null },
            title = { Text("Invitació a un grup d’amics") },
            text = { Text(if (state.currentAccount == null)
                "Cal iniciar sessió per acceptar la invitació. Després obri de nou l’enllaç."
                else "Vols unir-te al grup i compartir la seua agenda d’esdeveniments?") },
            confirmButton = {
                TextButton(onClick = {
                    if (state.currentAccount == null) {
                        inviteAfterLogin = token
                        friendsInviteToAccept = null
                        pushPage("login")
                    } else {
                        state.joinFriendsCircle(token) { ok, _ ->
                            if (ok) { homeSection = HomeSection.COMUNITAT; clearPages() }
                        }
                        friendsInviteToAccept = null
                    }
                }) { Text(if (state.currentAccount == null) "Iniciar sessió" else "Unir-me") }
            },
            dismissButton = { TextButton(onClick = { friendsInviteToAccept = null }) { Text("Cancel·lar") } }
        )
    }

    listInviteToAccept?.let { token ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { listInviteToAccept = null },
            title = { Text("Llista compartida · l'Agenda") },
            text = {
                Text(when {
                    listInviteError != null -> listInviteError.orEmpty()
                    listInviteName == null -> "Carregant la llista compartida…"
                    state.currentAccount == null -> "Afegir llista \"${listInviteName}\"? Hauràs d'iniciar sessió per confirmar-ho."
                    else -> "Afegir llista \"${listInviteName}\" als teus Guardats?"
                })
            },
            confirmButton = { TextButton(enabled = listInviteName != null && listInviteError == null, onClick = {
                if (state.currentAccount == null) {
                    listInviteAfterLogin = token
                    listInviteToAccept = null
                    pushPage("login")
                } else {
                    state.joinSharedSavedList(token) { ok, _ ->
                        if (ok) { homeSection = HomeSection.GUARDATS; clearPages() }
                    }
                    listInviteToAccept = null
                }
            }) { Text(if (state.currentAccount == null) "Iniciar sessió i afegir" else "Afegir llista") } },
            dismissButton = { TextButton(onClick = { listInviteToAccept = null }) { Text("Cancel·lar") } }
        )
    }

    // Tots els esdeveniments usen el mateix contenidor popup. Els festivals
    // mostren horari per jornades; la resta tenen un horari sense navegació de dies.
    selectedConcert?.let { concert ->
        FestivalPopupDialog(
            state = state,
            concert = concert,
            onDismiss = {
                selectedConcert = null
                focusedFestivalArtist = null
                focusedFestivalTime = null
                focusedNotificationCommunityGroupId = null
            },
            onRequireLogin = {
                selectedConcert = null
                focusedFestivalArtist = null
                focusedFestivalTime = null
                focusedNotificationCommunityGroupId = null
                pushPage("login")
            },
            focusedArtist = focusedFestivalArtist.takeIf { concert.isFestivalExperience },
            focusedTime = focusedFestivalTime.takeIf { concert.isFestivalExperience },
            focusedCommunityGroupId = focusedNotificationCommunityGroupId,
            onOpenSpecialEvent = { special ->
                specialEventParentConcert = concert
                selectedConcert = null
                focusedNotificationCommunityGroupId = null
                specialEventConcert = special
            },
            onOpenArtist = { artist ->
                selectedConcert = null
                focusedNotificationCommunityGroupId = null
                selectedArtist = artist
            }
        )
    }

    // Últim diàleg: el selector de llistes ha de quedar per damunt del popup d'esdeveniment.
    state.savePickerEventId?.let { eventId ->
        SavedCollectionPickerDialog(state, eventId, onDismiss = state::closeSavePicker)
    }
}

@Composable
private fun SponsoredTopCard(
    state: AgendaState,
    advertisement: EventAdvertisement,
    concert: Concert,
    onOpenConcert: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember(advertisement.id) { mutableStateOf(false) }
    var detailsExpanded by remember(advertisement.id) { mutableStateOf(false) }
    var showTicketDialog by remember(advertisement.id) { mutableStateOf(false) }
    val context = LocalContext.current
    val backgroundImage = rememberImageBitmapFromUris(
        listOf(advertisement.imageUri) + state.cardImageCandidates(concert)
    )
    LaunchedEffect(advertisement.id) {
        state.recordAdvertisementImpression(advertisement)
    }
    Card(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        shape = RoundedCornerShape(if (expanded) 18.dp else 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .14f))
    ) {
        if (!expanded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .clickable {
                        state.recordAdvertisementClick(advertisement)
                        onOpenConcert()
                    }
            ) {
                backgroundImage?.let { image ->
                    Image(
                        bitmap = image,
                        contentDescription = "Imatge de ${concert.title}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alignment = BiasAlignment(
                            horizontalBias = advertisement.cardFocusX.coerceIn(0f, 1f) * 2f - 1f,
                            verticalBias = advertisement.cardFocusY.coerceIn(0f, 1f) * 2f - 1f
                        )
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = .18f),
                                    Color.Black.copy(alpha = .52f),
                                    Color.Black.copy(alpha = .78f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 54.dp, top = 12.dp, bottom = 12.dp)
                ) {
                    Text(
                        concert.title,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (concert.isFestival) {
                        Text(
                            concert.lineup.map(String::trim).filter(String::isNotBlank).distinctBy { it.lowercase() }.joinToString(" · "),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = .94f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        "${concert.town} · ${formatDateCompact(concert.date)} · ${concert.time}",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = .92f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = .32f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⌄", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Column(Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                ) {
                    backgroundImage?.let { image ->
                        Image(
                            bitmap = image,
                            contentDescription = "Imatge de ${concert.title}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alignment = BiasAlignment(
                                horizontalBias = advertisement.cardFocusX.coerceIn(0f, 1f) * 2f - 1f,
                                verticalBias = advertisement.cardFocusY.coerceIn(0f, 1f) * 2f - 1f
                            )
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .08f), Color.Black.copy(alpha = .72f))))
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = .42f))
                            .clickable { expanded = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⌃", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp)
                    ) {
                        Text(concert.title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Text(
                            "${concert.town} · ${formatDateCompact(concert.date)} · ${concert.time}",
                            color = Color.White.copy(alpha = .92f),
                            fontSize = 12.sp
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            state.recordAdvertisementClick(advertisement)
                            if (concert.ticketUrl.isNotBlank()) { state.recordTicketClick(concert.id); openTicketUrl(context, concert.ticketUrl) }
                            else showTicketDialog = true
                        },
                        enabled = concert.ticketAvailable,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.ConfirmationNumber, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Entrades")
                    }
                    Button(
                        onClick = {
                            state.recordAdvertisementClick(advertisement)
                            onOpenConcert()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Veure esdeveniment")
                    }
                }
                if (detailsExpanded) {
                    SponsoredInlineEventDetails(concert = concert)
                }
            }
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
}

@Composable
private fun SponsoredHeaderCard(
    state: AgendaState,
    advertisement: EventAdvertisement,
    concert: Concert,
    onOpenConcert: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember(advertisement.id) { mutableStateOf(false) }
    var detailsExpanded by remember(advertisement.id) { mutableStateOf(false) }
    var showTicketDialog by remember(advertisement.id) { mutableStateOf(false) }
    val saved = state.isSavedAnywhere(concert.id)
    val context = LocalContext.current
    val backgroundImage = rememberImageBitmapFromUris(
        listOf(advertisement.imageUri) + state.cardImageCandidates(concert)
    )

    LaunchedEffect(advertisement.id) {
        state.recordAdvertisementImpression(advertisement)
    }

    Card(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 7.dp),
        shape = RoundedCornerShape(if (expanded) 18.dp else 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .14f))
    ) {
        if (!expanded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .clickable {
                        state.recordAdvertisementClick(advertisement)
                        onOpenConcert()
                    }
            ) {
                backgroundImage?.let { image ->
                    Image(
                        bitmap = image,
                        contentDescription = "Imatge de ${concert.title}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alignment = BiasAlignment(
                            horizontalBias = advertisement.cardFocusX.coerceIn(0f, 1f) * 2f - 1f,
                            verticalBias = advertisement.cardFocusY.coerceIn(0f, 1f) * 2f - 1f
                        )
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = .18f),
                                    Color.Black.copy(alpha = .72f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 78.dp, top = 12.dp, bottom = 12.dp)
                ) {
                    Text(
                        concert.title,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (concert.isFestival) {
                        Text(
                            concert.lineup.map(String::trim).filter(String::isNotBlank).distinctBy { it.lowercase() }.joinToString(" · "),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = .94f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        "${concert.town} · ${formatDateCompact(concert.date)} · ${concert.time}",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = .92f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 8.dp, bottom = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { state.openSavePicker(concert.id) },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (saved) "Guardat" else "Guardar",
                            tint = Color.White,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable {
                                state.recordAdvertisementClick(advertisement)
                                onOpenConcert()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⌄", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(310.dp)
                ) {
                    backgroundImage?.let { image ->
                        Image(
                            bitmap = image,
                            contentDescription = "Imatge de ${concert.title}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alignment = BiasAlignment(
                                horizontalBias = advertisement.cardFocusX.coerceIn(0f, 1f) * 2f - 1f,
                                verticalBias = advertisement.cardFocusY.coerceIn(0f, 1f) * 2f - 1f
                            )
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Black.copy(alpha = .08f), Color.Black.copy(alpha = .72f))
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = .42f))
                            .clickable { expanded = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⌃", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        if (concert.isFestival) {
                            concert.lineup.map(String::trim).filter(String::isNotBlank).distinctBy { it.lowercase() }.joinToString(" · ")
                        } else concert.artist,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = if (concert.isFestival) 2 else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    if (concert.isFree) "Entrada gratuïta" else if (concert.ticketPrice > 0) "🎟 ${concert.ticketPrice} €" else if (concert.ticketAvailable) "🎟 Entrades" else "Sense venda d’entrades",
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 9.dp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            state.recordAdvertisementClick(advertisement)
                            if (concert.ticketUrl.isNotBlank()) { state.recordTicketClick(concert.id); openTicketUrl(context, concert.ticketUrl) }
                            else showTicketDialog = true
                        },
                        enabled = concert.ticketAvailable,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.ConfirmationNumber, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Entrades")
                    }
                    Button(
                        onClick = {
                            state.recordAdvertisementClick(advertisement)
                            onOpenConcert()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Veure esdeveniment")
                    }
                }
                if (detailsExpanded) {
                    SponsoredInlineEventDetails(concert = concert)
                }
            }
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
}

@Composable
private fun SponsoredInlineEventDetails(concert: Concert) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
    ) {
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = .16f))
        Spacer(Modifier.height(10.dp))

        if (concert.subtitle.isNotBlank()) {
            Text(
                concert.subtitle,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
        }

        Text(
            "${AgendaDateFormat.date(concert.date)} · ${concert.time}",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
        if (concert.venue.isNotBlank() || concert.town.isNotBlank()) {
            Text(
                listOf(concert.venue, concert.town).filter(String::isNotBlank).joinToString(" · "),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val artists = if (concert.isFestival) {
            concert.lineup.map(String::trim).filter(String::isNotBlank).distinctBy { it.lowercase() }.joinToString(" · ")
        } else {
            concert.artist.trim()
        }
        if (artists.isNotBlank() && !artists.equals(concert.title.trim(), ignoreCase = true)) {
            Spacer(Modifier.height(7.dp))
            Text(
                artists,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (concert.description.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                concert.description,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SponsoredHeaderPendingCard(
    advertisement: EventAdvertisement,
    modifier: Modifier = Modifier
) {
    val image = rememberImageBitmapFromUris(listOf(advertisement.imageUri), maxDimension = 720)
    Card(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .14f))
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(132.dp)) {
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = "Publicitat",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alignment = BiasAlignment(
                        horizontalBias = advertisement.cardFocusX.coerceIn(0f, 1f) * 2f - 1f,
                        verticalBias = advertisement.cardFocusY.coerceIn(0f, 1f) * 2f - 1f
                    )
                )
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = .08f), Color.Black.copy(alpha = .46f))
                        )
                    )
                )
            }
            Text(
                "Publicitat",
                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                color = if (image != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SponsoredHeaderSlider(
    state: AgendaState,
    advertisements: List<EventAdvertisement>,
    onOpenConcert: (Concert) -> Unit
) {
    if (advertisements.isEmpty()) return
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cardWidth = if (maxWidth > 248.dp) maxWidth - 8.dp else 240.dp
        LazyRow(
            contentPadding = PaddingValues(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(advertisements, key = { "header_slider_ad_${it.id}" }) { ad ->
                val concert = state.concertForAdvertisement(ad)
                if (concert != null) {
                    SponsoredHeaderCard(
                        state = state,
                        advertisement = ad,
                        concert = concert,
                        onOpenConcert = { onOpenConcert(concert) },
                        modifier = Modifier.width(cardWidth)
                    )
                } else {
                    // L'anunci és independent de la finestra de Per a tu. Si el seu event
                    // encara no s'ha materialitzat, mostrem la creativitat cachejada i el
                    // card es completa automàticament quan arriba el Concert.
                    SponsoredHeaderPendingCard(advertisement = ad, modifier = Modifier.width(cardWidth))
                }
            }
        }
    }
}

@Composable
private fun SponsoredSlider(
    state: AgendaState,
    advertisements: List<EventAdvertisement>,
    onOpenConcert: (Concert) -> Unit
) {
    if (advertisements.isEmpty()) return
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cardWidth = if (maxWidth > 248.dp) maxWidth - 8.dp else 240.dp
        LazyRow(
            contentPadding = PaddingValues(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(advertisements, key = { "slider_ad_${it.id}" }) { ad ->
                val concert = state.concertForAdvertisement(ad)
                if (concert != null) {
                    SponsoredTopCard(
                        state = state,
                        advertisement = ad,
                        concert = concert,
                        onOpenConcert = { onOpenConcert(concert) },
                        modifier = Modifier.width(cardWidth)
                    )
                } else {
                    // Una creativitat encara sense fitxa ni imatge no pot convertir
                    // l'espai comercial en una fila aparentment buida.
                    SponsoredHeaderPendingCard(advertisement = ad, modifier = Modifier.width(cardWidth))
                }
            }
        }
    }
}

@Composable
private fun SponsoredPopupDialog(
    state: AgendaState,
    advertisement: EventAdvertisement,
    concert: Concert,
    onDismiss: () -> Unit,
    onOpenConcert: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Recomanat") },
        text = {
            Column {
                SponsoredTopCard(
                    state = state,
                    advertisement = advertisement,
                    concert = concert,
                    onOpenConcert = onOpenConcert,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Publicitat",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onOpenConcert()
                    state.recordAdvertisementClick(advertisement)
                }
            ) { Text("Veure esdeveniment") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Tancar") }
        }
    )
}

@Composable
private fun LoginScreen(
    state: AgendaState,
    onAuthenticated: () -> Unit,
    onContinueAsGuest: (() -> Unit)? = null,
    registrationAccountOnly: Boolean = false
) {
    var showRegister by remember { mutableStateOf(false) }
    BackHandler(enabled = showRegister) { showRegister = false }
    if (showRegister) {
        RegistrationScreen(
            state = state,
            onBack = { showRegister = false },
            onAuthenticated = onAuthenticated,
            accountOnly = registrationAccountOnly || ServerApiRepository.pendingGuestClaimCount(LocalContext.current.applicationContext) > 0
        )
        return
    }

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var showPasswordRecovery by remember { mutableStateOf(false) }
    var recoveryEmail by remember { mutableStateOf("") }
    var recoveryBusy by remember { mutableStateOf(false) }
    var recoveryMessage by remember { mutableStateOf<String?>(null) }
    var googleBusy by remember { mutableStateOf(false) }
    var googleError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.logo_lagenda_horizontal),
                contentDescription = "l’Agenda Cultural Valenciana",
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .aspectRatio(938f / 284f),
                contentScale = ContentScale.Fit
            )

            Spacer(Modifier.height(20.dp))
            if (ServerApiRepository.pendingGuestClaimCount(context.applicationContext) > 0) {
                Text(
                    "L’esdeveniment ja està publicat. Crea un compte o inicia sessió per conservar-lo i, si has seleccionat venda interna, activar-ne la gestió.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
                )
            }
            OutlinedTextField(
                value = username, onValueChange = { username = it; error = false }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, label = { Text("Usuari o correu") }, leadingIcon = { Icon(Icons.Outlined.Person, null) },
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it; error = false }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, label = { Text("Contrasenya") }, visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (!state.isLoggingIn) state.login(username, password) { ok ->
                        error = !ok
                        if (ok) onAuthenticated()
                    }
                }),
                shape = RoundedCornerShape(14.dp)
            )
            if (error) Text(state.loginError ?: "Usuari o contrasenya incorrectes.", modifier = Modifier.fillMaxWidth().padding(top = 8.dp), color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            TextButton(
                onClick = {
                    recoveryEmail = if (username.contains("@")) username.trim() else ""
                    recoveryMessage = null
                    recoveryBusy = false
                    showPasswordRecovery = true
                },
                modifier = Modifier.align(Alignment.End).padding(top = 2.dp)
            ) { Text("Has oblidat la contrasenya?") }
            Button(onClick = {
                state.login(username, password) { ok ->
                    error = !ok
                    if (ok) onAuthenticated()
                }
            }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), enabled = !state.isLoggingIn && !googleBusy, shape = RoundedCornerShape(14.dp)) {
                Text(if (state.isLoggingIn && !googleBusy) "Entrant…" else "Entrar")
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Divider(modifier = Modifier.weight(1f))
                Text("o", modifier = Modifier.padding(horizontal = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Divider(modifier = Modifier.weight(1f))
            }

            OutlinedButton(
                onClick = {
                    googleError = null
                    error = false
                    googleBusy = true
                    coroutineScope.launch {
                        GoogleAuthManager.signIn(context)
                            .onSuccess { credential ->
                                state.loginWithGoogle(credential.idToken, credential.nonce) { ok ->
                                    googleBusy = false
                                    if (ok) onAuthenticated()
                                    else googleError = state.loginError ?: "No s'ha pogut iniciar sessió amb Google."
                                }
                            }
                            .onFailure { throwable ->
                                googleBusy = false
                                val raw = throwable.message.orEmpty()
                                googleError = when {
                                    raw.contains("AGENDA_GOOGLE_WEB_CLIENT_ID") -> "Falta configurar Google en esta compilació."
                                    raw.contains("cancel", ignoreCase = true) -> null
                                    else -> raw.ifBlank { "No s'ha completat l'accés amb Google." }
                                }
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoggingIn && !googleBusy,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (googleBusy) "Connectant amb Google…" else "Continua amb Google")
            }
            if (googleError != null) {
                Text(
                    googleError ?: "",
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp
                )
            }

            OutlinedButton(onClick = { showRegister = true }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), enabled = !state.isLoggingIn && !googleBusy, shape = RoundedCornerShape(14.dp)) {
                Text("Crear compte")
            }
            if (onContinueAsGuest != null) {
                TextButton(
                    onClick = onContinueAsGuest,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                ) { Text("Continuar sense iniciar sessió") }
            }
        }
    }

    if (showPasswordRecovery) {
        AlertDialog(
            onDismissRequest = { showPasswordRecovery = false },
            title = { Text("Recuperar contrasenya") },
            text = {
                Column {
                    if (recoveryMessage == null) {
                        Text("Escriu el correu del compte i t'enviarem un enllaç segur per crear una contrasenya nova.")
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = recoveryEmail,
                            onValueChange = { recoveryEmail = it; recoveryMessage = null },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Correu electrònic") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            shape = RoundedCornerShape(14.dp),
                            enabled = !recoveryBusy
                        )
                    } else {
                        Text(recoveryMessage ?: "")
                    }
                }
            },
            confirmButton = {
                if (recoveryMessage != null) {
                    Button(onClick = { showPasswordRecovery = false }) { Text("Tancar") }
                } else {
                    Button(
                        onClick = {
                            recoveryBusy = true
                            state.requestPasswordReset(recoveryEmail) { _, message ->
                                recoveryBusy = false
                                recoveryMessage = message
                            }
                        },
                        enabled = recoveryEmail.contains("@") && !recoveryBusy
                    ) { Text(if (recoveryBusy) "Enviant…" else "Enviar correu") }
                }
            },
            dismissButton = {
                if (recoveryMessage == null) {
                    TextButton(onClick = { showPasswordRecovery = false }, enabled = !recoveryBusy) { Text("Cancel·lar") }
                }
            }
        )
    }
}

@Composable
private fun RegistrationScreen(
    state: AgendaState,
    onBack: () -> Unit,
    onAuthenticated: () -> Unit,
    accountOnly: Boolean = false,
    sellerMode: Boolean = false
) {
    var step by remember { mutableStateOf(0) }
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var password2 by remember { mutableStateOf("") }
    var selectedCategories by remember { mutableStateOf(setOf<CulturalCategory>()) }
    var categorySearch by remember { mutableStateOf("") }
    var selectedArtists by remember { mutableStateOf(setOf<String>()) }
    var shownArtists by remember { mutableStateOf(setOf<String>()) }
    var artistSearch by remember { mutableStateOf("") }
    var selectedTowns by remember { mutableStateOf(setOf<String>()) }
    var townSearch by remember { mutableStateOf("") }
    var notifyConcerts by remember { mutableStateOf(true) }
    var notifySavedReminders by remember { mutableStateOf(true) }
    var notifyTickets by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }

    // El registre directe de Perfil no necessita carregar categories ni artistes.
    LaunchedEffect(accountOnly) { if (!accountOnly) state.refreshOnboardingRankings() }

    val categoryChoices = remember(state.onboardingRankings, state.concerts) {
        state.onboardingCategoryChoices()
    }
    val visibleCategoryChoices = remember(categorySearch, categoryChoices) {
        if (categorySearch.isBlank()) categoryChoices
        else categoryChoices.filter { state.searchContains(it.label, categorySearch) }
    }
    val profileRound = (step - 2).coerceIn(0, 2)
    val rankedArtistPool = remember(
        selectedCategories,
        state.onboardingRankings,
        state.concerts,
        state.entityImageCatalogRevision
    ) {
        state.onboardingArtistChoices(
            categories = selectedCategories,
            excluded = emptySet(),
            query = "",
            limit = 600
        )
    }
    val roundArtists = remember(step, rankedArtistPool, shownArtists) {
        if (step in 2..4) rankedArtistPool.asSequence().filter { it !in shownArtists }.take(12).toList() else emptyList()
    }
    val visibleArtists = remember(
        artistSearch,
        roundArtists,
        rankedArtistPool,
        selectedCategories,
        state.entityImageCatalogRevision
    ) {
        if (artistSearch.isBlank()) roundArtists
        else state.onboardingArtistChoices(
            categories = selectedCategories,
            excluded = emptySet(),
            query = artistSearch,
            limit = 80
        )
    }
    // v277: no retallar el cens a 650 abans de buscar; impediria trobar pobles
    // que existixen en el catàleg però no en la primera pàgina de recomanacions.
    val rankedTownPool = remember(state.onboardingRankings, state.municipalities, state.concerts) {
        state.onboardingTownChoices("", limit = 24)
    }
    val visibleTowns = remember(townSearch, rankedTownPool, state.municipalities, state.onboardingRankings, state.concerts) {
        if (townSearch.isBlank()) rankedTownPool
        else state.onboardingTownChoices(townSearch, limit = 80)
    }

    fun nextFromAccount() {
        errorText = when {
            email.isBlank() || !email.contains("@") -> "Escriu un correu electrònic vàlid."
            username.trim().length < 3 -> "El nom d'usuari ha de tindre almenys 3 caràcters."
            password.length < 8 -> "La contrasenya ha de tindre almenys 8 caràcters."
            password != password2 -> "Les contrasenyes no coincidixen."
            else -> null
        }
        if (errorText == null) step = 1
    }

    fun advanceProfileRound() {
        shownArtists = shownArtists + roundArtists
        artistSearch = ""
        step = if (step < 4) step + 1 else 5
    }

    fun finishRegistration() {
        errorText = null
        state.registerUser(
            email = email,
            username = username,
            password = password,
            // La selecció del formulari inicial ja és al cache guest i serà
            // sincronitzada per claus exactes després de registrar-se.
            categories = if (accountOnly) emptySet() else selectedCategories,
            artists = if (accountOnly) state.followedArtists else selectedArtists,
            preferredTowns = if (accountOnly) state.userPreferences.preferredLocations else selectedTowns,
            notifyConcerts = notifyConcerts,
            notifySavedReminders = notifySavedReminders,
            notifyTickets = notifyTickets
        ) { ok ->
            if (ok) onAuthenticated()
            else errorText = state.loginError ?: "No s'ha pogut crear el compte."
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 18.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    when {
                        step == 0 -> onBack()
                        step == 5 && selectedCategories.isEmpty() -> step = 1
                        else -> step--
                    }
                }) { Text("‹ Enrere") }
                Spacer(Modifier.weight(1f))
                if (step > 0) {
                    TextButton(
                        onClick = {
                            when (step) {
                                1 -> step = 5
                                in 2..4 -> advanceProfileRound()
                                5 -> step = 6
                                6 -> finishRegistration()
                            }
                        },
                        enabled = !state.isLoggingIn
                    ) { Text("Ometre") }
                }
            }
            if (!accountOnly) LinearProgressIndicator(
                progress = { (step + 1) / 7f },
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
            )
            Text(
                when (step) {
                    0 -> if (sellerMode) "Dades d’accés" else if (accountOnly) "Crea el teu compte" else "Crea el teu compte"
                    1 -> "Què t'agrada?"
                    2 -> "Perfils per a tu"
                    3 -> "En vols més?"
                    4 -> "Últimes propostes"
                    5 -> "On t'interessa la cultura?"
                    else -> "Notificacions"
                },
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 18.dp)
            )
            Text(
                when (step) {
                    0 -> if (sellerMode) "Primer crea el compte; després indicaràs les dades de facturació i acceptaràs les condicions de venda. L’esdeveniment ja està publicat." else if (accountOnly) "Només necessites un correu, un nom d’usuari i una contrasenya. Les teues preferències actuals es conservaran." else "Primer crea el compte. Després prepararem el teu Per a tu."
                    1 -> "Marca les disciplines que realment t'interessen."
                    in 2..4 -> if (selectedCategories.isEmpty()) "Busca i seguix perfils que t'agraden." else "Propostes segons els gustos que has marcat."
                    5 -> "Marca els pobles que vols tindre especialment presents."
                    else -> "Pots canviar estos avisos quan vulgues."
                },
                modifier = Modifier.padding(top = 5.dp, bottom = 16.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .62f)
            )
        }

        when (step) {
            0 -> item {
                OutlinedTextField(email, { email = it; errorText = null }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Correu electrònic") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), shape = RoundedCornerShape(12.dp))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(username, { username = it; errorText = null }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Nom d'usuari") }, shape = RoundedCornerShape(12.dp))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(password, { password = it; errorText = null }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Contrasenya") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), shape = RoundedCornerShape(12.dp))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(password2, { password2 = it; errorText = null }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Repetix la contrasenya") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), shape = RoundedCornerShape(12.dp))
                errorText?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
                Button(onClick = { if (accountOnly) {
                    errorText = when {
                        email.isBlank() || !email.contains("@") -> "Escriu un correu electrònic vàlid."
                        username.trim().length < 3 -> "El nom d'usuari ha de tindre almenys 3 caràcters."
                        password.length < 8 -> "La contrasenya ha de tindre almenys 8 caràcters."
                        password != password2 -> "Les contrasenyes no coincidixen."
                        else -> null
                    }
                    if (errorText == null) finishRegistration()
                } else nextFromAccount() }, enabled = !state.isLoggingIn,
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp)) {
                    Text(if (sellerMode) "Crear compte i continuar a facturació" else if (accountOnly) "Crear compte" else "Continuar")
                }
            }

            1 -> {
                item {
                    OutlinedTextField(
                        categorySearch,
                        { categorySearch = it },
                        Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        singleLine = true,
                        placeholder = { Text("Busca una disciplina") },
                        leadingIcon = { Icon(Icons.Outlined.Search, null) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                items(visibleCategoryChoices) { category ->
                    val selected = category in selectedCategories
                    RegistrationVisualChoiceRow(
                        label = category.label,
                        imageUrl = state.publicPhotoForCategory(category),
                        selected = selected,
                        selectedLabel = "Seleccionada",
                        onClick = { selectedCategories = if (selected) selectedCategories - category else selectedCategories + category }
                    )
                }
                item {
                    Button(
                        onClick = { step = if (selectedCategories.isEmpty()) 5 else 2 },
                        modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
                    ) { Text("Continuar") }
                }
            }

            in 2..4 -> {
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("${profileRound + 1} / 3", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.weight(1f))
                        if (selectedArtists.isNotEmpty()) Text("${selectedArtists.size} seguits", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedTextField(
                        artistSearch,
                        { artistSearch = it },
                        Modifier.fillMaxWidth().padding(top = 10.dp),
                        singleLine = true,
                        placeholder = { Text("Busca un artista o perfil") },
                        leadingIcon = { Icon(Icons.Outlined.Search, null) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                if (visibleArtists.isEmpty()) {
                    item { Text("No hi ha més perfils per mostrar.", modifier = Modifier.padding(vertical = 24.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f)) }
                } else {
                    items(visibleArtists) { artist ->
                        val selected = artist in selectedArtists
                        RegistrationVisualChoiceRow(
                            label = artist,
                            imageUrl = state.publicPhotoForArtist(artist),
                            selected = selected,
                            selectedLabel = "Seguint",
                            onClick = { selectedArtists = if (selected) selectedArtists - artist else selectedArtists + artist }
                        )
                    }
                }
                item {
                    Button(onClick = { advanceProfileRound() }, modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
                        Text(if (step < 4) "Següents propostes" else "Continuar")
                    }
                }
            }

            5 -> {
                item {
                    OutlinedTextField(
                        townSearch,
                        { townSearch = it },
                        Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Busca un poble") },
                        leadingIcon = { Icon(Icons.Outlined.Search, null) },
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (selectedTowns.isNotEmpty()) Text("${selectedTowns.size} poblacions preferides", modifier = Modifier.padding(vertical = 10.dp), fontWeight = FontWeight.SemiBold)
                }
                items(visibleTowns) { town ->
                    val selected = town.name in selectedTowns
                    RegistrationVisualChoiceRow(
                        label = town.name,
                        subtitle = listOf(town.comarca, town.province)
                            .filter(String::isNotBlank).distinct().joinToString(" · ")
                            .takeIf(String::isNotBlank),
                        imageUrl = state.publicPhotoForTown(town.name),
                        selected = selected,
                        selectedLabel = "Preferida",
                        onClick = { selectedTowns = if (selected) selectedTowns - town.name else selectedTowns + town.name }
                    )
                }
                item { Button(onClick = { step = 6 }, modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) { Text("Continuar") } }
            }

            else -> item {
                RegistrationNotificationRow("Concerts nous dels artistes que seguixes", notifyConcerts) { notifyConcerts = it }
                RegistrationNotificationRow("Esdeveniments guardats pròxims", notifySavedReminders) { notifySavedReminders = it }
                RegistrationNotificationRow("Compres d'entrades", notifyTickets) { notifyTickets = it }
                errorText?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
                Button(
                    onClick = { finishRegistration() },
                    enabled = !state.isLoggingIn,
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp)
                ) { Text(if (state.isLoggingIn) "Creant compte…" else "Entrar a l'Agenda") }
            }
        }
    }
}

@Composable
private fun RegistrationVisualChoiceRow(
    label: String,
    imageUrl: String?,
    selected: Boolean,
    selectedLabel: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    val image = rememberImageBitmapFromUri(imageUrl)
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = if (selected) 2.dp else 0.dp,
        border = BorderStroke(1.5.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = .28f))
    ) {
        Row(Modifier.fillMaxWidth().padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(66.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (image != null) {
                    Image(bitmap = image, contentDescription = label, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Text(label.take(1).uppercase(), fontWeight = FontWeight.Black, fontSize = 22.sp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                subtitle?.takeIf { it.isNotBlank() }?.let {
                    Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .58f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (selected) {
                Column(horizontalAlignment = Alignment.End) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Text(selectedLabel, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun RegistrationNotificationRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(modifier=Modifier.fillMaxWidth().padding(vertical=10.dp), verticalAlignment=Alignment.CenterVertically) {
        Text(label, modifier=Modifier.weight(1f), fontSize=15.sp)
        Switch(checked=checked, onCheckedChange=onChecked)
    }
    Divider()
}

@Composable
private fun InstagramTopBar(
    notificationCount: Int,
    onUser: () -> Unit,
    onPublish: () -> Unit,
    onDiscover: () -> Unit,
    onAlerts: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(62.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onUser) {
                Icon(Icons.Outlined.Person, contentDescription = "Usuari")
            }
            IconButton(onClick = onPublish) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = "Publicar",
                    tint = AgendaBrandOrange
                )
            }
            Image(
                painter = painterResource(id = R.drawable.logo_lagenda_horizontal),
                contentDescription = "l’Agenda Cultural Valenciana",
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .padding(horizontal = 8.dp),
                contentScale = ContentScale.Fit
            )

            IconButton(onClick = onDiscover) {
                Icon(Icons.Outlined.Tune, contentDescription = "Personalització")
            }

            Box {
                IconButton(onClick = onAlerts) {
                    Icon(Icons.Outlined.Notifications, contentDescription = "Notificacions")
                }
                if (notificationCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 6.dp, end = 5.dp)
                            .size(if (notificationCount > 9) 21.dp else 19.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (notificationCount > 99) "99+" else notificationCount.toString(),
                            color = MaterialTheme.colorScheme.onError,
                            fontSize = if (notificationCount > 9) 8.sp else 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PublishActionsSheet(
    state: AgendaState,
    onNewEvent: () -> Unit,
    onEditEvents: () -> Unit,
    onTickets: () -> Unit,
    onArtistCommerce: () -> Unit,
    onCreateAd: () -> Unit,
    onReviewPublications: () -> Unit,
    onReviewProfessionalClaims: () -> Unit,
    onProfessionalRequest: () -> Unit,
    onDismiss: () -> Unit
) {
    LaunchedEffect(state.currentAccount?.serverUserId) {
        state.refreshPublisherAccess()
        state.refreshPublicationTrust()
    }
    val authorized = state.isAuthorizedPublisher()
    // Desbloqueig per autoria real, no pel rol. S'executa cada vegada que s'obri "+".
    // La resposta inclou convidats (guest_id) i comptes (token).
    var ownPublishedEventIds by remember(state.currentAccount?.serverUserId) { mutableStateOf<Set<Int>>(emptySet()) }
    LaunchedEffect(state.currentAccount?.serverUserId) {
        state.loadOwnSubmittedEvents { items, _ ->
            ownPublishedEventIds = items.filter { it.status.equals("published", true) }.map { it.eventId }.toSet()
        }
    }
    val hasPublishedEvents = ownPublishedEventIds.isNotEmpty()
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            val isProfessional = authorized || state.currentAccount?.role == AccountRole.SUPER_ADMIN
            Text(if (isProfessional) "Panell professional" else "Publicar", fontSize = 21.sp, fontWeight = FontWeight.Black)
            Text(
                when {
                    state.publicationTrust?.suspendedByReport == true -> "Publicació temporalment suspesa. Els teus esdeveniments i vendes es conserven."
                    isProfessional -> "Publica directament i gestiona entrades, ingressos, esdeveniments i anuncis."
                    state.currentAccount == null -> "Pots publicar esdeveniments sense registrar-te. Els revisarem després de publicar-los; la venda d'entrades interna necessita compte."
                    else -> "Publica activitats directament. Les publicacions són revisades periòdicament."
                },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .62f)
            )
            if (state.publicationTrust?.suspendedByReport == true) {
                Text("Publicació suspesa: consulta les notificacions de moderació.", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(3.dp))
            PublishActionRow(Icons.Outlined.Add, "Nou esdeveniment", onNewEvent)
            if (hasPublishedEvents) {
                PublishActionRow(Icons.Outlined.Edit, "Editar esdeveniments", onEditEvents)
                PublishActionRow(Icons.Outlined.Campaign, "Crear anunci", onCreateAd)
            }
            if (isProfessional) {
                PublishActionRow(Icons.Outlined.ConfirmationNumber, "Entrades, ingressos i facturació", onTickets)
                PublishActionRow(Icons.Outlined.Album, "Vendre discos / merch", onArtistCommerce)
                PublishActionRow(Icons.Outlined.Campaign, "Donacions", onArtistCommerce)
            }
            if (state.currentAccount?.role == AccountRole.SUPER_ADMIN) {
                Spacer(Modifier.height(4.dp))
                Text("REVISIÓ", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                PublishActionRow(Icons.Outlined.CheckCircle, "Revisar publicacions", onReviewPublications)
                PublishActionRow(Icons.Outlined.Group, "Sol·licituds professionals", onReviewProfessionalClaims)
            }
            Spacer(Modifier.height(4.dp))
            if (state.currentAccount?.role != AccountRole.SUPER_ADMIN) {
                TextButton(
                    onClick = onProfessionalRequest,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        if (authorized) "Reclamar un altre grup o perfil" else "Soc organitzador o artista",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PublishActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .16f))
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = AgendaBrandOrange, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .62f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private enum class EventTicketMode(val label: String) {
    NONE("Sense entrades"),
    AGENDA("Vendre entrades a l’Agenda"),
    EXTERNAL("Enllaç extern")
}

private data class EventPublishResultUi(
    val published: Boolean,
    val eventTitle: String,
    val detail: String,
    val needsAccount: Boolean = false,
    val needsInternalSaleAccount: Boolean = false
)

private fun eventDateLabel(value: String): String = runCatching {
    AgendaDateFormat.date(value)
}.getOrElse { value.ifBlank { "Selecciona" } }

private fun eventTimeLabel(value: String): String = runCatching {
    LocalTime.parse(value.take(8)).format(DateTimeFormatter.ofPattern("HH:mm"))
}.getOrElse { value.take(5).ifBlank { "Selecciona" } }

private fun showEventTimePicker(
    context: Context,
    currentValue: String?,
    onSelected: (String) -> Unit
) {
    val initial = runCatching {
        LocalTime.parse(currentValue.orEmpty().take(5), DateTimeFormatter.ofPattern("HH:mm"))
    }.getOrNull() ?: LocalTime.now()
    TimePickerDialog(
        context,
        { _, hour, minute -> onSelected(String.format(Locale.ROOT, "%02d:%02d", hour, minute)) },
        initial.hour,
        initial.minute,
        true
    ).show()
}

@Composable
private fun EventDatePickerField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    allowClear: Boolean = false
) {
    val context = LocalContext.current
    Surface(
        modifier = modifier.clickable {
            showFilterDatePicker(context, value.takeIf(String::isNotBlank)) { onValueChange(it) }
        },
        shape = RoundedCornerShape(4.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .55f))
    ) {
        Row(
            Modifier.padding(start = 12.dp, end = 6.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    eventDateLabel(value),
                    fontSize = 16.sp,
                    color = if (value.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .65f)
                    else MaterialTheme.colorScheme.onSurface
                )
            }
            if (allowClear && value.isNotBlank()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Llevar data", modifier = Modifier.size(18.dp))
                }
            } else {
                Icon(
                    Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    tint = AgendaBrandOrange,
                    modifier = Modifier.padding(10.dp).size(19.dp)
                )
            }
        }
    }
}

@Composable
private fun EventTimePickerField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    allowClear: Boolean = true
) {
    val context = LocalContext.current
    Surface(
        modifier = modifier.clickable {
            showEventTimePicker(context, value.takeIf(String::isNotBlank)) { onValueChange(it) }
        },
        shape = RoundedCornerShape(4.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .55f))
    ) {
        Row(
            Modifier.padding(start = 12.dp, end = 6.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    eventTimeLabel(value),
                    fontSize = 16.sp,
                    color = if (value.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .65f)
                    else MaterialTheme.colorScheme.onSurface
                )
            }
            if (allowClear && value.isNotBlank()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Llevar hora", modifier = Modifier.size(18.dp))
                }
            } else {
                Icon(
                    Icons.Outlined.AccessTime,
                    contentDescription = null,
                    tint = AgendaBrandOrange,
                    modifier = Modifier.padding(10.dp).size(19.dp)
                )
            }
        }
    }
}

@Composable
private fun EventPublishResultScreen(
    result: EventPublishResultUi,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onCreateAccount: () -> Unit,
    onInternalSaleAccountRequired: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        PublisherPageHeader(
            title = if (result.published) "Esdeveniment publicat" else "Enviat a revisió",
            subtitle = if (result.published) "Publicació completada" else "Proposta rebuda",
            onBack = onBack
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (result.published) Icons.Filled.CheckCircle else Icons.Outlined.AccessTime,
                contentDescription = null,
                tint = if (result.published) Color(0xFF2E7D32) else AgendaBrandOrange,
                modifier = Modifier.size(58.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                if (result.published) "Esdeveniment publicat" else "Enviat a revisió",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black
            )
            if (result.eventTitle.isNotBlank()) {
                Spacer(Modifier.height(7.dp))
                Text(
                    result.eventTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                if (result.published)
                    if (result.needsInternalSaleAccount) "L’esdeveniment ja és públic. Ara pots iniciar el procés de venda interna." else if (result.needsAccount) "L’esdeveniment ja és públic. Ara crea un compte o inicia sessió per conservar-lo i gestionar-lo." else "Ja està publicat i disponible a l’Agenda."
                else
                    "No s’ha pogut confirmar la publicació. Revisa la resposta del servidor.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (!result.published) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AgendaBrandOrange.copy(alpha = .10f)
                ) {
                    Text(
                        "La publicació no està confirmada",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (result.detail.contains("no s'ha pogut", ignoreCase = true) || result.detail.contains("ATENCIÓ", ignoreCase = true)) {
                Spacer(Modifier.height(12.dp))
                Text(
                    result.detail,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = when {
                    result.needsInternalSaleAccount -> onInternalSaleAccountRequired
                    result.needsAccount -> onCreateAccount
                    else -> onBack
                },
                colors = ButtonDefaults.buttonColors(containerColor = AgendaBrandOrange)
            ) {
                Text(when {
                    result.needsInternalSaleAccount -> "Iniciar procés de venda"
                    result.needsAccount -> "Crear compte / Iniciar sessió"
                    else -> "Tornar a l’Agenda"
                }, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EventTicketModeSelector(
    selected: EventTicketMode,
    onSelected: (EventTicketMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Entrades", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        EventTicketMode.entries.forEach { mode ->
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { onSelected(mode) },
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(
                    1.dp,
                    if (selected == mode) AgendaBrandOrange.copy(alpha = .70f)
                    else MaterialTheme.colorScheme.outline.copy(alpha = .16f)
                )
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = selected == mode, onClick = { onSelected(mode) })
                    Spacer(Modifier.width(7.dp))
                    Column(Modifier.weight(1f)) {
                        Text(mode.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        when (mode) {
                            EventTicketMode.AGENDA -> Text(
                                "Primer es publica l'esdeveniment; després es crea o inicia sessió per activar la gestió d'entrades.", fontSize = 11.sp, color = Color.Gray
                            )
                            EventTicketMode.EXTERNAL -> Text(
                                "Envia l’usuari a la teua plataforma de venda.", fontSize = 11.sp, color = Color.Gray
                            )
                            EventTicketMode.NONE -> Unit
                        }
                    }
                }
            }
        }
    }
}

private fun catalogLookupKey(value: String): String =
    Normalizer.normalize(value.trim().lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")

private fun publisherConcertMatches(concert: Concert, query: String): Boolean {
    val key = catalogLookupKey(query)
    if (key.isBlank()) return true
    return listOf(
        concert.title,
        concert.artist,
        concert.lineup.joinToString(" "),
        concert.organizers.joinToString(" "),
        concert.town,
        concert.comarca,
        concert.venue,
        concert.date,
        concert.time
    ).any { catalogLookupKey(it).contains(key) }
}

private fun publisherOwnEventMatches(item: ServerApiRepository.OwnSubmittedEvent, query: String): Boolean {
    val key = catalogLookupKey(query)
    if (key.isBlank()) return true
    return listOf(
        item.title,
        item.subtitle,
        item.artists.joinToString(" "),
        item.organizers.joinToString(" "),
        item.town,
        item.comarca,
        item.venue,
        item.date,
        item.time
    ).any { catalogLookupKey(it).contains(key) }
}

@Composable
private fun PublisherSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = if (value.isBlank()) null else {
            {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Netejar cerca")
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        shape = RoundedCornerShape(14.dp)
    )
}

@Composable
private fun ArtistCatalogSelector(
    state: AgendaState,
    selected: List<String>,
    onSelectedChange: (List<String>) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    val existingArtists = remember(state.entityImageCatalogRevision) {
        ServerApiRepository.peekCachedEntityNames("ARTIST").ifEmpty { state.knownArtistNames() }
    }
    val suggestions = remember(query, selected, existingArtists) {
        val q = catalogLookupKey(query)
        if (q.isBlank()) emptyList()
        else existingArtists.asSequence()
            .filter { candidate -> selected.none { catalogLookupKey(it) == catalogLookupKey(candidate) } }
            .filter { catalogLookupKey(it).contains(q) }
            .take(8)
            .toList()
    }
    val cleanQuery = query.trim()
    val cleanKey = catalogLookupKey(cleanQuery)
    val exactExists = existingArtists.any { catalogLookupKey(it) == cleanKey } ||
        selected.any { catalogLookupKey(it) == cleanKey }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (selected.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(selected, key = { catalogLookupKey(it) }) { artist ->
                    AssistChip(
                        onClick = { onSelectedChange(selected.filterNot { it.equals(artist, ignoreCase = true) }) },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(artist, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.width(5.dp))
                                Icon(Icons.Outlined.Close, contentDescription = "Llevar", modifier = Modifier.size(15.dp))
                            }
                        }
                    )
                }
            }
        }
        Box(Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    menuOpen = it.isNotBlank()
                },
                label = { Text("Artistes / participants") },
                placeholder = { Text("Busca un artista…") },
                supportingText = { Text("Selecciona un existent o crea'l si no està al catàleg") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            DropdownMenu(
                expanded = menuOpen && cleanQuery.isNotBlank(),
                onDismissRequest = { menuOpen = false },
                properties = PopupProperties(focusable = false),
                modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)
            ) {
                suggestions.forEach { artist ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(artist, fontWeight = FontWeight.SemiBold)
                                Text("Artista existent", fontSize = 10.sp, color = Color.Gray)
                            }
                        },
                        leadingIcon = { Icon(Icons.Outlined.MusicNote, contentDescription = null, tint = AgendaBrandOrange) },
                        onClick = {
                            onSelectedChange((selected + artist).distinctBy(::catalogLookupKey))
                            query = ""
                            menuOpen = false
                        }
                    )
                }
                if (!exactExists) {
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text("Crear «$cleanQuery»", fontWeight = FontWeight.Bold)
                                Text("Nou artista · es revisarà amb l’esdeveniment", fontSize = 10.sp, color = Color.Gray)
                            }
                        },
                        leadingIcon = { Icon(Icons.Outlined.Add, contentDescription = null, tint = AgendaBrandOrange) },
                        onClick = {
                            onSelectedChange((selected + cleanQuery).distinctBy(::catalogLookupKey))
                            query = ""
                            menuOpen = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TownCatalogSelector(
    state: AgendaState,
    value: String,
    isNew: Boolean,
    onSelected: (String, Boolean) -> Unit
) {
    var query by remember(value) { mutableStateOf(value) }
    var menuOpen by remember { mutableStateOf(false) }
    val suggestions = remember(query, state.municipalities) {
        if (query.isBlank()) emptyList() else state.onboardingTownChoices(query, limit = 8)
    }
    val cleanQuery = query.trim()
    val exact = state.municipalityFor(cleanQuery) ?: suggestions.firstOrNull {
        it.name.equals(cleanQuery, ignoreCase = true) || it.officialName.equals(cleanQuery, ignoreCase = true)
    }

    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                onSelected(it, false)
                menuOpen = it.isNotBlank()
            },
            label = { Text("Poble *") },
            placeholder = { Text("Busca un poble…") },
            supportingText = {
                when {
                    value.isNotBlank() && isNew -> Text("Nou poble/localitat · es revisarà amb l’esdeveniment")
                    value.isNotBlank() && state.municipalityFor(value) != null -> Text("Poble existent seleccionat")
                    else -> Text("Selecciona una coincidència o prem Crear")
                }
            },
            leadingIcon = { Icon(Icons.Outlined.LocationOn, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        DropdownMenu(
            expanded = menuOpen && cleanQuery.isNotBlank(),
            onDismissRequest = { menuOpen = false },
            properties = PopupProperties(focusable = false),
            modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)
        ) {
            suggestions.forEach { municipality ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(municipality.name, fontWeight = FontWeight.SemiBold)
                            Text(municipality.comarca, fontSize = 10.sp, color = Color.Gray)
                        }
                    },
                    leadingIcon = { Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = AgendaBrandOrange) },
                    onClick = {
                        query = municipality.name
                        onSelected(municipality.name, false)
                        menuOpen = false
                    }
                )
            }
            if (exact == null) {
                DropdownMenuItem(
                    text = {
                        Column {
                            Text("Crear «$cleanQuery»", fontWeight = FontWeight.Bold)
                            Text("Nou poble/localitat · es revisarà amb l’esdeveniment", fontSize = 10.sp, color = Color.Gray)
                        }
                    },
                    leadingIcon = { Icon(Icons.Outlined.Add, contentDescription = null, tint = AgendaBrandOrange) },
                    onClick = {
                        query = cleanQuery
                        onSelected(cleanQuery, true)
                        menuOpen = false
                    }
                )
            }
        }
    }
}

@Composable
private fun PublisherPageHeader(title: String, subtitle: String? = null, onBack: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color.White)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ChevronLeft, contentDescription = "Arrere") }
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.Black)
                subtitle?.let { Text(it, fontSize = 11.sp, color = Color.Gray) }
            }
        }
        Divider(color = Color(0xFFEAEAEA))
    }
}

@Composable
private fun NewEventPublisherScreen(
    state: AgendaState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onCreateAccount: () -> Unit,
    onInternalSaleAccountRequired: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(state.currentAccount?.serverUserId) { state.refreshPublicationTrust() }
    val authorized = state.canAutoPublishEvents()
    var posterUri by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var selectedArtists by remember { mutableStateOf<List<String>>(emptyList()) }
    var organizersText by remember { mutableStateOf(if (authorized) state.currentAccount?.displayName.orEmpty() else "") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var town by remember { mutableStateOf("") }
    var townIsNew by remember { mutableStateOf(false) }
    var venue by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(CulturalCategory.MUSIC) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var stylesText by remember { mutableStateOf("") }
    var ticketMode by remember { mutableStateOf(EventTicketMode.NONE) }
    var priceText by remember { mutableStateOf("0") }
    var ticketUrl by remember { mutableStateOf("") }
    var ticketDrafts by remember { mutableStateOf<List<EventTicketDraft>>(emptyList()) }
    var description by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }
    var publishResult by remember { mutableStateOf<EventPublishResultUi?>(null) }
    val poster = rememberImageBitmapFromUris(listOf(posterUri), maxDimension = 720)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            posterUri = runCatching { cachePickedImageForEditing(context, uri) }.getOrElse { uri.toString() }
        }
    }
    publishResult?.let { result ->
        EventPublishResultScreen(
            result = result,
            contentPadding = contentPadding,
            onBack = onBack,
            onCreateAccount = onCreateAccount,
            onInternalSaleAccountRequired = onInternalSaleAccountRequired
        )
        return
    }

    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        PublisherPageHeader(
            title = "Nou esdeveniment",
            subtitle = "Publicació immediata. Comprovació posterior al Sheets.",
            onBack = onBack
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFAFAFA),
                    border = BorderStroke(1.dp, Color(0xFFE4E4E4))
                ) {
                    if (poster != null) {
                        Image(
                            bitmap = poster,
                            contentDescription = "Cartell",
                            modifier = Modifier.fillMaxWidth().height(260.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null, tint = AgendaBrandOrange, modifier = Modifier.size(30.dp))
                            Spacer(Modifier.height(6.dp))
                            Text("Compartir cartell", fontWeight = FontWeight.Bold)
                            Text("JPG, PNG o foto", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }
            item { OutlinedTextField(title, { title = it }, label = { Text("Títol *") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
            item { OutlinedTextField(subtitle, { subtitle = it }, label = { Text("Subtítol") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
            item {
                ArtistCatalogSelector(
                    state = state,
                    selected = selectedArtists,
                    onSelectedChange = { selectedArtists = it }
                )
            }
            item { OutlinedTextField(organizersText, { organizersText = it }, label = { Text("Organitzador / promotora") }, supportingText = { Text("Opcional; en festivals és especialment important") }, modifier = Modifier.fillMaxWidth()) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EventDatePickerField(
                        value = date,
                        label = "Data * · DD/MM/YYYY",
                        onValueChange = { date = it },
                        modifier = Modifier.weight(1f)
                    )
                    EventTimePickerField(
                        value = time,
                        label = "Hora",
                        onValueChange = { time = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                TownCatalogSelector(
                    state = state,
                    value = town,
                    isNew = townIsNew,
                    onSelected = { value, created -> town = value; townIsNew = created }
                )
            }
            item { OutlinedTextField(venue, { venue = it }, label = { Text("Lloc / recinte") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
            item {
                Box(Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { categoryExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Categoria · ${category.label}")
                    }
                    DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                        CulturalCategory.entries.forEach { candidate ->
                            DropdownMenuItem(
                                text = { Text(candidate.label) },
                                onClick = { category = candidate; categoryExpanded = false }
                            )
                        }
                    }
                }
            }
            item { OutlinedTextField(stylesText, { stylesText = it }, label = { Text("Estils") }, supportingText = { Text("Separa'ls amb comes") }, modifier = Modifier.fillMaxWidth()) }
            item {
                EventTicketModeSelector(
                    selected = ticketMode,
                    onSelected = { mode ->
                        ticketMode = mode
                        if (mode != EventTicketMode.EXTERNAL) ticketUrl = ""
                    }
                )
            }
            if (ticketMode == EventTicketMode.AGENDA) {
                item { EventTicketDraftEditor(drafts = ticketDrafts, onChange = { ticketDrafts = it }) }
            }
            if (ticketMode == EventTicketMode.EXTERNAL) {
                item {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it.filter { c -> c.isDigit() }.take(5) },
                        label = { Text("Preu €") },
                        supportingText = {
                            Text(if (ticketMode == EventTicketMode.AGENDA) "Preu de venda dins de l’Agenda" else "Preu orientatiu de l’entrada")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            }
            if (ticketMode == EventTicketMode.EXTERNAL) {
                item {
                    OutlinedTextField(
                        value = ticketUrl,
                        onValueChange = { ticketUrl = it },
                        label = { Text("Enllaç extern d’entrades *") },
                        placeholder = { Text("https://…") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
            item { OutlinedTextField(description, { description = it }, label = { Text("Informació") }, modifier = Modifier.fillMaxWidth(), minLines = 4, maxLines = 8) }
            if (sending && posterUri != null) item { LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth()) }
            message?.let { text ->
                item { Text(text, color = if (success) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error, fontSize = 13.sp) }
            }
            item {
                Button(
                    onClick = {
                        sending = true
                        success = false
                        message = null
                        val artists = selectedArtists
                        val organizers = organizersText.split(',', ';', '|').map(String::trim).filter(String::isNotBlank)
                        val styles = stylesText.split(',', ';', '|').map(String::trim).filter(String::isNotBlank)
                        val price = priceText.toIntOrNull() ?: 0
                        val ticketEnabled = ticketMode != EventTicketMode.NONE
                        val effectiveTicketUrl = if (ticketMode == EventTicketMode.EXTERNAL) ticketUrl.trim() else ""
                        if (ticketMode == EventTicketMode.EXTERNAL && effectiveTicketUrl.isBlank()) {
                            sending = false
                            message = "Indica l’enllaç extern de venda d’entrades."
                            return@Button
                        }
                        // V313: primer publicar. El registre es demana DESPRES, no abans.
                        // La venda AGENDA del convidat queda reservada fins vincular el compte.
                        // v308: qualsevol persona publica immediatament i la revisio
                        // es registra DESPRES al Sheets com a autoreport.
                        // Els comptes suspesos continuen bloquejats.
                        if (state.currentAccount != null && state.publicationTrust?.suspendedByReport == true) {
                            sending = false
                            message = "Tens la publicació temporalment suspesa. Consulta moderació."
                            return@Button
                        }
                        if (authorized) {
                            state.publishEventToServer(
                                posterUri = posterUri,
                                title = title,
                                subtitle = subtitle,
                                artists = artists,
                                organizers = organizers,
                                date = date,
                                time = time,
                                town = town,
                                allowNewTown = townIsNew,
                                venue = venue,
                                category = category,
                                styles = styles,
                                price = price,
                                ticketSalesEnabled = ticketMode == EventTicketMode.EXTERNAL,
                                ticketUrl = effectiveTicketUrl,
                                description = description,
                                onImageProgress = { progress = it }
                            ) { ok, msg, eventId, _, _ ->
                                sending = false
                                success = ok
                                message = if (ok) null else msg
                                if (ok) {
                                    if (ticketMode == EventTicketMode.AGENDA) {
                                        eventId?.let(state::rememberInternalSaleEvent)
                                        onInternalSaleAccountRequired()
                                    }
                                    val finish: (String) -> Unit = { note ->
                                        publishResult = EventPublishResultUi(
                                            published = true, eventTitle = title.trim(),
                                            detail = msg + note, needsAccount = false)
                                    }
                                    if (ticketMode == EventTicketMode.AGENDA && ticketDrafts.isNotEmpty() && eventId != null) {
                                        state.saveEventTicketDrafts(eventId, ticketDrafts.ticketDraftJson()) { saved, note ->
                                            finish(if (saved) " Modalitats guardades com a pendents." else " ATENCIÓ: modalitats no guardades: $note")
                                        }
                                    } else finish("")
                                }
                            }
                        } else {
                            state.submitEventForReviewToServer(
                                posterUri = posterUri,
                                title = title,
                                subtitle = subtitle,
                                artists = artists,
                                organizers = organizers,
                                date = date,
                                time = time,
                                town = town,
                                allowNewTown = townIsNew,
                                venue = venue,
                                category = category,
                                styles = styles,
                                price = price,
                                ticketSalesEnabled = ticketMode == EventTicketMode.EXTERNAL,
                                ticketUrl = effectiveTicketUrl,
                                ticketSaleOnAgenda = ticketMode == EventTicketMode.AGENDA,
                                description = description,
                                onImageProgress = { progress = it }
                            ) { ok, msg, published, eventId ->
                                sending = false
                                success = ok
                                message = if (ok) null else msg
                                if (ok) {
                                    // L'esdeveniment és públic ABANS de desar les modalitats (inactives).
                                    val finish: (String) -> Unit = { note ->
                                        publishResult = EventPublishResultUi(
                                            published = published, eventTitle = title.trim(),
                                            detail = msg + note,
                                            needsAccount = state.currentAccount == null,
                                            needsInternalSaleAccount = state.currentAccount == null && ticketMode == EventTicketMode.AGENDA
                                        )
                                        if (published) {
                                            if (ticketMode == EventTicketMode.AGENDA) onInternalSaleAccountRequired()
                                            else if (state.currentAccount == null) onCreateAccount()
                                        }
                                    }
                                    if (ticketMode == EventTicketMode.AGENDA && ticketDrafts.isNotEmpty() && eventId != null) {
                                        state.saveEventTicketDrafts(eventId, ticketDrafts.ticketDraftJson()) { saved, note ->
                                            finish(if (saved) " Modalitats guardades com a pendents." else " ATENCIÓ: modalitats no guardades: $note")
                                        }
                                    } else finish("")
                                }
                            }
                        }
                    },
                    enabled = !sending,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AgendaBrandOrange)
                ) {
                    Text(if (sending) "Publicant…" else "Publicar", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
            item { Spacer(Modifier.height(18.dp)) }
        }
    }
}

@Composable
private fun ManagedEventsPublisherScreen(
    state: AgendaState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onCreateAccount: () -> Unit,
    onInternalSaleAccountRequired: () -> Unit
) {
    val authorized = state.isAuthorizedPublisher()
    var editingManaged by remember { mutableStateOf<Concert?>(null) }
    var editingOwn by remember { mutableStateOf<ServerApiRepository.OwnSubmittedEvent?>(null) }
    var ownEvents by remember { mutableStateOf<List<ServerApiRepository.OwnSubmittedEvent>>(emptyList()) }
    var loading by remember { mutableStateOf(!authorized) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var deleteManaged by remember { mutableStateOf<Concert?>(null) }
    var deleteOwn by remember { mutableStateOf<ServerApiRepository.OwnSubmittedEvent?>(null) }
    var deleting by remember { mutableStateOf(false) }
    var actionMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(authorized, reloadKey) {
        if (!authorized) {
            loading = true
            error = null
            state.loadOwnSubmittedEvents { items, message ->
                ownEvents = items
                error = message
                loading = false
            }
        }
    }

    deleteManaged?.let { concert ->
        AlertDialog(
            onDismissRequest = { if (!deleting) deleteManaged = null },
            title = { Text("Eliminar esdeveniment?") },
            text = { Text("S’eliminarà «${concert.title}». Esta acció no es pot desfer.") },
            dismissButton = {
                TextButton(onClick = { deleteManaged = null }, enabled = !deleting) { Text("Cancel·lar") }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleting = true
                        state.deleteConcertFromEditor(concert.id) { ok, msg ->
                            deleting = false
                            actionMessage = msg
                            if (ok) {
                                deleteManaged = null
                                if (editingManaged?.id == concert.id) editingManaged = null
                            }
                        }
                    },
                    enabled = !deleting
                ) { Text(if (deleting) "Eliminant…" else "Eliminar", color = MaterialTheme.colorScheme.error) }
            }
        )
    }

    deleteOwn?.let { item ->
        AlertDialog(
            onDismissRequest = { if (!deleting) deleteOwn = null },
            title = { Text("Eliminar esdeveniment?") },
            text = { Text("S’eliminarà «${item.title}». Esta acció no es pot desfer.") },
            dismissButton = {
                TextButton(onClick = { deleteOwn = null }, enabled = !deleting) { Text("Cancel·lar") }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleting = true
                        state.deleteOwnSubmittedEvent(item.eventId) { ok, msg ->
                            deleting = false
                            actionMessage = msg
                            if (ok) {
                                ownEvents = ownEvents.filterNot { it.eventId == item.eventId }
                                deleteOwn = null
                                if (editingOwn?.eventId == item.eventId) editingOwn = null
                            }
                        }
                    },
                    enabled = !deleting
                ) { Text(if (deleting) "Eliminant…" else "Eliminar", color = MaterialTheme.colorScheme.error) }
            }
        )
    }

    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        PublisherPageHeader(
            "Editar esdeveniments",
            if (authorized) "Els teus esdeveniments" else "Els que has creat amb este compte o dispositiu",
            onBack
        )
        when {
            authorized && editingManaged != null -> {
                ManagedEventEditor(state, editingManaged!!, onDone = { editingManaged = null })
            }
            authorized -> {
                val events = state.editableConcerts()
                    .asSequence()
                    .filter { publisherConcertMatches(it, searchQuery) }
                    .sortedBy { it.date }
                    .distinctBy { it.id }
                    .toList()
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        PublisherSearchField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = "Buscar esdeveniment, artista, poble o recinte…"
                        )
                    }
                    actionMessage?.let { msg ->
                        item { Text(msg, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    if (events.isEmpty()) {
                        item {
                            Text(
                                if (searchQuery.isBlank()) "No tens esdeveniments editables." else "No hi ha resultats",
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(events, key = { it.id }) { concert ->
                            Surface(
                                Modifier.fillMaxWidth().clickable { editingManaged = concert },
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE6E6E6))
                            ) {
                                Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(concert.title, fontWeight = FontWeight.Bold)
                                        Text("${AgendaDateFormat.date(concert.date)} · ${concert.town}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Icon(Icons.Outlined.Edit, contentDescription = "Editar", tint = AgendaBrandOrange)
                                    if (state.canDeleteConcert(concert)) {
                                        IconButton(onClick = { deleteManaged = concert }) {
                                            Icon(Icons.Outlined.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            editingOwn != null -> {
                OwnSubmittedEventEditor(
                    state = state,
                    item = editingOwn!!,
                    onDone = { editingOwn = null },
                    onCreateAccount = onCreateAccount,
                    onInternalSaleAccountRequired = onInternalSaleAccountRequired,
                    onSaved = {
                        editingOwn = null
                        reloadKey++
                    }
                )
            }
            loading -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            else -> {
                val filteredOwnEvents = ownEvents
                    .asSequence()
                    .filter { publisherOwnEventMatches(it, searchQuery) }
                    .sortedBy { it.date }
                    .distinctBy { it.eventId }
                    .toList()
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        PublisherSearchField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = "Buscar esdeveniment, artista, poble o recinte…"
                        )
                    }
                    actionMessage?.let { msg ->
                        item { Text(msg, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    error?.let { item { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) } }
                    if (filteredOwnEvents.isEmpty() && error == null) item {
                        Text(
                            if (searchQuery.isBlank()) "Encara no has creat cap esdeveniment." else "No hi ha resultats",
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(filteredOwnEvents, key = { it.eventId }) { item ->
                        Surface(
                            Modifier.fillMaxWidth().clickable { editingOwn = item },
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE6E6E6))
                        ) {
                            Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.title, fontWeight = FontWeight.Bold)
                                    Text("${AgendaDateFormat.date(item.date)} · ${item.town}", fontSize = 11.sp, color = Color.Gray)
                                    Text(
                                        when (item.reviewStatus.uppercase()) {
                                            "PENDING" -> "Pendent de revisió"
                                            "PENDING_AUDIT" -> "Publicat · pendent d'auditoria"
                                            "APPROVED" -> "Aprovat"
                                            "REJECTED" -> "Rebutjat"
                                            else -> item.reviewStatus
                                        },
                                        fontSize = 10.sp,
                                        color = AgendaBrandOrange
                                    )
                                }
                                Icon(Icons.Outlined.Edit, contentDescription = "Editar", tint = AgendaBrandOrange)
                                IconButton(onClick = { deleteOwn = item }) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ManagedEventEditor(state: AgendaState, concert: Concert, onDone: () -> Unit) {
    var title by remember(concert.id) { mutableStateOf(concert.title) }
    var artists by remember(concert.id) { mutableStateOf(concert.lineup) }
    var date by remember(concert.id) { mutableStateOf(concert.date) }
    var time by remember(concert.id) { mutableStateOf(concert.time) }
    var town by remember(concert.id) { mutableStateOf(concert.town) }
    var townIsNew by remember(concert.id) { mutableStateOf(false) }
    var venue by remember(concert.id) { mutableStateOf(concert.venue) }
    var styles by remember(concert.id) { mutableStateOf(concert.styles.joinToString(", ")) }
    var description by remember(concert.id) { mutableStateOf(concert.description) }
    var ticketDrafts by remember(concert.id) { mutableStateOf<List<EventTicketDraft>>(emptyList()) }
    LaunchedEffect(concert.id) {
        state.loadEventTicketDrafts(concert.id) { drafts, _ -> if (drafts != null) ticketDrafts = drafts }
    }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        item { TextButton(onClick = onDone) { Text("← Tornar a la llista") } }
        item { OutlinedTextField(title, { title = it }, label = { Text("Títol") }, modifier = Modifier.fillMaxWidth()) }
        item { ArtistCatalogSelector(state = state, selected = artists, onSelectedChange = { artists = it }) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EventDatePickerField(
                    value = date,
                    label = "Data · DD/MM/YYYY",
                    onValueChange = { date = it },
                    modifier = Modifier.weight(1f)
                )
                EventTimePickerField(
                    value = time,
                    label = "Hora",
                    onValueChange = { time = it },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item { TownCatalogSelector(state = state, value = town, isNew = townIsNew, onSelected = { value, created -> town = value; townIsNew = created }) }
        item { OutlinedTextField(venue, { venue = it }, label = { Text("Recinte") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(styles, { styles = it }, label = { Text("Estils") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(description, { description = it }, label = { Text("Informació") }, minLines = 4, modifier = Modifier.fillMaxWidth()) }
        item { Text("Venda interna · modalitats d'entrada", fontWeight = FontWeight.Bold) }
        item { EventTicketDraftEditor(drafts = ticketDrafts, onChange = { ticketDrafts = it }) }
        item { Text("Si la venda ja està activa, els preus vigents es gestionen des del panell Professional. Estes modalitats són propostes pendents i no canvien les entrades ja venudes.", fontSize = 12.sp) }
        message?.let { item { Text(it, fontSize = 12.sp) } }
        item {
            Button(
                onClick = {
                    saving = true
                    state.updateConcertGeneralInfo(
                        concertId = concert.id,
                        title = title,
                        artists = artists,
                        organizers = concert.organizers,
                        date = date,
                        time = time,
                        town = town,
                        allowNewTown = townIsNew,
                        venue = venue,
                        styles = styles.split(',', ';', '|').map(String::trim).filter(String::isNotBlank),
                        description = description,
                        ticketPrice = concert.ticketPrice.takeIf { it >= 0 },
                        ticketSalesEnabled = concert.ticketAvailable,
                        ticketUrl = concert.ticketUrl
                    ) { ok, msg ->
                        if (ok) {
                            state.saveEventTicketDrafts(concert.id, ticketDrafts.ticketDraftJson()) { saved, note ->
                                saving = false
                                message = msg + (if (saved) " · Entrades preparades." else " · Modalitats no guardades: $note")
                            }
                        } else { saving = false; message = msg }
                    }
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AgendaBrandOrange)
            ) { Text(if (saving) "Guardant…" else "Guardar canvis", color = Color.Black, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun OwnSubmittedEventEditor(
    state: AgendaState,
    item: ServerApiRepository.OwnSubmittedEvent,
    onDone: () -> Unit,
    onCreateAccount: () -> Unit,
    onInternalSaleAccountRequired: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    var title by remember(item.eventId) { mutableStateOf(item.title) }
    var subtitle by remember(item.eventId) { mutableStateOf(item.subtitle) }
    var artists by remember(item.eventId) { mutableStateOf(item.artists) }
    var organizers by remember(item.eventId) { mutableStateOf(item.organizers.joinToString(", ")) }
    var date by remember(item.eventId) { mutableStateOf(item.date) }
    var endDate by remember(item.eventId) { mutableStateOf(item.endDate) }
    var time by remember(item.eventId) { mutableStateOf(item.time) }
    var endTime by remember(item.eventId) { mutableStateOf(item.endTime) }
    var town by remember(item.eventId) { mutableStateOf(item.town) }
    var townIsNew by remember(item.eventId) { mutableStateOf(false) }
    var venue by remember(item.eventId) { mutableStateOf(item.venue) }
    var styles by remember(item.eventId) { mutableStateOf(item.styles.joinToString(", ")) }
    var description by remember(item.eventId) { mutableStateOf(item.description) }
    var price by remember(item.eventId) { mutableStateOf(item.price.toString()) }
    var ticketMode by remember(item.eventId) {
        mutableStateOf(
            when {
                item.ticketSalePending -> EventTicketMode.AGENDA
                !item.ticketEnabled -> EventTicketMode.NONE
                item.ticketUrl.isNotBlank() -> EventTicketMode.EXTERNAL
                else -> EventTicketMode.AGENDA
            }
        )
    }
    var ticketUrl by remember(item.eventId) { mutableStateOf(item.ticketUrl) }
    var ticketDrafts by remember(item.eventId) { mutableStateOf<List<EventTicketDraft>>(emptyList()) }
    var ticketDraftLoading by remember(item.eventId) { mutableStateOf(true) }
    LaunchedEffect(item.eventId) {
        state.loadEventTicketDrafts(item.eventId) { drafts, err ->
            if (drafts != null) {
                ticketDrafts = drafts
                // L'event ja és públic, però la venda no està activada.
                // Reconéixer una configuració pendent encara que ticket_enabled=0.
                if (drafts.isNotEmpty() && ticketMode == EventTicketMode.NONE) {
                    ticketMode = EventTicketMode.AGENDA
                }
            }
            ticketDraftLoading = false
        }
    }
    var posterUri by remember(item.eventId) { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }
    val poster = rememberImageBitmapFromUris(listOf(posterUri, item.posterUrl), maxDimension = 720)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            posterUri = runCatching { cachePickedImageForEditing(context, uri) }.getOrElse { uri.toString() }
        }
    }

    val finishEdition: () -> Unit = {
        if (ticketMode == EventTicketMode.AGENDA) {
            state.saveEventTicketDrafts(item.eventId, ticketDrafts.ticketDraftJson()) { saved, note ->
                if (!saved) {
                    // L'esdeveniment ja està editat; no fingir que també s'han guardat les entrades.
                    message = "Canvis publicats, però no s'han guardat les modalitats: $note"
                } else {
                    onSaved()
                    state.rememberInternalSaleEvent(item.eventId)
                    onInternalSaleAccountRequired()
                }
            }
        } else {
            onSaved()
            if (ticketMode == EventTicketMode.AGENDA) {
                state.rememberInternalSaleEvent(item.eventId)
                onInternalSaleAccountRequired()
            } else if (state.currentAccount == null) onCreateAccount()
        }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        item { TextButton(onClick = onDone) { Text("← Tornar a la llista") } }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth().clickable {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFFAFAFA),
                border = BorderStroke(1.dp, Color(0xFFE4E4E4))
            ) {
                if (poster != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(bitmap = poster, contentDescription = "Cartell", modifier = Modifier.fillMaxWidth().height(220.dp), contentScale = ContentScale.Fit)
                        Text("Canviar cartell", Modifier.padding(10.dp), color = AgendaBrandOrange, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Add, null, tint = AgendaBrandOrange)
                        Spacer(Modifier.width(8.dp))
                        Text("Afegir o canviar cartell", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        item { OutlinedTextField(title, { title = it }, label = { Text("Títol") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(subtitle, { subtitle = it }, label = { Text("Subtítol") }, modifier = Modifier.fillMaxWidth()) }
        item { ArtistCatalogSelector(state = state, selected = artists, onSelectedChange = { artists = it }) }
        item { OutlinedTextField(organizers, { organizers = it }, label = { Text("Organització") }, modifier = Modifier.fillMaxWidth()) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EventDatePickerField(
                    value = date,
                    label = "Data · DD/MM/YYYY",
                    onValueChange = { date = it },
                    modifier = Modifier.weight(1f)
                )
                EventTimePickerField(
                    value = time,
                    label = "Hora",
                    onValueChange = { time = it },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EventDatePickerField(
                    value = endDate,
                    label = "Data fi · DD/MM/YYYY",
                    onValueChange = { endDate = it },
                    modifier = Modifier.weight(1f),
                    allowClear = true
                )
                EventTimePickerField(
                    value = endTime,
                    label = "Hora fi",
                    onValueChange = { endTime = it },
                    modifier = Modifier.weight(1f),
                    allowClear = true
                )
            }
        }
        item { TownCatalogSelector(state = state, value = town, isNew = townIsNew, onSelected = { value, created -> town = value; townIsNew = created }) }
        item { OutlinedTextField(venue, { venue = it }, label = { Text("Recinte") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(styles, { styles = it }, label = { Text("Estils") }, modifier = Modifier.fillMaxWidth()) }
        item {
            EventTicketModeSelector(
                selected = ticketMode,
                onSelected = { mode ->
                    ticketMode = mode
                    if (mode != EventTicketMode.EXTERNAL) ticketUrl = ""
                }
            )
        }
        if (ticketMode == EventTicketMode.AGENDA) {
            item { EventTicketDraftEditor(drafts = ticketDrafts, onChange = { ticketDrafts = it }) }
            if (ticketDraftLoading) item { Text("Carregant modalitats guardades…") }
        }
        if (ticketMode == EventTicketMode.EXTERNAL) {
            item {
                OutlinedTextField(
                    price, { price = it.filter(Char::isDigit) },
                    label = { Text("Preu €") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }
        if (ticketMode == EventTicketMode.EXTERNAL) {
            item {
                OutlinedTextField(
                    ticketUrl, { ticketUrl = it },
                    label = { Text("Enllaç extern d’entrades *") },
                    placeholder = { Text("https://…") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        item { OutlinedTextField(description, { description = it }, label = { Text("Informació") }, minLines = 4, modifier = Modifier.fillMaxWidth()) }
        if (saving && posterUri != null) item { LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth()) }
        message?.let { item { Text(it, fontSize = 12.sp, color = if (success) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error) } }
        item {
            Button(
                onClick = {
                    saving = true
                    success = false
                    message = null
                    val ticketEnabled = ticketMode != EventTicketMode.NONE
                    val effectiveTicketUrl = if (ticketMode == EventTicketMode.EXTERNAL) ticketUrl.trim() else ""
                    if (ticketMode == EventTicketMode.EXTERNAL && effectiveTicketUrl.isBlank()) {
                        saving = false
                        message = "Indica l’enllaç extern de venda d’entrades."
                        return@Button
                    }
                    state.updateOwnSubmittedEvent(
                        item = item,
                        title = title,
                        subtitle = subtitle,
                        artists = artists,
                        organizers = organizers.split(',', ';', '|').map(String::trim).filter(String::isNotBlank),
                        date = date,
                        endDate = endDate,
                        time = time,
                        endTime = endTime,
                        town = town,
                        venue = venue,
                        category = item.category,
                        styles = styles.split(',', ';', '|').map(String::trim).filter(String::isNotBlank),
                        price = price.toIntOrNull() ?: 0,
                        ticketEnabled = ticketMode == EventTicketMode.EXTERNAL ||
                            (ticketMode == EventTicketMode.AGENDA && item.ticketEnabled && !item.ticketSalePending),
                        ticketUrl = effectiveTicketUrl,
                        ticketSaleOnAgenda = ticketMode == EventTicketMode.AGENDA,
                        description = description
                    ) { ok, msg ->
                        if (!ok) {
                            saving = false
                            success = false
                            message = msg
                        } else if (!posterUri.isNullOrBlank()) {
                            state.updateOwnSubmittedEventPoster(
                                eventId = item.eventId,
                                imageUri = posterUri!!,
                                onProgress = { progress = it }
                            ) { posterOk, posterMsg ->
                                saving = false
                                success = posterOk
                                message = if (posterOk) "$msg $posterMsg" else "$msg $posterMsg"
                                // L'edició principal ja s'ha guardat al servidor.
                                // El registre és posterior per a TOTS els convidats,
                                // independentment de si venen entrades o no.
                                finishEdition()
                            }
                        } else {
                            saving = false
                            success = true
                            message = msg
                            finishEdition()
                        }
                    }
                },
                enabled = !saving && !ticketDraftLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AgendaBrandOrange)
            ) { Text(if (saving) "Guardant…" else "Guardar canvis", color = Color.Black, fontWeight = FontWeight.Bold) }
        }
        item { Text("Els canvis es publiquen immediatament i passen a revisió editorial posterior. Si encara no tens compte, després de guardar s'obrirà Crear compte / Iniciar sessió per conservar la gestió de la publicació. La venda interna s'activarà en vincular-la.", fontSize = 11.sp, color = Color.Gray) }
        item { Spacer(Modifier.height(18.dp)) }
    }
}

@Composable
internal fun TicketLegacyPublisherScreen(
    state: AgendaState,
    contentPadding: PaddingValues,
    onBack: () -> Unit
) {
    // v301: gestor antic també reutilitza la sessió ja autenticada, sense segon login.
    val activeAccountId = state.currentAccount?.let { it.serverUserId ?: it.id }
    if (activeAccountId == null) {
        Box(Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
            Text("Inicia sessió a L’Agenda per gestionar les entrades.")
        }
        return
    }
    var showSelectionHelp by remember { mutableStateOf(true) }
    var selectionHelpStep by remember { mutableStateOf(0) }
    var selectionRect by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var selected by remember { mutableStateOf<Concert?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        PublisherPageHeader("Vendre entrades", "Configura la venda dels teus esdeveniments", onBack)
        if (!state.isAuthorizedPublisher()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Necessites un perfil autoritzat.") }
        } else if (selected == null) {
            val events = state.editableConcerts()
                .asSequence()
                .filter { publisherConcertMatches(it, searchQuery) }
                .sortedBy { it.date }
                .distinctBy { it.id }
                .toList()
            Box(Modifier.fillMaxSize()) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Box(Modifier.onGloballyPositioned { selectionRect = it.boundsInRoot() }) {
                    PublisherSearchField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Buscar esdeveniment, artista, poble o recinte…"
                    )
                    }
                }
                if (events.isEmpty()) {
                    item {
                        Text(
                            if (searchQuery.isBlank()) "No tens esdeveniments disponibles." else "No hi ha resultats",
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(events, key = { it.id }) { concert ->
                        Surface(Modifier.fillMaxWidth().clickable { selected = concert; showSelectionHelp = false }, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Color(0xFFE6E6E6)), color = Color.White) {
                            Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(concert.title, fontWeight = FontWeight.Bold)
                                    Text("${AgendaDateFormat.date(concert.date)} · ${concert.town}", fontSize = 11.sp, color = Color.Gray)
                                }
                                Icon(Icons.Outlined.ConfirmationNumber, null, tint = AgendaBrandOrange)
                            }
                        }
                    }
                }
            }
            if (showSelectionHelp) {
                val guide = listOf(
                    TicketHelp("Busca el festival", "Selecciona un festival que gestiones. Esta pantalla mostra només els esdeveniments que tens permís per editar."),
                    TicketHelp("Obri la configuració", "Pots buscar pel nom, artista o poble. Després toca la targeta del festival per començar les sis pantalles guiades.")
                )
                TicketCoachMark(
                    targetInRoot = selectionRect,
                    help = guide[selectionHelpStep], index = selectionHelpStep, total = guide.size,
                    onNext = { if (selectionHelpStep == guide.lastIndex) showSelectionHelp = false else selectionHelpStep++ },
                    onSkip = { showSelectionHelp = false }
                )
            }
            }
        } else {
            val concert = selected!!
            if (concert.isFestivalExperience) {
                FestivalTicketManagementPanel(state = state, concert = concert, onBack = { selected = null })
            } else {
            var enabled by remember(concert.id) { mutableStateOf(concert.ticketAvailable) }
            var price by remember(concert.id) { mutableStateOf(concert.ticketPrice.takeIf { it >= 0 }?.toString() ?: "0") }
            var url by remember(concert.id) { mutableStateOf(concert.ticketUrl) }
            var saving by remember { mutableStateOf(false) }
            var message by remember { mutableStateOf<String?>(null) }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = { selected = null }) { Text("← Tornar a la llista") }
                Text(concert.title, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Venda activa", modifier = Modifier.weight(1f))
                    Switch(enabled, { enabled = it })
                }
                OutlinedTextField(price, { price = it.filter(Char::isDigit) }, label = { Text("Preu €") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(url, { url = it }, label = { Text("URL externa (opcional)") }, modifier = Modifier.fillMaxWidth())
                Text("Si deixes l'URL buida, la venda pot gestionar-se internament amb QR.", fontSize = 11.sp, color = Color.Gray)
                message?.let { Text(it, fontSize = 12.sp) }
                Button(
                    onClick = {
                        saving = true
                        state.updateTicketSales(concert.id, url, price.toIntOrNull() ?: 0, enabled) { _, msg -> saving = false; message = msg }
                    },
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AgendaBrandOrange)
                ) { Text(if (saving) "Guardant…" else "Guardar entrades", color = Color.Black, fontWeight = FontWeight.Bold) }
            }
            }
        }
    }
}

@Composable
private fun AdvertisementPublisherScreen(
    state: AgendaState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onPublished: () -> Unit
) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf<Concert?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var placement by remember { mutableStateOf(AdPlacement.FEED_NORMAL) }
    var placementExpanded by remember { mutableStateOf(false) }
    var duration by remember { mutableStateOf(1) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var showPublishedDialog by remember { mutableStateOf(false) }
    var pendingAdId by remember { mutableStateOf<Long?>(null) }
    val paymentSheet = remember {
        PaymentSheet.Builder { result ->
            when (result) {
                is PaymentSheetResult.Completed -> {
                    val adId = pendingAdId
                    if (adId != null) {
                        state.finalizeAdvertisementPayment(adId) { ok, msg ->
                            busy = false
                            if (ok) {
                                message = null
                                showPublishedDialog = true
                            } else {
                                message = msg
                            }
                        }
                    } else {
                        busy = false; message = "Falta l'identificador de l'anunci."
                    }
                }
                is PaymentSheetResult.Canceled -> { busy = false; message = null }
                is PaymentSheetResult.Failed -> { busy = false; message = result.error.localizedMessage ?: "Stripe ha fallat." }
            }
        }
    }.build()

    if (showPublishedDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Anunci publicat") },
            text = { Text("L’anunci s’ha publicat correctament.") },
            confirmButton = {
                Button(
                    onClick = {
                        showPublishedDialog = false
                        pendingAdId = null
                        onPublished()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AgendaBrandOrange)
                ) {
                    Text("OK", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Desbloqueig per autoria real, no pel rol. S'executa cada vegada que s'obri "+".
    // La resposta inclou convidats (guest_id) i comptes (token).
    var ownPublishedEvents by remember(state.currentAccount?.serverUserId) {
        mutableStateOf<List<ServerApiRepository.OwnSubmittedEvent>>(emptyList())
    }
    LaunchedEffect(state.currentAccount?.serverUserId) {
        state.loadOwnSubmittedEvents { items, _ ->
            ownPublishedEvents = items.filter { it.status.equals("published", true) }
        }
    }
    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        PublisherPageHeader("Crear anunci", "Promociona un dels teus esdeveniments", onBack)
        if (state.currentAccount == null) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Pots promocionar els esdeveniments que has publicat sense compte. Per contractar un anunci i pagar-lo, inicia sessió; conservarem la identificació d'este dispositiu.")
                Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) { Text("Iniciar sessió per crear anunci") }
            }
        } else if (ownPublishedEvents.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Primer has de publicar un esdeveniment amb este compte.") }
        } else if (selected == null) {
            // No dependre del feed: un esdeveniment propi pot estar fora del radi o dels filtres.
            val fromMyPublications = ownPublishedEvents.map { item ->
                state.concertById(item.eventId) ?: Concert(
                    id = item.eventId,
                    artist = item.artists.firstOrNull() ?: item.title,
                    title = item.title,
                    subtitle = item.subtitle,
                    lineup = item.artists,
                    date = item.date,
                    time = item.time,
                    endDate = item.endDate,
                    endTime = item.endTime,
                    town = item.town,
                    comarca = item.comarca,
                    venue = item.venue,
                    styles = item.styles,
                    price = item.price,
                    language = "Valencià",
                    description = item.description,
                    category = item.category,
                    posterUri = item.posterUrl,
                    ticketAvailable = item.ticketEnabled,
                    ticketUrl = item.ticketUrl
                )
            }
            val events = ((if (state.isAuthorizedPublisher()) state.editableConcerts() else emptyList()) + fromMyPublications)
                .asSequence()
                .filter { publisherConcertMatches(it, searchQuery) }
                .sortedBy { it.date }
                .distinctBy { it.id }
                .toList()
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    PublisherSearchField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Buscar esdeveniment, artista, poble o recinte…"
                    )
                }
                if (events.isEmpty()) {
                    item {
                        Text(
                            if (searchQuery.isBlank()) "No tens esdeveniments disponibles." else "No hi ha resultats",
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(events, key = { it.id }) { concert ->
                        Surface(Modifier.fillMaxWidth().clickable { selected = concert }, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Color(0xFFE6E6E6)), color = Color.White) {
                            Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(concert.title, fontWeight = FontWeight.Bold)
                                    Text("${AgendaDateFormat.date(concert.date)} · ${concert.town}", fontSize = 11.sp, color = Color.Gray)
                                }
                                Icon(Icons.Outlined.Campaign, null, tint = AgendaBrandOrange)
                            }
                        }
                    }
                }
            }
        } else {
            val concert = selected!!
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = { selected = null }) { Text("← Tornar a la llista") }
                Text(concert.title, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Box(Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { placementExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text("Ubicació · ${placement.label}") }
                    DropdownMenu(expanded = placementExpanded, onDismissRequest = { placementExpanded = false }) {
                        AdPlacement.entries.forEach { item -> DropdownMenuItem(text = { Text(item.label) }, onClick = { placement = item; placementExpanded = false }) }
                    }
                }
                Text("Duració", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 3, 7).forEach { days ->
                        FilterChip(selected = duration == days, onClick = { duration = days }, label = { Text("$days ${if (days == 1) "dia" else "dies"}") })
                    }
                }
                Text("L’anunci es mostrarà a tots els usuaris de l’Agenda mentre estiga pagat i vigent, independentment dels gustos, els seguiments o la localització.", fontSize = 11.sp, color = Color.Gray)
                message?.let { Text(it, fontSize = 12.sp) }
                Button(
                    onClick = {
                        busy = true; message = null
                        state.createAdvertisementPayment(
                            concertId = concert.id,
                            placement = placement,
                            durationDays = duration,
                            targetStyles = emptySet(),
                            targetSearchTerms = emptySet(),
                            targetTowns = emptySet(),
                            targetComarques = emptySet(),
                            targetFollowers = false,
                            targetHighIntent = false,
                            matchAllCriteria = false
                        ) { start, error ->
                            if (start == null) {
                                busy = false; message = error ?: "No s'ha pogut iniciar l'anunci."
                            } else {
                                pendingAdId = start.adId
                                runCatching {
                                    PaymentConfiguration.init(context, start.publishableKey)
                                    paymentSheet.presentWithPaymentIntent(
                                        start.clientSecret,
                                        PaymentSheet.Configuration.Builder(start.merchantName.ifBlank { "l'Agenda" }).build()
                                    )
                                }.onFailure { busy = false; message = it.localizedMessage ?: "No s'ha pogut obrir Stripe." }
                            }
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AgendaBrandOrange)
                ) { Text(if (busy) "Connectant…" else "Continuar al pagament", color = Color.Black, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun ArtistCommerceManagementScreen(
    state: AgendaState,
    contentPadding: PaddingValues,
    onBack: () -> Unit
) {
    val managedArtists = state.managedArtists()
    var searchQuery by remember { mutableStateOf("") }
    val filteredArtists = if (searchQuery.isBlank()) managedArtists else {
        val key = catalogLookupKey(searchQuery)
        managedArtists.filter { catalogLookupKey(it).contains(key) }
    }
    var selectedArtist by remember { mutableStateOf<String?>(managedArtists.firstOrNull()) }

    LaunchedEffect(managedArtists, searchQuery) {
        if (selectedArtist !in filteredArtists) selectedArtist = filteredArtists.firstOrNull()
    }

    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        PublisherPageHeader(
            "Discos, merch i donacions",
            "Gestiona la botiga i les aportacions dels grups vinculats al teu compte",
            onBack
        )
        if (managedArtists.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Encara no tens cap grup verificat vinculat a este compte.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    PublisherSearchField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Buscar grup…"
                    )
                }
                if (filteredArtists.isEmpty()) {
                    item {
                        Text(
                            "No hi ha resultats",
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    if (managedArtists.size > 1 || searchQuery.isNotBlank()) {
                        item {
                            Text("Grup", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            LazyRow(
                                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                items(filteredArtists, key = { it }) { artist ->
                                    FilterChip(
                                        selected = selectedArtist == artist,
                                        onClick = { selectedArtist = artist },
                                        label = { Text(artist) }
                                    )
                                }
                            }
                        }
                    }
                    selectedArtist?.takeIf { it in filteredArtists }?.let { artist ->
                        item {
                            if (managedArtists.size == 1) {
                                Text(artist, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            }
                            Text(
                                "Afig discos o merch, revisa comandes i activa les donacions del grup.",
                                modifier = Modifier.padding(top = 2.dp, bottom = 2.dp),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            GroupMerchSection(state = state, artist = artist)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfessionalAccessRequestScreen(
    state: AgendaState,
    contentPadding: PaddingValues,
    onBack: () -> Unit
) {
    var kind by remember { mutableStateOf("ARTIST") }
    var entityName by remember { mutableStateOf("") }
    var artistQuery by remember { mutableStateOf("") }
    var relation by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var messageText by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }

    val knownArtists = state.knownArtistNames()
    val normalizedArtistQuery = catalogLookupKey(artistQuery)
    val artistResults = if (normalizedArtistQuery.isBlank()) emptyList() else knownArtists
        .asSequence()
        .filter { catalogLookupKey(it).contains(normalizedArtistQuery) }
        .take(8)
        .toList()
    val alreadyManaged = entityName.isNotBlank() && state.canManageArtist(entityName)

    LaunchedEffect(kind) {
        entityName = ""
        artistQuery = ""
        result = null
        success = false
    }

    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        PublisherPageHeader("Soc organitzador o artista", "La verificació és manual: et contactarem nosaltres.", onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Quin tipus de perfil representes?", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = kind == "ARTIST", onClick = { kind = "ARTIST" }, label = { Text("Artista / grup") })
                FilterChip(selected = kind == "ORGANIZER", onClick = { kind = "ORGANIZER" }, label = { Text("Organitzador") })
            }

            if (kind == "ARTIST") {
                Text(
                    "Busca el grup en l’Agenda i selecciona el perfil que vols reclamar.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = artistQuery,
                    onValueChange = { value ->
                        artistQuery = value
                        if (!value.equals(entityName, ignoreCase = true)) entityName = ""
                    },
                    label = { Text("Buscar artista o grup *") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (entityName.isNotBlank()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(13.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f)) {
                                Text(entityName, fontWeight = FontWeight.Bold)
                                Text(
                                    if (alreadyManaged) "Este grup ja està vinculat al teu compte." else "Perfil seleccionat",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else if (normalizedArtistQuery.isNotBlank()) {
                    if (artistResults.isEmpty()) {
                        Text(
                            "No trobe cap grup amb este nom. La reclamació només es pot fer sobre un perfil existent.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            artistResults.forEach { artist ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        entityName = artist
                                        artistQuery = artist
                                        result = null
                                        success = false
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .32f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .14f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ArtistAvatar(artist, 38.dp, state.publicPhotoForArtist(artist))
                                        Spacer(Modifier.width(10.dp))
                                        Text(artist, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                        Text("Seleccionar", fontSize = 11.sp, color = AgendaBrandOrange, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    entityName,
                    { entityName = it },
                    label = { Text("Nom de l'organització *") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                relation,
                { relation = it },
                label = { Text("Quina relació tens? *") },
                placeholder = { Text("Membre, mànager, responsable…") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(contact, { contact = it }, label = { Text("Correu o telèfon de contacte *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(messageText, { messageText = it }, label = { Text("Informació addicional") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            Text(
                "El perfil no s’assigna automàticament. El superuser revisarà la sol·licitud i podrà aprovar-la o rebutjar-la; el contacte servirà per comprovar la vinculació.",
                fontSize = 11.sp,
                color = Color.Gray
            )
            result?.let { Text(it, fontSize = 12.sp, color = if (success) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error) }
            Button(
                onClick = {
                    sending = true
                    result = null
                    success = false
                    state.requestProfessionalAccess(entityName, kind, relation, contact, messageText) { ok, msg ->
                        sending = false
                        success = ok
                        result = msg
                    }
                },
                enabled = !sending && entityName.isNotBlank() && !alreadyManaged,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AgendaBrandOrange)
            ) {
                Text(
                    if (sending) "Enviant…" else if (kind == "ARTIST") "Reclamar grup" else "Enviar sol·licitud",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CommunityPublisherReviewScreen(
    state: AgendaState,
    contentPadding: PaddingValues,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var queue by remember { mutableStateOf<List<ServerApiRepository.CommunityReviewItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var reload by remember { mutableStateOf(0) }
    var busyEventId by remember { mutableStateOf<Int?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(reload, state.currentAccount?.role) {
        if (state.currentAccount?.role != AccountRole.SUPER_ADMIN) {
            loading = false
            return@LaunchedEffect
        }
        loading = true
        val result = withContext(Dispatchers.IO) { ServerApiRepository.pendingCommunityReviews(context) }
        result.onSuccess { queue = it; message = null }
            .onFailure { message = it.message ?: "No s'ha pogut carregar la cua." }
        loading = false
    }

    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        PublisherPageHeader("Revisar publicacions", "Propostes Base i auditories de publicacions directes", onBack)
        when {
            state.currentAccount?.role != AccountRole.SUPER_ADMIN -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Accés reservat a Super Admin.") }
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Carregant…") }
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                message?.let { msg -> item { Text(msg, fontSize = 12.sp, color = MaterialTheme.colorScheme.error) } }
                if (queue.isEmpty()) {
                    item { Text("No hi ha publicacions pendents de comprovació.", color = Color.Gray) }
                }
                items(queue, key = { it.eventId }) { review ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(15.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE6E6E6))
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(review.title, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                    Text("${AgendaDateFormat.date(review.date)} · ${review.town}${review.venue.takeIf(String::isNotBlank)?.let { " · $it" }.orEmpty()}", fontSize = 11.sp, color = Color.Gray)
                                }
                                Text(
                                    if (review.reviewStatus == "PENDING_AUDIT") "AUDITORIA" else "PENDENT",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AgendaBrandOrange
                                )
                            }
                            Text("Enviat per ${review.username} · ${review.publicationMode}", fontSize = 11.sp, color = Color.Gray)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        busyEventId = review.eventId
                                        message = null
                                        scope.launch {
                                            val result = withContext(Dispatchers.IO) { ServerApiRepository.reviewCommunityPublication(context, review.eventId, false) }
                                            busyEventId = null
                                            result.onSuccess { message = it; reload += 1 }
                                                .onFailure { message = it.message ?: "No s'ha pogut rebutjar." }
                                        }
                                    },
                                    enabled = busyEventId == null,
                                    modifier = Modifier.weight(1f)
                                ) { Text("Rebutjar") }
                                Button(
                                    onClick = {
                                        busyEventId = review.eventId
                                        message = null
                                        scope.launch {
                                            val result = withContext(Dispatchers.IO) { ServerApiRepository.reviewCommunityPublication(context, review.eventId, true) }
                                            busyEventId = null
                                            result.onSuccess { message = it; reload += 1 }
                                                .onFailure { message = it.message ?: "No s'ha pogut aprovar." }
                                        }
                                    },
                                    enabled = busyEventId == null,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = AgendaBrandOrange)
                                ) { Text(if (busyEventId == review.eventId) "Revisant…" else "Aprovar", color = Color.Black, fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfessionalClaimReviewScreen(
    state: AgendaState,
    contentPadding: PaddingValues,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var queue by remember { mutableStateOf<List<ServerApiRepository.ProfessionalClaimReviewItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var reload by remember { mutableStateOf(0) }
    var busyClaimId by remember { mutableStateOf<Long?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(reload, state.currentAccount?.role) {
        if (state.currentAccount?.role != AccountRole.SUPER_ADMIN) {
            loading = false
            return@LaunchedEffect
        }
        loading = true
        val result = withContext(Dispatchers.IO) { ServerApiRepository.pendingProfessionalClaims(context) }
        result.onSuccess { queue = it; message = null }
            .onFailure { message = it.message ?: "No s'han pogut carregar les sol·licituds." }
        loading = false
    }

    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        PublisherPageHeader("Sol·licituds professionals", "Verificació manual d'artistes i organitzadors", onBack)
        when {
            state.currentAccount?.role != AccountRole.SUPER_ADMIN -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Accés reservat a Super Admin.") }
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Carregant…") }
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                message?.let { msg -> item { Text(msg, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .72f)) } }
                if (queue.isEmpty()) item { Text("No hi ha sol·licituds professionals pendents.", color = Color.Gray) }
                items(queue, key = { it.claimId }) { claim ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(15.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE6E6E6))
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(claim.entityName.ifBlank { claim.matchedProfileName.ifBlank { "Perfil sense nom" } }, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Text(if (claim.profileType == "GROUP") "Artista / grup" else "Organitzador", fontSize = 11.sp, color = AgendaBrandOrange, fontWeight = FontWeight.Bold)
                            Text("Compte: ${claim.username}${claim.accountEmail.takeIf(String::isNotBlank)?.let { " · $it" }.orEmpty()}", fontSize = 11.sp)
                            if (claim.relation.isNotBlank()) Text("Relació: ${claim.relation}", fontSize = 11.sp)
                            val contact = claim.contactEmail.ifBlank { claim.contactPhone }
                            if (contact.isNotBlank()) Text("Contacte: $contact", fontSize = 11.sp)
                            if (claim.message.isNotBlank()) Text(claim.message, fontSize = 11.sp, color = Color.Gray)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        busyClaimId = claim.claimId
                                        message = null
                                        scope.launch {
                                            val result = withContext(Dispatchers.IO) { ServerApiRepository.reviewProfessionalClaim(context, claim.claimId, false) }
                                            busyClaimId = null
                                            result.onSuccess { message = it; reload += 1 }
                                                .onFailure { message = it.message ?: "No s'ha pogut rebutjar." }
                                        }
                                    },
                                    enabled = busyClaimId == null,
                                    modifier = Modifier.weight(1f)
                                ) { Text("Rebutjar") }
                                Button(
                                    onClick = {
                                        busyClaimId = claim.claimId
                                        message = null
                                        scope.launch {
                                            val result = withContext(Dispatchers.IO) { ServerApiRepository.reviewProfessionalClaim(context, claim.claimId, true) }
                                            busyClaimId = null
                                            result.onSuccess { message = it; reload += 1 }
                                                .onFailure { message = it.message ?: "No s'ha pogut aprovar." }
                                        }
                                    },
                                    enabled = busyClaimId == null,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = AgendaBrandOrange)
                                ) { Text(if (busyClaimId == claim.claimId) "Revisant…" else "Aprovar", color = Color.Black, fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactBottomMenu(
    state: AgendaState,
    selectedSection: HomeSection,
    onSelectSection: (HomeSection) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(52.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomMenuButton(
                label = "Inici",
                icon = "⌂",
                selected = selectedSection == HomeSection.VISUALITZACIO,
                onClick = { onSelectSection(HomeSection.VISUALITZACIO) },
                modifier = Modifier.weight(1f)
            )
            BottomMenuButton(
                label = "Guardats",
                icon = "🔖",
                selected = selectedSection == HomeSection.GUARDATS,
                onClick = { onSelectSection(HomeSection.GUARDATS) },
                modifier = Modifier.weight(1f)
            )
            BottomMenuButton(
                label = "Entrades",
                icon = "🎟",
                selected = selectedSection == HomeSection.ENTRADES,
                onClick = { onSelectSection(HomeSection.ENTRADES) },
                modifier = Modifier.weight(1f)
            )
            BottomMenuButton(
                label = "Comunitat",
                icon = "👥",
                selected = selectedSection == HomeSection.COMUNITAT,
                onClick = { onSelectSection(HomeSection.COMUNITAT) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BottomMenuButton(
    label: String,
    icon: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(horizontal = 2.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .10f) else Color.Transparent
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(icon, fontSize = 15.sp)
            Spacer(Modifier.width(5.dp))
            Text(
                label,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = .72f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeFeed(
    state: AgendaState,
    feedSessionEpoch: Int,
    batecsReturnEpoch: Int,
    homeSection: HomeSection,
    contentPadding: PaddingValues,
    onFilter: () -> Unit,
    onOpenStory: (Story, List<Story>) -> Unit,
    onOpenConcert: (Concert) -> Unit,
    onOpenFestivalArtist: (Concert, String, String?) -> Unit,
    onOpenAdvertisement: (Concert) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenSpecialEvent: (Concert) -> Unit,
    focusedTicketConcertId: Int?,
    focusedFeedConcertId: Int?,
    onSearchApplied: () -> Unit
) {
    // v278 pendent: mentre s'escriu una cerca, ni una sola targeta del catàleg
    // sense filtre pot ocupar la pantalla abans dels 220 ms de debounce.
    var searchDraftPending by remember { mutableStateOf(false) }
    // Una cerca NO es resol sobre el preview de dos mesos de «Per a tu».
    // Espere que acabe de carregar-se el catàleg general abans de mostrar coincidències.
    val fullSearchCataloguePending = homeSection == HomeSection.VISUALITZACIO &&
        state.isGlobalSearchCatalogLoading
    val criteriaPending = homeSection == HomeSection.VISUALITZACIO &&
        (searchDraftPending || state.preparedFeedCriteriaRevision != state.feedCriteriaRevision ||
            fullSearchCataloguePending)
    val preparedConcerts = if (criteriaPending) emptyList() else state.visibleConcertsState
    // v331: l'executor ja ha aplicat la cerca estricta. Tornar a executar-la
    // en Compose (fil principal) congelava el cercador just quan arribaven resultats.
    // preparedFeedCriteriaRevision garantix que no es mostre una cerca anterior.
    val baseConcerts = preparedConcerts
    // v273: «Sense filtre» és la font de dades, no un permís per afegir
    // esdeveniments aliens als criteris sol·licitats per l’usuari.
    // Els anuncis pagats tenen els seus emplaçaments patrocinats propis.
    val hasActiveSearchCriteria = searchDraftPending || state.search.isNotBlank() || !state.filters.isEmpty()
    var selectedBottomCommunityGroupId by remember { mutableStateOf<Int?>(null) }
    val sectionConcerts = when (homeSection) {
        HomeSection.VISUALITZACIO -> baseConcerts
        // Guardats utilitza el seu propi conjunt d'IDs sense filtres de cerca/opcions.
        HomeSection.GUARDATS -> emptyList()
        HomeSection.ENTRADES -> baseConcerts.filter { it.id in state.ticketPurchases }
        HomeSection.COMUNITAT -> baseConcerts
    }
    // 10fz: el feed que s'està llegint no es reconstrueix pel simple fet que
    // tornen resultats de xarxa o que es reprenga l'Activity després de compartir.
    // Els canvis EXPLÍCITS de mode, categoria, filtres o cerca sí obrin una nova vista.
    val readingViewKey = buildString {
        append("v259|") // nova arquitectura de pestanyes: no heretar instantànies velles
        append(state.currentAccount?.username?.lowercase() ?: "guest")
        append('|').append(state.feedCriteriaRevision) // una selecció explícita crea una vista neta
        append('|').append(state.feedMode.name)
        append('|').append(state.selectedFeedCategory?.name.orEmpty())
        append('|').append(state.filters.hashCode())
        append('|').append(state.search)
    }
    var manualRefreshEpoch by remember { mutableStateOf(0) }
    // v330: el gest manual de «Per a tu» és independent de la sincronització
    // periòdica. El token evita llançar diversos refrescs amb estirades seguides.
    var pendingManualForYouRefresh by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(state.completedReadingRefreshToken, pendingManualForYouRefresh) {
        if (pendingManualForYouRefresh != null &&
            state.completedReadingRefreshToken == pendingManualForYouRefresh) {
            pendingManualForYouRefresh = null
        }
    }
    // Seguretat: mai deixar una línia d'actualització permanent per un error
    // imprevist. El càlcul ja comunica els errors a l'estat (35 segons).
    LaunchedEffect(pendingManualForYouRefresh) {
        val requested = pendingManualForYouRefresh
        if (requested != null) {
            delay(45_000L)
            if (pendingManualForYouRefresh == requested) pendingManualForYouRefresh = null
        }
    }
    val readingEpoch = "$feedSessionEpoch:$manualRefreshEpoch"
    // v259: només persistim la posició de lectura. La selecció de targetes sempre
    // prové de l'estat actual: un concert nou d'un artista seguit no pot quedar
    // amagat darrere d'una instantània d'una hora.
    val readingContext = LocalContext.current.applicationContext
    val readingHydrated = state.isFeedReadyForReading
    val stableSectionConcerts = sectionConcerts
    val focusedFeedConcert = if (homeSection == HomeSection.VISUALITZACIO &&
        state.feedMode in setOf(FeedMode.FOR_YOU, FeedMode.UNFILTERED) &&
        !hasActiveSearchCriteria && focusedFeedConcertId != null) {
        state.concertById(focusedFeedConcertId)
    } else null
    // Una notificació navega a Inici, no a la fitxa inferior. L'esdeveniment es posa
    // temporalment al capdamunt del feed i queda desplegat, encara que no formara part
    // del lot visible actual de Per a tu.
    val artistSearchMatch = remember(
        homeSection, state.search, state.selectedFeedSearchType,
        state.selectedFeedSearchArtist, state.searchSuggestionRevision
    ) {
        if (homeSection == HomeSection.VISUALITZACIO && state.search.isNotBlank() &&
            (state.selectedFeedSearchType == null || state.selectedFeedSearchType == FeedSearchSuggestionType.ARTIST))
            state.selectedFeedSearchArtist ?: ArtistSearchPriority.resolve(state.search, state.knownArtistNames())
        else null
    }
    // v283: els anuncis contractats disposen dels seus propis espais.
    // No inserir mai targetes per garanties comercials, venda interna o externa,
    // ni per prioritat rebuda del servidor. «Per a tu» és estrictament orgànic.
    // v327: filtre O(N) només quan canvia el catàleg, la vista o una exclusió.
    // Les recomposicions per scroll, avisos, socials i publicitat ja no tornen a
    // construir tota la llista de cards. Les claus inclouen TOTES les exclusions.
    val concerts = remember(
        stableSectionConcerts, focusedFeedConcert, homeSection, state.feedMode,
        hasActiveSearchCriteria, state.hiddenConcertIds, state.blockedArtists,
        state.blockedCategories, state.blockedSubcategories, state.feedRenderRevision
    ) {
        (if (focusedFeedConcert != null) {
            listOf(focusedFeedConcert) + stableSectionConcerts.filterNot { it.id == focusedFeedConcert.id }
        } else stableSectionConcerts).filter { event ->
            // No incloure reserves internes gratuïtes, ni canviar la política d'anuncis.
            if (homeSection == HomeSection.VISUALITZACIO && state.feedMode == FeedMode.FOR_YOU &&
                !hasActiveSearchCriteria && state.isFreeInternalAgendaReservation(event)) return@filter false
            state.isEventVisibleForUser(event)
        }
    }
    // El cercador d'artistes treballa amb el catàleg complet, no només amb el primer lot del feed.
    LaunchedEffect(artistSearchMatch) {
        if (!artistSearchMatch.isNullOrBlank()) state.ensureFullAgendaLoaded("artist_search")
    }
    val festivalSearchIds = if (artistSearchMatch != null) concerts.asSequence()
        .filter { it.isFestivalExperience && matchedArtistInFestival(it, artistSearchMatch) != null }
        .map { it.id }.distinct().toList() else emptyList()
    // Demanem els horaris reals només dels festivals presents a la cerca.
    // El canvi de dades al servidor actualitza el llistat sense crear targetes de dies falsos.
    LaunchedEffect(artistSearchMatch, festivalSearchIds) {
        festivalSearchIds.filter { state.specialEventRemote(it) == null }.forEach { id -> state.refreshSpecialEvent(id) }
    }
    // v286: els resultats normals i els de festival han de tindre claus úniques.
    // No llevem jornades diferents: ArtistConcertSearchRow.key inclou feedInstanceKey.
    val firstArtistSuggestion = artistSearchMatch?.takeUnless {
        ArtistSearchPriority.isFollowing(it, state.followedArtists)
    }
    // v261: el feed no s'inicialitza amb un lot de 20. Compose continua usant
    // LazyColumn per crear visualment només les targetes que entren en pantalla.
    val progressiveForYou = homeSection == HomeSection.VISUALITZACIO &&
        state.feedMode == FeedMode.FOR_YOU && state.search.isBlank()
    val restoredPosition = remember(readingViewKey, readingEpoch, homeSection) {
        FeedReadingSessionStore.loadPosition(readingContext, readingViewKey)
    }
    var visibleConcertLimit by remember(readingViewKey, readingEpoch, homeSection) {
        // L'índex de LazyColumn inclou capçalera + Batecs + resultats.
        // Restaurar una posició avançada exigeix tindre carregat prou lot de cards.
        mutableStateOf(if (homeSection == HomeSection.VISUALITZACIO) {
            maxOf(FEED_PAGE_SIZE, (restoredPosition?.index ?: 0) + FEED_SCROLL_BATCH + 6)
        } else FEED_PAGE_SIZE)
    }
    LaunchedEffect(homeSection) {
        when (homeSection) {
            // Guardats/Entrades poden contindre IDs fora de la finestra futura inicial.
            // El catàleg complet només es materialitza perquè l'usuari ha demanat eixa vista.
            HomeSection.GUARDATS -> Unit // les llistes es refresquen dins el panell, després del primer frame
            HomeSection.ENTRADES -> {
                // QR/entrades locals primer. Només hidratar el catàleg quan falta una fitxa.
                if (state.ticketPurchases.any { state.concertById(it) == null })
                    state.ensureFullAgendaLoaded("tickets_missing_ids")
            }
            HomeSection.COMUNITAT -> {
                // No refrescar també Guardats: són dominis independents.
                kotlinx.coroutines.delay(300)
                state.refreshCommunityGroups(mine = true)
            }
            HomeSection.VISUALITZACIO -> Unit
        }
    }
    LaunchedEffect(homeSection, state.feedMode) {
        if (homeSection == HomeSection.VISUALITZACIO && state.feedMode == FeedMode.FOR_YOU) {
            // v324: primer pintar el snapshot + seguits des de SQLite. El catàleg
            // complet es carrega diferidament per incloure també els concerts
            // futurs fora dels dos mesos, sense bloquejar el primer scroll.
            delay(8_000)
            state.ensureFullAgendaLoaded("v324_followed_future_background")
        }
    }
    // v333: «Sense filtre» no convertix MILERS de concerts en files Compose abans
    // de dibuixar la primera targeta. Només les visibles + un bloc de reserva.
    // La font completa continua en `concerts`; no perdem esdeveniments ni cerques.
    val fastUnfilteredRows = homeSection == HomeSection.VISUALITZACIO &&
        state.feedMode == FeedMode.UNFILTERED && artistSearchMatch == null &&
        state.search.isBlank() && state.filters.isEmpty()
    val artistResults = remember(concerts, artistSearchMatch, state.specialEventsRemote,
        fastUnfilteredRows, visibleConcertLimit) {
        when {
            artistSearchMatch != null -> buildArtistConcertSearchRows(concerts, artistSearchMatch) { event ->
                val remote = state.specialEventRemote(event.id)?.let { remoteSpecialConfig(it, event).schedule }.orEmpty()
                val local = specialEventConfigFor(event)?.schedule.orEmpty()
                remote to local
            }
            fastUnfilteredRows -> concerts.asSequence()
                .map { ArtistConcertSearchRow(it) }
                .distinctBy(ArtistConcertSearchRow::key)
                .take(visibleConcertLimit + FEED_SCROLL_BATCH)
                .toList()
            else -> concerts.map { ArtistConcertSearchRow(it) }.distinctBy(ArtistConcertSearchRow::key)
        }
    }
    // És un recompte conservador quan algun UID es repeteix; evita escanejar tots
    // els models només per calcular la mida i no altera l'ordre ni la paginació.
    val totalArtistResultsCount = if (fastUnfilteredRows) concerts.size else artistResults.size
    val pagedArtistResults = artistResults.take(visibleConcertLimit)
    // L'índex local pot ser inicialment parcial, però al final entra el catàleg complet.
    LaunchedEffect(visibleConcertLimit, totalArtistResultsCount, homeSection) {
        if (homeSection == HomeSection.VISUALITZACIO && totalArtistResultsCount >= 100 &&
            visibleConcertLimit + 30 >= totalArtistResultsCount) {
            state.requestMoreAgendaForFeed()
        }
    }
    // No advertir «pocs resultats» quan encara falta hidratar el catàleg complet.
    val showLimitedResultsWarning = homeSection == HomeSection.VISUALITZACIO &&
        !criteriaPending && state.search.isBlank() && !state.filters.isEmpty() && state.isFeedReadyForReading && concerts.size <= 5
    // Els recomptes socials es demanen des del primer render per al primer bloc
    // complet del feed, no només quan l'usuari toca Guardar/M'agrada.
    val visibleSocialIds = remember(concerts, visibleConcertLimit) {
        concerts.take(visibleConcertLimit).map { it.id }
    }
    LaunchedEffect(visibleSocialIds) {
        // 10eo: els comptadors no competeixen amb la primera pintura ni amb les imatges.
        delay(1_800)
        state.refreshSocialCounts(visibleSocialIds)
        while (true) {
            // El like/guardar propi continua actualitzant-se immediatament. El polling només
            // sincronitza canvis d’altres usuaris i no cal despertar el feed cada 30 segons.
            delay(180_000)
            state.refreshSocialCounts(visibleSocialIds)
        }
    }
    // El canvi de data civil també invalida els batecs, sense esperar a
    // reiniciar l'app ni al límit cultural de les 05:00.
    var adClock by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            adClock = System.currentTimeMillis()
        }
    }
    val batecCivilDay = remember(adClock) {
        java.time.Instant.ofEpochMilli(adClock)
            .atZone(java.time.ZoneId.of("Europe/Madrid")).toLocalDate()
    }
    // Batecs és independent del mode/filtres de feed. Canviar entre Per a tu,
    // Seguits, Sense filtre o Categoria NO reinicia ni buida el carrusel.
    // Només reaccionem a dades pròpies de Batecs/seguiments/vistos.
    val batecStateKey = remember(state.stories, state.followedArtists, state.blockedArtists, state.seenBatecKeys, state.batecAgendaRevision, batecCivilDay) {
        listOf(
            state.stories.hashCode(),
            state.followedArtists.hashCode(),
            state.seenBatecKeys.hashCode(),
            state.blockedArtists.hashCode(),
            state.batecAgendaRevision.hashCode(),
            batecCivilDay.hashCode()
        ).hashCode()
    }
    val instantBatecs = remember(state.startupBatecSnapshot, state.followedArtists, state.blockedArtists, state.seenBatecKeys, batecCivilDay) {
        state.visibleStartupBatecs()
    }
    val allStories by produceState<List<Story>>(
        initialValue = instantBatecs,
        key1 = homeSection,
        key2 = batecStateKey,
        // v328: el snapshot arriba asíncronament. Sense esta clau, el carrusel
        // podia continuar buit fins al final del càlcul complet.
        key3 = instantBatecs
    ) {
        if (homeSection != HomeSection.VISUALITZACIO) {
            value = emptyList()
        } else {
            // La caché local ja és suficient per pintar sis artistes seguits.
            // No esperem cap descàrrega ni recàlcul per ensenyar-la.
            if (instantBatecs.isNotEmpty()) value = instantBatecs
            // Reconstrucció de novetats en segon pla, sense competir amb la UI.
            delay(if (instantBatecs.isNotEmpty()) 6_000L else 1_400L)
            val recalculated = withContext(Dispatchers.Default) {
                state.activeStories()
            }
            // No fem parpellejar el carrusel a buit mentre encara s'estan hidratant
            // concerts/seguiments; el snapshot antic dura només fins tindre resultat nou.
            // Un resultat buit pot ser legítim: tots els batecs s'han vist.
            value = recalculated
        }
    }
    val allBatecGroups = remember(allStories, state.followedArtists, state.blockedArtists, batecCivilDay) {
        groupBatecsByArtist(state.currentBatecsForDisplay(allStories))
    }
    // v248: sis artistes exactes, només si se'n seguixen més de sis.
    val batecGroups = allBatecGroups.take(MIN_BATEC_CAROUSEL_ITEMS)
    LaunchedEffect(homeSection) {
        if (homeSection == HomeSection.VISUALITZACIO) {
            while (true) {
                delay(10L * 60L * 1000L)
                state.refreshAdvertisementsFromServer()
            }
        }
    }
    val activeAdvertisements = if (homeSection == HomeSection.VISUALITZACIO) {
        state.activeAdvertisementsForCurrentUser(adClock)
    } else emptyList()
    // v255: publicitat pagada GLOBAL; cap preferència territorial, categoria,
    // bloqueig ni mode de feed pot ocultar una campanya activa. La priorització
    // d'esdeveniments és un flux diferent i no toca les ubicacions contractades.
    val audienceAdvertisements = if (state.search.isNotBlank()) emptyList() else activeAdvertisements
    val headerAdvertisements = audienceAdvertisements.filter { it.placement == AdPlacement.HEADER }
    val premiumAdvertisements = audienceAdvertisements.filter { it.placement == AdPlacement.FEED_PREMIUM }
    val normalAdvertisements = audienceAdvertisements.filter { it.placement == AdPlacement.FEED_NORMAL }
    val popupCandidate = audienceAdvertisements.firstOrNull { it.placement == AdPlacement.POPUP }
    var popupAdvertisement by remember { mutableStateOf<EventAdvertisement?>(null) }
    LaunchedEffect(homeSection, state.ticketPurchases) {
        // Els espais especials/entrades no competeixen amb l'arrancada d'Inici.
        if (homeSection == HomeSection.ENTRADES) {
            state.ticketPurchases.forEach { state.refreshSpecialEvent(it) }
        }
    }
    val specialAccessConcerts = if (homeSection == HomeSection.ENTRADES) {
        state.ticketPurchases
            .asSequence()
            .mapNotNull { purchasedId -> state.concerts.firstOrNull { it.id == purchasedId } }
            .filter { state.specialEventRemote(it.id)?.enabled == true || specialEventConfigFor(it) != null }
            .distinctBy { it.id }
            .toList()
    } else emptyList()

    val popupConcert = popupCandidate?.let(state::concertForAdvertisement)
    LaunchedEffect(popupCandidate?.id, popupConcert?.id) {
        val ad = popupCandidate
        if (ad != null && popupConcert != null && state.shouldShowPopupAdvertisement(ad.id)) {
            state.markPopupAdvertisementShown(ad.id)
            popupAdvertisement = ad
        }
    }

    // El text del cercador viu en un subcomposable propi. D'esta manera cada tecla
    // no recompon el feed complet; l'estat global només canvia quan acaba el debounce.
    val feedListState = remember(readingViewKey, readingEpoch, homeSection) { LazyListState() }
    var scrollPositionRestored by remember(readingViewKey, readingEpoch, homeSection) {
        mutableStateOf(restoredPosition == null || homeSection != HomeSection.VISUALITZACIO)
    }
    LaunchedEffect(readingViewKey, readingEpoch, homeSection, pagedArtistResults.size, readingHydrated) {
        val saved = restoredPosition
        if (!scrollPositionRestored && saved != null && homeSection == HomeSection.VISUALITZACIO) {
            val count = feedListState.layoutInfo.totalItemsCount
            if (count > saved.index || (readingHydrated && count > 0)) {
                feedListState.scrollToItem(saved.index.coerceAtMost(count - 1).coerceAtLeast(0), saved.offset)
                scrollPositionRestored = true
            }
        }
    }
    // Desem també l'offset (píxels dins de la card), no únicament el número de card.
    // Debounce per no escriure a disc en cada píxel de scroll.
    LaunchedEffect(readingViewKey, readingEpoch, feedListState, scrollPositionRestored, homeSection) {
        if (scrollPositionRestored && homeSection == HomeSection.VISUALITZACIO) {
            snapshotFlow { feedListState.firstVisibleItemIndex to feedListState.firstVisibleItemScrollOffset }
                .onEach { (index, offset) ->
                    FeedReadingSessionStore.cachePosition(readingViewKey, index, offset)
                }
                .debounce(350L)
                .collect { (index, offset) ->
                    FeedReadingSessionStore.savePosition(readingContext, readingViewKey, index, offset)
                }
        }
    }
    // En eixir del visor de Batecs retornem SEMPRE al capdamunt, no a la
    // ubicació final de l'artista que acaba de passar a «vist». La posició
    // també queda desada per a no reapareixer baix en reprendre l'app.
    LaunchedEffect(batecsReturnEpoch, homeSection) {
        if (batecsReturnEpoch > 0 && homeSection == HomeSection.VISUALITZACIO) {
            feedListState.scrollToItem(0)
            FeedReadingSessionStore.cachePosition(readingViewKey, 0, 0)
            FeedReadingSessionStore.savePosition(readingContext, readingViewKey, 0, 0)
        }
    }
    LaunchedEffect(focusedFeedConcertId, headerAdvertisements.size) {
        if (homeSection == HomeSection.VISUALITZACIO && focusedFeedConcert != null) {
            // v279: ja no existeix l'avís municipal en la cerca; no ocupa cap fila.
            val firstEventIndex =
                (if (headerAdvertisements.isNotEmpty()) 1 else 0) + 2
            feedListState.animateScrollToItem(firstEventIndex.coerceAtLeast(0))
        }
    }

    // v330: estirar des de l'inici del feed força un refresc manual real.
    // Sense roda: una línia fina confirma el gest i no oculta mai les targetes.
    // La sincronització automàtica no fa aparéixer cap indicador en «Per a tu».
    val pullRefreshState = rememberPullToRefreshState()
    val forYouWithoutSpinner = homeSection == HomeSection.VISUALITZACIO &&
        state.feedMode == FeedMode.FOR_YOU
    val manualForYouRefreshing = forYouWithoutSpinner && pendingManualForYouRefresh != null
    // v334: una sincronització automàtica mai dibuixa rodes en Guardats, Entrades ni Comunitat.
    val visualRefreshing = manualForYouRefreshing
    PullToRefreshBox(
        isRefreshing = visualRefreshing,
        state = pullRefreshState,
        indicator = {
            if (forYouWithoutSpinner) {
                // Progrés de la mateixa estirada abans de soltar, no un carregador
                // circular. En actualitzar, es manté una línia discreta animada.
                if (manualForYouRefreshing) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter)
                    )
                } else if (pullRefreshState.distanceFraction > 0f) {
                    LinearProgressIndicator(
                        progress = { pullRefreshState.distanceFraction.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter)
                    )
                }
            } else if (pullRefreshState.distanceFraction > 0f) {
                LinearProgressIndicator(
                    progress = { pullRefreshState.distanceFraction.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter)
                )
            }
        },
        onRefresh = {
            if (homeSection == HomeSection.GUARDATS) {
                // Guardats només sincronitza les llistes. No toca Per a tu.
                state.refreshSavedCollections()
                state.ensureFullAgendaLoaded("saved_refresh")
            } else if (!forYouWithoutSpinner || pendingManualForYouRefresh == null) {
                // Refresc explícit: salta els 30 minuts, actualitza catàleg,
                // batecs i feed; una obertura normal continua usant la caché.
                // No esborrar les targetes visibles mentre arriba la resposta.
                manualRefreshEpoch++
                val token = "$feedSessionEpoch:$manualRefreshEpoch"
                if (forYouWithoutSpinner) pendingManualForYouRefresh = token
                FeedReadingSessionStore.clearSnapshot(readingContext)
                state.refreshForReadingSession(token)
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            // v326: Guardats té un fons blanc uniforme en tota la pantalla,
            // també darrere del llistat i en zones buides.
            .then(if (homeSection == HomeSection.GUARDATS) Modifier.background(Color.White) else Modifier)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 0.17.7: el cercador queda sempre visible. Només es desplaça el feed.
            if (homeSection == HomeSection.VISUALITZACIO) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.background,
                    tonalElevation = 2.dp
                ) {
                    DebouncedFeedSearch(
                        state = state,
                        onSearchApplied = onSearchApplied,
                        onDraftPendingChanged = { searchDraftPending = it },
                        onFilter = onFilter
                    )
                }
                // v284: una cerca escrita és global: no presentar els filtres
                // territorials anteriors com si limitassen els resultats.
                if (state.search.isBlank() && !searchDraftPending && !state.filters.isEmpty()) {
                    ActiveFeedFiltersRow(
                        filters = state.filters,
                        onClear = { state.clearFilters() }
                    )
                }
            }

            LazyColumn(
                state = feedListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 36.dp)
            ) {

        // v289: «Per a tu» s'actualitza silenciosament, sense indicador ni
        // textos de diagnòstic. Conserve únicament l'error útil de connexió.
        if (homeSection == HomeSection.VISUALITZACIO &&
            state.feedMode == FeedMode.FOR_YOU &&
            !state.forYouRecalculationPending &&
            state.forYouRecalculationError != null) {
            item(key = "for_you_refresh_error_v289") {
                Text(
                    text = state.forYouRecalculationError.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        // v249: publicitat global independent de Seguits/Sense filtre/Per a tu/Categoria.
        if (homeSection == HomeSection.VISUALITZACIO && headerAdvertisements.isNotEmpty()) {
            item(key = "header_ads") {
                SponsoredHeaderSlider(
                    state = state,
                    advertisements = headerAdvertisements,
                    onOpenConcert = onOpenAdvertisement
                )
            }
        }


        if (homeSection == HomeSection.ENTRADES && specialAccessConcerts.isNotEmpty()) {
            items(specialAccessConcerts, key = { "special_event_access_${it.id}" }) { specialConcert ->
                SpecialEventAccessCard(
                    state = state,
                    concert = specialConcert,
                    onOpen = { onOpenSpecialEvent(specialConcert) }
                )
            }
        }

        // Batecs no forma part dels filtres del feed: sempre es mostra en Inici,
        // també en Per a tu, Seguits, Sense filtre i Categoria.
        if (homeSection == HomeSection.VISUALITZACIO && state.search.isBlank() && state.shouldDisplayBatecs()) item(key = "batecs_row") {
            StoriesRow(
                state = state,
                groups = batecGroups,
                concerts = state.concerts,
                followedArtists = state.followedArtists,
                hasMore = false,
                loadingMore = false,
                onLoadMore = {},
                returnToStartEpoch = batecsReturnEpoch,
                onOpenStory = { opened ->
                    onOpenStory(opened, batecGroups.flatMap { it.stories })
                }
            )
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = .35f))
        }


        // Guardats té ja el seu propi selector i capçalera al panell intern.
        // No dupliquem el títol «Guardats» ni el comptador a l'inici de la pestanya.
        if (homeSection != HomeSection.GUARDATS) item {
            val sectionLabel = when (homeSection) {
                HomeSection.VISUALITZACIO -> state.feedMode.label
                HomeSection.GUARDATS -> "" // La branca està exclosa del bloc visual.
                HomeSection.ENTRADES -> "Entrades comprades"
                HomeSection.COMUNITAT -> "Comunitat"
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (homeSection == HomeSection.VISUALITZACIO) {
                    var perYouMenuExpanded by remember { mutableStateOf(false) }
                    var categoryMenuExpanded by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box {
                            val perYouSelected = state.feedMode == FeedMode.FOR_YOU || state.feedMode == FeedMode.UNFILTERED
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { perYouMenuExpanded = true; state.refreshFilterPresetsFromServer() },
                                color = if (perYouSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
                            ) {
                                Text(
                                    when {
                                        searchDraftPending -> "Resultats de cerca ▾"
                                        state.feedMode == FeedMode.UNFILTERED -> when {
                                            state.search.isNotBlank() -> "Resultats de cerca ▾"
                                            state.activeFilterPresetName != null -> "${state.activeFilterPresetName} ▾"
                                            state.filters.isEmpty() -> "Sense filtre ▾"
                                            else -> "Filtres actius ▾"
                                        }
                                        else -> "Per a tu ▾"
                                    },
                                    modifier = Modifier.widthIn(max = 125.dp).padding(horizontal = 9.dp, vertical = 7.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 13.sp,
                                    fontWeight = if (perYouSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                            DropdownMenu(
                                expanded = perYouMenuExpanded,
                                onDismissRequest = { perYouMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Per a tu") },
                                    onClick = {
                                        perYouMenuExpanded = false
                                        state.selectFeedMode(FeedMode.FOR_YOU)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sense filtre") },
                                    onClick = {
                                        perYouMenuExpanded = false
                                        state.selectFeedMode(FeedMode.UNFILTERED)
                                    }
                                )
                                if (state.savedFilterSyncError != null) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(state.savedFilterSyncError.orEmpty(),
                                                color = MaterialTheme.colorScheme.error,
                                                fontSize = 12.sp, maxLines = 3)
                                        }, onClick = { state.refreshFilterPresetsFromServer() }
                                    )
                                }
                                state.savedFilterPresets.forEach { preset ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    preset.name,
                                                    modifier = Modifier.weight(1f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    fontSize = 13.sp
                                                )
                                                IconButton(
                                                    onClick = { state.deleteFilterPreset(preset.name) },
                                                    modifier = Modifier.size(38.dp)
                                                ) {
                                                    Icon(Icons.Outlined.Delete,
                                                        contentDescription = "Eliminar ${preset.name}",
                                                        modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        },
                                        onClick = {
                                            perYouMenuExpanded = false
                                            state.applyFilterPreset(preset.name)
                                        }
                                    )
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { state.selectFeedMode(FeedMode.FOLLOWED) },
                            color = if (state.feedMode == FeedMode.FOLLOWED) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
                        ) {
                            Text(
                                "Seguits",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                fontSize = 13.sp,
                                fontWeight = if (state.feedMode == FeedMode.FOLLOWED) FontWeight.Bold else FontWeight.Medium
                            )
                        }

                        Box {
                            val categorySelected = state.feedMode == FeedMode.CATEGORY
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { categoryMenuExpanded = true },
                                color = if (categorySelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
                            ) {
                                Text(
                                    if (categorySelected && state.selectedFeedCategory != null) "${state.selectedFeedCategory!!.label} ▾" else "Categoria ▾",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (categorySelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                            DropdownMenu(
                                expanded = categoryMenuExpanded,
                                onDismissRequest = { categoryMenuExpanded = false }
                            ) {
                                if (categorySelected) {
                                    DropdownMenuItem(
                                        text = { Text("Llevar disciplina · Per a tu") },
                                        onClick = {
                                            categoryMenuExpanded = false
                                            state.selectFeedMode(FeedMode.FOR_YOU)
                                        }
                                    )
                                }
                                CulturalCategory.values().forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category.label) },
                                        onClick = {
                                            categoryMenuExpanded = false
                                            state.selectFeedCategory(category)
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(sectionLabel, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                }
                Text(
                    if (homeSection == HomeSection.COMUNITAT) {
                        val active = state.communityGroups.count { group ->
                            (group.myStatus == "ACCEPTED" || group.myStatus == "CONTACT") &&
                                (group.myStatus == "CONTACT" || group.myRole == "CREATOR" || group.accessMode == com.example.agendavalenciana.model.CommunityAccessMode.APPROVAL || group.myMessageCount > 0)
                        }
                        "$active converses"
                    } else if (homeSection == HomeSection.ENTRADES) {
                        "${state.ticketsForCurrentUser().size} entrades"
                    } else "${if (artistSearchMatch != null) artistResults.size else concerts.size} esdev.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f)
                )
            }
        }

        if (homeSection == HomeSection.VISUALITZACIO &&
            state.followedArtists.isEmpty() &&
            state.recentSearchTerms.isEmpty() &&
            state.search.isBlank() &&
            state.feedMode != FeedMode.FOLLOWED
        ) {
            item(key = "personalization_welcome") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, top = 0.dp, bottom = 8.dp)
                ) {
                    Text(
                        "Benvingut a l’Agenda",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .72f)
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        "Explora l’Agenda i seguix artistes, grups i espais que t’agraden per crear el teu propi feed.",
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .48f)
                    )
                }
            }
        }

        if (firstArtistSuggestion != null) {
            item(key = "artist_search_first") {
                SearchedArtistFollowCard203(state, firstArtistSuggestion) { onOpenArtist(firstArtistSuggestion) }
            }
        }

        // Avisem només amb el resultat complet: el primer preview encara pot ser curt.
        // Llevar els filtres actius restaura «Per a tu».
        if (showLimitedResultsWarning) {
            item(key = "low_results_active_filters") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .44f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .28f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            if (concerts.isEmpty()) "No hi ha esdeveniments amb estos filtres"
                            else "Només ${concerts.size} ${if (concerts.size == 1) "esdeveniment" else "esdeveniments"} amb estos filtres",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Tens filtres aplicats en Opcions de cerca. Poden limitar els resultats.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = onFilter) { Text("Modificar opcions") }
                            TextButton(onClick = { state.clearFilters() }) { Text("Llevar filtres") }
                        }
                    }
                }
            }
        }

        if (homeSection == HomeSection.ENTRADES) {
            item {
                PurchasedTicketsPanel(
                    state = state,
                    focusedConcertId = focusedTicketConcertId,
                    onOpenConcert = onOpenConcert
                )
            }
        }

        if (homeSection == HomeSection.ENTRADES) {
            // Entrades és una pantalla pròpia: QR + espais especials actius.
            // No cau mai al bucle genèric de cards d'esdeveniment del feed.
        } else if (homeSection == HomeSection.GUARDATS) {
            item(key = "saved_collections_overview") {
                SavedCollectionsPanel(state, onOpenConcert, onOpenArtist)
            }
        } else if (homeSection == HomeSection.COMUNITAT) {
            item(key = "event_boards_only_header") {
                Text(
                    "Els teus taulons d'esdeveniments. Per crear-ne un, entra a la Comunitat de l'esdeveniment.",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val activeGroups = state.communityGroups
                .filter { group ->
                    (group.myStatus == "ACCEPTED" || group.myStatus == "CONTACT") &&
                        (group.myStatus == "CONTACT" || group.myRole == "CREATOR" || group.accessMode == com.example.agendavalenciana.model.CommunityAccessMode.APPROVAL || group.myMessageCount > 0)
                }
                .sortedWith(
                    compareByDescending<com.example.agendavalenciana.model.CommunityGroup> {
                        state.messagesForCommunityGroup(it.id).maxOfOrNull { message -> message.id } ?: 0
                    }.thenByDescending { it.id }
                )

            if (activeGroups.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Encara no participes en cap tauló. En trobaràs dins de cada esdeveniment.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(activeGroups, key = { "community_${it.id}" }) { group ->
                    val concert = state.concertById(group.concertId)
                    if (concert != null) {
                        val messages = state.messagesForCommunityGroup(group.id)
                        val lastMessage = messages.lastOrNull()
                        ActiveCommunityConversationCard(
                            concert = concert,
                            group = group,
                            lastMessage = lastMessage,
                            onOpen = { selectedBottomCommunityGroupId = group.id }
                        )
                    } else {
                        Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp)) {
                            CommunityGroupCard(state = state, group = group, onOpen = { selectedBottomCommunityGroupId = group.id })
                        }
                    }
                }
            }
        } else if (concerts.isEmpty()) {
            val defaultHome = homeSection == HomeSection.VISUALITZACIO &&
                state.search.isBlank() &&
                state.filters.isEmpty() &&
                state.feedMode == FeedMode.FOR_YOU
            item {
                if (criteriaPending) {
                    // v284: carregador del cercador deliberadament invisible;
                    // tampoc mostrar «No hi ha resultats» fins a acabar.
                    Spacer(Modifier.height(1.dp))
                } else if (defaultHome && state.agendaSyncError == null &&
                    (state.isAgendaRefreshing || state.forYouRecalculationPending || !state.isFeedReadyForReading)) {
                    // Sense roda, overlay, ni targetes inventades: indicació estàtica
                    // que deixa cercador, pestanyes i altres accions operatius.
                    Text(
                        "Preparant les activitats segons els teus gustos. Pots continuar navegant.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp)
                    )
                } else if ((state.search.isNotBlank() || !state.filters.isEmpty()) &&
                    !state.isGlobalSearchCatalogLoaded && state.agendaSyncError != null) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No s'ha pogut carregar l'agenda completa. Revisa la connexió i torna a actualitzar.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (defaultHome) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No hi ha esdeveniments que coincidisquen amb els teus seguiments i preferències. Afig-ne o tria «Sense filtre» per vore tota l'agenda.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    EmptyState()
                }
            }
        } else {
            // Feed compacte: es mantenen ocults els controls de Llista/Mapa,
            // però els esdeveniments continuen mostrant-se amb la targeta compacta original.
            itemsIndexed(
                pagedArtistResults,
                key = { _, result -> "list_${result.key}" },
                contentType = { _, result -> if (result.artist == null) "agenda_card" else "festival_match_card" }
            ) { index, result ->
                val concert = result.concert
                Column {
                    if (homeSection == HomeSection.VISUALITZACIO && state.feedMode == FeedMode.FOR_YOU && (index == 9 || index == 39)) {
                        FollowSuggestionFeedRow(
                            state = state,
                            slot = if (index == 9) 0 else 1,
                            onOpenArtist = onOpenArtist
                        )
                    }
                    if (homeSection == HomeSection.VISUALITZACIO && GlobalAdPlacementPolicy.premiumAt(index, premiumAdvertisements.isNotEmpty(), normalAdvertisements.isNotEmpty())) {
                        SponsoredSlider(
                            state = state,
                            advertisements = premiumAdvertisements,
                            onOpenConcert = onOpenAdvertisement
                        )
                    }
                    if (homeSection == HomeSection.VISUALITZACIO && GlobalAdPlacementPolicy.normalAt(index, premiumAdvertisements.isNotEmpty(), normalAdvertisements.isNotEmpty())) {
                        SponsoredSlider(
                            state = state,
                            advertisements = normalAdvertisements,
                            onOpenConcert = onOpenAdvertisement
                        )
                    }
                    val matchedArtist = result.artist
                    if (matchedArtist != null) {
                        // v331: el festival conserva EXACTAMENT la targeta habitual;
                        // només l'acció d'obertura enfoca el grup consultat en Horaris.
                        CompactConcertItem(
                            state = state,
                            concert = concert,
                            onOpenArtist = { artist -> onOpenArtist(artist) },
                            onOpenConcert = {
                                val selectedDay = result.slot?.date?.take(10)?.takeIf(String::isNotBlank)
                                val occurrence = if (selectedDay == null) concert else concert.copy(
                                    selectedDate = selectedDay, feedOccurrenceDate = selectedDay
                                )
                                onOpenFestivalArtist(occurrence, matchedArtist, result.slot?.time)
                            },
                            forceExpanded = focusedFeedConcertId == concert.id
                        )
                    } else {
                        CompactConcertItem(
                            state = state,
                            concert = concert,
                            onOpenArtist = { artist -> onOpenArtist(artist) },
                            onOpenConcert = { onOpenConcert(concert) },
                            forceExpanded = focusedFeedConcertId == concert.id
                        )
                    }
                    if (homeSection == HomeSection.GUARDATS && concert.isFestivalExperience) {
                        FestivalSavedArtists203(state, concert) { artist -> onOpenFestivalArtist(concert, artist, null) }
                    }
                }
            }


            // v255: els espais de blocs curts van DESPRÉS de l'últim esdeveniment
            // real, mai al principi del feed. No es calculen segons el primer lot
            // de 10, perquè la paginació desplaçava els anuncis actius.
            if (homeSection == HomeSection.VISUALITZACIO && visibleConcertLimit >= totalArtistResultsCount &&
                (!fastUnfilteredRows || state.isGlobalSearchCatalogLoaded)) {
                if (GlobalAdPlacementPolicy.premiumAfterLast(totalArtistResultsCount, premiumAdvertisements.isNotEmpty(), normalAdvertisements.isNotEmpty())) {
                    item(key = "premium_ads_tail") {
                        SponsoredSlider(state, premiumAdvertisements, onOpenAdvertisement)
                    }
                }
                if (GlobalAdPlacementPolicy.normalAfterLast(totalArtistResultsCount, premiumAdvertisements.isNotEmpty(), normalAdvertisements.isNotEmpty())) {
                    item(key = "normal_ads_tail") {
                        SponsoredSlider(state, normalAdvertisements, onOpenAdvertisement)
                    }
                }
            }

            // Este item només entra en composició en arribar al final del lot visible.
            // En eixe moment ampliem el feed automàticament en blocs de 10.
            if (visibleConcertLimit < totalArtistResultsCount) {
                item(key = "load_more_${visibleConcertLimit}") {
                    LaunchedEffect(visibleConcertLimit, totalArtistResultsCount) {
                        visibleConcertLimit = (visibleConcertLimit + FEED_SCROLL_BATCH).coerceAtMost(totalArtistResultsCount)
                    }
                    if (homeSection == HomeSection.VISUALITZACIO &&
                        state.feedMode == FeedMode.FOR_YOU) {
                        // v289: carregar la pàgina següent sense cartell ni indicador.
                        Spacer(Modifier.height(1.dp))
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Carregant 10 més…",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        }
    }
    }

    selectedBottomCommunityGroupId?.let { groupId ->
        CommunityChatDialog(
            state = state,
            groupId = groupId,
            onDismiss = { selectedBottomCommunityGroupId = null }
        )
    }

    if (state.search.isBlank()) popupAdvertisement?.let { ad ->
        state.concertForAdvertisement(ad)?.let { concert ->
            SponsoredPopupDialog(
                state = state,
                advertisement = ad,
                concert = concert,
                onDismiss = { popupAdvertisement = null },
                onOpenConcert = {
                    popupAdvertisement = null
                    onOpenAdvertisement(concert)
                }
            )
        }
    }
}

@Composable
private fun EventDateSaveControl(
    state: AgendaState,
    concert: Concert,
    showDiscipline: Boolean = false,
    savedListBadge: Pair<String, Color>? = null,
    onReminderClick: (() -> Unit)? = null
) {
    val saved = if (onReminderClick == null) state.isSavedAnywhere(concert.id) else false
    val disciplineAccent = eventAccentColor(concert)
    Column(
        // V253: dret més estret: l’espai del cor es dona a la informació de l’esquerra.
        modifier = Modifier.width(72.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showDiscipline) {
            Text(
                savedListBadge?.first ?: concert.category.label,
                fontSize = 9.sp,
                lineHeight = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = savedListBadge?.second ?: disciplineAccent.copy(alpha = .78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(1.dp))
        }
        // La data de la targeta del feed és SEMPRE «DD mes» (p. ex. «22 oct»).
        Text(
            if (FestivalFeedNextDay.isOngoingWithoutSchedule(concert, state.agendaToday()))
                "En curs"
            else AgendaDateFormat.cardDate(concert.feedDate),
            fontSize = 11.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = {
                        if (onReminderClick != null) onReminderClick()
                        else state.openSavePicker(concert.id)
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        if (onReminderClick != null) Icons.Filled.Notifications
                        else if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (onReminderClick != null) "Desactivar recordatori"
                            else if (saved) "Guardat" else "Guardar",
                        modifier = Modifier.size(20.dp),
                        tint = if (onReminderClick != null) Color(0xFFE9A622)
                            else if (saved) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .62f)
                    )
                }
                if (onReminderClick == null) {
                    Text(state.savedCountForConcert(concert.id).toString(), fontSize = 10.sp, lineHeight = 10.sp)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .72f)
                ) {
                    EventReportMenuButton(
                        state = state,
                        concert = concert,
                        compact = true,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun ActiveCommunityConversationCard(
    concert: Concert,
    group: com.example.agendavalenciana.model.CommunityGroup,
    lastMessage: com.example.agendavalenciana.model.CommunityChatMessage?,
    onOpen: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onOpen),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .14f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(group.type.emoji, fontSize = 18.sp)
            Spacer(Modifier.width(7.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${group.title} · ${concert.artist}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    lastMessage?.let { "${it.author}: ${it.text}" }
                        ?: "${concert.town} · ${AgendaDateFormat.date(concert.feedDate)}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(6.dp))
            Text("Obrir", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun TimeTicketLine(time: String, ticketText: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "🕒",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .76f)
        )
        Spacer(Modifier.width(4.dp))
        Box(modifier = Modifier.width(48.dp)) {
            if (time.isNotBlank()) {
                Text(
                    time,
                    maxLines = 1,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .76f)
                )
            }
        }
        Text(
            "·   🎟 $ticketText",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .76f)
        )
    }
}

@Composable
private fun ForYouChronologicalTailNotice() {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text("Mostrar-ho tot", fontWeight = FontWeight.Black)
            Text(
                "A partir d'ací, l'agenda continua sense personalització especial: data i hora.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CompactConcertItem(
    state: AgendaState,
    concert: Concert,
    onOpenArtist: (String) -> Unit,
    onOpenConcert: () -> Unit,
    forceExpanded: Boolean = false,
    savedListBadge: Pair<String, Color>? = null,
    onReminderClick: (() -> Unit)? = null
) {
    // La targeta no s'expandeix al feed: qualsevol esdeveniment obri popup.
    // El focus heretat de notificacions també es transforma en popup.
    var expanded by remember(concert.id, concert.feedDate) { mutableStateOf(false) }
    LaunchedEffect(forceExpanded, concert.id) {
        if (forceExpanded) onOpenConcert()
    }
    var selectedSection by remember(concert.id) { mutableStateOf<String?>(null) }
    var showTicketDialog by remember(concert.id) { mutableStateOf(false) }
    var showPosterDialog by remember(concert.id) { mutableStateOf(false) }
    var showCreateCommunityChat by remember(concert.id) { mutableStateOf(false) }
    var createCommunityType by remember(concert.id) { mutableStateOf(com.example.agendavalenciana.model.CommunityGroupType.CAR) }
    var selectedCommunityGroup by remember(concert.id) { mutableStateOf<com.example.agendavalenciana.model.CommunityGroup?>(null) }
    val artistPhotoUri = state.publicPhotoForArtist(concert.artist)
    val largePhoto = rememberImageBitmapFromUris(state.cardImageCandidates(concert), maxDimension = if (expanded) 900 else 240, eventId = concert.id, knownResolvedUri = concert.resolvedCardImageUri)
    val eventPosterUri = concert.posterUri?.trim().orEmpty()
    val context = LocalContext.current
    val ticketText = when {
        concert.isFree -> "Gratuït"
        concert.ticketPrice > 0 -> "${concert.ticketPrice} €"
        !concert.ticketAvailable -> "Sense venda"
        else -> "Entrades"
    }
    val cardArtists = concert.lineup.map(String::trim).filter(String::isNotBlank).distinctBy { it.lowercase() }
    val artistSectionLabel = if (concert.category == CulturalCategory.MUSIC) "Grups" else "Artista/es"
    val isFestival = concert.isFestivalExperience
    val ongoingWithoutDay = FestivalFeedNextDay.isOngoingWithoutSchedule(concert, state.agendaToday())
    val cardTime = if (ongoingWithoutDay) "" else concert.time
    val festivalLine = if (isFestival && !ongoingWithoutDay) {
        if (FestivalFeedNextDay.isConsecutiveFestival(concert)) {
            FestivalFeedNextDay.festivalPreview(concert, state.agendaToday())
        } else {
            concert.scheduleDaySummaries[concert.feedDate]
                ?.trim()
                ?.takeIf(String::isNotBlank)
                ?: cardArtists.joinToString(" · ")
        }
    } else ""
    val secondaryLine = if (isFestival) festivalLine else concert.subtitle.trim()
    val organizerName = if (isFestival) {
        concert.organizers.firstOrNull { it.contains("festival", ignoreCase = true) }?.trim()
            ?: concert.organizers.firstOrNull()?.trim().orEmpty()
    } else {
        concert.organizers.firstOrNull()?.trim().orEmpty()
    }
    val hasExtraFoldedLine = secondaryLine.isNotBlank()
    val attendance = state.attendanceFor(concert.id)
    val disciplineAccent = eventAccentColor(concert)

    LaunchedEffect(expanded, selectedSection, attendance, concert.id) {
        if (expanded && selectedSection == "community" && attendance == AttendanceStatus.GOING) {
            state.refreshCommunityGroups(concert.id)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            shape = RoundedCornerShape(if (expanded) 18.dp else 0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = if (expanded) BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = .12f)
            ) else null
        ) {
            if (!expanded) {
                // Vista minimitzada: blanca, compacta i sense blocs grisos.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable {
                            onOpenConcert()
                        }
                        .padding(start = 15.dp, end = 5.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(76.dp)
                            .height(96.dp)
                            .clickable { onOpenConcert() }
                            .border(1.5.dp, disciplineAccent.copy(alpha = .88f), RoundedCornerShape(9.dp))
                            .padding(2.dp)
                            .clip(RoundedCornerShape(7.dp))
                    ) {
                        if (largePhoto != null) {
                            Image(
                                bitmap = largePhoto,
                                contentDescription = concert.title,
                                modifier = Modifier.matchParentSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = .14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    concert.title.take(2).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(11.dp))

                    // v326: la línia de subtítol/avanç de festival afegix altura.
                    // La columna ha de poder créixer per a no retallar l'hora/entrades
                    // (també amb la mida de lletra ampliada per accessibilitat).
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = if (hasExtraFoldedLine) 112.dp else 96.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            concert.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        if (secondaryLine.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                secondaryLine,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 12.sp,
                                fontWeight = if (isFestival) FontWeight.SemiBold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .70f)
                            )
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "📍 ${concert.town} · ${concert.venue}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .80f)
                        )
                        Spacer(Modifier.height(3.dp))
                        TimeTicketLine(time = cardTime, ticketText = ticketText)
                    }

                    Spacer(Modifier.width(5.dp))
                    EventDateSaveControl(state = state, concert = concert, showDiscipline = true,
                        savedListBadge = savedListBadge, onReminderClick = onReminderClick)
                }
            } else {
                // Vista ampliada: la fotografia ocupa tota la targeta.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 360.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.5.dp, disciplineAccent.copy(alpha = .88f), RoundedCornerShape(18.dp))
                ) {
                    if (largePhoto != null) {
                        Image(
                            bitmap = largePhoto,
                            contentDescription = concert.title,
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = .30f),
                                            MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    )
                                )
                        )
                    }

                    // Capçalera al mateix lloc, sobre una franja suau semitransparent.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = .84f))
                            .clickable {
                                expanded = false
                                selectedSection = null
                            }
                            .padding(start = 15.dp, end = 5.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // En la vista desplegada no mostrem cap avatar/cercle: la identitat
                        // rellevant és l'esdeveniment, no un perfil concret.
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                concert.title,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            if (isFestival && organizerName.isNotBlank()) {
                                Text(
                                    "Organitza · $organizerName",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .66f)
                                )
                            } else if (concert.subtitle.isNotBlank()) {
                                Text(
                                    concert.subtitle,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .70f)
                                )
                            }
                            Spacer(Modifier.height(3.dp))
                            Text(
                                "📍 ${concert.town} · ${concert.venue}",
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .80f)
                            )
                            Spacer(Modifier.height(3.dp))
                            TimeTicketLine(time = cardTime, ticketText = ticketText)
                        }

                        Spacer(Modifier.width(5.dp))
                        EventDateSaveControl(state = state, concert = concert,
                            onReminderClick = onReminderClick)
                    }

                    if (eventPosterUri.isNotBlank()) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(start = 10.dp, top = if (isFestival || concert.subtitle.isNotBlank()) 132.dp else 112.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { showPosterDialog = true },
                            color = Color.Black.copy(alpha = .38f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = .30f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Outlined.PhotoCamera, contentDescription = null,
                                    modifier = Modifier.size(15.dp), tint = Color.White)
                                Text(
                                    "Cartell",
                                    color = Color.White.copy(alpha = .96f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Accions ràpides fixes sobre la imatge, just baix de la capçalera.
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = if (isFestival || concert.subtitle.isNotBlank()) 132.dp else 112.dp, end = 9.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .clickable {
                                    val activating = concert.id !in state.alertConcerts
                                    state.toggleConcertAlert(concert.id)
                                    Toast.makeText(context,
                                        if (activating) "Avís de l’esdeveniment guardat." else "Avís desactivat.",
                                        Toast.LENGTH_SHORT).show()
                                },
                            color = Color.Black.copy(alpha = .46f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = .22f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    if (concert.id in state.alertConcerts) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                                    contentDescription = if (concert.id in state.alertConcerts) "Desactivar avisos" else "Activar avisos",
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.White
                                )
                            }
                        }
                        Surface(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .clickable {
                                    state.recordShare(concert.id)
                                    EventShareCard.share(context, concert, largePhoto?.asAndroidBitmap())
                                },
                            color = Color.Black.copy(alpha = .46f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = .22f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.Share,
                                    contentDescription = "Compartir",
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    // Navegació de contingut integrada en la part inferior de la card.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = .86f))
                            .padding(horizontal = 5.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(Modifier.weight(1f)) {
                            InlineConcertSectionButton(
                                label = artistSectionLabel,
                                icon = if (concert.category == CulturalCategory.MUSIC) "♫" else "👤",
                                selected = selectedSection == "groups",
                                modifier = Modifier.fillMaxWidth()
                            ) { selectedSection = "groups" }
                        }
                        Box(Modifier.weight(1f)) {
                            InlineConcertSectionButton(
                                label = "Entrades",
                                icon = "🎟",
                                selected = selectedSection == "tickets",
                                modifier = Modifier.fillMaxWidth()
                            ) { selectedSection = "tickets" }
                        }
                        Box(Modifier.weight(1f)) {
                            InlineConcertSectionButton(
                                label = "Info",
                                icon = "ⓘ",
                                selected = selectedSection == "info",
                                modifier = Modifier.fillMaxWidth()
                            ) { selectedSection = "info" }
                        }
                        Box(Modifier.weight(1f)) {
                            InlineConcertSectionButton(
                                label = "Comunitat",
                                icon = "👥",
                                selected = selectedSection == "community",
                                modifier = Modifier.fillMaxWidth()
                            ) { selectedSection = "community" }
                        }
                    }
                }
            }
        }

        // Separador discret entre esdeveniments en la llista.
        if (!expanded) {
            Divider(
                modifier = Modifier.padding(horizontal = 8.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = .18f),
                thickness = 1.dp
            )
        }

        // Només el contingut de cada botó queda fora de la card, sobre fons blanc/surface.
        if (expanded && selectedSection != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp, bottom = 7.dp),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(0.dp, 0.dp, 16.dp, 16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .10f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    when (selectedSection) {
                        "info" -> {
                            Text("Informació", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                "${if (isFestival) concert.title else concert.artist} · ${concert.venue}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                "${concert.town} · ${concert.comarca}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .68f)
                            )
                            Text(
                                if (concert.endDate.isNotBlank() && concert.endDate != concert.date) {
                                    "Del ${formatDate(concert.date)} al ${formatDate(concert.endDate)}" +
                                        (if (concert.time.isNotBlank()) " · ${concert.time}" else "") +
                                        (if (concert.endTime.isNotBlank()) " → ${concert.endTime}" else "")
                                } else {
                                    formatDate(concert.date) + if (concert.time.isNotBlank()) " · ${concert.time}" else ""
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .76f)
                            )
                            if (concert.description.isNotBlank()) {
                                Text(
                                    concert.description,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .82f)
                                )
                            }
                            if (!isFestival) {
                                TextButton(onClick = { onOpenArtist(concert.artist) }, contentPadding = PaddingValues(0.dp)) {
                                    Text(if (concert.category == CulturalCategory.MUSIC) "Veure informació del grup" else "Veure informació de l'artista")
                                }
                            } else if (organizerName.isNotBlank()) {
                                Text(
                                    "Organitza · $organizerName",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .66f)
                                )
                            }
                        }
                        "tickets" -> {
                            Text("Entrades", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                when {
                                    concert.isFree -> "Gratuït"
                                    concert.ticketPrice > 0 -> "Preu: ${concert.ticketPrice} €."
                                    !concert.ticketAvailable -> "Encara no hi ha venda d’entrades disponible."
                                    else -> "Entrades disponibles."
                                },
                                fontSize = 14.sp
                            )
                            if (concert.ticketAvailable) {
                                Text(
                                    if (concert.ticketUrl.isNotBlank()) {
                                        "La venda es completarà en la plataforma externa configurada per l’organitzador."
                                    } else {
                                        "Venda interna d’Agenda: cada entrada genera el seu QR propi."
                                    },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .62f)
                                )
                                Button(
                                    onClick = {
                                        if (concert.ticketUrl.isNotBlank()) {
                                            state.recordTicketClick(concert.id)

                                            openTicketUrl(context, concert.ticketUrl)
                                        } else {
                                            showTicketDialog = true
                                        }
                                    },
                                    enabled = concert.ticketAvailable,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Outlined.ConfirmationNumber, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        when {
                                            concert.ticketUrl.isNotBlank() && concert.isFree -> "Reservar entrades"
                                            concert.ticketUrl.isNotBlank() && concert.ticketPrice > 0 -> "Comprar · ${concert.ticketPrice} €"
                                            concert.ticketUrl.isNotBlank() -> "Comprar entrades"
                                            concert.isFree -> "Reservar gratis"
                                            concert.ticketPrice > 0 -> "Comprar · ${concert.ticketPrice} €"
                                            else -> "Entrades"
                                        }
                                    )
                                }
                            }
                        }
                        "groups" -> {
                            Text(artistSectionLabel, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            cardArtists.forEach { artistName ->
                                EventArtistFollowRow(
                                    state = state,
                                    artistName = artistName,
                                    onOpenArtist = onOpenArtist
                                )
                            }
                            if (!isFestival && cardArtists.size == 1) {
                                state.profileForArtist(cardArtists.first())?.let { profile ->
                                    if (profile.bio.isNotBlank()) {
                                        Text(
                                            profile.bio,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .76f)
                                        )
                                    }
                                }
                            }
                        }
                        "community" -> {
                            CommunityBoardSections(
                                state = state,
                                concertId = concert.id,
                                groups = state.groupsForConcert(concert.id),
                                onCreate = { selected ->
                                    createCommunityType = selected
                                    showCreateCommunityChat = true
                                },
                                onOpen = { selectedCommunityGroup = it }
                            )
                        }


                    }
                }
            }
        }
    }

    if (showCreateCommunityChat) {
        CreateCommunityGroupDialog(
            state = state,
            concert = concert,
            onDismiss = { showCreateCommunityChat = false },
            initialType = createCommunityType,
            onCreated = {
                showCreateCommunityChat = false
                state.refreshCommunityGroups(concert.id)
            }
        )
    }

    selectedCommunityGroup?.let { group ->
        CommunityChatDialog(
            state = state,
            groupId = group.id,
            onDismiss = { selectedCommunityGroup = null }
        )
    }

    if (showPosterDialog && eventPosterUri.isNotBlank()) {
        EventPosterViewerDialog(
            posterUri = eventPosterUri,
            title = concert.title,
            onDismiss = { showPosterDialog = false }
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
}

@Composable
internal fun EventPosterViewerDialog(
    posterUri: String,
    title: String,
    onDismiss: () -> Unit
) {
    val poster = rememberImageBitmapFromUri(posterUri, maxDimension = 2400)
    var scale by remember(posterUri) { mutableStateOf(1f) }
    var offsetX by remember(posterUri) { mutableStateOf(0f) }
    var offsetY by remember(posterUri) { mutableStateOf(0f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (poster != null) {
                    Image(
                        bitmap = poster,
                        contentDescription = "Cartell de $title",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offsetX
                                translationY = offsetY
                            }
                            .pointerInput(posterUri) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                                    scale = newScale
                                    if (newScale <= 1.01f) {
                                        offsetX = 0f
                                        offsetY = 0f
                                    } else {
                                        val maxX = (size.width * (newScale - 1f)) / 2f
                                        val maxY = (size.height * (newScale - 1f)) / 2f
                                        offsetX = (offsetX + pan.x).coerceIn(-maxX, maxX)
                                        offsetY = (offsetY + pan.y).coerceIn(-maxY, maxY)
                                    }
                                }
                            },
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Carregant cartell…", color = Color.White)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(Color.Black.copy(alpha = .62f))
                        .statusBarsPadding()
                        .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        title,
                        modifier = Modifier.weight(1f),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Tancar cartell",
                            tint = Color.White
                        )
                    }
                }

                if (scale > 1.01f) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(16.dp),
                        color = Color.Black.copy(alpha = .60f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            "Zoom ${String.format("%.1f", scale)}×",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineConcertSectionButton(
    label: String,
    icon: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = if (selected)
            MaterialTheme.colorScheme.primary.copy(alpha = .16f)
        else MaterialTheme.colorScheme.surface.copy(alpha = .78f),
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .28f)
            else MaterialTheme.colorScheme.outline.copy(alpha = .12f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(icon, fontSize = 12.sp)
            Spacer(Modifier.width(3.dp))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ExpandedConcertAction(
    label: String,
    icon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(13.dp))
            .clickable(onClick = onClick),
        color = Color.Black.copy(alpha = .44f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .18f))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(icon, fontSize = 16.sp)
            Text(
                label,
                color = Color.White,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ConcertMapView(
    concerts: List<Concert>,
    onOpenConcert: (Concert) -> Unit
) {
    val minLat = 37.75
    val maxLat = 40.85
    val minLon = -1.55
    val maxLon = 0.55

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .4f))
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(560.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .72f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            Text(
                "Castelló",
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .24f),
                fontWeight = FontWeight.Black,
                fontSize = 28.sp
            )
            Text(
                "València",
                modifier = Modifier.align(Alignment.CenterEnd).padding(16.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .20f),
                fontWeight = FontWeight.Black,
                fontSize = 30.sp
            )
            Text(
                "Alacant",
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .20f),
                fontWeight = FontWeight.Black,
                fontSize = 28.sp
            )

            concerts.forEachIndexed { index, concert ->
                val coord = townCoordinate(concert.town)
                val lat = coord?.first ?: (38.3 + (index % 8) * 0.25)
                val lon = coord?.second ?: (-0.8 + (index % 5) * 0.18)
                val xFraction = ((lon - minLon) / (maxLon - minLon)).coerceIn(0.02, 0.93).toFloat()
                val yFraction = (1.0 - ((lat - minLat) / (maxLat - minLat))).coerceIn(0.03, 0.92).toFloat()
                val x = (maxWidth - 56.dp) * xFraction
                val y = (maxHeight - 56.dp) * yFraction

                Surface(
                    modifier = Modifier
                        .offset(x = x, y = y)
                        .size(48.dp)
                        .clickable { onOpenConcert(concert) },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 5.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            concert.artist.take(2).uppercase(Locale.getDefault()),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        Text(
            "Toca un punt per obrir el concert · ${concerts.size} resultats",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f)
        )
    }
}

private fun townCoordinate(town: String): Pair<Double, Double>? = when (town) {
    "Alzira" -> 39.1510 to -0.4350
    "Pego" -> 38.8430 to -0.1170
    "Ontinyent" -> 38.8210 to -0.6090
    "Xàtiva" -> 38.9890 to -0.5180
    "Burjassot" -> 39.5100 to -0.4140
    "Castelló de la Plana", "Castelló" -> 39.9860 to -0.0377
    "Gandia" -> 38.9680 to -0.1810
    "Vila-real" -> 39.9380 to -0.1010
    "Sagunt" -> 39.6790 to -0.2780
    "València" -> 39.4700 to -0.3760
    "Almassora" -> 39.9430 to -0.0630
    else -> null
}

private fun formatDateCompact(date: String): String = AgendaDateFormat.cardDate(date)

@Composable
private fun ActiveFeedFiltersRow(filters: Filters, onClear: () -> Unit) {
    val labels = buildList {
        filters.categories.forEach { add(it.label) }
        if (filters.dateFrom != null || filters.dateTo != null) {
            add("Dates: ${filters.dateFrom?.let(AgendaDateFormat::date) ?: "…"} → ${filters.dateTo?.let(AgendaDateFormat::date) ?: "…"}")
        }
        filters.month?.let { add("Mes: $it") }
        filters.comarca?.let { add(it) }
        filters.town?.let { add(it) }
        filters.maxDistanceKm?.let { add("≤ $it km") }
        filters.maxPrice?.let { add("≤ $it €") }
        if (filters.freeOnly) add("Gratuïts")
    }
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Text("Filtres actius", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, end = 2.dp))
            }
            items(labels) { label ->
                AssistChip(onClick = {}, label = { Text(label, fontSize = 11.sp) })
            }
            item {
                TextButton(onClick = onClear) { Text("Netejar", fontSize = 11.sp) }
            }
        }
    }
}

private data class BatecArtistGroup(
    val key: String,
    val artist: String,
    val stories: List<Story>
) {
    val allSeen: Boolean get() = stories.isNotEmpty() && stories.all { it.seen }
    val openStory: Story get() = stories.firstOrNull { !it.seen } ?: stories.first()
}

private fun batecArtistGroupKey(story: Story): String =
    story.artist.trim().lowercase().ifBlank {
        story.storyKey.ifBlank { "story:${story.id}" }
    }

private fun groupBatecsByArtist(stories: List<Story>): List<BatecArtistGroup> {
    // Un únic cercle per artista/perfil. Si un artista té diversos Batecs,
    // tots queden dins del mateix cercle i es recorren en obrir-lo.
    val grouped = linkedMapOf<String, MutableList<Story>>()
    stories.forEach { story ->
        grouped.getOrPut(batecArtistGroupKey(story)) { mutableListOf() }.add(story)
    }
    return grouped.map { (key, items) ->
        BatecArtistGroup(
            key = key,
            artist = items.firstOrNull()?.artist.orEmpty(),
            stories = items
        )
    }
}

private const val MIN_BATEC_CAROUSEL_ITEMS = 6

@Composable
private fun StoriesRow(
    state: AgendaState,
    groups: List<BatecArtistGroup>,
    concerts: List<Concert>,
    followedArtists: Set<String>,
    hasMore: Boolean,
    loadingMore: Boolean,
    onLoadMore: () -> Unit,
    returnToStartEpoch: Int,
    onOpenStory: (Story) -> Unit
) {
    val eventById = remember(concerts) { concerts.associateBy { it.id } }
    val batecsRowState = rememberLazyListState()
    // Quan un Batec es marca com vist, activeStories() pot moure'l al final
    // després d'un recàlcul diferit. Mantindre el primer lloc també en eixe
    // recàlcul, fins que la persona desplace voluntàriament el carrusel.
    val groupOrder = remember(groups) { groups.map { it.key } }
    var keepRowAtStart by remember { mutableStateOf(false) }
    LaunchedEffect(returnToStartEpoch) {
        if (returnToStartEpoch > 0) {
            keepRowAtStart = true
            batecsRowState.scrollToItem(0)
        }
    }
    LaunchedEffect(groupOrder) {
        if (keepRowAtStart) batecsRowState.scrollToItem(0)
    }
    LazyRow(
        state = batecsRowState,
        // Només el gest HUMÀ allibera el retorn al començament. El salt de
        // scroll programàtic no pot desactivar esta protecció.
        modifier = Modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    keepRowAtStart = false
                }
            }
            .padding(top = 2.dp, bottom = 5.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(groups, key = { it.key }) { group ->
            val representative = group.openStory.copy(seen = group.allSeen)
            state.batecPhotoCacheRevision
            val artistPhoto = state.cachedBatecPhotoForArtist(group.artist)
                ?: state.publicPhotoForArtist(group.artist)
            val photoCandidates = remember(group.key, group.stories, artistPhoto, eventById) {
                buildList<String?> {
                    // 10cn: un Batec no prova una cascada llarga de URLs. Foto d'artista
                    // i, com a màxim, tres candidats de l'esdeveniment principal.
                    add(artistPhoto)
                    group.stories.asSequence()
                        .mapNotNull { it.eventId }
                        .distinct()
                        .mapNotNull(eventById::get)
                        .firstOrNull()
                        ?.let { concert ->
                            add(concert.artistImageUri)
                            add(concert.posterUri)
                            add(concert.eventCardImageUri)
                        }
                }.filterNotNull().map(String::trim).filter(String::isNotBlank).distinct()
            }
            StoryBubble(
                story = representative,
                storyCount = group.stories.size,
                photoUris = photoCandidates,
                followed = group.artist in followedArtists,
                onClick = { onOpenStory(group.openStory) }
            )
        }
        if (hasMore) {
            item(key = "batecs_load_more_${groups.size}") {
                LaunchedEffect(groups.size, loadingMore) {
                    if (!loadingMore) onLoadMore()
                }
                Box(Modifier.width(42.dp).height(54.dp), contentAlignment = Alignment.Center) {
                    Text("…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StoryBubble(story: Story, storyCount: Int, photoUris: List<String>, followed: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(64.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .border(
                    width = 2.dp,
                    brush = if (story.seen) Brush.linearGradient(listOf(Color(0xFF9E9E9E), Color(0xFFBDBDBD))) else storyRingBrush(story.artist),
                    shape = CircleShape
                )
                .padding(3.dp)
                .clip(CircleShape)
                .background(if (story.seen) Brush.linearGradient(listOf(Color(0xFF757575), Color(0xFFB0B0B0))) else artistBrush(story.artist)),
            contentAlignment = Alignment.Center
        ) {
            val thumbnail = rememberImageBitmapFromUris(photoUris, maxDimension = 112)
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail,
                    contentDescription = story.artist,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    colorFilter = if (story.seen) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null
                )
            } else {
                val initials = story.artist
                    .split(" ")
                    .filter(String::isNotBlank)
                    .take(2)
                    .joinToString("") { it.first().uppercaseChar().toString() }
                Text(initials, color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
            }

            if (storyCount > 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (storyCount > 99) "99+" else storyCount.toString(),
                        fontSize = if (storyCount > 99) 7.sp else 9.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (followed) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(13.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1D9BF0))
                        .border(2.dp, Color.White, CircleShape)
                )
            }
        }

        Spacer(Modifier.height(3.dp))
        Text(
            text = story.artist,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 10.sp,
            color = if (story.seen) MaterialTheme.colorScheme.onSurface.copy(alpha = .48f) else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DebouncedFeedSearch(
    state: AgendaState,
    onSearchApplied: () -> Unit,
    onDraftPendingChanged: (Boolean) -> Unit,
    onFilter: () -> Unit
) {
    var searchDraft by remember { mutableStateOf(state.search) }
    var suggestionQuery by remember { mutableStateOf(state.search) }
    // Diferencia una actualització nostra (debounce) de canvis externs de mode/categoria.
    var lastDraftApplied by remember { mutableStateOf(state.search) }

    LaunchedEffect(state.search) {
        if (state.search != lastDraftApplied && state.search != searchDraft) {
            searchDraft = state.search
            suggestionQuery = state.search
            lastDraftApplied = state.search
            onDraftPendingChanged(false)
        }
    }
    // Si es tria una categoria/mode mentre hi ha tecles pendents del debounce,
    // la cerca antiga NO pot reactivar-se després i anul·lar la nova selecció.
    LaunchedEffect(state.feedMode, state.selectedFeedCategory, state.filters) {
        if (state.search.isBlank() && searchDraft.isNotBlank()) {
            searchDraft = ""
            suggestionQuery = ""
            lastDraftApplied = ""
            onDraftPendingChanged(false)
        }
    }

    // v331: la consulta principal va separada dels suggeriments. La cerca no
    // comença mentre Android encara rep pulsacions, ni reconstruïx el feed per
    // cada lletra. Els suggeriments apareixen asíncronament abans del resultat.
    LaunchedEffect(searchDraft) {
        suggestionQuery = ""
        if (searchDraft.isNotBlank()) delay(230)
        suggestionQuery = searchDraft
    }
    LaunchedEffect(searchDraft) {
        if (searchDraft.isNotBlank()) delay(650)
        if (state.search != searchDraft) {
            lastDraftApplied = searchDraft
            state.updateSearch(searchDraft)
            onSearchApplied()
        }
        onDraftPendingChanged(false)
    }

    // v285: la cerca explícita reconcilia la cache amb el servidor, una sola vegada
    // per finestra de 90 s. Les pulsacions i els suggeriments no disparen cada una
    // peticions de xarxa.
    LaunchedEffect(state.search) {
        if (state.search.isNotBlank()) {
            delay(450L)
            state.reconcileCatalogForLookup()
        }
    }

    // 10bp: construir l'índex/rànquing de suggeriments pot recórrer artistes,
    // municipis, sales i organitzadors. Mai ho fem al fil principal de Compose.
    val suggestions by produceState<List<FeedSearchSuggestion>>(
        initialValue = emptyList(),
        key1 = suggestionQuery,
        key2 = state.searchSuggestionRevision
    ) {
        val querySnapshot = suggestionQuery
        value = if (querySnapshot.isBlank()) {
            emptyList()
        } else {
            withContext(Dispatchers.Default) { state.feedSearchSuggestions(querySnapshot) }
        }
    }

    SearchAndFilter(
        search = searchDraft,
        suggestions = suggestions,
        activeFilters = !state.filters.isEmpty(),
        onSearchChange = { value ->
            searchDraft = value
            onDraftPendingChanged(value != state.search)
        },
        onSearchClear = {
            // La creu ha de recuperar «Per a tu» fins i tot si encara hi ha
            // una cerca pendent del debounce o filtres d'Opcions actius.
            state.selectFeedMode(FeedMode.FOR_YOU)
            onSearchApplied()
            onDraftPendingChanged(false)
        },
        onSearchSubmit = {
            state.updateSearch(searchDraft)
            state.commitSearch()
            onSearchApplied()
            onDraftPendingChanged(false)
        },
        onSuggestionSelected = { suggestion ->
            searchDraft = suggestion.value
            state.selectFeedSearchSuggestion(suggestion)
            onSearchApplied()
            onDraftPendingChanged(false)
        },
        onFilter = onFilter
    )
}

@Composable
private fun SearchAndFilter(
    search: String,
    suggestions: List<FeedSearchSuggestion>,
    activeFilters: Boolean,
    onSearchChange: (String) -> Unit,
    onSearchClear: () -> Unit,
    onSearchSubmit: () -> Unit,
    onSuggestionSelected: (FeedSearchSuggestion) -> Unit,
    onFilter: () -> Unit
) {
    var suppressSuggestions by remember { mutableStateOf(false) }
    var searchFieldWidth by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val searchScope = rememberCoroutineScope()
    val keepSearchEditing: () -> Unit = {
        searchScope.launch {
            delay(50)
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }
    val showSuggestions = !suppressSuggestions && search.isNotBlank() && suggestions.isNotEmpty()

    Row(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // El Box és l'àncora del menú. DropdownMenu usa un Popup, per tant els
        // suggeriments queden damunt del feed i no desplacen cap contingut.
        Box(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = search,
                onValueChange = {
                    suppressSuggestions = false
                    onSearchChange(it)
                },
                modifier = Modifier
                    .focusRequester(searchFocusRequester)
                    .fillMaxWidth()
                    .height(52.dp)
                    .onSizeChanged { size ->
                        searchFieldWidth = with(density) { size.width.toDp() }
                    },
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(19.dp), tint = AgendaBrandOrange) },
                trailingIcon = if (search.isNotBlank()) {
                    {
                        IconButton(onClick = {
                            suppressSuggestions = true
                            onSearchChange("")
                            onSearchClear()
                            keepSearchEditing()
                        }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Netejar cerca", modifier = Modifier.size(18.dp))
                        }
                    }
                } else null,
                placeholder = {
                    Text(
                        "Grup, poble, empresa, comarca…",
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                shape = RoundedCornerShape(15.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    suppressSuggestions = true
                    onSearchSubmit()
                    // En prémer “Cerca” al teclat, la cerca queda aplicada i el teclat es tanca.
                    focusManager.clearFocus(force = true)
                    keyboardController?.hide()
                })
            )

            DropdownMenu(
                expanded = showSuggestions,
                onDismissRequest = { suppressSuggestions = true },
                properties = PopupProperties(focusable = false),
                modifier = Modifier
                    .then(if (searchFieldWidth > 0.dp) Modifier.width(searchFieldWidth) else Modifier)
                    .heightIn(max = 360.dp)
            ) {
                suggestions.take(8).forEach { suggestion ->
                    DropdownMenuItem(
                        onClick = {
                            suppressSuggestions = true
                            onSuggestionSelected(suggestion)
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                        },
                        leadingIcon = {
                            Text(
                                when (suggestion.type) {
                                    FeedSearchSuggestionType.ARTIST -> "♫"
                                    FeedSearchSuggestionType.MUNICIPALITY -> "⌖"
                                    FeedSearchSuggestionType.COMARCA -> "▱"
                                    FeedSearchSuggestionType.COMPANY -> "▦"
                                    FeedSearchSuggestionType.VENUE -> "⌂"
                                    FeedSearchSuggestionType.STYLE -> "#"
                                },
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        text = {
                            Column {
                                Text(
                                    suggestion.value,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    suggestion.subtitle.ifBlank { suggestion.type.label },
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .height(44.dp)
                .width(88.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable {
                    suppressSuggestions = true
                    onFilter()
                },
            color = Color.White,
            border = BorderStroke(
                1.dp,
                if (activeFilters) AgendaBrandOrange.copy(alpha = .82f)
                else MaterialTheme.colorScheme.outline.copy(alpha = .18f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    Icons.Outlined.Tune,
                    contentDescription = "Opcions avançades",
                    modifier = Modifier.size(18.dp),
                    tint = AgendaBrandOrange
                )
                Text(
                    if (LocalUiLanguage.current == UiLanguage.SPANISH) "Opción." else "Opcions",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 10.dp),
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun GroupPostsRow(state: AgendaState, posts: List<GroupPost>) {
    val context = LocalContext.current
    LazyRow(
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(posts, key = { it.id }) { post ->
            Card(
                modifier = Modifier.width(280.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .55f))
            ) {
                Column {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ArtistAvatar(post.artist, 36.dp, state.publicPhotoForArtist(post.artist))
                        Spacer(Modifier.width(9.dp))
                        Text(post.artist, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        IconButton(
                            onClick = {
                                shareText(context, "${post.artist}: ${post.title}\n${post.body}")
                            }
                        ) {
                            Icon(Icons.Outlined.Share, contentDescription = "Compartir")
                        }
                    }

                    val postArtwork = rememberImageBitmapFromUri(state.publicPhotoForArtist(post.artist))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(145.dp)
                            .background(artistBrush(post.artist)),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        if (postArtwork != null) {
                            Image(
                                bitmap = postArtwork,
                                contentDescription = "Foto de ${post.artist}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .34f)))
                        }
                        Column(Modifier.padding(18.dp)) {
                            Text(
                                post.title,
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                "PUBLICACIÓ",
                                color = Color.White.copy(alpha = .72f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        post.body,
                        modifier = Modifier.padding(14.dp),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ConcertPost(
    concert: Concert,
    artistPhotoUri: String?,
    saved: Boolean,
    alerted: Boolean,
    followed: Boolean,
    savedCount: Int,
    viewCount: Int,
    comments: List<com.example.agendavalenciana.model.ConcertComment>,
    attendance: AttendanceStatus,
    onToggleSaved: () -> Unit,
    onToggleAlert: () -> Unit,
    onToggleFollow: () -> Unit,
    onToggleGoing: () -> Unit,
    onToggleWantToGo: () -> Unit,
    onAddComment: (String) -> Boolean,
    onBlockArtist: () -> Unit,
    onOpenArtist: () -> Unit,
    onShare: () -> Unit,
    onOpen: () -> Unit
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    var showComments by remember(concert.id) { mutableStateOf(false) }
    var commentText by remember(concert.id) { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.clickable(onClick = onOpenArtist)) {
                ArtistAvatar(concert.artist, 40.dp, artistPhotoUri)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f).clickable(onClick = onOpenArtist)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(concert.artist, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    if (!followed) {
                        TextButton(onClick = onToggleFollow, contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Text("Seguir", fontSize = 12.sp)
                        }
                    }
                }
                Text(
                    "${concert.town} · ${concert.comarca}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .58f)
                )
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "Més")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (followed) "Deixar de seguir" else "Seguir grup") },
                        onClick = {
                            menuExpanded = false
                            onToggleFollow()
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Outlined.Block, contentDescription = null) },
                        text = { Text("Bloqueja este grup") },
                        onClick = {
                            menuExpanded = false
                            onBlockArtist()
                        }
                    )
                }
            }
        }

        if (concert.sponsored) {
            Text(
                concert.sponsorLabel ?: "Patrocinat",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        EventArtwork(concert, artistPhotoUri, onOpen)

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TextButton(onClick = onToggleSaved) {
                        Icon(
                            if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (saved) "Guardat" else "Guardar",
                            tint = if (saved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Guardar", fontSize = 11.sp)
                    }
                    Text(savedCount.toString(), fontSize = 10.sp, lineHeight = 10.sp)
                }
            }
            item {
                TextButton(onClick = { showComments = !showComments }) {
                    Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Comentaris", modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Comentar", fontSize = 11.sp)
                }
            }
            item {
                TextButton(onClick = onOpen) {
                    Icon(Icons.Outlined.Groups, contentDescription = "Comunitat", modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Comunitat", fontSize = 11.sp)
                }
            }
            item {
                TextButton(
                    onClick = {
                        onShare()
                        EventShareCard.share(context, concert, null)
                    }
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = "Compartir", modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Compartir", fontSize = 11.sp)
                }
            }
        }

        Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 15.dp)) {
            Text(
                text = "$viewCount visualitzacions",
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 5.dp)
            )
            Text(
                text = concert.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = concert.description,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .72f),
                modifier = Modifier.padding(top = 3.dp)
            )

            if (comments.isNotEmpty()) {
                TextButton(
                    onClick = { showComments = !showComments },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        if (comments.size == 1) "Veure 1 comentari" else "Veure els ${comments.size} comentaris",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .58f)
                    )
                }
            }

            if (showComments) {
                comments.takeLast(3).forEach { comment ->
                    Row(modifier = Modifier.padding(top = 5.dp)) {
                        Text(comment.author, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(comment.text, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    }
                }
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Afig un comentari…") },
                        singleLine = true
                    )
                    TextButton(
                        onClick = {
                            if (onAddComment(commentText)) {
                                commentText = ""
                                showComments = true
                            }
                        },
                        enabled = commentText.isNotBlank()
                    ) { Text("Publicar") }
                }
            }
        }

        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = .45f))
    }
}

private fun categoryAccent(category: CulturalCategory): Color = when (category) {
    CulturalCategory.MUSIC -> Color(0xFF7357B4)
    CulturalCategory.THEATRE -> Color(0xFF9A4E56)
    CulturalCategory.CINEMA -> Color(0xFF4E6178)
    CulturalCategory.TALKS -> Color(0xFF4E756C)
    CulturalCategory.DANCE -> Color(0xFF8A5B75)
    CulturalCategory.EXHIBITIONS -> Color(0xFF987444)
    CulturalCategory.LITERATURE -> Color(0xFF5E704D)
    CulturalCategory.WORKSHOPS -> Color(0xFF577983)
    CulturalCategory.CHILDREN -> Color(0xFF94784B)
    CulturalCategory.POPULAR_CULTURE -> Color(0xFF8B6545)
    CulturalCategory.HERITAGE -> Color(0xFF66745A)
    CulturalCategory.OTHER -> Color(0xFF62666C)
}

/**
 * 10ba: dins de Música, la vora també comunica família musical.
 * Cambra té prioritat sobre Clàssica perquè sovint els dos termes conviuen.
 * Si l'estil no és prou clar, conservem Música general.
 */
private fun musicAccent(concert: Concert): Color {
    val style = concert.styles.joinToString(" ").lowercase(Locale.getDefault())
    return when {
        listOf("cambra", "chamber").any { style.contains(it) } -> Color(0xFF4F7D78)
        listOf("clàss", "class", "simf", "òpera", "opera", "líric", "barroc", "orquestr").any { style.contains(it) } -> Color(0xFF4A628C)
        listOf("jazz", "blues", "soul", "gospel", "swing").any { style.contains(it) } -> Color(0xFF8A6846)
        listOf("folk", "tradicional", "tradició", "arrel", "mediterr", "world music").any { style.contains(it) } -> Color(0xFF6C7A48)
        listOf("rock", "punk", "metal", "hardcore", "grunge").any { style.contains(it) } -> Color(0xFF8B4A4A)
        listOf("rap", "hip hop", "hip-hop", "trap", "urbana", "reggaeton", "reggaetón").any { style.contains(it) } -> Color(0xFF5F4D79)
        listOf("electr", "techno", "house", "ambient", "dance", "dj").any { style.contains(it) } -> Color(0xFF437989)
        listOf("pop", "indie", "cantautor", "singer-songwriter").any { style.contains(it) } -> Color(0xFF9A5C7E)
        else -> categoryAccent(CulturalCategory.MUSIC)
    }
}

private fun eventAccentColor(concert: Concert): Color =
    if (concert.category == CulturalCategory.MUSIC) musicAccent(concert) else categoryAccent(concert.category)

@Composable
private fun EventArtwork(concert: Concert, artistPhotoUri: String?, onOpen: () -> Unit) {
    // EventArtwork no rep AgendaState. Manté els candidats ja materialitzats al Concert;
    // el feed principal sí resol dinàmicament artista/sala/poble amb cardImageCandidates().
    val artwork = rememberImageBitmapFromUris(
        concert.imageCandidates(artistPhotoUri),
        eventId = concert.id,
        knownResolvedUri = concert.resolvedCardImageUri
    )
    val disciplineAccent = eventAccentColor(concert)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, disciplineAccent.copy(alpha = .88f), RoundedCornerShape(16.dp))
            .clickable(onClick = onOpen)
            .background(artistBrush(concert.artist))
    ) {
        if (artwork != null) {
            Image(
                bitmap = artwork,
                contentDescription = "Imatge de ${concert.title}",
                modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = concert.imageScale; scaleY = concert.imageScale },
                contentScale = ContentScale.Crop,
                alignment = BiasAlignment(concert.imageFocusX * 2f - 1f, concert.imageFocusY * 2f - 1f)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .68f))))
            )
        }
        Box(modifier = Modifier.fillMaxSize().padding(22.dp)) {
        Column(modifier = Modifier.align(Alignment.TopStart)) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = .46f)
            ) {
                Text(
                    text = concert.category.label.uppercase(Locale.getDefault()),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(modifier = Modifier.align(Alignment.BottomStart)) {
            Text(
                text = concert.title,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 36.sp,
                lineHeight = 37.sp
            )
            Spacer(Modifier.height(9.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CalendarToday, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(AgendaDateFormat.cardDate(concert.feedDate), color = Color.White, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(14.dp))
                Icon(Icons.Outlined.AccessTime, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Box(modifier = Modifier.width(48.dp)) {
                    if (concert.time.isNotBlank()) {
                        Text(concert.time, color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocationOn, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text(
                    "${concert.town} · ${concert.venue}",
                    color = Color.White.copy(alpha = .94f),
                    fontSize = 14.sp
                )
            }
        }
    }
        }
}

@Composable
private fun FollowSuggestionFeedRow(
    state: AgendaState,
    slot: Int,
    onOpenArtist: (String) -> Unit
) {
    val suggestions by produceState<List<FollowSuggestion>>(
        initialValue = emptyList(),
        key1 = slot,
        key2 = state.followSuggestionRevision,
        key3 = state.feedRenderRevision
    ) {
        value = withContext(Dispatchers.Default) { state.followSuggestions(slot = slot, limit = 2) }
    }
    // Cada targeta conserva la seua identitat. Seguir una proposta NO refà
    // l'altra targeta, encara que el catàleg global de suggeriments es recalculе.
    val positions = remember(slot) { mutableStateListOf<FollowSuggestion?>(null, null) }
    var rotationRequested by remember(slot) { mutableStateOf(false) }
    LaunchedEffect(slot, suggestions, state.followSuggestionRevision, state.feedRenderRevision) {
        if (rotationRequested) {
            // L'apartat «Canviar» sí pot substituir les dos targetes.
            if (suggestions.isNotEmpty()) {
                positions[0] = suggestions.getOrNull(0)
                positions[1] = suggestions.getOrNull(1)
                rotationRequested = false
            }
        } else if (positions.all { it == null }) {
            positions[0] = suggestions.getOrNull(0)?.takeUnless(state::isFollowingSuggestion)
            positions[1] = suggestions.getOrNull(1)?.takeUnless(state::isFollowingSuggestion)
        } else {
            // En sincronitzacions i seguiments externs, mantindre les targetes
            // encara vàlides en la mateixa columna, sense moure la parella.
            for (index in 0..1) {
                val current = positions[index]
                if (current == null || state.isFollowingSuggestion(current)) {
                    val excluded = positions.filterIndexed { i, _ -> i != index }
                        .filterNotNull().map { "${it.type}:${it.key}" }.toSet()
                    positions[index] = state.nextFollowSuggestion(slot, excluded, index)
                }
            }
        }
    }
    val visible = positions.filterNotNull()
    if (visible.isEmpty() && suggestions.isEmpty()) return
    val impressionKey = visible.joinToString("|") { "${it.type}:${it.key}" }
    LaunchedEffect(impressionKey) {
        if (visible.isNotEmpty()) state.markFollowSuggestionsShown(visible)
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Potser t’interessa seguir", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            TextButton(onClick = {
                rotationRequested = true
                state.rotateFollowSuggestions()
            }) { Text("Canviar", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (index in 0..1) {
                Box(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = positions[index],
                        transitionSpec = {
                            fadeIn(animationSpec = tween(durationMillis = 300, delayMillis = 450)) togetherWith
                                fadeOut(animationSpec = tween(durationMillis = 450))
                        },
                        label = "substitucio_suggeriment_$index"
                    ) { target ->
                        if (target != null) {
                            FollowSuggestionFeedCard(state, target, onOpenArtist) { selected ->
                                if (positions.getOrNull(index) != selected || state.isFollowingSuggestion(selected)) return@FollowSuggestionFeedCard
                                // Seguiment immediat; tan sols canvia esta posició.
                                state.followSuggestion(selected)
                                val excluded = positions.filterIndexed { i, _ -> i != index }
                                    .filterNotNull().map { "${it.type}:${it.key}" }.toSet()
                                positions[index] = state.nextFollowSuggestion(slot, excluded, index)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FollowSuggestionFeedCard(
    state: AgendaState,
    item: FollowSuggestion,
    onOpenArtist: (String) -> Unit,
    onFollow: (FollowSuggestion) -> Unit
) {
    val alreadyFollowing = state.isFollowingSuggestion(item)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (item.type) {
                            FollowSuggestionType.ARTIST -> Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { onOpenArtist(item.label) }
                            ) {
                                ArtistAvatar(
                                    artist = item.label,
                                    size = 48.dp,
                                    photoUri = state.publicPhotoForArtist(item.label)
                                )
                            }
                            FollowSuggestionType.ORGANIZER -> ArtistAvatar(
                                artist = item.label,
                                size = 48.dp,
                                photoUri = state.profileFor(item.key).photoUri
                            )
                            FollowSuggestionType.CATEGORY -> Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(categoryFollowEmoji(item.key), fontSize = 23.sp)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            item.label,
                            modifier = if (item.type == FollowSuggestionType.ARTIST) {
                                Modifier.clickable { onOpenArtist(item.label) }
                            } else Modifier,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            item.subtitle,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { if (!alreadyFollowing) onFollow(item) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (alreadyFollowing) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .42f) else Color.Transparent,
                                contentColor = if (alreadyFollowing) MaterialTheme.colorScheme.primary.copy(alpha = .72f) else MaterialTheme.colorScheme.primary
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (alreadyFollowing) MaterialTheme.colorScheme.primary.copy(alpha = .24f) else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Text(if (alreadyFollowing) "Seguint" else "Seguir", fontSize = 12.sp)
                        }
                    }
                }
}

@Composable
private fun EventArtistFollowRow(
    state: AgendaState,
    artistName: String,
    onOpenArtist: (String) -> Unit
) {
    val followed = artistName in state.followedArtists

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.clickable { onOpenArtist(artistName) }) {
            ArtistAvatar(
                artist = artistName,
                size = 40.dp,
                photoUri = state.publicPhotoForArtist(artistName)
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = artistName,
            modifier = Modifier
                .weight(1f)
                .clickable { onOpenArtist(artistName) }
                .padding(vertical = 8.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2
        )
        Spacer(Modifier.width(8.dp))
        OutlinedButton(
            onClick = { state.toggleFollowArtist(artistName) },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            modifier = Modifier.heightIn(min = 34.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (followed) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .42f) else Color.Transparent,
                contentColor = if (followed) MaterialTheme.colorScheme.primary.copy(alpha = .72f) else MaterialTheme.colorScheme.primary
            ),
            border = BorderStroke(
                1.dp,
                if (followed) MaterialTheme.colorScheme.primary.copy(alpha = .24f) else MaterialTheme.colorScheme.outline
            )
        ) {
            Text(if (followed) "Seguint" else "Seguir", fontSize = 12.sp)
        }
    }
}

private fun categoryFollowEmoji(key: String): String = when (key.uppercase()) {
    "MUSIC" -> "♫"
    "THEATRE" -> "🎭"
    "DANCE" -> "💃"
    "LITERATURE" -> "📚"
    "CINEMA" -> "🎬"
    "TALKS" -> "💬"
    "WORKSHOPS" -> "🛠"
    "EXHIBITIONS" -> "🖼"
    "POPULAR_CULTURE" -> "🎉"
    "CHILDREN" -> "🧒"
    else -> "✦"
}

@Composable
private fun ArtistAvatar(
    artist: String,
    size: androidx.compose.ui.unit.Dp,
    photoUri: String? = null
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(artistBrush(artist)),
        contentAlignment = Alignment.Center
    ) {
        AvatarImageOrInitials(
            label = artist,
            photoUri = photoUri,
            modifier = Modifier.fillMaxSize(),
            fontSize = (size.value * .32f).sp
        )
    }
}

@Composable
private fun AvatarImageOrInitials(
    label: String,
    photoUri: String?,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 18.sp
) {
    val image = rememberImageBitmapFromUri(photoUri, maxDimension = 192)

    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = label,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        val initials = label
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(initials, color = Color.White, fontWeight = FontWeight.Black, fontSize = fontSize)
        }
    }
}


@Composable
private fun GuestAccountScreen(
    state: AgendaState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onCreateAccount: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            TextButton(onClick = onBack) { Text("← Tornar") }
            Text("Usuari", fontWeight = FontWeight.Black, fontSize = 28.sp)
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Estàs utilitzant l’Agenda com a convidat", fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Text(
                        "Pots buscar, guardar, seguir i personalitzar Per a tu sense iniciar sessió. Estes dades es conserven al mòbil i se sincronitzen de manera anònima amb el servidor.",
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            Text("Configuració", fontWeight = FontWeight.Black, fontSize = 18.sp, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
            LanguageSettingsCard()
        }
        item {
            Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) { Text("Iniciar sessió") }
            OutlinedButton(onClick = onCreateAccount, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("Crear compte")
            }
            Text(
                "Inicia sessió quan vulgues comprar entrades o conservar les teues dades entre dispositius. En entrar, les dades del convidat es fusionaran amb el compte.",
                modifier = Modifier.padding(top = 8.dp),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AccountProfileScreen(
    state: AgendaState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val account = state.currentAccount ?: return
    val profile = state.profileFor(account.username)
    val context = LocalContext.current
    var profileName by remember(account.username, profile.displayName) { mutableStateOf(profile.displayName) }
    var profileBio by remember(account.username, profile.bio) { mutableStateOf(profile.bio) }
    var profileTown by remember(account.username, profile.homeTown) { mutableStateOf(profile.homeTown) }
    var profilePhoto by remember(account.username, profile.photoUri) { mutableStateOf(profile.photoUri) }
    var accountUsername by remember(account.username) { mutableStateOf(account.username) }
    var accountEmail by remember(account.username, account.email) { mutableStateOf(account.email.orEmpty()) }
    var saving by remember(account.username) { mutableStateOf(false) }
    var message by remember(account.username) { mutableStateOf<String?>(null) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            profilePhoto = runCatching { cachePickedImageForEditing(context, uri) }.getOrElse { uri.toString() }
            message = null
        }
    }
    val isSuperAdmin = account.role == AccountRole.SUPER_ADMIN
    var adminNotificationComposerOpen by remember(account.username) { mutableStateOf(false) }
    val isProfessional = account.role != AccountRole.USER && !isSuperAdmin
    val settings = state.professionalAccountSettings?.takeIf {
        state.professionalAccountSettingsOwnerId == account.serverUserId &&
            (it.email ?: "").equals(account.email.orEmpty(), ignoreCase = true)
    }
    var roleProfessional by remember(account.username) { mutableStateOf(isProfessional) }
    var phone by remember(account.username) { mutableStateOf("") }
    var savingRole by remember(account.username) { mutableStateOf(false) }
    var sendingEmail by remember(account.username) { mutableStateOf(false) }
    var roleMessage by remember(account.username) { mutableStateOf<String?>(null) }
    var emailMessage by remember(account.username) { mutableStateOf<String?>(null) }
    LaunchedEffect(account.serverUserId, account.username, account.email) {
        state.refreshProfessionalAccountSettings { ok, error ->
            if (!ok) roleMessage = error
        }
    }
    LaunchedEffect(settings) {
        if (settings != null && !savingRole) {
            phone = settings.phone
            roleProfessional = settings.role !in setOf("PUBLIC", "USER")
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← Tornar") }
                Spacer(Modifier.weight(1f))
            }
            Text("Perfil", fontWeight = FontWeight.Black, fontSize = 28.sp)
            Text("Pots modificar les dades directament ací i guardar-les en acabar.",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f)
            ) {
                Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(104.dp)) {
                        ArtistAvatar(profileName.ifBlank { account.displayName }, 104.dp, profilePhoto)
                        IconButton(
                            onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            modifier = Modifier.align(Alignment.TopEnd).offset(x = 9.dp, y = (-8).dp)
                                .size(32.dp).background(MaterialTheme.colorScheme.primary, CircleShape)
                        ) { Icon(Icons.Outlined.Add, contentDescription = "Canviar foto", tint = Color.White,
                            modifier = Modifier.size(19.dp)) }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(profileName.ifBlank { account.displayName }, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text("@${accountUsername}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
                OutlinedTextField(
                    profileName,
                    { profileName = it; message = null },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    label = { Text("Nom i cognoms") },
                    singleLine = true
                )
                OutlinedTextField(
                    accountUsername,
                    { accountUsername = it.trimStart(); message = null },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    label = { Text("Nom d'usuari") },
                    singleLine = true
                )
                OutlinedTextField(
                    accountEmail,
                    { accountEmail = it.trim(); message = null },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    label = { Text("Correu electrònic") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                Text("Rol", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
                if (isSuperAdmin) {
                    OutlinedButton(onClick = { adminNotificationComposerOpen = true },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Icon(Icons.Outlined.Notifications, contentDescription = null,
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Crear notificació · Superadmin")
                    }
                    // El Superadmin utilitza el mateix Perfil editable que la resta.
                    // El rol és una dada del servidor i no es pot canviar des d'ací.
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = true,
                            onClick = {},
                            enabled = false,
                            label = { Text("Superadmin") }
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !roleProfessional,
                            onClick = { roleProfessional = false; roleMessage = null },
                            label = { Text("Públic") }
                        )
                        FilterChip(
                            selected = roleProfessional,
                            onClick = { roleProfessional = true; roleMessage = null },
                            label = { Text("Professional") }
                        )
                    }
                    if (roleProfessional) {
                        OutlinedTextField(
                            phone,
                            { phone = it; roleMessage = null },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Telèfon professional (obligatori)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )
                    }
                }
                roleMessage?.let { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.error) }

                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!profilePhoto.isNullOrBlank()) {
                        OutlinedButton(onClick = { profilePhoto = null }, modifier = Modifier.weight(1f)) { Text("Llevar foto") }
                    }
                    Button(
                        enabled = !saving && !savingRole,
                        onClick = {
                            // Els canvis personals funcionen encara que el nou servidor
                            // professional no estiga desplegat; només fem POST de rol quan canvia.
                            val originalProfessional = settings?.role?.let { it !in setOf("PUBLIC", "USER") } ?: isProfessional
                            val roleChanged = !isSuperAdmin && (
                                roleProfessional != originalProfessional ||
                                    (roleProfessional && settings != null && phone.trim() != settings.phone.trim())
                                )
                            when {
                                accountUsername.trim().length < 3 -> message = "El nom d'usuari ha de tindre almenys 3 caràcters."
                                !accountEmail.contains("@") -> message = "Escriu un correu electrònic vàlid."
                                profileName.isBlank() -> message = "Falten el nom i els cognoms."
                                !isSuperAdmin && roleProfessional && (settings != null || roleChanged) &&
                                    !phone.trim().matches(Regex("^[+0-9() .-]{7,24}$")) -> message = "Indica un telèfon professional vàlid."
                                else -> {
                                    saving = true
                                    val remove = profilePhoto == null && !profile.photoUri.isNullOrBlank()
                                    state.updateOwnAccountRemote(
                                        username = accountUsername,
                                        email = accountEmail,
                                        homeTown = profile.homeTown,
                                        displayName = profileName,
                                        bio = profile.bio,
                                        photoUri = profilePhoto,
                                        removeImage = remove,
                                        onProgress = {},
                                        onResult = { ok, text, remoteUrl ->
                                            saving = false
                                            message = text
                                            if (ok) {
                                                profilePhoto = remoteUrl
                                                if (roleChanged) {
                                                    savingRole = true
                                                    state.updateProfessionalAccountSettings(roleProfessional, phone) { roleOk, roleText ->
                                                        savingRole = false
                                                        roleMessage = if (roleOk) null else roleText
                                                        message = if (roleOk) "Perfil guardat." else "Dades personals guardades; falta guardar el rol."
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text(if (saving || savingRole) "Guardant…" else "Guardar") }
                }
                message?.let { Text(it, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp)) }
        }
        item {
            Text("Configuració", fontWeight = FontWeight.Black, fontSize = 18.sp, modifier = Modifier.padding(top = 4.dp, bottom = 2.dp))
            LanguageSettingsCard(modifier = Modifier.padding(top = 6.dp))
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (account.email.isNullOrBlank()) {
                    Text("Afig un correu electrònic per poder-lo verificar.", fontSize = 12.sp)
                } else if (settings == null) {
                    Text("Verificació pendent de consultar al servidor.", fontSize = 12.sp)
                } else if (!settings.emailVerified) {
                    Text("Correu sense confirmar", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    OutlinedButton(enabled = !sendingEmail, onClick = {
                        sendingEmail = true
                        state.sendMyEmailVerification { ok, msg ->
                            sendingEmail = false
                            emailMessage = msg
                        }
                    }) { Text(if (sendingEmail) "Enviant…" else "Enviar correu de confirmació") }
                    TextButton(onClick = { state.refreshProfessionalAccountSettings() }) {
                        Text("Ja l'he confirmat · Comprovar")
                    }
                } else {
                    Text("Correu confirmat", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
                emailMessage?.let { Text(it, fontSize = 12.sp) }
            }
        }
        item { OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("Tancar sessió") } }
    }
    if (adminNotificationComposerOpen && isSuperAdmin) {
        AdminNotificationsComposer(onDismiss = { adminNotificationComposerOpen = false })
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .35f)) {
        Column(Modifier.padding(horizontal = 15.dp, vertical = 12.dp)) {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

/** v236: centre de preferències permanents. No són els filtres temporals del cercador. */
@Composable
private fun DiscoverScreen(
    state: AgendaState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenArtist: (String) -> Unit
) {
    var section by rememberSaveable { mutableStateOf(0) }
    var search by remember { mutableStateOf("") }
    var locationSearch by remember { mutableStateOf("") }
    var moreLimit by remember { mutableStateOf(20) }
    var categoryToBlock by remember { mutableStateOf<CulturalCategory?>(null) }
    var expandedCategory by rememberSaveable { mutableStateOf("") }
    var artistToBlock by remember { mutableStateOf<String?>(null) }
    var confirmLearningReset by remember { mutableStateOf(false) }
    val notificationContext = LocalContext.current
    val allArtists = remember(state.concerts, state.followedArtists, state.blockedArtists) {
        (state.concerts.flatMap { it.lineup } + state.followedArtists + state.blockedArtists)
            .map(String::trim).filter(String::isNotBlank).distinct().sorted()
    }
    val personalizationContext = LocalContext.current
    var personalizationNotificationsEnabled by remember {
        mutableStateOf(EventReminderScheduler.isAndroidEnabled(personalizationContext))
    }
    val personalizationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()) { granted ->
        EventReminderScheduler.setAndroidEnabled(personalizationContext, granted)
        personalizationNotificationsEnabled = granted
    }
    val lowerSearch = search.trim().lowercase(Locale.getDefault())

    LaunchedEffect(section, search) { moreLimit = 20 }
    categoryToBlock?.let { category ->
        AlertDialog(
            onDismissRequest = { categoryToBlock = null },
            title = { Text("No mostrar ${category.label}?") },
            text = { Text("No veuràs més esdeveniments d'esta categoria. Podràs revertir-ho en Personalització, dins de Contingut bloquejat.") },
            confirmButton = {
                Button(onClick = { state.blockCategory(category); categoryToBlock = null }) {
                    Text("Confirmar bloqueig")
                }
            },
            dismissButton = { TextButton(onClick = { categoryToBlock = null }) { Text("Cancel·lar") } }
        )
    }
    artistToBlock?.let { artist ->
        AlertDialog(
            onDismissRequest = { artistToBlock = null },
            title = { Text("No mostrar $artist?") },
            text = { Text("No tornaran a aparéixer els seus concerts en solitari. Els festivals amb altres artistes continuaran visibles. Podràs revertir-ho en Personalització.") },
            confirmButton = {
                Button(onClick = { state.blockArtist(artist); artistToBlock = null }) {
                    Text("Confirmar bloqueig")
                }
            },
            dismissButton = { TextButton(onClick = { artistToBlock = null }) { Text("Cancel·lar") } }
        )
    }

    if (confirmLearningReset) {
        AlertDialog(
            onDismissRequest = { confirmLearningReset = false },
            title = { Text("Reiniciar els gustos apresos?") },
            text = { Text("S'esborraran les afinitats apreses de les cerques i de les fitxes que has obert, també del compte al servidor quan es sincronitze. Es conservaran els artistes i disciplines seguits, els llocs, els guardats i els bloquejos.") },
            confirmButton = {
                Button(onClick = {
                    confirmLearningReset = false
                    state.resetLearnedPersonalization()
                }) { Text("Reiniciar gustos apresos") }
            },
            dismissButton = {
                TextButton(onClick = { confirmLearningReset = false }) { Text("Cancel·lar") }
            }
        )
    }

    // v240: les cinc opcions de Personalització caben alhora en UNA ÚNICA FILA.
    // El text abreujat és només visual; el nom complet queda a l'accessibilitat.
    val sections = listOf("Categories", "Artistes", "Localitzacions", "Bloquejats", "Preferències")
    val sectionIcons = listOf(
        Icons.Outlined.Album,
        Icons.Outlined.MusicNote,
        Icons.Outlined.LocationOn,
        Icons.Outlined.Block,
        Icons.Outlined.Tune
    )
    val listState = rememberLazyListState()
    LaunchedEffect(section) { listState.scrollToItem(0) }
    Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.ChevronLeft, contentDescription = "Tornar", modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(4.dp))
            Text("Personalització", fontWeight = FontWeight.Black, fontSize = 19.sp,
                modifier = Modifier.weight(1f))
            Text("v${BuildConfig.VERSION_CODE}", fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        // Les cinc destinacions es veuen alhora. Una fila fixa, no scrollable.
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            sections.forEachIndexed { index, label ->
                PersonalizationSectionButton(
                    label = label,
                    icon = sectionIcons[index],
                    selected = section == index,
                    modifier = Modifier.weight(1f),
                    onClick = { section = index; search = "" }
                )
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
        when (section) {
            0 -> {
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Categories culturals", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f))
                        Text("✓ Seguint · ⋮ Opcions", fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedTextField(value = search, onValueChange = { search = it },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null,
                            modifier = Modifier.size(18.dp)) },
                        placeholder = { Text("Busca una categoria") },
                        textStyle = MaterialTheme.typography.bodySmall)
                }
                // Categories generals desplegables: seguir tota la categoria o només subcategories.
                val visibleCategories = CategoryTaxonomy.mainCategories.filter { category ->
                    lowerSearch.isBlank() || category.label.lowercase(Locale.getDefault()).contains(lowerSearch) ||
                        CategoryTaxonomy.childrenOf(category).any { child ->
                            child.label.lowercase(Locale.getDefault()).contains(lowerSearch)
                        }
                }
                val showInfantil = lowerSearch.isBlank() || "infantil".contains(lowerSearch) || "famílies".contains(lowerSearch)
                if (visibleCategories.isEmpty() && !showInfantil) {
                    item { Text("No hi ha categories o subcategories que coincidisquen.") }
                }
                items(visibleCategories, key = { "category_${it.name}" }) { category ->
                    val blocked = category in state.blockedCategories
                    val followedAll = state.categoryFollowsAll(category)
                    val children = CategoryTaxonomy.childrenOf(category)
                    val selectedCount = state.followedSubcategoryCount(category)
                    val expanded = expandedCategory == category.name ||
                        (lowerSearch.isNotBlank() && children.any {
                            it.label.lowercase(Locale.getDefault()).contains(lowerSearch)
                        })
                    var categoryMenuExpanded by remember(category) { mutableStateOf(false) }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(9.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .28f)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                                    .padding(horizontal = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier.size(32.dp).clip(RoundedCornerShape(7.dp))
                                        .background(categoryAccent(category))
                                ) {
                                    AvatarImageOrInitials(
                                        category.label, state.publicPhotoForCategory(category),
                                        Modifier.fillMaxSize(), 11.sp
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(
                                    modifier = Modifier.weight(1f).clickable {
                                        expandedCategory = if (expandedCategory == category.name) "" else category.name
                                    },
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(category.label, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        when {
                                            blocked -> "Bloquejada"
                                            followedAll -> "Tota la categoria"
                                            selectedCount > 0 -> "$selectedCount de ${children.size} subcategories"
                                            else -> "Desplega per triar"
                                        },
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp, maxLines = 1
                                    )
                                }
                                IconButton(
                                    onClick = { state.toggleFollowCategory(category) },
                                    modifier = Modifier.size(33.dp)
                                ) {
                                    Icon(
                                        if (followedAll) Icons.Outlined.CheckCircle else Icons.Outlined.Add,
                                        contentDescription = if (followedAll) "Deixar de seguir tota ${category.label}"
                                            else "Seguir tota ${category.label}",
                                        modifier = Modifier.size(20.dp),
                                        tint = if (followedAll) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        expandedCategory = if (expandedCategory == category.name) "" else category.name
                                    }, modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                        contentDescription = if (expanded) "Plegar ${category.label}"
                                            else "Desplegar ${category.label}",
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                Box {
                                    IconButton(onClick = { categoryMenuExpanded = true },
                                        modifier = Modifier.size(30.dp)) {
                                        Icon(Icons.Outlined.MoreVert,
                                            contentDescription = "Més opcions per a ${category.label}",
                                            modifier = Modifier.size(18.dp))
                                    }
                                    DropdownMenu(
                                        expanded = categoryMenuExpanded,
                                        onDismissRequest = { categoryMenuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(if (blocked) "Tornar a mostrar" else "No mostrar categoria") },
                                            leadingIcon = { Icon(if (blocked) Icons.Outlined.CheckCircle else Icons.Outlined.Block,
                                                contentDescription = null) },
                                            onClick = {
                                                categoryMenuExpanded = false
                                                if (blocked) state.unblockCategory(category) else categoryToBlock = category
                                            }
                                        )
                                    }
                                }
                            }
                            if (expanded) {
                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = .18f))
                                children.filter { child ->
                                    lowerSearch.isBlank() || category.label.lowercase(Locale.getDefault()).contains(lowerSearch) ||
                                        child.label.lowercase(Locale.getDefault()).contains(lowerSearch)
                                }.forEach { child ->
                                    val isSelected = state.categoryFollowsSubcategory(child)
                                    val blockedChild = state.isSubcategoryBlocked(child)
                                    Row(
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp)
                                            .clickable { state.toggleFollowSubcategory(child) }
                                            .padding(start = 40.dp, end = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(child.label, modifier = Modifier.weight(1f), fontSize = 12.sp,
                                            maxLines = 2)
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { state.toggleFollowSubcategory(child) },
                                            modifier = Modifier.size(35.dp)
                                        )
                                        IconButton(
                                            onClick = {
                                                if (blockedChild) state.unblockSubcategory(child)
                                                else state.blockSubcategory(child)
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                if (blockedChild) Icons.Outlined.CheckCircle else Icons.Outlined.Block,
                                                contentDescription = if (blockedChild) "Restaurar ${child.label}" else "Bloquejar ${child.label}",
                                                tint = if (blockedChild) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // Infantil no és una dotzena categoria: és un interés transversal,
                // vàlid també per a cinema, teatre, música, tallers, etc.
                if (showInfantil) {
                    item(key = "transversal_infantil") {
                        val isSelected = state.categoryFollowsAll(CulturalCategory.CHILDREN)
                        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(9.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .28f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                                    .clickable { state.toggleFollowCategory(CulturalCategory.CHILDREN) }
                                    .padding(horizontal = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(32.dp).clip(RoundedCornerShape(7.dp))
                                    .background(categoryAccent(CulturalCategory.CHILDREN))) {
                                    AvatarImageOrInitials("Infantil",
                                        state.publicPhotoForCategory(CulturalCategory.CHILDREN),
                                        Modifier.fillMaxSize(), 11.sp)
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Infantil", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Transversal · totes les categories", fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Checkbox(checked = isSelected,
                                    onCheckedChange = { state.toggleFollowCategory(CulturalCategory.CHILDREN) })
                            }
                        }
                    }
                }
            }

            1 -> {
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Artistes i grups", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f))
                        Text("✓ Seguint · ⋮ Opcions", fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedTextField(value = search, onValueChange = { search = it },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null,
                            modifier = Modifier.size(18.dp)) },
                        placeholder = { Text("Busca un artista o grup") },
                        textStyle = MaterialTheme.typography.bodySmall)
                }
                val visibleArtists = allArtists.filter { artist ->
                    (lowerSearch.isBlank() || artist.lowercase(Locale.getDefault()).contains(lowerSearch)) &&
                        !state.isArtistBlocked(artist)
                }.sortedWith(compareByDescending<String> { it in state.followedArtists }.thenBy { it })
                if (visibleArtists.isEmpty()) item { Text("No hi ha artistes que coincidisquen.") }
                items(visibleArtists.take(moreLimit), key = { "artist_$it" }) { artist ->
                    var artistMenuExpanded by remember(artist) { mutableStateOf(false) }
                    val following = artist in state.followedArtists
                    val alertsEnabled = artist in state.artistAlerts
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(9.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .28f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(43.dp)
                                .padding(horizontal = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(32.dp).clip(RoundedCornerShape(7.dp)).background(artistBrush(artist))) {
                                AvatarImageOrInitials(artist, state.publicPhotoForArtist(artist), Modifier.fillMaxSize(), 11.sp)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                artist,
                                modifier = Modifier.weight(1f).clickable { onOpenArtist(artist) },
                                fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            if (following && alertsEnabled) {
                                Icon(Icons.Outlined.Notifications, contentDescription = "Avisos activats",
                                    modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(
                                onClick = { state.toggleFollowArtist(artist) },
                                modifier = Modifier.size(35.dp)
                            ) {
                                Icon(
                                    if (following) Icons.Outlined.CheckCircle else Icons.Outlined.Add,
                                    contentDescription = if (following) "Deixar de seguir $artist" else "Seguir $artist",
                                    modifier = Modifier.size(22.dp),
                                    tint = if (following) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box {
                                IconButton(
                                    onClick = { artistMenuExpanded = true },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Outlined.MoreVert, contentDescription = "Més opcions per a $artist", modifier = Modifier.size(20.dp))
                                }
                                DropdownMenu(
                                    expanded = artistMenuExpanded,
                                    onDismissRequest = { artistMenuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        enabled = following,
                                        text = { Text(if (alertsEnabled) "Desactivar avisos" else "Activar avisos") },
                                        leadingIcon = { Icon(Icons.Outlined.Notifications, contentDescription = null) },
                                        onClick = {
                                            artistMenuExpanded = false
                                            val activating = artist !in state.artistAlerts
                                            state.toggleArtistAlert(artist)
                                            Toast.makeText(notificationContext,
                                                if (activating) "Avisos de $artist guardats." else "Avisos de $artist desactivats.",
                                                Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("No mostrar artista") },
                                        leadingIcon = { Icon(Icons.Outlined.Block, contentDescription = null) },
                                        onClick = { artistMenuExpanded = false; artistToBlock = artist }
                                    )
                                }
                            }
                        }
                    }
                }
                if (moreLimit < visibleArtists.size) item {
                    OutlinedButton(onClick = { moreLimit += 20 }, modifier = Modifier.fillMaxWidth()) {
                        Text("Mostrar 20 artistes més")
                    }
                }
            }
            2 -> {
                item { Text("Les meues localitzacions", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                if (state.currentAccount == null) {
                    item { Text("Inicia sessió per guardar estos llocs al teu compte i recuperar-los en altres dispositius.", fontSize = 12.sp) }
                }
                state.placesSyncError?.let { error ->
                    item { Text(error, fontSize = 12.sp, color = MaterialTheme.colorScheme.error) }
                }
                if (state.currentAccount != null && state.placesSavedOnServer && state.placesSyncError == null) {
                    item { Text("Llocs sincronitzats amb el teu compte.", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                val selectedTowns = state.userPreferences.preferredLocations.filter(String::isNotBlank)
                    .sortedWith(String.CASE_INSENSITIVE_ORDER)
                val selectedComarques = state.userPreferences.preferredComarques.filter(String::isNotBlank)
                    .sortedWith(String.CASE_INSENSITIVE_ORDER)
                if (selectedTowns.isEmpty() && selectedComarques.isEmpty()) {
                    item { Text("No hi ha localitzacions seleccionades.", fontSize = 12.sp) }
                }
                items(selectedTowns, key = { "preferred_town_$it" }) { town ->
                    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(9.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .28f)) {
                        Row(modifier = Modifier.fillMaxWidth().height(47.dp).padding(horizontal = 5.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(32.dp).clip(RoundedCornerShape(7.dp))) {
                                AvatarImageOrInitials(town, state.publicPhotoForTown(town), Modifier.fillMaxSize(), 11.sp)
                            }
                            Spacer(Modifier.width(7.dp))
                            Column(Modifier.weight(1f)) {
                                Text(town, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Poble", fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            var radiusMenuExpanded by remember(town) { mutableStateOf(false) }
                            Box {
                                TextButton(onClick = { radiusMenuExpanded = true },
                                    contentPadding = PaddingValues(horizontal = 4.dp)) {
                                    Text("${state.userPreferences.radiusForLocation(town)} km", fontSize = 11.sp)
                                    Icon(Icons.Outlined.ExpandMore, contentDescription = "Radi de $town",
                                        modifier = Modifier.size(15.dp))
                                }
                                DropdownMenu(expanded = radiusMenuExpanded, onDismissRequest = { radiusMenuExpanded = false }) {
                                    PersonalizationOptions.locationRadiiKm.forEach { radius ->
                                        DropdownMenuItem(text = { Text("$radius km") }, onClick = {
                                            state.updateUserPreferences(state.userPreferences.copy(
                                                locationRadiiKm = state.userPreferences.locationRadiiKm + (town to radius)))
                                            radiusMenuExpanded = false
                                        })
                                    }
                                }
                            }
                            IconButton(onClick = { state.updateUserPreferences(state.userPreferences.copy(
                                preferredLocations = state.userPreferences.preferredLocations - town)) },
                                modifier = Modifier.size(31.dp)) {
                                Icon(Icons.Outlined.Close, contentDescription = "Llevar $town",
                                    modifier = Modifier.size(17.dp))
                            }
                        }
                    }
                }
                items(selectedComarques, key = { "preferred_comarca_$it" }) { comarca ->
                    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(9.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .28f)) {
                        Row(modifier = Modifier.fillMaxWidth().height(47.dp).padding(horizontal = 5.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(32.dp).clip(RoundedCornerShape(7.dp))) {
                                AvatarImageOrInitials(comarca, state.publicPhotoForComarca(comarca), Modifier.fillMaxSize(), 11.sp)
                            }
                            Spacer(Modifier.width(7.dp))
                            Column(Modifier.weight(1f)) {
                                Text(comarca, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Comarca", fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { state.updateUserPreferences(state.userPreferences.copy(
                                preferredComarques = state.userPreferences.preferredComarques - comarca)) },
                                modifier = Modifier.size(31.dp)) {
                                Icon(Icons.Outlined.Close, contentDescription = "Llevar $comarca",
                                    modifier = Modifier.size(17.dp))
                            }
                        }
                    }
                }
                item {
                    val typed = locationSearch.trim()
                    val town = state.municipalityFor(typed)?.name
                    val comarca = state.canonicalComarca(typed)
                    val alreadyAdded = (town != null && town in state.userPreferences.preferredLocations) ||
                        (comarca != null && comarca in state.userPreferences.preferredComarques)
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        OutlinedTextField(value = locationSearch, onValueChange = { locationSearch = it },
                            modifier = Modifier.weight(1f), singleLine = true,
                            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                            placeholder = { Text("Poble o comarca") })
                        Button(enabled = typed.isNotBlank() && !alreadyAdded, onClick = {
                            when {
                                town != null -> state.updateUserPreferences(state.userPreferences.copy(
                                    preferredLocations = state.userPreferences.preferredLocations + town))
                                comarca != null -> state.updateUserPreferences(state.userPreferences.copy(
                                    preferredComarques = state.userPreferences.preferredComarques + comarca))
                                else -> state.updateUserPreferences(state.userPreferences.copy(
                                    preferredLocations = state.userPreferences.preferredLocations + typed))
                            }
                            locationSearch = ""
                        }) { Text("Afegir") }
                    }
                }
                val matchingTowns = if (locationSearch.isBlank()) emptyList() else {
                    state.municipalityAutocompleteItems().asSequence()
                        .map { it.name }.filter(String::isNotBlank).distinct()
                        .filter { it.contains(locationSearch.trim(), ignoreCase = true) }
                        .filterNot { it in state.userPreferences.preferredLocations }
                        .take(7).toList()
                }
                items(matchingTowns, key = { "town_suggestion_$it" }) { town ->
                    Row(modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp)
                        .clickable {
                            state.updateUserPreferences(state.userPreferences.copy(
                                preferredLocations = state.userPreferences.preferredLocations + town))
                            locationSearch = ""
                        }.padding(horizontal = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(30.dp).clip(RoundedCornerShape(7.dp))) {
                            AvatarImageOrInitials(town, state.publicPhotoForTown(town), Modifier.fillMaxSize(), 11.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(town, modifier = Modifier.weight(1f), fontSize = 12.sp)
                        Text("Poble", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Icon(Icons.Outlined.Add, contentDescription = "Afegir $town", modifier = Modifier.size(17.dp))
                    }
                }
                val matchingComarques = if (locationSearch.isBlank()) emptyList() else {
                    state.knownComarques().asSequence()
                        .filter { it.contains(locationSearch.trim(), ignoreCase = true) }
                        .filterNot { it in state.userPreferences.preferredComarques }
                        .take(7).toList()
                }
                items(matchingComarques, key = { "comarca_suggestion_$it" }) { comarca ->
                    Row(modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp)
                        .clickable {
                            state.updateUserPreferences(state.userPreferences.copy(
                                preferredComarques = state.userPreferences.preferredComarques + comarca))
                            locationSearch = ""
                        }.padding(horizontal = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(30.dp).clip(RoundedCornerShape(7.dp))) {
                            AvatarImageOrInitials(comarca, state.publicPhotoForComarca(comarca), Modifier.fillMaxSize(), 11.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(comarca, modifier = Modifier.weight(1f), fontSize = 12.sp)
                        Text("Comarca", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Icon(Icons.Outlined.Add, contentDescription = "Afegir $comarca", modifier = Modifier.size(17.dp))
                    }
                }
            }
            3 -> {
                item {
                    Text("Contingut bloquejat", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    VisibilityExclusionsSection(state)
                }
            }
            else -> {
                item {
                    ToggleRow(
                        title = "Activar notificacions Android",
                        subtitle = "Permet avisos del sistema. Pots modificar la freqüència i les campanetes en Notificacions.",
                        checked = personalizationNotificationsEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= 33 &&
                                ContextCompat.checkSelfPermission(personalizationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            ) personalizationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            else {
                                EventReminderScheduler.setAndroidEnabled(personalizationContext, enabled)
                                personalizationNotificationsEnabled = enabled
                            }
                        }
                    )
                    Text("Preferències", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text("Decidix com es personalitza l'Agenda.", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ToggleRow(
                        title = "Prioritzar valencià",
                        subtitle = "Dona més pes als esdeveniments en valencià, sense excloure altres idiomes.",
                        checked = state.userPreferences.valencianFirst,
                        onCheckedChange = {
                            state.updateUserPreferences(state.userPreferences.copy(valencianFirst = it))
                        }
                    )
                    ToggleRow(
                        title = "Mostrar suggerències",
                        subtitle = "Mostra recomanacions a «Per a tu» i a la pestanya de suggerències de Notificacions.",
                        checked = state.userPreferences.showSuggestions,
                        onCheckedChange = {
                            state.updateUserPreferences(state.userPreferences.copy(showSuggestions = it))
                        }
                    )
                    if (state.userPreferences.showSuggestions) {
                        Spacer(Modifier.height(12.dp))
                        Text("Nivell de descoberta", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text("Tria si preferixes els teus gustos o més propostes diferents.",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    DiscoveryLevel.values().forEach { level ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.userPreferences.discoveryLevel == level,
                                onClick = {
                                    state.updateUserPreferences(state.userPreferences.copy(discoveryLevel = level))
                                }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(level.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(level.description, fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text("Gustos apresos automàticament", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(
                        "Les cerques i les fitxes que obris poden influir en «Per a tu» i en els suggeriments. " +
                            "Pots reiniciar només eixe aprenentatge, sense perdre cap preferència que hages triat.",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { confirmLearningReset = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Reiniciar afinitats de cerques i fitxes")
                    }
                    state.learningResetFeedback?.let { message ->
                        Text(message, fontSize = 12.sp,
                            color = if (state.learningResetSyncPending) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun PersonalizationSectionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.height(48.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(9.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .42f),
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .5f)) else null
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 2.dp, vertical = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon, contentDescription = label,
                modifier = Modifier.size(18.dp),
                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(1.dp))
            Text(
                when (label) {
                    "Categories" -> "Cats"
                    "Localitzacions" -> "Llocs"
                    "Bloquejats" -> "Bloq."
                    "Preferències" -> "Prefs"
                    else -> label
                },
                maxLines = 1,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DiscoverArtistRow(state: AgendaState, artist: String, onOpenArtist: (String) -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .32f)) {
        Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(artistBrush(artist))) {
                AvatarImageOrInitials(artist, state.publicPhotoForArtist(artist), Modifier.fillMaxSize(), 14.sp)
            }
            Spacer(Modifier.width(10.dp))
            Text(artist, modifier = Modifier.weight(1f).clickable { onOpenArtist(artist) }, fontWeight = FontWeight.SemiBold)
            AssistChip(onClick = { state.toggleFollowArtist(artist) }, label = { Text(if (artist in state.followedArtists) "✓ Seguint" else "+ Seguir") })
        }
    }
}

@Composable
private fun DiscoverOrganizerRow(state: AgendaState, username: String, name: String, subtitle: String) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .32f)) {
        Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Text(name.take(2).uppercase(), fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) { Text(name, fontWeight = FontWeight.SemiBold); Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            AssistChip(onClick = { state.toggleFollowOrganizer(username) }, label = { Text(if (username in state.followedOrganizers) "✓ Seguint" else "+ Seguir") })
        }
    }
}

@Composable
private fun DiscoverVenueRow(state: AgendaState, name: String, subtitle: String) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .32f)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.SemiBold)
                Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AssistChip(
                onClick = { state.toggleFollowVenue(name) },
                label = { Text(if (name in state.followedVenues) "✓ Seguint" else "+ Seguir") }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiscoverSheet(
    state: AgendaState,
    onOpenArtist: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<CulturalCategory?>(null) }
    var mode by remember { mutableStateOf("all") }

    val normalized = query.trim().lowercase(Locale.getDefault())
    val allConcerts = state.concerts
    val affinityCategories = buildList {
        addAll(allConcerts.filter { it.id in state.savedConcerts }.map { it.category })
        addAll(allConcerts.filter { concert -> concert.lineup.any { it in state.followedArtists } }.map { it.category })
    }.distinct()
    val suggestedCategories = if (affinityCategories.isNotEmpty()) affinityCategories else CulturalCategory.entries.toList()

    val artists = allConcerts
        .asSequence()
        .filter { selectedCategory == null || it.category == selectedCategory }
        .flatMap { it.lineup.asSequence() }
        .distinct()
        .filter { normalized.isBlank() || it.lowercase(Locale.getDefault()).contains(normalized) }
        .toList()

    val organizers = DemoData.accounts
        .asSequence()
        .filter { it.role in setOf(AccountRole.AGENCY, AccountRole.LABEL, AccountRole.VENUE, AccountRole.PROMOTER) }
        .filter { account ->
            if (selectedCategory == null) true
            else {
                val relatedArtists = account.managedArtists
                val relatedVenues = account.managedVenues
                allConcerts.any { concert ->
                    concert.category == selectedCategory &&
                        (concert.lineup.any { it in relatedArtists } || concert.venue in relatedVenues || account.username in concert.coEditorUsernames)
                }
            }
        }
        .filter { normalized.isBlank() || it.displayName.lowercase(Locale.getDefault()).contains(normalized) }
        .toList()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        SheetContent {
            SheetHeader("Descobrir", "Busca grups i organitzadors o explora suggeriments per tipologia.")

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                placeholder = { Text("Grup, organitzador o activitat") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        state.recordSearchTerm(query)
                    }
                )
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(selected = mode == "all", onClick = { mode = "all" }, label = { Text("Tot") })
                }
                item {
                    FilterChip(selected = mode == "groups", onClick = { mode = "groups" }, label = { Text("Grups") })
                }
                item {
                    FilterChip(selected = mode == "organizers", onClick = { mode = "organizers" }, label = { Text("Organitzadors") })
                }
            }

            Text("Tipus d’activitat", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("Totes") }
                    )
                }
                items(suggestedCategories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = {
                            selectedCategory = if (selectedCategory == category) null else category
                            state.recordCategoryInterest(category)
                        },
                        label = { Text(category.label) }
                    )
                }
            }

            if (mode != "organizers") {
                Text(
                    if (query.isBlank()) "Grups suggerits" else "Grups",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
                if (artists.isEmpty()) {
                    Text("No hi ha grups que coincidisquen.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    artists.take(12).forEach { artist ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .35f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(artistBrush(artist)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AvatarImageOrInitials(
                                        label = artist,
                                        photoUri = state.publicPhotoForArtist(artist),
                                        modifier = Modifier.fillMaxSize(),
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    artist,
                                    modifier = Modifier.weight(1f).clickable { onOpenArtist(artist) },
                                    fontWeight = FontWeight.SemiBold
                                )
                                AssistChip(
                                    onClick = { state.toggleFollowArtist(artist) },
                                    label = { Text(if (artist in state.followedArtists) "✓ Seguint" else "+ Seguir") }
                                )
                            }
                        }
                    }
                }
            }

            if (mode != "groups") {
                Text(
                    if (query.isBlank()) "Organitzadors suggerits" else "Organitzadors",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
                if (organizers.isEmpty()) {
                    Text("No hi ha organitzadors que coincidisquen.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    organizers.forEach { organizer ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .35f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(organizer.displayName.take(2).uppercase(), fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(organizer.displayName, fontWeight = FontWeight.SemiBold)
                                    Text(organizer.roleLabel, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                AssistChip(
                                    onClick = { state.toggleFollowOrganizer(organizer.username) },
                                    label = { Text(if (organizer.username in state.followedOrganizers) "✓ Seguint" else "+ Seguir") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun FilterDepartmentCard(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    icon: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .16f))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) { icon() }
                Spacer(Modifier.width(10.dp))
                Text(
                    title,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Text(
                    if (expanded) "⌃" else "⌄",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (expanded) {
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = .10f))
                Column(
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 14.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
private fun CompactFilterOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .11f) else Color.Transparent,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .48f)
            else MaterialTheme.colorScheme.outline.copy(alpha = .22f)
        )
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun showFilterDatePicker(
    context: Context,
    currentValue: String?,
    onSelected: (String) -> Unit
) {
    val initial = runCatching { LocalDate.parse(currentValue) }.getOrNull() ?: LocalDate.now()
    DatePickerDialog(
        context,
        { _, year, month, day -> onSelected(LocalDate.of(year, month + 1, day).toString()) },
        initial.year,
        initial.monthValue - 1,
        initial.dayOfMonth
    ).show()
}

private fun filterDateLabel(value: String?): String =
    value?.takeIf { it.isNotBlank() }
        ?.let { AgendaDateFormat.date(it) }
        ?: "Tria data"

@Composable
private fun DateFilterButton(
    label: String,
    value: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .24f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.CalendarToday,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(filterDateLabel(value), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(state: AgendaState, onDismiss: () -> Unit) {
    var draft by remember(state.filters) { mutableStateOf(state.filters) }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var presetNameDraft by remember { mutableStateOf("") }
    var placeFilterQuery by remember { mutableStateOf("") }
    // Cada apartat s'obri o es tanca de manera independent: no són un acordió exclusiu.
    var expandedSections by remember { mutableStateOf(setOf<String>()) }
    val filterContext = LocalContext.current

    val todayFilter = state.agendaToday()
    val weekendSaturday = todayFilter.plusDays(((6 - todayFilter.dayOfWeek.value + 7) % 7).toLong())
    val currentMonth = YearMonth.from(todayFilter)
    val locale = Locale("ca", "ES")

    val monthChoices = remember(todayFilter.year, todayFilter.monthValue) {
        (0L..11L).map { offset ->
            val value = YearMonth.from(todayFilter).plusMonths(offset)
            val monthName = value.month.getDisplayName(TextStyle.FULL, locale)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
            ("$monthName ${value.year}") to value.toString()
        }
    }

    val allComarques = remember(state.municipalities) {
        state.municipalities
            .map { it.comarca.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedBy { it.lowercase() }
    }
    val allTowns = remember(state.municipalities) {
        state.municipalities
            .map { it.name.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedBy { it.lowercase() }
    }


    val expandedSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = expandedSheetState,
        containerColor = Color.White
    ) {
        // v325: el full de filtres s'obri desplegat, amb els botons sempre visibles.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(.9f)
                .navigationBarsPadding()
                .imePadding()
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 12.dp)
            ) {
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Filtres",
                                modifier = Modifier.weight(1f),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                            TextButton(onClick = {
                                // v293: «Netejar» té efecte immediat, no deixa una vista
                                // «Sense filtre» amagada davall del full de filtres.
                                draft = Filters()
                                placeFilterQuery = ""
                                state.clearFilters()
                                onDismiss()
                            }) { Text("Netejar") }
                        }

                        Spacer(Modifier.height(8.dp))

                        FilterDepartmentCard(
                            title = "Quan",
                            expanded = "when" in expandedSections,
                            onToggle = {
                                expandedSections = if ("when" in expandedSections) expandedSections - "when"
                                                   else expandedSections + "when"
                            },
                            icon = { Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = AgendaBrandOrange) }
                        ) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                                item {
                                    CompactFilterOption(
                                        text = "Hui",
                                        selected = draft.dateFrom == todayFilter.toString() && draft.dateTo == todayFilter.toString(),
                                        onClick = { val d = todayFilter.toString(); draft = draft.copy(dateFrom = d, dateTo = d, month = null) }
                                    )
                                }
                                item {
                                    CompactFilterOption(
                                        text = "Demà",
                                        selected = draft.dateFrom == todayFilter.plusDays(1).toString() && draft.dateTo == todayFilter.plusDays(1).toString(),
                                        onClick = { val d = todayFilter.plusDays(1).toString(); draft = draft.copy(dateFrom = d, dateTo = d, month = null) }
                                    )
                                }
                                item {
                                    CompactFilterOption(
                                        text = "Cap de setmana",
                                        selected = draft.dateFrom == weekendSaturday.toString() && draft.dateTo == weekendSaturday.plusDays(1).toString(),
                                        onClick = { draft = draft.copy(dateFrom = weekendSaturday.toString(), dateTo = weekendSaturday.plusDays(1).toString(), month = null) }
                                    )
                                }
                                item {
                                    CompactFilterOption(
                                        text = "7 dies",
                                        selected = draft.dateFrom == todayFilter.toString() && draft.dateTo == todayFilter.plusDays(6).toString(),
                                        onClick = { draft = draft.copy(dateFrom = todayFilter.toString(), dateTo = todayFilter.plusDays(6).toString(), month = null) }
                                    )
                                }
                                item {
                                    CompactFilterOption(
                                        text = "Este mes",
                                        selected = draft.dateFrom == currentMonth.atDay(1).toString() && draft.dateTo == currentMonth.atEndOfMonth().toString(),
                                        onClick = { draft = draft.copy(dateFrom = currentMonth.atDay(1).toString(), dateTo = currentMonth.atEndOfMonth().toString(), month = null) }
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                DateFilterButton(
                                    label = "Des de",
                                    value = draft.dateFrom,
                                    onClick = {
                                        showFilterDatePicker(filterContext, draft.dateFrom) { value ->
                                            val picked = LocalDate.parse(value)
                                            val currentTo = runCatching { LocalDate.parse(draft.dateTo) }.getOrNull()
                                            draft = draft.copy(
                                                dateFrom = value,
                                                dateTo = if (currentTo != null && currentTo.isBefore(picked)) value else draft.dateTo,
                                                month = null
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                DateFilterButton(
                                    label = "Fins a",
                                    value = draft.dateTo,
                                    onClick = {
                                        showFilterDatePicker(filterContext, draft.dateTo ?: draft.dateFrom) { value ->
                                            val picked = LocalDate.parse(value)
                                            val currentFrom = runCatching { LocalDate.parse(draft.dateFrom) }.getOrNull()
                                            draft = draft.copy(
                                                dateFrom = if (currentFrom != null && currentFrom.isAfter(picked)) value else draft.dateFrom,
                                                dateTo = value,
                                                month = null
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Text("Mes", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.padding(top = 4.dp)) {
                                item {
                                    CompactFilterOption(
                                        text = "Tots",
                                        selected = draft.month == null && draft.dateFrom == null && draft.dateTo == null,
                                        onClick = { draft = draft.copy(month = null, dateFrom = null, dateTo = null) }
                                    )
                                }
                                items(monthChoices) { pair ->
                                    CompactFilterOption(
                                        text = pair.first,
                                        selected = draft.month == pair.second,
                                        onClick = { draft = draft.copy(month = pair.second, dateFrom = null, dateTo = null) }
                                    )
                                }
                            }
                        }

                        FilterDepartmentCard(
                            title = "On?",
                            expanded = "where" in expandedSections,
                            onToggle = {
                                expandedSections = if ("where" in expandedSections) expandedSections - "where"
                                                   else expandedSections + "where"
                            },
                            icon = { Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = AgendaBrandOrange) }
                        ) {
                            Text(
                                "Busca per poble o comarca",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            // Les ubicacions ja triades es poden llevar individualment.
                            if (!draft.town.isNullOrBlank() || !draft.comarca.isNullOrBlank()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(bottom = 7.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    draft.town?.takeIf(String::isNotBlank)?.let { town ->
                                        CompactFilterOption(
                                            text = "Poble: $town ×",
                                            selected = true,
                                            onClick = { draft = draft.copy(town = null) }
                                        )
                                    }
                                    draft.comarca?.takeIf(String::isNotBlank)?.let { comarca ->
                                        CompactFilterOption(
                                            text = "Comarca: $comarca ×",
                                            selected = true,
                                            onClick = { draft = draft.copy(comarca = null) }
                                        )
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = placeFilterQuery,
                                onValueChange = { placeFilterQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                                placeholder = { Text("Escriu un poble o comarca") }
                            )
                            // Match sense accents ni majúscules, sobre el catàleg municipal complet.
                            val townSuggestions = if (placeFilterQuery.isBlank()) emptyList() else allTowns
                                .asSequence()
                                .filter { state.searchContains(it, placeFilterQuery.trim()) }
                                .take(8)
                                .toList()
                            val comarcaSuggestions = if (placeFilterQuery.isBlank()) emptyList() else allComarques
                                .asSequence()
                                .filter { state.searchContains(it, placeFilterQuery.trim()) }
                                .take(8)
                                .toList()
                            if (townSuggestions.isNotEmpty()) {
                                Text("Pobles", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 9.dp, bottom = 3.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    items(townSuggestions) { town ->
                                        CompactFilterOption(
                                            text = town,
                                            selected = draft.town == town,
                                            onClick = {
                                                // Un poble concret substituïx una comarca prèvia incompatible.
                                                draft = draft.copy(town = town, comarca = null)
                                                placeFilterQuery = ""
                                            }
                                        )
                                    }
                                }
                            }
                            if (comarcaSuggestions.isNotEmpty()) {
                                Text("Comarques", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 9.dp, bottom = 3.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    items(comarcaSuggestions) { comarca ->
                                        CompactFilterOption(
                                            text = comarca,
                                            selected = draft.comarca == comarca,
                                            onClick = {
                                                draft = draft.copy(comarca = comarca, town = null)
                                                placeFilterQuery = ""
                                            }
                                        )
                                    }
                                }
                            }
                            if (placeFilterQuery.isNotBlank() && townSuggestions.isEmpty() && comarcaSuggestions.isEmpty()) {
                                Text("No hem trobat cap poble ni comarca amb este nom.",
                                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 6.dp))
                            }
                            // Es conserva també la selecció de totes les comarques sense escriure.
                            ChoiceRow(
                                title = "Comarca (totes)",
                                options = listOf("Totes") + allComarques,
                                selected = draft.comarca ?: "Totes",
                                onSelect = { value ->
                                    val comarca = value.takeUnless { it == "Totes" }
                                    draft = draft.copy(comarca = comarca, town = if (comarca != null) null else draft.town)
                                    placeFilterQuery = ""
                                }
                            )
                            ChoiceRow(
                                title = "Distància",
                                options = listOf("Qualsevol", "≤ 10 km", "≤ 25 km", "≤ 50 km", "≤ 100 km"),
                                selected = when (draft.maxDistanceKm) {
                                    10 -> "≤ 10 km"
                                    25 -> "≤ 25 km"
                                    50 -> "≤ 50 km"
                                    100 -> "≤ 100 km"
                                    else -> "Qualsevol"
                                },
                                onSelect = { value ->
                                    draft = draft.copy(
                                        maxDistanceKm = when (value) {
                                            "≤ 10 km" -> 10
                                            "≤ 25 km" -> 25
                                            "≤ 50 km" -> 50
                                            "≤ 100 km" -> 100
                                            else -> null
                                        }
                                    )
                                }
                            )
                        }

                        FilterDepartmentCard(
                            title = "Què",
                            expanded = "what" in expandedSections,
                            onToggle = {
                                expandedSections = if ("what" in expandedSections) expandedSections - "what"
                                                   else expandedSections + "what"
                            },
                            icon = { Icon(Icons.Outlined.Tune, contentDescription = null, tint = AgendaBrandOrange) }
                        ) {
                            MultiChoiceRow(
                                title = "Disciplines",
                                options = CulturalCategory.values().toList(),
                                selected = draft.categories,
                                label = { it.label },
                                onToggle = { category ->
                                    draft = draft.copy(
                                        categories = if (category in draft.categories) draft.categories - category else draft.categories + category
                                    )
                                }
                            )
                        }

                        FilterDepartmentCard(
                            title = "Preu",
                            expanded = "price" in expandedSections,
                            onToggle = {
                                expandedSections = if ("price" in expandedSections) expandedSections - "price"
                                                   else expandedSections + "price"
                            },
                            icon = { Icon(Icons.Outlined.ConfirmationNumber, contentDescription = null, tint = AgendaBrandOrange) }
                        ) {
                            ChoiceRow(
                                title = "Entrada",
                                options = listOf("Qualsevol", "Gratuït", "≤ 10 €", "≤ 15 €", "≤ 20 €"),
                                selected = when {
                                    draft.freeOnly -> "Gratuït"
                                    draft.maxPrice == 10 -> "≤ 10 €"
                                    draft.maxPrice == 15 -> "≤ 15 €"
                                    draft.maxPrice == 20 -> "≤ 20 €"
                                    else -> "Qualsevol"
                                },
                                onSelect = {
                                    draft = when (it) {
                                        "Gratuït" -> draft.copy(freeOnly = true, maxPrice = null)
                                        "≤ 10 €" -> draft.copy(freeOnly = false, maxPrice = 10)
                                        "≤ 15 €" -> draft.copy(freeOnly = false, maxPrice = 15)
                                        "≤ 20 €" -> draft.copy(freeOnly = false, maxPrice = 20)
                                        else -> draft.copy(freeOnly = false, maxPrice = null)
                                    }
                                }
                            )
                        }

                    }
                }
            }
            // v325: peu fix, fora del contingut desplaçable. Les dos accions
            // es veuen sempre en una mateixa línia, inclús si s'expandixen filtres.
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    onClick = {
                        if (draft.isEmpty()) state.selectFeedMode(FeedMode.FOR_YOU)
                        else state.updateFilters(draft)
                        onDismiss()
                    }
                ) {
                    Text("Aplicar", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { presetNameDraft = ""; showSavePresetDialog = true },
                    enabled = !draft.isEmpty(),
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Outlined.BookmarkBorder, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Guardar", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
    if (showSavePresetDialog) {
        AlertDialog(
            onDismissRequest = { if (!state.isFilterPresetSaving) showSavePresetDialog = false },
            title = { Text(if (state.isFilterPresetSaving) "Guardant al servidor…" else "Guardar filtres") },
            text = {
                Column {
                    OutlinedTextField(
                        value = presetNameDraft,
                        onValueChange = { presetNameDraft = it.take(52) },
                        label = { Text("Nom de la llista") },
                        placeholder = { Text("Ex.: Concerts prop") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (state.currentAccount == null) {
                        Text("Inicia sessió per guardar els filtres en la base de dades.",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    }
                    state.savedFilterSyncError?.let { error ->
                        Text(error, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    }
                    if (state.savedFilterPresets.any { it.name.equals(presetNameDraft.trim(), ignoreCase = true) }) {
                        Text("Ja existeix: es substituirà la configuració guardada.",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = presetNameDraft.isNotBlank() && !draft.isEmpty() && !state.isFilterPresetSaving,
                    onClick = {
                        val savedName = presetNameDraft.trim()
                        state.saveFilterPreset(savedName, draft) { saved ->
                            if (saved) {
                                showSavePresetDialog = false
                                state.applyFilterPreset(savedName)
                                onDismiss()
                            }
                        }
                    }
                ) { Text("Guardar") }
            },
            dismissButton = { TextButton(enabled = !state.isFilterPresetSaving,
                onClick = { showSavePresetDialog = false }) { Text("Cancel·lar") } }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlertsSheet(
    state: AgendaState,
    onOpenEvent: (Int) -> Unit,
    onOpenTickets: (Int?) -> Unit,
    onOpenCommunityEvent: (Int, Int) -> Unit,
    onOpenArtist: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var configuring by remember { mutableStateOf(false) }
    var showActiveReminders by remember { mutableStateOf(false) }
    var reminderPanelRevision by remember { mutableStateOf(0) }
    var selectedTab by remember { mutableStateOf(0) }
    var selectedFriendsGroupId by remember { mutableStateOf<Int?>(null) }
    var draft by remember(state.alertPreferences) { mutableStateOf(state.alertPreferences) }
    var emailDraft by remember(state.emailPreferences) { mutableStateOf(state.emailPreferences) }
    var emailSaveMessage by remember { mutableStateOf<String?>(null) }
    val androidContext = LocalContext.current
    // Es consulta el registre local quan s'obri el panell o es canvia un avís;
    // no cal fer una petició a la xarxa per visualitzar els recordatoris.
    val scheduledReminders = remember(showActiveReminders, reminderPanelRevision, state.notifications()) {
        if (showActiveReminders) EventReminderScheduler.activeReminders(androidContext) else emptyList()
    }
    var androidDraft by remember { mutableStateOf(EventReminderScheduler.settings(androidContext)) }
    val androidPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        androidDraft = androidDraft.copy(enabled = granted)
    }
    val notifications = state.notifications().filterNot { it.id in state.dismissedNotificationIds }
    val unread = state.unreadNotificationCount()
    val upcoming = state.notificationEvents(NotificationType.REMINDER)
    val newFollowed = state.notificationEvents(NotificationType.NEW_CONCERT)
    val ticketEvents = state.notificationEvents(NotificationType.TICKETS)
    val suggestions = state.notificationSuggestions()
    // Inclou també reports sense esdeveniment i suggerències noves.
    val generalNotifications = NotificationGeneralRules.unseen(
        notifications = notifications,
        suggestions = suggestions,
        readIds = state.readNotificationIds,
        dismissedIds = state.dismissedNotificationIds
    )
    val canClearNotifications = notifications.isNotEmpty() ||
        suggestions.any { state.suggestionNotificationId(it.id) !in state.readNotificationIds }
    val unreadByTab = NotificationTabBadgeRules.count(
        notifications = notifications,
        suggestions = suggestions,
        readIds = state.readNotificationIds,
        dismissedIds = state.dismissedNotificationIds,
        upcomingEventIds = upcoming.mapTo(HashSet()) { it.id },
        newEventIds = newFollowed.mapTo(HashSet()) { it.id },
        ticketEventIds = ticketEvents.mapTo(HashSet()) { it.id }
    )

    LaunchedEffect(Unit) { state.refreshNotificationPanel() }
    LaunchedEffect(configuring) {
        if (configuring) {
            state.refreshEmailPreferences { ok, message ->
                if (!ok) emailSaveMessage = message
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 6.dp)
                .statusBarsPadding()
                .navigationBarsPadding(),
            shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 8.dp, top = 12.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            when {
                                configuring -> "Configuració"
                                showActiveReminders -> "Recordatoris actius"
                                else -> "Notificacions"
                            },
                            fontSize = 23.sp, fontWeight = FontWeight.ExtraBold
                        )
                        if (!configuring && !showActiveReminders && unread > 0) {
                            Text("$unread sense llegir", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (!configuring && !showActiveReminders) {
                        // Panell al costat de Configuració: no modifica la pestanya seleccionada.
                        IconButton(onClick = { showActiveReminders = true; reminderPanelRevision++ }) {
                            Icon(Icons.Outlined.AlarmOn, contentDescription = "Veure recordatoris actius")
                        }
                        IconButton(onClick = { configuring = true }) {
                            Icon(Icons.Outlined.Tune, contentDescription = "Configuració de notificacions")
                        }
                    }
                    IconButton(onClick = {
                        when {
                            configuring -> configuring = false
                            showActiveReminders -> showActiveReminders = false
                            else -> onDismiss()
                        }
                    }) {
                        Icon(Icons.Outlined.Close, contentDescription = if (configuring || showActiveReminders) "Tornar a notificacions" else "Tancar")
                    }
                }

                if (showActiveReminders && !configuring) {
                    // Llista operativa: recordatoris locals (dia/hora) i campanetes actives.
                    val scheduledIds = scheduledReminders.mapTo(HashSet()) { it.eventId }
                    val otherAlerts = state.alertConcerts.filterNot { it in scheduledIds }
                        .sortedBy { state.concertById(it)?.date.orEmpty() }
                    val artistsWithBell = state.artistAlerts.sortedBy { it.lowercase(Locale.ROOT) }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        item {
                            Text("Des d'ací pots consultar, obrir o desactivar els avisos que tens configurats.",
                                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (scheduledReminders.isEmpty() && otherAlerts.isEmpty() && artistsWithBell.isEmpty()) {
                            item {
                                Text("No tens recordatoris actius.",
                                    modifier = Modifier.padding(top = 26.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (scheduledReminders.isNotEmpty() || otherAlerts.isNotEmpty()) {
                            item { Text("Esdeveniments", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp,
                                modifier = Modifier.padding(top = 9.dp)) }
                        }
                        items(scheduledReminders, key = { "scheduled_${it.eventId}" }) { reminder ->
                            val event = state.concertById(reminder.eventId)
                            ActiveReminderRow(
                                posterUri = event?.resolvedCardImageUri ?: event?.imageCandidates()?.firstOrNull(),
                                title = event?.title?.takeIf { it.isNotBlank() } ?: reminder.title.ifBlank { "Esdeveniment #${reminder.eventId}" },
                                detail = java.time.Instant.ofEpochMilli(reminder.atEpochMillis)
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm")) +
                                    (if (reminder.customTime) " · Personalitzat" else " · Recordatori") +
                                    reminder.town.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty(),
                                onOpen = { onOpenEvent(reminder.eventId) },
                                onDisable = {
                                    EventReminderScheduler.cancel(androidContext, reminder.eventId)
                                    if (reminder.eventId in state.alertConcerts) state.toggleConcertAlert(reminder.eventId)
                                    state.reminderConfigurationChanged()
                                    reminderPanelRevision++
                                    Toast.makeText(androidContext, "Recordatori desactivat.", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        items(otherAlerts, key = { "event_alert_$it" }) { eventId ->
                            val event = state.concertById(eventId)
                            ActiveReminderRow(
                                posterUri = event?.resolvedCardImageUri ?: event?.imageCandidates()?.firstOrNull(),
                                title = event?.title ?: "Esdeveniment #$eventId",
                                detail = "Campaneta de l’app · Sense hora personalitzada",
                                onOpen = { onOpenEvent(eventId) },
                                onDisable = {
                                    state.toggleConcertAlert(eventId)
                                    reminderPanelRevision++
                                    Toast.makeText(androidContext, "Avís desactivat.", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        if (artistsWithBell.isNotEmpty()) {
                            item { Text("Artistes i grups", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp,
                                modifier = Modifier.padding(top = 12.dp)) }
                        }
                        items(artistsWithBell, key = { "artist_bell_$it" }) { artist ->
                            ActiveReminderRow(
                                posterUri = state.publicPhotoForArtist(artist),
                                title = artist,
                                detail = "Campaneta activa · ${state.artistAlertFrequency(artist).label}",
                                onOpen = { onOpenArtist(artist) },
                                onDisable = {
                                    state.configureArtistAlert(artist, false)
                                    reminderPanelRevision++
                                    Toast.makeText(androidContext, "Avisos de $artist desactivats.", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        item { Spacer(Modifier.height(20.dp)) }
                    }
                } else if (configuring) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 24.dp)
                    ) {
                        item {
                            LanguageSettingsCard(modifier = Modifier.padding(top = 8.dp))
                            Spacer(Modifier.height(18.dp))
                            Text("Campaneta de l’app", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            AlertToggle("Novetats dels artistes amb campaneta activada", draft.followedArtist) {
                                draft = draft.copy(followedArtist = it)
                            }
                            if (draft.followedArtist) {
                                Text(
                                    "Configura la freqüència individualment en la campaneta de cada artista o grup.",
                                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 5.dp, bottom = 4.dp)
                                )
                            }
                            AlertToggle("Esdeveniments guardats pròxims", draft.dayBefore) {
                                draft = draft.copy(dayBefore = it)
                            }
                            AlertToggle("Compres d’entrades", draft.ticketUpdates) {
                                draft = draft.copy(ticketUpdates = it)
                            }
                            AlertToggle("Comunitat: missatges i propostes", draft.communityMessages) {
                                draft = draft.copy(communityMessages = it)
                            }
                            AlertToggle("Suggerències culturals", draft.suggestions) {
                                draft = draft.copy(suggestions = it)
                            }
                            Text(
                                "La freqüència dels artistes seguits és independent dels recordatoris personals, " +
                                    "que es mostren en la data que hages triat.",
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 5.dp)
                            )

                            Spacer(Modifier.height(18.dp))
                            Text("Correus electrònics", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                            Text(
                                if (state.currentAccount == null) "Per activar els correus necessites iniciar sessió. La resta de preferències es poden guardar sense compte."
                                else "Pots desactivar els correus o rebre un resum diari o setmanal segons els temes seleccionats.",
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 5.dp)
                            )
                            if (state.currentAccount != null) {
                                Text("Periodicitat", modifier = Modifier.padding(top = 14.dp, bottom = 4.dp), fontWeight = FontWeight.SemiBold)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    EmailDigestFrequency.entries.forEach { frequency ->
                                        FilterChip(
                                            selected = emailDraft.frequency == frequency,
                                            onClick = { emailDraft = emailDraft.copy(frequency = frequency) },
                                            label = { Text(frequency.label) }
                                        )
                                    }
                                }
                                Text("Què vols rebre", modifier = Modifier.padding(top = 12.dp), fontWeight = FontWeight.SemiBold)
                                AlertToggle("Suggeriments segons els teus gustos i afinitats", emailDraft.recommendations) {
                                    emailDraft = emailDraft.copy(recommendations = it)
                                }
                                AlertToggle("Esdeveniments dels artistes que seguixes", emailDraft.followedArtists) {
                                    emailDraft = emailDraft.copy(followedArtists = it)
                                }
                                AlertToggle("Guardats i recordatoris d’esdeveniments", emailDraft.savedReminders) {
                                    emailDraft = emailDraft.copy(savedReminders = it)
                                }
                                AlertToggle("Publicitat personalitzada", emailDraft.personalizedAds) {
                                    emailDraft = emailDraft.copy(personalizedAds = it)
                                }
                            }
                            Spacer(Modifier.height(18.dp))
                            Text("Notificacions Android", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                            Text(
                                "Només dels esdeveniments i artistes/grups en què actives expressament la campaneta. " +
                                    "Els recordatoris d’esdeveniments són locals; els avisos de noves activitats dels grups necessiten el servei push del servidor.",
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                            AlertToggle("Avisos dels esdeveniments amb campaneta activa", androidDraft.enabled) { enable ->
                                if (enable && Build.VERSION.SDK_INT >= 33 &&
                                    ContextCompat.checkSelfPermission(androidContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    androidPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else androidDraft = androidDraft.copy(enabled = enable)
                            }
                            Spacer(Modifier.height(8.dp))
                            AlertToggle("Respectar les hores de descans", androidDraft.quietEnabled) { enabled ->
                                androidDraft = androidDraft.copy(quietEnabled = enabled)
                            }
                            if (androidDraft.quietEnabled) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedButton(modifier = Modifier.weight(1f), onClick = {
                                        val current = androidDraft.quietStartMinutes
                                        TimePickerDialog(androidContext, { _, hour, minute ->
                                            androidDraft = androidDraft.copy(quietStartMinutes = hour * 60 + minute)
                                        }, current / 60, current % 60, true).show()
                                    }) { Text("De %02d:%02d".format(androidDraft.quietStartMinutes / 60, androidDraft.quietStartMinutes % 60)) }
                                    OutlinedButton(modifier = Modifier.weight(1f), onClick = {
                                        val current = androidDraft.quietEndMinutes
                                        TimePickerDialog(androidContext, { _, hour, minute ->
                                            androidDraft = androidDraft.copy(quietEndMinutes = hour * 60 + minute)
                                        }, current / 60, current % 60, true).show()
                                    }) { Text("A %02d:%02d".format(androidDraft.quietEndMinutes / 60, androidDraft.quietEndMinutes % 60)) }
                                }
                                Text(
                                    "Els recordatoris predeterminats que coincidisquen amb el descans es traslladen al final d’este període. " +
                                        "La data i hora personalitzades tenen prioritat i no es mouen.",
                                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No s'enviaran notificacions Android de suggeriments, Comunitat ni novetats " +
                                    "d'artistes només seguits. Els avisos dels grups amb campaneta activa s'enviaran " +
                                    "quan estiga connectat el servei push; ara funcionen els recordatoris locals.",
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            emailSaveMessage?.takeIf { it.isNotBlank() }?.let { message ->
                                Text(
                                    message,
                                    modifier = Modifier.padding(top = 10.dp),
                                    fontSize = 12.sp,
                                    color = if (message.contains("guardades", ignoreCase = true)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                            Button(
                                modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                                enabled = !state.emailPreferencesSyncing,
                                onClick = {
                                    state.updateAlertPreferences(draft)
                                    EventReminderScheduler.setSettings(androidContext, androidDraft)
                                    emailSaveMessage = null
                                    if (state.currentAccount == null) {
                                        emailSaveMessage = "Preferències locals guardades. Inicia sessió per configurar els correus."
                                        configuring = false
                                    } else if (emailDraft == state.emailPreferences) {
                                        emailSaveMessage = "Preferències guardades."
                                        configuring = false
                                    } else {
                                        state.updateEmailPreferences(emailDraft) { ok, message ->
                                            emailSaveMessage = message
                                            if (ok) configuring = false
                                        }
                                    }
                                }
                            ) { Text(if (state.emailPreferencesSyncing) "Guardant…" else "Guardar configuració") }
                        }
                    }
                } else {
                    // Sis icones proporcionals: totes visibles sense scroll horitzontal.
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)) {
                        NotificationPanelTab("General", Icons.Outlined.Notifications, selectedTab == 0, unreadByTab.general, Modifier.weight(1f)) { selectedTab = 0 }
                        NotificationPanelTab("Pròxim", Icons.Outlined.AccessTime, selectedTab == 1, unreadByTab.upcoming, Modifier.weight(1f)) { selectedTab = 1 }
                        NotificationPanelTab("Nous", Icons.Outlined.NewReleases, selectedTab == 2, unreadByTab.newEvents, Modifier.weight(1f)) { selectedTab = 2 }
                        NotificationPanelTab("Entrades", Icons.Outlined.ConfirmationNumber, selectedTab == 3, unreadByTab.tickets, Modifier.weight(1f)) { selectedTab = 3 }
                        NotificationPanelTab("Suggerències", Icons.Outlined.AutoAwesome, selectedTab == 4, unreadByTab.suggestions, Modifier.weight(1f)) { selectedTab = 4 }
                        NotificationPanelTab("Comunitat", Icons.Outlined.Groups, selectedTab == 5, unreadByTab.community, Modifier.weight(1f)) { selectedTab = 5 }
                    }
                    Text(
                        listOf("General", "Pròxim", "Nous", "Entrades", "Suggerències", "Comunitat")[selectedTab],
                        modifier = Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 2.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = .16f))

                    if (selectedTab == 0) {
                        if (generalNotifications.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                Text("No tens notificacions noves.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 22.dp)
                            ) {
                                items(generalNotifications, key = { "general_${it.id}" }) { notification ->
                                    Column {
                                        // General és una única bústia cronològica: cap etiqueta de secció.
                                        // Mateixa targeta d'esdeveniment que Pròxim, Nous,
                                        // Entrades i Suggerències: imatge, poble, data i hora.
                                        // Els avisos sense concert (p. ex. reports) conserven
                                        // la seua fila de text perquè no perden informació.
                                        val concert = notification.concertId?.let(state::concertById)
                                        if (concert != null) {
                                            NotificationEventCard(
                                                state = state,
                                                concert = concert,
                                                unread = true,
                                                onOpenArtist = onOpenArtist,
                                                onClick = {
                                                    state.markNotificationRead(notification.id)
                                                    if (notification.communityGroupId != null) {
                                                        onOpenCommunityEvent(concert.id, notification.communityGroupId)
                                                    } else if (notification.type == NotificationType.TICKETS) {
                                                        onOpenTickets(concert.id)
                                                    } else {
                                                        onOpenEvent(concert.id)
                                                    }
                                                }
                                            )
                                        } else {
                                            NotificationRow(
                                                notification = notification,
                                                unread = true,
                                                onClick = {
                                                    state.markNotificationRead(notification.id)
                                                    when {
                                                        notification.communityGroupId != null && notification.concertId != null ->
                                                            onOpenCommunityEvent(notification.concertId, notification.communityGroupId)
                                                        notification.type == NotificationType.COMMUNITY && notification.friendsGroupId != null ->
                                                            selectedFriendsGroupId = notification.friendsGroupId
                                                        else -> notification.concertId?.let { eventId ->
                                                            if (notification.type == NotificationType.TICKETS) onOpenTickets(eventId)
                                                            else onOpenEvent(eventId)
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else if (selectedTab == 5) {
                        val communityNotices = notifications.filter { it.type == NotificationType.COMMUNITY }
                        if (communityNotices.isEmpty()) {
                            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                Text("No hi ha novetats de Comunitat.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(top = 8.dp, bottom = 22.dp)) {
                                items(communityNotices, key = { "community_${it.id}" }) { notice ->
                                    val event = notice.concertId?.let(state::concertById)
                                    val openNotice: () -> Unit = {
                                        state.markNotificationRead(notice.id)
                                        if (event != null && notice.communityGroupId != null) {
                                            onOpenCommunityEvent(event.id, notice.communityGroupId)
                                        } else if (event != null) onOpenEvent(event.id)
                                        else selectedFriendsGroupId = notice.friendsGroupId
                                    }
                                    if (event != null) {
                                        NotificationEventCard(
                                            state = state, concert = event,
                                            unread = notice.id !in state.readNotificationIds,
                                            onOpenArtist = onOpenArtist, onClick = openNotice
                                        )
                                    } else {
                                        NotificationRow(notice, notice.id !in state.readNotificationIds, openNotice)
                                    }
                                }
                            }
                        }
                    } else {
                        val currentEvents = when (selectedTab) {
                            1 -> upcoming
                            2 -> newFollowed
                            3 -> ticketEvents
                            4 -> suggestions
                            else -> emptyList()
                        }

                        if (currentEvents.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                Text(
                                    when (selectedTab) {
                                        1 -> "No tens esdeveniments guardats pròxims."
                                        2 -> "No hi ha concerts nous dels artistes que seguixes."
                                        3 -> "No tens entrades noves esta setmana."
                                        else -> "No hi ha cap suggerència destacada ara."
                                    },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 22.dp)
                            ) {
                                items(currentEvents, key = { "notification_${selectedTab}_${it.id}" }) { concert ->
                                    val types = when (selectedTab) {
                                        1 -> listOf(NotificationType.REMINDER)
                                        2 -> listOf(NotificationType.NEW_CONCERT)
                                        3 -> listOf(NotificationType.TICKETS)
                                        else -> emptyList()
                                    }
                                    val unreadEvent = notifications.any {
                                        it.type in types && it.concertId == concert.id && it.id !in state.readNotificationIds
                                    } || (selectedTab == 4 && state.suggestionNotificationId(concert.id) !in state.readNotificationIds)
                                    Column {
                                        NotificationEventCard(
                                            state = state,
                                            concert = concert,
                                            unread = unreadEvent,
                                            onOpenArtist = onOpenArtist,
                                            onClick = {
                                                types.forEach { state.markNotificationForEvent(it, concert.id) }
                                                if (selectedTab == 4) state.markNotificationRead(state.suggestionNotificationId(concert.id))
                                                if (selectedTab == 3) onOpenTickets(concert.id) else onOpenEvent(concert.id)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { state.dismissAllNotifications() },
                            enabled = canClearNotifications,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = if (canClearNotifications) .72f else .24f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .38f)
                            ),
                            shape = RoundedCornerShape(9.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text("Netejar notificacions", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                }
            }
        }
    }
    // Diàleg sobre la campaneta, igual que el popup de concerts: tancar-lo
    // torna a la pestanya de notificacions, sense reiniciar llista ni selecció.
    selectedFriendsGroupId?.let { groupId ->
        state.friendsCircles.firstOrNull { it.id == groupId }?.let { group ->
            FriendsCircleDialog(
                state = state, group = group,
                onDismiss = { selectedFriendsGroupId = null },
                onOpenConcert = { event ->
                    selectedFriendsGroupId = null
                    onOpenEvent(event.id)
                }
            )
        }
    }
}

@Composable
private fun NotificationPanelTab(
    label: String,
    symbol: ImageVector,
    selected: Boolean,
    unreadCount: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier.clickable(onClick = onClick).padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
            Icon(
                symbol,
                contentDescription = if (unreadCount > 0) "$label · $unreadCount sense llegir" else label,
                modifier = Modifier.size(22.dp),
                tint = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (unreadCount > 0) {
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).size(16.dp)
                        .clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (unreadCount > 9) "9+" else "$unreadCount", fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(3.dp).background(
            if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
        ))
    }
}

@Composable
private fun ActiveReminderRow(
    title: String,
    detail: String,
    posterUri: String? = null,
    onOpen: () -> Unit,
    onDisable: () -> Unit
) {
    val poster = rememberImageBitmapFromUri(posterUri, maxDimension = 140)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .18f)),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(width = 54.dp, height = 64.dp)
                .clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onOpen), contentAlignment = Alignment.Center) {
                if (poster != null) Image(bitmap = poster, contentDescription = "Cartell de $title",
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Icon(Icons.Outlined.Event, contentDescription = null, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f).clickable(onClick = onOpen).padding(vertical = 4.dp)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2)
                Text(detail, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Obrir", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            }
            TextButton(onClick = onDisable) { Text("Desactivar", fontSize = 11.sp) }
        }
    }
}

@Composable
private fun NotificationEventCard(
    state: AgendaState,
    concert: Concert,
    unread: Boolean,
    onOpenArtist: (String) -> Unit,
    onClick: () -> Unit
) {
    val artistPhotoUri = state.publicPhotoForArtist(concert.artist)
    val photo = rememberImageBitmapFromUris(
        state.cardImageCandidates(concert),
        maxDimension = 260,
        eventId = concert.id,
        knownResolvedUri = concert.resolvedCardImageUri
    )
    val accent = eventAccentColor(concert)
    val ticketText = when {
        concert.isFree -> "Gratuït"
        concert.ticketPrice > 0 -> "${concert.ticketPrice} €"
        !concert.ticketAvailable -> "Sense venda"
        else -> "Entrades"
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = if (unread) MaterialTheme.colorScheme.primary.copy(alpha = .055f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .10f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(78.dp)
                    .border(1.5.dp, accent.copy(alpha = .88f), RoundedCornerShape(10.dp))
                    .padding(2.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenArtist(concert.artist) }
            ) {
                if (photo != null) {
                    Image(photo, concert.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Box(Modifier.fillMaxSize().background(accent.copy(alpha = .13f)), contentAlignment = Alignment.Center) {
                        Text(concert.title.take(2).uppercase(), fontWeight = FontWeight.Bold, color = accent)
                    }
                }
            }
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        concert.title,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    if (unread) {
                        Spacer(Modifier.width(6.dp))
                        Box(Modifier.size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    "${concert.town} · ${concert.venue}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    "${AgendaDateFormat.date(concert.feedDate)} · ${concert.time} · $ticketText",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun NotificationCountChip(label: String, count: Int) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = .09f)
    ) {
        Text(
            "$label  $count",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun NotificationRow(
    notification: AgendaNotification,
    unread: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = if (unread) MaterialTheme.colorScheme.primary.copy(alpha = .07f) else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(notification.type.emoji, fontSize = 20.sp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(notification.title, modifier = Modifier.weight(1f), fontWeight = if (unread) FontWeight.Bold else FontWeight.SemiBold, fontSize = 14.sp)
                    if (unread) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(AgendaDateFormat.datesInText(notification.body), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserSheet(
    state: AgendaState,
    onLogout: () -> Unit,
    onDismiss: () -> Unit
) {
    val account = state.currentAccount ?: return
    val profile = state.profileFor(account.username)
    val context = LocalContext.current
    var profileEditorOpen by remember(account.username) { mutableStateOf(false) }
    var profileName by remember(account.username, profile.displayName) { mutableStateOf(profile.displayName) }
    var profileBio by remember(account.username, profile.bio) { mutableStateOf(profile.bio) }
    var profilePhoto by remember(account.username, profile.photoUri) { mutableStateOf(profile.photoUri) }
    var profileMessage by remember(account.username) { mutableStateOf<String?>(null) }
    var profileSaving by remember(account.username) { mutableStateOf(false) }
    var profileUploadProgress by remember(account.username) { mutableStateOf<Int?>(null) }
    val profilePhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            profilePhoto = runCatching { cachePickedImageForEditing(context, uri) }.getOrElse { uri.toString() }
            profileMessage = null
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        SheetContent {
            Text("Perfil", fontWeight = FontWeight.Black, fontSize = 18.sp)
            Surface(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .72f)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(64.dp)) {
                            ArtistAvatar(profileName.ifBlank { profile.displayName }, 64.dp, profilePhoto)
                            IconButton(
                                onClick = { profilePhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                modifier = Modifier.align(Alignment.TopEnd).offset(x = 5.dp, y = (-5).dp)
                                    .size(26.dp).background(MaterialTheme.colorScheme.primary, CircleShape)
                            ) { Icon(Icons.Outlined.Add, contentDescription = "Canviar foto",
                                tint = Color.White, modifier = Modifier.size(16.dp)) }
                        }
                        Spacer(Modifier.width(13.dp))
                        Column(Modifier.weight(1f)) {
                            Text(profileName.ifBlank { profile.displayName }, fontSize = 21.sp, fontWeight = FontWeight.Black)
                            Text("@${account.username} · ${account.roleLabel}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .58f))
                        }
                        IconButton(onClick = { profileEditorOpen = !profileEditorOpen }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Editar perfil")
                        }
                    }

                    if (profileEditorOpen) {
                        OutlinedButton(
                            onClick = { profilePhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                        ) { Text(if (profilePhoto.isNullOrBlank()) "Pujar imatge de perfil" else "Canviar imatge de perfil") }
                        OutlinedTextField(
                            value = profileName,
                            onValueChange = { profileName = it; profileMessage = null },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            label = { Text("Nom visible") },
                            singleLine = true
                        )
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!profilePhoto.isNullOrBlank()) {
                                OutlinedButton(onClick = { profilePhoto = null }, modifier = Modifier.weight(1f)) { Text("Llevar foto") }
                            }
                            Button(
                                enabled = !profileSaving,
                                onClick = {
                                    profileSaving = true
                                    profileUploadProgress = null
                                    val remove = profilePhoto == null && !profile.photoUri.isNullOrBlank()
                                    state.updateAccountProfileRemote(
                                        username = account.username,
                                        displayName = profileName,
                                        bio = profile.bio,
                                        photoUri = profilePhoto,
                                        removeImage = remove,
                                        onProgress = { profileUploadProgress = it },
                                        onResult = { ok, text, remoteUrl ->
                                            profileSaving = false
                                            profileMessage = text
                                            if (ok) {
                                                profilePhoto = remoteUrl
                                                profileUploadProgress = 100
                                                profileEditorOpen = false
                                            }
                                        }
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text(if (profileSaving) "Guardant…" else "Guardar") }
                        }
                        profileUploadProgress?.takeIf { profileSaving || it in 1..99 }?.let { pct ->
                            LinearProgressIndicator(progress = { pct / 100f }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                            Text("Pujant i sincronitzant imatge: $pct%", fontSize = 10.sp)
                        }
                        profileMessage?.let { Text(it, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp)) }
                    }
                }
            }

            Text("La meua agenda", fontWeight = FontWeight.Black, fontSize = 18.sp, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Stat(state.savedConcerts.size, "Guardats")
                Stat(state.followedArtists.size, "Seguint")
                Stat(state.blockedArtists.size, "Bloquejats")
            }

            Text("Guardats", fontWeight = FontWeight.Black, fontSize = 17.sp)
            Spacer(Modifier.height(8.dp))
            val saved = state.concerts.filter { it.id in state.savedConcerts }
            if (saved.isEmpty()) {
                MutedLine("Encara no has guardat cap concert.")
            } else {
                saved.forEach { concert ->
                    SmallConcertRow(concert) { state.openSavePicker(concert.id) }
                }
            }

            Spacer(Modifier.height(22.dp))
            Text("Seguiments", fontWeight = FontWeight.Black, fontSize = 17.sp)
            Spacer(Modifier.height(8.dp))
            if (state.followedArtists.isEmpty()) {
                MutedLine("Encara no segueixes cap grup.")
            } else {
                state.followedArtists.sorted().forEach { artist ->
                    ArtistManageRow(
                        artist = artist,
                        action = "Deixar de seguir",
                        onAction = { state.toggleFollowArtist(artist) }
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            Text("Bloquejos", fontWeight = FontWeight.Black, fontSize = 17.sp)
            Spacer(Modifier.height(8.dp))
            if (state.blockedArtists.isEmpty()) {
                MutedLine("No tens cap grup bloquejat.")
            } else {
                state.blockedArtists.sorted().forEach { artist ->
                    ArtistManageRow(
                        artist = artist,
                        action = "Desbloquejar",
                        onAction = { state.unblockArtist(artist) }
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            Text("Calendari personal", fontWeight = FontWeight.Black, fontSize = 17.sp)
            Spacer(Modifier.height(8.dp))
            val goingConcerts = state.goingConcerts()
            if (goingConcerts.isEmpty()) {
                MutedLine("No tens esdeveniments al calendari personal.")
            } else {
                goingConcerts.forEach { concert ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CalendarToday, null)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(concert.artist, fontWeight = FontWeight.Bold)
                                Text("${formatDate(concert.date)} · ${concert.time} · ${concert.town}", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp)
            ) {
                Text("Tancar sessió")
            }
        }
    }
}

@Composable
private fun SheetContent(content: @Composable ColumnScope.() -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(.9f)
            .navigationBarsPadding()
            .imePadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 40.dp)
    ) {
        item {
            Column(content = content)
        }
    }
}

@Composable
private fun SheetHeader(title: String, subtitle: String) {
    Text(title, fontSize = 24.sp, fontWeight = FontWeight.Black)
    Text(
        subtitle,
        modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f),
        fontSize = 13.sp
    )
}


@Composable
private fun <T> MultiChoiceRow(
    title: String,
    options: List<T>,
    selected: Set<T>,
    label: (T) -> String,
    onToggle: (T) -> Unit
) {
    Text(
        title,
        modifier = Modifier.padding(top = 10.dp, bottom = 7.dp),
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(options) { value ->
            CompactFilterOption(
                text = label(value),
                selected = value in selected,
                onClick = { onToggle(value) }
            )
        }
    }
}

@Composable
private fun ChoiceRow(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Text(
        title,
        modifier = Modifier.padding(top = 10.dp, bottom = 7.dp),
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(options) { value ->
            CompactFilterOption(
                text = value,
                selected = selected == value,
                onClick = { onSelect(value) }
            )
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .58f)
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun AlertToggle(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun Stat(value: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .58f))
    }
}

@Composable
private fun SmallConcertRow(concert: Concert, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArtistAvatar(concert.artist, 38.dp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(concert.artist, fontWeight = FontWeight.Bold)
            Text(
                "${concert.town} · ${formatDate(concert.date)}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .58f)
            )
        }
        TextButton(onClick = onRemove) { Text("Llevar") }
    }
}

@Composable
private fun ArtistManageRow(artist: String, action: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArtistAvatar(artist, 38.dp)
        Spacer(Modifier.width(10.dp))
        Text(artist, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
        TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable
private fun MutedLine(text: String) {
    Text(
        text,
        modifier = Modifier.padding(vertical = 8.dp),
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f)
    )
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 46.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("No hi ha resultats", fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text(
            "Prova a llevar algun filtre o canviar la cerca.",
            modifier = Modifier.padding(top = 6.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .56f)
        )
    }
}

@Composable
private fun StoryViewer(
    stories: List<Story>,
    concerts: List<Concert>,
    initialStoryKey: String,
    photoForArtist: (String) -> String?,
    isFollowed: (String) -> Boolean,
    onToggleFollow: (String) -> Unit,
    onSeenGroup: (List<Story>) -> Unit,
    onOpenEvent: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    if (stories.isEmpty()) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    // Un únic Batec visual per grup. El visor només rep els grups que ja estan
    // carregats al carrusel i no torna a reagrupar tot el catàleg.
    val stableGroups = remember(stories, initialStoryKey) { groupBatecsByArtist(stories) }
    if (stableGroups.isEmpty()) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    val initialGroupIndex = stableGroups.indexOfFirst { group ->
        group.stories.any { it.storyKey.ifBlank { "manual:${it.id}" } == initialStoryKey }
    }.let { if (it >= 0) it else 0 }

    var currentGroupIndex by remember(initialStoryKey) { mutableStateOf(initialGroupIndex) }
    // Permet reiniciar també el temporitzador en tocar «anterior» al primer grup.
    var replayEpoch by remember(initialStoryKey) { mutableStateOf(0) }
    var progress by remember(currentGroupIndex, replayEpoch) { mutableStateOf(0f) }

    val group = stableGroups[currentGroupIndex.coerceIn(stableGroups.indices)]
    val representativeStory = group.openStory
    val artistPhotoUri = photoForArtist(group.artist)

    // 10cn: abans cada canvi de Batec recorria i ordenava tota l'agenda dins del
    // fil de Compose. Ara fem UNA passada en Dispatchers.Default per als grups visibles.
    val wantedArtistKeys = remember(stableGroups) {
        stableGroups.map { it.artist.trim().lowercase(Locale.ROOT) }.filter(String::isNotBlank).toSet()
    }
    val activeConcertsByArtist by produceState<Map<String, List<Concert>>>(
        initialValue = emptyMap(),
        key1 = concerts,
        key2 = wantedArtistKeys
    ) {
        value = withContext(Dispatchers.Default) {
            val today = LocalDate.now()
            val out = linkedMapOf<String, MutableList<Concert>>()
            val seenIds = hashSetOf<Int>()
            concerts.forEach { concert ->
                if (seenIds.add(concert.id)) {
                    val lastDateText = concert.endDate.takeIf(String::isNotBlank) ?: concert.date
                    val lastDate = runCatching { LocalDate.parse(lastDateText.take(10)) }.getOrNull()
                    if (lastDate == null || !lastDate.isBefore(today)) {
                        val keys = buildSet {
                            concert.artist.trim().lowercase(Locale.ROOT).takeIf(String::isNotBlank)?.let(::add)
                            concert.lineup.forEach { artist ->
                                artist.trim().lowercase(Locale.ROOT).takeIf(String::isNotBlank)?.let(::add)
                            }
                        }.filter { it in wantedArtistKeys }
                        keys.forEach { key -> out.getOrPut(key) { mutableListOf() }.add(concert) }
                    }
                }
            }
            val comparator = compareBy<Concert> { concert ->
                runCatching { LocalDate.parse(concert.date.take(10)) }.getOrNull() ?: LocalDate.MAX
            }.thenBy { concert ->
                concert.time.take(5).takeIf { it.matches(Regex("\\d{2}:\\d{2}")) } ?: "99:99"
            }.thenBy { concert -> concert.title.lowercase(Locale.ROOT) }
            out.mapValues { (_, items) -> items.sortedWith(comparator) }
        }
    }
    val activeArtistConcerts = activeConcertsByArtist[group.artist.trim().lowercase(Locale.ROOT)].orEmpty()

    val storyPhotoCandidates = remember(group.key, artistPhotoUri, activeArtistConcerts) {
        buildList<String?> {
            add(artistPhotoUri)
            activeArtistConcerts.firstOrNull()?.let { concert ->
                add(concert.artistImageUri)
                add(concert.posterUri)
                add(concert.eventCardImageUri)
            }
        }.filterNotNull().map(String::trim).filter(String::isNotBlank).distinct()
    }

    fun goNextGroup() {
        if (currentGroupIndex < stableGroups.lastIndex) {
            currentGroupIndex += 1
        } else {
            onDismiss()
        }
    }

    fun goPreviousGroup() {
        if (currentGroupIndex > 0) {
            currentGroupIndex -= 1
        } else {
            replayEpoch += 1
        }
    }

    // Obrir o lliscar un Batec marca tot el grup com vist. El cercle continua
    // visible en gris fins que es publique contingut nou amb una altra story_key.
    LaunchedEffect(group.key) {
        onSeenGroup(group.stories)
    }

    LaunchedEffect(currentGroupIndex, replayEpoch, stableGroups.size) {
        progress = 0f
        repeat(150) { step ->
            delay(100L)
            progress = (step + 1) / 150f
        }
        goNextGroup()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 16f),
            shape = RoundedCornerShape(24.dp),
            color = Color.Transparent
        ) {
            val storyArtwork = rememberImageBitmapFromUris(storyPhotoCandidates, maxDimension = 512)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(artistBrush(group.artist))
                    // El detector esta ara al CONTENIDOR PARE del visor. Abans les
                    // dos zones clicables estaven per davall de la capa de contingut
                    // i els tocs laterals no arribaven als seus onClick.
                    // Compose dona prioritat als fills interactius (tancar, seguir,
                    // fletxes, targetes de concerts); els tocs restants naveguen.
                    .pointerInput(stableGroups.size, currentGroupIndex, replayEpoch) {
                        detectTapGestures { position ->
                            if (position.x < size.width / 2f) goPreviousGroup() else goNextGroup()
                        }
                    }
                    // Igual que les històries: desplaçament HORITZONTAL amb el dit.
                    // La llista de concerts continua acceptant el scroll vertical.
                    .pointerInput(stableGroups.size, currentGroupIndex) {
                        var horizontalDrag = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { horizontalDrag = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                horizontalDrag += dragAmount
                                change.consume()
                            },
                            onDragEnd = {
                                when {
                                    horizontalDrag < -56.dp.toPx() -> goNextGroup()
                                    horizontalDrag > 56.dp.toPx() -> goPreviousGroup()
                                }
                                horizontalDrag = 0f
                            },
                            onDragCancel = { horizontalDrag = 0f }
                        )
                    }
            ) {
                if (storyArtwork != null) {
                    Image(
                        bitmap = storyArtwork,
                        contentDescription = "Batec de ${group.artist}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .50f)))
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    // Una sola barra perquè ara cada grup és un únic Batec visual.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(Color.White.copy(alpha = .30f))
                    ) {
                        if (progress > 0f) {
                            Box(
                                Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                                    .background(Color.White)
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ArtistAvatar(group.artist, 38.dp, artistPhotoUri)
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text(group.artist, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(
                                "${group.stories.size} novetat${if (group.stories.size == 1) "" else "s"} · ${batecAgeLabel(group.stories.maxOfOrNull { it.publishedAtMillis } ?: representativeStory.publishedAtMillis)}",
                                color = Color.White.copy(alpha = .72f),
                                fontSize = 11.sp
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Outlined.Close, contentDescription = "Tancar", tint = Color.White)
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Text(
                        "Pròxims concerts",
                        color = Color.White.copy(alpha = .78f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        group.artist,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 34.sp,
                        lineHeight = 35.sp
                    )

                    Spacer(Modifier.height(16.dp))

                    if (activeArtistConcerts.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            activeArtistConcerts.forEach { concert ->
                                val dateLabel = runCatching {
                                    AgendaDateFormat.date(concert.date)
                                }.getOrElse { concert.date }
                                val locationLabel = listOf(concert.town, concert.venue)
                                    .filter(String::isNotBlank)
                                    .distinct()
                                    .joinToString(" · ")

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenEvent(concert.id) },
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.Black.copy(alpha = .38f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            if (concert.title.isNotBlank() && !concert.title.equals(group.artist, ignoreCase = true)) {
                                                Text(
                                                    concert.title,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    maxLines = 1
                                                )
                                                Spacer(Modifier.height(2.dp))
                                            }
                                            if (locationLabel.isNotBlank()) {
                                                Text(
                                                    locationLabel,
                                                    color = Color.White.copy(alpha = .94f),
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1
                                                )
                                            }
                                            Spacer(Modifier.height(3.dp))
                                            Text(
                                                listOf(dateLabel, concert.time).filter(String::isNotBlank).joinToString(" · "),
                                                color = Color.White.copy(alpha = .82f),
                                                fontSize = 13.sp
                                            )
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            "›",
                                            color = Color.White,
                                            fontSize = 30.sp,
                                            fontWeight = FontWeight.Light
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            Text(
                                representativeStory.body.ifBlank { "No hi ha concerts actius ara mateix." },
                                color = Color.White.copy(alpha = .90f),
                                fontSize = 16.sp,
                                lineHeight = 22.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    if (representativeStory.organizer) {
                        Button(
                            onClick = { onToggleFollow(group.artist) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isFollowed(group.artist)) "Seguint" else "Seguir ${group.artist}")
                        }
                    }
                }
                // Franges de pulsació laterals: ocupen tota la zona central,
                // però no tapen el tancament de dalt ni el final del visor.
                // Els tocs funcionen fins i tot damunt d'una Column desplaçable.
                if (currentGroupIndex > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxHeight()
                            .width(56.dp)
                            .padding(top = 72.dp, bottom = 48.dp)
                            .clickable { goPreviousGroup() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.ChevronLeft,
                            contentDescription = "Batec anterior",
                            modifier = Modifier
                                .size(42.dp)
                                .background(Color.Black.copy(alpha = .30f), CircleShape)
                                .padding(9.dp),
                            tint = Color.White
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(56.dp)
                        .padding(top = 72.dp, bottom = 48.dp)
                        .clickable { goNextGroup() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.ChevronRight,
                        contentDescription = if (currentGroupIndex < stableGroups.lastIndex) "Batec següent" else "Tancar Batecs",
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color.Black.copy(alpha = .30f), CircleShape)
                            .padding(9.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

private fun batecAgeLabel(publishedAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val minutes = ((nowMillis - publishedAtMillis).coerceAtLeast(0L) / 60_000L).toInt()
    return when {
        minutes < 1 -> "ara"
        minutes < 60 -> "fa ${minutes} min"
        minutes < 24 * 60 -> "fa ${minutes / 60} h"
        else -> "fa ${minutes / (24 * 60)} d"
    }
}

private fun artistBrush(seed: String): Brush {
    val palettes = listOf(
        listOf(Color(0xFF833AB4), Color(0xFFF77737)),
        listOf(Color(0xFF0052D4), Color(0xFF65C7F7)),
        listOf(Color(0xFF11998E), Color(0xFF38EF7D)),
        listOf(Color(0xFFDA4453), Color(0xFF89216B)),
        listOf(Color(0xFFF7971E), Color(0xFFFFD200)),
        listOf(Color(0xFF232526), Color(0xFF414345)),
        listOf(Color(0xFF614385), Color(0xFF516395)),
        listOf(Color(0xFF1D4350), Color(0xFFA43931))
    )
    val index = (seed.hashCode() and Int.MAX_VALUE) % palettes.size
    return Brush.linearGradient(palettes[index])
}

private fun storyRingBrush(seed: String): Brush {
    val extra = if (seed.hashCode() % 2 == 0) Color(0xFF833AB4) else Color(0xFFFD1D1D)
    return Brush.sweepGradient(
        listOf(
            Color(0xFFFCAF45),
            Color(0xFFFD1D1D),
            extra,
            Color(0xFFFCAF45)
        )
    )
}

private fun formatDate(iso: String): String = AgendaDateFormat.date(iso)

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Compartir"))
}


/** Resultat prioritari d'artista a la cerca; independent dels festivals. */
@Composable
private fun SearchedArtistFollowCard203(state: AgendaState, artist: String, onOpen: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(Modifier.clickable(onClick = onOpen)) {
                ArtistAvatar(artist = artist, size = 52.dp, photoUri = state.publicPhotoForArtist(artist))
            }
            Column(Modifier.weight(1f).clickable(onClick = onOpen)) {
                Text(artist, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("Artista · Vore perfil", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(onClick = { state.toggleFollowArtist(artist) }) { Text("Seguir") }
        }
    }
}

/** Un festival, tots els artistes escollits, cadascun amb accés al seu horari real. */
@Composable
private fun FestivalSavedArtists203(state: AgendaState, concert: Concert, onOpenSchedule: (String) -> Unit) {
    val selected = festivalSelectedArtistNames(state, concert.id)
    if (selected.isEmpty()) return
    val config = state.specialEventRemote(concert.id)?.let { remoteSpecialConfig(it, concert) }
        ?: specialEventConfigFor(concert)
    LaunchedEffect(concert.id) { state.refreshSpecialEvent(concert.id) }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .38f)),
        shape = RoundedCornerShape(13.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("${concert.title} · Vull veure (${selected.size})", fontWeight = FontWeight.Black)
            selected.forEach { artist ->
                val row = config?.schedule?.firstOrNull {
                    festivalArtistKey(it.artistName?.takeIf(String::isNotBlank) ?: it.title) == festivalArtistKey(artist)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(artist, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        if (row != null) {
                            Text(listOf(row.date.takeIf(String::isNotBlank)?.let(AgendaDateFormat::date),
                                row.time.takeIf(String::isNotBlank), row.location.takeIf(String::isNotBlank))
                                .filterNotNull().joinToString(" · "), fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    TextButton(onClick = { onOpenSchedule(artist) }) { Text("Horari", fontSize = 12.sp) }
                    TextButton(onClick = { toggleFestivalArtistByName(state, concert.id, artist) }) {
                        Text("Llevar", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
