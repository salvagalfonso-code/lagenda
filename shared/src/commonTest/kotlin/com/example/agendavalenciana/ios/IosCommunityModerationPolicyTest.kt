package com.example.agendavalenciana.ios

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class IosCommunityModerationPolicyTest {
    @Test fun reportAndBlockValidation() {
        assertTrue(IosCommunityModerationPolicy.validReport(1,12,"Reason"))
        assertFalse(IosCommunityModerationPolicy.validReport(1,12,"  "))
        assertFalse(IosCommunityModerationPolicy.validReport(1,12,"x".repeat(501)))
        assertFalse(IosCommunityModerationPolicy.validBlock(6,6))
        assertTrue(IosCommunityModerationPolicy.verifiedBlock(false,false))
    }
    @Test fun managementAndRoleGuards() {
        val members=IosCommunityManagement(listOf(
            IosCommunityMember(1,"creator","CREATOR","ACCEPTED"),
            IosCommunityMember(2,"member","MEMBER","ACCEPTED")),listOf(IosCommunityUser(2,"member")))
        assertFalse(IosCommunityModerationPolicy.validManagement(1,"BAN_MEMBER",1,0,1))
        assertFalse(IosCommunityModerationPolicy.eligibleManagement(members,"BAN_MEMBER",1))
        assertTrue(IosCommunityModerationPolicy.eligibleManagement(members,"UNBAN_MEMBER",2))
        assertTrue(IosCommunityModerationPolicy.verifiedBan(emptySet(),2,"UNBAN_MEMBER"))
    }
    @Test fun reportStatusesAndEligibility() {
        val r=IosCommunityReport(3,5,0,0,"group","","report","","OPEN")
        assertFalse(IosCommunityModerationPolicy.eligibleAdmin(r,"DELETE_MESSAGE"))
        assertFalse(IosCommunityModerationPolicy.eligibleAdmin(r,"BAN_MEMBER"))
        assertEquals("DISMISSED",IosCommunityModerationPolicy.adminStatus("DISMISS"))
        assertEquals("ACTIONED",IosCommunityModerationPolicy.adminStatus("DELETE_MESSAGE"))
        assertTrue(IosCommunityModerationPolicy.verifiedReportStatus(listOf(r.copy(status="REVIEWED")),3,"REVIEWED"))
    }
}
