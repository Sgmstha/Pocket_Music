package com.pocketmusic.android;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class SearchResultCacheTest {
 @Test public void equivalentQueriesShareResultsWithoutSharingMutableLists(){SearchResultCache cache=new SearchResultCache();List<Track> input=new ArrayList<>(List.of(new Track("abcdefghijk","Song")));cache.put("  HELLO   world ",input,100);input.clear();List<Track> hit=cache.get("hello world",200);assertEquals(1,hit.size());hit.clear();assertEquals(1,cache.get("hello world",300).size());}
 @Test public void expiryAndClockResetDoNotServeStaleResults(){SearchResultCache cache=new SearchResultCache();cache.put("song",List.of(new Track("abcdefghijk","Song")),100);assertNull(cache.get("song",1800100));cache.put("song",List.of(new Track("abcdefghijk","Song")),200);assertNull(cache.get("song",100));}
 @Test public void leastRecentlyUsedQueryIsEvicted(){SearchResultCache cache=new SearchResultCache();for(int i=0;i<8;i++)cache.put("q"+i,List.of(new Track("abcdefghijk","Song")),100);assertNotNull(cache.get("q0",101));cache.put("new",List.of(new Track("abcdefghijk","Song")),102);assertNull(cache.get("q1",103));assertNotNull(cache.get("q0",103));}
 @Test public void emptyResponsesDoNotPoisonCache(){SearchResultCache cache=new SearchResultCache();cache.put("q",List.of(),100);assertNull(cache.get("q",101));}
 @Test public void metadataSubtitleDoesNotInventMissingArtistOrDuration(){assertEquals("",new Track("abcdefghijk","Song").subtitle());assertEquals("Artist · 3:09",new Track("abcdefghijk","Song","Artist",189).subtitle());assertEquals("Artist",new Track("abcdefghijk","Song","Artist",-1).subtitle());}
}
