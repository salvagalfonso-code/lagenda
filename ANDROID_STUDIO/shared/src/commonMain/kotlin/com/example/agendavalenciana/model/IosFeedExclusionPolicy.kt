package com.example.agendavalenciana.model

import com.example.agendavalenciana.ios.IosCategoryTaxonomy

/** User-controlled hide rules apply ONLY to ordinary feed cards.
 * Contracted campaigns and internal ticket-sale cards cannot be suppressed by personal exclusions.
 * Keep this independent of UI to make the commercial guarantee testable. */
object IosFeedExclusionPolicy {
    fun visible(eventId: Int, artists: List<String>, category: String,
                hiddenEventIds: Set<Int>, hiddenArtists: Set<String>, hiddenCategories: Set<String>,
                commerciallyGuaranteed: Boolean, styles: List<String> = emptyList()): Boolean {
        if (commerciallyGuaranteed) return true
        if (eventId in hiddenEventIds) return false
        val artistKeys = hiddenArtists.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
        // Android v336: a mixed lineup remains visible until ALL named artists are hidden.
        val meaningful = artists.map(String::trim).filter(String::isNotBlank).distinctBy(String::lowercase)
        if (meaningful.isNotEmpty() && meaningful.all { it.lowercase() in artistKeys }) return false
        val categories = hiddenCategories.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
        if (category.trim().lowercase() in categories) return false
        val blockedMain = hiddenCategories.mapNotNull(IosCategoryTaxonomy::mainKey).toSet()
        if (IosCategoryTaxonomy.mainKey(category) in blockedMain) return false
        val blockedChildren = hiddenCategories.mapNotNull(IosCategoryTaxonomy::subcategoryCode).toSet()
        if (blockedChildren.isNotEmpty() && IosCategoryTaxonomy.matchesEvent(category, styles,
                IosCategoryTaxonomy.normalized(emptySet(), blockedChildren))) return false
        return true
    }
}
