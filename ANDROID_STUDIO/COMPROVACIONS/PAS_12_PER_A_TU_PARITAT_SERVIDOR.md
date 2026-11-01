# L’Agenda iOS · Pas 12/48 — «Per a tu» i caché compartida

## Referència real: Android v336 congelat
- Android `AgendaState.kt` (funció `personalizedForYou`) aplica les quatre rutes de selecció per `ForYouHundredPolicy`: seguiments, estils afins a seguits dins de radi, subcategories o territori preferit (fins a 100 km), i suggerències basades en senyals dels últims 15 dies dins de radi. Sense mínim obligatori de 100.
- Android ordena cronològicament, amb preferència valenciana dins de la data. La firma «dura» `forYouHardPreferenceSignature` és un `Int` del client, i l'Android guarda IDs en `for-you/index.php` (TTL 30 min). No calcula el feed complet en PHP.
- Al servidor hi ha una **altra** ruta, `for-you/feed.php`, amb motor PHP `_engine.php`: selecciona al servidor de manera semblant, fa consulta dels seguiments i afinitats de l'usuari de MariaDB i emmagatzema `for_you_server_cache` amb TTL de 30 min quan la taula està disponible. **No és el mateix algoritme integral d’Android v336** (p. ex. les classes de comportaments i detalls dels esdeveniments no hi són tots).

## Implementat (només iOS)
1. `IosForYouServerSelection.kt`: parse segur de les llistes d'IDs del servidor. Accepta la caché Android `for-you/index.php` només amb `hit` + `fresh` confirmats i **signatura real recuperada de l'estat central**; no inventa ni publica signatures d'Android. Si no està disponible, accepta `for-you/feed.php` únicament amb `ok`, `calculated_on_server` i `source=server-v1` confirmats.
2. `IosAgendaApi.kt`: peticions autenticades amb el Bearer associat a l'ID actual, verificació abans i després de la resposta, i comprovació de cua personal pendent abans de reutilitzar una cache Android. No ha escrit res al servidor nou ni ha modificat Android.
3. `AgendaIosScreen.kt`: recupera preferències i senyals de la sessió, demana els IDs a l'API, i associa exclusivament els IDs confirmats al catàleg públic que iOS coneix. Un canvi de compte cancel·la i invalida la selecció anterior. La cerca i «Sense filtre» eviten calcular el feed personal en segon pla, i l'app no confon un convidat sense preferències amb un usuari que ja ha seguit artistes.
4. Sense connexió o API antiga, usa `IosForYouSnapshot` (cache RAM de 30 min) com a aproximació provisional. El motor local de reserva reutilitza la classe exacta `ForYouHundredPolicy.kt` d'Android per al càlcul de quota i quatre rutes. Els esdeveniments comercials s'incorporen en l'última capa de feed, no es presenten com a gustos orgànics.
5. El PHP existent `for-you/feed.php` consulta la base de dades per a l'usuari autenticat i manté caché pròpia de 30 minuts. iOS no desa dades personals del feed en `UserDefaults`.

## Comprovacions
- Proves estàtiques acumulades 01–12: PASS.
- Kotlin/JVM amb el **fitxer real** `IosForYouServerSelection.kt` i dobles mínims de JSON: 13 casos PASS (cache Android, frescor, resposta validada, ordre, IDs inexistents, identificador, signatura, contracte de preferències).
- Sintaxi Swift: PASS; sintaxi PHP del servidor *inclòs* (sense modificació): PASS.
- Comparació byte a byte amb Android v336 original i amb l'Android empaquetat en el ZIP iOS: PASS.

## Limitacions que cal resoldre i provar
- **No es pot certificar que iOS mostre exactament la mateixa selecció d'Android si no hi ha snapshot Android vàlid:** `for-you/feed.php` té un algoritme PHP propi i no és la mateixa implementació que `AgendaState.kt` d'Android v336. No s'ha canviat el servidor ni Android en este pas. La conversió completa necessita una font de càlcul compartida o desplegar el motor complet d'Android al servidor, sempre amb contracte compatible.
- La signatura Android està consultada només si existeix en `sync/state.php` amb la clau esperada; si Android no la sincronitza, la lectura de `for-you/index.php` es descarta de forma segura.
- La data de la caché Android correspon al dia cultural (canvia a les 05:00 Europe/Madrid); iOS encara usa `currentLocalDateIso()` per a la lectura. En la franja de mitjanit a les 05:00 pot fallar el HIT i activar el càlcul alternatiu, no associar un snapshot incorrecte.
- Cap comprovació en dispositiu real ni cap connexió a MariaDB en producció; el desplegament i la taula cache server-v1 no han sigut verificats en viu. L'algoritme local provisional continua simplificat en metadades de comarca, llengües i comportaments.
- La compensació d'anuncis/entrades es manté en la capa de feed d'iOS, independent de la selecció orgànica.
- La compilació KMP/iOS amb Xcode/Gradle no s'ha pogut executar a l'entorn actual (no hi ha Xcode i no es pot descarregar Gradle).

**Proper pas (13):** «Sense filtre». Propera entrega: pas 15, acumulativa 1–15.
