package com.example.agendavalenciana.ios

/** Artist bells are separate from follows and require authenticated per-user persistence. */
data class IosArtistBell(val artistName: String, val frequency: String, val enabled: Boolean)

object IosArtistBellPolicy {
    val frequencies: Set<String> = setOf("IMMEDIATE", "DAILY", "WEEKLY")
    fun parseFrequency(value: String): String = value.uppercase().takeIf { it in frequencies } ?: "WEEKLY"
    fun key(artist: String): String = artist.trim().replace(Regex("\\s+"), " ").lowercase()
    fun label(frequency: String): String = when (frequency) {
        "IMMEDIATE" -> "Al moment"; "DAILY" -> "Diari"; else -> "Setmanal"
    }
    fun update(current: Map<String, IosArtistBell>, bell: IosArtistBell): Map<String, IosArtistBell> {
        val next = current.toMutableMap()
        if (bell.enabled) next[key(bell.artistName)] = bell
        else next.remove(key(bell.artistName))
        return next
    }
}
