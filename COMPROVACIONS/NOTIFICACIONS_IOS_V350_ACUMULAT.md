# L’Agenda — Notificacions iOS, implementació acumulada v350 (29/10/2026)

## Comparació real amb Android v349

Fitxers Android consultats: `app/src/main/java/com/example/agendavalenciana/ui/AgendaApp.kt` (`AlertsSheet`, línies 10020+), `state/AgendaState.kt` (constructor de notificacions, línies 10380+) i `data/ServerApiRepository.kt` (preferències de correu, línies 5097+). Android original no s'ha modificat.

## Implementat en codi iOS

1. Modal a altura completa, títol, recompte pendent, botó de recordatoris, configuració i tancament amb tornada a la pàgina de notificacions.
2. Sis pestanyes amb icones i marcadors: **General, Pròxim, Nous, Entrades, Suggerències, Comunitat**. «General» mostra avisos no llegits i suggerències no vistes.
3. «Pròxim»: guardats amb inici entre hui i set dies; no mostra esdeveniments acabats. Està basat en dades reals de l'API.
4. «Nous»: novetats reals d'artistes amb campaneta activada, i marques temporals de creació verificables; es respecten les freqüències per artista d'Android amb la mateixa finestra de 7 dies / 24 h i la serialització dels avisos diaris i setmanals per artista. Sense marca de creació no es fabrica cap notificació.
5. «Entrades»: notificacions oficials del servidor i relació d'entrades del compte obtingudes de `tickets/mine.php`. No se les inventa data de compra ni badge nou si el servidor no la retorna.
6. «Comunitat»: notificacions arribades del servidor, sense inventar-ne a partir de xats genèrics.
7. «Suggerències»: selecció conservadora d'activitats de la pròxima setmana que coincidixen simultàniament amb una categoria i un poble escollits; només si l'usuari té activades les suggerències. No equival encara a la puntuació avançada d'afinitat d'Android.
8. Lectura i neteja: marcadors locals persistents per compte, evitant reapareixença en reobrir el panell. `notifications/read.php` i `notifications/artist-inbox.php` es continuen utilitzant per als avisos remots, i l'eliminació total intenta marcar-los també al servidor. Els suggeriments netejats continuen en la seua pestanya, com Android.
9. Recordatoris actius: seccions d'esdeveniments i artistes; obrir, desactivar campaneta d'esdeveniment; obrir artista, editar o desactivar campaneta d'artista (condicionat a resposta real API).
10. Configuració interna: preferències separades per categoria en la campaneta de l'app; guardades per compte en cache local de la interfície. No confondre amb preferències del servei de correu.
11. Correu electrònic: consulta i guardat autenticats contra la mateixa ruta `mail/preferences-api.php` d'Android, amb periodicitat desactivat/diari/setmanal i quatre opcions temàtiques. L'app no anuncia que el correu s'haja guardat si falla HTTP.
12. Ajustos del sistema: accés als ajustos de notificacions d'iOS, separat de la configuració pròpia de l'app; s'indica explícitament la dependència del servei APNs.
13. Barra superior: badge calculat de les notificacions realment no llegides, incloses les derivades. Suggerències no incrementen el badge principal, igual que Android.
14. La icona d'estrela de suggerències es dibuixa en el kit visual compartit d'iOS.

## Limitacions i bloquejos que NO són funcionalitats acabades

- Sense compilació completa a macOS/Xcode: no es pot afirmar paritat visual exacta en un iPhone.
- Servidor del paquet facilitat incomplet per a `notifications/artist-inbox.php` i `notifications/artist-bell.php`. Les crides iOS existixen, però cal desplegar i provar eixes rutes en el servidor real.
- Push APNs extern d'artistes/grups pendent de validació contra servidor + entitlements + certificat/token.
- Hores de descans d'Android no integrades en el planificador natiu iOS; es mostra explícitament com a pendent, no com un interruptor funcional.
- Falta equiparar exactament el motor de suggerències (Android usa puntuació `>=95` amb preferències, activitats i senyals recents).
- En Android les compres noves necessiten marca de compra real de <=7 dies; l'API de tiquets iOS del paquet no proporciona `purchased_at`, per això no es pot afirmar que un tiquet siga una compra nova de la setmana.
- Correus: `mail/preferences-api.php` està referenciat en el codi Android original i ara en iOS, però la seua existència/desplegament en el servidor de producció no s'ha verificat.
- La neteja local de notificacions no equival a una operació de supressió en el servidor, i cal provar el comportament en diversos dispositius.

## Verificacions

- 11 proves de lògica pures Kotlin/JVM superades sobre el codi real de `IosNotificationParity.kt`, amb dobles mínims per a models i data.
- 93 fitxers Android sense modificacions: comprovació `sha256sum -c` superada.
- Intent de compilació del mòdul compartit en Linux bloquejat abans d'iniciar Gradle (DNS de `services.gradle.org` no disponible); cap prova de compilació iOS completada.
- ZIP: NO generat. Numeració: es manté v350. Arbre acumulatiu `/mnt/data/LAGENDA_V350_TREBALL`.
