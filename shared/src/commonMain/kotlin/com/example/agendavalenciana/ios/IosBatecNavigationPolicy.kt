package com.example.agendavalenciana.ios

/** Android v336 Batecs: six stable artist groups; swipe and tap traverse whole groups. */
object IosBatecNavigationPolicy {
    sealed interface Navigation {
        data class Select(val index: Int) : Navigation
        data object Restart : Navigation
        data object Close : Navigation
    }

    /** Back at the first story restarts its timer; forward at last closes the viewer. */
    fun navigate(index: Int, size: Int, delta: Int): Navigation {
        if (size <= 0 || index !in 0 until size) return Navigation.Close
        if (delta < 0 && index == 0) return Navigation.Restart
        val next = index + delta
        return if (next in 0 until size) Navigation.Select(next) else Navigation.Close
    }

    /** Left half goes backwards, right half goes forwards (Android story-viewer rule). */
    fun tapDirection(x: Float, width: Float): Int =
        if (width <= 0f || x < width / 2f) -1 else 1

    /** No accidental navigation on short horizontal drags or vertical scrolling. */
    fun swipeDirection(horizontalDelta: Float, threshold: Float): Int = when {
        threshold <= 0f -> 0
        horizontalDelta <= -threshold -> 1
        horizontalDelta >= threshold -> -1
        else -> 0
    }

    /** Kept for compatibility with earlier step contracts. */
    fun move(index: Int, size: Int, delta: Int): Int? =
        (index + delta).takeIf { size > 0 && it in 0 until size }
}
