package com.pocketmusic.android;
import java.util.*;

/** Small in-memory cache: no requests or credentials, explicit bounded lifetime. */
final class SearchResultCache {
 private static final int LIMIT=8;private static final long TTL=30*60*1000L;
 private static final class Entry {final List<Track> tracks;final long time;Entry(List<Track> t,long n){tracks=new ArrayList<>(t);time=n;}}
 private final LinkedHashMap<String,Entry> entries=new LinkedHashMap<>(16,.75f,true);
 static String key(String query){return query.trim().replaceAll("\\s+"," ").toLowerCase(Locale.ROOT);}
 synchronized List<Track> get(String query,long now){String key=key(query);Entry e=entries.get(key);if(e==null)return null;if(now<e.time||now-e.time>=TTL){entries.remove(key);return null;}return new ArrayList<>(e.tracks);}
 synchronized void put(String query,List<Track> tracks,long now){if(tracks.isEmpty())return;entries.put(key(query),new Entry(tracks.subList(0,Math.min(20,tracks.size())),now));while(entries.size()>LIMIT)entries.remove(entries.keySet().iterator().next());}
 synchronized void clear(){entries.clear();}
}
