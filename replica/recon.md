# Pocket Music desktop reconstruction

Scope: rebuild the desktop listening and browsing shell around public Spotify desktop UX patterns, retaining Pocket Music branding and the Android blue palette. Keep WPF/PowerShell, the existing local player, official YouTube embed, download folder/format workflow and Windows integration.

Public sources reviewed 2026-10-25/26:
- https://newsroom.spotify.com/2023-06-20/spotify-desktop-experience-redesign-your-library-now-playing-views-customize/ — official public search excerpt establishes left library / centre browsing / right now-playing composition. Full article fetch timed out; its full screenshots were not inspected.
- https://newsroom.spotify.com/2021-03-25/introducing-a-new-spotify-experience-across-desktop-app-and-web-player/ — navigation, queue, sorting and offline UX; public image viewed as reference.
- https://support.spotify.com/us/article/your-library/ — collection navigation and filtering.
- https://github.com/morluto/rea — official Rea instructions and CLI documentation.

No Spotify application code, binary, private endpoints, account, logo, font or artwork was imported. Rea inspected Pocket Music's own media DLL; it supplied no Spotify internals.

| Screen | Access | States |
|---|---|---|
| S01 Home | Home | Empty/filled/narrow; six recent plays, four saved tracks, moods and discovery |
| S02 Search | Search, Ctrl+K, global query | Ready topics/suggestions/loading/results/empty/error |
| S03 Library | Library | Folder, filter, sort, explicit playback, empty/unreadable folder |
| S04 Now playing | Header toggle | Open/closed/narrow/local/preview/idle; current state and next queue entries |
| S05 Downloads | Header or result save | Existing ready/busy/saved/error; separate format and destination |

Core flows: choose folder → sidebar/shelf → play with contextual next/previous; query/topic → real YouTube search → separate preview/save; current playback → now-playing toggle → same player/state; literal library filter/sort → play or enqueue. Track retains Title/Subtitle/Artwork/Path or Id/Url/Duration. The existing PlaybackService owns all playback state.
