# L’Agenda v340 — Integració iPhone + Android acumulada

**Base:** v339 del 28/10/2026. **Projecte:** `ANDROID_STUDIO` (Android `app`, multiplataforma `shared`, iOS `iosApp`).

## Què s'ha implementat de manera real a v340

- **Entrades d'iPhone:** les modalitats reals, el preu, la disponibilitat i el màxim per comanda es consulten a `tickets/options.php`; el backend `tickets/purchase.php` emet **només entrades gratuïtes**. No es simula cap compra de pagament. La pantalla «Entrades» disposa de botó QR natiu per al codi real emés pel servidor; utilitza Core Image, sense biblioteques externes i sense generar codis nous.
- **Validació professional:** des de Perfil per a comptes no públics, introducció de codi d'entrada i ID d'esdeveniment, amb `tickets/validate.php`. El PHP comprova l'autorització i el consum atòmic de l'entrada; no s'ha afegit un escàner de càmera, sinó validació manual.
- **Comunitat:** crear taulons per a un esdeveniment a través de `community/create.php`; denunciar missatges a través de `community/report.php`. Tipus compatibles amb PHP `CAR`, `TRANSPORT`, `MEETUP`, `OTHER` (allotjament compartit encara és `OTHER` al servidor). El sistema de permisos de Comunitat continua controlat pel PHP.
- **Sessions:** tancament amb intent de revocació del token en `auth/logout.php`, i eliminació del token de Keychain fins i tot sense xarxa.
- **Navegació:** notificacions internes amb deep-link a la fitxa d'esdeveniment, Comunitat, Entrades o artista, només per als tipus expressament reconeguts.
- **Feed:** esdeveniments de campanyes i venda interna garantits durant les cerques tant de «Per a tu» com de «Sense filtre», sense duplicar fitxes. Etiqueta els emplaçaments `HEADER`, `FEED_PREMIUM`, `FEED_NORMAL`, però **la distribució visual exacta d'Android no està migrada**.
- **No s'ha modificat la base de dades de producció ni els PHP.** Les API indicades ja existixen en el paquet de servidor aportat.

## Com provar en Mac (també Mac remot)

1. Descomprimir este ZIP i obrir Terminal dins d'`ANDROID_STUDIO`.
2. Instal·lar Xcode complet i un JDK 17 o superior.
3. Executar `sh COMPROVACIONS/compilar_ios_mac.sh`. El script descarrega Gradle 9.5.0 comprovant-ne el SHA256, construïx el framework iOS i executa Xcode per a simulador sense firma.
4. Si compila, obrir `iosApp/AgendaIOS.xcodeproj`, seleccionar `AgendaIOS` i un simulador d'iPhone i executar Run.
5. Per compilar Android, obrir `ANDROID_STUDIO` a Android Studio i executar `:app:assembleDebug`.

**No és possible certificar la compilació iOS sense macOS i Xcode.** La comprovació de sintaxi Swift feta fora de macOS no valida el tipatge Swift amb el framework Kotlin/Native ni l'enllaç final.

## Proves manuals prioritàries en el simulador

- Obrir «Sense filtre» i «Per a tu» sense connexió i amb connexió; refrescar; comprovar cartells i posicions publicitàries.
- Iniciar i tancar sessió; confirmar que el compte es restaura després de tancar l'app.
- Guardar esdeveniments i configurar recordatoris; comprovar el permís de notificacions.
- Obrir «Entrades» amb un compte que tinga tiquets emesos; visualitzar QR i verificar-lo amb l'API d'un professional autoritzat.
- En una activitat amb entrada gratuïta, consultar opcions, reservar-ne una i comprovar que apareix a «Entrades».
- En una activitat de pagament, comprovar que **no** apareix una falsa confirmació ni una emissió anticipada.
- Crear tauló, obrir missatges, denunciar, comprovar notificacions i ús de deep-links.
- Validar un codi correcte, un ja consumit i un codi inexistent; comprovar el rebuig amb comptes no autoritzats.

## Mancances conegudes que impedixen declarar la conversió completa

1. Paritat de l'algorisme exacte d'afinitat Android (geografia per comarques/radis, exclusions personals, pesos, suggeriments i històric) i de la posició comercial de tots els formats publicitaris.
2. Pagaments d'entrades amb Stripe PaymentSheet i/o Redsys: cal SDK i confirmació de l'estat de la comanda. El servidor actual crea PaymentIntents de Stripe; no s'ha desplegat Redsys.
3. Push APNs del servidor, registre i revocació de tokens de dispositiu, navegació al tocar un push.
4. Escàner QR per càmera, gestió PRO completa, edició de perfil amb fotografia, inici de sessió extern i eliminació de compte des de l'app.
5. Moderació i bloqueig d'usuaris consistent entre dispositius, totes les variants de batecs, llistes compartides, preferències i taulons avançats.
6. Validació real en iPhone (simulador i dispositiu), TestFlight, privacitat, accessibilitat i revisió App Store.

**Abast honest:** v340 augmenta la funcionalitat real iOS i reduïx fluxos ficticis, però **no és equivalent a Android ni apta per publicar sense les integracions i proves indicades**.
