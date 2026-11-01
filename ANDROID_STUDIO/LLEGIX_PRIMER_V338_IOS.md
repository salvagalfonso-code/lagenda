# L’Agenda v338 — Android acumulat + migració iOS ampliada (Kotlin Multiplatform)

## Estat real

**Aquest ZIP NO és una conversió completa ni un IPA instal·lable.** Conté el projecte Android acumulat de la v336/v337, amb els components de política compartits, i una implementació iOS de moltes pantalles connectada a la mateixa API. **No està validada per Xcode.** Les noves operacions requereixen proves en Mac ARM/Apple Silicon i en dispositiu iOS real.

### Nou en v338 per a iOS

- Navegació iPhone amb **Inici / Guardats / Entrades / Comunitat / Perfil**; cap pantalla de demo amb activitats falses.
- Catàleg públic complet de `api/events/index.php`, **persistit en fitxer privat d’Application Support** durant 30 minuts; si hi ha connexió pot refrescar-se manualment, sense netejar la vista abans de rebre dades.
- Cartells reals (Coil Multiplatform), scroll paginat de 5 + 15 + 10, cerca per artistes/títols/pobles, detall amb horaris i focus en artista cercat, botó de cartell.
- Login a `auth/login.php`, comprovació `auth/me.php`, token **al Keychain iOS**, dades d’usuari a `user-state/mine.php`. Contrasenya no persistida.
- `Per a tu` **parcial** segons seguits reals i preferències de categoria/població i imposició comercial del feed; llista ordenada en caché **per usuari durant 30 min**. No s’ha portat la puntuació completa de l’Android (radis, afinitats avançades, descobertes, exclusions ni personalització remota): **no hi ha paritat**.
- Guardar/desar esdeveniments via `events/save.php`, seguir artistes via `user-state/follow.php`, activar avisos via `user-state/event-alert.php`, local iOS amb UserNotifications i desactivar-los amb confirmació.
- Pestanyes Guardats / Recordatoris; orfes no es mostren; imatges en les targetes.
- Batecs: consulta de `batecs/index.php` i vista dels seguits si n’hi ha almenys 6. Les visualitzacions de batecs multimèdia encara necessiten una migració pròpia.
- Entrades del compte reals des de `tickets/mine.php`; es mostren estat i codi. **No generem QR ficticis** ni pagaments simulats.
- Taulons de Comunitat (`community/groups.php`, `join.php`, `messages.php`, `send.php`), amb inici de sessió i control de membres.
- Safata interna de notificacions (`notifications/mine.php`, `read.php`), filera de sis icones i panell de recordatoris. La segregació per categories requereix dades de servidor; no s’inventa.
- Perfil, autenticació, personalització local bàsica i campanyes de bústia interna del SUPER_ADMIN amb previsualització i doble confirmació.
- Accés extern a URLs HTTPS d'entrades; **no** s'han portat les compres natives Stripe/Redsys.

### Funcions encara NO migrades o incompletes

1. **Paritat de «Per a tu»**: l'algorisme Android en `AgendaState.kt` (afinitat, radi, comarca, exclusions i cerca, preferències remotes, puntuació i finestres temporals) és més complex. La versió iOS només fa una selecció bàsica fidel als seguiments explícits i prioritats comercials, NO promet el mateix feed.
2. **Pagaments**, compra/reserva completa, opció de tiquet, reversions i confirmació iOS, QR natiu de la compra i validador PRO.
3. **Push APNs**: les notificacions locals sí estan implementades; les push remotes exigixen registre de tokens APNs al backend, credencials d’Apple i una cua d’enviament real. El sistema existent només envia notificacions a la safata interna.
4. **Google / Sign in with Apple**, registre, recuperació de contrasenya, perfils professionals complets, editor d’esdeveniments, botiga, donatius i dashboard avançat.
5. **Chat, llistes compartides, notificacions i batecs**: les seccions bàsiques estan connectades a les API, però faltarà reproduir tots els estats de la v336 i els permisos/editorials detallats.
6. **Caché local robusta** de l’Android per a tota la informació; a iOS es guarden esdeveniments al directori privat d’Application Support i IDs del feed a preferències locals, no una base SQLite completa. No es pot garantir funcionament offline total.
7. **Test real**: no hi ha compilació Xcode, IPA, TestFlight ni prova visual en iPhone. Pot haver-hi errors de compilació/API que calga reparar en Mac.

