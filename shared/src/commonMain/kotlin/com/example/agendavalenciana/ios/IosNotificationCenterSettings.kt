package com.example.agendavalenciana.ios

import kotlinx.serialization.json.*

/** Pas 29: account-scoped channels; NEVER stored in UserDefaults.
 * Alert key names are the ones Android v336 uses in its notification configuration.
 * On Android v336 some of them remain device-local: do not claim cross-device parity.
 */
internal data class IosNotificationCenterSettings(
    val community: Boolean = true,
    val followed: Boolean = true,
    val dayBefore: Boolean = true,
    val tickets: Boolean = true,
    val suggestions: Boolean = true,
    val changes: Boolean = true,
    val homeTown: Boolean = true,
    val homeComarca: Boolean = true,
    val favoriteStyle: Boolean = false,
    val emailFrequency: String = "NONE",
    val emailRecommendations: Boolean = false,
    val emailArtists: Boolean = false,
    val emailReminders: Boolean = false,
    val emailAds: Boolean = false,
    val pushEnabled: Boolean = false,
    val quietEnabled: Boolean = false,
    val quietStart: Int = 23 * 60,
    val quietEnd: Int = 8 * 60
) {
    fun centralMutations(): Map<String, JsonObject> = mapOf(
        "alert_community" to IosPersonalEncoded.bool(community),
        "alert_followed" to IosPersonalEncoded.bool(followed),
        "alert_day_before" to IosPersonalEncoded.bool(dayBefore),
        "alert_tickets" to IosPersonalEncoded.bool(tickets),
        "alert_suggestions" to IosPersonalEncoded.bool(suggestions),
        "alert_changes" to IosPersonalEncoded.bool(changes),
        "alert_town" to IosPersonalEncoded.bool(homeTown),
        "alert_comarca" to IosPersonalEncoded.bool(homeComarca),
        "alert_style" to IosPersonalEncoded.bool(favoriteStyle),
        "ios_push_enabled" to IosPersonalEncoded.bool(pushEnabled),
        "ios_push_quiet_enabled" to IosPersonalEncoded.bool(quietEnabled),
        "ios_push_quiet_start" to IosPersonalEncoded.long(quietStart.toLong()),
        "ios_push_quiet_end" to IosPersonalEncoded.long(quietEnd.toLong())
    )
    fun validate(registered: Boolean) {
        require(quietStart in 0..1439 && quietEnd in 0..1439) { "Hora de descans invàlida" }
        require(emailFrequency in setOf("NONE", "DAILY", "WEEKLY")) { "Freqüència de correu invàlida" }
        if (registered && emailFrequency != "NONE") require(
            emailRecommendations || emailArtists || emailReminders || emailAds
        ) { "Tria almenys un tipus de correu" }
    }
    fun withCentral(state: IosCentralStateSnapshot): IosNotificationCenterSettings = copy(
        community = state.bool("alert_community") ?: community,
        followed = state.bool("alert_followed") ?: followed,
        dayBefore = state.bool("alert_day_before") ?: dayBefore,
        tickets = state.bool("alert_tickets") ?: tickets,
        suggestions = state.bool("alert_suggestions") ?: suggestions,
        changes = state.bool("alert_changes") ?: changes,
        homeTown = state.bool("alert_town") ?: homeTown,
        homeComarca = state.bool("alert_comarca") ?: homeComarca,
        favoriteStyle = state.bool("alert_style") ?: favoriteStyle,
        pushEnabled = state.bool("ios_push_enabled") ?: pushEnabled,
        quietEnabled = state.bool("ios_push_quiet_enabled") ?: quietEnabled,
        quietStart = (state.long("ios_push_quiet_start")?.toInt() ?: quietStart).coerceIn(0, 1439),
        quietEnd = (state.long("ios_push_quiet_end")?.toInt() ?: quietEnd).coerceIn(0, 1439)
    )
    fun withEmail(root: JsonObject): IosNotificationCenterSettings {
        val p = root["preferences"] as? JsonObject ?: return this
        fun flag(name: String) = p[name]?.jsonPrimitive?.booleanOrNull == true
        val freq = p["digest_frequency"]?.jsonPrimitive?.contentOrNull ?: "NONE"
        return copy(emailFrequency = freq.takeIf { it in setOf("NONE", "DAILY", "WEEKLY") } ?: "NONE",
            emailRecommendations = flag("recommendations_enabled"),
            emailArtists = flag("followed_artists_enabled"),
            emailReminders = flag("saved_reminders_enabled"), emailAds = flag("ads_enabled"))
    }
    fun emailPayload() = buildJsonObject {
        put("digest_frequency", emailFrequency)
        put("recommendations_enabled", emailRecommendations)
        put("followed_artists_enabled", emailArtists)
        put("saved_reminders_enabled", emailReminders)
        put("ads_enabled", emailAds)
    }
    companion object {
        fun displayQuietTime(minute: Int): String = "${(minute/60).toString().padStart(2,'0')}:${(minute%60).toString().padStart(2,'0')}"
    }
}
