# Lower-power playback

The app now stops download, search, local-player and preview timers when inactive. Search and download progress are checked every 300 ms only while a job is running. Native playback updates its position every 500 ms while visible; paused and minimized native playback need no polling. Windows media events still handle track completion and errors.

YouTube sends immediate playback state changes and periodic position messages once a second during visible playback, or once every five seconds when hidden/paused. Its watchdog runs every five seconds only while a preview exists. Local-copy availability scans the selected folder once per refresh instead of once per result. Local playback disposes the embedded browser.

The earlier background-playback browser flags remain enabled so minimizing does not interrupt YouTube playback. Streaming still requires WebView2, network traffic and video decoding. Play local copy is the lowest-overhead playback path; Audio preview mode still uses a real visible YouTube video player.

## Verification — 24 October 2026

Two short idle samples on this PC measured the app's PowerShell process after three seconds of startup: before, 0.219 CPU-seconds over eight seconds; after, 0.000 measurable CPU-seconds over eight seconds. Working set was 221.9 MB before and 219.2 MB after. These are short local observations, not a guaranteed CPU percentage, battery-life result, or a measurement of active WebView2 streaming. This optimization reduces CPU wakeups rather than replacing the PowerShell/WPF runtime or materially reducing its memory footprint.

Desktop checks verify all four idle timers are stopped. The browser bridge tests verify reduced hidden-page heartbeat traffic. All 41 existing native playback checks passed. Live YouTube checks passed, including 25 seconds minimized, pause/seek/resume, Audio/Video mode changes, queue navigation, local-copy handoff and error handling.
