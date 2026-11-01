# Pas 4/48 — Lectura de l'estat personal al servidor (sense canvis Android)

**Base acumulativa:** iOS v349 + passos 1, 2, 3 i 4. **Android v336:** immutable. No es crea ZIP en este pas.

## Canvis aplicats (únicament iOS)

1. `IosCentralState.kt`: model RAM de la resposta de `GET api/sync/state.php` idèntica a Android v336: `user_id`, `revision`, mapa COMPLET `values`. Admet valors de SharedPreferences tipats `{t,v}` (`string`, `bool`, `set`) i manté les claus desconegudes en el mapa sense descartar-les.
2. `IosAgendaApi.loadServerState()`: resol l'usuari convidat real o el compte registrat, fixa l'ID i el Bearer abans de les peticions, comprova que la resposta té el mateix `user_id`; cap escriptura, i revalida el compte abans de retornar les dades.
3. Recupera addicionalment en paral·lel **GET** `preferences/locations.php`, `preferences/feed.php` i `user-state/mine.php` per al mateix token. Si falla alguna API específica, no substituïx els valors existents per llistes buides. Els dominis dedicats (`locations`/`feed`) tenen prioritat quan existixen; si no, recupera les claus compartides d'Android (`preferred_locations_guest`, `preferred_locations_<username>`, `show_suggestions`, `valencian_first`, `discovery_level`, etc.).
4. `AgendaIosScreen.kt`: recupera les dades després de restaurar sessió, crear convidat, iniciar sessió, registrar compte, fusionar el convidat i amb actualització manual. Valida identitat abans de projectar preferències en la UI i conserva l'estat cru exclusivament en memòria. Els seguiments, guardats, avisos i afinitats estan disponibles en `userDomain` i es projecten parcialment en les vistes existents. Les pantalles específiques continuen carregant els seus dominis corresponents en `loadAccountPages`.
5. `IosAgendaApi.registerAccount`: **incidència detectada al pas 3 mitigada des del client**: abans de convertir el convidat en compte nou, consulta `user-state/mine.php` amb el token convidat i passa els quatre flags `notification_preferences` existents com a `notifications` a `auth/register.php`. Si no pot llegir els flags, no completa la conversió: preferible error a sobreescriptura silenciosa. El servidor no s'ha tocat.

## Comprovacions

- Proves estàtiques `comprovar_pas_01.py` a `comprovar_pas_04.py`: PASS.
- Kotlin JVM: `IosCentralState.kt` compila amb un doble controlat de kotlinx.serialization i de DiscoveryLevel; 10 comprovacions de lectura, identitat, precedència i tipus: PASS. **No implica compilació completa KMP.**
- Proves de `IosCentralStateTest.kt` escrites per a compilar amb les dependències reals de la versió, **no executades amb Gradle**.
- `swiftc -frontend -parse iosApp/AgendaIOS/*.swift`: PASS (sintaxi Swift, no integració Swift/KMP).
- Arxiu de referència Android v336 i servidor inclòs a iOS: hash SHA-256 igual que els originals, sense modificacions.

## Límit del pas 4

- Este és el **pas de lectura**. No ha eliminat la persistència antiga de preferències en `UserDefaults` que encara utilitzen algunes accions d'iOS; la substitució de les escriptures és el **pas 5** i la cua offline és el **pas 6**. És incorrecte afirmar encara sincronització bidireccional completa.
- No s'ha fet cap consulta real a una sessió de producció, ni compilació Kotlin/Native/Xcode ni prova en iPhone.
- `guest/claim.php` i les preferències de notificacions s'han analitzat estàticament: el GET previ evita l'error de valors per defecte en el flux normal, però **no prova transaccions reals ni protegeix davant de canvis simultanis al servidor**. La solució definitiva és preservar els valors a PHP dins de la transacció; es deixa a revisió, sense modificar servidor en este pas.
- Algunes claus d'Android no corresponen a controls d'iOS existents i encara no es projecten en pantalla; es conserven en `values` de la instantània. Altres dominis (entrades, llistes, notis, Batecs) continuen depenent d'API pròpies.

**Següent:** pas 5: enviar i guardar les modificacions personals al servidor sense conservar UserDefaults com a font persistent.
