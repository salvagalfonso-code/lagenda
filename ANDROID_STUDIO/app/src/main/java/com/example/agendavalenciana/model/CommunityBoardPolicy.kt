package com.example.agendavalenciana.model

/** Cinc seccions fixes per esdeveniment. Els tipus heretats continuen accessibles. */
object CommunityBoardPolicy {
    val defaultTypes = listOf(
        CommunityGroupType.CAR,
        CommunityGroupType.TRANSPORT,
        CommunityGroupType.MEETUP,
        CommunityGroupType.HOSTING,
        CommunityGroupType.OTHER
    )

    /** Agrupa els taulons antics en les cinc seccions, sense canviar-ne els IDs ni la BD. */
    fun section(type: CommunityGroupType): CommunityGroupType = when (type) {
        CommunityGroupType.CAR -> CommunityGroupType.CAR
        CommunityGroupType.TRANSPORT, CommunityGroupType.TRAIN -> CommunityGroupType.TRANSPORT
        CommunityGroupType.MEETUP, CommunityGroupType.COMPANY -> CommunityGroupType.MEETUP
        CommunityGroupType.HOSTING, CommunityGroupType.NEED_HOSTING -> CommunityGroupType.HOSTING
        else -> CommunityGroupType.OTHER
    }

    fun boardsInSection(boards: List<CommunityGroup>, sectionType: CommunityGroupType): List<CommunityGroup> =
        boards.filter { section(it.type) == sectionType }

    fun title(type: CommunityGroupType, location: String, availablePlaces: Int?, subject: String = ""): String {
        val where = location.trim().replace(Regex("\\s+"), " ")
        val quantity = availablePlaces?.takeIf { it > 0 }?.let { " - $it ${if (it == 1) "plaça" else "places"}" }.orEmpty()
        return when (type) {
            CommunityGroupType.CAR -> "Cotxe compartit" + where.takeIf { it.isNotBlank() }?.let { " des de $it" }.orEmpty() + quantity
            CommunityGroupType.TRANSPORT, CommunityGroupType.TRAIN -> "Transport organitzat" + where.takeIf { it.isNotBlank() }?.let { " des de $it" }.orEmpty() + quantity
            CommunityGroupType.MEETUP, CommunityGroupType.COMPANY -> "Quedada" + where.takeIf { it.isNotBlank() }?.let { " a $it" }.orEmpty()
            CommunityGroupType.HOSTING -> "Compartisc allotjament" + where.takeIf { it.isNotBlank() }?.let { " a $it" }.orEmpty() + quantity
            CommunityGroupType.NEED_HOSTING -> "Busque allotjament" + where.takeIf { it.isNotBlank() }?.let { " a $it" }.orEmpty()
            else -> subject.trim().ifBlank { "Tauló de l'esdeveniment" }.take(160)
        }
    }
}
