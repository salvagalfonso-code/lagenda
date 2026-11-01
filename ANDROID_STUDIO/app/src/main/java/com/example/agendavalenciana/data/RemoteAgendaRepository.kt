package com.example.agendavalenciana.data

import android.content.Context
import android.net.Uri
import com.example.agendavalenciana.BuildConfig
import com.example.agendavalenciana.model.Concert
import com.example.agendavalenciana.model.CulturalCategory
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Font externa de l'agenda.
 *
 * El servidor publica un CSV i les imatges en URLs HTTPS. L'app conserva una còpia
 * local del CSV i dels cartells per continuar funcionant sense connexió.
 */
object RemoteAgendaRepository {
    private const val CACHE_DIR = "remote_agenda"
    private const val CSV_CACHE = "events.csv"
    private const val PREFS = "remote_agenda_sync"
    private const val CONNECT_TIMEOUT = 12_000
    private const val READ_TIMEOUT = 20_000

    data class SyncResult(
        val concerts: List<Concert>,
        val downloaded: Boolean,
        val syncedAtMillis: Long
    )

    fun configuredUrl(): String = BuildConfig.REMOTE_EVENTS_CSV_URL.trim()

    fun configuredBaseUrl(): String = BuildConfig.REMOTE_AGENDA_BASE_URL.trim().let {
        if (it.endsWith("/")) it else "$it/"
    }

    fun isConfigured(): Boolean =
        ServerApiRepository.isConfigured() || configuredUrl().startsWith("https://", ignoreCase = true)

    fun loadCached(context: Context): List<Concert> {
        val apiCached = ServerApiRepository.loadCachedEvents(context)
        if (apiCached.isNotEmpty()) return apiCached
        val cache = File(File(context.filesDir, CACHE_DIR), CSV_CACHE)
        if (!cache.exists()) return emptyList()
        return runCatching { parseCsv(cache.readText(StandardCharsets.UTF_8), context) }
            .getOrDefault(emptyList())
    }

