# Pocket Music Windows v0.4.0-beta.1

The desktop now shares the refined Android player's dark-blue palette and restrained portable-player hierarchy. Existing WPF/Windows PowerShell code and the central playback service are retained.

## Library

Find a saved track or format with the local filter, select a format from All audio/MP3/M4A/Opus/FLAC/WAV, or use the remembered title/newest sort. Counts and storage reflect the current view. Play all preserves the visible collection order. The compact row overflow holds Play, Play next, Add to queue, Show in Explorer and Delete file. Selecting a row still does not start playback.

Delete file identifies the exact filename and selected folder, defaults to No, then delegates to Windows' Recycle Bin operation. Windows controls any additional confirmation/progress and may offer permanent deletion for unsupported drives; follow that dialog. Other copies are not located or deleted. Only supported audio files directly inside the selected music folder are accepted; folders, linked files/folders and outside paths are rejected. Active downloads must finish/cancel first. Deleting the currently playing file stops it and clears stale queue/session references; deleting a queued file preserves the current song. Cancelling the app confirmation changes nothing. If Windows cancels after the player has released a locked file, the file stays and playback can be resumed.

## Focused player

While saved audio is selected, click the artwork in the bottom bar or **Expand player** in Now playing. The focused view fills the app window with artwork, source/format, title, timeline, queue position and minimal transport. Maximize the normal window for a larger view. Back or Escape returns to browsing without restarting music. Sound reveals volume; Timer, Queue and track options stay available. Playback position/play state, Windows media keys and taskbar controls all use the same session.

Official YouTube previews keep their existing visible mini-player/Audio–Video panel. The focused local-audio view does not hide or duplicate the official player. No decoder, browser, animation loop or periodic timer is added. Artwork is reused; position updates use the existing active-playback timer, which still stops while minimized.

## Install or update

Extract the complete Windows ZIP to a new folder and launch **Pocket Music.exe** there. Keep its scripts/resources together. Existing library files and per-user preferences/session stay in their existing locations; do not delete your music folder. The executable is an unsigned portable launcher, not an installer or a single-file runtime. Windows 10/11 with Windows PowerShell 5.1 is required. Run **Setup.cmd** if FFmpeg, Node or the WebView2 runtime is missing; it uses the existing Windows Package Manager setup. The ZIP includes the existing yt-dlp and WebView2 SDK components with their notices.

## Verification and limits

Native WPF smoke checks cover the actual UI, filter/sort/format, non-autoplay selection, focused-view state preservation, volume, deletion boundaries/cancellation and stale queue removal. Native playback checks use generated audio in all five formats, including pause/seek/resume through the focused view. Windows media-session/thumbnail tests exercise transport and metadata while minimized. Conversion, download-error and synthetic encrypted-session regressions also run. No real credentials are used in automated runs; no new authenticated download or fresh-machine dependency installation is claimed. The Recycle Bin callback is substituted with a fixture move for automated deletion tests, so Windows' own confirmation UI still needs normal user testing. This remains a prerelease.
