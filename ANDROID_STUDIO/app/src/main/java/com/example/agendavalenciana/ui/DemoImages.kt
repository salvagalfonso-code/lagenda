package com.example.agendavalenciana.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.example.agendavalenciana.data.ServerApiRepository
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Sistema d'imatges restaurat a partir de BATECS 0.17.12.
 *
 * Manté la mateixa regla provada de càrrega i la mateixa cache `entity_images`,
 * de manera que les fotos predefinides antigues tornen a ser reutilitzables.
 * Les funcions noves de DUAL (múltiples candidats i selecció d'imatge) es
 * mantenen com una extensió damunt del carregador BATECS.
 */
private val bitmapMemoryCacheSizeKb = ((Runtime.getRuntime().maxMemory() / 1024L) / 16L)
    .coerceIn(8L * 1024L, 48L * 1024L)
    .toInt()
private val bitmapMemoryCache = object : LruCache<String, Bitmap>(bitmapMemoryCacheSizeKb) {
    override fun sizeOf(key: String, value: Bitmap): Int = (value.byteCount / 1024).coerceAtLeast(1)
}

// Evita que dos composables descarreguen/decodifiquen alhora la mateixa imatge.
private val bitmapLoadLocks = ConcurrentHashMap<String, Any>()
// Si una URL falla, no la reintentem cada vegada que una card ix i torna a entrar al viewport.
private val failedImageUntil = ConcurrentHashMap<String, Long>()
// 10ep: miniatures de Batecs i cartells del feed no compartixen la mateixa cua.
// Abans 10 cercles podien ocupar els tres permisos i deixar els cartells esperant.
private val feedImageDownloadSemaphore = Semaphore(2, true)
private val tinyImageDownloadSemaphore = Semaphore(1, true)
// La descodificació també consumeix CPU. Limitem el treball gran encara que la imatge
// ja estiga en disc perquè 5 BitmapFactory simultanis poden fer tartamudejar Compose.
private val feedImageDecodeSemaphore = Semaphore(2, true)
private val tinyImageDecodeSemaphore = Semaphore(1, true)
// 10eo: no recorrem centenars de fitxers de cache durant les primeres miniatures.
private val lastImageCacheTrimAt = AtomicLong(System.currentTimeMillis())
private const val IMAGE_CACHE_TRIM_INTERVAL_MS = 5L * 60L * 1000L

private fun normalizedImageDimension(maxDimension: Int): Int = when {
    maxDimension <= 128 -> 128
    maxDimension <= 256 -> 256
    maxDimension <= 512 -> 512
    maxDimension <= 720 -> 720
    else -> 1080
}

private fun bitmapCacheKey(raw: String, maxDimension: Int): String =
    "$raw|${normalizedImageDimension(maxDimension)}"

internal fun cachePickedImageForEditing(context: Context, uri: Uri): String {
    val resolver = context.contentResolver
    val mime = runCatching { resolver.getType(uri) }.getOrNull()?.lowercase().orEmpty()
    val ext = when (mime) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/jpeg", "image/jpg" -> "jpg"
        else -> "img"
    }
    val dir = File(context.cacheDir, "agenda_picked_media").apply {
        if (!exists() && !mkdirs()) error("No s’ha pogut preparar la carpeta temporal d’imatges.")
    }
    dir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(24)?.forEach { runCatching { it.delete() } }
    val target = File(dir, "picked_${System.currentTimeMillis()}_${uri.toString().hashCode().toUInt().toString(16)}.$ext")

    // OpenDocument pot retornar providers que no implementen bé openInputStream().
    // Provem primer stream normal i després descriptor de fitxer. La còpia local
    // fa que preview/crop/upload ja no depenguen del permís temporal content://.
    val copied = runCatching {
        resolver.openInputStream(uri)?.use { input ->
            target.outputStream().buffered().use { output -> input.copyTo(output, 64 * 1024) }
            true
        } ?: false
    }.getOrDefault(false) || runCatching {
        resolver.openFileDescriptor(uri, "r")?.use { pfd ->
            android.os.ParcelFileDescriptor.AutoCloseInputStream(pfd).use { input ->
                target.outputStream().buffered().use { output -> input.copyTo(output, 64 * 1024) }
            }
            true
        } ?: false
    }.getOrDefault(false)

    if (!copied || !target.exists() || target.length() <= 0L) {
        runCatching { target.delete() }
        error("No s’ha pogut obrir la imatge seleccionada. Torna a triar-la des de Fitxers/Galeria.")
    }
    return Uri.fromFile(target).toString()
}


