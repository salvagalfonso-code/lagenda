package com.example.agendavalenciana.ios
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
fun main() = runBlocking {
    var encrypted = "{\"version\":1,\"pending\":[]}"
    var online = false
    val delivered = mutableListOf<String>()
    val q = IosPendingServerQueue({ encrypted }, { encrypted = it; true }) { action, token ->
        check(token == "u41")
        check(action.ownerId == 41)
        if (!online) error("offline")
        delivered += action.id
    }
    val state = buildJsonObject { put("mutations", buildJsonArray { add(buildJsonObject { put("key", "when_5") }) }) }
    val alert = buildJsonObject { put("event_id", 5); put("active", "true") }
    val artist = buildJsonObject { put("artist_name", "La Maria"); put("enabled", "true"); put("frequency", "DAILY") }
    q.append(IosQueuedAction("first",41,"state",state))
    q.append(IosQueuedAction("second",41,"post",alert,"user-state/event-alert.php"))
    q.append(IosQueuedAction("third",41,"post",artist,"notifications/artist-bell.php"))
    check(q.countFor(41)==3)
    check(q.pendingStateActions(41).size==1)
    check(q.pendingEventAlertActions(41).size==1)
    check(q.pendingArtistBellActions(41).size==1)
    check(q.flush(41,"u41")==0)
    check(q.countFor(41)==3 && delivered.isEmpty())
    check(q.countFor(42)==0)
    online=true
    check(q.flush(41,"u41")==3)
    check(delivered==listOf("first","second","third"))
    check(q.countFor(41)==0)
    check(q.pendingArtistBellActions(41).isEmpty())
    println("PAS28_QUEUE_PASS: ordered state + event bell + artist bell, offline retry, owner isolation, ack")
}
