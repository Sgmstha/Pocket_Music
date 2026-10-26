# v0.4 Android verification

Tests target Pocket Music, using synthetic local files and isolated preference snapshots. No real Google session is used. Native instrumentation replaces browser Playwright because this is an Android Java UI.

| Flow | Cases and evidence |
| --- | --- |
| F01 discovery | Typed Songs only, legacy cache rejection, hidden-ID filtering, duplicate/artist caps, liked metadata retention, account/profile cache scope, partial/all-source failures, offline cache, request cooldown. RecommendationPolicyTest / DiscoveryMetadataTest. |
| F02 taste | Save artist/genre preferences, limits, signal rotation, liked collection, hide and Undo. MusicExperienceTest. |
| F03 search | Songs/Videos cache isolation, replacement queries, cached search, malformed links, empty/history/local cases. MusicExperienceTest / SearchExperienceTest / VideoLinksTest. |
| F04 playback | Native background playback and MediaSession, pause/seek/volume, repeat/shuffle/focus, queue edits and explicit collection restrictions. PlaybackTest / QueuePlaybackTest / QueueEditingTest. |
| F05 previews | Reuse one official player, mode/track switching, stale-event rejection, error retry and transport contract. PreviewReuseTest plus preview-player.test.cjs. |
| F06 phone | Home/Search/Library separation, mini/full player, sleep timer, widget provider, folder and encrypted-session isolation. ExperienceUiTest / PhoneFeaturesTest / DownloadFolderTest / YouTubeSessionTest. |
| F07 visual | Home, Search, Taste, Liked and full player captures; dark surfaces on a light-system emulator. V04ScreensTest. Palette contrast checked with Replica contrast.py. |
| F08 live catalogue | Opt-in anonymous single Songs search and three-source recommendation pipeline. LiveMusicCatalogTest. Ordinary bounded metadata requests only. |

API 36.1 Pixel 4 emulator used; unit tests and release lint supplement instrumentation. APKs are signed with the existing beta certificate and checked with apksigner/zipalign. Full results are recorded in build-log.md after the final pass.

Physical-phone checklist: install over beta 5; confirm library/session retained; save taste and like songs; inspect several refreshes for relevance; search Songs versus Videos; download an authorized song to chosen folder; test offline saved music, locked-screen playback, media buttons/Bluetooth, timer/widget and font scaling. Real private-account login/download and battery/low-end CPU benchmarks are not claimed by the emulator suite.
