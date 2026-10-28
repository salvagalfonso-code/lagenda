package com.example.agendavalenciana.ui

import com.example.agendavalenciana.model.AgendaDateFormat

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.agendavalenciana.model.Concert
import com.example.agendavalenciana.model.PurchasedTicket
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

internal object TicketPdf {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    fun writeToUri(context: Context, uri: Uri, concert: Concert, ticket: PurchasedTicket): Result<Unit> = runCatching {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            val pdf = createDocument(context, concert, ticket)
            try {
                pdf.writeTo(out)
            } finally {
                pdf.close()
            }
        } ?: error("No s'ha pogut obrir el fitxer PDF.")
    }

    fun share(context: Context, concert: Concert, ticket: PurchasedTicket): Result<Unit> = runCatching {
        val dir = File(context.cacheDir, "shared_tickets").apply { mkdirs() }
        val file = File(dir, "entrada_${safeName(concert.title)}_${ticket.code.takeLast(6)}.pdf")
        file.outputStream().use { out ->
            val pdf = createDocument(context, concert, ticket)
            try {
                pdf.writeTo(out)
            } finally {
                pdf.close()
            }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Entrada · ${concert.title}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir entrada"))
    }

    fun suggestedFileName(concert: Concert, number: Int): String =
        "Entrada_${safeName(concert.title)}_${number}.pdf"

    fun displayCode(code: String): String = code.uppercase().chunked(4).joinToString("-")

    private fun createDocument(context: Context, concert: Concert, ticket: PurchasedTicket): PdfDocument {
        val pdf = PdfDocument()
        val page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
        drawPage(context, page.canvas, concert, ticket)
        pdf.finishPage(page)
        return pdf
    }

    private fun drawPage(context: Context, canvas: Canvas, concert: Concert, ticket: PurchasedTicket) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawColor(android.graphics.Color.WHITE)

        val posterHeight = 190
        decodePoster(context, concert.posterUri)?.let { poster ->
            val source = centerCropRect(poster.width, poster.height, PAGE_WIDTH, posterHeight)
            canvas.drawBitmap(poster, source, Rect(0, 0, PAGE_WIDTH, posterHeight), paint)
            poster.recycle()
            paint.color = android.graphics.Color.argb(125, 0, 0, 0)
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), posterHeight.toFloat(), paint)
        } ?: run {
            paint.color = android.graphics.Color.rgb(26, 35, 126)
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), posterHeight.toFloat(), paint)
        }

        paint.color = android.graphics.Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 16f
        canvas.drawText("AGENDA VALENCIANA", 36f, 42f, paint)
        paint.textSize = 27f
        drawWrappedText(canvas, concert.title, 36f, 92f, PAGE_WIDTH - 72f, 31f, paint, 3)

        var y = 225f
        paint.color = android.graphics.Color.rgb(28, 28, 28)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 18f
        canvas.drawText("Entrada", 36f, y, paint)
        y += 34f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 13f
        val eventDate = formatDate(concert.date)
        val eventLine = listOf(eventDate, concert.time.takeIf { it.isNotBlank() }, concert.venue.takeIf { it.isNotBlank() }, concert.town.takeIf { it.isNotBlank() })
            .filterNotNull().joinToString(" · ")
        drawWrappedText(canvas, eventLine, 36f, y, PAGE_WIDTH - 72f, 18f, paint, 2)
        y += 46f

        val optionLine = listOfNotNull(
            ticket.ticketOptionName.takeIf { it.isNotBlank() },
            ticket.sessionDate.takeIf { it.isNotBlank() },
            ticket.sessionTime.takeIf { it.isNotBlank() },
            ticket.sector.takeIf { it.isNotBlank() },
            ticket.offer.takeIf { it.isNotBlank() }
        ).joinToString(" · ")
        if (optionLine.isNotBlank()) {
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 12f
            canvas.drawText("Modalitat", 36f, y, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            drawWrappedText(canvas, optionLine, 115f, y, PAGE_WIDTH - 151f, 16f, paint, 2)
            y += 34f
        }

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        canvas.drawText("Titular", 36f, y, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(ticket.buyerDisplayName.ifBlank { ticket.buyerUsername }, 115f, y, paint)
        y += 27f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Estat", 36f, y, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(ticket.status.label, 115f, y, paint)
        y += 28f
        if (concert.ticketPrice >= 0) {
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Import", 36f, y, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(if (concert.ticketPrice == 0) "Gratuïta" else "${concert.ticketPrice} €", 115f, y, paint)
            y += 38f
        }

        val qr = createQr(ticket.code, 720)
        val qrSize = 245
        val left = (PAGE_WIDTH - qrSize) / 2
        canvas.drawBitmap(qr, null, Rect(left, y.toInt(), left + qrSize, y.toInt() + qrSize), paint)
        qr.recycle()
        y += qrSize + 27f

        paint.color = android.graphics.Color.BLACK
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        paint.textSize = 13f
        val code = displayCode(ticket.code)
        val codeWidth = paint.measureText(code)
        canvas.drawText(code, (PAGE_WIDTH - codeWidth) / 2f, y, paint)
        y += 35f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 10.5f
        paint.color = android.graphics.Color.DKGRAY
        drawWrappedText(
            canvas,
            "Presenta este QR a l'accés. La primera validació correcta inutilitza qualsevol còpia posterior del mateix codi.",
            46f,
            y,
            PAGE_WIDTH - 92f,
            15f,
            paint,
            4
        )
        y += 70f
        paint.textSize = 9f
        canvas.drawText("Codi únic d'entrada · ${ticket.code.length} caràcters", 46f, y, paint)
    }

    private fun decodePoster(context: Context, raw: String?): Bitmap? {
        val value = raw?.trim().orEmpty()
        if (value.isBlank()) return null
        return runCatching {
            val uri = Uri.parse(value)
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                ?: if (uri.scheme == "file") BitmapFactory.decodeFile(uri.path) else null
        }.getOrNull()
    }

    private fun centerCropRect(srcW: Int, srcH: Int, dstW: Int, dstH: Int): Rect {
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

    private fun drawWrappedText(canvas: Canvas, text: String, x: Float, y: Float, maxWidth: Float, lineHeight: Float, paint: Paint, maxLines: Int) {
        val words = text.split(Regex("\\s+"))
        var line = ""
        var lineY = y
        var lines = 0
        for (word in words) {
            val candidate = if (line.isBlank()) word else "$line $word"
            if (paint.measureText(candidate) <= maxWidth) {
                line = candidate
            } else {
                if (line.isNotBlank()) {
                    canvas.drawText(line, x, lineY, paint)
                    lineY += lineHeight
                    lines++
                    if (lines >= maxLines) return
                }
                line = word
            }
        }
        if (line.isNotBlank() && lines < maxLines) canvas.drawText(line, x, lineY, paint)
    }

    private fun createQr(value: String, size: Int): Bitmap {
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

    private fun formatDate(raw: String): String = AgendaDateFormat.date(raw)

    private fun safeName(value: String): String = value
        .trim()
        .replace(Regex("[^A-Za-z0-9À-ÿ_-]+"), "_")
        .trim('_')
        .take(60)
        .ifBlank { "esdeveniment" }
}
