package com.example.agendavalenciana.ios

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

// Clock és multiplataforma i evita l'accés Objective-C que falla en Kotlin/Native 2.4.
actual fun currentEpochMillis(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
// NSDateFormatter usa la zona horària local del dispositiu per defecte.
// Evita constructors Objective-C NSTimeZone no disponibles en Kotlin/Native.
actual fun currentLocalDateIso(): String = NSDateFormatter().apply {
    dateFormat = "yyyy-MM-dd"
}.stringFromDate(NSDate())
// El dia cultural canvia a les 05:00 de l'hora local, fins i tot amb canvi DST.
actual fun currentCulturalDateIso(): String {
    val now = NSDate()
    val hour = NSDateFormatter().apply { dateFormat = "HH" }.stringFromDate(now).toIntOrNull() ?: 12
    val day = if (hour < 5) NSDate(timeIntervalSince1970 = now.timeIntervalSince1970 - 12.0 * 3600.0) else now
    return NSDateFormatter().apply { dateFormat = "yyyy-MM-dd" }.stringFromDate(day)
}
actual fun platformReadPublicCache(key: String): String =
    IosNativeBridge.current?.readCache(key).orEmpty()
actual fun platformWritePublicCache(key: String, value: String) {
    IosNativeBridge.current?.writeCache(key, value)
}
actual fun platformOpenExternal(url: String) {
    if (!url.startsWith("https://")) return
    platform.Foundation.NSURL.URLWithString(url)?.let { uri ->
        platform.UIKit.UIApplication.sharedApplication.openURL(uri)
    }
}

actual suspend fun platformScheduleReminder(userId: Int, eventId: Int, title: String, isoDate: String): Boolean =
    suspendCancellableCoroutine { cont ->
        val bridge = IosNativeBridge.current
        if (bridge == null) {
            cont.resume(false)
        } else {
            bridge.scheduleLocalReminderConfirmed(userId, eventId, title, isoDate,
                object : IosReminderSchedulingCallback {
                    override fun onResult(success: Boolean, message: String) {
                        if (cont.isActive) cont.resume(success)
                    }
                })
        }
    }
actual fun platformCancelReminder(userId: Int, eventId: Int) {
    IosNativeBridge.current?.cancelLocalReminder(userId, eventId)
}
