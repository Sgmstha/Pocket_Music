package com.pocketmusic.android;
import android.app.*;
import android.content.*;
import android.media.*;
import android.media.session.*;
import android.os.*;
import java.io.File;
public final class LocalPlayerService extends Service {
 public static LocalPlayerService active;
 private MediaPlayer player; private MediaSession session; private AudioManager audio; private AudioFocusRequest focus;
 public String title="Local audio", error=""; private boolean ready;
 private final BroadcastReceiver noisy=new BroadcastReceiver(){public void onReceive(Context c,Intent i){pause();}};
 public void onCreate(){super.onCreate();active=this;audio=getSystemService(AudioManager.class);
  getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel("playback","Local playback",NotificationManager.IMPORTANCE_LOW));
  session=new MediaSession(this,"Pocket Music");session.setCallback(new MediaSession.Callback(){public void onPlay(){play();} public void onPause(){pause();} public void onSeekTo(long p){seek((int)p);} public void onStop(){stopSelf();}});session.setActive(true);
  focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).setOnAudioFocusChangeListener(change->{if(change<=0)pause();}).build();
  if(Build.VERSION.SDK_INT>=33)registerReceiver(noisy,new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),Context.RECEIVER_NOT_EXPORTED);else registerReceiver(noisy,new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
 }
 public int onStartCommand(Intent i,int flags,int startId){
  if(i==null){stopSelf();return START_NOT_STICKY;}
  String action=i.getAction();
  if("stop".equals(action)){stopSelf();return START_NOT_STICKY;}
  startForeground(10,notification());
  if("toggle".equals(action)){if(playing())pause();else play();return START_NOT_STICKY;}
  String path=i.getStringExtra("path"); if(path!=null)open(new File(path));return START_NOT_STICKY;
 }
 private void open(File f){try{
  if(!f.isFile() || !f.getCanonicalFile().getParentFile().equals(MusicRepository.folder(this).getCanonicalFile()))throw new IllegalArgumentException("Local file not found.");
  if(player!=null)player.release();ready=false;title=f.getName();error="";player=new MediaPlayer();
  player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
  player.setWakeMode(this,PowerManager.PARTIAL_WAKE_LOCK);player.setDataSource(f.getAbsolutePath());
  player.setOnPreparedListener(p->{ready=true;session.setMetadata(new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE,title).putLong(MediaMetadata.METADATA_KEY_DURATION,p.getDuration()).build());play();});
  player.setOnCompletionListener(p->{update();stopForeground(STOP_FOREGROUND_DETACH);audio.abandonAudioFocusRequest(focus);});
  player.setOnErrorListener((p,w,e)->{error="This audio could not be played. Try MP3 or M4A.";ready=false;update();stopSelf();return true;});player.prepareAsync();update();
 }catch(Exception e){error=e.getMessage();getSharedPreferences("music",0).edit().putString("playerError",error).apply();stopSelf();}}
 public boolean playing(){return ready && player!=null && player.isPlaying();}
 public int position(){return ready?player.getCurrentPosition():0;} public int duration(){return ready?player.getDuration():0;}
 public void seek(int ms){if(ready){player.seekTo(Math.max(0,Math.min(ms,duration())));update();}}
 public void volume(float v){if(player!=null)player.setVolume(v,v);}
 public void play(){if(ready && audio.requestAudioFocus(focus)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED){startForeground(10,notification());player.start();update();}}
 public void pause(){if(ready && player.isPlaying())player.pause();update();}
 private PendingIntent command(String action){return PendingIntent.getService(this,action.hashCode(),new Intent(this,LocalPlayerService.class).setAction(action),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
 private Notification notification(){return new Notification.Builder(this,"playback").setSmallIcon(R.drawable.ic_music).setContentTitle(title).setContentText(error.isEmpty()?"Local audio · Pocket Music":error).setContentIntent(PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT)).setOnlyAlertOnce(true).setOngoing(playing()).addAction(new Notification.Action.Builder(null,playing()?"Pause":"Play",command("toggle")).build()).addAction(new Notification.Action.Builder(null,"Stop",command("stop")).build()).setStyle(new Notification.MediaStyle().setMediaSession(session.getSessionToken()).setShowActionsInCompactView(0,1)).build();}
 private void update(){session.setPlaybackState(new PlaybackState.Builder().setActions(PlaybackState.ACTION_PLAY|PlaybackState.ACTION_PAUSE|PlaybackState.ACTION_PLAY_PAUSE|PlaybackState.ACTION_SEEK_TO|PlaybackState.ACTION_STOP).setState(playing()?PlaybackState.STATE_PLAYING:PlaybackState.STATE_PAUSED,position(),playing()?1:0).build());getSystemService(NotificationManager.class).notify(10,notification());}
 public IBinder onBind(Intent i){return null;}
 public void onDestroy(){if(player!=null)player.release();ready=false;audio.abandonAudioFocusRequest(focus);session.release();unregisterReceiver(noisy);active=null;stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();}
}
