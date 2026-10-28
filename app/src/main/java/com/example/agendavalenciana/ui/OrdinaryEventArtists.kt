package com.example.agendavalenciana.ui

/**
 * Una activitat municipal, una visita o un taller no és automàticament un artista.
 * La vinculació explícita d'un artista a una sessió és la dada més fiable.
 */
internal fun ordinaryEventArtists(
    title: String,
    primaryArtist: String,
    lineup: List<String>,
    organizers: List<String>,
    isStageEvent: Boolean,
    scheduledArtists: List<String>
): List<String> {
    val explicit = scheduledArtists.map(String::trim).filter(String::isNotBlank)
    val genericActivities = Regex("^(visita|visites|taller|tallers|activitat|activitats|ruta|rutes|exposici[oó]|xarrada|confer[eè]ncia|jornada|jornades|fira|mercat|portes obertes|presentaci[oó]|sessió|sessio|recorregut)\\b", RegexOption.IGNORE_CASE)
    val genericOrganizations = Regex("^(ajuntament|diputaci[oó]|generalitat|biblioteca|museu|centre cultural|casa de cultura|oficina de turisme|regidoria|associaci[oó])\\b", RegexOption.IGNORE_CASE)
    val candidates = explicit + if (isStageEvent) (lineup + primaryArtist) else emptyList()
    return candidates.map(String::trim).filter(String::isNotBlank)
        .filter { name ->
            val explicitArtist = explicit.any { it.equals(name, ignoreCase = true) }
            organizers.none { it.equals(name, ignoreCase = true) } &&
                !genericOrganizations.containsMatchIn(name) &&
                (explicitArtist || (
                    !genericActivities.containsMatchIn(name) &&
                    !(name.equals(title, ignoreCase = true) && genericActivities.containsMatchIn(title))
                ))
        }
        .distinctBy { it.lowercase() }
}
