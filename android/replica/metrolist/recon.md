# Metrolist-inspired discovery, search and playback — 26 October 2026

User-authorized scope: improve Pocket Music Android's personalized recommendations, search and playback. Retain Java, the current blue/dark design tokens, encrypted YouTube connection, chosen-folder downloads and official visible IFrame previews. No complete app rewrite.

| Public source | Evidence | Limit |
| --- | --- | --- |
| https://github.com/MetrolistGroup/Metrolist | Product README: personalized quick picks, search, offline audio, queue ordering, account login | Feature claims, not implementation |
| https://metrolist.cc | Product landing page | Browser-rendered; text extractor has no content |
| ../recon.md and ../screens/ | Previously inspected public store Home/player screenshots | Static layouts; no logged-in reference feed available |
| https://github.com/yt-dlp/yt-dlp#usage-and-options | Existing integration's metadata, flat-playlist, limits and cookie options | No Metrolist private endpoints inspected/copied |

## Screens and flows

| ID | Screen | States/components |
| --- | --- | --- |
| M01 | Home | Account connection prompt; cached account picks; local listening discovery; loading, offline, expired session, empty account feed |
| M02 | Search | Recent/local suggestions; on-device matches; bounded cached online results; refresh; superseded requests; empty and offline states |
| M03 | Playback / queue | Local-first song action; retained visible preview player for next song; play next/add/remove queue actions; shuffle/repeat; buffering and error |

F01 Home → connect existing account if needed → refresh account picks → play song / separate download action. Account feed must be labeled as YouTube, not YouTube Music, because existing yt-dlp's recommended feed is general YouTube.

F02 Search → local suggestions immediately → submit → cache or online results → play saved copy when present, otherwise visible preview. A newer query may replace an older one without waiting. Failed/late requests must not replace the current query.

F03 Song options → play next / add to queue → queue → select, move next or remove. Native audio stays background-capable; visible YouTube previews remain foreground-only. Switching search pages must not replace the active preview queue.

## Data and limitations

Track: id/title, optionally artist/duration (metadata evidence; high confidence). Feed cache: bounded tracks, source/basis, update time and account scope. Query cache: normalized query, results and expiry. Queue: stable track indices plus traversal order. No new catalogue, recommendation server, telemetry or app framework.

Clean-room UX pass: no target implementation code, private endpoints, branding, copy or assets imported. Existing Pocket Music login predates this pass. No user account opened by the agent. Account-specific live verification belongs on the user's phone. Music-only account feeds, albums/artist pages, account library synchronization, hidden YouTube background playback, lyrics and crossfade are outside this scoped pass.
