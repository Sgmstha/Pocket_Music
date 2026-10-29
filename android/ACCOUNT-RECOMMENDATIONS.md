# Account music recommendations — Android beta 8

Home now requests your connected YouTube Music Home feed instead of using artist/genre searches as if they were account recommendations. Existing on-device likes and recent listening remain separate library sections. If you have not connected YouTube, local suggestions are labeled and the Home card offers Connect YouTube.

After updating, open Home and Refresh. If requested, use More → YouTube account → Reconnect, complete Google sign-in and tap Use this session on YouTube Music. No cookie export is needed. Your existing encrypted session is reused when it has the authentication material required by YouTube Music.

The first supported song shelf becomes the Home card. Other supported shelves retain their account titles and ordering. Up to four shelves/12 songs each are shown, deduplicated with hidden-song filtering and an artist cap. Audio tracks and official music videos with explicit music-type metadata are supported. General videos, podcasts, unsupported card types, deleted and malformed entries are omitted. Up to two playlist shelves from the Home response may be expanded; this is not full YouTube Music UI/feed parity.

Connected-account failures keep cached account picks and show refresh/reconnect status. They never trigger generic search replacements. Caches are scoped to the encrypted session fingerprint; reconnect/disconnect invalidates old account picks, and an account change during loading discards its results. Old search-based caches are invalidated on upgrade. Offline cached metadata remains available; playback/downloads still need internet unless saved locally.

Session plaintext is leased from encrypted storage only for the request and deleted in finally. Cookie/auth headers go only to the fixed HTTPS music.youtube.com origin; redirects are rejected, requests are cancellable, response sizes/counts/timeouts are bounded, and secrets/raw account responses are not logged or committed. No account mutations, Google-history writes or subscriptions are performed. Native Pocket Music plays do not automatically update Google's listening history. The account's own YouTube Music history/settings govern its remote recommendations.

This uses the web client's unofficial browse format, which may change. Standard/default Google account behavior is the target; secondary/brand-account identity has not been verified on-device. Public client bootstrap returned HTTP 200 and current client configuration was found. Signed-in recommendation content cannot be verified without a real account session, so test with your connected phone. No successful live personalized-feed validation or stable certification is claimed.

Protocol/schema references, consulted as documentation (no source/assets copied):
- https://github.com/sigma67/ytmusicapi/blob/main/ytmusicapi/mixins/browsing.py
- https://github.com/sigma67/ytmusicapi/blob/main/ytmusicapi/navigation.py
- https://github.com/sigma67/ytmusicapi/blob/main/ytmusicapi/ytmusic.py
