package com.example.mef.demo.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DateUtil {

    private static final DateTimeFormatter FR_SHORT_DATE =
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH);

    /** Formats a date like "31 juil. 2026", or "—" if null. */
    public static String frShort(LocalDate date) {
        return date == null ? "—" : FR_SHORT_DATE.format(date);
    }

    public static String frShort(LocalDateTime date) {
        return date == null ? "—" : frShort(date.toLocalDate());
    }

    /** Formats dates in the application's active language. */
    public static String localizedShort(LocalDate date) {
        return date == null ? "—" : DateTimeFormatter.ofPattern("d MMM yyyy", I18n.getLocale()).format(date);
    }

    public static String localizedShort(LocalDateTime date) {
        return date == null ? "—" : localizedShort(date.toLocalDate());
    }
}
