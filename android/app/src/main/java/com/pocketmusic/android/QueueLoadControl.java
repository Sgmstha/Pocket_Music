package com.pocketmusic.android;

import androidx.media3.common.Timeline;
import androidx.media3.exoplayer.*;
import androidx.media3.exoplayer.analytics.PlayerId;
import androidx.media3.exoplayer.source.MediaSource.MediaPeriodId;
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.exoplayer.trackselection.ExoTrackSelection;
import androidx.media3.exoplayer.upstream.Allocator;

/** A throttled current stream must not indefinitely starve one second of upcoming audio. */
@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
final class QueueLoadControl implements LoadControl {
 private final DefaultLoadControl playback=new DefaultLoadControl.Builder()
  .setBufferDurationsMs(3000,15000,500,1500).setTargetBufferBytes(4*1024*1024)
  .setPlayerTargetBufferBytes("preload",512*1024).setPrioritizeTimeOverSizeThresholds(true).build();
 private volatile boolean headroom;
 @Override public void onPrepared(PlayerId id){playback.onPrepared(id);}
 @Override public void onTracksSelected(Parameters p,TrackGroupArray groups,ExoTrackSelection[] selections){playback.onTracksSelected(p,groups,selections);}
 @Override public void onStopped(PlayerId id){headroom=false;playback.onStopped(id);}
 @Override public void onReleased(PlayerId id){headroom=false;playback.onReleased(id);}
 @Override public Allocator getAllocator(PlayerId id){return playback.getAllocator(id);}
 @Override public long getBackBufferDurationUs(PlayerId id){return playback.getBackBufferDurationUs(id);}
 @Override public boolean retainBackBufferFromKeyframe(PlayerId id){return playback.retainBackBufferFromKeyframe(id);}
 @Override public boolean shouldContinueLoading(Parameters p){if(!p.playerId.equals(PlayerId.PRELOAD))headroom=p.playWhenReady&&!p.rebuffering&&p.bufferedDurationUs>=3000000L;return playback.shouldContinueLoading(p);}
 @Override public boolean shouldStartPlayback(Parameters p){return playback.shouldStartPlayback(p);}
 @Override public boolean shouldContinuePreloading(PlayerId id,Timeline timeline,MediaPeriodId period,long bufferedUs){return playback.shouldContinuePreloading(id,timeline,period,bufferedUs)||headroom;}
}
