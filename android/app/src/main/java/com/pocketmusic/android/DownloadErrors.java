package com.pocketmusic.android;

import java.util.Locale;

final class DownloadErrors {
 static boolean recoverable(String message) {
  String m=message==null?"":message.toLowerCase(Locale.ROOT);
  return m.contains("403") || m.contains("signature") || m.contains("nsig") || m.contains("requested format") || m.contains("challenge");
 }
 static String friendly(String message) {
  String m=message==null?"":message.toLowerCase(Locale.ROOT);
  if(m.contains("403"))return "YouTube refused this download. Try another track or network, then retry.";
  if(m.contains("private") || m.contains("unavailable") || m.contains("sign in"))return "This video is private, unavailable, or requires sign-in. Try another result.";
  if(m.contains("timed out") || m.contains("resolve") || m.contains("network") || m.contains("connection"))return "Could not connect. Check your internet connection and retry.";
  if(m.contains("space"))return "Your phone is low on storage. Free some space and retry.";
  return "Could not save this track. Retry, or open Details for the download log.";
 }
}
