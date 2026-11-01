package com.example.agendavalenciana.ios

/** Pas 7. Mirror of Android v336 model/CategoryTaxonomy.kt identifiers (23/10/2026).
 * ONLY the display captions live here; the server's user_follows ENTITY_KEY is authoritative.
 * Android source is frozen; this iOS projection does not introduce another persistence domain.
 */
internal data class IosCulturalSubcategory(
    val categoryKey: String, val index: Int, val code: String, val label: String,
    val aliases: Set<String> = emptySet()
) {
    val legacyKey: String get() = "${categoryKey}__SUB_$index"
}
internal data class IosCategoryChoice(val key: String, val label: String)
internal data class IosCategorySelection(val categories: Set<String>, val subcategories: Set<String>)
internal data class IosCategoryFollowMutation(val key: String, val label: String, val following: Boolean)

internal object IosCategoryTaxonomy {
    val categories: List<IosCategoryChoice> = listOf(
        IosCategoryChoice("MUSIC", "Música"),
        IosCategoryChoice("THEATRE", "Teatre"),
        IosCategoryChoice("CINEMA", "Cinema"),
        IosCategoryChoice("TALKS", "Xarrades"),
        IosCategoryChoice("DANCE", "Dansa"),
        IosCategoryChoice("EXHIBITIONS", "Exposicions"),
        IosCategoryChoice("LITERATURE", "Literatura"),
        IosCategoryChoice("WORKSHOPS", "Tallers"),
        IosCategoryChoice("CHILDREN", "Infantil"),
        IosCategoryChoice("POPULAR_CULTURE", "Cultura popular"),
        IosCategoryChoice("HERITAGE", "Patrimoni"),
        IosCategoryChoice("OTHER", "Altres")
    )
    val subcategories: List<IosCulturalSubcategory> = listOf(
        IosCulturalSubcategory("MUSIC", 1, "MUSIC__ROCK_SKA_PUNK_METAL", "Rock, Ska, punk i metal", setOf("rock", "ska", "punk", "hardcore", "metal", "rock alternatiu")),
        IosCulturalSubcategory("MUSIC", 2, "MUSIC__POP_INDIE_URBANA", "Pop, indie i música urbana", setOf("pop", "indie", "hip-hop", "rap", "trap", "reggaeton", "electrònica")),
        IosCulturalSubcategory("MUSIC", 3, "MUSIC__FOLK_TRADICIONAL_MON", "Folk, tradicional i músiques del món", setOf("folk", "arrel", "música tradicional", "valencià", "balcànica", "músiques del món")),
        IosCulturalSubcategory("MUSIC", 4, "MUSIC__JAZZ_BLUES_SOUL", "Jazz, blues i soul", setOf("jazz", "blues", "soul", "funk", "swing", "rhythm & blues")),
        IosCulturalSubcategory("MUSIC", 5, "MUSIC__CLASSICA_OPERA_BANDA", "Clàssica, òpera i música de banda", setOf("clàssica", "simfònica", "cambra", "òpera", "lírica", "banda", "coral")),
        IosCulturalSubcategory("THEATRE", 1, "THEATRE__COMEDIA_HUMOR", "Comèdia i humor", setOf("comèdia", "humor", "monòlegs", "stand-up")),
        IosCulturalSubcategory("THEATRE", 2, "THEATRE__DRAMA", "Drama", setOf("drama", "tragèdia", "teatre dramàtic")),
        IosCulturalSubcategory("THEATRE", 3, "THEATRE__MUSICAL", "Musical", setOf("teatre musical", "musical")),
        IosCulturalSubcategory("THEATRE", 4, "THEATRE__CONTEMPORANI_EXPERIMENTAL", "Teatre contemporani i experimental", setOf("teatre contemporani", "experimental", "performance teatral")),
        IosCulturalSubcategory("THEATRE", 5, "THEATRE__TITELLES_GESTUAL", "Titelles i teatre gestual", setOf("titelles", "marionetes", "teatre gestual", "objectes", "ombres")),
        IosCulturalSubcategory("CINEMA", 1, "CINEMA__FICCIO", "Ficció", setOf("llargmetratge", "ficció", "narrativa cinematogràfica")),
        IosCulturalSubcategory("CINEMA", 2, "CINEMA__DOCUMENTAL", "Documental", setOf("documental", "cinema documental", "no ficció")),
        IosCulturalSubcategory("CINEMA", 3, "CINEMA__AUTOR_INDEPENDENT", "Cinema d'autor i independent", setOf("cinema d'autor", "cinema independent", "cineclub")),
        IosCulturalSubcategory("CINEMA", 4, "CINEMA__ANIMACIO", "Animació", setOf("animació", "stop-motion")),
        IosCulturalSubcategory("CINEMA", 5, "CINEMA__CURTMETRATGES", "Curtmetratges", setOf("curts", "curtmetratges")),
        IosCulturalSubcategory("TALKS", 1, "TALKS__HISTORIA_PATRIMONI", "Història i patrimoni", setOf("història", "patrimoni", "arqueologia", "memòria històrica")),
        IosCulturalSubcategory("TALKS", 2, "TALKS__CIENCIA_TECNOLOGIA", "Ciència i tecnologia", setOf("ciència", "divulgació", "tecnologia", "astronomia")),
        IosCulturalSubcategory("TALKS", 3, "TALKS__SOCIETAT_POLITICA", "Societat i política", setOf("política", "societat", "drets humans", "igualtat", "debat")),
        IosCulturalSubcategory("TALKS", 4, "TALKS__ART_CULTURA", "Art i cultura", setOf("arts", "cultura", "música", "cinema", "literatura")),
        IosCulturalSubcategory("TALKS", 5, "TALKS__FILOSOFIA_PENSAMENT", "Filosofia i pensament", setOf("filosofia", "pensament", "ètica")),
        IosCulturalSubcategory("DANCE", 1, "DANCE__BALLET_CLASSIC", "Ballet clàssic", setOf("ballet", "dansa clàssica")),
        IosCulturalSubcategory("DANCE", 2, "DANCE__CONTEMPORANIA", "Dansa contemporània", setOf("dansa contemporània", "dansa-teatre")),
        IosCulturalSubcategory("DANCE", 3, "DANCE__TRADICIONAL", "Dansa tradicional", setOf("danses populars", "dansà", "ball tradicional")),
        IosCulturalSubcategory("DANCE", 4, "DANCE__URBANA", "Dansa urbana", setOf("hip-hop", "breaking", "street dance")),
        IosCulturalSubcategory("DANCE", 5, "DANCE__FLAMENC_SALO", "Flamenc i balls de saló", setOf("flamenc", "flamenco", "tango", "balls de saló")),
        IosCulturalSubcategory("EXHIBITIONS", 1, "EXHIBITIONS__PINTURA_ESCULTURA", "Pintura i escultura", setOf("pintura", "escultura", "dibuix")),
        IosCulturalSubcategory("EXHIBITIONS", 2, "EXHIBITIONS__FOTOGRAFIA", "Fotografia", setOf("fotografia", "fotoperiodisme", "fotogràfic")),
        IosCulturalSubcategory("EXHIBITIONS", 3, "EXHIBITIONS__ART_CONTEMPORANI", "Art contemporani", setOf("art contemporani", "instal·lació", "arts visuals")),
        IosCulturalSubcategory("EXHIBITIONS", 4, "EXHIBITIONS__HISTORIA_PATRIMONI", "Història i patrimoni", setOf("història", "memòria històrica", "patrimoni", "etnologia")),
        IosCulturalSubcategory("EXHIBITIONS", 5, "EXHIBITIONS__CIENCIA_TECNOLOGIA", "Ciència i tecnologia", setOf("ciència", "tecnologia", "exposició científica")),
        IosCulturalSubcategory("LITERATURE", 1, "LITERATURE__NOVELLA_NARRATIVA", "Novel·la i narrativa", setOf("novel·la", "narrativa", "relat")),
        IosCulturalSubcategory("LITERATURE", 2, "LITERATURE__POESIA", "Poesia", setOf("poesia", "recitals poètics")),
        IosCulturalSubcategory("LITERATURE", 3, "LITERATURE__COMIC_ILLUSTRACIO", "Còmic i il·lustració", setOf("còmic", "manga", "novel·la gràfica", "il·lustració")),
        IosCulturalSubcategory("LITERATURE", 4, "LITERATURE__PRESENTACIONS_LLIBRES", "Presentacions de llibres", setOf("presentació de llibre", "autores", "autors")),
        IosCulturalSubcategory("LITERATURE", 5, "LITERATURE__CLUBS_TROBADES", "Clubs de lectura i trobades", setOf("club de lectura", "tertúlia literària", "trobades")),
        IosCulturalSubcategory("WORKSHOPS", 1, "WORKSHOPS__ARTS_ARTESANIA", "Arts plàstiques i artesania", setOf("pintura", "ceràmica", "manualitats", "artesania")),
        IosCulturalSubcategory("WORKSHOPS", 2, "WORKSHOPS__MUSICA_DANSA", "Música i dansa", setOf("música", "instrument", "cant", "dansa", "ball")),
        IosCulturalSubcategory("WORKSHOPS", 3, "WORKSHOPS__TEATRE_EXPRESSIO", "Teatre i expressió corporal", setOf("teatre", "interpretació", "moviment", "expressió corporal")),
        IosCulturalSubcategory("WORKSHOPS", 4, "WORKSHOPS__LITERATURA_AUDIOVISUAL", "Literatura i audiovisual", setOf("escriptura", "fotografia", "vídeo", "audiovisual")),
        IosCulturalSubcategory("WORKSHOPS", 5, "WORKSHOPS__CIENCIA_TECNOLOGIA", "Ciència i tecnologia", setOf("tecnologia", "ciència", "robòtica")),
        IosCulturalSubcategory("POPULAR_CULTURE", 1, "POPULAR_CULTURE__FESTES_POPULARS", "Festes populars i patronals", setOf("festes populars", "festes patronals", "festa major")),
        IosCulturalSubcategory("POPULAR_CULTURE", 2, "POPULAR_CULTURE__FALLES_MOROS_CRISTIANS", "Falles, Moros i Cristians", setOf("falles", "moros i cristians", "entrada mora", "entrada cristiana")),
        IosCulturalSubcategory("POPULAR_CULTURE", 3, "POPULAR_CULTURE__DANSES_MUSICA_TRADICIONAL", "Danses i música tradicional", setOf("danses tradicionals", "música tradicional", "cant d'estil")),
        IosCulturalSubcategory("POPULAR_CULTURE", 4, "POPULAR_CULTURE__MUIXERANGUES_TRADICIONS", "Muixerangues i altres tradicions", setOf("muixeranga", "gegants", "cabuts", "correfocs", "romeries")),
        IosCulturalSubcategory("POPULAR_CULTURE", 5, "POPULAR_CULTURE__FIRES_MERCATS_TRADICIONALS", "Fires i mercats tradicionals", setOf("fires tradicionals", "mercat tradicional", "artesania")),
        IosCulturalSubcategory("HERITAGE", 1, "HERITAGE__HISTORIA_ARQUEOLOGIA", "Història i arqueologia", setOf("història", "arqueologia", "jaciments")),
        IosCulturalSubcategory("HERITAGE", 2, "HERITAGE__ARQUITECTURA_MONUMENTS", "Arquitectura i monuments", setOf("arquitectura", "monuments", "edificis històrics")),
        IosCulturalSubcategory("HERITAGE", 3, "HERITAGE__MUSEUS", "Museus", setOf("museu", "col·lecció museística")),
        IosCulturalSubcategory("HERITAGE", 4, "HERITAGE__MEMORIA_HISTORICA", "Memòria històrica", setOf("memòria històrica", "memòria democràtica")),
        IosCulturalSubcategory("HERITAGE", 5, "HERITAGE__PATRIMONI_ETNOLOGIC", "Patrimoni etnològic", setOf("etnologia", "costums", "oficis", "patrimoni immaterial")),
        IosCulturalSubcategory("OTHER", 1, "OTHER__CIRC_ACROBACIES", "Circ i acrobàcies", setOf("circ", "acrobàcies", "trapezi", "malabars")),
        IosCulturalSubcategory("OTHER", 2, "OTHER__MAGIA", "Màgia", setOf("màgia", "il·lusionisme")),
        IosCulturalSubcategory("OTHER", 3, "OTHER__ARTS_VIVES_PERFORMANCE", "Arts vives i performance", setOf("arts vives", "performance", "experimentació")),
        IosCulturalSubcategory("OTHER", 4, "OTHER__GASTRONOMIA", "Gastronomia", setOf("gastronomia", "tastos", "cuina", "cultura gastronòmica")),
        IosCulturalSubcategory("OTHER", 5, "OTHER__ACTIVITATS_MULTIDISCIPLINARIES", "Activitats multidisciplinàries", setOf("multidisciplinari", "diferents disciplines", "festival multidisciplinari"))
    )
    private val mainByKey = categories.associateBy { it.key }
    private val byCode = subcategories.associateBy { it.code }
    private val byLegacy = subcategories.associate { it.legacyKey to it.code }

