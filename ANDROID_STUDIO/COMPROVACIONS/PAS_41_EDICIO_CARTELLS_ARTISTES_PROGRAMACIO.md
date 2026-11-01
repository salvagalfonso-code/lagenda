# Pas 41/48 — Edició de publicacions, artistes, cartells i programació — implantació en codi

## Canvis i correspondència amb Android v336

- `IosPublisherPanel.kt`: les activitats pròpies publicades es poden obrir per editar, canviar de cartell i accedir a programació. A l'editor s'han fet editables data/hora finals, la llista d'artistes afegible/eliminable (sense crear perfils d'artista nous) i els camps d'ubicació, descripció, disciplina, organitzadors i venda. Els canvis d'artistes es guarden via `events/community-update.php`; no s'afirma que canviar una llista edite el perfil de l'artista. El panell mostra la imatge del cartell actual.
- `IosPublisherRoles.kt`: afegit `posterUrl` a l'esdeveniment propi.
- `IosAgendaApi.kt`: `publisherOwnEvents` llig noms i cartells del servidor; `publisherEdit` contrasta després del POST els camps editables amb el GET server, incloent dates, artistes, organitzadors, estil, preu i canal de venda; `publisherUploadPoster` reutilitza el selector JPEG natiu i `events/community-poster.php` de l'Android de referència, verifica que el servidor ha associat el nou URL a l'esdeveniment i deixa el resultat incert sense cap reintent automàtic. La foto del cartell no es desa en preferències locals.
- `IosPublisherSchedulePanel.kt`: editor de la programació especial, amb títol, dia, inici/final, escenari, artista, detalls, imatge HTTPS, ordenació i eliminació amb confirmació. Com Android, usa `special-events/manage/config.php`, `save-schedule.php` i `delete-schedule.php`. El client només permet escriure quan `access.can_edit_schedule` és autoritzat pel servidor i refresca el GET després de cada POST. No hi ha una API general de programació per a esdeveniments ordinaris a la referència Android; el panell de programació especial mostra un avís si no existeix eixe mòdul al servidor.
- `IosPublicationEditPolicy.kt`: verificació estricta dels camps editats, cartell i horaris. Les operacions POST no es reintenten automàticament després de respostes incertes, per evitar duplicar actuacions i versions.

## Proves realment executades

- `COMPROVACIONS/provar_pas_41.kt`: **39/39** assercions de la política Kotlin real compilada amb `kotlinc` i executada en JVM. No equival a compilació de Compose / Kotlin Native.
- `/mnt/data/verify_pas41.py`: **PASS**. 447 fitxers del ZIP 1–39 inalterats, incloent tots els fitxers `app/` Android, `iosApp/` Swift i servidors de referència; les modificacions són només de `IosAgendaApi`, `IosPublisherRoles`, `IosPublisherPanel` i els fitxers nous de la funcionalitat, més el pas 40 anterior. Es conserva el pas 40.
- `swiftc -frontend -parse iosApp/AgendaIOS/*.swift`: **PASS**, sense canvis Swift.
- `bash ./gradlew :shared:compileKotlinIosSimulatorArm64 --offline`: **NO executat amb èxit**, el wrapper intenta resoldre `services.gradle.org` i la xarxa el bloqueja (`curl: (6) Could not resolve host`). No hi ha build complet en Xcode ni prova física iPhone.

## Limitacions importants

- No s'ha fet cap POST real ni comprovació de permisos/estats contra MariaDB; no és possible certificar que tots els camps reflectits pel PHP en viu coincidisquen amb el model de referència.
- El cartell s'envia des de la llibreria de Fotos mitjançant el selector iOS ja disponible, amb la mateixa entrada JPEG base64 que Android; no incorpora un editor de retall/enquadrament encara que l'API permet `image_focus_x/y` i `image_scale`, que s'envien centrats.
- La programació especial NO està habilitada per defecte a tots els esdeveniments: depén de la configuració de `special-events/manage` i permisos `can_edit_schedule` retornats per servidor.
- No s'ha revisat l'edició completa de perfils d'artista (es diferent de la llista d'artistes d'un esdeveniment). La publicació de convidats i el control de revisió són els del pas 40, sense canvis.
- No hi ha encara compilació verificada d'iPhone ni paritat visual certificada. NO presentar estes comprovacions estàtiques com a prova d'integració en iPhone.

## Estat / següent

**Pas 41 implementat en codi i acumulat sobre els passos 1–40.** Pròxim pas 42: alta professional, dades fiscals i activació de venda. El ZIP acumulatiu 14/16 és exclusivament després d'acabar el pas 42.
