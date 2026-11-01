#!/usr/bin/env python3
"""Verificació del contracte del pas 2, sense executar endpoints reals."""
from pathlib import Path
import hashlib, zipfile

root = Path(__file__).resolve().parents[1]
source = root.parent.parent
api = (root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosAgendaApi.kt').read_text()
guest = (root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosGuestSession.kt').read_text()
screen = (root/'shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/AgendaIosScreen.kt').read_text()
swift = (root/'iosApp/AgendaIOS/AgendaKeychain.swift').read_text()
tests = (root/'shared/src/commonTest/kotlin/com/example/agendavalenciana/ios/IosGuestSessionTest.kt').read_text()

assert 'loadGuestIdentity(): String?' in api and 'saveGuestIdentity(json: String): Boolean' in api
assert 'IosServerConnection.json("guest/create.php", buildJsonObject {})' in api
assert 'guestSession.current()?.token' in api
assert 'if (authenticated) guestSession.current()?.token else null' in api
assert 'sessionBridge?.loadGuestIdentity()' in api
assert 'sessionBridge?.saveGuestIdentity(data) == true' in api
assert 'if (!api.hasRegisteredSessionToken())' in screen
assert 'api.ensureGuestIdentity()' in screen and 'delay(60_000L)' in screen
assert 'api.restoreAccount()' in screen
assert 'Mutex()' in guest and 'mutex.lock()' in guest and 'mutex.unlock()' in guest
assert 'check(saveSecure(created.persistedJson()))' in guest
assert 'root.bool("ok")' in guest and 'root.text("role") == "GUEST"' in guest
assert 'userId > 0' in guest and 'token.matches(Regex("[a-fA-F0-9]{64}"))' in guest
assert 'let update = SecItemUpdate(guestQuery as CFDictionary' in swift
assert 'return SecItemAdd(attributes as CFDictionary, nil) == errSecSuccess' in swift
assert 'kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly' in swift
assert 'func loadGuestIdentity() -> String?' in swift
assert 'func saveGuestIdentity(json: String) -> Bool' in swift
assert 'UserDefaults.standard.set(json' not in swift and 'UserDefaults.standard.string(forKey: "server_guest_identity")' not in swift
assert 'createsOnlyOnceThenRestoresAcrossSessions' in tests
assert 'failedSecurePersistenceMustNotPretendSuccess' in tests
assert 'rejectsMalformedOrForgedCredentials' in tests

orig_android = source/'LAGENDA_V336_ANDROID_STUDIO_v336_COMPILACIO_CORREGIDA(3).zip'
with zipfile.ZipFile(orig_android) as z:
    members=[n for n in z.namelist() if n.startswith('ANDROID_STUDIO/') and not n.endswith('/')]
    for name in members:
        original=z.read(name)
        current=(source/'ANDROID_V336_REFERENCIA'/name).read_bytes()
        assert hashlib.sha256(original).digest()==hashlib.sha256(current).digest(), f'Android v336 changed: {name}'
print(f'PASS: original Android v336 unmodified ({len(members)} checked files)')

orig_ios = source/'LAGENDA_V349_IOS_PANTALLA_COMPLETA_FEED_SCROLL (1)(2).zip'
with zipfile.ZipFile(orig_ios) as z:
    protected = [n for n in z.namelist() if n.startswith(('ANDROID_STUDIO/app/', 'SERVIDOR_')) and not n.endswith('/')]
    for n in protected:
        assert hashlib.sha256(z.read(n)).digest()==hashlib.sha256((source/'LAGENDA_IOS_TREBALL_ACUMULATIU'/n).read_bytes()).digest(), f'Embedded Android or server changed: {n}'
print(f'PASS: packaged Android/server unmodified ({len(protected)} checked files)')
print('PASS: server-issued guest, Keychain storage, API bearer, launch recovery, 60s retry and tests present')
