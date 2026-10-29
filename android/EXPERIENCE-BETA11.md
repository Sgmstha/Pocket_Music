# Android account and download experience — beta 11

## Account avatar

The header's circular account button shows the photo from the verified YouTube Music account menu. Cached initials are used when no photo is available/offline; guests see a generic account icon. Tap for the verified account name and connection controls. Existing complete beta 10 sessions can retrieve the photo without signing in again. Profile refreshes are bounded (normally once daily); image requests carry no session headers and share the app's small image cache.

Photo URLs accept only HTTPS Google profile-image hosts and remain in the Keystore-encrypted session bundle. Optional metadata keeps existing session payloads readable. Conditional profile writes use a cross-process lock so an old response cannot overwrite a newly connected account. Account routing scopes now depend on credentials/account/channel/visitor, excluding name/photo metadata; changing a photo therefore does not discard playing/prepared tracks or Home caches. The digest cache is bounded, and cached profile changes still refresh the header. Old pre-update Home caches may refresh once after installing.

## Home recovery

Account recommendations remain account-backed. Direct shelves survive failures in optional mixes. Supported Music radio cards expand through the Music next endpoint with audio/persistent-panel request fields; primary songs are retained without also adding video counterparts. Immersive shelves, overlay music type, playlist-panel tracks and Music artist/album metadata are recognized. General videos/podcasts remain excluded by explicit type or required Music provenance.

When a response omits the logged-out flag, the account identity is checked instead of treating omission as expiry. Explicitly logged-out/error responses still require reconnection. If no supported Home songs are available, a bounded account-likes request can populate a shelf labeled Your YouTube Music likes; it is not represented as a new recommendation model. Generic search replacements are not used for connected accounts. Requests remain bounded; authentication, offline and rate-limit errors can still prevent refreshes.

## Download manager and notifications

More → Downloads and notification taps open a full-height manager. All / Active / Attention / Finished filters separate work, errors and history. Each row shows artwork, format and phase; progress, speed and ETA appear only when available. Converting/finishing and folder saving stay distinct from completed downloads. Tap Save folder to choose/change the existing Android folder permission.

Cancel a job/all pending jobs, retry interrupted/failed work, retry saving without downloading again, play a local audio copy, or open Library for video files. Clear history removes only non-pending entries, never saved files. Two-worker and same-video serialization behavior is unchanged.

The ongoing notification shows current work and determinate zero-percent progress correctly; unknown progress uses an indeterminate bar. Expanded text lists pending tracks. View downloads and Cancel all operate the existing queue. A batch result notification remains after work stops, with Retry for attention or Open library and (single audio result) Play. Notifications respect Android permission/channel settings. Taps route into an already-running Activity instead of losing the destination. Completion/folder-save errors are not presented as successful exported files.

## Verification

Source references for protocol compatibility: https://github.com/sigma67/ytmusicapi/blob/main/ytmusicapi/mixins/watch.py and https://github.com/sigma67/ytmusicapi/blob/main/ytmusicapi/parsers/watch.py . Native notification APIs are used; no new runtime dependencies.

Final results are in TEST-RESULTS.md. Synthetic fixtures exercise account feeds/avatar metadata and the real Android UI, notification manager/actions, local playback and a real save-only foreground-service retry. The notification Play action starts actual saved audio; clear-history leaves the test audio file intact. Screenshots of the synthetic signed-in header and download states were inspected. No real account credentials or private images are used. Live personalized-feed accuracy, real photo retrieval and live YouTube transfer/OEM behavior still need phone verification. Test-signed prerelease only.
