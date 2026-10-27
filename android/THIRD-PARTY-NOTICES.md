# Reused components and assets

Pocket Music is GPL-3.0 licensed. Complete application source accompanies the release in this repository. Libraries are consumed through Gradle, with versions in gradle/libs.versions.toml; Gradle resolves their corresponding source artifacts. The following new v0.4 components replace custom infrastructure. License texts ship in app/src/main/assets/licenses and the APK; the app's Open-source licenses menu exposes the principal components.

| Component | Pinned version/source | License/use |
| --- | --- | --- |
| NewPipeExtractor | [v0.26.5](https://github.com/TeamNewPipe/NewPipeExtractor/tree/v0.26.5) | GPL-3.0-or-later; music search metadata and public audio stream resolution |
| AndroidX Media3 ExoPlayer | [1.9.0](https://github.com/androidx/media/tree/1.9.0) | Apache-2.0; buffered online audio playback |
| Material Components Android | [1.13.0](https://github.com/material-components/material-components-android/tree/1.13.0) | Apache-2.0; native navigation, cards, chips, sheets |
| AndroidX RecyclerView | [1.4.0](https://developer.android.com/jetpack/androidx/releases/recyclerview) | Apache-2.0; recycled album shelves |
| Glide | [4.16.0](https://github.com/bumptech/glide/tree/v4.16.0) | BSD/Apache-2.0, included upstream notice; artwork cache |
| Lucide | [1802f33f6c270395fcd981067f22719140e2ffbc](https://github.com/lucide-icons/lucide/tree/1802f33f6c270395fcd981067f22719140e2ffbc) | ISC; original SVGs and conversion script in third_party/lucide |
| jsoup | [1.22.2](https://github.com/jhy/jsoup/tree/jsoup-1.22.2) | MIT; extractor dependency |
| Rhino | [1.8.1](https://github.com/mozilla/rhino/tree/Rhino1_8_1_Release) | MPL-2.0; extractor dependency |
| NanoJSON | [e9d656ddb49a412a5a0a5d5ef20ca7ef09549996](https://github.com/TeamNewPipe/nanojson/tree/e9d656ddb49a412a5a0a5d5ef20ca7ef09549996) | Apache-2.0; extractor dependency |
| Protocol Buffers Java | [4.35.1](https://github.com/protocolbuffers/protobuf/tree/v35.1) | BSD-3-Clause; extractor dependency |

AndroidX/AppCompat and desugar_jdk_libs_nio 2.1.5 retain their upstream Apache/GPL-with-Classpath-exception notices in their published distributions. Existing youtubedl-android, yt-dlp and FFmpeg versions/provenance are documented in BUNDLED-ENGINE.json and the project build files. No Metrolist or Spotify implementation/assets were copied. Album artwork is loaded from catalogue thumbnail URLs for the corresponding displayed tracks, not bundled as application art. Public reference screenshots are excluded from published source and APKs.
