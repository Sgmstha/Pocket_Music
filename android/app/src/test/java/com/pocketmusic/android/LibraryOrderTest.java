package com.pocketmusic.android;

import org.junit.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import static org.junit.Assert.*;

public class LibraryOrderTest {
 private File directory,audio,video,older;
 @Before public void setup()throws Exception{directory=Files.createTempDirectory("pocket-library").toFile();audio=new File(directory,"zebra.wav");video=new File(directory,"Clip.MP4");older=new File(directory,"Alpha.mp3");for(File f:new File[]{audio,video,older})assertTrue(f.createNewFile());assertTrue(audio.setLastModified(3000));assertTrue(video.setLastModified(2000));assertTrue(older.setLastModified(1000));}
 @After public void cleanup(){for(File f:new File[]{audio,video,older})if(f!=null)f.delete();if(directory!=null)directory.delete();}
 @Test public void audioFilterExcludesUppercaseVideo(){assertEquals(List.of(audio,older),LibraryOrder.select(List.of(video,older,audio),"audio",false));}
 @Test public void videoFilterDoesNotShowAudio(){assertEquals(List.of(video),LibraryOrder.select(List.of(older,video,audio),"video",false));}
 @Test public void nameOrderIsCaseInsensitiveAndDoesNotMutateSource(){List<File> files=new ArrayList<>(List.of(audio,video,older));assertEquals(List.of(older,video,audio),LibraryOrder.select(files,"all",true));assertEquals(List.of(audio,video,older),files);}
 @Test public void newestOrderAndEmptyFilteredList(){assertEquals(List.of(audio,video,older),LibraryOrder.select(List.of(older,video,audio),"all",false));assertTrue(LibraryOrder.select(List.of(audio),"video",false).isEmpty());}
}