@Composable
internal fun rememberImageBitmapFromUri(photoUri: String?, maxDimension: Int = 640): ImageBitmap? {
    val context = LocalContext.current
    val raw = photoUri?.trim()?.takeIf { it.isNotBlank() }
    val effectiveDimension = normalizedImageDimension(maxDimension)
    val exactMemory = raw?.let { bitmapMemoryCache.get(bitmapCacheKey(it, effectiveDimension)) }
    // Quan una imatge gran encara no està preparada, reutilitza de seguida la miniatura
    // que ja s'ha vist al feed/Batecs. Després la versió gran arriba en segon pla.
    val progressiveMemory = exactMemory ?: raw?.let { uri ->
        sequenceOf(256, 128)
            .filter { it < effectiveDimension }
            .mapNotNull { dimension -> bitmapMemoryCache.get(bitmapCacheKey(uri, dimension)) }
            .firstOrNull()
    }
    return produceState<ImageBitmap?>(initialValue = progressiveMemory?.asImageBitmap(), key1 = raw, key2 = effectiveDimension) {
        value = withContext(Dispatchers.IO) {
            raw?.let { loadBitmapBatecs(context, it, effectiveDimension)?.asImageBitmap() }
        }
    }.value
}

/**
 * Extensió DUAL: prova els candidats en ordre, però cada candidat usa exactament
 * el mecanisme de càrrega BATECS. Una URL trencada no deixa la card buida.
 */
@Composable
internal fun rememberImageBitmapFromUris(
    photoUris: List<String?>,
    maxDimension: Int = 640,
    eventId: Int? = null,
    knownResolvedUri: String? = null
): ImageBitmap? {
    val context = LocalContext.current
    val effectiveDimension = normalizedImageDimension(maxDimension)
    // L'ordre rebut és autoritatiu. Una resolució cachejada mai pot saltar-se un
    // candidat de més prioritat (p. ex. un artista cachejat per damunt d'un cartell nou).
    val candidates = (photoUris + listOf(knownResolvedUri))
        .mapNotNull { it?.trim()?.takeIf(String::isNotBlank) }
        .distinct()
    val cacheKey = candidates.joinToString("\u001f")

    // Només reutilitzem immediatament la memòria del PRIMER candidat. Reutilitzar una
    // imatge inferior faria que un artista ja cachejat impedira carregar el cartell.
    val firstCandidate = candidates.firstOrNull()
    val firstExactHit = firstCandidate?.let { uri ->
        bitmapMemoryCache.get(bitmapCacheKey(uri, effectiveDimension))?.let { uri to it }
    }
    val firstProgressiveHit = firstExactHit ?: if (effectiveDimension > 256 && firstCandidate != null) {
        sequenceOf(720, 256, 128)
            .filter { it < effectiveDimension }
            .mapNotNull { dimension ->
                bitmapMemoryCache.get(bitmapCacheKey(firstCandidate, dimension))?.let { firstCandidate to it }
            }
            .firstOrNull()
    } else null

    return produceState<ImageBitmap?>(
        initialValue = firstProgressiveHit?.second?.asImageBitmap(),
        key1 = cacheKey,
        key2 = "$effectiveDimension|${eventId ?: 0}"
    ) {
        val imageStarted = if (eventId != null) SystemClock.elapsedRealtime() else 0L
        val loaded = firstExactHit ?: withContext(Dispatchers.IO) {
            // Prova estrictament en ordre. Només passa al següent si l'anterior no carrega.
            candidates.asSequence().mapNotNull { uri ->
                loadBitmapBatecs(context, uri, effectiveDimension)?.let { uri to it }
            }.firstOrNull()
        }
        if (eventId != null) {
            val elapsed = SystemClock.elapsedRealtime() - imageStarted
            if (elapsed >= 400L) {
                Log.d("AgendaPerf", "image[event=$eventId] ${elapsed}ms · ${candidates.size} candidates · ${effectiveDimension}px")
            }
        }
        value = loaded?.second?.asImageBitmap()
        // USER és només consumidor: carregar una card no ha de provocar cap POST al servidor.
        // La resolució/canonització d'imatges correspon al backend/PRO. Evitem una petició
        // extra per cada imatge nova del feed, que podia saturar xarxa i servidor durant scroll.
    }.value
}

