package com.pocketmusic.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.widget.Button;

/** Fixed-size accessible controls without relying on the device's symbol font. */
final class PlayerButton extends Button {
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final Path path=new Path();
 private final String kind;
 private boolean paused=true;
 public PlayerButton(Context c){this(c,"play");}
 PlayerButton(Context c,String kind){super(c);this.kind=kind;setText("");setMinWidth(0);setMinimumWidth(0);setMinHeight(0);setMinimumHeight(0);setElevation(0);}
 void setPlaying(boolean playing){paused=!playing;invalidate();}
 @Override protected void onDraw(Canvas canvas){
  super.onDraw(canvas);canvas.save();float size=24*getResources().getDisplayMetrics().density;
  canvas.translate((getWidth()-size)/2,(getHeight()-size)/2);canvas.scale(size/24,size/24);
  paint.setColor("play".equals(kind)?0xff101827:GlassTheme.ACCENT);paint.setStyle(Paint.Style.FILL);
  path.reset();
  if("play".equals(kind)){if(paused){path.moveTo(8,5);path.lineTo(20,12);path.lineTo(8,19);path.close();canvas.drawPath(path,paint);}else{canvas.drawRect(6,5,10,19,paint);canvas.drawRect(14,5,18,19,paint);}}
  else if("previous".equals(kind)){canvas.drawRect(4,5,6,19,paint);path.moveTo(19,5);path.lineTo(7,12);path.lineTo(19,19);path.close();canvas.drawPath(path,paint);}
  else if("next".equals(kind)){canvas.drawRect(18,5,20,19,paint);path.moveTo(5,5);path.lineTo(17,12);path.lineTo(5,19);path.close();canvas.drawPath(path,paint);}
  canvas.restore();
 }
}
