# L'Agenda v350 — fusió de seguiments del convidat en iniciar sessió

## Referència Android
- `ANDROID_STUDIO/app/src/main/java/com/example/agendavalenciana/state/AgendaState.kt`: `mergeGuestStatePayload` (unió compte ∪ convidat), migració dels `followed_artists`, `followed_organizers`, `followed_venues` i `followed_categories`; només es pugen les diferències.
- `ANDROID_STUDIO/app/src/main/java/com/example/agendavalenciana/data/ServerApiRepository.kt`: API existent del servidor per a l'estat d'usuari.

## Aplicat a iOS (còpia de treball v350, sense ZIP)
1. **Persistència real per a convidats:** `IosGuestFollowTransfer.kt` guarda seguiments locals per ARTIST, ORGANIZER, VENUE i CATEGORY, amb serialització segura per a noms Unicode i salts de línia. `AgendaIosScreen.kt` els recupera en obrir l'app i actualitza l'estat quan es toca Seguir. `IosPersonalizationScreen.kt` també permet seguir i deixar de seguir artistes sense iniciar sessió, conservant l'exigència de compte per a les campanetes del servidor.
2. **Fusió additiva:** inici de sessió amb contrasenya, registre, Apple/Google i restauració de sessió preparen un *journal* per compte (`ios_pending_guest_follows_v350_<id>`). La còpia del convidat s'esborra només després que les dades pendents s'hagen pogut rellegir del journal.
3. **Protecció dels comptes:** l'estat remot autoritatiu es conserva, amb afegit de les entrades pendents. Una càrrega del servidor no esborra els seguiments pendents. Els pendents del compte A no apareixen en el compte B.
4. **Sincronització:** es consulta `user-state/mine.php` i s'envien només les entrades absents a `user-state/follow.php` amb `following=true`. Cada confirmació s'elimina individualment del journal; fallades de xarxa mantenen les entrades restants per a intents posteriors.
5. **Evita reactivacions indegudes:** deixar de seguir una entitat autenticada retira el seu pendent després que el servidor confirme l'acció. Les crides de sincronització i d'edició manual compartixen un mutex.
6. **Categories i subcategories:** les seleccions fetes com a convidat s'incorporen a la mateixa fusió. Dues subcategories amb la mateixa etiqueta però codis diferents es preserven independentment. Les preferències de categories del convidat s'esborren només després d'una etapa durable, per no importar-les en iniciar una sessió amb un altre compte.
7. **Sense canvis de servidor:** utilitza els mateixos endpoints ja emprats per Android, sense noves migracions SQL.

## Comprovacions efectuades
- 15 proves executables sobre la fusió, deduplicació, accents, categories homònimes, fusió compte+convidat, persistència de pendents, comptes independents i errors d'escriptura.
- Android: els 93 fitxers del directori `ANDROID_STUDIO/app/` són idèntics al ZIP v350 de referència.
- Inspecció estàtica dels punts d'inici de sessió, registre, restauració, actualització i tancament de sessió.

## Pendent de verificar
- No s'ha compilat el framework Kotlin/Native ni el projecte complet amb Xcode.
- Cal provar `user-state/mine.php` i `user-state/follow.php` contra el servidor real, incloent-hi una desconnexió i posterior reconnexió.
- Cal verificar que el magatzem `readCache/writeCache` del pont Swift persisteix realment en dispositiu iOS.
- La migració de llocs, guardats, campanetes i altres preferències no forma part d'este pas de *seguiments*.
