#!/usr/bin/env python3
"""Pas 4: comprovacions reals de contracte + regressions; sense API real ni Xcode."""
from pathlib import Path
import hashlib, zipfile, re

project = Path(__file__).resolve().parents[1]
base = project.parent.parent
code = project/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios'
api = (code/'IosAgendaApi.kt').read_text()
state = (code/'IosCentralState.kt').read_text()
screen = (code/'AgendaIosScreen.kt').read_text()
state_tests = (project/'shared/src/commonTest/kotlin/com/example/agendavalenciana/ios/IosCentralStateTest.kt').read_text()
android_sync = (base/'ANDROID_V336_REFERENCIA/ANDROID_STUDIO/app/src/main/java/com/example/agendavalenciana/data/ServerManagedPreferences.kt').read_text()
php_sync = (base/'audit_lagenda/web/api/sync/state.php').read_text()
php_register = (base/'audit_lagenda/web/api/auth/register.php').read_text()
php_user = (base/'audit_lagenda/web/api/user-state/mine.php').read_text()

assert '"sync/state.php"' in api and '"preferences/locations.php"' in api and '"preferences/feed.php"' in api
assert '"user-state/mine.php"' in api
assert 'expectedUserId' in state and 'id == expectedUserId' in state
assert 'central.revision' not in screen or 'serverState' in screen
assert 'val root' not in screen or 'serverState' in screen
assert 'val guest = guestSession.ensure()' in api and 'bearer = guest.token' in api
assert 'bearer = registeredToken' in api and 'identityId = account?.id' in api
assert 'account?.id ?: guestReadyUserId' in screen
assert 'serverState = loaded' in screen and 'serverStateRefreshEpoch += 1' in screen
assert 'loaded.towns(expectedUsername)' in screen and 'loaded.comarques(expectedUsername)' in screen
assert 'loaded.radii(expectedUsername)' in screen
assert 'loaded.suggestions()' in screen and 'loaded.valencianFirst()' in screen
assert 'loaded.discovery()' in screen and 'loaded.userDomain' in screen
assert 'guestRegistrationNotifications(guestToken: String)' in api
assert '"notifications", it' in api and '"guest_token", it.token' in api
assert api.index('guestRegistrationNotifications(it.token)') < api.index('IosServerConnection.json("auth/register.php"')
assert 'ON DUPLICATE KEY UPDATE followed_artist_concerts=VALUES(followed_artist_concerts)' in php_register
assert 'notification_preferences' in php_user and 'followed_artist_concerts' in php_user
assert '"t", "set"' in state_tests and 'neverApplyOtherUsersData' in state_tests
assert '"revision"' in state and '"values"' in state
assert 'saved_concerts_' in android_sync and 'transientKey' in android_sync
assert 'SELECT state_key,value_json' in php_sync and "['revision'" not in php_sync
# Step 4's READ method must remain read-only; step 5 legitimately writes elsewhere.
read_method = api.split('internal suspend fun loadServerState()')[1].split('private val personalWriteMutex')[0]
assert '"sync/state.php", bearer = bearer' in read_method
assert '"sync/state.php", buildJsonObject' not in read_method
assert 'UserDefaults.standard' not in state and 'platformWritePublicCache(' not in state
assert 'UserDefaults' not in read_method

with zipfile.ZipFile(base/'LAGENDA_V336_ANDROID_STUDIO_v336_COMPILACIO_CORREGIDA(3).zip') as z:
    members = [n for n in z.namelist() if n.startswith('ANDROID_STUDIO/') and not n.endswith('/')]
    for n in members:
        assert hashlib.sha256(z.read(n)).digest() == hashlib.sha256((base/'ANDROID_V336_REFERENCIA'/n).read_bytes()).digest(),n
with zipfile.ZipFile(base/'LAGENDA_V349_IOS_PANTALLA_COMPLETA_FEED_SCROLL (1)(2).zip') as z:
    members_embedded = [n for n in z.namelist() if n.startswith(('ANDROID_STUDIO/app/', 'SERVIDOR_')) and not n.endswith('/')]
    for n in members_embedded:
        assert hashlib.sha256(z.read(n)).digest() == hashlib.sha256((base/'LAGENDA_IOS_TREBALL_ACUMULATIU'/n).read_bytes()).digest(),n
print('PASS: iOS guest + registered central server state, read-only load, correct user ID, domain preferences, registration notifications')
print('PASS: original Android v336: '+str(len(members))+' immutable files')
print('PASS: bundled Android and server: '+str(len(members_embedded))+' immutable files')
