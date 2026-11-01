# Pas 14 / 48 — Càrrega, cache, refresc i posició de lectura

**Base de treball:** iOS v349 acumulat, passos 1–13. Android v336 és referència immutable i el servidor no es modifica.

## Implementació en codi

- Primera tanda 5, segona 20 (5+15), posteriors de 10 amb sentinel real de `LazyColumn`; les insercions comercials no alteren el recompte orgànic.
- `EventCatalog.get(force=false)` manté la cache pública de 30 min (inclòs primer avanç de 50 fitxes) i `refresh(true)` invalida a petició explícita. No es força cap refresc de catàleg en canvis de pestanya.
- `IosFeedReadingSession.kt` guarda una fotografia de lectura només en RAM i la persisteix exclusivament a `sync/state.php` del servidor amb clau `ios_feed_reading_session_v1` (format Android `{t:string,v:JSON}`); peticions amb cua xifrada del pas 6. Inclou `ownerId`, `viewKey`, `visibleCount`, `firstIndex`, `firstOffset`, `lastActiveAt`, `backgroundAt`. No es guarda cap posició en `UserDefaults`.
- En iniciar/recuperar la sessió, recupera la posició i l'offset en píxels si la identitat/vista coincideixen i l'absència és inferior a una hora; amplia el lot abans de reposicionar la llista, també si el catàleg arriba tard. Lectures tardanes no desfan scrolls més recents.
- El listener de fase SwiftUI (iOS 16 compatible) avisa de foreground/background; dins de l'app, transcórrer una hora sense eixir no caduca la lectura. Si el procés és matat abans de confirmar el fons, `lastActiveAt` és un límit conservador.
- El gest manual es diferencia de les actualitzacions automàtiques; manté les targetes mentre refà el catàleg. La barra discreta es mostra en refresc manual, no per la sincronització periòdica.

## Precisions i limitacions

1. **Diferència real d'Android v336:** a `AgendaApp.kt` es troba `FEED_PAGE_SIZE=100`, `FEED_SCROLL_BATCH=10`; no és literalment 5 + 15 + 10. Ací s'aplica 5 + 15 + 10 per instrucció expressa de l'usuari, sense canviar Android.
2. La persistència a servidor depèn de l'API i de la cua offline; en iOS, el procés pot ser suspés abans que es confirme l'últim scroll en entrar en segon pla. L'última posició confirmada preval després d'un rellançament. Una prova de dispositiu real és imprescindible.
3. El servidor guarda una clau pròpia de posició de lectura d'iOS; això no sincronitza la posició del feed amb Android. El catàleg i les dades personals continuen compartits; Android no es modifica.
4. Cache pública del catàleg existent, no una segona base de dades personal local. Solament es conserven al dispositiu els pendents xifrats del pas 6.
5. No hi ha compilació Kotlin/Native/Xcode ni proves contra PHP/MariaDB reals. S'han executat proves de política Kotlin/JVM amb JSON simulat i verificacions estàtiques.

**Lliurament:** no generar ZIP al pas 14; pròxim ZIP després del pas 15.
