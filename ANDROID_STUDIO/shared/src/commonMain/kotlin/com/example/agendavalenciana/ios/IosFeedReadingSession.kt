package com.example.agendavalenciana.ios

import kotlinx.serialization.json.*

/** Reading position is user-specific. The durable copy lives only in sync/state.php;
 * in RAM it stays responsive while the app is navigating or briefly offline.
 * The offline queue is the only local durable copy of an unacknowledged write.
 */
internal object IosFeedReadingPolicy {
    const val serverKey = "ios_feed_reading_session_v1"
    const val initial = 5
    const val second = 20
    const val step = 10
    const val oneHourMs = 60L * 60L * 1000L

    fun page(visible: Int, total: Int): Int = IosUnfilteredFeedPolicy.nextCount(visible, total)

    data class Snapshot(
        val ownerId: Int, val viewKey: String, val visibleCount: Int,
        val firstIndex: Int, val firstOffset: Int, val lastActiveAt: Long,
        val backgroundAt: Long = 0L
    ) {
        fun encode(): String = buildJsonObject {
            put("owner_id", ownerId); put("view", viewKey)
            put("count", visibleCount); put("index", firstIndex); put("offset", firstOffset)
            put("last_active_at", lastActiveAt); put("background_at", backgroundAt)
        }.toString()
    }

    fun decode(value: String?): Snapshot? = runCatching {
        val o = Json.parseToJsonElement(value.orEmpty()) as? JsonObject ?: return@runCatching null
        Snapshot(
            o["owner_id"]?.jsonPrimitive?.intOrNull ?: return@runCatching null,
            o["view"]?.jsonPrimitive?.contentOrNull ?: return@runCatching null,
            o["count"]?.jsonPrimitive?.intOrNull ?: return@runCatching null,
            o["index"]?.jsonPrimitive?.intOrNull ?: return@runCatching null,
            o["offset"]?.jsonPrimitive?.intOrNull ?: return@runCatching null,
            o["last_active_at"]?.jsonPrimitive?.longOrNull ?: return@runCatching null,
            o["background_at"]?.jsonPrimitive?.longOrNull ?: 0L
        )
    }.getOrNull()?.takeIf { it.ownerId > 0 && it.viewKey.length in 1..512 &&
        it.visibleCount >= initial && it.firstIndex >= 0 && it.firstOffset >= 0 &&
        it.lastActiveAt > 0L && it.backgroundAt >= 0L }

    fun eligible(snapshot: Snapshot?, owner: Int, view: String, now: Long): Snapshot? =
        snapshot?.takeIf { saved ->
            // Android counts only time OUT of the app. If the OS terminated the app
            // before the background callback, lastActiveAt is a conservative fallback.
            val leftAt = saved.backgroundAt.takeIf { it > 0L } ?: saved.lastActiveAt
            saved.ownerId == owner && saved.viewKey == view && now >= leftAt &&
                now - leftAt < oneHourMs
        }

    fun restoredLimit(snapshot: Snapshot?, total: Int): Int = when {
        total <= 0 -> 0
        snapshot == null -> initial.coerceAtMost(total)
        else -> maxOf(initial, snapshot.visibleCount, snapshot.firstIndex + 6).coerceAtMost(total)
    }
}

/** Not a global/user data store: one RAM-only reading session owned by the screen. */
internal class IosFeedReadingMemory {
    private var current: IosFeedReadingPolicy.Snapshot? = null
    private var initializedOwner: Int? = null
    private var appActive: Boolean = true
    fun initialized(owner: Int): Boolean = initializedOwner == owner
    fun importServer(raw: String?, owner: Int, now: Long): Boolean {
        if (initialized(owner)) return false // a late GET cannot roll back a recent scroll
        current = IosFeedReadingPolicy.decode(raw)?.takeIf { it.ownerId == owner &&
            IosFeedReadingPolicy.eligible(it, owner, it.viewKey, now) != null }
        initializedOwner = owner
        return current != null
    }
    fun restore(owner: Int, view: String, now: Long): IosFeedReadingPolicy.Snapshot? {
        val saved = current ?: return null
        // One hour counts OUTSIDE the application, not time spent reading it.
        if (appActive && saved.backgroundAt == 0L &&
            saved.ownerId == owner && saved.viewKey == view) return saved
        return IosFeedReadingPolicy.eligible(saved, owner, view, now)
    }

    fun record(owner: Int, view: String, count: Int, index: Int, offset: Int, now: Long) {
        if (owner <= 0 || view.isBlank()) return
        initializedOwner = owner
        current = IosFeedReadingPolicy.Snapshot(owner, view,
            count.coerceAtLeast(IosFeedReadingPolicy.initial), index.coerceAtLeast(0),
            offset.coerceAtLeast(0), now)
    }
    fun markBackground(now: Long) {
        appActive = false
        current = current?.copy(backgroundAt = now)
    }
    fun foreground(now: Long): Boolean {
        appActive = true
        val saved = current ?: return false
        if (IosFeedReadingPolicy.eligible(saved, saved.ownerId, saved.viewKey, now) != null) {
            current = saved.copy(backgroundAt = 0L, lastActiveAt = now)
            return false
        }
        current = null
        return true
    }
    fun discard() { current = null }
    fun snapshot(): IosFeedReadingPolicy.Snapshot? = current
}
