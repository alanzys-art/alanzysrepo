package com.adhkaritv.app;

import java.time.ZonedDateTime;

/** Arabic text formatting for times and Gregorian dates. Pure Java (unit-testable). */
public final class ArabicFormat {

    private static final String[] WEEKDAYS = {"الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت", "الأحد"};
    private static final String[] MONTHS = {"يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
            "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"};
    public static final String[] HIJRI_MONTHS = {"محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
            "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"};

    private ArabicFormat() {}

    /** Converts Western digits to Arabic-Indic digits (٠١٢٣٤٥٦٧٨٩). */
    public static String digits(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            b.append(c >= '0' && c <= '9' ? (char) ('٠' + (c - '0')) : c);
        }
        return b.toString();
    }

    public static String digits(long n) { return digits(Long.toString(n)); }

    private static String two(long n) { return digits(n < 10 ? "0" + n : Long.toString(n)); }

    /** "٥:٣٠" (12-hour, no leading zero on the hour). */
    public static String clock12(ZonedDateTime t) {
        int h = t.getHour() % 12;
        if (h == 0) h = 12;
        return digits(h) + ":" + two(t.getMinute());
    }

    /** "صباحًا" or "مساءً". */
    public static String dayPeriod(ZonedDateTime t) { return t.getHour() < 12 ? "صباحًا" : "مساءً"; }

    /** Short form for the prayer panel: "٤:٣٩ ص". */
    public static String shortTime(ZonedDateTime t) { return clock12(t) + " " + (t.getHour() < 12 ? "ص" : "م"); }

    /** "الجمعة، ٣٠ أكتوبر ٢٠٢٦". */
    public static String gregorianDate(ZonedDateTime t) {
        return WEEKDAYS[t.getDayOfWeek().getValue() - 1] + "، " + digits(t.getDayOfMonth()) + " "
                + MONTHS[t.getMonthValue() - 1] + " " + digits(t.getYear());
    }

    /** "١٩ جمادى الأولى ١٤٤٨ هـ" — month is 1-based. */
    public static String hijriDate(int day, int month, int year) {
        return digits(day) + " " + HIJRI_MONTHS[month - 1] + " " + digits(year) + " هـ";
    }

    /** "٠١:٢٥:٣٠". */
    public static String countdown(long seconds) {
        if (seconds < 0) seconds = 0;
        return two(seconds / 3600) + ":" + two((seconds / 60) % 60) + ":" + two(seconds % 60);
    }
}
