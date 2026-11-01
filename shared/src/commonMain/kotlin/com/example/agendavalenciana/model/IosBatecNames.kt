package com.example.agendavalenciana.model

/** Portable subset of Android normalizeSearchValue for artist identities. */
object IosBatecNames {
    fun canonical(raw: String): String = raw.trim().lowercase()
        .replace('à', 'a').replace('á', 'a').replace('â', 'a').replace('ä', 'a')
        .replace('è', 'e').replace('é', 'e').replace('ê', 'e').replace('ë', 'e')
        .replace('í', 'i').replace('ï', 'i').replace('ì', 'i')
        .replace('ò', 'o').replace('ó', 'o').replace('ô', 'o').replace('ö', 'o')
        .replace('ú', 'u').replace('ü', 'u').replace('ù', 'u')
        .replace('ç', 'c').replace('ñ', 'n')
        .replace('’', '\'').replace('‘', '\'').replace('`', '\'')
        .replace('–', '-').replace('—', '-').replace(Regex("\\s+"), " ")
}
