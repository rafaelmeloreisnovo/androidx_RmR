/*
 * Copyright (C) 2026 Rafael Melo Reis (RmR)
 *
 * Licensed under the Apache License, Version 2.0 with Additional Restrictions.
 * See LEGAL_NOTICE.md for complete terms and automatic penalty provisions.
 * 
 * AUTHORIZED USE ONLY - Unauthorized use subject to automatic penalties.
 */

package androidx.rmr.rafaelia;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/** Compatibility helpers for low-API classpath safety. */
final class RafaeliaCompat {
    private RafaeliaCompat() {}

    static boolean isRegularFile(@Nullable File file) {
        return file != null && file.isFile();
    }

    @NonNull
    static String readUtf8File(@NonNull File file) throws IOException {
        try (FileInputStream input = new FileInputStream(file);
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }

    static boolean isCurrentTimeBefore(@Nullable String iso8601DateTime) {
        Long target = parseIso8601ToUtcMillis(iso8601DateTime);
        if (target == null) {
            return false;
        }
        return System.currentTimeMillis() < target.longValue();
    }

    static boolean isCurrentTimeAtOrAfter(@Nullable String iso8601DateTime) {
        Long target = parseIso8601ToUtcMillis(iso8601DateTime);
        if (target == null) {
            return false;
        }
        return System.currentTimeMillis() >= target.longValue();
    }

    @Nullable
    private static Long parseIso8601ToUtcMillis(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        if (text.isEmpty()) {
            return null;
        }

        int length = text.length();
        if (length < 20) {
            return null;
        }

        int timeMarker = text.indexOf('T');
        if (timeMarker < 0) {
            timeMarker = text.indexOf('t');
        }
        if (timeMarker < 0) {
            return null;
        }

        int zoneStart = findZoneStart(text, timeMarker + 1);
        if (zoneStart < 0) {
            return null;
        }

        try {
            int year = parseInt(text, 0, 4);
            int month = parseInt(text, 5, 7);
            int day = parseInt(text, 8, 10);
            int hour = parseInt(text, 11, 13);
            int minute = parseInt(text, 14, 16);
            int second = parseInt(text, 17, 19);

            int milli = 0;
            if (zoneStart > 19 && text.charAt(19) == '.') {
                milli = parseFractionalMillis(text, 20, zoneStart);
            }

            int offsetMinutes = parseZoneOffsetMinutes(text, zoneStart);

            Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
            calendar.setLenient(false);
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month - 1);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, second);
            calendar.set(Calendar.MILLISECOND, milli);

            long utcMillis = calendar.getTimeInMillis();
            return utcMillis - (offsetMinutes * 60L * 1000L);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static int findZoneStart(String text, int fromIndex) {
        int zIndex = text.indexOf('Z', fromIndex);
        if (zIndex < 0) {
            zIndex = text.indexOf('z', fromIndex);
        }
        int plusIndex = text.indexOf('+', fromIndex);
        int minusIndex = text.indexOf('-', fromIndex);

        int index = -1;
        if (zIndex >= 0) {
            index = zIndex;
        }
        if (plusIndex >= 0 && (index < 0 || plusIndex < index)) {
            index = plusIndex;
        }
        if (minusIndex >= 0 && (index < 0 || minusIndex < index)) {
            index = minusIndex;
        }
        return index;
    }

    private static int parseZoneOffsetMinutes(String text, int start) {
        char indicator = text.charAt(start);
        if (indicator == 'Z' || indicator == 'z') {
            if (start != text.length() - 1) {
                throw new IllegalArgumentException("Invalid zone");
            }
            return 0;
        }
        if (indicator != '+' && indicator != '-') {
            throw new IllegalArgumentException("Missing zone sign");
        }
        int sign = indicator == '+' ? 1 : -1;

        if (start + 6 != text.length() || text.charAt(start + 3) != ':') {
            throw new IllegalArgumentException("Unsupported zone format");
        }
        int hours = parseInt(text, start + 1, start + 3);
        int minutes = parseInt(text, start + 4, start + 6);
        if (hours > 23 || minutes > 59) {
            throw new IllegalArgumentException("Invalid zone offset");
        }
        return sign * (hours * 60 + minutes);
    }

    private static int parseFractionalMillis(String text, int start, int end) {
        int digits = end - start;
        if (digits <= 0) {
            return 0;
        }
        int value = 0;
        int limit = Math.min(3, digits);
        for (int i = 0; i < limit; i++) {
            char c = text.charAt(start + i);
            if (c < '0' || c > '9') {
                throw new IllegalArgumentException("Invalid fractional second");
            }
            value = (value * 10) + (c - '0');
        }
        if (limit == 1) {
            return value * 100;
        }
        if (limit == 2) {
            return value * 10;
        }
        return value;
    }

    private static int parseInt(String text, int start, int end) {
        if (start < 0 || end > text.length() || end <= start) {
            throw new IllegalArgumentException("Invalid range");
        }
        int value = 0;
        for (int i = start; i < end; i++) {
            char c = text.charAt(i);
            if (c < '0' || c > '9') {
                throw new IllegalArgumentException("Invalid number");
            }
            value = (value * 10) + (c - '0');
        }
        return value;
    }
}
