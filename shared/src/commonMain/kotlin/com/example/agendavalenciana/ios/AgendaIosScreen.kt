package com.example.agendavalenciana.ios

import com.example.agendavalenciana.model.BatecCandidate
import com.example.agendavalenciana.model.FollowedBatecsPolicy
import com.example.agendavalenciana.model.IosBatecArtistGroup
import com.example.agendavalenciana.model.IosBatecArtistGroups
import com.example.agendavalenciana.model.IosBatecTimelinePolicy
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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val Ink = Color(0xFF252329)
private val Accent = Color(0xFFE8DFF2)
private val Pale = Color(0xFFF7F6F9)

/** Do not mix taste selections or reminder times between accounts on the same iPhone. */
private fun iosScopedCacheKey(name: String, userId: Int?): String = IosAccountCachePolicy.key(name, userId)
private fun iosScopedSet(name: String, userId: Int?): Set<String> = IosAccountCachePolicy.decode(
    platformReadPublicCache(iosScopedCacheKey(name, userId)),
    if (userId == null) platformReadPublicCache(name) else "", userId == null
)
private fun iosReminderKey(userId: Int, eventId: Int): String = IosAccountCachePolicy.reminderKey(userId, eventId)

/** Native iPhone Compose screen. The original Android UI remains in app/. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaIosScreen(sessionBridge: IosSessionBridge? = null) {
    IosNativeBridge.current = sessionBridge
    val api = remember { IosAgendaApi(sessionBridge) }
    val scope = rememberCoroutineScope()
    var events by remember { mutableStateOf(IosEventCatalog.cached()) }
    var updating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf("Inici") }
    var profileReturnTab by remember { mutableStateOf("Inici") }
    var feedMode by remember { mutableStateOf("Per a tu") }
    var selectedFeedCategory by remember { mutableStateOf<String?>(null) }
    var activeFilterPresetName by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var appliedQuery by remember { mutableStateOf("") }
    // Query is committed only by Search / tapping a suggestion. Typing never recalculates the feed.
    var suggestionDraft by remember { mutableStateOf("") }
    LaunchedEffect(query) { kotlinx.coroutines.delay(240); suggestionDraft = query.trim() }
    var chosenSuggestion by remember { mutableStateOf<IosSearchSuggestion?>(null) }
    var appliedSuggestion by remember { mutableStateOf<IosSearchSuggestion?>(null) }
    var limit by remember { mutableIntStateOf(5) }
    // Restored only after a real background interval, not when closing a popup or changing tabs.
    var feedExpiredEpoch by remember { mutableIntStateOf(0) }
    DisposableEffect(sessionBridge) {
        // Covers cold starts, including an iOS process killed while suspended.
        if (IosFeedReadingSession.foreground()) feedExpiredEpoch++
        sessionBridge?.setFeedLifecycleListener(object : IosFeedLifecycleCallback {
            override fun onForeground() {
                if (IosFeedReadingSession.foreground()) feedExpiredEpoch++
            }
            override fun onBackground() { IosFeedReadingSession.background() }
        })
        onDispose { sessionBridge?.setFeedLifecycleListener(null) }
    }
    LaunchedEffect(feedExpiredEpoch) {
        if (feedExpiredEpoch > 0) limit = 5
    }
    var selected by remember { mutableStateOf<AgendaEventSummary?>(null) }
    var artist by remember { mutableStateOf<String?>(null) }
    var artistSourceEvent by remember { mutableStateOf<AgendaEventSummary?>(null) }
    var pendingOpenEventId by remember { mutableIntStateOf(0) }
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
                if (eventId > 0) pendingOpenEventId = eventId
            }
        })
    }
    var festivalMatchedArtist by remember { mutableStateOf("") }
    var savedFestivalRevision by remember { mutableIntStateOf(0) }
    var batecStoryKey by remember { mutableStateOf<String?>(null) }
    var batecsReturnEpoch by remember { mutableIntStateOf(0) }
    fun closeBatecs() {
        batecStoryKey = null
        batecsReturnEpoch += 1
    }
    var searchFiltersOpen by remember { mutableStateOf(false) }
    var appliedSearchFilters by remember { mutableStateOf(IosSearchFilters()) }
    var searchPresets by remember { mutableStateOf(emptyList<IosSavedSearch>()) }
    var loginOpen by remember { mutableStateOf(false) }
    // Shown only after an anonymous first launch, never after signing out of an existing account.
    val initialGuestOnboardingEligible = remember {
        platformReadPublicCache("ios_first_run_state_v350") == "offered" ||
            (platformReadPublicCache("ios_first_run_state_v350").isBlank() &&
             platformReadPublicCache("events_at").isBlank() &&
             platformReadPublicCache(IosGuestFollowTransfer.GUEST_KEY).isBlank() &&
             platformReadPublicCache(iosScopedCacheKey("pref_categories", null)).isBlank() &&
             platformReadPublicCache(iosScopedCacheKey("pref_towns", null)).isBlank())
    }
    var firstRunOpen by remember { mutableStateOf(false) }
    var personalizationImagesRevision by remember { mutableIntStateOf(0) }
    LaunchedEffect(firstRunOpen) {
        if (firstRunOpen && IosPersonalizationImages.cached().isEmpty()) {
            runCatching { IosPersonalizationImages.ingest(api.entityStyles()) }
                .onSuccess { personalizationImagesRevision++ }
        }
    }
    fun completeFirstRun() {
        platformWritePublicCache("ios_first_run_state_v350", "done")
        firstRunOpen = false
    }
    var registrationOpen by remember { mutableStateOf(false) }
    var resetOpen by remember { mutableStateOf(false) }
    var prefOpen by remember { mutableStateOf(false) }
    LaunchedEffect(prefOpen) {
        if (prefOpen && IosPersonalizationImages.cached().isEmpty()) {
            runCatching { IosPersonalizationImages.ingest(api.entityStyles()) }
                .onSuccess { personalizationImagesRevision++ }
        }
    }
    var notificationsOpen by remember { mutableStateOf(false) }
    var communityGroup by remember { mutableStateOf<IosCommunityGroup?>(null) }
    val guestFollowTransfer = remember {
        IosGuestFollowTransfer(::platformReadPublicCache, ::platformWritePublicCache)
    }
    fun guestUserState(): IosUserState = IosUserState(
        follows = guestFollowTransfer.guest(),
        saved = iosScopedSet("saved_events", null).mapNotNull(String::toIntOrNull).filter { it > 0 }.toSet(),
        alerts = iosScopedSet("alert_events", null).mapNotNull(String::toIntOrNull).filter { it > 0 }.toSet()
    )
    var user by remember { mutableStateOf(guestUserState()) }
    var account by remember { mutableStateOf<IosAccount?>(null) }
    val followSyncMutex = remember { Mutex() }
    val feedPrefsSyncMutex = remember { Mutex() }
    var feedPrefsServerRevision by remember { mutableIntStateOf(0) }
    // Android's account ∪ guest migration: first journal, then post missing follows.
    // Never clear a journal entry before a server response confirms it.
    fun stageGuestFollows(accountId: Int) {
        val guestCategories = (iosScopedSet("pref_categories", null) + iosScopedSet("pref_subcategories", null))
            .map { IosPersonalizationTaxonomy.canonicalFollowKey(it) }
            .filter { key -> key in IosPersonalizationTaxonomy.categories.map { it.code } ||
                key == "CHILDREN" || IosPersonalizationTaxonomy.subcategories.any { it.code == key } }
            .map { key ->
                val label = IosPersonalizationTaxonomy.categoryLabel(key).let { if (it == key)
                    IosPersonalizationTaxonomy.subcategoryLabel(key) else it }
                IosFollow("CATEGORY", key, label)
            }
        val staged = guestFollowTransfer.stage(accountId, guestCategories)
        if (guestCategories.all { IosGuestFollowTransfer.contains(staged, it) } &&
            guestCategories.all { IosGuestFollowTransfer.contains(guestFollowTransfer.pending(accountId), it) }) {
            // Guest category tastes must not be accidentally imported into a SECOND account.
            platformWritePublicCache(iosScopedCacheKey("pref_categories",null), "~")
            platformWritePublicCache(iosScopedCacheKey("pref_subcategories",null), "~")
        }
    }
    // Stage anonymous first-run locations before switching scopes. The account and guest
    // locations remain isolated and failed writes stay pending for the next login.
    fun stageGuestFirstRunPlaces(accountId: Int) {
        if (accountId <= 0) return
        val guestTowns = iosScopedSet("pref_towns", null)
        if (guestTowns.isEmpty()) return
        val guestRadii = IosPersonalFeedPolicy.decodeRadii(iosScopedSet("pref_radii", null))
        val accountTowns = iosScopedSet("pref_towns", accountId)
        val accountRadii = IosPersonalFeedPolicy.decodeRadii(iosScopedSet("pref_radii", accountId))
        val pendingTowns = iosScopedSet("intro_pending_towns", accountId) + guestTowns
        val mergedRadii = accountRadii + guestTowns.associateWith {
            guestRadii[it] ?: PersonalizationOptions.defaultRadiusKm
        }
        platformWritePublicCache(iosScopedCacheKey("intro_pending_towns", accountId),
            IosAccountCachePolicy.encode(pendingTowns))
        platformWritePublicCache(iosScopedCacheKey("pref_towns", accountId),
            IosAccountCachePolicy.encode(accountTowns + guestTowns))
        platformWritePublicCache(iosScopedCacheKey("pref_radii", accountId),
            IosAccountCachePolicy.encode(IosPersonalFeedPolicy.encodeRadii(mergedRadii)))
        platformWritePublicCache(iosScopedCacheKey("pref_places_dirty", accountId), "true")
        // Clear guest only after the account-scoped journal has round-tripped.
        if (guestTowns.all { it in iosScopedSet("intro_pending_towns", accountId) }) {
            platformWritePublicCache(iosScopedCacheKey("pref_towns", null), "~")
            platformWritePublicCache(iosScopedCacheKey("pref_radii", null), "~")
        }
    }
    suspend fun synchronizeGuestFollows(accountId: Int, remoteFollows: List<IosFollow>) {
        var updatedOnServer = false
        followSyncMutex.withLock {
            if (account?.id != accountId) return@withLock
            // A remote read may already contain a previous successful POST.
            remoteFollows.filter { remote ->
                guestFollowTransfer.pending(accountId).any { pending ->
                    IosGuestFollowTransfer.contains(listOf(remote), pending)
                }
            }.forEach { guestFollowTransfer.acknowledge(accountId, it) }
            for (follow in guestFollowTransfer.missing(accountId, remoteFollows)) {
                if (account?.id != accountId) break
                // Continue with later entities even when one post fails.
                val confirmed = runCatching { api.followEntity(follow.type, follow.key, follow.label, true) }.isSuccess
                if (confirmed) {
                    guestFollowTransfer.acknowledge(accountId, follow)
                    updatedOnServer = true
                }
            }
        }
        if (account?.id == accountId && updatedOnServer) {
            // Registered users' category follows are read from MariaDB by for-you/feed.php.
            // Rerun that selection only after follow.php confirms the server-side update.
            feedPrefsServerRevision += 1
            platformWritePublicCache("for_you_${accountId}_signature", "")
        }
        if (account?.id == accountId && guestFollowTransfer.pending(accountId).isNotEmpty())
            status = "Alguns seguiments encara estan pendents de sincronitzar"
    }
    // On a cold launch the Keychain may already belong to a logged-in user.
    // Never overwrite that user's stored reading position with a temporary guest feed
    // while auth/me.php is resolving (or the network is unavailable).
    var awaitingInitialAccount by remember { mutableStateOf(!api.token.isNullOrBlank()) }
    var notices by remember { mutableStateOf(emptyList<IosNotification>()) }
    var notificationPreferences by remember(account?.id) {
        mutableStateOf(IosNotificationPreferences.decode(platformReadPublicCache(iosScopedCacheKey("notification_preferences",account?.id))))
    }
    var readLocalNotifications by remember(account?.id) {
        mutableStateOf(iosScopedSet("notification_read",account?.id))
    }
    var dismissedLocalNotifications by remember(account?.id) {
        mutableStateOf(iosScopedSet("notification_dismissed",account?.id))
    }
    fun markLocalNotificationRead(key: String) {
        readLocalNotifications = readLocalNotifications + key
        platformWritePublicCache(iosScopedCacheKey("notification_read",account?.id),IosAccountCachePolicy.encode(readLocalNotifications))
    }

    var tickets by remember { mutableStateOf(emptyList<IosTicket>()) }
    var groups by remember { mutableStateOf(emptyList<IosCommunityGroup>()) }
    var batecs by remember { mutableStateOf(emptyList<IosBatec>()) }
    var seenBatecs by remember { mutableStateOf(emptySet<String>()) }
    var batecRecentEventCreations by remember { mutableStateOf(emptyMap<Int, Long>()) }
    var batecClockTick by remember { mutableLongStateOf(currentEpochMillis()) }
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
    var chosenCategories by remember { mutableStateOf(iosScopedSet("pref_categories", null)) }
    var chosenTowns by remember { mutableStateOf(iosScopedSet("pref_towns", null)) }
    var chosenSubcategories by remember { mutableStateOf(iosScopedSet("pref_subcategories", null)) }
    var chosenComarques by remember { mutableStateOf(iosScopedSet("pref_comarques", null)) }
    var chosenRadii by remember { mutableStateOf(IosPersonalFeedPolicy.decodeRadii(iosScopedSet("pref_radii", null))) }
    var placesWriteRevision by remember { mutableIntStateOf(0) }
    var feedPrefsWriteRevision by remember { mutableIntStateOf(0) }

    var visibilityWriteRevision by remember { mutableIntStateOf(0) }
    var showSuggestions by remember { mutableStateOf(platformReadPublicCache(iosScopedCacheKey("pref_suggestions",null)) != "false") }
    var valencianFirst by remember { mutableStateOf(platformReadPublicCache(iosScopedCacheKey("pref_valencian",null)) != "false") }
    var discoveryLevel by remember { mutableStateOf(DiscoveryLevel.BALANCED) }
    var learningResetRevision by remember { mutableIntStateOf(0) }
    var personalizationPushEnabled by remember(account?.id) {
        mutableStateOf(platformReadPublicCache(iosScopedCacheKey("pref_ios_push",account?.id)) == "true")
    }

    // Personal feed exclusions are local and namespace-scoped by account.
    var excludedEventIds by remember { mutableStateOf(iosScopedSet("hidden_events", null).mapNotNull(String::toIntOrNull).toSet()) }
    var excludedArtists by remember { mutableStateOf(iosScopedSet("hidden_artists", null)) }
    var excludedCategories by remember { mutableStateOf(iosScopedSet("hidden_categories", null)) }
    var alertToDisable by remember { mutableStateOf<AgendaEventSummary?>(null) }
    var reminderToSchedule by remember { mutableStateOf<AgendaEventSummary?>(null) }
    var ticketEvent by remember { mutableStateOf<AgendaEventSummary?>(null) }
    var groupCreateFor by remember { mutableStateOf<AgendaEventSummary?>(null) }
    // Android's + menu, role-aware and refreshed each time it opens.
    var publisherOpen by remember { mutableStateOf(false) }
    var publisherLoading by remember { mutableStateOf(false) }
    var publisherRole by remember { mutableStateOf<String?>(null) }
    var publisherOwnEvents by remember { mutableStateOf(emptyList<IosOwnPublishedEvent>()) }
    var publisherSuspended by remember { mutableStateOf(false) }
    var publisherWarning by remember { mutableStateOf<String?>(null) }
    var publisherDestination by remember { mutableStateOf<IosPublishAction?>(null) }
    var publisherRequestOpen by remember { mutableStateOf(false) }
    LaunchedEffect(publisherOpen, account?.id) {
        if (!publisherOpen) return@LaunchedEffect
        val requestedId = account?.id
        publisherRole = account?.role
        publisherOwnEvents = emptyList() // Don't show old account ownership while verifying.
        publisherSuspended = false
        publisherWarning = null
        publisherLoading = true
        try {
            coroutineScope {
                val accessRequest = if (requestedId == null) null else async {
                    runCatching { api.publisherAccess() }
                }
                val trustRequest = if (requestedId == null) null else async {
                    runCatching { api.publicationTrust() }
                }
                val ownedRequest = async { runCatching { api.ownPublishedEvents(IosGuestIdentity.get()) } }
                val access = accessRequest?.await()
                val trust = trustRequest?.await()
                val owned = ownedRequest.await()
                if (!publisherOpen || account?.id != requestedId) return@coroutineScope
                if (access != null) {
                    access.onSuccess { value ->
                        publisherRole = value.role
                        account = account?.copy(role = value.role)
                    }.onFailure {
                        publisherWarning = "No s'ha pogut refrescar el rol professional. Es mostra el rol de la sessió."
                    }
                }
                trust?.onSuccess { publisherSuspended = it.suspended }
                owned.onSuccess { publisherOwnEvents = it }
                    .onFailure { publisherWarning = "No s'han pogut carregar les teues publicacions." }
            }
        } finally { if (publisherOpen && account?.id == requestedId) publisherLoading = false }
    }
    var validatorOpen by remember { mutableStateOf(false) }
    var savedLists by remember { mutableStateOf(emptyList<IosSavedList>()) }
    var profileSettings by remember { mutableStateOf<IosProfileSettings?>(null) }
    var profileSaving by remember { mutableStateOf(false) }
    var profileSettingsOpen by remember { mutableStateOf(false) }
    var myProfile by remember { mutableStateOf<IosMyProfile?>(null) }
    var editProfileOpen by remember { mutableStateOf(false) }
    var deleteAccountOpen by remember { mutableStateOf(false) }
    var reportEvent by remember { mutableStateOf<AgendaEventSummary?>(null) }
    var pendingFollows by remember { mutableStateOf(emptySet<String>()) }
    var artistBells by remember { mutableStateOf(emptyMap<String, IosArtistBell>()) }
    var artistBellEditing by remember { mutableStateOf<String?>(null) }


    fun work(action: suspend () -> Unit) {
        scope.launch {
            try { error = ""; action() } catch (e: Exception) { error = e.message ?: "No s'ha pogut completar l'operació." }
        }
    }
    fun refresh(force: Boolean = false) {
        if (updating) return
        val requestedAccountId = account?.id
        updating = true
        work {
            try {
                events = IosEventCatalog.get(force)
                paid = runCatching { api.promoted() }.getOrDefault(paid)
                entityStyles = withContext(Dispatchers.Default) { IosEntityAffinityCatalog.refresh(api, force) }
                if (requestedAccountId != null) {
                    val refreshedUser = runCatching { api.userState() }.getOrNull()
                    if (account?.id == requestedAccountId && refreshedUser != null) {
                        user = refreshedUser.copy(follows = guestFollowTransfer.preview(requestedAccountId, refreshedUser.follows))
                        synchronizeGuestFollows(requestedAccountId, refreshedUser.follows)
                    }
                }
                val newPublications = runCatching { api.batecs(user.follows.filter { it.type.equals("ARTIST", true) }
                    .map { it.label }.toSet()) }.getOrNull()
                if (account?.id == requestedAccountId) newPublications?.let { batecs = it }
                if (requestedAccountId != null && account?.id == requestedAccountId) {
                    runCatching { api.recentBatecEventCreations() }.onSuccess { batecRecentEventCreations = it }
                }
                if (force) status = "Agenda actualitzada"
            } finally { updating = false }
        }
    }
    fun useAccountPreferences(id: Int?) {
        chosenCategories = iosScopedSet("pref_categories", id)
        chosenTowns = iosScopedSet("pref_towns", id)
        chosenSubcategories = iosScopedSet("pref_subcategories", id)
        chosenComarques = iosScopedSet("pref_comarques", id)
        chosenRadii = IosPersonalFeedPolicy.decodeRadii(iosScopedSet("pref_radii",id))
        showSuggestions = platformReadPublicCache(iosScopedCacheKey("pref_suggestions",id)) != "false"
        valencianFirst = platformReadPublicCache(iosScopedCacheKey("pref_valencian",id)) != "false"
        discoveryLevel = runCatching { DiscoveryLevel.valueOf(platformReadPublicCache(iosScopedCacheKey("pref_discovery",id))) }.getOrDefault(DiscoveryLevel.BALANCED)

        excludedEventIds = iosScopedSet("hidden_events", id).mapNotNull(String::toIntOrNull).toSet()
        excludedArtists = iosScopedSet("hidden_artists", id)
        excludedCategories = iosScopedSet("hidden_categories", id)
    }
    // First-run and Personalització share the exact same guest state and follow migration.
    // A subcategory selection narrows the parent discipline; it does not imply the whole category.
    fun saveGuestFirstRun(cats: Set<String>, styles: Set<String>, locations: Map<String, Int>, artists: Set<String>) {
        if (account != null) return
        val followKeys = IosFirstRunPolicy.followKeys(cats, styles)
        chosenCategories = followKeys.intersect(IosFirstRunPolicy.disciplines.toSet())
        chosenSubcategories = followKeys.filter { it.contains("__") }.toSet()
        chosenTowns = locations.keys
        chosenRadii = locations.mapValues { PersonalizationOptions.validRadius(it.value) }
        platformWritePublicCache(iosScopedCacheKey("pref_categories", null), IosAccountCachePolicy.encode(chosenCategories))
        platformWritePublicCache(iosScopedCacheKey("pref_subcategories", null), IosAccountCachePolicy.encode(chosenSubcategories))
        platformWritePublicCache(iosScopedCacheKey("pref_towns", null), IosAccountCachePolicy.encode(chosenTowns))
        platformWritePublicCache(iosScopedCacheKey("pref_radii", null),
            IosAccountCachePolicy.encode(IosPersonalFeedPolicy.encodeRadii(chosenRadii)))
        val rest = user.follows.filterNot { it.type.equals("CATEGORY",true) || it.type.equals("ARTIST",true) }
        val categoryFollows = followKeys.map { key ->
            val label = if (key.contains("__")) IosPersonalizationTaxonomy.subcategoryLabel(key)
                else IosPersonalizationTaxonomy.categoryLabel(key)
            IosFollow("CATEGORY",key,label)
        }
        val artistFollows = artists.map { IosFollow("ARTIST",it,it) }
        val nextFollows = IosGuestFollowTransfer.union(rest, categoryFollows + artistFollows)
        guestFollowTransfer.setGuest(nextFollows)
        user = user.copy(follows = nextFollows)
    }
    // User follows are the shared Android/iOS source of truth for account categories.
    // Do not leave Personalització reading stale device-only selections after sign-in.
    fun applyAccountCategoryFollows(accountId: Int, follows: List<IosFollow>) {
        if (account?.id != accountId) return
        val keys = follows.asSequence()
            .filter { it.type.equals("CATEGORY", ignoreCase = true) }
            .map { IosPersonalizationTaxonomy.canonicalFollowKey(it.key) }
            .toSet()
        val validCats = IosPersonalizationTaxonomy.categories.map { it.code }.toSet() + "CHILDREN"
        val validSubs = IosPersonalizationTaxonomy.subcategories.map { it.code }.toSet()
        val cats = keys.intersect(validCats)
        val subs = keys.intersect(validSubs)
        // Preserve custom, non-taxonomy styles; standard category choices follow the server.
        val extraStyles = chosenSubcategories.filterNot {
            IosPersonalizationTaxonomy.canonicalFollowKey(it) in validSubs
        }.toSet()
        chosenCategories = cats
        chosenSubcategories = subs + extraStyles
        platformWritePublicCache(iosScopedCacheKey("pref_categories",accountId), IosAccountCachePolicy.encode(cats))
        platformWritePublicCache(iosScopedCacheKey("pref_subcategories",accountId), IosAccountCachePolicy.encode(subs + extraStyles))
    }
    suspend fun reconcileFeedPreferences(expectedAccount: Int, expectedRevision: Int, remote: IosServerFeedPreferences?) {
        if (remote == null || account?.id != expectedAccount) return
        val dirtyKey = iosScopedCacheKey("pref_feed_dirty", expectedAccount)
        val dirty = platformReadPublicCache(dirtyKey) == "true"
        if (feedPrefsWriteRevision != expectedRevision) return // a newer UI edit wins
        if (dirty || !remote.exists) {
            // For old accounts seed their account-local choices exactly once; persist offline edits.
            feedPrefsSyncMutex.withLock {
                if (account?.id != expectedAccount || feedPrefsWriteRevision != expectedRevision) return@withLock
                runCatching { api.saveFeedPreferences(showSuggestions, discoveryLevel.name, valencianFirst) }
                    .onSuccess { if (account?.id == expectedAccount && feedPrefsWriteRevision == expectedRevision) {
                        platformWritePublicCache(dirtyKey,"false")
                        feedPrefsServerRevision += 1
                    } }
                    .onFailure { if (account?.id == expectedAccount) {
                        platformWritePublicCache(dirtyKey,"true")
                        error = "Preferències de Per a tu pendents de sincronització: ${it.message.orEmpty()}"
                    } }
            }
        } else {
            showSuggestions = remote.suggestions
            valencianFirst = remote.valencianFirst
            discoveryLevel = runCatching { DiscoveryLevel.valueOf(remote.discovery) }.getOrDefault(DiscoveryLevel.BALANCED)
            platformWritePublicCache(iosScopedCacheKey("pref_suggestions",expectedAccount),showSuggestions.toString())
            platformWritePublicCache(iosScopedCacheKey("pref_discovery",expectedAccount),discoveryLevel.name)
            platformWritePublicCache(iosScopedCacheKey("pref_valencian",expectedAccount),valencianFirst.toString())
        }
    }
    fun loadAccountPages() {
        val expectedAccount = account?.id ?: return
        work {
            // Independent requests can run together. One slow mailbox must not delay tickets,
            // follows or saved lists; failed optional calls retain their previous contents.
            coroutineScope {
                val userRequest = async { api.userState() }
                val noticesRequest = async { runCatching { api.notifications() }.getOrNull() }
                val ticketsRequest = async { runCatching { api.tickets() }.getOrNull() }
                val groupsRequest = async { runCatching { api.community() }.getOrNull() }
                val seenRequest = async { runCatching { api.seenBatecs() }.getOrNull() }
                val creationsRequest = async { runCatching { api.recentBatecEventCreations() }.getOrNull() }
                val listsRequest = async { runCatching { api.savedLists() }.getOrNull() }
                val presetsRequest = async { runCatching { api.savedSearches() }.getOrNull() }
                val bellsRequest = async { runCatching { api.artistBells() }.getOrNull() }
                val feedPrefsRevisionOnRequest = feedPrefsWriteRevision
                val feedPrefsRequest = async { runCatching { api.feedPreferences() }.getOrNull() }
                val placesRevisionOnRequest = placesWriteRevision
                val placesRequest = async { runCatching { api.locationPreferences() }.getOrNull() }
                val visibilityRevisionOnRequest = visibilityWriteRevision
                val visibilityRequest = async { runCatching { api.visibilityExclusions() }.getOrNull() }
                val newUser = userRequest.await()
                // A session may be restored after the public catalog was loaded.
                // Re-fetch the followed Batecs without clearing the existing strip on failure.
                val newBatecs = runCatching { api.batecs(newUser.follows
                    .filter { it.type.equals("ARTIST", true) }.map { it.label }.toSet()) }.getOrNull()
                val newNotices = noticesRequest.await()
                val newTickets = ticketsRequest.await()
                val newGroups = groupsRequest.await()
                val newSeen = seenRequest.await()
                val newCreations = creationsRequest.await()
                val newSavedLists = listsRequest.await()
                val newPresets = presetsRequest.await()
                val newBells = bellsRequest.await()
                val newFeedPreferences = feedPrefsRequest.await()
                val newPlaces = placesRequest.await()
                val remoteVisibility = visibilityRequest.await()
                // Never replace the new account's state with an old account's response.
                if (account?.id == expectedAccount) {
                    user = newUser.copy(follows = guestFollowTransfer.preview(expectedAccount, newUser.follows))
                    applyAccountCategoryFollows(expectedAccount, user.follows)
                    reconcileFeedPreferences(expectedAccount, feedPrefsRevisionOnRequest, newFeedPreferences)
                    // Send additions in the background, without blocking other account pages.
                    scope.launch { synchronizeGuestFollows(expectedAccount, newUser.follows) }
                    newBatecs?.let { batecs = it }
                    newNotices?.let { notices = it }
                    newBells?.let { artistBells = it.filter { b -> b.enabled }.associateBy { b -> IosArtistBellPolicy.key(b.artistName) } }
                    if (remoteVisibility != null && visibilityWriteRevision == visibilityRevisionOnRequest) {
                        val dirtyKey = iosScopedCacheKey("pref_visibility_dirty",expectedAccount)
                        if (platformReadPublicCache(dirtyKey) == "true") {
                            val local = IosVisibilityExclusions(excludedArtists,excludedEventIds,excludedCategories)
                            val ops = mutableListOf<Triple<String,String,Boolean>>()
                            fun delta(kind:String,remote:Set<String>,desired:Set<String>) {
                                (remote - desired).forEach { ops += Triple(kind,it,false) }
                                (desired - remote).forEach { ops += Triple(kind,it,true) }
                            }
                            delta("ARTIST",remoteVisibility.artists,local.artists)
                            delta("EVENT",remoteVisibility.events.map(Int::toString).toSet(),local.events.map(Int::toString).toSet())
                            delta("CATEGORY",remoteVisibility.categories,local.categories)
                            var failures = 0
                            ops.forEach { (kind,key,hide) ->
                                runCatching { api.changeVisibility(kind,key,hide) }.onFailure { failures++ }
                            }
                            if (account?.id == expectedAccount && visibilityWriteRevision == visibilityRevisionOnRequest && failures == 0)
                                platformWritePublicCache(dirtyKey,"false")
                        } else {
                            excludedArtists = remoteVisibility.artists
                            excludedEventIds = remoteVisibility.events
                            excludedCategories = remoteVisibility.categories
                            platformWritePublicCache(iosScopedCacheKey("hidden_events",expectedAccount),
                                IosAccountCachePolicy.encode(remoteVisibility.events.map(Int::toString).toSet()))
                            platformWritePublicCache(iosScopedCacheKey("hidden_artists",expectedAccount),
                                IosAccountCachePolicy.encode(remoteVisibility.artists))
                            platformWritePublicCache(iosScopedCacheKey("hidden_categories",expectedAccount),
                                IosAccountCachePolicy.encode(remoteVisibility.categories))
                        }
                    }
                    if (newPlaces != null && placesWriteRevision == placesRevisionOnRequest) {
                        val dirtyKey = iosScopedCacheKey("pref_places_dirty",expectedAccount)
                        if (platformReadPublicCache(dirtyKey) == "true") {
                            val guestPending = iosScopedSet("intro_pending_towns", expectedAccount)
                            // For onboarding, union the existing account's remote places and the
                            // locally staged guest places. Do not overwrite a returning user's towns.
                            val sendRadii = if (guestPending.isNotEmpty()) {
                                val remote = if (newPlaces.exists) newPlaces.towns else emptyMap()
                                remote + chosenTowns.associateWith { town ->
                                    chosenRadii[town] ?: PersonalizationOptions.defaultRadiusKm
                                }
                            } else chosenTowns.associateWith {
                                chosenRadii[it] ?: PersonalizationOptions.defaultRadiusKm
                            }
                            val sendComarques = if (guestPending.isNotEmpty() && newPlaces.exists)
                                newPlaces.comarques + chosenComarques else chosenComarques
                            if (guestPending.isNotEmpty()) {
                                chosenTowns = sendRadii.keys
                                chosenRadii = sendRadii
                                chosenComarques = sendComarques
                            }
                            runCatching { api.saveLocationPreferences(sendRadii, sendComarques) }.onSuccess {
                                if (account?.id == expectedAccount && placesWriteRevision == placesRevisionOnRequest) {
                                    platformWritePublicCache(dirtyKey,"false")
                                    platformWritePublicCache(iosScopedCacheKey("intro_pending_towns", expectedAccount), "~")
                                    platformWritePublicCache(iosScopedCacheKey("pref_towns", expectedAccount),
                                        IosAccountCachePolicy.encode(sendRadii.keys))
                                    platformWritePublicCache(iosScopedCacheKey("pref_radii", expectedAccount),
                                        IosAccountCachePolicy.encode(IosPersonalFeedPolicy.encodeRadii(sendRadii)))
                                    platformWritePublicCache(iosScopedCacheKey("pref_comarques", expectedAccount),
                                        IosAccountCachePolicy.encode(sendComarques))
                                }
                            }
                        } else if (newPlaces.exists) {
                            chosenTowns = newPlaces.towns.keys
                            chosenRadii = newPlaces.towns
                            chosenComarques = newPlaces.comarques
                            platformWritePublicCache(iosScopedCacheKey("pref_towns",expectedAccount),
                                IosAccountCachePolicy.encode(chosenTowns))
                            platformWritePublicCache(iosScopedCacheKey("pref_radii",expectedAccount),
                                IosAccountCachePolicy.encode(IosPersonalFeedPolicy.encodeRadii(chosenRadii)))
                            platformWritePublicCache(iosScopedCacheKey("pref_comarques",expectedAccount),
                                IosAccountCachePolicy.encode(chosenComarques))
                        }
                    }
                    newTickets?.let { tickets = it }
                    newGroups?.let { groups = it }
                    newSeen?.let { seenBatecs = it }
                    newCreations?.let { batecRecentEventCreations = it }
                    newSavedLists?.let { savedLists = it }
                    newPresets?.let { searchPresets = it }
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        refresh(false)
        val restored = api.restoreAccount()
        awaitingInitialAccount = restored == null && !api.token.isNullOrBlank()
        if (restored == null && api.token.isNullOrBlank() && initialGuestOnboardingEligible &&
            platformReadPublicCache("ios_first_run_state_v350") != "done") {
            platformWritePublicCache("ios_first_run_state_v350", "offered")
            firstRunOpen = true
        }
        if (restored != null) {
            // A recognised returning user should never see anonymous onboarding after logout.
            platformWritePublicCache("ios_first_run_state_v350", "done")
            account = restored
            stageGuestFollows(restored.id)
            stageGuestFirstRunPlaces(restored.id)
            user = user.copy(follows = guestFollowTransfer.preview(restored.id, emptyList()))
            useAccountPreferences(restored.id); loadAccountPages()
            myProfile = runCatching { api.myPublicProfile() }.getOrNull()
        }
    }
    LaunchedEffect(tab, account?.id) {
        if (tab == "Comunitat" && account == null) {
            runCatching { api.publicCommunityGroups() }
                .onSuccess { if (account == null) groups = it }
                .onFailure { if (account == null) status = "No s'han pogut carregar els taulons públics" }
        }
    }
    LaunchedEffect(tab, account?.id) {
        if (tab == "Perfil" && account != null) {
            val expectedId = account?.id
            val currentSettings = runCatching { api.profileSettings() }.getOrNull()
            if (account?.id == expectedId) profileSettings = currentSettings
            val currentProfile = runCatching { api.myPublicProfile() }.getOrNull()
            if (account?.id == expectedId && currentProfile != null) myProfile = currentProfile
        }
    }
    // Batecs have a 48-hour freshness window, and new event publications arrive
    // independently of the 30-minute catalog cache. Refresh invisibly while at Inici.
    LaunchedEffect(tab, account?.id) {
        val expectedId = account?.id
        if (tab == "Inici" && expectedId != null) {
            while (true) {
                kotlinx.coroutines.delay(120_000L)
                val nextPublications = runCatching { api.batecs(user.follows
                    .filter { it.type.equals("ARTIST", true) }.map { it.label }.toSet()) }.getOrNull()
                val nextCreationTimes = runCatching { api.recentBatecEventCreations() }.getOrNull()
                val nextSeen = runCatching { api.seenBatecs() }.getOrNull()
                if (account?.id != expectedId) break
                nextPublications?.let { batecs = it }
                nextCreationTimes?.let { batecRecentEventCreations = it }
                nextSeen?.let { seenBatecs = it }
                batecClockTick = currentEpochMillis()
            }
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
    var today by remember { mutableStateOf(currentLocalDateIso()) }
    // If the app stays open at midnight, expire events and update saved-day labels.
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000L)
            val newDate = currentLocalDateIso()
            if (newDate != today) today = newDate
        }
    }
    // Both an APNs event alert and a scheduled local reminder may arrive before
    // the public catalog finishes loading. Open once the real event is available.
    LaunchedEffect(pendingOpenEventId, events) {
        val eventId = pendingOpenEventId
        if (eventId > 0) {
            val match = events.firstOrNull { it.id == eventId }
            if (match != null) {
                tab = "Inici"
                selected = match
                festivalMatchedArtist = ""
                pendingOpenEventId = 0
            }
        }
    }
    val available = remember(events, paid, today) {
        // The commercial feed is not allowed to lose paid campaigns or internal ticket sales
        // just because the global catalog is stale or incomplete.
        (events + paid.priorityEvents).distinctBy { it.key }
            .filter { it.endDate.ifBlank { it.date }.isBlank() || it.endDate.ifBlank { it.date } >= today }
            .sortedWith(compareBy<AgendaEventSummary> { it.date.ifBlank { "9999" } }.thenBy { it.time }.thenBy { it.id })
    }
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
    // Android does not depend on six manually published server Batecs:
    // it synthesizes recent event news and, as a fallback, grey followed-artist circles.
    val activeBatecs = remember(batecs, user.follows, available, batecRecentEventCreations,
        today, excludedArtists, account?.id, batecClockTick) {
        IosBatecsFeed.assemble(
            server = batecs, follows = user.follows, catalog = available,
            recentEventCreations = batecRecentEventCreations,
            nowMillis = batecClockTick, todayIso = today,
            excludedArtists = excludedArtists
        )
    }
    val followedGroups = remember(activeBatecs, user.follows, seenBatecs,
        excludedArtists, account?.id, batecClockTick) {
        val remaining = user.follows.filter { it.type.equals("ARTIST", true) &&
            IosBatecTimelinePolicy.normal(it.label) !in excludedArtists.map(IosBatecTimelinePolicy::normal) }
        IosBatecArtistGroups.select(
            activeBatecs.map { BatecCandidate(it.storyKey, it.artist, it.publishedAtMillis,
                it.sourceType == "FOLLOWED_PLACEHOLDER") },
            remaining.map { it.label.ifBlank { it.key } }.toSet(), seenBatecs,
            nowMillis = batecClockTick
        )
    }
    val batecRowState = rememberLazyListState()
    LaunchedEffect(batecsReturnEpoch) {
        if (batecsReturnEpoch > 0) batecRowState.scrollToItem(0)
    }
    // Build the suggestion index off the UI thread only when the catalog changes.
    val searchPool by produceState<List<IosSearchSuggestion>>(initialValue = emptyList(), available) {
        value = withContext(Dispatchers.Default) {
            IosAdvancedSearch.pool(available, IosGeoPlaces.allTowns(), IosGeoPlaces.allComarques())
        }
    }
    val suggestions by produceState<List<IosSearchSuggestion>>(initialValue = emptyList(), suggestionDraft, searchPool, chosenSuggestion) {
        value = withContext(Dispatchers.Default) {
            if (chosenSuggestion != null) emptyList() else IosAdvancedSearch.suggest(searchPool, suggestionDraft)
        }
    }
    // Android: changing a feed tab abandons the active query, criteria and named list.
    fun chooseFeedMode(mode: String, category: String? = null) {
        // A deliberate mode change starts a new reading view.
        IosFeedReadingSession.clear()
        feedMode = mode
        selectedFeedCategory = if (mode == "Categoria") category else null
        activeFilterPresetName = null
        query = ""; appliedQuery = ""; suggestionDraft = ""
        chosenSuggestion = null; appliedSuggestion = null
        appliedSearchFilters = IosSearchFilters()
        limit = 5
    }
    fun applyNamedPreset(preset: IosSavedSearch): Boolean {
        val criteria = preset.filters.copy(referenceTowns = chosenTowns)
        if (!criteria.isActive || !criteria.valid()) {
            status = "Revisa el poble d'origen per a aplicar el radi d'esta llista."
            return false
        }
        chooseFeedMode("Sense filtre")
        appliedSearchFilters = criteria
        activeFilterPresetName = preset.name
        limit = 5
        return true
    }
    val categoriesForSearch = remember(available) { available.map { it.category }.filter(String::isNotBlank).distinct().sorted() }
    val stylesForSearch = remember(available) { available.flatMap { it.styles.flatMap { s -> s.split('|',',',';') } }.map(String::trim).filter(String::isNotBlank).distinct().sorted() }
    val effectiveSearchFilters = remember(appliedSearchFilters, chosenTowns) {
        appliedSearchFilters.copy(referenceTowns = chosenTowns)
    }
    val searchInProgress = appliedQuery.isNotBlank() || effectiveSearchFilters.isActive
    // Android resolves an exact artist name even when the person presses Search
    // without tapping a suggestion: return that artist's concerts/festivals exclusively.
    val effectiveSearchSuggestion = remember(appliedQuery,appliedSuggestion,searchPool) {
        appliedSuggestion ?: searchPool.firstOrNull { suggestion ->
            suggestion.type == IosSearchType.ARTIST &&
                IosEventPopupPolicy.artistKey(suggestion.value) == IosEventPopupPolicy.artistKey(appliedQuery) &&
                appliedQuery.isNotBlank()
        }
    }
    // Strict global search: never restrict results to the previous personalized snapshot.
    val computedFiltered by produceState<List<AgendaEventSummary>>(initialValue = emptyList(), available, appliedQuery, effectiveSearchSuggestion, effectiveSearchFilters) {
        value = withContext(Dispatchers.Default) {
            available.filter { IosAdvancedSearch.matches(it, appliedQuery, effectiveSearchSuggestion, effectiveSearchFilters) }
        }
    }
    // Unfiltered renders the catalog immediately, without waiting for an unnecessary re-filter.
    val filtered = if (searchInProgress) computedFiltered else available
    // The potentially costly affinity calculation never runs in the Compose render pass.
    // Only follows (not saved/alerts) and explicit tastes invalidate it; typing does not.
    // Keep the last valid selection visible during an update, but never show another account's.
    val selectedForYou by produceState<Pair<Int?, List<AgendaEventSummary>>>(
        initialValue = null to emptyList(), available, account?.id, user.follows,
        chosenCategories, chosenTowns, chosenSubcategories, chosenComarques, chosenRadii,
        showSuggestions, discoveryLevel, valencianFirst, paid, entityStyles, learningResetRevision, feedPrefsServerRevision
    ) {
        val owner = account?.id
        // Never independently select events when the shared server endpoint is working.
        // Only recover the historical offline snapshot if the server is unavailable.
        val serverIds = runCatching {
            api.serverForYouIds(chosenCategories, chosenSubcategories, chosenTowns,
                chosenComarques, chosenRadii, showSuggestions, discoveryLevel.name,
                valencianFirst, user.follows)
        }.getOrNull()
        val result = if (serverIds != null) {
            withContext(Dispatchers.Default) {
                val lookup = available.groupBy { it.id }
                serverIds.flatMap { lookup[it].orEmpty() }.distinctBy { it.key }
            }
        } else withContext(Dispatchers.Default) {
            IosForYouSnapshot.select(available, owner, user, chosenCategories, chosenTowns, paid,
                chosenSubcategories, chosenComarques, chosenRadii, showSuggestions,
                discoveryLevel, valencianFirst, entityStyles)
        }
        value = owner to result
    }
    val forYou = if (selectedForYou.first == account?.id) selectedForYou.second else emptyList()
    // Commercially guaranteed events retain priority even during searching. No duplicate cards.
    val activeFeed = remember(forYou, filtered, available, paid, feedMode, selectedFeedCategory,
        user.follows, searchInProgress, valencianFirst, today, effectiveSearchSuggestion,
        chosenCategories, chosenTowns) {
        // Commercial commitments remain global; organic results follow the chosen Android tab.
        val commercialIds = paid.guaranteedEventIds + paid.ads.map { it.eventId }
        val guaranteed = available.filter { it.id in commercialIds || it.ticketAvailable }
        val organic = when {
            searchInProgress -> filtered // A manual search is always over the whole catalog.
            feedMode == "Sense filtre" -> available
            feedMode == "Seguits" -> IosFeedModePolicy.followed(available, user.follows, today)
            feedMode == "Categoria" -> IosFeedModePolicy.category(available, selectedFeedCategory, today)
            account == null && forYou.isEmpty() &&
                chosenCategories.isEmpty() && chosenTowns.isEmpty() -> available
            else -> forYou
        }
        if (!searchInProgress && feedMode == "Sense filtre") {
            available.sortedWith(compareBy<AgendaEventSummary> {
                IosUnfilteredFeedPolicy.relevantDate(it.date, it.endDate, today,
                    if (it.isFestival) it.schedule.map { slot -> slot.date } else emptyList())
            }.thenBy { it.time }.thenBy { it.id })
        } else (guaranteed + organic).distinctBy {
            if (searchInProgress && effectiveSearchSuggestion?.type == IosSearchType.ARTIST && it.isFestival)
                "festival_${it.id}" else it.key
        }.sortedWith(compareBy<AgendaEventSummary> {
            IosUnfilteredFeedPolicy.relevantDate(it.date, it.endDate, today,
                if (it.isFestival) it.schedule.map { slot -> slot.date } else emptyList())
        }.thenByDescending { feedMode == "Per a tu" && valencianFirst &&
            (IosGeoPlaces.key(it.language).contains("valencia") ||
             IosGeoPlaces.key(it.language).contains("catala") || it.language.equals("ca",true)) }
            .thenBy { it.time }.thenBy { it.id })
    }
    // Exclusions do not invalidate the 30-minute ordered snapshot; they are applied
    // only to the organic display list. Commercial commitments always override hiding.
    val personalizedFeed = remember(activeFeed, excludedEventIds, excludedArtists, excludedCategories, paid) {
        val protectedIds = paid.guaranteedEventIds + paid.ads.map { it.eventId }
        activeFeed.filter { ev ->
            // Android: a blocked solo artist does not hide a multi-artist festival.
            val performers = (ev.artists + ev.schedule.map { it.artist }).filter(String::isNotBlank).distinct()
            val blockedByCanonicalCategory = IosPersonalizationTaxonomy.categoryForEvent(ev.category)
                ?.let { it in excludedCategories } ?: false
            val blockedBySubcategory = excludedCategories.any { hidden ->
                IosPersonalizationTaxonomy.matchesSubcategory(hidden,ev.category,ev.styles)
            }
            ((ev.id in protectedIds || ev.ticketAvailable) || (!blockedByCanonicalCategory && !blockedBySubcategory)) &&
                IosFeedExclusionPolicy.visible(
                ev.id, if (ev.isFestival && performers.size > 1) emptyList() else performers, ev.category,
                excludedEventIds, excludedArtists, excludedCategories,
                commerciallyGuaranteed = ev.id in protectedIds || ev.ticketAvailable)
        }
    }
    val visibleFeed = remember(personalizedFeed, limit) { personalizedFeed.take(limit) }

    fun showEvent(event: AgendaEventSummary, q: String = "", explicitArtistFocus: Boolean = false) {
        // A visit contributes to optional discovery on its next snapshot refresh,
        // never forces a new feed calculation while navigating the current one.
        IosRecentInterest.recordOpen(account?.id,event)
        selected = event
        // A saved/notified event must not inherit an unrelated old feed search.
        val focusedSearch = q.trim()
        val kind = effectiveSearchSuggestion?.takeIf { suggestion ->
            focusedSearch.isNotBlank() &&
                IosEventPopupPolicy.artistKey(suggestion.value) == IosEventPopupPolicy.artistKey(focusedSearch)
        }?.type
        festivalMatchedArtist = if (explicitArtistFocus && event.isFestival) focusedSearch
            else IosEventPopupPolicy.requestedArtist(event, focusedSearch, kind)
    }
    fun excludeFromFeed(event: AgendaEventSummary, mode: String) {
        when (mode) {
            "EVENT" -> {
                excludedEventIds = excludedEventIds + event.id
                platformWritePublicCache(iosScopedCacheKey("hidden_events", account?.id),
                    IosAccountCachePolicy.encode(excludedEventIds.map(Int::toString).toSet()))
                status = "Esdeveniment ocultat del feed"
            }
            "ARTIST" -> {
                val firstArtist = event.artists.firstOrNull { it.isNotBlank() }
                    ?: event.schedule.firstOrNull { it.artist.isNotBlank() }?.artist ?: return
                excludedArtists = excludedArtists + firstArtist
                platformWritePublicCache(iosScopedCacheKey("hidden_artists", account?.id),
                    IosAccountCachePolicy.encode(excludedArtists))
                status = "Artista ocultat del feed"
            }
            "CATEGORY" -> {
                if (event.category.isBlank()) return
                excludedCategories = excludedCategories + event.category
                platformWritePublicCache(iosScopedCacheKey("hidden_categories", account?.id),
                    IosAccountCachePolicy.encode(excludedCategories))
                status = "Categoria ocultada del feed"
            }
        }
    }
    fun saveExclusions(events: Set<Int>, artists: Set<String>, categories: Set<String>) {
        excludedEventIds = events; excludedArtists = artists; excludedCategories = categories
        platformWritePublicCache(iosScopedCacheKey("hidden_events", account?.id), IosAccountCachePolicy.encode(events.map(Int::toString).toSet()))
        platformWritePublicCache(iosScopedCacheKey("hidden_artists", account?.id), IosAccountCachePolicy.encode(artists))
        platformWritePublicCache(iosScopedCacheKey("hidden_categories", account?.id), IosAccountCachePolicy.encode(categories))
    }
    fun toggleSaved(event: AgendaEventSummary) {
        val next = event.id !in user.saved
        if (account == null) {
            val updated = if (next) user.saved + event.id else user.saved - event.id
            platformWritePublicCache(iosScopedCacheKey("saved_events", null),
                IosAccountCachePolicy.encode(updated.map(Int::toString).toSet()))
            user = user.copy(saved = updated)
            status = if (next) "Esdeveniment guardat en este iPhone" else "Esdeveniment llevat de Guardats"
            return
        }
        val expectedId = account?.id ?: return
        work {
            api.saveEvent(event, next)
            if (account?.id != expectedId) return@work
            user = user.copy(saved = if (next) user.saved + event.id else user.saved - event.id)
            status = if (next) "Esdeveniment guardat" else "Esdeveniment desat"
        }
    }
    fun toggleReminder(event: AgendaEventSummary, next: Boolean, reminderAt: String = "") {
        // Guests use local iOS notifications (identity 0); no private API is called.
        val requestUserId = account?.id ?: 0
        work {
            if (next) {
                require(reminderAt.matches(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}"))) {
                    "Cal indicar data i hora (AAAA-MM-DD HH:MM)."
                }
                require(reminderAt.take(10) <= event.endDate.ifBlank { event.date }) {
                    "El recordatori no pot ser posterior a l'activitat."
                }
                require(platformScheduleReminder(requestUserId, event.id, event.title, reminderAt)) {
                    "iOS no ha pogut programar l'avís. Comprova la data i el permís de notificacions."
                }
                if ((account?.id ?: 0) != requestUserId) {
                    platformCancelReminder(requestUserId, event.id)
                    return@work
                }
                if (requestUserId > 0) {
                    try { api.alertEvent(event, true) }
                    catch (ex: Exception) {
                        platformCancelReminder(requestUserId, event.id)
                        throw ex
                    }
                }
                platformWritePublicCache(iosReminderKey(requestUserId, event.id), reminderAt)
                val nextAlerts = user.alerts + event.id
                // Android parity: enabling an event bell also adds it to General.
                val nextSaved = user.saved + event.id
                if (requestUserId == 0) {
                    platformWritePublicCache(iosScopedCacheKey("alert_events", null),
                        IosAccountCachePolicy.encode(nextAlerts.map(Int::toString).toSet()))
                    platformWritePublicCache(iosScopedCacheKey("saved_events", null),
                        IosAccountCachePolicy.encode(nextSaved.map(Int::toString).toSet()))
                } else if (event.id !in user.saved) {
                    runCatching { api.saveEvent(event, true) }.onFailure {
                        status = "Recordatori activat; el guardat general no s'ha pogut sincronitzar"
                    }
                }
                if ((account?.id ?: 0) == requestUserId) {
                    user = user.copy(alerts = nextAlerts, saved = nextSaved)
                    if (status.isBlank()) status = "Recordatori programat en iPhone"
                }
            } else {
                if (requestUserId > 0) api.alertEvent(event, false)
                platformCancelReminder(requestUserId, event.id)
                if ((account?.id ?: 0) == requestUserId) {
                    platformWritePublicCache(iosReminderKey(requestUserId, event.id), "")
                    val nextAlerts = user.alerts - event.id
                    if (requestUserId == 0) platformWritePublicCache(iosScopedCacheKey("alert_events", null),
                        IosAccountCachePolicy.encode(nextAlerts.map(Int::toString).toSet()))
                    user = user.copy(alerts = nextAlerts)
                    status = "Recordatori desactivat"
                }
            }
        }
    }

    fun toggleFollowEntity(type: String, value: String) {
        val clean = value.trim()
        if (clean.isBlank() || type !in setOf("ARTIST", "ORGANIZER", "VENUE", "CATEGORY")) return
        val canonical = if (type == "CATEGORY") IosPersonalizationTaxonomy.canonicalFollowKey(clean) else clean
        val follow = IosFollow(type, canonical, if (type == "CATEGORY") {
            IosPersonalizationTaxonomy.categoryLabel(canonical).let {
                if (it == canonical) IosPersonalizationTaxonomy.subcategoryLabel(canonical) else it
            }
        } else clean)
        val expectedId = account?.id
        val actionId = "$type:$canonical"
        if (actionId in pendingFollows) return
        val next = !IosGuestFollowTransfer.contains(user.follows, follow)
        if (expectedId == null) {
            val nextFollows = IosFollowMutation.updated(user.follows,type,canonical,follow.label,next)
            guestFollowTransfer.setGuest(nextFollows)
            user = user.copy(follows = nextFollows)
            status = if (next) "Ara seguixes $clean" else "Has deixat de seguir $clean"
            return
        }
        pendingFollows = pendingFollows + actionId
        work {
            try {
                followSyncMutex.withLock {
                    if (account?.id != expectedId) return@withLock
                    api.followEntity(type, canonical, follow.label, next)
                    if (account?.id != expectedId) return@withLock
                    // An explicit unfollow must not be re-added by the pending guest journal.
                    if (!next) guestFollowTransfer.acknowledge(expectedId, follow)
                    user = user.copy(follows = IosFollowMutation.updated(user.follows,
                        type,canonical,follow.label,next))
                    status = if (next) "Ara seguixes $clean" else "Has deixat de seguir $clean"
                }
            } finally { pendingFollows = pendingFollows - actionId }
        }
    }
    fun toggleFollow(value: String) = toggleFollowEntity("ARTIST", value)

    val notificationEntries = remember(notices,events,user.saved,artistBells,notificationPreferences,today,batecClockTick) {
        IosNotificationParity.build(notices, events, user.saved, artistBells.values.toList(),
            today,batecClockTick,notificationPreferences)
    }
    val notificationBadge = notificationEntries.count { entry ->
        entry.key !in dismissedLocalNotifications && entry.key !in readLocalNotifications &&
            (entry.remoteId == null || notices.firstOrNull { it.id == entry.remoteId }?.read == false)
    }
    val notificationSuggestions = remember(events,user.saved,user.follows,chosenCategories,chosenTowns,today,
        showSuggestions,notificationPreferences) {
        IosNotificationParity.suggestions(events,user.saved,user.follows.filter {it.type.equals("ARTIST",true)}.map{it.label}.toSet(),
            chosenCategories,chosenTowns,today,showSuggestions && notificationPreferences.suggestions)
    }
    MaterialTheme(colorScheme = lightColorScheme(primary = Ink, background = Color.White)) {
        Column(Modifier.fillMaxSize().background(Color.White)) {
            // Android parity: same horizontal logo and controls, stable symmetric layout.
            if (tab != "Perfil") Row(Modifier.fillMaxWidth().height(82.dp).padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                AgendaTopAction("person", onClick = { profileReturnTab = tab; tab = "Perfil" })
                AgendaTopAction("plus", color = AgendaGold, onClick = { publisherOpen = true })
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    AgendaBrandLogo(Modifier.fillMaxWidth().widthIn(max = 175.dp).height(58.dp))
                }
                AgendaTopAction("sliders", onClick = { prefOpen = true })
                AgendaTopAction("bell", onClick = {
                    notificationsOpen = true
                    if (account != null) loadAccountPages()
                }, badge = notificationBadge)
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
                                            if (query.isNotBlank()) {
                                                feedMode = "Sense filtre"; selectedFeedCategory = null; activeFilterPresetName = null
                                            }
                                            IosFeedReadingSession.clear()
                                            appliedQuery = query.trim(); appliedSuggestion = chosenSuggestion
                                            limit = 5; IosRecentInterest.recordSearch(account?.id, appliedQuery, available)
                                        }))
                                }
                                Row(Modifier.height(52.dp).background(Color(0xFFF2F2F2), RoundedCornerShape(14.dp))
                                    .clickable { searchFiltersOpen = true }
                                    .padding(horizontal = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                                    AgendaGlyph("sliders", Modifier.size(20.dp), AgendaGold)
                                    Text("Opcions", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                }
                            }
                            if (query.isNotBlank()) {
                                TextButton(onClick = {
                                    feedMode = "Sense filtre"; selectedFeedCategory = null; activeFilterPresetName = null
                                    IosFeedReadingSession.clear()
                                    appliedQuery = query.trim(); appliedSuggestion = chosenSuggestion
                                    limit = 5; IosRecentInterest.recordSearch(account?.id, appliedQuery, available)
                                }, modifier = Modifier.align(Alignment.End)) { Text("Cercar") }
                            }
                            if (suggestions.isNotEmpty() && query.trim() == suggestionDraft && chosenSuggestion == null && tab == "Inici") {
                                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                                    .heightIn(max = 216.dp).verticalScroll(rememberScrollState())) {
                                    suggestions.forEach { suggestion ->
                                        Row(Modifier.fillMaxWidth().clickable {
                                            feedMode = "Sense filtre"; selectedFeedCategory = null; activeFilterPresetName = null
                                            chosenSuggestion = suggestion
                                            query = suggestion.value
                                            IosFeedReadingSession.clear()
                                            appliedQuery = suggestion.value
                                            appliedSuggestion = suggestion
                                            suggestionDraft = ""
                                            limit = 5
                                            IosRecentInterest.recordSearch(account?.id, suggestion.value, available)
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
                            if (followedGroups.size >= 6) {
                                LazyRow(
                                    state = batecRowState,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                                ) {
                                    items(followedGroups, key = { it.key }) { group ->
                                        val cover = activeBatecs.firstOrNull { it.storyKey == group.openingStoryKey }
                                        Column(
                                            Modifier.width(67.dp).clickable { batecStoryKey = group.key },
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(
                                                Modifier.size(56.dp)
                                                    .border(2.dp, if (group.allSeen) Color(0xFFAAAAAA) else Color(0xFF8F59AD), CircleShape)
                                                    .padding(3.dp).clip(CircleShape)
                                                    .background(if (group.allSeen) Color(0xFFE5E5E5) else Accent),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Poster(
                                                    cover?.imageUrl.orEmpty().ifBlank {
                                                        cover?.eventId?.let { eventById[it]?.posterUrl }.orEmpty()
                                                    },
                                                    Modifier.fillMaxSize().clip(CircleShape).alpha(if (group.allSeen) 0.43f else 1f)
                                                )
                                                if (group.storyKeys.size > 1) {
                                                    Box(Modifier.align(Alignment.TopEnd).size(19.dp)
                                                        .background(Color.White, CircleShape), contentAlignment = Alignment.Center) {
                                                        Text(group.storyKeys.size.coerceAtMost(99).toString(),
                                                            color = Ink, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            Text(group.artist, fontSize = 10.sp, maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = if (group.allSeen) Color.Gray else Ink)
                                        }
                                    }
                                }
                            }
                            // Android's 3 independent selectors: search modes/presets, followed artists and categories.
                            Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 5.dp, bottom = 3.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                var modeMenu by remember { mutableStateOf(false) }
                                var categoryMenu by remember { mutableStateOf(false) }
                                Box {
                                    val modeSelected = feedMode == "Per a tu" || feedMode == "Sense filtre"
                                    val modeLabel = when {
                                        appliedQuery.isNotBlank() -> "Resultats de cerca"
                                        activeFilterPresetName != null -> activeFilterPresetName.orEmpty()
                                        searchInProgress -> "Filtres actius"
                                        feedMode == "Sense filtre" -> "Sense filtre"
                                        else -> "Per a tu"
                                    }
                                    Surface(shape = RoundedCornerShape(10.dp),
                                        color = if (modeSelected) Color(0xFFEAEAEA) else Color.Transparent,
                                        modifier = Modifier.clickable {
                                            modeMenu = true
                                            if (account != null) {
                                                val expected = account?.id
                                                work {
                                                    val newest = runCatching { api.savedSearches() }.getOrNull()
                                                    if (account?.id == expected && newest != null) searchPresets = newest
                                                }
                                            }
                                        }) {
                                        Text("$modeLabel ▾", modifier = Modifier.widthIn(max = 123.dp)
                                            .padding(horizontal = 9.dp, vertical = 9.dp), maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontWeight = if (modeSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.sp)
                                    }
                                    DropdownMenu(expanded = modeMenu, onDismissRequest = { modeMenu = false }) {
                                        listOf("Per a tu", "Sense filtre").forEach { mode ->
                                            DropdownMenuItem(text = { Text(mode) }, onClick = {
                                                chooseFeedMode(mode); modeMenu = false
                                            })
                                        }
                                        searchPresets.forEach { preset ->
                                            DropdownMenuItem(onClick = {
                                                if (applyNamedPreset(preset)) modeMenu = false
                                            }, text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(preset.name, Modifier.weight(1f), maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis)
                                                    IconButton(onClick = {
                                                        val expected = account?.id
                                                        work {
                                                            api.removeSavedSearch(preset.name)
                                                            if (account?.id == expected) {
                                                                searchPresets = searchPresets.filterNot { it.name == preset.name }
                                                                if (activeFilterPresetName == preset.name) chooseFeedMode("Per a tu")
                                                            }
                                                        }
                                                    }, modifier = Modifier.size(38.dp)) {
                                                        Text("×", fontSize = 19.sp)
                                                    }
                                                }
                                            })
                                        }
                                    }
                                }
                                Surface(shape = RoundedCornerShape(10.dp),
                                    color = if (feedMode == "Seguits") Color(0xFFEAEAEA) else Color.Transparent,
                                    modifier = Modifier.clickable { chooseFeedMode("Seguits") }) {
                                    Text("Seguits", modifier = Modifier.padding(horizontal = 9.dp, vertical = 9.dp),
                                        fontWeight = if (feedMode == "Seguits") FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp)
                                }
                                Box {
                                    Surface(shape = RoundedCornerShape(10.dp),
                                        color = if (feedMode == "Categoria") Color(0xFFEAEAEA) else Color.Transparent,
                                        modifier = Modifier.clickable { categoryMenu = true }) {
                                        Text("${if (feedMode == "Categoria") IosFeedModePolicy.categoryLabel(selectedFeedCategory) else "Categoria"} ▾",
                                            modifier = Modifier.widthIn(max = 120.dp)
                                                .padding(horizontal = 9.dp, vertical = 9.dp),
                                            fontWeight = if (feedMode == "Categoria") FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                                        if (feedMode == "Categoria") {
                                            DropdownMenuItem(text = { Text("Llevar disciplina · Per a tu") }, onClick = {
                                                chooseFeedMode("Per a tu"); categoryMenu = false
                                            })
                                        }
                                        (IosPersonalizationTaxonomy.categories + IosPersonalizationTaxonomy.children).forEach { category ->
                                            DropdownMenuItem(text = { Text(category.label) }, onClick = {
                                                chooseFeedMode("Categoria", category.code); categoryMenu = false
                                            })
                                        }
                                    }
                                }
                                Spacer(Modifier.weight(1f))
                                Text("${personalizedFeed.size} esdev.", color = Color.Gray, fontSize = 11.sp)
                            }
                            if (searchInProgress || query.isNotBlank()) {
                                TextButton(onClick = {
                                    query = ""; appliedQuery = ""; chosenSuggestion = null; appliedSuggestion = null
                                    chooseFeedMode("Per a tu")
                                }) { Text("Netejar cerca") }
                            }
                            if (searchInProgress) {
                                Text("Filtres actius · ${filtered.size} resultats orgànics", fontSize = 11.sp,
                                    color = Color.DarkGray, modifier = Modifier.padding(horizontal = 16.dp))
                            }
                            if (feedMode == "Seguits" && user.follows.none { it.type.equals("ARTIST", true) }) {
                                Text("Encara no seguixes cap artista o grup. Pots seguir-los des de Personalització.",
                                    modifier = Modifier.padding(horizontal = 16.dp), fontSize = 13.sp)
                            }
                            if (feedMode == "Categoria" && selectedFeedCategory == null) {
                                Text("Tria una categoria per vore els seus esdeveniments.",
                                    modifier = Modifier.padding(horizontal = 16.dp), fontSize = 13.sp)
                            }
                            if (feedMode == "Per a tu" && account != null && !user.follows.any { it.type == "ARTIST" } && chosenCategories.isEmpty()) {
                                Text("Seguix artistes o tria categories a Personalització.", modifier = Modifier.padding(horizontal = 16.dp), fontSize = 13.sp)
                            }
                            if (updating) LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().height(2.dp), color = Ink, trackColor = Accent)
                                },
                                onLoadMore = { limit = IosUnfilteredFeedPolicy.nextCount(limit, personalizedFeed.size) },
                                today = today,
                                scrollContext = "${feedMode}|${selectedFeedCategory}|${activeFilterPresetName}|${appliedQuery}|${effectiveSearchFilters.hashCode()}|${account?.id}",
                                readingEnabled = !awaitingInitialAccount && feedMode == "Per a tu" && !searchInProgress && activeFilterPresetName == null,
                                loadedCount = limit,
                                onRestoreLoaded = { required -> limit = maxOf(limit, required) },
                                feedExpiredEpoch = feedExpiredEpoch,
                                catalogHydrated = IosEventCatalog.fullyHydrated(),
                                savedIds = user.saved, onClick = { showEvent(it, appliedQuery) }, onSave = ::toggleSaved,
                                ads = paid.ads, protectedIds = paid.guaranteedEventIds,
                                commercialEvents = eventById,
                                onAdViewed = ::adViewed, onAdOpened = ::adOpened,
                                refreshing = updating, onRefresh = {
                                    IosFeedReadingSession.clear()
                                    feedExpiredEpoch++
                                    limit = 5
                                    refresh(true)
                                },
                                returnToStartEpoch = batecsReturnEpoch,
                                onExclude = ::excludeFromFeed, onReport = { reportEvent = it })
                        }
                    }
                    "Guardats" -> {
                        val visibleAccountId = account?.id ?: 0
                        key(visibleAccountId) {
                            IosSavedAgendaScreen(
                                accountId = visibleAccountId,
                                authenticated = account != null,
                                onLoginRequired = { loginOpen = true },
                                user = user,
                                lists = if (account == null) emptyList() else savedLists,
                                available = available,
                                today = today,
                                api = api,
                                work = ::work,
                                onListsChanged = { result -> if (account?.id == visibleAccountId && visibleAccountId > 0) savedLists = result },
                                onStatus = { status = it },
                                onOpenEvent = { event, matchedArtist -> showEvent(event, matchedArtist, matchedArtist.isNotBlank()) },
                                onToggleSaved = ::toggleSaved,
                                onDisableReminder = { alertToDisable = it },
                                reminderTime = { event -> platformReadPublicCache(iosReminderKey(visibleAccountId,event.id)) },
                                festivalRevision = savedFestivalRevision
                            )
                        }
                    }
                    "Entrades" -> {
                        Column(Modifier.fillMaxSize()) {
                            PageTitle("Les meues entrades")
                            if (account == null) {
                                Text("Pots consultar les entrades disponibles des de la fitxa de qualsevol esdeveniment. Per recuperar les entrades comprades o emeses al teu nom, inicia sessió.",
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp), fontSize = 14.sp)
                                TextButton(onClick = { tab = "Inici" }, modifier = Modifier.padding(horizontal = 12.dp)) {
                                    Text("Buscar esdeveniments")
                                }
                                TextButton(onClick = { loginOpen = true }, modifier = Modifier.padding(horizontal = 12.dp)) {
                                    Text("Iniciar sessió per recuperar entrades")
                                }
                            } else {
                                TextButton(onClick = { work { tickets = api.tickets() } }, modifier = Modifier.padding(horizontal = 12.dp)) { Text("↻ Actualitzar entrades") }
                                if (tickets.isEmpty()) EmptyState("No hi ha entrades vinculades al teu compte.")
                                else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(tickets, key = { it.id }) { ticket ->
                                        ElevatedCard(Modifier.fillMaxWidth()) {
                                            Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(ticket.eventTitle.ifBlank { ticket.eventId.toIntOrNull()?.let { eventById[it]?.title }.orEmpty() }.ifBlank { "Entrada ${ticket.id}" }, fontWeight = FontWeight.Bold)
                                                Text("Estat: ${ticket.status}", fontSize = 13.sp)
                                                Text("Número: ${ticket.id}", fontSize = 12.sp)
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
                        IosCommunityHub(groups, api,
                            isAdmin = account != null && IosCommunityPolicy.canSeeAdminQueue(account?.role.orEmpty()),
                            authenticated = account != null,
                            reload = { mine -> work {
                                groups = if (account == null) api.publicCommunityGroups() else api.community(mine = mine)
                            } },
                            onOpen = { communityGroup = it })
                    }
                    "Perfil" -> {
                        if (account == null) {
                            IosGuestProfileScreen(
                                onBack = { tab = profileReturnTab },
                                onLogin = { loginOpen = true },
                                onRegister = { registrationOpen = true },
                                onSystemLanguageSettings = { sessionBridge?.showNotificationSettings() }
                            )
                        } else {
                            IosAccountProfileScreen(
                                account = account!!, profile = myProfile,
                                settings = profileSettings, saving = profileSaving,
                                onBack = { tab = profileReturnTab },
                                onPickPhoto = {
                                    sessionBridge?.pickProfilePhoto(object : IosPickedPhotoCallback {
                                        override fun onComplete(base64Jpeg: String, imageError: String) {
                                            if (imageError.isNotBlank()) { error = imageError; return }
                                            val expectedId = account?.id
                                            work {
                                                val url = api.uploadMyPhoto(base64Jpeg)
                                                if (expectedId != null && account?.id == expectedId) {
                                                    myProfile = (myProfile ?: IosMyProfile(account?.username.orEmpty(), "", "")).copy(photoUrl = url)
                                                    status = "Foto actualitzada"
                                                }
                                            }
                                        }
                                    }) ?: run { error = "El selector de fotografies no està disponible." }
                                },
                                onRemovePhoto = {
                                    val expected = account?.id
                                    work {
                                        val base = myProfile ?: IosMyProfile(account?.username.orEmpty(), "", "")
                                        val result = api.updateMyPublicProfile(base.displayName.ifBlank { account?.username.orEmpty() }, base.bio, true)
                                        if (account?.id == expected) { myProfile = result; status = "Foto eliminada" }
                                    }
                                },
                                onSave = { username, email, name, bio, role, phone ->
                                    if (!profileSaving) {
                                        profileSaving = true
                                        val expected = account?.id
                                        work {
                                            try {
                                                if (expected != null && account?.id == expected) {
                                                    if (username != account?.username || email != account?.email) {
                                                        account = api.updateMyAccountIdentity(username, email)
                                                    }
                                                    val updated = api.updateMyPublicProfile(name, bio)
                                                    if (account?.id == expected) myProfile = updated
                                                    val current = profileSettings
                                                    if (account?.role != "SUPER_ADMIN" && current != null &&
                                                        (role != current.role.uppercase() || (role == "PROFESSIONAL" && phone != current.phone))) {
                                                        try {
                                                            val saved = api.saveProfileSettings(role, phone)
                                                            if (account?.id == expected) {
                                                                profileSettings = saved
                                                                account = account?.copy(role = saved.role)
                                                            }
                                                        } catch (roleError: Exception) {
                                                            if (account?.id == expected) status = "Dades personals guardades; no s'ha canviat el rol"
                                                            throw roleError
                                                        }
                                                    }
                                                    if (account?.id == expected) {
                                                        status = "Perfil guardat"
                                                        // A new email may need verification even if the old one was confirmed.
                                                        profileSettings = runCatching { api.profileSettings() }.getOrNull()
                                                    }
                                                }
                                            } finally { profileSaving = false }
                                        }
                                    }
                                },
                                onSendEmailVerification = {
                                    work { api.sendMyEmailVerification(); status = "Consulta el correu per confirmar l'adreça" }
                                },
                                onRefreshEmailVerification = {
                                    val expected = account?.id
                                    work { val next = api.profileSettings(); if (account?.id == expected) profileSettings = next }
                                },
                                onOpenPersonalization = { prefOpen = true },
                                onOpenNotificationSettings = { sessionBridge?.showNotificationSettings() },
                                onOpenLanguageSettings = { sessionBridge?.showNotificationSettings() },
                                onDeleteAccount = { deleteAccountOpen = true },
                                onLogout = { work {
                                    account?.id?.let { sessionBridge?.cancelAllLocalReminders(it) }
                                    try { api.signOutSecurely() } catch (_: Exception) { api.signOut() }
                                    account = null; myProfile = null; profileSettings = null
                                    user = guestUserState(); artistBells = emptyMap(); tickets = emptyList()
                                    notices = emptyList(); groups = emptyList(); seenBatecs = emptySet()
                                    batecRecentEventCreations = emptyMap(); savedLists = emptyList(); searchPresets = emptyList()
                                    activeFilterPresetName = null; selectedFeedCategory = null
                                    appliedSearchFilters = IosSearchFilters(); appliedQuery = ""; query = ""
                                    appliedSuggestion = null; chosenSuggestion = null
                                    useAccountPreferences(null); tab = "Inici"
                                    status = "Sessió tancada"
                                } },
                                api = api, onMessage = { status = it }
                            )
                        }
                    }
                }
            }
            if (tab != "Perfil") Row(Modifier.fillMaxWidth().height(62.dp).background(Color(0xFFF1F1F1))
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
                stageGuestFollows(account!!.id)
                stageGuestFirstRunPlaces(account!!.id)
                profileSettings = null
                user = IosUserState(follows = guestFollowTransfer.preview(account!!.id, emptyList())); artistBells = emptyMap(); tickets = emptyList(); notices = emptyList(); groups = emptyList(); seenBatecs = emptySet(); batecRecentEventCreations = emptyMap(); savedLists = emptyList(); searchPresets = emptyList(); activeFilterPresetName = null; selectedFeedCategory = null; appliedSearchFilters = IosSearchFilters(); appliedQuery = ""; query = ""; appliedSuggestion = null; chosenSuggestion = null
                useAccountPreferences(account?.id)
                registrationOpen = false; loginOpen = false; status = "Compte creat"
                loadAccountPages()
                myProfile = runCatching { api.myPublicProfile() }.getOrNull()
            }
        })
        if (searchFiltersOpen && tab == "Inici") {
            IosSearchFilterSheet(
                initial = effectiveSearchFilters,
                today = today,
                categories = categoriesForSearch,
                availablePresets = searchPresets,
                isSignedIn = account != null,
                onDismiss = { searchFiltersOpen = false },
                onClear = {
                    chooseFeedMode("Per a tu")
                    searchFiltersOpen = false
                },
                onApply = { criteria ->
                    if (criteria.valid()) {
                        val currentQuery = query.trim()
                        val currentSuggestion = chosenSuggestion
                        chooseFeedMode(if (criteria.isActive || currentQuery.isNotBlank()) "Sense filtre" else "Per a tu")
                        appliedSearchFilters = criteria.copy(referenceTowns = chosenTowns)
                        query = currentQuery; appliedQuery = currentQuery
                        chosenSuggestion = currentSuggestion; appliedSuggestion = currentSuggestion
                        limit = 5; searchFiltersOpen = false
                    }
                },
                onLoadPreset = { saved ->
                    if (applyNamedPreset(saved)) searchFiltersOpen = false
                },
                onSavedPreset = { name, filters ->
                    if (applyNamedPreset(IosSavedSearch(name, filters))) searchFiltersOpen = false
                },
                onSave = { name, criteria ->
                    val expectedUser = account?.id ?: error("Inicia sessió abans de guardar filtres.")
                    api.saveSearchFilters(name, criteria)
                    if (account?.id == expectedUser) runCatching { api.savedSearches() }
                        .onSuccess { searchPresets = it }
                        .onFailure { status = "Filtres guardats, però no es pot actualitzar la llista" }
                },
                onDeletePreset = { name ->
                    val expectedUser = account?.id ?: error("Inicia sessió abans d'eliminar filtres.")
                    api.removeSavedSearch(name)
                    if (account?.id == expectedUser) runCatching { api.savedSearches() }
                        .onSuccess { searchPresets = it }
                        .onFailure { status = "Filtres eliminats, però no es pot actualitzar la llista" }
                }
            )
        }
        if (publisherOpen) IosPublisherMenuSheet(
            policy = IosPublisherMenuPolicy.menu(publisherRole, publisherOwnEvents.any { it.status.equals("published",true) }, publisherSuspended),
            loading = publisherLoading, warning = publisherWarning,
            onAction = { action ->
                publisherOpen = false
                publisherDestination = action
            },
            onProfessionalRequest = {
                publisherOpen = false
                if (account == null) loginOpen = true else publisherRequestOpen = true
            },
            onDismiss = { publisherOpen = false }
        )
        if (publisherRequestOpen && account != null) IosProfessionalRequestSheet(
            api = api,
            artists = available.flatMap { it.artists + it.schedule.map { entry -> entry.artist } }
                .filter(String::isNotBlank).distinct().sorted(),
            onDismiss = { publisherRequestOpen = false }
        )
        publisherDestination?.let { destination ->
            IosPublisherPendingDestination(destination, onDismiss = { publisherDestination = null })
        }
        if (loginOpen) LoginDialog(onDismiss = { loginOpen = false }, onSocial = { provider ->
            sessionBridge?.beginSocialSignIn(provider, object : IosSocialAuthCallback {
                override fun onComplete(idToken: String, nonce: String, authError: String) {
                    if (authError.isNotBlank()) { error = authError; return }
                    work {
                        val next = api.signInWithProvider(provider, idToken, nonce)
                        account = next
                        stageGuestFollows(next.id)
                        stageGuestFirstRunPlaces(next.id)
                        profileSettings = null
                        user = IosUserState(follows = guestFollowTransfer.preview(next.id, emptyList())); artistBells = emptyMap(); tickets = emptyList(); notices = emptyList()
                        groups = emptyList(); seenBatecs = emptySet(); batecRecentEventCreations = emptyMap(); savedLists = emptyList(); searchPresets = emptyList(); activeFilterPresetName = null; selectedFeedCategory = null
                        myProfile = null; useAccountPreferences(next.id)
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
                stageGuestFollows(account!!.id)
                stageGuestFirstRunPlaces(account!!.id)
                profileSettings = null
                user = IosUserState(follows = guestFollowTransfer.preview(account!!.id, emptyList())); artistBells = emptyMap(); tickets = emptyList(); notices = emptyList(); groups = emptyList(); seenBatecs = emptySet(); batecRecentEventCreations = emptyMap(); savedLists = emptyList(); searchPresets = emptyList(); activeFilterPresetName = null; selectedFeedCategory = null; appliedSearchFilters = IosSearchFilters(); appliedQuery = ""; query = ""; appliedSuggestion = null; chosenSuggestion = null
                useAccountPreferences(account?.id)
                loginOpen = false; status = "Sessió iniciada"
                loadAccountPages()
                myProfile = runCatching { api.myPublicProfile() }.getOrNull()
            }
        })
        if (prefOpen) IosPersonalizationScreen(
            categories = chosenCategories, towns = chosenTowns,
            hiddenEventIds = excludedEventIds, hiddenArtists = excludedArtists,
            hiddenCategories = excludedCategories, events = available,
            townOptions = events.map { it.town }.filter(String::isNotBlank).distinct().sorted(),
            subcategories = chosenSubcategories, comarques = chosenComarques,
            radii = chosenRadii, suggestions = showSuggestions,
            discovery = discoveryLevel, valencian = valencianFirst,
            artistFollows = user.follows.filter { it.type.equals("ARTIST",true) }.map { it.label }.toSet(),
            categoryFollows = user.follows.filter { it.type.equals("CATEGORY",true) }.map { it.key }.toSet(),
            alertedArtists = artistBells.values.filter { it.enabled }.map { it.artistName }.toSet(),
            accountSignedIn = account != null,
            entityImages = remember(personalizationImagesRevision,entityStyles) {
                IosPersonalizationImages.cached()
            },
            notificationsEnabled = personalizationPushEnabled,
            onNotificationsEnabled = { enable ->
                if (!enable) {
                    personalizationPushEnabled = false
                    platformWritePublicCache(iosScopedCacheKey("pref_ios_push", account?.id), "false")
                    sessionBridge?.showNotificationSettings()
                } else {
                    val accountAtRequest = account?.id
                    sessionBridge?.requestRemoteNotifications(object : IosReminderSchedulingCallback {
                        override fun onResult(success: Boolean, message: String) {
                            if (account?.id != accountAtRequest) return
                            if (success) {
                                personalizationPushEnabled = true
                                platformWritePublicCache(iosScopedCacheKey("pref_ios_push",accountAtRequest), "true")
                                status = "Permís de notificacions acceptat"
                            } else {
                                personalizationPushEnabled = false
                                error = "No s'han pogut activar els avisos iOS: $message"
                            }
                        }
                    }) ?: run { error = "No hi ha disponible el pont de notificacions d'iOS." }
                }
            },
            onFollowArtist = { name -> toggleFollow(name) },
            onArtistAlerts = { name, enabled ->
                val id = account?.id
                if (id == null) loginOpen = true else work {
                    val old = artistBells[IosArtistBellPolicy.key(name)]
                    runCatching { api.saveArtistBell(name,enabled,old?.frequency ?: "WEEKLY") }
                        .onSuccess { saved ->
                            if (account?.id == id) {
                                artistBells = IosArtistBellPolicy.update(artistBells,saved)
                                status = if (enabled) "Avisos de $name activats" else "Avisos desactivats"
                            }
                        }.onFailure { error = "No s'ha guardat la campaneta: ${it.message.orEmpty()}" }
                }
            },
            onResetLearning = {
                val id = account?.id
                platformWritePublicCache(iosScopedCacheKey("recent_styles",id), "")
                // Invalidate the personalized feed immediately. Preserve explicit follows, places and blocks.
                platformWritePublicCache("for_you_${id ?: "guest"}_signature", "")
                learningResetRevision += 1
                status = "Gustos apresos reiniciats en este dispositiu"
                if (id != null) work {
                    runCatching { api.resetLearnedPersonalization() }
                        .onFailure { error = "Reinici local fet. El servidor no ha confirmat el reinici: ${it.message.orEmpty()}" }
                }
            },
            onDismiss = { prefOpen = false },
            onSave = { cats, towns, hiddenEvents, hiddenArtists, hiddenCategories,
                styles, comarques, radii, suggestions, discovery, valencian ->
                val accountId = account?.id
                // Precisely the follow keys used by Android: main codes and 55 subcategory codes.
                val nextKeys = (cats + styles).filter { key ->
                    key in IosPersonalizationTaxonomy.categories.map { it.code } ||
                        key == "CHILDREN" ||
                        IosPersonalizationTaxonomy.subcategories.any { it.code == key }
                }.toSet()
                val serverKeys = user.follows.filter { it.type.equals("CATEGORY",true) }
                    .map { IosPersonalizationTaxonomy.canonicalFollowKey(it.key) }.toSet()
                val oldHiddenEvents = excludedEventIds
                val oldHiddenArtists = excludedArtists
                val oldHiddenCategories = excludedCategories
                val visibilityChanged = oldHiddenEvents != hiddenEvents || oldHiddenArtists != hiddenArtists ||
                    oldHiddenCategories != hiddenCategories
                if (visibilityChanged) visibilityWriteRevision += 1
                val placesChanged = towns != chosenTowns || comarques != chosenComarques || radii != chosenRadii
                chosenCategories = cats; chosenTowns = towns
                chosenSubcategories = styles; chosenComarques = comarques; chosenRadii = radii
                if (placesChanged) placesWriteRevision += 1
                val feedPrefsChanged = showSuggestions != suggestions || discoveryLevel != discovery || valencianFirst != valencian
                if (feedPrefsChanged) feedPrefsWriteRevision += 1
                showSuggestions = suggestions; discoveryLevel = discovery; valencianFirst = valencian
                saveExclusions(hiddenEvents, hiddenArtists, hiddenCategories)
                if (accountId != null && visibilityChanged) {
                    val revision = visibilityWriteRevision
                    val dirtyKey = iosScopedCacheKey("pref_visibility_dirty",accountId)
                    platformWritePublicCache(dirtyKey,"true")
                    val ops = mutableListOf<Triple<String,String,Boolean>>()
                    fun delta(kind:String,old:Set<String>,next:Set<String>) {
                        (old - next).forEach { ops += Triple(kind,it,false) }
                        (next - old).forEach { ops += Triple(kind,it,true) }
                    }
                    delta("EVENT",oldHiddenEvents.map(Int::toString).toSet(),hiddenEvents.map(Int::toString).toSet())
                    delta("ARTIST",oldHiddenArtists,hiddenArtists)
                    delta("CATEGORY",oldHiddenCategories,hiddenCategories)
                    work {
                        var failures = 0
                        for ((kind,target,hide) in ops) {
                            runCatching { api.changeVisibility(kind,target,hide) }.onFailure { failures++ }
                        }
                        if (account?.id == accountId && revision == visibilityWriteRevision && failures == 0)
                            platformWritePublicCache(dirtyKey,"false")
                        if (failures > 0) error = "${failures} bloquejos no s'han sincronitzat. S'han guardat a iPhone."
                    }
                }
                platformWritePublicCache(iosScopedCacheKey("pref_categories",accountId), IosAccountCachePolicy.encode(cats))
                platformWritePublicCache(iosScopedCacheKey("pref_towns",accountId), IosAccountCachePolicy.encode(towns))
                platformWritePublicCache(iosScopedCacheKey("pref_subcategories",accountId), IosAccountCachePolicy.encode(styles))
                platformWritePublicCache(iosScopedCacheKey("pref_comarques",accountId), IosAccountCachePolicy.encode(comarques))
                platformWritePublicCache(iosScopedCacheKey("pref_radii",accountId), IosAccountCachePolicy.encode(IosPersonalFeedPolicy.encodeRadii(radii)))
                platformWritePublicCache(iosScopedCacheKey("pref_suggestions",accountId),suggestions.toString())
                platformWritePublicCache(iosScopedCacheKey("pref_discovery",accountId),discovery.name)
                platformWritePublicCache(iosScopedCacheKey("pref_valencian",accountId),valencian.toString())
                if (accountId != null && (feedPrefsChanged || platformReadPublicCache(iosScopedCacheKey("pref_feed_dirty", accountId)) == "true")) {
                    val revision = feedPrefsWriteRevision
                    val dirtyKey = iosScopedCacheKey("pref_feed_dirty",accountId)
                    platformWritePublicCache(dirtyKey,"true")
                    work {
                        feedPrefsSyncMutex.withLock {
                            if (account?.id != accountId) return@withLock
                            // Always persist the latest draft, not an earlier rapid tap's payload.
                            val latestRevision = feedPrefsWriteRevision
                            runCatching { api.saveFeedPreferences(showSuggestions,discoveryLevel.name,valencianFirst) }
                                .onSuccess { if (account?.id == accountId && latestRevision == feedPrefsWriteRevision) {
                                    platformWritePublicCache(dirtyKey,"false")
                                    feedPrefsServerRevision += 1
                                } }
                                .onFailure { if (account?.id == accountId)
                                    error = "Preferències de Per a tu guardades en iPhone, pendents de servidor: ${it.message.orEmpty()}" }
                        }
                    }
                }
                if (accountId != null && placesChanged) {
                    val revision = placesWriteRevision
                    val dirtyKey = iosScopedCacheKey("pref_places_dirty",accountId)
                    platformWritePublicCache(dirtyKey,"true")
                    work {
                        runCatching { api.saveLocationPreferences(towns.associateWith {
                            radii[it] ?: PersonalizationOptions.defaultRadiusKm
                        },comarques) }.onSuccess {
                            if (account?.id == accountId && revision == placesWriteRevision)
                                platformWritePublicCache(dirtyKey,"false")
                        }.onFailure { error = "Llocs guardats en iPhone, però falta sincronitzar-los: ${it.message.orEmpty()}" }
                    }
                }
                if (accountId == null) {
                    val guestWithoutCategories = user.follows.filterNot { it.type.equals("CATEGORY",true) }
                    val guestCategories = nextKeys.map { key ->
                        val label = IosPersonalizationTaxonomy.categoryLabel(key).let { if (it == key)
                            IosPersonalizationTaxonomy.subcategoryLabel(key) else it }
                        IosFollow("CATEGORY",key,label)
                    }
                    val merged = IosGuestFollowTransfer.union(guestWithoutCategories,guestCategories)
                    guestFollowTransfer.setGuest(merged)
                    user = user.copy(follows = merged)
                }
                if (accountId != null && nextKeys != serverKeys) work {
                    // Persist changes through the same user-state/follow.php service as Android.
                    val removals = serverKeys - nextKeys
                    val additions = nextKeys - serverKeys
                    val outcomes = (removals.map { it to false } + additions.map { it to true })
                    var failed = 0
                    followSyncMutex.withLock {
                        for ((key, following) in outcomes) {
                            if (account?.id != accountId) break
                            val label = IosPersonalizationTaxonomy.categoryLabel(key).let { if (it == key)
                                IosPersonalizationTaxonomy.subcategoryLabel(key) else it }
                            runCatching { api.followEntity("CATEGORY",key,label,following) }
                                .onSuccess {
                                    if (account?.id == accountId) {
                                        if (!following) guestFollowTransfer.acknowledge(accountId,IosFollow("CATEGORY",key,label))
                                        user = user.copy(follows = IosFollowMutation.updated(
                                            user.follows,"CATEGORY",key,label,following))
                                    }
                                }.onFailure { failed++ }
                        }
                    }
                    if (failed > 0) error = "$failed seguiments de categoria no s'han sincronitzat amb el servidor."
                }
                status = "Preferències actualitzades"
            }
        )
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
        if (notificationsOpen) NotificationsDialog(
            notifications = notices,
            artistBells = artistBells.values.sortedBy { it.artistName },
            reminders = events.filter { it.id in user.alerts &&
                (it.endDate.ifBlank { it.date }.isBlank() || it.endDate.ifBlank { it.date } >= today) },
            tickets = tickets, suggestions = notificationSuggestions,
            eventsById = eventById, today = today,accountId=account?.id,api=api,
            preferences=notificationPreferences,onPreferencesChange={ next ->
                notificationPreferences=next
                platformWritePublicCache(iosScopedCacheKey("notification_preferences",account?.id),next.encode())
            },readLocalIds=readLocalNotifications,dismissedIds=dismissedLocalNotifications,
            onReadLocal=::markLocalNotificationRead,
            onDismissAll={ keys ->
                dismissedLocalNotifications=dismissedLocalNotifications+keys
                readLocalNotifications=readLocalNotifications+keys
                platformWritePublicCache(iosScopedCacheKey("notification_dismissed",account?.id),
                    IosAccountCachePolicy.encode(dismissedLocalNotifications))
                platformWritePublicCache(iosScopedCacheKey("notification_read",account?.id),
                    IosAccountCachePolicy.encode(readLocalNotifications))
                val pending=notices.filter{ "server:${it.id}" in keys && !it.read }
                if(pending.isNotEmpty()) work {
                    pending.forEach { runCatching { api.markRead(it.id) } }
                    notices=notices.map { notice -> if(notice in pending) notice.copy(read=true) else notice }
                }
            },onDismiss={notificationsOpen=false},
            onRead={notice ->
                work { runCatching { api.markRead(notice.id) }.onSuccess {
                    notices=notices.map { if(it.id==notice.id) it.copy(read=true) else it }
                }.onFailure { error="No s'ha pogut marcar com a llegida: ${it.message.orEmpty()}" } }
                when(notice.linkType.uppercase()) {
                    "EVENT", "EVENT_ID", "EVENT_DETAIL", "UPCOMING", "UPCOMING_EVENT", "EVENT_REMINDER", "REMINDER",
                    "NEW_EVENT", "SUGGESTION", "SUGGESTED_EVENT", "RECOMMENDATION" -> notice.linkValue.toIntOrNull()?.let { id ->
                        eventById[id]?.let { notificationsOpen=false;showEvent(it) }
                    }
                    "TICKET", "TICKETS", "TICKET_ID", "ORDER", "TICKET_ORDER" -> {notificationsOpen=false;tab="Entrades"}
                    "COMMUNITY", "COMMUNITY_GROUP", "COMMUNITY_MESSAGE", "BOARD" -> {notificationsOpen=false;tab="Comunitat"}
                    "ARTIST", "NEW_ARTIST_EVENT", "FOLLOWED_ARTIST_NEW" -> if(notice.linkValue.isNotBlank()) {
                        notificationsOpen=false;artistSourceEvent=null;artist=notice.linkValue
                    }
                }
            },onReminderClick={ev->notificationsOpen=false;showEvent(ev)},
            onReminderDisable={ev->notificationsOpen=false;alertToDisable=ev},
            onArtistBell={name->notificationsOpen=false;artistBellEditing=name},
            onOpenArtist={name->notificationsOpen=false;artistSourceEvent=null;artist=name},
            onArtistDisable={name->
                val existing=artistBells[IosArtistBellPolicy.key(name)]
                if(existing!=null) work {
                    runCatching { api.saveArtistBell(name,false,existing.frequency) }
                        .onSuccess { artistBells=artistBells-IosArtistBellPolicy.key(name);status="Campaneta desactivada" }
                        .onFailure { error="No s'ha pogut desactivar: ${it.message.orEmpty()}" }
                }
            },
            onOpenTickets={notificationsOpen=false;tab="Entrades"},
            onOpenSuggestion={ev->notificationsOpen=false;showEvent(ev)},
            reminderTime={ev->account?.id?.let{id->platformReadPublicCache(iosReminderKey(id,ev.id))}.orEmpty()},
            entries=notificationEntries)
        val openedBatecGroup = followedGroups.firstOrNull { it.key == batecStoryKey }
        if (batecStoryKey != null && openedBatecGroup != null) {
            BatecViewer(
                groups = followedGroups,
                selectedKey = openedBatecGroup.key,
                stories = activeBatecs,
                allEvents = available,
                onDismiss = ::closeBatecs,
                onOpenEvent = { id ->
                    closeBatecs()
                    eventById[id]?.let { showEvent(it) }
                },
                onGroupChanged = { next -> batecStoryKey = next.key }
            )
            // Mark every actual publication of the artist as seen. A failed request
            // must not invent server state or turn the ring grey optimistically.
            LaunchedEffect(batecStoryKey, account?.id) {
                val expectedId = account?.id
                val group = followedGroups.firstOrNull { it.key == batecStoryKey }
                if (expectedId != null && group != null) {
                    val pending = group.storyKeys.filter { it !in seenBatecs &&
                        IosBatecTimelinePolicy.isServerSeenKey(it) }
                    // This job belongs to the screen, not to the current story:
                    // a quick swipe must not cancel marking the previous group.
                    scope.launch {
                        pending.forEach { key ->
                            if (account?.id != expectedId) return@launch
                            runCatching { api.markBatecSeen(key) }.onSuccess {
                                if (account?.id == expectedId) seenBatecs = seenBatecs + key
                            }
                        }
                    }
                }
            }
        }
        selected?.let { ev -> EventDetailSheet(ev, user.saved.contains(ev.id), user.alerts.contains(ev.id),
            matchedArtist = festivalMatchedArtist,
            followsEntity = { type, name -> user.followsType(type, name) },
            onToggleFollowEntity = { type, name -> toggleFollowEntity(type, name) },
            onDismiss = { selected = null; festivalMatchedArtist = ""; savedFestivalRevision++ },
            onSave = { toggleSaved(ev) }, onReminder = { if (ev.id in user.alerts) alertToDisable = ev else reminderToSchedule = ev },
            userId = account?.id, onReport = { selected = null; reportEvent = ev },
            loadSchedule = api::festivalSchedule,
            onArtist = { artistSourceEvent = ev; selected = null; artist = it }, onCommunity = {
                selected = null
                val requestedId = account?.id
                tab = "Comunitat"
                work {
                    val loaded = if (requestedId == null) api.publicCommunityGroups(ev.id) else api.community(ev.id)
                    if (account?.id == requestedId) groups = loaded
                }
            }, onCreateGroup = {
                if (account == null) loginOpen = true else { selected = null; groupCreateFor = ev }
            }, onTickets = {
                if (ev.ticketUrl.startsWith("https://")) platformOpenExternal(ev.ticketUrl)
                else if (ev.ticketAvailable) {
                    selected = null
                    ticketEvent = ev // Viewing ticket options is public; reservation requires auth.
                } else status = "Este esdeveniment no té venda d'entrades disponible."
            }) }
        artist?.let { value ->
            val matchingEvents = available.filter { e ->
                e.artists.any { it.equals(value, true) } || e.schedule.any { it.artist.equals(value, true) }
            }
            // A schedule entry may exist only in the detailed remote festival response,
            // not the abbreviated feed: keep that festival visible in this artist popup.
            val fromFestival = artistSourceEvent?.let { listOf(it) }.orEmpty()
            ArtistDialog(value, user.follows(value), (fromFestival + matchingEvents).distinctBy { it.key },
                onFollow = { toggleFollow(value) },
                bell = artistBells[IosArtistBellPolicy.key(value)],
                onBell = {
                    if (account == null) loginOpen = true
                    else {
                        artistBellEditing = value
                        val requestedUser = account?.id
                        work {
                            val bells = api.artistBells()
                            if (account?.id == requestedUser) artistBells = bells.filter { b -> b.enabled }
                                .associateBy { b -> IosArtistBellPolicy.key(b.artistName) }
                        }
                    }
                },
                onDismiss = { artist = null; artistSourceEvent = null },
                onEvent = { artist = null; artistSourceEvent = null; showEvent(it, value, true) })
        }
        artistBellEditing?.let { selectedArtist ->
            val existing = artistBells[IosArtistBellPolicy.key(selectedArtist)]
            ArtistBellEditorDialog(selectedArtist, existing,
                onDismiss = { artistBellEditing = null },
                onSave = { enabled, frequency ->
                    val requestUser = account?.id
                    if (requestUser == null) { artistBellEditing = null; loginOpen = true }
                    else work {
                        val saved = api.saveArtistBell(selectedArtist, enabled, frequency)
                        if (account?.id == requestUser) {
                            artistBells = IosArtistBellPolicy.update(artistBells, saved)
                            artistBellEditing = null
                            status = if (enabled) "Campaneta guardada · ${IosArtistBellPolicy.label(frequency)}" else "Campaneta desactivada"
                            if (enabled) IosNativeBridge.current?.requestRemoteNotifications(object : IosReminderSchedulingCallback {
                                override fun onResult(success: Boolean, message: String) { /* Apple controls the permission separately. */ }
                            })
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
                        account = account?.copy(role = newSettings.role)
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
                    sessionBridge?.pickProfilePhoto(object : IosPickedPhotoCallback {
                        override fun onComplete(base64Jpeg: String, imageError: String) {
                            if (imageError.isNotBlank()) { error = imageError; return }
                            val expectedId = account?.id
                            work {
                                val url = api.uploadMyPhoto(base64Jpeg)
                                if (expectedId != null && account?.id == expectedId) {
                                    myProfile = (myProfile ?: IosMyProfile("", "", "")).copy(photoUrl = url)
                                    status = "Foto guardada al servidor"
                                }
                            }
                        }
                    }) ?: run { error = "El selector de fotografies no està disponible." }
                },
                onSave = { name, bio, remove ->
                    val expectedId = account?.id
                    work {
                        val saved = api.updateMyPublicProfile(name, bio, remove)
                        if (account?.id == expectedId) { myProfile = saved; editProfileOpen = false; status = "Perfil guardat" }
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
                        account = null; myProfile = null; user = guestUserState(); artistBells = emptyMap()
                        tickets = emptyList(); notices = emptyList(); groups = emptyList(); seenBatecs = emptySet(); batecRecentEventCreations = emptyMap()
                        savedLists = emptyList(); searchPresets = emptyList()
                        sessionBridge?.eraseLocalAccountData(expectedId ?: 0)
                        useAccountPreferences(null); deleteAccountOpen = false
                        status = "Compte desactivat i dades personals retirades del servidor"
                    }
                }
            })
        }
        if (validatorOpen) ProfessionalValidationDialog(api, onDismiss = { validatorOpen = false })
        ticketEvent?.let { event -> TicketOptionsDialog(event, api,
            authenticated = account != null,
            onLoginRequired = { ticketEvent = null; loginOpen = true },
            onDismiss = { ticketEvent = null }, onPurchased = {
                ticketEvent = null; status = "Entrades gratuïtes emeses correctament"; tab = "Entrades"
                work { tickets = api.tickets() }
            }, onError = { error = it }) }
        groupCreateFor?.let { event -> CreateGroupDialog(event, api, onDismiss = { groupCreateFor = null },
            onCreated = { groupCreateFor = null; status = "Tauló creat"; work { groups = api.community(event.id) } },
            onError = { error = it }) }
        communityGroup?.let { g ->
            if (account == null) IosGuestCommunityPreview(g,
                onDismiss = { communityGroup = null }, onLogin = { communityGroup = null; loginOpen = true })
            else IosCommunityChatDialog(g, api, account?.id ?: 0,
                onDismiss = { communityGroup = null }, onChanged = { work { groups = api.community() } })
        }
        reminderToSchedule?.let { ev -> ReminderEditorDialog(ev, account?.id ?: 0,
            onDismiss = { reminderToSchedule = null }, onSave = { requestedAt ->
            reminderToSchedule = null
            toggleReminder(ev, true, requestedAt)
        }) }
        alertToDisable?.let { ev -> AlertDialog(onDismissRequest = { alertToDisable = null }, title = { Text("Vols desactivar este recordatori?") },
            text = { Text(ev.title) }, dismissButton = { TextButton(onClick = { alertToDisable = null }) { Text("Mantindre") } },
            confirmButton = { TextButton(onClick = { toggleReminder(ev, false); alertToDisable = null }) { Text("Desactivar") } }) }
    }
    if (firstRunOpen && account == null) {
        IosFirstRunOnboardingDialog(
            initialCategories = (chosenCategories + chosenSubcategories.mapNotNull { sub ->
                IosPersonalizationTaxonomy.subcategories.firstOrNull { it.code == sub }?.categoryCode
            }).intersect(IosFirstRunPolicy.disciplines.toSet()),
            initialSubcategories = chosenSubcategories,
            initialLocations = chosenTowns.associateWith { chosenRadii[it] ?: PersonalizationOptions.defaultRadiusKm },
            initialArtists = user.follows.filter { it.type.equals("ARTIST",true) }.map { it.label }.toSet(),
            events = events,
            entityImages = remember(personalizationImagesRevision) { IosPersonalizationImages.cached() },
            onDraftChanged = ::saveGuestFirstRun,
            onSkip = ::completeFirstRun,
            onContinueGuest = ::completeFirstRun,
            onLogin = { completeFirstRun(); loginOpen = true },
            onRegister = { completeFirstRun(); registrationOpen = true }
        )
    }
}

