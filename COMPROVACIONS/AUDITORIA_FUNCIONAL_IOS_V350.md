# L’Agenda — inventari de funcionalitats iOS v350 (auditoria v349)

**Abast:** 29/10/2026, paquet Android + KMP iOS v349, servidor PHP facilitat com `images.zip`, export MariaDB `agendapv (2)(3).sql`.

**Advertiment:** cap fila marcada CODI o CORREGIT significa compilada, provada en iPhone, desplegada al servidor o publicada. Les files indiquen evidència estàtica, no funcionament certificat.

**Llegenda:** `CODI` = codi identificat amb ruta aparentment existent; `CORREGIT` = canvi nou v350 sense prova en iPhone; `PARCIAL` = implementació incompleta; `BLOQUEJAT` = crida de client sense API aportada; `ABSENT` = funcionalitat iOS no implementada; `PROVA` = test o configuració pendent.

**Total:** 98 funcionalitats/àrees revisades. Repartiment: CODI: 54, PROVA: 3, ABSENT: 7, CORREGIT: 6, PARCIAL: 20, BLOQUEJAT: 8.

## Llista detallada

| # | Àrea | Estat | Funcionalitat | Evidència / limitació |
|---:|---|---|---|---|
| 1 | Base tècnica | CODI | Projecte iOS SwiftUI + Compose Multiplatform | iosApp + shared |
| 2 | Base tècnica | PROVA | Compilació Kotlin/Native per iOS | No hi ha build Xcode complet verificat |
| 3 | Base tècnica | PROVA | Enllaç Swift/Kotlin, simulador i iPhone real | No executat en estes proves |
| 4 | Base tècnica | CODI | Compilació de simulador amb Codemagic | codemagic.yaml; necessita execució real |
| 5 | Base tècnica | ABSENT | Workflow IPA signat, TestFlight i distribució App Store | No hi ha pipeline de publicació |
| 6 | Base tècnica | CORREGIT | Entitlement APNs diferenciada Debug/Release | aps-environment i build settings v350 |
| 7 | Base tècnica | PARCIAL | Accessibilitat VoiceOver / Dynamic Type / focus | Hi ha algunes etiquetes; sense auditoria |
| 8 | Base tècnica | ABSENT | Monitoratge de crashes, rendiment i telemetria iOS | Sense circuit de telemetria detectat |
| 9 | Base tècnica | PROVA | Revisió de privacitat i compliment App Store | Sense validació final de política i fitxa |
| 10 | Base tècnica | CODI | Caché local de dades i sessió segura en Keychain | AgendaKeychain.swift |
| 11 | Base tècnica | CODI | Caché del catàleg en fitxer i vista prèvia | EventCatalog.kt + AgendaKeychain.swift |
| 12 | Base tècnica | CORREGIT | Actualització de data en obrir-se la mitjanit | AgendaIosScreen.kt v350 |
| 13 | Base tècnica | PARCIAL | Recuperació de caigudes de xarxa i estats buits | Catàleg en caché; sense proves de xarxa |
| 14 | Inici i feed | CODI | Feed Per a tu | IosForYouSnapshot i IosPersonalFeedPolicy |
| 15 | Inici i feed | PARCIAL | Paritat exacta del rànquing Per a tu amb Android | Algorisme propi KMP; contrast de resultats pendent |
| 16 | Inici i feed | CODI | Feed Sense filtre | IosUnfilteredFeedPolicy i composició |
| 17 | Inici i feed | CODI | Càrrega progressiva 5 + 15 + 10 | limit i sentinella en EventList |
| 18 | Inici i feed | CODI | Refresc manual des de dalt | PullToRefreshBox |
| 19 | Inici i feed | PARCIAL | Retindre posició del feed durant 1 hora | Estat de scroll no persistent entre totes les navegacions |
| 20 | Inici i feed | PARCIAL | Paritat visual exacta Android/iOS | Colors i targetes adaptades; sense comparació en dispositiu |
| 21 | Inici i feed | CODI | Targeta de contingut cultural multidisciplinari | FeedCompactEventCard |
| 22 | Inici i feed | CODI | Dates i actes de festivals amb jornades | IosUnfilteredFeedPolicy |
| 23 | Inici i feed | CODI | Exclusions de fitxes, artistes i categories | IosFeedExclusionPolicy |
| 24 | Inici i feed | PARCIAL | Prioritats comercials, publicitat i entrades internes | Política codificada; falta prova exhaustiva sobre el catàleg real |
| 25 | Inici i feed | CODI | Opcions tres punts i denúncia d’esdeveniment | EventList, reportEvent |
| 26 | Inici i feed | PARCIAL | Preferència de valencià i suggeriments contextuals | Codificats; falten tests de correspondència Android |
| 27 | Inici i feed | PARCIAL | Territori, comarques i radis contextuals | IosGeoPlaces + IosPersonalFeedPolicy; validar geografia real |
| 28 | Cercador | CODI | Cerca general per artista, sala, poble, entitat i categoria | IosAdvancedSearch |
| 29 | Cercador | CODI | Autocompletat amb retard, sense recalcular cada pulsació | LaunchedEffect(query) 240 ms |
| 30 | Cercador | CODI | On? localitats/comarques i radi | Filtres avançats |
| 31 | Cercador | CODI | Data inici/fi, categoria, estil i subcategoria | Filtres avançats |
| 32 | Cercador | CODI | Cerques desades al servidor | api/searches via IosAgendaApi |
| 33 | Cercador | PARCIAL | Resultats i jerarquia exacta de cerques Android | Contracte codificat; equivalència no demostrada |
| 34 | Popups | CODI | Popup normal Info / Entrades / Ubicació / Comunitat | EventDetailSheet |
| 35 | Popups | CODI | Popup festival amb horaris i selecció del dia | EventDetailSheet + festivalSchedule |
| 36 | Popups | CODI | Buscar artista dins del cartell del festival | matchedSchedule |
| 37 | Popups | PARCIAL | Horaris guardats per actuació i sincronització | Guardat local per compte, no sincronitzat amb servidor |
| 38 | Popups | CODI | Popup d’artista amb seguiment | ArtistDialog |
| 39 | Popups | PARCIAL | Enllaços, catàleg, imatge i metadades d’artista | Presentació i contractes no equiparats del tot a Android |
| 40 | Popups | CODI | Compartir esdeveniment/enllaç | Pont nadiu UIActivityViewController |
| 41 | Batecs | CODI | Només artistes seguits i mínim de sis artistes | IosBatecArtistGroups |
| 42 | Batecs | CODI | Agrupació per artista i publicacions | IosBatecArtistGroups |
| 43 | Batecs | CODI | Vistos i coloració | batecs/seen.php |
| 44 | Batecs | PARCIAL | Gestos i pantalla batecs idèntics Android | Navegació implementada, gestos/dispositiu per provar |
| 45 | Personalització | CODI | Categories i subcategories | PersonalizationDialog |
| 46 | Personalització | CODI | Localitats i comarques preferides | PersonalizationDialog |
| 47 | Personalització | CODI | Radis personalitzats | IosPersonalFeedPolicy |
| 48 | Personalització | CODI | Suggeriments ON/OFF i nivell de descoberta | PersonalizationDialog |
| 49 | Personalització | CODI | Preferències separades per compte | IosAccountCachePolicy |
| 50 | Guardats | CODI | Guardats generals i recordatoris | IosSavedAgendaScreen |
| 51 | Guardats | CODI | Llistes: crear, canviar nom i eliminar | api/collections/index.php |
| 52 | Guardats | CODI | Llistes: compartir, unir-se i abandonar | api/collections/index.php |
| 53 | Guardats | CODI | Afegir i llevar esdeveniments a llistes | api/collections/index.php |
| 54 | Guardats | PARCIAL | Calendari individual de festivals entre dispositius | Còpia local; API de sync absent |
| 55 | Guardats | PARCIAL | Finestra de General de 7 dies o mínim 3 | No queda demostrada la mateixa lògica visual que Android |
| 56 | Notificacions | CODI | Bústia interna i marcar com llegides | notifications/mine.php i read.php |
| 57 | Notificacions | PARCIAL | Separació General / Pròxim / Nous / Entrades / Suggerències / Comunitat | Agrupació de client, no totes les tipologies generades al servidor |
| 58 | Notificacions | CODI | Recordatoris locals amb dia i hora | UNUserNotificationCenter iOS |
| 59 | Notificacions | CORREGIT | Obrir fitxa d’esdeveniment tocant un avís | Deep-link a evento via pont Swift/Kotlin v350 |
| 60 | Notificacions | CORREGIT | Cancel·lar avisos locals de compte en tancar sessió | AgendaKeychain.swift + AgendaIosScreen.kt |
| 61 | Notificacions | CORREGIT | Permisos de notificacions contextuals | Sense demanar permís al primer inici |
| 62 | Notificacions | BLOQUEJAT | Enviar notificacions APNs des de servidor | No hi ha api/notifications/apns-device.php al ZIP del servidor |
| 63 | Notificacions | BLOQUEJAT | Campaneta d’artista amb freqüència diària/setmanal | Falta api/notifications/artist-bell.php |
| 64 | Notificacions | BLOQUEJAT | Bústia de novetats d’artistes | Falta api/notifications/artist-inbox.php |
| 65 | Notificacions | ABSENT | Panell funcional per canal: campaneta, correu i push iOS | No hi ha sistema complet de preferències i enviaments |
| 66 | Usuaris | CODI | Iniciar sessió amb correu/usuari i contrasenya | auth/login.php |
| 67 | Usuaris | CODI | Registrar compte i recuperar contrasenya | auth/register.php i request-password-reset.php |
| 68 | Usuaris | CODI | Tancar sessió i revocar token | auth/logout.php |
| 69 | Usuaris | BLOQUEJAT | Sign in with Apple | Falta auth/apple-ios.php; alta/configuració Apple |
| 70 | Usuaris | BLOQUEJAT | Login amb Google a iOS | Falta auth/google-ios.php; configuració OAuth |
| 71 | Usuaris | PARCIAL | Visualitzar/editar nom, biografia i fotografia | S’han preparat 2 endpoints PHP nous; falta desplegament i prova |
| 72 | Usuaris | CODI | Canviar tipus de compte públic / professional | account/profile-settings.php |
| 73 | Usuaris | BLOQUEJAT | Eliminació del propi compte | Falta account/delete-ios.php, sense supressió segura confirmada |
| 74 | Usuaris | ABSENT | Onboarding inicial equivalent a Android | No hi ha flux iOS complet equiparable |
| 75 | Entrades | CODI | Consultar entrades comprades | tickets/mine.php |
| 76 | Entrades | CODI | Mostrar QR emés pel servidor | CoreImage QR i codi real |
| 77 | Entrades | CODI | Consultar tipus, preus i estoc | tickets/options.php |
| 78 | Entrades | CODI | Reservar entrada gratuïta | tickets/purchase.php amb confirmació servidor |
| 79 | Entrades | ABSENT | Compra d’entrades de pagament en iOS | Sense PaymentSheet/Redsys i retorn verificat |
| 80 | Entrades | CODI | Obrir venda externa HTTPS quan està indicada | platformOpenExternal |
| 81 | Entrades | PARCIAL | Validació professional per càmera i manual | AVFoundation i tickets/validate.php; proves de càmera/permisos pendents |
| 82 | Entrades | ABSENT | Reemborsaments i casos complets de pagament | No hi ha flux iOS complet |
| 83 | Comunitat | CODI | Consultar taulons per activitat | community/groups.php |
| 84 | Comunitat | CODI | Crear grups/transport/quedades i missatges | community/create.php i send.php |
| 85 | Comunitat | CODI | Demana accés, aprova membres i abandona | community/join.php, respond.php, leave.php |
| 86 | Comunitat | CODI | Denunciar missatges/taulons | community/report.php |
| 87 | Comunitat | BLOQUEJAT | Bloqueig d’usuaris i moderació avançada | Falten community/blocks.php, manage.php |
| 88 | Comunitat | BLOQUEJAT | Cua administrativa de denúncies Comunitat | Falta community/admin-reports.php |
| 89 | Publicitat | CODI | Inventari HEADERS/Premium/Normal/Popup | ads/active-global.php |
| 90 | Publicitat | CODI | Enregistrar impressions i clics | ads/track.php |
| 91 | Publicitat | PARCIAL | Segmentació, solapaments i garanties comercials | Regles locals; requerix prova amb servidor i Android |
| 92 | AgendaPRO | PARCIAL | Perfil professional i validació d’entrades | Opcions bàsiques; manca gran part de gestió |
| 93 | AgendaPRO | ABSENT | Creació/editació completa, vendes i estadístiques PRO | Més funcionalitats en Android que en iOS |
| 94 | Superadmin | CODI | Resum de dades, consulta d’usuaris i publicacions | IosSuperAdminConsole |
| 95 | Superadmin | CORREGIT | Editar rol i activació des del panell | Canvi a admin/user-update.php existent |
| 96 | Superadmin | CODI | Anonimitzar usuari per moderació | admin/user-delete.php existent |
| 97 | Superadmin | CODI | Enviar campanyes de notificació interna | admin/notifications-send.php |
| 98 | Superadmin | PARCIAL | Gestió administrativa global i permisos comparats amb Android | Conjunt iOS incomplet; servidor fa autorització |

