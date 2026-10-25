# Replica comparison and readiness

This is an incremental Pocket Music Android UI pass, not a complete Metrolist replica. Public screenshots were the reference; no reference code/assets/copy are included. Search and Library reference screenshots were not available in the published six-frame set, so no screenshot-parity score is assigned to those screens.

## Scoped functionality

Replica's feature tool reports 100/100 for **eight explicitly observed patterns**, six must-haves complete. See feature-score.md and features.csv. This deliberately small denominator covers compact music rows, mood shortcuts, artwork shelves, navigation and player controls. It does **not** mean 100% Metrolist parity. Personalized remote discovery, recognition, social listening, artist/lyrics pages, and catalogue ownership are outside the agreed pass. Native search history, library filters/order and automatic folder saving are additional Pocket Music requirements verified separately, not guesses that the original lacks them.

## Raw visual diagnostics

| Screen | Raw layout score /100 | Tool verdict |
|---|---:|---|
| Home S01 | 18.2 | different |
| Player S04 | 10.7 | different |

These scores are low and are retained unmodified. Both inputs are 1080x2400. The emulator uses 420dpi (about 411dp wide), selected from the reference's approximately 130px small artwork and a 48dp native target; reference density itself is not known. The reference is populated with photo album art, artists, and playlists; our reproducible screen test uses four temporary silent WAVs with original letter covers. imgdiff counts texture/text edge density as well as geometry. Consequently these diagnostics do not isolate pure layout and should not be combined into a claimed visual-parity percentage. No artificial normalization/masking was applied to improve them. The JPEG-encoded Home source was only converted to PNG for tool compatibility.

Manual findings that drove changes: remove the large top banner; place compact music rows and mood shortcuts first; use horizontal artwork shelves; replace repeated form-style track buttons with full-row actions; enlarge player artwork; remove heavy ghost-button fills; keep a separate primary transport and secondary controls. The result intentionally retains Pocket Music's blue dark-glass palette and native layout rather than the reference brand.

Own captures: clone-screens/S01.png, S02.png, S03.png and S04.png. Temporary fixture tracks are not shipped. External reference images remain local/ignored and are not part of the APK or public source.

## Behavior / readiness

Native QA covers row playback, filters, title sorting with matching Next order, recent history, background queue controls and focus handling. SAF tests and real MP3 download-to-folder integration still pass. The expanded player no longer shows underlying UI. Automatic folder saving and local/YouTube exclusivity remain.

Verdict: **ready for another beta test**, not a full visual replica or a stable device-certified release. No reproduced S1/S2 remains in exercised flows. The timing-sensitive background test observation is disclosed in bugs.md. Physical ARM phones, long battery sessions, real calls/Bluetooth, OEM background limits and other storage providers remain to check. Do not label the app stable from a scoped parity score.