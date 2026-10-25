# Incremental architecture

Native Java Views remain. No Compose/web rewrite or new renderer. Existing LocalPlayerService, PlaybackOrder, MusicRepository and DownloadFolder remain the real data/playback/storage layer. Implement S01 -> S03 -> S02 -> S04, then recheck S05/S06.

Design tokens live in replica/design/tokens.json and map to DesignTokens.java; Android values are dp/sp, not CSS pixels. CSS/Tailwind/Storybook/Playwright outputs from the web-oriented skill are inapplicable: native token primitives, emulator screenshots, instrumentation and Gradle lint are the equivalents.

RecentHistory stores bounded local paths/searches in SharedPreferences. It performs no network scans or background polling. UI filters and sort are in-memory over the existing local library. Artwork remains loaded off the UI thread. No fake production catalogue or seeded user history is shipped.

Reference screenshots are ignored by Git and never packaged. Scoped feature scores are not full Metrolist parity. Visual checks cover only public reference screens with evidence, at the same screenshot dimensions; differences in music/artwork/data are disclosed.
