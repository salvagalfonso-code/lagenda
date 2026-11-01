package com.example.agendavalenciana.ios
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
fun main() = runBlocking {
 var encrypted="{\"version\":1,\"pending\":[]}"
 var online=false
 val transmitted=mutableListOf<String>()
 val queue=IosPendingServerQueue({encrypted},{encrypted=it;true}){action,bearer->
  check(bearer=="owner42")
  check(action.ownerId==42)
  if(!online) error("offline")
  transmitted+=action.id
 }
 val state=buildJsonObject{put("mutations",buildJsonArray{add(buildJsonObject{put("key","alert_followed")})})}
 val email=buildJsonObject{put("digest_frequency","DAILY");put("recommendations_enabled","true")}
 queue.append(IosQueuedAction("a",42,"state",state))
 queue.append(IosQueuedAction("b",42,"post",email,"mail/preferences-api.php"))
 check(queue.countFor(42)==2)
 check(queue.flush(42,"owner42")==0)
 check(queue.countFor(42)==2)
 check(queue.countFor(7)==0)
 online=true
 check(queue.flush(42,"owner42")==2)
 check(transmitted==listOf("a","b"))
 check(queue.countFor(42)==0)
 check(runCatching {IosQueuedAction("c",42,"post",email,"other/url.php")}.isFailure)
 println("PAS29_QUEUE_PASS: mail + state, offline, ordering, bearer isolation, allowlist")
}
