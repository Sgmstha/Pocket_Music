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