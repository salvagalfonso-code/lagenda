# Pas 47/48 · Superadministració, controls d'accés i permisos

## Canvis en el codi acumulatiu iOS v349

1. API administrativa separada `adminRead`/`adminWrite` amb sessió registrada `SUPER_ADMIN` i bearer congelat abans de la xarxa. Després de la petició es comprova el mateix actor, rol i token; una resposta endarrerida no pot alterar la pantalla després d'un canvi de compte. La comprovació efectiva de privilegis continua corresponent al PHP.
2. Protecció d'accions administratives amb `adminMutationMutex` per no solapar canvis de rol, baixes, moderació, revisions i enviaments de campanyes. Cap POST no idempotent es reintenta automàticament.
3. Canvi de rol i estat: prohibició de modificar-se a si mateix, validació dels rols del backend, comprovació prèvia de la identitat del destinatari, resposta de `admin/user-access-ios.php` i relectura de `admin/users.php`. **Necessita desplegar el PHP i la migració 001_SUPERADMIN_IOS.sql que ja constaven com a material auxiliar; no s'han instal·lat al servidor.**
4. Baixa/anonimització administrativa: verificació del destinatari, confirmació obligatòria `ELIMINAR <username>` i relectura del llistat. Si la postlectura no acredita la baixa, es bloqueja la confirmació i es demana comprovació manual.
5. Revisió de publicacions: l'ID ha de figurar entre els pendents, es valida la decisió i el motiu, i es comprova que ha desaparegut de la cua. Reclamacions professionals: confirmació expressa en pantalla, justificació en rebuig i comprovació posterior de la cua.
6. Moderació: `APPROVE`, `REMOVE_CONFIRMED` i `RESTORE`, amb motiu detallat per retirar; la UI diferencia que el servidor haja admés l'acció de la verificació del resultat final. Una moderació no equival a la resolució formal d'una denúncia.
7. Campanyes internes: per cada enviament es torna a previsualitzar el nombre de destinataris; si canvia, no s'envia. L'enviament es comprova contra `recipient_count`; no s'afirma entrega APNs/FCM.
8. Els llistats administratius de la consola són estat efímer, reinicialitzat per l'ID de l'administrador. No hi ha cap caché local persistent nova per a dades personals de tercers.

## Fitxers afectats pel pas 47

- `shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosAgendaApi.kt` (modificat)
- `shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosSuperAdminConsole.kt` (modificat)
- `shared/src/commonMain/kotlin/com/example/agendavalenciana/ios/IosAdminActionPolicy.kt` (nou)
- `COMPROVACIONS/provar_pas_47.kt` i `COMPROVACIONS/comprovar_pas_47.py` (nous)

## Verificació executada

- 148 assercions Kotlin/JVM sobre rols, autocanvi, autorització, aprovació, retirada, reclamacions i campanyes: **PASS**.
- 17 proves del model preexistent de superadmin (v343): **PASS**.
- Comprovació estàtica de 14 rutes API, proteccions de sessió i control de confirmació: **PASS**.
- Integritat respecte del ZIP 15/16: 481 fitxers originals preservats, 7 amb canvis entre passos 46–47, acotats a iOS/registre. Android i servidor de referència no modificats: **PASS**.
- `swiftc -frontend -parse AgendaKeychain.swift`: **PASS** (únicament anàlisi sintàctica).
- `php -l` del servidor auxiliar `user-access-ios.php`: **PASS** (no està desplegat ni integrat en un MariaDB real).

## No verificat, no donar per complet en producció

- La compilació total KMP i Xcode, l'execució en iPhone i la correcció de la UX nativa, ja que no hi ha entorn Xcode/macos ni Gradle funcional en esta sessió.
- Permisos i comportament reals dels PHP desplegats: una comprovació estàtica no prova que tots els endpoints a producció retornen 401/403 correctament. Cal provar amb sessió convidada, usuari, professional, superadmin i administradors revocats, i auditar accions en MariaDB.
- El mecanisme de canvis de rol requereix desplegar la migració SQL i PHP auxiliares ja inclosos; fins aleshores fallarà amb la ruta absent o migració requerida.
- L'anonimització es verifica conservadorament. Si la resposta del servidor té altre format, cal adaptar el criteri abans de declarar-ho provat.
- Les denúncies (`admin/reports.php`) són només consulta en la consola: no s'ha inventat una API de resolució formal. Les campanyes administratives són per a bústia interna, no push massiu.
- El pagament pendent després de tancar completament l'app (passos 38, 44, 45, 46) continua pendent de contracte backend recuperable.

**Estat:** pas 47 implementat al codi iOS acumulatiu, compilació integral i permisos reals pendents. **No es crea ZIP al pas 47.** El ZIP 16/16 s'entrega després del pas 48, una vegada feta l'auditoria final disponible.
