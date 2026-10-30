# L'Agenda v350 — auditoria de diferències de sistemes Android/iOS

Data de revisió: 29-10-2026. Base: carpeta de treball LAGENDA_V350_TREBALL, no només el ZIP anterior; Android `app/` i iOS `shared/src/commonMain/.../ios` + `iosApp/` + el bolcat SQL. Revisió estàtica de codi, no prova de l'API real, iPhone o compilació integral. Les funcions d'Android són verificades com a implementacions en el seu codi, no com a proves en dispositiu.

## Dèficits confirmats / riscos de paritat

| Sistema | Android (codi) | iOS (codi) | Tipus |
|---|---|---|---|
| Arquitectura | `AgendaState` + múltiples pantalles Android i repositoris | `AgendaIosScreen` + components independents, sense reutilitzar el `AgendaState` original | Implementació paral·lela, risc de divergència |
| Base local i sincronització | `AgendaLocalDatabase` (SQLite, revisions/deltas) + `AgendaSyncWorker` periòdic | `IosEventCatalog`, cache JSON, preview 50 i TTL 30 minuts; sense equivalent de WorkManager | Parcial, capacitats offline diferents |
| Acollida inicial | `FirstRunOnboarding` i elecció de perfils/categories | No hi ha flux equivalent invocat en l'arrancada d'iOS | Faltant |
| Seguiments en convidat | `toggleFollowArtist` guarda localment i `mergeGuestStatePayload` en iniciar sessió | `toggleFollowEntity` obri login quan `account == null` | Error funcional confirmat |
| Migració de convidat | Fusiona dades del convidat al compte | No hi ha migració equivalent de guardats/seguiments a l'inici de sessió | Faltant |
| Suggerències per defecte | `UserPreferences.showSuggestions = true`, cache default true | `showSuggestions` només true quan cache diu `"true"` | Error de paritat confirmat |
| Feed Per a tu | `AgendaState` amb pes de senyals, afinitat i snapshots, seguiments i descoberta | Algoritme `IosPersonalFeedPolicy` separat, no idèntic | Parcial, discrepàncies possibles |
| Gestió de categories, Seguits, cerca i presets | Motor Android complet | Existeixen implementacions v350, pendents de proves UI/API | Codi, no verificat |
| Batecs | Seguiments locals, estat de vistos, carrusel i fallback | Generació i carrusel previstos; depenen d'estat de seguiments, API i codi propi | Parcial, només tests de lògica |
| Popup normal/festival | `ConcertDetailSheet`, `FestivalPopup`, fluxos complets | `EventDetailSheet` + horaris; correcció del cartell no provada visualment | Parcial |
| Guardar des del popup | Selector de llistes en Android | `onSave` commuta general, no selector de llistes | Faltant |
| Comunitat dins del popup | Taulons contextuals i altres accions | Dos botons «Obrir Comunitat» i «Crear tauló», no llistat integrat | Parcial |
| Comentaris i continguts socials | `ConcertPost`, comentaris, comptadors i socials | Sense reproducció completa de totes les accions | Parcial |
| Festivals especials | Configuracions especials, serveis, productes, rols de personal, transports | Model resumit `AgendaEventSummary` i horari, no mateix gestor | Faltant/parcial |
| Imatges i compartir | Cartells, opcions visuals i `EventShareCard` | `shareText` natiu, sense targeta visual equivalent | Parcial |
| Entrades gratuïtes | Compra/reserva segons contracte | `reserveFreeTicket` present | Codi, falta prova backend |
| Entrades de pagament | Stripe PaymentSheet, verificació de comanda | UI declara explícitament que Stripe/Redsys no disponible | Faltant crític |
| QR/validació | Mostra/valida QR, pantalla professional | Swift té QR/escàner i crida `validateTicket`; no verificat en dispositiu | Parcial |
| Gestió d'entrades i PDF | Dashboard professional, PDF, facturació, gestió | Pantalla «Les meues entrades», sense dashboard complet | Faltant/parcial |
| Menú «+» | Accions de publicació/edició/anunci i gestió | Reprodueix els botons segons rol, però `IosPublisherPendingDestination` només mostra un avís de pendent | Faltant crític |
| Publicar/editar esdeveniments | Formularis i persistència completes | No hi ha editor/publishing natiu funcional | Faltant crític |
| Campanyes d'anuncis | Crear, editar, cobrar, imatges, control | Només mostra i comptabilitza publicitat activa; no campanyes des d'iOS | Faltant |
| Merch/donacions | Pantalles i APIs específiques | «Catàleg de productes encara no disponible a iOS»; menu + pendent | Faltant |
| Comunitat avançada | Grups, missatges, membres, moderació | Pantalles parcials i endpoints complementaris en carpetes sense prova de desplegament | Parcial |
| Notificacions in-app | Configuració, categories i lectura | Sis pestanyes i configuració parcial, sense garantir criteris exactes | Parcial |
| Push i campanetes | Android notificacions locals / preferències Android | UNUserNotificationCenter/APNs preparat, però endpoints i configuració no verificats en servidor | Blocatge de desplegament/proves |
| Hores de descans | Ajustos de quiet hours en Android | iOS explicita que no estan integrades | Faltant |
| Perfil i idioma | Selector de configuració d'idioma | iOS `onOpenLanguageSettings` invoca `showNotificationSettings()` | Error funcional confirmat |
| Idiomes | Android té lògica de localització | iOS informa que traducció completa pendent | Faltant |
| Login Apple/Google | Android té gestió de Google i email | Swift integra UI Apple/Google; exigeix `auth/apple-ios.php` i `google-ios.php` en servidor | No verificat servidor |
| Perfil i baixa del compte | Actualitzacions Android | iOS integra endpoints PHP auxiliars que cal desplegar/provar; la baixa fa servir `delete-ios.php` | No verificat servidor |
| Superadmin | Revisions, rols, denúncies, publicacions, notis i sol·licituds | `IosSuperAdminConsole` implementació parcial, no tot el circuit Android | Parcial |
| Altres funcions PRO | Eines de guanys, personal d'accés, permisos, productes | No migrades íntegrament | Faltant |
| Permisos/sessió | Lògica Android de sessió i fusió convidat | Keychain natiu i token, però restauració de sessió gestiona errors de xarxa com a cap compte temporalment | Risc |
| Plataforma i posada en producció | Android es pot compilar amb Gradle | El darrer log Codemagic disponible mostra error Kotlin antic; la correcció s'ha empaquetat, però no hi ha build completa amb èxit aportada | Sense verificació final |

