package com.pocketmusic.android;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import java.io.*;
import java.util.*;
import static org.junit.Assert.*;
/** Opt-in SAF deletion checks, using only the task emulator's dedicated test folder. */
public class DeleteSavedFolderTest {
 @Test public void defaultKeepsSavedCopyAndExplicitDeleteTouchesOnlyTrackedDocument()throws Exception{
  Assume.assumeTrue("Dedicated emulator folder only", "true".equals(InstrumentationRegistry.getArguments().getString("testDeleteFolder")));Instrumentation test=InstrumentationRegistry.getInstrumentation();Context context=test.getTargetContext();if(android.os.Build.VERSION.SDK_INT>=33)test.getUiAutomation().grantRuntimePermission(context.getPackageName(),android.Manifest.permission.POST_NOTIFICATIONS);TestMusicFixtures.Snapshot snapshot=new TestMusicFixtures.Snapshot(context);MainActivity[] activity={null};List<Uri> created=new ArrayList<>();File file=new File(MusicRepository.folder(context),"Delete SAF fixture.mp3");
  try{try(FileOutputStream out=new FileOutputStream(file)){out.write(new byte[64]);}TestMusicFixtures.seed(context,List.of(TestMusicFixtures.song("abcdefghijk","Test song","Artist")));activity[0]=(MainActivity)test.startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));Uri initial=DocumentsContract.buildDocumentUri("com.android.externalstorage.documents","primary:Music/PocketMusicDeleteTest");test.runOnMainSync(()->activity[0].startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).putExtra(DocumentsContract.EXTRA_INITIAL_URI,initial).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION|Intent.FLAG_GRANT_PREFIX_URI_PERMISSION),42));click(test,"Use this folder");click(test,"Allow");long end=System.currentTimeMillis()+10000;while(!DownloadFolder.hasAccess(context)&&System.currentTimeMillis()<end)Thread.sleep(100);assertTrue(DownloadFolder.hasAccess(context));Thread.sleep(700);Uri kept=DownloadFolder.save(context,file);created.add(kept);LocalDownloads.remove(context,file,false);assertFalse(file.exists());try(InputStream in=context.getContentResolver().openInputStream(kept)){assertNotNull(in);assertEquals(64,in.readAllBytes().length);}
   try(FileOutputStream out=new FileOutputStream(file)){out.write(new byte[96]);}Uri tracked=DownloadFolder.save(context,file);created.add(tracked);assertNotEquals(kept,tracked);LocalDownloads.remove(context,file,true);assertFalse(file.exists());boolean missing=false;try(InputStream in=context.getContentResolver().openInputStream(tracked)){missing=in==null;}catch(FileNotFoundException|IllegalArgumentException expected){missing=true;}assertTrue(missing);try(InputStream in=context.getContentResolver().openInputStream(kept)){assertNotNull(in);assertEquals(64,in.readAllBytes().length);}
  }finally{for(Uri uri:created)try{DocumentsContract.deleteDocument(context.getContentResolver(),uri);}catch(Exception ignored){}test.runOnMainSync(()->{if(activity[0]!=null)activity[0].finish();});file.delete();snapshot.restore();}
 }
 private void click(Instrumentation test,String text)throws Exception{long end=System.currentTimeMillis()+10000;while(System.currentTimeMillis()<end){AccessibilityNodeInfo found=find(test.getUiAutomation().getRootInActiveWindow(),text);if(found!=null){while(!found.isClickable()&&found.getParent()!=null)found=found.getParent();assertTrue(found.performAction(AccessibilityNodeInfo.ACTION_CLICK));Thread.sleep(500);return;}Thread.sleep(100);}fail("Missing picker action: "+text);}
 private AccessibilityNodeInfo find(AccessibilityNodeInfo node,String text){if(node==null)return null;if(node.getText()!=null&&node.getText().toString().equalsIgnoreCase(text))return node;for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo value=find(node.getChild(i),text);if(value!=null)return value;}return null;}
}
