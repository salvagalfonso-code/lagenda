# Pas 37/48 · Entrades: tipus, dates, quantitats i zones

## Font de referència
Android v336 (original inclòs sense alteracions): `model/Models.kt` `TicketOption`,
`data/ServerApiRepository.kt` `fetchTicketOptions` i `ui/ConcertDetailSheet.kt` `TicketPurchaseDialog`.
API: `tickets/options.php?event_id=<id>` amb `options`, `capacity`, `session_date`, `session_time`,
`sector`, `offer`, `price`, `capacity`, `sold`, `reserved`, `max_per_order`, `sale_starts_at`, `sale_ends_at`, `active`.

## Implementat en codi iOS (pas 37)
- Nou contracte `IosTicketCatalogPolicy.kt`: modalitats identificades per ID remot, data i hora de sessió,
  zona, etiqueta d'oferta, preu, aforament, vendudes, reservades, límit configurat fins a 20, estat actiu
  i períodes de venda. `available = max(0, capacity - sold - reserved)` sense inventar estoc si és desconegut.
- `IosAgendaApi.ticketCatalog(eventId)` consulta l'API actual amb GET, valida identificador,
  preu i camps; l'API continua sent la font de veritat. No converteix un preu invàlid en 0 €.
- Pestanya Entrades del popup: dades de modalitats reals, dates del festival, zones,
  preus, places, oferta, màxim per comanda, períodes declarats. No mostrar com disponibles
  modalitats `active=false`, caduques per data de sessió o esgotades per contingent.
- Diàleg de selecció: filtres combinables data/zona, opció real obligatòria, quantitat
  limitada pel menor entre `max_per_order`, contingent i el màxim de **6** de `purchaseTickets`
  de l'Android v336 (el backend s'ha de confirmar abans d'ampliar-lo); opcions esgotades deshabilitades,
  error i reconsulta del servidor. Scroll quan hi ha moltes modalitats.
- Reserva gratuïta existent protegida: reconsulta l'opció abans del POST de compra i
  no emet si no hi ha opció gratuïta, activa i amb stock. Credencial congelada abans del POST,
  comprovació de tickets emesos després de la resposta i sense reintent automàtic.
  No hi ha comandes pagades simulades.
- Dades i selecció en memòria de pantalla; cap nova persistència local.

## Limitacions
- El pas 38 completarà pagaments utilitzant el sistema del servidor. Les entrades de pagament
  són visibles però no es poden comprar encara en iOS. L'estoc real i les finestres horàries
  de venda es validen definitivament al servidor. La pantalla mostra les finestres de venda
  sense afirmar que l'hora concreta estiga oberta (encara no comprovada pel client).
- El pas 39 completarà les entrades emeses, QR offline i validació amb paritat Android.
- Sense compilació completa Xcode/Kotlin Native ni proves en iPhone o MariaDB real.
- Android original i servidor sense modificar. **No ZIP** fins acabar el pas 39.

## Verificacions
- `kotlinc` del mòdul de política real + `COMPROVACIONS/provar_pas_37.kt`: 28 assertions PASS.
- `COMPROVACIONS/comprovar_pas_37.py`: contracte API, UI, regressions i comparació immutable
  amb el ZIP adjunt de passos 1–36.
- Compilació total no disponible: `gradlew` intenta descarregar Gradle de `services.gradle.org`,
  sense accés de xarxa; entorn Linux sense Xcode.
