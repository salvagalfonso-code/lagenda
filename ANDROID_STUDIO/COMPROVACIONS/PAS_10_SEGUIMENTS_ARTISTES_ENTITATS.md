# Pas 10/48 — Seguiments compartits Android v336 ↔ iOS v349

- Android v336 **no s'ha modificat**. Tampoc `web.zip` ni el servidor.
- Capa iOS: `user-state/mine.php` per a llegir i `user-state/follow.php` per a seguir/deixar de seguir. Els convidats utilitzen el mateix contracte Bearer i la mateixa `user_follows` que els comptes registrats.
- Identitat del seguiment: `entity_type` + `entity_key` PHP normalitzada (minúscules, espais compactats); la `entity_label` és descriptiva, no una clau per esborrar. Els tipus ARTIST, ORGANIZER, VENUE i CATEGORY es mantenen separats.
- Per a ORGANIZER, quan el catàleg públic `user-profiles/catalog.php` dona una correspondència única, s'utilitza el `username`, com Android. Si no hi ha identificador inequívoc en el catàleg, l'etiqueta continua sent el millor valor disponible: **no es pot certificar paritat absoluta** per a eixos casos sense ampliar els identificadors del catàleg/API.
- La cua del pas 6 projecta seguiments pendents per usuari sobre les lectures antigues sense perdre les operacions. En els POST de `user-state/follow.php`, la resposta ha de confirmar tipus, clau i `following` abans d'eliminar l'acció de la cua xifrada.
- UI sense mur de registre per a seguir, i protecció davant de GET iniciats abans d'un canvi de seguiment. Canvis pendents de xarxa es mostren com a pendents, no guardats en servidor.
- Regressió de codis CATEGORY: `IosCategoryTaxonomy.subcategoryCode` admet les claus normalitzades en minúscules que retorna PHP.
- Proves: `comprovar_pas_10.py` + `IosFollowCompatibilityTest.kt` i prova JVM aïllada de `IosFollowMutation.kt`.
- **No verificat**: compilació Kotlin/Native/Xcode, execució en iPhone real, endpoints en producció, lectura/escriptura real MariaDB, càrrega de catàleg de perfils en entorn real. Sense nova versió ni ZIP (ZIP previst després del pas 12).

## Resultats de les comprovacions d'esta passada

- `comprovar_pas_01.py` a `comprovar_pas_10.py`: PASS.
- `kotlinc IosFollowMutation.kt FollowSmoke.kt ...` + `java -jar`: PASS (11 afirmacions).
- `kotlinc IosFollowMutation.kt IosCategoryTaxonomy.kt ...` + `TaxonomySmokeKt`: PASS.
- `swiftc -frontend -parse iosApp/AgendaIOS/AgendaKeychain.swift`: PASS.
- Compilació completa `bash ./gradlew ... --offline`: **NO EXECUTADA** (no resol `services.gradle.org`).
