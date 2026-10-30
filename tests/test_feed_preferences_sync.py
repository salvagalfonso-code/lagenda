"""Static cross-layer contract audit; not a compiler or live DB integration test."""
from pathlib import Path
r=Path(__file__).resolve().parents[1]
server=(r/'SERVIDOR_PER_A_TU/api/preferences/feed.php').read_text()
engine=(r/'SERVIDOR_PER_A_TU/api/for-you/feed.php').read_text()
sql=(r/'SERVIDOR_PER_A_TU/sql/02_user_for_you_preferences.sql').read_text()
android_api=(r/'ANDROID_STUDIO/app/src/main/java/com/example/agendavalenciana/data/ServerApiRepository.kt').read_text()
android_state=(r/'ANDROID_STUDIO/app/src/main/java/com/example/agendavalenciana/state/AgendaState.kt').read_text()
ios_api=(r/'ANDROID_STUDIO/shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosAgendaApi.kt').read_text()
ios_state=(r/'ANDROID_STUDIO/shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/AgendaIosScreen.kt').read_text()
checks={
 'preferences in account row': 'user_for_you_preferences' in sql and 'REFERENCES users(id)' in sql,
 'server account auth': 'current_user(true)' in server and 'user_id=?' in server and 'ON DUPLICATE KEY UPDATE' in server,
 'server rejects malformed payload': "is_bool($options['suggestions'])" in server and "is_bool($options['valencian_first'])" in server,
 'server deletes stale cached feed': 'DELETE FROM for_you_server_cache' in server,
 'server feed overrides posted switches': "'suggestions']=(bool)$storedFeedPrefs['suggestions']" in engine and "'valencian_first']=(bool)$storedFeedPrefs['valencian_first']" in engine,
 'Android uses same URL': 'preferences/feed.php' in android_api and 'fetchFeedPreferences(' in android_api and 'saveFeedPreferences(' in android_api,
 'iOS uses same URL': 'preferences/feed.php' in ios_api and 'feedPreferences()' in ios_api and 'saveFeedPreferences(' in ios_api,
 'Android account-scoped save': 'userCacheKey("show_suggestions")' in android_state and 'userCacheKey("discovery_level")' in android_state,
 'Android login and cold restart': android_state.count('synchronizeFeedPreferencesWithServer()') >= 7,
 'Android offline journal': 'feed_prefs_remote_dirty' in android_state and 'userPreferencesWriteRevision' in android_state,
 'iOS account and dirty protections': 'pref_feed_dirty' in ios_state and 'feedPrefsWriteRevision' in ios_state and 'account?.id == expectedAccount' in ios_state,
 'iOS updates server result only after save': 'feedPrefsServerRevision += 1' in ios_state and 'feedPrefsServerRevision' in ios_state[ios_state.index('val selectedForYou by produceState'):],
 'no duplicated Android sync declarations': android_state.count('private fun synchronizeFeedPreferencesWithServer()') == 1,
}
for key, ok in checks.items(): print(('OK' if ok else 'FAIL')+': '+key)
assert all(checks.values())
