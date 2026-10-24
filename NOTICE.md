# Attribution and modification notice

Preview play uses Microsoft's WebView2 SDK 1.0.4314.35. Its binary components under tools/webview2 retain Microsoft's accompanying LICENSE.txt and NOTICE.txt, rather than the application's GPL license. YouTube's player/API is loaded from YouTube and is not bundled or relicensed. Preview.ps1 and preview/index.html are GPL-3.0 application source.

Pocket Music is an independent Windows desktop prototype, not an official Metrolist release.

This desktop adaptation is provided under GNU GPL version 3; see LICENSE. Its complete application source is included as PowerShell and XAML. Third-party executables retain their own licenses (see README.md).

Reused resources originate from **Metrolist Project (C) 2026**, licensed GPL-3.0, with contributors recorded in the upstream Git history:

https://github.com/MetrolistGroup/Metrolist/tree/6d08504262de76a4ba841dc24489a754e2f48e4f

- `metrolist-source/` contains unmodified copies of home_outlined.xml, search.xml, download.xml, library_music_outlined.xml, music_note.xml from app/src/main/res/drawable, plus ui/theme/Theme.kt for provenance.
- `MetrolistIcons.xaml` adapts the original Android vector path geometry into WPF GeometryGroups. Colors and sizes are supplied by the desktop theme; Android stroke/tint attributes are not copied wholesale.
- `Desktop.xaml` and `DesktopController.ps1` adapt visual patterns from HomeScreen.kt, OnlineSearchScreen.kt, and the published screenshot_1.png: pure-dark surfaces, rounded chips and cards, music rows, large section headings, and navigation. The lavender palette follows that published screenshot rather than reproducing Android wallpaper-driven dynamic colors. The original theme's default seed is #ED5564; it is not falsely presented as lavender.
- Navigation has been moved into a desktop rail, and a permanent-file download panel added. The inspiration cards are new abstract graphics and actual search shortcuts, not copied album covers or personalized recommendations.
- `Search.ps1` is new desktop code using yt-dlp's public YouTube search. It does not yet port Metrolist's Kotlin/InnerTubeX search backend, categorized YouTube Music results, Android Compose runtime, or Media3 services.

Changes made 23 October 2026: resource conversion, Windows layout adaptation, search/results selection, local-folder library, file-player launching, and integration with the existing audio export controller. All new application source in this folder is offered under GPL-3.0. No Metrolist logo or claim of official affiliation is used.
