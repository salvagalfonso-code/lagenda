package com.example.agendavalenciana.model

/** User-controlled hide rules apply ONLY to ordinary feed cards.
 * Contracted campaigns and internal ticket-sale cards cannot be suppressed by personal exclusions.
 * Keep this independent of UI to make the commercial guarantee testable. */
object IosFeedExclusionPolicy {
    fun visible(eventId: Int, artists: List<String>, category: String,
                hiddenEventIds: Set<Int>, hiddenArtists: Set<String>, hiddenCategories: Set<String>,
                commerciallyGuaranteed: Boolean): Boolean {
        if (commerciallyGuaranteed) return true
        if (eventId in hiddenEventIds) return false
        val artistKeys = hiddenArtists.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
        if (artists.any { it.trim().lowercase() in artistKeys }) return false
        val categories = hiddenCategories.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
        if (category.trim().lowercase() in categories) return false
        return true
    }
}
