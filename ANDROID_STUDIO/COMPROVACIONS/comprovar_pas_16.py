#!/usr/bin/env python3
"""Static regression for event saving; does not prove actual iPhone/server behavior."""
from pathlib import Path
import zipfile
root=Path(__file__).resolve().parents[1]
ios=root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios'
api=(ios/'IosAgendaApi.kt').read_text()
ui=(ios/'AgendaIosScreen.kt').read_text()
queue=(ios/'IosPendingServerQueue.kt').read_text()
contract=(ios/'IosSavedEventContract.kt').read_text()
assert 'events/save.php' in api and 'events/saved.php' in api
assert 'IosSavedEventContract.verifyPost(response, action.payload)' in api
assert 'IosSavedEventContract.verifyCanonicalList(remoteSaved, action.payload)' in api
assert 'reconcilePendingUserState' in api and 'pendingSavedActions(expectedId)' in api
assert 'pendingSavedActions(ownerId:' in queue
assert 'if (account == null) { loginOpen = true; return }' not in ui[ui.index('fun toggleSaved('):ui.index('fun toggleReminder(')]
assert 'api.saveEvent(event, desired,' in ui
assert 'savedEditRevision' in ui and 'savingEventIds' in ui
assert 'savedOwnerId = account?.id ?: guestReadyUserId' in ui
assert 'api.userState()' in ui and 'tab == "Guardats"' in ui
assert 'put("event_id", event.id)' in contract and 'put("saved", saved)' in contract
assert 'event.feedOccurrenceDate' not in contract
assert (root/'shared/src/commonTest/kotlin/com/example/agendavalenciana/ios/IosSavedEventContractTest.kt').is_file()
with zipfile.ZipFile('/mnt/data/LAGENDA_V336_ANDROID_STUDIO_v336_COMPILACIO_CORREGIDA(3).zip') as z:
    ref=Path('/mnt/data/ANDROID_V336_REFERENCIA')
    files=[s for s in z.namelist() if s.startswith('ANDROID_STUDIO/') and not s.endswith('/')]
    assert all((ref/s).read_bytes()==z.read(s) for s in files), 'Android reference changed'
with zipfile.ZipFile('/mnt/data/LAGENDA_V349_IOS_PANTALLA_COMPLETA_FEED_SCROLL (1)(2).zip') as z:
    files=[s for s in z.namelist() if s.startswith('ANDROID_STUDIO/app/') and not s.endswith('/')]
    assert all((root/s.removeprefix('ANDROID_STUDIO/')).read_bytes()==z.read(s) for s in files), 'embedded Android changed'
print('PAS_16_STATIC_PASS: guest/registered saves, verified receipts, pending overlay, stable event IDs, Android untouched')
