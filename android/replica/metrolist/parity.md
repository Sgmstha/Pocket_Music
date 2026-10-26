# Scoped Replica assessment

Feature score: **93.2 / 100** for the eight explicitly scoped behaviors, using the pack's parity.py. Five of six must-haves are complete; account-based picks remain partial pending a real signed-in feed check on the user's phone. This is not a score for the entire Metrolist product.

Home, Search and player captures are in clone-screens/. They use fictional titles and a synthetic account session. The Home hierarchy follows compact picks followed by saved listening; Search uses direct local matches plus compact metadata rows; the existing full player retains clear artwork and transport controls. Pocket Music's dark blue tokens and original fallback covers remain. Fifteen design-token contrast pairs pass AA.

No numeric visual-equivalence claim: the public reference is 1080×2400 with actual catalogue artwork, while this pass uses 1080×2280 synthetic emulator captures. Different data/viewport make a pixel/layout score unsuitable. The images were reviewed for hierarchy, legibility, clipping and navigation, not for copying the target's appearance.

Behavior differences retained: general YouTube account picks may include non-music videos; guest discovery uses an explicit listening/search seed; download is a separate action; visible official previews remain foreground-only; native audio handles background listening. Preview/local queues remain distinct. These limits are visible/documented, not represented as full YouTube Music parity.

Verification: 43 unit tests; 23 distinct Android instrumentation tests across the full and targeted runs, including preview Java-controller reuse/error state; preview JavaScript contract; release build/lint with zero errors. The initial full test run had environment/precondition failures which were corrected; final full and affected targeted runs pass. The Home saved-song index found during visual review is covered by the final capture assertion.

Verdict: **test beta for physical-device evaluation; not a stable release or complete Metrolist clone**. Account-feed relevance/authentication on the user's phone remains the outstanding end-to-end check.

Future work, outside this pass: music-only catalogue recommendations through an appropriate integration; artist/album/playlist navigation; unified local/online queue design; persistence/resume across process death; phone performance and Bluetooth/audio-focus validation.
