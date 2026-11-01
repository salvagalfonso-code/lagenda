package kotlinx.serialization.json
open class JsonElement
class JsonPrimitive(val content:String):JsonElement() {
    val booleanOrNull:Boolean? get()=when(content){"true"->true;"false"->false;else->null}
    val contentOrNull:String? get()=content
}
class JsonObject(private val data:Map<String,JsonElement>):JsonElement(),Map<String,JsonElement> by data
val JsonElement.jsonPrimitive:JsonPrimitive get()=this as JsonPrimitive
class JsonBuilder {
 val map=linkedMapOf<String,JsonElement>()
 fun put(k:String,v:String){map[k]=JsonPrimitive(v)}
 fun put(k:String,v:Boolean){map[k]=JsonPrimitive(v.toString())}
 fun put(k:String,v:JsonElement){map[k]=v}
}
fun buildJsonObject(block:JsonBuilder.()->Unit):JsonObject{val b=JsonBuilder();b.block();return JsonObject(b.map)}
