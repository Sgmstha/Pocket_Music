package com.pocketmusic.android;
import org.junit.Test;
import static org.junit.Assert.*;
public class SessionAuthPolicyTest {
 private final String cookie="SAPISID=synthetic; __Secure-3PSID=synthetic";
 @Test public void selectedAccountAndChannelArePreserved(){SessionAuth auth=SessionAuth.capture(cookie,"visitor","12345||identity","2","1.test");assertEquals("12345",auth.dataSyncId);assertEquals("2",auth.authUser);assertTrue(auth.complete());assertEquals("Alice",auth.identity(" Alice ").accountName);}
 @Test(expected=IllegalArgumentException.class) public void absentVisitorCannotBecomeACompleteSession(){SessionAuth.capture(cookie,"","","0","1.test");}
 @Test(expected=IllegalArgumentException.class) public void invalidAccountIndexCannotEnterRequestHeaders(){SessionAuth.capture(cookie,"visitor","","2\r\ninjected","1.test");}
 @Test(expected=IllegalArgumentException.class) public void missingAccountIdentityCannotBeConfirmed(){SessionAuth.capture(cookie,"visitor","","0","1.test").identity(" ");}
 @Test public void configurationReadIsRestrictedToExactMusicOrigin(){assertTrue(MusicLoginFlow.trusted("https://music.youtube.com/"));assertFalse(MusicLoginFlow.trusted("https://music.youtube.com.evil.com/"));assertFalse(MusicLoginFlow.trusted("https://accounts.google.com/"));assertFalse(MusicLoginFlow.trusted("http://music.youtube.com/"));assertFalse(MusicLoginFlow.trusted("https://evil@music.youtube.com/"));}
}
