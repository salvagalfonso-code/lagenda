# L’Agenda v350 — correcció de sincronització de Personalització en iniciar sessió

## Diagnòstic confirmat al codi (no en producció)

1. `refresh()` llegia `user-state/mine.php` però no actualitzava `chosenCategories` i `chosenSubcategories`: els seguiments del servidor i la pantalla de Personalització podien divergir.
2. `loadAccountPages()` agrupava diverses peticions amb una `async { api.userState() }` que podia fallar i cancel·lar la lectura de la resta de preferències.
3. Quan una edició de categories fallava al servidor, no hi havia cap cua persistent de canvis per a reintentar-la en la pròxima autenticació.
4. `feedPreferences()` i `locationPreferences()` podien fallar silenciosament i deixar valors locals obsolets.

## Implementació en el codi de treball

- Reaplicació dels seguiments confirmats després d'actualitzar el compte.
- Les peticions opcionals són independents de les dades principals de `userState()`.
- Categories modificades en un compte: persistència local + marca `pref_categories_dirty` per compte, reintent dels canvis pendents en login i refresh, lectura confirmada posterior i invalidació del càlcul de «Per a tu».
- Un GET remot no sobreescriu edicions locals no confirmades.
- Els errors de recuperació de categories, llocs i les tres opcions del feed ja no queden silenciosos.

**Fitxer canviat:** `ANDROID_STUDIO/shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/AgendaIosScreen.kt` (en el directori de treball). No s'ha generat un ZIP nou; es manté v350.

## Comprovacions locals

- 14 comprovacions estructurals específiques de sincronització superades.
- 13 proves de contracte d'opcions del feed superades.
- 19 comprovacions estructurals del formulari inicial superades.
- 8 proves PHP de la selecció «Per a tu» superades.
- `php -l` correcte en els dos endpoints del paquet.
- **No s'ha compilat iOS/Android ni s'ha provat un login real contra producció.**

## Desplegament de servidor necessari

La compilació de l'app **no instal·la PHP ni fa migracions SQL al servidor de producció**. Perquè les tres opcions del feed funcionen entre dispositius cal comprovar que:

- `app/api/preferences/feed.php` està instal·lat i respon autenticadament.
- `app/api/preferences/locations.php` existeix i respon per a un usuari autenticat.
- `app/api/for-you/feed.php` i `_engine.php` amb els fitxers `data/` estan instal·lats.
- MariaDB conté `user_for_you_preferences` i `for_you_server_cache`, de les migracions `SERVIDOR_PER_A_TU/sql/`.
- La mateixa sessió retorna les categories des de `user-state/mine.php` (taula `user_follows`).

La disponibilitat d'aquests endpoints en el servidor real no s'ha pogut confirmar en aquesta passada. Si no estan desplegats, la sincronització de les opcions noves no funcionarà, independentment del ZIP iOS.

## Prova d'acceptació pendent

En un compte de prova, triar una disciplina i una subcategoria en Android; iniciar sessió en iOS i comprovar-les, canviar-les en iOS i tornar a Android. Repetir amb pobles/radis, suggerències, descoberta i valencià. Verificar errors HTTP, dades reals de MariaDB i el recàlcul de «Per a tu»; fer el mateix amb dues sessions de comptes diferents i un tall de xarxa.
