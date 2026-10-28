package com.example.agendavalenciana.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.Html
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executors
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.Normalizer
import kotlin.math.abs

/**
 * Catàleg territorial v277 dels Països Catalans, inclòs offline en els assets de l'APK.
 * Per compatibilitat manté la font valenciana de Viquipèdia/ICV com a fallback excepcional.
 */
data class Municipality(
    val code: String,
    val name: String,
    val comarca: String,
    val province: String,
    val latitude: Double,
    val longitude: Double,
    /** Denominació oficial alternativa quan difereix del topònim valencià. */
    val officialName: String = "",
    val type: String = "MUNICIPI",
    val territoryId: String = "PV",
    val comarcaId: String = "",
    val parentId: String = "",
    val otherNames: String = ""
)

object MunicipalityRepository {
    private val backgroundExecutor = Executors.newSingleThreadExecutor()
    private const val CACHE_FILE = "municipis_pv_wikipedia.json"
    private const val LEGACY_CACHE_FILE = "municipis_pv_icv.json"
    // v277: catàleg complet empaquetat amb l'APK; sense FTP, CSV ni connexió necessària.
    private const val BUNDLED_ASSET_FILE = "agenda_territori_v277.json"
    // Igual que el catàleg valencià, el cens complet es conserva en filesDir.
    // El fitxer s'independitza de la cache antiga perquè té IDs, nuclis i tot el territori.
    private const val PPCC_CACHE_FILE = "municipis_ppcc_v277.json"
    @Volatile private var bundledCache: List<Municipality>? = null
    @Volatile private var bundledComarcaNames: List<String> = emptyList()

    fun bundledComarques(): List<String> = bundledComarcaNames

    private fun bundledAvailable(context: Context): Boolean = runCatching {
        context.assets.open(BUNDLED_ASSET_FILE).use { true }
    }.getOrDefault(false)

    private fun loadBundled(context: Context): List<Municipality>? {
        bundledCache?.let { return it }
        synchronized(this) {
            bundledCache?.let { return it }
            val local = File(context.filesDir, PPCC_CACHE_FILE)
            // Prioritzem la memòria interna, com es feia amb els 542 municipis valencians.
            // Si està incompleta o malmesa, recuperem el catàleg inclòs en l'APK.
            val sources = buildList<Pair<Boolean, String>> {
                if (local.isFile) {
                    runCatching { local.readText(Charsets.UTF_8) }.getOrNull()?.let {
                        add(true to it)
                    }
                }
                runCatching {
                    context.assets.open(BUNDLED_ASSET_FILE)
                        .bufferedReader(Charsets.UTF_8).use { it.readText() }
                }.getOrNull()?.let { add(false to it) }
            }
            for ((fromDisk, raw) in sources) {
                val root = runCatching { JSONObject(raw) }.getOrNull() ?: continue
                if (root.optInt("version") != 277) continue
                val decoded = runCatching { decodeCache(raw) }.getOrNull() ?: continue
                val all = decoded.distinctBy { it.code }
                    .sortedWith(compareBy<Municipality> { it.name.lowercase() }
                        .thenBy { if (it.type == "MUNICIPI" || it.type == "PARRÒQUIA") 0 else 1 }
                        .thenBy { it.code })
                if (all.size != 6016) continue
                val groups = root.optJSONArray("comarques") ?: continue
                if (groups.length() != 90) continue
                val names = (0 until groups.length()).mapNotNull { idx ->
                    groups.optJSONObject(idx)?.optString("name")?.trim()?.takeIf(String::isNotBlank)
                }
                if (names.size != 90) continue
                if (!fromDisk) {
                    // Escriptura de millor esforç: el catàleg en l'APK continua disponible.
                    runCatching { local.writeText(raw, Charsets.UTF_8) }
                }
                bundledComarcaNames = names
                bundledCache = all
                return all
            }
            return null
        }
    }


