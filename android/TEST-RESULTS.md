# Android beta 2 verification — 25 October 2026

Build **0.3.0-beta.2**, versionCode 4, package com.pocketmusic.android. Android 8+; compile/target SDK 36. Windows source is unchanged.

## Passed

- assembleRelease, assembleDebug, assembleDebugAndroidTest, 14 unit tests and lintRelease. Release lint: 0 errors, 20 warnings (English UI strings and dependency/WebView advisories).
- Nine instrumentation tests passed on API 36.1 x86_64 in 51.766 seconds: four SAF folder cases, full native UI journey, live automatic download-to-folder integration, two queue/media-session/focus cases, and native audio conversion/background playback.
- The real Android folder picker granted Documents/PocketMusic read/write access. Selecting it automatically saved an existing WAV outside app storage. Access remained valid after app restart and in-place release APK update.
- SAF tests verified actual external file contents, persisted access, idempotent repeat saves, retry after a destination file was deleted, retention of the playback copy when no folder was configured, and preservation of unrelated same-name files.
- LiveFolderDownloadTest invoked the actual foreground DownloadService, downloaded the Creative Commons Big Buck Bunny sample YE7VzlLtp-4, converted it to MP3, automatically saved it into the authorized folder, and verified the external byte count and final status. Test artifacts were removed by the test.
- ExperienceUiTest verified Home/Jump back in/discovery sections, separate Search input, Library tap-to-play, no repeated Play local copy/Export buttons, expanded local controls, and Import under the top menu.
- The existing tests verify native media-session Previous/Next/Pause/Play/seek, notification token/actions, automatic next behind Home, repeat-one, ducking, transient focus recovery, manual-pause precedence, generated WAV-to-MP3 conversion and background playback.
- After the keyboard/idle-player changes, all eight affected non-network runtime tests passed again in 17.833 seconds. The final Home wording/spacing change received another UI-journey pass before packaging.
- Manual release inspections: Home, Search, Library, Settings/system folder picker and opaque Now Playing. A 360dp x 640dp layout with 1.3 font scaling kept Search usable while the keyboard was open; bottom UI moved out of the typing area. Idle player is hidden. Normal 360dp x 800dp screenshots are included.
- ARM64/ARMv7 release APK signatures and ZIP alignment verified during packaging. Signing SHA-256 remains e57c3f7d6338c169e0f3da576689da5cba191ed63051e7e2b6d8d0b142ccd5ed, matching beta 1 and allowing in-place updates.

## Design and performance

Native Java Views, MediaPlayer, MediaSession and the official YouTube IFrame API are retained. Metrolist was reviewed for navigation/Home section patterns; no code/assets were copied. The requested Anime.js skill's stagger/easing principles are implemented with native ViewPropertyAnimator, not a web animation runtime. Reveals finish in under 500ms, respect disabled animations, and stop on backgrounding. No continuous blur, animation, recommendation polling or background library scan was introduced.

## Still beta

This pass uses an emulator, not physical ARM hardware. Long screen-off/battery sessions, real calls/headsets, all supported Android versions/OEM policies, cloud/removable-storage provider failures and 16KB-page native-library compatibility need physical-device coverage. Focus tests invoke the actual service focus listener; this does not replace real call/Bluetooth tests.

The existing local test certificate remains in use; this is not a production/Play Store signing identity. YouTube preview behavior and its visible 200x200 Audio player are retained from beta 1; preview pauses outside the foreground. Saved/imported local audio supports background listening.

Downloads are staged in app-specific storage and copied to the user-selected folder after completion. Keeping a playback copy uses additional disk space. Folder copies survive uninstall; app-private copies/preferences do not. Settings can retry saving existing tracks without downloading again. Import remains an explicit action; the selected destination is not continuously scanned. Export remains in a track's secondary menu as Save a copy elsewhere.