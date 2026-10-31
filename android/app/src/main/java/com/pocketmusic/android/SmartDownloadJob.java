package com.pocketmusic.android;
import android.app.job.*;
import java.util.concurrent.*;

/** Constrained background batches. Interrupted or incomplete fills retry automatically. */
public final class SmartDownloadJob extends JobService {
 private final ExecutorService work=Executors.newSingleThreadExecutor(),control=Executors.newSingleThreadExecutor();
 private final ConcurrentHashMap<Integer,Task> tasks=new ConcurrentHashMap<>();
 private static final class Task {final SmartDownloads.Run run=new SmartDownloads.Run();Future<?> future;}
 @Override public boolean onStartJob(JobParameters params){Task task=new Task();tasks.put(params.getJobId(),task);task.future=work.submit(()->{try{SmartDownloads.refresh(this,task.run);}finally{tasks.remove(params.getJobId(),task);if(!task.run.stopped){boolean retry=task.run.retry&&SmartDownloads.enabled(this);if(retry&&params.getJobId()==SmartDownloads.DAILY){SmartDownloads.schedule(this,true);retry=false;}jobFinished(params,retry);}}});return true;}
 @Override public boolean onStopJob(JobParameters params){stop(tasks.remove(params.getJobId()));return SmartDownloads.enabled(this);}
 private void stop(Task task){if(task==null)return;task.run.stopped=true;String process=task.run.process;control.execute(()->SmartDownloads.cancelProcess(process));if(task.future!=null)task.future.cancel(true);}
 @Override public void onDestroy(){for(Task task:tasks.values())stop(task);tasks.clear();work.shutdownNow();control.shutdown();super.onDestroy();}
}
