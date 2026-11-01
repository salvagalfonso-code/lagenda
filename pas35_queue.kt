package com.example.agendavalenciana.ios
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
fun main() = runBlocking {
    var secure = "{\"version\":1,\"pending\":[]}"
    val sent = mutableListOf<String>()
    val queue = IosPendingServerQueue({ secure }, { secure = it; true }) { action, _ -> sent += action.id }
    val payload = buildJsonObject { put("key", "follows") }
    queue.append(IosQueuedAction("owner-a", 100, "state", payload))
    queue.append(IosQueuedAction("owner-b", 200, "state", payload))
    queue.append(IosQueuedAction("owner-a2", 100, "state", payload))
    check(queue.countFor(100)==2 && queue.countFor(200)==1)
    queue.discardDeletedAccount(100)
    check(queue.countFor(100)==0 && queue.countFor(200)==1)
    check(queue.flush(100,"oldBearer")==0)
    check(queue.flush(200,"otherBearer")==1)
    check(sent==listOf("owner-b"))
    println("PAS35_QUEUE_PASS: deleted-account purge, other-account retention, no replay")
}
