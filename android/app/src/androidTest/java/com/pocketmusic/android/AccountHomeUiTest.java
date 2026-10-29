package com.pocketmusic.android;
import android.app.*;
import android.content.*;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import java.io.*;
import java.lang.reflect.Method;
import java.util.*;
import static org.junit.Assert.*;
public class AccountHomeUiTest {
 @Test public void accountHomeShowsActualShelfAndKeepsItOnRefreshFailure()throws Exception{
  Instrumentation test=InstrumentationRegistry.getInstrumentation();Context c=test.getTargetContext();Assume.assumeFalse(YouTubeSession.connected(c));if(android.os.Build.VERSION.SDK_INT>=33)test.getUiAutomation().grantRuntimePermission(c.getPackageName(),android.Manifest.permission.POST_NOTIFICATIONS);TestMusicFixtures.Snapshot snapshot=new TestMusicFixtures.Snapshot(c);AccountMusic.Source previous=Recommendations.accountSource;MainActivity[] activity={null};
  try{c.getSharedPreferences("music",0).edit().remove("tasteHidden").remove("musicCooldown").commit();YouTubeSession.save(c,"SAPISID=synthetic-ui-session");Recommendations.accountSource=(context,p)->List.of(new DiscoveryShelf("Your account favorites","Account",List.of(new Track("abcdefghijk","Account favorite song","Favorite artist",0,true,""))));Recommendations.clear(c);
   activity[0]=(MainActivity)test.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));await(test,"Your account favorites");await(test,"YOUR YOUTUBE MUSIC");assertTrue(test.getUiAutomation().getRootInActiveWindow().findAccessibilityNodeInfosByText("Find your flow").isEmpty());
   Recommendations.accountSource=(context,p)->{throw new IOException("Reconnect YouTube");};test.runOnMainSync(()->{try{Method method=MainActivity.class.getDeclaredMethod("refreshDiscovery",boolean.class);method.setAccessible(true);method.invoke(activity[0],true);}catch(Exception error){throw new AssertionError(error);}});await(test,"Reconnect YouTube from More");await(test,"Your account favorites");assertEquals("abcdefghijk",Recommendations.cached(c).get(0).id);
   if("true".equals(InstrumentationRegistry.getArguments().getString("accountScreen"))){test.waitForIdleSync();android.graphics.Bitmap screen=test.getUiAutomation().takeScreenshot();File folder=new File(c.getExternalFilesDir(null),"screens");folder.mkdirs();try(OutputStream out=new FileOutputStream(new File(folder,"account-home.png"))){screen.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}screen.recycle();}
  }finally{if(activity[0]!=null)test.runOnMainSync(activity[0]::finish);test.waitForIdleSync();Recommendations.accountSource=previous;YouTubeSession.disconnect(c);snapshot.restore();}
 }
 private void await(Instrumentation test,String text)throws Exception{long end=System.currentTimeMillis()+7000;while(System.currentTimeMillis()<end){AccessibilityNodeInfo root=test.getUiAutomation().getRootInActiveWindow();if(root!=null&&!root.findAccessibilityNodeInfosByText(text).isEmpty())return;Thread.sleep(80);}fail("Missing account UI text: "+text);}
}
