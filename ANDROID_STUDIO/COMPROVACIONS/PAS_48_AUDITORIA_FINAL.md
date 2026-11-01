# L’Agenda iOS v349 — PAS 48/48 · Auditoria final Android ↔ iOS

**Data:** 01/11/2026  
**Paquet:** ZIP 16/16 · acumulatiu passos 01–48  
**Abast real:** revisió de codi, regles de negoci, connexions declarades, integritat, sintaxi Swift i proves Kotlin/JVM de polítiques reals aïllades. **No s'ha compilat ni executat iOS en iPhone/Xcode, ni s'han provat respostes de PHP/MariaDB, Stripe o APNs en viu.**

## 1. Resultat verificat

- **Integritat:** els 481 fitxers del ZIP 15/16 continuen presents. Les modificacions d'arxius preexistents posteriors al ZIP 15/16 es limiten a set fitxers dins de l'àmbit iOS i el registre d'estat; els arxius originals d'Android, l'Android v336 de referència i els materials de servidor continuen idèntics. Els ZIP base 1–36, 1–39 i 1–42 tampoc han perdut fitxers.
- **Capa nativa:** 30 noms de mètode del pont `IosSessionBridge` tenen implementacions declarades en Swift. És **presència estàtica**, no conformitat de tipus compilada. El projecte Xcode conté dependència de StripePaymentSheet i URL de retorn 3D Secure.
- **Sintaxi:** `swiftc -frontend -parse` dels quatre fitxers Swift: **PASS**; `php -l` de `SERVIDOR_SUPERADMIN_IOS/admin/user-access-ios.php`: **PASS**, sense desplegament.
- **Proves Kotlin/JVM executades de nou contra els fitxers de política:** pas 37 **28**, 38 **24**, 39 **26**, 40 **36**, 42 **55**, 43 **59**, 44 **66**, 45 **65**, 46 **32**, 47 **148**; **539 assercions** en total, totes **PASS**. Són proves aïllades amb stubs quan correspon, **no compilació integral KMP**, i no acrediten UI, xarxa real ni processament de pagaments.
- **Gradle/Xcode:** `gradlew` necessita obtindre una distribució de `services.gradle.org`; l'entorn no resol eixe domini. Tampoc disposa de `xcodebuild` ni d'un simulador Apple. **Build total: NO EXECUTAT.**

## 2. Correcció incorporada al pas 48

`IosAgendaApi.reportEvent()` deixava en `UserDefaults` una clau persistent `ios_report_guest_id` creada exclusivament al telèfon. Ara reutilitza `guestSession.ensure().guestId`, és a dir, la **identitat real emesa pel servidor `guest/create.php`**, i elimina la lectura/escriptura d'eixa clau local. L'API continua decidint si accepta la denúncia. **No es pot assegurar** que l'historial d'incidències siga idèntic entre Android i iOS sense validar la lectura de resultats de denúncies amb el backend desplegat.

## 3. Auditoria funcional i visual — estat separat

| Secció | Presència de codi i connexions | Paritat real verificada en dispositiu |
|---|---|---|
| Feed «Per a tu» i «Sense filtres», publicitat comercial | Sí; polítiques i contractes separats | **No**; el motor PHP «Per a tu» no és exacte en tots els casos sense snapshot Android |
| Popups, festivals, cercador, Batecs | Sí, pantalles i descodificadors | **No**; no hi ha captura/execució d'iOS per comparar amb les referències Android |
| Guardats, llistes, comunitat, preferències | Sí, API i cua offline xifrada | **No**; necessita una prova real simultània d'Android i iOS |
| Notificacions i campanetes | Sí, APNs i programació local declarats | **No**; entrega push i cron/migracions sense prova real |
| Entrades, QR offline i bescanvi | Sí, QR local xifrat i autoritat al servidor | **No**; falta compra i inspecció real amb MariaDB |
| Publicació, editor, facturació | Sí, formularis i endpoints | **No**; falta comprovar cues de revisió i activació real de venda |
| Panell, publicitat, botiga, donacions, serveis | Sí, UI i regles de cobrament | **No**; falta pagar i gestionar comandes reals |
| Superadministració, canvis de rol | Sí, pantalles i verificació posterior | **No**; l'endpoint auxiliar no està desplegat |

**Paritat visual:** només es pot certificar després de compilar i comparar pantalles, pestanyes, alçàries, gestos, text, scroll, teclat, popups i estats de càrrega/errores en un iPhone. La presència d'un composable no és una prova de paritat visual.

## 4. Resultats de rendiment i de sincronització

