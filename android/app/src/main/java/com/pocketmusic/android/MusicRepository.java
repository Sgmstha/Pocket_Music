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
 public static synchronized void init(Context c) throws Exception { YoutubeDL.getInstance().init(c.getApplicationContext()); }
 public static File folder(Context c) { File f=c.getExternalFilesDir(Environment.DIRECTORY_MUSIC); if(f==null) f=new File(c.getFilesDir(),"Music"); f.mkdirs();return f; }
 public static List<File> files(Context c) {
  File[] list=folder(c).listFiles(f->f.isFile() && f.getName().matches("(?i).+\\.(mp3|m4a|opus|flac|wav|mp4)$"));
  List<File> result=new ArrayList<>(list==null?List.of():Arrays.asList(list)); result.sort(Comparator.comparingLong(File::lastModified).reversed());return result;
 }
 public static File local(Context c,String id) { for(File f:files(c)) {String n=f.getName();if(!n.endsWith(".mp4") && n.substring(0,n.lastIndexOf('.')).endsWith("["+id+"]")) return f;} return null; }
 public static List<Track> search(Context c,String query) throws Exception {
  if(query.isBlank() || query.length()>200) throw new IllegalArgumentException("Enter a song, artist, or YouTube link (up to 200 characters).");
  String id=VideoLinks.id(query); if(id!=null)return List.of(new Track(id,"YouTube video · "+id));
  if(query.contains("://")) throw new IllegalArgumentException("Enter a valid HTTPS YouTube video link.");
  init(c); YoutubeDLRequest r=new YoutubeDLRequest("ytsearch10:"+query);
  r.addOption("--flat-playlist").addOption("--skip-download").addOption("--dump-single-json").addOption("--socket-timeout","15").addOption("--retries","1").addOption("--extractor-retries","1");
  String json=YoutubeDL.getInstance().execute(r,"search",false,null).getOut();
  JSONArray entries=new JSONObject(json).optJSONArray("entries"); List<Track> tracks=new ArrayList<>();
  if(entries!=null)for(int i=0;i<entries.length();i++){JSONObject e=entries.optJSONObject(i);if(e!=null && e.optString("id").matches("[A-Za-z0-9_-]{11}"))tracks.add(new Track(e.getString("id"),e.optString("title","Untitled video")));}return tracks;
 }
 public static YoutubeDLRequest download(Context c,String id,String format) throws Exception {
  init(c); FFmpeg.getInstance().init(c.getApplicationContext());
  YoutubeDLRequest r=new YoutubeDLRequest(VideoLinks.url(id));
  r.addOption("--no-playlist").addOption("--no-overwrites").addOption("--socket-timeout","20").addOption("--retries","2").addOption("--concurrent-fragments","1").addOption("--newline").addOption("--no-mtime").addOption("--trim-filenames","140");
  if("mp4".equals(format))r.addOption("-f","bv[ext=mp4][height<=720]+ba[ext=m4a]/b[ext=mp4]").addOption("--merge-output-format","mp4");
  else if("mp3".equals(format)) r.addOption("-f","bestaudio/best").addOption("-x").addOption("--audio-format","mp3").addOption("--audio-quality","5");
  else throw new IllegalArgumentException("Choose MP3 or MP4.");
  r.addOption("-o",new File(folder(c),"%(title)s [%(id)s].%(ext)s").getAbsolutePath());return r;
 }
}
