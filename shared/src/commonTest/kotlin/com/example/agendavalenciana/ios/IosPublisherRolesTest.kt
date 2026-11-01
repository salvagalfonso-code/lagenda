package com.example.agendavalenciana.ios

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class IosPublisherRolesTest {
    @Test fun guestAndUserCanPublishWithoutProfessionalRole() {
        val guest = IosPublisherSnapshot(1, "GUEST", serverConfirmed = true)
        assertTrue(IosPublishAction.NEW_EVENT in IosPublisherRoles.visibleActions(false, guest))
        assertFalse(IosPublishAction.TICKETS in IosPublisherRoles.visibleActions(false, guest))
        assertFalse(IosPublisherRoles.canPublishDirectly(guest))
    }
    @Test fun professionalRequiresServerConfirmationAndDoesNotAutoPublish() {
        val role = IosPublisherSnapshot(2,"PROMOTER",professionalVerified=true,serverConfirmed=true)
        assertTrue(IosPublisherRoles.isProfessional(role))
        assertFalse(IosPublisherRoles.canPublishDirectly(role))
        assertFalse(IosPublisherRoles.isProfessional(role.copy(serverConfirmed=false)))
        assertFalse(IosPublisherRoles.canPublishDirectly(role.copy(canAutoPublish=true,suspended=true)))
    }
    @Test fun ownPublishedEventsUnlockEditingAndAds() {
        val role = IosPublisherSnapshot(3,"USER",ownedPublishedIds=setOf(45),serverConfirmed=true)
        assertTrue(IosPublishAction.EDIT_EVENTS in IosPublisherRoles.visibleActions(true,role))
        assertTrue(IosPublishAction.CREATE_AD in IosPublisherRoles.visibleActions(true,role))
        assertFalse(IosPublishAction.TICKETS in IosPublisherRoles.visibleActions(true,role))
    }
    @Test fun superAdminCanReviewButGuestCannot() {
        val admin = IosPublisherSnapshot(4,"SUPER_ADMIN",serverConfirmed=true)
        assertTrue(IosPublishAction.REVIEW_PUBLICATIONS in IosPublisherRoles.visibleActions(true,admin))
        assertFalse(IosPublishAction.REVIEW_PUBLICATIONS in IosPublisherRoles.visibleActions(false,admin))
    }
    @Test fun publicationDataValidateCategoryAndTickets() {
        assertFailsWith<IllegalArgumentException> {
            IosPublishDraft("Concert","2026-11-03","19:00","Sagunt",category="MUSIC").validate()
        }
        IosPublishDraft("Teatre","2026-11-03","19:00","Sagunt",category="THEATRE").validate()
    }
    @Test fun onboardingRequiresOwnEventAndLegalConsents() {
        assertFailsWith<IllegalArgumentException> { IosSellerActivation(3).validate() }
        IosSellerActivation(3,existingCompanyId=5,acceptedPrivacy=true,acceptedSales=true).validate()
    }
}
