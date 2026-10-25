package com.pocketmusic.android;
import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.yausername.youtubedl_android.YoutubeDL;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class LiveDownloadTest {
 @Test public void upstreamTestVideoDownloads() throws Exception {
  // Opt-in: requires internet; uses the Creative Commons Big Buck Bunny sample from upstream documentation.
  org.junit.Assume.assumeTrue("true".equals(InstrumentationRegistry.getArguments().getString("liveDownload")));
  Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
  MusicRepository.updateEngine(c,true);
  assertNotNull(YoutubeDL.getInstance().version(c));
  YoutubeDL.getInstance().execute(MusicRepository.download(c,"YE7VzlLtp-4","mp3"),"live-test",false,null);
  File audio=MusicRepository.local(c,"YE7VzlLtp-4");
  assertNotNull("YouTube test video saved as MP3",audio);
  assertTrue(audio.length()>1000);
  audio.delete();
 }
}
