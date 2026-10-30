# L’Agenda iOS v350 — pas menú «+» segons rol (acumulatiu)

## Codi introduït
- `AgendaIosScreen.kt`: el «+» deixa de redirigir a Perfil i obri un `ModalBottomSheet`; en obrir, refresca rol, confiança de publicació i esdeveniments de l'usuari des del servidor. La consulta controla canvis de compte i no reutilitza resultats d'un compte anterior.
- `IosPublisherMenuPolicy.kt`: estructura i regles del mateix menú d'Android (`PublishActionsSheet`): nou esdeveniment universal; edició i anuncis si existixen esdeveniments propis publicats; entrades, merch i donacions si compte professional; revisió i sol·licituds professionals només SUPER_ADMIN; enllaç per a reclamar perfil per a la resta.
- `IosPublisherMenuSheet.kt`: popup inferior blanc amb títol, subtítol, files amb icona groga, fletxa dreta, separador Revisió i reclamació al peu.
- `IosAgendaApi.kt`: GET `account/publisher-access.php`, `account/publication-trust.php`, `events/my-published-v312.php?guest_id=...`, POST `professional-profiles/request.php`.
- `IosGuestIdentity.kt`: UUID persistent per a les publicacions de convidats (no és una credencial).
- `IosPublisherDestinations.kt`: sol·licitud real de verificació professional amb formulari i endpoint d'Android; pantalles encara no migrades identificades explícitament en clicar, sense fingir accions.
- `AgendaVisualKit.kt`: pictogrames de les noves opcions.

## Limitació explícita
Este pas replica i connecta **el menú** i **la sol·licitud professional**. Les pantalles de creació d'esdeveniments, edició, anuncis, facturació, merch, donacions i les dues pantalles administratives encara requereixen la implementació funcional específica; actualment en tocar-les s'informa que estan pendents. No anunciar estes rutes com a funcionals.

## Proves
- `tests/TestIosPublisherMenu.kt`: 14 verificacions de combinacions de rol i autoria superades amb `kotlinc`.
- Android app: fitxers originals idèntics a v349; la lògica iOS s'ha canviat només dins `shared`.
- No s'ha pogut executar Xcode/Kotlin-Native en este entorn: validació d'aplicació i desplegament real pendents.
- No s'ha generat cap ZIP ni canviat el número de versió.
