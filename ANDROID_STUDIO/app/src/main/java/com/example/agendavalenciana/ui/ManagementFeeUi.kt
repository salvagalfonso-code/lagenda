package com.example.agendavalenciana.ui

import kotlin.math.roundToLong

/** Import final de compres generals: (((preu + 1,21 €) × 1,015) + 0,25 €). */
internal fun generalPurchaseChargeCents(baseCents: Long): Long {
    if (baseCents <= 0L) return 0L
    return (((baseCents + 121L) * 1.015) + 25.0).roundToLong()
}

internal fun generalPurchaseFeeCents(baseCents: Long): Long =
    (generalPurchaseChargeCents(baseCents) - baseCents).coerceAtLeast(0L)
