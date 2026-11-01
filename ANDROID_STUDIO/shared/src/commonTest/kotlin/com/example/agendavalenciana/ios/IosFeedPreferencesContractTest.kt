package com.example.agendavalenciana.ios

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Pas 9: contracte dels codis d'Android v336, independent del canal de dades. */
class IosFeedPreferencesContractTest {
    @Test fun languageCodesAreByteCompatibleWithAndroidUiLanguageStore() {
        assertEquals(listOf("ca", "es", "en"), IosUiLanguage.entries.map { it.code })
        assertEquals(IosUiLanguage.VALENCIAN, IosUiLanguage.fromServer("ca"))
        assertEquals(IosUiLanguage.SPANISH, IosUiLanguage.fromServer("es"))
        assertEquals(IosUiLanguage.ENGLISH, IosUiLanguage.fromServer("en"))
        assertNull(IosUiLanguage.fromServer("fr"))
        assertNull(IosUiLanguage.fromServer(null))
    }

    @Test fun noConfusionBetweenInterfaceLanguageAndCulturalPreference() {
        assertEquals("selected_language", IosFeedPreferencesContract.centralUiLanguageKey)
        assertEquals("valencian_first", IosFeedPreferencesContract.centralValencianFirstKey)
        assertTrue(IosFeedPreferencesContract.centralUiLanguageKey !=
            IosFeedPreferencesContract.centralValencianFirstKey)
    }

    @Test fun discoveryLevelUsesExactAndroidAndServerCodes() {
        assertTrue(listOf("MY_TASTES", "BALANCED", "EXPLORE").all(
            IosFeedPreferencesContract::validDiscovery))
        assertFalse(IosFeedPreferencesContract.validDiscovery("OTHER"))
    }

    @Test fun discoveryIsHiddenButChoiceIsNotDestroyedWhenSuggestionsOff() {
        assertFalse(IosFeedPreferencesContract.displayDiscovery(false))
        assertTrue(IosFeedPreferencesContract.displayDiscovery(true))
    }
}