    fun mainKey(value: String): String? = categories.firstOrNull {
        it.key.equals(value.trim(), ignoreCase = true) || it.label.equals(value.trim(), ignoreCase = true)
    }?.key

    /** Accept Android's old index alias for READ and remove it when writing the canonical key. */
    fun subcategoryCode(value: String): String? {
        val raw = value.trim()
        val canonical = byLegacy.entries.firstOrNull { it.key.equals(raw, true) }?.value ?: raw
        return byCode.entries.firstOrNull { it.key.equals(canonical, true) }?.value?.code
    }

    fun childrenOf(categoryKey: String): List<IosCulturalSubcategory> =
        subcategories.filter { it.categoryKey == categoryKey }
    fun label(key: String): String = mainByKey[key]?.label ?: byCode[key]?.label ?: key
    fun isKnown(key: String): Boolean = mainKey(key) != null || subcategoryCode(key) != null

    /** Labels are NOT keys. Never use the displayed text to identify a subcategory. */
    fun fromServer(follows: List<IosFollow>): IosCategorySelection {
        val selected = follows.filter { it.type.equals("CATEGORY", ignoreCase = true) }
        val mains = selected.mapNotNull { mainKey(it.key) }.toSet()
        val children = selected.mapNotNull { subcategoryCode(it.key) }.filter { code ->
            byCode[code]?.categoryKey !in mains
        }.toSet()
        return IosCategorySelection(mains, children)
    }

