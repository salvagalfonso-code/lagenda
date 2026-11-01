package kotlinx.serialization.json
open class JsonElement
class JsonPrimitive(val content:String):JsonElement(){
 val intOrNull:Int? get()=content.toIntOrNull()
 val booleanOrNull:Boolean? get()=when(content){"true"->true;"false"->false;else->null}
 val contentOrNull:String? get()=content
}
class JsonObject(private val data:Map<String,JsonElement>):JsonElement(),Map<String,JsonElement> by data
class JsonArray(private val data:List<JsonElement>):JsonElement(),List<JsonElement> by data
val JsonElement.jsonPrimitive:JsonPrimitive get()=this as JsonPrimitive
class JsonBuilder {val map=linkedMapOf<String,JsonElement>();fun put(k:String,v:String){map[k]=JsonPrimitive(v)};fun put(k:String,v:Int){map[k]=JsonPrimitive(v.toString())};fun put(k:String,v:Boolean){map[k]=JsonPrimitive(v.toString())};fun put(k:String,v:JsonElement){map[k]=v}}
fun buildJsonObject(block:JsonBuilder.()->Unit):JsonObject{val v=JsonBuilder();v.block();return JsonObject(v.map)}
class JsonArrayBuilder{val list=mutableListOf<JsonElement>();fun add(v:Int){list.add(JsonPrimitive(v.toString()))};fun add(v:String){list.add(JsonPrimitive(v))};fun add(v:JsonElement){list.add(v)}}
fun buildJsonArray(block:JsonArrayBuilder.()->Unit):JsonArray{val v=JsonArrayBuilder();v.block();return JsonArray(v.list)}
