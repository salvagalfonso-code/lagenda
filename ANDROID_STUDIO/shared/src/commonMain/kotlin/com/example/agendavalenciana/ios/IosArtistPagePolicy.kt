package com.example.agendavalenciana.ios

/** Pas 27: public artist data from Android v336 endpoints, no local user storage.
 * Strict identity comparison matters: a substring match can show a different artist's merchandise.
 */
data class IosArtistProfessional(
    val name: String, val imageUrl: String = "", val style: String = "", val language: String = ""
)
data class IosArtistProduct(
    val id: Long, val artist: String, val name: String, val type: String,
    val description: String, val imageUrl: String, val priceCents: Long,
    val shippingCents: Long, val stock: Int?, val active: Boolean = true
)
data class IosArtistPublicPage(
    val artist: String, val profile: IosArtistProfessional?,
    val products: List<IosArtistProduct>, val storeMessage: String = ""
)

object IosArtistPagePolicy {
    fun key(name: String): String = name.trim().replace(Regex("\\s+"), " ").lowercase()
    fun sameArtist(a: String, b: String): Boolean = a.isNotBlank() && key(a) == key(b)
    /** Only use a profile photo, not a poster of a potentially multi-artist event. */
    fun imageUrl(url: String): String = url.trim().takeIf {
        (it.startsWith("https://", ignoreCase = true) && !it.any(Char::isWhitespace)) ||
        (it.startsWith("/") && !it.startsWith("//") && !it.contains(".."))
    }.orEmpty()
    fun money(cents: Long): String {
        val safe = cents.coerceAtLeast(0)
        return "${safe / 100},${(safe % 100).toString().padStart(2, '0')} €"
    }
    fun inStock(stock: Int?): Boolean = stock == null || stock > 0
    fun discipline(categories: List<String>): String = categories.filter(String::isNotBlank)
        .groupingBy { key(it) }.eachCount().maxByOrNull { it.value }?.key.orEmpty()
    fun isMusic(category: String): Boolean = key(category).let {
        it.contains("músic") || it.contains("music") || it.contains("concert")
    }
    fun activityHeading(category: String): String = when {
        isMusic(category) -> "Concerts"
        key(category).contains("teatre") || key(category).contains("dansa") -> "Espectacles"
        key(category).contains("cinema") -> "Projeccions i activitats"
        key(category).contains("expos") -> "Exposicions i activitats"
        else -> "Activitats"
    }
}
