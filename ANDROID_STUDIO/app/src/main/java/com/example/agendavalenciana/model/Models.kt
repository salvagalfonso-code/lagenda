package com.example.agendavalenciana.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Contracte funcional: un festival AUTO necessita almenys tres jornades reals. */
private const val MIN_FESTIVAL_DAYS = 3

enum class AccountRole {
    USER,
    GROUP,
    AGENCY,
    LABEL,
    VENUE,
    PROMOTER,
    SUPER_ADMIN
}

data class AppAccount(
    val id: Int,
    val username: String,
    val password: String,
    val displayName: String,
    val role: AccountRole,
    val managedArtists: Set<String> = emptySet(),
    val managedVenues: Set<String> = emptySet(),
    /** ID del compte en MySQL. Null només per als comptes demo/local antics. */
    val serverUserId: Int? = null,
    val email: String? = null
) {
    fun canManageArtist(artist: String): Boolean = when (role) {
        AccountRole.SUPER_ADMIN -> true
        AccountRole.GROUP -> artist in managedArtists
        AccountRole.AGENCY, AccountRole.LABEL, AccountRole.PROMOTER -> artist in managedArtists
        AccountRole.VENUE, AccountRole.USER -> false
    }

    fun canManageVenue(venue: String): Boolean =
        role == AccountRole.SUPER_ADMIN || (role == AccountRole.VENUE && venue in managedVenues)

    val roleLabel: String
        get() = when (role) {
            AccountRole.USER -> "Usuari"
            AccountRole.GROUP -> "Grup"
            AccountRole.AGENCY -> "Agència"
            AccountRole.LABEL -> "Segell"
            AccountRole.VENUE -> "Sala / espai"
            AccountRole.PROMOTER -> "Promotora"
            AccountRole.SUPER_ADMIN -> "Super Admin"
        }
}

data class AccountProfile(
    val username: String,
    val displayName: String,
    val bio: String = "",
    val photoUri: String? = null,
    /** Població principal del compte. Separada dels pobles d'interés del feed. */
    val homeTown: String = "",
    /** Disciplina professional explícita. Buit = inferir de les activitats publicades. */
    val discipline: String = "",
    val companyName: String = "",
    val castText: String = "",
    val publisherName: String = "",
    val booksText: String = "",
    val worksText: String = "",
    val spacesText: String = ""
)

enum class CulturalCategory(val label: String) {
    MUSIC("Música"),
    THEATRE("Teatre"),
    CINEMA("Cinema"),
    TALKS("Xarrades"),
    DANCE("Dansa"),
    EXHIBITIONS("Exposicions"),
    LITERATURE("Literatura"),
    WORKSHOPS("Tallers"),
    CHILDREN("Infantil"),
    POPULAR_CULTURE("Cultura popular"),
    HERITAGE("Patrimoni"),
    OTHER("Altres")
}

data class TicketOption(
    val id: Long,
    val name: String,
    val sessionDate: String = "",
    val sessionTime: String = "",
    val sector: String = "",
    val offer: String = "",
    val price: Int = 0,
    val capacity: Int? = null,
    val sold: Int = 0,
    val reserved: Int = 0,
    val maxPerOrder: Int = 6,
    val saleStartsAt: String = "",
    val saleEndsAt: String = "",
    val active: Boolean = true
) {
    val remaining: Int? get() = capacity?.let { (it - sold - reserved).coerceAtLeast(0) }
    val isSoldOut: Boolean get() = remaining == 0
}

data class EventDailyStart(
    val date: String = "",
    val time: String = "",
    val label: String = "Inici"
)

data class EventTransportInfo(
    val type: String = "",
    val text: String = ""
)

data class EventInfo(
    val dailyStart: List<EventDailyStart> = emptyList(),
    val address: String = "",
    val mapsUrl: String = "",
    val access: String = "",
    val transport: List<EventTransportInfo> = emptyList(),
    val pmrStatus: String = "UNKNOWN",
    val pmrText: String = "",
    val notices: List<String> = emptyList()
)

