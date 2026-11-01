# Pas 46/48 · Donacions i serveis especials · L'Agenda iOS v349

## Implementat al codi acumulatiu

- Donacions a artistes: lectura pública `artist-store/donations/config.php`, formulari d'import fix/personalitzat, anonimat i missatge, Stripe PaymentSheet natiu, pagament amb `payment-create.php`, confirmació només amb `payment-status.php`. Sense donar una donació per feta per una resposta de Stripe.
- Gestió de donacions: artistes gestionats reconeguts en `professional-profiles/mine.php`, activació/desactivació, imports suggerits, text i imatge per `donations/save-config.php`; lectura de verificació posterior.
- Serveis especials: lectura pública `special-events/public.php`, serveis i productes actius, estoc, `reserve_only`, carros de diversos productes del mateix servei, preflight de disponibilitat i cobrament amb `payments/service-create.php` i `payments/service-status.php`. QR de servei mostrat únicament després de confirmació remota de comanda pagada.
- Gestió professional: `special-events/manage/my-events.php` i `config.php` amb control `can_edit_services`; crear, editar o eliminar serveis/productes amb els 4 endpoints existents `save-service`, `delete-service`, `save-product`, `delete-product`, verificació posterior del servidor.
- Bescanvi: escàner de càmera iOS separat del de tickets, inspecció amb `special-events/order-inspect.php`, confirmació explícita i bescanvi amb `special-events/order-redeem.php`. No existeix bescanvi sense connexió.
- Interfície: panell de publicació/professional, perfil d'artista, fitxa d'esdeveniment i visor QR. Cap dada de compres ni configuracions es desa de forma persistent al mòbil; intents pendents només en RAM.
- **Android v336 i servidor sense modificacions.** La base acumulativa són els 481 fitxers del ZIP 15/16 i es preserven tots.

## Proves executades en este entorn

1. `kotlinc` de la política Kotlin real i 32 asserts JVM: **PASS**.
2. `swiftc -frontend -parse AgendaKeychain.swift`: **PASS** (sintaxi, no compilació ni runtime).
3. `python COMPROVACIONS/comprovar_pas_46.py`: **PASS**, 481 fitxers de base preservats; 5 fitxers iOS modificats, cap Android ni servidor; 15 rutes PHP i punts d'interfície connectats.
4. Compilació KMP/Xcode **NO EXECUTADA**: el wrapper necessita accedir a `services.gradle.org`, DNS inaccessible.
5. Sense comprovacions reals contra PHP/MariaDB ni operacions Stripe d'iPhone.

## Limitacions pendents (no certificar com a verificat)

- Si l'usuari tanca completament l'app durant una compra, la referència disponible no té una API verificada d'índex de comandes per a restaurar-ne exactament l'intent: la prevenció de duplicats en RAM no cobreix esta situació. Cal resoldre-ho/validar-ho contra el backend abans de producció.
- Les comandes de serveis i els seus QR no estan conservats sense connexió ni sincronitzats a llarg termini: el pagament es comprova contra MariaDB. No confondre això amb les entrades de l'apartat 13, per a les quals existeix una excepció de cache xifrada.
- La inspecció per càmera necessita permisos en dispositiu real; compilació KMP i implementació Swift vinculada no verificades per l'absència de compilador iOS/Gradle en este entorn.
- No s'han validat anuncis de xarxa, codis de QR reals ni posat en marxa una configuració real de donacions en producció.

**Estat:** pas 46 incorporat al codi acumulatiu, verificació integral pendent. Següent pas 47: superadministració i permisos. ZIP 16/16 només després del pas 48.
