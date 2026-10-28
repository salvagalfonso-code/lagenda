# L’Agenda v343 · «Sense filtre», targetes i càrrega progressiva d’iOS

**Base exacta:** `LAGENDA_V343_KMP_IOS_BATECS_VISUAL_GESTOS_ACUMULADA.zip`.

## Canvis implementats

1. **Targeta compacta del feed:** cartell de 76 × 96 dp amb marc discret de disciplina; títol en una sola línia; subtítol d’un màxim d’una línia; població i sala; hora i estat/preu de venda; categoria, data `DD mes`, guardat, recompte de guardats que envia el servidor i menú d’incidències/exclusions. Tocant la targeta s’obri el popup original; els altres usos de `EventCard` fora del feed no s’han modificat.
2. **Festivals:** la previsualització compacta presenta les actuacions de la jornada pertinent, sense mesclar dies; en un festival començat es prioritza la següent data programada, i sense programa futur explícit es mostra «En curs». L’ID de l’esdeveniment no es modifica. La consulta detallada de l’horari continua dins del popup.
3. **Càrrega progressiva:** primer lot de 5 activitats; segon, fins a 20; després increments de 10. La sentinella de `LazyColumn` s’activa en arribar al final del lot, sense botó de «Mostrar més», ni roda de càrrega. Ja no s’estima la posició del scroll comptant files, que fallava en presència de HEADER i anuncis dins del feed.
4. **Publicitat conservada:** HEADER separat, Premium i Normal en les posicions de l’inventari complet, i espais de final de bloc només quan s’acaba l’últim lot; els continguts comercials obligatoris mantenen les excepcions de visibilitat i no es dupliquen en «Sense filtre».
5. **Caché de primera pantalla:** els refrescos guarden una vista reduïda de 50 activitats, que es pot descodificar i mostrar ràpidament en la següent obertura. La càrrega completa es descodifica en `Dispatchers.Default` en segon pla, conservant el JSON íntegre i els 30 minuts de vigència. En una instal·lació nova encara es necessita baixar el catàleg, perquè el servidor no oferix una API paginada pública en este projecte.
6. **Canvi de mode / cerca:** el feed torna a l’inici quan canvia el conjunt real de resultats. Les cerques explícites continuen sent globals, excepte les excepcions comercials obligatòries.

## Comprovacions realitzades

- 21 comprovacions independents de la política Kotlin: lots, límits, dates, festivals i tipus d’entrades. Vegeu `COMPROVACIONS/provar_sense_filtre_v343.kt`.
- No s’ha modificat cap fitxer del mòdul `app/` d’Android ni de `SERVIDOR_*` respecte al ZIP base.
- L’anàlisi de sintaxi no ha indicat errors de parseig al fitxer Compose; la compilació aïllada no es pot completar sense les dependències Compose Multiplatform.
- **No s’ha compilat amb Xcode ni provat en simulador ni en iPhone real.** Això és implementació en codi, no validació d’execució.

## Funcions de paritat exacta encara pendents

- Resolució d’imatge de la targeta segons prioritat de cartell, foto d’artista, sala i població: l’API `events/index.php` només proporciona el camp `poster_url` en les dades bàsiques i Android utilitza fonts visuals addicionals.
- Color exacte de totes les categories i variacions de tipografia/espaiats segons dispositiu.
- Reposició de la posició de lectura per una hora i sincronització de lectura; el mode/cerca reinicia la posició per evitar mostrar files de resultats antics.
- Inicialització des d’un índex SQLite local com fa Android: iOS usa JSON en caché. Primer inici després d’instal·lar-se sense dades requerix descarregar el catàleg complet.
- Compilació i prova de disposició, VoiceOver, gestos, imatges, memòria i estabilitat en iPhone.

## Resta de blocs generals oberts

Pagaments reals, APNs amb credencials i desplegament del servidor, fotografia/autenticació Apple-Google/eliminació de compte, AgendaPRO i Superadmin, desplegament de moderació Comunitat, validació conjunta Xcode i TestFlight.
