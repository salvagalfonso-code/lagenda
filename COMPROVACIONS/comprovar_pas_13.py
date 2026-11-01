#!/usr/bin/env python3
"""Step 13 static integration + immutable-source check. Not an iOS build."""
from pathlib import Path
import zipfile
root=Path(__file__).resolve().parents[1]
ios=root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios'
policy=(ios/'IosUnfilteredAgenda.kt').read_text()
ui=(ios/'AgendaIosScreen.kt').read_text()
catalog=(ios/'EventCatalog.kt').read_text()
assert 'IosUnfilteredAgenda.select(available, today,' in ui
assert '.filter { IosUnfilteredAgenda.hasFutureDate(it, today) }' in ui
assert 'IosForYouServerOrder.select' in ui and 'IosForYouSnapshot.select' in ui
assert 'IosFeedExclusionPolicy.visible' in ui and 'GlobalAdPlacementPolicy' in ui
assert 'IosFeedReadingPolicy.page' in ui and 'IosUnfilteredFeedPolicy.nextCount' in (ios/'IosFeedReadingSession.kt').read_text()
assert 'val displayDate get() = feedOccurrenceDate.ifBlank { date }' in catalog
assert 'val key get() = "${id}_${displayDate}"' in catalog
assert 'date = obj.text("event_date", "start_date", "date", "feed_instance_date", "selected_date")' in catalog
assert 'feedOccurrenceDate = obj.text("feed_instance_date", "selected_date")' in catalog
assert 'isConsecutiveFestival' in policy and 'isFestivalBeforeStart' in policy
assert 'private fun longExhibition' in policy
assert 'hasFutureDate' in policy and 'scheduledDays' in policy
assert 'currentLocalDateIso() // Re-evaluate' in ui
assert (root/'shared/src/commonTest/kotlin/com/example/agendavalenciana/ios/IosUnfilteredAgendaTest.kt').exists()
with zipfile.ZipFile('/mnt/data/LAGENDA_V336_ANDROID_STUDIO_v336_COMPILACIO_CORREGIDA(3).zip') as z:
    original=[n for n in z.namelist() if n.startswith('ANDROID_STUDIO/') and not n.endswith('/')]
    for n in original:
        assert (Path('/mnt/data/ANDROID_V336_REFERENCIA')/n).read_bytes()==z.read(n),n
with zipfile.ZipFile('/mnt/data/LAGENDA_V349_IOS_PANTALLA_COMPLETA_FEED_SCROLL (1)(2).zip') as z:
    original=[n for n in z.namelist() if n.startswith('ANDROID_STUDIO/app/') and not n.endswith('/')]
    for n in original:
        assert (root/n.removeprefix('ANDROID_STUDIO/')).read_bytes()==z.read(n),n
print('PASS 13: chronological feed, explicit days, festivals/expositions, existing ads and cache, immutable Android',len(original))
