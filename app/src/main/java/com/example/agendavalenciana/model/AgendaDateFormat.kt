package com.example.agendavalenciana.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Format de presentació únic per a USER i PRO: DD/MM/YYYY.
 * Les dades, comparacions, filtres i peticions al servidor continuen utilitzant ISO 8601.
 */
object AgendaDateFormat {
    private val display = DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ROOT)
    private val dashShortYear = DateTimeFormatter.ofPattern("dd-MM-uu", Locale.ROOT)
    private val iso = DateTimeFormatter.ISO_LOCAL_DATE
    private val dayMonthFullYear = DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ROOT)
    private val dayMonthShortYear = DateTimeFormatter.ofPattern("dd/MM/uu", Locale.ROOT)
    private val dashFullYear = DateTimeFormatter.ofPattern("dd-MM-uuuu", Locale.ROOT)
    private val monthNumbers = mapOf(
        "gener" to 1, "febrer" to 2, "març" to 3, "abril" to 4, "maig" to 5, "juny" to 6,
        "juliol" to 7, "agost" to 8, "setembre" to 9, "octubre" to 10,
        "novembre" to 11, "desembre" to 12
    )
    private val wordsDate = Regex("""^(\d{1,2})\s+(?:(?:de\s+)|d['’])?([a-zàèéíïòóúüç]+)\s+(\d{4})$""", RegexOption.IGNORE_CASE)
    private val wordsRange = Regex("""^(\d{1,2})\s*[–—-]\s*(\d{1,2})\s+([a-zàèéíïòóúüç]+)\s+(\d{4})$""", RegexOption.IGNORE_CASE)
    private val spaceOrT = Regex("[ T]")
    private val embeddedIsoDate = Regex("""(?<!\d)\d{4}-\d{2}-\d{2}(?!\d)""")
    private val embeddedLegacyDate = Regex("""(?<!\d)\d{1,2}[-/]\d{1,2}[-/]\d{2,4}(?!\d)""")

    private fun parsed(value: String): LocalDate? {
        val clean = value.trim()
        if (clean.isBlank()) return null
        val portion = clean.substringBefore('T').substringBefore(' ')
        val numeric = sequenceOf(iso, display, dashShortYear, dashFullYear, dayMonthFullYear, dayMonthShortYear)
            .mapNotNull { fmt -> runCatching { LocalDate.parse(portion, fmt) }.getOrNull() }
            .firstOrNull()
        if (numeric != null) return numeric
        val natural = wordsDate.matchEntire(clean.lowercase(Locale.ROOT)) ?: return null
        val month = monthNumbers[natural.groupValues[2]] ?: return null
        return runCatching { LocalDate.of(natural.groupValues[3].toInt(), month, natural.groupValues[1].toInt()) }.getOrNull()
    }

    /** Mostra una data, sense reformatejar el valor intern ni inventar-ne cap. */
    fun date(raw: String): String = parsed(raw)?.format(display) ?: raw

    /** Excepció visual exclusiva de les targetes d'esdeveniments: «05 nov». */
    fun cardDate(raw: String): String {
        val day = parsed(raw) ?: return raw
        val shortMonths = arrayOf(
            "gen", "feb", "març", "abr", "maig", "juny",
            "jul", "ago", "set", "oct", "nov", "des"
        )
        return "${day.dayOfMonth.toString().padStart(2, '0')} ${shortMonths[day.monthValue - 1]}"
    }

    /** Mostra data i hora (si n'hi ha), també per als missatges SQL/ISO. */
    fun dateTime(raw: String): String {
        val local = parsed(raw) ?: return raw
        val original = raw.trim()
        val separator = spaceOrT.find(original)?.range?.first ?: return local.format(display)
        val time = original.substring(separator + 1).take(5)
        return if (time.matches(Regex("""\d{2}:\d{2}"""))) "${local.format(display)} · $time" else local.format(display)
    }

    /** Hora exacta en dispositiu, en les etiquetes de compra, sense canviar el timestamp. */
    fun timestamp(epochMillis: Long): String {
        val value = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
        return "${value.toLocalDate().format(display)} · ${value.format(DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT))}"
    }

    /** Normalitza camps descriptius quan contenen un rang ISO. */
    fun dateRangeLabel(raw: String): String {
        val natural = wordsRange.matchEntire(raw.trim().lowercase(Locale.ROOT))
        if (natural != null) {
            val from = "${natural.groupValues[1]} ${natural.groupValues[3]} ${natural.groupValues[4]}"
            val to = "${natural.groupValues[2]} ${natural.groupValues[3]} ${natural.groupValues[4]}"
            return "${date(from)} – ${date(to)}"
        }
        val parts = raw.split(Regex("""\s*[–—]\s*"""))
        return if (parts.size == 2) "${date(parts[0])} – ${date(parts[1])}" else date(raw)
    }

    /** Camp editable de venda: mostra dates completes en DD/MM/YYYY i conserva el tecleig parcial. */
    fun editableDateTime(raw: String): String {
        if (!Regex("""^\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}(:\d{2})?$""").matches(raw.trim())) return raw
        val isoPart = raw.take(10)
        if (toIso(isoPart) == null) return raw
        return "${date(isoPart)} ${raw.substring(11, 16)}"
    }

    /** Permet presentar també notificacions antigues que inclouen dates ISO en el cos. */
    fun datesInText(raw: String): String {
        val convertedIso = embeddedIsoDate.replace(raw) { date(it.value) }
        return embeddedLegacyDate.replace(convertedIso) { date(it.value) }
    }

    /** Entrada editable: el text parcial queda igual; en completar DD/MM/YYYY, es guarda en ISO. */
    fun inputDateIso(raw: String): String = toIso(raw) ?: raw

    /** Els horaris de venda utilitzen el mateix format visual de data, amb hora HH:mm. */
    fun inputDateTimeIso(raw: String): String {
        val clean = raw.trim()
        val match = Regex("""^(.+)[ T](\d{2}:\d{2})(:\d{2})?$""").matchEntire(clean) ?: return raw
        val normalizedDate = toIso(match.groupValues[1]) ?: return raw
        return "$normalizedDate ${match.groupValues[2]}"
    }

    /** El camp del formulari mostra DD/MM/YYYY, però en guardar continua enviant yyyy-MM-dd. */
    fun toIso(raw: String): String? = parsed(raw)?.format(iso)
}
