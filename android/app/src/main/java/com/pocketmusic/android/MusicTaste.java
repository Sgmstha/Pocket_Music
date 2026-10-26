package com.pocketmusic.android;
import android.content.Context;
import org.json.*;
import java.io.File;
import java.util.*;

/** Small on-device taste profile. Only catalogue songs become online music signals. */
final class MusicTaste {
 private static List<String> values(Context c,String key){List<String> values=new ArrayList<>();try{JSONArray a=new JSONArray(c.getSharedPreferences("music",0).getString(key,"[]"));for(int i=0;i<Math.min(200,a.length());i++){String v=a.optString(i);if(!v.isBlank())values.add(v);}}catch(Exception ignored){}return values;}
 static List<String> artists(Context c){return values(c,"tasteArtists");}
 static List<String> genres(Context c){List<String> g=values(c,"tasteGenres");return g.isEmpty()?List.of("Pop","Acoustic"):g;}
 static long version(Context c){return c.getSharedPreferences("music",0).getLong("tasteVersion",0);}
 static Set<String> hidden(Context c){return new HashSet<>(values(c,"tasteHidden"));}
 static List<Track> known(Context c){try{return TrackMetadata.parse(c.getSharedPreferences("music",0).getString("tasteKnown","{}"),200);}catch(Exception e){return new ArrayList<>();}}
 static Track track(Context c,String id){if(id==null)return null;for(Track t:known(c))if(t.id.equals(id))return t;return null;}
 static synchronized void remember(Context c,List<Track> tracks){LinkedHashMap<String,Track> all=new LinkedHashMap<>();List<Track> old=known(c);for(String id:values(c,"tasteLikes"))for(Track liked:old)if(liked.id.equals(id))all.putIfAbsent(id,liked);for(Track t:tracks)if(t.music)all.putIfAbsent(t.id,t);for(Track t:old)all.putIfAbsent(t.id,t);try{c.getSharedPreferences("music",0).edit().putString("tasteKnown",TrackMetadata.encode(new ArrayList<>(all.values()))).apply();}catch(Exception ignored){}}
 private static List<Track> resolve(Context c,String key){Map<String,Track> all=new HashMap<>();for(Track t:known(c))all.put(t.id,t);List<Track> tracks=new ArrayList<>();for(String id:values(c,key)){Track t=all.get(id);if(t!=null&&t.music&&!hidden(c).contains(id))tracks.add(t);}return tracks;}
 static List<Track> liked(Context c){return resolve(c,"tasteLikes");}
 static List<Track> recent(Context c){return resolve(c,"tasteHistory");}
 static boolean liked(Context c,String id){return values(c,"tasteLikes").contains(id);}
 static synchronized boolean toggleLike(Context c,Track t){if(t==null||!t.music)return false;remember(c,List.of(t));List<String> likes=values(c,"tasteLikes");boolean added=!likes.remove(t.id);if(added){likes.add(0,t.id);List<String> hidden=values(c,"tasteHidden");hidden.remove(t.id);c.getSharedPreferences("music",0).edit().putString("tasteHidden",new JSONArray(hidden).toString()).apply();}trim(likes,100);c.getSharedPreferences("music",0).edit().putString("tasteLikes",new JSONArray(likes).toString()).putLong("tasteVersion",version(c)+1).apply();return added;}
 static synchronized void hide(Context c,String id,boolean hidden){List<String> ids=values(c,"tasteHidden");ids.remove(id);if(hidden)ids.add(0,id);trim(ids,200);c.getSharedPreferences("music",0).edit().putString("tasteHidden",new JSONArray(ids).toString()).putLong("tasteVersion",version(c)+1).apply();}
 static synchronized void played(Context c,Track t){if(t==null||!t.music)return;remember(c,List.of(t));List<String> history=values(c,"tasteHistory");history.remove(t.id);history.add(0,t.id);trim(history,60);c.getSharedPreferences("music",0).edit().putString("tasteHistory",new JSONArray(history).toString()).putLong("tastePlayedAt",System.currentTimeMillis()).apply();}
 static synchronized void choose(Context c,String artistText,List<String> genres){List<String> artists=new ArrayList<>();for(String a:artistText.split(",")){String clean=RecommendationPolicy.clean(a);if(!clean.isEmpty()&&clean.length()<=80&&!clean.contains("://")&&!artists.contains(clean))artists.add(clean);if(artists.size()==5)break;}c.getSharedPreferences("music",0).edit().putString("tasteArtists",new JSONArray(artists).toString()).putString("tasteGenres",new JSONArray(genres.subList(0,Math.min(4,genres.size()))).toString()).putLong("tasteVersion",version(c)+1).apply();}
 static List<String> seeds(Context c){return RecommendationPolicy.seeds(artists(c),liked(c),recent(c),genres(c),c.getSharedPreferences("music",0).getString("recommendationSeed",""),c.getSharedPreferences("music",0).getLong("musicRotation",0));}
 static boolean localLiked(Context c,File file){String id=TrackFiles.videoId(file);return id!=null&&liked(c,id)||values(c,"tasteLocalLikes").contains(file.getAbsolutePath());}
 static synchronized boolean toggleLocal(Context c,File file){Track t=track(c,TrackFiles.videoId(file));if(t!=null)return toggleLike(c,t);List<String> paths=values(c,"tasteLocalLikes");boolean added=!paths.remove(file.getAbsolutePath());if(added)paths.add(0,file.getAbsolutePath());trim(paths,100);c.getSharedPreferences("music",0).edit().putString("tasteLocalLikes",new JSONArray(paths).toString()).putLong("tasteLocalVersion",System.nanoTime()).apply();return added;}
 private static void trim(List<?> list,int max){while(list.size()>max)list.remove(list.size()-1);}
}
