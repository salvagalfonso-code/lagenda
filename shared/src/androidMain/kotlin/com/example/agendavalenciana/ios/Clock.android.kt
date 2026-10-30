package com.example.agendavalenciana.ios

actual fun currentEpochMillis(): Long = System.currentTimeMillis()
actual fun currentLocalDateIso(): String = java.time.LocalDate.now(java.time.ZoneId.of("Europe/Madrid")).toString()
actual fun currentCulturalDateIso(): String {
    val now = java.time.ZonedDateTime.now(java.time.ZoneId.of("Europe/Madrid"))
    return (if (now.hour < 5) now.toLocalDate().minusDays(1) else now.toLocalDate()).toString()
}

// These KMP UI bridges are only used by iosApp. The existing Android activity keeps its native implementations.
private val publicCache = mutableMapOf<String, String>()
actual fun platformReadPublicCache(key: String): String = publicCache[key].orEmpty()
actual fun platformWritePublicCache(key: String, value: String) { publicCache[key] = value }
actual fun platformOpenExternal(url: String) { /* iOS-only shared UI, never called by Android app */ }
actual suspend fun platformScheduleReminder(userId: Int, eventId: Int, title: String, isoDate: String): Boolean = false
actual fun platformCancelReminder(userId: Int, eventId: Int) {}
