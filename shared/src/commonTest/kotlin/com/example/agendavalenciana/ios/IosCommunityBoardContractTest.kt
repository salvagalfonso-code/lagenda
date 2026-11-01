package com.example.agendavalenciana.ios

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IosCommunityBoardContractTest {
    @Test fun androidHasFiveSectionsButPreservesElevenTypes() {
        val p = IosCommunityBoardContract
        assertEquals(listOf("CAR", "TRANSPORT", "MEETUP", "HOSTING", "OTHER"),p.sections)
        assertEquals(11,p.allowedTypes.size)
        assertEquals("TRANSPORT",p.section("TRAIN"))
        assertEquals("MEETUP",p.section("COMPANY"))
        assertEquals("HOSTING",p.section("NEED_HOSTING"))
        assertEquals("OTHER",p.section("LGBTQ_FRIENDLY"))
        assertEquals("OTHER",p.serverType("HOSTING"))
        assertEquals("CAR",p.serverType("CAR"))
        assertEquals("MEETUP",p.serverType("COMPANY"))
    }
    @Test fun creatingAGroupValidatesIdentityAndLimits() {
        val p=IosCommunityBoardContract
        assertTrue(p.validCreate(123,"Cotxe des de Sagunt","CAR",4,"2026-11-20 20:30"))
        assertTrue(p.validCreate(123,"Quedada","MEETUP",0,""))
        assertFalse(p.validCreate(0,"Quedada","MEETUP",0,""))
        assertFalse(p.validCreate(123,"  ","MEETUP",0,""))
        assertFalse(p.validCreate(123,"T", "UNSUPPORTED",0,""))
        assertFalse(p.validCreate(123,"T", "CAR",101,""))
        assertFalse(p.validCreate(123,"T", "CAR",4,"demà"))
    }
    @Test fun fullBoardsStillAllowExistingMembersToWrite() {
        assertTrue(IosCommunityPolicy.canSend("FULL", "ACCEPTED"))
        assertFalse(IosCommunityPolicy.canSend("FULL", "NONE"))
        assertFalse(IosCommunityPolicy.canRequestAccess("FULL", "NONE"))
        assertFalse(IosCommunityPolicy.canSend("CLOSED", "ACCEPTED"))
    }
    @Test fun defaultNamesFollowAndroidWithoutInventingFields() {
        val p=IosCommunityBoardContract
        assertEquals("Cotxe compartit des de Sagunt - 2 places",p.defaultTitle("CAR","Sagunt",2))
        assertEquals("Transport organitzat",p.defaultTitle("TRAIN","",0))
        assertEquals("Quedada a València",p.defaultTitle("MEETUP","València",0))
        assertEquals("Compartisc allotjament a Gandia - 1 plaça",p.defaultTitle("HOSTING","Gandia",1))
        assertEquals("Altres",p.label("OTHER"))
    }
}
