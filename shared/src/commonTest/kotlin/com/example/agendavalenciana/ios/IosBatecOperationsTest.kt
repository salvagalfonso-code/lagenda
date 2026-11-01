package com.example.agendavalenciana.ios

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.serialization.json.*

class IosBatecOperationsTest {
    private fun article(id: Int = 7, eventId: Int = 50, canEdit: Boolean = true): JsonObject = buildJsonObject {
        put("id", id); put("event_id", eventId); put("title", "Títol")
        put("body", "Descripció"); put("creator_user_id", 8); put("can_edit", canEdit)
        put("author_name", "Artista")
    }
    @Test fun seenMatchesPhpCanonicalKeys() {
        assertEquals("event:50", IosBatecOperations.canonicalKey("event:50:artista"))
        assertEquals("manual:7", IosBatecOperations.canonicalKey("manual:7"))
        assertFailsWith<IllegalArgumentException> { IosBatecOperations.canonicalKey("followed:artista") }
        val remote = buildJsonObject { put("ok", true); put("seen_keys", buildJsonArray {
            add("manual:7"); add("event:50"); add("event:invalid") }) }
        assertEquals(setOf("manual:7", "event:50"), IosBatecOperations.seenKeys(remote))
        IosBatecOperations.verifySeenReceipt(buildJsonObject {
            put("ok", true); put("story_key", "manual:7"); put("seen", true)
        }, "manual:7")
    }
    @Test fun permissionAndCreationWindowUseServerResponse() {
        val forbidden = IosBatecOperations.eligibility(buildJsonObject {
            put("ok", true); put("event_id", 50); put("allowed", false)
            put("organizer", false); put("reason", "Fora de finestra")
        }, 50)
        assertFalse(forbidden.allowed)
        assertEquals("Fora de finestra", forbidden.reason)
    }
    @Test fun mutationReceiptsMustMatchRequestedBatec() {
        val response = buildJsonObject { put("ok", true); put("batec", article()) }
        assertEquals(7, IosBatecOperations.verifyMutation(response, 50, "Títol", "Descripció").id)
        assertFailsWith<IllegalStateException> { IosBatecOperations.verifyMutation(response, 50, "Alt", "Descripció") }
        assertFailsWith<IllegalArgumentException> { IosBatecOperations.verifyMutation(response, 51, "Títol", "Descripció") }
        assertFailsWith<IllegalStateException> { IosBatecOperations.verifyMutation(response, 50, "Títol", "Descripció", 9) }
        assertFailsWith<IllegalStateException> {
            IosBatecOperations.verifyMutation(buildJsonObject { put("ok", true); put("batec", article(canEdit=false)) }, 50, "Títol", "Descripció")
        }
        IosBatecOperations.verifyDelete(buildJsonObject { put("ok", true); put("id", 7) }, 7)
        assertFailsWith<IllegalStateException> { IosBatecOperations.verifyDelete(buildJsonObject { put("ok", true); put("id", 8) }, 7) }
    }
    @Test fun acceptsProperLimitsAndServerList() {
        assertEquals(120, IosBatecOperations.validate("x".repeat(120), "").first.length)
        assertFailsWith<IllegalArgumentException> { IosBatecOperations.validate(" ", "") }
        assertFailsWith<IllegalArgumentException> { IosBatecOperations.validate("x".repeat(121), "") }
        assertFailsWith<IllegalArgumentException> { IosBatecOperations.validate("x", "y".repeat(1001)) }
        val list = buildJsonObject { put("ok", true); put("batecs", buildJsonArray { add(article()); add(article(9)) }) }
        assertEquals(listOf(7,9), IosBatecOperations.decodeList(list,50).map { it.id })
        assertTrue(IosBatecOperations.decodeList(list,50).all { it.canEdit })
    }
}
