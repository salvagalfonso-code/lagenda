#!/usr/bin/env python3
"""Pas 15 contract checks; NOT an end-to-end simulator or iPhone build."""
from pathlib import Path
import subprocess, zipfile
root=Path(__file__).resolve().parents[1]
ios=root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios'
api=(ios/'IosAgendaApi.kt').read_text()
ui=(ios/'AgendaIosScreen.kt').read_text()
contract=(ios/'IosCommercialVisibility.kt').read_text()
feed=(ios/'IosUnfilteredAgenda.kt').read_text()
policy=(root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/model/GlobalAdPlacementPolicy.kt').read_bytes()
android_policy=Path('/mnt/data/ANDROID_V336_REFERENCIA/ANDROID_STUDIO/app/src/main/java/com/example/agendavalenciana/model/GlobalAdPlacementPolicy.kt').read_bytes()
assert policy==android_policy,'the same source of sponsored slot placement as Android v336'
assert 'ads/active-global.php' in api and 'ads/active.php' in api and 'ads/visual-map.php' in api and 'ads/track.php' in api
assert 'IosCommercialVisuals.apply(inventory.ads, visuals)' in api
assert 'IosCommercialVisibility.guaranteed(available, commercialIds)' in ui
assert 'IosCommercialVisibility.protected(ev, protectedIds)' in ui
assert 'IosCommercialVisibility.protected(ev, protectedIds + advertisedEventIds)' in ui
assert 'IosCommercialVisibility.protected(event, protectedIds)' in feed
assert 'event.ticketAvailable && event.ticketUrl.isBlank()' in contract
assert 'paid.priorityEvents' in ui and 'priority_event_ids' in api
assert 'platform_ticket_event_ids' in api and 'paid_ticket_event_ids' in api
for placement in ('HEADER','FEED_PREMIUM','FEED_NORMAL','POPUP'):
 assert placement in ui and placement in api
assert 'GlobalAdPlacementPolicy.premiumAt(index' in ui and 'GlobalAdPlacementPolicy.normalAt(index' in ui
assert 'GlobalAdPlacementPolicy.premiumAfterLast(total' in ui and 'GlobalAdPlacementPolicy.normalAfterLast(total' in ui
assert 'onViewed(ad)' in ui and 'onOpened(ad, it)' in ui
assert (root/'shared/src/commonTest/kotlin/com/example/agendavalenciana/model/IosCommercialVisibilityTest.kt').is_file()
with zipfile.ZipFile('/mnt/data/LAGENDA_V336_ANDROID_STUDIO_v336_COMPILACIO_CORREGIDA(3).zip') as z:
    ref=Path('/mnt/data/ANDROID_V336_REFERENCIA')
    files=[p for p in z.namelist() if p.startswith('ANDROID_STUDIO/') and not p.endswith('/')]
    assert all((ref/p).read_bytes()==z.read(p) for p in files),'Android reference altered'
with zipfile.ZipFile('/mnt/data/LAGENDA_V349_IOS_PANTALLA_COMPLETA_FEED_SCROLL (1)(2).zip') as z:
    files=[p for p in z.namelist() if p.startswith('ANDROID_STUDIO/app/') and not p.endswith('/')]
    assert all((root/p.removeprefix('ANDROID_STUDIO/')).read_bytes()==z.read(p) for p in files),'embedded Android altered'
print('PAS_15_STATIC_PASS: Android exact ad slots, global+fallback+visual, protected internal tickets and ad placements, Android immutable')
