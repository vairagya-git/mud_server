package com.rama.mudstock.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class TypeConverstionUtil {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private TypeConverstionUtil() {
    }

    public static Timestamp toTimestampFromEpochNanos(Long epochNanos) {
        if (epochNanos == null || epochNanos <= 0) {
            return null;
        }
        long seconds = Math.floorDiv(epochNanos, 1_000_000_000L);
        long nanos = Math.floorMod(epochNanos, 1_000_000_000L);
        return Timestamp.from(Instant.ofEpochSecond(seconds, nanos));
    }

    public static Long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    public static Integer toInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static Integer toInteger(Object value) {
        if (value instanceof Integer intValue) {
            return intValue;
        }
        if (value instanceof Number numberValue) {
            return numberValue.intValue();
        }
        if (value instanceof String strValue) {
            return toInteger(strValue);
        }
        return null;
    }

    public static String toString(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String stringValue) {
            return stringValue.trim();
        }
        return String.valueOf(value).trim();
    }

    public static Boolean toBoolean(Object value) {
        if (value instanceof Boolean boolValue) {
            return boolValue;
        }
        if (value instanceof String strValue) {
            return Boolean.parseBoolean(strValue.trim());
        }
        return null;
    }

    public static Boolean toBooleanStrict(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim();
        if ("true".equalsIgnoreCase(normalized)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(normalized)) {
            return Boolean.FALSE;
        }
        return null;
    }

    public static List<String> toStringList(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(raw.split(","))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .toList();
    }

    public static LocalDate toLocalDateFromDateOrDateTimePrefix(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String datePart = raw.length() >= 10 ? raw.substring(0, 10) : raw;
        try {
            return LocalDate.parse(datePart);
        } catch (Exception ex) {
            return null;
        }
    }

    public static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        return new BigDecimal(value.toString());
    }

    public static BigDecimal toBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    public static LocalDate toLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return null;
        }
        return MudDateUtil.parseIso(text);
    }

    public static BigDecimal round(BigDecimal value, int scale) {
        if (value == null) {
            return null;
        }
        return value.setScale(scale, RoundingMode.HALF_UP);
    }

    public static BigDecimal toPercentAndRound(BigDecimal value, int scale) {
        if (value == null) {
            return null;
        }
        return value.multiply(HUNDRED).setScale(scale, RoundingMode.HALF_UP);
    }
}