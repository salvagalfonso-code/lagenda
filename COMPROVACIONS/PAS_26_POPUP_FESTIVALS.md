# Pas 26/48 — Popup de festivals en iOS (v349 acumulativa)

## Fitxers modificats
- `shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosFestivalPopupPolicy.kt` (nou): jornades publicades i futures amb la jornada cultural 05:00, selecció del dia, programa per dia, «Tots / Els meus», claus modernes/antigues, escenaris reals, entrades per data i mescla prudent de programa remot amb l'índex.
- `shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosFestivalScheduleDecoder.kt`: llig `group_name`, `item_type`, `sort_order`, `end_time` i `details` de `special-events/public.php` igual que les dades utilitzades per Android.
- `shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/EventCatalog.kt`: conserva metadades noves de cada actuació sense trencar el constructor antic.
- `shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/AgendaIosScreen.kt`: popup de festival amb pestanyes Horaris, Info, Entrades, Ubicació, Comunitat; filtre «Els meus» per jornada, guardats individuals al servidor via funció existent, escenaris i localitzacions distintes amb confirmació Mapes, dates d'entrades publicades, artistes seguibles, reintents de programa i scroll de la jornada reiniciat.
- `shared/src/commonTest/kotlin/com/example/agendavalenciana/ios/IosFestivalPopupPolicyTest.kt`: proves del contracte.
- `COMPROVACIONS/comprovar_pas_26.py` + ajustos de compatibilitat a les comprovacions 22 i 25 perquè reconeguen les noves implementacions funcionalment equivalents.

## Persistència / identitat
- La selecció d'actuacions usa la funció del pas 18, `sync/state.php`, cua xifrada per user_id i overlay de pendents; no afegim persistència local permanent.
- Les dades d'horaris i escenaris procedeixen exclusivament de `special-events/public.php` i/o del programa ja publicat en `events/index.php`. No inferim sessions de l'interval inici-fi.
- Entrades: API `tickets/options.php` i callback de compra del pas 25, sense implementar cobraments nous (pas 39+).
- Comunitat: mateixa consulta `community/groups.php` i accions del pas 25, amb identitat convidada del servidor.
- L'API i MariaDB no s'han modificat.

## Comprovacions executades
- `comprovar_pas_01.py` a `comprovar_pas_26.py`: **26/26 PASS**.
- Kotlin/JVM aïllat: `IosFestivalPopupPolicy.kt` de producció + models d'entrada simulats i clau equivalent: **16 assercions PASS**.
- Sintaxi Swift: `swiftc -frontend -parse` **PASS**.
- Integritat original Android v336 i subarbre Android incrustat v349 verificada amb SHA-256.
- Es va intentar compilar amb Gradle: **NO EXECUTABLE** per dependència externa `services.gradle.org` inaccessible; tampoc disposem d'Xcode ni hem provat en un iPhone / MariaDB real. El test amb `kotlinc` sobre la UI sense dependències no detecta errors de parser, però NO és una compilació vàlida (referències Compose no resoltes).

## Límits pendents
1. Android v336 guarda les actuacions seleccionades dels festivals en `SharedPreferences` locals. Amb Android congelat, iOS les pot guardar al servidor però **la sincronització automàtica Android ↔ iOS d'estes seleccions individuals no està garantida**. No s'ha maquillat esta limitació.
2. El model iOS només tracta part de les dades de serveis/activitats extra del festival. El popup usa grups/escenaris i sessions, però no clona totes les fitxes i metadades avançades del `FestivalPopup.kt` Android (p. ex. serveis i productes especials), que es revisaran en passos posteriors.
3. La compra i la disponibilitat en viu depenen de `tickets/options.php` i el flux de compra posterior: sense prova real, no certifiquem cobrament.
4. La paritat visual/tàctil real i l'ús de 05:00 en dispositiu necessiten compilació i prova iPhone.

**No es genera ZIP per al pas 26. El següent ZIP és el 1–27.**
