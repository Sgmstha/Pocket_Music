package com.pocketmusic.android;
import android.content.Context;
import android.os.SystemClock;
import android.provider.Settings;
final class SleepTimer {
 static void set(Context c,long duration){c.getSharedPreferences("music",0).edit().putLong("sleepDeadline",duration<=0?0:SystemClock.elapsedRealtime()+duration).putInt("sleepBoot",Settings.Global.getInt(c.getContentResolver(),Settings.Global.BOOT_COUNT,0)).apply();if(LocalPlayerService.active!=null)LocalPlayerService.active.armSleepTimer();}
 static long remaining(Context c){if(c.getSharedPreferences("music",0).getInt("sleepBoot",-1)!=Settings.Global.getInt(c.getContentResolver(),Settings.Global.BOOT_COUNT,0))return 0;long deadline=c.getSharedPreferences("music",0).getLong("sleepDeadline",0);return deadline==0?0:Math.max(0,deadline-SystemClock.elapsedRealtime());}
 static boolean consumeExpired(Context c){long deadline=c.getSharedPreferences("music",0).getLong("sleepDeadline",0);if(deadline>0&&remaining(c)==0){c.getSharedPreferences("music",0).edit().remove("sleepDeadline").apply();return true;}return false;}
}
