# Pas 22 — Cercador d'artistes, activitats i horaris de festivals

**Abast:** solament iOS v349 acumulada. Android v336 i servidor immutables.

## Implementat en codi

- `IosFestivalArtistSearch.kt`: equivalència de la cerca artística d'Android per a nom exacte o subcadena inequívoca (no confondre una cerca de lloc/entitat explícita amb una d'artista); normalització d'accents; coincidència exacta d'artista al cartell/programa sense falsos positius per substring.
- `IosAdvancedSearch.kt`: indexa també artistes consignats al `title` quan la fila de programa no conté `artist_name`. Manté cerca multidisciplinària (activitats, llocs, organitzadors, sales, categories i estils) i intersecció estricta de filtres.
- `AgendaIosScreen.kt`: les cerques només s'apliquen quan es confirmen; el text teclejat no dispara recàlcul. Una cerca d'artista reconeguda usa l'entitat canònica i carrega els horaris publicats amb `special-events/public.php?event_id=` només per a festivals candidats del catàleg; màxim de 80 consultes per petició de cerca. L'horari validat es mostra com a context de la targeta i es fa servir per ordenar i obrir la jornada al popup. Una fitxa per festival, inclosa una mateixa fitxa amb publicitat i resultat orgànic, amb el seu ID original. Si no hi ha horari verificat no s'inventa data/hora/escenari.
- `EventDetailSheet`: focalitza primer el dia i destaca les sessions del grup per igualtat de nom normalitzat, no per coincidències parcials que puguen confondre dos artistes. Conserva navegació, guardar sessions i pestanyes d'un festival.
- Cap emmagatzematge personal local nou ni cap escriptura al servidor per als resultats de cerca. Només lecturas del catàleg i dels horaris publicats; els senyals d'afinitat de cerca existents es mantenen.

## Proves

- `comprovar_pas_22.py`: contractes i invariància de l'Android original.
- `IosFestivalArtistSearchTest.kt`: tests de regressió que es podran executar amb Gradle en macOS.
- `test_pas22_jvm.py`: compila **les classes de producció** `IosAdvancedSearch.kt` i `IosFestivalArtistSearch.kt` amb models de prova lleugers; 29 assertions superades.
- Comprovacions estàtiques de passos 1–21 també superades.

## Límits (no certificats)

- No hi ha compilació completa de KMP/SwiftUI ni simulador iPhone ni prova amb API/MariaDB de producció.
- La cerca troba artistes coneguts pel catàleg d'esdeveniments: si l'artista només existix en la configuració remota d'un festival i no està indexat en el cartell ni en el programa resumit, el servidor no oferix un endpoint global que permeta descobrir-lo des d'iOS sense inspeccionar tots els festivals. No s'han inventat resultats.
- La càrrega d'horaris es limita als primers 80 festivals candidats per a evitar saturar el servidor. Si la xarxa falla, resta el programa resumit o el missatge que l'horari està pendent de confirmar.
- Les proves aïllades no acrediten paritat exacta visual ni compilació d'iPhone.
- La paritat de suggeriments territorials i filtres avançats continuarà als passos 23–24.
