package com.pocketmusic.android;
import android.content.*;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import static org.junit.Assert.*;
public class DiscoveryMetadataTest {
 private Context context;private File root;private String prefs;
 @Before public void setup()throws Exception{Context app=InstrumentationRegistry.getInstrumentation().getTargetContext();root=Files.createTempDirectory(app.getCacheDir().toPath(),"discovery-test-").toFile();prefs="discovery-test-"+UUID.randomUUID();context=new ContextWrapper(app){@Override public File getNoBackupFilesDir(){return root;}@Override public File getCacheDir(){return root;}@Override public SharedPreferences getSharedPreferences(String name,int mode){return super.getSharedPreferences(prefs,mode);}};}
 @After public void cleanup(){context.getSharedPreferences("music",0).edit().clear().commit();File[] files=root.listFiles();if(files!=null)for(File f:files)f.delete();root.delete();context.deleteSharedPreferences(prefs);}
 private void cache(String scope)throws Exception{context.getSharedPreferences("music",0).edit().putString("feedScope",scope).putString("feedTracks",TrackMetadata.encode(List.of(new Track("abcdefghijk","Quiet song","Artist",191)))).putLong("feedTime",System.currentTimeMillis()).putString("feedBasis",Recommendations.seed(context)).commit();}
 @Test public void parserDeduplicatesAndRejectsUnavailableEntries()throws Exception{String json="{\"entries\":[{\"id\":\"abcdefghijk\",\"title\":\"Music\",\"uploader\":\"Artist\",\"duration\":191},{\"id\":\"abcdefghijk\",\"title\":\"Duplicate\"},{\"id\":\"12345678901\",\"title\":\"[Private video]\"},{\"id\":\"short\",\"title\":\"Bad id\"}]}";List<Track> tracks=TrackMetadata.parse(json,12);assertEquals(1,tracks.size());assertEquals("Artist · 3:11",tracks.get(0).subtitle());assertEquals("Music",TrackMetadata.parse(TrackMetadata.encode(tracks),12).get(0).title);}
 @Test public void accountCacheNeverLeaksAcrossReconnectOrDisconnect()throws Exception{YouTubeSession.save(context,"SAPISID=synthetic-one");String first=Recommendations.scope(context);cache(first);assertEquals(1,Recommendations.cached(context).size());assertTrue(Recommendations.account(context));YouTubeSession.save(context,"SAPISID=synthetic-two");assertNotEquals(first,Recommendations.scope(context));assertTrue(Recommendations.cached(context).isEmpty());cache(Recommendations.scope(context));YouTubeSession.disconnect(context);assertTrue(Recommendations.cached(context).isEmpty());assertFalse(Recommendations.account(context));}
 @Test public void changedListeningSeedRefreshesPublicPicksWithoutRelabelingCache()throws Exception{cache("guest");assertTrue(Recommendations.fresh(context));context.getSharedPreferences("music",0).edit().putString("recommendationSeed","a new artist").commit();assertFalse(Recommendations.fresh(context));assertTrue(Recommendations.basis(context).contains("popular music"));assertEquals(1,Recommendations.cached(context).size());}
 @Test public void futureAccountCachesAreNotConsideredFresh()throws Exception{cache("guest");context.getSharedPreferences("music",0).edit().putLong("feedTime",System.currentTimeMillis()+86400000L).commit();assertFalse(Recommendations.fresh(context));}
}
