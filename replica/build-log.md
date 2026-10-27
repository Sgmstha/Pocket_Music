# Build log

2026-10-25/26: backed up pre-change scripts/XAML in work/desktop-spotify-before. Reviewed public Spotify UX sources with Replica and used the official Rea CLI on Pocket Music's media DLL. Rebuilt the desktop shell with persistent library, global search, recent plays, cover shelf and optional synchronized now playing. Kept Android blue roles and existing player/preview/download/Windows integration.

Fixed WPF's PSObject artwork conversion issue; cached frozen local bitmaps now follow recycled rows. Added collection-specific queue routing. Inspected populated Home, Library and minimum-size Search/Downloads captures. Screenshot track names are visual fixtures only. Existing launcher still loads scripts/XAML, so no new runtime or executable rebuild was needed.

Validation is recorded in parity.md. Source updates remain local; no release was published.
