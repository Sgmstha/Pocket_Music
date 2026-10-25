package com.pocketmusic.android;

import android.app.*;
import android.content.*;
import android.media.*;
import android.media.session.*;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.*;
import java.nio.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class QueuePlaybackTest {
 private android.app.Instrumentation instrumentation;
 private Context context;
 private File first,second,third;
 private int oldRepeat,oldVolume;private boolean oldShuffle;
 private void main(Runnable action){instrumentation.runOnMainSync(action);}
 private void await(java.util.function.BooleanSupplier condition,String message)throws Exception{long deadline=System.currentTimeMillis()+10000;while(!condition.getAsBoolean()&&System.currentTimeMillis()<deadline)Thread.sleep(75);assertTrue(message,condition.getAsBoolean());}
 @Before public void prepare()throws Exception{
  instrumentation=InstrumentationRegistry.getInstrumentation();context=instrumentation.getTargetContext();
  oldRepeat=context.getSharedPreferences("music",0).getInt("repeat",0);oldShuffle=context.getSharedPreferences("music",0).getBoolean("shuffle",false);oldVolume=context.getSharedPreferences("music",0).getInt("volume",70);
  context.getSharedPreferences("music",0).edit().putInt("repeat",0).putBoolean("shuffle",false).putInt("volume",0).commit();
  main(()->context.stopService(new Intent(context,LocalPlayerService.class)));
  context.startActivity(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));instrumentation.waitForIdleSync();
  File directory=MusicRepository.folder(context);first=tone(directory,"Glass Queue A.wav");second=tone(directory,"Glass Queue B.wav");third=tone(directory,"Glass Queue C.wav");long stamp=System.currentTimeMillis();first.setLastModified(stamp+3000);second.setLastModified(stamp+2000);third.setLastModified(stamp+1000);
  main(()->context.startForegroundService(new Intent(context,LocalPlayerService.class).putExtra("path",first.getAbsolutePath())));
  await(()->LocalPlayerService.active!=null&&LocalPlayerService.active.playing(),"First track started");
 }
 @After public void cleanup(){
  main(()->context.stopService(new Intent(context,LocalPlayerService.class)));instrumentation.waitForIdleSync();
  for(File f:new File[]{first,second,third})if(f!=null)f.delete();
  context.getSharedPreferences("music",0).edit().putInt("repeat",oldRepeat).putBoolean("shuffle",oldShuffle).putInt("volume",oldVolume).commit();
 }
 @Test public void mediaSessionSkipsAndAutomaticNextWorkBehindHome()throws Exception{
  LocalPlayerService player=LocalPlayerService.active;MediaController controller=new MediaController(context,player.sessionToken());
  assertEquals("Glass Queue A",controller.getMetadata().getString(MediaMetadata.METADATA_KEY_TITLE));
  assertTrue((controller.getPlaybackState().getActions()&PlaybackState.ACTION_SKIP_TO_NEXT)!=0);
  controller.getTransportControls().skipToNext();await(()->player.path.equals(second.getAbsolutePath())&&player.playing(),"Media-session Next plays second track");
  controller.getTransportControls().skipToPrevious();await(()->player.path.equals(first.getAbsolutePath())&&player.playing(),"Media-session Previous plays first track");
  controller.getTransportControls().pause();await(()->!player.playing(),"Media-session Pause");
  controller.getTransportControls().seekTo(600);Thread.sleep(250);assertTrue(player.position()>=500);
  controller.getTransportControls().play();await(player::playing,"Media-session Play");
  NotificationManager notifications=context.getSystemService(NotificationManager.class);boolean found=false;
  for(android.service.notification.StatusBarNotification n:notifications.getActiveNotifications())if(n.getId()==10){found=true;assertTrue(n.getNotification().actions.length>=3);assertNotNull(n.getNotification().extras.getParcelable(Notification.EXTRA_MEDIA_SESSION));}
  assertTrue("Foreground media notification exists",found);
  context.startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
  main(()->player.seek(player.duration()-120));await(()->player.path.equals(second.getAbsolutePath())&&player.playing(),"Completion advances to next track behind Home");
  int before=player.position();Thread.sleep(900);assertTrue("Background audio advances",player.playing()&&player.position()>before+500);
 }
 @Test public void repeatAndFocusRecoveryRespectManualPause()throws Exception{
  LocalPlayerService player=LocalPlayerService.active;
  main(()->{player.repeat(PlaybackOrder.ONE);player.seek(player.duration()-120);});Thread.sleep(600);
  assertEquals(first.getAbsolutePath(),player.path);assertTrue(player.playing());assertTrue("Repeat-one restarted current track",player.position()<1500);
  main(()->{player.volume(.8f);player.onFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK);});assertEquals(.16f,player.effectiveVolume(),.01f);
  main(()->player.onFocusChange(AudioManager.AUDIOFOCUS_GAIN));assertEquals(.8f,player.effectiveVolume(),.01f);
  main(()->player.onFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT));assertFalse(player.playing());
  main(()->player.onFocusChange(AudioManager.AUDIOFOCUS_GAIN));await(player::playing,"Transient interruption resumes");
  main(()->{player.onFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT);player.pause();player.onFocusChange(AudioManager.AUDIOFOCUS_GAIN);});assertFalse("Manual pause suppresses focus-gain auto-resume",player.playing());
  main(()->{player.repeat(PlaybackOrder.OFF);player.play();player.next();});await(()->player.path.equals(second.getAbsolutePath())&&player.playing(),"Manual Next still advances with repeat-one disabled");
 }
 private static File tone(File directory,String name)throws Exception{
  int samples=22050*8;ByteBuffer buffer=ByteBuffer.allocate(44+samples*2).order(ByteOrder.LITTLE_ENDIAN);
  buffer.put("RIFF".getBytes()).putInt(36+samples*2).put("WAVEfmt ".getBytes()).putInt(16).putShort((short)1).putShort((short)1).putInt(22050).putInt(44100).putShort((short)2).putShort((short)16).put("data".getBytes()).putInt(samples*2);
  for(int i=0;i<samples;i++)buffer.putShort((short)(Math.sin(i*440*2*Math.PI/22050)*100));File f=new File(directory,name);try(FileOutputStream out=new FileOutputStream(f)){out.write(buffer.array());}return f;
 }
}
