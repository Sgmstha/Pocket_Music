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

/** One native media session for saved files and online audio. */
public final class LocalPlayerService extends Service {
 private static final long ONLINE_PREPARE_TIMEOUT_MS=90000L;
 public static LocalPlayerService active;
 private MediaPlayer player;
 private MediaSession session;
 private AudioManager audio;
 private AudioFocusRequest focus;
 private boolean ready,resumeOnGain,ducked;
 private final PlaybackOrder order=new PlaybackOrder();
 private final List<File> queue=new ArrayList<>();
 public boolean online,networkPlayback;public Track song;
 private final List<Track> onlineQueue=new ArrayList<>();
 static OnlineAudio.Resolver onlineResolver=OnlineAudio::resolve;
 private final ExecutorService resolveWorker=Executors.newSingleThreadExecutor(),cancelWorker=Executors.newSingleThreadExecutor();
 private Future<?> resolveJob;private String resolveProcess="";private boolean desiredPlay=true;
 private android.net.wifi.WifiManager.WifiLock wifiLock;
 private PowerManager.WakeLock resolveLock;
 private final Runnable prepareTimeout=()->{if(!ready&&active==this)fail("Online audio took too long. Check your connection and tap the song to retry.");};
 private final ExecutorService metadata=Executors.newSingleThreadExecutor();
 private final Handler main=new Handler(Looper.getMainLooper());
 private Future<?> coverJob;
 private final Runnable sleepCheck=()->{if(SleepTimer.consumeExpired(this)){pause();}else armSleepTimer();};
 public void armSleepTimer(){main.removeCallbacks(sleepCheck);long left=SleepTimer.remaining(this);if(left>0)main.postDelayed(sleepCheck,left);else if(SleepTimer.consumeExpired(this))pause();}
 private int generation,recordedOnlineGeneration=-1;
 private float baseVolume=.7f;
 private String trackArtist="Pocket Music · Local audio";
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
   public void onSkipToQueueItem(long id){playQueued((int)id);}
   public void onStop(){stopSelf();}
  });session.setActive(true);
  resolveLock=getSystemService(PowerManager.class).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"PocketMusic:resolve");resolveLock.setReferenceCounted(false);
  android.net.wifi.WifiManager wifi=(android.net.wifi.WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);if(wifi!=null){wifiLock=wifi.createWifiLock(android.net.wifi.WifiManager.WIFI_MODE_FULL_HIGH_PERF,"PocketMusic:online-audio");wifiLock.setReferenceCounted(false);}
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
  if(file!=null){select(new File(file),intent.getBooleanExtra("sortByTitle",false),intent.getStringArrayListExtra("queuePaths"));}
  else if(intent.hasExtra("onlineQueue"))selectOnline(intent.getStringExtra("onlineQueue"),intent.getStringExtra("onlineId"));
  else if("next".equals(action))next();
  else if("previous".equals(action))previous();
  else if("toggle".equals(action)){if(pauseAvailable())pause();else play();}
  if(!playing()&&!"preparing".equals(state))stopForeground(STOP_FOREGROUND_DETACH);
  return START_NOT_STICKY;
 }
 private void select(File file,boolean byTitle,List<String> requested){
  try{
   if(!file.isFile()||!file.getCanonicalFile().getParentFile().equals(MusicRepository.folder(this).getCanonicalFile())||!TrackFiles.audio(file))throw new IOException("Local audio file not found.");
   cancelResolve();online=false;networkPlayback=false;onlineQueue.clear();queue.clear();List<File> available=LibraryOrder.select(MusicRepository.files(this),"audio",byTitle);if(requested==null)queue.addAll(available);else for(String path:requested.subList(0,Math.min(200,requested.size())))for(File candidate:available)if(candidate.getAbsolutePath().equals(path)&&!queue.contains(candidate))queue.add(candidate);
   int selected=queue.indexOf(file);if(selected<0){queue.add(file);selected=queue.size()-1;}
   order.reset(queue.size(),selected,shuffled);
   List<MediaSession.QueueItem> items=new ArrayList<>();
   for(int i:order.sequence())items.add(new MediaSession.QueueItem(new MediaDescription.Builder().setMediaId(queue.get(i).getAbsolutePath()).setTitle(TrackFiles.title(queue.get(i))).build(),i));
   session.setQueue(items);session.setQueueTitle("Your local queue");openCurrent();
  }catch(Exception failure){fail("Local audio file could not be opened.");}
 }
 private void openCurrent(){
  if(online){openOnlineCurrent();return;}cancelResolve();desiredPlay=true;
  if(order.current()<0||order.current()>=queue.size())return;
  File file=queue.get(order.current());int token=++generation;
  ready=false;resumeOnGain=false;ducked=false;error="";state="preparing";
  if(player!=null){player.release();player=null;}
  path=file.getAbsolutePath();title=TrackFiles.title(file);artwork=null;
  getSharedPreferences("music",0).edit().putString("lastPlayed",path).apply();
  RecentHistory.touch(this,"recentTracks",path);Track catalogSong=MusicTaste.track(this,TrackFiles.videoId(file));song=catalogSong;if(catalogSong!=null)title=catalogSong.title;trackArtist=catalogSong==null||catalogSong.artist.isEmpty()?"Pocket Music · Local audio":catalogSong.artist;MusicTaste.played(this,catalogSong);
  getSharedPreferences("music",0).edit().putString("recommendationSeed",title).apply();
  getSharedPreferences("music",0).edit().remove("playerError").apply();
  try{
   if(!file.isFile())throw new IOException("The audio file is no longer available.");
   MediaPlayer current=new MediaPlayer();player=current;
   current.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
   current.setWakeMode(this,PowerManager.PARTIAL_WAKE_LOCK);current.setDataSource(path);applyVolume();
   current.setOnPreparedListener(p->{if(p!=player)return;ready=true;if(desiredPlay)play();else{state="paused";publish();}});
   current.setOnCompletionListener(p->{if(p!=player)return;complete();});
   current.setOnErrorListener((p,what,extra)->{if(p==player)fail("This audio could not be played. Try another file.");return true;});
   current.prepareAsync();publish();loadCover(file,token);
  }catch(Exception failure){fail("The audio file is missing or unsupported.");}
 }
 private void selectOnline(String json,String id){try{List<Track> songs=TrackMetadata.parse(json,200);if(songs.isEmpty())throw new IOException();cancelResolve();online=true;queue.clear();onlineQueue.clear();Set<String> ids=new HashSet<>();for(Track track:songs)if(ids.add(track.id))onlineQueue.add(track);int selected=0;for(int i=0;i<onlineQueue.size();i++)if(onlineQueue.get(i).id.equals(id))selected=i;order.reset(onlineQueue.size(),selected,shuffled);publishQueue();openOnlineCurrent();}catch(Exception e){fail("Choose a valid online song.");}}
 private void cancelResolve(){main.removeCallbacks(prepareTimeout);if(resolveJob!=null){resolveJob.cancel(true);resolveJob=null;}String process=resolveProcess;resolveProcess="";if(!process.isEmpty())cancelWorker.execute(()->OnlineAudio.cancel(process));if(resolveLock!=null&&resolveLock.isHeld())resolveLock.release();}
 private void releaseNetworkLocks(){if(wifiLock!=null&&wifiLock.isHeld())wifiLock.release();if(resolveLock!=null&&resolveLock.isHeld())resolveLock.release();}
 private void openOnlineCurrent(){
  if(order.current()<0||order.current()>=onlineQueue.size())return;cancelResolve();releaseNetworkLocks();int token=++generation;ready=false;desiredPlay=true;resumeOnGain=false;ducked=false;error="";state="preparing";if(player!=null){player.release();player=null;}
  song=onlineQueue.get(order.current());Track selected=song;title=song.title;trackArtist=song.artist.isEmpty()?"Pocket Music · Online audio":song.artist;path="";artwork=null;networkPlayback=true;getSharedPreferences("music",0).edit().remove("playerError").putString("recommendationSeed",title).apply();
  File local=MusicRepository.local(this,song.id);if(local!=null){path=local.getAbsolutePath();networkPlayback=false;prepareOnlineSource(token,null);loadCover(local,token);return;}
  try{startForeground(10,notification());}catch(RuntimeException e){fail("Open Pocket Music to start online audio.");return;}publish();loadOnlineCover(selected,token);resolveLock.acquire(ONLINE_PREPARE_TIMEOUT_MS);main.postDelayed(prepareTimeout,ONLINE_PREPARE_TIMEOUT_MS);String process="online-"+UUID.randomUUID();resolveProcess=process;
  resolveJob=resolveWorker.submit(()->{try{OnlineAudio.Source source=onlineResolver.resolve(this,selected,process);main.post(()->{if(active!=this||token!=generation)return;resolveJob=null;resolveProcess="";prepareOnlineSource(token,source);});}catch(Exception e){main.post(()->{if(active==this&&token==generation)fail(OnlineAudio.friendly(e));});}});
 }
 private void prepareOnlineSource(int token,OnlineAudio.Source source){
  try{MediaPlayer current=new MediaPlayer();player=current;current.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());current.setWakeMode(this,PowerManager.PARTIAL_WAKE_LOCK);if(source==null)current.setDataSource(path);else current.setDataSource(this,android.net.Uri.parse(source.url),source.headers);applyVolume();
   current.setOnPreparedListener(p->{if(p!=player||token!=generation)return;main.removeCallbacks(prepareTimeout);if(resolveLock.isHeld())resolveLock.release();ready=true;if(desiredPlay)play();else{state="paused";publish();}});
   current.setOnCompletionListener(p->{if(p==player)complete();});current.setOnErrorListener((p,what,extra)->{if(p==player)fail("Online audio stopped. Check your connection and tap the song to retry.");return true;});current.setOnInfoListener((p,what,extra)->{if(p!=player)return false;if(ready&&(what==MediaPlayer.MEDIA_INFO_BUFFERING_START||what==MediaPlayer.MEDIA_INFO_BUFFERING_END)){state=what==MediaPlayer.MEDIA_INFO_BUFFERING_START?"buffering":p.isPlaying()?"playing":"paused";publish();}return false;});current.prepareAsync();publish();
  }catch(Exception e){fail("Online audio could not open. Tap the song to retry or use Watch YouTube preview.");}
 }
 private void loadOnlineCover(Track track,int token){if(coverJob!=null)coverJob.cancel(true);coverJob=metadata.submit(()->{Bitmap cover=null;com.bumptech.glide.request.FutureTarget<Bitmap> target=null;try{Artwork.init(this);target=com.bumptech.glide.Glide.with(getApplicationContext()).asBitmap().load(Artwork.url(track)).override(320,320).submit();cover=target.get(7,TimeUnit.SECONDS).copy(Bitmap.Config.ARGB_8888,false);}catch(Exception ignored){}finally{if(target!=null)com.bumptech.glide.Glide.with(getApplicationContext()).clear(target);}Bitmap image=cover;main.post(()->{if(active==this&&token==generation&&image!=null){artwork=image;publish();}});});}
 private void complete(){
  int next=order.advance(1,repeat,true);
  if(next>=0){boolean same=online?onlineQueue.get(next).id.equals(song.id):queue.get(next).getAbsolutePath().equals(path);if(same){player.seekTo(0);play();}else openCurrent();}
  else{state="ended";resumeOnGain=false;releaseNetworkLocks();publish();stopForeground(STOP_FOREGROUND_DETACH);audio.abandonAudioFocusRequest(focus);}
 }
 public void next(){if(order.advance(1,repeat,false)>=0)openCurrent();}
 public void previous(){if(position()>3000){seek(0);return;}if(order.advance(-1,repeat,false)>=0)openCurrent();else seek(0);}
 public void shuffle(boolean enabled){shuffled=enabled;order.setShuffle(enabled);getSharedPreferences("music",0).edit().putBoolean("shuffle",enabled).apply();publishQueue();}
 public void repeat(int mode){repeat=mode>=0&&mode<=2?mode:0;getSharedPreferences("music",0).edit().putInt("repeat",repeat).apply();publish();}
 public List<Track> onlineTracks(){return new ArrayList<>(onlineQueue);}
 public List<File> tracks(){return new ArrayList<>(queue);}
 public int queueIndex(){return order.current();}
 public List<Integer> queueOrder(){return order.sequence();}
 public void playQueued(int index){if(order.select(index))openCurrent();}
 private void publishQueue(){List<MediaSession.QueueItem> items=new ArrayList<>();for(int i:order.sequence()){String id=online?onlineQueue.get(i).id:queue.get(i).getAbsolutePath();String title=online?onlineQueue.get(i).title:TrackFiles.title(queue.get(i));items.add(new MediaSession.QueueItem(new MediaDescription.Builder().setMediaId(id).setTitle(title).build(),i));}session.setQueue(items);session.setQueueTitle(online?"Your online audio queue":"Your local queue");publish();}
 public boolean enqueueOnline(Track track,boolean next){if(!online||track==null||!track.id.matches("[A-Za-z0-9_-]{11}"))return false;int index=-1;for(int i=0;i<onlineQueue.size();i++)if(onlineQueue.get(i).id.equals(track.id)){index=i;break;}if(index==order.current())return false;if(index>=0){if(next)order.playNext(index);}else{if(onlineQueue.size()>=200)return false;onlineQueue.add(track);order.append(next);}publishQueue();return true;}
 public boolean enqueue(File file,boolean next){if(online)return false;try{if(!file.isFile()||!file.getCanonicalFile().getParentFile().equals(MusicRepository.folder(this).getCanonicalFile())||!TrackFiles.audio(file))return false;int index=queue.indexOf(file);if(index==order.current())return false;if(index>=0){if(next)order.playNext(index);}else{queue.add(file);order.append(next);}publishQueue();return true;}catch(IOException e){return false;}}
 public boolean removeQueued(int index){if(!order.remove(index))return false;if(online)onlineQueue.remove(index);else queue.remove(index);publishQueue();return true;}
 public boolean moveNext(int index){if(!order.playNext(index))return false;publishQueue();return true;}
 public MediaSession.Token sessionToken(){return session.getSessionToken();}
 public boolean pauseAvailable(){return playing()||online&&!ready&&desiredPlay&&"preparing".equals(state);}
 public boolean playing(){return ready&&player!=null&&player.isPlaying();}
 public int position(){return ready&&player!=null?player.getCurrentPosition():0;}
 public int duration(){return ready&&player!=null?player.getDuration():0;}
 public void seek(int ms){if(ready){player.seekTo(Math.max(0,Math.min(ms,duration())));publish();}}
 public void volume(float value){baseVolume=Math.max(0,Math.min(1,value));applyVolume();getSharedPreferences("music",0).edit().putInt("volume",Math.round(baseVolume*100)).apply();}
 float effectiveVolume(){return ducked?baseVolume*.2f:baseVolume;}
 private void applyVolume(){if(player!=null)player.setVolume(effectiveVolume(),effectiveVolume());}
 public void play(){
  desiredPlay=true;if(!ready||player==null){if(online&&resolveJob!=null){state="preparing";try{startForeground(10,notification());}catch(RuntimeException ignored){}publish();}return;}if(SleepTimer.consumeExpired(this)){state="paused";publish();return;}
  resumeOnGain=false;ducked=false;
  try{startForeground(10,notification());}catch(RuntimeException blocked){state="paused";error="Open Pocket Music to resume audio playback.";publish();return;}
  if(audio.requestAudioFocus(focus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED){state="paused";error="Audio is busy. Tap Play when other audio finishes.";publish();stopForeground(STOP_FOREGROUND_DETACH);return;}
  error="";if(position()>=duration()-50)player.seekTo(0);applyVolume();player.start();if(online&&recordedOnlineGeneration!=generation){recordedOnlineGeneration=generation;MusicTaste.played(this,song);}state="playing";if(networkPlayback&&wifiLock!=null&&!wifiLock.isHeld())try{wifiLock.acquire();}catch(RuntimeException ignored){}publish();
 }
 public void pause(){desiredPlay=false;resumeOnGain=false;ducked=false;pauseInternal(true);}
 private void pauseInternal(boolean abandonFocus){
  if(ready&&player!=null&&player.isPlaying())player.pause();
  if(ready||online)state="paused";releaseNetworkLocks();applyVolume();publish();stopForeground(STOP_FOREGROUND_DETACH);
  if(abandonFocus)audio.abandonAudioFocusRequest(focus);
 }
 void onFocusChange(int change){
  if(change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK){ducked=true;applyVolume();}
  else if(change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT){resumeOnGain=pauseAvailable();desiredPlay=false;pauseInternal(false);}
  else if(change==AudioManager.AUDIOFOCUS_LOSS){resumeOnGain=false;desiredPlay=false;pauseInternal(true);}
  else if(change==AudioManager.AUDIOFOCUS_GAIN){ducked=false;applyVolume();if(resumeOnGain){resumeOnGain=false;play();}}
 }
 private void fail(String message){
  ++generation;error=message;state="error";ready=false;desiredPlay=false;cancelResolve();releaseNetworkLocks();
  getSharedPreferences("music",0).edit().putString("playerError",message).apply();publish();stopSelf();
 }
 private void loadCover(File file,int token){
  if(coverJob!=null)coverJob.cancel(true);
  coverJob=metadata.submit(()->{
   Bitmap image=null;MediaMetadataRetriever reader=new MediaMetadataRetriever();
   try{reader.setDataSource(file.getAbsolutePath());byte[] data=reader.getEmbeddedPicture();if(data!=null&&data.length<=4*1024*1024){BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(data,0,data.length,o);o.inSampleSize=Math.max(1,Math.max(o.outWidth,o.outHeight)/320);o.inJustDecodeBounds=false;image=BitmapFactory.decodeByteArray(data,0,data.length,o);}}catch(Exception ignored){}finally{try{reader.release();}catch(Exception ignored){}}
   String id=TrackFiles.videoId(file);
   if(image==null&&id!=null&&!Thread.currentThread().isInterrupted()){
    com.bumptech.glide.request.FutureTarget<Bitmap> target=null;try{Artwork.init(this);Track song=MusicTaste.track(this,id);if(song==null)song=new Track(id,title);target=com.bumptech.glide.Glide.with(getApplicationContext()).asBitmap().load(Artwork.url(song)).override(320,320).submit();image=target.get(7,TimeUnit.SECONDS).copy(Bitmap.Config.ARGB_8888,false);}catch(Exception ignored){}finally{if(target!=null)com.bumptech.glide.Glide.with(getApplicationContext()).clear(target);}
   }
   final Bitmap cover=image;
   main.post(()->{if(token==generation&&active==this&&cover!=null){artwork=cover;publish();}});
  });
 }
 private PendingIntent command(String action){return PendingIntent.getService(this,action.hashCode(),new Intent(this,LocalPlayerService.class).setAction(action),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
 private Notification.Action action(int icon,String title,String command){return new Notification.Action.Builder(Icon.createWithResource(this,icon),title,command(command)).build();}
 private Notification notification(){
  return new Notification.Builder(this,"playback").setSmallIcon(R.drawable.ic_music).setContentTitle(title).setContentText(error.isEmpty()?(networkPlayback?"Online audio · ":"Saved audio · ")+(order.current()+1)+" of "+(online?onlineQueue.size():queue.size()):error).setLargeIcon(artwork)
   .setContentIntent(PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT)).setOnlyAlertOnce(true).setOngoing(playing())
   .addAction(action(R.drawable.ic_previous,"Previous","previous")).addAction(action(pauseAvailable()?R.drawable.ic_pause:R.drawable.ic_play,pauseAvailable()?"Pause":"Play","toggle")).addAction(action(R.drawable.ic_next,"Next","next"))
   .addAction(action(R.drawable.ic_stop,"Stop","stop")).setStyle(new Notification.MediaStyle().setMediaSession(session.getSessionToken()).setShowActionsInCompactView(0,1,2)).build();
 }
 private void publish(){
  if(session==null)return;
  MediaMetadata.Builder data=new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE,title).putString(MediaMetadata.METADATA_KEY_ARTIST,trackArtist).putLong(MediaMetadata.METADATA_KEY_DURATION,duration());
  if(artwork!=null)data.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART,artwork);session.setMetadata(data.build());
  int playbackState=("preparing".equals(state)||"buffering".equals(state))?PlaybackState.STATE_BUFFERING:playing()?PlaybackState.STATE_PLAYING:"error".equals(state)?PlaybackState.STATE_ERROR:"ended".equals(state)?PlaybackState.STATE_STOPPED:PlaybackState.STATE_PAUSED;
  long actions=PlaybackState.ACTION_PLAY|PlaybackState.ACTION_PAUSE|PlaybackState.ACTION_PLAY_PAUSE|PlaybackState.ACTION_SEEK_TO|PlaybackState.ACTION_STOP|PlaybackState.ACTION_SKIP_TO_PREVIOUS|PlaybackState.ACTION_SKIP_TO_QUEUE_ITEM;
  if((online?onlineQueue.size():queue.size())>1)actions|=PlaybackState.ACTION_SKIP_TO_NEXT;
  PlaybackState.Builder b=new PlaybackState.Builder().setActions(actions).setState(playbackState,position(),playing()?1:0).setActiveQueueItemId(order.current());if(!error.isEmpty())b.setErrorMessage(error);session.setPlaybackState(b.build());
  getSystemService(NotificationManager.class).notify(10,notification());MusicWidget.update(this);
 }
 @Override public IBinder onBind(Intent intent){return null;}
 @Override public void onDestroy(){
  ++generation;cancelResolve();releaseNetworkLocks();resolveWorker.shutdownNow();cancelWorker.shutdown();ready=false;if(player!=null){player.release();player=null;}
  metadata.shutdownNow();main.removeCallbacksAndMessages(null);audio.abandonAudioFocusRequest(focus);
  session.setActive(false);session.release();unregisterReceiver(noisy);if(active==this)active=null;MusicWidget.update(this);stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();
 }
}
