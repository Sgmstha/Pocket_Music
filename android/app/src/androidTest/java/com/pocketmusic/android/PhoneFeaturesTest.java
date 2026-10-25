package com.pocketmusic.android;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.widget.*;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import java.io.*;
import java.nio.*;
import static org.junit.Assert.*;

public class PhoneFeaturesTest {
 private Instrumentation test;private Context context;private Activity activity;private File file;
 private void main(Runnable action){test.runOnMainSync(action);}
 private void await(java.util.function.BooleanSupplier condition)throws Exception{long until=System.currentTimeMillis()+7000;while(!condition.getAsBoolean()&&System.currentTimeMillis()<until)Thread.sleep(80);assertTrue(condition.getAsBoolean());}
 @Before public void setup()throws Exception{
  test=InstrumentationRegistry.getInstrumentation();context=test.getTargetContext();main(()->{SleepTimer.set(context,0);context.stopService(new Intent(context,LocalPlayerService.class));});await(()->LocalPlayerService.active==null);
  int samples=22050*20;ByteBuffer b=ByteBuffer.allocate(44+samples*2).order(ByteOrder.LITTLE_ENDIAN);b.put("RIFF".getBytes()).putInt(36+samples*2).put("WAVEfmt ".getBytes()).putInt(16).putShort((short)1).putShort((short)1).putInt(22050).putInt(44100).putShort((short)2).putShort((short)16).put("data".getBytes()).putInt(samples*2);file=new File(MusicRepository.folder(context),"Phone Features.wav");try(FileOutputStream out=new FileOutputStream(file)){out.write(b.array());}
  activity=test.startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));main(()->context.startForegroundService(new Intent(context,LocalPlayerService.class).putExtra("path",file.getAbsolutePath())));await(()->LocalPlayerService.active!=null&&LocalPlayerService.active.playing());
 }
 @After public void cleanup()throws Exception{main(()->{SleepTimer.set(context,0);context.stopService(new Intent(context,LocalPlayerService.class));if(activity!=null)activity.finish();});await(()->LocalPlayerService.active==null);if(file!=null)file.delete();}
 @Test public void sleepTimerPausesAfterHome()throws Exception{LocalPlayerService p=LocalPlayerService.active;main(()->SleepTimer.set(context,500));context.startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));await(()->!p.playing());assertEquals("paused",p.state);assertEquals(0,SleepTimer.remaining(context));}
 @Test public void cancelledTimerLeavesPlaybackRunning()throws Exception{main(()->{SleepTimer.set(context,300);SleepTimer.set(context,0);});Thread.sleep(650);assertTrue(LocalPlayerService.active.playing());}
 @Test public void widgetUsesRealPlayerAndRemoteViewsLayout()throws Exception{boolean found=false;for(AppWidgetProviderInfo info:AppWidgetManager.getInstance(context).getInstalledProviders())if(info.provider.getClassName().equals(MusicWidget.class.getName()))found=true;assertTrue("Widget provider registered",found);main(()->{android.view.View view=new RemoteViews(context.getPackageName(),R.layout.music_widget).apply(context,new FrameLayout(context));assertNotNull(view.findViewById(R.id.widget_toggle));new MusicWidget().onReceive(context,new Intent(context,MusicWidget.class).setAction("com.pocketmusic.widget.toggle"));});await(()->!LocalPlayerService.active.playing());main(()->new MusicWidget().onReceive(context,new Intent(context,MusicWidget.class).setAction("com.pocketmusic.widget.toggle")));await(()->LocalPlayerService.active.playing());}
}
