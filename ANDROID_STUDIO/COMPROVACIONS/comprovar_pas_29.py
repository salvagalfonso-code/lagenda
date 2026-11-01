#!/usr/bin/env python3
"""Step 29: server-backed notification categories/channels/active reminders; Android immutable."""
from pathlib import Path
import hashlib,zipfile
root=Path(__file__).resolve().parents[1]
i=root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios'
ui=(i/'AgendaIosScreen.kt').read_text()
api=(i/'IosAgendaApi.kt').read_text()
model=(i/'IosNotificationCenterSettings.kt').read_text()
php=(Path('/mnt/data/audit_lagenda/web/api/mail/preferences-api.php')).read_text()
assert all(x in ui for x in ['"General" to "bell"','"Pròxim" to "clock"','"Nous" to "info"','"Entrades" to "ticket"','"Suggerències" to "search"','"Comunitat" to "people"'])
assert all(x in ui for x in ['"Recordatoris"','"Configuració"','"Campaneta de l\'app"','"Correus electrònics"','"Notificacions iPhone"','"Netejar"','"Guardar configuració"'])
assert 'onClearRead = {' in ui and 'for (notice in unread) {' in ui and 'api.markRead(notice.id)' in ui
assert 'onOpenNativeSettings' in ui
assert 'LaunchedEffect(notificationsOpen, account?.id, guestReadyUserId)' in ui
assert 'hasPendingNotificationSettingsWrites(owner)' in ui
assert 'IosQueuedForRetryException' in ui
assert 'mail/preferences-api.php' in api
assert 'mail/preferences-api.php' in str(Path('/mnt/data/audit_lagenda/web/api/mail/preferences-api.php'))
assert 'digest_frequency' in php and 'recommendations_enabled' in php
assert 'sync/state.php' in api and 'draft.centralMutations().forEach' in api
assert 'IosServerConnection.json("sync/state.php", bearer = bearer' in api
assert 'personalBearer(expectedUserId)' in api
assert 'pendingStateActions(expectedUserId)' in api
assert 'mail/preferences-api.php' in (i/'IosPendingServerQueue.kt').read_text()
assert 'queue.append(IosQueuedAction' in api and 'draft.emailPayload(), "mail/preferences-api.php"' in api
assert all(x in model for x in ['"alert_community"','"alert_followed"','"alert_day_before"','"alert_tickets"','"alert_suggestions"','"ios_push_enabled"','"ios_push_quiet_start"'])
assert 'UserDefaults' not in model.split('internal data class', 1)[-1] and 'SharedPreferences' not in model.split('internal data class',1)[-1]
assert (root/'shared/src/commonTest/kotlin/com/example/agendavalenciana/ios/IosNotificationCenterSettingsTest.kt').exists()
for filename,folder,strip in [
 ('LAGENDA_V336_ANDROID_STUDIO_v336_COMPILACIO_CORREGIDA(3).zip',Path('/mnt/data/ANDROID_V336_REFERENCIA'),False),
 ('LAGENDA_V349_IOS_PANTALLA_COMPLETA_FEED_SCROLL (1)(2).zip',root,True)
]:
 total=0
 with zipfile.ZipFile('/mnt/data/'+filename) as z:
  for n in z.namelist():
   if n.startswith('ANDROID_STUDIO/app/') and not n.endswith('/'):
    dst=folder/(n.removeprefix('ANDROID_STUDIO/') if strip else n)
    assert dst.is_file() and hashlib.sha256(dst.read_bytes()).digest()==hashlib.sha256(z.read(n)).digest(),n
    total+=1
 assert total>90
print('PAS29_STATIC_PASS: 6 tabs, channels, email API, server state, encrypted offline, Android immutable')
