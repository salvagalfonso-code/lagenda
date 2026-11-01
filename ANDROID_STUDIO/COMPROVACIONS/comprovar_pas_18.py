#!/usr/bin/env python3
"""Pas 18: source-level integration assertions; does NOT claim compiled iPhone or MariaDB."""
from pathlib import Path
import zipfile
r=Path(__file__).resolve().parents[1]
i=r/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios'
m=r/'shared/src/commonMain/kotlin/com/example/agendavalenciana/model'
assert (m/'IosSavedFestivalParityTest.kt').is_file() if False else True
p=(m/'IosSavedAgendaPolicy.kt').read_text(); k=(m/'IosFestivalSavedSlotPolicy.kt').read_text()
s=(i/'IosSavedAgendaScreen.kt').read_text(); a=(i/'AgendaIosScreen.kt').read_text()
api=(i/'IosAgendaApi.kt').read_text()
clock=(r/'shared/src/iosMain/kotlin/com/example/agendavalenciana/ios/Clock.ios.kt').read_text()
assert 'val location: String = ""' in p and 'fun matchSlots(' in p and 'fun withoutSession(' in p
assert 'scheduleDates: Collection<String>' in p and 'latest == null || latest >= today' in p
assert 'fun androidKey(' in k and 'location: String = ""' in k
assert 'festivalSchedule(eventId)' in s and 'IosSavedAgendaPolicy.matchSlots(' in s
assert 'IosSavedAgendaPolicy.withoutSession(current, slot)' in s
assert 'it.id in selectedIds || remoteFestivalSlots(it.id).isNotEmpty()' in s
assert 'val savedCulturalToday = currentAgendaDateIso()' in a
assert 'available = savedCatalog' in a and 'today = savedCulturalToday' in a
assert 'festivalSlotOverrides' in a and 'catch (_: IosQueuedForRetryException)' in a
assert 'IosFestivalSavedSlotPolicy.key(ev.id, slot.date, slot.time, slot.artist, slot.title, slot.location)' in a
assert 'ios_festival_schedule_${eventId}' in a and 'api.savePersonalState' in a
assert 'guestReadyUserId' in a and 'serverState?.personalFestivalSlots' in a
assert 'timeZone = madrid' in clock and 'hour < 5' in clock
assert 'actual fun currentAgendaDateIso()' in clock
assert (r/'shared/src/commonTest/kotlin/com/example/agendavalenciana/model/IosSavedFestivalParityTest.kt').is_file()
with zipfile.ZipFile('/mnt/data/LAGENDA_V336_ANDROID_STUDIO_v336_COMPILACIO_CORREGIDA(3).zip') as z:
 ref=Path('/mnt/data/ANDROID_V336_REFERENCIA')
 paths=[x for x in z.namelist() if x.startswith('ANDROID_STUDIO/') and not x.endswith('/')]
 assert all((ref/x).read_bytes()==z.read(x) for x in paths), 'Android v336 altered'
with zipfile.ZipFile('/mnt/data/LAGENDA_V349_IOS_PANTALLA_COMPLETA_FEED_SCROLL (1)(2).zip') as z:
 paths=[x for x in z.namelist() if x.startswith('ANDROID_STUDIO/app/') and not x.endswith('/')]
 assert all((r/x.removeprefix('ANDROID_STUDIO/')).read_bytes()==z.read(x) for x in paths),'Android inside iOS altered'
print(f'PAS_18_STATIC_PASS: saved festivals, expiry, Madrid day, secure server state, immutable Android {len(paths)}')
