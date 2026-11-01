package com.example.agendavalenciana.ios
import kotlinx.serialization.json.*
fun main(){
 val d=IosNotificationCenterSettings()
 check(d.centralMutations().size==13)
 check(d.centralMutations().containsKey("alert_community"))
 check(d.centralMutations().containsKey("alert_followed"))
 check(d.centralMutations().containsKey("ios_push_quiet_end"))
 check(d.emailFrequency=="NONE")
 check(runCatching {d.copy(emailFrequency="DAILY").validate(true)}.isFailure)
 check(runCatching {d.copy(emailFrequency="DAILY").validate(false)}.isSuccess)
 check(runCatching {d.copy(quietStart=1441).validate(false)}.isFailure)
 check(IosNotificationCenterSettings.displayQuietTime(8*60)=="08:00")
 val s=d.withCentral(IosCentralStateSnapshot(mapOf("alert_community" to false, "alert_followed" to false, "ios_push_enabled" to true),mapOf("ios_push_quiet_start" to 22*60L)))
 check(!s.community&&!s.followed&&s.pushEnabled&&s.quietStart==1320)
 check(s.dayBefore)
 val email=buildJsonObject {put("preferences",buildJsonObject {
  put("recommendations_enabled",true);put("followed_artists_enabled",true)
  put("saved_reminders_enabled",false);put("ads_enabled",false);put("digest_frequency","DAILY")}) }
 val read=s.withEmail(email)
 check(read.emailFrequency=="DAILY"&&read.emailRecommendations&&read.emailArtists)
 check(!read.emailReminders&&!read.emailAds)
 check(read.emailPayload()["digest_frequency"]?.jsonPrimitive?.content=="DAILY")
 check(read.centralMutations()["alert_community"]?.get("v")?.jsonPrimitive?.booleanOrNull==false)
 check(IosNotificationCenterSettings().withEmail(buildJsonObject{}).emailFrequency=="NONE")
 check(read.copy(emailFrequency="WEEKLY").validate(true)==Unit)
 println("PAS29_POLICY_PASS: 17 afirmacions")
}
