plugins { alias(libs.plugins.android.application) }
android {
    namespace = "com.pocketmusic.android"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.pocketmusic.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 17
        versionName = "0.4.0-beta.10"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17; isCoreLibraryDesugaringEnabled = true }
    buildTypes { release { isMinifyEnabled = false; signingConfig = signingConfigs.getByName("debug") } }
    splits { abi { isEnable = true; reset(); include("arm64-v8a", "armeabi-v7a", "x86_64"); isUniversalApk = false } }
    packaging { jniLibs { useLegacyPackaging = true; keepDebugSymbols += "**/*.zip.so" }; resources.excludes += setOf("META-INF/DEPENDENCIES", "META-INF/NOTICE*", "META-INF/LICENSE*") }
}
dependencies { implementation("androidx.media3:media3-exoplayer:1.9.0"); implementation(libs.recyclerview); implementation(libs.material); implementation(libs.glide); implementation(libs.newpipe); implementation(libs.ytdlp); implementation(libs.ffmpeg); implementation(libs.webkit); coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.1.5"); testImplementation("junit:junit:4.13.2"); androidTestImplementation("androidx.test:runner:1.6.2"); androidTestImplementation("androidx.test.ext:junit:1.2.1") }
