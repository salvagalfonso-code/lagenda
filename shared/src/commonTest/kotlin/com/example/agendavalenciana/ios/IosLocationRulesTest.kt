package com.example.agendavalenciana.ios

import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IosLocationRulesTest {
    @Test fun fiveSupportedRadiiAndDefault30KmMatchAndroid() {
        for (radius in listOf(5, 15, 30, 50, 100)) {
            val actual = IosLocationRules.prepare(setOf("Sagunt"), emptySet(), mapOf("Sagunt" to radius))
            assertEquals(radius, actual.towns["Sagunt"])
        }
        assertEquals(30, IosLocationRules.prepare(setOf("Sagunt"), emptySet(),
            mapOf("Sagunt" to 42)).towns["Sagunt"])
    }

    @Test fun serverConfirmsExactTownsRadiiComarquesNotJustHttp200() {
        val expected = IosLocationRules.prepare(setOf("Sagunt", "Benifairó de les Valls"),
            setOf("Camp de Morvedre"), mapOf("Sagunt" to 5,"Benifairó de les Valls" to 30))
        assertTrue(IosLocationRules.matches(expected, IosLocationRules.prepare(
            setOf("sagunt", "Benifairó de les Valls"), setOf("camp de morvedre"),
            mapOf("sagunt" to 5,"Benifairó de les Valls" to 30))))
        assertFalse(IosLocationRules.matches(expected, expected.copy(towns = mapOf("Sagunt" to 30))))
        assertFalse(IosLocationRules.matches(expected, expected.copy(comarques = emptySet())))
        assertFalse(IosLocationRules.matches(expected, expected.copy(towns = expected.towns + ("Sagunt" to 50))))
    }

    @Test fun rejectsDuplicateLocationsAndInvalidNamesBeforeOfflineQueue() {
        assertFailsWith<IllegalArgumentException> {
            IosLocationRules.prepare(setOf("Sagunt", "sagunt"),emptySet(),emptyMap())
        }
        assertFailsWith<IllegalArgumentException> {
            IosLocationRules.prepare(setOf(""),emptySet(),emptyMap())
        }
        assertFailsWith<IllegalArgumentException> {
            IosLocationRules.prepare((1..101).map { "Poble $it" }.toSet(),emptySet(),emptyMap())
        }
    }

    @Test fun explicitEmptySetIsAuthoritativeAndExistingUnknownTownIsNotRemoved() {
        val empty = IosLocationRules.prepare(emptySet(),emptySet(),emptyMap())
        assertEquals(emptyMap(),empty.towns)
        assertEquals(emptySet(),empty.comarques)
        // No silent drop of valid saved place names not in bundled offline catalog.
        assertTrue("Nom remot heretat" in IosLocationRules.prepare(setOf("Nom remot heretat"),
            emptySet(),emptyMap()).towns)
    }

    @Test fun androidGetParserDefaultsAbsentRadiiTo30() {
        val server = IosServerLocations.decode(buildJsonObject {
            put("ok", true); put("exists", true); put("revision", 3)
            put("locations", buildJsonObject {
                put("towns", buildJsonArray {
                    add(buildJsonObject { put("name", "Sagunt") })
                })
                put("comarques", buildJsonArray { add("Camp de Morvedre") })
            })
        })
        assertEquals(30, server.towns["Sagunt"])
        assertEquals(setOf("Camp de Morvedre"),server.comarques)
    }
}
