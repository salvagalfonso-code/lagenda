package com.example.agendavalenciana.ui

import com.example.agendavalenciana.model.AgendaDateFormat

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import androidx.core.content.FileProvider
import com.example.agendavalenciana.R
import com.example.agendavalenciana.model.Concert
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.Executors

/** Genera una card visual compartible amb identitat de l'Agenda. */
internal object EventShareCard {
    private val shareExecutor = Executors.newSingleThreadExecutor()
    private const val WIDTH = 1080
    private const val HEIGHT = 1350

    fun deepLink(concertId: Int): String = "agendavalenciana://event/$concertId"

    fun share(context: Context, concert: Concert, artwork: Bitmap? = null) {
        // Renderitzar i comprimir una card 1080x1350 no ha de bloquejar Compose.
        shareExecutor.execute {
            runCatching {
                val bitmap = render(context, concert, artwork)
                val dir = File(context.cacheDir, "shared_cards").apply { mkdirs() }
                val file = File(dir, "agenda_${concert.id}_${System.currentTimeMillis()}.jpg")
                file.outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out) }
                bitmap.recycle()
                dir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(20)?.forEach { runCatching { it.delete() } }

                val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                val caption = buildString {
                    append(concert.title)
                    if (concert.town.isNotBlank()) append(" · ").append(concert.town)
                    append('\n')
                    append(formatDate(concert.date))
                    if (concert.time.isNotBlank()) append(" · ").append(concert.time)
                    if (concert.venue.isNotBlank()) append(" · ").append(concert.venue)
                    append('\n').append(deepLink(concert.id))
                }
                Intent(Intent.ACTION_SEND).apply {
                    type = "image/jpeg"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, caption)
                    putExtra(Intent.EXTRA_SUBJECT, concert.title)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }.onSuccess { intent ->
                Handler(Looper.getMainLooper()).post {
                    context.startActivity(Intent.createChooser(intent, "Compartir esdeveniment"))
                }
            }
        }
    }

    private fun render(context: Context, concert: Concert, artwork: Bitmap?): Bitmap {
        val out = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawColor(Color.rgb(15, 15, 17))

        val heroBottom = 735
        if (artwork != null && !artwork.isRecycled) {
            val src = centerCropRect(artwork.width, artwork.height, WIDTH, heroBottom)
            canvas.drawBitmap(artwork, src, Rect(0, 0, WIDTH, heroBottom), paint)
            paint.color = Color.argb(80, 0, 0, 0)
            canvas.drawRect(0f, 0f, WIDTH.toFloat(), heroBottom.toFloat(), paint)
        } else {
            paint.color = Color.rgb(35, 35, 42)
            canvas.drawRect(0f, 0f, WIDTH.toFloat(), heroBottom.toFloat(), paint)
        }

        // Degradat simple sobre la part baixa del cartell.
        for (i in 0..180) {
            val alpha = (i / 180f * 210).toInt().coerceIn(0, 210)
            paint.color = Color.argb(alpha, 15, 15, 17)
            canvas.drawRect(0f, (heroBottom - 180 + i).toFloat(), WIDTH.toFloat(), (heroBottom - 179 + i).toFloat(), paint)
        }

        var y = 790f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.WHITE
        paint.textSize = 66f
        y = drawWrapped(canvas, concert.title, 64f, y, WIDTH - 128f, 76f, paint, 3)

        y += 26f
        paint.textSize = 34f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.rgb(235, 235, 238)
        val whenLine = listOf(AgendaDateFormat.cardDate(concert.date), concert.time.takeIf(String::isNotBlank)).filterNotNull().joinToString(" · ")
        canvas.drawText(whenLine, 64f, y, paint)

        y += 58f
        paint.textSize = 31f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.rgb(205, 205, 212)
        val where = listOf(concert.venue.takeIf(String::isNotBlank), concert.town.takeIf(String::isNotBlank)).filterNotNull().joinToString(" · ")
        y = drawWrapped(canvas, where, 64f, y, WIDTH - 128f, 42f, paint, 2)

        // Peu de marca.
        val footerTop = 1195f
        paint.color = Color.rgb(245, 245, 247)
        canvas.drawRoundRect(RectF(54f, footerTop, WIDTH - 54f, 1302f), 34f, 34f, paint)
        val logo = BitmapFactory.decodeResource(context.resources, R.drawable.ic_login_logo)
        if (logo != null) {
            val box = Rect(78, 1212, 148, 1282)
            canvas.drawBitmap(logo, null, box, Paint(Paint.ANTI_ALIAS_FLAG))
            logo.recycle()
        }
        paint.color = Color.rgb(18, 18, 20)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 31f
        canvas.drawText("Agenda Valenciana de Cultura", 170f, 1254f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 22f
        paint.color = Color.rgb(80, 80, 86)
        canvas.drawText("Obri l'esdeveniment a l'Agenda", 170f, 1284f, paint)
        return out
    }

    private fun centerCropRect(srcW: Int, srcH: Int, dstW: Int, dstH: Int): Rect {
        if (srcW <= 0 || srcH <= 0) return Rect(0, 0, 1, 1)
        val srcRatio = srcW.toFloat() / srcH
        val dstRatio = dstW.toFloat() / dstH
        return if (srcRatio > dstRatio) {
            val width = (srcH * dstRatio).toInt()
            val left = (srcW - width) / 2
            Rect(left, 0, left + width, srcH)
        } else {
            val height = (srcW / dstRatio).toInt()
            val top = (srcH - height) / 2
            Rect(0, top, srcW, top + height)
        }
    }

    private fun drawWrapped(canvas: Canvas, text: String, x: Float, y: Float, maxWidth: Float, lineHeight: Float, paint: Paint, maxLines: Int): Float {
        val words = text.trim().split(Regex("\\s+"))
        var line = ""
        var lineY = y
        var lines = 0
        for (word in words) {
            val candidate = if (line.isBlank()) word else "$line $word"
            if (paint.measureText(candidate) <= maxWidth) line = candidate
            else {
                if (line.isNotBlank()) {
                    canvas.drawText(line, x, lineY, paint)
                    lineY += lineHeight
                    lines++
                    if (lines >= maxLines) return lineY
                }
                line = word
            }
        }
        if (line.isNotBlank() && lines < maxLines) {
            canvas.drawText(line, x, lineY, paint)
            lineY += lineHeight
        }
        return lineY
    }

    private fun formatDate(raw: String): String = AgendaDateFormat.date(raw)
}