## Aspectes que ja tenen implementació iOS (NO equival a verificat en iPhone)

- Navegació principal; cercador i filtres (Quan, On, Què, Preu); selector de modes, Categoria i Seguits.
- Preferències i categories/subcategories; localitats amb radis; alguns bloquejos i seguiments autenticats.
- Feed, publicitat de visualització obligada i rastreig de clics; Batecs; popup i horaris; guardat general/local.
- Guardats i llistes autenticades, entrades emeses/gratuïtes i QR; comunitat bàsica; notificacions in-app i locals.
- Perfil, foto, correu, registre, sessions Keychain, algunes eines superadmin i panell «+» (l'últim només com a menú).

## Tres errors objectius addicionals

1. Convidat no pot seguir artistes en iOS: `AgendaIosScreen.kt`, funció `toggleFollowEntity`, obri login. Android `AgendaState.kt`, `toggleFollowArtist`, guarda local i sincronitza posteriorment.
2. Idioma iOS mal connectat: `AgendaIosScreen.kt`, `onOpenLanguageSettings = { sessionBridge?.showNotificationSettings() }`.
3. Suggerències desactivades per defecte en iOS: `platformReadPublicCache(... "pref_suggestions" ...) == "true"`; Android `UserPreferences(showSuggestions=true)` i cache default true.

## Prioritat de tancament

- **P0 (bloqueig funcional):** menú + i fluxos professionals, pagament real, seguiments de convidats i fusió, proves de backend per APIs auxiliars.
- **P1 (paritat essencial):** popup integrat complet; Guardar amb selector; Comunitat contextual; horaris/festival especial, notificacions/APNs, idioma, suggerències per defecte i afinitats del feed, onboarding.
- **P2 (robustesa):** cache offline/delta, navegació de deep links, cartell compartible, traduccions, accessibilitat, simulador i proves multi-compte.

**Limitació important:** només s'ha analitzat el codi en fitxers facilitats; no hi ha verificació de què està desplegat realment a quimerafest.com, cap checkout real, ni execució de l'app després dels últims canvis. No s'han alterat els fitxers de l'aplicació durant esta auditoria. No s'ha generat ZIP ni s'ha incrementat versió.
