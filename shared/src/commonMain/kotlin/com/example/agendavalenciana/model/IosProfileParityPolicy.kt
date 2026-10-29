package com.example.agendavalenciana.model

/** UI-only profile policy: the server remains authoritative for sessions and role changes. */
object IosProfileParityPolicy {
    fun guest(accountId: Int?): Boolean = accountId == null || accountId <= 0
    fun superAdmin(role: String): Boolean = role.equals("SUPER_ADMIN", ignoreCase = true)
    fun professional(role: String): Boolean = !superAdmin(role) && role.uppercase() !in setOf("PUBLIC", "USER", "")
    fun validName(value: String): Boolean = value.trim().isNotBlank() && value.trim().length <= 190
    fun validUsername(value: String): Boolean = value.trim().length in 3..80
    fun validEmail(value: String): Boolean {
        val email = value.trim()
        return email.length <= 190 && email.count { it == '@' } == 1 &&
            email.substringBefore('@').isNotBlank() &&
            email.substringAfter('@').split('.').let { it.size >= 2 && it.all(String::isNotBlank) }
    }
    fun validProfessionalPhone(value: String): Boolean = value.trim().matches(Regex("[+0-9() .-]{7,24}"))
    fun roleChangePermitted(role: String, serverSettingsLoaded: Boolean): Boolean =
        !superAdmin(role) && serverSettingsLoaded
}
