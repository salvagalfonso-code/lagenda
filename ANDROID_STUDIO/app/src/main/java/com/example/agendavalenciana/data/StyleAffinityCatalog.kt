package com.example.agendavalenciana.data

import android.content.Context
import org.json.JSONObject
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Diccionari intern d'estils de l'Agenda.
 *
 * Regles:
 * - Mai canvia el text original que es mostra a l'usuari.
 * - Les equivalències compten com coincidència exacta.
 * - Les proximitats només s'apliquen després de resoldre equivalències.
 * - prox_1 és més pròxim que prox_12.
 */
object StyleAffinityCatalog {
    private const val ASSET = "style_affinity_catalog_v3.json"

    private data class Catalog(
        val aliases: Map<String, String>,
        val proximity: Map<String, List<String>>,
        val families: Map<String, String>
    )

    @Volatile private var cached: Catalog? = null
    private val combiningMarksRegex = Regex("\\p{Mn}+")
    private val whitespaceRegex = Regex("\\s+")
    private val fallbackCharsRegex = Regex("[^a-z0-9]+")
    private val canonicalCache = ConcurrentHashMap<String, String>()

    private fun data(context: Context): Catalog = cached ?: synchronized(this) {
        cached ?: run {
            val raw = context.assets.open(ASSET).bufferedReader(Charsets.UTF_8).use { it.readText() }
            val root = JSONObject(raw)
            val aliasesJson = root.optJSONObject("aliases") ?: JSONObject()
            val proximityJson = root.optJSONObject("proximity") ?: JSONObject()
            val familiesJson = root.optJSONObject("families") ?: JSONObject()

            val aliases = linkedMapOf<String, String>()
            aliasesJson.keys().forEach { key ->
                val value = aliasesJson.optString(key).trim()
                if (key.isNotBlank() && value.isNotBlank()) aliases[key] = value
            }

            val proximity = linkedMapOf<String, List<String>>()
            proximityJson.keys().forEach { source ->
                val array = proximityJson.optJSONArray(source)
                if (array != null) {
                    proximity[source] = buildList {
                        for (i in 0 until array.length()) {
                            array.optString(i).trim().takeIf(String::isNotBlank)?.let(::add)
                        }
                    }.distinct()
                }
            }

            val families = linkedMapOf<String, String>()
            familiesJson.keys().forEach { key ->
                familiesJson.optString(key).trim().takeIf(String::isNotBlank)?.let { families[key] = it }
            }
            Catalog(aliases, proximity, families).also { cached = it }
        }
    }

    private fun lookupKey(raw: String): String {
        val decomposed = Normalizer.normalize(raw.trim().replace('’', '\''), Normalizer.Form.NFD)
            .replace(combiningMarksRegex, "")
            .lowercase(Locale.ROOT)
        return decomposed.replace(whitespaceRegex, " ").trim()
    }

    private fun fallbackKeyFromLookup(lookup: String): String = lookup
        .replace(fallbackCharsRegex, "_")
        .trim('_')

    fun canonicalKey(context: Context, raw: String): String {
        if (raw.isBlank()) return ""
        canonicalCache[raw]?.let { return it }
        val catalog = data(context)
        val lookup = lookupKey(raw)
        val canonical = catalog.aliases[lookup] ?: fallbackKeyFromLookup(lookup)
        if (canonicalCache.size < 4_000) canonicalCache.putIfAbsent(raw, canonical)
        return canonical
    }

    fun canonicalKeys(context: Context, rawStyles: Iterable<String>): Set<String> = rawStyles
        .asSequence()
        .map { canonicalKey(context, it) }
        .filter(String::isNotBlank)
        .toSet()

    /** 0 = equivalència exacta; 1..12 = proximitat; null = sense relació coneguda. */
    fun proximityRank(context: Context, sourceRawOrKey: String, candidateRawOrKey: String): Int? {
        val catalog = data(context)
        val source = if (sourceRawOrKey in catalog.proximity || sourceRawOrKey in catalog.families) {
            sourceRawOrKey
        } else canonicalKey(context, sourceRawOrKey)
        val candidate = if (candidateRawOrKey in catalog.proximity || candidateRawOrKey in catalog.families) {
            candidateRawOrKey
        } else canonicalKey(context, candidateRawOrKey)
        if (source.isBlank() || candidate.isBlank()) return null
        if (source == candidate) return 0
        val index = catalog.proximity[source]?.indexOf(candidate) ?: -1
        return if (index >= 0) index + 1 else null
    }

    /** 1.0 per equivalència; descens lineal fins a 1/13 en prox_12. */
    fun proximityFactor(context: Context, sourceKey: String, candidateKey: String): Double {
        val rank = proximityRank(context, sourceKey, candidateKey) ?: return 0.0
        return if (rank == 0) 1.0 else (13.0 - rank.toDouble()) / 13.0
    }

    fun bestProximityRank(context: Context, sourceKeys: Set<String>, candidateKeys: Set<String>): Int? {
        if (sourceKeys.isEmpty() || candidateKeys.isEmpty()) return null
        var best: Int? = null
        for (source in sourceKeys) {
            for (candidate in candidateKeys) {
                val rank = proximityRank(context, source, candidate) ?: continue
                if (best == null || rank < best!!) best = rank
                if (best == 0) return 0
            }
        }
        return best
    }

    fun weightedAffinity(context: Context, sourceWeights: Map<String, Double>, candidateKeys: Set<String>): Double {
        if (sourceWeights.isEmpty() || candidateKeys.isEmpty()) return 0.0
        var total = 0.0
        for ((source, weight) in sourceWeights) {
            if (weight <= 0.0) continue
            var best = 0.0
            for (candidate in candidateKeys) {
                best = maxOf(best, proximityFactor(context, source, candidate))
                if (best >= 1.0) break
            }
            total += weight * best
        }
        return total
    }

    fun family(context: Context, rawOrKey: String): String? {
        val catalog = data(context)
        val key = if (rawOrKey in catalog.families) rawOrKey else canonicalKey(context, rawOrKey)
        return catalog.families[key]
    }
}
