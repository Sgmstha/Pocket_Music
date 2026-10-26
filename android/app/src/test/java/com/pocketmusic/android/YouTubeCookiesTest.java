package com.pocketmusic.android;
import org.junit.Test;
import static org.junit.Assert.*;
public class YouTubeCookiesTest {
 @Test public void sessionFileIsScopedToYoutube(){String text=YouTubeCookies.netscape("SAPISID=fake; PREF=test");assertTrue(text.startsWith("# Netscape HTTP Cookie File\n"));assertTrue(text.contains(".youtube.com\tTRUE\t/\tTRUE\t0\tSAPISID\tfake\n"));assertFalse(text.contains("google.com"));}
 @Test(expected=IllegalArgumentException.class) public void anonymousCookiesRejected(){YouTubeCookies.netscape("PREF=value");}
 @Test(expected=IllegalArgumentException.class) public void emptyAuthenticationRejected(){YouTubeCookies.netscape("SAPISID=");}
 @Test public void newlineInjectionDropped(){String text=YouTubeCookies.netscape("SAPISID=fake; PREF=bad\n.evil.com\tTRUE");assertFalse(text.contains("evil.com"));}
 @Test public void rateLimitsAndLoginNeverTriggerEngineRetry(){assertFalse(DownloadErrors.recoverable("HTTP 429 signature challenge"));assertFalse(DownloadErrors.recoverable("403 Sign in to confirm you're not a bot"));assertTrue(DownloadErrors.friendly("429 Sign in").contains("limiting"));assertTrue(DownloadErrors.friendly("Sign in to confirm").contains("YouTube account"));}
 @Test public void logsHideAuthentication(){assertFalse(DownloadErrors.redact("hello\nCookie: SAPISID=secret\nfinished").contains("secret"));}
}
