# L’Agenda · auditoria preventiva per a la compilació iOS
Data: 28/10/2026.

## Àmbit i font

Base: `LAGENDA_IOS_PROJECTE_COMPLET_KOTLIN_CORREGIT.zip`, amb les dues correccions posteriors aportades en `LAGENDA_CORRECCIO_CLOCK_IOS_FINAL.zip` i `LAGENDA_CORRECCIO_IOS_COMPONENT2.zip`. S'ha incorporat també el registre complet de Codemagic que l'usuari ha aportat. **Açò és una còpia de treball, no una lectura autenticada de l'últim commit de GitHub**. Les modificacions que hages fet al repositori després dels ZIP no estan incloses necessàriament.

## Resultat de la inspecció local

- Inventari de 194 fitxers del projecte de treball, incloent l'eina d'auditoria i el seu JSON de resultat.
- 33 comprovacions estàtiques satisfetes: carpetes essencials, esquema compartit d'Xcode, Swift inclòs al projecte, funcions del pont iOS, coherència de scripts i YAML, conflictes Git i integritat de JSON/XML/plist.
- Sintaxi de Swift validada amb `swiftc -frontend -parse` en Linux: **açò NO comprova els imports d'Apple ni els tipus reals dels frameworks**.
- 14 fitxers de polítiques Kotlin sense dependències externes compilats amb Kotlin/JVM 1.9 (l'objectiu real usa Kotlin/Native 2.4.20). Advertència existent: argument no utilitzat a `EventPublishingPolicy.kt`.
- `IosForYouSnapshot.kt` comprovat en compilació Kotlin/JVM aïllada amb dobles de prova dels seus tipus de dependència: tipus i expressions acceptats. **No és una compilació real de KMP**.
- 6 proves ràpides de regles de radi, publicitat, seguiments i feed superades.
- El catàleg iOS d'estils reconstrueix un JSON vàlid (19 fragments, 792 àlies, 647 proximitats). Les dades territorials embegudes contenen 6.013 files amb 8 camps cadascuna; sense files malformades.
- No s'han detectat marcadors de conflicte `<<<<<<<`, `=======`, `>>>>>>>` als fitxers inspeccionats.

## Correccions integrades (només dues fonts Kotlin)

1. `shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosForYouSnapshot.kt`: eliminats `toSortedMap()` i la desestructuració `component1/component2` que estaven fallant a Kotlin/Native; s'ordenen les claus i es consulta el radi amb `Int` explícit. Es conserva la lògica del feed i de les garanties comercials.
2. `shared/src/iosMain/kotlin/com/example/agendavalenciana/ios/Clock.ios.kt`: hora actual amb Kotlin Clock i data local amb `NSDateFormatter` sense constructors o membres invàlids de `NSTimeZone`/`NSDate`. La zona horària ara és la local del dispositiu (no forçada a Europe/Madrid).

## Límits de la revisió

**No hi ha cap compilació Xcode/Kotlin-Native superada.** L'entorn actual no és macOS i no conté el SDK d'Apple; cal comprovar l'última versió del projecte a Codemagic. Tampoc no s'han compilat els grans mòduls Android amb Gradle, ni s'han pogut executar tests iOS en un simulador o un iPhone real. És possible que apareguen altres errors de Swift/Kotlin després de resoldre els actuals. Els 33 controls són verificacions estàtiques, no 33 tests integrals de funcionament.

## Instruccions de GitHub (recomanació: pedaç, no ZIP complet)

1. Fes una còpia de seguretat del repositori o confirma els canvis al GitHub Desktop.
2. Descomprimix `LAGENDA_AUDITORIA_IOS_PATCH_ACUMULAT.zip`.
3. Copia **sols la carpeta `shared`** a l'arrel local `lagenda`, fusionant carpetes i substituint exactament `IosForYouSnapshot.kt` i `Clock.ios.kt`.
4. A GitHub Desktop, confirma que els canvis són només estos dos fitxers, fes `Commit to main` i `Push origin`.
5. Al GitHub web, comprova que `IosForYouSnapshot.kt` ja NO conté `toSortedMap()` ni `Clock.ios.kt` conté `NSTimeZone(name = ...)`.
6. A Codemagic, selecciona la branca `main` i comprova que el commit coincideix amb el de GitHub abans de compilar.
7. Utilitza el workflow existent `L'Agenda - iPhone PROJECTE COMPLET`; no cal tocar YAML si es pot executar l'`xcodebuild` d'abans. Si falla, descarrega `artifacts/xcodebuild.log` i extrau tots els errors amb `COMPROVACIONS/resum_errors_codemagic.py`.

## Estat honest

- Corregit en codi: **2 fitxers**, comprovats localment amb les limitacions indicades.
- Configuració Codemagic: **ja arriba a Kotlin/Native**, segons el registre aportat.
- Compilat i verificat en simulador iPhone: **NO**.
- Funcionalitats iOS verificades en dispositiu: **NO**.
