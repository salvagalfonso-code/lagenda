# PAS 38/48 — Compra d'entrades amb la mateixa passarel·la del servidor (Stripe)

**Base acumulativa:** passos 1–37, sense generar ZIP en este pas.
**Referència inalterada:** Android v336 — `ServerApiRepository.createStripePayment`, `fetchStripePaymentStatus`, `AgendaState.finalizeStripePayment`, `TicketPurchaseDialog`.

## Implementació iOS

1. `IosAgendaApi.createTicketPayment`: `POST payments/create.php` amb `event_id`, `ticket_option_id`, `quantity`, bearer del compte congelat, validació d'opció de pagament i de la resposta (order_id, PaymentIntent secret, clau pública, import, moneda, entorn).
2. `AgendaKeychain.presentTicketPayment`: Stripe iOS **PaymentSheet natiu**, no un navegador extern ni simulació de pagament. S'usa la clau publicable retornada pel servidor, `merchant_name` i el secret específic d'eixa comanda. Només un full natiu simultani; 3D Secure i redireccions retornen a `agendavalenciana://stripe-redirect` via `StripeAPI.handleURLCallback`.
3. Dependència SwiftPM `stripe-ios-spm`, producte `StripePaymentSheet`, versió exacta 25.17.0 als fitxers `iosApp/project.yml` i `AgendaIOS.xcodeproj/project.pbxproj`. Es requerix Xcode amb resolució de paquets per a compilar.
4. Resultat `COMPLETED` del SDK **no acredita** la compra: es consulta `GET payments/status.php?order_id=...` fins que el servidor retornat confirma `paid` i entrega exactament les entrades identificades del mateix esdeveniment. Reintents només GET (màxim 10, cada 700 ms).
5. Només després d'eixa confirmació es recuperen les entrades reals del servidor, s'obre la secció Entrades i es demana **sense bloquejar la compra** el correu `POST mail/ticket-receipt-now.php` (mateixa ruta que Android).
6. Si l'usuari cancel·la, la passarel·la falla o el servidor encara té `payment_pending`: **cap entrada fictícia i cap segona comanda automàtica**. El diàleg permet consultar l'estat de la mateixa comanda i reprendre el mateix PaymentIntent després de consultar el servidor. La referència pendent es conserva només en RAM al llarg de la sessió (indexada per `userId,eventId`), fins i tot si l'usuari tanca i reobri el diàleg.
7. Les entrades gratuïtes conserven el circuit del pas 37, exclusivament per `tickets/purchase.php` i verificació de les entrades retornades. **Mai** s'envia eixa ruta per a opcions de pagament.
8. No s'alteren Android, SQL, PHP ni els arxius del servidor originals. No hi ha emmagatzematge permanent de dades de pagament a iPhone. No es guarda cap clau secreta de Stripe al client.

## Límits i comprovacions

- 24 assercions Kotlin/JVM del **codi real** de `IosPaidTicketPolicy` i `IosTicketCatalogPolicy` — PASS.
- Comprovació estàtica pas 38: 431 fitxers de l'arxiu original 1–36 byte a byte inalterats, inclòs TOT `app/` d'Android i complements PHP. Només han canviat els fitxers iOS i l'informe d'estat especificats — PASS.
- `swiftc -frontend -parse` dels fonts Swift — PASS, **no equivaleix a compilar l'app**.
- `plutil -lint` d'Info.plist i project.pbxproj — PASS.
- **No certificat:** build Kotlin/Native, resolució real de SwiftPM a Mac/Xcode, PaymentSheet en simulador/iPhone, claus i comptes Stripe, permisos de redirecció, respostes PHP en viu i compra final real; no s'ha accedit a MariaDB.
- **Limitació estructural del backend existent:** no hi ha recuperació certificada d'identificadors de comandes pendents després de reiniciar l'app. L'operació continua al servidor i les entrades pagades es carreguen de `tickets/mine.php`. Sense un endpoint de llistat de comandes, iOS no pot garantir recuperació d'ordres sense ID.
- Es reserva el pas 39 per a les entrades comprades, QR sense connexió i validació.

**Cap ZIP ara. Següent pas: 39/48. Després del 39, ZIP acumulatiu 13/16.**
