## Pas: Modes del feed, «Seguits», «Categoria» i filtres guardats — 29/10/2026

**Referent directe:** `app/src/main/java/com/example/agendavalenciana/ui/AgendaApp.kt`
(selector de modes), `state/AgendaState.kt` (funcions `selectFeedMode`,
`selectFeedCategory`, `followedEventsChronological`, `applyFilterPreset`) i la taula
`agenda_user_saved_search_filters` de la còpia SQL aportada.

- **Implementat en codi iOS:** «Categoria» no obri «Opcions» del cercador. Ara desplega un menú independent de 12 categories (11 principals + Infantil). La categoria elegida filtra els esdeveniments del catàleg complet, amb etiqueta activa i opció «Llevar disciplina · Per a tu».
- **Implementat en codi iOS:** «Seguits» activa un mode propi, exclusiu d'artistes/grups seguits (incloent-hi els artistes presents a l'horari de festivals), cronològic i sense afinitats ni descobertes. Categories, organitzadors i sales seguits no alimenten «Seguits» sinó «Per a tu». Els continguts comercials globalment garantits conserven l'excepció obligatòria existent.
- **Implementat en codi iOS:** «Per a tu / Sense filtre» desplega també els filtres guardats al servidor amb el seu nom, aplicació directa, etiqueta activa i opció d'eliminar. Una cerca amb nom no queda limitada al rànquing personalitzat.
- **Implementat en codi iOS:** en desar «Guardar» des del popup, només després de la confirmació del servidor es carrega la cerca amb el nom visible al selector. No es presenta com a guardada si l'API ha fallat. La llista es refresca en obrir el selector.
- **Implementat en codi iOS:** canviar entre els modes elimina la cerca, el preset i els criteris anteriors; una cerca explícita feta dins de «Seguits» o «Categoria» torna al catàleg global; l'scroll es reinicia correctament en canviar de mode/categoria/preset.
- **Verificat localment:** 14 proves `kotlinc` de modes (artistes, horaris de festivals, accents, passat, categories, ordre), 6 comprovacions estàtiques d'integració de menú i persistència, i integritat SHA256 de tots els fitxers originals Android.
- **No verificat:** compilació completa Kotlin/Native/Xcode, comportament visual en iPhone, dades i respostes efectives del `preferences/search-filters.php` en producció. El SQL acredita l'estructura de persistència, no una transacció real d'iOS.
- **Versió fixa:** v350. Sense generar ni lliurar cap ZIP en este pas.

# L'Agenda iOS — v350 (versió fixa) · canvis acumulatius

## Pas: Opcions del cercador — 29/10/2026

- **Implementat en codi:** el botó «Opcions» obri `IosSearchFilterSheet`, un `ModalBottomSheet` sobre el feed. S'ha llevat el bloc desplegable que estava inserit en el feed.
- **Implementat en codi:** panell blanc amb identificació «Filtres», «Netejar» i quatre apartats desplegables independents (Quan, On?, Què i Preu) amb icones originals de `AgendaGlyph`.
- **Implementat en codi:** peu amb «Aplicar» i «Guardar» sempre visibles; esborrany de filtres i cancel·lació sense aplicar canvis.
- **Implementat en codi:** Hui, Demà, Cap de setmana, 7 dies, Este mes; selector de dates natiu Compose Material 3, mes actual + onze següents.
- **Implementat en codi:** cerca local de pobles/comarques amb suggeriments i xips, comarques completes, distància ≤ 10 / 25 / 50 / 100 km des d'un poble concret.
- **Implementat en codi:** selecció múltiple de disciplines, opcions Gratuït / ≤ 10 / 15 / 20 €; estes opcions afecten el filtratge real de l'índex d'esdeveniments.
- **Implementat en codi:** neteja restaura «Per a tu» com Android; els filtres desats s'apliquen sense arrossegar una cerca textual anterior.
- **Implementat en codi:** criteris remots complets amb les claus d'Android (`categories`, `dateFrom`, `dateTo`, `month`, `comarca`, `town`, `maxDistanceKm`, `maxPrice`, `freeOnly`) i `style` iOS quan existisca. «Guardar» no anuncia èxit abans de resposta confirmada del servidor. Les llistes es mostren, s'apliquen i es poden eliminar.
- **Distància:** la cerca iOS usa una població triada o les poblacions preferides de l'usuari per a calcular el radi, com Android. Quan no existeix cap origen, les opcions de distància queden desactivades i un preset remot només pot aplicar-se després de triar origen. No s'inventen coordenades ni es tracta distància desconeguda com a coincidència positiva.
- **No comprovat:** compilació amb Xcode, simulador, aparell iOS real i integració en viu amb l'API PHP. Les comprovacions amb `kotlinc` de regles pures de filtratge sí s'han executat localment.
- **No modificat:** fitxers originals Android, servidor ni versió v350. **No s'ha creat ni lliurat cap ZIP nou.**

## Pas: Notificacions — 29/10/2026

Vegeu `NOTIFICACIONS_IOS_V350_ACUMULAT.md` per al contrast amb Android, la llista de 14 funcions modificades, els bloquejos i les proves. Projecte v350 iOS acumulatiu: sense ZIP, sense alterar Android.
