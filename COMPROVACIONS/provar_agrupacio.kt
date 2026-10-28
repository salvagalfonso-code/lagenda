import com.example.agendavalenciana.ios.IosNotificationGrouping
import com.example.agendavalenciana.ios.IosBatecNavigationPolicy
fun main() {
    val p = IosNotificationGrouping
    check(p.fromDeepLinkType("TICKETS") == p.TICKETS)
    check(p.fromDeepLinkType("community_message") == p.COMMUNITY)
    check(p.fromDeepLinkType("event_reminder") == p.UPCOMING)
    check(p.fromDeepLinkType("new_artist_event") == p.NEW)
    check(p.fromDeepLinkType("suggestion") == p.SUGGESTED)
    check(p.fromDeepLinkType("EVENT") == p.GENERAL)
    check(p.fromDeepLinkType("") == p.GENERAL)
    check(IosBatecNavigationPolicy.move(0, 6, 1) == 1)
    check(IosBatecNavigationPolicy.move(5, 6, 1) == null)
    check(IosBatecNavigationPolicy.move(4, 6, -1) == 3)
    check(IosBatecNavigationPolicy.move(0, 6, -1) == null)
    check(IosBatecNavigationPolicy.move(0, 0, 1) == null)
    println("PASS 12 comprovacions: categories reals i navegació Batecs")
}
