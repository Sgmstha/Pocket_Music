package com.pocketmusic.android;
import java.net.URI;
final class StreamUrlPolicy {
 static boolean allowed(String value){if(value==null||value.length()>32768)return false;try{URI uri=new URI(value);String host=uri.getHost();return "https".equalsIgnoreCase(uri.getScheme())&&host!=null&&(host.equalsIgnoreCase("googlevideo.com")||host.toLowerCase(java.util.Locale.ROOT).endsWith(".googlevideo.com"))&&uri.getUserInfo()==null&&(uri.getPort()==-1||uri.getPort()==443);}catch(Exception e){return false;}}
}
