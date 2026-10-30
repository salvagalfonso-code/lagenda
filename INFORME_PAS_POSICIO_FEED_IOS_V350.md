# L'Agenda iOS v350 — Conservar posició de lectura durant 1 hora fora de l'app

## Estat: implementat en codi; pendent de compilació i prova en iPhone.

Data: 29/10/2026

### Contracte respecte d'Android
- Una hora es compta exclusivament des que l'app entra en segon pla, no durant l'ús continuat.
- Durant menys de 3.600.000 ms en segon pla es restaura el primer ítem visible i l'offset en píxels.
- Amb 3.600.000 ms o més, o si el rellotge retrocedeix respecte a l'entrada en segon pla, es descarta la posició i es torna al començament.
- Reobrir Guardats, Entrades, Comunitat, Perfil o un popup, sense abandonar l'app, no ha de reiniciar la lectura.
- Canviar expressament de mode, confirmar una cerca o refrescar manualment sí reinicia la posició.
- Tancar el visor de Batecs conserva el seu comportament anterior: torna a l'inici.
- Les preferències i la lectura estan separades segons la vista i el compte; no es reutilitza una posició d'altre compte.
- Es restaura també el lot carregat, i no es fa scroll abans d'hidratar els ítems necessaris.
- No sobreescriure la posició d'un compte amb el feed de convidat provisional mentre auth/me.php continua pendent.
- Una vista prèvia de 50 esdeveniments no força una restauració errònia si la posició queda més avall.

### Fitxers modificats (respecte al ZIP v350 de Codemagic anterior)
- `ANDROID_STUDIO/shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosFeedReadingSession.kt` (nou)
- `ANDROID_STUDIO/shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/AgendaIosScreen.kt`
- `ANDROID_STUDIO/shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/EventCatalog.kt`
- `ANDROID_STUDIO/shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosAgendaApi.kt`
- `ANDROID_STUDIO/iosApp/AgendaIOS/AgendaKeychain.swift`
- `ANDROID_STUDIO/iosApp/AgendaIOS/AgendaIOSApp.swift`

### Verificació executada
- 12 comprovacions JVM de la sessió de lectura: càrrega, desament, persistència, vista diferent, 59:59.999, caducitat 60:00, més de cinc hores dins de l'app, rellotge enrere, normalització dels valors i neteja.
- 10 comprovacions estàtiques de la unió entre composable, API KMP, SwiftUI scenePhase i condicions de restauració.
- Sense modificacions als fitxers originals d'Android.

### Pendent
- Compilar `compileKotlinIosSimulatorArm64`, Xcode i executar al simulador.
- Provar en un iPhone: deixar el feed al mig d'una card, tornar als 30 min (mateixa posició); tornar als 61 min (inici); consultar altres pestanyes sense eixir; obrir/tancar un popup; iniciar sessió després d'haver guardat una posició; restauració des del catàleg complet i amb dades noves.
- No s'ha creat cap ZIP ni incrementat v350.
