package com.example.agendavalenciana.ios

import kotlinx.serialization.json.*
import com.example.agendavalenciana.model.DiscoveryLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertFailsWith

class IosForYouServerSelectionTest {
    @Test fun cachedAndroidRequiresFreshConfirmation() {
        val good = buildJsonObject {
            put("ok",true);put("hit",true);put("fresh",true)
            put("ids", JsonArray(listOf(JsonPrimitive(12),JsonPrimitive(9))))
        }
        assertEquals(listOf(12,9), IosForYouServerIds.androidCache(good)?.ids)
        assertNull(IosForYouServerIds.androidCache(buildJsonObject {
            put("ok",true);put("hit",true);put("fresh",false)
        }))
        assertFailsWith<IllegalArgumentException> {
            IosForYouServerIds.androidCache(buildJsonObject {put("ok",false)})
        }
    }
    @Test fun serverV1MustExplicitlyConfirmEngineAndCannotInventIds() {
        val good = buildJsonObject {put("ok",true);put("source","server-v1")
            put("calculated_on_server",true);put("ids",JsonArray(listOf(JsonPrimitive(3))))}
        assertEquals("server-v1",IosForYouServerIds.serverCalculation(good).source)
        assertFailsWith<IllegalArgumentException> { IosForYouServerIds.serverCalculation(buildJsonObject {
            put("ok",true);put("ids",JsonArray(emptyList())) }) }
    }
    @Test fun signedAndroidCacheCannotBeGuessed() {
        assertNull(IosForYouServerOrder.androidSignature(null,"salva"))
    }
    @Test fun payloadUsesServerOwnedFollowsAndSupportedRadius() {
        val req = IosForYouServerOrder.payload(setOf("MUSIC"),emptySet(),setOf("Sagunt"),emptySet(),
            mapOf("Sagunt" to 50,"Incorrecte" to 7), true, DiscoveryLevel.BALANCED,true)
        val prefs=req["preferences"] as JsonObject
        assertEquals(50,((prefs["radii"] as JsonObject)["Sagunt"] as JsonPrimitive).intOrNull)
        assertNull((prefs["radii"] as JsonObject)["Incorrecte"])
        assertEquals(0,(req["follows"] as JsonArray).size)
    }
}
