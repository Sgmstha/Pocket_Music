# Android v0.4 music discovery

Recommendations and default Search now use NewPipeExtractor's YouTube Music **Songs** catalogue. General YouTube feed entries and legacy cached video recommendations are not eligible. This is a catalogue boundary, not an infallible semantic classifier: incorrectly categorized songs, spoken-word releases or unusual music uploads may still occur. Use **Not for me** to hide a result; Undo restores it. Choose **Videos** explicitly to search general videos. Direct YouTube links remain supported.

Home offers an artwork mix, quick picks, liked songs, up to four discovery shelves, recent listening and downloaded music. Actual results determine which shelves appear. Artist and genre preferences, likes and music listening guide bounded song searches; refresh rotates the available signals. Reasons show the search source. This is on-device personalization, not Spotify's proprietary recommender or synchronization of your Google listening history/library. Google sign-in remains for the existing download session; catalogue metadata requests do not receive account cookies.

**Tune your music** in the header accepts up to five artists and four genres. Like a catalogue song or saved local file to keep it in Liked songs. Only known music-catalogue tracks contribute music recommendation metadata; arbitrary imported files/videos do not silently become online recommendation signals. Local favorites are still playable.

Each refresh uses at most three song searches. Cached shelves last three hours, filter hidden items immediately and survive temporary network failure. Artist caps and ID deduplication improve variety. Likes/preferences invalidate stale recommendations; feedback is debounced. Failed requests preserve available cached music and show a retry state, with a cooldown for rate limits. There is no fallback to the general YouTube video feed. No continuous discovery polling or artwork animation is added.

All-local collections use an explicit native queue, preserving the collection order and accepting library files only. Online/mixed collections use the existing visible official YouTube preview queue; native-local and preview queues remain separate. Native saved music retains background playback, media controls, sleep timer and widget. Official previews retain foreground/embedding restrictions. Downloads, session encryption, folder saving and per-track progress remain separate.

Material Components supplies reusable navigation, chips, cards and bottom sheets. RecyclerView recycles album-shelf views. Glide bounds artwork memory/disk caches and clears recycled views. Lucide supplies consistent licensed icons. These are integrated into the existing Java app; Metrolist's incompatible Compose UI and branding were not copied. See THIRD-PARTY-NOTICES.md.

## Release status

v0.4.0-beta.1 (versionCode 8) uses the existing beta signing identity so it can update beta 5. Emulator regression tests, live anonymous music searches, lint and signed release packaging are checked. Real Google login/downloads were not repeated with a private account during this pass. Physical phone background behavior, Bluetooth/headset controls, battery use and unusual screen/font settings still need user testing; this is a prerelease, not a stable-quality claim.
