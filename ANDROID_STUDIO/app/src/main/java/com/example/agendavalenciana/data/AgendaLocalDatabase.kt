package com.example.agendavalenciana.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject

/**
 * Cache local persistent de l'agenda.
 *
 * 0.20.8.10ap: el servidor pot retornar diverses instàncies virtuals del mateix event_id
 * (una per dia de festival). Per això la clau de cache és event_id + feed_instance_date,
 * però totes les interaccions de negoci continuen utilitzant l'event_id real.
 */
object AgendaLocalDatabase {
    private const val DB_NAME = "agenda_cache_v4.db"
    private const val DB_VERSION = 2

    private class Helper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE events_cache (" +
                    "cache_key TEXT PRIMARY KEY, " +
                    "event_id INTEGER NOT NULL, " +
                    "feed_date TEXT, " +
                    "json TEXT NOT NULL)"
            )
            db.execSQL("CREATE INDEX idx_events_cache_event_id ON events_cache(event_id)")
            db.execSQL("CREATE INDEX idx_events_cache_feed_date ON events_cache(feed_date)")
            db.execSQL("CREATE TABLE cache_meta (meta_key TEXT PRIMARY KEY, meta_value TEXT NOT NULL)")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // És només cache: una recreació és més segura que intentar conservar un esquema
            // antic que tenia id INTEGER PRIMARY KEY i eliminava instàncies multidia.
            db.execSQL("DROP TABLE IF EXISTS events_cache")
            db.execSQL("DROP TABLE IF EXISTS cache_meta")
            onCreate(db)
        }
    }

    // v327: SQLiteOpenHelper gestiona el cicle de vida de la seua connexió compartida.
    // NO fer `use` sobre readableDatabase/writableDatabase: `use` la tanca en
    // cada consulta i obliga a reobrir-la en el següent lot, especialment al feed.
    // Els Cursor continuen tancant-se amb `use` i les transaccions amb finally.
    @Volatile private var helperInstance: Helper? = null

    private fun helper(context: Context): Helper = helperInstance ?: synchronized(this) {
        helperInstance ?: Helper(context.applicationContext).also { helperInstance = it }
    }

    private fun eventId(obj: JSONObject): Int = obj.optInt("event_id").takeIf { it > 0 } ?: obj.optInt("id")

    private fun feedDate(obj: JSONObject): String? = sequenceOf(
        obj.optString("feed_instance_date"),
        obj.optString("selected_date"),
        obj.optString("date"),
        obj.optString("event_date")
    ).map(String::trim).firstOrNull { it.isNotBlank() && !it.equals("null", ignoreCase = true) }?.take(10)

    private fun cacheKey(obj: JSONObject): String? {
        val id = eventId(obj)
        if (id <= 0) return null
        val date = feedDate(obj).orEmpty()
        return if (date.isBlank()) "event_$id" else "event_${id}_$date"
    }

    private fun insertEvent(db: SQLiteDatabase, obj: JSONObject) {
        val id = eventId(obj)
        val key = cacheKey(obj) ?: return
        val values = ContentValues().apply {
            put("cache_key", key)
            put("event_id", id)
            put("feed_date", feedDate(obj))
            put("json", obj.toString())
        }
        db.insertWithOnConflict("events_cache", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun hasEvents(context: Context): Boolean = helper(context).readableDatabase.let { db ->
        db.rawQuery("SELECT 1 FROM events_cache LIMIT 1", null).use { it.moveToFirst() }
    }

    fun revision(context: Context): Long = helper(context).readableDatabase.let { db ->
        db.rawQuery("SELECT meta_value FROM cache_meta WHERE meta_key='events_revision' LIMIT 1", null).use { c ->
            if (c.moveToFirst()) c.getString(0)?.toLongOrNull() ?: 0L else 0L
        }
    }

    fun replaceFull(context: Context, events: JSONArray, revision: Long) {
        helper(context).writableDatabase.let { db ->
            db.beginTransaction()
            try {
                db.delete("events_cache", null, null)
                for (i in 0 until events.length()) {
                    events.optJSONObject(i)?.let { insertEvent(db, it) }
                }
                setMeta(db, "events_revision", revision.toString())
                db.setTransactionSuccessful()
            } finally { db.endTransaction() }
        }
    }

    fun applyDelta(context: Context, upserts: JSONArray, deletedIds: JSONArray, revision: Long) {
        helper(context).writableDatabase.let { db ->
            db.beginTransaction()
            try {
                // Si canvia una data o el nombre de jornades, eliminem totes les instàncies
                // antigues del mateix event_id abans d'inserir el lot nou.
                val idsToReplace = linkedSetOf<Int>()
                for (i in 0 until upserts.length()) {
                    upserts.optJSONObject(i)?.let { obj -> eventId(obj).takeIf { it > 0 }?.let(idsToReplace::add) }
                }
                idsToReplace.forEach { id -> db.delete("events_cache", "event_id=?", arrayOf(id.toString())) }
                for (i in 0 until upserts.length()) {
                    upserts.optJSONObject(i)?.let { insertEvent(db, it) }
                }
                for (i in 0 until deletedIds.length()) {
                    val id = deletedIds.optInt(i)
                    if (id > 0) db.delete("events_cache", "event_id=?", arrayOf(id.toString()))
                }
                setMeta(db, "events_revision", revision.toString())
                db.setTransactionSuccessful()
            } finally { db.endTransaction() }
        }
    }

    fun deleteEvent(context: Context, eventId: Int) {
        if (eventId <= 0) return
        helper(context).writableDatabase.let { db ->
            db.delete("events_cache", "event_id=?", arrayOf(eventId.toString()))
        }
    }

    fun loadEvents(context: Context): JSONArray = helper(context).readableDatabase.let { db ->
        val out = JSONArray()
        db.rawQuery("SELECT json FROM events_cache ORDER BY feed_date, event_id", null).use { c ->
            while (c.moveToNext()) runCatching { JSONObject(c.getString(0)) }.getOrNull()?.let(out::put)
        }
        out
    }

    /**
     * Fallback de primer frame quan no hi ha snapshot personal (primer ús, canvi de compte
     * o snapshot amb IDs ja caducats). Usa l'índex de feed_date i no parseja tota la BD.
     */
    fun loadFirstEvents(context: Context, fromDate: String, limit: Int = 5): JSONArray {
        val safeLimit = limit.coerceIn(1, 64)
        val out = JSONArray()
        helper(context).readableDatabase.let { db ->
            // La consulta principal evita l'OR sobre feed_date perquè SQLite puga usar
            // directament idx_events_cache_feed_date. Les files sense data només són fallback.
            db.rawQuery(
                "SELECT json FROM events_cache WHERE feed_date >= ? ORDER BY feed_date, event_id LIMIT ?",
                arrayOf(fromDate.take(10), safeLimit.toString())
            ).use { c ->
                while (c.moveToNext()) {
                    runCatching { JSONObject(c.getString(0)) }.getOrNull()?.let(out::put)
                }
            }
            val missing = safeLimit - out.length()
            if (missing > 0) {
                db.rawQuery(
                    "SELECT json FROM events_cache WHERE feed_date IS NULL ORDER BY event_id LIMIT ?",
                    arrayOf(missing.toString())
                ).use { c ->
                    while (c.moveToNext()) {
                        runCatching { JSONObject(c.getString(0)) }.getOrNull()?.let(out::put)
                    }
                }
            }
        }
        return out
    }

    /**
     * Finestra indexada per a la segona fase del feed. Permet preparar 20/60 cards
     * sense llegir i parsejar tota la taula quan la cache ja conté milers d'events.
     */
    fun loadUpcomingEvents(context: Context, fromDate: String, limit: Int = 200): JSONArray {
        val safeLimit = limit.coerceIn(20, 500)
        val out = JSONArray()
        helper(context).readableDatabase.let { db ->
            db.rawQuery(
                "SELECT json FROM events_cache WHERE feed_date >= ? ORDER BY feed_date, event_id LIMIT ?",
                arrayOf(fromDate.take(10), safeLimit.toString())
            ).use { c ->
                while (c.moveToNext()) {
                    runCatching { JSONObject(c.getString(0)) }.getOrNull()?.let(out::put)
                }
            }
        }
        return out
    }

    /**
     * Finestra completa per a «Per a tu». A diferència de loadUpcomingEvents(), no té
     * un límit artificial de files: el filtre personal necessita veure tots els candidats
     * dels pròxims dos mesos abans de validar o reescriure el seu snapshot.
     */
    fun loadEventsInRange(context: Context, fromDate: String, toDate: String): JSONArray {
        val out = JSONArray()
        helper(context).readableDatabase.let { db ->
            db.rawQuery(
                "SELECT json FROM events_cache WHERE feed_date >= ? AND feed_date <= ? ORDER BY feed_date, event_id",
                arrayOf(fromDate.take(10), toDate.take(10))
            ).use { c ->
                while (c.moveToNext()) {
                    runCatching { JSONObject(c.getString(0)) }.getOrNull()?.let(out::put)
                }
            }
        }
        return out
    }

    /** Carrega els IDs necessaris per al primer frame i conserva totes les jornades virtuals. */
    fun loadEventsByIds(context: Context, ids: List<Int>): JSONArray {
        val clean = ids.filter { it > 0 }.distinct().take(20)
        if (clean.isEmpty()) return JSONArray()
        val found = linkedMapOf<Int, MutableList<JSONObject>>()
        helper(context).readableDatabase.let { db ->
            val placeholders = clean.joinToString(",") { "?" }
            db.rawQuery(
                "SELECT event_id, json FROM events_cache WHERE event_id IN ($placeholders) ORDER BY feed_date, event_id",
                clean.map(Int::toString).toTypedArray()
            ).use { c ->
                while (c.moveToNext()) {
                    val id = c.getInt(0)
                    runCatching { JSONObject(c.getString(1)) }.getOrNull()?.let { found.getOrPut(id) { mutableListOf() }.add(it) }
                }
            }
        }
        return JSONArray().also { out -> clean.forEach { id -> found[id].orEmpty().forEach(out::put) } }
    }

    /** v329 · Snapshot complet per usuari, sense l'antic topall artificial de 20 IDs.
     * Usa el mateix índex d'event_id i lots menuts per no topar amb els límits SQLite.
     * No retorna un catàleg global: només els IDs ja calculats de Per a tu.
     */
    fun loadSelectionByIds(context: Context, ids: List<Int>): JSONArray {
        val clean = ids.asSequence().filter { it > 0 }.distinct().take(5000).toList()
        if (clean.isEmpty()) return JSONArray()
        val found = HashMap<Int, MutableList<JSONObject>>(clean.size)
        val db = helper(context).readableDatabase
        clean.chunked(150).forEach { chunk ->
            val placeholders = chunk.joinToString(",") { "?" }
            db.rawQuery(
                "SELECT event_id, json FROM events_cache WHERE event_id IN ($placeholders) ORDER BY feed_date,event_id",
                chunk.map(Int::toString).toTypedArray()
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getInt(0)
                    runCatching { JSONObject(cursor.getString(1)) }.getOrNull()
                        ?.let { found.getOrPut(id) { mutableListOf() }.add(it) }
                }
            }
        }
        return JSONArray().also { out -> clean.forEach { id -> found[id].orEmpty().forEach(out::put) } }
    }

    private fun setMeta(db: SQLiteDatabase, key: String, value: String) {
        val values = ContentValues().apply { put("meta_key", key); put("meta_value", value) }
        db.insertWithOnConflict("cache_meta", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }
}
