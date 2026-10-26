# Findings and fixes

- **V04-01 / S3, fixed:** On a light-system phone the new taste sheet inherited a white surface. Open Tune your music: text was unreadable. Force the application's intended dark Material mode and set the sheet background. Covered by taste interaction and final screenshots.
- **V04-02 / S2, fixed:** Destroy an Activity containing recycled album shelves: asking Glide to retrieve a new Activity request manager during detach threw `You cannot start a load for a destroyed activity`. Retain the original lifecycle request manager and use it only to clear recycled requests. The multi-Activity regression suite reproduces the teardown path.
- **V04-03 / compatibility, fixed:** Release lint found an API-27 theme attribute under minSdk26 and an incompatible custom Button base after adopting AppCompat. Remove the unnecessary attribute and extend AppCompatButton. No baseline suppression added.
- **V04-04 / S4, fixed:** Native playback used raw filename text even when music catalogue metadata was available. Use known title/artist; prefer higher-resolution album-art URLs for the expanded player.

To check on a phone: Bluetooth interruptions, lock-screen artwork on vendor ROMs, large fonts/compact displays, real signed-in downloads and battery use. Catalogue categorization can still contain unusual releases; Not for me is available. No claim of perfect music classification or Spotify-equivalent recommendation quality.
