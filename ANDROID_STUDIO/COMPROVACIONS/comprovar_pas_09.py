#!/usr/bin/env python3
"""Pas 9: proves de compatibilitat Android/servidor i integritat acumulativa."""
from pathlib import Path
import re, zipfile
b=Path(__file__).resolve().parent.parent
src=b/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios'
android=Path('/mnt/data/ANDROID_V336_REFERENCIA/ANDROID_STUDIO')
reference=(android/'app/src/main/java/com/example/agendavalenciana/ui/LocalizedText.kt').read_text()
server=Path('/mnt/data/audit_ios_port/s/api/preferences/feed.php').read_text()
api=(src/'IosAgendaApi.kt').read_text()
ui=(src/'AgendaIosScreen.kt').read_text()
state=(src/'IosCentralState.kt').read_text()
contract=(src/'IosFeedPreferencesContract.kt').read_text()
queue=(src/'IosPendingServerQueue.kt').read_text()
assert 'private const val KEY = "selected_language"' in reference
assert 'ServerManagedPreferences.get(context)' in reference
for code in ['ca','es','en']:
    assert f'("{code}",' in contract, code
assert 'fun uiLanguage(): IosUiLanguage?' in state
assert 'central.string(IosFeedPreferencesContract.centralUiLanguageKey)' in state
for key in ['suggestions', 'discovery', 'valencian_first']:
    assert f"'{key}'" in server and f'"{key}"' in api
assert 'sync/state.php' in api and 'preferences/feed.php' in api
assert 'IosFeedPreferencesContract.validDiscovery(discovery)' in api
assert 'check(response["exists"]?.jsonPrimitive?.booleanOrNull == true)' in api
assert 'hasPendingFeedWrites(expectedId)' in ui
assert 'feedEditRevision == expectedFeedRevision' in ui
assert 'hasPendingKind(expectedUserId, "feed")' in api
assert 'hasPendingKind(expectedUserId, "state")' in api
assert 'suspend fun hasPendingKind(ownerId: Int, kind: String)' in queue
assert 'IosUiLanguage.entries.forEach' in ui
assert 'IosFeedPreferencesContract.centralUiLanguageKey to IosPersonalEncoded.string(language.code)' in ui
assert 'IosFeedPreferencesContract.displayDiscovery(suggestionsEnabled)' in ui
assert 'selectedUiLanguage' in ui and 'uiLanguage = language' in ui
assert '"show_suggestions" to IosPersonalEncoded.bool(suggestions)' in ui
assert '"discovery_level" to IosPersonalEncoded.string(discovery.name)' in ui
assert '"valencian_first" to IosPersonalEncoded.bool(valencian)' in ui
assert 'feedEditRevision += 1' in ui
assert 'saveFeedPreferences(expectedId, suggestions, discovery.name, valencian)' in ui
assert '"selected_language"' in contract and '"selected_language"' in reference
# Embedded Android v349 is a reference snapshot, and stays untouched byte-for-byte.
original=Path('/mnt/data/LAGENDA_V349_IOS_PANTALLA_COMPLETA_FEED_SCROLL (1)(2).zip')
files=0
with zipfile.ZipFile(original) as z:
    for name in z.namelist():
        if name.startswith('ANDROID_STUDIO/app/') and not name.endswith('/'):
            path=b/name.removeprefix('ANDROID_STUDIO/')
            assert path.is_file() and path.read_bytes()==z.read(name), name
            files+=1
assert files>=75
print(f'PAS_09_OK: idioma ca/es/en, 3 claus feed, cua protegida, contracte PHP, Android intacte ({files} fitxers)')
