# Correcció de compilació Codemagic 02 — 01/11/2026

## Diagnòstic
La compilació del paquet CORRECCIO_01 va confirmar els 6 hash de font (`OK`) i només va presentar cinc errors de Kotlin/Native en `Clock.ios.kt` (línies 18 i 26).

- `NSTimeZone(name = "Europe/Madrid")`: constructor Objective-C no disponible en Kotlin/Native. Es reemplaça per `NSTimeZone.timeZoneWithName("Europe/Madrid")`.
- `NSTimeZone.systemTimeZone`: referència que el compilador no reconeix; s'elimina. En cas improbable d'absència de la zona horària, el formatador conserva la zona local.
- `NSDate(timeIntervalSinceNow = -86_400.0)`: constructor no disponible. Es reemplaça per `NSDate(timeIntervalSinceReferenceDate = localDate.timeIntervalSinceReferenceDate - 86_400.0)`, que és un constructor reconegut pel compilador.
- Es conserva la regla funcional que abans de les 05.00 el dia cultural és l'anterior en zona Europe/Madrid.

## Integritat
S'han conservat sense alteració els 500 fitxers del ZIP 01; s'ha canviat només `Clock.ios.kt`, el manifest SHA-256 i els scripts de diagnòstic (el YAML també té còpia en l'arrel). S'afegeix este informe.

## Estat de verificació
- Verificació sintàctica/estàtica i dels SHA-256: realitzada.
- Compilació Kotlin/Native i Xcode al Mac de Codemagic: **pendent**.
- Android i servidor PHP/SQL: **no modificats**.
