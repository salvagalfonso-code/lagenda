# L'Agenda iOS v349 — Pas 7/48: categories i subcategories

**Android v336 és la referència immutable. No s'ha modificat el codi Android ni PHP/MariaDB.**

## Implementat en el codi acumulatiu d'iOS

- `IosCategoryTaxonomy.kt`: catàleg d'identificadors d'Android v336, 12 categories i 55 subcategories amb els codis i etiquetes exactes, 196 àlies editorials per a la lectura i claus històriques `CATEGORY__SUB_<n>`.
- `AgendaIosScreen.kt`: personalització llig ara els `entity_key` de `user-state/mine.php` i no les etiquetes `entity_label`; recuperació tant de convidats com de comptes registrats, també després de refrescar. Selecció d'una categoria completa o dels seus fills amb les mateixes regles bàsiques que Android. Escritures de categories i subcategories només com `CATEGORY` mitjançant `user-state/follow.php`, utilitzant la cua xifrada del pas 6. Es retiren les noves escriptures a `ios_pref_subcategories_v1`; qualsevol valor antic es tracta com a llegat, no com a font de veritat.
- `IosPersonalFeedPolicy.kt`: projecta els mateixos codis editorials en la selecció d'esdeveniments d'iOS, amb coincidència exacta de subcategoria, etiqueta o àlies del catàleg Android. Inclou Infantil transversal. No es declara encara paritat de la selecció «Per a tu» (pas 10).
- `IosCategoryTaxonomyTest.kt`: tests comuns de claus, conversió de claus històriques, diffs, selecció i coincidències editorials.

## Comprovacions executades

- `COMPROVACIONS/comprovar_pas_01.py` ... `comprovar_pas_06.py`: PASS.
- `COMPROVACIONS/comprovar_pas_07.py`: PASS; compara codis/etiquetes amb la font d'Android, camins del codi de lectura/escriptura i hashes de 78 fitxers Kotlin Android v336 respecte del ZIP original.
- Compilació + execució aïllada JVM del nou `IosCategoryTaxonomy.kt` amb dependències simulades: PASS. Proves de selecció de categoria, expansió de subcategories, migració `MUSIC__SUB_4`, diferències de seguiments, coincidència exacta, Infantil transversal.
- Compilació Kotlin/Native / Xcode / Gradle de l'app completa: **NO executada** (Gradle wrapper necessita descarregar la distribució de `services.gradle.org`, sense accés en este entorn; no hi ha Xcode).
- Integració real amb MariaDB / iPhone físic: **NO executada**. Els tests comuns `.kt` estan escrits, no executats com a `commonTest`.

## Limitacions i seguiment

- El bolcat d'API `events/index.php` del model actual d'iOS pot no exposar un camp separat amb codis de subcategoria: el filtratge local fa coincidències exactes contra `styles`, les etiquetes i els àlies del catàleg. Per a paritat real del feed, verificar `subcategoryCodes` i l'algoritme d'Android en el pas 10.
- Les preferències repartides entre diversos endpoints no tenen encara transacció multi-endpoint (pas 5). En cas de fallada, la cua conserva els canvis pendents, però no es confirma tota la pantalla com a guardada.
- No s'ha esborrat cap clau `ios_pref_subcategories_v1` antiga de MariaDB; simplement es deixa de consultar o escriure com a font de selecció.

**Resultat:** pas 7 implementat en codi, comprovació estàtica i Kotlin aïllat superades. Pendent verificació d'app completa i servidor real.

**Pròxim pas:** 8: poblacions, comarques i radis del servidor. **Pròxim ZIP:** després del pas 9.
