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
  JSONArray entries=new JSONObject(json).optJSONArray("entries"); List<Track> tracks=new ArrayList<>();
  if(entries!=null)for(int i=0;i<entries.length();i++){JSONObject e=entries.optJSONObject(i);if(e!=null && e.optString("id").matches("[A-Za-z0-9_-]{11}"))tracks.add(new Track(e.getString("id"),e.optString("title","Untitled video")));}return tracks;
 }
 public static synchronized YoutubeDLRequest download(Context c,String id,String format) throws Exception {
  init(c); FFmpeg.getInstance().init(c.getApplicationContext());
  YoutubeDLRequest r=new YoutubeDLRequest(VideoLinks.url(id));
  r.addOption("--remote-components","ejs:github").addOption("--no-playlist").addOption("--no-overwrites").addOption("--socket-timeout","20").addOption("--retries","2").addOption("--concurrent-fragments","1").addOption("--newline").addOption("--no-mtime").addOption("--trim-filenames","140");
  if("mp4".equals(format))r.addOption("-f","bv[ext=mp4][height<=720]+ba[ext=m4a]/b[ext=mp4]").addOption("--merge-output-format","mp4");
  else if("mp3".equals(format)) r.addOption("-f","bestaudio/best").addOption("-x").addOption("--audio-format","mp3").addOption("--audio-quality","5");
  else throw new IllegalArgumentException("Choose MP3 or MP4.");
  r.addOption("-o",new File(folder(c),"%(title)s [%(id)s].%(ext)s").getAbsolutePath());return r;
 }
}
