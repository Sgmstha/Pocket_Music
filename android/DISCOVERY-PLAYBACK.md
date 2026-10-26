# Discovery and playback — Android 0.3.0-beta.5

This is a native Java update to Pocket Music, using Replica's public Metrolist UX references. It retains Pocket Music's dark blue theme, account connection, download queue, selected-folder saving, local MediaSession and visible official YouTube IFrame. No Metrolist implementation, assets or private endpoints are copied.

## Home and recommendations

Signed-in users get **Your YouTube picks** near the top of Home. The existing yt-dlp integration requests a metadata-only recommended feed, capped at 12 items, using a separate temporary session lease. Results are cached for an hour; Refresh requests an update. Rows offer Play, separate Download, and song options. Browse all opens the remaining picks in Search.

This is the general YouTube account feed, **not an equivalent of YouTube Music's music-only personalized catalogue**. It may include non-music videos. Your existing YouTube activity shapes that feed. Pocket Music does not upload a listening profile, sync account playlists, or claim that its separate preview WebView is signed into the account.

Without an account, discovery uses the most recent search or played title as an explicitly displayed search seed. These public results are cached for six hours. Saved music comes first on guest Home. Empty, loading, stale/offline and reconnect states are explicit; cached tracks remain available when refresh fails. Search uses a compact horizontal discovery shelf.

Caches are matched to the encrypted session's fingerprint. Reconnecting or disconnecting clears the old account feed on return to the app. Late results from a prior session are discarded. The metadata cache is private application preference data, excluded from Android backup by the existing manifest. It contains titles and IDs, not session cookies. Account requests are paced, bounded and never polled in the background. A 429 applies a minimum one-minute feed cooldown.

## Search

Local/history suggestions stay instant and offline; typing does not trigger online requests. A submitted query also shows matching saved audio. Repeated online queries use an eight-query in-memory LRU cache with a 30-minute expiry; Refresh bypasses it. Empty/failing responses are not cached. New queries can replace in-flight searches. Each request has its own process name, cancellation and a generation guard so a late error/result cannot replace the latest query. Search has its own worker, independent of import/file work and artwork requests. Artist/channel and duration appear when metadata is available; duplicate/private/deleted entries are omitted.

## Playback and queue

Tapping a saved result uses the local audio player, including background playback. **Watch YouTube preview** remains in song options if you explicitly want the video. Unsaved results use the existing visible IFrame. Audio/Video mode keeps the same player. Streamed Next/queue selection now uses loadVideoById in the same WebView, without recreating the renderer; stale status from the previous video is ignored. Preview errors clear the playing state and Play can retry the player.

Song options offer **Play next** and **Add to queue**. Local-file options have the same actions. Queue items can be selected, moved next or removed, without restarting the current song; removing the current song is prevented. Queue selection preserves manual order instead of rebuilding the library queue. Native MediaSession publishes the traversal order. Shuffle/repeat, seek, volume, media controls, sleep timer and widget remain.

Local and preview queues remain separate. While local audio is playing, an unsaved online song must be played first to start a preview queue. While a preview queue is active, its queued tracks continue as visible previews. No hidden YouTube background playback, autoplay catalogue expansion, crossfade, lyrics or artist/album pages are added.

## Test status

43 unit tests pass, covering bounded/expiring search caches and queue policies as well as existing behavior. Android emulator tests cover account-cache isolation, parsing, replacing an in-flight query, cache hits without requests, native queue mutations without losing paused position, local MediaSession/background playback, timer/widget controls, encrypted sessions and synthetic screen captures. The preview JavaScript contract verifies a single player, loadVideoById reuse, stale-video filtering, transport and error recovery without contacting YouTube.

Release lint has zero errors (existing warnings remain). APKs retain beta 4's signing identity and increment versionCode to 7. Install over beta 4 without uninstalling. Live personalized-feed output and long sessions on your physical phone remain to be checked; this is a test-signed beta. Replica's scoped assessment and test evidence are in replica/metrolist/.
