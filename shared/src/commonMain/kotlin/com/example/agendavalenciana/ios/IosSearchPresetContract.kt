package com.example.agendavalenciana.ios

import kotlinx.serialization.json.*

/** Android v336 Filters / PHP preferences/search-filters.php wire contract.
 * Style is an on-screen transient filter: PHP does not accept it and MUST NOT be silently lost.
 * Never save presets to UserDefaults or into a parallel sync/state key.
 */
internal object IosSearchPresetContract {
    private fun string(json: JsonObject, key: String): String =
        (json[key] as? JsonPrimitive)?.contentOrNull.orEmpty().trim()

    fun isIsoMonth(value: String): Boolean =
        Regex("[0-9]{4}-(0[1-9]|1[0-2])").matches(value)

    fun isIsoDate(value: String): Boolean {
        if (!Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(value)) return false
        val year = value.substring(0, 4).toIntOrNull() ?: return false
        val month = value.substring(5, 7).toIntOrNull() ?: return false
        val day = value.substring(8, 10).toIntOrNull() ?: return false
        if (year !in 1..9999 || month !in 1..12) return false
        val leap = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
        val limit = when (month) { 2 -> if (leap) 29 else 28; 4,6,9,11 -> 30; else -> 31 }
        return day in 1..limit
    }

    fun priceEuros(raw: String): Double? {
        val s = raw.trim().lowercase()
        if (s in setOf("gratuït", "gratuit", "gratis", "free", "entrada gratuïta", "entrada gratuita")) return 0.0
        if (s.isEmpty()) return null // unknown is not free
        val numeric = Regex("[0-9]+(?:[.,][0-9]{1,2})?").find(s)?.value ?: return null
        return numeric.replace(',', '.').toDoubleOrNull()
    }

    fun canonicalCategories(filters: IosSearchFilters): List<String> =
        (filters.categoryCodes + listOfNotNull(IosCategoryTaxonomy.mainKey(filters.category)))
            .mapNotNull(IosCategoryTaxonomy::mainKey).distinct().sorted()

    fun toServer(filters: IosSearchFilters): JsonObject {
        require(filters.valid()) { "Revisa les dates, el radi o el preu." }
        require(filters.style.isBlank()) { "El servidor d'Android no admet guardar el filtre d'estil. Lleva'l per guardar esta cerca." }
        val categories = canonicalCategories(filters)
        val result = buildJsonObject {
            put("categories", JsonArray(categories.map(::JsonPrimitive)))
            filters.fromDate.takeIf(String::isNotBlank)?.let { put("dateFrom", it) }
            filters.toDate.takeIf(String::isNotBlank)?.let { put("dateTo", it) }
            filters.month.takeIf(String::isNotBlank)?.let { put("month", it) }
            filters.comarca.trim().takeIf(String::isNotBlank)?.let { put("comarca", it) }
            filters.town.trim().takeIf(String::isNotBlank)?.let { put("town", it) }
            if (filters.radiusKm > 0) put("maxDistanceKm", filters.radiusKm)
            filters.maxPrice?.let { put("maxPrice", it) }
            put("freeOnly", filters.freeOnly)
        }
        require(categories.isNotEmpty() || filters.fromDate.isNotBlank() || filters.toDate.isNotBlank() ||
            filters.month.isNotBlank() || filters.comarca.isNotBlank() || filters.town.isNotBlank() ||
            filters.radiusKm > 0 || filters.maxPrice != null || filters.freeOnly) {
            "Tria almenys un filtre per guardar."
        }
        require(filters.town.length <= 120 && filters.comarca.length <= 120)
        return result
    }

    fun fromServer(json: JsonObject): IosSearchFilters {
        val categories = (json["categories"] as? JsonArray).orEmpty().mapNotNull { element ->
            (element as? JsonPrimitive)?.contentOrNull?.let(IosCategoryTaxonomy::mainKey)
        }.toSet()
        val distance = (json["maxDistanceKm"] as? JsonPrimitive)?.intOrNull ?: 0
        val maxPrice = (json["maxPrice"] as? JsonPrimitive)?.intOrNull
        val preset = IosSearchFilters(
            town = string(json, "town"), comarca = string(json, "comarca"), radiusKm = distance,
            fromDate = string(json, "dateFrom"), toDate = string(json, "dateTo"),
            categoryCodes = categories, month = string(json, "month"), maxPrice = maxPrice,
            freeOnly = (json["freeOnly"] as? JsonPrimitive)?.booleanOrNull == true
        )
        require(preset.valid()) { "El servidor ha retornat filtres incompatibles." }
        return preset
    }

    fun same(a: IosSearchFilters, b: IosSearchFilters): Boolean =
        toServer(a) == toServer(b)
}

internal data class IosSavedSearch(val name: String, val filters: IosSearchFilters)
