package com.example.agendavalenciana.ios

import com.example.agendavalenciana.model.DiscoveryLevel

/** 30 minute ordered feed snapshot, account and explicit-taste scoped. */
object IosForYouSnapshot {
    private const val TTL = 30L * 60L * 1000L
    fun select(events: List<AgendaEventSummary>, userId: Int?, user: IosUserState,
        categories: Set<String>, towns: Set<String>, paid: IosPromoted,
        subcategories: Set<String> = emptySet(), comarques: Set<String> = emptySet(),
        radii: Map<String,Int> = emptyMap(), suggestions: Boolean = false,
        discovery: DiscoveryLevel = DiscoveryLevel.BALANCED,
        valencianFirst: Boolean = true,
        entityStyles: Map<String, Set<String>> = emptyMap()): List<AgendaEventSummary> {
        val settings = IosPersonalSettings(categories,subcategories,towns,comarques,radii,suggestions,discovery,valencianFirst)
        val signature = buildString {
            append(userId ?: "guest"); append('|')
            // A new event or changed date/title must not be silently lost.
            append(events.size); append(':'); events.forEach { append(it.key); append('_'); append(it.updatedAt); append('_'); append(it.category); append(';') }
            append('|'); user.follows.sortedWith(compareBy({ it.type }, { it.key })).forEach { append(it.type); append(it.key); append(';') }
            append('|'); categories.sorted().forEach { append(it); append(';') }
            append('|'); subcategories.sorted().forEach { append(it); append(';') }
            append('|'); towns.sorted().forEach { append(it); append(';') }
            append('|'); comarques.sorted().forEach { append(it); append(';') }
            append('|')
            // Evitar la desestructuracio (k, v): Kotlin/Native no pot inferir component2() aci.
            for (townName: String in radii.keys.sorted()) {
                val radiusKm: Int = radii[townName] ?: continue
                append(townName)
                append(':')
                append(radiusKm)
                append(';')
            }
            append('|'); append(suggestions); append(':'); append(discovery.name); append(':'); append(valencianFirst)
            append('|')
            // Bucle explicit: evita inferencia/desestructuracio amb lambdes imbricades en Kotlin/Native.
            for (name in entityStyles.keys.sorted()) {
                append(name)
                append('=')
                for (code in entityStyles[name].orEmpty().sorted()) {
                    append(code)
                    append(',')
                }
                append(';')
            }
            append('|'); paid.guaranteedEventIds.sorted().forEach { append(it); append(';') }
            append('|'); paid.ads.forEach { append(it.id); append(';') }
        }
        val prefix = "for_you_${userId ?: "guest"}_"
        val storedSignature = platformReadPublicCache(prefix + "signature")
        val storedAt = platformReadPublicCache(prefix + "at").toLongOrNull() ?: 0L
        val age = currentEpochMillis() - storedAt
        if (signature == storedSignature && age in 0 until TTL) {
            val rawIds = platformReadPublicCache(prefix + "ordered_ids")
            // Persist a deliberately empty selection too; don't rebuild it on every launch.
            if (rawIds == "~") return emptyList()
            val ids = rawIds.split(',').filter(String::isNotBlank)
            val lookup = events.associateBy { it.key }
            val restored = ids.mapNotNull(lookup::get)
            if (restored.size == ids.size && ids.isNotEmpty()) return restored
        }
        val priorityIds = paid.guaranteedEventIds + paid.ads.map { it.eventId }
        val inputs = events.map { event ->
            IosPersonalEvent(event.id, event.key, event.date, event.time, event.artists,
                event.schedule.map { it.artist },event.organizers,event.venue,event.category,
                event.styles,event.town,event.language,event.ticketAvailable)
        }
        val selectedKeys = IosPersonalFeedPolicy.select(inputs,user.follows,settings,priorityIds,
            if (suggestions) IosRecentInterest.current(userId) else emptySet(),
            artistMetadata = entityStyles,
            recentStyleWeights = if (suggestions) IosRecentInterest.weighted(userId) else emptyMap())
        val lookup = events.associateBy { it.key }
        val result = selectedKeys.mapNotNull(lookup::get)
        platformWritePublicCache(prefix + "signature", signature)
        platformWritePublicCache(prefix + "at", currentEpochMillis().toString())
        platformWritePublicCache(prefix + "ordered_ids", if (result.isEmpty()) "~" else result.joinToString(",") { it.key })
        return result
    }
}
