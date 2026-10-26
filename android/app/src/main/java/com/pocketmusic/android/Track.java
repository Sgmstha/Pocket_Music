package com.pocketmusic.android;
public final class Track {
 public final String id,title,artist;public final long durationSeconds;
 public Track(String i,String t){this(i,t,"",0);}
 public Track(String i,String t,String a,long seconds){id=i;title=t;artist=a==null?"":a;durationSeconds=Math.max(0,seconds);}
 public String subtitle(){String length=durationSeconds>0?String.format(java.util.Locale.ROOT,"%d:%02d",durationSeconds/60,durationSeconds%60):"";return artist.isEmpty()?length:length.isEmpty()?artist:artist+" · "+length;}
}
