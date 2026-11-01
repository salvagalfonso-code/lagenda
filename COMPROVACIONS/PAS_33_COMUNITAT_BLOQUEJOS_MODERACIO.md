# Pas 33/48 · Comunitat — bloquejos, denúncies, expulsions i moderació

## Abast implementat (només iOS v349)

- `IosCommunityModerationPolicy.kt`: comprovacions de validesa de denúncia i actor/destinatari, precondicions de BAN_MEMBER, UNBAN_MEMBER, DELETE_MESSAGE; comprovació dels estats de denúncia, estats de bloqueig i receipts de moderació. **Les decisions definitives de permisos són del servidor.**
- `IosAgendaApi.kt`: `community/report.php`, `community/blocks.php`, `community/manage.php` i `community/admin-reports.php` amb **identitat de convidat o usuari registrat fixada durant l'operació**, i verificació de la mateixa identitat en acabar. Els GET de moderació retornen dades del servidor, no d'una còpia local.
- **Bloquejos:** no permet bloquejar-se a un mateix; POST `BLOCK`/`UNBLOCK` comprova `ok` i el booleà de resposta, i GET posterior de la llista canònica confirma el canvi abans d'actualitzar la interfície. Convidats inclosos, si disposen de token vàlid al servidor.
- **Denúncies de tauló i missatge:** motiu no buit, màxim 500 caràcters; POST i `ok` verificat, sense reintents automàtics perquè el PHP no admet `idempotency_key`. El backend exigeix pertinença al tauló per denunciar un missatge, i limita les denúncies a 20 per 24 hores.
- **Expulsions/readmissions:** GET previ `community/manage.php` només per al creador (aplicació explícita de permisos PHP); prohibit expulsar el creador o el compte actiu, confirmació del POST amb l'acció exacta, GET posterior per verificar l'estat de la prohibició del tauló. Retirada de missatges: receipt `{ok:true,action:DELETE_MESSAGE}`. El PHP no retorna un ID de transacció de moderació ni exposa un historial de tots els missatges per a verificar de manera absoluta la retirada en pantalla.
- **Superadmin:** només es mostra l'accés de moderació amb rol `SUPER_ADMIN`; les API comproven el rol de nou (i PHP ho valida realment). Consulta d'incidències obertes, precondicions d'acció (missatge, grup, persona), `DISMISS`, `REVIEW`, `DELETE_MESSAGE`, `CLOSE_GROUP`, `BAN_MEMBER`, comprovació del `status` retornat i GET posterior de totes les denúncies fins trobar la fila modificada.
- `IosCommunityAdvanced.kt`: consulta bloquejos i revoques, botons de denúncia, expulsió, readmissió, retirada de missatges i tauler superadmin; el diàleg del xat conserva els estats separats per ID d'usuari, i les accions administratives destructives requerixen confirmació.
- Totes les operacions són **remotes**, no es guarden dades de moderació en `UserDefaults`, no s'inserixen en cua offline i no es reintenten automàticament després d'una resposta ambigua.

## Proves efectuades

- 33/33 scripts estàtics `COMPROVACIONS/comprovar_pas_XX.py`: PASS.
- 39 assercions Kotlin/JVM executades amb el fitxer real de producció `IosCommunityModerationPolicy.kt` i models de prova: PASS.
- `swiftc -frontend -parse iosApp/AgendaIOS/*.swift`: PASS.
- `php -l` dels fitxers de `SERVIDOR_COMUNITAT/api/community`: PASS.
- Integritat SHA de 119 fitxers de la referència Android v336 i `app/src` de la v349 original: PASS.

**NO comprovat:** compilació de tot el projecte Kotlin Multiplatform/Xcode (Gradle no disponible per DNS), execució de la interfície en iPhone, permisos contra un servidor HTTP real, efectes en MariaDB i moderació real d'usuaris.

## Requisits de desplegament / limitacions

- `community/blocks.php`, `community/manage.php`, `community/admin-reports.php` i el SQL `001_comunitat_bloc_moderacio.sql` del complement **SERVIDOR_COMUNITAT** no estan dins de `web.zip` original i no consta que estiguen desplegats. No s'han publicat en producció ni s'ha alterat l'original del servidor.
- `community/report.php` sí que té un fitxer original, però la implementació reforçada del complement depén del mateix esquema SQL de moderació; verificar versió del servei en desplegament.
- En el GET administratiu `?status=ALL`, PHP retorna un màxim de 200 denúncies; una denúncia molt antiga pot haver-se aplicat i no aparéixer en la comprovació posterior. En este cas iOS dona error de verificació, **no reenvia el POST**.
- El GET de `community/manage.php` limita el rol de gestió al creador; els moderadors globals tenen el seu endpoint independent.
- Les limitacions dels passos anteriors (consultes privades requerixen el paquet complementari; Android v336 congelat manté algun estat local; no hi ha compilació iPhone) no desapareixen amb este pas.

Cap modificació d'Android v336 ni del servidor original. ZIP acumulatiu complet en el pas 33.
