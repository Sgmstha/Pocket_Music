# Replica playback refinement — Android beta 5

Scope: Pocket Music's existing Android online-audio player, especially queued transitions. Preserve the native service, Java structure, local playback and existing UI. Study public functionality; write the implementation here. No Metrolist source, private endpoints, account traffic, artwork or assets were copied.

| Public reference | Observed evidence | Boundary |
| --- | --- | --- |
| https://github.com/MetrolistGroup/Metrolist | Product README lists music streaming, background playback, queue and caching/offline functionality | Does not establish its exact preloading algorithm or startup timings |
| https://developer.android.com/media/media3/exoplayer/preloading-media/preloadmanager | Media3 provides preparation ahead of playback | Official supported library API, already pinned in Pocket Music |
| https://developer.android.com/media/media3/exoplayer/preloading-media/preloadmanager/create | Shared preload/player builder and buffer budget | Configure one upcoming item and one second, rather than a large carousel |
| https://developer.android.com/media/media3/exoplayer/preloading-media/preloadmanager/manage-play | Hand prepared MediaSource to the player; remove/release unused items | Preserve ownership while the player is using the source |

P01 remains the current full player and mini-player: preparing, playing, buffering, paused, ended and error. No new screen is needed. F-P01: play a song → allow current playback to settle → resolve one upcoming queue song on an eligible connection → load about one second into memory → Next/automatic completion consumes that prepared source in the same engine. F-P02: pause, Stop, saved-file switch or queue changes cancel obsolete preparation; stale callbacks cannot activate another song.

The eight-entry signed-source cache remains account/network scoped and short lived. Upcoming work does not launch yt-dlp, update an engine, lease account cookies, create a downloaded file or retry failures repeatedly. It is skipped on metered connections, Data Saver and battery saver. Current shuffle/repeat/manual queue order determines the upcoming item without advancing the queue. A current song can join an already running preparation for itself; it does not start a second extraction. Normal resolution remains the fallback.

Implementation: reuse the service's online AudioPlayer; reset callbacks and media state between selections. Build Media3's DefaultPreloadManager and ExoPlayer from the same builder, with a 512 KiB preload target and one-second duration. Retain the existing 500 ms playback startup and 3–15 second forward-buffer thresholds. Release preloaded content on cancellation and the manager on service destruction. Saved files retain MediaPlayer.

First uncached play still needs a YouTube lookup and network preparation. This change targets prepared queued tracks; it does not promise instant startup for every search result or gapless playback. No physical-device equivalence to Metrolist is claimed. Size: small bounded playback change; the hard parts are cancellation, source ownership, and stale account/network/queue state.

Verification will record real HTTP prepared-source reuse, paused/cancelled preparation, policy exclusion, queue peeking, native controls, and public playback timings before release. Timing results belong in STARTUP-PERFORMANCE.md, with test limits stated explicitly.

During verification, real public HTTP streams exposed that a current request can remain loading indefinitely. The default preload control waits for idle loading; the Pocket Music wrapper permits the single short preload when current playback holds a three-second reserve, and yields when the reserve drops or rebuffering begins. Completion is best effort; Next uses a resolved source without waiting for optional audio buffering. A failed warm extraction is not redundantly repeated before authenticated fallback on explicit playback.
