# L'Agenda iOS v350 — pas Feed «Per a tu» (29/10/2026)

## Comparació amb Android (font: `app/src/main/java/com/example/agendavalenciana/state/AgendaState.kt`)
- Android deixa `Sense filtre` automàticament si no hi ha cap senyal EXPLÍCIT; passa a `Per a tu` quan hi ha seguiments, categories o preferències territorials.
- Android té `show_suggestions=true` per defecte, però només usa descoberta quan hi ha senyals vàlids i està activada.
- Android considera seguits artistes, organitzadors i sales; categories/subcategories, localitzacions/comarques i radis; cerca/fitxes obertes només influeixen suggerències.
- Android manté un sistema de lectura de feed amb posició durant menys d'una hora fora de l'app.

## Aplicat en la còpia acumulativa v350 (sense ZIP)
1. `AgendaIosScreen.kt`: mode inicial `Sense filtre`; commutació automàtica entre `Sense filtre` / `Per a tu` només en funció de preferències/seguiments explícits. Respecta `Seguits`, `Categoria` i mode `Sense filtre` triats manualment i les cerques en curs.
2. `IosForYouModePolicy.kt`: política comuna, pura i comprovable per a senyals explícits i selecció del mode; utilitza `DefaultFeedPolicy` d'Android.
3. Suggerències activades per defecte si no existeix una preferència explícita `false`. Respecta l'elecció de l'usuari i el nivell de descoberta.
4. `IosGuestFollowStore.kt`: persistència local de seguiments de convidat, tipada, separada de la informació dels comptes autenticats i amb noms Unicode.
5. `AgendaIosScreen.kt`: `Seguir` funciona sense compte; `Seguits`, `Per a tu` i Batecs poden consultar estos seguiments. En restaurar un compte, es buida l'estat convidat de la UI abans de carregar el perfil real; en tancar sessió es recuperen seguiments anònims locals.
6. `IosPersonalizationScreen.kt`: `Seguir` sense autenticació en la pestanya Artistes i correcció de l'avís de convidat; es manté l'obligació d'autenticar-se per a les campanetes d'artistes al servidor.
7. `IosPersonalFeedPolicy.kt` i `IosForYouSnapshot.kt`: valor per defecte de suggerències coherent amb Android.
8. `shared/src/commonTest/.../IosForYouHomeModeTest.kt`: proves per a preferències explícites, estats i serialització de seguiments.

## Comprovat
- 8 comprovacions de política de modes compilades amb `kotlinc` i executades localment: totes correctes.
- Comparació binària de 93 fitxers de l'aplicació Android contra el ZIP anterior: cap modificació.
- Revisió estàtica del codi i fitxers nous.

## No verificat o pendent
- Compilació total de Kotlin/Native i Swift amb Codemagic; no afirmar que funciona en iPhone fins a provar-la.
- Prova d'escriptura i lectura del magatzem JSON dels seguiments en iOS real.
- Fusió explícita/sincronització de seguiments anònims en iniciar sessió (no s'ha activat automàticament per no mesclar dades de convidat i comptes sense confirmar el flux d'Android).
- Reproduir la conservació exacta de la posició de lectura (fins a una hora fora de l'app) de l'Android.
- Provar contra l'API real els efectes de les preferències, campanyes i entrades internes.
