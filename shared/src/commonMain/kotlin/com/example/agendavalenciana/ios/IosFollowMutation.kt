package com.example.agendavalenciana.ios

/** Immutable post-API-success follow state. Never delete another entity type with the same label. */
object IosFollowMutation {
    val supported = setOf("ARTIST", "ORGANIZER", "VENUE", "CATEGORY")
    fun contains(items: List<IosFollow>, type: String, keyOrLabel: String): Boolean =
        type.uppercase() in supported && keyOrLabel.isNotBlank() && items.any {
            it.type.equals(type, ignoreCase = true) &&
                (it.key.equals(keyOrLabel, ignoreCase = true) || it.label.equals(keyOrLabel, ignoreCase = true))
        }

    fun updated(items: List<IosFollow>, type: String, key: String, label: String, follow: Boolean): List<IosFollow> {
        require(type in supported) { "Tipus de seguiment no admés" }
        require(key.isNotBlank() && label.isNotBlank()) { "Falta el nom de l'entitat" }
        val retained = items.filterNot {
            it.type.equals(type, ignoreCase = true) &&
                (it.key.equals(key, ignoreCase = true) || it.label.equals(label, ignoreCase = true))
        }
        return if (follow) retained + IosFollow(type, key.trim(), label.trim()) else retained
    }
}
