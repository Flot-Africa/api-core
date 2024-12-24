package africa.flot.infrastructure.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DateUtil {
    private static final String DEFAULT_DATE_FORMAT = "dd MMMM yyyy";
    private static final Locale DEFAULT_LOCALE = Locale.FRENCH;

    public static String formatCurrentDate() {
        return formatDate(LocalDateTime.now());
    }

    public static String formatDate(LocalDateTime date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT, DEFAULT_LOCALE);
        return date.format(formatter);
    }

    public static String getDateFormat() {
        return DEFAULT_DATE_FORMAT;
    }

    public static String getLocale() {
        return DEFAULT_LOCALE.getLanguage();
    }
}