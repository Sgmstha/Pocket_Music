package com.pocketmusic.android;

import android.content.Context;
import org.json.JSONArray;
import java.util.*;

/** Small explicit history. No database, network feed, background scan or user profiling. */
final class RecentHistory {
 private static final int LIMIT=12;
 static synchronized List<String> read(Context c,String key){
  List<String> values=new ArrayList<>();
  try{JSONArray a=new JSONArray(c.getSharedPreferences("music",0).getString(key,"[]"));for(int i=0;i<Math.min(a.length(),LIMIT);i++){String v=a.optString(i);if(!v.isEmpty())values.add(v);}}catch(Exception ignored){}
  return values;
 }
 static synchronized void touch(Context c,String key,String value){
  String clean=value.trim();if(clean.isEmpty())return;
  List<String> values=read(c,key);values.removeIf(v->key.equals("recentQueries")?v.equalsIgnoreCase(clean):v.equals(clean));values.add(0,clean);
  if(values.size()>LIMIT)values=new ArrayList<>(values.subList(0,LIMIT));c.getSharedPreferences("music",0).edit().putString(key,new JSONArray(values).toString()).apply();
 }
 static void clear(Context c,String key){c.getSharedPreferences("music",0).edit().remove(key).apply();}
}
