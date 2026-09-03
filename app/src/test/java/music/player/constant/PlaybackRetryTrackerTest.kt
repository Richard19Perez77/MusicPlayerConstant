package music.player.constant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackRetryTrackerTest {

    @Test
    fun retriesUntilMaxFailuresThenStops() {
        val tracker = PlaybackRetryTracker(maxFailures = 3)

        assertTrue(tracker.shouldRetry())
        assertEquals(1, tracker.failureCount)
        assertTrue(tracker.shouldRetry())
        assertEquals(2, tracker.failureCount)
        assertFalse(tracker.shouldRetry())
        assertEquals(3, tracker.failureCount)
    }

    @Test
    fun resetClearsFailureCount() {
        val tracker = PlaybackRetryTracker(maxFailures = 2)

        assertTrue(tracker.shouldRetry())
        assertFalse(tracker.shouldRetry())

        tracker.reset()
        assertEquals(0, tracker.failureCount)
        assertTrue(tracker.shouldRetry())
    }
}
