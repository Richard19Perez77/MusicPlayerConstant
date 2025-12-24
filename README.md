# Music Player Constant 🎵

A lightweight Android app that plays background music continuously, even when using other applications. Perfect for maintaining focus with study music while browsing, watching videos, or using any other app.

## Features ✨

- **Continuous Background Playback** - Music keeps playing while you use other apps
- **Foreground Service** - Ensures the music won't be killed by the Android system
- **Works Over Everything** - Plays alongside VLC, YouTube, TikTok, Twitter, and any other app
- **Survives Screen Lock** - Music continues even when your screen is off
- **Auto-Restart on Error** - Automatically recovers from playback errors
- **Persistent Notification** - Control playback directly from your notification shade
- **Simple Controls** - Easy start/stop interface
- **Modern UI** - Built with Jetpack Compose and Material Design 3

## Screenshots 📱

The app provides a clean, minimal interface with:
- Status indicator (Playing/Stopped)
- Start Music button
- Stop Music button
- Helpful information about background playback

## How It Works 🔧

The app uses an Android **Foreground Service** to maintain continuous audio playback. This ensures that:

1. The music player runs independently of the main activity
2. Android won't kill the service to free up memory
3. Playback continues even when the app is closed or minimized
4. The service automatically restarts if terminated by the system

### Technical Implementation

- **Service Type**: Foreground Service with `mediaPlayback` type
- **Audio Stream**: Uses `AudioAttributes` for modern Android compatibility
- **Looping**: MediaPlayer configured for infinite loop
- **Error Handling**: Automatic recovery and restart on playback errors
- **Notification**: Persistent notification with stop action

## Getting Started 🚀

### Prerequisites

- Android Studio Hedgehog or newer
- Android SDK 24+ (Android 7.0 Nougat)
- Target SDK 36

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

1. Place your audio file in `app/src/main/res/raw/`
2. Update the resource reference in `MusicPlayerService.kt`:

```kotlin
val uri = "android.resource://$packageName/${R.raw.your_audio_file}".toUri()
```

Supported formats: MP3, WAV, OGG, AAC

## Usage 📖

1. **Launch the app** and grant notification permission (Android 13+)
2. **Tap "Start Music"** to begin playback
3. **Use other apps freely** - the music will continue playing
4. **Check your notification shade** to see the music player notification
5. **Stop playback** by:
   - Tapping "Stop Music" in the app, or
   - Tapping "Stop" in the notification, or
   - Swiping away the notification

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
│   ├── MainActivity.kt              # UI with Jetpack Compose
│   ├── MusicPlayerService.kt        # Background service for playback
│   └── ui/theme/                    # Material Design theme
├── res/
│   ├── raw/
│   │   └── studywaves.mp3          # Audio file
│   └── values/
│       ├── strings.xml
│       ├── colors.xml
│       └── themes.xml
└── AndroidManifest.xml              # App configuration & permissions
```

## Requirements 📋

### Minimum Requirements
- **minSdk**: 24 (Android 7.0 Nougat)
- **targetSdk**: 36
- **Kotlin**: 1.9+
- **Compose**: Latest stable

### Dependencies
- AndroidX Core KTX
- AndroidX Lifecycle Runtime
- Jetpack Compose (Material 3)
- AndroidX Activity Compose

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
- Verify the audio file exists in `res/raw/`
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

