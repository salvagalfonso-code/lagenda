# Pas 31/48 · Comunitat: cinc seccions i creació de taulons

## Implementat en la còpia acumulativa iOS v349

- Mateixes **cinc seccions** que `CommunityBoardPolicy` d'Android v336: Cotxe compartit, Transport organitzat, Quedades, Allotjament compartit, Altres. `IosCommunityBoardContract` projecta **onze tipus** antics sense canviar els identificadors reals dels taulons.
- `IosAgendaApi.community()` consulta `community/groups.php` (també convidats), i en «Els meus» incorpora els taulons contactats privadament de `community/board-settings.php?contacted=1`. Fa consultes en lots de fins a 100 IDs a `board-settings.php` per llegir el tipus específic i si el xat està activat.
- El formulari de creació des de l'esdeveniment permet els cinc tipus, accés obert/amb aprovació, descripció, origen, data/hora, capacitat i xat públic activat/desactivat.
- `createCommunityGroup` congela `user_id` i Bearer de l'usuari convidat o registrat, fa un preflight de `board-settings.php` abans de crear, i crea la fila base amb `community/create.php` seguint el contracte d'Android. Després escriu `board_type` i `chat_enabled` a `community/board-settings.php` i ho comprova en una lectura posterior. Per a `HOSTING`, la fila PHP legacy té `group_type=OTHER` i la fila d'ajustos retorna `HOSTING`.
- **No es reintenta automàticament** una creació després d'un possible POST aplicat: l'API no és idempotent. Si falla la segona operació, es comunica l'ID ja creat i s'evita tornar a enviar des del mateix diàleg. Els usuaris poden revisar el grup abans de tornar a crear-ne un.
- La lectura «Els meus» i el filtre del creador funcionen també amb `guestUserId` i no només amb el compte registrat. La consulta i el xat no es guarden permanentment al dispositiu.
- L'edició manté el tipus de tauló i `chat_enabled`; les operacions d'aprovació, expulsions i missatges es revisaran en els passos 32–33.

## Validació disponible

- Scripts de regressió 01–31, proves Kotlin/JVM de la política i parse Swift quan està disponible.
- Android v336 i l'Android embegut al projecte iOS no modificats. No s'ha creat ZIP en este pas.

## Limitacions reals

- **No s'ha compilat l'app completa en iPhone ni verificat l'API contra MariaDB en producció.**
- La creació de la fila base i els ajustos són **dos POST no atòmics** del servidor existent. Si falla el segon, pot quedar un tauló parcial. En este cas es mostra l'ID en lloc de repetir automàticament la creació; l'usuari pot revisar o corregir l'existent.
- Per a tipus antics la classificació específica i l'opció de xat depenen que `community/board-settings.php` i les seues taules estiguen desplegats. Si falla la lectura d'ajustos, es mostra la categoria base de PHP sense inventar valors.
- Els taulons amb xat desactivat indiquen que funcionen amb consultes privades; la interfície completa d'eixes consultes correspon al pas 32.
