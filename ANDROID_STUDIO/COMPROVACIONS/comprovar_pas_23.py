#!/usr/bin/env python3
"""Pas 23: contrast amb Android + invariància (no prova la compilació iPhone)."""
from pathlib import Path
import hashlib,zipfile
root=Path(__file__).resolve().parents[1]
s=root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios'
geo=(s/'IosGeoPlaces.kt').read_text()
advanced=(s/'IosAdvancedSearch.kt').read_text()
ui=(s/'AgendaIosScreen.kt').read_text()
event=(s/'EventCatalog.kt').read_text()
checks={
 'catàleg estructurat': 'fun municipalityEntries()' in geo and 'val official: String' in geo,
 'pobles homònims': 'listOf(norm(it.name), norm(it.comarca), it.territory)' in geo,
 'àlies oficials': 'candidate.aliases' in advanced and 'town.official' in advanced,
 'subtítol de comarca': 'town.comarca.takeIf(String::isNotBlank)' in advanced,
 'comarca remota real': 'comarca = obj.text("comarca", "county", "region")' in event,
 'comparació filtrada de comarca': 'event.comarca.ifBlank' in advanced,
 'rànquing Android': 'val rank = (listOf(candidate.value) + candidate.aliases)' in advanced,
 'llista acotada': 'if (best.size > max) best.removeAt(best.lastIndex)' in advanced,
 'precarrega en background': 'IosGeoPlaces.municipalityEntries()' in ui and 'withContext(Dispatchers.Default)' in ui,
 'desacoblament de feed': 'territoryTownDraft' in ui and 'territoryComarcaDraft' in ui,
 'visual del subtítol': 'Text(town.subtitle, fontSize = 10.sp' in ui,
 'no camp de font alternatiu local': 'UserDefaults' not in advanced,
 'tests mantinguts': (root/'shared/src/commonTest/kotlin/com/example/agendavalenciana/ios/IosTerritorySuggestionsTest.kt').exists(),
}
assert all(checks.values()),[k for k,v in checks.items() if not v]
for source in ['/mnt/data/LAGENDA_V336_ANDROID_STUDIO_v336_COMPILACIO_CORREGIDA(3).zip',
               '/mnt/data/LAGENDA_V349_IOS_PANTALLA_COMPLETA_FEED_SCROLL (1)(2).zip']:
 with zipfile.ZipFile(source) as z:
  num=0
  for name in z.namelist():
   if name.startswith('ANDROID_STUDIO/app/') and not name.endswith('/'):
    target=(root/name.removeprefix('ANDROID_STUDIO/')) if 'V349' in source else (Path('/mnt/data/ANDROID_V336_REFERENCIA')/name)
    assert target.is_file() and hashlib.sha256(target.read_bytes()).digest()==hashlib.sha256(z.read(name)).digest(),name
    num+=1
  assert num>90
print('PAS23_STATIC_PASS: territori, noms oficials/comarques/homònims, Android immutable')