    // 10ai: el catàleg municipal es parseja una sola vegada per procés. Abans,
    // parseEvent() podia rellegir este JSON per cada esdeveniment sense comarca.
    @Volatile private var memoryCachePath: String? = null
    @Volatile private var memoryCacheMtime: Long = Long.MIN_VALUE
    @Volatile private var memoryCache: List<Municipality> = emptyList()

    // Catàleg territorial de referència demanat per a l'app.
    private const val WIKIPEDIA_URL =
        "https://ca.wikipedia.org/wiki/Llista_dels_municipis_del_Pa%C3%ADs_Valenci%C3%A0"

    // Fonts ICV conservades únicament com a fallback si Viquipèdia no està disponible.
    private const val MUNICIPALITIES_URL =
        "https://terramapas.icv.gva.es/0105_Delimitaciones?service=WFS&request=GetFeature&version=2.0.0&typeNames=ICV.Municipios&srsName=EPSG:4326&outputFormat=application/json"
    private const val COMARCAS_URL =
        "https://terramapas.icv.gva.es/0105_Delimitaciones?service=WFS&request=GetFeature&version=2.0.0&typeNames=ICV.Comarcas&srsName=EPSG:4326&outputFormat=application/json"

    fun isCacheFresh(context: Context, maxAgeMillis: Long = 7L * 24L * 60L * 60L * 1000L): Boolean {
        if (bundledAvailable(context)) return true
        val preferred = File(context.filesDir, CACHE_FILE)
        val legacy = File(context.filesDir, LEGACY_CACHE_FILE)
        val file = when {
            preferred.exists() -> preferred
            legacy.exists() -> legacy
            else -> return false
        }
        val age = System.currentTimeMillis() - file.lastModified()
        return file.length() > 0L && age in 0 until maxAgeMillis
    }

    fun loadCached(context: Context): List<Municipality> {
        loadBundled(context)?.let { return it }
        val preferred = File(context.filesDir, CACHE_FILE)
        val legacy = File(context.filesDir, LEGACY_CACHE_FILE)
        val file = when {
            preferred.exists() -> preferred
            legacy.exists() -> legacy
            else -> {
                memoryCachePath = null
                memoryCacheMtime = Long.MIN_VALUE
                memoryCache = emptyList()
                return emptyList()
            }
        }
        val path = file.absolutePath
        val mtime = file.lastModified()
        if (memoryCachePath == path && memoryCacheMtime == mtime && memoryCache.isNotEmpty()) {
            return memoryCache
        }
        synchronized(this) {
            if (memoryCachePath == path && memoryCacheMtime == mtime && memoryCache.isNotEmpty()) {
                return memoryCache
            }
            val decoded = runCatching { decodeCache(file.readText(Charsets.UTF_8)) }.getOrDefault(emptyList())
                .map(::repairMunicipality).distinctBy { normalize(it.name) }
            memoryCachePath = path
            memoryCacheMtime = mtime
            memoryCache = decoded
            return decoded
        }
    }

    fun syncInBackground(
        context: Context,
        onComplete: (Result<List<Municipality>>) -> Unit
    ) {
        val appContext = context.applicationContext
        backgroundExecutor.execute {
            val result = runCatching { sync(appContext) }
            Handler(Looper.getMainLooper()).post { onComplete(result) }
        }
    }

    fun sync(context: Context): List<Municipality> {
        // La v277 no ha d'esborrar els 6.016 registres amb una descàrrega de 542 municipis.
        loadBundled(context)?.let { return it }
        val wikipedia = runCatching {
            parseWikipediaMunicipalities(download(WIKIPEDIA_URL))
                .map(::repairMunicipality)
                .distinctBy { normalize(it.name) }
                .sortedBy { it.name.lowercase() }
        }.getOrNull()

        val municipalities = if (wikipedia != null && wikipedia.size >= 540) {
            wikipedia
        } else {
            // Fallback de continuïtat: si canvia temporalment l'HTML de Viquipèdia,
            // l'agenda continua operativa amb les capes territorials de l'ICV.
            val municipalGeoJson = download(MUNICIPALITIES_URL)
            val comarcaGeoJson = download(COMARCAS_URL)
            val comarcas = parseComarcas(comarcaGeoJson)
            parseMunicipalities(municipalGeoJson, comarcas)
                .map(::repairMunicipality)
                .distinctBy { normalize(it.name) }
                .sortedBy { it.name.lowercase() }
        }

        require(municipalities.size >= 540) {
            "Catàleg municipal incomplet: ${municipalities.size} municipis"
        }

        val target = File(context.filesDir, CACHE_FILE)
        target.writeText(encodeCache(municipalities), Charsets.UTF_8)
        memoryCachePath = target.absolutePath
        memoryCacheMtime = target.lastModified()
        memoryCache = municipalities
        return municipalities
    }

