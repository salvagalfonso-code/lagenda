# Pas 30/48 — APNs i preferències d'enviament iOS v349

## Implementat en el client iOS
- `AgendaKeychain.swift`: token APNs hexadecimal, emmagatzemat al **Keychain** i no en `UserDefaults`; dades de sessió registrat/convidat independents, reassignació de registre per Bearer vàlid i sol·licitud de baixa del token quan el compte es tanca o es desactiva push. HTTP valida 2xx + cos JSON `ok` i `registered` per confirmar peticions; si falla, reintent de registre quan l'app torna al primer pla.
- `AgendaIOSApp.swift`: registre del delegat APNs, extracció de token oficial, notificació visible en primer pla i obertura de les fitxes d'artista/esdeveniment amb identificadors del payload. **No** demana permís automàtic en la primera obertura.
- `AgendaIosScreen.kt` i `IosAgendaApi.kt`: l'opció push es carrega primer de `sync/state.php`; quan l'usuari l'activa es demana permís i es registra el dispositiu, si el desactiva se sol·licita la baixa. Es gestiona per convidat i compte registrat i es deixa de mostrar l'estat anterior en canviar de compte. Les preferències i hores de descans es continuen guardant en la cua xifrada i al servidor. El deep link de notificacions EVENT obri una fitxa només si existeix en el catàleg carregat.
- `AgendaIOS.entitlements`, `project.yml`, `AgendaIOS.xcodeproj`: entorn `aps-environment` Debug=development i Release=production, l'aplicació llig el valor de l'entitlement real signat abans d'enregistrar-se.

## Ampliació de servidor preparada, **NO desplegada**
- Els cinc fitxers originals de `SERVIDOR_APNS/` es conserven, i el canvi preparat per desplegament està en `SERVIDOR_APNS_PAS30/`.
- `api/notifications/apns-device.php`: obliga a tindre `ios_push_enabled=true` abans de registrar un token; una baixa amb Bearer autenticat no afecta el compte d'un altre usuari.
- `cron/_ios_push_prefs.php`: valors de l'estat compartit `agenda_user_client_state`, categories autoritzades, quiet hours (23:00–08:00 per defecte si s'activen; valors guardats en minuts), jornada Europa/Madrid amb canvi DST. Els avisos d'esdeveniment amb hora explícita (`custom_time=1`) no es retarden.
- `cron/artist-push.php`: manté `IMMEDIATE`, `DAILY` (19h) i `WEEKLY` (dilluns 19h), utilitza la configuració de cada usuari, ajorna durant descans, reintenta i reconeix pendents ajornats.
- `cron/inbox-push.php`: lliurament via APNs HTTP/2 de notificacions **reals** d'`admin_notification_inbox`, només si el compte ha optat in i té token; respecta preferències i descans; detecta tokens revocats. Porta receipts per (inbox_id, token), evita duplicar quan Apple ha retornat HTTP 200. **Apple acceptat no significa que l'iPhone haja rebut/presentat l'avís.**
- `sql/02_apns_inbox_delivery.sql`: taula nova de rebuts d'enviament, sense modificar taules d'Android.

## Posada en producció necessària
1. Desplegar prèviament el paquet `SERVIDOR_APNS` existent: SQL `01_artist_bells_apns.sql`, endpoints i cron d'artistes, només si encara no està instal·lat. **No s'ha comprovat l'estat del servidor real.**
2. Desplegar **sobre els fitxers corresponents del servidor** la carpeta additiva `SERVIDOR_APNS_PAS30` i executar `sql/02_apns_inbox_delivery.sql` en la base **agendapv**. No executar SQL en bases WordPress.
3. Configurar en el servidor, fora de la carpeta pública: `APNS_TEAM_ID`, `APNS_KEY_ID`, `APNS_P8_PATH`, `APNS_TOPIC=com.agendavalenciana.app.ios`, `APNS_SEND_ENABLED=1`, `APNS_JWT_CACHE_PATH`. No incloure .p8 ni secrets en ZIP ni en iOS. Comprovar PHP curl HTTP/2 i OpenSSL.
4. Activar Push Notifications en Apple Developer, renovar perfil de firma, compilar una versió signada Debug i una Release; només després validar `development/production` amb un iPhone físic. `CODE_SIGNING_ALLOWED=NO` en simulador **no** certifica APNs.
5. Programar `cron/artist-push.php` i `cron/inbox-push.php` cada minut (o freqüència compatible amb l'hora/latència desitjada); mantenir els cron de recordatoris i bústia existents. Comprovar idempotència amb un dispositiu i després amb dos per al mateix compte, activar/desactivar, convidat→registrat, canvis d'usuari, descans nocturn i notificació personalitzada.

## Proves realitzades
- Scripts `comprovar_pas_01.py` fins a `comprovar_pas_30.py`: **30/30 PASS** (inclou comprovació SHA d'Android v336 i mòdul Android incrustat).
- `swiftc -frontend -parse` dels arxius Swift: **PASS** (anàlisi sintàctica, no typecheck d'UIKit ni compilació d'iPhone).
- `php -l` dels fitxers d'ampliació APNs: **PASS**.
- PHP CLI amb hores simulades i preferències, descans nocturn/diürn, categories i recordatori personalitzat: **PASS**.
- No s'han proporcionat credencials APNs de producció ni un iPhone signat; **no s'ha enviat cap push real ni provat la base MariaDB en viu**. Gradle i Xcode complets no han estat disponibles.

## Límits persistents
- En aquest pas es prepara un **desplegament** de servidor, no s'instal·la al servidor en viu; sense SQL, endpoints, cron, credencials i perfil Apple correctes els push no s'enviaran.
- Android v336 conserva algunes campanetes d'artista i guardats de festivals localment; com Android està congelat, no es pot certificar 100% de sincronització.
- Les categories sense generador al servidor no creen notificacions per art de màgia. La programació actual d'artistes és immediata, diària a les 19h i setmanal dilluns a les 19h (Europe/Madrid); no existeix selector d'hora individual per artista.
- Les hores de descans s'interpreten en la zona Europe/Madrid. Una zona horària per a usuaris que viatgen encara requeriria un camp nou, que no s'ha inventat.
- La desactivació mentre l'usuari no té internet queda pendent d'enviar al servidor; no es pot garantir que Apple retire una notificació ja acceptada.
