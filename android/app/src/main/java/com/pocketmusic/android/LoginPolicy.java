package com.pocketmusic.android;
import java.net.URI;
import java.util.*;
/** Fixed HTTPS destinations for the visible account handoff; no arbitrary browser redirects. */
final class LoginPolicy {
 static boolean allowed(String url){try{URI uri=new URI(url);String host=uri.getHost(),path=uri.getPath();if(!"https".equalsIgnoreCase(uri.getScheme())||host==null||uri.getRawUserInfo()!=null||(uri.getPort()!=-1&&uri.getPort()!=443))return false;host=host.toLowerCase(Locale.ROOT);return host.equals("youtube.com")||host.endsWith(".youtube.com")||host.equals("accounts.google.com")||host.equals("consent.google.com")||host.equals("myaccount.google.com")||host.equals("www.google.com")&&path!=null&&path.startsWith("/accounts/");}catch(Exception error){return false;}}
 static boolean ready(String header){try{String exported=YouTubeCookies.netscape(header);for(String line:exported.split("\n")){String[] fields=line.split("\t",-1);if(fields.length==7&&!fields[6].isEmpty()&&List.of("SAPISID","__Secure-3PAPISID","__Secure-1PAPISID").contains(fields[5]))return true;}return false;}catch(Exception error){return false;}}
}
