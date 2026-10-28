package com.example.agendavalenciana.model

import java.text.Normalizer
import java.util.Locale

/** Els codis provenen del catàleg SUBCATEGORIES de Google Sheets (23/10/2026). */
data class CulturalSubcategory(
    val category: CulturalCategory,
    val index: Int,
    val code: String,
    val label: String,
    val aliases: List<String> = emptyList()
) {
    val followKey: String get() = code
    val legacyFollowKey: String get() = "${category.name}__SUB_$index"
}

object CategoryTaxonomy {
    val mainCategories: List<CulturalCategory> = listOf(
        CulturalCategory.MUSIC, CulturalCategory.THEATRE, CulturalCategory.CINEMA,
        CulturalCategory.TALKS, CulturalCategory.DANCE, CulturalCategory.EXHIBITIONS,
        CulturalCategory.LITERATURE, CulturalCategory.WORKSHOPS,
        CulturalCategory.POPULAR_CULTURE, CulturalCategory.HERITAGE, CulturalCategory.OTHER
    )
    val childrenCategory: CulturalCategory = CulturalCategory.CHILDREN
    val allSubcategories: List<CulturalSubcategory> = listOf(
        CulturalSubcategory(CulturalCategory.MUSIC, 1, "MUSIC__ROCK_SKA_PUNK_METAL", "Rock, Ska, punk i metal", listOf("rock","ska","punk","hardcore","metal","rock alternatiu")),
        CulturalSubcategory(CulturalCategory.MUSIC, 2, "MUSIC__POP_INDIE_URBANA", "Pop, indie i música urbana", listOf("pop","indie","hip-hop","rap","trap","reggaeton","electrònica")),
        CulturalSubcategory(CulturalCategory.MUSIC, 3, "MUSIC__FOLK_TRADICIONAL_MON", "Folk, tradicional i músiques del món", listOf("folk","arrel","música tradicional","valencià","balcànica","músiques del món")),
        CulturalSubcategory(CulturalCategory.MUSIC, 4, "MUSIC__JAZZ_BLUES_SOUL", "Jazz, blues i soul", listOf("jazz","blues","soul","funk","swing","rhythm & blues")),
        CulturalSubcategory(CulturalCategory.MUSIC, 5, "MUSIC__CLASSICA_OPERA_BANDA", "Clàssica, òpera i música de banda", listOf("clàssica","simfònica","cambra","òpera","lírica","banda","coral")),
        CulturalSubcategory(CulturalCategory.THEATRE, 1, "THEATRE__COMEDIA_HUMOR", "Comèdia i humor", listOf("comèdia","humor","monòlegs","stand-up")),
        CulturalSubcategory(CulturalCategory.THEATRE, 2, "THEATRE__DRAMA", "Drama", listOf("drama","tragèdia","teatre dramàtic")),
        CulturalSubcategory(CulturalCategory.THEATRE, 3, "THEATRE__MUSICAL", "Musical", listOf("teatre musical","musical")),
        CulturalSubcategory(CulturalCategory.THEATRE, 4, "THEATRE__CONTEMPORANI_EXPERIMENTAL", "Teatre contemporani i experimental", listOf("teatre contemporani","experimental","performance teatral")),
        CulturalSubcategory(CulturalCategory.THEATRE, 5, "THEATRE__TITELLES_GESTUAL", "Titelles i teatre gestual", listOf("titelles","marionetes","teatre gestual","objectes","ombres")),
        CulturalSubcategory(CulturalCategory.CINEMA, 1, "CINEMA__FICCIO", "Ficció", listOf("llargmetratge","ficció","narrativa cinematogràfica")),
        CulturalSubcategory(CulturalCategory.CINEMA, 2, "CINEMA__DOCUMENTAL", "Documental", listOf("documental","cinema documental","no ficció")),
        CulturalSubcategory(CulturalCategory.CINEMA, 3, "CINEMA__AUTOR_INDEPENDENT", "Cinema d'autor i independent", listOf("cinema d'autor","cinema independent","cineclub")),
        CulturalSubcategory(CulturalCategory.CINEMA, 4, "CINEMA__ANIMACIO", "Animació", listOf("animació","stop-motion")),
        CulturalSubcategory(CulturalCategory.CINEMA, 5, "CINEMA__CURTMETRATGES", "Curtmetratges", listOf("curts","curtmetratges")),
        CulturalSubcategory(CulturalCategory.TALKS, 1, "TALKS__HISTORIA_PATRIMONI", "Història i patrimoni", listOf("història","patrimoni","arqueologia","memòria històrica")),
        CulturalSubcategory(CulturalCategory.TALKS, 2, "TALKS__CIENCIA_TECNOLOGIA", "Ciència i tecnologia", listOf("ciència","divulgació","tecnologia","astronomia")),
        CulturalSubcategory(CulturalCategory.TALKS, 3, "TALKS__SOCIETAT_POLITICA", "Societat i política", listOf("política","societat","drets humans","igualtat","debat")),
        CulturalSubcategory(CulturalCategory.TALKS, 4, "TALKS__ART_CULTURA", "Art i cultura", listOf("arts","cultura","música","cinema","literatura")),
        CulturalSubcategory(CulturalCategory.TALKS, 5, "TALKS__FILOSOFIA_PENSAMENT", "Filosofia i pensament", listOf("filosofia","pensament","ètica")),
        CulturalSubcategory(CulturalCategory.DANCE, 1, "DANCE__BALLET_CLASSIC", "Ballet clàssic", listOf("ballet","dansa clàssica")),
        CulturalSubcategory(CulturalCategory.DANCE, 2, "DANCE__CONTEMPORANIA", "Dansa contemporània", listOf("dansa contemporània","dansa-teatre")),
        CulturalSubcategory(CulturalCategory.DANCE, 3, "DANCE__TRADICIONAL", "Dansa tradicional", listOf("danses populars","dansà","ball tradicional")),
        CulturalSubcategory(CulturalCategory.DANCE, 4, "DANCE__URBANA", "Dansa urbana", listOf("hip-hop","breaking","street dance")),
        CulturalSubcategory(CulturalCategory.DANCE, 5, "DANCE__FLAMENC_SALO", "Flamenc i balls de saló", listOf("flamenc","flamenco","tango","balls de saló")),
        CulturalSubcategory(CulturalCategory.EXHIBITIONS, 1, "EXHIBITIONS__PINTURA_ESCULTURA", "Pintura i escultura", listOf("pintura","escultura","dibuix")),
        CulturalSubcategory(CulturalCategory.EXHIBITIONS, 2, "EXHIBITIONS__FOTOGRAFIA", "Fotografia", listOf("fotografia","fotoperiodisme","fotogràfic")),
        CulturalSubcategory(CulturalCategory.EXHIBITIONS, 3, "EXHIBITIONS__ART_CONTEMPORANI", "Art contemporani", listOf("art contemporani","instal·lació","arts visuals")),
        CulturalSubcategory(CulturalCategory.EXHIBITIONS, 4, "EXHIBITIONS__HISTORIA_PATRIMONI", "Història i patrimoni", listOf("història","memòria històrica","patrimoni","etnologia")),
        CulturalSubcategory(CulturalCategory.EXHIBITIONS, 5, "EXHIBITIONS__CIENCIA_TECNOLOGIA", "Ciència i tecnologia", listOf("ciència","tecnologia","exposició científica")),
        CulturalSubcategory(CulturalCategory.LITERATURE, 1, "LITERATURE__NOVELLA_NARRATIVA", "Novel·la i narrativa", listOf("novel·la","narrativa","relat")),
        CulturalSubcategory(CulturalCategory.LITERATURE, 2, "LITERATURE__POESIA", "Poesia", listOf("poesia","recitals poètics")),
        CulturalSubcategory(CulturalCategory.LITERATURE, 3, "LITERATURE__COMIC_ILLUSTRACIO", "Còmic i il·lustració", listOf("còmic","manga","novel·la gràfica","il·lustració")),
        CulturalSubcategory(CulturalCategory.LITERATURE, 4, "LITERATURE__PRESENTACIONS_LLIBRES", "Presentacions de llibres", listOf("presentació de llibre","autores","autors")),
        CulturalSubcategory(CulturalCategory.LITERATURE, 5, "LITERATURE__CLUBS_TROBADES", "Clubs de lectura i trobades", listOf("club de lectura","tertúlia literària","trobades")),
        CulturalSubcategory(CulturalCategory.WORKSHOPS, 1, "WORKSHOPS__ARTS_ARTESANIA", "Arts plàstiques i artesania", listOf("pintura","ceràmica","manualitats","artesania")),
        CulturalSubcategory(CulturalCategory.WORKSHOPS, 2, "WORKSHOPS__MUSICA_DANSA", "Música i dansa", listOf("música","instrument","cant","dansa","ball")),
        CulturalSubcategory(CulturalCategory.WORKSHOPS, 3, "WORKSHOPS__TEATRE_EXPRESSIO", "Teatre i expressió corporal", listOf("teatre","interpretació","moviment","expressió corporal")),
        CulturalSubcategory(CulturalCategory.WORKSHOPS, 4, "WORKSHOPS__LITERATURA_AUDIOVISUAL", "Literatura i audiovisual", listOf("escriptura","fotografia","vídeo","audiovisual")),
        CulturalSubcategory(CulturalCategory.WORKSHOPS, 5, "WORKSHOPS__CIENCIA_TECNOLOGIA", "Ciència i tecnologia", listOf("tecnologia","ciència","robòtica")),
        CulturalSubcategory(CulturalCategory.POPULAR_CULTURE, 1, "POPULAR_CULTURE__FESTES_POPULARS", "Festes populars i patronals", listOf("festes populars","festes patronals","festa major")),
        CulturalSubcategory(CulturalCategory.POPULAR_CULTURE, 2, "POPULAR_CULTURE__FALLES_MOROS_CRISTIANS", "Falles, Moros i Cristians", listOf("falles","moros i cristians","entrada mora","entrada cristiana")),
        CulturalSubcategory(CulturalCategory.POPULAR_CULTURE, 3, "POPULAR_CULTURE__DANSES_MUSICA_TRADICIONAL", "Danses i música tradicional", listOf("danses tradicionals","música tradicional","cant d'estil")),
        CulturalSubcategory(CulturalCategory.POPULAR_CULTURE, 4, "POPULAR_CULTURE__MUIXERANGUES_TRADICIONS", "Muixerangues i altres tradicions", listOf("muixeranga","gegants","cabuts","correfocs","romeries")),
        CulturalSubcategory(CulturalCategory.POPULAR_CULTURE, 5, "POPULAR_CULTURE__FIRES_MERCATS_TRADICIONALS", "Fires i mercats tradicionals", listOf("fires tradicionals","mercat tradicional","artesania")),
        CulturalSubcategory(CulturalCategory.HERITAGE, 1, "HERITAGE__HISTORIA_ARQUEOLOGIA", "Història i arqueologia", listOf("història","arqueologia","jaciments")),
        CulturalSubcategory(CulturalCategory.HERITAGE, 2, "HERITAGE__ARQUITECTURA_MONUMENTS", "Arquitectura i monuments", listOf("arquitectura","monuments","edificis històrics")),
        CulturalSubcategory(CulturalCategory.HERITAGE, 3, "HERITAGE__MUSEUS", "Museus", listOf("museu","col·lecció museística")),
        CulturalSubcategory(CulturalCategory.HERITAGE, 4, "HERITAGE__MEMORIA_HISTORICA", "Memòria històrica", listOf("memòria històrica","memòria democràtica")),
        CulturalSubcategory(CulturalCategory.HERITAGE, 5, "HERITAGE__PATRIMONI_ETNOLOGIC", "Patrimoni etnològic", listOf("etnologia","costums","oficis","patrimoni immaterial")),
        CulturalSubcategory(CulturalCategory.OTHER, 1, "OTHER__CIRC_ACROBACIES", "Circ i acrobàcies", listOf("circ","acrobàcies","trapezi","malabars")),
        CulturalSubcategory(CulturalCategory.OTHER, 2, "OTHER__MAGIA", "Màgia", listOf("màgia","il·lusionisme")),
        CulturalSubcategory(CulturalCategory.OTHER, 3, "OTHER__ARTS_VIVES_PERFORMANCE", "Arts vives i performance", listOf("arts vives","performance","experimentació")),
        CulturalSubcategory(CulturalCategory.OTHER, 4, "OTHER__GASTRONOMIA", "Gastronomia", listOf("gastronomia","tastos","cuina","cultura gastronòmica")),
        CulturalSubcategory(CulturalCategory.OTHER, 5, "OTHER__ACTIVITATS_MULTIDISCIPLINARIES", "Activitats multidisciplinàries", listOf("multidisciplinari","diferents disciplines","festival multidisciplinari"))
    )
    private val byCode = allSubcategories.associateBy { it.code }
    private val legacyToCanonical = allSubcategories.associate { it.legacyFollowKey to it.code }

