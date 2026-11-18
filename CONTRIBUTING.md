# Contributing to Pocket Music

Welcome, and thank you for your interest in contributing to **Pocket Music**! 🎉

Pocket Music is an open-source, cross-platform personal music player and audio utility designed for Windows and Android. Whether you're fixing a minor typo, proposing a new feature, testing on your Android phone, or optimizing core playback routines, every contribution is appreciated!

---

## 🌟 Where We Need the Most Help

We are actively seeking collaborators in several areas:

### 1. 📱 Android Native Development
* **Background Playback & Services:** Enhancing MediaSession and background playback stability across various OEM battery managers (Samsung OneUI, Xiaomi MIUI/HyperOS, etc.).
* **UI/UX Polish:** Implementing Material 3 dynamic color theming, gestures, queue rearrangement, and animations.
* **Device Testing:** Validating early-access APK builds across different Android versions (Android 8.0 through Android 15) and architectures (ARM64, ARMv7).

### 2. 💻 Windows Desktop (C# / WPF / PowerShell)
* **WPF Interface Refinement:** Smoothing transitions, modern scrollbars, and high-DPI scaling.
* **System Integrations:** Enhancing Windows System Media Transport Controls (SMTC), taskbar previews, and global hotkeys.
* **Packaging & Delivery:** Improving portable release packaging and self-contained builds.

### 3. 🎵 Audio & Metadata Engine
* **Metadata Tagging:** Improving ID3 / vorbis comment extraction and embedding album artwork into downloaded audio files.
* **Performance & Safety:** Ensuring reliable process cleanup during cancellation and network interruptions.

### 4. 🌍 Localization & Documentation
* **Translations:** Translating strings into multiple languages.
* **Documentation:** Improving onboarding guides, FAQs, and troubleshooting documentation.

---

## 🛠️ Setting Up for Local Development

### Windows Desktop Setup
1. **Prerequisites:**
   * Windows 10 or 11
   * Windows PowerShell 5.1+
   * [.NET Framework Developer Pack](https://dotnet.microsoft.com/download/dotnet-framework) (C# compiler `csc.exe` for building the launcher)
   * [FFmpeg](https://ffmpeg.org/download.html) and [Node.js LTS](https://nodejs.org/) installed and available in `PATH`.
2. **Running locally:**
   * Double-click `Launch.cmd` or `Launch-Metrolist.cmd` in the repository root.
3. **Running automated tests:**
   * Open PowerShell and run:
     ```powershell
     powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\Test.ps1
     ```

### Android App Setup
1. **Prerequisites:**
   * [Android Studio](https://developer.android.com/studio) (Koala / Ladybug or newer recommended)
   * JDK 17
   * Android SDK (API 26+)
2. **Building the APK:**
   * Navigate to the `android/` directory:
     ```bash
     cd android
     ./gradlew assembleDebug
     ```
   * Open the `android/` folder in Android Studio to run directly on an emulator or connected device.

---

## 🚀 How to Submit a Contribution

1. **Find or open an issue:**
   * Check [existing Issues](https://github.com/Sgmstha/Pocket_Music/issues) before starting significant work.
   * If you have an idea or found a bug, feel free to open a new Issue using the templates.
2. **Fork and create a branch:**
   ```bash
   git checkout -b feature/my-cool-feature
   # or
   git checkout -b fix/issue-description
   ```
3. **Make your changes:**
   * Keep your pull requests focused on a single concern.
   * Ensure existing tests pass.
   * Respect user privacy: never commit cookies, credentials, or personal tokens.
4. **Submit a Pull Request (PR):**
   * Fill out the PR template explaining the problem, the solution, and how you tested it.
   * We will review your PR promptly, provide constructive feedback, and merge it!

---

## 🤝 Community & Support

Have questions or need guidance on where to start? Open a GitHub Discussion or create an Issue with the `question` or `help-wanted` label. We are excited to build this together with you!
