package com.pocketmusic.android;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class SmartDownloadPolicyTest {
 private Track song(String id){return new Track(id,id,"Artist",180,true,"");}
 @Test public void interleavesPersonalSignalsAndDeduplicates(){Track a=song("abcdefghijk"),b=song("12345678901"),c=song("ZYXWVUTSRQP");assertEquals(List.of(a,b,c),SmartDownloadPolicy.select(List.of(a,b),List.of(b,a),List.of(c,a),Set.of(),Set.of(),3));}
 @Test public void skipsHiddenExcludedManualVideosAndLongMixes(){Track good=song("abcdefghijk");List<Track> input=List.of(new Track("12345678901","Video","",180,false,""),new Track("ZYXWVUTSRQP","Long","",3600,true,""),new Track("abcdefghij2","Short","",10,true,""),song("abcdefghij3"),song("abcdefghij4"),good);assertEquals(List.of(good),SmartDownloadPolicy.select(input,List.of(),List.of(),Set.of("abcdefghij3"),Set.of("abcdefghij4"),10));}
 @Test public void capAndEmptyProfileAreBounded(){assertTrue(SmartDownloadPolicy.select(List.of(),List.of(),List.of(),Set.of(),Set.of(),25).isEmpty());assertEquals(1,SmartDownloadPolicy.select(List.of(song("abcdefghijk"),song("12345678901")),List.of(),List.of(),Set.of(),Set.of(),1).size());}
 @Test public void verifiedAccountSongsCanResolveUnknownDurationAtDownloadTime(){Track account=new Track("abcdefghijk","Account song","Artist",0,true,"");assertEquals(List.of(account),SmartDownloadPolicy.select(List.of(),List.of(),List.of(account),Set.of(),Set.of(),10));}
}