data class Concert(
    val id: Int,
    val artist: String,
    /** Títol públic de l'esdeveniment. En concerts simples pot coincidir amb l'artista. */
    val title: String = artist,
    /** Subtítol opcional. En el card plegat ocupa una sola línia sota el títol. */
    val subtitle: String = "",
    /** Artistes vinculats. Permet que festivals i concerts compartits apareguen en cada perfil. */
    val lineup: List<String> = listOf(artist),
    /** Cartell pujat per l'organització. Té sempre prioritat visual. */
    val posterUri: String? = null,
    /** Imatges base centralitzades en l'API per a fallbacks de la card. */
    val artistImageUri: String? = null,
    val venueImageUri: String? = null,
    val townImageUri: String? = null,
    /** Fallbacks administrables des del catàleg central d'imatges. */
    val categoryImageUri: String? = null,
    val organizerImageUri: String? = null,
    /** Organitzadors professionals vinculats a l'esdeveniment. */
    val organizers: List<String> = emptyList(),
    /** Configuració visual explícita de la card. AUTO conserva la jerarquia habitual. */
    val imageSource: String = "AUTO",
    val imageSourceEntityName: String? = null,
    val eventCardImageUri: String? = null,
    val preferredImageUri: String? = null,
    /** URL que un client ja ha comprovat que carrega. És informativa/cache; no altera la jerarquia visual. */
    val resolvedCardImageUri: String? = null,
    val resolvedCardImageSource: String? = null,
    val resolvedCardImageStatus: String = "PENDING",
    /** Enquadrament persistent de la imatge de la card. 0.5/0.5 = centre; 1f = zoom normal. */
    val imageFocusX: Float = 0.5f,
    val imageFocusY: Float = 0.5f,
    val imageScale: Float = 1f,
    val date: String,
    val time: String,
    /** Data/hora final. Buit = esdeveniment d'un sol dia. */
    val endDate: String = "",
    val endTime: String = "",
    /** Data concreta amb què esta aparició es mostra al feed; no crea un altre esdeveniment. */
    val feedOccurrenceDate: String? = null,
    val town: String,
    val comarca: String,
    val venue: String,
    val styles: List<String>,
    val price: Int,
    val language: String,
    val description: String,
    val category: CulturalCategory = CulturalCategory.MUSIC,
    val ticketPrice: Int = -1,
    val ticketAvailable: Boolean = false,
    /** Enllaç extern de venda o reserva d'entrades. */
    val ticketUrl: String = "",
    /** Aforament general, si l’organització el configura. */
    val capacity: Int? = null,
    /** Modalitats/sessions/sectors/tarifes disponibles per a este esdeveniment. */
    val ticketOptions: List<TicketOption> = emptyList(),
    val sponsored: Boolean = false,
    val sponsorLabel: String? = null,
    /** Nombre global de persones que han guardat l'esdeveniment al servidor. */
    val savedCount: Int = 0,
    /** Nombre global de persones que han marcat M'agrada al servidor. */
    val likeCount: Int = 0,
    /** Usuari que va crear l'esdeveniment al servidor. */
    val creatorUserId: Int? = null,
    /** Permís explícit retornat pel servidor quan existisca. */
    val canEditByCurrentUser: Boolean = false,
    /** Data real de creació/publicació al servidor. 0 si la font antiga no la facilita. */
    val createdAtMillis: Long = 0L,
    /** Idiomes desglossats de l'esdeveniment i dels artistes del cartell. */
    val languages: List<String> = emptyList(),
    /** Usuaris professionals que poden coeditar esta fitxa, a més de l'artista i Super Admin. */
    val coEditorUsernames: Set<String> = emptySet(),
    /** UID virtual de la instància de feed. Mai s'utilitza per likes, guardats, comunitat o entrades. */
    val feedInstanceUid: String? = null,
    /** Data que s'ha de preseleccionar en obrir un festival des d'una instància diària. */
    val selectedDate: String? = null,
    /** Dies reals amb programació. Si existeixen, tenen prioritat sobre el rang start/end per crear instàncies del feed. */
    val scheduleDates: List<String> = emptyList(),
    /** Resum curt de la programació de cada dia, preparat pel servidor per a la card diària del feed. */
    val scheduleDaySummaries: Map<String, String> = emptyMap(),
    /** Primera hora programada de cada jornada. S'usa en la card diària sense alterar l'esdeveniment real. */
    val scheduleDayStartTimes: Map<String, String> = emptyMap(),
    /** Preu propi de cada jornada: 0 = gratuït. Evita mostrar l'abonament global en un dia gratuït. */
    val scheduleDayTicketPrices: Map<String, Int> = emptyMap(),
    /** Senyal explícit retornat pel servidor per a festivals/cicles/multidia. */
    val isMultidayFromServer: Boolean = false,
    /** Classificació estructural del servidor: AUTO, STANDARD, FESTIVAL, PROGRAMME o CYCLE. */
    val eventStructure: String = "AUTO",
    /** Estat de la programació: NONE, MISSING, PARTIAL o COMPLETE. */
    val scheduleStatus: String = "NONE",
    /** Informació universal: horaris per jornada, accés, transport, PMR i avisos. */
    val eventInfo: EventInfo = EventInfo(),
    /** Etiquetes explícites de l’API; diferents d’estils artístics. */
    val subcategoryCodes: List<String> = emptyList(),
    /** Públic infantil transversal. */
    val isInfantil: Boolean = false,
    /** Identificador estable dels Sheets: s’utilitza per a la classificació, no per a tickets. */
    val sourceFeedUid: String = ""
) {
    /**
     * Artistes reals vinculats a l'esdeveniment. En feeds antics, quan no hi havia
     * artista, alguns parsers utilitzaven el títol com a fallback; això no és una
     * prova que siga una actuació d'un únic artista.
     */
    private val distinctFestivalArtists: List<String> get() =
        lineup.asSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .filterNot { it.equals(title.trim(), ignoreCase = true) }
            .distinctBy { it.lowercase() }
            .toList()

    /** Nombre d’artistes diferents fiables vinculats a l’esdeveniment. */
    private val distinctArtistCount: Int get() = distinctFestivalArtists.size

    /**
     * Nombre de jornades lògiques. Si el servidor envia scheduleDates, eixes dates manen:
     * una actuació a la 01:00 pot continuar pertanyent a la jornada anterior.
     * Si no hi ha scheduleDates, usem el rang start/end com a fallback.
     */
    private val festivalDayCount: Int get() {
        val logicalDays = scheduleDates.map(String::trim).filter(String::isNotBlank).distinct()
        if (logicalDays.isNotEmpty()) return logicalDays.size

        val start = runCatching { LocalDate.parse(date.take(10)) }.getOrNull()
        val end = runCatching { LocalDate.parse(endDate.take(10)) }.getOrNull()
        if (start != null && end != null && !end.isBefore(start)) {
            return ChronoUnit.DAYS.between(start, end).toInt() + 1
        }
        return if (isMultidayFromServer) 2 else 1
    }

    /**
     * Nombre de programes diaris diferents. El servidor usa estos resums per indicar
     * què passa realment en cada jornada, per tant són una prova millor que el simple
     * rang start/end.
     */
    private val distinctDayProgrammeCount: Int get() =
        scheduleDaySummaries.values
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinctBy { it.lowercase() }
            .size

    /**
     * Un únic camp venue pot contindre diversos recintes quan el festival és itinerant
     * o reparteix la programació entre espais. Açò evita aplicar l'excepció de
     * «mateix artista + mateix lloc» quan el lloc no és realment únic.
     */
    private val hasMultipleFestivalVenues: Boolean get() {
        val raw = venue.trim().lowercase()
        if (raw.isBlank()) return false
        return raw.contains(" · ") || raw.contains(" / ") || raw.contains(";") ||
            raw.contains("diversos espais") || raw.contains("diverses ubicacions") ||
            raw.contains("varios espacios") || raw.contains("varias ubicaciones")
    }

    /**
     * Excepció acordada: una mateixa actuació o artista repetit 3+ dies al mateix lloc
     * no es converteix en festival només per duració.
     */
    private val isRepeatedSingleProgrammeAcrossDays: Boolean get() {
        if (festivalDayCount < MIN_FESTIVAL_DAYS) return false
        if (hasMultipleFestivalVenues) return false
        if (distinctArtistCount > 1) return false
        if (distinctDayProgrammeCount > 1) return false
        return distinctArtistCount == 1 || distinctDayProgrammeCount == 1
    }

    /**
     * Fallback només per a instal·lacions/cache antigues que encara no han rebut
     * event_structure. No identifica marques concretes: descriu formats culturals
     * habitualment multijornada.
     */
    private val titleLooksLikeFestivalContainer: Boolean get() =
        Regex("\\b(festival|fira|feria|fest|fesporrat|cicle|ciclo|trobada|jornades?|mostra|certamen|aplec)\\b", RegexOption.IGNORE_CASE)
            .containsMatchIn(title)

    /**
     * Regla general de FESTIVAL acordada per a l'Agenda:
     * - una classificació explícita FESTIVAL/PROGRAMME/CYCLE del servidor preval;
     * - en AUTO, calen 3 o més jornades reals d'activitat;
     * - les exposicions no entren en este popup;
     * - la mateixa actuació/artista repetida al mateix lloc tampoc;
     * - no hi ha excepcions per ID ni per nom de festival.
     */
    val isFestival: Boolean get() {
        if (category == CulturalCategory.EXHIBITIONS) return false

        return when (eventStructure.trim().uppercase()) {
            "FESTIVAL", "PROGRAMME", "CYCLE" -> true
            "STANDARD" -> false
            else -> {
                if (festivalDayCount < MIN_FESTIVAL_DAYS) return false
                if (isRepeatedSingleProgrammeAcrossDays) return false

                val hasExplicitSchedule = scheduleDates
                    .map(String::trim)
                    .filter(String::isNotBlank)
                    .distinct()
                    .size >= MIN_FESTIVAL_DAYS
                val scheduleIsKnown = scheduleStatus.trim().uppercase() in setOf("PARTIAL", "COMPLETE")

                when {
                    distinctArtistCount > 1 -> true
                    distinctDayProgrammeCount > 1 -> true
                    distinctArtistCount == 1 && hasMultipleFestivalVenues && hasExplicitSchedule -> true
                    hasExplicitSchedule && scheduleIsKnown && distinctArtistCount == 0 -> true
                    distinctArtistCount == 0 && titleLooksLikeFestivalContainer -> true
                    else -> false
                }
            }
        }
    }
    val isFree: Boolean get() = ticketPrice == 0
    val feedDate: String get() = feedOccurrenceDate?.takeIf(String::isNotBlank) ?: selectedDate?.takeIf(String::isNotBlank) ?: date
    val feedInstanceKey: String get() = feedInstanceUid?.takeIf(String::isNotBlank) ?: "event_${id}_${feedDate}"
    val isMultiday: Boolean get() {
        if (isMultidayFromServer) return true
        if (scheduleDates.map(String::trim).filter(String::isNotBlank).distinct().size > 1) return true
        val start = date.take(10)
        val end = endDate.take(10)
        return start.isNotBlank() && end.isNotBlank() && start != end
    }

    /**
     * Experiència visual de festival/programació segons la classificació estructural
     * retornada pel servidor. No depén del nombre d'artistes ni del rang de dates.
     */
    val isFestivalExperience: Boolean get() = isFestival

    /**
     * Jerarquia visual definitiva de l'Agenda:
     * 1) cartell propi de l'esdeveniment
     * 2) grup/artista
     * 3) sala/recinte
     * 4) poble
     *
     * Important: ni una preferència manual antiga ni una URL "resolved" cachejada poden
     * avançar-se al cartell. Si s'afegeix un cartell després, passa automàticament a ser
     * la imatge principal. Organitzador i categoria ja no formen part del fallback del feed.
     */
    fun imageCandidates(artistProfileUri: String? = null): List<String> =
        listOfNotNull(
            posterUri,
            eventCardImageUri,
            artistImageUri,
            artistProfileUri,
            venueImageUri,
            townImageUri
        )
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()

    fun resolvedImageUri(artistProfileUri: String? = null): String? =
        imageCandidates(artistProfileUri).firstOrNull()
}



