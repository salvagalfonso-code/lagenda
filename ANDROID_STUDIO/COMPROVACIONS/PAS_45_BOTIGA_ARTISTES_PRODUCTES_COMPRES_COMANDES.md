# PAS 45/48 — Botiga d'artistes, productes, compres i comandes

**Treball sobre iOS v349 acumulatiu 1–44. Android v336, Swift i servidor de referència NO modificats.**

## Flux implementat en codi

- Perfil d'artista: catàleg públic `artist-store/catalog.php` (ja llegible al pas 27), etiqueta d'estoc i botó de compra per als productes actius.
- Compra: usuari registrat, quantitat, nom de destinatari, domicili, CP, ciutat, país; preu orientatiu abans de la sol·licitud i preu exacte del servidor en el PaymentIntent.
- `artist-store/payment-create.php` seguit de Stripe PaymentSheet existent de la integració dels passos 38/44. 3DS es gestiona al component natiu; un `COMPLETED` de Stripe no és prova de cobrament.
- `artist-store/payment-status.php`: només `paid` del servidor i l'ID de comanda esperat permeten declarar compra confirmada. Reprendre el mateix PaymentIntent en RAM en lloc de crear una comanda diferent.
- Gestió per artistes verificats en `professional-profiles/mine.php` (`managed_artists`): `save-product.php`, `delete-product.php`, `manage/orders.php`, `manage/mark-shipped.php`.
- Preus i ports en **cèntims**, estoc explícit/nul (il·limitat), tipus `DISC` i `MERCH`; comprovació posterior GET de cadascuna de les mutacions del catàleg i de l'estat d'enviament.
- Dades de compra i enviament únicament en RAM i al PHP/MariaDB. Sense nova persistència local d'estat personal. Identitat, JWT/Bearer i operacions bloquejades per canvi de compte.
- Donacions NO habilitades en este pas: pertanyen al pas 46.

## Resultats de proves disponibles

- `COMPROVACIONS/provar_pas_45.kt`: **65 assercions Kotlin/JVM PASS** sobre la política real, imports, estoc, domini d'artista, dades d'enviament, pagament, comprovants i enviament.
- Regressions executades: pas 36 (36), pas 40 (36), pas 41 (39), pas 42 (55), pas 43 (59), pas 44 (66). Total **356 comprovacions Kotlin/JVM** incloent el pas 45.
- `COMPROVACIONS/comprovar_pas_45.py`: comprovació de tots els originals contra ZIP 14/16, inclosa immutabilitat byte a byte d'Android i del servidor; validació de presència i cablejat dels endpoints.
- Sintaxi Swift del codi natiu: `swiftc -frontend -parse` PASS. Cap nou fitxer Swift necessari; reutilitza PaymentSheet.

## Limitacions i riscos pendents

- **No s'ha compilat tota l'app KMP amb Xcode** ni s'ha verificat en un iPhone.
- **No s'han fet cobraments Stripe reals, ni peticions PHP/MariaDB de prova** (no hi ha claus ni base de dades de test connectades a l'entorn).
- El servidor actual no exposa en la referència Android una llista de les compres del comprador recuperable després de reiniciar l'app: si l'app es tanca completament amb una comanda sense acabar, la seua recuperació directa per ID no està assegurada, encara que l'estat de cobrament es manté al servidor. Abans del llançament convé provar i resoldre este cas al backend sense duplicar cobraments.
- Els permisos exactes de reclamacions de perfils i el formulari de gestió requereixen prova d'integració real.
- Una resposta perduda en operació de creació no idempotent bloqueja reintents automàtics; cal consulta i acceptació explícita del risc de duplicitat.

## Següent

Pas 46: donacions i serveis especials. Lliurament ZIP 15/16 en acabar este pas 45; ZIP 16/16 després del 48.
