# Musike-inspired desktop update

This Windows update studies the publicly documented interaction patterns in [Musike](https://github.com/Ernani1234/musike): a desktop-first listening surface, immediate search, a visible queue, responsive side panels, and a personal collection alongside playback controls.

Pocket Music remains its own PowerShell/WPF application. No Musike source, assets, account data, or branding are included in this build.

## What changed

- **Favourites collection:** select an offline track in Library and choose **Favourite**, or use the track's `…` menu. The Home screen now opens this collection directly.
- **Persistent and local:** favorites are stored as canonical paths in Pocket Music's existing local desktop settings. They never upload audio, copy a file, or depend on a YouTube connection.
- **Clear collection state:** the Favourites filter has an intentional empty state and clears together with the normal Library filter.
- **Existing desktop strengths retained:** queue editing, compact/full local player, preview playback, the taskbar controls, media keys, and responsive now-playing/download panels all stay on the shared playback service.

## Verification and limits

The packaged desktop self-test verifies favorite toggling, filtering, and settings restore along with existing desktop flows. It does not prove a live YouTube search, preview, or download, because those depend on external availability and account restrictions.

An REA artifact-inspection attempt was made against the packaged Windows folder, but the cached REA CLI referenced by this workspace was no longer present. The change was therefore verified with Pocket Music's real launcher self-test rather than claiming unavailable REA evidence.
