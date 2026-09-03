# Music Player Constant 🎵

A lightweight Android app that plays background music continuously, even when using other applications. Perfect for maintaining focus with study music while browsing, watching videos, or using any other app.

## Features ✨

- **Continuous Background Playback** - Music keeps playing while you use other apps
- **Foreground Service** - Ensures the music won't be killed by the Android system
- **Works Over Everything** - Plays alongside VLC, YouTube, TikTok, Twitter, and any other app
- **Survives Screen Lock** - Music continues even when your screen is off
- **Auto-Restart on Error** - Recovers from playback errors (stops after 3 failed attempts)
- **Persistent Notification** - Media-style notification with stop action and MediaSession support
- **Simple Controls** - Easy start/stop interface
- **Modern UI** - Built with Jetpack Compose and Material Design 3

## High-level design

The app is split into a thin Compose UI and a long-running **media playback foreground service**. The activity never owns the `MediaPlayer`. It only sends start/stop commands and observes playback state published by the service.

```text
┌─────────────────────┐       start / stop intents        ┌──────────────────────────┐
│  MainActivity       │ ────────────────────────────────► │  MusicPlayerService      │
│  (Compose UI)       │                                   │  foregroundServiceType=  │
│                     │ ◄──────────────────────────────── │  mediaPlayback           │
│  collect isPlaying  │       StateFlow<Boolean>          │                          │
└─────────────────────┘                                   │  ┌────────────────────┐  │
                                                          │  │ MediaPlayer        │  │
                                                          │  │ assets/studywaves  │  │
                                                          │  │ looping + wake lock│  │
                                                          │  └────────────────────┘  │
                                                          │  ┌────────────────────┐  │
                                                          │  │ MediaSession       │  │
                                                          │  │ notification Stop  │  │
                                                          │  └────────────────────┘  │
                                                          └──────────────────────────┘
```

### Why a foreground service

Android will not let a normal background service keep playing audio after the user leaves the app. `MusicPlayerService` is a foreground service of type `mediaPlayback`, so it:

1. Runs independently of `MainActivity` (rotation, backing out, or opening another app does not stop audio)
2. Shows an ongoing media notification, which is required for this service type
3. Can be restarted by the system with `START_STICKY` if the process is killed, then immediately calls `startForeground()` and resumes playback
4. Returns `START_NOT_STICKY` on a user-requested stop so the system does not bring it back

### UI and state

- `MusicPlayerScreen` is a Compose screen with Start / Stop and a playing/stopped status card.
- Playback state lives in `MusicPlayerService.isPlaying` (`StateFlow`), not in local Compose `remember` state.
- The UI collects that flow with `collectAsStateWithLifecycle()`, so it stays correct after rotation, returning to the app, or stopping from the notification.
- On Android 13+, the screen requests `POST_NOTIFICATIONS` once on launch so the foreground notification (and its Stop action) can appear.
- The layout uses edge-to-edge drawing plus `safeDrawingPadding()` so controls sit clear of system bars.

### Playback pipeline

1. **Start** — UI calls `MusicPlayerService.startService()`, which uses `ContextCompat.startForegroundService()` with `ACTION_START`.
2. **Foreground** — `onStartCommand` promotes the service with a media-style notification and type `FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK`.
3. **Load** — Audio is read from `assets/studywaves.mp3` (uncompressed MP3). `MediaPlayer` is configured with `USAGE_MEDIA`, looping, and `PARTIAL_WAKE_LOCK` so playback can continue with the screen off.
4. **Play** — `prepareAsync()` then `start()` on the prepared callback. `isPlaying` is set to `true`.
5. **Stop** — UI, notification Stop, or MediaSession pause/stop send `ACTION_STOP`. The player is released, the notification is removed, and the service stops.

`ACTION_START` and a **null** sticky-restart intent both take the “keep playing” path. Only `ACTION_STOP` shuts the service down.

### MediaSession and notification

A `MediaSessionCompat` is created when the service starts. It publishes metadata and playback state, and the notification uses `MediaStyle` so Android can treat this as real media playback (lock screen / system media controls). The notification is ongoing (not swipe-dismissible) and includes a Stop action that delivers `ACTION_STOP` to the already-running service.

### Error handling

`PlaybackRetryTracker` caps automatic recovery at **3** failures. A decoder or file error releases the current `MediaPlayer`, retries, and if the cap is hit, shows an error and shuts the service down. That avoids an infinite prepare/error loop. `isPlaying` / `stop()` / `release()` are wrapped so a player in the error state cannot crash the service.

