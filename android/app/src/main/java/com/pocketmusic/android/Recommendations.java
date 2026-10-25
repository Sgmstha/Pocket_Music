package com.pocketmusic.android;
import android.content.Context;
import org.json.*;
import java.util.*;
final class Recommendations {
 static List<Track> cached(Context c){List<Track> tracks=new ArrayList<>();try{JSONArray a=new JSONArray(c.getSharedPreferences("music",0).getString("recommendations","[]"));for(int i=0;i<Math.min(6,a.length());i++){JSONObject o=a.getJSONObject(i);String id=o.getString("id");if(id.matches("[A-Za-z0-9_-]{11}"))tracks.add(new Track(id,o.getString("title")));}}catch(Exception ignored){}return tracks;}
 static String seed(Context c){return c.getSharedPreferences("music",0).getString("recommendationSeed","popular music");}
 static boolean fresh(Context c){return !cached(c).isEmpty()&&System.currentTimeMillis()-c.getSharedPreferences("music",0).getLong("recommendationTime",0)<21600000L;}
 static void fetch(Context c)throws Exception{String seed=seed(c);List<Track> tracks=MusicRepository.search(c,seed,"recommendations");JSONArray a=new JSONArray();for(Track t:tracks.subList(0,Math.min(6,tracks.size())))a.put(new JSONObject().put("id",t.id).put("title",t.title));c.getSharedPreferences("music",0).edit().putString("recommendationBasis",seed).putString("recommendations",a.toString()).putLong("recommendationTime",System.currentTimeMillis()).apply();}
}
