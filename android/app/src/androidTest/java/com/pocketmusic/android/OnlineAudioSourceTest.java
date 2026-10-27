package com.pocketmusic.android;
import org.junit.Test;
import static org.junit.Assert.*;
public class OnlineAudioSourceTest {
 @Test public void resolverRetainsSafeHeadersAndNeverForwardsAccountCookies()throws Exception{OnlineAudio.Source source=OnlineAudio.parse("{\"url\":\"https://rr1.googlevideo.com/audio?sig=example\",\"vcodec\":\"none\",\"http_headers\":{\"User-Agent\":\"Example player\",\"Cookie\":\"private\",\"Authorization\":\"private\",\"Origin\":\"https://youtube.com\",\"Referer\":\"bad\\r\\nvalue\"}}");assertEquals("Example player",source.headers.get("User-Agent"));assertFalse(source.headers.containsKey("Cookie"));assertFalse(source.headers.containsKey("Authorization"));assertFalse(source.headers.containsKey("Referer"));}
 @Test public void rejectsVideoAndUnsupportedUrls()throws Exception{for(String value:new String[]{"{\"url\":\"https://rr1.googlevideo.com/audio\",\"vcodec\":\"avc1\"}","{\"url\":\"file:///sdcard/file.mp3\",\"vcodec\":\"none\"}","{}"}){boolean failed=false;try{OnlineAudio.parse(value);}catch(Exception expected){failed=true;}assertTrue(failed);}}
}
