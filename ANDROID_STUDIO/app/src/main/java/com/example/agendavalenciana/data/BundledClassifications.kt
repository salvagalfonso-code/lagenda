package com.example.agendavalenciana.data

import android.content.Context
import com.example.agendavalenciana.model.CategoryTaxonomy
import org.json.JSONObject
import java.text.Normalizer
import java.util.Locale

/** Instantània comprovada dels Sheets; només CLASSIFICAT, mai els casos REVISAR. */
internal object BundledClassifications {
    data class Classification(val categoryCode: String, val codes: List<String>, val infantil: Boolean)
    @Volatile private var snapshot: JSONObject? = null
    private val accentMarks = Regex("\\p{M}+")
    private val whitespace = Regex("\\s+")

    private fun read(context: Context): JSONObject? {
        snapshot?.let { return it }
        return synchronized(this) {
            snapshot?.let { return@synchronized it }
            val loaded = runCatching {
                JSONObject(context.assets.open("cultural_classifications_verified.json").bufferedReader().use { it.readText() })
            }.getOrNull()
            if (loaded != null) snapshot = loaded
            loaded
        }
    }

    private fun norm(s: String): String = Normalizer.normalize(s, Normalizer.Form.NFD)
        .replace(accentMarks, "").lowercase(Locale.ROOT)
        .replace(whitespace, " ").trim()

    fun lookup(context: Context, id: Int, uid: String?, title: String): Classification? {
        val catalog = read(context) ?: return null
        val byUid = uid?.trim()?.takeIf(String::isNotEmpty)?.let {
            catalog.optJSONObject("by_uid")?.optJSONObject(it)
        }
        val byId = catalog.optJSONObject("by_id")?.optJSONObject(id.toString())
        // Les IDs es poden reutilitzar entre fonts: cal verificar SEMPRE títol/UID.
        val match = when {
            byUid != null && norm(byUid.optString("title")) == norm(title) -> byUid
            byId != null && norm(byId.optString("title")) == norm(title) &&
                (uid.isNullOrBlank() || byId.optString("uid") == uid) -> byId
            else -> null
        } ?: return null
        val codes = match.optJSONArray("codes") ?: return null
        val valid = (0 until codes.length()).mapNotNull { i ->
            CategoryTaxonomy.canonicalizeCode(codes.optString(i)).takeIf(CategoryTaxonomy::isAnySubcategoryKey)
        }.distinct()
        return valid.takeIf { it.isNotEmpty() }?.let { Classification(match.optString("category"), it, match.optBoolean("infantil")) }
    }
}
