package com.pocketmusic.android;

import java.util.regex.*;

/** Clean YouTube video titles into normalized track titles and artist names. GPL-3.0. */
final class MetadataCleaner {
    static final class Result {
        final String rawTitle, cleanTitle, primaryArtist;
        Result(String r, String c, String a) {
            rawTitle = r; cleanTitle = c; primaryArtist = a;
        }
    }

    private static final Pattern[] JUNK_PATTERNS = new Pattern[] {
        Pattern.compile("(?i)\\s*[\\[(]\\s*(?:official\\s+)?lyrics?\\s*(?:video)?\\s*[\\])].*$"),
        Pattern.compile("(?i)\\s*\\(.*?(official\\s+music\\s+video|official\\s+video|official\\s+audio|music\\s+video|lyric\\s+video|lyrics\\s+video|official\\s+visualizer|visualizer|video|audio|mv|pv|hd|hq|4k|1080p|720p|60fps|uhd).*?\\)"),
        Pattern.compile("(?i)\\s*\\[.*?(official\\s+music\\s+video|official\\s+video|official\\s+audio|music\\s+video|lyric\\s+video|lyrics\\s+video|official\\s+visualizer|visualizer|video|audio|mv|pv|hd|hq|4k|1080p|720p|60fps|uhd).*?\\]"),
        Pattern.compile("(?i)\\s*【.*?(official|mv|pv|video|audio|lyrics).*?】"),
        Pattern.compile("(?i)\\s*「.*?(official|mv|pv|video|audio|lyrics).*?」"),
        Pattern.compile("\\s*\\|.*$"),
        Pattern.compile("\\s*//.*$"),
        Pattern.compile("\\s*#\\w+"),
        Pattern.compile("(?i)\\s*-\\s*(official|video|audio|lyrics|lyric|visualizer).*$")
    };

    private static final Pattern ARTIST_TITLE_SPLIT = Pattern.compile("^\\s*([^–\\-:|]+?)\\s*[\\-–]\\s*[\"']?(.+?)[\"']?\\s*$");

    static Result clean(String rawTitle, String rawArtist) {
        if (rawTitle == null || rawTitle.trim().isEmpty()) {
            return new Result("", "", rawArtist == null ? "" : rawArtist.trim());
        }
        String title = rawTitle.trim()
            .replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'");
        String artist = rawArtist == null ? "" : rawArtist.trim()
            .replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'");

        artist = artist.replaceAll("(?i)\\s*-\\s*Topic$", "")
                       .replaceAll("(?i)VEVO$", "")
                       .replaceAll("(?i)\\s+Official$", "")
                       .replaceAll("(?i)\\s+Records$", "");

        Matcher split = ARTIST_TITLE_SPLIT.matcher(title);
        if (split.matches()) {
            String candArtist = split.group(1).trim();
            String candTitle = split.group(2).trim();
            if (candArtist.length() > 1 && !candArtist.matches("^\\d+$")) {
                artist = candArtist;
                title = candTitle;
            }
        }

        for (Pattern p : JUNK_PATTERNS) {
            title = p.matcher(title).replaceAll("");
        }

        title = title.replaceAll("\\s{2,}", " ").trim();
        title = title.replaceAll("^[\\s\\-–|/]+|[\\s\\-–|/]+$", "");
        artist = artist.replaceAll("\\s{2,}", " ").trim();
        artist = artist.replaceAll("^[\\s\\-–|/]+|[\\s\\-–|/]+$", "");

        if (artist.isEmpty()) artist = (rawArtist == null || rawArtist.trim().isEmpty()) ? "YouTube" : rawArtist.trim();
        if (title.isEmpty()) title = rawTitle.trim();

        return new Result(rawTitle, title, artist);
    }
}
