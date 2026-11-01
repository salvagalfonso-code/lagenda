#!/usr/bin/env python3
"""Static safeguards; not an iPhone runtime test."""
from pathlib import Path
import zipfile
root=Path(__file__).resolve().parents[1]
ios=root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios'
ui=(ios/'AgendaIosScreen.kt').read_text()
policy=(ios/'IosFeedReadingSession.kt').read_text()
api=(ios/'IosAgendaApi.kt').read_text()
swift=(root/'iosApp/AgendaIOS/AgendaKeychain.swift').read_text()
app=(root/'iosApp/AgendaIOS/AgendaIOSApp.swift').read_text()
assert 'const val initial = 5' in policy and 'const val second = 20' in policy and 'const val step = 10' in policy
assert 'IosUnfilteredFeedPolicy.nextCount' in policy
assert 'const val oneHourMs = 60L * 60L * 1000L' in policy
assert 'sync/state.php' in api and 'savePersonalState(snapshot.ownerId' in ui
assert 'IosPersonalEncoded.string(snapshot.encode())' in ui
assert 'IosFeedReadingMemory()' in ui and 'readingMemory.importServer' in ui
assert 'restoreReading = ' in ui and 'readingRestoreEpoch = readingRestoreEpoch' in ui
assert 'firstVisibleItemScrollOffset' in ui and 'listState.scrollToItem(saved.firstIndex' in ui
assert 'onReadingPosition' in ui and '.debounce(900L)' in ui
assert 'onRefresh = {' in ui and 'refresh(true)' in ui and 'manualRefreshing' in ui
assert 'setFeedLifecycleListener' in api and swift and 'onChange(of: scenePhase)' in app
assert 'UserDefaults' not in policy and 'platformWritePublicCache' not in policy
assert (root/'shared/src/commonTest/kotlin/com/example/agendavalenciana/ios/IosFeedReadingSessionTest.kt').exists()
# Immutable reference and embedded Android projects.
with zipfile.ZipFile('/mnt/data/LAGENDA_V336_ANDROID_STUDIO_v336_COMPILACIO_CORREGIDA(3).zip') as z:
    names=[n for n in z.namelist() if n.startswith('ANDROID_STUDIO/') and not n.endswith('/')]
    for name in names:
        assert (Path('/mnt/data/ANDROID_V336_REFERENCIA')/name).read_bytes()==z.read(name),name
with zipfile.ZipFile('/mnt/data/LAGENDA_V349_IOS_PANTALLA_COMPLETA_FEED_SCROLL (1)(2).zip') as z:
    names=[n for n in z.namelist() if n.startswith('ANDROID_STUDIO/app/') and not n.endswith('/')]
    for name in names:
        assert (root/name.removeprefix('ANDROID_STUDIO/')).read_bytes()==z.read(name),name
print('PAS_14_STATIC_PASS: pagination, restored pixels, server-only persistence, Swift lifecycle, immutable Android')
