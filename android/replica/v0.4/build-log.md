# Final verification — 2026-10-26

- Release assemble and lint: successful; lint reports 0 errors, 54 warnings. No new lint baseline/suppression.
- JVM unit tests: 50 passed.
- Full API36.1 emulator instrumentation: Gradle reports 57 completed, 9 opt-in cases skipped, zero failures (initial discovery estimate was 48). Captures enabled. Skips include folder-provider fixtures, live downloads and live catalogue flags; these are not silently counted as passes.
- Separate opt-in live catalogue + final screen run: 3 passed, zero skipped/failures. Verifies actual anonymous Songs metadata and a three-source, multi-shelf recommendation pipeline; captures use real public catalogue metadata/artwork with generated silent local audio, not downloaded songs.
- JavaScript preview-player contract: passed; reuse, stale events, seek/volume/transport, error retry and video-ID validation.
- Both ARM64 and ARMv7 release APKs: apksigner verification and zipalign checks passed. Certificate SHA256 e57c3f7d6338c169e0f3da576689da5cba191ed63051e7e2b6d8d0b142ccd5ed matches beta 5.
- APK bytes: ARM64 61,018,036; ARMv7 54,365,220. No claim of unchanged binary size: reusable catalogue/UI dependencies add size.
- Final screenshots reviewed: home, search, liked collection, taste sheet and native player; dark Material surfaces work on a light-system emulator. Album shelf teardown crash and theme contrast issue fixed before final pass.
- Replica token contrast report: contrast.json. Feature-matrix score is scoped to the requested subset, not a claim of full Metrolist/Spotify parity. Viewports/data differ from public references; no misleading numerical screenshot similarity is reported. Familiar music hierarchy is retained with Pocket Music's own dark-blue palette.

No private account was used; authenticated downloads/login were not repeated. No physical-phone/Bluetooth/battery benchmark. Release remains v0.4.0-beta.1 with existing beta signing.
