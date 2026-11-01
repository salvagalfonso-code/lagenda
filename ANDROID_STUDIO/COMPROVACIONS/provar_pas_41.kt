package com.example.agendavalenciana.ios

private var tests = 0
private fun ensure(value: Boolean) { check(value) { "Fallada en la prova ${tests+1}" }; tests++ }
fun main() {
    val draft = IosPublishDraft(title="Festival", subtitle="La trobada",date="2026-12-05",time="19:30",
        town="Vila-real", category="MUSIC", artists=listOf("Grup A","Grup B"),
        organizers=listOf("Org"),endDate="2026-12-06",endTime="01:00", comarca="Plana Baixa",
        venue="Sala", styles=listOf("Folk"),price=12,ticketChannel="EXTERNAL",
        ticketUrl="https://agendaval.com/entrades", description="Activitat familiar")
    val actual = IosPublishedEvent(53,"Festival","published","2026-12-05","Vila-real","MUSIC",
        "Activitat familiar","La trobada","19:30","2026-12-06","01:00","Plana Baixa","Sala",
        listOf("Grup A","Grup B"),listOf("Org"),listOf("Folk"),12,true,"https://agendaval.com/entrades")
    ensure(IosPublicationEditPolicy.confirmed(53,draft,actual))
    ensure(!IosPublicationEditPolicy.confirmed(54,draft,actual))
    ensure(!IosPublicationEditPolicy.confirmed(53,draft,actual.copy(status="pending_review")))
    ensure(!IosPublicationEditPolicy.confirmed(53,draft,null))
    ensure(!IosPublicationEditPolicy.confirmed(53,draft,actual.copy(date="2026-12-07")))
    ensure(!IosPublicationEditPolicy.confirmed(53,draft,actual.copy(time="15:00")))
    ensure(!IosPublicationEditPolicy.confirmed(53,draft,actual.copy(endDate="2026-12-08")))
    ensure(!IosPublicationEditPolicy.confirmed(53,draft,actual.copy(endTime="23:00")))
    ensure(!IosPublicationEditPolicy.confirmed(53,draft,actual.copy(subtitle="")))
    ensure(!IosPublicationEditPolicy.confirmed(53,draft,actual.copy(town="Castelló")))
    ensure(!IosPublicationEditPolicy.confirmed(53,draft,actual.copy(comarca="")))
    ensure(!IosPublicationEditPolicy.confirmed(53,draft,actual.copy(venue="")))
    ensure(!IosPublicationEditPolicy.confirmed(53,draft,actual.copy(description="")))
    ensure(!IosPublicationEditPolicy.confirmed(53,draft,actual.copy(category="DANCE")))
    ensure(!IosPublicationEditPolicy.confirmed(53,draft,actual.copy(artists=listOf("Grup A"))))
    ensure(!IosPublicationEditPolicy.confirmed(53,draft,actual.copy(organizers=emptyList())))
    ensure(!IosPublicationEditPolicy.confirmed(53,draft,actual.copy(styles=emptyList())))
    ensure(!IosPublicationEditPolicy.confirmed(53,draft,actual.copy(price=15)))
    ensure(!IosPublicationEditPolicy.confirmed(53,draft,actual.copy(ticketEnabled=false)))
    ensure(!IosPublicationEditPolicy.confirmed(53,draft,actual.copy(ticketUrl="https://fake.example")))
    ensure(IosPublicationEditPolicy.posterConfirmed("https://example.com/poster.jpg?x=4","https://example.com/poster.jpg?x=7"))
    ensure(!IosPublicationEditPolicy.posterConfirmed("http://example.com/p.jpg","http://example.com/p.jpg"))
    ensure(!IosPublicationEditPolicy.posterConfirmed("https://example.com/a.jpg","https://example.com/b.jpg"))
    val slot = IosSpecialScheduleSlot(18,53,"Actuació","2026-12-05","19:30","20:30","Escenari A","Grup A","Música en viu")
    slot.validate(); ensure(true)
    ensure(IosSpecialSchedulePolicy.confirmed(slot,listOf(slot),emptySet()))
    ensure(!IosSpecialSchedulePolicy.confirmed(slot,listOf(slot.copy(id=19)),emptySet()))
    ensure(!IosSpecialSchedulePolicy.confirmed(slot,listOf(slot.copy(stage="B")),emptySet()))
    ensure(!IosSpecialSchedulePolicy.confirmed(slot,listOf(slot.copy(artist="B")),emptySet()))
    ensure(!IosSpecialSchedulePolicy.confirmed(slot,listOf(slot.copy(startTime="18:00")),emptySet()))
    ensure(!IosSpecialSchedulePolicy.confirmed(slot,listOf(slot.copy(details="")),emptySet()))
    val candidate=slot.copy(id=0)
    ensure(IosSpecialSchedulePolicy.confirmed(candidate,listOf(slot),emptySet()))
    ensure(!IosSpecialSchedulePolicy.confirmed(candidate,listOf(slot),setOf(18)))
    ensure(!IosSpecialSchedulePolicy.confirmed(candidate,listOf(slot.copy(title="Altra")),emptySet()))
    ensure(!IosSpecialSchedulePolicy.confirmed(candidate,listOf(slot.copy(date="2026-12-06")),emptySet()))
    ensure(runCatching { slot.copy(date="bad").validate() }.isFailure)
    ensure(runCatching { slot.copy(endTime="18:00").validate() }.isFailure)
    ensure(runCatching { slot.copy(imageUrl="javascript:unsafe").validate() }.isFailure)
    ensure(runCatching { slot.copy(eventId=0).validate() }.isFailure)
    ensure(runCatching { slot.copy(id=-1).validate() }.isFailure)
    println("PAS41_JVM_PASS assertions=$tests")
}
