package com.rama.mudstock.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;

public final class DataConversionUtil {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private DataConversionUtil() {
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