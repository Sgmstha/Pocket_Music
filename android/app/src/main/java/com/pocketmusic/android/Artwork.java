package com.pocketmusic.android;
import android.content.Context;
import android.widget.ImageView;
import android.net.Uri;
import com.bumptech.glide.*;
import com.bumptech.glide.load.engine.cache.*;
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool;

/** Reuse Glide's lifecycle cancellation, deduplication and bounded image caches. */
final class Artwork {
 private static boolean initialized;
 static synchronized void init(Context c){if(initialized)return;Glide.init(c.getApplicationContext(),new GlideBuilder().setMemoryCache(new LruResourceCache(8*1024*1024)).setBitmapPool(new LruBitmapPool(4*1024*1024)).setDiskCache(new InternalCacheDiskCacheFactory(c.getApplicationContext(),"artwork",40*1024*1024)));initialized=true;}
 static boolean safeUrl(String value){if(value==null||value.length()>2048)return false;try{Uri u=Uri.parse(value);String h=u.getHost();return "https".equals(u.getScheme())&&h!=null&&(h.equals("i.ytimg.com")||h.equals("lh3.googleusercontent.com")||h.equals("yt3.googleusercontent.com"));}catch(Exception e){return false;}}
 static String url(Track t){if(safeUrl(t.artworkUrl)){String url=t.artworkUrl;if(Uri.parse(url).getHost().equals("lh3.googleusercontent.com"))url=url.replaceFirst("=w[0-9]+-h[0-9]+[-a-zA-Z0-9]*$","=w512-h512-l90-rj");return url;}return "https://i.ytimg.com/vi/"+t.id+"/hqdefault.jpg";}
 static void load(Context c,Track track,ImageView view,int pixels){init(c);Glide.with(view).load(url(track)).placeholder(new TrackCoverDrawable(track.title)).error(new TrackCoverDrawable(track.title)).centerCrop().override(pixels,pixels).into(view);}
}
