package com.pocketmusic.android;
import org.json.*;
import java.util.*;

/** One bounded YouTube Music session; serialize only inside encrypted storage. */
final class SessionAuth {
 final String cookie,visitorData,dataSyncId,authUser,clientVersion,accountName;
 private SessionAuth(String cookie,String visitor,String sync,String user,String version,String name){this.cookie=header(YouTubeCookies.netscape(cookie));visitorData=visitor;dataSyncId=sync;authUser=user;clientVersion=version;accountName=name;}
 static SessionAuth legacy(String cookie){YouTubeCookies.netscape(cookie);return new SessionAuth(cookie,"","","0","","");}
 static SessionAuth capture(String cookie,String visitor,String sync,String user,String version){
  if(!LoginPolicy.ready(cookie)||visitor==null||visitor.isBlank()||visitor.length()>4096||visitor.matches("(?s).*[\\x00-\\x20\\x7f].*")||version==null||!version.matches("[A-Za-z0-9._-]{1,100}"))throw new IllegalArgumentException("YouTube Music has not finished connecting this session.");
  String index=user==null?"0":user;if(!index.matches("[0-9]{1,3}"))throw new IllegalArgumentException("Invalid account selection.");String channel=sync==null?"":sync.split("\\|\\|",2)[0];if(!channel.matches("[A-Za-z0-9_-]{0,256}"))throw new IllegalArgumentException("Invalid channel selection.");return new SessionAuth(cookie,visitor,channel,index,version,"");
 }
 boolean complete(){return !visitorData.isEmpty()&&!clientVersion.isEmpty();}
 SessionAuth identity(String name){String clean=name==null?"":name.replaceAll("[\\p{Cntrl}]","").trim();if(clean.isEmpty()||clean.length()>200)throw new IllegalArgumentException("YouTube did not confirm an account identity.");return new SessionAuth(cookie,visitorData,dataSyncId,authUser,clientVersion,clean);}
 String encode()throws JSONException{return new JSONObject().put("format",2).put("cookies",YouTubeCookies.netscape(cookie)).put("visitorData",visitorData).put("dataSyncId",dataSyncId).put("authUser",authUser).put("clientVersion",clientVersion).put("accountName",accountName).toString();}
 static SessionAuth decode(String plain)throws Exception{if(!plain.startsWith("{"))return legacy(header(plain));JSONObject o=new JSONObject(plain);if(o.optInt("format")!=2)throw new IllegalArgumentException("Unsupported saved account.");String cookie=header(o.getString("cookies"));if(o.optString("visitorData").isEmpty())return legacy(cookie);SessionAuth auth=capture(cookie,o.getString("visitorData"),o.optString("dataSyncId"),o.optString("authUser","0"),o.getString("clientVersion"));return auth.identity(o.getString("accountName"));}
 static String header(String netscape){StringBuilder header=new StringBuilder();for(Map.Entry<String,String> item:AccountMusic.cookies(netscape).entrySet())header.append(item.getKey()).append('=').append(item.getValue()).append("; ");return header.toString();}
 boolean sameIdentity(SessionAuth other){return other!=null&&accountName.equals(other.accountName)&&visitorData.equals(other.visitorData)&&dataSyncId.equals(other.dataSyncId)&&authUser.equals(other.authUser)&&clientVersion.equals(other.clientVersion)&&YouTubeCookies.netscape(cookie).equals(YouTubeCookies.netscape(other.cookie));}
}
