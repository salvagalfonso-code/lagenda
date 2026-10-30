# L’Agenda iOS v350 — formulari inicial dels convidats

## Implementat en el codi (no compilat)

- Nova pantalla Compose `ANDROID_STUDIO/shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosFirstRunOnboarding.kt` que reprodueix el recorregut d’Android `FirstRunOnboarding.kt` i `FirstRunOnboardingPolicy.kt`: benvinguda; 8 disciplines en dues pantalles; subcategories; municipis i radis 5/15/30/50/100 km; grups de música, **només** si s’ha marcat Música; final amb «Continuar sense usuari», «Crear un usuari» i «Iniciar sessió». Les disciplines i subcategories usen codis canònics compartits.
- Efecte de primera instal·lació: solament després de comprovar que no hi ha sessió restaurada, amb registre persistent `ios_first_run_state_v350=offered|done`. Es pot ometre i no reapareix a cada obertura, ni en tancar sessió. Una instal·lació anterior amb catàleg/preferències persistides no és interrompuda en actualitzar.
- Les seleccions del convidat s’apliquen immediatament a les claus `pref_categories_guest`, `pref_subcategories_guest`, `pref_towns_guest`, `pref_radii_guest` i a `ios_guest_follows_v350`. El pas actual es conserva en `ios_intro_step_v350`; la selecció sobreviu si es tanca l’app abans de finalitzar.
- Les categories que tenen subcategories específiques es converteixen als **codis de subcategoria**; les categories sense subcategories seleccionades es converteixen a **CATEGORY general**, com en Android.
- La migració de seguiments de categories i artistes al compte reutilitza `IosGuestFollowTransfer` i `user-state/follow.php`. Després d'una escriptura confirmada al servidor, el feed «Per a tu» torna a recalcular-se.
- Nova transferència amb jornal de pobles/radis del convidat al compte per a restauració de sessió, registre, login amb contrasenya i login social. Es combinen amb les localitzacions ja existents al compte, sense substituir-les; si l'API falla, la cua local queda pendent. L’estat es conserva per compte i no es reaplica als comptes següents.
- Fotografies canòniques d’`entities/images.php` quan estan disponibles i targetes amb reserva visual quan no n’hi ha. Els grups es proposen en funció de concerts reals del catàleg; no s’inventen artistes.
- Per a un convidat nou, «Suggerències» té el mateix valor inicial que en la càrrega normal de Personalització (activades per defecte llevat que s’hagen desactivat).

## Verificació executada

- `python tests/test_first_run_onboarding_ios_v350.py` — **19 comprovacions estructurals superades**.
- `python tests/test_feed_preferences_sync.py` — **13 comprovacions estructurals superades**.
- `php SERVIDOR_PER_A_TU/tests/test_engine.php` — **8 comprovacions del motor PHP superades**.
- `kotlinc` sense les biblioteques Compose analitza la sintaxi sense detectar errors sintàctics en la nova pantalla, però assenyala importacions no resoltes i **no** valida la compilació del projecte.

## Pendent abans de declarar-la operativa

- Compilar la v350 amb Kotlin/Native i Xcode en Codemagic, i provar cada pantalla en simulador iPhone.
- Validar fotografies i suggeriments amb el catàleg real.
- Comprovar restauració de mig formulari, inici de sessió en una altra instal·lació, guest → compte existent i còpia de poblacions/radis amb MariaDB de producció, sense sobreescriure preferències remotes.
- Desplegar les rutes noves `api/preferences/feed.php` i `api/for-you/feed.php` i les migracions de MariaDB de la passada anterior perquè el feed compartit siga operatiu.

No s’ha augmentat el número de versió, no s’ha creat cap ZIP i no s’ha desplegat res al servidor.
