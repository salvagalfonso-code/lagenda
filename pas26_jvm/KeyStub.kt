package com.example.agendavalenciana.model
object IosFestivalSavedSlotPolicy {
 fun key(eventId:Int, day:String,time:String,artist:String,title:String,location:String=""):String =
  (listOf(eventId.toString(),day.trim().take(10),time.trim().take(5),artist.trim().lowercase(),title.trim().lowercase())+
   location.trim().takeIf(String::isNotBlank).let { if(it!=null) listOf(it.lowercase()) else emptyList() })
     .joinToString("|") { "${it.length}:$it" }
}
