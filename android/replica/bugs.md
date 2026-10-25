# Replica QA findings

No reproduced S1/S2 app issue remains in the exercised flows. This statement is limited to the emulator/test plan; it is not a stable-release certificate.

## Resolved / reviewed

UI-01 (S3, existing beta 2): populated Home places a large collection banner before music, leaving key content below the fold. Reproduced in the stored beta 2 screenshot and the user's screenshots. Expected: quick access to tracks. Resolution: remove banner; compact picks/shelves and chips. Evidence: clone-screens/S01.png and ExperienceUiTest.

UI-02 (S4, existing): missing artwork uses the same oversized note in list/player. Expected: identifiable fallback without fake album art. Resolution: original deterministic cover artwork keyed by track title; no external assets.

QA-01 (test observation, not classified as a proven app bug): the fixed 900ms post-auto-next background assertion failed twice; the same test then passed in isolation and with the original class. There was no captured position/state evidence proving production pause. The fixture now waits for actual >500ms progress within a bounded timeout and waits for service destruction before cleanup. Final affected and release suites passed. This does not measure gaplessness or certify real-phone background timing; those remain to check.

TOOL-01: the public Home file is JPEG-encoded despite its .png extension. Replica imgdiff rejected it. It was decoded/re-encoded locally to PNG without altering visible content; reference never shipped. Low raw image-diff scores are retained rather than adjusted or hidden.

Review prevention: library title sorting initially affected only presentation. The new service queue uses the same sort policy; a native regression verifies Next under deliberately different timestamps. This was fixed during review, not reported as a separately reproduced production incident.

## To check on physical devices

Long screen-off sessions, real call/Bluetooth interruptions, OEM battery controls, removable/cloud folder failures, all supported Android versions, native libraries on 16KB-page devices, and extended preview queue transitions. No claim of gapless playback or personalized reference-app parity.