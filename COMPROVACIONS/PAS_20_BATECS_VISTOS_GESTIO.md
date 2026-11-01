# PAS 20/48 — Batecs: vistos, publicació, edició i eliminació

## Estat i abast

Implementat **en codi** sobre iOS v349 acumulat fins al pas 19. **Android v336 i servidor PHP intactes**. No s'ha creat ZIP: el lliurament acumulatiu correspon al pas 21.

## Contracte Android v336 i PHP reutilitzat

- `batecs/seen.php` (GET/POST) — marca el vist d'un usuari, convidat o registrat, amb clau canònica `manual:ID` / `event:ID`. PHP fa UPSERT i guarda les claus set dies.
- `batecs/index.php?event_id=...&offset=...&limit=50` — llista paginada manual activa de 48 h. El camp `can_edit` del PHP és l'única autoritat sobre l'edició; no inferim propietat a partir del nom d'artista.
- `batecs/eligibility.php?event_id=...` — comprova si es pot publicar. Els organitzadors autoritzats poden publicar; la resta només d'1 h abans de l'actuació fins a 24 h després. **El PHP és qui decidix**.
- `batecs/create.php`, `batecs/update.php`, `batecs/delete.php` — només amb sessió registrada, com Android v336. Creació i edició validen títol (1–120) i cos (fins a 1000). L'edició i esborrament exigixen ser autor o superadministrador i el servidor retorna 403 si no hi ha permís.

## Canvis iOS

1. `IosBatecOperations.kt` nou: validació d'identificadors, elegibilitat, títol/cos, permisos i receipts de vistos/creació/edició/esborrament.
2. `IosPendingServerQueue.kt`: suport `post` a `batecs/seen.php` i consulta de pendents per ID d'usuari; les accions vistes són idempotents en PHP i es poden reintentar després d'un tall de xarxa.
3. `IosAgendaApi.kt`: vistos obtinguts del servidor amb overlay de cua xifrada del mateix usuari. Enviament a cua abans del POST; POST amb `ok`, `story_key` i `seen`, més GET posterior de vistos abans d'esborrar la cua. Consulta paginada del Batec de cada esdeveniment i operacions de gestió amb ID/token registrat congelats; confirma `id`, `event_id`, `can_edit`, títol i cos. Després d'eliminar, rellegix la llista per comprovar absència de l'ID.
4. `AgendaIosScreen.kt`: permet marcar vistos tant com a convidat com registrat; si la connexió falla, la representació en RAM els mostra vistos mentre la cua xifrada continua pendent; no s'afirma que estiguen confirmats al servidor. En el popup de l'esdeveniment, la secció Info inclou «Batecs de l'esdeveniment · Publicar i gestionar»; el diàleg mostra publicacions, comprovació d'elegibilitat, creació, edició i eliminació confirmada. Als convidats se'ls oferix iniciar sessió per publicar, sense crear comptes paral·lels. Les noves publicacions invaliden la projecció de cercles i es tornen a llegir.

## Precaucions

- **No** s'encuen creacions (`create.php` no té clau d'idempotència): evitar duplicats per reintents de connexió o doble publicació. Igualment l'edició/esborrament són operacions en línia; la resposta del servidor és necessària.
- `event:ID:artista` es transforma en `event:ID`, exigència de PHP. Això significa que un mateix esdeveniment compartit per diversos artistes seguits quedarà vist per tots; no és possible distingir-los sense modificar el servidor i Android.
- Un Batec ja aplicat en el servidor però amb resposta HTTP perduda pot requerir refrescar la llista per mostrar-lo. El client no ha d'afirmar que ha publicat sense receipt.
- Un `GET` de Batecs en un compte pot tornar resposta després d'un canvi de sessió; se'n comprova el `user_id` abans d'aplicar-la.
- Publicacions manuals només vives les últimes 48 h segons PHP; l'API pot ocultar publicacions caducades.

## Proves executades

- Scripts estàtics **1–20 PASS**, inclosa la comprovació byte a byte d'Android v336 i dels fitxers Android incrustats en iOS v349.
- Kotlin/JVM aïllat: **34 comprovacions PASS** del contracte real `IosBatecOperations.kt` amb JSON simulat.
- Kotlin/JVM de **la cua real** amb coroutines i JSON fals: **PASS**, offline, reintent, verificació de propietari i confirmació abans de retirar.
- `swiftc -frontend -parse` del codi natiu Swift: **PASS**.
- `IosBatecOperationsTest.kt` creat per a commonTest; **no executat amb Gradle** per falta de dependències descarregables.
- **NO provat**: compilació integral Kotlin/Native/SwiftUI en Xcode, trucades API reals, MariaDB, sincronització iPhone↔Android.

## Següent

Pas 21 — Completar gestos i acabat visual dels Batecs; després generar ZIP acumulatiu 1–21.
