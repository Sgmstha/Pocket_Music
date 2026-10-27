# Pocket Music desktop studio

Launch **Pocket Music.exe** in this folder. Its existing native launcher loads the redesigned UI. No Electron wrapper, new app runtime or Rea dependency is required to run Pocket Music.

The desktop uses a Spotify-style listening layout with Pocket Music Android's dark blue identity: persistent library, global search, recent quick plays, saved-track cover shelf, optional now-playing panel and compact transport. Sidebar and shelf playback preserve their own collection order.

Ctrl+K searches, Ctrl+D opens downloads, Enter/double-click plays. The header toggles now playing, which auto-collapses on small displays. Folder, sort, search history and panel choice are remembered locally. Sleep presets, queue editing, official YouTube Audio/Video preview, local audio, media keys and taskbar controls remain available.

Replica informed public Spotify UX reconstruction. Rea's CLI supplied static evidence about Pocket Music's own media DLL; Spotify internals were not inspected. See replica/ for evidence, scope and validation. The Windows v0.4.0-beta.1 release includes this existing desktop composition plus the library/player refinement documented in DESKTOP-REFINEMENT.md.
