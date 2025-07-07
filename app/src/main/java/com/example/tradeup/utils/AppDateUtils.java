package com.example.tradeup.utils;

import android.text.format.DateUtils; // Import Android's DateUtils
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class AppDateUtils { // Đổi tên class từ DateUtils thành AppDateUtils

    private static final String ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    // Converts ISO 8601 string to Date object
    public static Date parseIsoDate(String isoDateString) {
        SimpleDateFormat sdf = new SimpleDateFormat(ISO_FORMAT, Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // Important: Parse UTC time
        try {
            return sdf.parse(isoDateString);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Formats Date object to ISO 8601 string
    public static String formatIsoDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat(ISO_FORMAT, Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // Important: Format to UTC time
        return sdf.format(date);
    }

    // Gets a human-readable "time ago" string (e.g., "5 minutes ago", "yesterday")
    public static CharSequence getTimeAgo(String isoDateString) {
        Date date = parseIsoDate(isoDateString);
        if (date == null) {
            return isoDateString; // Fallback to original string if parsing fails
        }
        long now = System.currentTimeMillis();
        long time = date.getTime();
        // Use Android's DateUtils for relative time span
        return DateUtils.getRelativeTimeSpanString(time, now, DateUtils.MINUTE_IN_MILLIS);
    }

    // Formats Date object to a specific display format (e.g., "dd/MM/yyyy HH:mm")
    public static String formatDisplayDate(String isoDateString) {
        Date date = parseIsoDate(isoDateString);
        if (date == null) {
            return isoDateString;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(date);
    }
}
