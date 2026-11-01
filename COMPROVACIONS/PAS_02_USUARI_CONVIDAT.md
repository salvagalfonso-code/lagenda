# L'Agenda iOS — Pas 2/48: creació i recuperació del convidat al servidor

**Base acumulada:** iOS v349 + pas 1. **Android de referència:** v336, immutable.

## Què s'ha implementat

1. `IosGuestSession.kt`: DTO `IosGuestIdentity` (`users.id`, `guest_id`, token de 64 caràcters) segons la resposta real de `guest/create.php` d'Android v336. Només s'admeten credencials emeses per servidor; l'app no genera IDs ni tokens per autenticar convidats.
2. `IosGuestSession`: càrrega primer la identitat existent al Keychain; un `Mutex` evita dues altes simultànies; si no n'hi ha, fa **POST** a `guest/create.php` i valida el resultat abans de persistir-lo. Les fallades de xarxa no esborren les credencials.
3. `IosSessionBridge` + `AgendaKeychain.swift`: entrada pròpia `server_guest_identity`, separada de `api_bearer_token`; persistència `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`, no UserDefaults. L'escriptura retorna resultat real i evita la finestra delete-add.
4. `IosAgendaApi`: `ensureGuestIdentity()` i `guestUserId()`; quan una API requereix autenticació, accepta el Bearer del compte registrat, i si no n'hi ha, el Bearer del convidat ja emmagatzemat. El convidat no es confon amb un compte registrat en `restoreAccount`.
5. `AgendaIosScreen`: si no hi ha sessió registrada, crea o recupera automàticament el convidat; si la xarxa falla, reintenta al cap de 60 s mentre la pantalla existisca. En tancar sessió es recupera l'identificador convidat. Si hi ha token registrat però es perd la xarxa, no es reemplaça amb un convidat.

## Comprovacions

- PASS: `python COMPROVACIONS/comprovar_pas_01.py` (regressió de transport).
- PASS: `python COMPROVACIONS/comprovar_pas_02.py` (contracte, Keychain, petició `guest/create.php`, recuperació, aïllament del token i conservació dels 119 fitxers protegits d'Android/servidor).
- PASS: `swiftc -frontend -parse` de tots els fitxers Swift (només sintaxi).
- Afegits **quatre tests Kotlin** en `IosGuestSessionTest.kt`: acceptació d'identitat, rebuig de credencials falses, idempotència entre sessions i fallada de Keychain. **No executats**: falta distribució Gradle amb dependències i no hi ha Xcode.
- **PENDENT:** compilació Kotlin/Native i framework Swift; prova real de `guest/create.php` amb MariaDB; execució en iPhone i comprovació de persistència real durant reinicis i sense xarxa.

## Fora d'abast fins a passos següents

- Pas 3: `guest/claim.php` en fer login/registre, fusió de dades i gestió de credencials després de l'upgrade.
- Pas 4 i següents: carregar l'estat del convidat des del servidor, permetre interaccions de convidat a totes les pantalles i sincronitzar preferències. Les comprovacions actuals no certifiquen estes funcions.
- No s'ha modificat Android v336, el codi PHP del servidor ni MariaDB.
- No s'ha generat un ZIP; es lliurarà el primer ZIP acumulat després del pas 3.
