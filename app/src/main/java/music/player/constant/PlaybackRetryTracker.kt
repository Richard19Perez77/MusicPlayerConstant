package music.player.constant

/**
 * Caps automatic MediaPlayer restarts so a persistent decoder/file error
 * cannot loop forever.
 */
internal class PlaybackRetryTracker(private val maxFailures: Int = DEFAULT_MAX_FAILURES) {
    var failureCount: Int = 0
        private set

    fun reset() {
        failureCount = 0
    }

    /**
     * Records a failure. Returns true if playback should be attempted again.
     */
    fun shouldRetry(): Boolean {
        failureCount++
        return failureCount < maxFailures
    }

    companion object {
        const val DEFAULT_MAX_FAILURES = 3
    }
}
