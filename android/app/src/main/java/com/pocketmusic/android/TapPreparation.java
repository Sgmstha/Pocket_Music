package com.pocketmusic.android;

import android.content.Context;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;

/** Resolve two visible song URLs and optionally buffer one short audio prefix in memory. */
@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
final class TapPreparation {
 static Predicate<Context> allowed=LocalPlayerService::unmeteredPreparation;
 static OnlineAudio.Resolver resolver=OnlineAudio::warm;
 private static final ExecutorService work=Executors.newFixedThreadPool(2),control=Executors.newSingleThreadExecutor();
 private static final Map<String,Job> jobs=new LinkedHashMap<>();
 private static final Map<String,OnlineAudio.Source> awaiting=new HashMap<>();
 private static final android.os.Handler main=new android.os.Handler(android.os.Looper.getMainLooper());
 private static final class Job {
  final String owner,key,id,process="tap-ready-"+UUID.randomUUID();final long created=android.os.SystemClock.elapsedRealtime();final FutureTask<OnlineAudio.Source> task;final boolean buffer;boolean claimed;
  Job(Context context,String owner,Track track,boolean buffer){this.buffer=buffer;this.owner=owner;id=track.id;key=OnlineAudio.scope(context,track.id);task=new FutureTask<>(()->resolver.resolve(context,track,process)){@Override protected void done(){if(buffer&&!isCancelled())main.post(()->prepareMedia(context,Job.this));}};}
 }
 record Result(OnlineAudio.Source source,boolean failed){}
 static synchronized void plan(Context context,String owner,List<Track> tracks){
  Context app=context.getApplicationContext();if(!allowed.test(app)){cancel(owner);return;}
  Set<String> wanted=new LinkedHashSet<>();List<Track> selected=new ArrayList<>();for(Track t:tracks){if(!t.music||!t.id.matches("[A-Za-z0-9_-]{11}"))continue;String key=OnlineAudio.scope(app,t.id);if(wanted.add(key)){selected.add(t);if(selected.size()==2)break;}}
  Iterator<Job> entries=jobs.values().iterator();while(entries.hasNext()){Job job=entries.next();if(job.claimed&&expired(job)||job.owner.equals(owner)&&!job.claimed&&(!wanted.contains(job.key)||expired(job))){entries.remove();stop(job);}}
  for(Track t:selected){String key=OnlineAudio.scope(app,t.id);if(jobs.containsKey(key))continue;Job job=new Job(app,owner,t,t==selected.get(0));jobs.put(key,job);work.execute(job.task);}
 }
 private static boolean expired(Job job){return android.os.SystemClock.elapsedRealtime()-job.created>=300000;}
 static synchronized void prioritize(Context context,String owner,Track track){String key=OnlineAudio.scope(context,track.id);Job chosen=jobs.get(key);if(chosen!=null&&!expired(chosen))chosen.claimed=true;cancel(owner);}
 static synchronized void cancel(String owner){Iterator<Job> entries=jobs.values().iterator();while(entries.hasNext()){Job job=entries.next();if(job.owner.equals(owner)&&!job.claimed){entries.remove();stop(job);}}}
 static synchronized void cancelClaim(Context context,Track track){Job job=jobs.get(OnlineAudio.scope(context,track.id));if(job!=null&&job.claimed){jobs.remove(job.key);stop(job);}}
 static Result take(Context context,Track track)throws InterruptedException{return take(context,track,"test-"+UUID.randomUUID());}
 static Result take(Context context,Track track,String process)throws InterruptedException{
  String key=OnlineAudio.scope(context,track.id);Job job;synchronized(TapPreparation.class){job=jobs.remove(key);}
  if(job==null)return new Result(null,false);if(expired(job)){stop(job);return new Result(null,false);}
  try{OnlineAudio.Source source=job.task.get(9,TimeUnit.SECONDS);if(!key.equals(OnlineAudio.scope(context,track.id))||source!=null&&!OnlineAudio.sourceFresh(source)){releaseSource(source);return new Result(null,false);}if(source!=null&&source.media!=null)synchronized(TapPreparation.class){awaiting.put(process,source);}return new Result(source,source==null);}
  catch(InterruptedException e){stop(job);throw e;}catch(CancellationException e){return new Result(null,false);}catch(ExecutionException|TimeoutException e){stop(job);return new Result(null,key.equals(OnlineAudio.scope(context,track.id)));}
 }
 private static void stop(Job job){if(job.task.isDone()&&!job.task.isCancelled())try{releaseSource(job.task.get());}catch(Exception ignored){}job.task.cancel(true);control.execute(()->MusicCatalog.cancel(job.process));}
 private static synchronized void prepareMedia(Context context,Job job){if(!job.key.equals(OnlineAudio.scope(context,job.id))||jobs.get(job.key)!=job||job.claimed||!allowed.test(context)||LocalPlayerService.active!=null||expired(job))return;try{OnlineAudio.Source source=job.task.get();if(source!=null&&StreamUrlPolicy.allowed(source.url)&&OnlineAudio.sourceFresh(source)&&source.media==null)source.media=new AudioPlayer.Preparation(context,source);}catch(Exception ignored){}}
 static synchronized AudioPlayer.Preparation adopt(OnlineAudio.Source source){awaiting.values().removeIf(value->value==source);AudioPlayer.Preparation prepared=source.media;source.media=null;if(prepared!=null&&prepared.failed){prepared.release();return null;}return prepared!=null&&!prepared.released?prepared:null;}
 private static synchronized void releaseSource(OnlineAudio.Source source){if(source==null)return;AudioPlayer.Preparation prepared=source.media;source.media=null;if(prepared!=null)main.post(prepared::release);}
 static synchronized void discard(String process){OnlineAudio.Source source=awaiting.remove(process);releaseSource(source);}
 static synchronized boolean mediaPresent(Context context,Track track){Job job=jobs.get(OnlineAudio.scope(context,track.id));if(job==null||!job.task.isDone())return false;try{OnlineAudio.Source source=job.task.get();return source!=null&&source.media!=null;}catch(Exception e){return false;}}
 static synchronized boolean ready(Context context,Track track){Job job=jobs.get(OnlineAudio.scope(context,track.id));if(job==null||!job.task.isDone())return false;try{OnlineAudio.Source source=job.task.get();return source!=null&&source.media!=null&&source.media.ready;}catch(Exception e){return false;}}
 static synchronized void clear(){for(Job job:jobs.values())stop(job);jobs.clear();for(OnlineAudio.Source source:awaiting.values())releaseSource(source);awaiting.clear();}
}
