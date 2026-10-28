# L'Agenda v339 — Android + Kotlin Multiplatform + iOS

## Estat real

Aquesta versió és **una migració ampliada i corregida**, no una equivalència acabada ni una compilació certificada a Xcode. Manté la base Android, els 11 fitxers de regles compartides i les pantalles iOS desenvolupades en v338.

### Millores funcionals v339

- «Per a tu»: càlcul de la selecció completa independent del text del cercador; reutilització de la caché de 30 minuts. S'incorporen els IDs obligatoris de les campanyes i les fitxes prioritàries retornades per `ads/active-global.php`, fins i tot si el catàleg local és incomplet. Es conserva la regla de no duplicar fitxes.
- Batecs: es mostra el cartell de l'esdeveniment real quan l'endpoint no inclou una imatge pròpia.
- Entrades: es resol el nom de l'activitat des del catàleg local quan el PHP no retorna `event_title`.
- Recordatoris: l'usuari indica la data i l'hora (`AAAA-MM-DD` + `HH:MM`). iOS prepara una notificació local amb esta hora, no una alerta fixa un dia abans. S'invalida una data passada o posterior a l'activitat. Es cancel·la localment quan desactives l'avís. El missatge de la UI indica que s'ha **sol·licitat** la programació, no que Apple l'haja confirmada asíncronament. La data i hora són locals al dispositiu; el servidor només desa la campaneta activa/inactiva.
- Xarxa: temps màxim de 15 segons per petició de les dues APIs Kotlin, en lloc d'esperes indefinides.
- Contractes: prova Kotlin addicional per als camps `start_date` / `event_time` del JSON patrocinat.

### Preparació de compilació

- El mòdul `shared` té targets `iosArm64`, `iosSimulatorArm64` i `iosX64` (Mac Intel).
- El projecte inclou `gradlew` i `gradlew.bat` amb descàrrega de Gradle 9.5.0 i **verificació SHA256**. El JAR estàndard `gradle-wrapper.jar` no és necessari amb este bootstrap particular.
- El script Xcode llança `sh ./gradlew`, sense dependre dels permisos d'execució del ZIP.
- Per a una primera compilació sense compte d'Apple: `sh COMPROVACIONS/compilar_ios_mac.sh` des de la carpeta `ANDROID_STUDIO` en un Mac amb Xcode. Genera el framework Kotlin i fa build de l'app per a simulador amb `CODE_SIGNING_ALLOWED=NO`.
- Per a Android des de Windows: `COMPROVACIONS\\compilar_android_windows.bat`, o obrir `ANDROID_STUDIO` en Android Studio i fer `:app:assembleDebug`.

### Validat en este entorn

- 11 fitxers Kotlin de polítiques compartides compilats amb `kotlinc`.
- Sintaxi de Swift analitzada amb `swiftc -frontend -parse`.
- `plutil -lint` correcte per a Xcode project i `Info.plist`.
- Scripts Shell comprovats amb `bash -n`.
- Comparació i recompte de fitxers i integritat del ZIP (vegeu informe).

### Encara pendent (no ho donem com a complet)

- **Compilació nativa macOS/Xcode de tot el projecte** i correcció dels errors que apareguen; no hi ha Xcode ni SDK Apple en este entorn.
- Paritat exacta de l'algorisme «Per a tu» Android (seguiments per entitat/ubicació/comarques, afinitat, descoberta, exclusions, pesos); la selecció iOS continua sent simplificada.
- Posicionament exacte d'ads HEADER/PREMIUM/NORMAL i comprovació de campanyes en tots els modes.
- Compra interna, Redsys/Stripe, codis QR i validador PRO.
- Push APNs des del servidor, registre de dispositius i confirmació asíncrona efectiva dels avisos locals.
- Perfil ampliat, notificacions avançades, totes les opcions de Comunitat, popups complets, moderació, accessibilitat i proves de seguretat.
- Proves en dispositiu físic, TestFlight i criteris de publicació a l'App Store.

No s'han modificat els PHP ni la BD de producció. No s'han creat dades simulades de l'usuari.
