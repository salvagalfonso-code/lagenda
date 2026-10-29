# L'Agenda iOS v350 — Batecs (treball acumulat, sense ZIP)

## Diagnòstic
- La versió iOS anterior construïa el carrusel exclusivament amb `batecs/index.php`, exigint sis artistes diferents amb una publicació manual. La base SQL del 29/10/2026 només conté dos Batecs manuals; Android, en canvi, forma Batecs també amb esdeveniments futurs i marcadors en gris.
- iOS descartava `event:<id>:<artist>` en llegir i publicar l'estat de vistos; l'API Android utilitza precisament eixe identificador.
- iOS podia mostrar més de sis cercles, o cap, en comptes de seguir la regla Android de set artistes seguits com a llindar i sis cercles visibles.

## Correccions implementades en el codi iOS
1. `IosBatecsFeed`: fusió de publicacions manuals reals amb novetats automàtiques d'esdeveniments futurs dels artistes seguits, deduplicació, suport al cartell de festival i marcadors explícits en gris sense novetats.
2. `IosBatecArtistGroups`: sis cercles exactes quan es seguixen set o més artistes, prioritat de novetats recents sense vore, publicacions antigues o ja vistes en gris.
3. `IosBatecTimelinePolicy` i `IosBatecTime`: validació dels identificadors de vistos, normalització dels artistes i expiració de 48 hores com Android, sense inventar timestamps.
4. `IosAgendaApi`: descodificació de `published_at`, consulta opcional de `batecs/recent-events.php`, GET/POST `batecs/seen.php` amb les claus automàtiques completes.
5. `AgendaIosScreen`: integra el carrusel derivat, evita marcar marcadors en gris com a vistos, restaura estat en entrar en sessió, actualitza cada dos minuts quan està en Inici i torna a l'inici del feed després de tancar el visor.
6. `EventCatalog`: inclou `created_at` si està disponible i recupera l'artista principal si no hi ha una llista `artists`.

## Proves
- `tests/BatecsParitySmoke.kt`: 17 casos PASS amb compilació JVM de les polítiques i compositor de Batecs.
- Tests d'integració multiplataforma `shared/src/commonTest/.../IosBatecsFeedTest.kt`: escrits; NO executats amb Gradle/Kotlin-Native.
- SHA256 d'Android contra els 93 fitxers originals: 93/93 iguals.
- NO verificat: compilació completa Xcode, execució en iPhone, resposta actual dels endpoints remots ni marcat de vistos en el servidor de producció.

## Restriccions
- Sense canviar la numeració v350.
- Sense generar ni lliurar un ZIP.