- **Optimitzacions comprovades en codi:** catàleg públic amb cache i descodificació en `Dispatchers.Default`; càrrega progressiva configurada; instantànies `Per a tu`; posició de lectura i preferències al servidor quan correspon; refresc independent d'anuncis. **No hi ha mètriques obtingudes** de temps d'arrancada, primeres 5 targetes, memòria, FPS, saturació de xarxa o latència API.
- **Divergència confirmada:** Android v336 conserva les seleccions d'actuacions de festivals en `SharedPreferences` en lloc d'una única autoritat MariaDB. iOS sí que usa l'estat del servidor. Sense modificar Android i/o desplegar una migració sincronitzable, **no hi pot haver la paritat total** que s'ha demanat.
- **Altres divergències conegudes:** Android v336 usa un llindar de **7 artistes seguits** per activar Batecs, mentre la preferència funcional indicada és **6**; l'iOS de referència també comprova `eligible.size <= 6`. La paginació d'Android v336 no coincideix amb els lots 5+15+10 sol·licitats. L'Android v336 amaga alguns anuncis en cerques de text tot i la política global d'anuncis sempre visibles; iOS segueix la política explícita. No s'han alterat estos criteris en una auditoria que manté Android congelat.
- **Estat local no autoritatiu que resta:** `ad_popup_last_day` en la cache local d'iOS controla si s'ha obert ja un anunci de tipus popup. És una dada de presentació, però continua sent una persistència local d'estat i no s'ha migrat a servidor. La cache de cartellera pública és temporal i no s'usa com a autoritat per a preferències personals.

## 5. Bloquejos abans de declarar la funcionalitat completa

**Prioritat alta:**
1. **Compilar** Kotlin Multiplatform i Xcode amb les dependències reals; corregir errors de tipus, imports, enllaç de frameworks i qualsevol tancament al llançament. Són proves pendents, no fallades confirmades.
2. **Fer proves amb Android i iPhone connectats al mateix servidor i usuari:** convidat, fusió de convidat, registrat, professional i superadmin; llistes, seguiments, exclusions, festivals, avisos, desconnexió, recuperació i canvi de compte.
3. **Comprovar Stripe i reentrada de l'app:** recuperar qualsevol comanda `payment_pending` després d'un tancament total sense repetir càrrecs; els endpoints actualment acreditats no garanteixen recuperar el `client_secret` per ID per a tots els tipus de compra.
4. **Desplegar i provar els components auxiliars de servidor** (APNs, perfils i `admin/user-access-ios.php` + `001_SUPERADMIN_IOS.sql`) després d'una revisió de seguretat. **No s'han desplegat.** Provar que un usuari sense permís rep `401/403` de PHP en cada operació.
5. **Completar paritat Android de seleccions de festivals i estat persistent** i resoldre la clau de popup publicitari en servidor; no es pot certificar «tot al servidor» fins que això estiga comprovat.

**Prioritat mitjana:**
6. Comparació visual funcional completa pantalla a pantalla Android ↔ iOS, incloent convidats, festivals, tabulacions, teclat i gestos.
7. Mesurar rendiment real amb catàleg gran, connexió lenta i mode offline; comprovar scroll, cache, pujada de cartells, càrrega inicial i errors recuperables.
8. Conciliar requisit de 6 artistes seguits en Batecs, paginació desitjada i publicitat visible amb l'Android congelat, perquè l'especificació i el codi de referència actual no sempre concorden.
9. Comprovar enviament de correu, notificacions APNs, lectura de resultats de denúncies, moderació i liquidació de comandes amb contractes PHP productius.

## 6. Com executar les proves que falten

- **Entorn:** Mac amb Xcode, SDK iOS, dependències Kotlin/Native/Gradle accessibles i configuració correcta de signatura/Stripe.
- **Construcció:** utilitzar el projecte `ANDROID_STUDIO/iosApp/AgendaIOS.xcodeproj`, esquema `AgendaIOS`, `xcodebuild` per a iOS Simulator amb `CODE_SIGNING_ALLOWED=NO`, i verificar l'APP produïda. Les comprovacions de sintaxi del present informe no substituïxen això.
- **Backend:** entorn de proves amb comptes convidat/registrat/professional/superadmin, Stripe en mode prova, APNs i PHP/MariaDB instrumentats. Fer check de codis de resposta, persistència de dades i tornada després d'errors de connexió.
- **Regressió:** executar `python3 ANDROID_STUDIO/COMPROVACIONS/comprovar_pas_48.py` des del paquet amb les bases ZIP a `/mnt/data`, si s'executa en l'entorn de treball original; si es trasllada de màquina, adaptar només la ruta de les bases del test, **no el codi de l'app**.

## 7. Resultat final del pas 48

**Auditoria estàtica i correcció puntual completades; paquet acumulatiu tancat i verificat.** L'estat correcte del producte és **«implementat en codi amb comprovacions parcials; no compilat ni validat integralment en iPhone/servidor real»**. No està acreditat com a llest per a producció, i els bloquejos enumerats són reals.
