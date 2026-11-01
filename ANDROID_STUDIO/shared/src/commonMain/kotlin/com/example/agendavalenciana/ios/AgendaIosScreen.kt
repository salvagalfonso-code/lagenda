package com.example.agendavalenciana.ios

import com.example.agendavalenciana.model.BatecCandidate
import com.example.agendavalenciana.model.FollowedBatecsPolicy
import com.example.agendavalenciana.model.IosBatecArtistGroup
import com.example.agendavalenciana.model.IosBatecArtistGroups
import com.example.agendavalenciana.model.GlobalAdPlacementPolicy
import com.example.agendavalenciana.model.IosAccountCachePolicy
import com.example.agendavalenciana.model.IosFeedExclusionPolicy
import com.example.agendavalenciana.model.IosFestivalSavedSlotPolicy
import com.example.agendavalenciana.model.DiscoveryLevel
import com.example.agendavalenciana.model.PersonalizationOptions
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first

private val Ink = Color(0xFF252329)
private val Accent = Color(0xFFE8DFF2)
private val Pale = Color(0xFFF7F6F9)

/** Native iPhone Compose screen. The original Android UI remains in app/. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaIosScreen(sessionBridge: IosSessionBridge? = null) {
    IosNativeBridge.current = sessionBridge
    val api = remember { IosAgendaApi(sessionBridge) }
    val scope = rememberCoroutineScope()
    var events by remember { mutableStateOf(IosEventCatalog.cached()) }
    var updating by remember { mutableStateOf(false) }
    var manualRefreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf("Inici") }
    var feedMode by remember { mutableStateOf("Per a tu") }
    var query by remember { mutableStateOf("") }
    var appliedQuery by remember { mutableStateOf("") }
    // Query is committed only by Search / tapping a suggestion. Typing never recalculates the feed.
    var suggestionDraft by remember { mutableStateOf("") }
    LaunchedEffect(query) { kotlinx.coroutines.delay(240); suggestionDraft = query.trim() }
    var chosenSuggestion by remember { mutableStateOf<IosSearchSuggestion?>(null) }
    var appliedSuggestion by remember { mutableStateOf<IosSearchSuggestion?>(null) }
    var limit by remember { mutableIntStateOf(IosFeedReadingPolicy.initial) }
    // Pas 14: only the server holds a permanent reading position. RAM serves fast UI
    // navigation; no scroll location is stored in UserDefaults or a local preferences file.
    val readingMemory = remember { IosFeedReadingMemory() }
    var readingRestoreEpoch by remember { mutableIntStateOf(0) }
    var readingPersistEpoch by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<AgendaEventSummary?>(null) }
    var artist by remember { mutableStateOf<String?>(null) }
    var pendingPushEventId by remember { mutableStateOf<Int?>(null) }
    var artistSourceEvent by remember { mutableStateOf<AgendaEventSummary?>(null) }
    // Public artist data only in RAM; no cached personal or profile state in UserDefaults.
    var artistPublicPage by remember { mutableStateOf<IosArtistPublicPage?>(null) }
    var artistPageLoading by remember { mutableStateOf(false) }
    var artistPageError by remember { mutableStateOf("") }
    var artistPageRefresh by remember { mutableIntStateOf(0) }
    LaunchedEffect(artist, artistPageRefresh) {
        val name = artist?.takeIf(String::isNotBlank) ?: return@LaunchedEffect
        artistPublicPage = null
        artistPageError = ""
        artistPageLoading = true
        try {
            val loaded = api.artistPublicPage(name)
            if (artist == name) artistPublicPage = loaded
        } catch (ex: Exception) {
            if (artist == name) artistPageError = ex.message ?: "No s'ha pogut carregar la fitxa."
        } finally {
            if (artist == name) artistPageLoading = false
        }
    }

    LaunchedEffect(sessionBridge) {
        sessionBridge?.setFeedLifecycleListener(object : IosFeedLifecycleCallback {
            override fun onBackground() {
                readingMemory.markBackground(currentEpochMillis())
                readingPersistEpoch += 1
            }
            override fun onForeground() {
                if (readingMemory.foreground(currentEpochMillis())) {
                    limit = IosFeedReadingPolicy.initial
                    readingRestoreEpoch += 1
                }
            }
        })
    }
    LaunchedEffect(sessionBridge) {
        sessionBridge?.setPushOpenListener(object : IosPushOpenCallback {
            override fun onOpenArtist(artistName: String) {
                if (artistName.isNotBlank()) {
                    tab = "Inici"
                    artistSourceEvent = null
                    artist = artistName
                }
            }
            override fun onOpenEvent(eventId: Int) {
                if (eventId > 0) pendingPushEventId = eventId
            }
        })
    }
    LaunchedEffect(pendingPushEventId, events) {
        val id = pendingPushEventId ?: return@LaunchedEffect
        val match = events.firstOrNull { it.id == id } ?: return@LaunchedEffect
        pendingPushEventId = null
        tab = "Inici"
        selected = match
    }
    var festivalMatchedArtist by remember { mutableStateOf("") }
    var savedFestivalRevision by remember { mutableIntStateOf(0) }
    // RAM-only optimistic projection of encrypted pending writes, scoped to the user.
    // It must never become a second persistent source of truth.
    var festivalSlotOverrides by remember { mutableStateOf<Map<Pair<Int, Int>, Set<String>>>(emptyMap()) }
    var batecStoryKey by remember { mutableStateOf<String?>(null) }
    // Snapshot in RAM: a seen confirmation may reorder the strip, never the open viewer.
    var batecViewerGroups by remember { mutableStateOf(emptyList<IosBatecArtistGroup>()) }
    var batecViewerStories by remember { mutableStateOf(emptyList<IosBatec>()) }
    var batecViewerOwnerId by remember { mutableStateOf<Int?>(null) }
    var batecsReturnEpoch by remember { mutableIntStateOf(0) }
    fun closeBatecs() {
        batecStoryKey = null
        batecViewerGroups = emptyList()
        batecViewerStories = emptyList()
        batecViewerOwnerId = null
        batecsReturnEpoch += 1
    }
    var searchFiltersOpen by remember { mutableStateOf(false) }
    var searchTown by remember { mutableStateOf("") }
    var searchCategory by remember { mutableStateOf("") }
    var searchComarca by remember { mutableStateOf("") }
    var searchStyle by remember { mutableStateOf("") }
    var searchRadius by remember { mutableIntStateOf(0) }
    var searchFromDate by remember { mutableStateOf("") }
    var searchToDate by remember { mutableStateOf("") }
    var searchMonth by remember { mutableStateOf("") }
    var searchPriceLimit by remember { mutableStateOf<Int?>(null) }
    var searchFreeOnly by remember { mutableStateOf(false) }
    var searchCategories by remember { mutableStateOf(emptySet<String>()) }
    var searchPresetRevision by remember { mutableIntStateOf(0) }
    var searchPresetSaving by remember { mutableStateOf(false) }
    var appliedSearchFilters by remember { mutableStateOf(IosSearchFilters()) }
    var searchPresets by remember { mutableStateOf(emptyList<IosSavedSearch>()) }
    var newSearchName by remember { mutableStateOf("") }
    var publisherMenuOpen by remember { mutableStateOf(false) }
    var adminDestination by remember { mutableStateOf("Resum") }
    var serverPublisherAccess by remember { mutableStateOf<IosPublisherSnapshot?>(null) }
    LaunchedEffect(account?.id, tab) {
        val id = account?.id
        serverPublisherAccess = null
        if (id != null && tab == "Perfil") {
            val verified = runCatching { api.publisherSnapshot() }.getOrNull()
            if (account?.id == id) serverPublisherAccess = verified
        }
    }
    var loginOpen by remember { mutableStateOf(false) }
    var registrationOpen by remember { mutableStateOf(false) }
    var resetOpen by remember { mutableStateOf(false) }
    var prefOpen by remember { mutableStateOf(false) }
    var notificationsOpen by remember { mutableStateOf(false) }
    var communityGroup by remember { mutableStateOf<IosCommunityGroup?>(null) }
    var user by remember { mutableStateOf(IosUserState()) }
    var account by remember { mutableStateOf<IosAccount?>(null) }
    // Pas 4: never persist the server snapshot on the phone; keep it only in RAM.
    // Its revision is useful for future optimistic writes in step 5.
    var serverState by remember { mutableStateOf<IosHydratedServerState?>(null) }
    var guestReadyUserId by remember { mutableStateOf<Int?>(null) }
    var serverStateRefreshEpoch by remember { mutableIntStateOf(0) }
    suspend fun updateFestivalSlots(owner: Int, eventId: Int, selected: Set<String>): Boolean {
        require(owner > 0 && eventId > 0)
        check((account?.id ?: guestReadyUserId) == owner) { "La sessió ha canviat" }
        val confirmed = try {
            api.savePersonalState(owner, mapOf(
                "ios_festival_schedule_${eventId}" to IosPersonalEncoded.strings(selected)))
            true
        } catch (_: IosQueuedForRetryException) {
            // The secure queue has already persisted the write and will retry it.
            false
        }
        check((account?.id ?: guestReadyUserId) == owner) { "La sessió ha canviat" }
        festivalSlotOverrides = festivalSlotOverrides + ((owner to eventId) to selected)
        savedFestivalRevision += 1
        if (confirmed) serverStateRefreshEpoch += 1
        return confirmed
    }
    var notices by remember { mutableStateOf(emptyList<IosNotification>()) }
    var notificationSettings by remember { mutableStateOf(IosNotificationCenterSettings()) }
    var notificationSettingsRevision by remember { mutableIntStateOf(0) }
    var notificationSettingsBusy by remember { mutableStateOf(false) }
    var notificationSettingsLoadedOwner by remember { mutableStateOf<Int?>(null) }

    var tickets by remember { mutableStateOf(emptyList<IosTicket>()) }
    var ticketsOffline by remember { mutableStateOf(false) }
    var ticketLoadNotice by remember { mutableStateOf("") }
    var groups by remember { mutableStateOf(emptyList<IosCommunityGroup>()) }
    var batecs by remember { mutableStateOf(emptyList<IosBatec>()) }
    var seenBatecs by remember { mutableStateOf(emptySet<String>()) }
    var batecsRefreshEpoch by remember { mutableIntStateOf(0) }
    var paid by remember { mutableStateOf(IosPromoted()) }
    // One popup per local calendar date, as in Android; never lock navigation for feeds.
    var popupAd by remember { mutableStateOf<IosAd?>(null) }
    val viewedAds = remember { mutableSetOf<String>() }
    fun adViewed(ad: IosAd) {
        if (viewedAds.add("${ad.id}:${ad.placement}"))
            scope.launch { runCatching { api.trackAdvertisement(ad, false) } }
    }
    fun adOpened(ad: IosAd, ev: AgendaEventSummary) {
        scope.launch { runCatching { api.trackAdvertisement(ad, true) } }
        popupAd = null
        selected = ev
        festivalMatchedArtist = ""
    }
    var entityStyles by remember { mutableStateOf(IosEntityAffinityCatalog.cached()) }
    var chosenCategories by remember { mutableStateOf(emptySet<String>()) }
    var chosenTowns by remember { mutableStateOf(emptySet<String>()) }
    var chosenSubcategories by remember { mutableStateOf(emptySet<String>()) }
    var chosenComarques by remember { mutableStateOf(emptySet<String>()) }
    var chosenRadii by remember { mutableStateOf(emptyMap<String, Int>()) }
    // Do not apply an older GET response over a new in-flight user choice.
    var locationEditRevision by remember { mutableIntStateOf(0) }
    var feedEditRevision by remember { mutableIntStateOf(0) }
    // Reject a follow GET that started before an in-flight or queued edit.
    var followEditRevision by remember { mutableIntStateOf(0) }
    // One in-flight action per event; older server snapshots must not roll back guardats.
    var savedEditRevision by remember { mutableIntStateOf(0) }
    var savingEventIds by remember { mutableStateOf(emptySet<Int>()) }
    var uiLanguage by remember { mutableStateOf(IosUiLanguage.VALENCIAN) }
    var showSuggestions by remember { mutableStateOf(true) }
    var valencianFirst by remember { mutableStateOf(true) }
    var discoveryLevel by remember { mutableStateOf(DiscoveryLevel.BALANCED) }

    // Personal feed exclusions are hydrated only from the server, never UserDefaults.
    var excludedEventIds by remember { mutableStateOf(emptySet<Int>()) }
    var excludedArtists by remember { mutableStateOf(emptySet<String>()) }
    var excludedCategories by remember { mutableStateOf(emptySet<String>()) }
    var visibilityEditRevision by remember { mutableIntStateOf(0) }
    // Pending times are RAM-only and owner-scoped; encrypted queue is the durable offline source.
    var reminderTimeOverrides by remember { mutableStateOf(emptyMap<Pair<Int, Int>, String>()) }
    fun remoteReminderTime(eventId: Int): String {
        val owner = account?.id ?: guestReadyUserId
        val pending = owner?.let { reminderTimeOverrides[it to eventId] }
        if (pending != null) return pending
        return serverState?.takeIf { it.userId == owner }?.central?.long("when_$eventId")
            ?.takeIf { it > 0 }?.let { IosNativeBridge.current?.formatReminderEpochMillis(it) }.orEmpty()
    }
    var alertToDisable by remember { mutableStateOf<AgendaEventSummary?>(null) }
    var reminderToSchedule by remember { mutableStateOf<AgendaEventSummary?>(null) }
    var ticketEvent by remember { mutableStateOf<AgendaEventSummary?>(null) }
    // Only RAM (never local persistent user data). Keeps pending server order IDs
    // across opening/closing the entry dialog, separated by server user/event.
    var pendingTicketPayments by remember {
        mutableStateOf(mapOf<Pair<Int, Int>, IosTicketPaymentStart>())
    }
    var groupCreateFor by remember { mutableStateOf<AgendaEventSummary?>(null) }
    var validatorOpen by remember { mutableStateOf(false) }
    var savedLists by remember { mutableStateOf(emptyList<IosSavedList>()) }
    var savedListsRevision by remember { mutableIntStateOf(0) }
    var profileSettings by remember { mutableStateOf<IosProfileSettings?>(null) }
    var profileSettingsOpen by remember { mutableStateOf(false) }
    var myProfile by remember { mutableStateOf<IosMyProfile?>(null) }
    var editProfileOpen by remember { mutableStateOf(false) }
    var editAccountOpen by remember { mutableStateOf(false) }
    var deleteAccountOpen by remember { mutableStateOf(false) }
    var reportEvent by remember { mutableStateOf<AgendaEventSummary?>(null) }
    var pendingFollows by remember { mutableStateOf(emptySet<String>()) }
    var organizerAccountKeys by remember { mutableStateOf(emptyMap<String, String>()) }
    fun resolvedFollowKey(type: String, labelOrKey: String): String {
        val raw = labelOrKey.trim()
        if (type == "CATEGORY") return IosCategoryTaxonomy.mainKey(raw) ?: raw
        if (type != "ORGANIZER") return raw
        // For existing follows, preserve the exact user_follows.entity_key even if the
        // public-facing label differs from its username.
        val existing = user.follows.filter { it.type.equals("ORGANIZER", true) &&
            IosFollowMutation.canonicalKey(it.label) == IosFollowMutation.canonicalKey(raw) }
        return existing.singleOrNull()?.key ?: organizerAccountKeys[IosFollowMutation.canonicalKey(raw)] ?: raw
    }
    var artistBells by remember { mutableStateOf(emptyMap<String, IosArtistBell>()) }
    var artistBellEditing by remember { mutableStateOf<String?>(null) }


    fun work(action: suspend () -> Unit) {
        scope.launch {
            try { error = ""; action() }
            catch (e: IosQueuedForRetryException) {
                status = e.message ?: "Acció pendent de sincronitzar"
            }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { error = e.message ?: "No s'ha pogut completar l'operació." }
        }
    }
    fun refresh(force: Boolean = false) {
        if (updating) return
        updating = true
        if (force) manualRefreshing = true
        val followRevisionBeforeRead = followEditRevision
        val followOwnerBeforeRead = account?.id ?: guestReadyUserId
        work {
            try {
                events = IosEventCatalog.get(force)
                paid = runCatching { api.promoted() }.getOrDefault(paid)
                entityStyles = withContext(Dispatchers.Default) { IosEntityAffinityCatalog.refresh(api, force) }
                if (api.activePersonalId() != null) runCatching { api.userState() }.onSuccess { fresh ->
                    if (followRevisionBeforeRead == followEditRevision &&
                        followOwnerBeforeRead == (account?.id ?: guestReadyUserId)) {
                        user = fresh
                        val selectedCategories = IosCategoryTaxonomy.fromServer(fresh.follows)
                        chosenCategories = selectedCategories.categories
                        chosenSubcategories = selectedCategories.subcategories
                    }
                }
                if (force) status = "Agenda actualitzada"
            } finally {
                updating = false
                if (force) manualRefreshing = false
            }
        }
    }
    // An account switch resets personal UI state until its own server snapshot arrives.
    fun useAccountPreferences(id: Int?) {
        // A guest/registered account transition must not reopen a previous person's editor.
        editAccountOpen = false; editProfileOpen = false; profileSettingsOpen = false
        tickets = emptyList(); ticketsOffline = false; ticketLoadNotice = ""
        profileSettings = null; myProfile = null
        searchPresetRevision += 1
        searchPresets = emptyList()
        searchPresetSaving = false
        followEditRevision += 1 // invalidate any GET launched for a previous account
        chosenCategories = emptySet(); chosenTowns = emptySet(); chosenSubcategories = emptySet()
        chosenComarques = emptySet(); chosenRadii = emptyMap()
        showSuggestions = true; valencianFirst = true; discoveryLevel = DiscoveryLevel.BALANCED
        uiLanguage = IosUiLanguage.VALENCIAN
        excludedEventIds = emptySet(); excludedArtists = emptySet(); excludedCategories = emptySet()
        batecs = emptyList()
        serverState = null
        notificationSettings = IosNotificationCenterSettings()
        IosNativeBridge.current?.setPushDeliveryEnabled(false)
        notificationSettingsLoadedOwner = null
        notificationSettingsRevision += 1
    }
    /** On network failure retain ONLY the pre-fetched encrypted tickets for the same owner.
     * Never treat the cached status as authoritative for entry validation.
     */
    suspend fun reloadMyTickets() {
        // An offline relaunch cannot call auth/me.php. Show already encrypted QR first,
        // then try restoring the registered session if the network has returned.
        if (account == null && api.hasRegisteredSessionToken()) {
            api.offlineTickets()?.let { cached ->
                tickets = cached; ticketsOffline = true
                ticketLoadNotice = "Còpia sense connexió. L'estat necessita confirmació del servidor."
            }
            val restored = api.restoreAccount()
            if (restored != null && account == null) {
                account = restored; useAccountPreferences(restored.id); loadAccountPages()
            }
        }
        val expectedOwner = account?.id ?: guestReadyUserId ?: api.activePersonalId()
        val hasSecureOwner = expectedOwner != null || api.hasRegisteredSessionToken()
        if (!hasSecureOwner) { tickets = emptyList(); ticketsOffline = false; return }
        val result = if (expectedOwner != null) runCatching { api.tickets() } else null
        if (expectedOwner != (account?.id ?: guestReadyUserId ?: api.activePersonalId())) return
        if (result?.isSuccess == true) {
            tickets = result.getOrThrow()
            ticketsOffline = false
            ticketLoadNotice = if (api.offlineTickets() == null)
                "Entrades carregades del servidor. No s'ha pogut conservar una còpia xifrada per a ús sense connexió."
                else ""
        } else {
            val cached = api.offlineTickets()
            if (cached != null) {
                tickets = cached
                ticketsOffline = true
                ticketLoadNotice = "Còpia xifrada per a ús sense connexió. L'estat pot haver canviat; no confirma validesa."
            } else {
                tickets = emptyList()
                ticketsOffline = false
                ticketLoadNotice = "No s'han pogut recuperar entrades. Necessites connexió per descarregar-les una primera vegada."
            }
        }
    }
    fun loadAccountPages() {
        val expectedAccount = account?.id ?: return
        val followRevisionBeforeRead = followEditRevision
        val savedRevisionBeforeRead = savedEditRevision
        val listsRevisionBeforeRead = savedListsRevision
        val presetsRevisionBeforeRead = searchPresetRevision
        work {
            // Independent requests can run together. One slow mailbox must not delay tickets,
            // follows or saved lists; failed optional calls retain their previous contents.
            coroutineScope {
                val userRequest = async { api.userState() }
                val noticesRequest = async { runCatching { api.notifications() }.getOrNull() }
                val ticketsRequest = async { runCatching { api.tickets() }.getOrNull() }
                val groupsRequest = async { runCatching { api.community() }.getOrNull() }
                val seenRequest = async { runCatching { api.seenBatecs() }.getOrNull() }
                val listsRequest = async { runCatching { api.savedLists() }.getOrNull() }
                val presetsRequest = async { runCatching { api.savedSearches() }.getOrNull() }
                val bellsRequest = async { runCatching { api.artistBells() }.getOrNull() }
                val newUser = userRequest.await()
                val newNotices = noticesRequest.await()
                val newTickets = ticketsRequest.await()
                val newGroups = groupsRequest.await()
                val newSeen = seenRequest.await()
                val newSavedLists = listsRequest.await()
                val newPresets = presetsRequest.await()
                val newBells = bellsRequest.await()
                // Never replace the new account's state with an old account's response.
                if (account?.id == expectedAccount) {
                    if (followRevisionBeforeRead == followEditRevision) {
                        user = newUser.copy(saved = if (savedRevisionBeforeRead == savedEditRevision)
                            newUser.saved else user.saved)
                        val selectedCategories = IosCategoryTaxonomy.fromServer(newUser.follows)
                        chosenCategories = selectedCategories.categories
                        chosenSubcategories = selectedCategories.subcategories
                    } else if (savedRevisionBeforeRead == savedEditRevision) {
                        user = user.copy(saved = newUser.saved)
                    }
                    newNotices?.let { notices = it }
                    newBells?.let { artistBells = it.filter { b -> b.enabled }.associateBy { b -> IosArtistBellPolicy.key(b.artistName) } }
                    if (newTickets != null) {
                        tickets = newTickets; ticketsOffline = false; ticketLoadNotice = ""
                    } else {
                        api.offlineTickets()?.let { cached ->
                            tickets = cached; ticketsOffline = true
                            ticketLoadNotice = "Còpia sense connexió: consulta l'estat actual en tornar a tindre internet."
                        }
                    }
                    newGroups?.let { groups = it }
                    newSeen?.let { seenBatecs = it }
                    if (listsRevisionBeforeRead == savedListsRevision) newSavedLists?.let { savedLists = it }
                    if (presetsRevisionBeforeRead == searchPresetRevision)
                        newPresets?.let { searchPresets = it }
                }
            }
        }
    }
    // Step 24: server-only private search presets for guests AND registered accounts.
    // A stale request cannot import presets from a previous identity.
    LaunchedEffect(account?.id, guestReadyUserId, searchFiltersOpen) {
        val expectedOwner = account?.id ?: guestReadyUserId ?: return@LaunchedEffect
        val before = searchPresetRevision
        val remote = runCatching { api.savedSearches() }.getOrNull()
        if (remote != null && (account?.id ?: guestReadyUserId) == expectedOwner &&
            before == searchPresetRevision) searchPresets = remote
    }
    LaunchedEffect(tab, account?.id, guestReadyUserId) {
        if (tab == "Entrades") reloadMyTickets()
    }
    // Pas 6: retry encrypted pending actions while the app is visible and at tab changes.
    // Never replay one account's actions with another account's token. When acknowledged,
    // read the server again so Android/iOS present the same canonical information.
    LaunchedEffect(account?.id, guestReadyUserId, tab) {
        while (true) {
            val ownerBefore = account?.id ?: guestReadyUserId
            if (ownerBefore != null && ownerBefore == api.activePersonalId()) {
                try {
                    val replayed = api.flushOfflineActions()
                    if (replayed > 0 && (account?.id ?: guestReadyUserId) == ownerBefore) {
                        serverStateRefreshEpoch += 1
                        status = "Accions pendents sincronitzades amb el servidor"
                    }
                } catch (cancel: kotlinx.coroutines.CancellationException) {
                    throw cancel
                } catch (_: Exception) {
                    // Keep the encrypted queue; retry on next foreground/tab or in 60s.
                }
            }
            kotlinx.coroutines.delay(60_000L)
        }
    }
    // Drop an optimistic overlay ONLY when the same user's server state confirms it.
    LaunchedEffect(serverState?.central?.revision, serverState?.userId) {
        val snapshot = serverState ?: return@LaunchedEffect
        val confirmed = festivalSlotOverrides.filter { (key, slots) ->
            key.first == snapshot.userId && snapshot.personalFestivalSlots(key.second) == slots
        }.keys
        if (confirmed.isNotEmpty()) festivalSlotOverrides = festivalSlotOverrides - confirmed
    }
    // Guardats is available to guest users too. Refresh canonical saved IDs on entry,
    // preserving newer local actions until the server confirms them.
    LaunchedEffect(tab, account?.id, guestReadyUserId) {
        val owner = account?.id ?: guestReadyUserId
        if (tab == "Guardats" && owner != null) {
            val revisionBefore = savedEditRevision
            val listsBefore = savedListsRevision
            val fromServer = runCatching { api.userState() }.getOrNull()
            if (fromServer != null && (account?.id ?: guestReadyUserId) == owner &&
                revisionBefore == savedEditRevision) {
                user = user.copy(saved = fromServer.saved)
            }
            val remoteLists = runCatching { api.savedLists() }.getOrNull()
            if (remoteLists != null && (account?.id ?: guestReadyUserId) == owner && listsBefore == savedListsRevision) savedLists = remoteLists
        }
    }
    // Pas 4: server-first hydration for BOTH guest and registered accounts. Never write
    // the fetched values back to UserDefaults or to the server. A failed read leaves
    // in-memory/UI values untouched and may be retried after the next session refresh.
    LaunchedEffect(account?.id, account?.role, guestReadyUserId, serverStateRefreshEpoch) {
        val expectedId = account?.id ?: guestReadyUserId ?: return@LaunchedEffect
        val expectedUsername = account?.username
        val expectedLocationRevision = locationEditRevision
        val expectedFeedRevision = feedEditRevision
        val expectedFollowRevision = followEditRevision
        val expectedSavedRevision = savedEditRevision
        val expectedVisibilityRevision = visibilityEditRevision
        val expectedNotificationRevision = notificationSettingsRevision
        val loaded = runCatching { api.loadServerState() }.getOrNull() ?: return@LaunchedEffect
        if (loaded.userId != expectedId || (account?.id ?: guestReadyUserId) != expectedId ||
            account?.username != expectedUsername) return@LaunchedEffect
        serverState = loaded
        // Import a saved reading session ONCE for this identity; late GET responses
        // can never rewind an active scroll or import another account's position.
        if (readingMemory.importServer(loaded.central.string(IosFeedReadingPolicy.serverKey),
                expectedId, currentEpochMillis())) readingRestoreEpoch += 1
        // Dedicated location/feed API values take priority over the Android SharedPrefs
        // mirror. Project only well-known fields; preserve ALL others inside serverState.
        val locationWritesPending = runCatching { api.hasPendingLocationWrites(expectedId) }.getOrDefault(true)
        if (!locationWritesPending && locationEditRevision == expectedLocationRevision) {
            loaded.towns(expectedUsername)?.let { chosenTowns = it }
            loaded.comarques(expectedUsername)?.let { chosenComarques = it }
            loaded.radii(expectedUsername)?.let { chosenRadii = it.filterKeys { name ->
                loaded.towns(expectedUsername)?.contains(name) == true
            } }
        }
        // Separate feed and UI-language domains, but guard BOTH against pending writes.
        // A delayed GET must not overwrite edits accepted into the encrypted offline queue.
        val feedWritesPending = runCatching { api.hasPendingFeedWrites(expectedId) }.getOrDefault(true)
        if (!feedWritesPending && feedEditRevision == expectedFeedRevision) {
            loaded.suggestions()?.let { showSuggestions = it }
            loaded.valencianFirst()?.let { valencianFirst = it }
            loaded.discovery()?.let { discoveryLevel = it }
            loaded.uiLanguage()?.let { uiLanguage = it }
        }
        if (notificationSettingsRevision == expectedNotificationRevision) {
            val notifyWritesPending = runCatching { api.hasPendingNotificationSettingsWrites(expectedId) }.getOrDefault(true)
            if (!notifyWritesPending) {
                notificationSettings = notificationSettings.withCentral(loaded.central)
                IosNativeBridge.current?.setPushDeliveryEnabled(notificationSettings.pushEnabled)
            }
        }
        // Pas 11: affinities are returned from user-state/mine.php, not ios_recent_styles_v1.
        // Pas 7: category/subcategory selection comes only from user_follows, never ios_pref_subcategories_v1.
        loaded.visibility?.let { remote ->
            val projected = runCatching { api.reconcilePendingVisibility(remote, expectedId) }.getOrNull()
            if (projected != null && visibilityEditRevision == expectedVisibilityRevision) {
                excludedEventIds = projected.events; excludedArtists = projected.artists
                excludedCategories = projected.categories
            }
        }
        // Follows, saved events and alerts live in dedicated tables and API, not in
        // sync/state.php. Guests may read them too; account-specific screens read
        // further domain data independently in loadAccountPages().
        runCatching {
            loaded.userDomain?.let { api.reconcilePendingUserState(api.decodeUserState(it), expectedId) }
                ?: api.userState()
        }.onSuccess { loadedUser ->
            if ((account?.id ?: guestReadyUserId) == expectedId && account?.username == expectedUsername) {
                if (followEditRevision == expectedFollowRevision) {
                    user = loadedUser.copy(saved = if (savedEditRevision == expectedSavedRevision)
                        loadedUser.saved else user.saved)
                    val selection = IosCategoryTaxonomy.fromServer(loadedUser.follows)
                    chosenCategories = selection.categories // exact Android entity_key
                    chosenSubcategories = selection.subcategories // exact v336 codes
                } else if (savedEditRevision == expectedSavedRevision) {
                    user = user.copy(saved = loadedUser.saved)
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        // Public Android-compatible username resolution; optional and kept only in RAM.
        organizerAccountKeys = runCatching { api.publicOrganizerAccountKeys() }.getOrDefault(emptyMap())
    }
    LaunchedEffect(Unit) {
        refresh(false)
        val restored = api.restoreAccount()
        if (restored != null) {
            account = restored; useAccountPreferences(restored.id); loadAccountPages()
            myProfile = runCatching { api.myPublicProfile() }.getOrNull()
        } else if (!api.hasRegisteredSessionToken()) {
            // Android v336 registers every guest in MariaDB. Retry in case the first launch
            // is offline; never replace a registered token during a temporary API outage.
            while (true) {
                val createdId = runCatching { api.ensureGuestIdentity() }.getOrNull()
                if (createdId != null) { guestReadyUserId = createdId; break }
                kotlinx.coroutines.delay(60_000L)
            }
        }
    }
    // Pas 3: reclaim guest data after EVERY successful authentication, including
    // restored sessions. A failed merge never discards the guest token: retry later.
    // Guest personal state is fetched from the server again after a confirmed merge.
    LaunchedEffect(account?.id) {
        val expectedUserId = account?.id ?: return@LaunchedEffect
        while (api.hasPendingGuestClaim() && account?.id == expectedUserId) {
            val outcome = runCatching { api.claimPendingGuestIdentity(expectedUserId) }
            if (outcome.isSuccess && outcome.getOrDefault(false)) {
                if (account?.id == expectedUserId) {
                    status = "Dades de convidat vinculades al compte"
                    loadAccountPages()
                    serverStateRefreshEpoch += 1 // read merged state after server claim
                }
                break
            }
            if (outcome.isFailure && account?.id == expectedUserId) {
                status = "Fusió de convidat pendent: es reintentarà en recuperar connexió."
            }
            kotlinx.coroutines.delay(60_000L)
        }
    }
    // Campaigns expire or begin independently of the 30-minute personal feed cache.
    // Refresh only paid inventory, quietly while Inici is open; never display a spinner.
    LaunchedEffect(tab) {
        if (tab == "Inici") {
            while (true) {
                kotlinx.coroutines.delay(120_000L)
                runCatching { api.promoted() }.onSuccess { paid = it }
            }
        }
    }
    val today = currentLocalDateIso() // Re-evaluate local calendar date on recomposition (midnight/foreground).
    val available = remember(events, paid, today) {
        // The commercial feed is not allowed to lose paid campaigns or internal ticket sales
        // just because the global catalog is stale or incomplete.
        (events + paid.priorityEvents).distinctBy { it.key }
            .filter { IosUnfilteredAgenda.hasFutureDate(it, today) }
            .sortedWith(compareBy<AgendaEventSummary> { it.date.ifBlank { "9999" } }.thenBy { it.time }.thenBy { it.id })
    }
    // Guardats must not inherit the feed's date filter or occurrence projections:
    // Android checks the final real day at 05:00 (including festival programme).
    val savedCatalog = remember(events, paid.priorityEvents) {
        (events + paid.priorityEvents).distinctBy { it.id }
    }
    val savedCulturalToday = currentAgendaDateIso()
    val eventById = remember(available) { available.associateBy { it.id } }
    LaunchedEffect(paid.ads, today, tab) {
        if (tab != "Inici" || selected != null) return@LaunchedEffect
        val alreadyShown = platformReadPublicCache("ad_popup_last_day") == today
        if (!alreadyShown) {
            val candidate = paid.ads.firstOrNull { it.placement == "POPUP" && it.eventId in eventById }
            if (candidate != null) {
                platformWritePublicCache("ad_popup_last_day", today)
                popupAd = candidate
            }
        }
    }
    // The Android carousel contains ONE circle per followed artist. Multiple server
    // publications share that circle; viewed status is grey only when ALL are seen.
    // Pas 19: single server-backed Batecs refresh, including newly followed artists.
    // A stale response may never cross a guest/registered account boundary.
    LaunchedEffect(events, user.follows, excludedArtists, account?.id, guestReadyUserId, batecsRefreshEpoch) {
        val owner = account?.id ?: guestReadyUserId
        val follows = user.follows.filter { it.type.equals("ARTIST", true) }
            .map { it.label.ifBlank { it.key } }.filter(String::isNotBlank).toSet()
        val eligible = follows.map(IosBatecFeedContract::norm).toSet() -
            excludedArtists.map(IosBatecFeedContract::norm).toSet()
        if (owner == null || eligible.size <= 6) { batecs = emptyList(); return@LaunchedEffect }
        val next = runCatching { api.batecs(follows, events, excludedArtists) }.getOrNull()
        if (next != null && (account?.id ?: guestReadyUserId) == owner) batecs = next
    }
    val followedGroups = remember(batecs, user.follows, seenBatecs, excludedArtists) {
        IosBatecArtistGroups.select(
            batecs.map { BatecCandidate(it.storyKey, it.artist, it.sourceType, it.publishedAt) },
            user.follows.filter { it.type.equals("ARTIST", true) }
                .map { it.label.ifBlank { it.key } }.filter { label ->
                    excludedArtists.none { IosBatecFeedContract.norm(it) == IosBatecFeedContract.norm(label) }
                }.toSet(),
            seenBatecs
        )
    }
    val batecRowState = rememberLazyListState()
    val batecStripOrder = remember(followedGroups) { followedGroups.map { it.key } }
    var keepBatecStripAtStart by remember { mutableStateOf(false) }
    LaunchedEffect(batecsReturnEpoch) {
        if (batecsReturnEpoch > 0) {
            keepBatecStripAtStart = true
            batecRowState.scrollToItem(0)
        }
    }
    LaunchedEffect(batecStripOrder) {
        if (keepBatecStripAtStart) batecRowState.scrollToItem(0)
    }
    fun openBatecs(key: String) {
        val owner = account?.id ?: guestReadyUserId
        if (owner != null && followedGroups.any { it.key == key }) {
            batecViewerGroups = followedGroups.toList()
            batecViewerStories = batecs.toList()
            batecViewerOwnerId = owner
            batecStoryKey = key
        }
    }
    // Build the suggestion index off the UI thread only when the catalog changes.
    val searchPool by produceState<List<IosSearchSuggestion>>(initialValue = emptyList(), available) {
        value = withContext(Dispatchers.Default) {
            IosAdvancedSearch.pool(available, IosGeoPlaces.allTowns(), IosGeoPlaces.allComarques(),
                IosGeoPlaces.municipalityEntries())
        }
    }
    val suggestions by produceState<List<IosSearchSuggestion>>(initialValue = emptyList(), suggestionDraft, searchPool, chosenSuggestion) {
        value = withContext(Dispatchers.Default) {
            if (chosenSuggestion != null) emptyList() else IosAdvancedSearch.suggest(searchPool, suggestionDraft)
        }
    }
    // Step 23: bounded off-main suggestions for the "On?" location filters.
    // Typing does not apply a search or recalculate the event feed.
    var territoryTownDraft by remember { mutableStateOf("") }
    var territoryComarcaDraft by remember { mutableStateOf("") }
    var territoryTownChosen by remember { mutableStateOf(false) }
    var territoryComarcaChosen by remember { mutableStateOf(false) }
    val territoryTownPool = remember(searchPool) { searchPool.filter { it.type == IosSearchType.TOWN } }
    val territoryComarcaPool = remember(searchPool) { searchPool.filter { it.type == IosSearchType.COMARCA } }
    LaunchedEffect(searchTown, searchFiltersOpen) {
        kotlinx.coroutines.delay(220)
        territoryTownDraft = if (searchFiltersOpen && !territoryTownChosen) searchTown else ""
    }
    LaunchedEffect(searchComarca, searchFiltersOpen) {
        kotlinx.coroutines.delay(220)
        territoryComarcaDraft = if (searchFiltersOpen && !territoryComarcaChosen) searchComarca else ""
    }
    val townSuggestions by produceState(initialValue = emptyList<IosSearchSuggestion>(), territoryTownDraft, searchPool) {
        value = withContext(Dispatchers.Default) {
            IosAdvancedSearch.suggest(territoryTownPool, territoryTownDraft, 8)
        }
    }
    val comarcaSuggestions by produceState(initialValue = emptyList<IosSearchSuggestion>(), territoryComarcaDraft, searchPool) {
        value = withContext(Dispatchers.Default) {
            IosAdvancedSearch.suggest(territoryComarcaPool, territoryComarcaDraft, 6)
        }
    }
    // Step 22: a committed full artist name has the same meaning as an ARTIST
    // suggestion, even when the keyboard Search button was used directly.
    val searchedArtist = remember(appliedQuery, appliedSuggestion, searchPool) {
        IosFestivalArtistSearch.resolveArtist(appliedQuery, appliedSuggestion, searchPool)
    }
    val effectiveSearchSuggestion = remember(appliedQuery, appliedSuggestion, searchedArtist) {
        if (searchedArtist != null) IosSearchSuggestion(IosSearchType.ARTIST, searchedArtist)
        else appliedSuggestion
    }
    // Detailed schedules only for festivals that already match the published
    // catalog artist/lineup. No speculative queries for all events on typing.
    val artistFestivalSchedule by produceState<Map<Int, List<AgendaScheduleEntry>>>(
        initialValue = emptyMap(), available, searchedArtist) {
        value = emptyMap()
        if (searchedArtist != null) {
            val candidates = available.asSequence()
                .filter { it.isFestival && IosFestivalArtistSearch.isInLineup(it, searchedArtist) }
                .map { it.id }.distinct().take(80).toList()
            val verified = mutableMapOf<Int, List<AgendaScheduleEntry>>()
            for (id in candidates) {
                val details = try { api.festivalSchedule(id) }
                catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
                catch (_: Exception) { null }
                if (details != null) {
                    verified[id] = details
                    // Publish incrementally: the search doesn't need to wait
                    // for every festival's remote schedule to appear.
                    value = verified.toMap()
                }
            }
        }
    }
    val categoriesForSearch = remember(available) { available.map { it.category }.filter(String::isNotBlank).distinct().sorted() }
    val stylesForSearch = remember(available) { available.flatMap { it.styles.flatMap { s -> s.split('|',',',';') } }.map(String::trim).filter(String::isNotBlank).distinct().sorted() }
    val searchInProgress = appliedQuery.isNotBlank() || appliedSearchFilters.isActive
    // Strict global search: never restrict results to the previous personalized snapshot.
    val computedFiltered by produceState<List<AgendaEventSummary>>(initialValue = emptyList(), available, appliedQuery, searchedArtist,
        effectiveSearchSuggestion, appliedSearchFilters) {
        value = withContext(Dispatchers.Default) {
            available.filter { IosAdvancedSearch.matches(
                it, searchedArtist ?: appliedQuery, effectiveSearchSuggestion, appliedSearchFilters) }
        }
    }
    // Unfiltered renders the catalog immediately, without waiting for an unnecessary re-filter.
    val filtered = if (searchInProgress) computedFiltered else available
    // The potentially costly affinity calculation never runs in the Compose render pass.
    // Only follows (not saved/alerts) and explicit tastes invalidate it; typing does not.
    // Keep the last valid selection visible during an update, but never show another account's.
    val personalizationOwnerId = account?.id ?: guestReadyUserId
    val selectedForYou by produceState<Pair<Int?, List<AgendaEventSummary>>>(
        initialValue = null to emptyList(), available, personalizationOwnerId, user.follows,
        user.affinities, chosenCategories, chosenTowns, chosenSubcategories, chosenComarques, chosenRadii,
        showSuggestions, discoveryLevel, valencianFirst, paid, entityStyles,
        serverState?.central?.revision, serverStateRefreshEpoch, excludedEventIds,
        excludedArtists, excludedCategories, feedMode, searchInProgress
    ) {
        val owner = personalizationOwnerId
        // Never show a different identity's cached personalized events.
        if (owner == null || feedMode != "Per a tu" || searchInProgress) {
            value = owner to emptyList()
        } else {
            val prefs = IosForYouServerOrder.payload(chosenCategories, chosenSubcategories,
                chosenTowns, chosenComarques, chosenRadii, showSuggestions, discoveryLevel, valencianFirst)
            val hasPendingPersonalWrites = try {
                api.hasPendingPersonalWrites(owner)
            } catch (cancel: kotlinx.coroutines.CancellationException) {
                throw cancel
            } catch (_: Exception) { true }
            val androidSignature = if (serverState?.userId == owner && !hasPendingPersonalWrites) {
                IosForYouServerOrder.androidSignature(serverState, account?.username)
            } else null
            val selected = try {
                val serverIds = api.forYouServerSelection(owner, androidSignature, today, prefs)
                IosForYouServerOrder.select(serverIds.ids, available)
            } catch (cancel: kotlinx.coroutines.CancellationException) {
                throw cancel
            } catch (_: Exception) {
                // Offline/older server: preserve access with a 30-min RAM-only fallback.
                // This is NOT labelled as Android's definitive ordered snapshot.
                withContext(Dispatchers.Default) {
                    IosForYouSnapshot.select(available, owner, user, chosenCategories, chosenTowns, paid,
                        chosenSubcategories, chosenComarques, chosenRadii, showSuggestions,
                        discoveryLevel, valencianFirst, entityStyles)
                }
            }
            if ((account?.id ?: guestReadyUserId) == owner) value = owner to selected
        }
    }
    val forYou = if (selectedForYou.first == personalizationOwnerId) selectedForYou.second else emptyList()
    // Commercially guaranteed events retain priority even during searching. No duplicate cards.
    val activeFeed = remember(forYou, filtered, available, paid, feedMode, searchInProgress,
        searchedArtist, artistFestivalSchedule,
        valencianFirst, today, user.follows, chosenCategories, chosenSubcategories, chosenTowns, chosenComarques) {
        // Active paid placements and internal ticket-sale cards are independent of
        // personal preferences and searches, including when the search has zero hits.
        // This is the project's explicit permanent commercial-visibility rule.
        val commercialIds = paid.guaranteedEventIds + paid.ads.map { it.eventId }
        val guaranteed = IosCommercialVisibility.guaranteed(available, commercialIds)
        val organic = when {
            searchInProgress || feedMode == "Sense filtre" -> filtered
            forYou.isEmpty() &&
                !com.example.agendavalenciana.model.DefaultFeedPolicy.hasExplicitSignals(
                    user.follows.filter { it.type == "ARTIST" }.map { it.key }.toSet(),
                    user.follows.filter { it.type == "ORGANIZER" }.map { it.key }.toSet(),
                    user.follows.filter { it.type == "VENUE" }.map { it.key }.toSet(),
                    user.follows.filter { it.type == "CATEGORY" }.map { it.key }.toSet() + chosenCategories + chosenSubcategories,
                    chosenTowns, chosenComarques) -> available
            else -> forYou
        }
        if (!searchInProgress && feedMode == "Sense filtre") {
            // Use the shared full catalog without first concatenating paid subsets (no duplicates).
            // An ongoing festival is dated by its next relevant day, without mutating its original ID.
            IosUnfilteredAgenda.select(available, today,
                paid.guaranteedEventIds + paid.ads.map { it.eventId })
        } else {
            val organicRows = if (searchInProgress && searchedArtist != null)
                IosFestivalArtistSearch.searchRows(organic, searchedArtist, artistFestivalSchedule)
            else organic
            val promotedRows = if (searchInProgress && searchedArtist != null)
                IosFestivalArtistSearch.searchRows(guaranteed, searchedArtist, artistFestivalSchedule)
            else guaranteed
            (promotedRows + organicRows).distinctBy {
                // One original festival ID in artist searches, even when its paid
                // card and artist-day projection have different display dates.
                if (searchInProgress && searchedArtist != null && it.isFestival) "festival:${it.id}" else it.key
            }.sortedWith(compareBy<AgendaEventSummary> { it.displayDate.ifBlank { "9999" } }
                .thenByDescending { feedMode == "Per a tu" && valencianFirst &&
                    (IosGeoPlaces.key(it.language).contains("valencia") ||
                     IosGeoPlaces.key(it.language).contains("catala") || it.language.equals("ca",true)) }
                .thenBy { it.time }.thenBy { it.id })
        }
    }
    // Exclusions do not invalidate the 30-minute ordered snapshot; they are applied
    // only to the organic display list. Commercial commitments always override hiding.
    val personalizedFeed = remember(activeFeed, excludedEventIds, excludedArtists, excludedCategories, paid) {
        val protectedIds = paid.guaranteedEventIds + paid.ads.map { it.eventId }
        activeFeed.filter { ev ->
            IosFeedExclusionPolicy.visible(ev.id, ev.artists + ev.schedule.map { it.artist }, ev.category,
                excludedEventIds, excludedArtists, excludedCategories,
                commerciallyGuaranteed = IosCommercialVisibility.protected(ev, protectedIds), styles = ev.styles)
        }
    }
    val readingOwner = account?.id ?: guestReadyUserId
    val readingViewKey = "v14|${readingOwner ?: 0}|${feedMode}|${appliedQuery}|" +
        "${appliedSuggestion?.value.orEmpty()}|${appliedSearchFilters.hashCode()}"
    // Never reset the visible batch after a routine network refresh or returning
    // to Inici. A new explicit mode/search/view starts with the first five cards.
    var appliedReadingView by remember { mutableStateOf("") }
    LaunchedEffect(readingViewKey, readingRestoreEpoch, readingOwner, personalizedFeed.size) {
        if (appliedReadingView != readingViewKey) {
            limit = IosFeedReadingPolicy.initial
            appliedReadingView = readingViewKey
        }
        val resumed = readingOwner?.let { readingMemory.restore(it, readingViewKey, currentEpochMillis()) }
        if (resumed != null) {
            limit = maxOf(limit, IosFeedReadingPolicy.restoredLimit(resumed, personalizedFeed.size))
        } else if (limit <= 0) {
            limit = IosFeedReadingPolicy.restoredLimit(null, personalizedFeed.size)
        }
    }
    // Debounce writes to sync/state.php. The encrypted offline queue handles failures;
    // no persistent local user-state store is created by this screen.
    LaunchedEffect(readingPersistEpoch, readingOwner) {
        if (readingPersistEpoch == 0) return@LaunchedEffect
        kotlinx.coroutines.delay(2_000L)
        val snapshot = readingMemory.snapshot() ?: return@LaunchedEffect
        if (snapshot.ownerId != readingOwner || api.activePersonalId() != snapshot.ownerId) return@LaunchedEffect
        try {
            api.savePersonalState(snapshot.ownerId, mapOf(
                IosFeedReadingPolicy.serverKey to IosPersonalEncoded.string(snapshot.encode())
            ))
        } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
        catch (_: Exception) { /* The RAM snapshot stays; step 6 handles retryable writes. */ }
    }
    val visibleFeed = remember(personalizedFeed, limit) { personalizedFeed.take(limit) }

    fun recordSearchOnServer(term: String) {
        if (term.trim().length < 3) return
        work {
            try { api.recordSearchAffinity(term) }
            catch (_: IosQueuedForRetryException) { /* Encrypted action will replay later. */ }
        }
    }
    fun showEvent(event: AgendaEventSummary, q: String = "") {
        work {
            try { api.recordOpenAffinity(event) }
            catch (_: IosQueuedForRetryException) { /* Offline queue owns this signal. */ }
        }
        selected = event
        festivalMatchedArtist = if (event.isFestival && q.isNotBlank() &&
            (IosFestivalArtistSearch.isInLineup(event, q) ||
                artistFestivalSchedule[event.id].orEmpty().any { IosFestivalArtistSearch.matchesArtist(it, q) })) q else ""
    }
    fun saveExclusions(events: Set<Int>, artists: Set<String>, categories: Set<String>) {
        val expectedId = account?.id ?: guestReadyUserId
        if (expectedId == null) { error = "Cal establir la identitat del servidor"; return }
        val previous = IosVisibilitySnapshot(excludedEventIds, excludedArtists, excludedCategories)
        val desired = IosVisibilitySnapshot(events, artists, categories)
        visibilityEditRevision += 1
        work {
            var delayed = api.updateVisibility(expectedId, previous, desired)
            // Android v336 never follows and blocks the same artist/category at once.
            val newlyBlockedArtists = desired.artists - previous.artists
            val newlyBlockedCategories = desired.categories - previous.categories
            val conflictingFollows = user.follows.filter { follow ->
                when (follow.type.uppercase()) {
                    "ARTIST" -> newlyBlockedArtists.any { IosFollowMutation.canonicalKey(it) ==
                        IosFollowMutation.canonicalKey(follow.key) }
                    "CATEGORY" -> newlyBlockedCategories.any { blocked ->
                        val main = IosCategoryTaxonomy.mainKey(blocked)
                        val keyMain = IosCategoryTaxonomy.mainKey(follow.key)
                        val childCode = IosCategoryTaxonomy.subcategoryCode(follow.key)
                        (main != null && (main == keyMain || (childCode != null &&
                            IosCategoryTaxonomy.childrenOf(main).any { it.code == childCode }))) ||
                            (IosCategoryTaxonomy.subcategoryCode(blocked) != null &&
                             IosCategoryTaxonomy.subcategoryCode(blocked) == childCode)
                    }
                    else -> false
                }
            }
            for (follow in conflictingFollows) {
                try { api.followEntity(follow.type.uppercase(), follow.key, follow.label.ifBlank { follow.key }, false) }
                catch (_: IosQueuedForRetryException) { delayed = true }
            }
            if ((account?.id ?: guestReadyUserId) == expectedId) {
                val verified = runCatching { api.visibility() }.getOrNull()
                // Offline: the encrypted queue was durably written; show its expected projection.
                val displayed = verified ?: desired
                excludedEventIds = displayed.events; excludedArtists = displayed.artists
                excludedCategories = displayed.categories
                serverStateRefreshEpoch += 1
                status = if (delayed || verified == null) "Exclusions pendents de sincronització"
                    else "Exclusions guardades al servidor"
            }
        }
    }
    fun excludeFromFeed(event: AgendaEventSummary, mode: String) {
        when (mode) {
            "EVENT" -> saveExclusions(excludedEventIds + event.id, excludedArtists, excludedCategories)
            "ARTIST" -> {
                val firstArtist = event.artists.firstOrNull { it.isNotBlank() }
                    ?: event.schedule.firstOrNull { it.artist.isNotBlank() }?.artist ?: return
                saveExclusions(excludedEventIds, excludedArtists + firstArtist, excludedCategories)
            }
            "CATEGORY" -> {
                val code = IosCategoryTaxonomy.mainKey(event.category) ?: return
                saveExclusions(excludedEventIds, excludedArtists, excludedCategories + code)
            }
        }
    }
    fun toggleSaved(event: AgendaEventSummary) {
        // The guest is a real MariaDB user. No login prompt is required.
        if (event.id <= 0 || event.id in savingEventIds) return
        val requestedOwner = account?.id ?: guestReadyUserId
        savingEventIds = savingEventIds + event.id
        work {
            try {
                val owner = requestedOwner ?: api.ensureGuestIdentity().also { created ->
                    if (account == null) guestReadyUserId = created
                }
                if ((account?.id ?: guestReadyUserId) != owner || api.activePersonalId() != owner) return@work
                val desired = event.id !in user.saved
                var queued = false
                try {
                    api.saveEvent(event, desired, chosenTowns.firstOrNull().orEmpty(),
                        chosenComarques.firstOrNull().orEmpty())
                } catch (_: IosQueuedForRetryException) {
                    queued = true // durable encrypted queue, NOT a MariaDB confirmation
                }
                if ((account?.id ?: guestReadyUserId) == owner && api.activePersonalId() == owner) {
                    savedEditRevision += 1
                    user = user.copy(saved = if (desired) user.saved + event.id else user.saved - event.id)
                    status = if (queued) (if (desired) "Guardat pendent de sincronització" else "Eliminació pendent de sincronització") else
                        if (desired) "Esdeveniment guardat al servidor" else "Esdeveniment eliminat dels guardats"
                    serverStateRefreshEpoch += 1
                }
            } finally {
                savingEventIds = savingEventIds - event.id
            }
        }
    }
    fun toggleReminder(event: AgendaEventSummary, next: Boolean, reminderAt: String = "") {
        val requestUserId = account?.id ?: guestReadyUserId
        if (requestUserId == null) { status = "S'està creant la identitat del convidat"; return }
        work {
            val epoch = if (next) {
                require(IosReminderPolicy.validLocalDateTime(reminderAt)) {
                    "Cal indicar data i hora (AAAA-MM-DD HH:MM)."
                }
                require(reminderAt.take(10) <= event.endDate.ifBlank { event.date }.take(10)) {
                    "El recordatori no pot ser posterior a l'activitat."
                }
                (IosNativeBridge.current?.reminderEpochMillis(reminderAt) ?: 0L).also {
                    IosReminderPolicy.requireFuture(event.id, true, it, currentEpochMillis())
                }
            } else 0L
            var queued = false
            try {
                api.configureEventReminder(event, next, epoch,
                    serverState?.takeIf { it.userId == requestUserId }?.central?.strings("event_ids"))
            } catch (_: IosQueuedForRetryException) {
                queued = true // Both the state mutation and bell operation are safely queued.
            }
            // A previous iOS installation could have an old local UNNotificationRequest.
            // The server now schedules the only authoritative reminder: avoid duplicate push.
            platformCancelReminder(requestUserId, event.id)
            if ((account?.id ?: guestReadyUserId) == requestUserId) {
                reminderTimeOverrides = reminderTimeOverrides + ((requestUserId to event.id) to
                    (if (next) reminderAt else ""))
                user = user.copy(alerts = if (next) user.alerts + event.id else user.alerts - event.id)
                if (!queued) serverStateRefreshEpoch += 1
                status = if (queued) "Recordatori pendent de sincronització amb el servidor"
                    else if (next) "Recordatori guardat al servidor" else "Recordatori desactivat"
            }
        }
    }
    /** Android v336: entity_type + entity_key is the identity, including for guests.
     * Distinct professional usernames must never be replaced by their public labels.
     */
    fun toggleFollowEntity(type: String, value: String, label: String = value) {
        val cleanLabel = label.trim().ifBlank { value.trim() }
        val cleanKey = resolvedFollowKey(type, value)
        if (IosFollowMutation.canonicalKey(cleanKey).isBlank() || type !in IosFollowMutation.supported) return
        val expectedId = account?.id ?: guestReadyUserId
        if (expectedId == null) {
            status = "S'està creant el teu usuari convidat al servidor"
            return
        }
        val expectedUsername = account?.username
        val actionId = "$type:${IosFollowMutation.canonicalKey(cleanKey)}"
        if (actionId in pendingFollows) return
        val next = !IosFollowMutation.contains(user.follows, type, cleanKey)
        pendingFollows = pendingFollows + actionId
        work {
            try {
                var queued = false
                try {
                    api.followEntity(type, cleanKey, cleanLabel, next)
                } catch (_: IosQueuedForRetryException) {
                    queued = true // Accepted into encrypted queue, NOT confirmed by MariaDB yet.
                }
                if ((account?.id ?: guestReadyUserId) != expectedId || account?.username != expectedUsername) return@work
                user = user.copy(follows = IosFollowMutation.updated(user.follows, type, cleanKey, cleanLabel, next))
                followEditRevision += 1
                if (!queued) serverStateRefreshEpoch += 1
                status = if (queued) "Seguiment pendent de sincronitzar" else
                    if (next) "Ara seguixes $cleanLabel" else "Has deixat de seguir $cleanLabel"
                // The returned server snapshot (or pending overlay) is authoritative at next refresh.
            } finally {
                pendingFollows = pendingFollows - actionId
            }
        }
    }
    fun toggleFollow(value: String) = toggleFollowEntity("ARTIST", value)

    MaterialTheme(colorScheme = lightColorScheme(primary = Ink, background = Color.White)) {
        Column(Modifier.fillMaxSize().background(Color.White)) {
            // Android parity: same horizontal logo and controls, stable symmetric layout.
            Row(Modifier.fillMaxWidth().height(82.dp).padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                AgendaTopAction("person", onClick = { tab = "Perfil" })
                AgendaTopAction("plus", color = AgendaGold, onClick = {
                    // Android v336: + opens PublishActionsSheet, for guests as well.
                    publisherMenuOpen = true
                })
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    AgendaBrandLogo(Modifier.fillMaxWidth().widthIn(max = 175.dp).height(58.dp))
                }
                AgendaTopAction("sliders", onClick = { prefOpen = true })
                AgendaTopAction("bell", onClick = {
                    notificationsOpen = true
                    if (account != null) loadAccountPages()
                }, badge = notices.count { !it.read })
            }
            if (error.isNotBlank()) {
                Surface(color = Color(0xFFFFEFEF), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(error, modifier = Modifier.weight(1f), color = Color(0xFF9D2525), fontSize = 12.sp)
                        TextButton(onClick = { error = "" }) { Text("×") }
                    }
                }
            }
            if (status.isNotBlank()) {
                LaunchedEffect(status) { kotlinx.coroutines.delay(3000); status = "" }
                Text(status, modifier = Modifier.fillMaxWidth().background(Accent).padding(horizontal = 15.dp, vertical = 5.dp), fontSize = 12.sp)
            }
            Box(Modifier.weight(1f)) {
                when (tab) {
                    "Inici" -> {
                        Column(Modifier.fillMaxSize()) {
                            // Search comes FIRST (as on Android), before header ad and batecs.
                            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                                Row(Modifier.weight(1f).height(52.dp)
                                    .border(1.dp, Color(0xFFE1E1E1), RoundedCornerShape(15.dp))
                                    .padding(start = 12.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    AgendaGlyph("search", Modifier.size(22.dp), AgendaGold)
                                    OutlinedTextField(value = query, onValueChange = {
                                        query = it; chosenSuggestion = null
                                    }, singleLine = true,
                                        placeholder = { Text("Grup, poble, empresa, comarca...", fontSize = 12.sp, maxLines = 1) },
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedBorderColor = Color.Transparent,
                                            focusedBorderColor = Color.Transparent),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                        keyboardActions = KeyboardActions(onSearch = {
                                            appliedQuery = query.trim(); appliedSuggestion = chosenSuggestion
                                            limit = 5; recordSearchOnServer(appliedQuery)
                                        }))
                                }
                                Row(Modifier.height(52.dp).background(Color(0xFFF2F2F2), RoundedCornerShape(14.dp))
                                    .clickable { searchFiltersOpen = !searchFiltersOpen }
                                    .padding(horizontal = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                                    AgendaGlyph("sliders", Modifier.size(20.dp), AgendaGold)
                                    Text("Opcions", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                }
                            }
                            if (query.isNotBlank()) {
                                TextButton(onClick = {
                                    appliedQuery = query.trim(); appliedSuggestion = chosenSuggestion
                                    limit = 5; recordSearchOnServer(appliedQuery)
                                }, modifier = Modifier.align(Alignment.End)) { Text("Cercar") }
                            }
                            if (suggestions.isNotEmpty() && query.trim() == suggestionDraft && chosenSuggestion == null && tab == "Inici") {
                                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                                    .heightIn(max = 216.dp).verticalScroll(rememberScrollState())) {
                                    suggestions.forEach { suggestion ->
                                        Row(Modifier.fillMaxWidth().clickable {
                                            chosenSuggestion = suggestion
                                            query = suggestion.value
                                            appliedQuery = suggestion.value
                                            appliedSuggestion = suggestion
                                            suggestionDraft = ""
                                            limit = 5
                                            recordSearchOnServer(suggestion.value)
                                        }.padding(vertical = 7.dp, horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(when(suggestion.type) {
                                                IosSearchType.ARTIST -> "♫"
                                                IosSearchType.TOWN -> "⌖"
                                                IosSearchType.COMARCA -> "▱"
                                                IosSearchType.ORGANIZER -> "▦"
                                                IosSearchType.VENUE -> "⌂"
                                                IosSearchType.STYLE -> "#"
                                                IosSearchType.CATEGORY -> "◇"
                                                IosSearchType.EVENT -> "▤"
                                            }, modifier = Modifier.width(28.dp))
                                            Column {
                                                Text(suggestion.value, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                                Text(suggestion.subtitle, color = Color.Gray, fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                            EventList(visibleFeed, personalizedFeed.size,
                                headerContent = {
                            // Contractual HEADER creative before batecs, as Android. No duplicate in LazyColumn.
                            SponsoredCarousel("Capçalera", paid.ads.filter { it.placement == "HEADER" },
                                eventById, ::adViewed, ::adOpened, header = true)
                            if ((account != null || guestReadyUserId != null) && followedGroups.size >= 6) {
                                LazyRow(
                                    state = batecRowState,
                                    modifier = Modifier.fillMaxWidth()
                                        .pointerInput(Unit) {
                                            awaitEachGesture {
                                                awaitFirstDown(requireUnconsumed = false)
                                                keepBatecStripAtStart = false
                                            }
                                        }
                                        .padding(top = 2.dp, bottom = 5.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(followedGroups, key = { it.key }) { group ->
                                        val cover = batecs.firstOrNull { it.storyKey == group.openingStoryKey }
                                        BatecBubble(
                                            group = group,
                                            photoUrl = cover?.imageUrl.orEmpty().ifBlank {
                                                cover?.eventId?.let { eventById[it]?.posterUrl }.orEmpty()
                                            },
                                            onClick = { openBatecs(group.key) }
                                        )
                                    }
                                }
                            }
                            // Android feed tabs and total: modes do not displace the search box.
                            Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 5.dp, bottom = 3.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                var modeMenu by remember { mutableStateOf(false) }
                                Box {
                                    Surface(shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFF4F4F4),
                                        modifier = Modifier.clickable { modeMenu = true }) {
                                        Text("$feedMode ▾", modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    }
                                    DropdownMenu(expanded = modeMenu, onDismissRequest = { modeMenu = false }) {
                                        listOf("Per a tu", "Sense filtre").forEach { mode ->
                                            DropdownMenuItem(text = { Text(mode) }, onClick = {
                                                feedMode = mode; limit = 5; modeMenu = false
                                            })
                                        }
                                    }
                                }
                                Text("Seguits", modifier = Modifier.clickable {
                                    feedMode = "Per a tu"; limit = 5
                                }, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text("Categoria ▾", modifier = Modifier.clickable { searchFiltersOpen = true }, fontSize = 13.sp)
                                Spacer(Modifier.weight(1f))
                                Text("${personalizedFeed.size} esdev.", color = Color.Gray, fontSize = 11.sp)
                            }
                            if (searchInProgress || query.isNotBlank()) {
                                TextButton(onClick = {
                                    query = ""; appliedQuery = ""; chosenSuggestion = null; appliedSuggestion = null
                                    searchTown = ""; searchComarca = ""; searchRadius = 0
                                    searchCategory = ""; searchStyle = ""; searchFromDate = ""; searchToDate = ""; searchMonth = "";
                                    searchPriceLimit = null; searchFreeOnly = false; searchCategories = emptySet()
                                    appliedSearchFilters = IosSearchFilters(); limit = 5
                                }) { Text("Netejar cerca") }
                            }
                            if (searchFiltersOpen) {
                                Dialog(onDismissRequest = { searchFiltersOpen = false }) {
                                Surface(shape = RoundedCornerShape(18.dp), color = Color.White) {
                                Column(Modifier.fillMaxWidth().heightIn(max = 690.dp)
                                    .verticalScroll(rememberScrollState()).padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically) {
                                        Text("Filtres de cerca", fontWeight = FontWeight.Bold, fontSize = 19.sp)
                                        TextButton(onClick = { searchFiltersOpen = false }) { Text("Tancar") }
                                    }
                                    OutlinedTextField(searchTown, { searchTown = it; territoryTownChosen = false; if (it.isBlank()) searchRadius = 0 },
                                        label = { Text("On? Poble") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                    if (searchTown.length >= 2) {
                                        if (territoryTownDraft == searchTown) townSuggestions.forEach { town ->
                                            TextButton(onClick = { searchTown = town.value; territoryTownChosen = true; territoryTownDraft = "" }) {
                                                Column(horizontalAlignment = Alignment.Start) {
                                                    Text("⌖ ${town.value}")
                                                    if (town.subtitle.isNotBlank()) Text(town.subtitle, fontSize = 10.sp, color = Color.Gray)
                                                }
                                            }
                                        }
                                    }
                                    OutlinedTextField(searchComarca, { searchComarca = it; territoryComarcaChosen = false },
                                        label = { Text("Comarca") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                    if (searchComarca.length >= 2) {
                                        if (territoryComarcaDraft == searchComarca) comarcaSuggestions.forEach { comarca ->
                                            TextButton(onClick = { searchComarca = comarca.value; territoryComarcaChosen = true; territoryComarcaDraft = "" }) {
                                                Text("▱ ${comarca.value}")
                                            }
                                        }
                                    }
                                    Text("Radi des del poble (0 km = només el poble)", fontSize = 12.sp)
                                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(0,5,15,30,50,100).forEach { radius ->
                                            FilterChip(selected = searchRadius == radius, enabled = searchTown.isNotBlank() || radius == 0,
                                                onClick = { searchRadius = radius }, label = { Text("$radius km") })
                                        }
                                    }
                                    Text("Categories (pots marcar-ne diverses)", fontSize = 12.sp)
                                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                        FilterChip(selected = searchCategories.isEmpty() && searchCategory.isBlank(),
                                            onClick = { searchCategories = emptySet(); searchCategory = "" }, label = { Text("Totes") })
                                        categoriesForSearch.forEach { category ->
                                            val code = IosCategoryTaxonomy.mainKey(category)
                                            if (code != null) FilterChip(selected = code in searchCategories,
                                                onClick = { searchCategories = if (code in searchCategories) searchCategories - code
                                                    else searchCategories + code; searchCategory = "" }, label = { Text(category) })
                                        }
                                    }
                                    OutlinedTextField(searchStyle, { searchStyle = it }, label = { Text("Estil / subcategoria") },
                                        singleLine = true, modifier = Modifier.fillMaxWidth())
                                    if (searchStyle.length >= 2) stylesForSearch.asSequence().filter { it.contains(searchStyle,true) }
                                        .take(6).forEach { style -> TextButton(onClick = { searchStyle = style }) { Text("# $style") } }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(searchFromDate, { searchFromDate = it.take(10) },
                                            label = { Text("Des de (AAAA-MM-DD)") }, singleLine = true, modifier = Modifier.weight(1f))
                                        OutlinedTextField(searchToDate, { searchToDate = it.take(10) },
                                            label = { Text("Fins a (AAAA-MM-DD)") }, singleLine = true, modifier = Modifier.weight(1f))
                                    }
                                    OutlinedTextField(searchMonth, { searchMonth = it.take(7) },
                                        label = { Text("Mes (AAAA-MM)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                    Text("Preu de l'entrada", fontSize = 12.sp)
                                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(null, 10, 15, 20).forEach { price ->
                                            FilterChip(selected = !searchFreeOnly && searchPriceLimit == price,
                                                onClick = { searchFreeOnly = false; searchPriceLimit = price },
                                                label = { Text(if (price == null) "Qualsevol" else "≤ $price €") })
                                        }
                                        FilterChip(selected = searchFreeOnly, onClick = {
                                            searchFreeOnly = !searchFreeOnly; if (searchFreeOnly) searchPriceLimit = null
                                        }, label = { Text("Gratuït") })
                                    }
                                    val draft = IosSearchFilters(searchTown.trim(), searchComarca.trim(), searchRadius,
                                        searchCategory.trim(), searchStyle.trim(), searchFromDate.trim(), searchToDate.trim(),
                                        categoryCodes = searchCategories, month = searchMonth.trim(),
                                        maxPrice = searchPriceLimit, freeOnly = searchFreeOnly)
                                    if (!draft.valid()) Text("Revisa les dates, el mes i el radi; per a la distància cal un poble.",
                                        color = Color.Red, fontSize = 12.sp)
                                    OutlinedTextField(newSearchName, { newSearchName = it.take(52) },
                                        label = { Text("Nom per a guardar els filtres") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                    if (searchStyle.isNotBlank()) Text("L'estil s'aplica a la cerca, però Android no el pot guardar en les seues llistes. Lleva'l abans de guardar.",
                                        color = Color(0xFF934F00), fontSize = 11.sp)
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(modifier = Modifier.weight(1f), enabled = draft.valid(), onClick = {
                                            appliedSearchFilters = draft; appliedQuery = query.trim(); appliedSuggestion = chosenSuggestion
                                            searchFiltersOpen = false; limit = 5
                                        }) { Text("Aplicar") }
                                        OutlinedButton(modifier = Modifier.weight(1f),
                                            enabled = draft.valid() && draft.isActive && newSearchName.trim().isNotBlank() &&
                                                !searchPresetSaving && searchStyle.isBlank() &&
                                                (account?.id ?: guestReadyUserId) != null,
                                            onClick = {
                                                val owner = account?.id ?: guestReadyUserId
                                                searchPresetRevision += 1 // invalidate in-flight GETs before mutating
                                                val revisionBefore = searchPresetRevision
                                                val name = newSearchName.trim()
                                                searchPresetSaving = true
                                                work {
                                                    try {
                                                        val confirmed = api.saveSearch(name, draft)
                                                        if ((account?.id ?: guestReadyUserId) == owner &&
                                                            searchPresetRevision == revisionBefore) {
                                                            searchPresets = confirmed; searchPresetRevision += 1
                                                            newSearchName = ""; status = "Filtres guardats al servidor"
                                                            appliedSearchFilters = draft.copy(preferredTowns = chosenTowns)
                                                            appliedQuery = query.trim(); appliedSuggestion = chosenSuggestion
                                                            searchFiltersOpen = false; limit = 5
                                                        }
                                                    } finally {
                                                        if ((account?.id ?: guestReadyUserId) == owner) searchPresetSaving = false
                                                    }
                                                }
                                            }) { Text(if (searchPresetSaving) "Guardant…" else "Guardar") }
                                    }
                                    Text("Els filtres restringixen els resultats de la cerca; la publicitat conserva els seus espais.",
                                        fontSize = 10.sp, color = Color.Gray)
                                    if (searchPresets.isNotEmpty()) Text("Cerques guardades", fontWeight = FontWeight.SemiBold)
                                    searchPresets.forEach { preset ->
                                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                            TextButton(onClick = {
                                                val f = preset.filters
                                                searchTown = f.town; searchComarca = f.comarca; searchRadius = f.radiusKm
                                                searchCategory = f.category; searchCategories = f.categoryCodes
                                                searchStyle = ""; searchFromDate = f.fromDate; searchToDate = f.toDate
                                                searchMonth = f.month; searchPriceLimit = f.maxPrice; searchFreeOnly = f.freeOnly
                                                appliedSearchFilters = f.copy(preferredTowns = chosenTowns); appliedQuery = query.trim(); appliedSuggestion = chosenSuggestion
                                                searchFiltersOpen = false; limit = 5
                                            }, modifier = Modifier.weight(1f)) { Text(preset.name, maxLines = 1) }
                                            TextButton(onClick = {
                                                val owner = account?.id ?: guestReadyUserId
                                                searchPresetRevision += 1 // invalidate in-flight GETs
                                                val before = searchPresetRevision
                                                work {
                                                    val confirmed = api.removeSavedSearch(preset.name)
                                                    if ((account?.id ?: guestReadyUserId) == owner && before == searchPresetRevision) {
                                                        searchPresets = confirmed; searchPresetRevision += 1
                                                        status = "Llista de filtres eliminada del servidor"
                                                    }
                                                }
                                            }) { Text("Eliminar") }
                                        }
                                    }
                                }
                                }
                                }
                            }
                            if (searchInProgress) {
                                Text("Filtres actius · ${filtered.size} resultats orgànics", fontSize = 11.sp,
                                    color = Color.DarkGray, modifier = Modifier.padding(horizontal = 16.dp))
                            }
                            if (feedMode == "Per a tu" && (account != null || guestReadyUserId != null) && !user.follows.any { it.type == "ARTIST" } && chosenCategories.isEmpty()) {
                                Text("Seguix artistes o tria categories a Personalització.", modifier = Modifier.padding(horizontal = 16.dp), fontSize = 13.sp)
                            }
                            if (manualRefreshing) LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().height(2.dp), color = Ink, trackColor = Accent)
                                },
                                onLoadMore = {
                                    limit = IosFeedReadingPolicy.page(limit, personalizedFeed.size)
                                },
                                today = today,
                                scrollContext = readingViewKey,
                                restoreReading = readingOwner?.let {
                                    readingMemory.restore(it, readingViewKey, currentEpochMillis())
                                },
                                readingRestoreEpoch = readingRestoreEpoch,
                                onReadingPosition = { index, offset, settled ->
                                    val owner = readingOwner
                                    if (owner != null && tab == "Inici") {
                                        if (index != 0 || offset != 0 || readingMemory.initialized(owner)) {
                                            readingMemory.record(owner, readingViewKey, limit, index, offset, currentEpochMillis())
                                            if (settled) readingPersistEpoch += 1
                                        }
                                    }
                                },
                                savedIds = user.saved,
                                artistSearch = searchedArtist.orEmpty(),
                                festivalSchedules = artistFestivalSchedule,
                                onClick = { showEvent(it, searchedArtist.orEmpty()) }, onSave = ::toggleSaved,
                                ads = paid.ads, protectedIds = paid.guaranteedEventIds,
                                commercialEvents = eventById,
                                onAdViewed = ::adViewed, onAdOpened = ::adOpened,
                                refreshing = manualRefreshing, onRefresh = {
                                    if (!updating) {
                                    // Explicit gesture bypasses the 30-minute catalog cache.
                                    // Recomposition/automatic updates never reset reading.
                                    readingMemory.discard()
                                    limit = IosFeedReadingPolicy.initial
                                    readingRestoreEpoch += 1
                                    refresh(true)
                                    }
                                },
                                returnToStartEpoch = batecsReturnEpoch,
                                onExclude = ::excludeFromFeed, onReport = { reportEvent = it })
                        }
                    }
                    "Guardats" -> {
                        val savedOwnerId = account?.id ?: guestReadyUserId
                        if (savedOwnerId == null) {
                            Column(Modifier.fillMaxSize()) {
                                PageTitle("Les meues agendes")
                                Text("Recuperant el compte de convidat…", modifier = Modifier.padding(16.dp))
                            }
                        } else {
                            val expectedAccountId = savedOwnerId
                            key(expectedAccountId) {
                                IosSavedAgendaScreen(
                                    accountId = expectedAccountId,
                                    user = user,
                                    lists = savedLists,
                                    available = savedCatalog,
                                    today = savedCulturalToday,
                                    api = api,
                                    work = ::work,
                                    onListsChanged = { result -> if ((account?.id ?: guestReadyUserId) == expectedAccountId) {
                                        savedListsRevision += 1; savedLists = result
                                    } },
                                    onStatus = { status = it },
                                    onOpenEvent = { event, matchedArtist -> showEvent(event, matchedArtist) },
                                    onToggleSaved = ::toggleSaved,
                                    onDisableReminder = { alertToDisable = it },
                                    reminderTime = { event -> remoteReminderTime(event.id) },
                                    festivalRevision = savedFestivalRevision,
                                    remoteFestivalSlots = { eventId ->
                                        festivalSlotOverrides[expectedAccountId to eventId]
                                            ?: serverState?.personalFestivalSlots(eventId).orEmpty()
                                    },
                                    onSaveFestivalSlots = { eventId, next ->
                                        updateFestivalSlots(expectedAccountId, eventId, next)
                                    }
                                )
                            }
                        }
                    }
                    "Entrades" -> {
                        Column(Modifier.fillMaxSize()) {
                            PageTitle("Les meues entrades")
                            if (account == null && guestReadyUserId == null &&
                                !api.hasRegisteredSessionToken() && api.guestUserId() == null) {
                                Text("Preparant el perfil de convidat del servidor…", modifier = Modifier.padding(16.dp))
                            } else {
                                TextButton(onClick = { work { reloadMyTickets() } },
                                    modifier = Modifier.padding(horizontal = 12.dp)) { Text("↻ Actualitzar entrades") }
                                if (ticketsOffline) Text(ticketLoadNotice, modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                else if (ticketLoadNotice.isNotBlank()) Text(ticketLoadNotice,
                                    modifier = Modifier.padding(horizontal = 16.dp), fontSize = 12.sp)
                                if (tickets.isEmpty()) EmptyState("No hi ha entrades descarregades per a este compte.")
                                else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(tickets, key = { it.id }) { ticket ->
                                        ElevatedCard(Modifier.fillMaxWidth()) {
                                            Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(ticket.eventTitle.ifBlank { ticket.eventId.toIntOrNull()?.let { eventById[it]?.title }.orEmpty() }.ifBlank { "Entrada ${ticket.id}" }, fontWeight = FontWeight.Bold)
                                                Text("Estat: ${IosOfflineTicketPolicy.displayStatus(ticket.status)}" +
                                                    if (ticketsOffline) " (última còpia)" else "", fontSize = 13.sp)
                                                Text("Número: ${ticket.id}", fontSize = 12.sp)
                                                val details = IosOfflineTicketPolicy.details(ticket)
                                                if (details.isNotBlank()) Text(details, fontSize = 12.sp)
                                                if (ticket.status.equals("USED", true) && ticket.validatedAt.isNotBlank()) {
                                                    Text("Utilitzada: ${ticket.validatedAt}", fontSize = 12.sp)
                                                }
                                                if (ticket.code.isNotBlank()) {
                                                    Text("Codi: ${ticket.code}", fontSize = 12.sp)
                                                    Button(onClick = { showIssuedTicketQr(ticket.code, ticket.eventTitle.ifBlank {
                                                        ticket.eventId.toIntOrNull()?.let { eventById[it]?.title }.orEmpty().ifBlank { "Entrada" }
                                                    }) }) { Text("Mostrar QR de l'entrada") }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "Comunitat" -> {
                        if ((account?.id ?: guestReadyUserId) == null) EmptyState("Connectant el perfil de convidat…")
                        else IosCommunityHub(groups, api, currentUserId = account?.id ?: guestReadyUserId ?: 0,
                            isAdmin = IosCommunityPolicy.canSeeAdminQueue(account?.role.orEmpty()),
                            reload = { mine -> work { groups = api.community(mine=mine) } },
                            onOpen = { communityGroup = it })
                    }
                    "Perfil" -> {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Perfil", fontWeight = FontWeight.Bold, fontSize = 26.sp)
                            if (account == null) {
                                val serverGuestId = guestReadyUserId
                                Text(if (serverGuestId == null) "Connectant el perfil al servidor…"
                                    else "Convidat · ID de servidor: $serverGuestId", fontWeight = FontWeight.Medium)
                                Text("Els guardats, seguiments, campanetes i preferències del convidat es desen al servidor.", fontSize = 13.sp)
                                OutlinedButton(onClick = { prefOpen = true }, enabled = serverGuestId != null) {
                                    Text("Preferències i personalització")
                                }
                                OutlinedButton(onClick = { notificationsOpen = true }, enabled = serverGuestId != null) {
                                    Text("Notificacions i recordatoris")
                                }
                                OutlinedButton(onClick = {
                                    serverStateRefreshEpoch += 1
                                    refresh(true)
                                }, enabled = serverGuestId != null) { Text("Actualitzar dades del servidor") }
                                Button(onClick = { loginOpen = true }) { Text("Iniciar sessió o crear compte") }
                                Text("El nom públic, el correu i la fotografia només són editables després de registrar el compte. No es crea un perfil públic fictici per al convidat.", fontSize = 12.sp, color = Color.Gray)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Box(Modifier.size(72.dp).clip(CircleShape).background(Accent), contentAlignment = Alignment.Center) {
                                        if (!myProfile?.photoUrl.isNullOrBlank())
                                            AsyncImage(model = myProfile?.photoUrl, contentDescription = "Foto del meu perfil", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                        else Text(account?.username.orEmpty().take(1).uppercase(), fontSize = 27.sp)
                                    }
                                    Column {
                                        Text(myProfile?.displayName?.ifBlank { account?.username.orEmpty() } ?: account?.username.orEmpty(), fontWeight = FontWeight.Bold)
                                        Text("@${account?.username.orEmpty()}", fontSize = 12.sp)
                                        Text(account?.email.orEmpty(), fontSize = 12.sp)
                                    }
                                }
                                OutlinedButton(onClick = {
                                    val expectedId = account?.id
                                    work {
                                        val loaded = api.myPublicProfile()
                                        if (account?.id == expectedId) { myProfile = loaded; editProfileOpen = true }
                                    }
                                }) { Text("Editar foto i perfil públic") }
                                OutlinedButton(onClick = { editAccountOpen = true }) {
                                    Text("Usuari, correu i població")
                                }
                                if (!myProfile?.homeTown.isNullOrBlank()) {
                                    Text("Població: ${myProfile?.homeTown}", fontSize = 12.sp)
                                }
                                Text("Rol: ${serverPublisherAccess?.role ?: account?.role} · ${if (serverPublisherAccess != null && IosPublisherRoles.isProfessional(serverPublisherAccess!!)) "Professional verificat" else "Permisos pendents de consulta"}", fontSize = 12.sp)
                                Text("Artistes seguits: ${user.follows.count { it.type == "ARTIST" }}")
                                Button(onClick = {
                                    loadAccountPages()
                                    serverStateRefreshEpoch += 1
                                }) { Text("Actualitzar dades") }
                                if (account?.role != "SUPER_ADMIN") {
                                    OutlinedButton(onClick = {
                                        val expectedId = account?.id
                                        work {
                                            val settings = api.profileSettings()
                                            if (expectedId != null && account?.id == expectedId) {
                                                profileSettings = settings
                                                profileSettingsOpen = true
                                            }
                                        }
                                    }) { Text("Tipus de compte i dades professionals") }
                                }
                                OutlinedButton(onClick = { deleteAccountOpen = true }) { Text("Eliminar el meu compte") }
                                OutlinedButton(onClick = { work {
                                    val leavingAccountId = account?.id
                                    api.signOutSecurely() // clears local credentials even if remote logout is offline
                                    sessionBridge?.eraseLocalAccountData(leavingAccountId ?: 0)
                                    account = null; myProfile = null; user = IosUserState(); artistBells = emptyMap(); tickets = emptyList()
                                    notices = emptyList(); groups = emptyList(); seenBatecs = emptySet(); savedLists = emptyList(); searchPresets = emptyList(); searchTown = ""; searchCategory = ""; searchComarca = ""; searchStyle = ""; searchRadius = 0; searchFromDate = ""; searchToDate = ""; searchMonth = ""; searchPriceLimit = null; searchFreeOnly = false; searchCategories = emptySet(); searchPresetRevision += 1; searchPresetSaving = false; appliedSearchFilters = IosSearchFilters(); appliedQuery = ""; query = ""; appliedSuggestion = null; chosenSuggestion = null; newSearchName = ""
                                    useAccountPreferences(null)
                                    // Recover the existing guest (or issue one when first needed).
                                    // Account claim/merge is deliberately deferred to step 3.
                                    serverState = null
                                    guestReadyUserId = runCatching { api.ensureGuestIdentity() }.getOrNull()
                                    serverStateRefreshEpoch += 1
                                    status = "Sessió tancada"
                                } }) { Text("Tancar sessió") }
                                if (serverPublisherAccess?.let(IosPublisherRoles::isProfessional) == true) {
                                    Divider()
                                    Button(onClick = { validatorOpen = true }) { Text("Validar entrades (PRO)") }
                                    Text("El servidor comprova els permisos de cada esdeveniment.", fontSize = 11.sp, color = Color.Gray)
                                }
                                if (serverPublisherAccess?.let { IosPublisherRoles.isSuperAdmin(it.role) && it.serverConfirmed } == true) {
                                    Divider()
                                    IosSuperAdminConsole(api, actorId = account?.id ?: 0, initialSection = adminDestination, onMessage = { status = it })
                                }
                            }
                            if ((account != null || guestReadyUserId != null) && user.follows.isNotEmpty()) {
                                HorizontalDivider()
                                Text("Els meus seguiments", fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 10.dp, bottom = 5.dp))
                                user.follows.distinctBy { it.type + "|" + it.key }.forEach { item ->
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text("${when (item.type.uppercase()) { "ARTIST" -> "Artista"; "VENUE" -> "Sala"; "ORGANIZER" -> "Organització"; "CATEGORY" -> "Categoria"; else -> item.type }} · ${item.label}",
                                            fontSize = 12.sp, modifier = Modifier.weight(1f))
                                        TextButton(onClick = { toggleFollowEntity(item.type.uppercase(), item.key.ifBlank { item.label }) }) {
                                            Text("Deixar de seguir")
                                        }
                                    }
                                }
                            }
                            Divider()
                            Button(onClick = { prefOpen = true }) { Text("Personalització") }
                            OutlinedButton(onClick = { IosNativeBridge.current?.showNotificationSettings() }) {
                                Text("Configurar notificacions d'iPhone")
                            }
                            Text("Versió iOS en migració · L'Agenda KMP", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().height(62.dp).background(Color(0xFFF1F1F1))
                .padding(horizontal = 7.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf("Inici" to "home", "Guardats" to "bookmark", "Entrades" to "ticket",
                    "Comunitat" to "people").forEach { (name, symbol) ->
                    Row(Modifier.weight(1f).fillMaxHeight()
                        .background(if (tab == name) Color(0xFFDCDCDC) else Color.Transparent,
                            RoundedCornerShape(12.dp))
                        .clickable { tab = name }.padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        AgendaGlyph(symbol, Modifier.size(19.dp),
                            when (name) { "Guardats", "Entrades" -> Color(0xFFCF4547)
                                "Comunitat" -> Color(0xFF287BBF); else -> AgendaBlack })
                        Spacer(Modifier.width(5.dp))
                        Text(name, fontSize = 11.sp, maxLines = 1,
                            fontWeight = if (tab == name) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
        reportEvent?.let { event -> EventReportDialog(event, onDismiss = { reportEvent = null },
            onSend = { reason, details ->
                work {
                    api.reportEvent(event, reason, details)
                    reportEvent = null
                    status = "Incidència enviada per a revisió"
                }
            }) }
        if (resetOpen) PasswordResetDialog(onDismiss = { resetOpen = false }, onSubmit = { email ->
            work { status = api.requestPasswordReset(email); resetOpen = false }
        })
        if (registrationOpen) RegistrationDialog(onDismiss = { registrationOpen = false }, onSubmit = { username, email, password ->
            work {
                account = api.registerAccount(username, email, password)
                serverState = null
                user = IosUserState(); artistBells = emptyMap(); tickets = emptyList(); ticketsOffline = false; ticketLoadNotice = ""
                        notices = emptyList(); groups = emptyList(); seenBatecs = emptySet(); savedLists = emptyList(); searchPresets = emptyList(); searchTown = ""; searchCategory = ""; searchComarca = ""; searchStyle = ""; searchRadius = 0; searchFromDate = ""; searchToDate = ""; searchMonth = ""; searchPriceLimit = null; searchFreeOnly = false; searchCategories = emptySet(); searchPresetRevision += 1; searchPresetSaving = false; appliedSearchFilters = IosSearchFilters(); appliedQuery = ""; query = ""; appliedSuggestion = null; chosenSuggestion = null; newSearchName = ""
                useAccountPreferences(account?.id)
                registrationOpen = false; loginOpen = false; status = "Compte creat"
                loadAccountPages()
                myProfile = runCatching { api.myPublicProfile() }.getOrNull()
            }
        })
        if (loginOpen) LoginDialog(onDismiss = { loginOpen = false }, onSocial = { provider ->
            sessionBridge?.beginSocialSignIn(provider, object : IosSocialAuthCallback {
                override fun onComplete(idToken: String, nonce: String, authError: String) {
                    if (authError.isNotBlank()) { error = authError; return }
                    // A cancelled login dialog must not complete into an unrelated session.
                    if (!loginOpen || account != null) return
                    work {
                        check(loginOpen && account == null) { "La sessió ha canviat; repetix l'accés." }
                        val next = api.signInWithProvider(provider, idToken, nonce)
                        // A late provider response must not silently sign in after dismissal.
                        if (!loginOpen) { api.signOutSecurely(); return@work }
                        account = next
                        serverState = null
                        user = IosUserState(); artistBells = emptyMap(); tickets = emptyList(); notices = emptyList()
                        groups = emptyList(); seenBatecs = emptySet(); savedLists = emptyList(); searchPresets = emptyList()
                        searchTown = ""; searchCategory = ""; searchComarca = ""; searchStyle = ""
                        searchRadius = 0; searchFromDate = ""; searchToDate = ""; searchMonth = ""
                        searchPriceLimit = null; searchFreeOnly = false; searchCategories = emptySet()
                        searchPresetRevision += 1; searchPresetSaving = false
                        appliedSearchFilters = IosSearchFilters(); appliedQuery = ""; query = ""
                        appliedSuggestion = null; chosenSuggestion = null; newSearchName = ""
                        myProfile = null; guestReadyUserId = null; useAccountPreferences(next.id)
                        loginOpen = false; status = "Sessió iniciada amb $provider"
                        loadAccountPages()
                        runCatching { myProfile = api.myPublicProfile() }
                    }
                }
            }) ?: run { error = "No hi ha disponible el pont d'autenticació d'iPhone." }
        }, onRegister = {
            loginOpen = false; registrationOpen = true
        }, onForgotPassword = {
            loginOpen = false; resetOpen = true
        }, onSubmit = { login, password ->
            work {
                account = api.signIn(login, password)
                serverState = null
                user = IosUserState(); artistBells = emptyMap(); tickets = emptyList(); ticketsOffline = false; ticketLoadNotice = ""
                        notices = emptyList(); groups = emptyList(); seenBatecs = emptySet(); savedLists = emptyList(); searchPresets = emptyList(); searchTown = ""; searchCategory = ""; searchComarca = ""; searchStyle = ""; searchRadius = 0; searchFromDate = ""; searchToDate = ""; searchMonth = ""; searchPriceLimit = null; searchFreeOnly = false; searchCategories = emptySet(); searchPresetRevision += 1; searchPresetSaving = false; appliedSearchFilters = IosSearchFilters(); appliedQuery = ""; query = ""; appliedSuggestion = null; chosenSuggestion = null; newSearchName = ""
                useAccountPreferences(account?.id)
                loginOpen = false; status = "Sessió iniciada"
                loadAccountPages()
                myProfile = runCatching { api.myPublicProfile() }.getOrNull()
            }
        })
        if (prefOpen) PersonalizationDialog(chosenCategories, chosenTowns, excludedEventIds, excludedArtists,
            excludedCategories, available, IosCategoryTaxonomy.categories.map { it.key },
            events.map { it.town }.filter(String::isNotBlank).distinct().sorted(),
            chosenSubcategories, chosenComarques, chosenRadii, showSuggestions,discoveryLevel,valencianFirst,
            uiLanguage, onDismiss = { prefOpen = false }, onSave = { cats, towns, hiddenEvents, hiddenArtists, hiddenCategories,
                styles,comarques,radii,suggestions,discovery,valencian,language ->
                val expectedId = account?.id ?: guestReadyUserId
                val username = account?.username?.trim()?.lowercase()?.takeIf(String::isNotBlank) ?: "guest"
                if (expectedId == null) {
                    error = "Cal identificar l'usuari al servidor abans de guardar."
                } else {
                    // Protect the current edit against older parallel server refreshes.
                    locationEditRevision += 1
                    feedEditRevision += 1
                    work {
                    val normalizedLocations = IosLocationRules.prepare(towns, comarques, radii)
                    val savedTowns = normalizedLocations.towns.keys
                    val savedComarques = normalizedLocations.comarques
                    val savedRadii = normalizedLocations.towns
                    // Dedicated domains first; do not mark UI saved until ALL calls succeed.
                    api.saveLocationPreferences(expectedId, savedTowns, savedComarques, savedRadii)
                    api.saveFeedPreferences(expectedId, suggestions, discovery.name, valencian)
                    api.savePersonalState(expectedId, mapOf(
                        "preferred_locations_$username" to IosPersonalEncoded.strings(savedTowns),
                        "preferred_comarques_$username" to IosPersonalEncoded.strings(savedComarques),
                        "preferred_location_radii_$username" to IosPersonalEncoded.string(
                            kotlinx.serialization.json.buildJsonObject {
                                savedRadii.forEach { (town, radius) -> put(town, radius) }
                            }.toString()),
                        "show_suggestions" to IosPersonalEncoded.bool(suggestions),
                        "discovery_level" to IosPersonalEncoded.string(discovery.name),
                        "valencian_first" to IosPersonalEncoded.bool(valencian),
                        // Android UiLanguageStore writes exactly this key to sync/state.php.
                        IosFeedPreferencesContract.centralUiLanguageKey to IosPersonalEncoded.string(language.code)
                    ))
                    val exclusionsPending = api.updateVisibility(expectedId,
                        IosVisibilitySnapshot(excludedEventIds, excludedArtists, excludedCategories),
                        IosVisibilitySnapshot(hiddenEvents, hiddenArtists, hiddenCategories))
                    visibilityEditRevision += 1
                    // Pas 7: MAIN and SUBCATEGORY share the *same* Android CATEGORY follows.
                    // Never compare entity_label; only exact entity_key (legacy aliases are removed).
                    val hiddenMain = hiddenCategories.mapNotNull(IosCategoryTaxonomy::mainKey).toSet()
                    val hiddenSub = hiddenCategories.mapNotNull(IosCategoryTaxonomy::subcategoryCode).toSet()
                    val desiredCategoryFollows = IosCategoryTaxonomy.normalized(
                        cats - hiddenMain,
                        styles.filterNot { child ->
                            val canonical = IosCategoryTaxonomy.subcategoryCode(child)
                            canonical in hiddenSub || hiddenMain.any { main ->
                                IosCategoryTaxonomy.childrenOf(main).any { it.code == canonical }
                            }
                        }.toSet())
                    for (change in IosCategoryTaxonomy.mutations(user.follows, desiredCategoryFollows)) {
                        api.followEntity("CATEGORY", change.key, change.label, change.following)
                    }
                    check((account?.id ?: guestReadyUserId) == expectedId) { "Ha canviat la sessió" }
                    chosenCategories = desiredCategoryFollows.categories; chosenTowns = savedTowns
                    chosenSubcategories = desiredCategoryFollows.subcategories
                    chosenComarques = savedComarques; chosenRadii = savedRadii
                    showSuggestions = suggestions; discoveryLevel = discovery; valencianFirst = valencian
                    uiLanguage = language
                    val visibilityNow = api.visibility()
                    excludedEventIds = visibilityNow.events; excludedArtists = visibilityNow.artists
                    excludedCategories = visibilityNow.categories
                    user = api.userState()
                    val confirmedCategories = IosCategoryTaxonomy.fromServer(user.follows)
                    chosenCategories = confirmedCategories.categories
                    chosenSubcategories = confirmedCategories.subcategories
                    serverStateRefreshEpoch += 1
                    prefOpen = false
                    status = if (exclusionsPending) "Preferències pendents de sincronització" else "Preferències guardades al servidor"
                    }
                }
            })
        popupAd?.let { ad ->
            val promotedEvent = eventById[ad.eventId]
            if (promotedEvent != null) {
                Dialog(onDismissRequest = { popupAd = null }) {
                    Surface(shape = RoundedCornerShape(18.dp), color = Color.White) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Publicitat", fontSize = 11.sp, color = Color.Gray)
                            LaunchedEffect(ad.id) { adViewed(ad) }
                            SponsoredPoster(ad, promotedEvent.posterUrl,
                                Modifier.fillMaxWidth().height(176.dp))
                            Text(promotedEvent.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("${promotedEvent.town} · ${promotedEvent.date}", fontSize = 12.sp, color = Color.DarkGray)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { popupAd = null }) { Text("Tancar") }
                                Button(onClick = { adOpened(ad, promotedEvent) }) { Text("Veure esdeveniment") }
                            }
                        }
                    }
                }
            }
        }
        LaunchedEffect(notificationsOpen, account?.id, guestReadyUserId) {
            if (!notificationsOpen) return@LaunchedEffect
            val owner = account?.id ?: guestReadyUserId ?: return@LaunchedEffect
            val before = notificationSettingsRevision
            runCatching { api.notificationCenterSettings(owner) }.onSuccess { loaded ->
                if (notificationsOpen && (account?.id ?: guestReadyUserId) == owner &&
                    before == notificationSettingsRevision &&
                    runCatching { api.hasPendingNotificationSettingsWrites(owner) }.getOrDefault(true).not()) {
                    notificationSettings = loaded
                    notificationSettingsLoadedOwner = owner
                    IosNativeBridge.current?.setPushDeliveryEnabled(loaded.pushEnabled)
                }
            }
            // Guests and registered users each have their own server inbox.
            runCatching { api.notifications() }.onSuccess { fresh ->
                if (notificationsOpen && (account?.id ?: guestReadyUserId) == owner) notices = fresh
            }
        }
        if (notificationsOpen) NotificationsDialog(notices, artistBells.values.sortedBy { it.artistName }, eventsById = eventById, onDismiss = { notificationsOpen = false }, onRead = { notice ->
            work { api.markRead(notice.id); notices = notices.map { if (it.id == notice.id) it.copy(read = true) else it } }
            when (notice.linkType.uppercase()) {
                "EVENT", "EVENT_ID", "EVENT_DETAIL" -> notice.linkValue.toIntOrNull()?.let { id ->
                    eventById[id]?.let { notificationsOpen = false; showEvent(it) }
                }
                "TICKETS", "TICKET" -> { notificationsOpen = false; tab = "Entrades" }
                "COMMUNITY" -> { notificationsOpen = false; tab = "Comunitat" }
                "ARTIST", "NEW_ARTIST_EVENT" -> { if (notice.linkValue.isNotBlank()) {
                    notificationsOpen = false; artistSourceEvent = null; artist = notice.linkValue
                } }
            }
        }, reminders = events.filter { it.id in user.alerts && (it.endDate.ifBlank { it.date }.isBlank() || it.endDate.ifBlank { it.date } >= today) },
            onReminderClick = { ev -> notificationsOpen = false; showEvent(ev) },
            onReminderDisable = { ev -> notificationsOpen = false; alertToDisable = ev },
            onArtistBell = { name -> notificationsOpen = false; artistBellEditing = name },
            reminderTime = { ev -> remoteReminderTime(ev.id) },
            settings = notificationSettings,
            isRegistered = account != null,
            savingSettings = notificationSettingsBusy,
            onSaveSettings = { draft ->
                val owner = account?.id ?: guestReadyUserId
                if (owner != null && notificationSettingsLoadedOwner != owner) {
                    error = "Cal recuperar la configuració del servidor abans de modificar-la."
                } else if (owner != null && !notificationSettingsBusy) {
                    notificationSettingsBusy = true
                    work {
                        try {
                            val pending = api.saveNotificationCenterSettings(owner, draft)
                            if ((account?.id ?: guestReadyUserId) == owner) {
                                notificationSettings = draft
                                notificationSettingsRevision += 1
                                IosNativeBridge.current?.setPushDeliveryEnabled(draft.pushEnabled)
                                if (draft.pushEnabled) IosNativeBridge.current?.requestRemoteNotifications(object : IosReminderSchedulingCallback {
                                    override fun onResult(success: Boolean, message: String) {
                                        if (!success) status = "Preferència guardada; permisos push no concedits"
                                    }
                                })
                                status = if (pending) "Preferències pendents de sincronitzar" else "Configuració guardada al servidor"
                            }
                        } catch (pending: IosQueuedForRetryException) {
                            if ((account?.id ?: guestReadyUserId) == owner) {
                                // Only persisted encrypted mutations can be projected in RAM.
                                notificationSettings = draft
                                notificationSettingsRevision += 1
                                IosNativeBridge.current?.setPushDeliveryEnabled(draft.pushEnabled)
                                status = "Preferències pendents de sincronitzar amb el servidor"
                            }
                        } finally { notificationSettingsBusy = false }
                    }
                }
            },
            onClearRead = { 
                val owner = account?.id ?: guestReadyUserId
                if (owner != null) work {
                    val unread = notices.filter { !it.read }
                    for (notice in unread) {
                        if ((account?.id ?: guestReadyUserId) != owner) break
                        api.markRead(notice.id)
                    }
                    if ((account?.id ?: guestReadyUserId) == owner) notices = api.notifications()
                }
            },
            onOpenNativeSettings = { IosNativeBridge.current?.showNotificationSettings() })
        val batecViewerUserId = account?.id ?: guestReadyUserId
        val openedBatecGroup = batecViewerGroups.firstOrNull { it.key == batecStoryKey }
        // Never show a previous account's snapshot after a sign-out/sign-in or guest claim.
        if (batecStoryKey != null && batecViewerOwnerId != batecViewerUserId) {
            LaunchedEffect(batecViewerUserId, batecStoryKey) { closeBatecs() }
        }
        if (batecStoryKey != null && openedBatecGroup != null && batecViewerOwnerId == batecViewerUserId) {
            BatecViewer(
                groups = batecViewerGroups,
                selectedKey = openedBatecGroup.key,
                stories = batecViewerStories,
                allEvents = available,
                isFollowed = { artist -> user.followsType("ARTIST", resolvedFollowKey("ARTIST", artist)) },
                onToggleFollow = { artist -> toggleFollow(artist) },
                onDismiss = ::closeBatecs,
                onOpenEvent = { id ->
                    closeBatecs()
                    eventById[id]?.let { showEvent(it) }
                },
                onGroupChanged = { next -> batecStoryKey = next.key }
            )
            // Mark every actual publication of the artist as seen. A failed request
            // must not invent server state or turn the ring grey optimistically.
            LaunchedEffect(batecStoryKey, account?.id, guestReadyUserId) {
                val expectedId = account?.id ?: guestReadyUserId
                val group = batecViewerGroups.firstOrNull { it.key == batecStoryKey }
                if (expectedId != null && expectedId == batecViewerOwnerId && group != null) {
                    val pending = group.storyKeys.filter { key ->
                        val seenKey = IosBatecArtistGroups.canonicalSeenKey(key)
                        seenKey !in seenBatecs && batecViewerStories.any {
                            it.storyKey == key && it.sourceType in setOf("MANUAL", "AUTO_EVENT")
                        }
                    }
                    // This job belongs to the screen, not to the current story:
                    // a quick swipe must not cancel marking the previous group.
                    scope.launch {
                        pending.forEach { key ->
                            if ((account?.id ?: guestReadyUserId) != expectedId) return@launch
                            runCatching { api.markBatecSeen(key) }.onSuccess {
                                if ((account?.id ?: guestReadyUserId) == expectedId) seenBatecs = seenBatecs +
                                    IosBatecArtistGroups.canonicalSeenKey(key)
                            }.onFailure { ex ->
                                if (ex is IosQueuedForRetryException && (account?.id ?: guestReadyUserId) == expectedId) {
                                    // Show as viewed locally while the encrypted queue waits for confirmation.
                                    seenBatecs = seenBatecs + IosBatecArtistGroups.canonicalSeenKey(key)
                                }
                            }
                        }
                    }
                }
            }
        }
        selected?.let { ev -> EventDetailSheet(ev, user.saved.contains(ev.id), user.alerts.contains(ev.id),
            matchedArtist = festivalMatchedArtist,
            followsEntity = { type, name -> user.followsType(type, resolvedFollowKey(type, name)) },
            onToggleFollowEntity = { type, name -> toggleFollowEntity(type, name) },
            onDismiss = { selected = null; festivalMatchedArtist = ""; savedFestivalRevision++ },
            onSave = { toggleSaved(ev) }, onReminder = { if (ev.id in user.alerts) alertToDisable = ev else reminderToSchedule = ev },
            userId = account?.id ?: guestReadyUserId,
            serverSlots = (account?.id ?: guestReadyUserId)?.let { ownerId ->
                festivalSlotOverrides[ownerId to ev.id] ?: serverState?.personalFestivalSlots(ev.id)
            },
            saveFestivalSlots = { newSlots ->
                val ownerId = account?.id ?: guestReadyUserId ?: throw IllegalStateException("Identitat no disponible")
                updateFestivalSlots(ownerId, ev.id, newSlots)
            },
            onReport = { selected = null; reportEvent = ev },
            batecApi = api, canPublishBatec = account != null,
            onBatecsChanged = { batecsRefreshEpoch += 1 },
            onBatecSignIn = { selected = null; loginOpen = true },
            loadSchedule = api::festivalSchedule,
            onArtist = { artistSourceEvent = ev; selected = null; artist = it }, onCommunity = {
                // A guest is a real server user. Never demand a registered account to browse boards.
                selected = null
                work { groups = api.community(ev.id); tab = "Comunitat" }
            }, onOpenCommunityGroup = { group ->
                selected = null
                communityGroup = group
            }, onCreateGroup = {
                if ((account?.id ?: guestReadyUserId) == null) {
                    status = "Cal establir la connexió del convidat abans de crear un tauló."
                } else { selected = null; groupCreateFor = ev }
            }, onTickets = {
                if (IosEventDetailContract.isSafeExternalTicketUrl(ev.ticketUrl)) platformOpenExternal(ev.ticketUrl)
                else if (ev.ticketAvailable && ev.ticketUrl.isBlank()) {
                    selected = null
                    if ((account?.id ?: guestReadyUserId) == null) status = "Cal establir la connexió del convidat."
                    else ticketEvent = ev
                } else status = "Este esdeveniment no té venda d'entrades disponible."
            }) }
    // The artist-bell PHP endpoint accepts a server-authenticated guest as well.
    // Fetch the guest's preferences under their own Bearer, not a previous account's.
    LaunchedEffect(guestReadyUserId, account?.id) {
        val guestId = guestReadyUserId?.takeIf { account == null } ?: return@LaunchedEffect
        val bells = runCatching { api.artistBells() }.getOrNull() ?: return@LaunchedEffect
        if (account == null && guestReadyUserId == guestId) {
            artistBells = bells.filter { it.enabled }.associateBy { IosArtistBellPolicy.key(it.artistName) }
        }
    }
        artist?.let { value ->
            val matchingEvents = available.filter { e ->
                e.artists.any { it.equals(value, true) } || e.schedule.any { it.artist.equals(value, true) }
            }
            // A schedule entry may exist only in the detailed remote festival response,
            // not the abbreviated feed: keep that festival visible in this artist popup.
            val fromFestival = artistSourceEvent?.let { listOf(it) }.orEmpty()
            ArtistDialog(api, value, user.follows(value), (fromFestival + matchingEvents).distinctBy { it.id },
                onLogin = { artist = null; artistSourceEvent = null; loginOpen = true },
                onStorePaid = { artistPageRefresh++ },
                publicPage = artistPublicPage?.takeIf { IosArtistPagePolicy.sameArtist(it.artist, value) },
                pageLoading = artistPageLoading, pageError = artistPageError,
                onRetryPage = { artistPageRefresh++ },
                onFollow = { toggleFollow(value) },
                bell = artistBells[IosArtistBellPolicy.key(value)],
                onBell = {
                    val requestedUser = account?.id ?: guestReadyUserId
                    if (requestedUser == null) status = "Cal establir la identitat del convidat."
                    else {
                        artistBellEditing = value
                        work {
                            val bells = api.artistBells()
                            if ((account?.id ?: guestReadyUserId) == requestedUser) artistBells = bells.filter { b -> b.enabled }
                                .associateBy { b -> IosArtistBellPolicy.key(b.artistName) }
                        }
                    }
                },
                onDismiss = { artist = null; artistSourceEvent = null },
                onEvent = { artist = null; artistSourceEvent = null; showEvent(it) })
        }
        artistBellEditing?.let { selectedArtist ->
            val existing = artistBells[IosArtistBellPolicy.key(selectedArtist)]
            ArtistBellEditorDialog(selectedArtist, existing,
                onDismiss = { artistBellEditing = null },
                onSave = { enabled, frequency ->
                    val requestUser = account?.id ?: guestReadyUserId
                    if (requestUser == null) { status = "Cal establir la identitat del convidat." }
                    else work {
                        var queued = false
                        val saved = try {
                            api.saveArtistBell(selectedArtist, enabled, frequency)
                        } catch (_: IosQueuedForRetryException) {
                            queued = true
                            IosArtistBell(selectedArtist, frequency, enabled)
                        }
                        if ((account?.id ?: guestReadyUserId) == requestUser) {
                            artistBells = IosArtistBellPolicy.update(artistBells, saved)
                            artistBellEditing = null
                            status = if (queued) "Campaneta pendent de sincronització"
                                else if (enabled) "Campaneta guardada · ${IosArtistBellPolicy.label(frequency)}"
                                else "Campaneta desactivada"
                            if (enabled) IosNativeBridge.current?.requestRemoteNotifications(object : IosReminderSchedulingCallback {
                                override fun onResult(success: Boolean, message: String) { /* Permission is separate from server save. */ }
                            })
                        }
                    }
                })
        }
        if (editAccountOpen && account != null) {
            AccountIdentityIosDialog(account!!, myProfile?.homeTown.orEmpty(),
                onDismiss = { editAccountOpen = false },
                onSave = { username, email, town ->
                    val expectedId = account?.id
                    val expectedUsername = account?.username
                    val oldEmail = account?.email.orEmpty()
                    work {
                        check(expectedId != null && account?.id == expectedId && account?.username == expectedUsername) {
                            "La sessió ha canviat abans de guardar el compte"
                        }
                        val updated = api.updateMyAccountIdentity(username, email, town)
                        if (account?.id == expectedId && account?.username == updated.username) {
                            account = updated
                            myProfile = runCatching { api.myPublicProfile() }.getOrNull()
                                ?.copy(homeTown = town.trim())
                            editAccountOpen = false
                            serverStateRefreshEpoch += 1
                            status = if (!oldEmail.equals(updated.email, ignoreCase = true)) "Compte actualitzat. Revisa el correu de verificació si cal."
                                else "Dades del compte actualitzades al servidor"
                        }
                    }
                })
        }
        if (profileSettingsOpen && profileSettings != null) {
            ProfileSettingsEditor(profileSettings!!, onDismiss = { profileSettingsOpen = false }, onSave = { role, phone ->
                val expectedId = account?.id
                work {
                    val newSettings = api.saveProfileSettings(role, phone)
                    if (expectedId != null && account?.id == expectedId) {
                        profileSettings = newSettings
                        // Profile settings do not grant verified publisher authorization.
                        serverPublisherAccess = runCatching { api.publisherSnapshot() }.getOrNull()
                        profileSettingsOpen = false
                        status = "Perfil actualitzat"
                    }
                }
            })
        }
        if (editProfileOpen && account != null) {
            ProfileEditIosDialog(myProfile ?: IosMyProfile(account?.username.orEmpty(), "", ""),
                onDismiss = { editProfileOpen = false },
                onPickImage = {
                    val selectedForId = account?.id
                    val selectedForUsername = account?.username
                    sessionBridge?.pickProfilePhoto(object : IosPickedPhotoCallback {
                        override fun onComplete(base64Jpeg: String, imageError: String) {
                            if (imageError.isNotBlank()) { error = imageError; return }
                            if (selectedForId == null || account?.id != selectedForId ||
                                account?.username != selectedForUsername) {
                                error = "La sessió ha canviat: no s'ha enviat la fotografia"
                                return
                            }
                            work {
                                check(account?.id == selectedForId && account?.username == selectedForUsername) {
                                    "Compte canviat abans d'enviar la foto"
                                }
                                val url = api.uploadMyPhoto(base64Jpeg)
                                if (account?.id == selectedForId && account?.username == selectedForUsername) {
                                    myProfile = (myProfile ?: IosMyProfile("", "", "")).copy(photoUrl = url)
                                    status = "Foto guardada al servidor"
                                }
                            }
                        }
                    }) ?: run { error = "El selector de fotografies no està disponible." }
                },
                onSave = { updated, remove ->
                    val expectedId = account?.id
                    val expectedUsername = account?.username
                    work {
                        check(expectedId != null && account?.id == expectedId && account?.username == expectedUsername) {
                            "La sessió ha canviat abans de guardar el perfil"
                        }
                        val saved = api.updateMyPublicProfile(updated.displayName, updated.bio, remove,
                            discipline = updated.discipline, companyName = updated.companyName,
                            castText = updated.castText, publisherName = updated.publisherName,
                            booksText = updated.booksText, worksText = updated.worksText,
                            spacesText = updated.spacesText)
                        if (account?.id == expectedId && account?.username == expectedUsername) {
                            myProfile = saved; editProfileOpen = false; status = "Perfil guardat al servidor"
                        }
                    }
                })
        }
        if (deleteAccountOpen && account != null) {
            DeleteAccountIosDialog(onDismiss = { deleteAccountOpen = false }, onDelete = { phrase, password ->
                val expectedId = account?.id
                work {
                    api.deleteMyAccount(phrase, password)
                    if (account?.id == expectedId) {
                        // Revokes local reminders for the previous identity and clears account-scoped UI.
                        user.alerts.forEach { eventId -> sessionBridge?.cancelLocalReminder(expectedId ?: 0, eventId) }
                        account = null; myProfile = null; user = IosUserState(); artistBells = emptyMap()
                        tickets = emptyList(); ticketsOffline = false; ticketLoadNotice = ""
                        notices = emptyList(); groups = emptyList(); seenBatecs = emptySet()
                        savedLists = emptyList(); searchPresets = emptyList()
                        sessionBridge?.eraseLocalAccountData(expectedId ?: 0)
                        serverState = null
                        guestReadyUserId = runCatching { api.ensureGuestIdentity() }.getOrNull()
                        serverStateRefreshEpoch += 1
                        useAccountPreferences(null); deleteAccountOpen = false
                        status = "Baixa confirmada pel servidor. Les dades exigides legalment es poden conservar anonimitzades."
                    }
                }
            })
        }
        if (publisherMenuOpen) {
            val expectedPublisherOwner = account?.id ?: guestReadyUserId
            if (expectedPublisherOwner != null) {
                key(expectedPublisherOwner) {
                    IosPublisherPanel(api, expectedPublisherOwner, registered = account != null,
                        onDismiss = { publisherMenuOpen = false },
                        onAuth = { publisherMenuOpen = false; loginOpen = true },
                        onPublished = { id ->
                            // Keep confirmation/dialog open: guest must see "Crear compte" or "Ara no".
                            status = "Esdeveniment $id publicat al servidor"
                            // The publisher panel now displays the single confirmation and guest prompt.
                            refresh(true)
                        },
                        onAdmin = { action ->
                            publisherMenuOpen = false
                            adminDestination = if (action == IosPublishAction.REVIEW_PUBLICATIONS) "Publicacions" else "Sol·licituds"
                            tab = "Perfil"
                        },
                        onNotice = { status = it })
                }
            } else {
                LaunchedEffect(Unit) { guestReadyUserId = runCatching { api.ensureGuestIdentity() }.getOrNull() }
            }
        }
        if (validatorOpen) ProfessionalValidationDialog(api, onDismiss = { validatorOpen = false })
        ticketEvent?.let { event -> TicketOptionsDialog(event, api,
            pendingStart = pendingTicketPayments[(api.activePersonalId() ?: 0) to event.id],
            onPaymentCreated = { start ->
                pendingTicketPayments = pendingTicketPayments + ((start.userId to start.eventId) to start)
            },
            onPaymentCleared = { start ->
                val key = start.userId to start.eventId
                if (pendingTicketPayments[key]?.orderId == start.orderId)
                    pendingTicketPayments = pendingTicketPayments - key
            },
            onDismiss = { ticketEvent = null }, onPurchased = { paidPurchase ->
                ticketEvent = null
                status = if (paidPurchase) "Pagament confirmat i entrades disponibles" else "Entrades gratuïtes emeses correctament"
                tab = "Entrades"
                work { reloadMyTickets() }
            }, onError = { error = it }) }
        groupCreateFor?.let { event -> CreateGroupDialog(event, api, onDismiss = { groupCreateFor = null },
            onCreated = { groupCreateFor = null; status = "Tauló creat"; work { groups = api.community(event.id) } },
            onError = { error = it }) }
        communityGroup?.let { g -> IosCommunityChatDialog(g, api, account?.id ?: guestReadyUserId ?: 0,
            onDismiss = { communityGroup = null }, onChanged = { work { groups = api.community() } }) }
        reminderToSchedule?.let { ev -> ReminderEditorDialog(ev, account?.id ?: guestReadyUserId ?: 0, remoteReminderTime(ev.id),
            onDismiss = { reminderToSchedule = null }, onSave = { requestedAt ->
            reminderToSchedule = null
            toggleReminder(ev, true, requestedAt)
        }) }
        alertToDisable?.let { ev -> AlertDialog(onDismissRequest = { alertToDisable = null }, title = { Text("Vols desactivar este recordatori?") },
            text = { Text(ev.title) }, dismissButton = { TextButton(onClick = { alertToDisable = null }) { Text("Mantindre") } },
            confirmButton = { TextButton(onClick = { toggleReminder(ev, false); alertToDisable = null }) { Text("Desactivar") } }) }
    }
}

@Composable
private fun ReminderEditorDialog(event: AgendaEventSummary, userId: Int, serverReminderTime: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val previous = serverReminderTime
    var date by remember(event.id) { mutableStateOf(previous.take(10).ifBlank { event.date.take(10) }) }
    var time by remember(event.id) { mutableStateOf(previous.drop(11).take(5).ifBlank { "10:00" }) }
    val valid = IosReminderPolicy.validLocalDateTime("${date}T${time}") &&
        (IosNativeBridge.current?.reminderEpochMillis("${date}T${time}") ?: 0L) > currentEpochMillis()
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Quan vols que t'avisem?") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(event.title, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text("Tria el dia i l'hora exactes. No s'enviarà cap avís abans de la data indicada.", fontSize = 12.sp)
            OutlinedTextField(date, { date = it.take(10) }, label = { Text("Data AAAA-MM-DD") }, singleLine = true)
            OutlinedTextField(time, { time = it.take(5) }, label = { Text("Hora HH:MM") }, singleLine = true)
            Text("Per als avisos externs cal autoritzar les notificacions en iOS.", fontSize = 11.sp, color = Color.Gray)
        } },
        confirmButton = { TextButton(enabled = valid, onClick = { onSave("${date}T${time}") }) { Text("Activar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel·lar") } })
}

@Composable
private fun PageTitle(text: String) { Text(text, modifier = Modifier.padding(start = 18.dp, top = 17.dp, bottom = 12.dp), fontSize = 26.sp, fontWeight = FontWeight.Bold) }
@Composable
private fun EmptyState(msg: String) { Text(msg, modifier = Modifier.padding(20.dp), color = Color.Gray, fontSize = 14.sp) }
@Composable
private fun LoginPrompt(open: () -> Unit) { Column(Modifier.padding(20.dp)) {
    Text("Inicia sessió per consultar les teues dades privades.")
    Spacer(Modifier.height(12.dp))
    Button(onClick = open) { Text("Iniciar sessió") }
} }
@Composable
private fun Poster(url: String, modifier: Modifier = Modifier) {
    Box(modifier.background(Accent, RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) {
        if (url.isNotBlank()) AsyncImage(model = url, contentDescription = "Cartell", contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize())
        else Text("♪", fontSize = 24.sp, color = Ink)
    }
}
@Composable
private fun EventCard(ev: AgendaEventSummary, isSaved: Boolean, onClick: () -> Unit, trailing: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Poster(ev.posterUrl, Modifier.width(88.dp).height(109.dp).clip(RoundedCornerShape(11.dp)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(ev.title, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (ev.subtitle.isNotBlank()) Text(ev.subtitle, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("📍 ${ev.town}${if (ev.venue.isNotBlank()) " · ${ev.venue}" else ""}", maxLines = 1, fontSize = 12.sp, color = Color.DarkGray)
            Text("${ev.date} ${ev.time} · ${ev.category}", fontSize = 11.sp, color = Color.DarkGray)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) { trailing() }
    }
    HorizontalDivider(color = Color(0xFFEEEEEE))
}
/** The compact WHITE card used by Android's 'Sense filtre' and 'Per a tu'.
 * A fixed-width date/save column keeps its title single-line, without clipping metadata.
 * Festival summaries refer to the relevant day only; tapping still opens the original popup. */
@Composable
private fun FeedCompactEventCard(ev: AgendaEventSummary, today: String, onClick: () -> Unit,
                                  trailing: @Composable () -> Unit) {
    val scheduledDays = ev.schedule.map { it.date }
    val shownDate = ev.feedOccurrenceDate.ifBlank {
        IosUnfilteredFeedPolicy.relevantDate(ev.date, ev.endDate, today,
            if (ev.isFestival) scheduledDays else emptyList())
    }
    // The source must contain an explicit future programme day. Never label an
    // unscheduled festival as 'En curs' and invent an occurrence on today's date.
    val preStartFestival = IosUnfilteredAgenda.isFestivalBeforeStart(ev, today)
    val subtitle = when {
        preStartFestival -> (ev.artists + ev.schedule.map { it.artist.ifBlank { it.title } })
            .filter(String::isNotBlank).distinctBy { it.lowercase() }.joinToString(" · ")
            .ifBlank { ev.subtitle }
        ev.isFestival -> IosUnfilteredFeedPolicy.festivalSummary(shownDate,
            ev.schedule.map { it.date to it.artist.ifBlank { it.title } },
            ev.subtitle)
        else -> ev.subtitle
    }
    val categoryColor = when {
        ev.category.contains("música", true) || ev.category.contains("musica", true) -> Color(0xFF8C59A9)
        ev.category.contains("teatre", true) || ev.category.contains("escèn", true) -> Color(0xFFAE673F)
        ev.category.contains("cinema", true) || ev.category.contains("audiovisual", true) -> Color(0xFF447EA0)
        else -> Color(0xFF766C87)
    }
    Column {
        Row(Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(98.dp).height(116.dp)
                .border(1.5.dp, categoryColor, RoundedCornerShape(9.dp))
                .padding(2.dp).clip(RoundedCornerShape(7.dp))) {
                Poster(ev.posterUrl, Modifier.fillMaxSize())
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f).heightIn(min = if (subtitle.isNotBlank()) 116.dp else 102.dp),
                verticalArrangement = Arrangement.Center) {
                Text(ev.title, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(subtitle, fontSize = 12.sp, maxLines = 1,
                        color = Color.DarkGray, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(4.dp))
                Text("📍 ${listOf(ev.town,ev.venue).filter(String::isNotBlank).joinToString(" · ")}",
                    fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = Ink)
                Spacer(Modifier.height(4.dp))
                val displayedTime = ev.time
                val ticket = IosUnfilteredFeedPolicy.ticketLabel(ev.ticketAvailable, ev.price)
                Text("◷ ${displayedTime.ifBlank { "—" }} · $ticket", fontSize = 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.DarkGray)
            }
            Spacer(Modifier.width(5.dp))
            Column(Modifier.width(64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(ev.category, fontSize = 9.sp, maxLines = 1,
                    color = categoryColor, overflow = TextOverflow.Ellipsis)
                Text(IosUnfilteredFeedPolicy.cardDate(shownDate),
                    fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Ink, maxLines = 1)
                trailing()
            }
        }
        HorizontalDivider(color = Color(0xFFEEEEEE))
    }
}

/** Keep the paid campaign's own crop focal point, independent of the event poster. */
@Composable
private fun SponsoredPoster(ad: IosAd, fallback: String, modifier: Modifier = Modifier) {
    val url = ad.imageUrl.ifBlank { fallback }
    Box(modifier.background(Accent, RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) {
        if (url.isNotBlank()) AsyncImage(model = url, contentDescription = "Creativitat publicitària",
            contentScale = ContentScale.Crop,
            alignment = BiasAlignment(ad.focusX * 2f - 1f, ad.focusY * 2f - 1f),
            modifier = Modifier.fillMaxSize())
        else Text("Publicitat", fontSize = 14.sp, color = Ink)
    }
}

/** Paid inventory has its own visual placements, independent of the organic cards.
 * Every active ad in a placement is reachable in a horizontal slider; initial visible
 * creative rotates every 30 minutes without changing the contracted slot. */
@Composable
private fun SponsoredCarousel(label: String, ads: List<IosAd>, eventById: Map<Int, AgendaEventSummary>,
                              onViewed: (IosAd) -> Unit, onOpened: (IosAd, AgendaEventSummary) -> Unit,
                              header: Boolean = false) {
    if (ads.isEmpty()) return
    val start = remember(ads) { ((currentEpochMillis() / (30 * 60 * 1000L)) % ads.size).toInt() }
    val state = androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex = start)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cardWidth = if (header) maxWidth - 24.dp else 270.dp
        LazyRow(state = state, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            items(ads, key = { "${it.placement}_${it.id}" }) { ad ->
                val event = eventById[ad.eventId]
                LaunchedEffect(ad.id, ad.placement) { onViewed(ad) }
                if (header) {
                    Box(Modifier.width(cardWidth).height(133.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .clickable(enabled = event != null) { event?.let { onOpened(ad, it) } }) {
                        SponsoredPoster(ad, event?.posterUrl.orEmpty(), Modifier.fillMaxSize())
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
                            Color.Transparent, Color.Black.copy(alpha = 0.12f), Color.Black.copy(alpha = 0.72f)))))
                        Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                            Text(event?.title ?: "Activitat patrocinada", color = Color.White,
                                fontWeight = FontWeight.Bold, fontSize = 17.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (event != null) {
                                Text(event.artists.take(3).joinToString(" · ").ifBlank { event.category },
                                    color=Color.White, fontSize = 12.sp, maxLines=1, overflow=TextOverflow.Ellipsis)
                                Text("${event.town} · ${IosUnfilteredFeedPolicy.cardDate(event.date)} · ${event.time}",
                                    color=Color.White, fontSize = 12.sp, maxLines=1)
                            }
                        }
                        AgendaGlyph("bookmark", Modifier.align(Alignment.BottomEnd)
                            .padding(bottom=10.dp,end=10.dp).size(21.dp), Color.White)
                    }
                } else {
                    Surface(shape = RoundedCornerShape(15.dp), color = Color.White,
                        tonalElevation = 1.dp, shadowElevation = 1.dp,
                        modifier = Modifier.width(cardWidth)
                            .clickable(enabled = event != null) { event?.let { onOpened(ad, it) } }) {
                        Column {
                            SponsoredPoster(ad, event?.posterUrl.orEmpty(), Modifier.fillMaxWidth().height(120.dp))
                            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                Text(event?.title ?: "Activitat patrocinada", fontWeight = FontWeight.SemiBold,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                                if (event != null) Text("${event.date} · ${event.town}",
                                    maxLines = 1, fontSize = 11.sp, color = Color.DarkGray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventList(events: List<AgendaEventSummary>, total: Int,
                      artistSearch: String = "",
                      festivalSchedules: Map<Int, List<AgendaScheduleEntry>> = emptyMap(),
                      headerContent: @Composable () -> Unit, onLoadMore: () -> Unit,
                      today: String, scrollContext: String,
                      restoreReading: IosFeedReadingPolicy.Snapshot?,
                      readingRestoreEpoch: Int,
                      onReadingPosition: (Int, Int, Boolean) -> Unit,
                      savedIds: Set<Int>, onClick: (AgendaEventSummary) -> Unit,
                      onSave: (AgendaEventSummary) -> Unit, ads: List<IosAd>,
                      protectedIds: Set<Int>, commercialEvents: Map<Int, AgendaEventSummary>,
                      onAdViewed: (IosAd) -> Unit,
                      onAdOpened: (IosAd, AgendaEventSummary) -> Unit,
                      refreshing: Boolean, onRefresh: () -> Unit,
                      returnToStartEpoch: Int,
                      onExclude: (AgendaEventSummary, String) -> Unit,
                      onReport: (AgendaEventSummary) -> Unit) {
    val listState = rememberLazyListState()
    var positionRestored by remember(scrollContext, readingRestoreEpoch) { mutableStateOf(false) }
    val latestOnReadingPosition by rememberUpdatedState(onReadingPosition)
    // Restore both item and pixel offset. The first LazyColumn row is the header,
    // so keep its real list index instead of assuming card index = row index.
    LaunchedEffect(scrollContext, readingRestoreEpoch) {
        positionRestored = false
        val saved = restoreReading
        val targetRows = if (saved == null) 1 else
            (saved.firstIndex + 1).coerceAtMost(total + 1).coerceAtLeast(1)
        snapshotFlow { listState.layoutInfo.totalItemsCount }.first { it >= targetRows }
        if (saved != null) {
            listState.scrollToItem(saved.firstIndex.coerceIn(0, listState.layoutInfo.totalItemsCount - 1),
                saved.firstOffset)
        } else listState.scrollToItem(0)
        positionRestored = true
    }
    // Keep the in-memory position at pixel precision even if the app backgrounds
    // mid-scroll; ONLY the settled position starts a debounced server write.
    LaunchedEffect(scrollContext, readingRestoreEpoch, listState, positionRestored) {
        if (positionRestored) {
            snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
                .collect { (index, offset) -> latestOnReadingPosition(index, offset, false) }
        }
    }
    LaunchedEffect(scrollContext, readingRestoreEpoch, listState, positionRestored) {
        if (positionRestored) {
            snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
                .debounce(900L)
                .collect { (index, offset) -> latestOnReadingPosition(index, offset, true) }
        }
    }
    // Closing the Batec returns the whole feed to its first card/ad, not only the story strip.
    LaunchedEffect(returnToStartEpoch) {
        if (returnToStartEpoch > 0) listState.scrollToItem(0)
    }
    val premiumAds = remember(ads) { ads.filter { it.placement == "FEED_PREMIUM" } }
    val normalAds = remember(ads) { ads.filter { it.placement == "FEED_NORMAL" } }
    val premium = premiumAds.isNotEmpty()
    val normal = normalAds.isNotEmpty()
    // Use the FULL validated inventory, not just the currently paginated organic rows.
    // Otherwise paid ads with distant events would be impossible to open.
    val eventById = commercialEvents
    val advertisedEventIds = remember(ads) { ads.mapTo(HashSet()) { it.eventId } }
    // LazyColumn composes this sentinel only when the user reaches the real end of a
    // rendered batch. Counting index positions instead breaks when HEADER/paid ads exist.
    PullToRefreshBox(isRefreshing = refreshing, onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(), indicator = {}) {
        LazyColumn(state = listState, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 5.dp)) {
            item(key = "inici_header", contentType = "inici_header") { headerContent() }
            itemsIndexed(events, key = { _, ev -> ev.key }, contentType = { _, _ -> "event" }) { index, ev ->
                Column {
                    if (GlobalAdPlacementPolicy.premiumAt(index, premium, normal)) {
                        SponsoredCarousel("Premium", premiumAds, eventById, onAdViewed, onAdOpened)
                    }
                    if (GlobalAdPlacementPolicy.normalAt(index, premium, normal)) {
                        SponsoredCarousel("Normal", normalAds, eventById, onAdViewed, onAdOpened)
                    }
                    val isCommercial = IosCommercialVisibility.protected(ev, protectedIds + advertisedEventIds)
                    Box(Modifier.fillMaxWidth()) {
                        FeedCompactEventCard(ev, today, onClick = { onClick(ev) }, trailing = {
                            var menuVisible by remember(ev.key) { mutableStateOf(false) }
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(Modifier.size(32.dp).clickable { onSave(ev) }, contentAlignment = Alignment.Center) {
                                        AgendaGlyph("bookmark", Modifier.size(20.dp), if (ev.id in savedIds) Ink else Color.Gray)
                                    }
                                    Text(ev.savedCount.toString(), fontSize = 9.sp, color = Color.Gray)
                                }
                                Box {
                                    Box(Modifier.size(32.dp).clickable { menuVisible = true }, contentAlignment = Alignment.Center) {
                                        Text("⋮", fontSize = 20.sp)
                                    }
                                    DropdownMenu(expanded = menuVisible, onDismissRequest = { menuVisible = false }) {
                                    DropdownMenuItem(text = { Text("Informar d'una incidència") }, onClick = {
                                        menuVisible = false; onReport(ev)
                                    })
                                    if (isCommercial) {
                                        DropdownMenuItem(text = { Text("Activitat comercial obligatòria") },
                                            enabled = false, onClick = {})
                                    } else {
                                        DropdownMenuItem(text = { Text("No mostrar esdeveniment") }, onClick = {
                                            menuVisible = false; onExclude(ev, "EVENT")
                                        })
                                        if (ev.artists.any(String::isNotBlank) || ev.schedule.any { it.artist.isNotBlank() }) {
                                            DropdownMenuItem(text = { Text("No mostrar artista") }, onClick = {
                                                menuVisible = false; onExclude(ev, "ARTIST")
                                            })
                                        }
                                        if (ev.category.isNotBlank()) {
                                            DropdownMenuItem(text = { Text("No mostrar categoria") }, onClick = {
                                                menuVisible = false; onExclude(ev, "CATEGORY")
                                            })
                                        }
                                    }
                                }
                                }
                            }
                        })
                    }
                    if (artistSearch.isNotBlank() && ev.isFestival &&
                        IosFestivalArtistSearch.isInLineup(ev, artistSearch)) {
                        val artistSlot = IosFestivalArtistSearch.firstVerified(
                            ev, artistSearch, festivalSchedules[ev.id])
                        val summary = if (artistSlot != null) {
                            listOf(artistSearch, artistSlot.date.take(10), artistSlot.time,
                                artistSlot.location).filter(String::isNotBlank).joinToString(" · ")
                        } else "$artistSearch · Horari pendent de confirmar"
                        Text(summary, modifier = Modifier.padding(start = 17.dp, end = 17.dp, bottom = 6.dp),
                            fontSize = 11.sp, fontWeight = FontWeight.Medium, color = AgendaBlack)
                    }
                }
            }
            // Do not insert tail ads at each intermediate batch: they are appended only
            // after the real last organic row, preserving the contracted visual slots.
            if (events.size >= total) {
                if (GlobalAdPlacementPolicy.premiumAfterLast(total, premium, normal)) {
                    item(key = "ad_premium_tail", contentType = "sponsored") {
                        SponsoredCarousel("Premium", premiumAds, eventById, onAdViewed, onAdOpened)
                    }
                }
                if (GlobalAdPlacementPolicy.normalAfterLast(total, premium, normal)) {
                    item(key = "ad_normal_tail", contentType = "sponsored") {
                        SponsoredCarousel("Normal", normalAds, eventById, onAdViewed, onAdOpened)
                    }
                }
            }
            if (events.size < total) item(key = "load_more_${events.size}", contentType = "page_sentinel") {
                LaunchedEffect(events.size, total) { onLoadMore() }
                Spacer(Modifier.fillMaxWidth().height(1.dp))
            }
            if (events.isEmpty() && ads.isEmpty()) item(key = "empty") {
                EmptyState("No hi ha activitats que coincidisquen amb els criteris actuals.")
            }
        }
    }
}

/** Real event report: all reasons are the exact codes accepted by events/report.php. */
@Composable
private fun EventReportDialog(event: AgendaEventSummary, onDismiss: () -> Unit,
                              onSend: (String, String) -> Unit) {
    val options = listOf(
        "INCORRECT_INFO" to "Informació incorrecta",
        "CANCELLED" to "S'ha cancel·lat",
        "DUPLICATE" to "Esdeveniment duplicat",
        "NOT_CULTURAL" to "No és un esdeveniment cultural",
        "ANIMAL_ABUSE" to "Maltractament animal",
        "HUMAN_RIGHTS" to "Ataca els drets humans",
        "HATE_DISCRIMINATION" to "Odi o discriminació",
        "INCITEMENT_VIOLENCE" to "Incitació a la violència",
        "POSSIBLE_SCAM" to "Possible estafa",
        "INAPPROPRIATE_CONTENT" to "Contingut inadequat",
        "OTHER" to "Altres"
    )
    var reason by remember(event.key) { mutableStateOf("INCORRECT_INFO") }
    var details by remember(event.key) { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Informar d'una incidència") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(event.title, fontWeight = FontWeight.Bold)
            Box {
                OutlinedButton(onClick = { menuOpen = true }) {
                    Text(options.firstOrNull { it.first == reason }?.second ?: "Motiu")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    options.forEach { (code, label) ->
                        DropdownMenuItem(text = { Text(label) }, onClick = { reason = code; menuOpen = false })
                    }
                }
            }
            OutlinedTextField(details, { details = it.take(1500) },
                label = { Text("Detalls (opcional)") }, minLines = 3, maxLines = 5)
            Text("El report s'envia al servei de revisió. No elimina automàticament l'activitat.",
                fontSize = 11.sp, color = Color.Gray)
        } },
        confirmButton = { TextButton(onClick = { onSend(reason, details) }) { Text("Enviar report") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel·lar") } })
}

@Composable
private fun LoginDialog(onDismiss: () -> Unit, onSocial: (String) -> Unit, onRegister: () -> Unit,
                        onForgotPassword: () -> Unit, onSubmit: (String, String) -> Unit) {
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Iniciar sessió") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(value = login, onValueChange = { login = it }, singleLine = true, label = { Text("Usuari o correu") })
            OutlinedTextField(value = password, onValueChange = { password = it }, singleLine = true, label = { Text("Contrasenya") },
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
            Text("Connexió al mateix compte de L'Agenda. La contrasenya no es guarda al dispositiu.", fontSize = 11.sp, color = Color.Gray)
            OutlinedButton(onClick = { onSocial("APPLE") }, modifier = Modifier.fillMaxWidth()) { Text("Continua amb Apple") }
            OutlinedButton(onClick = { onSocial("GOOGLE") }, modifier = Modifier.fillMaxWidth()) { Text("Continua amb Google") }
            TextButton(onClick = onRegister) { Text("Crear compte nou") }
            TextButton(onClick = onForgotPassword) { Text("He oblidat la contrasenya") }
        } },
        confirmButton = { TextButton(enabled = login.isNotBlank() && password.isNotBlank(), onClick = { onSubmit(login, password) }) { Text("Entrar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel·lar") } })
}

@Composable
private fun PasswordResetDialog(onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    var email by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Recuperar contrasenya") },
        text = { Column {
            Text("Si hi ha un compte associat, rebràs un correu de recuperació.", fontSize = 12.sp)
            OutlinedTextField(email, { email = it.trim().take(200) }, label = { Text("Correu electrònic") }, singleLine = true)
        } },
        confirmButton = { TextButton(enabled = email.contains("@"), onClick = { onSubmit(email) }) { Text("Enviar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel·lar") } })
}

@Composable
private fun RegistrationDialog(onDismiss: () -> Unit, onSubmit: (String, String, String) -> Unit) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Crear compte") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            OutlinedTextField(username, { username = it.take(80) }, label = { Text("Nom d'usuari") }, singleLine = true)
            OutlinedTextField(email, { email = it.take(200) }, label = { Text("Correu electrònic") }, singleLine = true)
            OutlinedTextField(password, { password = it }, label = { Text("Contrasenya (mínim 8 caràcters)") }, singleLine = true,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
            Text("S'utilitza el mateix compte del servidor de L'Agenda. Es guardarà només el token en el Keychain.",
                fontSize = 11.sp, color = Color.Gray)
        } },
        confirmButton = { TextButton(enabled = username.length >= 3 && email.contains("@") && password.length >= 8,
            onClick = { onSubmit(username, email, password) }) { Text("Crear") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel·lar") } })
}

@Composable
private fun PersonalizationDialog(categories: Set<String>, towns: Set<String>, hiddenEventIds: Set<Int>,
                                  hiddenArtists: Set<String>, hiddenCategories: Set<String>,
                                  allEvents: List<AgendaEventSummary>, options: List<String>, townOptions: List<String>,
                                  styles: Set<String>, comarques: Set<String>, radii: Map<String,Int>,
                                  suggestions: Boolean, discovery: DiscoveryLevel, valencian: Boolean,
                                  language: IosUiLanguage, onDismiss: () -> Unit,
                                  onSave: (Set<String>, Set<String>, Set<Int>, Set<String>, Set<String>,
                                           Set<String>, Set<String>, Map<String,Int>, Boolean, DiscoveryLevel,
                                           Boolean, IosUiLanguage) -> Unit) {
    var cat by remember { mutableStateOf(categories) }
    var selectedTowns by remember { mutableStateOf(towns) }
    var townQuery by remember { mutableStateOf("") }
    var selectedStyles by remember { mutableStateOf(styles) }
    var selectedComarques by remember { mutableStateOf(comarques) }
    var perTownRadius by remember { mutableStateOf(radii) }
    var suggestionsEnabled by remember { mutableStateOf(suggestions) }
    var selectedDiscovery by remember { mutableStateOf(discovery) }
    var preferredValencian by remember { mutableStateOf(valencian) }
    var selectedUiLanguage by remember { mutableStateOf(language) }
    var comarcaQuery by remember { mutableStateOf("") }
    // Exact Android taxonomy, not free-form event styles: user_follows stores these codes.
    val possibleSubcategories = remember { IosCategoryTaxonomy.subcategories }
    val possibleComarques = remember { IosGeoPlaces.allComarques() }

    var hiddenEventsDraft by remember { mutableStateOf(hiddenEventIds) }
    var hiddenArtistsDraft by remember { mutableStateOf(hiddenArtists) }
    var hiddenCategoriesDraft by remember { mutableStateOf(hiddenCategories) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(22.dp), color = Color.White, modifier = Modifier.fillMaxWidth().fillMaxHeight(0.88f)) {
            Column(Modifier.padding(16.dp)) {
                Text("Personalització", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text("Seguiments sense límit de distància · estils dins del radi triat · preferències fins a 100 km.",
                    fontSize = 11.sp, color = Color.Gray)
                Spacer(Modifier.height(10.dp))
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    Text("Categories", fontWeight = FontWeight.Bold)
                    options.forEach { key ->
                        val caption = IosCategoryTaxonomy.label(key)
                        Row(Modifier.fillMaxWidth().clickable {
                            val next = IosCategoryTaxonomy.toggleCategory(
                                IosCategorySelection(cat, selectedStyles), key)
                            cat = next.categories; selectedStyles = next.subcategories
                        }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = key in cat, onCheckedChange = {
                                val next = IosCategoryTaxonomy.toggleCategory(
                                    IosCategorySelection(cat, selectedStyles), key)
                                cat = next.categories; selectedStyles = next.subcategories
                            })
                            Text(caption, fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Subcategories", fontWeight = FontWeight.Bold)
                    possibleSubcategories.forEach { sub ->
                        val selected = sub.categoryKey in cat || sub.code in selectedStyles
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = selected, onCheckedChange = {
                                val next = IosCategoryTaxonomy.toggleSubcategory(
                                    IosCategorySelection(cat, selectedStyles), sub.code)
                                cat = next.categories; selectedStyles = next.subcategories
                            })
                            Column {
                                Text(sub.label, fontSize = 13.sp)
                                Text(IosCategoryTaxonomy.label(sub.categoryKey), fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Comarques d'interés",fontWeight = FontWeight.Bold)
                    OutlinedTextField(comarcaQuery,{ comarcaQuery = it },label = { Text("Busca una comarca") },
                        singleLine = true,modifier = Modifier.fillMaxWidth())
                    (selectedComarques + possibleComarques.filter {
                        comarcaQuery.isNotBlank() && it.contains(comarcaQuery,true) }.take(20)).distinct().sorted().forEach { item ->
                        Row(Modifier.fillMaxWidth(),verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = item in selectedComarques,onCheckedChange = { checked ->
                                selectedComarques = if (checked) selectedComarques + item else selectedComarques - item })
                            Text(item,fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Poblacions d'interés", fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = townQuery, onValueChange = { townQuery = it }, singleLine = true,
                        label = { Text("Busca una població") }, modifier = Modifier.fillMaxWidth())
                    (selectedTowns + townOptions.filter { townQuery.isNotBlank() && it.contains(townQuery, true) }.take(25)
                        + IosGeoPlaces.searchTowns(townQuery)).distinct().sorted().forEach { item ->
                        Row(Modifier.fillMaxWidth().clickable { selectedTowns = if (item in selectedTowns) selectedTowns - item else selectedTowns + item },
                            verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = item in selectedTowns, onCheckedChange = { active ->
                                selectedTowns = if (active) selectedTowns + item else selectedTowns - item
                            })
                            Column {
                                Text(item, fontSize = 13.sp)
                                if (item in selectedTowns) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        PersonalizationOptions.locationRadiiKm.forEach { r ->
                                            val isSelected = (perTownRadius[item] ?: PersonalizationOptions.defaultRadiusKm) == r
                                            TextButton(onClick = { perTownRadius = perTownRadius + (item to r) },
                                                contentPadding = PaddingValues(horizontal = 3.dp, vertical = 1.dp)) {
                                                Text("${r}k",fontSize = 10.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Idioma de l'app (Android / iPhone)", fontWeight = FontWeight.Bold)
                    IosUiLanguage.entries.forEach { option ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedUiLanguage == option,
                                onClick = { selectedUiLanguage = option })
                            Text(option.label, modifier = Modifier.clickable { selectedUiLanguage = option }, fontSize = 13.sp)
                        }
                    }
                    Text("L'idioma de l'app és independent de l'idioma dels esdeveniments.",
                        fontSize = 11.sp, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(),verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = preferredValencian,onCheckedChange = { preferredValencian = it })
                        Text("Prioritzar activitats en valencià",fontSize = 13.sp)
                    }
                    Row(Modifier.fillMaxWidth(),verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = suggestionsEnabled,onCheckedChange = { suggestionsEnabled = it })
                        Text("Suggerències segons les activitats obertes (últims 15 dies)",fontSize = 12.sp)
                    }
                    if (IosFeedPreferencesContract.displayDiscovery(suggestionsEnabled)) {
                        DiscoveryLevel.entries.forEach { item ->
                            Row(Modifier.fillMaxWidth(),verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = selectedDiscovery == item,onClick = { selectedDiscovery = item })
                                Text(item.label,fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(15.dp))
                    Text("Contingut ocultat del feed", fontWeight = FontWeight.Bold)
                    Text("Pots recuperar qualsevol exclusió. Les activitats comercials obligatòries continuen visibles.",
                        fontSize = 11.sp, color = Color.Gray)
                    hiddenEventsDraft.sorted().forEach { id ->
                        val title = allEvents.firstOrNull { it.id == id }?.title ?: "Esdeveniment #$id"
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(title, modifier = Modifier.weight(1f), fontSize = 12.sp)
                            TextButton(onClick = { hiddenEventsDraft = hiddenEventsDraft - id }) { Text("Mostrar") }
                        }
                    }
                    hiddenArtistsDraft.sorted().forEach { name ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Artista: $name", modifier = Modifier.weight(1f), fontSize = 12.sp)
                            TextButton(onClick = { hiddenArtistsDraft = hiddenArtistsDraft - name }) { Text("Mostrar") }
                        }
                    }
                    hiddenCategoriesDraft.sorted().forEach { name ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Categoria: $name", modifier = Modifier.weight(1f), fontSize = 12.sp)
                            TextButton(onClick = { hiddenCategoriesDraft = hiddenCategoriesDraft - name }) { Text("Mostrar") }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel·lar") }
                    Button(onClick = { onSave(cat, selectedTowns, hiddenEventsDraft, hiddenArtistsDraft, hiddenCategoriesDraft,
                        selectedStyles,selectedComarques,perTownRadius.filterKeys { it in selectedTowns },
                        suggestionsEnabled,selectedDiscovery,preferredValencian,selectedUiLanguage) }) { Text("Guardar") }
                }
            }
        }
    }
}

@Composable
private fun EventDetailSheet(ev: AgendaEventSummary, saved: Boolean, reminded: Boolean, matchedArtist: String,
                             followsEntity: (String, String) -> Boolean, onToggleFollowEntity: (String, String) -> Unit,
                             onDismiss: () -> Unit, onSave: () -> Unit, onReminder: () -> Unit,
                             userId: Int?, serverSlots: Set<String>?, saveFestivalSlots: suspend (Set<String>) -> Boolean,
                             onReport: () -> Unit,
                             batecApi: IosAgendaApi, canPublishBatec: Boolean,
                             onBatecsChanged: () -> Unit, onBatecSignIn: () -> Unit,
                             loadSchedule: suspend (Int) -> List<AgendaScheduleEntry>,
                             onArtist: (String) -> Unit, onCommunity: () -> Unit,
                             onOpenCommunityGroup: (IosCommunityGroup) -> Unit,
                             onCreateGroup: () -> Unit, onTickets: () -> Unit) {
    // Never invent days between start and end dates. Only real scheduled sessions get tabs.
    var servicesOpen by remember(ev.id) { mutableStateOf(false) }
    var detailSchedule by remember(ev.id) { mutableStateOf(ev.schedule) }
    var detailLoading by remember(ev.id) { mutableStateOf(ev.isFestival) }
    var detailError by remember(ev.id) { mutableStateOf("") }
    var detailReload by remember(ev.id) { mutableIntStateOf(0) }
    LaunchedEffect(ev.id, detailReload) {
        if (ev.isFestival) {
            detailLoading = true
            detailError = ""
            runCatching { loadSchedule(ev.id) }
                .onSuccess { fresh ->
                    if (fresh.isNotEmpty()) detailSchedule =
                        IosFestivalPopupPolicy.mergePublished(fresh, ev.schedule)
                    else if (detailSchedule.isEmpty()) detailError = "Encara no hi ha horaris publicats."
                }
                .onFailure { detailError = "No s'ha pogut carregar la programació." }
        }
        detailLoading = false
    }
    // Android uses the published remote programme, falling back to the catalog
    // slot for the searched artist if the remote programme omits that artist.
    // Both sources are actual schedule rows; no date is inferred from festival span.
    val festivalProgramme = remember(ev.id, matchedArtist, detailSchedule, ev.schedule) {
        val publishedContainsArtist = detailSchedule.any {
            IosFestivalArtistSearch.matchesArtist(it, matchedArtist)
        }
        if (matchedArtist.isBlank() || publishedContainsArtist) detailSchedule
        else (detailSchedule + ev.schedule.filter {
            IosFestivalArtistSearch.matchesArtist(it, matchedArtist)
        }).distinctBy { listOf(it.date, it.time, it.artist, it.title, it.location) }
    }
    // A cultural day ends at 05:00 Europe/Madrid, just like Android's FestivalDayVisibility.
    var culturalToday by remember(ev.id) { mutableStateOf(currentAgendaDateIso()) }
    LaunchedEffect(ev.id) {
        while (true) {
            culturalToday = currentAgendaDateIso()
            kotlinx.coroutines.delay(60_000L)
        }
    }
    val festivalDays = remember(ev.id, festivalProgramme, culturalToday) {
        IosFestivalPopupPolicy.upcomingDays(festivalProgramme, culturalToday)
    }
    val targetSlot = remember(ev.id, matchedArtist, festivalProgramme) {
        IosFestivalArtistSearch.firstVerified(ev, matchedArtist, festivalProgramme)
    }
    var section by remember(ev.id, matchedArtist) {
        mutableStateOf(if (ev.isFestival) "Horaris" else "Info")
    }
    var selectedDay by remember(ev.id, matchedArtist) {
        mutableStateOf(IosFestivalPopupPolicy.initialDay(festivalDays, targetSlot, ev.displayDate))
    }
    LaunchedEffect(targetSlot?.date, festivalDays) {
        selectedDay = IosFestivalPopupPolicy.initialDay(festivalDays, targetSlot, selectedDay)
    }
    var onlyMyPerformances by remember(ev.id, userId, selectedDay) { mutableStateOf(false) }
    var ticketsDayFilter by remember(ev.id) { mutableStateOf("") }
    var mapDestination by remember(ev.id) { mutableStateOf("") }
    val detailScrollState = rememberScrollState()
    LaunchedEffect(section, selectedDay) { detailScrollState.scrollTo(0) }
    // Each event and server identity receives its own ephemeral tab data.
    var eventGroups by remember(ev.id, userId) { mutableStateOf(emptyList<IosCommunityGroup>()) }
    var communityLoading by remember(ev.id, userId) { mutableStateOf(false) }
    var communityError by remember(ev.id, userId) { mutableStateOf("") }
    var communityReload by remember(ev.id, userId) { mutableIntStateOf(0) }
    var ticketOptions by remember(ev.id) { mutableStateOf(emptyList<IosTicketOption>()) }
    var ticketsLoading by remember(ev.id) { mutableStateOf(false) }
    var ticketOptionsError by remember(ev.id) { mutableStateOf("") }
    var ticketsReload by remember(ev.id) { mutableIntStateOf(0) }
    LaunchedEffect(ev.id, userId, section, communityReload) {
        if (section == "Comunitat" && userId != null) {
            communityLoading = true
            communityError = ""
            runCatching { batecApi.community(ev.id) }
                .onSuccess { eventGroups = it.filter { group -> group.eventId == ev.id } }
                .onFailure { communityError = it.message ?: "No s'han pogut consultar els taulons" }
            communityLoading = false
        }
    }
    LaunchedEffect(ev.id, section, ticketsReload) {
        if (section == "Entrades" && ev.ticketAvailable && ev.ticketUrl.isBlank()) {
            ticketsLoading = true
            ticketOptionsError = ""
            ticketOptions = emptyList() // no deixar dades antigues visibles després d'un error HTTP
            runCatching { batecApi.ticketOptions(ev.id) }
                .onSuccess { ticketOptions = it }
                .onFailure { ticketOptionsError = it.message ?: "No s'han pogut consultar les entrades" }
            ticketsLoading = false
        }
    }
    var showBatecManager by remember(ev.id) { mutableStateOf(false) }
    var showPoster by remember(ev.id) { mutableStateOf(false) }
    var savedSlots by remember(ev.id, userId) { mutableStateOf(serverSlots.orEmpty()) }
    var localSaveNote by remember(ev.id, userId) { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(serverSlots, userId, ev.id) { savedSlots = serverSlots.orEmpty() }
    var slotSaving by remember(ev.id, userId) { mutableStateOf(false) }
    fun toggleSlot(slot: AgendaScheduleEntry) {
        if (slotSaving) return
        val key = IosFestivalSavedSlotPolicy.key(ev.id, slot.date, slot.time, slot.artist, slot.title, slot.location)
        val oldKey = IosFestivalSavedSlotPolicy.key(ev.id, slot.date, slot.time, slot.artist, slot.title)
        val wasSelected = key in savedSlots || oldKey in savedSlots
        val next = if (wasSelected) savedSlots - key - oldKey else savedSlots + key
        slotSaving = true
        coroutineScope.launch {
            try {
                val confirmed = saveFestivalSlots(next)
                savedSlots = next
                localSaveNote = if (confirmed) "Horari guardat al servidor" else
                    "Canvi pendent de sincronització; es conservarà xifrat"
            } catch (ex: Exception) {
                localSaveNote = "No s'ha guardat: ${ex.message ?: "error de connexió"}"
            } finally { slotSaving = false }
        }
    }
    Dialog(onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(22.dp), color = Color.White,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                .fillMaxHeight(if (ev.isFestival) 0.92f else 0.78f)) {
            Column {
                // Android event popup: image/title/date/place form a SINGLE gradient hero.
                Box(Modifier.fillMaxWidth().height(154.dp)) {
                    Poster(ev.posterUrl, Modifier.fillMaxSize())
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = .08f),
                            Color.Black.copy(alpha = .19f), Color.Black.copy(alpha = .91f)))))
                    Column(Modifier.align(Alignment.BottomStart).padding(start = 15.dp, end = 8.dp, bottom = 13.dp)) {
                        Text(if (ev.isFestival) "FESTIVAL" else "ESDEVENIMENT", color = Color.White, fontSize = 10.sp,
                            fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(ev.title, color = Color.White, fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold, maxLines = 2,
                            overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(5.dp))
                        Text(IosUnfilteredFeedPolicy.fullDate(ev.date), color = AgendaGold,
                            fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("⌖  ${listOf(ev.venue,ev.town).filter(String::isNotBlank).joinToString(" · ")}",
                            color = Color.White, fontSize = 13.sp, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                    }
                    Row(Modifier.align(Alignment.TopEnd).padding(7.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(43.dp).background(Color.White, CircleShape)
                            .clickable(onClick = onReminder), contentAlignment = Alignment.Center) {
                            AgendaGlyph("bell", Modifier.size(23.dp))
                        }
                        Box(Modifier.size(43.dp).background(Color.White, CircleShape)
                            .clickable { IosNativeBridge.current?.shareText(
                                listOf(ev.title,ev.date,ev.venue,ev.town,ev.ticketUrl)
                                    .filter(String::isNotBlank).joinToString(" · ")) },
                            contentAlignment = Alignment.Center) { AgendaGlyph("share", Modifier.size(23.dp)) }
                        Box(Modifier.size(43.dp).background(Color.White, CircleShape)
                            .clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
                            AgendaGlyph("close", Modifier.size(23.dp))
                        }
                    }
                    if (ev.posterUrl.isNotBlank()) {
                        Surface(Modifier.align(Alignment.TopStart).padding(8.dp)
                            .clickable { showPoster = true }, shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha=.55f)) {
                            Text("▣  Cartell", Modifier.padding(horizontal = 11.dp,vertical = 7.dp),
                                color = Color.White,fontSize=11.sp)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    AgendaGlyph("calendar", Modifier.size(19.dp), Color.Gray)
                    Spacer(Modifier.width(7.dp))
                    Text(if (ev.isFestival) "${festivalDays.size} jornades disponibles · Consulta els horaris"
                        else "${IosUnfilteredFeedPolicy.fullDate(ev.date)} · ${ev.time}",
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text(IosUnfilteredFeedPolicy.ticketLabel(ev.ticketAvailable,ev.price),
                        color = Color(0xFF946C00), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = RoundedCornerShape(12.dp), color = Color.White,
                        border = BorderStroke(1.dp, AgendaBlack),
                        modifier = Modifier.weight(1f).height(43.dp).clickable(onClick = onSave)) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center) {
                            AgendaGlyph("bookmark", Modifier.size(17.dp))
                            Spacer(Modifier.width(7.dp))
                            Text(if (saved) "Guardat" else "Guardar", fontSize=13.sp,
                                fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Surface(shape=RoundedCornerShape(12.dp), color=AgendaBlack,
                        modifier=Modifier.weight(1f).height(43.dp).clickable(onClick=onTickets)) {
                        Row(verticalAlignment=Alignment.CenterVertically,
                            horizontalArrangement=Arrangement.Center) {
                            AgendaGlyph("ticket", Modifier.size(17.dp), Color.White)
                            Spacer(Modifier.width(7.dp))
                            Text("Entrades",color=Color.White,fontSize=13.sp,
                                fontWeight=FontWeight.SemiBold)
                        }
                    }
                }
                // Non-festivals: exactly four Android-compatible tabs. Festival Horaris
                // is retained for step 26 and gets an independent Entrades tab.
                val tabs = IosEventDetailContract.tabs(ev.isFestival)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(horizontal = 11.dp, vertical=12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tabs.forEach { name ->
                        val icon = when(name) { "Horaris" -> "calendar"; "Entrades" -> "ticket"
                            "Info" -> "info"; "Ubicació" -> "pin"; else -> "people" }
                        val active = section == name
                        Column(Modifier.width(if (ev.isFestival) 78.dp else 86.dp)
                            .border(1.dp, if(active) AgendaGold else Color(0xFFE4E4E4),RoundedCornerShape(13.dp))
                            .background(if(active) Color(0xFFFFFAEB) else Color.White,RoundedCornerShape(13.dp))
                            .clickable { section = name }.padding(vertical=9.dp),
                            horizontalAlignment=Alignment.CenterHorizontally) {
                            AgendaGlyph(icon, Modifier.size(17.dp), AgendaGold)
                            Spacer(Modifier.height(6.dp))
                            Text(name, fontSize=11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Column(Modifier.weight(1f).verticalScroll(detailScrollState).padding(horizontal = 16.dp)) {
                    when (section) {
                        "Info" -> {
                            Text(ev.title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text(listOf(IosUnfilteredFeedPolicy.fullDate(ev.date), ev.time, ev.venue, ev.town)
                                .filter(String::isNotBlank).joinToString(" · "), fontSize = 12.sp, color = Color.Gray)
                            if (ev.artists.isNotEmpty() && !ev.isFestival) {
                                Spacer(Modifier.height(12.dp))
                                Text("Artistes", fontWeight = FontWeight.Bold)
                                ev.artists.distinct().forEach { name ->
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        TextButton(onClick = { onArtist(name) }, modifier = Modifier.weight(1f)) {
                                            Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        TextButton(onClick = { onToggleFollowEntity("ARTIST", name) }) {
                                            Text(if (followsEntity("ARTIST", name)) "✓ Seguint" else "+ Seguir")
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            if (ev.description.isNotBlank()) {
                                Text("Informació", fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(6.dp))
                                Text(ev.description, fontSize = 13.sp)
                                Spacer(Modifier.height(12.dp))
                            }
                            if (ev.organizers.isNotEmpty()) {
                                Text("Organització", fontWeight = FontWeight.Bold)
                                ev.organizers.distinct().forEach { name ->
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text(name, modifier = Modifier.weight(1f), fontSize = 13.sp)
                                        TextButton(onClick = { onToggleFollowEntity("ORGANIZER", name) }) {
                                            Text(if (followsEntity("ORGANIZER", name)) "✓ Seguint" else "+ Seguir")
                                        }
                                    }
                                }
                            }
                            if (ev.category.isNotBlank()) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text("Categoria: ${ev.category}", modifier = Modifier.weight(1f), fontSize = 13.sp)
                                    TextButton(onClick = { onToggleFollowEntity("CATEGORY", ev.category) }) {
                                        Text(if (followsEntity("CATEGORY", ev.category)) "✓ Seguint" else "+ Seguir")
                                    }
                                }
                            }
                            if (ev.artists.isNotEmpty()) {
                                Text("Artistes", fontWeight = FontWeight.Bold)
                                ev.artists.distinct().forEach { a -> TextButton(onClick = { onArtist(a) }) { Text(a) } }
                            }
                            OutlinedButton(onClick = { showBatecManager = true }) {
                                Text("Batecs de l'esdeveniment · Publicar i gestionar")
                            }
                            if (ev.isFestival) TextButton(onClick = { section = "Horaris" }) { Text("Veure l'horari") }
                            OutlinedButton(onClick = { servicesOpen = true }) { Text("Serveis especials") }
                            TextButton(onClick = onReport) { Text("Informar d'un problema en la fitxa") }
                        }
                        "Horaris" -> {
                            if (detailLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
                            if (detailError.isNotBlank()) {
                                Text(detailError, fontSize = 12.sp, color = Color.Gray)
                                TextButton(onClick = { detailReload++ }) { Text("Tornar a consultar l'horari") }
                            }
                            if (festivalDays.size > 1) {
                                Row(Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    festivalDays.forEach { day ->
                                        FilterChip(selected = selectedDay == day, onClick = { selectedDay = day },
                                            label = { Text(IosUnfilteredFeedPolicy.fullDate(day)) })
                                    }
                                }
                            }
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(if (selectedDay.isNotBlank()) IosUnfilteredFeedPolicy.fullDate(selectedDay)
                                    else "Programació", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                FilterChip(selected = !onlyMyPerformances, onClick = { onlyMyPerformances = false },
                                    label = { Text("Tots") })
                                Spacer(Modifier.width(5.dp))
                                FilterChip(selected = onlyMyPerformances, onClick = { onlyMyPerformances = true },
                                    label = { Text("Els meus") })
                            }
                            if (localSaveNote.isNotBlank()) Text(localSaveNote, fontSize = 11.sp, color = Color.Gray)
                            val daySlots = IosFestivalPopupPolicy.performances(festivalProgramme,
                                selectedDay, onlyMyPerformances, savedSlots, ev.id)
                            if (daySlots.isEmpty() && !detailLoading) {
                                Text(when {
                                    festivalDays.isEmpty() && festivalProgramme.isNotEmpty() ->
                                        "Totes les jornades publicades ja han passat."
                                    onlyMyPerformances -> "Encara no has guardat cap actuació d'esta jornada."
                                    else -> "Programació pendent de publicar per a esta jornada."
                                }, color = Color.Gray, fontSize = 13.sp)
                            }
                            var priorGroup = ""
                            daySlots.forEach { slot ->
                                val group = slot.groupName.ifBlank { slot.location }.trim()
                                if (group.isNotBlank() && group != priorGroup) {
                                    Text(group, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                        modifier = Modifier.padding(top = 10.dp, bottom = 3.dp))
                                }
                                priorGroup = group
                                val isMatch = IosFestivalArtistSearch.matchesArtist(slot, matchedArtist)
                                val isSaved = IosFestivalPopupPolicy.saved(ev.id, slot, savedSlots)
                                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    .border(1.dp, if (isMatch || isSaved) AgendaGold else Color(0xFFE4E4E4),
                                        RoundedCornerShape(12.dp))
                                    .background(if (isMatch) Color(0xFFFFFAEB) else Color.White,
                                        RoundedCornerShape(12.dp)).padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(slot.time.ifBlank { "Hora per confirmar" } +
                                            slot.endTime.takeIf(String::isNotBlank)?.let { " – $it" }.orEmpty(),
                                            modifier = Modifier.weight(1f), fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold, color = Color(0xFF946C00))
                                        TextButton(enabled = !slotSaving, onClick = { toggleSlot(slot) }) {
                                            Text(if (isSaved) "✓ Guardat" else "+ Guardar", fontSize = 12.sp)
                                        }
                                    }
                                    val who = slot.artist.ifBlank { slot.title }
                                    TextButton(onClick = { if (slot.artist.isNotBlank()) onArtist(slot.artist) },
                                        enabled = slot.artist.isNotBlank(),
                                        contentPadding = PaddingValues(0.dp)) {
                                        Text(slot.title.ifBlank { who }, fontSize = 14.sp,
                                            fontWeight = if (isMatch) FontWeight.Bold else FontWeight.SemiBold)
                                    }
                                    if (slot.artist.isNotBlank() && !slot.artist.equals(slot.title, true)) {
                                        Text(slot.artist, fontSize = 12.sp)
                                    }
                                    if (slot.location.isNotBlank()) Text(slot.location, fontSize = 11.sp, color = Color.Gray)
                                    if (slot.details.isNotBlank()) Text(slot.details, fontSize = 12.sp,
                                        color = Color.DarkGray, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                    if (slot.artist.isNotBlank()) {
                                        TextButton(onClick = { onToggleFollowEntity("ARTIST", slot.artist) }) {
                                            Text(if (followsEntity("ARTIST", slot.artist)) "✓ Seguint" else "+ Seguir",
                                                fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                            if (savedSlots.isNotEmpty()) Text("Actuacions guardades al teu compte del servidor.",
                                fontSize = 11.sp, color = Color.Gray)
                        }
                        "Entrades" -> {
                            Text("Entrades", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            if (ev.price.isNotBlank()) Text("Preu: ${ev.price} €", fontSize = 13.sp)
                            when {
                                !ev.ticketAvailable -> Text("No hi ha venda d'entrades disponible.", color = Color.Gray)
                                IosEventDetailContract.isSafeExternalTicketUrl(ev.ticketUrl) -> {
                                    Text("Venda externa de l'organitzador.", fontSize = 12.sp, color = Color.Gray)
                                    Button(onClick = onTickets) { Text("Consultar entrades externes") }
                                }
                                ev.ticketUrl.isNotBlank() -> Text("L'enllaç de venda no és segur o no està disponible.", color = Color.Gray)
                                else -> {
                                    Text("Venda interna d'L'Agenda", fontSize = 12.sp, color = Color.Gray)
                                    if (ticketsLoading) CircularProgressIndicator(Modifier.size(22.dp))
                                    if (ticketOptionsError.isNotBlank()) {
                                        Text(ticketOptionsError, color = Color(0xFFAD3030), fontSize = 12.sp)
                                        TextButton(onClick = { ticketsReload++ }) { Text("Tornar a consultar") }
                                    }
                                    val validTicketOptions = IosTicketCatalogPolicy.visible(ticketOptions, culturalToday)
                                    if (ev.isFestival && IosTicketCatalogPolicy.days(validTicketOptions).isNotEmpty()) {
                                        Row(Modifier.horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                            FilterChip(selected = ticketsDayFilter.isBlank(),
                                                onClick = { ticketsDayFilter = "" }, label = { Text("Totes") })
                                            IosTicketCatalogPolicy.days(validTicketOptions).forEach { day ->
                                                FilterChip(selected = ticketsDayFilter == day,
                                                    onClick = { ticketsDayFilter = day },
                                                    label = { Text(IosUnfilteredFeedPolicy.fullDate(day)) })
                                            }
                                        }
                                    }
                                    val listed = IosTicketCatalogPolicy.filter(validTicketOptions,
                                        if (ev.isFestival) ticketsDayFilter else "")
                                    listed.forEach { option ->
                                        Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)
                                            .background(Pale, RoundedCornerShape(9.dp)).padding(10.dp)) {
                                            Text(option.name, fontWeight = FontWeight.SemiBold)
                                            Text(if (option.price == 0.0) "Gratuïta" else "${option.price} €",
                                                fontSize = 12.sp)
                                            val detail = IosTicketCatalogPolicy.details(option)
                                            if (detail.isNotBlank()) Text(detail, fontSize = 12.sp)
                                            option.available?.let { Text(if (it == 0) "Esgotada" else "$it places disponibles", fontSize = 12.sp) }
                                            Text("Màxim ${IosTicketCatalogPolicy.quantityLimit(option)} per reserva ara", fontSize = 11.sp, color = Color.Gray)
                                            if (option.saleStartsAt.isNotBlank() || option.saleEndsAt.isNotBlank())
                                                Text("Venda: ${option.saleStartsAt.ifBlank { "inici no definit" }} — ${option.saleEndsAt.ifBlank { "fi no definit" }}",
                                                    fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }
                                    if (!ticketsLoading && ticketOptionsError.isBlank() && validTicketOptions.isEmpty())
                                        Text("No hi ha modalitats d'entrada disponibles.", fontSize = 12.sp)
                                    Button(enabled = ev.ticketAvailable && !ticketsLoading && ticketOptionsError.isBlank() &&
                                        validTicketOptions.any(IosTicketCatalogPolicy::selectable), onClick = onTickets) {
                                        Text("Seleccionar entrades")
                                    }
                                }
                            }
                        }
                        "Ubicació" -> {
                            Text(ev.venue.ifBlank { ev.town }, fontWeight = FontWeight.Bold)
                            if (ev.venue.isNotBlank() && ev.town.isNotBlank()) Text(ev.town, fontSize = 13.sp)
                            if (ev.venue.isNotBlank()) {
                                TextButton(onClick = { onToggleFollowEntity("VENUE", ev.venue) }) {
                                    Text(if (followsEntity("VENUE", ev.venue)) "✓ Seguint esta sala" else "+ Seguir esta sala")
                                }
                            }
                            if (ev.comarca.isNotBlank()) Text(ev.comarca, fontSize = 12.sp, color = Color.Gray)
                            val places = if (ev.isFestival) IosFestivalPopupPolicy.places(ev.venue, ev.town, festivalProgramme)
                                else listOfNotNull(IosEventDetailContract.placeQuery(ev.venue, ev.town)
                                    .takeIf(String::isNotBlank)?.let { ev.venue.ifBlank { ev.town } to it })
                            if (places.isEmpty()) {
                                Text("L'organització no ha publicat una ubicació verificable.", fontSize = 12.sp)
                            } else places.forEach { (name, search) ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Text(name, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    TextButton(onClick = { mapDestination = search }) { Text("Com arribar") }
                                }
                            }
                        }
                        "Comunitat" -> {
                            Text("Taulons d'este esdeveniment", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            if (userId == null) {
                                Text("Connectant el perfil de convidat…", fontSize = 12.sp, color = Color.Gray)
                            } else {
                                if (communityLoading) CircularProgressIndicator(Modifier.size(22.dp))
                                if (communityError.isNotBlank()) {
                                    Text(communityError, fontSize = 12.sp, color = Color(0xFFAD3030))
                                    TextButton(onClick = { communityReload++ }) { Text("Tornar a consultar") }
                                }
                                if (!communityLoading && communityError.isBlank() && eventGroups.isEmpty()) {
                                    Text("Encara no hi ha taulons per a este esdeveniment.", fontSize = 12.sp, color = Color.Gray)
                                }
                                eventGroups.forEach { group ->
                                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                        .border(1.dp, Color(0xFFE4E4E4), RoundedCornerShape(12.dp))
                                        .clickable { onOpenCommunityGroup(group) }.padding(11.dp)) {
                                        Text(group.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text(IosEventDetailContract.communityTypeLabel(group.type) +
                                            " · " + if (group.access == "OPEN") "Obert" else "Amb aprovació",
                                            fontSize = 11.sp, color = Color.Gray)
                                        if (group.description.isNotBlank()) Text(group.description, fontSize = 12.sp,
                                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        if (group.participants > 0) Text("${group.participants} participants", fontSize = 11.sp)
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = onCreateGroup, modifier = Modifier.weight(1f)) { Text("Crear tauló") }
                                    OutlinedButton(onClick = onCommunity, modifier = Modifier.weight(1f)) {
                                        Text("Veure tots")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (servicesOpen) IosSpecialServicesDialog(batecApi, ev.id,
        onLogin = onBatecSignIn, onDismiss = { servicesOpen = false })
    if (showBatecManager) BatecManagementDialog(
        eventId = ev.id, api = batecApi, ownerId = userId, canPublish = canPublishBatec,
        onSignIn = onBatecSignIn, onChanged = onBatecsChanged,
        onDismiss = { showBatecManager = false }
    )
    if (showPoster) Dialog(onDismissRequest = { showPoster = false }) {
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.85f).clickable { showPoster = false }) {
            Poster(ev.posterUrl, Modifier.fillMaxSize())
        }
    }
    if (mapDestination.isNotBlank()) {
        AlertDialog(onDismissRequest = { mapDestination = "" },
            title = { Text("Obrir les indicacions?") },
            text = { Text("S'obrirà Mapes fora de L'Agenda per a esta ubicació.") },
            confirmButton = { TextButton(onClick = {
                val dest = mapDestination
                mapDestination = ""
                platformOpenExternal("https://maps.apple.com/?q=${dest.encodeURLParameter()}")
            }) { Text("Obrir Mapes") } },
            dismissButton = { TextButton(onClick = { mapDestination = "" }) { Text("Cancel·lar") } })
    }
}
@Composable
private fun ArtistDialog(api: IosAgendaApi, name: String, followed: Boolean, concerts: List<AgendaEventSummary>,
                         onLogin: () -> Unit, onStorePaid: () -> Unit,
                         publicPage: IosArtistPublicPage?, pageLoading: Boolean, pageError: String,
                         onRetryPage: () -> Unit, onFollow: () -> Unit,
                         bell: IosArtistBell?, onBell: () -> Unit,
                         onDismiss: () -> Unit, onEvent: (AgendaEventSummary) -> Unit) {
    var selectedStoreProduct by remember(name) { mutableStateOf<IosArtistProduct?>(null) }
    var donationOpen by remember(name) { mutableStateOf(false) }
    val discipline = IosArtistPagePolicy.discipline(concerts.map { it.category })
    val photo = publicPage?.profile?.imageUrl.orEmpty()
    val style = publicPage?.profile?.style.orEmpty()
    // Search links are not represented as official artist-controlled profiles.
    val platforms = if (IosArtistPagePolicy.isMusic(discipline)) {
        val query = name.encodeURLParameter()
        listOf("Spotify" to "https://open.spotify.com/search/$query",
            "YouTube Music" to "https://music.youtube.com/search?q=$query",
            "Apple Music" to "https://music.apple.com/es/search?term=$query",
            "Deezer" to "https://www.deezer.com/search/$query",
            "TIDAL" to "https://listen.tidal.com/search?q=$query",
            "Bandcamp" to "https://bandcamp.com/search?q=$query")
    } else emptyList()
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White,
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f)) {
            LazyColumn(contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Box(Modifier.fillMaxWidth().height(224.dp)
                        .background(Color(0xFFF0ECF4), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))) {
                        if (photo.isNotBlank()) {
                            AsyncImage(model = if (photo.startsWith("/"))
                                IosServerConnection.SITE_BASE + photo else photo,
                                contentDescription = "Fotografia de $name",
                                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            // Never mislabel a festival poster as a portrait of one artist.
                            Text(name.take(2).uppercase(), fontSize = 48.sp, color = Ink.copy(alpha = .35f),
                                modifier = Modifier.align(Alignment.Center))
                        }
                        if (discipline.isNotBlank()) Text(discipline.replaceFirstChar { it.uppercase() },
                            color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                                .background(Ink.copy(alpha = .75f), RoundedCornerShape(9.dp))
                                .padding(horizontal = 9.dp, vertical = 6.dp))
                        TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopStart)
                            .padding(6.dp).background(Color.White.copy(alpha = .90f), CircleShape)) {
                            Text("×", color = Ink, fontSize = 22.sp)
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = onFollow, modifier = Modifier.weight(1f)) {
                            Text(if (followed) "✓ Seguint" else "+ Seguir")
                        }
                        OutlinedButton(onClick = onBell, modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 7.dp)) {
                            Text(if (bell != null) "🔔 ${IosArtistBellPolicy.label(bell.frequency)}" else "🔔 Notificacions",
                                fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
                item {
                    Column(Modifier.padding(horizontal = 17.dp, vertical = 2.dp)) {
                        Text(name, fontSize = 25.sp, fontWeight = FontWeight.Black, color = Ink)
                        if (style.isNotBlank()) Text(style, fontSize = 12.sp, color = Color.Gray)
                        if (pageLoading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
                        if (pageError.isNotBlank()) {
                            Text(pageError, color = Color(0xFF9C3535), fontSize = 12.sp)
                            TextButton(onClick = onRetryPage) { Text("Reintentar la fitxa") }
                        }
                    }
                }
                if (platforms.isNotEmpty()) item {
                    Column {
                        Text("Buscar música", fontSize = 11.sp, color = Color.Gray,
                            modifier = Modifier.padding(start = 17.dp))
                        LazyRow(contentPadding = PaddingValues(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(platforms) { (label, url) ->
                                OutlinedButton(onClick = { platformOpenExternal(url) },
                                    contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp)) {
                                    Text(label, fontSize = 10.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                }
                item { Text(IosArtistPagePolicy.activityHeading(discipline),
                    fontWeight = FontWeight.Black, fontSize = 19.sp,
                    modifier = Modifier.padding(start = 17.dp, top = 8.dp)) }
                if (concerts.isEmpty()) item {
                    Text("No hi ha activitats futures publicades.",
                        modifier = Modifier.padding(horizontal = 17.dp), color = Color.Gray, fontSize = 12.sp)
                }
                items(concerts.distinctBy { it.id }, key = { "artist_event_${it.id}" }) { ev ->
                    Box(Modifier.padding(horizontal = 12.dp)) {
                        EventCard(ev, isSaved = false, onClick = { onEvent(ev) }, trailing = {})
                    }
                }
                if (IosArtistPagePolicy.isMusic(discipline)) item {
                    OutlinedButton(onClick = { donationOpen = true },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 17.dp)) {
                        Text("Donacions a l'artista")
                    }
                }
                if (IosArtistPagePolicy.isMusic(discipline) || !publicPage?.products.isNullOrEmpty()) {
                    item { Text("Productes", fontWeight = FontWeight.Black, fontSize = 19.sp,
                        modifier = Modifier.padding(start = 17.dp, top = 9.dp)) }
                    when {
                        pageLoading -> item { Text("Carregant productes...",
                            modifier = Modifier.padding(horizontal = 17.dp), color = Color.Gray) }
                        publicPage?.storeMessage?.isNotBlank() == true -> item {
                            Text(publicPage.storeMessage, color = Color.Gray, fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 17.dp))
                            TextButton(onClick = onRetryPage) { Text("Reintentar") }
                        }
                        publicPage?.products.isNullOrEmpty() -> item {
                            Text("No hi ha productes publicats per a este artista.", color = Color.Gray,
                                fontSize = 12.sp, modifier = Modifier.padding(horizontal = 17.dp))
                        }
                        else -> items(publicPage!!.products.filter { it.active }, key = { "product_${it.id}" }) { product ->
                            Row(Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (product.imageUrl.isNotBlank()) AsyncImage(
                                    model = if (product.imageUrl.startsWith("/"))
                                        IosServerConnection.SITE_BASE + product.imageUrl else product.imageUrl,
                                    contentDescription = product.name,
                                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop)
                                Column(Modifier.weight(1f)) {
                                    Text(product.name, fontWeight = FontWeight.SemiBold)
                                    if (product.description.isNotBlank()) Text(product.description,
                                        maxLines = 2, fontSize = 11.sp, color = Color.Gray)
                                    Text(IosArtistPagePolicy.money(product.priceCents),
                                        fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    if (!IosArtistPagePolicy.inStock(product.stock))
                                        Text("Esgotat", color = Color.Gray, fontSize = 11.sp)
                                    else OutlinedButton(onClick = { selectedStoreProduct = product }) {
                                        Text("Comprar")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (donationOpen) IosArtistDonationCheckout(api, name,
        onLogin = { donationOpen = false; onLogin() }, onDismiss = { donationOpen = false })
    selectedStoreProduct?.let { product ->
        IosArtistStoreCheckout(api, product,
            onClose = { selectedStoreProduct = null },
            onLogin = { selectedStoreProduct = null; onLogin() },
            onPaid = { selectedStoreProduct = null; onStorePaid() })
    }
}

/** One independent artist bell per account; never assume push works until APNs is configured. */
@Composable
private fun ArtistBellEditorDialog(artist: String, current: IosArtistBell?,
        onDismiss: () -> Unit, onSave: (Boolean, String) -> Unit) {
    var frequency by remember(artist, current?.frequency) { mutableStateOf(current?.frequency ?: "WEEKLY") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Avisos de $artist") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Avisa'm de noves activitats d'este artista o grup.", fontSize = 13.sp)
                IosArtistBellPolicy.frequencies.sortedBy { listOf("IMMEDIATE", "DAILY", "WEEKLY").indexOf(it) }
                    .forEach { value ->
                        Row(Modifier.fillMaxWidth().clickable { frequency = value },
                            verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = frequency == value, onClick = { frequency = value })
                            Text(IosArtistBellPolicy.label(value))
                        }
                    }
                Text("Els avisos fora de l'app requerixen permís iOS i l'emissor APNs del servidor actiu.",
                    fontSize = 11.sp, color = Color.Gray)
            }
        }, confirmButton = {
            TextButton(onClick = { onSave(true, frequency) }) { Text("Guardar freqüència") }
        }, dismissButton = {
            Row {
                if (current != null) TextButton(onClick = { onSave(false, frequency) }) { Text("Desactivar") }
                TextButton(onClick = onDismiss) { Text("Cancel·lar") }
            }
        })
}

/** Pas 20: manual Batecs are owned by the PHP server, never cached as personal phone data.
 * The editor uses explicit online-only writes because create.php has no idempotency key.
 */
@Composable
private fun BatecManagementDialog(
    eventId: Int, api: IosAgendaApi, ownerId: Int?, canPublish: Boolean,
    onSignIn: () -> Unit, onChanged: () -> Unit, onDismiss: () -> Unit
) {
    var items by remember(eventId, ownerId) { mutableStateOf(emptyList<IosManualBatec>()) }
    var eligibility by remember(eventId, ownerId) { mutableStateOf<IosBatecEligibility?>(null) }
    var busy by remember(eventId, ownerId) { mutableStateOf(false) }
    var message by remember(eventId, ownerId) { mutableStateOf("") }
    var editingId by remember(eventId, ownerId) { mutableStateOf<Int?>(null) }
    var deletingId by remember(eventId, ownerId) { mutableStateOf<Int?>(null) }
    var title by remember(eventId, ownerId) { mutableStateOf("") }
    var body by remember(eventId, ownerId) { mutableStateOf("") }
    var refresh by remember(eventId, ownerId) { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(eventId, ownerId, canPublish, refresh) {
        val next = runCatching { api.eventBatecs(eventId) }
        next.onSuccess { items = it }.onFailure { message = it.message ?: "No s'han pogut carregar els Batecs" }
        if (canPublish) {
            runCatching { api.batecEligibility(eventId) }
                .onSuccess { eligibility = it }
                .onFailure { eligibility = null; message = it.message ?: "No es poden comprovar els permisos" }
        } else eligibility = null
    }
    Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
        Surface(shape = RoundedCornerShape(18.dp), color = Color.White,
            modifier = Modifier.fillMaxWidth().fillMaxHeight(.82f)) {
            Column(Modifier.fillMaxSize().padding(15.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Batecs de l'esdeveniment", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    TextButton(onClick = onDismiss, enabled = !busy) { Text("Tancar") }
                }
                if (message.isNotBlank()) Text(message, color = Color(0xFF925100), fontSize = 12.sp)
                if (editingId != null) {
                    Text(if (editingId == 0) "Publicar un Batec" else "Editar un Batec", fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = title, onValueChange = { title = it.take(120) },
                        label = { Text("Títol") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = body, onValueChange = { body = it.take(1000) },
                        label = { Text("Text") }, modifier = Modifier.fillMaxWidth(), maxLines = 5)
                    Row {
                        TextButton(onClick = { editingId = null }, enabled = !busy) { Text("Cancel·lar") }
                        Button(onClick = {
                            val currentId = editingId ?: return@Button
                            busy = true
                            scope.launch {
                                val result = runCatching {
                                    if (currentId == 0) api.createBatec(eventId, title, body)
                                    else api.updateBatec(currentId, eventId, title, body)
                                }
                                result.onSuccess {
                                    editingId = null; message = "Batec guardat al servidor"
                                    refresh += 1; onChanged()
                                }.onFailure { message = it.message ?: "No s'ha guardat el Batec" }
                                busy = false
                            }
                        }, enabled = !busy && title.isNotBlank()) { Text("Guardar") }
                    }
                } else {
                    if (!canPublish) {
                        OutlinedButton(onClick = onSignIn) { Text("Inicia sessió per publicar un Batec") }
                    } else {
                        val permission = eligibility
                        if (permission == null) Text("Comprovant autorització…", fontSize = 12.sp)
                        else if (permission.allowed) Button(onClick = {
                            editingId = 0; title = ""; body = ""; message = ""
                        }) { Text("+ Crear Batec") }
                        else Text(permission.reason.ifBlank { "No es pot publicar ara" }, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        if (items.isEmpty()) Text("No hi ha Batecs publicats.", color = Color.Gray)
                        items.forEach { item ->
                            Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)
                                .background(Pale, RoundedCornerShape(10.dp)).padding(10.dp)) {
                                Text(item.title, fontWeight = FontWeight.SemiBold)
                                if (item.body.isNotBlank()) Text(item.body, fontSize = 12.sp)
                                Text(item.author, fontSize = 11.sp, color = Color.Gray)
                                if (item.canEdit && canPublish) {
                                    Row {
                                        TextButton(onClick = {
                                            editingId = item.id; title = item.title; body = item.body; message = ""
                                        }, enabled = !busy) { Text("Editar") }
                                        TextButton(onClick = { deletingId = item.id }, enabled = !busy) { Text("Eliminar") }
                                    }
                                }
                                if (deletingId == item.id) {
                                    Text("Eliminar este Batec definitivament?", fontSize = 12.sp)
                                    Row {
                                        TextButton(onClick = { deletingId = null }) { Text("Cancel·lar") }
                                        TextButton(enabled = !busy, onClick = {
                                            busy = true
                                            scope.launch {
                                                runCatching { api.deleteBatec(item.id, eventId) }
                                                    .onSuccess { deletingId = null; message = "Batec eliminat del servidor"
                                                        refresh += 1; onChanged() }
                                                    .onFailure { message = it.message ?: "No s'ha pogut eliminar" }
                                                busy = false
                                            }
                                        }) { Text("Confirmar eliminació") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Exact Android v336 seeded palettes; no image files or local user state. */
private fun batecArtistBrush(seed: String): Brush {
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
    return Brush.linearGradient(palettes[(seed.hashCode() and Int.MAX_VALUE) % palettes.size])
}

private fun batecStoryRingBrush(seed: String): Brush = Brush.sweepGradient(
    listOf(Color(0xFFFCAF45), Color(0xFFFD1D1D),
        if (seed.hashCode() % 2 == 0) Color(0xFF833AB4) else Color(0xFFFD1D1D),
        Color(0xFFFCAF45))
)

/** Android v336 story bubble: artist ring, grey seen state, count and followed dot. */
@Composable
private fun BatecBubble(
    group: IosBatecArtistGroup,
    photoUrl: String,
    onClick: () -> Unit
) {
    val grey = group.allSeen
    val ring = if (grey) Brush.linearGradient(listOf(Color(0xFF9E9E9E), Color(0xFFBDBDBD)))
               else batecStoryRingBrush(group.artist)
    val interior = if (grey) Brush.linearGradient(listOf(Color(0xFF757575), Color(0xFFB0B0B0)))
                   else batecArtistBrush(group.artist)
    Column(
        modifier = Modifier.width(64.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(54.dp).border(2.dp, ring, CircleShape)
                .padding(3.dp).clip(CircleShape).background(interior),
            contentAlignment = Alignment.Center
        ) {
            if (photoUrl.isNotBlank()) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = "Batec de ${group.artist}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    colorFilter = if (grey) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null
                )
            } else {
                val initials = group.artist.split(" ").filter(String::isNotBlank).take(2)
                    .joinToString("") { it.first().uppercaseChar().toString() }
                Text(initials, color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
            }
            if (group.storyKeys.size > 1) {
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).size(18.dp)
                        .background(Color.White, CircleShape).border(1.dp, Color.LightGray, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (group.storyKeys.size > 99) "99+" else group.storyKeys.size.toString(),
                        color = Ink, fontSize = if (group.storyKeys.size > 99) 7.sp else 9.sp,
                        fontWeight = FontWeight.Black)
                }
            }
            // Every story here belongs to a followed artist; Android marks this in blue.
            Box(
                modifier = Modifier.align(Alignment.BottomEnd).size(13.dp)
                    .background(Color(0xFF1D9BF0), CircleShape)
                    .border(2.dp, Color.White, CircleShape)
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(group.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 10.sp,
            color = if (grey) Ink.copy(alpha = .48f) else Ink)
    }
}

/** One artist per visual Batec, with stories and current concerts inside its viewer. */
@Composable
private fun BatecViewer(
    groups: List<IosBatecArtistGroup>,
    selectedKey: String,
    stories: List<IosBatec>,
    allEvents: List<AgendaEventSummary>,
    isFollowed: (String) -> Boolean,
    onToggleFollow: (String) -> Unit,
    onDismiss: () -> Unit,
    onOpenEvent: (Int) -> Unit,
    onGroupChanged: (IosBatecArtistGroup) -> Unit
) {
    val selectedIndex = groups.indexOfFirst { it.key == selectedKey }.coerceAtLeast(0)
    val group = groups[selectedIndex]
    val groupKeys = remember(group.storyKeys) { group.storyKeys.toSet() }
    val groupStories = remember(stories, groupKeys) { stories.filter { it.storyKey in groupKeys } }
    val representative = groupStories.firstOrNull { it.storyKey == group.openingStoryKey }
        ?: groupStories.firstOrNull()
        ?: return
    val upcoming = remember(allEvents, group.key) {
        allEvents.asSequence().filter { ev ->
            (ev.artists.any { it.trim().equals(group.artist.trim(), ignoreCase = true) } ||
             ev.schedule.any { it.artist.trim().equals(group.artist.trim(), ignoreCase = true) }) &&
             (ev.endDate.ifBlank { ev.date }.isBlank() || ev.endDate.ifBlank { ev.date } >= currentLocalDateIso())
        }.distinctBy { it.id }.sortedWith(compareBy<AgendaEventSummary> { it.date }.thenBy { it.time }).take(12).toList()
    }
    var replayEpoch by remember(selectedKey) { mutableIntStateOf(0) }
    var progress by remember(selectedKey, replayEpoch) { mutableFloatStateOf(0f) }
    fun go(delta: Int) {
        when (val action = IosBatecNavigationPolicy.navigate(selectedIndex, groups.size, delta)) {
            IosBatecNavigationPolicy.Navigation.Restart -> replayEpoch += 1
            IosBatecNavigationPolicy.Navigation.Close -> onDismiss()
            is IosBatecNavigationPolicy.Navigation.Select -> onGroupChanged(groups[action.index])
        }
    }
    LaunchedEffect(selectedKey, replayEpoch) {
        progress = 0f
        repeat(150) { step ->
            kotlinx.coroutines.delay(100)
            progress = (step + 1) / 150f
        }
        go(1) // fifteen seconds, then the next group or close after the last.
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp), color = Color(0xFF1D1921),
            modifier = Modifier.fillMaxWidth().aspectRatio(9f / 16f)
        ) {
            var horizontalDrag by remember(selectedKey) { mutableFloatStateOf(0f) }
            Box(
                Modifier.fillMaxSize()
                    .pointerInput(selectedKey, groups.size) {
                        detectTapGestures { p ->
                            go(IosBatecNavigationPolicy.tapDirection(p.x, size.width.toFloat()))
                        }
                    }
                    .pointerInput(selectedKey, groups.size) {
                        detectHorizontalDragGestures(
                            onDragStart = { horizontalDrag = 0f },
                            onHorizontalDrag = { change, delta -> horizontalDrag += delta; change.consume() },
                            onDragEnd = {
                                val direction = IosBatecNavigationPolicy.swipeDirection(horizontalDrag, 56.dp.toPx())
                                if (direction != 0) go(direction)
                                horizontalDrag = 0f
                            },
                            onDragCancel = { horizontalDrag = 0f }
                        )
                    }
            ) {
                val artwork = representative.imageUrl.ifBlank { upcoming.firstOrNull()?.posterUrl.orEmpty() }
                Box(Modifier.fillMaxSize().background(batecArtistBrush(group.artist)))
                if (artwork.isNotBlank()) {
                    AsyncImage(model = artwork, contentDescription = "Batec de ${group.artist}",
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .50f)))
                Column(Modifier.fillMaxSize().padding(14.dp)) {
                    Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = .26f))) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(progress.coerceIn(0f, 1f))
                            .background(Color.White))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(39.dp).clip(CircleShape).background(Accent)) {
                            Poster(representative.imageUrl, Modifier.fillMaxSize().clip(CircleShape))
                        }
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text(group.artist, color = Color.White, fontWeight = FontWeight.Bold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val statusText = when {
                                groupStories.all { it.sourceType == "FOLLOWED_PLACEHOLDER" } -> "Sense novetats"
                                groupStories.all { it.sourceType == "EVENT_FALLBACK" } -> "Pròxima actuació"
                                else -> "${group.storyKeys.size} novetat${if (group.storyKeys.size == 1) "" else "s"}"
                            }
                            Text("$statusText · ${selectedIndex + 1}/${groups.size}",
                                color = Color.White.copy(alpha = .72f), fontSize = 11.sp)
                        }
                        TextButton(onClick = onDismiss) { Text("✕", color = Color.White, fontSize = 20.sp) }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text("Pròximes actuacions", color = Color.White.copy(alpha = .8f),
                        fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(group.artist, color = Color.White, fontWeight = FontWeight.Black,
                        fontSize = 25.sp, maxLines = 2)
                    Spacer(Modifier.height(14.dp))
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        if (upcoming.isNotEmpty()) {
                            items(upcoming, key = { "batec_concert_${it.id}" }) { ev ->
                                Row(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = .15f))
                                        .clickable { onOpenEvent(ev.id) }.padding(9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Poster(ev.posterUrl, Modifier.size(56.dp).clip(RoundedCornerShape(7.dp)))
                                    Spacer(Modifier.width(9.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(ev.title, color = Color.White, fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp, maxLines = 2)
                                        Text("${ev.date} ${ev.time} · ${ev.town}", color = Color.White.copy(alpha = .8f),
                                            fontSize = 11.sp, maxLines = 2)
                                    }
                                    Text("›", color = Color.White, fontSize = 27.sp)
                                }
                            }
                        } else {
                            item {
                                Text(representative.text.ifBlank { "No hi ha actuacions pròximes disponibles." },
                                    color = Color.White, fontSize = 15.sp)
                            }
                        }
                    }
                    if (groupStories.size > 1) {
                        Text("${groupStories.size} publicacions d’este artista", color = Color.White.copy(alpha = .73f),
                            fontSize = 11.sp, modifier = Modifier.padding(top = 7.dp))
                    }
                    OutlinedButton(
                        onClick = { onToggleFollow(group.artist) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = .65f))
                    ) {
                        Text(if (isFollowed(group.artist)) "Seguint" else "Seguir ${group.artist}")
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { go(-1) }) { Text("‹ Anterior", color = Color.White) }
                        TextButton(onClick = { go(1) }) {
                            Text(if (selectedIndex == groups.lastIndex) "Tancar" else "Següent ›", color = Color.White)
                        }
                    }
                }
                // Full-height edge targets are above the scrollable content, but below
                // the toolbar and footer. Back is absent on the first item (tap still replays).
                if (selectedIndex > 0) {
                    Box(Modifier.align(Alignment.CenterStart).width(56.dp).fillMaxHeight()
                        .padding(top = 72.dp, bottom = 48.dp).clickable { go(-1) },
                        contentAlignment = Alignment.Center) {
                        Text("‹", modifier = Modifier.background(Color.Black.copy(alpha = .30f), CircleShape)
                            .padding(horizontal = 9.dp), color = Color.White, fontSize = 36.sp)
                    }
                }
                Box(Modifier.align(Alignment.CenterEnd).width(56.dp).fillMaxHeight()
                    .padding(top = 72.dp, bottom = 48.dp).clickable { go(1) },
                    contentAlignment = Alignment.Center) {
                    Text("›", modifier = Modifier.background(Color.Black.copy(alpha = .30f), CircleShape)
                        .padding(horizontal = 9.dp), color = Color.White, fontSize = 36.sp)
                }
            }
        }
    }
}

@Composable
private fun IosSettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical=2.dp),
        verticalAlignment=Alignment.CenterVertically) {
        Text(label,Modifier.weight(1f),fontSize=13.sp)
        Switch(checked=checked,onCheckedChange=onChange)
    }
}

/** Android-equivalent full-width notification sheet; no fabricated notifications. */
@Composable
private fun NotificationsDialog(
    notifications: List<IosNotification>,
    artistBells: List<IosArtistBell>,
    reminders: List<AgendaEventSummary>,
    eventsById: Map<Int, AgendaEventSummary>,
    onDismiss: () -> Unit,
    onRead: (IosNotification) -> Unit,
    onReminderClick: (AgendaEventSummary) -> Unit,
    onReminderDisable: (AgendaEventSummary) -> Unit,
    onArtistBell: (String) -> Unit,
    reminderTime: (AgendaEventSummary) -> String,
    settings: IosNotificationCenterSettings,
    isRegistered: Boolean,
    savingSettings: Boolean,
    onSaveSettings: (IosNotificationCenterSettings) -> Unit,
    onClearRead: () -> Unit,
    onOpenNativeSettings: () -> Unit
) {
    var view by remember { mutableStateOf(IosNotificationGrouping.GENERAL) }
    var draft by remember(settings) { mutableStateOf(settings) }
    val tabs = listOf("General" to "bell", "Pròxim" to "clock", "Nous" to "info",
        "Entrades" to "ticket", "Suggerències" to "search", "Comunitat" to "people")
    val grouped = remember(notifications) { notifications.groupBy {
        IosNotificationGrouping.fromDeepLinkType(it.linkType) } }
    val shown = if (view == IosNotificationGrouping.GENERAL) notifications else grouped[view].orEmpty()
    val pending = notifications.count { !it.read }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Surface(color = Color.White, shape = RoundedCornerShape(topStart=21.dp,topEnd=21.dp),
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.94f)) {
                Column {
                    Row(Modifier.fillMaxWidth().padding(start=18.dp,end=14.dp,top=15.dp,bottom=5.dp),
                        verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Notificacions", fontSize=23.sp,fontWeight=FontWeight.Bold)
                            Text("$pending sense llegir", fontSize=12.sp, color=Color.Gray)
                        }
                        AgendaTopAction("clock", onClick={ view="Recordatoris" },
                            badge=reminders.size+artistBells.size)
                        AgendaTopAction("sliders", onClick={ view="Configuració" })
                        AgendaTopAction("close", onClick=onDismiss)
                    }
                    Row(Modifier.fillMaxWidth().padding(horizontal=5.dp,vertical=9.dp)) {
                        tabs.forEach { (label,symbol) ->
                            val active = view==label
                            val unread = grouped[label].orEmpty().count{!it.read}
                            Column(Modifier.weight(1f).clickable { view=label }
                                .padding(vertical=7.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                                Box(contentAlignment=Alignment.TopEnd) {
                                    AgendaGlyph(symbol,Modifier.size(22.dp),if(active) AgendaBlack else Color(0xFF555555))
                                    if(unread>0) Box(Modifier.offset(x=8.dp,y=(-5).dp).size(15.dp)
                                        .background(AgendaBlack,CircleShape), contentAlignment=Alignment.Center) {
                                        Text(unread.coerceAtMost(9).toString(),color=Color.White,fontSize=8.sp)
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                Box(Modifier.fillMaxWidth().height(2.dp)
                                    .background(if(active) AgendaBlack else Color.Transparent))
                            }
                        }
                    }
                    Text(view, modifier=Modifier.fillMaxWidth().padding(vertical=7.dp),
                        textAlign=androidx.compose.ui.text.style.TextAlign.Center,
                        fontSize=12.sp, fontWeight=FontWeight.SemiBold)
                    LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(horizontal=10.dp,vertical=6.dp),
                        verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        if (view == "Configuració") {
                            item { Text("Campaneta de l'app", fontWeight=FontWeight.Bold, fontSize=17.sp) }
                            item { IosSettingSwitch("Novetats d'artistes amb campaneta", draft.followed) { draft=draft.copy(followed=it) } }
                            item { IosSettingSwitch("Esdeveniments guardats pròxims", draft.dayBefore) { draft=draft.copy(dayBefore=it) } }
                            item { IosSettingSwitch("Compres d'entrades", draft.tickets) { draft=draft.copy(tickets=it) } }
                            item { IosSettingSwitch("Comunitat", draft.community) { draft=draft.copy(community=it) } }
                            item { IosSettingSwitch("Suggerències culturals", draft.suggestions) { draft=draft.copy(suggestions=it) } }
                            item { IosSettingSwitch("Canvis d'activitats", draft.changes) { draft=draft.copy(changes=it) } }
                            item { IosSettingSwitch("Població preferida", draft.homeTown) { draft=draft.copy(homeTown=it) } }
                            item { IosSettingSwitch("Comarca preferida", draft.homeComarca) { draft=draft.copy(homeComarca=it) } }
                            item { IosSettingSwitch("Estils preferits", draft.favoriteStyle) { draft=draft.copy(favoriteStyle=it) } }
                            item { HorizontalDivider() }
                            item { Text("Correus electrònics", fontWeight=FontWeight.Bold, fontSize=17.sp) }
                            if (isRegistered) {
                                item { Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                                    listOf("NONE","DAILY","WEEKLY").forEach { freq ->
                                        FilterChip(selected=draft.emailFrequency==freq,
                                            onClick={draft=draft.copy(emailFrequency=freq)},
                                            label={Text(when(freq){"NONE"->"Cap";"DAILY"->"Diari";else->"Setmanal"})})
                                    }
                                } }
                                item { IosSettingSwitch("Recomanacions",draft.emailRecommendations){draft=draft.copy(emailRecommendations=it)} }
                                item { IosSettingSwitch("Artistes seguits",draft.emailArtists){draft=draft.copy(emailArtists=it)} }
                                item { IosSettingSwitch("Recordatoris",draft.emailReminders){draft=draft.copy(emailReminders=it)} }
                                item { IosSettingSwitch("Publicitat personalitzada",draft.emailAds){draft=draft.copy(emailAds=it)} }
                            } else item { Text("Per rebre correus has d'iniciar sessió.",fontSize=12.sp) }
                            item { HorizontalDivider() }
                            item { Text("Notificacions iPhone",fontWeight=FontWeight.Bold,fontSize=17.sp) }
                            item { IosSettingSwitch("Rebre notificacions push en este iPhone",draft.pushEnabled){draft=draft.copy(pushEnabled=it)} }
                            item { IosSettingSwitch("Respectar les hores de descans",draft.quietEnabled){draft=draft.copy(quietEnabled=it)} }
                            if (draft.quietEnabled) item {
                                Column {
                                    Text("Descans: ${IosNotificationCenterSettings.displayQuietTime(draft.quietStart)} – ${IosNotificationCenterSettings.displayQuietTime(draft.quietEnd)}",fontSize=12.sp)
                                    Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                                        listOf(22*60,23*60,0).forEach { start ->
                                            FilterChip(selected=draft.quietStart==start,
                                                onClick={draft=draft.copy(quietStart=start)},label={Text(IosNotificationCenterSettings.displayQuietTime(start))})
                                        }
                                    }
                                    Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                                        listOf(7*60,8*60,9*60).forEach { end ->
                                            FilterChip(selected=draft.quietEnd==end,
                                                onClick={draft=draft.copy(quietEnd=end)},label={Text(IosNotificationCenterSettings.displayQuietTime(end))})
                                        }
                                    }
                                }
                            }
                            item { TextButton(onClick=onOpenNativeSettings) { Text("Permisos de notificacions de l'iPhone") } }
                            item { Button(onClick={onSaveSettings(draft)},enabled=!savingSettings,
                                modifier=Modifier.fillMaxWidth()) {
                                Text(if(savingSettings) "Guardant…" else "Guardar configuració")
                            } }
                            item { Text("Les preferències es guarden al servidor; els permisos i els enviaments push depenen d'APNs.",fontSize=11.sp,color=Color.Gray) }
                        } else if (view == "Recordatoris") {
                            items(reminders,key={"rem_${it.id}"}) { ev ->
                                Row(Modifier.fillMaxWidth().background(AgendaChrome,RoundedCornerShape(13.dp))
                                    .padding(10.dp),verticalAlignment=Alignment.CenterVertically) {
                                    Poster(ev.posterUrl,Modifier.size(width=65.dp,height=78.dp)
                                        .clip(RoundedCornerShape(9.dp)))
                                    Column(Modifier.weight(1f).clickable{onReminderClick(ev)}
                                        .padding(start=10.dp)) {
                                        Text(ev.title,fontWeight=FontWeight.Bold,fontSize=14.sp,maxLines=2)
                                        Text("${ev.town} · ${ev.venue}",fontSize=11.sp,maxLines=1,color=Color.Gray)
                                        Text(reminderTime(ev).replace('T',' '),fontSize=11.sp)
                                    }
                                    AgendaTopAction("bell",onClick={onReminderDisable(ev)})
                                }
                            }
                            items(artistBells,key={"bell_"+IosArtistBellPolicy.key(it.artistName)}) { item ->
                                Row(Modifier.fillMaxWidth().background(AgendaChrome,RoundedCornerShape(13.dp))
                                    .clickable{onArtistBell(item.artistName)}.padding(15.dp),
                                    verticalAlignment=Alignment.CenterVertically) {
                                    Text(item.artistName,Modifier.weight(1f),fontWeight=FontWeight.Bold)
                                    Text(IosArtistBellPolicy.label(item.frequency),fontSize=12.sp)
                                    AgendaGlyph("bell",Modifier.size(20.dp))
                                }
                            }
                            if(reminders.isEmpty() && artistBells.isEmpty()) item {
                                EmptyState("No tens recordatoris actius.")
                            }
                        } else {
                            if (view == IosNotificationGrouping.UPCOMING) {
                                items(reminders, key={ "upcoming_${it.id}" }) { ev ->
                                    Row(Modifier.fillMaxWidth().background(AgendaChrome,RoundedCornerShape(13.dp))
                                        .padding(10.dp), verticalAlignment=Alignment.CenterVertically) {
                                        Poster(ev.posterUrl,Modifier.width(58.dp).height(70.dp)
                                            .clip(RoundedCornerShape(8.dp)))
                                        Column(Modifier.weight(1f).clickable { onReminderClick(ev) }.padding(start=10.dp)) {
                                            Text(ev.title, fontWeight=FontWeight.Bold,fontSize=13.sp,maxLines=2)
                                            Text(reminderTime(ev).replace('T',' '),fontSize=11.sp)
                                            Text("Recordatori actiu",color=Color.Gray,fontSize=10.sp)
                                        }
                                        AgendaTopAction("bell",onClick={onReminderDisable(ev)})
                                    }
                                }
                            }
                            items(shown,key={it.id}) { notice ->
                                val ev = eventsById[notice.linkValue.toIntOrNull()]
                                    ?: eventsById.values.firstOrNull { it.title.equals(notice.title,ignoreCase=true) }
                                Row(Modifier.fillMaxWidth()
                                    .background(if(notice.read) Color(0xFFF6F6F6) else AgendaChrome,
                                        RoundedCornerShape(13.dp))
                                    .clickable{onRead(notice)}.padding(10.dp),
                                    verticalAlignment=Alignment.CenterVertically) {
                                    if(ev != null) Poster(ev.posterUrl,
                                        Modifier.width(65.dp).height(79.dp).clip(RoundedCornerShape(9.dp)))
                                    else Box(Modifier.size(65.dp).background(Color.White,RoundedCornerShape(9.dp)),
                                        contentAlignment=Alignment.Center) {
                                        AgendaGlyph("bell",Modifier.size(24.dp))
                                    }
                                    Spacer(Modifier.width(11.dp))
                                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                                        Text(notice.title,maxLines=2,overflow=TextOverflow.Ellipsis,
                                            fontSize=14.sp,fontWeight=FontWeight.SemiBold)
                                        Text(if(ev==null) notice.body else listOf(ev.town,ev.venue)
                                            .filter(String::isNotBlank).joinToString(" · "),
                                            maxLines=2,overflow=TextOverflow.Ellipsis,
                                            fontSize=12.sp,color=Color.DarkGray)
                                        Text(if(ev != null) listOf(ev.date,ev.time,
                                            IosUnfilteredFeedPolicy.ticketLabel(ev.ticketAvailable,ev.price))
                                            .filter(String::isNotBlank).joinToString(" · ") else notice.createdAt,
                                            fontSize=11.sp,color=AgendaBlack)
                                    }
                                    if(!notice.read) Box(Modifier.align(Alignment.Top).size(7.dp)
                                        .background(AgendaBlack,CircleShape))
                                }
                            }
                            if(shown.isEmpty() && (view != IosNotificationGrouping.UPCOMING || reminders.isEmpty()))
                                item { EmptyState("No hi ha avisos en esta categoria.") }
                        }
                    }
                    if(view != "Configuració") {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement=Arrangement.End) {
                            OutlinedButton(onClick=onClearRead,enabled=pending>0) {
                                Text("Netejar",fontSize=12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatDialog(group: IosCommunityGroup, api: IosAgendaApi, isCreator: Boolean,
                       onDismiss: () -> Unit, work: (suspend () -> Unit) -> Unit) {
    var messages by remember(group.id) { mutableStateOf(emptyList<IosMessage>()) }
    var text by remember { mutableStateOf("") }
    var membership by remember { mutableStateOf(group.membership) }
    var error by remember { mutableStateOf("") }
    var reporting by remember { mutableStateOf<IosMessage?>(null) }
    var reviewingRequests by remember { mutableStateOf(false) }
    var requests by remember(group.id) { mutableStateOf(emptyList<IosMemberRequest>()) }
    LaunchedEffect(group.id) { runCatching { api.messages(group.id) }.onSuccess { messages = it }.onFailure { error = it.message.orEmpty() } }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f)) {
            Column(Modifier.padding(15.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(group.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("✕") }
                }
                Text(group.type, color = Color.Gray, fontSize = 12.sp)
                if (isCreator) TextButton(onClick = {
                    work { requests = api.pendingRequests(group.id); reviewingRequests = true }
                }) { Text("Sol·licituds d'accés") }
                if (membership != "ACCEPTED") Button(onClick = { work {
                    runCatching { api.joinGroup(group.id, group.eventId, group.access) }.onSuccess {
                        membership = it
                        if (membership == "ACCEPTED") messages = api.messages(group.id)
                    }.onFailure { error = it.message.orEmpty() }
                } }) { Text(if (membership == "PENDING") "Petició pendent" else "Sol·licitar accés") }
                if (error.isNotBlank()) Text(error, color = Color.Gray, fontSize = 12.sp)
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(messages, key = { it.id }) { msg ->
                        Column(Modifier.fillMaxWidth().background(Pale, RoundedCornerShape(12.dp)).padding(9.dp)) {
                            Text(msg.author, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(msg.body, fontSize = 13.sp)
                            TextButton(onClick = { reporting = msg }) { Text("Denunciar missatge", fontSize = 11.sp) }
                        }
                    }
                }
                if (membership == "ACCEPTED") Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Missatge") },
                        modifier = Modifier.weight(1f), maxLines = 3)
                    TextButton(enabled = text.isNotBlank(), onClick = { val toSend = text; work {
                        api.sendMessage(group.id, toSend); text = ""; messages = api.messages(group.id)
                    } }) { Text("Enviar") }
                }
            }
        }
    }
    if (reviewingRequests) AlertDialog(onDismissRequest = { reviewingRequests = false },
        title = { Text("Sol·licituds pendents") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            if (requests.isEmpty()) Text("No hi ha sol·licituds pendents.")
            requests.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.displayName, modifier = Modifier.weight(1f), fontSize = 13.sp)
                    TextButton(onClick = { work {
                        api.respondToMember(group.id, item.userId, true)
                        requests = requests.filterNot { it.userId == item.userId }
                    } }) { Text("Acceptar") }
                    TextButton(onClick = { work {
                        api.respondToMember(group.id, item.userId, false)
                        requests = requests.filterNot { it.userId == item.userId }
                    } }) { Text("Rebutjar") }
                }
            }
        } }, confirmButton = { TextButton(onClick = { reviewingRequests = false }) { Text("Tancar") } })
    reporting?.let { msg ->
        var reason by remember(msg.id) { mutableStateOf("") }
        AlertDialog(onDismissRequest = { reporting = null },
            title = { Text("Denunciar missatge") },
            text = { OutlinedTextField(reason, { reason = it.take(500) }, label = { Text("Motiu") }) },
            confirmButton = { TextButton(enabled = reason.isNotBlank(), onClick = { work {
                api.reportCommunityMessage(group.id, msg.id.toIntOrNull() ?: 0, reason)
                reporting = null
                error = "Denúncia enviada a moderació."
            } }) { Text("Enviar denúncia") } },
            dismissButton = { TextButton(onClick = { reporting = null }) { Text("Cancel·lar") } })
    }
}

@Composable
private fun TicketOptionsDialog(event: AgendaEventSummary, api: IosAgendaApi,
                                pendingStart: IosTicketPaymentStart?,
                                onPaymentCreated: (IosTicketPaymentStart) -> Unit,
                                onPaymentCleared: (IosTicketPaymentStart) -> Unit,
                                onDismiss: () -> Unit,
                                onPurchased: (Boolean) -> Unit, onError: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var catalog by remember(event.id) { mutableStateOf(IosTicketCatalog(emptyList(), null)) }
    var loaded by remember(event.id) { mutableStateOf(false) }
    var reload by remember(event.id) { mutableIntStateOf(0) }
    var busy by remember(event.id) { mutableStateOf(false) }
    var quantity by remember(event.id) { mutableIntStateOf(1) }
    var selectedId by remember(event.id) { mutableIntStateOf(0) }
    var selectedDay by remember(event.id) { mutableStateOf("") }
    var selectedZone by remember(event.id) { mutableStateOf("") }
    var serverError by remember(event.id) { mutableStateOf("") }
    val paymentStart = pendingStart
    var nativePaymentOpen by remember(event.id) { mutableStateOf(false) }
    var paymentMessage by remember(event.id) { mutableStateOf("") }
    var checkingPayment by remember(event.id) { mutableStateOf(false) }
    suspend fun checkPaidOrder(start: IosTicketPaymentStart) {
        // Server is authoritative. Stripe's COMPLETED only starts verification.
        checkingPayment = true
        try {
            var lastStatus = "payment_pending"
            var lastError = ""
            repeat(10) { attempt ->
                val answer = try { api.ticketPaymentStatus(start) }
                    catch (ex: Exception) {
                        lastError = ex.message ?: "No es pot consultar l'estat de la comanda"
                        null
                    }
                if (answer != null) {
                    lastStatus = answer.status
                    if (IosPaidTicketPolicy.isPaid(answer.status)) {
                        // Receipt endpoint is best-effort and cannot override paid state.
                        runCatching { api.requestTicketReceipt(start) }
                        onPaymentCleared(start)
                        onPurchased(true)
                        return
                    }
                    if (IosPaidTicketPolicy.isTerminalFailure(answer.status)) {
                        paymentMessage = when (answer.status.lowercase()) {
                            "payment_failed" -> "Pagament rebutjat pel servidor."
                            "refunded" -> "La comanda consta com a reembossada."
                            else -> "Pagament cancel·lat al servidor."
                        }
                        onPaymentCleared(start)
                        return
                    }
                }
                if (attempt < 9) kotlinx.coroutines.delay(700L)
            }
            paymentMessage = if (lastError.isNotBlank())
                "$lastError. La comanda ${start.orderId} no està confirmada; comprova-la abans de tornar a pagar."
            else "Comanda ${start.orderId}: $lastStatus. Encara sense confirmació; revisa l'estat més tard. No repetisques el pagament."
        } finally { checkingPayment = false }
    }
    fun openStripe(start: IosTicketPaymentStart) {
        val native = IosNativeBridge.current
        if (native == null) {
            paymentMessage = "No està disponible el pagament natiu d'iPhone."
            return
        }
        try {
            nativePaymentOpen = true
            native.presentTicketPayment(start.clientSecret,start.publishableKey,
                start.merchantName, object : IosTicketPaymentCallback {
                override fun onComplete(result: String, error: String) {
                    scope.launch {
                        nativePaymentOpen = false
                        when (result.uppercase()) {
                            "COMPLETED" -> {
                                paymentMessage = "Pagament tramitat; esperant confirmació del servidor."
                                checkPaidOrder(start)
                            }
                            "CANCELLED" -> {
                                paymentMessage = "Has tancat Stripe. La comanda continua sense confirmar; pots reprendre este mateix pagament."
                            }
                            else -> {
                                paymentMessage = error.ifBlank { "Stripe no ha pogut completar el pagament." } +
                                    " Consulta la comanda abans de repetir-la."
                            }
                        }
                    }
                }
            })
        } catch (ex: Exception) {
            nativePaymentOpen = false
            paymentMessage = (ex.message ?: "No s'ha pogut obrir Stripe") +
                ". La comanda continua pendent al servidor."
        }
    }
    LaunchedEffect(event.id, reload) {
        loaded = false
        serverError = ""
        try {
            catalog = api.ticketCatalog(event.id)
            val visible = IosTicketCatalogPolicy.visible(catalog.options, currentAgendaDateIso())
            selectedId = visible.firstOrNull(IosTicketCatalogPolicy::selectable)?.id ?: 0
            quantity = 1
        } catch (ex: Exception) {
            catalog = IosTicketCatalog(emptyList(), null)
            serverError = ex.message ?: "No s'han pogut consultar les entrades"
        } finally { loaded = true }
    }
    val visibleOptions = IosTicketCatalogPolicy.visible(catalog.options, currentAgendaDateIso())
    val filtered = IosTicketCatalogPolicy.filter(visibleOptions, selectedDay, selectedZone)
    val selectedOption = visibleOptions.firstOrNull { it.id == selectedId }
    val limit = IosTicketCatalogPolicy.quantityLimit(selectedOption)
    val validSelection = !busy && !nativePaymentOpen && !checkingPayment && paymentStart == null &&
        loaded && serverError.isBlank() && IosTicketCatalogPolicy.selectable(selectedOption) &&
        quantity in 1..limit && selectedOption in filtered
    AlertDialog(onDismissRequest = {
        if (!busy && !nativePaymentOpen && !checkingPayment) onDismiss()
    },
        title = { Text("Entrades · ${event.title}") },
        text = { Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(9.dp)) {
            if (!loaded) CircularProgressIndicator(Modifier.size(22.dp))
            if (serverError.isNotBlank()) {
                Text(serverError, color = Color(0xFFAD3030), fontSize = 12.sp)
                TextButton(onClick = { reload++ }) { Text("Tornar a consultar") }
            }
            if (loaded && visibleOptions.isEmpty() && serverError.isBlank())
                Text("No hi ha cap modalitat d'entrada disponible. No es pot reservar sense confirmació del servidor.")
            catalog.capacity?.let { Text("Aforament total: $it", fontSize = 12.sp, color = Color.Gray) }
            val days = IosTicketCatalogPolicy.days(visibleOptions)
            if (days.isNotEmpty()) {
                Text("Dates", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    FilterChip(selectedDay.isEmpty(), onClick = { selectedDay = "" }, label = { Text("Totes") })
                    days.forEach { day -> FilterChip(selectedDay == day, onClick = { selectedDay = day },
                        label = { Text(IosUnfilteredFeedPolicy.fullDate(day)) }) }
                }
            }
            val zones = IosTicketCatalogPolicy.sectors(visibleOptions)
            if (zones.isNotEmpty()) {
                Text("Zones", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    FilterChip(selectedZone.isEmpty(), onClick = { selectedZone = "" }, label = { Text("Totes") })
                    zones.forEach { zone -> FilterChip(selectedZone == zone, onClick = { selectedZone = zone },
                        label = { Text(zone) }) }
                }
            }
            if (filtered.isEmpty() && visibleOptions.isNotEmpty())
                Text("No hi ha entrades per a esta combinació de data i zona.", fontSize = 12.sp)
            filtered.forEach { option ->
                val selectable = IosTicketCatalogPolicy.selectable(option)
                Row(Modifier.fillMaxWidth().clickable(enabled = selectable && !busy && paymentStart == null) {
                    selectedId = option.id; quantity = 1
                }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedId == option.id,
                        enabled = selectable && !busy && paymentStart == null,
                        onClick = { selectedId = option.id; quantity = 1 })
                    Column(Modifier.weight(1f)) {
                        Text(option.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(if (option.price == 0.0) "Gratuïta" else "${option.price} €", fontSize = 12.sp)
                        val details = IosTicketCatalogPolicy.details(option)
                        if (details.isNotBlank()) Text(details, fontSize = 11.sp)
                        option.available?.let { Text(if (it == 0) "Esgotada" else "$it disponibles", fontSize = 11.sp) }
                        Text("Màxim ${IosTicketCatalogPolicy.quantityLimit(option)} en esta compra", fontSize = 11.sp, color = Color.Gray)
                        if (option.saleStartsAt.isNotBlank() || option.saleEndsAt.isNotBlank())
                            Text("Venda: ${option.saleStartsAt.ifBlank { "sense inici" }} — ${option.saleEndsAt.ifBlank { "sense fi" }}",
                                fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
            if (selectedOption != null && selectedOption in filtered) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Quantitat: $quantity", modifier = Modifier.weight(1f))
                    TextButton(enabled = !busy && paymentStart == null && quantity > 1,
                        onClick = { quantity-- }) { Text("−") }
                    TextButton(enabled = !busy && paymentStart == null && quantity < limit,
                        onClick = { quantity++ }) { Text("+") }
                }
                if (selectedOption.price > 0.0) {
                    Text("Import base: ${selectedOption.price * quantity} € · $quantity entrada/es. " +
                         "La passarel·la mostrarà l'import definitiu del servidor.",
                         fontSize = 12.sp, color = Color.Gray)
                } else Text("Reserva gratuïta · $quantity entrada/es. El servidor confirmarà l'estoc final.",
                    fontSize = 12.sp, color = Color.Gray)
            }
            if (paymentMessage.isNotBlank()) Text(paymentMessage, color = Color(0xFFAD3030), fontSize = 12.sp)
            if (checkingPayment) Text("Verificant el pagament amb el servidor…", fontSize = 12.sp)
            paymentStart?.let { pending ->
                Text("Comanda ${pending.orderId} en comprovació. No tornes a pagar sense revisar-ne l'estat.", fontSize = 12.sp)
                OutlinedButton(enabled = !busy && !nativePaymentOpen && !checkingPayment,
                    onClick = { scope.launch { checkPaidOrder(pending) } }) { Text("Consultar estat de la comanda") }
                OutlinedButton(enabled = !busy && !nativePaymentOpen && !checkingPayment,
                    onClick = {
                        busy = true
                        scope.launch {
                            try {
                                // Before reopening Stripe check if the same order was already paid.
                                val status = api.ticketPaymentStatus(pending)
                                if (IosPaidTicketPolicy.isPaid(status.status)) {
                                    runCatching { api.requestTicketReceipt(pending) }
                                    onPaymentCleared(pending)
                                    onPurchased(true)
                                } else if (IosPaidTicketPolicy.isTerminalFailure(status.status)) {
                                    onPaymentCleared(pending)
                                    paymentMessage = "La comanda ja no permet pagament. Pots seleccionar-ne una altra."
                                } else openStripe(pending)
                            } catch (ex: Exception) {
                                paymentMessage = (ex.message ?: "No s'ha pogut consultar la comanda") +
                                    ". No es pot reprendre fins que es consulte el servidor."
                            } finally { busy = false }
                        }
                    }) { Text("Continuar amb la mateixa comanda") }
            }
        } },
        confirmButton = {
            TextButton(enabled = validSelection, onClick = {
                val option = selectedOption ?: return@TextButton
                busy = true
                paymentMessage = ""
                if (option.price == 0.0) {
                    scope.launch {
                        try { api.reserveFreeTicket(event.id, option.id, quantity); onPurchased(false) }
                        catch (ex: Exception) { paymentMessage = ex.message ?: "No s'ha pogut emetre l'entrada" }
                        finally { busy = false }
                    }
                } else if (IosNativeBridge.current == null) {
                    paymentMessage = "No està disponible la passarel·la nativa d'iPhone."
                    busy = false
                } else {
                    scope.launch {
                        try {
                            val start = api.createTicketPayment(event.id,option.id,quantity)
                            onPaymentCreated(start)
                            openStripe(start)
                        } catch (ex: Exception) {
                            paymentMessage = (ex.message ?: "No s'ha pogut iniciar el pagament") +
                                ". Consulta les teues entrades si s'ha creat una comanda."
                        } finally { busy = false }
                    }
                }
            }) {
                Text(when {
                    busy -> "Processant…"
                    selectedOption?.price == 0.0 -> "Reservar gratuïtament"
                    else -> "Pagar amb Stripe"
                })
            }
        },
        dismissButton = { TextButton(enabled = !busy && !nativePaymentOpen && !checkingPayment,
            onClick = onDismiss) { Text("Tancar") } })
}

@Composable
private fun CreateGroupDialog(event: AgendaEventSummary, api: IosAgendaApi, onDismiss: () -> Unit,
                              onCreated: () -> Unit, onError: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("OTHER") }
    var open by remember { mutableStateOf(true) }
    var chatEnabled by remember { mutableStateOf(true) }
    var description by remember { mutableStateOf("") }
    var origin by remember { mutableStateOf("") }
    var meetingAt by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Crear tauló") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(event.title, fontSize = 13.sp)
            OutlinedTextField(title, { title = it.take(160) }, label = { Text("Nom del tauló") })
            Text("Tipus de tauló", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            listOf("CAR" to "Cotxe compartit", "TRANSPORT" to "Transport organitzat",
                "MEETUP" to "Quedades", "HOSTING" to "Allotjament compartit", "OTHER" to "Altres").forEach { (key, label) ->
                Row(Modifier.fillMaxWidth().clickable { kind = key }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = kind == key, onClick = { kind = key })
                    Text(label, fontSize = 12.sp)
                }
            }
            OutlinedTextField(description, { description = it.take(1000) }, label = { Text("Descripció") })
            OutlinedTextField(origin, { origin = it.take(190) }, label = { Text("Origen o lloc") })
            OutlinedTextField(meetingAt, { meetingAt = it.take(19) }, label = { Text("Data i hora AAAA-MM-DD HH:MM") })
            OutlinedTextField(capacity, { capacity = it.filter(Char::isDigit).take(3) }, label = { Text("Places disponibles") })
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(open, onCheckedChange = { open = it }); Text("Obert (sense aprovació)", fontSize = 12.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(chatEnabled, onCheckedChange = { chatEnabled = it }); Text("Xat públic activat", fontSize = 12.sp)
            }
        } },
        confirmButton = { TextButton(enabled = !busy && !submitted && IosCommunityBoardContract.validCreate(event.id, title.trim(), kind,
                capacity.toIntOrNull() ?: 0, meetingAt.trim()), onClick = { busy = true; submitted = true; scope.launch {
            try { api.createCommunityGroup(event.id, title, kind, open, description, origin, meetingAt, capacity.toIntOrNull() ?: 0, chatEnabled); onCreated() }
            catch (ex: Exception) { onError((ex.message ?: "Error creant el tauló") + ". Consulta Comunitat abans de tornar a crear-lo.") }
            finally { busy = false }
        } }) { Text("Crear") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel·lar") } })
}

@Composable
private fun ProfessionalValidationDialog(api: IosAgendaApi, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var eventId by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var working by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Validar una entrada") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("Introduïx el codi que figura en l'entrada. El servidor verificarà els permisos i evitarà validar-la dos vegades.", fontSize = 12.sp)
            OutlinedTextField(eventId, { eventId = it.filter(Char::isDigit).take(10) }, label = { Text("ID de l'esdeveniment") })
            OutlinedTextField(code, { code = it.trim().take(160) }, label = { Text("Codi d'entrada") })
            OutlinedButton(enabled = eventId.toIntOrNull()?.let { it > 0 } == true,
                onClick = { eventId.toIntOrNull()?.let { IosNativeBridge.current?.showTicketScanner(it) } }) {
                Text("Escanejar QR amb la càmera")
            }
            Text("El lector d'iPhone necessita permís de càmera i connexió per verificar el codi.", fontSize = 11.sp, color = Color.Gray)
            if (result.isNotBlank()) Text(result, fontWeight = FontWeight.Bold)
        } },
        confirmButton = { TextButton(enabled = eventId.toIntOrNull()?.let { it > 0 } == true && code.isNotBlank() && !working,
            onClick = { working = true; scope.launch {
                try { result = api.validateTicket(eventId.toInt(), code) }
                catch (e: Exception) { result = e.message ?: "Error de validació" }
                finally { working = false }
            } }) { Text(if (working) "Validant…" else "Validar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Tancar") } })
}

@Composable
private fun AdminNotificationPanel(api: IosAgendaApi, work: (suspend () -> Unit) -> Unit, onMessage: (String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<Int?>(null) }
    Text("Notificacions administratives", fontSize = 16.sp, fontWeight = FontWeight.Bold)
    Text("Campanyes de bústia interna, no push APNs. Confirmació obligatòria.", fontSize = 11.sp, color = Color.Gray)
    OutlinedTextField(value = title, onValueChange = { title = it; preview = null }, label = { Text("Títol") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value = body, onValueChange = { body = it; preview = null }, label = { Text("Missatge") }, modifier = Modifier.fillMaxWidth())
    OutlinedButton(onClick = { work { preview = api.adminPreview() } }, enabled = title.isNotBlank() && body.isNotBlank()) { Text("Previsualitzar destinataris") }
    preview?.let { count ->
        Text("Destinataris: $count", fontSize = 13.sp)
        var confirm by remember(count) { mutableStateOf(false) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = confirm, onCheckedChange = { confirm = it })
            Text("Confirme l'enviament", fontSize = 12.sp)
        }
        Button(enabled = confirm && count > 0, onClick = { work {
            api.adminSend(title, body, count); onMessage("Campanya interna enviada"); preview = null
        } }) { Text("Enviar campanya") }
    }
}


/** End-to-end saved lists using the server collections/index.php contract.
 * No local-only lists: the owner's changes are visible on Android and web too. */
@Composable
private fun SavedListsPanel(lists: List<IosSavedList>, available: List<AgendaEventSummary>,
                            api: IosAgendaApi, work: (suspend () -> Unit) -> Unit,
                            onOpenEvent: (AgendaEventSummary) -> Unit,
                            onChanged: (List<IosSavedList>) -> Unit, onStatus: (String) -> Unit) {
    var newName by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }
    var chosenId by remember { mutableStateOf<Int?>(null) }
    var renameName by remember { mutableStateOf("") }
    var deletingId by remember { mutableStateOf<Int?>(null) }
    var sharing by remember { mutableStateOf<IosSavedListShare?>(null) }
    val chosen = lists.firstOrNull { it.id == chosenId }
    fun update(message: String) {
        work { onChanged(api.savedLists()); onStatus(message) }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = newName, onValueChange = { newName = it.take(100) },
                modifier = Modifier.weight(1f), label = { Text("Nova llista") }, singleLine = true)
            Spacer(Modifier.width(5.dp))
            Button(enabled = newName.isNotBlank(), onClick = {
                val name = newName.trim()
                work { api.createSavedList(name); newName = ""; onChanged(api.savedLists()); onStatus("Llista creada") }
            }) { Text("Crear") }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = inviteCode, onValueChange = { inviteCode = it.take(200) },
                modifier = Modifier.weight(1f), singleLine = true, label = { Text("Codi d'invitació") })
            Spacer(Modifier.width(5.dp))
            Button(enabled = inviteCode.isNotBlank(), onClick = {
                val code = inviteCode.trim()
                work { api.joinSavedList(code); inviteCode = ""; onChanged(api.savedLists()); onStatus("Llista incorporada") }
            }) { Text("Unir-me") }
        }
        TextButton(onClick = { update("Llistes actualitzades") }) { Text("↻ Actualitzar llistes") }
        HorizontalDivider()
        if (lists.isEmpty()) EmptyState("Encara no tens llistes. Crea'n una o introduïx un codi.")
        else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items(lists, key = { it.id }) { item ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(item.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                            Text(if (item.owner) "Meua" else "Compartida", fontSize = 11.sp, color = Color.Gray)
                        }
                        Text("${item.eventIds.size} activitats", fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            OutlinedButton(onClick = { chosenId = if (chosenId == item.id) null else item.id }) {
                                Text(if (chosenId == item.id) "Tancar" else "Veure i editar")
                            }
                            if (item.owner) TextButton(onClick = {
                                work { sharing = api.shareSavedList(item.id) }
                            }) { Text("Compartir") }
                            else TextButton(onClick = { deletingId = item.id }) { Text("Eixir") }
                        }
                        if (chosen?.id == item.id) {
                            if (item.owner) {
                                OutlinedTextField(value = renameName, onValueChange = { renameName = it.take(100) },
                                    label = { Text("Canviar nom") }, singleLine = true)
                                Row {
                                    Button(enabled = renameName.isNotBlank(), onClick = {
                                        val name = renameName.trim()
                                        work { api.renameSavedList(item.id, name); renameName = "";
                                            onChanged(api.savedLists()); onStatus("Nom modificat") }
                                    }) { Text("Guardar nom") }
                                    TextButton(onClick = { deletingId = item.id }) { Text("Eliminar llista") }
                                }
                            }
                            val itemsById = available.groupBy { it.id }
                            if (item.eventIds.isEmpty()) Text("Esta llista encara no té esdeveniments.", fontSize = 12.sp)
                            else item.eventIds.sorted().forEach { id ->
                                val event = itemsById[id]?.firstOrNull()
                                if (event != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        TextButton(onClick = { onOpenEvent(event) }, modifier = Modifier.weight(1f)) {
                                            Text(event.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        }
                                        if (item.owner) TextButton(onClick = {
                                            work { api.changeSavedListEvent(item.id, id, false);
                                                onChanged(api.savedLists()); onStatus("Activitat eliminada de la llista") }
                                        }) { Text("Llevar") }
                                    }
                                } else Text("Activitat #$id no disponible al catàleg actual", fontSize = 11.sp, color = Color.Gray)
                            }
                            if (item.owner) {
                                Text("Afegir un esdeveniment guardat des del seu detall:", fontSize = 12.sp)
                                // Use existing visible event ids, no phantom records.
                                var eventSearch by remember(item.id) { mutableStateOf("") }
                                OutlinedTextField(value = eventSearch, onValueChange = { eventSearch = it.take(80) },
                                    singleLine = true, label = { Text("Buscar activitat per afegir") })
                                if (eventSearch.trim().length >= 2) {
                                    available.asSequence().filter { it.id !in item.eventIds && it.matches(eventSearch) }
                                        .distinctBy { it.id }.take(5).forEach { event ->
                                            TextButton(onClick = { work { api.changeSavedListEvent(item.id, event.id, true);
                                                onChanged(api.savedLists()); eventSearch = ""; onStatus("Activitat afegida") } }) {
                                                Text("+ ${event.title}", maxLines = 2)
                                            }
                                        }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    deletingId?.let { id ->
        val item = lists.firstOrNull { it.id == id }
        AlertDialog(onDismissRequest = { deletingId = null },
            title = { Text(if (item?.owner == true) "Eliminar la llista?" else "Deixar la llista compartida?") },
            text = { Text(item?.name.orEmpty()) },
            dismissButton = { TextButton(onClick = { deletingId = null }) { Text("Cancel·lar") } },
            confirmButton = { TextButton(onClick = {
                deletingId = null
                work { if (item?.owner == true) api.deleteSavedList(id) else api.leaveSavedList(id)
                    onChanged(api.savedLists()); if (chosenId == id) chosenId = null
                    onStatus("Llista actualitzada") }
            }) { Text("Confirmar") } }
        )
    }
    sharing?.let { shared ->
        AlertDialog(onDismissRequest = { sharing = null },
            title = { Text("Compartir llista") },
            text = { Column {
                Text("Envia este codi a qui vulgues convidar:")
                androidx.compose.foundation.text.selection.SelectionContainer { Text(shared.shareCode, fontWeight = FontWeight.Bold) }
                Text("O compartix este enllaç:")
                androidx.compose.foundation.text.selection.SelectionContainer { Text(shared.inviteUrl, fontSize = 12.sp) }
            } },
            confirmButton = { TextButton(onClick = { sharing = null }) { Text("Tancar") } }
        )
    }
}

@Composable
private fun ProfileSettingsEditor(current: IosProfileSettings, onDismiss: () -> Unit,
                                  onSave: (String, String) -> Unit) {
    var role by remember(current) { mutableStateOf(if (current.role == "PUBLIC") "PUBLIC" else "PROFESSIONAL") }
    var phone by remember(current) { mutableStateOf(current.phone) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Tipus de compte", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(if (current.emailVerified) "Correu verificat" else "Correu pendent de verificació", fontSize = 12.sp)
                listOf("PUBLIC" to "Públic", "PROFESSIONAL" to "Professional").forEach { (value, label) ->
                    Row(Modifier.fillMaxWidth().clickable { role = value }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = role == value, onClick = { role = value }); Text(label)
                    }
                }
                if (role == "PROFESSIONAL") OutlinedTextField(value = phone, onValueChange = { phone = it.take(24) },
                    label = { Text("Telèfon professional") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    TextButton(onClick = onDismiss) { Text("Cancel·lar") }
                    Button(onClick = { onSave(role, phone) },
                        enabled = role == "PUBLIC" || phone.matches(Regex("[+0-9() .-]{7,24}"))) { Text("Guardar") }
                }
            }
        }
    }
}


@Composable
private fun AccountIdentityIosDialog(current: IosAccount, homeTown: String,
    onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var username by remember(current.id) { mutableStateOf(current.username) }
    var email by remember(current.id) { mutableStateOf(current.email) }
    var town by remember(current.id, homeTown) { mutableStateOf(homeTown) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Dades del compte") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(username, { username = it.take(190) }, label = { Text("Usuari") }, singleLine = true)
            OutlinedTextField(email, { email = it.take(254) }, label = { Text("Correu electrònic") }, singleLine = true)
            OutlinedTextField(town, { town = it.take(190) }, label = { Text("Població") }, singleLine = true)
            Text("El canvi de correu pot requerir una nova verificació.", fontSize = 12.sp)
        } }, confirmButton = {
            TextButton(enabled = runCatching { IosProfileParity.validateIdentity(username,email,town); true }.getOrDefault(false),
                onClick = { onSave(username, email, town) }) { Text("Guardar") }
        }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel·lar") } })
}

@Composable
private fun ProfileEditIosDialog(current: IosMyProfile, onDismiss: () -> Unit,
                                 onPickImage: () -> Unit, onSave: (IosMyProfile, Boolean) -> Unit) {
    var name by remember(current.displayName) { mutableStateOf(current.displayName) }
    var bio by remember(current.bio) { mutableStateOf(current.bio) }
    var discipline by remember(current.discipline) { mutableStateOf(current.discipline) }
    var company by remember(current.companyName) { mutableStateOf(current.companyName) }
    var cast by remember(current.castText) { mutableStateOf(current.castText) }
    var publisher by remember(current.publisherName) { mutableStateOf(current.publisherName) }
    var books by remember(current.booksText) { mutableStateOf(current.booksText) }
    var works by remember(current.worksText) { mutableStateOf(current.worksText) }
    var spaces by remember(current.spacesText) { mutableStateOf(current.spacesText) }
    var remove by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Editar perfil") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            if (current.photoUrl.isNotBlank() && !remove) {
                AsyncImage(model = current.photoUrl, contentDescription = "Foto actual", modifier = Modifier.size(90.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            }
            OutlinedButton(onClick = { remove = false; onPickImage() }) { Text("Triar fotografia de Fotos") }
            if (current.photoUrl.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = remove, onCheckedChange = { remove = it })
                    Text("Llevar la fotografia actual")
                }
            }
            OutlinedTextField(name, { name = it.take(190) }, label = { Text("Nom i cognoms") }, singleLine = true)
            OutlinedTextField(bio, { bio = it.take(3000) }, label = { Text("Biografia") }, minLines = 2, maxLines = 4)
            OutlinedTextField(discipline, { discipline = it.take(190) }, label = { Text("Disciplina") })
            OutlinedTextField(company, { company = it.take(190) }, label = { Text("Companyia / empresa") })
            OutlinedTextField(cast, { cast = it.take(3000) }, label = { Text("Repartiment") })
            OutlinedTextField(publisher, { publisher = it.take(190) }, label = { Text("Editorial") })
            OutlinedTextField(books, { books = it.take(3000) }, label = { Text("Llibres") })
            OutlinedTextField(works, { works = it.take(3000) }, label = { Text("Obres") })
            OutlinedTextField(spaces, { spaces = it.take(3000) }, label = { Text("Espais") })
            Text("El perfil públic i la fotografia es guarden al servidor.", fontSize = 11.sp, color = Color.Gray)
        } },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = {
            onSave(current.copy(displayName = name, bio = bio, discipline = discipline,
                companyName = company, castText = cast, publisherName = publisher,
                booksText = books, worksText = works, spacesText = spaces), remove)
        }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel·lar") } })
}

@Composable
private fun DeleteAccountIosDialog(onDismiss: () -> Unit, onDelete: (String, String) -> Unit) {
    var phrase by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Eliminar el meu compte") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("Esta operació desactiva definitivament l'accés, revoca totes les sessions i retira dades personals. Les operacions econòmiques que s'hagen de conservar legalment no es poden eliminar immediatament.", fontSize = 13.sp)
            OutlinedTextField(phrase, { phrase = it.take(20) }, label = { Text("Escriu ELIMINAR") }, singleLine = true)
            OutlinedTextField(password, { password = it }, label = { Text("Contrasenya (si tens accés amb contrasenya)") },
                singleLine = true, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
        } },
        confirmButton = { TextButton(enabled = phrase == "ELIMINAR", onClick = { onDelete(phrase, password) }) { Text("Eliminar definitivament") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel·lar") } })
}
