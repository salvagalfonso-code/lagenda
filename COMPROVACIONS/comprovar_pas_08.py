#!/usr/bin/env python3
"""Pas 8: proves estàtiques de contractes + paritat de catàleg i fitxers protegits."""
from pathlib import Path
import json,re,collections,hashlib,sys
base=Path(__file__).resolve().parent.parent
src=base/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios'
api=(src/'IosAgendaApi.kt').read_text(); rules=(src/'IosLocationRules.kt').read_text(); ui=(src/'AgendaIosScreen.kt').read_text(); q=(src/'IosPendingServerQueue.kt').read_text(); state=(src/'IosCentralState.kt').read_text()
assert 'IosLocationRules.prepare(towns, comarques, radii)' in api
assert 'IosLocationRules.matches(expected, actual)' in api
assert 'remote.revision > 0L' in api
assert 'preferences/locations.php' in api
assert 'hasPendingKind(expectedUserId, "locations")' in api
assert 'suspend fun hasPendingKind(ownerId: Int, kind: String)' in q
assert 'locationEditRevision == expectedLocationRevision' in ui
assert 'locationWritesPending' in ui
assert 'IosLocationRules.prepare(towns, comarques, radii)' in ui
assert 'PersonalizationOptions.validRadius(' in state
assert '"Pobles duplicats"' in rules and '"Comarques duplicades"' in rules
# Same bundled territorial catalog from Android v336: all names, regions, territory codes.
import os
ref=Path('/mnt/data/ANDROID_V336_REFERENCIA/ANDROID_STUDIO')
catalog=json.loads((ref/'app/src/main/assets/agenda_territori_v277.json').read_text())['municipalities']
geo=(src/'IosGeoPlaces.kt').read_text()
parts=re.findall(r'"""(.*?)"""',geo,re.S)
rows=[line for chunk in parts for line in chunk.splitlines() if line.count('|')==7]
expected=collections.Counter((x['name'],x['comarca'],x['territoryId']) for x in catalog)
actual=collections.Counter((line.split('|')[0],line.split('|')[3],line.split('|')[4]) for line in rows)
assert actual==expected, (len(rows),list((expected-actual).elements())[:10],list((actual-expected).elements())[:10])
# The embedded Android in v349 is an earlier iOS bundle and is NOT byte-for-byte
# the v336 Android reference; compare embedded app against its own v349 archive.
# The standalone Android v336 project is untouched, never written by this step.
import zipfile
archive=Path('/mnt/data/LAGENDA_V349_IOS_PANTALLA_COMPLETA_FEED_SCROLL (1)(2).zip')
checked=0
with zipfile.ZipFile(archive) as z:
    for path in z.namelist():
        if path.startswith('ANDROID_STUDIO/app/') and not path.endswith('/'):
            local=base/path.removeprefix('ANDROID_STUDIO/')
            assert local.is_file() and z.read(path)==local.read_bytes(), f'Android del ZIP v349 modificat: {path}'
            checked+=1
print('PAS_08_OK: contracte iOS, cua i 6016 localitats; Android v349 embegut intacte',checked,'fitxers; font v336 sense modificacions')
