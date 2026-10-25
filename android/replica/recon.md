# Pocket Music Android: Replica recon

Scope confirmed by the user's Android UI requests and approval to run Replica on 25 October 2026: Metrolist-inspired Home, Search, local Library, playback, and chosen-folder downloading for the user's standalone Pocket Music app. Keep native Java and the lightweight/offline behavior. This is an incremental improvement, not a full Metrolist clone.

## Public sources

| Source | Evidence | Limits |
|---|---|---|
| https://github.com/MetrolistGroup/Metrolist | Public feature overview and store image directory | No implementation/source code inspected in this Replica pass |
| https://raw.githubusercontent.com/MetrolistGroup/Metrolist/main/fastlane/metadata/android/en-US/images/screenshots/screenshot_1.png | Home: compact mood chips, four-row picks, artwork shelves, persistent navigation | Static filled state; network/recommendation logic not observed |
| https://raw.githubusercontent.com/MetrolistGroup/Metrolist/main/fastlane/metadata/android/en-US/images/screenshots/screenshot_5.png | Player: dominant square artwork, title/source, scrubber, transport, secondary controls | Static paused state only |
| https://developer.android.com/training/data-storage/shared/documents-files | Public SAF behavior and persistable folder permission | Platform behavior, not reference-app internals |
| User's supplied Pocket Music phone screenshots and beta 2 emulator captures | Existing hierarchy, library actions, transparency and home-density problems | Evidence of Pocket Music, not Metrolist |

Six public store screenshots were inspected manually. Only Home and player match the scoped screens; artist details, recognition, Together and lyrics are excluded. Reference images remain local in replica/screens, never in the APK or public source. Search/Library designs use observed compact-row/nav patterns; their exact reference states were not supplied, so visual parity for those screens cannot be claimed.

## Inventory

| ID | Screen | Entry / purpose | Components | States |
|---|---|---|---|---|
| S01 | Home | Launch/Home; immediate access to music | Compact mood strip, playable rows, recent artwork shelf, offline collection shortcut | Empty library, populated, missing recent file |
| S02 | Search | Search tab/discovery; find and preview | Search field, remembered queries, static loading rows, compact results and separate download action | Empty, loading, results, offline/error, long titles |
| S03 | Library | Library tab; play saved music | Media filters, sort control, full-row playback, artwork, overflow | Empty, audio/video, no matching filter, long names |
| S04 | Now Playing | Mini-player title; full local controls | Square cover, title/source, timeline, previous/play/next, shuffle/repeat/queue/volume | Preparing, playing, paused, ended, missing artwork |
| S05 | Download/folder | Result Download / menu Settings | Format, folder permission, progress and retry | Unconfigured/cancelled folder, saving, failure, complete |
| S06 | Preview | Result Preview; official embedded playback | Audio/Video mode, visible IFrame, common mini controls | Loading, playing, paused, unavailable, foreground only |

## Flows and click goals

F01: launch -> S01 playable row -> audio starts (1 tap), mini title -> S04 (1 additional tap). Empty library -> Search (1 tap).
F02: Search -> enter query -> results -> Preview (submit + 1 tap). Recent query -> results (1 tap after opening Search). Offline/error keeps retry/input available.
F03: Library -> media filter/sort -> tap track -> S04. Playback is 1 row tap; overflow is a separate 48dp action.
F04: Download -> format -> choose folder once if needed -> completion automatically saves to that folder. Subsequent downloads need no Export step. Permission loss retries saving without re-downloading.
F05: mini -> full player -> pause/seek/next/queue -> close. Position and state must survive opening/closing the sheet. Background local playback stays native.
F06: Preview -> Audio/Video -> bottom play/seek/volume. The same IFrame survives mode switches; local playback and preview are exclusive.

Click counts for the reference were not measured from static store screenshots. Pocket Music click goals are explicit design targets, not claimed improvements over measured reference flows.

## Components / model

Track (local path, readable title, optional thumbnail/video ID, media type); Track preview (YouTube ID, title); PlaybackOrder (current queue/shuffle/repeat); RecentHistory (bounded MRU paths and search strings); DownloadFolder (persisted URI + safe-save mapping). These are Pocket Music's existing model plus bounded recent history, not inferred reference internals.

Repeated primitives: compact row, artwork tile, chip, ghost/primary action, navigation item, media transport, field, progress/error panel. Android roles, content descriptions, disabled/pressed/selected states and 48dp actions replace web semantics in the skill templates.

## Excluded / size

No account sync, proprietary recommendations/catalogue data, lyrics, artist biographies, music recognition, social listening, radio, payments, or reference branding/assets/copy. No private APIs or binary/source inspection. Existing official YouTube preview and downloader integration remain. Size: M for the complete quality pass; hardest parts are honest empty/offline home content, large-text/keyboard layouts, and retaining playback/folder behavior while changing screens.
