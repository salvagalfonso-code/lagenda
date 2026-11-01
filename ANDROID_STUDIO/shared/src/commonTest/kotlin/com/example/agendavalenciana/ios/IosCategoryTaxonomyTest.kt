package com.example.agendavalenciana.ios

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IosCategoryTaxonomyTest {
    @Test fun sameCodesAsAndroidAndLabelsAreNotStorageKeys() {
        assertEquals(12, IosCategoryTaxonomy.categories.size)
        assertEquals(55, IosCategoryTaxonomy.subcategories.size)
        assertEquals("MUSIC", IosCategoryTaxonomy.mainKey("Música"))
        assertEquals("MUSIC__JAZZ_BLUES_SOUL", IosCategoryTaxonomy.subcategoryCode("MUSIC__SUB_4"))
    }
    @Test fun restoresKeysRatherThanLabelsFromRegisteredOrGuestFollows() {
        val server = listOf(
            IosFollow("CATEGORY", "THEATRE", "Teatre"),
            IosFollow("CATEGORY", "MUSIC__JAZZ_BLUES_SOUL", "Jazz, blues i soul"),
            IosFollow("ARTIST", "Pep", "Pep")
        )
        assertEquals(IosCategorySelection(setOf("THEATRE"), setOf("MUSIC__JAZZ_BLUES_SOUL")),
            IosCategoryTaxonomy.fromServer(server))
    }
    @Test fun wholeCategoryAndSubcategoryExpandExactlyLikeAndroid() {
        val music = IosCategoryTaxonomy.toggleCategory(IosCategorySelection(emptySet(), emptySet()), "MUSIC")
        assertEquals(setOf("MUSIC"), music.categories)
        val afterRemoveJazz = IosCategoryTaxonomy.toggleSubcategory(music,"MUSIC__JAZZ_BLUES_SOUL")
        assertTrue("MUSIC" !in afterRemoveJazz.categories)
        assertEquals(4, afterRemoveJazz.subcategories.size)
        val again = IosCategoryTaxonomy.toggleSubcategory(afterRemoveJazz,"MUSIC__JAZZ_BLUES_SOUL")
        assertEquals(setOf("MUSIC"),again.categories)
        assertTrue(again.subcategories.isEmpty())
    }
    @Test fun diffPreservesUnrelatedAndCleansLegacyKeys() {
        val actual = listOf(IosFollow("CATEGORY","MUSIC__SUB_4","Jazz"),
            IosFollow("CATEGORY","THEATRE","Teatre"), IosFollow("ARTIST","La Maria","La Maria"),
            IosFollow("CATEGORY", "EXTERNAL_NON_TAXONOMY", "Extra"))
        val expected = IosCategoryTaxonomy.mutations(actual,
            IosCategorySelection(setOf("THEATRE"),setOf("MUSIC__JAZZ_BLUES_SOUL")))
        assertEquals(2,expected.size)
        assertTrue(expected.any { it.key == "MUSIC__SUB_4" && !it.following })
        assertTrue(expected.any { it.key == "MUSIC__JAZZ_BLUES_SOUL" && it.following })
        assertFalse(expected.any { it.key == "EXTERNAL_NON_TAXONOMY" })
    }
    @Test fun exactSubcategoryMatchDoesNotBroadenToUnrelatedGenre() {
        val jazz = IosCategorySelection(emptySet(),setOf("MUSIC__JAZZ_BLUES_SOUL"))
        assertTrue(IosCategoryTaxonomy.matchesEvent("Música",listOf("Jazz"),jazz))
        assertTrue(IosCategoryTaxonomy.matchesEvent("MUSIC",listOf("MUSIC__JAZZ_BLUES_SOUL"),jazz))
        assertFalse(IosCategoryTaxonomy.matchesEvent("Música",listOf("Punk"),jazz))
        assertFalse(IosCategoryTaxonomy.matchesEvent("Teatre",listOf("Jazz"),jazz))
        assertTrue(IosCategoryTaxonomy.matchesEvent("Teatre", listOf("Familiar"),
            IosCategorySelection(setOf("CHILDREN"), emptySet())))
    }
}
