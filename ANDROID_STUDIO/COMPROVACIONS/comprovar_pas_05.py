#!/usr/bin/env python3
"""Step 5 source-contract, source isolation and static behavior checks, not native compile."""
from pathlib import Path
import hashlib, re, zipfile
base=Path(__file__).resolve().parents[1]
root=base.parent
work=root/'ANDROID_STUDIO'
code=base/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios'
ui=(code/'AgendaIosScreen.kt').read_text()
api=(code/'IosAgendaApi.kt').read_text()
screen=(code/'IosSavedAgendaScreen.kt').read_text()
state=(code/'IosCentralState.kt').read_text()
mut=(code/'IosPersonalMutations.kt').read_text()
swift=(base/'iosApp/AgendaIOS/AgendaKeychain.swift').read_text()
php=(root.parent/'audit_lagenda/web/api/sync/state.php').read_text()
locations=(root.parent/'audit_lagenda/web/api/preferences/locations.php').read_text()
feed=(root.parent/'audit_lagenda/web/api/preferences/feed.php').read_text()
assert 'mutations' in php and 'applied_revision' in php and 'ALREADY_APPLIED' in php
assert 'user_id' in php and 'revision' in php and 'payload_hash' in php
assert 'locations_clean_towns' in locations and "['towns']" in locations
assert "['discovery']" in feed and 'suggestions' in feed and 'valencian_first' in feed
assert 'private val personalWriteMutex = Mutex()' in api and 'personalWriteMutex.withLock' in api
assert '"sync/state.php"' in api and 'queuedWrite(expectedUserId, "state"' in api
assert 'ackIds == expectedIds' in api and 'IosPendingServerQueue' in api
assert 'check(activePersonalId() == expectedUserId)' in api
assert '"preferences/locations.php"' in api and '"preferences/feed.php"' in api
assert all(s in mut for s in ('"set"','"string"','"bool"','"long"','Random.Default.nextBytes(16)'))
# Exclusion and affinity keys were migrated intentionally in step 11 to Android's
# visibility/index.php and affinities/record.php. No iOS-only mirror is authoritative.
assert all(s not in ui for s in ('"ios_hidden_events_v1"','"ios_hidden_artists_v1"',
    '"ios_hidden_categories_v1"','"ios_recent_styles_v1"'))
# Pas 28 moved reminder mutation construction to the single API writer.
assert '"ios_festival_schedule_${eventId}"' in ui
assert '"when_${event.id}"' in api and 'api.configureEventReminder(' in ui
assert 'api.updateVisibility' in ui and 'api.recordOpenAffinity' in ui
assert 'platformWritePublicCache' not in screen and 'platformReadPublicCache' not in screen
assert not re.search(r'platform(?:Write|Read)PublicCache\([^\n]*(?:pref_|hidden_|festival_schedule|recent_styles|when_)', ui)
assert 'IosPersonalEncoded.strings' in ui and 'api.savePersonalState' in ui
assert 'saveFestivalSlots(next)' in ui
assert 'if (slotSaving) return' in ui
assert 'serverState?.personalFestivalSlots' in ui
assert 'local-cache' not in screen and 'IosAccountCachePolicy.key("festival_schedule_' not in screen
assert 'reminderEpochMillis(isoDate: String) -> Int64' in swift
assert 'formatReminderEpochMillis(epochMillis: Int64) -> String' in swift
assert 'IosRecentInterest.hydrate' not in ui and 'IosAffinitySignals.styleWeights' in (code/'IosForYouSnapshot.kt').read_text()
assert 'platformWritePublicCache' not in (code/'IosRecentInterest.kt').read_text()
assert 'platformWritePublicCache' not in (code/'IosForYouSnapshot.kt').read_text()
for sourcezip, prefix, dest in [
    (root.parent/'LAGENDA_V336_ANDROID_STUDIO_v336_COMPILACIO_CORREGIDA(3).zip','ANDROID_STUDIO/',root.parent/'ANDROID_V336_REFERENCIA'),
    (root.parent/'LAGENDA_V349_IOS_PANTALLA_COMPLETA_FEED_SCROLL (1)(2).zip','ANDROID_STUDIO/app/',root)]:
    with zipfile.ZipFile(sourcezip) as z:
        names=[n for n in z.namelist() if n.startswith(prefix) and not n.endswith('/')]
        for name in names:
            assert hashlib.sha256(z.read(name)).digest()==hashlib.sha256((dest/name).read_bytes()).digest(),name
        print('PASS immutable:', len(names), sourcezip.name)
print('PASS 05: acknowledged writes, dedicated domains, server-only personal storage, account guards, Android immutable')
