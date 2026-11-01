package com.example.agendavalenciana.ios
fun main() {
 val p=IosCommunityBoardContract
 check(p.sections.size==5); check(p.allowedTypes.size==11)
 val section=mapOf("CAR" to "CAR","TRAIN" to "TRANSPORT","TRANSPORT" to "TRANSPORT","MEETUP" to "MEETUP","COMPANY" to "MEETUP","HOSTING" to "HOSTING","NEED_HOSTING" to "HOSTING","WOMEN_ONLY" to "OTHER","VEG_FRIENDLY" to "OTHER","LGBTQ_FRIENDLY" to "OTHER","OTHER" to "OTHER")
 section.forEach { (t, s) -> check(p.section(t)==s) {"$t/$s"} }
 check(p.serverType("HOSTING")=="OTHER")
 check(p.serverType("NEED_HOSTING")=="OTHER")
 check(p.serverType("TRAIN")=="TRANSPORT")
 check(p.serverType("MEETUP")=="MEETUP")
 check(p.validCreate(41,"Tauló", "CAR",0,""))
 check(p.validCreate(41,"Tauló", "HOSTING",3,"2027-02-14 19:15"))
 check(!p.validCreate(0,"Tauló", "CAR",0,""))
 check(!p.validCreate(41,"", "CAR",0,""))
 check(!p.validCreate(41,"Tauló", "CAR",101,""))
 check(!p.validCreate(41,"Tauló", "DOG",0,""))
 check(!p.validCreate(41,"Tauló", "CAR",0,"tomorrow"))
 check(p.defaultTitle("CAR","Sagunt",2)=="Cotxe compartit des de Sagunt - 2 places")
 check(p.defaultTitle("HOSTING","Gandia",1)=="Compartisc allotjament a Gandia - 1 plaça")
 check(p.defaultTitle("MEETUP","València",0)=="Quedada a València")
 check(p.label("LGBTQ_FRIENDLY")=="LGTBIQ+ friendly")
 check(IosCommunityPolicy.canSend("FULL","ACCEPTED"))
 check(!IosCommunityPolicy.canSend("FULL","NONE"))
 check(!IosCommunityPolicy.canRequestAccess("FULL","NONE"))
 println("PAS31_KOTLIN_JVM_PASS: 11 types, five sections, legacy PHP type, UI labels, creation checks (29 assertions)")
}
