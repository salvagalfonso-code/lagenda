import com.example.agendavalenciana.model.IosAccountCachePolicy
import com.example.agendavalenciana.model.IosSavedAgendaPolicy
import com.example.agendavalenciana.ios.IosBatecNavigationPolicy
import com.example.agendavalenciana.ios.IosArtistBellPolicy
import com.example.agendavalenciana.ios.IosArtistBell
fun main() {
  check(IosAccountCachePolicy.key("pref", 1) != IosAccountCachePolicy.key("pref", 2))
  check(IosAccountCachePolicy.decode("~", "a|b", true).isEmpty())
  check(IosSavedAgendaPolicy.eventStillUpcoming("2026-10-20", "2026-11-02", "2026-10-29"))
  check(!IosSavedAgendaPolicy.eventStillUpcoming("2026-10-20", "2026-10-21", "2026-10-29"))
  check(IosBatecNavigationPolicy.move(5, 6, 1) == null)
  check(IosArtistBellPolicy.update(emptyMap(), IosArtistBell("Grup", "DAILY", true)).size == 1)
  println("6/6 contract smoke checks PASS")
}