enum class EventSubmissionStatus(val label: String) {
    DRAFT("Esborrany"),
    PENDING_REVIEW("Pendent de revisió"),
    PUBLISHED("Publicat")
}

enum class TicketStatus(val label: String) {
    ACTIVE("Activa"),
    USED("Utilitzada"),
    CANCELLED("Anul·lada")
}

/** Entrada individual. Les noves entrades utilitzen un token criptogràfic de 26 caràcters. */
data class PurchasedTicket(
    val code: String,
    val concertId: Int,
    val buyerUsername: String,
    val buyerDisplayName: String = buyerUsername,
    val status: TicketStatus = TicketStatus.ACTIVE,
    val purchasedAtMillis: Long = System.currentTimeMillis(),
    val usedAtMillis: Long? = null,
    val serverId: Long? = null,
    val ticketOptionName: String = "",
    val sessionDate: String = "",
    val sessionTime: String = "",
    val sector: String = "",
    val offer: String = ""
)

data class EventSubmission(
    val id: Int,
    val ownerUsername: String,
    val posterUri: String?,
    val title: String,
    val subtitle: String = "",
    val artists: List<String>,
    val date: String,
    val time: String,
    val endDate: String = "",
    val endTime: String = "",
    val town: String,
    val venue: String,
    val category: CulturalCategory,
    val styles: List<String> = emptyList(),
    val language: String = "Valencià",
    val price: Int = 0,
    val ticketSalesEnabled: Boolean = false,
    val ticketUrl: String = "",
    val description: String = "",
    val recognizedText: String = "",
    val status: EventSubmissionStatus = EventSubmissionStatus.DRAFT,
    val createdAtMillis: Long = System.currentTimeMillis()
)

