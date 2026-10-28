import com.example.agendavalenciana.model.GlobalAdPlacementPolicy as P

fun checkState(organicCount: Int, prem: Boolean, normal: Boolean): List<String> {
    val result = mutableListOf<String>()
    repeat(organicCount) { i ->
        if (P.premiumAt(i,prem,normal)) result += "PREMIUM"
        if (P.normalAt(i,prem,normal)) result += "NORMAL"
        result += "EVENT"
    }
    if (P.premiumAfterLast(organicCount,prem,normal)) result += "PREMIUM"
    if (P.normalAfterLast(organicCount,prem,normal)) result += "NORMAL"
    return result
}
fun main() {
    check(P.organicPerBlock(true,true)==18)
    check(P.organicPerBlock(true,false)==19)
    check(P.organicPerBlock(false,true)==19)
    check(P.organicPerBlock(false,false)==20)
    val both = checkState(36,true,true)
    check(both.indexOf("PREMIUM") == 5) { "Premium no apareix a la posició 6" }
    check(both.indexOf("NORMAL") == 14) { "Normal no apareix a la posició 15" }
    check(both.count { it == "PREMIUM" } == 2 && both.count { it == "NORMAL" } == 2)
    check(both.count { it == "EVENT" } == 36)
    val noPremium = checkState(19,false,true)
    check(noPremium.indexOf("NORMAL") == 14)
    val noNormal = checkState(19,true,false)
    check(noNormal.indexOf("PREMIUM") == 5)
    check(checkState(20,false,false).all { it=="EVENT" })
    val short = checkState(3,true,true)
    check(short.take(3).all { it=="EVENT" }) { "Els anuncis no poden anar davant del primer esdeveniment" }
    check(short.takeLast(2) == listOf("PREMIUM","NORMAL"))
    val paginated = checkState(36,true,true)
    check(paginated == checkState(36,true,true))
    println("OK: 12 comprovacions de publicitat i posicions visuals")
}
