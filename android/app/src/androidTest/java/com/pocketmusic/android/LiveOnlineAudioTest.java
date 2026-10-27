package com.pocketmusic.android;
import android.app.*;
import android.content.*;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import java.util.*;
import java.lang.reflect.*;
import static org.junit.Assert.*;
/** Opt-in public music streaming, muted; never uses a real account or saves audio. */
public class LiveOnlineAudioTest {
 @Test public void publicAudioPlaysAfterHomeWithoutWritingMusicFile()throws Exception{
  Assume.assumeTrue("Opt-in public stream","true".equals(InstrumentationRegistry.getArguments().getString("liveOnline")));Instrumentation test=InstrumentationRegistry.getInstrumentation();Context context=test.getTargetContext();assertFalse("Use an account-free emulator",YouTubeSession.connected(context));int before=MusicRepository.files(context).size();List<Track> songs=MusicCatalog.songs("Daft Punk Get Lucky","live-online-catalogue");assertFalse("No current public music fixture",songs.isEmpty());Track track=songs.get(0);
  TestMusicFixtures.Snapshot snapshot=new TestMusicFixtures.Snapshot(context);OnlineAudio.Resolver original=LocalPlayerService.onlineResolver;MainActivity[] activity={null};
  try{context.getSharedPreferences("music",0).edit().putInt("volume",0).remove("playerError").commit();TestMusicFixtures.seed(context,List.of(track));if(android.os.Build.VERSION.SDK_INT>=33)test.getUiAutomation().grantRuntimePermission(context.getPackageName(),android.Manifest.permission.POST_NOTIFICATIONS);activity[0]=(MainActivity)test.startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));test.runOnMainSync(()->{try{Method play=MainActivity.class.getDeclaredMethod("playTrack",Track.class,List.class);play.setAccessible(true);play.invoke(activity[0],track,List.of(track));}catch(Exception e){throw new AssertionError(e);}});context.startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));long end=System.currentTimeMillis()+95000;while((LocalPlayerService.active==null||!LocalPlayerService.active.playing()||LocalPlayerService.active.position()<250)&&System.currentTimeMillis()<end){if(!context.getSharedPreferences("music",0).getString("playerError","").isEmpty())break;Thread.sleep(100);}LocalPlayerService player=LocalPlayerService.active;assertNotNull("Public audio did not open: "+context.getSharedPreferences("music",0).getString("playerError",""),player);assertTrue("Public audio is not playing",player.playing());context.startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));int position=player.position();Thread.sleep(1500);assertTrue("Public audio stopped after Home",player.position()>position+600);assertTrue(player.networkPlayback);assertEquals(before,MusicRepository.files(context).size());}
  finally{test.runOnMainSync(()->{context.stopService(new Intent(context,LocalPlayerService.class));if(activity[0]!=null)activity[0].finish();});long end=System.currentTimeMillis()+5000;while(LocalPlayerService.active!=null&&System.currentTimeMillis()<end)Thread.sleep(50);LocalPlayerService.onlineResolver=original;snapshot.restore();}
 }
}
