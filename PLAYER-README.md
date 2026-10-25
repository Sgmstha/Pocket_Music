# Playback inside Pocket Music

Open **Launch-Metrolist.cmd**, choose the folder containing your music, and open **Library**. Double-click a track, press Enter, or use Play selected to play it inside Pocket Music. Home's recent-file list also plays locally with double-click or Enter. The player stays available along the bottom while you search or download.

- Play/pause, elapsed time, duration, seeking, and volume are in the bottom player.
- Next follows the displayed library order captured when you select a track. Previous restarts the current track after three seconds, otherwise goes to the previous track.
- Playback advances automatically and stops at the end of the queue. It does not wrap. Play restarts the last track after the queue ends.
- The library refreshes after a successful download. Double-click the new file to play it. Downloads do not interrupt the current song or start playback unexpectedly.
- Refreshing or changing the folder does not interrupt an existing playback queue. Starting a new track uses the newly displayed library.

MP3, M4A and WAV open through Windows' built-in media engine. Opus and FLAC are decoded by the existing FFmpeg installation to a temporary 48 kHz, 16-bit stereo WAV for playback. Other native-format failures also attempt this compatibility path. Original audio files are never modified. Playback is not a bit-perfect audio implementation.

Preparation completes before compatibility playback begins. Long files take longer and use temporary disk space (approximately 11.5 MB per minute). Temporary playback files are cleaned when changing tracks or closing the app normally. Extremely long files approaching the WAV 4 GB limit are not a supported playback target in this prototype. Windows Media features and a working audio output device are required. Corrupt, missing, or unplayable files show a message in the player instead of silently opening another application.

Local/downloaded files and YouTube search-result previews share this player. See PREVIEW-README.md for the official embedded streaming player. The current playback service also supplies shuffle, repeat and saved local queues; equalizer is not implemented.

## Verification, 24 October 2026

41 integration checks passed using generated eight-second audio in all five formats, through the actual Windows player in the application:

- duration and title, advancing playback position, pause stability, seeking, resume and volume for each format;
- explicit Play selected starts playback; selection alone is passive;
- next/previous, automatic next and end-of-queue stop;
- readable missing-file error.

Audio tests were muted. They verify engine state and timing, not a human listening test of speaker output. Earlier search and download behavior is preserved. The user separately confirmed successful real music downloading before requesting this player.

To repeat with a folder of test fixtures containing one file in each supported format:

`powershell.exe -NoProfile -STA -ExecutionPolicy Bypass -File .\MusicDownloader.ps1 -Desktop -PlaybackTestFolder "C:\path\to\fixtures"`

Implementation references: [Microsoft WPF MediaPlayer](https://learn.microsoft.com/en-us/dotnet/api/system.windows.media.mediaplayer), [FFmpeg format documentation](https://ffmpeg.org/ffmpeg-formats.html). The new player source is GPL-3.0 alongside the desktop adaptation.

Windows thumbnail buttons, system media keys and title/artwork integration are documented in WINDOWS-MEDIA-README.md. They use this same playback service for local audio and YouTube previews.
