# L’Agenda · v337 KMP — LLEGIX PRIMER

## Què conté este ZIP?

És un **projecte Android + Kotlin Multiplatform real**, preparat per a continuar la migració d’iPhone. Parteix **íntegrament** de la v336; el mòdul Android no ha sigut substituït per una demo. S’hi ha afegit `shared` i el projecte d’Xcode `iosApp`.

### Migrat de veritat a `shared/src/commonMain`

11 fitxers originals de lògica Kotlin, amb el mateix paquet per no trencar imports Android:
`AgendaReservationPolicy`, `DefaultFeedPolicy`, `EventPublishingPolicy`, `FollowSuggestionPaging`, `ForYouDiagnostics`, `ForYouHundredPolicy`, `ForYouPublicationPolicy`, `ForYouSnapshotReusePolicy`, `GlobalAdPlacementPolicy`, `PersonalizationOptions` i `SavedCollections`.

`app` ara utilitza `implementation(project(":shared"))`. La lògica compartida és consumida per Android; cap classe no està duplicada en els dos mòduls.

### Primera pantalla iOS (prototip real, no paritat funcional)

- App iOS amb entrada SwiftUI i `ComposeUIViewController`.
- «Sense filtre» consulta el mateix `events/index.php` públic de l’Android; mostra una llista cronològica, cerca per títol/poble/categoria i un diàleg de detall bàsic.
- Actualització manual i caché **en memòria** de 30 minuts **només mentre el procés iOS està obert**. Encara no és la caché persistent de la v336 Android.
- «Per a tu» està identificat com a pendent: **no** inventa recomanacions ni usa preferències d’altres persones.
- Les dades de l’API només es mostren si el servidor torna `ok=true`; no hi ha usuaris ni contrasenyes incorporats.

### Pendent per a paritat amb Android v336

Autenticació/sessió, feed personalitzat per usuari i caché persistent, batecs, fitxes completes amb cartells/horaris, festivals, guardats, recordatoris, notificacions push i APNs, Comunitat, compra/entrades/QR, perfils, configuració, permisos i aplicació de totes les regles editorials/visibilitat al dispositiu iOS. **Aquesta v337 NO és una aplicació preparada per a l’App Store.**

## Com obrir-la des de Windows

1. Descomprimix tot el ZIP, entra en `ANDROID_STUDIO` i obri eixa carpeta en Android Studio.
2. Sincronitza Gradle; confirma que apareixen els mòduls `app` i `shared`.
3. Compila `:app:assembleDebug` i executa la versió Android. No cal Mac per a esta part.
4. Els fitxers de iOS són dins `iosApp` però **no pots compilar l’iPhone a Windows**.

## Com compilar iPhone amb un Mac o Mac remot

1. Copia tota la carpeta `ANDROID_STUDIO` a macOS. Instal·la Xcode compatible, Command Line Tools i JDK 17/21.
2. Des de Terminal al directori `ANDROID_STUDIO`: `chmod +x gradlew` i `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` (Mac Apple Silicon; en Mac Intel usa `linkDebugFrameworkIosX64`).
3. Obri `iosApp/AgendaIOS.xcodeproj` amb Xcode, selecciona l’esquema `AgendaIOS` i un simulador d’iPhone.
4. La fase **Compile Kotlin framework** ja executa `:shared:embedAndSignAppleFrameworkForXcode` abans de compilar Swift.
5. Configura un **Team** de firma en `Signing & Capabilities` si vols instal·lar en un dispositiu físic; per al simulador normalment no cal un compte Apple de pagament.
6. Si la versió d’Xcode no obri el projecte, el fitxer `iosApp/project.yml` permet regenerar-lo amb XcodeGen (`brew install xcodegen`; `cd iosApp && xcodegen generate`).

## Versió i compatibilitat configurades

- Android Gradle Plugin 9.3.1; Gradle 9.5.0, Kotlin/KMP 2.4.20.
- Compose Multiplatform 1.12.2; Ktor 3.6.0; Android compileSdk 37, iOS 16+.
- Xcode compatible amb Kotlin 2.4.20; les versions actualitzades poden demanar ajustaments de plugins.

## Comprovacions realitzades

- Els 11 fitxers compartits de política s’han compilat independentment amb el compilador Kotlin/JVM disponible en este entorn (1.9.0, prova limitada del codi pur): **correcte**.
- `Info.plist` i `project.pbxproj` passen `plutil -lint`.
- Noms de fitxers, importacions i duplicats comprovats; arxiu ZIP íntegre.
- **No s’ha executat una sincronització Gradle completa, un build Android amb AGP 9 ni la compilació iOS/Xcode**; requerixen dependències i/o eines que no hi ha en este entorn. Per tant el primer build en Mac pot revelar incompatibilitats addicionals.

## Seguretat i dades

La connexió iOS és només al catàleg públic. Cap credencial de producció està incrustada en el nou mòdul. Abans de publicar cal revisar privacitat, autenticació, pagaments, tractament d’imatges, visibilitat i normes de moderació en el client iOS.
