# Validation

Run `MusicDownloader.ps1 -Desktop -SmokeTest` in Windows PowerShell STA. Native-window assertions cover navigation, suggestions, library filtering and sorting, sleep presets/cancellation, settings, queue and Windows toolbar creation. Tests mock playback when exercising UI actions; no download is started.

Run with `-Desktop -PlaybackTestFolder <fixtures>` for all five native audio formats. Run separately with `-Desktop -WindowsMediaTest -PlaybackTestFolder <fixtures>` for native Windows metadata/artwork, thumbnail controls and media commands while minimized.

Use `-SmokeTest -PreviewImage <absolute png>` for wide, library, downloads and minimum-size captures. Check compact navigation labels and result actions. Run Replica's contrast.py on replica/design/tokens.json.

Additional reconstruction checks cover persistent sidebar data/filter/playback, browse query routing, narrow now-playing toggle, mutually exclusive right panels and cached frozen artwork. Playback fixtures exercise sidebar/shelf queue order and now-playing synchronization against the real native player. The PreviewBridge.test.js contract test does not need network. Replica parity.py uses features.csv for the declared UI subset only.
