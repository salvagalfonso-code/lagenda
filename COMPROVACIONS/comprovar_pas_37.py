#!/usr/bin/env python3
"""Step 37 static regression / immutable Android + 36 prior files from uploaded archive."""
from pathlib import Path
import hashlib, zipfile
root=Path(__file__).resolve().parents[1]
work=root.parent
source=Path('/mnt/data/LAGENDA_IOS_V349_PASSOS_01_AL_36_ACUMULATIU(1).zip')
assert source.exists(), 'No existeix la base 1-36 original'
modified={
    'ESTAT_TREBALL_PASSOS.md',
    'ANDROID_STUDIO/shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosAgendaApi.kt',
    'ANDROID_STUDIO/shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/AgendaIosScreen.kt'
}
original_files=0
with zipfile.ZipFile(source) as z:
    for name in z.namelist():
        if name.endswith('/'):
            continue
        dest=work/name
        assert dest.is_file(), 'Original del pas 1-36 perdut: '+name
        if name not in modified:
            assert hashlib.sha256(dest.read_bytes()).digest()==hashlib.sha256(z.read(name)).digest(),name
            original_files+=1
        if name.startswith(('ANDROID_STUDIO/app/','ANDROID_STUDIO/iosApp/')):
            assert dest.read_bytes()==z.read(name), 'Android o codi natiu Swift alterats: '+name
src=root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios'
a=(src/'IosAgendaApi.kt').read_text()
u=(src/'AgendaIosScreen.kt').read_text()
p=(src/'IosTicketCatalogPolicy.kt').read_text()
for field in ['session_date','session_time','sector','offer','capacity','sold','reserved','max_per_order',
              'sale_starts_at','sale_ends_at','active','tickets/options.php','ticketCatalog(eventId']:
    assert field in a,field
for field in ['IosTicketCatalogPolicy.visible','IosTicketCatalogPolicy.days','IosTicketCatalogPolicy.sectors',
              'IosTicketCatalogPolicy.filter','IosTicketCatalogPolicy.quantityLimit','IosTicketCatalogPolicy.selectable',
              'api.ticketCatalog(event.id)','selectedDay','selectedZone','No hi ha cap modalitat',
              'selectedOption?.price == 0.0']:
    assert field in u,field
assert 'quantity in 1..6' not in a
assert 'price = option.text("price").toDoubleOrNull() ?: 0.0' not in a
assert 'price = option.text("price").toDoubleOrNull() ?: 0.0' not in u
assert 'capacity?.let { (it - sold - reserved).coerceAtLeast(0) }' in p
assert (root/'COMPROVACIONS/PAS_37_ENTRADES_TIPUS_DATES_QUANTITATS_ZONES.md').is_file()
assert (root/'shared/src/commonTest/kotlin/com/example/agendavalenciana/ios/IosTicketCatalogPolicyTest.kt').is_file()
for i in range(1,37):
    assert list((root/'COMPROVACIONS').glob(f'PAS_{i:02d}_*.md')),i
print(f'PAS37_STATIC_PASS preserved={original_files}, prior_steps=36; Android app/iosApp untouched')
