package com.example.agendavalenciana.ios
import kotlinx.serialization.json.*

fun main() {
  var cases = 0
  fun yes(x:Boolean, label:String) { check(x) {label}; cases++ }
  fun fail(label:String, block:()->Unit) {val r=runCatching(block);yes(r.isFailure,label)}
  fun obj(vararg xs:Pair<String,JsonElement>):JsonObject=JsonObject(mapOf(*xs))
  fun s(v:String):JsonPrimitive=JsonPrimitive(v)
  fun rootFor(id:Int=13, event:Int=31, title:String="Voluntariat", body:String="Sona música", canEdit:Boolean=true):JsonObject =
    obj("id" to s("$id"),"event_id" to s("$event"),"title" to s(title),"body" to s(body),
        "can_edit" to s("$canEdit"),"creator_user_id" to s("42"),"author_name" to s("Prova"),"published_at" to s("2026-10-31 21:00:00"))
  val good=obj("ok" to s("true"),"batec" to rootFor())
  yes(IosBatecOperations.canonicalKey("manual:234")=="manual:234","manual key")
  yes(IosBatecOperations.canonicalKey("event:14:Artista")=="event:14","event artist converted PHP")
  fail("invalid manual key"){ IosBatecOperations.canonicalKey("manual:a") }
  fail("invalid seen placeholder"){ IosBatecOperations.canonicalKey("followed:xy") }
  yes(IosBatecOperations.canonicalKey("event:14:artist;DROP")=="event:14","event artist suffix safely ignored")
  yes(IosBatecOperations.validate(" ExemPle "," cos ")== ("ExemPle" to "cos"),"trim")
  fail("blank title"){IosBatecOperations.validate(" ","")}
  fail("long title"){IosBatecOperations.validate("x".repeat(121),"")}
  fail("long body"){IosBatecOperations.validate("ok","x".repeat(1001))}
  yes(IosBatecOperations.validate("x".repeat(120),"y".repeat(1000)).first.length==120,"boundary")
  val seen=obj("ok" to s("true"),"seen_keys" to JsonArray(listOf(s("manual:1"),s("event:3"),s("event:wrong"),s("other:2"))))
  yes(IosBatecOperations.seenKeys(seen)==setOf("manual:1","event:3"),"seen parsed filtered")
  fail("seen missing array") { IosBatecOperations.seenKeys(obj("ok" to s("true"))) }
  fail("seen HTTP error") { IosBatecOperations.seenKeys(obj("ok" to s("false"),"seen_keys" to JsonArray(emptyList()))) }
  val okSeen=obj("ok" to s("true"),"seen" to s("true"),"story_key" to s("manual:2"))
  IosBatecOperations.verifySeenReceipt(okSeen,"manual:2"); cases++
  fail("incorrect receipt") {IosBatecOperations.verifySeenReceipt(okSeen,"manual:3")}
  fail("receipt not seen") {IosBatecOperations.verifySeenReceipt(obj("ok" to s("true"),"seen" to s("false"),"story_key" to s("manual:2")),"manual:2")}
  val elig=obj("ok" to s("true"),"event_id" to s("31"),"allowed" to s("false"),"organizer" to s("false"),"reason" to s("Fora de finestra"))
  yes(!IosBatecOperations.eligibility(elig,31).allowed,"publication window blocked")
  yes(IosBatecOperations.eligibility(elig,31).reason=="Fora de finestra","reason")
  fail("wrong event eligibility") {IosBatecOperations.eligibility(elig,12)}
  val organizer=obj("ok" to s("true"),"event_id" to s("31"),"allowed" to s("true"),"organizer" to s("true"))
  yes(IosBatecOperations.eligibility(organizer,31).allowed,"organizer permitted")
  val valid=IosBatecOperations.verifyMutation(good,31,"Voluntariat","Sona música")
  yes(valid.id==13 && valid.creatorId==42,"created exact")
  yes(valid.canEdit,"permission parsed")
  val updated=IosBatecOperations.verifyMutation(good,31,"Voluntariat","Sona música",13)
  yes(updated.id==13,"updated exact")
  fail("wrong post ID") {IosBatecOperations.verifyMutation(good,31,"Voluntariat","Sona música",99)}
  fail("wrong event post") {IosBatecOperations.verifyMutation(good,40,"Voluntariat","Sona música")}
  fail("wrong text post") {IosBatecOperations.verifyMutation(good,31,"Voluntariat","mal")}
  fail("no server edit rights") {IosBatecOperations.verifyMutation(obj("ok" to s("true"),"batec" to rootFor(canEdit=false)),31,"Voluntariat","Sona música")}
  fail("missing post") {IosBatecOperations.verifyMutation(obj("ok" to s("true")),31,"Voluntariat","Sona música")}
  val list=obj("ok" to s("true"),"batecs" to JsonArray(listOf(rootFor(13),rootFor(14))))
  yes(IosBatecOperations.decodeList(list,31).map { it.id }==listOf(13,14),"event list")
  fail("list foreign event") {IosBatecOperations.decodeList(obj("ok" to s("true"),"batecs" to JsonArray(listOf(rootFor(event=5)))),31)}
  fail("bad list") {IosBatecOperations.decodeList(obj("ok" to s("false"),"batecs" to JsonArray(emptyList())),31)}
  IosBatecOperations.verifyDelete(obj("ok" to s("true"),"id" to s("13")),13); cases++
  fail("wrong delete id") {IosBatecOperations.verifyDelete(obj("ok" to s("true"),"id" to s("14")),13)}
  fail("delete no confirmation") {IosBatecOperations.verifyDelete(obj("ok" to s("false"),"id" to s("13")),13)}
  println("PAS20_KOTLIN_JVM_PASS: $cases assertions")
}
