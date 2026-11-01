package com.example.agendavalenciana.ios

import kotlin.test.*
import kotlinx.serialization.json.*
import com.example.agendavalenciana.model.IosFeedExclusionPolicy

class IosVisibilityAffinityCompatibilityTest {
    @Test fun visibilityServerDiffIsReversible() {
        val before = IosVisibilitySnapshot(setOf(10), setOf("La Maria"), setOf("MUSIC"))
        val after = IosVisibilitySnapshot(setOf(11), setOf("La Maria","Al Tall"), emptySet())
        val actions = IosVisibilityAffinityContract.mutations(before, after)
        assertEquals(4, actions.size)
        assertEquals(after, actions.fold(before, IosVisibilityAffinityContract::apply))
    }
    @Test fun serverGetNeverTreatsMissingExclusionsAsEmpty() {
        assertFails { IosVisibilitySnapshot.decode(buildJsonObject { put("ok",true) }) }
    }
    @Test fun affinitySignalUsesAndroidActionAndStableDeduplication() {
        val payload = IosAffinitySignals.search("jazz", "test-uuid")
        assertEquals("SEARCH_FREE", payload["action_type"]?.jsonPrimitive?.content)
        assertEquals("test-uuid", payload["dedupe_key"]?.jsonPrimitive?.content)
    }
    @Test fun hiddenArtistDoesNotHideMixedLineup() {
        assertTrue(IosFeedExclusionPolicy.visible(3,listOf("La Maria","Al Tall"),"MUSIC",
            emptySet(),setOf("La Maria"),emptySet(),false))
        assertFalse(IosFeedExclusionPolicy.visible(3,listOf("La Maria","Al Tall"),"MUSIC",
            emptySet(),setOf("La Maria","Al Tall"),emptySet(),false))
    }
    @Test fun paidPlacementOverridesPersonalHiding() {
        assertTrue(IosFeedExclusionPolicy.visible(3,listOf("La Maria"),"MUSIC",
            setOf(3),setOf("La Maria"),setOf("MUSIC"),true))
    }
}
