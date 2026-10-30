import com.example.agendavalenciana.ios.*
fun main() {
    val guest = IosPublisherMenuPolicy.menu(null,false)
    check(guest.heading=="Publicar")
    check(guest.actions==listOf(IosPublishAction.NEW_EVENT))
    check(guest.reviewActions.isEmpty())
    check(guest.claimLabel=="Soc organitzador o artista")
    val publicHasEvents = IosPublisherMenuPolicy.menu("PUBLIC",true)
    check(publicHasEvents.actions==listOf(IosPublishAction.NEW_EVENT,IosPublishAction.EDIT_EVENTS,IosPublishAction.CREATE_AD))
    val professionalEmpty = IosPublisherMenuPolicy.menu("PROFESSIONAL",false)
    check(professionalEmpty.actions == listOf(IosPublishAction.NEW_EVENT,IosPublishAction.TICKETS,IosPublishAction.MERCH,IosPublishAction.DONATIONS))
    check(professionalEmpty.claimLabel=="Reclamar un altre grup o perfil")
    val artistPro = IosPublisherMenuPolicy.menu("GROUP",true)
    check(artistPro.actions.size==6)
    check(artistPro.reviewActions.isEmpty())
    for (role in listOf("AGENCY","LABEL","VENUE","PROMOTER","ORGANIZER","PUBLISHER","PROFESSIONAL")) {
        check(IosPublisherMenuPolicy.isProfessional(role)) { role }
        check(IosPublisherMenuPolicy.menu(role,true).actions.size==6)
        check(IosPublisherMenuPolicy.menu(role,true).reviewActions.isEmpty())
    }
    val admin = IosPublisherMenuPolicy.menu("SUPER_ADMIN",true)
    check(admin.heading=="Panell professional")
    check(admin.actions.size==6)
    check(admin.reviewActions==listOf(IosPublishAction.REVIEW_PUBLICATIONS,IosPublishAction.PROFESSIONAL_CLAIMS))
    check(admin.claimLabel==null)
    val suspended = IosPublisherMenuPolicy.menu("PROFESSIONAL",true,true)
    check(suspended.suspended && suspended.subtitle.contains("suspesa"))
    check(!IosPublisherMenuPolicy.isProfessional("PUBLIC"))
    check(!IosPublisherMenuPolicy.isProfessional("USER"))
    check(!IosPublisherMenuPolicy.isProfessional("HACKER"))
    println("14 validacions de permisos i contingut del menú + superades")
}
