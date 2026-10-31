# Automatic offline collection — beta 12

Home no longer displays the persistent download-status banner, Retry download or Details buttons. Manual transfer progress, retry/save actions and errors remain in Downloads and notifications. Downloads also shows Smart download progress/status and opens its settings when tapped.

Enable Smart downloads once from Downloads or the More menu, confirm download permission, and save settings. Choose a song/storage limit and optional charging requirement. New settings default to charging optional; existing explicit preferences are preserved. Work requires a validated unmetered connection, battery at least 20%, playback/manual downloads idle and free-space reserves. Android determines background execution timing; force-stopping the app or OEM battery restrictions can postpone it.

Smart downloads now refreshes through the same account-backed music feed as Home. A connected account no longer needs listening history on this phone. Failed account requests never substitute generic search results. Cached candidates/local likes/history can keep an existing collection useful. With no account or personal signals, setup asks for a connection or likes instead of downloading random music.

The first constrained fill is scheduled on enable. Opening the app requests a missing fill at most every 15 minutes. Interrupted/blocked work retries with Android backoff, starting at 15 minutes. Small five-attempt/four-minute batches continue automatically while songs remain and the selected limits allow. Daily refresh stays persisted across reboot. Stable candidates prevent daily-rotation math from deleting the previous batch during a multi-batch fill. The requested song count is an upper limit; fewer eligible account songs, unavailable transfers or storage limits can yield fewer copies.

Automatic copies stay app-owned in Library; manual downloads and permanent copies are never rotated. Removing an automatic song excludes it from future fills. Existing download format, folder saving, account encryption and signing identity are unchanged.

Verification uses synthetic account/feed/download fixtures and actual JobScheduler/UI tests. Live authenticated downloads and physical-phone background/OEM behavior require user validation; this is a prerelease.
