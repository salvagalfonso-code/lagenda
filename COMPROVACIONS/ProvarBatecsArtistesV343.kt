import com.example.agendavalenciana.model.BatecCandidate
import com.example.agendavalenciana.model.IosBatecArtistGroups
import com.example.agendavalenciana.ios.IosBatecNavigationPolicy

fun main() {
    val names = listOf("La Maria", "Zoo", "Feliu Ventura", "El Diluvi", "Auxili", "Al Tall", "Oques Grasses")
    val manyOfFirst = (1..7).map { BatecCandidate("manual:$it", "La Maria") }
    val other = names.drop(1).mapIndexed { i, name -> BatecCandidate("event:${i+11}", name) }
    fun select(c:List<BatecCandidate>, followed:Set<String>, seen:Set<String> = emptySet()) =
        IosBatecArtistGroups.select(c,followed,seen)
    check(select(manyOfFirst, names.toSet()).isEmpty()) { "Seven publications from one artist must not fake six artists" }
    check(select(manyOfFirst + other.take(4),names.toSet()).isEmpty()) { "Five distinct artists are insufficient" }
    val six = select(manyOfFirst + other.take(5),names.toSet())
    check(six.size==6) { "Six followed artists should display six circles" }
    check(six.first().storyKeys.size==7) { "The first circle should aggregate the seven publications" }
    check(six.first().openingStoryKey=="manual:1")
    val partlySeen = select(manyOfFirst + other.take(5), names.toSet(), setOf("manual:1", "manual:2"))
    check(partlySeen.first().openingStoryKey == "manual:3")
    check(!partlySeen.first().allSeen)
    val seen = select(manyOfFirst + other.take(5),names.toSet(),manyOfFirst.map{it.storyKey}.toSet())
    check(seen.first().allSeen && !seen.last().allSeen)
    check(seen.first().openingStoryKey=="manual:1")
    val duplicates=select((manyOfFirst+other.take(5))+BatecCandidate("manual:1","Zoo"),names.toSet())
    check(duplicates.first().storyKeys.size==7 && duplicates[1].storyKeys.size==1) { "Never assign one ID to two artists" }
    check(select(manyOfFirst + other, names.take(6).map(String::lowercase).toSet()).size==6)
    check(select(manyOfFirst + other, setOf("Unfollowed")).isEmpty())
    check(select(manyOfFirst + other.take(5) + BatecCandidate("invalid","Zoo"),names.toSet()).size==6)
    check(IosBatecNavigationPolicy.move(0,6,1)==1)
    check(IosBatecNavigationPolicy.move(5,6,1)==null)
    check(IosBatecNavigationPolicy.move(0,6,-1)==null)
    println("16 comprovacions de Batecs superades")
}
