package com.pocketmusic.android;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class RecommendationPolicyTest {
 private Track song(String id,String artist){return new Track(id,"Song",artist,180,true,"");}
 @Test public void wordsLikeMusicCannotAdmitGeneralVideos(){List<Track> result=RecommendationPolicy.select(List.of(new Track("abcdefghijk","Music news podcast","Channel",180),song("12345678901","Artist")),Set.of(),new HashSet<>(),12,4);assertEquals(1,result.size());assertEquals("12345678901",result.get(0).id);}
 @Test public void hiddenAndDuplicateSongsNeverReappear(){Set<String> seen=new HashSet<>();Track song=song("abcdefghijk","Artist");assertTrue(RecommendationPolicy.select(List.of(song),Set.of(song.id),seen,12,4).isEmpty());assertEquals(1,RecommendationPolicy.select(List.of(song,song),Set.of(),seen,12,4).size());assertTrue(RecommendationPolicy.select(List.of(song),Set.of(),seen,12,4).isEmpty());}
 @Test public void artistDiversityCapDoesNotRemoveOtherArtists(){List<Track> candidates=new ArrayList<>();for(int i=0;i<6;i++)candidates.add(song("0000000000"+i,"Same Artist - Topic"));candidates.add(song("abcdefghijk","Different Artist"));List<Track> found=RecommendationPolicy.select(candidates,Set.of(),new HashSet<>(),12,3);assertEquals(4,found.size());assertEquals("Different Artist",found.get(3).artist);}
 @Test public void invalidAndNonPositiveDurationMetadataIsExcluded(){assertTrue(RecommendationPolicy.select(List.of(new Track("short","Bad","Artist",120,true,""),new Track("abcdefghijk","Live","Artist",0,true,"")),Set.of(),new HashSet<>(),12,4).isEmpty());}
 @Test public void tasteSeedsAreBoundedAndCannotTurnIntoArbitraryUrls(){List<String> seeds=RecommendationPolicy.seeds(List.of("https://example.com","Dua Lipa","dua lipa"),List.of(song("abcdefghijk","Artist - Topic")),List.of(),List.of("Jazz"),"");assertEquals(3,seeds.size());assertEquals("Dua Lipa",seeds.get(0));assertFalse(seeds.stream().anyMatch(s->s.contains("://")));assertEquals(1,seeds.stream().filter(s->s.equalsIgnoreCase("Dua Lipa")).count());}
 @Test public void coldStartStillUsesOnlyExplicitMusicSearchSeeds(){List<String> seeds=RecommendationPolicy.seeds(List.of(),List.of(),List.of(),List.of("Nepali","Acoustic"),"");assertEquals(3,seeds.size());assertEquals("Nepali songs",seeds.get(0));}
 @Test public void refreshRotationReachesMoreChosenArtistsAndGenres(){List<String> initial=RecommendationPolicy.seeds(List.of("One","Two","Three","Four"),List.of(),List.of(),List.of("Pop","Jazz"),"",0);List<String> rotated=RecommendationPolicy.seeds(List.of("One","Two","Three","Four"),List.of(),List.of(),List.of("Pop","Jazz"),"",1);assertNotEquals(initial,rotated);assertTrue(rotated.contains("Three"));assertTrue(rotated.contains("Jazz songs"));assertEquals(3,rotated.size());}
}
