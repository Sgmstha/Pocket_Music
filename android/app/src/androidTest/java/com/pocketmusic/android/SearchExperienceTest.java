package com.pocketmusic.android;
import android.app.*;
import android.content.*;
import android.widget.EditText;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;
public class SearchExperienceTest {
 private boolean prepared;private Instrumentation test;private Context context;private MainActivity activity;private final CountDownLatch release=new CountDownLatch(1);private final Map<String,Object> prefs=new HashMap<>();
 private Object field(String name)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(activity);}
 private void invoke(String name)throws Exception{Method m=MainActivity.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(activity);}
 private void search(String query){test.runOnMainSync(()->{try{((EditText)field("query")).setText(query);invoke("startSearch");}catch(Exception e){throw new AssertionError(e);}});}
 private void await(java.util.function.BooleanSupplier condition)throws Exception{long end=System.currentTimeMillis()+7000;while(!condition.getAsBoolean()&&System.currentTimeMillis()<end)Thread.sleep(80);assertTrue(condition.getAsBoolean());}
 private boolean contains(String title){final boolean[] found={false};test.runOnMainSync(()->{try{for(Track t:(List<Track>)field("queue"))if(t.title.equals(title))found[0]=true;}catch(Exception e){throw new AssertionError(e);}});return found[0];}
 @Before public void setup()throws Exception{test=InstrumentationRegistry.getInstrumentation();context=test.getTargetContext();if(android.os.Build.VERSION.SDK_INT>=33)test.getUiAutomation().grantRuntimePermission(context.getPackageName(),android.Manifest.permission.POST_NOTIFICATIONS);Assume.assumeFalse("Never use a real account for test fixtures",YouTubeSession.connected(context));SharedPreferences p=context.getSharedPreferences("music",0);for(String k:List.of("feedScope","feedBasis","feedTracks","feedTime","recommendationSeed","recentQueries"))if(p.contains(k))prefs.put(k,p.getAll().get(k));prepared=true;p.edit().putString("feedScope","guest").putString("recommendationSeed","fixture music").putString("feedBasis","fixture music").putString("feedTracks",TrackMetadata.encode(List.of(new Track("abcdefghijk","Fixture song")))).putLong("feedTime",System.currentTimeMillis()).commit();activity=(MainActivity)test.startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));}
 @After public void cleanup(){release.countDown();if(activity!=null)test.runOnMainSync(activity::finish);if(context!=null&&prepared){SharedPreferences.Editor e=context.getSharedPreferences("music",0).edit();for(String k:List.of("feedScope","feedBasis","feedTracks","feedTime","recommendationSeed","recentQueries")){e.remove(k);Object old=prefs.get(k);if(old instanceof String)e.putString(k,(String)old);else if(old instanceof Long)e.putLong(k,(Long)old);}e.commit();}}
 @Test public void repeatedQueryUsesCacheWithoutNetwork()throws Exception{int[] calls={0};test.runOnMainSync(()->{activity.searchSource=(q,p)->{calls[0]++;return List.of();};try{((SearchResultCache)field("searchCache")).put("cached song",List.of(new Track("12345678901","Cached song")),android.os.SystemClock.elapsedRealtime());}catch(Exception e){throw new AssertionError(e);}});search("  CACHED SONG ");await(()->contains("Cached song"));assertEquals(0,calls[0]);}
 @Test public void supersededRequestCannotReplaceNewResults()throws Exception{CountDownLatch entered=new CountDownLatch(1);test.runOnMainSync(()->activity.searchSource=(q,p)->{if(q.equals("first")){entered.countDown();boolean waiting=true;while(waiting)try{release.await();waiting=false;}catch(InterruptedException ignored){}return List.of(new Track("12345678901","Old result"));}return List.of(new Track("abcdefghijk","New result"));});search("first");assertTrue(entered.await(5,TimeUnit.SECONDS));search("second");release.countDown();await(()->contains("New result"));assertFalse(contains("Old result"));}
}
