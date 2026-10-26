package com.pocketmusic.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.DocumentsContract;
import java.io.*;
import org.json.JSONArray;
import java.util.*;

/** Deletes only a validated library file and, when explicitly chosen, its tracked SAF copy. */
final class LocalDownloads {
 static synchronized void remove(Context c,File file,boolean deleteSavedCopy)throws Exception{
  if(file==null||!file.isFile()||!file.getCanonicalFile().getParentFile().equals(MusicRepository.folder(c).getCanonicalFile())||!file.getName().matches("(?i).+\\.(mp3|m4a|opus|flac|wav|mp4)$"))throw new IOException("This download is no longer available.");
  for(DownloadQueue.Job job:DownloadService.jobs(c))if(job.pending()&&job.id.equals(TrackFiles.videoId(file)))throw new IOException("Wait for this track’s download to finish or cancel it first.");
  Uri saved=DownloadFolder.savedUri(c,file);
  if(deleteSavedCopy&&saved!=null){if(!DownloadFolder.hasAccess(c))throw new IOException("Your saved folder is unavailable. Reconnect it in Settings, or remove only the app copy.");if(!DocumentsContract.deleteDocument(c.getContentResolver(),saved))throw new IOException("The saved copy could not be deleted. The app copy was kept.");}
  if(!file.delete())throw new IOException("The app copy could not be removed. Please try again.");
  SharedPreferences p=c.getSharedPreferences("music",0);SharedPreferences.Editor edit=p.edit();for(String key:new String[]{"recentTracks","tasteLocalLikes"}){List<String> values=new ArrayList<>();try{JSONArray array=new JSONArray(p.getString(key,"[]"));for(int i=0;i<array.length();i++){String value=array.optString(i);if(!value.equals(file.getAbsolutePath()))values.add(value);}}catch(Exception ignored){}edit.putString(key,new JSONArray(values).toString());}if(p.getString("lastPlayed","").equals(file.getAbsolutePath()))edit.remove("lastPlayed");edit.putLong("tasteLocalVersion",System.nanoTime()).apply();
 }
}