### Audio focus (intentional)

The service does **not** request audio focus. Other apps can play at the same time (YouTube, VLC, TikTok, and so on). That is a product choice for “study music under everything,” not an oversight.

### Key files

| File | Role |
|------|------|
| `MainActivity.kt` | Compose UI, notification permission, start/stop commands |
| `MusicPlayerService.kt` | Foreground service, player, MediaSession, notification, published state |
| `PlaybackRetryTracker.kt` | Retry budget for playback errors |
| `assets/studywaves.mp3` | Looping audio track |
| `AndroidManifest.xml` | Permissions and `foregroundServiceType="mediaPlayback"` |

## Getting Started 🚀

### Prerequisites

- Android Studio Hedgehog or newer
- Android SDK 24+ (Android 7.0 Nougat)
- Target SDK 37

### Installation

1. Clone the repository:
```bash
git clone https://github.com/Richard19Perez77/MusicPlayerConstant.git
cd MusicPlayerConstant
```

2. Open the project in Android Studio

3. Sync Gradle files

4. Build and run on your device or emulator

### Adding Your Own Music

To replace the study music with your own audio file:

1. Place your audio file in `app/src/main/assets/`
2. Update the asset name in `MusicPlayerService.kt` (`AUDIO_ASSET`)

Supported formats: MP3, WAV, OGG, AAC

## Usage 📖

1. **Launch the app** and grant notification permission when prompted (Android 13+)
2. **Tap "Start Music"** to begin playback
3. **Use other apps freely** - the music will continue playing
4. **Check your notification shade** to see the music player notification
5. **Stop playback** by:
   - Tapping "Stop Music" in the app, or
   - Tapping "Stop" in the notification

## Permissions 🔐

The app requires the following permissions:

- `FOREGROUND_SERVICE` - Run the music service in the background
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK` - Specify the service is for media playback
- `POST_NOTIFICATIONS` - Display the playback notification (Android 13+)
- `WAKE_LOCK` - Keep the device awake during playback

## Project Structure 📂

```
app/src/main/
├── java/music/player/constant/
│   ├── MainActivity.kt              # Compose UI; observes service state
│   ├── MusicPlayerService.kt        # Foreground service, player, MediaSession
│   ├── PlaybackRetryTracker.kt      # Caps automatic error retries
│   └── ui/theme/                    # Material Design theme
├── assets/
│   └── studywaves.mp3               # Looping audio track
├── res/
│   └── values/
│       ├── strings.xml
│       ├── colors.xml
│       └── themes.xml
└── AndroidManifest.xml              # App configuration & permissions
```

## Requirements 📋

### Minimum Requirements
- **minSdk**: 24 (Android 7.0 Nougat)
- **targetSdk**: 37
- **Kotlin**: 2.2+
- **Compose**: Latest stable

### Dependencies
- AndroidX Core KTX
- AndroidX Lifecycle Runtime (including Compose)
- Jetpack Compose (Material 3)
- AndroidX Activity Compose
- AndroidX Media (`MediaSessionCompat` / media-style notifications)

## Building the App 🔨

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build
```bash
./gradlew assembleRelease
```

## Known Limitations ⚠️

- Only plays a single audio file (no playlist support)
- No volume control within the app (use system volume)
- No pause/resume functionality (only start/stop)
- Audio focus is not requested (plays alongside other audio)

## Future Enhancements 💡

Potential improvements for future versions:
- [ ] Pause/Resume functionality
- [ ] Volume control slider
- [ ] Playlist support
- [ ] Multiple audio file selection
- [ ] Audio focus management (duck other audio)
- [ ] Sleep timer
- [ ] Playback speed control
- [ ] Widget for home screen control

## Troubleshooting 🔧

### Music stops playing after a while
- Check if battery optimization is enabled for the app
- Disable battery optimization: Settings → Apps → Music Player Constant → Battery → Unrestricted

### Notification doesn't appear
- Grant notification permission in app settings
- Check if notifications are enabled for the app

### Music doesn't start
- Verify the audio file exists in `assets/`
- Check logcat for error messages
- Ensure proper permissions are granted

## Contributing 🤝

Contributions are welcome! Feel free to:
- Report bugs
- Suggest new features
- Submit pull requests

## License 📄

This project is open source and available for personal and educational use.

## Contact 📧

For questions or suggestions, please open an issue on GitHub.

---

**Built with ❤️ using Kotlin and Jetpack Compose**