enum class AdPlacement(val label: String) {
    FEED_NORMAL("Feed normal"),
    FEED_PREMIUM("Feed Premium"),
    HEADER("Capçalera"),
    POPUP("Popup")
}

/** Anunci temporal vinculat sempre a un esdeveniment ja publicat. */
data class EventAdvertisement(
    val id: Int,
    val ownerUsername: String,
    val concertId: Int,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val startsAtMillis: Long = System.currentTimeMillis(),
    val durationHours: Int = 24,
    val imageUri: String? = null,
    /** Punt focal normalitzat (0..1) per al retall de la imatge en cards publicitàries. */
    val cardFocusX: Float = 0.5f,
    val cardFocusY: Float = 0.5f,
    val targetStyles: Set<String> = emptySet(),
    val targetSearchTerms: Set<String> = emptySet(),
    val targetTowns: Set<String> = emptySet(),
    val targetComarques: Set<String> = emptySet(),
    val targetFollowers: Boolean = false,
    val targetHighIntent: Boolean = false,
    val matchAllCriteria: Boolean = false,
    val placement: AdPlacement = AdPlacement.FEED_NORMAL,
    val amountCents: Int = 0,
    val status: String = "active",
    // Data exacta de caducitat retornada per MySQL, sense arrodoniment d'hores.
    val serverEndsAtMillis: Long? = null
) {
    val endsAtMillis: Long get() = serverEndsAtMillis ?: (startsAtMillis + durationHours * 60L * 60L * 1000L)
    fun isActive(nowMillis: Long = System.currentTimeMillis()): Boolean =
        status.equals("active", ignoreCase = true) && nowMillis in startsAtMillis until endsAtMillis
}


