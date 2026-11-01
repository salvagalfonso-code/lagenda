package com.example.agendavalenciana.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Contract: personal feed exclusions never cancel commercial commitments. */
class IosFeedExclusionPolicyTest {
    @Test fun excludesChosenEvent() = assertFalse(IosFeedExclusionPolicy.visible(
        31, listOf("Grup A"), "Teatre", setOf(31), emptySet(), emptySet(), false))

    @Test fun excludesArtistCaseInsensitively() = assertFalse(IosFeedExclusionPolicy.visible(
        31, listOf("La Maria"), "Música", emptySet(), setOf("la MARIA"), emptySet(), false))

    @Test fun excludesArtistsFromFestivalSchedules() = assertFalse(IosFeedExclusionPolicy.visible(
        31, listOf("Artista dins de l'horari"), "Festival", emptySet(), setOf("artista dins de l'horari"), emptySet(), false))

    @Test fun excludesCategoryCaseInsensitively() = assertFalse(IosFeedExclusionPolicy.visible(
        31, emptyList(), "Dansa", emptySet(), emptySet(), setOf("DANSA"), false))

    @Test fun contractedCampaignCannotBeHidden() = assertTrue(IosFeedExclusionPolicy.visible(
        31, listOf("Grup A"), "Música", setOf(31), setOf("Grup A"), setOf("Música"), true))

    @Test fun noFilterMeansVisible() = assertTrue(IosFeedExclusionPolicy.visible(
        31, listOf("Grup A"), "Música", emptySet(), emptySet(), emptySet(), false))
}
