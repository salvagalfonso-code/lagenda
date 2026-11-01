import com.example.agendavalenciana.ios.*
fun main() {
 var passed=0
 fun ok(value:Boolean, desc:String) { check(value){desc}; passed++ }
 val guest=IosPublisherSnapshot(6,"GUEST",serverConfirmed=true)
 val public=IosPublisherSnapshot(7,"PUBLIC",serverConfirmed=true)
 val user=IosPublisherSnapshot(7,"USER",ownedPublishedIds=setOf(42),serverConfirmed=true)
 val professional=IosPublisherSnapshot(8,"PROMOTER",professionalVerified=true,ownedPublishedIds=setOf(4),serverConfirmed=true)
 val admin=IosPublisherSnapshot(9,"SUPER_ADMIN",serverConfirmed=true)
 val suspended=IosPublisherSnapshot(8,"PROMOTER",canAutoPublish=true,suspended=true,serverConfirmed=true)
 val trusted=IosPublisherSnapshot(7,"USER",canAutoPublish=true,approvedCount=2,serverConfirmed=true)
 ok(IosPublisherRoles.normalized(null)=="GUEST","missing role")
 ok(IosPublisherRoles.normalized(" promoter ")=="PROMOTER","normalize")
 ok(!IosPublisherRoles.isProfessional(guest),"guest not pro")
 ok(!IosPublisherRoles.isProfessional(public),"public not pro")
 ok(IosPublisherRoles.isProfessional(professional),"promoter")
 ok(IosPublisherRoles.isProfessional(admin),"admin")
 ok(!IosPublisherRoles.canPublishDirectly(guest),"guest moderated")
 ok(!IosPublisherRoles.canPublishDirectly(professional),"role does not authorize direct")
 ok(IosPublisherRoles.canPublishDirectly(trusted),"trust")
 ok(!IosPublisherRoles.canPublishDirectly(suspended),"suspension")
 ok(IosPublishAction.NEW_EVENT in IosPublisherRoles.visibleActions(false,guest),"guest publish")
 ok(IosPublishAction.EDIT_EVENTS !in IosPublisherRoles.visibleActions(false,guest),"no ownership")
 ok(IosPublishAction.CREATE_AD in IosPublisherRoles.visibleActions(true,user),"ownership ads")
 ok(IosPublishAction.EDIT_EVENTS in IosPublisherRoles.visibleActions(true,user),"ownership edit")
 ok(IosPublishAction.TICKETS !in IosPublisherRoles.visibleActions(true,user),"user not seller")
 ok(IosPublishAction.ACTIVATE_SALES in IosPublisherRoles.visibleActions(true,user),"publisher sales onboarding")
 ok(IosPublishAction.CLAIM_PROFESSIONAL in IosPublisherRoles.visibleActions(false,guest),"guest sees pro login")
 ok(IosPublishAction.TICKETS !in IosPublisherRoles.visibleActions(false,professional),"guest flag never pro")
 ok(IosPublishAction.MERCH in IosPublisherRoles.visibleActions(true,professional),"seller merch")
 ok(IosPublishAction.DONATIONS in IosPublisherRoles.visibleActions(true,professional),"seller donations")
 ok(IosPublishAction.CLAIM_PROFESSIONAL in IosPublisherRoles.visibleActions(true,user),"claim")
 ok(IosPublishAction.CLAIM_PROFESSIONAL !in IosPublisherRoles.visibleActions(true,admin),"admin no claim")
 ok(IosPublishAction.REVIEW_PUBLICATIONS in IosPublisherRoles.visibleActions(true,admin),"review publications")
 ok(IosPublishAction.REVIEW_PROFESSIONAL_CLAIMS in IosPublisherRoles.visibleActions(true,admin),"review claims")
 ok(!IosPublisherRoles.allows(IosPublishAction.NEW_EVENT,false,guest.copy(serverConfirmed=false)),"no confirmation")
 ok(!IosPublisherRoles.allows(IosPublishAction.TICKETS,false,professional),"guest gated")
 ok(!IosPublisherRoles.isProfessional(professional.copy(serverConfirmed=false)),"no inferred pro")
 try { IosPublishDraft("","2026-11-03","19:00","Sagunt").validate();error("invalid") } catch (_:IllegalArgumentException) {passed++}
 try { IosPublishDraft("Concert","2026-11-03","19:00","Sagunt",category="MUSIC").validate();error("no artists") } catch (_:IllegalArgumentException) {passed++}
 IosPublishDraft("Obra","2026-11-03","19:00","Sagunt",category="THEATRE").validate();passed++
 try { IosPublishDraft("Obra","2026-11-03","19:00","Sagunt",ticketChannel="EXTERNAL",ticketUrl="http://example.org").validate();error("non https") } catch (_: IllegalArgumentException) {passed++}
 IosPublishDraft("Concert","2026-11-03","19:00","Sagunt",category="MUSIC",artists=listOf("Artista"),ticketChannel="AGENDA").validate();passed++
 try { IosSellerActivation(4).validate();error("missing consent") } catch (_: IllegalArgumentException){passed++}
 try { IosSellerActivation(4,acceptedPrivacy=true,acceptedSales=true).validate();error("missing person") } catch (_: IllegalArgumentException){passed++}
 IosSellerActivation(4,kind="PERSON",displayName="Exemple",taxId="12345678Z",address="Carrer Major",postalCode="46500",town="Sagunt",acceptedPrivacy=true,acceptedSales=true).validate();passed++
 IosSellerActivation(4,existingCompanyId=7,acceptedPrivacy=true,acceptedSales=true).validate();passed++
 println("PAS36_KOTLIN_JVM_PASS $passed")
}
