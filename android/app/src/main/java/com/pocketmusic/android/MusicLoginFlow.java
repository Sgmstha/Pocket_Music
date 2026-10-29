package com.pocketmusic.android;
import android.content.Context;
import org.json.*;
import java.io.*;
import java.util.function.BooleanSupplier;

/** Trusted Music configuration only. Never reads forms or exposes a native JS bridge. */
final class MusicLoginFlow {
 static final String CONFIG_SCRIPT="(function(){if(location.origin!=='https://music.youtube.com')return null;var get=function(k){return (window.ytcfg&&window.ytcfg.get&&window.ytcfg.get(k))||(window.yt&&window.yt.config_&&window.yt.config_[k]);};var ctx=get('INNERTUBE_CONTEXT')||{};return JSON.stringify({visitorData:get('VISITOR_DATA')||(ctx.client&&ctx.client.visitorData)||'',dataSyncId:get('DATASYNC_ID')||'',authUser:String(get('SESSION_INDEX')||0),clientVersion:get('INNERTUBE_CLIENT_VERSION')||(ctx.client&&ctx.client.clientVersion)||''});})()";
 interface Validator {SessionAuth verify(SessionAuth candidate,String process)throws Exception;}
 static Validator validator=AccountMusic::verifyAccount;
 static boolean trusted(String url){try{java.net.URI uri=new java.net.URI(url);return "https".equals(uri.getScheme())&&"music.youtube.com".equals(uri.getHost())&&uri.getRawUserInfo()==null&&(uri.getPort()==-1||uri.getPort()==443);}catch(Exception e){return false;}}
 static SessionAuth capture(String cookie,String evaluated)throws Exception{if(evaluated==null||evaluated.length()>16384)throw new IOException("Music session context is not ready.");Object value=new JSONTokener(evaluated).nextValue();if(!(value instanceof String))throw new IOException("Music session context is not ready.");JSONObject config=new JSONObject((String)value);return SessionAuth.capture(cookie,config.optString("visitorData"),config.optString("dataSyncId"),config.optString("authUser","0"),config.optString("clientVersion"));}
 static SessionAuth connect(Context c,SessionAuth candidate,String process,BooleanSupplier cancelled)throws Exception{SessionAuth verified=validator.verify(candidate,process);if(cancelled.getAsBoolean()||Thread.currentThread().isInterrupted())throw new InterruptedIOException("Connection cancelled.");try{YouTubeSession.save(c,verified);}catch(Exception error){throw new IOException("Account verified, but secure storage failed. Retry connection.",error);}return verified;}
}
