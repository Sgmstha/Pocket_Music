# Pocket Music for Android — standalone test build

Version 0.1.0-test, package `com.pocketmusic.android`. Requires Android 8.0 or newer. This is a separate native Android implementation; the Windows app is unchanged.

## Install

Use the ARM64 APK for most current phones. Use the ARMv7 APK only for an older 32-bit ARM phone. The x86_64 build is for the emulator. Copy the APK to the phone, open it in Files, and allow installation from that source when Android asks. Allow notifications for download and background-playback controls. No PC server, account or API key is required.

These APKs are signed with the local Android **test signing key**, not a production release identity. Keep the source/build environment if you want compatible test updates. They are not Play Store releases. Device stability must still be confirmed on your phone.

## Features and boundaries

- Search song/artist names or paste an HTTPS YouTube video URL. YouTube Share → Pocket Music fills the search box.
- Preview uses the official YouTube IFrame API. Audio keeps a visible 200 × 200 player beside the thumbnail. Video enlarges that same player; switching does not navigate/restart it. Mode is remembered.
- Bottom controls provide play/pause, seek, volume and previous/next. YouTube previews pause when the app leaves the foreground. For screen-off/background listening, use **Play local copy** or imported audio.
- Standalone MP3 and MP4 downloads use bundled yt-dlp, QuickJS and FFmpeg. MP4 prefers video up to 720p to reduce download/decode cost. Choose the format in the separate Download dialog and confirm you have permission to download the content.
- Downloads run with a foreground notification, with Cancel available there. Availability is subject to YouTube network/region/embedding restrictions; no sign-in bypass is included.
- Library plays downloaded audio through Android's native player and media notification, handles audio focus/headphone disconnect, and supports background playback. Import accepts audio chosen using Android's file picker.
- Downloads/imports live in app-specific storage. **Uninstalling removes those files.** Use **Export** from Library to save a persistent copy in Downloads or another folder before uninstalling. MP4 playback is through an external player after Export.
- No analytics, ads added by Pocket Music, background scanning, or broad storage permission. YouTube's own embedded-player behavior still applies. Native conversion uses CPU only while downloading/preparing files; preview uses the system WebView.

## Build and test

Open this directory in Android Studio. Use JDK 17+ (tested with Android Studio's JDK 21), Android SDK platform 36, and Gradle 9.1.0. Set `sdk.dir` in local.properties to your Android SDK path.

Run `gradlew.bat :app:assembleRelease :app:testDebugUnitTest :app:lintRelease` on Windows. With an emulator/device connected, run `gradlew.bat :app:connectedDebugAndroidTest`. The instrumentation test generates its own WAV, converts it using the actual bundled yt-dlp/FFmpeg, checks native pause/seek/resume, and verifies playback behind the Home screen. It does not download copyrighted test media.

APK output: `app/build/outputs/apk/release/`. Dependencies are pinned in Gradle. Release APKs are non-debuggable but test-signed; shrinking is disabled for this first testing build to avoid breaking reflection/JNI in downloader dependencies.

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
