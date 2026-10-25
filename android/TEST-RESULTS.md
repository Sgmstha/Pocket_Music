# Android verification — 25 October 2026

Build: 0.1.0-test, minSdk 26, target/compile SDK 36, test-signed release APKs for ARM64, ARMv7 and x86_64. Release APK is not debuggable. Phone architectures were compiled and signature checked; runtime testing used the Android 16 / API 36.1 x86_64 emulator, not a physical ARM phone.

Passed:

- Final Gradle `assembleRelease`, `testDebugUnitTest`, and `lintRelease`. Lint has no errors; warnings include dependency updates, English-only UI strings, and the expected JavaScript-enabled WebView review warning. Preview needs JavaScript; file/content access and mixed content are disabled, and top-level navigation is restricted to the bundled app-assets origin.
- URL tests accept supported single-video HTTPS links and reject malformed links, non-HTTPS, unrelated hosts, credentials and unexpected ports.
- Android instrumentation: real bundled yt-dlp and FFmpeg converted a generated four-second WAV to MP3. Native media playback started, paused, sought, resumed, and continued behind the Android Home screen. Result: `OK (1 test)`, 19.352 seconds.
- Release APK installed and launched in the emulator. Standalone keyword search for `piano` returned 10 results without a PC server/API key.
- Official YouTube demo M7lc1UVf-VE played in the release APK. Bottom-bar duration was 22:24; elapsed position advanced. Switching Audio to Video retained playback; bottom-bar pause stopped at 0:50.
- APK signature verification (v2) and 16 KB ZIP alignment check passed for the initial ARM64 artifact; final artifacts are verified again when copied to the delivery folder. ZIP alignment does not substitute for physical 16 KB-page-device testing of every bundled native executable.

The first Gradle-managed device test was interrupted during slow concurrent emulator package installations. Sequential installation and a direct instrumentation rerun passed. No app exception was reported in the successful run.

Not yet verified: a physical phone, long-duration battery/stability measurements, real YouTube MP3/MP4 downloads on the user's network, all manufacturers' background-service policies, and every Android version supported by minSdk. Generated local media was used to verify the download/conversion engine. Treat this as a tested development APK for device testing, not a production-certified release.