@Composable
private fun ReminderEditorDialog(event: AgendaEventSummary, userId: Int, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val previous = remember(event.id, userId) { platformReadPublicCache(iosReminderKey(userId, event.id)) }
    var date by remember(event.id) { mutableStateOf(previous.take(10).ifBlank { event.date.take(10) }) }
    var time by remember(event.id) { mutableStateOf(previous.drop(11).take(5).ifBlank { "10:00" }) }
    val valid = date.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) &&
        time.matches(Regex("(?:[01]\\d|2[0-3]):[0-5]\\d"))
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
    val shownDate = IosUnfilteredFeedPolicy.relevantDate(ev.date, ev.endDate, today,
        if (ev.isFestival) scheduledDays else emptyList())
    val ongoing = ev.isFestival && IosUnfilteredFeedPolicy.ongoingWithoutSchedule(
        ev.date, ev.endDate, today, scheduledDays)
    val subtitle = if (ev.isFestival && !ongoing) {
        IosUnfilteredFeedPolicy.festivalSummary(shownDate,
            ev.schedule.map { it.date to it.artist.ifBlank { it.title } },
            ev.artists.joinToString(" · ").ifBlank { ev.subtitle })
    } else if (ev.isFestival) "" else ev.subtitle
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
                val displayedTime = if (ongoing) "" else ev.time
                val ticket = IosUnfilteredFeedPolicy.ticketLabel(ev.ticketAvailable, ev.price)
                Text("◷ ${displayedTime.ifBlank { "—" }} · $ticket", fontSize = 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.DarkGray)
            }
            Spacer(Modifier.width(5.dp))
            Column(Modifier.width(64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(ev.category, fontSize = 9.sp, maxLines = 1,
                    color = categoryColor, overflow = TextOverflow.Ellipsis)
                Text(if (ongoing) "En curs" else IosUnfilteredFeedPolicy.cardDate(shownDate),
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

@OptIn(ExperimentalMaterial3Api::class, kotlinx.coroutines.FlowPreview::class)
@Composable
private fun EventList(events: List<AgendaEventSummary>, total: Int,
                      headerContent: @Composable () -> Unit, onLoadMore: () -> Unit,
                      today: String, scrollContext: String,
                      readingEnabled: Boolean, loadedCount: Int,
                      onRestoreLoaded: (Int) -> Unit, feedExpiredEpoch: Int,
                      catalogHydrated: Boolean,
                      savedIds: Set<Int>, onClick: (AgendaEventSummary) -> Unit,
                      onSave: (AgendaEventSummary) -> Unit, ads: List<IosAd>,
                      protectedIds: Set<Int>, commercialEvents: Map<Int, AgendaEventSummary>,
                      onAdViewed: (IosAd) -> Unit,
                      onAdOpened: (IosAd, AgendaEventSummary) -> Unit,
                      refreshing: Boolean, onRefresh: () -> Unit,
                      returnToStartEpoch: Int,
                      onExclude: (AgendaEventSummary, String) -> Unit,
                      onReport: (AgendaEventSummary) -> Unit) {
    // LazyListState lives across ordinary recompositions; disk state survives full relaunches.
    val listState = rememberLazyListState()
    val restored = remember(scrollContext, readingEnabled, feedExpiredEpoch) {
        if (readingEnabled) IosFeedReadingSession.load(scrollContext) else null
    }
    var restoredToCard by remember(scrollContext, readingEnabled, feedExpiredEpoch) {
        mutableStateOf(restored == null)
    }
    LaunchedEffect(scrollContext, readingEnabled, feedExpiredEpoch) {
        if (restored != null) {
            // A restored index is beyond the initial five cards: hydrate enough BEFORE scrollToItem.
            onRestoreLoaded(maxOf(5, restored.loaded, restored.index + 20))
        } else {
            listState.scrollToItem(0)
        }
    }
    LaunchedEffect(scrollContext, feedExpiredEpoch, events.size, total, restored, catalogHydrated) {
        val target = restored
        if (target != null && !restoredToCard && events.isNotEmpty()) {
            // Wait for Compose to lay out the newly hydrated batch, avoiding an early clamp to 0.
            val required = target.index
            if (events.size + 1 > required || (catalogHydrated && events.size >= total)) {
                val count = snapshotFlow { listState.layoutInfo.totalItemsCount }.first {
                    it > required || (catalogHydrated && events.size >= total && it > 0)
                }
                listState.scrollToItem(required.coerceIn(0, count - 1), target.offset)
                restoredToCard = true
            }
        }
    }
    // Reproduce Android's exact index + pixel offset; keep the latest unsaved pixels in RAM
    // so entering the iOS background flushes them without waiting for the debounce.
    LaunchedEffect(scrollContext, readingEnabled, restoredToCard, loadedCount, feedExpiredEpoch) {
        if (readingEnabled && restoredToCard) {
            snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
                .onEach { (index, offset) ->
                    IosFeedReadingSession.cache(scrollContext, index, offset, loadedCount)
                }
                .debounce(350L)
                .collect { IosFeedReadingSession.flush() }
        }
    }
    DisposableEffect(scrollContext, readingEnabled, restoredToCard) {
        onDispose { if (readingEnabled && restoredToCard) IosFeedReadingSession.flush() }
    }
    // Batecs intentionally return to the first card, even with a persisted reading session.
    LaunchedEffect(returnToStartEpoch) {
        if (returnToStartEpoch > 0) {
            listState.scrollToItem(0)
            if (readingEnabled) {
                IosFeedReadingSession.cache(scrollContext, 0, 0, loadedCount)
                IosFeedReadingSession.flush()
            }
        }
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
                    val isCommercial = ev.id in protectedIds || ev.ticketAvailable || ev.id in advertisedEventIds
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
private fun EventDetailSheet(ev: AgendaEventSummary, saved: Boolean, reminded: Boolean, matchedArtist: String,
                             followsEntity: (String, String) -> Boolean, onToggleFollowEntity: (String, String) -> Unit,
                             onDismiss: () -> Unit, onSave: () -> Unit, onReminder: () -> Unit,
                             userId: Int?, onReport: () -> Unit,
                             loadSchedule: suspend (Int) -> List<AgendaScheduleEntry>,
                             onArtist: (String) -> Unit, onCommunity: () -> Unit, onCreateGroup: () -> Unit, onTickets: () -> Unit) {
    // Never invent days between start and end dates. Only real scheduled sessions get tabs.
    var detailSchedule by remember(ev.id) { mutableStateOf(ev.schedule) }
    var detailLoading by remember(ev.id) { mutableStateOf(ev.isFestival) }
    var detailLoadError by remember(ev.id) { mutableStateOf(false) }
    var scheduleRetry by remember(ev.id) { mutableIntStateOf(0) }
    LaunchedEffect(ev.id, scheduleRetry) {
        if (ev.isFestival) {
            detailLoading = true
            detailLoadError = false
            runCatching { loadSchedule(ev.id) }.onSuccess { fresh ->
                if (fresh.isNotEmpty()) detailSchedule = fresh
            }.onFailure { detailLoadError = true }
        }
        detailLoading = false
    }
    var visibleToday by remember(ev.id) { mutableStateOf(currentCulturalDateIso()) }
    LaunchedEffect(ev.id) {
        while (true) {
            visibleToday = currentCulturalDateIso()
            kotlinx.coroutines.delay(60_000L)
        }
    }
    val festivalDays = remember(ev.id, detailSchedule, visibleToday) {
        IosEventPopupPolicy.publishedDays(detailSchedule, visibleToday)
    }
    val targetSlot = remember(ev.id, matchedArtist, detailSchedule, visibleToday) {
        IosEventPopupPolicy.matchingSlot(detailSchedule, matchedArtist, fromDate = visibleToday)
    }
    var section by remember(ev.id, matchedArtist) { mutableStateOf("Artistes") }
    var selectedDay by remember(ev.id, matchedArtist) {
        mutableStateOf(targetSlot?.date?.take(10)?.takeIf(String::isNotBlank) ?: festivalDays.firstOrNull().orEmpty())
    }
    LaunchedEffect(targetSlot?.date, festivalDays) {
        val focused = targetSlot?.date?.take(10)
        if (!focused.isNullOrBlank() && focused in festivalDays) selectedDay = focused
        else if (selectedDay !in festivalDays) selectedDay = festivalDays.firstOrNull().orEmpty()
    }
    var showPoster by remember(ev.id) { mutableStateOf(false) }
    var pendingDirections by remember(ev.id) { mutableStateOf<String?>(null) }
    val locationChoices = remember(ev.id, detailSchedule) {
        val main = listOf(ev.venue, ev.town).filter(String::isNotBlank).distinct().joinToString(", ")
        (listOf(main) + (if (ev.isFestival) detailSchedule.map { slot ->
            listOf(slot.location, ev.venue, ev.town).filter(String::isNotBlank)
                .distinct().joinToString(", ")
        } else emptyList())).filter(String::isNotBlank).distinct()
    }
    val slotCache = remember(ev.id, userId) { iosScopedCacheKey("festival_schedule_${ev.id}", userId) }
    var savedSlots by remember(slotCache) {
        mutableStateOf(IosFestivalSavedSlotPolicy.decode(platformReadPublicCache(slotCache)))
    }
    var localSaveNote by remember(slotCache) { mutableStateOf("") }
    fun toggleSlot(slot: AgendaScheduleEntry) {
        val key = IosFestivalSavedSlotPolicy.key(ev.id, slot.date, slot.time, slot.artist, slot.title)
        val next = if (key in savedSlots) savedSlots - key else savedSlots + key
        savedSlots = next
        platformWritePublicCache(slotCache, IosFestivalSavedSlotPolicy.encode(next))
        localSaveNote = if (key in next) "Actuació guardada en este iPhone" else "Actuació llevada de l'horari guardat"
    }
    Dialog(onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val desiredHeight = if (ev.isFestival) 720.dp else if (ev.description.length > 250 || ev.artists.size > 2) 640.dp else 555.dp
        Surface(shape = RoundedCornerShape(22.dp), color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE7E7E7)), shadowElevation = 10.dp,
            modifier = Modifier.fillMaxWidth(.96f).height(minOf(desiredHeight, maxHeight * .92f))) {
            Column {
                // Android event popup: image/title/date/place form a SINGLE gradient hero.
                Box(Modifier.fillMaxWidth().heightIn(min = 170.dp)) {
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
                        Text(if (ev.isFestival && ev.endDate.isNotBlank() && ev.endDate.take(10) != ev.date.take(10))
                            "${IosUnfilteredFeedPolicy.fullDate(ev.date)} – ${IosUnfilteredFeedPolicy.fullDate(ev.endDate)}"
                            else IosUnfilteredFeedPolicy.fullDate(ev.date), color = AgendaGold,
                            fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("⌖  ${listOf(ev.venue,ev.town).filter(String::isNotBlank).joinToString(" · ")}",
                            color = Color.White, fontSize = 13.sp, lineHeight = 17.sp)
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
                    Text(if (ev.isFestival && ev.endDate.isNotBlank() && ev.endDate.take(10) != ev.date.take(10))
                        "Consulta les jornades" else "${IosUnfilteredFeedPolicy.fullDate(ev.date)} · ${ev.time}",
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
                val tabs = listOf("Artistes", "Info", "Ubicació", "Comunitat")
                Row(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical=12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tabs.forEach { name ->
                        val icon = when(name) { "Artistes" -> "person"; "Horaris" -> "calendar"
                            "Info" -> "info"; "Ubicació" -> "pin"; else -> "people" }
                        val active = section == name
                        Column(Modifier.weight(1f)
                            .border(1.dp, if(active) AgendaGold else Color(0xFFE4E4E4),RoundedCornerShape(13.dp))
                            .background(if(active) Color(0xFFFFFAEB) else Color.White,RoundedCornerShape(13.dp))
                            .clickable {
                                section = name
                                if (name == "Ubicació" && locationChoices.size == 1) {
                                    pendingDirections = locationChoices.first()
                                }
                            }.padding(vertical=9.dp),
                            horizontalAlignment=Alignment.CenterHorizontally) {
                            AgendaGlyph(icon, Modifier.size(17.dp), AgendaGold)
                            Spacer(Modifier.height(6.dp))
                            Text(name, fontSize=11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                if (ev.isFestival && section == "Artistes") {
                    IosFestivalSchedulePanel(
                        event = ev, schedule = detailSchedule, loading = detailLoading,
                        loadFailed = detailLoadError, onRetry = { scheduleRetry++ },
                        visibleDays = festivalDays, selectedDay = selectedDay,
                        matchedArtist = matchedArtist, savedSlots = savedSlots,
                        onToggleSlot = ::toggleSlot, onSelectDay = { selectedDay = it },
                        onArtist = onArtist, followsEntity = followsEntity,
                        onToggleFollowEntity = onToggleFollowEntity,
                        modifier = Modifier.weight(1f)
                    )
                } else Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                    when (section) {
                        "Artistes" -> {
                            Text("Artistes", fontSize=19.sp, fontWeight=FontWeight.Bold,
                                modifier=Modifier.padding(vertical=10.dp))
                            ev.artists.filter(String::isNotBlank).distinct().forEach { a ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)
                                    .background(Color(0xFFFAFAFA),RoundedCornerShape(12.dp))
                                    .padding(10.dp), verticalAlignment=Alignment.CenterVertically) {
                                    val artistPhoto = IosPersonalizationImages.photo(IosPersonalizationImages.cached(), "ARTIST", a)
                                    Poster(artistPhoto, Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)))
                                    Spacer(Modifier.width(10.dp))
                                    Text(a, Modifier.weight(1f).clickable { onArtist(a) },
                                        fontWeight=FontWeight.SemiBold, fontSize=14.sp,
                                        maxLines=1, overflow=TextOverflow.Ellipsis)
                                    TextButton(onClick={ onToggleFollowEntity("ARTIST",a) }) {
                                        Text(if(followsEntity("ARTIST",a)) "✓ Seguint" else "+ Seguir",
                                            fontSize=11.sp, color=AgendaBlack)
                                    }
                                }
                            }
                        }
                        "Info" -> {
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
                            if (ev.isFestival) TextButton(onClick = { section = "Horaris" }) { Text("Veure l'horari") }
                            TextButton(onClick = onReport) { Text("Informar d'un problema en la fitxa") }
                        }
                        "Horaris" -> {
                            if (festivalDays.size > 1) {
                                Row(Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    festivalDays.forEach { day ->
                                        FilterChip(selected = selectedDay == day, onClick = { selectedDay = day },
                                            label = { Text(day) })
                                    }
                                }
                            }
                            if (detailLoading && detailSchedule.isEmpty()) Text("Consultant l'horari…", fontSize = 12.sp, color = Color.Gray)
                            if (localSaveNote.isNotBlank()) Text(localSaveNote, fontSize = 11.sp, color = Color.Gray)
                            val daySlots = detailSchedule.distinctBy { IosFestivalSavedSlotPolicy.key(ev.id, it.date, it.time, it.artist, it.title) }
                                .filter { festivalDays.size <= 1 || it.date.take(10) == selectedDay }
                                .sortedWith(compareByDescending<AgendaScheduleEntry> { slot ->
                                    matchedArtist.isNotBlank() && (slot.artist.contains(matchedArtist, true) || slot.title.contains(matchedArtist, true))
                                }.thenBy { it.time }.thenBy { it.artist })
                            if (daySlots.isEmpty()) Text("No hi ha actuacions detallades disponibles.", color = Color.Gray, fontSize = 13.sp)
                            daySlots.forEach { slot ->
                                val isMatch = matchedArtist.isNotBlank() && (slot.artist.contains(matchedArtist, true) || slot.title.contains(matchedArtist, true))
                                val key = IosFestivalSavedSlotPolicy.key(ev.id, slot.date, slot.time, slot.artist, slot.title)
                                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    .background(if (isMatch) Accent else Pale, RoundedCornerShape(9.dp)).padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Text(slot.time.ifBlank { "—" }, modifier = Modifier.width(48.dp), fontSize = 12.sp)
                                    Column(Modifier.weight(1f)) {
                                        val who = slot.artist.ifBlank { slot.title }
                                        TextButton(onClick = { if (who.isNotBlank()) onArtist(who) },
                                            contentPadding = PaddingValues(0.dp)) {
                                            Text(who, fontSize = 13.sp, fontWeight = if (isMatch) FontWeight.Bold else FontWeight.Normal)
                                        }
                                        if (slot.location.isNotBlank()) Text(slot.location, fontSize = 11.sp, color = Color.Gray)
                                    }
                                    TextButton(onClick = { toggleSlot(slot) }) {
                                        Text(if (key in savedSlots) "✓" else "+", fontSize = 18.sp)
                                    }
                                }
                            }
                            if (savedSlots.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Text("Horari guardat localment en este dispositiu (no sincronitzat amb altres).", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                        "Entrades" -> {
                            Text(if (ev.ticketAvailable) "Venda d'entrades" else "Sense venda d'entrades", fontWeight = FontWeight.Bold)
                            if (ev.price.isNotBlank()) Text("Preu: ${ev.price} €")
                            Button(onClick = onTickets, enabled = ev.ticketAvailable) { Text("Consultar entrades") }
                        }
                        "Ubicació" -> {
                            Text("Ubicació", fontSize=18.sp, fontWeight=FontWeight.Bold)
                            if (ev.venue.isNotBlank()) {
                                Text(ev.venue, fontWeight = FontWeight.SemiBold)
                                TextButton(onClick = { onToggleFollowEntity("VENUE", ev.venue) }) {
                                    Text(if (followsEntity("VENUE", ev.venue)) "✓ Seguint esta sala" else "+ Seguir esta sala")
                                }
                            }
                            if (locationChoices.isEmpty()) Text("Ubicació pendent de confirmar",fontSize=13.sp,color=Color.Gray)
                            locationChoices.forEach { place ->
                                Surface(shape=RoundedCornerShape(12.dp), color=Color.White,
                                    border=BorderStroke(1.dp, Color(0xFFE7E7E7)),
                                    modifier=Modifier.fillMaxWidth().padding(vertical=4.dp)
                                        .clickable { pendingDirections = place }) {
                                    Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
                                        AgendaGlyph("pin",Modifier.size(20.dp),AgendaGold)
                                        Spacer(Modifier.width(9.dp))
                                        Text(place,Modifier.weight(1f),fontSize=13.sp)
                                        Text("Com arribar",fontSize=11.sp,fontWeight=FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                        "Comunitat" -> {
                            Text("Taulons d'este esdeveniment", fontWeight = FontWeight.Bold)
                            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Button(onClick = onCommunity, modifier = Modifier.fillMaxWidth()) { Text("Obrir Comunitat") }
                                OutlinedButton(onClick = onCreateGroup, modifier = Modifier.fillMaxWidth()) { Text("Crear tauló") }
                            }
                        }
                    }
                }
            }
        }
        }
    }
    pendingDirections?.let { place ->
        AlertDialog(onDismissRequest = { pendingDirections = null },
            title = { Text("Obrir indicacions externes?") },
            text = { Text("S'obrirà Mapes fora de l'Agenda per a anar a $place.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDirections = null
                    platformOpenExternal("https://maps.apple.com/?q=${place.encodeURLParameter()}")
                }) { Text("Obrir Mapes") }
            },
            dismissButton = { TextButton(onClick = { pendingDirections = null }) { Text("Cancel·lar") } }
        )
    }
    if (showPoster) Dialog(onDismissRequest = { showPoster = false }) {
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.85f).clickable { showPoster = false }) {
            Poster(ev.posterUrl, Modifier.fillMaxSize())
        }
    }
}

/** Festival schedule is the Artistes tab in Android. We never fabricate a slot's day/time. */
@Composable
private fun IosFestivalSchedulePanel(
    event: AgendaEventSummary,
    schedule: List<AgendaScheduleEntry>,
    loading: Boolean,
    loadFailed: Boolean,
    onRetry: () -> Unit,
    visibleDays: List<String>,
    selectedDay: String,
    matchedArtist: String,
    savedSlots: Set<String>,
    onToggleSlot: (AgendaScheduleEntry) -> Unit,
    onSelectDay: (String) -> Unit,
    onArtist: (String) -> Unit,
    followsEntity: (String, String) -> Boolean,
    onToggleFollowEntity: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var onlyMine by remember(event.id, selectedDay) { mutableStateOf(false) }
    val allRows = remember(event.id, schedule, selectedDay, visibleDays) {
        schedule.distinctBy { IosFestivalSavedSlotPolicy.key(event.id,it.date,it.time,it.artist,it.title) }
            .filter { it.date.take(10) in visibleDays && (selectedDay.isBlank() || it.date.take(10) == selectedDay) }
            .sortedWith(compareBy<AgendaScheduleEntry> { it.time.ifBlank { "99:99" } }
                .thenBy { it.artist.ifBlank { it.title } })
    }
    val rows = if (onlyMine) allRows.filter { row ->
        IosFestivalSavedSlotPolicy.key(event.id,row.date,row.time,row.artist,row.title) in savedSlots
    } else allRows
    val matchedKey = IosEventPopupPolicy.artistKey(matchedArtist)
    val focusedIndex = if (onlyMine || matchedKey.isBlank()) -1 else rows.indexOfFirst { row ->
        IosEventPopupPolicy.artistKey(row.artist.ifBlank { row.title }) == matchedKey
    }
    val listState = rememberLazyListState()
    LaunchedEffect(event.id, selectedDay, matchedKey, focusedIndex, onlyMine) {
        if (focusedIndex >= 0) listState.scrollToItem(focusedIndex + 2)
        else listState.scrollToItem(0)
    }
    LazyColumn(modifier = modifier.fillMaxWidth(), state = listState,
        contentPadding = PaddingValues(start = 15.dp, end = 15.dp, top = 10.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)) {
        item(key = "jornades") {
            if (visibleDays.size > 1) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)) {
                    items(visibleDays, key = { it }) { day ->
                        FilterChip(selected = selectedDay == day, onClick = { onSelectDay(day) },
                            label = { Text(IosUnfilteredFeedPolicy.fullDate(day),fontSize=12.sp) })
                    }
                }
            } else if (visibleDays.size == 1) {
                Text(IosUnfilteredFeedPolicy.fullDate(visibleDays.first()),
                    fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        }
        item(key = "controls") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Programació", modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Surface(shape = RoundedCornerShape(50), color=Color.White,
                    border=BorderStroke(1.dp,Color(0xFFE7E7E7))) {
                    Row(Modifier.padding(3.dp), horizontalArrangement=Arrangement.spacedBy(3.dp)) {
                        listOf(false to "Tots",true to "Els meus").forEach { (mine,label) ->
                            Surface(shape=RoundedCornerShape(30.dp),
                                color=if (mine==onlyMine) Color(0xFFFFF7E6) else Color.White,
                                modifier=Modifier.clickable { onlyMine=mine }) {
                                Text(label,Modifier.padding(horizontal=9.dp,vertical=6.dp),
                                    fontSize=11.sp,fontWeight=if (mine==onlyMine) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }
        }
        if (rows.isEmpty()) {
            item(key = "sense_programacio") {
                val hasArtist = IosEventPopupPolicy.isArtistInFestival(event, matchedArtist)
                Text(when {
                    onlyMine -> "Encara no has guardat cap actuació d'esta jornada."
                    loading -> "Carregant la programació…"
                    loadFailed -> "No s'ha pogut carregar la programació. Torna-ho a intentar."
                    visibleDays.isEmpty() && schedule.isNotEmpty() -> "Totes les jornades publicades ja han passat."
                    hasArtist -> "$matchedArtist figura al cartell, però encara no hi ha dia ni hora confirmats."
                    else -> "Programació pendent de publicar per a esta jornada."
                }, modifier=Modifier.padding(vertical=12.dp),fontSize=13.sp,color=Color.Gray)
                if (loadFailed) TextButton(onClick=onRetry) { Text("Tornar a intentar") }
            }
            if (!loading && !onlyMine && visibleDays.isEmpty() && schedule.isEmpty()) {
                val artists = event.artists.distinctBy(IosEventPopupPolicy::artistKey)
                items(artists, key = { "lineup_" + IosEventPopupPolicy.artistKey(it) }) { name ->
                    val artistPhoto = IosPersonalizationImages.photo(IosPersonalizationImages.cached(),"ARTIST",name)
                    Row(Modifier.fillMaxWidth().background(Color(0xFFFAFAFA),RoundedCornerShape(13.dp))
                        .padding(11.dp),verticalAlignment=Alignment.CenterVertically) {
                        Poster(artistPhoto,Modifier.size(54.dp).clip(RoundedCornerShape(10.dp)))
                        Spacer(Modifier.width(9.dp))
                        Text(name,Modifier.weight(1f).clickable { onArtist(name) },fontWeight=FontWeight.Bold,fontSize=14.sp)
                        TextButton(onClick={onToggleFollowEntity("ARTIST",name)}) {
                            Text(if(followsEntity("ARTIST",name)) "✓ Seguint" else "+ Seguir",fontSize=11.sp)
                        }
                    }
                }
            }
        }
        itemsIndexed(rows, key = { _,row -> IosFestivalSavedSlotPolicy.key(event.id,row.date,row.time,row.artist,row.title) }) { _,slot ->
            val name = slot.artist.ifBlank { slot.title }
            val knownArtist = slot.artist.isNotBlank() || event.artists.any {
                IosEventPopupPolicy.artistKey(it) == IosEventPopupPolicy.artistKey(name)
            }
            val match = matchedKey.isNotBlank() && IosEventPopupPolicy.artistKey(name)==matchedKey
            val key = IosFestivalSavedSlotPolicy.key(event.id,slot.date,slot.time,slot.artist,slot.title)
            val isSaved = key in savedSlots
            Surface(color=Color.White,shape=RoundedCornerShape(13.dp),
                border=BorderStroke(if(match) 2.dp else 1.dp,
                    if(match || isSaved) AgendaGold else Color(0xFFE7E7E7))) {
                Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    val artistPhoto=IosPersonalizationImages.photo(IosPersonalizationImages.cached(),"ARTIST",name)
                    Poster(artistPhoto,Modifier.size(54.dp).clip(RoundedCornerShape(10.dp)))
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                        if (slot.time.isNotBlank()) Text("${slot.time} h",color=AgendaGold,
                            fontWeight=FontWeight.Bold,fontSize=12.sp)
                        Text(name,Modifier.then(if (knownArtist) Modifier.clickable { onArtist(name) } else Modifier),fontSize=14.sp,
                            fontWeight=if(match) FontWeight.ExtraBold else FontWeight.SemiBold)
                        if (slot.title.isNotBlank() && IosEventPopupPolicy.artistKey(slot.title) != IosEventPopupPolicy.artistKey(name))
                            Text(slot.title,fontSize=12.sp)
                        if (slot.location.isNotBlank()) Text(slot.location,fontSize=11.sp,color=Color.Gray)
                    }
                    Column(horizontalAlignment=Alignment.End) {
                        if (knownArtist) TextButton(onClick={onToggleFollowEntity("ARTIST",name)}) {
                            Text(if(followsEntity("ARTIST",name)) "✓ Seguint" else "+ Seguir",fontSize=10.sp)
                        }
                        TextButton(onClick={onToggleSlot(slot)}) {
                            Text(if(isSaved) "✓ Guardat" else "+ Guardar",fontSize=10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistDialog(name: String, followed: Boolean, concerts: List<AgendaEventSummary>,
                         onFollow: () -> Unit, bell: IosArtistBell?, onBell: () -> Unit,
                         onDismiss: () -> Unit, onEvent: (AgendaEventSummary) -> Unit) {
    // Artist search platforms mirror Android, but these are SEARCH links, not claimed official profiles.
    val firstEvent = concerts.firstOrNull()
    val discipline = concerts.map { it.category }.filter(String::isNotBlank)
        .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key.orEmpty()
    val platforms = if (discipline.contains("musi", true)) {
        val query = name.encodeURLParameter()
        listOf(
            "Spotify" to "https://open.spotify.com/search/$query",
            "YouTube Music" to "https://music.youtube.com/search?q=$query",
            "Apple Music" to "https://music.apple.com/es/search?term=$query",
            "Deezer" to "https://www.deezer.com/search/$query",
            "TIDAL" to "https://listen.tidal.com/search?q=$query",
            "Bandcamp" to "https://bandcamp.com/search?q=$query"
        )
    } else emptyList()
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White,
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.90f)) {
            Column {
                Box(Modifier.fillMaxWidth().height(170.dp)) {
                    // Until we retrieve an artist-owned photo, don't mislabel an event poster as a profile photograph.
                    Poster(firstEvent?.posterUrl.orEmpty(), Modifier.fillMaxSize())
                    if (discipline.isNotBlank()) {
                        Text(discipline, color = Ink, fontSize = 11.sp,
                            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
                                .background(Color.White.copy(alpha = 0.88f), RoundedCornerShape(8.dp)).padding(7.dp))
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = onFollow) { Text(if (followed) "✓ Seguint" else "+ Seguir") }
                    OutlinedButton(onClick = onBell) {
                        Text(if (bell != null) "🔔 ${IosArtistBellPolicy.label(bell.frequency)}" else "🔔 Avisos")
                    }
                    TextButton(onClick = onDismiss) { Text("×") }
                }
                Text(name, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                if (platforms.isNotEmpty()) {
                    Text("Buscar música", fontSize = 11.sp, color = Color.Gray,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp))
                    LazyRow(contentPadding = PaddingValues(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        items(platforms) { (label, url) ->
                            OutlinedButton(onClick = { platformOpenExternal(url) },
                                contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp)) {
                                Text(label, fontSize = 10.sp, maxLines = 1)
                            }
                        }
                    }
                }
                LazyColumn(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { Text(if (platforms.isNotEmpty()) "Concerts" else "Activitats",
                        fontWeight = FontWeight.Bold, fontSize = 17.sp) }
                    if (concerts.isEmpty()) item { Text("No hi ha activitats futures en el catàleg.", color = Color.Gray) }
                    items(concerts.distinctBy { it.key }, key = { it.key }) { ev ->
                        EventCard(ev, isSaved = false, onClick = { onEvent(ev) }, trailing = {})
                    }
                    // Products require a real artist-owned merchandise endpoint; no invented items or payments.
                    item {
                        Spacer(Modifier.height(7.dp))
                        Text("Productes", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("Catàleg de productes encara no disponible a iOS.",
                            color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        }
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

/** One artist per visual Batec, with stories and current concerts inside its viewer. */
@Composable
private fun BatecViewer(
    groups: List<IosBatecArtistGroup>,
    selectedKey: String,
    stories: List<IosBatec>,
    allEvents: List<AgendaEventSummary>,
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
        if (delta < 0 && selectedIndex == 0) {
            replayEpoch += 1 // Android restarts the first Batec instead of closing.
        } else {
            val next = IosBatecNavigationPolicy.move(selectedIndex, groups.size, delta)
            if (next == null) onDismiss() else onGroupChanged(groups[next])
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
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.89f)
        ) {
            var horizontalDrag by remember(selectedKey) { mutableFloatStateOf(0f) }
            Box(
                Modifier.fillMaxSize()
                    .pointerInput(selectedKey, groups.size) {
                        detectTapGestures { p ->
                            if (p.x < size.width * 0.24f) go(-1)
                            else if (p.x > size.width * 0.76f) go(1)
                        }
                    }
                    .pointerInput(selectedKey, groups.size) {
                        detectHorizontalDragGestures(
                            onDragStart = { horizontalDrag = 0f },
                            onHorizontalDrag = { change, delta -> horizontalDrag += delta; change.consume() },
                            onDragEnd = {
                                when {
                                    horizontalDrag < -56.dp.toPx() -> go(1)
                                    horizontalDrag > 56.dp.toPx() -> go(-1)
                                }
                                horizontalDrag = 0f
                            },
                            onDragCancel = { horizontalDrag = 0f }
                        )
                    }
            ) {
                Poster(
                    representative.imageUrl.ifBlank { upcoming.firstOrNull()?.posterUrl.orEmpty() },
                    Modifier.fillMaxSize()
                )
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
                    Color.Black.copy(alpha = .60f), Color.Black.copy(alpha = .83f),
                    Color.Black.copy(alpha = .92f)
                ))))
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
                            Text("${group.storyKeys.size} novetat${if (group.storyKeys.size == 1) "" else "s"} · ${selectedIndex + 1}/${groups.size}",
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
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { go(-1) }) { Text("‹ Anterior", color = Color.White) }
                        TextButton(onClick = { go(1) }) {
                            Text(if (selectedIndex == groups.lastIndex) "Tancar" else "Següent ›", color = Color.White)
                        }
                    }
                }
                // Large hit areas stay outside the scrollable concert content.
                Box(Modifier.align(Alignment.CenterStart).width(35.dp).height(88.dp)
                    .clickable { go(-1) }, contentAlignment = Alignment.Center) {
                    Text("‹", color = Color.White, fontSize = 36.sp)
                }
                Box(Modifier.align(Alignment.CenterEnd).width(35.dp).height(88.dp)
                    .clickable { go(1) }, contentAlignment = Alignment.Center) {
                    Text("›", color = Color.White, fontSize = 36.sp)
                }
            }
        }
    }
}

/** Six Android notification tabs, full-height sheet, internal settings and active reminders.
 * Server messages and locally derived alerts remain separately identifiable and read-safe. */
@Composable
private fun NotificationsDialog(
    notifications: List<IosNotification>, artistBells: List<IosArtistBell>,
    reminders: List<AgendaEventSummary>, tickets: List<IosTicket>,
    suggestions: List<AgendaEventSummary>, eventsById: Map<Int, AgendaEventSummary>,
    today: String, accountId: Int?, api: IosAgendaApi,
    preferences: IosNotificationPreferences, onPreferencesChange: (IosNotificationPreferences) -> Unit,
    readLocalIds: Set<String>, dismissedIds: Set<String>,
    onReadLocal: (String) -> Unit, onDismissAll: (List<String>) -> Unit,
    onDismiss: () -> Unit, onRead: (IosNotification) -> Unit,
    onReminderClick: (AgendaEventSummary) -> Unit,
    onReminderDisable: (AgendaEventSummary) -> Unit, onArtistBell: (String) -> Unit,
    onOpenArtist: (String) -> Unit, onArtistDisable: (String) -> Unit,
    onOpenTickets: () -> Unit, onOpenSuggestion: (AgendaEventSummary) -> Unit,
    reminderTime: (AgendaEventSummary) -> String,
    entries: List<IosNotificationEntry>
) {
    var selectedTab by remember { mutableStateOf(IosNotificationGrouping.GENERAL) }
    var page by remember { mutableStateOf("Notificacions") }
    var draft by remember(preferences,page) { mutableStateOf(preferences) }
    var emailDraft by remember { mutableStateOf(IosEmailPreferences()) }
    var emailLoaded by remember(accountId) { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var settingsMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val tabs = listOf(
        Triple("General","bell",""), Triple("Pròxim","clock",""), Triple("Nous","info",""),
        Triple("Entrades","ticket",""), Triple("Suggerències","star",""), Triple("Comunitat","people","")
    )
    val currentEntries = entries.filter { it.key !in dismissedIds }
    fun isUnread(entry: IosNotificationEntry) = entry.key !in readLocalIds &&
        (entry.remoteId == null || notifications.firstOrNull { it.id == entry.remoteId }?.read == false)
    val unread = currentEntries.count(::isUnread)
    LaunchedEffect(page, accountId) {
        if (page == "Configuració" && accountId != null && !emailLoaded) {
            runCatching { api.emailPreferences() }
                .onSuccess { emailDraft = it; emailLoaded = true }
                .onFailure { settingsMessage = "No s'han pogut consultar els correus: ${it.message.orEmpty()}" }
        }
    }
    Dialog(onDismissRequest = {
        if (page == "Notificacions") onDismiss() else page = "Notificacions"
    }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = Color.White, shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
            modifier = Modifier.fillMaxSize().padding(top = 5.dp)) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(start=18.dp,end=8.dp,top=12.dp,bottom=6.dp),
                    verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(page, fontSize=23.sp,fontWeight=FontWeight.ExtraBold)
                        if(page == "Notificacions" && unread > 0) Text("$unread sense llegir",
                            color=Color.Gray, fontSize=11.sp)
                    }
                    if(page == "Notificacions") {
                        AgendaTopAction("clock",onClick={page="Recordatoris actius"},badge=reminders.size+artistBells.size)
                        AgendaTopAction("sliders",onClick={page="Configuració";settingsMessage=""})
                    }
                    AgendaTopAction("close",onClick={ if(page == "Notificacions") onDismiss() else page="Notificacions" })
                }
                when(page) {
                    "Recordatoris actius" -> {
                        LazyColumn(Modifier.weight(1f).fillMaxWidth(),
                            contentPadding=PaddingValues(horizontal=16.dp,vertical=10.dp),
                            verticalArrangement=Arrangement.spacedBy(10.dp)) {
                            item { Text("Des d'ací pots consultar, obrir o desactivar els avisos que tens configurats.",
                                fontSize=13.sp,color=Color.Gray) }
                            if(reminders.isNotEmpty()) item {
                                Text("Esdeveniments",fontWeight=FontWeight.Bold,fontSize=17.sp)
                            }
                            items(reminders,key={"rem_${it.id}"}) { event ->
                                Row(Modifier.fillMaxWidth().border(1.dp,Color(0xFFEAEAEA),RoundedCornerShape(13.dp))
                                    .padding(10.dp),verticalAlignment=Alignment.CenterVertically) {
                                    Poster(event.posterUrl,Modifier.size(width=62.dp,height=76.dp).clip(RoundedCornerShape(9.dp)))
                                    Column(Modifier.weight(1f).clickable{onReminderClick(event)}.padding(horizontal=10.dp)) {
                                        Text(event.title,fontWeight=FontWeight.Bold,fontSize=14.sp,maxLines=2)
                                        Text("${event.date} · ${event.town}",fontSize=11.sp,color=Color.Gray)
                                        val scheduled=reminderTime(event)
                                        Text(if(scheduled.isNotBlank()) scheduled.replace('T',' ') else "Campaneta de l’app · Sense hora personalitzada",
                                            fontSize=11.sp,color=Color.Gray)
                                    }
                                    AgendaTopAction("bell",onClick={onReminderDisable(event)})
                                }
                            }
                            if(artistBells.isNotEmpty()) item {
                                Text("Artistes i grups",fontWeight=FontWeight.Bold,fontSize=17.sp,
                                    modifier=Modifier.padding(top=12.dp))
                            }
                            items(artistBells,key={"artist_"+IosArtistBellPolicy.key(it.artistName)}) { bell ->
                                Row(Modifier.fillMaxWidth().border(1.dp,Color(0xFFEAEAEA),RoundedCornerShape(13.dp))
                                    .padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f).clickable{onOpenArtist(bell.artistName)}) {
                                        Text(bell.artistName,fontWeight=FontWeight.Bold,fontSize=14.sp)
                                        Text("Campaneta activa · ${IosArtistBellPolicy.label(bell.frequency)}",color=Color.Gray,fontSize=11.sp)
                                    }
                                    TextButton(onClick={onArtistBell(bell.artistName)}) { Text("Editar",fontSize=11.sp) }
                                    TextButton(onClick={onArtistDisable(bell.artistName)}) { Text("Desactivar",fontSize=11.sp) }
                                }
                            }
                            if(reminders.isEmpty() && artistBells.isEmpty()) item {
                                EmptyState("No tens recordatoris actius.")
                            }
                        }
                    }
                    "Configuració" -> {
                        LazyColumn(Modifier.weight(1f).fillMaxWidth(),
                            contentPadding=PaddingValues(horizontal=18.dp,vertical=14.dp),
                            verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            item {
                                Text("Campaneta de l’app",fontSize=18.sp,fontWeight=FontWeight.ExtraBold)
                                IosNotificationToggle("Novetats dels artistes amb campaneta activada",draft.followedArtist) {
                                    draft=draft.copy(followedArtist=it)
                                }
                                if(draft.followedArtist) Text("La freqüència es configura en cada artista o grup.",fontSize=12.sp,color=Color.Gray)
                                IosNotificationToggle("Esdeveniments guardats pròxims",draft.upcoming) {
                                    draft=draft.copy(upcoming=it)
                                }
                                IosNotificationToggle("Compres d’entrades",draft.tickets) {draft=draft.copy(tickets=it)}
                                IosNotificationToggle("Comunitat: missatges i propostes",draft.community) {draft=draft.copy(community=it)}
                                IosNotificationToggle("Suggerències culturals",draft.suggestions) {draft=draft.copy(suggestions=it)}
                                Spacer(Modifier.height(18.dp))
                                Text("Correus electrònics",fontSize=19.sp,fontWeight=FontWeight.ExtraBold)
                                if(accountId == null) Text("Inicia sessió per configurar els correus.",fontSize=12.sp,color=Color.Gray)
                                else {
                                    Text("Periodicitat",fontSize=13.sp,fontWeight=FontWeight.Bold,
                                        modifier=Modifier.padding(top=10.dp))
                                    Row(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                                        listOf("NONE" to "Desactivat","DAILY" to "Diari","WEEKLY" to "Setmanal").forEach { (wire,label) ->
                                            FilterChip(selected=emailDraft.frequency==wire,onClick={emailDraft=emailDraft.copy(frequency=wire)},
                                                label={Text(label,fontSize=12.sp)})
                                        }
                                    }
                                    Text("Què vols rebre",fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(top=8.dp))
                                    IosNotificationToggle("Suggeriments segons els teus gustos",emailDraft.recommendations) {
                                        emailDraft=emailDraft.copy(recommendations=it)
                                    }
                                    IosNotificationToggle("Esdeveniments dels artistes que seguixes",emailDraft.followedArtists) {
                                        emailDraft=emailDraft.copy(followedArtists=it)
                                    }
                                    IosNotificationToggle("Guardats i recordatoris",emailDraft.savedReminders) {
                                        emailDraft=emailDraft.copy(savedReminders=it)
                                    }
                                    IosNotificationToggle("Publicitat personalitzada",emailDraft.personalizedAds) {
                                        emailDraft=emailDraft.copy(personalizedAds=it)
                                    }
                                }
                                Spacer(Modifier.height(18.dp))
                                Text("Notificacions iOS",fontSize=19.sp,fontWeight=FontWeight.ExtraBold)
                                Text("Els recordatoris locals i els avisos push necessiten autorització d’iOS. " +
                                    "Les campanetes d’artistes només enviaran push quan el servidor APNs estiga connectat.",
                                    fontSize=12.sp,color=Color.Gray)
                                OutlinedButton(onClick={IosNativeBridge.current?.showNotificationSettings()}) {
                                    Text("Obrir els ajustos de notificacions d’iOS")
                                }
                                Text("Les hores de descans personalitzades d’Android encara no estan integrades en el planificador iOS.",
                                    color=Color.Gray,fontSize=12.sp)
                                if(settingsMessage.isNotBlank()) Text(settingsMessage,fontSize=12.sp,color=Color(0xFF9D2525),
                                    modifier=Modifier.padding(top=8.dp))
                                Button(onClick={
                                    onPreferencesChange(draft)
                                    if(accountId != null && emailLoaded) {
                                        saving=true; settingsMessage=""
                                        scope.launch {
                                            runCatching { api.updateEmailPreferences(emailDraft) }
                                                .onSuccess { emailDraft=it; settingsMessage="Configuració guardada.";page="Notificacions" }
                                                .onFailure { settingsMessage="Preferències de l’app guardades, però no s’han pogut guardar els correus: ${it.message.orEmpty()}" }
                                            saving=false
                                        }
                                    } else if (accountId != null) {
                                        settingsMessage="Preferències de l’app guardades. No s’ha pogut carregar la configuració de correu."
                                    } else {
                                        settingsMessage="Preferències locals guardades.";page="Notificacions"
                                    }
                                },enabled=!saving,modifier=Modifier.fillMaxWidth().padding(top=18.dp)) {
                                    Text(if(saving) "Guardant…" else "Guardar configuració")
                                }
                            }
                        }
                    }
                    else -> {
                        Row(Modifier.fillMaxWidth().padding(horizontal=5.dp)) {
                            tabs.forEach { (label,iconName,_) ->
                                val active = selectedTab==label
                                val count = if(label=="Suggerències")
                                    currentEntries.count { it.section==label && isUnread(it) } +
                                        suggestions.count { "suggest_${it.id}" !in readLocalIds }
                                    else currentEntries.count { it.section==label && isUnread(it) }
                                Column(Modifier.weight(1f).clickable{selectedTab=label}.padding(vertical=9.dp),
                                    horizontalAlignment=Alignment.CenterHorizontally) {
                                    Box(contentAlignment=Alignment.TopEnd) {
                                        AgendaGlyph(iconName,Modifier.size(22.dp),if(active) AgendaBlack else Color(0xFF555555))
                                        if(count>0) Box(Modifier.offset(x=8.dp,y=(-5).dp).size(15.dp)
                                            .background(AgendaBlack,CircleShape),contentAlignment=Alignment.Center) {
                                            Text(count.coerceAtMost(9).toString(),fontSize=8.sp,color=Color.White)
                                        }
                                    }
                                    Spacer(Modifier.height(9.dp))
                                    Box(Modifier.fillMaxWidth().height(2.dp)
                                        .background(if(active) AgendaBlack else Color.Transparent))
                                }
                            }
                        }
                        Text(selectedTab,modifier=Modifier.fillMaxWidth().padding(top=5.dp,bottom=4.dp),
                            textAlign=androidx.compose.ui.text.style.TextAlign.Center,
                            fontSize=12.sp,fontWeight=FontWeight.SemiBold)
                        HorizontalDivider(color=Color(0xFFEAEAEA))
                        val visible = if(selectedTab=="General") currentEntries.filter { isUnread(it) }
                            else currentEntries.filter { it.section==selectedTab }
                        LazyColumn(Modifier.weight(1f).fillMaxWidth(),
                            contentPadding=PaddingValues(horizontal=14.dp,vertical=10.dp),
                            verticalArrangement=Arrangement.spacedBy(9.dp)) {
                            items(visible,key={it.key}) { notice ->
                                val original=notice.remoteId?.let { id->notifications.firstOrNull { it.id==id } }
                                val event=notice.eventId?.let(eventsById::get)
                                Row(Modifier.fillMaxWidth()
                                    .background(if(isUnread(notice)) Color(0xFFF1F1F1) else Color(0xFFF9F9F9),RoundedCornerShape(13.dp))
                                    .clickable {
                                        onReadLocal(notice.key)
                                        if(original!=null) onRead(original)
                                        else if(event!=null) onReminderClick(event)
                                        else if(notice.artistName.isNotBlank()) onArtistBell(notice.artistName)
                                    }.padding(10.dp),verticalAlignment=Alignment.CenterVertically) {
                                    if(event!=null) Poster(event.posterUrl,Modifier.width(65.dp).height(79.dp)
                                        .clip(RoundedCornerShape(9.dp)))
                                    else Box(Modifier.size(65.dp).background(Color.White,RoundedCornerShape(9.dp)),
                                        contentAlignment=Alignment.Center) { AgendaGlyph("bell",Modifier.size(24.dp)) }
                                    Spacer(Modifier.width(11.dp))
                                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                                        Text(notice.title,fontSize=14.sp,fontWeight=FontWeight.SemiBold,maxLines=2,overflow=TextOverflow.Ellipsis)
                                        Text(notice.body,fontSize=12.sp,maxLines=2,overflow=TextOverflow.Ellipsis,color=Color.DarkGray)
                                    }
                                    if(isUnread(notice)) Box(Modifier.size(7.dp).background(AgendaBlack,CircleShape))
                                }
                            }
                            if(selectedTab=="Entrades") {
                                val known = visible.mapNotNull { it.eventId }.toSet()
                                items(tickets.filter { it.eventId.toIntOrNull() !in known }.distinctBy{it.eventId},
                                    key={"ticket_"+it.id}) { ticket ->
                                    Row(Modifier.fillMaxWidth().border(1.dp,Color(0xFFE8E8E8),RoundedCornerShape(12.dp))
                                        .clickable{onOpenTickets()}.padding(14.dp),verticalAlignment=Alignment.CenterVertically) {
                                        AgendaGlyph("ticket",Modifier.size(24.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(ticket.eventTitle,fontWeight=FontWeight.SemiBold)
                                            Text("Entrada comprada · Veure les meues entrades",fontSize=11.sp,color=Color.Gray)
                                        }
                                    }
                                }
                            }
                            if(selectedTab=="Suggerències" || selectedTab=="General") {
                                val existing=visible.mapNotNull { it.eventId }.toSet()
                                val eligible = suggestions.filterNot { it.id in existing }.filter { event ->
                                    selectedTab!="General" || "suggest_${event.id}" !in readLocalIds
                                }
                                items(eligible,key={"suggest_${it.id}"}) { event ->
                                    Row(Modifier.fillMaxWidth().border(1.dp,Color(0xFFE8E8E8),RoundedCornerShape(12.dp))
                                        .clickable{onReadLocal("suggest_${event.id}");onOpenSuggestion(event)}
                                        .padding(10.dp),verticalAlignment=Alignment.CenterVertically) {
                                        Poster(event.posterUrl,Modifier.width(65.dp).height(79.dp).clip(RoundedCornerShape(8.dp)))
                                        Column(Modifier.weight(1f).padding(start=12.dp)) {
                                            Text(event.title,maxLines=2,fontSize=14.sp,fontWeight=FontWeight.Bold)
                                            Text("${event.date} · ${event.town}",fontSize=11.sp,color=Color.Gray)
                                        }
                                    }
                                }
                            }
                            if(visible.isEmpty() && (selectedTab!="Entrades" || tickets.isEmpty()) &&
                                (selectedTab!="Suggerències" || suggestions.isEmpty()) &&
                                (selectedTab!="General" || suggestions.none { "suggest_${it.id}" !in readLocalIds })) item {
                                Box(Modifier.fillMaxWidth().padding(top=35.dp),contentAlignment=Alignment.Center) {
                                    Text(when(selectedTab) {
                                        "General" -> "No tens notificacions noves."
                                        "Pròxim" -> "No tens esdeveniments guardats pròxims."
                                        "Nous" -> "No hi ha novetats d’artistes amb campaneta activa."
                                        "Entrades" -> "No tens entrades noves esta setmana."
                                        "Comunitat" -> "No hi ha novetats de Comunitat."
                                        else -> "No hi ha cap suggerència destacada ara."
                                    },fontSize=13.sp,color=Color.Gray)
                                }
                            }
                        }
                        Row(Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=8.dp),
                            horizontalArrangement=Arrangement.End) {
                            OutlinedButton(onClick={onDismissAll(currentEntries.map {it.key} +
                                suggestions.map { "suggest_${it.id}" })},
                                enabled=currentEntries.isNotEmpty() || suggestions.any { "suggest_${it.id}" !in readLocalIds }) {
                                Text("Netejar notificacions",fontSize=12.sp,fontWeight=FontWeight.Bold,
                                    color=Color(0xFF9D2525))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IosNotificationToggle(title: String, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical=3.dp),verticalAlignment=Alignment.CenterVertically) {
        Text(title,Modifier.weight(1f),fontSize=13.sp)
        Switch(checked=enabled,onCheckedChange=onChange)
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
                    runCatching { api.joinGroup(group.id) }.onSuccess {
                        membership = if (group.access == "OPEN") "ACCEPTED" else "PENDING"
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
                                authenticated: Boolean, onLoginRequired: () -> Unit, onDismiss: () -> Unit,
                                onPurchased: () -> Unit, onError: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var options by remember(event.id) { mutableStateOf(emptyList<IosTicketOption>()) }
    var loaded by remember(event.id) { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var quantity by remember { mutableIntStateOf(1) }
    var selectedOption by remember { mutableStateOf<IosTicketOption?>(null) }
    var serverError by remember { mutableStateOf("") }
    LaunchedEffect(event.id) {
        try { options = api.ticketOptions(event.id) }
        catch (ex: Exception) { serverError = ex.message.orEmpty() }
        finally { loaded = true }
    }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Entrades · ${event.title}") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            if (!loaded) Text("Consultant opcions…")
            if (serverError.isNotBlank()) Text(serverError, color = Color.Red, fontSize = 12.sp)
            if (loaded && options.isEmpty() && serverError.isBlank()) Text("Entrada general · ${event.price} €")
            options.forEach { option ->
                Row(Modifier.fillMaxWidth().clickable { selectedOption = option }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedOption?.id == option.id, onClick = { selectedOption = option })
                    Column { Text(option.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("${option.price} €${option.remaining?.let { " · $it disponibles" }.orEmpty()}", fontSize = 12.sp) }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Quantitat: $quantity", modifier = Modifier.weight(1f))
                TextButton(enabled = quantity > 1, onClick = { quantity-- }) { Text("−") }
                TextButton(enabled = quantity < (selectedOption?.maxPerOrder ?: 6).coerceAtMost(6),
                    onClick = { quantity++ }) { Text("+") }
            }
            val price = selectedOption?.price ?: event.price.toDoubleOrNull()
            if (price != null && price > 0.0) Text("El cobrament amb Stripe/Redsys encara no està disponible en esta versió d'iPhone. No es crearà cap comanda de pagament.",
                fontSize = 12.sp, color = Color.Gray)
        } },
        confirmButton = {
            val price = selectedOption?.price ?: event.price.toDoubleOrNull()
            TextButton(enabled = loaded && !busy && serverError.isBlank() && (options.isEmpty() || selectedOption != null) && price == 0.0 &&
                (selectedOption?.remaining == null || selectedOption!!.remaining!! >= quantity),
                onClick = {
                    if (!authenticated) { onLoginRequired(); return@TextButton }
                    busy = true
                    scope.launch {
                        try { api.reserveFreeTicket(event.id, selectedOption?.id ?: 0, quantity); onPurchased() }
                        catch (ex: Exception) { onError(ex.message ?: "No s'ha pogut emetre l'entrada") }
                        finally { busy = false }
                    }
                }) { Text(if (!authenticated) "Iniciar sessió per reservar" else if (busy) "Emetent…" else "Reservar gratuïtament") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Tancar") } })
}

@Composable
private fun CreateGroupDialog(event: AgendaEventSummary, api: IosAgendaApi, onDismiss: () -> Unit,
                              onCreated: () -> Unit, onError: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("OTHER") }
    var open by remember { mutableStateOf(true) }
    var description by remember { mutableStateOf("") }
    var origin by remember { mutableStateOf("") }
    var meetingAt by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
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
        } },
        confirmButton = { TextButton(enabled = !busy && title.isNotBlank(), onClick = { busy = true; scope.launch {
            try { api.createCommunityGroup(event.id, title, kind, open, description, origin, meetingAt, capacity.toIntOrNull() ?: 0); onCreated() }
            catch (ex: Exception) { onError(ex.message ?: "Error creant el tauló") }
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
        confirmButton = { TextButton(enabled = eventId.toIntOrNull() != null && code.isNotBlank() && !working,
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
private fun ProfileEditIosDialog(current: IosMyProfile, onDismiss: () -> Unit,
                                 onPickImage: () -> Unit, onSave: (String, String, Boolean) -> Unit) {
    var name by remember(current.displayName) { mutableStateOf(current.displayName) }
    var bio by remember(current.bio) { mutableStateOf(current.bio) }
    var remove by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Editar perfil") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            if (current.photoUrl.isNotBlank() && !remove) {
                AsyncImage(model = current.photoUrl, contentDescription = "Foto actual", modifier = Modifier.size(90.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            }
            OutlinedButton(onClick = onPickImage) { Text("Triar fotografia de Fotos") }
            if (current.photoUrl.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = remove, onCheckedChange = { remove = it })
                    Text("Llevar la fotografia actual")
                }
            }
            OutlinedTextField(name, { name = it.take(190) }, label = { Text("Nom i cognoms") }, singleLine = true)
            OutlinedTextField(bio, { bio = it.take(3000) }, label = { Text("Biografia") }, minLines = 2, maxLines = 4)
            Text("La fotografia es guarda al servidor, igual que en Android.", fontSize = 11.sp, color = Color.Gray)
        } },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onSave(name, bio, remove) }) { Text("Guardar") } },
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
