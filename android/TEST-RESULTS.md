# Android verification — 25 October 2026

Build: **0.3.0-beta.1**, versionCode 3. Android 8+ (minSdk 26), target/compile SDK 36. Native Java UI, MediaPlayer and MediaSession are retained. Windows files are unchanged by this update.

## Passed

- Release/debug APK builds, instrumentation APK build, 14 unit tests, and release lint. Lint has 0 errors and 36 warnings, including English UI strings, dependency updates, and the JavaScript-enabled YouTube WebView.
- Four emulator instrumentation tests passed in 42.897 seconds on Android 16 / API 36.1 x86_64. They cover a live YouTube MP3 download/conversion, generated WAV-to-MP3 conversion and native playback, media-session Previous/Next/Pause/Play/seek, notification actions/token, automatic next behind Home, repeat-one, ducking, transient focus recovery, and manual-pause precedence.
- The live test uses the Creative Commons Big Buck Bunny sample YE7VzlLtp-4 and deletes its downloaded file. It runs only with the liveDownload=true instrumentation argument.
- Queue unit tests cover repeat boundaries, shuffle without duplicates, current-track preservation, manual next versus repeat-one, and empty/single-track queues. Filename tests cover readable titles without YouTube IDs and MP4 classification.
- Dark glass home, library, compact player, and expanded local player were inspected on the emulator. UI refresh stops while the Activity is backgrounded. Glass uses static gradients, with no continuous blur/animation loop or extra renderer dependency.
- Release preview smoke test played the official YouTube API demo video M7lc1UVf-VE. Bottom-bar pause worked; Video to Audio retained the paused 0:51 position. Audio WebView measured 200 x 200 dp, using the same embedded player.
- Non-debuggable release APK installs over the existing test app, retaining its local library. Package ID remains com.pocketmusic.android. ARM64/ARMv7 signatures and ZIP alignment pass. Certificate SHA-256: e57c3f7d6338c169e0f3da576689da5cba191ed63051e7e2b6d8d0b142ccd5ed.

The bundled downloader engine remains the verified official yt-dlp nightly 2026.10.23.233055; daily official update/recovery behavior is retained. See BUNDLED-ENGINE.json for provenance.

## Beta limits

The APKs remain signed with the existing local **test certificate**, not a production release identity. ARM64/ARMv7 builds are compiled and signed but have not been physically tested in this pass. Emulator tests do not certify battery consumption, prolonged screen-off playback, every supported Android version, Bluetooth/headset hardware, every OEM battery policy, or all native libraries on 16 KB-page devices.

Audio-focus tests invoke the actual service listener with representative focus events; calls and other players still need physical-device checks. Preview queue auto-next/repeat needs broader video/network testing. YouTube previews pause when the Activity leaves the foreground; background listening uses downloaded/imported local audio. YouTube may refuse specific videos/networks.

Install over the previous version. **Do not uninstall first**: app-specific downloads are removed on uninstall. Export files before uninstalling. Promote to stable only after testing playback and battery cases on real phones and establishing a backed-up production signing identity.