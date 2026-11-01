package com.example.agendavalenciana.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.agendavalenciana.MainActivity
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/** Recordatoris locals. WorkManager sobreviu al tancament de l'app i al reinici;
 * Android pot retardar-ne l'execució per l'estalvi de bateria. */
internal data class AndroidReminderSettings(
    val enabled: Boolean = false,
    val quietEnabled: Boolean = false,
    val quietStartMinutes: Int = 22 * 60,
    val quietEndMinutes: Int = 9 * 60
)

/** Configuració real conservada localment per a un recordatori futur. */
internal data class ActiveEventReminder(
    val eventId: Int,
    val title: String,
    val town: String,
    val atEpochMillis: Long,
    val customTime: Boolean
)

internal object EventReminderScheduler {
    private const val PREFS = "agenda_android_event_reminders_v326"
    fun settings(context: Context): AndroidReminderSettings {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return AndroidReminderSettings(
            enabled = prefs.getBoolean("android_enabled", false),
            quietEnabled = prefs.getBoolean("quiet_enabled", false),
            quietStartMinutes = prefs.getInt("quiet_start", 22 * 60).coerceIn(0, 1439),
            quietEndMinutes = prefs.getInt("quiet_end", 9 * 60).coerceIn(0, 1439)
        )
    }

    fun isAndroidEnabled(context: Context): Boolean = settings(context).enabled

    fun setAndroidEnabled(context: Context, enabled: Boolean) {
        val old = settings(context)
        setSettings(context, old.copy(enabled = enabled))
    }

    fun setSettings(context: Context, value: AndroidReminderSettings) {
        require(value.quietStartMinutes in 0..1439 && value.quietEndMinutes in 0..1439)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("android_enabled", value.enabled)
            .putBoolean("quiet_enabled", value.quietEnabled)
            .putInt("quiet_start", value.quietStartMinutes)
            .putInt("quiet_end", value.quietEndMinutes)
            .apply()
        // L'horari de silenci és local: els recordatoris personalitzats
        // mantenen exactament el dia/hora triats per l'usuari.
        prefs.getStringSet("event_ids", emptySet()).orEmpty().toList().forEach { idString ->
            val eventId = idString.toIntOrNull() ?: return@forEach
            val originalMillis = prefs.getLong(key(eventId), 0L)
            if (originalMillis > System.currentTimeMillis()) {
                enqueue(context, eventId, prefs.getString("title_$eventId", "").orEmpty(),
                    prefs.getString("town_$eventId", "").orEmpty(), originalMillis,
                    prefs.getBoolean("custom_$eventId", false))
            }
        }
    }

    /** Retarda un recordatori predeterminat fins al final del descans, sense moure'n un de personalitzat. */
    internal fun adjustedTime(requestedMillis: Long, custom: Boolean, settings: AndroidReminderSettings): Long {
        if (custom || !settings.quietEnabled || settings.quietStartMinutes == settings.quietEndMinutes) return requestedMillis
        val whenLocal = Instant.ofEpochMilli(requestedMillis).atZone(ZoneId.systemDefault())
        val minute = whenLocal.hour * 60 + whenLocal.minute
        val start = settings.quietStartMinutes
        val end = settings.quietEndMinutes
        val inside = if (start < end) minute in start until end else minute >= start || minute < end
        if (!inside) return requestedMillis
        val endDay = whenLocal.toLocalDate().plusDays(if (start > end && minute >= start) 1 else 0)
        return endDay.atTime(LocalTime.of(end / 60, end % 60))
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun workName(eventId: Int) = "agenda_event_reminder_$eventId"
    private fun key(eventId: Int) = "when_$eventId"

    fun get(context: Context, eventId: Int): Instant? {
        val ts = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(key(eventId), 0L)
        return ts.takeIf { it > 0 }?.let(Instant::ofEpochMilli)
    }

    /** Retorna tota la configuració futura, sense reconstruir la llista d'esdeveniments. */
    fun activeReminders(context: Context, nowMillis: Long = System.currentTimeMillis()): List<ActiveEventReminder> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getStringSet("event_ids", emptySet()).orEmpty().mapNotNull { storedId ->
            val eventId = storedId.toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
            val whenMillis = prefs.getLong(key(eventId), 0L)
            if (whenMillis <= nowMillis) return@mapNotNull null
            ActiveEventReminder(
                eventId = eventId,
                title = prefs.getString("title_$eventId", "").orEmpty(),
                town = prefs.getString("town_$eventId", "").orEmpty(),
                atEpochMillis = whenMillis,
                customTime = prefs.getBoolean("custom_$eventId", false)
            )
        }.sortedWith(compareBy<ActiveEventReminder> { it.atEpochMillis }.thenBy { it.eventId })
    }

    fun save(context: Context, eventId: Int, title: String, town: String, atEpochMillis: Long, customTime: Boolean = false) {
        require(atEpochMillis > System.currentTimeMillis()) { "La data de l'avís ha de ser futura" }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val newIds = prefs.getStringSet("event_ids", emptySet()).orEmpty().toMutableSet().apply { add(eventId.toString()) }
        val edit = prefs.edit()
            .putLong(key(eventId), atEpochMillis)
            .putString("title_$eventId", title.take(160))
            .putString("town_$eventId", town.take(120))
            .putBoolean("custom_$eventId", customTime)
            .putStringSet("event_ids", newIds)
        if (!prefs.contains("android_enabled")) edit.putBoolean("android_enabled", true)
        edit.apply()
        enqueue(context, eventId, title, town, atEpochMillis, customTime)
    }

    private fun enqueue(context: Context, eventId: Int, title: String, town: String, atEpochMillis: Long, customTime: Boolean) {
        val displayMillis = adjustedTime(atEpochMillis, customTime, settings(context))
        val delay = (displayMillis - System.currentTimeMillis()).coerceAtLeast(0)
        val request = OneTimeWorkRequestBuilder<EventReminderWorker>()
            .setInputData(Data.Builder()
                .putInt("event_id", eventId)
                .putString("title", title.take(160))
                .putString("town", town.take(120))
                .putLong("when", atEpochMillis)
                .putBoolean("custom_time", customTime)
                .build())
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(workName(eventId), ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(context: Context, eventId: Int) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val ids = prefs.getStringSet("event_ids", emptySet()).orEmpty().toMutableSet().apply { remove(eventId.toString()) }
        prefs.edit().remove(key(eventId)).remove("title_$eventId").remove("town_$eventId")
            .remove("custom_$eventId").putStringSet("event_ids", ids).apply()
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(workName(eventId))
    }

    internal fun isStillScheduled(context: Context, eventId: Int, whenMillis: Long): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(key(eventId), 0L) == whenMillis
}

internal class EventReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val id = inputData.getInt("event_id", 0)
        val whenMillis = inputData.getLong("when", 0)
        if (id <= 0 || whenMillis <= 0 ||
            !EventReminderScheduler.isStillScheduled(applicationContext, id, whenMillis) ||
            !EventReminderScheduler.isAndroidEnabled(applicationContext)
        ) return Result.success()
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return Result.success()

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "agenda_event_reminders"
        manager.createNotificationChannel(NotificationChannel(channelId, "Recordatoris d’esdeveniments", NotificationManager.IMPORTANCE_DEFAULT))
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("agendavalenciana://event/$id")
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pending = PendingIntent.getActivity(
            applicationContext, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Recordatori · ${inputData.getString("title").orEmpty()}")
            .setContentText(inputData.getString("town").orEmpty())
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        manager.notify(id + 40_000, notification)
        return Result.success()
    }
}
