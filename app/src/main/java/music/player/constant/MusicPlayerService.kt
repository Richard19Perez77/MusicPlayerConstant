package music.player.constant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media.app.NotificationCompat as MediaNotificationCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MusicPlayerService : Service(), MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener {

    private var mediaPlayer: MediaPlayer? = null
    private var mediaSession: MediaSessionCompat? = null
    private var isPreparing = false
    private val retryTracker = PlaybackRetryTracker()

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"

        private const val CHANNEL_ID = "MusicPlayerChannel"
        private const val NOTIFICATION_ID = 1
        private const val AUDIO_ASSET = "studywaves.mp3"
        private const val MEDIA_SESSION_TAG = "MusicPlayerConstant"

        private val _isPlaying = MutableStateFlow(false)
        val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

        fun startService(context: Context) {
            val intent = Intent(context, MusicPlayerService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, MusicPlayerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        mediaSession = MediaSessionCompat(this, MEDIA_SESSION_TAG).apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    enterForegroundAndPlay()
                }

                override fun onPause() {
                    stopPlaybackAndShutdown()
                }

                override fun onStop() {
                    stopPlaybackAndShutdown()
                }
            })
            setMetadata(
                MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "Study music")
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "Music Player Constant")
                    .build()
            )
            isActive = true
        }
        updatePlaybackState(PlaybackStateCompat.STATE_NONE)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_STOP -> {
                stopPlaybackAndShutdown()
                START_NOT_STICKY
            }
            else -> {
                // ACTION_START, null sticky restart, or any other start: keep playing.
                enterForegroundAndPlay()
                START_STICKY
            }
        }
    }

    private fun enterForegroundAndPlay() {
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        _isPlaying.value = true
        initMediaPlayer()
    }

    private fun initMediaPlayer() {
        if (isPlayerActive() || isPreparing) {
            return
        }

        releaseMediaPlayer()

        try {
            isPreparing = true
            mediaPlayer = MediaPlayer().apply {
                setOnPreparedListener(this@MusicPlayerService)
                setOnErrorListener(this@MusicPlayerService)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                isLooping = true
                setVolume(1.0f, 1.0f)
                assets.openFd(AUDIO_ASSET).use { afd ->
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                }
                setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isPreparing = false
            releaseMediaPlayer()
            handlePlaybackFailure("Error loading music file")
        }
    }

    override fun onPrepared(mp: MediaPlayer?) {
        isPreparing = false
        retryTracker.reset()
        try {
            mp?.start()
            _isPlaying.value = true
            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
            Toast.makeText(this, "Music started playing...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            handlePlaybackFailure("Playback error, restarting...")
        }
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        isPreparing = false
        handlePlaybackFailure("Playback error, restarting...")
        return true
    }

    private fun handlePlaybackFailure(message: String) {
        releaseMediaPlayer()
        if (retryTracker.shouldRetry()) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            initMediaPlayer()
        } else {
            Toast.makeText(this, "Unable to play music", Toast.LENGTH_SHORT).show()
            stopPlaybackAndShutdown()
        }
    }

    private fun isPlayerActive(): Boolean {
        val player = mediaPlayer ?: return false
        return try {
            player.isPlaying
        } catch (_: IllegalStateException) {
            false
        }
    }

    private fun releaseMediaPlayer() {
        val player = mediaPlayer ?: return
        mediaPlayer = null
        isPreparing = false
        player.setOnPreparedListener(null)
        player.setOnErrorListener(null)
        try {
            if (player.isPlaying) {
                player.stop()
            }
        } catch (_: IllegalStateException) {
            // Player was already in an error or idle state.
        }
        try {
            player.reset()
        } catch (_: Exception) {
        }
        try {
            player.release()
        } catch (_: Exception) {
        }
    }

    private fun stopPlaybackAndShutdown() {
        releaseMediaPlayer()
        retryTracker.reset()
        _isPlaying.value = false
        updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
        mediaSession?.isActive = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        releaseMediaPlayer()
        _isPlaying.value = false
        mediaSession?.apply {
            isActive = false
            release()
        }
        mediaSession = null
        super.onDestroy()
        Toast.makeText(this, "Music stopped", Toast.LENGTH_SHORT).show()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun updatePlaybackState(state: Int) {
        val actions = PlaybackStateCompat.ACTION_PLAY or
            PlaybackStateCompat.ACTION_PAUSE or
            PlaybackStateCompat.ACTION_STOP or
            PlaybackStateCompat.ACTION_PLAY_PAUSE
        mediaSession?.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1.0f)
                .build()
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Music Player",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps music playing in background"
            setSound(null, null)
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, MusicPlayerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Music Player")
            .setContentText("Playing study music...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Stop",
                stopPendingIntent
            )
            .setStyle(
                MediaNotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(0)
            )
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
}
