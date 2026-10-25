package com.pocketmusic.android;

import android.content.*;
import android.net.Uri;
import android.provider.DocumentsContract;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import java.io.*;
import java.util.*;
import static org.junit.Assert.*;

/** Runs against a folder actually authorized through Android's system picker. */
public class DownloadFolderTest {
 private Context context;private File file;private final List<Uri> created=new ArrayList<>();
 @Before public void setup()throws Exception{
  context=InstrumentationRegistry.getInstrumentation().getTargetContext();
  if("true".equals(InstrumentationRegistry.getArguments().getString("requireFolder")))assertTrue("A real persisted folder grant is required for release QA",DownloadFolder.hasAccess(context));else Assume.assumeTrue("Choose a download folder in the app before running SAF tests",DownloadFolder.hasAccess(context));
  file=new File(MusicRepository.folder(context),"Folder test नेपाली "+System.nanoTime()+".mp3");
  try(FileOutputStream out=new FileOutputStream(file)){out.write("A saved download survives outside the app.\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));}
 }
 @After public void cleanup()throws Exception{if(context!=null)for(Uri u:created)try{DocumentsContract.deleteDocument(context.getContentResolver(),u);}catch(Exception ignored){}if(file!=null)file.delete();}
 @Test public void selectedFolderPersistsAndDuplicateSaveIsIdempotent()throws Exception{
  Uri first=DownloadFolder.save(context,file);created.add(first);assertEquals(first,DownloadFolder.savedUri(context,file));assertEquals(first,DownloadFolder.save(context,file));
  byte[] expected=java.nio.file.Files.readAllBytes(file.toPath());try(InputStream in=context.getContentResolver().openInputStream(first)){assertNotNull(in);assertArrayEquals(expected,in.readAllBytes());}
  assertTrue(DownloadFolder.hasAccess(context));assertTrue(file.isFile());
 }
 @Test public void missingDestinationKeepsPlaybackCopyAndCanRetry()throws Exception{
  SharedPreferences preferences=context.getSharedPreferences("music",0);String previous=preferences.getString(DownloadFolder.KEY,"");preferences.edit().remove(DownloadFolder.KEY).commit();
  try{try{DownloadFolder.save(context,file);fail("Missing destination must not report success");}catch(IOException expected){}assertTrue(file.isFile());assertNull(DownloadFolder.savedUri(context,file));}
  finally{preferences.edit().putString(DownloadFolder.KEY,previous).commit();}
  Uri saved=DownloadFolder.save(context,file);created.add(saved);assertNotNull(saved);
 }
 @Test public void retryRestoresADeletedDestinationFile()throws Exception{
  Uri first=DownloadFolder.save(context,file);created.add(first);assertTrue(DocumentsContract.deleteDocument(context.getContentResolver(),first));
  Uri restored=DownloadFolder.save(context,file);created.add(restored);try(InputStream in=context.getContentResolver().openInputStream(restored)){assertNotNull(in);assertEquals(file.length(),in.readAllBytes().length);}
 }
 @Test public void aSameNameUserFileIsNeverOverwritten()throws Exception{
  Uri tree=DownloadFolder.selected(context);Uri parent=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));
  Uri original=DocumentsContract.createDocument(context.getContentResolver(),parent,"audio/mpeg",file.getName());assertNotNull(original);created.add(original);
  byte[] content="Existing user file".getBytes();try(OutputStream out=context.getContentResolver().openOutputStream(original,"wt")){assertNotNull(out);out.write(content);}
  Uri saved=DownloadFolder.save(context,file);created.add(saved);assertNotEquals(original,saved);
  try(InputStream in=context.getContentResolver().openInputStream(original)){assertNotNull(in);assertArrayEquals(content,in.readAllBytes());}
 }
}
