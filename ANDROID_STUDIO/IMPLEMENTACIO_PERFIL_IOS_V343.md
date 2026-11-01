# L’AGENDA — v343 acumulada · Perfil iOS

## Canvis implementats en codi

1. Perfil amb avatar (fotografia o inicial), nom i correu; càrrega del perfil del servidor quan es recupera/inicia sessió.
2. Edició de nom visible i biografia via `account/profile-ios-edit.php`, independent de dades professionals; botó per retirar foto.
3. Selector natiu iOS `PHPickerViewController`, redimensiona fins a 1.280 px i convertix a JPEG; pujada autenticada a l’endpoint EXISTENT `images/upload.php`, amb límit 6 MB i control de propietat. No s’emmagatzemen imatges en UserDefaults.
4. Accés Apple mitjançant `AuthenticationServices`: nonce SHA-256, ID token, verificació servidor (firma RSA/JWKS, emissor, audience, caducitat, nonce i un sol ús) i sessió pròpia al Keychain.
5. Accés Google amb `ASWebAuthenticationSession`, OAuth 2.0 Code + PKCE S256, state i nonce, ID token comprovat al servidor amb l’audiència iOS; token API propi al Keychain. Evita fer servir contrasenyes de Google.
6. Baixa explícita des del Perfil: text `ELIMINAR`, contrasenya actual quan el compte en té, revocació de tokens, retirada d’identitats socials i preferències, baixa de notificacions APNs, neteja de dades locals. Intent d’eliminació física amb les FK; si una relació exigix conservar-la, anonimització i bloqueig permanent de l’usuari, sense destruir les dades de facturació i moderació legalment exigibles.

## Fitxers afegits

- `ANDROID_STUDIO/iosApp/AgendaIOS/AgendaProfileNative.swift`
- `SERVIDOR_PERFIL_IOS/auth/apple-ios.php`
- `SERVIDOR_PERFIL_IOS/auth/google-ios.php` (adaptació controlada de l'endpoint Google web ja existent)
- `SERVIDOR_PERFIL_IOS/account/profile-ios.php`
- `SERVIDOR_PERFIL_IOS/account/profile-ios-edit.php`
- `SERVIDOR_PERFIL_IOS/account/delete-ios.php`
- `SERVIDOR_PERFIL_IOS/001_PROFILE_AUTH_IOS.sql`

## Desplegament NECESSARI (no fet)

1. Crear còpia de seguretat de MariaDB i dels fitxers de servidor.
2. Executar `001_PROFILE_AUTH_IOS.sql` en la base de dades original.
3. Copiar els cinc fitxers PHP a `app/api/auth/` i `app/api/account/` segons la ruta interior, **sense substituir els PHP d'Android**. `images/upload.php` ja és necessari al servidor original.
4. Configurar al servidor `AGENDA_APPLE_IOS_BUNDLE_ID=com.agendavalenciana.app.ios` i `AGENDA_GOOGLE_IOS_CLIENT_ID` (OAuth 2.0 client iOS real).
5. Al projecte Xcode, definir `AGENDA_GOOGLE_IOS_CLIENT_ID` i `AGENDA_GOOGLE_REVERSED_CLIENT_ID` amb el client iOS de Google. El segon és l’esquema `com.googleusercontent.apps.<id>` i ha de coincidir amb el registre a Google Cloud Console.
6. Activar Sign in with Apple, Team ID, Bundle Identifier i els entitlements correctes en Apple Developer/Xcode. La signatura, les credencials i l’autorització han d’estar configurades abans de provar.
7. Provar Apple/Google amb usuaris nous i existents, sessions revocades, error de xarxa, emails coincidents i token reutilitzat. Verificar la baixa amb i sense relacions econòmiques amb una base de dades de proves i validar la política de conservació i purga de dades.

## Limitacions verificables

- **No s'ha compilat l'aplicació amb Xcode**, ni es pot garantir la conformitat de la declaració KMP ↔ Swift fins que s'execute el build al Mac.
- **No hi ha proves de login OAuth real, fotos reals ni base de dades de producció** perquè les credencials i el servidor no estan a l'abast.
- La via Google exigix comprovar que el redirect URI natiu i el client OAuth es corresponguen exactament amb l'aplicació signada; encara no s'ha validat en iPhone.
- La baixa no elimina documents fiscals que legalment s'hagen de conservar; la còpia física de fitxers antics o altres dades no referenciades pot requerir neteja específica per a compliment de privacitat. El servidor no ha rebut cap mutació de dades reals.
- Els comptes superadministradors han de transferir primer el rol abans de donar-se de baixa.
- Falten proves de reautenticació/social linking per a un correu ja associat a una altra identitat; per defecte l’accés Apple no vincula automàticament comptes amb el mateix correu (evita un enllaç insegur).

## Proves locals

`swiftc -frontend -parse` dels fitxers Swift; `plutil -lint` de plist i entitlements; `php -l` dels PHP; test OpenSSL de reconstrucció RSA JWK; preservació exacta d’Android i verificació de ZIP. Estes proves **no equivalen** a un build iOS ni a integració de xarxa.
