package com.example.agendavalenciana.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.util.Base64
import com.example.agendavalenciana.BuildConfig
import com.example.agendavalenciana.model.CategoryTaxonomy
import com.example.agendavalenciana.model.PersonalizationOptions
import com.example.agendavalenciana.model.AccountRole
import com.example.agendavalenciana.model.AdPlacement
import com.example.agendavalenciana.model.EventAdvertisement
import com.example.agendavalenciana.model.EventTransportInfo
import com.example.agendavalenciana.model.EventDailyStart
import com.example.agendavalenciana.model.EventInfo
import com.example.agendavalenciana.model.AppAccount
import com.example.agendavalenciana.model.Concert
import com.example.agendavalenciana.model.CulturalCategory
import com.example.agendavalenciana.model.CommunityBoardInquiry
import com.example.agendavalenciana.model.CommunityBoardInboxMessage
import com.example.agendavalenciana.model.CommunityAccessMode
import com.example.agendavalenciana.model.CommunityChatMessage
import com.example.agendavalenciana.model.CommunityGroup
import com.example.agendavalenciana.model.CommunityGroupType
import com.example.agendavalenciana.model.CommunityMemberRequest
import com.example.agendavalenciana.model.EmailDigestFrequency
import com.example.agendavalenciana.model.EmailPreferences
import com.example.agendavalenciana.model.PurchasedTicket
import com.example.agendavalenciana.model.Story
import com.example.agendavalenciana.model.TicketStatus
import com.example.agendavalenciana.model.TicketOption
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.text.Normalizer
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.math.roundToInt
import org.json.JSONArray
import org.json.JSONObject

/**
 * Client HTTPS de l'API central d'Agenda Valenciana.
 *
 * MySQL i les seues credencials existeixen només al servidor. L'APK rep un token
 * de sessió i treballa exclusivament amb https://quimerafest.com/app/api/.
 */
object ServerApiRepository {
    private const val PREFS = "agenda_server_session"
    private const val GUEST_PREFS = "agenda_guest_identity"
    private const val CACHE_DIR = "remote_agenda"
    private const val API_CACHE = "api_events_0192.json"
    private const val ENTITY_IMAGES_CACHE = "api_entity_images.json"
    private const val EVENT_VISUALS_CACHE = "api_event_visuals_0192.json"
    private const val SCHEDULE_DATES_CACHE = "api_schedule_dates_020810dx.json"
    private const val EVENT_INFO_CACHE = "api_event_info_020810dz.json"
    private const val BATECS_CACHE = "api_batecs_page_0.json"
    private const val CONNECT_TIMEOUT = 12_000
    private const val READ_TIMEOUT = 25_000
    private const val REPORT_WEB_APP_URL = "https://script.google.com/macros/s/AKfycbzendY8IigeM_eyyfYAb0hFYSSOgD9ss5ZRp4l69KRsNvRDWie9DJwhe-zHeFOhHwVB2A/exec"
    @Volatile private var entityNamesCacheMtime: Long = Long.MIN_VALUE
    @Volatile private var entityNamesCache: Map<String, List<String>> = emptyMap()
    @Volatile private var entityAffinityCacheMtime: Long = Long.MIN_VALUE
    @Volatile private var entityAffinityCache: Map<String, EntityAffinityMetadata> = emptyMap()
    // 0.20.8.1: el catàleg d'imatges NO es torna a llegir ni parsejar per cada card/avatar.
    // Es conserva en RAM i només es reconstrueix quan canvia realment el fitxer de cache.
    @Volatile private var entityImagesCacheMtime: Long = Long.MIN_VALUE
    @Volatile private var entityImagesMemoryCache: EntityImageCatalog = EntityImageCatalog.EMPTY
    @Volatile private var entityImagesLastStatCheckMillis: Long = 0L
    @Volatile private var parsedEventsMemoryRevision: Long = Long.MIN_VALUE
    @Volatile private var parsedEventsMemory: List<Concert> = emptyList()
    // Enriquiments de dates i horaris també es necessiten en la primera pantalla.
    // Conservem els JSON interpretats en memòria fins que canvien al disc.
    @Volatile private var scheduleDatesCacheMtime: Long = Long.MIN_VALUE
    @Volatile private var scheduleDatesMemoryCache: Map<Int, ScheduleEventMeta> = emptyMap()
    @Volatile private var eventInfoCacheMtime: Long = Long.MIN_VALUE
    @Volatile private var eventInfoMemoryCache: Map<Int, EventInfo> = emptyMap()
    @Volatile private var eventVisualsCacheMtime: Long = Long.MIN_VALUE
    @Volatile private var eventVisualsMemoryCache: Map<Int, EventVisual> = emptyMap()
    private val reportedResolvedCardImages = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    // 10az: enriquiment de l’agenda en paral·lel. Evita encadenar 3 peticions abans de sync.
    private val syncEnrichmentExecutor = Executors.newFixedThreadPool(4)
    // 10ep: la persistència de la primera descàrrega no retarda el primer feed.
    private val firstLoadCacheExecutor = Executors.newSingleThreadExecutor()
    // La primera descàrrega publica un primer lot abans de guardar tot el JSON.
    // El cercador global no ha de donar per complet el cens fins que acabe eixa escriptura.
    // Serialitza les escriptures d'una cache entre WorkManager, UI i primera arrancada.
    private val eventCatalogWriteLock = Any()
    @Volatile private var firstLoadCachePersisting = false
    fun isFirstLoadCachePersisting(): Boolean = firstLoadCachePersisting

    private const val ENTITY_IMAGE_STAT_INTERVAL_MS = 30_000L

