# Pas 36/48 — Rols, permisos, menú «+» i accés professional

## Font comprovada
Android v336: `AgendaApp.kt` `PublishActionsSheet`, `AgendaState.kt` `refreshPublisherAccess`, `refreshPublicationTrust`, `canAutoPublishEvents`, `ServerApiRepository.kt`. API PHP en `web.zip` (snapshot del servidor, **no prova de desplegament**).

## Implementat en codi iOS v349
- `+` obri un diàleg de publicació per a convidats i registrats, ja no obri Perfil.
- Cada obertura consulta `events/my-published-v312.php` i, per als registrats, `account/publisher-access.php` i `account/publication-trust.php`. Gating per `serverConfirmed`, identitat/token congelats i refresc; superadmin no es deduïx de configuració de perfil.
- Rol usuari/guest: pot publicar activitats amb `events/community-submit.php`; edició amb `events/community-update.php`, comprovant autoria i resposta canònica. Sense auto-reintentar POST no idempotents. Categoria cultural multidisciplinària.
- Després de publicar una activitat pròpia, es mostra «Editar esdeveniments» i «Crear anunci» (mateixa condició que Android). L'entrada d'anuncis no crea pagaments ni campanyes incompletes (vegeu limitacions).
- Professional verificat per rol d'API o perfil reclamat: accés al menú d'entrades, ingressos, merch i donacions. Els convidats veuen «Soc organitzador o artista» però han d'iniciar sessió per enviar la reclamació. Després de publicar, un usuari registrat sense rol professional pot obrir «Activar venda» i completar el titular fiscal abans de rebre el rol professional del servidor. La reclamació utilitza `professional-profiles/request.php`; no concedeix rols automàticament.
- Per a entrades: consulta real de `seller-onboarding/index.php`, elecció d'esdeveniment propi, titular fiscal nou o existent, enllaços legals i acceptacions. POST fiscal amb GET de comprovació, sense activar automàticament entrades/pagaments.
- SUPER_ADMIN: dues entrades de revisió cap a la consola administrativa existent, supeditades al rol confirmat i als permisos del backend.
- `Perfil`: rol/professional verificat per resposta del servidor, validador PRO i superadmin no apareixen sols per un text de perfil local.

## Limitacions explícites
- El pas 36 **no** completa l'editor de campanyes pagades, el TPV, els tipus d'entrades, el catàleg de merch ni les donacions. Els botons d'accés ho indiquen i no executen cap compra fictícia. L'editor de publicació és funcional en camps bàsics, però no té encara la paritat visual ni totes les opcions avançades de l'editor Android (cartell i programació extensa).
- L'API `events/my-published-v312.php` i `seller-onboarding/index.php` han d'estar desplegades amb la versió PHP/SQL correcta; no s'ha comprovat MariaDB real.
- Signatura, compilació total Kotlin/Native/Xcode, ús en iPhone i alta de venda/operacions amb el backend real no verificades.
- Android v336 original i Android encastat en v349 no s'han modificat.

## Proves
`COMPROVACIONS/provar_pas_36.kt` + mòdul de política Kotlin compilat per JVM, 34 assercions; `COMPROVACIONS/comprovar_pas_36.py` compara rutes i hashes Android; scripts anteriors 1–35 i parse Swift. El ZIP 1–36 inclou tots els passos previs.