    /** La Torre de les Maçanes: elimina coordenades enganxades i variants oficials del topònim. */
    fun canonicalTown(raw: String): String {
        val clean = raw.trim().replace(Regex("\\s+"), " ")
        val normalized = normalize(clean)
        return if (
            Regex("^(la )?torre de les macanes(?=$|[ ,(0-9°])").containsMatchIn(normalized) ||
            Regex("^torremanzanas(?=$|[ /,(0-9°])").containsMatchIn(normalized) ||
            normalized.startsWith("torremanzanas/la torre de les macanes")
        ) "La Torre de les Maçanes" else clean
    }

    private fun repairMunicipality(item: Municipality): Municipality {
        val town = canonicalTown(item.name)
        val fixedComarca = comarcaOverrides[normalize(town)] ?: item.comarca
        return if (town != item.name || fixedComarca != item.comarca)
            item.copy(name = town, comarca = fixedComarca) else item
    }

    fun findByName(items: List<Municipality>, name: String): Municipality? {
        val target = normalize(canonicalTown(name))
        return items.firstOrNull {
            normalize(it.name) == target ||
                (it.officialName.isNotBlank() && normalize(it.officialName) == target) ||
                it.otherNames.split('|', ';').any { alias -> alias.isNotBlank() && normalize(alias) == target }
        }
    }

    private val comarcaOverrides: Map<String, String> = mapOf(
        normalize("La Torre de les Maçanes") to "L'Alacantí",
        normalize("Torre de les Maçanes") to "L'Alacantí",
        normalize("Torremanzanas") to "L'Alacantí"
    )

    fun comarcaFor(items: List<Municipality>, town: String): String? =
        comarcaOverrides[normalize(canonicalTown(town))]
            ?: findByName(items, town)?.comarca?.takeIf { it.isNotBlank() }

