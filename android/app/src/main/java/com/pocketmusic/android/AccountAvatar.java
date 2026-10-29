package com.pocketmusic.android;
final class AccountAvatar {
 static boolean allowed(String url){try{if(url==null||url.length()>4096)return false;java.net.URI uri=new java.net.URI(url);String host=uri.getHost();return "https".equals(uri.getScheme())&&uri.getRawUserInfo()==null&&(uri.getPort()==-1||uri.getPort()==443)&&host!=null&&(host.equals("yt3.ggpht.com")||host.endsWith(".googleusercontent.com"));}catch(Exception e){return false;}}
}
