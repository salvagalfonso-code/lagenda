#!/usr/bin/env python3
from pathlib import Path
import hashlib,zipfile
root=Path(__file__).resolve().parents[1]
s=root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios'
contract=(s/'IosSearchPresetContract.kt').read_text()
api=(s/'IosAgendaApi.kt').read_text()
ui=(s/'AgendaIosScreen.kt').read_text()
advanced=(s/'IosAdvancedSearch.kt').read_text()
checks={
 'contracte Android categories/data/mes/lloc/preu/gratuït': all(k in contract for k in ['dateFrom','dateTo','maxDistanceKm','maxPrice','freeOnly','categories','month','comarca','town']),
 'no guardar estil en API sense suport': 'require(filters.style.isBlank())' in contract,
 'guardat per server PHP': 'put("action", "save")' in api and 'preferences/search-filters.php' in api,
 'presets convidat amb token propi': 'guestSession.ensure()' in api and 'searchFilterIdentity' in api,
 'verificació POST i GET per guardat': 'require(result.bool("ok") && result.bool("saved"))' in api and 'IosSearchPresetContract.same(it.filters, filters)' in api,
 'verificació delete': 'require(result.bool("ok") && result.bool("deleted"))' in api and 'actual.none { it.name.equals(cleaned' in api,
 'identitat congelada': 'ensureSearchIdentity(identity)' in api,
 'no local persistent': not any(x in contract for x in ['UserDefaults.', 'SharedPreferences.']),
 'filtres multidisciplinaris': 'filters.categoryCodes.isNotEmpty()' in advanced,
 'mes i preu filtrats': 'filters.month.isNotBlank()' in advanced and 'filters.maxPrice != null' in advanced,
 'dues accions a la mateixa fila': 'Text("Aplicar")' in ui and 'Text(if (searchPresetSaving) "Guardant…" else "Guardar")' in ui,
 'popup de filtres': 'Dialog(onDismissRequest = { searchFiltersOpen = false })' in ui,
 'presets també per a convidats': 'LaunchedEffect(account?.id, guestReadyUserId, searchFiltersOpen)' in ui and 'api.savedSearches()' in ui,
 'preset complet recuperat': 'searchMonth = f.month; searchPriceLimit = f.maxPrice' in ui,
 'cap nivell de feed recalculat per tecla': 'appliedSearchFilters = draft;' in ui,
 'commonTest': (root/'shared/src/commonTest/kotlin/com/example/agendavalenciana/ios/IosSearchPresetContractTest.kt').exists(),
}
assert all(checks.values()),[k for k,v in checks.items() if not v]
for source in ['/mnt/data/LAGENDA_V336_ANDROID_STUDIO_v336_COMPILACIO_CORREGIDA(3).zip',
               '/mnt/data/LAGENDA_V349_IOS_PANTALLA_COMPLETA_FEED_SCROLL (1)(2).zip']:
 with zipfile.ZipFile(source) as z:
  n=0
  for name in z.namelist():
   if name.startswith('ANDROID_STUDIO/app/') and not name.endswith('/'):
    target=(root/name.removeprefix('ANDROID_STUDIO/')) if 'V349' in source else (Path('/mnt/data/ANDROID_V336_REFERENCIA')/name)
    assert target.is_file() and hashlib.sha256(target.read_bytes()).digest()==hashlib.sha256(z.read(name)).digest(),name
    n+=1
  assert n>90
print('PAS24_STATIC_PASS: full Android filters, server-only guest/registered, confirmed CRUD, Android immutable')
