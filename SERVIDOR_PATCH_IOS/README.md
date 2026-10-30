# Complement de servidor iOS v350 — perfil propi

Es faciliten dos endpoints nous que **no estaven** en la còpia de servidor:

- `api/account/profile-ios.php` (GET autenticat)
- `api/account/profile-ios-edit.php` (POST autenticat)

Col·locar exclusivament en `/app/api/account/` **després** de provar-los en una còpia del servidor. Requerixen les rutes existents `api/_bootstrap.php`, `api/images/_images.php`, una taula `user_profiles` d’acord amb el SQL aportat, connexió MariaDB i l’autenticació Bearer actual. No inclou credencials. No hi ha migració SQL per a estos dos endpoints.

**Proves locals:** comprovació sintàctica `php -l`; no s'ha provat connexió, permisos de fitxer, `INSERT/UPDATE`, ni registres reals. No substitueix els nou endpoints addicionals ni Stripe/APNs.
