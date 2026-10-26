package com.pocketmusic.android;
import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.widget.EditText;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.*;
import java.util.*;
import static org.junit.Assert.*;

/** Opt-in emulator screenshots; synthetic feed/account, never real user credentials. */
public class DiscoveryScreensTest {
 @Test public void captureDiscoverySearchAndPlayback()throws Exception{
  Assume.assumeTrue("true".equals(InstrumentationRegistry.getArguments().getString("captureReplica")));Assume.assumeTrue(android.os.Build.FINGERPRINT.contains("generic")||android.os.Build.MODEL.contains("sdk"));
  Instrumentation test=InstrumentationRegistry.getInstrumentation();Context context=test.getTargetContext();Assume.assumeFalse(YouTubeSession.connected(context));MainActivity[] activity={null};File file=new File(MusicRepository.folder(context),"Blue hour [abcdefghijk].wav");Assume.assumeFalse(file.exists());
  try{YouTubeSession.save(context,"SAPISID=synthetic-screenshot-only");List<Track> tracks=List.of(new Track("abcdefghijk","Blue hour","Demo artist",203),new Track("12345678901","Blue skies","Demo artist",186),new Track("ZYXWVUTSRQP","Blue dawn","Demo artist",225),new Track("98765432109","Blue notes","Demo artist",197));context.getSharedPreferences("music",0).edit().putString("feedScope",Recommendations.scope(context)).putString("feedTracks",TrackMetadata.encode(tracks)).putLong("feedTime",System.currentTimeMillis()).commit();if(android.os.Build.VERSION.SDK_INT>=33)test.getUiAutomation().grantRuntimePermission(context.getPackageName(),android.Manifest.permission.POST_NOTIFICATIONS);
   int samples=22050*20;ByteBuffer buffer=ByteBuffer.allocate(44+samples*2).order(ByteOrder.LITTLE_ENDIAN);buffer.put("RIFF".getBytes()).putInt(36+samples*2).put("WAVEfmt ".getBytes()).putInt(16).putShort((short)1).putShort((short)1).putInt(22050).putInt(44100).putShort((short)2).putShort((short)16).put("data".getBytes()).putInt(samples*2);try(FileOutputStream out=new FileOutputStream(file)){out.write(buffer.array());}
   activity[0]=(MainActivity)test.startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));capture(test,context,"M01-home");assertFalse(test.getUiAutomation().getRootInActiveWindow().findAccessibilityNodeInfosByText("Saved · plays offline").isEmpty());
   test.runOnMainSync(()->{try{SearchResultCache cache=(SearchResultCache)field(activity[0],"searchCache");cache.put("blue",tracks,android.os.SystemClock.elapsedRealtime());((EditText)field(activity[0],"query")).setText("blue");call(activity[0],"startSearch");}catch(Exception e){throw new AssertionError(e);}});capture(test,context,"M02-search");
   test.runOnMainSync(()->{try{Method play=MainActivity.class.getDeclaredMethod("playTrack",Track.class,List.class);play.setAccessible(true);play.invoke(activity[0],tracks.get(0),tracks);}catch(Exception e){throw new AssertionError(e);}});long deadline=System.currentTimeMillis()+7000;while((LocalPlayerService.active==null||!LocalPlayerService.active.playing())&&System.currentTimeMillis()<deadline)Thread.sleep(80);assertNotNull(LocalPlayerService.active);assertTrue(LocalPlayerService.active.playing());test.runOnMainSync(()->{try{call(activity[0],"showNowPlaying");}catch(Exception e){throw new AssertionError(e);}});capture(test,context,"M03-player");
  }finally{test.runOnMainSync(()->{context.stopService(new Intent(context,LocalPlayerService.class));if(activity[0]!=null)activity[0].finish();});file.delete();YouTubeSession.disconnect(context);Recommendations.clear(context);}
 }
 private Object field(Object object,String name)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
 private void call(Object object,String name)throws Exception{Method m=object.getClass().getDeclaredMethod(name);m.setAccessible(true);m.invoke(object);}
 private void capture(Instrumentation test,Context context,String name)throws Exception{Thread.sleep(900);Bitmap image=test.getUiAutomation().takeScreenshot();assertNotNull(image);File dir=new File(context.getExternalFilesDir(null),"replica/metrolist");dir.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(dir,name+".png"))){assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,out));}if(android.os.Build.VERSION.SDK_INT>=29){ContentValues values=new ContentValues();values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME,name+".png");values.put(android.provider.MediaStore.Images.Media.MIME_TYPE,"image/png");values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH,"Pictures/PocketMusicTest-beta5");android.net.Uri uri=context.getContentResolver().insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);assertNotNull(uri);try(OutputStream out=context.getContentResolver().openOutputStream(uri)){assertNotNull(out);assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,out));}}image.recycle();}
}
