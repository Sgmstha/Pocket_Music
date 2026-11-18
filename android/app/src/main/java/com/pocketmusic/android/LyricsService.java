package com.pocketmusic.android;

import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** Lightweight LRCLIB synced lyrics service for Android. GPL-3.0. */
final class LyricsService {
    static final class LyricsResult {
        final boolean found;
        final String trackName, artistName, plainLyrics;

        LyricsResult(boolean f, String t, String a, String p) {
            found = f; trackName = t; artistName = a; plainLyrics = p;
        }
    }

    interface Callback {
        void onLyrics(LyricsResult result);
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "PocketMusicLyrics");
        t.setDaemon(true);
        return t;
    });
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Map<String, LyricsResult> CACHE = new ConcurrentHashMap<>();

    static void fetch(String rawTitle, String rawArtist, long durationSeconds, Callback callback) {
        MetadataCleaner.Result meta = MetadataCleaner.clean(rawTitle, rawArtist);
        String title = meta.cleanTitle;
        String artist = meta.primaryArtist;
        if (title == null || title.isEmpty()) {
            callback.onLyrics(new LyricsResult(false, "", "", ""));
            return;
        }

        String cacheKey = (title + "::" + artist).toLowerCase(Locale.ROOT);
        LyricsResult cached = CACHE.get(cacheKey);
        if (cached != null) {
            callback.onLyrics(cached);
            return;
        }

        EXECUTOR.execute(() -> {
            LyricsResult res = loadHttp(title, artist, durationSeconds);
            CACHE.put(cacheKey, res);
            MAIN.post(() -> callback.onLyrics(res));
        });
    }

    private static LyricsResult loadHttp(String title, String artist, long duration) {
        try {
            String encTitle = URLEncoder.encode(title, StandardCharsets.UTF_8.name());
            String encArtist = (artist == null || artist.isEmpty() || artist.equals("YouTube")) ? "" : URLEncoder.encode(artist, StandardCharsets.UTF_8.name());
            String urlStr = "https://lrclib.net/api/get?track_name=" + encTitle;
            if (!encArtist.isEmpty()) urlStr += "&artist_name=" + encArtist;
            if (duration > 5) urlStr += "&duration=" + duration;

            JSONObject obj = fetchJson(urlStr);
            if (obj == null) {
                // Fallback to search query
                String searchQ = URLEncoder.encode((title + " " + artist).trim(), StandardCharsets.UTF_8.name());
                String searchUrl = "https://lrclib.net/api/search?q=" + searchQ;
                String raw = fetchRaw(searchUrl);
                if (raw != null) {
                    JSONArray arr = new JSONArray(raw);
                    if (arr.length() > 0) obj = arr.optJSONObject(0);
                }
            }

            if (obj != null) {
                String plain = obj.optString("plainLyrics", "");
                String synced = obj.optString("syncedLyrics", "");
                String lyrics = !plain.isEmpty() ? plain : cleanSyncedToPlain(synced);
                if (!lyrics.isEmpty()) {
                    return new LyricsResult(true, obj.optString("trackName", title), obj.optString("artistName", artist), lyrics);
                }
            }
        } catch (Exception ignored) {
        }
        return new LyricsResult(false, title, artist, "");
    }

    private static String cleanSyncedToPlain(String synced) {
        if (synced == null || synced.isEmpty()) return "";
        return synced.replaceAll("\\[\\d{1,2}:\\d{2}(?:\\.\\d{1,3})?\\]\\s*", "");
    }

    private static JSONObject fetchJson(String url) {
        try {
            String raw = fetchRaw(url);
            return (raw != null && raw.startsWith("{")) ? new JSONObject(raw) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String fetchRaw(String urlStr) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            conn.setRequestProperty("User-Agent", "PocketMusic-Android/1.0");
            if (conn.getResponseCode() != 200) return null;
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) sb.append(line).append('\n');
                return sb.toString();
            }
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
