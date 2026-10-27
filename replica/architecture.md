# Native architecture

Desktop.xaml owns layout and native templates. DesktopStudio.ps1 supplies the persistent library, adaptive panels, cached image loading and projection of central playback state. DesktopUX.ps1 retains settings, suggestions, filtering and keyboard actions. DesktopController.ps1 owns search and collection actions.

Player.xaml / Player.ps1 keep the same transport in a 92px strip with a circular play button. PlaybackService.ps1 remains the sole player and queue owner. Set-PlaybackUIState updates the bottom bar and now-playing panel; contextual playback uses its existing KeepQueue entry point.

Rea observed managed CIL/AnyCPU metadata in the shipped PocketMusic.Media.dll. The Windows bridge is unchanged. Static evidence is documented in rea-findings.md; runtime regressions establish media behavior separately.

No new app runtime, continuous blur or idle timer. Original fallback PNGs decode into frozen cached bitmaps. Image setup is coalesced after container generation and follows recycled DataContexts. Existing download, search and player timers still stop when idle. Local settings add optional nowPlayingPanel without breaking older files. Rea's CLI did not alter Codex configuration.
