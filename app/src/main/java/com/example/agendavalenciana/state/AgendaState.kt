package com.example.agendavalenciana.state

import com.example.agendavalenciana.ui.ticketDraftItems
import com.example.agendavalenciana.model.AgendaDateFormat
import com.example.agendavalenciana.model.CategoryTaxonomy
import com.example.agendavalenciana.model.CulturalSubcategory

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.agendavalenciana.data.FestivalFeedNextDay
import com.example.agendavalenciana.data.EventReminderScheduler
import com.example.agendavalenciana.data.AgendaDedupe
import com.example.agendavalenciana.data.DemoData
import com.example.agendavalenciana.data.Municipality
import com.example.agendavalenciana.data.MunicipalityRepository
import com.example.agendavalenciana.data.GeoDistance
import com.example.agendavalenciana.data.RemoteAgendaRepository
import com.example.agendavalenciana.data.ServerApiRepository
import com.example.agendavalenciana.data.StyleAffinityCatalog
import com.example.agendavalenciana.data.TicketQrCache
import com.example.agendavalenciana.model.AccountProfile
import com.example.agendavalenciana.model.AccountRole
import com.example.agendavalenciana.model.AdPlacement
import com.example.agendavalenciana.model.BellFollowedFrequency
import com.example.agendavalenciana.model.AlertPreferences
import com.example.agendavalenciana.model.AgendaNotification
import com.example.agendavalenciana.model.NotificationType
import com.example.agendavalenciana.model.AppAccount
import com.example.agendavalenciana.model.Concert
import com.example.agendavalenciana.model.CulturalCategory
import com.example.agendavalenciana.model.EmailDigestFrequency
import com.example.agendavalenciana.model.EmailPreferences
import com.example.agendavalenciana.model.ConcertViewMode
import com.example.agendavalenciana.model.AttendanceStatus
import com.example.agendavalenciana.model.CommunityAccessMode
import com.example.agendavalenciana.model.CommunityGroup
import com.example.agendavalenciana.model.PersonalSavedList
import com.example.agendavalenciana.model.FriendsCircle
import com.example.agendavalenciana.model.FriendsCircleMessage
import com.example.agendavalenciana.model.FriendsCircleNotice
import com.example.agendavalenciana.model.CommunityGroupType
import com.example.agendavalenciana.model.CommunityIdentity
import com.example.agendavalenciana.model.CommunityMemberRequest
import com.example.agendavalenciana.model.ChatMessage
import com.example.agendavalenciana.model.ConcertComment
import com.example.agendavalenciana.model.FeedMode
import com.example.agendavalenciana.model.FeedSearchSuggestion
import com.example.agendavalenciana.model.FeedSearchSuggestionType
import com.example.agendavalenciana.model.FollowSuggestion
import com.example.agendavalenciana.model.FollowSuggestionPaging
import com.example.agendavalenciana.model.FollowSuggestionType
import com.example.agendavalenciana.model.EventSubmission
import com.example.agendavalenciana.model.EventAdvertisement
import com.example.agendavalenciana.model.EventSubmissionStatus
import com.example.agendavalenciana.model.ArtistAnalytics
import com.example.agendavalenciana.model.ArtistPublicProfile
import com.example.agendavalenciana.model.CommunityChatMessage
import com.example.agendavalenciana.model.Filters
import com.example.agendavalenciana.model.SavedFilterPreset
import com.example.agendavalenciana.model.GroupPost
import com.example.agendavalenciana.model.Story
import com.example.agendavalenciana.model.UserPreferences
import com.example.agendavalenciana.model.ForYouHundredPolicy
import com.example.agendavalenciana.model.ForYouDiagnostics
import com.example.agendavalenciana.BuildConfig
import com.example.agendavalenciana.model.ForYouSnapshotReusePolicy
import com.example.agendavalenciana.model.ForYouPublicationPolicy
import com.example.agendavalenciana.model.AgendaReservationPolicy
import com.example.agendavalenciana.model.DiscoveryLevel
import com.example.agendavalenciana.model.PersonalizationOptions
import com.example.agendavalenciana.model.UserVisibilityRules
import com.example.agendavalenciana.model.AdTargetingDefaults
import com.example.agendavalenciana.model.BehaviourKind
import com.example.agendavalenciana.model.UserBehaviourEvent
import com.example.agendavalenciana.model.UserBehaviourProfile
import com.example.agendavalenciana.model.TicketStatus
import com.example.agendavalenciana.model.PurchasedTicket
import com.example.agendavalenciana.model.TicketOption
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt
import kotlin.random.Random
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONArray
import org.json.JSONObject

class AgendaState(context: Context) {
    private val appContext = context.applicationContext
    private val stateStartedAt = SystemClock.elapsedRealtime()
    private val store = context.getSharedPreferences("agenda_valenciana", Context.MODE_PRIVATE)
    // Only the previous session owner may migrate the old, app-global taste switches.
    private val legacyFeedPrefsOwner = store.getString("session_username", null)
    // 10az: totes les tasques I/O de l’estat compartixen un pool acotat.
    // Evita crear desenes de fils nous durant scroll, likes, guardats i sincronitzacions.
    private val backgroundExecutor = Executors.newFixedThreadPool(4)
    // 10eo: el feed té cua pròpia. Xarxa, reports, socials o notificacions no poden
    // deixar la primera càrrega esperant darrere d'una petició lenta.
    private val feedExecutor = Executors.newSingleThreadExecutor()
    // v328: la lectura de 8 IDs prèviament seleccionats no espera que es parsege
    // tota la finestra de dos mesos. És un preview condicional, mai un rànquing final.
    private val startupPreviewExecutor = Executors.newSingleThreadExecutor()
    // 10ep: carregar el catàleg complet és una operació diferent de preparar el feed visible.
    // No pot ocupar la cua que calcula els 5/20 primers cards.
    private val catalogExecutor = Executors.newSingleThreadExecutor()
    // v333: la primera pantalla de Sense filtre té cua pròpia. No espera el
    // parseig global ni els snapshots de Per a tu a l'arrancada.
    private val unfilteredPreviewExecutor = Executors.newSingleThreadExecutor()
    // El rànquing complet de Per a tu també té cua pròpia: un recàlcul costós no deixa
    // bloquejades les lectures SQLite ni una cerca que l'usuari acaba de fer.
    private val personalizationExecutor = Executors.newSingleThreadExecutor()
    // El cercador té un fil propi per preparar els índexs sense competir amb xarxa,
    // sincronitzacions, likes o notificacions del pool general.
    private val searchIndexExecutor = Executors.newSingleThreadExecutor()

    // 10ep: normalitzar text és una operació molt repetida en feed, Batecs i cerca.
    // Les Regex es compilen una sola vegada i els valors curts repetits es memoritzen.
    private val normalizeMarksRegex = Regex("\\p{M}+")
    private val normalizeApostropheRegex = Regex("[’‘`´]")
    private val normalizeDashRegex = Regex("[‐‑‒–—−]")
    private val normalizeWhitespaceRegex = Regex("\\s+")
    private val searchNormalizationCache = ConcurrentHashMap<String, String>()
    private val searchNormalizationCacheLimit = 12_000

    // El dia cultural de l’Agenda canvia a les 05:00, no a mitjanit.
    // Això manté visibles els esdeveniments nocturns fins al final real de la jornada.
    private val agendaZone: ZoneId = ZoneId.of("Europe/Madrid")

    fun agendaToday(): LocalDate {
        val now = ZonedDateTime.now(agendaZone)
        return if (now.toLocalTime().isBefore(LocalTime.of(5, 0))) {
            now.toLocalDate().minusDays(1)
        } else {
            now.toLocalDate()
        }
    }

    fun millisUntilNextAgendaDayBoundary(): Long {
        val now = ZonedDateTime.now(agendaZone)
        var next = now.toLocalDate().atTime(5, 0).atZone(agendaZone)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next).toMillis().coerceAtLeast(1_000L)
    }

    fun refreshAgendaDayBoundary() {
        cachedVisibleConcertsKey = null
        calculatingVisibleConcertsKey = null
        feedRenderRevision += 1L
        scheduleVisibleConcertsRefresh()
        invalidateNotificationCache(0L)
    }

    private val affinityBatchLock = Any()
    private val pendingAffinityBatch = mutableListOf<JSONObject>()
    private var affinityBatchScheduled = false
    private var guestSyncScheduled = false
    private var pendingGuestMerge: JSONObject? = null
    @Volatile private var socialCountsRefreshInFlight = false
    private var pendingSocialCountIds: Set<Int> = emptySet()
    // Comptadors socials desacoblats del model complet del concert. Actualitzar likes/saves
    // no reconstrueix concerts, remoteConcerts ni el snapshot del feed.
    private data class SocialUiCounts(val saved: Int, val liked: Int)
    private var socialCountsByEvent by mutableStateOf<Map<Int, SocialUiCounts>>(emptyMap())
    // Protecció contra respostes de xarxa/polling antigues després d'un toc.
    private val savedMutationVersions = mutableMapOf<Int, Int>()
    private val likedMutationVersions = mutableMapOf<Int, Int>()
    private val savedOptimisticCounts = mutableMapOf<Int, Int>()
    private val likedOptimisticCounts = mutableMapOf<Int, Int>()
    private val savedOptimisticAt = mutableMapOf<Int, Long>()
    private val likedOptimisticAt = mutableMapOf<Int, Long>()
    private val socialActionsExecutor = Executors.newSingleThreadExecutor()

    private fun mergeRemoteCount(id: Int, remote: Int, isSaved: Boolean): Int {
        val optimistic = if (isSaved) savedOptimisticCounts else likedOptimisticCounts
        val timestamps = if (isSaved) savedOptimisticAt else likedOptimisticAt
        val current = optimistic[id] ?: return remote.coerceAtLeast(0)
        // Límit temporal per no tapar indefinidament els nous vots d'altres usuaris.
        if (SystemClock.elapsedRealtime() - (timestamps[id] ?: 0L) > 90_000L) {
            optimistic.remove(id)
            timestamps.remove(id)
            return remote.coerceAtLeast(0)
        }
        val selected = if (isSaved) id in savedConcerts else id in likedConcerts
        // Si el servidor encara no reflectix el nostre últim toc, el número no retrocedix.
        val confirmed = if (selected) remote >= current else remote <= current
        if (confirmed) {
            optimistic.remove(id)
            timestamps.remove(id)
            return remote.coerceAtLeast(0)
        }
        return current
    }

    var municipalities by mutableStateOf<List<Municipality>>(emptyList())
        private set

    var municipalitySyncError by mutableStateOf<String?>(null)
        private set

    // v277: el cens entra en segon pla. La seua arribada ha de refrescar tant
    // l'índex del cercador com el desplegable encara que el feed no canvie.
    @Volatile private var municipalityCatalogRevision: Long = 0L

    private fun applyMunicipalityCatalog(items: List<Municipality>) {
        if (items.isEmpty()) return
        municipalities = items
        municipalityCatalogRevision += 1L
        cachedSuggestionPoolRevision = -1L
        cachedSuggestionPoolMunicipalityRevision = -1L
        searchSuggestionRevision += 1L
        if (userPreferences.preferredLocations.isNotEmpty() ||
            userPreferences.preferredComarques.isNotEmpty()) invalidateFeedCache()
    }

    var currentAccount by mutableStateOf(loadSession())
        private set

    var isLoggingIn by mutableStateOf(false)
        private set

    var specialEventsRemote by mutableStateOf<Map<Int, ServerApiRepository.RemoteSpecialEvent>>(emptyMap())
        private set
    var specialEventManageRemote by mutableStateOf<Map<Int, ServerApiRepository.RemoteSpecialManageConfig>>(emptyMap())
        private set
    var specialAccessibleEvents by mutableStateOf<List<ServerApiRepository.RemoteSpecialEventSummary>>(emptyList())
        private set
    var loginError by mutableStateOf<String?>(null)
        private set
    var isPublishingEvent by mutableStateOf(false)
        private set
    var eventPublishError by mutableStateOf<String?>(null)
        private set

    var isBulkImporting by mutableStateOf(false)
        private set
    var bulkImportProgress by mutableStateOf(0f)
        private set
    var bulkImportMessage by mutableStateOf<String?>(null)
        private set
    var isBulkExporting by mutableStateOf(false)
        private set
    var bulkExportMessage by mutableStateOf<String?>(null)
        private set
    var isEntityImageImporting by mutableStateOf(false)
        private set
    var isEntityImageExporting by mutableStateOf(false)
        private set
    var entityImageMessage by mutableStateOf<String?>(null)
        private set
    var isProfessionalProfileImporting by mutableStateOf(false)
        private set
    var professionalProfileMessage by mutableStateOf<String?>(null)
        private set
    var isPurchasingTicket by mutableStateOf(false)
        private set
    var ticketOperationError by mutableStateOf<String?>(null)
        private set
    var ticketOptionsByEvent by mutableStateOf<Map<Int, List<TicketOption>>>(emptyMap())
        private set
    var ticketCapacityByEvent by mutableStateOf<Map<Int, Int?>>(emptyMap())
        private set

    var isPurchasingAdvertisement by mutableStateOf(false)
        private set
    var advertisementOperationError by mutableStateOf<String?>(null)
        private set

    var isPublishingBatec by mutableStateOf(false)
        private set
    var batecOperationError by mutableStateOf<String?>(null)
        private set

    var search by mutableStateOf("")
    var filters by mutableStateOf(Filters())
    var savedFilterPresets by mutableStateOf(if (currentAccount == null) emptyList() else loadSavedFilterPresets())
        private set
    var savedFilterSyncError by mutableStateOf<String?>(null)
        private set
    var isFilterPresetSaving by mutableStateOf(false)
        private set
    private val filterPresetsExecutor = Executors.newSingleThreadExecutor()

    var activeFilterPresetName by mutableStateOf<String?>(null)
        private set
    var feedMode by mutableStateOf(FeedMode.UNFILTERED)
    var selectedFeedCategory by mutableStateOf<CulturalCategory?>(null)
        private set
    private var feedModeSelectionToken: Long = 0L
    private val feedModeResetMillis = 30L * 60L * 1000L
    // 10cx: mentre l'usuari no trie manualment una vista, Inici usa un mode per defecte
    // sensible a si ja existeixen senyals reals de personalització.
    private var feedModeUsesAutomaticDefault: Boolean = true
    // Els filtres/cercador són una consulta temporal sobre TOTA l'Agenda, mai
    // sobre el rànquing de «Per a tu». Quan es lleva l'últim criteri, tornem
    // automàticament a «Per a tu» (llevat que l'usuari trie un mode manual).
    private var feedModeScopedByCriteria: Boolean = false
    // La vista del feed és només visual. Les antigues opcions Llista/Mapa
    // ja no formen part de la navegació i qualsevol preferència antiga s'ignora.
    var concertViewMode by mutableStateOf(ConcertViewMode.VISUAL)
        private set

    var eventSubmissions by mutableStateOf<List<EventSubmission>>(emptyList())
        private set

    var advertisements by mutableStateOf<List<EventAdvertisement>>(emptyList())
        private set
    // Cache separat dels concerts anunciats: la publicitat no depén que l'esdeveniment
    // estiga dins de la finestra de 2 mesos de Per a tu.
    private var advertisementConcertCache by mutableStateOf<Map<Int, Concert>>(emptyMap())
    // v250: fitxes que el servidor marca com a garantides per anunci pagat vigent
    // o per una venda d'entrades confirmada en la plataforma. No són afinitats personals.
    private var priorityFeedConcertCache by mutableStateOf<Map<Int, Concert>>(emptyMap())
    private var priorityFeedEventIds by mutableStateOf<Set<Int>>(emptySet())


    // DIAGNÒSTIC v280: traça real retornada pel selector; només la mostra la UI debug.
    var forYouSelectionExplanations by mutableStateOf<Map<Int, String>>(emptyMap())
        private set

    var recentSearchTerms by mutableStateOf<Set<String>>(emptySet())
        private set

    var behaviourProfile by mutableStateOf(UserBehaviourProfile(currentAccount?.username.orEmpty()))
        private set

    // v266: estat del reinici selectiu de les afinitats implícites.
    var learningResetFeedback by mutableStateOf<String?>(null)
        private set
    var learningResetSyncPending by mutableStateOf(false)
        private set
    @Volatile private var learningResetGeneration: Long = 0L

    var affinityAnalytics by mutableStateOf<ServerApiRepository.AffinityAnalytics?>(null)
        private set
    var affinityAnalyticsLoading by mutableStateOf(false)
        private set
    var affinityAnalyticsError by mutableStateOf<String?>(null)
        private set

    // 10br: onboarding ordenat per seguiments/preferències agregades del servidor.
    // Si el rànquing encara no està disponible, les funcions de candidats usen
    // activitat real de l'agenda com a desempat; mai l'ordre cru de la llista.
    var onboardingRankings by mutableStateOf(ServerApiRepository.OnboardingRankings())
        private set
    @Volatile private var onboardingRankingsLoading = false

    fun refreshOnboardingRankings(force: Boolean = false) {
        if (onboardingRankingsLoading) return
        if (!force && (onboardingRankings.artists.isNotEmpty() || onboardingRankings.towns.isNotEmpty())) return
        onboardingRankingsLoading = true
        backgroundExecutor.execute {
            // El registre ha de conéixer totes les entitats ARTIST sincronitzades,
            // encara que no tinguen concerts publicats en l'agenda actual.
            runCatching { ServerApiRepository.warmEntityImageCache(appContext) }
            runCatching { ServerApiRepository.cachedEntityNames(appContext, "ARTIST") }
            runCatching { ServerApiRepository.cachedArtistAffinityMetadata(appContext) }
            val loaded = ServerApiRepository.fetchOnboardingRankings().getOrNull()
            Handler(Looper.getMainLooper()).post {
                if (loaded != null) onboardingRankings = loaded
                onboardingRankingsLoading = false
                cachedSuggestionPoolRevision = -1L
                cachedKnownArtistRevision = -1L
                cachedFeedSearchIndex = null
                warmSearchIndexesAsync()
                entityImageCatalogRevision += 1L
                searchSuggestionRevision += 1L
            }
        }
    }

    private fun onboardingRank(map: Map<String, Int>, value: String): Int =
        map[value.trim().lowercase()] ?: 0

    fun onboardingCategoryChoices(): List<CulturalCategory> {
        val activity = concerts.groupingBy { it.category }.eachCount()
        return CulturalCategory.entries
            .filter { it != CulturalCategory.OTHER }
            .sortedWith(
                compareByDescending<CulturalCategory> { onboardingRank(onboardingRankings.categories, it.label) }
                    .thenByDescending { activity[it] ?: 0 }
                    .thenBy { it.label }
            )
    }

    fun onboardingArtistChoices(
        categories: Set<CulturalCategory>,
        excluded: Set<String> = emptySet(),
        query: String = "",
        limit: Int = 80
    ): List<String> {
        val q = normalizeSearchValue(query)
        val excludedKeys = excluded.asSequence().map(::normalizeSearchValue).filter(String::isNotBlank).toSet()
        val wantedCategories = categories.asSequence().map { canonicalCategoryKey(it.label) }.toSet()
        val entityMetadata = ServerApiRepository.cachedArtistAffinityMetadata(appContext)
        val activity = linkedMapOf<String, Int>()

        // Els concerts només donen pes d'activitat; no decideixen si l'entitat existeix.
        concerts.asSequence()
            .filter { categories.isEmpty() || it.category in categories }
            .forEach { concert ->
                (sequenceOf(concert.artist) + concert.lineup.asSequence())
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinctBy(AgendaDedupe::artistKey)
                    .forEach { artist -> activity[artist] = (activity[artist] ?: 0) + 1 }
            }

        // ENTITATS és la font canònica del selector. Inclou artistes sense concerts actuals.
        ServerApiRepository.cachedEntityNames(appContext, "ARTIST").forEach { artist ->
            val key = normalizeSearchValue(artist)
            val meta = entityMetadata[key]
            val categoryMatches = categories.isEmpty() || meta == null ||
                canonicalCategoryKey(meta.category) in wantedCategories
            if (categoryMatches) activity.putIfAbsent(artist, 0)
        }

        return activity.keys.asSequence()
            .filter { normalizeSearchValue(it) !in excludedKeys }
            .filter { q.isBlank() || normalizeSearchValue(it).contains(q) }
            .distinctBy(AgendaDedupe::artistKey)
            .sortedWith(
                compareByDescending<String> { onboardingRank(onboardingRankings.artists, it) }
                    .thenByDescending { activity[it] ?: 0 }
                    .thenBy { normalizeSearchValue(it) }
            )
            .take(limit.coerceAtLeast(1))
            .toList()
    }

    /** v297: propostes només de GRUPS MUSICALS reals, ordenades primer pel
     * nombre de concerts DISTINTS del catàleg actual. Les subcategories actuen
     * exclusivament com a desempat; la publicitat no dona punts extra.
     * Una busca explícita també troba grups musicals que encara no tenen concerts.
     */
    fun onboardingMusicGroupChoices(
        selectedSubcategoryCodes: Set<String> = emptySet(),
        query: String = "",
        limit: Int = 8
    ): List<String> {
        val metadata = ServerApiRepository.peekCachedArtistAffinityMetadata()
        val normalizedQuery = normalizeSearchValue(query)
        val concertIds = linkedMapOf<String, MutableSet<Int>>()
        val displayNames = linkedMapOf<String, String>()
        concerts.asSequence().filter { it.category == CulturalCategory.MUSIC }.forEach { concert ->
            val names = concert.lineup.filter(String::isNotBlank)
                .ifEmpty { listOf(concert.artist) }
            names.map(String::trim).filter(String::isNotBlank)
                .distinctBy(::normalizeSearchValue).forEach { name ->
                    val key = normalizeSearchValue(name)
                    displayNames.putIfAbsent(key, name)
                    concertIds.getOrPut(key) { linkedSetOf() }.add(concert.id)
                }
        }
        // Les entitats sense concerts completen, si cal, les huit propostes,
        // sempre darrere de tots els grups que sí que tenen concerts.
        metadata.values.asSequence().filter { it.type.equals("ARTIST", ignoreCase = true) }
            .filter { info ->
                canonicalCategoryKey(info.category) == canonicalCategoryKey(CulturalCategory.MUSIC.label) ||
                    (info.styles + info.subcategoryCodes).any { code ->
                        CategoryTaxonomy.findByCode(code)?.category == CulturalCategory.MUSIC
                    }
            }.forEach { info ->
                displayNames.putIfAbsent(normalizeSearchValue(info.name), info.name)
            }
        val musicStyles = selectedSubcategoryCodes.mapNotNull(CategoryTaxonomy::findByCode)
            .filter { it.category == CulturalCategory.MUSIC }.map { it.code }.toSet()
        return displayNames.entries.asSequence()
            .filter { (key, _) -> normalizedQuery.isBlank() || key.contains(normalizedQuery) }
            .sortedWith(
                compareByDescending<Map.Entry<String, String>> { (key, _) -> concertIds[key]?.size ?: 0 }
                    .thenByDescending { (key, _) ->
                        if (musicStyles.isEmpty()) 0 else metadata[key]?.let { info ->
                            (info.subcategoryCodes + info.styles)
                                .map(CategoryTaxonomy::canonicalizeCode)
                                .count(musicStyles::contains)
                        } ?: 0
                    }
                    .thenByDescending { (_, name) -> onboardingRank(onboardingRankings.artists, name) }
                    .thenBy { it.key }
            )
            .map { it.value }
            .take(limit.coerceAtLeast(1))
            .toList()
    }

    /** Fotos de subcategories: retrats de l'ENTITAT ARTIST que tinga aquell
     * `subcategoria_code`, amb fallback a la imatge pròpia d'un artista de
     * l'esdeveniment classificat. Mai usem el cartell genèric d'un concert.
     */
    fun onboardingSubcategoryArtistPhotos(): Map<String, String> {
        val metadata = ServerApiRepository.peekCachedArtistAffinityMetadata()
        val concertsPerArtist = linkedMapOf<String, MutableSet<Int>>()
        concerts.forEach { concert ->
            concert.lineup.filter(String::isNotBlank).ifEmpty { listOf(concert.artist) }
                .map(String::trim).filter(String::isNotBlank)
                .distinctBy(::normalizeSearchValue).forEach { name ->
                    concertsPerArtist.getOrPut(normalizeSearchValue(name)) { linkedSetOf() }.add(concert.id)
                }
        }
        // Per cada subcategoria, oferim les fotografies reals dels seus artistes.
        val candidates = linkedMapOf<String, MutableMap<String, String>>()
        fun propose(code: String, artist: String, photo: String?) {
            val exact = CategoryTaxonomy.findByCode(code)?.code ?: return
            val image = photo?.trim()?.takeIf(String::isNotBlank) ?: return
            val key = normalizeSearchValue(artist)
            if (key.isNotBlank()) candidates.getOrPut(exact) { linkedMapOf() }.putIfAbsent(key, image)
        }
        metadata.values.asSequence().filter { it.type.equals("ARTIST", ignoreCase = true) }
            .forEach { info ->
                val photo = publicPhotoForArtist(info.name)
                (info.subcategoryCodes + info.styles).forEach { code -> propose(code, info.name, photo) }
            }
        // Suport per a entitats encara sense els codis carregats en ENTITATS,
        // però vinculades a un concert classificat explícitament.
        concerts.forEach { concert ->
            val codes = (concert.subcategoryCodes + concert.styles)
                .mapNotNull(CategoryTaxonomy::findByCode)
                .filter { it.category == concert.category }.map { it.code }.distinct()
            if (codes.isEmpty()) return@forEach
            val names = concert.lineup.filter(String::isNotBlank)
                .ifEmpty { listOf(concert.artist) }
            names.forEach { artist ->
                val image = publicPhotoForArtist(artist)
                    ?: concert.artistImageUri?.takeIf {
                        normalizeSearchValue(artist) == normalizeSearchValue(concert.artist)
                    }
                codes.forEach { code -> propose(code, artist, image) }
            }
        }
        return candidates.mapValues { (_, photos) ->
            photos.entries.sortedWith(
                compareByDescending<Map.Entry<String, String>> {
                    concertsPerArtist[it.key]?.size ?: 0
                }.thenBy { it.key }
            ).first().value
        }
    }

    fun onboardingTownChoices(query: String = "", limit: Int = 80): List<Municipality> {
        val q = normalizeSearchValue(query)
        val activity = concerts.groupingBy { normalizeSearchValue(it.town) }.eachCount()
        return municipalityAutocompleteItems().asSequence()
            .filter { municipality ->
                q.isBlank() ||
                    normalizeSearchValue(municipality.name).contains(q) ||
                    normalizeSearchValue(municipality.comarca).contains(q) ||
                    normalizeSearchValue(municipality.officialName).contains(q) ||
                    municipality.otherNames.split('|', ';').any { normalizeSearchValue(it).contains(q) }
            }
            // Els pobles homònims de comarques distintes també han d'aparéixer.
            .distinctBy { it.code.ifBlank { "${normalizeSearchValue(it.name)}:${normalizeSearchValue(it.comarca)}" } }
            .sortedWith(
                compareByDescending<Municipality> { onboardingRank(onboardingRankings.towns, it.name) }
                    .thenByDescending { activity[normalizeSearchValue(it.name)] ?: 0 }
                    .thenBy { it.name.lowercase() }
            )
            .take(limit.coerceAtLeast(1))
            .toList()
    }

    // 0.18.0: mai fem I/O ni parseig de tota l'agenda durant la construcció de l'estat.
    // La cache entra en segon pla i el servidor continua sent la font de veritat.
    private var remoteConcerts: List<Concert> = emptyList()
    private var remoteConcertIds: Set<Int> = emptySet()
    // 10ai: durant els primers mil·lisegons podem mostrar només els 5 concerts
    // ja decidits per l'últim snapshot mentre la BD completa es parseja en background.
    // La finestra temporal inicial no és cap selecció limitada a 20 fitxes.
    // El catàleg complet es materialitza durant l'arrancada per poder calcular
    // només els esdeveniments personalitzats; anuncis independents.
    @Volatile private var fullAgendaLoadedForProcess: Boolean = false
    @Volatile private var fullAgendaLoadInFlight: Boolean = false
    /** La busca usa tot el catàleg local, no la finestra ni el snapshot de «Per a tu». */
    val isGlobalSearchCatalogLoading: Boolean
        get() {
            feedHydrationTick // lectura observable per a recomposicions Compose
            return (search.isNotBlank() || !filters.isEmpty()) && !fullAgendaLoadedForProcess &&
                (fullAgendaLoadInFlight || isAgendaRefreshing || ServerApiRepository.isFirstLoadCachePersisting())
        }

    val isGlobalSearchCatalogLoaded: Boolean
        get() {
            feedHydrationTick
            return fullAgendaLoadedForProcess
        }

    // «Per a tu» només pot validar/recalcular la seua caché quan té la finestra
    // COMPLETA de dos mesos i els senyals locals de l'usuari ja hidratats.
    @Volatile private var forYouWindowLoadedForProcess: Boolean = false
    @Volatile private var forYouSignalsReadyForProcess: Boolean = false
    private val forYouFastPreviewMaxAge = 30L * 60L * 1000L
    private var startupForYouPreview: List<Concert> = emptyList()
    private var startupForYouPreviewReady = false
    private var startupForYouPreviewAccount: String = ""
    private var startupForYouPreviewSignature: Int = Int.MIN_VALUE
    private var startupForYouPreviewDay: String = ""
    private var startupForYouPreviewDbRevision: Long = Long.MIN_VALUE
    private var startupForYouPreviewHardSignature: Int = Int.MIN_VALUE

    /** Mostra una selecció validada del compte abans de parsejar tota la finestra.
     * No la publica si han canviat preferències, dia, BD o compte.
     * En acabar la càrrega, visibleConcerts() la substituïx per la font completa.
     */
    private fun publishFastStartupForYouPreview() {
        if (!startupForYouPreviewReady || !forYouSignalsReadyForProcess ||
            feedMode != FeedMode.FOR_YOU || search.isNotBlank() || !filters.isEmpty() ||
            startupForYouPreviewAccount != (currentAccount?.username?.lowercase() ?: "guest") ||
            startupForYouPreviewDay != agendaToday().toString() ||
            startupForYouPreviewHardSignature != forYouHardPreferenceSignature() ||
            (cachedVisibleConcertsFinal && visibleConcertsState.isNotEmpty())
        ) return
        val shown = safeVisibleFeed(startupForYouPreview)
            .filterNot(::isFreeInternalAgendaReservation)
        if (shown.isEmpty()) return
        // Un preview és només una fotografia visual. No pot alimentar nous snapshots
        // ni impedir que es calculen les recomanacions amb tots els candidats.
        visibleConcertsState = shown
        preparedFeedCriteriaRevision = feedCriteriaRevision
        feedRenderRevision += 1L
        Log.d("AgendaPerf", "forYouFirstPreview ${SystemClock.elapsedRealtime() - stateStartedAt}ms · ${shown.size} cards")
    }

    /** La UI es considera hidratada només quan el catàleg i el càlcul estan preparats. */
    private var feedHydrationTick by mutableStateOf(0L)
    val isFeedReadyForReading: Boolean
        get() {
            // Depèn de l'estat observable: la UI sap quan el preview deixa de ser parcial.
            feedHydrationTick
            // v324: el resultat materialitzat es pot llegir mentre la xarxa i el
            // rànquing de descoberta s'actualitzen. No bloquegem el scroll/posició
            // per carregar el catàleg GLOBAL, que només requerixen les cerques.
            val hasStableForYou = feedMode == FeedMode.FOR_YOU &&
                search.isBlank() && filters.isEmpty() &&
                forYouSignalsReadyForProcess &&
                (forYouWindowLoadedForProcess || startupForYouPreviewReady) &&
                preparedFeedCriteriaRevision == feedCriteriaRevision &&
                visibleConcertsState.isNotEmpty()
            return hasStableForYou || (
                forYouWindowLoadedForProcess &&
                !visibleConcertsRefreshQueued && calculatingVisibleConcertsKey == null &&
                !isAgendaRefreshing && !forYouRecalculationPending &&
                (feedMode != FeedMode.FOR_YOU || (forYouSignalsReadyForProcess &&
                    cachedVisibleConcertsFinal)) &&
                fullAgendaLoadedForProcess
            )
        }

    // L'usuari ha demanat una lectura nova (recàrrega manual o sessió caducada).
    // No congelar la fotografia fins que la sincronització i el feed visible hagen acabat.
    private var pendingReadingRefreshToken: String? = null
    var completedReadingRefreshToken by mutableStateOf("")
        private set

    fun refreshForReadingSession(token: String) {
        pendingReadingRefreshToken = token
        // El gest manual força SEMPRE una selecció nova, fins si el servidor no
        // ha canviat cap esdeveniment o hi ha una sincronització en curs.
        refreshAllContent(forceForYouRefresh = true)
        if (feedMode == FeedMode.FOR_YOU) ensureFullAgendaLoaded("v264_manual_refresh")
        scheduleVisibleConcertsRefresh(0L)
    }

    var isAgendaRefreshing by mutableStateOf(false)
        private set

    var lastAgendaSyncMillis by mutableStateOf(RemoteAgendaRepository.lastSyncMillis(appContext))
        private set

    var agendaSyncError by mutableStateOf<String?>(null)
        private set

    var concerts by mutableStateOf(initialConcerts())
        private set

    // 0.18.0: cache del feed. Canviar de pestanya no recalcula "Per a tu".
    private var feedDataRevision: Long = 0L
    private var cachedVisibleConcertsKey: String? = null
    private var cachedVisibleConcerts: List<Concert> = emptyList()
    // v260: diferenciar una vista de transició de la selecció ja calculada.
    // L'antic bug guardava els 2 primers cards com a resultat definitiu.
    private var cachedVisibleConcertsFinal = false
    // v259: «Seguits» és una agenda materialitzada. No refem la selecció per
    // canvis de gustos, recomanacions, cerca passada o grau de descoberta.
    private var followedFeedCacheKey: String? = null
    private var followedFeedCache: List<Concert> = emptyList()
    // v324: els seguits dins de «Per a tu» es materialitzen per revisió de catàleg
    // i seguiments, NO per cada recàlcul de suggeriments/afinitats.
    private var forYouFollowedCacheKey: String? = null
    private var forYouFollowedCache: List<Concert> = emptyList()
    // 10fi: Per a tu manté una còpia materialitzada pròpia en memòria.
    // Sense filtre/cercador poden reutilitzar la caché genèrica sense expulsar-la.
    @Volatile private var residentForYouConcerts: List<Concert> = emptyList()
    @Volatile private var residentForYouPreferenceSignature: Int = Int.MIN_VALUE
    // No reutilitzem una llista antiga si l'usuari acaba de deixar de seguir algú.
    @Volatile private var residentForYouFollowedSignature: Int? = null
    @Volatile private var residentForYouDay: String = ""
    // v301: la cache resident sols val per a la mateixa revisió del catàleg.
    private var residentForYouDataRevision: Long = -1L
    @Volatile private var residentForYouReady: Boolean = false
    private data class ConcertSearchEntry(
        val titleKey: String,
        val artistKeys: Set<String>,
        val venueKey: String,
        val organizerKeys: Set<String>,
        val comarcaKey: String,
        val blob: String
    )

    private data class FeedSearchIndex(
        val revision: Long,
        val entriesById: Map<Int, ConcertSearchEntry>,
        val artistKeys: Set<String>,
        val eventTitleKeys: Set<String>,
        val venueKeys: Set<String>,
        val organizerKeys: Set<String>
    )

    private data class IndexedFeedSearchSuggestion(
        val suggestion: FeedSearchSuggestion,
        val normalizedValue: String
    )

    private val feedSearchIndexLock = Any()
    @Volatile private var cachedFeedSearchIndex: FeedSearchIndex? = null
    private var cachedKnownArtistRevision: Long = -1L
    private var cachedKnownArtistNames: List<String> = emptyList()
    // Tipus triat al desplegable, no deduït per la coincidència del nom amb altres entitats.
    var selectedFeedSearchArtist by mutableStateOf<String?>(null)
        private set
    // v274: el suggeriment seleccionat queda tipificat; no es tracta com a text lliure.
    var selectedFeedSearchType by mutableStateOf<FeedSearchSuggestionType?>(null)
        private set
    private var cachedSuggestionPoolRevision: Long = -1L
    private var cachedSuggestionPoolMunicipalityRevision: Long = -1L
    private var cachedSuggestionPool: List<IndexedFeedSearchSuggestion> = emptyList()
    private var cachedDedupRevision: Long = -1L
    private var cachedDedupConcerts: List<Concert> = emptyList()
    // Índex O(1) per a consultes de composició (likes, guardats, anuncis, focus).
    // Es reconstrueix només quan canvia la font base del feed.
    @Volatile private var concertByIdCache: Map<Int, Concert> = emptyMap()
    // 10bp: índex O(1) de municipis. La cerca municipal abans podia fer
    // municipis × esdeveniments i normalitzar centenars de milers de cadenes.
    private val municipalityLookupLock = Any()
    @Volatile private var municipalityLookupSource: List<Municipality>? = null
    @Volatile private var municipalityLookup: Map<String, Municipality> = emptyMap()
    // El resultat de "Per a tu" és un snapshot estable. Totes les accions continuen
    // alimentant el mateix algoritme, però no el tornem a executar a cada inici/toc.
    // Només es reconstrueix quan canvien preferències o dades rellevants dels pròxims 2 mesos.
    @Volatile private var cachedFollowSuggestionsEpoch: Long = Long.MIN_VALUE
    private val cachedFollowSuggestionsBySlot: MutableMap<String, List<FollowSuggestion>> = ConcurrentHashMap()
    private val cachedFollowSuggestionPairs: MutableMap<String, List<List<FollowSuggestion>>> = ConcurrentHashMap()
    private var followSuggestionPoolRevision = 0L
    // Es recomponen només els mòduls «Potser t’interessa seguir», mai el feed complet.
    var followSuggestionRevision by mutableStateOf(0L)
        private set
    private fun invalidateFollowSuggestions() {
        cachedFollowSuggestionsBySlot.clear()
        cachedFollowSuggestionPairs.clear()
        followSuggestionPoolRevision++
        followSuggestionRevision++
    }
    @Volatile private var calculatingVisibleConcertsKey: String? = null
    // v264: cada actualització manual o canvi de preferències crea una generació nova.
    // Un càlcul antic no pot reescriure el resultat d'una preferència recent.
    @Volatile private var forYouRecalculationGeneration: Long = 0L
    // v267: l'actualització no acaba amb la descàrrega: ha de finalitzar
    // quan s'ha publicat el nou resultat personalitzat (o un error verificable).
    var forYouRecalculationPending by mutableStateOf(false)
        private set
    var forYouRecalculationError by mutableStateOf<String?>(null)
        private set
    var forYouLastCompletedCount by mutableStateOf<Int?>(null)
        private set
    var forYouLastCompletedAtMillis by mutableStateOf(0L)
        private set
    var feedRenderRevision by mutableStateOf(0L)
        private set

    // Revisió exclusiva de les dades base que poden canviar els Batecs. No es toca
    // en canviar Per a tu, Seguits, Categoria o altres filtres visuals del feed.
    var batecAgendaRevision by mutableStateOf(0L)
        private set
    var searchSuggestionRevision by mutableStateOf(0L)
        private set

    // 10aj: Compose pinta una llista ja preparada. La composició no executa visibleConcerts().
    var visibleConcertsState by mutableStateOf<List<Concert>>(emptyList())
        private set
    // v251: cada canvi explícit de categoria, cerca, mode o Opcions inaugura una consulta.
    // Compose no pot atribuir a la consulta nova les cards calculades per a l'anterior.
    var feedCriteriaRevision by mutableStateOf(0L)
        private set
    var preparedFeedCriteriaRevision by mutableStateOf(0L)
        private set
    @Volatile private var visibleConcertsRefreshQueued = false

    private fun feedCriteriaChanged() {
        feedCriteriaRevision += 1L
        // La llista anterior no és un resultat vàlid de la nova consulta.
        // No es buida l'estat visible global fins que el càlcul acabe: altres seccions
        // i els mòduls socials el poden continuar usant com a fallback intern.
        scheduleVisibleConcertsRefresh(0L)
    }

    // 10aj: el badge de notificacions no recorre tota l'agenda durant cada recomposició.
    var unreadNotificationCountState by mutableStateOf(0)
        private set
    @Volatile private var notificationsCacheReady = false
    @Volatile private var cachedNotificationsList: List<AgendaNotification> = emptyList()
    var notificationPanelNotificationsState by mutableStateOf<List<AgendaNotification>>(emptyList())
        private set
    var notificationSuggestionsState by mutableStateOf<List<Concert>>(emptyList())
        private set
    @Volatile private var notificationBadgeRefreshInFlight = false
    @Volatile private var notificationSuggestionsRefreshInFlight = false
    @Volatile private var reportResultNotifications: List<ServerApiRepository.ReportResultNotification> = emptyList()
    @Volatile private var reportResultsRefreshInFlight = false
    var reportResultPopupState by mutableStateOf<List<ServerApiRepository.ReportResultNotification>>(emptyList())
        private set

    private fun invalidateFeedCache() {
        // O(N) una sola vegada en canviar l'agenda; evita O(N) per card durant Compose.
        concertByIdCache = concerts.associateBy { it.id }
        feedDataRevision += 1L
        // La revisió dels suggeriments és independent de la del feed:
        // el cens municipal també la incrementa fora del cicle d'esdeveniments.
        searchSuggestionRevision += 1L
        cachedVisibleConcertsKey = null
        cachedVisibleConcertsFinal = false
        calculatingVisibleConcertsKey = null
        cachedFeedSearchIndex = null
        cachedKnownArtistRevision = -1L
        cachedSuggestionPoolRevision = -1L
        // Conservem la llista antiga com a fallback visual fins que la nova estiga preparada.
        // No és font de veritat: només evita una pantalla buida o un bloqueig durant el recàlcul.
        feedRenderRevision += 1L
        scheduleVisibleConcertsRefresh()
        // 10ep: els índexs del cercador són lazy. Construir-los automàticament cada vegada
        // que canvia la finestra del feed només gastava CPU/ENTITATS sense que l'usuari buscara.
        // Notificacions també esperen fins que la primera interacció ja és fluida.
        refreshNotificationBadgeAsync(10_000L)
        refreshNotificationSuggestionsAsync(12_000L)
    }

    // El token de lectura es tanca només després del resultat personalitzat.
    // Un fallback temporal o un càlcul antic no són una actualització completada.
    private fun finishForYouRefresh(count: Int, error: String? = null) {
        forYouRecalculationPending = false
        forYouRecalculationError = error
        if (error == null) {
            forYouLastCompletedCount = count
            forYouLastCompletedAtMillis = System.currentTimeMillis()
        }
        val pending = pendingReadingRefreshToken
        if (pending != null && !isAgendaRefreshing) {
            completedReadingRefreshToken = pending
            pendingReadingRefreshToken = null
        }
    }

    private fun scheduleVisibleConcertsRefresh(delayMillis: Long = 0L) {
        if (visibleConcertsRefreshQueued) return
        visibleConcertsRefreshQueued = true
        Handler(Looper.getMainLooper()).postDelayed({
            val keyAtStart = visibleConcertsCacheKey()
            val criteriaAtStart = feedCriteriaRevision
            feedExecutor.execute {
                val started = SystemClock.elapsedRealtime()
                val prepared = runCatching { visibleConcerts() }.getOrElse {
                    Log.e("AgendaSearch", "Search/feed preparation failed", it)
                    if (search.isNotBlank() || !filters.isEmpty()) emptyList() else cachedVisibleConcerts
                }
                Log.d("AgendaPerf", "visibleConcerts ${SystemClock.elapsedRealtime() - started}ms · ${prepared.size} cards")
                Handler(Looper.getMainLooper()).post {
                    visibleConcertsRefreshQueued = false
                    feedHydrationTick += 1L
                    val currentKey = visibleConcertsCacheKey()
                    // No pintem mai un resultat vell si l'usuari ha canviat la cerca mentre
                    // el càlcul anterior estava en curs. Això elimina el «retard doble».
                    if (currentKey == keyAtStart && feedCriteriaRevision == criteriaAtStart) {
                        // v267: el càlcul personalitzat pot acabar ENTRE el retorn de
                        // `visibleConcerts()` (que porta la llista antiga) i este post.
                        // Mai deixar que un resultat provisional posterior sobreescriga
                        // el resultat definitiu publicat pel personalitzador.
                        val finalResultAlreadyAvailable = feedMode == FeedMode.FOR_YOU &&
                            cachedVisibleConcertsFinal &&
                            cachedVisibleConcertsKey == currentKey
                        val nextItems = safeVisibleFeed(
                            ForYouPublicationPolicy.choose(
                                prepared, cachedVisibleConcerts, finalResultAlreadyAvailable
                            )
                        )
                        // El fil de la finestra encara pot estar obrint SQLite.
                        // No esborrar els 8 IDs validats amb una resposta provisional buida.
                        if (nextItems.isNotEmpty() || forYouWindowLoadedForProcess ||
                            feedMode != FeedMode.FOR_YOU || visibleConcertsState.isEmpty()) {
                            visibleConcertsState = nextItems
                        }
                        preparedFeedCriteriaRevision = criteriaAtStart
                        // El primer pas sol tornar la instantània antiga mentre
                        // el càlcul de Per a tu encara està treballant.
                        if (!forYouRecalculationPending ||
                            feedMode != FeedMode.FOR_YOU || cachedVisibleConcertsFinal) {
                            if (forYouRecalculationPending && feedMode == FeedMode.FOR_YOU) {
                                finishForYouRefresh(visibleConcertsState.size)
                            } else {
                                val pending = pendingReadingRefreshToken
                                if (pending != null && !isAgendaRefreshing) {
                                    completedReadingRefreshToken = pending
                                    pendingReadingRefreshToken = null
                                }
                            }
                        }
                    } else {
                        scheduleVisibleConcertsRefresh(0L)
                    }
                }
            }
        }, delayMillis.coerceAtLeast(0L))
    }

    private fun warmSearchIndexesAsync() {
        val revision = feedDataRevision
        searchIndexExecutor.execute {
            if (feedDataRevision != revision) return@execute
            runCatching {
                feedSearchIndex()
                feedSearchSuggestionPool()
            }
        }
    }

    /**
     * Consulta lleugera i independent dels resultats dels reports.
     * Funciona tant amb compte com amb guest_id i no depén de recalcular el feed.
     */
    fun refreshReportResults() {
        if (!ServerApiRepository.isConfigured() || reportResultsRefreshInFlight) return
        reportResultsRefreshInFlight = true
        backgroundExecutor.execute {
            val result = ServerApiRepository.fetchReportResultNotifications(appContext)
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { reports ->
                    reportResultNotifications = reports
                    reportResultPopupState = reports
                    // La campaneta usa el mateix snapshot en el pròxim buildNotifications().
                    notificationsCacheReady = false
                }
                reportResultsRefreshInFlight = false
            }
        }
    }

    private fun refreshNotificationBadgeAsync(delayMillis: Long = 0L) {
        if (notificationBadgeRefreshInFlight) return
        notificationBadgeRefreshInFlight = true
        Handler(Looper.getMainLooper()).postDelayed({
            backgroundExecutor.execute {
                val reportResult = ServerApiRepository.fetchReportResultNotifications(appContext)
                reportResult.onSuccess { reports -> reportResultNotifications = reports }
                val built = runCatching { buildNotifications() }.getOrDefault(emptyList())
                Handler(Looper.getMainLooper()).post {
                    reportResult.onSuccess { reports -> reportResultPopupState = reports }
                    cachedNotificationsList = built
                    notificationPanelNotificationsState = built
                    notificationsCacheReady = true
                    unreadNotificationCountState = built.count { it.id !in readNotificationIds }
                    notificationBadgeRefreshInFlight = false
                }
            }
        }, delayMillis.coerceAtLeast(0L))
    }

    private fun invalidateNotificationCache(delayMillis: Long = 120L) {
        notificationsCacheReady = false
        refreshNotificationBadgeAsync(delayMillis)
        refreshNotificationSuggestionsAsync(delayMillis + 180L)
    }

    /**
     * Prepara les suggerències fora de la composició. El popup sempre s'obri amb
     * l'últim snapshot disponible i este refresc el substituïx després en segon pla.
     */
    private fun refreshNotificationSuggestionsAsync(delayMillis: Long = 0L) {
        if (notificationSuggestionsRefreshInFlight) return
        notificationSuggestionsRefreshInFlight = true
        Handler(Looper.getMainLooper()).postDelayed({
            backgroundExecutor.execute {
                val built = runCatching { buildStrongNotificationSuggestions() }.getOrDefault(emptyList())
                Handler(Looper.getMainLooper()).post {
                    notificationSuggestionsState = built
                    notificationSuggestionsRefreshInFlight = false
                }
            }
        }, delayMillis.coerceAtLeast(0L))
    }

    private fun updateVisibleConcertCard(id: Int, transform: (Concert) -> Concert) {
        if (visibleConcertsState.none { it.id == id } && cachedVisibleConcerts.none { it.id == id }) return
        visibleConcertsState = visibleConcertsState.map { if (it.id == id) transform(it) else it }
        cachedVisibleConcerts = cachedVisibleConcerts.map { if (it.id == id) transform(it) else it }
    }

    var stories by mutableStateOf<List<Story>>(emptyList())
        private set

    // 10cp: snapshot lleuger del carrusel ja seleccionat. Es llig directament de
    // SharedPreferences abans de qualsevol JSON gran, SQLite o petició de xarxa.
    // Això permet pintar Batecs en el primer frame real de l'app.
    var startupBatecSnapshot by mutableStateOf<List<Story>>(emptyList())
        private set

    private var batecPhotoByArtist: Map<String, String> = emptyMap()
    var batecPhotoCacheRevision by mutableStateOf(0L)
        private set

    // El catàleg d'imatges d'ENTITATS es carrega fora del fil principal.
    // Compose llig esta revisió perquè les fotos apareguen quan el preload acaba,
    // sense haver de parsejar api_entity_images.json des d'una card o un Batec.
    var entityImageCatalogRevision by mutableStateOf(0L)
        private set

    var batecsHasMore by mutableStateOf(false)
        private set
    var isLoadingMoreBatecs by mutableStateOf(false)
        private set
    private var batecsNextOffset: Int = 0

    var seenBatecKeys by mutableStateOf<Set<String>>(emptySet())
        private set

    // v220: una vegada obert un artista, no reapareix fins que es publique
    // un NOU concert d'eixe artista. Es conserva per compte i en reiniciar l'app.
    var viewedBatecArtistsAt by mutableStateOf(loadViewedBatecArtistsAt())
        private set

    private fun loadViewedBatecArtistsAt(): Map<String, Long> {
        val raw = store.getString(userCacheKey("viewed_batec_artists_v220"), null) ?: return emptyMap()
        return runCatching {
            val json = JSONObject(raw)
            buildMap {
                json.keys().forEach { key ->
                    json.optLong(key).takeIf { it > 0L }?.let { put(key, it) }
                }
            }
        }.getOrDefault(emptyMap())
    }

    /** Els Batecs vistos continuen visibles en gris. Només els artistes seguits apareixen. */
    fun canShowBatecArtist(artist: String): Boolean {
        val normalized = normalizeSearchValue(artist)
        return normalized.isNotBlank() && !isArtistBlocked(artist) &&
            followedArtists.any { normalizeSearchValue(it) == normalized }
    }

    // La informació històrica de visualitzacions es conserva per compatibilitat,
    // però ja NO oculta els cercles. L'estat gris depén dels story_key vistos.
    fun markBatecArtistViewed(artist: String) {
        val key = normalizeSearchValue(artist)
        if (key.isBlank()) return
        val whenViewed = System.currentTimeMillis()
        viewedBatecArtistsAt = viewedBatecArtistsAt + (key to whenViewed)
        val json = JSONObject()
        viewedBatecArtistsAt.forEach { (k, timestamp) -> json.put(k, timestamp) }
        store.edit().putString(userCacheKey("viewed_batec_artists_v220"), json.toString()).apply()
    }

    // La selecció de Batecs (afinitats, seguiments i novetats) és relativament costosa.
    // Es conserva durant un minut i marcar un Batec com vist només reordena esta llista
    // barata: obrir/tancar el visor no torna a puntuar centenars d'esdeveniments.
    private var cachedActiveStoriesBaseKey: String? = null
    private var cachedActiveStoriesBase: List<Story> = emptyList()
    private var cachedActiveStoriesBaseExpiresAtMillis: Long = 0L
    private var lastSavedBatecSnapshotJson: String? = null

    private var recentBatecEventCreations by mutableStateOf<Map<Int, Long>>(emptyMap())

    var batecEligibilityByEvent by mutableStateOf<Map<Int, ServerApiRepository.BatecEligibility>>(emptyMap())
        private set

    private fun batecStartupSnapshotKey(): String = userCacheKey("batecs_startup_snapshot_v1")
    private fun batecPhotoCacheKey(): String = userCacheKey("batecs_photo_cache_v1")

    private fun storyToStartupJson(story: Story): JSONObject = JSONObject()
        .put("id", story.id)
        .put("artist", story.artist)
        .put("initials", story.initials)
        .put("title", story.title)
        .put("body", story.body)
        .put("published_at", story.publishedAtMillis)
        .put("event_id", story.eventId ?: JSONObject.NULL)
        .put("event_title", story.eventTitle)
        .put("event_town", story.eventTown)
        .put("event_date", story.eventDate)
        .put("event_time", story.eventTime)
        .put("creator_user_id", story.creatorUserId ?: JSONObject.NULL)
        .put("author_username", story.authorUsername)
        .put("organizer", story.organizer)
        .put("can_edit", story.canEdit)
        .put("source_type", story.sourceType)
        .put("story_key", story.storyKey)
        .put("seen", story.seen)
        .put("relevance", story.relevance)

    private fun storyFromStartupJson(item: JSONObject): Story? = runCatching {
        Story(
            id = item.optInt("id"),
            artist = item.optString("artist"),
            initials = item.optString("initials"),
            title = item.optString("title"),
            body = item.optString("body"),
            publishedAtMillis = item.optLong("published_at"),
            eventId = item.optInt("event_id").takeIf { it > 0 },
            eventTitle = item.optString("event_title"),
            eventTown = item.optString("event_town"),
            eventDate = item.optString("event_date"),
            eventTime = item.optString("event_time"),
            creatorUserId = item.optInt("creator_user_id").takeIf { it > 0 },
            authorUsername = item.optString("author_username"),
            organizer = item.optBoolean("organizer", false),
            canEdit = item.optBoolean("can_edit", false),
            sourceType = item.optString("source_type", "MANUAL"),
            storyKey = item.optString("story_key"),
            seen = item.optBoolean("seen", false),
            relevance = item.optInt("relevance", 0)
        )
    }.getOrNull()?.takeIf { it.artist.isNotBlank() }

    private fun loadBatecStartupSnapshot(): List<Story> {
        val raw = store.getString(batecStartupSnapshotKey(), null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    array.optJSONObject(i)?.let(::storyFromStartupJson)?.let(::add)
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun saveBatecStartupSnapshot(items: List<Story>) {
        if (items.isEmpty()) return
        // Conservar primer UN representant de cadascun dels sis artistes. Si
        // l'artista més actiu té 20 batecs, no ha de deixar fora els altres cinc
        // en truncar el snapshot a 40 registres.
        val representatives = items.distinctBy { normalizeSearchValue(it.artist) }.take(6)
        val extraItems = items.filterNot { candidate ->
            representatives.any { it.storyKey == candidate.storyKey && it.id == candidate.id }
        }.take((40 - representatives.size).coerceAtLeast(0))
        val array = JSONArray()
        (representatives + extraItems).forEach { array.put(storyToStartupJson(it)) }
        val json = array.toString()
        if (lastSavedBatecSnapshotJson == json) return
        if (lastSavedBatecSnapshotJson == null && store.getString(batecStartupSnapshotKey(), null) == json) {
            lastSavedBatecSnapshotJson = json
            return
        }
        lastSavedBatecSnapshotJson = json
        store.edit().putString(batecStartupSnapshotKey(), json).apply()
    }

    private fun loadBatecPhotoCache(): Map<String, String> {
        val raw = store.getString(batecPhotoCacheKey(), null) ?: return emptyMap()
        return runCatching {
            val obj = JSONObject(raw)
            buildMap {
                obj.keys().forEach { key ->
                    obj.optString(key).trim().takeIf(String::isNotBlank)?.let { put(key, it) }
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun saveBatecPhotoCache(items: Map<String, String>) {
        val obj = JSONObject()
        items.entries.take(80).forEach { (key, value) -> obj.put(key, value) }
        store.edit().putString(batecPhotoCacheKey(), obj.toString()).apply()
    }

    fun cachedBatecPhotoForArtist(artist: String): String? {
        batecPhotoCacheRevision
        return batecPhotoByArtist[normalizeSearchValue(artist)]
    }

    private fun rebuildBatecPhotoCache() {
        val artistNames = (startupBatecSnapshot.asSequence() + stories.asSequence())
            .map { it.artist.trim() }
            .filter(String::isNotBlank)
            .distinctBy(::normalizeSearchValue)
            .take(50)
            .toList()
        if (artistNames.isEmpty()) return
        val concertSnapshot = concerts
        val updated = batecPhotoByArtist.toMutableMap()
        artistNames.forEach { artist ->
            val key = normalizeSearchValue(artist)
            if (key.isBlank()) return@forEach
            val fromEntity = ServerApiRepository.peekCachedEntityImageUrl("ARTIST", artist)
            val fromEvent = concertSnapshot.firstOrNull { concert ->
                normalizeSearchValue(concert.artist) == key || concert.lineup.any { normalizeSearchValue(it) == key }
            }?.let { concert ->
                concert.artistImageUri?.takeIf(String::isNotBlank)
                    ?: concert.posterUri?.takeIf(String::isNotBlank)
                    ?: concert.eventCardImageUri?.takeIf(String::isNotBlank)
            }
            (fromEntity ?: fromEvent)?.trim()?.takeIf(String::isNotBlank)?.let { updated[key] = it }
        }
        if (updated != batecPhotoByArtist) {
            batecPhotoByArtist = updated
            saveBatecPhotoCache(updated)
            Handler(Looper.getMainLooper()).post { batecPhotoCacheRevision += 1L }
        }
    }

    /**
     * v248: Batecs per a més de sis artistes seguits (7+), exactament sis cercles.
     * Només els artistes seguits no bloquejats. Prioritat als continguts nous
     * i sense vore. Els ja vistos o caducats són grisos si cal completar sis.
     * No es mostren concerts anteriors al dia civil actual.
     */
    private fun batecFollowedNames(): List<String> =
        BatecVisibilityPolicy.visibleFollowedArtists(
            followedArtists, blockedArtists, ::normalizeSearchValue
        )

    fun shouldDisplayBatecs(): Boolean = batecFollowedNames().size > 6

    /** També filtra el snapshot: mai presentar durant l'arrancada una data passada. */
    private fun currentBatecDate(
        item: Story,
        eventsById: Map<Int, Concert>,
        today: LocalDate
    ): Boolean {
        val event = item.eventId?.let(eventsById::get)
        if (event != null) {
            return BatecVisibilityPolicy.isCurrentOrFutureConcert(event.date, event.endDate, today)
        }
        if (item.eventId != null || item.sourceType == "AUTO_EVENT" || item.eventDate.isNotBlank()) {
            return BatecVisibilityPolicy.isCurrentOrFutureConcert(item.eventDate, "", today)
        }
        // Manual sense concert ni data: no afirma que hi haja cap actuació passada.
        return true
    }

    /** Tallafoc de data de la UI: invalida també els batecs que estaven en
     * pantalla exactament quan el dia canvia, abans del següent recàlcul.
     */
    fun currentBatecsForDisplay(items: List<Story>): List<Story> {
        val today = java.time.LocalDate.now(agendaZone)
        // L'índex es manté quan canvia el catàleg; construir associateBy() ací
        // escanejava tots els concerts en una recomposició dels sis cercles.
        val eventsById = concertByIdCache
        val eligibleKeys = batecFollowedNames().mapTo(HashSet(), ::normalizeSearchValue)
        return items.filter { item ->
            normalizeSearchValue(item.artist) in eligibleKeys && currentBatecDate(item, eventsById, today)
        }
    }

    /** Manté un cercle per artista i fa el rànquing DESPRÉS d'aplicar els vistos. */
    private fun selectSixBatecStories(items: List<Story>, nowMillis: Long): List<Story> {
        val eligible = batecFollowedNames()
        if (eligible.size <= 6) return emptyList()
        val today = java.time.LocalDate.now(agendaZone)
        val eventsById = concertByIdCache
        val eligibleKeys = eligible.mapTo(HashSet(), ::normalizeSearchValue)
        val grouped = items.asSequence()
            .filter { normalizeSearchValue(it.artist) in eligibleKeys && currentBatecDate(it, eventsById, today) }
            .map { item ->
                val key = item.storyKey.ifBlank { "manual:${item.id}" }
                val recent = BatecVisibilityPolicy.isRecentPublication(
                    item.publishedAtMillis, nowMillis, STORY_LIFETIME_MILLIS
                )
                item.copy(seen = !recent || BatecVisibilityPolicy.isGrey(item.sourceType, key, seenBatecKeys))
            }
            .groupBy { normalizeSearchValue(it.artist) }
        val candidates = grouped.values.map { group ->
            BatecVisibilityPolicy.ArtistCandidate(
                artist = group.first().artist,
                items = group,
                newestUnseenAt = group.filterNot { it.seen }.maxOfOrNull { it.publishedAtMillis } ?: 0L,
                newestAnyAt = group.maxOfOrNull { it.publishedAtMillis } ?: 0L
            )
        }
        val six = BatecVisibilityPolicy.selectSixArtists(candidates, eligible.size)
        // No mostrar un snapshot incomplet si els seguiments han canviat; el
        // recàlcul amb els artistes nous el reconstruirà tot seguit.
        if (six.size < 6) return emptyList()
        return six.flatMap { it.items }
    }

    fun visibleStartupBatecs(nowMillis: Long = System.currentTimeMillis()): List<Story> =
        selectSixBatecStories(startupBatecSnapshot, nowMillis)

    fun activeStories(nowMillis: Long = System.currentTimeMillis()): List<Story> {
        val followedNames = batecFollowedNames()
        if (followedNames.size <= 6) return emptyList()

        val followedKeys = followedNames.map(::normalizeSearchValue).toSet()
        val today = java.time.LocalDate.now(agendaZone)
        val baseKey = buildString {
            append(today).append('|')
            append(stories.hashCode()).append('|')
            append(recentBatecEventCreations.hashCode()).append('|')
            // La revisió ja canvia quan varia el catàleg. Evitem el hash profund
            // de TOTS els Concerts en cada lectura dels Batecs.
            append(batecAgendaRevision).append('|')
            append(followedKeys.sorted().joinToString(",")).append('|')
            append(blockedArtists.hashCode()).append('|')
            append(currentAccount?.serverUserId ?: 0)
        }
        val base = if (cachedActiveStoriesBaseKey == baseKey &&
            nowMillis < cachedActiveStoriesBaseExpiresAtMillis) {
            cachedActiveStoriesBase
        } else {
            val eventById = concertByIdCache
            val eventsByArtist = linkedMapOf<String, MutableList<Concert>>()
            concerts.asSequence()
                .filter { BatecVisibilityPolicy.isCurrentOrFutureConcert(it.date, it.endDate, today) }
                .forEach { event ->
                    (sequenceOf(event.artist) + event.lineup.asSequence())
                        .map(::normalizeSearchValue).filter { it in followedKeys }.distinct()
                        .forEach { key -> eventsByArtist.getOrPut(key) { mutableListOf() }.add(event) }
                }

            // La novetat manual s'atribuïx a un artista seguit del cartell només
            // si és un batec general del concert. Concerts caducats no participen.
            val manualByArtist = stories.asSequence()
                .filter { currentBatecDate(it, eventById, today) }
                .mapNotNull { item ->
                    val event = item.eventId?.let(eventById::get)
                    val artistKey = normalizeSearchValue(item.artist)
                    val selected = if (artistKey in followedKeys) {
                        followedNames.firstOrNull { normalizeSearchValue(it) == artistKey }
                    } else if (event != null &&
                        (artistKey.isBlank() || artistKey == normalizeSearchValue(event.artist))) {
                        followedNames.firstOrNull { followed ->
                            val key = normalizeSearchValue(followed)
                            normalizeSearchValue(event.artist) == key ||
                                event.lineup.any { normalizeSearchValue(it) == key }
                        }
                    } else null
                    selected?.let { artist ->
                        item.copy(
                            artist = artist,
                            eventTown = item.eventTown.ifBlank { event?.town.orEmpty() },
                            eventDate = item.eventDate.ifBlank { event?.date.orEmpty() },
                            eventTime = item.eventTime.ifBlank { event?.time.orEmpty() },
                            storyKey = item.storyKey.ifBlank { "manual:${item.id}" }
                        )
                    }
                }.groupBy { normalizeSearchValue(it.artist) }

            val freshManualEventArtistKeys = manualByArtist.values.asSequence()
                .flatten()
                .filter { BatecVisibilityPolicy.isRecentPublication(it.publishedAtMillis, nowMillis, STORY_LIFETIME_MILLIS) }
                .mapNotNull { item -> item.eventId?.let { it to normalizeSearchValue(item.artist) } }
                .toSet()
            val automaticByArtist = automaticEventBatecs(nowMillis, freshManualEventArtistKeys)
                .groupBy { normalizeSearchValue(it.artist) }

            fun latestConcert(artistKey: String): Concert? = eventsByArtist[artistKey]
                ?.maxByOrNull { recentBatecEventCreations[it.id] ?: it.createdAtMillis }

            fun lastConcertStory(event: Concert, artist: String, artistKey: String): Story {
                val publishedAt = recentBatecEventCreations[event.id] ?: event.createdAtMillis
                return Story(
                    id = -1_000_000 - event.id,
                    artist = artist,
                    initials = artist.split(' ').filter(String::isNotBlank).take(2)
                        .joinToString("") { it.take(1).uppercase() },
                    title = "Esdeveniment",
                    body = listOf(event.town, AgendaDateFormat.date(event.date), event.time)
                        .filter(String::isNotBlank).joinToString(" · "),
                    publishedAtMillis = publishedAt.takeIf { it > 0L } ?: 0L,
                    eventId = event.id,
                    eventTitle = event.title,
                    eventTown = event.town,
                    eventDate = event.date,
                    eventTime = event.time,
                    creatorUserId = event.creatorUserId,
                    sourceType = "AUTO_EVENT",
                    storyKey = "event:${event.id}:$artistKey"
                )
            }

            fun emptyArtistStory(artist: String, artistKey: String, index: Int) = Story(
                id = Int.MIN_VALUE + index,
                artist = artist,
                initials = artist.split(' ').filter(String::isNotBlank).take(2)
                    .joinToString("") { it.take(1).uppercase() },
                title = "Sense novetats",
                body = "Encara no hi ha novetats d'este artista.",
                publishedAtMillis = 0L,
                sourceType = "FOLLOWED_PLACEHOLDER",
                storyKey = "followed:$artistKey",
                seen = true
            )

            val selected = followedNames.flatMapIndexed { index, artist ->
                val key = normalizeSearchValue(artist)
                val freshManual = manualByArtist[key].orEmpty().filter {
                    BatecVisibilityPolicy.isRecentPublication(it.publishedAtMillis, nowMillis, STORY_LIFETIME_MILLIS)
                }
                val olderManual = manualByArtist[key].orEmpty().filterNot {
                    BatecVisibilityPolicy.isRecentPublication(it.publishedAtMillis, nowMillis, STORY_LIFETIME_MILLIS)
                }
                val content = (freshManual + automaticByArtist[key].orEmpty() + olderManual)
                    .distinctBy { it.storyKey.ifBlank { "manual:${it.id}" } }
                    .sortedByDescending { it.publishedAtMillis }
                    .take(20)
                val concert = if (content.isEmpty()) latestConcert(key) else null
                when {
                    content.isNotEmpty() -> content
                    concert != null -> listOf(lastConcertStory(concert, artist, key))
                    else -> listOf(emptyArtistStory(artist, key, index))
                }
            }
            // No prolongar arbitràriament una novetat passada de 48 hores.
            // La cache s'invalida exactament quan la més pròxima caduca.
            val nextExpiry = (stories.asSequence().map { it.publishedAtMillis } +
                concerts.asSequence().map { recentBatecEventCreations[it.id] ?: it.createdAtMillis })
                .filter { it > 0L && it <= Long.MAX_VALUE - STORY_LIFETIME_MILLIS }
                .map { it + STORY_LIFETIME_MILLIS }
                .filter { it > nowMillis }
                .minOrNull()
            cachedActiveStoriesBaseExpiresAtMillis = minOf(
                nowMillis + forYouFastPreviewMaxAge, nextExpiry ?: Long.MAX_VALUE
            )
            cachedActiveStoriesBaseKey = baseKey
            cachedActiveStoriesBase = selected
            selected
        }
        val featured = selectSixBatecStories(base, nowMillis)
        if (featured.isNotEmpty()) saveBatecStartupSnapshot(featured)
        return featured
    }

    private fun automaticEventBatecs(nowMillis: Long, manualEventArtistKeys: Set<Pair<Int, String>>): List<Story> {
        // Batecs és independent de Per a tu: les novetats automàtiques només provenen
        // de concerts d'artistes/grups seguits publicats en les últimes 48 h.
        val followedKeys = followedArtists.asSequence()
            .map(::normalizeSearchValue)
            .filter(String::isNotBlank)
            .toSet()
        if (followedKeys.isEmpty()) return emptyList()

        val today = java.time.LocalDate.now(agendaZone)
        return concerts.asSequence()
            .mapNotNull { concert ->
                val createdAt = recentBatecEventCreations[concert.id] ?: concert.createdAtMillis
                if (createdAt <= 0L || nowMillis - createdAt !in 0 until STORY_LIFETIME_MILLIS) return@mapNotNull null
                if (!BatecVisibilityPolicy.isCurrentOrFutureConcert(concert.date, concert.endDate, today)) {
                    return@mapNotNull null
                }
                val artists = BatecVisibilityPolicy.participatingFollowedArtists(
                    concert.artist, concert.lineup, followedArtists, ::normalizeSearchValue
                ).filter { (concert.id to normalizeSearchValue(it)) !in manualEventArtistKeys }
                if (artists.isEmpty()) null else artists.map { Triple(concert, it, createdAt) }
            }
            .flatMap { it.asSequence() }
            .sortedByDescending { (_, _, createdAt) -> createdAt }
            .take(120)
            .map { (concert, displayArtist, createdAt) ->
                val key = "event:${concert.id}:${normalizeSearchValue(displayArtist)}"
                val whenText = buildString {
                    append(concert.town)
                    append(" · ")
                    append(concert.date)
                    if (concert.time.isNotBlank()) { append(" · "); append(concert.time) }
                }
                Story(
                    id = -concert.id,
                    artist = displayArtist,
                    initials = displayArtist.split(" ").filter(String::isNotBlank).take(2).joinToString("") { it.take(1).uppercase() }.ifBlank { "AV" },
                    title = "Nou esdeveniment",
                    body = whenText,
                    publishedAtMillis = createdAt,
                    eventId = concert.id,
                    eventTitle = concert.title,
                    eventTown = concert.town,
                    eventDate = concert.date,
                    eventTime = concert.time,
                    creatorUserId = concert.creatorUserId,
                    organizer = false,
                    canEdit = false,
                    sourceType = "AUTO_EVENT",
                    storyKey = key,
                    seen = key in seenBatecKeys,
                    relevance = 100
                )
            }
            .toList()
    }

    fun markBatecSeen(story: Story) = markBatecsSeen(listOf(story))

    fun markBatecsSeen(storiesToMark: List<Story>) {
        val keys = storiesToMark.asSequence()
            .filter { it.sourceType != "FOLLOWED_PLACEHOLDER" }
            .map { story ->
                story.storyKey.ifBlank {
                    if (story.sourceType == "AUTO_EVENT" && story.eventId != null) "event:${story.eventId}" else "manual:${story.id}"
                }
            }
            .filter(String::isNotBlank)
            .distinct()
            .filterNot { it in seenBatecKeys }
            .toList()
        if (keys.isEmpty()) return

        // Un sol canvi d'estat i una sola escriptura local encara que el grup tinga
        // diversos Batecs. Les confirmacions al servidor continuen en segon pla.
        seenBatecKeys = seenBatecKeys + keys
        saveStringSet(userCacheKey("seen_batecs"), seenBatecKeys)
        keys.forEach { key ->
            postServerStateOrQueue("batecs/seen.php", JSONObject().put("story_key", key))
        }
    }

    // El panell d'un esdeveniment continua mostrant els seus batecs manuals,
    // encara que l'organitzador no seguisca l'artista. La limitació als seguits
    // s'aplica al carrusel d'Inici, no a la gestió professional.
    fun batecsForConcert(concertId: Int): List<Story> =
        stories.filter { it.eventId == concertId }

    fun refreshBatecEligibility(concertId: Int) {
        if (currentAccount == null || concertId <= 0) return
        backgroundExecutor.execute {
            val result = ServerApiRepository.fetchBatecEligibility(appContext, concertId)
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { eligibility ->
                    batecEligibilityByEvent = batecEligibilityByEvent + (concertId to eligibility)
                }
            }
        }
    }

    fun refreshBatecsFromServer(onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        if (!ServerApiRepository.isConfigured()) return
        backgroundExecutor.execute {
            // v248: baixar fins a 50 publicacions recents permet recuperar més
            // activitat d'artistes seguits abans de seleccionar els sis cercles.
            val initialBatecLimit = 50
            val result = ServerApiRepository.fetchBatecsPage(appContext, offset = 0, limit = initialBatecLimit)

            // 10ba: el carrusel no espera seen.php + recent-events.php. En quant arriba
            // index.php pintem els Batecs; els metadades auxiliars s'actualitzen després.
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { page ->
                    stories = page.items
                    batecsHasMore = page.hasMore
                    batecsNextOffset = page.nextOffset
                    batecOperationError = null
                    onResult(true, null)
                }.onFailure { error ->
                    val message = error.message ?: "No s'han pogut carregar els batecs."
                    batecOperationError = message
                    onResult(false, message)
                }
            }

            if (result.isSuccess) {
                // seen.php i recent-events.php són auxiliars. No els fem competir amb
                // index.php ni amb les imatges que es descarreguen just després d'obrir Inici.
                Handler(Looper.getMainLooper()).postDelayed({
                    backgroundExecutor.execute {
                        val seenResult = ServerApiRepository.fetchSeenBatecKeys(appContext)
                        Handler(Looper.getMainLooper()).post {
                            seenResult.onSuccess {
                                seenBatecKeys = it
                                saveStringSet(userCacheKey("seen_batecs"), seenBatecKeys)
                            }
                        }
                    }
                }, 1_200L)
                Handler(Looper.getMainLooper()).postDelayed({
                    backgroundExecutor.execute {
                        val recentResult = ServerApiRepository.fetchRecentBatecEventCreations(appContext)
                        Handler(Looper.getMainLooper()).post {
                            recentResult.onSuccess { incoming ->
                                if (recentBatecEventCreations != incoming) {
                                    recentBatecEventCreations = incoming
                                    batecAgendaRevision += 1L
                                }
                            }
                        }
                    }
                }, 3_500L)
            }
        }
    }

    fun loadMoreBatecs() {
        if (!ServerApiRepository.isConfigured() || !batecsHasMore || isLoadingMoreBatecs) return
        isLoadingMoreBatecs = true
        val offset = batecsNextOffset
        backgroundExecutor.execute {
            val result = ServerApiRepository.fetchBatecsPage(appContext, offset = offset, limit = 10)
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { page ->
                    val existingKeys = stories.map { it.storyKey.ifBlank { "manual:${it.id}" } }.toHashSet()
                    stories = stories + page.items.filter { it.storyKey.ifBlank { "manual:${it.id}" } !in existingKeys }
                    batecsHasMore = page.hasMore
                    batecsNextOffset = page.nextOffset
                }.onFailure { batecOperationError = it.message }
                isLoadingMoreBatecs = false
            }
        }
    }

    private fun concertStartMillis(concert: Concert): Long? = runCatching {
        val date = LocalDate.parse(concert.date)
        val time = LocalTime.parse(concert.time.take(5), DateTimeFormatter.ofPattern("HH:mm"))
        LocalDateTime.of(date, time)
            .atZone(ZoneId.of("Europe/Madrid"))
            .toInstant()
            .toEpochMilli()
    }.getOrNull()

    fun canCreateBatecForConcert(concert: Concert, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (currentAccount == null) return false
        if (batecEligibilityByEvent[concert.id]?.organizer == true) return true
        if (canManageConcert(concert)) return true
        val start = concertStartMillis(concert) ?: return false
        return nowMillis in (start - 60L * 60L * 1000L)..(start + 24L * 60L * 60L * 1000L)
    }

    fun batecCreationMessage(concert: Concert, nowMillis: Long = System.currentTimeMillis()): String {
        if (currentAccount == null) return "Inicia sessió per crear un batec."
        if (batecEligibilityByEvent[concert.id]?.organizer == true || canManageConcert(concert)) {
            return "Com a organitzador pots crear batecs d'este concert en qualsevol moment."
        }
        val start = concertStartMillis(concert)
            ?: return "No es pot obrir la participació perquè el concert no té una hora vàlida."
        val opens = start - 60L * 60L * 1000L
        val closes = start + 24L * 60L * 60L * 1000L
        return when {
            nowMillis < opens -> "El públic podrà crear batecs des d'1 hora abans que comence el concert."
            nowMillis > closes -> "El període per crear batecs del públic ja ha acabat."
            else -> "Pots crear batecs fins a 24 hores després de l'inici del concert."
        }
    }

    fun publishEventBatec(
        concertId: Int,
        title: String,
        body: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val concert = concerts.firstOrNull { it.id == concertId }
        if (currentAccount == null) {
            onResult(false, "Inicia sessió per crear un batec.")
            return
        }
        if (concert == null) {
            onResult(false, "No s'ha trobat l'esdeveniment.")
            return
        }
        if (title.isBlank()) {
            onResult(false, "Escriu un títol per al batec.")
            return
        }
        if (!canCreateBatecForConcert(concert)) {
            onResult(false, batecCreationMessage(concert))
            return
        }
        if (isPublishingBatec) return

        isPublishingBatec = true
        batecOperationError = null
        backgroundExecutor.execute {
            val result = ServerApiRepository.createBatec(appContext, concertId, title, body)
            Handler(Looper.getMainLooper()).post {
                isPublishingBatec = false
                result.onSuccess { created ->
                    stories = listOf(created) + stories.filterNot { it.id == created.id }
                    batecOperationError = null
                    onResult(true, "Batec publicat.")
                }.onFailure { error ->
                    val message = error.message ?: "No s'ha pogut publicar el batec."
                    batecOperationError = message
                    onResult(false, message)
                }
            }
        }
    }

    var posts by mutableStateOf(DemoData.posts)
        private set

    var communityGroups by mutableStateOf<List<CommunityGroup>>(emptyList())
        private set

    var communityRequestsByGroup by mutableStateOf<Map<Int, List<CommunityMemberRequest>>>(emptyMap())
        private set

    var communityOperationMessage by mutableStateOf<String?>(null)
        private set

    var chatMessages by mutableStateOf(DemoData.chatMessages)
        private set

    var concertComments by mutableStateOf(DemoData.concertComments)
        private set

    var communityChatMessages by mutableStateOf<List<CommunityChatMessage>>(emptyList())
        private set
    // Avisos dels xats vinculats a esdeveniments. El cursor per grup és persistent
    // i independent dels avisos dels grups d'amics (API collections).
    var eventCommunityNotifications by mutableStateOf<List<AgendaNotification>>(emptyList())
        private set
    @Volatile private var eventCommunityRefreshInFlight = false

    private fun eventCommunitySeenKey(userId: Int, groupId: Int) = "event_chat_seen_${userId}_$groupId"

    /** Comprova missatges de grups d'esdeveniment sense bloquejar el feed. */
    fun refreshEventCommunityNotifications() {
        val ownerId = currentAccount?.serverUserId ?: run {
            eventCommunityNotifications = emptyList()
            invalidateNotificationCache(0L)
            return
        }
        if (eventCommunityRefreshInFlight || !ServerApiRepository.isConfigured()) return
        eventCommunityRefreshInFlight = true
        backgroundExecutor.execute {
            val alerts = mutableListOf<AgendaNotification>()
            val groups = ServerApiRepository.fetchCommunityGroups(appContext, mine = true).getOrNull()
            if (groups != null) {
                // Rotem els grups en lots de 16: mai deixem grups seguits fora
                // permanentment i evitem una petició per cada xat en un únic refresc.
                val acceptedGroups = groups.filter { it.myStatus == "ACCEPTED" && it.chatEnabled }
                val pollOffsetKey = "event_chat_poll_offset_$ownerId"
                val pollStart = if (acceptedGroups.isEmpty()) 0
                    else store.getInt(pollOffsetKey, 0) % acceptedGroups.size
                val pollingGroups = if (acceptedGroups.size <= 16) acceptedGroups else
                    List(16) { index -> acceptedGroups[(pollStart + index) % acceptedGroups.size] }
                if (acceptedGroups.isNotEmpty()) {
                    store.edit().putInt(pollOffsetKey, (pollStart + pollingGroups.size) % acceptedGroups.size).apply()
                }
                pollingGroups.forEach { group ->
                    val messages = ServerApiRepository.fetchCommunityMessages(appContext, group.id).getOrNull()
                        ?: return@forEach
                    val key = eventCommunitySeenKey(ownerId, group.id)
                    val lastSeen = store.getInt(key, -1)
                    val newest = messages.maxOfOrNull { it.id } ?: 0
                    if (lastSeen < 0) {
                        // Primera consulta: no enviar una allau de missatges històrics.
                        store.edit().putInt(key, newest).apply()
                    } else {
                        messages.asSequence()
                            .filter { it.id > lastSeen && it.authorUserId != ownerId }
                            .toList().takeLast(5)
                            .forEach { message ->
                                alerts += AgendaNotification(
                                    id = "event_chat_${group.id}_${message.id}",
                                    type = NotificationType.COMMUNITY,
                                    title = "${group.title} · Missatge nou",
                                    body = "${message.author} · ${message.text}",
                                    concertId = group.concertId.takeIf { it > 0 },
                                    sortKey = runCatching {
                                        LocalDateTime.parse(message.createdAt.replace(" ", "T"))
                                            .atZone(ZoneId.of("UTC")).toInstant().toEpochMilli()
                                    }.getOrDefault(System.currentTimeMillis()),
                                    communityGroupId = group.id
                                )
                            }
                    }
                }
                // Les consultes privades del tauló: avisar només el destinatari,
                // no enviar-les mai als altres participants d'un xat.
                val inbox = ServerApiRepository.fetchCommunityBoardInbox(appContext).getOrNull()
                if (inbox != null) {
                    val key = "board_inbox_seen_$ownerId"
                    val seen = store.getInt(key, -1)
                    val newest = inbox.maxOfOrNull { it.id } ?: 0
                    if (seen < 0) {
                        store.edit().putInt(key, newest).apply()
                    } else {
                        inbox.asSequence().filter { it.id > seen && it.authorUserId != ownerId }
                            .toList().takeLast(12).forEach { entry ->
                                alerts += AgendaNotification(
                                    id = "board_private_${entry.id}",
                                    type = NotificationType.COMMUNITY,
                                    title = "${entry.title} · Missatge privat",
                                    body = "${entry.author} · ${entry.body}",
                                    concertId = entry.eventId.takeIf { it > 0 },
                                    sortKey = runCatching {
                                        LocalDateTime.parse(entry.createdAt.replace(" ", "T"))
                                            .atZone(ZoneId.of("UTC")).toInstant().toEpochMilli()
                                    }.getOrDefault(System.currentTimeMillis()),
                                    communityGroupId = entry.groupId
                                )
                            }
                        if (newest > seen) store.edit().putInt(key, newest).apply()
                    }
                }
                // Canvis de l'estat de xats i sol·licituds pendents de validació.
                groups.filter { it.myRole == "CREATOR" && it.pendingCount > 0 }.forEach { group ->
                    alerts += AgendaNotification(
                        id = "event_requests_${group.id}_${group.pendingCount}",
                        type = NotificationType.COMMUNITY,
                        title = "${group.title} · Sol·licituds pendents",
                        body = "${group.pendingCount} ${if (group.pendingCount == 1) "sol·licitud" else "sol·licituds"} per revisar",
                        concertId = group.concertId.takeIf { it > 0 },
                        sortKey = System.currentTimeMillis(),
                        communityGroupId = group.id
                    )
                }
                groups.forEach { group ->
                    val metadataKey = "event_board_fields_${ownerId}_${group.id}"
                    val currentFields = listOf(group.title, group.description, group.town,
                        group.meetingAt, group.capacity?.toString().orEmpty(), group.chatEnabled.toString())
                        .joinToString("|#|")
                    val previousFields = store.getString(metadataKey, null)
                    val pendingMetaKey = "event_board_changes_${ownerId}_${group.id}"
                    val pendingMetaTimeKey = "event_board_changes_at_${ownerId}_${group.id}"
                    if (previousFields != null && previousFields != currentFields) {
                        store.edit().putString(pendingMetaKey, currentFields)
                            .putLong(pendingMetaTimeKey, System.currentTimeMillis()).apply()
                    }
                    if (store.getString(pendingMetaKey, null) == currentFields) {
                        alerts += AgendaNotification(
                            id = "event_board_changed_${group.id}_${currentFields.hashCode()}",
                            type = NotificationType.COMMUNITY,
                            title = "${group.title} · Dades actualitzades",
                            body = "S'ha modificat la informació del tauló.",
                            concertId = group.concertId.takeIf { it > 0 },
                            sortKey = store.getLong(pendingMetaTimeKey, System.currentTimeMillis()),
                            communityGroupId = group.id
                        )
                    }
                    if (previousFields != currentFields) store.edit().putString(metadataKey, currentFields).apply()
                    val key = "event_chat_status_${ownerId}_${group.id}"
                    val previous = store.getString(key, null)
                    val pendingKey = "event_chat_status_pending_${ownerId}_${group.id}"
                    val timestampKey = "event_chat_status_at_${ownerId}_${group.id}"
                    if (previous != null && previous != group.status) {
                        store.edit()
                            .putString(pendingKey, group.status)
                            .putLong(timestampKey, System.currentTimeMillis())
                            .apply()
                    }
                    if (store.getString(pendingKey, null) == group.status) {
                        alerts += AgendaNotification(
                            id = "event_status_${group.id}_${group.status}",
                            type = NotificationType.COMMUNITY,
                            title = "${group.title} · Canvi en el xat",
                            body = if (group.status == "CLOSED") "El grup s’ha tancat" else "L’estat del grup ha canviat",
                            concertId = group.concertId.takeIf { it > 0 },
                            sortKey = store.getLong(timestampKey, System.currentTimeMillis()),
                            communityGroupId = group.id
                        )
                    }
                    if (previous != group.status) store.edit().putString(key, group.status).apply()
                }
            }
            Handler(Looper.getMainLooper()).post {
                if (currentAccount?.serverUserId == ownerId && groups != null) {
                    eventCommunityNotifications = alerts
                    invalidateNotificationCache(0L)
                }
                eventCommunityRefreshInFlight = false
            }
        }
    }

    var attendanceByConcert by mutableStateOf<Map<Int, AttendanceStatus>>(emptyMap())
        private set

    var issuedTickets by mutableStateOf<List<PurchasedTicket>>(emptyList())
        private set

    var ticketPurchases by mutableStateOf<Set<Int>>(emptySet())
        private set

    /** Artistes seleccionats per l'usuari dins de cada festival per construir el seu horari personal. */
    var festivalArtistSelections by mutableStateOf<Map<Int, Set<String>>>(emptyMap())
        private set

    var ticketQuantityByConcert by mutableStateOf<Map<Int, Int>>(emptyMap())
        private set

    var accountProfiles by mutableStateOf<Map<String, AccountProfile>>(emptyMap())
        private set

    var savedConcerts by mutableStateOf<Set<Int>>(emptySet())
        private set

    var personalSavedLists by mutableStateOf<List<PersonalSavedList>>(emptyList())
        private set
    var friendsCircles by mutableStateOf<List<FriendsCircle>>(emptyList())
        private set
    var friendsCircleNotices by mutableStateOf<List<FriendsCircleNotice>>(emptyList())
        private set
    var friendMessages by mutableStateOf<Map<Int, List<FriendsCircleMessage>>>(emptyMap())
        private set
    var collectionsError by mutableStateOf<String?>(null)
        private set
    var savePickerEventId by mutableStateOf<Int?>(null)
        private set
    private var savedCollectionsRequestRevision = 0L
    fun openSavePicker(eventId: Int) {
        // Amb només General, guardem immediatament sense obligar a obrir un diàleg.
        if (personalSavedLists.none { it.owner }) {
            toggleSaved(eventId)
        } else savePickerEventId = eventId
    }
    fun closeSavePicker() { savePickerEventId = null }

    fun allCollectionSavedIds(): Set<Int> = buildSet {
        addAll(savedConcerts)
        personalSavedLists.forEach { addAll(it.eventIds) }
        // Els grups de Comunitat no són llistes personals de Guardats.
    }

    fun isSavedAnywhere(eventId: Int): Boolean = eventId in savedConcerts ||
        personalSavedLists.any { eventId in it.eventIds }

    /** La sessió anònima conserva el guardat general existent; les llistes remotes requerixen compte. */
    fun refreshSavedCollections(onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        // Només l'última consulta pot substituir la fotografia actual.
        val requestRevision = ++savedCollectionsRequestRevision
        if (currentAccount == null || ServerApiRepository.token(appContext).isNullOrBlank()) {
            personalSavedLists = emptyList()
            friendsCircles = emptyList()
            friendsCircleNotices = emptyList()
            friendMessages = emptyMap()
            invalidateNotificationCache(0L)
            return
        }
        val owner = currentAccount?.serverUserId
        backgroundExecutor.execute {
            val result = ServerApiRepository.getAuthenticatedJson(appContext, "collections/index.php")
            Handler(Looper.getMainLooper()).post {
                if (owner != currentAccount?.serverUserId || requestRevision != savedCollectionsRequestRevision) return@post
                result.onSuccess { root ->
                    fun jsonIds(source: JSONObject): Set<Int> {
                        val values = source.optJSONArray("event_ids") ?: JSONArray()
                        return buildSet {
                            for (i in 0 until values.length()) values.optInt(i).takeIf { it > 0 }?.let(::add)
                        }
                    }
                    val listValues = root.optJSONArray("lists") ?: JSONArray()
                    personalSavedLists = buildList {
                        for (i in 0 until listValues.length()) listValues.optJSONObject(i)?.let { obj ->
                            add(PersonalSavedList(
                                obj.optInt("id"), obj.optString("name"), jsonIds(obj),
                                obj.optBoolean("owner", true), obj.optString("invite_url"), obj.optString("share_code")
                            ))
                        }
                    }
                    val groupValues = root.optJSONArray("groups") ?: JSONArray()
                    friendsCircles = buildList {
                        for (i in 0 until groupValues.length()) groupValues.optJSONObject(i)?.let { obj ->
                            add(FriendsCircle(
                                obj.optInt("id"), obj.optString("name"), jsonIds(obj),
                                obj.optInt("members_count"), obj.optInt("unread_count"),
                                obj.optString("invite_url"), obj.optBoolean("creator")
                            ))
                        }
                    }
                    val notifications = root.optJSONArray("notices") ?: JSONArray()
                    friendsCircleNotices = buildList {
                        for (i in 0 until notifications.length()) notifications.optJSONObject(i)?.let { obj ->
                            add(FriendsCircleNotice(
                                obj.optInt("id"), obj.optInt("group_id"), obj.optString("group_name"),
                                obj.optInt("event_id").takeIf { it > 0 },
                                obj.optString("event_title"), obj.optString("created_at"),
                                obj.optString("kind", "EVENT_ADDED"),
                                obj.optString("message"), obj.optString("author")
                            ))
                        }
                    }
                    invalidateNotificationCache(0L)
                    collectionsError = null
                    onResult(true, "Llistes actualitzades.")
                }.onFailure { error ->
                    collectionsError = error.message ?: "No s’han pogut carregar les llistes."
                    onResult(false, collectionsError.orEmpty())
                }
            }
        }
    }

    /** Les modificacions s'escriuen al servidor abans de mostrar-les com a confirmades. */
    private fun writeCollection(action: String, body: JSONObject, onResult: (Boolean, String) -> Unit) {
        if (currentAccount == null) {
            onResult(false, "Cal iniciar sessió per compartir o crear llistes.")
            return
        }
        val owner = currentAccount?.serverUserId
        body.put("action", action)
        backgroundExecutor.execute {
            val result = ServerApiRepository.postAuthenticatedJson(appContext, "collections/index.php", body)
            Handler(Looper.getMainLooper()).post {
                if (owner != currentAccount?.serverUserId) return@post
                result.onSuccess { response ->
                    collectionsError = null
                    refreshSavedCollections()
                    onResult(true, response.optString("message", "Canvis guardats."))
                }.onFailure { error ->
                    collectionsError = error.message ?: "No s'ha pogut sincronitzar el canvi."
                    onResult(false, collectionsError.orEmpty())
                }
            }
        }
    }

    fun createPersonalSavedList(name: String, eventId: Int? = null, callback: (Boolean, String) -> Unit = { _, _ -> }) {
        if (name.trim().isBlank()) { callback(false, "Posa un nom a la llista."); return }
        val payload = JSONObject().put("name", name.trim())
        eventId?.takeIf { it > 0 }?.let { payload.put("event_id", it) }
        // Guardar en una llista pròpia és també un Guardat general: compta una sola vegada.
        if (eventId != null && eventId > 0 && eventId !in savedConcerts) toggleSaved(eventId)
        writeCollection("create_list", payload, callback)
    }
    fun renamePersonalSavedList(id: Int, name: String, callback: (Boolean, String) -> Unit = { _, _ -> }) {
        writeCollection("rename_list", JSONObject().put("list_id", id).put("name", name.trim()), callback)
    }
    fun deletePersonalSavedList(id: Int, callback: (Boolean, String) -> Unit = { _, _ -> }) {
        writeCollection("delete_list", JSONObject().put("list_id", id), callback)
    }
    fun setPersonalListEvent(listId: Int, eventId: Int, add: Boolean, callback: (Boolean, String) -> Unit = { _, _ -> }) {
        if (personalSavedLists.firstOrNull { it.id == listId }?.owner != true) {
            callback(false, "Només el creador pot editar esta llista compartida.")
            return
        }
        // Si és el primer guardat, actualitzem el comptador abans de la xarxa.
        // En traure'l només d'una llista no es desmarca automàticament de General.
        if (add && eventId !in savedConcerts) toggleSaved(eventId)
        writeCollection("list_event", JSONObject().put("list_id", listId).put("event_id", eventId).put("add", add), callback)
    }
    fun sharePersonalSavedList(id: Int, callback: (Boolean, String, String, String) -> Unit) {
        if (currentAccount == null || personalSavedLists.none { it.id == id && it.owner }) {
            callback(false, "No tens permís per compartir esta llista.", "", "")
            return
        }
        val owner = currentAccount?.serverUserId
        backgroundExecutor.execute {
            val result = ServerApiRepository.postAuthenticatedJson(appContext, "collections/index.php",
                JSONObject().put("action", "share_list").put("list_id", id))
            Handler(Looper.getMainLooper()).post {
                if (owner != currentAccount?.serverUserId) return@post
                result.onSuccess { response ->
                    refreshSavedCollections()
                    callback(true, "Llista preparada per compartir.", response.optString("invite_url"), response.optString("share_code"))
                }.onFailure { error ->
                    val detail = error.message.orEmpty()
                    val message = if (detail.contains("Acció no disponible", ignoreCase = true) ||
                        detail.contains("compartir no està disponible", ignoreCase = true)) {
                        "Cal actualitzar collections/index.php al servidor de l’API configurada i executar la migració SQL de llistes compartides."
                    } else detail.ifBlank { "No s'ha pogut compartir la llista." }
                    callback(false, message, "", "")
                }
            }
        }
    }
    fun joinSharedSavedList(codeOrToken: String, callback: (Boolean, String) -> Unit = { _, _ -> }) {
        writeCollection("join_list", JSONObject().put("invite", codeOrToken.trim()), callback)
    }
    fun leaveSharedSavedList(id: Int, callback: (Boolean, String) -> Unit = { _, _ -> }) {
        writeCollection("leave_list", JSONObject().put("list_id", id), callback)
    }
    fun revokePersonalSavedListSharing(id: Int, callback: (Boolean, String) -> Unit = { _, _ -> }) {
        writeCollection("revoke_list_share", JSONObject().put("list_id", id), callback)
    }
    fun createFriendsCircle(name: String, callback: (Boolean, String) -> Unit = { _, _ -> }) {
        if (name.trim().isBlank()) { callback(false, "Posa nom al grup d’amics."); return }
        writeCollection("create_group", JSONObject().put("name", name.trim()), callback)
    }
    fun joinFriendsCircle(token: String, callback: (Boolean, String) -> Unit = { _, _ -> }) {
        writeCollection("join_group", JSONObject().put("invite_token", token), callback)
    }
    fun leaveFriendsCircle(groupId: Int, callback: (Boolean, String) -> Unit = { _, _ -> }) {
        writeCollection("leave_group", JSONObject().put("group_id", groupId), callback)
    }
    fun deleteFriendsCircle(groupId: Int, callback: (Boolean, String) -> Unit = { _, _ -> }) {
        writeCollection("delete_group", JSONObject().put("group_id", groupId), callback)
    }
    fun rotateFriendsCircleInvite(groupId: Int, callback: (Boolean, String) -> Unit = { _, _ -> }) {
        writeCollection("rotate_invite", JSONObject().put("group_id", groupId), callback)
    }
    fun setFriendsCircleEvent(groupId: Int, eventId: Int, add: Boolean, callback: (Boolean, String) -> Unit = { _, _ -> }) {
        writeCollection("group_event", JSONObject().put("group_id", groupId).put("event_id", eventId).put("add", add), callback)
    }
    fun refreshFriendsCircleMessages(groupId: Int) {
        if (friendsCircles.none { it.id == groupId }) return
        val owner = currentAccount?.serverUserId
        backgroundExecutor.execute {
            val result = ServerApiRepository.getAuthenticatedJson(appContext, "collections/index.php?action=messages&group_id=$groupId")
            Handler(Looper.getMainLooper()).post {
                if (owner != currentAccount?.serverUserId) return@post
                result.onSuccess { response ->
                    val messages = response.optJSONArray("messages") ?: JSONArray()
                    val parsed = buildList {
                        for (i in 0 until messages.length()) messages.optJSONObject(i)?.let { obj ->
                            add(FriendsCircleMessage(
                                obj.optInt("id"), groupId, obj.optString("author"),
                                obj.optString("text"), obj.optInt("event_id").takeIf { it > 0 },
                                obj.optString("created_at")
                            ))
                        }
                    }
                    friendMessages = friendMessages + (groupId to parsed)
                }.onFailure { collectionsError = it.message }
            }
        }
    }
    fun sendFriendsCircleMessage(groupId: Int, text: String, callback: (Boolean, String) -> Unit = { _, _ -> }) {
        if (text.isBlank()) return
        writeCollection("send_message", JSONObject().put("group_id", groupId).put("text", text.trim()), callback)
    }
    fun markFriendsCircleRead(groupId: Int) {
        if (currentAccount == null) return
        if (friendsCircleNotices.any { it.groupId == groupId }) {
            friendsCircleNotices = friendsCircleNotices.filterNot { it.groupId == groupId }
            invalidateNotificationCache(0L)
        }
        backgroundExecutor.execute {
            val result = ServerApiRepository.postAuthenticatedJson(appContext, "collections/index.php",
                JSONObject().put("action", "mark_read").put("group_id", groupId))
            if (result.isSuccess) refreshSavedCollections()
        }
    }


    var likedConcerts by mutableStateOf<Set<Int>>(emptySet())
        private set

    var alertConcerts by mutableStateOf<Set<Int>>(emptySet())
        private set

    @Volatile private var firstRunChoicesRevision: Long = 0L
    var followedArtists by mutableStateOf<Set<String>>(emptySet())
        private set

    var followedOrganizers by mutableStateOf<Set<String>>(emptySet())
        private set

    var followedVenues by mutableStateOf<Set<String>>(emptySet())
        private set

    var followedCategories by mutableStateOf<Set<String>>(emptySet())
        private set

    var artistAlerts by mutableStateOf<Set<String>>(emptySet())
        private set

    var blockedArtists by mutableStateOf<Set<String>>(emptySet())
        private set

    /** Exclusions permanents, independents dels filtres temporals del cercador. */
    var hiddenConcertIds by mutableStateOf<Set<Int>>(emptySet())
        private set

    var blockedCategories by mutableStateOf<Set<CulturalCategory>>(emptySet())
        private set

    var blockedSubcategories by mutableStateOf<Set<String>>(emptySet())
        private set

    var userPreferences by mutableStateOf(UserPreferences())
        private set

    // Canvis de Llocs fets amb l'app oberta prevalen sobre una càrrega retardada.
    @Volatile private var userPreferencesWriteRevision = 0L
    private val placesSyncExecutor = Executors.newSingleThreadExecutor()
    private val feedPrefsSyncExecutor = Executors.newSingleThreadExecutor()
    var placesSyncError by mutableStateOf<String?>(null)
        private set
    var placesSavedOnServer by mutableStateOf(false)
        private set

    /** Server is the source of truth for all signed-in feed switches. Offline edits are journaled per account. */
    private fun synchronizeFeedPreferencesWithServer() {
        val name = currentAccount?.username ?: return
        val tokenAtRequest = ServerApiRepository.token(appContext)?.takeIf(String::isNotBlank) ?: return
        val revisionAtRequest = userPreferencesWriteRevision
        feedPrefsSyncExecutor.execute {
            if (ServerApiRepository.token(appContext) != tokenAtRequest) return@execute
            val dirtyKey = userCacheKey("feed_prefs_remote_dirty", name)
            val remoteResult = ServerApiRepository.fetchFeedPreferences(tokenAtRequest)
            if (remoteResult.isFailure) return@execute // offline: keep local journal for next login/foreground
            val remote = remoteResult.getOrThrow()
            if (ServerApiRepository.token(appContext) != tokenAtRequest) return@execute
            val pending = store.getBoolean(dirtyKey,false)
            if (pending || !remote.exists) {
                val local = loadUserPreferences(name)
                val result = ServerApiRepository.saveFeedPreferences(tokenAtRequest,local.showSuggestions,
                    local.discoveryLevel.name,local.valencianFirst)
                Handler(Looper.getMainLooper()).post {
                    if (currentAccount?.username != name || ServerApiRepository.token(appContext) != tokenAtRequest) return@post
                    if (result.isSuccess && revisionAtRequest == userPreferencesWriteRevision) {
                        store.edit().putBoolean(dirtyKey,false).apply()
                        expireForYouSnapshot(); invalidateFeedCache()
                    } else if (result.isFailure) {
                        store.edit().putBoolean(dirtyKey,true).apply()
                    } else synchronizeFeedPreferencesWithServer()
                }
            } else {
                Handler(Looper.getMainLooper()).post {
                    if (currentAccount?.username != name || ServerApiRepository.token(appContext) != tokenAtRequest) return@post
                    if (revisionAtRequest != userPreferencesWriteRevision || store.getBoolean(dirtyKey,false)) {
                        synchronizeFeedPreferencesWithServer(); return@post
                    }
                    val level = runCatching { DiscoveryLevel.valueOf(remote.discovery) }.getOrDefault(DiscoveryLevel.BALANCED)
                    if (userPreferences.showSuggestions != remote.suggestions ||
                        userPreferences.discoveryLevel != level || userPreferences.valencianFirst != remote.valencianFirst) {
                        userPreferencesWriteRevision += 1L
                        userPreferences = userPreferences.copy(showSuggestions=remote.suggestions,
                            discoveryLevel=level,valencianFirst=remote.valencianFirst)
                        persistFeedPreferencesLocally(name)
                        expireForYouSnapshot(); invalidateFeedCache()
                    }
                }
            }
        }
    }
    private fun persistFeedPreferencesLocally(name: String?) {
        store.edit()
            .putBoolean(userCacheKey("valencian_first",name),userPreferences.valencianFirst)
            .putBoolean(userCacheKey("show_suggestions",name),userPreferences.showSuggestions)
            .putString(userCacheKey("discovery_level",name),userPreferences.discoveryLevel.name)
            .commit()
    }

    /** Manté l'estat de Llocs entre dispositius, sense perdre canvis offline. */
    private fun synchronizePlacesWithServer() {
        val accountName = currentAccount?.username ?: return
        val sessionToken = ServerApiRepository.token(appContext)?.takeIf(String::isNotBlank) ?: return
        val revisionAtRequest = userPreferencesWriteRevision
        placesSyncExecutor.execute {
            // No es fan peticions per al compte anterior després d'un canvi de sessió.
            if (ServerApiRepository.token(appContext) != sessionToken) return@execute
            val pendingKey = userCacheKey("places_remote_dirty", accountName)
            // Consultem primer l'estat remot. Un compte nou pot tindre Llocs locals de v262.
            val remoteResult = ServerApiRepository.fetchLocationPreferences(sessionToken)
            if (remoteResult.isFailure) {
                Handler(Looper.getMainLooper()).post {
                    if (currentAccount?.username == accountName) {
                        placesSavedOnServer = false
                        placesSyncError = "Llocs guardats al mòbil; falta sincronitzar amb el servidor."
                    }
                }
                return@execute
            }
            val remote = remoteResult.getOrThrow()
            if (ServerApiRepository.token(appContext) != sessionToken) return@execute
            val local = loadUserPreferences(accountName)
            val pendingLocal = store.getBoolean(pendingKey, false)
            val needsSeed = !remote.exists && (local.preferredLocations.isNotEmpty() || local.preferredComarques.isNotEmpty())
            if (pendingLocal || needsSeed) {
                val result = ServerApiRepository.saveLocationPreferences(
                    sessionToken,
                    local.preferredLocations.associateWith { local.radiusForLocation(it) },
                    local.preferredComarques
                )
                Handler(Looper.getMainLooper()).post {
                    if (currentAccount?.username != accountName ||
                        ServerApiRepository.token(appContext) != sessionToken) return@post
                    if (result.isSuccess) {
                        placesSyncError = null
                        if (revisionAtRequest == userPreferencesWriteRevision) {
                            placesSavedOnServer = true
                            store.edit().putBoolean(pendingKey, false).commit()
                        } else {
                            placesSavedOnServer = false
                            // Editada durant la petició: enviar la versió més recent.
                            synchronizePlacesWithServer()
                        }
                    } else {
                        // Mantenim el canvi local en la cua per al pròxim intent.
                        store.edit().putBoolean(pendingKey, true).commit()
                        placesSavedOnServer = false
                        placesSyncError = "Llocs guardats al mòbil; falta sincronitzar amb el servidor."
                    }
                }
            } else if (remote.exists) {
                Handler(Looper.getMainLooper()).post {
                    if (currentAccount?.username != accountName ||
                        ServerApiRepository.token(appContext) != sessionToken) return@post
                    // Mai restaurar una fotografia remota sobre una edició recent.
                    if (revisionAtRequest != userPreferencesWriteRevision ||
                        store.getBoolean(pendingKey, false)) {
                        synchronizePlacesWithServer()
                        return@post
                    }
                    val newLocations = remote.towns.keys.toSet()
                    val newRadii = remote.towns
                    val newComarques = remote.comarques
                    if (userPreferences.preferredLocations != newLocations ||
                        userPreferences.preferredComarques != newComarques ||
                        userPreferences.locationRadiiKm != newRadii) {
                        userPreferencesWriteRevision += 1L
                        userPreferences = userPreferences.copy(
                            preferredLocations = newLocations,
                            preferredComarques = newComarques,
                            locationRadiiKm = newRadii
                        )
                        persistPlacesLocally(accountName)
                        invalidateFeedCache()
                    }
                    placesSyncError = null
                    placesSavedOnServer = true
                }
            } else {
                Handler(Looper.getMainLooper()).post {
                    if (currentAccount?.username == accountName) placesSyncError = null
                }
            }
        }
    }

    private fun persistPlacesLocally(username: String?) {
        store.edit()
            .putStringSet(userCacheKey("preferred_locations", username), userPreferences.preferredLocations.toSet())
            .putStringSet(userCacheKey("preferred_comarques", username), userPreferences.preferredComarques.toSet())
            .putString(userCacheKey("preferred_location_radii", username), JSONObject().apply {
                userPreferences.locationRadiiKm.forEach { (town, radius) ->
                    put(town, PersonalizationOptions.validRadius(radius))
                }
            }.toString())
            .commit()
    }

    /** Conserva el comportament v262 de convidat sense sobreescriure un compte ja existent. */
    private fun importGuestPlacesIfEmpty(guest: UserPreferences?) {
        val incoming = guest ?: return
        // El formulari v294 és una elecció explícita per a l'usuari nou.
        // En el PRIMER login, fusiona els llocs amb els que el compte ja tenia,
        // conserva els radis anteriors en cas de conflicte i marca el canvi com
        // pendent d'enviar a MariaDB. No copia els llocs del convidat a comptes
        // successius del mateix dispositiu.
        if (store.getBoolean("v294_first_run_done", false) &&
            store.contains("v294_intro_locations") &&
            !store.getBoolean("v294_intro_places_migrated", false) &&
            incoming.preferredLocations.isNotEmpty()) {
            userPreferencesWriteRevision += 1L
            userPreferences = userPreferences.copy(
                preferredLocations = userPreferences.preferredLocations + incoming.preferredLocations,
                preferredComarques = userPreferences.preferredComarques + incoming.preferredComarques,
                locationRadiiKm = incoming.locationRadiiKm + userPreferences.locationRadiiKm
            )
            persistPlacesLocally(currentAccount?.username)
            store.edit().putBoolean(userCacheKey("places_remote_dirty"), true)
                .putBoolean("v294_intro_places_migrated", true).commit()
            invalidateFeedCache()
            return
        }
        if (userPreferences.preferredLocations.isNotEmpty() || userPreferences.preferredComarques.isNotEmpty()) return
        if (incoming.preferredLocations.isEmpty() && incoming.preferredComarques.isEmpty()) return
        userPreferencesWriteRevision += 1L
        userPreferences = userPreferences.copy(
            preferredLocations = incoming.preferredLocations,
            preferredComarques = incoming.preferredComarques,
            locationRadiiKm = incoming.locationRadiiKm
        )
        // La fusió és només una proposta local de migració. Si el servidor tenia
        // una selecció anterior, la baixada remota preval; no la sobreescrivim.
        persistPlacesLocally(currentAccount?.username)
        invalidateFeedCache()
    }


    var alertPreferences by mutableStateOf(AlertPreferences())
        private set

    var emailPreferences by mutableStateOf(EmailPreferences())
        private set

    var emailPreferencesSyncing by mutableStateOf(false)
        private set

    var emailPreferencesMessage by mutableStateOf<String?>(null)
        private set

    var readNotificationIds by mutableStateOf<Set<String>>(emptySet())
        private set

    var dismissedNotificationIds by mutableStateOf<Set<String>>(emptySet())
        private set

    var concertChangeNotifications by mutableStateOf<List<AgendaNotification>>(emptyList())
        private set

    init {
        Log.d("AgendaPerf", "stateConstructed ${SystemClock.elapsedRealtime() - stateStartedAt}ms")
        // 10ah: el constructor no parseja caches grans ni inicia xarxa.
        // La primera composició només necessita la sessió i els estats locals menuts.
        hydrateStartupCachesAndAgenda()
        if (currentAccount != null) {
            Handler(Looper.getMainLooper()).postDelayed({ refreshFilterPresetsFromServer() }, 1_500L)
        }

        // El catàleg territorial quasi no canvia i no forma part del primer frame.
        if (!MunicipalityRepository.isCacheFresh(appContext)) {
            Handler(Looper.getMainLooper()).postDelayed({
                MunicipalityRepository.syncInBackground(appContext) { result ->
                    result.onSuccess { items ->
                        applyMunicipalityCatalog(items)
                        municipalitySyncError = null
                    }.onFailure { error ->
                        municipalitySyncError = error.message
                    }
                }
            }, 35_000L)
        }
    }

    fun remoteAgendaConfigured(): Boolean = RemoteAgendaRepository.isConfigured()

    /**
     * Arrancada ràpida: obri la UI abans de tocar SQLite/JSON. La cache es llig en un
     * fil de treball i, una vegada aplicada, es llança la sincronització incremental.
     */
    private fun hydrateStartupCachesAndAgenda() {
        // 10eo: ni tan sols el JSON menut de Batecs es parseja durant la construcció
        // de l'estat. Primer frame net; snapshot, fotos i cache completa entren després.
        Handler(Looper.getMainLooper()).postDelayed({
            backgroundExecutor.execute {
                val batecCacheStarted = SystemClock.elapsedRealtime()
                val startupSnapshot = runCatching { loadBatecStartupSnapshot() }.getOrDefault(emptyList())
                val photoCache = runCatching { loadBatecPhotoCache() }.getOrDefault(emptyMap())
                val cachedBatecs = if (ServerApiRepository.isConfigured()) {
                    runCatching { ServerApiRepository.loadCachedBatecs(appContext) }.getOrDefault(emptyList()).take(30)
                } else emptyList()
                Log.d("AgendaPerf", "batecLocalCache ${SystemClock.elapsedRealtime() - batecCacheStarted}ms · ${cachedBatecs.size} items")
                Handler(Looper.getMainLooper()).post {
                    if (startupSnapshot.isNotEmpty()) startupBatecSnapshot = startupSnapshot
                    if (photoCache.isNotEmpty()) {
                        batecPhotoByArtist = photoCache
                        batecPhotoCacheRevision += 1L
                    }
                    if (cachedBatecs.isNotEmpty() && stories.isEmpty()) stories = cachedBatecs
                }
            }
        }, 240L)

        // ENTITATS alimenta sobretot els fallbacks artista/sala/poble. Carreguem la
        // còpia local en els fast paths i, després del primer frame, la refresquem en
        // background si està vella. Quan canvia, només invalidem la capa visual/índexs:
        // no recalcularem ni reordenarem Per a tu.
        Handler(Looper.getMainLooper()).postDelayed({
            backgroundExecutor.execute {
                val entityWarmStarted = SystemClock.elapsedRealtime()
                val changes = runCatching { ServerApiRepository.refreshEntityCatalogChanges(appContext) }
                    .getOrDefault(ServerApiRepository.EntityCatalogChanges(false, false))
                runCatching { ServerApiRepository.cachedEntityNames(appContext, "ARTIST") }
                runCatching { ServerApiRepository.cachedArtistAffinityMetadata(appContext) }
                runCatching { rebuildBatecPhotoCache() }
                Log.d("AgendaPerf", "entityCatalogRefresh ${SystemClock.elapsedRealtime() - entityWarmStarted}ms · visuals=${changes.visualsChanged} affinities=${changes.affinitiesChanged}")
                Handler(Looper.getMainLooper()).post {
                    // v301: una actualització de fotos no llança tot el càlcul de Per a tu.
                    // Només la metadata cultural dels artistes altera la selecció.
                    if (changes.affinitiesChanged) {
                        expireForYouSnapshot()
                        scheduleVisibleConcertsRefresh(0L)
                    }
                    entityImageCatalogRevision += 1L
                    cachedSuggestionPoolRevision = -1L
                    cachedKnownArtistRevision = -1L
                    cachedFeedSearchIndex = null
                    searchSuggestionRevision += 1L
                }
            }
        }, 1_800L)

        // v261: eliminada la fotografia d'arrancada de 20 esdeveniments.
        // El feed comença amb la finestra disponible i acaba materialitzant el
        // catàleg complet; cap bloc de 20 suplanta la selecció de 100.

        // FASE 0.5: estat d'usuari menut després del primer frame. No bloqueja setContent.
        Handler(Looper.getMainLooper()).postDelayed({
            backgroundExecutor.execute {
                val userCacheStarted = SystemClock.elapsedRealtime()
                val firstRunRevisionAtRead = firstRunChoicesRevision
                val username = currentAccount?.username
                val cachedRecentSearchTerms = runCatching { loadStringSet(searchTermsKey(username)) }.getOrDefault(emptySet())
                val cachedSaved = runCatching { loadIntSet(userCacheKey("saved_concerts", username)) }.getOrDefault(emptySet())
                val cachedLiked = runCatching { loadIntSet(userCacheKey("liked_concerts", username)) }.getOrDefault(emptySet())
                val cachedAlerts = runCatching { loadIntSet(userCacheKey("alert_concerts", username)) }.getOrDefault(emptySet())
                val cachedFollowedArtists = runCatching { loadStringSet(userCacheKey("followed_artists", username)) }.getOrDefault(emptySet())
                val cachedFollowedOrganizers = runCatching { loadStringSet(userCacheKey("followed_organizers", username)) }.getOrDefault(emptySet())
                val cachedFollowedVenues = runCatching { loadStringSet(userCacheKey("followed_venues", username)) }.getOrDefault(emptySet())
                val cachedFollowedCategories = runCatching { loadStringSet(userCacheKey("followed_categories", username)) }.getOrDefault(emptySet())
                val cachedArtistAlerts = runCatching { loadStringSet(userCacheKey("artist_alerts", username)) }.getOrDefault(emptySet())
                val cachedBlockedArtists = runCatching { loadStringSet(userCacheKey("blocked_artists", username)) }.getOrDefault(emptySet())
                val cachedHiddenConcerts = runCatching { loadIntSet(userCacheKey("hidden_concerts", username)) }.getOrDefault(emptySet())
                val cachedBlockedCategories = runCatching { loadStringSet(userCacheKey("blocked_categories", username))
                    .mapNotNull { code -> enumValues<CulturalCategory>().firstOrNull { it.name == code } }.toSet()
                }.getOrDefault(emptySet())
                // v262: llegim preferències del compte capturat; un canvi fet des de Llocs
                // durant la hidratació no pot ser sobreescrit per la fotografia antiga.
                val preferencesAccountAtRead = currentAccount?.username
                val preferencesRevisionAtRead = userPreferencesWriteRevision
                val cachedUserPreferences = runCatching {
                    loadUserPreferences(preferencesAccountAtRead)
                }.getOrDefault(UserPreferences())
                val cachedAlertPreferences = runCatching { loadAlertPreferences() }.getOrDefault(AlertPreferences())
                val cachedEmailPreferences = runCatching { loadEmailPreferences() }.getOrDefault(EmailPreferences())
                val cachedReadNotifications = runCatching { loadStringSet("read_notification_ids") }.getOrDefault(emptySet())
                val cachedDismissedNotifications = runCatching { loadStringSet("dismissed_notification_ids") }.getOrDefault(emptySet())
                val cachedBehaviour = runCatching { loadBehaviourProfile(username) }.getOrDefault(UserBehaviourProfile(username.orEmpty()))
                val cachedAttendance = runCatching { loadAttendance() }.getOrDefault(emptyMap())
                val cachedTickets = runCatching { loadIssuedTickets() }.getOrDefault(emptyList())
                val cachedFestivalSelections = runCatching { loadFestivalArtistSelections() }.getOrDefault(emptyMap())
                Log.d("AgendaPerf", "userLocalCache ${SystemClock.elapsedRealtime() - userCacheStarted}ms")
                Handler(Looper.getMainLooper()).post {
                    recentSearchTerms = cachedRecentSearchTerms
                    savedConcerts = cachedSaved
                    likedConcerts = cachedLiked
                    alertConcerts = cachedAlerts
                    if (firstRunRevisionAtRead == firstRunChoicesRevision) {
                        followedArtists = cachedFollowedArtists
                        followedCategories = CategoryTaxonomy.canonicalizeFollowSet(cachedFollowedCategories)
                    }
                    followedOrganizers = cachedFollowedOrganizers
                    followedVenues = cachedFollowedVenues
                    artistAlerts = cachedArtistAlerts
                    blockedArtists = cachedBlockedArtists
                    hiddenConcertIds = cachedHiddenConcerts
                    blockedCategories = cachedBlockedCategories
                    blockedSubcategories = CategoryTaxonomy.canonicalizeFollowSet(loadStringSet(userCacheKey("blocked_subcategories", username)))
                    if (preferencesRevisionAtRead == userPreferencesWriteRevision &&
                        currentAccount?.username == preferencesAccountAtRead) {
                        userPreferences = cachedUserPreferences
                    }
                    alertPreferences = cachedAlertPreferences
                    emailPreferences = cachedEmailPreferences
                    readNotificationIds = cachedReadNotifications
                    dismissedNotificationIds = cachedDismissedNotifications
                    behaviourProfile = cachedBehaviour
                    attendanceByConcert = cachedAttendance
                    issuedTickets = cachedTickets
                    festivalArtistSelections = cachedFestivalSelections
                    refreshTicketPurchaseCache()
                    ticketQuantityByConcert = ticketQuantityMapForCurrentUser()
                    forYouSignalsReadyForProcess = true
                    feedHydrationTick += 1L
                    applyAutomaticDefaultFeedMode()
                    publishFastStartupForYouPreview()
                    // Migració sense perdre la caché del dispositiu v328: si la
                    // signatura completa antiga encaixa encara amb els gustos,
                    // habilitem el nou TTL estable i pintem els IDs existents.
                    if (store.getInt(forYouSnapshotHardSignatureKey(), Int.MIN_VALUE) == Int.MIN_VALUE &&
                        store.getInt(forYouSnapshotPreferenceSignatureKey(), Int.MIN_VALUE) == forYouPreferenceSignature() &&
                        loadForYouSnapshotTimestamp() > 0L &&
                        loadForYouSnapshot().isNotEmpty()) {
                        val migratedHardSignature = forYouHardPreferenceSignature()
                        val migratedOwner = currentAccount?.username?.lowercase() ?: "guest"
                        val migratedDay = agendaToday().toString()
                        store.edit().putInt(forYouSnapshotHardSignatureKey(), migratedHardSignature).apply()
                        startupPreviewExecutor.execute {
                            val migratedIds = loadForYouSnapshot()
                            val migratedCards = ServerApiRepository.loadCachedForYouPreview(appContext, migratedIds.take(20))
                            Handler(Looper.getMainLooper()).post {
                                startupForYouPreview = migratedCards
                                startupForYouPreviewAccount = migratedOwner
                                startupForYouPreviewDay = migratedDay
                                startupForYouPreviewHardSignature = migratedHardSignature
                                startupForYouPreviewReady = migratedCards.isNotEmpty()
                                publishFastStartupForYouPreview()
                            }
                        }
                    }
                    // Si l'usuari entra en un segon mòbil o recupera la instal·lació,
                    // el servidor pot contindre la selecció preparada. No espere en UI.
                    val savedAge = System.currentTimeMillis() - loadForYouSnapshotTimestamp()
                    val localCurrent = savedAge in 0 until 24L * 60L * 60L * 1000L &&
                        store.getInt(forYouSnapshotHardSignatureKey(), Int.MIN_VALUE) == forYouHardPreferenceSignature()
                    if (!localCurrent && currentAccount?.serverUserId?.let { it > 0 } == true &&
                        feedMode == FeedMode.FOR_YOU && ServerApiRepository.isConfigured()) {
                        val owner = currentAccount?.username?.lowercase() ?: "guest"
                        val sig = forYouHardPreferenceSignature()
                        val day = agendaToday().toString()
                        startupPreviewExecutor.execute {
                            val remote = ServerApiRepository.readPreparedForYouCache(appContext, sig, day)
                                .getOrNull()?.takeIf { it.ids.isNotEmpty() }
                            if (remote != null) {
                                val cards = ServerApiRepository.loadCachedForYouSelection(appContext, remote.ids)
                                Handler(Looper.getMainLooper()).post {
                                    if (currentAccount?.username?.lowercase() == owner &&
                                        forYouHardPreferenceSignature() == sig && agendaToday().toString() == day &&
                                        cards.isNotEmpty() && !cachedVisibleConcertsFinal) {
                                        if (remote.fresh) {
                                            store.edit().putString(forYouSnapshotKey(), remote.ids.joinToString(","))
                                                .putLong(forYouSnapshotTimestampKey(), System.currentTimeMillis())
                                                .putInt(forYouSnapshotHardSignatureKey(), sig).apply()
                                        }
                                        startupForYouPreview = cards
                                        startupForYouPreviewAccount = owner
                                        startupForYouPreviewDay = day
                                        startupForYouPreviewHardSignature = sig
                                        startupForYouPreviewReady = true
                                        publishFastStartupForYouPreview()
                                    }
                                }
                            }
                        }
                    }
                    cachedVisibleConcertsKey = null
                    // El càlcul personalitzat espera al catàleg complet.
                    // Els senyals locals es carreguen una sola vegada.
                    if (forYouWindowLoadedForProcess) scheduleVisibleConcertsRefresh(0L)
                    invalidateNotificationCache(250L)
                }
            }
        }, 120L)

        // Resultats de reports: comprovació pròpia i ràpida, també per a convidats.
        // No espera que el feed o la campaneta invaliden res.
        Handler(Looper.getMainLooper()).postDelayed({ refreshReportResults() }, 8_000L)

        // Estat anònim al servidor: no bloqueja l'arrancada i només s'aplica si continuem com a convidat.
        Handler(Looper.getMainLooper()).postDelayed({
            if (currentAccount == null && ServerApiRepository.isConfigured()) {
                backgroundExecutor.execute {
                    ServerApiRepository.fetchGuestState(appContext).onSuccess { remote ->
                        Handler(Looper.getMainLooper()).post {
                            if (currentAccount == null) {
                                mergeGuestStatePayload(remote, persist = true)
                                // El guest_id ja està confirmat i l'estat fusionat: tornem a mirar
                                // si hi ha una resolució pendent d'un report d'eixe dispositiu.
                                refreshReportResults()
                            }
                        }
                    }
                }
            }
        }, 10_000L)

        // v328: PRIMERA PINTURA sense esperar el parseig de centenars/milers de
        // candidats. Només 8 IDs de l'últim resultat comprovat i menys de 30 minuts,
        // per al mateix compte, dia, preferències i revisió SQLite. Els canvis
        // reals continuen obligant a validar tota la finestra i els seguits.
        Handler(Looper.getMainLooper()).postDelayed({
            startupPreviewExecutor.execute {
                val account = currentAccount?.username?.lowercase() ?: "guest"
                val day = agendaToday().toString()
                val timestamp = loadForYouSnapshotTimestamp()
                val age = System.currentTimeMillis() - timestamp
                // A diferència de v328, una sync incremental d'altres events no
                // invalida l'instantània durant 30 minuts.
                val savedHardSignature = store.getInt(forYouSnapshotHardSignatureKey(), Int.MIN_VALUE)
                // Després de 30 min la llista és STALE, no invisible:
                // es mostra filtrada mentre es recalcula en segon pla.
                val valid = timestamp > 0L && age in 0 until 24L * 60L * 60L * 1000L &&
                    savedHardSignature != Int.MIN_VALUE
                val ids = if (valid) loadForYouSnapshot() else emptyList()
                // Primer lot indexat de 20 targetes; no esperar el parseig del
                // snapshot complet per mostrar el primer scroll.
                val firstCards = if (ids.isNotEmpty()) runCatching {
                    ServerApiRepository.loadCachedForYouPreview(appContext, ids.take(20))
                }.getOrDefault(emptyList()) else emptyList()
                Handler(Looper.getMainLooper()).post {
                    startupForYouPreview = firstCards
                    startupForYouPreviewAccount = account
                    startupForYouPreviewDay = day
                    startupForYouPreviewHardSignature = savedHardSignature
                    startupForYouPreviewReady = firstCards.isNotEmpty()
                    publishFastStartupForYouPreview()
                }
                // A continuació, tota la mateixa llista, en el mateix executor
                // de disc i sense reavaluar preferències ni el catàleg.
                if (ids.size > 20) {
                    val allCards = runCatching {
                        ServerApiRepository.loadCachedForYouSelection(appContext, ids)
                    }.getOrDefault(emptyList())
                    Handler(Looper.getMainLooper()).post {
                        if (allCards.isNotEmpty()) {
                            startupForYouPreview = allCards
                            startupForYouPreviewReady = true
                            publishFastStartupForYouPreview()
                        }
                    }
                }
            }
        }, 100L)

        // FASE 1: recuperem la finestra local mentre el catàleg complet es prepara.
        // No hi ha cap top-N ni bloc de 20: la finestra és només una font transitòria.
        // La selecció de «Per a tu» només és definitiva amb el catàleg complet.
        val loadInitialAgendaWindow = {
            feedExecutor.execute {
                val started = SystemClock.elapsedRealtime()
                val today = agendaToday()
                val window = if (ServerApiRepository.isConfigured()) {
                    runCatching {
                        ServerApiRepository.loadCachedForYouWindowFast(
                            appContext,
                            today.minusMonths(1).toString(),
                            today.plusMonths(FOR_YOU_WINDOW_MONTHS).toString()
                        )
                    }.getOrDefault(emptyList())
                } else if (RemoteAgendaRepository.isConfigured()) {
                    runCatching { RemoteAgendaRepository.loadCached(appContext) }.getOrDefault(emptyList())
                } else emptyList()
                val preparedWindow = sanitizeRemoteOrganizerLinks(window)
                Log.d("AgendaPerf", "forYouWindow ${SystemClock.elapsedRealtime() - started}ms · ${preparedWindow.size} events")
                Handler(Looper.getMainLooper()).post {
                    val applyStarted = SystemClock.elapsedRealtime()
                    forYouWindowLoadedForProcess = true
                    // v329: conservar la instantània completa els 30 minuts. La
                    // lectura del catàleg no pot destruir-la en plena navegació.
                    feedHydrationTick += 1L
                    if (fullAgendaLoadedForProcess) {
                        // Mai substituir el catàleg complet per una finestra parcial.
                    } else if (preparedWindow.isNotEmpty()) {
                        applyRemoteConcertsPrepared(preparedWindow)
                    } else {
                        remoteConcerts = emptyList()
                        remoteConcertIds = emptySet()
                        concerts = initialConcerts()
                        invalidateFeedCache()
                    }
                    Log.d("AgendaPerf", "applyForYouWindowMain ${SystemClock.elapsedRealtime() - applyStarted}ms")
                    // v324: no carregar 1.000s d'activitats ni invalidar «Per a tu»
                    // abans que el primer lot i els seguits s'hagen dibuixat. El
                    // catàleg global s'hidrata sota demanda / en segon pla més tard.
                    // La finestra local ja és completa per al rang de dos mesos.
                    scheduleStartupRefresh(hasCachedAgenda = preparedWindow.isNotEmpty())
                }

            }
        }
        Handler(Looper.getMainLooper()).postDelayed({ loadInitialAgendaWindow() }, 140L)

        // Publicitat de capçalera: és cache local i és menuda. La carreguem després del
        // primer frame, no 8 segons més tard. Els concerts anunciats tenen cache pròpia
        // perquè un anunci no depenga de pertànyer al subconjunt de Per a tu (2 mesos).
        Handler(Looper.getMainLooper()).postDelayed({
            backgroundExecutor.execute {
                val adsStarted = SystemClock.elapsedRealtime()
                val cachedAds = runCatching { loadAdvertisements() }.getOrDefault(emptyList())
                val adConcertIds = cachedAds.asSequence().map { it.concertId }.filter { it > 0 }.distinct().toList()
                val cachedAdConcerts = if (adConcertIds.isNotEmpty() && ServerApiRepository.isConfigured()) {
                    runCatching { ServerApiRepository.loadCachedEventsByIds(appContext, adConcertIds) }.getOrDefault(emptyList())
                } else emptyList()
                Log.d(
                    "AgendaPerf",
                    "adsLocalCache ${SystemClock.elapsedRealtime() - adsStarted}ms · ${cachedAds.size} ads · ${cachedAdConcerts.size} concerts"
                )
                Handler(Looper.getMainLooper()).post {
                    advertisements = cachedAds
                    if (cachedAdConcerts.isNotEmpty()) {
                        advertisementConcertCache = cachedAdConcerts.associateBy { it.id }
                    }
                }
            }
        }, 220L)

        // Municipis és una cache local menuda però és clau per al quart fallback visual
        // (poble): permet resoldre des del principi nom valencià <-> nom oficial. No
        // reordena Per a tu; només força una nova resolució visual de les cards.
        Handler(Looper.getMainLooper()).postDelayed({
            backgroundExecutor.execute {
                val municipalStarted = SystemClock.elapsedRealtime()
                val cachedMunicipalities = runCatching { MunicipalityRepository.loadCached(appContext) }.getOrDefault(emptyList())
                Log.d("AgendaPerf", "municipalityLocalCache ${SystemClock.elapsedRealtime() - municipalStarted}ms · ${cachedMunicipalities.size}")
                if (cachedMunicipalities.isNotEmpty()) {
                    Handler(Looper.getMainLooper()).post {
                        applyMunicipalityCatalog(cachedMunicipalities)
                        entityImageCatalogRevision += 1L
                    }
                }
            }
        }, 360L)

        // FASE 3: caches auxiliars només quan Inici ja ha tingut temps de pintar.
        Handler(Looper.getMainLooper()).postDelayed({
            backgroundExecutor.execute {
                val auxiliaryStarted = SystemClock.elapsedRealtime()
                val cachedSubmissions = runCatching { loadEventSubmissions() }.getOrDefault(emptyList())
                val cachedProfiles = runCatching { loadAccountProfiles() }.getOrDefault(emptyMap())
                Log.d("AgendaPerf", "auxiliaryCaches ${SystemClock.elapsedRealtime() - auxiliaryStarted}ms")
                Handler(Looper.getMainLooper()).post {
                    eventSubmissions = cachedSubmissions
                    accountProfiles = cachedProfiles
                    invalidateNotificationCache(250L)
                }

                backgroundExecutor.execute {
                    // El catàleg d'imatges ja està en RAM des de l'arrancada. Ací només
                    // preparem els noms auxiliars, també fora del fil principal.
                    runCatching { ServerApiRepository.peekCachedEntityNames("ORGANIZER") }
                }
            }
        }, 6_000L)
    }

    /**
     * Materialitza el catàleg complet només quan una interacció el necessita.
     * L'arrancada normal treballa només amb la finestra temporal de Per a tu. Esta
     * lectura usa una cua separada i no pot bloquejar
     * `feedExecutor`, que continua reservat per al que l'usuari veu.
     */
    fun ensureFullAgendaLoaded(reason: String = "interaction") {
        if (!ServerApiRepository.isConfigured() && !RemoteAgendaRepository.isConfigured()) return
        if (fullAgendaLoadedForProcess || fullAgendaLoadInFlight) return
        fullAgendaLoadInFlight = true
        catalogExecutor.execute {
            val started = SystemClock.elapsedRealtime()
            val full = runCatching {
                if (ServerApiRepository.isConfigured()) ServerApiRepository.loadCachedEventsFast(appContext)
                else RemoteAgendaRepository.loadCached(appContext)
            }.getOrDefault(emptyList())
            val preparedFull = sanitizeRemoteOrganizerLinks(full)
            val elapsed = SystemClock.elapsedRealtime() - started
            Log.d("AgendaPerf", "fullAgenda[$reason] ${elapsed}ms · ${preparedFull.size} events")
            Handler(Looper.getMainLooper()).post {
                fullAgendaLoadInFlight = false
                if (preparedFull.isNotEmpty()) {
                    fullAgendaLoadedForProcess = true
                    feedHydrationTick += 1L
                    val applyStarted = SystemClock.elapsedRealtime()
                    applyRemoteConcertsPrepared(preparedFull)
                    // La selecció d'un artista ha de poder trobar concerts que encara
                    // no hagen entrat en la cache local: consulta el servidor si falten.
                    refreshMissingSelectedArtistFromServer()
                    Log.d("AgendaPerf", "applyFullAgenda[$reason] ${SystemClock.elapsedRealtime() - applyStarted}ms")
                } else if (ServerApiRepository.isFirstLoadCachePersisting()) {
                    // Primer ús: el feed ràpid pot arribar abans que s'acabe de persistir
                    // TOT el catàleg. No donar per final una cerca feta sobre eixe preview.
                    Handler(Looper.getMainLooper()).postDelayed({
                        ensureFullAgendaLoaded("first_load_cache_persisted")
                    }, 350L)
                }
            }
        }
    }

    fun requestMoreAgendaForFeed() {
        ensureFullAgendaLoaded("scroll_end")
    }

    private fun scheduleStartupRefresh(hasCachedAgenda: Boolean) {
        val handler = Handler(Looper.getMainLooper())
        val loggedIn = currentAccount != null && !ServerApiRepository.token(appContext).isNullOrBlank()

        // Amb cache, cap xarxa competeix amb el primer frame ni amb les primeres imatges.
        // Sense cache, l'agenda remota sí s'avança perquè és necessària per tindre contingut.
        if (ServerApiRepository.isConfigured()) {
            handler.postDelayed({ refreshAdvertisementsFromServer() }, if (hasCachedAgenda) 2_500L else 1_000L)
            // Amb cache local, els Batecs ja estan visibles: la xarxa espera perquè no
            // competisca amb el primer scroll ni amb les miniatures.
            val hasInstantBatecs = startupBatecSnapshot.isNotEmpty() || stories.isNotEmpty()
            handler.postDelayed(
                { refreshBatecsFromServer() },
                when {
                    hasInstantBatecs -> 18_000L
                    hasCachedAgenda -> 18_000L
                    else -> 900L
                }
            )
        }
        val agendaAgeMillis = (System.currentTimeMillis() - lastAgendaSyncMillis).coerceAtLeast(0L)
        val agendaAutoRefreshNeeded = !hasCachedAgenda || lastAgendaSyncMillis <= 0L || agendaAgeMillis >= 2L * 60L * 1000L
        if (agendaAutoRefreshNeeded) {
            // Una cache existent és immediatament usable. La sincronització pesada espera
            // i només corre si la còpia local realment està vella.
            handler.postDelayed({ refreshRemoteAgenda() }, if (hasCachedAgenda) 3_000L else 500L)
        }

        if (loggedIn) {
            handler.postDelayed({ refreshTicketsForCurrentUser() }, if (hasCachedAgenda) 16_000L else 1_800L)
            handler.postDelayed({ refreshUserStateFromServer() }, if (hasCachedAgenda) 23_000L else 2_200L)
            handler.postDelayed({ synchronizePlacesWithServer() }, if (hasCachedAgenda) 10_000L else 2_500L)
            handler.postDelayed({ synchronizeFeedPreferencesWithServer() }, if (hasCachedAgenda) 11_000L else 2_600L)
        }
        if (ServerApiRepository.isConfigured()) {
            handler.postDelayed({ refreshUserProfilesFromServer() }, 21_000L)
            if (currentAccount != null) handler.postDelayed({ refreshEmailPreferences() }, 25_000L)
        }
    }

    fun refreshAllContent(forceForYouRefresh: Boolean = false) {
        if (forceForYouRefresh) expireForYouSnapshot()
        refreshRemoteAgenda()
        if (ServerApiRepository.isConfigured()) {
            refreshUserProfilesFromServer()
            refreshAdvertisementsFromServer()
            refreshBatecsFromServer()
            refreshCommunityGroups()
        }
    }

    fun refreshUserProfilesFromServer() {
        if (!ServerApiRepository.isConfigured()) return
        backgroundExecutor.execute {
            val result = ServerApiRepository.fetchUserProfiles(appContext)
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { remote ->
                    if (remote.isNotEmpty()) {
                        val updated = accountProfiles.toMutableMap()
                        remote.forEach { profile ->
                            val local = updated[profile.username] ?: AccountProfile(profile.username, profile.displayName)
                            val merged = local.copy(
                                displayName = profile.displayName.ifBlank { local.displayName },
                                bio = profile.bio,
                                photoUri = profile.imageUrl,
                                homeTown = profile.homeTown.ifBlank { local.homeTown },
                                discipline = profile.discipline,
                                companyName = profile.companyName,
                                castText = profile.castText,
                                publisherName = profile.publisherName,
                                booksText = profile.booksText,
                                worksText = profile.worksText,
                                spacesText = profile.spacesText
                            )
                            updated[profile.username] = merged
                            persistProfile(merged)
                        }
                        accountProfiles = updated
                    }
                }
            }
        }
    }

    private var queuedFullCatalogReconciliation = false
    private var lastLookupReconciliationAttemptElapsed = 0L

    /**
     * La cerca i els perfils consulten TOT el catàleg; no poden quedar-se indefinidament
     * amb una revisió local incompleta. Les consultes consecutives es coalescen i la xarxa
     * continua fora del fil principal. Cap dada de Per a tu s'usa com a filtre.
     */
    fun reconcileCatalogForLookup() {
        if (!ServerApiRepository.isConfigured()) return
        val now = SystemClock.elapsedRealtime()
        if (lastLookupReconciliationAttemptElapsed > 0 &&
            now - lastLookupReconciliationAttemptElapsed < 90_000L) return
        lastLookupReconciliationAttemptElapsed = now
        refreshRemoteAgenda(forceFullCatalog = true)
    }

    fun refreshRemoteAgenda(forceFullCatalog: Boolean = false) {
        if (isAgendaRefreshing) {
            if (forceFullCatalog) queuedFullCatalogReconciliation = true
            return
        }
        if (!RemoteAgendaRepository.isConfigured()) {
            agendaSyncError = "Falta configurar la URL externa de l'agenda."
            return
        }
        isAgendaRefreshing = true
        agendaSyncError = null
        backgroundExecutor.execute {
            val result = RemoteAgendaRepository.sync(appContext, forceFullCatalog = forceFullCatalog)
            if (result.isSuccess && currentAccount?.role == AccountRole.SUPER_ADMIN) {
                // Manté el catàleg API d'artistes, sales i pobles alineat també amb
                // esdeveniments antics que existien abans de la migració d'imatges.
                runCatching {
                    ServerApiRepository.ensureEntitiesForEvents(appContext, result.getOrThrow().concerts)
                }
            }
            val preparedSuccess = result.getOrNull()?.let { synced ->
                // La sincronització actualitza SQLite. Si continuem en el mode lleuger,
                // hem de tornar a materialitzar EXACTAMENT la mateixa finestra completa
                // de Per a tu (2 mesos), no l'antic top-160. Reduir-la a 160 després del
                // sync feia que el feed canviara de mida, invalidara la caché i recalculara
                // una tercera vegada. El catàleg complet continua reservat al cercador/
                // Sense filtre.
                val uiEvents = if (fullAgendaLoadedForProcess) {
                    synced.concerts
                } else {
                    val today = agendaToday()
                    runCatching {
                        ServerApiRepository.loadCachedForYouWindowFast(
                            appContext,
                            today.minusMonths(1).toString(),
                            today.plusMonths(FOR_YOU_WINDOW_MONTHS).toString()
                        )
                    }.getOrDefault(emptyList()).ifEmpty {
                        synced.concerts.filter(::isInsideForYouWindow)
                    }
                }
                synced to sanitizeRemoteOrganizerLinks(uiEvents)
            }
            val fallbackPrepared = if (result.isFailure) {
                val cached = runCatching { RemoteAgendaRepository.loadCached(appContext) }.getOrDefault(emptyList())
                val scoped = if (fullAgendaLoadedForProcess) cached else cached.filter(::isInsideForYouWindow)
                sanitizeRemoteOrganizerLinks(scoped)
            } else emptyList()
            Handler(Looper.getMainLooper()).post {
                if (preparedSuccess != null) {
                    val (synced, preparedConcerts) = preparedSuccess
                    val wasFeedEmpty = concerts.isEmpty()
                    val applyStarted = SystemClock.elapsedRealtime()
                    applyRemoteConcertsPrepared(preparedConcerts)
                    Log.d("AgendaPerf", "applyRemoteAgendaMain ${SystemClock.elapsedRealtime() - applyStarted}ms · ${preparedConcerts.size} events")
                    if (wasFeedEmpty && preparedConcerts.isNotEmpty()) {
                        Log.d("AgendaPerf", "firstFeedFromNetwork ${SystemClock.elapsedRealtime() - stateStartedAt}ms · ${preparedConcerts.size} events")
                    }
                    lastAgendaSyncMillis = synced.syncedAtMillis
                    agendaSyncError = null
                    // v324: la sincronització ja ha refrescat la finestra local.
                    // No forcem ací una segona lectura GLOBAL que trencaria la
                    // memòria de «Per a tu» abans que l'usuari puga navegar.
                    // El cercador i la càrrega diferida obrin el catàleg complet.
                } else {
                    val error = result.exceptionOrNull()
                    agendaSyncError = error?.message ?: "No s'ha pogut actualitzar l'agenda."
                    if (fallbackPrepared.isNotEmpty()) applyRemoteConcertsPrepared(fallbackPrepared)
                }
                isAgendaRefreshing = false
                // Si la cerca ha sol·licitat una reconciliació mentre hi havia
                // una sincronització incremental en curs, executar-la després.
                if (queuedFullCatalogReconciliation) {
                    queuedFullCatalogReconciliation = false
                    Handler(Looper.getMainLooper()).postDelayed({
                        refreshRemoteAgenda(forceFullCatalog = true)
                    }, 100L)
                }
                // Inclús amb error de xarxa cal acabar la recàrrega explícita:
                // el resultat visible serà el catàleg local que ja tinguem.
                if (pendingReadingRefreshToken != null) {
                    // No tanquem el token ací: esperar el resultat del feed.
                    scheduleVisibleConcertsRefresh(0L)
                }
            }
        }
    }

    /** Recarrega la cache escrita per WorkManager sense necessitat de xarxa. */
    fun reloadRemoteAgendaCache() {
        feedExecutor.execute {
            val cached = sanitizeRemoteOrganizerLinks(RemoteAgendaRepository.loadCached(appContext))
            val syncMillis = RemoteAgendaRepository.lastSyncMillis(appContext)
            Handler(Looper.getMainLooper()).post {
                if (cached.isNotEmpty()) applyRemoteConcertsPrepared(cached)
                lastAgendaSyncMillis = syncMillis
            }
        }
    }

    private fun initialConcerts(): List<Concert> {
        val byId = linkedMapOf<Int, Concert>()
        // Producció: els esdeveniments visibles provenen de l'API/cache remota.
        // DemoData ja no s'utilitza com a fallback de l'agenda.
        remoteConcerts.forEach { byId[it.id] = it }
        eventSubmissions
            .filter { it.status == EventSubmissionStatus.PUBLISHED }
            .map(::concertFromSubmission)
            .forEach { byId[it.id] = it }
        return byId.values.toList()
    }

    /**
     * Un ARTIST/GRUP no és organitzador d'un esdeveniment alié.
     * Per aplicar esta regla no cal consultar tot ENTITATS: només cal mirar el cartell
     * del mateix esdeveniment. Si hi ha més d'un artista, cap d'ells figura com a
     * organitzador; si és una actuació en solitari, sí que pot mantindre's.
     */
    private fun sanitizeEventOrganizers(
        artists: List<String>,
        organizers: List<String>
    ): List<String> {
        val cleanArtists = artists
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinctBy(::normalizeSearchValue)
        val lineupKeys = cleanArtists.map(::normalizeSearchValue).filter(String::isNotBlank).toSet()
        val soloArtistKey = lineupKeys.singleOrNull()
        return organizers
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinctBy(::normalizeSearchValue)
            .filter { organizer ->
                val key = normalizeSearchValue(organizer)
                key !in lineupKeys || (soloArtistKey != null && key == soloArtistKey)
            }
    }

    private fun sanitizeRemoteOrganizerLinks(events: List<Concert>): List<Concert> {
        if (events.isEmpty()) return events
        return events.map { event ->
            val safeOrganizers = sanitizeEventOrganizers(event.lineup, event.organizers)
            if (safeOrganizers == event.organizers) event
            else event.copy(
                organizers = safeOrganizers,
                organizerImageUri = event.organizerImageUri.takeIf { safeOrganizers.isNotEmpty() }
            )
        }
    }

    /** Aplica una llista ja sanejada. Esta funció és Main-safe i no fa I/O. */
    private fun applyRemoteConcertsPrepared(newRemote: List<Concert>) {
        // Un sync incremental sense canvis no ha d'invalidar ni recompondre tot el feed.
        if (remoteConcerts.size == newRemote.size && remoteConcerts == newRemote) return

        // Canvi de finestra/cache completa: si no hi ha contingut local separat,
        // remoteConcerts és tota la font actual i es pot substituir O(1) sense map fusion.
        val hasPublishedLocalSubmissions = eventSubmissions.any { it.status == EventSubmissionStatus.PUBLISHED }
        val hasNonRemoteConcerts = concerts.any { it.id !in remoteConcertIds }
        if (remoteConcerts.isNotEmpty() && !hasPublishedLocalSubmissions && !hasNonRemoteConcerts) {
            remoteConcerts = newRemote
            remoteConcertIds = newRemote.asSequence().map { it.id }.toSet()
            concerts = newRemote
            batecAgendaRevision += 1L
            invalidateFeedCache()
            return
        }

        // Arrancada: la cache remota ja ve materialitzada. Si encara no hi havia agenda,
        // evitem construir i fusionar mapes de centenars d'elements al Main thread.
        if (remoteConcerts.isEmpty() && concerts.isEmpty() && eventSubmissions.none { it.status == EventSubmissionStatus.PUBLISHED }) {
            remoteConcerts = newRemote
            remoteConcertIds = newRemote.asSequence().map { it.id }.toSet()
            concerts = newRemote
            batecAgendaRevision += 1L
            invalidateFeedCache()
            return
        }

        val oldRemoteIds = remoteConcertIds
        val byId = linkedMapOf<Int, Concert>()
        // Conserva únicament contingut local creat/editat que no provenia de l'última
        // sincronització. No es reintrodueixen esdeveniments demo.
        concerts.filterNot { it.id in oldRemoteIds }.forEach { byId[it.id] = it }
        newRemote.forEach { byId[it.id] = it }
        eventSubmissions
            .filter { it.status == EventSubmissionStatus.PUBLISHED }
            .map(::concertFromSubmission)
            .forEach { byId[it.id] = it }
        remoteConcerts = newRemote
        remoteConcertIds = newRemote.mapTo(mutableSetOf()) { it.id }
        concerts = byId.values.toList()
        batecAgendaRevision += 1L
        invalidateFeedCache()
    }

    fun municipalityFor(name: String): Municipality? {
        val key = normalizeSearchValue(name)
        if (key.isBlank()) return null
        val source = municipalities
        if (municipalityLookupSource !== source) {
            synchronized(municipalityLookupLock) {
                if (municipalityLookupSource !== source) {
                    val rebuilt = HashMap<String, Municipality>(source.size * 2)
                    source.forEach { municipality ->
                        normalizeSearchValue(municipality.name).takeIf(String::isNotBlank)?.let { rebuilt.putIfAbsent(it, municipality) }
                        normalizeSearchValue(municipality.officialName).takeIf(String::isNotBlank)?.let { rebuilt.putIfAbsent(it, municipality) }
                        municipality.otherNames.split('|', ';').forEach { alias ->
                            normalizeSearchValue(alias).takeIf(String::isNotBlank)?.let { rebuilt.putIfAbsent(it, municipality) }
                        }
                    }
                    municipalityLookup = rebuilt
                    municipalityLookupSource = source
                }
            }
        }
        return municipalityLookup[key]
    }

    fun comarcaForTown(name: String): String? =
        municipalityFor(name)?.comarca?.takeIf { it.isNotBlank() }

    /** Comarques reals del catàleg municipal i dels esdeveniments, sense noms fabricats. */
    fun knownComarques(): List<String> = (
        MunicipalityRepository.bundledComarques() +
        municipalityAutocompleteItems().map { it.comarca } +
            concerts.map { it.comarca }
        ).map(String::trim).filter { it.isNotEmpty() &&
            !it.equals("Sense comarca", ignoreCase = true)
        }.distinctBy(::normalizeSearchValue).sortedBy(::normalizeSearchValue)

    fun canonicalComarca(name: String): String? {
        val key = normalizeSearchValue(name)
        return knownComarques().firstOrNull { normalizeSearchValue(it) == key }
    }

    fun publicPhotoForComarca(comarca: String): String? {
        entityImageCatalogRevision
        // Si el catàleg fotogràfic oferix una imatge pròpia de comarca, usar-la.
        // Mai assignar-li una fotografia arbitrària d'un poble veí.
        return ServerApiRepository.peekCachedEntityImageUrl("COMARCA", comarca)
    }

    /**
     * Catàleg per a l'autocompletat. La font principal és el catàleg oficial de municipis.
     * Si encara s'està sincronitzant o no hi ha cache, almenys conserva els municipis
     * dels esdeveniments reals ja carregats perquè el formulari no quede sense suggerències.
     */
    fun municipalityAutocompleteItems(): List<Municipality> {
        if (municipalities.isNotEmpty()) return municipalities
        return concerts.asSequence()
            .filter { it.town.isNotBlank() }
            .map {
                Municipality(
                    code = "agenda:${it.town.lowercase()}",
                    name = it.town,
                    comarca = it.comarca,
                    province = "",
                    latitude = 0.0,
                    longitude = 0.0
                )
            }
            .distinctBy { it.name.lowercase() }
            .sortedBy { it.name.lowercase() }
            .toList()
    }

    // v320: esdeveniment que ha sol·licitat venda interna (mai bloqueja l'app).
    var pendingInternalSaleEventId by mutableStateOf(0)
        private set
    fun rememberInternalSaleEvent(eventId: Int) {
        if (eventId > 0) pendingInternalSaleEventId = eventId
    }
    fun applySellerProfessionalRole() {
        val current = currentAccount ?: return
        if (current.role == AccountRole.USER) {
            currentAccount = current.copy(role = AccountRole.PROMOTER)
            store.edit().putString("role", "PROFESSIONAL").apply()
        }
        refreshPublisherAccess()
    }

    var guestClaimNotice by mutableStateOf<String?>(null)
        private set

    /** Guarda la propietat dels esdeveniments convidats en el compte autenticat. */
    private fun claimGuestPublicationsAfterAuthentication() {
        if (ServerApiRepository.pendingGuestClaimCount(appContext) == 0) return
        backgroundExecutor.execute {
            val result = ServerApiRepository.claimPendingGuestEvents(appContext)
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { outcome ->
                    if (outcome.claimed > 0) {
                        refreshPublisherAccess()
                        refreshPublicationTrust()
                        refreshRemoteAgenda()
                    }
                    guestClaimNotice = if (outcome.failed > 0) {
                        "${outcome.failed} publicacio(ns) encara pendent(s) de vincular. Conserva este dispositiu i torna a intentar-ho."
                    } else if (outcome.claimed > 0) "${outcome.claimed} esdeveniment(s) vinculats al teu compte." else null
                }.onFailure { error ->
                    guestClaimNotice = "No s'han pogut vincular els esdeveniments: ${error.message ?: "error de connexio"}"
                }
            }
        }
    }

    fun retryPendingGuestClaims() = claimGuestPublicationsAfterAuthentication()
    fun clearGuestClaimNotice() { guestClaimNotice = null }

    fun registerUser(
        email: String,
        username: String,
        password: String,
        categories: Set<CulturalCategory>,
        artists: Set<String>,
        preferredTowns: Set<String>,
        notifyConcerts: Boolean,
        notifySavedReminders: Boolean,
        notifyTickets: Boolean,
        onResult: (Boolean) -> Unit = {}
    ) {
        if (isLoggingIn) return
        isLoggingIn = true
        loginError = null
        backgroundExecutor.execute {
            val result = ServerApiRepository.register(
                appContext, email, username, password, categories, artists,
                notifyConcerts, false, false, false
            )
            // 0.20.8.10l: el registre ja ha guardat el token; demanem al servidor
            // el correu de benvinguda + verificació sense bloquejar l'accés a l'app.
            if (result.isSuccess) {
                runCatching { ServerApiRepository.sendWelcomeVerificationEmail(appContext).getOrThrow() }
            }
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { logged ->
                    val guestBeforeLogin = if (currentAccount == null) buildGuestStatePayload() else null
                    if (guestBeforeLogin != null) pendingGuestMerge = guestBeforeLogin
                    val guestPlacesBeforeLogin = if (currentAccount == null) userPreferences else null
                    val account = logged.account
                    currentAccount = account
                    savedFilterPresets = loadSavedFilterPresets()
                    isFilterPresetSaving = false
                    savedFilterSyncError = null
                    activeFilterPresetName = null
                    restoreLearningResetSyncStatus(account.username)
                    placesSavedOnServer = false
                    placesSyncError = null
                    // No compartim Llocs accidentalment entre comptes. Si l'usuari
                    // seleccionà Llocs de convidat i el compte encara no en tenia,
                    // els importem sense xafar preferències antigues del compte.
                    userPreferencesWriteRevision += 1L
                    userPreferences = loadUserPreferences(account.username)
                    importGuestPlacesIfEmpty(guestPlacesBeforeLogin)
                    loadUserStateCache(account.username)
                    behaviourProfile = loadBehaviourProfile(account.username)
                    recentSearchTerms = loadStringSet(searchTermsKey(account.username))
                    store.edit().putString("session_username", account.username).apply()

                    followedArtists = artists
                    // Si l'usuari ve del formulari inicial, conservar EXACTAMENT els
                    // codis de subcategoria triats com a convidat. No convertir-los
                    // tots en un seguiment complet de disciplina.
                    followedCategories = CategoryTaxonomy.canonicalizeFollowSet(
                        guestBeforeLogin?.let { jsonStringSet(it, "followed_categories") }.orEmpty()
                    )
                    saveStringSet(userCacheKey("followed_artists"), followedArtists)
                    saveStringSet(userCacheKey("followed_categories"), followedCategories)
                    followedCategories.forEach { syncCategoryFollow(it, true) }
                    // Un compte nou sense artistes ni cerques comença en Sense filtre.
                    selectDefaultFeedModeForCurrentUser()
                    updateUserPreferences(
                        userPreferences.copy(
                            preferredLocations = userPreferences.preferredLocations + preferredTowns,
                            favoriteArtists = userPreferences.favoriteArtists + artists
                        )
                    )

                    updateAlertPreferences(
                        alertPreferences.copy(
                            followedArtist = notifyConcerts,
                            dayBefore = notifySavedReminders,
                            ticketUpdates = notifyTickets,
                            favoriteStyle = false,
                            homeTown = false,
                            homeComarca = false,
                            communityMessages = false,
                            changes = false
                        )
                    )
                    store.edit().putBoolean("alert_followed_news", false).apply()
                    categories.forEach { recordBehaviour(BehaviourKind.CATEGORY, it.name, serverAction = "SEARCH_RELATED") }
                    artists.forEach { recordBehaviour(BehaviourKind.FOLLOWED_ARTIST, it) }
                    preferredTowns.forEach { town ->
                        recordBehaviour(BehaviourKind.TOWN, town, serverAction = "SEARCH_RELATED")
                        comarcaForTown(town)?.let { recordBehaviour(BehaviourKind.COMARCA, it, serverAction = "SEARCH_RELATED") }
                    }

                    loginError = null
                    // 0.20.8.10cr: allibera Login i navega a Inici ABANS de les
                    // sincronitzacions posteriors. Eixes càrregues són auxiliars i no
                    // poden retenir l'usuari en la pantalla d'autenticació.
                    isLoggingIn = false
                    // Vinculacio posterior a registre/login: mai abans de publicar.
                    claimGuestPublicationsAfterAuthentication()
                    onResult(true)
                    refreshUserStateFromServer()
                    refreshFilterPresetsFromServer()
                    synchronizePlacesWithServer()
                    synchronizeFeedPreferencesWithServer()
                    refreshEmailPreferences()
                    refreshAdvertisementsFromServer()
                    refreshBatecsFromServer()
                    refreshCommunityGroups()
                    refreshRemoteAgenda()
                }.onFailure { error ->
                    loginError = error.message ?: "No s'ha pogut crear el compte."
                    isLoggingIn = false
                    onResult(false)
                }
            }
        }
    }

    fun requestPasswordReset(email: String, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        val cleanEmail = email.trim()
        if (!cleanEmail.contains("@")) {
            onResult(false, "Escriu un correu electrònic vàlid.")
            return
        }
        backgroundExecutor.execute {
            val result = ServerApiRepository.requestPasswordReset(cleanEmail)
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { message -> onResult(true, message) }
                    .onFailure { error -> onResult(false, error.message ?: "No s'ha pogut enviar el correu.") }
            }
        }
    }

    fun loginWithGoogle(idToken: String, nonce: String, onResult: (Boolean) -> Unit = {}) {
        if (isLoggingIn) return
        if (idToken.isBlank() || nonce.isBlank()) {
            loginError = "No s'ha pogut obtindre una credencial vàlida de Google."
            onResult(false)
            return
        }
        isLoggingIn = true
        loginError = null
        backgroundExecutor.execute {
            val result = ServerApiRepository.loginWithGoogle(appContext, idToken, nonce)
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { logged ->
                    val guestBeforeLogin = if (currentAccount == null) buildGuestStatePayload() else null
                    if (guestBeforeLogin != null) pendingGuestMerge = guestBeforeLogin
                    val guestPlacesBeforeLogin = if (currentAccount == null) userPreferences else null
                    val account = logged.account
                    currentAccount = account
                    savedFilterPresets = loadSavedFilterPresets()
                    isFilterPresetSaving = false
                    savedFilterSyncError = null
                    activeFilterPresetName = null
                    restoreLearningResetSyncStatus(account.username)
                    placesSavedOnServer = false
                    placesSyncError = null
                    // No compartim Llocs accidentalment entre comptes. Si l'usuari
                    // seleccionà Llocs de convidat i el compte encara no en tenia,
                    // els importem sense xafar preferències antigues del compte.
                    userPreferencesWriteRevision += 1L
                    userPreferences = loadUserPreferences(account.username)
                    importGuestPlacesIfEmpty(guestPlacesBeforeLogin)
                    loadUserStateCache(account.username)
                    behaviourProfile = loadBehaviourProfile(account.username)
                    recentSearchTerms = loadStringSet(searchTermsKey(account.username))
                    selectDefaultFeedModeForCurrentUser()
                    store.edit().putString("session_username", account.username).apply()
                    loginError = null
                    isLoggingIn = false
                    // Vinculacio posterior a registre/login: mai abans de publicar.
                    claimGuestPublicationsAfterAuthentication()
                    onResult(true)
                    refreshTicketPurchaseCache()
                    refreshTicketsForCurrentUser()
                    refreshUserStateFromServer()
                    refreshFilterPresetsFromServer()
                    synchronizePlacesWithServer()
                    synchronizeFeedPreferencesWithServer()
                    refreshEmailPreferences()
                    refreshAdvertisementsFromServer()
                    refreshBatecsFromServer()
                    refreshCommunityGroups()
                    refreshRemoteAgenda()
                }.onFailure { error ->
                    loginError = error.message ?: "No s'ha pogut iniciar sessió amb Google."
                    isLoggingIn = false
                    onResult(false)
                }
            }
        }
    }

    fun login(username: String, password: String, onResult: (Boolean) -> Unit = {}) {
        if (isLoggingIn) return
        if (username.isBlank() || password.isBlank()) {
            loginError = "Escriu usuari i contrasenya."
            onResult(false)
            return
        }
        isLoggingIn = true
        loginError = null
        backgroundExecutor.execute {
            val result = ServerApiRepository.login(appContext, username, password)
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { logged ->
                    val guestBeforeLogin = if (currentAccount == null) buildGuestStatePayload() else null
                    if (guestBeforeLogin != null) pendingGuestMerge = guestBeforeLogin
                    val guestPlacesBeforeLogin = if (currentAccount == null) userPreferences else null
                    val account = logged.account
                    currentAccount = account
                    savedFilterPresets = loadSavedFilterPresets()
                    isFilterPresetSaving = false
                    savedFilterSyncError = null
                    activeFilterPresetName = null
                    restoreLearningResetSyncStatus(account.username)
                    placesSavedOnServer = false
                    placesSyncError = null
                    // No compartim Llocs accidentalment entre comptes. Si l'usuari
                    // seleccionà Llocs de convidat i el compte encara no en tenia,
                    // els importem sense xafar preferències antigues del compte.
                    userPreferencesWriteRevision += 1L
                    userPreferences = loadUserPreferences(account.username)
                    importGuestPlacesIfEmpty(guestPlacesBeforeLogin)
                    loadUserStateCache(account.username)
                    behaviourProfile = loadBehaviourProfile(account.username)
                    recentSearchTerms = loadStringSet(searchTermsKey(account.username))
                    selectDefaultFeedModeForCurrentUser()
                    store.edit().putString("session_username", account.username).apply()
                    loginError = null
                    isLoggingIn = false
                    // Vinculacio posterior a registre/login: mai abans de publicar.
                    claimGuestPublicationsAfterAuthentication()
                    onResult(true)
                    refreshTicketPurchaseCache()
                    refreshTicketsForCurrentUser()
                    refreshUserStateFromServer()
                    refreshFilterPresetsFromServer()
                    synchronizePlacesWithServer()
                    synchronizeFeedPreferencesWithServer()
                    refreshEmailPreferences()
                    refreshAdvertisementsFromServer()
                    refreshBatecsFromServer()
                    refreshCommunityGroups()
                    refreshRemoteAgenda()
                }.onFailure { error ->
                    loginError = error.message ?: "No s'ha pogut iniciar sessió."
                    isLoggingIn = false
                    onResult(false)
                }
            }
        }
    }

    /** Exigix contrasenya en cada accés a la gestió d'entrades, sense guardar-la. */
    fun verifyTicketManagerPassword(password: String, onResult: (Boolean, String) -> Unit) {
        val account = currentAccount
        if (account == null || !isAuthorizedPublisher() || password.isBlank()) {
            onResult(false, "Inicia sessió amb un compte professional i escriu la contrasenya.")
            return
        }
        val expectedId = account.serverUserId ?: account.id
        backgroundExecutor.execute {
            val result = ServerApiRepository.verifyTicketManagerCredentials(account.username, password)
            Handler(Looper.getMainLooper()).post {
                val allowed = result.getOrNull() == expectedId &&
                    currentAccount?.let { (it.serverUserId ?: it.id) == expectedId } == true && isAuthorizedPublisher()
                onResult(allowed, if (allowed) "Identitat verificada." else "Contrasenya incorrecta o permisos insuficients.")
            }
        }
    }

    fun logout() {
        resetLocalSocialCounters()
        val oldAccount = currentAccount
        currentAccount = null
        publicationTrust = null
        publicationTrustOwnerId = null
        savedFilterPresets = emptyList()
        isFilterPresetSaving = false
        savedFilterSyncError = null
        activeFilterPresetName = null
        learningResetGeneration += 1L
        learningResetSyncPending = false
        learningResetFeedback = null
        placesSavedOnServer = false
        placesSyncError = null
        userPreferencesWriteRevision += 1L
        userPreferences = loadUserPreferences(null)
        behaviourProfile = loadBehaviourProfile(null)
        recentSearchTerms = loadStringSet(searchTermsKey(null))
        ticketPurchases = emptySet()
        ticketQuantityByConcert = emptyMap()
        festivalArtistSelections = loadFestivalArtistSelections()
        savedConcerts = loadIntSet(userCacheKey("saved_concerts", null))
        likedConcerts = loadIntSet(userCacheKey("liked_concerts", null))
        alertConcerts = loadIntSet(userCacheKey("alert_concerts", null))
        followedArtists = loadStringSet(userCacheKey("followed_artists", null))
        followedOrganizers = loadStringSet(userCacheKey("followed_organizers", null))
        followedVenues = loadStringSet(userCacheKey("followed_venues", null))
        followedCategories = CategoryTaxonomy.canonicalizeFollowSet(loadStringSet(userCacheKey("followed_categories", null)))
        artistAlerts = loadStringSet(userCacheKey("artist_alerts", null))
        selectDefaultFeedModeForCurrentUser()
        blockedArtists = loadStringSet(userCacheKey("blocked_artists", null))
        hiddenConcertIds = loadIntSet(userCacheKey("hidden_concerts", null))
        blockedSubcategories = CategoryTaxonomy.canonicalizeFollowSet(loadStringSet(userCacheKey("blocked_subcategories", null)))
        blockedCategories = loadStringSet(userCacheKey("blocked_categories", null))
            .mapNotNull { code -> enumValues<CulturalCategory>().firstOrNull { it.name == code } }.toSet()
        onVisibilityExclusionsChanged()
        attendanceByConcert = loadAttendance()
        seenBatecKeys = emptySet()
        viewedBatecArtistsAt = loadViewedBatecArtistsAt()
        startupBatecSnapshot = loadBatecStartupSnapshot()
        recentBatecEventCreations = emptyMap()
        communityGroups = emptyList()
        communityChatMessages = emptyList()
        communityRequestsByGroup = emptyMap()
        store.edit().remove("session_username").apply()
        if (oldAccount != null) {
            backgroundExecutor.execute { ServerApiRepository.logout(appContext) }
        } else {
            ServerApiRepository.clearSession(appContext)
        }
    }

    fun canCreateEvent(): Boolean = true

    /** USER base pot proposar; la publicació immediata queda reservada als perfils autoritzats. */
    fun isAuthorizedPublisher(): Boolean = currentAccount?.role?.let { it != AccountRole.USER } == true

    // v304: separar la condició de professional de l'autorització d'autopublicar.
    // Sense una resposta vigent del servidor, només Superadmin pot publicar directament.
    var publicationTrust by mutableStateOf<ServerApiRepository.PublicationTrust?>(null)
        private set
    var publicationTrustOwnerId by mutableStateOf<Int?>(null)
        private set
    @Volatile private var publicationTrustRefreshInFlight = false

    fun canAutoPublishEvents(): Boolean {
        val account = currentAccount ?: return false
        val trust = publicationTrust
        return com.example.agendavalenciana.model.EventPublishingPolicy.canPublishDirectly(
            superAdmin = account.role == AccountRole.SUPER_ADMIN,
            signedInUserId = account.serverUserId,
            trustOwnerUserId = publicationTrustOwnerId,
            approvedCount = trust?.approvedCount ?: 0,
            autoPublishEnabled = trust?.autoPublishEnabled == true,
            suspendedByReport = trust?.suspendedByReport == true
        )
    }

    fun refreshPublicationTrust() {
        val account = currentAccount ?: run {
            publicationTrust = null
            publicationTrustOwnerId = null
            return
        }
        val uid = account.serverUserId ?: return
        if (publicationTrustRefreshInFlight || !ServerApiRepository.isConfigured()) return
        // Es nega autopublicar durant la revalidació per evitar drets antics.
        publicationTrust = null
        publicationTrustOwnerId = null
        publicationTrustRefreshInFlight = true
        backgroundExecutor.execute {
            val result = ServerApiRepository.fetchPublicationTrust(appContext)
            Handler(Looper.getMainLooper()).post {
                publicationTrustRefreshInFlight = false
                if (currentAccount?.serverUserId != uid) return@post
                result.onSuccess { trust ->
                    publicationTrust = trust
                    publicationTrustOwnerId = uid
                }
            }
        }
    }

    @Volatile private var publisherAccessRefreshInFlight = false

    /**
     * Reconsulta el rol en obrir Publicar. Així una verificació manual entra en vigor
     * sense obligar l'usuari a tancar sessió i tornar a entrar.
     */
    fun refreshPublisherAccess() {
        val account = currentAccount ?: return
        if (publisherAccessRefreshInFlight || account.serverUserId == null || !ServerApiRepository.isConfigured()) return
        publisherAccessRefreshInFlight = true
        backgroundExecutor.execute {
            val result = ServerApiRepository.refreshPublisherAccess(appContext)
            Handler(Looper.getMainLooper()).post {
                publisherAccessRefreshInFlight = false
                result.onSuccess { access ->
                    val before = currentAccount ?: return@onSuccess
                    if (before.serverUserId != account.serverUserId) return@onSuccess
                    val updated = before.copy(
                        role = access.role,
                        displayName = access.displayName?.takeIf(String::isNotBlank) ?: before.displayName,
                        managedArtists = access.managedArtists
                    )
                    if (updated != before) {
                        currentAccount = updated
                        val previousProfile = accountProfiles[before.username]
                        if (previousProfile != null && previousProfile.displayName != updated.displayName) {
                            accountProfiles = accountProfiles + (before.username to previousProfile.copy(displayName = updated.displayName))
                        }
                    }
                }
            }
        }
    }

    fun updateSearch(
        value: String,
        selectedArtist: String? = null,
        selectedType: FeedSearchSuggestionType? = null
    ) {
        // v293: en llevar una cerca es torna explícitament a «Per a tu».
        // També llevem els filtres que encara estigueren actius, per a no deixar
        // una vista filtrada després de netejar el cercador.
        if (value.isBlank() && search.isNotBlank() && selectedArtist.isNullOrBlank()) {
            selectFeedMode(FeedMode.FOR_YOU)
            return
        }
        val explicitArtist = selectedArtist?.trim()?.takeIf(String::isNotBlank)
        val selectedEntityType = selectedType.takeIf { value.isNotBlank() }
        if (search == value && selectedFeedSearchArtist == explicitArtist &&
            selectedFeedSearchType == selectedEntityType) return
        search = value
        selectedFeedSearchArtist = explicitArtist
        selectedFeedSearchType = selectedEntityType
        synchronizeCriteriaFeedMode()
        if (value.isNotBlank()) ensureFullAgendaLoaded("search")
        feedCriteriaChanged()
    }

    /** Guarda una cerca i resol l'entitat fora del fil principal. */
    fun commitSearch() {
        val raw = search.trim()
        // El resultat visual ja es calcula en segon pla des d'updateSearch().
        scheduleVisibleConcertsRefresh(0L)
        recordSearchTerm(raw)
        if (raw.isBlank()) return

        // 10bp: la resolució exacta abans recorria municipis, artistes, sales i
        // organitzadors al Main thread just en prémer Cerca. Ara és telemetria asíncrona.
        val searchGeneration = learningResetGeneration
        backgroundExecutor.execute {
            val resolution = runCatching { resolveCommittedSearch(raw) }.getOrNull()
            Handler(Looper.getMainLooper()).post {
                if (searchGeneration != learningResetGeneration) return@post
                resolution?.let { resolved ->
                    recordBehaviour(resolved.kind, resolved.value, serverAction = resolved.serverAction)
                    resolved.relatedComarca?.takeIf(String::isNotBlank)?.let { comarca ->
                        recordBehaviour(BehaviourKind.COMARCA, comarca, serverAction = "SEARCH_RELATED")
                    }
                }
            }
        }
    }

    private data class CommittedSearchResolution(
        val kind: BehaviourKind,
        val value: String,
        val serverAction: String,
        val relatedComarca: String? = null
    )

    private fun resolveCommittedSearch(raw: String): CommittedSearchResolution {
        val normalized = normalizeSearchValue(raw)

        municipalityFor(raw)?.let { municipality ->
            return CommittedSearchResolution(
                BehaviourKind.TOWN, municipality.name, "SEARCH_ENTITY", municipality.comarca
            )
        }

        municipalities.asSequence().map { it.comarca }.filter(String::isNotBlank)
            .firstOrNull { normalizeSearchValue(it) == normalized }?.let {
                return CommittedSearchResolution(BehaviourKind.COMARCA, it, "SEARCH_RELATED")
            }

        knownArtistNames().firstOrNull { normalizeSearchValue(it) == normalized }?.let {
            return CommittedSearchResolution(BehaviourKind.ARTIST, it, "SEARCH_ENTITY")
        }

        concerts.asSequence().map { it.venue.trim() }.filter(String::isNotBlank)
            .firstOrNull { normalizeSearchValue(it) == normalized }?.let {
                return CommittedSearchResolution(BehaviourKind.VENUE, it, "SEARCH_ENTITY")
            }

        val professional = DemoData.accounts.firstOrNull { account ->
            account.role in setOf(AccountRole.AGENCY, AccountRole.LABEL, AccountRole.VENUE, AccountRole.PROMOTER) &&
                (normalizeSearchValue(account.username) == normalized || normalizeSearchValue(profileFor(account.username).displayName) == normalized)
        }
        if (professional != null) {
            return CommittedSearchResolution(
                BehaviourKind.ORGANIZER,
                profileFor(professional.username).displayName,
                "SEARCH_ENTITY"
            )
        }

        sequence {
            ServerApiRepository.cachedEntityNames(appContext, "FESTIVAL").forEach { yield(it) }
            ServerApiRepository.cachedEntityNames(appContext, "ORGANIZER").forEach { yield(it) }
        }.firstOrNull { normalizeSearchValue(it) == normalized }?.let {
            // El backend conserva FESTIVAL com a entitat pròpia; l'afinitat utilitza
            // ORGANIZER per compatibilitat amb el model de seguiments actual.
            return CommittedSearchResolution(BehaviourKind.ORGANIZER, it, "SEARCH_ENTITY")
        }

        CulturalCategory.entries.firstOrNull {
            normalizeSearchValue(it.label) == normalized || normalizeSearchValue(it.name) == normalized
        }?.let {
            return CommittedSearchResolution(BehaviourKind.CATEGORY, it.name, "SEARCH_ENTITY")
        }

        concerts.asSequence().flatMap { it.styles.asSequence() }.map(String::trim).filter(String::isNotBlank)
            .firstOrNull { normalizeSearchValue(it) == normalized }?.let {
                return CommittedSearchResolution(BehaviourKind.STYLE, it, "SEARCH_ENTITY")
            }

        return CommittedSearchResolution(BehaviourKind.SEARCH, raw.lowercase(), "SEARCH_FREE")
    }

    fun recordSearchTerm(value: String) {
        val clean = value.trim().lowercase()
        if (clean.length < 2) return
        val previousTerms = recentSearchTerms
        recentSearchTerms = (recentSearchTerms.toList().filterNot { it == clean } + clean).takeLast(60).toSet()
        saveStringSet(searchTermsKey(currentAccount?.username), recentSearchTerms)
        invalidateFollowSuggestions()
        applyAutomaticDefaultFeedMode()
        if (recentSearchTerms != previousTerms) {
            // Les cerques són senyals d'aprenentatge, NO criteris explícits.
            // S'aplicaran al pròxim recàlcul de 30 min, sense destruir l'agenda.
            invalidateFollowSuggestions()
        }
        recordBehaviour(BehaviourKind.SEARCH, clean, serverAction = "SEARCH_RAW")
    }

    fun removeRecentSearchTerm(value: String) {
        val clean = value.trim().lowercase()
        if (clean !in recentSearchTerms) return
        recentSearchTerms = recentSearchTerms - clean
        saveStringSet(searchTermsKey(currentAccount?.username), recentSearchTerms)
        invalidateFollowSuggestions()
        applyAutomaticDefaultFeedMode()
        // Traure una cerca recent és aprenentatge implícit: el feed vigent dura 30 minuts.
    }

    /**
     * v266 · Oblida només l'aprenentatge implícit per cerques i visites.
     * Les preferències explícites (seguides, llocs, guardats i exclusions) no es toquen.
     * El tall temporal evita reimportar afinitats remotes obsoletes si hi ha xarxa lenta.
     */
    fun resetLearnedPersonalization() {
        val username = currentAccount?.username
        val resetAt = System.currentTimeMillis()
        learningResetGeneration += 1L
        val resetGeneration = learningResetGeneration
        store.edit()
            .putLong(learningResetKey(username), resetAt)
            .remove(searchTermsKey(username))
            .apply()
        recentSearchTerms = emptySet()
        val explicit = setOf(
            BehaviourKind.FOLLOWED_ARTIST, BehaviourKind.FOLLOWED_ORGANIZER,
            BehaviourKind.FOLLOWED_CATEGORY, BehaviourKind.SAVED_EVENT,
            BehaviourKind.WANT_TO_GO, BehaviourKind.GOING,
            BehaviourKind.MAX_DISTANCE, BehaviourKind.MAX_PRICE, BehaviourKind.FREE_ONLY
        )
        behaviourProfile = behaviourProfile.copy(
            events = behaviourProfile.events.filter { it.kind in explicit },
            updatedAtMillis = resetAt
        )
        saveBehaviourProfile(behaviourProfile)

        // Una petició antiga en la cua offline no pot recrear les afinitats reiniciades.
        synchronized(affinityBatchLock) { pendingAffinityBatch.clear() }
        synchronized(store) {
            val queued = runCatching { JSONArray(store.getString(pendingPostsKey(username), "[]")) }
                .getOrDefault(JSONArray())
            val retained = JSONArray()
            for (i in 0 until queued.length()) {
                val item = queued.optJSONObject(i) ?: continue
                val path = item.optString("path")
                if (path != "affinities/record.php" && path != "affinities/batch.php" &&
                    path != "affinities/reset-learning.php") retained.put(item)
            }
            store.edit().putString(pendingPostsKey(username), retained.toString()).apply()
        }

        invalidateFollowSuggestions()
        expireForYouSnapshot()
        applyAutomaticDefaultFeedMode()
        if (feedMode == FeedMode.FOR_YOU) ensureFullAgendaLoaded("v267_reset_learning")
        scheduleVisibleConcertsRefresh(0L)
        invalidateNotificationCache(0L)
        if (username.isNullOrBlank()) {
            learningResetSyncPending = false
            learningResetFeedback = "Gustos apresos reiniciats en este dispositiu."
            scheduleGuestStateSync()
            return
        }
        if (ServerApiRepository.token(appContext).isNullOrBlank()) {
            learningResetSyncPending = true
            learningResetFeedback = "Reinici fet al mòbil. Pendent de sincronitzar amb el compte."
            enqueuePendingServerPost("affinities/reset-learning.php", JSONObject())
            return
        }
        learningResetSyncPending = true
        learningResetFeedback = "Gustos reiniciats. Sincronitzant amb el servidor…"
        backgroundExecutor.execute {
            val result = ServerApiRepository.postAuthenticatedJson(
                appContext, "affinities/reset-learning.php", JSONObject()
            )
            if (result.isFailure) enqueuePendingServerPost("affinities/reset-learning.php", JSONObject())
            Handler(Looper.getMainLooper()).post {
                if (learningResetGeneration != resetGeneration || currentAccount?.username != username) return@post
                learningResetSyncPending = result.isFailure
                learningResetFeedback = if (result.isSuccess)
                    "Gustos apresos reiniciats també al servidor."
                else "Reinici fet al mòbil. El servidor està pendent d'actualitzar."
                if (result.isSuccess) backgroundExecutor.execute { flushPendingServerPosts() }
            }
        }
    }

    private fun restoreLearningResetSyncStatus(username: String) {
        val queued = runCatching { JSONArray(store.getString(pendingPostsKey(username), "[]")) }
            .getOrDefault(JSONArray())
        learningResetSyncPending = (0 until queued.length()).any { index ->
            queued.optJSONObject(index)?.optString("path") == "affinities/reset-learning.php"
        }
        learningResetFeedback = if (learningResetSyncPending)
            "Reinici local guardat. Pendent de sincronitzar els gustos amb el servidor."
        else null
    }

    private fun learningResetKey(username: String?): String =
        "implicit_learning_reset_v266_${username?.trim()?.lowercase().takeUnless { it.isNullOrBlank() } ?: "guest"}"

    /** Suggeriments vius del cercador principal: territori, artistes i professionals. */
    @Synchronized
    private fun feedSearchSuggestionPool(): List<IndexedFeedSearchSuggestion> {
        val agendaRevision = feedDataRevision
        val territoryRevision = municipalityCatalogRevision
        if (cachedSuggestionPoolRevision == agendaRevision &&
            cachedSuggestionPoolMunicipalityRevision == territoryRevision) return cachedSuggestionPool
        val values = mutableListOf<FeedSearchSuggestion>()
        knownArtistNames().forEach { artist ->
            values += FeedSearchSuggestion(FeedSearchSuggestionType.ARTIST, artist, "Grup / artista")
        }
        val municipalitiesIndex = municipalityAutocompleteItems()
        municipalitiesIndex.forEach { municipality ->
            val subtitle = listOf(
                municipality.officialName.takeIf { it.isNotBlank() && !it.equals(municipality.name, true) },
                municipality.comarca,
                municipality.province
            ).filterNotNull().filter(String::isNotBlank).distinct().joinToString(" · ")
            values += FeedSearchSuggestion(FeedSearchSuggestionType.MUNICIPALITY, municipality.name, subtitle)
        }
        municipalitiesIndex.asSequence().map { it.comarca.trim() }.filter(String::isNotBlank)
            .distinctBy(::normalizeSearchValue).forEach { comarca ->
                values += FeedSearchSuggestion(FeedSearchSuggestionType.COMARCA, comarca, "Comarca")
            }
        val professionalAccounts = buildList {
            addAll(DemoData.accounts.filter { it.role in setOf(AccountRole.AGENCY, AccountRole.LABEL, AccountRole.VENUE, AccountRole.PROMOTER) })
            currentAccount?.takeIf { it.role in setOf(AccountRole.AGENCY, AccountRole.LABEL, AccountRole.VENUE, AccountRole.PROMOTER) }?.let(::add)
        }.distinctBy { it.username.lowercase() }
        professionalAccounts.forEach { account ->
            val display = profileFor(account.username).displayName.ifBlank { account.displayName }
            values += FeedSearchSuggestion(FeedSearchSuggestionType.COMPANY, display, account.roleLabel)
        }
        ServerApiRepository.cachedEntityNames(appContext, "FESTIVAL").forEach { display ->
            values += FeedSearchSuggestion(FeedSearchSuggestionType.COMPANY, display, "Festival")
        }
        ServerApiRepository.cachedEntityNames(appContext, "ORGANIZER").forEach { display ->
            values += FeedSearchSuggestion(FeedSearchSuggestionType.COMPANY, display, "Organitzador / agència")
        }
        concerts.asSequence().flatMap { it.organizers.asSequence() }.map(String::trim).filter(String::isNotBlank)
            .distinctBy(::normalizeSearchValue).forEach { display ->
                values += FeedSearchSuggestion(FeedSearchSuggestionType.COMPANY, display, "Organitzador / agència")
            }
        concerts.asSequence().flatMap { it.coEditorUsernames.asSequence() }.distinct().forEach { username ->
            val display = profileFor(username).displayName.ifBlank { username }
            values += FeedSearchSuggestion(FeedSearchSuggestionType.COMPANY, display, "Professional / coeditor")
        }
        ServerApiRepository.cachedEntityNames(appContext, "VENUE").forEach { venue ->
            values += FeedSearchSuggestion(FeedSearchSuggestionType.VENUE, venue, "Sala / espai")
        }
        concerts.asSequence().map { it.venue.trim() }.filter(String::isNotBlank)
            .distinctBy(::normalizeSearchValue).forEach { venue ->
                values += FeedSearchSuggestion(FeedSearchSuggestionType.VENUE, venue, "Sala / espai")
            }
        cachedSuggestionPool = values
            // Per als pobles homònims, la comarca del subtítol diferencia entrades.
            .distinctBy {
                Triple(it.type, normalizeSearchValue(it.value),
                    if (it.type == FeedSearchSuggestionType.MUNICIPALITY) it.subtitle else "")
            }
            .map { IndexedFeedSearchSuggestion(it, normalizeSearchValue(it.value)) }
        cachedSuggestionPoolRevision = agendaRevision
        cachedSuggestionPoolMunicipalityRevision = territoryRevision
        return cachedSuggestionPool
    }

    /**
     * Suggeriments vius amb índex normalitzat. Manté només els millors `limit` candidats
     * mentre recorre la llista: no ordena milers de coincidències per a després mostrar-ne 8.
     */
    fun feedSearchSuggestions(rawQuery: String, limit: Int = 12): List<FeedSearchSuggestion> {
        val query = normalizeSearchValue(rawQuery)
        if (query.isBlank() || limit <= 0) return emptyList()
        data class Ranked(val rank: Int, val typeRank: Int, val suggestion: FeedSearchSuggestion)
        fun typeRank(type: FeedSearchSuggestionType): Int = when (type) {
            FeedSearchSuggestionType.ARTIST -> 0
            FeedSearchSuggestionType.MUNICIPALITY -> 1
            FeedSearchSuggestionType.COMARCA -> 2
            FeedSearchSuggestionType.COMPANY -> 3
            FeedSearchSuggestionType.VENUE -> 4
            FeedSearchSuggestionType.STYLE -> 5
        }
        val comparator = compareBy<Ranked> { it.rank }
            .thenBy { it.typeRank }
            .thenBy { it.suggestion.value.lowercase() }
        val best = ArrayList<Ranked>(limit)
        feedSearchSuggestionPool().forEach { indexed ->
            val normalized = indexed.normalizedValue
            val rank = when {
                normalized == query -> 0
                normalized.startsWith(query) -> 1
                normalized.contains(query) -> 2
                else -> return@forEach
            }
            val candidate = Ranked(rank, typeRank(indexed.suggestion.type), indexed.suggestion)
            var insertAt = best.binarySearch(candidate, comparator)
            if (insertAt < 0) insertAt = -insertAt - 1
            if (insertAt < limit) {
                best.add(insertAt, candidate)
                if (best.size > limit) best.removeAt(best.lastIndex)
            }
        }
        return best.map { it.suggestion }
    }

    fun selectFeedSearchSuggestion(suggestion: FeedSearchSuggestion) {
        // La selecció d'un suggeriment és una cerca real: ha d'invalidar/refrescar
        // el feed igual que prémer la tecla Cerca.
        updateSearch(
            suggestion.value,
            selectedArtist = suggestion.value.takeIf { suggestion.type == FeedSearchSuggestionType.ARTIST },
            selectedType = suggestion.type
        )
        recordSearchTerm(suggestion.value)
        when (suggestion.type) {
            FeedSearchSuggestionType.MUNICIPALITY -> {
                recordBehaviour(BehaviourKind.TOWN, suggestion.value, serverAction = "SEARCH_ENTITY")
                municipalityFor(suggestion.value)?.comarca?.takeIf(String::isNotBlank)?.let {
                    recordBehaviour(BehaviourKind.COMARCA, it, serverAction = "SEARCH_ENTITY")
                }
            }
            FeedSearchSuggestionType.COMARCA -> recordBehaviour(BehaviourKind.COMARCA, suggestion.value, serverAction = "SEARCH_ENTITY")
            FeedSearchSuggestionType.ARTIST -> recordBehaviour(BehaviourKind.ARTIST, suggestion.value, serverAction = "SEARCH_ENTITY")
            FeedSearchSuggestionType.COMPANY -> recordBehaviour(BehaviourKind.ORGANIZER, suggestion.value, serverAction = "SEARCH_ENTITY")
            FeedSearchSuggestionType.VENUE -> recordBehaviour(BehaviourKind.VENUE, suggestion.value, serverAction = "SEARCH_ENTITY")
            FeedSearchSuggestionType.STYLE -> recordBehaviour(BehaviourKind.STYLE, suggestion.value, serverAction = "SEARCH_ENTITY")
        }
        if (suggestion.type == FeedSearchSuggestionType.ARTIST) refreshMissingSelectedArtistFromServer()
    }

    /** Una cerca exacta d'artista sense actes locals força la reconciliació completa. */
    private fun refreshMissingSelectedArtistFromServer() {
        val artistKey = selectedFeedSearchArtist?.let(::normalizeSearchValue) ?: return
        if (!fullAgendaLoadedForProcess || isAgendaRefreshing || !RemoteAgendaRepository.isConfigured()) return
        val today = agendaToday()
        if (concerts.any { event ->
                artistKey in eventArtistKeys(event) && isCurrentOrFutureAgendaEvent(event, today)
            }) return
        reconcileCatalogForLookup()
    }

    fun recordArtistView(artist: String) {
        val clean = artist.trim()
        if (clean.isBlank()) return
        recordBehaviour(BehaviourKind.ARTIST, clean, serverAction = "OPEN_ENTITY")
    }

    fun recordOrganizerInterest(username: String) {
        val clean = username.trim()
        if (clean.isBlank()) return
        recordBehaviour(BehaviourKind.ORGANIZER, clean, serverAction = "OPEN_ENTITY")
    }

    fun recordCategoryInterest(category: CulturalCategory) {
        recordBehaviour(BehaviourKind.CATEGORY, category.name, serverAction = "OPEN_ENTITY")
    }

    fun recordConcertView(concert: Concert) {
        // Una obertura pesa poc. El context (prop/lluny, idioma, artistes, organitzador)
        // es calcula al puntuar a partir del mateix esdeveniment, no creant gustos falsos.
        recordBehaviour(BehaviourKind.EVENT_VIEW, concert.id.toString(), serverAction = "OPEN_EVENT", concert = concert)
    }

    fun refreshAffinityAnalytics(
        days: Int = 180,
        town: String = "",
        comarca: String = "",
        username: String = ""
    ) {
        if (currentAccount?.role != AccountRole.SUPER_ADMIN || affinityAnalyticsLoading) return
        affinityAnalyticsLoading = true
        affinityAnalyticsError = null
        backgroundExecutor.execute {
            val result = ServerApiRepository.fetchAffinityAnalytics(appContext, days, town, comarca, username)
            Handler(Looper.getMainLooper()).post {
                affinityAnalyticsLoading = false
                result.onSuccess {
                    affinityAnalytics = it
                    affinityAnalyticsError = null
                }.onFailure {
                    affinityAnalyticsError = it.message ?: "No s'ha pogut carregar l'analítica d'afinitats."
                }
            }
        }
    }

    fun activeManagedConcerts(today: LocalDate = agendaToday()): List<Concert> =
        editableConcerts()
            .filter { concert ->
                runCatching { LocalDate.parse(concert.date) }.getOrNull()?.let { !it.isBefore(today) } ?: true
            }
            .sortedWith(compareBy<Concert> { it.date }.thenBy { it.time })

    fun pastManagedConcerts(today: LocalDate = agendaToday()): List<Concert> =
        editableConcerts()
            .filter { concert ->
                runCatching { LocalDate.parse(concert.date) }.getOrNull()?.isBefore(today) == true
            }
            .sortedWith(compareByDescending<Concert> { it.date }.thenByDescending { it.time })

    fun advertisementsForCurrentAccount(): List<EventAdvertisement> {
        val account = currentAccount ?: return emptyList()
        return if (account.role == AccountRole.SUPER_ADMIN) {
            advertisements.sortedByDescending { it.createdAtMillis }
        } else {
            advertisements.filter { it.ownerUsername == account.username }.sortedByDescending { it.createdAtMillis }
        }
    }

    fun createAdvertisement(
        concertId: Int,
        durationHours: Int,
        imageUri: String?,
        targetStyles: Set<String>,
        targetSearchTerms: Set<String>,
        targetTowns: Set<String>,
        targetComarques: Set<String>,
        targetFollowers: Boolean,
        matchAllCriteria: Boolean
    ): Boolean {
        val account = currentAccount ?: return false
        if (account.role == AccountRole.USER || durationHours !in 1..2160) return false
        val concert = concerts.firstOrNull { it.id == concertId } ?: return false
        if (!canManageConcert(concert)) return false
        val concertDate = runCatching { LocalDate.parse(concert.date) }.getOrNull()
        if (concertDate != null && concertDate.isBefore(agendaToday())) return false

        val nextId = (advertisements.maxOfOrNull { it.id } ?: 0) + 1
        val ad = EventAdvertisement(
            id = nextId,
            ownerUsername = account.username,
            concertId = concertId,
            durationHours = durationHours,
            imageUri = imageUri?.trim()?.takeIf { it.isNotBlank() },
            targetStyles = targetStyles.map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet(),
            targetSearchTerms = targetSearchTerms.map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet(),
            targetTowns = targetTowns.map(String::trim).filter(String::isNotBlank).toSet(),
            targetComarques = targetComarques.map(String::trim).filter(String::isNotBlank).toSet(),
            targetFollowers = targetFollowers,
            matchAllCriteria = matchAllCriteria
        )
        advertisements = listOf(ad) + advertisements
        saveAdvertisements()
        saveAdTargetingDefaults(
            AdTargetingDefaults(
                durationHours = durationHours,
                targetStyles = ad.targetStyles,
                targetSearchTerms = ad.targetSearchTerms,
                targetTowns = ad.targetTowns,
                targetComarques = ad.targetComarques,
                targetFollowers = targetFollowers,
                matchAllCriteria = matchAllCriteria
            )
        )
        return true
    }

    /**
     * v249 · Els anuncis actius són globals: mai depenen de gustos, comarca,
     * pobles, seguiments, inici de sessió, tipus de compte o mode del feed.
     * Només mantenim vigència i, si l'esdeveniment és conegut, data futura/vigent.
     * El servidor determina quines campanyes estan pagades i publicades.
     */
    fun activeAdvertisementsForCurrentUser(nowMillis: Long = System.currentTimeMillis()): List<EventAdvertisement> =
        advertisements
            .asSequence()
            .filter { it.isActive(nowMillis) }
            .filter { ad ->
                concertForAdvertisement(ad)?.let { concert ->
                    val lastDay = concert.endDate.ifBlank { concert.date }
                    runCatching { LocalDate.parse(lastDay.take(10)) }.getOrNull()
                        ?.let { !it.isBefore(agendaToday()) } ?: true
                } ?: true
            }
            .sortedWith(compareBy<EventAdvertisement> { it.startsAtMillis }.thenBy { it.id })
            .toList()

    /**
     * Les reserves gratuïtes internes no entren a «Per a tu».
     * Les entrades de pagament, les reserves i les campanyes publicitàries
     * no donen cap inclusió automàtica a les targetes del feed.
     */
    fun isFreeInternalAgendaReservation(event: Concert): Boolean =
        AgendaReservationPolicy.isFreeInternalAgendaReservation(
            ticketEnabled = event.ticketAvailable,
            ticketPrice = event.ticketPrice,
            ticketUrl = event.ticketUrl
        )

    /**
     * Resolució canònica d'un anunci. Un mateix event pot existir en diverses caches
     * (feed resident, SQLite d'anuncis, instàncies multidia). Per a obrir-lo, prioritzem
     * qualsevol versió que conserve la classificació estructural de festival/programació.
     * Així l'anunci sap si ha d'obrir el popup de festival o la ruta normal de concert.
     */
    fun concertForAdvertisement(ad: EventAdvertisement): Concert? {
        val id = ad.concertId
        val candidates = buildList {
            concertById(id)?.let(::add)
            advertisementConcertCache[id]?.let(::add)
            concerts.asSequence().filter { it.id == id }.forEach(::add)
        }.distinctBy { concert ->
            listOf(concert.id, concert.feedDate, concert.eventStructure, concert.scheduleStatus).joinToString("|")
        }
        var target = candidates.firstOrNull { it.isFestivalExperience } ?: candidates.firstOrNull() ?: return null

        // Si ja hem consultat l'espai especial, usem també la seua classificació com a
        // garantia addicional. No fem cap petició de xarxa al clic: és només metadata
        // que ja puga estar disponible en memòria.
        val remoteSpecial = specialEventsRemote[id]
        val remoteStructure = remoteSpecial?.eventStructure?.trim()?.uppercase().orEmpty()
        if (!target.isFestivalExperience && remoteStructure in setOf("FESTIVAL", "PROGRAMME", "CYCLE")) {
            target = target.copy(
                eventStructure = remoteStructure,
                scheduleStatus = remoteSpecial?.scheduleStatus?.ifBlank { target.scheduleStatus } ?: target.scheduleStatus
            )
        }
        return target
    }



    fun eventSubmissionsForCurrentAccount(): List<EventSubmission> {
        val account = currentAccount ?: return emptyList()
        return if (account.role == AccountRole.SUPER_ADMIN) {
            eventSubmissions.sortedByDescending { it.createdAtMillis }
        } else {
            eventSubmissions.filter { it.ownerUsername == account.username }.sortedByDescending { it.createdAtMillis }
        }
    }

    fun createEventSubmission(
        posterUri: String?,
        title: String,
        subtitle: String = "",
        artists: List<String>,
        date: String,
        time: String,
        endDate: String = "",
        endTime: String = "",
        town: String,
        venue: String,
        category: CulturalCategory,
        styles: List<String>,
        language: String,
        price: Int,
        ticketSalesEnabled: Boolean,
        ticketUrl: String,
        description: String,
        recognizedText: String,
        submitForReview: Boolean
    ): Boolean {
        val account = currentAccount ?: return false
        if (price < 0) return false
        if (submitForReview && (title.isBlank() || date.isBlank() || town.isBlank())) return false

        val cleanArtists = artists.map(String::trim).filter(String::isNotBlank).distinct()
        val fallbackArtists = when {
            cleanArtists.isNotEmpty() -> cleanArtists
            account.role == AccountRole.GROUP -> account.managedArtists.toList()
            else -> emptyList()
        }
        if (submitForReview && category == CulturalCategory.MUSIC && fallbackArtists.isEmpty()) return false

        val nextId = (eventSubmissions.maxOfOrNull { it.id } ?: 0) + 1
        val submission = EventSubmission(
            id = nextId,
            ownerUsername = account.username,
            posterUri = posterUri,
            title = title.trim(),
            subtitle = subtitle.trim(),
            artists = fallbackArtists,
            date = date.trim(),
            time = time.trim(),
            endDate = endDate.trim(),
            endTime = endTime.trim(),
            town = town.trim(),
            venue = venue.trim(),
            category = category,
            styles = styles.map(String::trim).filter(String::isNotBlank).distinct(),
            language = language.trim().ifBlank { "Valencià" },
            price = price,
            ticketSalesEnabled = ticketSalesEnabled,
            ticketUrl = ticketUrl.trim(),
            description = description.trim(),
            recognizedText = recognizedText.trim(),
            status = if (submitForReview) EventSubmissionStatus.PENDING_REVIEW else EventSubmissionStatus.DRAFT
        )
        eventSubmissions = listOf(submission) + eventSubmissions
        saveEventSubmissions()
        return true
    }

    /**
     * Usuari base o convidat: envia l'esdeveniment al servidor per publicar ara.
     * El servidor el deixa visible i n'obri una auditoria posterior al Sheets.
     */
    fun submitEventForReviewToServer(
        posterUri: String?,
        title: String,
        subtitle: String = "",
        artists: List<String>,
        organizers: List<String> = emptyList(),
        date: String,
        time: String,
        endDate: String = "",
        endTime: String = "",
        town: String,
        allowNewTown: Boolean = false,
        venue: String,
        category: CulturalCategory,
        styles: List<String>,
        price: Int,
        ticketSalesEnabled: Boolean,
        ticketUrl: String,
        ticketSaleOnAgenda: Boolean,
        description: String,
        onImageProgress: (Int) -> Unit = {},
        onResult: (Boolean, String, Boolean, Int?) -> Unit
    ) {
        val account = currentAccount
        if (title.isBlank() || date.isBlank() || town.isBlank()) {
            onResult(false, "Revisa títol, data i població.", false, null)
            return
        }
        val municipality = municipalityFor(town)
        if (municipalities.isNotEmpty() && municipality == null && !allowNewTown) {
            onResult(false, "Selecciona la població existent o usa Crear per afegir-ne una de nova.", false, null)
            return
        }
        val canonicalTown = municipality?.name ?: town.trim()
        val canonicalComarca = municipality?.comarca.orEmpty().ifBlank { comarcaForTown(canonicalTown).orEmpty() }
        val cleanArtists = artists.map(String::trim).filter(String::isNotBlank).distinct()
        val cleanOrganizers = organizers.map(String::trim).filter(String::isNotBlank).distinct()
        if (category == CulturalCategory.MUSIC && cleanArtists.isEmpty()) {
            onResult(false, "En música cal indicar almenys un grup o artista.", false, null)
            return
        }
        if (price < 0) {
            onResult(false, "El preu no pot ser negatiu.", false, null)
            return
        }
        backgroundExecutor.execute {
            val result = ServerApiRepository.submitEventForReview(
                context = appContext,
                title = title.trim(),
                subtitle = subtitle.trim(),
                artists = cleanArtists,
                organizers = cleanOrganizers,
                date = date.trim(),
                time = time.trim(),
                endDate = endDate.trim(),
                endTime = endTime.trim(),
                town = canonicalTown,
                comarca = canonicalComarca,
                venue = venue.trim(),
                category = category,
                styles = styles.map(String::trim).filter(String::isNotBlank),
                price = price,
                ticketSalesEnabled = ticketSalesEnabled,
                ticketUrl = ticketUrl.trim(),
                ticketSaleOnAgenda = ticketSaleOnAgenda,
                description = description.trim()
            )
            result.onSuccess { submission ->
                val eventId = submission.eventId
                var imageMessage = ""
                if (!posterUri.isNullOrBlank()) {
                    val imageResult = if (account == null || !isAuthorizedPublisher()) {
                        ServerApiRepository.uploadOwnSubmittedEventPoster(
                            context = appContext,
                            eventId = eventId,
                            imageUri = posterUri,
                            onProgress = { pct -> Handler(Looper.getMainLooper()).post { onImageProgress(pct) } }
                        )
                    } else {
                        ServerApiRepository.updateEventCardVisual(
                            context = appContext,
                            concertId = eventId,
                            imageSource = "EVENT",
                            entityName = null,
                            imageUri = posterUri,
                            removeImage = false,
                            imageFocusX = 0.5f,
                            imageFocusY = 0.5f,
                            imageScale = 1f,
                            onUploadProgress = { pct -> Handler(Looper.getMainLooper()).post { onImageProgress(pct) } }
                        ).map { it.imageUrl.orEmpty() }
                    }
                    imageResult.onFailure { imageMessage = " El cartell no s'ha pogut pujar i caldrà afegir-lo en la revisió." }
                }
                Handler(Looper.getMainLooper()).post {
                    if (submission.published) {
                        if (ticketSaleOnAgenda) rememberInternalSaleEvent(submission.eventId)
                        refreshRemoteAgenda()
                        refreshPublisherAccess()
                        refreshPublicationTrust()
                        onResult(
                            true,
                            "Publicat. Entrarà en la pròxima revisió editorial i de moderació.$imageMessage",
                            true,
                            eventId
                        )
                    } else {
                        onResult(false, "El servidor no ha confirmat la publicació. Comprova la configuració del PHP.", false, null)
                    }
                }
            }.onFailure { error ->
                Handler(Looper.getMainLooper()).post {
                    onResult(false, error.message ?: "No s'ha pogut publicar l'esdeveniment.", false, null)
                }
            }
        }
    }

    fun saveEventTicketDrafts(eventId: Int, options: org.json.JSONArray, onResult: (Boolean, String) -> Unit) {
        backgroundExecutor.execute {
            val result = ServerApiRepository.saveEventTicketDrafts(appContext, eventId, options)
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { onResult(true, "Modalitats guardades; venda pendent d'activació.") }
                    .onFailure { onResult(false, it.message ?: "No s'han guardat les modalitats.") }
            }
        }
    }

    internal fun loadEventTicketDrafts(eventId: Int, onResult: (List<com.example.agendavalenciana.ui.EventTicketDraft>?, String?) -> Unit) {
        backgroundExecutor.execute {
            val result = ServerApiRepository.getEventTicketDrafts(appContext, eventId)
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { onResult(it.ticketDraftItems(), null) }
                    .onFailure { onResult(null, it.message) }
            }
        }
    }

    fun loadOwnSubmittedEvents(onResult: (List<ServerApiRepository.OwnSubmittedEvent>, String?) -> Unit) {
        backgroundExecutor.execute {
            val result = ServerApiRepository.fetchOwnSubmittedEvents(appContext)
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { onResult(it, null) }
                    .onFailure { onResult(emptyList(), it.message ?: "No s'han pogut carregar els teus esdeveniments.") }
            }
        }
    }

    fun deleteOwnSubmittedEvent(eventId: Int, onResult: (Boolean, String) -> Unit) {
        if (eventId <= 0) {
            onResult(false, "Esdeveniment no vàlid.")
            return
        }
        backgroundExecutor.execute {
            val result = ServerApiRepository.deleteOwnSubmittedEvent(appContext, eventId)
            Handler(Looper.getMainLooper()).post {
                result.onSuccess {
                    concerts = concerts.filterNot { it.id == eventId }
                    remoteConcerts = remoteConcerts.filterNot { it.id == eventId }
                    remoteConcertIds = remoteConcertIds - eventId
                    savedConcerts = savedConcerts - eventId
                    invalidateFeedCache()
                    onResult(true, "Esdeveniment eliminat.")
                }.onFailure { error ->
                    onResult(false, error.message ?: "No s'ha pogut eliminar l'esdeveniment.")
                }
            }
        }
    }

    fun updateOwnSubmittedEvent(
        item: ServerApiRepository.OwnSubmittedEvent,
        title: String,
        subtitle: String,
        artists: List<String>,
        organizers: List<String>,
        date: String,
        endDate: String,
        time: String,
        endTime: String,
        town: String,
        venue: String,
        category: CulturalCategory,
        styles: List<String>,
        price: Int,
        ticketEnabled: Boolean,
        ticketUrl: String,
        ticketSaleOnAgenda: Boolean,
        description: String,
        onResult: (Boolean, String) -> Unit
    ) {
        if (title.isBlank() || date.isBlank() || town.isBlank()) {
            onResult(false, "Revisa títol, data i població.")
            return
        }
        val municipality = municipalityFor(town)
        val canonicalTown = municipality?.name ?: town.trim()
        val comarca = municipality?.comarca.orEmpty().ifBlank { item.comarca.ifBlank { comarcaForTown(canonicalTown).orEmpty() } }
        backgroundExecutor.execute {
            val result = ServerApiRepository.updateOwnSubmittedEvent(
                context = appContext,
                eventId = item.eventId,
                title = title,
                subtitle = subtitle,
                artists = artists,
                organizers = organizers,
                date = date,
                endDate = endDate,
                time = time,
                endTime = endTime,
                town = canonicalTown,
                comarca = comarca,
                venue = venue,
                category = category,
                styles = styles,
                price = price,
                ticketEnabled = ticketEnabled,
                ticketUrl = ticketUrl,
                ticketSaleOnAgenda = ticketSaleOnAgenda,
                description = description
            )
            Handler(Looper.getMainLooper()).post {
                result.onSuccess {
                    if (ticketSaleOnAgenda && currentAccount != null) refreshPublisherAccess()
                    onResult(true, it)
                }
                    .onFailure { onResult(false, it.message ?: "No s'han pogut guardar els canvis.") }
            }
        }
    }

    fun updateOwnSubmittedEventPoster(
        eventId: Int,
        imageUri: String,
        onProgress: (Int) -> Unit = {},
        onResult: (Boolean, String) -> Unit
    ) {
        backgroundExecutor.execute {
            val result = ServerApiRepository.uploadOwnSubmittedEventPoster(
                appContext, eventId, imageUri,
                onProgress = { pct -> Handler(Looper.getMainLooper()).post { onProgress(pct) } }
            )
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { onResult(true, "Cartell actualitzat.") }
                    .onFailure { onResult(false, it.message ?: "No s'ha pogut actualitzar el cartell.") }
            }
        }
    }

    fun requestProfessionalAccess(
        entityName: String,
        profileType: String,
        relation: String,
        contact: String,
        message: String,
        onResult: (Boolean, String) -> Unit
    ) {
        if (currentAccount == null) {
            onResult(false, "Cal iniciar sessió.")
            return
        }
        if (entityName.isBlank() || relation.isBlank() || contact.isBlank()) {
            onResult(false, "Indica l'entitat, la teua relació i un correu o telèfon de contacte.")
            return
        }
        backgroundExecutor.execute {
            val result = ServerApiRepository.requestProfessionalProfileClaim(
                appContext,
                entityName.trim(),
                profileType,
                relation.trim(),
                contact.trim(),
                message.trim()
            )
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { onResult(true, it) }
                    .onFailure { onResult(false, it.message ?: "No s'ha pogut enviar la sol·licitud.") }
            }
        }
    }

    /** Publica directament en el servidor central. En acabar, tots els dispositius el reben via l'API. */
    fun publishEventToServer(
        posterUri: String?,
        title: String,
        subtitle: String = "",
        artists: List<String>,
        organizers: List<String> = emptyList(),
        date: String,
        time: String,
        endDate: String = "",
        endTime: String = "",
        town: String,
        allowNewTown: Boolean = false,
        venue: String,
        category: CulturalCategory,
        styles: List<String>,
        price: Int,
        ticketSalesEnabled: Boolean,
        ticketUrl: String,
        description: String,
        imageFocusX: Float = 0.5f,
        imageFocusY: Float = 0.5f,
        imageScale: Float = 1f,
        onImageProgress: (Int) -> Unit = {},
        onResult: (Boolean, String, Int?, Boolean, String?) -> Unit
    ) {
        val account = currentAccount
        if (account == null || !canAutoPublishEvents()) {
            onResult(false, "Cal iniciar sessió i tindre el permís de publicació actiu. Si estàs suspés, consulta el perfil.", null, false, null)
            return
        }
        val cleanArtists = artists.map(String::trim).filter(String::isNotBlank).distinct()
        val fallbackArtists = when {
            cleanArtists.isNotEmpty() -> cleanArtists
            account.role == AccountRole.GROUP -> account.managedArtists.toList()
            else -> emptyList()
        }
        val cleanOrganizers = sanitizeEventOrganizers(
            artists = fallbackArtists,
            organizers = organizers
        )
        if (title.isBlank() || date.isBlank() || town.isBlank()) {
            onResult(false, "Revisa títol, data i població.", null, false, null)
            return
        }
        val municipality = municipalityFor(town)
        if (municipalities.isNotEmpty() && municipality == null && !allowNewTown) {
            onResult(false, "Selecciona la població existent o usa Crear per afegir-ne una de nova.", null, false, null)
            return
        }
        val canonicalTown = municipality?.name ?: town.trim()
        val canonicalComarca = municipality?.comarca.orEmpty().ifBlank { comarcaForTown(canonicalTown).orEmpty() }
        if (category == CulturalCategory.MUSIC && fallbackArtists.isEmpty()) {
            onResult(false, "En música cal indicar almenys un grup o artista.", null, false, null)
            return
        }
        if (fallbackArtists.size >= 3 && cleanOrganizers.isEmpty()) {
            onResult(false, "En festivals cal indicar l’organitzador o productora.", null, false, null)
            return
        }
        if (isPublishingEvent) return
        val existingDuplicate = findExistingEventDuplicate(
            title, fallbackArtists, date, time, canonicalTown, venue,
            isMultiday = endDate.isNotBlank() && endDate != date
        )
        if (existingDuplicate != null) {
            onResult(false, "Ja hi ha un esdeveniment molt semblant (ID ${existingDuplicate.id}). Revisa'l abans de publicar.", null, false, null)
            return
        }

        isPublishingEvent = true
        eventPublishError = null
        val publishImageProgress: (Int) -> Unit = { percent ->
            Handler(Looper.getMainLooper()).post { onImageProgress(percent.coerceIn(0, 100)) }
        }
        if (!posterUri.isNullOrBlank()) publishImageProgress(0)
        backgroundExecutor.execute {
            val createResult = ServerApiRepository.createEvent(
                context = appContext,
                posterUri = posterUri,
                title = title,
                subtitle = subtitle,
                artists = fallbackArtists,
                date = date,
                time = time,
                endDate = endDate,
                endTime = endTime,
                town = canonicalTown,
                comarca = canonicalComarca,
                venue = venue,
                category = category,
                styles = styles,
                price = price,
                ticketSalesEnabled = ticketSalesEnabled,
                ticketUrl = ticketUrl,
                description = description
            )
            createResult.onSuccess { created ->
                if (ticketSalesEnabled && ticketUrl.isBlank()) {
                    Handler(Looper.getMainLooper()).post { rememberInternalSaleEvent(created.concert.id) }
                }
                val savedOrganizers = if (cleanOrganizers.isNotEmpty()) {
                    ServerApiRepository.replaceEventOrganizers(appContext, created.concert.id, cleanOrganizers)
                        .getOrElse { cleanOrganizers }
                } else created.concert.organizers
                var createdConcert = created.concert.copy(organizers = savedOrganizers)
                // La publicació professional és immediata, però queda en cua perquè
                // les passades de revisió la tornen a examinar. Un error d'auditoria
                // no bloqueja la publicació ja creada.
                ServerApiRepository.markEventForAudit(appContext, created.concert.id)
                var imageApplied = posterUri.isNullOrBlank()
                var serverImageUrl: String? = null
                var imageError: String? = null
                if (!posterUri.isNullOrBlank()) {
                    ServerApiRepository.updateEventCardVisual(
                        context = appContext,
                        concertId = createdConcert.id,
                        imageSource = "EVENT",
                        entityName = null,
                        imageUri = posterUri,
                        removeImage = false,
                        imageFocusX = imageFocusX,
                        imageFocusY = imageFocusY,
                        imageScale = imageScale,
                        onUploadProgress = publishImageProgress
                    ).onSuccess { visual ->
                        serverImageUrl = visual.imageUrl
                        imageApplied = !serverImageUrl.isNullOrBlank()
                        createdConcert = createdConcert.copy(
                            imageSource = "EVENT",
                            eventCardImageUri = serverImageUrl,
                            preferredImageUri = serverImageUrl,
                            resolvedCardImageUri = serverImageUrl,
                            imageFocusX = imageFocusX.coerceIn(0f, 1f),
                            imageFocusY = imageFocusY.coerceIn(0f, 1f),
                            imageScale = imageScale.coerceIn(1f, 4f)
                        )
                    }.onFailure { error ->
                        imageError = error.message ?: "No s'ha pogut pujar el cartell."
                    }
                }
                val resultText = when {
                    posterUri.isNullOrBlank() -> "Publicat. L'esdeveniment ja està disponible per a tots els usuaris."
                    imageApplied -> "Publicat. El cartell s'ha guardat al servidor i s'ha aplicat a la card."
                    else -> "L'esdeveniment s'ha publicat, però el cartell no s'ha pogut aplicar: ${imageError ?: "error desconegut"}"
                }
                Handler(Looper.getMainLooper()).post {
                    val byId = concerts.associateBy { it.id }.toMutableMap()
                    byId[createdConcert.id] = createdConcert
                    concerts = byId.values.toList()
                    eventPublishError = if (imageApplied) null else resultText
                    invalidateFeedCache()
                    refreshRemoteAgenda()
                    // El servidor passa a Professional només en publicar amb venda.
                    if (ticketSalesEnabled) refreshPublisherAccess()
                    refreshPublicationTrust()
                    onResult(true, resultText, createdConcert.id, imageApplied, serverImageUrl)
                    isPublishingEvent = false
                }
            }.onFailure { error ->
                Handler(Looper.getMainLooper()).post {
                    val text = error.message ?: "No s'ha pogut publicar l'esdeveniment."
                    eventPublishError = text
                    onResult(false, text, null, false, null)
                    isPublishingEvent = false
                }
            }
        }
    }

    fun importEventsCsv(
        uri: Uri,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val account = currentAccount
        if (account == null || account.role != AccountRole.SUPER_ADMIN) {
            onResult(false, "Només el Super Admin pot fer importacions massives.")
            return
        }
        if (isBulkImporting) return

        isBulkImporting = true
        bulkImportProgress = 0f
        bulkImportMessage = "Llegint CSV…"

        backgroundExecutor.execute {
            val outcome = runCatching {
                val raw = appContext.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: error("No s'ha pogut obrir el fitxer seleccionat.")
                val rows = RemoteAgendaRepository.parseRows(raw)
                require(rows.isNotEmpty()) { "El CSV està buit." }

                val header = rows.first().mapIndexed { index, value ->
                    (if (index == 0) value.removePrefix("\uFEFF") else value).trim().lowercase()
                }
                fun col(name: String): Int = header.indexOf(name)
                fun value(row: List<String>, name: String): String {
                    val i = col(name)
                    return if (i >= 0 && i < row.size) row[i].trim() else ""
                }
                require(col("title") >= 0 && col("date") >= 0 && col("town") >= 0) {
                    "El CSV ha de contindre almenys title, date i town."
                }

                fun split(rawValue: String): List<String> = rawValue
                    .split('|', ',', ';')
                    .map(String::trim)
                    .filter(String::isNotBlank)
                    .distinct()

                fun boolean(rawValue: String, default: Boolean = false): Boolean = when (rawValue.trim().lowercase()) {
                    "1", "true", "yes", "si", "sí", "y" -> true
                    "0", "false", "no", "n" -> false
                    else -> default
                }

                fun category(rawValue: String): CulturalCategory = runCatching {
                    CulturalCategory.valueOf(rawValue.trim().uppercase())
                }.getOrDefault(CulturalCategory.OTHER)

                fun signature(title: String, date: String, town: String, venue: String): String =
                    listOf(title, date, town, venue).joinToString("|") { it.trim().lowercase() }

                val known = concerts.mapTo(mutableSetOf()) { signature(it.title, it.date, it.town, it.venue) }
                val dataRows = rows.drop(1).filterNot { row -> row.all(String::isBlank) }
                var imported = 0
                var skipped = 0
                var failed = 0
                val errors = mutableListOf<String>()

                dataRows.forEachIndexed { index, row ->
                    val title = value(row, "title")
                    val date = value(row, "date")
                    val town = value(row, "town")
                    val venue = value(row, "venue")
                    val sig = signature(title, date, town, venue)

                    if (title.isBlank() || date.isBlank() || town.isBlank()) {
                        failed++
                        errors += "Fila ${index + 2}: falta title, date o town."
                    } else if (sig in known) {
                        skipped++
                    } else {
                        val artists = split(value(row, "artists"))
                        val styles = split(value(row, "styles"))
                        val price = value(row, "ticket_price")
                            .replace(',', '.')
                            .toDoubleOrNull()?.toInt()?.coerceAtLeast(0) ?: -1
                        val ticketUrl = value(row, "ticket_url")
                        val ticketEnabled = boolean(value(row, "ticket_enabled"), default = false) || ticketUrl.isNotBlank()
                        val artistImageUrl = value(row, "artist_image_url").takeIf(String::isNotBlank)
                        val venueImageUrl = value(row, "venue_image_url").takeIf(String::isNotBlank)
                        val townImageUrl = value(row, "town_image_url").takeIf(String::isNotBlank)
                        val organizers = split(value(row, "organizers"))
                        val result = ServerApiRepository.createEvent(
                            context = appContext,
                            posterUri = null,
                            title = title,
                            subtitle = value(row, "subtitle"),
                            artists = artists,
                            date = date,
                            time = value(row, "time"),
                            town = town,
                            comarca = value(row, "comarca").ifBlank { comarcaForTown(town).orEmpty() },
                            venue = venue,
                            category = category(value(row, "category")),
                            styles = styles,
                            price = price,
                            ticketSalesEnabled = ticketEnabled,
                            ticketUrl = ticketUrl,
                            description = value(row, "description"),
                            artistImageUrl = artistImageUrl,
                            venueImageUrl = venueImageUrl,
                            townImageUrl = townImageUrl
                        )
                        result.onSuccess { created ->
                            if (organizers.isNotEmpty()) {
                                runCatching { ServerApiRepository.replaceEventOrganizers(appContext, created.concert.id, organizers) }
                            }
                            imported++
                            known += sig
                        }.onFailure { error ->
                            failed++
                            errors += "Fila ${index + 2} · $title: ${error.message ?: "error desconegut"}"
                        }
                    }

                    val progress = if (dataRows.isEmpty()) 1f else (index + 1).toFloat() / dataRows.size.toFloat()
                    Handler(Looper.getMainLooper()).post {
                        bulkImportProgress = progress
                        bulkImportMessage = "Important ${index + 1}/${dataRows.size} · $imported publicats · $skipped duplicats · $failed errors"
                    }
                }

                val summary = buildString {
                    append("Importació acabada: $imported publicats directament")
                    if (skipped > 0) append(", $skipped duplicats omesos")
                    if (failed > 0) append(", $failed errors")
                    append('.')
                    if (errors.isNotEmpty()) append("\n").append(errors.take(5).joinToString("\n"))
                    if (errors.size > 5) append("\n… i ${errors.size - 5} errors més.")
                }
                Triple(imported, failed, summary)
            }

            Handler(Looper.getMainLooper()).post {
                outcome.onSuccess { (imported, failed, summary) ->
                    isBulkImporting = false
                    bulkImportProgress = 1f
                    bulkImportMessage = summary
                    if (imported > 0) refreshRemoteAgenda()
                    onResult(failed == 0, summary)
                }.onFailure { error ->
                    isBulkImporting = false
                    bulkImportProgress = 0f
                    val text = error.message ?: "No s'ha pogut importar el CSV."
                    bulkImportMessage = text
                    onResult(false, text)
                }
            }
        }
    }

    /**
     * Descarrega els esdeveniments existents de l'API central en CSV.
     * El fitxer es pot editar i reutilitzar com a base per a una importació massiva.
     */
    fun exportExistingEventsCsv(
        uri: Uri,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val account = currentAccount
        if (account == null || account.role != AccountRole.SUPER_ADMIN) {
            onResult(false, "Només el Super Admin pot descarregar l'exportació completa.")
            return
        }
        if (isBulkExporting) return

        isBulkExporting = true
        bulkExportMessage = "Preparant exportació des de l'API…"

        backgroundExecutor.execute {
            val outcome = ServerApiRepository.fetchEventsExportCsv(appContext).mapCatching { csv ->
                require(csv.isNotBlank()) { "L'API ha retornat un CSV buit." }
                appContext.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use { writer ->
                    writer.write(csv)
                    if (!csv.endsWith("\n")) writer.newLine()
                } ?: error("No s'ha pogut escriure el fitxer seleccionat.")
                val rows = RemoteAgendaRepository.parseRows(csv).drop(1).count { row -> row.any(String::isNotBlank) }
                rows
            }

            Handler(Looper.getMainLooper()).post {
                outcome.onSuccess { rows ->
                    isBulkExporting = false
                    val text = "Exportació completada: $rows esdeveniments descarregats."
                    bulkExportMessage = text
                    onResult(true, text)
                }.onFailure { error ->
                    isBulkExporting = false
                    val text = error.message ?: "No s'han pogut descarregar els esdeveniments."
                    bulkExportMessage = text
                    onResult(false, text)
                }
            }
        }
    }

    /** Catàleg editable en Excel/CSV: grups, sales, pobles, categories i organitzadors. */
    fun importEntityImagesCsv(
        uri: Uri,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val account = currentAccount
        if (account == null || account.role != AccountRole.SUPER_ADMIN) {
            onResult(false, "Només el Super Admin pot modificar el catàleg d'imatges.")
            return
        }
        if (isEntityImageImporting) return
        isEntityImageImporting = true
        entityImageMessage = "Llegint catàleg d'imatges…"
        backgroundExecutor.execute {
            val outcome = runCatching {
                val raw = appContext.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: error("No s'ha pogut obrir el CSV.")
                val rows = RemoteAgendaRepository.parseRows(raw)
                require(rows.size >= 2) { "El CSV està buit." }
                val header = rows.first().mapIndexed { index, value ->
                    (if (index == 0) value.removePrefix("\uFEFF") else value).trim().lowercase()
                }
                fun col(name: String) = header.indexOf(name)
                fun value(row: List<String>, name: String): String {
                    val index = col(name)
                    return if (index >= 0 && index < row.size) row[index].trim() else ""
                }
                require(col("type") >= 0 && col("name") >= 0 && col("image_url") >= 0) {
                    "El CSV necessita type, name i image_url."
                }
                val allowed = setOf("ARTIST", "VENUE", "TOWN", "CATEGORY", "ORGANIZER", "FESTIVAL")
                val parsed = rows.drop(1).filter { it.any(String::isNotBlank) }.mapIndexedNotNull { index, row ->
                    val type = value(row, "type").uppercase()
                    val name = value(row, "name")
                    val image = value(row, "image_url")
                    if (type !in allowed || name.isBlank()) {
                        null
                    } else ServerApiRepository.EntityImageRow(
                        type = type,
                        name = name,
                        imageUrl = image.takeIf(String::isNotBlank),
                        sourceUrl = value(row, "source_url").takeIf(String::isNotBlank),
                        imageOrigin = value(row, "image_origin").ifBlank { "MANUAL" },
                        imageStatus = if (image.isBlank()) "pending" else "ready"
                    )
                }
                require(parsed.isNotEmpty()) { "No hi ha cap fila vàlida per importar." }
                ServerApiRepository.bulkUpsertEntityImages(appContext, parsed).getOrThrow()
            }
            Handler(Looper.getMainLooper()).post {
                isEntityImageImporting = false
                outcome.onSuccess { count ->
                    entityImageMessage = "$count imatges/entitats actualitzades. Refrescant agenda…"
                    refreshRemoteAgenda()
                    onResult(true, "$count imatges/entitats actualitzades.")
                }.onFailure { error ->
                    val message = error.message ?: "No s'ha pogut importar el catàleg d'imatges."
                    entityImageMessage = message
                    onResult(false, message)
                }
            }
        }
    }

    /** Importa perfils professionals reclamables. GROUP és ARTIST; AGENCY és ORGANIZER. */
    fun importProfessionalProfilesCsv(
        uri: Uri,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val account = currentAccount
        if (account == null || account.role != AccountRole.SUPER_ADMIN) {
            onResult(false, "Només el Super Admin pot importar perfils professionals.")
            return
        }
        if (isProfessionalProfileImporting) return
        isProfessionalProfileImporting = true
        professionalProfileMessage = "Llegint perfils reclamables…"
        backgroundExecutor.execute {
            val outcome = runCatching {
                val raw = appContext.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: error("No s'ha pogut obrir el CSV.")
                val rows = RemoteAgendaRepository.parseRows(raw)
                require(rows.size >= 2) { "El CSV està buit." }
                val header = rows.first().mapIndexed { index, value ->
                    (if (index == 0) value.removePrefix("\uFEFF") else value).trim().lowercase()
                }
                fun col(name: String) = header.indexOf(name)
                fun value(row: List<String>, name: String): String {
                    val index = col(name)
                    return if (index >= 0 && index < row.size) row[index].trim() else ""
                }
                require(col("profile_type") >= 0 && col("name") >= 0) {
                    "El CSV necessita profile_type i name."
                }
                val parsed = rows.drop(1).filter { it.any(String::isNotBlank) }.mapNotNull { row ->
                    val profileType = value(row, "profile_type").uppercase()
                    val name = value(row, "name")
                    if (profileType !in setOf("GROUP", "AGENCY") || name.isBlank()) null
                    else ServerApiRepository.ProfessionalProfileImportRow(
                        profileType = profileType,
                        name = name,
                        imageUrl = value(row, "image_url").takeIf(String::isNotBlank),
                        sourceUrl = value(row, "source_url").takeIf(String::isNotBlank),
                        imageOrigin = value(row, "image_origin").ifBlank { "RESEARCH" },
                        stylePrimary = value(row, "style_primary").takeIf(String::isNotBlank),
                        styleSecondary = value(row, "style_secondary").takeIf(String::isNotBlank),
                        styleTags = value(row, "style_tags").takeIf(String::isNotBlank),
                        languagePrimary = value(row, "language_primary").takeIf(String::isNotBlank),
                        languageSecondary = value(row, "language_secondary").takeIf(String::isNotBlank),
                        languageTags = value(row, "language_tags").takeIf(String::isNotBlank),
                        sourceOrigin = value(row, "source_origin").takeIf(String::isNotBlank),
                        confidence = value(row, "confidence").takeIf(String::isNotBlank),
                        notes = value(row, "notes").takeIf(String::isNotBlank),
                        agencyNames = value(row, "agency_names")
                            .split('|', ';').map(String::trim).filter(String::isNotBlank).distinct()
                    )
                }
                require(parsed.isNotEmpty()) { "No hi ha cap perfil GROUP o AGENCY vàlid." }
                ServerApiRepository.bulkUpsertProfessionalProfiles(appContext, parsed).getOrThrow()
            }
            Handler(Looper.getMainLooper()).post {
                isProfessionalProfileImporting = false
                outcome.onSuccess { count ->
                    professionalProfileMessage = "$count perfils professionals creats/actualitzats com a reclamables."
                    refreshRemoteAgenda()
                    onResult(true, professionalProfileMessage!!)
                }.onFailure { error ->
                    val message = error.message ?: "No s'han pogut importar els perfils professionals."
                    professionalProfileMessage = message
                    onResult(false, message)
                }
            }
        }
    }

    fun exportEntityImagesCsv(
        uri: Uri,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val account = currentAccount
        if (account == null || account.role != AccountRole.SUPER_ADMIN) {
            onResult(false, "Només el Super Admin pot exportar el catàleg d'imatges.")
            return
        }
        if (isEntityImageExporting) return
        isEntityImageExporting = true
        entityImageMessage = "Preparant catàleg d'imatges…"
        backgroundExecutor.execute {
            val outcome = ServerApiRepository.fetchEntityImageRows(appContext).mapCatching { rows ->
                fun csv(value: String?): String {
                    val clean = value.orEmpty()
                    return if (clean.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
                        "\"" + clean.replace("\"", "\"\"") + "\""
                    } else clean
                }
                val text = buildString {
                    appendLine("type,name,image_url,source_url,image_origin,image_status")
                    rows.forEach { row ->
                        appendLine(listOf(row.type, row.name, row.imageUrl.orEmpty(), row.sourceUrl.orEmpty(), row.imageOrigin, row.imageStatus)
                            .joinToString(",") { csv(it) })
                    }
                }
                appContext.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use { it.write(text) }
                    ?: error("No s'ha pogut escriure el CSV.")
                rows.size
            }
            Handler(Looper.getMainLooper()).post {
                isEntityImageExporting = false
                outcome.onSuccess { count ->
                    entityImageMessage = "Catàleg exportat: $count entitats."
                    onResult(true, entityImageMessage!!)
                }.onFailure { error ->
                    val message = error.message ?: "No s'ha pogut exportar el catàleg."
                    entityImageMessage = message
                    onResult(false, message)
                }
            }
        }
    }

    fun publishEventSubmission(submissionId: Int): Boolean {
        val account = currentAccount ?: return false
        if (account.role != AccountRole.SUPER_ADMIN) return false
        val submission = eventSubmissions.firstOrNull { it.id == submissionId } ?: return false
        if (submission.status == EventSubmissionStatus.PUBLISHED) return true
        if (submission.title.isBlank() || submission.date.isBlank() || submission.town.isBlank()) return false

        val published = submission.copy(status = EventSubmissionStatus.PUBLISHED)
        concerts = concerts.filterNot { it.id == 100_000 + submission.id } + concertFromSubmission(published)
        eventSubmissions = eventSubmissions.map {
            if (it.id == submissionId) it.copy(status = EventSubmissionStatus.PUBLISHED) else it
        }
        saveEventSubmissions()
        return true
    }

    @Synchronized
    fun knownArtistNames(): List<String> {
        if (cachedKnownArtistRevision == feedDataRevision) return cachedKnownArtistNames
        cachedKnownArtistNames = (
            concerts.flatMap { it.lineup } +
                concerts.map { it.artist } +
                ServerApiRepository.peekCachedEntityNames("ARTIST") +
                DemoData.artistProfiles.map { it.artist } +
                DemoData.accounts.filter { it.role == AccountRole.GROUP }.flatMap { it.managedArtists } +
                currentAccount?.managedArtists.orEmpty()
            )
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinctBy(AgendaDedupe::artistKey)
            .sortedBy(::normalizeSearchValue)
        cachedKnownArtistRevision = feedDataRevision
        return cachedKnownArtistNames
    }

    fun managedArtists(): List<String> = when (currentAccount?.role) {
        AccountRole.SUPER_ADMIN -> concerts.flatMap { it.lineup }.distinct().sorted()
        else -> currentAccount?.managedArtists?.sorted().orEmpty()
    }

    fun canManageConcert(concert: Concert): Boolean {
        val account = currentAccount ?: return false
        val isCreator = account.serverUserId != null && concert.creatorUserId == account.serverUserId
        return account.role == AccountRole.SUPER_ADMIN ||
            isCreator ||
            concert.canEditByCurrentUser ||
            concert.lineup.any(account::canManageArtist) ||
            account.username in concert.coEditorUsernames ||
            account.canManageVenue(concert.venue)
    }

    fun editableConcerts(): List<Concert> = concerts.filter(::canManageConcert)

    fun updateConcertGeneralInfo(
        concertId: Int,
        title: String,
        subtitle: String = "",
        artists: List<String>,
        organizers: List<String>? = null,
        date: String,
        time: String,
        endDate: String = "",
        endTime: String = "",
        town: String,
        allowNewTown: Boolean = false,
        venue: String,
        styles: List<String>,
        description: String,
        ticketPrice: Int? = null,
        ticketSalesEnabled: Boolean = false,
        ticketUrl: String = "",
        onResult: (Boolean, String) -> Unit
    ) {
        val existing = concerts.firstOrNull { it.id == concertId }
        if (existing == null || !canManageConcert(existing)) {
            onResult(false, "No tens permisos per editar este esdeveniment.")
            return
        }
        if (title.isBlank() || artists.isEmpty() || date.isBlank() || time.isBlank() || town.isBlank()) {
            onResult(false, "Revisa títol, artistes, data, hora i població.")
            return
        }
        val cleanArtists = artists.map(String::trim).filter(String::isNotBlank).distinctBy(::normalizeSearchValue)
        val cleanOrganizers = organizers?.let { sanitizeEventOrganizers(cleanArtists, it) }
        if (cleanArtists.size >= 3 && cleanOrganizers != null && cleanOrganizers.isEmpty()) {
            onResult(false, "En festivals cal indicar l’organitzador o productora.")
            return
        }
        val municipality = municipalityFor(town)
        if (municipalities.isNotEmpty() && municipality == null && !allowNewTown) {
            onResult(false, "Selecciona la població existent o usa Crear per afegir-ne una de nova.")
            return
        }
        val canonicalTown = municipality?.name ?: MunicipalityRepository.canonicalTown(town)
        val canonicalComarca = municipality?.comarca.orEmpty().ifBlank { comarcaForTown(canonicalTown).orEmpty() }
        val existingDuplicate = findExistingEventDuplicate(
            title, cleanArtists, date, time, canonicalTown, venue,
            excludeId = concertId, isMultiday = endDate.isNotBlank() && endDate != date
        )
        if (existingDuplicate != null) {
            onResult(false, "Possible duplicat de l'esdeveniment ${existingDuplicate.id}; revisa'l abans de guardar.")
            return
        }
        backgroundExecutor.execute {
            val result = runCatching {
                val updated = ServerApiRepository.updateEvent(
                    context = appContext,
                    concertId = concertId,
                    title = title.trim(),
                    subtitle = subtitle.trim(),
                    artists = cleanArtists,
                    date = date.trim(),
                    time = time.trim(),
                    endDate = endDate.trim(),
                    endTime = endTime.trim(),
                    town = canonicalTown,
                    comarca = canonicalComarca,
                    venue = venue.trim(),
                    styles = styles.map(String::trim).filter(String::isNotBlank),
                    description = description.trim(),
                    ticketPrice = ticketPrice,
                    clearTicketPrice = !ticketSalesEnabled || ticketPrice == null,
                    ticketSalesEnabled = ticketSalesEnabled,
                    ticketUrl = if (ticketSalesEnabled) ticketUrl.trim() else ""
                ).getOrThrow()
                if (cleanOrganizers != null) {
                    val savedOrganizers = ServerApiRepository.replaceEventOrganizers(appContext, concertId, cleanOrganizers).getOrThrow()
                    updated.copy(organizers = savedOrganizers)
                } else updated
            }
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { updated ->
                    concerts = concerts.map { if (it.id == concertId) updated else it }
                    refreshRemoteAgenda()
                    onResult(true, "Canvis guardats al servidor.")
                }.onFailure { error -> onResult(false, error.message ?: "No s'han pogut guardar els canvis.") }
            }
        }
    }

    fun canDeleteConcert(concert: Concert): Boolean {
        val account = currentAccount ?: return false
        val isCreator = account.serverUserId != null && concert.creatorUserId == account.serverUserId
        return account.role == AccountRole.SUPER_ADMIN || isCreator
    }

    fun updateConcertSuperAdminEdit(
        concertId: Int,
        title: String,
        subtitle: String = "",
        artists: List<String>,
        organizers: List<String>,
        date: String,
        time: String,
        endDate: String = "",
        endTime: String = "",
        town: String,
        venue: String,
        styles: List<String>,
        description: String,
        ticketPrice: Int? = null,
        ticketSalesEnabled: Boolean = false,
        ticketUrl: String = "",
        imageSource: String,
        imageEntityName: String?,
        imageUri: String?,
        removeImage: Boolean,
        imageFocusX: Float = 0.5f,
        imageFocusY: Float = 0.5f,
        imageScale: Float = 1f,
        onImageProgress: (Int) -> Unit = {},
        onResult: (Boolean, String) -> Unit
    ) {
        val account = currentAccount
        val existing = concerts.firstOrNull { it.id == concertId }
        if (account?.role != AccountRole.SUPER_ADMIN || existing == null) {
            onResult(false, "Només Super Admin pot usar l'edició global de la card.")
            return
        }
        if (title.isBlank() || artists.isEmpty() || date.isBlank() || time.isBlank() || town.isBlank()) {
            onResult(false, "Revisa títol, artistes, data, hora i població.")
            return
        }
        val municipality = municipalityFor(town)
        if (municipalities.isNotEmpty() && municipality == null) {
            onResult(false, "Selecciona la població des de la base de municipis.")
            return
        }
        val canonicalTown = municipality?.name ?: town.trim()
        val canonicalComarca = municipality?.comarca.orEmpty().ifBlank { comarcaForTown(canonicalTown).orEmpty() }
        val cleanArtists = artists.map(String::trim).filter(String::isNotBlank).distinctBy(::normalizeSearchValue)
        val cleanOrganizers = sanitizeEventOrganizers(cleanArtists, organizers)
        if (cleanArtists.size >= 3 && cleanOrganizers.isEmpty()) {
            onResult(false, "En festivals cal indicar un organitzador o productora que no siga un dels grups del cartell.")
            return
        }
        backgroundExecutor.execute {
            val result = runCatching {
                // La configuració visual es guarda de forma independent. Així una foto no
                // queda bloquejada perquè falle després una edició general o d'organitzadors.
                val visual = ServerApiRepository.updateEventCardVisual(
                    context = appContext,
                    concertId = concertId,
                    imageSource = imageSource,
                    entityName = imageEntityName,
                    imageUri = imageUri,
                    removeImage = removeImage,
                    imageFocusX = imageFocusX,
                    imageFocusY = imageFocusY,
                    imageScale = imageScale,
                    onUploadProgress = { percent ->
                        Handler(Looper.getMainLooper()).post { onImageProgress(percent.coerceIn(0, 100)) }
                    }
                ).getOrThrow()
                val updated = ServerApiRepository.updateEvent(
                    context = appContext,
                    concertId = concertId,
                    title = title.trim(),
                    subtitle = subtitle.trim(),
                    artists = cleanArtists,
                    date = date.trim(),
                    time = time.trim(),
                    endDate = endDate.trim(),
                    endTime = endTime.trim(),
                    town = canonicalTown,
                    comarca = canonicalComarca,
                    venue = venue.trim(),
                    styles = styles.map(String::trim).filter(String::isNotBlank),
                    description = description.trim(),
                    ticketPrice = ticketPrice,
                    clearTicketPrice = !ticketSalesEnabled || ticketPrice == null,
                    ticketSalesEnabled = ticketSalesEnabled,
                    ticketUrl = if (ticketSalesEnabled) ticketUrl.trim() else ""
                ).getOrThrow()
                val savedOrganizers = ServerApiRepository.replaceEventOrganizers(appContext, concertId, cleanOrganizers).getOrThrow()
                val source = visual.imageSource.uppercase()
                val imageUrl = visual.imageUrl
                updated.copy(
                    organizers = savedOrganizers,
                    imageSource = source,
                    imageSourceEntityName = visual.entityName,
                    eventCardImageUri = if (source == "EVENT") imageUrl else updated.eventCardImageUri,
                    artistImageUri = if (source == "ARTIST") imageUrl else updated.artistImageUri,
                    venueImageUri = if (source == "VENUE") imageUrl else updated.venueImageUri,
                    organizerImageUri = if (source == "ORGANIZER") imageUrl else updated.organizerImageUri,
                    townImageUri = if (source == "TOWN") imageUrl else updated.townImageUri,
                    categoryImageUri = if (source == "CATEGORY") imageUrl else updated.categoryImageUri,
                    preferredImageUri = if (source == "AUTO") null else imageUrl,
                    imageFocusX = visual.focusX,
                    imageFocusY = visual.focusY,
                    imageScale = visual.scale
                )
            }
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { updated ->
                    concerts = concerts.map { if (it.id == concertId) updated else it }
                    invalidateFeedCache()
                    refreshRemoteAgenda()
                    onResult(true, "Esdeveniment, organitzadors i imatge guardats.")
                }.onFailure { error ->
                    onResult(false, error.message ?: "No s'han pogut guardar tots els canvis.")
                }
            }
        }
    }

    fun updateConcertCardVisualOnly(
        concertId: Int,
        imageSource: String,
        imageEntityName: String?,
        imageUri: String?,
        removeImage: Boolean,
        imageFocusX: Float = 0.5f,
        imageFocusY: Float = 0.5f,
        imageScale: Float = 1f,
        onProgress: (Int) -> Unit = {},
        onResult: (Boolean, String, String?) -> Unit
    ) {
        val account = currentAccount
        val existing = concerts.firstOrNull { it.id == concertId }
        if (account == null || existing == null || !canManageConcert(existing)) {
            onResult(false, "No tens permisos per canviar el cartell d’este esdeveniment.", null)
            return
        }
        val normalizedSource = imageSource.trim().uppercase().ifBlank { "AUTO" }
        if (account.role != AccountRole.SUPER_ADMIN && normalizedSource !in setOf("AUTO", "EVENT")) {
            onResult(false, "Els editors poden canviar el cartell de l’esdeveniment; les imatges canòniques d’entitats són de Super Admin.", null)
            return
        }
        backgroundExecutor.execute {
            val result = ServerApiRepository.updateEventCardVisual(
                context = appContext,
                concertId = concertId,
                imageSource = normalizedSource,
                entityName = imageEntityName,
                imageUri = imageUri,
                removeImage = removeImage,
                imageFocusX = imageFocusX,
                imageFocusY = imageFocusY,
                imageScale = imageScale,
                onUploadProgress = { percent ->
                    Handler(Looper.getMainLooper()).post { onProgress(percent.coerceIn(0, 100)) }
                }
            )
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { visual ->
                    concerts = concerts.map { item ->
                        if (item.id != concertId) return@map item
                        val source = visual.imageSource.uppercase()
                        val url = visual.imageUrl
                        item.copy(
                            imageSource = source,
                            imageSourceEntityName = visual.entityName,
                            eventCardImageUri = if (source == "EVENT") url else item.eventCardImageUri,
                            artistImageUri = if (source == "ARTIST") url else item.artistImageUri,
                            venueImageUri = if (source == "VENUE") url else item.venueImageUri,
                            organizerImageUri = if (source == "ORGANIZER") url else item.organizerImageUri,
                            townImageUri = if (source == "TOWN") url else item.townImageUri,
                            categoryImageUri = if (source == "CATEGORY") url else item.categoryImageUri,
                            preferredImageUri = if (source == "AUTO") null else url,
                            imageFocusX = visual.focusX,
                            imageFocusY = visual.focusY,
                            imageScale = visual.scale
                        )
                    }
                    invalidateFeedCache()
                    refreshRemoteAgenda()
                    val text = when {
                        removeImage -> "Imatge eliminada i preferència actualitzada."
                        visual.uploaded -> "Foto pujada al servidor i assignada correctament."
                        else -> "Preferència d’imatge guardada."
                    }
                    onResult(true, text, visual.imageUrl)
                }.onFailure { error ->
                    onResult(false, error.message ?: "No s'ha pogut guardar la imatge.", null)
                }
            }
        }
    }

    fun deleteConcertFromEditor(concertId: Int, onResult: (Boolean, String) -> Unit) {
        val existing = concerts.firstOrNull { it.id == concertId }
        if (existing == null || !canDeleteConcert(existing)) {
            onResult(false, "Només el creador o Super Admin poden eliminar este esdeveniment.")
            return
        }
        backgroundExecutor.execute {
            val result = ServerApiRepository.deleteEvent(appContext, concertId)
            Handler(Looper.getMainLooper()).post {
                result.onSuccess {
                    concerts = concerts.filterNot { it.id == concertId }
                    remoteConcerts = remoteConcerts.filterNot { it.id == concertId }
                    remoteConcertIds = remoteConcertIds - concertId
                    savedConcerts = savedConcerts - concertId
                    invalidateFeedCache()
                    onResult(true, "Esdeveniment eliminat.")
                }.onFailure { error ->
                    onResult(false, error.message ?: "No s'ha pogut eliminar l'esdeveniment.")
                }
            }
        }
    }

    fun updateConcertInfo(
        concertId: Int,
        date: String,
        time: String,
        town: String,
        venue: String,
        description: String,
        ticketPrice: Int,
        ticketAvailable: Boolean,
        onResult: (Boolean, String) -> Unit
    ) {
        val existing = concerts.firstOrNull { it.id == concertId }
        if (existing == null || !canManageConcert(existing)) {
            onResult(false, "No tens permisos per editar este esdeveniment.")
            return
        }
        if (date.isBlank() || time.isBlank() || town.isBlank() || venue.isBlank() || ticketPrice < 0) {
            onResult(false, "Revisa data, hora, població, espai i preu.")
            return
        }
        val municipality = municipalityFor(town)
        if (municipalities.isNotEmpty() && municipality == null) {
            onResult(false, "Selecciona la població des de la base de municipis.")
            return
        }
        val canonicalTown = municipality?.name ?: town.trim()
        val canonicalComarca = municipality?.comarca.orEmpty().ifBlank { comarcaForTown(canonicalTown).orEmpty() }
        val newDate = date.trim()
        val newTime = time.trim()
        val newVenue = venue.trim()
        val newDescription = description.trim()

        backgroundExecutor.execute {
            val result = ServerApiRepository.updateEvent(
                context = appContext,
                concertId = concertId,
                date = newDate,
                time = newTime,
                town = canonicalTown,
                comarca = canonicalComarca,
                venue = newVenue,
                description = newDescription,
                ticketPrice = ticketPrice,
                ticketSalesEnabled = ticketAvailable,
                ticketUrl = existing.ticketUrl
            )
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { updated ->
                    val changedFields = buildList {
                        if (existing.date != updated.date) add("data")
                        if (existing.time != updated.time) add("horari")
                        if (existing.town != updated.town || existing.venue != updated.venue) add("lloc")
                        if (existing.description != updated.description) add("informació")
                        if (existing.ticketPrice != updated.ticketPrice || existing.ticketAvailable != updated.ticketAvailable) add("entrades")
                    }
                    concerts = concerts.map { if (it.id == concertId) updated else it }
                    if (changedFields.isNotEmpty()) {
                        concertChangeNotifications = listOf(
                            AgendaNotification(
                                id = "change_${concertId}_${System.currentTimeMillis()}",
                                type = NotificationType.CHANGES,
                                title = "Canvis · ${updated.artist}",
                                body = "S’ha actualitzat: ${changedFields.joinToString(", ")}. ${updated.town} · ${updated.time}",
                                concertId = concertId,
                                sortKey = System.currentTimeMillis()
                            )
                        ) + concertChangeNotifications
                    }
                    refreshRemoteAgenda()
                    onResult(true, "Canvis guardats al servidor.")
                }.onFailure { error ->
                    onResult(false, error.message ?: "No s'han pogut guardar els canvis.")
                }
            }
        }
    }

    fun updateTicketSales(
        concertId: Int,
        ticketUrl: String,
        ticketPrice: Int,
        ticketAvailable: Boolean,
        capacity: Int? = null,
        ticketOptions: List<TicketOption>? = null,
        configureAdvanced: Boolean = false,
        onResult: (Boolean, String) -> Unit
    ) {
        val existing = concerts.firstOrNull { it.id == concertId }
        if (existing == null || !canManageConcert(existing) || ticketPrice < 0) {
            onResult(false, "No s’ha pogut guardar la configuració d’entrades.")
            return
        }
        val cleanUrl = ticketUrl.trim()
        backgroundExecutor.execute {
            val result = runCatching {
                val updated = ServerApiRepository.updateEvent(
                    context = appContext,
                    concertId = concertId,
                    ticketPrice = if (ticketAvailable) ticketPrice else null,
                    clearTicketPrice = !ticketAvailable,
                    ticketSalesEnabled = ticketAvailable,
                    ticketUrl = if (ticketAvailable) cleanUrl else ""
                ).getOrThrow()
                val advanced = if (configureAdvanced) {
                    ServerApiRepository.configureTicketOptions(
                        context = appContext,
                        concertId = concertId,
                        capacity = capacity,
                        options = ticketOptions.orEmpty()
                    ).getOrThrow()
                } else null
                updated to advanced
            }
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { (updated, advanced) ->
                    concerts = concerts.map { if (it.id == concertId) updated.copy(capacity = advanced?.capacity ?: it.capacity, ticketOptions = advanced?.options ?: it.ticketOptions) else it }
                    if (advanced != null) {
                        ticketOptionsByEvent = ticketOptionsByEvent + (concertId to advanced.options)
                        ticketCapacityByEvent = ticketCapacityByEvent + (concertId to advanced.capacity)
                    }
                    concertChangeNotifications = listOf(
                        AgendaNotification(
                            id = "tickets_${concertId}_${System.currentTimeMillis()}",
                            type = NotificationType.TICKETS,
                            title = "Entrades · ${updated.title}",
                            body = if (ticketAvailable) {
                                if (cleanUrl.isBlank()) "Entrades internes amb QR activades." else "Venda externa d’entrades actualitzada."
                            } else "Venda d’entrades desactivada.",
                            concertId = concertId,
                            sortKey = System.currentTimeMillis()
                        )
                    ) + concertChangeNotifications
                    refreshRemoteAgenda()
                    onResult(true, if (ticketAvailable && cleanUrl.isBlank()) "Entrades internes activades. Els QR es generen en cada reserva." else "Configuració d’entrades guardada.")
                }.onFailure { error ->
                    onResult(false, error.message ?: "No s’ha pogut guardar la configuració d’entrades.")
                }
            }
        }
    }

    fun canManageArtist(artist: String): Boolean = currentAccount?.canManageArtist(artist) == true

    fun editableAccounts(): List<AppAccount> {
        val account = currentAccount ?: return emptyList()
        return when (account.role) {
            AccountRole.SUPER_ADMIN -> DemoData.accounts
            AccountRole.GROUP, AccountRole.VENUE -> listOf(account)
            AccountRole.AGENCY, AccountRole.LABEL, AccountRole.PROMOTER -> {
                val assignedGroups = DemoData.accounts.filter {
                    it.role == AccountRole.GROUP && it.managedArtists.any(account.managedArtists::contains)
                }
                (listOf(account) + assignedGroups).distinctBy { it.username }
            }
            AccountRole.USER -> listOf(account)
        }
    }

    fun profileFor(username: String): AccountProfile = accountProfiles[username]
        ?: DemoData.accounts.firstOrNull { it.username == username }?.let(::defaultProfile)
        ?: AccountProfile(username = username, displayName = username)

    fun accountForArtist(artist: String): AppAccount? = DemoData.accounts.firstOrNull {
        it.role == AccountRole.GROUP && artist in it.managedArtists
    }

    fun profileForArtist(artist: String): AccountProfile? = accountForArtist(artist)?.let { profileFor(it.username) }


    /**
     * La foto pública del grup usa primer la imatge canònica central. Així, quan
     * Super Admin la canvia des d'una card, també canvien perfil, llistes i avatars.
     */
    fun publicPhotoForArtist(artist: String): String? {
        // Registra la dependència de Compose, però la consulta real és només RAM.
        entityImageCatalogRevision
        return ServerApiRepository.peekCachedEntityImageUrl("ARTIST", artist)
            ?: profileForArtist(artist)?.photoUri
    }

    fun publicPhotoForCategory(category: CulturalCategory): String? {
        entityImageCatalogRevision
        // En Personalització mostrem la imatge principal de CATEGORY com fem amb
        // l'avatar canònic d'ARTIST. Si el catàleg remot encara no ha arribat,
        // només acceptem la mateixa imatge de categoria incrustada als concerts.
        // No substituïm mai la foto d'una categoria pel cartell aleatori d'un acte.
        return ServerApiRepository.peekCachedEntityImageUrl("CATEGORY", category.label)
            ?: ServerApiRepository.peekCachedEntityImageUrl("CATEGORY", category.name)
            ?: concerts.asSequence()
                .filter { it.category == category }
                .mapNotNull { it.categoryImageUri?.takeIf(String::isNotBlank) }
                .firstOrNull()
    }

    fun publicPhotoForTown(town: String): String? {
        entityImageCatalogRevision
        val municipality = municipalityFor(town)
        val aliases = listOfNotNull(
            town.takeIf(String::isNotBlank),
            municipality?.name?.takeIf(String::isNotBlank),
            municipality?.officialName?.takeIf(String::isNotBlank)
        ).distinctBy(::normalizeSearchValue)
        return aliases.asSequence()
            .mapNotNull { alias -> ServerApiRepository.peekCachedEntityImageUrl("TOWN", alias) }
            .firstOrNull()
            ?: concerts.firstOrNull { event ->
                aliases.any { alias -> normalizeSearchValue(event.town) == normalizeSearchValue(alias) }
            }?.townImageUri
    }

    fun publicPhotoForVenue(venue: String): String? {
        entityImageCatalogRevision
        return ServerApiRepository.peekCachedEntityImageUrl("VENUE", venue)
            ?: concerts.firstOrNull { it.venue.equals(venue, ignoreCase = true) }?.venueImageUri
    }

    /**
     * Candidats visuals del card resolts en temps de render. Açò és important sobretot
     * per al fallback de poble: el Concert pot haver-se materialitzat abans que ENTITATS
     * estiguera en RAM. Quan el catàleg entra, Compose observa entityImageCatalogRevision
     * i pot usar immediatament la foto canònica sense reconstruir tot el feed.
     */
    fun cardImageCandidates(concert: Concert): List<String?> {
        entityImageCatalogRevision
        val artistCanonical = concert.lineup.asSequence()
            .mapNotNull { artist -> ServerApiRepository.peekCachedEntityImageUrl("ARTIST", artist) }
            .firstOrNull()
            ?: publicPhotoForArtist(concert.artist)
        val venueCanonical = publicPhotoForVenue(concert.venue)
        val townCanonical = publicPhotoForTown(concert.town)
        return listOf(
            concert.posterUri,
            concert.eventCardImageUri,
            concert.artistImageUri,
            artistCanonical,
            concert.venueImageUri,
            venueCanonical,
            concert.townImageUri,
            townCanonical
        )
    }

    fun canManageAccount(username: String): Boolean {
        val actor = currentAccount ?: return false
        if (actor.role == AccountRole.SUPER_ADMIN) return true
        if (actor.username.equals(username, ignoreCase = true)) return true
        val target = DemoData.accounts.firstOrNull { it.username.equals(username, ignoreCase = true) } ?: return false
        if (actor.role == AccountRole.AGENCY || actor.role == AccountRole.LABEL || actor.role == AccountRole.PROMOTER) {
            return target.role == AccountRole.GROUP && target.managedArtists.any(actor.managedArtists::contains)
        }
        return false
    }

    // v302: les dades i el rol del perfil són sempre propietat del servidor.
    // No es poden elevar els permisos en local ni canviar SUPER_ADMIN des del perfil.
    var professionalAccountSettings by mutableStateOf<ServerApiRepository.ProfessionalAccountSettings?>(null)
        private set
    var professionalAccountSettingsLoading by mutableStateOf(false)
        private set
    var professionalAccountSettingsOwnerId by mutableStateOf<Int?>(null)
        private set

    fun refreshProfessionalAccountSettings(onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        val account = currentAccount ?: return
        professionalAccountSettingsLoading = true
        professionalAccountSettings = null // Evita mostrar dades fiscals/verificació de la sessió anterior.
        professionalAccountSettingsOwnerId = null
        backgroundExecutor.execute {
            val result = ServerApiRepository.getProfessionalAccountSettings(appContext)
            Handler(Looper.getMainLooper()).post {
                if (currentAccount?.serverUserId != account.serverUserId ||
                    currentAccount?.username != account.username) return@post
                professionalAccountSettingsLoading = false
                result.onSuccess { settings ->
                    professionalAccountSettings = settings
                    professionalAccountSettingsOwnerId = account.serverUserId
                    val current = currentAccount ?: return@onSuccess
                    if (current.role != AccountRole.SUPER_ADMIN) {
                        val serverRole = when (settings.role) {
                            "PROFESSIONAL" -> AccountRole.PROMOTER
                            "PUBLIC", "USER" -> AccountRole.USER
                            else -> current.role // Mantindre rols històrics gestionats al servidor.
                        }
                        if (serverRole != current.role) currentAccount = current.copy(role = serverRole)
                    }
                    onResult(true, "")
                }.onFailure { onResult(false, it.message ?: "No s'han pogut consultar les dades del perfil.") }
            }
        }
    }

    fun updateProfessionalAccountSettings(professional: Boolean, phone: String, onResult: (Boolean, String) -> Unit) {
        val account = currentAccount
        if (account == null || account.serverUserId == null) {
            onResult(false, "Inicia sessió per canviar el rol.")
            return
        }
        if (account.role == AccountRole.SUPER_ADMIN) {
            onResult(false, "El rol Superadmin només es pot gestionar des del servidor.")
            return
        }
        val cleanPhone = phone.trim()
        if (professional && !cleanPhone.matches(Regex("^[+0-9() .-]{7,24}$"))) {
            onResult(false, "Escriu un telèfon vàlid per al perfil professional.")
            return
        }
        backgroundExecutor.execute {
            val result = ServerApiRepository.saveProfessionalAccountSettings(appContext, professional, cleanPhone)
            Handler(Looper.getMainLooper()).post {
                if (currentAccount?.serverUserId != account.serverUserId ||
                    currentAccount?.username != account.username) return@post
                result.onSuccess { settings ->
                    professionalAccountSettings = settings
                    professionalAccountSettingsOwnerId = account.serverUserId
                    val current = currentAccount ?: return@onSuccess
                    val remoteRole = when (settings.role) {
                        "PROFESSIONAL" -> AccountRole.PROMOTER
                        "PUBLIC", "USER" -> AccountRole.USER
                        else -> current.role
                    }
                    // La UI canvia només després que el servidor confirme el canvi.
                    if (current.role != AccountRole.SUPER_ADMIN) currentAccount = current.copy(role = remoteRole)
                    onResult(true, "Rol i dades professionals guardats.")
                }.onFailure { onResult(false, it.message ?: "No s'han pogut guardar les dades professionals.") }
            }
        }
    }

    fun sendMyEmailVerification(onResult: (Boolean, String) -> Unit) {
        val account = currentAccount ?: return
        backgroundExecutor.execute {
            val result = ServerApiRepository.sendWelcomeVerificationEmail(appContext)
            Handler(Looper.getMainLooper()).post {
                if (currentAccount?.serverUserId != account.serverUserId ||
                    currentAccount?.username != account.username) return@post
                result.onSuccess { onResult(true, "Consulta el correu per confirmar l'adreça.") }
                    .onFailure { onResult(false, it.message ?: "No s'ha pogut enviar el correu de verificació.") }
            }
        }
    }

    fun updateOwnAccountRemote(
        username: String,
        email: String,
        homeTown: String,
        displayName: String,
        bio: String,
        photoUri: String?,
        removeImage: Boolean = false,
        onProgress: (Int) -> Unit = {},
        onResult: (Boolean, String, String?) -> Unit
    ) {
        val account = currentAccount
        if (account == null) {
            onResult(false, "Inicia sessió per editar el compte.", null)
            return
        }
        val cleanUsername = username.trim()
        val cleanEmail = email.trim()
        val cleanTown = homeTown.trim()
        if (cleanUsername.length < 3) {
            onResult(false, "El nom d'usuari ha de tindre almenys 3 caràcters.", null)
            return
        }
        if (!cleanEmail.contains("@") || cleanEmail.substringAfterLast("@", "").isBlank()) {
            onResult(false, "Escriu un correu electrònic vàlid.", null)
            return
        }
        if (displayName.isBlank()) {
            onResult(false, "Falta el nom visible.", null)
            return
        }
        val oldUsername = account.username
        val oldProfile = profileFor(oldUsername)
        backgroundExecutor.execute {
            val identityResult = ServerApiRepository.updateMyAccount(
                context = appContext,
                username = cleanUsername,
                email = cleanEmail,
                homeTown = cleanTown,
                displayName = displayName
            )
            val identity = identityResult.getOrElse { error ->
                Handler(Looper.getMainLooper()).post {
                    onResult(false, error.message ?: "No s'han pogut guardar les dades del compte.", null)
                }
                return@execute
            }

            val profileResult = ServerApiRepository.updateUserProfile(
                context = appContext,
                username = identity.username,
                displayName = displayName,
                bio = bio,
                imageUri = photoUri,
                discipline = oldProfile.discipline,
                companyName = oldProfile.companyName,
                castText = oldProfile.castText,
                publisherName = oldProfile.publisherName,
                booksText = oldProfile.booksText,
                worksText = oldProfile.worksText,
                spacesText = oldProfile.spacesText,
                removeImage = removeImage,
                onUploadProgress = { pct -> Handler(Looper.getMainLooper()).post { onProgress(pct) } }
            )

            Handler(Looper.getMainLooper()).post {
                if (!oldUsername.equals(identity.username, ignoreCase = true)) {
                    migrateUsernameScopedCache(oldUsername, identity.username)
                    behaviourProfile = behaviourProfile.copy(username = identity.username)
                }
                val updatedAccount = account.copy(
                    username = identity.username,
                    email = identity.email,
                    displayName = displayName.trim()
                )
                currentAccount = updatedAccount
                store.edit().putString("session_username", identity.username).apply()

                val remote = profileResult.getOrNull()
                val updatedProfile = oldProfile.copy(
                    username = identity.username,
                    displayName = remote?.displayName?.ifBlank { displayName.trim() } ?: displayName.trim(),
                    bio = remote?.bio ?: bio.trim(),
                    photoUri = remote?.imageUrl ?: photoUri,
                    homeTown = identity.homeTown,
                    discipline = remote?.discipline ?: oldProfile.discipline,
                    companyName = remote?.companyName ?: oldProfile.companyName,
                    castText = remote?.castText ?: oldProfile.castText,
                    publisherName = remote?.publisherName ?: oldProfile.publisherName,
                    booksText = remote?.booksText ?: oldProfile.booksText,
                    worksText = remote?.worksText ?: oldProfile.worksText,
                    spacesText = remote?.spacesText ?: oldProfile.spacesText
                )
                accountProfiles = accountProfiles - oldUsername + (identity.username to updatedProfile)
                persistProfile(updatedProfile)

                val revisedLocations = (userPreferences.preferredLocations - oldProfile.homeTown)
                    .let { locations -> if (identity.homeTown.isBlank()) locations else locations + identity.homeTown }
                updateUserPreferences(userPreferences.copy(preferredLocations = revisedLocations))

                if (identity.emailVerificationRequired) {
                    backgroundExecutor.execute { ServerApiRepository.sendWelcomeVerificationEmail(appContext) }
                }
                refreshUserProfilesFromServer()
                if (profileResult.isSuccess) {
                    onResult(true, if (identity.emailVerificationRequired)
                        "Compte guardat. Revisa el nou correu per verificar-lo."
                    else "Compte guardat.", updatedProfile.photoUri)
                } else {
                    onResult(true, "Usuari, correu i població guardats. El perfil visual no s'ha pogut sincronitzar.", updatedProfile.photoUri)
                }
            }
        }
    }

    fun updateAccountProfileRemote(
        username: String,
        displayName: String,
        bio: String,
        photoUri: String?,
        discipline: String = profileFor(username).discipline,
        companyName: String = profileFor(username).companyName,
        castText: String = profileFor(username).castText,
        publisherName: String = profileFor(username).publisherName,
        booksText: String = profileFor(username).booksText,
        worksText: String = profileFor(username).worksText,
        spacesText: String = profileFor(username).spacesText,
        removeImage: Boolean = false,
        onProgress: (Int) -> Unit = {},
        onResult: (Boolean, String, String?) -> Unit
    ) {
        if (!canManageAccount(username) || displayName.isBlank()) {
            onResult(false, "No tens permís o falta el nom.", null)
            return
        }
        backgroundExecutor.execute {
            val result = ServerApiRepository.updateUserProfile(
                context = appContext,
                username = username,
                displayName = displayName,
                bio = bio,
                imageUri = photoUri,
                discipline = discipline,
                companyName = companyName,
                castText = castText,
                publisherName = publisherName,
                booksText = booksText,
                worksText = worksText,
                spacesText = spacesText,
                removeImage = removeImage,
                onUploadProgress = { pct -> Handler(Looper.getMainLooper()).post { onProgress(pct) } }
            )
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { remote ->
                    val updated = AccountProfile(
                        username = remote.username,
                        displayName = remote.displayName,
                        bio = remote.bio,
                        photoUri = remote.imageUrl,
                        homeTown = remote.homeTown.ifBlank { profileFor(remote.username).homeTown },
                        discipline = remote.discipline,
                        companyName = remote.companyName,
                        castText = remote.castText,
                        publisherName = remote.publisherName,
                        booksText = remote.booksText,
                        worksText = remote.worksText,
                        spacesText = remote.spacesText
                    )
                    accountProfiles = accountProfiles + (remote.username to updated)
                    persistProfile(updated)
                    refreshRemoteAgenda()
                    onResult(true, "Perfil i imatge sincronitzats amb el servidor.", remote.imageUrl)
                }.onFailure { error ->
                    onResult(false, error.message ?: "No s'ha pogut sincronitzar el perfil.", null)
                }
            }
        }
    }

    /**
     * Permission is enforced here, not only in the UI.
     * Super Admin can act for any artist. A group can act only for itself.
     * Agency/label accounts can act only for explicitly assigned artists.
     */
    fun publishStory(artist: String, title: String, body: String): Boolean {
        if (!canManageArtist(artist) || title.isBlank()) return false
        val nextId = (stories.maxOfOrNull { it.id } ?: 0) + 1
        stories = listOf(
            Story(
                id = nextId,
                artist = artist,
                initials = artistInitials(artist),
                title = title.trim(),
                body = body.trim().ifBlank { "Batec publicat des del panell professional." },
                publishedAtMillis = System.currentTimeMillis()
            )
        ) + stories
        return true
    }

    fun updateStory(id: Int, title: String, body: String): Boolean {
        val existing = stories.firstOrNull { it.id == id } ?: return false
        if (!canManageArtist(existing.artist) || title.isBlank()) return false
        stories = stories.map {
            if (it.id == id) it.copy(title = title.trim(), body = body.trim()) else it
        }
        return true
    }

    fun deleteStory(id: Int): Boolean {
        val existing = stories.firstOrNull { it.id == id } ?: return false
        if (!canManageArtist(existing.artist)) return false
        stories = stories.filterNot { it.id == id }
        return true
    }

    fun publishPost(artist: String, title: String, body: String): Boolean {
        if (!canManageArtist(artist) || title.isBlank()) return false
        val nextId = (posts.maxOfOrNull { it.id } ?: 0) + 1
        posts = listOf(
            GroupPost(
                id = nextId,
                artist = artist,
                title = title.trim(),
                body = body.trim().ifBlank { "Publicació creada des del panell professional." }
            )
        ) + posts
        return true
    }

    fun updatePost(id: Int, title: String, body: String): Boolean {
        val existing = posts.firstOrNull { it.id == id } ?: return false
        if (!canManageArtist(existing.artist) || title.isBlank()) return false
        posts = posts.map {
            if (it.id == id) it.copy(title = title.trim(), body = body.trim()) else it
        }
        return true
    }

    fun deletePost(id: Int): Boolean {
        val existing = posts.firstOrNull { it.id == id } ?: return false
        if (!canManageArtist(existing.artist)) return false
        posts = posts.filterNot { it.id == id }
        return true
    }

    fun concertById(concertId: Int): Concert? = concertByIdCache[concertId]
        ?: priorityFeedConcertCache[concertId]
        ?: advertisementConcertCache[concertId]
        ?: concerts.firstOrNull { it.id == concertId }

    /**
     * En Personalització → Bloquejats mostrem únicament actes encara vigents.
     * Les exclusions continuen persistides encara que la fitxa haja caducat: no
     * reactivem accidentalment actes antics si la font els torna a publicar.
     * Un esdeveniment de diversos dies només caduca després de l'última jornada.
     */
    fun activeHiddenConcerts(): List<Concert> = hiddenConcertIds.asSequence()
        .mapNotNull(::concertById)
        .filter { isCurrentOrFutureAgendaEvent(it) }
        .sortedWith(compareBy<Concert> { it.date }.thenBy { it.title }.thenBy { it.id })
        .toList()

    fun attendanceFor(concertId: Int): AttendanceStatus =
        attendanceByConcert[concertId] ?: AttendanceStatus.NONE

    fun setAttendance(concertId: Int, status: AttendanceStatus) {
        attendanceByConcert = if (status == AttendanceStatus.NONE) attendanceByConcert - concertId else attendanceByConcert + (concertId to status)
        saveAttendanceCache()
        invalidateNotificationCache(250L)
        val concert = concerts.firstOrNull { it.id == concertId }

        when (status) {
            AttendanceStatus.WANT_TO_GO -> if (concert != null) recordBehaviour(BehaviourKind.WANT_TO_GO, concertId.toString(), concert = concert)
            AttendanceStatus.GOING -> if (concert != null) recordBehaviour(BehaviourKind.GOING, concertId.toString(), concert = concert)
            AttendanceStatus.NONE -> Unit
        }

        if (currentAccount != null && ServerApiRepository.token(appContext) != null && concert != null) {
            val payload = eventServerPayload(concert).put("attendance", status.name)
            postServerStateOrQueue("user-state/attendance.php", payload)
        }

        // "Aniré" activa els avisos operatius del concert automàticament.
        if (status == AttendanceStatus.GOING && concertId !in alertConcerts) {
            alertConcerts = alertConcerts + concertId
            saveIntSet(userCacheKey("alert_concerts"), alertConcerts)
            if (concert != null) postServerStateOrQueue("user-state/event-alert.php", eventServerPayload(concert).put("active", true))
        }
    }

    fun ticketOptionsFor(concert: Concert): List<TicketOption> =
        ticketOptionsByEvent[concert.id] ?: concert.ticketOptions

    fun ticketCapacityFor(concert: Concert): Int? =
        ticketCapacityByEvent[concert.id] ?: concert.capacity

    fun refreshTicketOptions(concertId: Int) {
        if (concertId <= 0) return
        backgroundExecutor.execute {
            ServerApiRepository.fetchTicketOptions(appContext, concertId)
                .onSuccess { info ->
                    Handler(Looper.getMainLooper()).post {
                        ticketOptionsByEvent = ticketOptionsByEvent + (concertId to info.options)
                        ticketCapacityByEvent = ticketCapacityByEvent + (concertId to info.capacity)
                    }
                }
        }
    }

    fun purchaseTicket(
        concertId: Int,
        quantity: Int = 1,
        ticketOptionId: Long? = null,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val concert = concerts.firstOrNull { it.id == concertId }
        val option = concert?.let { c -> ticketOptionId?.let { id -> ticketOptionsFor(c).firstOrNull { it.id == id } } }
        val maxQuantity = option?.maxPerOrder ?: 6
        if (currentAccount == null || concert == null || !concert.ticketAvailable || quantity !in 1..maxQuantity) {
            onResult(false, "No s'han pogut crear les entrades.")
            return
        }
        if (concert.ticketUrl.isNotBlank()) {
            onResult(false, "Este esdeveniment utilitza una venda externa.")
            return
        }
        if (isPurchasingTicket) return
        isPurchasingTicket = true
        ticketOperationError = null
        backgroundExecutor.execute {
            val result = ServerApiRepository.purchaseTickets(appContext, concertId, quantity, ticketOptionId)
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { created ->
                    mergeTickets(created)
                    recordTicketPurchase(concertId, created.size.coerceAtLeast(quantity), created.firstOrNull()?.code.orEmpty())
                    setAttendance(concertId, AttendanceStatus.GOING)

                    // Les reserves gratuïtes també reben el correu transaccional immediat.
                    // purchase.php no retorna l'order_id, així que el servidor resol l'última
                    // comanda interna gratuïta de l'usuari per a este esdeveniment.
                    backgroundExecutor.execute {
                        runCatching {
                            ServerApiRepository.postAuthenticatedJson(
                                appContext,
                                "mail/ticket-receipt-now.php",
                                JSONObject().put("event_id", concertId)
                            )
                        }
                    }

                    ticketOperationError = null
                    onResult(true, if (created.size == 1) "Entrada creada." else "${created.size} entrades creades.")
                }.onFailure { error ->
                    val text = error.message ?: "No s'han pogut crear les entrades."
                    ticketOperationError = text
                    onResult(false, text)
                }
                isPurchasingTicket = false
            }
        }
    }


    fun createStripePayment(
        concertId: Int,
        quantity: Int = 1,
        ticketOptionId: Long? = null,
        onResult: (ServerApiRepository.StripePaymentStart?, String?) -> Unit
    ) {
        val concert = concerts.firstOrNull { it.id == concertId }
        val option = concert?.let { c -> ticketOptionId?.let { id -> ticketOptionsFor(c).firstOrNull { it.id == id } } }
        val maxQuantity = option?.maxPerOrder ?: 6
        if (currentAccount == null || concert == null || !concert.ticketAvailable || concert.ticketUrl.isNotBlank() || quantity !in 1..maxQuantity) {
            onResult(null, "No s'ha pogut iniciar el pagament.")
            return
        }
        val effectivePrice = option?.price ?: concert.ticketPrice
        if (effectivePrice < 0) {
            onResult(null, "Esta modalitat no té preu configurat.")
            return
        }
        if (effectivePrice == 0) {
            onResult(null, "Esta entrada és gratuïta i no necessita Stripe.")
            return
        }
        if (isPurchasingTicket) return
        isPurchasingTicket = true
        ticketOperationError = null
        recordTicketClick(concertId)
        backgroundExecutor.execute {
            val result = ServerApiRepository.createStripePayment(appContext, concertId, quantity, ticketOptionId)
            Handler(Looper.getMainLooper()).post {
                isPurchasingTicket = false
                result.onSuccess { start ->
                    ticketOperationError = null
                    onResult(start, null)
                }.onFailure { error ->
                    val text = error.message ?: "No s'ha pogut iniciar el pagament amb Stripe."
                    ticketOperationError = text
                    onResult(null, text)
                }
            }
        }
    }

    fun finalizeStripePayment(
        concertId: Int,
        orderId: Long,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        if (currentAccount == null || orderId <= 0L) {
            onResult(false, "No s'ha pogut verificar el pagament.")
            return
        }
        if (isPurchasingTicket) return
        isPurchasingTicket = true
        ticketOperationError = null
        backgroundExecutor.execute {
            var lastError: String? = null
            var paidTickets: List<PurchasedTicket>? = null
            var finalStatus = "payment_pending"
            for (attempt in 0 until 10) {
                val result = ServerApiRepository.fetchStripePaymentStatus(appContext, orderId)
                result.onSuccess { status ->
                    finalStatus = status.status
                    if (status.status.equals("paid", ignoreCase = true) && status.tickets.isNotEmpty()) {
                        paidTickets = status.tickets
                    }
                }.onFailure { error ->
                    lastError = error.message
                }
                if (paidTickets != null || finalStatus in listOf("payment_failed", "cancelled", "refunded")) break
                if (attempt < 9) Thread.sleep(700)
            }
            Handler(Looper.getMainLooper()).post {
                isPurchasingTicket = false
                val tickets = paidTickets
                if (!tickets.isNullOrEmpty()) {
                    mergeTickets(tickets)
                    recordTicketPurchase(concertId, tickets.size, "order:$orderId")
                    setAttendance(concertId, AttendanceStatus.GOING)

                    // 0.20.8.10q: quan l'app ja veu la compra pagada i els tiquets creats,
                    // demana immediatament al servidor el correu transaccional.
                    // No depén del webhook de Stripe ni de la periodicitat de correus.
                    // Si falla, webhook + ticket_worker continuen sent xarxa de seguretat.
                    backgroundExecutor.execute {
                        runCatching {
                            ServerApiRepository.postAuthenticatedJson(
                                appContext,
                                "mail/ticket-receipt-now.php",
                                JSONObject().put("order_id", orderId)
                            )
                        }
                    }

                    ticketOperationError = null
                    onResult(true, if (tickets.size == 1) "Pagament confirmat. Entrada creada." else "Pagament confirmat. ${tickets.size} entrades creades.")
                } else {
                    val text = lastError ?: when (finalStatus) {
                        "payment_failed" -> "Stripe ha rebutjat el pagament."
                        "cancelled" -> "El pagament s'ha cancel·lat."
                        "refunded" -> "El pagament està reemborsat."
                        else -> "Pagament rebut. Encara estem esperant la confirmació del servidor; revisa Entrades en uns segons."
                    }
                    ticketOperationError = text
                    onResult(false, text)
                }
            }
        }
    }


    fun refreshSpecialAccessibleEvents(onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        if (currentAccount == null) return
        backgroundExecutor.execute {
            val result=ServerApiRepository.fetchMySpecialEvents(appContext)
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { specialAccessibleEvents=it; onResult(true,null) }
                    .onFailure { specialAccessibleEvents=emptyList(); onResult(false,it.message) }
            }
        }
    }

    fun specialEventRemote(eventId: Int): ServerApiRepository.RemoteSpecialEvent? = specialEventsRemote[eventId]
    fun specialEventManage(eventId: Int): ServerApiRepository.RemoteSpecialManageConfig? = specialEventManageRemote[eventId]

    fun refreshSpecialEvent(eventId: Int, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        backgroundExecutor.execute {
            val result = ServerApiRepository.fetchSpecialEvent(appContext, eventId)
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { remote ->
                    specialEventsRemote = specialEventsRemote + (eventId to remote)
                    onResult(true, null)
                }.onFailure { onResult(false, it.message) }
            }
        }
    }

    fun refreshSpecialEventManage(eventId: Int, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        backgroundExecutor.execute {
            val result = ServerApiRepository.fetchSpecialEventManage(appContext, eventId)
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { remote ->
                    specialEventManageRemote = specialEventManageRemote + (eventId to remote)
                    specialEventsRemote = specialEventsRemote + (eventId to remote.event)
                    onResult(true, null)
                }.onFailure { onResult(false, it.message) }
            }
        }
    }

    private fun runSpecialMutation(eventId: Int, action: () -> Result<org.json.JSONObject>, onResult: (Boolean, String?) -> Unit) {
        backgroundExecutor.execute {
            val result = action()
            if (result.isFailure) {
                Handler(Looper.getMainLooper()).post { onResult(false, result.exceptionOrNull()?.message) }
            } else {
                val refreshed = ServerApiRepository.fetchSpecialEventManage(appContext, eventId)
                Handler(Looper.getMainLooper()).post {
                    refreshed.onSuccess { remote ->
                        specialEventManageRemote = specialEventManageRemote + (eventId to remote)
                        specialEventsRemote = specialEventsRemote + (eventId to remote.event)
                        onResult(true, null)
                    }.onFailure { onResult(true, it.message) }
                }
            }
        }
    }

    fun saveSpecialEvent(eventId:Int,displayTitle:String,heroImageUrl:String,generalInfo:String,startAt:String?,venueLabel:String,enabled:Boolean=true,communityEnabled:Boolean=true,onResult:(Boolean,String?)->Unit={_,_->}) = runSpecialMutation(eventId,{ServerApiRepository.saveSpecialEvent(appContext,eventId,displayTitle,heroImageUrl,generalInfo,startAt,venueLabel,enabled,communityEnabled)},onResult)
    fun saveSpecialSchedule(eventId:Int,id:Long,title:String,startAt:String,endAt:String?,location:String,details:String,artistName:String?,imageUrl:String?,sortOrder:Int=0,artistId:Long?=null,onResult:(Boolean,String?)->Unit={_,_->}) = runSpecialMutation(eventId,{ServerApiRepository.saveSpecialSchedule(appContext,eventId,id,title,startAt,endAt,location,details,artistName,imageUrl,sortOrder,artistId)},onResult)
    fun deleteSpecialSchedule(eventId:Int,id:Long,onResult:(Boolean,String?)->Unit={_,_->}) = runSpecialMutation(eventId,{ServerApiRepository.deleteSpecialSchedule(appContext,eventId,id)},onResult)
    fun saveSpecialService(eventId:Int,id:Long,name:String,description:String,imageUrl:String,overlayColor:String,onResult:(Boolean,String?)->Unit={_,_->}) = runSpecialMutation(eventId,{ServerApiRepository.saveSpecialService(appContext,eventId,id,name,description,imageUrl,overlayColor)},onResult)
    fun deleteSpecialService(eventId:Int,id:Long,onResult:(Boolean,String?)->Unit={_,_->}) = runSpecialMutation(eventId,{ServerApiRepository.deleteSpecialService(appContext,eventId,id)},onResult)
    fun saveSpecialProduct(eventId:Int,serviceId:Long,id:Long,name:String,description:String,imageUrl:String,priceCents:Int,reserveOnly:Boolean,stock:Int?,onResult:(Boolean,String?)->Unit={_,_->}) = runSpecialMutation(eventId,{ServerApiRepository.saveSpecialProduct(appContext,eventId,serviceId,id,name,description,imageUrl,priceCents,reserveOnly,stock)},onResult)
    fun deleteSpecialProduct(eventId:Int,serviceId:Long,id:Long,onResult:(Boolean,String?)->Unit={_,_->}) = runSpecialMutation(eventId,{ServerApiRepository.deleteSpecialProduct(appContext,eventId,serviceId,id)},onResult)
    fun saveSpecialRole(eventId:Int,id:Long,name:String,editEvent:Boolean,editSchedule:Boolean,editServices:Boolean,manageStaff:Boolean,viewAllOrders:Boolean,services:List<ServerApiRepository.RemoteSpecialRoleService>,onResult:(Boolean,String?)->Unit={_,_->}) = runSpecialMutation(eventId,{ServerApiRepository.saveSpecialRole(appContext,eventId,id,name,editEvent,editSchedule,editServices,manageStaff,viewAllOrders,services)},onResult)
    fun assignSpecialStaff(eventId:Int,username:String,roleId:Long,onResult:(Boolean,String?)->Unit={_,_->}) = runSpecialMutation(eventId,{ServerApiRepository.assignSpecialStaff(appContext,eventId,username,roleId)},onResult)
    fun removeSpecialStaff(eventId:Int,staffId:Long,onResult:(Boolean,String?)->Unit={_,_->}) = runSpecialMutation(eventId,{ServerApiRepository.removeSpecialStaff(appContext,eventId,staffId)},onResult)

    fun createSpecialServicePayment(
        concertId: Int,
        serviceId: Long,
        serviceName: String,
        items: List<Triple<Long, String, Int>>,
        onResult: (ServerApiRepository.SpecialServicePaymentStart?, String?) -> Unit
    ) {
        if (currentAccount == null || items.isEmpty()) {
            onResult(null, "No s'ha pogut iniciar el pagament del servei.")
            return
        }
        if (isPurchasingTicket) return
        isPurchasingTicket = true
        ticketOperationError = null
        backgroundExecutor.execute {
            val result = ServerApiRepository.createSpecialServicePayment(appContext, concertId, serviceId, serviceName, items)
            Handler(Looper.getMainLooper()).post {
                isPurchasingTicket = false
                result.onSuccess { start ->
                    ticketOperationError = null
                    onResult(start, null)
                }.onFailure { error ->
                    val text = error.message ?: "No s'ha pogut iniciar el pagament amb Stripe."
                    ticketOperationError = text
                    onResult(null, text)
                }
            }
        }
    }

    fun finalizeSpecialServicePayment(
        orderId: Long,
        onResult: (ServerApiRepository.SpecialServicePaidOrder?, String?) -> Unit
    ) {
        if (currentAccount == null || orderId <= 0L) {
            onResult(null, "No s'ha pogut verificar la compra.")
            return
        }
        if (isPurchasingTicket) return
        isPurchasingTicket = true
        ticketOperationError = null
        backgroundExecutor.execute {
            var paid: ServerApiRepository.SpecialServicePaidOrder? = null
            var lastError: String? = null
            var finalStatus = "payment_pending"
            for (attempt in 0 until 10) {
                val result = ServerApiRepository.fetchSpecialServicePaymentStatus(appContext, orderId)
                result.onSuccess { order ->
                    finalStatus = order.status
                    if (order.status.equals("paid", ignoreCase = true) && order.qrPayload.isNotBlank()) paid = order
                }.onFailure { lastError = it.message }
                if (paid != null || finalStatus in listOf("payment_failed", "cancelled", "redeemed")) break
                if (attempt < 9) Thread.sleep(700)
            }
            Handler(Looper.getMainLooper()).post {
                isPurchasingTicket = false
                if (paid != null) {
                    ticketOperationError = null
                    onResult(paid, null)
                } else {
                    val text = lastError ?: "El pagament encara no està confirmat pel servidor."
                    ticketOperationError = text
                    onResult(null, text)
                }
            }
        }
    }

    fun inspectSpecialServiceOrder(
        qrToken: String,
        onResult: (ServerApiRepository.SpecialServiceInspection?, String?) -> Unit
    ) {
        backgroundExecutor.execute {
            val result = ServerApiRepository.inspectSpecialServiceOrder(appContext, qrToken)
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { onResult(it, null) }
                    .onFailure { onResult(null, it.message ?: "No s'ha pogut comprovar la comanda.") }
            }
        }
    }

    fun redeemSpecialServiceOrder(
        qrToken: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        backgroundExecutor.execute {
            val result = ServerApiRepository.redeemSpecialServiceOrder(appContext, qrToken)
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { status -> onResult(status == "redeemed" || status == "already_redeemed", status) }
                    .onFailure { onResult(false, it.message ?: "No s'ha pogut validar la comanda.") }
            }
        }
    }

    fun advertisementPriceCents(
        placement: AdPlacement,
        durationDays: Int,
        targetFollowers: Boolean,
        targetInterests: Boolean,
        targetHighIntent: Boolean
    ): Int {
        val base = when (placement) {
            AdPlacement.FEED_NORMAL -> when (durationDays) { 1 -> 100; 3 -> 250; 7 -> 500; else -> 0 }
            AdPlacement.FEED_PREMIUM -> when (durationDays) { 1 -> 200; 3 -> 500; 7 -> 1000; else -> 0 }
            AdPlacement.HEADER -> when (durationDays) { 1 -> 400; 3 -> 900; 7 -> 1800; else -> 0 }
            AdPlacement.POPUP -> when (durationDays) { 1 -> 700; 3 -> 1600; 7 -> 3000; else -> 0 }
        }
        var extra = 0.0
        if (targetFollowers) extra += 0.10
        if (targetInterests) extra += 0.10
        if (targetHighIntent) extra += 0.15
        extra = extra.coerceAtMost(0.30)
        return kotlin.math.round(base * (1.0 + extra)).toInt()
    }

    fun refreshAdvertisementsFromServer(onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        if (!ServerApiRepository.isConfigured()) return
        backgroundExecutor.execute {
            val result = ServerApiRepository.fetchActiveAdvertisements(appContext)
            val snapshot = result.getOrNull()
            val incoming = snapshot?.advertisements.orEmpty()
            val priorityConcerts = snapshot?.priorityConcerts.orEmpty()
            val priorityIds = snapshot?.priorityEventIds.orEmpty()

            // Els events publicitaris arriben independentment de la finestra cronològica
            // i del filtre de l'usuari. Els detalls del servidor tenen prioritat.
            val includedConcerts = snapshot?.concerts.orEmpty()
            val includedIds = includedConcerts.asSequence().map { it.id }.toSet()
            val adConcertIds = incoming.asSequence().map { it.concertId }
                .filter { it > 0 && it !in includedIds }.distinct().toList()
            val cachedConcerts = if (adConcertIds.isNotEmpty()) {
                runCatching { ServerApiRepository.loadCachedEventsByIds(appContext, adConcertIds) }.getOrDefault(emptyList())
            } else emptyList()
            val adConcerts = includedConcerts + cachedConcerts
            Handler(Looper.getMainLooper()).post {
                result.onSuccess {
                    advertisements = incoming
                    // Substitució atòmica també quan ja no queda cap anunci vigent.
                    advertisementConcertCache = adConcerts.associateBy { concert -> concert.id }
                    priorityFeedConcertCache = priorityConcerts.associateBy { concert -> concert.id }
                    priorityFeedEventIds = priorityIds + priorityConcerts.map { it.id }
                    // La UI incorpora/retira els garantits sense refer la fotografia
                    // permanent de les preferències de l'usuari ni perdre l'scroll.
                    feedRenderRevision += 1L
                    saveAdvertisements()
                    onResult(true, null)
                }.onFailure { error ->
                    onResult(false, error.message)
                }
            }
        }
    }

    fun refreshMyAdvertisementsFromServer(onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        val account = currentAccount ?: return
        if (!ServerApiRepository.isConfigured()) return
        backgroundExecutor.execute {
            val result = ServerApiRepository.fetchMyAdvertisements(appContext)
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { incoming ->
                    val others = advertisements.filterNot { it.ownerUsername.equals(account.username, ignoreCase = true) }
                    advertisements = (incoming + others).distinctBy { it.id }.sortedByDescending { it.createdAtMillis }
                    saveAdvertisements()
                    onResult(true, null)
                }.onFailure { error -> onResult(false, error.message) }
            }
        }
    }

    fun updateAdvertisement(
        adId: Int,
        targetStyles: Set<String>,
        targetSearchTerms: Set<String>,
        targetTowns: Set<String>,
        targetComarques: Set<String>,
        targetFollowers: Boolean,
        targetHighIntent: Boolean,
        matchAllCriteria: Boolean,
        onResult: (Boolean, String) -> Unit
    ) {
        val account = currentAccount
        val ad = advertisements.firstOrNull { it.id == adId }
        if (account == null || ad == null || (account.role != AccountRole.SUPER_ADMIN && !ad.ownerUsername.equals(account.username, ignoreCase = true))) {
            onResult(false, "Només el creador de l'anunci pot editar-lo.")
            return
        }
        backgroundExecutor.execute {
            val result = ServerApiRepository.updateAdvertisement(
                context = appContext,
                adId = adId,
                targetStyles = targetStyles,
                targetSearchTerms = targetSearchTerms,
                targetTowns = targetTowns,
                targetComarques = targetComarques,
                targetFollowers = targetFollowers,
                targetHighIntent = targetHighIntent,
                matchAllCriteria = matchAllCriteria
            )
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { updated ->
                    advertisements = advertisements.map { if (it.id == adId) updated else it }
                    saveAdvertisements()
                    onResult(true, "Anunci actualitzat.")
                }.onFailure { error ->
                    onResult(false, error.message ?: "No s'ha pogut editar l'anunci.")
                }
            }
        }
    }

    fun updateAdvertisementVisual(
        adId: Long,
        cardFocusX: Float,
        cardFocusY: Float,
        imageUri: String? = null,
        removeImage: Boolean = false,
        onProgress: (Int) -> Unit = {},
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        if (adId <= 0L) {
            onResult(false, "Anunci no vàlid.")
            return
        }
        backgroundExecutor.execute {
            val result = ServerApiRepository.updateAdvertisementVisual(
                appContext, adId, cardFocusX, cardFocusY, imageUri, removeImage,
                onUploadProgress = { pct -> Handler(Looper.getMainLooper()).post { onProgress(pct) } }
            )
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { updated ->
                    if (updated != null) {
                        advertisements = advertisements.map { if (it.id == updated.id) updated else it }
                        saveAdvertisements()
                    } else {
                        advertisements = advertisements.map {
                            if (it.id.toLong() == adId) it.copy(
                                cardFocusX = cardFocusX.coerceIn(0f, 1f),
                                cardFocusY = cardFocusY.coerceIn(0f, 1f),
                                imageUri = if (removeImage) null else it.imageUri
                            ) else it
                        }
                        saveAdvertisements()
                    }
                    onResult(true, "Imatge i composició de l'anunci sincronitzades.")
                }.onFailure { error -> onResult(false, error.message ?: "No s'ha pogut guardar la imatge de l'anunci.") }
            }
        }
    }

    fun createAdvertisementPayment(
        concertId: Int,
        placement: AdPlacement,
        durationDays: Int,
        targetStyles: Set<String>,
        targetSearchTerms: Set<String>,
        targetTowns: Set<String>,
        targetComarques: Set<String>,
        targetFollowers: Boolean,
        targetHighIntent: Boolean,
        matchAllCriteria: Boolean,
        onResult: (ServerApiRepository.AdPaymentStart?, String?) -> Unit
    ) {
        val concert = concerts.firstOrNull { it.id == concertId }
        if (currentAccount == null || concert == null || !canManageConcert(concert)) {
            onResult(null, "No pots promocionar este esdeveniment.")
            return
        }
        if (durationDays !in setOf(1, 3, 7)) {
            onResult(null, "La duració ha de ser 1, 3 o 7 dies.")
            return
        }
        if (isPurchasingAdvertisement) return
        isPurchasingAdvertisement = true
        advertisementOperationError = null
        backgroundExecutor.execute {
            val result = ServerApiRepository.createAdvertisementPayment(
                context = appContext,
                concertId = concertId,
                placement = placement,
                durationDays = durationDays,
                targetStyles = targetStyles,
                targetSearchTerms = targetSearchTerms,
                targetTowns = targetTowns,
                targetComarques = targetComarques,
                targetFollowers = targetFollowers,
                targetHighIntent = targetHighIntent,
                matchAllCriteria = matchAllCriteria
            )
            Handler(Looper.getMainLooper()).post {
                isPurchasingAdvertisement = false
                result.onSuccess { start ->
                    advertisementOperationError = null
                    onResult(start, null)
                }.onFailure { error ->
                    val message = error.message ?: "No s'ha pogut iniciar el pagament de l'anunci."
                    advertisementOperationError = message
                    onResult(null, message)
                }
            }
        }
    }

    fun finalizeAdvertisementPayment(
        adId: Long,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        if (currentAccount == null || adId <= 0L) {
            onResult(false, "No s'ha pogut verificar l'anunci.")
            return
        }
        if (isPurchasingAdvertisement) return
        isPurchasingAdvertisement = true
        advertisementOperationError = null
        backgroundExecutor.execute {
            var lastError: String? = null
            var finalStatus = "payment_pending"
            var activated: EventAdvertisement? = null
            for (attempt in 0 until 10) {
                val result = ServerApiRepository.fetchAdvertisementPaymentStatus(appContext, adId)
                result.onSuccess { status ->
                    finalStatus = status.status
                    if (status.status.equals("active", ignoreCase = true)) activated = status.advertisement
                }.onFailure { error -> lastError = error.message }
                if (activated != null || finalStatus in listOf("payment_failed", "cancelled")) break
                if (attempt < 9) Thread.sleep(700)
            }

            Handler(Looper.getMainLooper()).post {
                isPurchasingAdvertisement = false
                if (activated != null) {
                    refreshAdvertisementsFromServer()
                    advertisementOperationError = null
                    onResult(true, "Pagament confirmat. L'anunci ja està actiu.")
                } else {
                    val message = lastError ?: when (finalStatus) {
                        "payment_failed" -> "Stripe ha rebutjat el pagament de l'anunci."
                        "cancelled" -> "El pagament de l'anunci s'ha cancel·lat."
                        else -> "Pagament rebut. L'anunci encara està esperant confirmació."
                    }
                    advertisementOperationError = message
                    onResult(false, message)
                }
            }
        }
    }

    fun recordAdvertisementImpression(ad: EventAdvertisement) {
        backgroundExecutor.execute { ServerApiRepository.trackAdvertisement(appContext, ad.id, ad.placement, click = false) }
    }

    fun recordAdvertisementClick(ad: EventAdvertisement) {
        backgroundExecutor.execute { ServerApiRepository.trackAdvertisement(appContext, ad.id, ad.placement, click = true) }
    }

    fun shouldShowPopupAdvertisement(adId: Int): Boolean {
        val today = LocalDate.now().toString()
        return store.getString("ad_popup_last_day", "") != today
    }

    fun markPopupAdvertisementShown(adId: Int) {
        store.edit()
            .putString("ad_popup_last_day", LocalDate.now().toString())
            .putInt("ad_popup_last_id", adId)
            .apply()
    }

    /**
     * El servidor és la font de veritat. Abans de descarregar l'estat, reintenta
     * les operacions que l'usuari haja fet sense xarxa. Després substitueix la cache local.
     */
    fun refreshUserStateFromServer(onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        val account = currentAccount ?: return
        if (ServerApiRepository.token(appContext).isNullOrBlank()) return
        if (store.getBoolean(userCacheKey("places_remote_dirty"), false)) synchronizePlacesWithServer()
        if (store.getBoolean(userCacheKey("feed_prefs_remote_dirty"), false)) synchronizeFeedPreferencesWithServer()
        // La sincronització remota no ha de provocar una recàrrega visual si el servidor
        // ens torna exactament els mateixos senyals que ja teníem en cache local.
        val preferenceSignatureBefore = forYouPreferenceSignature()
        backgroundExecutor.execute {
            flushPendingServerPosts()
            val result = ServerApiRepository.fetchUserState(appContext)
            val visibilityRemote = ServerApiRepository.getAuthenticatedJson(appContext, "visibility/index.php")
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { snapshot ->
                    followedArtists = snapshot.follows
                        .filter { it.entityType.equals("ARTIST", true) }
                        .map { it.entityLabel.ifBlank { followDisplayFromKey(it.entityKey) } }
                        .filter(String::isNotBlank).toSet()
                    followedOrganizers = snapshot.follows
                        .filter { it.entityType.equals("ORGANIZER", true) }
                        .map { it.entityKey }
                        .filter(String::isNotBlank).toSet()
                    followedVenues = snapshot.follows
                        .filter { it.entityType.equals("VENUE", true) }
                        .map { it.entityLabel.ifBlank { it.entityKey } }
                        .filter(String::isNotBlank).toSet()
                    followedCategories = CategoryTaxonomy.canonicalizeFollowSet(
                        snapshot.follows
                            .filter { it.entityType.equals("CATEGORY", true) }
                            .map { it.entityKey.uppercase() }
                            .filter(String::isNotBlank)
                            .toSet()
                    )
                    savedConcerts = snapshot.eventStates.filter { it.saved }.map { it.eventId }.toSet()
                    likedConcerts = snapshot.eventStates.filter { it.liked }.map { it.eventId }.toSet()
                    alertConcerts = snapshot.eventAlerts
                    attendanceByConcert = snapshot.eventStates.mapNotNull { item ->
                        val status = runCatching { AttendanceStatus.valueOf(item.attendance.uppercase()) }.getOrNull() ?: AttendanceStatus.NONE
                        if (status == AttendanceStatus.NONE) null else item.eventId to status
                    }.toMap()

                    saveStringSet(userCacheKey("followed_artists"), followedArtists)
                    saveStringSet(userCacheKey("followed_organizers"), followedOrganizers)
                    saveStringSet(userCacheKey("followed_venues"), followedVenues)
                    saveStringSet(userCacheKey("followed_categories"), followedCategories)
                    saveIntSet(userCacheKey("saved_concerts"), savedConcerts)
                    saveIntSet(userCacheKey("liked_concerts"), likedConcerts)
                    saveIntSet(userCacheKey("alert_concerts"), alertConcerts)
                    saveAttendanceCache()

                    visibilityRemote.onSuccess { result ->
                        val exclusions = result.optJSONObject("exclusions") ?: JSONObject()
                        blockedArtists = jsonStringSet(exclusions, "artists")
                        hiddenConcertIds = jsonIntSet(exclusions, "events")
                        blockedCategories = jsonStringSet(exclusions, "categories")
                            .mapNotNull { key -> enumValues<CulturalCategory>().firstOrNull { it.name == key } }.toSet()
                        blockedSubcategories = blockedSubcategories +
                            CategoryTaxonomy.canonicalizeFollowSet(jsonStringSet(exclusions, "categories"))
                                .filter(CategoryTaxonomy::isAnySubcategoryKey).toSet()
                        saveStringSet(userCacheKey("blocked_subcategories"), blockedSubcategories)
                        saveStringSet(userCacheKey("blocked_artists"), blockedArtists)
                        saveIntSet(userCacheKey("hidden_concerts"), hiddenConcertIds)
                        saveStringSet(userCacheKey("blocked_categories"), blockedCategories.map { it.name }.toSet())
                        onVisibilityExclusionsChanged()
                    }

                    snapshot.notificationPreferences?.let { prefs ->
                        updateAlertPreferences(
                            alertPreferences.copy(
                                followedArtist = prefs.followedArtistConcerts,
                                favoriteStyle = prefs.tasteSuggestions,
                                homeTown = prefs.nearbySuggestions,
                                homeComarca = prefs.nearbySuggestions
                            )
                        )
                        store.edit().putBoolean("alert_followed_news", prefs.followedArtistNews).apply()
                    }

                    val cachedDefaults = behaviourProfile.adDefaults
                    val localFilterSignals = behaviourProfile.events.filter {
                        it.kind == BehaviourKind.MAX_DISTANCE || it.kind == BehaviourKind.MAX_PRICE || it.kind == BehaviourKind.FREE_ONLY
                    }
                    val remoteProfile = profileFromRemoteAffinities(account.username, snapshot.affinities, cachedDefaults)
                    behaviourProfile = remoteProfile.copy(
                        events = (remoteProfile.events + localFilterSignals)
                            .sortedBy { it.timestampMillis }
                            .takeLast(1000),
                        updatedAtMillis = System.currentTimeMillis()
                    )
                    saveBehaviourProfile(behaviourProfile)

                    // 0.20.8.10fg: el compte remot és la base canònica. Només DESPRÉS
                    // fusionem l'activitat del convidat i pugem les diferències. Això evita
                    // que l'estat guest puga substituir likes/guardats/seguiments antics.
                    val guestToMerge = pendingGuestMerge
                    if (guestToMerge != null) {
                        mergeGuestStatePayload(
                            guestToMerge,
                            persist = true,
                            pushToAccount = true,
                            refreshFeed = false
                        )
                        pendingGuestMerge = null
                        clearGuestStateAfterAccountMerge()
                    }

                    applyAutomaticDefaultFeedMode()
                    val preferenceSignatureAfter = forYouPreferenceSignature()
                    if (preferenceSignatureAfter != preferenceSignatureBefore) {
                        Log.d("AgendaPerf", "remote+guest userState changed prefs · refresh Per a tu")
                        cachedVisibleConcertsKey = null
                        scheduleVisibleConcertsRefresh(350L)
                    } else {
                        Log.d("AgendaPerf", "remote+guest userState same prefs · keep Per a tu")
                    }
                    invalidateNotificationCache(250L)
                    onResult(true, null)
                }.onFailure { error ->
                    // Sense xarxa: conservem el cache local del compte i hi fusionem igualment
                    // el guest. Les diferències queden en la cua autenticada i es pujaran en
                    // la pròxima sincronització; per tant tampoc es perd cap estat preexistent.
                    pendingGuestMerge?.let { guest ->
                        mergeGuestStatePayload(
                            guest,
                            persist = true,
                            pushToAccount = true,
                            refreshFeed = true
                        )
                        pendingGuestMerge = null
                        clearGuestStateAfterAccountMerge()
                    }
                    onResult(false, error.message)
                }
            }
        }
    }

    /** Compatibilitat amb pantalles antigues: ara els guardats es carreguen dins de l'estat central. */
    fun refreshSavedConcertsForCurrentUser(onResult: (Boolean, String?) -> Unit = { _, _ -> }) =
        refreshUserStateFromServer(onResult)

    fun refreshTicketsForCurrentUser(onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        if (currentAccount == null || ServerApiRepository.token(appContext).isNullOrBlank()) return
        backgroundExecutor.execute {
            val result = ServerApiRepository.fetchMyTickets(appContext)
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { incoming ->
                    val username = currentAccount?.username
                    issuedTickets = if (username == null) incoming else {
                        issuedTickets.filterNot { it.buyerUsername.equals(username, ignoreCase = true) } + incoming
                    }
                    saveIssuedTickets()
                    refreshTicketPurchaseCache()
                    invalidateNotificationCache(250L)
                    onResult(true, null)
                }.onFailure { error -> onResult(false, error.message) }
            }
        }
    }

    fun refreshManagedTickets(concertId: Int, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        val concert = concerts.firstOrNull { it.id == concertId } ?: return
        if (!canManageConcert(concert)) return
        backgroundExecutor.execute {
            val result = ServerApiRepository.fetchManagedTickets(appContext, concertId)
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { incoming ->
                    issuedTickets = issuedTickets.filterNot { it.concertId == concertId } + incoming
                    saveIssuedTickets()
                    refreshTicketPurchaseCache()
                    onResult(true, null)
                }.onFailure { error -> onResult(false, error.message) }
            }
        }
    }

    fun validateTicketRemote(
        concertId: Int,
        code: String,
        onResult: (ServerApiRepository.TicketValidationResult?, String?) -> Unit
    ) {
        backgroundExecutor.execute {
            val result = ServerApiRepository.validateTicket(appContext, concertId, code)
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { validation ->
                    validation.ticket?.let(::mergeTicket)
                    onResult(validation, null)
                }.onFailure { error ->
                    onResult(null, error.message ?: "No s'ha pogut comprovar l'entrada.")
                }
            }
        }
    }

    fun setTicketStatusRemote(
        concertId: Int,
        code: String,
        status: TicketStatus,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        backgroundExecutor.execute {
            val result = ServerApiRepository.setTicketStatus(appContext, concertId, code, status)
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { updated ->
                    mergeTicket(updated)
                    onResult(true, when (status) {
                        TicketStatus.ACTIVE -> "Entrada reactivada."
                        TicketStatus.CANCELLED -> "Entrada anul·lada."
                        TicketStatus.USED -> "Entrada actualitzada."
                    })
                }.onFailure { error -> onResult(false, error.message ?: "No s'ha pogut actualitzar l'entrada.") }
            }
        }
    }

    private fun mergeTickets(incoming: List<PurchasedTicket>) {
        if (incoming.isEmpty()) return
        val codes = incoming.map { it.code }.toSet()
        issuedTickets = issuedTickets.filterNot { it.code in codes } + incoming
        saveIssuedTickets()
        refreshTicketPurchaseCache()
        // El QR queda disponible sense xarxa fins i tot si Android mata el procés.
        backgroundExecutor.execute { incoming.forEach { TicketQrCache.ensureCached(appContext, it.code) } }
    }

    private fun mergeTicket(incoming: PurchasedTicket) {
        issuedTickets = issuedTickets.filterNot { it.code.equals(incoming.code, ignoreCase = true) } + incoming
        saveIssuedTickets()
        refreshTicketPurchaseCache()
        backgroundExecutor.execute { TicketQrCache.ensureCached(appContext, incoming.code) }
    }

    fun ticketQuantityFor(concertId: Int): Int = ticketQuantityByConcert[concertId] ?: 0

    fun ticketsForCurrentUser(concertId: Int? = null): List<PurchasedTicket> {
        val username = currentAccount?.username ?: return emptyList()
        return issuedTickets
            .filter { it.buyerUsername.equals(username, ignoreCase = true) && (concertId == null || it.concertId == concertId) }
            .sortedBy { it.purchasedAtMillis }
    }

    fun ticketsForManagedConcert(concertId: Int): List<PurchasedTicket> {
        val concert = concerts.firstOrNull { it.id == concertId } ?: return emptyList()
        if (!canManageConcert(concert)) return emptyList()
        return issuedTickets.filter { it.concertId == concertId }.sortedByDescending { it.purchasedAtMillis }
    }

    fun findManagedTicket(concertId: Int, rawCode: String): PurchasedTicket? {
        val clean = rawCode.filter(Char::isLetterOrDigit).uppercase()
        if (clean.isBlank()) return null
        return ticketsForManagedConcert(concertId).firstOrNull { it.code.equals(clean, ignoreCase = true) }
    }

    fun groupsForConcert(concertId: Int): List<CommunityGroup> =
        communityGroups.filter { it.concertId == concertId }.sortedBy { it.type.ordinal }


    /**
     * Retorna només la identitat mínima que es pot mostrar en comunitats.
     * Els comptes particulars no tenen perfil públic navegable.
     */
    fun communityIdentity(author: String): CommunityIdentity {
        val account = DemoData.accounts.firstOrNull { profileFor(it.username).displayName == author }
        if (account != null) {
            val profile = profileFor(account.username)
            val official = account.role != AccountRole.USER
            val tastes = when (account.role) {
                AccountRole.USER -> if (account.username == currentAccount?.username) {
                    val chosen = buildList {
                        inferredStylePreferences().take(3).forEach(::add)
                        addAll(userPreferences.favoriteArtists.take(2))
                    }.distinct().take(3)
                    chosen.ifEmpty {
                        DemoData.communityPeople.firstOrNull { it.displayName == profile.displayName }
                            ?.tastes.orEmpty().take(3)
                    }
                } else {
                    DemoData.communityPeople.firstOrNull { it.displayName == profile.displayName }
                        ?.tastes.orEmpty().take(3)
                }
                AccountRole.GROUP -> {
                    val artist = account.managedArtists.firstOrNull()
                    concerts.firstOrNull { artist in it.lineup }?.styles.orEmpty().take(3)
                }
                else -> account.managedArtists.flatMap { managed ->
                    concerts.firstOrNull { it.artist == managed }?.styles.orEmpty()
                }.distinct().take(3)
            }
            return CommunityIdentity(
                displayName = profile.displayName,
                tastes = tastes,
                photoUri = profile.photoUri,
                official = official
            )
        }
        return DemoData.communityPeople.firstOrNull { it.displayName == author }
            ?: CommunityIdentity(displayName = author)
    }

    fun messagesForConcert(concertId: Int): List<ChatMessage> =
        chatMessages.filter { it.concertId == concertId }.sortedBy { it.id }

    fun messagesForCommunityGroup(groupId: Int): List<CommunityChatMessage> =
        communityChatMessages.filter { it.groupId == groupId }.sortedBy { it.id }

    fun requestsForCommunityGroup(groupId: Int): List<CommunityMemberRequest> =
        communityRequestsByGroup[groupId].orEmpty()

    fun isMemberOfCommunityGroup(groupId: Int): Boolean =
        communityGroups.firstOrNull { it.id == groupId }?.myStatus == "ACCEPTED"

    fun isCreatorOfCommunityGroup(groupId: Int): Boolean =
        communityGroups.firstOrNull { it.id == groupId }?.myRole == "CREATOR"

    fun refreshCommunityGroups(eventId: Int? = null, mine: Boolean = false) {
        if (currentAccount == null || ServerApiRepository.token(appContext).isNullOrBlank()) return
        backgroundExecutor.execute {
            val result = ServerApiRepository.fetchCommunityGroups(appContext, eventId, mine)
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { incoming ->
                    communityGroups = if (eventId != null) {
                        val ids = incoming.map { it.id }.toSet()
                        (communityGroups.filterNot { it.concertId == eventId || it.id in ids } + incoming)
                            .distinctBy { it.id }
                    } else incoming
                    communityOperationMessage = null
                }.onFailure { error ->
                    communityOperationMessage = error.message ?: "No s'ha pogut carregar la comunitat."
                }
            }
        }
    }

    private fun mergeCommunityGroup(group: CommunityGroup) {
        communityGroups = (communityGroups.filterNot { it.id == group.id } + group)
            .sortedWith(compareBy<CommunityGroup> { it.concertId }.thenBy { it.id })
    }

    fun createCommunityGroup(
        concertId: Int,
        type: CommunityGroupType,
        accessMode: CommunityAccessMode,
        town: String,
        meetingAt: String?,
        title: String,
        description: String,
        capacity: Int?,
        chatEnabled: Boolean = true,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        if (currentAccount == null) { onResult(false, "Inicia sessió per crear un tauló."); return }
        if (title.isBlank()) { onResult(false, "Cal indicar les dades del tauló."); return }
        backgroundExecutor.execute {
            val result = ServerApiRepository.createCommunityGroup(
                appContext, concertId, type, accessMode, title, description,
                town.trim(), meetingAt, capacity, chatEnabled
            )
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { group ->
                    mergeCommunityGroup(group)
                    communityOperationMessage = null
                    onResult(true, "Tauló creat.")
                }.onFailure { error ->
                    val message = error.message ?: "No s'ha pogut crear el grup."
                    communityOperationMessage = message
                    onResult(false, message)
                }
            }
        }
    }

    fun updateCommunityGroup(
        groupId: Int,
        type: CommunityGroupType,
        accessMode: CommunityAccessMode,
        town: String,
        meetingAt: String?,
        title: String,
        description: String,
        capacity: Int?,
        chatEnabled: Boolean = true,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        if (!isCreatorOfCommunityGroup(groupId)) { onResult(false, "Només el creador pot editar el xat."); return }
        if (title.isBlank()) { onResult(false, "Escriu un nom per al xat."); return }
        backgroundExecutor.execute {
            val result = ServerApiRepository.updateCommunityGroup(
                appContext, groupId, type, accessMode, title, description,
                town.trim(), meetingAt, capacity, chatEnabled
            )
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { group ->
                    mergeCommunityGroup(group)
                    communityOperationMessage = null
                    onResult(true, "Tauló actualitzat.")
                }.onFailure { error ->
                    val message = error.message ?: "No s'ha pogut actualitzar el xat."
                    communityOperationMessage = message
                    onResult(false, message)
                }
            }
        }
    }

    var communityBoardInquiries by mutableStateOf<List<com.example.agendavalenciana.model.CommunityBoardInquiry>>(emptyList())
        private set
    var communityInquiriesError by mutableStateOf<String?>(null)
        private set

    fun refreshCommunityBoardInquiries(groupId: Int, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        if (currentAccount == null) return
        backgroundExecutor.execute {
            val result = ServerApiRepository.fetchCommunityBoardInquiries(appContext, groupId)
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { messages ->
                    communityBoardInquiries = messages
                    communityInquiriesError = null
                    onResult(true, "Consultes actualitzades")
                }.onFailure { error ->
                    communityInquiriesError = error.message ?: "No s'han pogut llegir les consultes privades."
                    onResult(false, communityInquiriesError.orEmpty())
                }
            }
        }
    }

    fun sendCommunityBoardInquiry(groupId: Int, peerUserId: Int?, message: String, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        if (currentAccount == null) { onResult(false, "Inicia sessió per contactar amb el creador."); return }
        if (message.isBlank()) { onResult(false, "Escriu un missatge."); return }
        backgroundExecutor.execute {
            val result = ServerApiRepository.sendCommunityBoardInquiry(appContext, groupId, peerUserId, message)
            Handler(Looper.getMainLooper()).post {
                result.onSuccess {
                    refreshCommunityBoardInquiries(groupId)
                    // També apareix en la secció general encara que no siga membre d'un xat.
                    refreshCommunityGroups(mine = true)
                    onResult(true, "Missatge enviat al destinatari.")
                }.onFailure { error -> onResult(false, error.message ?: "No s'ha pogut enviar el missatge.") }
            }
        }
    }

    fun joinCommunityGroup(groupId: Int, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        if (currentAccount == null) { onResult(false, "Inicia sessió per entrar al grup."); return }
        backgroundExecutor.execute {
            val result = ServerApiRepository.joinCommunityGroup(appContext, groupId)
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { group ->
                    mergeCommunityGroup(group)
                    val msg = if (group.myStatus == "ACCEPTED") "Accés aprovat." else "Sol·licitud enviada al creador."
                    onResult(true, msg)
                    if (group.myStatus == "ACCEPTED") refreshCommunityMessages(groupId)
                }.onFailure { error -> onResult(false, error.message ?: "No s'ha pogut entrar al grup.") }
            }
        }
    }

    fun leaveCommunityGroup(groupId: Int, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        backgroundExecutor.execute {
            val result = ServerApiRepository.leaveCommunityGroup(appContext, groupId)
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { group -> mergeCommunityGroup(group); onResult(true, "Has eixit del grup.") }
                    .onFailure { error -> onResult(false, error.message ?: "No s'ha pogut abandonar el grup.") }
            }
        }
    }

    fun refreshCommunityMessages(groupId: Int) {
        if (!isMemberOfCommunityGroup(groupId)) return
        backgroundExecutor.execute {
            val result = ServerApiRepository.fetchCommunityMessages(appContext, groupId)
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { incoming ->
                    communityChatMessages = communityChatMessages.filterNot { it.groupId == groupId } + incoming
                    currentAccount?.serverUserId?.let { owner ->
                        val newest = incoming.maxOfOrNull { it.id } ?: 0
                        store.edit().putInt(eventCommunitySeenKey(owner, groupId), newest).apply()
                        eventCommunityNotifications = eventCommunityNotifications.filterNot {
                            it.communityGroupId == groupId && it.id.startsWith("event_chat_")
                        }
                        invalidateNotificationCache(0L)
                    }
                }
            }
        }
    }

    fun sendCommunityChatMessage(
        groupId: Int,
        text: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        if (text.isBlank() || !isMemberOfCommunityGroup(groupId)) { onResult(false, "No pots escriure en este grup."); return }
        backgroundExecutor.execute {
            val result = ServerApiRepository.sendCommunityMessage(appContext, groupId, text)
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { message ->
                    communityChatMessages = (communityChatMessages.filterNot { it.id == message.id } + message).sortedBy { it.id }
                    communityGroups = communityGroups.map { group ->
                        if (group.id == groupId) group.copy(myMessageCount = group.myMessageCount + 1) else group
                    }
                    onResult(true, "")
                }.onFailure { error -> onResult(false, error.message ?: "No s'ha pogut enviar el missatge.") }
            }
        }
    }

    fun refreshCommunityRequests(groupId: Int) {
        if (!isCreatorOfCommunityGroup(groupId)) return
        backgroundExecutor.execute {
            val result = ServerApiRepository.fetchCommunityRequests(appContext, groupId)
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { incoming -> communityRequestsByGroup = communityRequestsByGroup + (groupId to incoming) }
            }
        }
    }

    fun respondCommunityRequest(groupId: Int, userId: Int, accept: Boolean, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        backgroundExecutor.execute {
            val result = ServerApiRepository.respondCommunityRequest(appContext, groupId, userId, accept)
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { group ->
                    mergeCommunityGroup(group)
                    communityRequestsByGroup = communityRequestsByGroup + (groupId to requestsForCommunityGroup(groupId).filterNot { it.userId == userId })
                    onResult(true, if (accept) "Persona acceptada." else "Sol·licitud rebutjada.")
                }.onFailure { error -> onResult(false, error.message ?: "No s'ha pogut gestionar la sol·licitud.") }
            }
        }
    }

    fun setCommunityGroupClosed(groupId: Int, closed: Boolean, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        backgroundExecutor.execute {
            val result = ServerApiRepository.setCommunityGroupStatus(appContext, groupId, if (closed) "CLOSED" else "OPEN")
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { group -> mergeCommunityGroup(group); onResult(true, if (closed) "Grup tancat." else "Grup obert.") }
                    .onFailure { error -> onResult(false, error.message ?: "No s'ha pogut canviar el grup.") }
            }
        }
    }

    fun sendChatMessage(concertId: Int, text: String): Boolean {
        val account = currentAccount ?: return false
        if (text.isBlank()) return false
        val concert = concerts.firstOrNull { it.id == concertId } ?: return false
        val nextId = (chatMessages.maxOfOrNull { it.id } ?: 0) + 1
        val official = account.role == AccountRole.SUPER_ADMIN || concert.lineup.any(::canManageArtist)
        chatMessages = chatMessages + ChatMessage(
            id = nextId,
            concertId = concertId,
            author = profileFor(account.username).displayName,
            text = text.trim(),
            official = official,
            timestamp = "ara"
        )
        return true
    }

    fun commentsForConcert(concertId: Int): List<ConcertComment> =
        concertComments.filter { it.concertId == concertId }.sortedBy { it.id }

    fun addConcertComment(concertId: Int, text: String): Boolean {
        val account = currentAccount ?: return false
        if (text.isBlank() || concerts.none { it.id == concertId }) return false
        val nextId = (concertComments.maxOfOrNull { it.id } ?: 0) + 1
        concertComments = concertComments + ConcertComment(
            id = nextId,
            concertId = concertId,
            author = profileFor(account.username).displayName,
            text = text.trim(),
            timestamp = "ara"
        )
        return true
    }

    fun deleteConcertComment(commentId: Int): Boolean {
        val account = currentAccount ?: return false
        val comment = concertComments.firstOrNull { it.id == commentId } ?: return false
        val canDelete = account.role == AccountRole.SUPER_ADMIN ||
            comment.author == profileFor(account.username).displayName
        if (!canDelete) return false
        concertComments = concertComments.filterNot { it.id == commentId }
        return true
    }

    fun deleteChatMessage(messageId: Int): Boolean {
        val account = currentAccount ?: return false
        val message = chatMessages.firstOrNull { it.id == messageId } ?: return false
        val canDelete = account.role == AccountRole.SUPER_ADMIN ||
            message.author == profileFor(account.username).displayName
        if (!canDelete) return false
        chatMessages = chatMessages.filterNot { it.id == messageId }
        return true
    }

    private fun isEventFromFollowedEntities(concert: Concert): Boolean {
        // La vista "Seguits" del feed és estrictament d'artistes/grups seguits.
        // Organitzadors, sales i categories continuen sent senyals per a "Per a tu",
        // però no poden fer entrar un esdeveniment en esta vista.
        val followedArtistKeys = followedArtists.asSequence()
            .map(::normalizeSearchValue)
            .filter(String::isNotBlank)
            .toSet()
        if (followedArtistKeys.isEmpty()) return false
        return normalizeSearchValue(concert.artist) in followedArtistKeys ||
            concert.lineup.any { normalizeSearchValue(it) in followedArtistKeys }
    }

    /** v303: només les preferències EXPLÍCITES o els seguiments activen Per a tu.
     * Cerques, guardats, compres, likes i altres senyals passius no l'activen sols.
     * Els favorits legacy són globals al dispositiu; no els usem per adjudicar
     * preferències d'un compte antic a un compte nou.
     */
    private fun hasPersonalizationSeed(): Boolean = com.example.agendavalenciana.model.DefaultFeedPolicy.hasExplicitSignals(
        followedArtists = followedArtists,
        followedOrganizers = followedOrganizers,
        followedVenues = followedVenues,
        followedCategories = followedCategories,
        preferredLocations = userPreferences.preferredLocations,
        preferredComarques = userPreferences.preferredComarques
    )

    /**
     * v254 · El radi d'una ubicació preferida és un límit per a «Per a tu».
     * Una comarca elegida inclou els seus municipis; si no hi ha preferències
     * geogràfiques, no inventem un radi ni excloem les activitats llunyanes.
     */
    /**
     * v262 · «Llocs» fa una diferència quantificable en la quota de proximitat:
     * poble exacte > comarca seleccionada > altres municipis dins del radi.
     * Mai exclou concerts seguits ni comercials, ni retalla la resta del feed.
     */
    private fun preferredGeographyScore(concert: Concert): Int {
        val preferredTowns = userPreferences.preferredLocations
        val preferredRegions = userPreferences.preferredComarques
        if (preferredTowns.isEmpty() && preferredRegions.isEmpty()) return 0
        val eventTown = normalizeSearchValue(concert.town)
        val eventMunicipality = municipalityFor(concert.town)
        val eventComarca = normalizeSearchValue(concert.comarca.takeUnless {
            it.isBlank() || it.equals("Sense comarca", ignoreCase = true)
        } ?: eventMunicipality?.comarca.orEmpty())
        var best = if (eventComarca.isNotBlank() && preferredRegions.any {
                normalizeSearchValue(it) == eventComarca
            }) 700 else 0
        preferredTowns.forEach { town ->
            if (normalizeSearchValue(town) == eventTown) {
                best = maxOf(best, 1_000)
            } else {
                val origin = municipalityFor(town)
                if (origin != null && isConcertInMunicipality(concert, origin)) {
                    best = maxOf(best, 1_000)
                } else if (origin != null) {
                    val km = distanceFromMunicipality(origin, concert)
                    if (km != null && km <= userPreferences.radiusForLocation(town).toDouble()) {
                        best = maxOf(best, (600.0 - km * 4.0).toInt().coerceAtLeast(100))
                    }
                }
            }
        }
        return best
    }

    private fun insidePreferredForYouGeography(concert: Concert): Boolean {
        val locations = userPreferences.preferredLocations
        val comarques = userPreferences.preferredComarques
        if (locations.isEmpty() && comarques.isEmpty()) return true
        val eventComarca = normalizeSearchValue(
            concert.comarca.takeUnless { it.isBlank() || it.equals("Sense comarca", ignoreCase = true) }
                ?: municipalityFor(concert.town)?.comarca.orEmpty()
        )
        if (eventComarca.isNotBlank() && comarques.any { normalizeSearchValue(it) == eventComarca }) return true
        return locations.any { location ->
            val wanted = municipalityFor(location)
            if (wanted != null && isConcertInMunicipality(concert, wanted)) return@any true
            if (normalizeSearchValue(concert.town) == normalizeSearchValue(location)) return@any true
            val distance = wanted?.let { distanceFromMunicipality(it, concert) }
            distance != null && distance <= userPreferences.radiusForLocation(location).toDouble()
        }
    }

    private fun applyAutomaticDefaultFeedMode() {
        val hasExplicitPreferences = hasPersonalizationSeed()
        // Encara que algú haguera seleccionat manualment Per a tu, sense cap gust
        // explícit no s'ha d'executar el selector personalitzat.
        if (!feedModeUsesAutomaticDefault && (hasExplicitPreferences || feedMode != FeedMode.FOR_YOU)) return
        val target = if (hasExplicitPreferences) FeedMode.FOR_YOU else FeedMode.UNFILTERED
        val unchanged = feedMode == target && selectedFeedCategory == null
        if (!unchanged) {
            feedMode = target
            selectedFeedCategory = null
            feedModeSelectionToken += 1L
            scheduleVisibleConcertsRefresh(15L)
        }
        if (target == FeedMode.UNFILTERED) {
            requestUnfilteredFirstFrame()
            ensureFullAgendaLoaded("v333_default_sense_filtre")
        }
    }

    /** Inici: Sense filtre sense gustos; Per a tu quan hi ha preferències/seguiments. */
    fun selectDefaultFeedModeForCurrentUser() {
        // Tornar a Inici o hidratar el compte no pot posar «Per a tu» sobre
        // una cerca/filtre encara actiu; la seua base continua sent l'agenda completa.
        if (search.isNotBlank() || !filters.isEmpty()) {
            synchronizeCriteriaFeedMode()
            return
        }
        feedModeScopedByCriteria = false
        feedModeUsesAutomaticDefault = true
        applyAutomaticDefaultFeedMode()
    }

    /**
     * Cerca i Opcions consulten el catàleg complet amb criteris actius.
     * En llevar l'últim criteri, v293 torna al feed «Per a tu».
     * «Sense filtre» només es manté si l'usuari el tria manualment.
     */
    private fun synchronizeCriteriaFeedMode() {
        if (search.isNotBlank() || !filters.isEmpty()) {
            feedModeScopedByCriteria = true
            feedModeUsesAutomaticDefault = false
            if (feedMode != FeedMode.UNFILTERED || selectedFeedCategory != null) {
                feedMode = FeedMode.UNFILTERED
                selectedFeedCategory = null
                feedModeSelectionToken += 1L
            }
        } else if (feedModeScopedByCriteria) {
            // En netejar la cerca, recuperem el mode automàtic corresponent
            // als seguiments/preferències actuals, sense personalitzar un perfil buit.
            feedModeScopedByCriteria = false
            feedModeUsesAutomaticDefault = true
            applyAutomaticDefaultFeedMode()
            if (feedMode == FeedMode.FOR_YOU && canReuseResidentForYou()) {
                visibleConcertsState = safeVisibleFeed(residentForYouConcerts)
                feedRenderRevision += 1L
            }
        }
    }

    private fun canReuseResidentForYou(): Boolean =
        residentForYouReady &&
            residentForYouDataRevision == feedDataRevision &&
            residentForYouDay == agendaToday().toString() &&
            residentForYouPreferenceSignature == forYouPreferenceSignature() &&
            (System.currentTimeMillis() - loadForYouSnapshotTimestamp()) in 0 until forYouFastPreviewMaxAge

    private fun rememberResidentForYou(items: List<Concert>) {
        residentForYouConcerts = safeVisibleFeed(items)
        residentForYouPreferenceSignature = forYouPreferenceSignature()
        residentForYouFollowedSignature = followedArtists.hashCode()
        residentForYouDay = agendaToday().toString()
        residentForYouDataRevision = feedDataRevision
        residentForYouReady = true
    }

    fun selectFeedMode(mode: FeedMode) {
        // Una petició de Per a tu sense gustos equival a Sense filtre, i es manté
        // automàtica perquè el primer follow passe a personalitzat sense un segon toc.
        val hasExplicitPreferences = hasPersonalizationSeed()
        val effectiveMode = if (mode == FeedMode.FOR_YOU && !hasExplicitPreferences) FeedMode.UNFILTERED else mode
        val hadCriteria = search.isNotBlank() || !filters.isEmpty()
        val wouldClearCategory = effectiveMode != FeedMode.CATEGORY && selectedFeedCategory != null
        if (feedMode == effectiveMode && !wouldClearCategory && !hadCriteria) return
        feedModeScopedByCriteria = false
        feedModeUsesAutomaticDefault = effectiveMode == FeedMode.UNFILTERED && !hasExplicitPreferences
        search = ""
        selectedFeedSearchArtist = null
        selectedFeedSearchType = null
        filters = Filters()
        activeFilterPresetName = null
        feedMode = effectiveMode
        // Tornar de Sense filtre a Per a tu no ha d'esperar cap reconstrucció: pintem
        // immediatament l'última llista Per a tu materialitzada i la validem en background.
        if (effectiveMode == FeedMode.FOR_YOU && canReuseResidentForYou()) {
            visibleConcertsState = safeVisibleFeed(residentForYouConcerts)
            feedRenderRevision += 1L
        }
        if (effectiveMode != FeedMode.CATEGORY) selectedFeedCategory = null
        feedModeSelectionToken += 1L
        feedCriteriaChanged()
        if (effectiveMode == FeedMode.UNFILTERED) requestUnfilteredFirstFrame()
        if (effectiveMode != FeedMode.FOR_YOU) ensureFullAgendaLoaded("feed_mode_${effectiveMode.name.lowercase()}")
        // v331: tornar a Sense filtre recupera les targetes ja preparades;
        // el fil de feed continua validant-les sense buidar la pantalla.
        if (effectiveMode == FeedMode.UNFILTERED &&
            residentUnfilteredKey == unfilteredSelectionKey() && residentUnfilteredItems.isNotEmpty()) {
            visibleConcertsState = residentUnfilteredItems
            preparedFeedCriteriaRevision = feedCriteriaRevision
            feedRenderRevision += 1L
        }
    }

    fun selectFeedCategory(category: CulturalCategory) {
        feedModeScopedByCriteria = false
        feedModeUsesAutomaticDefault = false
        selectedFeedCategory = category
        feedMode = FeedMode.CATEGORY
        ensureFullAgendaLoaded("category")
        // Categoria és una vista exclusiva: només compta la categoria elegida.
        search = ""
        selectedFeedSearchArtist = null
        selectedFeedSearchType = null
        filters = Filters()
        activeFilterPresetName = null
        feedModeSelectionToken += 1L
        feedCriteriaChanged()
    }

    fun selectConcertViewMode(mode: ConcertViewMode) {
        // Llista i Mapa s'han eliminat. Conservem el mètode per compatibilitat
        // amb codi antic, però el feed sempre queda en mode Visual.
        concertViewMode = ConcertViewMode.VISUAL
        store.edit().putString("concert_view_mode", ConcertViewMode.VISUAL.name).apply()
    }

    private fun eventIdentity(concert: Concert): AgendaDedupe.EventIdentity =
        AgendaDedupe.EventIdentity(
            title = concert.title,
            artists = concert.lineup.ifEmpty { listOf(concert.artist) },
            date = concert.date,
            time = concert.time,
            town = MunicipalityRepository.canonicalTown(concert.town),
            venue = concert.venue,
            festivalOrMultiday = concert.isFestivalExperience ||
                (concert.endDate.isNotBlank() && concert.endDate != concert.date)
        )

    /** Bloqueja solament els duplicats fiables; els pareguts queden per a auditoria. */
    private fun findExistingEventDuplicate(
        title: String, artists: List<String>, date: String, time: String,
        town: String, venue: String, excludeId: Int? = null,
        isMultiday: Boolean = false
    ): Concert? {
        val candidate = AgendaDedupe.EventIdentity(
            title, artists, date, time,
            MunicipalityRepository.canonicalTown(town), venue, isMultiday
        )
        return concerts.firstOrNull { existing ->
            existing.id != excludeId && AgendaDedupe.sameEvent(eventIdentity(existing), candidate)
        }
    }

    private fun mergeDuplicateConcerts(first: Concert, second: Concert): Concert {
        val canonical = when {
            first.id > 0 && second.id > 0 -> if (first.id <= second.id) first else second
            first.id > 0 -> first
            second.id > 0 -> second
            else -> first
        }
        val other = if (canonical === first) second else first
        return canonical.copy(
            lineup = (canonical.lineup + other.lineup).map(String::trim).filter(String::isNotBlank).distinctBy(AgendaDedupe::artistKey),
            posterUri = canonical.posterUri ?: other.posterUri,
            artistImageUri = canonical.artistImageUri ?: other.artistImageUri,
            venueImageUri = canonical.venueImageUri ?: other.venueImageUri,
            townImageUri = canonical.townImageUri ?: other.townImageUri,
            categoryImageUri = canonical.categoryImageUri ?: other.categoryImageUri,
            organizerImageUri = canonical.organizerImageUri ?: other.organizerImageUri,
            organizers = (canonical.organizers + other.organizers).map(String::trim).filter(String::isNotBlank).distinctBy(::normalizeSearchValue),
            eventCardImageUri = canonical.eventCardImageUri ?: other.eventCardImageUri,
            preferredImageUri = canonical.preferredImageUri ?: other.preferredImageUri,
            venue = canonical.venue.ifBlank { other.venue },
            styles = (canonical.styles + other.styles).map(String::trim).filter(String::isNotBlank).distinctBy(::normalizeSearchValue),
            price = when { canonical.price > 0 -> canonical.price; other.price > 0 -> other.price; else -> canonical.price },
            language = canonical.language.ifBlank { other.language },
            description = canonical.description.ifBlank { other.description },
            // Els camps d'entrades pertanyen a l'ID canònic. Barrejar flags
            // de dos registres diferents podia crear reserves gratuïtes fictícies.
            ticketPrice = canonical.ticketPrice,
            ticketAvailable = canonical.ticketAvailable,
            ticketUrl = canonical.ticketUrl,
            savedCount = maxOf(canonical.savedCount, other.savedCount),
            likeCount = maxOf(canonical.likeCount, other.likeCount),
            languages = (canonical.languages + other.languages).map(String::trim).filter(String::isNotBlank).distinctBy(::normalizeSearchValue),
            scheduleDates = (canonical.scheduleDates + other.scheduleDates + listOfNotNull(canonical.feedOccurrenceDate, other.feedOccurrenceDate, canonical.selectedDate, other.selectedDate))
                .map(String::trim).filter(String::isNotBlank).map { it.take(10) }.distinct().sorted(),
            isMultidayFromServer = canonical.isMultidayFromServer || other.isMultidayFromServer,
            feedOccurrenceDate = null,
            feedInstanceUid = null,
            selectedDate = null
        )
    }

    @Synchronized
    private fun deduplicatedConcertsForFeed(): List<Concert> {
        if (cachedDedupRevision == feedDataRevision) return cachedDedupConcerts
        val result = mutableListOf<Concert>()
        // Agrupem per dia i municipi, no pel títol (que pot variar entre fonts).
        val candidatesByPlaceDate = mutableMapOf<String, MutableList<Int>>()
        concerts.forEach { concert ->
            val base = listOf(
                concert.date.take(10),
                AgendaDedupe.nameKey(MunicipalityRepository.canonicalTown(concert.town))
            ).joinToString("|")
            val indices = candidatesByPlaceDate.getOrPut(base) { mutableListOf() }
            val matchIndex = indices.firstOrNull { index ->
                AgendaDedupe.sameEvent(eventIdentity(result[index]), eventIdentity(concert))
            }
            if (matchIndex == null) {
                indices += result.size
                result += concert
            } else {
                result[matchIndex] = mergeDuplicateConcerts(result[matchIndex], concert)
            }
        }
        cachedDedupConcerts = result
        cachedDedupRevision = feedDataRevision
        return result
    }

    private fun feedSearchIndex(): FeedSearchIndex {
        cachedFeedSearchIndex?.takeIf { it.revision == feedDataRevision }?.let { return it }
        synchronized(feedSearchIndexLock) {
            cachedFeedSearchIndex?.takeIf { it.revision == feedDataRevision }?.let { return it }

            val deduplicated = deduplicatedConcertsForFeed()
            val entries = HashMap<Int, ConcertSearchEntry>(deduplicated.size * 2)
            val titleKeys = HashSet<String>()
            val venueKeys = HashSet<String>()
            val organizerKeys = HashSet<String>()
            val indexedArtistKeys = HashSet<String>()

            deduplicated.forEach { concert ->
                val titleKey = normalizeSearchValue(concert.title)
                val artistKeys = buildSet {
                    normalizeSearchValue(concert.artist).takeIf(String::isNotBlank)?.let(::add)
                    concert.lineup.forEach { normalizeSearchValue(it).takeIf(String::isNotBlank)?.let(::add) }
                }
                indexedArtistKeys.addAll(artistKeys)
                val venueKey = normalizeSearchValue(concert.venue)
                val eventOrganizerKeys = buildSet {
                    concert.organizers.forEach { normalizeSearchValue(it).takeIf(String::isNotBlank)?.let(::add) }
                    concert.coEditorUsernames.forEach { username ->
                        normalizeSearchValue(username).takeIf(String::isNotBlank)?.let(::add)
                        normalizeSearchValue(profileFor(username).displayName).takeIf(String::isNotBlank)?.let(::add)
                    }
                }
                val comarcaKey = normalizeSearchValue(concert.comarca)
                val blob = buildList {
                    add(concert.title); add(concert.subtitle); add(concert.description)
                    add(concert.artist); addAll(concert.lineup)
                    add(concert.town); add(concert.comarca); add(concert.venue)
                    addAll(concert.styles); add(concert.language); addAll(concert.languages)
                    addAll(concert.organizers); addAll(concert.coEditorUsernames)
                }.joinToString(" ").let(::normalizeSearchValue)

                entries[concert.id] = ConcertSearchEntry(
                    titleKey = titleKey,
                    artistKeys = artistKeys,
                    venueKey = venueKey,
                    organizerKeys = eventOrganizerKeys,
                    comarcaKey = comarcaKey,
                    blob = blob
                )
                if (titleKey.isNotBlank()) titleKeys += titleKey
                if (venueKey.isNotBlank()) venueKeys += venueKey
                organizerKeys += eventOrganizerKeys
            }

            ServerApiRepository.peekCachedEntityNames("VENUE")
                .mapTo(venueKeys, ::normalizeSearchValue)
            sequenceOf("FESTIVAL", "ORGANIZER").forEach { type ->
                ServerApiRepository.peekCachedEntityNames(type)
                    .mapTo(organizerKeys, ::normalizeSearchValue)
            }

            val index = FeedSearchIndex(
                revision = feedDataRevision,
                entriesById = entries,
                artistKeys = indexedArtistKeys + knownArtistNames().asSequence().map(::normalizeSearchValue).filter(String::isNotBlank).toSet(),
                eventTitleKeys = titleKeys,
                venueKeys = venueKeys.filterTo(HashSet(), String::isNotBlank),
                organizerKeys = organizerKeys.filterTo(HashSet(), String::isNotBlank)
            )
            cachedFeedSearchIndex = index
            return index
        }
    }

    /** Cap estat de feed, ni tan sols una cache vella, pot mostrar dates passades. */
    private fun safeVisibleFeed(items: List<Concert>): List<Concert> =
        FestivalFeedNextDay.cleanVisible(items.filter(::isEventVisibleForUser), agendaToday())

    private fun visibleConcertsCacheKey(): String = buildString {
        append(feedDataRevision).append('|').append(search).append('|').append(selectedFeedSearchArtist.orEmpty())
        append('|').append(selectedFeedSearchType?.name.orEmpty()).append('|').append(filters.hashCode())
        append('|').append(feedMode.name).append('|').append(selectedFeedCategory?.name ?: "")
        // Likes, guardats, seguiments i altres afinitats canvien sovint. No invalidem
        // el rànquing de Per a tu enmig de la interacció: la UI reflecteix l'estat
        // immediatament i el nou rànquing s'incorpora en el pròxim refresc de dades.
        append('|').append(blockedArtists.hashCode()).append('|').append(userPreferences.hashCode())
        append('|').append(hiddenConcertIds.hashCode()).append('|').append(blockedCategories.hashCode())
        append('|').append(blockedSubcategories.hashCode())
        append('|').append(currentAccount?.serverUserId ?: 0)
        append('|').append(hasPersonalizationSeed())
        append('|').append(followedArtists.hashCode()).append('|').append(recentSearchTerms.hashCode())
        append('|').append(agendaToday())
        // No reutilitzar una resposta de generació anterior després d'un refresc explícit.
        append('|').append(forYouRecalculationGeneration)
    }

    /**
     * Mostra immediatament l'última agenda personal estable mentre el càlcul nou es fa
     * fora del fil principal. L'ordre antic només s'usa com a pont visual; les altes,
     * baixes i modificacions reals provenen sempre de [chronological].
     */
    private fun fastStableForYou(
        chronological: List<Concert>,
        snapshot: List<Int> = loadForYouSnapshot()
    ): List<Concert> {
        if (snapshot.isEmpty()) return emptyList()
        val byId = chronological.associateBy { it.id }
        // El snapshot conté el conjunt filtrat, no només un prefix ordenat. Per tant,
        // no reincorporem ací la resta de l'agenda.
        return snapshot.mapNotNull(byId::get)
            .distinctBy { it.id }
            .sortedWith(compareBy<Concert> { it.date }.thenBy { it.time }.thenBy { it.id })
    }

    private fun eventStartDate(concert: Concert): LocalDate? =
        runCatching { LocalDate.parse(concert.date.take(10)) }.getOrNull()

    private fun eventEndDate(concert: Concert): LocalDate? =
        concert.endDate.takeIf(String::isNotBlank)?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
            ?: eventStartDate(concert)

    private fun eventLastRelevantDate(concert: Concert): LocalDate? {
        val scheduledLast = concert.scheduleDates.asSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .mapNotNull { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
            .maxOrNull()
        return listOfNotNull(eventEndDate(concert), eventStartDate(concert), scheduledLast).maxOrNull()
    }

    private fun isCurrentOrFutureAgendaEvent(concert: Concert, day: LocalDate = agendaToday()): Boolean =
        eventLastRelevantDate(concert)?.let { !it.isBefore(day) } ?: true

    /**
     * «Per a tu» treballa només amb la finestra mòbil dels pròxims dos mesos.
     * La resta de l'agenda continua disponible íntegrament en Sense filtre i al cercador.
     * Un esdeveniment llarg entra si solapa la finestra, encara que començara abans de hui.
     */
    private fun isInsideForYouWindow(concert: Concert, day: LocalDate = agendaToday()): Boolean {
        val start = eventStartDate(concert) ?: return false
        val end = eventLastRelevantDate(concert) ?: start
        val limit = day.plusMonths(FOR_YOU_WINDOW_MONTHS)
        return !end.isBefore(day) && !start.isAfter(limit)
    }

    private fun isLongContinuousActivity(concert: Concert): Boolean {
        // Les exposicions són l’excepció explícita de la regla general de festivals.
        // Encara que duren molts dies, es tracten com activitat contínua per no saturar el feed
        // i mai s’obrin automàticament amb el popup de festival només per duració.
        if (concert.category != CulturalCategory.EXHIBITIONS) return false
        val start = eventStartDate(concert) ?: return false
        val end = eventEndDate(concert) ?: return false
        return !end.isBefore(start) && ChronoUnit.DAYS.between(start, end) + 1L > 5L
    }

    private fun maxDate(a: LocalDate, b: LocalDate): LocalDate = if (a.isAfter(b)) a else b
    private fun minDate(a: LocalDate, b: LocalDate): LocalDate = if (a.isBefore(b)) a else b

    /**
     * Genera aparicions del feed sense convertir cada dia en un esdeveniment nou.
     * Exposicions contínues de més de 5 dies no saturen el feed:
     * - 6-7 dies: només la pròxima jornada disponible.
     * - més de 7 dies: una aparició cada 7 dies.
     * Quan hi ha una franja temporal explícita, l'activitat continua sent trobable
     * sempre que solape la franja, però es mostra una sola vegada.
     */
    private fun expandMultiDayOccurrences(source: List<Concert>): List<Concert> = source.flatMap { concert ->
        val today = agendaToday()
        val explicitDates = concert.scheduleDates.asSequence()
            .map(String::trim).filter(String::isNotBlank).mapNotNull { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
            .distinct().sorted().toList()
        val start = eventStartDate(concert)
        val end = eventEndDate(concert)

        if (FestivalFeedNextDay.isFeedProgramme(concert)) {
            // Les aparicions completes i les del primer fotograma seguixen la mateixa
            // data efectiva. El rang inici-fi no crea dies ficticis de programació.
            FestivalFeedNextDay.upcoming(concert, today)
        } else if (isLongContinuousActivity(concert) && start != null && end != null) {
            val filterFrom = filters.dateFrom?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
            val filterTo = filters.dateTo?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
            val monthFilter = filters.month?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
            val rangeSearch = filterFrom != null || filterTo != null || monthFilter != null
            val occurrence = if (rangeSearch) {
                val visibleFrom = maxDate(start, maxDate(today, filterFrom ?: monthFilter?.atDay(1) ?: today))
                val visibleTo = minDate(end, filterTo ?: monthFilter?.atEndOfMonth() ?: end)
                if (visibleFrom.isAfter(visibleTo)) null else visibleFrom
            } else {
                val totalDays = ChronoUnit.DAYS.between(start, end) + 1L
                if (totalDays > 7L) {
                    if (today.isBefore(start) || today == start) start
                    else {
                        val elapsed = ChronoUnit.DAYS.between(start, today)
                        val offset = ((elapsed + 6L) / 7L) * 7L
                        start.plusDays(offset).takeUnless { it.isAfter(end) }
                    }
                } else {
                    maxDate(start, today).takeUnless { it.isAfter(end) }
                }
            }
            if (occurrence == null) emptyList() else {
                val value = occurrence.toString()
                listOf(concert.copy(
                    feedOccurrenceDate = value,
                    selectedDate = value,
                    feedInstanceUid = "event_${concert.id}_$value",
                    isMultidayFromServer = true
                ))
            }
        } else {
            // Només materialitzem jornades quan el servidor/feeder ens les dona de forma
            // explícita. Un simple rang inici-fi (obra, residència, cicle, etc.) no significa
            // activitat cada dia i abans podia multiplicar una sola fitxa en desenes de cards.
            val dates: List<LocalDate> = explicitDates
            if (dates.isEmpty()) {
                listOf(concert.copy(feedOccurrenceDate = null, selectedDate = null, feedInstanceUid = null))
            } else {
                dates.filterNot { it.isBefore(today) }.map { day ->
                    val value = day.toString()
                    val daySummary = concert.scheduleDaySummaries[value].orEmpty().trim()
                    val dayArtists = daySummary
                        .split('·')
                        .map(String::trim)
                        .filter(String::isNotBlank)
                        .distinctBy { it.lowercase() }
                    val dayStartTime = concert.scheduleDayStartTimes[value].orEmpty().trim()
                    val dayTicketPrice = concert.scheduleDayTicketPrices[value]
                    val occurrenceLineup = when {
                        dayArtists.isNotEmpty() -> dayArtists
                        concert.isFestivalExperience && dates.size > 1 -> emptyList()
                        else -> concert.lineup
                    }
                    concert.copy(
                        lineup = occurrenceLineup,
                        subtitle = daySummary.ifBlank { if (concert.isFestivalExperience && dates.size > 1) "" else concert.subtitle },
                        time = dayStartTime.ifBlank { concert.time },
                        ticketPrice = dayTicketPrice ?: concert.ticketPrice,
                        feedOccurrenceDate = value,
                        selectedDate = value,
                        feedInstanceUid = "event_${concert.id}_$value",
                        scheduleDates = dates.map(LocalDate::toString),
                        isMultidayFromServer = concert.isMultidayFromServer || dates.size > 1
                    )
                }
            }
        }
    }

    /**
     * Si falten setmanes, evita que totes les jornades d'un mateix festival ocupen
     * posicions consecutives en Per a tu. Quan la jornada és pròxima, preval la cronologia.
     */
    private fun spreadFarFutureFestivalOccurrences(source: List<Concert>): List<Concert> {
        if (source.size < 3) return source
        val today = agendaToday()
        val nearLimit = today.plusDays(7)
        val out = source.toMutableList()
        var i = 1
        while (i < out.size) {
            val current = out[i]
            val day = runCatching { LocalDate.parse(current.feedDate.take(10)) }.getOrNull()
            if (out[i - 1].id == current.id && day != null && day.isAfter(nearLimit)) {
                val swap = (i + 1 until minOf(out.size, i + 7)).firstOrNull { out[it].id != current.id }
                if (swap != null) { val moved = out.removeAt(swap); out.add(i, moved) }
            }
            i++
        }
        return out
    }

    private fun isConcertInMunicipality(concert: Concert, municipality: Municipality): Boolean {
        val eventMunicipality = municipalityFor(concert.town)
        if (eventMunicipality?.code == municipality.code) return true
        val eventTown = normalizeSearchValue(concert.town)
        val canonical = normalizeSearchValue(municipality.name)
        val official = normalizeSearchValue(municipality.officialName)
        return eventTown == canonical || (official.isNotBlank() && eventTown == official)
    }

    private fun distanceFromMunicipality(origin: Municipality, concert: Concert): Double? {
        val destination = municipalityFor(concert.town) ?: return null
        if (origin.latitude == 0.0 && origin.longitude == 0.0) return null
        if (destination.latitude == 0.0 && destination.longitude == 0.0) return null
        return GeoDistance.kilometres(
            origin.latitude, origin.longitude,
            destination.latitude, destination.longitude
        )
    }

    /**
     * La cerca escrita o la selecció d'un poble és estricta sobre tota l'agenda.
     * La proximitat només pertany a la personalització/descoberta fora de Cerca;
     * NO es busquen pobles veïns ni la comarca quan el poble no té esdeveniments.
     * Els filtres d'Opcions NO limiten les cerques explícites.
     */
    private fun exactMunicipalitySearch(source: List<Concert>, municipality: Municipality): List<Concert> =
        source.filter { isConcertInMunicipality(it, municipality) }
            .sortedWith(compareBy<Concert> { it.date }.thenBy { it.time })

    private fun filterOccurrenceMonth(source: List<Concert>): List<Concert> {
        // Després de materialitzar les jornades d'un festival, les aparicions
        // fora de les dates demanades no poden quedar visibles. Els esdeveniments
        // sense jornada expandida ja s'han filtrat per solapament de dates abans.
        val from = filters.dateFrom?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
        val to = filters.dateTo?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
        val month = filters.month
        if (from == null && to == null && month == null) return source
        return source.filter { event ->
            val occurrence = event.feedOccurrenceDate ?: return@filter true
            val day = runCatching { LocalDate.parse(occurrence.take(10)) }.getOrNull() ?: return@filter true
            (from == null || !day.isBefore(from)) &&
                (to == null || !day.isAfter(to)) &&
                (month == null || occurrence.startsWith(month))
        }
    }

    /** Ordre estrictament cronològic en TOTES les vistes, sense algorismes antics de barreja. */
    private fun orderVisibleAgendaDays(items: List<Concert>): List<Concert> {
        val dateFirst = compareBy<Concert> { it.feedDate }
        if (feedMode != FeedMode.FOR_YOU || !userPreferences.valencianFirst) {
            return items.sortedWith(dateFirst.thenBy { it.time }.thenBy { it.id })
        }
        // La preferència de valencià desempata actes del mateix dia. Mai amaga
        // artistes Seguits ni canvia el criteri d'inclusió de les tres vies.
        val valenciaKey = canonicalLanguageKey("Valencià")
        return items.sortedWith(dateFirst
            .thenByDescending { concertLanguageKeys(it).contains(valenciaKey) }
            .thenBy { it.time }.thenBy { it.id })
    }

    /**
     * «Seguits» només es recalcula amb canvis reals: concerts del catàleg,
     * altes/baixes de seguiment, exclusions i canvi de dia.
     * Cap afinitat, grau de descoberta ni quota comercial intervé ací.
     */
    private fun followedEventsChronological(): List<Concert> {
        val key = listOf(
            feedDataRevision.toString(),
            fullAgendaLoadedForProcess.toString(),
            followedArtists.hashCode().toString(),
            hiddenConcertIds.hashCode().toString(),
            blockedArtists.hashCode().toString(),
            blockedCategories.hashCode().toString(),
            blockedSubcategories.hashCode().toString(),
            agendaToday().toString()
        ).joinToString("|")
        if (followedFeedCacheKey == key) return followedFeedCache

        val followedKeys = followedArtists.asSequence()
            .map(::normalizeSearchValue).filter(String::isNotBlank).toSet()
        val result = if (followedKeys.isEmpty()) emptyList() else {
            val base = deduplicatedConcertsForFeed().asSequence()
                .filter(::isCurrentOrFutureAgendaEvent)
                .filter(::isEventVisibleForUser)
                .filter { event ->
                    normalizeSearchValue(event.artist) in followedKeys ||
                        event.lineup.any { normalizeSearchValue(it) in followedKeys }
                }.toList()
            safeVisibleFeed(expandMultiDayOccurrences(base)
                .sortedWith(compareBy<Concert> { it.feedDate }.thenBy { it.time }.thenBy { it.id }))
        }
        followedFeedCacheKey = key
        followedFeedCache = result
        return result
    }

    /**
     * v324 · Part fixa de «Per a tu»: només comprovem artistes seguits en canviar
     * revisió, preferència de seguiment o exclusions. Una actualització de likes,
     * radi, suggeriments o cerca no necessita reconstruir esta part del resultat.
     * Els nous objectes Concert de MariaDB substituïxen els antics per ID.
     */
    private fun materializedFollowedForYou(source: List<Concert>): List<Concert> {
        val key = buildString {
            append(feedDataRevision).append('|')
            append(followedArtists.hashCode()).append('|')
            append(hiddenConcertIds.hashCode()).append('|')
            append(blockedArtists.hashCode()).append('|')
            append(blockedCategories.hashCode()).append('|')
            append(blockedSubcategories.hashCode()).append('|')
            append(agendaToday())
        }
        if (forYouFollowedCacheKey == key) return forYouFollowedCache
        val keys = followedArtists.asSequence().map(::normalizeSearchValue)
            .filter(String::isNotBlank).toSet()
        val result = if (keys.isEmpty()) emptyList() else source.filter { event ->
            !isFreeInternalAgendaReservation(event) &&
                (normalizeSearchValue(event.artist) in keys ||
                    event.lineup.any { normalizeSearchValue(it) in keys })
        }
        forYouFollowedCacheKey = key
        forYouFollowedCache = result
        return result
    }

    /**
     * v274: «Sense filtre» és només la font. Ni la memòria de lectura ni una resposta
     * provisional del feed poden passar com a resultats d'una cerca activa.
     * També funciona sense haver premut cap suggeriment.
     */
    private fun applyStrictFeedSearch(
        source: List<Concert>,
        query: String,
        index: FeedSearchIndex?,
        municipality: Municipality?
    ): List<Concert> {
        if (query.isBlank()) return source
        fun entry(event: Concert) = index?.entriesById?.get(event.id)
        val explicitType = selectedFeedSearchType
        if (explicitType != null) {
            return when (explicitType) {
                FeedSearchSuggestionType.ARTIST -> source.filter { query in entry(it)?.artistKeys.orEmpty() }
                FeedSearchSuggestionType.MUNICIPALITY -> if (municipality != null) {
                    exactMunicipalitySearch(source, municipality)
                } else source.filter { normalizeSearchValue(it.town) == query }
                FeedSearchSuggestionType.COMARCA -> source.filter {
                    entry(it)?.comarcaKey == query ||
                        normalizeSearchValue(municipalityFor(it.town)?.comarca.orEmpty()) == query
                }
                FeedSearchSuggestionType.COMPANY -> source.filter {
                    query in entry(it)?.organizerKeys.orEmpty() || entry(it)?.titleKey == query
                }
                FeedSearchSuggestionType.VENUE -> source.filter { entry(it)?.venueKey == query }
                FeedSearchSuggestionType.STYLE -> source.filter { event ->
                    event.styles.any { normalizeSearchValue(it) == query }
                }
            }
        }
        // v284: el text lliure busca en TOTS els camps de TOTS els esdeveniments,
        // encara que el text coincidisca amb un municipi, artista o sala del catàleg.
        // Per exemple, «Barcelona» pot trobar activitats de Barcelona i activitats
        // que mencionen «Barcelona» al títol. Només triar el suggeriment MUNICIPALITY
        // limita el resultat a eixe municipi, mai a pobles del voltant.
        // Els àlies municipals i comarcals també s'accepten sense eixamplar el radi.
        val literalMatches = source.filter { entry(it)?.blob?.contains(query) == true }
        val aliasedMunicipalMatches = municipality?.let { exactMunicipalitySearch(source, it) }.orEmpty()
        val comarcaMatches = if (municipalities.any { normalizeSearchValue(it.comarca) == query }) {
            source.filter { event ->
                entry(event)?.comarcaKey == query ||
                    normalizeSearchValue(municipalityFor(event.town)?.comarca.orEmpty()) == query
            }
        } else emptyList()
        return (literalMatches + aliasedMunicipalMatches + comarcaMatches).distinctBy { it.id }

    }

    /** Últim control del cercador: mai renderitzar tot Sense filtre amb una consulta activa. */
    fun strictSearchResultsForDisplay(source: List<Concert>): List<Concert> {
        val query = normalizeSearchValue(search)
        if (query.isBlank()) return source
        return applyStrictFeedSearch(source, query, feedSearchIndex(), municipalityFor(search))
    }

    // v333 · Primer fotograma de Sense filtre des de SQLite (persistix en disc).
    // No es dona per catàleg complet: l'hidratació posterior inclou esdeveniments
    // antics encara en curs, dates sense indexar i totes les jornades dels festivals.
    @Volatile private var unfilteredPreviewDay = ""
    @Volatile private var unfilteredPreviewRevision = Long.MIN_VALUE
    @Volatile private var unfilteredPreviewItems: List<Concert> = emptyList()
    @Volatile private var unfilteredPreviewInFlight = false

    /** La llista universal no depén de seguiments, likes, cerques ni afinitats. */
    private fun unfilteredSelectionKey(): String = buildString {
        append(feedDataRevision).append('|').append(agendaToday()).append('|')
        append(currentAccount?.serverUserId ?: 0).append('|')
        append(blockedArtists.hashCode()).append('|').append(hiddenConcertIds.hashCode()).append('|')
        append(blockedCategories.hashCode()).append('|').append(blockedSubcategories.hashCode())
    }

    private fun requestUnfilteredFirstFrame() {
        if (fullAgendaLoadedForProcess || unfilteredPreviewInFlight ||
            feedMode != FeedMode.UNFILTERED || search.isNotBlank() || !filters.isEmpty()) return
        val requestedDay = agendaToday().toString()
        val requestedAccount = currentAccount?.username?.lowercase() ?: "guest"
        val requestedCriteria = feedCriteriaRevision
        val requestedRevision = feedDataRevision
        if (unfilteredPreviewItems.isNotEmpty() && unfilteredPreviewDay == requestedDay &&
            unfilteredPreviewRevision == requestedRevision) {
            if (preparedFeedCriteriaRevision != requestedCriteria) {
                visibleConcertsState = safeVisibleFeed(unfilteredPreviewItems)
                preparedFeedCriteriaRevision = requestedCriteria
                feedRenderRevision += 1L
            }
            return
        }
        unfilteredPreviewInFlight = true
        unfilteredPreviewExecutor.execute {
            val started = SystemClock.elapsedRealtime()
            // No obrir la mateixa finestra de dos mesos ni el catàleg complet.
            // El preview de 48 registres és transitori, no un topall d'esdeveniments.
            val first = if (ServerApiRepository.isConfigured()) {
                ServerApiRepository.loadCachedUnfilteredPreview(appContext, requestedDay, 48)
            } else emptyList()
            Log.d("AgendaPerf", "unfilteredSqlitePreview ${SystemClock.elapsedRealtime() - started}ms · ${first.size} candidates")
            Handler(Looper.getMainLooper()).post {
                unfilteredPreviewInFlight = false
                if (first.isEmpty() || fullAgendaLoadedForProcess ||
                    (currentAccount?.username?.lowercase() ?: "guest") != requestedAccount ||
                    requestedDay != agendaToday().toString() ||
                    feedMode != FeedMode.UNFILTERED || search.isNotBlank() || !filters.isEmpty() ||
                    requestedCriteria != feedCriteriaRevision) return@post
                // La personalització ja està hidratada: no fer aparéixer cap bloquejat.
                if (!forYouSignalsReadyForProcess) return@post
                val preview = safeVisibleFeed(
                    orderVisibleAgendaDays(expandMultiDayOccurrences(first.filter {
                        isCurrentOrFutureAgendaEvent(it) && isEventVisibleForUser(it)
                    }).sortedWith(compareBy<Concert> { it.feedDate }.thenBy { it.time }.thenBy { it.id }))
                )
                if (preview.isEmpty()) return@post
                unfilteredPreviewItems = preview
                unfilteredPreviewDay = requestedDay
                unfilteredPreviewRevision = feedDataRevision
                // Mai reemplaçar un resultat més complet i actual amb el preview.
                if (preparedFeedCriteriaRevision != requestedCriteria ||
                    visibleConcertsState.isEmpty() || visibleConcertsState.size < preview.size) {
                    visibleConcertsState = preview
                    preparedFeedCriteriaRevision = requestedCriteria
                    feedRenderRevision += 1L
                }
                Log.d("AgendaPerf", "unfilteredFirstPaint ${SystemClock.elapsedRealtime() - started}ms · ${preview.size} cards")
            }
        }
    }

    // v331: «Sense filtre» compartix el MATEIX catàleg públic ordenat entre
    // comptes. Es construïx una vegada per revisió de dades i dia civil.
    // Les exclusions personals s'apliquen DESPRÉS, mai a la llista comuna.
    @Volatile private var sharedUnfilteredRevision = Long.MIN_VALUE
    @Volatile private var sharedUnfilteredDay = ""
    @Volatile private var sharedUnfilteredCatalogue: List<Concert> = emptyList()
    // Resultat ja expandit amb exclusions del compte. Permet tornar a esta
    // pestanya sense recalcular cap jornada després d'haver visitat Per a tu.
    @Volatile private var residentUnfilteredKey = ""
    @Volatile private var residentUnfilteredItems: List<Concert> = emptyList()

    @Synchronized
    private fun sharedUnfilteredBase(day: LocalDate): List<Concert> {
        val todayKey = day.toString()
        if (sharedUnfilteredRevision == feedDataRevision && sharedUnfilteredDay == todayKey) {
            return sharedUnfilteredCatalogue
        }
        val base = deduplicatedConcertsForFeed()
            .asSequence()
            .filter { isCurrentOrFutureAgendaEvent(it, day) }
            .sortedWith(compareBy<Concert> { it.date }.thenBy { it.time }.thenBy { it.id })
            .toList()
        sharedUnfilteredCatalogue = base
        sharedUnfilteredDay = todayKey
        sharedUnfilteredRevision = feedDataRevision
        Log.d("AgendaPerf", "sharedUnfiltered BUILD · ${base.size} candidates")
        return base
    }

    fun visibleConcerts(): List<Concert> {
        // Llegir esta versió fa que Compose torne a consultar el feed quan acaba un
        // recàlcul asíncron. No s'utilitza com a dada funcional.
        feedRenderRevision
        val cacheKey = visibleConcertsCacheKey()
        if (feedMode == FeedMode.FOLLOWED && search.isBlank() && filters.isEmpty()) {
            return followedEventsChronological()
        }
        // v331: Sense filtre simple usa la llista universal, ja deduplicada i
        // ordenada, sense passar pel motor de filtres/cerca. L'única diferència
        // per compte són les exclusions i la visibilitat dels esdeveniments.
        if (feedMode == FeedMode.UNFILTERED && search.isBlank() && filters.isEmpty()) {
            // Primer lot persistent: només fins que arribe el catàleg complet.
            // No cal recórrer/deduplicar tota la finestra per a la primera pintura.
            if (!fullAgendaLoadedForProcess && unfilteredPreviewItems.isNotEmpty() &&
                unfilteredPreviewDay == agendaToday().toString() &&
                unfilteredPreviewRevision == feedDataRevision) {
                return safeVisibleFeed(unfilteredPreviewItems)
            }
            // Una segona visita a Sense filtre ni tan sols reconstrueix els dies.
            if (cacheKey == cachedVisibleConcertsKey) return cachedVisibleConcerts
            if (unfilteredSelectionKey() == residentUnfilteredKey && residentUnfilteredItems.isNotEmpty()) {
                cachedVisibleConcertsKey = cacheKey
                cachedVisibleConcerts = residentUnfilteredItems
                Log.d("AgendaPerf", "unfilteredResident HIT · ${residentUnfilteredItems.size} cards")
                return residentUnfilteredItems
            }
            val publicAgenda = sharedUnfilteredBase(agendaToday())
            val visible = publicAgenda.filter(::isEventVisibleForUser)
            val dated = filterOccurrenceMonth(expandMultiDayOccurrences(visible))
                .sortedWith(compareBy<Concert> { it.feedDate }.thenBy { it.time }.thenBy { it.id })
            val answer = safeVisibleFeed(orderVisibleAgendaDays(dated))
            cachedVisibleConcertsKey = cacheKey
            cachedVisibleConcerts = answer
            // Un resultat construït sobre la finestra inicial no és el catàleg
            // global complet. Mai guardar-lo com a resident definitiu.
            if (fullAgendaLoadedForProcess) {
                residentUnfilteredKey = unfilteredSelectionKey()
                residentUnfilteredItems = answer
            }
            return answer
        }
        val defaultForYou = feedMode == FeedMode.FOR_YOU && search.isBlank()
        // v303: l'inici sense seguiments ni preferències usa Sense filtre;
        // només s'executa esta branca personalitzada amb gustos explícits.
        val personalizedForYouMode = defaultForYou
        // Si el catàleg (feedDataRevision) i les preferències no han canviat dins de la
        // mateixa sessió, Per a tu també pot tornar directament la caché de memòria sense
        // ni tan sols recórrer/deduplicar el catàleg.
        if (cacheKey == cachedVisibleConcertsKey &&
            (!personalizedForYouMode || cachedVisibleConcertsFinal)) {
            if (!personalizedForYouMode) return safeVisibleFeed(cachedVisibleConcerts)
            val savedPreferenceSignature = store.getInt(forYouSnapshotPreferenceSignatureKey(), Int.MIN_VALUE)
            val snapshotAge = System.currentTimeMillis() - loadForYouSnapshotTimestamp()
            val stillFresh = snapshotAge in 0 until forYouFastPreviewMaxAge &&
                store.getInt(forYouSnapshotHardSignatureKey(), Int.MIN_VALUE) == forYouHardPreferenceSignature()
            if (stillFresh && savedPreferenceSignature == forYouPreferenceSignature()) return safeVisibleFeed(cachedVisibleConcerts)
        }
        // v301: tornar de Seguits/Sense filtre a Per a tu no torna a recórrer
        // milers d'esdeveniments si ja hi ha un resultat complet de la mateixa
        // revisió, mateix dia i mateixes preferències. L'expiració manual invalida
        // residentForYouReady i les modificacions d'agenda canvien feedDataRevision.
        if (personalizedForYouMode && filters.isEmpty() &&
            selectedFeedCategory == null && canReuseResidentForYou()) {
            val resident = safeVisibleFeed(residentForYouConcerts)
            cachedVisibleConcertsKey = cacheKey
            cachedVisibleConcerts = resident
            cachedVisibleConcertsFinal = true
            return resident
        }

        // v329 · Caché REAL de 30 min: una agenda personal ja calculada
        // no torna a deduplicar, puntuar ni ordenar milers de candidats per
        // sincronitzacions sense efecte, clics, likes ni repintats de la UI.
        // Només es reordenen les fitxes seleccionades i es col·loquen els
        // nous esdeveniments de seguits quan ha canviat la revisió del catàleg.
        if (personalizedForYouMode && filters.isEmpty() && selectedFeedCategory == null &&
            !forYouRecalculationPending && forYouWindowLoadedForProcess &&
            !store.getString(forYouSnapshotKey(), "").isNullOrBlank() &&
            store.getInt(forYouSnapshotHardSignatureKey(), Int.MIN_VALUE) == forYouHardPreferenceSignature() &&
            (System.currentTimeMillis() - loadForYouSnapshotTimestamp()) in 0 until forYouFastPreviewMaxAge
        ) {
            val ids = loadForYouSnapshot()
            val liveById = concertByIdCache
            val initial = ids.mapNotNull(liveById::get)
            if (initial.isNotEmpty()) {
                val latestRevision = runCatching {
                    com.example.agendavalenciana.data.AgendaLocalDatabase.revision(appContext)
                }.getOrDefault(Long.MIN_VALUE)
                val savedRevision = store.getLong(forYouSnapshotDbRevisionKey(), Long.MIN_VALUE)
                val followedExtras = if (latestRevision != savedRevision) {
                    materializedFollowedForYou(concerts)
                        .filter(::isInsideForYouWindow)
                } else emptyList()
                val merged = (initial + followedExtras).distinctBy { it.id }
                val active = merged.filter(::isInsideForYouWindow).filterNot(::isFreeInternalAgendaReservation)
                val occurrences = filterOccurrenceMonth(expandMultiDayOccurrences(active))
                    .sortedWith(compareBy<Concert> { it.feedDate }.thenBy { it.time }.thenBy { it.id })
                val ordered = safeVisibleFeed(orderVisibleAgendaDays(occurrences))
                cachedVisibleConcertsKey = cacheKey
                cachedVisibleConcerts = ordered
                cachedVisibleConcertsFinal = true
                rememberResidentForYou(ordered)
                Log.d("AgendaPerf", "forYouThirtyMinuteCache HIT · ${ordered.size} cards")
                return ordered
            }
        }

        val query = normalizeSearchValue(search)
        val municipalityAlias = municipalityFor(search)
        val searchIndex = if (query.isBlank()) null else feedSearchIndex()
        val explicitArtistSearch = query.isNotBlank() &&
            selectedFeedSearchArtist?.let(::normalizeSearchValue) == query
        val exactArtistSearch = query.isNotBlank() &&
            (explicitArtistSearch || (selectedFeedSearchType == null &&
                municipalityAlias == null && searchIndex?.artistKeys?.contains(query) == true))
        val exactEventTitleSearch = query.isNotBlank() && searchIndex?.eventTitleKeys?.contains(query) == true
        val exactVenueSearch = query.isNotBlank() && searchIndex?.venueKeys?.contains(query) == true
        val exactOrganizerSearch = query.isNotBlank() && searchIndex?.organizerKeys?.contains(query) == true
        val exactComarcaSearch = query.isNotBlank() && municipalities.asSequence()
            .map { it.comarca.trim() }.filter(String::isNotBlank).distinct()
            .any { normalizeSearchValue(it) == query }

        // v284: qualsevol cerca escrita (text lliure o suggeriment seleccionat) és
        // GLOBAL, independent de Llocs, del radi i dels filtres d'Opcions que
        // puguen haver quedat actius d'una navegació anterior. Eixos filtres
        // continuen aplicant-se quan no hi ha cap cerca escrita.
        val globalTextSearch = query.isNotBlank()
        val explicitFilteringDisabled = query.isBlank() && filters.isEmpty() && feedMode == FeedMode.UNFILTERED
        val agendaDay = agendaToday()
        val baseCandidates = deduplicatedConcertsForFeed()
            .asSequence()
            .filter { concert -> isCurrentOrFutureAgendaEvent(concert, agendaDay) }
            // v280: el catàleg de «Per a tu» és complet, però no hi ha un mínim de 100.
            .filter(::isEventVisibleForUser)

        val searchCandidates = baseCandidates
                .filter { globalTextSearch || explicitFilteringDisabled || filters.categories.isEmpty() || it.category in filters.categories }
                .filter { concert ->
                    if (globalTextSearch || explicitFilteringDisabled || (filters.dateFrom == null && filters.dateTo == null)) true else {
                        val eventStart = eventStartDate(concert)
                        val eventEnd = eventEndDate(concert) ?: eventStart
                        val wantedFrom = filters.dateFrom?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
                        val wantedTo = filters.dateTo?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
                        if (eventStart == null || eventEnd == null) true else
                            (wantedTo == null || !eventStart.isAfter(wantedTo)) &&
                            (wantedFrom == null || !eventEnd.isBefore(wantedFrom))
                    }
                }
                .filter { concert ->
                    if (globalTextSearch || explicitFilteringDisabled || filters.month == null) true else {
                        val month = filters.month!!
                        val startMonth = concert.date.take(7)
                        val endMonth = concert.endDate.takeIf(String::isNotBlank)?.take(7) ?: startMonth
                        startMonth <= month && endMonth >= month
                    }
                }
                .filter { globalTextSearch || explicitFilteringDisabled || filters.comarca == null || it.comarca == filters.comarca }
                .filter { globalTextSearch || explicitFilteringDisabled || filters.town == null || it.town == filters.town }
                .filter { concert ->
                    if (globalTextSearch || explicitFilteringDisabled) true else {
                        val maxDistance = filters.maxDistanceKm
                        maxDistance == null || (distanceFromPreferredLocations(concert)?.let { it <= maxDistance } ?: true)
                    }
                }
                .filter { globalTextSearch || explicitFilteringDisabled || filters.maxPrice == null || (it.ticketPrice >= 0 && it.ticketPrice <= filters.maxPrice!!) }
                .filter { globalTextSearch || explicitFilteringDisabled || !filters.freeOnly || it.isFree }
                .toList()

        val filtered = applyStrictFeedSearch(searchCandidates, query, searchIndex, municipalityAlias)

        val modeFiltered = when {
            // «Seguits» és estricte quan no hi ha cerca; una cerca escrita és global,
            // per poder trobar grups que l'usuari encara no seguix, també en festivals.
            feedMode == FeedMode.FOLLOWED && query.isBlank() -> filtered.filter(::isEventFromFollowedEntities)
            query.isNotBlank() -> {
                // En la resta de modes, una cerca explícita continua sent global.
                filtered
            }
            else -> when (feedMode) {
                // Proximitat i seguiments formen part del rànquing de Per a tu; ja no són pestanyes independents.
                FeedMode.FOR_YOU -> filtered
                FeedMode.FOLLOWED -> emptyList() // cobert de manera estricta abans
                FeedMode.CATEGORY -> selectedFeedCategory?.let { selected ->
                    filtered.filter { concert -> concert.category == selected }
                } ?: emptyList()
                FeedMode.UNFILTERED -> filtered
            }
        }

        val chronologicalBase = modeFiltered.sortedWith(compareBy<Concert> { it.date }.thenBy { it.time })

        // Cerca, filtres alternatius i "Sense filtre" són barats i responen en el moment.
        if (feedMode != FeedMode.FOR_YOU || query.isNotBlank()) {
            val occurrences = expandMultiDayOccurrences(chronologicalBase)
            // Una cerca exacta d'artista no ha de repetir el festival en totes les jornades.
            // El feed renderitza únicament les actuacions reals de l'artista a partir del schedule.
            // Conservem les sessions diàries dels esdeveniments que no són festivals.
            val searchOccurrences = if (exactArtistSearch) occurrences.distinctBy { event ->
                if (FestivalFeedNextDay.isConsecutiveFestival(event)) "festival_${event.id}" else event.feedInstanceKey
            } else occurrences
            val chronological = (if (globalTextSearch) searchOccurrences else filterOccurrenceMonth(searchOccurrences))
                .sortedWith(compareBy<Concert> { it.feedDate }.thenBy { it.time })
            val ordered = orderVisibleAgendaDays(chronological)
            cachedVisibleConcertsKey = cacheKey
            cachedVisibleConcerts = safeVisibleFeed(ordered)
            return ordered
        }

        // No donar per definitiva cap llista mentre els senyals o el catàleg
        // complet no estiguen hidratats. El snapshot és només una vista transitòria.
        if (!forYouWindowLoadedForProcess || !forYouSignalsReadyForProcess) {
            val stableOccurrences = if (canReuseResidentForYou()) {
                residentForYouConcerts
            } else {
                // El snapshot no és l'autoritat sobre els seguits: els unim SEMPRE.
                // Si un artista seguit té una activitat nova, no ha d'esperar que
                // torne a executar-se tota la selecció d'afinitats.
                val stableBase = (fastStableForYou(chronologicalBase) + materializedFollowedForYou(chronologicalBase))
                    .distinctBy { it.id }
                filterOccurrenceMonth(expandMultiDayOccurrences(stableBase))
                    .sortedWith(compareBy<Concert> { it.feedDate }.thenBy { it.time }.thenBy { it.id })
            }
            val ordered = orderVisibleAgendaDays(stableOccurrences)
            cachedVisibleConcertsKey = cacheKey
            cachedVisibleConcerts = safeVisibleFeed(ordered)
            cachedVisibleConcertsFinal = false
            return ordered
        }

        // Caché persistent de Per a tu. No caduca cada 10 minuts: es reutilitza mentre
        // coincidisquen preferències/senyals de l'usuari i empremtes dels esdeveniments
        // de la finestra de dos mesos. Això elimina el recàlcul pesat en un inici normal.
        val previousSnapshotIds = loadForYouSnapshot()
        val currentPreferenceSignature = forYouPreferenceSignature()
        val currentEventFingerprints = forYouEventFingerprints(chronologicalBase)
        val savedFollowedIds = loadForYouSnapshotFollowedIds()
        val savedPreferenceSignature = store.getInt(forYouSnapshotPreferenceSignatureKey(), Int.MIN_VALUE)
        val hasUsableSnapshot = loadForYouSnapshotTimestamp() > 0L &&
            savedPreferenceSignature == currentPreferenceSignature
        // v328: la mateixa cadena JSON de fingerprints s'obria fins a tres voltes
        // per validació. Ara es parseja una sola vegada per revisió del feed.
        val savedFingerprints = if (hasUsableSnapshot) loadForYouEventFingerprints() else emptyMap()
        val currentFollowedIds = materializedFollowedForYou(chronologicalBase)
            .mapTo(LinkedHashSet()) { it.id }
        val patchedSnapshotIds = if (hasUsableSnapshot && ForYouSnapshotReusePolicy.isComplete(
                previousSnapshotIds, currentEventFingerprints, savedFingerprints
            )) {
            previousSnapshotIds
        } else if (hasUsableSnapshot && store.contains(forYouSnapshotFollowedIdsKey())) {
            // Si només han canviat fitxes SEGUITES, no reavaluar suggeriments.
            ForYouSnapshotReusePolicy.patchFollowedChanges(
                previousSnapshotIds, currentEventFingerprints,
                savedFingerprints, savedFollowedIds, currentFollowedIds
            )
        } else null
        if (patchedSnapshotIds != null) {
            if (patchedSnapshotIds != previousSnapshotIds ||
                currentEventFingerprints != savedFingerprints) {
                saveForYouSnapshot(patchedSnapshotIds, chronologicalBase, loadForYouSnapshotReasons())
                Log.d("AgendaPerf", "forYouSnapshot PATCH_FOLLOWED · ${patchedSnapshotIds.size} cards")
            } else {
                Log.d("AgendaPerf", "forYouSnapshot HIT · ${chronologicalBase.size} candidates")
            }
            forYouSelectionExplanations = loadForYouSnapshotReasons()
            val stableBase = fastStableForYou(chronologicalBase, patchedSnapshotIds)
            val stableOccurrences = filterOccurrenceMonth(expandMultiDayOccurrences(stableBase))
                .sortedWith(compareBy<Concert> { it.feedDate }.thenBy { it.time }.thenBy { it.id })
            val ordered = orderVisibleAgendaDays(stableOccurrences)
            cachedVisibleConcertsKey = cacheKey
            cachedVisibleConcerts = safeVisibleFeed(ordered)
            cachedVisibleConcertsFinal = true
            rememberResidentForYou(ordered)
            return ordered
        }

        Log.d("AgendaPerf", "forYouSnapshot MISS · ${chronologicalBase.size} candidates · old=${previousSnapshotIds.size}")
        // MISS: no fem el salt intermedi "preview -> snapshot antic complet -> resultat nou".
        // Mantindrem exactament el que l'usuari ja està veient fins que el resultat nou estiga
        // complet. Això deixa un únic canvi visual del feed.
        val followsHaveChanged = residentForYouFollowedSignature?.let { it != followedArtists.hashCode() } ?: false
        val immediateBase = when {
            // No perpetuem una fitxa només perquè abans se seguia l'artista.
            followsHaveChanged -> fastStableForYou(chronologicalBase)
            canReuseResidentForYou() -> residentForYouConcerts
            cachedVisibleConcerts.isNotEmpty() && cachedVisibleConcertsKey == cacheKey -> cachedVisibleConcerts
            else -> fastStableForYou(chronologicalBase).ifEmpty { visibleConcertsState }
        }
        // Els seguits nous entren immediatament encara amb el rànquing calculant-se.
        // No es mostren fitxes retirades, passades ni excloses: sempre provenen del
        // catàleg actual i es passen pel filtre de visibilitat.
        val currentById = chronologicalBase.associateBy { it.id }
        val immediate = orderVisibleAgendaDays(
            (immediateBase.mapNotNull { currentById[it.id] } + materializedFollowedForYou(chronologicalBase))
                .distinctBy { it.id }
        )
        cachedVisibleConcertsKey = cacheKey
        cachedVisibleConcerts = safeVisibleFeed(immediate)
        cachedVisibleConcertsFinal = false
        if (calculatingVisibleConcertsKey != cacheKey) {
            calculatingVisibleConcertsKey = cacheKey
            val chronologicalSnapshot = chronologicalBase.toList()
            // Si ja tenim un snapshot visible, deixem respirar imatges/capçalera abans del
            // recalcul. Sense snapshot, calculem quasi immediatament per no deixar Inici buit.
            val personalizationDelay = if (immediate.isNotEmpty()) 160L else 30L
            Handler(Looper.getMainLooper()).postDelayed({
                personalizationExecutor.execute {
                    if (visibleConcertsCacheKey() != cacheKey) {
                        Handler(Looper.getMainLooper()).post {
                            if (calculatingVisibleConcertsKey == cacheKey) calculatingVisibleConcertsKey = null
                        }
                        return@execute
                    }
                    val computedStarted = SystemClock.elapsedRealtime()
                    val calculation = runCatching { personalizedForYou(chronologicalSnapshot, cacheKey) }
                    if (calculation.isFailure) {
                        Log.e("AgendaPerf", "forYouV260 calculation failed", calculation.exceptionOrNull())
                        Handler(Looper.getMainLooper()).post {
                            if (calculatingVisibleConcertsKey == cacheKey) {
                                calculatingVisibleConcertsKey = null
                                if (visibleConcertsCacheKey() == cacheKey) {
                                    finishForYouRefresh(
                                        visibleConcertsState.size,
                                        "No s'ha pogut recalcular Per a tu. Torna a actualitzar-lo."
                                    )
                                }
                                feedHydrationTick += 1L
                            }
                        }
                        return@execute
                    }
                    val computedRaw = calculation.getOrThrow()
                    // El resultat final ha de ser estrictament cronològic. No fixem un prefix
                    // antic davant del resultat nou perquè podria trencar l'ordre per data/hora.
                    val computed = computedRaw.events
                    val computedOccurrences = orderVisibleAgendaDays(
                        filterOccurrenceMonth(expandMultiDayOccurrences(computed))
                            .sortedWith(compareBy<Concert> { it.feedDate }.thenBy { it.time }.thenBy { it.id })
                    )
                    Log.d("AgendaPerf", "personalizedForYou ${SystemClock.elapsedRealtime() - computedStarted}ms · ${computed.size} cards")
                    Handler(Looper.getMainLooper()).post {
                        if (visibleConcertsCacheKey() == cacheKey) {
                            cachedVisibleConcertsKey = cacheKey
                            cachedVisibleConcerts = safeVisibleFeed(computedOccurrences)
                            cachedVisibleConcertsFinal = true
                            rememberResidentForYou(computedOccurrences)
                            forYouSelectionExplanations = computedRaw.reasons
                            visibleConcertsState = safeVisibleFeed(computedOccurrences)
                            if (forYouRecalculationPending) {
                                finishForYouRefresh(visibleConcertsState.size)
                            }
                            feedRenderRevision += 1L
                        }
                        if (calculatingVisibleConcertsKey == cacheKey) {
                            calculatingVisibleConcertsKey = null
                            feedHydrationTick += 1L
                        }
                    }
                }
            }, personalizationDelay)
        }
        return safeVisibleFeed(immediate)
    }

    private data class AggregatedBehaviour(
        val kind: BehaviourKind,
        val value: String,
        val lastTimestamp: Long,
        val count: Int
    )

    private fun canonicalCategoryKey(raw: String): String {
        val wanted = normalizeSearchValue(raw)
        if (wanted.isBlank()) return ""
        val category = CulturalCategory.values().firstOrNull { item ->
            normalizeSearchValue(item.name) == wanted || normalizeSearchValue(item.label) == wanted
        }
        return category?.name?.let(::normalizeSearchValue) ?: wanted
    }

    /**
     * ESTIL/CATEGORIA d'ENTITATS són la font principal. Els camps de l'esdeveniment
     * es conserven i actuen també de fallback/complement quan el catàleg encara no
     * està disponible en cache. El text visible no es modifica mai.
     */
    private fun entityAffinityForConcert(concert: Concert): List<ServerApiRepository.EntityAffinityMetadata> {
        val catalog = ServerApiRepository.peekCachedArtistAffinityMetadata()
        if (catalog.isEmpty()) return emptyList()
        return (concert.lineup + concert.artist).asSequence()
            .map(::normalizeSearchValue)
            .filter(String::isNotBlank)
            .distinct()
            .mapNotNull(catalog::get)
            .toList()
    }

    private fun canonicalStyleKeys(concert: Concert): Set<String> = buildSet {
        addAll(StyleAffinityCatalog.canonicalKeys(appContext, concert.styles))
        entityAffinityForConcert(concert).forEach { meta ->
            addAll(StyleAffinityCatalog.canonicalKeys(appContext, meta.styles))
        }
    }

    private fun canonicalCategoryKeys(concert: Concert): Set<String> = buildSet {
        canonicalCategoryKey(concert.category.name).takeIf(String::isNotBlank)?.let(::add)
        entityAffinityForConcert(concert).forEach { meta ->
            canonicalCategoryKey(meta.category).takeIf(String::isNotBlank)?.let(::add)
        }
    }

    private fun eventArtistKeys(concert: Concert): Set<String> = (concert.lineup + concert.artist)
        .asSequence().map(::normalizeSearchValue).filter(String::isNotBlank).toSet()

    /** v288 · Mateix llenguatge de codis en styles (agenda) i ESTIL (Entitats). */
    private data class ForYouCalculation(val events: List<Concert>, val reasons: Map<Int, String>)

    private fun canonicalStyleCodes(rawStyles: Iterable<String>): Set<String> = rawStyles.asSequence()
        .flatMap { it.split('|', ',', ';').asSequence() }
        .map(CategoryTaxonomy::canonicalizeCode)
        .filter(CategoryTaxonomy::isAnySubcategoryKey)
        .toSet()

    /** styles és la font principal; subcategories_codis només compatibilitat amb la BD antiga. */
    private fun explicitEventSubcategoryCodes(concert: Concert): Set<String> {
        val codesFromStyles = canonicalStyleCodes(concert.styles)
        if (codesFromStyles.isNotEmpty()) return codesFromStyles
        val legacyCodes = canonicalStyleCodes(concert.subcategoryCodes)
        if (legacyCodes.isNotEmpty()) return legacyCodes
        return entityAffinityForConcert(concert).asSequence()
            .flatMap { it.styles.asSequence() }
            .flatMap { it.split('|', ',', ';').asSequence() }
            .map(CategoryTaxonomy::canonicalizeCode)
            .filter(CategoryTaxonomy::isAnySubcategoryKey).toSet()
    }

    /** ESTIL de les Entitats vinculades a Seguits, mai inferit per noms d'estils. */
    private fun followedEntitySubcategoryCodes(followedKeys: Set<String>): Set<String> {
        if (followedKeys.isEmpty()) return emptySet()
        val catalog = ServerApiRepository.peekCachedArtistAffinityMetadata()
        val classified = followedKeys.asSequence().mapNotNull(catalog::get).flatMap { entity ->
            val primary = canonicalStyleCodes(entity.styles)
            (if (primary.isNotEmpty()) primary else canonicalStyleCodes(entity.subcategoryCodes)).asSequence()
        }.toSet()
        val missing = followedKeys.filter { key ->
            val meta = catalog[key]
            meta == null || canonicalStyleCodes(meta.styles + meta.subcategoryCodes).isEmpty()
        }.toSet()
        val fromConcerts = if (missing.isEmpty()) emptySet() else concerts.asSequence()
            .filter { concert -> eventArtistKeys(concert).any(missing::contains) }
            .flatMap { explicitEventSubcategoryCodes(it).asSequence() }.toSet()
        return classified + fromConcerts
    }

    /** Subcategories elegides expressament, identificades pel mateix codi d'estil. */
    private fun selectedPersonalizationSubcategoryCodes(): Set<String> = buildSet {
        followedCategories.forEach { key ->
            val item = CategoryTaxonomy.findByCode(key)
            if (item != null) add(item.code) else {
                val category = CulturalCategory.entries.firstOrNull { it.name == key }
                if (category != null) addAll(CategoryTaxonomy.childrenOf(category).map { it.code })
            }
        }
    }

    /** Radi fix de 100 km per a preferències; per als Seguits i les suggerències, el radi propi. */
    private fun withinFixedPersonalizationRadius100(
        concert: Concert, comarcaOrigins: List<Municipality>
    ): Boolean {
        val towns = userPreferences.preferredLocations
        val comarques = userPreferences.preferredComarques
        if (towns.isEmpty() && comarques.isEmpty()) return false
        val eventTown = normalizeSearchValue(concert.town)
        if (towns.any { town ->
            val origin = municipalityFor(town)
            normalizeSearchValue(town) == eventTown ||
                (origin != null && (isConcertInMunicipality(concert, origin) ||
                    distanceFromMunicipality(origin, concert)?.let { it <= 100.0 } == true))
        }) return true
        val eventComarca = normalizeSearchValue(concert.comarca.takeUnless {
            it.isBlank() || it.equals("Sense comarca", ignoreCase = true)
        } ?: municipalityFor(concert.town)?.comarca.orEmpty())
        if (eventComarca.isNotBlank() && comarques.any { normalizeSearchValue(it) == eventComarca }) return true
        val destination = municipalityFor(concert.town) ?: return false
        if (destination.latitude == 0.0 || destination.longitude == 0.0) return false
        return comarcaOrigins.any { origin ->
            origin.latitude != 0.0 && origin.longitude != 0.0 &&
                GeoDistance.kilometres(origin.latitude, origin.longitude,
                    destination.latitude, destination.longitude) <= 100.0
        }
    }

    /**
     * Només cerques fetes durant els últims 15 dies i artistes/grups de cards oberts.
     * Les obertures també es limiten a 15 dies per a no acumular gustos antics.
     * Un municipi cercat no transforma automàticament tots els seus actes en gustos.
     */
    private fun activeSuggestedStyleCodes(now: Long, source: List<Concert>): Map<String, Int> {
        val cutoff = now - 15L * 86_400_000L
        val eligible = behaviourProfile.events.asSequence()
            .filter { it.timestampMillis in cutoff..now }
            .filter { it.kind in setOf(BehaviourKind.SEARCH, BehaviourKind.ARTIST,
                BehaviourKind.EVENT_VIEW, BehaviourKind.STYLE) }
            .toList().takeLast(120)
        if (eligible.isEmpty()) return emptyMap()
        val metadata = ServerApiRepository.peekCachedArtistAffinityMetadata()
        val sourceById = source.associateBy { it.id }
        // V298 · Normalitzar artistes i títols una única vegada, només quan
        // una cerca o un artista sense metadata necessite escanejar el catàleg.
        data class SearchableEvent(val event: Concert, val artists: Set<String>, val title: String)
        val searchable by lazy(LazyThreadSafetyMode.NONE) {
            source.map { SearchableEvent(it, eventArtistKeys(it), normalizeSearchValue(it.title)) }
        }
        val exactArtistMatches = HashMap<String, List<Set<String>>>()
        val searchTermMatches = HashMap<String, List<Set<String>>>()
        val counts = linkedMapOf<String, Int>()
        fun addCodes(codes: Set<String>) { codes.forEach { counts[it] = (counts[it] ?: 0) + 1 } }
        eligible.forEach { signal ->
            val term = normalizeSearchValue(signal.value)
            when (signal.kind) {
                BehaviourKind.EVENT_VIEW -> {
                    val opened = signal.value.toIntOrNull()?.let(sourceById::get)
                    if (opened != null) addCodes(explicitEventSubcategoryCodes(opened))
                }
                BehaviourKind.ARTIST -> {
                    val entity = metadata[term]
                    if (entity != null) addCodes(canonicalStyleCodes(entity.styles + entity.subcategoryCodes))
                    else exactArtistMatches.getOrPut(term) {
                        searchable.asSequence().filter { term in it.artists }.take(5)
                            .map { explicitEventSubcategoryCodes(it.event) }.toList()
                    }.forEach(::addCodes)
                }
                BehaviourKind.STYLE -> addCodes(canonicalStyleCodes(listOf(signal.value)))
                BehaviourKind.SEARCH -> {
                    if (term.length < 3) return@forEach
                    // Cerques explícites d'artista o títol, mai activitat per població sola.
                    metadata[term]?.let { addCodes(canonicalStyleCodes(it.styles + it.subcategoryCodes)) }
                    searchTermMatches.getOrPut(term) {
                        searchable.asSequence().filter { event ->
                            event.artists.any { key -> key.contains(term) } ||
                                event.title.contains(term)
                        }.take(6).map { explicitEventSubcategoryCodes(it.event) }.toList()
                    }.forEach(::addCodes)
                }
                else -> Unit
            }
        }
        return counts
    }

    private fun personalizedForYou(chronological: List<Concert>, expectedCacheKey: String): ForYouCalculation {
        if (chronological.isEmpty()) {
            if (visibleConcertsCacheKey() == expectedCacheKey) saveForYouSnapshot(emptyList(), chronological, emptyMap())
            return ForYouCalculation(emptyList(), emptyMap())
        }
        val signature = forYouPreferenceSignature()
        // New canonical server selection. The existing local Android engine remains an
        // OFFLINE fallback only; successful server results are never re-ranked locally.
        if (ServerApiRepository.isConfigured()) {
            val prefs = JSONObject()
                .put("categories", JSONArray(followedCategories.toList()))
                .put("subcategories", JSONArray(selectedPersonalizationSubcategoryCodes().toList()))
                .put("towns", JSONArray(userPreferences.preferredLocations.toList()))
                .put("comarques", JSONArray(userPreferences.preferredComarques.toList()))
                .put("suggestions", userPreferences.showSuggestions)
                .put("discovery", userPreferences.discoveryLevel.name)
                .put("valencian_first", userPreferences.valencianFirst)
            val radii = JSONObject()
            userPreferences.preferredLocations.forEach { town ->
                radii.put(town, userPreferences.radiusForLocation(town))
            }
            prefs.put("radii", radii)
            val guestFollows = JSONArray()
            followedArtists.forEach { name -> guestFollows.put(JSONObject()
                .put("type", "ARTIST").put("key", name).put("label", name)) }
            followedOrganizers.forEach { name -> guestFollows.put(JSONObject()
                .put("type", "ORGANIZER").put("key", name).put("label", name)) }
            followedVenues.forEach { name -> guestFollows.put(JSONObject()
                .put("type", "VENUE").put("key", name).put("label", name)) }
            followedCategories.forEach { name -> guestFollows.put(JSONObject()
                .put("type", "CATEGORY").put("key", name).put("label", name)) }
            val serverIds = ServerApiRepository.computeSharedForYou(appContext, prefs, guestFollows).getOrNull()
            if (serverIds != null) {
                val remoteSelected = serverIds.toSet()
                val normalizedFollowed = followedArtists.map(::normalizeSearchValue).toSet()
                val ordered = chronological.filter { event ->
                    event.id in remoteSelected &&
                        (isEventVisibleForUser(event) || eventArtistKeys(event).any { it in normalizedFollowed }) &&
                        !isFreeInternalAgendaReservation(event)
                }.sortedWith(compareBy<Concert> { parseConcertDate(it) ?: java.time.LocalDate.MAX }
                    .thenByDescending { userPreferences.valencianFirst &&
                        concertLanguageKeys(it).contains(canonicalLanguageKey("Valencià")) }
                    .thenBy { it.time }.thenBy { it.id })
                if (visibleConcertsCacheKey() == expectedCacheKey && forYouPreferenceSignature() == signature) {
                    saveForYouSnapshot(ordered.map { it.id }, chronological, emptyMap())
                }
                return ForYouCalculation(ordered, emptyMap())
            }
        }
        val followedKeys = followedArtists.asSequence().map(::normalizeSearchValue)
            .filter(String::isNotBlank).toSet()
        val followedOrganizerKeys = followedOrganizers.asSequence()
            .flatMap { organizer -> sequenceOf(organizer, profileFor(organizer).displayName) }
            .map(::normalizeSearchValue).filter(String::isNotBlank).toSet()
        val followedVenueKeys = followedVenues.asSequence()
            .map(::normalizeSearchValue).filter(String::isNotBlank).toSet()
        val followedCodes = followedEntitySubcategoryCodes(followedKeys)
        val selectedCodes = selectedPersonalizationSubcategoryCodes()
        val hasPreferredGeography = userPreferences.preferredLocations.isNotEmpty() ||
            userPreferences.preferredComarques.isNotEmpty()
        val suggestedCodes = if (userPreferences.showSuggestions)
            activeSuggestedStyleCodes(System.currentTimeMillis(), chronological) else emptyMap()
        val valenciaKey = canonicalLanguageKey("Valencià")
        val comarcaKeys = userPreferences.preferredComarques.map(::normalizeSearchValue).toSet()
        val comarcaOrigins = if (comarcaKeys.isEmpty()) emptyList() else municipalities.filter {
            normalizeSearchValue(it.comarca) in comarcaKeys
        }
        // Molts actes comparteixen municipi. Un resultat geogràfic depén del
        // poble i la comarca, no del títol: calcular-lo una volta per ubicació.
        // Les caches només viuen durant este càlcul, mai entre canvis de gustos.
        val preferredRadiusByPlace = HashMap<Pair<String, String>, Boolean>()
        val fixed100KmByPlace = HashMap<Pair<String, String>, Boolean>()
        val candidates = chronological.map { event ->
            val codes = explicitEventSubcategoryCodes(event)
            val followed = (followedKeys.isNotEmpty() && eventArtistKeys(event).any(followedKeys::contains)) ||
                (followedOrganizerKeys.isNotEmpty() &&
                    (event.organizers.any { normalizeSearchValue(it) in followedOrganizerKeys } ||
                        event.coEditorUsernames.any { normalizeSearchValue(it) in followedOrganizerKeys })) ||
                (followedVenueKeys.isNotEmpty() && normalizeSearchValue(event.venue) in followedVenueKeys)
            val matchesFollowedStyle = !followed && codes.any(followedCodes::contains)
            val matchesPersonalization = codes.any(selectedCodes::contains)
            val matchesSuggestion = codes.any(suggestedCodes::contains)
            val place = event.town to event.comarca
            // Els seguits entren sempre, sense haver de calcular distàncies.
            val preferredNearby = (matchesFollowedStyle || matchesSuggestion) &&
                preferredRadiusByPlace.getOrPut(place) { preferredGeographyScore(event) > 0 }
            val fromFollowed = preferredNearby && matchesFollowedStyle
            // Categoria sense Llocs: coincidència cultural. Llocs sense categoria:
            // actes dins del territori preferit. Les dos juntes exigixen les dos.
            val personalized = (if (selectedCodes.isNotEmpty()) matchesPersonalization else hasPreferredGeography) &&
                (!hasPreferredGeography || fixed100KmByPlace.getOrPut(place) {
                    withinFixedPersonalizationRadius100(event, comarcaOrigins)
                })
            val suggestionScore = if (preferredNearby) codes.sumOf { suggestedCodes[it] ?: 0 } else 0
            val diagnostics = if (BuildConfig.DEBUG && ForYouDiagnostics.ENABLED) buildList {
                if (followed) add("Seguits: artistes, organitzadors o sales seleccionats")
                if (fromFollowed) add("ESTIL dels Seguits: ${codes.intersect(followedCodes).sorted().joinToString(", ")} · radi elegit")
                if (personalized) add("Personalització: ${codes.intersect(selectedCodes).sorted().joinToString(", ").ifBlank { "territori" }}" +
                    if (hasPreferredGeography) " · radi preferit" else "")
                if (suggestionScore > 0) add("Suggerència: cerca o card oberta en els últims 15 dies · radi elegit")
                if (userPreferences.valencianFirst && concertLanguageKeys(event).contains(valenciaKey))
                    add("Preferència de valencià activa")
            } else emptyList()
            ForYouHundredPolicy.Candidate(
                id = event.id,
                eligibleForRecommendations = isEventVisibleForUser(event),
                followed = followed,
                freeInternalReservation = isFreeInternalAgendaReservation(event),
                interest = fromFollowed,
                proximity = personalized,
                discovery = suggestionScore > 0,
                recent = suggestionScore > 0,
                affinityScore = suggestionScore,
                diagnosticSignals = diagnostics
            )
        }
        val selection = ForYouHundredPolicy.selectDetailed(
            candidates, userPreferences.discoveryLevel,
            includeReasons = BuildConfig.DEBUG && ForYouDiagnostics.ENABLED
        )
        val selectedIds = selection.ids.toSet()
        val result = chronological.filter { it.id in selectedIds }
            .sortedWith(compareBy<Concert> { parseConcertDate(it) ?: java.time.LocalDate.MAX }
                .thenByDescending { userPreferences.valencianFirst && concertLanguageKeys(it).contains(valenciaKey) }
                .thenBy { it.time }.thenBy { it.id })
        Log.d("AgendaPerf", "forYouV288 candidates=${candidates.size} selected=${result.size} " +
            "follows=${candidates.count { it.followed }} similar=${candidates.count { it.interest }} " +
            "personalization=${candidates.count { it.proximity }} suggestions=${candidates.count { it.discovery }}")
        if (visibleConcertsCacheKey() == expectedCacheKey && forYouPreferenceSignature() == signature) {
            saveForYouSnapshot(result.map { it.id }, chronological,
                if (BuildConfig.DEBUG && ForYouDiagnostics.ENABLED) selection.reasons else emptyMap())
        }
        return ForYouCalculation(result, selection.reasons)
    }

    private fun aggregateBehaviour(now: Long): List<AggregatedBehaviour> = behaviourProfile.events
        .asSequence()
        .filter { behaviourDecay(it.timestampMillis, now) > 0.0 }
        .groupBy { it.kind to normalizeSearchValue(it.value) }
        .map { (key, values) ->
            AggregatedBehaviour(key.first, key.second, values.maxOf { it.timestampMillis }, values.size)
        }

    private fun canonicalLanguageKey(raw: String): String {
        val key = normalizeSearchValue(raw)
        return when (key) {
            normalizeSearchValue("Valencià"),
            normalizeSearchValue("Valenciano"),
            normalizeSearchValue("Català"),
            normalizeSearchValue("Catalán") -> normalizeSearchValue("Català")

            normalizeSearchValue("Castellà"),
            normalizeSearchValue("Castellano"),
            normalizeSearchValue("Español"),
            normalizeSearchValue("Espanyol") -> normalizeSearchValue("Castellà")

            else -> key
        }
    }

    private fun concertLanguageKeys(concert: Concert): List<String> {
        val entityLanguages = entityAffinityForConcert(concert)
            .asSequence()
            .flatMap { meta ->
                meta.language.replace(" / ", "|").split('|', ',', ';').asSequence()
            }
            .map(::canonicalLanguageKey)
            .filter(String::isNotBlank)
            .distinct()
            .toList()
        if (entityLanguages.isNotEmpty()) return entityLanguages

        val raw = if (concert.languages.isNotEmpty()) concert.languages else concert.language
            .replace(" / ", "|")
            .split('|', ',', ';')
        return raw.asSequence()
            .map(::canonicalLanguageKey)
            .filter(String::isNotBlank)
            .distinct()
            .toList()
    }

    private fun personalizedInterestScore(
        concert: Concert,
        inferredStyles: Set<String>,
        followedStyles: Set<String>,
        aggregated: List<AggregatedBehaviour>,
        eventById: Map<Int, Concert>,
        languageProfile: Map<String, Double>,
        now: Long
    ): Int {
        var score = 0
        // Normalitzem cada camp una sola vegada per candidat. Abans es repetia dins de cada
        // senyal d’afinitat i multiplicava el cost per events × signals.
        val lineupKeys = concert.lineup.asSequence().map(::normalizeSearchValue).filter(String::isNotBlank).toSet()
        val townKey = normalizeSearchValue(concert.town)
        val comarcaKey = normalizeSearchValue(concert.comarca)
        val venueKey = normalizeSearchValue(concert.venue)
        val categoryKey = normalizeSearchValue(concert.category.name)
        val organizerKeys = concert.coEditorUsernames.asSequence().map(::normalizeSearchValue).filter(String::isNotBlank).toSet()
        val organizerDisplayKeys = concert.coEditorUsernames.asSequence()
            .map { normalizeSearchValue(profileFor(it).displayName) }
            .filter(String::isNotBlank).toSet()
        val styleKeys = canonicalStyleKeys(concert)
        val languageKeys = concertLanguageKeys(concert)
        val searchText = normalizeSearchValue(
            listOf(concert.title, concert.artist, concert.lineup.joinToString(" "), concert.venue, concert.town, concert.comarca)
                .plus(concert.styles).joinToString(" ")
        )

        // Els creadors veuen sempre els seus propis esdeveniments dins de la zona cronològica corresponent.
        val account = currentAccount
        if (account?.serverUserId != null && concert.creatorUserId == account.serverUserId) score += 500
        if (concert.lineup.any { it in followedArtists }) score += 92
        val organizerUsernames = concert.coEditorUsernames
        if (organizerUsernames.any { it in followedOrganizers }) score += 62
        if (followsConcertCategory(concert)) score += 68
        if (concert.id in savedConcerts) score += 45
        when (attendanceFor(concert.id)) {
            AttendanceStatus.WANT_TO_GO -> score += 50
            AttendanceStatus.GOING -> score += 65
            else -> Unit
        }
        if (concert.id in likedConcerts) score += 30
        score += styleKeys.count { it in inferredStyles } * 10
        if (styleKeys.any { it in followedStyles }) score += 12

        aggregated.forEach { signal ->
            val decay = behaviourDecay(signal.lastTimestamp, now)
            if (decay <= 0.0) return@forEach
            val frequency = 1.0 + ((signal.count - 1).coerceIn(0, 5) * 0.12)
            val value = signal.value
            val rawBonus = when (signal.kind) {
                BehaviourKind.ARTIST -> if (value in lineupKeys) 50 else 0
                BehaviourKind.TOWN -> if (townKey == value) 25 else 0
                BehaviourKind.COMARCA -> if (comarcaKey == value) 36 else 0
                BehaviourKind.VENUE -> if (venueKey == value) 14 else 0
                BehaviourKind.CATEGORY -> if (categoryKey == value) 38 else 0
                BehaviourKind.ORGANIZER -> if (value in organizerKeys || value in organizerDisplayKeys) 44 else 0
                BehaviourKind.SEARCH -> if (value.isNotBlank() && searchText.contains(value)) 16 else 0
                BehaviourKind.EVENT_VIEW,
                BehaviourKind.SAVED_EVENT,
                BehaviourKind.WANT_TO_GO,
                BehaviourKind.GOING -> {
                    val sourceId = value.toIntOrNull() ?: return@forEach
                    val source = eventById[sourceId] ?: return@forEach
                    contextualEventAffinity(signal.kind, source, concert)
                }
                BehaviourKind.FOLLOWED_ARTIST, BehaviourKind.FOLLOWED_ORGANIZER, BehaviourKind.FOLLOWED_CATEGORY -> 0
                BehaviourKind.STYLE -> {
                    val canonical = StyleAffinityCatalog.canonicalKey(appContext, value)
                    when {
                        canonical in styleKeys -> 15
                        StyleAffinityCatalog.bestProximityRank(appContext, setOf(canonical), styleKeys) != null -> 7
                        else -> 0
                    }
                }
                BehaviourKind.LANGUAGE -> if (value in languageKeys) 15 else 0
                else -> 0
            }
            score += (rawBonus * decay * frequency).roundToInt()
        }

        if (languageKeys.isNotEmpty()) score += ((languageKeys.maxOfOrNull { languageProfile[it] ?: 0.0 } ?: 0.0) * 24.0).roundToInt()
        if (userPreferences.valencianFirst && languageKeys.any { it == normalizeSearchValue("Català") }) score += 10
        return score
    }

    private fun contextualEventAffinity(kind: BehaviourKind, source: Concert, candidate: Concert): Int {
        val multiplier = when (kind) {
            BehaviourKind.EVENT_VIEW -> 1.0
            BehaviourKind.SAVED_EVENT -> 2.2
            BehaviourKind.WANT_TO_GO -> 2.8
            BehaviourKind.GOING -> 3.3
            else -> 1.0
        }
        if (source.id == candidate.id) return (12 * multiplier).roundToInt()
        val near = isTerritoriallyNear(source)
        var points = 0
        val sameArtist = source.lineup.any { a -> candidate.lineup.any { b -> a.equals(b, true) } }
        val sourceOrganizers = source.coEditorUsernames.map(::normalizeSearchValue).toSet()
        val candidateOrganizers = candidate.coEditorUsernames.map(::normalizeSearchValue).toSet()
        val sameOrganizer = sourceOrganizers.intersect(candidateOrganizers).isNotEmpty()
        val sameLanguage = concertLanguageKeys(source).toSet().intersect(concertLanguageKeys(candidate).toSet()).isNotEmpty()
        val sameCategory = source.category == candidate.category
        val sourceStyles = canonicalStyleKeys(source)
        val candidateStyles = canonicalStyleKeys(candidate)
        val sameStyle = sourceStyles.intersect(candidateStyles).isNotEmpty()
        val nearStyle = !sameStyle && StyleAffinityCatalog.bestProximityRank(appContext, sourceStyles, candidateStyles) != null

        if (near) {
            if (source.comarca.equals(candidate.comarca, true)) points += 8
            if (source.town.equals(candidate.town, true)) points += 5
            if (sameArtist) points += 7
            if (sameOrganizer) points += 5
            if (sameCategory) points += 4
            if (sameLanguage) points += 4
            if (sameStyle) points += 3 else if (nearStyle) points += 1
        } else {
            // Si l'usuari s'interessa per un acte llunyà, el contingut pesa més que el territori.
            if (sameArtist) points += 15
            if (sameOrganizer) points += 11
            if (sameCategory) points += 8
            if (sameLanguage) points += 9
            if (sameStyle) points += 9 else if (nearStyle) points += 5
            if (source.comarca.equals(candidate.comarca, true)) points += 2
        }
        return (points * multiplier).roundToInt()
    }

    private fun isTerritoriallyNear(concert: Concert): Boolean {
        val eventComarca = concert.comarca.takeIf {
            it.isNotBlank() && !it.equals("Sense comarca", ignoreCase = true)
        } ?: comarcaForTown(concert.town).orEmpty()
        if (userPreferences.preferredComarques.any { normalizeSearchValue(it) == normalizeSearchValue(eventComarca) }) return true
        if (userPreferences.preferredLocations.isNotEmpty() || userPreferences.preferredComarques.isNotEmpty()) {
            if (userPreferences.preferredLocations.any { town ->
                    val origin = municipalityFor(town)
                    if (origin == null) concert.town.equals(town, ignoreCase = true)
                    else isConcertInMunicipality(concert, origin) ||
                        (distanceFromMunicipality(origin, concert)?.let { it <= userPreferences.radiusForLocation(town) } == true)
                }) return true
            // Una localització guardada fora de radi no obté impuls per comarca.
            return false
        }
        val strongest = strongestComarcaInterest()?.first
        return strongest != null && concert.comarca.equals(strongest, ignoreCase = true)
    }

    /**
     * Idioma implícit: es dedueix del contingut dels artistes, organitzadors, territoris
     * i esdeveniments pels quals l'usuari ha mostrat interés. No cal cercar "valencià".
     */
    private fun languageAffinityProfile(
        aggregated: List<AggregatedBehaviour>,
        eventById: Map<Int, Concert>,
        now: Long
    ): Map<String, Double> {
        val weights = mutableMapOf<String, Double>()
        val languageCache = mutableMapOf<String, List<String>>()
        val entityLanguages = ServerApiRepository.peekCachedArtistAffinityMetadata().mapValues { (_, meta) ->
            meta.language.replace(" / ", "|")
                .split('|', ',', ';')
                .map(::canonicalLanguageKey)
                .filter(String::isNotBlank)
                .distinct()
        }
        fun languagesOf(concert: Concert): List<String> = languageCache.getOrPut(concert.feedInstanceKey) {
            val fromEntities = eventArtistKeys(concert).flatMap { entityLanguages[it].orEmpty() }.distinct()
            if (fromEntities.isNotEmpty()) fromEntities else concertLanguageKeys(concert)
        }
        fun addLanguage(language: String, weight: Double) {
            val key = canonicalLanguageKey(language)
            if (key.isNotBlank() && weight > 0.0) weights[key] = (weights[key] ?: 0.0) + weight
        }
        fun distribute(items: List<Concert>, weight: Double) {
            if (items.isEmpty() || weight <= 0.0) return
            val grouped = items.asSequence().flatMap { languagesOf(it).asSequence() }.groupingBy { it }.eachCount()
            val total = grouped.values.sum().coerceAtLeast(1)
            grouped.forEach { (lang, count) -> addLanguage(lang, weight * count.toDouble() / total.toDouble()) }
        }

        // Índexs d’una sola passada. Substituïxen concerts.filter(...) repetits per cada senyal.
        val artistsRaw = mutableMapOf<String, MutableList<Concert>>()
        val artistsNorm = mutableMapOf<String, MutableList<Concert>>()
        val organizersRaw = mutableMapOf<String, MutableList<Concert>>()
        val organizersNorm = mutableMapOf<String, MutableList<Concert>>()
        val townsNorm = mutableMapOf<String, MutableList<Concert>>()
        val comarquesNorm = mutableMapOf<String, MutableList<Concert>>()
        val categoriesNorm = mutableMapOf<String, MutableList<Concert>>()
        concerts.forEach { concert ->
            concert.lineup.distinct().forEach { artist ->
                artistsRaw.getOrPut(artist) { mutableListOf() }.add(concert)
            }
            concert.lineup.asSequence().map(::normalizeSearchValue).filter(String::isNotBlank).distinct().forEach { key ->
                artistsNorm.getOrPut(key) { mutableListOf() }.add(concert)
            }
            concert.coEditorUsernames.distinct().forEach { organizer ->
                organizersRaw.getOrPut(organizer) { mutableListOf() }.add(concert)
            }
            buildSet {
                concert.coEditorUsernames.forEach { organizer ->
                    normalizeSearchValue(organizer).takeIf(String::isNotBlank)?.let(::add)
                    normalizeSearchValue(profileFor(organizer).displayName).takeIf(String::isNotBlank)?.let(::add)
                }
            }.forEach { key -> organizersNorm.getOrPut(key) { mutableListOf() }.add(concert) }
            normalizeSearchValue(concert.town).takeIf(String::isNotBlank)?.let { townsNorm.getOrPut(it) { mutableListOf() }.add(concert) }
            normalizeSearchValue(concert.comarca).takeIf(String::isNotBlank)?.let { comarquesNorm.getOrPut(it) { mutableListOf() }.add(concert) }
            normalizeSearchValue(concert.category.name).takeIf(String::isNotBlank)?.let { categoriesNorm.getOrPut(it) { mutableListOf() }.add(concert) }
        }

        followedArtists.forEach { artist -> distribute(artistsRaw[artist].orEmpty(), 3.5) }
        followedOrganizers.forEach { organizer -> distribute(organizersRaw[organizer].orEmpty(), 3.0) }
        followedCategories.forEach { category -> distribute(categoriesNorm[normalizeSearchValue(category)].orEmpty(), 2.2) }

        aggregated.forEach { signal ->
            val decay = behaviourDecay(signal.lastTimestamp, now)
            if (decay <= 0.0) return@forEach
            val intensity = decay * (1.0 + ((signal.count - 1).coerceIn(0, 5) * 0.10))
            when (signal.kind) {
                BehaviourKind.ARTIST -> distribute(artistsNorm[signal.value].orEmpty(), 2.4 * intensity)
                BehaviourKind.ORGANIZER -> distribute(organizersNorm[signal.value].orEmpty(), 2.2 * intensity)
                BehaviourKind.TOWN -> distribute(townsNorm[signal.value].orEmpty(), 1.5 * intensity)
                BehaviourKind.COMARCA -> distribute(comarquesNorm[signal.value].orEmpty(), 1.8 * intensity)
                BehaviourKind.CATEGORY -> distribute(categoriesNorm[signal.value].orEmpty(), 1.2 * intensity)
                BehaviourKind.LANGUAGE -> addLanguage(signal.value, 2.0 * intensity)
                BehaviourKind.EVENT_VIEW,
                BehaviourKind.SAVED_EVENT,
                BehaviourKind.WANT_TO_GO,
                BehaviourKind.GOING -> {
                    val sourceId = signal.value.toIntOrNull() ?: return@forEach
                    val source = eventById[sourceId] ?: return@forEach
                    val base = when (signal.kind) {
                        BehaviourKind.EVENT_VIEW -> if (isTerritoriallyNear(source)) 0.5 else 1.2
                        BehaviourKind.SAVED_EVENT -> if (isTerritoriallyNear(source)) 1.5 else 2.7
                        BehaviourKind.WANT_TO_GO -> if (isTerritoriallyNear(source)) 2.0 else 3.2
                        BehaviourKind.GOING -> if (isTerritoriallyNear(source)) 2.4 else 3.8
                        else -> 0.0
                    }
                    val sourceLanguages = languagesOf(source)
                    if (sourceLanguages.isNotEmpty()) sourceLanguages.forEach { addLanguage(it, base * intensity / sourceLanguages.size.toDouble()) }
                }
                else -> Unit
            }
        }

        val total = weights.values.sum()
        if (total <= 0.0) return emptyMap()
        return weights.mapValues { it.value / total }
    }

    private fun searchTopicBonus(term: String, concert: Concert): Int {
        if (term.isBlank()) return 0
        val text = normalizeSearchValue(
            listOf(concert.title, concert.artist, concert.lineup.joinToString(" "), concert.venue, concert.town, concert.comarca)
                .plus(concert.styles)
                .joinToString(" ")
        )
        return if (text.contains(term)) 16 else 0
    }

    /** 100% el primer mes; descens lineal fins a 0% als sis mesos. Buscar de nou reinicia el rellotge. */
    private fun behaviourDecay(timestampMillis: Long, nowMillis: Long = System.currentTimeMillis()): Double {
        val ageDays = ((nowMillis - timestampMillis).coerceAtLeast(0L) / 86_400_000.0)
        return when {
            ageDays <= 30.0 -> 1.0
            ageDays >= 180.0 -> 0.0
            else -> (180.0 - ageDays) / 150.0
        }
    }

    private fun strongestComarcaInterest(): Pair<String, Double>? {
        val now = System.currentTimeMillis()
        val grouped = behaviourProfile.events
            .asSequence()
            .filter { it.kind == BehaviourKind.COMARCA && it.value.isNotBlank() }
            .groupBy { normalizeSearchValue(it.value) }
        return grouped.mapNotNull { (_, events) ->
            val last = events.maxByOrNull { it.timestampMillis } ?: return@mapNotNull null
            val decay = behaviourDecay(last.timestampMillis, now)
            if (decay <= 0.0) return@mapNotNull null
            val frequency = 1.0 + ((events.size - 1).coerceIn(0, 5) * 0.12)
            last.value to (decay * frequency).coerceAtMost(1.6)
        }.maxByOrNull { it.second }
    }


    private fun nearbyComarques(comarca: String): Set<String> {
        if (municipalities.isEmpty() || comarca.isBlank()) return emptySet()
        data class Center(val comarca: String, val lat: Double, val lon: Double)
        val centers = municipalities
            .filter { it.comarca.isNotBlank() }
            .groupBy { it.comarca }
            .map { (name, items) -> Center(name, items.map { it.latitude }.average(), items.map { it.longitude }.average()) }
        val origin = centers.firstOrNull { it.comarca.equals(comarca, ignoreCase = true) } ?: return emptySet()
        return centers.asSequence()
            .filterNot { it.comarca.equals(origin.comarca, ignoreCase = true) }
            .map { center ->
                val dLat = center.lat - origin.lat
                val dLon = center.lon - origin.lon
                center.comarca to (dLat * dLat + dLon * dLon)
            }
            .sortedBy { it.second }
            .take(4)
            .map { it.first }
            .toSet()
    }

    private fun parseConcertDate(concert: Concert): LocalDate? =
        runCatching { LocalDate.parse(concert.date.take(10)) }.getOrNull()

    /**
     * Els gustos musicals no es demanen manualment: es dedueixen dels concerts
     * guardats i dels grups seguits.
     */
    fun inferredStylePreferences(): Set<String> {
        val fromSaved = concerts
            .filter { it.id in savedConcerts }
            .flatMap { it.styles }
        val fromFollowed = concerts
            .filter { concert -> concert.lineup.any { it in followedArtists } }
            .flatMap { it.styles }
        val fromAffinities = behaviourProfile.events
            .filter { it.kind == BehaviourKind.STYLE }
            .map { it.value }
        return (fromSaved + fromFollowed + fromAffinities).toSet()
    }

    fun distanceFromPreferredLocations(concert: Concert): Double? {
        if (userPreferences.preferredLocations.isEmpty()) return null
        val destination = municipalityFor(concert.town) ?: return null
        return userPreferences.preferredLocations
            .mapNotNull(::municipalityFor)
            .minOfOrNull { origin ->
                GeoDistance.kilometres(
                    origin.latitude, origin.longitude,
                    destination.latitude, destination.longitude
                )
            }
    }

    /**
     * Un esdeveniment és "meu" si l'usuari l'ha guardat, ha dit que hi anirà
     * o té almenys una entrada. Estos mateixos esdeveniments continuen apareixent
     * també a Per a tu/Llista quan els correspon.
     */
    fun isPersonalEvent(concertId: Int): Boolean =
        concertId in savedConcerts ||
            festivalArtistSelections[concertId].orEmpty().isNotEmpty() ||
            attendanceFor(concertId) == AttendanceStatus.GOING ||
            concertId in ticketPurchases

    fun personalEventLabel(concertId: Int): String = when {
        concertId in ticketPurchases -> "🎟 Tens entrada"
        attendanceFor(concertId) == AttendanceStatus.GOING -> "✓ Hi aniràs"
        concertId in savedConcerts -> "🔖 Guardat"
        festivalArtistSelections[concertId].orEmpty().isNotEmpty() -> "✓ Vull veure"
        else -> ""
    }

    /** Biblioteca d'Entrades: independent del selector del feed i dels seus filtres. */
    fun savedAndTicketConcerts(): List<Concert> = deduplicatedConcertsForFeed()
        .filter { it.id in savedConcerts || it.id in ticketPurchases }
        .sortedWith(compareBy<Concert> { it.date }.thenBy { it.time })

    fun selectedFestivalArtists(concertId: Int): Set<String> = festivalArtistSelections[concertId].orEmpty()

    /** Cada selecció de festival conserva la sessió concreta, separada de Guardar el festival complet. */
    fun allSelectedFestivalSlots(): Map<Int, Set<String>> = festivalArtistSelections


    fun toggleFestivalArtist(concertId: Int, artist: String) {
        val clean = artist.trim()
        if (clean.isBlank()) return
        val current = festivalArtistSelections[concertId].orEmpty()
        val updated = if (current.any { it.equals(clean, ignoreCase = true) }) {
            current.filterNot { it.equals(clean, ignoreCase = true) }.toSet()
        } else current + clean
        festivalArtistSelections = if (updated.isEmpty()) {
            festivalArtistSelections - concertId
        } else festivalArtistSelections + (concertId to updated)
        saveFestivalArtistSelections()
    }

    fun goingConcerts(): List<Concert> = concerts
        .filter { attendanceFor(it.id) == AttendanceStatus.GOING }
        .sortedWith(compareBy<Concert> { it.date }.thenBy { it.time })


    fun concertsForArtistProfile(artist: String): List<Concert> {
        val artistKey = normalizeSearchValue(artist)
        if (artistKey.isBlank()) return emptyList()
        return concerts.filter { concert ->
            concert.lineup.any { normalizeSearchValue(it) == artistKey } ||
                normalizeSearchValue(concert.artist) == artistKey
        }.sortedWith(compareBy<Concert> { it.date }.thenBy { it.time })
    }

    fun publicArtistProfile(artist: String): ArtistPublicProfile =
        DemoData.artistProfiles.firstOrNull { it.artist == artist }
            ?: ArtistPublicProfile(artist, "", concerts.firstOrNull { artist in it.lineup }?.styles?.joinToString(" · ").orEmpty(), "País Valencià")

    fun managementForArtist(artist: String): AppAccount? =
        DemoData.accounts.firstOrNull { account ->
            account.role == AccountRole.AGENCY && artist in account.managedArtists
        }


    fun artistAnalytics(artist: String): ArtistAnalytics {
        val artistConcerts = concerts.filter { artist in it.lineup }
        val ids = artistConcerts.map { it.id }.toSet()
        val baseSeed = (artist.hashCode().toLong() and 0x7fffffff).toInt()
        val savedBase = 80 + (baseSeed % 340)
        val likeBase = 120 + (baseSeed % 620)
        val wantBase = 35 + (baseSeed % 170)
        val goingBase = 20 + (baseSeed % 120)
        val comments = concertComments.count { it.concertId in ids } + chatMessages.count { it.concertId in ids }
        return ArtistAnalytics(
            artist = artist,
            saved = savedBase + ids.count { it in savedConcerts },
            likes = likeBase + ids.count { it in likedConcerts },
            wantToGo = wantBase + ids.count { attendanceFor(it) == AttendanceStatus.WANT_TO_GO },
            going = goingBase + ids.count { attendanceFor(it) == AttendanceStatus.GOING },
            conversations = comments + 20 + (baseSeed % 60),
            topTowns = listOf("València" to 31, "Castelló" to 22, "Vila-real" to 17),
            sharedStyles = artistConcerts.flatMap { it.styles }.distinct().take(3).mapIndexed { index, style -> style to (55 - index * 12) }
        )
    }

    /**
     * Actualitza només els comptadors socials visibles. És una crida molt lleugera
     * i no torna a descarregar ni a reordenar el feed.
     */
    fun refreshSocialCounts(eventIds: Collection<Int>) {
        val ids = eventIds.asSequence().filter { it > 0 }.distinct().take(100).toList()
        if (ids.isEmpty()) return
        if (socialCountsRefreshInFlight) {
            pendingSocialCountIds = (pendingSocialCountIds + ids).take(100).toSet()
            return
        }
        socialCountsRefreshInFlight = true
        // El mateix polling pot haver començat abans que un like/guardat.
        val savedAtRequest = ids.associateWith { savedMutationVersions[it] ?: 0 }
        val likedAtRequest = ids.associateWith { likedMutationVersions[it] ?: 0 }
        backgroundExecutor.execute {
            val result = ServerApiRepository.fetchEventSocialCounts(appContext, ids)
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { counts ->
                    if (counts.isNotEmpty()) {
                        // Merge per camp: un refresh antic no anul·la el toc que acabem de fer.
                        val updates = counts.mapValues { (id, social) ->
                            val existing = socialCountsByEvent[id]
                                ?: concertById(id)?.let { SocialUiCounts(it.savedCount, it.likeCount) }
                                ?: SocialUiCounts(0, 0)
                            val saved = if (savedAtRequest[id] != (savedMutationVersions[id] ?: 0)) existing.saved
                                        else mergeRemoteCount(id, social.savedCount, isSaved = true)
                            val liked = if (likedAtRequest[id] != (likedMutationVersions[id] ?: 0)) existing.liked
                                        else mergeRemoteCount(id, social.likeCount, isSaved = false)
                            SocialUiCounts(saved, liked)
                        }
                        socialCountsByEvent = socialCountsByEvent + updates
                    }
                }
                socialCountsRefreshInFlight = false
                val pending = pendingSocialCountIds
                pendingSocialCountIds = emptySet()
                if (pending.isNotEmpty()) refreshSocialCounts(pending)
            }
        }
    }

    fun savedCountForConcert(id: Int): Int =
        socialCountsByEvent[id]?.saved
            ?: concertById(id)?.savedCount?.coerceAtLeast(0)
            ?: if (id in savedConcerts) 1 else 0

    fun toggleSaved(id: Int) {
        val concert = concerts.firstOrNull { it.id == id } ?: return
        val wasSaved = id in savedConcerts
        val nowSaved = !wasSaved
        savedConcerts = if (nowSaved) savedConcerts + id else savedConcerts - id
        saveIntSet(userCacheKey("saved_concerts"), savedConcerts)

        val current = socialCountsByEvent[id]?.saved ?: concert.savedCount.coerceAtLeast(0)
        val liked = socialCountsByEvent[id]?.liked ?: concert.likeCount.coerceAtLeast(0)
        val optimisticCount = (current + if (nowSaved) 1 else -1).coerceAtLeast(0)
        val revision = (savedMutationVersions[id] ?: 0) + 1
        savedMutationVersions[id] = revision
        savedOptimisticCounts[id] = optimisticCount
        savedOptimisticAt[id] = SystemClock.elapsedRealtime()
        socialCountsByEvent = socialCountsByEvent + (id to SocialUiCounts(
            saved = optimisticCount,
            liked = liked
        ))
        invalidateNotificationCache(250L)
        if (nowSaved) recordBehaviour(BehaviourKind.SAVED_EVENT, id.toString(), concert = concert)

        if (currentAccount == null || ServerApiRepository.token(appContext).isNullOrBlank()) return
        val payload = eventServerPayload(concert).put("saved", nowSaved)
        val accountAtRequest = currentAccount?.serverUserId
        socialActionsExecutor.execute {
            val result = ServerApiRepository.postAuthenticatedJson(appContext, "events/save.php", payload)
            result.onSuccess { remote ->
                val remoteSaved = serverJsonBoolean(remote, "saved", nowSaved)
                val remoteCount = remote.optInt("saved_count", optimisticCount).coerceAtLeast(0)
                Handler(Looper.getMainLooper()).post {
                    if (accountAtRequest != currentAccount?.serverUserId || savedMutationVersions[id] != revision) return@post
                    if (remoteSaved != nowSaved) {
                        enqueuePendingServerPost("events/save.php", payload)
                    } else {
                        val currentLiked = socialCountsByEvent[id]?.liked ?: liked
                        socialCountsByEvent = socialCountsByEvent +
                            (id to SocialUiCounts(mergeRemoteCount(id, remoteCount, isSaved = true), currentLiked))
                    }
                }
            }.onFailure { enqueuePendingServerPost("events/save.php", payload) }
        }
    }

    fun likeCountForConcert(id: Int): Int =
        socialCountsByEvent[id]?.liked
            ?: concertById(id)?.likeCount?.coerceAtLeast(0)
            ?: if (id in likedConcerts) 1 else 0

    fun viewCountForConcert(id: Int): Int =
        180 + ((id * 389) % 4200)

    fun toggleLiked(id: Int) {
        val concert = concerts.firstOrNull { it.id == id } ?: return
        val wasLiked = id in likedConcerts
        val nowLiked = !wasLiked
        likedConcerts = if (nowLiked) likedConcerts + id else likedConcerts - id
        saveIntSet(userCacheKey("liked_concerts"), likedConcerts)

        val saved = socialCountsByEvent[id]?.saved ?: concert.savedCount.coerceAtLeast(0)
        val current = socialCountsByEvent[id]?.liked ?: concert.likeCount.coerceAtLeast(0)
        val optimisticCount = (current + if (nowLiked) 1 else -1).coerceAtLeast(0)
        val revision = (likedMutationVersions[id] ?: 0) + 1
        likedMutationVersions[id] = revision
        likedOptimisticCounts[id] = optimisticCount
        likedOptimisticAt[id] = SystemClock.elapsedRealtime()
        socialCountsByEvent = socialCountsByEvent + (id to SocialUiCounts(
            saved = saved,
            liked = optimisticCount
        ))
        if (currentAccount == null || ServerApiRepository.token(appContext).isNullOrBlank()) return
        val payload = eventServerPayload(concert).put("liked", nowLiked)
        val accountAtRequest = currentAccount?.serverUserId
        socialActionsExecutor.execute {
            val result = ServerApiRepository.postAuthenticatedJson(appContext, "events/like.php", payload)
            result.onSuccess { remote ->
                val remoteLiked = serverJsonBoolean(remote, "liked", nowLiked)
                val remoteCount = remote.optInt("like_count", optimisticCount).coerceAtLeast(0)
                Handler(Looper.getMainLooper()).post {
                    if (accountAtRequest != currentAccount?.serverUserId || likedMutationVersions[id] != revision) return@post
                    if (remoteLiked != nowLiked) {
                        enqueuePendingServerPost("events/like.php", payload)
                    } else {
                        val currentSaved = socialCountsByEvent[id]?.saved ?: saved
                        socialCountsByEvent = socialCountsByEvent +
                            (id to SocialUiCounts(currentSaved, mergeRemoteCount(id, remoteCount, isSaved = false)))
                    }
                }
            }.onFailure { enqueuePendingServerPost("events/like.php", payload) }
        }
    }

    /** Actualitza la campaneta immediatament després de canviar un avís personalitzat. */
    fun reminderConfigurationChanged() { invalidateNotificationCache(0L) }

    fun toggleConcertAlert(id: Int) {
        val concert = concerts.firstOrNull { it.id == id }
        val active = id !in alertConcerts
        alertConcerts = if (active) alertConcerts + id else alertConcerts - id
        saveIntSet(userCacheKey("alert_concerts"), alertConcerts)
        if (concert != null) postServerStateOrQueue("user-state/event-alert.php", eventServerPayload(concert).put("active", active))
    }

    fun submitEventReport(
        concert: Concert,
        reason: String,
        details: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        runCatching {
            backgroundExecutor.execute {
                val result = ServerApiRepository.submitEventReport(
                    context = appContext,
                    eventId = concert.id,
                    eventTitle = concert.title,
                    eventTown = concert.town,
                    eventDate = concert.feedDate,
                    reason = reason,
                    details = details
                )
                Handler(Looper.getMainLooper()).post {
                    result.fold(
                        onSuccess = { onResult(true, null) },
                        onFailure = { onResult(false, it.message ?: "No s’ha pogut enviar la informació.") }
                    )
                }
            }
        }.onFailure { throwable ->
            Handler(Looper.getMainLooper()).post {
                onResult(false, throwable.message ?: "No s’ha pogut iniciar l’enviament.")
            }
        }
    }

    fun reportResultNotificationId(reportId: String, reviewRound: Int): String =
        "report_result_${reportId}_${reviewRound.coerceAtLeast(1)}"

    fun submitReportFollowup(
        report: ServerApiRepository.ReportResultNotification,
        message: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val clean = message.trim()
        if (!report.reviewStatus.equals("DISMISSED", ignoreCase = true)) {
            onResult(false, "Este avís no està pendent de revisió de nou.")
            return
        }
        if (clean.length < 5) {
            onResult(false, "Explica breument què falta o què hauríem de comprovar.")
            return
        }
        backgroundExecutor.execute {
            val result = ServerApiRepository.submitReportFollowup(appContext, report.reportId, clean)
            Handler(Looper.getMainLooper()).post {
                result.fold(
                    onSuccess = {
                        val id = reportResultNotificationId(report.reportId, report.reviewRound)
                        markNotificationRead(id)
                        reportResultNotifications = reportResultNotifications.filterNot {
                            it.reportId == report.reportId && it.reviewRound == report.reviewRound
                        }
                        reportResultPopupState = reportResultPopupState.filterNot {
                            it.reportId == report.reportId && it.reviewRound == report.reviewRound
                        }
                        notificationsCacheReady = false
                        refreshNotificationBadgeAsync(100L)
                        onResult(true, null)
                    },
                    onFailure = { onResult(false, it.message ?: "No s’ha pogut enviar la nova explicació.") }
                )
            }
        }
    }

    fun recordShare(id: Int) {
        val concert = concerts.firstOrNull { it.id == id } ?: return
        syncAffinitySignal("SHARE_EVENT", "EVENT", id.toString(), concert.title, concert)
    }

    fun recordTicketClick(id: Int) {
        val concert = concerts.firstOrNull { it.id == id } ?: return
        syncAffinitySignal("TICKET_CLICK", "EVENT", id.toString(), concert.title, concert)
    }

    private fun recordTicketPurchase(concertId: Int, quantity: Int, dedupeKey: String = "") {
        val concert = concerts.firstOrNull { it.id == concertId } ?: return
        val payload = affinityServerPayload("TICKET_PURCHASE", "EVENT", concertId.toString(), concert.title, concert)
            .put("metadata_json", JSONObject().put("quantity", quantity.coerceAtLeast(1)).toString())
        if (dedupeKey.isNotBlank()) payload.put("dedupe_key", "ticket_purchase:$dedupeKey")
        enqueueAffinitySignal(payload)
    }

    fun toggleFollowArtist(artist: String) {
        val cleanArtist = artist.trim()
        if (cleanArtist.isBlank()) return
        val wasFollowing = cleanArtist in followedArtists
        val nowFollowing = !wasFollowing

        // Resposta visual immediata i barata. No reescrivim l'historial d'afinitats
        // ni invalidem el rànquing complet del feed en cada toc.
        followedArtists = if (nowFollowing) followedArtists + cleanArtist else followedArtists - cleanArtist
        // Deixar de seguir no lleva una campaneta activada expressament.
        // Els dos controls són independents.
        saveStringSet(userCacheKey("followed_artists"), followedArtists)
        invalidateFollowSuggestions()
        applyAutomaticDefaultFeedMode()
        // Les actuacions seguides són fixes en Per a tu; un follow/unfollow
        // canvia immediatament la selecció i no pot reutilitzar el snapshot antic.
        expireForYouSnapshot()
        // La vista materialitzada de «Seguits» s'invalida per un canvi REAL de follows.
        // No depén de recalcular l'algoritme de recomanacions ni de cap timer.
        if (feedMode == FeedMode.FOLLOWED || feedMode == FeedMode.FOR_YOU) {
            scheduleVisibleConcertsRefresh(0L)
        }
        invalidateNotificationCache(250L)

        val payload = JSONObject()
            .put("entity_type", "ARTIST")
            .put("entity_key", cleanArtist)
            .put("entity_label", cleanArtist)
            .put("following", nowFollowing)
        postServerStateOrQueue("user-state/follow.php", payload)
    }

    fun toggleFollowOrganizer(username: String) {
        val display = profileFor(username).displayName.ifBlank { username }
        val wasFollowing = username in followedOrganizers
        val nowFollowing = !wasFollowing
        followedOrganizers = if (nowFollowing) followedOrganizers + username else followedOrganizers - username
        saveStringSet(userCacheKey("followed_organizers"), followedOrganizers)
        applyAutomaticDefaultFeedMode()
        expireForYouSnapshot()
        scheduleVisibleConcertsRefresh(0L)
        invalidateFollowSuggestions()
        invalidateNotificationCache(250L)
        if (nowFollowing) {
            recordBehaviour(BehaviourKind.FOLLOWED_ORGANIZER, display)
        } else {
            behaviourProfile = behaviourProfile.copy(
                events = behaviourProfile.events.filterNot { it.kind == BehaviourKind.FOLLOWED_ORGANIZER && it.value.equals(display, true) },
                updatedAtMillis = System.currentTimeMillis()
            ).also(::saveBehaviourProfile)
        }
        val payload = JSONObject()
            .put("entity_type", "ORGANIZER")
            .put("entity_key", username)
            .put("entity_label", display)
            .put("following", nowFollowing)
        postServerStateOrQueue("user-state/follow.php", payload)
    }

    fun toggleFollowVenue(venue: String) {
        val clean = venue.trim()
        if (clean.isBlank()) return
        val nowFollowing = clean !in followedVenues
        followedVenues = if (nowFollowing) followedVenues + clean else followedVenues - clean
        saveStringSet(userCacheKey("followed_venues"), followedVenues)
        applyAutomaticDefaultFeedMode()
        expireForYouSnapshot()
        scheduleVisibleConcertsRefresh(0L)
        invalidateNotificationCache(250L)
        val payload = JSONObject()
            .put("entity_type", "VENUE")
            .put("entity_key", clean)
            .put("entity_label", clean)
            .put("following", nowFollowing)
        postServerStateOrQueue("user-state/follow.php", payload)
        scheduleGuestStateSync()
    }

    fun categoryFollowsAll(category: CulturalCategory): Boolean = category.name in followedCategories

    fun categoryFollowsSubcategory(sub: CulturalSubcategory): Boolean =
        categoryFollowsAll(sub.category) || sub.followKey in followedCategories

    fun followedSubcategoryCount(category: CulturalCategory): Int =
        CategoryTaxonomy.childrenOf(category).count(::categoryFollowsSubcategory)

    /** Compatibilitat amb el servidor: tant la categoria com els seus subnivells són follows de tipus CATEGORY. */
    private fun updateCategoryFollows(next: Set<String>) {
        val previous = followedCategories
        if (previous == next) return
        followedCategories = CategoryTaxonomy.canonicalizeFollowSet(next)
        saveStringSet(userCacheKey("followed_categories"), next)
        applyAutomaticDefaultFeedMode()
        (previous - next).forEach { key -> syncCategoryFollow(key, false) }
        (next - previous).forEach { key -> syncCategoryFollow(key, true) }
        expireForYouSnapshot()
        invalidateFeedCache()
        scheduleVisibleConcertsRefresh(0L)
        invalidateFollowSuggestions()
        invalidateNotificationCache(250L)
    }

    private fun syncCategoryFollow(key: String, following: Boolean) {
        val label = if (CategoryTaxonomy.isAnySubcategoryKey(key)) {
            CategoryTaxonomy.findByCode(key)?.label ?: key
        } else CulturalCategory.entries.firstOrNull { it.name == key }?.label ?: key
        postServerStateOrQueue("user-state/follow.php", JSONObject()
            .put("entity_type", "CATEGORY")
            .put("entity_key", key)
            .put("entity_label", label)
            .put("following", following))
        // Si venia de la v242, deixar de seguir ha d'eliminar TAMBÉ la clau
        // vella del servidor; d'altra manera reapareixeria en el següent login.
        if (!following) CategoryTaxonomy.findByCode(key)?.legacyFollowKey?.let { oldKey ->
            postServerStateOrQueue("user-state/follow.php", JSONObject()
                .put("entity_type", "CATEGORY")
                .put("entity_key", oldKey)
                .put("entity_label", label)
                .put("following", false))
        }
    }

    /** v294: guarda els gustos elegits al formulari inicial en les mateixes
     * claus de Personalització/Seguits que sincronitza user-state/follow.php.
     * Sense sessió es conserven per al merge guest->compte.
     */
    fun applyFirstRunChoices(
        disciplines: Set<CulturalCategory>,
        subcategoryCodes: Set<String>,
        locationRadii: Map<String, Int>,
        artists: Set<String>
    ) {
        firstRunChoicesRevision += 1L
        updateCategoryFollows(com.example.agendavalenciana.model.FirstRunOnboardingPolicy
            .followKeys(disciplines, subcategoryCodes))
        val cleanLocations = locationRadii.entries.mapNotNull { (town, radius) ->
            town.trim().takeIf(String::isNotBlank)?.let { it to PersonalizationOptions.validRadius(radius) }
        }.toMap()
        updateUserPreferences(userPreferences.copy(
            preferredLocations = userPreferences.preferredLocations + cleanLocations.keys,
            locationRadiiKm = userPreferences.locationRadiiKm + cleanLocations
        ))
        val desiredArtists = artists.map(String::trim).filter(String::isNotBlank).toSet()
        (desiredArtists - followedArtists).forEach(::toggleFollowArtist)
        // La funció no esborra altres seguiments previs, especialment en comptes ja existents.
        scheduleGuestStateSync()
    }

    fun toggleFollowCategory(category: CulturalCategory) {
        if (category in blockedCategories) unblockCategory(category)
        if (blockedSubcategories.isNotEmpty()) {
            CategoryTaxonomy.childrenOf(category).forEach { sub ->
                if (sub.code in blockedSubcategories) unblockSubcategory(sub)
            }
        }
        val hadFull = categoryFollowsAll(category)
        val childKeys = CategoryTaxonomy.childrenOf(category).map { it.followKey }.toSet()
        val withoutBranch = followedCategories - category.name - childKeys
        updateCategoryFollows(if (hadFull) withoutBranch else withoutBranch + category.name)
        if (!hadFull) recordBehaviour(BehaviourKind.FOLLOWED_CATEGORY, category.name)
        else behaviourProfile = behaviourProfile.copy(
            events = behaviourProfile.events.filterNot {
                it.kind == BehaviourKind.FOLLOWED_CATEGORY && it.value.equals(category.name, true)
            }, updatedAtMillis = System.currentTimeMillis()
        ).also(::saveBehaviourProfile)
    }

    /** Seleccionar un subnivell no obliga a seguir la categoria completa. */
    fun toggleFollowSubcategory(sub: CulturalSubcategory) {
        if (sub.code in blockedSubcategories) unblockSubcategory(sub)
        if (sub.category in blockedCategories) unblockCategory(sub.category)
        val allChildren = CategoryTaxonomy.childrenOf(sub.category)
        val next = if (categoryFollowsAll(sub.category)) {
            // Quan seguia tota la categoria, desmarcar-ne un subnivell deixa els altres quatre marcats.
            (followedCategories - sub.category.name) +
                allChildren.filterNot { it.followKey == sub.followKey }.map { it.followKey }
        } else if (sub.followKey in followedCategories) {
            followedCategories - sub.followKey
        } else {
            followedCategories + sub.followKey
        }
        val selectedChildren = allChildren.count { it.followKey in next }
        // Cinc de cinc és equivalent a seguir la categoria sencera.
        val canonical = if (selectedChildren == allChildren.size) {
            (next - allChildren.map { it.followKey }.toSet()) + sub.category.name
        } else next
        updateCategoryFollows(canonical)
    }

    /** Categoria principal, subcategoria editorial o públic infantil transversal. */
    private fun followsConcertCategory(concert: Concert): Boolean {
        if (concert.category.name in followedCategories) return true
        if (CulturalCategory.CHILDREN.name in followedCategories && CategoryTaxonomy.isInfantil(concert)) return true
        return CategoryTaxonomy.childrenOf(concert.category).any { sub ->
            sub.followKey in followedCategories && CategoryTaxonomy.matches(concert, sub)
        }
    }

    /**
     * Dues propostes estables per convertir afinitats temporals en seguiments permanents.
     * Prioritza cerques/interaccions reals; si falta una segona proposta, usa interessos
     * generals amb oferta pròxima. Les impressions penalitzen lleument la repetició.
     */
    fun rotateFollowSuggestions() {
        // Rotació compartida entre les files: evita propostes repetides quan n'hi ha dos.
        val key = userCacheKey("follow_suggestion_rotation")
        store.edit().putInt(key, (store.getInt(key, 0).coerceAtLeast(0) + 1) % 100_000).apply()
        // La baralla de candidats no canvia fins que canvien les afinitats o les dades.
        // Només avancem la pàgina, sense reconstruir el feed ni repetir cartes recents.
        cachedFollowSuggestionsBySlot.clear()
        followSuggestionRevision++
    }

    fun followSuggestions(slot: Int = 0, limit: Int = 2): List<FollowSuggestion> {
        if (limit <= 0) return emptyList()
        val epoch = loadForYouSnapshotTimestamp()
        if (cachedFollowSuggestionsEpoch != epoch) {
            cachedFollowSuggestionsEpoch = epoch
            cachedFollowSuggestionsBySlot.clear()
        }
        val revisionAtStart = followSuggestionRevision
        val rotation = store.getInt(userCacheKey("follow_suggestion_rotation"), 0).coerceAtLeast(0)
        val stableKey = "${agendaToday()}:${slot.coerceAtLeast(0)}:${limit.coerceAtLeast(1)}:$rotation:$revisionAtStart"
        cachedFollowSuggestionsBySlot[stableKey]?.let { return it }
        val deckKey = "${userCacheKey("follow_suggestion_deck")}:${loadForYouSnapshotTimestamp()}:${agendaToday()}:$feedDataRevision:$followSuggestionPoolRevision"
        cachedFollowSuggestionPairs[deckKey]?.let { deck ->
            return FollowSuggestionPaging.inSlot(deck, slot, rotation, limit)
        }
        val now = System.currentTimeMillis()
        val aggregated = aggregateBehaviour(now)
        val followedArtistKeys = followedArtists.map(::normalizeSearchValue).toSet()
        val followedOrganizerKeys = followedOrganizers.map(::normalizeSearchValue).toSet()
        val followedCategoryKeys = followedCategories.map(::normalizeSearchValue).toSet()
        val suggestionEvents = concerts.asSequence()
            .filter { isCurrentOrFutureAgendaEvent(it) && isEventVisibleForUser(it) }
            .filter(::insidePreferredForYouGeography)
            .filter { followedCategories.isEmpty() || followsConcertCategory(it) }
            .toList()
        val relevantArtists = suggestionEvents.asSequence().flatMap { eventArtistKeys(it).asSequence() }.toSet()
        val relevantOrganizers = suggestionEvents.asSequence()
            .flatMap { (it.coEditorUsernames + it.organizers).asSequence() }
            .map(::normalizeSearchValue).filter(String::isNotBlank).toSet()
        data class Candidate(val suggestion: FollowSuggestion, val normalized: String)
        val candidates = linkedMapOf<String, Candidate>()

        fun impressionPenalty(type: FollowSuggestionType, key: String): Int {
            val count = store.getInt(followSuggestionImpressionKey(type, key), 0).coerceAtMost(8)
            return count * 14
        }
        fun put(type: FollowSuggestionType, key: String, label: String, subtitle: String, score: Int) {
            if (key.isBlank() || label.isBlank()) return
            val normalizedKey = normalizeSearchValue(key)
            val isExplicitRecentSearch = subtitle.startsWith("Cerca recent")
            // No suggerir categories generals o artistes d'una altra disciplina
            // o d'un altre territori per simple popularitat. Una cerca expressa
            // de l'usuari sí pot obrir interessos nous.
            if (type == FollowSuggestionType.CATEGORY && !isExplicitRecentSearch) return
            if (!isExplicitRecentSearch && when (type) {
                    FollowSuggestionType.ARTIST -> normalizedKey !in relevantArtists
                    FollowSuggestionType.ORGANIZER -> normalizedKey !in relevantOrganizers
                    FollowSuggestionType.CATEGORY -> false
                }) return
            if (when (type) {
                    FollowSuggestionType.ARTIST -> normalizedKey in followedArtistKeys
                    FollowSuggestionType.ORGANIZER -> normalizedKey in followedOrganizerKeys
                    FollowSuggestionType.CATEGORY -> normalizedKey in followedCategoryKeys
                }) return
            val normalized = normalizeSearchValue(label)
            val mapKey = "${type.name}:$normalized"
            val adjusted = score - impressionPenalty(type, key)
            val old = candidates[mapKey]
            if (old == null || adjusted > old.suggestion.score) {
                candidates[mapKey] = Candidate(FollowSuggestion(type, key, label, subtitle, adjusted), normalized)
            }
        }

        aggregated.forEach { signal ->
            val decay = behaviourDecay(signal.lastTimestamp, now)
            if (decay <= 0.0) return@forEach
            val frequency = 1.0 + ((signal.count - 1).coerceIn(0, 6) * 0.18)
            val base = (decay * frequency * 100.0).roundToInt()
            when (signal.kind) {
                BehaviourKind.ARTIST -> {
                    val artist = knownArtistNames().firstOrNull { normalizeSearchValue(it) == signal.value } ?: return@forEach
                    if (artist !in followedArtists) put(FollowSuggestionType.ARTIST, artist, artist, "Grup / artista", 115 + base)
                }
                BehaviourKind.ORGANIZER -> {
                    organizerCandidateFor(signal.value)?.let { (username, display) ->
                        if (username !in followedOrganizers) put(FollowSuggestionType.ORGANIZER, username, display, "Organitzador / programador", 105 + base)
                    }
                }
                BehaviourKind.CATEGORY -> {
                    CulturalCategory.entries.firstOrNull { normalizeSearchValue(it.name) == signal.value || normalizeSearchValue(it.label) == signal.value }?.let { category ->
                        if (category.name !in followedCategories && category != CulturalCategory.OTHER) {
                            put(FollowSuggestionType.CATEGORY, category.name, category.label, "Interés", 88 + base)
                        }
                    }
                }
                BehaviourKind.EVENT_VIEW, BehaviourKind.SAVED_EVENT, BehaviourKind.WANT_TO_GO, BehaviourKind.GOING -> {
                    val source = signal.value.toIntOrNull()?.let { id -> concerts.firstOrNull { it.id == id } } ?: return@forEach
                    val eventBoost = when (signal.kind) {
                        BehaviourKind.EVENT_VIEW -> 30
                        BehaviourKind.SAVED_EVENT -> 62
                        BehaviourKind.WANT_TO_GO -> 78
                        BehaviourKind.GOING -> 92
                        else -> 0
                    }
                    source.lineup.take(2).forEach { artist ->
                        if (artist.isNotBlank() && artist !in followedArtists) put(FollowSuggestionType.ARTIST, artist, artist, "Grup / artista", eventBoost + base / 2)
                    }
                    source.coEditorUsernames.take(1).forEach { username ->
                        if (username.isNotBlank() && username !in followedOrganizers) {
                            val display = profileFor(username).displayName.ifBlank { username }
                            put(FollowSuggestionType.ORGANIZER, username, display, "Organitzador / programador", eventBoost - 5 + base / 2)
                        }
                    }
                    if (source.category != CulturalCategory.OTHER && source.category.name !in followedCategories) {
                        put(FollowSuggestionType.CATEGORY, source.category.name, source.category.label, "Interés", eventBoost - 12 + base / 2)
                    }
                }
                else -> Unit
            }
        }

        // v260: les cerques recents alimenten DIRECTAMENT els suggeriments de seguir,
        // tant si són cerques lliures com si s'han fet sense iniciar sessió.
        // No cal esperar a la pròxima reconstrucció del rànquing de «Per a tu».
        val artistNamesByKey = knownArtistNames().associateBy(::normalizeSearchValue)
        val categoryByKey = CulturalCategory.entries
            .filter { it != CulturalCategory.OTHER }
            .flatMap { category -> listOf(normalizeSearchValue(category.name) to category,
                normalizeSearchValue(category.label) to category) }.toMap()
        recentSearchTerms.asSequence().map(::normalizeSearchValue)
            .filter { it.length >= 2 }.distinct().take(35).forEach { term ->
                artistNamesByKey[term]?.let { artist ->
                    put(FollowSuggestionType.ARTIST, artist, artist, "Cerca recent · artista", 160)
                }
                categoryByKey[term]?.let { category ->
                    put(FollowSuggestionType.CATEGORY, category.name, category.label, "Cerca recent · disciplina", 125)
                }
                organizerCandidateFor(term)?.let { (username, display) ->
                    put(FollowSuggestionType.ORGANIZER, username, display, "Cerca recent · organitzador", 130)
                }
            }

        // Fallback específic de grups/artistes: encara que l'usuari no haja generat
        // prou senyals de comportament, el feed ha d'oferir també perfils musicals
        // amb presència real en l'agenda, no només categories.
        suggestionEvents.asSequence()
            .flatMap { it.lineup.asSequence() }
            .map(String::trim)
            .filter(String::isNotBlank)
            .filter { it !in followedArtists }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key.lowercase() })
            .take(24)
            .forEach { (artist, eventCount) ->
                put(
                    FollowSuggestionType.ARTIST,
                    artist,
                    artist,
                    "Grup / artista",
                    34 + eventCount.coerceAtMost(12) * 3
                )
            }

        // Sense fallback global de disciplines: les propostes només deriven de
        // preferències, accions o cerques expressament fetes per l'usuari.

        val sorted = candidates.values.map { it.suggestion }
            .filter { it.score > -80 }
            .sortedWith(compareByDescending<FollowSuggestion> { it.score }.thenBy { it.label.lowercase() })

        // Aleatorietat estable per cada snapshot: no canviem les targetes a cada scroll.
        // Les opcions de millor puntuació continuen tenint prioritat, sense imposar
        // «1 artista + 1 categoria». Poden eixir dos artistes i MAI dos categories.
        // El mateix ordre s'utilitza en tots els slots per evitar duplicats entre files.
        val randomSeed = epoch xor agendaToday().toEpochDay() xor 0x5DEECE66DL
        val randomized = sorted.take(24).shuffled(Random(randomSeed)) + sorted.drop(24)
        // Fem parelles reals (màxim una categoria per parella) i avancem de
        // pàgina en cada toc. El lot de 2 files comparteix pàgina i no es duplica.
        val pairs = FollowSuggestionPaging.pairs(randomized) { it.type == FollowSuggestionType.CATEGORY }
        val deck = cachedFollowSuggestionPairs.putIfAbsent(deckKey, pairs) ?: pairs
        val stable = FollowSuggestionPaging.inSlot(deck, slot, rotation, limit)
        if (revisionAtStart == followSuggestionRevision) cachedFollowSuggestionsBySlot[stableKey] = stable
        return stable
    }

    /** Candidat de relleu per a UNA posició: respecta la targeta veïna. */
    fun nextFollowSuggestion(slot: Int, excluded: Set<String>, position: Int): FollowSuggestion? {
        // La crida també assegura que hi haja una baralla calculada per a les
        // afinitats actuals, sense fer girar els altres mòduls del feed.
        followSuggestions(slot, limit = 2)
        val deckKey = "${userCacheKey("follow_suggestion_deck")}:${loadForYouSnapshotTimestamp()}:${agendaToday()}:$feedDataRevision:$followSuggestionPoolRevision"
        val deck = cachedFollowSuggestionPairs[deckKey].orEmpty()
        val excludedNormalized = excluded.mapNotNull { value ->
            val type = value.substringBefore(':', missingDelimiterValue = "")
            val key = value.substringAfter(':', missingDelimiterValue = "")
            if (type.isBlank() || key.isBlank()) null else "$type:${normalizeSearchValue(key)}"
        }.toSet()
        val alreadyCategory = excludedNormalized.any { it.startsWith("CATEGORY:") }
        // Primer la pàgina següent, per a no repetir la mateixa proposta.
        val start = (slot.coerceAtLeast(0) + position.coerceAtLeast(0) + 2) % deck.size.coerceAtLeast(1)
        val candidates = (deck.drop(start) + deck.take(start)).flatten()
        return FollowSuggestionPaging.replacement(
            candidates = candidates,
            excludedKeys = excludedNormalized,
            keyOf = { "${it.type}:${normalizeSearchValue(it.key)}" },
            isCategory = { it.type == FollowSuggestionType.CATEGORY },
            isFollowing = ::isFollowingSuggestion,
            categoryAlreadyVisible = alreadyCategory
        )
    }

    fun isFollowingSuggestion(item: FollowSuggestion): Boolean = when (item.type) {
        FollowSuggestionType.ARTIST -> followedArtists.any { normalizeSearchValue(it) == normalizeSearchValue(item.key) }
        FollowSuggestionType.ORGANIZER -> followedOrganizers.any { normalizeSearchValue(it) == normalizeSearchValue(item.key) }
        FollowSuggestionType.CATEGORY -> followedCategories.any { normalizeSearchValue(it) == normalizeSearchValue(item.key) }
    }

    fun markFollowSuggestionsShown(items: List<FollowSuggestion>) {
        items.forEach { item ->
            val key = followSuggestionImpressionKey(item.type, item.key)
            store.edit().putInt(key, store.getInt(key, 0) + 1).apply()
        }
    }

    fun followSuggestion(item: FollowSuggestion) {
        if (isFollowingSuggestion(item)) return
        when (item.type) {
            FollowSuggestionType.ARTIST -> toggleFollowArtist(item.key)
            FollowSuggestionType.ORGANIZER -> toggleFollowOrganizer(item.key)
            FollowSuggestionType.CATEGORY -> CulturalCategory.entries.firstOrNull { it.name.equals(item.key, true) }?.let(::toggleFollowCategory)
        }
    }

    private fun organizerCandidateFor(normalizedValue: String): Pair<String, String>? {
        val usernames = buildSet {
            concerts.forEach { addAll(it.coEditorUsernames) }
            DemoData.accounts.filter { it.role in setOf(AccountRole.AGENCY, AccountRole.LABEL, AccountRole.VENUE, AccountRole.PROMOTER) }
                .forEach { add(it.username) }
        }
        return usernames.firstNotNullOfOrNull { username ->
            val display = profileFor(username).displayName.ifBlank { username }
            if (normalizeSearchValue(username) == normalizedValue || normalizeSearchValue(display) == normalizedValue) username to display else null
        }
    }

    private fun candidateKey(item: FollowSuggestion): String = "${item.type}:${normalizeSearchValue(item.key)}"

    private fun followSuggestionImpressionKey(type: FollowSuggestionType, key: String): String =
        userCacheKey("follow_suggestion_${type.name.lowercase()}_${normalizeSearchValue(key)}")

    /** La campaneta és un consentiment explícit i independent de «Seguir».
     *  La freqüència és específica de cada artista/grup i queda aïllada per compte.
     *  Els avisos Android fora de l'app necessiten un emissor push del servidor.
     */
    private fun artistBellFrequencyKey(artist: String, username: String? = currentAccount?.username): String =
        userCacheKey("artist_bell_frequency_${normalizeSearchValue(artist)}", username)

    fun artistAlertFrequency(artist: String): BellFollowedFrequency = runCatching {
        BellFollowedFrequency.valueOf(
            store.getString(artistBellFrequencyKey(artist), BellFollowedFrequency.WEEKLY.name).orEmpty()
        )
    }.getOrDefault(BellFollowedFrequency.WEEKLY)

    fun configureArtistAlert(artist: String, enabled: Boolean, frequency: BellFollowedFrequency = BellFollowedFrequency.WEEKLY) {
        val cleaned = artist.trim()
        if (cleaned.isBlank()) return
        artistAlerts = if (enabled) artistAlerts + cleaned else artistAlerts - cleaned
        saveStringSet(userCacheKey("artist_alerts"), artistAlerts)
        store.edit().apply {
            if (enabled) putString(artistBellFrequencyKey(cleaned), frequency.name)
            else remove(artistBellFrequencyKey(cleaned))
        }.apply()
        invalidateNotificationCache(0L)
    }

    fun toggleArtistAlert(artist: String) {
        val alreadyEnabled = artist in artistAlerts
        configureArtistAlert(artist, !alreadyEnabled, artistAlertFrequency(artist))
    }

    /**
     * Si hi ha diversos artistes, bloquejar-ne un no fa desaparéixer el cartell sencer:
     * només s'oculta quan tots els artistes identificables estan bloquejats.
     * Les activitats sense artista real continuen depenent d'esdeveniment/categoria.
     */
    fun hideableArtists(concert: Concert): List<String> = UserVisibilityRules.artists(concert)

    fun isArtistBlocked(artist: String): Boolean = UserVisibilityRules.artistIsBlocked(artist, blockedArtists)

    fun isEventVisibleForUser(concert: Concert): Boolean = UserVisibilityRules.isVisible(
        concert, hiddenConcertIds, blockedCategories, blockedArtists
    ) && blockedSubcategories.none { key ->
        CategoryTaxonomy.findByCode(key)?.let { CategoryTaxonomy.matches(concert, it) } == true
    }

    fun isSubcategoryBlocked(sub: CulturalSubcategory): Boolean = sub.code in blockedSubcategories

    fun blockSubcategory(sub: CulturalSubcategory) {
        if (sub.code in blockedSubcategories) return
        // Evitar marcar i blocar al mateix temps la mateixa subcategoria.
        if (categoryFollowsSubcategory(sub)) toggleFollowSubcategory(sub)
        blockedSubcategories = blockedSubcategories + sub.code
        saveStringSet(userCacheKey("blocked_subcategories"), blockedSubcategories)
        syncVisibilityChange("CATEGORY", sub.code, "add")
        onVisibilityExclusionsChanged()
    }

    fun unblockSubcategory(sub: CulturalSubcategory) {
        blockedSubcategories = blockedSubcategories - sub.code
        saveStringSet(userCacheKey("blocked_subcategories"), blockedSubcategories)
        syncVisibilityChange("CATEGORY", sub.code, "remove")
        onVisibilityExclusionsChanged()
    }

    private fun onVisibilityExclusionsChanged() {
        // L'exclusió expressa té efecte en la UI en el mateix fotograma: la
        // reconstrucció i la sincronització del servidor es fan després.
        visibleConcertsState = safeVisibleFeed(visibleConcertsState)
        cachedVisibleConcerts = safeVisibleFeed(cachedVisibleConcerts)
        cachedVisibleConcertsKey = null
        feedRenderRevision += 1L
        invalidateFollowSuggestions()
        scheduleVisibleConcertsRefresh(0L)
    }

    fun hideConcert(concertId: Int) {
        if (concertId <= 0) return
        hiddenConcertIds = hiddenConcertIds + concertId
        saveIntSet(userCacheKey("hidden_concerts"), hiddenConcertIds)
        syncVisibilityChange("EVENT", concertId.toString(), "add")
        onVisibilityExclusionsChanged()
    }

    fun unhideConcert(concertId: Int) {
        hiddenConcertIds = hiddenConcertIds - concertId
        saveIntSet(userCacheKey("hidden_concerts"), hiddenConcertIds)
        syncVisibilityChange("EVENT", concertId.toString(), "remove")
        onVisibilityExclusionsChanged()
    }

    fun blockCategory(category: CulturalCategory) {
        // Excloure no pot coexistir amb un seguiment actiu.
        val keysToClear = followedCategories.filter { key ->
            key == category.name || CategoryTaxonomy.isSubcategoryFollowKey(key, category)
        }.toSet()
        if (keysToClear.isNotEmpty()) updateCategoryFollows(followedCategories - keysToClear)
        blockedCategories = blockedCategories + category
        saveStringSet(userCacheKey("blocked_categories"), blockedCategories.map { it.name }.toSet())
        syncVisibilityChange("CATEGORY", category.name, "add")
        onVisibilityExclusionsChanged()
    }

    fun unblockCategory(category: CulturalCategory) {
        blockedCategories = blockedCategories - category
        saveStringSet(userCacheKey("blocked_categories"), blockedCategories.map { it.name }.toSet())
        syncVisibilityChange("CATEGORY", category.name, "remove")
        onVisibilityExclusionsChanged()
    }

    fun blockArtist(artist: String) {
        val clean = artist.trim()
        if (clean.isBlank()) return
        blockedArtists = blockedArtists + clean
        // Un bloqueig de visibilitat no hauria de crear seguiments nous.
        val previouslyFollowed = followedArtists.firstOrNull { normalizeSearchValue(it) == normalizeSearchValue(clean) }
        if (previouslyFollowed != null) {
            followedArtists = followedArtists - previouslyFollowed
            saveStringSet(userCacheKey("followed_artists"), followedArtists)
            val payload = JSONObject().put("entity_type", "ARTIST").put("entity_key", previouslyFollowed)
                .put("entity_label", previouslyFollowed).put("following", false)
            postServerStateOrQueue("user-state/follow.php", payload)
        }
        // No generar tampoc avisos personalitzats d'artistes exclosos.
        artistAlerts = artistAlerts.filterNot { normalizeSearchValue(it) == normalizeSearchValue(clean) }.toSet()
        saveStringSet(userCacheKey("artist_alerts"), artistAlerts)
        saveStringSet(userCacheKey("blocked_artists"), blockedArtists)
        syncVisibilityChange("ARTIST", clean, "add")
        onVisibilityExclusionsChanged()
    }

    fun unblockArtist(artist: String) {
        blockedArtists = blockedArtists.filterNot { normalizeSearchValue(it) == normalizeSearchValue(artist) }.toSet()
        saveStringSet(userCacheKey("blocked_artists"), blockedArtists)
        syncVisibilityChange("ARTIST", artist, "remove")
        onVisibilityExclusionsChanged()
    }

    private fun syncVisibilityChange(kind: String, target: String, action: String) {
        // La cua remota és per usuari; offline la cache local conserva l'exclusió.
        postServerStateOrQueue("visibility/index.php", JSONObject()
            .put("action", action).put("kind", kind).put("target", target))
    }

    /** Les llistes guardades són independents de «Per a tu» i dels seguiments. */
    private fun filterPresetStoreKey(): String = userCacheKey("saved_search_filters_v290")

    private fun encodePresetFilters(value: Filters): JSONObject = JSONObject().apply {
        put("categories", JSONArray().apply { value.categories.forEach { put(it.name) } })
        value.dateFrom?.let { put("dateFrom", it) }
        value.dateTo?.let { put("dateTo", it) }
        value.month?.let { put("month", it) }
        value.comarca?.let { put("comarca", it) }
        value.town?.let { put("town", it) }
        value.maxDistanceKm?.let { put("maxDistanceKm", it) }
        value.maxPrice?.let { put("maxPrice", it) }
        put("freeOnly", value.freeOnly)
    }

    private fun decodePresetFilters(json: JSONObject): Filters {
        val rawCategories = json.optJSONArray("categories") ?: JSONArray()
        val categories = (0 until rawCategories.length()).mapNotNull { index ->
            val name = rawCategories.optString(index)
            enumValues<CulturalCategory>().firstOrNull { it.name == name }
        }.toSet()
        fun text(key: String) = json.optString(key).takeIf { it.isNotBlank() }
        fun integer(key: String) = if (json.has(key) && !json.isNull(key))
            json.optInt(key).takeIf { it >= 0 } else null
        return Filters(
            categories = categories,
            dateFrom = text("dateFrom"), dateTo = text("dateTo"), month = text("month"),
            comarca = text("comarca"), town = text("town"),
            maxDistanceKm = integer("maxDistanceKm"), maxPrice = integer("maxPrice"),
            freeOnly = json.optBoolean("freeOnly", false)
        )
    }

    private fun loadSavedFilterPresets(): List<SavedFilterPreset> = runCatching {
        val array = JSONArray(store.getString(filterPresetStoreKey(), "[]") ?: "[]")
        (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val name = item.optString("name").trim().takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val criteria = item.optJSONObject("filters") ?: return@mapNotNull null
            SavedFilterPreset(name, decodePresetFilters(criteria))
        }.distinctBy { it.name.lowercase(Locale.ROOT) }
    }.getOrDefault(emptyList())

    private fun persistSavedFilterPresets() {
        val array = JSONArray().apply {
            savedFilterPresets.forEach { preset ->
                put(JSONObject().put("name", preset.name).put("filters", encodePresetFilters(preset.filters)))
            }
        }
        store.edit().putString(filterPresetStoreKey(), array.toString()).apply()
    }

    /**
     * v291: llista remota = font de veritat; la cache local només serveix per mostrar
     * immediatament l'última lectura confirmada, no per donar un fals "guardat".
     * La cua única serialitza GET/UPSERT/DELETE i evita que GET vell xafe una escriptura.
     */
    fun refreshFilterPresetsFromServer() {
        val accountName = currentAccount?.username ?: return
        val tokenAtStart = ServerApiRepository.token(appContext) ?: return
        filterPresetsExecutor.execute {
            if (ServerApiRepository.token(appContext) != tokenAtStart) return@execute
            val result = ServerApiRepository.fetchFilterPresets(tokenAtStart)
            if (result.isFailure) {
                Handler(Looper.getMainLooper()).post {
                    if (currentAccount?.username == accountName && ServerApiRepository.token(appContext) == tokenAtStart)
                        savedFilterSyncError = "No s'han pogut llegir les llistes del servidor: ${result.exceptionOrNull()?.message.orEmpty()}"
                }
                return@execute
            }
            val fromServer = decodeRemoteFilterPresets(result.getOrThrow())
            val migrationKey = userCacheKey("saved_search_filters_migrated_v291", accountName)
            var merged = fromServer
            // Migrar una vegada els guardats locals de v290 sense substituir els que
            // ja existisquen al servidor. Les operacions són UPSERT idempotents.
            if (!store.getBoolean(migrationKey, false)) {
                // Una foto immutable de les llistes v290 evita perdre les que
                // fallen en una migració parcial, fins i tot després d'altres canvis.
                val legacyKey = userCacheKey("saved_search_filters_legacy_import_v291", accountName)
                if (!store.contains(legacyKey)) {
                    val oldRaw = store.getString(userCacheKey("saved_search_filters_v290", accountName), "[]") ?: "[]"
                    store.edit().putString(legacyKey, oldRaw).commit()
                }
                val pending = loadSavedFilterPresetsForAccount(accountName, legacyKey).filterNot { local ->
                    fromServer.any { it.name.equals(local.name, ignoreCase = true) }
                }
                var migrated = true
                pending.forEach { item ->
                    val outcome = ServerApiRepository.saveFilterPresetOnServer(
                        tokenAtStart, item.name, encodePresetFilters(item.filters)
                    )
                    if (outcome.isSuccess) merged = merged + item else migrated = false
                }
                if (migrated) store.edit().putBoolean(migrationKey, true).apply()
                else {
                    Handler(Looper.getMainLooper()).post {
                        if (currentAccount?.username == accountName && ServerApiRepository.token(appContext) == tokenAtStart)
                            savedFilterSyncError = "No s'han pogut importar totes les llistes locals al servidor."
                    }
                }
            }
            val resolved = merged.distinctBy { it.name.lowercase(Locale.ROOT) }
                .sortedBy { it.name.lowercase(Locale.ROOT) }
            Handler(Looper.getMainLooper()).post {
                if (currentAccount?.username != accountName || ServerApiRepository.token(appContext) != tokenAtStart) return@post
                savedFilterPresets = resolved
                persistSavedFilterPresets()
                if (activeFilterPresetName != null && resolved.none { it.name.equals(activeFilterPresetName, true) })
                    activeFilterPresetName = null
                if (store.getBoolean(migrationKey, false)) savedFilterSyncError = null
            }
        }
    }

    private fun loadSavedFilterPresetsForAccount(name: String, key: String = userCacheKey("saved_search_filters_v290", name)): List<SavedFilterPreset> = runCatching {
        val raw = store.getString(key, "[]") ?: "[]"
        decodeRemoteFilterPresets(JSONArray(raw))
    }.getOrDefault(emptyList())

    private fun decodeRemoteFilterPresets(array: JSONArray): List<SavedFilterPreset> =
        (0 until array.length()).mapNotNull { index ->
            val row = array.optJSONObject(index) ?: return@mapNotNull null
            val name = row.optString("name").trim().takeIf(String::isNotBlank) ?: return@mapNotNull null
            val filters = row.optJSONObject("filters") ?: return@mapNotNull null
            SavedFilterPreset(name, decodePresetFilters(filters))
        }.distinctBy { it.name.lowercase(Locale.ROOT) }

    /** Guarda al servidor. Només comunica èxit quan MariaDB ha confirmat el commit. */
    fun saveFilterPreset(name: String, value: Filters, onResult: (Boolean) -> Unit = {}) {
        val clean = name.trim().take(52)
        val accountName = currentAccount?.username
        val tokenAtStart = ServerApiRepository.token(appContext)
        if (clean.isBlank() || value.isEmpty() || accountName == null || tokenAtStart.isNullOrBlank()) {
            savedFilterSyncError = if (accountName == null) "Inicia sessió per guardar les llistes en el teu compte."
                else "Escriu un nom i tria almenys un filtre."
            onResult(false)
            return
        }
        if (isFilterPresetSaving) return
        isFilterPresetSaving = true
        savedFilterSyncError = null
        filterPresetsExecutor.execute {
            val result = if (ServerApiRepository.token(appContext) == tokenAtStart)
                ServerApiRepository.saveFilterPresetOnServer(tokenAtStart, clean, encodePresetFilters(value))
            else Result.failure(IllegalStateException("La sessió ha canviat."))
            Handler(Looper.getMainLooper()).post {
                if (currentAccount?.username != accountName || ServerApiRepository.token(appContext) != tokenAtStart) return@post
                isFilterPresetSaving = false
                if (result.isSuccess) {
                    savedFilterPresets = (savedFilterPresets.filterNot { it.name.equals(clean, true) } +
                        SavedFilterPreset(clean, value)).sortedBy { it.name.lowercase(Locale.ROOT) }
                    persistSavedFilterPresets()
                    savedFilterSyncError = null
                    onResult(true)
                } else {
                    savedFilterSyncError = "No s'han guardat els filtres en la base de dades: ${result.exceptionOrNull()?.message.orEmpty()}"
                    onResult(false)
                }
            }
        }
    }

    fun deleteFilterPreset(name: String) {
        val accountName = currentAccount?.username
        val tokenAtStart = ServerApiRepository.token(appContext)
        if (accountName == null || tokenAtStart.isNullOrBlank()) {
            savedFilterSyncError = "Inicia sessió per eliminar llistes del servidor."
            return
        }
        filterPresetsExecutor.execute {
            val result = if (ServerApiRepository.token(appContext) == tokenAtStart)
                ServerApiRepository.deleteFilterPresetOnServer(tokenAtStart, name)
            else Result.failure(IllegalStateException("La sessió ha canviat."))
            Handler(Looper.getMainLooper()).post {
                if (currentAccount?.username != accountName || ServerApiRepository.token(appContext) != tokenAtStart) return@post
                if (result.isSuccess) {
                    val wasActive = activeFilterPresetName.equals(name, true)
                    savedFilterPresets = savedFilterPresets.filterNot { it.name.equals(name, true) }
                    if (wasActive) selectFeedMode(FeedMode.FOR_YOU)
                    persistSavedFilterPresets()
                    savedFilterSyncError = null
                } else savedFilterSyncError = "No s'ha eliminat la llista del servidor: ${result.exceptionOrNull()?.message.orEmpty()}"
            }
        }
    }

    fun applyFilterPreset(name: String) {
        val preset = savedFilterPresets.firstOrNull { it.name == name } ?: return
        // Una llista és una cerca completa sobre el catàleg; mai sobre «Per a tu».
        selectFeedMode(FeedMode.UNFILTERED)
        updateFilters(preset.filters)
        activeFilterPresetName = preset.name
    }

    fun updateFilters(value: Filters) {
        // Aplicar una configuració buida equival a llevar els filtres:
        // es recupera «Per a tu», encara que hi haguera una cerca escrita.
        if (value.isEmpty()) {
            selectFeedMode(FeedMode.FOR_YOU)
            return
        }
        activeFilterPresetName = null
        if (filters == value) return
        filters = value
        synchronizeCriteriaFeedMode()
        if (!value.isEmpty()) ensureFullAgendaLoaded("filters")
        feedCriteriaChanged()
        // 10ao: els filtres adquirits també queden persistits al servidor. Els filtres
        // culturals/territorials alimenten la mateixa afinitat ja existent; els filtres
        // numèrics es guarden com a telemetria sense alterar els pesos de Per a tu.
        value.town?.takeIf(String::isNotBlank)?.let { recordBehaviour(BehaviourKind.TOWN, it, serverAction = "SEARCH_RELATED") }
        value.comarca?.takeIf(String::isNotBlank)?.let { recordBehaviour(BehaviourKind.COMARCA, it, serverAction = "SEARCH_RELATED") }
        value.categories.forEach { recordBehaviour(BehaviourKind.CATEGORY, it.name, serverAction = "SEARCH_RELATED") }
        value.maxDistanceKm?.let {
            recordBehaviour(BehaviourKind.MAX_DISTANCE, it.toString())
            syncAffinitySignal("FILTER_APPLIED", "FILTER", "max_distance", it.toString(), null)
        }
        value.maxPrice?.let {
            recordBehaviour(BehaviourKind.MAX_PRICE, it.toString())
            syncAffinitySignal("FILTER_APPLIED", "FILTER", "max_price", it.toString(), null)
        }
        if (value.freeOnly) {
            recordBehaviour(BehaviourKind.FREE_ONLY, "true")
            syncAffinitySignal("FILTER_APPLIED", "FILTER", "free_only", "true", null)
        }
    }

    fun clearFilters() {
        // Els botons «Netejar» i «Llevar filtres» abandonen tota la consulta,
        // també si hi havia una cerca escrita, i tornen a «Per a tu».
        selectFeedMode(FeedMode.FOR_YOU)
    }

    fun updateUserPreferences(value: UserPreferences) {
        if (userPreferences == value) return
        userPreferencesWriteRevision += 1L
        val feedSwitchesChanged = value.valencianFirst != userPreferences.valencianFirst ||
            value.showSuggestions != userPreferences.showSuggestions || value.discoveryLevel != userPreferences.discoveryLevel
        val placesChanged = value.preferredLocations != userPreferences.preferredLocations ||
            value.preferredComarques != userPreferences.preferredComarques ||
            value.locationRadiiKm != userPreferences.locationRadiiKm
        val newPlaces = value.preferredLocations - userPreferences.preferredLocations
        val newComarques = value.preferredComarques - userPreferences.preferredComarques
        val newArtists = value.favoriteArtists - userPreferences.favoriteArtists
        userPreferences = value.copy(
            locationRadiiKm = value.locationRadiiKm.filterKeys { town ->
                value.preferredLocations.any { it.equals(town, ignoreCase = true) }
            }.mapValues { (_, radius) -> PersonalizationOptions.validRadius(radius) }
        )
        newPlaces.forEach { town ->
            recordBehaviour(BehaviourKind.TOWN, town, serverAction = "SEARCH_RELATED")
            comarcaForTown(town)?.let { recordBehaviour(BehaviourKind.COMARCA, it, serverAction = "SEARCH_RELATED") }
        }
        newComarques.forEach { recordBehaviour(BehaviourKind.COMARCA, it, serverAction = "SEARCH_RELATED") }
        newArtists.forEach(::recordArtistView)
        store.edit()
            // Llocs són de cada compte, no globals de l'aplicació.
            .putStringSet(userCacheKey("preferred_locations"), userPreferences.preferredLocations.toSet())
            .putStringSet(userCacheKey("preferred_comarques"), userPreferences.preferredComarques.toSet())
            .putStringSet("favorite_artists", userPreferences.favoriteArtists)
            .putBoolean(userCacheKey("valencian_first"), userPreferences.valencianFirst)
            .putBoolean(userCacheKey("show_suggestions"), userPreferences.showSuggestions)
            .putString(userCacheKey("preferred_location_radii"), JSONObject().apply {
                userPreferences.locationRadiiKm.forEach { (town, radius) ->
                    put(town, PersonalizationOptions.validRadius(radius))
                }
            }.toString())
            .putString(userCacheKey("discovery_level"), userPreferences.discoveryLevel.name)
            .apply {
                if (placesChanged && currentAccount != null) {
                    putBoolean(userCacheKey("places_remote_dirty"), true)
                }
                if (feedSwitchesChanged && currentAccount != null) {
                    putBoolean(userCacheKey("feed_prefs_remote_dirty"), true)
                }
            }
            .commit()
        // En triar/llevar l'últim poble o comarca, el feed canvia de mode.
        applyAutomaticDefaultFeedMode()
        // Els suggeriments depenen ara també del territori i les disciplines.
        invalidateFollowSuggestions()
        // v264: qualsevol preferència canviada invalida el rànquing guardat.
        // Una cache d'abans del canvi no és vàlida, encara que el catàleg siga idèntic.
        expireForYouSnapshot()
        invalidateFeedCache()
        if (placesChanged) {
            placesSyncError = null
            placesSavedOnServer = false
            synchronizePlacesWithServer()
        }
        if (feedSwitchesChanged) synchronizeFeedPreferencesWithServer()
    }

    private fun buildNotifications(): List<AgendaNotification> {
        val result = mutableListOf<AgendaNotification>()
        val today = agendaToday()
        val nowMillis = System.currentTimeMillis()
        val weekMillis = 7L * 24L * 60L * 60L * 1000L
        val weekEnd = today.plusDays(7)

        // RESULTATS DE REPORTS: respostes de moderació enviades a este usuari/dispositiu.
        reportResultNotifications.forEach { report ->
            val sort = runCatching {
                LocalDateTime.parse(report.resolvedAt.replace(" ", "T")).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }.getOrDefault(nowMillis)
            result += AgendaNotification(
                id = reportResultNotificationId(report.reportId, report.reviewRound),
                type = NotificationType.CHANGES,
                title = report.title,
                body = report.message,
                concertId = report.eventId,
                sortKey = sort
            )
        }

        if (alertPreferences.communityMessages) result += eventCommunityNotifications

        // V233: els grups d'amics són llistes compartides, no taulons d'esdeveniments.
        // Les notificacions de Comunitat només corresponen als taulons vinculats a actes.

        // Avisos triats explícitament a la campaneta de la fitxa: apareixen
        // quan arriba la data programada. No queden restringits a la finestra de 7 dies.
        if (alertPreferences.dayBefore) {
            concerts.asSequence()
                .filter { it.id in alertConcerts && it.id in savedConcerts }
                .mapNotNull { concert ->
                    val scheduledAt = EventReminderScheduler.get(appContext, concert.id)?.toEpochMilli()
                        ?: return@mapNotNull null
                    val lastDate = runCatching {
                        LocalDate.parse(concert.endDate.ifBlank { concert.date }.take(10))
                    }.getOrNull() ?: return@mapNotNull null
                    if (scheduledAt > nowMillis || lastDate.isBefore(today)) return@mapNotNull null
                    AgendaNotification(
                        id = "personal_reminder_${concert.id}_$scheduledAt",
                        type = NotificationType.REMINDER,
                        title = "Recordatori: ${concert.title}",
                        body = "${concert.date.take(10)} · ${concert.town}",
                        concertId = concert.id,
                        sortKey = scheduledAt
                    )
                }.forEach(result::add)
        }

        // PRÒXIMS: guardats que entren en la pròxima setmana. La notificació naix
        // quan l'esdeveniment entra en esta finestra i continua sent navegable des de la campaneta.
        if (alertPreferences.dayBefore) {
            concerts.asSequence()
                .filter { it.id in savedConcerts && EventReminderScheduler.get(appContext, it.id) == null }
                .mapNotNull { concert ->
                    val date = runCatching { LocalDate.parse(concert.date.take(10)) }.getOrNull() ?: return@mapNotNull null
                    if (date.isBefore(today) || date.isAfter(weekEnd)) return@mapNotNull null
                    concert to date
                }
                .sortedWith(compareBy<Pair<Concert, LocalDate>> { it.second }.thenBy { it.first.time })
                .forEach { (concert, date) ->
                    val daysAway = ChronoUnit.DAYS.between(today, date)
                    val whenLabel = when (daysAway) {
                        0L -> "Hui"
                        1L -> "Demà"
                        else -> "En $daysAway dies"
                    }
                    result += AgendaNotification(
                        id = "saved_${concert.id}_${concert.date}",
                        type = NotificationType.REMINDER,
                        title = "$whenLabel: ${concert.title}",
                        body = "${concert.time} · ${concert.town}",
                        concertId = concert.id,
                        // Moment d'entrada en la finestra de set dies, en epoch ms.
                        // Així General pot intercalar els recordatoris cronològicament
                        // amb avisos de compres, artistes i grups.
                        sortKey = date.minusDays(7)
                            .atStartOfDay(ZoneId.systemDefault())
                            .toInstant().toEpochMilli()
                    )
                }
        }

        // NOUS: només per a artistes amb campaneta activa i freqüència específica. No afecta els
        // recordatoris manuals ni els missatges de comunitat o compres.
        if (alertPreferences.followedArtist && artistAlerts.isNotEmpty()) {
            data class FollowedCandidate(val artist: String, val concert: Concert, val createdAt: Long)

            fun stableCreatedAt(concert: Concert): Long {
                if (concert.createdAtMillis > 0L) return concert.createdAtMillis
                val eventDate = runCatching { LocalDate.parse(concert.date.take(10)) }.getOrNull() ?: today
                return eventDate.minusDays(28)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            }

            val candidates = concerts.mapNotNull { concert ->
                val concertDate = runCatching { LocalDate.parse(concert.date.take(10)) }.getOrNull()
                if (concertDate != null && concertDate.isBefore(today)) return@mapNotNull null
                val matchedArtist = (listOf(concert.artist) + concert.lineup)
                    .asSequence()
                    .map(String::trim)
                    .filter(String::isNotBlank)
                    .distinctBy { it.lowercase() }
                    .firstOrNull { artist -> artistAlerts.any { it.equals(artist, ignoreCase = true) } }
                    ?: return@mapNotNull null
                FollowedCandidate(matchedArtist, concert, stableCreatedAt(concert))
            }.groupBy { normalizeSearchValue(it.artist) }

            candidates.values.forEach { artistQueue ->
                // Cada campaneta té periodicitat pròpia, no la genèrica del seguiment.
                val artistFrequency = artistAlertFrequency(artistQueue.first().artist)
                val followedGapMillis = when (artistFrequency) {
                    BellFollowedFrequency.IMMEDIATE -> 0L
                    BellFollowedFrequency.DAILY -> 24L * 60L * 60L * 1000L
                    BellFollowedFrequency.WEEKLY -> weekMillis
                }
                val followedVisibleMillis = if (followedGapMillis == 0L) weekMillis else followedGapMillis
                var previousRelease: Long? = null
                val scheduled = artistQueue
                    .sortedWith(compareBy<FollowedCandidate> { it.createdAt }.thenBy { it.concert.id })
                    .map { candidate ->
                        val releaseAt = previousRelease?.let { maxOf(candidate.createdAt, it + followedGapMillis) }
                            ?: candidate.createdAt
                        previousRelease = releaseAt
                        candidate to releaseAt
                    }

                // Només una finestra setmanal activa per artista; no acumulem una pila d'avisos antics.
                scheduled
                    .filter { (_, releaseAt) -> releaseAt <= nowMillis && nowMillis - releaseAt in 0 until followedVisibleMillis }
                    .maxByOrNull { it.second }
                    ?.let { (candidate, releaseAt) ->
                        result += AgendaNotification(
                            id = "followed_${normalizeSearchValue(candidate.artist)}_${candidate.concert.id}_$releaseAt",
                            type = NotificationType.NEW_CONCERT,
                            title = "Nou concert de ${candidate.artist}",
                            body = "${candidate.concert.title} · ${AgendaDateFormat.date(candidate.concert.date)} · ${candidate.concert.town}",
                            concertId = candidate.concert.id,
                            sortKey = releaseAt
                        )
                    }
            }
        }

        // ENTRADES: una compra nova apareix en la campaneta durant un màxim de 7 dies.
        if (alertPreferences.ticketUpdates) {
            ticketsForCurrentUser()
                .groupBy { it.concertId }
                .forEach { (concertId, tickets) ->
                    val latestPurchase = tickets.maxOfOrNull { it.purchasedAtMillis } ?: return@forEach
                    if (latestPurchase > nowMillis || nowMillis - latestPurchase >= weekMillis) return@forEach
                    val concert = concerts.firstOrNull { it.id == concertId } ?: return@forEach
                    val qty = tickets.size
                    result += AgendaNotification(
                        id = "ticket_${concert.id}_$latestPurchase",
                        type = NotificationType.TICKETS,
                        title = "Entrades · ${concert.title}",
                        body = "$qty ${if (qty == 1) "entrada" else "entrades"} · ${AgendaDateFormat.date(concert.date)} · ${concert.town}",
                        concertId = concert.id,
                        sortKey = latestPurchase
                    )
                }
        }

        return result
            .distinctBy { it.id }
            .filterNot { it.id in dismissedNotificationIds }
            .sortedByDescending { it.sortKey }
    }

    /** Esdeveniments que alimenten cada pestanya real de la campaneta. */
    fun notificationEvents(type: NotificationType): List<Concert> {
        val byId = concerts.associateBy { it.id }
        val source = notifications().filter { it.type == type }
        val mapped = source.mapNotNull { notification -> notification.concertId?.let(byId::get) }.distinctBy { it.id }
        return when (type) {
            NotificationType.REMINDER -> mapped.sortedWith(compareBy<Concert> { it.date }.thenBy { it.time })
            else -> mapped
        }
    }

    /**
     * Suggerències = pocs avisos d'alta confiança. No són una llista general de
     * recomanacions i mai incrementen el badge de la campaneta. Només entren
     * esdeveniments d'esta setmana amb una afinitat realment alta.
     */
    private fun buildStrongNotificationSuggestions(): List<Concert> {
        val today = agendaToday()
        val until = today.plusDays(7)
        val now = System.currentTimeMillis()
        val candidates = concerts.asSequence()
            .filter { concert ->
                val date = runCatching { LocalDate.parse(concert.date.take(10)) }.getOrNull() ?: return@filter false
                !date.isBefore(today) && !date.isAfter(until)
            }
            .filterNot { it.id in savedConcerts || it.id in ticketPurchases }
            .filterNot { concert ->
                (listOf(concert.artist) + concert.lineup).any { artist ->
                    followedArtists.any { it.equals(artist.trim(), ignoreCase = true) }
                }
            }
            .toList()
        if (candidates.isEmpty()) return emptyList()

        val aggregated = aggregateBehaviour(now)
        val eventById = concerts.associateBy { it.id }
        val languageProfile = languageAffinityProfile(aggregated, eventById, now)
        val inferredStyles = inferredStylePreferences().map(::normalizeSearchValue).toSet()
        val followedStyles = concerts.asSequence()
            .filter { candidate -> candidate.lineup.any { it in followedArtists } }
            .flatMap { it.styles.asSequence() }
            .map(::normalizeSearchValue)
            .toSet()

        data class ScoredSuggestion(val concert: Concert, val score: Int)
        val ranked = candidates.map { concert ->
            ScoredSuggestion(
                concert = concert,
                score = personalizedInterestScore(
                    concert = concert,
                    inferredStyles = inferredStyles,
                    followedStyles = followedStyles,
                    aggregated = aggregated,
                    eventById = eventById,
                    languageProfile = languageProfile,
                    now = now
                )
            )
        }
            .filter { it.score >= 95 }
            .sortedWith(
                compareByDescending<ScoredSuggestion> { it.score }
                    .thenBy { it.concert.date }
                    .thenBy { it.concert.time }
            )

        // Màxim cinc i, sempre que siga possible, un per artista perquè siguen avisos
        // realment destacats i no una segona còpia del feed personalitzat.
        val selected = mutableListOf<Concert>()
        val usedArtists = mutableSetOf<String>()
        for (item in ranked) {
            val artistKey = normalizeSearchValue(item.concert.lineup.firstOrNull().orEmpty().ifBlank { item.concert.artist })
            if (artistKey.isNotBlank() && artistKey in usedArtists) continue
            selected += item.concert
            if (artistKey.isNotBlank()) usedArtists += artistKey
            if (selected.size >= 5) break
        }
        return selected
    }

    fun notificationSuggestions(): List<Concert> =
        if (userPreferences.showSuggestions && alertPreferences.suggestions) notificationSuggestionsState else emptyList()

    /** ID persistent del suggeriment en General, separat dels avisos que compten al badge. */
    fun suggestionNotificationId(concertId: Int): String = com.example.agendavalenciana.model.NotificationGeneralRules.suggestionId(concertId)

    fun markNotificationForEvent(type: NotificationType, concertId: Int) {
        notifications()
            .filter { it.type == type && it.concertId == concertId }
            .forEach { markNotificationRead(it.id) }
    }

    fun refreshNotificationPanel() {
        // Les notificacions de Comunitat venen de la consulta autenticada de col·leccions;
        // refresquem l'últim estat sense frenar l'obertura del panell.
        if (currentAccount != null) {
            refreshSavedCollections()
            refreshEventCommunityNotifications()
        }
        // No fem cap càlcul síncron ací: el diàleg s'obri immediatament amb la cache
        // i estes dos tasques refresquen el contingut en segon pla.
        refreshNotificationBadgeAsync(0L)
        refreshNotificationSuggestionsAsync(0L)
    }

    fun notifications(): List<AgendaNotification> = notificationPanelNotificationsState

    fun unreadNotificationCount(): Int = unreadNotificationCountState

    fun notificationCount(type: NotificationType): Int = notifications().count { it.type == type }

    fun markNotificationRead(id: String) {
        if (id in readNotificationIds) return
        readNotificationIds = readNotificationIds + id
        saveStringSet("read_notification_ids", readNotificationIds)
        unreadNotificationCountState = cachedNotificationsList.count { it.id !in readNotificationIds }
    }

    fun markAllNotificationsRead() {
        readNotificationIds = readNotificationIds + notifications().map { it.id } +
            notificationSuggestionsState.map { suggestionNotificationId(it.id) }
        saveStringSet("read_notification_ids", readNotificationIds)
        unreadNotificationCountState = 0
    }

    /** Elimina les notificacions actuals del panell. Les noves que apareguen després continuen entrant. */
    fun dismissAllNotifications() {
        val ids = notifications().mapTo(mutableSetOf()) { it.id }
        val suggestionIds = notificationSuggestionsState.mapTo(mutableSetOf()) { suggestionNotificationId(it.id) }
        if (ids.isEmpty() && suggestionIds.all { it in readNotificationIds }) return
        dismissedNotificationIds = dismissedNotificationIds + ids
        // Les suggerències es mantenen disponibles a la seua pestanya, però
        // deixen de figurar com a noves en General després de netejar.
        readNotificationIds = readNotificationIds + ids + suggestionIds
        saveStringSet("dismissed_notification_ids", dismissedNotificationIds)
        saveStringSet("read_notification_ids", readNotificationIds)
        cachedNotificationsList = emptyList()
        notificationPanelNotificationsState = emptyList()
        notificationsCacheReady = true
        unreadNotificationCountState = 0
    }

    fun updateAlertPreferences(value: AlertPreferences) {
        alertPreferences = value
        store.edit()
            .putBoolean("alert_community", value.communityMessages)
            .putBoolean("alert_followed", value.followedArtist)
            .putBoolean("alert_town", value.homeTown)
            .putBoolean("alert_comarca", value.homeComarca)
            .putBoolean("alert_style", value.favoriteStyle)
            .putBoolean("alert_day_before", value.dayBefore)
            .putBoolean("alert_tickets", value.ticketUpdates)
            .putBoolean("alert_changes", value.changes)
            .putBoolean("alert_suggestions", value.suggestions)
            .putString("alert_followed_frequency", value.followedFrequency.name)
            .apply()
        invalidateNotificationCache()
    }

    fun refreshEmailPreferences(onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        if (currentAccount == null || ServerApiRepository.token(appContext).isNullOrBlank()) {
            onResult(false, "Inicia sessió per configurar els correus.")
            return
        }
        if (emailPreferencesSyncing) return
        emailPreferencesSyncing = true
        emailPreferencesMessage = null
        backgroundExecutor.execute {
            val result = ServerApiRepository.fetchEmailPreferences(appContext)
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { incoming ->
                    emailPreferences = incoming
                    saveEmailPreferences(incoming)
                    emailPreferencesMessage = null
                    onResult(true, null)
                }.onFailure { error ->
                    val message = error.message ?: "No s'han pogut carregar les preferències de correu."
                    emailPreferencesMessage = message
                    onResult(false, message)
                }
                emailPreferencesSyncing = false
            }
        }
    }

    fun updateEmailPreferences(value: EmailPreferences, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        if (currentAccount == null || ServerApiRepository.token(appContext).isNullOrBlank()) {
            onResult(false, "Inicia sessió per configurar els correus.")
            return
        }
        if (value.frequency != EmailDigestFrequency.NONE && !value.hasSelectedContent) {
            onResult(false, "Tria almenys un tipus de contingut per als correus.")
            return
        }
        if (emailPreferencesSyncing) return
        emailPreferencesSyncing = true
        emailPreferencesMessage = null
        backgroundExecutor.execute {
            val result = ServerApiRepository.updateEmailPreferences(appContext, value)
            Handler(Looper.getMainLooper()).post {
                result.onSuccess { saved ->
                    emailPreferences = saved
                    saveEmailPreferences(saved)
                    emailPreferencesMessage = "Preferències de correu guardades."
                    onResult(true, emailPreferencesMessage)
                }.onFailure { error ->
                    val message = error.message ?: "No s'han pogut guardar les preferències de correu."
                    emailPreferencesMessage = message
                    onResult(false, message)
                }
                emailPreferencesSyncing = false
            }
        }
    }

    private fun concertFromSubmission(submission: EventSubmission): Concert {
        val primaryArtist = submission.artists.firstOrNull().orEmpty().ifBlank { submission.title }
        val lineupText = submission.artists.takeIf { it.size > 1 }?.joinToString(" · ")
        val detail = buildList {
            if (lineupText != null) add("Artistes: $lineupText")
            if (submission.description.isNotBlank()) add(submission.description)
            if (submission.ticketUrl.isNotBlank()) add("Entrades: ${submission.ticketUrl}")
        }.joinToString("\n").ifBlank { submission.title }
        return Concert(
            id = 100_000 + submission.id,
            artist = primaryArtist,
            title = submission.title,
            subtitle = submission.subtitle,
            lineup = submission.artists.ifEmpty { listOf(primaryArtist) },
            posterUri = submission.posterUri,
            date = submission.date,
            time = submission.time,
            endDate = submission.endDate,
            endTime = submission.endTime,
            town = submission.town,
            comarca = comarcaForTown(submission.town) ?: "Sense comarca",
            venue = submission.venue,
            styles = submission.styles.ifEmpty { listOf(submission.category.label) },
            price = submission.price,
            language = submission.language,
            description = detail,
            category = submission.category,
            ticketPrice = submission.price,
            ticketAvailable = submission.ticketSalesEnabled,
            ticketUrl = submission.ticketUrl,
            coEditorUsernames = setOf(submission.ownerUsername)
        )
    }

    private fun loadEventSubmissions(): List<EventSubmission> {
        val raw = store.getString("event_submissions", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val artistsJson = item.optJSONArray("artists") ?: JSONArray()
                    val stylesJson = item.optJSONArray("styles") ?: JSONArray()
                    val artists = buildList {
                        for (i in 0 until artistsJson.length()) add(artistsJson.optString(i))
                    }.filter { it.isNotBlank() }
                    val styles = buildList {
                        for (i in 0 until stylesJson.length()) add(stylesJson.optString(i))
                    }.filter { it.isNotBlank() }
                    add(
                        EventSubmission(
                            id = item.getInt("id"),
                            ownerUsername = item.optString("ownerUsername"),
                            posterUri = if (item.isNull("posterUri")) null else item.optString("posterUri").takeIf { it.isNotBlank() },
                            title = item.optString("title"),
                            subtitle = item.optString("subtitle"),
                            artists = artists,
                            date = item.optString("date"),
                            time = item.optString("time"),
                            endDate = item.optString("endDate"),
                            endTime = item.optString("endTime"),
                            town = item.optString("town"),
                            venue = item.optString("venue"),
                            category = runCatching { CulturalCategory.valueOf(item.optString("category")) }.getOrDefault(CulturalCategory.MUSIC),
                            styles = styles,
                            language = item.optString("language", "Valencià"),
                            price = item.optInt("price", 0),
                            ticketSalesEnabled = item.optBoolean("ticketSalesEnabled", item.optString("ticketUrl").isNotBlank()),
                            ticketUrl = item.optString("ticketUrl"),
                            description = item.optString("description"),
                            recognizedText = item.optString("recognizedText"),
                            status = runCatching { EventSubmissionStatus.valueOf(item.optString("status")) }.getOrDefault(EventSubmissionStatus.DRAFT),
                            createdAtMillis = item.optLong("createdAtMillis", System.currentTimeMillis())
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun saveEventSubmissions() {
        val array = JSONArray()
        eventSubmissions.forEach { submission ->
            val artists = JSONArray()
            submission.artists.forEach { artists.put(it) }
            val styles = JSONArray()
            submission.styles.forEach { styles.put(it) }
            array.put(
                JSONObject()
                    .put("id", submission.id)
                    .put("ownerUsername", submission.ownerUsername)
                    .put("posterUri", submission.posterUri ?: JSONObject.NULL)
                    .put("title", submission.title)
                    .put("subtitle", submission.subtitle)
                    .put("artists", artists)
                    .put("date", submission.date)
                    .put("time", submission.time)
                    .put("endDate", submission.endDate)
                    .put("endTime", submission.endTime)
                    .put("town", submission.town)
                    .put("venue", submission.venue)
                    .put("category", submission.category.name)
                    .put("styles", styles)
                    .put("language", submission.language)
                    .put("price", submission.price)
                    .put("ticketSalesEnabled", submission.ticketSalesEnabled)
                    .put("ticketUrl", submission.ticketUrl)
                    .put("description", submission.description)
                    .put("recognizedText", submission.recognizedText)
                    .put("status", submission.status.name)
                    .put("createdAtMillis", submission.createdAtMillis)
            )
        }
        store.edit().putString("event_submissions", array.toString()).apply()
    }

    private fun loadAdvertisements(): List<EventAdvertisement> {
        val raw = store.getString("event_advertisements", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    fun stringSet(key: String): Set<String> {
                        val values = item.optJSONArray(key) ?: return emptySet()
                        return buildSet { for (i in 0 until values.length()) add(values.optString(i)) }
                    }
                    add(
                        EventAdvertisement(
                            id = item.getInt("id"),
                            ownerUsername = item.optString("ownerUsername"),
                            concertId = item.getInt("concertId"),
                            createdAtMillis = item.optLong("createdAtMillis", System.currentTimeMillis()),
                            startsAtMillis = item.optLong("startsAtMillis", System.currentTimeMillis()),
                            durationHours = item.optInt("durationHours", 24),
                            imageUri = if (item.isNull("imageUri")) null else item.optString("imageUri").takeIf { it.isNotBlank() },
                            cardFocusX = item.optDouble("cardFocusX", 0.5).toFloat().coerceIn(0f, 1f),
                            cardFocusY = item.optDouble("cardFocusY", 0.5).toFloat().coerceIn(0f, 1f),
                            targetStyles = stringSet("targetStyles"),
                            targetSearchTerms = stringSet("targetSearchTerms"),
                            targetTowns = stringSet("targetTowns"),
                            targetComarques = stringSet("targetComarques"),
                            targetFollowers = item.optBoolean("targetFollowers", false),
                            targetHighIntent = item.optBoolean("targetHighIntent", false),
                            matchAllCriteria = item.optBoolean("matchAllCriteria", false),
                            placement = runCatching { AdPlacement.valueOf(item.optString("placement", "FEED_NORMAL")) }.getOrDefault(AdPlacement.FEED_NORMAL),
                            amountCents = item.optInt("amountCents", 0),
                            status = item.optString("status", "active"),
                            serverEndsAtMillis = item.optLong("serverEndsAtMillis", 0L).takeIf { it > 0L }
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun saveAdvertisements() {
        val array = JSONArray()
        advertisements.forEach { ad ->
            fun jsonArray(values: Set<String>) = JSONArray().also { out -> values.forEach { value -> out.put(value) } }
            array.put(
                JSONObject()
                    .put("id", ad.id)
                    .put("ownerUsername", ad.ownerUsername)
                    .put("concertId", ad.concertId)
                    .put("createdAtMillis", ad.createdAtMillis)
                    .put("startsAtMillis", ad.startsAtMillis)
                    .put("durationHours", ad.durationHours)
                    .put("imageUri", ad.imageUri ?: JSONObject.NULL)
                    .put("cardFocusX", ad.cardFocusX)
                    .put("cardFocusY", ad.cardFocusY)
                    .put("targetStyles", jsonArray(ad.targetStyles))
                    .put("targetSearchTerms", jsonArray(ad.targetSearchTerms))
                    .put("targetTowns", jsonArray(ad.targetTowns))
                    .put("targetComarques", jsonArray(ad.targetComarques))
                    .put("targetFollowers", ad.targetFollowers)
                    .put("targetHighIntent", ad.targetHighIntent)
                    .put("matchAllCriteria", ad.matchAllCriteria)
                    .put("placement", ad.placement.name)
                    .put("amountCents", ad.amountCents)
                    .put("status", ad.status)
                    .put("serverEndsAtMillis", ad.serverEndsAtMillis ?: JSONObject.NULL)
            )
        }
        store.edit().putString("event_advertisements", array.toString()).apply()
    }

    fun behaviourValues(kind: BehaviourKind): Set<String> =
        behaviourProfile.events
            .asSequence()
            .filter { it.kind == kind }
            .map { it.value }
            .filter(String::isNotBlank)
            .toSet()

    fun topBehaviourValues(kind: BehaviourKind, limit: Int = 20): List<Pair<String, Int>> =
        behaviourProfile.events
            .filter { it.kind == kind }
            .groupingBy { it.value }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(limit.coerceAtLeast(1))
            .map { it.key to it.value }

    fun adTargetingDefaults(): AdTargetingDefaults = behaviourProfile.adDefaults

    private fun recordBehaviour(
        kind: BehaviourKind,
        rawValue: String,
        serverAction: String? = null,
        concert: Concert? = null
    ) {
        val username = currentAccount?.username.orEmpty()
        val value = rawValue.trim()
        if (value.isBlank()) return
        val normalized = when (kind) {
            BehaviourKind.SEARCH, BehaviourKind.STYLE, BehaviourKind.LANGUAGE -> value.lowercase()
            else -> value
        }
        val now = System.currentTimeMillis()
        val event = UserBehaviourEvent(kind = kind, value = normalized, timestampMillis = now)
        val retentionCutoff = now - 180L * 86_400_000L
        val retainedEvents = (behaviourProfile.events.asSequence()
            .filter { it.timestampMillis >= retentionCutoff }
            .plus(event)
            .toList())
            .takeLast(800)
        val updated = behaviourProfile.copy(
            username = username,
            events = retainedEvents,
            updatedAtMillis = now
        )
        behaviourProfile = updated
        saveBehaviourProfile(updated)
        // El comportament implícit (visitar una fitxa, cercar, escoltar gustos)
        // informa el PRÒXIM recàlcul. No destruïx una llista de 30 minuts.
        // Les accions explícites de seguir/bloquejar continuen invalidant a l'instant.

        if (!serverAction.isNullOrBlank()) {
            val entityType = when (kind) {
                BehaviourKind.TOWN -> "TOWN"
                BehaviourKind.COMARCA -> "COMARCA"
                BehaviourKind.ARTIST, BehaviourKind.FOLLOWED_ARTIST -> "ARTIST"
                BehaviourKind.ORGANIZER, BehaviourKind.FOLLOWED_ORGANIZER -> "ORGANIZER"
                BehaviourKind.VENUE -> "VENUE"
                BehaviourKind.STYLE -> "STYLE"
                BehaviourKind.LANGUAGE -> "LANGUAGE"
                BehaviourKind.CATEGORY, BehaviourKind.FOLLOWED_CATEGORY -> "CATEGORY"
                BehaviourKind.EVENT_VIEW, BehaviourKind.SAVED_EVENT, BehaviourKind.WANT_TO_GO, BehaviourKind.GOING -> "EVENT"
                else -> "RAW"
            }
            syncAffinitySignal(serverAction, entityType, normalized, value, concert)
        }
    }

    private fun syncAffinitySignal(
        action: String,
        entityType: String,
        entityKey: String,
        entityLabel: String,
        concert: Concert?
    ) {
        if (currentAccount == null || ServerApiRepository.token(appContext).isNullOrBlank()) return
        val payload = affinityServerPayload(action, entityType, entityKey, entityLabel, concert)
        // No pujar un interés nou abans que acabe el reinici del servidor:
        // podria ser eliminat per l'endpoint si s'hi envia abans.
        if (learningResetSyncPending) enqueuePendingServerPost("affinities/record.php", payload)
        else enqueueAffinitySignal(payload)
    }

    /** Agrupa cerques/obertures pròximes en una sola petició HTTPS. */
    private fun enqueueAffinitySignal(payload: JSONObject) {
        var startWorker = false
        synchronized(affinityBatchLock) {
            pendingAffinityBatch += JSONObject(payload.toString())
            if (!affinityBatchScheduled) {
                affinityBatchScheduled = true
                startWorker = true
            }
        }
        if (!startWorker) return
        backgroundExecutor.execute {
            while (true) {
                try { Thread.sleep(350L) } catch (_: InterruptedException) { }
                val batchGeneration = learningResetGeneration
                val batch = synchronized(affinityBatchLock) {
                    if (pendingAffinityBatch.isEmpty()) {
                        affinityBatchScheduled = false
                        emptyList()
                    } else {
                        val count = minOf(50, pendingAffinityBatch.size)
                        List(count) { JSONObject(pendingAffinityBatch.removeAt(0).toString()) }
                    }
                }
                if (batch.isEmpty()) break
                if (batchGeneration != learningResetGeneration) continue
                ServerApiRepository.postAffinityBatch(appContext, batch).onFailure {
                    if (batchGeneration != learningResetGeneration) return@onFailure
                    batch.forEach { enqueuePendingServerPost("affinities/record.php", it) }
                }
            }
        }
    }

    private fun affinityServerPayload(
        action: String,
        entityType: String,
        entityKey: String,
        entityLabel: String,
        concert: Concert?
    ): JSONObject {
        val organizer = concert?.coEditorUsernames?.firstOrNull()?.let { profileFor(it).displayName.ifBlank { it } }.orEmpty()
        val userTown = userPreferences.preferredLocations.firstOrNull().orEmpty()
        val userComarca = userTown.takeIf(String::isNotBlank)?.let(::comarcaForTown).orEmpty()
        return JSONObject()
            .put("action_type", action)
            .put("entity_type", entityType)
            .put("entity_key", entityKey)
            .put("entity_label", entityLabel)
            .put("user_town", userTown)
            .put("user_comarca", userComarca)
            .put("town", concert?.town.orEmpty())
            .put("comarca", concert?.comarca.orEmpty())
            .put("category", concert?.category?.name.orEmpty())
            .put("language", concert?.language.orEmpty())
            .put("primary_style", concert?.styles?.firstOrNull().orEmpty())
            .put("primary_artist", concert?.lineup?.firstOrNull().orEmpty().ifBlank { concert?.artist.orEmpty() })
            .put("organizer", organizer)
            .put("venue", concert?.venue.orEmpty())
            .put("styles_csv", concert?.styles?.joinToString(" | ").orEmpty())
            .also { payload -> concert?.id?.let { payload.put("event_id", it) } }
    }

    private fun eventServerPayload(concert: Concert): JSONObject {
        val organizer = concert.coEditorUsernames.firstOrNull()?.let { profileFor(it).displayName.ifBlank { it } }.orEmpty()
        val userTown = userPreferences.preferredLocations.firstOrNull().orEmpty()
        val userComarca = userTown.takeIf(String::isNotBlank)?.let(::comarcaForTown).orEmpty()
        return ServerApiRepository.eventStatePayload(
            eventId = concert.id,
            entityLabel = concert.title,
            userTown = userTown,
            userComarca = userComarca,
            concert = concert,
            organizer = organizer
        )
    }

    /**
     * Escriu en servidor. Si falla la xarxa, conserva exactament la petició en una cua local.
     * Esta cua és només transport offline; la font de veritat continua sent MySQL.
     */
    private fun postServerStateOrQueue(path: String, payload: JSONObject) {
        if (currentAccount == null || ServerApiRepository.token(appContext).isNullOrBlank()) return
        backgroundExecutor.execute {
            ServerApiRepository.postAuthenticatedJson(appContext, path, payload)
                .onFailure { enqueuePendingServerPost(path, payload) }
        }
    }

    private fun pendingPostsKey(username: String? = currentAccount?.username): String =
        "pending_server_posts_v1_${username?.trim()?.lowercase().takeUnless { it.isNullOrBlank() } ?: "guest"}"

    private fun pendingPostDedupeKey(path: String, payload: JSONObject): String = when (path) {
        "user-state/follow.php" -> "${path}|${payload.optString("entity_type")}|${payload.optString("entity_key")}"
        "user-state/attendance.php", "events/save.php", "events/like.php", "user-state/event-alert.php" ->
            "${path}|${payload.optInt("event_id")}"
        "batecs/seen.php" -> "${path}|${payload.optString("story_key")}" 
        "visibility/index.php" -> "${path}|${payload.optString("kind")}|${payload.optString("target").trim().lowercase()}"
        else -> ""
    }

    private fun enqueuePendingServerPost(path: String, payload: JSONObject) =
        enqueuePendingServerPosts(listOf(path to payload))

    /** Escriu moltes operacions en una sola transacció de SharedPreferences. */
    private fun enqueuePendingServerPosts(posts: List<Pair<String, JSONObject>>) {
        if (posts.isEmpty()) return
        synchronized(store) {
            val current = runCatching { JSONArray(store.getString(pendingPostsKey(), "[]")) }.getOrDefault(JSONArray())
            val items = mutableListOf<JSONObject>()
            for (i in 0 until current.length()) current.optJSONObject(i)?.let { items += it }

            posts.forEach { (path, payload) ->
                val dedupeKey = pendingPostDedupeKey(path, payload)
                if (dedupeKey.isNotBlank()) {
                    items.removeAll { item ->
                        val oldPath = item.optString("path")
                        val oldPayload = item.optJSONObject("payload") ?: JSONObject()
                        pendingPostDedupeKey(oldPath, oldPayload) == dedupeKey
                    }
                }
                items += JSONObject()
                    .put("path", path)
                    .put("payload", JSONObject(payload.toString()))
                    .put("created_at", System.currentTimeMillis())
            }

            // La migració guest pot portar centenars d'afinitats; 1500 evita perdre
            // canvis antics de la cua mentre continua limitant-ne el creixement.
            val kept = items.takeLast(1500)
            val out = JSONArray().also { array -> kept.forEach { item -> array.put(item) } }
            store.edit().putString(pendingPostsKey(), out.toString()).apply()
        }
    }

    /** S'executa en fil de fons abans de descarregar l'estat canònic. */
    private fun flushPendingServerPosts() {
        if (ServerApiRepository.token(appContext).isNullOrBlank()) return
        val flushGeneration = learningResetGeneration
        val array = synchronized(store) {
            runCatching { JSONArray(store.getString(pendingPostsKey(), "[]")) }.getOrDefault(JSONArray())
        }
        if (array.length() == 0) return
        val remaining = JSONArray()
        val affinityItems = mutableListOf<Pair<JSONObject, JSONObject>>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val path = item.optString("path")
            val payload = item.optJSONObject("payload") ?: JSONObject()
            if (path == "affinities/record.php") {
                affinityItems += item to payload
                continue
            }
            val postSucceeded = path.isNotBlank() &&
                ServerApiRepository.postAuthenticatedJson(appContext, path, payload).isSuccess
            if (!postSucceeded) {
                remaining.put(item)
            } else if (path == "affinities/reset-learning.php") {
                Handler(Looper.getMainLooper()).post {
                    learningResetSyncPending = false
                    learningResetFeedback = "Gustos apresos sincronitzats amb el servidor."
                }
            }
        }
        // Si el reinici continua pendent, cap afinitat posterior pot recrear
        // les dades del compte fins que el reset haja sigut acceptat.
        val resetStillPending = (0 until remaining.length()).any { index ->
            remaining.optJSONObject(index)?.optString("path") == "affinities/reset-learning.php"
        }
        if (resetStillPending) {
            affinityItems.forEach { remaining.put(it.first) }
            if (flushGeneration == learningResetGeneration) {
                synchronized(store) { store.edit().putString(pendingPostsKey(), remaining.toString()).apply() }
            }
            return
        }
        if (flushGeneration != learningResetGeneration) return
        affinityItems.chunked(50).forEach { chunk ->
            val ok = ServerApiRepository.postAffinityBatch(appContext, chunk.map { it.second }).isSuccess
            if (!ok) chunk.forEach { remaining.put(it.first) }
        }
        if (flushGeneration != learningResetGeneration) return
        synchronized(store) { store.edit().putString(pendingPostsKey(), remaining.toString()).apply() }
    }

    private fun serverJsonBoolean(obj: JSONObject, key: String, default: Boolean): Boolean {
        if (!obj.has(key) || obj.isNull(key)) return default
        return when (val value = obj.opt(key)) {
            is Boolean -> value
            is Number -> value.toInt() != 0
            is String -> value.equals("true", true) || value == "1" || value.equals("sí", true) || value.equals("si", true)
            else -> default
        }
    }

    private fun saveAdTargetingDefaults(defaults: AdTargetingDefaults) {
        val username = currentAccount?.username ?: return
        val updated = behaviourProfile.copy(
            username = username,
            adDefaults = defaults,
            updatedAtMillis = System.currentTimeMillis()
        )
        behaviourProfile = updated
        saveBehaviourProfile(updated)
    }

    /**
     * Cerca tolerant: els accents no són determinants. També unifica apòstrofs i guions Unicode.
     */
    private fun normalizeSearchValue(value: String): String {
        if (value.isBlank()) return ""
        if (value.length <= 180) searchNormalizationCache[value]?.let { return it }
        val normalized = Normalizer
            .normalize(value.trim().lowercase(), Normalizer.Form.NFKD)
            .replace(normalizeMarksRegex, "")
            .replace(normalizeApostropheRegex, "'")
            .replace(normalizeDashRegex, "-")
            .replace(normalizeWhitespaceRegex, " ")
        if (value.length <= 180 && searchNormalizationCache.size < searchNormalizationCacheLimit) {
            searchNormalizationCache.putIfAbsent(value, normalized)
        }
        return normalized
    }

    fun searchContains(value: String, query: String): Boolean {
        val wanted = normalizeSearchValue(query)
        return wanted.isBlank() || normalizeSearchValue(value).contains(wanted)
    }

    private fun searchTermsKey(username: String?): String =
        if (username.isNullOrBlank()) "recent_search_terms_guest" else "recent_search_terms_${username.lowercase()}"

    private fun behaviourKey(username: String?): String = "behaviour_${username?.trim()?.lowercase().takeUnless { it.isNullOrBlank() } ?: "guest"}"

    private fun loadBehaviourProfile(username: String?): UserBehaviourProfile {
        val effectiveUsername = username?.trim().orEmpty()
        val raw = store.getString(behaviourKey(username), null)
            ?: return UserBehaviourProfile(username = effectiveUsername)
        return runCatching {
            val root = JSONObject(raw)
            val eventsJson = root.optJSONArray("events") ?: JSONArray()
            val now = System.currentTimeMillis()
            val retentionCutoff = now - 180L * 86_400_000L
            val events = buildList {
                // Només necessitem els registres recents; els de més de 6 mesos tenen pes 0.
                val startIndex = (eventsJson.length() - 1000).coerceAtLeast(0)
                for (index in startIndex until eventsJson.length()) {
                    val item = eventsJson.getJSONObject(index)
                    val kind = runCatching { BehaviourKind.valueOf(item.optString("kind")) }.getOrNull() ?: continue
                    val value = item.optString("value").trim()
                    if (value.isBlank()) continue
                    val timestamp = item.optLong("timestampMillis", now)
                    if (timestamp < retentionCutoff) continue
                    add(
                        UserBehaviourEvent(
                            kind = kind,
                            value = value,
                            timestampMillis = timestamp
                        )
                    )
                }
            }.takeLast(800)
            val defaultsJson = root.optJSONObject("adDefaults") ?: JSONObject()
            fun defaultsSet(key: String): Set<String> {
                val values = defaultsJson.optJSONArray(key) ?: return emptySet()
                return buildSet {
                    for (index in 0 until values.length()) {
                        values.optString(index).takeIf(String::isNotBlank)?.let(::add)
                    }
                }
            }
            UserBehaviourProfile(
                username = effectiveUsername,
                events = events,
                adDefaults = AdTargetingDefaults(
                    durationHours = defaultsJson.optInt("durationHours", 24),
                    targetStyles = defaultsSet("targetStyles"),
                    targetSearchTerms = defaultsSet("targetSearchTerms"),
                    targetTowns = defaultsSet("targetTowns"),
                    targetComarques = defaultsSet("targetComarques"),
                    targetFollowers = defaultsJson.optBoolean("targetFollowers", false),
                    matchAllCriteria = defaultsJson.optBoolean("matchAllCriteria", false)
                ),
                updatedAtMillis = root.optLong("updatedAtMillis", System.currentTimeMillis())
            )
        }.getOrDefault(UserBehaviourProfile(username = effectiveUsername))
    }

    private fun followDisplayFromKey(key: String): String = key.trim()

    /** Converteix el resum canònic del servidor en cache local consumible pel rànquing. */
    private fun profileFromRemoteAffinities(
        username: String,
        remote: List<ServerApiRepository.RemoteAffinity>,
        defaults: AdTargetingDefaults
    ): UserBehaviourProfile {
        val now = System.currentTimeMillis()
        val cutoff = now - 180L * 86_400_000L
        val resetAt = store.getLong(learningResetKey(username), 0L)
        val events = buildList {
            remote.forEach { affinity ->
                val kind = when {
                    affinity.permanent && affinity.entityType.equals("ARTIST", true) -> BehaviourKind.FOLLOWED_ARTIST
                    affinity.permanent && affinity.entityType.equals("ORGANIZER", true) -> BehaviourKind.FOLLOWED_ORGANIZER
                    affinity.permanent && affinity.entityType.equals("CATEGORY", true) -> BehaviourKind.FOLLOWED_CATEGORY
                    affinity.entityType.equals("TOWN", true) -> BehaviourKind.TOWN
                    affinity.entityType.equals("COMARCA", true) -> BehaviourKind.COMARCA
                    affinity.entityType.equals("ARTIST", true) -> BehaviourKind.ARTIST
                    affinity.entityType.equals("ORGANIZER", true) -> BehaviourKind.ORGANIZER
                    affinity.entityType.equals("VENUE", true) -> BehaviourKind.VENUE
                    affinity.entityType.equals("STYLE", true) -> BehaviourKind.STYLE
                    affinity.entityType.equals("LANGUAGE", true) -> BehaviourKind.LANGUAGE
                    affinity.entityType.equals("CATEGORY", true) -> BehaviourKind.CATEGORY
                    affinity.entityType.equals("RAW", true) -> BehaviourKind.SEARCH
                    affinity.entityType.equals("EVENT", true) -> when (affinity.lastAction.uppercase()) {
                        "GOING" -> BehaviourKind.GOING
                        "WANT_TO_GO" -> BehaviourKind.WANT_TO_GO
                        "SAVE_EVENT" -> BehaviourKind.SAVED_EVENT
                        else -> BehaviourKind.EVENT_VIEW
                    }
                    else -> BehaviourKind.SEARCH
                }
                val value = if (affinity.entityType.equals("EVENT", true)) affinity.entityKey
                    else affinity.entityLabel.ifBlank { affinity.entityKey }
                if (value.isBlank()) return@forEach
                val timestamp = parseServerUtcMillis(affinity.lastSeenAt).takeIf { it > 0L } ?: now
                if (!affinity.permanent && (timestamp < cutoff || timestamp <= resetAt)) return@forEach
                val repetitions = when {
                    affinity.permanent -> 1
                    affinity.searchCount > 0 -> affinity.searchCount.coerceIn(1, 20)
                    affinity.interactionCount > 0 -> affinity.interactionCount.coerceIn(1, 12)
                    affinity.effectiveScore >= 60.0 -> 3
                    affinity.effectiveScore >= 30.0 -> 2
                    else -> 1
                }
                repeat(repetitions) { add(UserBehaviourEvent(kind, value, timestamp)) }
            }
        }.takeLast(1000)
        return UserBehaviourProfile(username = username, events = events, adDefaults = defaults, updatedAtMillis = now)
    }

    private fun parseServerUtcMillis(raw: String): Long = runCatching {
        val normalized = raw.trim().replace(' ', 'T')
        LocalDateTime.parse(normalized).toInstant(ZoneOffset.UTC).toEpochMilli()
    }.getOrDefault(0L)

    private fun saveBehaviourProfile(profile: UserBehaviourProfile) {
        fun jsonArray(values: Set<String>) = JSONArray().also { out -> values.forEach { value -> out.put(value) } }
        val events = JSONArray()
        profile.events.forEach { event ->
            events.put(
                JSONObject()
                    .put("kind", event.kind.name)
                    .put("value", event.value)
                    .put("timestampMillis", event.timestampMillis)
            )
        }
        val defaults = profile.adDefaults
        val root = JSONObject()
            .put("username", profile.username)
            .put("updatedAtMillis", profile.updatedAtMillis)
            .put("events", events)
            .put(
                "adDefaults",
                JSONObject()
                    .put("durationHours", defaults.durationHours)
                    .put("targetStyles", jsonArray(defaults.targetStyles))
                    .put("targetSearchTerms", jsonArray(defaults.targetSearchTerms))
                    .put("targetTowns", jsonArray(defaults.targetTowns))
                    .put("targetComarques", jsonArray(defaults.targetComarques))
                    .put("targetFollowers", defaults.targetFollowers)
                    .put("matchAllCriteria", defaults.matchAllCriteria)
            )
        store.edit().putString(behaviourKey(profile.username), root.toString()).apply()
        scheduleGuestStateSync()
    }

    private fun loadSession(): AppAccount? {
        if (ServerApiRepository.isConfigured()) {
            return ServerApiRepository.loadSavedAccount(appContext)
        }
        val username = store.getString("session_username", null) ?: return null
        return DemoData.accounts.firstOrNull { it.username.equals(username, ignoreCase = true) }
    }

    private fun loadAccountProfiles(): Map<String, AccountProfile> {
        val accounts = buildList {
            addAll(DemoData.accounts)
            currentAccount?.takeIf { current -> none { it.username.equals(current.username, ignoreCase = true) } }?.let(::add)
        }
        return accounts.associate { account ->
            val fallback = defaultProfile(account)
            account.username to AccountProfile(
                username = account.username,
                displayName = store.getString(profileKey(account.username, "name"), fallback.displayName) ?: fallback.displayName,
                bio = store.getString(profileKey(account.username, "bio"), fallback.bio) ?: fallback.bio,
                photoUri = store.getString(profileKey(account.username, "photo"), fallback.photoUri) ?: fallback.photoUri,
                homeTown = store.getString(profileKey(account.username, "home_town"), fallback.homeTown) ?: fallback.homeTown,
                discipline = store.getString(profileKey(account.username, "discipline"), fallback.discipline) ?: fallback.discipline,
                companyName = store.getString(profileKey(account.username, "company_name"), fallback.companyName) ?: fallback.companyName,
                castText = store.getString(profileKey(account.username, "cast_text"), fallback.castText) ?: fallback.castText,
                publisherName = store.getString(profileKey(account.username, "publisher_name"), fallback.publisherName) ?: fallback.publisherName,
                booksText = store.getString(profileKey(account.username, "books_text"), fallback.booksText) ?: fallback.booksText,
                worksText = store.getString(profileKey(account.username, "works_text"), fallback.worksText) ?: fallback.worksText,
                spacesText = store.getString(profileKey(account.username, "spaces_text"), fallback.spacesText) ?: fallback.spacesText
            )
        }
    }

    private fun defaultProfile(account: AppAccount): AccountProfile = AccountProfile(
        username = account.username,
        displayName = account.displayName,
        bio = when (account.role) {
            AccountRole.SUPER_ADMIN -> "Administració general de l'Agenda Valenciana."
            AccountRole.GROUP -> ""
            AccountRole.AGENCY -> "Agència amb diversos grups gestionats."
            AccountRole.LABEL -> "Segell amb diversos grups gestionats."
            AccountRole.VENUE -> "Espai cultural amb concerts coeditats."
            AccountRole.PROMOTER -> "Promotora amb concerts i grups gestionats."
            AccountRole.USER -> "Usuari de l'Agenda Valenciana."
        },
        photoUri = when (account.role) {
            AccountRole.GROUP -> DemoData.artistDemoPhotoUri(account.displayName)
            AccountRole.USER -> DemoData.userDemoPhotoUri(account.displayName)
            else -> null
        }
    )

    private fun persistProfile(profile: AccountProfile) {
        store.edit()
            .putString(profileKey(profile.username, "name"), profile.displayName)
            .putString(profileKey(profile.username, "bio"), profile.bio)
            .putString(profileKey(profile.username, "photo"), profile.photoUri)
            .putString(profileKey(profile.username, "home_town"), profile.homeTown)
            .putString(profileKey(profile.username, "discipline"), profile.discipline)
            .putString(profileKey(profile.username, "company_name"), profile.companyName)
            .putString(profileKey(profile.username, "cast_text"), profile.castText)
            .putString(profileKey(profile.username, "publisher_name"), profile.publisherName)
            .putString(profileKey(profile.username, "books_text"), profile.booksText)
            .putString(profileKey(profile.username, "works_text"), profile.worksText)
            .putString(profileKey(profile.username, "spaces_text"), profile.spacesText)
            .apply()
    }

    private fun profileKey(username: String, field: String): String = "profile_${username.lowercase()}_$field"

    /** Migració única: preserva els Llocs de v261 dins del compte que estava actiu. */
    private fun migrateLegacyPlacesIfNecessary(username: String?) {
        if (store.getBoolean("places_scoped_v262_migrated", false)) return
        val legacyLocations = if (store.contains("preferred_locations")) loadStringSet("preferred_locations")
            else store.getString("home_town", null)?.takeIf(String::isNotBlank)?.let(::setOf).orEmpty()
        store.edit()
            .putStringSet(userCacheKey("preferred_locations", username), legacyLocations)
            .putStringSet(userCacheKey("preferred_comarques", username), loadStringSet("preferred_comarques"))
            .putString(userCacheKey("preferred_location_radii", username),
                store.getString("preferred_location_radii", "{}") ?: "{}")
            .putBoolean("places_scoped_v262_migrated", true)
            .commit()
    }

    private fun loadUserPreferences(username: String? = currentAccount?.username): UserPreferences {
        migrateLegacyPlacesIfNecessary(username)
        val locations = loadStringSet(userCacheKey("preferred_locations", username))
        return UserPreferences(
            preferredLocations = locations,
            preferredComarques = loadStringSet(userCacheKey("preferred_comarques", username)),
            favoriteArtists = loadStringSet("favorite_artists"),
            valencianFirst = store.getBoolean(userCacheKey("valencian_first",username),
                if (username != null && legacyFeedPrefsOwner == username)
                    store.getBoolean("valencian_first",true) else true),
            showSuggestions = store.getBoolean(userCacheKey("show_suggestions",username),
                if (username != null && legacyFeedPrefsOwner == username)
                    store.getBoolean("show_suggestions",true) else true),
            locationRadiiKm = runCatching {
                val json = JSONObject(store.getString(userCacheKey("preferred_location_radii", username), "{}") ?: "{}")
                val map = linkedMapOf<String, Int>()
                val keys = json.keys()
                while (keys.hasNext()) {
                    val town = keys.next()
                    if (locations.any { it.equals(town, ignoreCase = true) }) {
                        map[town] = PersonalizationOptions.validRadius(json.optInt(town, 30))
                    }
                }
                map
            }.getOrDefault(emptyMap()),
            discoveryLevel = runCatching {
                DiscoveryLevel.valueOf(store.getString(userCacheKey("discovery_level",username),
                    if (username != null && legacyFeedPrefsOwner == username)
                        store.getString("discovery_level", "BALANCED") else "BALANCED") ?: "BALANCED")
            }.getOrDefault(DiscoveryLevel.BALANCED)
        )
    }

    private fun loadConcertViewMode(): ConcertViewMode {
        val raw = store.getString("concert_view_mode", ConcertViewMode.LIST.name)
        val mode = runCatching { ConcertViewMode.valueOf(raw ?: ConcertViewMode.LIST.name) }
            .getOrDefault(ConcertViewMode.LIST)
        return if (mode == ConcertViewMode.VISUAL) ConcertViewMode.LIST else mode
    }

    private fun saveEmailPreferences(value: EmailPreferences) {
        store.edit()
            .putString("email_digest_frequency", value.frequency.wireValue)
            .putBoolean("email_recommendations", value.recommendations)
            .putBoolean("email_followed_artists", value.followedArtists)
            .putBoolean("email_saved_reminders", value.savedReminders)
            .putBoolean("email_personalized_ads", value.personalizedAds)
            .apply()
    }

    private fun loadEmailPreferences(): EmailPreferences = EmailPreferences(
        frequency = EmailDigestFrequency.fromWire(store.getString("email_digest_frequency", "NONE")),
        recommendations = store.getBoolean("email_recommendations", false),
        followedArtists = store.getBoolean("email_followed_artists", false),
        savedReminders = store.getBoolean("email_saved_reminders", false),
        personalizedAds = store.getBoolean("email_personalized_ads", false)
    )

    private fun loadAlertPreferences(): AlertPreferences = AlertPreferences(
        communityMessages = store.getBoolean("alert_community", true),
        followedArtist = store.getBoolean("alert_followed", true),
        homeTown = store.getBoolean("alert_town", true),
        homeComarca = store.getBoolean("alert_comarca", true),
        favoriteStyle = store.getBoolean("alert_style", false),
        dayBefore = store.getBoolean("alert_day_before", true),
        ticketUpdates = store.getBoolean("alert_tickets", true),
        changes = store.getBoolean("alert_changes", true),
        suggestions = store.getBoolean("alert_suggestions", true),
        followedFrequency = runCatching {
            BellFollowedFrequency.valueOf(store.getString("alert_followed_frequency", "WEEKLY").orEmpty())
        }.getOrDefault(BellFollowedFrequency.WEEKLY)
    )

    private fun artistInitials(artist: String): String = artist
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }

    private fun loadIssuedTickets(): List<PurchasedTicket> {
        val raw = store.getString("issued_tickets_v1", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val status = runCatching {
                        TicketStatus.valueOf(item.optString("status", TicketStatus.ACTIVE.name))
                    }.getOrDefault(TicketStatus.ACTIVE)
                    add(
                        PurchasedTicket(
                            code = item.getString("code"),
                            concertId = item.getInt("concertId"),
                            buyerUsername = item.getString("buyerUsername"),
                            buyerDisplayName = item.optString("buyerDisplayName", item.getString("buyerUsername")),
                            status = status,
                            purchasedAtMillis = item.optLong("purchasedAtMillis", System.currentTimeMillis()),
                            usedAtMillis = if (item.has("usedAtMillis") && !item.isNull("usedAtMillis")) item.getLong("usedAtMillis") else null,
                            serverId = if (item.has("serverId") && !item.isNull("serverId")) item.getLong("serverId") else null
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun saveIssuedTickets() {
        val array = JSONArray()
        issuedTickets.forEach { ticket ->
            array.put(
                JSONObject()
                    .put("code", ticket.code)
                    .put("concertId", ticket.concertId)
                    .put("buyerUsername", ticket.buyerUsername)
                    .put("buyerDisplayName", ticket.buyerDisplayName)
                    .put("status", ticket.status.name)
                    .put("serverId", ticket.serverId ?: JSONObject.NULL)
                    .put("purchasedAtMillis", ticket.purchasedAtMillis)
                    .put("usedAtMillis", ticket.usedAtMillis ?: JSONObject.NULL)
            )
        }
        store.edit().putString("issued_tickets_v1", array.toString()).apply()
    }

    private fun ticketConcertIdsForCurrentUser(): Set<Int> {
        val username = currentAccount?.username ?: return emptySet()
        return issuedTickets
            .asSequence()
            .filter { it.buyerUsername.equals(username, ignoreCase = true) }
            .map { it.concertId }
            .toSet()
    }

    private fun ticketQuantityMapForCurrentUser(): Map<Int, Int> {
        val username = currentAccount?.username ?: return emptyMap()
        return issuedTickets
            .filter { it.buyerUsername.equals(username, ignoreCase = true) }
            .groupingBy { it.concertId }
            .eachCount()
    }

    private fun refreshTicketPurchaseCache() {
        ticketPurchases = ticketConcertIdsForCurrentUser()
        ticketQuantityByConcert = ticketQuantityMapForCurrentUser()
    }

    private fun buildGuestStatePayload(): JSONObject {
        val attendance = JSONObject()
        attendanceByConcert.forEach { (id, status) ->
            if (status != AttendanceStatus.NONE) attendance.put(id.toString(), status.name)
        }
        return JSONObject()
            .put("saved", JSONArray(savedConcerts.sorted()))
            .put("liked", JSONArray(likedConcerts.sorted()))
            .put("alerts", JSONArray(alertConcerts.sorted()))
            .put("followed_artists", JSONArray(followedArtists.sorted()))
            .put("followed_organizers", JSONArray(followedOrganizers.sorted()))
            .put("followed_venues", JSONArray(followedVenues.sorted()))
            .put("followed_categories", JSONArray(followedCategories.sorted()))
            .put("artist_alerts", JSONArray(artistAlerts.sorted()))
            .put("hidden_events", JSONArray(hiddenConcertIds.sorted()))
            .put("blocked_artists", JSONArray(blockedArtists.sorted()))
            .put("blocked_categories", JSONArray(blockedCategories.map { it.name }.sorted()))
            .put("blocked_subcategories", JSONArray(blockedSubcategories.sorted()))
            .put("recent_searches", JSONArray(recentSearchTerms.sorted()))
            .put("attendance", attendance)
            .put("behaviour", store.getString(behaviourKey(null), "").orEmpty())
            .put("updated_at_ms", System.currentTimeMillis())
    }

    private fun jsonIntSet(root: JSONObject, key: String): Set<Int> = buildSet {
        val arr = root.optJSONArray(key) ?: return@buildSet
        for (i in 0 until arr.length()) arr.optInt(i).takeIf { it > 0 }?.let(::add)
    }

    private fun jsonStringSet(root: JSONObject, key: String): Set<String> = buildSet {
        val arr = root.optJSONArray(key) ?: return@buildSet
        for (i in 0 until arr.length()) arr.optString(i).trim().takeIf(String::isNotBlank)?.let(::add)
    }

    private fun mergeGuestStatePayload(
        root: JSONObject,
        persist: Boolean,
        pushToAccount: Boolean = false,
        refreshFeed: Boolean = true
    ) {
        val preferenceSignatureBefore = forYouPreferenceSignature()
        val guestSaved = jsonIntSet(root, "saved")
        val guestLiked = jsonIntSet(root, "liked")
        val guestAlerts = jsonIntSet(root, "alerts")
        val guestArtists = jsonStringSet(root, "followed_artists")
        val guestOrganizers = jsonStringSet(root, "followed_organizers")
        val guestVenues = jsonStringSet(root, "followed_venues")
        val guestCategories = CategoryTaxonomy.canonicalizeFollowSet(jsonStringSet(root, "followed_categories"))
        val guestArtistAlerts = jsonStringSet(root, "artist_alerts")
        val guestHiddenEvents = jsonIntSet(root, "hidden_events")
        val guestBlockedArtists = jsonStringSet(root, "blocked_artists")
        val guestBlockedCategories = jsonStringSet(root, "blocked_categories")
            .mapNotNull { name -> enumValues<CulturalCategory>().firstOrNull { it.name == name } }.toSet()
        val guestBlockedSubcategories = CategoryTaxonomy.canonicalizeFollowSet(jsonStringSet(root, "blocked_subcategories"))
            .filter(CategoryTaxonomy::isAnySubcategoryKey).toSet()
        val guestSearches = jsonStringSet(root, "recent_searches")

        // Guardem la base del compte per saber què és realment nou i pujar només la
        // diferència del guest. La unió és sempre account ∪ guest, mai guest -> account.
        val accountSaved = savedConcerts
        val accountLiked = likedConcerts
        val accountAlerts = alertConcerts
        val accountArtists = followedArtists
        val accountOrganizers = followedOrganizers
        val accountVenues = followedVenues
        val accountCategories = followedCategories
        val accountArtistAlerts = artistAlerts
        val accountHiddenEvents = hiddenConcertIds
        val accountBlockedArtists = blockedArtists
        val accountBlockedCategories = blockedCategories
        val accountBlockedSubcategories = blockedSubcategories
        val accountAttendance = attendanceByConcert

        savedConcerts = accountSaved + guestSaved
        likedConcerts = accountLiked + guestLiked
        alertConcerts = accountAlerts + guestAlerts
        followedArtists = accountArtists + guestArtists
        followedOrganizers = accountOrganizers + guestOrganizers
        followedVenues = accountVenues + guestVenues
        followedCategories = CategoryTaxonomy.canonicalizeFollowSet(accountCategories + guestCategories)
        artistAlerts = accountArtistAlerts + guestArtistAlerts
        hiddenConcertIds = accountHiddenEvents + guestHiddenEvents
        blockedArtists = accountBlockedArtists + guestBlockedArtists
        blockedCategories = accountBlockedCategories + guestBlockedCategories
        blockedSubcategories = CategoryTaxonomy.canonicalizeFollowSet(accountBlockedSubcategories + guestBlockedSubcategories)
        recentSearchTerms = recentSearchTerms + guestSearches

        val guestAttendance = linkedMapOf<Int, AttendanceStatus>()
        val attendanceRoot = root.optJSONObject("attendance")
        if (attendanceRoot != null) {
            val merged = accountAttendance.toMutableMap()
            attendanceRoot.keys().forEach { key ->
                val id = key.toIntOrNull() ?: return@forEach
                val status = runCatching { AttendanceStatus.valueOf(attendanceRoot.optString(key)) }.getOrNull() ?: AttendanceStatus.NONE
                if (status != AttendanceStatus.NONE) {
                    guestAttendance[id] = status
                    // En conflicte, l'estat que ja tenia el compte preval.
                    if (merged[id] == null) merged[id] = status
                }
            }
            attendanceByConcert = merged
        }

        // L'historial d'afinitat també es fusiona per a comptes preexistents. Abans
        // només s'importava si el compte no tenia cap historial, perdent els senyals guest.
        val guestBehaviourEvents = parseGuestBehaviourEvents(root)
        if (guestBehaviourEvents.isNotEmpty()) {
            val now = System.currentTimeMillis()
            val cutoff = now - 180L * 86_400_000L
            val mergedEvents = (behaviourProfile.events + guestBehaviourEvents)
                .asSequence()
                .filter { it.timestampMillis >= cutoff }
                .distinctBy { Triple(it.kind, normalizeSearchValue(it.value), it.timestampMillis) }
                .sortedBy { it.timestampMillis }
                .toList()
                .takeLast(1000)
            behaviourProfile = behaviourProfile.copy(
                username = currentAccount?.username.orEmpty(),
                events = mergedEvents,
                updatedAtMillis = now
            )
            saveBehaviourProfile(behaviourProfile)
        }

        if (persist) {
            saveIntSet(userCacheKey("saved_concerts"), savedConcerts)
            saveIntSet(userCacheKey("liked_concerts"), likedConcerts)
            saveIntSet(userCacheKey("alert_concerts"), alertConcerts)
            saveStringSet(userCacheKey("followed_artists"), followedArtists)
            saveStringSet(userCacheKey("followed_organizers"), followedOrganizers)
            saveStringSet(userCacheKey("followed_venues"), followedVenues)
            saveStringSet(userCacheKey("followed_categories"), followedCategories)
            saveStringSet(userCacheKey("artist_alerts"), artistAlerts)
            // Mantindre la freqüència que el convidat havia triat abans de crear compte.
            // Si el compte ja tenia una preferència, preval la configuració del compte.
            val migratedArtistFrequencies = store.edit()
            guestArtistAlerts.forEach { artist ->
                val guestKey = artistBellFrequencyKey(artist, null)
                val accountKey = artistBellFrequencyKey(artist)
                val frequency = store.getString(guestKey, null)
                if (!frequency.isNullOrBlank() && !store.contains(accountKey)) {
                    migratedArtistFrequencies.putString(accountKey, frequency)
                }
            }
            migratedArtistFrequencies.apply()
            saveIntSet(userCacheKey("hidden_concerts"), hiddenConcertIds)
            saveStringSet(userCacheKey("blocked_artists"), blockedArtists)
            saveStringSet(userCacheKey("blocked_categories"), blockedCategories.map { it.name }.toSet())
            saveStringSet(userCacheKey("blocked_subcategories"), blockedSubcategories)
            saveStringSet(searchTermsKey(currentAccount?.username), recentSearchTerms)
            saveAttendanceCache()
        }

        applyAutomaticDefaultFeedMode()

        if (pushToAccount && currentAccount != null) {
            val migrationPosts = mutableListOf<Pair<String, JSONObject>>()
            (guestSaved - accountSaved).forEach { id ->
                migrationPosts += "events/save.php" to guestMergeEventPayload(id).put("saved", true)
            }
            (guestLiked - accountLiked).forEach { id ->
                migrationPosts += "events/like.php" to guestMergeEventPayload(id).put("liked", true)
            }
            (guestAlerts - accountAlerts).forEach { id ->
                migrationPosts += "user-state/event-alert.php" to guestMergeEventPayload(id).put("active", true)
            }
            guestAttendance.forEach { (id, status) ->
                if (accountAttendance[id] == null) {
                    migrationPosts += "user-state/attendance.php" to guestMergeEventPayload(id).put("attendance", status.name)
                }
            }
            (guestArtists - accountArtists).forEach { value ->
                migrationPosts += "user-state/follow.php" to JSONObject().put("entity_type", "ARTIST").put("entity_key", value).put("entity_label", value).put("following", true)
            }
            (guestOrganizers - accountOrganizers).forEach { value ->
                migrationPosts += "user-state/follow.php" to JSONObject().put("entity_type", "ORGANIZER").put("entity_key", value).put("entity_label", profileFor(value).displayName.ifBlank { value }).put("following", true)
            }
            (guestVenues - accountVenues).forEach { value ->
                migrationPosts += "user-state/follow.php" to JSONObject().put("entity_type", "VENUE").put("entity_key", value).put("entity_label", value).put("following", true)
            }
            (guestCategories - accountCategories).forEach { value ->
                migrationPosts += "user-state/follow.php" to JSONObject().put("entity_type", "CATEGORY").put("entity_key", value).put("entity_label", value).put("following", true)
            }
            (guestHiddenEvents - accountHiddenEvents).forEach { id ->
                migrationPosts += "visibility/index.php" to JSONObject()
                    .put("action", "add").put("kind", "EVENT").put("target", id.toString())
            }
            (guestBlockedArtists - accountBlockedArtists).forEach { artist ->
                migrationPosts += "visibility/index.php" to JSONObject()
                    .put("action", "add").put("kind", "ARTIST").put("target", artist)
            }
            (guestBlockedCategories - accountBlockedCategories).forEach { category ->
                migrationPosts += "visibility/index.php" to JSONObject()
                    .put("action", "add").put("kind", "CATEGORY").put("target", category.name)
            }
            // Els bloquejos de subcategories del convidat també s'han de migrar
            // al servidor en iniciar sessió, sense tocar els bloquejos ja existents.
            (guestBlockedSubcategories - accountBlockedSubcategories).forEach { code ->
                migrationPosts += "visibility/index.php" to JSONObject()
                    .put("action", "add").put("kind", "CATEGORY").put("target", code)
            }
            migrationPosts += guestBehaviourPostsForAccount(guestBehaviourEvents)
            // Primer queda tot durable al disc; només després es pot buidar el guest.
            enqueuePendingServerPosts(migrationPosts)
            backgroundExecutor.execute { flushPendingServerPosts() }
        }
        val preferenceSignatureAfter = forYouPreferenceSignature()
        if (refreshFeed && preferenceSignatureAfter != preferenceSignatureBefore) {
            Log.d("AgendaPerf", "guestState merged prefs · refresh Per a tu")
            invalidateFeedCache()
        } else if (refreshFeed) {
            Log.d("AgendaPerf", "guestState same prefs · keep Per a tu")
        }
    }

    private fun parseGuestBehaviourEvents(root: JSONObject): List<UserBehaviourEvent> {
        val raw = root.optString("behaviour").trim()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val behaviourRoot = JSONObject(raw)
            val array = behaviourRoot.optJSONArray("events") ?: JSONArray()
            val now = System.currentTimeMillis()
            val cutoff = now - 180L * 86_400_000L
            buildList {
                val start = (array.length() - 1000).coerceAtLeast(0)
                for (index in start until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val kind = runCatching { BehaviourKind.valueOf(item.optString("kind")) }.getOrNull() ?: continue
                    val value = item.optString("value").trim()
                    val timestamp = item.optLong("timestampMillis", now)
                    if (value.isNotBlank() && timestamp >= cutoff) {
                        add(UserBehaviourEvent(kind, value, timestamp))
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    /** Payload mínim segur: la migració guest no depén que l'esdeveniment estiga ara carregat al feed. */
    private fun guestMergeEventPayload(eventId: Int): JSONObject {
        val concert = concertById(eventId) ?: advertisementConcertCache[eventId]
        return if (concert != null) eventServerPayload(concert) else JSONObject().put("event_id", eventId)
    }

    private fun guestBehaviourPostsForAccount(events: List<UserBehaviourEvent>): List<Pair<String, JSONObject>> {
        if (events.isEmpty() || currentAccount == null) return emptyList()
        return events.takeLast(500).mapNotNull { event ->
            val action = when (event.kind) {
                BehaviourKind.SEARCH -> "SEARCH_RELATED"
                BehaviourKind.TOWN, BehaviourKind.COMARCA, BehaviourKind.ARTIST, BehaviourKind.ORGANIZER,
                BehaviourKind.VENUE, BehaviourKind.STYLE, BehaviourKind.LANGUAGE, BehaviourKind.CATEGORY -> "SEARCH_RELATED"
                BehaviourKind.EVENT_VIEW -> "OPEN_EVENT"
                else -> null
            } ?: return@mapNotNull null
            val entityType = when (event.kind) {
                BehaviourKind.TOWN -> "TOWN"
                BehaviourKind.COMARCA -> "COMARCA"
                BehaviourKind.ARTIST -> "ARTIST"
                BehaviourKind.ORGANIZER -> "ORGANIZER"
                BehaviourKind.VENUE -> "VENUE"
                BehaviourKind.STYLE -> "STYLE"
                BehaviourKind.LANGUAGE -> "LANGUAGE"
                BehaviourKind.CATEGORY -> "CATEGORY"
                BehaviourKind.EVENT_VIEW -> "EVENT"
                else -> "RAW"
            }
            val concert = if (entityType == "EVENT") event.value.toIntOrNull()?.let(::concertById) else null
            "affinities/record.php" to affinityServerPayload(action, entityType, event.value, event.value, concert)
        }
    }

    /**
     * Una vegada el guest està fusionat i totes les diferències estan en la cua del compte,
     * buidem l'estat anònim. Es conserva guest_id perquè reports/submissions continuen vinculats
     * al dispositiu, però eixes preferències ja no poden tornar-se a importar en un login futur.
     */
    private fun clearGuestStateAfterAccountMerge() {
        val editor = store.edit()
        listOf(
            "saved_concerts", "liked_concerts", "alert_concerts", "artist_alerts",
            "followed_artists", "followed_organizers", "followed_venues", "followed_categories",
            "attendance_cache", "seen_batecs", "hidden_concerts", "blocked_artists", "blocked_categories", "blocked_subcategories"
        ).forEach { base -> editor.remove(userCacheKey(base, null)) }
        editor
            .remove(searchTermsKey(null))
            .remove(behaviourKey(null))
            .remove("for_you_2m_v5_snapshot_guest")
            .remove("for_you_2m_v5_snapshot_guest_updated_at")
            .remove("for_you_2m_v5_snapshot_guest_prefs")
            .apply()

        // El servidor guest també queda buit. No usem linkGuestStateToAccount perquè la
        // migració ja s'ha fet camp a camp i així mai pot substituir l'estat del compte.
        backgroundExecutor.execute {
            ServerApiRepository.syncGuestState(
                appContext,
                JSONObject()
                    .put("saved", JSONArray())
                    .put("liked", JSONArray())
                    .put("alerts", JSONArray())
                    .put("followed_artists", JSONArray())
                    .put("followed_organizers", JSONArray())
                    .put("followed_venues", JSONArray())
                    .put("followed_categories", JSONArray())
                    .put("artist_alerts", JSONArray())
                    .put("hidden_events", JSONArray())
                    .put("blocked_artists", JSONArray())
                    .put("blocked_categories", JSONArray())
            .put("blocked_subcategories", JSONArray())
                    .put("recent_searches", JSONArray())
                    .put("attendance", JSONObject())
                    .put("behaviour", "")
                    .put("updated_at_ms", System.currentTimeMillis())
            )
        }
    }

    private fun scheduleGuestStateSync() {
        if (currentAccount != null || !ServerApiRepository.isConfigured() || guestSyncScheduled) return
        guestSyncScheduled = true
        Handler(Looper.getMainLooper()).postDelayed({
            guestSyncScheduled = false
            if (currentAccount == null) {
                val payload = buildGuestStatePayload()
                backgroundExecutor.execute { ServerApiRepository.syncGuestState(appContext, payload) }
            }
        }, 750L)
    }

    private fun migrateUsernameScopedCache(oldUsername: String, newUsername: String) {
        val oldSuffix = "_${oldUsername.trim().lowercase()}"
        val newSuffix = "_${newUsername.trim().lowercase()}"
        if (oldSuffix == newSuffix) return
        val editor = store.edit()
        store.all.forEach { (key, value) ->
            if (!key.endsWith(oldSuffix)) return@forEach
            val newKey = key.removeSuffix(oldSuffix) + newSuffix
            when (value) {
                is String -> editor.putString(newKey, value)
                is Boolean -> editor.putBoolean(newKey, value)
                is Int -> editor.putInt(newKey, value)
                is Long -> editor.putLong(newKey, value)
                is Float -> editor.putFloat(newKey, value)
                is Set<*> -> editor.putStringSet(newKey, value.filterIsInstance<String>().toSet())
            }
        }
        editor.apply()
    }

    private fun userCacheKey(base: String, username: String? = currentAccount?.username): String =
        "${base}_${username?.trim()?.lowercase().takeUnless { it.isNullOrBlank() } ?: "guest"}"

    private fun resetLocalSocialCounters() {
        savedOptimisticCounts.clear()
        likedOptimisticCounts.clear()
        savedOptimisticAt.clear()
        likedOptimisticAt.clear()
        savedMutationVersions.clear()
        likedMutationVersions.clear()
        socialCountsByEvent = emptyMap()
        invalidateFollowSuggestions()
    }

    private fun loadUserStateCache(username: String) {
        resetLocalSocialCounters()
        savedConcerts = loadIntSet(userCacheKey("saved_concerts", username))
        likedConcerts = loadIntSet(userCacheKey("liked_concerts", username))
        alertConcerts = loadIntSet(userCacheKey("alert_concerts", username))
        artistAlerts = loadStringSet(userCacheKey("artist_alerts", username))
        blockedArtists = loadStringSet(userCacheKey("blocked_artists", username))
        hiddenConcertIds = loadIntSet(userCacheKey("hidden_concerts", username))
        blockedSubcategories = CategoryTaxonomy.canonicalizeFollowSet(loadStringSet(userCacheKey("blocked_subcategories", username)))
        blockedCategories = loadStringSet(userCacheKey("blocked_categories", username))
            .mapNotNull { code -> enumValues<CulturalCategory>().firstOrNull { it.name == code } }.toSet()
        onVisibilityExclusionsChanged()
        followedArtists = loadStringSet(userCacheKey("followed_artists", username))
        followedOrganizers = loadStringSet(userCacheKey("followed_organizers", username))
        followedVenues = loadStringSet(userCacheKey("followed_venues", username))
        followedCategories = CategoryTaxonomy.canonicalizeFollowSet(loadStringSet(userCacheKey("followed_categories", username)))
        festivalArtistSelections = loadFestivalArtistSelections()
        seenBatecKeys = loadStringSet(userCacheKey("seen_batecs", username))
        viewedBatecArtistsAt = loadViewedBatecArtistsAt()
        startupBatecSnapshot = runCatching { loadBatecStartupSnapshot() }.getOrDefault(emptyList())
        batecPhotoByArtist = runCatching { loadBatecPhotoCache() }.getOrDefault(emptyMap())
        batecPhotoCacheRevision += 1L
        cachedActiveStoriesBaseKey = null
        attendanceByConcert = loadAttendance()
    }

    private fun loadAttendance(): Map<Int, AttendanceStatus> {
        val raw = store.getString(userCacheKey("attendance_cache"), "").orEmpty()
        if (raw.isBlank()) return emptyMap()
        return runCatching {
            val obj = JSONObject(raw)
            buildMap {
                obj.keys().forEach { key ->
                    val id = key.toIntOrNull() ?: return@forEach
                    val status = runCatching { AttendanceStatus.valueOf(obj.optString(key)) }.getOrNull() ?: AttendanceStatus.NONE
                    if (status != AttendanceStatus.NONE) put(id, status)
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun saveAttendanceCache() {
        val obj = JSONObject()
        attendanceByConcert.forEach { (id, status) -> if (status != AttendanceStatus.NONE) obj.put(id.toString(), status.name) }
        store.edit().putString(userCacheKey("attendance_cache"), obj.toString()).apply()
        scheduleGuestStateSync()
    }

    private fun forYouSnapshotKey(): String = "for_you_v268_unified_snapshot_${currentAccount?.username?.lowercase() ?: "guest"}"
    private fun forYouSnapshotTimestampKey(): String = "${forYouSnapshotKey()}_updated_at"
    private fun forYouSnapshotDbRevisionKey(): String = "${forYouSnapshotKey()}_db_revision_v328"
    private fun forYouSnapshotHardSignatureKey(): String = "${forYouSnapshotKey()}_hard_sig_v329"
    private fun forYouSnapshotPreferenceSignatureKey(): String = "${forYouSnapshotKey()}_prefs"
    private fun forYouSnapshotFingerprintsKey(): String = "${forYouSnapshotKey()}_events"
    private fun forYouSnapshotReasonsKey(): String = "${forYouSnapshotKey()}_diagnostic_v280"
    private fun forYouSnapshotFollowedIdsKey(): String = "${forYouSnapshotKey()}_followed_ids_v324"
    private fun loadForYouSnapshotFollowedIds(): Set<Int> =
        store.getString(forYouSnapshotFollowedIdsKey(), "").orEmpty()
            .split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()

    private fun loadForYouSnapshotReasons(): Map<Int, String> = runCatching {
        val root = JSONObject(store.getString(forYouSnapshotReasonsKey(), "{}").orEmpty())
        buildMap {
            root.keys().forEach { key ->
                val id = key.toIntOrNull() ?: return@forEach
                put(id, root.optString(key))
            }
        }
    }.getOrDefault(emptyMap())

    private fun loadForYouSnapshot(): List<Int> = store.getString(forYouSnapshotKey(), "")
        .orEmpty().split(',').mapNotNull { it.trim().toIntOrNull() }

    private fun loadForYouSnapshotTimestamp(): Long =
        store.getLong(forYouSnapshotTimestampKey(), 0L)

    /** Signatura estable dels senyals personals que poden canviar la selecció. */
    private fun forYouPreferenceSignature(): Int = buildString {
        // Invalidar snapshots antics que reomplien el feed amb propostes sense afinitat.
        append("for-you-v288-styles-estil-four-routes-15days-100km").append('|')
        // El dia cultural acaba a les 05:00 (Europe/Madrid), no a mitjanit UTC.
        // Evita invalidacions de snapshot innecessàries entre les 00:00 i les 05:00.
        append(agendaToday()).append('|')
        append(currentAccount?.serverUserId ?: 0).append('|')
        append(followedArtists.map(::normalizeSearchValue).sorted()).append('|')
        append(followedOrganizers.map(::normalizeSearchValue).sorted()).append('|')
        append(followedVenues.map(::normalizeSearchValue).sorted()).append('|')
        append(followedCategories.map(::normalizeSearchValue).sorted()).append('|')
        append(userPreferences.preferredLocations.map { town ->
            "${normalizeSearchValue(town)}:${userPreferences.radiusForLocation(town)}"
        }.sorted()).append('|')
        append(userPreferences.preferredComarques.map(::normalizeSearchValue).sorted()).append('|')
        append(userPreferences.discoveryLevel.name).append('|')
        append(userPreferences.valencianFirst).append('|')
        append(userPreferences.showSuggestions).append('|')
        append(savedConcerts.sorted()).append('|')
        append(recentSearchTerms.map(::normalizeSearchValue).sorted()).append('|')
        append(likedConcerts.sorted()).append('|')
        append(ticketPurchases.sorted()).append('|')
        append(hiddenConcertIds.sorted()).append('|')
        append(blockedArtists.map(::normalizeSearchValue).sorted()).append('|')
        append(blockedCategories.map { it.name }.sorted()).append('|')
        append(blockedSubcategories.sorted()).append('|')
        append(attendanceByConcert.entries.sortedBy { it.key }.joinToString { "${it.key}:${it.value.name}" }).append('|')
        behaviourProfile.events.asSequence()
            .sortedWith(compareBy<UserBehaviourEvent> { it.kind.name }
                .thenBy { normalizeSearchValue(it.value) }
                .thenBy { it.timestampMillis })
            .forEach { event ->
                append(event.kind.name).append(':')
                append(normalizeSearchValue(event.value)).append(':')
                append(event.timestampMillis).append(';')
            }
    }.hashCode()

    /** v329 · Clau ESTABLE durant 30 minuts. Els m'agrada, guardats, clics,
     * lectures i cerques influïxen en el pròxim recàlcul, però no destruïxen
     * el feed davant d'un simple toc. Canvis explícits de gustos i exclusions sí.
     */
    private fun forYouHardPreferenceSignature(): Int = buildString {
        append("for-you-v329-half-hour").append('|')
        append(agendaToday()).append('|')
        append(currentAccount?.serverUserId ?: 0).append('|')
        append(followedArtists.map(::normalizeSearchValue).sorted()).append('|')
        append(followedOrganizers.map(::normalizeSearchValue).sorted()).append('|')
        append(followedVenues.map(::normalizeSearchValue).sorted()).append('|')
        append(followedCategories.map(::normalizeSearchValue).sorted()).append('|')
        append(userPreferences.preferredLocations.map { town ->
            "${normalizeSearchValue(town)}:${userPreferences.radiusForLocation(town)}"
        }.sorted()).append('|')
        append(userPreferences.preferredComarques.map(::normalizeSearchValue).sorted()).append('|')
        append(userPreferences.discoveryLevel.name).append('|')
        append(userPreferences.valencianFirst).append('|')
        append(userPreferences.showSuggestions).append('|')
        append(hiddenConcertIds.sorted()).append('|')
        append(blockedArtists.map(::normalizeSearchValue).sorted()).append('|')
        append(blockedCategories.map { it.name }.sorted()).append('|')
        append(blockedSubcategories.sorted())
    }.hashCode()

    /** Empremta dels camps d'un esdeveniment que poden alterar Per a tu. */
    private fun forYouEventFingerprint(concert: Concert): Int = buildString {
        append(concert.id).append('|')
        append(concert.artist).append('|').append(concert.lineup.joinToString("~")).append('|')
        append(concert.subcategoryCodes.sorted().joinToString("~")).append('|')
        append(concert.languages.sorted().joinToString("~")).append('|')
        append(concert.language).append('|')
        append(concert.organizers.joinToString("~")).append('|')
        append(concert.coEditorUsernames.sorted().joinToString("~")).append('|')
        append(concert.date).append('|').append(concert.endDate).append('|')
        append(concert.scheduleDates.joinToString("~")).append('|')
        append(concert.town).append('|').append(concert.comarca).append('|')
        append(concert.category.name).append('|').append(concert.styles.joinToString("~")).append('|')
        append(concert.languages.joinToString("~")).append('|').append(concert.language).append('|')
        append(concert.creatorUserId ?: 0).append('|')
        append(concert.ticketAvailable).append('|').append(concert.ticketUrl).append('|')
        append(concert.ticketPrice)
    }.hashCode()

    private fun forYouEventFingerprints(source: List<Concert>): Map<Int, Int> =
        source.associate { it.id to forYouEventFingerprint(it) }

    private fun loadForYouEventFingerprints(): Map<Int, Int> {
        val raw = store.getString(forYouSnapshotFingerprintsKey(), "").orEmpty()
        if (raw.isBlank()) return emptyMap()
        return runCatching {
            val root = JSONObject(raw)
            buildMap {
                root.keys().forEach { key ->
                    val id = key.toIntOrNull() ?: return@forEach
                    put(id, root.optInt(key))
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun expireForYouSnapshot() {
        // Invalidació en memòria + disc. La generació és part de la clau de càlcul,
        // i per això es descarten també els resultats que arriben tard.
        forYouRecalculationGeneration += 1L
        if (feedMode == FeedMode.FOR_YOU) {
            forYouRecalculationPending = true
            forYouRecalculationError = null
            val generation = forYouRecalculationGeneration
            // Un problema d'hidratació, xarxa o càlcul no pot deixar el refresc
            // carregant indefinidament sense dir a l'usuari què ha passat.
            Handler(Looper.getMainLooper()).postDelayed({
                if (forYouRecalculationGeneration == generation && forYouRecalculationPending) {
                    finishForYouRefresh(
                        visibleConcertsState.size,
                        "Per a tu no ha acabat d'actualitzar-se. Revisa la connexió i torna-ho a provar."
                    )
                }
            }, 35_000L)
        }
        store.edit()
            .remove(forYouSnapshotKey())
            .putLong(forYouSnapshotTimestampKey(), 0L)
            .remove(forYouSnapshotDbRevisionKey())
            .remove(forYouSnapshotHardSignatureKey())
            .remove(forYouSnapshotPreferenceSignatureKey())
            .remove(forYouSnapshotFingerprintsKey())
            .remove(forYouSnapshotReasonsKey())
            .remove(forYouSnapshotFollowedIdsKey())
            .apply()
        forYouSelectionExplanations = emptyMap()
        startupForYouPreviewReady = false
        startupForYouPreview = emptyList()
        cachedVisibleConcertsKey = null
        cachedVisibleConcertsFinal = false
        calculatingVisibleConcertsKey = null
        residentForYouReady = false
        residentForYouDataRevision = -1L
        residentForYouConcerts = emptyList()
        residentForYouPreferenceSignature = Int.MIN_VALUE
        residentForYouDay = ""
        cachedFollowSuggestionsEpoch = Long.MIN_VALUE
        cachedFollowSuggestionsBySlot.clear()
        cachedActiveStoriesBaseKey = null
        feedRenderRevision += 1L
    }

    private fun saveForYouSnapshot(ids: List<Int>, source: List<Concert>, reasons: Map<Int, String>) {
        val normalized = ids.distinct()
        val now = System.currentTimeMillis()
        // La revisió s'obté ací, en el fil del personalitzador, mai al main.
        val dbRevision = runCatching {
            com.example.agendavalenciana.data.AgendaLocalDatabase.revision(appContext)
        }.getOrDefault(Long.MIN_VALUE)
        val fingerprints = JSONObject()
        forYouEventFingerprints(source).forEach { (id, fingerprint) ->
            fingerprints.put(id.toString(), fingerprint)
        }
        val reasonJson = JSONObject()
        normalized.forEach { id -> reasons[id]?.let { reasonJson.put(id.toString(), it) } }
        store.edit()
            .putString(forYouSnapshotKey(), normalized.joinToString(","))
            .putLong(forYouSnapshotTimestampKey(), now)
            .putLong(forYouSnapshotDbRevisionKey(), dbRevision)
            .putInt(forYouSnapshotHardSignatureKey(), forYouHardPreferenceSignature())
            .putInt(forYouSnapshotPreferenceSignatureKey(), forYouPreferenceSignature())
            .putString(forYouSnapshotFingerprintsKey(), fingerprints.toString())
            .putString(forYouSnapshotReasonsKey(), reasonJson.toString())
            .putString(forYouSnapshotFollowedIdsKey(),
                materializedFollowedForYou(source).map { it.id }.joinToString(","))
            .apply()
        cachedFollowSuggestionsEpoch = Long.MIN_VALUE
        cachedFollowSuggestionsBySlot.clear()
        cachedActiveStoriesBaseKey = null
        // Recalcular només quan venç la finestra, si la persona continua en Per a tu.
        // La versió anterior mostrava indefinidament la selecció perquè els HIT
        // de memòria no comprovaven el temps transcorregut.
        val savedAt = now
        Handler(Looper.getMainLooper()).postDelayed({
            if (loadForYouSnapshotTimestamp() == savedAt && feedMode == FeedMode.FOR_YOU &&
                search.isBlank() && filters.isEmpty()) {
                cachedVisibleConcertsKey = null
                residentForYouReady = false
                scheduleVisibleConcertsRefresh(0L)
            }
        }, forYouFastPreviewMaxAge + 1000L)
        // La pujada mai bloqueja l'Android ni el primer frame. El servidor és
        // una segona còpia; SQLite és la font immediata del dispositiu.
        if (currentAccount?.serverUserId?.let { it > 0 } == true && ServerApiRepository.isConfigured()) {
            val serverIds = normalized.toList()
            val serverSignature = forYouHardPreferenceSignature()
            val serverDay = agendaToday().toString()
            val serverAccountId = currentAccount?.serverUserId
            backgroundExecutor.execute {
                // No usar mai el token d'un compte nou per pujar dades del vell.
                if (currentAccount?.serverUserId == serverAccountId &&
                    forYouHardPreferenceSignature() == serverSignature) {
                    ServerApiRepository.writePreparedForYouCache(appContext, serverSignature, serverDay, serverIds)
                        .onFailure { Log.d("AgendaPerf", "forYouServerCacheWrite: ${it.javaClass.simpleName}") }
                }
            }
        }
    }

    private fun loadFestivalArtistSelections(): Map<Int, Set<String>> {
        val raw = store.getString(userCacheKey("festival_artist_selections"), null) ?: return emptyMap()
        return runCatching {
            val root = JSONObject(raw)
            buildMap {
                root.keys().forEach { key ->
                    val eventId = key.toIntOrNull() ?: return@forEach
                    val arr = root.optJSONArray(key) ?: return@forEach
                    val artists = buildSet {
                        for (i in 0 until arr.length()) arr.optString(i).trim().takeIf(String::isNotBlank)?.let { add(it) }
                    }
                    if (artists.isNotEmpty()) put(eventId, artists)
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun saveFestivalArtistSelections() {
        val root = JSONObject()
        festivalArtistSelections.forEach { (eventId, artists) ->
            root.put(eventId.toString(), JSONArray().also { arr -> artists.sorted().forEach { value -> arr.put(value) } })
        }
        store.edit().putString(userCacheKey("festival_artist_selections"), root.toString()).apply()
    }

    private fun loadIntSet(key: String): Set<Int> =
        loadStringSet(key).mapNotNull { it.toIntOrNull() }.toSet()

    private fun loadStringSet(key: String): Set<String> =
        store.getStringSet(key, emptySet())?.toSet().orEmpty()

    private fun saveIntSet(key: String, values: Set<Int>) {
        saveStringSet(key, values.map(Int::toString).toSet())
    }

    private fun saveStringSet(key: String, values: Set<String>) {
        store.edit().putStringSet(key, values).apply()
        scheduleGuestStateSync()
    }

    private fun <T> toggled(source: Set<T>, value: T): Set<T> =
        if (value in source) source - value else source + value

    companion object {
        private const val STORY_LIFETIME_MILLIS = 48L * 60L * 60L * 1000L
        private const val MIN_VISIBLE_BATEC_GROUPS = 10
        private const val FOR_YOU_WINDOW_MONTHS = 2L
        private const val AUTO_BATEC_AFFINITY_THRESHOLD = 48
        private const val AUTO_BATEC_MAX_ITEMS = 40
    }

}
