# Desktop v0.4 refinement verification

The current task transfers the Android library/player refinements to the existing WPF application. It retains the previously implemented desktop composition and session/download behavior. The supplied portable-player references inform hierarchy, not copied photographs or branding.

- Source UI smoke: 112 checks with visual capture. Packaged native launcher: exit 0, 110 checks (capture-only assertions excluded).
- Native generated-audio playback: 53 checks across MP3, M4A, Opus, FLAC and WAV. Focused view preserves the decoder/token/play state; seek and pause/resume use its controls, and closing does not stop music.
- Windows media transport: 21 checks, including native session metadata/artwork, taskbar thumbnail controls and OS commands while minimized.
- Conversion/URL isolation: 39 checks. Download network/error rules: 15 checks. Encrypted-session handling: 40 synthetic checks. No real account cookies, browser database or live media download is used.
- Delete checks exercise literal bracket names, cancellation, outside-folder/unknown-extension/directory rejection, preserved unselected files and removal from queue/current session. They substitute Windows recycling with a move inside the generated fixture directory. Windows' own Recycle Bin confirmation remains a manual check; no actual user song is deleted by tests.
- Screens reviewed: populated library plus wide and compact focused views. Fixture tracks/artists and tiny files exist only during tests, not in shipped library data. No additional timer/decoder/browser or animation loop is introduced.
- Fixed before packaging: Windows PowerShell 5.1 UTF-8 BOM handling, test settings isolation (real maximized preferences previously affected a compact-layout assertion), and immediate time-label refresh after paused seeking.

Manual check: extract ZIP; run Setup.cmd if dependencies are missing; play a saved track and click bottom artwork; test Back/Escape, volume and Windows media buttons; filter/sort/Play all; choose a disposable song and confirm/cancel Windows recycling. Keep the build a prerelease: fresh-machine setup, accessibility software and arbitrary audio devices have not been comprehensively tested. The launcher is unsigned and portable. No benchmark CPU/battery percentages or pixel-equivalence claims.
