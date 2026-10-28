package com.example.agendavalenciana.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class IosFestivalSavedSlotPolicyTest {
    @Test fun eachFestivalSessionHasIndependentIdentity() {
        val morning = IosFestivalSavedSlotPolicy.key(15, "2027-07-12", "11:00", "Artista", "Concert")
        val evening = IosFestivalSavedSlotPolicy.key(15, "2027-07-12", "20:00", "Artista", "Concert")
        val anotherFestival = IosFestivalSavedSlotPolicy.key(16, "2027-07-12", "11:00", "Artista", "Concert")
        assertNotEquals(morning, evening)
        assertNotEquals(morning, anotherFestival)
        assertEquals(morning, IosFestivalSavedSlotPolicy.key(15, "2027-07-12", "11:00", "artista", "CONCERT"))
    }
    @Test fun saveAndRestoreWithoutCollidingOnPunctuation() {
        val a = IosFestivalSavedSlotPolicy.key(2, "2027-01-01", "12:30", "A|B", "X:Y")
        val b = IosFestivalSavedSlotPolicy.key(2, "2027-01-01", "12:30", "A", "B|X:Y")
        assertNotEquals(a, b)
        val restored = IosFestivalSavedSlotPolicy.decode(IosFestivalSavedSlotPolicy.encode(setOf(a, b)))
        assertTrue(a in restored)
        assertTrue(b in restored)
        assertFalse("fake" in restored)
        assertEquals(emptySet(), IosFestivalSavedSlotPolicy.decode("corrupt"))
    }
}
