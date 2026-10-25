package com.pocketmusic.android;
import android.content.*;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import java.util.*;
import static org.junit.Assert.*;
public class RecentHistoryTest {
 private Context context;private String before;
 @Before public void setup(){context=InstrumentationRegistry.getInstrumentation().getTargetContext();before=context.getSharedPreferences("music",0).getString("testHistory",null);RecentHistory.clear(context,"testHistory");}
 @After public void cleanup(){SharedPreferences.Editor edit=context.getSharedPreferences("music",0).edit();if(before==null)edit.remove("testHistory");else edit.putString("testHistory",before);edit.commit();}
 @Test public void mostRecentIsFirstWithoutDuplicates(){RecentHistory.touch(context,"testHistory","one");RecentHistory.touch(context,"testHistory","two");RecentHistory.touch(context,"testHistory","one");assertEquals(List.of("one","two"),RecentHistory.read(context,"testHistory"));}
 @Test public void historyIsBoundedAndIgnoresBlank(){for(int i=0;i<20;i++)RecentHistory.touch(context,"testHistory","query"+i);RecentHistory.touch(context,"testHistory","  ");List<String> values=RecentHistory.read(context,"testHistory");assertEquals(12,values.size());assertEquals("query19",values.get(0));assertEquals("query8",values.get(11));}
 @Test public void malformedSavedHistoryRecovers(){context.getSharedPreferences("music",0).edit().putString("testHistory","broken json").commit();assertTrue(RecentHistory.read(context,"testHistory").isEmpty());RecentHistory.touch(context,"testHistory","music");assertEquals(List.of("music"),RecentHistory.read(context,"testHistory"));}
}
