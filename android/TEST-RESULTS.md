# Validated Music connection — beta 10 verification

VersionCode 17. Final release/debug builds and 69 JVM tests passed. Release lint: 0 errors, 70 warnings. Full API 36.1 x86_64 Android suite: 101 cases, 84 passed, 17 optional skipped, no failures, 88.846 seconds.

Seven new runtime cases cover real WebView Music configuration and cookie capture (including HttpOnly cookies), actual Keystore-encrypted save/read-back and same-context Home requests, rejected verification, cancellation after verification, legacy cookie migration/lease compatibility, foreign-origin capture rejection, and refusal to save an unverified identity. Network account/feed responses are mocked with synthetic fixtures; no Google credentials were entered. Five new JVM cases cover account/channel normalization and configuration trust/validation. The separate-process UI test covers the revised automatic-connection controls, Home/return and Cancel preserving the old saved account. An initial focused run exposed only a stale Reload-label assertion; it was updated for Choose account and the final complete suite passed.

REA CLI 6.8.0 observed Metrolist v13.7.0's installed package and anonymous Google login entry. Static source comparison and wire-format implementation are separate from REA runtime evidence. No complete Metrolist account login, real Google 2FA, or Pocket Music live account validation is claimed. Reference identities, exact Evidence IDs, limitations and source revisions are recorded in replica/metrolist/login-beta10.md. Physical ARM sign-in, secondary/channel behavior and Google's embedded-browser acceptance still need phone validation. Test-signed prerelease only. Existing Android signing identity and APK alignment are verified during release packaging.

Earlier reports follow.

# Login recovery — beta 9 verification

VersionCode 16. Final release/debug builds and 64 JVM tests passed. Release lint: 0 errors, 69 warnings. Final Android API 36.1 x86_64 suite: 94 cases, 77 passed, 17 optional skipped, zero failures, 81.774 seconds. Both ARM APK signatures and ZIP alignment verified; existing signing identity retained.

Five new JVM cases cover valid Google/YouTube handoff and consent destinations, malicious/unsupported redirects, session/signing readiness, load-independent capture eligibility and invalid/empty/injected cookies. The real separate-process login UI test confirms Save starts disabled despite an existing encrypted account, recovery controls exist, the same task returns from Home, Open YouTube works and Cancel preserves the existing saved account. Recents-style task restoration uses test-only shell permissions; no extra permission is added to the app. Fixtures contain only synthetic session data.

Earlier attempts found a fresh-install notification prompt covering an existing Home test and a case-sensitive login-button assertion after Android transformed its label. Permission setup and case handling were corrected; the final full suite passed. No production change was made to bypass those test issues.

A real Google password/two-step challenge was not entered on the emulator. Actual post-2FA session transfer, Google embedded-browser acceptance, secondary/brand accounts and physical device behavior still need validation. No real two-step success, universal Google-login compatibility or stable certification is claimed. See LOGIN-RECOVERY.md.

Earlier reports follow.

# Account recommendations — beta 8 verification

VersionCode 15. Final release/debug builds and 59 JVM tests passed. Release lint: 0 errors, 66 warnings. Final Android API 36.1 x86_64 regression: 93 cases, 76 passed, 17 opt-in skipped, no failures, 89.725 seconds. Both ARM APKs passed signature and ZIP-alignment checks; signing certificate unchanged.

Nine new account metadata/auth tests verify connected-source routing without search fallback, failed-refresh cache retention, in-flight account switching, hidden/duplicate filtering, music-only renderer parsing, signed-out/ambiguous rejection, YouTube-domain cookie filtering, web-client configuration/auth digest and legacy-cache invalidation. A new real Activity UI test verifies account shelf/title presentation and cached picks plus reconnect status after refresh failure. Its screenshot uses synthetic fixtures and was visually inspected. A new JVM case verifies account songs with unknown duration can enter Smart selection; the downloader enforces 30–600 seconds before saving.

