# L’Agenda — v350 · sincronització compartida de Personalització

## Implementat en el codi de treball

- **Tres opcions abans només locals**: `suggestions` (Suggerències), `discovery` (`MY_TASTES`, `BALANCED`, `EXPLORE`) i `valencian_first` (Prioritzar valencià).
- **Backend únic**, sense crear una base de dades diferent: `SERVIDOR_PER_A_TU/api/preferences/feed.php` (`GET`/`POST` autenticat). La configuració resideix a `user_for_you_preferences` i s'identifica pel token del compte, mai per un ID aportat pel client.
- **Migració MariaDB**: `SERVIDOR_PER_A_TU/sql/02_user_for_you_preferences.sql`.
- **Motor de Per a tu**: `SERVIDOR_PER_A_TU/api/for-you/feed.php` llig les opcions del compte des de MariaDB i les imposa sobre valors locals obsolets. La nova escriptura invalida `for_you_server_cache`.
- **Android**: `data/ServerApiRepository.kt` i `state/AgendaState.kt` implementen lectura, escriptura, recuperació després d'inici de sessió i arrancada, persistència per usuari, detecció de canvis durant una lectura i reintent dels canvis locals pendents. La cache del feed s'invalida quan arriba una actualització remota.
- **iOS**: `ios/IosAgendaApi.kt` i `ios/AgendaIosScreen.kt` fan el mateix `GET`/`POST`; els canvis es guarden per compte, es serialitzen amb `Mutex`, es recuperen en iniciar sessió i forcen un nou càlcul de «Per a tu» després que el servidor confirme l'escriptura.
- Les **categories i subcategories**, els **artistes seguits**, els **llocs/radis** i els **bloquejos** conserven els mecanismes de sincronització existents. Aquesta passada amplia els tres ajustos globals del feed; no substituïx ni desplega les rutes anteriors d'eixes funcionalitats.

## Verificació local

- `php -l`: tots dos PHP sense errors de sintaxi.
- `php SERVIDOR_PER_A_TU/tests/test_engine.php`: 8 comprovacions del motor correcte.
- `python tests/test_feed_preferences_sync.py`: 13 comprovacions estàtiques del contracte correctes.
- **No s'ha compilat Android ni iOS**, i encara no s'ha fet una prova d'integració amb MariaDB ni amb una sessió de producció.

## Desplegament imprescindible per fer-ho operatiu

1. Backup de la base de dades i de les rutes `app/api/preferences` i `app/api/for-you`.
2. Sobre la MariaDB EXISTENT, executar `02_user_for_you_preferences.sql` (i `01_for_you_server_cache.sql` si encara no estava instal·lat).
3. Copiar `api/preferences/feed.php` i els fitxers `api/for-you/feed.php`, `_engine.php` i `data/` a l'API original, conservant `api/for-you/index.php` i la resta dels endpoints.
4. Desplegar noves versions dels clients Android/iOS compilades. Comprovar que les tres opcions viatgen d'Android a iOS i d'iOS a Android amb el mateix compte després d'iniciar sessió, i que no es barregen comptes.
5. Provar fallada de xarxa, canvi ràpid de preferències, primer login d'un compte antic, selecció d'IDs en el servidor i invalidació de caché.

## Limitacions de la v350 que encara resten

- El càlcul de «Per a tu» ja té ruta PHP compartida però no està desplegat; manté **fallback local** per absència del servidor.
- El servidor llig categories de `user_follows`. Els **llocs/radis** encara arriben al motor des del client després de la sincronització anterior `preferences/locations.php`; per això queda pendent verificar una sessió real amb tots els dispositius i dades carregades.
- Cal validar dades reals i paritat completa de la selecció del feed. Les proves estàtiques no validen aquestes condicions.

**Regla de lliurament**: mateixa v350 acumulada. No s'ha generat cap ZIP, ni s'ha incrementat la versió.
