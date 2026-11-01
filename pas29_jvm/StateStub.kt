package com.example.agendavalenciana.ios
import kotlinx.serialization.json.*
object IosPersonalEncoded {
 fun bool(v:Boolean)=buildJsonObject {put("t","bool");put("v",v)}
 fun long(v:Long)=buildJsonObject {put("t","long");put("v",v.toString())}
}
class IosCentralStateSnapshot(private val b:Map<String,Boolean>,private val n:Map<String,Long>){
 fun bool(key:String)=b[key]
 fun long(key:String)=n[key]
}
