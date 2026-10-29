package com.pocketmusic.android;
import android.app.*;
import android.app.job.*;
import android.content.*;
import android.view.*;
import android.widget.*;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.Assert.*;
public class SmartSettingsTest {
 private View text(View view,String label){if(view instanceof TextView&&label.contentEquals(((TextView)view).getText()))return view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){View found=text(((ViewGroup)view).getChildAt(i),label);if(found!=null)return found;}return null;}
 @Test public void consentEnablesConstrainedJobsAndDisablingKeepsFiles()throws Exception{
  Instrumentation test=InstrumentationRegistry.getInstrumentation();Context c=test.getTargetContext();TestMusicFixtures.Snapshot snapshot=new TestMusicFixtures.Snapshot(c);java.util.function.Predicate<Context> eligible=SmartDownloads.eligible;MainActivity[] activity={null};SmartDownloads.eligible=context->false;
  try{c.getSharedPreferences("music",0).edit().putBoolean("smartEnabled",false).putBoolean("smartConsent",false).commit();TestMusicFixtures.seed(c,List.of());test.getUiAutomation().grantRuntimePermission(c.getPackageName(),android.Manifest.permission.POST_NOTIFICATIONS);activity[0]=(MainActivity)test.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));test.runOnMainSync(()->{try{Method show=MainActivity.class.getDeclaredMethod("showSmartDownloads");show.setAccessible(true);show.invoke(activity[0]);Field field=MainActivity.class.getDeclaredField("smartDialog");field.setAccessible(true);androidx.appcompat.app.AlertDialog dialog=(androidx.appcompat.app.AlertDialog)field.get(activity[0]);View root=dialog.getWindow().getDecorView();((CheckBox)text(root,"Enable Smart downloads")).performClick();text(root,"Save settings").performClick();assertFalse(SmartDownloads.enabled(c));((CheckBox)text(root,"Automatically download selected music I own or have permission to download.")).performClick();text(root,"Save settings").performClick();assertTrue(SmartDownloads.enabled(c));assertNotNull(c.getSystemService(JobScheduler.class).getPendingJob(SmartDownloads.DAILY));((CheckBox)text(root,"Enable Smart downloads")).performClick();text(root,"Save settings").performClick();assertFalse(SmartDownloads.enabled(c));assertNull(c.getSystemService(JobScheduler.class).getPendingJob(SmartDownloads.DAILY));assertNull(c.getSystemService(JobScheduler.class).getPendingJob(SmartDownloads.NOW));}catch(Exception e){throw new AssertionError(e);}});if("true".equals(InstrumentationRegistry.getArguments().getString("smartScreen"))){test.waitForIdleSync();Thread.sleep(600);java.io.File folder=c.getExternalFilesDir("screens");folder.mkdirs();android.graphics.Bitmap bitmap=test.getUiAutomation().takeScreenshot();try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(folder,"smart-downloads.png"))){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();}}
  finally{test.runOnMainSync(()->{if(activity[0]!=null)activity[0].finish();});c.getSystemService(JobScheduler.class).cancel(SmartDownloads.DAILY);c.getSystemService(JobScheduler.class).cancel(SmartDownloads.NOW);SmartDownloads.eligible=eligible;snapshot.restore();}
 }
}
