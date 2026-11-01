package com.example.agendavalenciana.ios

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import kotlinx.serialization.json.*

class IosNotificationCenterSettingsTest {
    @Test fun mirrorsAndroidAlertKeysAndServerPushKeys() {
        val x = IosNotificationCenterSettings(community = false, pushEnabled = true)
        assertEquals(13, x.centralMutations().size)
        assertEquals(false, x.centralMutations()["alert_community"]?.get("v")?.jsonPrimitive?.booleanOrNull)
        assertEquals(true, x.centralMutations()["ios_push_enabled"]?.get("v")?.jsonPrimitive?.booleanOrNull)
    }
    @Test fun readsCanonicalEmail() {
        val server = buildJsonObject { put("preferences", buildJsonObject {
            put("digest_frequency", "WEEKLY")
            put("recommendations_enabled", true)
            put("followed_artists_enabled", false)
            put("saved_reminders_enabled", true)
            put("ads_enabled", false)
        }) }
        val prefs = IosNotificationCenterSettings().withEmail(server)
        assertEquals("WEEKLY", prefs.emailFrequency)
        assertTrue(prefs.emailRecommendations)
        assertFalse(prefs.emailArtists)
        assertTrue(prefs.emailReminders)
        assertEquals("WEEKLY", prefs.emailPayload()["digest_frequency"]?.jsonPrimitive?.content)
    }
    @Test fun validatesFrequencyAndQuietHours() {
        assertFailsWith<IllegalArgumentException> { IosNotificationCenterSettings(emailFrequency="DAILY").validate(true) }
        IosNotificationCenterSettings(emailFrequency="DAILY",emailArtists=true).validate(true)
        assertFailsWith<IllegalArgumentException> { IosNotificationCenterSettings(quietEnd=2000).validate(false) }
        assertEquals("23:00",IosNotificationCenterSettings.displayQuietTime(1380))
    }
}
