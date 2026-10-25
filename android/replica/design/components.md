# Native component specification

Measured reference structure: screenshot_1 is 1080x2400; its navigation occupies roughly the bottom 220px, row artwork roughly 130px square, and the first row starts around y550. Screenshot_5 gives most of its upper half to a square cover and separates primary transport from secondary actions. These are measured image bands, not assumed dp values; typography/contrast below use Pocket Music's own tokens.

## Primitives

- Row: 68dp minimum, 48dp artwork, 12dp text gap, title 14sp/600 max 2 lines, secondary 12sp. Whole row click/ripple; separate 48dp options. Pressed/selected use raised surface; long text ellipsizes. No per-track two-button forms.
- Shelf: horizontally scrollable 132dp square cover/title tiles with 12dp gaps. Only actual saved/recent tracks. Empty state offers discovery/import rather than fabricated content.
- Chip: 48dp minimum touch height, compact text, selected filled accent and dark text. Focus outline uses border-input; disabled content visibly muted.
- Navigation: Home/Search/Library, 64dp target band, selected icon pill within the item. Import in menu. Persistent playback continues across destinations. Keyboard hides bottom controls.
- Search: labeled field, explicit submit, recent-query chips, Clear history, result count/loading/error. Static loading placeholders do not continuously animate. Stale search callbacks must not replace another tab.
- Library: compact total, media chips, latest/title sort. Empty and filtered-empty states differ. Play and overflow remain separate accessible actions.
- Player: opaque sheet backdrop, large square cover with original fallback artwork; title/source/time, 48dp+ transport, secondary controls and volume. No transparent duplicate controls. Scrolls on small/large-text layouts.
- Feedback: progress only while active; actionable errors persist; completed downloads do not leave stale permanent banners.

Existing system dialogs/picker retain Android's semantics. Preview IFrame is always at least 200x200 and is not cloned/replaced when modes change. Selected-state descriptions and TalkBack labels identify current actions.

Contrast: run replica-design/contrast.py on tokens.json; body 4.5:1, controls 3:1. Glass is limited to shell/panels; text surfaces are controlled opaque colours. Original palette, icons, fallback artwork and labels; reference photos/art/logos are never used as app assets.
