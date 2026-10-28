package com.example.agendavalenciana.ios

import com.example.agendavalenciana.model.BatecCandidate
import com.example.agendavalenciana.model.FollowedBatecsPolicy
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
    LaunchedEffect(query) { kotlinx.coroutines.delay(220); appliedQuery = query.trim() }
    var limit by remember { mutableIntStateOf(5) }
    var selected by remember { mutableStateOf<AgendaEventSummary?>(null) }
    var artist by remember { mutableStateOf<String?>(null) }
    var artistSourceEvent by remember { mutableStateOf<AgendaEventSummary?>(null) }
    var festivalMatchedArtist by remember { mutableStateOf("") }
    var savedMode by remember { mutableStateOf("Guardats") }
    var batecStoryKey by remember { mutableStateOf<String?>(null) }
    var searchFiltersOpen by remember { mutableStateOf(false) }
    var searchTown by remember { mutableStateOf("") }
    var searchCategory by remember { mutableStateOf("") }
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
    var reportEvent by remember { mutableStateOf<AgendaEventSummary?>(null) }
    var pendingFollows by remember { mutableStateOf(emptySet<String>()) }

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
                batecs = runCatching { api.batecs() }.getOrDefault(batecs)
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
                val newUser = userRequest.await()
                val newNotices = noticesRequest.await()
                val newTickets = ticketsRequest.await()
                val newGroups = groupsRequest.await()
                val newSeen = seenRequest.await()
                val newSavedLists = listsRequest.await()
                val newPresets = presetsRequest.await()
                // Never replace the new account's state with an old account's response.
                if (account?.id == expectedAccount) {
                    user = newUser
                    newNotices?.let { notices = it }
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
        if (restored != null) { account = restored; useAccountPreferences(restored.id); loadAccountPages() }
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
    val followedStories = remember(batecs, user.follows) {
        val followedStoryKeys = FollowedBatecsPolicy.firstSix(
            batecs.map { BatecCandidate(it.storyKey, it.artist) },
            user.follows.filter { it.type.equals("ARTIST", true) }.map { it.label }.toSet()
        )
        val byKey = batecs.associateBy { it.storyKey }
        followedStoryKeys.mapNotNull(byKey::get)
    }
    // Search filters affect only the requested results. The feed snapshot itself is untouched.
    val filtered = remember(available, appliedQuery, searchTown, searchCategory) {
        available.filter { ev ->
            ev.matches(appliedQuery) &&
                (searchTown.isBlank() || ev.town.contains(searchTown.trim(), ignoreCase = true)) &&
                (searchCategory.isBlank() || ev.category.contains(searchCategory.trim(), ignoreCase = true))
        }
    }
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
    val activeFeed = remember(forYou, filtered, available, paid, feedMode, appliedQuery, searchTown, searchCategory,valencianFirst) {
        // All active commercial placements and internal ticket sales remain visible,
        // even when the user enters a search; avoid duplicates by event occurrence.
        val commercialIds = paid.guaranteedEventIds + paid.ads.map { it.eventId }
        val guaranteed = available.filter { it.id in commercialIds || it.ticketAvailable }
        val selectedKeys = filtered.mapTo(HashSet(filtered.size)) { it.key }
        val organic = if (feedMode == "Sense filtre") filtered else forYou.filter { it.key in selectedKeys }
        (guaranteed + organic).distinctBy { it.key }
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
        festivalMatchedArtist = if (event.isFestival) q else ""
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
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { tab = "Perfil" }) { Text("♙", fontSize = 25.sp) }
                Text("l’Agenda", fontSize = 31.sp, fontWeight = FontWeight.Black, color = Ink)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { prefOpen = true }) { Text("☷", fontSize = 25.sp) }
                    TextButton(onClick = { notificationsOpen = true; if (account != null) loadAccountPages() }) {
                        Text("♧${if (notices.any { !it.read }) "•" else ""}", fontSize = 24.sp)
                    }
                }
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
                            Text("Descobrix la cultura", modifier = Modifier.padding(start = 18.dp, top = 8.dp, bottom = 8.dp),
                                fontSize = 23.sp, fontWeight = FontWeight.Bold)
                            Row(Modifier.padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Per a tu", "Sense filtre").forEach { mode ->
                                    FilterChip(selected = feedMode == mode, onClick = { feedMode = mode; limit = 5 }, label = { Text(mode) })
                                }
                            }
                            if (account != null && user.follows.any { it.type == "ARTIST" }) {
                                if (followedStories.size == 6) {
                                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        followedStories.forEach { b ->
                                            val viewed = b.storyKey in seenBatecs
                                            Column(Modifier.width(73.dp).clickable {
                                                // A Batec is a story, not an immediate event popup.
                                                batecStoryKey = b.storyKey
                                            }, horizontalAlignment = Alignment.CenterHorizontally) {
                                                Box(Modifier.size(58.dp).background(if (viewed) Color(0xFFE0E0E0) else Accent, CircleShape).padding(2.dp)) {
                                                    Poster(b.imageUrl.ifBlank { eventById[b.eventId]?.posterUrl.orEmpty() }, Modifier.fillMaxSize().clip(CircleShape))
                                                }
                                                Text(b.artist, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                                    color = if (viewed) Color.Gray else Ink)
                                            }
                                        }
                                    }
                                }
                            }
                            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(value = query, onValueChange = { query = it; limit = 5 },
                                    singleLine = true, label = { Text("Buscar artistes, actes, pobles…") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = {
                                        appliedQuery = query.trim()
                                        IosRecentInterest.recordSearch(account?.id, query, available)
                                    }))
                                TextButton(onClick = {
                                    appliedQuery = query.trim()
                                    IosRecentInterest.recordSearch(account?.id, query, available)
                                }, enabled = query.trim().length >= 3) { Text("Cercar") }
                            }
                            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { searchFiltersOpen = !searchFiltersOpen }) {
                                    Text(if (searchFiltersOpen) "▴ Ocultar filtres" else "☷ Filtres de cerca")
                                }
                                if (searchTown.isNotBlank() || searchCategory.isNotBlank()) {
                                    TextButton(onClick = { searchTown = ""; searchCategory = ""; limit = 5 }) { Text("Netejar") }
                                }
                            }
                            if (searchFiltersOpen) {
                                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(searchTown, { searchTown = it; limit = 5 },
                                        label = { Text("On? Poble") }, singleLine = true, modifier = Modifier.weight(1f))
                                    OutlinedTextField(searchCategory, { searchCategory = it; limit = 5 },
                                        label = { Text("Categoria") }, singleLine = true, modifier = Modifier.weight(1f))
                                }
                                Text("Els filtres de cerca no amplien els resultats; els anuncis contractats conserven la seua prioritat.",
                                    fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 16.dp))
                                if (account != null) {
                                    val currentId = account?.id
                                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(newSearchName, { newSearchName = it.take(52) },
                                            label = { Text("Nom de la cerca") }, modifier = Modifier.weight(1f), singleLine = true)
                                        TextButton(enabled = searchTown.isNotBlank() && newSearchName.isNotBlank(), onClick = {
                                            val name = newSearchName.trim(); val town = searchTown.trim()
                                            work {
                                                api.saveTownSearch(name, town)
                                                if (account?.id == currentId) {
                                                    searchPresets = api.savedSearches()
                                                    newSearchName = ""; status = "Filtre de població guardat"
                                                }
                                            }
                                        }) { Text("Guardar") }
                                    }
                                    searchPresets.forEach { preset ->
                                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically) {
                                            TextButton(onClick = { searchTown = preset.town; limit = 5 },
                                                modifier = Modifier.weight(1f)) { Text("${preset.name} · ${preset.town}", maxLines = 1) }
                                            TextButton(onClick = { work {
                                                api.removeSavedSearch(preset.name)
                                                if (account?.id == currentId) searchPresets = api.savedSearches()
                                            } }) { Text("✕") }
                                        }
                                    }
                                }
                            }
                            Row(Modifier.fillMaxWidth().padding(horizontal = 15.dp), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (updating && events.isEmpty()) "Carregant catàleg…" else "${personalizedFeed.size} activitats", fontSize = 12.sp, color = Color.DarkGray)
                                TextButton(onClick = { refresh(true) }, enabled = !updating) { Text(if (updating) "Actualitzant…" else "↻ Actualitzar") }
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
                            EventList(visibleFeed, personalizedFeed.size, onLoadMore = { limit = (limit + if (limit == 5) 15 else 10).coerceAtMost(personalizedFeed.size) },
                                savedIds = user.saved, onClick = { showEvent(it, appliedQuery) }, onSave = ::toggleSaved,
                                ads = paid.ads, protectedIds = paid.guaranteedEventIds,
                                commercialEvents = eventById,
                                onAdViewed = ::adViewed, onAdOpened = ::adOpened,
                                refreshing = updating, onRefresh = { refresh(true) },
                                onExclude = ::excludeFromFeed, onReport = { reportEvent = it })
                        }
                    }
                    "Guardats" -> {
                        Column(Modifier.fillMaxSize()) {
                            Text("Les meues agendes", fontSize = 26.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 18.dp, top = 17.dp, bottom = 10.dp))
                            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                listOf("Guardats", "Recordatoris", "Llistes").forEach { item ->
                                    FilterChip(selected = savedMode == item, onClick = { savedMode = item }, label = { Text(item) })
                                }
                            }
                            val ids = if (savedMode == "Guardats") user.saved else user.alerts
                            val results = remember(events, ids, today) {
                                events.filter { it.id in ids && (it.endDate.ifBlank { it.date }.isBlank() || it.endDate.ifBlank { it.date } >= today) }
                            }
                            if (account == null) LoginPrompt { loginOpen = true }
                            else if (savedMode == "Llistes") {
                                val expectedAccountId = account?.id
                                key(expectedAccountId) {
                                    SavedListsPanel(
                                        lists = savedLists, available = available, api = api, work = ::work,
                                        onOpenEvent = { showEvent(it) },
                                        onChanged = { incoming -> if (account?.id == expectedAccountId) savedLists = incoming },
                                        onStatus = { status = it }
                                    )
                                }
                            }
                            else if (results.isEmpty()) EmptyState(if (savedMode == "Guardats") "Encara no tens guardats." else "No tens recordatoris actius.")
                            else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(results, key = { it.key }) { ev ->
                                    EventCard(ev, ev.id in user.saved, onClick = { showEvent(ev) },
                                        trailing = {
                                            if (savedMode == "Recordatoris") TextButton(onClick = { alertToDisable = ev }) { Text("♧", fontSize = 23.sp) }
                                            else TextButton(onClick = { toggleSaved(ev) }) { Text("▣", fontSize = 23.sp) }
                                        })
                                }
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
                        Column(Modifier.fillMaxSize()) {
                            PageTitle("Comunitat")
                            if (account == null) LoginPrompt { loginOpen = true }
                            else {
                                TextButton(onClick = { work { groups = api.community() } }, modifier = Modifier.padding(horizontal = 12.dp)) { Text("↻ Actualitzar comunitat") }
                                if (groups.isEmpty()) EmptyState("No hi ha taulons disponibles.")
                                else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(groups, key = { it.id }) { g ->
                                        ElevatedCard(Modifier.fillMaxWidth().clickable { communityGroup = g }) {
                                            Column(Modifier.padding(13.dp)) {
                                                Text(g.title.ifBlank { "Tauló #${g.id}" }, fontWeight = FontWeight.Bold)
                                                Text("${g.type} · ${g.access}", fontSize = 12.sp, color = Color.Gray)
                                                if (g.description.isNotBlank()) Text(g.description, maxLines = 2, fontSize = 13.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "Perfil" -> {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Perfil", fontWeight = FontWeight.Bold, fontSize = 26.sp)
                            if (account == null) {
                                Text("No has iniciat sessió.")
                                Button(onClick = { loginOpen = true }) { Text("Iniciar sessió") }
                            } else {
                                Text(account?.username.orEmpty(), fontWeight = FontWeight.Bold)
                                Text(account?.email.orEmpty(), fontSize = 12.sp)
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
                                OutlinedButton(onClick = { work {
                                    try { api.signOutSecurely() } catch (_: Exception) { /* Local token is always revoked. */ }
                                    account = null; user = IosUserState(); tickets = emptyList()
                                    notices = emptyList(); groups = emptyList(); seenBatecs = emptySet(); savedLists = emptyList(); searchPresets = emptyList(); searchTown = ""; searchCategory = ""; newSearchName = ""
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
                                    AdminNotificationPanel(api, work = ::work, onMessage = { status = it })
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
            NavigationBar(containerColor = Pale) {
                listOf("Inici" to "⌂", "Guardats" to "♥", "Entrades" to "▤", "Comunitat" to "♙").forEach { (name, symbol) ->
                    NavigationBarItem(selected = tab == name, onClick = { tab = name },
                        icon = { Text(symbol, fontSize = 20.sp) }, label = { Text(name, fontSize = 10.sp) })
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
                user = IosUserState(); tickets = emptyList(); notices = emptyList(); groups = emptyList(); seenBatecs = emptySet(); savedLists = emptyList(); searchPresets = emptyList(); searchTown = ""; searchCategory = ""; newSearchName = ""
                useAccountPreferences(account?.id)
                registrationOpen = false; loginOpen = false; status = "Compte creat"
                loadAccountPages()
            }
        })
        if (loginOpen) LoginDialog(onDismiss = { loginOpen = false }, onRegister = {
            loginOpen = false; registrationOpen = true
        }, onForgotPassword = {
            loginOpen = false; resetOpen = true
        }, onSubmit = { login, password ->
            work {
                account = api.signIn(login, password)
                user = IosUserState(); tickets = emptyList(); notices = emptyList(); groups = emptyList(); seenBatecs = emptySet(); savedLists = emptyList(); searchPresets = emptyList(); searchTown = ""; searchCategory = ""; newSearchName = ""
                useAccountPreferences(account?.id)
                loginOpen = false; status = "Sessió iniciada"
                loadAccountPages()
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
        if (notificationsOpen) NotificationsDialog(notices, onDismiss = { notificationsOpen = false }, onRead = { notice ->
            work { api.markRead(notice.id); notices = notices.map { if (it.id == notice.id) it.copy(read = true) else it } }
            when (notice.linkType.uppercase()) {
                "EVENT", "EVENT_ID", "EVENT_DETAIL" -> notice.linkValue.toIntOrNull()?.let { id ->
                    eventById[id]?.let { notificationsOpen = false; showEvent(it) }
                }
                "TICKETS", "TICKET" -> { notificationsOpen = false; tab = "Entrades" }
                "COMMUNITY" -> { notificationsOpen = false; tab = "Comunitat" }
                "ARTIST" -> { if (notice.linkValue.isNotBlank()) {
                    notificationsOpen = false; artistSourceEvent = null; artist = notice.linkValue
                } }
            }
        }, reminders = events.filter { it.id in user.alerts && (it.endDate.ifBlank { it.date }.isBlank() || it.endDate.ifBlank { it.date } >= today) },
            onReminderClick = { ev -> notificationsOpen = false; showEvent(ev) },
            onReminderDisable = { ev -> notificationsOpen = false; alertToDisable = ev },
            reminderTime = { ev -> account?.id?.let { id -> platformReadPublicCache(iosReminderKey(id,ev.id)) }.orEmpty() })
        if (batecStoryKey != null && followedStories.isNotEmpty()) {
            BatecViewer(followedStories, batecStoryKey!!,
                eventFor = { id -> eventById[id] },
                onDismiss = { batecStoryKey = null },
                onOpenEvent = { id ->
                    batecStoryKey = null
                    eventById[id]?.let { showEvent(it) }
                },
                onStoryChanged = { story -> batecStoryKey = story.storyKey })
            // Each opened story is marked viewed once. Account-scoped server state is authoritative.
            LaunchedEffect(batecStoryKey, account?.id) {
                val key = batecStoryKey
                if (key != null && account != null && key !in seenBatecs) {
                    val expectedId = account?.id
                    runCatching { api.markBatecSeen(key) }.onSuccess {
                        if (account?.id == expectedId) seenBatecs = seenBatecs + key
                    }
                }
            }
        }
        selected?.let { ev -> EventDetailSheet(ev, user.saved.contains(ev.id), user.alerts.contains(ev.id),
            matchedArtist = festivalMatchedArtist,
            followsEntity = { type, name -> user.followsType(type, name) },
            onToggleFollowEntity = { type, name -> toggleFollowEntity(type, name) },
            onDismiss = { selected = null; festivalMatchedArtist = "" },
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
                onDismiss = { artist = null; artistSourceEvent = null },
                onEvent = { artist = null; artistSourceEvent = null; showEvent(it) })
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
        if (validatorOpen) ProfessionalValidationDialog(api, onDismiss = { validatorOpen = false })
        ticketEvent?.let { event -> TicketOptionsDialog(event, api,
            onDismiss = { ticketEvent = null }, onPurchased = {
                ticketEvent = null; status = "Entrades gratuïtes emeses correctament"; tab = "Entrades"
                work { tickets = api.tickets() }
            }, onError = { error = it }) }
        groupCreateFor?.let { event -> CreateGroupDialog(event, api, onDismiss = { groupCreateFor = null },
            onCreated = { groupCreateFor = null; status = "Tauló creat"; work { groups = api.community(event.id) } },
            onError = { error = it }) }
        communityGroup?.let { g -> ChatDialog(g, api, isCreator = g.creatorUserId > 0 && g.creatorUserId == account?.id,
            onDismiss = { communityGroup = null }, work = ::work) }
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
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text("Publicitat · $label", fontSize = 11.sp, color = Color.DarkGray,
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 4.dp))
        LazyRow(state = state, contentPadding = PaddingValues(horizontal = 15.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(ads, key = { "${it.placement}_${it.id}" }) { ad ->
                val event = eventById[ad.eventId]
                LaunchedEffect(ad.id, ad.placement) { onViewed(ad) }
                Surface(shape = RoundedCornerShape(15.dp), color = Color.White,
                    tonalElevation = 1.dp, shadowElevation = 1.dp,
                    modifier = Modifier.width(if (header) 308.dp else 270.dp)
                        .clickable(enabled = event != null) { event?.let { onOpened(ad, it) } }) {
                    Column {
                        SponsoredPoster(ad, event?.posterUrl.orEmpty(),
                            Modifier.fillMaxWidth().height(if (header) 134.dp else 120.dp))
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Text(event?.title ?: "Activitat patrocinada", fontWeight = FontWeight.SemiBold,
                                maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                            if (event != null) {
                                Text("${event.date} · ${event.town}", maxLines = 1,
                                    fontSize = 11.sp, color = Color.DarkGray)
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
                      savedIds: Set<Int>, onClick: (AgendaEventSummary) -> Unit,
                      onSave: (AgendaEventSummary) -> Unit, ads: List<IosAd>,
                      protectedIds: Set<Int>, commercialEvents: Map<Int, AgendaEventSummary>,
                      onAdViewed: (IosAd) -> Unit,
                      onAdOpened: (IosAd, AgendaEventSummary) -> Unit,
                      refreshing: Boolean, onRefresh: () -> Unit,
                      onExclude: (AgendaEventSummary, String) -> Unit,
                      onReport: (AgendaEventSummary) -> Unit) {
    val listState = rememberLazyListState()
    val headerAds = remember(ads) { ads.filter { it.placement == "HEADER" } }
    val premiumAds = remember(ads) { ads.filter { it.placement == "FEED_PREMIUM" } }
    val normalAds = remember(ads) { ads.filter { it.placement == "FEED_NORMAL" } }
    val premium = premiumAds.isNotEmpty()
    val normal = normalAds.isNotEmpty()
    // Use the FULL validated inventory, not just the currently paginated organic rows.
    // Otherwise paid ads with distant events would be impossible to open.
    val eventById = commercialEvents
    val advertisedEventIds = remember(ads) { ads.mapTo(HashSet()) { it.eventId } }
    val nearBottom by remember(listState, events.size) { derivedStateOf {
        val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        last >= events.size - 3 && last >= 0
    } }
    LaunchedEffect(nearBottom, events.size, total) { if (nearBottom && events.size < total) onLoadMore() }
    PullToRefreshBox(isRefreshing = refreshing, onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(), indicator = {}) {
        LazyColumn(state = listState, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 5.dp)) {
            if (headerAds.isNotEmpty()) item(key = "ad_header", contentType = "sponsored") {
                SponsoredCarousel("Capçalera", headerAds, eventById, onAdViewed, onAdOpened, header = true)
            }
            itemsIndexed(events, key = { _, ev -> ev.key }, contentType = { _, _ -> "event" }) { index, ev ->
                Column {
                    if (GlobalAdPlacementPolicy.premiumAt(index, premium, normal)) {
                        SponsoredCarousel("Premium", premiumAds, eventById, onAdViewed, onAdOpened)
                    }
                    if (GlobalAdPlacementPolicy.normalAt(index, premium, normal)) {
                        SponsoredCarousel("Normal", normalAds, eventById, onAdViewed, onAdOpened)
                    }
                    val isCommercial = ev.id in protectedIds || ev.ticketAvailable || ev.id in advertisedEventIds
                    Box(Modifier.padding(horizontal = 15.dp)) {
                        EventCard(ev, ev.id in savedIds, onClick = { onClick(ev) }, trailing = {
                            var menuVisible by remember(ev.key) { mutableStateOf(false) }
                            TextButton(onClick = { onSave(ev) }) { Text(if (ev.id in savedIds) "▣" else "▢", fontSize = 22.sp) }
                            Box {
                                TextButton(onClick = { menuVisible = true }) { Text("⋮", fontSize = 23.sp) }
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
            if (events.size < total) item(key = "load_more") {
                TextButton(onClick = onLoadMore, modifier = Modifier.fillMaxWidth()) { Text("Mostrar més") }
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
private fun LoginDialog(onDismiss: () -> Unit, onRegister: () -> Unit,
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
        mutableStateOf(if (ev.isFestival) "Horaris" else "Info")
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
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White,
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f)) {
            Column {
                Box(Modifier.fillMaxWidth().height(170.dp)) {
                    Poster(ev.posterUrl, Modifier.fillMaxSize())
                    TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd)) {
                        Text("✕", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    if (ev.posterUrl.isNotBlank()) {
                        TextButton(onClick = { showPoster = true },
                            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.48f), RoundedCornerShape(18.dp))) {
                            Text("▣  Cartell", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
                Column(Modifier.padding(horizontal = 13.dp, vertical = 7.dp)) {
                    Text(ev.title, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(listOf(ev.date, ev.time, ev.town, ev.venue).filter(String::isNotBlank).joinToString(" · "),
                        fontSize = 12.sp, color = Color.Gray)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onSave) { Text(if (saved) "▣ Guardat" else "▢ Guardar") }
                        TextButton(onClick = onReminder) { Text(if (reminded) "♧ Avís actiu" else "♧ Recordatori") }
                        if (ev.isFestival && ev.ticketAvailable) {
                            TextButton(onClick = onTickets) { Text("🎟 Entrades") }
                        }
                    }
                    // Four fixed destinations, with a separate ticket CTA for festivals.
                    val tabs = if (ev.isFestival) listOf("Horaris", "Info", "Ubicació", "Comunitat")
                        else listOf("Info", "Entrades", "Ubicació", "Comunitat")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        tabs.forEach { name ->
                            TextButton(onClick = { section = name }, modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 6.dp)) {
                                Text(name, fontSize = 10.sp,
                                    fontWeight = if (section == name) FontWeight.Bold else FontWeight.Normal,
                                    color = if (section == name) Ink else Color.DarkGray, maxLines = 1)
                            }
                        }
                    }
                    HorizontalDivider()
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                    when (section) {
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
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End).padding(7.dp)) { Text("Tancar") }
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
                         onFollow: () -> Unit, onDismiss: () -> Unit, onEvent: (AgendaEventSummary) -> Unit) {
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
                    TextButton(onClick = onDismiss) { Text("Tancar") }
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

/** Full story navigation. Never invents extra Batecs or turns stories into event cards. */
@Composable
private fun BatecViewer(stories: List<IosBatec>, selectedKey: String,
                        eventFor: (Int) -> AgendaEventSummary?,
                        onDismiss: () -> Unit, onOpenEvent: (Int) -> Unit,
                        onStoryChanged: (IosBatec) -> Unit) {
    val selectedIndex = stories.indexOfFirst { it.storyKey == selectedKey }.coerceAtLeast(0)
    val story = stories[selectedIndex]
    fun go(offset: Int) {
        val next = IosBatecNavigationPolicy.move(selectedIndex, stories.size, offset)
        if (next == null) onDismiss() else onStoryChanged(stories[next])
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFF1F1D24),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.87f)) {
            var horizontalDrag by remember(story.storyKey) { mutableFloatStateOf(0f) }
            Column(Modifier.fillMaxSize().pointerInput(story.storyKey) {
                detectHorizontalDragGestures(
                    onDragStart = { horizontalDrag = 0f },
                    onDragEnd = {
                        when {
                            horizontalDrag < -55f -> go(1)
                            horizontalDrag > 55f -> go(-1)
                        }
                        horizontalDrag = 0f
                    },
                    onHorizontalDrag = { change, delta -> horizontalDrag += delta; change.consume() }
                )
            }) {
                Row(Modifier.fillMaxWidth().padding(11.dp), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("${selectedIndex + 1} / ${stories.size}", color = Color.White, fontSize = 13.sp)
                    Text(story.artist, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
                    TextButton(onClick = onDismiss) { Text("✕", color = Color.White) }
                }
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    Poster(story.imageUrl.ifBlank { eventFor(story.eventId)?.posterUrl.orEmpty() }, Modifier.fillMaxSize())
                }
                Column(Modifier.padding(12.dp)) {
                    Text(story.eventTitle.ifBlank { eventFor(story.eventId)?.title.orEmpty() },
                        color = Color.White, fontWeight = FontWeight.Bold, maxLines = 2)
                    if (story.text.isNotBlank()) Text(story.text, color = Color.White, fontSize = 13.sp, maxLines = 3)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { go(-1) }, enabled = selectedIndex > 0) { Text("← Anterior") }
                        if (eventFor(story.eventId) != null) {
                            TextButton(onClick = { onOpenEvent(story.eventId) }) { Text("Activitat") }
                        }
                        TextButton(onClick = { go(1) }) { Text(if (selectedIndex == stories.lastIndex) "Tancar" else "Següent →") }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationsDialog(notifications: List<IosNotification>, reminders: List<AgendaEventSummary>,
                                onDismiss: () -> Unit, onRead: (IosNotification) -> Unit,
                                onReminderClick: (AgendaEventSummary) -> Unit,
                                onReminderDisable: (AgendaEventSummary) -> Unit,
                                reminderTime: (AgendaEventSummary) -> String) {
    var view by remember { mutableStateOf(IosNotificationGrouping.GENERAL) }
    val tabs = listOf("General" to "♧", "Pròxim" to "◷", "Nous" to "✧", "Entrades" to "▤", "Suggerències" to "✦", "Comunitat" to "♙")
    val grouped = remember(notifications) { notifications.groupBy { IosNotificationGrouping.fromDeepLinkType(it.linkType) } }
    val shown = grouped[view].orEmpty()
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f)) {
            Column(Modifier.padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Notificacions", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onDismiss) { Text("✕") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    tabs.forEach { (name, symbol) ->
                        val unread = grouped[name].orEmpty().count { !it.read }
                        Column(Modifier.weight(1f).clickable { view = name }.background(if (view == name) Accent else Color.White,
                            RoundedCornerShape(7.dp)).padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(symbol, fontSize = 20.sp)
                            Text(name.take(4), fontSize = 9.sp)
                            if (unread > 0) Text(unread.toString(), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                TextButton(onClick = { view = "Recordatoris" }) { Text("♧ Recordatoris actius (${reminders.size})") }
                HorizontalDivider()
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    if (view == "Recordatoris") {
                        items(reminders, key = { it.key }) { ev ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Poster(ev.posterUrl, Modifier.size(63.dp).clip(RoundedCornerShape(8.dp)))
                                Column(Modifier.weight(1f).clickable { onReminderClick(ev) }.padding(8.dp)) {
                                    Text(ev.title, fontWeight = FontWeight.Bold)
                                    Text(ev.date, fontSize = 12.sp)
                                    val whenScheduled = reminderTime(ev)
                                    if (whenScheduled.isNotBlank()) {
                                        Text("Avís: ${whenScheduled.replace('T',' ')}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                                TextButton(onClick = { onReminderDisable(ev) }) { Text("♧", fontSize = 24.sp) }
                            }
                        }
                        if (reminders.isEmpty()) item { EmptyState("No tens recordatoris actius.") }
                    } else {
                        items(shown, key = { it.id }) { notice ->
                            Column(Modifier.fillMaxWidth().clickable { onRead(notice) }.padding(8.dp)) {
                                Text(notice.title + if (!notice.read) " •" else "", fontWeight = FontWeight.Bold)
                                Text(notice.body, fontSize = 13.sp)
                                Text(notice.createdAt, fontSize = 10.sp, color = Color.Gray)
                                HorizontalDivider()
                            }
                        }
                        if (shown.isEmpty()) item { EmptyState("No hi ha avisos en esta categoria.") }
                    }
                }
                // Only mark messages displayed in the selected category, never hidden categories.
                if (view != "Recordatoris" && shown.any { !it.read }) {
                    TextButton(onClick = { shown.filterNot { it.read }.forEach(onRead) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Marcar esta categoria com a llegida")
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
    var busy by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Crear tauló") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(event.title, fontSize = 13.sp)
            OutlinedTextField(title, { title = it.take(160) }, label = { Text("Nom del tauló") })
            Text("Tipus de tauló", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            listOf("CAR" to "Cotxe compartit", "TRANSPORT" to "Transport organitzat",
                "MEETUP" to "Quedades", "OTHER" to "Altres (inclou allotjament)").forEach { (key, label) ->
                Row(Modifier.fillMaxWidth().clickable { kind = key }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = kind == key, onClick = { kind = key })
                    Text(label, fontSize = 12.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(open, onCheckedChange = { open = it }); Text("Obert (sense aprovació)", fontSize = 12.sp)
            }
        } },
        confirmButton = { TextButton(enabled = !busy && title.isNotBlank(), onClick = { busy = true; scope.launch {
            try { api.createCommunityGroup(event.id, title, kind, open); onCreated() }
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
