# Pas 23/48 — Suggeriments de pobles i comarques (iOS v349)

**Estat:** incorporat a la còpia de treball acumulativa, passos 1–23. Cap ZIP fins al pas 24.

## Què ha canviat

- La font territorial és el mateix cens empaquetat a iOS en el pas 8 (contrastat amb l'asset Android v336); no es crea cap base de dades local d'usuari.
- `IosGeoPlaces.municipalityEntries()` conserva una entrada per municipi amb denominació alternativa/oficial, comarca, territori i àlies; ja no es confonen noms homònims en el pool de suggeriments.
- `IosAdvancedSearch.pool` crea suggeriments territorials amb subtítol (nom oficial, comarca i territori). Els noms alternatius es poden buscar però es mostra sempre el nom canònic del catàleg.
- Classificació com Android: coincidència exacta > començament > contingut, després prioritat de tipus i nom. Top-N limitat, cerca des de fil de fons, amb retard i sense recalcular el feed per cada lletra.
- El cercador principal conserva la distinció TOWN/COMARCA; les opcions «On? Poble» i «Comarca» mostren suggeriments específics, incloent la comarca sota el poble.
- El filtre accepta la comarca retornada pel catàleg de servidor si està disponible, per distingir pobles amb el mateix nom. Quan l'API no aporta comarca i hi ha dos pobles homònims, no s'infereix arbitràriament l'assignació.
- No es modifiquen les preferències personals, servidor, Android v336 ni el codi Android empaquetat.

## Proves realitzades

- `comprovar_pas_01.py` … `comprovar_pas_23.py`: comprovacions estàtiques satisfactòries.
- Kotlin/JVM: compilació aïllada de `IosGeoPlaces.kt`, `IosAdvancedSearch.kt`, `IosFestivalArtistSearch.kt` amb 19 assercions de cerca del cens real satisfactòries; proves `IosTerritorySuggestionsTest.kt` incloses a commonTest.
- Fonts Android v336 i Android incrustat en iOS comparades amb hashes dels ZIP originals: sense canvis.
- **No s'ha compilat l'app completa en Kotlin/Native iOS, ni s'ha provat un iPhone o el servidor viu.**

## Límits

- L'API d'esdeveniments no sempre proporciona `comarca`: si un poble homònim no porta comarca en el JSON, no es pot resoldre amb certesa quin és sense ampliar l'API. No es modifica PHP.
- La pantalla principal conserva altres tipus de suggeriment d'iOS (categoria, activitat) que el cercador principal actual d'Android no enumera en el mateix pool; paritat visual/funcional completa pendent d'auditoria.
