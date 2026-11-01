# L'Agenda iOS · Pas 09/48 — Idioma, suggeriments i descoberta

## Referència immutable
- Android v336, `ui/LocalizedText.kt`: `UiLanguageStore.KEY = "selected_language"` i codis `ca`, `es`, `en`. La clau és gestionada per `ServerManagedPreferences`, que sincronitza `sync/state.php`.
- Android v336, `state/AgendaState.kt`: `show_suggestions`, `discovery_level` (`MY_TASTES`, `BALANCED`, `EXPLORE`) i `valencian_first`.
- Servidor existent `api/preferences/feed.php`: GET/POST de **tres** camps `suggestions`, `discovery`, `valencian_first`, emmagatzemats per `user_id` de Bearer; després del POST retorna `exists=true` i els valors.
- El codi Android i el PHP del servidor s'han mantingut intactes.

## Implementat en el codi iOS (còpia acumulativa 1–9)
1. `IosFeedPreferencesContract.kt`: idioma de la interfície independent de la prioritat valenciana; codis idèntics a Android, tres nivells de descoberta i ocultació del selector quan suggeriments OFF.
2. `IosCentralState.kt`: restaura `selected_language` exclusivament del snapshot tipat de `sync/state.php`; manté les tres preferències de feed del domini existent amb recurs a les claus tipades d'Android quan el domini dedicat no existeix.
3. `AgendaIosScreen.kt`: ofereix Valencià/Castellano/English dins de Personalització, recupera preferències per a usuaris convidats i registrats i envia `selected_language` al servidor com `{t:string,v:ca/es/en}`. No usa `UserDefaults` per a guardar-lo.
4. `IosAgendaApi.kt`: reutilitza `preferences/feed.php` per als tres camps del feed i comprova `exists=true` i coincidència exacta de les dades retornades, abans de buidar la cua xifrada.
5. Protecció de lectures obsoletes: abans de projectar estat remot del feed i idioma, iOS comprova la revisió d'edició i qualsevol operació pendent dels tipus `feed` o `state` per al mateix ID d'usuari. Això evita substituir seleccions recents per snapshots anteriors.
6. La desactivació de les suggerències amaga el selector de descoberta, però no canvia el nivell seleccionat, preservant-lo per quan es reactive.

## Comprovacions executades
- Scripts de regressió estàtica `comprovar_pas_01.py` fins a `comprovar_pas_09.py`: PASS.
- `IosFeedPreferencesContract.kt` compilat amb Kotlin/JVM: PASS, 14 comprovacions sobre codis/valors i modes.
- Compilador Swift, mode de parse de `iosApp/*.swift`: PASS.
- Integritat del ZIP complet i de les fonts Android original i empaquetada: PASS.
- `IosFeedPreferencesContractTest.kt` afegit a `commonTest`, però no s'ha executat en Gradle.

## Pendents / limitacions
- **L'idioma de la interfície se sincronitza com a preferència, però el text de tota la interfície d'iOS continua principalment en valencià.** La traducció i localització integral és un altre treball; no cal confondre guardar l'idioma amb traduir l'app.
- No s'ha executat una compilació KMP per a iOS ni proves reals en iPhone, MariaDB o Android ↔ iPhone. Falta Gradle operatiu/Xcode en l'entorn.
- El formulari desa diversos dominis de servidor consecutivament, no en una transacció global. La cua conserva les escriptures no confirmades; no es declara atomicitat entre endpoints.
- Paritat total de l'algoritme de «Per a tu» continua prevista per al **pas 10**.

**Lliurament:** tercer ZIP complet acumulatiu, amb passos 1–9.
