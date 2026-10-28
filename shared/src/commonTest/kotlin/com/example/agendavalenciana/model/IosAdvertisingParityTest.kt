package com.example.agendavalenciana.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Mirrors the EXACT same placement policy used by Android's AgendaApp.kt. */
class IosAdvertisingParityTest {
    @Test fun premiumAndNormalAreAtVisualPositionsSixAndFifteen() {
        val rendered = mutableListOf<String>()
        repeat(18) { index ->
            if (GlobalAdPlacementPolicy.premiumAt(index, true, true)) rendered += "P"
            if (GlobalAdPlacementPolicy.normalAt(index, true, true)) rendered += "N"
            rendered += "O"
        }
        assertEquals(5, rendered.indexOf("P"))
        assertEquals(14, rendered.indexOf("N"))
        assertEquals(20, rendered.size)
    }

    @Test fun eachSinglePlacementKeepsItsContractedPosition() {
        val premiumOnly = (0 until 19).firstOrNull {
            GlobalAdPlacementPolicy.premiumAt(it,true,false)
        }
        val normalOnly = (0 until 19).firstOrNull {
            GlobalAdPlacementPolicy.normalAt(it,false,true)
        }
        assertEquals(5, premiumOnly)
        assertEquals(14, normalOnly)
    }

    @Test fun shortFinalPageNeverStartsWithInsertedPaidSlot() {
        assertTrue((0 until 3).none {
            GlobalAdPlacementPolicy.premiumAt(it, true,true) || GlobalAdPlacementPolicy.normalAt(it,true,true)
        })
        assertTrue(GlobalAdPlacementPolicy.premiumAfterLast(3,true,true))
        assertTrue(GlobalAdPlacementPolicy.normalAfterLast(3,true,true))
    }
}
