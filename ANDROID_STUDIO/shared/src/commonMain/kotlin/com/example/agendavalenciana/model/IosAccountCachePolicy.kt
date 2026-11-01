package com.example.agendavalenciana.model

/** Keys cannot overlap between guest, user A and user B on a shared device. */
object IosAccountCachePolicy {
    fun key(name: String, userId: Int?): String = "${name}_${userId ?: "guest"}"
    fun reminderKey(userId: Int, eventId: Int): String = "reminder_${userId}_${eventId}_at"
    /** Explicit empty selections must not resurrect legacy preferences. */
    fun encode(values: Set<String>): String = if (values.isEmpty()) "~" else values.joinToString("|")
    fun decode(scoped: String, legacyGuest: String, isGuest: Boolean): Set<String> {
        val value = when {
            scoped == "~" -> ""
            scoped.isNotBlank() -> scoped
            isGuest -> legacyGuest
            else -> ""
        }
        return value.split('|').map(String::trim).filter(String::isNotBlank).toSet()
    }
}
