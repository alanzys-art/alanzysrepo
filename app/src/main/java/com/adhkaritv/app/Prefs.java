package com.adhkaritv.app;

import android.content.Context;
import android.content.SharedPreferences;

/** User settings with sensible defaults. */
public final class Prefs {
    static final int[] ADHKAR_SECONDS = {15, 20, 30, 45, 60, 90, 120};
    static final int[] PHOTO_SECONDS = {30, 60, 120, 300, 600};
    static final int[] HIJRI_ADJUST = {-1, 0, 1};
    static final String[] FONT_LABELS = {"صغير", "متوسط", "كبير"};
    static final float[] FONT_SP = {30f, 36f, 42f};
    static final String[] CLOCK_FONT_LABELS = {"Inter Tight (افتراضي)", "Outfit", "Playfair Display", "Roboto Condensed"};
    static final int[] CARD_OPACITY = {100, 75, 50, 25, 0};

    private static final String FILE = "athkari_settings";
    private final SharedPreferences p;

    public Prefs(Context c) { p = c.getSharedPreferences(FILE, Context.MODE_PRIVATE); }

    public int adhkarSeconds() { return p.getInt("adhkar_seconds", 30); }
    public int photoSeconds() { return p.getInt("photo_seconds", 60); }
    public int hijriAdjust() { return p.getInt("hijri_adjust", 0); }
    public boolean countdown() { return p.getBoolean("countdown", true); }
    public int fontIndex() { return p.getInt("font_index", 1); }
    public int clockFont() { return p.getInt("clock_font", 0); }
    public int cardOpacity() { return p.getInt("card_opacity", 100); }
    public void setClockFont(int v) { p.edit().putInt("clock_font", v).apply(); }
    public void setCardOpacity(int v) { p.edit().putInt("card_opacity", v).apply(); }
    public PrayerCalculator.Method method() { return PrayerCalculator.Method.fromName(p.getString("method", "KUWAIT")); }

    public void setAdhkarSeconds(int v) { p.edit().putInt("adhkar_seconds", v).apply(); }
    public void setPhotoSeconds(int v) { p.edit().putInt("photo_seconds", v).apply(); }
    public void setHijriAdjust(int v) { p.edit().putInt("hijri_adjust", v).apply(); }
    public void setCountdown(boolean v) { p.edit().putBoolean("countdown", v).apply(); }
    public void setFontIndex(int v) { p.edit().putInt("font_index", v).apply(); }
    public void setMethod(PrayerCalculator.Method m) { p.edit().putString("method", m.name()).apply(); }
    public void reset() { p.edit().clear().apply(); }
}
