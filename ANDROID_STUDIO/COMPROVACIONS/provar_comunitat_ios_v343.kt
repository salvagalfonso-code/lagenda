import com.example.agendavalenciana.ios.IosCommunityPolicy as P

fun main() {
    check(P.displayMessage(11,setOf(5)))
    check(!P.displayMessage(5,setOf(5)))
    check(P.canRequestAccess("OPEN","NONE"))
    check(!P.canRequestAccess("OPEN","PENDING"))
    check(!P.canRequestAccess("CLOSED","NONE"))
    check(P.canSend("OPEN","ACCEPTED"))
    check(!P.canSend("CLOSED","ACCEPTED"))
    check(P.isCreator(3,3))
    check(!P.canModerateMember(3,4,5))
    check(!P.canModerateMember(3,3,3))
    check(P.canModerateMember(3,3,5))
    check(P.canSeeAdminQueue("super_admin"))
    check(!P.canSeeAdminQueue("PUBLIC"))
    println("13 comprovacions de regles Comunitat correctes")
}
