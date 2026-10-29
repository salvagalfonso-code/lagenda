package com.example.agendavalenciana.model

/** UTC timestamps are interpreted exactly as Android's parseServerTimestamp().
 * Invalid timestamps stay unknown: an old post must never be advertised as a new one. */
object IosBatecTime {
    fun epochMillis(raw: String): Long? {
        val text = raw.trim().replace('T', ' ')
        if (text.length < 10) return null
        val date = text.take(10)
        if (date.length != 10 || date[4] != '-' || date[7] != '-') return null
        val y = date.substring(0, 4).toIntOrNull() ?: return null
        val m = date.substring(5, 7).toIntOrNull() ?: return null
        val d = date.substring(8, 10).toIntOrNull() ?: return null
        if (y !in 1970..9999 || m !in 1..12) return null
        val monthLength = when (m) {
            2 -> if (y % 400 == 0 || (y % 4 == 0 && y % 100 != 0)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
        if (d !in 1..monthLength) return null
        val hh = text.getOrNull(11)?.let { text.substring(11).take(2).toIntOrNull() } ?: 0
        val mm = text.getOrNull(14)?.let { text.substring(14).take(2).toIntOrNull() } ?: 0
        val ss = text.getOrNull(17)?.let { text.substring(17).take(2).toIntOrNull() } ?: 0
        if (hh !in 0..23 || mm !in 0..59 || ss !in 0..59) return null
        // Proleptic Gregorian civil date -> days since 1970-01-01 (Hinnant algorithm).
        val yr = y - if (m <= 2) 1 else 0
        val era = yr / 400
        val yearOfEra = yr - era * 400
        val monthOfYear = m + if (m > 2) -3 else 9
        val dayOfYear = (153 * monthOfYear + 2) / 5 + d - 1
        val yearOfEraDay = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
        val days = era * 146097L + yearOfEraDay - 719468L
        return ((days * 24L + hh) * 60L * 60L + mm * 60L + ss) * 1000L
    }
}

/** One Batec circle per artist, matching the Android contract:
 * more than six followed artists -> exactly six circles, including grey artists without news.
 * Recent unseen news precedes already seen / old content. */
object IosBatecTimelinePolicy {
    const val LIFETIME_MILLIS = 48L * 60L * 60L * 1000L
    private val manualKey = Regex("manual:[0-9]+")
    private val eventKey = Regex("event:[0-9]+(?::.{1,190})?")
    fun isServerSeenKey(key: String): Boolean = manualKey.matches(key) || eventKey.matches(key)
    fun isBatecKey(key: String): Boolean = isServerSeenKey(key) || key.startsWith("followed:")
    fun normal(name: String): String = name.trim().lowercase()
        .replace(Regex("\\s+"), " ")
        .map { character -> when (character) {
            'à', 'á', 'â', 'ä', 'ã', 'å' -> 'a'
            'è', 'é', 'ê', 'ë' -> 'e'
            'ì', 'í', 'î', 'ï' -> 'i'
            'ò', 'ó', 'ô', 'ö', 'õ' -> 'o'
            'ù', 'ú', 'û', 'ü' -> 'u'
            'ç' -> 'c'
            'ñ' -> 'n'
            '’', '‘', 'ʼ' -> '\''
            '–', '—', '‑' -> '-'
            else -> character
        } }.joinToString("")
    fun recent(publishedAt: Long, now: Long): Boolean =
        publishedAt > 0 && now >= publishedAt && now - publishedAt < LIFETIME_MILLIS
}
