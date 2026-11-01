import com.example.agendavalenciana.ios.IosAdminActionPolicy
import com.example.agendavalenciana.model.IosAdminPermissions

fun main() {
    var cases = 0
    fun t(value: Boolean, msg: String) { check(value) { msg }; cases++ }
    for (role in IosAdminPermissions.roles) {
        t(IosAdminPermissions.canOpen(role) == (role == "SUPER_ADMIN"), "access $role")
        t(IosAdminPermissions.validRole(role), "backend role $role")
    }
    for (actor in listOf(-1,0,1,20)) for (target in listOf(-1,0,1,20,30)) {
        t(IosAdminPermissions.canEdit(actor,"SUPER_ADMIN",target) == (actor>0 && target>0 && actor!=target), "self/status $actor $target")
        t(!IosAdminPermissions.canEdit(actor,"USER",target), "no regular access $actor $target")
    }
    for (event in listOf(-1,0,1,99)) {
        for (decision in listOf("APPROVE", "REJECT", "DELETE", "approve")) {
            val result = event > 0 && decision == "APPROVE"
            t(IosAdminActionPolicy.validReview(event,decision,"") == result,"review blank $event $decision")
            t(IosAdminActionPolicy.validReview(event,decision,"Aquest esdeveniment infringeix la normativa") ==
                (event > 0 && decision in setOf("APPROVE","REJECT")), "review motive $event $decision")
        }
        for (action in listOf("APPROVE", "REMOVE_CONFIRMED", "RESTORE", "BAN_USER", "remove_confirmed")) {
            val base = event>0 && action in setOf("APPROVE","RESTORE")
            t(IosAdminActionPolicy.validModeration(event,action,"") == base,"moderation blank $event $action")
            t(IosAdminActionPolicy.validModeration(event,action,"Justificació extensa i concreta") ==
                (event > 0 && action in setOf("APPROVE","REMOVE_CONFIRMED","RESTORE")),"moderation notes")
        }
    }
    for (claim in listOf(-1,0,1,20)) {
        t(IosAdminActionPolicy.validProfessionalClaim(claim,true,"") == (claim>0),"accept claim")
        t(!IosAdminActionPolicy.validProfessionalClaim(claim,false,"breu"),"short reject")
        t(IosAdminActionPolicy.validProfessionalClaim(claim,false,"Manca documentació fiable") == (claim>0),"valid reject")
    }
    t(!IosAdminActionPolicy.validCampaign("", "Cos", 1),"campaign title")
    t(!IosAdminActionPolicy.validCampaign("Títol", "", 1),"campaign body")
    t(!IosAdminActionPolicy.validCampaign("Títol", "Cos", 0),"campaign count")
    t(!IosAdminActionPolicy.validCampaign("x".repeat(161), "Cos", 1),"campaign long title")
    t(!IosAdminActionPolicy.validCampaign("Títol", "x".repeat(1201), 1),"campaign long body")
    t(IosAdminActionPolicy.validCampaign("Títol", "Cos", 100),"campaign valid")
    println("PAS47: $cases assercions Kotlin/JVM correctes")
}
