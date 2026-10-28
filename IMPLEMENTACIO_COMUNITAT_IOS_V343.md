# L’Agenda — Integració de Comunitat iOS sobre la v343

## Base i abast

Base: `LAGENDA_V343_KMP_IOS_GUARDATS_VISUAL_ACUMULADA.zip`. S'ha mantingut el projecte Android i s'han afegit canvis a la UI i API compartides d'iOS; el servidor no s'ha desplegat.

## Implementat en el codi iOS

- Filtres per tipus de tauló: Cotxe compartit, Transport organitzat, Quedades, Allotjament compartit i Altres; vista Tots/Els meus.
- Fitxes de tauló amb estat, origen, trobada, descripció i participants quan l'API retorna les dades.
- Crear tauló amb descripció, lloc, data/hora, capacitat i control d'aprovació; editar les mateixes propietats com a creador.
- Accedir, sol·licitar accés, revisar peticions, acceptar/rebutjar participants, abandonar, tancar i reobrir.
- Denunciar missatges o taulons, amb motiu i confirmació després de resposta de l'API.
- Bloqueig d'usuaris des dels missatges, llista de persones bloquejades i desbloqueig.
- Moderació del creador: retirar missatges, expulsar/bloquejar participants dins del tauló, readmetre.
- Cua de denúncies de Comunitat per a `SUPER_ADMIN`: descartar, marcar revisada, retirar missatge, tancar tauló o expulsar participant. La comprovació del rol es repetix al servidor.

## Requisits imprescindibles de servidor

Desplegar en eixe ordre i **després de fer còpia de seguretat**:

1. `SERVIDOR_COMUNITAT/sql/001_comunitat_bloc_moderacio.sql` a la base MariaDB real. Només crea taules noves; no esborra dades.
2. Copiar els fitxers de `SERVIDOR_COMUNITAT/api/community/` al directori `app/api/community/` real. Les versions incloses de `_community.php`, `groups.php`, `join.php`, `send.php`, `messages.php`, `report.php`, `inquiries.php` són substitucions dels fitxers equivalents; conservar un backup abans de substituir-los. Els endpoints nous són `blocks.php`, `manage.php` i `admin-reports.php`.
3. Verificar que els endpoints existents `update.php`, `status.php`, `requests.php`, `respond.php`, `leave.php`, `create.php`, i la capa d'autenticació del servidor són els mateixos que els de la còpia facilitada. `HOSTING` es resol amb `_community.php` actualitzat.
4. Comprovar en un entorn de proves dues sessions amb rol PUBLIC i una SUPER_ADMIN, i provar els permisos del servidor, especialment que una persona no creadora no pot expulsar ningú i que PUBLIC no pot revisar la cua de denúncies.

**No** s'ha modificat la base de dades de producció i **no** hi ha cap enviament real a terceres persones. Les funcions de bloqueig/moderació requerixen el desplegament anterior; abans, les respostes 404 del servidor es mostren com a error sense confirmar falsament l'acció.

## Comprovacions disponibles

- 13 proves de regles Kotlin aïllades: permisos, missatges ocultats, estat de taulons i rols; superades.
- Sintaxi de tots els fitxers PHP inclosos: validada amb `php -l`.
- Comparació SHA256 dels fitxers d'Android inclosos respecte del ZIP de partida: sense modificacions (vegeu `COMPROVACIONS/comprovar_comunitat_v343.py`).
- No és equivalent a compilar l'app: falta Gradle/Kotlin-Native i Xcode en un Mac.

## Pendents de validació funcional

Compilar Kotlin Multiplatform i iOS, provar totes les interaccions de diàlegs, pantalla i permisos; executar les migracions en un entorn de proves; comprovar escenaris de grups amb i sense xat (la UI actual se centra en taulons amb xat), denúncies massives, bloquejos de contacte privat i recuperació després d'interrupcions de xarxa. Cal auditar també l'accessibilitat i el comportament en dispositiu real.
