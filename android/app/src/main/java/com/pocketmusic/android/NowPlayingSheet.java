package com.pocketmusic.android;

import android.app.*;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

/** Expanded controls for native local audio; YouTube keeps its visible embedded panel. */
final class NowPlayingSheet extends Dialog {
 interface Actions {void toggle();void step(int direction);void seek(double seconds);void volume(int percent);void shuffle();void repeat();void queue();}
 private final Activity owner;private final Actions actions;
 private TextView title,source,time;private ImageView cover;private PlayerButton play;
 private SeekBar seek,volume;private Button shuffle,repeat;private boolean tracking;private double duration;private Bitmap lastCover;private Boolean lastShuffle;private int lastRepeat=-1;
 NowPlayingSheet(Activity owner,Actions actions){super(owner);this.owner=owner;this.actions=actions;}
 protected void onCreate(Bundle saved){
  super.onCreate(saved);requestWindowFeature(Window.FEATURE_NO_TITLE);
  LinearLayout root=column();root.setPadding(dp(22),dp(16),dp(22),dp(18));root.setBackground(GlassTheme.panel(owner,30,false));
  LinearLayout header=row();header.addView(label("NOW PLAYING",11),new LinearLayout.LayoutParams(0,-2,1));header.addView(button("Close",this::dismiss));root.addView(header);
  cover=new ImageView(owner);cover.setScaleType(ImageView.ScaleType.FIT_CENTER);cover.setImageResource(R.drawable.ic_music);cover.setContentDescription("Current track artwork");root.addView(cover,new LinearLayout.LayoutParams(-1,dp(164)));
  title=label("",22);title.setMaxLines(2);title.setEllipsize(android.text.TextUtils.TruncateAt.END);root.addView(title);source=label("Local audio · background playback",12);source.setTextColor(GlassTheme.MUTED);source.setPadding(0,dp(6),0,dp(8));root.addView(source);
  seek=new SeekBar(owner);seek.setMax(1000);seek.setContentDescription("Expanded playback position");tint(seek);root.addView(seek);time=label("0:00 / 0:00",12);root.addView(time);
  seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){tracking=true;}public void onStopTrackingTouch(SeekBar s){actions.seek(duration*s.getProgress()/1000);tracking=false;}public void onProgressChanged(SeekBar s,int p,boolean user){}});
  LinearLayout transport=row();transport.setGravity(Gravity.CENTER);transport.addView(icon("previous","Previous track",()->actions.step(-1)),new LinearLayout.LayoutParams(dp(64),dp(64)));play=icon("play","Play or pause",actions::toggle);transport.addView(play,new LinearLayout.LayoutParams(dp(64),dp(64)));transport.addView(icon("next","Next track",()->actions.step(1)),new LinearLayout.LayoutParams(dp(64),dp(64)));root.addView(transport);
  LinearLayout options=row();shuffle=button("Shuffle",actions::shuffle);repeat=button("Repeat",actions::repeat);options.addView(shuffle,new LinearLayout.LayoutParams(0,-2,1));options.addView(repeat,new LinearLayout.LayoutParams(0,-2,1));options.addView(button("Queue",actions::queue),new LinearLayout.LayoutParams(0,-2,1));root.addView(options);
  LinearLayout vol=row();vol.addView(label("Volume",12));volume=new SeekBar(owner);volume.setMax(100);volume.setContentDescription("Expanded playback volume");tint(volume);vol.addView(volume,new LinearLayout.LayoutParams(0,-2,1));root.addView(vol);
  volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}public void onProgressChanged(SeekBar s,int p,boolean user){if(user)actions.volume(p);}});
  ScrollView scroll=new ScrollView(owner);scroll.setFillViewport(true);scroll.addView(root);setContentView(scroll);
  Window window=getWindow();if(window!=null){window.setBackgroundDrawableResource(android.R.color.transparent);window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);WindowManager.LayoutParams p=window.getAttributes();p.gravity=Gravity.BOTTOM;p.dimAmount=.65f;window.setAttributes(p);window.setNavigationBarColor(GlassTheme.BG);}
 }
 public void show(){super.show();Window w=getWindow();if(w!=null){w.setGravity(Gravity.BOTTOM);w.setLayout(-1,Math.min(dp(620),(int)(owner.getResources().getDisplayMetrics().heightPixels*.86f)));}}
 void update(String name,String hint,Bitmap image,boolean playing,double position,double length,int percent,boolean shuffled,int repeatMode){
  if(title==null)return;title.setText(name);source.setText("Local audio · background playback");duration=length;play.setPlaying(playing);play.setContentDescription(playing?"Pause":"Play");if(!tracking)seek.setProgress(length>0?(int)(position*1000/length):0);seek.setEnabled(length>0);time.setText(clock(position)+" / "+clock(length));volume.setProgress(percent);
  if(image!=lastCover){lastCover=image;if(image==null)cover.setImageResource(R.drawable.ic_music);else cover.setImageBitmap(image);}
  shuffle.setText(shuffled?"Shuffle · On":"Shuffle");if(lastShuffle==null||lastShuffle!=shuffled){lastShuffle=shuffled;shuffle.setBackground(GlassTheme.button(owner,shuffled));}repeat.setText(repeatMode==1?"Repeat · All":repeatMode==2?"Repeat · One":"Repeat");if(lastRepeat!=repeatMode){lastRepeat=repeatMode;repeat.setBackground(GlassTheme.button(owner,repeatMode!=0));}
 }
 private PlayerButton icon(String kind,String hint,Runnable action){PlayerButton b=new PlayerButton(owner,kind);b.setContentDescription(hint);b.setBackground("play".equals(kind)?GlassTheme.primary(owner):GlassTheme.button(owner,false));b.setOnClickListener(v->action.run());return b;}
 private Button button(String name,Runnable action){Button b=new Button(owner);b.setText(name);b.setTextSize(12);b.setAllCaps(false);b.setMinHeight(dp(48));b.setTextColor(GlassTheme.TEXT);b.setElevation(0);b.setStateListAnimator(null);b.setBackground(GlassTheme.button(owner,false));b.setOnClickListener(v->action.run());return b;}
 private void tint(SeekBar s){s.setProgressTintList(ColorStateList.valueOf(GlassTheme.ACCENT));s.setThumbTintList(ColorStateList.valueOf(GlassTheme.ACCENT));}
 private TextView label(String value,int size){TextView t=new TextView(owner);t.setText(value);t.setTextSize(size);t.setTextColor(GlassTheme.TEXT);return t;}
 private LinearLayout row(){LinearLayout l=new LinearLayout(owner);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
 private LinearLayout column(){LinearLayout l=new LinearLayout(owner);l.setOrientation(LinearLayout.VERTICAL);return l;}
 private int dp(int n){return GlassTheme.dp(owner,n);}
 private static String clock(double seconds){int s=(int)Math.max(0,seconds);return String.format(java.util.Locale.ROOT,"%d:%02d",s/60,s%60);}
}
