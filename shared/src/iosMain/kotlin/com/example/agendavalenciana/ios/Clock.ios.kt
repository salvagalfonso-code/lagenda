package com.example.agendavalenciana.ios

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSTimeZone
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

// Kotlin stdlib Clock evita els membres NSDate no resolts pel compilador Kotlin/Native.
actual fun currentEpochMillis(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
actual fun currentLocalDateIso(): String = NSDateFormatter().apply {
    dateFormat = "yyyy-MM-dd"
    timeZone = NSTimeZone(name = "Europe/Madrid")
}.stringFromDate(NSDate())

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
