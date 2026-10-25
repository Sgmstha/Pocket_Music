# Pocket Music Android — Home & Library beta

Version **0.3.0-beta.2**, versionCode 4, package com.pocketmusic.android. Android 8+; standalone, with no PC server/account/API key. This update changes only Android.

## Install and choose a folder

Use ARM64 for most current phones, or ARMv7 for older 32-bit ARM phones. Install over the previous beta to retain your library. **Do not uninstall first.** Allow notifications for download/background playback controls.

Open the top **⋯ menu → Settings & download folder → Choose / change folder**. Android's system picker asks you to authorize a directory. Choose or create a folder such as Music/Pocket Music or Documents/Pocket Music. Some Android versions restrict selecting storage/Download roots; select a subfolder instead.

Existing saved tracks are copied to that folder when you select it. Future MP3/MP4 downloads save there automatically after conversion. The first download asks for a folder if none is authorized. Folder choice persists across app restarts and updates. Settings also offers **Save existing music to this folder** for retries.

Files in the selected folder remain after uninstall. An app-specific playback copy is retained for reliable offline playback, so there are two copies. Uninstall removes the app-specific copy and preferences. Save existing music to the folder before uninstalling. This build does not automatically scan or import other files already in that folder; use **Import audio** in the top menu.

## What's changed

- Separate **Home, Search, Library** destinations. Import moves into the top menu and Settings.
- Home has a collection card, Jump back in, mood/discovery shortcuts and Recently added. Content comes from your local library; mood cards run a search when tapped, rather than showing invented personalized recommendations. No automatic discovery network polling.
- Search has its own input/results page. Preview and Download remain separate actions.
- Library uses compact artwork/title rows: **tap a track to play**. A track's ⋮ menu has playback/save actions. The former Play local copy/Export button pair is removed from each row. Play all and folder settings are available at the top.
- Downloads copy automatically to your chosen directory. Saving is idempotent for already completed copies; unrelated same-name files are preserved. If a destination fails, the downloaded playback copy remains and Retry saving does not re-download the track.
- The player sheet has an opaque dark gradient backing, so underlying controls do not show through. The mini-player appears only after a track is selected. Keyboard entry hides bottom navigation/mini controls while audio continues.
- Short staggered native opacity/translation reveals use a custom easing curve and respect Android's disabled-animation setting. No Anime.js runtime, live blur, continuous animation, or new UI framework is added. The requested animation skill's motion principles are adapted to native Views.

![Home](preview-android.png)
![Library](preview-library-android.png)
![Now Playing](preview-player-android.png)

## Playback and storage boundaries

Local audio uses Android MediaPlayer/MediaSession with background playback, queue auto-next, shuffle, repeat, seek, volume, notification/media-key controls, audio-focus handling and artwork. Tap the playing title for full controls. File import/export remains available in secondary menus. MP4 can be opened with an external video app after it is saved to the chosen folder.

YouTube previews use the official IFrame API, with a visible 200 × 200 dp Audio player or larger Video player. Switching modes keeps the same player/position/state. Preview pauses outside the foreground; use downloaded/imported audio for background listening. A new search does not replace the active preview queue.

Bundled yt-dlp, QuickJS and FFmpeg handle MP3/MP4 downloads. MP4 prefers up to 720p. The verified bundled nightly engine, daily official update check and one failure-recovery retry are unchanged. See BUNDLED-ENGINE.json. Videos/networks/embedding may be unavailable; Details and Retry report failures.

No analytics, broad storage permissions, background library scans, or additional renderer dependencies. Source remains native Java. Metrolist's Home sections and compact music-navigation patterns were reviewed as design references: https://github.com/MetrolistGroup/Metrolist and its HomeScreen.kt. No Metrolist code/assets were copied or imported.

## Build and verification

Use Android Studio/JDK 17+ (tested JDK 21), Android SDK 36 and Gradle wrapper 9.1.0. Configure sdk.dir in local.properties. Run:

```
gradlew.bat :app:assembleRelease :app:testDebugUnitTest :app:lintRelease
gradlew.bat :app:connectedDebugAndroidTest
```

The SAF instrumentation tests require a directory authorized through the actual system picker; otherwise they skip. Enable liveDownload=true to run network download tests. See TEST-RESULTS.md for results and limits.

Release APKs under app/build/outputs/apk/release are non-debuggable, signed with the **existing local test key**. This remains a beta, not a production/Play Store signing identity. Real ARM phones, long battery sessions and OEM background policies need validation. Keep/back up the signing environment for compatible updates.

## Source and licenses

Pocket Music Android is provided under GPL-3.0; see LICENSE. This directory contains its application source, tests and build scripts. No Metrolist Android code is copied.

- youtubedl-android 0.18.1 (GPL-3.0), including the native dependency build recipes: https://github.com/yausername/youtubedl-android/tree/0.18.1
- yt-dlp: https://github.com/yt-dlp/yt-dlp
- FFmpeg source and license: https://ffmpeg.org/download.html and https://ffmpeg.org/legal.html; Android build recipes: https://github.com/yausername/youtubedl-android/blob/0.18.1/BUILD_FFMPEG.md
- Python: https://www.python.org/downloads/source/ and https://docs.python.org/3/license.html
- QuickJS: https://bellard.org/quickjs/
- AndroidX and Kotlin: Apache-2.0; https://android.googlesource.com/platform/frameworks/support/ and https://github.com/JetBrains/kotlin
- Jackson: Apache-2.0; https://github.com/FasterXML/jackson
- Apache Commons IO: Apache-2.0; https://commons.apache.org/proper/commons-io/

Upstream components retain their original copyrights and licenses. Bundled native binaries are supplied by the pinned youtubedl-android artifacts; refer to that upstream version for its corresponding sources and build scripts.
