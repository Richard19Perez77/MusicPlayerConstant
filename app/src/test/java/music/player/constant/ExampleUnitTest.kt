package music.player.constant

import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun defaultRetryLimitIsThree() {
        assertEquals(3, PlaybackRetryTracker.DEFAULT_MAX_FAILURES)
    }
}