## Bloquejos reals de contracte API

L’anàlisi de les rutes literals de `IosAgendaApi.kt` contra els PHP facilitats deixa **9 rutes absents** després dels canvis v350 i els dos endpoints PHP nous del paquet (sense desplegar):

1. `auth/apple-ios.php` (servidor Apple OAuth).
2. `auth/google-ios.php` (verificació de credencials Google iOS).
3. `account/delete-ios.php` (baixa de compte pròpia, anonimització i conservació legal).
4. `notifications/apns-device.php` (registre i desvinculació de token de dispositiu).
5. `notifications/artist-bell.php` (subscripció de cada artista per compte).
6. `notifications/artist-inbox.php` (bústia de novetats d’artistes).
7. `community/blocks.php` (bloqueig d’usuaris).
8. `community/manage.php` (gestió avançada de membres i missatges).
9. `community/admin-reports.php` (moderació administrativa de denúncies).

L'absència és respecte del **ZIP aportat**, no una comprovació directa de l'entorn de producció. Pot haver-hi versions diferents al servidor. Els endpoints del perfil `account/profile-ios.php` i `account/profile-ios-edit.php` s’han creat en el paquet **SERVIDOR_PATCH_IOS** i **no s’han desplegat**.

## Correccions introduïdes a v350

- Obrir l'esdeveniment real en tocar una notificació local o APNs d'esdeveniment, incloent l'arribada de la notificació abans de la càrrega del catàleg; navegar als artistes si el payload indica artista.
- Cancel·lar avisos locals pendents del compte quan es tanca la sessió; no barrejar marcadors de guardats en respostes tardanes d'altres sessions.
- Evitar que el refresc modifique l'estat d'un altre compte quan s'ha canviat de sessió.
- Mantindre actualitzada la data local si l'app es deixa oberta després de mitjanit.
- Substituir `admin/user-access-ios.php` per l'endpoint real `admin/user-update.php` conservant la prohibició de modificar els propis permisos.
- Separar la configuració de l'entitlement APNs de Debug (`development`) i Release (`production`); no significa que la distribució APNs estiga configurada.
- Demanar permisos push contextualment, no automàticament en el primer inici.
- Afegir dos controladors PHP autenticats de consulta/edició del perfil propi amb el model `user_profiles`, en un paquet separat de desplegament manual.

