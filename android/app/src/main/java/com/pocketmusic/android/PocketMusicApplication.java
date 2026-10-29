package com.pocketmusic.android;
import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;

/** Keep every Material surface dark even when the phone uses a light system theme. */
public final class PocketMusicApplication extends Application {
 @Override public void onCreate(){super.onCreate();SmartDownloads.schedule(this,false);AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);}
}
