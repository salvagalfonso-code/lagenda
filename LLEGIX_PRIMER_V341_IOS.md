# L’Agenda v341 — Kotlin Multiplatform Android + iOS

## Canvis sobre v340

- Registre i recuperació de contrasenya mitjançant els endpoints PHP existents; el token continua en Keychain.
- Lector QR natiu AVFoundation en iPhone, amb autorització de càmera i validació **només** per l'API amb permisos professionals. Mostra el codi i requerix confirmació abans de validar.
- Correcció de seguretat en lectura de respostes de validació: `ok:true` no significa que l'entrada s'haja validat. **Només** `outcome: VALIDATED` és un èxit.
- Taulons: el creador pot consultar les sol·licituds pendents i acceptar-les o rebutjar-les des de l'app.
- «Per a tu»: millora l'elecció d'artistes, festivals, categories, organitzadors, espais i activitats amb entrada, mantenint la selecció en caché 30 minuts. Encara no reproduïx l'algorisme íntegre Android.
- Cerca de grup en festival: inclou artistes que només apareixen a l'horari del festival.
- Esdeveniments de més d'un dia: no desapareixen abans de la data de finalització.
- Cercador: debat de 220ms per evitar filtrar a cada pulsació.
- Menys consultes repetides en obrir les pestanyes; agrupació d'anuncis una sola vegada per bloc.
- Permís de notificacions i accés a Configuració d'iPhone; des de la instal·lació no es pot preguntar, només en la primera obertura.
- Script de preflight Xcode/iOS i nous tests del contracte i festivals.
- Codi font original d'Android **sense canvis**.

## Primera compilació macOS

1. Instal·lar Xcode complet i obrir-lo almenys una vegada per acceptar la llicència i descarregar el simulador.
2. Instal·lar JDK 17 o posterior i permetre xarxa per a descarregar Gradle i dependències (fitxers Gradle verificats amb SHA256 pel bootstrap existent).
3. Descomprimir `ANDROID_STUDIO/` en un directori simple sense permisos especials.
4. Des de `ANDROID_STUDIO/`, executar `sh COMPROVACIONS/preflight_ios_v341.sh`.
5. Executar `sh COMPROVACIONS/compilar_ios_mac.sh`.
6. Si falla, compartir **el primer error** de Kotlin/Swift/Xcode (fitxer, línia i 30 línies pròximes). Els errors en cascada no solen ser la causa inicial.

## Comprovacions locals realitzades

- `swiftc -frontend -parse iosApp/AgendaIOS/*.swift`: correcte (només sintaxi, sense SDK Apple).
- `plutil -lint iosApp/AgendaIOS/Info.plist iosApp/AgendaIOS.xcodeproj/project.pbxproj`: correcte.
- `kotlinc` dels onze fitxers Kotlin de regles compartides: correcte, un advertiment preexistent.
- `kotlinc` del selector de «Per a tu» amb models de prova: correcte; reutilització del snapshot i prioritat comercial validades.
- `bash -n` dels scripts: correcte.
- Comparació de SHA amb v340 de tots els fonts de `app/src`: no hi ha canvis.
- No s'ha executat `xcodebuild`, compilació completa Kotlin/Native ni TestFlight: este entorn no té macOS/Xcode.

## Limitacions que NO s'han d'interpretar com a funcions acabades

La paritat funcional **no és encara completa**. Falten pagament de les entrades de pagament, APNs amb backend, QR i càmera en dispositiu real, autenticació externa, perfil i compte, paritat exacta del feed i de la publicitat, la UI completa iOS i la validació d'App Store. No s'ha desplegat res al servidor.

Vegeu `PARITAT_IOS_V341.csv` per secció.
