# v0.4.0-beta.2 verification

- 50 JVM tests passed; unchanged preview JavaScript contract passed.
- Final direct Android instrumentation: 55 cases, 46 passed, 9 opt-in cases skipped, zero failures. The new real SAF deletion case passed. Skips include unrelated live network downloads/catalogue calls and legacy provider-fixture cases; no new authenticated download claim.
- Library navigation/filter/sort, local search, full player navigation, delete Cancel, delete the playing file, native queue/background/timer/widget and encrypted-session regressions passed.
- Local deletion checks reject files outside the library and unknown extensions; preserve online catalogue likes; clean stale local history. The real system-picker test confirms default app-only removal keeps the saved document and explicit folder deletion removes only the tracked copy, leaving a previous same-name document intact.
- Initial test-host cold-boot System UI/launcher ANRs obscured the app. Hardware graphics and running compiled instrumentation without a resident Gradle daemon resolved host memory pressure. These interrupted runs are not counted as passes.
- A new negative test exposed the existing audio helper's broad definition (anything except MP4). The deletion boundary now uses an explicit supported-media whitelist; the final test passed. The folder test was adjusted for Android's provider returning IllegalArgumentException for a deleted document, rather than FileNotFoundException.
- Release lint: zero errors, 58 warnings; no lint baseline suppression. Signed ARM64/ARMv7 APKs retain the existing beta certificate, with apksigner/zipalign validation.
- Final native library/player screenshots reviewed. One synthetic silent track demonstrates layout; fixtures are test-only. Final copy edit corrects the singular track count. No claim of pixel equivalence to the supplied reference brands or of physical low-end battery/CPU measurements.

Physical-phone checklist: update without uninstalling; search/sort a real library; open/close player while music continues; seek/pause/next/shuffle/repeat and Sound volume; delete a nonplaying song, then a playing song; verify unchecked saved-folder copy remains and explicitly selected saved copy is deleted; test locked-screen/headset controls. Version remains a prerelease pending phone testing.
