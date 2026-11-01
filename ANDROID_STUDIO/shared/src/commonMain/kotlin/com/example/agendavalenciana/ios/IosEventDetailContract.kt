package com.example.agendavalenciana.ios

/** UI contract: one server-owned event, shared ticket and community endpoints. */
object IosEventDetailContract {
    /** The festival programme remains available until the dedicated step 26. */
    fun tabs(isFestival: Boolean): List<String> =
        if (isFestival) listOf("Horaris", "Info", "Entrades", "Ubicació", "Comunitat")
        else listOf("Info", "Entrades", "Ubicació", "Comunitat")

    fun isSafeExternalTicketUrl(url: String): Boolean = url.startsWith("https://", ignoreCase = true)

    fun placeQuery(venue: String, town: String): String =
        listOf(venue.trim(), town.trim()).filter(String::isNotEmpty).distinct().joinToString(", ")

    fun communityTypeLabel(code: String): String = when (code.uppercase()) {
        "CAR" -> "Cotxe compartit"
        "TRANSPORT" -> "Transport organitzat"
        "MEETUP" -> "Quedades"
        "HOSTING" -> "Allotjament compartit"
        else -> IosCommunityBoardContract.label(code)
    }
}
