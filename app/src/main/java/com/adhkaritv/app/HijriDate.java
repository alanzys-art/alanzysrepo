package com.adhkaritv.app;

import android.icu.util.IslamicCalendar;
import android.icu.util.TimeZone;
import android.icu.util.ULocale;

import java.time.LocalDate;

/**
 * Hijri date using the Umm al-Qura calendar (Android's built-in ICU, IslamicCalendar ISLAMIC_UMALQURA).
 *
 * Umm al-Qura is a tabulated calendar (Saudi Arabia). It is NOT an official Kuwaiti source. On 30 Oct 2026
 * it gives 19 Jumada al-Ula 1448, matching the date printed by Kuwaiti daily timetables (Al-Anba), but around
 * month starts Kuwait's announced date can differ by a day — hence the user setting of −1 / 0 / +1 day.
 * The date changes at midnight Kuwait time (civil convention), not at Maghrib.
 */
public final class HijriDate {
    private HijriDate() {}

    public static String format(LocalDate kuwaitDate, int adjustDays) {
        IslamicCalendar cal = new IslamicCalendar(TimeZone.getTimeZone("Asia/Kuwait"), ULocale.ROOT);
        cal.setCalculationType(IslamicCalendar.CalculationType.ISLAMIC_UMALQURA);
        long noonMillis = kuwaitDate.plusDays(adjustDays).atTime(12, 0)
                .atZone(PrayerCalculator.KUWAIT).toInstant().toEpochMilli();
        cal.setTimeInMillis(noonMillis);
        int day = cal.get(IslamicCalendar.DAY_OF_MONTH);
        int month = cal.get(IslamicCalendar.MONTH) + 1; // ICU months are 0-based
        int year = cal.get(IslamicCalendar.YEAR);
        return ArabicFormat.hijriDate(day, month, year);
    }
}
