package com.pocketmusic.android;
import org.junit.Test;
import static org.junit.Assert.*;
public class OnlineAudioSourceTest {
 @Test public void resolverRetainsSafeHeadersAndNeverForwardsAccountCookies()throws Exception{OnlineAudio.Source source=OnlineAudio.parse("{\"url\":\"https://rr1.googlevideo.com/audio?sig=example\",\"vcodec\":\"none\",\"http_headers\":{\"User-Agent\":\"Example player\",\"Cookie\":\"private\",\"Authorization\":\"private\",\"Origin\":\"https://youtube.com\",\"Referer\":\"bad\\r\\nvalue\"}}");assertEquals("Example player",source.headers.get("User-Agent"));assertFalse(source.headers.containsKey("Cookie"));assertFalse(source.headers.containsKey("Authorization"));assertFalse(source.headers.containsKey("Referer"));}
 @Test public void rejectsVideoAndUnsupportedUrls()throws Exception{for(String value:new String[]{"{\"url\":\"https://rr1.googlevideo.com/audio\",\"vcodec\":\"avc1\"}","{\"url\":\"file:///sdcard/file.mp3\",\"vcodec\":\"none\"}","{}"}){boolean failed=false;try{OnlineAudio.parse(value);}catch(Exception expected){failed=true;}assertTrue(failed);}}
 @Test public void signedSourcesExpireAndMemoryCacheIsBounded()throws Exception{
  android.content.Context context=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getTargetContext();
  java.lang.reflect.Method remember=OnlineAudio.class.getDeclaredMethod("remember",android.content.Context.class,String.class,OnlineAudio.Source.class),cached=OnlineAudio.class.getDeclaredMethod("cached",android.content.Context.class,String.class);remember.setAccessible(true);cached.setAccessible(true);
  java.lang.reflect.Field field=OnlineAudio.class.getDeclaredField("CACHE");field.setAccessible(true);java.util.Map<?,?> cache=(java.util.Map<?,?>)field.get(null);
  try{cache.clear();OnlineAudio.Source valid=new OnlineAudio.Source("https://rr1.googlevideo.com/audio",java.util.Map.of());remember.invoke(null,context,"test",valid);assertSame(valid,cached.invoke(null,context,"test"));OnlineAudio.forget(context,"test");assertNull(cached.invoke(null,context,"test"));
   remember.invoke(null,context,"expired",new OnlineAudio.Source("https://rr1.googlevideo.com/audio?expire=1",java.util.Map.of()));assertNull(cached.invoke(null,context,"expired"));for(int n=0;n<12;n++)remember.invoke(null,context,"song"+n,valid);assertEquals(8,cache.size());assertNull(cached.invoke(null,context,"song0"));assertSame(valid,cached.invoke(null,context,"song11"));
  }finally{cache.clear();}
 }
}
