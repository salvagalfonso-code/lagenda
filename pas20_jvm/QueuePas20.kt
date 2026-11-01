package com.example.agendavalenciana.ios
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
fun main() = runBlocking {
  var encrypted = "{\"version\":1,\"pending\":[]}"
  var offline = true
  var sendCount=0
  val queue=IosPendingServerQueue({encrypted},{encrypted=it;true}){item,bearer->
    check(bearer=="guest-41" && item.ownerId==41)
    check(item.path=="batecs/seen.php")
    check(encrypted.contains("seen-1"))
    sendCount++
    if(offline) error("offline")
  }
  val payload=buildJsonObject { put("story_key","event:31") }
  queue.append(IosQueuedAction("seen-1",41,"post",payload,"batecs/seen.php"))
  check(queue.pendingBatecSeenActions(41).size==1)
  check(queue.pendingBatecSeenActions(42).isEmpty())
  check(queue.flush(41,"guest-41")==0 && sendCount==1)
  check(queue.countFor(41)==1)
  offline=false
  check(queue.flush(41,"guest-41")==1 && sendCount==2)
  check(queue.countFor(41)==0 && queue.pendingBatecSeenActions(41).isEmpty())
  println("PAS20_BATEC_SEEN_QUEUE_PASS: offline, retry, owner, encrypted retention, ack")
}
