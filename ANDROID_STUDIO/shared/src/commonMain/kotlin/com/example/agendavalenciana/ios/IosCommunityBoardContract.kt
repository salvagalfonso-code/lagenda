package com.example.agendavalenciana.ios

/** Android v336 CommunityBoardPolicy: five visible sections, eleven underlying types.
 * The PHP community_groups.group_type is a five-type legacy field; board-settings.php
 * keeps the exact additional type and chat preference for the SAME group ID.
 */
internal object IosCommunityBoardContract {
    val sections = listOf("CAR", "TRANSPORT", "MEETUP", "HOSTING", "OTHER")
    val allowedTypes = setOf("CAR", "TRAIN", "TRANSPORT", "HOSTING", "NEED_HOSTING",
        "COMPANY", "MEETUP", "WOMEN_ONLY", "VEG_FRIENDLY", "LGBTQ_FRIENDLY", "OTHER")

    fun section(raw: String): String = when(raw.uppercase()) {
        "CAR" -> "CAR"
        "TRANSPORT", "TRAIN" -> "TRANSPORT"
        "MEETUP", "COMPANY" -> "MEETUP"
        "HOSTING", "NEED_HOSTING" -> "HOSTING"
        else -> "OTHER"
    }
    fun serverType(raw: String): String = when(section(raw)) {
        "HOSTING" -> "OTHER" // Android v336 legacy PHP stores all non-car/transport/meetup as OTHER.
        else -> section(raw)
    }
    fun canonicalType(raw: String): String = raw.uppercase().takeIf { it in allowedTypes } ?: "OTHER"
    fun label(raw: String): String = when(canonicalType(raw)) {
        "CAR" -> "Cotxe compartit"
        "TRAIN" -> "Tren"
        "TRANSPORT" -> "Transport organitzat"
        "HOSTING" -> "Allotjament compartit"
        "NEED_HOSTING" -> "Busque allotjament"
        "COMPANY" -> "Busque gent per anar"
        "MEETUP" -> "Quedada"
        "WOMEN_ONLY" -> "Sols xiques"
        "VEG_FRIENDLY" -> "Vegans / vegetarians"
        "LGBTQ_FRIENDLY" -> "LGTBIQ+ friendly"
        else -> "Altres"
    }
    fun defaultTitle(type: String, location: String, places: Int, subject: String = ""): String {
        val where = location.trim().replace(Regex("\\s+"), " ")
        val available = if (places > 0) " - $places ${if (places == 1) "plaça" else "places"}" else ""
        return when(canonicalType(type)) {
            "CAR" -> "Cotxe compartit${if (where.isNotEmpty()) " des de $where" else ""}$available"
            "TRANSPORT", "TRAIN" -> "Transport organitzat${if (where.isNotEmpty()) " des de $where" else ""}$available"
            "MEETUP", "COMPANY" -> "Quedada${if (where.isNotEmpty()) " a $where" else ""}"
            "HOSTING" -> "Compartisc allotjament${if (where.isNotEmpty()) " a $where" else ""}$available"
            "NEED_HOSTING" -> "Busque allotjament${if (where.isNotEmpty()) " a $where" else ""}"
            else -> subject.trim().ifBlank { "Tauló de l'esdeveniment" }.take(160)
        }
    }
    fun validCreate(eventId: Int, title: String, type: String, capacity: Int, meeting: String): Boolean =
        eventId > 0 && title.isNotBlank() && title.length <= 160 && canonicalType(type) == type &&
            capacity in 0..100 && (meeting.isBlank() || Regex("\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}(:\\d{2})?").matches(meeting))
}
