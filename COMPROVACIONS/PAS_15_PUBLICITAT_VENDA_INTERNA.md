# Pas 15/48 — Publicitat global i venda interna prioritària

## Base immutable

- Referència: `LAGENDA_V336_ANDROID_STUDIO_v336_COMPILACIO_CORREGIDA(3).zip`.
- Còpia acumulativa iOS v349, passos 1–14 conservats.
- **Cap canvi** a Android v336, Android encastat a v349 o als fitxers PHP originals.

## Funcions implementades

1. `IosAgendaApi.promoted()` llig `ads/active-global.php` (public, global), amb `ads/active.php` només com a **fallback** de compatibilitat si la consulta global falla, igual que Android v336.
2. Sol·licita `ads/visual-map.php` i mescla *únicament* visuals corresponents als anuncis actius retornats; conserva el fotograma `card_focus_x/y` i la creativitat pròpia. La fallada del mapa opcional no anul·la el catàleg d'anuncis.
3. Contracte `IosCommercialVisibility`: un esdeveniment queda protegit si està en els IDs de campanya activa / prioritats comercials del servidor o té venda **interna** habilitada (`ticketAvailable` i `ticketUrl` buit). Els enllaços a taquilles externes no obtenen una garantia comercial pel simple fet de tindre entrades.
4. Visibilitat comercial protegida també contra exclusions personals i contra la deduplicació difusa en «Sense filtre». No s'inventen fitxes quan només hi ha un ID comercial sense dades completes.
5. S'han preservat les quatre ubicacions publicitàries: `HEADER`, `FEED_PREMIUM`, `FEED_NORMAL`, `POPUP`. El codi `GlobalAdPlacementPolicy.kt` d'iOS és **idèntic byte a byte** al de la referència Android v336 (posicions visuals 6 i 15 per cada 20).
6. Es manté `ads/track.php` per impressions i clics sobre creativitats visibles, amb `ad_id`, `placement` i `action`; no s'inventa cap campanya ni cap compra.
7. La consulta d'inventari comercial es refresca separadament del feed personalitzat, el qual pot romandre 30 minuts en cache.

## Validacions

- Scripts de regressió `comprovar_pas_01.py` … `comprovar_pas_15.py`.
- Compilació aïllada Kotlin/JVM amb **18 escenaris** del codi de producció `IosCommercialVisibility.kt` + `IosUnfilteredAgenda.kt` i dobles mínims de dependències.
- Sintaxi Swift amb `swiftc -frontend -parse`.
- SHA/contingut Android v336 i Android encastat a iOS v349 contrastats amb els ZIP originals.
- ZIP complet acumulatiu, revisat amb `ZipFile.testzip()` i contingut necessari.

## Limitacions verificables

- **Sense compilació completa** Kotlin/Native/Xcode en aquest entorn: Gradle requereix accés a `services.gradle.org` no disponible (DNS). Les 18 comprovacions JVM no substitueixen un build iPhone ni proves de campanya contra MariaDB.
- El fallback `ads/active.php` retorna anuncis però no les fitxes `priority_events` ni la relació completa d'esdeveniments amb venda interna; per això la garantia completa depén de `active-global.php` en producció. No s'ha fet cap crida a producció.
- En la referència Android v336, `AgendaApp.kt` **amaga espais d'anuncis quan la cerca textual no està buida**, malgrat el comentari de publicitat global. iOS conserva la norma editorial acordada de mostrar campanyes sempre, també en cerca, perquè Android s'ha de mantindre immutable. Això és una diferència de paritat real fins que es resolga, no una equivalència exacta.
- `AgendaReservationPolicy` d'Android comenta que una venda interna no hauria de donar prioritat en «Per a tu», mentre que el PHP `ads/active-global.php` sí retorna tots els IDs de venda interna. S'ha seguit la regla explícita de L'Agenda i l'API pública; no es pot afirmar paritat total de les dues regles d'Android.
- No s'ha implementat contractació/compra d'anuncis (pas 44) ni pagaments d'entrades (pas 38–39); el pas 15 és visibilitat pública i prioritat.
