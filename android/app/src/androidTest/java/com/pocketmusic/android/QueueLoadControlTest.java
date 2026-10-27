package com.pocketmusic.android;
import androidx.media3.common.Timeline;
import androidx.media3.exoplayer.LoadControl;
import androidx.media3.exoplayer.analytics.PlayerId;
import org.junit.Test;
import static org.junit.Assert.*;

@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
public class QueueLoadControlTest {
 private final Timeline timeline=new androidx.media3.exoplayer.source.SinglePeriodTimeline(40000000L,true,false,false,null,androidx.media3.common.MediaItem.fromUri("https://fixture.googlevideo.com/audio"));
 private final androidx.media3.exoplayer.source.MediaSource.MediaPeriodId period=new androidx.media3.exoplayer.source.MediaSource.MediaPeriodId(timeline.getUidOfPeriod(0));
 @Test public void currentAudioReserveGatesPreloadingEvenWhileHttpRequestIsActive(){
  QueueLoadControl control=new QueueLoadControl();PlayerId current=new PlayerId("fixture");
  control.onPrepared(current);
  try{
   control.shouldContinueLoading(parameters(current,1000000,true,false));assertFalse(preload(control));
   control.shouldContinueLoading(parameters(current,4000000,true,false));assertTrue(preload(control));
   control.shouldContinueLoading(parameters(current,1000000,true,true));assertFalse(preload(control));
   control.shouldContinueLoading(parameters(current,4000000,false,false));assertFalse(preload(control));
  }finally{control.onReleased(current);}
 }
 private LoadControl.Parameters parameters(PlayerId id,long buffered,boolean play,boolean rebuffer){return new LoadControl.Parameters(id,timeline,period,0,buffered,1f,play,rebuffer,0,0);}
 private boolean preload(QueueLoadControl control){return control.shouldContinuePreloading(PlayerId.PRELOAD,timeline,period,0);}
}
