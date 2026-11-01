#!/usr/bin/env python3
"""Pas 28: source contracts, no device-local scheduler, server projections, immutable Android."""
from pathlib import Path
import hashlib, zipfile
base=Path(__file__).resolve().parents[1]
src=base/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios'
api=(src/'IosAgendaApi.kt').read_text()
ui=(src/'AgendaIosScreen.kt').read_text()
queue=(src/'IosPendingServerQueue.kt').read_text()
policy=(src/'IosReminderPolicy.kt').read_text()
php=Path('/mnt/data/audit_lagenda/web/api/sync/state.php').read_text()
event=Path('/mnt/data/audit_lagenda/web/api/user-state/event-alert.php').read_text()
artist=Path('/mnt/data/LAGENDA_IOS_TREBALL_ACUMULATIU/SERVIDOR_APNS/api/notifications/artist-bell.php').read_text()
assert 'agenda_user_reminders' in php and "if ($field==='when')" in php and 'ALREADY_APPLIED' in php
assert 'user_event_alerts' in event and "'active'=>$active" in event
assert 'ios_artist_bells' in artist and "['IMMEDIATE','DAILY','WEEKLY']" in artist
assert 'private val reminderWriteMutex = Mutex()' in api
assert 'suspend fun configureEventReminder' in api
assert 'queue.append(IosQueuedAction(IosPersonalMutationId.make(), owner, "state", state))' in api
assert '"user-state/event-alert.php"))' in api and '"when_${event.id}"' in api
assert '"event_ids"' in api and 'IosPersonalEncoded.strings(changedIds)' in api
assert 'sync/state.php' in api and 'user-state/mine.php' in api
assert 'notifications/artist-bell.php' in queue
assert 'pendingEventAlertActions' in queue and 'pendingArtistBellActions' in queue
assert 'pendingArtistBellActions(owner)' in api
assert 'pendingEventAlertActions(expectedId)' in api
assert 'user-state/event-alert.php' in api and 'ids) == enabled' in api
assert 'artist-bell.php' in api and 'current.text("frequency") == freq' in api
assert 'if (account == null) { loginOpen = true; return }' not in ui[ui.index('fun toggleReminder('):ui.index('/** Android v336: entity_type')]
assert 'val requestUserId = account?.id ?: guestReadyUserId' in ui
assert 'api.configureEventReminder(' in ui
assert 'platformScheduleReminder(' not in ui  # no persistent local scheduling
assert 'platformCancelReminder(requestUserId, event.id)' in ui  # cleanup of old duplicate alarms
assert 'ReminderEditorDialog(ev, account?.id ?: guestReadyUserId ?: 0' in ui
assert 'IosReminderPolicy.validLocalDateTime' in ui
assert 'artistBells = IosArtistBellPolicy.update(artistBells, saved)' in ui
assert 'Campaneta pendent de sincronització' in ui
assert 'fun projectedAlerts(' in policy and 'fun updatedIds(' in policy
assert (base/'shared/src/commonTest/kotlin/com/example/agendavalenciana/ios/IosReminderPolicyTest.kt').exists()
# Original app module, including all of Android, must remain byte-identical.
for filename, folder, original_prefix in [
    ('LAGENDA_V336_ANDROID_STUDIO_v336_COMPILACIO_CORREGIDA(3).zip', Path('/mnt/data/ANDROID_V336_REFERENCIA'),''),
    ('LAGENDA_V349_IOS_PANTALLA_COMPLETA_FEED_SCROLL (1)(2).zip',base,'')
]:
    count=0
    with zipfile.ZipFile('/mnt/data/'+filename) as z:
        for n in z.namelist():
            if n.startswith('ANDROID_STUDIO/app/') and not n.endswith('/'):
                target=folder/(n if filename.startswith('LAGENDA_V336') else n.removeprefix('ANDROID_STUDIO/'))
                assert target.is_file(),n
                assert hashlib.sha256(z.read(n)).digest()==hashlib.sha256(target.read_bytes()).digest(),n
                count+=1
    assert count>90
print('PAS28_STATIC_PASS: server event + artist bells, secure offline queue, guest access, no device scheduler, Android immutable')
