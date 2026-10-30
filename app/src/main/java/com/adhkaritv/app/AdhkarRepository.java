package com.adhkaritv.app;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Loads the built-in adhkar from assets/adhkar.json (works offline). */
public final class AdhkarRepository {

    public static final class Item {
        public final String text, source, category, time, count;
        Item(String text, String source, String category, String time, String count) {
            this.text = text; this.source = source; this.category = category; this.time = time; this.count = count;
        }
    }

    public static final String MORNING = "morning", EVENING = "evening", ANY = "any", BOTH = "both";

    private final List<Item> all = new ArrayList<>();

    public AdhkarRepository(Context c) {
        try (InputStream in = c.getAssets().open("adhkar.json")) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            JSONArray arr = new JSONObject(new String(out.toByteArray(), StandardCharsets.UTF_8)).getJSONArray("adhkar");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                all.add(new Item(o.getString("text"), o.getString("source"), o.getString("category"),
                        o.optString("time", ANY), o.optString("count", "")));
            }
        } catch (Exception e) {
            // Should never happen with the bundled file; keep the screen useful anyway.
            all.add(new Item("سُبْحَانَ اللَّهِ وَبِحَمْدِهِ", "صحيح البخاري 6405", "التسبيح", ANY, ""));
        }
    }

    /** Morning- or evening-specific items (including those marked "both"); empty for any other period. */
    public List<Item> specific(String period) {
        List<Item> out = new ArrayList<>();
        if (!MORNING.equals(period) && !EVENING.equals(period)) return out;
        for (Item it : all) if (it.time.equals(period) || BOTH.equals(it.time)) out.add(it);
        return out;
    }

    /** Items suitable at any time of day. */
    public List<Item> general() {
        List<Item> out = new ArrayList<>();
        for (Item it : all) if (ANY.equals(it.time)) out.add(it);
        if (out.isEmpty()) out.addAll(all);
        return out;
    }
}
