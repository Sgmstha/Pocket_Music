package com.pocketmusic.android;

import java.io.File;
import java.util.*;

final class LibraryOrder {
 static List<File> select(List<File> source,String media,boolean byName){
  List<File> selected=new ArrayList<>();for(File f:source)if("all".equals(media)||("audio".equals(media)==TrackFiles.audio(f)))selected.add(f);
  if(byName)selected.sort(Comparator.comparing(TrackFiles::title,String.CASE_INSENSITIVE_ORDER).thenComparing(File::getName));
  else selected.sort(Comparator.comparingLong(File::lastModified).reversed().thenComparing(File::getName));
  return selected;
 }
}
