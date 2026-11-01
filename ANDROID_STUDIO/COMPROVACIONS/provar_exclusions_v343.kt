import com.example.agendavalenciana.model.IosFeedExclusionPolicy as Rule
fun main() {
    fun checkRule(expected: Boolean, id:Int, artists:List<String>, category:String,
                  ids:Set<Int>, hideArtists:Set<String>, hideCategories:Set<String>, commercial:Boolean) {
        check(Rule.visible(id, artists, category, ids, hideArtists, hideCategories, commercial) == expected)
    }
    checkRule(false, 1, listOf("A"), "Música", setOf(1), emptySet(), emptySet(), false)
    checkRule(false, 1, listOf("Artista A"), "Teatre", emptySet(), setOf("ARTISTA A"), emptySet(), false)
    checkRule(false, 1, emptyList(), "Dansa", emptySet(), emptySet(), setOf("dansa"), false)
    checkRule(true, 1, listOf("Artista A"), "Dansa", setOf(1), setOf("Artista A"), setOf("Dansa"), true)
    checkRule(true, 1, emptyList(), "Música", emptySet(), emptySet(), emptySet(), false)
    checkRule(true, 2, listOf("B"), "Música", setOf(1), setOf("A"), setOf("Dansa"), false)
    println("PASS: 6 casos d'exclusions i garantia comercial")
}
