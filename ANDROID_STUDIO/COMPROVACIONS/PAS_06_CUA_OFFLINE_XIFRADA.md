# Pas 6/48 · Cua temporal xifrada d'accions sense connexió — iOS v349

## Implementat en codi

- `IosPendingServerQueue.kt`: cua *write-ahead* i FIFO per identitat, amb mutex, identificadors estables, límit de 1.000 accions i 1 MB, `owner_id` i tipus; errors de lectura i de persistència interrompen el guardat per no perdre accions.
- `AgendaKeychain.swift`: emmagatzematge de la cua en un fitxer AES-256-GCM a `Application Support/AgendaOffline/pending-v1.aesgcm`, amb clau en Keychain `ThisDeviceOnly`, protecció de fitxer i exclusió de còpies de seguretat. No es guarden tokens Bearer ni preferències en clar al fitxer. Fitxer d'accions temporal, no font de veritat.
- `IosAgendaApi.kt`: escriptures en cua ANTES del POST, reintents amb el mateix payload i IDs (API `sync/state.php` ja té comprovants `APPLIED`/`ALREADY_APPLIED`), validació de `user_id`, revisió i IDs rebuts. Preferències de llocs/feed són POSTs de substitució idempotent amb confirmació de resposta. Peticions que fallin o no estiguen confirmades queden en cua.
- Operacions cobertes per la cua: `sync/state.php`, `preferences/locations.php`, `preferences/feed.php`, `events/save.php`, `user-state/event-alert.php` i `user-state/follow.php`.
- `IosGuestAccountLinker.kt`: canvia el propietari dels elements pendents de convidat a registrat sols **després** d'una fusió/upgrade confirmat pel servidor i abans d'esborrar la credencial del convidat; si el trasllat de la cua falla, conserva la credencial perquè es puga reintentar.
- `AgendaIosScreen.kt`: reintents a l'entrada, en canviar de pestanya i cada 60 segons mentre l'app està activa. Recarrega l'estat del servidor quan es reprodueix alguna acció confirmada. Si queda en cua, mostra **pendent de sincronitzar**, no «guardat al servidor».

## Verificacions executades

- `comprovar_pas_01.py` fins a `comprovar_pas_06.py`: PASS amb les rutes de queue noves (pas 05 actualitzat, el POST ja no és directe).
- `swiftc -frontend -parse` del pont Swift: PASS **sintaxi**, no compilació contra SDK iOS.
- `kotlinc` JVM 1.9: **el fitxer Kotlin real `IosPendingServerQueue.kt` compilat** junt a un JSON substitut de proves i coroutines JVM disponible. Prova de fum PASS sobre escriptura anticipada, fallada, reintent, identificadors estables, aïllament, fusió i fail-closed en error de lectura. **No és una prova de la serialització real** ni de Kotlin/Native.
- S'han afegit 6 tests de contracte Kotlin a `shared/src/commonTest/.../IosPendingServerQueueTest.kt`, però **no s'han executat** per manca de Gradle i dependències de plataforma en este entorn.
- Hash SHA-256 comparat amb ZIP originals: Android v336 119 fitxers idèntics; 93 fitxers de l'Android/servidor inclosos a v349 idèntics. Cap PHP ni SQL modificat.

## Límits i pendents abans de donar el sistema per verificat

1. No compilat framework Kotlin/Native ni app amb Xcode; no executat en simulador/iPhone. La sintaxi Swift no demostra que CryptoKit i el pont KMP estiguen correctament enllaçats.
2. No s'ha provat un POST real contra MariaDB, ni desconnexió/reconnexió real en iPhone ni la recuperació després d'una reinstal·lació. La cua està pensada només per a una instal·lació: no sobreviu a desinstal·lar l'app i **no és la font de dades**.
3. No es posen en cua accions destructives, enviament de missatges, compres, pagaments, publicació, validació d'entrades o operacions professionals. Les fases corresponents necessiten contractes específics; no fingim que totes les funcions estan offline.
4. Els POST de llocs i feed són operacions substitutives i idempotents, però **no** tenen comprovants de mutació de servidor: si una resposta es perd, es poden repetir; el resultat final hauria de mantindre's, però genera revisió nova en llocs. Les diverses API no formen una única transacció.
5. Els canvis fets offline en dos dispositius al mateix temps poden tenir conflictes d'ordre: no hi ha política de resolució per claus compartides més enllà de les regles de servidor. Android està congelat per decisió de producte.
6. Els canvis pendents d'un usuari registrat després de tancar sessió es conserven xifrats però NO es trameten amb credencials d'un compte diferent. Cal tornar a iniciar sessió amb el compte propietari. La fusió convidat→registrat és una excepció expressament verificada.
7. Una resposta `ok=true` confirma les operacions `events/save.php`, `user-state/follow.php` i `user-state/event-alert.php`; no hi ha comprovant d'idempotència amb UUID per a estes API, encara que les operacions siguen de tipus set/unset.
8. L'estat local de pantalles ja existents és memòria transitòria; les accions en cua es mostren com a pendents, no com a canvis confirmats en tots els popups. Requereix prova d'UX real.

**Criteri de tancament actual:** implementat en codi i verificat parcialment en proves locals; **NO compilat ni verificat en iPhone ni amb la base de dades real**.