The initial full run found two stale UI-test expectations for the old Your daily mix label. Tests were updated for Local music mix, then the final full suite passed. Public YouTube Music bootstrap returned HTTP 200 and current client/visitor configuration was present. No real signed-in account is available in the test emulator: authenticated feed content/accuracy, secondary/brand identity and physical ARM/OEM behavior are unverified. This remains a test-signed prerelease. No live personalized-feed success or full web-client parity is claimed.

Earlier reports follow.

# Smart downloads — beta 7 verification

VersionCode 14, same signing identity. Debug/release APK builds, 58 JVM tests and release lint passed (0 errors, 66 warnings). Android API 36.1 x86_64 regression: 83 cases, 66 passed, 17 opt-in skipped. New cases cover ownership/rotation/manual-file safety, Keep permanently, deletion exclusions, modified-file safety, lost eligibility, real scheduled-job cancellation before commit, unavailable candidates, persisted job constraints and settings consent/enable/disable. The settings dialog was visually inspected on the emulator.

An additional opt-in real download was attempted: YouTube returned HTTP 429 on the account-free emulator; the batch stopped and respected cooldown. This is not a successful live-transfer verification. Physical ARM phones, an existing signed-in account, overnight scheduling, reboot and OEM battery policies remain user/device validation. Beta prerelease, not stable certification. APK signing and alignment verified during release packaging.

Earlier version reports follow; their limitations describe those versions, not subsequent fixes.

# Android beta 3 — final feature verification

Version 0.3.0-beta.3, versionCode 5. Android 8+, compile/target 36, same package and test signing key. Windows unchanged.

- Final Gradle debug/release/instrumentation builds and **28 unit tests** passed; release lint has **0 errors, 28 warnings** (mostly UI text and widget/dependency advisories).
- **12 affected native runtime cases passed in 32.091s**: three new phone-feature cases, two UI journeys, three queue/order/focus cases, conversion/background playback and three history cases. Sleep expiry paused actual native audio after Home; turning the timer off left playback running. Widget provider discovery, RemoteViews inflation and real Play/Pause commands passed.
- **MultiDownloadIntegrationTest passed in 46.395s**. It enqueued the documented Creative Commons sample YE7VzlLtp-4 and a deliberately unavailable video, observed two running jobs and actual progress callbacks, confirmed the unavailable job failed without blocking the successful MP3, and checked the automatic external-folder copy's byte count. Artifacts were removed. Same-video formats are separately covered by queue unit tests.
- Unit tests cover parallel limit, duplicate requests, independent cancellation, failed-job isolation, save-only retry, idle completion and search suggestion ordering/bounds. Existing library/queue/error/link tests remain.
- The actual emulator launcher pin dialog was accepted and the widget rendered on Home. Its rendered title, Play/Pause and Next controls were present. Native instrumented controls operate the existing service.
- Earlier Replica phase checks in this same build cycle covered real SAF access/retry/collision safety, a non-debuggable native screen journey, ~411dp and 360x640dp with 1.3 fonts, preview play and a visible 200x200dp official Audio player. Phase reports/raw comparison data remain under replica/; they are not a full Metrolist parity claim.
- ARM64/ARMv7 delivery signatures and ZIP alignment are verified at packaging. Certificate SHA-256 remains e57c3f7d6338c169e0f3da576689da5cba191ed63051e7e2b6d8d0b142ccd5ed.

## Limits

Still test-signed beta. Physical ARM/OEM battery policies, long screen-off/battery sessions, real calls/Bluetooth, launcher variants, cloud/removable providers and 16KB-page library compatibility require device testing. Two parallel conversions can use more CPU than a single job. Widgets control saved audio; official YouTube previews pause outside the foreground. Recommendations are real cached search results, not account-personalized feeds. Interrupted jobs require explicit Retry.

The early fixed-900ms background assertion observation and low raw image-comparison scores are disclosed in replica/bugs.md and replica/parity.md. Later bounded-progress suites passed. No gapless, exact visual replica, or stable-device certification is claimed.