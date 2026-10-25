package com.pocketmusic.android;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.provider.DocumentsContract;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import java.io.*;
import java.util.*;
import static org.junit.Assert.*;

public class MultiDownloadIntegrationTest {
 @Test public void twoJobsReportProgressAndOneFailureDoesNotStopTheOther()throws Exception{
  Assume.assumeTrue("true".equals(InstrumentationRegistry.getArguments().getString("liveDownload")));
  Instrumentation test=InstrumentationRegistry.getInstrumentation();Context c=test.getTargetContext();assertTrue("Choose a real folder first",DownloadFolder.hasAccess(c));
  String good="YE7VzlLtp-4",bad="___________";File before=MusicRepository.downloaded(c,good,"mp3");Set<String> previous=new HashSet<>();for(DownloadQueue.Job j:DownloadService.jobs(c))previous.add(j.key);
  Activity a=test.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
  try{
   test.runOnMainSync(()->{c.startForegroundService(new Intent(c,DownloadService.class).putExtra("id",good).putExtra("title","Creative Commons download test").putExtra("format","mp3"));c.startForegroundService(new Intent(c,DownloadService.class).putExtra("id",bad).putExtra("title","Unavailable video test").putExtra("format","mp3"));});
   long deadline=System.currentTimeMillis()+180000;DownloadQueue.Job success=null,failure=null;boolean parallel=false,progress=false;
   while(System.currentTimeMillis()<deadline){int running=0;for(DownloadQueue.Job j:DownloadService.jobs(c))if(!previous.contains(j.key)){if(j.id.equals(good))success=j;if(j.id.equals(bad))failure=j;if("running".equals(j.state))running++;if(j.id.equals(good)&&j.progress>=0)progress=true;}parallel|=running==2;if(success!=null&&failure!=null&&!success.pending()&&!failure.pending())break;Thread.sleep(100);}
   assertNotNull(success);assertNotNull(failure);assertTrue("Both jobs ran independently",parallel);assertTrue("Real download progress was reported",progress);assertEquals("done",success.state);assertEquals("failed",failure.state);File audio=MusicRepository.downloaded(c,good,"mp3");assertNotNull(audio);Uri saved=DownloadFolder.savedUri(c,audio);assertNotNull(saved);try(InputStream in=c.getContentResolver().openInputStream(saved)){assertNotNull(in);byte[] b=new byte[32768];long count=0;int n;while((n=in.read(b))!=-1)count+=n;assertEquals(audio.length(),count);}
  }finally{if(DownloadService.busy)test.runOnMainSync(()->c.startService(new Intent(c,DownloadService.class).setAction("cancel")));File f=MusicRepository.downloaded(c,good,"mp3");if(before==null&&f!=null){Uri saved=DownloadFolder.savedUri(c,f);if(saved!=null)DocumentsContract.deleteDocument(c.getContentResolver(),saved);f.delete();}test.runOnMainSync(a::finish);}
 }
}