private fun sampleSize(width: Int, height: Int, maxDimension: Int): Int {
    if (maxDimension <= 0) return 1
    var sample = 1
    var w = width
    var h = height
    while (maxOf(w, h) > maxDimension * 2) {
        sample *= 2
        w /= 2
        h /= 2
    }
    return sample.coerceAtLeast(1)
}

private fun decodeSampledFile(file: File, maxDimension: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val opts = BitmapFactory.Options().apply {
        inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
        inPreferredConfig = Bitmap.Config.RGB_565
    }
    return BitmapFactory.decodeFile(file.absolutePath, opts)
}

private fun decodeSampledBytes(bytes: ByteArray, maxDimension: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val opts = BitmapFactory.Options().apply {
        inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
        inPreferredConfig = Bitmap.Config.RGB_565
    }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
}

private fun downloadImageToFile(url: String, target: File, cacheDir: File): Boolean = runCatching {
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 6_000
        readTimeout = 10_000
        requestMethod = "GET"
        instanceFollowRedirects = true
        setRequestProperty("User-Agent", "AgendaValenciana-Android/0.20.8.1")
        setRequestProperty("Accept", "image/avif,image/webp,image/jpeg,image/png,image/*;q=0.8")
    }
    try {
        if (connection.responseCode !in 200..299) return@runCatching false
        val length = connection.contentLengthLong
        if (length > 12L * 1024L * 1024L) return@runCatching false
        val tmp = File(cacheDir, target.name + ".tmp")
        connection.inputStream.use { input ->
            tmp.outputStream().buffered().use { output ->
                val buffer = ByteArray(32 * 1024)
                var total = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    total += read
                    if (total > 12L * 1024L * 1024L) { tmp.delete(); return@runCatching false }
                    output.write(buffer, 0, read)
                }
            }
        }
        if (tmp.length() <= 0L) { tmp.delete(); return@runCatching false }
        if (target.exists()) target.delete()
        tmp.renameTo(target)
    } finally {
        connection.disconnect()
    }
}.getOrDefault(false)

private fun trimImageCache(cacheDir: File) {
    runCatching {
        val files = cacheDir.listFiles()?.filter { it.isFile && !it.name.endsWith(".tmp") } ?: return
        var total = files.sumOf { it.length() }
        var count = files.size
        if (count <= 500 && total <= 120L * 1024L * 1024L) return
        for (file in files.sortedBy { it.lastModified() }) {
            val length = file.length()
            if (file.delete()) {
                total -= length
                count--
            }
            if (total <= 90L * 1024L * 1024L && count <= 400) break
        }
    }
}

private fun maybeTrimImageCache(cacheDir: File) {
    val now = System.currentTimeMillis()
    val previous = lastImageCacheTrimAt.get()
    if (now - previous < IMAGE_CACHE_TRIM_INTERVAL_MS) return
    if (lastImageCacheTrimAt.compareAndSet(previous, now)) trimImageCache(cacheDir)
}