    fun childrenOf(category: CulturalCategory): List<CulturalSubcategory> =
        allSubcategories.filter { it.category == category }

    fun canonicalizeCode(value: String): String =
        legacyToCanonical[value.trim()] ?: value.trim()

    fun canonicalizeFollowSet(values: Set<String>): Set<String> =
        values.map(::canonicalizeCode).filter(String::isNotBlank).toSet()

    fun findByCode(value: String): CulturalSubcategory? = byCode[canonicalizeCode(value)]
    fun isSubcategoryFollowKey(value: String, category: CulturalCategory): Boolean =
        findByCode(value)?.category == category

    fun isAnySubcategoryKey(value: String): Boolean = findByCode(value) != null

    private fun normalized(input: String): String = Normalizer.normalize(input, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), " ").trim()

    /**
     * Les etiquetes explícites del servidor tenen prioritat absoluta.
     * Si encara no han arribat, només es fa coincidència EXACTA amb un estil
     * històric: no inferim punk per una guitarra, ni clàssica per un nom d'artista.
     */
    fun matches(concert: Concert, subcategory: CulturalSubcategory): Boolean {
        if (concert.category != subcategory.category) return false
        if (concert.subcategoryCodes.isNotEmpty()) {
            return concert.subcategoryCodes.any { canonicalizeCode(it) == subcategory.code }
        }
        val ambiguous = setOf("valencia", "valencia catala", "valencia", "musica", "cultura", "art", "activitats", "festival", "espectacle")
        val allowed = (listOf(subcategory.label) + subcategory.aliases)
            .map(::normalized).filter { it.length >= 3 && it !in ambiguous }.toSet()
        return concert.styles.any { style -> normalized(style) in allowed }
    }

    fun isInfantil(concert: Concert): Boolean = concert.isInfantil ||
        concert.category == CulturalCategory.CHILDREN ||
        concert.styles.any { normalized(it) in setOf("infantil", "public infantil", "familiar", "per a xiquets", "families") }
}
