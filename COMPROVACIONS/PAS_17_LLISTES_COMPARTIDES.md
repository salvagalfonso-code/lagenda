# Pas 17/48 — Llistes personals i compartides (iOS)

## Implementat sobre passos 1–16
- `IosAgendaApi.kt`: ús autenticat de **la mateixa API Android** `GET/POST collections/index.php` amb Bearer de convidat o registrat. L'ID de servidor és la identitat de cada llista; cap llista permanent a `UserDefaults`.
- `IosSavedListContract.kt`: decoder estricte de `lists` (id, name, owner, share_code, invite_url, event_ids), comprovació de permisos **abans** de la mutació; lectura posterior del servidor per confirmar create, rename, delete, list_event, share, leave i revoke. Manté el mateix `list_id` i `event_id` d'Android.
- Les modificacions de llistes estan serialitzades per `Mutex`; l'ID d'usuari i Bearer s'immobilitzen durant l'operació i es revaliden després de cada POST/GET. Un GET d'un compte antic no pot reassignar llistes d'un compte nou.
- `IosSavedAgendaScreen.kt`: el convidat pot crear, anomenar, modificar, eliminar, compartir per codi/enllaç, unir-se a una llista compartida, eixir-ne i revocar codis. El contingut d'una llista rebuda només és de lectura. «Revocar» impedix **noves** incorporacions, però no elimina membres ja existents (igual que PHP/Android).
- Compartir sempre demana el codi vigent al servidor i torna a recuperar les llistes. El format d'invitació és `https://agendaval.com/app/lists.php?invite=...`, compatible amb `IosSavedListInvitation` i Android.
- `AgendaIosScreen.kt`: `savedListsRevision` protegix contra GET obsolets quan ja hi ha una actualització confirmada al mateix compte.
- A més de crear i esborrar llistes, `list_event` permet incorporar/eliminar esdeveniments publicats sense modificar un llistat compartit alié. El guardat **General** es manté independent en `events/save.php`, pas 16.

## Proves disponibles
- `COMPROVACIONS/comprovar_pas_01.py` … `comprovar_pas_17.py`: PASS.
- El fitxer de **producció** `IosSavedListContract.kt` compilat i executat com Kotlin/JVM amb JSON de prova: **28 escenaris PASS** (crear, renombrar, esborrar, editar elements, compartició, revocació, permisos, convidat, membre, respostes invàlides).
- `swiftc -frontend -parse` de tots els Swift de `iosApp`: PASS.
- Android v336 i `app/` Android integrat en v349: comparació contra els ZIP originals, sense canvis.
- Proves `IosSavedListContractTest.kt` escrites a commonTest, però **no executades amb Gradle**.

## Limitacions
- **Sense compilació completa Kotlin/Native ni Xcode**, ni proves reals de PHP, MariaDB o sincronització entre dos dispositius. En aquest entorn Gradle no està disponible per a compilar el projecte.
- `collections/index.php` **no retorna `list_id`** després de `join_list`. Si el compte ja era membre, el servidor confirma l'operació (`ok=true`) i el GET no presenta cap canvi; no es pot demostrar en client a quina llista corresponia el codi sense modificar l'API. La lectura es valida com a coherent, però no és confirmació independent del codi.
- Un POST de `create_list` que s'haja aplicat però perda la resposta no és segur per repetir automàticament: la cua offline del pas 6 **no** reenvia creacions de llistes; cal recuperar el GET i comprovar visualment abans d'intentar crear de nou. Mateix criteri de dades centralitzades, evitant duplicacions.
- No s'han desplegat canvis al servidor ni migracions; el funcionament exigix que l'API de llistes compartides i les taules associades existisquen al servidor real.

**No s'ha generat ZIP en el pas 17.** El pròxim ZIP complet acumulatiu correspon al pas 18.
