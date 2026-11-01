package com.example.agendavalenciana.ios

/** Android v336 and PHP user-state/follow.php use the same stable entity_type + entity_key.
 * The displayed label is metadata, never a primary key for removing another follow.
 */
object IosFollowMutation {
    val supported = setOf("ARTIST", "ORGANIZER", "VENUE", "CATEGORY")
    fun canonicalKey(raw: String): String = raw.trim().replace(Regex("\\s+"), " ").lowercase()
    fun sameKey(type: String, first: String, second: String): Boolean =
        type.uppercase() in supported && canonicalKey(first).isNotBlank() &&
            canonicalKey(first) == canonicalKey(second)

    /** UI can accept a label when the catalog exposes no ID. Writes require entity_key. */
    fun contains(items: List<IosFollow>, type: String, keyOrLabel: String): Boolean =
        type.uppercase() in supported && canonicalKey(keyOrLabel).isNotBlank() && items.any {
            it.type.equals(type, ignoreCase = true) &&
                (sameKey(type, it.key, keyOrLabel) ||
                    (it.key.isBlank() && canonicalKey(it.label) == canonicalKey(keyOrLabel)))
        }

    fun updated(items: List<IosFollow>, type: String, key: String, label: String, follow: Boolean): List<IosFollow> {
        require(type.uppercase() in supported) { "Tipus de seguiment no admés" }
        require(canonicalKey(key).isNotBlank() && label.isNotBlank()) { "Falta l'entitat" }
        val cleanType = type.uppercase()
        val retained = items.filterNot {
            it.type.equals(cleanType, ignoreCase = true) && sameKey(cleanType, it.key, key)
        }
        return if (follow) retained + IosFollow(cleanType, canonicalKey(key), label.trim()) else retained
    }
}
