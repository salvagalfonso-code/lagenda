package com.example.agendavalenciana.ios
fun main() {
    val p = IosReminderPolicy
    check(p.validLocalDateTime("2026-11-01T19:45"))
    check(!p.validLocalDateTime("2026-11-01T25:61"))
    check(!p.validLocalDateTime("2026/11/01 19:45"))
    check(!p.validLocalDateTime("2026-11-01T20:7"))
    check(!p.validLocalDateTime("2026-11-01T20:77"))
    check(!p.validLocalDateTime("2026-11-01"))
    p.requireFuture(5,true,8000,7999)
    p.requireFuture(5,false,0,7999)
    check(runCatching {p.requireFuture(5,true,7999,7999)}.isFailure)
    check(runCatching {p.requireFuture(-1,false,0,7999)}.isFailure)
    check(p.updatedIds(setOf("2"),5,true)==setOf("2","5"))
    check(p.updatedIds(setOf("2","5"),5,false)==setOf("2"))
    check(p.updatedIds(null,5,true)==null)
    check(p.updatedIds(emptySet(),5,true)==setOf("5"))
    check(p.updatedIds(setOf("foo","-3"),5,true)==setOf("5"))
    check(p.projectedAlerts(setOf(2,3),listOf(2 to false,4 to true))==setOf(3,4))
    check(p.projectedAlerts(emptySet(),listOf(4 to true,4 to false))==emptySet<Int>())
    check(p.projectedAlerts(emptySet(),listOf(4 to true,4 to false,4 to true))==setOf(4))
    println("PAS28_POLICY_PASS 18 casos")
}
