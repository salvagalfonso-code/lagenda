package com.example.agendavalenciana.ios

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// This is the *actual* payload of api/events/index.php (not invented local demo data).
data class AgendaScheduleEntry(val date: String, val time: String, val title: String, val artist: String, val location: String)
data class AgendaEventSummary(
    val id: Int, val title: String, val date: String, val time: String,
    val town: String, val venue: String, val category: String, val description: String,
    val posterUrl: String, val subtitle: String = "", val artists: List<String> = emptyList(),
    val organizers: List<String> = emptyList(), val styles: List<String> = emptyList(),
    val endDate: String = "", val ticketAvailable: Boolean = false, val ticketUrl: String = "",
    val price: String = "", val schedule: List<AgendaScheduleEntry> = emptyList(),
    val language: String = "", val updatedAt: String = "", val eventStructure: String = "",
    val savedCount: Int = 0, val createdAt: String = ""
) {
    val key get() = "${id}_${date}"
    val isFestival get() = IosEventPopupPolicy.isFestival(this)
    val names get() = artists + organizers
    fun matches(q: String): Boolean {
        val term = q.trim()
        if (term.isEmpty()) return true
        // The artist may appear only in the festival's schedule, not in its top-level artist list.
        return (listOf(title, subtitle, town, venue, category) + names + styles +
            schedule.flatMap { listOf(it.artist, it.title) })
            .any { it.contains(term, ignoreCase = true) }
    }
    fun matchedSchedule(q: String): AgendaScheduleEntry? = schedule.firstOrNull {
        q.isNotBlank() && (it.artist.contains(q, true) || it.title.contains(q, true))
    }
}

internal fun JsonObject.text(vararg keys: String): String = keys.asSequence()
    .mapNotNull { this[it]?.let { field -> (field as? JsonPrimitive)?.contentOrNull } }
    .map { it.trim() }.firstOrNull { it.isNotBlank() && it != "null" }.orEmpty()
