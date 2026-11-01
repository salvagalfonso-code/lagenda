# Pas 34 — Perfil, fotografia i configuració, paritat d'API amb Android v336

## Canvis implementats només en iOS

- Lectura del perfil públic amb `user-profiles/catalog.php`, coincidència **exacta de username** i credencial Bearer del mateix compte. Per absència de perfil, no s'inventen camps professionals.
- Edició amb `user-profiles/update.php`, conservant **tots** els camps que Android envia: `display_name`, `bio`, `discipline`, `company_name`, `cast_text`, `publisher_name`, `books_text`, `works_text`, `spaces_text`, `remove_image`. Les respostes han de confirmar la mateixa identitat, nom i camps professionals. No s'escriuen snapshots personals al local.
- Pujada JPEG amb `images/upload.php` (`owner_type=USER`, `owner_key=username`, base64) i lectura posterior amb `images/owner.php` per comprovar la URL del servidor. El selector natiu convertix a JPEG abans de l'enviament, en memòria.
- Edició separada de nom d'usuari, correu i població amb `account/update.php`, com Android. El resultat del servidor es comprova i actualitza l'identificador visible de la sessió; canvis de correu poden exigir nova verificació.
- `account/profile-settings.php` compartida: rol públic/professional, telèfon i estat de verificació; s'evita l'edició d'un superadministrador per este formulari i es confirma amb GET després del POST.
- Tant el perfil com la fotografia són protegits contra **canvi d'identitat** durant peticions asíncrones o durant el selector de fotos. No es reenvien automàticament operacions no idempotents després d'una resposta ambigua.
- El convidat és reconegut pel seu **ID real del servidor**, amb accés des de Perfil a Personalització, Notificacions i refresc de preferències del servidor; l'edició del perfil públic, del correu i de la foto està reservada als comptes registrats, igual que a Android. No es crea un compte professional fictici per a convidats.
- Sense canvis a Android v336, a l'Android incrustat ni a PHP; codi mantingut sobre els passos 1–33.

## Limitacions

- No s'ha verificat en una base de dades real que les instal·lacions tinguin els mateixos endpoints i restriccions. El perfil antic `account/profile-ios.php` no és el contracte primari de paritat.
- Un canvi de username pot afectar referències de fotos `owner_key` si el backend no reassigna el propietari. L'API iOS no inventa ni migra fotografies amb credencials sense consultar el servidor.
- Perfil públic editable només per a comptes registrats, com Android. Els convidats sí que tenen registre i preferències persistents en servidor, però no dades de correu o fotografia pròpies d'un compte registrat.
- No hi ha una compilació completa KMP/Xcode ni prova visual en iPhone. S'ha fet validació de mòdul de política Kotlin/JVM amb dades simulades, sintaxi Swift i regressió estàtica.

## Proves

- `COMPROVACIONS/comprovar_pas_01.py` a `comprovar_pas_34.py`: PASS.
- Kotlin/JVM real de `IosProfileParity.kt`: 25 assercions PASS, amb model de prova.
- `swiftc -frontend -parse` dels fitxers iOS: PASS.
- Immutabilitat Android v336 119 fitxers i Android incrustat v349 verificada amb SHA-256 dels ZIP originals: PASS.
- Compilació completa iPhone, integració MariaDB, pujada real d'imatges i confirmació d'un canvi de compte: PENDENTS.

**No es genera ZIP en el pas 34; pròxim ZIP acumulatiu en acabar el pas 36.**
