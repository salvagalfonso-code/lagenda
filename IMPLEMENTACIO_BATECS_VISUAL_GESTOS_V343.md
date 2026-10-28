# L’Agenda v343 — Batecs: visual, gestos i agrupació (iOS)

## Punt de partida

Base exacta: `LAGENDA_V343_KMP_IOS_COMUNITAT_BLOQUEJOS_MODERACIO_ACUMULADA.zip`.
Conserva `ANDROID_STUDIO`, `SERVIDOR_APNS` i `SERVIDOR_COMUNITAT` (sense canvis al servidor).
No s’han modificat fitxers `ANDROID_STUDIO/app/src` d’Android.

## Implementat en codi

1. **Un cercle per artista seguit**, agrupant tots els `story_key` de les seues publicacions. Evita que sis publicacions d’un sol artista simulen sis artistes seguits.
2. **Mínim sis artistes diferents** per a mostrar el carrusel: no afegix perfils ficticis ni artistes no seguits.
3. **Estat de vist per grup**: el cercle i el nom queden grisos només quan totes les publicacions reals de l’artista estan marcades com a vistes pel servidor; la consulta és per compte.
4. **Carrusel horitzontal LazyRow**, amb desplaçament tàctil, cercle de 56 dp i distintiu de recompte quan l’artista té més d’una publicació.
5. **En tancar el visor**, tant el carrusel com la llista del feed tornen al primer element, sense alterar els seguiments ni reordenar els continguts.
6. **Visor vertical** amb progressió visual, imatge de fons i capa fosca, avatar, títol d’artista i actuacions futures del catàleg.
7. **Gestos**: tocar els extrems esquerre i dret, arrossegar horitzontalment; els concerts mantenen scroll vertical i una acció per obrir la fitxa.
8. **Avanç automàtic cada 15 s**; anterior al primer reinicia eixe Batec, següent a l’últim tanca el visor.
9. **Marcat de vistos** de totes les publicacions del grup en treball de la pantalla, resistent als canvis ràpids de Batec. No pinta vist abans de rebre èxit de l’API.
10. **Paginació acotada de l’API**: si les primeres 50 publicacions no contenen sis artistes seguits distints, l’iPhone busca fins a quatre pàgines de 50 registres, mentre el servidor indique `has_more`.
11. **Sessió restaurada**: després de recuperar els seguiments del compte, torna a consultar el catàleg de Batecs per evitar el resultat de visitant sense autenticació.

## Comprovacions locals

- `COMPROVACIONS/ProvarBatecsArtistesV343.kt`: 16 comprovacions superades via `kotlinc` i `java`, agrupació, mínim sis artistes, duplicats, vistos, ordre, seguiments i límits de navegació.
- Fonts originals `ANDROID_STUDIO/app/src`: 91 fitxers comparats byte a byte amb el ZIP base, tots idèntics.
- Swift: comprovació de sintaxi amb `swiftc -frontend -parse` dels fitxers existents, sense errors.
- Integritat del ZIP: verificada amb `ZipFile.testzip()`.

## Pendent de verificar, sense Xcode ni servidor de proves

- **No hi ha compilació Xcode** ni execució al simulador o iPhone. Les noves pantalles Compose no s’han pogut compilar per als targets iOS en este entorn.
- Proves reals de tocs laterals / gest de lliscament mentre es desplaça la llista vertical de concerts, temporitzador i rendiment de la imatge a iPhone.
- Retorn al començament de la llista del feed implementat. La validació tàctil d’este salt queda pendent en iPhone.
- Fotos d’artista diferenciades de cartells, escala d’accessibilitat i VoiceOver.
- L'API només retorna les dades que existixen: cap imatge o fitxa inventada. La paginació es limita a 200 registres; si no hi ha sis artistes seguits reals en eixe conjunt, el carrusel es manté ocult.
- La paritat visual exacta només es podrà contrastar amb captures de l’app en un simulador.

## Altres blocs del projecte pendents

`Sense filtre` (fidelitat visual i càrrega), entrades de pagament, notificacions/APNs desplegat i provat, perfil i login Apple/Google/eliminació, AgendaPRO, servidor Comunitat desplegat, verificació Xcode/TestFlight.
