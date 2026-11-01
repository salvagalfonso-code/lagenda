package com.example.agendavalenciana.ios

import com.example.agendavalenciana.model.*

// Only the data-only companions are stubbed; the real production Batecs policies compile below.
data class AgendaScheduleEntry(val date: String, val time: String, val title: String, val artist: String, val location: String)
data class AgendaEventSummary(
  val id: Int, val title: String, val date: String, val time: String, val town: String,
  val venue: String = "", val category: String = "", val description: String = "", val posterUrl: String = "",
  val artists: List<String> = emptyList(), val schedule: List<AgendaScheduleEntry> = emptyList(),
  val endDate: String = "", val updatedAt: String = ""
)
data class IosBatec(val id:String, val eventId:Int, val artist:String, val eventTitle:String,
  val text:String,val imageUrl:String,val storyKey:String="",val sourceType:String="MANUAL",
  val publishedAt:String="",val eventDate:String="")
object IosUnfilteredAgenda {
  fun validDay(day:String):Boolean = Regex("\\d{4}-\\d{2}-\\d{2}").matches(day)
}

fun main() {
  val names = (1..8).map { "Artista $it" }.toSet()
  fun event(id:Int, artist:String, date:String="2026-11-10", updated:String="2026-10-31 20:00:00") =
    AgendaEventSummary(id, "Actuació $artist", date, "21:00", "València",artists=listOf(artist),updatedAt=updated)
  val catalog = (1..8).map { event(it,"Artista $it") }
  val today = "2026-10-31"
  fun select(entries:List<IosBatec>, follow:Set<String> = names, seen:Set<String> = emptySet()) =
    IosBatecArtistGroups.select(entries.map { BatecCandidate(it.storyKey,it.artist,it.sourceType,it.publishedAt) },follow,seen)
  var checks=0
  fun checkNamed(condition:Boolean, desc:String) { check(condition) { desc };checks++ }
  val placeholders=IosBatecFeedContract.project(names,emptySet(),catalog,emptyList(),emptyMap(),today)
  checkNamed(placeholders.size==8,"8 followed => 8 projection entries")
  checkNamed(select(placeholders).size==6,"exactly 6 circles")
  checkNamed(select(placeholders).all {it.allSeen},"placeholders grey")
  checkNamed(IosBatecFeedContract.project(names.take(6).toSet(), emptySet(),catalog,emptyList(),emptyMap(),today).isEmpty(),"6 follows -> hidden")
  checkNamed(select(placeholders,names.take(6).toSet()).isEmpty(),"UI hidden with 6 follows")
  val blocked=IosBatecFeedContract.project(names, setOf("ARTISTA 8","Artista 7"),catalog,emptyList(),emptyMap(),today)
  checkNamed(blocked.isEmpty(),"exclude two followed => hidden")
  val source=IosBatec("10",1,"","Actuació","Text","","manual:10",publishedAt="2026-10-31 20:01:00")
  val manual=IosBatecFeedContract.project(names,emptySet(),catalog,listOf(source),emptyMap(),today)
  checkNamed(manual.any { it.storyKey=="manual:10" && it.artist=="Artista 1" },"artist resolved by event programme")
  checkNamed(select(manual).first().artist=="Artista 1","unseen first")
  checkNamed(select(manual,seen=setOf("manual:10")).all { it.allSeen },"seen switches to grey")
  val manualUnfollowed=source.copy(artist="Un altra persona", storyKey="manual:11")
  val corrected=IosBatecFeedContract.project(names,emptySet(),catalog,listOf(manualUnfollowed),emptyMap(),today)
  checkNamed(corrected.any {it.storyKey=="manual:11" && it.artist=="Artista 1" },"never use author as artist")
  val recency=IosBatecFeedContract.project(names,emptySet(),catalog,emptyList(),mapOf(2 to "2026-10-31 20:55:00"),today)
  checkNamed(recency.any {it.storyKey=="event:2:artista 2" && it.sourceType=="AUTO_EVENT"},"recent event sourced from server")
  checkNamed(select(recency).first().artist=="Artista 2","recent event ranked above fallback")
  checkNamed(recency.count {it.artist=="Artista 2"}==1,"single artist content")
  val staleCatalog=listOf(event(1,"Artista 1","2026-10-20")) + catalog.drop(1)
  val filtered=IosBatecFeedContract.project(names,emptySet(),staleCatalog,listOf(source),mapOf(1 to "2026-10-31 20:50:00"),today)
  checkNamed(filtered.none {it.storyKey=="manual:10" || it.storyKey.startsWith("event:1:")},"past event is not displayed")
  checkNamed(filtered.any {it.storyKey=="followed:artista 1"},"placeholder for followed with no future event")
  val mult=event(20,"Artista 1").copy(artists=listOf("Artista 1","Artista 4"))
  val mixed=IosBatecFeedContract.project(names,emptySet(),catalog+mult,emptyList(),mapOf(20 to "2026-10-31 23:00:00"),today)
  checkNamed(mixed.any {it.storyKey=="event:20:artista 1"} && mixed.any {it.storyKey=="event:20:artista 4"}, "same event allowed for multiple followed artists")
  val dupe=listOf(source,source.copy(publishedAt="2026-10-31 20:33:00"))
  checkNamed(IosBatecFeedContract.project(names,emptySet(),catalog,dupe,emptyMap(),today).count {it.storyKey=="manual:10"}==1,"deduplicated server ID")
  checkNamed(select(placeholders,names).size==6,"six exact")
  checkNamed(com.example.agendavalenciana.model.IosBatecNames.canonical("Àngel   D'Ór") == "angel d'or", "accent and whitespace normalization")
  checkNamed(IosBatecArtistGroups.canonicalSeenKey("event:20:artista 1") == "event:20", "PHP canonical seen alias")
  val sharedSeen = IosBatecArtistGroups.select((1..7).map { i ->
      BatecCandidate("event:$i:artista $i", "Artista $i", "AUTO_EVENT", "2026-10-31 20:01:00") }, names, setOf("event:1"))
  checkNamed(sharedSeen.none { it.artist == "Artista 1" && !it.allSeen }, "seen alias paints grey")
  checkNamed(IosBatecFeedContract.project(names, emptySet(), catalog, emptyList(), emptyMap(), today)
      .none { it.artist == "No Seguit" }, "no unfollowed artist")
  println("PAS19_REAL_KOTLIN_JVM_PASS: $checks assertions")
}
