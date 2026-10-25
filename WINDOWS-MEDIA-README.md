# Windows taskbar and media controls

Restart **Pocket Music.exe**, start a local track or YouTube preview, then minimize the window. Hover over its taskbar icon to see **Previous**, **Play/Pause**, and **Next**. Their enabled states follow the bottom player; the middle icon and tooltip switch with the actual playback state. Clicking a thumbnail control does not restore the window.

Windows hardware media keys and media-session UI use the same playback service. The current track title, artist when available, playback status and artwork are published to Windows. Local files use embedded artwork when available; downloaded files can use their saved YouTube ID to fetch a thumbnail. Preview tracks use their existing YouTube artwork. The app icon remains as a fallback when cover art is unavailable or offline. Windows decides whether to present these details in its volume flyout, Quick Settings media card, or other system media UI.

## Implementation

- `WindowsMedia.ps1` creates WPF `TaskbarItemInfo` and three `ThumbButtonInfo` controls, whose routed commands call `Toggle-PlayPause` and `Step-PlaybackTrack`. It loads from the existing desktop controller.
- `Set-PlaybackUIState` synchronizes both the thumbnail toolbar and the Windows media session after existing local/streamed state changes. Artwork is refreshed only on metadata changes, not on a timer.
- `WindowsMediaBridge.cs` obtains the window's `SystemMediaTransportControls` through the desktop interop API. WinRT button events are dispatched onto the WPF UI thread before invoking PowerShell. Play and Pause are idempotent to avoid duplicate-key toggles. Focused-window media-key handling defers to the system media session to avoid double execution.
- Artwork loads asynchronously with cancellation and track-generation checks, so a late image from an earlier track cannot replace the current cover. The media session, handlers and streams are released on close.
- When the app media session is active, WebView2 hardware media-key handling is disabled so the embedded player cannot register a competing handler. The existing background playback flags remain in place.
- `PocketMusic.Media.dll` is the precompiled .NET Framework helper. Keep it with the application. No change to the WPF/PowerShell playback engine, MSIX registration, global keyboard hook or extra polling timer is required.

Rebuild with Windows PowerShell 5.1 and the Windows 10/11 SDK:

```
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\Build-WindowsMedia.ps1
```

The SDK is only needed to rebuild; Windows 10/11 supplies the runtime APIs. If the system media API is unavailable, the app logs the failure and retains its WPF taskbar and normal player controls.

## Verification — 25 October 2026

- 64 real-window desktop checks pass, including button ordering, Play/Pause icon updates, disabled idle controls, SMTC initialization, title/status synchronization and fallback artwork.
- 21 native Windows integration checks pass while minimized. They invoke the thumbnail routed commands and Windows' own media-session commands against a uniquely titled generated-audio test session. They verify Previous/Next/Play/Pause, duplicate Pause, title changes, queue-end button availability, and that the window stays minimized.
- The Windows session observer opens and decodes the published image stream and distinguishes the large track artwork from the app-icon fallback. This verifies OS-readable cover art, rather than just a non-null thumbnail property.
- A live official YouTube IFrame run passed: preview advanced for 25 seconds while minimized, thumbnail pause/resume and Previous/Next worked across real previews, Audio/Video mode retained position, and switching between local and streamed playback remained exclusive. Simulated offline/unavailable/embedding errors and teardown also passed. The test uses an isolated WebView profile. An initial run found an obsolete legacy-queue setup in the test harness; it was changed to the current Enqueue-Track API and the final run passed.
- 41 existing playback checks pass across generated MP3/M4A/Opus/FLAC/WAV. The YouTube iframe bridge suite also passes.
- No global simulated keyboard input is sent, so the integration test cannot target another music app. Physical media-key hardware and visual OS flyout appearance remain manual checks; the system routes those keys through the SMTC event path exercised by the tests.

To repeat the native integration test with the existing generated fixture folder:

```
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\Build-WindowsMedia.ps1 -IncludeTests
powershell.exe -NoProfile -STA -ExecutionPolicy Bypass -File .\MusicDownloader.ps1 -Desktop -WindowsMediaTest -PlaybackTestFolder "C:\path\to\fixtures"
```

The fixture folder needs `Playback test.mp3` and `Playback test.m4a`. Test playback is muted and uses isolated session settings.

Microsoft references: [WPF taskbar thumbnail controls](https://learn.microsoft.com/en-us/dotnet/api/system.windows.shell.taskbariteminfo), [desktop SMTC interop](https://learn.microsoft.com/en-us/windows/win32/api/systemmediatransportcontrolsinterop/nf-systemmediatransportcontrolsinterop-isystemmediatransportcontrolsinterop-getforwindow), [manual SMTC integration](https://learn.microsoft.com/en-us/windows/apps/develop/media-playback/system-media-transport-controls).
