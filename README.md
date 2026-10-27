# Pocket Music — Windows and Android

| Platform | Source and instructions | Download |
| --- | --- | --- |
| Windows 10/11 | [Desktop installation and refinements](DESKTOP-REFINEMENT.md) | [Windows v0.4.0 beta 1](https://github.com/Sgmstha/Pocket_Music/releases/tag/windows-v0.4.0-beta.1) |
| Android 8+ | [Android source and installation guide](android/README.md) | [Android v0.4.0 beta 3](https://github.com/Sgmstha/Pocket_Music/releases/tag/android-v0.4.0-beta.3) |

Android is a standalone native app: it does not connect to the Windows app. The Android APKs are test-signed prerelease builds; see [verification and limitations](android/TEST-RESULTS.md). Use the ARM64 APK for most phones or ARMv7 for older 32-bit ARM phones. APK binaries are attached to GitHub Releases, not committed to Git.

The Windows source stays in its original location so existing launch instructions remain valid. Android source, Gradle wrapper, tests and documentation are under `android/`. Build output, local SDK paths and signing keys are excluded.

## Windows v0.4.0 beta 1

Extract the complete portable ZIP into a new folder, run **Pocket Music.exe**, and keep its supporting files together. The refined library adds format filtering, storage counts and confirmed file deletion. Click the bottom artwork or **Expand player** for the focused saved-audio view; Back/Escape preserves playback. See [behavior, setup and validation limits](DESKTOP-REFINEMENT.md). The portable launcher remains unsigned and the build is a prerelease.

**Windows media controls:** taskbar thumbnails now offer Previous, Play/Pause and Next, synchronized with the existing player. Hardware media keys and the Windows media card show the current title and artwork. Restart Pocket Music to load the integration. See WINDOWS-MEDIA-README.md.

**Desktop UI update (25 October):** Downloads now opens as a panel when needed, Library has filtering and explicit playback, and folder/format/window preferences are remembered. Ctrl+K focuses search; Ctrl+D toggles Downloads. See PC-UX-README.md for the changes and verification.

**Preview play:** search results and the selected-track card now offer streaming through the official YouTube IFrame player. The existing bottom bar controls both local and streamed playback. See PREVIEW-README.md for runtime requirements, errors and test details. Download audio remains a separate action.

**Direct application launcher:** double-click **Pocket Music.exe**, or use the **Pocket Music** desktop shortcut. Keep the executable and supporting files together in this folder. No console window is shown. This is an unsigned portable Windows launcher, not a single-file bundle; the application still uses the included source files, Windows PowerShell/WPF, and the existing audio tools.

The launcher source is PocketMusicLauncher.cs (GPL-3.0). Build it with the Windows .NET Framework C# compiler using `/target:winexe /reference:System.Windows.Forms.dll /win32icon:PocketMusic.ico /out:"Pocket Music.exe" PocketMusicLauncher.cs`. Running `"Pocket Music.exe" --self-test` checks the real desktop window and writes launcher-diagnostics.txt. The current launcher self-test passes all 64 desktop checks.

**Latest desktop version:** use **Launch-Metrolist.cmd** for the Metrolist-style interface with search, downloading, a local library, and integrated playback. See **PLAYER-README.md** for player controls, format handling and current tests. The original **Launch.cmd** interface below remains a fallback.

## Start on this PC

1. Open this folder in File Explorer and double-click **Launch.cmd**.
2. Paste a YouTube video URL.
3. Choose MP3, M4A, Opus, FLAC, or WAV and an existing destination folder.
4. Confirm you own the content or have permission to download it, then click **Download audio**.
5. When “Saved • ready to play” appears, click **Open folder** to find the file.

The official yt-dlp executable is included. This PC already has FFmpeg/FFprobe and Node.js. Python is not required. Keep the entire folder together. Launch.cmd uses Windows PowerShell 5.1 and the built-in WPF desktop framework; it does not change your saved execution-policy settings.

## Setup on another Windows PC / update the downloader

Use Windows 10 or 11. Double-click **Setup.cmd** with an internet connection. It downloads the latest official yt-dlp executable and, if missing, installs FFmpeg and Node.js LTS through Windows Package Manager. Installers may request Windows elevation. Close and reopen the app afterward so it receives updated PATH settings. Setup requires `winget` for missing dependencies.

Manual alternatives: install [FFmpeg](https://ffmpeg.org/download.html) (including ffprobe) and [Node.js LTS](https://nodejs.org/), making their executables available on PATH; place the official [yt-dlp Windows executable](https://github.com/yt-dlp/yt-dlp/releases/latest) in `tools/yt-dlp.exe`. Tools can also be placed directly in `tools/`. The [yt-dlp documentation](https://github.com/yt-dlp/yt-dlp#dependencies) explains the FFmpeg and JavaScript runtime requirements. Do not download executables from unofficial mirrors.

## What it does

- Downloads one publicly accessible YouTube video at a time and extracts its audio.
- Supports standard watch, shortened, Shorts, live-video links, and YouTube Music watch links. Playlist parameters are discarded; playlist-only links are rejected.
- Saves ordinary audio files named with the video title and ID. Those files are independent of your music account and subscription.
- Shows download percentage, conversion status, details, and readable failure messages.
- Cancels the downloader and its child conversion process. Closing during a download asks before cancelling.
- Ignores external yt-dlp configuration and supplies no cookies, login, DRM bypass, or access-control bypass options.
- Avoids overwriting existing downloads. Partial files may remain after cancellation or failure; retrying may resume them. Check existing files if a repeated download is skipped.

MP3 is the simplest compatibility choice. FLAC/WAV do not restore quality absent from the original YouTube audio. M4A and Opus may also require conversion, depending on the source.

## Limits and troubleshooting

This prototype is not a music service or a Premium offline-export tool. YouTube Premium does not itself provide authorization to export videos as independent files. Use content you own or have permission to download. Restricted, private, paid, DRM-protected, and sign-in-required content is not supported.

YouTube availability, regional limits, bot checks, and upstream changes may prevent downloads. On a restricted-content error, choose another publicly accessible authorized video. On a network error, check connectivity. On a rate limit, wait. Run Setup.cmd to update yt-dlp when site changes cause failures. Details may include an upstream warning about the Python runtime bundled inside yt-dlp; installing Python separately does not change that bundled runtime.

Live streams can keep downloading until they end or you cancel; prefer completed videos. Launch.cmd is the fallback downloader. Use Pocket Music.exe for desktop search, Library, local/streamed playback, queue and Windows media controls. Android is documented in the section above. The Windows app is portable and unsigned.

## Verification

Run `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\Test.ps1` from this folder. Tests generate a one-second tone locally, run it through yt-dlp/FFmpeg into all five formats, inspect each output using FFprobe, validate URLs/errors, and briefly open and close the real window while checking the permission message. The test's file-URL support is not enabled in the app.

See TEST-RESULTS.md for the recorded results. No live YouTube download has been verified; no claim of live download success is made.

## Source and dependencies

`MusicDownloader.ps1` contains the interface and process controller; `Core.ps1` handles validation and downloader arguments. Setup.ps1 installs/updates tools. No analytics, account credentials, or cloud storage are used. Downloads contact YouTube; setup contacts GitHub and package sources.

yt-dlp is a third-party project. See its [license and bundled executable notices](https://github.com/yt-dlp/yt-dlp#license). FFmpeg and Node.js are separately installed and retain their respective licenses.
