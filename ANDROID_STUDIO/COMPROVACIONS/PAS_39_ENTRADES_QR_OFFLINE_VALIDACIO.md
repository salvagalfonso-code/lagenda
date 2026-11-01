# L'Agenda iOS v349 — Pas 39/48

## Abast incorporat sobre passos 1–38

- Entrades emeses reals (PHP `tickets/mine.php`, `tickets/purchase.php`, `payments/status.php`) amb modalitat, data, hora, zona, oferta, estat i validació anterior.
- Còpia **exclusiva** d'entrades emeses per a consulta i QR sense connexió: JSON projectat en fitxer AES-256-GCM, clau del Keychain local i identitat vinculada a l'empremta del bearer vigent. No es guarda cap token, estat persistent general ni informació de pagament en esta còpia.
- Persistència immediata després de confirmació de reserves gratuïtes i de `status=paid` amb la llista completa d'entrades del servidor; res no es considera comprat només perquè Stripe retorne `completed`.
- Restauració de la còpia xifrada quan falta connexió, inclús si `auth/me.php` no pot restaurar encara el perfil en relançar l'app. Si torna la connexió, es torna a consultar el servidor i se substituïx la còpia antiga. Sessions de convidat amb ID del servidor admeses.
- Les entrades locals s'etiqueten clarament com a **còpia no actualitzada** (mai autoritat d'ús, anul·lació o pagament).
- El QR usa el codi exacte retornat pel servidor i es genera amb Core Image sense connexió. El fitxer xifrat és l'única persistència necessària; no és necessari guardar PNG independents.
- Validació professional amb càmera AVFoundation i POST a `tickets/validate.php`: l'únic resultat acceptat com a èxit és `outcome=VALIDATED`; rebuig d'entrades utilitzades, anul·lades o desconegudes el determina exclusivament el PHP existent. L'escàner no valida cap entrada offline; requerix un bearer amb permisos i un esdeveniment real.
- A l'eixida de sessió i a la baixa de compte confirmada, s'esborra la còpia d'entrades corresponent al compte. Cap còpia es pot consultar amb un bearer diferent del de la descàrrega.

## Fitxers iOS modificats

- `shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosAgendaApi.kt`
- `shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/AgendaIosScreen.kt`
- `shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosOfflineTicketPolicy.kt` (nou)
- `iosApp/AgendaIOS/AgendaKeychain.swift`
- `iosApp/AgendaIOS/AgendaProfileNative.swift`

## Comprovacions i límits

- Política offline real compilada en Kotlin/JVM aïllat: 26 assercions PASS.
- Regressions de política dels passos 37 i 38: 28 i 24 PASS.
- Script pas 39: els 430 originals no exceptuats de la base 1–36 mantenen el SHA256; el codi Android v336 i els complements PHP de servidor no s'han modificat.
- `swiftc -frontend -parse` PASS; **no** és una compilació contra frameworks d'iOS ni Stripe.
- **Pendent:** compilació de tot el projecte Kotlin/Native+Xcode, prova amb iPhone i APNs/Stripe, prova d'entrada real, tests sobre el PHP/MariaDB en viu, escaneig de codis reals, validació dels permisos del venedor i reconexió després d'un ús. El projecte és implementat en codi, no certificat en producció.
- Un token renovat requerix tornar a descarregar entrades amb connexió per a vincular la nova sessió. La llista guardada no és una comprovació de la validesa actual.

## Entrega

ZIP acumulatiu 13/16: passos 1–39. No modifica Android ni servidors originals.
