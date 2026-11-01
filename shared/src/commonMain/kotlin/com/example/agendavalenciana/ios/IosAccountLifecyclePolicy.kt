package com.example.agendavalenciana.ios

/** Guard rails around the EXISTING v336 / iOS supplemental authentication contracts.
 * All irreversible decisions are made by the PHP server, never by local claims.
 */
internal object IosAccountLifecyclePolicy {
    fun requireLoginReceipt(id: Int, username: String, bearer: String) {
        require(id > 0 && username.isNotBlank()) { "El servidor no ha confirmat un usuari real" }
        require(bearer.matches(Regex("[a-fA-F0-9]{64}"))) { "Bearer del servidor invàlid" }
    }
    fun requireSocialCredential(idToken: String, nonce: String) {
        require(idToken.length in 101..12000 && idToken.count { it == '.' } == 2) {
            "Token social incomplet"
        }
        require(nonce.length in 16..256 && nonce.all {
            it.isLetterOrDigit() || it == '_' || it == '-'
        }) { "Nonce invàlid" }
    }
    fun requireDeleteConfirmation(phrase: String, id: Int) {
        require(id > 0 && phrase == "ELIMINAR") { "Cal confirmar ELIMINAR des d'un compte registrat" }
    }
    fun requireDeleteReceipt(deleted: Boolean, mode: String) {
        check(deleted && mode in setOf("DELETED", "ANONYMIZED_LEGAL_RETENTION")) {
            "El servidor no ha confirmat la baixa del compte"
        }
    }
}
