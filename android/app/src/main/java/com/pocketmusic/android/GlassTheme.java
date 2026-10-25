package com.pocketmusic.android;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;

/** Static translucency and edge highlights: no blur pass, render loop or animated shader. */
final class GlassTheme {
    static final int BG=0xff080c16, TEXT=0xffeff4ff, MUTED=0xffa8b6ce, ACCENT=0xffc9dcff;
    static int dp(Context c,int n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
    static Drawable panel(Context c,int radius,boolean selected){
        GradientDrawable fill=new GradientDrawable(GradientDrawable.Orientation.TL_BR,selected?new int[]{0x65abc8ff,0x304b689f}:new int[]{0x302b3b57,0x9a131c2e});
        fill.setCornerRadius(dp(c,radius));fill.setStroke(dp(c,1),selected?0x70d9e6ff:0x20d8e8ff);
        return fill;
    }
    static Drawable button(Context c,boolean selected){return new RippleDrawable(ColorStateList.valueOf(0x30d4e5ff),panel(c,28,selected),null);}
    static Drawable primary(Context c){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xffedf5ff,0xffa9c8ff});d.setCornerRadius(dp(c,32));d.setStroke(dp(c,1),0xccefffff);return d;}
    static Drawable background(){return new Backdrop();}
    static Drawable tile(Context c,int top,int bottom){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{top,bottom});d.setCornerRadius(dp(c,22));return d;}
    static Drawable sheet(Context c){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff1b2941,0xff0d1422});d.setCornerRadius(dp(c,30));d.setStroke(dp(c,1),0x40d8e8ff);return d;}
    private static final class Backdrop extends Drawable {
        private final Paint base=new Paint(),halo=new Paint(Paint.ANTI_ALIAS_FLAG);
        private Shader gradient,upper,lower;
        protected void onBoundsChange(Rect r){
            gradient=new LinearGradient(0,0,r.width(),r.height(),new int[]{0xff101a30,0xff080c16,0xff111322},null,Shader.TileMode.CLAMP);
            upper=new RadialGradient(r.width()*.95f,r.height()*.17f,Math.max(1,r.width()*.85f),new int[]{0x55375386,0x00375386},null,Shader.TileMode.CLAMP);
            lower=new RadialGradient(0,r.height()*.83f,Math.max(1,r.width()),new int[]{0x353f2f68,0x003f2f68},null,Shader.TileMode.CLAMP);
        }
        public void draw(Canvas c){base.setShader(gradient);c.drawRect(getBounds(),base);halo.setShader(upper);c.drawRect(getBounds(),halo);halo.setShader(lower);c.drawRect(getBounds(),halo);}
        public void setAlpha(int alpha){base.setAlpha(alpha);invalidateSelf();}
        public void setColorFilter(ColorFilter filter){base.setColorFilter(filter);invalidateSelf();}
        public int getOpacity(){return PixelFormat.OPAQUE;}
    }
}
