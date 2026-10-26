package com.pocketmusic.android;
import java.util.*;
final class DiscoveryShelf {
 final String title,reason;final List<Track> tracks;
 DiscoveryShelf(String title,String reason,List<Track> tracks){this.title=title;this.reason=reason;this.tracks=new ArrayList<>(tracks);}
}
