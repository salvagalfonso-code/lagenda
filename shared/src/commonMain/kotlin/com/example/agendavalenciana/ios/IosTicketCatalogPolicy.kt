package com.example.agendavalenciana.ios

/** Pas 37/48. Les modalitats i el contingent provenen exclusivament de tickets/options.php.
 * Cap selecció es guarda permanentment al dispositiu. L'estoc final el valida el servidor.
 */
data class IosTicketOption(
    val id: Int,
    val name: String,
    val price: Double,
    val remaining: Int?,
    val maxPerOrder: Int,
    val sessionDate: String,
    val sessionTime: String,
    val sector: String = "",
    val offer: String = "",
    val capacity: Int? = null,
    val sold: Int = 0,
    val reserved: Int = 0,
    val saleStartsAt: String = "",
    val saleEndsAt: String = "",
    val active: Boolean = true
) {
    val available: Int? get() = capacity?.let { (it - sold - reserved).coerceAtLeast(0) } ?: remaining
    val soldOut: Boolean get() = available == 0
}

data class IosTicketCatalog(val options: List<IosTicketOption>, val capacity: Int?)

/** No fa inferències sobre la disponibilitat quan el servidor no envia contingent.
 * El catàleg accepta un màxim configurat de 20, però l'Android v336 envia com a màxim
 * 6 entrades per POST a tickets/purchase.php. La selecció respecta este límit d'API.
 */
object IosTicketCatalogPolicy {
    fun valid(option: IosTicketOption): Boolean =
        option.id > 0 && option.name.isNotBlank() && option.price.isFinite() && option.price >= 0.0 &&
        option.maxPerOrder in 1..20 && (option.capacity == null || option.capacity >= 0) &&
        option.sold >= 0 && option.reserved >= 0 && (option.remaining == null || option.remaining >= 0)

    fun visible(options: List<IosTicketOption>, culturalToday: String = ""): List<IosTicketOption> =
        options.asSequence().filter(::valid).filter { it.active }
            .filter { culturalToday.isBlank() || it.sessionDate.isBlank() || it.sessionDate.take(10) >= culturalToday }
            .distinctBy { it.id }
            .sortedWith(compareBy<IosTicketOption> { it.sessionDate.isNotBlank() }
                .thenBy { it.sessionDate }.thenBy { it.sessionTime }
                .thenBy { it.sector }.thenBy { it.name }).toList()

    fun days(options: List<IosTicketOption>): List<String> =
        options.map { it.sessionDate.take(10) }.filter { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }
            .distinct().sorted()

    fun sectors(options: List<IosTicketOption>): List<String> =
        options.map { it.sector.trim() }.filter(String::isNotEmpty).distinct().sorted()

    fun filter(options: List<IosTicketOption>, day: String = "", sector: String = ""): List<IosTicketOption> =
        options.filter { (day.isBlank() || it.sessionDate.isBlank() || it.sessionDate.take(10) == day) &&
            (sector.isBlank() || it.sector.isBlank() || it.sector == sector) }

    fun quantityLimit(option: IosTicketOption?): Int = option?.takeIf(::valid)?.let {
        minOf(it.maxPerOrder, it.available ?: it.maxPerOrder, 6)
    }?.coerceAtLeast(0) ?: 0

    fun selectable(option: IosTicketOption?): Boolean = option != null && option.active && quantityLimit(option) > 0

    fun details(option: IosTicketOption): String =
        listOf(option.sessionDate.takeIf(String::isNotBlank), option.sessionTime.takeIf(String::isNotBlank),
            option.sector.takeIf(String::isNotBlank)?.let { "Zona: $it" },
            option.offer.takeIf(String::isNotBlank)).filterNotNull().joinToString(" · ")
}
