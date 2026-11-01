package com.example.agendavalenciana.ios

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Pas 3: no network. Real MariaDB transaction remains the responsibility of guest/claim.php. */
class IosGuestAccountLinkerTest {
    private val guestId = 142
    private val registeredId = 937
    private val token = "b".repeat(64)
    private val issued = buildJsonObject {
        put("ok", true)
        put("role", "GUEST")
        put("user_id", guestId)
        put("guest_id", "a".repeat(32))
        put("token", token)
    }

    private fun store(): Pair<IosGuestSession, () -> String?> {
        var data: String? = IosGuestIdentity.fromServer(issued).persistedJson()
        return IosGuestSession(
            loadSecure = { data },
            saveSecure = { data = it; true },
            clearSecure = { data = null; true },
            createGuest = { issued }
        ) to { data }
    }

    @Test fun mergesExistingAccountOnlyAfterMatchingServerConfirmation() {
        val (guest, persisted) = store()
        var serverCalls = 0
        val linker = IosGuestAccountLinker(guest) { submittedToken, bearer ->
            assertEquals(token, submittedToken)
            assertEquals("registered-bearer", bearer)
            serverCalls++
            response(guestId, registeredId)
        }
        assertTrue(completed { linker.claimInto(registeredId, "registered-bearer") })
        assertEquals(1, serverCalls)
        assertNull(persisted())
        assertNull(guest.current())
        assertFalse(completed { linker.claimInto(registeredId, "registered-bearer") })
        assertEquals(1, serverCalls)
    }

    @Test fun failedOrMismatchedServerResultPreservesGuest() {
        val (guest, persisted) = store()
        val failed = IosGuestAccountLinker(guest) { _, _ -> error("HTTP 503") }
        assertFailsWith<IllegalStateException> {
            completed { failed.claimInto(registeredId, "registered-bearer") }
        }
        assertNotNull(persisted())
        val wrong = IosGuestAccountLinker(guest) { _, _ -> response(guestId, 555) }
        assertFailsWith<IllegalArgumentException> {
            completed { wrong.claimInto(registeredId, "registered-bearer") }
        }
        assertEquals(guestId, guest.current()?.userId)
        assertNotNull(persisted())
    }

    @Test fun alreadyClaimedIsIdempotentAndAllowsCleanup() {
        val (guest, persisted) = store()
        val linker = IosGuestAccountLinker(guest) { _, _ -> response(guestId, registeredId, already = true) }
        assertTrue(completed { linker.claimInto(registeredId, "registered-bearer") })
        assertNull(persisted())
    }

    @Test fun inPlaceRegistrationKeepsUserIdAndSkipsClaimEndpoint() {
        val (guest, persisted) = store()
        val linker = IosGuestAccountLinker(guest) { _, _ -> error("The endpoint must not be called") }
        assertTrue(completed { linker.discardUpgradedGuest(guestId) })
        assertNull(persisted())
        assertFalse(completed { linker.discardUpgradedGuest(guestId) })
    }

    @Test fun guestTokenIsRetainedIfKeychainCannotErase() {
        var data: String? = IosGuestIdentity.fromServer(issued).persistedJson()
        val guest = IosGuestSession(
            loadSecure = { data }, saveSecure = { data = it; true },
            clearSecure = { false }, createGuest = { issued }
        )
        val linker = IosGuestAccountLinker(guest) { _, _ -> response(guestId, registeredId) }
        assertFailsWith<IllegalStateException> {
            completed { linker.claimInto(registeredId, "registered-bearer") }
        }
        assertNotNull(data)
        assertEquals(guestId, guest.current()?.userId)
    }

    private fun response(guest: Int, registered: Int, already: Boolean = false): JsonObject = buildJsonObject {
        put("ok", true); put("merged_guest_user_id", guest)
        put("into_user_id", registered); put("already_claimed", already)
    }

    private fun <T> completed(block: suspend () -> T): T {
        var result: Result<T>? = null
        block.startCoroutine(object : Continuation<T> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(value: Result<T>) { result = value }
        })
        return requireNotNull(result) { "Suspension unexpectedly asynchronous" }.getOrThrow()
    }
}
