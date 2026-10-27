package com.pocketmusic.android;
import android.content.Context;
import com.yausername.youtubedl_android.*;
import java.io.*;
import java.util.*;
import org.json.*;

/** Resolve an authorized audio stream in memory; never download or persist its signed URL. */
final class OnlineAudio {
 interface Resolver {Source resolve(Context context,Track track,String process)throws Exception;}
 static final class Source {final String url;final Map<String,String> headers;Source(String url,Map<String,String> headers){this.url=url;this.headers=Collections.unmodifiableMap(new HashMap<>(headers));}}
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
  MusicRepository.init(context);File cookies=null;
  try{cookies=YouTubeSession.lease(context);YoutubeDLRequest request=new YoutubeDLRequest(VideoLinks.url(track.id));request.addOption("--no-playlist").addOption("--skip-download").addOption("--dump-single-json").addOption("--no-warnings").addOption("--socket-timeout","15").addOption("--retries","0").addOption("--extractor-retries","0").addOption("--remote-components","ejs:github").addOption("-f","bestaudio[ext=m4a][protocol=https]/bestaudio[protocol=https]");if(cookies!=null)request.addOption("--cookies",cookies.getAbsolutePath());if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Cancelled");String result;try{result=YoutubeDL.getInstance().execute(request,process,false,null).getOut();}catch(Exception first){if(Thread.currentThread().isInterrupted()||!DownloadErrors.recoverable(first.getMessage()))throw first;MusicRepository.updateEngine(context,true);if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Cancelled");result=YoutubeDL.getInstance().execute(request,process,false,null).getOut();}if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Cancelled");return parse(result);}
  catch(Exception e){if(DownloadErrors.rateLimited(e.getMessage()))context.getSharedPreferences("music",0).edit().putLong("youtubeCooldown",System.currentTimeMillis()+60000L).apply();throw e;}
  finally{if(cookies!=null)cookies.delete();}
 }
 static String friendly(Throwable error){String message=error.getMessage();if(DownloadErrors.rateLimited(message))return "YouTube is limiting requests. Wait before playing again.";if(DownloadErrors.needsLogin(message))return "YouTube requests sign-in. Connect or reconnect in YouTube account, then tap the song again.";if(message!=null&&message.contains("403"))return "YouTube refused this audio stream. Reconnect your YouTube account, try later, or use Watch YouTube preview.";return "Online audio unavailable. Check your connection, tap the song to retry, or use Watch YouTube preview.";}
 static void cancel(String process){try{YoutubeDL.getInstance().destroyProcessById(process);}catch(Exception ignored){}}
}
