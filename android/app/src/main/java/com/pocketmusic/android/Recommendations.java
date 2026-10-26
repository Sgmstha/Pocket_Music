package com.pocketmusic.android;
import android.content.Context;
import android.content.SharedPreferences;
import org.json.*;
import java.util.*;

/** Music-only metadata shelves. The old general YouTube account feed is never displayed. */
final class Recommendations {
 interface SongSource {List<Track> find(String query,String process)throws Exception;}
 static String scope(Context c){return YouTubeSession.fingerprint(c);}
 static boolean account(Context c){String s=scope(c);return !s.equals("guest")&&!s.equals("unreadable");}
 static String seed(Context c){return MusicTaste.seeds(c).get(0);}
 static List<DiscoveryShelf> shelves(Context c){SharedPreferences p=c.getSharedPreferences("music",0);if(p.getInt("discoverySchema",0)!=4||!p.getString("musicScope","").equals(scope(c)))return List.of();List<DiscoveryShelf> shelves=new ArrayList<>();try{JSONArray a=new JSONArray(p.getString("musicShelves","[]"));for(int i=0;i<Math.min(4,a.length());i++){JSONObject o=a.getJSONObject(i);List<Track> all=TrackMetadata.parse(o.getString("tracks"),20);List<Track> filtered=RecommendationPolicy.select(all,MusicTaste.hidden(c),new HashSet<>(),12,12);if(!filtered.isEmpty())shelves.add(new DiscoveryShelf(o.getString("title"),o.getString("reason"),filtered));}}catch(Exception ignored){}return shelves;}
 static List<Track> cached(Context c){LinkedHashMap<String,Track> all=new LinkedHashMap<>();for(DiscoveryShelf shelf:shelves(c))for(Track t:shelf.tracks)all.putIfAbsent(t.id,t);return new ArrayList<>(all.values());}
 static boolean fresh(Context c){SharedPreferences p=c.getSharedPreferences("music",0);long age=System.currentTimeMillis()-p.getLong("musicTime",0);return age>=0&&age<3*3600000L&&!cached(c).isEmpty()&&p.getLong("musicProfileVersion",-1)==MusicTaste.version(c)&&!(age>1800000L&&MusicTaste.recent(c).size()>p.getInt("musicHistorySize",0));}
 static String basis(Context c){return "YouTube Music songs · shaped by your on-device taste";}
 static void fetch(Context c)throws Exception{fetch(c,"music-discovery");}
 static void fetch(Context c,String process)throws Exception{fetch(c,process,MusicCatalog::songs);}
 static void fetch(Context c,String process,SongSource source)throws Exception{
  SharedPreferences prefs=c.getSharedPreferences("music",0);if(System.currentTimeMillis()<prefs.getLong("musicCooldown",0))throw new java.io.IOException("HTTP 429: Wait before refreshing music.");String before=scope(c);long version=MusicTaste.version(c);List<String> seeds=MusicTaste.seeds(c);List<DiscoveryShelf> sections=new ArrayList<>();List<Track> combined=new ArrayList<>();Set<String> seen=new HashSet<>();int failures=0;
  for(int i=0;i<Math.min(3,seeds.size());i++){if(Thread.currentThread().isInterrupted())throw new java.io.InterruptedIOException("Music refresh cancelled.");try{List<Track> found=new ArrayList<>(source.find(seeds.get(i),process+"-"+i));if(!found.isEmpty())Collections.rotate(found,-(int)Math.floorMod(prefs.getLong("musicRotation",0),found.size()));List<Track> songs=RecommendationPolicy.select(found,MusicTaste.hidden(c),seen,12,8);if(!songs.isEmpty()){combined.addAll(songs);sections.add(new DiscoveryShelf(i==0?"More of your sound":i==1?"Keep exploring":"A different direction","Songs matching “"+seeds.get(i)+"”",songs));}}catch(Exception e){if(Thread.currentThread().isInterrupted()||e instanceof java.io.InterruptedIOException)throw e;failures++;if(DownloadErrors.rateLimited(e.getMessage())){prefs.edit().putLong("musicCooldown",System.currentTimeMillis()+60000L).apply();break;}}}
  if(!before.equals(scope(c))||version!=MusicTaste.version(c))return;if(combined.isEmpty())throw new java.io.IOException("Music catalogue unavailable. Cached music is kept; try again later.");
  List<Track> mix=new ArrayList<>();int size=sections.stream().mapToInt(s->s.tracks.size()).max().orElse(0);for(int n=0;n<size;n++)for(DiscoveryShelf shelf:sections)if(n<shelf.tracks.size())mix.add(shelf.tracks.get(n));mix=RecommendationPolicy.select(mix,MusicTaste.hidden(c),new HashSet<>(),12,4);List<DiscoveryShelf> output=new ArrayList<>();output.add(new DiscoveryShelf("Your daily mix","A blend of your artists, likes and recent listening",mix));output.addAll(sections);save(c,before,version,output,failures);MusicTaste.remember(c,combined);prefs.edit().putLong("musicRotation",prefs.getLong("musicRotation",0)+1).apply();
 }
 static void save(Context c,String scope,long version,List<DiscoveryShelf> shelves,int failed)throws JSONException{JSONArray array=new JSONArray();for(DiscoveryShelf s:shelves.subList(0,Math.min(4,shelves.size())))array.put(new JSONObject().put("title",s.title).put("reason",s.reason).put("tracks",TrackMetadata.encode(s.tracks)));c.getSharedPreferences("music",0).edit().putInt("discoverySchema",4).putString("musicScope",scope).putLong("musicProfileVersion",version).putString("musicShelves",array.toString()).putLong("musicTime",System.currentTimeMillis()).putInt("musicHistorySize",MusicTaste.recent(c).size()).putInt("musicPartialFailures",failed).remove("feedTracks").remove("recommendations").apply();}
 static void clear(Context c){c.getSharedPreferences("music",0).edit().remove("musicScope").remove("musicShelves").remove("musicTime").remove("feedTracks").remove("feedScope").remove("recommendations").apply();}
}
