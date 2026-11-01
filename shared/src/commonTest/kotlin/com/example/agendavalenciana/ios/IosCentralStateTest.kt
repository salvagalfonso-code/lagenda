package com.example.agendavalenciana.ios

import com.example.agendavalenciana.model.DiscoveryLevel
import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Pas 4: fixtures matching sync/state.php and preferences/* GET contracts.
 * No live network and no mutation of registered/guest accounts.
 */
class IosCentralStateTest {
    private fun central(id: Int = 72): IosCentralStateSnapshot = IosCentralStateSnapshot.decode(buildJsonObject {
        put("ok", true); put("user_id", id); put("revision", 12)
        put("values", buildJsonObject {
            put("preferred_locations_guest", buildJsonObject {
                put("t", "set"); put("v", buildJsonArray { add("Sagunt"); add("Benifairó de les Valls") })
            })
            put("preferred_comarques_guest", buildJsonObject {
                put("t", "set"); put("v", buildJsonArray { add("Camp de Morvedre") })
            })
            put("preferred_location_radii_guest", buildJsonObject {
                put("t", "string"); put("v", """{"Sagunt":15}""")
            })
            put("show_suggestions", buildJsonObject { put("t", "bool"); put("v", false) })
            put("valencian_first", buildJsonObject { put("t", "bool"); put("v", true) })
            put("discovery_level", buildJsonObject { put("t", "string"); put("v", "MY_TASTES") })
            put("new_android_key", buildJsonObject { put("t", "int"); put("v", 45) })
        })
    }, id)

    @Test fun entireSnapshotIsKeptWithRevisionAndAndroidTypedValues() {
        val s = central()
        assertEquals(72, s.userId)
        assertEquals(12L, s.revision)
        assertEquals(7, s.values.size) // unknown keys are NOT discarded
        assertEquals(setOf("Sagunt", "Benifairó de les Valls"), s.strings("preferred_locations_guest"))
        assertEquals(false, s.bool("show_suggestions"))
        assertNull(s.bool("new_android_key"))
    }

    @Test fun neverApplyOtherUsersDataOrIncompleteResponses() {
        assertFailsWith<IllegalArgumentException> {
            IosCentralStateSnapshot.decode(buildJsonObject {
                put("ok", true); put("user_id", 72); put("revision", 7); put("values", buildJsonObject {})
            }, expectedUserId = 73)
        }
        assertFailsWith<IllegalStateException> {
            IosCentralStateSnapshot.decode(buildJsonObject {
                put("ok", true); put("user_id", 72); put("revision", 7)
            }, expectedUserId = 72)
        }
    }

    @Test fun hydratesGuestAndroidSharedPreferencesWithoutAnyLocalWrite() {
        val state = IosHydratedServerState(central(), null, null)
        assertEquals(setOf("Sagunt", "Benifairó de les Valls"), state.towns(null))
        assertEquals(setOf("Camp de Morvedre"), state.comarques(null))
        assertEquals(mapOf("Sagunt" to 15), state.radii(null))
        assertEquals(false, state.suggestions())
        assertEquals(true, state.valencianFirst())
        assertEquals(DiscoveryLevel.MY_TASTES, state.discovery())
    }

    @Test fun dedicatedDomainWinsIncludingAnExplicitlyEmptySelection() {
        val places = IosServerLocations.decode(buildJsonObject {
            put("ok", true); put("exists", true); put("revision", 3)
            put("locations", buildJsonObject {
                put("towns", buildJsonArray {})
                put("comarques", buildJsonArray {})
            })
        })
        val feed = IosServerFeedPreferences.decode(buildJsonObject {
            put("ok", true); put("exists", true)
            put("preferences", buildJsonObject {
                put("suggestions", true); put("discovery", "EXPLORE"); put("valencian_first", false)
            })
        })
        val state = IosHydratedServerState(central(), places, feed)
        assertTrue(state.towns(null)!!.isEmpty())
        assertTrue(state.comarques(null)!!.isEmpty())
        assertTrue(state.radii(null)!!.isEmpty())
        assertEquals(true, state.suggestions())
        assertEquals(false, state.valencianFirst())
        assertEquals(DiscoveryLevel.EXPLORE, state.discovery())
    }

    @Test fun absentDomainsDoNotDeleteCentralState() {
        val locations = IosServerLocations.decode(buildJsonObject {
            put("ok", true); put("exists", false)
            put("locations", buildJsonObject { put("towns", buildJsonArray {}); put("comarques", buildJsonArray {}) })
        })
        val state = IosHydratedServerState(central(), locations, null)
        assertTrue("Sagunt" in state.towns(null).orEmpty())
        assertEquals(false, state.suggestions())
    }
}
