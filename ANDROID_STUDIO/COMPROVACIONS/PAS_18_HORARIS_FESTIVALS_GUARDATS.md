# PAS 18 — Horaris de festivals i Guardats (iOS v349, passos 1–18)

## Implementat en codi

- Cada actuació de festival guarda data, hora, artista/títol i ubicació/escenari en la seua clau (v2). Es poden recuperar les claus de cinc camps del ZIP anterior (v1).
- Guardats recupera els identificadors de l'usuari convidat o registrat des del servidor, incloses les seleccions d'actuacions de `sync/state.php` (`ios_festival_schedule_<event_id>` amb `{t:set,v:[...]}`). No hi ha `UserDefaults` com a magatzem personal persistent.
- Per mostrar una actuació, la seleccionada s'ha de trobar en el programa real de `special-events/public.php` (el mateix endpoint que consulta el popup) o en el programa ja rebut al catàleg. No s'inventen actuacions o dates entre inicis i finals.
- La pantalla Guardats considera la data final real de la programació; una primera jornada caducada no amaga un festival amb actuacions futures. Si les dates són incompletes, conserva la fitxa en lloc d'ocultar-la injustificadament.
- La data cultural utilitza **Europe/Madrid i canvi de jornada a les 05:00**, igual que `AgendaState.agendaToday()` d'Android. Evita confondre la data del mòbil amb la de la jornada.
- Els esdeveniments de Guardats es consulten des del catàleg complet, independent de la projecció cronològica del feed; la resta del feed no s'ha canviat.
- Si una escriptura falla per falta de connexió, la cua AES-GCM del pas 6 la reté. La pantalla mostra «pendent de sincronització», no «guardat al servidor». Les modificacions pendents es projecten només en RAM, separades per user_id i event_id, fins que el servidor confirme el mateix conjunt.
- Guardats de llistes compartides continua només amb esdeveniments, no amb les actuacions privades d'altres usuaris.

## Limitació estructural (Android v336 intacte)

`AgendaState.kt` d'Android fa `store.edit().putString(userCacheKey("festival_artist_selections"), ...).apply()` a `saveFestivalArtistSelections()` i llig la mateixa preferència amb `store.getString(...)` a `loadFestivalArtistSelections()`. El `store` és `context.getSharedPreferences("agenda_valenciana", ...)`. **No es fa crida API en estos dos mètodes.**

Això significa que, sense canviar Android, les seleccions individuals de festivals fetes en Android NO estan garantides en MariaDB i iOS NO les pot recuperar. iOS guarda les seues seleccions al servidor, però Android v336 tampoc consulta eixa clau iOS. No donar per assolida la paritat entre dispositius fins que la font de veritat siga compartida en els dos clients. Ni el PHP ni Android s'han modificat en este pas, com s'ha demanat.

## Validació

- 18 scripts d'auditoria estàtica: PASS.
- 24 comprovacions reals Kotlin/JVM de `IosSavedAgendaPolicy` i `IosFestivalSavedSlotPolicy` amb simulació JSON: PASS.
- Sintaxi Swift (`swiftc -frontend -parse`): PASS.
- Android original v336 (119 fitxers) i mòdul Android encastat dins del projecte iOS (93 fitxers): verificat idèntic byte a byte.
- Compilació KMP/Kotlin Native/iPhone: NO realitzada (entorn Linux sense Xcode i Gradle remot no accessible).
- Test HTTP real/MariaDB i prova de sincronització Android↔iPhone: NO realitzats.
- Limitació de backend: si `events/index.php` no retorna un festival perquè la primera data ja és antiga, o `special-events/public.php` no retorna el programa, la pantalla no pot reconstruir dades que no ha rebut.
- Migració de clau v1 sense escenari: un guardat antic pot identificar diverses actuacions idèntiques en escenaris diferents; la nova clau elimina la col·lisió per a guardats nous, però l'ambigüitat històrica no es pot resoldre sense metadades addicionals.

### Fitxers destacats

- `shared/src/commonMain/kotlin/com/example/agendavalenciana/model/IosFestivalSavedSlotPolicy.kt`
- `shared/src/commonMain/kotlin/com/example/agendavalenciana/model/IosSavedAgendaPolicy.kt`
- `shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosSavedAgendaScreen.kt`
- `shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/AgendaIosScreen.kt`
- `shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/EventCatalog.kt`
- `shared/src/iosMain/kotlin/com/example/agendavalenciana/ios/Clock.ios.kt`
- `shared/src/androidMain/kotlin/com/example/agendavalenciana/ios/Clock.android.kt`
- `shared/src/commonTest/kotlin/com/example/agendavalenciana/model/IosSavedFestivalParityTest.kt`
- `COMPROVACIONS/comprovar_pas_18.py`

Pròxim pas 19: Batecs d'artistes seguits i mínim de sis; pròxim ZIP després del pas 21.
