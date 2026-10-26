# Implementation and design

Reuse existing DesignTokens/GlassTheme, native rows, 48dp controls, 132dp artwork shelves and Home/Search/Library tabs. Add a prominent compact account shelf before local Home content and show its source and freshness. Cached content remains usable offline. All text and artwork use Pocket Music's existing tokens and original fallback covers.

Recommendations owns source-aware bounded metadata caching. Account requests use the existing yt-dlp recommended extractor and request-specific encrypted-session leases; caches are matched to the encrypted session fingerprint. Account changes discard late results and invalidate account metadata. Public recommendations remain distinctly labeled, driven by explicit listening/search history.

Search uses an independent single-worker executor, explicit submit, a small TTL/LRU result cache, unique yt-dlp process names and generation checks. Local typeahead performs no network requests. Cancellation destroys only the superseded process. Refresh bypasses cache. All results are deduplicated and bounded.

PlaybackOrder gains append, play-next, select and removal without resetting current position. LocalPlayerService publishes queue changes to MediaSession without reopening the current track. Visible previews reuse their existing IFrame instance through loadVideoById; Audio/Video mode persists. Playback provenance/artist/duration are visible where known; missing metadata is not invented.

No new runtime dependencies. Test pure policies with JUnit; session scoping, parser/cache and native queue mutations with isolated synthetic Android tests. Document reference differences honestly rather than claiming pixel equivalence or music-only account recommendations.
