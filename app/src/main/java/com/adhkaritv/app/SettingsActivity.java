package com.adhkaritv.app;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.time.LocalDate;

/**
 * Remote-friendly Arabic settings. Up/Down moves between rows; OK or Left changes a value forward,
 * Right changes it back; Back returns to the screen (changes apply immediately).
 */
public class SettingsActivity extends Activity {

    private interface Row { String value(); void change(int dir); }

    private Prefs prefs;
    private float unit;
    private LinearLayout list;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = new Prefs(this);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        unit = Math.max(dm.widthPixels, dm.heightPixels) / 960f;

        ScrollView scroll = new ScrollView(this);
        scroll.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{AdhkarView.NIGHT_TOP, AdhkarView.NIGHT_BOTTOM}));
        scroll.setFillViewport(true);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        list.setPadding(u(150), u(36), u(150), u(36));
        scroll.addView(list);
        setContentView(scroll);

        TextView title = text(26, AdhkarView.GOLD);
        try { title.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Amiri-Regular.ttf")); } catch (Exception ignored) { }
        title.setText("إعدادات أذكاري");
        list.addView(title);
        TextView sub = text(12, AdhkarView.IVORY_DIM);
        sub.setText("الأسهم للتنقل • زر OK لتغيير القيمة • زر الرجوع للعودة");
        list.addView(sub, margins(u(2), u(18)));

        addRow("مدة تغيير الأذكار", new Row() {
            public String value() { return seconds(prefs.adhkarSeconds()); }
            public void change(int d) { prefs.setAdhkarSeconds(cycle(Prefs.ADHKAR_SECONDS, prefs.adhkarSeconds(), d)); }
        });
        addRow("مدة تغيير الصور", new Row() {
            public String value() { return seconds(prefs.photoSeconds()); }
            public void change(int d) { prefs.setPhotoSeconds(cycle(Prefs.PHOTO_SECONDS, prefs.photoSeconds(), d)); }
        });
        addRow("ضبط التاريخ الهجري", new Row() {
            public String value() {
                int a = prefs.hijriAdjust();
                String label = a == 0 ? "بدون تعديل" : (a > 0 ? "+١ يوم" : "−١ يوم");
                String preview;
                try { preview = HijriDate.format(LocalDate.now(PrayerCalculator.KUWAIT), a); } catch (Exception e) { preview = ""; }
                return label + "  (" + preview + ")";
            }
            public void change(int d) { prefs.setHijriAdjust(cycle(Prefs.HIJRI_ADJUST, prefs.hijriAdjust(), d)); }
        });
        addRow("العد التنازلي للصلاة القادمة", new Row() {
            public String value() { return prefs.countdown() ? "مُفعَّل" : "متوقف"; }
            public void change(int d) { prefs.setCountdown(!prefs.countdown()); }
        });
        addRow("حجم خط الأذكار", new Row() {
            public String value() { return Prefs.FONT_LABELS[Math.max(0, Math.min(2, prefs.fontIndex()))]; }
            public void change(int d) { prefs.setFontIndex(Math.floorMod(prefs.fontIndex() + d, Prefs.FONT_LABELS.length)); }
        });
        addRow("طريقة حساب مواقيت الصلاة", new Row() {
            public String value() { return prefs.method().arabicName; }
            public void change(int d) {
                PrayerCalculator.Method[] all = PrayerCalculator.Method.values();
                prefs.setMethod(all[Math.floorMod(prefs.method().ordinal() + d, all.length)]);
            }
        });
        final String[] refreshed = {"اضغط OK لإعادة الحساب وعرض مواقيت اليوم"};
        addRow("تحديث مواقيت الصلاة", new Row() {
            public String value() { return refreshed[0]; }
            public void change(int d) {
                PrayerCalculator.Day day = PrayerCalculator.compute(LocalDate.now(PrayerCalculator.KUWAIT), prefs.method());
                StringBuilder b = new StringBuilder();
                for (int i = 0; i < 6; i++) {
                    if (i > 0) b.append("  |  ");
                    b.append(PrayerCalculator.NAMES[i]).append(" ").append(ArabicFormat.shortTime(day.times[i]));
                }
                refreshed[0] = b.toString();
            }
        });
        addRow("استعادة الإعدادات الافتراضية", new Row() {
            public String value() { return "زر OK"; }
            public void change(int d) { prefs.reset(); recreate(); }
        });

        TextView note = text(11.5f, AdhkarView.IVORY_DIM);
        note.setGravity(Gravity.RIGHT);
        note.setLineSpacing(0, 1.25f);
        note.setText("• تُحسب مواقيت الصلاة على الجهاز نفسه لمدينة الكويت (٢٩٫٣٨° شمالًا، ٤٧٫٩٨° شرقًا) بتوقيت الكويت، "
                + "ولا تحتاج إلى الإنترنت. الطريقة الافتراضية هي الطريقة الكويتية، وقد طابقت نتائجها الجدول المنشور "
                + "في الكويت يوم ٣٠ أكتوبر ٢٠٢٦. قد تختلف المواقيت الرسمية لوزارة الأوقاف بدقيقة أحيانًا.\n"
                + "• التاريخ الهجري محسوب وفق تقويم أم القرى، وليس مصدرًا كويتيًا رسميًا؛ وقد يختلف بيوم عن التاريخ المعلن "
                + "في الكويت عند بداية الشهر، فاستخدم «ضبط التاريخ الهجري» عند الحاجة.\n"
                + "• لا يُشغَّل الأذان صوتيًا في هذا الإصدار.");
        list.addView(note, margins(u(22), 0));

        if (list.getChildCount() > 2) list.getChildAt(2).requestFocus();
    }

    private void addRow(String label, Row row) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setFocusable(true);
        r.setFocusableInTouchMode(true);
        r.setPadding(u(18), u(10), u(18), u(10));
        GradientDrawable focused = new GradientDrawable();
        focused.setColor(0x33E2BE6A);
        focused.setStroke(Math.max(1, u(1)), AdhkarView.GOLD);
        focused.setCornerRadius(u(10));
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_focused}, focused);
        bg.addState(new int[]{}, new ColorDrawable(0x14FFFFFF));
        r.setBackground(bg);

        TextView name = text(15, AdhkarView.IVORY);
        name.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        TextView value = text(13.5f, AdhkarView.GOLD);
        value.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        value.setText(row.value());
        r.addView(name, new LinearLayout.LayoutParams(0, -2, 1f));
        r.addView(value, new LinearLayout.LayoutParams(0, -2, 1.6f));
        name.setText(label);

        r.setOnClickListener(v -> { row.change(1); value.setText(row.value()); });
        r.setOnKeyListener((v, keyCode, e) -> {
            if (e.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) { row.change(1); value.setText(row.value()); return true; }
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) { row.change(-1); value.setText(row.value()); return true; }
            return false;
        });
        list.addView(r, margins(u(5), u(5)));
    }

    private static int cycle(int[] options, int current, int dir) {
        int idx = 0;
        for (int i = 0; i < options.length; i++) if (options[i] == current) idx = i;
        return options[Math.floorMod(idx + dir, options.length)];
    }

    private static String seconds(int s) {
        if (s < 60) return ArabicFormat.digits(s) + " ثانية";
        int m = s / 60;
        return m == 1 ? "دقيقة واحدة" : m == 2 ? "دقيقتان" : ArabicFormat.digits(m) + (m <= 10 ? " دقائق" : " دقيقة");
    }

    private TextView text(float size, int color) {
        TextView t = new TextView(this);
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, size * unit);
        t.setTextDirection(View.TEXT_DIRECTION_RTL);
        t.setGravity(Gravity.RIGHT);
        return t;
    }

    private LinearLayout.LayoutParams margins(int top, int bottom) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = top;
        lp.bottomMargin = bottom;
        return lp;
    }

    private int u(float v) { return Math.round(v * unit); }
}
