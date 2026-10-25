package com.pocketmusic.android;
import org.junit.Test;
import java.io.File;
import static org.junit.Assert.*;
public class TrackFilesTest {
    @Test public void titlesAreCleanWithoutRenaming(){File f=new File("Artist - Song [abcdefghijk].mp3");assertEquals("Artist - Song",TrackFiles.title(f));assertEquals("abcdefghijk",TrackFiles.videoId(f));assertEquals("Artist - Song [abcdefghijk].mp3",f.getName());}
    @Test public void importedNamesStayIntact(){assertEquals("Piano [live]",TrackFiles.title(new File("Piano [live].FLAC")));assertNull(TrackFiles.videoId(new File("Piano [live].flac")));assertFalse(TrackFiles.audio(new File("Video.MP4")));}
}