    fun lastSyncMillis(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong("last_sync_millis", 0L)

    fun sync(context: Context, forceFullCatalog: Boolean = false): Result<SyncResult> {
        if (ServerApiRepository.isConfigured()) {
            // 10ep: amb cache buida no esperem els enriquiments del sync complet. El primer
            // feed torna tan prompte com arriba la resposta base; la persistència continua
            // en segon pla. En sincronitzacions posteriors conservem el camí incremental.
            val api = if (forceFullCatalog || AgendaLocalDatabase.hasEvents(context)) {
                ServerApiRepository.fetchEvents(context, forceFullCatalog = forceFullCatalog)
            } else {
                ServerApiRepository.fetchEventsFastFirstLoad(context)
            }
            if (api.isSuccess) {
                val now = System.currentTimeMillis()
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().putLong("last_sync_millis", now).apply()
                return Result.success(SyncResult(api.getOrThrow(), downloaded = true, syncedAtMillis = now))
            }
            // Durant la migració, el CSV continua sent un fallback offline/compatibilitat.
            if (!configuredUrl().startsWith("https://", ignoreCase = true)) {
                return Result.failure(api.exceptionOrNull() ?: IllegalStateException("No s'ha pogut consultar l'API."))
            }
        }
        return syncCsv(context)
    }

    private fun syncCsv(context: Context): Result<SyncResult> = runCatching {
        val csvUrl = configuredUrl()
        require(csvUrl.startsWith("https://", ignoreCase = true)) {
            "Configura AGENDA_EVENTS_CSV_URL amb una URL HTTPS del CSV extern."
        }

        val cacheDir = File(context.filesDir, CACHE_DIR).apply { mkdirs() }
        val csvCache = File(cacheDir, CSV_CACHE)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val oldEtag = prefs.getString("csv_etag", null)

        val connection = (URL(csvUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT
            readTimeout = READ_TIMEOUT
            requestMethod = "GET"
            setRequestProperty("Accept", "text/csv,text/plain;q=0.9,*/*;q=0.5")
            setRequestProperty("Cache-Control", "no-cache")
            if (!oldEtag.isNullOrBlank()) setRequestProperty("If-None-Match", oldEtag)
        }

        val downloaded: Boolean
        try {
            when (connection.responseCode) {
                HttpURLConnection.HTTP_NOT_MODIFIED -> {
                    require(csvCache.exists()) { "El servidor ha retornat 304 però no hi ha cache local." }
                    downloaded = false
                }
                in 200..299 -> {
                    val body = connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                    // Validem abans de substituir la còpia bona.
                    parseRows(body)
                    csvCache.writeText(body, StandardCharsets.UTF_8)
                    connection.getHeaderField("ETag")?.takeIf { it.isNotBlank() }?.let {
                        prefs.edit().putString("csv_etag", it).apply()
                    }
                    downloaded = true
                }
                else -> error("HTTP ${connection.responseCode} en actualitzar l'agenda")
            }
        } finally {
            connection.disconnect()
        }

        val concerts = parseCsv(csvCache.readText(StandardCharsets.UTF_8), context)
        val now = System.currentTimeMillis()
        prefs.edit().putLong("last_sync_millis", now).apply()
        SyncResult(concerts = concerts, downloaded = downloaded, syncedAtMillis = now)
    }

    private fun parseCsv(raw: String, context: Context): List<Concert> {
        val rows = parseRows(raw)
        if (rows.isEmpty()) return emptyList()
        val header = rows.first().mapIndexed { index, value ->
            (if (index == 0) value.removePrefix("\uFEFF") else value).trim().lowercase()
        }
        fun column(name: String): Int = header.indexOf(name)
        fun value(row: List<String>, name: String): String {
            val index = column(name)
            return if (index >= 0 && index < row.size) row[index].trim() else ""
        }

        require(column("id") >= 0 && column("title") >= 0 && column("date") >= 0) {
            "El CSV ha de contindre almenys les columnes id,title,date."
        }

        return rows.drop(1).mapNotNull { row ->
            if (row.all { it.isBlank() }) return@mapNotNull null
            runCatching {
                val id = value(row, "id").toInt()
                val title = value(row, "title")
                val canonicalTown = MunicipalityRepository.canonicalTown(value(row, "town"))
                val artists = splitList(value(row, "artists"))
                val primaryArtist = artists.firstOrNull().orEmpty().ifBlank { title }
                val posterUrl = resolveRemoteUrl(value(row, "poster_url"))
                val posterUri = posterUrl.takeIf(String::isNotBlank)
                val category = categoryFrom(value(row, "category"))
                val price = value(row, "price").toIntOrNull() ?: 0
                val ticketPrice = value(row, "ticket_price").toDoubleOrNull()?.toInt() ?: -1
                val ticketUrl = value(row, "ticket_url")
                val ticketAvailable = value(row, "ticket_available").toBooleanLoose(default = false) || ticketUrl.isNotBlank()
                val sponsored = value(row, "sponsored").toBooleanLoose(default = false)
                Concert(
                    id = id,
                    artist = primaryArtist,
                    title = title.ifBlank { primaryArtist },
                    subtitle = value(row, "subtitle"),
                    lineup = artists.ifEmpty { listOf(primaryArtist) },
                    posterUri = posterUri,
                    artistImageUri = value(row, "artist_image_url").takeIf { it.startsWith("https://", ignoreCase = true) },
                    venueImageUri = value(row, "venue_image_url").takeIf { it.startsWith("https://", ignoreCase = true) },
                    townImageUri = value(row, "town_image_url").takeIf { it.startsWith("https://", ignoreCase = true) },
                    organizers = splitList(value(row, "organizers")),
                    date = value(row, "date"),
                    time = value(row, "time"),
                    endDate = value(row, "end_date"),
                    endTime = value(row, "end_time"),
                    town = canonicalTown,
                    comarca = MunicipalityRepository.comarcaFor(MunicipalityRepository.loadCached(context), canonicalTown)
                        ?: value(row, "comarca").ifBlank { "Sense comarca" },
                    venue = value(row, "venue"),
                    styles = splitList(value(row, "styles")).ifEmpty { listOf(category.label) },
                    price = price,
                    language = value(row, "language"),
                    description = value(row, "description"),
                    category = category,
                    ticketPrice = ticketPrice,
                    ticketAvailable = ticketAvailable,
                    ticketUrl = ticketUrl,
                    sponsored = sponsored,
                    sponsorLabel = value(row, "sponsor_label").takeIf { it.isNotBlank() },
                    createdAtMillis = value(row, "created_at").toLongOrNull() ?: 0L,
                    coEditorUsernames = splitList(value(row, "coeditors")).toSet()
                )
            }.getOrNull()
        }
    }

    /** Parser CSV amb suport de comes, cometes dobles i salts de línia dins de camps citats. */
    internal fun parseRows(raw: String): List<List<String>> {
        val rows = mutableListOf<MutableList<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < raw.length && raw[i + 1] == '"') {
                        field.append('"')
                        i++
                    } else {
                        quoted = false
                    }
                } else {
                    field.append(c)
                }
            } else {
                when (c) {
                    '"' -> quoted = true
                    ',' -> {
                        row += field.toString()
                        field.setLength(0)
                    }
                    '\n' -> {
                        row += field.toString().trimEnd('\r')
                        field.setLength(0)
                        rows += row
                        row = mutableListOf()
                    }
                    else -> field.append(c)
                }
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row += field.toString().trimEnd('\r')
            rows += row
        }
        return rows
    }

    private fun splitList(raw: String): List<String> = raw
        .split('|')
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()

    private fun categoryFrom(raw: String): CulturalCategory {
        val normalized = raw.trim()
        if (normalized.isBlank()) return CulturalCategory.MUSIC
        return CulturalCategory.entries.firstOrNull {
            it.name.equals(normalized, ignoreCase = true) || it.label.equals(normalized, ignoreCase = true)
        } ?: CulturalCategory.OTHER
    }

    private fun String.toBooleanLoose(default: Boolean): Boolean = when (trim().lowercase()) {
        "1", "true", "si", "sí", "yes", "y" -> true
        "0", "false", "no", "n" -> false
        else -> default
    }


    private fun resolveRemoteUrl(raw: String): String {
        val value = raw.trim()
        if (value.isBlank()) return ""
        if (value.startsWith("https://", ignoreCase = true)) return value
        if (value.startsWith("http://", ignoreCase = true)) return ""

        val base = configuredBaseUrl()
        if (!base.startsWith("https://", ignoreCase = true)) return value
        return if (value.startsWith("/")) {
            runCatching {
                val origin = URL(base)
                "${origin.protocol}://${origin.host}${if (origin.port > 0) ":${origin.port}" else ""}$value"
            }.getOrDefault(base.trimEnd('/') + value)
        } else {
            base + value.removePrefix("./")
        }
    }

    private fun cachePoster(context: Context, concertId: Int, posterUrl: String): String? {
        if (posterUrl.isBlank()) return null
        if (!posterUrl.startsWith("https://", ignoreCase = true)) return posterUrl

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val postersDir = File(File(context.filesDir, CACHE_DIR), "images").apply { mkdirs() }
        val extension = runCatching {
            val path = Uri.parse(posterUrl).lastPathSegment.orEmpty()
            path.substringAfterLast('.', "jpg").substringBefore('?').take(5).ifBlank { "jpg" }
        }.getOrDefault("jpg")
        val target = File(postersDir, "$concertId.$extension")
        val oldUrl = prefs.getString("poster_url_$concertId", null)
        if (target.exists() && oldUrl == posterUrl) return Uri.fromFile(target).toString()

        return runCatching {
            val connection = (URL(posterUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                requestMethod = "GET"
            }
            try {
                require(connection.responseCode in 200..299) { "HTTP ${connection.responseCode}" }
                val temp = File(postersDir, "$concertId.tmp")
                connection.inputStream.use { input -> temp.outputStream().use { output -> input.copyTo(output) } }
                if (target.exists()) target.delete()
                require(temp.renameTo(target)) { "No s'ha pogut guardar el cartell" }
                prefs.edit().putString("poster_url_$concertId", posterUrl).apply()
                Uri.fromFile(target).toString()
            } finally {
                connection.disconnect()
            }
        }.getOrElse {
            if (target.exists()) Uri.fromFile(target).toString() else null
        }
    }
}
