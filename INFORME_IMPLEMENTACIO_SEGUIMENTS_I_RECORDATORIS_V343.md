# L’Agenda iOS · Continuació acumulada sobre v343

Data: 28/10/2026. Base: `LAGENDA_V343_KMP_IOS_PERSONALITZACIO_TERRITORIAL_ACUMULADA.zip`.

## Funcionalitats implementades en codi en esta passada (NO verificades amb Xcode)

1. **Seguiment d'organitzadors** des de la pestanya Info dels esdeveniments, enviant l'acció autentificada a `user-state/follow.php` amb `entity_type=ORGANIZER`.
2. **Seguiment de sales/espais** des d'Ubicació, amb `entity_type=VENUE`.
3. **Seguiment de categories** des d'Info, amb `entity_type=CATEGORY`.
4. **Gestió dels seguiments al perfil:** es poden veure artistes, sales, categories i organitzadors seguits i deixar-los de seguir. El canvi local sols es confirma després d'una resposta satisfactòria del servidor.
5. **Recordatoris actius en Notificacions:** cartell, data i hora local guardada, campaneta de desactivació i popup de confirmació, evitant tindre dos diàlegs superposats.
6. **Paral·lelització de dades del compte:** seguiments, bústia, entrades, Comunitat, batecs vistos, llistes i cerques guardades s'obtenen de manera concurrent. Les consultes opcionals fallides conserven la dada anterior i es comprova l'ID del compte abans de publicar resultats.
7. **Protecció contra peticions de seguiment repetides** mentre se n'està processant una i desduplicació per tipus i entitat; entitats de tipus diferent amb el mateix nom no se sobreescriuen.

## Proves realitzades

- `IosFollowMutation.kt` compilat independentment amb `kotlinc`, usant un doble del tipus de dada de seguiment, i **5 comprovacions executades correctament**.
- `swiftc -frontend -parse` sobre els Swift de `iosApp/AgendaIOS`: **PASS**.
- Comparació SHA256 dels fitxers `app/src` amb el ZIP base: **91 de 91 idèntics**.
- No s'han executat proves integrades del mòdul Compose, Gradle, Kotlin/Native o Xcode; no es pot garantir compilació iOS.
- No s'han fet canvis al servidor de producció ni proves de transaccions contra l'API en viu.

## Estat de completitud

- **COMPLET i verificat en iPhone: 0 apartats.** Continua sense haver-hi compilació o prova de simulador en este entorn.
- **Implementat al codi, pendent de prova:** els 7 punts anteriors. El flux de seguir/desseguir requerix confirmació real de resposta del servidor, i el panell de recordatoris requerix prova visual.
- **Parcial:** paritat precisa de les metadades culturals dels seguits (iOS encara deriva estils dels esdeveniments), autenticació externa, cercador i popups íntegres, Comunitat i AgendaPRO.
- **Pendent de serveis/credencials i integració:** pagaments interns de pagament, push APNs de servidor, eliminació de compte i fluxs exigits per App Store.

## Comprovació en Mac

`cd ANDROID_STUDIO && sh COMPROVACIONS/compilar_ios_mac.sh`

La versió segueix sent **v343**, amb els canvis acumulats.
