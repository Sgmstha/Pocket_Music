package com.pocketmusic.android;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.*;

/** Explicitly labels the account feed and history-driven public discovery separately. */
final class Recommendations {
 static String scope(Context c){return YouTubeSession.fingerprint(c);}
 static boolean account(Context c){String scope=scope(c);return !scope.equals("guest")&&!scope.equals("unreadable");}
 static String seed(Context c){return c.getSharedPreferences("music",0).getString("recommendationSeed","popular music");}
 static List<Track> cached(Context c){SharedPreferences p=c.getSharedPreferences("music",0);String expected=scope(c);if(!p.getString("feedScope","").equals(expected))return List.of();if(expected.equals("unreadable"))return List.of();try{return TrackMetadata.parse(p.getString("feedTracks","{}"),12);}catch(Exception e){return List.of();}}
 static boolean fresh(Context c){SharedPreferences p=c.getSharedPreferences("music",0);return System.currentTimeMillis()>=p.getLong("feedTime",0)&&!cached(c).isEmpty()&&(account(c)||p.getString("feedBasis","").equals(seed(c)))&&System.currentTimeMillis()-p.getLong("feedTime",0)<(account(c)?3600000L:21600000L);}
 static String basis(Context c){return account(c)?"From your signed-in YouTube account · may include non-music videos":"Based on “"+c.getSharedPreferences("music",0).getString("feedBasis",seed(c))+"”";}
 static void fetch(Context c)throws Exception{fetch(c,"recommendations");}
 static void fetch(Context c,String process)throws Exception{String before=scope(c);if(before.equals("unreadable"))throw new java.io.IOException("Sign in again to reconnect YouTube.");boolean account=!before.equals("guest");String seed=seed(c);List<Track> tracks=account?MusicRepository.accountRecommendations(c,process):MusicRepository.search(c,seed,process);if(!before.equals(scope(c)))return;if(tracks.isEmpty())throw new java.io.IOException(account?"No account picks were returned. Try again later or reconnect YouTube.":"No suggestions were returned. Try a different search.");c.getSharedPreferences("music",0).edit().putString("feedScope",before).putString("feedBasis",seed).putString("feedTracks",TrackMetadata.encode(tracks)).putLong("feedTime",System.currentTimeMillis()).apply();}
 static void clear(Context c){c.getSharedPreferences("music",0).edit().remove("feedScope").remove("feedTracks").remove("feedTime").remove("feedBasis").remove("recommendations").apply();}
}
