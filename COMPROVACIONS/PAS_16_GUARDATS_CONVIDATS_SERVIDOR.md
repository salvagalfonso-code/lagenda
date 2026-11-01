# Pas 16/48 — Guardar i eliminar esdeveniments: convidats i registrats

## Base / abast
- Còpia acumulativa iOS v349 amb passos **1–15 conservats**; pas 16 afegit sense crear ZIP.
- **Android v336 immutable** i Android empaquetat en iOS v349 immutable. Cap canvi a PHP ni a MariaDB.
- No s'estenen les funcions de **llistes compartides** (pas 17) ni horaris de festival (pas 18).

## Mateix contracte que Android
- `POST events/save.php`: `event_id` canònic de l'esdeveniment, `saved` booleà, `entity_label` i metadades de context disponibles en el catàleg (poble, categoria, artistes, estil, organitzador, llengua). No utilitza `feedOccurrenceDate` com a identificador.
- `GET user-state/mine.php`: `event_states[*].event_id` i `saved` es transformen en `IosUserState.saved` tant amb Bearer de convidat com de registrat.
- `GET events/saved.php`: comprovació posterior que l'ID realment apareix/desapareix de la llista de MariaDB **abans de buidar la cua**. La resposta POST ha de donar `ok=true`, `saved` amb el valor demanat i `saved_count` no negatiu.
- `IosPendingServerQueue` conserva les altes/baixes en un fitxer xifrat per usuari si falla qualsevol comprovació HTTP/GET. Les peticions pendents es projecten **en ordre** sobre les lectures antigues, però l'estat només s'etiqueta com a «sincronitzat» després de confirmar amb el servidor.
- La pantalla `Guardats` és accessible per a usuaris convidats amb identitat del servidor; ja no mostra forçosament el formulari de registre. Es recuperen els guardats en entrar en la pestanya.
- Un canvi de compte o una lectura que comença abans d'una edició no pot esborrar una selecció més recent; la UI utilitza ID de propietari, `savedEditRevision`, i evita dos clics simultanis en el mateix esdeveniment.

## Verificacions realitzades
- `COMPROVACIONS/comprovar_pas_01.py` fins `comprovar_pas_16.py`: **16/16 PASS**. El pas 10 s'ha actualitzat només en l'assert de regressió del nom `reconcilePendingUserState` (inclou la reconciliació de seguiments anterior).
- Compilació real del **fitxer Kotlin de producció** `IosSavedEventContract.kt` amb dobles JSON mínims: **27 escenaris JVM PASS** (confirmacions, IDs, accions oposades, ordre, errors).
- Compilació real de `IosPendingServerQueue.kt` amb model JSON simulat i coroutines: **PASS** (cua de guardats separada per usuari, reintents, fusió confirmada i ordre).
- `swiftc -frontend -parse` del codi Swift: **PASS**.
- Comparació byte a byte dels arxius Android de v336 i Android original empaquetat a v349: **PASS**.
- Integritat dels passos anteriors: en la còpia 1–15 només canvien `AgendaIosScreen.kt`, `IosAgendaApi.kt`, `IosPendingServerQueue.kt` i un assert del script del pas 10; els altres 317 fitxers del ZIP 1–15 no s'han modificat.

## Pendents i límits
- **No compilació completa KMP/iPhone**: el wrapper `gradlew` no pot accedir a `services.gradle.org` (DNS). La sintaxi Swift i les proves JVM no garanteixen el build d'iOS.
- **No prova real** d'identitats convidat/registrat, sincronització Android ↔ iOS, servidor PHP ni MariaDB. No s'ha escrit cap dada en producció.
- El PHP `events/save.php` és funcionalment idempotent en el valor `saved`, però registra afinitats per cada petició; si un POST té èxit i el GET de comprovació falla, el reintent pot tornar a registrar el senyal d'afinitat. Sense canviar PHP, no es pot garantir idempotència del senyal intern per UUID.
- Sense connexió en el primer ús, iOS necessita haver rebut abans la identitat de convidat del servidor per poder encolar l'acció en el seu nom. No s'inventa un usuari local alternatiu.
- Els guardats de llistes i els slots de festival es tractaran en els passos 17–18.

**Pròxim:** pas 17 — llistes de guardats; **pròxim ZIP acumulatiu en acabar el pas 18**.
