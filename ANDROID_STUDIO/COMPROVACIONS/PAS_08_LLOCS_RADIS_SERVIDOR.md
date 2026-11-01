# L'Agenda iOS · Pas 08/48 — Pobles, comarques i radis

## Referència immutable
- Android v336: `ServerApiRepository.fetchLocationPreferences()` / `saveLocationPreferences()`;
  servidor existent `api/preferences/locations.php`; font de 6.016 localitats
  `app/src/main/assets/agenda_territori_v277.json`.
- No s'han modificat cap codi d'Android v336 ni cap fitxer del servidor.
- La ruta de preferències utilitza `user_id` resolt per `Bearer`. El contracte
  actual identifica els pobles/comarques pel **nom**, no per ID territorial numèric.

## Implementació
- Afegit `IosLocationRules.kt`: contracte `towns: [{name, radius_km}]`,
  `comarques: [string]`; 5/15/30/50/100 km, 30 km per defecte; noms retallats,
  sense duplicats, màxim 100 per camp; conserva topònims remots heretats.
- `IosAgendaApi.saveLocationPreferences` valida abans de posar la mutació en la cua
  xifrada i utilitza l'endpoint real `preferences/locations.php`.
- Recepció POST: comprova `exists`, `revision > 0`, i **igualtat exacta** del
  conjunt de pobles, radis i comarques retornat per l'API. No elimina la mutació
  de la cua davant una resposta incompleta/diferent.
- `IosPendingServerQueue.hasPendingKind(ownerId,"locations")`: comprova la cua
  del mateix usuari abans d'hidratar una fotografia remota.
- `AgendaIosScreen`: guarda els llocs normalitzats al domini específic, evita
  restauracions remotes sobre edicions en vol, i manté l'espill tipat de
  `sync/state.php` emprat per Android sense guardar-ho en `UserDefaults`.
- `IosServerLocations.decode`: quan falta/és invàlid `radius_km`, usa 30 km,
  igual que el parser Android.
- `IosGeoPlaces.kt`: incorporats 3 nuclis que faltaven en la còpia iOS
  (`el Grau de Castelló`, `La Marmudella`, `Urbanització El Boqueró-Montpobla`),
  d'acord amb les dades originals d'Android. La llista té 6.016 registres.

## Comprovacions executades
- `comprovar_pas_01.py` a `comprovar_pas_08.py`: PASS.
- Kotlin CLI (JVM) de `IosLocationRules.kt` + `PersonalizationOptions.kt` amb
  8 casos: PASS.
- Kotlin CLI (JVM) de **l'autèntic** `IosGeoPlaces.kt` amb stub del normalitzador
  de cerca, proveïnt validació de topònims i selecció geogràfica: PASS.
- Paritat de **6.016** entrades del catàleg de llocs (nom, comarca i territori)
  respecte d'Android v336: PASS.
- 93 fitxers de la carpeta Android empaquetada en la v349 comparats byte a byte
  amb el ZIP iOS v349 original: intactes. Font v336 sense modificar.
- 5 tests commonTest (`IosLocationRulesTest.kt`) escrits però **no executats amb Gradle**.

## Límits
- Sense compilació Xcode/Kotlin/Native, sense proves en iPhone, sense
  comprovació HTTP real de `preferences/locations.php` ni MariaDB.
- El contracte territorial actual només identifica els llocs pel **nom**, i pot
  existir homonímia en nuclis de comarques diferents. Solucionar IDs de lloc
  requeriria un canvi de servidor/Android fora d'este pas; no s'ha fet.
- Este pas no certifica encara la igualtat completa de «Per a tu» entre plataformes;
  serà tractada al pas 10.

**No s'ha generat ZIP.** El següent correspon al pas 9, acumulant 1–9.
