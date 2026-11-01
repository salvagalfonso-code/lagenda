package com.example.agendavalenciana

import android.content.Intent
import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.agendavalenciana.data.EventReminderScheduler
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.agendavalenciana.data.AgendaSyncScheduler
import com.example.agendavalenciana.ui.AgendaApp
import com.example.agendavalenciana.ui.FeedReadingSessionStore
import com.example.agendavalenciana.ui.UiLanguageStore
import com.example.agendavalenciana.ui.applyAndroidUiLocale
import com.example.agendavalenciana.ui.theme.AgendaValencianaTheme

class MainActivity : ComponentActivity() {
    private var pendingDeepLink by mutableStateOf<String?>(null)
    private var feedSessionEpoch by mutableStateOf(0)
    private val initialNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) EventReminderScheduler.setAndroidEnabled(applicationContext, true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (FeedReadingSessionStore.onForeground(this)) feedSessionEpoch++
        UiLanguageStore.read(applicationContext)?.let { applyAndroidUiLocale(this, it) }
        pendingDeepLink = intent?.data?.toString()
        enableEdgeToEdge()
        setContent {
            AgendaValencianaTheme {
                AgendaApp(
                    deepLinkUri = pendingDeepLink,
                    feedSessionEpoch = feedSessionEpoch,
                    onDeepLinkConsumed = { pendingDeepLink = null }
                )
            }
        }
        // WorkManager és necessari, però inicialitzar-lo no ha de competir amb el primer frame.
        window.decorView.postDelayed({
            if (Build.VERSION.SDK_INT >= 33) {
                val prefs = getSharedPreferences("agenda_notification_permission_v334", MODE_PRIVATE)
                // Una única petició en la primera instal·lació, sense repetir-la si es rebutja.
                if (!prefs.getBoolean("asked_once", false) &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                    prefs.edit().putBoolean("asked_once", true).apply()
                    initialNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }, 4_000L)
        window.decorView.postDelayed({
            AgendaSyncScheduler.schedule(applicationContext)
        }, 45_000L)
    }

    override fun onStart() {
        super.onStart()
        // La tornada de WhatsApp, compartir o una altra app no reinicia res.
        // Cada retorn abans d'una hora anul·la el còmput anterior.
        if (FeedReadingSessionStore.onForeground(this)) feedSessionEpoch++
    }

    override fun onStop() {
        if (!isChangingConfigurations) FeedReadingSessionStore.onBackground(this)
        super.onStop()
    }

    override fun onDestroy() {
        // Acabar expressament l'Activity és un tancament real, no una pausa externa.
        if (isFinishing && !isChangingConfigurations) FeedReadingSessionStore.onExplicitClose(this)
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDeepLink = intent.data?.toString()
    }
}
