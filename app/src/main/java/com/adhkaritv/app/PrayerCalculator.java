package com.adhkaritv.app;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * On-device prayer-time calculation for Kuwait City.
 *
 * Algorithm: the standard PrayTimes.org astronomical method (sun declination and
 * equation of time), computed for the Kuwait calendar date in the Asia/Kuwait zone.
 *
 * Default method — "Kuwait" (as used by Kuwaiti timetables): Fajr at 18°, Isha at 17.5°
 * below the horizon, Asr by the standard (shadow = 1×) rule, Maghrib at sunset.
 * Checked on 30 Oct 2026 against the Kuwaiti daily timetable published by Al-Anba:
 * Fajr 04:39, Sunrise 05:59, Dhuhr 11:32, Asr 14:40, Maghrib 17:04, Isha 18:22 — exact match.
 *
 * Pure Java (no Android classes) so it can be unit-tested on a desktop JVM.
 */
public final class PrayerCalculator {

    public static final ZoneId KUWAIT = ZoneId.of("Asia/Kuwait");
    /** Kuwait City. */
    public static final double LATITUDE = 29.3759;
    public static final double LONGITUDE = 47.9774;

    public static final int FAJR = 0, SUNRISE = 1, DHUHR = 2, ASR = 3, MAGHRIB = 4, ISHA = 5;
    public static final String[] NAMES = {"الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء"};
    /** Obligatory prayers, in order. Sunrise is informational only and never "next prayer". */
    private static final int[] PRAYERS = {FAJR, DHUHR, ASR, MAGHRIB, ISHA};

    public enum Method {
        /** Kuwait: Fajr 18°, Isha 17.5°. */
        KUWAIT(18.0, 17.5, 0, "الطريقة الكويتية (الفجر 18°، العشاء 17.5°)"),
        /** Umm al-Qura, Makkah: Fajr 18.5°, Isha 90 minutes after Maghrib (Ramadan 120-minute rule not applied). */
        UMM_AL_QURA(18.5, 0, 90, "أم القرى (الفجر 18.5°، العشاء بعد المغرب بـ90 دقيقة)"),
        /** Muslim World League: Fajr 18°, Isha 17°. */
        MWL(18.0, 17.0, 0, "رابطة العالم الإسلامي (الفجر 18°، العشاء 17°)");

        final double fajrAngle;
        final double ishaAngle;
        final int ishaMinutes;
        public final String arabicName;

        Method(double fajrAngle, double ishaAngle, int ishaMinutes, String arabicName) {
            this.fajrAngle = fajrAngle;
            this.ishaAngle = ishaAngle;
            this.ishaMinutes = ishaMinutes;
            this.arabicName = arabicName;
        }

        public static Method fromName(String name) {
            for (Method m : values()) if (m.name().equals(name)) return m;
            return KUWAIT;
        }
    }

    /** The six times for one Kuwait calendar date, rounded to the minute. */
    public static final class Day {
        public final LocalDate date;
        public final ZonedDateTime[] times;
        Day(LocalDate date, ZonedDateTime[] times) { this.date = date; this.times = times; }
    }

    /** The upcoming obligatory prayer. */
    public static final class Next {
        public final int prayer;
        public final ZonedDateTime time;
        /** True after Isha: the next prayer is tomorrow's Fajr. */
        public final boolean tomorrow;
        Next(int prayer, ZonedDateTime time, boolean tomorrow) { this.prayer = prayer; this.time = time; this.tomorrow = tomorrow; }

        /** Whole seconds remaining, rounded up, never negative. */
        public long secondsRemaining(ZonedDateTime now) {
            long ms = Duration.between(now, time).toMillis();
            if (ms <= 0) return 0;
            return (ms + 999) / 1000;
        }
    }

    private PrayerCalculator() {}

