# L'Agenda iOS — Pas 3/48: vinculació del convidat i primer paquet acumulatiu

**Referència immutable:** Android v336. **iOS:** v349 + passos 1, 2 i 3. No s'ha desplegat codi al servidor.

## Implementat

1. El compte nou inclou `guest_token` en `auth/register.php`: el servidor el converteix **en el mateix `users.id`** (`guest_upgraded_in_place`), sense duplicar l'usuari.
2. Si s'inicia sessió en un compte existent (correu/contrasenya o Apple/Google), `IosGuestAccountLinker` utilitza **POST** `guest/claim.php` amb `Authorization: Bearer <token registrat>` i JSON `{"guest_token":"<token convidat>"}`.
3. Es verifica que la resposta `merged_guest_user_id` i `into_user_id` coincideix amb els dos IDs esperats, incloent `already_claimed=true`. Només després d'eixa confirmació s'esborra la credencial antiga de convidat del Keychain.
4. Si la fusió no funciona (connexió, resposta incongruent, error servidor o fallada del Keychain), les credencials no s'esborren. iOS reintenta cada 60 segons després d'autenticar-se, també quan recupera una sessió anterior. Un error de fusió no anul·la un inici de sessió vàlid.
5. Després d'una fusió confirmada, la pantalla recarrega les dades del compte des de les API i manté la sessió registrada. La transacció PHP del servidor fa la fusió real de les taules personals, seguidors, guardats, comunitat, historial i altres dades.
6. `IosGuestSessionTest.kt` adaptat a la neteja segura; afegit `IosGuestAccountLinkerTest.kt` amb 5 casos (èxit, error, idempotència, upgrade en lloc i error Keychain).

## Comprovacions executades

- `python COMPROVACIONS/comprovar_pas_01.py` — PASS.
- `python COMPROVACIONS/comprovar_pas_02.py` — PASS.
- `python COMPROVACIONS/comprovar_pas_03.py` — PASS, compara 119 fitxers Android v336 i 119 fitxers protegits Android/servidor de l'arxiu iOS original.
- `swiftc -frontend -parse iosApp/AgendaIOS/*.swift` — PASS, **únicament sintaxi**.
- Compilats amb `kotlinc-jvm` els **dos fitxers Kotlin de la lògica de convidats** i executats **5 escenaris** amb el `kotlinx-coroutines-core` disponible i un *stub* JSON — PASS. Això comprova la lògica de fusió, no la serialització real ni la integració amb l'API.

## Limitacions abans de considerar completada la sincronització multiplataforma

- **No verificat:** transacció real de `guest/claim.php` sobre MariaDB, compilació Kotlin/Native, enllaçat Swift/KMP, execució en simulador o iPhone. No es disposa de Xcode en aquest entorn.
- **El registre nou pot tindre una diferència de servidor a revisar:** `auth/register.php` fa un `ON DUPLICATE KEY UPDATE` de les preferències bàsiques de notificacions amb valors per defecte quan no s'envia `notifications`; això pot sobreescriure les del convidat en una conversió en lloc. No s'ha modificat PHP per mantindre intacte el servidor de referència. Cal abordar-ho abans de certificar «totes les dades» preservades.
- La paritat de totes les preferències, guardats i cues offline d'iOS està prevista en els **passos 4 a 21**; este pas només vincula identitats i aprofita les taules ja persistides al servidor. No converteix dades guardades únicament en `UserDefaults` en dades de MariaDB.
- `guest/claim.php` preserva preferències del compte registrat quan hi ha una clau coincident (`INSERT IGNORE`), no sobrescriu tot el compte amb les del convidat: és la política actual de l'API d'Android.

## Següent pas

**Pas 4:** carregar l'estat complet de l'usuari des del servidor, també convidat, sense perdre l'estat local fins a haver finalitzat la migració.
