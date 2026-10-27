# Desktop components

tokens.json preserves Android's colour and mood roles, adding desktop panel #111A29 and measured native dimensions. Core colour combinations pass 17 contrast checks. Segoe UI remains the Windows font.

- Global header: query, local/history suggestions, shortcuts, panel and download toggles.
- Persistent library: 248px normally, 208px on smaller displays, 88px compact rail. Real saved tracks, literal filter, explicit double-click/Enter playback.
- Home: six recent quick plays and a four-track cover shelf, moods and discovery. Test screenshots use labelled visual fixtures; the app never seeds fake songs.
- Search: ready-state topics execute real queries. Suggestions use bounded history/local titles and guided suffixes, not account-personalized recommendations.
- Now playing: optional 304px sidebar, 280px deliberate narrow view, current artwork/title/source/state, play/pause and three queue entries. Auto-collapses on small windows. Downloads and now playing do not squeeze the middle simultaneously.
- Transport: compact 92px strip, circular play/pause, same seek/volume/shuffle/repeat/sleep/queue.
- Original cover assets: generated record motifs provide local fallbacks. WPF image conversion explicitly unwraps PowerShell values; decoded images are frozen and reused. No embedded album-art extraction was added.

Spotify's public layout patterns were adapted; branding, artwork and source remain Pocket Music's own.
