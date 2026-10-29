# Smart downloads — Android beta 7

Enable in More → Smart downloads (also available from Library and Settings). Confirm you own or have permission to download the selected content. Off by default.

Select 10/25/50 songs and 100/250/500 MiB; defaults are 25 songs/250 MiB. Charging is required by default and can be disabled. Refresh now schedules the same constrained job; it does not override its safeguards.

Updates require validated unmetered internet, no Data Saver or Battery Saver, battery at least 40%, sufficient free storage, and no active player or manual download. A persisted Android JobScheduler job requests an update roughly daily, plus one after enabling/refreshing. Android/OEM power policies can postpone it. Each refresh attempts at most five new songs, sequentially. Unavailable songs are skipped; sign-in restrictions and rate limits stop the batch.

Suggestions interleave recent listening, likes and music recommendations, excluding hidden/deleted/existing songs and clips outside 30 seconds–10 minutes. Cached recommendations are preferred; a stale cache allows one bounded Songs query. This uses Pocket Music's existing catalogue and encrypted account session, not Google's recommendation model or private APIs.

Smart copies are audio-only M4A/AAC in app-managed music storage, playable through the existing offline player. They are not automatically exported to your selected folder because automatic rotation must not remove your personal files. Use Keep permanently to remove a song from rotation and attempt a copy to your selected folder. Ordinary manual downloads retain their existing folder behavior.

Only files matching the app's ownership ledger can be rotated or cleared. Manual, imported, kept and exported files are protected. Current and queued songs are protected too, so lowering limits can temporarily retain extra copies until playback/queue stops. A manually deleted smart song is excluded from future refreshes. Disabling keeps downloaded copies; Clear smart downloads removes only eligible app-managed smart files. Uninstalling can remove app-managed copies; keep/export favorites first.

This remains a test build. Real daily/overnight scheduling, physical ARM devices and manufacturer battery policies need device validation. The account-free emulator's live transfer was blocked by YouTube HTTP 429; successful live Smart transfer is not claimed. Existing sign-in and server availability still affect downloads.
