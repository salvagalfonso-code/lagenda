package com.example.agendavalenciana.model

/** Parelles coherents i pàgines rotatives per als suggeriments de seguiment. */
object FollowSuggestionPaging {
    fun <T> pairs(items: List<T>, isCategory: (T) -> Boolean): List<List<T>> {
        val available = items.toMutableList()
        val result = mutableListOf<List<T>>()
        while (available.isNotEmpty()) {
            val first = available.removeAt(0)
            val pair = mutableListOf(first)
            val otherIndex = available.indexOfFirst { other -> !isCategory(first) || !isCategory(other) }
            if (otherIndex >= 0) pair += available.removeAt(otherIndex)
            result += pair
        }
        return result
    }

    fun <T> inSlot(deck: List<List<T>>, slot: Int, rotation: Int, limit: Int): List<T> {
        if (deck.isEmpty() || limit <= 0) return emptyList()
        val rowsPerPage = 2
        val pageCount = (deck.size + rowsPerPage - 1) / rowsPerPage
        val rowIndex = ((rotation.coerceAtLeast(0) % pageCount) * rowsPerPage + slot.coerceAtLeast(0)) % deck.size
        return deck[rowIndex].take(limit)
    }
    /** Substituïx una sola targeta; la companya no es toca. */
    fun <T> replacement(
        candidates: List<T>,
        excludedKeys: Set<String>,
        keyOf: (T) -> String,
        isCategory: (T) -> Boolean,
        isFollowing: (T) -> Boolean,
        categoryAlreadyVisible: Boolean
    ): T? = candidates.firstOrNull { item ->
        !isFollowing(item) && keyOf(item) !in excludedKeys &&
            !(categoryAlreadyVisible && isCategory(item))
    }

}
