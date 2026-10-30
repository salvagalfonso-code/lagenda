# v350 — Navegació sense sessió (Guardats, Entrades, Comunitat)

Referència verificada: `ANDROID_STUDIO/app/src/main/java/com/example/agendavalenciana/ui/AgendaApp.kt`, `SavedCollectionsUi.kt`, `TicketingUi.kt` i `state/AgendaState.kt`.

## Implementat en codi iOS (sense increment de versió)
- Guardats accessible en sessió anònima: pantalla d'agendes amb General, actes guardats i horaris de festival desats localment.
- Guardar/desar esdeveniment anònim sense mostrar inici de sessió; persisteix en cache `guest_saved_events_guest` i és segregat dels comptes.
- Recordatoris locals iOS per a convidat, utilitzant identificador de notificació 0; la campaneta guarda igualment l'acte en General. El backend no rep cap avís autenticat per al convidat.
- Nova llista i llistes compartides continuen requerint compte, amb accés voluntari al login.
- Entrades accessible per a convidat amb estat buit i opció d'iniciar sessió per recuperar compres del compte; no es mostren QR privats ni s'executa `tickets/mine.php` sense sessió.
- Comunitat accessible per a convidat. Els taulons propis, missatgeria i participació continuen protegits per autenticació (`community/groups.php` en el patch PHP requereix `community_user()`).
- Des del popup d'esdeveniment, Comunitat obri la secció fins i tot sense sessió.
- En tancar sessió o eliminar compte, retorna la selecció de Guardats i avisos anònims conservats; no filtra dades de l'usuari anterior.

## Limitacions pendents
- Compilació Xcode i verificació real en iPhone/simulador.
- Entrades de convidat: el backend actual consulta `tickets/mine.php` amb autenticació; no s'han implementat compres/QR anònims inexistents.
- Comunitat: el guest pot navegar a la pantalla, però l'API de taulons/missatges requereix sessió; no s'ha relaxat cap permís del servidor.
- Migració de guardats i recordatoris anònims a un compte quan s'inicia sessió, per fer paritat completa amb l'Android.

## Comprovacions locals
- Vuit comprovacions estructurals satisfactòries.
- Compilació i execució de prova Kotlin de `IosAccountCachePolicy` satisfactòria.
- 91 fitxers Android comparats amb el ZIP v350 anterior: cap modificació ni absència.
