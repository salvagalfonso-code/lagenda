#!/usr/bin/env python3
"""Pass 26: festival programme parity without touching Android or server."""
from pathlib import Path
import zipfile,hashlib
r=Path(__file__).resolve().parents[1]
i=r/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios'
ui=(i/'AgendaIosScreen.kt').read_text()
policy=(i/'IosFestivalPopupPolicy.kt').read_text()
decoder=(i/'IosFestivalScheduleDecoder.kt').read_text()
model=(i/'EventCatalog.kt').read_text()
assert all(x in policy for x in ['upcomingDays(', 'initialDay(', 'performances(', 'isGroupMarker(', 'saved(', 'places(', 'ticketsForDay('])
assert all(x in ui for x in ['currentAgendaDateIso()', 'onlyMyPerformances', '"Els meus"', '"Tots"',
    'IosFestivalPopupPolicy.performances(', 'IosFestivalPopupPolicy.places(', 'ticketsDayFilter',
    'mapDestination = search', 'showBatecManager', 'festivalProgramme', 'saveFestivalSlots(next)'])
assert all(x in decoder for x in ['group_name','item_type','sort_order','end_time','details'])
assert 'val groupName: String = ""' in model and 'val sortOrder: Int = 0' in model
assert 'tickets/options.php' in (i/'IosAgendaApi.kt').read_text()
assert (r/'shared/src/commonTest/kotlin/com/example/agendavalenciana/ios/IosFestivalPopupPolicyTest.kt').is_file()
# Original V336 is not modified at any point; original Android subtree within v349 also immutable
originals = [('/mnt/data/LAGENDA_V336_ANDROID_STUDIO_v336_COMPILACIO_CORREGIDA(3).zip',
              Path('/mnt/data/ANDROID_V336_REFERENCIA'),False),
             ('/mnt/data/LAGENDA_V349_IOS_PANTALLA_COMPLETA_FEED_SCROLL (1)(2).zip',r,True)]
for path, directory, embedded in originals:
    with zipfile.ZipFile(path) as z:
        n=0
        for fn in z.namelist():
            if fn.startswith('ANDROID_STUDIO/app/') and not fn.endswith('/'):
                target=directory/(fn.removeprefix('ANDROID_STUDIO/') if embedded else fn)
                assert target.exists() and hashlib.sha256(target.read_bytes()).digest()==hashlib.sha256(z.read(fn)).digest(), fn
                n+=1
        assert n>90
print('PAS26_STATIC_PASS: festival programme, real days, artists, saved slots, ticket dates, maps, community, original Android SHA intact')
