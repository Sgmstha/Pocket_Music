package com.pocketmusic.android;
import android.app.job.*;
import java.util.concurrent.*;

/** Short, constrained JobScheduler work; never launch a foreground service from the background. */
public final class SmartDownloadJob extends JobService {
 private final ExecutorService work=Executors.newSingleThreadExecutor(),control=Executors.newSingleThreadExecutor();
 private SmartDownloads.Run run;private Future<?> job;
 @Override public boolean onStartJob(JobParameters params){run=new SmartDownloads.Run();SmartDownloads.Run current=run;job=work.submit(()->{try{SmartDownloads.refresh(this,current);}finally{if(!current.stopped)jobFinished(params,false);}});return true;}
 @Override public boolean onStopJob(JobParameters params){stop();return SmartDownloads.enabled(this);}
 private void stop(){if(run!=null){run.stopped=true;String process=run.process;control.execute(()->SmartDownloads.cancelProcess(process));}if(job!=null)job.cancel(true);}
 @Override public void onDestroy(){stop();work.shutdownNow();control.shutdown();super.onDestroy();}
}
