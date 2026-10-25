# Verification — 23 October 2026

Update, 24 October: integrated playback passed 41 additional real-player checks across MP3/M4A/Opus/FLAC/WAV. See PLAYER-README.md. The user has independently confirmed that live music downloading works for them. The original agent's tests below remain a historical record.

**Result: 34 checks passed on this Windows PC.**

- Four supported URL variants normalized to a single video URL.
- Six malformed, insecure, playlist-only, or deceptive URLs rejected.
- Sign-in and rate-limit failures mapped to readable messages.
- Generated a one-second synthetic 440 Hz tone locally, with no copyrighted test media or external media download.
- Used the actual yt-dlp executable and FFmpeg to produce MP3, M4A, Opus, FLAC, and WAV files. Verified a nonempty output and readable audio stream in each with FFprobe.
- Checked external configuration isolation for each format.
- Opened the real WPF window under Windows PowerShell, invoked Download without authorization checked, verified its permission message, and closed it successfully.

Installed tools used: yt-dlp 2026.10.21, FFmpeg 8.1.2, Node.js 22.19.0. The bundled yt-dlp emitted an upstream Python 3.10 deprecation warning but all conversions passed.

The first sandboxed conversion was blocked when renaming a temporary file. Re-running the same tests with approved normal Windows access passed.

Not verified: live YouTube retrieval, real network progress, cancellation during a live transfer, long-running streams, or setup on a second PC. The test covers the real extraction/conversion engine and a window smoke test, not every interactive state. No live YouTube download is claimed.

## Desktop UI/UX update — 25 October 2026

44 real-window desktop checks and 41 native playback checks pass. The browser bridge suite also passes. Library selection is now passive; explicit Play selected, Enter or double-click starts playback. Drawer layout, filtering, preferences and smaller-window screenshots were verified. See PC-UX-README.md. Live network playback/download and physical audio output were not retested for this UI pass.

## Windows media controls — 25 October 2026

64 desktop checks, 21 native Windows media-session/thumbnail integration checks while minimized, and 41 existing five-format playback checks pass. The native probe reads and decodes actual Windows-published artwork and exercises Play/Pause/Previous/Next without restoring the app. Test sessions use generated muted audio and isolated settings. The iframe bridge unit suite also passes. See WINDOWS-MEDIA-README.md for commands and manual hardware-key/UI limits.

Live IFrame verification also passed with the Windows controls: 25-second minimized streaming, thumbnail pause/resume/previous/next, Audio/Video continuity, local/stream exclusivity, offline/error mapping and clean browser disposal. The test uses the central queue API and an isolated browser profile.
