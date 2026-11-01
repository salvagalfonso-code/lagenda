package com.example.agendavalenciana.data

import java.text.Normalizer
import java.util.Locale
import kotlin.math.max

/**
 * Identitats conservadores per a no confondre sessions diferents.
 * El mateix key d'artista permet unificar visualment accents, majúscules,
 * espais i puntuació. Les semblances parcials només són candidats de revisió:
 * no es poden convertir en canvis irreversibles d'ID al servidor.
 */
object AgendaDedupe {
    data class EventIdentity(
        val title: String,
        val artists: List<String>,
        val date: String,
        val time: String,
        val town: String,
        val venue: String,
        val festivalOrMultiday: Boolean = false
    )

    fun nameKey(raw: String): String = Normalizer
        .normalize(raw.lowercase(Locale.ROOT).trim(), Normalizer.Form.NFKD)
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")

    fun artistKey(raw: String): String = nameKey(raw)

    private fun ratio(a: String, b: String): Double {
        if (a == b) return 1.0
        if (a.isEmpty() || b.isEmpty()) return 0.0
        if (a.length > 160 || b.length > 160) return 0.0
        var previous = IntArray(b.length + 1) { it }
        for (i in a.indices) {
            val current = IntArray(b.length + 1)
            current[0] = i + 1
            for (j in b.indices) {
                current[j + 1] = minOf(
                    current[j] + 1, previous[j + 1] + 1,
                    previous[j] + if (a[i] == b[j]) 0 else 1
                )
            }
            previous = current
        }
        return 1.0 - previous[b.length].toDouble() / max(a.length, b.length)
    }

    fun nameSimilarity(a: String, b: String): Double {
        val ak = nameKey(a)
        val bk = nameKey(b)
        if (ak == bk && ak.isNotBlank()) return 1.0
        if (ak.isBlank() || bk.isBlank()) return 0.0
        val aWords = ak.split(' ').toSet()
        val bWords = bk.split(' ').toSet()
        val wordScore = aWords.intersect(bWords).size.toDouble() / aWords.union(bWords).size
        return max(ratio(ak, bk), wordScore * 0.95)
    }

    /** Candidats només per a una revisió que comprove imatges, perfils i actuacions. */
    fun similarArtistCandidates(names: List<String>, threshold: Double = 0.84): List<Triple<String, String, Double>> {
        val unique = names.map(String::trim).filter(String::isNotBlank).distinctBy(::artistKey)
        val sorted = unique.sortedBy(::artistKey)
        val candidates = mutableListOf<Triple<String, String, Double>>()
        for (i in sorted.indices) for (j in i + 1 until sorted.size) {
            val a = sorted[i]
            val b = sorted[j]
            // Noms massa curts generen falsos positius: només els compara la identitat exacta.
            if (nameKey(a).length < 6 || nameKey(b).length < 6) continue
            val score = nameSimilarity(a, b)
            if (score >= threshold && artistKey(a) != artistKey(b)) candidates.add(Triple(a, b, score))
        }
        return candidates.sortedByDescending { it.third }
    }

    fun sameEvent(a: EventIdentity, b: EventIdentity): Boolean {
        if (a.date.take(10).isBlank() || a.date.take(10) != b.date.take(10)) return false
        if (nameKey(a.town).isBlank() || nameKey(a.town) != nameKey(b.town)) return false
        if (a.festivalOrMultiday != b.festivalOrMultiday) return false

        val venueA = nameKey(a.venue)
        val venueB = nameKey(b.venue)
        if (venueA.isNotBlank() && venueB.isNotBlank() && venueA != venueB) return false
        val sameVenue = venueA.isNotBlank() && venueA == venueB
        val timeA = a.time.trim().take(5)
        val timeB = b.time.trim().take(5)
        if (timeA.isNotBlank() && timeB.isNotBlank() && timeA != timeB) return false
        val sameTime = timeA.isNotBlank() && timeA == timeB

        val aArtists = a.artists.map(::artistKey).filter(String::isNotBlank).toSet()
        val bArtists = b.artists.map(::artistKey).filter(String::isNotBlank).toSet()
        val overlap = if (aArtists.isEmpty() || bArtists.isEmpty()) 0.0 else
            aArtists.intersect(bArtists).size.toDouble() / aArtists.union(bArtists).size
        val titlesSame = nameKey(a.title).isNotBlank() && nameKey(a.title) == nameKey(b.title)
        val titleSimilarity = nameSimilarity(a.title, b.title)
        // Dos cartells de festival amb el mateix nom/dia/lloc són un sol festival,
        // encara que una font incloga menys artistes que una altra.
        if (a.festivalOrMultiday) return titlesSame && (sameVenue || venueA.isBlank() || venueB.isBlank())
        if (titlesSame) {
            // Mai fusionar dos cartells d'artistes incompatibles només perquè
            // s'anuncien com «Concert» al mateix recinte.
            if (aArtists.isNotEmpty() && bArtists.isNotEmpty()) return overlap >= 0.50
            return sameVenue || (aArtists.isEmpty() && bArtists.isEmpty())
        }
        // El mateix artista/dia no és suficient: podria haver-hi dos passis o recintes.
        if (overlap >= 0.67 && sameVenue && (sameTime || titleSimilarity >= 0.70)) return true
        return overlap >= 0.67 && titleSimilarity >= 0.88 && (sameTime || sameVenue)
    }
}
