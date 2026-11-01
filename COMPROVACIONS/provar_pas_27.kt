import com.example.agendavalenciana.ios.*

fun main() {
    var assertions = 0
    fun verify(value: Boolean) { check(value); assertions++ }
    verify(IosArtistPagePolicy.sameArtist("La Maria", " la  maria "))
    verify(!IosArtistPagePolicy.sameArtist("La Maria", "La Marina"))
    verify(!IosArtistPagePolicy.sameArtist("", ""))
    verify(IosArtistPagePolicy.imageUrl("https://cdn.org/a.jpg") == "https://cdn.org/a.jpg")
    verify(IosArtistPagePolicy.imageUrl("/app/img.jpg") == "/app/img.jpg")
    verify(IosArtistPagePolicy.imageUrl("http://bad.org/a.jpg").isEmpty())
    verify(IosArtistPagePolicy.imageUrl("//bad.org/a.jpg").isEmpty())
    verify(IosArtistPagePolicy.imageUrl("/../bad.jpg").isEmpty())
    verify(IosArtistPagePolicy.imageUrl("/hello world.jpg")=="/hello world.jpg")
    verify(IosArtistPagePolicy.money(1235) == "12,35 €")
    verify(IosArtistPagePolicy.money(0) == "0,00 €")
    verify(IosArtistPagePolicy.money(-5) == "0,00 €")
    verify(IosArtistPagePolicy.inStock(null))
    verify(!IosArtistPagePolicy.inStock(0))
    verify(IosArtistPagePolicy.inStock(3))
    verify(IosArtistPagePolicy.isMusic("Música"))
    verify(IosArtistPagePolicy.isMusic("MUSIC"))
    verify(!IosArtistPagePolicy.isMusic("Teatre"))
    verify(IosArtistPagePolicy.activityHeading("Teatre") == "Espectacles")
    verify(IosArtistPagePolicy.activityHeading("Dansa") == "Espectacles")
    verify(IosArtistPagePolicy.activityHeading("Cinema") == "Projeccions i activitats")
    verify(IosArtistPagePolicy.activityHeading("Música") == "Concerts")
    verify(IosArtistPagePolicy.discipline(listOf("Música","Teatre","Música")) == "música")
    verify(IosArtistPagePolicy.discipline(emptyList()).isBlank())
    println("PAS27_KOTLIN_POLICY_PASS: $assertions verificacions")
}
