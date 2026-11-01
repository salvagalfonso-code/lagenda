# L'Agenda iOS — Pas 1/48: unificació de les connexions API

Base de treball: iOS v349. Referència funcional INALTERABLE: Android v336.

## Implementació en codi
- `IosServerConnection.kt` centralitza URL pública (`https://quimerafest.com/app/api/`), GET i POST JSON, Bearer opcional/obligatori, `Accept: application/json`, `Cache-Control: no-cache`, resposta `ok`, errors HTTP i temps d'espera (connexió 12s, socket 25s, petició 30s).
- `IosAgendaApi.kt` abandona la seua connexió duplicada i utilitza `IosServerConnection.json()`. Conserva el token del compte en Keychain a través de `IosSessionBridge`.
- `EventCatalog.kt` consulta `events/index.php` amb el mateix transport; cache públic només per a visualització temporal.
- `AgendaKeychain.swift` centralitza les dos peticions natives (APNs i validació de QR) en `AgendaNativeApi.post`, amb capçaleres i URL compatibles.
- Tests: `IosServerConnectionContractTest.kt`; comprovació estàtica: `COMPROVACIONS/comprovar_pas_01.py`.

## Verificacions executades
- PASS: script d'invariants HTTP, rutes, Bearer, Keychain i capçaleres.
- PASS: `swiftc -frontend -parse` sobre `AgendaKeychain.swift` (sintaxi, no enllaç Apple).
- PASS: 119 fitxers d'Android/servidor empaquetats en iOS v349, idèntics a l'original.
- PASS: Android v336 original, cap fitxer canviat.
- PENDENT: compilar Kotlin/iOS amb Gradle i Xcode/Codemagic: no hi ha la distribució Gradle descarregada i el contenidor no pot accedir a `services.gradle.org`; no hi ha Xcode/iPhone al contenidor.

## Límits i següents passos
- No s'ha creat cap endpoint ni s'ha modificat servidor, Android, les taules SQL o el flux d'usuaris.
- Això **no** significa que iOS sincronitze ja els convidats ni les preferències: passos 2-12, segons el pla.
- No s'ha creat ZIP. Acumular sobre aquesta mateixa carpeta per al pas 2 i ZIP després del 3.
