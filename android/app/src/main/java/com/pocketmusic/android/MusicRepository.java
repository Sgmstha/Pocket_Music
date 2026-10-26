package com.pocketmusic.android;
import android.content.Context;
import android.os.Environment;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.ffmpeg.FFmpeg;
import org.json.*;
import java.io.File;
import java.util.*;
public final class MusicRepository {
 public static synchronized void init(Context c) throws Exception {
  YoutubeDL.getInstance().init(c.getApplicationContext());
  if(c.getSharedPreferences("music",0).getInt("bundledEngine",0)<2){
   File destination=new File(new File(c.getNoBackupFilesDir(),YoutubeDL.baseName),YoutubeDL.ytdlpDirName);
   File temporary=new File(destination,"engine.tmp");
   try(java.io.InputStream in=c.getResources().openRawResource(R.raw.ytdlp);java.io.OutputStream out=new java.io.FileOutputStream(temporary)){byte[] buffer=new byte[32768];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);}
   if(!temporary.renameTo(new File(destination,YoutubeDL.ytdlpBin)))throw new java.io.IOException("Cannot install bundled download engine");
   c.getSharedPreferences("music",0).edit().putInt("bundledEngine",2).apply();
  }
 }
 public static synchronized void updateEngine(Context c, boolean force) throws Exception {
  init(c);
  long last=c.getSharedPreferences("music",0).getLong("engineChecked",0);
  if(!force && System.currentTimeMillis()-last<86400000L)return;
  YoutubeDL.getInstance().updateYoutubeDL(c.getApplicationContext(),YoutubeDL.UpdateChannel._NIGHTLY);
  c.getSharedPreferences("music",0).edit().putLong("engineChecked",System.currentTimeMillis()).apply();
 }
 public static File folder(Context c) { File f=c.getExternalFilesDir(Environment.DIRECTORY_MUSIC); if(f==null) f=new File(c.getFilesDir(),"Music"); f.mkdirs();return f; }
 public static List<File> files(Context c) {
  File[] list=folder(c).listFiles(f->f.isFile() && f.getName().matches("(?i).+\\.(mp3|m4a|opus|flac|wav|mp4)$"));
  List<File> result=new ArrayList<>(list==null?List.of():Arrays.asList(list)); result.sort(Comparator.comparingLong(File::lastModified).reversed());return result;
 }
 public static File local(Context c,String id) { for(File f:files(c)) {String n=f.getName();if(!n.endsWith(".mp4") && n.substring(0,n.lastIndexOf('.')).endsWith("["+id+"]")) return f;} return null; }
 public static File downloaded(Context c,String id,String format){for(File f:files(c)){String n=f.getName();int dot=n.lastIndexOf('.');if(dot>0&&n.substring(0,dot).endsWith("["+id+"]")&&n.substring(dot+1).equalsIgnoreCase(format))return f;}return null;}
 public static List<Track> search(Context c,String query) throws Exception {return search(c,query,"search");}
 public static List<Track> search(Context c,String query,String process) throws Exception {
  if(query.isBlank() || query.length()>200) throw new IllegalArgumentException("Enter a song, artist, or YouTube link (up to 200 characters).");
  String id=VideoLinks.id(query); if(id!=null)return List.of(new Track(id,"YouTube video · "+id));
  if(query.contains("://")) throw new IllegalArgumentException("Enter a valid HTTPS YouTube video link.");
  init(c); YoutubeDLRequest r=new YoutubeDLRequest("ytsearch10:"+query);
  r.addOption("--flat-playlist").addOption("--skip-download").addOption("--dump-single-json").addOption("--socket-timeout","15").addOption("--retries","1").addOption("--extractor-retries","1");
  String json=YoutubeDL.getInstance().execute(r,process,false,null).getOut();
  return TrackMetadata.parse(json,10);
 }
 static List<Track> accountRecommendations(Context c,String process)throws Exception{
  init(c);if(System.currentTimeMillis()<c.getSharedPreferences("music",0).getLong("accountFeedCooldown",0))throw new java.io.IOException("HTTP 429: Wait before refreshing your account feed.");
  File cookies=YouTubeSession.lease(c);if(cookies==null)throw new java.io.IOException("Sign in to connect your YouTube account.");
  try{YoutubeDLRequest r=new YoutubeDLRequest(":ytrec");r.addOption("--cookies",cookies.getAbsolutePath()).addOption("--flat-playlist").addOption("--skip-download").addOption("--dump-single-json").addOption("--playlist-end","12").addOption("--socket-timeout","15").addOption("--retries","0").addOption("--extractor-retries","0").addOption("--sleep-requests","1");return TrackMetadata.parse(YoutubeDL.getInstance().execute(r,process,false,null).getOut(),12);}
  catch(Exception e){if(DownloadErrors.rateLimited(e.getMessage()))c.getSharedPreferences("music",0).edit().putLong("accountFeedCooldown",System.currentTimeMillis()+60000L).apply();throw e;}
  finally{cookies.delete();}
 }
 public static synchronized YoutubeDLRequest download(Context c,String id,String format) throws Exception {
  init(c); FFmpeg.getInstance().init(c.getApplicationContext());
  YoutubeDLRequest r=new YoutubeDLRequest(VideoLinks.url(id));
  r.addOption("--remote-components","ejs:github").addOption("--no-playlist").addOption("--no-overwrites").addOption("--socket-timeout","20").addOption("--retries","1").addOption("--extractor-retries","0").addOption("--sleep-requests","1").addOption("--concurrent-fragments","1").addOption("--newline").addOption("--no-mtime").addOption("--trim-filenames","140");
  if("mp4".equals(format))r.addOption("-f","bv[ext=mp4][height<=720]+ba[ext=m4a]/b[ext=mp4]").addOption("--merge-output-format","mp4");
  else if("mp3".equals(format)) r.addOption("-f","bestaudio/best").addOption("-x").addOption("--audio-format","mp3").addOption("--audio-quality","5");
  else throw new IllegalArgumentException("Choose MP3 or MP4.");
  r.addOption("-o",new File(folder(c),"%(title)s [%(id)s].%(ext)s").getAbsolutePath());return r;
 }
}
