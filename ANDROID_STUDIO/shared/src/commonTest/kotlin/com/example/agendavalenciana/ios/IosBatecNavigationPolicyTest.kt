package com.example.agendavalenciana.ios

import kotlin.test.Test
import kotlin.test.assertEquals

class IosBatecNavigationPolicyTest {
    @Test fun firstBackReplaysInsteadOfClosing() {
        assertEquals(IosBatecNavigationPolicy.Navigation.Restart, IosBatecNavigationPolicy.navigate(0, 6, -1))
    }
    @Test fun lastNextCloses() {
        assertEquals(IosBatecNavigationPolicy.Navigation.Close, IosBatecNavigationPolicy.navigate(5, 6, 1))
    }
    @Test fun middleMovesBetweenArtistGroups() {
        assertEquals(IosBatecNavigationPolicy.Navigation.Select(1), IosBatecNavigationPolicy.navigate(2, 6, -1))
        assertEquals(IosBatecNavigationPolicy.Navigation.Select(3), IosBatecNavigationPolicy.navigate(2, 6, 1))
    }
    @Test fun emptyAndInvalidIndexNeverNavigate() {
        assertEquals(IosBatecNavigationPolicy.Navigation.Close, IosBatecNavigationPolicy.navigate(0, 0, 1))
        assertEquals(IosBatecNavigationPolicy.Navigation.Close, IosBatecNavigationPolicy.navigate(7, 6, 1))
    }
    @Test fun halfScreenTapsAsOnAndroid() {
        assertEquals(-1, IosBatecNavigationPolicy.tapDirection(49f, 100f))
        assertEquals(1, IosBatecNavigationPolicy.tapDirection(50f, 100f))
        assertEquals(1, IosBatecNavigationPolicy.tapDirection(99f, 100f))
    }
    @Test fun horizontalGesturesThreshold() {
        assertEquals(0, IosBatecNavigationPolicy.swipeDirection(-55.9f, 56f))
        assertEquals(0, IosBatecNavigationPolicy.swipeDirection(55.9f, 56f))
        assertEquals(1, IosBatecNavigationPolicy.swipeDirection(-56f, 56f))
        assertEquals(-1, IosBatecNavigationPolicy.swipeDirection(56f, 56f))
        assertEquals(0, IosBatecNavigationPolicy.swipeDirection(100f, 0f))
    }
}
