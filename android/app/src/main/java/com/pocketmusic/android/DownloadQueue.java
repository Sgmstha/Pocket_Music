package com.pocketmusic.android;
import java.util.*;

/** Bounded parallel work, independent cancellation and duplicate suppression. */
final class DownloadQueue {
 static final class Job {
  final String key,id,title,format;volatile String state="queued",message="Waiting",error="";volatile int progress=-1;volatile long etaSeconds=-1;volatile String speed="";volatile boolean cancelled,saveOnly;
  Job(String id,String title,String format){this(UUID.randomUUID().toString(),id,title,format);}
  Job(String key,String id,String title,String format){this.key=key;this.id=id;this.title=title;this.format=format;}
  boolean pending(){return "queued".equals(state)||"running".equals(state);}
 }
 private final List<Job> jobs=new ArrayList<>();private final int limit;
 DownloadQueue(int limit){this.limit=Math.max(1,limit);}
 synchronized Job enqueue(String id,String title,String format){for(Job j:jobs)if(j.pending()&&j.id.equals(id)&&j.format.equals(format))return j;Job job=new Job(id,title,format);jobs.add(job);trim();return job;}
 synchronized void restore(Job job){if(find(job.key)==null)jobs.add(job);}
 synchronized Job find(String key){for(Job j:jobs)if(j.key.equals(key))return j;return null;}
 synchronized List<Job> all(){return new ArrayList<>(jobs);}
 synchronized List<Job> startable(){int count=0;for(Job j:jobs)if("running".equals(j.state))count++;List<Job> ready=new ArrayList<>();for(Job j:jobs)if(count<limit&&"queued".equals(j.state)&&!j.cancelled&&!sameVideoRunning(j.id)){j.state="running";ready.add(j);count++;}return ready;}
 private boolean sameVideoRunning(String id){for(Job j:jobs)if("running".equals(j.state)&&j.id.equals(id))return true;return false;}
 synchronized void cancel(String key){Job j=find(key);if(j!=null&&j.pending()){j.cancelled=true;if("queued".equals(j.state)){j.state="cancelled";j.message="Cancelled";}}}
 synchronized boolean retry(String key){Job j=find(key);if(j==null||j.pending()||"done".equals(j.state))return false;for(Job other:jobs)if(other!=j&&other.pending()&&other.id.equals(j.id)&&other.format.equals(j.format))return false;j.saveOnly="saveFailed".equals(j.state);j.cancelled=false;j.progress=-1;j.etaSeconds=-1;j.speed="";j.error="";j.state="queued";j.message="Waiting";return true;}
 synchronized boolean pending(){for(Job j:jobs)if(j.pending())return true;return false;}
 synchronized List<Job> clearFinished(){List<Job> removed=new ArrayList<>();for(Iterator<Job> it=jobs.iterator();it.hasNext();){Job job=it.next();if(!job.pending()){removed.add(job);it.remove();}}return removed;}
 private void trim(){while(jobs.size()>40){int remove=-1;for(int i=0;i<jobs.size();i++)if(!jobs.get(i).pending()){remove=i;break;}if(remove<0)break;jobs.remove(remove);}}
}
