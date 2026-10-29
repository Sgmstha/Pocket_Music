# Google/YouTube login recovery — Android beta 9

Earlier login navigation allowed accounts.google.com and YouTube but blocked Google's www.google.com/accounts session handoff and consent pages. It also rejected third-party login cookies, tied Save to the final Music page URL, and cleared browser session state on every Activity destruction. These can interrupt the post-verification redirect or make a ready session impossible to save.

This update permits the specific HTTPS Google account/consent handoff hosts and the /accounts/ path on www.google.com. Arbitrary destinations, HTTP, embedded credentials, nonstandard ports and lookalike hosts remain blocked. The default WebView identity is retained; no user-agent spoofing, password interception or injected page JavaScript.

Only the separate, visible login WebView temporarily accepts third-party sign-in cookies. The return destination is regular YouTube, rather than waiting for the heavier Music page. Readiness checks the YouTube origins once per second while the login screen is foreground. Save account becomes available once a valid YouTube session with music-signing material is present, even when the current redirect/page is still loading. Saving remains an explicit user action and writes through the existing encrypted session store.

Open YouTube and Reload remain available to recover after completing verification. A progress bar and a 30-second loading hint replace an unexplained spinner; the app never automatically skips or restarts an active challenge. Finish verification first, then use Open YouTube if the redirect stalls, and tap Save account when enabled. On success, a confirmation returns to Pocket Music and Home detects the saved account.

Home/background pauses stop the readiness polling. An unfinished Activity recreation restores WebView state and retains its isolated login cookies. Fresh sign-in starts clear the old browser profile, and explicit Cancel/success/disconnect clear browser cookies and storage. Pending browser session state may remain in the private WebView profile after process death until the flow resumes or a fresh login clears it. App backup is disabled. The long-lived app session remains encrypted in no-backup storage. Cancelling reconnect does not erase the previously saved account.

Verification uses synthetic cookies/session fixtures and the real separate-process login UI without any Google credentials. It cannot certify a real two-step challenge or override Google's embedded-browser restrictions. If Google explicitly rejects this WebView, the app reports the limitation; it does not bypass Google's security checks. Default/secondary/brand account behavior still requires physical-device testing.

Platform references:
- https://developer.android.com/reference/android/webkit/CookieManager
- https://developer.android.com/reference/android/webkit/WebView
- https://developers.google.com/identity/protocols/oauth2/policies
