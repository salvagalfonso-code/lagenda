package com.example.agendavalenciana.ios

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IosFeedReadingSessionTest {
    private val now = 1_800_000_000_000L
    private val key = "v14|74|Sense filtre"

    @Test fun batchesAreFiveFifteenTen() {
        assertEquals(20, IosFeedReadingPolicy.page(5, 100))
        assertEquals(30, IosFeedReadingPolicy.page(20, 100))
        assertEquals(40, IosFeedReadingPolicy.page(30, 100))
        assertEquals(12, IosFeedReadingPolicy.page(5, 12))
    }
    @Test fun serverCodecAndIdentityAreStrict() {
        val snapshot = IosFeedReadingPolicy.Snapshot(74, key, 40, 31, 12, now)
        assertEquals(snapshot, IosFeedReadingPolicy.decode(snapshot.encode()))
        assertNull(IosFeedReadingPolicy.eligible(snapshot, 75, key, now + 1000))
        assertNull(IosFeedReadingPolicy.eligible(snapshot, 74, "other", now + 1000))
        assertNull(IosFeedReadingPolicy.decode("bad-json"))
    }
    @Test fun oneHourCountsOutsideNotInsideApp() {
        val session = IosFeedReadingMemory()
        val snapshot = IosFeedReadingPolicy.Snapshot(74, key, 20, 13, 42, now)
        assertTrue(session.importServer(snapshot.encode(), 74, now + 100))
        assertEquals(42, session.restore(74, key, now + 2 * IosFeedReadingPolicy.oneHourMs)?.firstOffset)
        session.markBackground(now + 3 * IosFeedReadingPolicy.oneHourMs)
        assertFalse(session.foreground(now + 3 * IosFeedReadingPolicy.oneHourMs + 30_000))
        assertEquals(42, session.restore(74, key, now + 3 * IosFeedReadingPolicy.oneHourMs + 30_000)?.firstOffset)
        session.markBackground(now + 4 * IosFeedReadingPolicy.oneHourMs)
        assertTrue(session.foreground(now + 5 * IosFeedReadingPolicy.oneHourMs))
        assertNull(session.snapshot())
    }
    @Test fun lateServerResponseCannotRewindFreshScroll() {
        val session = IosFeedReadingMemory()
        val older = IosFeedReadingPolicy.Snapshot(74, key, 20, 13, 2, now)
        session.record(74, key, 35, 28, 68, now + 100)
        assertFalse(session.importServer(older.encode(), 74, now + 200))
        assertEquals(68, session.restore(74, key, now + 300)?.firstOffset)
    }
    @Test fun deepPositionRequiresEnoughRowsAndClamps() {
        val snapshot = IosFeedReadingPolicy.Snapshot(74, key, 5, 82, 0, now)
        assertEquals(88, IosFeedReadingPolicy.restoredLimit(snapshot, 200))
        assertEquals(20, IosFeedReadingPolicy.restoredLimit(snapshot, 20))
        assertEquals(5, IosFeedReadingPolicy.restoredLimit(null, 200))
    }
}
