# PAS 19/48 — Batecs: només artistes seguits i sis cercles d'Android v336

## Estat

Implementat **en codi** sobre la còpia acumulativa v349 dels passos 1–18, sense tocar l'Android v336 ni el PHP. **No compilat ni provat en iPhone/MariaDB real**. Cap ZIP fins al pas 21.

## Regla exacta contrastada amb Android v336

`AgendaState.shouldDisplayBatecs()` exigeix **més de sis artistes seguits vàlids (7+)** i `BatecVisibilityPolicy.selectSixArtists` selecciona **exactament sis cercles**. Un cercle és un artista, encara que tinga diverses publicacions. Es prioritzen novetats sense vore, després contingut recent, després els artistes amb una actuació futura i finalment placeholders grisos dels seguits sense novetats. Artistes bloquejats no compten, ni apareixen. No mostrar concerts passats.

## Canvis

- Nova `IosBatecFeedContract.kt`: projecta en RAM les publicacions de `batecs/index.php`, les novetats reals de `batecs/recent-events.php`, les actuacions futures del catàleg `events/index.php` i, si no hi ha contingut, els cercles grisos dels artistes seguits. Els `event_id` i `story_key` originals es conserven.
- `IosAgendaApi.kt`: lectura autenticada com a convidat o registrat, recorregut dels Batecs manuals, recuperació de novetats, identitat congelada abans/després de la consulta. No usa UserDefaults ni crea un segon servei. El PHP no sempre retorna `event_artist`: la correspondència es comprova amb el programa real de l'esdeveniment, o amb `author_name` si coincideix amb un artista seguit.
- `IosBatecNames.kt`: normalització de topònims/apòstrofs/accents habituals als noms d'artista, compatible amb la política d'Android.
- `IosBatecArtistGroups.kt`: llindar real de set seguits, exactament sis cercles, un per artista, rànquing de no vistos i clau de vistos compatible amb PHP; placeholders grisos.
- `AgendaIosScreen.kt`: consulta de Batecs reactiva al catàleg, compte, seguiments i exclusions; sense reusar dades d'un altre usuari; etiquetes de «Sense novetats» per als placeholders. Les claus `event:ID:artista` es projecten com `event:ID` en enviar el vist a `batecs/seen.php`, tal com exigix el PHP.
- Proves: `IosBatecStep19Test.kt` (commonTest, escrita), `provar_pas_19.kt` (JVM real de les polítiques de producció amb dobles de les classes d'entrada) i `comprovar_pas_19.py` (invariants de codi/PHP/Android intacte).

## Comprovacions

- 19 scripts de regressió estàtica: **PASS**.
- Compilació Kotlin/JVM aïllada del codi nou i 22 assertions: **PASS**.
- `swiftc -frontend -parse` dels quatre fitxers Swift: **PASS**.
- Integritat de 78 `.kt` d'Android v336 i 93 fitxers Android originals embeguts al projecte iOS v349: **PASS**, no modificats.
- Gradle KMP/Xcode i proves reals API/MariaDB/iPhone: **NO EXECUTATS**. `gradlew` intenta descarregar Gradle però `services.gradle.org` no resol.

## Límits i incidències

1. Android utilitza el llindar de 7 seguits per a mostrar 6 cercles. S'ha respectat la **regla del codi real** d'Android, no la interpretació literal «sis seguits = sis cercles».
2. En el servidor aportat, `batecs/index.php` exposa només publicacions manuals; les novetats automàtiques es deriven del feed real de publicacions dels esdeveniments, com Android. No es generen falses dates de publicació.
3. `batecs/seen.php` només admet `manual:ID` o `event:ID`. No permet identificar individualment `event:ID:artista`, per tant un mateix esdeveniment compartit per dos artistes té vist a nivell d'esdeveniment. La sincronització dels vistos i gestos complets es revisarà en el pas 20/21.
4. L'ordenació de «última actuació» usa `updatedAt` quan el catàleg iOS no exposa `createdAt`, i no pot reproduir al 100% la tria d'Android sense exposar també el camp `created_at` original. Este límit no justifica modificar Android congelat.
5. La lectura inicial depén que el catàleg continga els artistes del programa. Si un esdeveniment no té ni artistes ni programa en la resposta del servidor, no se li pot atribuir un artista seguit sense especular. El fallback mostrarà el placeholder segur.

## Següent pas

Pas 20: lectura/escriptura de Batecs vistos i operacions de publicació/edició/esborrament en les API, amb la gestió de convidats i errors; després pas 21 gestos. ZIP acumulatiu 1–21 **només** després del pas 21.
