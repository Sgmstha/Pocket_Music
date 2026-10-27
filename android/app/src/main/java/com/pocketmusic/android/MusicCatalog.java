package com.pocketmusic.android;

import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.downloader.*;
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.GZIPInputStream;

/** Public Songs catalogue and in-process audio resolution; no account cookies. */
final class MusicCatalog {
 private static boolean initialized;
 private static final ThreadLocal<Long> DEADLINE=new ThreadLocal<>();
 private static final ThreadLocal<String> SCOPE=new ThreadLocal<>();
 private static final Map<String,Set<HttpURLConnection>> ACTIVE=new ConcurrentHashMap<>();
 private static synchronized void init(){if(!initialized){NewPipe.init(new CatalogDownloader());initialized=true;}}
 static void cancel(String process){for(Map.Entry<String,Set<HttpURLConnection>> entry:ACTIVE.entrySet())if(entry.getKey().equals(process)||entry.getKey().startsWith(process+"-"))for(HttpURLConnection c:entry.getValue())c.disconnect();}
 static OnlineAudio.Source audio(String id,String process)throws Exception{
  checkInterrupted();DEADLINE.set(android.os.SystemClock.elapsedRealtime()+8000);SCOPE.set(process);ACTIVE.put(process,ConcurrentHashMap.newKeySet());
  try{init();org.schabi.newpipe.extractor.stream.StreamExtractor extractor=ServiceList.YouTube.getStreamExtractor(VideoLinks.url(id));extractor.fetchPage();
   org.schabi.newpipe.extractor.stream.AudioStream best=null;
   for(org.schabi.newpipe.extractor.stream.AudioStream stream:extractor.getAudioStreams()){
    checkInterrupted();if(!stream.isUrl()||stream.getDeliveryMethod()!=org.schabi.newpipe.extractor.stream.DeliveryMethod.PROGRESSIVE_HTTP||!StreamUrlPolicy.allowed(stream.getContent()))continue;
    if(best==null||Math.abs(stream.getAverageBitrate()-128)<Math.abs(best.getAverageBitrate()-128))best=stream;
   }
   if(best==null)throw new IOException("No direct audio stream.");
   return new OnlineAudio.Source(best.getContent(),Map.of("User-Agent","Mozilla/5.0","Referer","https://www.youtube.com/"),true);
  }finally{ACTIVE.remove(process);SCOPE.remove();DEADLINE.remove();}
 }
 static List<Track> songs(String query,String process)throws Exception{
  if(query==null||query.isBlank()||query.length()>200||query.contains("://"))throw new IllegalArgumentException("Enter a song or artist (up to 200 characters).");
  checkInterrupted();SCOPE.set(process);ACTIVE.put(process,ConcurrentHashMap.newKeySet());
  try{init();checkInterrupted();SearchExtractor extractor=ServiceList.YouTube.getSearchExtractor(query,List.of(YoutubeSearchQueryHandlerFactory.MUSIC_SONGS),"");extractor.fetchPage();List<Track> tracks=new ArrayList<>();Set<String> ids=new HashSet<>();
   for(InfoItem item:extractor.getInitialPage().getItems()){checkInterrupted();if(!(item instanceof StreamInfoItem))continue;StreamInfoItem song=(StreamInfoItem)item;String id=VideoLinks.id(song.getUrl());if(id==null||!ids.add(id)||song.getDuration()<=0)continue;String image="";for(Image art:song.getThumbnails())if(Artwork.safeUrl(art.getUrl()))image=art.getUrl();tracks.add(new Track(id,song.getName(),song.getUploaderName(),song.getDuration(),true,image));if(tracks.size()>=20)break;}return tracks;
  }catch(SearchExtractor.NothingFoundException empty){return List.of();}finally{ACTIVE.remove(process);SCOPE.remove();}
 }
 private static void checkInterrupted()throws InterruptedIOException{if(DEADLINE.get()!=null&&android.os.SystemClock.elapsedRealtime()>=DEADLINE.get())throw new InterruptedIOException("Stream lookup timed out.");if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Search cancelled.");}
 private static boolean allowed(URL url){String host=url.getHost();return url.getProtocol().equals("https")&&(host.equals("youtube.com")||host.endsWith(".youtube.com")||host.equals("youtubei.googleapis.com")||host.equals("www.google.com"));}
 private static final class CatalogDownloader extends Downloader {
  @Override public Response execute(Request request)throws IOException{
   URL url=new URL(request.url());
   for(int redirects=0;redirects<4;redirects++){checkInterrupted();if(!allowed(url))throw new IOException("Unsupported music catalogue host.");HttpURLConnection connection=(HttpURLConnection)url.openConnection();String scope=SCOPE.get();Set<HttpURLConnection> running=scope==null?null:ACTIVE.get(scope);if(running!=null)running.add(connection);
    try{checkInterrupted();int timeout=DEADLINE.get()==null?12000:(int)Math.max(1,Math.min(4000,DEADLINE.get()-android.os.SystemClock.elapsedRealtime()));connection.setConnectTimeout(timeout);connection.setReadTimeout(timeout);connection.setInstanceFollowRedirects(false);connection.setRequestMethod(request.httpMethod());for(Map.Entry<String,List<String>> h:request.headers().entrySet())for(String value:h.getValue())connection.addRequestProperty(h.getKey(),value);connection.setRequestProperty("Accept-Encoding","gzip");byte[] body=request.dataToSend();if(body!=null&&body.length>0){if(body.length>1048576)throw new IOException("Catalogue request too large.");connection.setDoOutput(true);try(OutputStream output=connection.getOutputStream()){output.write(body);}}
     int code=connection.getResponseCode();if(code==429)throw new IOException("HTTP 429: Music catalogue is limiting requests. Try later.");if(code==301||code==302||code==303||code==307||code==308){String location=connection.getHeaderField("Location");if(location==null)throw new IOException("Invalid catalogue redirect.");url=new URL(url,location);continue;}
     InputStream raw=code>=400?connection.getErrorStream():connection.getInputStream();String response="";if(raw!=null)try(InputStream input="gzip".equalsIgnoreCase(connection.getContentEncoding())?new GZIPInputStream(raw):raw;ByteArrayOutputStream output=new ByteArrayOutputStream()){byte[] buffer=new byte[16384];int n;while((n=input.read(buffer))!=-1){checkInterrupted();if(output.size()+n>8*1024*1024)throw new IOException("Catalogue response too large.");output.write(buffer,0,n);}response=output.toString(StandardCharsets.UTF_8.name());}return new Response(code,connection.getResponseMessage(),connection.getHeaderFields(),response,url.toString());
    }finally{if(running!=null)running.remove(connection);connection.disconnect();}
   }throw new IOException("Too many music catalogue redirects.");
  }
 }
}
