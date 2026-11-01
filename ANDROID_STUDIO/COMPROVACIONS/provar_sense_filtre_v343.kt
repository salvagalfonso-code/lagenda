import com.example.agendavalenciana.ios.IosUnfilteredFeedPolicy as P
fun expect(name: String, actual: Any?, expected: Any?) {
    check(actual == expected) { "$name: expected=$expected actual=$actual" }
    println("OK $name")
}
fun main() {
    expect("first batch", P.nextCount(0, 100), 5)
    expect("second batch", P.nextCount(5, 100), 20)
    expect("later batch", P.nextCount(20, 100), 30)
    expect("last partial batch", P.nextCount(40, 47), 47)
    expect("small catalog", P.nextCount(0, 3), 3)
    expect("empty catalog", P.nextCount(0, 0), 0)
    expect("date short month", P.cardDate("2026-10-23"), "23 oct")
    expect("date another year", P.cardDate("2027-01-01"), "01 gen")
    expect("date invalid", P.cardDate("2026-42-02"), "2026-42-02")
    expect("date empty", P.cardDate(""), "")
    expect("festival next day", P.relevantDate("2026-10-27","2026-10-31","2026-10-28",listOf("2026-10-27","2026-10-29")),"2026-10-29")
    expect("festival ongoing", P.relevantDate("2026-10-27","2026-10-31","2026-10-28",emptyList()),"2026-10-28")
    expect("future normal", P.relevantDate("2026-11-07","2026-11-09","2026-10-28",emptyList()),"2026-11-07")
    expect("festival planned not ongoing", P.ongoingWithoutSchedule("2026-10-27","2026-10-31","2026-10-28",listOf("2026-10-29")), false)
    expect("festival ongoing not scheduled", P.ongoingWithoutSchedule("2026-10-27","2026-10-31","2026-10-28",emptyList()), true)
    expect("festival day only", P.festivalSummary("2026-10-29",listOf("2026-10-28" to "old", "2026-10-29" to "A", "2026-10-29" to "B", "2026-10-29" to "C"), "fallback"), "A · B · +1")
    expect("festival fallback", P.festivalSummary("2026-10-30",emptyList(),"Bandes locals"), "Bandes locals")
    expect("ticket unavailable", P.ticketLabel(false,""), "Sense venda")
    expect("ticket unpriced", P.ticketLabel(true,""), "Entrades")
    expect("ticket free", P.ticketLabel(true,"0"), "Gratuït")
    expect("ticket price", P.ticketLabel(true,"12 €"), "12 €")
}
