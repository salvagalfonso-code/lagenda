# L’Agenda iOS — Pas 24/48 · Filtres avançats i cerques guardades

## Canvis aplicats sobre la versió acumulada 1–23

- `IosSearchPresetContract.kt`: contracte del PHP `preferences/search-filters.php` i d’Android v336, amb categories per **codi** `MUSIC`, `THEATRE`, etc., `dateFrom`, `dateTo`, `month`, `town`, `comarca`, `maxDistanceKm`, `maxPrice`, `freeOnly`. Cap clau d’iOS fora de la llista blanca. Validació real de dates, mes, quantitats, preu decimal i categoria. Mai desar en UserDefaults.
- `IosAdvancedSearch.kt`: aplicació dels camps del filtre complet (diverses categories, mes, distància, preu i gratuïtat), amb intersecció amb la cerca confirmada, sense ampliar resultats. Radi heretat d’Android sense poble: fa servir la ubicació preferida de l’usuari; sense ubicacions conegudes no inventa resultats.
- `IosAgendaApi.kt`: llig, guarda i elimina cerques en la mateixa taula de servidor per convidats i registrats amb identitat/token congelats, POST i comprovació amb GET canònic, `Mutex` per serialitzar escriptures i bloqueig davant canvi d’usuari. No confirma un guardat pel sol HTTP 200 ni en cas d’error de lectura posterior.
- `AgendaIosScreen.kt`: diàleg de filtres (no redibuix inline del feed), selecció múltiple de categories, pobles/comarques, radi, dates, mes, preu i gratuïtat. Botons «Aplicar» i «Guardar» en la mateixa línia, nom de cerca, llistes del compte i «Eliminar», recuperació en obrir o canviar de sessió, protecció contra GET antics, restauració de tots els camps i aplicació de la cerca guardada.
- Cap escriptura permanent de configuració de filtre al dispositiu; les dades només es presenten en memòria mentre s’editen.

## Proves

- 24 comprovacions estàtiques: passades, inclosa regressió dels passos 1–23.
- Codi real de `IosSearchPresetContract`, `IosAdvancedSearch`, `IosGeoPlaces` i `IosCategoryTaxonomy` compilat en Kotlin/JVM aïllat amb implementacions simulades únicament de `kotlinx.serialization.json` i dels models: 26 escenaris PASS. No equival a una compilació de l’aplicació KMP.
- Tests `commonTest` afegits, però sense poder-los executar per Gradle.
- Swift de la capa nativa analitzat correctament amb `swiftc -frontend -parse`.
- Android v336 i Android incrustat en iOS immutables, comparats per SHA-256 amb els ZIP originals.
- ZIP complet acumulatiu 1–24 verificat pel test de la llibreria ZIP, noms i existència dels arxius nous.

## Límits i advertiments

1. **No hi ha compilació KMP/Native ni en iPhone**: Gradle no està disponible en este entorn amb les dependències requerides. Sense proves de servidor o MariaDB reals, no es certifica la sincronització de punta a punta.
2. **Estil/subcategoria de text**: `preferences/search-filters.php` NO admet el camp `style`. iOS el pot aplicar com a filtre temporal, però impedix guardar la cerca amb estil per no perdre'n silenciosament la informació. No s'ha canviat PHP ni Android.
3. **Convidats:** l’API admet Bearer convidat i iOS l’aprofita; el diàleg de filtres actual d’Android v336 mostra una petició d’inici de sessió per a guardar amb compte convidat. Esta diferència de la UI d’Android queda pendent mentre Android estiga congelat.
4. **Preus no coneguts:** l’aplicació estricta de gratuïtat o import màxim exclou activitats sense import publicat. No es presumix gratuïtat.
5. **Distància:** Android accepta radis basats en ubicacions preferides de l’usuari, fins a 2.000 km. iOS respecta el valor d’un filtre guardat i utilitza les ubicacions preferides disponibles per a calcular, sense crear referències fictícies.
6. **Mutacions sense connexió:** els filtres guardats exigixen resposta i comprovació del servidor. El pas 24 no posa estes operacions en una cua offline perquè el seu contracte és independent de `sync/state.php`; en desconnexió es mostra un error, no un fals guardat.

**Android v336 intacte. Servidor intacte. Pròxim pas 25/48: popups d’esdeveniment. Pròxim ZIP al pas 27.**
