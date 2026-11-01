# Pas 5/48 · iOS v349 · Escriptures personals al servidor

**Regla:** Android v336 intacte; servidor PHP i SQL sense tocar. Pas 5 acumulat als passos 1–4. NO ZIP fins al pas 6.

## Implementat en codi

- Nova classe `IosPersonalMutations.kt`: identificadors UUIDv4 de mutació, valors tipats `{t,v}` compatibles amb Android.
- `IosAgendaApi.savePersonalState()`: enviament de mutacions POST a `sync/state.php` amb Bearer convidat o registrat, ID verificat abans i després, serialització amb `Mutex`, confirmació de tots els IDs (`APPLIED` / `ALREADY_APPLIED`) i revisió retornada.
- `IosAgendaApi.saveLocationPreferences()` i `saveFeedPreferences()` amb POST a endpoints dedicats ja existents. No alteren Android ni creen taules alternatives.
- `AgendaIosScreen.kt`: Personalització (categories seguides via `user-state/follow.php`; pobles/comarques/radis via `preferences/locations.php`; suggeriments/descoberta/idioma via `preferences/feed.php` i claus Android en `sync/state.php`); exclusions del feed en estat central; cap persistència personal en UserDefaults. UI es confirma després del servidor.
- Dates de recordatori: `when_<eventId>` com a epoch ms, `title_<id>`, `town_<id>`, `custom_<id>` segons contracte Android; amb `user-state/event-alert.php` i formatador Swift. L'avís programat localment al sistema operatiu és un mecanisme d'entrega, no la font de veritat. En fallada del guardat s'anul·la l'avís local i hi ha compensació best-effort del servidor.
- Horaris de festival: `ios_festival_schedule_<id>` s'escriu en estat central i es llig en popup i Guardats, no en UserDefaults. És una clau de persistència compartida al servidor, però Android v336 **no usa encara aquesta clau**: no equival a paritat d'horaris entre clients.
- Afinitats recents: `ios_recent_styles_v1` s'hidrata del servidor, s'actualitza via API i es reté només en RAM. Cache de `Per a tu` es manté exclusivament en RAM, no UserDefaults.
- El cache públic d'esdeveniments, el catàleg d'entitats, el token tècnic APNs i l'estat transitori de publicitat encara poden utilitzar el cache local: **no són preferències personals**. La gestió de dades offline persistents queda per al pas 6.

## Proves executades

- `comprovar_pas_01.py` ... `comprovar_pas_05.py`: PASS després d'adaptar la prova 04 perquè verifique estrictament la funció de lectura (el pas 05 ara escriu legítimament).
- `swiftc -frontend -parse iosApp/AgendaIOS/*.swift`: PASS.
- `kotlinc` JVM amb dobles mínims de JSON sobre `IosPersonalMutations.kt`: PASS, 10.000 identificadors distints amb format UUIDv4, `bool`, `set` ordenat, `long`.
- Hashes de referència: 119 fitxers Android v336 idèntics; els fitxers Android i servidor empaquetats en el projecte iOS continuen idèntics.

## Pendent de validació i límits

1. No s'ha compilat el framework Kotlin/Native ni provat en Xcode, simulador o iPhone. Les proves de contracte no substitueixen compilació integral.
2. No s'ha fet cap POST a producció ni prova de fusió real de dades MariaDB. No es pot afirmar que la sincronització bidireccional haja funcionat realment encara.
3. Diversos valors que no tenen equivalent específic d'Android (`ios_*_v1`, horaris de festivals) estan en el **mateix** servidor, però Android v336 no els llig. Per tant, són persistència correcta al servidor d'iOS, **no paritat funcional comprovada**.
4. La personalització usa múltiples endpoints independents. Si falla un entre POST successius, el servidor pot quedar parcialment actualitzat. La UI no confirma èxit, però no hi ha transacció multi-endpoint. Recarregar de servidor per reconciliar.
5. Sense connexió, el pas 5 informa de l'error i no conserva permanentment les modificacions no confirmades. La cua temporal xifrada, reintents i idempotència de reentregues són el pas **6**.
6. Els fluxos encara no traslladats (entrades, professional, etc.) continuen en els seus passos posteriors; aquest pas migra les escriptures personals de la UI actual, no la totalitat de funcions futures.
7. La conversió de convidat continua depenent del tractament PHP de notificacions, corregida provisionalment al pas 4 des del client. Pendent prova en una base de dades real.

> **Actualització pas 6:** la cua offline xifrada posterior substituïx la limitació de transport offline descrita en este informe històric. Vegeu `PAS_06_CUA_OFFLINE_XIFRADA.md`.