## Proves executades

- Anàlisi de sintaxi Swift amb `swiftc -frontend -parse`: superada. **No verifica tipatge amb SDK iOS**.
- Test Kotlin/JVM de sis regles independents de UI (identitats de comptes, memòria local, dates de festivals, avanç de batecs, campaneta): **6/6 superades**.
- Correspondència de 17 mètodes del pont `IosSessionBridge` amb implementacions Swift: **17/17** detectats estàticament.
- XML del `Info.plist` i entitlements: estructura vàlida.
- Dos fitxers PHP nous: `php -l` correcte; **sense proves HTTP ni MariaDB**.
- Verificació de fitxers Android contra v349: idèntics; no s'ha reescrit el codi d'Android.
- **No** s'ha pogut executar `xcodebuild`, Kotlin/Native amb SDK Apple, simulador ni iPhone real en este entorn. No s'ha fet cap modificació al servidor ni a la base de dades en producció.

## Prioritats abans de considerar la versió final

1. **Compilació real amb Codemagic**: corregir qualsevol error Kotlin/Native o Swift tipat; després obrir la build al simulador i iPhone.
2. **Completar contracte de servidor**: desplegar i provar 2 endpoints del perfil; desenvolupar amb migracions i comprovacions els 9 pendents, començant per la baixa de compte, OAuth i APNs.
3. **Integrar pagament real d'entrades**: Stripe PaymentSheet o el TPV acordat, verificació de webhook, consulta de comanda i comprovació de QR; cap emissió abans de confirmació.
4. **Paritat funcional Android**: AgendaPRO, comunitat i bloquejos, paritat visual, notificacions per canals, onboarding i guardats de festivals multi-dispositiu.
5. **TestFlight/App Store**: signatura, identificadors, privacitat, proves de compra, notificacions, UGC i accessibilitat.

**Seguretat:** no desplegar PHP de perfil ni habilitar APNs, OAuth o pagaments sense provar autoritzacions, migracions, credencials i regressions amb un entorn de proves. No copiar la base de dades real dins del ZIP de l’app.
