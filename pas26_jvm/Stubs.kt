package com.example.agendavalenciana.ios

data class AgendaScheduleEntry(val date:String,val time:String,val title:String,val artist:String,val location:String,
 val groupName:String="",val itemType:String="performance",val sortOrder:Int=0,val endTime:String="",val details:String="")
data class IosTicketOption(val id:Int,val name:String,val price:Double,val remaining:Int?,val maxPerOrder:Int,val sessionDate:String,val sessionTime:String)
