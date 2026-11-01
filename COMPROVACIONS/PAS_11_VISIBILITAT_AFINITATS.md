# Pas 11 — Visibilitat i afinitats iOS, compatibles amb Android v336

## Implantat en codi
- **Font autoritativa d'exclusions:** `GET/POST visibility/index.php` (`agenda_visibility_exclusions`) amb `EVENT`, `ARTIST`, `CATEGORY`, i codis de subcategories. Ja no s'escriu ni es consulta com a font principal `ios_hidden_events_v1`, `ios_hidden_artists_v1` ni `ios_hidden_categories_v1`.
- **Diferencial segur:** altes i baixes per entitat, sense sobreescriure el conjunt remot complet. Cada acció s'enfila xifrada i es reté fins al `ok` del POST **i** una comprovació GET de l'efecte. GET amb camp absent es tracta com a error, no com a llista buida.
- **Dades en trànsit:** un GET no pot anul·lar accions pròpies pendents de la cua ni els canvis de pantalla iniciats després de la lectura. Els convidats i comptes registrats tenen identificadors i tokens separats.
- **Afinitats:** `user-state/mine.php` retornant `affinities` (inclou `effective_score`); en iOS les ponderacions de descoberta ixen d'eixe valor i no d'`ios_recent_styles_v1`. Cerques i obertures d'activitats a través d'`affinities/record.php` amb `SEARCH_FREE`/`OPEN_EVENT` i clau `dedupe_key` estable per acció xifrada (evita duplicats en reintents).
- **Visibilitat editorial:** mateix criteri d'Android de no ocultar un festival amb dos artistes si només un està bloquejat, i la publicitat obligatòria conserva visibilitat. Codis principals i subcategories del catàleg Android.
- **Coherència dels seguiments:** quan es bloqueja explícitament un artista/categoria des de les opcions de feed, es desfan també seguiments conflictius (cua per usuari); en el formulari de personalització no es resegueixen categories ocultades.
- **Cua offline:** cap token dins del cos xifrat. Una resposta fallida o de l'altre compte no elimina accions pendents.

## Límits comprovats
- Les antigues claus `ios_hidden_*` del servidor es deixen com a dades històriques però **no es fusionen automàticament** sobre les exclusions actuals d'Android (evita ressuscitar elements ja reactivats en Android). Si hi ha dades històriques exclusives d'iOS, cal migració amb comprovació explícita d'usuari.
- El pas 11 no completa les alertes d'artistes ja configurades ni totes les classes d'events implícits de l'algoritme Android; calen les fases de campanetes i «Per a tu» posterior. El PHP actual tampoc té cap endpoint de transacció multi-domini entre seguiments, alertes i exclusions.
- La selecció d'afinitats al feed continua amb algoritme iOS; el pas 12 és la paritat de «Per a tu».
- **No verificat:** API en producció, efectes en MariaDB, compilació Kotlin/Native / Xcode, i proves simultànies Android ↔ iPhone.

## Proves fetes
- Comprovacions estàtiques dels passos 1–11; sintaxi Swift i hash Android originals.
- Compilació Kotlin/JVM dels models reals de visibilitat/afinitats i política de festivals amb dependències simulades (no compilació integral de l'app).
- Tests `commonTest` escrits però no executats amb Gradle.
- **No generar ZIP en este pas:** lliurament següent després del pas 12.
