# PAS 13/48 — «Sense filtre» — iOS v349 (Android v336 immutable)

**Estat:** implementat en codi, proves locals disponibles superades. **NO compilat ni verificat en iPhone** i **NO provat contra MariaDB real**.

## Què s'ha modificat (només iOS)

- `shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosUnfilteredAgenda.kt` (nou): llista cronològica universal per jornada, sense puntuació ni personalització; projeta únicament dates explícites del programa i mai genera dies imaginaris dins d'un rang inici-fi; un sol cartell que evoluciona per festivals consecutius; múltiples jornades per cicles; excepció d'exposicions llargues amb aparició setmanal o de jornada propera segons Android; descart de passats; ordenació per data efectiva, hora i ID; control prudent de duplicats entre fonts per nom/town/data/venue/intèrprets, sense fusionar IDs de campanyes o venda interna.
- `EventCatalog.kt`: conserva la data original com a dada canònica, distingix `feed_instance_date` / `selected_date` amb `feedOccurrenceDate`, i utilitza la data real de la jornada per a la clau de targeta; no canvia l'ID original per a guardats, entrades o popups. Evita classificar exposicions com a festivals només per tindre un interval llarg.
- `AgendaIosScreen.kt`: integra la selecció universal en «Sense filtre», manté l'ordre i la data de cada jornada, mostra el cartell complet abans d'un festival consecutiu i el resum del dia una vegada començat; conserva les exclusions de `visibility/index.php`, les excepcions publicitàries, les entrades internes i el sistema de càrrega 5 + 15 + 10. El dia actual es torna a consultar en recompondre la pantalla.
- `IosUnfilteredAgendaTest.kt` (nou): tests de casos de festivals, cicles, exposicions, duplicats, dates i publicitat.
- `COMPROVACIONS/comprovar_pas_13.py` (nou): prova d'integració estàtica, contractes i hashes d'Android immutable.

## Comprovacions fetes

- Scripts d'integració estàtica 01–13: **PASS**.
- Kotlin/JVM compilant la política real `IosUnfilteredAgenda.kt` amb un model simulat equivalent: **19 escenaris PASS** (incloent dates, sessions, festivitats, exposicions, ordre cronològic, deduplicació i preservació comercial).
- Anàlisi sintàctica Swift: **PASS**.
- Fitxers Android v336 original i Android empaquetat dins de la v349: **no modificats**, comprovació contra els ZIP originals.
- Intent de compilació KMP/iOS: **NO COMPLETAT**. Gradle wrapper requereix descàrrega de `services.gradle.org`, DNS inacessible. No hi ha Xcode iOS disponible en este entorn.
- No executats els tests commonTest de Gradle ni proves realistes del feed en iPhone/API.

## Diferències que NO es poden donar per certificades

1. Android v336 té `AgendaDedupe` amb comparacions difuses de títols i artistes. El pas 13 només incorpora la deduplicació conservadora amb identitats exactes: no s'ha traslladat tot l'algoritme de semblança.
2. La font `events/index.php` del ZIP de servidor pot excloure des de PHP un esdeveniment amb `end_date` passat encara que tinga un nou calendari futur. iOS no pot recuperar dades que el servidor no envia; no s'ha modificat el servidor.
3. La cache inicial d'iOS continua sent JSON; Android usa SQLite com a primera vista. El rendiment i la posició de lectura es tracten en el pas 14.
4. Les excepcions de visibilitat comercial del backend no s'han validat amb campanyes reals; els controls de codi existents es preserven.

**Pròxim pas:** 14/48, càrrega progressiva, cache i conservació de posició. **Pròxim ZIP acumulatiu:** després del pas 15 (passos 1–15).
