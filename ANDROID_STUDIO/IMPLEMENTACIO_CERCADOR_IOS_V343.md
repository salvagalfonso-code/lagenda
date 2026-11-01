# L’Agenda iOS v343 · Cercador amb suggeriments i filtres avançats

## Què s’ha incorporat al codi

- Suggeriments tipificats (grup/artista, població, comarca, organitzador, sala, estil, categoria i títol d’esdeveniment), ordenats per coincidència exacta, prefix i contingut, amb un màxim de 10 propostes. Sense inventar entitats que no existisquen al catàleg local de l’app.
- Cerca sense bloquejos: escriure modifica només l’esborrany; els suggeriments es processen amb un retard de 240 ms fora del fil visual; el filtratge global es dispara en confirmar «Cercar», tocar un suggeriment o aplicar els filtres. La cerca no invalida la selecció de «Per a tu» mentre s’escriu.
- La cerca confirmada consulta **tots els esdeveniments disponibles a la còpia de catàleg**, no només la selecció de «Per a tu». Quan el resultat és un festival i la cerca és d’un artista, el popup rep el nom de l’artista per obrir l’actuació coincident.
- Criteris AND: població exacta, comarca, radi des de la població (0, 5, 15, 30, 50, 100 km), categoria, estil/subcategoria, rang de dates (AAAA-MM-DD). Festivals que solapen el rang són elegibles; si no hi ha coordenades, no s’inventen distàncies.
- Cerca per tipus seleccionat: ARTIST busca només noms d’artistes o horaris de festivals; TOWN en municipis; COMARCA per comarca; ORGANIZER, VENUE, STYLE, CATEGORY i EVENT pels seus camps. Les coincidències ignoraran accents habituals.
- Filtre actiu visible, neteja completa i cap ampliació automàtica dels resultats orgànics fora dels criteris. **Excepció contractual preexistent:** l’apartat comercial continua incorporant els compromisos obligatoris, sense duplicar fitxes.
- Cerques guardades al servidor: poble i categoria. **Les altres opcions (radi, comarca, estil i dates) encara no es desen al servidor**; l’app ho indica explícitament.
- Les 91 fonts Android `app/src` continuen intactes. Cap canvi de base de dades o servidor de producció.

## Comprovacions fetes

- 16 proves independents amb el compilador Kotlin: suggeriments d’artista/comarca/poble/organitzador/sala, concerts i festivals amb artistes en horaris, accents, combinació de filtres, radis, solapament de dates i validació de criteris.
- Test unitari `IosAdvancedSearchTest.kt` inclòs a `shared/src/commonTest` per executar-lo després amb Gradle.
- Validació d’integritat de ZIP i comprovació que els fitxers originals d’Android continuen sense canvis.

## No verificat / encara pendent

- Compilació completa del mòdul Kotlin Multiplatform i de l’app amb Xcode; proves al simulador i en iPhone real.
- Resultats d’artistes quan un festival no porta el seu horari al catàleg inicial: la consulta del detall del festival s’ha mantingut, però l’índex general de cerca no pot incloure una actuació no present al catàleg sense una API de cerca global al servidor.
- Suggeriments d’entitats que no apareixen a cap esdeveniment local (coeditors, catàleg complet de professionals/sales): necessita ampliar l’API/índex general.
- Sincronització entre dispositius de TOTS els camps dels filtres avançats; l’API actual només exposa nom, poble i categories.
- Proves d’accessibilitat de controls, teclat i rendiment real amb catàlegs grans en iPhone.

## Compilació al Mac

A `ANDROID_STUDIO` executar `sh COMPROVACIONS/compilar_ios_mac.sh`. Després executar tests `./gradlew :shared:iosSimulatorArm64Test` des d’un Mac Apple Silicon o `:shared:iosX64Test` en un Mac Intel. El projecte encara no s’ha compilat conjuntament en Xcode en este entorn.
