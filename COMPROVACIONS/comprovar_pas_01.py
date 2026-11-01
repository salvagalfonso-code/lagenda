#!/usr/bin/env python3
"""Contracte estàtic pas 1; no requerix internet, Gradle ni dispositiu Apple."""
from pathlib import Path
import hashlib
import re
import zipfile
import sys

project = Path(__file__).resolve().parents[1]
sources = {
    "connection": project / "shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosServerConnection.kt",
    "api": project / "shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosAgendaApi.kt",
    "catalog": project / "shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/EventCatalog.kt",
    "swift": project / "iosApp/AgendaIOS/AgendaKeychain.swift",
}
s = {k: p.read_text() for k, p in sources.items()}
assert 'const val SITE_BASE = "https://quimerafest.com"' in s['connection']
assert 'const val APP_BASE = "$SITE_BASE/app/"' in s['connection']
assert 'const val API_BASE = "${APP_BASE}api/"' in s['connection']
assert 'static let baseURL = "https://quimerafest.com/app/api/"' in s['swift']
assert 'private suspend fun request(' in s['api'] and 'IosServerConnection.json(path, data,' in s['api'] and 'authenticated)' in s['api']
assert 'IosServerConnection.publicJsonText("events/index.php")' in s['catalog']
assert 'AgendaNativeApi.post("notifications/apns-device.php"' in s['swift']
assert 'AgendaNativeApi.post("tickets/validate.php"' in s['swift']
assert 'header(HttpHeaders.Accept, "application/json")' in s['connection']
assert 'header(HttpHeaders.CacheControl, "no-cache")' in s['connection']
assert 'header(HttpHeaders.Authorization, "Bearer $bearer")' in s['connection']
assert 'connectTimeoutMillis = 12_000' in s['connection']
assert 'socketTimeoutMillis = 25_000' in s['connection']
assert 'if (status !in 200..299)' in s['connection']
assert 'sessionBridge?.loadToken()' in s['api']
assert 'bridge.saveToken(bearer)' in s['api'] and 'bridge.loadToken() == bearer' in s['api']
assert 'sessionBridge?.clearToken()' in s['api']
assert 'import io.ktor.http.encodeURLParameter' in s['api']
assert 'https://quimerafest.com/app/api/' not in s['api'] + s['catalog']

# Android/servidor inclosos dins del projecte de treball, sense modificacions.
source_dir = project.parent.parent
original_ios = next(source_dir.glob("LAGENDA_V349*zip"), None)
if original_ios is None:
    original_ios = next(source_dir.glob("LAGENDA_V349*zip"), None)
if original_ios is not None:
    with zipfile.ZipFile(original_ios) as z:
        protected = [n for n in z.namelist() if n.startswith(('ANDROID_STUDIO/app/', 'SERVIDOR_')) and not n.endswith('/')]
        different = [n for n in protected if hashlib.sha256(z.read(n)).digest() != hashlib.sha256((source_dir / 'LAGENDA_IOS_TREBALL_ACUMULATIU' / n).read_bytes()).digest()]
        assert not different, f'Android/servidor modificat: {different[:4]}'
        print(f'Android i servidor dins de iOS v349: {len(protected)} fitxers idèntics')
print('PASS: API, Bearer, cabçaleres, Keychain, HTTP errors i rutes natives')