## Instruccions per al teu Windows + Mac remot

1. Descomprimeix el ZIP. A Windows obri **`ANDROID_STUDIO/`**, sincronitza Gradle i verifica `:app:assembleDebug`. Per primera vegada cal executar `setup-wrapper.bat` perquè el ZIP original no tenia el `gradle-wrapper.jar`.
2. Copia **tota** la carpeta `ANDROID_STUDIO/` a un **Mac Apple Silicon** amb Xcode i JDK 17/21. El target `iosX64` està retirat: Coil 3.6.3 no dona suport al simulador Intel.
3. Des de la carpeta: `chmod +x gradlew && ./gradlew :shared:linkDebugFrameworkIosSimulatorArm64`. Si falta el wrapper, instal·la Gradle 9.5.0 o genera `gradle/wrapper/gradle-wrapper.jar` amb una distribució Gradle vàlida.
4. Obri `iosApp/AgendaIOS.xcodeproj` i selecciona simulador iPhone. La fase de compilació embeguda de Kotlin executa `:shared:embedAndSignAppleFrameworkForXcode`. Si regenera Xcode amb `xcodegen`, el fitxer `iosApp/project.yml` inclou tots els fitxers Swift de la carpeta.
5. Comprova inicis de sessió, actualització, cartells, targetes, festivals, segueix/dessegueix, Guardats, avisos, Comunitat i notícies amb un compte de proves. No proves pagaments en producció.
6. Per distribuir en TestFlight cal Apple Developer, signatura i configuració d’App Store Connect. Per distribuir amb push cal implementar APNs abans.

## Arquitectura i seguretat

- La **versió Android original** continua al mòdul `app`; no s'han reescrit les seues pantalles ni serveis en un intent d'unificar-los a cegues.
- El mòdul `shared` exposa les onze regles Kotlin extretes a la v337 i nous serveis/data classes + la UI Compose iOS.
- El nou codi Kotlin reutilitza els endpoints públics i autenticats del servidor real `https://quimerafest.com/app/api/` que usa Android.
- Els tokens **només** es guarden al **Keychain natiu d'Apple** (`AgendaKeychain.swift`), amb `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`. Mai es guarden en `NSUserDefaults`.
- Les peticions HTTP són HTTPS amb capçalera `Authorization: Bearer`; no s'ha inclòs cap clau secreta ni token en el ZIP.
- Atés que l'endpoint públic inclou tots els actes publicats, les exclusions editorials i les campanyes han de validar-se de manera integral abans d'obrir iOS a usuaris. La pantalla de preferències iOS encara no implementa totes les exclusions de l'Android.

## Comprovacions efectuades sense Mac

- `plutil -lint` de l'`Info.plist` i de l'`AgendaIOS.xcodeproj/project.pbxproj`: OK.
- `swiftc -frontend -parse` dels fitxers Swift: OK (anàlisi sintàctica, **no** linking ni validació de framework).
- `kotlinc` 1.9 en mode parser de les fonts `shared/.../ios/*.kt`: cap error de sintaxi detectat, però **no** compila ni enllaça Kotlin/Native ni les dependències Compose 2.4.20.
- `:shared:linkDebugFrameworkIosSimulatorArm64`, `:app:assembleDebug` i proves reals iOS: **NO EXECUTADES**; falten SDK i dependències d'Android/macOS en este entorn.

## Rendiment i peticions

El catàleg se serveix abans de sincronitzar-lo, amb persistència de 30 minuts en iPhone. La selecció d'IDs de «Per a tu» també es guarda per compte per 30 minuts. No es fa cap consulta HTTP addicional en entrar a Guardats mentre la sessió està carregada; actualitzar una pestanya personal sí pot consultar l'API. Si fallen les dades del servidor, els últims esdeveniments públics es mantenen visibles; errors d'operacions autenticades es mostren a la UI.
