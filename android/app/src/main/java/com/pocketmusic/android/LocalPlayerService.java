package com.pocketmusic.android;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.media.session.*;
import android.os.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public final class LocalPlayerService extends Service {
 public static LocalPlayerService active;
 private MediaPlayer player;
 private MediaSession session;
 private AudioManager audio;
 private AudioFocusRequest focus;
 private boolean ready,resumeOnGain,ducked;
 private final PlaybackOrder order=new PlaybackOrder();
 private final List<File> queue=new ArrayList<>();
 private final ExecutorService metadata=Executors.newSingleThreadExecutor();
 private final Handler main=new Handler(Looper.getMainLooper());
 private Future<?> coverJob;
 private final Runnable sleepCheck=()->{if(SleepTimer.consumeExpired(this)){pause();}else armSleepTimer();};
 public void armSleepTimer(){main.removeCallbacks(sleepCheck);long left=SleepTimer.remaining(this);if(left>0)main.postDelayed(sleepCheck,left);else if(SleepTimer.consumeExpired(this))pause();}
 private int generation;
 private float baseVolume=.7f;
 public String title="Local audio",error="",state="idle",path="";
 public boolean shuffled;
 public int repeat;
 public Bitmap artwork;
 private final BroadcastReceiver noisy=new BroadcastReceiver(){public void onReceive(Context c,Intent i){pause();}};

 @Override public void onCreate(){
  super.onCreate();active=this;audio=getSystemService(AudioManager.class);
  shuffled=getSharedPreferences("music",0).getBoolean("shuffle",false);
  repeat=getSharedPreferences("music",0).getInt("repeat",0);
  baseVolume=getSharedPreferences("music",0).getInt("volume",70)/100f;
  getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel("playback","Music playback",NotificationManager.IMPORTANCE_LOW));
  session=new MediaSession(this,"Pocket Music");
  session.setCallback(new MediaSession.Callback(){
   public void onPlay(){play();}public void onPause(){pause();}public void onSeekTo(long p){seek((int)p);}
   public void onSkipToNext(){next();}public void onSkipToPrevious(){previous();}
   public void onSkipToQueueItem(long id){if(id>=0&&id<queue.size()){order.reset(queue.size(),(int)id,shuffled);openCurrent();}}
   public void onStop(){stopSelf();}
  });session.setActive(true);
  focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
   .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
   .setOnAudioFocusChangeListener(this::onFocusChange,main).build();
  if(Build.VERSION.SDK_INT>=33)registerReceiver(noisy,new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),Context.RECEIVER_NOT_EXPORTED);
  else registerReceiver(noisy,new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));armSleepTimer();
 }
 @Override public int onStartCommand(Intent intent,int flags,int startId){
  if(intent==null){stopSelf();return START_NOT_STICKY;}
  String action=intent.getAction();
  if("stop".equals(action)){stopSelf();return START_NOT_STICKY;}
  try{startForeground(10,notification());}catch(RuntimeException blocked){fail("Open Pocket Music to resume audio playback.");return START_NOT_STICKY;}
  String file=intent.getStringExtra("path");
  if(file!=null){select(new File(file),intent.getBooleanExtra("sortByTitle",false));}
  else if("next".equals(action))next();
  else if("previous".equals(action))previous();
  else if("toggle".equals(action)){if(playing())pause();else play();}
  if(!playing()&&!"preparing".equals(state))stopForeground(STOP_FOREGROUND_DETACH);
  return START_NOT_STICKY;
 }
 private void select(File file,boolean byTitle){
  try{
   if(!file.isFile()||!file.getCanonicalFile().getParentFile().equals(MusicRepository.folder(this).getCanonicalFile())||!TrackFiles.audio(file))throw new IOException("Local audio file not found.");
   queue.clear();queue.addAll(LibraryOrder.select(MusicRepository.files(this),"audio",byTitle));
   int selected=queue.indexOf(file);if(selected<0){queue.add(file);selected=queue.size()-1;}
   order.reset(queue.size(),selected,shuffled);
   List<MediaSession.QueueItem> items=new ArrayList<>();
   for(int i=0;i<queue.size();i++)items.add(new MediaSession.QueueItem(new MediaDescription.Builder().setMediaId(queue.get(i).getAbsolutePath()).setTitle(TrackFiles.title(queue.get(i))).build(),i));
   session.setQueue(items);session.setQueueTitle("Your local queue");openCurrent();
  }catch(Exception failure){fail("Local audio file could not be opened.");}
 }
 private void openCurrent(){
  if(order.current()<0||order.current()>=queue.size())return;
  File file=queue.get(order.current());int token=++generation;
  ready=false;resumeOnGain=false;ducked=false;error="";state="preparing";
  if(player!=null){player.release();player=null;}
  path=file.getAbsolutePath();title=TrackFiles.title(file);artwork=null;
  getSharedPreferences("music",0).edit().putString("lastPlayed",path).apply();
  RecentHistory.touch(this,"recentTracks",path);
  getSharedPreferences("music",0).edit().remove("playerError").apply();
  try{
   if(!file.isFile())throw new IOException("The audio file is no longer available.");
   MediaPlayer current=new MediaPlayer();player=current;
   current.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
   current.setWakeMode(this,PowerManager.PARTIAL_WAKE_LOCK);current.setDataSource(path);applyVolume();
   current.setOnPreparedListener(p->{if(p!=player)return;ready=true;play();});
   current.setOnCompletionListener(p->{if(p!=player)return;complete();});
   current.setOnErrorListener((p,what,extra)->{if(p==player)fail("This audio could not be played. Try another file.");return true;});
   current.prepareAsync();publish();loadCover(file,token);
  }catch(Exception failure){fail("The audio file is missing or unsupported.");}
 }
 private void complete(){
  int next=order.advance(1,repeat,true);
  if(next>=0){if(queue.get(next).getAbsolutePath().equals(path)){player.seekTo(0);play();}else openCurrent();}
  else{state="ended";resumeOnGain=false;publish();stopForeground(STOP_FOREGROUND_DETACH);audio.abandonAudioFocusRequest(focus);}
 }
 public void next(){if(order.advance(1,repeat,false)>=0)openCurrent();}
 public void previous(){if(position()>3000){seek(0);return;}if(order.advance(-1,repeat,false)>=0)openCurrent();else seek(0);}
 public void shuffle(boolean enabled){shuffled=enabled;order.setShuffle(enabled);getSharedPreferences("music",0).edit().putBoolean("shuffle",enabled).apply();publish();}
 public void repeat(int mode){repeat=mode>=0&&mode<=2?mode:0;getSharedPreferences("music",0).edit().putInt("repeat",repeat).apply();publish();}
 public List<File> tracks(){return new ArrayList<>(queue);}
 public int queueIndex(){return order.current();}
 public MediaSession.Token sessionToken(){return session.getSessionToken();}
 public boolean playing(){return ready&&player!=null&&player.isPlaying();}
 public int position(){return ready&&player!=null?player.getCurrentPosition():0;}
 public int duration(){return ready&&player!=null?player.getDuration():0;}
 public void seek(int ms){if(ready){player.seekTo(Math.max(0,Math.min(ms,duration())));publish();}}
 public void volume(float value){baseVolume=Math.max(0,Math.min(1,value));applyVolume();getSharedPreferences("music",0).edit().putInt("volume",Math.round(baseVolume*100)).apply();}
 float effectiveVolume(){return ducked?baseVolume*.2f:baseVolume;}
 private void applyVolume(){if(player!=null)player.setVolume(effectiveVolume(),effectiveVolume());}
 public void play(){
  if(!ready||player==null)return;if(SleepTimer.consumeExpired(this)){state="paused";publish();return;}
  resumeOnGain=false;ducked=false;
  try{startForeground(10,notification());}catch(RuntimeException blocked){state="paused";error="Open Pocket Music to resume audio playback.";publish();return;}
  if(audio.requestAudioFocus(focus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED){state="paused";error="Audio is busy. Tap Play when other audio finishes.";publish();stopForeground(STOP_FOREGROUND_DETACH);return;}
  error="";if(position()>=duration()-50)player.seekTo(0);applyVolume();player.start();state="playing";publish();
 }
 public void pause(){resumeOnGain=false;ducked=false;pauseInternal(true);}
 private void pauseInternal(boolean abandonFocus){
  if(ready&&player!=null&&player.isPlaying())player.pause();
  if(ready)state="paused";applyVolume();publish();stopForeground(STOP_FOREGROUND_DETACH);
  if(abandonFocus)audio.abandonAudioFocusRequest(focus);
 }
 void onFocusChange(int change){
  if(change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK){ducked=true;applyVolume();}
  else if(change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT){resumeOnGain=playing();pauseInternal(false);}
  else if(change==AudioManager.AUDIOFOCUS_LOSS){resumeOnGain=false;pauseInternal(true);}
  else if(change==AudioManager.AUDIOFOCUS_GAIN){ducked=false;applyVolume();if(resumeOnGain){resumeOnGain=false;play();}}
 }
 private void fail(String message){
  error=message;state="error";ready=false;
  getSharedPreferences("music",0).edit().putString("playerError",message).apply();publish();stopSelf();
 }
 private void loadCover(File file,int token){
  if(coverJob!=null)coverJob.cancel(true);
  coverJob=metadata.submit(()->{
   Bitmap image=null;MediaMetadataRetriever reader=new MediaMetadataRetriever();
   try{reader.setDataSource(file.getAbsolutePath());byte[] data=reader.getEmbeddedPicture();if(data!=null&&data.length<=4*1024*1024){BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(data,0,data.length,o);o.inSampleSize=Math.max(1,Math.max(o.outWidth,o.outHeight)/320);o.inJustDecodeBounds=false;image=BitmapFactory.decodeByteArray(data,0,data.length,o);}}catch(Exception ignored){}finally{try{reader.release();}catch(Exception ignored){}}
   String id=TrackFiles.videoId(file);
   if(image==null&&id!=null&&!Thread.currentThread().isInterrupted()){
    HttpURLConnection connection=null;
    try{connection=(HttpURLConnection)new URL("https://i.ytimg.com/vi/"+id+"/mqdefault.jpg").openConnection();connection.setConnectTimeout(5000);connection.setReadTimeout(5000);try(InputStream in=connection.getInputStream()){image=BitmapFactory.decodeStream(in);}}catch(Exception ignored){}finally{if(connection!=null)connection.disconnect();}
   }
   final Bitmap cover=image;
   main.post(()->{if(token==generation&&active==this&&cover!=null){artwork=cover;publish();}});
  });
 }
 private PendingIntent command(String action){return PendingIntent.getService(this,action.hashCode(),new Intent(this,LocalPlayerService.class).setAction(action),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
 private Notification.Action action(int icon,String title,String command){return new Notification.Action.Builder(Icon.createWithResource(this,icon),title,command(command)).build();}
 private Notification notification(){
  return new Notification.Builder(this,"playback").setSmallIcon(R.drawable.ic_music).setContentTitle(title).setContentText(error.isEmpty()?"Local audio · "+(order.current()+1)+" of "+queue.size():error).setLargeIcon(artwork)
   .setContentIntent(PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT)).setOnlyAlertOnce(true).setOngoing(playing())
   .addAction(action(R.drawable.ic_previous,"Previous","previous")).addAction(action(playing()?R.drawable.ic_pause:R.drawable.ic_play,playing()?"Pause":"Play","toggle")).addAction(action(R.drawable.ic_next,"Next","next"))
   .addAction(action(R.drawable.ic_stop,"Stop","stop")).setStyle(new Notification.MediaStyle().setMediaSession(session.getSessionToken()).setShowActionsInCompactView(0,1,2)).build();
 }
 private void publish(){
  if(session==null)return;
  MediaMetadata.Builder data=new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE,title).putString(MediaMetadata.METADATA_KEY_ARTIST,"Pocket Music · Local audio").putLong(MediaMetadata.METADATA_KEY_DURATION,duration());
  if(artwork!=null)data.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART,artwork);session.setMetadata(data.build());
  int playbackState=playing()?PlaybackState.STATE_PLAYING:"preparing".equals(state)?PlaybackState.STATE_BUFFERING:"error".equals(state)?PlaybackState.STATE_ERROR:"ended".equals(state)?PlaybackState.STATE_STOPPED:PlaybackState.STATE_PAUSED;
  long actions=PlaybackState.ACTION_PLAY|PlaybackState.ACTION_PAUSE|PlaybackState.ACTION_PLAY_PAUSE|PlaybackState.ACTION_SEEK_TO|PlaybackState.ACTION_STOP|PlaybackState.ACTION_SKIP_TO_PREVIOUS|PlaybackState.ACTION_SKIP_TO_QUEUE_ITEM;
  if(queue.size()>1)actions|=PlaybackState.ACTION_SKIP_TO_NEXT;
  PlaybackState.Builder b=new PlaybackState.Builder().setActions(actions).setState(playbackState,position(),playing()?1:0).setActiveQueueItemId(order.current());if(!error.isEmpty())b.setErrorMessage(error);session.setPlaybackState(b.build());
  getSystemService(NotificationManager.class).notify(10,notification());MusicWidget.update(this);
 }
 @Override public IBinder onBind(Intent intent){return null;}
 @Override public void onDestroy(){
  ++generation;ready=false;if(player!=null){player.release();player=null;}
  metadata.shutdownNow();main.removeCallbacksAndMessages(null);audio.abandonAudioFocusRequest(focus);
  session.setActive(false);session.release();unregisterReceiver(noisy);if(active==this)active=null;MusicWidget.update(this);stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();
 }
}
