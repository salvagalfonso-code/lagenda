package com.example.agendavalenciana.ios

import kotlinx.serialization.json.*
import kotlin.random.Random

/** Same {t,v} encoding and UUID-shaped mutation receipts as Android v336.
 * This file contains no durable storage; pending offline writes belong to step 6.
 */
internal object IosPersonalMutationId {
    fun make(): String {
        val bytes = Random.Default.nextBytes(16)
        bytes[6] = ((bytes[6].toInt() and 0x0f) or 0x40).toByte()
        bytes[8] = ((bytes[8].toInt() and 0x3f) or 0x80).toByte()
        val hex = bytes.joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') }
        return "${hex.substring(0,8)}-${hex.substring(8,12)}-${hex.substring(12,16)}-${hex.substring(16,20)}-${hex.substring(20)}"
    }
}

internal object IosPersonalEncoded {
    fun bool(v: Boolean): JsonObject = buildJsonObject { put("t", "bool"); put("v", v) }
    fun string(v: String): JsonObject = buildJsonObject { put("t", "string"); put("v", v) }
    fun long(v: Long): JsonObject = buildJsonObject { put("t", "long"); put("v", v) }
    fun strings(values: Set<String>): JsonObject = buildJsonObject {
        put("t", "set"); put("v", buildJsonArray { values.sorted().forEach { add(it) } })
    }
}
