package com.pocketmusic.android;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class PlaybackOrderTest {
    @Test public void playNextPreservesCurrentAndTraversal(){PlaybackOrder q=new PlaybackOrder();q.reset(3,1,false);int appended=q.append(true);assertEquals(3,appended);assertEquals(1,q.current());assertEquals(3,q.advance(1,0,false));assertEquals(2,q.advance(1,0,false));}
    @Test public void movingExistingTrackNextDoesNotDuplicateIt(){PlaybackOrder q=new PlaybackOrder();q.reset(4,0,true);assertTrue(q.playNext(3));assertEquals(0,q.current());assertEquals(3,q.advance(1,0,false));assertEquals(4,new HashSet<>(q.sequence()).size());}
    @Test public void removalRemapsIndicesWithoutChangingSelectedSong(){PlaybackOrder q=new PlaybackOrder();q.reset(4,2,false);assertTrue(q.remove(0));assertEquals(1,q.current());assertEquals(List.of(0,1,2),q.sequence());assertFalse(q.remove(1));assertEquals(2,q.advance(1,0,false));}
    @Test public void selectingQueueItemKeepsManualOrdering(){PlaybackOrder q=new PlaybackOrder();q.reset(4,0,false);q.playNext(3);assertTrue(q.select(3));assertEquals(1,q.advance(1,0,false));assertFalse(q.select(20));}
    @Test public void stopsAtEnd(){PlaybackOrder q=new PlaybackOrder();q.reset(2,0,false);assertEquals(1,q.advance(1,0,true));assertEquals(-1,q.advance(1,0,true));assertEquals(1,q.current());}
    @Test public void repeatAllWraps(){PlaybackOrder q=new PlaybackOrder();q.reset(2,1,false);assertEquals(0,q.advance(1,1,true));assertEquals(1,q.advance(-1,1,false));}
    @Test public void repeatOneOnlyRepeatsAutomaticCompletion(){PlaybackOrder q=new PlaybackOrder();q.reset(3,1,false);assertEquals(1,q.advance(1,2,true));assertEquals(2,q.advance(1,2,false));}
    @Test public void shuffleVisitsEveryTrackOnce(){PlaybackOrder q=new PlaybackOrder(new Random(8));q.reset(12,4,true);Set<Integer> seen=new HashSet<>();seen.add(q.current());int i;while((i=q.advance(1,0,true))!=-1)assertTrue(seen.add(i));assertEquals(12,seen.size());}
    @Test public void togglingShufflePreservesCurrent(){PlaybackOrder q=new PlaybackOrder(new Random(2));q.reset(5,2,false);q.setShuffle(true);assertEquals(2,q.current());int n=q.advance(1,0,false);q.setShuffle(false);assertEquals(n,q.current());assertEquals(n-1,q.advance(-1,0,false));}
    @Test public void emptyQueueHasNoNext(){PlaybackOrder q=new PlaybackOrder();q.reset(0,0,false);assertEquals(-1,q.advance(1,1,true));}
    @Test public void aSingleTrackCanRepeat(){PlaybackOrder q=new PlaybackOrder();q.reset(1,0,true);assertEquals(0,q.advance(1,1,true));assertEquals(-1,q.advance(1,0,false));}
}
