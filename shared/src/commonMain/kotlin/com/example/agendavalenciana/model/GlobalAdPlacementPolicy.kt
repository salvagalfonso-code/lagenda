package com.example.agendavalenciana.model

/**
 * v255 · Dos espais publicitaris GLOBALS per cada 20 posicions VISUALS:
 * Premium a la posició 6; Normal a la 15 (comptades des d'1).
 *
 * Si un tipus de campanya no està actiu, aquell espai queda orgànic, sense
 * desplaçar l'altre tipus de la posició contractada. Per això el bloc conté
 * 18, 19 o 20 elements orgànics segons quants tipus d'anunci hi haja.
 *
 * No es calculen els espais amb la mida del lot paginat. En un últim bloc
 * incomplet, els anuncis pendents es mostren després de l'última card real,
 * mai davant de la primera card com ocorria abans de v255.
 */
object GlobalAdPlacementPolicy {
    private const val VISUAL_PER_BLOCK = 20
    private const val PREMIUM_AFTER_ORGANIC = 5
    private const val NORMAL_VISUAL_POSITION = 15

    fun organicPerBlock(premiumAvailable: Boolean, normalAvailable: Boolean): Int =
        VISUAL_PER_BLOCK - (if (premiumAvailable) 1 else 0) - (if (normalAvailable) 1 else 0)

    private fun normalAfterOrganic(premiumAvailable: Boolean): Int =
        NORMAL_VISUAL_POSITION - 1 - (if (premiumAvailable) 1 else 0)

    fun premiumAt(organicIndex: Int, premiumAvailable: Boolean, normalAvailable: Boolean): Boolean =
        premiumAvailable && organicIndex >= 0 &&
            organicIndex % organicPerBlock(premiumAvailable, normalAvailable) == PREMIUM_AFTER_ORGANIC

    fun normalAt(organicIndex: Int, premiumAvailable: Boolean, normalAvailable: Boolean): Boolean =
        normalAvailable && organicIndex >= 0 &&
            organicIndex % organicPerBlock(premiumAvailable, normalAvailable) == normalAfterOrganic(premiumAvailable)

    // Només s'han de consultar si la pàgina del feed JA està carregada completa.
    fun premiumAfterLast(organicCount: Int, premiumAvailable: Boolean, normalAvailable: Boolean): Boolean =
        premiumAvailable && (organicCount == 0 ||
            (organicCount > 0 && organicCount % organicPerBlock(premiumAvailable, normalAvailable)
                in 1..PREMIUM_AFTER_ORGANIC))

    fun normalAfterLast(organicCount: Int, premiumAvailable: Boolean, normalAvailable: Boolean): Boolean =
        normalAvailable && (organicCount == 0 ||
            (organicCount > 0 && organicCount % organicPerBlock(premiumAvailable, normalAvailable)
                in 1..normalAfterOrganic(premiumAvailable)))
}
