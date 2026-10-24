# Preview play — official YouTube embedded player

Click **▶ Preview** on a search result, or **▶ Preview play** on the selected-track card. Pocket Music shows an Audio | Video toggle above the existing bottom bar. Audio shows the thumbnail and title in a compact lower-right card with a visible 200 × 200 YouTube mini-player. It no longer reserves a full preview row. Video enlarges that same player to 560 × 315. Switching changes only its size, preserving position and play/pause state. The last mode is remembered in %LOCALAPPDATA%\PocketMusic\preview-mode.json. You can use YouTube's controls or the bottom bar's play/pause, seek, volume and previous/next buttons.

The preview queue follows the search results visible when you start it. Previous restarts after three seconds; otherwise it moves to the preceding result. Next moves to the next result. The queue advances at the end and stops after its final item. Starting local playback disposes the browser player immediately; starting a preview closes local playback and any active audio preparation. Minimizing keeps playback running. Background timer throttling, renderer backgrounding, and occluded-window backgrounding are disabled for WebView2. The app never requests suspension; renderer resume preserves the chosen play/pause state. Stop preview removes the embedded player.

**Downloads remain independent.** Choose MP3 for audio or MP4 (video) for video with audio in the Download format dropdown. The preview toggle never changes the download format. MP4 files are saved to the chosen folder; use Open folder to play them in your video player. Preview uses only YouTube's official IFrame Player API and does not invoke yt-dlp, extract media URLs, transcode, or save an ordinary audio file. WebView2 uses normal browser storage/cache and may retain YouTube cookies in `%LOCALAPPDATA%\PocketMusic\WebView2`.

YouTube's normal branding, controls, ads, content restrictions and embedding rules remain in effect. When the browser blocks scripted playback, click Play inside the visible YouTube player. A video whose owner disables embedding cannot be previewed here. Errors distinguish unavailable/private videos, embedding disabled, offline/network failures, and player identification errors. You can retry with either Preview button. No login or bypass is added.

## Runtime and implementation

- Windows x64 with Microsoft Edge WebView2 Evergreen Runtime. It is present on the development PC. Setup.cmd installs it via Windows Package Manager when missing. Manual runtime installer: https://developer.microsoft.com/microsoft-edge/webview2/
- Microsoft WebView2 SDK 1.0.4314.35 managed assemblies and x64 loader are included under tools/webview2, with Microsoft's LICENSE.txt and NOTICE.txt. They were downloaded from Microsoft's package on NuGet. These components retain their own license.
- Preview.ps1 provides the WPF bridge; preview/index.html uses `YT.Player` from `https://www.youtube.com/iframe_api`. It is served from WebView2's local virtual origin `https://pocketmusic.desktop`, mapped only to the preview directory. The page supplies its origin and strict-origin-when-cross-origin referrer policy, rather than using an anonymous file/about:blank embed.
- Native messages are accepted only from that local page and the current preview token. Commands are limited to play/pause/seek/volume. Previous preview events cannot change the active track. Navigations out of the local host are blocked; user-initiated YouTube/Google popup links can open in the default browser.
- The prior local player, downloader, and launcher are retained. This does not replace search with a new service or rewrite the application.

## Tests

The live integration test streams YouTube's public IFrame API demo at zero volume. It verifies real playback, pause, seek, resume, duration/progress, and preview teardown. Additional integration checks cover local/preview handoff using generated audio, simulated browser offline events, stale-event isolation, and mapped unavailable/embedding/identity errors. Simulated errors are not claims that a real restricted video was accessed.

Recorded result, 24 October 2026: the live preview checks passed, including next/previous queue transitions; all 41 existing local-playback checks passed; the .exe launch test passed 16 desktop checks, including routed clicks on both Preview buttons and confirmation that they did not invoke downloading. The browser bridge test also passed. Live testing used the YouTube API demo; availability of other videos depends on their embedding and regional restrictions.

`node PreviewBridge.test.js` verifies the browser/native command and event contract without network. The existing 41 local-format playback checks are rerun separately.

Official references:
- https://developers.google.com/youtube/iframe_api_reference
- https://developers.google.com/youtube/terms/required-minimum-functionality
- https://learn.microsoft.com/en-us/microsoft-edge/webview2/concepts/working-with-local-content

Audio/Video update verification: 21 desktop checks, 39 download/GUI checks (including a generated MP4 with both audio and video streams), and the browser bridge test passed. Live YouTube testing verified mode switches during paused and playing playback, preserving browser identity, token, position and state. Existing bottom-bar controls and preview/local handoffs passed. No live YouTube download was used for the MP4 test.

## Play local copy

Play local copy is available on search results, the selected-track card, and the preview card when the selected download folder contains MP3, M4A, Opus, FLAC, or WAV with the exact [video-id] suffix written by Pocket Music. MP4 and title-only matches are excluded. Folder changes, completed downloads, and library refresh update availability; clicking rechecks the file. Renamed files remain playable from Library.

The action closes YouTube and starts native audio playback, which continues minimized without video. Preview mode switching still preserves position and play state, independently of download format.

Verified 24 October 2026: live YouTube playback advanced for 25 seconds while minimized without suspension or watchdog error. Paused playback stayed paused across minimize/restore. Play local copy switched to native audio and continued minimized. Mode switches, bottom controls, queue navigation, handoffs, and error checks passed. All 26 desktop checks and JavaScript bridge checks passed. Background flags were verified with the installed WebView2 runtime.

References: [WebView2 flags](https://learn.microsoft.com/en-us/microsoft-edge/webview2/concepts/webview-features-flags), [Chromium switches](https://chromium.googlesource.com/chromium/src/+/HEAD/content/public/common/content_switches.cc).