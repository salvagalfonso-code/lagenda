package com.example.agendavalenciana.ios

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Pas 2: deterministic tests without network, real Keychain or live account creation. */
class IosGuestSessionTest {
    private val issued = buildJsonObject {
        put("ok", true)
        put("role", "GUEST")
        put("user_id", 1843)
        put("guest_id", "a".repeat(32))
        put("token", "b".repeat(64))
    }

    @Test fun serverIdentityRoundtrip() {
        val guest = IosGuestIdentity.fromServer(issued)
        assertEquals(1843, guest.userId)
        assertEquals("b".repeat(64), guest.token)
        assertEquals(guest, IosGuestIdentity.fromKeychain(guest.persistedJson()))
    }

    @Test fun rejectsMalformedOrForgedCredentials() {
        assertNull(IosGuestIdentity.fromKeychain(null))
        assertNull(IosGuestIdentity.fromKeychain("broken"))
        assertNull(IosGuestIdentity.fromKeychain("""{"user_id":42,"guest_id":"123","token":"bad"}"""))
        assertFailsWith<IllegalArgumentException> {
            IosGuestIdentity.fromServer(buildJsonObject {
                put("ok", true); put("role", "USER")
                put("user_id", 1843); put("guest_id", "a".repeat(32)); put("token", "b".repeat(64))
            })
        }
    }

    @Test fun createsOnlyOnceThenRestoresAcrossSessions() {
        var secureStored: String? = null
        var creationRequests = 0
        fun newSession() = IosGuestSession(
            loadSecure = { secureStored },
            saveSecure = { secureStored = it; true },
            clearSecure = { secureStored = null; true },
            createGuest = { creationRequests++; issued }
        )
        val initial = newSession()
        assertEquals(1843, completed { initial.ensure() }.userId)
        assertEquals(1843, completed { initial.ensure() }.userId)
        assertEquals(1843, completed { newSession().ensure() }.userId)
        assertEquals(1, creationRequests)
        assertTrue(secureStored!!.contains("\"user_id\":1843"))
    }

    @Test fun failedSecurePersistenceMustNotPretendSuccess() {
        val session = IosGuestSession(
            loadSecure = { null }, saveSecure = { false }, clearSecure = { true }, createGuest = { issued }
        )
        assertFailsWith<IllegalStateException> { completed { session.ensure() } }
        assertNull(session.current())
    }

    private fun <T> completed(block: suspend () -> T): T {
        var result: Result<T>? = null
        block.startCoroutine(object : Continuation<T> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(value: Result<T>) { result = value }
        })
        return requireNotNull(result) { "Test suspendido inesperadament" }.getOrThrow()
    }
}
