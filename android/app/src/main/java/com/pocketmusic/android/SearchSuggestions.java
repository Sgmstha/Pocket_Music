package com.pocketmusic.android;
import java.util.*;
final class SearchSuggestions {
 static List<String> build(String query,List<String> recent,List<String> titles){
  String q=query.trim();if(q.isEmpty())return List.of();LinkedHashMap<String,String> choices=new LinkedHashMap<>();String lower=q.toLowerCase(Locale.ROOT);
  for(List<String> source:List.of(recent,titles))for(String value:source)if(value.toLowerCase(Locale.ROOT).contains(lower)&&!value.equalsIgnoreCase(q))choices.putIfAbsent(value.toLowerCase(Locale.ROOT),value);
  if(!q.contains("://"))for(String suffix:new String[]{" songs"," live"," acoustic"})choices.putIfAbsent((q+suffix).toLowerCase(Locale.ROOT),q+suffix);
  List<String> result=new ArrayList<>(choices.values());return result.subList(0,Math.min(5,result.size()));
 }
}
