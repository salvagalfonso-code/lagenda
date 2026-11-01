# Pas 27/48 · Popup d'artista · iOS v349

**Referència congelada:** Android v336. **Còpia de treball:** iOS acumulat 1–26. Cap canvi a Android ni al PHP.

## Implementat en codi

- Popup d'artista amb **fotografia pròpia** d'`professional-profiles/catalog.php?type=GROUP&q=...` o, com a alternativa, `entities/images.php` de tipus ARTIST. Es requereix coincidència exacta del nom normalitzat; **mai s'utilitza el cartell d'un concert com si fora el retrat de l'artista**. Si no hi ha imatge oficial es mostren les inicials.
- Disciplina estimada de les **categories reals de les activitats** (el payload públic de `professional-profiles/catalog.php` no inclou `discipline`). Es mostra en cantó superior dret de la imatge. La font d'estil (`style_primary`) es mostra separada, sense confondre-la amb disciplina.
- Botons **Seguir** i **Notificacions** immediatament sota la fotografia. Seguir reutilitza `user-state/follow.php`/`user-state/mine.php` i la cua xifrada dels passos 10 i 6. La campaneta reutilitza `notifications/artist-bell.php`, que també accepta identitat de convidat amb Bearer; actualització només per a la identitat de servidor que l'ha sol·licitada. Els avisos APNs continuen pendents de verificació.
- Plataformes musicals compactes com a **enllaços de cerca**, explícitament no perfils oficials: Spotify, YouTube Music, Apple Music, Deezer, TIDAL, Bandcamp. Només en perfils identificats com a música per activitats conegudes.
- Secció de **Concerts** (o activitats de la disciplina) amb esdeveniments futurs del catàleg i ID canònic, accés al popup d'esdeveniment.
- **Productes reals** des de `artist-store/catalog.php?artist=...`: nom, fotografia, descripció, preu en cèntims convertit a euros, disponibilitat. No es mostren productes d'un nom semblant. L'API ja existent és la mateixa que l'Android v336. **No** es simula compra, pagament o stock: la integració de pagament queda per a un pas posterior.
- Errors de càrrega/reintent visibles. Caches de detalls únicament en RAM de la pantalla; no es creen preferències persistents noves d'usuari al dispositiu.

## Comprovacions disponibles

- 27 scripts estàtics 01–27: **PASS**.
- Kotlin/JVM aïllat de la política real `IosArtistPagePolicy.kt`: 24 comprovacions **PASS** (identitats exactes, seguretat d'URLs, cèntims, stock, disciplina).
- Sintaxi dels fitxers Swift (`swiftc -frontend -parse`): **PASS**.
- Hash SHA256 dels fitxers Android de referència v336 i de l'Android original incrustat en la v349: **sense canvis**.
- ZIP acumulatiu comprovat; inclou tota la còpia de treball 1–27.

## No verificat / limitacions

- **No** compilació completa Kotlin Multiplatform/iOS o proves visuals amb iPhone. `gradlew` ha intentat obtindre Gradle, però `services.gradle.org` no és resoluble des d'este entorn.
- **No** proves d'API/MariaDB en viu; es validen els contractes sobre els PHP aportats.
- `notifications/artist-bell.php` està en `SERVIDOR_APNS`, però no en el paquet principal `web.zip`. Cal confirmar desplegament i taules abans de garantir campanetes reals. Android v336 guarda les campanetes pròpies en preferències locals (`artist_alerts`): amb Android congelat **no** es pot garantir paritat Android ↔ iOS d'eixes preferències.
- El catàleg públic de perfils professionals no conté el camp de disciplina explícita que utilitza Android; iOS deriva la disciplina de les activitats. Sense activitats conegudes pot quedar sense identificar i no mostrar plataformes musicals.
- La compra de productes, Stripe, comandes i gestió professional no formen part d'este pas; són treballs posteriors del pla.
