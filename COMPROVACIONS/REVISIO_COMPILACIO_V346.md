# V346 — Errors Kotlin/Native que resol aquesta revisió

| Fitxer | Error registrat | Canvi |
|---|---|---|
| `IosForYouSnapshot.kt` | Desestructuració de `Map.Entry` i `it` en codi comú a partir de `toSortedMap()` | `entries.sortedBy { it.key }` amb iteració explícita, compatible amb targets iOS |
| `Clock.ios.kt` | `Unresolved reference timeIntervalSince1970` | `kotlin.time.Clock.System.now().toEpochMilliseconds()` |
| `Clock.ios.kt` | `Unresolved reference timeZoneWithName` | Constructor `NSTimeZone(name = "Europe/Madrid")` |

La v346 canvia **dos fitxers Kotlin** respecte de v345; la resta del projecte és idèntica. El script de Codemagic es conserva.

## Proves locals realitzades

1. Kotlin JVM 1.9 amb stubs dels tipus iOS: el fitxer `IosForYouSnapshot.kt` compila.
2. Test de consistència de la signatura per a mapes amb diferents ordres d'inserció: passa.
3. Canvi de valor d'un radi genera una signatura nova: passa.
4. Les comprovacions existents de conservació d'Android i d'estils: passen.

El fitxer `Clock.ios.kt` exigeix compilar amb Kotlin/Native i SDK d'Apple, **no comprovat en este entorn**. Kotlin 2.4.20 està declarat a `build.gradle.kts`.
