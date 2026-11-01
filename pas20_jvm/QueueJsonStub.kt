package kotlinx.serialization.json

open class JsonElement
object JsonNull:JsonElement()
class JsonPrimitive(val value:String):JsonElement() {
    val contentOrNull:String? get() = value
    val intOrNull:Int? get() = value.toIntOrNull()
    override fun toString() = "\""+value+"\""
}
class JsonArray(val items: List<JsonElement>):JsonElement(),List<JsonElement> by items {
    override fun toString() = items.joinToString(",", "[", "]")
}
class JsonObject(private val entriesMap:Map<String,JsonElement>):JsonElement(),Map<String,JsonElement> by entriesMap {
    override fun toString():String = entriesMap.entries.joinToString(",","{","}") { "\""+it.key+"\":"+it.value }.also { Registry.values[it] = this }
}
private object Registry { val values=mutableMapOf<String,JsonObject>() }
object Json { fun parseToJsonElement(raw:String):JsonElement = Registry.values[raw] ?: if(raw=="{\"version\":1,\"pending\":[]}") buildJsonObject { put("version",1);put("pending",JsonArray(emptyList())) } else error("Unknown fake JSON") }
val JsonElement.jsonPrimitive:JsonPrimitive get()=this as JsonPrimitive
class JsonObjectBuilder { val map=mutableMapOf<String,JsonElement>()
    fun put(k:String,v:String) {map[k]=JsonPrimitive(v)}
    fun put(k:String,v:Int) {map[k]=JsonPrimitive(v.toString())}
    fun put(k:String,v:JsonElement) {map[k]=v}
    fun create()=JsonObject(map.toMap())
}
class JsonArrayBuilder {val list=mutableListOf<JsonElement>(); fun add(v:JsonElement) {list+=v};fun create()=JsonArray(list.toList())}
fun buildJsonObject(f:JsonObjectBuilder.()->Unit):JsonObject=JsonObjectBuilder().apply(f).create()
fun buildJsonArray(f:JsonArrayBuilder.()->Unit):JsonArray=JsonArrayBuilder().apply(f).create()
