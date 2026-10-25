package com.pocketmusic.android;

import android.app.*;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import static com.pocketmusic.android.DesignTokens.*;

/** Local audio controls. The official YouTube player stays in its visible preview panel. */
final class NowPlayingSheet extends Dialog {
 interface Actions {void toggle();void step(int direction);void seek(double seconds);void volume(int percent);void shuffle();void repeat();void queue();void sleep();}
 private final Activity owner;private final Actions actions;
 private TextView title,source,time;private ImageView cover;private Button play,shuffle,repeat;private SeekBar seek,volume;
 private boolean tracking;private double duration;private Bitmap lastCover;private Boolean lastShuffle;private int lastRepeat=-1;private String lastTitle="";
 NowPlayingSheet(Activity owner,Actions actions){super(owner);this.owner=owner;this.actions=actions;}
 @Override protected void onCreate(Bundle saved){
  super.onCreate(saved);requestWindowFeature(Window.FEATURE_NO_TITLE);
  LinearLayout root=column();root.setPadding(dp(SPACE_24),dp(SPACE_16),dp(SPACE_24),dp(SPACE_24));root.setBackground(GlassTheme.sheet(owner));
  LinearLayout heading=row();TextView label=text("NOW PLAYING",TYPE_CAPTION);label.setTextColor(COLOR_TEXT_MUTED);heading.addView(label,new LinearLayout.LayoutParams(0,-2,1));heading.addView(button("Timer",actions::sleep));heading.addView(button("Close",this::dismiss));root.addView(heading);
  cover=new ImageView(owner);cover.setImageResource(R.drawable.ic_music);cover.setScaleType(ImageView.ScaleType.CENTER_CROP);cover.setContentDescription("Current track artwork");cover.setBackground(GlassTheme.tile(owner,COLOR_SURFACE,COLOR_SURFACE_RAISED));cover.setClipToOutline(true);
  int edge=Math.min(owner.getResources().getDisplayMetrics().widthPixels-dp(SPACE_48*2),(int)(owner.getResources().getDisplayMetrics().heightPixels*.34f));LinearLayout.LayoutParams picture=new LinearLayout.LayoutParams(edge,edge);picture.gravity=Gravity.CENTER_HORIZONTAL;picture.topMargin=dp(SPACE_16);picture.bottomMargin=dp(SPACE_24);root.addView(cover,picture);
  title=text("",TYPE_SECTION);title.setTypeface(null,android.graphics.Typeface.BOLD);title.setMaxLines(2);title.setEllipsize(android.text.TextUtils.TruncateAt.END);root.addView(title);source=text("Saved audio · Pocket Music",TYPE_CAPTION);source.setTextColor(COLOR_TEXT_MUTED);source.setPadding(0,dp(SPACE_8),0,dp(SPACE_12));root.addView(source);
  seek=new SeekBar(owner);seek.setMax(1000);seek.setMinimumHeight(dp(SIZE_TOUCH));seek.setContentDescription("Expanded playback position");tint(seek);root.addView(seek);time=text("0:00 / 0:00",TYPE_CAPTION);time.setTextColor(COLOR_TEXT_MUTED);root.addView(time);
  seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){tracking=true;}public void onStopTrackingTouch(SeekBar s){actions.seek(duration*s.getProgress()/1000);tracking=false;}public void onProgressChanged(SeekBar s,int p,boolean user){}});
  LinearLayout transport=row();transport.setPadding(0,dp(SPACE_16),0,dp(SPACE_16));transport.addView(icon("previous","Previous track",()->actions.step(-1)),new LinearLayout.LayoutParams(dp(SIZE_NAV),dp(SIZE_NAV)));
  play=button("Play",actions::toggle);play.setBackground(GlassTheme.primary(owner));play.setTextColor(COLOR_ON_ACCENT);play.setTextSize(TYPE_TITLE);LinearLayout.LayoutParams main=new LinearLayout.LayoutParams(0,dp(SIZE_NAV),1);main.leftMargin=main.rightMargin=dp(SPACE_12);transport.addView(play,main);transport.addView(icon("next","Next track",()->actions.step(1)),new LinearLayout.LayoutParams(dp(SIZE_NAV),dp(SIZE_NAV)));root.addView(transport);
  LinearLayout options=row();shuffle=button("Shuffle",actions::shuffle);repeat=button("Repeat",actions::repeat);Button queue=button("Queue",actions::queue);for(Button b:new Button[]{shuffle,repeat,queue}){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(SIZE_TOUCH),1);p.rightMargin=dp(SPACE_4);options.addView(b,p);}root.addView(options);
  LinearLayout volumeRow=row();volumeRow.setPadding(0,dp(SPACE_12),0,0);volumeRow.addView(text("Volume",TYPE_CAPTION));volume=new SeekBar(owner);volume.setMax(100);volume.setMinimumHeight(dp(SIZE_TOUCH));volume.setContentDescription("Expanded playback volume");tint(volume);volumeRow.addView(volume,new LinearLayout.LayoutParams(0,-2,1));root.addView(volumeRow);
  volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}public void onProgressChanged(SeekBar s,int p,boolean user){if(user)actions.volume(p);}});
  ScrollView scroll=new ScrollView(owner);scroll.setFillViewport(true);scroll.addView(root);setContentView(scroll);Window w=getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);WindowManager.LayoutParams p=w.getAttributes();p.gravity=Gravity.BOTTOM;p.dimAmount=.75f;w.setAttributes(p);w.setNavigationBarColor(COLOR_BG);}
 }
 @Override public void show(){super.show();Window w=getWindow();if(w!=null){w.setGravity(Gravity.BOTTOM);w.setLayout(-1,(int)(owner.getResources().getDisplayMetrics().heightPixels*.92f));}}
 void update(String name,String hint,Bitmap image,boolean playing,double position,double length,int percent,boolean shuffled,int repeatMode){
  if(title==null)return;title.setText(name);duration=length;play.setText(playing?"Pause":"Play");play.setContentDescription(playing?"Pause":"Play");source.setText(playing?"Playing from your collection":"Saved audio · Pocket Music");if(!tracking)seek.setProgress(length>0?(int)(position*1000/length):0);seek.setEnabled(length>0);time.setText(clock(position)+" / "+clock(length));volume.setProgress(percent);
  if(image!=lastCover||(image==null&&!name.equals(lastTitle))){lastCover=image;lastTitle=name;if(image==null)cover.setImageDrawable(new TrackCoverDrawable(name));else cover.setImageBitmap(image);}
  shuffle.setText(shuffled?"Shuffle on":"Shuffle");if(lastShuffle==null||lastShuffle!=shuffled){lastShuffle=shuffled;shuffle.setBackground(GlassTheme.chip(owner,shuffled));shuffle.setTextColor(shuffled?COLOR_ON_ACCENT:COLOR_TEXT);}
  repeat.setText(repeatMode==1?"Repeat all":repeatMode==2?"Repeat one":"Repeat");if(lastRepeat!=repeatMode){lastRepeat=repeatMode;repeat.setBackground(GlassTheme.chip(owner,repeatMode!=0));repeat.setTextColor(repeatMode!=0?COLOR_ON_ACCENT:COLOR_TEXT);}
 }
 private PlayerButton icon(String kind,String label,Runnable run){PlayerButton b=new PlayerButton(owner,kind);b.setContentDescription(label);b.setBackground(GlassTheme.chip(owner,false));b.setOnClickListener(v->run.run());return b;}
 private Button button(String name,Runnable run){Button b=new Button(owner);b.setAllCaps(false);b.setText(name);b.setTextSize(TYPE_CAPTION);b.setTextColor(COLOR_TEXT);b.setMinHeight(dp(SIZE_TOUCH));b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(dp(SPACE_8),0,dp(SPACE_8),0);b.setBackground(GlassTheme.ghost(owner));b.setStateListAnimator(null);b.setOnClickListener(v->run.run());return b;}
 private TextView text(String value,int size){TextView t=new TextView(owner);t.setText(value);t.setTextSize(size);t.setTextColor(COLOR_TEXT);t.setIncludeFontPadding(false);return t;}
 private LinearLayout row(){LinearLayout l=column();l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
 private LinearLayout column(){LinearLayout l=new LinearLayout(owner);l.setOrientation(LinearLayout.VERTICAL);return l;}
 private void tint(SeekBar s){s.setProgressTintList(ColorStateList.valueOf(COLOR_ACCENT));s.setThumbTintList(ColorStateList.valueOf(COLOR_ACCENT));}
 private int dp(int value){return GlassTheme.dp(owner,value);}
 private static String clock(double seconds){int s=(int)Math.max(0,seconds);return String.format(java.util.Locale.ROOT,"%d:%02d",s/60,s%60);}
}
