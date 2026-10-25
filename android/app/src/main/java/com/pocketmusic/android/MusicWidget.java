package com.pocketmusic.android;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.graphics.Bitmap;
import android.widget.RemoteViews;
import java.io.File;

public final class MusicWidget extends AppWidgetProvider {
 @Override public void onUpdate(Context c,AppWidgetManager manager,int[] ids){update(c);}
 @Override public void onReceive(Context c,Intent intent){super.onReceive(c,intent);String action=intent.getAction();if(action==null||!action.startsWith("com.pocketmusic.widget."))return;String command=action.substring("com.pocketmusic.widget.".length());if(!command.equals("previous")&&!command.equals("toggle")&&!command.equals("next"))return;Intent service=new Intent(c,LocalPlayerService.class).setAction(command);if(LocalPlayerService.active==null){File file=new File(c.getSharedPreferences("music",0).getString("lastPlayed",""));if(!file.isFile()){c.startActivity(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));return;}service.putExtra("path",file.getAbsolutePath());}try{c.startForegroundService(service);}catch(RuntimeException e){c.startActivity(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}}
 static void update(Context c){
  AppWidgetManager manager=AppWidgetManager.getInstance(c);int[] ids=manager.getAppWidgetIds(new ComponentName(c,MusicWidget.class));if(ids.length==0)return;
  LocalPlayerService player=LocalPlayerService.active;RemoteViews views=new RemoteViews(c.getPackageName(),R.layout.music_widget);String name=player==null?"Pocket Music":player.title;views.setTextViewText(R.id.widget_title,name);views.setTextViewText(R.id.widget_source,player!=null&&player.playing()?"Playing saved music":"Saved music · tap Play or open app");views.setImageViewResource(R.id.widget_toggle,player!=null&&player.playing()?R.drawable.ic_pause:R.drawable.ic_play);
  if(player!=null&&player.artwork!=null){Bitmap small=Bitmap.createScaledBitmap(player.artwork,96,96,true);views.setImageViewBitmap(R.id.widget_art,small);}else views.setImageViewResource(R.id.widget_art,R.drawable.ic_music);
  views.setOnClickPendingIntent(R.id.widget_title,PendingIntent.getActivity(c,70,new Intent(c,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
  String[] actions={"previous","toggle","next"};int[] buttons={R.id.widget_previous,R.id.widget_toggle,R.id.widget_next};for(int i=0;i<actions.length;i++)views.setOnClickPendingIntent(buttons[i],PendingIntent.getBroadcast(c,71+i,new Intent(c,MusicWidget.class).setAction("com.pocketmusic.widget."+actions[i]),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
  manager.updateAppWidget(ids,views);
 }
}
