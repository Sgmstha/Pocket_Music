package com.pocketmusic.android;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.provider.DocumentsContract;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import java.io.*;
import static org.junit.Assert.*;

public class LiveFolderDownloadTest {
 @Test public void downloadServiceAutomaticallySavesOutsideApp()throws Exception{
  Assume.assumeTrue("true".equals(InstrumentationRegistry.getArguments().getString("liveDownload")));
  Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();Context c=instrumentation.getTargetContext();Assume.assumeTrue(DownloadFolder.hasAccess(c));
  String id="YE7VzlLtp-4";File before=MusicRepository.downloaded(c,id,"mp3");Uri existing=before==null?null:DownloadFolder.savedUri(c,before);
  Activity activity=instrumentation.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
  try{
   instrumentation.runOnMainSync(()->c.startForegroundService(new Intent(c,DownloadService.class).putExtra("id",id).putExtra("format","mp3")));
   long deadline=System.currentTimeMillis()+180000;while(!DownloadService.busy&&System.currentTimeMillis()<deadline)Thread.sleep(50);assertTrue("Download service started",DownloadService.busy);
   while(DownloadService.busy&&System.currentTimeMillis()<deadline)Thread.sleep(200);assertFalse("Download completed",DownloadService.busy);
   assertEquals("",c.getSharedPreferences("music",0).getString("downloadDetails",""));assertEquals("",c.getSharedPreferences("music",0).getString("folderSaveError",""));
   File audio=MusicRepository.downloaded(c,id,"mp3");assertNotNull(audio);Uri saved=DownloadFolder.savedUri(c,audio);assertNotNull("External folder copy exists",saved);
   try(InputStream in=c.getContentResolver().openInputStream(saved)){assertNotNull(in);byte[] buffer=new byte[32768];long bytes=0;int n;while((n=in.read(buffer))!=-1)bytes+=n;assertEquals(audio.length(),bytes);}
   assertTrue(c.getSharedPreferences("music",0).getString("downloadStatus","").startsWith("Saved to "));
  }finally{
   if(DownloadService.busy)instrumentation.runOnMainSync(()->c.startService(new Intent(c,DownloadService.class).setAction("cancel")));
   File downloaded=MusicRepository.downloaded(c,id,"mp3");if(downloaded!=null&&before==null){Uri saved=DownloadFolder.savedUri(c,downloaded);if(saved!=null&&existing==null)DocumentsContract.deleteDocument(c.getContentResolver(),saved);downloaded.delete();}
   instrumentation.runOnMainSync(activity::finish);
  }
 }
}
