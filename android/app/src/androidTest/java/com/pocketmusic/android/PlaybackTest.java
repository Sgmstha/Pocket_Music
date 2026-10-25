package com.pocketmusic.android;
import android.content.*;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.*;
import java.util.*;
import com.yausername.youtubedl_android.*;
@RunWith(AndroidJUnit4.class) public class PlaybackTest {
 @Test public void nativeDownloadConversionAndBackgroundPlayback()throws Exception{
  var instrumentation=InstrumentationRegistry.getInstrumentation();Context c=instrumentation.getTargetContext();
  File wav=new File(c.getCacheDir(),"fixture.wav");int n=44100*4;ByteBuffer b=ByteBuffer.allocate(44+n*2).order(ByteOrder.LITTLE_ENDIAN);b.put("RIFF".getBytes()).putInt(36+n*2).put("WAVEfmt ".getBytes()).putInt(16).putShort((short)1).putShort((short)1).putInt(44100).putInt(88200).putShort((short)2).putShort((short)16).put("data".getBytes()).putInt(n*2);for(int i=0;i<n;i++)b.putShort((short)(Math.sin(i*440*2*Math.PI/44100)*100));try(FileOutputStream out=new FileOutputStream(wav)){out.write(b.array());}
  List<String> args=new ArrayList<>(MusicRepository.download(c,"abcdefghijk","mp3").buildCommand());args.remove(args.size()-1);
  YoutubeDLRequest request=new YoutubeDLRequest(wav.toURI().toString());request.addCommands(args).addOption("--enable-file-urls");YoutubeDL.getInstance().execute(request,"fixture",false,null);
  File audio=null;for(File f:MusicRepository.files(c))if(f.getName().endsWith("[fixture].mp3"))audio=f;assertNotNull("Native yt-dlp + FFmpeg produced MP3",audio);assertTrue(audio.length()>100);
  final File track=audio;c.startActivity(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));instrumentation.waitForIdleSync();
  instrumentation.runOnMainSync(()->c.startForegroundService(new Intent(c,LocalPlayerService.class).putExtra("path",track.getAbsolutePath())));
  long deadline=System.currentTimeMillis()+10000;while((LocalPlayerService.active==null || !LocalPlayerService.active.playing())&&System.currentTimeMillis()<deadline)Thread.sleep(100);
  assertNotNull(LocalPlayerService.active);assertTrue("Native player started",LocalPlayerService.active.playing());
  instrumentation.runOnMainSync(()->LocalPlayerService.active.pause());assertFalse(LocalPlayerService.active.playing());
  instrumentation.runOnMainSync(()->{LocalPlayerService.active.seek(500);LocalPlayerService.active.volume(0);LocalPlayerService.active.play();});
  c.startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));Thread.sleep(1500);
  assertTrue("Local audio continues behind the home screen",LocalPlayerService.active.playing());assertTrue(LocalPlayerService.active.position()>1000);
  instrumentation.runOnMainSync(()->c.stopService(new Intent(c,LocalPlayerService.class)));wav.delete();audio.delete();
 }
}
