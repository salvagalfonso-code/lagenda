package com.example.agendavalenciana.ios
fun main() {
 var n=0
 fun test(value:Boolean) { check(value); n++ }
 fun mustFail(f:()->Unit) { test(runCatching { f() }.isFailure) }
 val id=IosProfileParity.validateIdentity(" Salva_24 "," info@example.org "," Vila-real ")
 test(id.first=="Salva_24");test(id.second=="info@example.org");test(id.third=="Vila-real")
 for(x in listOf("x","bad name",""))mustFail { IosProfileParity.validateIdentity(x,"info@example.org","") }
 for(x in listOf("a@","abc","x@@example.org","x @example.org"))mustFail { IosProfileParity.validateIdentity("tester",x,"") }
 mustFail { IosProfileParity.validateIdentity("tester","info@example.org","a".repeat(191)) }
 test(IosProfileParity.validateProfile(" A "," B ")==("A" to "B"))
 mustFail { IosProfileParity.validateProfile("","") }
 mustFail { IosProfileParity.validateProfile("A","b".repeat(3001)) }
 val a=IosMyProfile("Name","bio","url","Town","MUSIC","Co","Cast","Publisher","Books","Works","Spaces")
 test(IosProfileParity.sameProfessionalFields(a,a.copy(displayName="Other")))
 test(!IosProfileParity.sameProfessionalFields(a,a.copy(discipline="Theatre")))
 test(!IosProfileParity.sameProfessionalFields(a,a.copy(spacesText="new")))
 test(!IosProfileParity.sameProfessionalFields(a,a.copy(castText="new")))
 test(!IosProfileParity.sameProfessionalFields(a,a.copy(publisherName="new")))
 test(!IosProfileParity.sameProfessionalFields(a,a.copy(booksText="new")))
 test(!IosProfileParity.sameProfessionalFields(a,a.copy(worksText="new")))
 test(!IosProfileParity.sameProfessionalFields(a,a.copy(companyName="new")))
 test(IosProfileParity.sameUsername("USER", " user "))
 test(!IosProfileParity.sameUsername("", "USER"))
 test(!IosProfileParity.sameUsername("USER","OTHER"))
 println("PAS34_KOTLIN_JVM_PASS: $n assertions")
}
