package com.example.agendavalenciana.ios

import kotlinx.serialization.json.*
import kotlin.test.*

class IosSavedEventContractTest {
    private val ev = AgendaEventSummary(283, "Obra de teatre", "2026-11-25", "20:00",
        "Sagunt", "Teatre Romà", "Teatre", "", "", styles = listOf("teatre"),
        artists = listOf("Companyia X"), feedOccurrenceDate = "2026-12-02")
    private fun receipt(saved: Boolean) = buildJsonObject {
        put("ok", true); put("saved", saved); put("saved_count", 11)
    }
    private fun list(vararg ids: Int) = buildJsonObject {
        put("ok", true); put("event_ids", buildJsonArray { ids.forEach { add(it) } })
    }

    @Test fun usesOriginalEventIdAndAndroidContext() {
        val body = IosSavedEventContract.payload(ev, true, "Sagunt", "Camp de Morvedre")
        assertEquals(283, body["event_id"]?.jsonPrimitive?.int)
        assertEquals("Sagunt", body["user_town"]?.jsonPrimitive?.content)
        assertEquals("Companyia X", body["primary_artist"]?.jsonPrimitive?.content)
        assertEquals(true, body["saved"]?.jsonPrimitive?.boolean)
    }
    @Test fun verifiesBothSaveAndUnsaveBeforeDequeue() {
        for (saved in listOf(true, false)) {
            val body = IosSavedEventContract.payload(ev, saved)
            IosSavedEventContract.verifyPost(receipt(saved), body)
            IosSavedEventContract.verifyCanonicalList(if (saved) list(283, 7) else list(7), body)
        }
    }
    @Test fun rejectsMismatchedOrMissingReceipt() {
        assertFails { IosSavedEventContract.verifyPost(receipt(false), IosSavedEventContract.payload(ev, true)) }
        assertFails { IosSavedEventContract.verifyPost(buildJsonObject { put("ok", true); put("saved", true) },
            IosSavedEventContract.payload(ev, true)) }
        assertFails { IosSavedEventContract.verifyCanonicalList(list(7), IosSavedEventContract.payload(ev, true)) }
        assertFails { IosSavedEventContract.verifyCanonicalList(list(283), IosSavedEventContract.payload(ev, false)) }
    }
    @Test fun pendingWritesAreOrderedAndIsolatedByOwnerInQueue() {
        val save = IosSavedEventContract.payload(ev, true)
        val unsave = IosSavedEventContract.payload(ev, false)
        assertEquals(setOf(7), IosSavedEventContract.project(setOf(7,283), listOf(unsave)))
        assertEquals(setOf(7,283), IosSavedEventContract.project(setOf(7), listOf(save)))
        assertEquals(setOf(7), IosSavedEventContract.project(setOf(7), listOf(save,unsave)))
        assertEquals(setOf(7,283), IosSavedEventContract.project(setOf(7), listOf(unsave,save)))
    }
    @Test fun rejectsInvalidIdentifiers() {
        assertFails { IosSavedEventContract.payload(ev.copy(id=0), true) }
        assertFails { IosSavedEventContract.expected(buildJsonObject { put("event_id", -1); put("saved", true) }) }
    }
}
