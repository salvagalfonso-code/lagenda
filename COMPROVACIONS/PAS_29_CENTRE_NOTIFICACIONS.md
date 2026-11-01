# PAS 29/48 — Centre de notificacions i canals (iOS v349)

## Canvis implementats
- Sis pestanyes visibles: General (bústia completa), Pròxim, Nous, Entrades, Suggerències i Comunitat, classificades només pels deep links confirmats del servidor.
- «Pròxim» mostra també els recordatoris actius que retorna `user-state/mine.php`; el panell de recordatoris enumera recordatoris d'esdeveniment i campanetes d'artistes. No es fabriquen missatges ni horaris.
- Configuració integrada dins de Notificacions, no només en els ajustos del sistema: campaneta de l'app (nou indicadors), correus (resum NONE/DAILY/WEEKLY i continguts) i notificacions iPhone (opt-in i hores de descans). Enllaç separat als permisos del sistema.
- `IosNotificationCenterSettings.kt`: clau `alert_*` idèntica al nom d'Android v336, emmagatzemada via `sync/state.php` {t,v}, amb reintents en cua xifrada i protecció per identificador. Configuració de push en claus `ios_push_*` del mateix estat central, no en UserDefaults.
- Preferències de correu de comptes registrats llegides/escrites via `mail/preferences-api.php`, mateixa taula i esquema que Android, amb cua xifrada d’escriptures idempotents i GET de confirmació; convidats sense correu fins a iniciar sessió.
- «Netejar» és una acció explícita de marcar com a llegides en les API d'inbox; **no elimina registres** perquè el servidor actual no té endpoint d'esborrament. Cap esborrament fictici.
- Lectures protegides contra canvis de compte i sobrescriptura de configuració pendent.

## Verificacions
- `COMPROVACIONS/comprovar_pas_01.py` ... `comprovar_pas_29.py` (29 scripts de regressió i comparació SHA d'Android), PASS.
- Kotlin/JVM de `IosNotificationCenterSettings.kt` real amb dobles JSON i 17 comprovacions, PASS. `IosNotificationCenterSettingsTest.kt` afegit per a una futura execució amb Gradle.
- Anàlisi de sintaxi Swift només; **no** compilació completa Kotlin Multiplatform/Xcode, no prova en iPhone ni API real/MariaDB.
- Android v336 i còpia Android embeguda en v349: sense modificacions; PHP i SQL: sense modificacions.

## Limitacions reals i explícites
1. Els correus són persistents en la taula `email_preferences`. No s'envien fins que el servidor tinga el servei corresponent operatiu.
2. `sync/state.php` desa les preferències de campaneta en claus centrals, però Android v336 encara consulta algunes preferències de notificació de SharedPreferences locals, així que no hi ha paritat completa entre dispositius sense modificar Android.
3. Els flags `ios_push_*` i les hores de descans es guarden al servidor però el servei APNs encara **no** consulta estes preferències; la seua activació efectiva és part del pas 30 (no prometre notificacions push funcionals ara).
4. L'API `notifications/mine.php` inclou la bústia administrativa; altres tipus de notificació d'Android són calculats al dispositiu o provenen d'altres serveis. Les categories existeixen i classifiquen missatges reals, però algunes es mantindran buides sense una font al servidor. No s'inventen continguts.
5. El panell de recordatoris mostra aquells esdeveniments actius accessibles al catàleg: un recordatori amb un ID sense fitxa al catàleg pot no visualitzar-se encara.
6. El botó «Netejar» marca avisos com llegits, no els esborra permanentment.

## Següent pas
30/48 APNs: registre de dispositius, enviaments reals de prova, permisos i compliment dels canals, sempre sense modificar Android v336; ZIP 1–30 després del pas 30.

## Protecció de lectura incompleta
No es permet guardar preferències d'un compte registrat si no s'ha llegit prèviament la configuració canònica de correu: evita sobreescriure silenciosament preferències quan falla l'API. La cua xifrada s'encarrega del reintent de les mutacions d'estat i correu ja acceptades.