    /** Android toggling semantics: whole branch or individual children, never both. */
    fun toggleCategory(current: IosCategorySelection, key: String): IosCategorySelection {
        require(key in mainByKey)
        val descendants = childrenOf(key).map { it.code }.toSet()
        return if (key in current.categories)
            current.copy(categories = current.categories - key, subcategories = current.subcategories - descendants)
        else current.copy(categories = current.categories + key,
            subcategories = current.subcategories - descendants)
    }

    fun toggleSubcategory(current: IosCategorySelection, code: String): IosCategorySelection {
        val sub = byCode[code] ?: error("Subcategoria desconeguda")
        val kids = childrenOf(sub.categoryKey).map { it.code }.toSet()
        val newCategories = current.categories - sub.categoryKey
        val next = if (sub.categoryKey in current.categories)
            (current.subcategories - kids) + (kids - code) // uncheck one of a whole category
        else if (code in current.subcategories) current.subcategories - code
        else current.subcategories + code
        return if (kids.isNotEmpty() && kids.all { it in next })
            IosCategorySelection(newCategories + sub.categoryKey, next - kids)
        else IosCategorySelection(newCategories, next)
    }

    fun normalized(categories: Set<String>, subcategories: Set<String>): IosCategorySelection {
        val mains = categories.mapNotNull(::mainKey).toSet()
        val subs = subcategories.mapNotNull(::subcategoryCode).filter { byCode[it]?.categoryKey !in mains }.toSet()
        return IosCategorySelection(mains, subs)
    }


