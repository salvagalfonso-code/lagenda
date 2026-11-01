import com.example.agendavalenciana.ios.*
import com.example.agendavalenciana.model.IosFestivalSavedSlotPolicy
fun main() {
 val event=24
 val a=AgendaScheduleEntry("2026-11-05","19:00","Concert A","Artista A","Escenari 1",groupName="Nit",sortOrder=3)
 val b=AgendaScheduleEntry("2026-11-05","18:00","Concert B","Artista B","Escenari 2",groupName="Nit",sortOrder=1)
 val c=AgendaScheduleEntry("2026-11-06","20:00","Concert C","Artista C","Escenari 1")
 val marker=AgendaScheduleEntry("2026-11-05","17:00","Nit","","Escenari 1",groupName="Nit",itemType="activity",sortOrder=0)
 val old=AgendaScheduleEntry("2026-11-01","23:00","Passat","","Escenari 1")
 val rows=listOf(a,b,c,marker,old)
 var checks=0
 fun checkIt(value:Boolean,reason:String){if(!value) error(reason);checks++}
 checkIt(IosFestivalPopupPolicy.upcomingDays(rows,"2026-11-05")==listOf("2026-11-05","2026-11-06"),"real days")
 checkIt(IosFestivalPopupPolicy.upcomingDays(rows,"2026-11-07").isEmpty(),"expired")
 checkIt(IosFestivalPopupPolicy.initialDay(listOf("2026-11-05","2026-11-06"),c,"2026-11-05")=="2026-11-06","focused day")
 checkIt(IosFestivalPopupPolicy.initialDay(listOf("2026-11-05"),old,"2026-11-05")=="2026-11-05","past focused")
 checkIt(IosFestivalPopupPolicy.performances(rows,"2026-11-05",false, emptySet(),event)==listOf(b,a),"schedule order and marker")
 checkIt(IosFestivalPopupPolicy.performances(rows,"",false, emptySet(),event).isEmpty(),"no invented day")
 checkIt(IosFestivalPopupPolicy.isGroupMarker(marker),"group marker")
 checkIt(IosFestivalPopupPolicy.mergePublished(listOf(a.copy(details="remot")),listOf(a,b)).map { it.artist }==listOf("Artista A","Artista B"),"merge partial remote published")
 val prev=IosFestivalSavedSlotPolicy.key(event,a.date,a.time,a.artist,a.title)
 val now=IosFestivalSavedSlotPolicy.key(event,b.date,b.time,b.artist,b.title,b.location)
 checkIt(IosFestivalPopupPolicy.saved(event,a,setOf(prev)),"old slot key")
 checkIt(IosFestivalPopupPolicy.saved(event,b,setOf(now)),"stage-aware slot key")
 checkIt(!IosFestivalPopupPolicy.saved(event,a,setOf(now)),"different artist")
 checkIt(IosFestivalPopupPolicy.performances(rows,"2026-11-05",true,setOf(prev),event)==listOf(a),"only mine")
 checkIt(IosFestivalPopupPolicy.places("Recinte","Vila-real",rows).map{it.first}==listOf("Recinte","Escenari 1","Escenari 2"),"stage navigation")
 checkIt(IosFestivalPopupPolicy.places("","",emptyList()).isEmpty(),"no invented map")
 val pass=IosTicketOption(1,"Abonament",20.0,null,4,"","")
 val d5=IosTicketOption(2,"Dia5",10.0,null,4,"2026-11-05", "19:00")
 val d6=IosTicketOption(3,"Dia6",10.0,null,4,"2026-11-06", "19:00")
 checkIt(IosFestivalPopupPolicy.ticketsForDay(listOf(pass,d5,d6),"2026-11-05").map{it.id}==listOf(1,2),"day ticket and full pass")
 checkIt(IosFestivalPopupPolicy.ticketsForDay(listOf(pass,d5,d6),"").size==3,"all tickets")
 println("PAS26_KOTLIN_JVM_PASS: $checks assertions (real production policy, input models stubbed)")
}
