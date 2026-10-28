package com.example.agendavalenciana.model

/**
 * v304 — Qualsevol compte no suspés pot publicar directament; professional és un permís distint.
 * La decisió real i definitiva també s'ha de validar a events/create.php.
 */
object EventPublishingPolicy {
    const val REQUIRED_APPROVALS = 0

    fun canPublishDirectly(
        superAdmin: Boolean,
        signedInUserId: Int?,
        trustOwnerUserId: Int?,
        approvedCount: Int,
        autoPublishEnabled: Boolean,
        suspendedByReport: Boolean
    ): Boolean {
        if (superAdmin) return true
        return signedInUserId != null && signedInUserId == trustOwnerUserId &&
            autoPublishEnabled && !suspendedByReport
    }
}
