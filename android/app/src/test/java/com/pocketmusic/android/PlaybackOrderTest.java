package com.pocketmusic.android;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class PlaybackOrderTest {
    @Test public void stopsAtEnd(){PlaybackOrder q=new PlaybackOrder();q.reset(2,0,false);assertEquals(1,q.advance(1,0,true));assertEquals(-1,q.advance(1,0,true));assertEquals(1,q.current());}
    @Test public void repeatAllWraps(){PlaybackOrder q=new PlaybackOrder();q.reset(2,1,false);assertEquals(0,q.advance(1,1,true));assertEquals(1,q.advance(-1,1,false));}
    @Test public void repeatOneOnlyRepeatsAutomaticCompletion(){PlaybackOrder q=new PlaybackOrder();q.reset(3,1,false);assertEquals(1,q.advance(1,2,true));assertEquals(2,q.advance(1,2,false));}
    @Test public void shuffleVisitsEveryTrackOnce(){PlaybackOrder q=new PlaybackOrder(new Random(8));q.reset(12,4,true);Set<Integer> seen=new HashSet<>();seen.add(q.current());int i;while((i=q.advance(1,0,true))!=-1)assertTrue(seen.add(i));assertEquals(12,seen.size());}
    @Test public void togglingShufflePreservesCurrent(){PlaybackOrder q=new PlaybackOrder(new Random(2));q.reset(5,2,false);q.setShuffle(true);assertEquals(2,q.current());int n=q.advance(1,0,false);q.setShuffle(false);assertEquals(n,q.current());assertEquals(n-1,q.advance(-1,0,false));}
    @Test public void emptyQueueHasNoNext(){PlaybackOrder q=new PlaybackOrder();q.reset(0,0,false);assertEquals(-1,q.advance(1,1,true));}
    @Test public void aSingleTrackCanRepeat(){PlaybackOrder q=new PlaybackOrder();q.reset(1,0,true);assertEquals(0,q.advance(1,1,true));assertEquals(-1,q.advance(1,0,false));}
}
