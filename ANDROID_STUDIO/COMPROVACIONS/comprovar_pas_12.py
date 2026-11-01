#!/usr/bin/env python3
"""Pas 12: static regression / PHP contract / immutable Android. Not an iPhone test."""
from pathlib import Path
import hashlib, zipfile
root=Path(__file__).resolve().parents[1]
ios=root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios'
api=(ios/'IosAgendaApi.kt').read_text()
ui=(ios/'AgendaIosScreen.kt').read_text()
selector=(ios/'IosForYouServerSelection.kt').read_text()
policy=(ios/'IosPersonalFeedPolicy.kt').read_text()
android=Path('/mnt/data/ANDROID_V336_REFERENCIA/ANDROID_STUDIO/app/src/main/java/com/example/agendavalenciana')
php=Path('/mnt/data/audit_ios_port/s/api/for-you')
assert 'for-you/index.php?sig=' in api and 'for-you/feed.php' in api
assert 'forYouServerSelection' in api and 'activePersonalId() == expectedOwner' in api
assert 'hasPendingPersonalWrites' in api and 'personalBearer(expectedOwner)' in api
assert 'androidSignature(state: IosHydratedServerState?' in selector
assert 'source' in selector and 'server-v1' in selector and 'android-v329' in selector
assert 'toSortedMap()' not in selector
assert 'IosForYouServerOrder.payload' in ui and 'IosForYouServerOrder.select' in ui
assert 'IosForYouSnapshot.select' in ui and 'IosForYouServerOrder.androidSignature' in ui
assert 'DefaultFeedPolicy.hasExplicitSignals' in ui
assert 'ForYouHundredPolicy.selectDetailed' in policy
assert (android/'model/ForYouHundredPolicy.kt').read_text() == (root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/model/ForYouHundredPolicy.kt').read_text()
assert 'calculated_on_server' in (php/'feed.php').read_text()
assert 'for_you_server_cache' in (php/'feed.php').read_text()
assert 'hard_signature' in (php/'index.php').read_text()
assert 'for_you_feed_cache' in (php/'index.php').read_text()
assert 'fy_select' in (php/'_engine.php').read_text()
ref=Path('/mnt/data/LAGENDA_V336_ANDROID_STUDIO_v336_COMPILACIO_CORREGIDA(3).zip')
with zipfile.ZipFile(ref) as z:
    files=[n for n in z.namelist() if n.startswith('ANDROID_STUDIO/') and not n.endswith('/')]
    for n in files:
        assert (Path('/mnt/data/ANDROID_V336_REFERENCIA')/n).read_bytes()==z.read(n),n
with zipfile.ZipFile('/mnt/data/LAGENDA_V349_IOS_PANTALLA_COMPLETA_FEED_SCROLL (1)(2).zip') as z:
    files2=[n for n in z.namelist() if n.startswith('ANDROID_STUDIO/app/') and not n.endswith('/')]
    for n in files2:
        assert (root/n.removeprefix('ANDROID_STUDIO/')).read_bytes()==z.read(n),n
print('PAS_12_PASS: PHP protocols, honest cache source, owner guard, Android policy, immutable Android',len(files),len(files2))