enum class BehaviourKind {
    SEARCH,
    TOWN,
    COMARCA,
    ARTIST,
    FOLLOWED_ARTIST,
    FOLLOWED_ORGANIZER,
    FOLLOWED_CATEGORY,
    ORGANIZER,
    VENUE,
    STYLE,
    LANGUAGE,
    CATEGORY,
    MAX_DISTANCE,
    MAX_PRICE,
    FREE_ONLY,
    EVENT_VIEW,
    SAVED_EVENT,
    WANT_TO_GO,
    GOING
}

/** Registre intern d'interés. Es guarda separat per usuari. */
data class UserBehaviourEvent(
    val kind: BehaviourKind,
    val value: String,
    val timestampMillis: Long = System.currentTimeMillis()
)

/** Configuració habitual que reutilitza el creador d'anuncis. */
data class AdTargetingDefaults(
    val durationHours: Int = 24,
    val targetStyles: Set<String> = emptySet(),
    val targetSearchTerms: Set<String> = emptySet(),
    val targetTowns: Set<String> = emptySet(),
    val targetComarques: Set<String> = emptySet(),
    val targetFollowers: Boolean = false,
    val matchAllCriteria: Boolean = false
)

data class UserBehaviourProfile(
    val username: String,
    val events: List<UserBehaviourEvent> = emptyList(),
    val adDefaults: AdTargetingDefaults = AdTargetingDefaults(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

enum class FeedSearchSuggestionType(val label: String) {
    ARTIST("Grup / artista"),
    MUNICIPALITY("Poble"),
    COMARCA("Comarca"),
    COMPANY("Empresa / professional"),
    VENUE("Sala / espai"),
    STYLE("Estil")
}

data class FeedSearchSuggestion(
    val type: FeedSearchSuggestionType,
    val value: String,
    val subtitle: String = ""
)


enum class FollowSuggestionType {
    ARTIST,
    ORGANIZER,
    CATEGORY
}

data class FollowSuggestion(
    val type: FollowSuggestionType,
    val key: String,
    val label: String,
    val subtitle: String,
    val score: Int
)

enum class FeedMode(val label: String) {
    FOR_YOU("Per a tu"),
    FOLLOWED("Seguits"),
    CATEGORY("Categoria"),
    UNFILTERED("Sense filtre")
}

enum class ConcertViewMode(val label: String) {
    LIST("Llista"),
    VISUAL("Visual"),
    MAP("Mapa")
}

enum class AttendanceStatus {
    NONE,
    WANT_TO_GO,
    GOING
}

enum class CommunityGroupType(val label: String, val emoji: String) {
    CAR("Cotxe compartit", "🚗"),
    TRAIN("Tren", "🚆"),
    TRANSPORT("Transport organitzat", "🚌"),
    HOSTING("Allotjament compartit", "🛏️"),
    NEED_HOSTING("Busque allotjament", "🏠"),
    COMPANY("Busque gent per anar", "👥"),
    MEETUP("Quedada", "📍"),
    WOMEN_ONLY("Sols xiques", "♀️"),
    VEG_FRIENDLY("Vegans / vegetarians", "🌱"),
    LGBTQ_FRIENDLY("LGTBIQ+ friendly", "🏳️‍🌈"),
    OTHER("Altres", "💬")
}

enum class CommunityAccessMode(val label: String) {
    OPEN("Grup obert"),
    APPROVAL("Amb aprovació")
}

data class CommunityGroup(
    val id: Int,
    val concertId: Int,
    val type: CommunityGroupType,
    val creator: String,
    val town: String,
    val title: String,
    val description: String,
    val capacity: Int? = null,
    val members: Set<String> = emptySet(),
    val creatorUserId: Int? = null,
    val eventTitle: String = "",
    val accessMode: CommunityAccessMode = CommunityAccessMode.APPROVAL,
    val meetingAt: String = "",
    val status: String = "OPEN",
    val participantCount: Int = 0,
    val pendingCount: Int = 0,
    val myStatus: String = "NONE",
    val myRole: String = "",
    val myMessageCount: Int = 0,
    /** Un tauló pot funcionar sense xat públic, amb consultes privades al creador. */
    val chatEnabled: Boolean = true
)

data class CommunityBoardInboxMessage(
    val id: Int,
    val groupId: Int,
    val eventId: Int,
    val title: String,
    val author: String,
    val authorUserId: Int,
    val body: String,
    val createdAt: String
)

data class CommunityBoardInquiry(
    val id: Int,
    val groupId: Int,
    val peerUserId: Int,
    val peerName: String,
    val author: String,
    val body: String,
    val createdAt: String,
    val own: Boolean
)

data class CommunityMemberRequest(
    val userId: Int,
    val displayName: String,
    val photoUri: String? = null,
    val requestedAt: String = ""
)

data class CommunityIdentity(
    val displayName: String,
    val tastes: List<String> = emptyList(),
    val photoUri: String? = null,
    val official: Boolean = false
)

data class ChatMessage(
    val id: Int,
    val concertId: Int,
    val author: String,
    val text: String,
    val official: Boolean = false,
    val timestamp: String = "ara"
)

data class ConcertComment(
    val id: Int,
    val concertId: Int,
    val author: String,
    val text: String,
    val timestamp: String = "ara"
)

data class CommunityChatMessage(
    val id: Int,
    val groupId: Int,
    val author: String,
    val text: String,
    val timestamp: String = "ara",
    val authorUserId: Int? = null,
    val photoUri: String? = null,
    // Data original del servidor; necessària per intercalar xats en General.
    val createdAt: String = ""
)

data class Story(
    val id: Int,
    val artist: String,
    val initials: String,
    val title: String,
    val body: String,
    val publishedAtMillis: Long = System.currentTimeMillis(),
    val eventId: Int? = null,
    val eventTitle: String = "",
    val eventTown: String = "",
    val eventDate: String = "",
    val eventTime: String = "",
    val creatorUserId: Int? = null,
    val authorUsername: String = "",
    val organizer: Boolean = false,
    val canEdit: Boolean = false,
    /** AUTO_EVENT per a novetats generades des d'un esdeveniment; MANUAL per a batecs publicats. */
    val sourceType: String = "MANUAL",
    /** Identificador estable guardat al servidor per marcar el batec com vist. */
    val storyKey: String = "",
    /** Els vistos continuen visibles amb cercle gris fins a una publicació nova. */
    val seen: Boolean = false,
    /** Puntuació interna usada només per ordenar novetats automàtiques no vistes. */
    val relevance: Int = 0
)

data class GroupPost(
    val id: Int,
    val artist: String,
    val title: String,
    val body: String
)

data class ArtistPublicProfile(
    val artist: String,
    val bio: String,
    val musicDescription: String,
    val hometown: String,
    val linksText: String = ""
)

data class ArtistAnalytics(
    val artist: String,
    val saved: Int,
    val likes: Int,
    val wantToGo: Int,
    val going: Int,
    val conversations: Int,
    val topTowns: List<Pair<String, Int>>,
    val sharedStyles: List<Pair<String, Int>>
)


data class Filters(
    val categories: Set<CulturalCategory> = emptySet(),
    val dateFrom: String? = null,
    val dateTo: String? = null,
    val month: String? = null,
    val comarca: String? = null,
    val town: String? = null,
    val maxDistanceKm: Int? = null,
    val maxPrice: Int? = null,
    val freeOnly: Boolean = false
) {
    fun isEmpty(): Boolean =
        categories.isEmpty() &&
            dateFrom == null &&
            dateTo == null &&
            month == null &&
            comarca == null &&
            town == null &&
            maxDistanceKm == null &&
            maxPrice == null &&
            !freeOnly
}

/** Configuració de cerca guardada per compte al dispositiu. */
data class SavedFilterPreset(val name: String, val filters: Filters)

data class UserPreferences(
    val preferredLocations: Set<String> = emptySet(),
    val preferredComarques: Set<String> = emptySet(),
    val favoriteArtists: Set<String> = emptySet(),
    val valencianFirst: Boolean = true,
    val showSuggestions: Boolean = true,
    val locationRadiiKm: Map<String, Int> = emptyMap(),
    val discoveryLevel: DiscoveryLevel = DiscoveryLevel.BALANCED
) {
    fun radiusForLocation(town: String): Int = PersonalizationOptions.validRadius(
        locationRadiiKm[town] ?: locationRadiiKm.entries.firstOrNull {
            it.key.equals(town, ignoreCase = true)
        }?.value
    )
}

enum class NotificationType(val label: String, val emoji: String) {
    COMMUNITY("Comunitat", "💬"),
    NEW_CONCERT("Concerts nous", "🎵"),
    REMINDER("Recordatoris", "⏰"),
    TICKETS("Entrades", "🎟"),
    CHANGES("Canvis", "⚠️"),
    // Només per a la vista General: les suggerències no incrementen la campaneta.
    SUGGESTION("Suggerències", "✨")
}

data class AgendaNotification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val concertId: Int? = null,
    val sortKey: Long = 0L,
    // Per obrir directament la conversa del grup des de la campaneta.
    val friendsGroupId: Int? = null,
    // Xat de Comunitat vinculat a un esdeveniment (Cotxe, Transport, Quedades...).
    val communityGroupId: Int? = null
)

enum class BellFollowedFrequency(val label: String) {
    IMMEDIATE("Al moment"),
    DAILY("Diari"),
    WEEKLY("Setmanal")
}

data class AlertPreferences(
    val communityMessages: Boolean = true,
    val followedArtist: Boolean = true,
    val homeTown: Boolean = true,
    val homeComarca: Boolean = true,
    val favoriteStyle: Boolean = false,
    val dayBefore: Boolean = true,
    val ticketUpdates: Boolean = true,
    val changes: Boolean = true,
    val suggestions: Boolean = true,
    val followedFrequency: BellFollowedFrequency = BellFollowedFrequency.WEEKLY
)

enum class EmailDigestFrequency(val wireValue: String, val label: String) {
    NONE("NONE", "Desactivats"),
    DAILY("DAILY", "Diari"),
    WEEKLY("WEEKLY", "Setmanal");

    companion object {
        fun fromWire(value: String?): EmailDigestFrequency =
            entries.firstOrNull { it.wireValue.equals(value?.trim(), ignoreCase = true) } ?: NONE
    }
}

data class EmailPreferences(
    val frequency: EmailDigestFrequency = EmailDigestFrequency.NONE,
    val recommendations: Boolean = false,
    val followedArtists: Boolean = false,
    val savedReminders: Boolean = false,
    val personalizedAds: Boolean = false
) {
    val hasSelectedContent: Boolean
        get() = recommendations || followedArtists || savedReminders || personalizedAds
}
