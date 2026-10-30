# Servidor · permisos superadministrador v343

Estos fitxers són **nous** i **no estan desplegats en producció**.

1. Fes còpia de seguretat de la base de dades i del servidor.
2. Executa `001_SUPERADMIN_IOS.sql` en MariaDB: crea `agenda_admin_access_audit`.
3. Copia `admin/user-access-ios.php` a `app/api/admin/user-access-ios.php` mantenint l'estructura existent `_bootstrap.php`.
4. Comprova que `api_tokens` existix i que els endpoints `admin/users.php`, `admin/user-detail.php`, `admin/user-delete.php`, `admin/dashboard.php`, `admin/events.php`, `admin/reports.php`, `admin/notifications-preview.php`, `admin/notifications-send.php`, `events/community-pending.php`, `events/community-review.php`, `moderation/review.php` ja estan desplegats.
5. Prova primer amb una base de dades i usuaris de proves. Revocar tokens obliga a tornar a iniciar sessió.

**Advertiment:** els rols i la moderació afecten usuaris i publicacions reals. La interfície amagada no és una barrera; les validacions es realitzen al servidor. Este paquet no activa cap acció per si mateix.
