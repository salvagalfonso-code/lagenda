# L'Agenda · v350 · Per a tu calculat en un únic backend

## Situació detectada

Android v350 calcula `personalizedForYou()` en `AgendaState.kt`, escriu els IDs en una instantània local i sincronitza la còpia amb `api/for-you/index.php`. L'endpoint antic NO és el motor de recomanacions. iOS v350 usa `IosForYouSnapshot.select()` / `IosPersonalFeedPolicy.select()` de forma local.

## Canvi real en el codi de treball

S'ha afegit `SERVIDOR_PER_A_TU/api/for-you/feed.php` + `_engine.php`, amb dades de taxonomia (55 subcategories) i geografia (6016 localitats) extretes de les mateixes fonts del projecte Android. Es consulta el mateix catàleg `events` que `api/events/index.php`, les relacions d'artistes i organitzadors, `user_follows` per al compte autenticat, i les afinitats recents del compte. El servidor selecciona i retorna IDs. També s'ha afegit caché de 30 minuts per compte (`sql/01_for_you_server_cache.sql`) i control de revisió del catàleg per invalidar-la.

Ambos clients contacten **la mateixa ruta** (`for-you/feed.php`) amb les preferències explícites, i mostren els IDs del servidor quan és disponible; en cas de fallada de xarxa o backend no instal·lat mantenen el càlcul local com a recuperació. La caché antiga `for-you/index.php` no s'elimina perquè és usada per versions Android ja publicades.

**iOS:** `shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/{IosAgendaApi.kt,AgendaIosScreen.kt}`.

**Android:** `app/src/main/java/com/example/agendavalenciana/{data/ServerApiRepository.kt,state/AgendaState.kt}`.

## Regles cobertes per les proves PHP

- Categories i subcategories explícites.
- Radi fix de 100 km per a preferències culturals amb municipis seleccionats.
- Artistes seguits amb concerts en qualsevol ubicació.
- Exclusió de reserves gratuïtes internes.
- Mateixes quotes numèriques de descoberta que `ForYouHundredPolicy`.
- Infantil transversal.
- Evitar radis no admesos.

## Limitacions que impedixen donar la tasca per completada

1. **No desplegat:** encara no s'ha copiat `SERVIDOR_PER_A_TU/api/for-you/` al directori `app/api/for-you/` del servidor ni executat l'SQL. No s'ha accedit a MariaDB de producció.
2. **No compilat** ni testat en un simulador iPhone o en Android físic.
3. **Paritat funcional encara no completa:** els estils inferits d'entitats seguides i els senyals de descoberta del servidor no estan contrastats amb tots els casos de l'Android actual; les exclusions de visibilitat continuen aplicant-se al client, després de rebre els IDs, i hi ha regles de comercialització que s'han de reconciliar amb la implementació Android.
4. Per als comptes autenticats, les categories són autoritatives a `user_follows`. Les ubicacions i els radis encara viatgen en la petició del client: cal provar la recuperació efectiva de `preferences/locations.php` en els dos sistemes abans d'afirmar paritat entre dispositius.
5. L'API PHP de producció pot tindre variacions de l'esquema respecte del snapshot `_backend_v1`. Cal comprovar l'esquema real i els camps `end_date`, `ticket_enabled`, `ticket_price`, etc.
6. La caché d'Android pot continuar mostrant temporalment una instantània local fins que arribe el resultat nou, com a mecanisme de continuïtat.

## Ordre segur de desplegament

1. Fer una còpia de seguretat de la base de dades i de `/app/api/for-you/`.
2. Verificar compatibilitat de l'esquema i instal·lar `sql/01_for_you_server_cache.sql`.
3. Copiar la ruta `api/for-you/` al servidor existent, sense substituir `index.php`, l'autenticació ni la resta de rutes.
4. Provar peticions amb compte autenticat i convidat; revisar `calculated_on_server=true`, `source=server-v1` i els IDs amb fixtures de dades reals.
5. Fer tests de paritat de la mateixa sessió Android+iOS: seguits, canvis de categoria, ubicacions, 2 mesos, suggerències ON/OFF, exclòs, canvi de compte, sense xarxa, 30 min, festival multidia i campanyes.
6. Només després compilar i publicar els clients. Fins aleshores no qualificar la migració de completa.

## Verificació local

`php -l` en els dos PHP: correcte. `php tests/test_engine.php`: 8 casos correctes. Encara no hi ha test integrat amb MariaDB ni compilació Kotlin/Native.
