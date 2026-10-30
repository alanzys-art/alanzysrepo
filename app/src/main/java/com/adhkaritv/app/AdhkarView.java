package com.adhkaritv.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * The full-screen Athkari screen, shared by the launcher activity and the screensaver (DreamService).
 *
 * Layout: clock | dates top-left, branding top-right, the dhikr in a frosted-glass card in the centre,
 * and a frosted-glass prayer card at the bottom (next prayer, time remaining, progress, six times).
 * Photos cross-fade underneath; the cards and clock never fade.
 *
 * Lifecycle: call {@link #start()} when visible, {@link #stop()} when hidden, {@link #destroy()} at the end.
 */
public class AdhkarView extends FrameLayout {

    static final int IVORY = 0xFFF6F0E3;
    static final int IVORY_SOFT = 0xCCF6F0E3;
    static final int IVORY_DIM = 0x99F6F0E3;
    static final int GOLD = 0xFFE2BE6A;
    static final int NIGHT_TOP = 0xFF0B1830;
    static final int NIGHT_BOTTOM = 0xFF16304F;
    private static final int SHADOW = 0x99000000;
    private static final int GLASS_TINT = 0x52101B2E; // navy wash over the blurred photo

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Prefs prefs;
    private final AdhkarRepository repo;
    private final PhotoManager photos;
    private final float unit; // 1 unit = 1/960 of the screen width
    private final Typeface amiri;

    private final ImageView[] photoViews = new ImageView[2];
    private int frontPhoto = 0;
    private boolean hasPhoto;

    private final TextView clockText, gregorianText, hijriText;
    private final GlassCard dhikrCard, prayerCard;
    private final LinearLayout dhikrContent;
    private final TextView categoryText, dhikrText, countText, sourceText;
    private final TextView cityText, nextLabel, nextName, remainLabel, remainText, hintText;
    private final LinearLayout countdownRow;
    private final FrameLayout progressTrack;
    private final View progressFill;
    private final TextView[] nameCells = new TextView[6], timeCells = new TextView[6];
    private final LinearLayout[] cells = new LinearLayout[6];

    private boolean running;
    private int adhkarIndex = -1;
    private String lastPeriod = "";
    private String lastClock = "", lastDateKey = "";
    private LocalDate lastPanelDate;
    private int lastHighlight = -2;
    private PrayerCalculator.Method method;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!running) return;
            updateClockAndPrayers();
            long now = System.currentTimeMillis();
            handler.postDelayed(this, 1000 - (now % 1000) + 15); // align to the next wall-clock second
        }
    };
    private final Runnable nextDhikr = new Runnable() {
        @Override public void run() {
            if (!running) return;
            showNextDhikr(true);
            handler.postDelayed(this, prefs.adhkarSeconds() * 1000L);
        }
    };
    private final Runnable nextPhoto = new Runnable() {
        @Override public void run() {
            if (!running) return;
            photos.next(AdhkarView.this::crossFadeTo);
            handler.postDelayed(this, prefs.photoSeconds() * 1000L);
        }
    };

    public AdhkarView(Context context) {
        super(context);
        prefs = new Prefs(context);
        repo = new AdhkarRepository(context);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int w = Math.max(dm.widthPixels, dm.heightPixels), h = Math.min(dm.widthPixels, dm.heightPixels);
        unit = w / 960f;
        photos = new PhotoManager(context, w, h);
        Typeface tf;
        try { tf = Typeface.createFromAsset(context.getAssets(), "fonts/Amiri-Regular.ttf"); }
        catch (Exception e) { tf = Typeface.SERIF; }
        amiri = tf;
        setLayoutDirection(LAYOUT_DIRECTION_LTR); // positions below are physical left/right

        // --- background: night-blue fallback, two photo layers, light readability shading ---
        View base = new View(context);
        base.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{NIGHT_TOP, NIGHT_BOTTOM}));
        addView(base, new LayoutParams(-1, -1));
        for (int i = 0; i < 2; i++) {
            ImageView iv = new ImageView(context);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setAlpha(0f);
            photoViews[i] = iv;
            addView(iv, new LayoutParams(-1, -1));
        }
        View topShade = new View(context);
        topShade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0x80000000, 0x26000000, 0x00000000}));
        addView(topShade, new LayoutParams(-1, u(150), Gravity.TOP));

        // --- top-left: clock | dates ---
        LinearLayout clockRow = new LinearLayout(context);
        clockRow.setOrientation(LinearLayout.HORIZONTAL);
        clockRow.setGravity(Gravity.CENTER_VERTICAL);
        clockText = label(context, 46, IVORY, null);
        clockText.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        clockText.setIncludeFontPadding(false);
        clockRow.addView(clockText);
        View divider = new View(context);
        divider.setBackgroundColor(0x66F6F0E3);
        LinearLayout.LayoutParams dvp = new LinearLayout.LayoutParams(Math.max(1, u(1)), u(40));
        dvp.setMargins(u(14), 0, u(14), 0);
        clockRow.addView(divider, dvp);
        LinearLayout dateCol = new LinearLayout(context);
        dateCol.setOrientation(LinearLayout.VERTICAL);
        gregorianText = label(context, 14.5f, IVORY, null);
        hijriText = label(context, 14.5f, GOLD, null);
        gregorianText.setGravity(Gravity.RIGHT);
        hijriText.setGravity(Gravity.RIGHT);
        dateCol.addView(gregorianText, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = u(2);
        dateCol.addView(hijriText, hp);
        clockRow.addView(dateCol);
        LayoutParams clp = new LayoutParams(-2, -2, Gravity.TOP | Gravity.LEFT);
        clp.setMargins(u(48), u(28), 0, 0);
        addView(clockRow, clp);

        // --- top-right: branding ---
        LinearLayout brand = new LinearLayout(context);
        brand.setOrientation(LinearLayout.HORIZONTAL);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout brandText = new LinearLayout(context);
        brandText.setOrientation(LinearLayout.VERTICAL);
        brandText.setGravity(Gravity.RIGHT);
        TextView brandAr = label(context, 24, GOLD, amiri);
        brandAr.setText("أذكاري");
        brandAr.setIncludeFontPadding(false);
        TextView brandEn = label(context, 8.5f, IVORY_DIM, null);
        brandEn.setText("ATHKARI");
        brandEn.setLetterSpacing(0.35f);
        brandText.addView(brandAr);
        brandText.addView(brandEn, vertical(u(1)));
        ImageView crescent = new ImageView(context);
        crescent.setImageResource(R.drawable.crescent);
        brand.addView(brandText);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(u(30), u(30));
        cp.leftMargin = u(10);
        brand.addView(crescent, cp);
        LayoutParams bp = new LayoutParams(-2, -2, Gravity.TOP | Gravity.RIGHT);
        bp.setMargins(0, u(30), u(48), 0);
        addView(brand, bp);

        // --- centre: dhikr in a glass card ---
        dhikrCard = new GlassCard(context, u(22), Math.max(1, u(0.8f)), GLASS_TINT);
        dhikrContent = dhikrCard.content;
        dhikrContent.setGravity(Gravity.CENTER_HORIZONTAL);
        dhikrContent.setPadding(u(36), u(22), u(36), u(22));
        categoryText = label(context, 12.5f, GOLD, null);
        categoryText.setBackground(pill(0x26E2BE6A, 0x66E2BE6A));
        categoryText.setPadding(u(14), u(3), u(14), u(4));
        dhikrText = label(context, 36, IVORY, amiri);
        dhikrText.setLineSpacing(0, 1.15f);
        countText = label(context, 12.5f, GOLD, null);
        sourceText = label(context, 12, IVORY_SOFT, null);
        dhikrContent.addView(categoryText, new LinearLayout.LayoutParams(-2, -2));
        LinearLayout.LayoutParams dtp = new LinearLayout.LayoutParams(-2, -2);
        dtp.topMargin = u(14);
        dhikrContent.addView(dhikrText, dtp);
        dhikrContent.addView(countText, vertical(u(10)));
        dhikrContent.addView(sourceText, vertical(u(8)));
        // Centred in the space between the clock row (top) and the prayer card (bottom).
        LayoutParams dcp = new LayoutParams(-2, -2, Gravity.CENTER);
        dcp.setMargins(u(110), u(96), u(110), u(172));
        addView(dhikrCard, dcp);
        dhikrContent.setMinimumWidth(u(440));
        dhikrText.setMaxWidth(u(960 - 220 - 72));

        // --- bottom: prayer card ---
        prayerCard = new GlassCard(context, u(18), Math.max(1, u(0.8f)), GLASS_TINT);
        LinearLayout pc = prayerCard.content;
        pc.setPadding(u(20), u(10), u(20), u(10));
        cityText = label(context, 11, GOLD, null);
        pc.addView(cityText, new LinearLayout.LayoutParams(-1, -2));

        countdownRow = new LinearLayout(context);
        countdownRow.setOrientation(LinearLayout.HORIZONTAL);
        countdownRow.setLayoutDirection(LAYOUT_DIRECTION_RTL);
        LinearLayout nextCol = new LinearLayout(context);
        nextCol.setOrientation(LinearLayout.VERTICAL);
        nextLabel = label(context, 10, IVORY_SOFT, null);
        nextLabel.setText("الصلاة القادمة");
        nextName = label(context, 16, IVORY, null);
        nextLabel.setGravity(Gravity.RIGHT);
        nextName.setGravity(Gravity.RIGHT);
        nextCol.addView(nextLabel, new LinearLayout.LayoutParams(-1, -2));
        nextCol.addView(nextName, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout remainCol = new LinearLayout(context);
        remainCol.setOrientation(LinearLayout.VERTICAL);
        remainLabel = label(context, 10, IVORY_SOFT, null);
        remainText = label(context, 16, GOLD, null);
        remainLabel.setGravity(Gravity.LEFT);
        remainText.setGravity(Gravity.LEFT);
        remainCol.addView(remainLabel, new LinearLayout.LayoutParams(-1, -2));
        remainCol.addView(remainText, new LinearLayout.LayoutParams(-1, -2));
        countdownRow.addView(nextCol, new LinearLayout.LayoutParams(0, -2, 1f));
        countdownRow.addView(remainCol, new LinearLayout.LayoutParams(0, -2, 1f));
        LinearLayout.LayoutParams crp = new LinearLayout.LayoutParams(-1, -2);
        crp.topMargin = u(6);
        pc.addView(countdownRow, crp);

        progressTrack = new FrameLayout(context);
        progressTrack.setBackground(pill(0x33F6F0E3, 0));
        progressFill = new View(context);
        progressFill.setBackground(pill(GOLD, 0));
        progressTrack.addView(progressFill, new LayoutParams(-1, -1));
        LinearLayout.LayoutParams ptp = new LinearLayout.LayoutParams(-1, Math.max(2, u(2.5f)));
        ptp.setMargins(0, u(7), 0, u(7));
        pc.addView(progressTrack, ptp);

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutDirection(LAYOUT_DIRECTION_RTL); // الفجر on the right
        for (int i = 0; i < 6; i++) {
            LinearLayout cell = new LinearLayout(context);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            cell.setPadding(u(12), u(3), u(12), u(4));
            nameCells[i] = label(context, 11, IVORY_SOFT, null);
            nameCells[i].setText(PrayerCalculator.NAMES[i]);
            timeCells[i] = label(context, 15, IVORY, null);
            cell.addView(nameCells[i]);
            cell.addView(timeCells[i], vertical(u(1)));
            cells[i] = cell;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.setMargins(u(3), 0, u(3), 0);
            row.addView(cell, lp);
        }
        pc.addView(row, new LinearLayout.LayoutParams(-2, -2));
        LayoutParams pcp = new LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        pcp.bottomMargin = u(26);
        addView(prayerCard, pcp);

        // --- transient hint (launcher mode only) ---
        hintText = label(context, 11.5f, IVORY_SOFT, null);
        hintText.setBackground(pill(0x66000000, 0));
        hintText.setPadding(u(12), u(4), u(12), u(5));
        hintText.setAlpha(0f);
        LayoutParams hlp = new LayoutParams(-2, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        hlp.topMargin = u(34);
        addView(hintText, hlp);
    }

    // ---------- lifecycle ----------

    public void start() {
        if (running) return;
        running = true;
        method = prefs.method();
        boolean countdown = prefs.countdown();
        countdownRow.setVisibility(countdown ? VISIBLE : GONE);
        progressTrack.setVisibility(countdown ? VISIBLE : GONE);
        lastClock = ""; lastDateKey = ""; lastPanelDate = null; lastHighlight = -2; // settings may have changed
        handler.post(tick);
        if (adhkarIndex < 0) showNextDhikr(false); else applyDhikrSize();
        handler.postDelayed(nextDhikr, prefs.adhkarSeconds() * 1000L);
        if (!hasPhoto) handler.post(nextPhoto); else handler.postDelayed(nextPhoto, prefs.photoSeconds() * 1000L);
    }

    public void stop() {
        running = false;
        handler.removeCallbacksAndMessages(null);
    }

    public void destroy() {
        stop();
        photos.close();
        for (ImageView iv : photoViews) iv.setImageDrawable(null);
        dhikrCard.clear();
        prayerCard.clear();
    }

    public void showHint(String text, long millis) {
        hintText.setText(text);
        hintText.animate().alpha(1f).setDuration(600).start();
        handler.postDelayed(() -> hintText.animate().alpha(0f).setDuration(900).start(), millis);
    }

    // ---------- clock, dates, prayers ----------

    private void updateClockAndPrayers() {
        ZonedDateTime now = ZonedDateTime.now(PrayerCalculator.KUWAIT);

        String clock = ArabicFormat.clock12(now) + " " + ArabicFormat.dayPeriod(now);
        if (!clock.equals(lastClock)) {
            lastClock = clock;
            SpannableStringBuilder sb = new SpannableStringBuilder(ArabicFormat.clock12(now));
            int s = sb.length();
            sb.append(" ").append(now.getHour() < 12 ? "ص" : "م");
            sb.setSpan(new RelativeSizeSpan(0.36f), s, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new ForegroundColorSpan(IVORY_SOFT), s, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            clockText.setText(sb);
        }

        // A TV whose clock was never set reports a date years in the past; don't show times as if verified.
        if (now.getYear() < 2025) {
            String key = "invalid";
            if (!key.equals(lastDateKey)) {
                lastDateKey = key;
                gregorianText.setText("تعذّر التحقق من تاريخ الجهاز");
                hijriText.setText("يُرجى ضبط الوقت والتاريخ من إعدادات التلفاز");
                for (TextView t : timeCells) t.setText("—");
                lastPanelDate = null; // force the times back in once the clock is valid
                cityText.setText("مواقيت الصلاة غير متاحة — ساعة الجهاز غير مضبوطة");
                nextName.setText("—");
                remainText.setText("—");
                lastHighlight = -2; highlight(-1);
            }
            return;
        }

        String dateKey = now.toLocalDate() + "|" + prefs.hijriAdjust();
        if (!dateKey.equals(lastDateKey)) {
            lastDateKey = dateKey;
            gregorianText.setText(ArabicFormat.gregorianDate(now));
            String hijri;
            try { hijri = HijriDate.format(now.toLocalDate(), prefs.hijriAdjust()); }
            catch (Exception e) { hijri = ""; }
            hijriText.setText(hijri);
        }

        PrayerCalculator.Next next = PrayerCalculator.next(now, method);
        LocalDate panelDate = next.tomorrow ? now.toLocalDate().plusDays(1) : now.toLocalDate();
        if (!panelDate.equals(lastPanelDate)) {
            lastPanelDate = panelDate;
            PrayerCalculator.Day day = PrayerCalculator.compute(panelDate, method);
            for (int i = 0; i < 6; i++) timeCells[i].setText(ArabicFormat.shortTime(day.times[i]));
            cityText.setText(next.tomorrow ? "مدينة الكويت  •  مواقيت الغد" : "مدينة الكويت  •  الكويت");
        }
        if (next.prayer != lastHighlight) {
            lastHighlight = next.prayer;
            highlight(next.prayer);
            String name = PrayerCalculator.NAMES[next.prayer];
            nextName.setText(next.tomorrow ? name + " (غدًا)" : name);
            remainLabel.setText("متبقي على أذان " + name);
        }

        if (countdownRow.getVisibility() == VISIBLE) {
            remainText.setText(ArabicFormat.countdown(next.secondsRemaining(now)));
            ZonedDateTime prev = PrayerCalculator.previous(next, method);
            long total = Duration.between(prev, next.time).getSeconds();
            long done = Duration.between(prev, now).getSeconds();
            float f = total > 0 ? Math.max(0f, Math.min(1f, done / (float) total)) : 0f;
            progressFill.setPivotX(progressFill.getWidth()); // fills from the right (RTL)
            progressFill.setScaleX(Math.max(0.01f, f));
        }

        // Switch the adhkar pool promptly when the morning/evening window changes.
        String period = periodFor(now);
        if (!period.equals(lastPeriod) && !lastPeriod.isEmpty()) { lastPeriod = period; adhkarIndex = -1; }
        lastPeriod = period;
    }

    private void highlight(int prayer) {
        for (int i = 0; i < 6; i++) {
            boolean on = i == prayer;
            nameCells[i].setTextColor(on ? GOLD : IVORY_SOFT);
            timeCells[i].setTextColor(on ? GOLD : IVORY);
            cells[i].setBackground(on ? pill(0x26E2BE6A, 0x59E2BE6A) : null);
        }
    }

    /** Morning adhkar from Fajr until Dhuhr; evening adhkar from Asr until midnight (Kuwait time). */
    private String periodFor(ZonedDateTime now) {
        PrayerCalculator.Day d = PrayerCalculator.compute(now.toLocalDate(), method);
        if (!now.isBefore(d.times[PrayerCalculator.FAJR]) && now.isBefore(d.times[PrayerCalculator.DHUHR])) return AdhkarRepository.MORNING;
        if (!now.isBefore(d.times[PrayerCalculator.ASR])) return AdhkarRepository.EVENING;
        return AdhkarRepository.ANY;
    }

    // ---------- adhkar ----------

    /**
     * Rotation: inside the morning (Fajr→Dhuhr) or evening (Asr→midnight) window, period-specific adhkar
     * alternate with general ones; otherwise only general adhkar are shown.
     */
    private void showNextDhikr(boolean animate) {
        final String period = lastPeriod.isEmpty() ? periodFor(ZonedDateTime.now(PrayerCalculator.KUWAIT)) : lastPeriod;
        List<AdhkarRepository.Item> specific = repo.specific(period);
        List<AdhkarRepository.Item> general = repo.general();
        adhkarIndex++;
        final AdhkarRepository.Item item;
        if (!specific.isEmpty() && adhkarIndex % 2 == 0) {
            item = specific.get((adhkarIndex / 2) % specific.size());
        } else if (!specific.isEmpty()) {
            item = general.get((adhkarIndex / 2) % general.size());
        } else {
            item = general.get(adhkarIndex % general.size());
        }
        if (!animate) { bind(item, period); return; }
        // Only the text fades; the glass card itself stays in place.
        dhikrContent.animate().alpha(0f).setDuration(700).withEndAction(() -> {
            bind(item, period);
            dhikrContent.animate().alpha(1f).setDuration(900).start();
        }).start();
    }

    private void bind(AdhkarRepository.Item item, String period) {
        String heading = item.category;
        if (AdhkarRepository.BOTH.equals(item.time)) {
            heading = AdhkarRepository.EVENING.equals(period) ? "أذكار المساء" : "أذكار الصباح";
        }
        categoryText.setText(heading);
        dhikrText.setText(item.text);
        applyDhikrSize();
        countText.setText(item.count);
        countText.setVisibility(item.count.isEmpty() ? GONE : VISIBLE);
        sourceText.setText(item.source);
    }

    /** Long texts get a smaller size so they always fit the centre card. */
    private void applyDhikrSize() {
        CharSequence t = dhikrText.getText();
        int letters = 0;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (!(c >= 'ً' && c <= 'ٟ') && c != 'ٰ' && c != ' ') letters++; // ignore diacritics
        }
        float f = letters <= 45 ? 1f : letters <= 90 ? 0.86f : letters <= 160 ? 0.72f : letters <= 260 ? 0.6f : 0.5f;
        dhikrText.setTextSize(TypedValue.COMPLEX_UNIT_PX, u(Prefs.FONT_SP[clampFont(prefs.fontIndex())] * f));
    }

    // ---------- photos ----------

    private void crossFadeTo(Bitmap bmp, Bitmap blurred) {
        if (!running && hasPhoto) return;
        // The two photo layers keep a fixed z-order (below every text layer). Whichever one is on top does
        // the fading, so the old photo stays fully visible underneath until the new one covers it: no black frame.
        final ImageView incoming = photoViews[1 - frontPhoto];
        final ImageView outgoing = photoViews[frontPhoto];
        incoming.animate().cancel();
        outgoing.animate().cancel();
        incoming.setImageBitmap(bmp);
        boolean incomingOnTop = indexOfChild(incoming) > indexOfChild(outgoing);
        if (!hasPhoto || incomingOnTop) {
            incoming.setAlpha(0f);
            outgoing.setAlpha(hasPhoto ? 1f : 0f);
            incoming.animate().alpha(1f).setDuration(2000).withEndAction(() -> {
                outgoing.setAlpha(0f);
                outgoing.setImageDrawable(null); // release the old bitmap
            }).start();
        } else {
            incoming.setAlpha(1f); // sits under the current photo
            outgoing.animate().alpha(0f).setDuration(2000).withEndAction(() -> outgoing.setImageDrawable(null)).start();
        }
        frontPhoto = 1 - frontPhoto;
        hasPhoto = true;
        if (blurred != null) {
            dhikrCard.setBackdrop(blurred);
            prayerCard.setBackdrop(blurred);
        }
    }

    // ---------- helpers ----------

    private int u(float v) { return Math.round(v * unit); }

    private static int clampFont(int i) { return Math.max(0, Math.min(Prefs.FONT_SP.length - 1, i)); }

    private GradientDrawable pill(int fill, int stroke) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(u(40));
        if (stroke != 0) g.setStroke(Math.max(1, u(0.75f)), stroke);
        return g;
    }

    private TextView label(Context c, float size, int color, Typeface tf) {
        TextView t = new TextView(c);
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, size * unit);
        t.setGravity(Gravity.CENTER);
        t.setTextDirection(TEXT_DIRECTION_RTL);
        t.setShadowLayer(u(3), 0, u(1), SHADOW);
        if (tf != null) t.setTypeface(tf);
        return t;
    }

    private LinearLayout.LayoutParams vertical(int topMargin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.topMargin = topMargin;
        return lp;
    }
}
