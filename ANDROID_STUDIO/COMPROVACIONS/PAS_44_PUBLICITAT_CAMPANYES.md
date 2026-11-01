# Pas 44/48 · Creació, contractació i gestió de campanyes publicitàries

**Implementació en codi sobre l'acumulatiu 1–43, sense crear encara el ZIP 15/16.**

## Referència Android v336

- `app/src/main/java/com/example/agendavalenciana/ui/AgendaApp.kt`: `AdvertisementPublisherScreen`, Stripe PaymentSheet, ubicacions HEADER/FEED_PREMIUM/FEED_NORMAL/POPUP i duracions de 1, 3 i 7 dies.
- `app/src/main/java/com/example/agendavalenciana/data/ServerApiRepository.kt`: `ads/create-payment.php`, `ads/status.php`, `ads/mine.php`, `ads/update.php`, `ads/visual.php`, `images/upload.php` amb propietari `AD`, `ads/visual-map.php`.
- El client iOS ja llegia les campanyes globals actives (`ads/active-global.php`) amb les mateixes normes de visibilitat del feed; el pas 44 afegeix la **gestió** de campanyes.

## Codi afegit

- `IosAdvertisingPolicy.kt`: models del contracte, comprovació de quatre espais, tres duracions, segmentació, dades de Stripe emeses pel PHP, estat actiu contrastat amb dos GET i verificació exacta d'edició i creativitat.
- `IosAgendaApi.kt`: consultes autoritzades `ads/mine.php`, creació amb verificació d'autoria o permís de perfil professional, consulta de l'estat, comprovació estricta d'activació, edició de segmentació, pujada de JPEG `images/upload.php` i de composició `ads/visual.php`, reconsulta posterior; protecció contra nova comanda si n'hi ha una de pendent al servidor o en RAM.
- `IosAdvertisingPanel.kt`: selecció d'esdeveniments propis i de perfils gestionats, espai, duració i segmentació; llista d'anuncis amb estats, import, imatge, dates; edició de creativitat i focus i eliminació de la imatge; revisió d'estat; ús del pont Stripe PaymentSheet ja existent.
- `IosPublisherPanel.kt`: `CREATE_AD` obri el panell real en lloc d'un text de funcionalitat no implementada. Els convidats veuen accés a identificació i **no** poden iniciar pagaments. La publicació convidada continua operativa i no es confon amb el pagament.

## Regles de seguretat i límits

- **Stripe no activa campanyes:** ni tan sols `PaymentSheet.COMPLETED`. Només `ads/status.php` amb estat `active` i `ads/mine.php` amb la mateixa campanya activa autoritzen indicar-ho.
- El `POST ads/create-payment.php` no es repeteix automàticament. Els pagaments pendents es mantenen només en RAM de la instància API i poden reprendre's amb el mateix `client_secret` en la mateixa sessió. Si la sessió es perd, es poden veure els pendents en `ads/mine.php`, però no es pot recuperar un secret no retornat pel servidor. No s'inventa un altre pagament.
- El servidor calcula el preu; els imports del client es representen en cèntims i no equivalen a factura o liquidació. Mai es guarda un secret Stripe ni dades de campanya permanentment en el dispositiu.
- La pujada de foto i els canvis de segmentació/composició es reconsulten. Si una resposta queda en dubte, es mostra un estat incert i no es reenvia el POST automàticament.
- No es modifica cap fitxer de l'aplicació Android v336, Swift original ni backend.

## Comprovacions

- 66 assercions Kotlin/JVM del **pas 44** sobre la política real: entrades vàlides/invàlides, estats de pagament, activació exigint dues fonts, segmentació, enfocament i imatge.
- Regressions disponibles dels passos 36 (36), 40 (36), 41 (39), 42 (55) i 43 (59): total 225 assercions; total amb 44, **291**.
- `COMPROVACIONS/comprovar_pas_44.py`: comparació byte a byte dels 465 fitxers del ZIP 14/16; els 462 no declarats com a modificats continuen idèntics iOS/Android/Swift/backend originals preservats.
- **No és una compilació completa** de Kotlin Multiplatform o Xcode, no acredita cap compra real, ni respostes reals de PHP/MariaDB/Stripe; eixes comprovacions continuen pendents.

**Següent:** pas 45/48 botiga d'artistes, productes, compres i gestió de comandes. Després del 45, crear i comprovar el **ZIP 15/16** acumulatiu 1–45.
