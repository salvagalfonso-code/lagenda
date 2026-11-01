# L’Agenda iOS v349 · Correcció de la compilació Kotlin/Native

**Base:** ZIP 16/16 original, passos 1–48. **Entrada:** registre de Codemagic de l’1 de novembre de 2026 (`:shared:compileKotlinIosSimulatorArm64` fallava). **Abast:** exclusivament codi Kotlin iOS, sense modificació d'Android ni backend/SQL.

## Errors detectats al registre i correcció aplicada

1. `AgendaIosScreen.kt`: `account` s’usava abans de la declaració. `LaunchedEffect(account?.id, tab)` es trasllada després de declarar `account`.
2. `AgendaIosScreen.kt`: la funció local `loadAccountPages` era invocada abans de la seua declaració. S’ha reordenat amb `reloadMyTickets` sense canviar-ne el comportament.
3. `AgendaIosScreen.kt`: `JsonObjectBuilder.put(town, radius)` rebia `Int` on s'espera `JsonElement`; ara usa `JsonPrimitive(radius)`.
4. `IosAgendaApi.kt`/`IosEntityAffinityCatalog.kt`: l’API pública exposava tipus `internal`. S’ha restringit `IosAgendaApi` a `internal` i `IosEntityAffinityCatalog.refresh` a `internal`; el punt d'entrada `AgendaIosScreen` continua sent públic per al pont Swift.
5. `IosAgendaApi.kt`, `IosBatecFeedContract.kt`, `IosFestivalPopupPolicy.kt`: `MutableMap.putIfAbsent()` no es resolia en commonMain de Kotlin/Native; reemplaçat per `getOrPut()` per a preservar la regla de no sobreescriure el valor inicial.
6. `Clock.ios.kt`: substituïdes crides Objective-C no resoltes per inicialitzadors Foundation (`NSTimeZone(name = ...)` i `NSDate(timeIntervalSinceNow = ...)`), conservant `Europe/Madrid` i el canvi de jornada cultural a les 05:00.

## Comprovacions realitzades

- 496/496 fitxers del ZIP original preservats; diferències només en 6 fonts Kotlin d'iOS.
- Android i fitxers de servidor idèntics al ZIP 16/16 original.
- Comprovacions estàtiques noves dels sis grups d'errors: **PASS**.
- `swiftc -frontend -parse` de totes les fonts Swift del projecte: **PASS**.
- `COMPROVACIONS/comprovar_pas_30.py`: **PASS**.

## Límit de validació

**NO s'ha compilat de nou amb Xcode/Kotlin/Native** en este entorn Linux. No es pot afirmar que l'error final de Codemagic haja desaparegut fins a executar el mateix build en un Mac amb Xcode 26.6. Els scripts de regressió antics que comparaven bytes de ZIPs d'etapes anteriors no són tests aplicables a una revisió que intencionadament canvia codi compartit d'iOS. No s'han modificat per simular un PASS.

## Tornar a Codemagic

1. Descomprimir este ZIP i conservar la carpeta `ANDROID_STUDIO` com a arrel del projecte.
2. Mantindre el mateix `xcodebuild` i el mateix esquema `AgendaIOS` de la prova anterior.
3. Verificar que Gradle arriba a `:shared:compileKotlinIosSimulatorArm64` i, si hi ha nous errors, enviar el registre complet incloent el primer missatge `error:`.
4. Si `xcodebuild` arriba a **BUILD SUCCEEDED**, descarregar l'artefacte `.app` de simulador i passar a proves de pantalla i API.
