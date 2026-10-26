package com.pocketmusic.android;
import android.app.*;
import android.content.*;
import android.os.*;
import com.yausername.youtubedl_android.YoutubeDL;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;

public final class DownloadService extends Service {
 public static volatile boolean busy;public static volatile String status="";private static DownloadQueue queue;private static DownloadService active;
 private final ExecutorService workers=Executors.newFixedThreadPool(2);private final Handler main=new Handler(Looper.getMainLooper());private int lastStart;private long lastPersist;
 private static synchronized DownloadQueue store(Context c){
  if(queue==null){queue=new DownloadQueue(2);try{JSONArray saved=new JSONArray(c.getSharedPreferences("music",0).getString("downloadJobs","[]"));for(int i=0;i<saved.length();i++){JSONObject o=saved.getJSONObject(i);DownloadQueue.Job j=new DownloadQueue.Job(o.getString("key"),o.getString("id"),o.optString("title","Track"),o.getString("format"));j.state=o.optString("state","interrupted");j.message=o.optString("message","");j.error=o.optString("error","");j.progress=o.optInt("progress",-1);if(j.pending()){j.state="interrupted";j.message="Interrupted · tap Retry";}queue.restore(j);}}catch(Exception ignored){}}
  return queue;
 }
 static List<DownloadQueue.Job> jobs(Context c){return store(c).all();}
 static int progress(Context c){int sum=0,count=0;for(DownloadQueue.Job j:jobs(c))if(j.pending()){if(j.progress>=0)sum+=j.progress;count++;}return count==0?0:sum/count;}
 @Override public void onCreate(){super.onCreate();active=this;YouTubeSession.cleanAbandoned(this);store(this);getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel("downloads","Downloads",NotificationManager.IMPORTANCE_LOW));}
 @Override public int onStartCommand(Intent intent,int flags,int startId){
  if(intent==null){stopSelf();return START_NOT_STICKY;}lastStart=startId;String action=intent.getAction();
  try{startForeground(20,notice());}catch(RuntimeException blocked){status="Open Pocket Music to manage downloads.";stopSelf();return START_NOT_STICKY;}
  if("cancel".equals(action)||"cancelJob".equals(action)){String key=intent.getStringExtra("job");for(DownloadQueue.Job j:queue.all())if(key==null||j.key.equals(key)){queue.cancel(j.key);new Thread(()->YoutubeDL.getInstance().destroyProcessById("download-"+j.key)).start();}}
  else if("retry".equals(action))queue.retry(intent.getStringExtra("job"));
  else{String id=intent.getStringExtra("id"),format=intent.getStringExtra("format");if(id!=null&&id.matches("[A-Za-z0-9_-]{11}")&&("mp3".equals(format)||"mp4".equals(format))){queue.enqueue(id,intent.getStringExtra("title")==null?"YouTube · "+id:intent.getStringExtra("title"),format);getSharedPreferences("music",0).edit().remove("downloadDetails").remove("folderSaveError").apply();}}
  pump();return START_NOT_STICKY;
 }
 private void pump(){
  if(active!=this)return;for(DownloadQueue.Job job:queue.startable())workers.execute(()->run(job));publish(null);
  if(!queue.pending()){stopForeground(STOP_FOREGROUND_REMOVE);stopSelfResult(lastStart);}
 }
 private void run(DownloadQueue.Job job){
  String process="download-"+job.key;java.io.File session=null;
  try{
   if(job.cancelled)return;if(!job.saveOnly&&System.currentTimeMillis()<getSharedPreferences("music",0).getLong("youtubeCooldown",0))throw new java.io.IOException("HTTP 429: YouTube cooldown. Wait before retrying.");job.message="Preparing…";publish(job);
   if(!job.saveOnly)try{MusicRepository.updateEngine(this,false);}catch(Exception e){if(job.cancelled)return;job.message="Using installed engine…";}
   if(!job.saveOnly)session=YouTubeSession.lease(this);
   for(int attempt=0;!job.saveOnly&&attempt<2;attempt++){
    if(job.cancelled)return;long[] tick={0};
    try{com.yausername.youtubedl_android.YoutubeDLRequest request=MusicRepository.download(this,job.id,job.format);if(session!=null)request.addOption("--cookies",session.getAbsolutePath());YoutubeDL.getInstance().execute(request,process,false,(p,eta,line)->{if(job.cancelled)throw new java.util.concurrent.CancellationException();if(SystemClock.elapsedRealtime()-tick[0]>=700){tick[0]=SystemClock.elapsedRealtime();job.progress=p<0?-1:Math.min(100,Math.round(p));job.message=p<0?"Converting / finishing…":"Downloading · "+job.progress+"%";publish(job);}return kotlin.Unit.INSTANCE;});break;}
    catch(Exception e){if(job.cancelled)throw e;if(attempt==1||!DownloadErrors.recoverable(e.getMessage()))throw e;job.message="Refreshing engine and retrying…";publish(job);MusicRepository.updateEngine(this,true);}
   }
   if(job.cancelled)return;java.io.File file=MusicRepository.downloaded(this,job.id,job.format);if(file==null)throw new java.io.IOException("Completed file not found.");job.message="Saving to "+DownloadFolder.label(this)+"…";job.progress=-1;publish(job);
   try{DownloadFolder.save(this,file);job.state="done";job.progress=100;job.message="Saved to "+DownloadFolder.label(this);}
   catch(Exception e){job.state="saveFailed";job.error=DownloadErrors.redact(e.getMessage());job.message="Downloaded · folder save needs attention";getSharedPreferences("music",0).edit().putString("folderSaveError",job.error).apply();}
  }catch(Exception e){if(!job.cancelled){job.state="failed";job.error=DownloadErrors.redact(e.getMessage());job.message=DownloadErrors.friendly(e.getMessage());if(DownloadErrors.rateLimited(e.getMessage()))getSharedPreferences("music",0).edit().putLong("youtubeCooldown",System.currentTimeMillis()+60000L).apply();getSharedPreferences("music",0).edit().putString("downloadDetails",job.error).apply();}}
  finally{if(session!=null)session.delete();if(job.cancelled){job.state="cancelled";job.message="Cancelled";}else if("running".equals(job.state)){job.state="failed";job.message="Interrupted · tap Retry";}publish(job);main.post(this::pump);}
 }
 private synchronized void publish(DownloadQueue.Job changed){
  List<DownloadQueue.Job> jobs=queue.all();int running=0,waiting=0;for(DownloadQueue.Job j:jobs){if("running".equals(j.state))running++;if("queued".equals(j.state))waiting++;}busy=running+waiting>0;
  if(busy)status=running+" downloading"+(waiting>0?" · "+waiting+" queued":"");else if(changed!=null)status=changed.message;else if(!jobs.isEmpty())status=jobs.get(jobs.size()-1).message;
  if(changed==null||!changed.pending()||SystemClock.elapsedRealtime()-lastPersist>=5000){lastPersist=SystemClock.elapsedRealtime();
  JSONArray saved=new JSONArray();for(DownloadQueue.Job j:jobs)try{saved.put(new JSONObject().put("key",j.key).put("id",j.id).put("title",j.title).put("format",j.format).put("state",j.state).put("message",j.message).put("error",j.error).put("progress",j.progress));}catch(JSONException ignored){}
  getSharedPreferences("music",0).edit().putString("downloadJobs",saved.toString()).putString("downloadStatus",status).apply();}
  if(active==this&&busy)getSystemService(NotificationManager.class).notify(20,notice());
 }
 private Notification notice(){PendingIntent open=PendingIntent.getActivity(this,20,new Intent(this,MainActivity.class).putExtra("downloads",true),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);PendingIntent cancel=PendingIntent.getService(this,21,new Intent(this,DownloadService.class).setAction("cancel"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);return new Notification.Builder(this,"downloads").setSmallIcon(R.drawable.ic_music).setContentTitle("Pocket Music downloads").setContentText(status).setContentIntent(open).setOnlyAlertOnce(true).setOngoing(true).setProgress(100,progress(this),progress(this)==0).addAction(new Notification.Action.Builder(null,"Cancel all",cancel).build()).build();}
 static String message(Exception e){String m=e.getMessage();return m==null?"Please retry.":m.substring(0,Math.min(700,m.length()));}
 @Override public void onTimeout(int id,int type){for(DownloadQueue.Job j:queue.all())if(j.pending()){queue.cancel(j.key);new Thread(()->YoutubeDL.getInstance().destroyProcessById("download-"+j.key)).start();}status="Downloads timed out. Retry from Downloads.";publish(null);stopSelf();}
 @Override public void onDestroy(){if(active==this){active=null;busy=false;}workers.shutdownNow();main.removeCallbacksAndMessages(null);super.onDestroy();}
 @Override public IBinder onBind(Intent i){return null;}
}
