package com.adhkaritv.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Landscape photographs: list in assets/photos.json, downloaded once and kept in a disk cache,
 * decoded off the main thread at screen size, with the next photo pre-fetched.
 * When offline it rotates through whatever is already cached; if nothing is cached the view keeps
 * its night-blue fallback background.
 */
public final class PhotoManager {

    public interface Callback { void onPhoto(Bitmap bitmap); }

    private static final int MAX_CACHED_FILES = 60;

    private final List<String> urls = new ArrayList<>();
    private final File dir;
    private final int targetW, targetH;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile int position = -1;
    private volatile boolean closed;

    public PhotoManager(Context c, int screenW, int screenH) {
        this.targetW = Math.max(1280, Math.min(screenW, 3840));
        this.targetH = Math.max(720, Math.min(screenH, 2160));
        this.dir = new File(c.getCacheDir(), "photos");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        try (InputStream in = c.getAssets().open("photos.json")) {
            JSONArray arr = new JSONObject(readAll(in)).getJSONArray("photos");
            for (int i = 0; i < arr.length(); i++) {
                String base = arr.getJSONObject(i).getString("url");
                String sep = base.contains("?") ? "&" : "?";
                // Unsplash/imgix parameters: width matched to the screen, JPEG (decodable on every Android version).
                urls.add(base.contains("images.unsplash.com") ? base + sep + "w=" + targetW + "&q=82&fm=jpg" : base);
            }
        } catch (Exception ignored) { }
    }

    /** Loads the next photo (trying further URLs, then the cache, on failure) and pre-fetches the one after. */
    public void next(Callback cb) {
        if (closed || urls.isEmpty()) return;
        position = (position + 1) % urls.size();
        final int start = position;
        executor.execute(() -> {
            Bitmap bmp = null;
            int tried = 0;
            int idx = start;
            while (bmp == null && tried < urls.size() && !closed) {
                bmp = load(urls.get(idx));
                if (bmp == null) { idx = (idx + 1) % urls.size(); tried++; }
            }
            if (bmp == null && !closed) bmp = anyCached(start);
            position = idx;
            final Bitmap result = bmp;
            if (result != null && !closed) main.post(() -> { if (!closed) cb.onPhoto(result); });
            if (!closed) prefetch(urls.get((idx + 1) % urls.size()));
        });
    }

    public void close() {
        closed = true;
        executor.shutdownNow();
        main.removeCallbacksAndMessages(null);
    }

    // ---- internals (background thread) ----

    private Bitmap load(String url) {
        File f = fileFor(url);
        if (!f.exists() && !download(url, f)) return null;
        Bitmap b = decode(f);
        if (b == null) { //noinspection ResultOfMethodCallIgnored
            f.delete(); }
        return b;
    }

    private void prefetch(String url) {
        File f = fileFor(url);
        if (!f.exists()) download(url, f);
        trimCache();
    }

    private Bitmap anyCached(int seed) {
        File[] files = dir.listFiles((d, name) -> name.endsWith(".jpg"));
        if (files == null || files.length == 0) return null;
        for (int i = 0; i < files.length; i++) {
            Bitmap b = decode(files[(seed + i) % files.length]);
            if (b != null) return b;
        }
        return null;
    }

    private boolean download(String url, File dest) {
        HttpURLConnection conn = null;
        File tmp = new File(dest.getPath() + ".part");
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(15000);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "Athkari-AndroidTV");
            if (conn.getResponseCode() != 200) return false;
            try (InputStream in = conn.getInputStream(); OutputStream out = new FileOutputStream(tmp)) {
                byte[] buf = new byte[16384];
                int n;
                while ((n = in.read(buf)) > 0) {
                    if (closed) return false;
                    out.write(buf, 0, n);
                }
            }
            return tmp.renameTo(dest);
        } catch (Exception e) {
            return false;
        } finally {
            if (conn != null) conn.disconnect();
            if (tmp.exists()) { //noinspection ResultOfMethodCallIgnored
                tmp.delete(); }
        }
    }

    private Bitmap decode(File f) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(f.getPath(), o);
            if (o.outWidth <= 0 || o.outHeight <= 0) return null;
            int sample = 1;
            while (o.outWidth / (sample * 2) >= targetW && o.outHeight / (sample * 2) >= targetH) sample *= 2;
            BitmapFactory.Options d = new BitmapFactory.Options();
            d.inSampleSize = sample;
            d.inPreferredConfig = Bitmap.Config.ARGB_8888;
            return BitmapFactory.decodeFile(f.getPath(), d);
        } catch (OutOfMemoryError | Exception e) {
            return null;
        }
    }

    private void trimCache() {
        File[] files = dir.listFiles((d, name) -> name.endsWith(".jpg"));
        if (files == null || files.length <= MAX_CACHED_FILES) return;
        java.util.Arrays.sort(files, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
        for (int i = 0; i < files.length - MAX_CACHED_FILES; i++) { //noinspection ResultOfMethodCallIgnored
            files[i].delete(); }
    }

    private File fileFor(String url) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-1").digest(url.getBytes(StandardCharsets.UTF_8));
            StringBuilder s = new StringBuilder();
            for (byte b : h) s.append(String.format("%02x", b));
            return new File(dir, s + ".jpg");
        } catch (Exception e) {
            return new File(dir, Integer.toHexString(url.hashCode()) + ".jpg");
        }
    }

    private static String readAll(InputStream in) throws java.io.IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
