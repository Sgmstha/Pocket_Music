package com.pocketmusic.android;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import static com.pocketmusic.android.DesignTokens.*;

/** Static translucency and edge highlights: no blur pass, render loop or animated shader. */
final class GlassTheme {
    static final int BG=COLOR_BG, TEXT=COLOR_TEXT, MUTED=COLOR_TEXT_MUTED, ACCENT=COLOR_ACCENT;
    static int dp(Context c,int n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
    static Drawable panel(Context c,int radius,boolean selected){
        GradientDrawable fill=new GradientDrawable(GradientDrawable.Orientation.TL_BR,selected?new int[]{COLOR_SURFACE_RAISED,COLOR_SURFACE}:new int[]{COLOR_SURFACE,COLOR_BG});
        fill.setCornerRadius(dp(c,radius));
        return fill;
    }
    static Drawable button(Context c,boolean selected){return new RippleDrawable(ColorStateList.valueOf(0x30d4e5ff),panel(c,28,selected),null);}
    static Drawable primary(Context c){return tile(c,COLOR_ACCENT,COLOR_ACCENT);}
    static Drawable background(){return new Backdrop();}
    static Drawable ghost(Context c){return row(c);}
    static Drawable row(Context c){return new RippleDrawable(ColorStateList.valueOf(0x30c9dfff),null,tile(c,0xffffffff,0xffffffff));}
    static Drawable chip(Context c,boolean selected){return new RippleDrawable(ColorStateList.valueOf(0x30c9dfff),tile(c,selected?COLOR_ACCENT:COLOR_SURFACE,selected?COLOR_ACCENT:COLOR_SURFACE),null);}
    static Drawable navigation(Context c){return tile(c,COLOR_SURFACE,COLOR_BG);}
    static Drawable selectedNav(Context c){return tile(c,COLOR_SURFACE_RAISED,COLOR_SURFACE_RAISED);}
    static Drawable tile(Context c,int top,int bottom){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{top,bottom});d.setCornerRadius(dp(c,RADIUS_CARD));return d;}
    static Drawable coverFrame(Context c){GradientDrawable d=new GradientDrawable();d.setColor(COLOR_SURFACE_RAISED);d.setCornerRadius(dp(c,RADIUS_SMALL));return d;}
    static Drawable input(Context c){StateListDrawable states=new StateListDrawable();for(boolean focused:new boolean[]{true,false}){GradientDrawable d=new GradientDrawable();d.setColor(COLOR_SURFACE);d.setCornerRadius(dp(c,RADIUS_CARD));d.setStroke(dp(c,focused?2:1),focused?COLOR_ACCENT:COLOR_BORDER_INPUT);states.addState(focused?new int[]{android.R.attr.state_focused}:new int[]{},d);}return states;}
    static Drawable sheet(Context c){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{COLOR_SURFACE_RAISED,COLOR_BG});d.setCornerRadius(dp(c,RADIUS_SHEET));return d;}
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
