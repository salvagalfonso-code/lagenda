# L’Agenda iOS v349 · PAS 28/48 · Campanetes i recordatoris

## Canvis aplicats només a iOS

- `IosAgendaApi.kt`: recordatoris d'esdeveniments, també per a usuaris convidats identificats, mitjançant `sync/state.php` i `user-state/event-alert.php`; ambdós escrits es col·loquen en la cua xifrada **abans de començar** la transmissió. Els rebuts `APPLIED` / `ALREADY_APPLIED`, `user_id`, `revision`, POST amb `active` i GET `user-state/mine.php` controlen quan l'operació es pot eliminar de la cua. L'estructura `when_ID` (mil·lisegons UTC), `title_ID`, `town_ID`, `custom_ID` i, quan hi ha snapshot fiable, `event_ids`, coincideix amb Android v336. PHP `sync/state.php` projecta `when_ID` a `agenda_user_reminders`; l'emissor cron és del servidor.
- `IosReminderPolicy.kt`: comprovació de data/hora, esdeveniment futur, actualització de la llista d'IDs sense perdre altres avisos quan es desconeix el snapshot, i projecció de canvis pendents en RAM.
- `IosPendingServerQueue.kt`: noves accions de `notifications/artist-bell.php`, seguiment separat de pendents d'artista/esdeveniment i del `sync/state.php`.
- `IosAgendaApi.kt`: campanetes d'artista a `notifications/artist-bell.php` amb comprovació POST + GET, `artist_name`, `frequency` i `enabled`, tant per a convidats com per a usuaris registrats. Lectura de campanetes aplica la cua pendent corresponent a l'usuari.
- `AgendaIosScreen.kt`: la campaneta d'esdeveniment no exigeix iniciar sessió si ja hi ha identificador convidat; mostra el selector de **dia i hora**, valida hora futura i guarda **només al servidor**. Deixa d'invocar la programació local de `UNUserNotificationCenter`; cancel·la només alarms antigues per evitar duplicats. Distingix «guardat al servidor» de «pendent de sincronització». La campaneta d'artista continua amb **Al moment / Diari / Setmanal** (els tres valors que PHP i Android v336 admeten), però utilitza ara la cua xifrada amb confirmació.

## Límit de funcions i de paritat

1. Android v336 conserva la pròpia campaneta d'artista en preferències locals (`artist_alerts` i `artist_bell_frequency_*`). No es pot certificar equivalència entre Android i iPhone per a eixes campanetes sense modificar Android, que està congelat per petició de l'usuari.
2. El backend principal `web.zip` no inclou `notifications/artist-bell.php` ni les taules `ios_artist_bells`; apareixen en el paquet separat `SERVIDOR_APNS`. L'API i les migracions s'han de desplegar i provar abans que la funció puga enviar avisos. No s'ha modificat el servidor.
3. Android i el PHP d'artistes només admeten tres freqüències. **No hi ha un camp per a triar dia/hora específica de cada artista**. Les dates/hores personalitzades d'este pas són per a esdeveniments. Les opcions de canals, APNs i entrega real es treballaran en passos 29 i 30.
4. La cua garantix conservar les operacions pendents en iOS, però dos endpoints de recordatori no són una transacció atòmica. S'escriuen en ordre, es reintenten i es validen fins que es completen. Quan no hi ha cap snapshot dels `event_ids`, s'evita sobreescriure una llista desconeguda; el camp `when_ID` és suficient perquè el planificador servidor cree el recordatori, però la llista `event_ids` s'haurà de reconciliar després.
5. No s'ha compilat KMP/Kotlin Native ni s'ha provat el servidor/MariaDB o un iPhone real. La verificació és de codi, contractes amb PHP, proves Kotlin/JVM amb dobles de xarxa i comprovacions estàtiques.

## Validació d'este pas

- Scripts acumulats `COMPROVACIONS/comprovar_pas_01.py` a `comprovar_pas_28.py`: PASS (28/28); el control 05 s'ha actualitzat perquè la mutació `when_ID` ara s'implementa en la capa API i no directament en el composable.
- Kotlin/JVM **codi real** `IosReminderPolicy.kt`: 18 comprovacions PASS.
- Kotlin/JVM **cua real** `IosPendingServerQueue.kt` amb JSON i transport simulats: OFFLINE, reintents, ordre, ID d'usuari, descart només amb confirmació: PASS.
- Kotlin/JVM `IosArtistBellPolicy.kt` existent: PASS.
- `swiftc -frontend -parse iosApp/AgendaIOS/*.swift`: PASS.
- Comprovació SHA de tots els fitxers Android v336 i del mòdul Android original incrustat en v349: PASS; **cap fitxer Android ni PHP modificat**.

**Pas 28 conservat en còpia acumulativa; NO generar ZIP fins al pas 30.**
