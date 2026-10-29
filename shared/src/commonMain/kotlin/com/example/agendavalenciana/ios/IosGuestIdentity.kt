package com.example.agendavalenciana.ios

import kotlin.random.Random

/** Stable anonymous ID, analogous to Android's guestId. Not an authentication token. */
internal object IosGuestIdentity {
    private const val KEY = "ios_publisher_guest_id_v350"
    fun get(): String {
        val saved = platformReadPublicCache(KEY)
        if (saved.matches(Regex("[0-9a-f-]{36}"))) return saved
        val bytes = Random.Default.nextBytes(16)
        // UUIDv4 shape; this is only a public correlation key, never a secret owner credential.
        bytes[6] = ((bytes[6].toInt() and 0x0f) or 0x40).toByte()
        bytes[8] = ((bytes[8].toInt() and 0x3f) or 0x80).toByte()
        val chars = "0123456789abcdef"
        val hex = buildString {
            bytes.forEachIndexed { index, byte ->
                if (index in setOf(4, 6, 8, 10)) append('-')
                val n = byte.toInt() and 0xff
                append(chars[n ushr 4]); append(chars[n and 0x0f])
            }
        }
        platformWritePublicCache(KEY,hex)
        return hex
    }
}
