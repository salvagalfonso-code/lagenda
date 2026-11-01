package com.example.agendavalenciana.ios

/** Pas 9. Codis estables de l'idioma de la interfície en Android v336
 * (UiLanguageStore.KEY == selected_language). No canvien l'idioma dels actes.
 * La preferència valencian_first és independent.
 */
internal enum class IosUiLanguage(val code: String, val label: String) {
    VALENCIAN("ca", "Valencià"),
    SPANISH("es", "Castellano"),
    ENGLISH("en", "English");

    companion object {
        fun fromServer(code: String?): IosUiLanguage? = entries.firstOrNull { it.code == code }
    }
}

/** Les tres claus del domini preferences/feed.php, exactament les del PHP existent. */
internal object IosFeedPreferencesContract {
    const val centralUiLanguageKey = "selected_language"
    const val centralSuggestionsKey = "show_suggestions"
    const val centralDiscoveryKey = "discovery_level"
    const val centralValencianFirstKey = "valencian_first"

    fun validDiscovery(code: String): Boolean = code in setOf("MY_TASTES", "BALANCED", "EXPLORE")
    /** Quan les suggerències estan desactivades el nivell es conserva, però s'oculta. */
    fun displayDiscovery(suggestions: Boolean): Boolean = suggestions
}
