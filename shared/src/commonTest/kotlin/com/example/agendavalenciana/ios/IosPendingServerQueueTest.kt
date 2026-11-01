package com.example.agendavalenciana.ios

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Contract tests: simulated secure file and simulated server. No real iOS/HTTP/DB. */
class IosPendingServerQueueTest {
    private val payload = buildJsonObject { put("suggestions", true); put("discovery", "BALANCED"); put("valencian_first", true) }
    private fun action(id: String, user: Int) = IosQueuedAction(id, user, "feed", payload)
    private fun <T> immediately(f: suspend () -> T): T {
        var value: Result<T>? = null
        f.startCoroutine(object : Continuation<T> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<T>) { value = result }
        })
        return requireNotNull(value) { "The test unexpectedly suspended" }.getOrThrow()
    }

    @Test fun writesEncryptedStoreBeforeHttpAndOnlyRemovesAfterAck() {
        var file = "{\"version\":1,\"pending\":[]}"
        val deliveries = mutableListOf<String>()
        val queue = IosPendingServerQueue({ file }, { file = it; true }) { op, _ ->
            assertTrue(file.contains(op.id)) // write ahead of request
            deliveries += op.id
        }
        immediately { queue.append(action("first", 10)) }
        assertEquals(1, immediately { queue.countFor(10) })
        assertEquals(1, immediately { queue.flush(10, "bearer-10") })
        assertEquals(listOf("first"), deliveries)
        assertEquals(0, immediately { queue.countFor(10) })
    }

    @Test fun transportFailureKeepsSameActionAndRetriesWithoutChangingId() {
        var file = "{\"version\":1,\"pending\":[]}"
        var fail = true
        val received = mutableListOf<String>()
        val queue = IosPendingServerQueue({ file }, { file = it; true }) { op, _ ->
            received += op.id
            if (fail) error("offline")
        }
        immediately { queue.append(action("persistent-id", 15)) }
        assertEquals(0, immediately { queue.flush(15, "bearer-15") })
        assertTrue(file.contains("persistent-id"))
        fail = false
        assertEquals(1, immediately { queue.flush(15, "bearer-15") })
        assertEquals(listOf("persistent-id", "persistent-id"), received)
        assertFalse(file.contains("persistent-id"))
    }

    @Test fun neverSendsPreviousAccountsActions() {
        var file = "{\"version\":1,\"pending\":[]}"
        val deliveries = mutableListOf<Pair<Int,String>>()
        val queue = IosPendingServerQueue({ file }, { file = it; true }) { op, bearer ->
            deliveries += op.ownerId to bearer
        }
        immediately { queue.append(action("old", 21)); queue.append(action("new", 22)) }
        assertEquals(1, immediately { queue.flush(22, "bearer-for-22") })
        assertEquals(listOf(22 to "bearer-for-22"), deliveries)
        assertEquals(1, immediately { queue.countFor(21) })
    }

    @Test fun failedEncryptionOrUnreadableFileCannotOverwriteOrSend() {
        val queue = IosPendingServerQueue({ null }, { error("must not overwrite") }) { _, _ -> error("must not send") }
        assertFailsWith<IllegalStateException> { immediately { queue.append(action("x", 5)) } }
        assertFailsWith<IllegalStateException> { immediately { queue.flush(5, "bearer") } }
    }

    @Test fun successfulClaimTransfersOnlyGuestActions() {
        var file = "{\"version\":1,\"pending\":[]}"
        val deliveries = mutableListOf<Int>()
        val queue = IosPendingServerQueue({ file }, { file = it; true }) { op, _ -> deliveries += op.ownerId }
        immediately { queue.append(action("guest", 13)); queue.append(action("another", 999)) }
        immediately { queue.transferAfterConfirmedClaim(13, 42) }
        assertEquals(0, immediately { queue.countFor(13) })
        assertEquals(1, immediately { queue.countFor(42) })
        assertEquals(1, immediately { queue.countFor(999) })
        assertEquals(1, immediately { queue.flush(42, "registered-bearer") })
        assertEquals(listOf(42), deliveries)
    }

    @Test fun unconfirmedResponseMustNotCauseDequeue() {
        var file = "{\"version\":1,\"pending\":[]}"
        val queue = IosPendingServerQueue({ file }, { file = it; true }) { _, _ ->
            error("Missing APPLIED or ALREADY_APPLIED")
        }
        immediately { queue.append(action("not-confirmed", 7)) }
        assertEquals(0, immediately { queue.flush(7, "bearer") })
        assertEquals(1, immediately { queue.countFor(7) })
    }
}
