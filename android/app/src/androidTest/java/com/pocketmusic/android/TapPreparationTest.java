package com.pocketmusic.android;
import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;

public class TapPreparationTest {
 private Context context;private java.util.function.Predicate<Context> policy;private OnlineAudio.Resolver resolver;
 private final Track first=new Track("abcdefghijk","First","Artist",40,true,""),second=new Track("12345678901","Second","Artist",40,true,""),third=new Track("abcdefghij2","Third","Artist",40,true,"");
 @Before public void setup(){context=InstrumentationRegistry.getInstrumentation().getTargetContext();policy=TapPreparation.allowed;resolver=TapPreparation.resolver;TapPreparation.clear();TapPreparation.allowed=c->true;}
 @After public void cleanup(){TapPreparation.clear();TapPreparation.allowed=policy;TapPreparation.resolver=resolver;}
 @Test public void twoResultsOnlyAndClaimedPlaySurvivesLeavingScreen()throws Exception{
  AtomicInteger calls=new AtomicInteger();CountDownLatch entered=new CountDownLatch(2),release=new CountDownLatch(1),otherCancelled=new CountDownLatch(1);OnlineAudio.Source source=new OnlineAudio.Source("https://fixture.googlevideo.com/audio",Map.of());
  TapPreparation.resolver=(c,t,p)->{calls.incrementAndGet();entered.countDown();try{release.await(5,TimeUnit.SECONDS);}catch(InterruptedException e){if(t.id.equals(second.id))otherCancelled.countDown();throw e;}return source;};
  TapPreparation.plan(context,"screen",List.of(first,second,third));assertTrue(entered.await(5,TimeUnit.SECONDS));TapPreparation.prioritize(context,"screen",first);TapPreparation.cancel("screen");assertTrue(otherCancelled.await(5,TimeUnit.SECONDS));release.countDown();Thread.sleep(100);TapPreparation.plan(context,"screen",List.of(first));TapPreparation.Result result=TapPreparation.take(context,first);assertSame(source,result.source());assertFalse(result.failed());assertEquals(2,calls.get());
 }
 @Test public void failedPreparationIsKnownAndPolicyExclusionDoesNoWork()throws Exception{
  AtomicInteger calls=new AtomicInteger();CountDownLatch done=new CountDownLatch(1);TapPreparation.resolver=(c,t,p)->{calls.incrementAndGet();done.countDown();throw new java.io.IOException("Fixture failure");};TapPreparation.allowed=c->false;TapPreparation.plan(context,"screen",List.of(first));Thread.sleep(100);assertEquals(0,calls.get());TapPreparation.allowed=c->true;TapPreparation.plan(context,"screen",List.of(first));assertTrue(done.await(5,TimeUnit.SECONDS));TapPreparation.Result result=TapPreparation.take(context,first);assertTrue(result.failed());assertNull(result.source());assertEquals(1,calls.get());
 }
 @Test public void leavingUnclaimedResultsCancelsTheirWork()throws Exception{
  CountDownLatch entered=new CountDownLatch(1),cancelled=new CountDownLatch(1);TapPreparation.resolver=(c,t,p)->{entered.countDown();try{Thread.sleep(10000);}catch(InterruptedException e){cancelled.countDown();throw e;}return null;};TapPreparation.plan(context,"screen",List.of(first));assertTrue(entered.await(5,TimeUnit.SECONDS));TapPreparation.cancel("screen");assertTrue(cancelled.await(5,TimeUnit.SECONDS));assertNull(TapPreparation.take(context,first).source());
 }
}
