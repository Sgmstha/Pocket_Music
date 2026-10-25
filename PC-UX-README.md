# Desktop UI and UX pass — 25 October 2026

Double-click **Pocket Music.exe** in this folder. The existing launcher loads the updated PowerShell/XAML files; keep the complete application folder together.

## What changed

- Downloads are a panel you open using **Downloads**, **Ctrl+D**, or **Save** on a search result. Hiding the panel gives Home, Search and Library the full content width. The header still shows download progress and completion when the panel is closed.
- The selected-track card and settings scroll together. Download, progress, status and Open download folder stay visible at the bottom of the panel, including at the smaller 980 × 640 minimum window size.
- Library filtering matches track names as literal text, without another disk scan. The count, empty state and Play all/Add all availability reflect the current filter. Choose folder is directly available in Library.
- Selecting a library or recent track does not interrupt playback. Double-click, press Enter, or use **Play selected** to start it. Double-click/Enter on search results starts a preview; **Save** opens download settings.
- Ctrl+K focuses and selects the search query. Ctrl+D toggles Downloads. Escape closes the queue first, then Downloads. The **?** button lists shortcuts. Playback arrow/space shortcuts respect lists, text fields, buttons and sliders so normal keyboard interaction works.
- Search has clear initial, loading, empty and failure states. Cancelling stops its loading indicator. Unavailable local-copy row actions are hidden.
- The app remembers the chosen music folder, download format, window size and maximized state in `%LOCALAPPDATA%\PocketMusic\desktop-settings.json`. Invalid settings use defaults; a missing saved folder is ignored. Existing playback-session and preview-mode settings remain separate.
- Home cards use the available width, the format selector has readable text, and the normal desktop window fits smaller laptop work areas. No additional idle polling timers or UI framework dependencies were introduced.

## Verification

- **44 checks** passed in the real WPF desktop window, covering existing search/preview behavior plus the drawer, filtering, explicit playback, keyboard navigation, search states and settings recovery.
- **41 native playback checks** passed across generated MP3, M4A, Opus, FLAC and WAV, including pause/seek/resume, next/previous, automatic advance and missing-file errors. Audio was muted.
- The browser bridge suite passed: controls, position, token isolation, blocked autoplay, offline and YouTube errors.
- Home, Library, Downloads and the 980 × 640 layout were rendered and visually inspected. The idle-timer regression check passes.

This pass updates the local Windows application. It does not publish a new PC release. Live network downloads, minimized streaming and physical speaker listening were not retested for these presentation changes.

## Home and Windows identity update

Home now has a featured artwork section, illustrated discovery shortcuts, responsive recent-file tiles with direct Play buttons, and a 1440-dp content limit for wide screens. Library titles hide the trailing YouTube ID in display text; the actual filenames remain unchanged. Artwork is local and static; provenance and the generation prompt are in assets/README.md.

The WPF host now sets the process and window AppUserModelID to `PocketMusic.Desktop` before showing the window. Window relaunch properties point to Pocket Music.exe and PocketMusic.ico, so the shell can display and pin the app rather than its PowerShell host. Properties are cleared before the window closes. WindowsIdentity.cs is GPL-3.0 source; PocketMusic.Shell.dll is its optional precompiled helper and is loaded from bytes to avoid locking it. Rebuild using Windows PowerShell and Build-DesktopIdentity.ps1. Startup can compile the source if the DLL is absent.

54 real-window checks pass, including native shell property readback, native icon presence, local artwork, Home navigation and rendered recent-file Play buttons. The existing Pocket Music.exe self-test also starts successfully. Close and reopen an older running instance to load the new layout and shell identity. Old manually pinned PowerShell shortcuts are not modified.

Microsoft references: [AppUserModelIDs](https://learn.microsoft.com/en-us/windows/win32/shell/appids), [window property store](https://learn.microsoft.com/en-us/windows/win32/api/shellapi/nf-shellapi-shgetpropertystoreforwindow), [relaunch icon](https://learn.microsoft.com/en-us/windows/win32/properties/props-system-appusermodel-relaunchiconresource).
