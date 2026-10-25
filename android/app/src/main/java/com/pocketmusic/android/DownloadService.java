package com.pocketmusic.android;
import android.app.*;
import android.content.*;
import android.os.*;
import com.yausername.youtubedl_android.YoutubeDL;
import java.util.concurrent.*;
public final class DownloadService extends Service {
 public static volatile boolean busy; public static volatile String status="";
 private final ExecutorService worker=Executors.newSingleThreadExecutor();private volatile boolean cancelled;private long lastNotice;
 public void onCreate(){super.onCreate();getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel("downloads","Downloads",NotificationManager.IMPORTANCE_LOW));}
 public int onStartCommand(Intent i,int flags,int startId){
  if(i==null){stopSelf();return START_NOT_STICKY;}
  if("cancel".equals(i.getAction())){cancelled=true;worker.shutdownNow();new Thread(()->YoutubeDL.getInstance().destroyProcessById("download")).start();status="Download cancelled";save();stopSelf();return START_NOT_STICKY;}
  if(busy)return START_NOT_STICKY;
  busy=true;cancelled=false;getSharedPreferences("music",0).edit().remove("downloadDetails").apply();status="Preparing download…";startForeground(20,notice());
  String id=i.getStringExtra("id"),format=i.getStringExtra("format");
  worker.execute(()->{try{
   status="Updating download engine…";
   try { MusicRepository.updateEngine(this,false); } catch(Exception updateError) {
    if(cancelled || Thread.currentThread().isInterrupted())return;
    status="Update unavailable · trying installed engine…";
   }
   for(int attempt=0;attempt<2;attempt++) {
   if(cancelled || Thread.currentThread().isInterrupted())return;
   var request=MusicRepository.download(this,id,format);
   try {
   YoutubeDL.getInstance().execute(request,"download",false,(progress,eta,line)->{if(SystemClock.elapsedRealtime()-lastNotice>1000){lastNotice=SystemClock.elapsedRealtime();status=progress<0?"Finishing download…":"Downloading · "+Math.min(100,Math.round(progress))+"%";getSystemService(NotificationManager.class).notify(20,notice());}return kotlin.Unit.INSTANCE;});
   break;
   } catch(Exception failure) {
    if(cancelled || attempt==1 || !DownloadErrors.recoverable(failure.getMessage()))throw failure;
    status="Refreshing download engine and retrying…";
    MusicRepository.updateEngine(this,true);
   }
   }
   if(!cancelled)status="Saved to Library. Use Export to keep a copy in Downloads.";
  }catch(Exception e){getSharedPreferences("music",0).edit().putString("downloadDetails",String.valueOf(e.getMessage())).apply();status=cancelled?"Download cancelled":DownloadErrors.friendly(e.getMessage());}finally{busy=false;save();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();}});return START_NOT_STICKY;
 }
 static String message(Exception e){String m=e.getMessage();return m==null?"Please retry.":m.substring(0,Math.min(700,m.length()));}
 private void save(){getSharedPreferences("music",0).edit().putString("downloadStatus",status).apply();}
 private Notification notice(){PendingIntent cancel=PendingIntent.getService(this,20,new Intent(this,DownloadService.class).setAction("cancel"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);return new Notification.Builder(this,"downloads").setSmallIcon(R.drawable.ic_music).setContentTitle("Pocket Music download").setContentText(status).setOnlyAlertOnce(true).setOngoing(true).setContentIntent(PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE)).addAction(new Notification.Action.Builder(null,"Cancel",cancel).build()).build();}
 public void onTimeout(int startId,int type){cancelled=true;YoutubeDL.getInstance().destroyProcessById("download");status="Download timed out. Please retry.";busy=false;save();stopSelf();}
 public void onDestroy(){worker.shutdownNow();super.onDestroy();}
 public IBinder onBind(Intent i){return null;}
}
