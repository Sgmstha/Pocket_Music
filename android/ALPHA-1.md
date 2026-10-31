# Pocket Music 0.5.0 Alpha 1

The first named alpha brings the existing account-backed music, native background playback, offline library and download manager together in a regular GitHub release. Alpha describes product maturity; it does not certify production stability. Existing Android signing identity, package and private data locations remain unchanged. Install over the previous beta without uninstalling.

## Refined everyday flows

- Home stays focused on account music. Download progress/errors/actions live in Downloads and system notifications.
- Library adds a Smart collection with its own empty state and a direct management action. Automatic copies can be browsed and played separately from manual audio/video downloads.
- Library search uses a per-screen file snapshot and a 180 ms typing debounce. Directory enumeration is no longer repeated on every character. Returning to Library or completing a manual/Smart download refreshes the snapshot.
- Full-screen playback explicitly shows preparing, buffering and paused states. Its Timer action displays remaining sleep-timer minutes. Native background audio, local-first playback, seek, media controls, queue and widget continue using the existing service.
- Smart download settings explain the current waiting condition: internet/unmetered Wi-Fi, Battery Saver, playback/manual downloads, battery level, optional charging or storage.
- Unavailable Smart download candidates receive a one-hour retry delay. Subsequent batches can reach later songs instead of retrying the same first five failures forever. Expired retry records are pruned; successful candidates clear their delay.

## Automatic offline setup

Open More → Downloads → Smart downloads. Enable it, confirm download permission, select song/storage limits and Save settings. Connect YouTube Music for account-based picks. Charging is optional; existing preferences are preserved. Automatic copies remain app-owned and can rotate. Keep permanently protects a song; manual downloads and selected-folder copies are never rotated.

Android controls job execution time. Smart work uses unmetered validated internet, at least 20% battery, storage reserves and idle playback/manual downloads. Force-stop, power saving and manufacturer background restrictions may postpone it. Counts are upper limits, depending on eligible available music.

## Release scope and limitations

Android-only source changes. Both ARM64 and ARMv7 APKs and SHA-256 checksums are provided. This alpha is published as a regular GitHub release as requested. It retains the existing test signing identity to allow upgrades; no signing keys or account data are published.

Automated coverage uses synthetic account/feed/transfer fixtures. Physical ARM phone behavior, real Google sign-in/account recommendations, live YouTube availability/rate limits and OEM background scheduling require continuing device validation. Sign-in cannot guarantee every video can be downloaded. No CPU benchmark or zero-delay playback claim is made.
