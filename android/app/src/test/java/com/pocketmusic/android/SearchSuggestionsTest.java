package com.pocketmusic.android;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class SearchSuggestionsTest {
 @Test public void historyAndSavedMatchesAppearBeforeQueryExpansions(){List<String> s=SearchSuggestions.build("piano",List.of("Quiet Piano"),List.of("Piano nights"));assertEquals("Quiet Piano",s.get(0));assertEquals("Piano nights",s.get(1));assertTrue(s.contains("piano live"));}
 @Test public void blankAndLinksDoNotGetInventedSuggestions(){assertTrue(SearchSuggestions.build(" ",List.of(),List.of()).isEmpty());assertTrue(SearchSuggestions.build("https://youtu.be/id",List.of(),List.of()).isEmpty());}
 @Test public void duplicateMatchesAndLengthAreBounded(){List<String> s=SearchSuggestions.build("a",List.of("Alpha","alpha","Amber","Atlantic","Alone","Again","Able"),List.of());assertEquals(5,s.size());assertEquals(1,s.stream().filter(v->v.equalsIgnoreCase("alpha")).count());}
}
