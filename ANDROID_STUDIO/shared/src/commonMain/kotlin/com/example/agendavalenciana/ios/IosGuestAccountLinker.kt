package com.example.agendavalenciana.ios

import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.JsonObject

/** Pas 3: Android v336 contract with guest/claim.php.
 * Only the PHP server merges the user's history, follows, saved events and memberships.
 * The client validates the destination and erases the old guest bearer only on success.
 */
internal class IosGuestAccountLinker(
    private val guestSession: IosGuestSession,
    private val onConfirmedBeforeDiscard: suspend (guestId: Int, accountId: Int) -> Unit = { _, _ -> },
    private val claimOnServer: suspend (guestToken: String, registeredBearer: String) -> JsonObject
) {
    private val mutex = Mutex()

    suspend fun claimInto(registeredUserId: Int, registeredBearer: String): Boolean {
        require(registeredUserId > 0 && registeredBearer.isNotBlank()) { "Cal un compte registrat." }
        mutex.lock()
        try {
            val guest = guestSession.current() ?: return false
            if (guest.userId == registeredUserId) {
                // register.php already upgraded this guest in place; the authenticated
                // response from auth/me.php or login.php confirms the same users.id.
                onConfirmedBeforeDiscard(guest.userId, registeredUserId)
                guestSession.discardVerified(guest)
                return true
            }
            val response = claimOnServer(guest.token, registeredBearer)
            require(response.bool("ok") &&
                response.text("merged_guest_user_id").toIntOrNull() == guest.userId &&
                response.text("into_user_id").toIntOrNull() == registeredUserId) {
                "El servidor no ha confirmat la fusió del convidat amb este compte."
            }
            // The server also accepts already_claimed=true for safe retries following
            // a lost HTTP response or an earlier failed Keychain deletion.
            onConfirmedBeforeDiscard(guest.userId, registeredUserId)
            guestSession.discardVerified(guest)
            return true
        } finally {
            mutex.unlock()
        }
    }

    suspend fun discardUpgradedGuest(registeredUserId: Int): Boolean {
        require(registeredUserId > 0)
        mutex.lock()
        try {
            val guest = guestSession.current() ?: return false
            if (guest.userId != registeredUserId) return false
            onConfirmedBeforeDiscard(guest.userId, registeredUserId)
            guestSession.discardVerified(guest)
            return true
        } finally {
            mutex.unlock()
        }
    }
}