private fun loadBitmapBatecs(context: Context, raw: String, maxDimension: Int): Bitmap? {
    val effectiveDimension = normalizedImageDimension(maxDimension)
    val memoryKey = bitmapCacheKey(raw, effectiveDimension)
    bitmapMemoryCache.get(memoryKey)?.let { return it }

    val now = System.currentTimeMillis()
    failedImageUntil[raw]?.let { until ->
        if (now < until) return null
        failedImageUntil.remove(raw, until)
    }

    val lock = bitmapLoadLocks.getOrPut(memoryKey) { Any() }
    try {
        synchronized(lock) {
            bitmapMemoryCache.get(memoryKey)?.let { return it }
            failedImageUntil[raw]?.let { until -> if (System.currentTimeMillis() < until) return null }

            val decoded = runCatching {
                when {
                    raw.startsWith("asset://") -> {
                        val path = raw.removePrefix("asset://")
                        val bytes = context.assets.open(path).use { it.readBytes() }
                        decodeSampledBytes(bytes, effectiveDimension)
                    }
                    raw.startsWith("file://") -> {
                        val path = Uri.parse(raw).path ?: return@runCatching null
                        decodeSampledFile(File(path), effectiveDimension)
                    }
                    raw.startsWith("https://", ignoreCase = true) -> {
                        val cacheDir = File(context.cacheDir, "entity_images").apply { mkdirs() }
                        val diskKey = "$raw|$effectiveDimension".hashCode().toUInt().toString(16)
                        val target = File(cacheDir, "$diskKey.img")
                        if (!target.exists() || target.length() <= 0L) {
                            val downloadSemaphore = if (effectiveDimension <= 128) {
                                tinyImageDownloadSemaphore
                            } else {
                                feedImageDownloadSemaphore
                            }
                            downloadSemaphore.acquire()
                            try {
                                // Un altre carregador pot haver completat el fitxer mentre esperàvem torn.
                                if (!target.exists() || target.length() <= 0L) {
                                    val encoded = URLEncoder.encode(raw, StandardCharsets.UTF_8.toString())
                                    val thumbQuality = when {
                                        effectiveDimension <= 128 -> 64
                                        effectiveDimension <= 512 -> 72
                                        else -> 78
                                    }
                                    val thumbUrl = ServerApiRepository.baseUrl() +
                                        "images/thumb.php?url=$encoded&w=${effectiveDimension.coerceIn(96, 1200)}&q=$thumbQuality"
                                    // 10ep: també per als cartells del mateix servidor demanem
                                    // primer la miniatura. Abans els JPG/PNG originals podien ser
                                    // de diversos MB i saturaven xarxa/CPU durant el primer scroll.
                                    // L'original continua sent fallback si el thumbnail no respon.
                                    val downloaded = downloadImageToFile(thumbUrl, target, cacheDir) ||
                                        downloadImageToFile(raw, target, cacheDir)
                                    if (!downloaded) return@runCatching null
                                }
                            } finally {
                                downloadSemaphore.release()
                            }
                            // La neteja recorre el directori: màxim una vegada cada 5 minuts.
                            maybeTrimImageCache(cacheDir)
                        }
                        val touchNow = System.currentTimeMillis()
                        if (touchNow - target.lastModified() > 60L * 60L * 1000L) {
                            target.setLastModified(touchNow)
                        }
                        val decodeSemaphore = if (effectiveDimension <= 128) tinyImageDecodeSemaphore else feedImageDecodeSemaphore
                        decodeSemaphore.acquire()
                        try {
                            decodeSampledFile(target, effectiveDimension)
                        } finally {
                            decodeSemaphore.release()
                        }
                    }
                    else -> {
                        val bytes = context.contentResolver.openInputStream(Uri.parse(raw))?.use { it.readBytes() }
                            ?: return@runCatching null
                        decodeSampledBytes(bytes, effectiveDimension)
                    }
                }
            }.getOrNull()

            if (decoded != null) {
                failedImageUntil.remove(raw)
                bitmapMemoryCache.put(memoryKey, decoded)
            } else {
                // Cinc minuts són suficients per evitar una tempesta de reintents mentre
                // l'usuari fa scroll, sense convertir un error temporal en permanent.
                failedImageUntil[raw] = System.currentTimeMillis() + 5L * 60L * 1000L
            }
            return decoded
        }
    } finally {
        bitmapLoadLocks.remove(memoryKey, lock)
    }
}
