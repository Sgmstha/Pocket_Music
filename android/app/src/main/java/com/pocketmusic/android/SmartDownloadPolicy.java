package com.pocketmusic.android;
import java.util.*;

/** Deterministic interleave of personal signals; never select general videos or long mixes. */
final class SmartDownloadPolicy {
 static List<Track> select(List<Track> recent,List<Track> likes,List<Track> recommendations,Set<String> excluded,Set<String> manual,int limit){
  List<Track> out=new ArrayList<>();Set<String> seen=new HashSet<>();List<List<Track>> lists=List.of(recent,likes,recommendations);
  for(int i=0;i<200&&out.size()<limit;i++)for(List<Track> list:lists)if(i<list.size()){
   Track t=list.get(i);if(t.music&&t.id.matches("[A-Za-z0-9_-]{11}")&&t.durationSeconds>=30&&t.durationSeconds<=600&&!excluded.contains(t.id)&&!manual.contains(t.id)&&seen.add(t.id)){out.add(t);if(out.size()==limit)break;}
  }return out;
 }
}
