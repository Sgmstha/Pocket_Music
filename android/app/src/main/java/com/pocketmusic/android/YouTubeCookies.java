package com.pocketmusic.android;

import java.util.LinkedHashMap;
import java.util.Map;

/** Converts only cookies obtained for YouTube into yt-dlp's cookie-file format. */
final class YouTubeCookies {
 static String netscape(String header) {
  if(header==null || header.length()>65536)throw new IllegalArgumentException("No usable YouTube session.");
  Map<String,String> values=new LinkedHashMap<>();
  for(String pair:header.split(";")){int at=pair.indexOf('=');if(at<1)continue;String name=pair.substring(0,at).trim(),value=pair.substring(at+1).trim();
   if(!name.matches("[A-Za-z0-9_\\-]+") || value.matches("(?s).*[\\x00-\\x20\\x7f].*"))continue;
   values.put(name,value);
  }
  boolean auth=false;for(String name:new String[]{"SAPISID","__Secure-1PSID","__Secure-3PSID"})if(!values.getOrDefault(name,"").isEmpty())auth=true;
  if(!auth)throw new IllegalArgumentException("Finish signing in to YouTube, then try again.");
  StringBuilder out=new StringBuilder("# Netscape HTTP Cookie File\n");
  for(Map.Entry<String,String> item:values.entrySet())out.append(".youtube.com\tTRUE\t/\tTRUE\t0\t").append(item.getKey()).append('\t').append(item.getValue()).append('\n');
  return out.toString();
 }
}
