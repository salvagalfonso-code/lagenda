# L’Agenda iOS v349 — Diagnòstic de Codemagic

Esta revisió **no canvia fonts Kotlin ni la lògica de l'app** respecte del ZIP `LAgenda_iOS_v349_Codemagic_Corregit.zip` (correcció 01). Només modifica la compilació de Codemagic per a:

1. Verificar per SHA-256 els sis fitxers Kotlin afectats per la correcció 01. Si Codemagic ha descarregat una còpia antiga o parcial, la compilació s’atura immediatament amb l’error 20.
2. Guardar `xcodebuild.log` com a artefacte i mostrar els primers 100 errors de compilació, no les últimes 50 línies.
3. Continuar generant `lagenda-ios-simulator.zip` si Xcode completa la compilació.

IMPORTANT: si el workflow està configurat des de la interfície de Codemagic, reemplaça també el contingut del pas «Compilar iOS Simulator amb Xcode» pel fitxer `CODemagic_COMPILAR_DIAGNOSTIC.sh` o selecciona la configuració YAML des del projecte; un canvi al YAML per si sol no afecta un script personalitzat de la interfície.

No s'ha compilat en Mac/iPhone en este entorn. Per a corregir nous errors Kotlin reals serà necessari `artifacts/xcodebuild.log` de la compilació feta amb esta versió.
