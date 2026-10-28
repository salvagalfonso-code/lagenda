package com.example.agendavalenciana.ui

import android.content.Context
import org.json.JSONArray

/**
 * La lectura del feed no és una consulta nova cada vegada que l'app recupera el focus.
 * La sessió es conserva mentre s'utilitza l'app i en absències inferiors a una hora.
 * Només hi ha una fotografia per vista activa; els canvis explícits de cerca/filtre
 * creen una vista nova, i la recàrrega manual invalida la fotografia.
 */
internal object FeedReadingSessionStore {
    private const val PREFS = "agenda_feed_reading_session_v1"
    private const val BACKGROUND_AT = "background_at"
    private const val VIEW_KEY = "view_key"
    private const val ORDER = "order"
    private const val HYDRATED = "hydrated"
    private const val INDEX = "first_item"
    private const val OFFSET = "first_offset"
    private const val ONE_HOUR_MS = 60L * 60L * 1_000L
    @Volatile private var pendingPosition: Pair<String, Position>? = null

    private fun prefs(context: Context) = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Només es comprova el temps FORA de l'app, no el temps llegint-la. */
    fun onForeground(context: Context): Boolean {
        val store = prefs(context)
        val backgroundAt = store.getLong(BACKGROUND_AT, 0L)
        val now = System.currentTimeMillis()
        val expired = backgroundAt > 0 && (now < backgroundAt || now - backgroundAt >= ONE_HOUR_MS)
        if (expired) {
            pendingPosition = null
            store.edit().clear().apply()
        } else {
            store.edit().remove(BACKGROUND_AT).apply()
        }
        return expired
    }

    fun onBackground(context: Context) {
        // El darrer desplaçament pot haver-se produït menys de 350 ms abans de
        // compartir. El bolcat directe evita perdre fins i tot eixos píxels.
        flushPosition(context)
        prefs(context).edit().putLong(BACKGROUND_AT, System.currentTimeMillis()).apply()
    }

    fun onExplicitClose(context: Context) {
        pendingPosition = null
        prefs(context).edit().clear().apply()
    }

    data class Snapshot(val eventKeys: List<String>, val hydrated: Boolean)
    data class Position(val index: Int, val offset: Int)

    fun loadSnapshot(context: Context, viewKey: String): Snapshot? {
        val store = prefs(context)
        if (store.getString(VIEW_KEY, null) != viewKey) return null
        val raw = store.getString(ORDER, null) ?: return null
        return runCatching {
            val array = JSONArray(raw)
            Snapshot((0 until array.length()).map { array.getString(it) }, store.getBoolean(HYDRATED, false))
        }.getOrNull()
    }

    fun saveSnapshot(context: Context, viewKey: String, keys: List<String>, hydrated: Boolean) {
        if (keys.isEmpty()) return
        val array = JSONArray()
        keys.forEach { array.put(it) }
        val store = prefs(context)
        val sameView = store.getString(VIEW_KEY, null) == viewKey
        val edit = store.edit()
            .putString(VIEW_KEY, viewKey)
            .putString(ORDER, array.toString())
            .putBoolean(HYDRATED, hydrated)
        if (!sameView) {
            pendingPosition = null
            edit.remove(INDEX).remove(OFFSET)
        }
        edit.apply()
    }

    fun loadPosition(context: Context, viewKey: String): Position? {
        pendingPosition?.let { (key, position) -> if (key == viewKey) return position }
        val store = prefs(context)
        if (store.getString(VIEW_KEY, null) != viewKey || !store.contains(INDEX)) return null
        return Position(store.getInt(INDEX, 0).coerceAtLeast(0), store.getInt(OFFSET, 0).coerceAtLeast(0))
    }

    fun cachePosition(viewKey: String, index: Int, offset: Int) {
        pendingPosition = viewKey to Position(index.coerceAtLeast(0), offset.coerceAtLeast(0))
    }

    fun flushPosition(context: Context) {
        pendingPosition?.let { (key, position) -> savePosition(context, key, position.index, position.offset) }
    }

    fun savePosition(context: Context, viewKey: String, index: Int, offset: Int) {
        val store = prefs(context)
        if (store.getString(VIEW_KEY, null) != viewKey) return
        store.edit().putInt(INDEX, index.coerceAtLeast(0)).putInt(OFFSET, offset.coerceAtLeast(0)).apply()
    }

    fun clearSnapshot(context: Context) {
        pendingPosition = null
        prefs(context).edit().remove(VIEW_KEY).remove(ORDER).remove(HYDRATED)
            .remove(INDEX).remove(OFFSET).apply()
    }
}
