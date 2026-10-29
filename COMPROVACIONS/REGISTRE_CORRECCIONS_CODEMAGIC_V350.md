# Correccions de compilació Codemagic — v350 (sense canvi de versió)

## 2026-10-29 — Error `compileKotlinIosSimulatorArm64` Personalització
- Fitxer: `shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosPersonalizationScreen.kt`, línia 448.
- Error: `.clickable(onClick)` resolia incorrectament l'argument posicional com a `enabled: Boolean`.
- Correcció real en la carpeta de treball: `.clickable(onClick = onClick)`.
- Comprovació: comparat el fitxer amb el ZIP enviat, l'única diferència és aquesta substitució; escaneig d'altres usos `.clickable(identifier)` al codi compartit sense resultats.
- Estat: corregit en codi, no recompilat per a iOS (requereix macOS/Xcode i dependències Gradle).
- Distribució: pendent a petició de l'usuari; el ZIP v350 anterior encara conté la línia errònia.
