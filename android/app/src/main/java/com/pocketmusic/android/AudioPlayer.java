package com.pocketmusic.android;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import java.util.Map;
import androidx.media3.common.*;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.*;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;

/** Keeps saved-file playback intact, with a small streaming buffer for online audio. */
@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
final class AudioPlayer {
 interface Prepared {void call(AudioPlayer player);}
 interface Completion {void call(AudioPlayer player);}
 interface Error {boolean call(AudioPlayer player,int what,int extra);}
 interface Info {boolean call(AudioPlayer player,int what,int extra);}
 private final MediaPlayer local;
 private final ExoPlayer stream;
 private final DefaultHttpDataSource.Factory http;
 private Prepared prepared;private Completion completion;private Error error;private Info info;
 private boolean notified;
 AudioPlayer(Context context,boolean online){
  if(!online){local=new MediaPlayer();stream=null;http=null;return;}
  local=null;http=new DefaultHttpDataSource.Factory().setConnectTimeoutMs(10000).setReadTimeoutMs(10000).setAllowCrossProtocolRedirects(false);
  stream=new ExoPlayer.Builder(context).setMediaSourceFactory(new DefaultMediaSourceFactory(context).setDataSourceFactory(http))
   .setLoadControl(new DefaultLoadControl.Builder().setBufferDurationsMs(3000,15000,500,1500).setTargetBufferBytes(4*1024*1024).setPrioritizeTimeOverSizeThresholds(true).build()).build();
  stream.setAudioAttributes(new androidx.media3.common.AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),false);
  stream.setWakeMode(C.WAKE_MODE_LOCAL);
  stream.addListener(new Player.Listener(){
   @Override public void onPlaybackStateChanged(int state){
    if(state==Player.STATE_READY){if(!notified){notified=true;if(prepared!=null)prepared.call(AudioPlayer.this);}else if(info!=null)info.call(AudioPlayer.this,MediaPlayer.MEDIA_INFO_BUFFERING_END,0);}
    else if(state==Player.STATE_BUFFERING&&notified&&info!=null)info.call(AudioPlayer.this,MediaPlayer.MEDIA_INFO_BUFFERING_START,0);
    else if(state==Player.STATE_ENDED&&completion!=null)completion.call(AudioPlayer.this);
   }
   @Override public void onIsPlayingChanged(boolean playing){if(notified&&info!=null)info.call(AudioPlayer.this,MediaPlayer.MEDIA_INFO_BUFFERING_END,0);}
   @Override public void onPlayerError(PlaybackException failure){if(error!=null)error.call(AudioPlayer.this,failure.errorCode,0);}
  });
 }
 void setAudioAttributes(AudioAttributes attrs){if(local!=null)local.setAudioAttributes(attrs);}
 void setWakeMode(Context context,int mode){if(local!=null)local.setWakeMode(context,mode);}
 void setDataSource(String path)throws Exception{local.setDataSource(path);}
 void setDataSource(Context context,Uri uri,Map<String,String> headers)throws Exception{if(local!=null)local.setDataSource(context,uri,headers);else{http.setDefaultRequestProperties(headers);stream.setMediaItem(MediaItem.fromUri(uri));}}
 void setOnPreparedListener(Prepared callback){prepared=callback;if(local!=null)local.setOnPreparedListener(p->callback.call(this));}
 void setOnCompletionListener(Completion callback){completion=callback;if(local!=null)local.setOnCompletionListener(p->callback.call(this));}
 void setOnErrorListener(Error callback){error=callback;if(local!=null)local.setOnErrorListener((p,w,e)->callback.call(this,w,e));}
 void setOnInfoListener(Info callback){info=callback;if(local!=null)local.setOnInfoListener((p,w,e)->callback.call(this,w,e));}
 void prepareAsync(){if(local!=null)local.prepareAsync();else stream.prepare();}
 void start(){if(local!=null)local.start();else stream.play();}
 void pause(){if(local!=null)local.pause();else stream.pause();}
 void seekTo(int position){if(local!=null)local.seekTo(position);else stream.seekTo(position);}
 void setVolume(float left,float right){if(local!=null)local.setVolume(left,right);else stream.setVolume(left);}
 boolean isPlaying(){return local!=null?local.isPlaying():query(()->stream.isPlaying());}
 int getCurrentPosition(){return local!=null?local.getCurrentPosition():query(()->(int)Math.min(Integer.MAX_VALUE,stream.getCurrentPosition()));}
 int getDuration(){return local!=null?local.getDuration():query(()->(int)Math.max(0,Math.min(Integer.MAX_VALUE,stream.getDuration())));}
 private <T> T query(java.util.concurrent.Callable<T> read){try{if(android.os.Looper.myLooper()==stream.getApplicationLooper())return read.call();java.util.concurrent.FutureTask<T> task=new java.util.concurrent.FutureTask<>(read);new android.os.Handler(stream.getApplicationLooper()).post(task);return task.get(2,java.util.concurrent.TimeUnit.SECONDS);}catch(Exception e){throw new IllegalStateException("Player query unavailable",e);}}
 void release(){if(local!=null)local.release();else stream.release();}
}
