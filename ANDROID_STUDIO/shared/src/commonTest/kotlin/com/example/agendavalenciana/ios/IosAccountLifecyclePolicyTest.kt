package com.example.agendavalenciana.ios

import kotlin.test.Test
import kotlin.test.assertFailsWith

class IosAccountLifecyclePolicyTest {
    @Test fun trustedServerBearerAndUserOnly() {
        IosAccountLifecyclePolicy.requireLoginReceipt(123, "artista", "a".repeat(64))
        assertFailsWith<IllegalArgumentException> { IosAccountLifecyclePolicy.requireLoginReceipt(0, "artista", "a".repeat(64)) }
        assertFailsWith<IllegalArgumentException> { IosAccountLifecyclePolicy.requireLoginReceipt(3, "", "a".repeat(64)) }
        assertFailsWith<IllegalArgumentException> { IosAccountLifecyclePolicy.requireLoginReceipt(3, "x", "secret") }
    }
    @Test fun socialCredentialsAreOpaqueAndNeverAcceptedAsBearer() {
        IosAccountLifecyclePolicy.requireSocialCredential("a".repeat(55)+"."+"b".repeat(50)+"."+"c".repeat(64), "nonce_secure_0123456789")
        assertFailsWith<IllegalArgumentException> { IosAccountLifecyclePolicy.requireSocialCredential("a".repeat(150), "nonce_secure_0123456789") }
        assertFailsWith<IllegalArgumentException> { IosAccountLifecyclePolicy.requireSocialCredential("a."+"b".repeat(160)+".c", "invalid nonce space") }
    }
    @Test fun deletionRequiresPositiveReceipt() {
        IosAccountLifecyclePolicy.requireDeleteConfirmation("ELIMINAR", 99)
        IosAccountLifecyclePolicy.requireDeleteReceipt(true, "DELETED")
        IosAccountLifecyclePolicy.requireDeleteReceipt(true, "ANONYMIZED_LEGAL_RETENTION")
        assertFailsWith<IllegalArgumentException> { IosAccountLifecyclePolicy.requireDeleteConfirmation("eliminar", 1) }
        assertFailsWith<IllegalStateException> { IosAccountLifecyclePolicy.requireDeleteReceipt(false, "DELETED") }
        assertFailsWith<IllegalStateException> { IosAccountLifecyclePolicy.requireDeleteReceipt(true, "UNKNOWN") }
    }
}
