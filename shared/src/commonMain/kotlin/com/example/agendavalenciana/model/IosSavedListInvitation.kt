package com.example.agendavalenciana.model

/** Server accepts a 10/16-digit invitation code or 48-character share token.
 * A pasted official invite URL is converted to the same server-side token.
 */
object IosSavedListInvitation {
    private val valid = Regex("(?:[a-f0-9]{48}|[A-Fa-f0-9]{10}|[A-Fa-f0-9]{16})")
    private const val urlPrefix = "https://agendaval.com/app/lists.php?invite="
    fun parse(input: String): String? {
        val trimmed = input.trim()
        val candidate = if (trimmed.startsWith(urlPrefix)) {
            trimmed.removePrefix(urlPrefix).substringBefore('&').substringBefore('#')
        } else trimmed
        return candidate.takeIf { valid.matches(it) }
    }
}
