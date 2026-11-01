# Pas 35/48 · Accés, vinculació del convidat, tancament i baixa (iOS v349)

## Implementat en codi, acumulat sobre 1–34

- Apple: flux natiu `ASAuthorizationAppleIDProvider`, nonce SHA-256, ID token comprovat **pel servidor** en `auth/apple-ios.php` de la carpeta complementària `SERVIDOR_PERFIL_IOS`.
- Google: OAuth iOS amb `ASWebAuthenticationSession`, `state`, nonce i PKCE S256, verificació posterior del token **pel servidor** en `auth/google-ios.php`. Necessita Client ID/reversed Client ID reals en el projecte i servidor.
- Mai s'utilitza l'ID token d'Apple o Google com a bearer de l'Agenda. Únicament s'accepta un bearer de MariaDB del format esperat, i es comprova que s'ha guardat i es pot rellegir al Keychain.
- Les operacions de sessió (`auth/me.php`, `auth/login.php`, `auth/register.php`, Apple/Google, `auth/logout.php`, `account/delete-ios.php`) són serialitzades al client: un retorn HTTP tardà no ha d'esborrar la sessió d'un altre compte. Els POST d'autenticació no hereten el bearer preexistent.
- Convidats: `auth/register.php` preserva ID i notificacions quan el PHP confirma conversió `guest_upgraded_in_place`; per a iniciar sessió en un compte ja existent el procés de `guest/claim.php` del pas 3 fa la fusió al servidor, elimina la credencial de convidat **només després de la confirmació** i la reintenta si falla. Això s'aplica també a l'accés social perquè les pantalles reactiven `claimPendingGuestIdentity` en canviar el compte.
- El tancament de sessió intenta la baixa APNs i `auth/logout.php` amb el bearer congelat, esborra el bearer del Keychain fins i tot sense connexió, purga preferències efímeres del compte en dispositiu i retorna a un convidat real del servidor. **No elimina** les accions offline xifrades i pendents del compte: no s'han de perdre sense confirmació.
- Baixa: text literal `ELIMINAR`, contrasenya quan el servidor ho exigix, `account/delete-ios.php` amb bearer registrat, `deleted=true` i `mode` conegut; només aleshores es neteja l'accés i s'eliminen les accions offline del compte eliminat (mai les d'altres comptes). Les dades fiscalment exigibles poden quedar anonimitzades conforme al resultat del backend. El superadministrador té la baixa bloquejada pel servidor fins transferir el rol.
- Una resposta social que arribe després de tancar el diàleg no inicia sessió; s'anul·la també la sessió si el diàleg es tanca durant l'intercanvi amb el servidor. Després d'un canvi de compte es reinicien els filtres personals abans de recarregar-los del servidor.

## Proves i límits

- 35 scripts de regressió estàtica sobre passos 1–35.
- Codi real de política compilat com Kotlin/JVM: 20 assercions; cua real Kotlin/JVM amb transport i JSON simulats, prova de purga selectiva després de baixa.
- Swift `-frontend -parse`; PHP `-l`; comprovació SHA Android v336 (119 fitxers) i Android incrustat al projecte.
- **No** compilació completa Kotlin/Native + Xcode, ni prova visual/funcional en iPhone, ni HTTP/MariaDB real ni configuració de credencials OAuth i Apple en producció. No afirmar que la vinculació social o la baixa estan operatives en producció.
- Els endpoints Apple/Google i baixa són del paquet **NO DESPLEGAT/NO VERIFICAT** `SERVIDOR_PERFIL_IOS`; requereix executar `001_PROFILE_AUTH_IOS.sql` i configurar les credencials i l'associació d'app de cada proveïdor. No s'ha modificat el servidor original ni Android.
- El client no pot provar la verificació criptogràfica real dels ID tokens ni la fusió del convidat sense infraestructura externa; depenen del backend. Un `guest/claim.php` parcial/indisponible manté la credencial convidada per reintentar.

**No es genera ZIP en el pas 35.** Pròxim ZIP complet 1–36, després del pas 36.
