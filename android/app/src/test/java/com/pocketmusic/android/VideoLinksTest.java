package com.pocketmusic.android;
import org.junit.Test;
import static org.junit.Assert.*;
public class VideoLinksTest {
 @Test public void acceptsSupportedLinks(){for(String s:new String[]{"https://youtu.be/abcdefghijk?t=2","https://music.youtube.com/watch?v=abcdefghijk&list=x","https://www.youtube.com/shorts/abcdefghijk","abcdefghijk"})assertEquals("abcdefghijk",VideoLinks.id(s));}
 @Test public void rejectsUntrustedOrMalformed(){for(String s:new String[]{"http://youtube.com/watch?v=abcdefghijk","https://youtube.com.evil.org/watch?v=abcdefghijk","https://youtube.com@evil.org/watch?v=abcdefghijk","https://youtube.com:444/watch?v=abcdefghijk","https://youtube.com/playlist?list=123","--exec bad","https://youtu.be/short"})assertNull(s,VideoLinks.id(s));}
}
