package com.example.agendavalenciana.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import java.io.File

/**
 * Còpia local persistent dels QR de les entrades.
 *
 * No depén de la xarxa: el codi de l'entrada és suficient per reconstruir el QR.
 * Guardem també el PNG en filesDir perquè el dia del concert continue disponible
 * encara que l'app haja estat tancada o Android haja buidat cacheDir.
 */
internal object TicketQrCache {
    private fun dir(context: Context): File = File(context.filesDir, "ticket_qr").apply { mkdirs() }

    private fun safeCode(code: String): String = code.filter(Char::isLetterOrDigit).uppercase().take(96)

    fun fileFor(context: Context, code: String): File = File(dir(context), "qr_${safeCode(code)}.png")

    fun ensureCached(context: Context, code: String, size: Int = 640): File? = runCatching {
        val file = fileFor(context, code)
        if (file.exists() && file.length() > 0L) return@runCatching file
        val bitmap = create(code, size)
        file.outputStream().buffered().use { out ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out))
        }
        bitmap.recycle()
        file
    }.getOrNull()

    fun loadOrCreate(context: Context, code: String, size: Int = 640): Bitmap {
        val file = ensureCached(context, code, size)
        val cached = file?.takeIf { it.exists() && it.length() > 0L }?.let { BitmapFactory.decodeFile(it.absolutePath) }
        return cached ?: create(code, size)
    }

    private fun create(value: String, size: Int): Bitmap {
        val matrix = MultiFormatWriter().encode(value, BarcodeFormat.QR_CODE, size, size)
        val pixels = IntArray(size * size)
        for (y in 0 until size) {
            val offset = y * size
            for (x in 0 until size) {
                pixels[offset + x] = if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            }
        }
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, size, 0, 0, size, size)
        }
    }
}
