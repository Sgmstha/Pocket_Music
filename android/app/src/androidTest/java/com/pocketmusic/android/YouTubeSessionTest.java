package com.pocketmusic.android;
import android.content.*;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import java.io.*;
import java.nio.file.Files;
import static org.junit.Assert.*;

/** Synthetic credentials and isolated directories: never opens a user's account session. */
public class YouTubeSessionTest {
 private Context context;private File root;
 @Before public void setup()throws Exception{Context app=InstrumentationRegistry.getInstrumentation().getTargetContext();root=Files.createTempDirectory(app.getCacheDir().toPath(),"session-test-").toFile();context=new ContextWrapper(app){@Override public File getNoBackupFilesDir(){return root;}@Override public File getCacheDir(){return root;}};}
 @After public void cleanup(){File[] files=root.listFiles();if(files!=null)for(File f:files)f.delete();root.delete();}
 @Test public void encryptedRoundTripAndConcurrentLeases()throws Exception{
  YouTubeSession.save(context,"SAPISID=synthetic-session; PREF=example");assertTrue(YouTubeSession.connected(context));
  byte[] encrypted=Files.readAllBytes(new File(root,"youtube-session.bin").toPath());assertFalse(new String(encrypted,java.nio.charset.StandardCharsets.UTF_8).contains("synthetic-session"));
  File first=YouTubeSession.lease(context),second=YouTubeSession.lease(context);assertNotEquals(first,second);assertTrue(new String(Files.readAllBytes(first.toPath()),java.nio.charset.StandardCharsets.UTF_8).contains("SAPISID\tsynthetic-session"));
  first.delete();assertTrue(second.exists());YouTubeSession.disconnect(context);assertFalse(YouTubeSession.connected(context));assertNull(YouTubeSession.lease(context));second.delete();
 }
 @Test public void invalidReplacementKeepsSavedSession()throws Exception{YouTubeSession.save(context,"SAPISID=original-fake");try{YouTubeSession.save(context,"PREF=anonymous");fail();}catch(IllegalArgumentException expected){}File lease=YouTubeSession.lease(context);assertTrue(new String(Files.readAllBytes(lease.toPath()),java.nio.charset.StandardCharsets.UTF_8).contains("original-fake"));lease.delete();}
 @Test public void abandonedLeaseCleanupPreservesLiveRequests()throws Exception{YouTubeSession.save(context,"SAPISID=fake");File live=YouTubeSession.lease(context),old=new File(root,"youtube-request-old-process-1.txt");Files.write(old.toPath(),"synthetic".getBytes(java.nio.charset.StandardCharsets.UTF_8));YouTubeSession.cleanAbandoned(context);assertFalse(old.exists());assertTrue(live.exists());live.delete();}
}
