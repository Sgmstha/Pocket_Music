# Online audio startup — beta 4

The Play path previously started a Python/yt-dlp extraction for every song, could update the engine before playing, then waited for Android MediaPlayer's network buffer. It did not download a whole song, but this could feel like a download.

Public songs now resolve with the already bundled NewPipeExtractor in-process. Lookup has an eight-second network budget; unsupported extraction falls back to the existing authenticated resolver. An initial failed public stream also gets one authenticated fallback, preserving pause state. Neither path performs an engine update before playback.

Only online audio uses AndroidX Media3 1.9.0, with 500 ms startup, 1.5 second rebuffer, and 3–15 second forward buffer thresholds and a 4 MiB target. The existing foreground service, audio focus, media session, controls and sleep timer remain shared. Saved files keep the original MediaPlayer path. No hidden WebView, persistent audio cache, full-song predownload, recurring prefetch, or new polling loop is used.

An eight-entry memory cache reuses resolved sources for at most five minutes, bounded further by signed URL expiry minus a minute. Keys include the account fingerprint and active network. Failed sources are evicted. URLs and headers never enter saved queues, logs or release artifacts; Cookie and Authorization headers are not sent to the audio CDN.

## Verification

- API 36.1 x86_64 emulator, account-free, public music selected from the current Songs catalogue. A measurement with the new in-process resolver and the previous MediaPlayer took 14.142 seconds, of which 4.465 seconds was resolution. This is a diagnostic comparison, not a controlled old-release benchmark.
- The complete new path took 8.747 and 9.006 seconds in two runs, including Activity startup through advancing playback after Home. A cached replay source lookup took 1 ms; this excludes decoder preparation and network buffering. Phone/network timings are not guaranteed.
- Nine focused instrumented cases passed: five real loopback HTTP playback/lifecycle cases, three source/privacy/cache cases, and one live public production playback case. These cover Home during loading, screen-off position advancement, notification/media transport seek/pause/play/next, widget previous, sleep timer, local switching, stale callbacks, failures, pause-preserving fallback, bounded/expired cache, and header filtering. Live playback did not create a downloaded music file.
- Release/debug builds, 54 JVM tests and release lint passed (0 errors, 57 warnings). Broader regression results are recorded in BACKGROUND-AUDIO-TESTS.md.

The fallback can still take longer when YouTube requires account extraction or the network is slow. Restricted/unavailable tracks and rate limits remain possible. Physical-phone battery behavior, account-restricted songs, cellular handovers and poor-network rebuffering need user testing. This is a prerelease.

Install the ARM64 APK on most current phones, ARMv7 only for a 32-bit ARM device. Update over the installed Pocket Music without uninstalling; package and signing identity are unchanged, versionCode 11. Keep the APK's requested update path so app data, downloads and account connection are retained.
