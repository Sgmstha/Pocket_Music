package com.pocketmusic.android;
import java.net.URI;
import java.util.Set;
public final class VideoLinks {
 private static final Set<String> HOSTS=Set.of("youtube.com","www.youtube.com","m.youtube.com","music.youtube.com");
 public static String id(String input) {
  try {
   String text=input.trim(); if(text.matches("[A-Za-z0-9_-]{11}")) return text;
   URI u=new URI(text); if(!"https".equalsIgnoreCase(u.getScheme()) || u.getUserInfo()!=null || (u.getPort()!=-1 && u.getPort()!=443)) return null;
   String found=null;
   if("youtu.be".equalsIgnoreCase(u.getHost())) found=u.getPath().substring(1);
   else if(HOSTS.contains(u.getHost())) {
    if("/watch".equals(u.getPath()) && u.getQuery()!=null) for(String p:u.getQuery().split("&")) if(p.startsWith("v=")) found=p.substring(2);
    if(u.getPath().matches("/(shorts|live)/[A-Za-z0-9_-]{11}/?")) found=u.getPath().split("/")[2];
   }
   return found!=null && found.matches("[A-Za-z0-9_-]{11}")?found:null;
  } catch(Exception e) { return null; }
 }
 public static String url(String id) { if(id==null || !id.matches("[A-Za-z0-9_-]{11}")) throw new IllegalArgumentException("Choose a YouTube video."); return "https://www.youtube.com/watch?v="+id; }
}
