# Native design and implementation

Keep Pocket Music's blue/dark colors, 4dp spacing scale, sans type and original fallback covers. Reuse Material Components 1.13.0 for BottomNavigationView, chips, cards, buttons, BottomSheetDialog and snackbar feedback. Use pinned Lucide assets converted directly from upstream SVG primitives with licenses retained. Glide 4.16.0 handles lifecycle requests, image deduplication and bounded 8MB resource / 4MB bitmap / 40MB disk caches. No continuously animated shader or blur background is added.

MusicCatalog is a metadata-only Java adapter around NewPipeExtractor v0.26.5's MUSIC_SONGS filter. HTTP uses normal TLS validation, bounded response size/timeouts and cancellation, without reading or sending Google account cookies. The existing yt-dlp connection remains responsible for authorized downloads and explicit general-video search. A music catalogue error never falls back to unfiltered video recommendations.

RecommendationPolicy is pure Java: require music provenance, valid IDs/duration and uniqueness, apply hidden IDs and artist-diversity caps. MusicTaste persists bounded on-device signals. Recommendations builds up to four real shelves from at most three explicit music searches per refresh, preserving cached music on failure. Versioned schema rejects legacy general-feed caches; account/profile changes discard late results. Do not invent content when sources fail.

MainActivity retains the existing app/player structure, with reusable view helpers and separate workers for metadata, artwork and file work. Search cache is split by Songs/Videos mode. Preserve player instance during streamed Next and manual Audio/Video switching. Local and preview queues retain their existing boundaries.

Validation: pure policy tests; synthetic profile/cache/cancellation and native UI/playback tests; opt-in real music-catalogue metadata test with no real account; phone-sized and compact captures; release lint/signatures/alignment; source and pinned dependency/license documentation published with APKs. Stable status requires more than emulator success.
