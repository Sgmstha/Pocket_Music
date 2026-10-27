# Connect YouTube using a local cookie export

1. Open **Pocket Music.exe** or **Launch.cmd**, then choose **Connect YouTube** in the header.
2. Use **Open YouTube / sign in** and sign in in your browser. Use **Export instructions** for yt-dlp's official YouTube cookie-export instructions. Export a YouTube-only file in Netscape cookies.txt format.
3. Choose **Import YouTube cookies...** and select that file locally. Do not paste or upload the cookie file into a chat or issue.
4. Retry your download after the rate limit has cleared, or use **Load my recommendations** / **My YouTube picks**.

Only user-selected cookie-file import is implemented. Pocket Music does not inspect Chrome, Edge, Firefox or other browser databases and never requests a Google password. It does not implement Google OAuth or change the account used by the separate official YouTube preview embed.

## Storage and cleanup

The importer validates Netscape rows, keeps youtube.com and its subdomains, removes other domains and expired cookies, and requires an unexpired YouTube sign-in cookie. Imports are limited to 512 KB. No source-file path is retained. The original export is not changed or deleted; manage that sensitive file yourself after import.

The filtered session is encrypted with Windows DPAPI for the current Windows user and saved in LocalAppData/PocketMusic/YouTube/session.bin. The private directory grants access to that user. Import replacement is atomic. No cookies or passwords are saved with the project or sent to a Pocket Music server.

For a user-started YouTube download/search/recommendation request, the session is decrypted into a private request-specific file for yt-dlp. Separate concurrent requests use separate files. Completion, failure, cancellation and window close clean up the request files. On the next start, abandoned files from processes that no longer exist are removed. Cookie headers and Netscape cookie rows are redacted from app activity. These protections do not defend against malware running as the same Windows user.

**Disconnect** deletes Pocket Music's encrypted session and owned temporary files; it does not sign your browser out. Session changes are blocked while a download or search is running.

## What connection status means

“Session saved” means a valid-looking export was encrypted locally. It does not claim that YouTube accepted the session. YouTube can expire or reject it; import a fresh export when that happens. A 429 remains a server-side rate limit and may persist even after importing cookies. The app paces requests, reduces retries and blocks immediate retries for at least 60 seconds after a 429; this is not a promise the server block lasts only one minute.

The recommendations action requests at most 12 entries from yt-dlp's supported YouTube account recommended feed, using the imported session. It uses metadata only, and results keep Preview and Save separate. These are YouTube home recommendations, not a claimed YouTube Music API or a guarantee that every item is music. There is no automatic background refresh or additional idle timer.

## Validation

- 40 isolated synthetic session checks: domain filtering, expiry, malformed/large input rejection, DPAPI encryption, private folder ACL, atomic replacement, separate leases, cleanup, disconnect, log redaction, argument wiring and real yt-dlp reading a synthetic cookie file against generated local audio.
- 95 real-window checks including the connection dialog, disabled account actions without a session and no credential lease on anonymous UI flows.
- 15 rate-limit/download regression checks; iframe bridge contract suite passed.
- All automated account tests use synthetic cookies in isolated temporary directories. Test runs do not inherit the listener's saved account.

Live authenticated downloading and personalized feed content remain unverified until the user imports their own current session through the app. No real cookie file or browser database was accessed during development.

References: https://github.com/yt-dlp/yt-dlp/wiki/FAQ#how-do-i-pass-cookies-to-yt-dlp and https://github.com/yt-dlp/yt-dlp/wiki/Extractors#exporting-youtube-cookies