    private fun download(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("Accept", "text/html,application/xhtml+xml,application/json,application/geo+json")
            setRequestProperty("User-Agent", "AgendaValenciana/1.0")
        }
        try {
            val status = connection.responseCode
            require(status in 200..299) { "HTTP $status" }
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Llig la taula principal de la pàgina «Llista dels municipis del País Valencià».
     * El topònim valencià és el nom canònic que mostra l'app; la denominació oficial
     * alternativa es conserva com a àlies perquè també es puga trobar escrivint-la.
     */
    private fun parseWikipediaMunicipalities(raw: String): List<Municipality> {
        val rowRegex = Regex("(?is)<tr\\b[^>]*>(.*?)</tr>")
        val cellRegex = Regex("(?is)<t[dh]\\b([^>]*)>(.*?)</t[dh]>")
        val colspanRegex = Regex("(?i)colspan\\s*=\\s*[\"']?([0-9]+)")
        return buildList {
            rowRegex.findAll(raw).forEach { row ->
                // Viquipèdia usa colspan=2 quan el topònim valencià i l'oficial coincideixen.
                // Expandim el colspan perquè totes les files mantinguen les 13 columnes de la capçalera.
                val cells = buildList {
                    cellRegex.findAll(row.groupValues[1]).forEach { match ->
                        val attrs = match.groupValues[1]
                        val text = htmlText(match.groupValues[2])
                        val colspan = colspanRegex.find(attrs)?.groupValues?.getOrNull(1)?.toIntOrNull()?.coerceIn(1, 4) ?: 1
                        repeat(colspan) { add(text) }
                    }
                }
                if (cells.size < 12) return@forEach
                val name = canonicalTown(cells[0].cleanWikipediaCell())
                if (name.isBlank() || name.contains("Topònim", ignoreCase = true)) return@forEach

                // Després d'expandir colspan, l'esquema esperat és el de la capçalera (13 columnes).
                // Si una fila antiga no porta la columna final «Destacat», les columnes territorials
                // continuen ocupant les mateixes posicions.
                val officialRaw = cells.getOrNull(1).orEmpty().cleanWikipediaCell()
                val comarca = comarcaOverrides[normalize(name)]
                    ?: cells.getOrNull(10).orEmpty().cleanWikipediaCell()
                val province = provinceFromWikipedia(cells.getOrNull(11).orEmpty().cleanWikipediaCell())
                if (comarca.isBlank() || province.isBlank()) return@forEach

                val longitude = parseDms(cells.getOrNull(7).orEmpty()) ?: 0.0
                val latitude = parseDms(cells.getOrNull(8).orEmpty()) ?: 0.0
                val official = officialRaw.takeIf { it.isNotBlank() && normalize(it) != normalize(name) }.orEmpty()
                add(
                    Municipality(
                        code = "wiki:${normalize(name).replace(" ", "-")}",
                        name = name,
                        comarca = comarca,
                        province = province,
                        latitude = latitude,
                        longitude = longitude,
                        officialName = official
                    )
                )
            }
        }
    }

    private fun htmlText(fragment: String): String = Html
        .fromHtml(fragment, Html.FROM_HTML_MODE_LEGACY)
        .toString()
        .replace('\u00A0', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun String.cleanWikipediaCell(): String = this
        .replace(Regex("\\[[0-9]+]"), "")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun provinceFromWikipedia(value: String): String {
        val clean = normalize(value)
        return when {
            clean == "a" || clean.contains("alacant") || clean.contains("alicante") -> "Alacant"
            clean == "c" || clean.contains("castello") || clean.contains("castellon") -> "Castelló"
            clean == "v" || clean.contains("valencia") -> "València"
            else -> ""
        }
    }

    private fun parseDms(value: String): Double? {
        val direction = value.uppercase().lastOrNull { it in "NSEW" } ?: return null
        val numbers = Regex("[0-9]+(?:[.,][0-9]+)?")
            .findAll(value)
            .map { it.value.replace(',', '.').toDoubleOrNull() }
            .filterNotNull()
            .toList()
        if (numbers.isEmpty()) return null
        val degrees = numbers.getOrElse(0) { 0.0 }
        val minutes = numbers.getOrElse(1) { 0.0 }
        val seconds = numbers.getOrElse(2) { 0.0 }
        val decimal = degrees + minutes / 60.0 + seconds / 3600.0
        return if (direction == 'W' || direction == 'S') -decimal else decimal
    }

    private data class RegionShape(
        val name: String,
        val polygons: List<List<Pair<Double, Double>>>
    )

    private fun parseComarcas(raw: String): List<RegionShape> {
        val features = JSONObject(raw).optJSONArray("features") ?: JSONArray()
        return buildList {
            for (i in 0 until features.length()) {
                val feature = features.optJSONObject(i) ?: continue
                val props = feature.optJSONObject("properties") ?: JSONObject()
                val name = firstString(
                    props,
                    "comarca", "COMARCA", "nom_comarca", "NOM_COMARCA",
                    "nombre", "NOMBRE", "nom", "NOM"
                ) ?: genericName(props, prefer = "comar") ?: continue
                val geometry = feature.optJSONObject("geometry") ?: continue
                val polygons = outerRings(geometry)
                if (polygons.isNotEmpty()) add(RegionShape(name, polygons))
            }
        }
    }

    private fun parseMunicipalities(raw: String, comarcas: List<RegionShape>): List<Municipality> {
        val features = JSONObject(raw).optJSONArray("features") ?: JSONArray()
        return buildList {
            for (i in 0 until features.length()) {
                val feature = features.optJSONObject(i) ?: continue
                val props = feature.optJSONObject("properties") ?: JSONObject()
                val name = firstString(
                    props,
                    "municipio", "MUNICIPIO", "municipi", "MUNICIPI",
                    "nom_municipi", "NOM_MUNICIPI", "nombre", "NOMBRE", "nom", "NOM"
                ) ?: genericName(props, prefer = "mun") ?: continue

                val geometry = feature.optJSONObject("geometry") ?: continue
                val rings = outerRings(geometry)
                val point = representativePoint(rings) ?: continue

                val code = firstString(
                    props,
                    "codine", "CODINE", "cod_ine", "COD_INE", "ine", "INE",
                    "codigo", "CODIGO", "codi", "CODI"
                ).orEmpty()

                val directComarca = firstString(
                    props,
                    "comarca", "COMARCA", "nom_comarca", "NOM_COMARCA"
                )
                val comarca = directComarca
                    ?: comarcas.firstOrNull { region ->
                        region.polygons.any { ring -> pointInPolygon(point.first, point.second, ring) }
                    }?.name
                    ?: ""

                val directProvince = firstString(
                    props,
                    "provincia", "PROVINCIA", "província", "PROVÍNCIA",
                    "provincia_nom", "nom_provincia", "NOM_PROVINCIA"
                )
                val province = directProvince ?: provinceFromCode(code)

                add(
                    Municipality(
                        code = code,
                        name = name,
                        comarca = comarca,
                        province = province,
                        latitude = point.second,
                        longitude = point.first
                    )
                )
            }
        }
    }

    /** Extrau l'anell exterior de Polygon i MultiPolygon GeoJSON. */
    private fun outerRings(geometry: JSONObject): List<List<Pair<Double, Double>>> {
        val type = geometry.optString("type")
        val coordinates = geometry.optJSONArray("coordinates") ?: return emptyList()
        return when (type) {
            "Polygon" -> listOfNotNull(readRing(coordinates.optJSONArray(0)))
            "MultiPolygon" -> buildList {
                for (i in 0 until coordinates.length()) {
                    val polygon = coordinates.optJSONArray(i) ?: continue
                    readRing(polygon.optJSONArray(0))?.let(::add)
                }
            }
            else -> emptyList()
        }
    }

    private fun readRing(array: JSONArray?): List<Pair<Double, Double>>? {
        if (array == null || array.length() < 3) return null
        return buildList {
            for (i in 0 until array.length()) {
                val point = array.optJSONArray(i) ?: continue
                if (point.length() >= 2) add(point.optDouble(0) to point.optDouble(1))
            }
        }.takeIf { it.size >= 3 }
    }

    /**
     * Usa el centroide de l'anell de més superfície. Si la geometria és degenerada,
     * usa el centre de la caixa envolupant. Les coordenades arriben en EPSG:4326.
     */
    private fun representativePoint(rings: List<List<Pair<Double, Double>>>): Pair<Double, Double>? {
        val ring = rings.maxByOrNull { abs(signedArea(it)) } ?: return null
        val areaTimesTwo = ring.indices.sumOf { i ->
            val a = ring[i]
            val b = ring[(i + 1) % ring.size]
            a.first * b.second - b.first * a.second
        }
        if (abs(areaTimesTwo) > 1e-12) {
            var cx = 0.0
            var cy = 0.0
            for (i in ring.indices) {
                val a = ring[i]
                val b = ring[(i + 1) % ring.size]
                val cross = a.first * b.second - b.first * a.second
                cx += (a.first + b.first) * cross
                cy += (a.second + b.second) * cross
            }
            val point = (cx / (3.0 * areaTimesTwo)) to (cy / (3.0 * areaTimesTwo))
            if (point.first.isFinite() && point.second.isFinite()) return point
        }
        val xs = ring.map { it.first }
        val ys = ring.map { it.second }
        return ((xs.minOrNull()!! + xs.maxOrNull()!!) / 2.0) to
            ((ys.minOrNull()!! + ys.maxOrNull()!!) / 2.0)
    }

    private fun signedArea(ring: List<Pair<Double, Double>>): Double {
        var total = 0.0
        for (i in ring.indices) {
            val a = ring[i]
            val b = ring[(i + 1) % ring.size]
            total += a.first * b.second - b.first * a.second
        }
        return total / 2.0
    }

    private fun pointInPolygon(x: Double, y: Double, ring: List<Pair<Double, Double>>): Boolean {
        var inside = false
        var j = ring.lastIndex
        for (i in ring.indices) {
            val xi = ring[i].first
            val yi = ring[i].second
            val xj = ring[j].first
            val yj = ring[j].second
            val crosses = ((yi > y) != (yj > y)) &&
                (x < (xj - xi) * (y - yi) / ((yj - yi).takeUnless { abs(it) < 1e-15 } ?: 1e-15) + xi)
            if (crosses) inside = !inside
            j = i
        }
        return inside
    }

    private fun firstString(obj: JSONObject, vararg keys: String): String? {
        for (key in keys) {
            if (!obj.has(key) || obj.isNull(key)) continue
            val value = obj.optString(key).trim()
            if (value.isNotBlank() && value.lowercase() != "null") return value
        }
        return null
    }

    private fun genericName(obj: JSONObject, prefer: String): String? {
        val keys = obj.keys().asSequence().toList()
        val preferred = keys.firstOrNull {
            val k = normalize(it)
            k.contains(prefer) && (k.contains("nom") || k.contains("nomb"))
        }
        val fallback = keys.firstOrNull {
            val k = normalize(it)
            k == "nom" || k == "nombre" || k.contains("denomin")
        }
        return listOfNotNull(preferred, fallback).firstNotNullOfOrNull { key ->
            obj.optString(key).trim().takeIf { it.isNotBlank() && it.lowercase() != "null" }
        }
    }

    private fun provinceFromCode(code: String): String {
        val digits = code.filter(Char::isDigit)
        return when {
            digits.startsWith("03") -> "Alacant"
            digits.startsWith("12") -> "Castelló"
            digits.startsWith("46") -> "València"
            else -> ""
        }
    }

    private fun encodeCache(items: List<Municipality>): String {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject()
                    .put("code", item.code)
                    .put("name", item.name)
                    .put("comarca", item.comarca)
                    .put("province", item.province)
                    .put("latitude", item.latitude)
                    .put("longitude", item.longitude)
                    .put("officialName", item.officialName)
                    .put("type", item.type)
                    .put("territoryId", item.territoryId)
                    .put("comarcaId", item.comarcaId)
                    .put("parentId", item.parentId)
                    .put("otherNames", item.otherNames)
            )
        }
        return JSONObject()
            .put("source", "Viquipèdia - Llista dels municipis del País Valencià")
            .put("count", items.size)
            .put("municipalities", array)
            .toString()
    }

    private fun decodeCache(raw: String): List<Municipality> {
        val array = JSONObject(raw).optJSONArray("municipalities") ?: return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                add(
                    Municipality(
                        code = obj.optString("code"),
                        name = obj.optString("name"),
                        comarca = obj.optString("comarca"),
                        province = obj.optString("province"),
                        latitude = obj.optDouble("latitude"),
                        longitude = obj.optDouble("longitude"),
                        officialName = obj.optString("officialName"),
                        type = obj.optString("type", "MUNICIPI"),
                        territoryId = obj.optString("territoryId", "PV"),
                        comarcaId = obj.optString("comarcaId"),
                        parentId = obj.optString("parentId"),
                        otherNames = obj.optString("otherNames")
                    )
                )
            }
        }
    }

    private fun normalize(value: String): String = Normalizer
        .normalize(value.trim().lowercase(), Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
}
