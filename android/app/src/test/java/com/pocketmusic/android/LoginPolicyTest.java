package com.pocketmusic.android;
import org.junit.Test;
import static org.junit.Assert.*;
public class LoginPolicyTest {
 @Test public void googleSessionHandoffAndConsentRoutesAreAllowed(){for(String url:new String[]{"https://accounts.google.com/signin/v2/challenge","https://www.google.com/accounts/SetSID","https://accounts.youtube.com/accounts/SetSID","https://consent.google.com/m","https://consent.youtube.com/m","https://www.youtube.com/","https://music.youtube.com/"})assertTrue(url,LoginPolicy.allowed(url));}
 @Test public void handoffCannotBecomeAnArbitraryWebBrowser(){for(String url:new String[]{"https://evil.com/","https://accounts.google.com.evil.com/","https://youtube.com.evil.com/","https://www.google.com/search?q=test","https://www.google.com/accounts-fake/","http://accounts.google.com/","javascript:alert(1)","file:///secret","https://accounts.google.com@evil.com/","https://evil@youtube.com/","https://youtube.com:8080/","not a url"})assertFalse(url,LoginPolicy.allowed(url));}
 @Test public void authenticationCookieWithoutMusicSigningMaterialIsNotReady(){assertFalse(LoginPolicy.ready("__Secure-3PSID=synthetic; PREF=anonymous"));assertFalse(LoginPolicy.ready("PREF=anonymous"));assertFalse(LoginPolicy.ready(null));}
 @Test public void completedSessionCanBeSavedWithoutDependingOnPageLoad(){assertTrue(LoginPolicy.ready("SAPISID=synthetic; __Secure-3PSID=synthetic"));assertTrue(LoginPolicy.ready("__Secure-3PAPISID=synthetic; __Secure-3PSID=synthetic"));}
 @Test public void emptyOrInjectedAuthenticationIsNotReady(){assertFalse(LoginPolicy.ready("SAPISID="));assertFalse(LoginPolicy.ready("SAPISID=bad\r\nheader"));assertFalse(LoginPolicy.ready("SAPISID=bad\r\nheader; __Secure-3PSID=synthetic"));assertFalse(LoginPolicy.ready("SAPISID= ; PREF=guest"));}
}
