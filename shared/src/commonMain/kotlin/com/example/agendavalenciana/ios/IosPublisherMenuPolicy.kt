package com.example.agendavalenciana.ios

/** Role/ownership rules mirrored from Android PublishActionsSheet (not client-side authorization). */
internal enum class IosPublishAction(val title: String, val glyph: String) {
    NEW_EVENT("Nou esdeveniment", "plus"),
    EDIT_EVENTS("Editar esdeveniments", "edit"),
    CREATE_AD("Crear anunci", "campaign"),
    TICKETS("Entrades, ingressos i facturació", "ticket"),
    MERCH("Vendre discos / merch", "disc"),
    DONATIONS("Donacions", "campaign"),
    REVIEW_PUBLICATIONS("Revisar publicacions", "check"),
    PROFESSIONAL_CLAIMS("Sol·licituds professionals", "people")
}

internal data class IosPublisherMenuState(
    val heading: String,
    val subtitle: String,
    val actions: List<IosPublishAction>,
    val reviewActions: List<IosPublishAction>,
    val claimLabel: String?,
    val suspended: Boolean
)

internal object IosPublisherMenuPolicy {
    private val professionalRoles = setOf("PROFESSIONAL", "PUBLISHER", "ORGANIZER", "GROUP", "AGENCY", "LABEL", "VENUE", "PROMOTER")
    fun isProfessional(role: String?): Boolean = role?.uppercase() in professionalRoles || isSuperAdmin(role)
    fun isSuperAdmin(role: String?): Boolean = role?.uppercase() == "SUPER_ADMIN"

    fun menu(role: String?, hasPublishedEvents: Boolean, suspendedByReport: Boolean = false): IosPublisherMenuState {
        val admin = isSuperAdmin(role)
        val pro = isProfessional(role)
        val actions = buildList {
            add(IosPublishAction.NEW_EVENT)
            if (hasPublishedEvents) {
                add(IosPublishAction.EDIT_EVENTS)
                add(IosPublishAction.CREATE_AD)
            }
            if (pro) {
                add(IosPublishAction.TICKETS)
                add(IosPublishAction.MERCH)
                add(IosPublishAction.DONATIONS)
            }
        }
        return IosPublisherMenuState(
            heading = if (pro) "Panell professional" else "Publicar",
            subtitle = when {
                suspendedByReport -> "Publicació temporalment suspesa. Els teus esdeveniments i vendes es conserven."
                pro -> "Publica directament i gestiona entrades, ingressos, esdeveniments i anuncis."
                role == null -> "Pots publicar esdeveniments sense registrar-te. Els revisarem després de publicar-los; la venda d'entrades interna necessita compte."
                else -> "Publica activitats directament. Les publicacions són revisades periòdicament."
            },
            actions = actions,
            reviewActions = if (admin) listOf(IosPublishAction.REVIEW_PUBLICATIONS,IosPublishAction.PROFESSIONAL_CLAIMS) else emptyList(),
            claimLabel = if (admin) null else if (pro) "Reclamar un altre grup o perfil" else "Soc organitzador o artista",
            suspended = suspendedByReport
        )
    }
}
