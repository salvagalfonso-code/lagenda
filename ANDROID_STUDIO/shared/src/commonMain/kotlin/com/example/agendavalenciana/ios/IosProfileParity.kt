package com.example.agendavalenciana.ios

/** Identity and profile fields supplied by Android v336's existing endpoints. */
internal object IosProfileParity {
    fun sameUsername(a: String, b: String): Boolean =
        a.isNotBlank() && b.isNotBlank() && a.trim().equals(b.trim(), ignoreCase = true)

    fun validateIdentity(username: String, email: String, homeTown: String): Triple<String, String, String> {
        val name = username.trim()
        val mail = email.trim()
        val town = homeTown.trim()
        require(name.length in 3..190 && !name.any(Char::isWhitespace)) { "Nom d'usuari no vàlid" }
        require(mail.length in 5..254 && mail.count { it == '@' } == 1 &&
            mail.substringAfter('@').contains('.') && !mail.any(Char::isWhitespace)) { "Correu no vàlid" }
        require(town.length <= 190) { "Població massa llarga" }
        return Triple(name, mail, town)
    }
    fun validateProfile(name: String, bio: String): Pair<String, String> {
        val visible = name.trim()
        val text = bio.trim()
        require(visible.length in 1..190 && text.length <= 3000) { "Nom o biografia no vàlids" }
        return visible to text
    }
    fun sameProfessionalFields(desired: IosMyProfile, actual: IosMyProfile): Boolean =
        desired.discipline == actual.discipline && desired.companyName == actual.companyName &&
        desired.castText == actual.castText && desired.publisherName == actual.publisherName &&
        desired.booksText == actual.booksText && desired.worksText == actual.worksText &&
        desired.spacesText == actual.spacesText
}
