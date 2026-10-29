# Smart downloads reference and scope

Public reference: https://support.google.com/youtubemusic/answer/6313535?co=GENIE.Platform%3DAndroid&hl=en
YouTube Music describes automatic music based on listening and background Wi-Fi updates. Pocket Music implements that interaction pattern with its own bounded catalogue/history selection, limits and Android framework. No upstream source, artwork, private endpoints or recommendation model copied. The opt-in settings and ownership-aware rotation reuse this application's downloader, library and native player. No new runtime dependency.

Architecture: JobScheduler → eligible device check → bounded candidate policy → sequential existing downloader → staged audio → atomic app-library commit → ownership ledger. Cancellation/account/eligibility checks run before commit. Clear/rotation validate exact file ownership. Manual download and Keep permanently remove smart ownership.

Verification: 58 JVM cases; Android regression 83 cases, 66 passed and 17 optional skipped in the previous complete run. Release lint zero errors. Focused Android ownership, changed-file protection, deletion exclusions, unavailable-candidate isolation, scheduled cancellation, persisted constraints and consent/settings tests passed. Final rerun recorded in TEST-RESULTS.md. An opt-in live transfer hit YouTube HTTP 429 and stopped safely; no successful live transfer or overnight/OEM certification is claimed.
