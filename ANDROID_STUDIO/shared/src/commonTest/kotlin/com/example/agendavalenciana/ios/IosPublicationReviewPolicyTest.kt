package com.example.agendavalenciana.ios

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class IosPublicationReviewPolicyTest {
    @Test fun publishedIsNotProofOfReviewQueue() {
        val receipt = IosPublicationReviewPolicy.fromServer(14, "published", "", null)
        assertTrue(receipt.published)
        assertFalse(receipt.reviewConfirmed)
        assertTrue("no s'ha confirmat" in receipt.message)
    }
    @Test fun explicitQueueReceiptConfirmsReview() {
        val receipt = IosPublicationReviewPolicy.fromServer(14, "PUBLISHED", "queued", null)
        assertTrue(receipt.published && receipt.reviewConfirmed)
    }
    @Test fun explicitFalseBeatsAmbiguousReviewStatus() {
        assertFalse(IosPublicationReviewPolicy.fromServer(14, "published", "queued", false).reviewConfirmed)
    }
    @Test fun pendingNeverLooksPublished() {
        val result = IosPublicationReviewPolicy.fromServer(32, "PENDING_REVIEW", "pending", null)
        assertFalse(result.published)
        assertEquals("pending_review", result.status)
        assertTrue("Encara no és públic" in result.message)
    }
    @Test fun missingStatusIsNotPublished() {
        assertFalse(IosPublicationReviewPolicy.fromServer(32, null, null, null).published)
    }
    @Test fun invalidResponseCannotBeSuccess() {
        assertFailsWith<IllegalArgumentException> { IosPublicationReviewPolicy.fromServer(0,"published",null,null) }
        assertFailsWith<IllegalArgumentException> { IosPublicationReviewPolicy.fromServer(5,"rejected",null,null) }
    }
}
