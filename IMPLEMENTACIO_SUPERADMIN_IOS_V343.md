# L’Agenda iOS v343 acumulada · Superadministració

## Implementació afegida

Panell natiu de Compose Multiplatform en **Perfil**, visible únicament quan la sessió té `role == SUPER_ADMIN`. El client no és un control de seguretat: totes les operacions sensibles passen pels endpoints PHP que validen la sessió i el rol al servidor.

### 1. Resum de gestió
- `GET admin/dashboard.php?days=7|30|90`, resum de registres, usuaris actius, connexions, esdeveniments futurs i pendents, denúncies, comandes, entrades i imports de venda.
- Historial recent de campanyes internes, si està disponible.
- Les dades personals i administratives no es persistixen a la caché de l’iPhone.

### 2. Comptes i permisos
- Llista d'usuaris i cerca local per nom/correu (`GET admin/users.php`), 20 per bloc, sense mostrar accions per a visitants anònims.
- Fitxa individual amb sessions, seguiments, comandes i activitat recent (`GET admin/user-detail.php`).
- Canvis de rol o activació a través de **nou endpoint específic** `POST admin/user-access-ios.php`. No modifica `user_profiles`; comprova rol, sessió, ID, rols permesos, confirmació i prohibició de modificar-se a si mateix; revoca tokens en desactivacions/canvis i audita la modificació en `agenda_admin_access_audit`.
- Baixa administrativa mitjançant endpoint EXISTENT `POST admin/user-delete.php`, que exigix `ELIMINAR <username>` i anonimitza el compte sense esborrar els registres econòmics protegits.

### 3. Esdeveniments i denúncies
- Cercador paginat (25 per pàgina) `GET admin/events.php`, amb accés a l'ID per revisar l'esdeveniment.
- Consulta de publicacions de la Comunitat pendents `GET events/community-pending.php`; aprovar/rebutjar via `POST events/community-review.php` amb confirmació, motiu obligatori per al rebuig en iOS (mínim 12 caràcters).
- Consulta de denúncies pendents/reobertes `GET admin/reports.php`, **sense simular resoldre-les**. La resolució formal d'una denúncia seguix un altre circuit del servidor.
- Accions de moderació `APPROVE`, `REMOVE_CONFIRMED`, `RESTORE` via `POST moderation/review.php`, sempre confirmades. `REMOVE_CONFIRMED` exigix motiu detallat i pot generar advertències/suspensions del publicador; no elimina les entrades venudes.

### 4. Campanyes internes
- Previsualització de destinataris abans d'enviar (`POST admin/notifications-preview.php`), recompte validat de nou en el moment d'enviament pel servidor (`POST admin/notifications-send.php`).
- Filtres configurables: públic complet, rol, plataforma, activitat recent, seguiment d'un artista i guardat d'un esdeveniment. La segmentació és la que implementa `_notification_targets.php`.
- Canvis en el contingut o els filtres INVALIDEN la previsualització. Cal confirmació explícita per enviar.
- **Només bústia interna**, no push APNs/FCM; els endpoints originals declaren `delivery=INTERNAL_INBOX`.

## Canvis tècnics

- Nou `shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosSuperAdminConsole.kt`.
- Nou `shared/src/commonMain/kotlin/com/example/agendavalenciana/model/IosAdminPermissions.kt`.
- Ampliació de `IosAgendaApi.kt` amb les crides autenticades i validacions locals.
- `AgendaIosScreen.kt`: substitució del panell exclusiu de notificacions per la consola ampliada sota `SUPER_ADMIN`.
- `COMPROVACIONS/provar_superadmin_v343.kt`: 17 casos independents de permisos i confirmacions.
- `SERVIDOR_SUPERADMIN_IOS/admin/user-access-ios.php` i `SERVIDOR_SUPERADMIN_IOS/001_SUPERADMIN_IOS.sql`.

## Desplegament necessari del nou endpoint

1. Fer còpia de seguretat de MariaDB i del directori API.
2. Executar `001_SUPERADMIN_IOS.sql` a la base de dades de l'Agenda; crea la taula d'auditoria.
3. Instal·lar `user-access-ios.php` sota `app/api/admin/` conservant la ruta relativa de `_bootstrap.php`.
4. Confirmar que el servidor real inclou les API administratives i de moderació que figuren més amunt. No substituir a cegues les seues versions per les còpies d'auditoria.
5. Provar manualment, en entorn de proves, accés sense sessió (401), compte no admin (403), admin vàlid, intent d'autocanvi (409), confirmació errònia (422), revocació de sessions i registre d'auditoria. Provar també aprovació/rebuig, retirada/restauració, segmentació i previsualització prèvia.

## Validació i límits

- Les proves independents del model de permisos **no** demostren que un servidor en marxa rebutge o accepte correctament cada petició.
- S'han revisat estàticament les comprovacions de rol d'**11 endpoints originals més el nou**. Sense proves HTTP ni accés a MariaDB.
- `php -l` del nou fitxer; comprovació de preservació byte a byte dels fitxers existents Android i de servidor.
- L'app iOS **no s'ha compilat amb Xcode** ni provat al simulador/terminal. Les interaccions visuals, OAuth i els canvis de sessió requerixen validació d'integració.
- Exports CSV administratius, gestió professional completa, resolució formal de denúncies i push remots de campanyes **no** es consideren tancats en esta passada.
