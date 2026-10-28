package com.example.agendavalenciana.ios

/** The strip and the full-screen viewer traverse the same six server stories. */
object IosBatecNavigationPolicy {
    /** null means there is no subsequent story: close the viewer instead of wrapping invisibly. */
    fun move(index: Int, size: Int, delta: Int): Int? =
        (index + delta).takeIf { size > 0 && it in 0 until size }
}