    private fun analyticsDeviceName(context: Context): String {
        val prefs = context.getSharedPreferences("agenda_installation_identity", Context.MODE_PRIVATE)
        var installationId = prefs.getString("installation_id", null).orEmpty().trim()
        if (installationId.isBlank()) {
            installationId = UUID.randomUUID().toString().replace("-", "").take(12)
            prefs.edit().putString("installation_id", installationId).apply()
        }
        val maker = Build.MANUFACTURER.orEmpty().trim().replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
        }
        val model = Build.MODEL.orEmpty().trim()
        val hardware = listOf(maker, model).filter { it.isNotBlank() }.distinct().joinToString(" ").ifBlank { "Android" }
        val appKind = if (BuildConfig.IS_PRO_APP) "Android PRO" else "Android"
        return "$appKind · $hardware · #$installationId"
    }
    private val entityCombiningMarksRegex = Regex("\\p{Mn}+")
    private val entityWhitespaceRegex = Regex("\\s+")

    data class ReportResultNotification(
        val reportId: String,
        val eventId: Int?,
        val title: String,
        val message: String,
        val resolvedAt: String,
        val reviewStatus: String,
        val reviewRound: Int
    )

    data class LoginResult(val account: AppAccount, val token: String)
    data class ProfessionalAccountSettings(
        val role: String,
        val phone: String,
        val emailVerified: Boolean,
        val email: String?
    )

    fun getProfessionalAccountSettings(context: Context): Result<ProfessionalAccountSettings> =
        getAuthenticatedJson(context, "account/profile-settings.php").mapCatching { root ->
            ProfessionalAccountSettings(
                role = root.optString("role", "PUBLIC").uppercase(Locale.ROOT),
                phone = root.optString("phone", ""),
                emailVerified = root.optBoolean("email_verified", false),
                email = root.optString("email", "").takeIf(String::isNotBlank)
            )
        }

    fun saveProfessionalAccountSettings(context: Context, professional: Boolean, phone: String): Result<ProfessionalAccountSettings> =
        postAuthenticatedJson(context, "account/profile-settings.php", JSONObject()
            .put("role", if (professional) "PROFESSIONAL" else "PUBLIC")
            .put("phone", if (professional) phone.trim() else "")
        ).mapCatching { root ->
            val role = root.optString("role", "PUBLIC").uppercase(Locale.ROOT)
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("role", role).apply()
            ProfessionalAccountSettings(
                role = role,
                phone = root.optString("phone", ""),
                emailVerified = root.optBoolean("email_verified", false),
                email = root.optString("email", "").takeIf(String::isNotBlank)
            )
        }

    data class AccountIdentityUpdate(
        val username: String,
        val email: String?,
        val homeTown: String,
        val emailVerificationRequired: Boolean = false
    )

    data class OnboardingRankings(
        val artists: Map<String, Int> = emptyMap(),
        val categories: Map<String, Int> = emptyMap(),
        val towns: Map<String, Int> = emptyMap()
    )

    fun fetchOnboardingRankings(): Result<OnboardingRankings> = runCatching {
        val root = jsonRequest("onboarding/rankings.php", "GET", JSONObject(), null)
        fun ranking(name: String): Map<String, Int> {
            val out = linkedMapOf<String, Int>()
            val rows = root.optJSONArray(name) ?: JSONArray()
            for (i in 0 until rows.length()) {
                val row = rows.optJSONObject(i) ?: continue
                val label = row.optString("label").trim()
                if (label.isNotBlank()) out[label.lowercase(Locale.ROOT)] = row.optInt("count", 0)
            }
            return out
        }
        OnboardingRankings(
            artists = ranking("artists"),
            categories = ranking("categories"),
            towns = ranking("towns")
        )
    }

    data class BatecPage(
        val items: List<Story>,
        val hasMore: Boolean,
        val nextOffset: Int
    )
    private data class ProfessionalIdentity(val displayName: String, val managedArtists: Set<String>)
    data class EventCreateResult(val concert: Concert)
    data class CommunitySubmitResult(
        val eventId: Int,
        val published: Boolean,
        val approvedCount: Int,
        val autoPublishEnabled: Boolean
    )
    data class OwnSubmittedEvent(
        val eventId: Int,
        val title: String,
        val subtitle: String,
        val artists: List<String>,
        val organizers: List<String>,
        val date: String,
        val endDate: String,
        val time: String,
        val endTime: String,
        val town: String,
        val comarca: String,
        val venue: String,
        val category: CulturalCategory,
        val styles: List<String>,
        val price: Int,
        val ticketEnabled: Boolean,
        val ticketUrl: String,
        val ticketSalePending: Boolean = false,
        val description: String,
        val status: String,
        val reviewStatus: String,
        val publicationMode: String,
        val posterUrl: String?
    )
    data class PublisherAccess(
        val role: AccountRole,
        val displayName: String?,
        val managedArtists: Set<String>
    )
    data class CommunityReviewItem(
        val eventId: Int,
        val submitterUserId: Int,
        val publicationMode: String,
        val reviewStatus: String,
        val title: String,
        val date: String,
        val town: String,
        val venue: String,
        val username: String,
        val email: String
    )
    data class ProfessionalClaimReviewItem(
        val claimId: Long,
        val claimantUserId: Int,
        val entityName: String,
        val profileType: String,
        val relation: String,
        val contactEmail: String,
        val contactPhone: String,
        val message: String,
        val username: String,
        val accountEmail: String,
        val matchedProfileName: String
    )
    private data class EventVisual(
        val imageSource: String = "AUTO",
        val entityName: String? = null,
        val eventImageUrl: String? = null,
        val preferredImageUrl: String? = null,
        val resolvedImageUrl: String? = null,
        val resolvedImageSource: String? = null,
        val resolvedImageStatus: String = "PENDING",
        val organizers: List<String> = emptyList(),
        val organizerImageUrl: String? = null,
        val focusX: Float = 0.5f,
        val focusY: Float = 0.5f,
        val scale: Float = 1f
    )

    data class EntityImageCatalog(
        val artists: Map<String, String> = emptyMap(),
        val venues: Map<String, String> = emptyMap(),
        val towns: Map<String, String> = emptyMap(),
        val categories: Map<String, String> = emptyMap(),
        val organizers: Map<String, String> = emptyMap()
    ) {
        companion object { val EMPTY = EntityImageCatalog() }
    }
    data class EntityImageRow(
        val type: String,
        val name: String,
        val imageUrl: String?,
        val sourceUrl: String?,
        val imageOrigin: String,
        val imageStatus: String
    )

    /** Metadades d'ENTITATS usades per afinitats. El text original es conserva. */
    data class EntityAffinityMetadata(
        val type: String,
        val name: String,
        val language: String = "",
        val category: String = "",
        val styles: List<String> = emptyList(),
        val subcategoryCodes: List<String> = emptyList(),
        val isInfantil: Boolean = false
    )
    private data class EntityCatalogBundle(
        val images: EntityImageCatalog,
        val names: Map<String, List<String>>,
        val affinities: Map<String, EntityAffinityMetadata>
    )
    data class ProfessionalProfileImportRow(
        val profileType: String,
        val name: String,
        val imageUrl: String?,
        val sourceUrl: String?,
        val imageOrigin: String,
        val stylePrimary: String?,
        val styleSecondary: String?,
        val styleTags: String?,
        val languagePrimary: String?,
        val languageSecondary: String?,
        val languageTags: String?,
        val sourceOrigin: String?,
        val confidence: String?,
        val notes: String?,
        val agencyNames: List<String> = emptyList()
    )

    data class SaveEventResult(val saved: Boolean, val savedCount: Int)

    data class EventSocialCount(
        val eventId: Int,
        val savedCount: Int,
        val likeCount: Int
    )

    data class UserFollowState(
        val entityType: String,
        val entityKey: String,
        val entityLabel: String
    )

    data class UserEventState(
        val eventId: Int,
        val saved: Boolean,
        val liked: Boolean,
        val attendance: String
    )

    data class RemoteAffinity(
        val entityType: String,
        val entityKey: String,
        val entityLabel: String,
        val searchCount: Int,
        val interactionCount: Int,
        val score: Double,
        val effectiveScore: Double,
        val permanent: Boolean,
        val lastAction: String,
        val lastSeenAt: String,
        val decayPercent: Int
    )

    data class RemoteNotificationPreferences(
        val followedArtistConcerts: Boolean = true,
        val followedArtistNews: Boolean = true,
        val tasteSuggestions: Boolean = true,
        val nearbySuggestions: Boolean = true
    )

    data class UserStateSnapshot(
        val follows: List<UserFollowState>,
        val eventStates: List<UserEventState>,
        val eventAlerts: Set<Int>,
        val affinities: List<RemoteAffinity>,
        val notificationPreferences: RemoteNotificationPreferences? = null
    )

    data class AffinityAnalyticsRow(
        val label: String,
        val total: Int,
        val users: Int,
        val type: String = "",
        val secondaryLabel: String = "",
        val secondaryType: String = ""
    )

    data class AffinityAnalytics(
        val signals: Int = 0,
        val users: Int = 0,
        val searches: Int = 0,
        val opens: Int = 0,
        val saves: Int = 0,
        val going: Int = 0,
        val topSearches: List<AffinityAnalyticsRow> = emptyList(),
        val topEntities: List<AffinityAnalyticsRow> = emptyList(),
        val audienceTowns: List<AffinityAnalyticsRow> = emptyList(),
        val audienceComarques: List<AffinityAnalyticsRow> = emptyList(),
        val topTowns: List<AffinityAnalyticsRow> = emptyList(),
        val topComarques: List<AffinityAnalyticsRow> = emptyList(),
        val topLanguages: List<AffinityAnalyticsRow> = emptyList(),
        val topStyles: List<AffinityAnalyticsRow> = emptyList(),
        val topCategories: List<AffinityAnalyticsRow> = emptyList(),
        val topArtists: List<AffinityAnalyticsRow> = emptyList(),
        val topOrganizers: List<AffinityAnalyticsRow> = emptyList(),
        val topVenues: List<AffinityAnalyticsRow> = emptyList(),
        val coaffinities: List<AffinityAnalyticsRow> = emptyList()
    )
    data class TicketValidationResult(
        val outcome: String,
        val message: String,
        val ticket: PurchasedTicket?
    )

    data class TicketOptionsInfo(
        val capacity: Int?,
        val options: List<TicketOption>
    )

    data class StripePaymentStart(
        val orderId: Long,
        val clientSecret: String,
        val publishableKey: String,
        val amountCents: Long,
        val currency: String,
        val merchantName: String,
        val mode: String
    )

    data class StripePaymentStatus(
        val orderId: Long,
        val status: String,
        val tickets: List<PurchasedTicket>
    )

    data class SpecialServicePaymentStart(
        val orderId: Long,
        val clientSecret: String,
        val publishableKey: String,
        val amountCents: Long,
        val currency: String,
        val merchantName: String,
        val mode: String
    )

    data class SpecialServicePaidItem(
        val productName: String,
        val quantity: Int,
        val unitPriceCents: Long
    )

    data class SpecialServicePaidOrder(
        val orderId: Long,
        val serviceName: String,
        val status: String,
        val totalCents: Long,
        val qrPayload: String,
        val items: List<SpecialServicePaidItem>
    )

    data class SpecialServiceInspection(
        val orderId: Long,
        val eventId: Int,
        val serviceName: String,
        val status: String,
        val totalCents: Long,
        val items: List<SpecialServicePaidItem>
    )

    data class AdPaymentStart(
        val adId: Long,
        val clientSecret: String,
        val publishableKey: String,
        val amountCents: Long,
        val currency: String,
        val merchantName: String,
        val mode: String
    )

    data class AdPaymentStatus(
        val adId: Long,
        val status: String,
        val advertisement: EventAdvertisement?
    )

    data class BatecEligibility(
        val allowed: Boolean,
        val organizer: Boolean,
        val reason: String?,
        val opensAt: String?,
        val closesAt: String?
    )

    fun baseUrl(): String = BuildConfig.REMOTE_API_BASE_URL.trim().let {
        if (it.endsWith("/")) it else "$it/"
    }

    fun isConfigured(): Boolean = baseUrl().startsWith("https://", ignoreCase = true)

    /** Campanyes internes des de Perfil, validades per la sessió SUPER_ADMIN al servidor.
     * No s'han de confondre amb push FCM: l'API actual respon push_android=false. */
    fun adminNotificationPreview(context: Context, androidAccountsOnly: Boolean): Result<Int> = runCatching {
        val auth = token(context) ?: error("Cal iniciar sessió com a superadministrador.")
        val filters = JSONObject().put("all_users", true)
            .put("platform", if (androidAccountsOnly) "ANDROID" else "")
        jsonRequest("admin/notifications-preview.php", "POST",
            JSONObject().put("filters", filters), auth).getInt("count")
    }

    fun adminNotificationSend(
        context: Context, title: String, body: String, androidAccountsOnly: Boolean, previewCount: Int
    ): Result<JSONObject> = runCatching {
        require(title.trim().isNotEmpty() && body.trim().isNotEmpty()) { "Indica títol i missatge." }
        require(previewCount >= 0) { "Cal previsualitzar el públic destinatari." }
        val auth = token(context) ?: error("Cal iniciar sessió com a superadministrador.")
        val filters = JSONObject().put("all_users", true)
            .put("platform", if (androidAccountsOnly) "ANDROID" else "")
        jsonRequest("admin/notifications-send.php", "POST", JSONObject()
            .put("filters", filters)
            .put("title", title.trim().take(160))
            .put("body", body.trim().take(1200))
            .put("expected_count", previewCount)
            .put("confirmation", "ENVIAR"), auth)
    }

    fun token(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("token", null)

    /** Telemetria del Panell, separada de les afinitats que alimenten Per a tu. */
    fun trackUsage(
        context: Context,
        type: String,
        value: String = "",
        metadata: JSONObject = JSONObject()
    ) {
        val savedToken = token(context) ?: return
        val cleanType = type.trim().uppercase(Locale.ROOT)
        if (cleanType.isBlank()) return
        val enriched = runCatching { JSONObject(metadata.toString()) }.getOrElse { JSONObject() }
            .put("platform", "ANDROID")
            .put("app_version", BuildConfig.VERSION_NAME)
            .put("app_version_code", BuildConfig.VERSION_CODE)
            .put("device_name", analyticsDeviceName(context))
        val payload = JSONObject()
            .put("type", cleanType.take(64))
            .put("value", value.trim().take(500))
            .put("metadata", enriched)
        syncEnrichmentExecutor.execute {
            runCatching { jsonRequest("analytics/track.php", "POST", payload, savedToken) }
        }
    }

    /** Identitat anònima estable mentre no s'esborren les dades de l'app. */
    fun guestId(context: Context): String {
        val prefs = context.getSharedPreferences(GUEST_PREFS, Context.MODE_PRIVATE)
        prefs.getString("guest_id", null)?.takeIf { it.isNotBlank() }?.let { return it }
        val created = UUID.randomUUID().toString()
        prefs.edit().putString("guest_id", created).apply()
        return created
    }

    fun fetchGuestState(context: Context): Result<JSONObject> = runCatching {
        val id = URLEncoder.encode(guestId(context), StandardCharsets.UTF_8.name())
        val root = jsonRequest("guest/state.php?guest_id=$id", "GET", JSONObject(), null)
        root.optJSONObject("state") ?: JSONObject()
    }

    fun syncGuestState(context: Context, state: JSONObject): Result<Unit> = runCatching {
        val payload = JSONObject().put("guest_id", guestId(context)).put("state", state)
        jsonRequest("guest/state.php", "POST", payload, null)
        Unit
    }

    fun linkGuestStateToAccount(context: Context): Result<Unit> = runCatching {
        val savedToken = token(context) ?: error("Cal iniciar sessió.")
        val payload = JSONObject().put("guest_id", guestId(context)).put("link_to_account", true)
        jsonRequest("guest/state.php", "POST", payload, savedToken)
        Unit
    }

    /** Envia una incidència sobre un esdeveniment. Funciona amb compte o com a convidat. */
    fun submitEventReport(
        context: Context,
        eventId: Int,
        eventTitle: String,
        eventTown: String,
        eventDate: String,
        reason: String,
        details: String
    ): Result<Unit> = runCatching {
        val payload = JSONObject()
            .put("event_id", eventId)
            .put("event_title", eventTitle.take(255))
            .put("event_town", eventTown.take(190))
            .put("event_date", eventDate.take(32))
            .put("reason", reason)
            .put("details", details.trim().take(1500))
            .put("guest_id", guestId(context))
        jsonRequest("events/report.php", "POST", payload, token(context))
        Unit
    }

    fun fetchReportResultNotifications(context: Context): Result<List<ReportResultNotification>> = runCatching {
        val guest = URLEncoder.encode(guestId(context), StandardCharsets.UTF_8.name())

        // 10dc: font principal directa = Apps Script / full Report.
        // Això evita que una resolució depenga de MySQL o de report_resolution.php.
        val root = runCatching {
            absoluteJsonRequest(
                "$REPORT_WEB_APP_URL?action=report_results&guest_id=$guest",
                "GET",
                JSONObject()
            )
        }.getOrElse {
            // Compatibilitat/fallback amb el pont anterior si el desplegament de GS encara no s'ha actualitzat.
            jsonRequest("events/report-results.php?guest_id=$guest", "GET", JSONObject(), token(context))
        }

        val arr = root.optJSONArray("results") ?: JSONArray()
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val message = o.optString("message").trim()
                val reportId = o.optString("report_id").trim()
                if (message.isBlank() || reportId.isBlank()) continue
                add(ReportResultNotification(
                    reportId = reportId,
                    eventId = o.optInt("event_id").takeIf { it > 0 },
                    title = o.optString("title", "Resultat del teu avís"),
                    message = message,
                    resolvedAt = o.optString("resolved_at"),
                    reviewStatus = o.optString("review_status").trim().uppercase(),
                    reviewRound = o.optInt("review_round", 1).coerceAtLeast(1)
                ))
            }
        }
    }

    /**
     * Dret a rèplica d'un report descartat.
     * 10dg: usem el mateix events/report.php que ja està verificat per crear reports.
     * Així evitem la resposta HTML/redirect de Google Apps Script en connexions Android directes.
     */
    fun submitReportFollowup(context: Context, reportId: String, message: String): Result<Unit> = runCatching {
        require(reportId.isNotBlank()) { "Report no vàlid." }
        val clean = message.trim()
        require(clean.length >= 5) { "Explica breument per què vols que ho revisem de nou." }
        require(clean.length <= 1200) { "L’explicació és massa llarga. Resumeix-la en menys de 1.200 caràcters." }

        val payload = JSONObject()
            .put("report_id", reportId.trim())
            .put("guest_id", guestId(context))
            .put("message", clean)

        val root = jsonRequest("events/report.php?mode=followup", "POST", payload, token(context))
        if (!root.optBoolean("reopened", false)) {
            error(root.optString("message").ifBlank { root.optString("error") }.ifBlank {
                "No s’ha pogut enviar la nova explicació."
            })
        }
        Unit
    }

    fun loadSavedAccount(context: Context): AppAccount? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val token = prefs.getString("token", null)?.takeIf { it.isNotBlank() } ?: return null
        val username = prefs.getString("username", null)?.takeIf { it.isNotBlank() } ?: return null
        val id = prefs.getInt("user_id", 0)
        val role = roleFromServer(prefs.getString("role", null))
        // Referenciar token evita que una sessió parcial siga considerada vàlida.
        if (token.isBlank()) return null
        return AppAccount(
            id = if (id > 0) id else username.hashCode(),
            username = username,
            password = "",
            displayName = prefs.getString("display_name", username) ?: username,
            role = role,
            email = prefs.getString("email", null),
            managedArtists = prefs.getStringSet("managed_artists", emptySet()) ?: emptySet(),
            serverUserId = id.takeIf { it > 0 }
        )
    }

    fun register(
        context: Context,
        email: String,
        username: String,
        password: String,
        categories: Set<CulturalCategory>,
        artists: Set<String>,
        notifyConcerts: Boolean,
        notifyArtistNews: Boolean,
        notifyTaste: Boolean,
        notifyNearby: Boolean
    ): Result<LoginResult> = runCatching {
        val payload = JSONObject()
            .put("email", email.trim())
            .put("username", username.trim())
            .put("password", password)
            .put("device_name", analyticsDeviceName(context))
            .put("categories", JSONArray(categories.map { it.name.lowercase() }))
            .put("artists", JSONArray(artists.sorted()))
            .put("notifications", JSONObject()
                .put("followed_artist_concerts", notifyConcerts)
                .put("followed_artist_news", notifyArtistNews)
                .put("taste_suggestions", notifyTaste)
                .put("nearby_suggestions", notifyNearby))
        val response = jsonRequest("auth/register.php", "POST", payload, null)
        val token = response.getString("token")
        val user = response.getJSONObject("user")
        val serverUsername = user.getString("username")
        val userId = user.getInt("id")
        val roleText = user.optString("role", "USER")
        val account = AppAccount(
            id = userId,
            username = serverUsername,
            password = "",
            displayName = user.optString("display_name", serverUsername).ifBlank { serverUsername },
            role = roleFromServer(roleText),
            email = user.optString("email", "").takeIf { it.isNotBlank() },
            serverUserId = userId
        )
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("token", token)
            .putInt("user_id", userId)
            .putString("username", serverUsername)
            .putString("display_name", account.displayName)
            .putString("email", account.email)
            .putString("role", roleText)
            .apply()
        LoginResult(account, token)
    }

    fun updateMyAccount(
        context: Context,
        username: String,
        email: String,
        homeTown: String,
        displayName: String
    ): Result<AccountIdentityUpdate> = runCatching {
        val savedToken = token(context) ?: error("La sessió de servidor no està disponible. Torna a iniciar sessió.")
        val payload = JSONObject()
            .put("username", username.trim())
            .put("email", email.trim())
            .put("home_town", homeTown.trim())
        val root = jsonRequest("account/update.php", "POST", payload, savedToken)
        val user = root.getJSONObject("user")
        val updatedUsername = user.getString("username")
        val updatedEmail = user.optString("email", "").takeIf { it.isNotBlank() }
        val updatedTown = user.optString("home_town", "")
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("username", updatedUsername)
            .putString("email", updatedEmail)
            .putString("display_name", displayName.trim().ifBlank { updatedUsername })
            .apply()
        AccountIdentityUpdate(
            username = updatedUsername,
            email = updatedEmail,
            homeTown = updatedTown,
            emailVerificationRequired = root.optBoolean("email_verification_required", false)
        )
    }

    fun requestPasswordReset(email: String): Result<String> = runCatching {
        val payload = JSONObject().put("email", email.trim())
        val response = jsonRequest("auth/request-password-reset.php", "POST", payload, null)
        response.optString(
            "message",
            "Si existeix un compte amb eixe correu, rebràs un enllaç per canviar la contrasenya."
        )
    }

    fun sendWelcomeVerificationEmail(context: Context): Result<Unit> = runCatching {
        postAuthenticatedJson(context, "mail/welcome.php", JSONObject()).getOrThrow()
        Unit
    }

    fun loginWithGoogle(context: Context, idToken: String, nonce: String): Result<LoginResult> = runCatching {
        val payload = JSONObject()
            .put("id_token", idToken)
            .put("nonce", nonce)
            .put("device_name", analyticsDeviceName(context))
        val response = jsonRequest("auth/google.php", "POST", payload, null)
        val token = response.getString("token")
        val user = response.getJSONObject("user")
        val username = user.getString("username")
        val userId = user.getInt("id")
        val roleText = user.optString("role", "PUBLIC")
        val email = user.optString("email", "").takeIf { it.isNotBlank() }
        val identity = runCatching { fetchProfessionalIdentity(token) }.getOrNull()
        val displayName = identity?.displayName?.takeIf(String::isNotBlank)
            ?: user.optString("display_name").takeIf(String::isNotBlank)
            ?: username
        val managedArtists = identity?.managedArtists ?: emptySet()
        val account = AppAccount(
            id = userId,
            username = username,
            password = "",
            displayName = displayName,
            role = roleFromServer(roleText),
            email = email,
            managedArtists = managedArtists,
            serverUserId = userId
        )
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("token", token)
            .putInt("user_id", userId)
            .putString("username", username)
            .putString("display_name", displayName)
            .putString("email", email)
            .putString("role", roleText)
            .putStringSet("managed_artists", managedArtists)
            .apply()
        LoginResult(account, token)
    }

    /** Torna únicament l'id del compte comprovat, sense substituir ni guardar la sessió. */
    fun verifyTicketManagerCredentials(username: String, password: String): Result<Int> = runCatching {
        require(username.isNotBlank() && password.isNotBlank()) { "Cal usuari i contrasenya." }
        val response = jsonRequest(
            "auth/login.php", "POST",
            JSONObject().put("login", username.trim()).put("password", password), null
        )
        val user = response.getJSONObject("user")
        require(!user.optString("role", "USER").equals("USER", ignoreCase = true)) {
            "Este compte no té permisos per vendre entrades."
        }
        user.getInt("id")
    }

    fun login(context: Context, login: String, password: String): Result<LoginResult> = runCatching {
        val payload = JSONObject()
            .put("login", login.trim())
            .put("password", password)
            .put("device_name", analyticsDeviceName(context))
        val response = jsonRequest("auth/login.php", "POST", payload, null)
        val token = response.getString("token")
        val user = response.getJSONObject("user")
        val username = user.getString("username")
        val userId = user.getInt("id")
        val roleText = user.optString("role", "USER")
        val email = user.optString("email", "").takeIf { it.isNotBlank() }
        val identity = runCatching { fetchProfessionalIdentity(token) }.getOrNull()
        val displayName = identity?.displayName?.takeIf(String::isNotBlank)
            ?: user.optString("display_name").takeIf(String::isNotBlank)
            ?: username
        val managedArtists = identity?.managedArtists ?: emptySet()
        val account = AppAccount(
            id = userId,
            username = username,
            password = "",
            displayName = displayName,
            role = roleFromServer(roleText),
            email = email,
            managedArtists = managedArtists,
            serverUserId = userId
        )
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("token", token)
            .putInt("user_id", userId)
            .putString("username", username)
            .putString("display_name", displayName)
            .putString("email", email)
            .putString("role", roleText)
            .putStringSet("managed_artists", managedArtists)
            .apply()
        LoginResult(account, token)
    }

    private fun fetchProfessionalIdentity(token: String): ProfessionalIdentity {
        val connection = open("professional-profiles/mine.php", "GET", token)
        val root = JSONObject(readResponse(connection))
        if (!root.optBoolean("ok", false)) return ProfessionalIdentity("", emptySet())
        val managed = buildSet {
            val items = root.optJSONArray("managed_artists") ?: JSONArray()
            for (i in 0 until items.length()) items.optString(i).trim().takeIf(String::isNotBlank)?.let(::add)
        }
        return ProfessionalIdentity(root.optString("display_name"), managed)
    }

    fun logout(context: Context) {
        val savedToken = token(context)
        clearSession(context)
        if (!savedToken.isNullOrBlank()) {
            runCatching { jsonRequest("auth/logout.php", "POST", JSONObject(), savedToken) }
        }
    }

    fun clearSession(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    /**
     * 10ep · primer ús / cache buida.
     *
     * El camí complet de [fetchEvents] espera ENTITATS, visuals, horaris i info abans de
     * retornar. Això és correcte per a una sincronització normal, però en una instal·lació
     * nova deixa Inici buit durant massa temps. Ací fem una única petició d'esdeveniments,
     * materialitzem només una finestra cronològica per a UI i persistim la resposta completa
     * en una cua separada. Cap enriquiment forma part del temps fins al primer feed.
     */
    fun fetchEventsFastFirstLoad(context: Context, uiLimit: Int = 160): Result<List<Concert>> = runCatching {
        val started = SystemClock.elapsedRealtime()
        val connection = open("events/index.php", "GET", token(context))
        val raw = readResponse(connection)
        val networkDone = SystemClock.elapsedRealtime()
        val root = JSONObject(raw)
        if (!root.optBoolean("ok", false)) {
            error(root.optString("message", "Resposta no vàlida del servidor."))
        }
        val eventsJson = root.getJSONArray("events")
        val revision = root.optLong("revision", 0L)
        val rawEventCount = eventsJson.length()
        val today = java.time.LocalDate.now(java.time.ZoneId.of("Europe/Madrid"))
        val fromDate = today.minusMonths(1).toString()
        val toDate = today.plusMonths(2).toString()
        // Primera instal·lació: també respectem la mateixa finestra temporal de Per a tu.
        // Així no tornem a introduir un subconjunt arbitrari de 160 que després force un
        // altre canvi de feed quan SQLite acaba de persistir la resposta completa.
        val window = fastEventWindow(eventsJson, fromDate, toDate, maxOf(uiLimit, 1200))
        val parsed = parseEvents(window, context, loadCachedEntityImages(context), emptyList())
        Log.d(
            "AgendaPerf",
            "firstNetworkEvents network=${networkDone - started}ms parse=${SystemClock.elapsedRealtime() - networkDone}ms raw=$rawEventCount ui=${parsed.size}"
        )

        // Reparsegem `raw` dins de la cua per no compartir JSONArray mutable entre fils.
        val appContext = context.applicationContext
        firstLoadCachePersisting = true
        firstLoadCacheExecutor.execute {
            try {
                val persistStarted = SystemClock.elapsedRealtime()
                runCatching {
                    synchronized(eventCatalogWriteLock) {
                        // Evita que un primer GET antic sobreescriga un delta més recent.
                        val newerRevision = AgendaLocalDatabase.revision(appContext)
                        if (newerRevision < revision ||
                            (newerRevision == revision && !AgendaLocalDatabase.hasEvents(appContext))) {
                            val persistedRoot = JSONObject(raw)
                            val persistedEvents = persistedRoot.getJSONArray("events")
                            AgendaLocalDatabase.replaceFull(appContext, persistedEvents, revision)
                            val cacheDir = File(appContext.filesDir, CACHE_DIR).apply { mkdirs() }
                            File(cacheDir, API_CACHE).writeText(raw, StandardCharsets.UTF_8)
                        } else {
                            Log.i("AgendaSync", "firstLoad snapshot skipped: local=$newerRevision incoming=$revision")
                        }
                    }
                }.onFailure { error ->
                    Log.w("AgendaPerf", "firstNetwork cache persist failed: ${error.message}")
                }.onSuccess {
                    Log.d("AgendaPerf", "firstNetworkCachePersist ${SystemClock.elapsedRealtime() - persistStarted}ms · $rawEventCount rows")
                }

                // Els enriquiments s'escalfen després; no bloquegen ni la resposta ni la cache base.
                syncEnrichmentExecutor.execute { runCatching { entityCatalogForSync(appContext) } }
                syncEnrichmentExecutor.execute { runCatching { eventVisualMapForSync(appContext) } }
                syncEnrichmentExecutor.execute { runCatching { scheduleDatesMapForSync(appContext) } }
                syncEnrichmentExecutor.execute { runCatching { eventInfoMapForSync(appContext) } }
            } finally {
                firstLoadCachePersisting = false
            }
        }
        parsed
    }

    /** Selecció barata de la finestra inicial sense construir `Concert` per a tota la resposta. */
    private fun fastEventWindow(events: JSONArray, fromDate: String, toDate: String, limit: Int): JSONArray {
        val safeLimit = limit.coerceIn(20, 2000)
        val out = JSONArray()
        // L'API normalment arriba cronològica. Tot i això filtrem pels dos extrems perquè
        // el primer feed represente la mateixa finestra de dos mesos que la cache normal.
        for (i in 0 until events.length()) {
            if (out.length() >= safeLimit) break
            val obj = events.optJSONObject(i) ?: continue
            val date = sequenceOf(
                obj.optString("feed_instance_date"),
                obj.optString("selected_date"),
                obj.optString("event_date"),
                obj.optString("date")
            ).map(String::trim).firstOrNull { it.length >= 10 && it != "null" }?.take(10)
            if (date == null || (date >= fromDate && date <= toDate)) out.put(obj)
        }
        // Si el servidor no inclou dates parsejables, preferim mostrar alguna cosa abans que
        // deixar Inici buit.
        if (out.length() == 0) {
            for (i in 0 until minOf(events.length(), safeLimit)) events.optJSONObject(i)?.let(out::put)
        }
        return out
    }

    fun fetchEvents(context: Context, forceFullCatalog: Boolean = false): Result<List<Concert>> = synchronized(eventCatalogWriteLock) {
        runCatching {
        val cacheDir = File(context.filesDir, CACHE_DIR).apply { mkdirs() }
        val entityCacheMtimeBefore = File(cacheDir, ENTITY_IMAGES_CACHE).takeIf(File::exists)?.lastModified() ?: 0L

        // 10az: sync d’esdeveniments i enriquiments independents corren en paral·lel.
        // Les tres funcions conserven les seues caches i fan fallback a l’última còpia bona.
        val entityFuture = syncEnrichmentExecutor.submit<EntityImageCatalog> { entityCatalogForSync(context) }
        val visualsFuture = syncEnrichmentExecutor.submit<Map<Int, EventVisual>> { eventVisualMapForSync(context) }
        val scheduleFuture = syncEnrichmentExecutor.submit<Map<Int, ScheduleEventMeta>> { scheduleDatesMapForSync(context) }
        val infoFuture = syncEnrichmentExecutor.submit<Map<Int, EventInfo>> { eventInfoMapForSync(context) }

        // Primer intentem sincronització incremental. Si el servidor encara no
        // té sync.php, fem fallback transparent a l’endpoint complet antic.
        val localRevision = AgendaLocalDatabase.revision(context)
        val hasLocal = AgendaLocalDatabase.hasEvents(context)
        // v285: una cerca/perfil explícits poden forçar una reconciliació completa.
        // La revisió incremental no prova per si sola que una cache anterior siga completa.
        val incremental = if (forceFullCatalog) null else runCatching {
            val since = if (hasLocal) localRevision else 0L
            val connection = open("events/sync.php?since=$since", "GET", token(context))
            JSONObject(readResponse(connection))
        }.getOrNull()

        val entityCatalog = runCatching { entityFuture.get() }.getOrElse { loadCachedEntityImages(context) }
        val eventVisuals = runCatching { visualsFuture.get() }.getOrElse { loadCachedEventVisualMap(context) }
        val scheduleDates = runCatching { scheduleFuture.get() }.getOrElse { loadCachedScheduleDatesMap(context) }
        val eventInfo = runCatching { infoFuture.get() }.getOrElse { loadCachedEventInfoMap(context) }
        val entityCacheMtimeAfter = File(cacheDir, ENTITY_IMAGES_CACHE).takeIf(File::exists)?.lastModified() ?: 0L

        if (incremental != null && incremental.optBoolean("ok", false)) {
            val revision = incremental.optLong("revision", localRevision).coerceAtLeast(localRevision)
            val isFull = incremental.optBoolean("full", false)
            val upserts = incremental.optJSONArray("upserts") ?: JSONArray()
            val deleted = incremental.optJSONArray("deleted_ids") ?: JSONArray()
            if (!isFull && upserts.length() == 0 && deleted.length() == 0 &&
                parsedEventsMemoryRevision == revision && parsedEventsMemory.isNotEmpty()) {
                return@runCatching applyEventVisuals(applyEventInfo(applyScheduleDates(parsedEventsMemory, scheduleDates), eventInfo), eventVisuals)
            }

            // Si la memòria representa exactament la revisió local anterior, podem aplicar
            // el delta també en RAM: no rellegim tota SQLite ni reconstruïm el JSON complet.
            val canPatchMemory = !isFull &&
                parsedEventsMemoryRevision == localRevision && parsedEventsMemory.isNotEmpty() &&
                entityCacheMtimeBefore == entityCacheMtimeAfter
            if (isFull) {
                val fullEvents = incremental.optJSONArray("events")
                    ?: error("La sincronització completa no conté el catàleg d'esdeveniments")
                // No substituir dades bones per una resposta incompleta o malformada.
                if (hasLocal && fullEvents.length() == 0) error("Catàleg complet inesperadament buit")
                AgendaLocalDatabase.replaceFull(context, fullEvents, revision)
            } else {
                AgendaLocalDatabase.applyDelta(context, upserts, deleted, revision)
            }

            val parsed = if (canPatchMemory) {
                val replacedIds = buildSet<Int> {
                    for (i in 0 until upserts.length()) {
                        val item = upserts.optJSONObject(i) ?: continue
                        (item.optInt("event_id").takeIf { it > 0 } ?: item.optInt("id").takeIf { it > 0 })?.let(::add)
                    }
                }
                val deletedIds = buildSet<Int> {
                    for (i in 0 until deleted.length()) deleted.optInt(i).takeIf { it > 0 }?.let(::add)
                }
                val changed = applyEventInfo(applyScheduleDates(parseEvents(upserts, context, entityCatalog), scheduleDates), eventInfo)
                applyEventInfo(applyScheduleDates(
                    parsedEventsMemory.asSequence()
                        .filterNot { it.id in replacedIds || it.id in deletedIds }
                        .toList() + changed,
                    scheduleDates
                ), eventInfo).sortedWith(compareBy<Concert> { it.feedDate }.thenBy { it.time }.thenBy { it.id })
            } else {
                val eventsJson = AgendaLocalDatabase.loadEvents(context)
                // Manté el fitxer antic només com a fallback de compatibilitat. En deltes calents
                // SQLite + RAM ja són la font de cache i evitem reescriure megabytes.
                val root = JSONObject().put("ok", true).put("events", eventsJson).put("revision", revision)
                File(cacheDir, API_CACHE).writeText(root.toString(), StandardCharsets.UTF_8)
                applyEventInfo(applyScheduleDates(parseEvents(eventsJson, context, entityCatalog), scheduleDates), eventInfo)
            }
            parsedEventsMemoryRevision = revision
            parsedEventsMemory = parsed
            return@runCatching applyEventVisuals(parsed, eventVisuals)
        }

        val connection = open("events/index.php", "GET", token(context))
        val raw = readResponse(connection)
        val root = JSONObject(raw)
        if (!root.optBoolean("ok", false)) error(root.optString("message", "Resposta no vàlida del servidor."))
        val eventsJson = root.getJSONArray("events")
        val revision = root.optLong("revision", 0L)
        if (hasLocal && eventsJson.length() == 0) {
            error("El servidor ha retornat un catàleg buit; es conserva la còpia local")
        }
        Log.i("AgendaSync", "full catalog reconciliation: rows=${eventsJson.length()} revision=$revision forced=$forceFullCatalog")
        AgendaLocalDatabase.replaceFull(context, eventsJson, revision)
        File(cacheDir, API_CACHE).writeText(raw, StandardCharsets.UTF_8)
        val parsed = applyEventInfo(applyScheduleDates(parseEvents(eventsJson, context, entityCatalog), scheduleDates), eventInfo)
        parsedEventsMemoryRevision = revision
        parsedEventsMemory = parsed
        applyEventVisuals(parsed, eventVisuals)
        }
    }


    private fun entityCatalogForSync(context: Context): EntityImageCatalog {
        val cacheDir = File(context.filesDir, CACHE_DIR).apply { mkdirs() }
        val entityCacheFile = File(cacheDir, ENTITY_IMAGES_CACHE)
        val cached = loadCachedEntityImages(context)
        val fresh = entityCacheFile.exists() &&
            System.currentTimeMillis() - entityCacheFile.lastModified() < 15L * 60L * 1000L &&
            (cached.artists.isNotEmpty() || cached.venues.isNotEmpty() || cached.towns.isNotEmpty() ||
                cached.categories.isNotEmpty() || cached.organizers.isNotEmpty())
        if (fresh) return cached
        return runCatching {
            val entityRoot = jsonRequest("entities/images.php", "GET", JSONObject(), token(context))
            val bundle = parseEntityCatalogBundle(entityRoot)
            val parsed = bundle.images
            val hasImages = parsed.artists.isNotEmpty() || parsed.venues.isNotEmpty() || parsed.towns.isNotEmpty() ||
                parsed.categories.isNotEmpty() || parsed.organizers.isNotEmpty()
            if (hasImages) {
                entityCacheFile.writeText(entityRoot.toString(), StandardCharsets.UTF_8)
                val mtime = entityCacheFile.lastModified()
                entityImagesMemoryCache = parsed
                entityImagesCacheMtime = mtime
                entityNamesCache = deduplicateEntityNames(bundle.names)
                entityNamesCacheMtime = mtime
                entityAffinityCache = bundle.affinities
                entityAffinityCacheMtime = mtime
                entityImagesLastStatCheckMillis = System.currentTimeMillis()
                parsed
            } else cached
        }.getOrElse { cached }
    }


    private data class ScheduleEventMeta(
        val dates: List<String>,
        val summaries: Map<String, String> = emptyMap(),
        val startTimes: Map<String, String> = emptyMap(),
        val ticketPrices: Map<String, Int> = emptyMap(),
        val eventStructure: String = "AUTO",
        val scheduleStatus: String = "NONE"
    )

    private fun parseScheduleDatesMap(root: JSONObject): Map<Int, ScheduleEventMeta> {
        val out = linkedMapOf<Int, ScheduleEventMeta>()
        val array = root.optJSONArray("events") ?: JSONArray()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val eventId = item.optInt("event_id")
            if (eventId <= 0) continue
            val dateSet = linkedSetOf<String>()
            item.optJSONArray("schedule_dates")?.let { values ->
                for (j in 0 until values.length()) {
                    values.optString(j).trim().takeIf(String::isNotBlank)?.let { dateSet += it.take(10) }
                }
            }
            val summaries = linkedMapOf<String, String>()
            val startTimes = linkedMapOf<String, String>()
            val ticketPrices = linkedMapOf<String, Int>()
            item.optJSONArray("instances")?.let { instances ->
                for (j in 0 until instances.length()) {
                    val instance = instances.optJSONObject(j) ?: continue
                    val date = sequenceOf(
                        instance.optString("feed_instance_date"),
                        instance.optString("selected_date")
                    ).map(String::trim).firstOrNull { it.isNotBlank() && it != "null" }?.take(10) ?: continue
                    dateSet += date
                    instance.optString("summary").trim().takeIf { it.isNotBlank() && it != "null" }?.let { summaries[date] = it }
                    instance.optString("start_time").trim().takeIf { it.isNotBlank() && it != "null" }?.let { startTimes[date] = it.take(5) }
                    if (instance.has("ticket_price") && !instance.isNull("ticket_price")) {
                        instance.optDouble("ticket_price", Double.NaN).takeIf { !it.isNaN() }?.let { ticketPrices[date] = it.roundToInt().coerceAtLeast(0) }
                    }
                }
            }
            val dates = dateSet.toList().distinct().sorted()
            val eventStructure = item.optString("event_structure", "AUTO").trim().ifBlank { "AUTO" }.uppercase()
            val scheduleStatus = item.optString("schedule_status", if (dates.isEmpty()) "MISSING" else "PARTIAL").trim().ifBlank { "NONE" }.uppercase()
            if (dates.isNotEmpty() || eventStructure != "AUTO" || scheduleStatus != "NONE") {
                out[eventId] = ScheduleEventMeta(dates, summaries, startTimes, ticketPrices, eventStructure, scheduleStatus)
            }
        }
        return out
    }

    private fun loadCachedScheduleDatesMap(context: Context): Map<Int, ScheduleEventMeta> {
        val file = File(File(context.filesDir, CACHE_DIR), SCHEDULE_DATES_CACHE)
        val timestamp = if (file.exists()) file.lastModified() else -1L
        if (scheduleDatesCacheMtime == timestamp) return scheduleDatesMemoryCache
        synchronized(this) {
            if (scheduleDatesCacheMtime == timestamp) return scheduleDatesMemoryCache
            val parsed = if (timestamp < 0L) emptyMap() else runCatching {
                parseScheduleDatesMap(JSONObject(file.readText(StandardCharsets.UTF_8)))
            }.getOrDefault(emptyMap())
            scheduleDatesMemoryCache = parsed
            scheduleDatesCacheMtime = timestamp
            return parsed
        }
    }

    private fun scheduleDatesMapForSync(context: Context): Map<Int, ScheduleEventMeta> {
        val cacheDir = File(context.filesDir, CACHE_DIR).apply { mkdirs() }
        val file = File(cacheDir, SCHEDULE_DATES_CACHE)
        val cached = loadCachedScheduleDatesMap(context)
        val fresh = file.exists() && System.currentTimeMillis() - file.lastModified() < 10L * 60L * 1000L
        if (fresh) return cached
        return runCatching {
            val root = jsonRequest("events/schedule-dates.php", "GET", JSONObject(), token(context))
            if (root.optBoolean("ok", false)) {
                file.writeText(root.toString(), StandardCharsets.UTF_8)
                parseScheduleDatesMap(root)
            } else cached
        }.getOrElse { cached }
    }

    private fun applyScheduleDates(events: List<Concert>, metaByEvent: Map<Int, ScheduleEventMeta>): List<Concert> = events.map { event ->
        val meta = metaByEvent[event.id] ?: return@map event
        val dates = (event.scheduleDates + meta.dates)
            .map(String::trim).filter(String::isNotBlank).map { it.take(10) }.distinct().sorted()
        event.copy(
            scheduleDates = dates,
            scheduleDaySummaries = event.scheduleDaySummaries + meta.summaries,
            scheduleDayStartTimes = event.scheduleDayStartTimes + meta.startTimes,
            scheduleDayTicketPrices = event.scheduleDayTicketPrices + meta.ticketPrices,
            isMultidayFromServer = event.isMultidayFromServer || dates.size > 1,
            // El payload principal és canònic; la cache auxiliar només completa AUTO/NONE.
            eventStructure = if (event.eventStructure != "AUTO") event.eventStructure else meta.eventStructure,
            scheduleStatus = if (event.scheduleStatus != "NONE") event.scheduleStatus else meta.scheduleStatus,
            endDate = if (dates.isNotEmpty()) event.endDate.ifBlank { dates.last() } else event.endDate
        )
    }

    private fun parseEventInfoObject(obj: JSONObject?): EventInfo {
        if (obj == null) return EventInfo()
        val daily = buildList {
            val arr = obj.optJSONArray("daily_start") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                add(EventDailyStart(
                    date = item.optString("date").takeIf { it != "null" }.orEmpty().take(10),
                    time = item.optString("time").takeIf { it != "null" }.orEmpty().take(5),
                    label = item.optString("label", "Inici").takeIf { it != "null" }.orEmpty().ifBlank { "Inici" }
                ))
            }
        }
        val location = obj.optJSONObject("location") ?: JSONObject()
        val transport = buildList {
            val arr = obj.optJSONArray("transport") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val text = item.optString("text").takeIf { it != "null" }.orEmpty().trim()
                if (text.isNotBlank()) add(EventTransportInfo(item.optString("type").takeIf { it != "null" }.orEmpty(), text))
            }
        }
        val pmr = obj.optJSONObject("pmr") ?: JSONObject()
        val notices = buildList {
            val arr = obj.optJSONArray("notices") ?: JSONArray()
            for (i in 0 until arr.length()) arr.optString(i).takeIf { it != "null" }?.trim()?.takeIf(String::isNotBlank)?.let(::add)
        }
        return EventInfo(
            dailyStart = daily,
            address = location.optString("address").takeIf { it != "null" }.orEmpty(),
            mapsUrl = location.optString("maps_url").takeIf { it != "null" }.orEmpty(),
            access = location.optString("access").takeIf { it != "null" }.orEmpty(),
            transport = transport,
            pmrStatus = pmr.optString("status", "UNKNOWN").takeIf { it != "null" }.orEmpty().ifBlank { "UNKNOWN" }.uppercase(),
            pmrText = pmr.optString("text").takeIf { it != "null" }.orEmpty(),
            notices = notices
        )
    }

    private fun parseEventInfoMap(root: JSONObject): Map<Int, EventInfo> {
        val out = linkedMapOf<Int, EventInfo>()
        val arr = root.optJSONArray("events") ?: JSONArray()
        for (i in 0 until arr.length()) {
            val row = arr.optJSONObject(i) ?: continue
            val id = row.optInt("event_id")
            if (id <= 0) continue
            out[id] = parseEventInfoObject(row.optJSONObject("info"))
        }
        return out
    }

    private fun loadCachedEventInfoMap(context: Context): Map<Int, EventInfo> {
        val file = File(File(context.filesDir, CACHE_DIR), EVENT_INFO_CACHE)
        val timestamp = if (file.exists()) file.lastModified() else -1L
        if (eventInfoCacheMtime == timestamp) return eventInfoMemoryCache
        synchronized(this) {
            if (eventInfoCacheMtime == timestamp) return eventInfoMemoryCache
            val parsed = if (timestamp < 0L) emptyMap() else runCatching {
                parseEventInfoMap(JSONObject(file.readText(StandardCharsets.UTF_8)))
            }.getOrDefault(emptyMap())
            eventInfoMemoryCache = parsed
            eventInfoCacheMtime = timestamp
            return parsed
        }
    }

    private fun eventInfoMapForSync(context: Context): Map<Int, EventInfo> {
        val cacheDir = File(context.filesDir, CACHE_DIR).apply { mkdirs() }
        val file = File(cacheDir, EVENT_INFO_CACHE)
        val cached = loadCachedEventInfoMap(context)
        val fresh = file.exists() && System.currentTimeMillis() - file.lastModified() < 10L * 60L * 1000L
        if (fresh) return cached
        return runCatching {
            val root = jsonRequest("events/info-map.php", "GET", JSONObject(), token(context))
            if (root.optBoolean("ok", false)) {
                file.writeText(root.toString(), StandardCharsets.UTF_8)
                parseEventInfoMap(root)
            } else cached
        }.getOrElse { cached }
    }

    private fun applyEventInfo(events: List<Concert>, infoByEvent: Map<Int, EventInfo>): List<Concert> = events.map { event ->
        infoByEvent[event.id]?.let { event.copy(eventInfo = it) } ?: event
    }

    private fun eventVisualMapForSync(context: Context): Map<Int, EventVisual> {
        val file = File(File(context.filesDir, CACHE_DIR), EVENT_VISUALS_CACHE)
        val fresh = file.exists() && System.currentTimeMillis() - file.lastModified() < 10L * 60L * 1000L
        return if (fresh) loadCachedEventVisualMap(context)
        else runCatching { fetchEventVisualMap(context) }.getOrElse { loadCachedEventVisualMap(context) }
    }

    private fun fetchEventVisualMap(context: Context): Map<Int, EventVisual> {
        val connection = open("events/visual-map.php", "GET", token(context))
        val raw = readResponse(connection)
        val root = JSONObject(raw)
        if (!root.optBoolean("ok", false)) return emptyMap()
        File(File(context.filesDir, CACHE_DIR).apply { mkdirs() }, EVENT_VISUALS_CACHE)
            .writeText(raw, StandardCharsets.UTF_8)
        val items = root.optJSONArray("items") ?: JSONArray()
        return buildMap {
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                val id = item.optInt("event_id")
                if (id <= 0) continue
                val organizerArray = item.optJSONArray("organizers") ?: JSONArray()
                val organizers = buildList {
                    for (j in 0 until organizerArray.length()) organizerArray.optString(j).trim().takeIf(String::isNotBlank)?.let(::add)
                }.distinct()
                put(id, EventVisual(
                    imageSource = item.optString("image_source", "AUTO").uppercase(),
                    entityName = item.optString("entity_name").takeIf { it.isNotBlank() && it != "null" },
                    eventImageUrl = resolvePublicUrl(item.optString("event_image_url")).takeIf(String::isNotBlank),
                    preferredImageUrl = resolvePublicUrl(item.optString("preferred_image_url")).takeIf(String::isNotBlank),
                    resolvedImageUrl = resolvePublicUrl(item.optString("resolved_image_url")).takeIf(String::isNotBlank),
                    resolvedImageSource = item.optString("resolved_image_source").takeIf { it.isNotBlank() && it != "null" },
                    resolvedImageStatus = item.optString("resolved_image_status", "PENDING").ifBlank { "PENDING" }.uppercase(),
                    organizers = organizers,
                    organizerImageUrl = resolvePublicUrl(item.optString("organizer_image_url")).takeIf(String::isNotBlank),
                    focusX = item.optDouble("image_focus_x", 0.5).toFloat().coerceIn(0f, 1f),
                    focusY = item.optDouble("image_focus_y", 0.5).toFloat().coerceIn(0f, 1f),
                    scale = item.optDouble("image_scale", 1.0).toFloat().coerceIn(1f, 4f)
                ))
            }
        }
    }

    private fun loadCachedEventVisualMap(context: Context): Map<Int, EventVisual> {
        val file = File(File(context.filesDir, CACHE_DIR), EVENT_VISUALS_CACHE)
        if (!file.exists()) {
            eventVisualsCacheMtime = Long.MIN_VALUE
            eventVisualsMemoryCache = emptyMap()
            return emptyMap()
        }
        val mtime = file.lastModified()
        if (eventVisualsCacheMtime == mtime) return eventVisualsMemoryCache
        synchronized(this) {
            if (eventVisualsCacheMtime == mtime) return eventVisualsMemoryCache
            val parsed = runCatching {
                val root = JSONObject(file.readText(StandardCharsets.UTF_8))
                val items = root.optJSONArray("items") ?: JSONArray()
                buildMap {
                    for (i in 0 until items.length()) {
                        val item = items.optJSONObject(i) ?: continue
                        val id = item.optInt("event_id")
                        if (id <= 0) continue
                        val organizerArray = item.optJSONArray("organizers") ?: JSONArray()
                        val organizers = buildList {
                            for (j in 0 until organizerArray.length()) organizerArray.optString(j).trim().takeIf(String::isNotBlank)?.let(::add)
                        }.distinct()
                        put(id, EventVisual(
                            imageSource = item.optString("image_source", "AUTO").uppercase(),
                            entityName = item.optString("entity_name").takeIf { it.isNotBlank() && it != "null" },
                            eventImageUrl = resolvePublicUrl(item.optString("event_image_url")).takeIf(String::isNotBlank),
                            preferredImageUrl = resolvePublicUrl(item.optString("preferred_image_url")).takeIf(String::isNotBlank),
                            resolvedImageUrl = resolvePublicUrl(item.optString("resolved_image_url")).takeIf(String::isNotBlank),
                            resolvedImageSource = item.optString("resolved_image_source").takeIf { it.isNotBlank() && it != "null" },
                            resolvedImageStatus = item.optString("resolved_image_status", "PENDING").ifBlank { "PENDING" }.uppercase(),
                            organizers = organizers,
                            organizerImageUrl = resolvePublicUrl(item.optString("organizer_image_url")).takeIf(String::isNotBlank),
                            focusX = item.optDouble("image_focus_x", 0.5).toFloat().coerceIn(0f, 1f),
                            focusY = item.optDouble("image_focus_y", 0.5).toFloat().coerceIn(0f, 1f),
                            scale = item.optDouble("image_scale", 1.0).toFloat().coerceIn(1f, 4f)
                        ))
                    }
                }
            }.getOrDefault(emptyMap())
            eventVisualsMemoryCache = parsed
            eventVisualsCacheMtime = mtime
            return parsed
        }
    }

    private fun applyEventVisuals(events: List<Concert>, visuals: Map<Int, EventVisual>): List<Concert> = events.map { concert ->
        val visual = visuals[concert.id] ?: return@map concert
        concert.copy(
            organizers = visual.organizers.ifEmpty { concert.organizers },
            imageSource = visual.imageSource,
            imageSourceEntityName = visual.entityName,
            eventCardImageUri = visual.eventImageUrl,
            preferredImageUri = visual.preferredImageUrl,
            resolvedCardImageUri = visual.resolvedImageUrl,
            resolvedCardImageSource = visual.resolvedImageSource,
            resolvedCardImageStatus = visual.resolvedImageStatus,
            organizerImageUri = visual.organizerImageUrl ?: concert.organizerImageUri,
            imageFocusX = visual.focusX,
            imageFocusY = visual.focusY,
            imageScale = visual.scale
        )
    }

    /**
     * CSV dels esdeveniments que ja existixen en l'API central.
     * L'endpoint és exclusiu de Super Admin i retorna una capçalera compatible amb
     * events_import.csv, incloses les URLs base d'imatges d'entitats quan existixen.
     */
    fun fetchEventsExportCsv(context: Context): Result<String> = runCatching {
        val savedToken = token(context) ?: error("La sessió de servidor no està disponible. Torna a iniciar sessió.")
        val connection = open("events/export.php", "GET", savedToken).apply {
            setRequestProperty("Accept", "text/csv, text/plain;q=0.9, */*;q=0.1")
        }
        readResponse(connection)
    }

    /**
     * La fila de SQLite no porta necessàriament totes les dates del festival:
     * provenen de schedule-dates.php i event-info.php, guardats separadament.
     * Les lectures ràpides han d'unir eixes dades també en el PRIMER frame.
     */
    private fun enrichCachedFestivalCalendar(context: Context, events: List<Concert>): List<Concert> {
        if (events.isEmpty()) return events
        val dates = loadCachedScheduleDatesMap(context)
        val info = loadCachedEventInfoMap(context)
        return applyEventInfo(applyScheduleDates(events, dates), info)
    }

    /** v329: previsualització estrictament local dels IDs d'una instantània
     * validada pel client. Sense fallback al JSON global ni parseig de catàlegs
     * d'imatges/municipis, que el procés normal ja completarà després.
     * El poster propi de l'esdeveniment es conserva des del JSON de SQLite.
     */
    fun loadCachedForYouPreview(context: Context, ids: List<Int>): List<Concert> {
        val clean = ids.filter { it > 0 }.distinct().take(20)
        if (clean.isEmpty()) return emptyList()
        val local = runCatching { AgendaLocalDatabase.loadEventsByIds(context, clean) }
            .getOrNull() ?: return emptyList()
        if (local.length() == 0) return emptyList()
        val parsed = runCatching { parseEvents(local, context, EntityImageCatalog.EMPTY, emptyList()) }
            .getOrDefault(emptyList())
        val byId = parsed.associateBy { it.id }
        return clean.mapNotNull(byId::get)
    }

    data class PreparedForYouServerCache(val ids: List<Int>, val fresh: Boolean)

    /** Caché per compte: només IDs, mai dades de perfil ni cartells. Opcional;
     * qualsevol servidor antic sense l'endpoint conserva el flux Android.
     */
    fun readPreparedForYouCache(context: Context, signature: Int, day: String): Result<PreparedForYouServerCache?> = runCatching {
        val savedToken = token(context) ?: return@runCatching null
        val root = jsonRequest("for-you/index.php?sig=$signature&day=$day", "GET", JSONObject(), savedToken)
        if (!root.optBoolean("hit", false)) return@runCatching null
        val array = root.optJSONArray("ids") ?: JSONArray()
        val ids = (0 until array.length()).mapNotNull { i -> array.optInt(i).takeIf { it > 0 } }
            .distinct().take(5000)
        PreparedForYouServerCache(ids, root.optBoolean("fresh", false))
    }

    fun writePreparedForYouCache(context: Context, signature: Int, day: String, ids: List<Int>): Result<Unit> = runCatching {
        val savedToken = token(context) ?: return@runCatching
        val values = JSONArray()
        ids.asSequence().filter { it > 0 }.distinct().take(5000).forEach { values.put(it) }
        jsonRequest("for-you/index.php", "POST", JSONObject()
            .put("sig", signature.toString()).put("day", day).put("ids", values), savedToken)
        Unit
    }

    /** v329 · Reconstrucció de tota la selecció de Per a tu en SQLite.
     * Ni xarxa, ni escaneig del catàleg complet, ni metadata de fotos al primer frame.
     * El feed final continuarà aplicant totes les regles de visibilitat.
     */
    fun loadCachedForYouSelection(context: Context, ids: List<Int>): List<Concert> {
        val clean = ids.asSequence().filter { it > 0 }.distinct().take(5000).toList()
        if (clean.isEmpty()) return emptyList()
        val rows = runCatching { AgendaLocalDatabase.loadSelectionByIds(context, clean) }
            .getOrNull() ?: return emptyList()
        if (rows.length() == 0) return emptyList()
        val parsed = runCatching { parseEvents(rows, context, EntityImageCatalog.EMPTY, emptyList()) }
            .getOrDefault(emptyList())
        val byId = parsed.associateBy { it.id }
        return clean.mapNotNull(byId::get)
    }

    /** 10ai: fast path d'arrancada. Parseja només els IDs del snapshot de Per a tu.
     *  No obri els catàlegs grans d'imatges/municipis: el lot complet els aplicarà després. */
    fun loadCachedEventsByIds(context: Context, ids: List<Int>): List<Concert> {
        if (ids.isEmpty()) return emptyList()
        val requested = ids.asSequence().filter { it > 0 }.distinct().toList()
        if (requested.isEmpty()) return emptyList()
        val entityImages = loadCachedEntityImages(context)
        val visuals = loadCachedEventVisualMap(context)

        val local = runCatching { AgendaLocalDatabase.loadEventsByIds(context, requested) }.getOrNull()
        val fromDb = if (local != null && local.length() > 0) {
            runCatching {
                applyEventVisuals(enrichCachedFestivalCalendar(context, parseEvents(local, context, entityImages, emptyList())), visuals)
            }.getOrDefault(emptyList())
        } else emptyList()

        // Un anunci pot apuntar a un event que encara no haja entrat en la finestra local
        // materialitzada. Si SQLite no el té, busquem només els IDs que falten en la
        // còpia completa de l'API. Açò evita que la capçalera desaparega per dependència
        // del subconjunt de Per a tu.
        val foundIds = fromDb.asSequence().map { it.id }.toHashSet()
        val missingIds = requested.filterNot(foundIds::contains).toHashSet()
        val fromFile = if (missingIds.isNotEmpty()) {
            val file = File(File(context.filesDir, CACHE_DIR), API_CACHE)
            if (!file.exists()) emptyList() else runCatching {
                val root = JSONObject(file.readText(StandardCharsets.UTF_8))
                val all = root.optJSONArray("events") ?: JSONArray()
                val selected = JSONArray()
                for (i in 0 until all.length()) {
                    val obj = all.optJSONObject(i) ?: continue
                    val id = obj.optInt("event_id").takeIf { it > 0 } ?: obj.optInt("id")
                    if (id in missingIds) selected.put(obj)
                }
                applyEventVisuals(enrichCachedFestivalCalendar(context, parseEvents(selected, context, entityImages, emptyList())), visuals)
            }.getOrDefault(emptyList())
        } else emptyList()

        val byId = (fromDb + fromFile).associateBy { it.id }
        return requested.mapNotNull(byId::get)
    }

    /**
     * v333: primer lot de «Sense filtre» directament de l'índex SQLite.
     * No espera el parseig del catàleg global, dels municipis ni del catàleg
     * d'imatges. El resultat és només una vista transitòria; el llistat complet
     * substituïx esta vista en acabar la hidratació del catàleg.
     */
    fun loadCachedUnfilteredPreview(context: Context, fromDate: String, limit: Int = 48): List<Concert> {
        val rows = runCatching { AgendaLocalDatabase.loadFirstEvents(context, fromDate, limit) }
            .getOrNull() ?: return emptyList()
        if (rows.length() == 0) return emptyList()
        return runCatching { parseEvents(rows, context, EntityImageCatalog.EMPTY, emptyList()) }
            .getOrDefault(emptyList())
    }

    fun loadCachedFirstEvents(context: Context, fromDate: String, limit: Int = 5): List<Concert> {
        val local = runCatching { AgendaLocalDatabase.loadFirstEvents(context, fromDate, limit) }.getOrNull() ?: return emptyList()
        if (local.length() == 0) return emptyList()
        return runCatching {
            applyEventVisuals(
                enrichCachedFestivalCalendar(context, parseEvents(local, context, loadCachedEntityImages(context), emptyList())),
                loadCachedEventVisualMap(context)
            )
        }.getOrDefault(emptyList())
    }

    /** Segona fase: prou agenda futura per construir el primer lot complet sense full scan. */
    fun loadCachedUpcomingEventsFast(
        context: Context,
        fromDate: String,
        limit: Int = 200
    ): List<Concert> {
        val local = runCatching { AgendaLocalDatabase.loadUpcomingEvents(context, fromDate, limit) }.getOrNull()
            ?: return emptyList()
        if (local.length() == 0) return emptyList()
        return runCatching {
            applyEventVisuals(
                enrichCachedFestivalCalendar(context, parseEvents(local, context, loadCachedEntityImages(context), emptyList())),
                loadCachedEventVisualMap(context)
            )
        }.getOrDefault(emptyList())
    }

    /**
     * Font completa i indexada de «Per a tu»: tots els candidats locals de la finestra
     * temporal, sense carregar la resta del catàleg que només necessita Sense filtre/cercador.
     */
    fun loadCachedForYouWindowFast(context: Context, fromDate: String, toDate: String): List<Concert> {
        val local = runCatching { AgendaLocalDatabase.loadEventsInRange(context, fromDate, toDate) }.getOrNull()
            ?: return emptyList()
        if (local.length() == 0) return emptyList()
        return runCatching {
            applyEventVisuals(
                enrichCachedFestivalCalendar(context, parseEvents(local, context, loadCachedEntityImages(context), emptyList())),
                loadCachedEventVisualMap(context)
            )
        }.getOrDefault(emptyList())
    }

    /**
     * 10eo: lectura ràpida per a l'arrancada. Només materialitza la cache principal;
     * no parseja ENTITATS, municipis, visuals ni metadades auxiliars. Això dona al feed
     * la llista completa sense convertir l'enriquiment en part del camí crític.
     */
    fun loadCachedEventsFast(context: Context): List<Concert> {
        val revision = runCatching { AgendaLocalDatabase.revision(context) }.getOrDefault(0L)
        val visuals = loadCachedEventVisualMap(context)
        if (revision == parsedEventsMemoryRevision && parsedEventsMemory.isNotEmpty()) {
            return applyEventVisuals(enrichCachedFestivalCalendar(context, parsedEventsMemory), visuals)
        }
        val local = runCatching { AgendaLocalDatabase.loadEvents(context) }.getOrNull()
        if (local != null && local.length() > 0) {
            return runCatching {
                applyEventVisuals(enrichCachedFestivalCalendar(context, parseEvents(local, context, loadCachedEntityImages(context), emptyList())), visuals)
            }.getOrDefault(emptyList())
        }
        val file = File(File(context.filesDir, CACHE_DIR), API_CACHE)
        if (!file.exists()) return emptyList()
        return runCatching {
            val root = JSONObject(file.readText(StandardCharsets.UTF_8))
            applyEventVisuals(enrichCachedFestivalCalendar(context, parseEvents(root.getJSONArray("events"), context, loadCachedEntityImages(context), emptyList())), visuals)
        }.getOrDefault(emptyList())
    }

    fun loadCachedEvents(context: Context): List<Concert> {
        val revision = runCatching { AgendaLocalDatabase.revision(context) }.getOrDefault(0L)
        if (revision == parsedEventsMemoryRevision && parsedEventsMemory.isNotEmpty()) {
            return applyEventVisuals(enrichCachedFestivalCalendar(context, parsedEventsMemory), loadCachedEventVisualMap(context))
        }
        val local = runCatching { AgendaLocalDatabase.loadEvents(context) }.getOrNull()
        if (local != null && local.length() > 0) {
            return runCatching {
                val parsed = enrichCachedFestivalCalendar(context, parseEvents(local, context, loadCachedEntityImages(context)))
                parsedEventsMemoryRevision = revision
                parsedEventsMemory = parsed
                applyEventVisuals(parsed, loadCachedEventVisualMap(context))
            }.getOrDefault(emptyList())
        }
        val file = File(File(context.filesDir, CACHE_DIR), API_CACHE)
        if (!file.exists()) return emptyList()
        return runCatching {
            val root = JSONObject(file.readText(StandardCharsets.UTF_8))
            applyEventVisuals(
                enrichCachedFestivalCalendar(context, parseEvents(root.getJSONArray("events"), context, loadCachedEntityImages(context))),
                loadCachedEventVisualMap(context)
            )
        }.getOrDefault(emptyList())
    }

    private fun ensureEntityCatalogCaches(context: Context): EntityImageCatalog {
        val file = File(File(context.filesDir, CACHE_DIR), ENTITY_IMAGES_CACHE)
        if (!file.exists()) {
            synchronized(this) {
                entityImagesCacheMtime = Long.MIN_VALUE
                entityImagesMemoryCache = EntityImageCatalog.EMPTY
                entityNamesCacheMtime = Long.MIN_VALUE
                entityNamesCache = emptyMap()
                entityAffinityCacheMtime = Long.MIN_VALUE
                entityAffinityCache = emptyMap()
            }
            return EntityImageCatalog.EMPTY
        }
        val mtime = file.lastModified()
        if (entityImagesCacheMtime == mtime && entityNamesCacheMtime == mtime && entityAffinityCacheMtime == mtime) {
            return entityImagesMemoryCache
        }
        synchronized(this) {
            if (entityImagesCacheMtime == mtime && entityNamesCacheMtime == mtime && entityAffinityCacheMtime == mtime) {
                return entityImagesMemoryCache
            }
            val bundle = runCatching {
                parseEntityCatalogBundle(JSONObject(file.readText(StandardCharsets.UTF_8)))
            }.getOrElse { EntityCatalogBundle(EntityImageCatalog.EMPTY, emptyMap(), emptyMap()) }
            entityImagesMemoryCache = bundle.images
            entityImagesCacheMtime = mtime
            entityNamesCache = deduplicateEntityNames(bundle.names)
            entityNamesCacheMtime = mtime
            entityAffinityCache = bundle.affinities
            entityAffinityCacheMtime = mtime
            entityImagesLastStatCheckMillis = System.currentTimeMillis()
            return bundle.images
        }
    }

    private fun loadCachedEntityImages(context: Context): EntityImageCatalog {
        val now = System.currentTimeMillis()
        // En recomposicions, ni tan sols fem stat() mentre la cache RAM és recent.
        if (entityImagesCacheMtime != Long.MIN_VALUE &&
            now - entityImagesLastStatCheckMillis < ENTITY_IMAGE_STAT_INTERVAL_MS) {
            return entityImagesMemoryCache
        }
        entityImagesLastStatCheckMillis = now
        return ensureEntityCatalogCaches(context)
    }

    private fun invalidateEntityImagesCache(context: Context) {
        File(File(context.filesDir, CACHE_DIR), ENTITY_IMAGES_CACHE).delete()
        synchronized(this) {
            entityImagesCacheMtime = Long.MIN_VALUE
            entityImagesMemoryCache = EntityImageCatalog.EMPTY
            entityImagesLastStatCheckMillis = 0L
            entityNamesCacheMtime = Long.MIN_VALUE
            entityNamesCache = emptyMap()
            entityAffinityCacheMtime = Long.MIN_VALUE
            entityAffinityCache = emptyMap()
        }
    }

    /** Preescalfa el catàleg fora del fil principal abans de pintar feed i Batecs. */
    fun warmEntityImageCache(context: Context) {
        loadCachedEntityImages(context)
    }

    /** Canvis del catàleg: una foto nova no altera les recomanacions. */
    data class EntityCatalogChanges(val visualsChanged: Boolean, val affinitiesChanged: Boolean)

    /**
     * v301: diferenciar imatges de dades culturals. Sols els canvis d'estil,
     * categoria, idioma o noms dels artistes invaliden el càlcul de Per a tu.
     */
    fun refreshEntityCatalogChanges(context: Context): EntityCatalogChanges {
        val previousImages = entityImagesMemoryCache
        val previousAffinity = entityAffinityCache
        val images = entityCatalogForSync(context)
        return EntityCatalogChanges(
            visualsChanged = images != previousImages,
            affinitiesChanged = entityAffinityCache != previousAffinity
        )
    }

    /** Compatibilitat amb altres consumidors que esperen un booleà. */
    fun refreshEntityImageCatalogCache(context: Context): Boolean {
        val changes = refreshEntityCatalogChanges(context)
        return changes.visualsChanged || changes.affinitiesChanged
    }

    /**
     * Lectura estrictament en RAM. No toca disc, no parseja JSON i no espera cap lock.
     * És la variant segura per a Compose/UI. El catàleg es preescalfa en segon pla.
     */
    fun peekCachedEntityImageUrl(type: String, name: String): String? {
        if (name.isBlank()) return null
        val key = entityLookupKey(name)
        val catalog = entityImagesMemoryCache
        return when (type.trim().uppercase(Locale.ROOT)) {
            "ARTIST" -> catalog.artists[key]
            "VENUE" -> catalog.venues[key]
            "TOWN" -> catalog.towns[key]
            "CATEGORY" -> catalog.categories[key]
            "ORGANIZER", "FESTIVAL" -> catalog.organizers[key]
            else -> null
        }
    }

    /** Sense alterar els IDs del servidor, evitem duplicats tipogràfics al catàleg públic. */
    private fun deduplicateEntityNames(source: Map<String, List<String>>): Map<String, List<String>> =
        source.mapValues { (type, names) ->
            if (type.equals("ARTIST", ignoreCase = true)) names.distinctBy(AgendaDedupe::artistKey)
            else names
        }

    /** Variant estrictament RAM: mai llig ni parseja el JSON d'ENTITATS. */
    fun peekCachedEntityNames(type: String): List<String> =
        entityNamesCache[type.trim().uppercase(Locale.ROOT)].orEmpty()

    /** Noms canònics del catàleg ja sincronitzat, encara que no tinguen concerts. */
    fun cachedEntityNames(context: Context, type: String): List<String> {
        ensureEntityCatalogCaches(context)
        return entityNamesCache[type.trim().uppercase(Locale.ROOT)].orEmpty()
    }

    /** Foto canònica d'una entitat ja sincronitzada amb l'agenda. */
    fun cachedEntityImageUrl(context: Context, type: String, name: String): String? {
        if (name.isBlank()) return null
        val key = entityLookupKey(name)
        val catalog = loadCachedEntityImages(context)
        return when (type.trim().uppercase()) {
            "ARTIST" -> catalog.artists[key]
            "VENUE" -> catalog.venues[key]
            "TOWN" -> catalog.towns[key]
            "CATEGORY" -> catalog.categories[key]
            "ORGANIZER", "FESTIVAL" -> catalog.organizers[key]
            else -> null
        }
    }

    /**
     * Llig IDIOMA/CATEGORIA/ESTIL del mateix catàleg d'ENTITATS que ja es cacheja
     * per a les imatges. Si el servidor antic encara no envia estos camps, retorna
     * un mapa buit i l'algoritme usa els estils/categoria dels esdeveniments com a fallback.
     */
    /** Lectura estrictament RAM per al feed: mai toca el JSON d'ENTITATS. */
    fun peekCachedArtistAffinityMetadata(): Map<String, EntityAffinityMetadata> = entityAffinityCache

    fun cachedArtistAffinityMetadata(context: Context): Map<String, EntityAffinityMetadata> {
        ensureEntityCatalogCaches(context)
        return entityAffinityCache
    }


    fun fetchEntityImageRows(context: Context): Result<List<EntityImageRow>> = runCatching {
        val savedToken = token(context) ?: error("La sessió de servidor no està disponible.")
        val connection = open("entities/admin-list.php", "GET", savedToken)
        val root = JSONObject(readResponse(connection))
        if (!root.optBoolean("ok", false)) error(root.optString("message", "No s'ha pogut carregar el catàleg d'imatges."))
        val items = root.optJSONArray("entities") ?: JSONArray()
        buildList {
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                add(EntityImageRow(
                    type = item.optString("type").uppercase(),
                    name = item.optString("name"),
                    imageUrl = resolvePublicUrl(item.optString("image_url")).takeIf(String::isNotBlank),
                    sourceUrl = item.optString("source_url").takeIf { it.isNotBlank() && it != "null" },
                    imageOrigin = item.optString("image_origin", "INTERNET"),
                    imageStatus = item.optString("image_status", "pending")
                ))
            }
        }
    }

    fun bulkUpsertEntityImages(context: Context, rows: List<EntityImageRow>): Result<Int> = runCatching {
        val savedToken = token(context) ?: error("La sessió de servidor no està disponible.")
        val payloadRows = JSONArray()
        rows.forEach { row ->
            payloadRows.put(JSONObject()
                .put("type", row.type.uppercase())
                .put("name", row.name.trim())
                .put("image_url", row.imageUrl.orEmpty())
                .put("source_url", row.sourceUrl.orEmpty())
                .put("image_origin", row.imageOrigin.ifBlank { "MANUAL" }))
        }
        val response = jsonRequest("entities/bulk_upsert.php", "POST", JSONObject().put("entities", payloadRows), savedToken)
        // invalidem la cache perquè els dos clients vegen els canvis en el següent refresh.
        invalidateEntityImagesCache(context)
        response.optInt("updated", rows.size)
    }

    fun bulkUpsertProfessionalProfiles(context: Context, rows: List<ProfessionalProfileImportRow>): Result<Int> = runCatching {
        val savedToken = token(context) ?: error("La sessió de servidor no està disponible.")
        val payloadRows = JSONArray()
        rows.forEach { row ->
            payloadRows.put(JSONObject()
                .put("profile_type", row.profileType.uppercase())
                .put("name", row.name.trim())
                .put("image_url", row.imageUrl.orEmpty())
                .put("source_url", row.sourceUrl.orEmpty())
                .put("image_origin", row.imageOrigin.ifBlank { "RESEARCH" })
                .put("style_primary", row.stylePrimary.orEmpty())
                .put("style_secondary", row.styleSecondary.orEmpty())
                .put("style_tags", row.styleTags.orEmpty())
                .put("language_primary", row.languagePrimary.orEmpty())
                .put("language_secondary", row.languageSecondary.orEmpty())
                .put("language_tags", row.languageTags.orEmpty())
                .put("source_origin", row.sourceOrigin.orEmpty())
                .put("confidence", row.confidence.orEmpty())
                .put("notes", row.notes.orEmpty())
                .put("agency_names", JSONArray(row.agencyNames)))
        }
        val response = jsonRequest(
            "professional-profiles/bulk_upsert.php",
            "POST",
            JSONObject().put("profiles", payloadRows),
            savedToken
        )
        invalidateEntityImagesCache(context)
        response.optInt("updated", rows.size)
    }

    fun fetchSavedEventIds(context: Context): Result<Set<Int>> = runCatching {
        val savedToken = token(context) ?: error("La sessió de servidor no està disponible. Torna a iniciar sessió.")
        val connection = open("events/saved.php", "GET", savedToken)
        val root = JSONObject(readResponse(connection))
        if (!root.optBoolean("ok", false)) error(root.optString("message", "No s'han pogut carregar els guardats."))
        val ids = root.optJSONArray("event_ids") ?: JSONArray()
        buildSet { for (i in 0 until ids.length()) add(ids.optInt(i)) }
    }

    fun setEventSaved(
        context: Context,
        eventId: Int,
        saved: Boolean,
        entityLabel: String = "",
        userTown: String = "",
        userComarca: String = "",
        concert: Concert? = null,
        organizer: String = ""
    ): Result<SaveEventResult> = runCatching {
        val payload = eventContextPayload(eventId, entityLabel, userTown, userComarca, concert, organizer)
            .put("saved", saved)
        val response = postAuthenticatedJson(context, "events/save.php", payload).getOrThrow()
        SaveEventResult(
            saved = jsonBoolean(response, "saved", saved),
            savedCount = response.optInt("saved_count", 0).coerceAtLeast(0)
        )
    }

    /**
     * Recompte social lleuger i independent de la revisió del feed.
     * Guardats/M'agrada poden canviar sense modificar l'esdeveniment, així que no
     * han de dependre de events/sync.php ni de la cache d'esdeveniments.
     */
    fun fetchEventSocialCounts(
        context: Context,
        eventIds: Collection<Int>
    ): Result<Map<Int, EventSocialCount>> = runCatching {
        val ids = eventIds.asSequence().filter { it > 0 }.distinct().take(100).toList()
        if (ids.isEmpty()) return@runCatching emptyMap()

        val encodedIds = URLEncoder.encode(ids.joinToString(","), StandardCharsets.UTF_8.name())
        val connection = open("events/social-counts.php?ids=$encodedIds", "GET", token(context))
        val root = JSONObject(readResponse(connection))
        if (!root.optBoolean("ok", false)) {
            error(root.optString("message", "No s'han pogut actualitzar els recomptes socials."))
        }
        val items = root.optJSONArray("items") ?: JSONArray()
        buildMap {
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                val eventId = item.optInt("event_id")
                if (eventId <= 0) continue
                put(
                    eventId,
                    EventSocialCount(
                        eventId = eventId,
                        savedCount = item.optInt("saved_count", 0).coerceAtLeast(0),
                        likeCount = item.optInt("like_count", 0).coerceAtLeast(0)
                    )
                )
            }
        }
    }

    /** Carrega la font de veritat del perfil de l'usuari: seguiments, guardats, assistència i afinitats. */
    fun fetchUserState(context: Context): Result<UserStateSnapshot> = runCatching {
        val savedToken = token(context) ?: error("La sessió de servidor no està disponible. Torna a iniciar sessió.")
        val connection = open("user-state/mine.php", "GET", savedToken)
        val root = JSONObject(readResponse(connection))
        if (!root.optBoolean("ok", false)) error(root.optString("message", "No s'ha pogut carregar l'estat d'usuari."))

        val follows = buildList {
            val items = root.optJSONArray("follows") ?: JSONArray()
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                add(UserFollowState(
                    entityType = item.optString("entity_type"),
                    entityKey = item.optString("entity_key"),
                    entityLabel = item.optString("entity_label")
                ))
            }
        }
        val eventStates = buildList {
            val items = root.optJSONArray("event_states") ?: JSONArray()
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                val id = item.optInt("event_id")
                if (id <= 0) continue
                add(UserEventState(
                    eventId = id,
                    saved = jsonBoolean(item, "saved", false),
                    liked = jsonBoolean(item, "liked", false),
                    attendance = item.optString("attendance", "NONE")
                ))
            }
        }
        val eventAlerts = buildSet {
            val items = root.optJSONArray("event_alerts") ?: JSONArray()
            for (i in 0 until items.length()) {
                val id = items.optInt(i)
                if (id > 0) add(id)
            }
        }
        val affinities = buildList {
            val items = root.optJSONArray("affinities") ?: JSONArray()
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                add(RemoteAffinity(
                    entityType = item.optString("entity_type"),
                    entityKey = item.optString("entity_key"),
                    entityLabel = item.optString("entity_label"),
                    searchCount = item.optInt("search_count", 0).coerceAtLeast(0),
                    interactionCount = item.optInt("interaction_count", 0).coerceAtLeast(0),
                    score = item.optDouble("score", 0.0).coerceAtLeast(0.0),
                    effectiveScore = item.optDouble("effective_score", item.optDouble("score", 0.0)).coerceAtLeast(0.0),
                    permanent = jsonBoolean(item, "permanent", false),
                    lastAction = item.optString("last_action"),
                    lastSeenAt = item.optString("last_seen_at"),
                    decayPercent = item.optInt("decay_percent", 0).coerceIn(0, 100)
                ))
            }
        }
        val notificationPreferences = root.optJSONObject("notification_preferences")?.let { item ->
            RemoteNotificationPreferences(
                followedArtistConcerts = jsonBoolean(item, "followed_artist_concerts", true),
                followedArtistNews = jsonBoolean(item, "followed_artist_news", true),
                tasteSuggestions = jsonBoolean(item, "taste_suggestions", true),
                nearbySuggestions = jsonBoolean(item, "nearby_suggestions", true)
            )
        }
        UserStateSnapshot(follows, eventStates, eventAlerts, affinities, notificationPreferences)
    }

    /** Envia fins a 50 senyals d'afinitat en una sola connexió. */
    fun postAffinityBatch(context: Context, signals: List<JSONObject>): Result<Unit> {
        if (signals.isEmpty()) return Result.success(Unit)
        val savedToken = token(context) ?: return Result.failure(IllegalStateException("La sessió de servidor no està disponible."))
        val batch = signals.take(50)
        val primary = runCatching {
            val payload = JSONObject().put("signals", JSONArray().apply { batch.forEach { put(it) } })
            jsonRequest("affinities/batch.php", "POST", payload, savedToken)
            Unit
        }
        if (primary.isSuccess) return primary
        // Compatibilitat durant el desplegament: si batch.php encara no està al servidor,
        // conserva el comportament 0.17.x amb una petició per senyal.
        return runCatching {
            batch.forEach { jsonRequest("affinities/record.php", "POST", it, savedToken) }
            Unit
        }
    }

    /** Metadades mínimes d'una invitació: el token ja dona accés al nom, no als esdeveniments. */
    fun publicSharedSavedListName(token: String): Result<String> = runCatching {
        require(token.matches(Regex("[a-f0-9]{48}"))) { "Enllaç d'invitació no vàlid." }
        val root = jsonRequest("collections/public-invite.php?invite=$token", "GET", JSONObject(), null)
        root.optString("name").takeIf { it.isNotBlank() } ?: error("No s'ha pogut carregar la llista.")
    }

    /** v291: les llistes de filtres viuen en MariaDB, privades per token d'usuari. */
    // Usar el token capturat abans d'encuar la petició evita que un canvi
    // de sessió envíe la llista de l'usuari anterior al compte nou.
    fun fetchFilterPresets(sessionToken: String): Result<JSONArray> = runCatching {
        jsonRequest("preferences/search-filters.php", "GET", JSONObject(), sessionToken)
            .optJSONArray("presets") ?: JSONArray()
    }

    fun saveFilterPresetOnServer(sessionToken: String, name: String, filters: JSONObject): Result<Unit> = runCatching {
        jsonRequest("preferences/search-filters.php", "POST",
            JSONObject().put("action", "save").put("name", name).put("filters", filters), sessionToken)
        Unit
    }

    fun deleteFilterPresetOnServer(sessionToken: String, name: String): Result<Unit> = runCatching {
        jsonRequest("preferences/search-filters.php", "POST",
            JSONObject().put("action", "delete").put("name", name), sessionToken)
        Unit
    }

    /** Lectura autenticada; els grups d’amics i les llistes no són dades públiques. */
    fun getAuthenticatedJson(context: Context, path: String): Result<JSONObject> = runCatching {
        val savedToken = token(context) ?: error("Inicia sessió per consultar les teues llistes.")
        jsonRequest(path, "GET", JSONObject(), savedToken)
    }

    /** POST autenticat públic per a la cua offline d'estat/afinitats. */
    fun postAuthenticatedJson(context: Context, path: String, payload: JSONObject): Result<JSONObject> = runCatching {
        val savedToken = token(context) ?: error("La sessió de servidor no està disponible. Torna a iniciar sessió.")
        jsonRequest(path, "POST", payload, savedToken)
    }

    fun eventStatePayload(
        eventId: Int,
        entityLabel: String,
        userTown: String,
        userComarca: String,
        concert: Concert?,
        organizer: String
    ): JSONObject = eventContextPayload(eventId, entityLabel, userTown, userComarca, concert, organizer)

    private fun eventContextPayload(
        eventId: Int,
        entityLabel: String,
        userTown: String,
        userComarca: String,
        concert: Concert?,
        organizer: String
    ): JSONObject = JSONObject()
        .put("event_id", eventId)
        .put("entity_label", entityLabel.ifBlank { concert?.title.orEmpty() })
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


    /**
     * Registra un senyal cru d'afinitat en l'API. És fire-and-forget des d'AgendaState:
     * si no hi ha sessió o xarxa, la UI continua funcionant amb l'historial local.
     */
    fun recordAffinitySignal(
        context: Context,
        actionType: String,
        entityType: String,
        entityKey: String,
        entityLabel: String = entityKey,
        eventId: Int? = null,
        userTown: String = "",
        userComarca: String = "",
        town: String = "",
        comarca: String = "",
        category: String = "",
        language: String = "",
        primaryArtist: String = "",
        organizer: String = "",
        venue: String = "",
        styles: List<String> = emptyList()
    ): Result<Unit> = runCatching {
        if (token(context).isNullOrBlank()) return@runCatching
        val payload = JSONObject()
            .put("action_type", actionType)
            .put("entity_type", entityType)
            .put("entity_key", entityKey)
            .put("entity_label", entityLabel)
            .put("user_town", userTown)
            .put("user_comarca", userComarca)
            .put("town", town)
            .put("comarca", comarca)
            .put("category", category)
            .put("language", language)
            .put("primary_style", styles.firstOrNull().orEmpty())
            .put("primary_artist", primaryArtist)
            .put("organizer", organizer)
            .put("venue", venue)
            .put("styles_csv", styles.joinToString(" | "))
        eventId?.let { payload.put("event_id", it) }
        postAuthenticatedJson(context, "affinities/record.php", payload).getOrThrow()
        Unit
    }

    fun fetchAffinityAnalytics(
        context: Context,
        days: Int = 180,
        town: String = "",
        comarca: String = "",
        username: String = ""
    ): Result<AffinityAnalytics> = runCatching {
        val savedToken = token(context) ?: error("La sessió de servidor no està disponible. Torna a iniciar sessió.")
        val query = buildString {
            append("affinities/analytics.php?days=")
            append(days.coerceIn(1, 365))
            if (town.isNotBlank()) append("&town=").append(URLEncoder.encode(town, StandardCharsets.UTF_8.name()))
            if (comarca.isNotBlank()) append("&comarca=").append(URLEncoder.encode(comarca, StandardCharsets.UTF_8.name()))
            if (username.isNotBlank()) append("&username=").append(URLEncoder.encode(username.removePrefix("@"), StandardCharsets.UTF_8.name()))
        }
        val connection = open(query, "GET", savedToken)
        val root = JSONObject(readResponse(connection))
        if (!root.optBoolean("ok", false)) error(root.optString("message", "No s'ha pogut carregar l'analítica."))
        val overview = root.optJSONObject("overview") ?: JSONObject()

        fun rows(key: String): List<AffinityAnalyticsRow> {
            val array = root.optJSONArray(key) ?: JSONArray()
            return buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    add(
                        AffinityAnalyticsRow(
                            label = item.optString("label", item.optString("a_label")),
                            total = item.optInt("total", item.optInt("users", 0)),
                            users = item.optInt("users", 0),
                            type = item.optString("entity_type", item.optString("a_type")),
                            secondaryLabel = item.optString("b_label"),
                            secondaryType = item.optString("b_type")
                        )
                    )
                }
            }
        }

        AffinityAnalytics(
            signals = overview.optInt("signals", 0),
            users = overview.optInt("users", 0),
            searches = overview.optInt("searches", 0),
            opens = overview.optInt("opens", 0),
            saves = overview.optInt("saves", 0),
            going = overview.optInt("going", 0),
            topSearches = rows("top_searches"),
            topEntities = rows("top_entities"),
            audienceTowns = rows("audience_towns"),
            audienceComarques = rows("audience_comarques"),
            topTowns = rows("top_towns"),
            topComarques = rows("top_comarques"),
            topLanguages = rows("top_languages"),
            topStyles = rows("top_styles"),
            topCategories = rows("top_categories"),
            topArtists = rows("top_artists"),
            topOrganizers = rows("top_organizers"),
            topVenues = rows("top_venues"),
            coaffinities = rows("coaffinities")
        )
    }

    /** Refresca el rol professional sense obligar a tancar sessió després d'una verificació manual. */
    fun refreshPublisherAccess(context: Context): Result<PublisherAccess> = runCatching {
        val savedToken = token(context) ?: error("La sessió de servidor no està disponible. Torna a iniciar sessió.")
        val connection = open("account/publisher-access.php", "GET", savedToken)
        val root = JSONObject(readResponse(connection))
        if (!root.optBoolean("ok", false)) {
            error(root.optString("message", "No s'ha pogut comprovar el perfil professional."))
        }
        val roleText = root.optString("role", "PUBLIC")
        val managed = buildSet {
            val items = root.optJSONArray("managed_artists") ?: JSONArray()
            for (i in 0 until items.length()) {
                items.optString(i).trim().takeIf(String::isNotBlank)?.let(::add)
            }
        }
        val displayName = root.optString("display_name", "").trim().takeIf(String::isNotBlank)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("role", roleText)
            .putString("display_name", displayName ?: loadSavedAccount(context)?.displayName.orEmpty())
            .putStringSet("managed_artists", managed)
            .apply()
        PublisherAccess(roleFromServer(roleText), displayName, managed)
    }

    /** v304: el permís d'autopublicar prové de la confiança del servidor, no del rol Professional. */
    data class PublicationTrust(
        val approvedCount: Int,
        val autoPublishEnabled: Boolean,
        val suspendedByReport: Boolean
    )

    fun fetchPublicationTrust(context: Context): Result<PublicationTrust> =
        getAuthenticatedJson(context, "account/publication-trust.php").mapCatching { root ->
            PublicationTrust(
                approvedCount = root.optInt("approved_count", 0).coerceAtLeast(0),
                autoPublishEnabled = root.optBoolean("auto_publish_enabled", false),
                suspendedByReport = root.optBoolean("suspended_by_report", false)
            )
        }

    fun pendingCommunityReviews(context: Context): Result<List<CommunityReviewItem>> = runCatching {
        val savedToken = token(context) ?: error("La sessió de servidor no està disponible. Torna a iniciar sessió.")
        val root = JSONObject(readResponse(open("events/community-pending.php", "GET", savedToken)))
        if (!root.optBoolean("ok", false)) error(root.optString("message", "No s'ha pogut carregar la cua."))
        val items = root.optJSONArray("items") ?: JSONArray()
        buildList {
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                add(
                    CommunityReviewItem(
                        eventId = item.optInt("event_id"),
                        submitterUserId = item.optInt("submitter_user_id"),
                        publicationMode = item.optString("publication_mode"),
                        reviewStatus = item.optString("review_status"),
                        title = item.optString("title"),
                        date = item.optString("event_date"),
                        town = item.optString("town"),
                        venue = item.optString("venue"),
                        username = item.optString("username"),
                        email = item.optString("email")
                    )
                )
            }
        }
    }

    fun reviewCommunityPublication(
        context: Context,
        eventId: Int,
        approve: Boolean,
        notes: String = ""
    ): Result<String> = runCatching {
        val response = postAuthenticatedJson(
            context,
            "events/community-review.php",
            JSONObject()
                .put("event_id", eventId)
                .put("decision", if (approve) "APPROVE" else "REJECT")
                .put("notes", notes.trim())
        ).getOrThrow()
        val count = response.optInt("approved_count", 0)
        val auto = response.optBoolean("auto_publish_enabled", false)
        if (approve) {
            if (auto) "Aprovat. Este compte ja pot autopublicar." else "Aprovat. Confiança: $count/3."
        } else "Rebutjat i retirat del feed."
    }

    fun pendingProfessionalClaims(context: Context): Result<List<ProfessionalClaimReviewItem>> = runCatching {
        val savedToken = token(context) ?: error("La sessió de servidor no està disponible. Torna a iniciar sessió.")
        val root = JSONObject(readResponse(open("professional-profiles/pending-requests.php", "GET", savedToken)))
        if (!root.optBoolean("ok", false)) error(root.optString("message", "No s'han pogut carregar les sol·licituds."))
        val items = root.optJSONArray("items") ?: JSONArray()
        buildList {
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                add(
                    ProfessionalClaimReviewItem(
                        claimId = item.optLong("id"),
                        claimantUserId = item.optInt("claimant_user_id"),
                        entityName = item.optString("requested_entity_name").ifBlank { item.optString("matched_profile_name") },
                        profileType = item.optString("requested_profile_type").ifBlank { item.optString("matched_profile_type") },
                        relation = item.optString("relation_text"),
                        contactEmail = item.optString("contact_email"),
                        contactPhone = item.optString("contact_phone"),
                        message = item.optString("message"),
                        username = item.optString("username"),
                        accountEmail = item.optString("account_email"),
                        matchedProfileName = item.optString("matched_profile_name")
                    )
                )
            }
        }
    }

    fun reviewProfessionalClaim(
        context: Context,
        claimId: Long,
        approve: Boolean,
        notes: String = ""
    ): Result<String> = runCatching {
        val response = postAuthenticatedJson(
            context,
            "professional-profiles/review-request.php",
            JSONObject()
                .put("claim_id", claimId)
                .put("decision", if (approve) "APPROVE" else "REJECT")
                .put("notes", notes.trim())
        ).getOrThrow()
        response.optString("message").ifBlank { if (approve) "Perfil verificat." else "Sol·licitud rebutjada." }
    }

    /** Tots els rols, també convidats: publica immediatament i deixa autoreport pendent. */
    fun submitEventForReview(
        context: Context,
        title: String,
        subtitle: String = "",
        artists: List<String>,
        organizers: List<String> = emptyList(),
        date: String,
        time: String,
        endDate: String = "",
        endTime: String = "",
        town: String,
        comarca: String,
        venue: String,
        category: CulturalCategory,
        styles: List<String>,
        price: Int,
        ticketSalesEnabled: Boolean,
        ticketUrl: String,
        ticketSaleOnAgenda: Boolean,
        description: String
    ): Result<CommunitySubmitResult> = runCatching {
        val payload = JSONObject()
            .put("title", title.trim())
            .put("subtitle", subtitle.trim())
            .put("artists", JSONArray(artists.map(String::trim).filter(String::isNotBlank).distinct()))
            .put("organizers", JSONArray(organizers.map(String::trim).filter(String::isNotBlank).distinct()))
            .put("date", date.trim())
            .put("time", time.trim())
            .put("end_date", endDate.trim())
            .put("end_time", endTime.trim())
            .put("town", town.trim())
            .put("comarca", comarca.trim())
            .put("venue", venue.trim())
            .put("category", category.name)
            .put("styles", JSONArray(styles.map(String::trim).filter(String::isNotBlank).distinct()))
            .put("price", price.coerceAtLeast(0))
            .put("ticket_enabled", ticketSalesEnabled)
            .put("ticket_channel", when {
                ticketSaleOnAgenda -> "AGENDA"
                ticketSalesEnabled -> "EXTERNAL"
                else -> "NONE"
            })
            .put("ticket_price", if (ticketSalesEnabled) price.coerceAtLeast(0) else JSONObject.NULL)
            .put("ticket_url", if (ticketSalesEnabled) ticketUrl.trim() else "")
            .put("description", description.trim())
            .put("guest_id", guestId(context))
        val response = jsonRequest("events/community-submit.php", "POST", payload, token(context))
        val eventId = response.optInt("event_id").takeIf { it > 0 }
            ?: error(response.optString("message", "No s'ha pogut registrar la proposta."))
        // Guardar el comprovant ABANS de mostrar la pantalla d'exit. Si l'app es tanca,
        // la publicacio continua disponible per vincular-la des d'este dispositiu.
        if (token(context).isNullOrBlank()) {
            val claim = response.optString("claim_token", "")
            require(claim.matches(Regex("[a-f0-9]{64}"))) { "Publicat, pero el servidor no ha enviat el comprovant de propietat. Contacta amb suport amb l'ID $eventId." }
            savePendingGuestClaim(context, eventId, claim)
        }
        CommunitySubmitResult(
            eventId = eventId,
            published = response.optString("status").equals("published", ignoreCase = true),
            approvedCount = response.optInt("approved_count", 0).coerceAtLeast(0),
            autoPublishEnabled = response.optBoolean("auto_publish_enabled", false)
        )
    }

    private const val PENDING_GUEST_CLAIMS = "agenda_guest_pending_claims_v313"

    private fun savePendingGuestClaim(context: Context, eventId: Int, claim: String) {
        val prefs = context.getSharedPreferences(PENDING_GUEST_CLAIMS, Context.MODE_PRIVATE)
        val current = runCatching { JSONArray(prefs.getString("items", "[]")) }.getOrDefault(JSONArray())
        val entries = JSONArray()
        for (i in 0 until current.length()) {
            val obj = current.optJSONObject(i) ?: continue
            if (obj.optInt("event_id") != eventId) entries.put(obj)
        }
        entries.put(JSONObject().put("event_id", eventId).put("claim_token", claim))
        check(prefs.edit().putString("items", entries.toString()).commit()) { "No s'ha pogut guardar el comprovant local de publicacio." }
    }

    fun pendingGuestClaimCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PENDING_GUEST_CLAIMS, Context.MODE_PRIVATE)
        return runCatching { JSONArray(prefs.getString("items", "[]")).length() }.getOrDefault(0)
    }

    private fun hasPendingGuestClaimForEvent(context: Context, eventId: Int): Boolean {
        val prefs = context.getSharedPreferences(PENDING_GUEST_CLAIMS, Context.MODE_PRIVATE)
        val entries = runCatching { JSONArray(prefs.getString("items", "[]")) }.getOrDefault(JSONArray())
        for (i in 0 until entries.length()) {
            val item = entries.optJSONObject(i) ?: continue
            if (item.optInt("event_id") == eventId && item.optString("claim_token").matches(Regex("[a-f0-9]{64}"))) return true
        }
        return false
    }

    data class GuestClaimOutcome(val claimed: Int, val failed: Int)
    fun claimPendingGuestEvents(context: Context): Result<GuestClaimOutcome> = runCatching {
        val savedToken = token(context) ?: error("Inicia sessio per conservar les publicacions.")
        val prefs = context.getSharedPreferences(PENDING_GUEST_CLAIMS, Context.MODE_PRIVATE)
        val current = JSONArray(prefs.getString("items", "[]"))
        val remaining = JSONArray()
        var claimed = 0
        var failed = 0
        for (i in 0 until current.length()) {
            val item = current.optJSONObject(i) ?: continue
            val eventId = item.optInt("event_id")
            val claimToken = item.optString("claim_token")
            if (eventId <= 0 || !claimToken.matches(Regex("[a-f0-9]{64}"))) { failed++; continue }
            val response = runCatching {
                jsonRequest("events/claim-guest.php", "POST",
                    JSONObject().put("event_id", eventId).put("claim_token", claimToken), savedToken)
            }
            if (response.isSuccess && response.getOrThrow().optBoolean("ok", false)) {
                claimed++
                val role = response.getOrThrow().optString("role", "")
                if (role.isNotBlank()) context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("role", role).apply()
            } else {
                failed++
                remaining.put(item)
            }
        }
        check(prefs.edit().putString("items", remaining.toString()).commit()) { "No s'han pogut conservar els comprovants pendents." }
        GuestClaimOutcome(claimed, failed)
    }

    fun fetchOwnSubmittedEvents(context: Context): Result<List<OwnSubmittedEvent>> = runCatching {
        val guest = URLEncoder.encode(guestId(context), StandardCharsets.UTF_8.name())
        // V312: la llista unificada funciona amb compte o sense compte.
        val root = jsonRequest("events/my-published-v312.php?guest_id=$guest", "GET", JSONObject(), token(context))
        val items = root.optJSONArray("items") ?: JSONArray()
        buildList {
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                fun stringList(name: String): List<String> {
                    val arr = item.optJSONArray(name) ?: return emptyList()
                    return buildList {
                        for (j in 0 until arr.length()) arr.optString(j).trim().takeIf(String::isNotBlank)?.let(::add)
                    }
                }
                val category = runCatching {
                    CulturalCategory.valueOf(item.optString("category", "OTHER").uppercase(Locale.ROOT))
                }.getOrDefault(CulturalCategory.OTHER)
                val styles = item.optString("styles", "")
                    .split('|', ',', ';').map(String::trim).filter(String::isNotBlank).distinct()
                add(
                    OwnSubmittedEvent(
                        eventId = item.optInt("event_id"),
                        title = item.optString("title"),
                        subtitle = item.optString("subtitle"),
                        artists = stringList("artists"),
                        organizers = stringList("organizers"),
                        date = item.optString("event_date"),
                        endDate = item.optString("end_date"),
                        time = item.optString("event_time").take(5),
                        endTime = item.optString("end_time").take(5),
                        town = item.optString("town"),
                        comarca = item.optString("comarca"),
                        venue = item.optString("venue"),
                        category = category,
                        styles = styles,
                        price = item.optDouble("price", 0.0).roundToInt(),
                        ticketEnabled = item.optInt("ticket_enabled", 0) == 1,
                        ticketUrl = item.optString("ticket_url"),
                        ticketSalePending = item.optInt("ticket_sale_pending", 0) == 1,
                        description = item.optString("description"),
                        status = item.optString("status"),
                        reviewStatus = item.optString("review_status"),
                        publicationMode = item.optString("publication_mode"),
                        posterUrl = resolvePublicUrl(item.optString("poster_url")).takeIf(String::isNotBlank)
                    )
                )
            }
        }
    }

    fun deleteOwnSubmittedEvent(context: Context, eventId: Int): Result<Unit> = runCatching {
        jsonRequest("events/community-delete.php", "POST",
            JSONObject().put("event_id", eventId).put("guest_id", guestId(context)), token(context))
        Unit
    }

    fun updateOwnSubmittedEvent(
        context: Context,
        eventId: Int,
        title: String,
        subtitle: String,
        artists: List<String>,
        organizers: List<String>,
        date: String,
        endDate: String,
        time: String,
        endTime: String,
        town: String,
        comarca: String,
        venue: String,
        category: CulturalCategory,
        styles: List<String>,
        price: Int,
        ticketEnabled: Boolean,
        ticketUrl: String,
        ticketSaleOnAgenda: Boolean,
        description: String
    ): Result<String> = runCatching {
        val payload = JSONObject()
            .put("event_id", eventId)
            .put("guest_id", guestId(context))
            .put("title", title.trim())
            .put("subtitle", subtitle.trim())
            .put("artists", JSONArray(artists.map(String::trim).filter(String::isNotBlank).distinct()))
            .put("organizers", JSONArray(organizers.map(String::trim).filter(String::isNotBlank).distinct()))
            .put("date", date.trim())
            .put("end_date", endDate.trim())
            .put("time", time.trim())
            .put("end_time", endTime.trim())
            .put("town", town.trim())
            .put("comarca", comarca.trim())
            .put("venue", venue.trim())
            .put("category", category.name)
            .put("styles", JSONArray(styles.map(String::trim).filter(String::isNotBlank).distinct()))
            .put("price", price.coerceAtLeast(0))
            .put("ticket_enabled", ticketEnabled)
            .put("ticket_channel", when {
                ticketSaleOnAgenda -> "AGENDA"
                !ticketEnabled -> "NONE"
                else -> "EXTERNAL"
            })
            .put("ticket_price", if (ticketEnabled) price.coerceAtLeast(0) else JSONObject.NULL)
            .put("ticket_url", if (ticketEnabled) ticketUrl.trim() else "")
            .put("description", description.trim())
        val root = jsonRequest("events/community-update.php", "POST", payload, token(context))
        // V316: el PHP emet un comprovant renovat en CADA edició de convidat.
        // Guardar-lo abans d'obrir el registre, comprovant l'ID concret editat;
        // una altra publicació pendent no és garantia de propietat d'esta.
        if (token(context).isNullOrBlank()) {
            val newClaimToken = root.optString("claim_token", "")
            if (newClaimToken.matches(Regex("[a-f0-9]{64}"))) {
                savePendingGuestClaim(context, eventId, newClaimToken)
            }
            check(hasPendingGuestClaimForEvent(context, eventId)) {
                "L'esdeveniment $eventId està guardat, però falta el comprovant de vinculació. Verifica que community-update.php siga la versió v316 i torna a guardar l'edició."
            }
        }
        root.optString("message", "Canvis guardats.")
    }

    fun uploadOwnSubmittedEventPoster(
        context: Context,
        eventId: Int,
        imageUri: String,
        onProgress: (Int) -> Unit = {}
    ): Result<String> = runCatching {
        onProgress(5)
        val prepared = prepareCardImageUpload(context, imageUri)
        onProgress(35)
        val payload = JSONObject()
            .put("event_id", eventId)
            .put("guest_id", guestId(context))
            .put("image_focus_x", 0.5)
            .put("image_focus_y", 0.5)
            .put("image_scale", 1.0)
            .put("mime_type", prepared.mime)
            .put("image_base64", Base64.encodeToString(prepared.bytes, Base64.NO_WRAP))
        onProgress(70)
        val root = jsonRequest("events/community-poster.php", "POST", payload, token(context))
        onProgress(100)
        resolvePublicUrl(root.optString("image_url")).takeIf(String::isNotBlank)
            ?: error(root.optString("message", "No s'ha pogut guardar el cartell."))
    }

    /** Marca una publicació professional perquè entre en les passades d'auditoria. */
    fun markEventForAudit(context: Context, eventId: Int): Result<Unit> = runCatching {
        require(eventId > 0) { "Esdeveniment no vàlid." }
        val response = postAuthenticatedJson(
            context,
            "events/mark-audit.php",
            JSONObject().put("event_id", eventId)
        ).getOrThrow()
        if (!response.optBoolean("ok", false)) {
            error(response.optString("message", "No s'ha pogut marcar per a auditoria."))
        }
        Unit
    }

    /** Sol·licitud perquè administració contacte manualment amb l'artista/organitzador. */
    fun requestProfessionalProfileClaim(
        context: Context,
        entityName: String,
        profileType: String,
        relation: String,
        contact: String,
        message: String
    ): Result<String> = runCatching {
        val cleanContact = contact.trim()
        val looksEmail = cleanContact.contains('@')
        val payload = JSONObject()
            .put("entity_name", entityName.trim())
            .put("profile_type", if (profileType.equals("ARTIST", true) || profileType.equals("GROUP", true)) "GROUP" else "AGENCY")
            .put("relation", relation.trim())
            .put("contact_email", if (looksEmail) cleanContact else "")
            .put("contact_phone", if (looksEmail) "" else cleanContact)
            .put("message", message.trim())
        val response = postAuthenticatedJson(context, "professional-profiles/request.php", payload).getOrThrow()
        response.optString("message").ifBlank { "Sol·licitud enviada. Et contactarem manualment." }
    }

    fun createEvent(
        context: Context,
        posterUri: String?,
        title: String,
        subtitle: String = "",
        artists: List<String>,
        date: String,
        time: String,
        endDate: String = "",
        endTime: String = "",
        town: String,
        comarca: String,
        venue: String,
        category: CulturalCategory,
        styles: List<String>,
        price: Int,
        ticketSalesEnabled: Boolean,
        ticketUrl: String,
        description: String,
        artistImageUrl: String? = null,
        venueImageUrl: String? = null,
        townImageUrl: String? = null
    ): Result<EventCreateResult> = runCatching {
        val savedToken = token(context) ?: error("La sessió de servidor no està disponible. Torna a iniciar sessió.")
        val boundary = "----AgendaValenciana${System.currentTimeMillis()}"
        val body = ByteArrayOutputStream().use { buffer ->
            DataOutputStream(buffer).use { out ->
                fun field(name: String, value: String) {
                    out.writeBytes("--$boundary\r\n")
                    out.writeBytes("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
                    out.write(value.toByteArray(StandardCharsets.UTF_8))
                    out.writeBytes("\r\n")
                }
                field("title", title.trim())
                field("subtitle", subtitle.trim())
                field("artists", artists.map(String::trim).filter(String::isNotBlank).distinct().joinToString(" | "))
                field("date", date.trim())
                field("time", time.trim())
                field("end_date", endDate.trim())
                field("end_time", endTime.trim())
                field("town", town.trim())
                field("comarca", comarca.trim())
                field("venue", venue.trim())
                field("category", category.name)
                field("styles", styles.map(String::trim).filter(String::isNotBlank).distinct().joinToString(" | "))
                field("price", if (price >= 0) price.toString() else "")
                field("ticket_enabled", if (ticketSalesEnabled) "1" else "0")
                field("ticket_price", if (ticketSalesEnabled && price >= 0) price.toString() else "")
                field("ticket_url", if (ticketSalesEnabled) ticketUrl.trim() else "")
                field("description", description.trim())
                out.writeBytes("--$boundary--\r\n")
                out.flush()
            }
            buffer.toByteArray()
        }
        val connection = open("events/create.php", "POST", savedToken).apply {
            doOutput = true
            readTimeout = 45_000
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            setFixedLengthStreamingMode(body.size)
        }
        try {
            connection.outputStream.use { it.write(body); it.flush() }
            val root = JSONObject(readResponse(connection))
            if (!root.optBoolean("ok", false)) error(root.optString("message", "No s'ha pogut publicar l'esdeveniment."))
            val event = parseEvent(root.getJSONObject("event"), context, loadCachedEntityImages(context))
            runCatching {
                ensureEventEntities(
                    context = context,
                    artists = artists,
                    venue = venue,
                    town = town,
                    artistImageUrl = artistImageUrl,
                    venueImageUrl = venueImageUrl,
                    townImageUrl = townImageUrl
                )
            }
            EventCreateResult(event)
        } finally {
            connection.disconnect()
        }
    }

    fun updateEvent(
        context: Context,
        concertId: Int,
        title: String? = null,
        subtitle: String? = null,
        artists: List<String>? = null,
        styles: List<String>? = null,
        date: String? = null,
        time: String? = null,
        endDate: String? = null,
        endTime: String? = null,
        town: String? = null,
        comarca: String? = null,
        venue: String? = null,
        description: String? = null,
        ticketPrice: Int? = null,
        clearTicketPrice: Boolean = false,
        ticketSalesEnabled: Boolean? = null,
        ticketUrl: String? = null
    ): Result<Concert> = runCatching {
        val savedToken = token(context) ?: error("La sessió de servidor no està disponible. Torna a iniciar sessió.")
        val payload = JSONObject().put("id", concertId)
        title?.let { payload.put("title", it.trim()) }
        subtitle?.let { payload.put("subtitle", it.trim()) }
        artists?.let { payload.put("artists", JSONArray(it)) }
        styles?.let { payload.put("styles", it.joinToString(",")) }
        date?.let { payload.put("date", it.trim()) }
        time?.let { payload.put("time", it.trim()) }
        endDate?.let { payload.put("end_date", it.trim()) }
        endTime?.let { payload.put("end_time", it.trim()) }
        town?.let { payload.put("town", it.trim()) }
        comarca?.let { payload.put("comarca", it.trim()) }
        venue?.let { payload.put("venue", it.trim()) }
        description?.let { payload.put("description", it.trim()) }
        if (clearTicketPrice) {
            payload.put("ticket_price", "")
        } else if (ticketPrice != null) {
            payload.put("ticket_price", ticketPrice.coerceAtLeast(0))
        }
        ticketSalesEnabled?.let { payload.put("ticket_enabled", it) }
        ticketUrl?.let { payload.put("ticket_url", it.trim()) }

        val response = jsonRequest("events/update.php", "POST", payload, savedToken)
        parseEvent(response.getJSONObject("event"), context, loadCachedEntityImages(context))
    }

    private data class PreparedImageUpload(
        val bytes: ByteArray,
        val mime: String,
        val extension: String
    )

    data class MediaUploadResult(
        val key: String,
        val imageUrl: String,
        val byteSize: Int
    )

    data class CardVisualUpdateResult(
        val imageSource: String,
        val entityName: String?,
        val imageUrl: String?,
        val uploaded: Boolean,
        val focusX: Float = 0.5f,
        val focusY: Float = 0.5f,
        val scale: Float = 1f
    )

    data class RemoteUserProfile(
        val username: String,
        val displayName: String,
        val bio: String,
        val imageUrl: String?,
        val homeTown: String = "",
        val discipline: String = "",
        val companyName: String = "",
        val castText: String = "",
        val publisherName: String = "",
        val booksText: String = "",
        val worksText: String = "",
        val spacesText: String = ""
    )

    data class OwnerMediaUpdateResult(
        val ownerType: String,
        val ownerKey: String,
        val imageUrl: String?,
        val mediaKey: String?,
        val uploaded: Boolean
    )

    /**
     * 0.19.2: totes les imatges noves es preparen al client i es guarden com a fitxers
     * físics dins de /app/images. MySQL conserva només la ruta canònica.
     */
    private fun prepareCardImageUpload(context: Context, rawUri: String): PreparedImageUpload {
        val uri = Uri.parse(rawUri)
        val resolver = context.contentResolver
        fun localFile(): File? = when {
            uri.scheme.equals("file", ignoreCase = true) -> uri.path?.let(::File)
            uri.scheme.isNullOrBlank() && rawUri.startsWith("/") -> File(rawUri)
            else -> null
        }?.takeIf { it.exists() && it.isFile && it.canRead() }
        fun openInput() = localFile()?.inputStream() ?: runCatching { resolver.openInputStream(uri) }.getOrNull()
        // No enviem el fitxer cru: decodifiquem i re-codifiquem a JPEG compatible
        // amb el servidor. Això també normalitza fotos procedents de proveïdors Android.

        // IMPORTANT: amb inJustDecodeBounds=true BitmapFactory retorna null expressament.
        // No podem usar el resultat de decodeStream() per decidir si l'InputStream s'ha obert.
        // A més, per als URI file:// temporals usem decodeFile(), exactament igual que la
        // previsualització, per evitar diferències entre preview i upload.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val sourceFile = localFile()
        if (sourceFile != null) {
            BitmapFactory.decodeFile(sourceFile.absolutePath, bounds)
        } else {
            val boundsInput = openInput() ?: error("No s'ha pogut obrir la imatge seleccionada.")
            boundsInput.use { BitmapFactory.decodeStream(it, null, bounds) }
        }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "El fitxer seleccionat no és una imatge vàlida." }

        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1600) sample *= 2
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = if (sourceFile != null) {
            BitmapFactory.decodeFile(sourceFile.absolutePath, options)
        } else {
            openInput()?.use { BitmapFactory.decodeStream(it, null, options) }
        } ?: error("No s'ha pogut preparar la imatge per pujar-la.")
        val maxSide = maxOf(decoded.width, decoded.height)
        var bitmap = if (maxSide > 1600) {
            val scale = 1600f / maxSide.toFloat()
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).toInt().coerceAtLeast(1),
                (decoded.height * scale).toInt().coerceAtLeast(1),
                true
            ).also { if (it !== decoded) decoded.recycle() }
        } else decoded

        try {
            fun jpegBytes(source: Bitmap, quality: Int): ByteArray = ByteArrayOutputStream().use { buffer ->
                source.compress(Bitmap.CompressFormat.JPEG, quality, buffer)
                buffer.toByteArray()
            }

            var quality = 82
            var compressed = jpegBytes(bitmap, quality)
            while (compressed.size > 600_000 && quality > 42) {
                quality -= 8
                compressed = jpegBytes(bitmap, quality)
            }

            if (compressed.size > 600_000 && maxOf(bitmap.width, bitmap.height) > 1100) {
                val scale = 1100f / maxOf(bitmap.width, bitmap.height).toFloat()
                val smaller = Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt().coerceAtLeast(1),
                    (bitmap.height * scale).toInt().coerceAtLeast(1),
                    true
                )
                if (smaller !== bitmap) {
                    bitmap.recycle()
                    bitmap = smaller
                }
                compressed = jpegBytes(bitmap, 68)
            }

            require(compressed.isNotEmpty() && compressed.size <= 1_200_000) {
                "No s'ha pogut reduir la imatge a una mida admesa pel servidor."
            }
            return PreparedImageUpload(compressed, "image/jpeg", "jpg")
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    private fun ensureImageServerReady(context: Context, savedToken: String) {
        val root = try {
            jsonRequest("images/health.php", "GET", JSONObject(), savedToken)
        } catch (error: Throwable) {
            throw IllegalStateException(
                "No s’ha pogut connectar amb el servidor d’imatges /app/images: ${error.message ?: "error desconegut"}",
                error
            )
        }
        if (!root.optBoolean("ready", false)) {
            val problems = root.optJSONArray("problems") ?: JSONArray()
            val detail = buildList {
                for (i in 0 until problems.length()) problems.optString(i).trim().takeIf(String::isNotBlank)?.let(::add)
            }.joinToString(" ").ifBlank { "El servidor no pot escriure en /app/images." }
            error(detail)
        }
    }

    private fun uploadImageForOwner(
        context: Context,
        ownerType: String,
        ownerKey: String,
        prepared: PreparedImageUpload,
        onProgress: (Int) -> Unit = {}
    ): MediaUploadResult {
        val savedToken = token(context) ?: error("La sessió de servidor no està disponible. Torna a iniciar sessió.")
        // El health-check és diagnòstic: no ha de bloquejar una pujada que el servidor sí pot acceptar.
        // Marquem progrés immediatament perquè la UI no quede clavada al 0%.
        onProgress(1)
        runCatching { ensureImageServerReady(context, savedToken) }
        onProgress(3)

        fun parseUpload(root: JSONObject): MediaUploadResult {
            if (!root.optBoolean("ok", false)) error(root.optString("message", "No s'ha pogut pujar la foto."))
            val imageUrl = resolvePublicUrl(root.optString("image_url"))
            require(imageUrl.startsWith("https://", ignoreCase = true) || imageUrl.startsWith("http://", ignoreCase = true)) {
                "El servidor no ha retornat una URL vàlida de la foto."
            }
            return MediaUploadResult(key = "", imageUrl = imageUrl, byteSize = root.optInt("byte_size", prepared.bytes.size))
        }

        val multipart = runCatching {
            val boundary = "----AgendaImage${System.currentTimeMillis()}"
            val body = ByteArrayOutputStream().use { buffer ->
                DataOutputStream(buffer).use { out ->
                    fun field(name: String, value: String) {
                        out.writeBytes("--$boundary\r\n")
                        out.writeBytes("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
                        out.write(value.toByteArray(StandardCharsets.UTF_8))
                        out.writeBytes("\r\n")
                    }
                    field("owner_type", ownerType)
                    field("owner_key", ownerKey)
                    out.writeBytes("--$boundary\r\n")
                    out.writeBytes("Content-Disposition: form-data; name=\"image\"; filename=\"image.${prepared.extension}\"\r\n")
                    out.writeBytes("Content-Type: ${prepared.mime}\r\n\r\n")
                    out.write(prepared.bytes)
                    out.writeBytes("\r\n--$boundary--\r\n")
                    out.flush()
                }
                buffer.toByteArray()
            }
            val connection = open("images/upload.php", "POST", savedToken).apply {
                doOutput = true
                readTimeout = 45_000
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                setFixedLengthStreamingMode(body.size)
            }
            try {
                onProgress(0)
                connection.outputStream.buffered(32 * 1024).use { output ->
                    var offset = 0
                    while (offset < body.size) {
                        val count = minOf(32 * 1024, body.size - offset)
                        output.write(body, offset, count)
                        offset += count
                        onProgress(minOf(92, ((offset.toLong() * 92L) / body.size.coerceAtLeast(1)).toInt()))
                    }
                    output.flush()
                }
                parseUpload(JSONObject(readResponse(connection)))
            } finally { connection.disconnect() }
        }
        multipart.getOrNull()?.let { result -> onProgress(100); return result }

        // Alguns hostings/proxies bloquegen multipart encara que PHP puga escriure la carpeta.
        // Reintent compatible: la mateixa imatge preparada, en JSON/base64, al mateix endpoint.
        val compatible = runCatching {
            onProgress(20)
            val payload = JSONObject()
                .put("owner_type", ownerType)
                .put("owner_key", ownerKey)
                .put("mime_type", prepared.mime)
                .put("image_base64", Base64.encodeToString(prepared.bytes, Base64.NO_WRAP))
            onProgress(55)
            val result = parseUpload(jsonRequest("images/upload.php", "POST", payload, savedToken))
            onProgress(100)
            result
        }
        compatible.getOrNull()?.let { return it }

        throw IllegalStateException(
            "No s'ha pogut pujar la imatge. Multipart: ${multipart.exceptionOrNull()?.message ?: "error"}. Reintent compatible: ${compatible.exceptionOrNull()?.message ?: "error"}.",
            compatible.exceptionOrNull() ?: multipart.exceptionOrNull()
        )
    }

    private fun removeImageForOwner(context: Context, ownerType: String, ownerKey: String) {
        val savedToken = token(context) ?: error("La sessió de servidor no està disponible. Torna a iniciar sessió.")
        jsonRequest(
            "images/remove.php", "POST",
            JSONObject().put("owner_type", ownerType).put("owner_key", ownerKey),
            savedToken
        )
    }

    fun updateOwnerMedia(
        context: Context,
        ownerType: String,
        ownerKey: String,
        imageUri: String? = null,
        removeImage: Boolean = false,
        cropX: Float = 0.5f,
        cropY: Float = 0.5f,
        cropZoom: Float = 1f,
        onUploadProgress: (Int) -> Unit = {}
    ): Result<OwnerMediaUpdateResult> = runCatching {
        require(!(removeImage && !imageUri.isNullOrBlank())) { "No pots pujar i eliminar una imatge al mateix temps." }
        if (removeImage) {
            removeImageForOwner(context, ownerType, ownerKey)
            invalidateEntityImagesCache(context)
            OwnerMediaUpdateResult(ownerType.uppercase(), ownerKey, null, null, false)
        } else if (!imageUri.isNullOrBlank() && !imageUri.startsWith("http://") && !imageUri.startsWith("https://")) {
            val prepared = prepareCardImageUpload(context, imageUri)
            val uploaded = uploadImageForOwner(context, ownerType, ownerKey, prepared, onUploadProgress)
            invalidateEntityImagesCache(context)
            File(File(context.filesDir, CACHE_DIR), EVENT_VISUALS_CACHE).delete()
            OwnerMediaUpdateResult(ownerType.uppercase(), ownerKey, uploaded.imageUrl, null, true)
        } else {
            val savedToken = token(context)
            val path = "images/owner.php?owner_type=" + URLEncoder.encode(ownerType, StandardCharsets.UTF_8.name()) +
                "&owner_key=" + URLEncoder.encode(ownerKey, StandardCharsets.UTF_8.name())
            val connection = open(path, "GET", savedToken)
            try {
                val root = JSONObject(readResponse(connection))
                OwnerMediaUpdateResult(ownerType.uppercase(), ownerKey, resolvePublicUrl(root.optString("image_url")).takeIf(String::isNotBlank), null, false)
            } finally { connection.disconnect() }
        }
    }

    fun fetchUserProfiles(context: Context): Result<List<RemoteUserProfile>> = runCatching {
        val root = jsonRequest("user-profiles/catalog.php", "GET", JSONObject(), token(context))
        val arr = root.optJSONArray("profiles") ?: JSONArray()
        buildList {
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val username = item.optString("username").trim()
                if (username.isBlank()) continue
                add(RemoteUserProfile(
                    username = username,
                    displayName = item.optString("display_name", username).ifBlank { username },
                    bio = item.optString("bio"),
                    imageUrl = resolvePublicUrl(item.optString("image_url")).takeIf(String::isNotBlank),
                    homeTown = item.optString("home_town"),
                    discipline = item.optString("discipline"),
                    companyName = item.optString("company_name"),
                    castText = item.optString("cast_text"),
                    publisherName = item.optString("publisher_name"),
                    booksText = item.optString("books_text"),
                    worksText = item.optString("works_text"),
                    spacesText = item.optString("spaces_text")
                ))
            }
        }
    }

    fun updateUserProfile(
        context: Context,
        username: String,
        displayName: String,
        bio: String,
        imageUri: String?,
        discipline: String = "",
        companyName: String = "",
        castText: String = "",
        publisherName: String = "",
        booksText: String = "",
        worksText: String = "",
        spacesText: String = "",
        removeImage: Boolean = false,
        onUploadProgress: (Int) -> Unit = {}
    ): Result<RemoteUserProfile> = runCatching {
        val savedToken = token(context) ?: error("La sessió de servidor no està disponible. Torna a iniciar sessió.")
        val uploaded = imageUri?.takeIf { it.isNotBlank() && !it.startsWith("http://") && !it.startsWith("https://") }?.let { rawUri ->
            uploadImageForOwner(context, "USER", username, prepareCardImageUpload(context, rawUri), onUploadProgress)
        }
        val payload = JSONObject()
            .put("username", username.trim())
            .put("display_name", displayName.trim())
            .put("bio", bio.trim())
            .put("discipline", discipline.trim())
            .put("company_name", companyName.trim())
            .put("cast_text", castText.trim())
            .put("publisher_name", publisherName.trim())
            .put("books_text", booksText.trim())
            .put("works_text", worksText.trim())
            .put("spaces_text", spacesText.trim())
            .put("remove_image", removeImage)
        val root = jsonRequest("user-profiles/update.php", "POST", payload, savedToken)
        val p = root.getJSONObject("profile")
        RemoteUserProfile(
            username = p.optString("username", username),
            displayName = p.optString("display_name", displayName),
            bio = p.optString("bio", bio),
            imageUrl = resolvePublicUrl(p.optString("image_url")).takeIf(String::isNotBlank) ?: uploaded?.imageUrl,
            homeTown = p.optString("home_town"),
            discipline = p.optString("discipline", discipline),
            companyName = p.optString("company_name", companyName),
            castText = p.optString("cast_text", castText),
            publisherName = p.optString("publisher_name", publisherName),
            booksText = p.optString("books_text", booksText),
            worksText = p.optString("works_text", worksText),
            spacesText = p.optString("spaces_text", spacesText)
        )
    }

    /**
     * Cartells d'esdeveniment: pujada atòmica. El mateix endpoint guarda el fitxer,
     * l'assigna a l'esdeveniment i conserva l'enquadrament. Això evita el vell flux
     * de dues peticions (images/upload + events/card-image) que podia deixar la UI al 0%.
     */
    private fun uploadEventPosterVisual(
        context: Context,
        eventId: Int,
        prepared: PreparedImageUpload,
        focusX: Float,
        focusY: Float,
        scale: Float,
        onProgress: (Int) -> Unit
    ): JSONObject {
        val savedToken = token(context) ?: error("La sessió de servidor no està disponible. Torna a iniciar sessió.")

        fun parse(root: JSONObject): JSONObject {
            if (!root.optBoolean("ok", false)) error(root.optString("message", "No s'ha pogut guardar el cartell."))
            val imageUrl = resolvePublicUrl(root.optString("image_url"))
            require(imageUrl.startsWith("https://", true)) { "El servidor no ha retornat una URL vàlida del cartell." }
            root.put("resolved_image_url", imageUrl)
            return root
        }

        val multipart = runCatching {
            val boundary = "----AgendaEventPoster${System.currentTimeMillis()}"
            val body = ByteArrayOutputStream().use { buffer ->
                DataOutputStream(buffer).use { out ->
                    fun field(name: String, value: String) {
                        out.writeBytes("--$boundary\r\n")
                        out.writeBytes("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
                        out.write(value.toByteArray(StandardCharsets.UTF_8))
                        out.writeBytes("\r\n")
                    }
                    field("event_id", eventId.toString())
                    field("image_focus_x", focusX.coerceIn(0f, 1f).toString())
                    field("image_focus_y", focusY.coerceIn(0f, 1f).toString())
                    field("image_scale", scale.coerceIn(1f, 4f).toString())
                    out.writeBytes("--$boundary\r\n")
                    out.writeBytes("Content-Disposition: form-data; name=\"poster\"; filename=\"poster.${prepared.extension}\"\r\n")
                    out.writeBytes("Content-Type: ${prepared.mime}\r\n\r\n")
                    out.write(prepared.bytes)
                    out.writeBytes("\r\n--$boundary--\r\n")
                    out.flush()
                }
                buffer.toByteArray()
            }
            val connection = open("events/poster.php", "POST", savedToken).apply {
                doOutput = true
                connectTimeout = 20_000
                readTimeout = 60_000
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                setFixedLengthStreamingMode(body.size)
            }
            try {
                onProgress(10)
                connection.outputStream.buffered(32 * 1024).use { output ->
                    var offset = 0
                    while (offset < body.size) {
                        val count = minOf(32 * 1024, body.size - offset)
                        output.write(body, offset, count)
                        offset += count
                        val pct = 10 + ((offset.toLong() * 82L) / body.size.coerceAtLeast(1)).toInt()
                        onProgress(pct.coerceIn(10, 92))
                    }
                    output.flush()
                }
                parse(JSONObject(readResponse(connection)))
            } finally {
                connection.disconnect()
            }
        }
        multipart.getOrNull()?.let {
            onProgress(100)
            return it
        }

        // Fallback per a hostings que bloquegen multipart: mateix endpoint, JSON/base64.
        val compatible = runCatching {
            onProgress(25)
            val payload = JSONObject()
                .put("event_id", eventId)
                .put("image_focus_x", focusX.coerceIn(0f, 1f).toDouble())
                .put("image_focus_y", focusY.coerceIn(0f, 1f).toDouble())
                .put("image_scale", scale.coerceIn(1f, 4f).toDouble())
                .put("mime_type", prepared.mime)
                .put("image_base64", Base64.encodeToString(prepared.bytes, Base64.NO_WRAP))
            onProgress(60)
            parse(jsonRequest("events/poster.php", "POST", payload, savedToken))
        }
        compatible.getOrNull()?.let {
            onProgress(100)
            return it
        }

        val first = multipart.exceptionOrNull()?.message.orEmpty()
        val second = compatible.exceptionOrNull()?.message.orEmpty()
        throw IllegalStateException(
            "No s'ha pogut guardar el cartell. Pujada directa: ${first.ifBlank { "error" }}. Reintent: ${second.ifBlank { "error" }}.",
            compatible.exceptionOrNull() ?: multipart.exceptionOrNull()
        )
    }

    fun updateEventCardVisual(
        context: Context,
        concertId: Int,
        imageSource: String,
        entityName: String?,
        imageUri: String? = null,
        removeImage: Boolean = false,
        imageFocusX: Float = 0.5f,
        imageFocusY: Float = 0.5f,
        imageScale: Float = 1f,
        onUploadProgress: (Int) -> Unit = {}
    ): Result<CardVisualUpdateResult> = runCatching {
        val savedToken = token(context) ?: error("La sessió de servidor no està disponible. Torna a iniciar sessió.")
        val source = imageSource.trim().uppercase().ifBlank { "AUTO" }
        require(!(removeImage && !imageUri.isNullOrBlank())) { "No pots pujar i eliminar una imatge al mateix temps." }
        if (!imageUri.isNullOrBlank() && source == "AUTO") error("Per pujar una foto, tria Esdeveniment, Grup, Sala, Organitzador, Poble o Categoria.")
        // Els cartells d'esdeveniment usen un endpoint atòmic: fitxer + assignació + crop.
        // Així no depenem de dues peticions encadenades per a l'acció "Pujar i guardar foto".
        if (!imageUri.isNullOrBlank() && source == "EVENT") {
            onUploadProgress(2)
            val prepared = prepareCardImageUpload(context, imageUri)
            onUploadProgress(8)
            val root = uploadEventPosterVisual(
                context = context,
                eventId = concertId,
                prepared = prepared,
                focusX = imageFocusX,
                focusY = imageFocusY,
                scale = imageScale,
                onProgress = onUploadProgress
            )
            val effectiveUrl = root.optString("resolved_image_url").takeIf(String::isNotBlank)
                ?: resolvePublicUrl(root.optString("image_url")).takeIf(String::isNotBlank)
                ?: error("El servidor ha guardat el cartell però no n'ha retornat la URL.")
            invalidateEntityImagesCache(context)
            File(File(context.filesDir, CACHE_DIR), EVENT_VISUALS_CACHE).delete()
            return@runCatching CardVisualUpdateResult(
                imageSource = "EVENT",
                entityName = null,
                imageUrl = effectiveUrl,
                uploaded = true,
                focusX = root.optDouble("image_focus_x", imageFocusX.toDouble()).toFloat().coerceIn(0f, 1f),
                focusY = root.optDouble("image_focus_y", imageFocusY.toDouble()).toFloat().coerceIn(0f, 1f),
                scale = root.optDouble("image_scale", imageScale.toDouble()).toFloat().coerceIn(1f, 4f)
            )
        }

        val ownerType = when (source) {
            "EVENT" -> "EVENT"
            "ARTIST" -> "GROUP"
            "VENUE", "ORGANIZER", "TOWN", "CATEGORY" -> source
            else -> null
        }
        val ownerKey = if (source == "EVENT") concertId.toString() else entityName.orEmpty().trim()
        val uploaded = if (!imageUri.isNullOrBlank() && ownerType != null) {
            onUploadProgress(1)
            val prepared = prepareCardImageUpload(context, imageUri)
            onUploadProgress(8)
            uploadImageForOwner(context, ownerType, ownerKey, prepared, onUploadProgress)
        } else null
        val payload = JSONObject()
            .put("event_id", concertId)
            .put("image_source", source)
            .put("entity_name", entityName.orEmpty().trim())
            .put("remove_image", removeImage)
            .put("image_focus_x", imageFocusX.coerceIn(0f, 1f).toDouble())
            .put("image_focus_y", imageFocusY.coerceIn(0f, 1f).toDouble())
            .put("image_scale", imageScale.coerceIn(1f, 4f).toDouble())
        val root = jsonRequest("events/card-image.php", "POST", payload, savedToken)
        val effectiveUrl = resolvePublicUrl(root.optString("effective_image_url")).takeIf(String::isNotBlank) ?: uploaded?.imageUrl
        invalidateEntityImagesCache(context)
        File(File(context.filesDir, CACHE_DIR), EVENT_VISUALS_CACHE).delete()
        CardVisualUpdateResult(
            imageSource = source,
            entityName = root.optString("entity_name").takeIf { it.isNotBlank() && it != "null" } ?: entityName,
            imageUrl = if (removeImage) null else effectiveUrl,
            uploaded = uploaded != null,
            focusX = root.optDouble("image_focus_x", imageFocusX.toDouble()).toFloat().coerceIn(0f, 1f),
            focusY = root.optDouble("image_focus_y", imageFocusY.toDouble()).toFloat().coerceIn(0f, 1f),
            scale = root.optDouble("image_scale", imageScale.toDouble()).toFloat().coerceIn(1f, 4f)
        )
    }

    /** Confirma al servidor la primera URL de card que el dispositiu ha pogut carregar. */
    fun confirmResolvedCardImage(
        context: Context,
        concertId: Int,
        imageUrl: String,
        previousResolvedUrl: String? = null
    ): Result<Unit> = runCatching {
        val cleanUrl = imageUrl.trim()
        if (concertId <= 0 || !cleanUrl.startsWith("https://", ignoreCase = true)) return@runCatching
        val previous = previousResolvedUrl?.trim().orEmpty()
        if (previous.equals(cleanUrl, ignoreCase = true)) return@runCatching
        val reportKey = "$concertId|$cleanUrl|$previous"
        if (!reportedResolvedCardImages.add(reportKey)) return@runCatching
        try {
            val payload = JSONObject()
                .put("event_id", concertId)
                .put("resolved_url", cleanUrl)
                .put("previous_resolved_url", previous)
            val root = jsonRequest("events/resolve-card-image.php", "POST", payload, token(context))
            if (root.optBoolean("changed", false)) {
                File(File(context.filesDir, CACHE_DIR), EVENT_VISUALS_CACHE).delete()
            }
        } catch (t: Throwable) {
            reportedResolvedCardImages.remove(reportKey)
            throw t
        }
    }

    fun replaceEventOrganizers(context: Context, concertId: Int, organizers: List<String>): Result<List<String>> = runCatching {
        val savedToken = token(context) ?: error("La sessió de servidor no està disponible. Torna a iniciar sessió.")
        val clean = organizers.map(String::trim).filter(String::isNotBlank).distinctBy { it.lowercase() }
        val payload = JSONObject().put("event_id", concertId).put("organizers", JSONArray(clean))
        val root = jsonRequest("events/replace-organizers.php", "POST", payload, savedToken)
        File(File(context.filesDir, CACHE_DIR), EVENT_VISUALS_CACHE).delete()
        val arr = root.optJSONArray("organizers") ?: JSONArray()
        buildList { for (i in 0 until arr.length()) arr.optString(i).trim().takeIf(String::isNotBlank)?.let(::add) }
    }

    fun deleteEvent(context: Context, concertId: Int): Result<Unit> = runCatching {
        val savedToken = token(context) ?: error("La sessió de servidor no està disponible. Torna a iniciar sessió.")
        jsonRequest("events/delete.php", "POST", JSONObject().put("event_id", concertId), savedToken)
        AgendaLocalDatabase.deleteEvent(context, concertId)
        File(File(context.filesDir, CACHE_DIR), API_CACHE).delete()
        File(File(context.filesDir, CACHE_DIR), EVENT_VISUALS_CACHE).delete()
        Unit
    }

    /** Guarda modalitats de venda PENDENTS. No activa el cobrament. */
    fun saveEventTicketDrafts(context: Context, eventId: Int, options: JSONArray): Result<Unit> = runCatching {
        require(eventId > 0)
        jsonRequest("events/ticket-drafts.php", "POST", JSONObject()
            .put("action", "save").put("event_id", eventId)
            .put("guest_id", guestId(context)).put("options", options), token(context))
        Unit
    }

    fun getEventTicketDrafts(context: Context, eventId: Int): Result<JSONArray> = runCatching {
        require(eventId > 0)
        jsonRequest("events/ticket-drafts.php", "POST", JSONObject()
            .put("action", "get").put("event_id", eventId)
            .put("guest_id", guestId(context)), token(context)).optJSONArray("options") ?: JSONArray()
    }

    fun fetchTicketOptions(context: Context, concertId: Int): Result<TicketOptionsInfo> = runCatching {
        require(concertId > 0) { "Esdeveniment no vàlid." }
        val path = "tickets/options.php?event_id=" + URLEncoder.encode(concertId.toString(), StandardCharsets.UTF_8.name())
        val connection = open(path, "GET", token(context))
        val root = JSONObject(readResponse(connection))
        if (!root.optBoolean("ok", false)) error(root.optString("message", "No s'han pogut carregar les modalitats d'entrada."))
        val options = buildList {
            val arr = root.optJSONArray("options") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val optionId = item.optLong("id", 0L)
                if (optionId <= 0L) continue
                add(
                    TicketOption(
                        id = optionId,
                        name = item.optString("name", "Entrada").ifBlank { "Entrada" },
                        sessionDate = item.optString("session_date").takeIf { it != "null" }.orEmpty(),
                        sessionTime = item.optString("session_time").takeIf { it != "null" }.orEmpty(),
                        sector = item.optString("sector").takeIf { it != "null" }.orEmpty(),
                        offer = item.optString("offer").takeIf { it != "null" }.orEmpty(),
                        price = nullableNumber(item, "price")?.roundToInt()?.coerceAtLeast(0) ?: 0,
                        capacity = item.optInt("capacity", -1).takeIf { it >= 0 },
                        sold = item.optInt("sold", 0).coerceAtLeast(0),
                        reserved = item.optInt("reserved", 0).coerceAtLeast(0),
                        maxPerOrder = item.optInt("max_per_order", 6).coerceIn(1, 20),
                        saleStartsAt = item.optString("sale_starts_at").takeIf { it != "null" }.orEmpty(),
                        saleEndsAt = item.optString("sale_ends_at").takeIf { it != "null" }.orEmpty(),
                        active = jsonBoolean(item, "active", true)
                    )
                )
            }
        }
        TicketOptionsInfo(
            capacity = root.optInt("capacity", -1).takeIf { it >= 0 },
            options = options
        )
    }

    fun configureTicketOptions(
        context: Context,
        concertId: Int,
        capacity: Int?,
        options: List<TicketOption>
    ): Result<TicketOptionsInfo> = runCatching {
        val savedToken = token(context) ?: error("La sessió de servidor no està disponible. Torna a iniciar sessió.")
        val array = JSONArray()
        options.forEach { option ->
            array.put(
                JSONObject()
                    .put("id", option.id.takeIf { it > 0 })
                    .put("name", option.name.trim())
                    .put("session_date", option.sessionDate.takeIf { it.isNotBlank() })
                    .put("session_time", option.sessionTime.takeIf { it.isNotBlank() })
                    .put("sector", option.sector.takeIf { it.isNotBlank() })
                    .put("offer", option.offer.takeIf { it.isNotBlank() })
                    .put("price", option.price)
                    .put("capacity", option.capacity)
                    .put("max_per_order", option.maxPerOrder)
                    .put("sale_starts_at", option.saleStartsAt.takeIf { it.isNotBlank() })
                    .put("sale_ends_at", option.saleEndsAt.takeIf { it.isNotBlank() })
                    .put("active", option.active)
            )
        }
        val payload = JSONObject()
            .put("event_id", concertId)
            .put("capacity", capacity)
            .put("options", array)
        jsonRequest("tickets/configure.php", "POST", payload, savedToken)
        fetchTicketOptions(context, concertId).getOrThrow()
    }

    fun purchaseTickets(context: Context, concertId: Int, quantity: Int, ticketOptionId: Long? = null): Result<List<PurchasedTicket>> = runCatching {
        require(quantity in 1..6) { "La quantitat ha d'estar entre 1 i 6." }
        val savedToken = token(context) ?: error("La sessió de servidor no està disponible. Torna a iniciar sessió.")
        val payload = JSONObject().put("event_id", concertId).put("quantity", quantity).also { if (ticketOptionId != null) it.put("ticket_option_id", ticketOptionId) }
        val response = jsonRequest("tickets/purchase.php", "POST", payload, savedToken)
        parseTickets(response.optJSONArray("tickets") ?: JSONArray())
    }

    fun createStripePayment(context: Context, concertId: Int, quantity: Int, ticketOptionId: Long? = null): Result<StripePaymentStart> = runCatching {
        require(quantity in 1..6) { "La quantitat ha d'estar entre 1 i 6." }
        val savedToken = token(context) ?: error("La sessió de servidor no està disponible. Torna a iniciar sessió.")
        val payload = JSONObject().put("event_id", concertId).put("quantity", quantity).also { if (ticketOptionId != null) it.put("ticket_option_id", ticketOptionId) }
        val response = jsonRequest("payments/create.php", "POST", payload, savedToken)
        StripePaymentStart(
            orderId = response.getLong("order_id"),
            clientSecret = response.getString("client_secret"),
            publishableKey = response.getString("publishable_key"),
            amountCents = response.optLong("amount_cents", 0L),
            currency = response.optString("currency", "eur"),
            merchantName = response.optString("merchant_name", "Agenda Valenciana"),
            mode = response.optString("mode", "test")
        )
    }

    fun fetchStripePaymentStatus(context: Context, orderId: Long): Result<StripePaymentStatus> = runCatching {
        val savedToken = token(context) ?: error("La sessió de servidor no està disponible. Torna a iniciar sessió.")
        val path = "payments/status.php?order_id=" + URLEncoder.encode(orderId.toString(), StandardCharsets.UTF_8.name())
        val connection = open(path, "GET", savedToken)
        val root = JSONObject(readResponse(connection))
        if (!root.optBoolean("ok", false)) error(root.optString("message", "No s'ha pogut comprovar el pagament."))
        StripePaymentStatus(
            orderId = root.optLong("order_id", orderId),
            status = root.optString("status", "payment_pending"),
            tickets = parseTickets(root.optJSONArray("tickets") ?: JSONArray())
        )
    }

    data class RemoteSpecialAccess(
        val canView: Boolean = false,
        val canEditEvent: Boolean = false,
        val canEditSchedule: Boolean = false,
        val canEditServices: Boolean = false,
        val canManageStaff: Boolean = false,
        val canViewAllOrders: Boolean = false,
        val roleName: String? = null
    )
    data class RemoteSpecialProduct(
        val id: Long,
        val serviceId: Long,
        val name: String,
        val description: String,
        val imageUrl: String?,
        val priceCents: Int,
        val reserveOnly: Boolean,
        val stock: Int?,
        val sortOrder: Int,
        val active: Boolean
    )
    data class RemoteSpecialService(
        val id: Long,
        val eventId: Int,
        val name: String,
        val description: String,
        val imageUrl: String?,
        val overlayColor: String,
        val sortOrder: Int,
        val active: Boolean,
        val products: List<RemoteSpecialProduct>
    )
    data class RemoteSpecialSchedule(
        val id: Long,
        val eventId: Int,
        val startAt: String,
        val endAt: String?,
        val title: String,
        val location: String,
        val details: String,
        val artistName: String?,
        val imageUrl: String?,
        val sortOrder: Int,
        val active: Boolean,
        val artistId: Long? = null,
        val scheduleDate: String = "",
        val startTime: String? = null,
        val endTime: String? = null,
        val stage: String = "",
        val groupName: String = "",
        val itemType: String = "performance"
    )
    data class RemoteSpecialEvent(
        val eventId: Int,
        val displayTitle: String,
        val enabled: Boolean,
        val heroImageUrl: String?,
        val generalInfo: String,
        val startAt: String?,
        val venueLabel: String,
        val communityEnabled: Boolean,
        val schedule: List<RemoteSpecialSchedule>,
        val services: List<RemoteSpecialService>,
        val access: RemoteSpecialAccess = RemoteSpecialAccess(),
        val eventStructure: String = "AUTO",
        val scheduleStatus: String = "NONE"
    )
    data class RemoteSpecialRoleService(
        val serviceId: Long,
        val canViewOrders: Boolean,
        val canRedeem: Boolean,
        val canManageProducts: Boolean
    )
    data class RemoteSpecialRole(
        val id: Long,
        val eventId: Int,
        val name: String,
        val canEditEvent: Boolean,
        val canEditSchedule: Boolean,
        val canEditServices: Boolean,
        val canManageStaff: Boolean,
        val canViewAllOrders: Boolean,
        val services: List<RemoteSpecialRoleService>
    )
    data class RemoteSpecialStaff(
        val id: Long,
        val userId: Long,
        val username: String,
        val email: String?,
        val roleId: Long,
        val roleName: String,
        val active: Boolean
    )
    data class RemoteSpecialEventSummary(
        val eventId: Int,
        val displayTitle: String,
        val enabled: Boolean,
        val startAt: String?,
        val venueLabel: String
    )
    data class RemoteSpecialManageConfig(
        val event: RemoteSpecialEvent,
        val roles: List<RemoteSpecialRole>,
        val staff: List<RemoteSpecialStaff>
    )

    private fun parseSpecialEvent(root: JSONObject): RemoteSpecialEvent {
        val e = root.optJSONObject("event") ?: JSONObject()
        val scheduleArray = root.optJSONArray("schedule") ?: JSONArray()
        val schedule = buildList {
            for (i in 0 until scheduleArray.length()) {
                val s = scheduleArray.optJSONObject(i) ?: continue
                val scheduleDate = sequenceOf(s.optString("date"), s.optString("schedule_date"), s.optString("start_at"))
                    .map(String::trim).firstOrNull { it.isNotBlank() && it != "null" }?.take(10).orEmpty()
                val startTime = s.optString("start_time").takeIf { it.isNotBlank() && it != "null" }
                    ?: s.optString("start_at").substringAfter(' ', "").takeIf(String::isNotBlank)?.take(8)
                val endTime = s.optString("end_time").takeIf { it.isNotBlank() && it != "null" }
                    ?: s.optString("end_at").substringAfter(' ', "").takeIf(String::isNotBlank)?.take(8)
                val startAt = s.optString("start_at").takeIf { it.isNotBlank() && it != "null" }
                    ?: listOfNotNull(scheduleDate.takeIf(String::isNotBlank), startTime).joinToString(" ")
                val endAt = s.optString("end_at").takeIf { it.isNotBlank() && it != "null" }
                    ?: if (scheduleDate.isNotBlank() && !endTime.isNullOrBlank()) "$scheduleDate $endTime" else null
                val stage = s.optString("stage").takeIf { it.isNotBlank() && it != "null" } ?: s.optString("location")
                add(RemoteSpecialSchedule(
                    id=s.optLong("id"), eventId=s.optInt("event_id"), startAt=startAt,
                    endAt=endAt, title=s.optString("title"), location=stage, details=s.optString("details"),
                    artistName=s.optString("artist_name").takeIf(String::isNotBlank),
                    imageUrl=resolvePublicUrl(s.optString("image_url")).takeIf(String::isNotBlank), sortOrder=s.optInt("sort_order"), active=s.optInt("active",1)!=0,
                    artistId=s.optLong("artist_id").takeIf { it > 0 }, scheduleDate=scheduleDate, startTime=startTime, endTime=endTime, stage=stage,
                    groupName=s.optString("group_name").takeIf { it != "null" }.orEmpty(),
                    itemType=s.optString("item_type", "performance").takeIf { it != "null" }.orEmpty().ifBlank { "performance" }
                ))
            }
        }
        val servicesArray = root.optJSONArray("services") ?: JSONArray()
        val services = buildList {
            for (i in 0 until servicesArray.length()) {
                val s=servicesArray.optJSONObject(i) ?: continue
                val pa=s.optJSONArray("products") ?: JSONArray()
                val products=buildList {
                    for(j in 0 until pa.length()){
                        val p=pa.optJSONObject(j)?:continue
                        add(RemoteSpecialProduct(
                            id=p.optLong("id"),serviceId=p.optLong("service_id"),name=p.optString("name"),description=p.optString("description"),
                            imageUrl=resolvePublicUrl(p.optString("image_url")).takeIf(String::isNotBlank),priceCents=p.optInt("price_cents"),reserveOnly=p.optInt("reserve_only")!=0,
                            stock=if(p.isNull("stock")) null else p.optInt("stock"),sortOrder=p.optInt("sort_order"),active=p.optInt("active",1)!=0
                        ))
                    }
                }
                add(RemoteSpecialService(id=s.optLong("id"),eventId=s.optInt("event_id"),name=s.optString("name"),description=s.optString("description"),imageUrl=resolvePublicUrl(s.optString("image_url")).takeIf(String::isNotBlank),overlayColor=s.optString("overlay_color","#FFE082"),sortOrder=s.optInt("sort_order"),active=s.optInt("active",1)!=0,products=products))
            }
        }
        val a=root.optJSONObject("access") ?: JSONObject()
        return RemoteSpecialEvent(
            eventId=e.optInt("event_id"),displayTitle=e.optString("display_title"),enabled=e.optInt("enabled",1)!=0,
            heroImageUrl=resolvePublicUrl(e.optString("hero_image_url")).takeIf(String::isNotBlank),generalInfo=e.optString("general_info"),startAt=e.optString("start_at").takeIf(String::isNotBlank),
            venueLabel=e.optString("venue_label"),communityEnabled=e.optInt("community_enabled",1)!=0,schedule=schedule,services=services,
            access=RemoteSpecialAccess(a.optBoolean("can_view"),a.optBoolean("can_edit_event"),a.optBoolean("can_edit_schedule"),a.optBoolean("can_edit_services"),a.optBoolean("can_manage_staff"),a.optBoolean("can_view_all_orders"),a.optString("role_name").takeIf(String::isNotBlank)),
            eventStructure=e.optString("event_structure", "AUTO").trim().ifBlank { "AUTO" }.uppercase(),
            scheduleStatus=e.optString("schedule_status", "NONE").trim().ifBlank { "NONE" }.uppercase()
        )
    }

    fun fetchSpecialEvent(context: Context, eventId: Int): Result<RemoteSpecialEvent> = runCatching {
        val path="special-events/public.php?event_id="+URLEncoder.encode(eventId.toString(),StandardCharsets.UTF_8.name())
        val connection=open(path,"GET",token(context)); val root=JSONObject(readResponse(connection)); if(!root.optBoolean("ok",false)) error(root.optString("message","No s'ha pogut carregar l'espai especial.")); parseSpecialEvent(root)
    }

    fun fetchMySpecialEvents(context: Context): Result<List<RemoteSpecialEventSummary>> = runCatching {
        val savedToken=token(context) ?: error("Cal iniciar sessió.")
        val connection=open("special-events/manage/my-events.php","GET",savedToken)
        val root=JSONObject(readResponse(connection)); if(!root.optBoolean("ok",false)) error(root.optString("message","No s'han pogut carregar els accessos."))
        val arr=root.optJSONArray("events")?:JSONArray()
        buildList { for(i in 0 until arr.length()){ val e=arr.optJSONObject(i)?:continue; add(RemoteSpecialEventSummary(e.optInt("event_id"),e.optString("display_title"),e.optInt("enabled",1)!=0,e.optString("start_at").takeIf(String::isNotBlank),e.optString("venue_label"))) } }
    }

    fun fetchSpecialEventManage(context: Context, eventId: Int): Result<RemoteSpecialManageConfig> = runCatching {
        val savedToken=token(context) ?: error("Cal iniciar sessió.")
        val path="special-events/manage/config.php?event_id="+URLEncoder.encode(eventId.toString(),StandardCharsets.UTF_8.name())
        val connection=open(path,"GET",savedToken); val root=JSONObject(readResponse(connection)); if(!root.optBoolean("ok",false)) error(root.optString("message","No s'ha pogut carregar la gestió."))
        val event=parseSpecialEvent(root)
        val ra=root.optJSONArray("roles")?:JSONArray(); val roles=buildList { for(i in 0 until ra.length()){ val r=ra.optJSONObject(i)?:continue; val rsa=r.optJSONArray("services")?:JSONArray(); val rs=buildList { for(j in 0 until rsa.length()){ val x=rsa.optJSONObject(j)?:continue; add(RemoteSpecialRoleService(x.optLong("service_id"),x.optInt("can_view_orders")!=0,x.optInt("can_redeem")!=0,x.optInt("can_manage_products")!=0)) } }; add(RemoteSpecialRole(r.optLong("id"),r.optInt("event_id"),r.optString("name"),r.optInt("can_edit_event")!=0,r.optInt("can_edit_schedule")!=0,r.optInt("can_edit_services")!=0,r.optInt("can_manage_staff")!=0,r.optInt("can_view_all_orders")!=0,rs)) } }
        val sa=root.optJSONArray("staff")?:JSONArray(); val staff=buildList { for(i in 0 until sa.length()){ val s=sa.optJSONObject(i)?:continue; add(RemoteSpecialStaff(s.optLong("id"),s.optLong("user_id"),s.optString("username"),s.optString("email").takeIf(String::isNotBlank),s.optLong("role_id"),s.optString("role_name"),s.optInt("active",1)!=0)) } }
        RemoteSpecialManageConfig(event,roles,staff)
    }

    private fun specialManagePost(context: Context,path:String,payload:JSONObject): Result<JSONObject> = runCatching { val savedToken=token(context)?:error("Cal iniciar sessió."); jsonRequest("special-events/manage/$path","POST",payload,savedToken) }
    fun saveSpecialEvent(context: Context,eventId:Int,displayTitle:String,heroImageUrl:String,generalInfo:String,startAt:String?,venueLabel:String,enabled:Boolean=true,communityEnabled:Boolean=true)=specialManagePost(context,"save-event.php",JSONObject().put("event_id",eventId).put("display_title",displayTitle).put("hero_image_url",heroImageUrl).put("general_info",generalInfo).put("start_at",startAt).put("venue_label",venueLabel).put("enabled",enabled).put("community_enabled",communityEnabled))
    fun saveSpecialSchedule(context:Context,eventId:Int,id:Long,title:String,startAt:String,endAt:String?,location:String,details:String,artistName:String?,imageUrl:String?,sortOrder:Int=0,artistId:Long?=null): Result<JSONObject> {
        val cleanStart = startAt.trim()
        val date = cleanStart.take(10).takeIf { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }.orEmpty()
        val startTime = cleanStart.substringAfter(' ', "").trim().takeIf(String::isNotBlank)
        val cleanEnd = endAt?.trim().takeIf { !it.isNullOrBlank() }
        val endTime = cleanEnd?.substringAfter(' ', "")?.trim()?.takeIf(String::isNotBlank)
            ?: cleanEnd?.takeIf { it.matches(Regex("\\d{1,2}:\\d{2}(:\\d{2})?")) }
        val payload = JSONObject().put("event_id",eventId).put("id",id).put("title",title)
            .put("date",date).put("schedule_date",date).put("start_time",startTime).put("end_time",endTime)
            .put("stage",location).put("location",location).put("details",details).put("artist_name",artistName)
            .put("artist_id",artistId).put("image_url",imageUrl).put("sort_order",sortOrder).put("active",true)
        // Compatibilitat amb l'endpoint anterior mentre es desplega el nou esquema.
        payload.put("start_at", cleanStart).put("end_at", cleanEnd)
        return specialManagePost(context,"save-schedule.php",payload)
    }
    fun deleteSpecialSchedule(context:Context,eventId:Int,id:Long)=specialManagePost(context,"delete-schedule.php",JSONObject().put("event_id",eventId).put("id",id))
    fun saveSpecialService(context:Context,eventId:Int,id:Long,name:String,description:String,imageUrl:String,overlayColor:String,sortOrder:Int=0)=specialManagePost(context,"save-service.php",JSONObject().put("event_id",eventId).put("id",id).put("name",name).put("description",description).put("image_url",imageUrl).put("overlay_color",overlayColor).put("sort_order",sortOrder).put("active",true))
    fun deleteSpecialService(context:Context,eventId:Int,id:Long)=specialManagePost(context,"delete-service.php",JSONObject().put("event_id",eventId).put("id",id))
    fun saveSpecialProduct(context:Context,eventId:Int,serviceId:Long,id:Long,name:String,description:String,imageUrl:String,priceCents:Int,reserveOnly:Boolean,stock:Int?,sortOrder:Int=0)=specialManagePost(context,"save-product.php",JSONObject().put("event_id",eventId).put("service_id",serviceId).put("id",id).put("name",name).put("description",description).put("image_url",imageUrl).put("price_cents",priceCents).put("reserve_only",reserveOnly).put("stock",stock).put("sort_order",sortOrder).put("active",true))
    fun deleteSpecialProduct(context:Context,eventId:Int,serviceId:Long,id:Long)=specialManagePost(context,"delete-product.php",JSONObject().put("event_id",eventId).put("service_id",serviceId).put("id",id))
    fun saveSpecialRole(context:Context,eventId:Int,id:Long,name:String,editEvent:Boolean,editSchedule:Boolean,editServices:Boolean,manageStaff:Boolean,viewAllOrders:Boolean,services:List<RemoteSpecialRoleService>):Result<JSONObject>{ val arr=JSONArray();services.forEach{arr.put(JSONObject().put("service_id",it.serviceId).put("can_view_orders",it.canViewOrders).put("can_redeem",it.canRedeem).put("can_manage_products",it.canManageProducts))};return specialManagePost(context,"save-role.php",JSONObject().put("event_id",eventId).put("id",id).put("name",name).put("can_edit_event",editEvent).put("can_edit_schedule",editSchedule).put("can_edit_services",editServices).put("can_manage_staff",manageStaff).put("can_view_all_orders",viewAllOrders).put("services",arr)) }
    fun assignSpecialStaff(context:Context,eventId:Int,username:String,roleId:Long)=specialManagePost(context,"assign-staff.php",JSONObject().put("event_id",eventId).put("username",username).put("role_id",roleId))
    fun removeSpecialStaff(context:Context,eventId:Int,staffId:Long)=specialManagePost(context,"remove-staff.php",JSONObject().put("event_id",eventId).put("staff_id",staffId))

    fun createSpecialServicePayment(
        context: Context,
        concertId: Int,
        serviceId: Long,
        serviceName: String,
        items: List<Triple<Long, String, Int>>
    ): Result<SpecialServicePaymentStart> = runCatching {
        require(items.isNotEmpty()) { "La comanda està buida." }
        require(serviceId > 0L || serviceName.isNotBlank()) { "Servei no vàlid." }
        val savedToken = token(context) ?: error("La sessió de servidor no està disponible. Torna a iniciar sessió.")
        val itemArray = JSONArray()
        items.forEach { (productId, productName, quantity) ->
            require(productId > 0L || productName.isNotBlank()) { "Producte no vàlid." }
            require(quantity in 1..99) { "Quantitat no vàlida." }
            itemArray.put(JSONObject().put("product_id", productId).put("product_name", productName).put("quantity", quantity))
        }
        val payload = JSONObject()
            .put("event_id", concertId)
            .put("service_id", serviceId)
            .put("service_name", serviceName)
            .put("items", itemArray)
        val response = jsonRequest("payments/service-create.php", "POST", payload, savedToken)
        SpecialServicePaymentStart(
            orderId = response.getLong("order_id"),
            clientSecret = response.getString("client_secret"),
            publishableKey = response.getString("publishable_key"),
            amountCents = response.optLong("amount_cents", 0L),
            currency = response.optString("currency", "eur"),
            merchantName = response.optString("merchant_name", "l'Agenda"),
            mode = response.optString("mode", "test")
        )
    }

    fun fetchSpecialServicePaymentStatus(
        context: Context,
        orderId: Long
    ): Result<SpecialServicePaidOrder> = runCatching {
        val savedToken = token(context) ?: error("La sessió de servidor no està disponible. Torna a iniciar sessió.")
        val path = "payments/service-status.php?order_id=" + URLEncoder.encode(orderId.toString(), StandardCharsets.UTF_8.name())
        val connection = open(path, "GET", savedToken)
        val root = JSONObject(readResponse(connection))
        if (!root.optBoolean("ok", false)) error(root.optString("message", "No s'ha pogut verificar la compra."))
        val itemArray = root.optJSONArray("items") ?: JSONArray()
        val items = buildList {
            for (i in 0 until itemArray.length()) {
                val item = itemArray.optJSONObject(i) ?: continue
                add(SpecialServicePaidItem(
                    productName = item.optString("product_name"),
                    quantity = item.optInt("quantity", 1),
                    unitPriceCents = item.optLong("unit_price_cents", 0L)
                ))
            }
        }
        SpecialServicePaidOrder(
            orderId = root.optLong("order_id", orderId),
            serviceName = root.optString("service_name"),
            status = root.optString("status", "payment_pending"),
            totalCents = root.optLong("total_cents", 0L),
            qrPayload = root.optString("qr_payload"),
            items = items
        )
    }

    fun inspectSpecialServiceOrder(context: Context, qrToken: String): Result<SpecialServiceInspection> = runCatching {
        val savedToken = token(context) ?: error("La sessió de servidor no està disponible.")
        val response = jsonRequest(
            "special-events/order-inspect.php",
            "POST",
            JSONObject().put("qr_token", qrToken),
            savedToken
        )
        val arr = response.optJSONArray("items") ?: JSONArray()
        val items = buildList {
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                add(SpecialServicePaidItem(
                    productName = item.optString("product_name"),
                    quantity = item.optInt("quantity", 1),
                    unitPriceCents = item.optLong("unit_price_cents", 0L)
                ))
            }
        }
        SpecialServiceInspection(
            orderId = response.optLong("order_id"),
            eventId = response.optInt("event_id"),
            serviceName = response.optString("service_name"),
            status = response.optString("status"),
            totalCents = response.optLong("total_cents"),
            items = items
        )
    }

    fun redeemSpecialServiceOrder(context: Context, qrToken: String): Result<String> = runCatching {
        val savedToken = token(context) ?: error("La sessió de servidor no està disponible.")
        val response = jsonRequest(
            "special-events/order-redeem.php",
            "POST",
            JSONObject().put("qr_token", qrToken),
            savedToken
        )
        response.optString("status", "redeemed")
    }

    fun createAdvertisementPayment(
        context: Context,
        concertId: Int,
        placement: AdPlacement,
        durationDays: Int,
        targetStyles: Set<String>,
        targetSearchTerms: Set<String>,
        targetTowns: Set<String>,
        targetComarques: Set<String>,
        targetFollowers: Boolean,
        targetHighIntent: Boolean,
        matchAllCriteria: Boolean
    ): Result<AdPaymentStart> = runCatching {
        require(durationDays in setOf(1, 3, 7)) { "La duració ha de ser 1, 3 o 7 dies." }
        val savedToken = token(context) ?: error("La sessió de servidor no està disponible. Torna a iniciar sessió.")
        val payload = JSONObject()
            .put("event_id", concertId)
            .put("guest_id", guestId(context))
            .put("placement", placement.name)
            .put("duration_days", durationDays)
            .put("target_styles", JSONArray(targetStyles.toList()))
            .put("target_search_terms", JSONArray(targetSearchTerms.toList()))
            .put("target_towns", JSONArray(targetTowns.toList()))
            .put("target_comarques", JSONArray(targetComarques.toList()))
            .put("target_followers", targetFollowers)
            .put("target_high_intent", targetHighIntent)
            .put("match_mode", if (matchAllCriteria) "ALL" else "ANY")

        val response = jsonRequest("ads/create-payment.php", "POST", payload, savedToken)
        AdPaymentStart(
            adId = response.getLong("ad_id"),
            clientSecret = response.getString("client_secret"),
            publishableKey = response.getString("publishable_key"),
            amountCents = response.optLong("amount_cents", 0L),
            currency = response.optString("currency", "eur"),
            merchantName = response.optString("merchant_name", "Agenda Valenciana"),
            mode = response.optString("mode", "test")
        )
    }

    fun fetchAdvertisementPaymentStatus(context: Context, adId: Long): Result<AdPaymentStatus> = runCatching {
        val savedToken = token(context) ?: error("La sessió de servidor no està disponible. Torna a iniciar sessió.")
        val path = "ads/status.php?ad_id=" + URLEncoder.encode(adId.toString(), StandardCharsets.UTF_8.name())
        val connection = open(path, "GET", savedToken)
        val root = JSONObject(readResponse(connection))
        if (!root.optBoolean("ok", false)) error(root.optString("message", "No s'ha pogut comprovar el pagament de l'anunci."))
        AdPaymentStatus(
            adId = root.optLong("ad_id", adId),
            status = root.optString("status", "payment_pending"),
            advertisement = root.optJSONObject("ad")?.let(::parseAdvertisement)
        )
    }

    private data class AdvertisementVisual(val x: Float, val y: Float, val imageUrl: String?)

    private fun fetchAdvertisementVisualMap(context: Context): Map<Int, AdvertisementVisual> = runCatching {
        val connection = open("ads/visual-map.php", "GET", token(context))
        val root = JSONObject(readResponse(connection))
        if (!root.optBoolean("ok", false)) return@runCatching emptyMap()
        val array = root.optJSONArray("visuals") ?: JSONArray()
        buildMap {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val id = item.optInt("ad_id")
                if (id <= 0) continue
                put(id, AdvertisementVisual(
                    x = item.optDouble("card_focus_x", .5).toFloat().coerceIn(0f, 1f),
                    y = item.optDouble("card_focus_y", .5).toFloat().coerceIn(0f, 1f),
                    imageUrl = resolvePublicUrl(item.optString("image_url")).takeIf(String::isNotBlank)
                ))
            }
        }
    }.getOrDefault(emptyMap())

    private fun mergeAdvertisementVisuals(
        ads: List<EventAdvertisement>,
        visuals: Map<Int, AdvertisementVisual>
    ): List<EventAdvertisement> = ads.map { ad ->
        visuals[ad.id]?.let { visual -> ad.copy(cardFocusX = visual.x, cardFocusY = visual.y, imageUri = visual.imageUrl ?: ad.imageUri) } ?: ad
    }

    data class ActiveAdvertisementSnapshot(
        val advertisements: List<EventAdvertisement>,
        val concerts: List<Concert>,
        val priorityConcerts: List<Concert>,
        val priorityEventIds: Set<Int>
    )

    fun fetchActiveAdvertisements(context: Context): Result<ActiveAdvertisementSnapshot> = runCatching {
        // v249: lectura pública independent de la sessió i les preferències.
        // El recurs anterior es manté com a fallback fins que es desplegue el nou PHP.
        val root = runCatching {
            val connection = open("ads/active-global.php", "GET", null)
            JSONObject(readResponse(connection)).also {
                if (!it.optBoolean("ok", false)) error(it.optString("message", "Anuncis globals no disponibles"))
            }
        }.getOrElse {
            val connection = open("ads/active.php", "GET", token(context))
            JSONObject(readResponse(connection)).also {
                if (!it.optBoolean("ok", false)) error(it.optString("message", "No s'han pogut carregar els anuncis."))
            }
        }
        val ads = mergeAdvertisementVisuals(
            parseAdvertisements(root.optJSONArray("ads") ?: JSONArray()),
            fetchAdvertisementVisualMap(context)
        )
        // La consulta global també inclou la fitxa de cada concert anunciat:
        // no ha de dependre de la cache ni del radi territorial de «Per a tu».
        val featuredEvents = root.optJSONArray("events") ?: JSONArray()
        val concerts = if (featuredEvents.length() > 0) {
            runCatching { parseEvents(featuredEvents, context, loadCachedEntityImages(context), emptyList()) }
                .getOrDefault(emptyList())
        } else emptyList()
        // v256: endpoint global: fitxes amb publicitat activa o venda INTERNA
        // habilitada, encara que ningú haja comprat entrades. Les compres
        // personals i les preferències no intervenen. El fallback antic no
        // pot garantir esdeveniments que encara no s'hagen carregat localment.
        val priorityArray = root.optJSONArray("priority_events") ?: JSONArray()
        val priorityConcerts = if (priorityArray.length() > 0) {
            runCatching { parseEvents(priorityArray, context, loadCachedEntityImages(context), emptyList()) }
                .getOrDefault(emptyList())
        } else concerts
        // v256: llegim IDs de prioritat comercial de totes les tipologies.
        // La compatibilitat amb el servidor v250 es conserva, però per garantir
        // TOTES les vendes pròpies és necessari desplegar active-global.php v256.
        val priorityIds = buildSet {
            listOf("priority_event_ids", "platform_ticket_event_ids", "paid_ticket_event_ids").forEach { key ->
                val ids = root.optJSONArray(key) ?: JSONArray()
                for (i in 0 until ids.length()) ids.optInt(i).takeIf { it > 0 }?.let(::add)
            }
            priorityConcerts.map { it.id }.filter { it > 0 }.forEach(::add)
        }
        ActiveAdvertisementSnapshot(ads, concerts, priorityConcerts, priorityIds)
    }

    fun fetchMyAdvertisements(context: Context): Result<List<EventAdvertisement>> = runCatching {
        val savedToken = token(context) ?: error("La sessió de servidor no està disponible. Torna a iniciar sessió.")
        val connection = open("ads/mine.php", "GET", savedToken)
        val root = JSONObject(readResponse(connection))
        if (!root.optBoolean("ok", false)) error(root.optString("message", "No s'han pogut carregar els anuncis."))
        mergeAdvertisementVisuals(
            parseAdvertisements(root.optJSONArray("ads") ?: JSONArray()),
            fetchAdvertisementVisualMap(context)
        )
    }

    fun updateAdvertisement(
        context: Context,
        adId: Int,
        targetStyles: Set<String>,
        targetSearchTerms: Set<String>,
        targetTowns: Set<String>,
        targetComarques: Set<String>,
        targetFollowers: Boolean,
        targetHighIntent: Boolean,
        matchAllCriteria: Boolean
    ): Result<EventAdvertisement> = runCatching {
        val savedToken = token(context) ?: error("La sessió de servidor no està disponible. Torna a iniciar sessió.")
        val payload = JSONObject()
            .put("ad_id", adId)
            .put("target_styles", JSONArray(targetStyles.toList()))
            .put("target_search_terms", JSONArray(targetSearchTerms.toList()))
            .put("target_towns", JSONArray(targetTowns.toList()))
            .put("target_comarques", JSONArray(targetComarques.toList()))
            .put("target_followers", targetFollowers)
            .put("target_high_intent", targetHighIntent)
            .put("match_mode", if (matchAllCriteria) "ALL" else "ANY")
        val response = jsonRequest("ads/update.php", "POST", payload, savedToken)
        parseAdvertisement(response.getJSONObject("ad"))
    }

    fun updateAdvertisementVisual(
        context: Context,
        adId: Long,
        cardFocusX: Float,
        cardFocusY: Float,
        imageUri: String? = null,
        removeImage: Boolean = false,
        onUploadProgress: (Int) -> Unit = {}
    ): Result<EventAdvertisement?> = runCatching {
        val savedToken = token(context) ?: error("La sessió de servidor no està disponible. Torna a iniciar sessió.")
        imageUri?.takeIf { it.isNotBlank() && !it.startsWith("http://") && !it.startsWith("https://") }?.let { rawUri ->
            uploadImageForOwner(context, "AD", adId.toString(), prepareCardImageUpload(context, rawUri), onUploadProgress)
        }
        val payload = JSONObject()
            .put("ad_id", adId)
            .put("card_focus_x", cardFocusX.coerceIn(0f, 1f).toDouble())
            .put("card_focus_y", cardFocusY.coerceIn(0f, 1f).toDouble())
            .put("remove_image", removeImage)
        val response = jsonRequest("ads/visual.php", "POST", payload, savedToken)
        response.optJSONObject("ad")?.let(::parseAdvertisement)
    }

    fun trackAdvertisement(context: Context, adId: Int, placement: AdPlacement, click: Boolean) {
        val payload = JSONObject()
            .put("ad_id", adId)
            .put("placement", placement.name)
            .put("action", if (click) "click" else "impression")
        runCatching {
            jsonRequest("ads/track.php", "POST", payload, token(context))
        }
    }

    fun fetchBatecsPage(context: Context, eventId: Int? = null, offset: Int = 0, limit: Int = 10): Result<BatecPage> = runCatching {
        val cleanOffset = offset.coerceAtLeast(0)
        val cleanLimit = limit.coerceIn(1, 50)
        val query = mutableListOf("offset=$cleanOffset", "limit=$cleanLimit")
        if (eventId != null && eventId > 0) query += "event_id=" + URLEncoder.encode(eventId.toString(), StandardCharsets.UTF_8.name())
        val connection = open("batecs/index.php?" + query.joinToString("&"), "GET", token(context))
        val root = JSONObject(readResponse(connection))
        if (!root.optBoolean("ok", false)) error(root.optString("message", "No s'han pogut carregar els batecs."))
        if (eventId == null && cleanOffset == 0) {
            File(File(context.filesDir, CACHE_DIR).apply { mkdirs() }, BATECS_CACHE)
                .writeText(root.toString(), StandardCharsets.UTF_8)
        }
        val items = parseStories(root.optJSONArray("batecs") ?: JSONArray())
        BatecPage(
            items = items,
            hasMore = root.optBoolean("has_more", false),
            nextOffset = root.optInt("next_offset", cleanOffset + items.size)
        )
    }

    fun fetchBatecs(context: Context, eventId: Int? = null): Result<List<Story>> =
        fetchBatecsPage(context, eventId, 0, if (eventId == null) 10 else 50).map { it.items }

    fun loadCachedBatecs(context: Context): List<Story> {
        val file = File(File(context.filesDir, CACHE_DIR), BATECS_CACHE)
        if (!file.exists()) return emptyList()
        return runCatching {
            val root = JSONObject(file.readText(StandardCharsets.UTF_8))
            parseStories(root.optJSONArray("batecs") ?: JSONArray())
        }.getOrDefault(emptyList())
    }

    fun fetchRecentBatecEventCreations(context: Context): Result<Map<Int, Long>> = runCatching {
        val savedToken = token(context) ?: return@runCatching emptyMap()
        val connection = open("batecs/recent-events.php", "GET", savedToken)
        val root = JSONObject(readResponse(connection))
        if (!root.optBoolean("ok", false)) error(root.optString("message", "No s'han pogut carregar les novetats de Batecs."))
        val array = root.optJSONArray("events") ?: JSONArray()
        buildMap {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val eventId = item.optInt("event_id")
                val createdAt = parseServerTimestamp(item.optString("created_at")) ?: continue
                if (eventId > 0) put(eventId, createdAt)
            }
        }
    }

    fun fetchSeenBatecKeys(context: Context): Result<Set<String>> = runCatching {
        val savedToken = token(context) ?: return@runCatching emptySet()
        val connection = open("batecs/seen.php", "GET", savedToken)
        val root = JSONObject(readResponse(connection))
        if (!root.optBoolean("ok", false)) error(root.optString("message", "No s'ha pogut carregar l'estat dels batecs."))
        val array = root.optJSONArray("seen_keys") ?: JSONArray()
        buildSet {
            for (i in 0 until array.length()) {
                array.optString(i).trim().takeIf(String::isNotBlank)?.let(::add)
            }
        }
    }

    fun markBatecSeen(context: Context, storyKey: String): Result<Unit> = runCatching {
        val savedToken = token(context) ?: return@runCatching
        val key = storyKey.trim()
        if (key.isBlank()) return@runCatching
        jsonRequest("batecs/seen.php", "POST", JSONObject().put("story_key", key), savedToken)
        Unit
    }

    fun fetchBatecEligibility(context: Context, eventId: Int): Result<BatecEligibility> = runCatching {
        val savedToken = token(context) ?: error("Cal iniciar sessió.")
        val path = "batecs/eligibility.php?event_id=" +
            URLEncoder.encode(eventId.toString(), StandardCharsets.UTF_8.name())
        val connection = open(path, "GET", savedToken)
        val root = JSONObject(readResponse(connection))
        if (!root.optBoolean("ok", false)) error(root.optString("message", "No s'ha pogut comprovar si pots crear el batec."))
        BatecEligibility(
            allowed = root.optBoolean("allowed", false),
            organizer = root.optBoolean("organizer", false),
            reason = root.optString("reason").takeIf { it.isNotBlank() && it != "null" },
            opensAt = root.optString("opens_at").takeIf { it.isNotBlank() && it != "null" },
            closesAt = root.optString("closes_at").takeIf { it.isNotBlank() && it != "null" }
        )
    }

    fun createBatec(
        context: Context,
        eventId: Int,
        title: String,
        body: String
    ): Result<Story> = runCatching {
        val savedToken = token(context) ?: error("Cal iniciar sessió.")
        val payload = JSONObject()
            .put("event_id", eventId)
            .put("title", title.trim())
            .put("body", body.trim())
        val root = jsonRequest("batecs/create.php", "POST", payload, savedToken)
        parseStory(root.getJSONObject("batec"))
    }

    fun updateBatec(
        context: Context,
        id: Int,
        title: String,
        body: String
    ): Result<Story> = runCatching {
        val savedToken = token(context) ?: error("Cal iniciar sessió.")
        val payload = JSONObject()
            .put("id", id)
            .put("title", title.trim())
            .put("body", body.trim())
        val root = jsonRequest("batecs/update.php", "POST", payload, savedToken)
        parseStory(root.getJSONObject("batec"))
    }

    fun deleteBatec(context: Context, id: Int): Result<Unit> = runCatching {
        val savedToken = token(context) ?: error("Cal iniciar sessió.")
        val payload = JSONObject().put("id", id)
        jsonRequest("batecs/delete.php", "POST", payload, savedToken)
        Unit
    }

    fun fetchCommunityGroups(
        context: Context,
        eventId: Int? = null,
        mine: Boolean = false
    ): Result<List<CommunityGroup>> = runCatching {
        val savedToken = token(context) ?: error("Cal iniciar sessió.")
        val params = buildList {
            eventId?.takeIf { it > 0 }?.let { add("event_id=" + URLEncoder.encode(it.toString(), StandardCharsets.UTF_8.name())) }
            if (mine) add("mine=1")
        }
        val path = "community/groups.php" + if (params.isEmpty()) "" else "?" + params.joinToString("&")
        val connection = open(path, "GET", savedToken)
        val root = JSONObject(readResponse(connection))
        if (!root.optBoolean("ok", false)) error(root.optString("message", "No s'ha pogut carregar la comunitat."))
        val mainGroups = parseCommunityGroups(root.optJSONArray("groups") ?: JSONArray())
        val contactGroups = if (mine) {
            getAuthenticatedJson(context,"community/board-settings.php?contacted=1").getOrNull()
                ?.optJSONArray("groups")?.let(::parseCommunityGroups).orEmpty()
        } else emptyList()
        val groups = (mainGroups + contactGroups).distinctBy { it.id }
        if (groups.isEmpty()) groups else {
            // La consulta és única per al lot, no una petició per cada tauló.
            val ids = groups.joinToString(",") { it.id.toString() }
            val details = getAuthenticatedJson(context, "community/board-settings.php?group_ids=$ids").getOrNull()
                ?.optJSONArray("boards")
            val settings = mutableMapOf<Int, Pair<CommunityGroupType, Boolean>>()
            if (details != null) for (i in 0 until details.length()) {
                val item = details.optJSONObject(i) ?: continue
                val boardType = runCatching { CommunityGroupType.valueOf(item.optString("board_type")) }.getOrNull()
                    ?: continue
                settings[item.optInt("group_id")] = boardType to item.optBoolean("chat_enabled", true)
            }
            groups.map { group ->
                settings[group.id]?.let { (type, chat) -> group.copy(type = type, chatEnabled = chat) } ?: group
            }
        }
    }

    fun createCommunityGroup(
        context: Context,
        eventId: Int,
        type: CommunityGroupType,
        accessMode: CommunityAccessMode,
        title: String,
        description: String,
        originLabel: String,
        meetingAt: String?,
        capacity: Int?,
        chatEnabled: Boolean = true
    ): Result<CommunityGroup> = runCatching {
        // Abans de crear al servidor antic, assegurem que està desplegat el sistema de taulons.
        getAuthenticatedJson(context, "community/board-settings.php?group_ids=1").getOrThrow()
        val savedToken = token(context) ?: error("Cal iniciar sessió.")
        val serverType = when (type) {
            CommunityGroupType.CAR -> "CAR"
            CommunityGroupType.TRANSPORT, CommunityGroupType.TRAIN -> "TRANSPORT"
            CommunityGroupType.MEETUP -> "MEETUP"
            else -> "OTHER"
        }
        val payload = JSONObject()
            .put("event_id", eventId)
            .put("type", serverType)
            .put("access_mode", accessMode.name)
            .put("title", title.trim())
            .put("description", description.trim())
            .put("origin_label", originLabel.trim())
        meetingAt?.takeIf { it.isNotBlank() }?.let { payload.put("meeting_at", it) }
        capacity?.takeIf { it > 0 }?.let { payload.put("capacity", it) }
        val root = jsonRequest("community/create.php", "POST", payload, savedToken)
        val group = parseCommunityGroup(root.getJSONObject("group"))
        // Error explícit si el servidor encara no té instal·lat el patch de taulons.
        postAuthenticatedJson(context, "community/board-settings.php", JSONObject()
            .put("group_id", group.id)
            .put("board_type", type.name)
            .put("chat_enabled", chatEnabled)).getOrThrow()
        group.copy(type = type, chatEnabled = chatEnabled)
    }

    fun updateCommunityGroup(
        context: Context,
        groupId: Int,
        type: CommunityGroupType,
        accessMode: CommunityAccessMode,
        title: String,
        description: String,
        originLabel: String,
        meetingAt: String?,
        capacity: Int?,
        chatEnabled: Boolean = true
    ): Result<CommunityGroup> = runCatching {
        val savedToken = token(context) ?: error("Cal iniciar sessió.")
        val serverType = when (type) {
            CommunityGroupType.CAR -> "CAR"
            CommunityGroupType.TRANSPORT, CommunityGroupType.TRAIN -> "TRANSPORT"
            CommunityGroupType.MEETUP -> "MEETUP"
            else -> "OTHER"
        }
        val payload = JSONObject()
            .put("group_id", groupId)
            .put("type", serverType)
            .put("access_mode", accessMode.name)
            .put("title", title.trim())
            .put("description", description.trim())
            .put("origin_label", originLabel.trim())
        if (meetingAt.isNullOrBlank()) payload.put("meeting_at", JSONObject.NULL) else payload.put("meeting_at", meetingAt.trim())
        if (capacity != null && capacity > 0) payload.put("capacity", capacity) else payload.put("capacity", JSONObject.NULL)
        val root = jsonRequest("community/update.php", "POST", payload, savedToken)
        val group = parseCommunityGroup(root.getJSONObject("group"))
        postAuthenticatedJson(context, "community/board-settings.php", JSONObject()
            .put("group_id", group.id)
            .put("board_type", type.name)
            .put("chat_enabled", chatEnabled)).getOrThrow()
        group.copy(type = type, chatEnabled = chatEnabled)
    }

    fun fetchCommunityBoardInbox(context: Context): Result<List<CommunityBoardInboxMessage>> = runCatching {
        val root = getAuthenticatedJson(context, "community/inquiries.php?inbox=1").getOrThrow()
        val messages = root.optJSONArray("messages") ?: JSONArray()
        buildList {
            for (i in 0 until messages.length()) {
                val obj = messages.optJSONObject(i) ?: continue
                add(CommunityBoardInboxMessage(
                    id = obj.optInt("id"), groupId = obj.optInt("group_id"), eventId = obj.optInt("event_id"),
                    title = obj.optString("board_title"), author = obj.optString("author"),
                    authorUserId = obj.optInt("author_user_id"), body = obj.optString("body"),
                    createdAt = obj.optString("created_at")
                ))
            }
        }
    }

    fun fetchCommunityBoardInquiries(context: Context, groupId: Int): Result<List<CommunityBoardInquiry>> = runCatching {
        val path = "community/inquiries.php?group_id=$groupId"
        val root = getAuthenticatedJson(context, path).getOrThrow()
        val messages = root.optJSONArray("messages") ?: JSONArray()
        buildList {
            for (i in 0 until messages.length()) {
                val obj = messages.optJSONObject(i) ?: continue
                add(CommunityBoardInquiry(
                    id = obj.optInt("id"), groupId = groupId,
                    peerUserId = obj.optInt("peer_user_id"), peerName = obj.optString("peer_name"),
                    author = obj.optString("author"), body = obj.optString("body"),
                    createdAt = obj.optString("created_at"), own = obj.optBoolean("own")
                ))
            }
        }
    }

    fun sendCommunityBoardInquiry(context: Context, groupId: Int, peerUserId: Int?, message: String): Result<Unit> = runCatching {
        val body = JSONObject().put("group_id", groupId).put("body", message.trim())
        peerUserId?.takeIf { it > 0 }?.let { body.put("peer_user_id", it) }
        postAuthenticatedJson(context, "community/inquiries.php", body).getOrThrow()
        Unit
    }

    fun joinCommunityGroup(context: Context, groupId: Int): Result<CommunityGroup> = runCatching {
        val savedToken = token(context) ?: error("Cal iniciar sessió.")
        val root = jsonRequest("community/join.php", "POST", JSONObject().put("group_id", groupId), savedToken)
        parseCommunityGroup(root.getJSONObject("group"))
    }

    fun leaveCommunityGroup(context: Context, groupId: Int): Result<CommunityGroup> = runCatching {
        val savedToken = token(context) ?: error("Cal iniciar sessió.")
        val root = jsonRequest("community/leave.php", "POST", JSONObject().put("group_id", groupId), savedToken)
        parseCommunityGroup(root.getJSONObject("group"))
    }

    fun fetchCommunityRequests(context: Context, groupId: Int): Result<List<CommunityMemberRequest>> = runCatching {
        val savedToken = token(context) ?: error("Cal iniciar sessió.")
        val path = "community/requests.php?group_id=" + URLEncoder.encode(groupId.toString(), StandardCharsets.UTF_8.name())
        val connection = open(path, "GET", savedToken)
        val root = JSONObject(readResponse(connection))
        if (!root.optBoolean("ok", false)) error(root.optString("message", "No s'han pogut carregar les sol·licituds."))
        val arr = root.optJSONArray("requests") ?: JSONArray()
        buildList {
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                add(CommunityMemberRequest(
                    userId = item.optInt("user_id"),
                    displayName = item.optString("display_name"),
                    photoUri = resolvePublicUrl(item.optString("photo_url")).takeIf { it.isNotBlank() },
                    requestedAt = item.optString("requested_at")
                ))
            }
        }
    }

    fun respondCommunityRequest(
        context: Context,
        groupId: Int,
        userId: Int,
        accept: Boolean
    ): Result<CommunityGroup> = runCatching {
        val savedToken = token(context) ?: error("Cal iniciar sessió.")
        val payload = JSONObject()
            .put("group_id", groupId)
            .put("user_id", userId)
            .put("action", if (accept) "ACCEPT" else "REJECT")
        val root = jsonRequest("community/respond.php", "POST", payload, savedToken)
        parseCommunityGroup(root.getJSONObject("group"))
    }

    fun fetchCommunityMessages(context: Context, groupId: Int): Result<List<CommunityChatMessage>> = runCatching {
        val savedToken = token(context) ?: error("Cal iniciar sessió.")
        val path = "community/messages.php?group_id=" + URLEncoder.encode(groupId.toString(), StandardCharsets.UTF_8.name())
        val connection = open(path, "GET", savedToken)
        val root = JSONObject(readResponse(connection))
        if (!root.optBoolean("ok", false)) error(root.optString("message", "No s'han pogut carregar els missatges."))
        parseCommunityMessages(root.optJSONArray("messages") ?: JSONArray())
    }

    fun sendCommunityMessage(context: Context, groupId: Int, text: String): Result<CommunityChatMessage> = runCatching {
        val savedToken = token(context) ?: error("Cal iniciar sessió.")
        val payload = JSONObject().put("group_id", groupId).put("text", text.trim())
        val root = jsonRequest("community/send.php", "POST", payload, savedToken)
        parseCommunityMessage(root.getJSONObject("message"))
    }

    fun setCommunityGroupStatus(context: Context, groupId: Int, status: String): Result<CommunityGroup> = runCatching {
        val savedToken = token(context) ?: error("Cal iniciar sessió.")
        val payload = JSONObject().put("group_id", groupId).put("status", status.uppercase())
        val root = jsonRequest("community/status.php", "POST", payload, savedToken)
        parseCommunityGroup(root.getJSONObject("group"))
    }

    private fun parseCommunityGroups(array: JSONArray): List<CommunityGroup> = buildList {
        for (i in 0 until array.length()) array.optJSONObject(i)?.let { add(parseCommunityGroup(it)) }
    }

    private fun parseCommunityGroup(obj: JSONObject): CommunityGroup {
        val membersArray = obj.optJSONArray("members") ?: JSONArray()
        val members = buildSet {
            for (i in 0 until membersArray.length()) {
                membersArray.optJSONObject(i)?.optString("display_name")?.trim()?.takeIf(String::isNotBlank)?.let(::add)
            }
        }
        val type = when (obj.optString("type").uppercase()) {
            "CAR" -> CommunityGroupType.CAR
            "TRANSPORT" -> CommunityGroupType.TRANSPORT
            "MEETUP" -> CommunityGroupType.MEETUP
            else -> CommunityGroupType.OTHER
        }
        val access = if (obj.optString("access_mode").uppercase() == "OPEN") CommunityAccessMode.OPEN else CommunityAccessMode.APPROVAL
        val rawCapacity = obj.optInt("capacity", 0)
        return CommunityGroup(
            id = obj.optInt("id"),
            concertId = obj.optInt("event_id"),
            type = type,
            creator = obj.optString("creator_name"),
            town = obj.optString("origin_label"),
            title = obj.optString("title"),
            description = obj.optString("description"),
            capacity = rawCapacity.takeIf { it > 0 },
            members = members,
            creatorUserId = obj.optInt("creator_user_id").takeIf { it > 0 },
            eventTitle = obj.optString("event_title"),
            accessMode = access,
            meetingAt = obj.optString("meeting_at"),
            status = obj.optString("status", "OPEN"),
            participantCount = obj.optInt("participant_count", 0),
            pendingCount = obj.optInt("pending_count", 0),
            myStatus = obj.optString("my_status", "NONE"),
            myRole = obj.optString("my_role"),
            myMessageCount = obj.optInt("my_message_count", 0)
        )
    }

    private fun parseCommunityMessages(array: JSONArray): List<CommunityChatMessage> = buildList {
        for (i in 0 until array.length()) array.optJSONObject(i)?.let { add(parseCommunityMessage(it)) }
    }

    private fun parseCommunityMessage(obj: JSONObject): CommunityChatMessage = CommunityChatMessage(
        id = obj.optInt("id"),
        groupId = obj.optInt("group_id"),
        author = obj.optString("author"),
        text = obj.optString("text"),
        timestamp = obj.optString("created_at").replace('T', ' ').takeLast(8).take(5).ifBlank { "ara" },
        authorUserId = obj.optInt("user_id").takeIf { it > 0 },
        photoUri = resolvePublicUrl(obj.optString("photo_url")).takeIf { it.isNotBlank() },
        createdAt = obj.optString("created_at")
    )

    fun fetchMyTickets(context: Context): Result<List<PurchasedTicket>> = runCatching {
        val savedToken = token(context) ?: error("La sessió de servidor no està disponible. Torna a iniciar sessió.")
        val connection = open("tickets/mine.php", "GET", savedToken)
        val root = JSONObject(readResponse(connection))
        if (!root.optBoolean("ok", false)) error(root.optString("message", "No s'han pogut carregar les entrades."))
        parseTickets(root.optJSONArray("tickets") ?: JSONArray())
    }

    fun fetchManagedTickets(context: Context, concertId: Int): Result<List<PurchasedTicket>> = runCatching {
        val savedToken = token(context) ?: error("La sessió de servidor no està disponible. Torna a iniciar sessió.")
        val path = "tickets/event.php?event_id=" + URLEncoder.encode(concertId.toString(), StandardCharsets.UTF_8.name())
        val connection = open(path, "GET", savedToken)
        val root = JSONObject(readResponse(connection))
        if (!root.optBoolean("ok", false)) error(root.optString("message", "No s'han pogut carregar les entrades."))
        parseTickets(root.optJSONArray("tickets") ?: JSONArray())
    }

    fun validateTicket(context: Context, concertId: Int, code: String): Result<TicketValidationResult> = runCatching {
        val savedToken = token(context) ?: error("La sessió de servidor no està disponible. Torna a iniciar sessió.")
        val payload = JSONObject().put("event_id", concertId).put("code", code.trim())
        val response = jsonRequest("tickets/validate.php", "POST", payload, savedToken)
        val ticket = response.optJSONObject("ticket")?.let(::parseTicket)
        TicketValidationResult(
            outcome = response.optString("outcome", "INVALID"),
            message = response.optString("message", "Entrada no vàlida."),
            ticket = ticket
        )
    }

    fun setTicketStatus(context: Context, concertId: Int, code: String, status: TicketStatus): Result<PurchasedTicket> = runCatching {
        require(status == TicketStatus.ACTIVE || status == TicketStatus.CANCELLED) { "Estat d'entrada no admés." }
        val savedToken = token(context) ?: error("La sessió de servidor no està disponible. Torna a iniciar sessió.")
        val payload = JSONObject()
            .put("event_id", concertId)
            .put("code", code.trim())
            .put("status", status.name)
        val response = jsonRequest("tickets/status.php", "POST", payload, savedToken)
        parseTicket(response.getJSONObject("ticket"))
    }

    private fun parseAdvertisements(array: JSONArray): List<EventAdvertisement> = buildList {
        for (i in 0 until array.length()) {
            array.optJSONObject(i)?.let { add(parseAdvertisement(it)) }
        }
    }

    private fun parseAdvertisement(obj: JSONObject): EventAdvertisement {
        val targets = obj.optJSONObject("targets") ?: JSONObject()
        fun stringSet(key: String): Set<String> {
            val arr = targets.optJSONArray(key) ?: JSONArray()
            return buildSet {
                for (i in 0 until arr.length()) {
                    arr.optString(i).trim().takeIf(String::isNotBlank)?.let(::add)
                }
            }
        }

        val placement = runCatching {
            AdPlacement.valueOf(obj.optString("placement", "FEED_NORMAL").uppercase())
        }.getOrDefault(AdPlacement.FEED_NORMAL)

        val starts = parseServerTimestamp(obj.optString("starts_at")) ?: System.currentTimeMillis()
        val ends = parseServerTimestamp(obj.optString("ends_at"))
        val durationHours = obj.optInt("duration_hours", 24).coerceAtLeast(1)
        val effectiveDuration = ends?.let { ((it - starts) / (60L * 60L * 1000L)).toInt().coerceAtLeast(1) }
            ?: durationHours

        return EventAdvertisement(
            id = obj.getInt("id"),
            ownerUsername = obj.optString("owner_username"),
            concertId = obj.getInt("event_id"),
            createdAtMillis = parseServerTimestamp(obj.optString("created_at")) ?: starts,
            startsAtMillis = starts,
            durationHours = effectiveDuration,
            imageUri = resolvePublicUrl(obj.optString("image_url")).takeIf { it.isNotBlank() },
            cardFocusX = obj.optDouble("card_focus_x", 0.5).toFloat().coerceIn(0f, 1f),
            cardFocusY = obj.optDouble("card_focus_y", 0.5).toFloat().coerceIn(0f, 1f),
            targetStyles = stringSet("styles"),
            targetSearchTerms = stringSet("search_terms"),
            targetTowns = stringSet("towns"),
            targetComarques = stringSet("comarques"),
            targetFollowers = targets.optBoolean("followers", false),
            targetHighIntent = targets.optBoolean("high_intent", false),
            matchAllCriteria = obj.optString("match_mode", "ANY").equals("ALL", ignoreCase = true),
            placement = placement,
            amountCents = kotlin.math.round(obj.optDouble("amount", 0.0) * 100.0).toInt(),
            status = obj.optString("status", "active"),
            serverEndsAtMillis = ends
        )
    }

    private fun parseStories(array: JSONArray): List<Story> = buildList {
        for (i in 0 until array.length()) {
            array.optJSONObject(i)?.let { add(parseStory(it)) }
        }
    }

    private fun parseStory(obj: JSONObject): Story {
        val authorName = obj.optString("author_name")
            .ifBlank { obj.optString("author_username", "Usuari") }
        val eventArtist = obj.optString("event_artist").trim()
        val displayArtist = eventArtist.ifBlank { authorName }
        val initials = displayArtist
            .split(" ")
            .filter(String::isNotBlank)
            .take(2)
            .joinToString("") { it.take(1).uppercase() }
            .ifBlank { "AV" }

        return Story(
            id = obj.getInt("id"),
            artist = displayArtist,
            initials = initials,
            title = obj.optString("title"),
            body = obj.optString("body"),
            publishedAtMillis = parseServerTimestamp(obj.optString("published_at"))
                ?: System.currentTimeMillis(),
            eventId = obj.optInt("event_id").takeIf { it > 0 },
            eventTitle = obj.optString("event_title"),
            eventTown = obj.optString("event_town"),
            eventDate = obj.optString("event_date"),
            eventTime = obj.optString("event_time").takeIf { it != "null" }.orEmpty(),
            creatorUserId = obj.optInt("creator_user_id").takeIf { it > 0 },
            authorUsername = obj.optString("author_username"),
            organizer = obj.optString("author_type").equals("ORGANIZER", ignoreCase = true),
            canEdit = obj.optBoolean("can_edit", false),
            sourceType = obj.optString("source_type", "MANUAL"),
            storyKey = obj.optString("story_key").ifBlank { "manual:${obj.optInt("id")}" },
            seen = obj.optBoolean("seen", false),
            relevance = obj.optInt("relevance", 0)
        )
    }

    private fun parseTickets(array: JSONArray): List<PurchasedTicket> = buildList {
        for (i in 0 until array.length()) {
            array.optJSONObject(i)?.let { add(parseTicket(it)) }
        }
    }

    private fun parseTicket(obj: JSONObject): PurchasedTicket {
        val status = runCatching { TicketStatus.valueOf(obj.optString("status", "ACTIVE").uppercase()) }
            .getOrDefault(TicketStatus.ACTIVE)
        val username = obj.optString("buyer_username").ifBlank { obj.optString("buyer_name", "Usuari") }
        return PurchasedTicket(
            code = obj.getString("code"),
            concertId = obj.getInt("event_id"),
            buyerUsername = username,
            buyerDisplayName = obj.optString("buyer_name").ifBlank { username },
            status = status,
            purchasedAtMillis = parseServerTimestamp(obj.optString("created_at")) ?: System.currentTimeMillis(),
            usedAtMillis = parseServerTimestamp(obj.optString("validated_at")),
            serverId = obj.optLong("id").takeIf { it > 0 },
            ticketOptionName = obj.optString("ticket_option_name").takeIf { it != "null" }.orEmpty(),
            sessionDate = obj.optString("session_date").takeIf { it != "null" }.orEmpty(),
            sessionTime = obj.optString("session_time").takeIf { it != "null" }.orEmpty(),
            sector = obj.optString("sector").takeIf { it != "null" }.orEmpty(),
            offer = obj.optString("offer").takeIf { it != "null" }.orEmpty()
        )
    }

    private fun parseServerTimestamp(raw: String?): Long? {
        val value = raw?.trim().orEmpty()
        if (value.isBlank() || value.equals("null", ignoreCase = true)) return null
        val normalized = value.replace('T', ' ').removeSuffix("Z")
        return runCatching {
            LocalDateTime.parse(normalized.take(19), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                .toInstant(ZoneOffset.UTC).toEpochMilli()
        }.getOrNull()
    }

    private fun parseEvents(
        array: JSONArray,
        context: Context,
        entityImages: EntityImageCatalog = EntityImageCatalog.EMPTY,
        municipalityCatalogOverride: List<Municipality>? = null
    ): List<Concert> {
        // En càrrega normal usem el catàleg territorial una sola vegada. En el camí
        // d'arrancada es passa emptyList() explícitament per evitar I/O abans del feed.
        val municipalityCatalog = municipalityCatalogOverride ?: MunicipalityRepository.loadCached(context)
        val parsed = buildList {
            for (i in 0 until array.length()) {
                // Un sol registre malformat no pot anul·lar tota la cerca o els perfils.
                val event = array.optJSONObject(i) ?: continue
                runCatching { parseEvent(event, context, entityImages, municipalityCatalog) }
                    .onSuccess { parsedEvent -> add(parsedEvent) }
                    .onFailure { Log.w("AgendaSync", "Event parse error row=$i id=${event.optInt("id")}: ${it.message}") }
            }
        }
        // El servidor pot retornar una fila virtual per jornada. Dins de l'estat de l'app
        // mantenim un únic Concert real i conservem les dates perquè l'algoritme el puntue
        // una vegada. expandMultiDayOccurrences() crearà després les instàncies visuals.
        return collapseFeedInstances(parsed)
    }

    private fun collapseFeedInstances(source: List<Concert>): List<Concert> = source
        .groupBy { if (it.id > 0) it.id.toString() else it.feedInstanceKey }
        .values
        .map { group ->
            val first = group.first()
            if (group.size == 1 && first.feedOccurrenceDate.isNullOrBlank() && first.selectedDate.isNullOrBlank()) first
            else {
                val dates = group.flatMap { row ->
                    row.scheduleDates + listOfNotNull(row.feedOccurrenceDate, row.selectedDate)
                }.map(String::trim).filter(String::isNotBlank).map { it.take(10) }.distinct().sorted()
                first.copy(
                    lineup = group.flatMap { it.lineup }.map(String::trim).filter(String::isNotBlank).distinctBy { it.lowercase(Locale.ROOT) },
                    organizers = group.flatMap { it.organizers }.map(String::trim).filter(String::isNotBlank).distinctBy { it.lowercase(Locale.ROOT) },
                    scheduleDates = dates,
                    scheduleDaySummaries = group.flatMap { it.scheduleDaySummaries.entries }.associate { it.key to it.value },
                    scheduleDayStartTimes = group.flatMap { it.scheduleDayStartTimes.entries }.associate { it.key to it.value },
                    scheduleDayTicketPrices = group.flatMap { it.scheduleDayTicketPrices.entries }.associate { it.key to it.value },
                    isMultidayFromServer = group.any { it.isMultidayFromServer } || dates.size > 1,
                    feedOccurrenceDate = null,
                    selectedDate = null,
                    feedInstanceUid = null
                )
            }
        }


    private fun scheduleMetaFromDay(obj: JSONObject): Triple<Map<String, String>, Map<String, String>, Map<String, Int>> {
        val schedule = obj.optJSONArray("schedule_for_day") ?: return Triple(emptyMap(), emptyMap(), emptyMap())
        val fallbackDate = sequenceOf(obj.optString("feed_instance_date"), obj.optString("selected_date"))
            .map(String::trim).firstOrNull { it.isNotBlank() && it != "null" }?.take(10).orEmpty()
        val labelsByDate = linkedMapOf<String, MutableList<String>>()
        val startByDate = linkedMapOf<String, String>()
        val priceByDate = linkedMapOf<String, Int>()
        for (i in 0 until schedule.length()) {
            val item = schedule.optJSONObject(i) ?: continue
            val date = sequenceOf(item.optString("date"), item.optString("schedule_date"), item.optString("start_at"))
                .map(String::trim).firstOrNull { it.isNotBlank() && it != "null" }?.take(10) ?: fallbackDate
            if (date.isBlank()) continue
            val artist = item.optString("artist_name").takeIf { it != "null" }.orEmpty().trim()
            val labels = if (artist.isNotBlank()) {
                artist.split(Regex("\\s*[|;]\\s*")).map(String::trim).filter(String::isNotBlank)
            } else {
                listOf(item.optString("title").takeIf { it != "null" }.orEmpty().trim()).filter(String::isNotBlank)
            }
            val bucket = labelsByDate.getOrPut(date) { mutableListOf() }
            labels.forEach { label -> if (bucket.none { it.equals(label, ignoreCase = true) }) bucket += label }
            val time = item.optString("start_time").takeIf { it != "null" }.orEmpty().trim().take(5)
            if (time.isNotBlank() && date !in startByDate) startByDate[date] = time
            if (item.has("ticket_price") && !item.isNull("ticket_price") && date !in priceByDate) {
                item.optDouble("ticket_price", Double.NaN).takeIf { !it.isNaN() }?.let { priceByDate[date] = it.roundToInt().coerceAtLeast(0) }
            }
        }
        val summaries = labelsByDate.mapValues { (_, values) -> values.joinToString(" · ") }
        return Triple(summaries, startByDate, priceByDate)
    }

    private fun scheduleDatesFrom(obj: JSONObject): List<String> = buildList {
        obj.optJSONArray("schedule_dates")?.let { dates ->
            for (i in 0 until dates.length()) dates.optString(i).trim().takeIf(String::isNotBlank)?.let { add(it.take(10)) }
        }
        obj.optJSONArray("schedule_for_day")?.let { schedule ->
            for (i in 0 until schedule.length()) {
                val item = schedule.optJSONObject(i) ?: continue
                sequenceOf(item.optString("date"), item.optString("schedule_date"), item.optString("start_at"))
                    .map(String::trim).firstOrNull { it.isNotBlank() && it != "null" }?.let { add(it.take(10)) }
            }
        }
        obj.optString("feed_instance_date").trim().takeIf { it.isNotBlank() && it != "null" }?.let { add(it.take(10)) }
        obj.optString("selected_date").trim().takeIf { it.isNotBlank() && it != "null" }?.let { add(it.take(10)) }
    }.distinct().sorted()

    private fun parseEvent(
        obj: JSONObject,
        context: Context,
        entityImages: EntityImageCatalog = EntityImageCatalog.EMPTY,
        municipalityCatalog: List<Municipality>? = null
    ): Concert {
        val artistsJson = obj.optJSONArray("artists") ?: JSONArray()
        val artists = buildList {
            for (i in 0 until artistsJson.length()) {
                val item = artistsJson.opt(i)
                val name = when (item) {
                    is JSONObject -> item.optString("name").ifBlank { item.optString("artist_name") }
                    is String -> item
                    else -> ""
                }.trim()
                if (name.isNotBlank()) add(name)
            }
            // Compatibilitat amb versions de l'API que envien noms en format pla.
            if (isEmpty()) {
                listOf("artist_names", "professional_artist_names", "artists_text", "artist")
                    .asSequence().map { obj.optString(it).trim() }
                    .firstOrNull { it.isNotBlank() && it != "null" }
                    ?.split(Regex("\\s*[|;]\\s*"))
                    ?.map(String::trim)?.filter(String::isNotBlank)?.let(::addAll)
            }
        }.distinctBy(AgendaDedupe::artistKey)
        val title = obj.optString("title").trim()
        val primary = artists.firstOrNull() ?: title.ifBlank { "Esdeveniment" }
        val id = obj.optInt("event_id").takeIf { it > 0 } ?: obj.getInt("id")
        val bundledClassification = BundledClassifications.lookup(
            context, id, obj.optString("feed_uid"), title.ifBlank { primary }
        )
        val category = categoryFrom(obj.optString("categoria_principal_nova")
            .takeIf { it.isNotBlank() && it != "null" }
            ?: bundledClassification?.categoryCode
            ?: obj.optString("category"))
        val town = MunicipalityRepository.canonicalTown(obj.optString("town"))
        val posterUrl = resolvePublicUrl(obj.optString("poster_url"))

        val price = nullableNumber(obj, "price")?.roundToInt() ?: 0
        val ticketPrice = nullableNumber(obj, "ticket_price")?.roundToInt() ?: -1
        val ticketUrl = obj.optString("ticket_url").takeIf { it != "null" }.orEmpty().trim()
        val ticketFlag = when {
            obj.has("ticket_enabled") -> jsonBoolean(obj, "ticket_enabled", false)
            obj.has("ticket_available") -> jsonBoolean(obj, "ticket_available", false)
            obj.has("ticket_sales_enabled") -> jsonBoolean(obj, "ticket_sales_enabled", false)
            else -> false
        }
        val effectiveTicketAvailable = ticketFlag || ticketUrl.isNotBlank()
        val directArtistImage = resolvePublicUrl(obj.optString("artist_image_url")).takeIf(String::isNotBlank)
        val directVenueImage = resolvePublicUrl(obj.optString("venue_image_url")).takeIf(String::isNotBlank)
        val directTownImage = resolvePublicUrl(obj.optString("town_image_url")).takeIf(String::isNotBlank)
        val hasEntityImages = entityImages !== EntityImageCatalog.EMPTY &&
            (entityImages.artists.isNotEmpty() || entityImages.venues.isNotEmpty() ||
                entityImages.towns.isNotEmpty() || entityImages.categories.isNotEmpty() ||
                entityImages.organizers.isNotEmpty())
        val artistImage = directArtistImage ?: if (hasEntityImages) {
            artists.asSequence().mapNotNull { entityImages.artists[entityLookupKey(it)] }.firstOrNull()
        } else null
        val venueImage = directVenueImage ?: if (hasEntityImages) entityImages.venues[entityLookupKey(obj.optString("venue"))] else null
        val townImage = directTownImage ?: if (hasEntityImages) entityImages.towns[entityLookupKey(town)] else null
        val categoryImage = if (hasEntityImages) {
            entityImages.categories[entityLookupKey(category.label)]
                ?: entityImages.categories[entityLookupKey(category.name)]
        } else null
        val organizerLabels = buildList {
            val organizerArray = obj.optJSONArray("organizers")
            if (organizerArray != null) {
                for (index in 0 until organizerArray.length()) {
                    val raw = organizerArray.opt(index)
                    val label = when (raw) {
                        is JSONObject -> raw.optString("name").ifBlank { raw.optString("display_name") }
                        else -> raw?.toString().orEmpty()
                    }.trim()
                    if (label.isNotBlank()) add(label)
                }
            }
            obj.optString("organizer").trim().takeIf(String::isNotBlank)?.let(::add)
        }
        val organizerImage = if (hasEntityImages) {
            organizerLabels
                .sortedBy { if (it.contains("festival", ignoreCase = true)) 0 else 1 }
                .asSequence()
                .mapNotNull { entityImages.organizers[entityLookupKey(it)] }
                .firstOrNull()
        } else null
        val languages = buildList {
            obj.optJSONArray("languages")?.let { array ->
                for (index in 0 until array.length()) {
                    array.optString(index).trim().takeIf(String::isNotBlank)?.let(::add)
                }
            }
            if (isEmpty()) {
                obj.optString("language")
                    .replace(" / ", "|")
                    .split('|', ',', ';')
                    .map(String::trim)
                    .filter(String::isNotBlank)
                    .forEach(::add)
            }
        }.distinctBy { it.lowercase(Locale.ROOT) }
        val feedInstanceDate = obj.optString("feed_instance_date").takeIf { it.isNotBlank() && it != "null" }?.take(10)
        val selectedDate = obj.optString("selected_date").takeIf { it.isNotBlank() && it != "null" }?.take(10)
        val scheduleDates = scheduleDatesFrom(obj)
        val capacity = obj.optInt("capacity", -1).takeIf { it >= 0 }
        val ticketOptions = buildList {
            val options = obj.optJSONArray("ticket_options") ?: obj.optJSONArray("ticket_types") ?: JSONArray()
            for (i in 0 until options.length()) {
                val item = options.optJSONObject(i) ?: continue
                val optionId = item.optLong("id", 0L)
                if (optionId <= 0L) continue
                add(
                    TicketOption(
                        id = optionId,
                        name = item.optString("name", item.optString("label", "Entrada")).ifBlank { "Entrada" },
                        sessionDate = item.optString("session_date", item.optString("date", "")).takeIf { it != "null" }.orEmpty(),
                        sessionTime = item.optString("session_time", item.optString("time", "")).takeIf { it != "null" }.orEmpty(),
                        sector = item.optString("sector").takeIf { it != "null" }.orEmpty(),
                        offer = item.optString("offer").takeIf { it != "null" }.orEmpty(),
                        price = nullableNumber(item, "price")?.roundToInt()?.coerceAtLeast(0) ?: ticketPrice.coerceAtLeast(0),
                        capacity = item.optInt("capacity", -1).takeIf { it >= 0 },
                        sold = item.optInt("sold", 0).coerceAtLeast(0),
                        reserved = item.optInt("reserved", 0).coerceAtLeast(0),
                        maxPerOrder = item.optInt("max_per_order", 6).coerceIn(1, 20),
                        saleStartsAt = item.optString("sale_starts_at").takeIf { it != "null" }.orEmpty(),
                        saleEndsAt = item.optString("sale_ends_at").takeIf { it != "null" }.orEmpty(),
                        active = jsonBoolean(item, "active", true)
                    )
                )
            }
        }
        val (embeddedDaySummaries, embeddedDayStartTimes, embeddedDayTicketPrices) = scheduleMetaFromDay(obj)
        return Concert(
            id = id,
            sourceFeedUid = obj.optString("feed_uid").takeIf { it != "null" }.orEmpty(),
            artist = primary,
            title = title.ifBlank { primary },
            subtitle = obj.optString("subtitle").takeIf { it.isNotBlank() && it != "null" }.orEmpty(),
            lineup = artists.ifEmpty { listOf(primary) },
            posterUri = posterUrl.takeIf(String::isNotBlank),
            artistImageUri = artistImage,
            venueImageUri = venueImage,
            townImageUri = townImage,
            categoryImageUri = categoryImage,
            organizerImageUri = organizerImage,
            organizers = organizerLabels.distinct(),
            date = obj.optString("start_date").takeIf { it.isNotBlank() && it != "null" } ?: obj.optString("date"),
            time = obj.optString("start_time").takeIf { it.isNotBlank() && it != "null" } ?: obj.optString("time").takeIf { it != "null" }.orEmpty(),
            endDate = obj.optString("end_date").takeIf { it.isNotBlank() && it != "null" }.orEmpty(),
            endTime = obj.optString("end_time").takeIf { it.isNotBlank() && it != "null" }.orEmpty(),
            feedOccurrenceDate = feedInstanceDate,
            feedInstanceUid = obj.optString("feed_instance_uid").takeIf { it.isNotBlank() && it != "null" },
            selectedDate = selectedDate,
            scheduleDates = scheduleDates,
            scheduleDaySummaries = embeddedDaySummaries,
            scheduleDayStartTimes = embeddedDayStartTimes,
            scheduleDayTicketPrices = embeddedDayTicketPrices,
            isMultidayFromServer = jsonBoolean(obj, "is_multiday", false),
            // La classificació estructural ha d'arribar també als objectes reconstruïts
            // des de SQLite/cache. Sense açò, un anunci d'un FESTIVAL podia quedar com
            // AUTO i obrir-se com un concert normal encara que el mateix event, des del
            // feed complet, obrira correctament el popup de festival.
            eventStructure = obj.optString("event_structure", "AUTO").trim().ifBlank { "AUTO" }.uppercase(),
            scheduleStatus = obj.optString("schedule_status", "NONE").trim().ifBlank { "NONE" }.uppercase(),
            town = town,
            comarca = MunicipalityRepository.comarcaFor(municipalityCatalog ?: MunicipalityRepository.loadCached(context), town)
                ?: obj.optString("comarca").takeIf { it.isNotBlank() && it != "null" }
                ?: "Sense comarca",
            venue = obj.optString("venue").takeIf { it != "null" }.orEmpty(),
            styles = splitList(obj.optString("styles")).ifEmpty { listOf(category.label) },
            subcategoryCodes = (obj.optJSONArray("subcategories")?.let { arr ->
                (0 until arr.length()).mapNotNull { index ->
                    arr.optString(index).trim().takeIf(String::isNotBlank)
                }
            } ?: splitList(obj.optString("subcategories_codis")))
                .map(CategoryTaxonomy::canonicalizeCode)
                .filter(CategoryTaxonomy::isAnySubcategoryKey)
                .distinct().ifEmpty { bundledClassification?.codes.orEmpty() },
            isInfantil = jsonBoolean(obj, "infantil_transversal", false) || jsonBoolean(obj, "is_infantil", false) ||
                (bundledClassification?.infantil == true),
            price = price,
            language = obj.optString("language").takeIf { it.isNotBlank() && it != "null" }.orEmpty(),
            languages = languages,
            description = obj.optString("description").takeIf { it != "null" }.orEmpty(),
            category = category,
            ticketPrice = ticketPrice,
            ticketAvailable = effectiveTicketAvailable,
            ticketUrl = ticketUrl,
            capacity = capacity,
            ticketOptions = ticketOptions,
            savedCount = obj.optInt("saved_count", 0).coerceAtLeast(0),
            likeCount = obj.optInt("like_count", 0).coerceAtLeast(0),
            creatorUserId = obj.optInt("creator_user_id").takeIf { it > 0 },
            canEditByCurrentUser = jsonBoolean(obj, "can_edit", false),
            createdAtMillis = parseServerTimestamp(obj.optString("created_at")) ?: 0L
        )
    }

    private fun entityLookupKey(raw: String): String {
        val decomposed = Normalizer.normalize(raw.trim(), Normalizer.Form.NFD)
            .replace(entityCombiningMarksRegex, "")
            .replace('’', '\'')
            .lowercase(Locale.ROOT)
        return decomposed.replace(entityWhitespaceRegex, " ")
    }

    private fun parseEntityCatalogBundle(root: JSONObject): EntityCatalogBundle {
        val artists = linkedMapOf<String, String>()
        val venues = linkedMapOf<String, String>()
        val towns = linkedMapOf<String, String>()
        val categories = linkedMapOf<String, String>()
        val organizers = linkedMapOf<String, String>()
        val groupedNames = linkedMapOf<String, MutableList<String>>()
        val affinities = linkedMapOf<String, EntityAffinityMetadata>()
        val items = root.optJSONArray("entities") ?: JSONArray()
        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            val name = item.optString("name", item.optString("display_name")).trim()
            val type = item.optString("type", item.optString("entity_type")).trim().uppercase(Locale.ROOT)
            if (name.isBlank() || type.isBlank()) continue

            groupedNames.getOrPut(type) { mutableListOf() }.add(name)

            val url = resolvePublicUrl(item.optString("image_url"))
            if (url.isNotBlank()) {
                when (type) {
                    "ARTIST" -> artists[entityLookupKey(name)] = url
                    "VENUE" -> venues[entityLookupKey(name)] = url
                    "TOWN" -> towns[entityLookupKey(name)] = url
                    "CATEGORY" -> categories[entityLookupKey(name)] = url
                    "ORGANIZER", "FESTIVAL" -> organizers[entityLookupKey(name)] = url
                }
            }

            if (type == "ARTIST") {
                val language = item.optString("language_raw", item.optString("language")).trim()
                val category = item.optString("category_raw", item.optString("category")).trim()
                val styleRaw = item.optString("style_raw", item.optString("style")).trim()
                val styles = styleRaw
                    .replace(" / ", "|")
                    .split('|', ',', ';')
                    .map(String::trim)
                    .filter(String::isNotBlank)
                    .distinctBy(::entityLookupKey)
                val subcategoryCodes = (item.optJSONArray("subcategories")?.let { a ->
                    (0 until a.length()).map { a.optString(it) }
                } ?: splitList(item.optString("subcategories_codis")))
                    .map(CategoryTaxonomy::canonicalizeCode)
                    .filter(CategoryTaxonomy::isAnySubcategoryKey).distinct()
                affinities[entityLookupKey(name)] = EntityAffinityMetadata(
                    type, name, language, category, styles,
                    subcategoryCodes = subcategoryCodes,
                    isInfantil = jsonBoolean(item, "infantil_transversal", false)
                )
            }
        }
        val names = groupedNames.mapValues { (_, values) ->
            values.distinctBy(::entityLookupKey).sortedBy { it.lowercase(Locale.ROOT) }
        }
        return EntityCatalogBundle(
            images = EntityImageCatalog(artists, venues, towns, categories, organizers),
            names = names,
            affinities = affinities
        )
    }

    private fun parseEntityImageCatalog(root: JSONObject): EntityImageCatalog =
        parseEntityCatalogBundle(root).images


    fun ensureEventEntities(
        context: Context,
        artists: List<String>,
        venue: String,
        town: String,
        artistImageUrl: String? = null,
        venueImageUrl: String? = null,
        townImageUrl: String? = null
    ): Result<Unit> = runCatching {
        val savedToken = token(context) ?: error("La sessió de servidor no està disponible.")
        val cleanArtists = artists.map(String::trim).filter(String::isNotBlank).distinct()
        val payload = JSONObject()
            .put("artists", JSONArray(cleanArtists))
            .put("venues", JSONArray(listOf(venue.trim()).filter(String::isNotBlank)))
            .put("towns", JSONArray(listOf(town.trim()).filter(String::isNotBlank)))
        if (!artistImageUrl.isNullOrBlank() && cleanArtists.isNotEmpty()) {
            payload.put("artist_images", JSONObject().put(cleanArtists.first(), artistImageUrl.trim()))
        }
        if (!venueImageUrl.isNullOrBlank() && venue.isNotBlank()) {
            payload.put("venue_images", JSONObject().put(venue.trim(), venueImageUrl.trim()))
        }
        if (!townImageUrl.isNullOrBlank() && town.isNotBlank()) {
            payload.put("town_images", JSONObject().put(town.trim(), townImageUrl.trim()))
        }
        jsonRequest("entities/ensure.php", "POST", payload, savedToken)
        Unit
    }

    fun ensureEntitiesForEvents(context: Context, events: List<Concert>): Result<Unit> = runCatching {
        val savedToken = token(context) ?: error("La sessió de servidor no està disponible.")
        val artists = events.flatMap { it.lineup }.map(String::trim).filter(String::isNotBlank).distinct()
        val venues = events.map { it.venue.trim() }.filter(String::isNotBlank).distinct()
        val towns = events.map { it.town.trim() }.filter(String::isNotBlank).distinct()
        val payload = JSONObject()
            .put("artists", JSONArray(artists))
            .put("venues", JSONArray(venues))
            .put("towns", JSONArray(towns))
        jsonRequest("entities/ensure.php", "POST", payload, savedToken)
        Unit
    }

    private fun nullableNumber(obj: JSONObject, key: String): Double? =
        if (!obj.has(key) || obj.isNull(key)) null else obj.optDouble(key).takeUnless { it.isNaN() }

    /** Accepta booleans JSON reals i també 1/0 o true/false enviats per PHP/MySQL. */
    private fun jsonBoolean(obj: JSONObject, key: String, default: Boolean = false): Boolean {
        if (!obj.has(key) || obj.isNull(key)) return default
        return when (val value = obj.opt(key)) {
            is Boolean -> value
            is Number -> value.toInt() != 0
            is String -> when (value.trim().lowercase()) {
                "1", "true", "si", "sí", "yes", "y" -> true
                "0", "false", "no", "n", "" -> false
                else -> default
            }
            else -> default
        }
    }

    private fun categoryFrom(raw: String): CulturalCategory =
        CulturalCategory.entries.firstOrNull {
            it.name.equals(raw.trim(), ignoreCase = true) || it.label.equals(raw.trim(), ignoreCase = true)
        } ?: CulturalCategory.OTHER

    private fun splitList(raw: String): List<String> {
        if (raw.isBlank() || raw.equals("null", ignoreCase = true)) return emptyList()
        return raw
        .split('|', ',', ';')
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinct()
    }

    private fun resolvePublicUrl(raw: String): String {
        val value = raw.trim()
        if (value.isBlank() || value == "null") return ""
        if (value.startsWith("https://", ignoreCase = true)) return value
        if (value.startsWith("http://", ignoreCase = true)) return ""
        val base = BuildConfig.REMOTE_AGENDA_BASE_URL.trim().let { if (it.endsWith('/')) it else "$it/" }
        return base + value.trimStart('/')
    }

    private fun cachePoster(context: Context, concertId: Int, posterUrl: String): String? {
        if (posterUrl.isBlank()) return null
        val dir = File(File(context.filesDir, CACHE_DIR), "images").apply { mkdirs() }
        val ext = Uri.parse(posterUrl).lastPathSegment.orEmpty().substringAfterLast('.', "jpg").take(5).ifBlank { "jpg" }
        val target = File(dir, "api_$concertId.$ext")
        val prefs = context.getSharedPreferences("remote_agenda_sync", Context.MODE_PRIVATE)
        val oldUrl = prefs.getString("api_poster_url_$concertId", null)
        if (target.exists() && oldUrl == posterUrl) return Uri.fromFile(target).toString()

        return runCatching {
            val connection = (URL(posterUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                requestMethod = "GET"
            }
            try {
                require(connection.responseCode in 200..299) { "HTTP ${connection.responseCode}" }
                val tmp = File(dir, "api_$concertId.tmp")
                connection.inputStream.use { input -> tmp.outputStream().use { output -> input.copyTo(output) } }
                if (target.exists()) target.delete()
                require(tmp.renameTo(target)) { "No s'ha pogut guardar el cartell" }
                prefs.edit().putString("api_poster_url_$concertId", posterUrl).apply()
                Uri.fromFile(target).toString()
            } finally {
                connection.disconnect()
            }
        }.getOrElse { if (target.exists()) Uri.fromFile(target).toString() else null }
    }


    /** Llocs guardats al compte MySQL. No hi ha ID d'usuari al payload: el resol el Bearer. */
    data class ServerLocationPreferences(
        val towns: Map<String, Int>,
        val comarques: Set<String>,
        val exists: Boolean
    )

    private fun parseServerLocations(root: JSONObject): ServerLocationPreferences {
        val places = root.getJSONObject("locations")
        val towns = linkedMapOf<String, Int>()
        val townRows = places.optJSONArray("towns") ?: JSONArray()
        for (i in 0 until townRows.length()) {
            val item = townRows.optJSONObject(i) ?: continue
            val name = item.optString("name").trim()
            if (name.isNotBlank()) towns[name] = PersonalizationOptions.validRadius(item.optInt("radius_km", 30))
        }
        val comarques = linkedSetOf<String>()
        val comarcaRows = places.optJSONArray("comarques") ?: JSONArray()
        for (i in 0 until comarcaRows.length()) {
            val comarca = comarcaRows.optString(i).trim()
            if (comarca.isNotBlank()) comarques += comarca
        }
        return ServerLocationPreferences(towns, comarques, root.optBoolean("exists", false))
    }

    fun fetchLocationPreferences(sessionToken: String): Result<ServerLocationPreferences> = runCatching {
        parseServerLocations(jsonRequest("preferences/locations.php", "GET", JSONObject(), sessionToken))
    }

    fun saveLocationPreferences(
        sessionToken: String,
        towns: Map<String, Int>,
        comarques: Set<String>
    ): Result<ServerLocationPreferences> = runCatching {
        val townRows = JSONArray()
        towns.toSortedMap(String.CASE_INSENSITIVE_ORDER).forEach { (name, radius) ->
            townRows.put(JSONObject().put("name", name.trim())
                .put("radius_km", PersonalizationOptions.validRadius(radius)))
        }
        val comarcaRows = JSONArray()
        comarques.toSortedSet(String.CASE_INSENSITIVE_ORDER).forEach { comarcaRows.put(it.trim()) }
        val body = JSONObject().put("towns", townRows).put("comarques", comarcaRows)
        parseServerLocations(jsonRequest("preferences/locations.php", "POST", body, sessionToken))
    }

    fun fetchEmailPreferences(context: Context): Result<EmailPreferences> = runCatching {
        val savedToken = token(context) ?: error("La sessió de servidor no està disponible.")
        val root = jsonRequest("mail/preferences-api.php", "GET", JSONObject(), savedToken)
        parseEmailPreferences(root.getJSONObject("preferences"))
    }

    fun updateEmailPreferences(context: Context, preferences: EmailPreferences): Result<EmailPreferences> = runCatching {
        val savedToken = token(context) ?: error("La sessió de servidor no està disponible.")
        val payload = JSONObject()
            .put("digest_frequency", preferences.frequency.wireValue)
            .put("recommendations_enabled", preferences.recommendations)
            .put("followed_artists_enabled", preferences.followedArtists)
            .put("saved_reminders_enabled", preferences.savedReminders)
            .put("ads_enabled", preferences.personalizedAds)
        val root = jsonRequest("mail/preferences-api.php", "POST", payload, savedToken)
        parseEmailPreferences(root.getJSONObject("preferences"))
    }

    private fun parseEmailPreferences(item: JSONObject): EmailPreferences = EmailPreferences(
        frequency = EmailDigestFrequency.fromWire(item.optString("digest_frequency", "NONE")),
        recommendations = jsonBoolean(item, "recommendations_enabled", false),
        followedArtists = jsonBoolean(item, "followed_artists_enabled", false),
        savedReminders = jsonBoolean(item, "saved_reminders_enabled", false),
        personalizedAds = jsonBoolean(item, "ads_enabled", false)
    )

    private fun absoluteJsonRequest(url: String, method: String, payload: JSONObject): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT
            readTimeout = READ_TIMEOUT
            requestMethod = method
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Cache-Control", "no-cache")
            doOutput = method != "GET"
            if (doOutput) setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        if (connection.doOutput) {
            BufferedWriter(OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8)).use {
                it.write(payload.toString())
            }
        }
        val raw = readResponse(connection)
        val trimmed = raw.trimStart()
        if (!trimmed.startsWith("{")) {
            error("El sistema de reports no ha retornat JSON. Actualitza el desplegament d’Apps Script i torna-ho a provar.")
        }
        val root = JSONObject(raw)
        if (!root.optBoolean("ok", false)) {
            val error = root.optString("message").ifBlank { root.optString("error") }
            val friendly = when (error) {
                "REPORT_NOT_FOUND" -> "No s’ha trobat este avís per a este dispositiu."
                "REPORT_NOT_DISMISSED" -> "Este avís ja no està disponible per a una nova revisió."
                "REPORT_ALREADY_REOPENED" -> "Este avís ja s’ha enviat a revisar de nou."
                "IDENTITY_REQUIRED" -> "No s’ha pogut identificar el dispositiu que va enviar l’avís."
                "INVALID_FOLLOWUP" -> "L’explicació no és vàlida."
                else -> error
            }
            error(friendly.ifBlank { "Resposta no vàlida del sistema de reports." })
        }
        return root
    }

    private fun jsonRequest(path: String, method: String, payload: JSONObject, token: String?): JSONObject {
        val connection = open(path, method, token).apply {
            doOutput = method != "GET"
            if (doOutput) setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        if (connection.doOutput) {
            BufferedWriter(OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8)).use {
                it.write(payload.toString())
            }
        }
        val raw = readResponse(connection)
        val root = JSONObject(raw)
        if (!root.optBoolean("ok", false)) {
            error(root.optString("message", "Resposta no vàlida del servidor."))
        }
        return root
    }

    private fun open(path: String, method: String, token: String?): HttpURLConnection =
        (URL(baseUrl() + path.removePrefix("/")).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT
            readTimeout = READ_TIMEOUT
            requestMethod = method
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Cache-Control", "no-cache")
            if (!token.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $token")
        }

    private fun readResponse(connection: HttpURLConnection): String {
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val raw = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (raw.isBlank()) error("Resposta buida del servidor (HTTP $code).")
            if (code !in 200..299) {
                val message = runCatching { JSONObject(raw).optString("message") }.getOrNull().orEmpty()
                error(message.ifBlank { "HTTP $code" })
            }
            return raw
        } finally {
            connection.disconnect()
        }
    }

    private fun roleFromServer(raw: String?): AccountRole = when (raw?.trim()?.uppercase()) {
        "SUPER_ADMIN" -> AccountRole.SUPER_ADMIN
        "GROUP" -> AccountRole.GROUP
        "AGENCY" -> AccountRole.AGENCY
        "LABEL" -> AccountRole.LABEL
        "VENUE" -> AccountRole.VENUE
        "PROMOTER", "ORGANIZER", "PROFESSIONAL", "PUBLISHER" -> AccountRole.PROMOTER
        else -> AccountRole.USER
    }
    data class ArtistStoreProduct(
        val id: Long,
        val artistName: String,
        val name: String,
        val productType: String,
        val description: String,
        val imageUrl: String?,
        val priceCents: Long,
        val shippingCents: Long,
        val stock: Int?,
        val active: Boolean
    )

    data class ArtistStorePaymentStart(
        val orderId: Long,
        val clientSecret: String,
        val publishableKey: String,
        val amountCents: Long,
        val currency: String,
        val merchantName: String,
        val mode: String
    )

    data class ArtistStorePaidOrder(
        val orderId: Long,
        val status: String,
        val productName: String,
        val quantity: Int,
        val productTotalCents: Long,
        val shippingCents: Long,
        val totalCents: Long
    )

    fun fetchArtistStoreProducts(context: Context, artistName: String): Result<List<ArtistStoreProduct>> = runCatching {
        val savedToken = token(context)
        val path = "artist-store/catalog.php?artist=" + URLEncoder.encode(artistName, StandardCharsets.UTF_8.name())
        val connection = open(path, "GET", savedToken)
        val root = JSONObject(readResponse(connection))
        if (!root.optBoolean("ok", false)) error(root.optString("message", "No s'ha pogut carregar la botiga."))
        val arr = root.optJSONArray("products") ?: JSONArray()
        buildList {
            for (i in 0 until arr.length()) {
                val p = arr.optJSONObject(i) ?: continue
                add(ArtistStoreProduct(
                    id = p.optLong("id"),
                    artistName = p.optString("artist_name", artistName),
                    name = p.optString("name"),
                    productType = p.optString("product_type", "MERCH"),
                    description = p.optString("description"),
                    imageUrl = p.optString("image_url").takeIf(String::isNotBlank),
                    priceCents = p.optLong("price_cents"),
                    shippingCents = p.optLong("shipping_cents"),
                    stock = if (p.isNull("stock")) null else p.optInt("stock"),
                    active = p.optInt("active", 1) != 0
                ))
            }
        }
    }

    private fun artistStorePost(context: Context, path: String, payload: JSONObject): Result<JSONObject> = runCatching {
        val savedToken = token(context) ?: error("Cal iniciar sessió.")
        jsonRequest("artist-store/$path", "POST", payload, savedToken)
    }

    fun saveArtistStoreProduct(
        context: Context, artistName: String, id: Long, name: String, productType: String,
        description: String, imageUrl: String?, priceCents: Long, shippingCents: Long, stock: Int?
    ): Result<JSONObject> = artistStorePost(context, "save-product.php", JSONObject()
        .put("artist", artistName).put("id", id).put("name", name).put("product_type", productType)
        .put("description", description).put("image_url", imageUrl).put("price_cents", priceCents)
        .put("shipping_cents", shippingCents).put("stock", stock).put("active", true))

    fun deleteArtistStoreProduct(context: Context, artistName: String, id: Long): Result<JSONObject> =
        artistStorePost(context, "delete-product.php", JSONObject().put("artist", artistName).put("id", id))

    fun createArtistStorePayment(
        context: Context, productId: Long, quantity: Int, recipientName: String, address: String,
        postalCode: String, city: String, country: String
    ): Result<ArtistStorePaymentStart> = runCatching {
        val savedToken = token(context) ?: error("Cal iniciar sessió.")
        val payload = JSONObject()
            .put("product_id", productId).put("quantity", quantity)
            .put("recipient_name", recipientName).put("shipping_address", address)
            .put("shipping_postal_code", postalCode).put("shipping_city", city).put("shipping_country", country)
        val response = jsonRequest("artist-store/payment-create.php", "POST", payload, savedToken)
        ArtistStorePaymentStart(
            orderId = response.getLong("order_id"),
            clientSecret = response.getString("client_secret"),
            publishableKey = response.getString("publishable_key"),
            amountCents = response.optLong("amount_cents"),
            currency = response.optString("currency", "eur"),
            merchantName = response.optString("merchant_name", "l'Agenda"),
            mode = response.optString("mode", "test")
        )
    }

    fun fetchArtistStorePaymentStatus(context: Context, orderId: Long): Result<ArtistStorePaidOrder> = runCatching {
        val savedToken = token(context) ?: error("Cal iniciar sessió.")
        val path = "artist-store/payment-status.php?order_id=" + URLEncoder.encode(orderId.toString(), StandardCharsets.UTF_8.name())
        val connection = open(path, "GET", savedToken)
        val root = JSONObject(readResponse(connection))
        if (!root.optBoolean("ok", false)) error(root.optString("message", "No s'ha pogut verificar la compra."))
        ArtistStorePaidOrder(
            orderId = root.optLong("order_id", orderId),
            status = root.optString("status"),
            productName = root.optString("product_name"),
            quantity = root.optInt("quantity", 1),
            productTotalCents = root.optLong("product_total_cents"),
            shippingCents = root.optLong("shipping_cents"),
            totalCents = root.optLong("total_cents")
        )
    }

    data class ArtistStoreOrder(
        val id: Long, val productName: String, val quantity: Int, val totalCents: Long,
        val status: String, val recipientName: String, val shippingAddress: String,
        val postalCode: String, val city: String, val country: String, val createdAt: String
    )

    fun fetchArtistStoreOrders(context: Context, artistName: String): Result<List<ArtistStoreOrder>> = runCatching {
        val savedToken = token(context) ?: error("Cal iniciar sessió.")
        val path = "artist-store/manage/orders.php?artist=" + URLEncoder.encode(artistName, StandardCharsets.UTF_8.name())
        val connection = open(path, "GET", savedToken)
        val root = JSONObject(readResponse(connection))
        if (!root.optBoolean("ok", false)) error(root.optString("message", "No s'han pogut carregar les comandes."))
        val arr = root.optJSONArray("orders") ?: JSONArray()
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                add(ArtistStoreOrder(
                    id=o.optLong("id"), productName=o.optString("product_name"), quantity=o.optInt("quantity",1),
                    totalCents=o.optLong("total_cents"), status=o.optString("status"), recipientName=o.optString("recipient_name"),
                    shippingAddress=o.optString("shipping_address"), postalCode=o.optString("shipping_postal_code"),
                    city=o.optString("shipping_city"), country=o.optString("shipping_country"), createdAt=o.optString("created_at")
                ))
            }
        }
    }

    fun markArtistStoreOrderShipped(context: Context, artistName: String, orderId: Long): Result<JSONObject> =
        artistStorePost(context, "manage/mark-shipped.php", JSONObject().put("artist", artistName).put("order_id", orderId))

    data class ArtistDonationConfig(
        val active: Boolean, val title: String, val description: String, val imageUrl: String?,
        val suggestedAmountsCents: List<Long>, val minAmountCents: Long, val maxAmountCents: Long
    )

    data class ArtistDonationPaymentStart(
        val donationId: Long, val clientSecret: String, val publishableKey: String,
        val amountCents: Long, val currency: String, val merchantName: String, val mode: String
    )

    data class ArtistDonationStatus(val donationId: Long, val status: String, val amountCents: Long)

    fun fetchArtistDonationConfig(context: Context, artistName: String): Result<ArtistDonationConfig> = runCatching {
        val path = "artist-store/donations/config.php?artist=" + URLEncoder.encode(artistName, StandardCharsets.UTF_8.name())
        val connection = open(path, "GET", token(context))
        val root = JSONObject(readResponse(connection))
        if (!root.optBoolean("ok", false)) error(root.optString("message", "No s'han pogut carregar les donacions."))
        val c = root.getJSONObject("config")
        val a = c.optJSONArray("suggested_amounts_cents") ?: JSONArray()
        val amounts = buildList { for (i in 0 until a.length()) add(a.optLong(i)) }.filter { it > 0 }
        ArtistDonationConfig(c.optBoolean("active", false), c.optString("title", "Dona suport al grup"), c.optString("description"),
            c.optString("image_url").takeIf(String::isNotBlank), amounts, c.optLong("min_amount_cents", 100), c.optLong("max_amount_cents", 100000))
    }

    fun saveArtistDonationConfig(context: Context, artistName: String, config: ArtistDonationConfig): Result<JSONObject> =
        artistStorePost(context, "donations/save-config.php", JSONObject().put("artist", artistName).put("active", config.active)
            .put("title", config.title).put("description", config.description).put("image_url", config.imageUrl)
            .put("suggested_amounts_cents", JSONArray(config.suggestedAmountsCents)).put("min_amount_cents", config.minAmountCents)
            .put("max_amount_cents", config.maxAmountCents))

    fun createArtistDonationPayment(context: Context, artistName: String, amountCents: Long, donorName: String, message: String, anonymous: Boolean = false): Result<ArtistDonationPaymentStart> = runCatching {
        val savedToken = token(context) ?: error("Cal iniciar sessió.")
        val response = jsonRequest("artist-store/donations/payment-create.php", "POST", JSONObject().put("artist", artistName)
            .put("amount_cents", amountCents).put("donor_name", donorName).put("message", message).put("anonymous", anonymous), savedToken)
        ArtistDonationPaymentStart(response.getLong("donation_id"), response.getString("client_secret"), response.getString("publishable_key"),
            response.optLong("amount_cents"), response.optString("currency", "eur"), response.optString("merchant_name", "l'Agenda"), response.optString("mode", "test"))
    }

    fun fetchArtistDonationStatus(context: Context, donationId: Long): Result<ArtistDonationStatus> = runCatching {
        val savedToken = token(context) ?: error("Cal iniciar sessió.")
        val path = "artist-store/donations/payment-status.php?donation_id=" + URLEncoder.encode(donationId.toString(), StandardCharsets.UTF_8.name())
        val connection = open(path, "GET", savedToken)
        val root = JSONObject(readResponse(connection))
        if (!root.optBoolean("ok", false)) error(root.optString("message", "No s'ha pogut verificar la donació."))
        ArtistDonationStatus(root.optLong("donation_id", donationId), root.optString("status"), root.optLong("amount_cents"))
    }

}
