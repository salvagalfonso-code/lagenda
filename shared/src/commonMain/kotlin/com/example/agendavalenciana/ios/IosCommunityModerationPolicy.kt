package com.example.agendavalenciana.ios

/** Step 33: safe client-side guards, with PHP always authoritative for permissions. */
object IosCommunityModerationPolicy {
    fun validReport(groupId:Int,messageId:Int,reason:String):Boolean =
        groupId>0 && messageId>=0 && reason.trim().isNotEmpty() && reason.trim().length<=500

    fun validBlock(actorId:Int,targetId:Int):Boolean = actorId>0 && targetId>0 && actorId!=targetId
    fun verifiedBlock(expected:Boolean,actuallyBlocked:Boolean):Boolean = expected==actuallyBlocked

    fun validManagement(groupId:Int,action:String,targetId:Int,messageId:Int,actorId:Int):Boolean =
        groupId>0 && actorId>0 && when(action) {
            "BAN_MEMBER","UNBAN_MEMBER" -> targetId>0 && actorId!=targetId && messageId==0
            "DELETE_MESSAGE" -> messageId>0 && targetId==0
            else -> false
        }

    fun eligibleManagement(data:IosCommunityManagement,action:String,targetId:Int):Boolean = when(action) {
        "BAN_MEMBER" -> data.members.any { it.id==targetId && it.role=="MEMBER" }
        "UNBAN_MEMBER" -> data.bans.any { it.id==targetId } &&
            data.members.any { it.id==targetId && it.role=="MEMBER" }
        "DELETE_MESSAGE" -> true // server checks message/group and creator
        else -> false
    }
    fun verifiedBan(bannedIds:Set<Int>,targetId:Int,action:String):Boolean = when(action) {
        "BAN_MEMBER" -> targetId in bannedIds
        "UNBAN_MEMBER" -> targetId !in bannedIds
        else -> false
    }
    fun validAdminAction(action:String):Boolean = action in setOf(
        "DISMISS","REVIEW","DELETE_MESSAGE","CLOSE_GROUP","BAN_MEMBER")
    fun adminStatus(action:String):String = when(action) {
        "DISMISS" -> "DISMISSED"
        "REVIEW" -> "REVIEWED"
        "DELETE_MESSAGE","CLOSE_GROUP","BAN_MEMBER" -> "ACTIONED"
        else -> "INVALID"
    }
    fun eligibleAdmin(report:IosCommunityReport,action:String):Boolean = when(action) {
        "DELETE_MESSAGE" -> report.groupId>0 && report.messageId>0
        "CLOSE_GROUP" -> report.groupId>0
        "BAN_MEMBER" -> report.groupId>0 && report.reportedUserId>0
        "DISMISS","REVIEW" -> true
        else -> false
    }
    fun verifiedReportStatus(reports:List<IosCommunityReport>,id:Int,expected:String):Boolean =
        reports.any {it.id==id && it.status==expected}
}
