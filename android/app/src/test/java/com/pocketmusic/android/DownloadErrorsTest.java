package com.pocketmusic.android;
import org.junit.Test;
import static org.junit.Assert.*;
public class DownloadErrorsTest {
 @Test public void oldWarningDoesNotHideActualError(){String log="WARNING: old yt-dlp version\nWARNING: SABR\nERROR: HTTP Error 403: Forbidden";assertTrue(DownloadErrors.recoverable(log));assertTrue(DownloadErrors.friendly(log).contains("YouTube refused"));}
 @Test public void offlineIsActionable(){assertTrue(DownloadErrors.friendly("Unable to resolve host").contains("internet"));assertFalse(DownloadErrors.recoverable("Unable to resolve host"));}
 @Test public void privateDoesNotRetry(){assertFalse(DownloadErrors.recoverable("Private video"));assertTrue(DownloadErrors.friendly("Private video").contains("private"));}
}
