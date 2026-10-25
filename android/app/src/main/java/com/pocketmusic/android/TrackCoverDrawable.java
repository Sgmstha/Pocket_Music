package com.pocketmusic.android;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import static com.pocketmusic.android.DesignTokens.*;

/** Original deterministic fallback, not fabricated album art or a reference-app asset. */
final class TrackCoverDrawable extends Drawable {
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private final String letter;private final int color;
 TrackCoverDrawable(String title){String value=title.trim();letter=value.isEmpty()?"♪":value.substring(0,value.offsetByCodePoints(0,1)).toUpperCase(java.util.Locale.ROOT);int[] colors={COLOR_MOOD_FOCUS,COLOR_MOOD_CALM,COLOR_MOOD_NIGHT,COLOR_MOOD_ENERGY};color=colors[Math.floorMod(title.hashCode(),colors.length)];}
 public void draw(Canvas canvas){Rect b=getBounds();float w=b.width(),h=b.height(),x=b.left,y=b.top;paint.setColor(color);paint.setStyle(Paint.Style.FILL);canvas.drawRect(b,paint);paint.setColor(COLOR_ACCENT);paint.setAlpha(30);canvas.drawCircle(x+w*.9f,y+h*.15f,w*.55f,paint);canvas.drawCircle(x+w*.1f,y+h*.95f,w*.38f,paint);paint.setAlpha(255);paint.setColor(COLOR_TEXT);paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(Math.min(w,h)*.42f);canvas.drawText(letter,x+w*.5f,y+h*.5f-(paint.ascent()+paint.descent())*.5f,paint);}
 public void setAlpha(int alpha){}public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);}public int getOpacity(){return PixelFormat.OPAQUE;}
}
