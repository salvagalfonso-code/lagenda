# L’Agenda – Correcció compilació iOS ARM64

## Diagnòstic del registre de Codemagic

L’error ocorre a `:shared:compileKotlinIosX64`. Gradle no pot resoldre
`org.jetbrains.compose.runtime:runtime:1.12.2` per `ios_x64` (ni diversos
altres mòduls Compose). El mateix registre confirma que
`:shared:compileKotlinIosSimulatorArm64` i
`:shared:linkDebugFrameworkIosSimulatorArm64` s’han executat abans de la fallada.

## Canvis inclosos

1. `shared/build.gradle.kts`: elimina la declaració `iosX64()`; conserva
   `iosArm64()` i `iosSimulatorArm64()`.
2. `codemagic.yaml`: el workflow manté l’identificador
   `lagenda-ios-projecte-complet` i passa a mostrar el nom
   «L’Agenda - iPhone ARM64 (corregit)»; `xcodebuild` utilitza
   `-arch arm64` sense `-destination generic`, per no exigir també x86_64.
3. `COMPROVACIONS/compilar_ios_mac.sh`: evita l’antic objectiu x64,
   requereix host Apple Silicon i compila el simulador només per ARM64.

## Verificació realitzada

- Sintaxi de YAML analitzada.
- Scripts del workflow validats amb `bash -n`; script local amb `sh -n`.
- Cap referència activa a `iosX64()` en la configuració Gradle.
- Artefactes del ZIP base conservats excepte els tres fitxers anteriors.

## Limitacions

No s’ha executat `xcodebuild` ni `compileKotlinIosSimulatorArm64` en la
versió corregida: esta màquina no disposa de macOS/Xcode. La correcció elimina
la incompatibilitat `ios_x64` coneguda; pot aparéixer un error nou en les fases
Swift/Xcode que no s’havien executat encara.

## Instruccions

Descomprimix el ZIP petit de correccions i copia exactament els tres fitxers
al repositori local `lagenda`, respectant les carpetes. Fes Commit to main i
Push origin en GitHub Desktop. Després refresca els workflows de Codemagic,
tria «L’Agenda - iPhone ARM64 (corregit)» i inicia una compilació nova.
No carregues el ZIP directament en Codemagic.

Si compila correctament, a Artifacts apareixerà
`lagenda-ios-simulator.zip`. Això comprova una app de simulador, **no** una
versió provada en un iPhone físic ni la paritat funcional amb Android.
