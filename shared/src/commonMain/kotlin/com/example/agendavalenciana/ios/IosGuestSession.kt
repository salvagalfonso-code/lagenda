package com.example.agendavalenciana.ios

import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Pas 2/48: guest issued by the same MariaDB API as Android v336.
 * An installation identifier generated on the phone is NEVER an authenticated user ID.
 */
internal data class IosGuestIdentity(val userId: Int, val guestId: String, val token: String) {
    fun persistedJson(): String = buildJsonObject {
        put("user_id", userId)
        put("guest_id", guestId)
        put("token", token)
    }.toString()

    companion object {
        /** Server contract: guest/create.php returns ok, role, user_id, guest_id, token. */
        fun fromServer(root: JsonObject): IosGuestIdentity {
            require(root.bool("ok") && root.text("role") == "GUEST") {
                "El servidor no ha confirmat un usuari convidat."
            }
            return fromValues(root)
        }

        /** A damaged or incomplete Keychain entry must never be used as an API credential. */
        fun fromKeychain(raw: String?): IosGuestIdentity? = runCatching {
            val content = Json.parseToJsonElement(raw?.takeIf(String::isNotBlank) ?: return null) as JsonObject
            fromValues(content)
        }.getOrNull()

        private fun fromValues(root: JsonObject): IosGuestIdentity {
            val userId = root.text("user_id").toIntOrNull() ?: 0
            val guestId = root.text("guest_id")
            val token = root.text("token")
            require(userId > 0 && guestId.matches(Regex("(?:[a-fA-F0-9]{32}|[a-fA-F0-9-]{36})")) &&
                token.matches(Regex("[a-fA-F0-9]{64}"))) { "Identitat de convidat invàlida." }
            return IosGuestIdentity(userId, guestId, token)
        }
    }
}

/** Prevents duplicate guest creation on simultaneous calls; Keychain is authoritative on relaunch.
 * A failed network request does not replace or delete the saved credential.
 */
internal class IosGuestSession(
    private val loadSecure: () -> String?,
    private val saveSecure: (String) -> Boolean,
    private val clearSecure: () -> Boolean,
    private val createGuest: suspend () -> JsonObject
) {
    private val mutex = Mutex()
    private var cached: IosGuestIdentity? = IosGuestIdentity.fromKeychain(loadSecure())

    fun current(): IosGuestIdentity? = cached ?: IosGuestIdentity.fromKeychain(loadSecure()).also { cached = it }

    /** Secure deletion is permitted ONLY after a matching server claim or verified
     * in-place account upgrade. A failure leaves the saved token available for retry.
     */
    suspend fun discardVerified(verified: IosGuestIdentity) {
        mutex.lock()
        try {
            val saved = current() ?: return
            check(saved == verified) { "La identitat de convidat ha canviat durant la fusió." }
            check(clearSecure()) { "No s'ha pogut netejar el Keychain després de vincular el convidat." }
            cached = null
        } finally {
            mutex.unlock()
        }
    }

    suspend fun ensure(): IosGuestIdentity {
        mutex.lock()
        try {
            current()?.let { return it }
            val created = IosGuestIdentity.fromServer(createGuest())
            check(saveSecure(created.persistedJson())) {
                "No s'ha pogut guardar la sessió del convidat al Keychain."
            }
            cached = created
            return created
        } finally {
            mutex.unlock()
        }
    }
}
