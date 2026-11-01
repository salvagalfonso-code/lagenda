# Pas 32/48 · Comunitat: membres, sol·licituds, xats i consultes privades

## Canvis aplicats exclusivament a iOS v349

- `IosAgendaApi.kt`: sessió de convidat o registrat amb `user_id`/Bearer capturats abans de cada operació i validats en acabar. `community/join.php` / `leave.php` retornen l'estat verificat amb una lectura de `community/groups.php?event_id=...`, sense confiar en l'estat anticipat d'iOS. Sol·licituds de creador `requests.php` i aprovacions/rebutjos `respond.php` amb validació de l'ID i comprovació que el pendent haja desaparegut. Els canvis d'estat no es reintenten automàticament.
- Els membres confirmats es descodifiquen del camp `members` de la resposta real de `community/groups.php`; el visor els mostra també a qui no és creador, sense donar permisos de moderació.
- `messages.php` només és accessible als acceptats; la UI recarrega l'estat abans de mostrar el xat, comprova `my_status` i separa els xats desactivats. `send.php` verifica missatge/usuari/grup/id de resposta i no crea missatges locals ficticis. Una fallada després del POST no es reintenta automàticament (POST no idempotent).
- `community/inquiries.php`: converses privades entre creador i cada persona, tant sense ser membre del xat com amb el xat desactivat, accés per `group_id` i bústia `?inbox=1`. L'autor pot respondre només a persones que han iniciat consulta; el client mostra cada conversa per `peer_user_id`. Les consultes usen únicament el servidor, sense `UserDefaults` ni fitxers personals persistents.
- `IosCommunityAdvanced.kt`: botons «Membres», «Estat» i «Consultes privades»; incorporació oberta o pendent d'aprovació; sol·licituds, aprovació, rebuig, missatges i bústia de consultes. Un canvi de compte no hereta els missatges previs.
- `IosCommunityPolicy.kt`: validació del resultat d'adhesió, estat confirmat, permisos locals de UI (servidor sempre és l'autoritat), grup FULL per membres existents i límit de 2000 caràcters.
- `AgendaIosScreen.kt`: el diàleg heretat compila amb la signatura actualitzada de `joinGroup` (sense alterar Android).

## Proves disponibles

- Comprovacions estàtiques acumulades 01–32 PASS i hashes SHA d'Android v336 + Android original del paquet iOS intactes.
- Compilació Kotlin/JVM **aïllada** del mòdul real `IosCommunityPolicy.kt` i 28 casos PASS; test commonTest `IosCommunityMembersMessagesTest.kt` afegit però no executat amb Gradle.
- `swiftc -frontend -parse iosApp/AgendaIOS/*.swift` PASS.
- La compilació completa KMP/iPhone no és executable en este entorn (no pot descarregar Gradle de services.gradle.org). No hi ha proves de HTTP contra PHP/MariaDB en viu ni ús real en iPhone.

## Dependències i limitacions importants

- `community/inquiries.php` i `community_board_inquiries`, incloses en `SERVIDOR_COMUNITAT` / el SQL complementari, **no formen part del `web.zip` principal i no consta que estiguen desplegades**. No s'ha modificat ni desplegat PHP, SQL o Android; esta funció no es considera operativa en producció fins a la verificació del desplegament.
- Les respostes `community/messages.php` tenen un màxim de 500 missatges; `community/inquiries.php` un màxim de 200 per tauló, i la bústia 100. No s'ha creat paginació no admesa pel PHP.
- Si falla internet just després d'un POST de missatge o consulta, el servidor podria haver-lo enregistrat. La UI conserva el text i indica que cal revisar la conversa abans de repetir: no hi ha un UUID d'idempotència en les API actuals.
- No s'ha aplicat cap esquema local com a segona font de veritat. El suport de bloqueig i moderació avançats es revisarà en el pas 33.

**Sense ZIP en el pas 32; pròxim lliurament acumulatiu: passos 1–33 quan acabe el pas 33.**
