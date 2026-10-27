package com.pocketmusic.android;
import android.content.*;
import java.util.*;
final class TestMusicFixtures {
 static Track song(String id,String title,String artist){return new Track(id,title,artist,180,true,"");}
 static void seed(Context context,List<Track> songs)throws Exception{MusicTaste.remember(context,songs);Recommendations.save(context,Recommendations.scope(context),MusicTaste.version(context),List.of(new DiscoveryShelf("Your daily mix","Synthetic test catalogue",songs),new DiscoveryShelf("More of your sound","Synthetic test catalogue",songs)),0);}
 static final class Snapshot {
  private final java.util.function.Predicate<Context> tapPolicy=TapPreparation.allowed;private final OnlineAudio.Resolver tapResolver=TapPreparation.resolver;
  private final SharedPreferences prefs;private final Map<String,?> before;
  Snapshot(Context c){TapPreparation.clear();TapPreparation.allowed=context->false;prefs=c.getSharedPreferences("music",0);before=new HashMap<>(prefs.getAll());}
  void restore(){TapPreparation.clear();TapPreparation.allowed=tapPolicy;TapPreparation.resolver=tapResolver;SharedPreferences.Editor e=prefs.edit().clear();for(Map.Entry<String,?> v:before.entrySet()){Object x=v.getValue();if(x instanceof String)e.putString(v.getKey(),(String)x);else if(x instanceof Long)e.putLong(v.getKey(),(Long)x);else if(x instanceof Integer)e.putInt(v.getKey(),(Integer)x);else if(x instanceof Boolean)e.putBoolean(v.getKey(),(Boolean)x);else if(x instanceof Float)e.putFloat(v.getKey(),(Float)x);else if(x instanceof Set)e.putStringSet(v.getKey(),(Set<String>)x);}e.commit();}
 }
}
