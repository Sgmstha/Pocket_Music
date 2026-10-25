package com.pocketmusic.android;
import java.io.File;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
final class TrackFiles {
    private static final Pattern ID=Pattern.compile("\\s*\\[([A-Za-z0-9_-]{11})\\]$");
    static String title(File file){
        String name=file.getName();int dot=name.lastIndexOf('.');
        if(dot>0)name=name.substring(0,dot);
        return ID.matcher(name).replaceFirst("").trim();
    }
    static String videoId(File file){
        String name=file.getName();int dot=name.lastIndexOf('.');
        Matcher m=ID.matcher(dot>0?name.substring(0,dot):name);return m.find()?m.group(1):null;
    }
    static boolean audio(File f){return !f.getName().toLowerCase(Locale.ROOT).endsWith(".mp4");}
}
