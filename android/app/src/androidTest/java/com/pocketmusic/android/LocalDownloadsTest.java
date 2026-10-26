package com.pocketmusic.android;
import android.content.*;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import java.io.*;
import java.util.*;
import static org.junit.Assert.*;
public class LocalDownloadsTest {
 private Context context;private File file;private TestMusicFixtures.Snapshot snapshot;
 @Before public void setup()throws Exception{context=InstrumentationRegistry.getInstrumentation().getTargetContext();snapshot=new TestMusicFixtures.Snapshot(context);file=new File(MusicRepository.folder(context),"Delete fixture [abcdefghijk].mp3");try(FileOutputStream out=new FileOutputStream(file)){out.write(new byte[32]);}}
 @After public void cleanup(){if(file!=null)file.delete();if(snapshot!=null)snapshot.restore();}
 @Test public void removeCleansLocalHistoryButKeepsCatalogueLike()throws Exception{Track song=TestMusicFixtures.song("abcdefghijk","Test song","Test artist");if(!MusicTaste.liked(context,song.id))MusicTaste.toggleLike(context,song);RecentHistory.touch(context,"recentTracks",file.getAbsolutePath());context.getSharedPreferences("music",0).edit().putString("lastPlayed",file.getAbsolutePath()).commit();LocalDownloads.remove(context,file,false);assertFalse(file.exists());assertTrue(MusicTaste.liked(context,song.id));assertFalse(RecentHistory.read(context,"recentTracks").contains(file.getAbsolutePath()));assertFalse(context.getSharedPreferences("music",0).contains("lastPlayed"));}
 @Test public void outsideLibraryIsNeverDeleted()throws Exception{File outside=File.createTempFile("keep-",".mp3",context.getCacheDir());try{try{LocalDownloads.remove(context,outside,false);fail();}catch(IOException expected){}assertTrue(outside.exists());assertTrue(file.exists());}finally{outside.delete();}}
 @Test public void unknownFileTypeIsNeverDeleted()throws Exception{File other=new File(MusicRepository.folder(context),"keep.txt");try{try(FileOutputStream out=new FileOutputStream(other)){out.write(1);}try{LocalDownloads.remove(context,other,false);fail();}catch(IOException expected){}assertTrue(other.exists());}finally{other.delete();}}
 @Test public void missingDownloadReportsFailure()throws Exception{assertTrue(file.delete());try{LocalDownloads.remove(context,file,false);fail();}catch(IOException expected){assertTrue(expected.getMessage().contains("no longer"));}}
}
