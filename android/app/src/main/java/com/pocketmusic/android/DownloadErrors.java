package com.pocketmusic.android;

import java.util.Locale;

final class DownloadErrors {
 static boolean rateLimited(String message){String m=message==null?"":message.toLowerCase(Locale.ROOT);return m.contains("429")||m.contains("too many requests");}
 static boolean needsLogin(String message){String m=message==null?"":message.toLowerCase(Locale.ROOT);return m.contains("sign in")||m.contains("sign-in")||m.contains("not a bot");}
 static String redact(String message){if(message==null)return "Download failed.";return message.replaceAll("(?im)^.*(?:cookie:|set-cookie:|authorization:|SAPISID=|__Secure-[13]PSID=).*$","[Session details hidden]");}
 static boolean recoverable(String message) {
  if(rateLimited(message)||needsLogin(message))return false;
  String m=message==null?"":message.toLowerCase(Locale.ROOT);
  return m.contains("403") || m.contains("signature") || m.contains("nsig") || m.contains("requested format") || m.contains("challenge");
 }
 static String friendly(String message) {
  String m=message==null?"":message.toLowerCase(Locale.ROOT);
  if(rateLimited(message))return "YouTube is limiting requests. Downloads pause for at least a minute. Wait before retrying; sign-in may not remove this limit.";
  if(needsLogin(message))return "YouTube requests sign-in. Open ⋯ → YouTube account to connect or reconnect, then retry.";
  if(m.contains("403"))return "YouTube refused this download. Try another track or network, then retry.";
  if(m.contains("private") || m.contains("unavailable") || m.contains("sign in"))return "This video is private, unavailable, or requires sign-in. Try another result.";
  if(m.contains("timed out") || m.contains("resolve") || m.contains("network") || m.contains("connection"))return "Could not connect. Check your internet connection and retry.";
  if(m.contains("space"))return "Your phone is low on storage. Free some space and retry.";
  return "Could not save this track. Retry, or open Details for the download log.";
 }
}
