package com.example.agendavalenciana.ios

/** Taxonomia 11 × 5 + Infantil transversal; codis idèntics a Android CategoryTaxonomy. */
data class IosCulturalSubcategory(val categoryCode: String, val code: String, val label: String, val aliases: List<String> = emptyList())
data class IosCulturalCategory(val code: String, val label: String)

object IosPersonalizationTaxonomy {
    val categories = listOf(
        IosCulturalCategory("MUSIC", "Música"),
        IosCulturalCategory("THEATRE", "Teatre"),
        IosCulturalCategory("CINEMA", "Cinema"),
        IosCulturalCategory("TALKS", "Xarrades"),
        IosCulturalCategory("DANCE", "Dansa"),
        IosCulturalCategory("EXHIBITIONS", "Exposicions"),
        IosCulturalCategory("LITERATURE", "Literatura"),
        IosCulturalCategory("WORKSHOPS", "Tallers"),
        IosCulturalCategory("POPULAR_CULTURE", "Cultura popular"),
        IosCulturalCategory("HERITAGE", "Patrimoni"),
        IosCulturalCategory("OTHER", "Altres")
    )
    val children = IosCulturalCategory("CHILDREN", "Infantil")
    val subcategories = listOf(
        IosCulturalSubcategory("MUSIC", "MUSIC__ROCK_SKA_PUNK_METAL", "Rock, Ska, punk i metal", listOf("rock", "ska", "punk", "hardcore", "metal", "rock alternatiu")),
        IosCulturalSubcategory("MUSIC", "MUSIC__POP_INDIE_URBANA", "Pop, indie i música urbana", listOf("pop", "indie", "hip-hop", "rap", "trap", "reggaeton", "electrònica")),
        IosCulturalSubcategory("MUSIC", "MUSIC__FOLK_TRADICIONAL_MON", "Folk, tradicional i músiques del món", listOf("folk", "arrel", "música tradicional", "valencià", "balcànica", "músiques del món")),
        IosCulturalSubcategory("MUSIC", "MUSIC__JAZZ_BLUES_SOUL", "Jazz, blues i soul", listOf("jazz", "blues", "soul", "funk", "swing", "rhythm & blues")),
        IosCulturalSubcategory("MUSIC", "MUSIC__CLASSICA_OPERA_BANDA", "Clàssica, òpera i música de banda", listOf("clàssica", "simfònica", "cambra", "òpera", "lírica", "banda", "coral")),
        IosCulturalSubcategory("THEATRE", "THEATRE__COMEDIA_HUMOR", "Comèdia i humor", listOf("comèdia", "humor", "monòlegs", "stand-up")),
        IosCulturalSubcategory("THEATRE", "THEATRE__DRAMA", "Drama", listOf("drama", "tragèdia", "teatre dramàtic")),
        IosCulturalSubcategory("THEATRE", "THEATRE__MUSICAL", "Musical", listOf("teatre musical", "musical")),
        IosCulturalSubcategory("THEATRE", "THEATRE__CONTEMPORANI_EXPERIMENTAL", "Teatre contemporani i experimental", listOf("teatre contemporani", "experimental", "performance teatral")),
        IosCulturalSubcategory("THEATRE", "THEATRE__TITELLES_GESTUAL", "Titelles i teatre gestual", listOf("titelles", "marionetes", "teatre gestual", "objectes", "ombres")),
        IosCulturalSubcategory("CINEMA", "CINEMA__FICCIO", "Ficció", listOf("llargmetratge", "ficció", "narrativa cinematogràfica")),
        IosCulturalSubcategory("CINEMA", "CINEMA__DOCUMENTAL", "Documental", listOf("documental", "cinema documental", "no ficció")),
        IosCulturalSubcategory("CINEMA", "CINEMA__AUTOR_INDEPENDENT", "Cinema d'autor i independent", listOf("cinema d'autor", "cinema independent", "cineclub")),
        IosCulturalSubcategory("CINEMA", "CINEMA__ANIMACIO", "Animació", listOf("animació", "stop-motion")),
        IosCulturalSubcategory("CINEMA", "CINEMA__CURTMETRATGES", "Curtmetratges", listOf("curts", "curtmetratges")),
        IosCulturalSubcategory("TALKS", "TALKS__HISTORIA_PATRIMONI", "Història i patrimoni", listOf("història", "patrimoni", "arqueologia", "memòria històrica")),
        IosCulturalSubcategory("TALKS", "TALKS__CIENCIA_TECNOLOGIA", "Ciència i tecnologia", listOf("ciència", "divulgació", "tecnologia", "astronomia")),
        IosCulturalSubcategory("TALKS", "TALKS__SOCIETAT_POLITICA", "Societat i política", listOf("política", "societat", "drets humans", "igualtat", "debat")),
        IosCulturalSubcategory("TALKS", "TALKS__ART_CULTURA", "Art i cultura", listOf("arts", "cultura", "música", "cinema", "literatura")),
        IosCulturalSubcategory("TALKS", "TALKS__FILOSOFIA_PENSAMENT", "Filosofia i pensament", listOf("filosofia", "pensament", "ètica")),
        IosCulturalSubcategory("DANCE", "DANCE__BALLET_CLASSIC", "Ballet clàssic", listOf("ballet", "dansa clàssica")),
        IosCulturalSubcategory("DANCE", "DANCE__CONTEMPORANIA", "Dansa contemporània", listOf("dansa contemporània", "dansa-teatre")),
        IosCulturalSubcategory("DANCE", "DANCE__TRADICIONAL", "Dansa tradicional", listOf("danses populars", "dansà", "ball tradicional")),
        IosCulturalSubcategory("DANCE", "DANCE__URBANA", "Dansa urbana", listOf("hip-hop", "breaking", "street dance")),
        IosCulturalSubcategory("DANCE", "DANCE__FLAMENC_SALO", "Flamenc i balls de saló", listOf("flamenc", "flamenco", "tango", "balls de saló")),
        IosCulturalSubcategory("EXHIBITIONS", "EXHIBITIONS__PINTURA_ESCULTURA", "Pintura i escultura", listOf("pintura", "escultura", "dibuix")),
        IosCulturalSubcategory("EXHIBITIONS", "EXHIBITIONS__FOTOGRAFIA", "Fotografia", listOf("fotografia", "fotoperiodisme", "fotogràfic")),
        IosCulturalSubcategory("EXHIBITIONS", "EXHIBITIONS__ART_CONTEMPORANI", "Art contemporani", listOf("art contemporani", "instal·lació", "arts visuals")),
        IosCulturalSubcategory("EXHIBITIONS", "EXHIBITIONS__HISTORIA_PATRIMONI", "Història i patrimoni", listOf("història", "memòria històrica", "patrimoni", "etnologia")),
        IosCulturalSubcategory("EXHIBITIONS", "EXHIBITIONS__CIENCIA_TECNOLOGIA", "Ciència i tecnologia", listOf("ciència", "tecnologia", "exposició científica")),
        IosCulturalSubcategory("LITERATURE", "LITERATURE__NOVELLA_NARRATIVA", "Novel·la i narrativa", listOf("novel·la", "narrativa", "relat")),
        IosCulturalSubcategory("LITERATURE", "LITERATURE__POESIA", "Poesia", listOf("poesia", "recitals poètics")),
        IosCulturalSubcategory("LITERATURE", "LITERATURE__COMIC_ILLUSTRACIO", "Còmic i il·lustració", listOf("còmic", "manga", "novel·la gràfica", "il·lustració")),
        IosCulturalSubcategory("LITERATURE", "LITERATURE__PRESENTACIONS_LLIBRES", "Presentacions de llibres", listOf("presentació de llibre", "autores", "autors")),
        IosCulturalSubcategory("LITERATURE", "LITERATURE__CLUBS_TROBADES", "Clubs de lectura i trobades", listOf("club de lectura", "tertúlia literària", "trobades")),
        IosCulturalSubcategory("WORKSHOPS", "WORKSHOPS__ARTS_ARTESANIA", "Arts plàstiques i artesania", listOf("pintura", "ceràmica", "manualitats", "artesania")),
        IosCulturalSubcategory("WORKSHOPS", "WORKSHOPS__MUSICA_DANSA", "Música i dansa", listOf("música", "instrument", "cant", "dansa", "ball")),
        IosCulturalSubcategory("WORKSHOPS", "WORKSHOPS__TEATRE_EXPRESSIO", "Teatre i expressió corporal", listOf("teatre", "interpretació", "moviment", "expressió corporal")),
        IosCulturalSubcategory("WORKSHOPS", "WORKSHOPS__LITERATURA_AUDIOVISUAL", "Literatura i audiovisual", listOf("escriptura", "fotografia", "vídeo", "audiovisual")),
        IosCulturalSubcategory("WORKSHOPS", "WORKSHOPS__CIENCIA_TECNOLOGIA", "Ciència i tecnologia", listOf("tecnologia", "ciència", "robòtica")),
        IosCulturalSubcategory("POPULAR_CULTURE", "POPULAR_CULTURE__FESTES_POPULARS", "Festes populars i patronals", listOf("festes populars", "festes patronals", "festa major")),
        IosCulturalSubcategory("POPULAR_CULTURE", "POPULAR_CULTURE__FALLES_MOROS_CRISTIANS", "Falles, Moros i Cristians", listOf("falles", "moros i cristians", "entrada mora", "entrada cristiana")),
        IosCulturalSubcategory("POPULAR_CULTURE", "POPULAR_CULTURE__DANSES_MUSICA_TRADICIONAL", "Danses i música tradicional", listOf("danses tradicionals", "música tradicional", "cant d'estil")),
        IosCulturalSubcategory("POPULAR_CULTURE", "POPULAR_CULTURE__MUIXERANGUES_TRADICIONS", "Muixerangues i altres tradicions", listOf("muixeranga", "gegants", "cabuts", "correfocs", "romeries")),
        IosCulturalSubcategory("POPULAR_CULTURE", "POPULAR_CULTURE__FIRES_MERCATS_TRADICIONALS", "Fires i mercats tradicionals", listOf("fires tradicionals", "mercat tradicional", "artesania")),
        IosCulturalSubcategory("HERITAGE", "HERITAGE__HISTORIA_ARQUEOLOGIA", "Història i arqueologia", listOf("història", "arqueologia", "jaciments")),
        IosCulturalSubcategory("HERITAGE", "HERITAGE__ARQUITECTURA_MONUMENTS", "Arquitectura i monuments", listOf("arquitectura", "monuments", "edificis històrics")),
        IosCulturalSubcategory("HERITAGE", "HERITAGE__MUSEUS", "Museus", listOf("museu", "col·lecció museística")),
        IosCulturalSubcategory("HERITAGE", "HERITAGE__MEMORIA_HISTORICA", "Memòria històrica", listOf("memòria històrica", "memòria democràtica")),
        IosCulturalSubcategory("HERITAGE", "HERITAGE__PATRIMONI_ETNOLOGIC", "Patrimoni etnològic", listOf("etnologia", "costums", "oficis", "patrimoni immaterial")),
        IosCulturalSubcategory("OTHER", "OTHER__CIRC_ACROBACIES", "Circ i acrobàcies", listOf("circ", "acrobàcies", "trapezi", "malabars")),
        IosCulturalSubcategory("OTHER", "OTHER__MAGIA", "Màgia", listOf("màgia", "il·lusionisme")),
        IosCulturalSubcategory("OTHER", "OTHER__ARTS_VIVES_PERFORMANCE", "Arts vives i performance", listOf("arts vives", "performance", "experimentació")),
        IosCulturalSubcategory("OTHER", "OTHER__GASTRONOMIA", "Gastronomia", listOf("gastronomia", "tastos", "cuina", "cultura gastronòmica")),
        IosCulturalSubcategory("OTHER", "OTHER__ACTIVITATS_MULTIDISCIPLINARIES", "Activitats multidisciplinàries", listOf("multidisciplinari", "diferents disciplines", "festival multidisciplinari"))
    )
    fun childrenOf(categoryCode: String) = subcategories.filter { it.categoryCode == categoryCode }
    fun categoryLabel(code: String) = (categories + children).firstOrNull { it.code == code }?.label ?: code
    fun subcategoryLabel(code: String) = subcategories.firstOrNull { it.code == code }?.label ?: code
    fun canonicalFollowKey(raw: String): String {
        val code = raw.trim()
        val legacy = Regex("^([A-Z_]+)__SUB_([1-5])$").matchEntire(code)
        if (legacy != null) {
            val match = childrenOf(legacy.groupValues[1]).getOrNull(legacy.groupValues[2].toInt() - 1)
            if (match != null) return match.code
        }
        val category = (categories + children).firstOrNull {
            it.code.equals(code, true) || it.label.equals(code, true)
        }
        return category?.code ?: code
    }
    /** Follows 5/5 are equivalent to following the whole category (Android). */
    fun toggleCategory(current: Set<String>, category: String): Set<String> {
        val branch = childrenOf(category).map { it.code }.toSet()
        val without = current - category - branch
        return if (category in current) without else without + category
    }
    fun toggleSubcategory(current: Set<String>, code: String): Set<String> {
        val sub = subcategories.firstOrNull { it.code == code } ?: return current
        val all = childrenOf(sub.categoryCode).map { it.code }.toSet()
        val next = if (sub.categoryCode in current) {
            (current - sub.categoryCode) + (all - code)
        } else if (code in current) current - code else current + code
        return if (all.all { it in next }) next - all + sub.categoryCode else next
    }
    fun isSelected(follows: Set<String>, category: String, subcode: String): Boolean =
        category in follows || subcode in follows
    fun categoryForEvent(raw: String): String? = (categories + children).firstOrNull {
        it.code.equals(raw.trim(), true) || it.label.equals(raw.trim(), true)
    }?.code
    /** Exact editorial styles, not loose substring matching; same principle as Android. */
    fun matchesSubcategory(code: String, eventCategory: String, eventStyles: List<String>): Boolean {
        val sub = subcategories.firstOrNull { it.code == canonicalFollowKey(code) } ?: return false
        if (categoryForEvent(eventCategory) != sub.categoryCode) return false
        val ambiguous = setOf("valencia", "valencia catala", "musica", "cultura", "art", "activitats", "festival", "espectacle")
        val allowed = (listOf(sub.code,sub.label) + sub.aliases).map(IosGeoPlaces::key)
            .filter { it.length >= 3 && it !in ambiguous }.toSet()
        return eventStyles.flatMap { it.split('|',',',';') }.map(IosGeoPlaces::key).any { it in allowed }
    }
}
