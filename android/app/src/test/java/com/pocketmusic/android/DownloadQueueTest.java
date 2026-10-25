package com.pocketmusic.android;
import org.junit.Test;
import static org.junit.Assert.*;
public class DownloadQueueTest {
 @Test public void onlyTwoDifferentVideosStartAndRemainingStayQueued(){DownloadQueue q=new DownloadQueue(2);q.enqueue("one","One","mp3");q.enqueue("two","Two","mp3");q.enqueue("three","Three","mp3");assertEquals(2,q.startable().size());assertEquals("queued",q.all().get(2).state);assertTrue(q.startable().isEmpty());}
 @Test public void duplicateActiveRequestUsesOneJob(){DownloadQueue q=new DownloadQueue(2);DownloadQueue.Job a=q.enqueue("one","One","mp3");assertSame(a,q.enqueue("one","Again","mp3"));assertEquals(1,q.all().size());}
 @Test public void sameVideoDifferentFormatsNeverCollide(){DownloadQueue q=new DownloadQueue(2);q.enqueue("one","One","mp3");q.enqueue("one","One","mp4");q.enqueue("two","Two","mp3");assertEquals(2,q.startable().size());assertEquals("queued",q.all().get(1).state);q.all().get(0).state="done";assertEquals(1,q.startable().size());}
 @Test public void cancellingWaitingJobDoesNotCancelOtherWork(){DownloadQueue q=new DownloadQueue(1);DownloadQueue.Job a=q.enqueue("one","One","mp3"),b=q.enqueue("two","Two","mp3");q.startable();q.cancel(b.key);assertEquals("running",a.state);assertEquals("cancelled",b.state);assertFalse(a.cancelled);}
 @Test public void oneFailureDoesNotBlockFollowingJob(){DownloadQueue q=new DownloadQueue(1);DownloadQueue.Job a=q.enqueue("one","One","mp3"),b=q.enqueue("two","Two","mp3");q.startable();a.state="failed";assertSame(b,q.startable().get(0));}
 @Test public void folderRetrySkipsNetworkButFailedDownloadDoesNot(){DownloadQueue q=new DownloadQueue(1);DownloadQueue.Job a=q.enqueue("one","One","mp3");a.state="saveFailed";assertTrue(q.retry(a.key));assertTrue(a.saveOnly);q.startable();a.state="failed";assertTrue(q.retry(a.key));assertFalse(a.saveOnly);}
 @Test public void finishedQueueBecomesIdle(){DownloadQueue q=new DownloadQueue(2);DownloadQueue.Job a=q.enqueue("one","One","mp3");q.startable();a.state="done";assertFalse(q.pending());assertTrue(q.startable().isEmpty());}
}