    public static Day compute(LocalDate date, Method m) {
        double tz = KUWAIT.getRules().getOffset(date.atTime(12, 0)).getTotalSeconds() / 3600.0;
        double jd = julian(date.getYear(), date.getMonthValue(), date.getDayOfMonth()) - LONGITUDE / (15.0 * 24.0);

        // Initial guesses in hours, refined twice.
        double fajr = 5, sunrise = 6, dhuhr = 12, asr = 13, sunset = 18, isha = 18;
        for (int i = 0; i < 2; i++) {
            double f = fajr / 24, sr = sunrise / 24, d = dhuhr / 24, a = asr / 24, ss = sunset / 24, is = isha / 24;
            fajr = sunAngleTime(jd, m.fajrAngle, f, true);
            sunrise = sunAngleTime(jd, 0.833, sr, true);
            dhuhr = midDay(jd, d);
            asr = asrTime(jd, 1, a);
            sunset = sunAngleTime(jd, 0.833, ss, false);
            isha = m.ishaMinutes > 0 ? sunset : sunAngleTime(jd, m.ishaAngle, is, false);
        }
        double shift = tz - LONGITUDE / 15.0;
        double[] hours = {fajr + shift, sunrise + shift, dhuhr + shift, asr + shift, sunset + shift,
                m.ishaMinutes > 0 ? sunset + shift + m.ishaMinutes / 60.0 : isha + shift};

        ZonedDateTime midnight = date.atStartOfDay(KUWAIT);
        ZonedDateTime[] out = new ZonedDateTime[6];
        for (int i = 0; i < 6; i++) {
            long minutes = Math.round(hours[i] * 60.0);
            out[i] = midnight.plusMinutes(minutes);
        }
        return new Day(date, out);
    }

    /** Finds the next obligatory prayer strictly after {@code now}; after Isha it is tomorrow's Fajr. */
    public static Next next(ZonedDateTime now, Method m) {
        ZonedDateTime k = now.withZoneSameInstant(KUWAIT);
        Day today = compute(k.toLocalDate(), m);
        for (int p : PRAYERS) {
            if (today.times[p].isAfter(k)) return new Next(p, today.times[p], false);
        }
        Day tomorrow = compute(k.toLocalDate().plusDays(1), m);
        return new Next(FAJR, tomorrow.times[FAJR], true);
    }

    /** The adhan before {@code next} (for Fajr: the previous evening's Isha). Used for the progress bar. */
    public static ZonedDateTime previous(Next next, Method m) {
        LocalDate d = next.time.toLocalDate();
        if (next.prayer == FAJR) return compute(d.minusDays(1), m).times[ISHA];
        int[] order = PRAYERS;
        for (int i = 1; i < order.length; i++) {
            if (order[i] == next.prayer) return compute(d, m).times[order[i - 1]];
        }
        return compute(d.minusDays(1), m).times[ISHA];
    }

    // ---- astronomy (PrayTimes.org) ----

    private static double sunAngleTime(double jd, double angle, double t, boolean beforeNoon) {
        double[] dp = sunPosition(jd + t);
        double decl = dp[0];
        double noon = midDay(jd, t);
        double x = (-dsin(angle) - dsin(decl) * dsin(LATITUDE)) / (dcos(decl) * dcos(LATITUDE));
        x = Math.max(-1, Math.min(1, x)); // never occurs at Kuwait's latitude; guards NaN
        double h = Math.toDegrees(Math.acos(x)) / 15.0;
        return noon + (beforeNoon ? -h : h);
    }

    private static double asrTime(double jd, double factor, double t) {
        double decl = sunPosition(jd + t)[0];
        double angle = -Math.toDegrees(Math.atan(1.0 / (factor + Math.tan(Math.toRadians(Math.abs(LATITUDE - decl))))));
        return sunAngleTime(jd, angle, t, false);
    }

    private static double midDay(double jd, double t) {
        double eqt = sunPosition(jd + t)[1];
        return fix(12 - eqt, 24);
    }

    /** Returns {declination in degrees, equation of time in hours}. */
    private static double[] sunPosition(double jd) {
        double d = jd - 2451545.0;
        double g = fix(357.529 + 0.98560028 * d, 360);
        double q = fix(280.459 + 0.98564736 * d, 360);
        double l = fix(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g), 360);
        double e = 23.439 - 0.00000036 * d;
        double ra = fix(Math.toDegrees(Math.atan2(dcos(e) * dsin(l), dcos(l))) / 15.0, 24);
        double eqt = q / 15.0 - ra;
        double decl = Math.toDegrees(Math.asin(dsin(e) * dsin(l)));
        return new double[]{decl, eqt};
    }

    private static double julian(int year, int month, int day) {
        if (month <= 2) { year -= 1; month += 12; }
        double a = Math.floor(year / 100.0);
        double b = 2 - a + Math.floor(a / 4.0);
        return Math.floor(365.25 * (year + 4716)) + Math.floor(30.6001 * (month + 1)) + day + b - 1524.5;
    }

    private static double dsin(double d) { return Math.sin(Math.toRadians(d)); }
    private static double dcos(double d) { return Math.cos(Math.toRadians(d)); }
    private static double fix(double a, double b) { a = a - b * Math.floor(a / b); return a < 0 ? a + b : a; }
}
