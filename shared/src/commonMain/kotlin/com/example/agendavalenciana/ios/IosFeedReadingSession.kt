package com.example.agendavalenciana.ios

/** Posició real del LazyColumn: inclou capçalera, targeta i offset en píxels. */
internal data class IosFeedReadingPosition(val index: Int, val offset: Int, val loaded: Int)

/**
 * Equivalent iOS de FeedReadingSessionStore d'Android.
 * Una sessió de lectura per vista, amb límit d'UNA HORA fora de l'app.
 * L'hora no compta mentre l'usuari està llegint (encara que passen hores).
 * No conserva dades privades ni tokens; la clau incorpora l'usuari i el mode de feed.
 */
internal object IosFeedReadingSession {
    const val ONE_HOUR_MS = 60L * 60L * 1000L
    private const val PREFIX = "ios_feed_reading_v350_"
    private const val VIEW = PREFIX + "view"
    private const val INDEX = PREFIX + "index"
    private const val OFFSET = PREFIX + "offset"
    private const val LOADED = PREFIX + "loaded"
    const val BACKGROUND_AT = PREFIX + "background_at"

    // Cache actualitzat en cada moviment, sense fer I/O en cada píxel.
    private var live: Pair<String, IosFeedReadingPosition>? = null

    /** En primer pla, les lectures inicials també rebutgen una absència caducada. */
    fun load(view: String, now: Long = currentEpochMillis()): IosFeedReadingPosition? {
        if (expired(now)) { clear(); return null }
        live?.let { (key, value) -> if (key == view) return value }
        if (platformReadPublicCache(VIEW) != view) return null
        val index = platformReadPublicCache(INDEX).toIntOrNull() ?: return null
        val offset = platformReadPublicCache(OFFSET).toIntOrNull() ?: return null
        val loaded = platformReadPublicCache(LOADED).toIntOrNull() ?: return null
        return IosFeedReadingPosition(index.coerceAtLeast(0), offset.coerceAtLeast(0),
            loaded.coerceAtLeast(5))
    }

    fun cache(view: String, index: Int, offset: Int, loaded: Int) {
        live = view to IosFeedReadingPosition(index.coerceAtLeast(0), offset.coerceAtLeast(0),
            loaded.coerceAtLeast(5))
    }

    fun flush() {
        val (view, position) = live ?: return
        platformWritePublicCache(VIEW, view)
        platformWritePublicCache(INDEX, position.index.toString())
        platformWritePublicCache(OFFSET, position.offset.toString())
        platformWritePublicCache(LOADED, position.loaded.toString())
    }

    fun background(now: Long = currentEpochMillis()) {
        flush()  // Inclou els píxels desplaçats en els últims 350 ms.
        platformWritePublicCache(BACKGROUND_AT, now.toString())
    }

    /** Retorna true exclusivament si la lectura ha caducat fora de l'aplicació. */
    fun foreground(now: Long = currentEpochMillis()): Boolean {
        val hasExpired = expired(now)
        if (hasExpired) clear()
        platformWritePublicCache(BACKGROUND_AT, "")
        return hasExpired
    }

    fun clear() {
        live = null
        platformWritePublicCache(VIEW, "")
        platformWritePublicCache(INDEX, "")
        platformWritePublicCache(OFFSET, "")
        platformWritePublicCache(LOADED, "")
        platformWritePublicCache(BACKGROUND_AT, "")
    }

    fun expired(now: Long = currentEpochMillis()): Boolean {
        val backgroundAt = platformReadPublicCache(BACKGROUND_AT).toLongOrNull() ?: return false
        return now < backgroundAt || now - backgroundAt >= ONE_HOUR_MS
    }
}
