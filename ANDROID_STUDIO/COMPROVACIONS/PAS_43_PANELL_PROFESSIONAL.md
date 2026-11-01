# Pas 43/48 — Panell professional: entrades, comandes i gestió de venda

**Estat: implementat en codi sobre el ZIP 14/16 (passos 1–42). NO compilat completament en iPhone.**

## Referència exacta Android v336

- `app/src/main/java/com/example/agendavalenciana/ui/ProfessionalTicketDashboard.kt`
- `app/src/main/java/com/example/agendavalenciana/ui/SellerTicketTypesEditor.kt`
- PHP consumit per Android: `professional-seller/dashboard.php` (GET company_id/period), `professional-seller/options.php` (GET/POST upsert/set_enabled), `professional-seller/assign-company.php` (POST), `professional-seller/invoices.php` (POST) i `events/ticket-drafts.php` (POST import).

## Implementació iOS (canvis acumulatius)

- `IosProfessionalSellerContract.kt` **nou**: models professionals tipats, imports exactes en cèntims, dates/aforament, verificació posterior de canvis i CSV sense fórmules.
- `IosProfessionalDashboardPanel.kt` **nou**: 5 pestanyes Resum, Vendes, Esdeveniments, Factures i Empreses, filtre fiscal/període, informe CSV compartible mitjançant pont natiu de iOS; edició de modalitats per dates, zones, contingent, preus, sessions, oferta, max_per_order; alta de referències de factures ja emeses externament.
- `IosAgendaApi.kt` **ampliat**: consulta tipada i autoritzada de panell, opcions, importació de modalitats, upsert, activació/desactivació de venda, assignació fiscal i registre extern de factura, amb `sellerActivationMutex` i reconsulta GET per verificar cadascuna de les mutacions. `professionalSetSaleEnabled` contempla activitats autoritzades d'un perfil professional reclamat, no només autoria directa; el PHP continua exercint els permisos.
- `IosPublisherPanel.kt` **ampliat**: l'acció `TICKETS` obri el panell nou; `ACTIVATE_SALES` conserva l'alta fiscal del pas 42. Enllaç de panell a alta de titular fiscal.

### Contractes i precaucions

- Les comandes mostrades són cobrades, no són totes les reserves ni s'han de confondre amb liquidacions. El total del panell pot incloure més comandes de les 100 últimes en pantalla.
- Les factures del panell són referències de documents emesos fora del sistema; la inscripció **no emet factura fiscal ni PDF**.
- La lectura econòmica necessita una sessió registrada i un resultat vàlid del servidor. Un rol local, un import estimat o una resposta buida no permeten afirmar cap venda.
- En `POST` no idempotents, una resposta incerta obliga a reconsultar el servidor. No repetir el POST automàticament.
- Els tipus d'entrada només poden modificar-se per qui figure amb permís de gestió al panell de servidor i tinga empresa fiscal assignada; les comprovacions definitives són de PHP.
- Les dades professionals es mantenen només en memòria RAM durant la pantalla; no s'han afegit preferències locals persistents.

## Proves executades

- Pas 43: política de producció compilada en Kotlin/JVM i provada: **59/59**.
- Regressions disponibles: pas 36 **36/36**, pas 40 **36/36**, pas 41 **39/39**, pas 42 **55/55**. Total aïllat **225 assercions superades**.
- Script nou `COMPROVACIONS/comprovar_pas_43.py`: comparació byte a byte contra el ZIP 14/16; 462 fitxers previs intactes; dos fitxers Kotlin iOS antics i el registre d'estat modificats. Android, backend i Swift originals preservats.
- Sintaxi de fitxers Swift originals: `swiftc -frontend -parse` correcte.

**NO verificat**: compilació completa Gradle/Kotlin-Native/Xcode, render real en iPhone, API professional en viu contra PHP/MariaDB, càrrecs i liquidacions reals. Falta Gradle iOS amb dependències en este entorn. Tampoc es certifica la compatibilitat dels camps de backend fins a una prova d'integració real.

**NO generar ZIP en pas 43.** Continuar amb pas 44: gestió de campanyes. Lliurar ZIP 15/16 acumulatiu en acabar pas 45.
