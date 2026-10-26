package com.pocketmusic.android;
import org.json.*;
import java.util.*;

final class TrackMetadata {
 static List<Track> parse(String json,int limit)throws JSONException{JSONObject object=new JSONObject(json);JSONArray entries=object.optJSONArray("entries");if(entries==null)entries=new JSONArray().put(object);List<Track> tracks=new ArrayList<>();Set<String> seen=new HashSet<>();
  for(int i=0;i<entries.length()&&tracks.size()<limit;i++){JSONObject e=entries.optJSONObject(i);if(e==null)continue;String id=e.optString("id"),title=e.optString("title","");String availability=e.optString("availability","");if(!id.matches("[A-Za-z0-9_-]{11}")||!seen.add(id)||title.isBlank()||title.equals("[Private video]")||title.equals("[Deleted video]")||availability.equals("private")||availability.equals("needs_auth"))continue;String artist=e.optString("artist","");if(artist.isBlank())artist=e.optString("uploader",e.optString("channel",""));tracks.add(new Track(id,title,artist,e.optLong("duration",0),e.optBoolean("music",false),e.optString("artwork","")));}return tracks;}
 static String encode(List<Track> tracks)throws JSONException{JSONArray array=new JSONArray();for(Track t:tracks.subList(0,Math.min(200,tracks.size())))array.put(new JSONObject().put("id",t.id).put("title",t.title).put("artist",t.artist).put("duration",t.durationSeconds).put("music",t.music).put("artwork",t.artworkUrl));return new JSONObject().put("entries",array).toString();}
}
