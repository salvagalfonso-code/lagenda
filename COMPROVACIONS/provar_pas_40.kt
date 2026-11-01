package com.example.agendavalenciana.ios

fun main() {
    var count = 0
    fun ok(yes: Boolean, message: String) { check(yes) { message }; count++ }
    val states = listOf("published", "PUBLISHED", "pending", "pending_review", "draft", "", "unknown")
    for (status in states) {
        val r = IosPublicationReviewPolicy.fromServer(24, status, null, null)
        ok(r.eventId == 24, "id")
        ok(r.published == status.equals("published", true), "publication status $status")
        ok(!r.reviewConfirmed, "must not infer review from event status $status")
        ok(r.message.isNotBlank(), "UX status $status")
    }
    val reviewed = IosPublicationReviewPolicy.fromServer(25, "published", "queued", null)
    ok(reviewed.reviewConfirmed, "explicit queue")
    ok(reviewed.message.contains("confirmat l'enviament"), "queue UX")
    ok(!IosPublicationReviewPolicy.fromServer(25, "published", "queued", false).reviewConfirmed, "false overrides queued")
    ok(IosPublicationReviewPolicy.fromServer(25, "pending_review", null, true).reviewConfirmed, "explicit flag")
    ok(!IosPublicationReviewPolicy.fromServer(25, "pending_review", null, true).published, "queued not published")
    ok(runCatching { IosPublicationReviewPolicy.fromServer(0, "published", null, null) }.isFailure, "invalid id")
    ok(runCatching { IosPublicationReviewPolicy.fromServer(25, "rejected", null, null) }.isFailure, "rejected")
    ok(!IosPublicationReviewPolicy.fromServer(25, null, null, null).published, "missing status cannot claim published")
    println("PAS40_KOTLIN_PASS $count asserts")
}
