# L’Agenda iOS — tancament funcional abans de Xcode (base v343)

## Objectiu
Completar fluxos operatius, no incrementar el número de versió. El projecte Android és el mateix que al paquet v343 anterior; s’han ampliat **només** `shared` i la documentació.

## Bloc 1: Guardats > Llistes — implementat al client i connectat al contracte PHP

Contracte verificat contra el codi de servidor `api/collections/index.php` aportat per l’usuari:

- Consultar llistes pròpies i compartides (`GET ?action=list`).
- Crear una llista (`create_list`).
- Canviar-ne el nom (`rename_list`).
- Eliminar una llista pròpia, amb confirmació (`delete_list`).
- Afegir i llevar esdeveniments publicats (`list_event`).
- Obtenir codi i enllaç oficial de compartició (`share_list`).
- Unir-se amb codi, token o enllaç oficial (`join_list`).
- Abandonar una llista rebuda (`leave_list`).
- Obrir fitxes de la llista que siguen presents al catàleg actual.
- La UI distingix les llistes pròpies (editables) de les compartides (consulta/abandó). L’API comprova permisos al servidor.
- La càrrega i les actualitzacions de llistes es vinculen al compte actual per no mostrar dades d’una sessió anterior.

**No verificat**: resposta HTTP de producció ni compilació Compose iOS. Una activitat no carregada es mostra com a no disponible, sense inventar dades ni eliminar-la de la llista.

## Bloc 2: Perfil públic/professional — implementat al client i connectat al contracte PHP

Contracte verificat contra `api/account/profile-settings.php`:

- Consulta del rol, telèfon i verificació del correu.
- Edició de tipus `PUBLIC` o `PROFESSIONAL` i telèfon professional (7–24 caràcters admesos segons l’API).
- Preservació dels rols professionals històrics per part del servidor.
- No s’ofereix el canvi a un compte `SUPER_ADMIN`; l’API també ho prohibix.
- Després de guardar, actualització del rol visible en la sessió actual.

**No verificat**: resposta HTTP de producció ni compilació Compose iOS.

## Regressions verificades localment

1. 7 accions de llistes corresponen als casos reals del PHP original.
2. Camps de la resposta (`lists`, `event_ids`, `owner`, `invite_url`, `share_code`) corresponen al PHP original.
3. L’endpoint del perfil reconeix els camps sol·licitats.
4. 5 proves Kotlin executades per validar els codis i enllaços d’invitació, inclosa exclusió de dominis aliens.
5. Els 73 fitxers font d’Android comparats continuen idèntics als de la v343.
6. Anàlisi de sintaxi Kotlin sense incidències sintàctiques detectades en els fitxers modificats (no equival a compilar Compose).

## Què no s’ha d’anunciar com a complet

- Una compilació Kotlin/Native + Compose iOS + Xcode encara no s’ha executat.
- Compra d’entrades de pagament i Redsys/Stripe: pendents.
- Push APNs i Firebase/servidor: pendents.
- Mateix algorisme de recomanació personalitzada que Android: pendent.
- Paritat exacta de Comunitat, batecs, popups, notificacions, AgendaPRO: pendent.
- Eliminació de compte i autenticacions externes iOS: pendents.

## Primera compilació

Obri el projecte en un Mac amb Xcode i JDK compatible. Des de `ANDROID_STUDIO/`:

```bash
sh COMPROVACIONS/compilar_ios_mac.sh
```

El primer error real de Gradle/Kotlin/Swift determinarà les correccions de compatibilitat, sense alterar Android. **No s’ha fet cap desplegament ni migració de MariaDB.**
