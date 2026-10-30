package com.adhkaritv.app;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Landscape photographs bundled inside the APK (assets/photos/*.jpg) — no internet needed.
 * The build (GitHub Actions) downloads the list in assets/photos.json into that folder.
 *
 * Order is random: a "shuffle bag" plays every photo once in random order, then reshuffles,
 * never showing the same photo twice in a row. Decoding happens off the main thread at screen size.
 */
public final class PhotoManager {

    public interface Callback { void onPhoto(Bitmap bitmap, Bitmap blurred); }

    private static final String DIR = "photos";

    private final AssetManager assets;
    private final List<String> files = new ArrayList<>();
    private final List<String> bag = new ArrayList<>();
    private final Random random = new Random();
    private final int targetW, targetH;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private String last;
    private volatile boolean closed;

    public PhotoManager(Context c, int screenW, int screenH) {
        this.assets = c.getAssets();
        this.targetW = Math.max(1280, Math.min(screenW, 3840));
        this.targetH = Math.max(720, Math.min(screenH, 2160));
        try {
            String[] names = assets.list(DIR);
            if (names != null) {
                for (String n : names) {
                    String l = n.toLowerCase();
                    if (l.endsWith(".jpg") || l.endsWith(".jpeg") || l.endsWith(".png") || l.endsWith(".webp")) files.add(n);
                }
            }
        } catch (Exception ignored) { }
    }

    /** Number of bundled photos (0 means the night-blue background is used). */
    public int count() { return files.size(); }

    /** Loads the next random photo (skipping any unreadable file). */
    public void next(Callback cb) {
        if (closed || files.isEmpty()) return;
        executor.execute(() -> {
            Bitmap bmp = null;
            for (int tries = 0; bmp == null && tries < files.size() && !closed; tries++) {
                bmp = decode(DIR + "/" + draw());
            }
            if (bmp == null || closed) return;
            final Bitmap result = bmp;
            final Bitmap blurred = blur(result);
            main.post(() -> { if (!closed) cb.onPhoto(result, blurred); });
        });
    }

    public void close() {
        closed = true;
        executor.shutdownNow();
        main.removeCallbacksAndMessages(null);
    }

    /** Shuffle bag: every photo once per round, random order, no immediate repeat across rounds. */
    private String draw() {
        if (bag.isEmpty()) {
            bag.addAll(files);
            Collections.shuffle(bag, random);
            if (bag.size() > 1 && bag.get(0).equals(last)) Collections.swap(bag, 0, 1 + random.nextInt(bag.size() - 1));
        }
        last = bag.remove(0);
        return last;
    }

    private Bitmap decode(String path) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            try (InputStream in = assets.open(path)) { BitmapFactory.decodeStream(in, null, o); }
            if (o.outWidth <= 0 || o.outHeight <= 0) return null;
            int sample = 1;
            while (o.outWidth / (sample * 2) >= targetW && o.outHeight / (sample * 2) >= targetH) sample *= 2;
            BitmapFactory.Options d = new BitmapFactory.Options();
            d.inSampleSize = sample;
            d.inPreferredConfig = Bitmap.Config.ARGB_8888;
            try (InputStream in = assets.open(path)) { return BitmapFactory.decodeStream(in, null, d); }
        } catch (OutOfMemoryError | Exception e) {
            return null;
        }
    }

    /**
     * Soft blur for the glass cards: shrink the photo to 1/10 size, run a true blur on that small image
     * (three box-blur passes, close to a Gaussian), and let the card enlarge it. Because the small image is
     * already smooth, the enlargement shows no blocks.
     */
    static Bitmap blur(Bitmap src) {
        try {
            int w = Math.max(8, src.getWidth() / 10), h = Math.max(8, src.getHeight() / 10);
            Bitmap small = Bitmap.createScaledBitmap(src, w, h, true);
            int[] px = new int[w * h];
            small.getPixels(px, 0, w, 0, 0, w, h);
            BoxBlur.blur(px, w, h, 4, 3);
            Bitmap out = Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888);
            if (small != src) small.recycle();
            return out;
        } catch (Throwable t) {
            return null;
        }
    }
}
