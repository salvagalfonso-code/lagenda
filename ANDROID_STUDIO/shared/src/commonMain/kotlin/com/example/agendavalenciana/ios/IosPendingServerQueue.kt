package com.example.agendavalenciana.ios

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*

/** Step 6: encrypted, durable transport only; MariaDB remains the source of truth.
 * SecureStore implementation is native CryptoKit AES-GCM, protected with a Keychain key.
 * No bearer token or personal cache is stored in this queue.
 */
internal data class IosQueuedAction(
    val id: String,
    val ownerId: Int,
    val kind: String,
    val payload: JsonObject,
    val path: String? = null
) {
    init {
        require(id.isNotBlank() && ownerId > 0 && kind in setOf("state", "locations", "feed", "post"))
        if (kind == "post") require(path in setOf("events/save.php", "user-state/event-alert.php", "user-state/follow.php", "visibility/index.php", "affinities/record.php", "batecs/seen.php", "notifications/artist-bell.php", "mail/preferences-api.php"))
    }
    fun encode(): JsonObject = buildJsonObject {
        put("id", id); put("owner_id", ownerId); put("kind", kind); put("payload", payload)
        path?.let { put("path", it) }
    }
    companion object {
        fun decode(root: JsonObject): IosQueuedAction = IosQueuedAction(
            id = root["id"]?.jsonPrimitive?.contentOrNull ?: error("Falta identificador"),
            ownerId = root["owner_id"]?.jsonPrimitive?.intOrNull ?: error("Falta propietari"),
            kind = root["kind"]?.jsonPrimitive?.contentOrNull ?: error("Falta tipus"),
            payload = root["payload"] as? JsonObject ?: error("Falta petició"),
            path = root["path"]?.jsonPrimitive?.contentOrNull
        )
    }
}

/** Only a confirmed, identity-matching server response may remove an action. */
internal class IosPendingServerQueue(
    private val loadSecure: () -> String?,
    private val saveSecure: (String) -> Boolean,
    private val deliverAndVerify: suspend (IosQueuedAction, String) -> Unit
) {
    private val lock = Mutex()
    private fun read(): MutableList<IosQueuedAction> {
        // null means decrypt/storage failure, NEVER treat it as an empty queue.
        val data = loadSecure() ?: error("No es pot llegir la cua xifrada; no se sobreescriurà")
        val obj = Json.parseToJsonElement(data) as? JsonObject ?: error("Cua corrupta")
        require(obj["version"]?.jsonPrimitive?.intOrNull == 1) { "Versió de cua incompatible" }
        val array = obj["pending"] as? JsonArray ?: error("Falten accions")
        require(array.size <= 1000) { "Cua excessivament gran" }
        val items = array.map { IosQueuedAction.decode(it as? JsonObject ?: error("Acció corrupta")) }
        require(items.distinctBy { it.id }.size == items.size) { "Identificadors de cua duplicats" }
        return items.toMutableList()
    }
    private fun persist(actions: List<IosQueuedAction>) {
        require(actions.size <= 1000) { "Cua completa; no es perd cap acció" }
        val json = buildJsonObject {
            put("version", 1)
            put("pending", buildJsonArray { actions.forEach { add(it.encode()) } })
        }.toString()
        require(json.length <= 1_000_000) { "Cua completa; no es perd cap acció" }
        check(saveSecure(json)) { "No s'ha pogut guardar la cua xifrada" }
    }
    suspend fun append(action: IosQueuedAction) = lock.withLock {
        val current = read()
        require(current.none { it.id == action.id }) { "Acció repetida" }
        current.add(action)
        persist(current) // write-ahead, before ANY HTTP request
    }
    /** Returns how many writes were acknowledged; stops at first failure per identity.
     * No operation owned by another account can be sent in this method.
     */
    suspend fun flush(ownerId: Int, bearer: String): Int = lock.withLock {
        require(ownerId > 0 && bearer.isNotBlank())
        val actions = read()
        var acknowledged = 0
        for (action in actions.toList()) {
            if (action.ownerId != ownerId) continue
            try {
                deliverAndVerify(action, bearer)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                break // transport, identity, validation or server failure: retain action
            }
            // CryptoKit persistence must succeed before we consider dequeue final.
            val remaining = actions.filterNot { it.id == action.id }
            persist(remaining)
            actions.removeAll { it.id == action.id }
            acknowledged++
        }
        acknowledged
    }
    suspend fun contains(id: String): Boolean = lock.withLock { read().any { it.id == id } }
    suspend fun countFor(ownerId: Int): Int = lock.withLock { read().count { it.ownerId == ownerId } }
    suspend fun hasPendingKind(ownerId: Int, kind: String): Boolean = lock.withLock {
        read().any { it.ownerId == ownerId && it.kind == kind }
    }
    /** Account deletion has been confirmed by the server: erase offline writes for
     * precisely that user, never those of another account or an unclaimed guest. */
    suspend fun discardDeletedAccount(ownerId: Int) = lock.withLock {
        require(ownerId > 0)
        val all = read()
        if (all.any { it.ownerId == ownerId }) persist(all.filterNot { it.ownerId == ownerId })
    }
    /** Queued follow/unfollow commands are ordered and scoped to the current identity. */
    suspend fun pendingFollowActions(ownerId: Int): List<IosQueuedAction> = lock.withLock {
        read().filter { it.ownerId == ownerId && it.kind == "post" && it.path == "user-state/follow.php" }
    }
    /** Latest unconfirmed save/unsave of each ID wins in the displayed projection. */
    suspend fun pendingSavedActions(ownerId: Int): List<IosQueuedAction> = lock.withLock {
        read().filter { it.ownerId == ownerId && it.kind == "post" && it.path == "events/save.php" }
    }
    suspend fun pendingEventAlertActions(ownerId: Int): List<IosQueuedAction> = lock.withLock {
        read().filter { it.ownerId == ownerId && it.kind == "post" && it.path == "user-state/event-alert.php" }
    }
    suspend fun pendingArtistBellActions(ownerId: Int): List<IosQueuedAction> = lock.withLock {
        read().filter { it.ownerId == ownerId && it.kind == "post" && it.path == "notifications/artist-bell.php" }
    }
    suspend fun pendingStateActions(ownerId: Int): List<IosQueuedAction> = lock.withLock {
        read().filter { it.ownerId == ownerId && it.kind == "state" }
    }

    suspend fun pendingVisibilityActions(ownerId: Int): List<IosQueuedAction> = lock.withLock {
        read().filter { it.ownerId == ownerId && it.kind == "post" && it.path == "visibility/index.php" }
    }
    suspend fun pendingBatecSeenActions(ownerId: Int): List<IosQueuedAction> = lock.withLock {
        read().filter { it.ownerId == ownerId && it.kind == "post" && it.path == "batecs/seen.php" }
    }
    /** Call only AFTER guest/claim.php or register.php confirms the merge.
     * The registered session must not accidentally replay old guest mutations as a new guest.
     */
    suspend fun transferAfterConfirmedClaim(guestId: Int, registeredId: Int) = lock.withLock {
        require(guestId > 0 && registeredId > 0)
        val pending = read()
        if (pending.any { it.ownerId == guestId }) {
            persist(pending.map { if (it.ownerId == guestId) it.copy(ownerId = registeredId) else it })
        }
    }
}

/** Operation still exists in encrypted storage; NEVER display as server-saved. */
internal class IosQueuedForRetryException : IllegalStateException(
    "Acció guardada temporalment i pendent de sincronitzar amb el servidor."
)