    /** Android v336: explicit code wins; otherwise match ONLY an exact known label/alias.
     * No inference of genre from the name of an artist or an event title.
     */
    fun matchesEvent(category: String, eventStyles: List<String>, selection: IosCategorySelection): Boolean {
        val main = mainKey(category) ?: return false
        if (main in selection.categories) return true
        val normalizedStyles = eventStyles.flatMap { it.split('|', ',', ';') }
            .map { IosGeoPlaces.key(it) }.filter(String::isNotBlank).toSet()
        // Android CHILDREN is transversal: infantil/familiar counts even when the event
        // itself is classified as theatre, literature, music, etc.
        if ("CHILDREN" in selection.categories && normalizedStyles.any {
                it in setOf("infantil", "public infantil", "familiar", "per a xiquets", "families")
            }) return true
        if (selection.subcategories.isEmpty()) return false
        return childrenOf(main).any { sub ->
            sub.code in selection.subcategories &&
                (IosGeoPlaces.key(sub.code) in normalizedStyles ||
                (listOf(sub.label) + sub.aliases).any { alias ->
                    val canonical = IosGeoPlaces.key(alias)
                    canonical.length >= 3 && canonical in normalizedStyles &&
                        canonical !in setOf("valencia", "musica", "cultura", "art", "activitats", "festival", "espectacle")
                })
        }
    }

    /** Diff over exact entity_key values in user_follows. Unrelated server follows are untouched.
     * Legacy aliases are explicitly removed, matching Android v336 unfollow policy.
     */
    fun mutations(existing: List<IosFollow>, next: IosCategorySelection): List<IosCategoryFollowMutation> {
        val intended = normalized(next.categories,next.subcategories)
        val desired = intended.categories + intended.subcategories
        val current = existing.filter { it.type.equals("CATEGORY", true) }.map { it.key }
        val knownCurrent = current.filter(::isKnown).toSet()
        fun canonical(key: String): String? = mainKey(key) ?: subcategoryCode(key)
        // PHP lowercases entity_key. That is NOT a legacy alias and must NOT cause
        // a remove+add pair on every personalization save.
        val remove = knownCurrent.filter { key ->
            val code = canonical(key)
            val legacyIndexAlias = key.contains("__SUB_", ignoreCase = true)
            code !in desired || legacyIndexAlias
        }.distinct().sorted()
        val add = desired.filter { code ->
            current.none { old -> canonical(old) == code && !old.contains("__SUB_", ignoreCase = true) }
        }.sorted()
        return remove.map { IosCategoryFollowMutation(it, label(subcategoryCode(it) ?: mainKey(it) ?: it), false) } +
            add.map { IosCategoryFollowMutation(it, label(it), true) }
    }
}
