package com.example.agendavalenciana.model

/** El catàleg inicial té huit disciplines, en l'ordre exacte de les dues pantalles. */
object FirstRunOnboardingPolicy {
    val disciplines = listOf(
        CulturalCategory.MUSIC, CulturalCategory.THEATRE,
        CulturalCategory.CINEMA, CulturalCategory.TALKS,
        CulturalCategory.DANCE, CulturalCategory.EXHIBITIONS,
        CulturalCategory.LITERATURE, CulturalCategory.POPULAR_CULTURE
    )

    /** v297: només preguntem pels grups de música, si s'ha seleccionat Música. */
    fun artistCategories(selected: Set<CulturalCategory>): List<CulturalCategory> =
        if (CulturalCategory.MUSIC in selected) listOf(CulturalCategory.MUSIC) else emptyList()

    /** 0: benvinguda; 1-2: disciplines; 3: estils; 4: llocs; 5: grups de Música (opcional); últim: final. */
    fun finalStep(selected: Set<CulturalCategory>): Int = 5 + artistCategories(selected).size

    fun artistCategoryAtStep(selected: Set<CulturalCategory>, step: Int): CulturalCategory? =
        artistCategories(selected).getOrNull(step - 5)

    /** Seguits i personalització compartixen les mateixes claus estables `subcategoria_code`. */
    fun followKeys(categories: Set<CulturalCategory>, selectedSubcategories: Set<String>): Set<String> {
        val valid = selectedSubcategories.mapNotNull(CategoryTaxonomy::findByCode)
            .filter { it.category in categories }
            .map(CulturalSubcategory::followKey).toSet()
        // Una disciplina sense cap subcategoria explícita equival a la disciplina completa;
        // en les altres només es guarden els codis seleccionats, mai la categoria completa.
        val full = categories.filter { category ->
            valid.none { code -> CategoryTaxonomy.findByCode(code)?.category == category }
        }.map { it.name }.toSet()
        return full + valid
    }
}
