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
    var feedMode by remember { mutableStateOf("Per a tu") }
    var query by remember { mutableStateOf("") }
    var appliedQuery by remember { mutableStateOf("") }
    // Query is committed only by Search / tapping a suggestion. Typing never recalculates the feed.
    var suggestionDraft by remember { mutableStateOf("") }
    LaunchedEffect(query) { kotlinx.coroutines.delay(240); suggestionDraft = query.trim() }
    var chosenSuggestion by remember { mutableStateOf<IosSearchSuggestion?>(null) }
    var appliedSuggestion by remember { mutableStateOf<IosSearchSuggestion?>(null) }
    var limit by remember { mutableIntStateOf(5) }
    var selected by remember { mutableStateOf<AgendaEventSummary?>(null) }
    var artist by remember { mutableStateOf<String?>(null) }
    var artistSourceEvent by remember { mutableStateOf<AgendaEventSummary?>(null) }
    LaunchedEffect(sessionBridge) {
        sessionBridge?.setPushOpenListener(object : IosPushOpenCallback {
            override fun onOpenArtist(artistName: String) {
                if (artistName.isNotBlank()) {
                    tab = "Inici"
                    artistSourceEvent = null
                    artist = artistName
                }
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
    var searchTown by remember { mutableStateOf("") }
    var searchCategory by remember { mutableStateOf("") }
    var searchComarca by remember { mutableStateOf("") }
    var searchStyle by remember { mutableStateOf("") }
    var searchRadius by remember { mutableIntStateOf(0) }
    var searchFromDate by remember { mutableStateOf("") }
    var searchToDate by remember { mutableStateOf("") }
    var appliedSearchFilters by remember { mutableStateOf(IosSearchFilters()) }
    var searchPresets by remember { mutableStateOf(emptyList<IosSavedSearch>()) }
    var newSearchName by remember { mutableStateOf("") }
    var loginOpen by remember { mutableStateOf(false) }
    var registrationOpen by remember { mutableStateOf(false) }
    var resetOpen by remember { mutableStateOf(false) }
    var prefOpen by remember { mutableStateOf(false) }
    var notificationsOpen by remember { mutableStateOf(false) }
    var communityGroup by remember { mutableStateOf<IosCommunityGroup?>(null) }
    var user by remember { mutableStateOf(IosUserState()) }
    var account by remember { mutableStateOf<IosAccount?>(null) }
    var notices by remember { mutableStateOf(emptyList<IosNotification>()) }
    var tickets by remember { mutableStateOf(emptyList<IosTicket>()) }
    var groups by remember { mutableStateOf(emptyList<IosCommunityGroup>()) }
    var batecs by remember { mutableStateOf(emptyList<IosBatec>()) }
    var seenBatecs by remember { mutableStateOf(emptySet<String>()) }
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
    var showSuggestions by remember { mutableStateOf(platformReadPublicCache(iosScopedCacheKey("pref_suggestions",null)) == "true") }
    var valencianFirst by remember { mutableStateOf(platformReadPublicCache(iosScopedCacheKey("pref_valencian",null)) != "false") }
    var discoveryLevel by remember { mutableStateOf(DiscoveryLevel.BALANCED) }

    // Personal feed exclusions are local and namespace-scoped by account.
    var excludedEventIds by remember { mutableStateOf(iosScopedSet("hidden_events", null).mapNotNull(String::toIntOrNull).toSet()) }
    var excludedArtists by remember { mutableStateOf(iosScopedSet("hidden_artists", null)) }
    var excludedCategories by remember { mutableStateOf(iosScopedSet("hidden_categories", null)) }
    var alertToDisable by remember { mutableStateOf<AgendaEventSummary?>(null) }
    var reminderToSchedule by remember { mutableStateOf<AgendaEventSummary?>(null) }
    var ticketEvent by remember { mutableStateOf<AgendaEventSummary?>(null) }
    var groupCreateFor by remember { mutableStateOf<AgendaEventSummary?>(null) }
    var validatorOpen by remember { mutableStateOf(false) }
    var savedLists by remember { mutableStateOf(emptyList<IosSavedList>()) }
    var profileSettings by remember { mutableStateOf<IosProfileSettings?>(null) }
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
        updating = true
        work {
            try {
                events = IosEventCatalog.get(force)
                paid = runCatching { api.promoted() }.getOrDefault(paid)
                entityStyles = withContext(Dispatchers.Default) { IosEntityAffinityCatalog.refresh(api, force) }
                if (account != null) user = runCatching { api.userState() }.getOrDefault(user)
                batecs = runCatching { api.batecs(user.follows.filter { it.type.equals("ARTIST", true) }
                    .map { it.label }.toSet()) }.getOrDefault(batecs)
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
        showSuggestions = platformReadPublicCache(iosScopedCacheKey("pref_suggestions",id)) == "true"
        valencianFirst = platformReadPublicCache(iosScopedCacheKey("pref_valencian",id)) != "false"
        discoveryLevel = runCatching { DiscoveryLevel.valueOf(platformReadPublicCache(iosScopedCacheKey("pref_discovery",id))) }.getOrDefault(DiscoveryLevel.BALANCED)

        excludedEventIds = iosScopedSet("hidden_events", id).mapNotNull(String::toIntOrNull).toSet()
        excludedArtists = iosScopedSet("hidden_artists", id)
        excludedCategories = iosScopedSet("hidden_categories", id)
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
                val listsRequest = async { runCatching { api.savedLists() }.getOrNull() }
                val presetsRequest = async { runCatching { api.savedSearches() }.getOrNull() }
                val bellsRequest = async { runCatching { api.artistBells() }.getOrNull() }
                val newUser = userRequest.await()
                // A session may be restored after the public catalog was loaded.
                // Re-fetch the followed Batecs without clearing the existing strip on failure.
                val newBatecs = runCatching { api.batecs(newUser.follows
                    .filter { it.type.equals("ARTIST", true) }.map { it.label }.toSet()) }.getOrNull()
                val newNotices = noticesRequest.await()
                val newTickets = ticketsRequest.await()
                val newGroups = groupsRequest.await()
                val newSeen = seenRequest.await()
                val newSavedLists = listsRequest.await()
                val newPresets = presetsRequest.await()
                val newBells = bellsRequest.await()
                // Never replace the new account's state with an old account's response.
                if (account?.id == expectedAccount) {
                    user = newUser
                    newBatecs?.let { batecs = it }
                    newNotices?.let { notices = it }
                    newBells?.let { artistBells = it.filter { b -> b.enabled }.associateBy { b -> IosArtistBellPolicy.key(b.artistName) } }
                    newTickets?.let { tickets = it }
                    newGroups?.let { groups = it }
                    newSeen?.let { seenBatecs = it }
                    newSavedLists?.let { savedLists = it }
                    newPresets?.let { searchPresets = it }
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        refresh(false)
        val restored = api.restoreAccount()
        if (restored != null) {
            account = restored; useAccountPreferences(restored.id); loadAccountPages()
            myProfile = runCatching { api.myPublicProfile() }.getOrNull()
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
    val today = remember { currentLocalDateIso() }
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
    // The Android carousel contains ONE circle per followed artist. Multiple server
    // publications share that circle; viewed status is grey only when ALL are seen.
    val followedGroups = remember(batecs, user.follows, seenBatecs) {
        IosBatecArtistGroups.select(
            batecs.map { BatecCandidate(it.storyKey, it.artist) },
            user.follows.filter { it.type.equals("ARTIST", true) }.map { it.label }.toSet(),
            seenBatecs
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
    val categoriesForSearch = remember(available) { available.map { it.category }.filter(String::isNotBlank).distinct().sorted() }
    val stylesForSearch = remember(available) { available.flatMap { it.styles.flatMap { s -> s.split('|',',',';') } }.map(String::trim).filter(String::isNotBlank).distinct().sorted() }
    val searchInProgress = appliedQuery.isNotBlank() || appliedSearchFilters.isActive
    // Strict global search: never restrict results to the previous personalized snapshot.
    val computedFiltered by produceState<List<AgendaEventSummary>>(initialValue = emptyList(), available, appliedQuery, appliedSuggestion, appliedSearchFilters) {
        value = withContext(Dispatchers.Default) {
            available.filter { IosAdvancedSearch.matches(it, appliedQuery, appliedSuggestion, appliedSearchFilters) }
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
        showSuggestions, discoveryLevel, valencianFirst, paid, entityStyles
    ) {
        val owner = account?.id
        val result = withContext(Dispatchers.Default) {
            IosForYouSnapshot.select(available, owner, user, chosenCategories, chosenTowns, paid,
                chosenSubcategories, chosenComarques, chosenRadii, showSuggestions,
                discoveryLevel, valencianFirst, entityStyles)
        }
        value = owner to result
    }
    val forYou = if (selectedForYou.first == account?.id) selectedForYou.second else emptyList()
    // Commercially guaranteed events retain priority even during searching. No duplicate cards.
    val activeFeed = remember(forYou, filtered, available, paid, feedMode, searchInProgress, valencianFirst, today) {
        // All active commercial placements and internal ticket sales remain visible,
        // even when the user enters a search; avoid duplicates by event occurrence.
        val commercialIds = paid.guaranteedEventIds + paid.ads.map { it.eventId }
        val guaranteed = available.filter { it.id in commercialIds || it.ticketAvailable }
        val organic = if (searchInProgress || feedMode == "Sense filtre") filtered else forYou
        if (!searchInProgress && feedMode == "Sense filtre") {
            // Use the shared full catalog without first concatenating paid subsets (no duplicates).
            // An ongoing festival is dated by its next relevant day, without mutating its original ID.
            available.sortedWith(compareBy<AgendaEventSummary> {
                IosUnfilteredFeedPolicy.relevantDate(it.date, it.endDate, today,
                    if (it.isFestival) it.schedule.map { slot -> slot.date } else emptyList())
            }.thenBy { it.time }.thenBy { it.id })
        } else (guaranteed + organic).distinctBy { it.key }
            .sortedWith(compareBy<AgendaEventSummary> { it.date.ifBlank { "9999" } }
                .thenByDescending { feedMode == "Per a tu" && valencianFirst &&
                    (IosGeoPlaces.key(it.language).contains("valencia") ||
                     IosGeoPlaces.key(it.language).contains("catala") || it.language.equals("ca",true)) }
                .thenBy { it.time }.thenBy { it.id })
    }
    // Exclusions do not invalidate the 30-minute ordered snapshot; they are applied
    // only to the organic display list. Commercial commitments always override hiding.
    val personalizedFeed = remember(activeFeed, excludedEventIds, excludedArtists, excludedCategories, paid) {
        val protectedIds = paid.guaranteedEventIds + paid.ads.map { it.eventId }
        activeFeed.filter { ev ->
            IosFeedExclusionPolicy.visible(ev.id, ev.artists + ev.schedule.map { it.artist }, ev.category,
                excludedEventIds, excludedArtists, excludedCategories,
                commerciallyGuaranteed = ev.id in protectedIds || ev.ticketAvailable)
        }
    }
    val visibleFeed = remember(personalizedFeed, limit) { personalizedFeed.take(limit) }

    fun showEvent(event: AgendaEventSummary, q: String = "") {
        // A visit contributes to optional discovery on its next snapshot refresh,
        // never forces a new feed calculation while navigating the current one.
        IosRecentInterest.recordOpen(account?.id,event)
        selected = event
        festivalMatchedArtist = if (event.isFestival && appliedSuggestion?.type == IosSearchType.ARTIST) appliedSuggestion?.value.orEmpty() else if (event.isFestival) q else ""
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
        if (account == null) { loginOpen = true; return }
        val next = event.id !in user.saved
        work {
            api.saveEvent(event, next)
            user = user.copy(saved = if (next) user.saved + event.id else user.saved - event.id)
            status = if (next) "Esdeveniment guardat" else "Esdeveniment desat"
        }
    }
    fun toggleReminder(event: AgendaEventSummary, next: Boolean, reminderAt: String = "") {
        if (account == null) { loginOpen = true; return }
        val requestUserId = account?.id ?: return
        work {
            if (next) {
                // Do not confirm until iOS has accepted the local notification request.
                require(reminderAt.matches(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}"))) {
                    "Cal indicar data i hora (AAAA-MM-DD HH:MM)."
                }
                require(reminderAt.take(10) <= event.endDate.ifBlank { event.date }) {
                    "El recordatori no pot ser posterior a l'activitat."
                }
                require(platformScheduleReminder(requestUserId, event.id, event.title, reminderAt)) {
                    "iOS no ha pogut programar l'avís. Comprova la data i el permís de notificacions."
                }
                // A logout or account switch may happen while Apple asks for notification permission.
                if (account?.id != requestUserId) {
                    platformCancelReminder(requestUserId, event.id)
                    return@work
                }
                try {
                    api.alertEvent(event, true)
                } catch (ex: Exception) {
                    platformCancelReminder(requestUserId, event.id)
                    throw ex
                }
                if (account?.id == requestUserId) {
                    platformWritePublicCache(iosReminderKey(requestUserId, event.id), reminderAt)
                    user = user.copy(alerts = user.alerts + event.id)
                    status = "Recordatori guardat i programat en iPhone"
                }
            } else {
                api.alertEvent(event, false)
                platformCancelReminder(requestUserId, event.id)
                if (account?.id == requestUserId) {
                    platformWritePublicCache(iosReminderKey(requestUserId, event.id), "")
                    user = user.copy(alerts = user.alerts - event.id)
                    status = "Recordatori desactivat"
                }
            }
        }
    }
    fun toggleFollowEntity(type: String, value: String) {
        if (account == null) { loginOpen = true; return }
        val clean = value.trim()
        if (clean.isBlank() || type !in setOf("ARTIST", "ORGANIZER", "VENUE", "CATEGORY")) return
        val expectedId = account?.id ?: return
        val actionId = "$type:$clean"
        if (actionId in pendingFollows) return
        val next = !IosFollowMutation.contains(user.follows, type, clean)
        pendingFollows = pendingFollows + actionId
        work {
            try {
                api.followEntity(type, clean, clean, next)
                if (account?.id != expectedId) return@work
                user = user.copy(follows = IosFollowMutation.updated(user.follows, type, clean, clean, next))
                status = if (next) "Ara seguixes $clean" else "Has deixat de seguir $clean"
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
                    // Android's add button opens the account/organizer area.
                    tab = "Perfil"
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
                                            limit = 5; IosRecentInterest.recordSearch(account?.id, appliedQuery, available)
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
                                    limit = 5; IosRecentInterest.recordSearch(account?.id, appliedQuery, available)
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
                            // Contractual HEADER creative before batecs, as Android. No duplicate in LazyColumn.
                            SponsoredCarousel("Capçalera", paid.ads.filter { it.placement == "HEADER" },
                                eventById, ::adViewed, ::adOpened, header = true)
                            if (account != null && followedGroups.size >= 6) {
                                LazyRow(
                                    state = batecRowState,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                                ) {
                                    items(followedGroups, key = { it.key }) { group ->
                                        val cover = batecs.firstOrNull { it.storyKey == group.openingStoryKey }
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
                                    searchCategory = ""; searchStyle = ""; searchFromDate = ""; searchToDate = ""
                                    appliedSearchFilters = IosSearchFilters(); limit = 5
                                }) { Text("Netejar cerca") }
                            }
                            if (searchFiltersOpen) {
                                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    OutlinedTextField(searchTown, { searchTown = it; if (it.isBlank()) searchRadius = 0 },
                                        label = { Text("On? Poble") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                    if (searchTown.length >= 2) {
                                        IosGeoPlaces.searchTowns(searchTown, 8).filterNot { it.equals(searchTown,true) }.forEach { town ->
                                            TextButton(onClick = { searchTown = town }) { Text("⌖ $town") }
                                        }
                                    }
                                    OutlinedTextField(searchComarca, { searchComarca = it },
                                        label = { Text("Comarca") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                    if (searchComarca.length >= 2) {
                                        IosGeoPlaces.allComarques().asSequence().filter { IosAdvancedSearch.norm(it).contains(IosAdvancedSearch.norm(searchComarca)) }
                                            .take(6).forEach { comarca ->
                                                TextButton(onClick = { searchComarca = comarca }) { Text("▱ $comarca") }
                                            }
                                    }
                                    Text("Radi des del poble (0 km = només el poble)", fontSize = 12.sp)
                                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(0,5,15,30,50,100).forEach { radius ->
                                            FilterChip(selected = searchRadius == radius, enabled = searchTown.isNotBlank() || radius == 0,
                                                onClick = { searchRadius = radius }, label = { Text("$radius km") })
                                        }
                                    }
                                    Text("Categoria", fontSize = 12.sp)
                                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                        FilterChip(selected = searchCategory.isBlank(), onClick = { searchCategory = "" }, label = { Text("Totes") })
                                        categoriesForSearch.forEach { category ->
                                            FilterChip(selected = searchCategory == category,
                                                onClick = { searchCategory = category }, label = { Text(category) })
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
                                    val draft = IosSearchFilters(searchTown.trim(), searchComarca.trim(), searchRadius,
                                        searchCategory.trim(), searchStyle.trim(), searchFromDate.trim(), searchToDate.trim())
                                    if (!draft.valid()) Text("Revisa les dates i el radi: cal seleccionar un poble per aplicar distància.", color = Color.Red, fontSize = 12.sp)
                                    Button(onClick = { appliedSearchFilters = draft; appliedQuery = query.trim(); appliedSuggestion = chosenSuggestion; limit = 5 },
                                        enabled = draft.valid()) { Text("Aplicar filtres") }
                                    Text("Els filtres no amplien els resultats d'una cerca; la publicitat contractual conserva els seus espais.",
                                        fontSize = 10.sp, color = Color.Gray)
                                    if (account != null) {
                                        val currentId = account?.id
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            OutlinedTextField(newSearchName, { newSearchName = it.take(52) },
                                                label = { Text("Nom de la cerca") }, modifier = Modifier.weight(1f), singleLine = true)
                                            TextButton(enabled = searchTown.isNotBlank() && newSearchName.isNotBlank(), onClick = {
                                                val name = newSearchName.trim(); val town = searchTown.trim(); val category = searchCategory.trim()
                                                work {
                                                    api.saveTownSearch(name,town,if (category.isBlank()) emptyList() else listOf(category))
                                                    if (account?.id == currentId) {
                                                        searchPresets = api.savedSearches(); newSearchName = ""
                                                        status = "Poble i categoria guardats; radi i dates no es sincronitzen"
                                                    }
                                                }
                                            }) { Text("Guardar") }
                                        }
                                        searchPresets.forEach { preset ->
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                TextButton(onClick = {
                                                    searchTown = preset.town
                                                    searchCategory = preset.categories.firstOrNull().orEmpty()
                                                    searchComarca = ""; searchRadius = 0; searchStyle = ""; searchFromDate = ""; searchToDate = ""
                                                    appliedSearchFilters = IosSearchFilters(town = searchTown,category = searchCategory)
                                                    limit = 5
                                                }, modifier = Modifier.weight(1f)) {
                                                    Text("${preset.name} · ${preset.town}", maxLines = 1)
                                                }
                                                TextButton(onClick = { work {
                                                    api.removeSavedSearch(preset.name)
                                                    if (account?.id == currentId) searchPresets = api.savedSearches()
                                                } }) { Text("✕") }
                                            }
                                        }
                                    }
                                }
                            }
                            if (searchInProgress) {
                                Text("Filtres actius · ${filtered.size} resultats orgànics", fontSize = 11.sp,
                                    color = Color.DarkGray, modifier = Modifier.padding(horizontal = 16.dp))
                            }
                            if (feedMode == "Per a tu" && account == null && chosenCategories.isEmpty() && chosenTowns.isEmpty()) {
                                Text("Inicia sessió i seguix artistes o tria preferències per personalitzar el feed.",
                                    fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp))
                                TextButton(onClick = { loginOpen = true }) { Text("Iniciar sessió") }
                            }
                            if (feedMode == "Per a tu" && account != null && !user.follows.any { it.type == "ARTIST" } && chosenCategories.isEmpty()) {
                                Text("Seguix artistes o tria categories a Personalització.", modifier = Modifier.padding(horizontal = 16.dp), fontSize = 13.sp)
                            }
                            if (updating) LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().height(2.dp), color = Ink, trackColor = Accent)
                            EventList(visibleFeed, personalizedFeed.size,
                                onLoadMore = { limit = IosUnfilteredFeedPolicy.nextCount(limit, personalizedFeed.size) },
                                today = today,
                                scrollContext = "${feedMode}|${appliedQuery}|${appliedSearchFilters.hashCode()}|${account?.id}",
                                savedIds = user.saved, onClick = { showEvent(it, appliedQuery) }, onSave = ::toggleSaved,
                                ads = paid.ads, protectedIds = paid.guaranteedEventIds,
                                commercialEvents = eventById,
                                onAdViewed = ::adViewed, onAdOpened = ::adOpened,
                                refreshing = updating, onRefresh = { refresh(true) },
                                returnToStartEpoch = batecsReturnEpoch,
                                onExclude = ::excludeFromFeed, onReport = { reportEvent = it })
                        }
                    }
                    "Guardats" -> {
                        if (account == null) {
                            Column(Modifier.fillMaxSize()) {
                                PageTitle("Les meues agendes")
                                LoginPrompt { loginOpen = true }
                            }
                        } else {
                            val expectedAccountId = account?.id ?: 0
                            key(expectedAccountId) {
                                IosSavedAgendaScreen(
                                    accountId = expectedAccountId,
                                    user = user,
                                    lists = savedLists,
                                    available = available,
                                    today = today,
                                    api = api,
                                    work = ::work,
                                    onListsChanged = { result -> if (account?.id == expectedAccountId) savedLists = result },
                                    onStatus = { status = it },
                                    onOpenEvent = { event, matchedArtist -> showEvent(event, matchedArtist) },
                                    onToggleSaved = ::toggleSaved,
                                    onDisableReminder = { alertToDisable = it },
                                    reminderTime = { event -> platformReadPublicCache(iosReminderKey(expectedAccountId,event.id)) },
                                    festivalRevision = savedFestivalRevision
                                )
                            }
                        }
                    }
                    "Entrades" -> {
                        Column(Modifier.fillMaxSize()) {
                            PageTitle("Les meues entrades")
                            if (account == null) LoginPrompt { loginOpen = true }
                            else {
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
                        if (account == null) LoginPrompt { loginOpen = true }
                        else IosCommunityHub(groups, api, isAdmin = IosCommunityPolicy.canSeeAdminQueue(account?.role.orEmpty()),
                            reload = { mine -> work { groups = api.community(mine=mine) } },
                            onOpen = { communityGroup = it })
                    }
                    "Perfil" -> {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Perfil", fontWeight = FontWeight.Bold, fontSize = 26.sp)
                            if (account == null) {
                                Text("No has iniciat sessió.")
                                Button(onClick = { loginOpen = true }) { Text("Iniciar sessió") }
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
                                }) { Text("Editar nom, foto i biografia") }
                                Text("Rol: ${account?.role}", fontSize = 12.sp)
                                Text("Artistes seguits: ${user.follows.count { it.type == "ARTIST" }}")
                                Button(onClick = { loadAccountPages() }) { Text("Actualitzar dades") }
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
                                    try { api.signOutSecurely() } catch (_: Exception) { /* Local token is always revoked. */ }
                                    account = null; myProfile = null; user = IosUserState(); artistBells = emptyMap(); tickets = emptyList()
                                    notices = emptyList(); groups = emptyList(); seenBatecs = emptySet(); savedLists = emptyList(); searchPresets = emptyList(); searchTown = ""; searchCategory = ""; searchComarca = ""; searchStyle = ""; searchRadius = 0; searchFromDate = ""; searchToDate = ""; appliedSearchFilters = IosSearchFilters(); appliedQuery = ""; query = ""; appliedSuggestion = null; chosenSuggestion = null; newSearchName = ""
                                    useAccountPreferences(null)
                                    status = "Sessió tancada"
                                } }) { Text("Tancar sessió") }
                                if (account?.role != "PUBLIC") {
                                    Divider()
                                    Button(onClick = { validatorOpen = true }) { Text("Validar entrades (PRO)") }
                                    Text("El servidor comprova els permisos de cada esdeveniment.", fontSize = 11.sp, color = Color.Gray)
                                }
                                if (account?.role == "SUPER_ADMIN") {
                                    Divider()
                                    IosSuperAdminConsole(api, actorId = account?.id ?: 0, onMessage = { status = it })
                                }
                            }
                            if (account != null && user.follows.isNotEmpty()) {
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
                user = IosUserState(); artistBells = emptyMap(); tickets = emptyList(); notices = emptyList(); groups = emptyList(); seenBatecs = emptySet(); savedLists = emptyList(); searchPresets = emptyList(); searchTown = ""; searchCategory = ""; searchComarca = ""; searchStyle = ""; searchRadius = 0; searchFromDate = ""; searchToDate = ""; appliedSearchFilters = IosSearchFilters(); appliedQuery = ""; query = ""; appliedSuggestion = null; chosenSuggestion = null; newSearchName = ""
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
                    work {
                        val next = api.signInWithProvider(provider, idToken, nonce)
                        account = next
                        user = IosUserState(); artistBells = emptyMap(); tickets = emptyList(); notices = emptyList()
                        groups = emptyList(); seenBatecs = emptySet(); savedLists = emptyList(); searchPresets = emptyList()
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
                user = IosUserState(); artistBells = emptyMap(); tickets = emptyList(); notices = emptyList(); groups = emptyList(); seenBatecs = emptySet(); savedLists = emptyList(); searchPresets = emptyList(); searchTown = ""; searchCategory = ""; searchComarca = ""; searchStyle = ""; searchRadius = 0; searchFromDate = ""; searchToDate = ""; appliedSearchFilters = IosSearchFilters(); appliedQuery = ""; query = ""; appliedSuggestion = null; chosenSuggestion = null; newSearchName = ""
                useAccountPreferences(account?.id)
                loginOpen = false; status = "Sessió iniciada"
                loadAccountPages()
                myProfile = runCatching { api.myPublicProfile() }.getOrNull()
            }
        })
        if (prefOpen) PersonalizationDialog(chosenCategories, chosenTowns, excludedEventIds, excludedArtists,
            excludedCategories, available, events.map { it.category }.filter(String::isNotBlank).distinct().sorted(),
            events.map { it.town }.filter(String::isNotBlank).distinct().sorted(),
            chosenSubcategories, chosenComarques, chosenRadii, showSuggestions,discoveryLevel,valencianFirst,
            onDismiss = { prefOpen = false }, onSave = { cats, towns, hiddenEvents, hiddenArtists, hiddenCategories,
                styles,comarques,radii,suggestions,discovery,valencian ->
                chosenCategories = cats; chosenTowns = towns
                chosenSubcategories = styles; chosenComarques = comarques; chosenRadii = radii
                showSuggestions = suggestions; discoveryLevel = discovery; valencianFirst = valencian
                saveExclusions(hiddenEvents, hiddenArtists, hiddenCategories)
                val id = account?.id
                platformWritePublicCache(iosScopedCacheKey("pref_categories",id), IosAccountCachePolicy.encode(cats))
                platformWritePublicCache(iosScopedCacheKey("pref_towns",id), IosAccountCachePolicy.encode(towns))
                platformWritePublicCache(iosScopedCacheKey("pref_subcategories",id), IosAccountCachePolicy.encode(styles))
                platformWritePublicCache(iosScopedCacheKey("pref_comarques",id), IosAccountCachePolicy.encode(comarques))
                platformWritePublicCache(iosScopedCacheKey("pref_radii",id), IosAccountCachePolicy.encode(IosPersonalFeedPolicy.encodeRadii(radii)))
                platformWritePublicCache(iosScopedCacheKey("pref_suggestions",id),suggestions.toString())
                platformWritePublicCache(iosScopedCacheKey("pref_discovery",id),discovery.name)
                platformWritePublicCache(iosScopedCacheKey("pref_valencian",id),valencian.toString())
                prefOpen = false; status = "Preferències guardades"
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
            reminderTime = { ev -> account?.id?.let { id -> platformReadPublicCache(iosReminderKey(id,ev.id)) }.orEmpty() })
        val openedBatecGroup = followedGroups.firstOrNull { it.key == batecStoryKey }
        if (batecStoryKey != null && openedBatecGroup != null) {
            BatecViewer(
                groups = followedGroups,
                selectedKey = openedBatecGroup.key,
                stories = batecs,
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
                    val pending = group.storyKeys.filter { it !in seenBatecs }
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
                if (account == null) loginOpen = true
                else work { groups = api.community(ev.id); tab = "Comunitat" }
            }, onCreateGroup = {
                if (account == null) loginOpen = true else { selected = null; groupCreateFor = ev }
            }, onTickets = {
                if (ev.ticketUrl.startsWith("https://")) platformOpenExternal(ev.ticketUrl)
                else if (ev.ticketAvailable) {
                    selected = null
                    if (account == null) loginOpen = true else ticketEvent = ev
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
                onEvent = { artist = null; artistSourceEvent = null; showEvent(it) })
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
                        account = null; myProfile = null; user = IosUserState(); artistBells = emptyMap()
                        tickets = emptyList(); notices = emptyList(); groups = emptyList(); seenBatecs = emptySet()
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
            onDismiss = { ticketEvent = null }, onPurchased = {
                ticketEvent = null; status = "Entrades gratuïtes emeses correctament"; tab = "Entrades"
                work { tickets = api.tickets() }
            }, onError = { error = it }) }
        groupCreateFor?.let { event -> CreateGroupDialog(event, api, onDismiss = { groupCreateFor = null },
            onCreated = { groupCreateFor = null; status = "Tauló creat"; work { groups = api.community(event.id) } },
            onError = { error = it }) }
        communityGroup?.let { g -> IosCommunityChatDialog(g, api, account?.id ?: 0,
            onDismiss = { communityGroup = null }, onChanged = { work { groups = api.community() } }) }
        reminderToSchedule?.let { ev -> ReminderEditorDialog(ev, account?.id ?: 0,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventList(events: List<AgendaEventSummary>, total: Int, onLoadMore: () -> Unit,
                      today: String, scrollContext: String,
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
    // A new mode/confirmed search has a new sequence. Do not reuse a deep scroll position
    // from an old filtered feed, even if the advertisement header is present.
    LaunchedEffect(scrollContext) { listState.scrollToItem(0) }
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
private fun PersonalizationDialog(categories: Set<String>, towns: Set<String>, hiddenEventIds: Set<Int>,
                                  hiddenArtists: Set<String>, hiddenCategories: Set<String>,
                                  allEvents: List<AgendaEventSummary>, options: List<String>, townOptions: List<String>,
                                  styles: Set<String>, comarques: Set<String>, radii: Map<String,Int>,
                                  suggestions: Boolean, discovery: DiscoveryLevel, valencian: Boolean,
                                  onDismiss: () -> Unit,
                                  onSave: (Set<String>, Set<String>, Set<Int>, Set<String>, Set<String>,
                                           Set<String>, Set<String>, Map<String,Int>, Boolean, DiscoveryLevel, Boolean) -> Unit) {
    var cat by remember { mutableStateOf(categories) }
    var selectedTowns by remember { mutableStateOf(towns) }
    var townQuery by remember { mutableStateOf("") }
    var selectedStyles by remember { mutableStateOf(styles) }
    var selectedComarques by remember { mutableStateOf(comarques) }
    var perTownRadius by remember { mutableStateOf(radii) }
    var suggestionsEnabled by remember { mutableStateOf(suggestions) }
    var selectedDiscovery by remember { mutableStateOf(discovery) }
    var preferredValencian by remember { mutableStateOf(valencian) }
    var comarcaQuery by remember { mutableStateOf("") }
    val possibleStyles = remember(allEvents) { allEvents.flatMap { it.styles }.flatMap { it.split('|',',',';') }.map(String::trim).filter(String::isNotBlank).distinct().sorted() }
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
                    options.forEach { item ->
                        Row(Modifier.fillMaxWidth().clickable { cat = if (item in cat) cat - item else cat + item },
                            verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = item in cat, onCheckedChange = { selected -> cat = if (selected) cat + item else cat - item })
                            Text(item, fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Estils i subcategories", fontWeight = FontWeight.Bold)
                    possibleStyles.forEach { item ->
                        Row(Modifier.fillMaxWidth(),verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = item in selectedStyles,onCheckedChange = { checked ->
                                selectedStyles = if (checked) selectedStyles + item else selectedStyles - item })
                            Text(item,fontSize = 13.sp)
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
                    Row(Modifier.fillMaxWidth(),verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = preferredValencian,onCheckedChange = { preferredValencian = it })
                        Text("Prioritzar activitats en valencià",fontSize = 13.sp)
                    }
                    Row(Modifier.fillMaxWidth(),verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = suggestionsEnabled,onCheckedChange = { suggestionsEnabled = it })
                        Text("Suggerències segons les activitats obertes (últims 15 dies)",fontSize = 12.sp)
                    }
                    if (suggestionsEnabled) {
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
                        suggestionsEnabled,selectedDiscovery,preferredValencian) }) { Text("Guardar") }
                }
            }
        }
    }
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
    LaunchedEffect(ev.id) {
        if (ev.isFestival) {
            runCatching { loadSchedule(ev.id) }.onSuccess { fresh ->
                if (fresh.isNotEmpty()) detailSchedule = fresh
            }
        }
        detailLoading = false
    }
    val festivalDays = remember(ev.id, detailSchedule) {
        detailSchedule.map { it.date.take(10) }.filter(String::isNotBlank).distinct().sorted()
    }
    val targetSlot = remember(ev.id, matchedArtist, detailSchedule) {
        detailSchedule.firstOrNull { matchedArtist.isNotBlank() &&
            (it.artist.contains(matchedArtist, true) || it.title.contains(matchedArtist, true)) }
    }
    var section by remember(ev.id, matchedArtist) {
        mutableStateOf(if (ev.isFestival) "Horaris" else "Artistes")
    }
    var selectedDay by remember(ev.id, matchedArtist) {
        mutableStateOf(targetSlot?.date?.take(10)?.takeIf(String::isNotBlank) ?: festivalDays.firstOrNull().orEmpty())
    }
    LaunchedEffect(targetSlot?.date, festivalDays) {
        val focused = targetSlot?.date?.take(10)
        if (!focused.isNullOrBlank() && focused in festivalDays) selectedDay = focused
        else if (selectedDay !in festivalDays) selectedDay = festivalDays.firstOrNull().orEmpty()
    }
    var showPoster by remember(ev.id) { mutableStateOf(false) }
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
        Surface(shape = RoundedCornerShape(22.dp), color = Color.White,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).fillMaxHeight(0.78f)) {
            Column {
                // Android event popup: image/title/date/place form a SINGLE gradient hero.
                Box(Modifier.fillMaxWidth().height(154.dp)) {
                    Poster(ev.posterUrl, Modifier.fillMaxSize())
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = .08f),
                            Color.Black.copy(alpha = .19f), Color.Black.copy(alpha = .91f)))))
                    Column(Modifier.align(Alignment.BottomStart).padding(start = 15.dp, end = 8.dp, bottom = 13.dp)) {
                        Text("ESDEVENIMENT", color = Color.White, fontSize = 10.sp,
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
                    Text("${IosUnfilteredFeedPolicy.fullDate(ev.date)} · ${ev.time}",
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
                val tabs = if (ev.isFestival) listOf("Horaris", "Info", "Ubicació", "Comunitat")
                    else listOf("Artistes", "Info", "Ubicació", "Comunitat")
                Row(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical=12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tabs.forEach { name ->
                        val icon = when(name) { "Artistes" -> "person"; "Horaris" -> "calendar"
                            "Info" -> "info"; "Ubicació" -> "pin"; else -> "people" }
                        val active = section == name
                        Column(Modifier.weight(1f)
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
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                    when (section) {
                        "Artistes" -> {
                            Text("Artistes", fontSize=19.sp, fontWeight=FontWeight.Bold,
                                modifier=Modifier.padding(vertical=10.dp))
                            ev.artists.ifEmpty { listOf(ev.title) }.distinct().forEach { a ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)
                                    .background(Color(0xFFFAFAFA),RoundedCornerShape(12.dp))
                                    .padding(10.dp), verticalAlignment=Alignment.CenterVertically) {
                                    Poster(ev.posterUrl, Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)))
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
                            Text(ev.venue.ifBlank { ev.town }, fontWeight = FontWeight.Bold)
                            if (ev.venue.isNotBlank() && ev.town.isNotBlank()) Text(ev.town, fontSize = 13.sp)
                            if (ev.venue.isNotBlank()) {
                                TextButton(onClick = { onToggleFollowEntity("VENUE", ev.venue) }) {
                                    Text(if (followsEntity("VENUE", ev.venue)) "✓ Seguint esta sala" else "+ Seguir esta sala")
                                }
                            }
                            val place = listOf(ev.venue, ev.town).filter(String::isNotBlank).joinToString(", ")
                            if (place.isNotBlank()) OutlinedButton(onClick = {
                                platformOpenExternal("https://maps.apple.com/?q=${place.encodeURLParameter()}")
                            }) { Text("Obrir indicacions en Mapes") }
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
    if (showPoster) Dialog(onDismissRequest = { showPoster = false }) {
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.85f).clickable { showPoster = false }) {
            Poster(ev.posterUrl, Modifier.fillMaxSize())
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
    reminderTime: (AgendaEventSummary) -> String
) {
    var view by remember { mutableStateOf(IosNotificationGrouping.GENERAL) }
    val tabs = listOf("General" to "bell", "Pròxim" to "clock", "Nous" to "info",
        "Entrades" to "ticket", "Suggerències" to "search", "Comunitat" to "people")
    val grouped = remember(notifications) { notifications.groupBy {
        IosNotificationGrouping.fromDeepLinkType(it.linkType) } }
    val shown = grouped[view].orEmpty()
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
                        AgendaTopAction("sliders", onClick={
                            IosNativeBridge.current?.showNotificationSettings()
                        })
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
                        if (view == "Recordatoris") {
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
                            if(shown.isEmpty()) item { EmptyState("No hi ha avisos en esta categoria.") }
                        }
                    }
                    if(view != "Recordatoris" && shown.any{!it.read}) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement=Arrangement.End) {
                            OutlinedButton(onClick={ shown.filterNot{it.read}.forEach(onRead) }) {
                                Text("Marcar com a llegides",fontSize=12.sp)
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
private fun TicketOptionsDialog(event: AgendaEventSummary, api: IosAgendaApi, onDismiss: () -> Unit,
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
                    busy = true
                    scope.launch {
                        try { api.reserveFreeTicket(event.id, selectedOption?.id ?: 0, quantity); onPurchased() }
                        catch (ex: Exception) { onError(ex.message ?: "No s'ha pogut emetre l'entrada") }
                        finally { busy = false }
                    }
                }) { Text(if (busy) "Emetent…" else "Reservar gratuïtament") }
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
