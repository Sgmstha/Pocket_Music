# Android verification — 25 October 2026

Build: **0.2.0-test**, versionCode 2, Android 8+ (minSdk 26), target/compile SDK 36. Release APKs are non-debuggable and test-signed. Runtime tests used Android 16 / API 36.1 x86_64; ARM variants require physical-device testing.

Passed for this update:

- Gradle assembleRelease, assembleDebug, assembleDebugAndroidTest, testDebugUnitTest, and lintRelease. Five unit tests passed, including HTTP 403, unavailable-video, and offline error classification. Lint: 0 errors, 31 warnings (including existing English UI strings, dependency updates, and the required JavaScript-enabled WebView).
- Runtime engine update fetched official yt-dlp nightly **2026.10.23.233055** on Android. The same upstream executable is bundled with the app; its SHA-256 matches the GitHub release asset digest in BUNDLED-ENGINE.json. Upgrades install this bundled engine before using it; daily online checks keep it current.
- Opt-in Android LiveDownloadTest downloaded the Creative Commons Big Buck Bunny sample YE7VzlLtp-4 from YouTube, converted it with bundled FFmpeg to MP3, verified the saved file, and deleted it. A final rerun against the final bundled-engine code passed together with the playback test: **OK (2 tests), 48.414 seconds**.
- Native regression test converted a generated four-second WAV to MP3 and verified pause, seek, resume, and playback behind Home.
- Final release APK installed over the earlier test app and launched. Its signing certificate matches v0.1.0, allowing in-place upgrades while retaining app storage.
- Release UI keyword search for piano returned 10 results. Artwork loaded, result actions and separate format dialog were inspected, and bottom navigation and the compact player were checked on the emulator.
- Release YouTube preview played. Bottom-bar pause and Audio → Video switching retained the same 1:11 position and paused state. Audio WebView bounds were 200 × 200 dp; Video enlarged the same WebView without navigating it.
- ARM64 and ARMv7 APK signatures and ZIP alignment are verified when packaging the delivery files. This does not certify every native library on every 16 KB-page physical device.

The first live test used the obsolete BaW_jenozKc sample and correctly failed because YouTube removed it. Upstream had replaced that sample in its documentation; the current documented Creative Commons sample succeeded. The failure was not hidden or treated as a passing download.

Remaining device checks: the exact video/network from the reported screenshot, physical ARM phones, all supported Android versions/OEM background policies, long battery tests, and broad MP4/region/restricted-video coverage. YouTube can still refuse individual videos or networks; the app presents a short message, Retry, and the diagnostic Details log. This remains a test prerelease.
