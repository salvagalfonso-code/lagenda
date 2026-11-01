import com.example.agendavalenciana.ios.IosArtistBell
import com.example.agendavalenciana.ios.IosArtistBellPolicy

fun main() {
    val p = IosArtistBellPolicy
    check(p.frequencies == setOf("IMMEDIATE", "DAILY", "WEEKLY"))
    check(p.key("  La   Maria ") == p.key("la maria"))
    check(p.parseFrequency("daily") == "DAILY")
    check(p.parseFrequency("nonsense") == "WEEKLY")
    check(p.label("IMMEDIATE") == "Al moment")
    val a = IosArtistBell("La Maria", "IMMEDIATE", true)
    val b = IosArtistBell("Al Tall", "WEEKLY", true)
    val first = p.update(emptyMap(), a)
    check(first[p.key("la maria")] == a)
    val second = p.update(first,b)
    check(second.size == 2)
    val third = p.update(second, IosArtistBell("LA MARIA","DAILY",false))
    check(third.size == 1 && p.key("Al Tall") in third)
    println("ARTIST_BELL_TESTS_OK")
}
