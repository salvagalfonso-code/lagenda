# L’Agenda iOS · Publicitat · v343 acumulada

Data: 28/10/2026. Base: `LAGENDA_V343_KMP_IOS_PER_A_TU_AFINITATS_CERQUES_ACUMULADA.zip`.

## Implementat en codi iOS

- Els anuncis actius es descarreguen de `ads/active-global.php`, independentment del compte, preferències, cerca, distància i categories. El servidor selecciona només campanyes pagades, vigents i esdeveniments publicats.
- Els IDs garantits incorporen `priority_event_ids`, `platform_ticket_event_ids`, `paid_ticket_event_ids`, les fitxes publicitàries i les fitxes prioritàries. No es deduïx l'activació de la venda interna de l'existència de compres.
- Carrusel de capçalera `HEADER` independent de la llista d'activitats, fins i tot si no hi ha resultats orgànics.
- Carrusels `FEED_PREMIUM` i `FEED_NORMAL`, amb política **compartida amb Android** `GlobalAdPlacementPolicy`: premium a la posició visual 6 i normal a la 15 de cada bloc de 20. Quan un tipus d'anunci falta, no es desplaça l'espai contractat de l'altre. La col·locació depén de l'índex absolut del feed, no de la primera pàgina de 5/15/10.
- En una cua de feed més curta, els espais pendents es col·loquen després de l'última activitat real, mai abans de la primera per culpa de la paginació.
- Totes les campanyes de cada espai són accessibles en un carrusel horitzontal; l'anunci inicial es rota cada període de 30 minuts, sense alterar la posició del carrusel.
- Creativitat de l'anunci amb el punt focal `card_focus_x/y` del servidor; recurs al cartell de l'esdeveniment quan no hi ha creativitat específica.
- Registre de la primera impressió visible de cada campanya per sessió i dels clics, amb l'API pública `ads/track.php`; errors d'analítica no bloquegen el feed.
- `POPUP`: una visualització per dia, només quan la fitxa de l'esdeveniment està disponible i la persona és a Inici; no es mostra en altres seccions. El clic obri la fitxa real.
- Recàrrega discreta de l'inventari de campanyes cada 120 segons mentre es visualitza Inici; no obliga a recalcular la caché orgànica de 30 minuts.
- Les targetes orgàniques continuen sent úniques per ocurrència (`event.key`), i el carrusel comercial és un espai diferent de la fitxa orgànica.
- Les exclusions personals no oculten els esdeveniments comercialment garantits, ni els anuncis contractats. El mateix és aplicable a cerques i `Sense filtre` / `Per a tu`.
- Codi Android intacte i sense canvis del servidor en este paquet.

## Verificació executada

- Compilació independent de `GlobalAdPlacementPolicy.kt` amb `kotlinc` i execució de les comprovacions visuals en `COMPROVACIONS/provar_publicitat_ios_v343.kt`: **OK**.
- Nova prova de regressió en `shared/src/commonTest/.../IosAdvertisingParityTest.kt` (preparada per a Gradle/Xcode).
- Nova prova de contracte JSON publicitari `IosAdResponseContractTest.kt` (pendent de Gradle).
- Fitxers Swift: validació `swiftc -frontend -parse` **OK**.
- `Info.plist` interpretat i integració Xcode–Gradle verificada a nivell estructural.
- Comparació byte a byte: tots els 93 fitxers de `app/` coincidixen amb la base anterior.
- ZIP verificat amb `zipfile.testzip`.

## Encara pendent

- **Compilació conjunta** de Kotlin Multiplatform + Compose iOS + Xcode, que no podem executar en l'entorn actual. Les proves de compilació de les noves pantalles i del parser amb Gradle estan preparades però no executades.
- Provar sobre un simulador i un iPhone real la posició, els gestos del carrusel, imatges recortades, popup, analítica i resposta de l'API del servidor desplegat.
- Verificar amb campanyes reals en la BD que es registren impressions i clics i que no s'ha instal·lat una versió antiga de `ads/active-global.php`.
- Revisió de condicions comercials avançades (freqüència publicitària i criteris de rotació si el producte exigix més que el carrusel contractat d'Android). No s'ha implementat un auto-scroll temporal dels carrusels, que Android tampoc té.
- La resta d'apartats de paritat Android–iOS continuen segons l'inventari de la v343 (pagaments, push APNs, AgendaPRO, etc.).

## Comprovar en Mac

Des de `ANDROID_STUDIO/`:

```sh
sh COMPROVACIONS/compilar_ios_mac.sh
```

Proves Kotlin/Gradle, quan s'hagen resolt les dependències:

```sh
./gradlew :shared:allTests
```

No es modifica l'API de producció ni la base de dades amb este ZIP.
