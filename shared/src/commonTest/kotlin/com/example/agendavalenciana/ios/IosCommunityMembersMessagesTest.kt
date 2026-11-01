package com.example.agendavalenciana.ios

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class IosCommunityMembersMessagesTest {
    @Test fun onlyValidMembershipCanSend() {
        assertTrue(IosCommunityPolicy.canOpenChat("ACCEPTED",true))
        assertFalse(IosCommunityPolicy.canOpenChat("PENDING",true))
        assertFalse(IosCommunityPolicy.canOpenChat("ACCEPTED",false))
        assertTrue(IosCommunityPolicy.canSend("FULL","ACCEPTED"))
        assertFalse(IosCommunityPolicy.canSend("OPEN","REJECTED"))
    }
    @Test fun joinRequiresMatchingResponse() {
        assertTrue(IosCommunityPolicy.verifiedJoin("PENDING",19,19,"PENDING","PENDING"))
        assertTrue(IosCommunityPolicy.verifiedJoin("ACCEPTED",19,19,"ACCEPTED","ACCEPTED"))
        assertFalse(IosCommunityPolicy.verifiedJoin("ACCEPTED",19,22,"ACCEPTED","ACCEPTED"))
        assertFalse(IosCommunityPolicy.verifiedJoin("ACCEPTED",19,19,"PENDING","ACCEPTED"))
    }
    @Test fun sendMustMatchServerAuthorAndBody() {
        assertTrue(IosCommunityPolicy.verifiedSentMessage(10,6," Hola ",40,10,6,"Hola"))
        assertFalse(IosCommunityPolicy.verifiedSentMessage(10,6,"Hola",40,11,6,"Hola"))
        assertFalse(IosCommunityPolicy.verifiedSentMessage(10,6,"Hola",40,10,8,"Hola"))
        assertFalse(IosCommunityPolicy.verifiedSentMessage(10,6,"Hola",0,10,6,"Hola"))
        assertFalse(IosCommunityPolicy.verifiedSentMessage(10,6,"Hola",40,10,6,"Altres"))
    }
    @Test fun privateMessagesAndLength() {
        assertTrue(IosCommunityPolicy.canStartPrivateInquiry(false,"OPEN",0))
        assertFalse(IosCommunityPolicy.canStartPrivateInquiry(false,"CLOSED",0))
        assertTrue(IosCommunityPolicy.canStartPrivateInquiry(true,"CLOSED",2))
        assertFalse(IosCommunityPolicy.canStartPrivateInquiry(true,"OPEN",0))
        assertTrue(IosCommunityPolicy.validMessage("Hola"))
        assertFalse(IosCommunityPolicy.validMessage(" "))
        assertFalse(IosCommunityPolicy.validMessage("A".repeat(2001)))
        assertEquals("NONE",IosCommunityPolicy.canonicalMembership("bogus"))
    }
}
