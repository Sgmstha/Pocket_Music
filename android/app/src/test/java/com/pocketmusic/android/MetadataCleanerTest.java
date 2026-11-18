package com.pocketmusic.android;

import org.junit.Test;
import static org.junit.Assert.*;

public class MetadataCleanerTest {
    @Test
    public void cleansOfficialMusicVideoAndSplitsArtist() {
        MetadataCleaner.Result r = MetadataCleaner.clean("Ed Sheeran - Shape of You (Official Music Video)", "Ed Sheeran");
        assertEquals("Shape of You", r.cleanTitle);
        assertEquals("Ed Sheeran", r.primaryArtist);
    }

    @Test
    public void cleansBracketedTags() {
        MetadataCleaner.Result r = MetadataCleaner.clean("Alan Walker - Faded [Official 4K Video]", "Alan Walker");
        assertEquals("Faded", r.cleanTitle);
        assertEquals("Alan Walker", r.primaryArtist);
    }

    @Test
    public void scrubsTopicAndVEVOChannelSuffixes() {
        MetadataCleaner.Result r = MetadataCleaner.clean("Blank Space", "TaylorSwiftVEVO");
        assertEquals("Blank Space", r.cleanTitle);
        assertEquals("TaylorSwift", r.primaryArtist);
    }
}
