# v0.4 music discovery and native UI

Scope authorized by the user: substantially improve Android recommendations and UI, exclude non-music videos from discovery, reuse licensed open-source components/assets, and release on GitHub. Retain standalone Java, working downloads/account connection, selected folders, native background audio and visible official previews. Target v0.4.0-beta.1 unless physical-device evidence supports stable status.

| Public reference | Evidence / use |
| --- | --- |
| https://github.com/MetrolistGroup/Metrolist | Product features: quick picks, music catalogue search, liked/library listening and compact music navigation |
| https://support.spotify.com/us/article/your-taste-profile/ | User control over recommendation taste; feedback and listening signals |
| https://www.spotify.com/uk/safetyandprivacy/understanding-recommendations/plain | Feedback shapes recommendation relevance |
| https://teamnewpipe.github.io/NewPipeExtractor/javadoc/ | Documented MUSIC_SONGS catalogue integration, not general YouTube search |
| https://github.com/material-components/material-components-android | Apache-2.0 reusable native components: navigation, chips, cards, sheets and snackbar |
| https://lucide.dev/license | ISC-licensed reusable icon assets; pinned revision in third_party/lucide/UPSTREAM.txt |
| https://github.com/bumptech/glide | BSD/Apache-2.0 image loading, bounded caches, lifecycle cleanup |

The user's explicit request to reuse licensed open-source code/assets applies to these libraries and icons. Replica's clean-room rule still applies to Spotify/Metrolist branding and proprietary assets. No Metrolist implementation, private endpoints or branding is copied; its Compose views are not directly compatible with this Java app. Public library APIs and properly attributed reusable assets replace building those systems from scratch.

## Screens / states

V01 Home: branded header, taste action, quick collection chips, mix hero, compact picks, multiple artwork shelves and saved/recent listening. Empty, refreshing, cached/offline, partial source failure and first-use states.

V02 Search: Songs default / Videos explicit mode, local matches, recent queries and cached results. Mode-specific cache, replacement/cancellation, typed music metadata, empty/error states. Video links remain accepted explicitly.

V03 Taste: up to five artists and four genres, likes/listening explanation, save/cancel. Preference updates invalidate stale recommendation requests.

V04 Liked songs: a real persisted song collection plus explicitly liked local files; play/download/queue actions and empty state.

V05 Player: existing transport/seek/volume/shuffle/repeat/queue/timer with Material sheet, artwork and an accessible favorite action. Native audio remains background-capable; previews stay visible/foreground.

## Core flows

F01 choose taste or listen/like → song-catalogue search seeds → diverse mix and source-labeled shelves → Play / separate Download.
F02 song options → Like / Not for me → liked collection or hidden recommendation → Undo where appropriate. Feedback stays in private on-device preferences.
F03 Search Songs → cache/network → catalogue songs → local-first playback; Videos is opt-in and its results never enter music discovery merely because of title words.
F04 return to saved/recent music → playback and queue editing; maintain current controls and no unrelated desktop changes.

Data: catalogue Track adds music provenance and artwork URL. Taste profile stores bounded known songs, likes, recent listening, hidden IDs, genres and artists. Shelves store source explanations, schema/version, scope, profile version and freshness. No claim to replicate Spotify/YouTube Music's proprietary recommendation models or to sync account watch history.
