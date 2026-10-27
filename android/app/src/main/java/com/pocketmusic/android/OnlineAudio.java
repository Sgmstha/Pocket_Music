package com.pocketmusic.android;
import android.content.Context;
import com.yausername.youtubedl_android.*;
import java.io.*;
import java.util.*;
import org.json.*;

/** Resolve an authorized audio stream in memory; never download or persist its signed URL. */
final class OnlineAudio {
 interface Resolver {Source resolve(Context context,Track track,String process)throws Exception;}
 static final class Source {final String url;final Map<String,String> headers;final boolean nativeSource;Source(String url,Map<String,String> headers){this(url,headers,false);}Source(String url,Map<String,String> headers,boolean nativeSource){this.url=url;this.headers=Collections.unmodifiableMap(new HashMap<>(headers));this.nativeSource=nativeSource;}}
 private record Cached(Source source,long until){}
 private static final Map<String,Cached> CACHE=new LinkedHashMap<>();
 private static String scope(Context c,String id){android.net.ConnectivityManager manager=c.getSystemService(android.net.ConnectivityManager.class);return YouTubeSession.fingerprint(c)+":"+(manager==null?"":manager.getActiveNetwork())+":"+id;}
 static synchronized void forget(Context c,String id){CACHE.remove(scope(c,id));}
 private static synchronized Source cached(Context c,String id){Cached item=CACHE.get(scope(c,id));return item!=null&&System.currentTimeMillis()<item.until?item.source:null;}
 private static synchronized Source remember(Context c,String id,Source source){long until=System.currentTimeMillis()+300000;try{String expiry=android.net.Uri.parse(source.url).getQueryParameter("expire");if(expiry!=null)until=Math.min(until,Long.parseLong(expiry)*1000-60000);}catch(Exception ignored){}CACHE.put(scope(c,id),new Cached(source,until));while(CACHE.size()>8)CACHE.remove(CACHE.keySet().iterator().next());return source;}
 static Source parse(String json)throws Exception{
  JSONObject info=new JSONObject(json);String url=info.optString("url","");
  if(!StreamUrlPolicy.allowed(url))throw new IOException("No supported audio stream is available.");
  if(!"none".equals(info.optString("vcodec","none")))throw new IOException("An audio-only stream is unavailable.");
  Map<String,String> headers=new HashMap<>();JSONObject raw=info.optJSONObject("http_headers");if(raw!=null)for(String key:List.of("User-Agent","Referer","Origin","Accept","Accept-Language")){String value=raw.optString(key,"");if(!value.isEmpty()&&value.length()<=4096&&!value.contains("\r")&&!value.contains("\n"))headers.put(key,value);}
  return new Source(url,headers);
 }
 static Source resolve(Context context,Track track,String process)throws Exception{
  if(track==null||track.id==null||!track.id.matches("[A-Za-z0-9_-]{11}"))throw new IOException("Choose a valid song.");
  if(System.currentTimeMillis()<context.getSharedPreferences("music",0).getLong("youtubeCooldown",0))throw new IOException("HTTP 429: Wait before retrying.");
  Source hit=cached(context,track.id);if(hit!=null)return hit;
  try{return remember(context,track.id,MusicCatalog.audio(track.id,process+"-native"));}catch(Exception fastFailure){if(Thread.currentThread().isInterrupted())throw fastFailure;if(DownloadErrors.rateLimited(fastFailure.getMessage())){context.getSharedPreferences("music",0).edit().putLong("youtubeCooldown",System.currentTimeMillis()+60000L).apply();throw fastFailure;}}
  return resolveLegacy(context,track,process);
 }
 static Source resolveLegacy(Context context,Track track,String process)throws Exception{
  MusicRepository.init(context);File cookies=null;
  try{cookies=YouTubeSession.lease(context);YoutubeDLRequest request=new YoutubeDLRequest(VideoLinks.url(track.id));request.addOption("--no-playlist").addOption("--skip-download").addOption("--dump-single-json").addOption("--no-warnings").addOption("--socket-timeout","15").addOption("--retries","0").addOption("--extractor-retries","0").addOption("--remote-components","ejs:github").addOption("-f","bestaudio[ext=m4a][protocol=https]/bestaudio[protocol=https]");if(cookies!=null)request.addOption("--cookies",cookies.getAbsolutePath());if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Cancelled");String result=YoutubeDL.getInstance().execute(request,process,false,null).getOut();if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Cancelled");return remember(context,track.id,parse(result));}
  catch(Exception e){if(DownloadErrors.rateLimited(e.getMessage()))context.getSharedPreferences("music",0).edit().putLong("youtubeCooldown",System.currentTimeMillis()+60000L).apply();throw e;}
  finally{if(cookies!=null)cookies.delete();}
 }
 static String friendly(Throwable error){String message=error.getMessage();if(DownloadErrors.rateLimited(message))return "YouTube is limiting requests. Wait before playing again.";if(DownloadErrors.needsLogin(message))return "YouTube requests sign-in. Connect or reconnect in YouTube account, then tap the song again.";if(message!=null&&message.contains("403"))return "YouTube refused this audio stream. Reconnect your YouTube account, try later, or use Watch YouTube preview.";return "Online audio unavailable. Check your connection, tap the song to retry, or use Watch YouTube preview.";}
 static void cancel(String process){MusicCatalog.cancel(process);try{YoutubeDL.getInstance().destroyProcessById(process);}catch(Exception ignored){}}
}
