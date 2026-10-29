# Validated account connection — Metrolist comparison

## Reference and evidence

Reference APK: Metrolist v13.7.0, package com.metrolist.music, versionCode 153. Official asset: https://github.com/MetrolistGroup/Metrolist/releases/download/v13.7.0/Metrolist.apk . SHA-256: 6d52643d29cf78fd5f10464c7a64012439f29767e48830064b06df40fe97d527.

REA 6.8.0 CLI ran with bundled Node 24.22.1. MCP was not connected; client configuration was not changed. JADX was unavailable, so no REA decompilation claim is made. Source inspection used public upstream source through normal repository tools, as the REA skill directs for complete open source.

- APK signing-record parent Evidence ev_6ce02aaddec78cb3e21691fcd157395eb60d1668c6df34f1bc0a022052b228c8 identifies the exact selected bytes. REA's structural signing inspection is not signature/authenticity verification.
- Installed package Evidence ev_587cea368d518391b7b7738765c400eeb1793c770c5dce3536adad13fe50ef2a observes the package/version. The ADB operation itself does not bind APK bytes.
- Login-window Evidence ev_abf2e4f4739d11364b085eb8f6e890f8700b65d97be8b27c5aa076a4ec091136 and screen Evidence ev_9491a4c72a900ed27bf918333a7731517e45a54ee9fef809969643127f25f87a observe the anonymous Google sign-in entry after Home → Account → Login on the isolated emulator. No credentials were entered. These observations do not prove completed login, 2FA success or persistence.

Pinned release source: 7c5ba3ad22cb562d337897f82163a2b7fce665f3. LoginScreen.kt is text-identical to upstream main 876e355181fd3b04e7063b10442fef1b721d53b3. Release transport innertubex v0.5.2 is pinned at 93b650b877530738cbc4d2183b4322b77bb9033e.

- https://github.com/MetrolistGroup/Metrolist/blob/7c5ba3ad22cb562d337897f82163a2b7fce665f3/app/src/main/kotlin/com/metrolist/music/ui/screens/LoginScreen.kt
- https://github.com/MetrolistGroup/Metrolist/blob/7c5ba3ad22cb562d337897f82163a2b7fce665f3/innertube/src/main/kotlin/com/metrolist/innertube/YouTube.kt
- https://github.com/MetrolistGroup/innertubex/blob/93b650b877530738cbc4d2183b4322b77bb9033e/src/commonMain/kotlin/com/metrolist/innertubex/InnerTube.kt

## Observed source behavior and adaptation

Metrolist captures YouTube cookies, VISITOR_DATA, normalized DATASYNC_ID and SESSION_INDEX on Music, retries extraction, verifies accountInfo, and persists before declaring success. It supports channel selection and intent handoffs, flushes browser state, and does not erase cookies on each login entry. These are source observations; they do not prove the exact cause of the user's on-device failure.

Pocket Music beta 9 saved cookies alone and treated an existing file as connected. Beta 10 replaces that incomplete contract with capture → account_menu identity verification → atomic encrypted bundle → decryption/read-back comparison → confirmed display name. Captured visitor/account/channel/client-version context is reused for Home, rather than rescraped under a guessed account. Legacy cookie files remain readable for downloads; the account menu labels them unverified and recommends reconnecting.

The native Java implementation retains Pocket Music's framework and existing AES-GCM/Android Keystore identity. It implements the documented wire format independently; no Metrolist APK assets or Kotlin modules are bundled. Metrolist and innertubex are GPL-3.0, as is Pocket Music; upstream references are credited above.

The visible browser follows ordinary HTTPS redirects, while session configuration is read only on the exact HTTPS music.youtube.com origin. The fixed JavaScript expression checks location.origin inside the page and reads only the required ytcfg/yt.config fields; there is no native JS bridge or password/form extraction. Intent handoffs from trusted Google/YouTube pages use a safe HTTPS fallback where available, otherwise a sanitized browsable VIEW intent. No user-agent spoofing or Google-security bypass is added.

A bounded Music extraction loop precedes automatic validation/saving. Navigation/account changes and explicit cancellation prevent pending validation from replacing credentials. Failed validation/write/read-back keeps the previous encrypted session where storage recovery is possible. Download leases contain only temporary Netscape cookies; account context remains in the encrypted bundle and is not written to public preferences/logs.

As in a normal retained browser profile, login cookies survive Activity restarts/cancel/success so the handoff can complete; only Disconnect clears the isolated browser cookies/storage plus the app session. The browser profile is private Android WebView storage, not the app's AES bundle. Backup remains disabled. Choose account uses Google's account chooser; the full Metrolist native channel-switcher list is not ported. Use the Music page's profile controls for channel changes.

## Verification boundary

Tests use real Android WebView cookie storage (including HttpOnly session cookies), trusted/foreign-origin script evaluation, mocked account/feed responses and actual Keystore-backed persistence. They check capture, verified identity, encrypted read-back, download-lease compatibility, legacy migration, account/context reuse, denial/cancellation preservation and rejection of generic logged-in flags. Real Google credentials and 2FA are not used. Physical-device sign-in and live account accuracy remain user validation; test-signed prerelease only. Final totals are in TEST-RESULTS.md.
