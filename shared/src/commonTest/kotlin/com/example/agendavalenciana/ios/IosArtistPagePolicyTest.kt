package com.example.agendavalenciana.ios

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IosArtistPagePolicyTest {
    @Test fun matchesExactArtistWithoutMatchingOthers() {
        assertTrue(IosArtistPagePolicy.sameArtist(" La Maria ", "la maria"))
        assertFalse(IosArtistPagePolicy.sameArtist("La Maria", "La Marina"))
        assertFalse(IosArtistPagePolicy.sameArtist("", ""))
    }
    @Test fun acceptsOnlySecureProfileImages() {
        assertEquals("https://site.example/artist.jpg", IosArtistPagePolicy.imageUrl("https://site.example/artist.jpg"))
        assertEquals("/uploads/artist.jpg", IosArtistPagePolicy.imageUrl("/uploads/artist.jpg"))
        assertEquals("", IosArtistPagePolicy.imageUrl("http://site.example/artist.jpg"))
        assertEquals("", IosArtistPagePolicy.imageUrl("//site.example/artist.jpg"))
        assertEquals("", IosArtistPagePolicy.imageUrl("/../private.jpg"))
    }
    @Test fun moneyAndStockDoNotInventPrices() {
        assertEquals("12,35 €", IosArtistPagePolicy.money(1235))
        assertEquals("0,00 €", IosArtistPagePolicy.money(-10))
        assertTrue(IosArtistPagePolicy.inStock(null))
        assertFalse(IosArtistPagePolicy.inStock(0))
    }
    @Test fun disciplinePreservesMultidisciplinaryArtists() {
        assertTrue(IosArtistPagePolicy.isMusic("Música"))
        assertFalse(IosArtistPagePolicy.isMusic("Teatre"))
        assertEquals("Espectacles", IosArtistPagePolicy.activityHeading("Teatre"))
        assertEquals("Concerts", IosArtistPagePolicy.activityHeading("Música"))
    }
}
