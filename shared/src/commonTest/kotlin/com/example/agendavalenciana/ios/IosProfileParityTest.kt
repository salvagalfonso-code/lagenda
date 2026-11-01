package com.example.agendavalenciana.ios

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class IosProfileParityTest {
    @Test fun registeredIdentityValidation() {
        assertEquals(Triple("test_usuari", "a@example.org", "Vila-real"),
            IosProfileParity.validateIdentity(" test_usuari ", " a@example.org ", " Vila-real "))
        for (username in listOf("xy", "bad user", ""))
            assertFailsWith<IllegalArgumentException> { IosProfileParity.validateIdentity(username, "a@example.org", "") }
        for (email in listOf("broken", "a@", "a @example.org", "a@@example.org"))
            assertFailsWith<IllegalArgumentException> { IosProfileParity.validateIdentity("tester", email, "") }
        assertFailsWith<IllegalArgumentException> { IosProfileParity.validateIdentity("tester", "a@example.org", "A".repeat(191)) }
    }
    @Test fun profileFieldsArePreserved() {
        val before = IosMyProfile("Grup", "bio", "https://example.org/avatar.jpg", "Sagunt",
            "Música", "Companyia", "Intèrprets", "Editorial", "Llibres", "Obres", "Sales")
        val desired = before.copy(displayName = "Grup renovat")
        assertTrue(IosProfileParity.sameProfessionalFields(before, desired))
        assertFalse(IosProfileParity.sameProfessionalFields(before, before.copy(spacesText = "Canviat")))
        assertFalse(IosProfileParity.sameProfessionalFields(before, before.copy(discipline = "Teatre")))
        assertEquals("Grup", IosProfileParity.validateProfile(" Grup ", " bio ").first)
        assertFailsWith<IllegalArgumentException> { IosProfileParity.validateProfile("", "") }
        assertFailsWith<IllegalArgumentException> { IosProfileParity.validateProfile("a", "x".repeat(3001)) }
        assertTrue(IosProfileParity.sameUsername("Usuari", "  usuari "))
        assertFalse(IosProfileParity.sameUsername("", "usuari"))
    }
}