internal fun JsonObject.bool(key: String): Boolean = when (text(key).lowercase()) { "true", "1" -> true; else -> false }
internal fun JsonObject.items(key: String): List<JsonObject> = (this[key] as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()
internal fun JsonObject.names(key: String): List<String> {
    val a = this[key]
    return when (a) {
        is JsonArray -> a.mapNotNull { (it as? JsonObject)?.text("name", "artist_name", "display_name") ?: (it as? JsonPrimitive)?.contentOrNull }
            .filter(String::isNotBlank)
        is JsonPrimitive -> a.contentOrNull.orEmpty().split('|', ',', ';').map(String::trim).filter(String::isNotBlank)
        else -> emptyList()
    }
}
internal fun JsonObject.arr(key: String): JsonArray = this[key] as? JsonArray ?: JsonArray(emptyList())

object IosEventCatalog {
    private const val ENDPOINT = "https://quimerafest.com/app/api/events/index.php"
    private val client = HttpClient {
        install(HttpTimeout) { requestTimeoutMillis = 15_000; connectTimeoutMillis = 10_000 }
    }
    private const val CACHE_MS = 30L * 60L * 1000L
    private var saved: List<AgendaEventSummary> = emptyList()
    private var savedAtMs: Long = 0L
    private var restored = false
    private var previewOnly = false

    fun cached(): List<AgendaEventSummary> {
        if (!restored) {
            restored = true
            // A 50-event preview is enough to draw the first 5/20 cards immediately.
            // Full JSON is still retained and decoded off-main by get(), never discarded.
            val preview = platformReadPublicCache("events_preview_json")
            val raw = preview.ifBlank { platformReadPublicCache("events_json") }
            saved = runCatching { decode(raw) }.getOrDefault(emptyList())
            previewOnly = preview.isNotBlank() && saved.isNotEmpty()
            savedAtMs = platformReadPublicCache("events_at").toLongOrNull() ?: 0L
        }
        return saved
    }
    fun cacheAgeMs(): Long = (currentEpochMillis() - savedAtMs).coerceAtLeast(0L)
    /** True only after the preview has been replaced by the full local/remote catalog. */
    fun fullyHydrated(): Boolean { cached(); return !previewOnly }
    suspend fun get(force: Boolean = false): List<AgendaEventSummary> {
        val existing = cached()
        if (!force && existing.isNotEmpty() && cacheAgeMs() < CACHE_MS) {
            if (previewOnly) {
                val full = withContext(Dispatchers.Default) {
                    runCatching { decode(platformReadPublicCache("events_json")) }.getOrNull()
                }
                if (!full.isNullOrEmpty()) {
                    saved = full
                    previewOnly = false
                    return full
                }
                // A damaged full cache must not pin a partial catalog until the TTL expires.
            } else return existing
        }
        val response = client.get(ENDPOINT).bodyAsText()
        val root = withContext(Dispatchers.Default) {
            Json.parseToJsonElement(response) as? JsonObject ?: error("Resposta del catàleg invàlida")
        }
        require(root.bool("ok")) { "El servidor no ha retornat ok=true" }
        val objects = root.items("events")
        val parsed = withContext(Dispatchers.Default) { decodeObjects(objects) }
        require(parsed.isNotEmpty()) { "El servidor no ha retornat esdeveniments vàlids." }
        saved = parsed
        previewOnly = false
        savedAtMs = currentEpochMillis()
        platformWritePublicCache("events_json", response)
        // Precompute this during refresh, not during the next UI startup.
        val previewJson = JsonObject(mapOf(
            "ok" to JsonPrimitive(true),
            "events" to JsonArray(objects.take(50))
        ))
        platformWritePublicCache("events_preview_json", previewJson.toString())
        platformWritePublicCache("events_at", savedAtMs.toString())
        return parsed
    }
    fun decode(raw: String): List<AgendaEventSummary> {
        val root = Json.parseToJsonElement(raw) as? JsonObject ?: error("Resposta del catàleg invàlida")
        require(root.bool("ok")) { "El servidor no ha retornat ok=true" }
        return decodeObjects(root.items("events"))
    }
    /** Both events/index.php and ads/active-global.php return the same logical event
     * with different date field names. One decoder avoids silently dropping sponsored cards. */
    internal fun decodeObjects(objects: List<JsonObject>): List<AgendaEventSummary> {
        return objects.mapNotNull { obj ->
            val id = obj.text("event_id", "id").toIntOrNull() ?: return@mapNotNull null
            val title = obj.text("title")
            if (title.isBlank()) return@mapNotNull null
            AgendaEventSummary(
                id = id, title = title, date = obj.text("feed_instance_date", "selected_date", "event_date", "start_date", "date").take(10),
                time = obj.text("start_time", "event_time", "time").take(5), town = obj.text("town"), venue = obj.text("venue"),
                category = obj.text("categoria_principal_nova", "category"), description = obj.text("description").take(15000),
                posterUrl = validMediaUrl(obj.text("poster_url")), subtitle = obj.text("subtitle"),
                artists = obj.names("artists").ifEmpty {
                    obj.text("artist_name", "artist").takeIf(String::isNotBlank)?.let { listOf(it) }.orEmpty()
                },
                organizers = obj.names("organizers"), styles = obj.names("styles"), endDate = obj.text("end_date"),
                ticketAvailable = obj.bool("ticket_sales_enabled") || obj.bool("ticket_available") || obj.bool("ticket_enabled"),
                ticketUrl = obj.text("ticket_url"), price = obj.text("ticket_price", "price"),
                language = obj.text("language"), updatedAt = obj.text("updated_at"),
                eventStructure = obj.text("event_structure").uppercase(),
                savedCount = obj.text("saved_count").toIntOrNull()?.coerceAtLeast(0) ?: 0,
                createdAt = obj.text("created_at"),
                schedule = obj.items("schedule").map { s ->
                    AgendaScheduleEntry(s.text("date", "schedule_date", "start_at").take(10),
                        s.text("start_time", "time").ifBlank { s.text("start_at").drop(11).take(5) }.take(5),
                        s.text("title"), s.text("artist_name", "group_name"), s.text("stage", "location"))
                }
            )
        }.distinctBy { it.key }.sortedWith(compareBy<AgendaEventSummary> { it.date.ifBlank { "9999" } }.thenBy { it.time }.thenBy { it.id })
    }
}

internal fun validMediaUrl(raw: String): String = when {
    raw.startsWith("https://", true) -> raw
    raw.startsWith("http://", true) -> "" // don't embed insecure transport
    raw.startsWith("/") -> "https://quimerafest.com${raw}"
    raw.isNotBlank() && !raw.startsWith("data:", true) -> "https://quimerafest.com/app/${raw.trimStart('/')}"
    else -> ""
}

expect fun currentEpochMillis(): Long
expect fun currentLocalDateIso(): String
/** Dia de programació: la jornada acaba a les 05:00, no a mitjanit. */
expect fun currentCulturalDateIso(): String
/** Non-sensitive on-device UI/cache data only. Tokens NEVER go here. */
expect fun platformReadPublicCache(key: String): String
expect fun platformWritePublicCache(key: String, value: String)
expect fun platformOpenExternal(url: String)
expect suspend fun platformScheduleReminder(userId: Int, eventId: Int, title: String, isoDate: String): Boolean
expect fun platformCancelReminder(userId: Int, eventId: Int)

fun showIssuedTicketQr(code: String, title: String) {
    if (code.isNotBlank()) IosNativeBridge.current?.showTicketQr(code, title)
}
