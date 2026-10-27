package com.pocketmusic.android;
import org.junit.Test;
import static org.junit.Assert.*;
public class StreamUrlPolicyTest {
 @Test public void allowsSignedHttpsCdnAudio(){assertTrue(StreamUrlPolicy.allowed("https://rr1---sn-example.googlevideo.com/videoplayback?expire=123&sig=example"));assertTrue(StreamUrlPolicy.allowed("https://RR1.GOOGLEVIDEO.COM:443/audio"));}
 @Test public void rejectsLocalAndUnencryptedMedia(){for(String url:new String[]{"file:///sdcard/song.mp3","content://example/audio","http://rr1.googlevideo.com/audio","http://127.0.0.1/audio"})assertFalse(StreamUrlPolicy.allowed(url));}
 @Test public void rejectsLookalikesCredentialsAndPorts(){for(String url:new String[]{"https://googlevideo.com.evil.test/audio","https://evilgooglevideo.com/audio","https://user:secret@rr1.googlevideo.com/audio","https://rr1.googlevideo.com:444/audio","https://rr1.googlevideo.com/\r\naudio"})assertFalse(StreamUrlPolicy.allowed(url));}
 @Test public void rejectsEmptyMalformedAndOversizedValues(){assertFalse(StreamUrlPolicy.allowed(null));assertFalse(StreamUrlPolicy.allowed(""));assertFalse(StreamUrlPolicy.allowed("not a url"));assertFalse(StreamUrlPolicy.allowed("https://rr1.googlevideo.com/"+"a".repeat(32768)));}
}
