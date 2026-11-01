package com.example.agendavalenciana.ios

import kotlinx.serialization.json.JsonObject

/** The server is the sole authority for paid placements and ticket-sale enabled flags.
 * A free reservation via the Agenda itself is still internal; an external ticket URL
 * does not create a global commercial guarantee on its own. */
internal object IosCommercialVisibility {
    fun internalSale(event: AgendaEventSummary): Boolean =
        event.ticketAvailable && event.ticketUrl.isBlank()

    fun protected(event: AgendaEventSummary, paidIds: Set<Int>): Boolean =
        event.id in paidIds || internalSale(event)

    /** Strictly keep server-provided commercial IDs, regardless of affinities or radius.
     * Never create a synthetic event card if the global feed has only an ID and no data.
     */
    fun guaranteed(events: List<AgendaEventSummary>, paidIds: Set<Int>): List<AgendaEventSummary> =
        events.filter { protected(it, paidIds) }.distinctBy { it.key }
}

internal data class IosAdVisual(val focusX: Float, val focusY: Float, val imageUrl: String)

/** Mirrors Android's ads/visual-map.php merge; never turns pending campaigns into active ads. */
internal object IosCommercialVisuals {
    fun decode(root: JsonObject): Map<Int, IosAdVisual> = root.items("visuals").mapNotNull { row ->
        val id = row.text("ad_id").toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
        id to IosAdVisual(
            row.text("card_focus_x").toFloatOrNull()?.coerceIn(0f, 1f) ?: 0.5f,
            row.text("card_focus_y").toFloatOrNull()?.coerceIn(0f, 1f) ?: 0.5f,
            validMediaUrl(row.text("image_url"))
        )
    }.toMap()

    fun apply(ads: List<IosAd>, visuals: Map<Int, IosAdVisual>): List<IosAd> = ads.map { ad ->
        val visual = visuals[ad.id.toIntOrNull()] ?: return@map ad
        ad.copy(imageUrl = visual.imageUrl.ifBlank { ad.imageUrl },
            focusX = visual.focusX, focusY = visual.focusY)
    }
}
